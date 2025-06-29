package com.example.faceswapapp.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Log
import android.os.Environment
import android.util.Base64
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import android.media.ExifInterface
import com.example.faceswapapp.OpenCVHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import org.json.JSONArray
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import okhttp3.Callback
import okhttp3.Call
import okhttp3.Response
import java.io.IOException
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private const val TAG = "FSWAPTRACE"

object ImageUtils {
    // ... tutte le funzioni precedenti invariato ...

    // Salva una bitmap PNG in cacheDir con nome file scelto e restituisce il path assoluto
    fun saveJobResultBitmapToFile(context: Context, bitmap: Bitmap, jobId: String): String {
        try {
            val file = File(context.cacheDir, "job_result_$jobId.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d(TAG, "saveJobResultBitmapToFile: Saved result to ${file.absolutePath}")
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "saveJobResultBitmapToFile: Error ${e.message}")
        }
        return ""
    }

    fun loadBitmapFromFile(path: String): Bitmap? {
        return try {
            val bmp = BitmapFactory.decodeFile(path)
            Log.d(TAG, "loadBitmapFromFile: Loaded $path ok? ${bmp != null}")
            bmp
        } catch (e: Exception) {
            Log.e(TAG, "loadBitmapFromFile: Error loading $path: ${e.message}")
            null
        }
    }

