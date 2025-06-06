package com.example.faceswapapp.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
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
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

// --- ML Kit imports per segmentazione persona ---
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.segmentation.Segmentation

import android.media.ExifInterface // <-- Import per gestione EXIF

// --- IMPORT OPENCV HELPER --- (aggiunto per inpainting reale)
import com.example.faceswapapp.OpenCVHelper

object ImageUtils {

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
        personBitmap: Bitmap,
        backgroundBitmap: Bitmap
    ): Bitmap {
        val bgResized = Bitmap.createScaledBitmap(backgroundBitmap, personBitmap.width, personBitmap.height, true)
        val result = Bitmap.createBitmap(personBitmap.width, personBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        canvas.drawBitmap(bgResized, 0f, 0f, null)
        canvas.drawBitmap(personBitmap, 0f, 0f, null)
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
                if (mask != null) {
                    val width = bitmap.width
                    val height = bitmap.height
                    val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val maskPixels = FloatArray(width * height)
                    mask.rewind()
                    for (i in maskPixels.indices) {
                        maskPixels[i] = mask.float
                    }
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            val alpha = (maskPixels[y * width + x] * 255).toInt().coerceIn(0, 255)
                            val rgb = bitmap.getPixel(x, y)
                            maskBitmap.setPixel(x, y, (alpha shl 24) or (rgb and 0x00FFFFFF))
                        }
                    }
                    maskBitmap
                } else null
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

    fun inpaintWithOpenCV(context: Context, bitmap: Bitmap, maskBitmap: Bitmap): Bitmap? {
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

    fun inpaintMaskArea(context: Context, bitmap: Bitmap, maskBitmap: Bitmap): Bitmap? =
        inpaintWithOpenCV(context, bitmap, maskBitmap)

    /**
     * Invio maschera e immagine a Lama Cleaner (AI REST API).
     * Sostituisci con la tua implementazione reale.
     */
    fun sendMaskAndImageToLamaCleaner(
        context: Context,
        image: Bitmap,
        mask: Bitmap,
        onSuccess: (Bitmap) -> Unit,
        onError: (String) -> Unit
    ) {
        // TODO: Implementa la chiamata REST a Lama Cleaner e chiama onSuccess/onError
        // Per ora stub che ritorna errore:
        onError("Funzione sendMaskAndImageToLamaCleaner non implementata!")
        // Per test: puoi usare onSuccess(image) per vedere la pipeline funzionare
        // onSuccess(image)
    }

    /**
     * Invio maschera e immagine a HuggingFace Inpainting (AI REST API).
     * Sostituisci con la tua implementazione reale.
     */
    fun sendMaskAndImageToHuggingFaceInpainting(
        context: Context,
        image: Bitmap,
        mask: Bitmap,
        prompt: String,
        numInferenceSteps: Int,
        guidanceScale: Int,
        onSuccess: (Bitmap) -> Unit,
        onError: (String) -> Unit
    ) {
        // TODO: Implementa la chiamata REST a HuggingFace Inpainting e chiama onSuccess/onError
        onError("Funzione sendMaskAndImageToHuggingFaceInpainting non implementata!")
        // Per test: puoi usare onSuccess(image)
        // onSuccess(image)
    }
}