    // Salva un bitmap come PNG in cacheDir con nome file scelto
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, filename: String) {
        try {
            val file = File(context.cacheDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val exifStream = context.contentResolver.openInputStream(uri)
                    val exif = exifStream?.let { ExifInterface(it) }
                    val orientation = exif?.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    ) ?: ExifInterface.ORIENTATION_NORMAL
                    exifStream?.close()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                        else -> bitmap
                    }
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun rotateBitmap(src: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    fun loadLastDebugStep6Bitmap(context: Context, width: Int, height: Int): Bitmap? {
        try {
            val filesDir = context.filesDir
            val pattern = Pattern.compile("debug_step6_final_blended_(\\d+)\\.png")
            val files = filesDir.listFiles(FileFilter { file ->
                pattern.matcher(file.name).matches()
            }) ?: return null
            if (files.isEmpty()) return null
            val lastFile = files.maxByOrNull { file ->
                val matcher = pattern.matcher(file.name)
                if (matcher.matches()) matcher.group(1)?.toIntOrNull() ?: 0 else 0
            } ?: return null
            val bmp = BitmapFactory.decodeFile(lastFile.absolutePath)
            return Bitmap.createScaledBitmap(bmp, width, height, true)
        } catch (_: Exception) {
            return null
        }
    }

    fun saveToGallery(context: Context, bitmap: Bitmap, onResult: (Boolean) -> Unit) {
        val filename = "faceswap_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.WIDTH, bitmap.width)
                put(MediaStore.Images.Media.HEIGHT, bitmap.height)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FaceSwap")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            val fos = resolver.openOutputStream(imageUri!!)
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.close()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
                onResult(true)
            } else {
                onResult(false)
            }
        } catch (_: Exception) {
            onResult(false)
        }
    }

    suspend fun detectLandmarksForFace(context: Context, bitmap: Bitmap): List<Offset> {
        return withContext(Dispatchers.IO) {
            val baseOptionBuilder = BaseOptions.builder().setModelAssetPath("face_landmarker.task")
            val baseOptions = baseOptionBuilder.build()
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setNumFaces(1)
                .build()

            try {
                val faceLandmarker = FaceLandmarker.createFromOptions(context, options)
                val mpImage = BitmapImageBuilder(bitmap).build()
                val results = faceLandmarker.detect(mpImage)
                faceLandmarker.close()
                results.faceLandmarks().firstOrNull()?.map { point ->
                    Offset(point.x() * bitmap.width, point.y() * bitmap.height)
                } ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    suspend fun detectFaceLandmarksFromUri(
        context: Context,
        uri: Uri,
        callback: (List<List<Offset>>) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val baseOptionBuilder = BaseOptions.builder().setModelAssetPath("face_landmarker.task")
            val baseOptions = baseOptionBuilder.build()
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setNumFaces(4)
                .build()
            try {
                val faceLandmarker = FaceLandmarker.createFromOptions(context, options)
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                val mpImage = BitmapImageBuilder(bitmap).build()
                val results = faceLandmarker.detect(mpImage)
                faceLandmarker.close()
                val landmarks = results.faceLandmarks().map { faceLandmarks ->
                    faceLandmarks.map { point ->
                        Offset(point.x() * bitmap.width, point.y() * bitmap.height)
                    }
                }
                callback(landmarks)
            } catch (_: Exception) {
                callback(emptyList())
            }
        }
    }

    fun cropFaceFromLandmarks(bitmap: Bitmap, landmarks: List<Offset>): Bitmap {
        if (landmarks.isEmpty()) return bitmap
        val minX = landmarks.minOf { it.x }.toInt().coerceAtLeast(0)
        val minY = landmarks.minOf { it.y }.toInt().coerceAtLeast(0)
        val maxX = landmarks.maxOf { it.x }.toInt().coerceAtMost(bitmap.width - 1)
        val maxY = landmarks.maxOf { it.y }.toInt().coerceAtMost(bitmap.height - 1)
        val boxWidth = maxX - minX
        val boxHeight = maxY - minY
        val size = maxOf(boxWidth, boxHeight)
        val centerX = (minX + maxX) / 2
        val centerY = (minY + maxY) / 2
        val left = (centerX - size / 2).coerceAtLeast(0)
        val top = (centerY - size / 2).coerceAtLeast(0)
        val right = minOf(left + size, bitmap.width)
        val bottom = minOf(top + size, bitmap.height)
        val cropRect = android.graphics.Rect(left, top, right, bottom)
        val faceBitmap = Bitmap.createBitmap(cropRect.width(), cropRect.height(), Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(faceBitmap)
        canvas.drawBitmap(bitmap, cropRect, android.graphics.Rect(0, 0, cropRect.width(), cropRect.height()), null)
        return faceBitmap
    }

    fun compositePersonOnBackground(
        originalBitmap: Bitmap,
        maskBitmap: Bitmap,
        backgroundBitmap: Bitmap
    ): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        val bgResized = Bitmap.createScaledBitmap(backgroundBitmap, width, height, true)
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val origPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        val bgPixels = IntArray(width * height)
        originalBitmap.getPixels(origPixels, 0, width, 0, 0, width, height)
        maskBitmap.getPixels(maskPixels, 0, width, 0, 0, width, height)
        bgResized.getPixels(bgPixels, 0, width, 0, 0, width, height)

        for (i in origPixels.indices) {
            val maskAlpha = (maskPixels[i] shr 24) and 0xFF
            if (maskAlpha > 128) {
                result.setPixel(i % width, i / width, origPixels[i])
            } else {
                result.setPixel(i % width, i / width, bgPixels[i])
            }
        }
        return result
    }

    fun extractPersonWithAlpha(original: Bitmap, mask: Bitmap): Bitmap {
        val width = original.width
        val height = original.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val origPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        original.getPixels(origPixels, 0, width, 0, 0, width, height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)
        for (i in origPixels.indices) {
            val maskAlpha = (maskPixels[i] shr 24) and 0xFF
            result.setPixel(i % width, i / width, (maskAlpha shl 24) or (origPixels[i] and 0x00FFFFFF))
        }
        return result
    }

    suspend fun segmentPersonBitmap(context: Context, bitmap: Bitmap): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val options = SelfieSegmenterOptions.Builder()
                    .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                    .build()
                val segmenter = Segmentation.getClient(options)
                val image = InputImage.fromBitmap(bitmap, 0)

                val maskResult = Tasks.await(segmenter.process(image))
                val mask = maskResult?.buffer
                val maskWidth = maskResult?.width ?: 0
                val maskHeight = maskResult?.height ?: 0

                if (mask != null && maskWidth > 0 && maskHeight > 0) {
                    // Crea bitmap della maschera a risoluzione nativa ML Kit
                    val maskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8)
                    val maskPixels = FloatArray(maskWidth * maskHeight)
                    mask.rewind()
                    for (i in maskPixels.indices) {
                        maskPixels[i] = mask.float
                    }
                    for (y in 0 until maskHeight) {
                        for (x in 0 until maskWidth) {
                            val alpha = (maskPixels[y * maskWidth + x] * 255).toInt().coerceIn(0, 255)
                            maskBitmap.setPixel(x, y, (alpha shl 24))
                        }
                    }

                    // === PATCH: SCALING "FILL" (CROP) ===
                    val aspectInput = bitmap.width.toFloat() / bitmap.height
                    val aspectMask = maskWidth.toFloat() / maskHeight

                    val destRect: android.graphics.Rect
                    val srcRect: android.graphics.Rect

                    if (aspectInput > aspectMask) {
                        // L'immagine di input è più larga: tagliare la maschera in orizzontale
                        val newMaskWidth = (maskHeight * aspectInput).toInt()
                        val cropX = (maskWidth - newMaskWidth) / 2
                        srcRect = android.graphics.Rect(
                            cropX.coerceAtLeast(0), 0,
                            (cropX + newMaskWidth).coerceAtMost(maskWidth), maskHeight
                        )
                    } else {
                        // L'immagine di input è più alta: tagliare la maschera in verticale
                        val newMaskHeight = (maskWidth / aspectInput).toInt()
                        val cropY = (maskHeight - newMaskHeight) / 2
                        srcRect = android.graphics.Rect(
                            0, cropY.coerceAtLeast(0),
                            maskWidth, (cropY + newMaskHeight).coerceAtMost(maskHeight)
                        )
                    }
                    destRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)

                    val finalMask = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
                    val canvas = android.graphics.Canvas(finalMask)
                    canvas.drawBitmap(maskBitmap, srcRect, destRect, null)
                    return@withContext finalMask
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun createImageUri(context: Context): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "fswap_photo_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/FaceSwap"
                )
            }
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveDebugMaskToFiles(context: Context, bitmap: Bitmap) {
        try {
            val debugFile = File(context.filesDir, "mask_debug_${System.currentTimeMillis()}.png")
            FileOutputStream(debugFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveDebugInpaintResult(context: Context, bitmap: Bitmap) {
        try {
            val debugFile = File(context.filesDir, "inpaint_result_${System.currentTimeMillis()}.png")
            FileOutputStream(debugFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveBitmapExact(context: Context, bitmap: Bitmap, filename: String) {
        saveBitmapToCache(context, bitmap, filename)
    }

    // == PATCH: OpenCV locale salva inputcv.png & maskcv.png ==
    fun inpaintWithOpenCV(context: Context, bitmap: Bitmap, maskBitmap: Bitmap): Bitmap? {
        saveBitmapExact(context, bitmap, "inputcv.png")
        saveBitmapExact(context, maskBitmap, "maskcv.png")
        saveDebugMaskToFiles(context, maskBitmap)
        return try {
            val result = OpenCVHelper.inpaint(bitmap, maskBitmap)
            saveDebugInpaintResult(context, result)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sendMaskAndImageToLamaCleaner(
        context: Context,
        image: Bitmap,
        mask: Bitmap,
        onSuccess: (Bitmap) -> Unit,
        onError: (String) -> Unit
    ) {
        saveBitmapExact(context, image, "input.png")
        saveBitmapExact(context, mask, "mask.png")
        onError("Funzione sendMaskAndImageToLamaCleaner non implementata!")
    }

    // PATCH: submit separato da poll per HuggingFace
    fun submitHuggingFaceJob(
        context: Context,
        image: Bitmap,
        mask: Bitmap,
        prompt: String,
        numInferenceSteps: Int = 15,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        // salvataggio immagini in cache
        saveBitmapExact(context, image, "inputh_debug.png")
        saveBitmapExact(context, mask, "maskh_debug.png")

        val ENDPOINT_INPAINT = "https://cantuma1-mynew-inpainting-space.hf.space/custom_inpaint_upload"
        val imageFile = File(context.cacheDir, "inputh.png")
        val maskFile = File(context.cacheDir, "maskh.png")
        FileOutputStream(imageFile).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileOutputStream(maskFile).use { mask.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val reqBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("prompt", prompt)
            .addFormDataPart("num_inference_steps", numInferenceSteps.toString())
            .addFormDataPart(
                "original", "inputh.png",
                imageFile.asRequestBody("image/png".toMediaTypeOrNull())
            )
            .addFormDataPart(
                "mask", "maskh.png",
                maskFile.asRequestBody("image/png".toMediaTypeOrNull())
            )
            .build()
        val client = OkHttpClient()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uploadReq = Request.Builder().url(ENDPOINT_INPAINT).post(reqBody).build()
                val uploadResp = client.newCall(uploadReq).execute()
                val uploadBody = uploadResp.body?.string() ?: ""
                if (!uploadResp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        onError("Errore API: ${uploadResp.message} -- $uploadBody")
                    }
                    return@launch
                }
                val eventId = try {
                    val arr = JSONObject(uploadBody).optJSONArray("data")
                    arr?.optString(2)
                } catch (e: Exception) { null }
                if (eventId.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        onError("Nessun event_id nella risposta: $uploadBody")
                    }
                    return@launch
                }
                withContext(Dispatchers.Main) { onSuccess(eventId) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Eccezione: ${e.message}") }
            } finally {
                try { imageFile.delete() } catch (_: Exception) {}
                try { maskFile.delete() } catch (_: Exception) {}
            }
        }
    }

    fun pollHuggingFaceJobResult(
        context: Context,
        jobId: String,
        onSuccess: (Bitmap) -> Unit,
        onError: (String) -> Unit
    ) {
        val ENDPOINT_POLL = "https://cantuma1-mynew-inpainting-space.hf.space/custom_poll"
        val client = OkHttpClient()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var outputUrl: String? = null
                for (attempt in 0 until 60) { // max 10 min
                    val pollPayload = JSONObject().put("data", JSONArray().put(jobId))
                    val pollReqBody = pollPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                    val pollReq = Request.Builder().url(ENDPOINT_POLL).post(pollReqBody).build()
                    val pollResp = client.newCall(pollReq).execute()
                    val pollBody = pollResp.body?.string() ?: ""
                    if (pollResp.isSuccessful) {
                        val root = JSONObject(pollBody)
                        val status = root.optString("status", "")
                        val url: String? = root.optJSONArray("data")?.optString(1)
                        if (status == "completed" && !url.isNullOrBlank()) {
                            outputUrl = url
                            break
                        } else if (status == "not_found") {
                            withContext(Dispatchers.Main) {
                                onError("Job non trovato: $pollBody")
                            }
                            return@launch
                        }
                    }
                    delay(10_000)
                }
                if (!outputUrl.isNullOrBlank()) {
                    val imgReq = Request.Builder().url(outputUrl!!).get().build()
                    val imgResp = client.newCall(imgReq).execute()
                    if (imgResp.isSuccessful) {
                        val imgBytes = imgResp.body?.bytes()
                        if (imgBytes != null && imgBytes.isNotEmpty()) {
                            val resultBitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                            withContext(Dispatchers.Main) { onSuccess(resultBitmap) }
                            return@launch
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onError("Errore scaricando l'immagine risultante.")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("Timeout: nessun risultato dopo 10 minuti.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Eccezione: ${e.message}") }
            }
        }
    }
}