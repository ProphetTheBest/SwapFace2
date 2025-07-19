package com.example.faceswapapp.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.example.faceswapapp.*
import com.example.faceswapapp.ui.getStickerMatrixNoScale
import java.io.File
import kotlinx.coroutines.launch

class FaceSwapViewModel : ViewModel() {
    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set
    var detectedLandmarks by mutableStateOf<List<List<Offset>>?>(null)
        private set
    var currentBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var currentFaceIndex by mutableStateOf(-1)
        private set
    var bitmapVersion by mutableStateOf(0)
        private set
    var isProcessing by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var facePreviews by mutableStateOf<List<Bitmap>?>(null)
        private set
    var showLandmarks by mutableStateOf(true)
        private set

    var placedStickers by mutableStateOf<List<PlacedSticker>>(listOf())

    var selectedStickerIndex by mutableStateOf<Int?>(null)
        private set

    var jobQueue by mutableStateOf<List<String>>(emptyList())
        private set

    // --- Sticker logic ---
    fun addSticker(sticker: Sticker) {
        placedStickers = placedStickers + PlacedSticker(sticker)
        selectedStickerIndex = placedStickers.lastIndex
    }
    fun selectSticker(idx: Int) {
        selectedStickerIndex = idx
        placedStickers = placedStickers.mapIndexed { i, s -> s.copy(isSelected = i == idx) }
    }
    fun removeSticker(idx: Int) {
        placedStickers = placedStickers.filterIndexed { i, _ -> i != idx }
        selectedStickerIndex = if (placedStickers.isNotEmpty()) {
            (if (idx == 0) 0 else idx - 1).coerceAtLeast(0)
        } else null
    }
    fun updateSelectedSticker(x: Float? = null, y: Float? = null, scale: Float? = null, rot: Float? = null) {
        selectedStickerIndex?.let { idx ->
            placedStickers = placedStickers.mapIndexed { i, s ->
                if (i == idx) s.copy(
                    x = x ?: s.x,
                    y = y ?: s.y,
                    scale = scale ?: s.scale,
                    rotation = rot ?: s.rotation
                ) else s
            }
        }
    }

    fun showLastDebugImage(context: Context, snackbarHostState: SnackbarHostState? = null) {
        val debugBitmap = com.example.faceswapapp.utils.ImageUtils.loadLastDebugStep6Bitmap(
            context,
            currentBitmap?.width ?: 300,
            currentBitmap?.height ?: 300
        )
        if (debugBitmap != null) {
            updateBitmap(debugBitmap)
            bitmapVersion++
            updateDetectedLandmarks(null)
            updateFacePreviews(null)
            placedStickers = emptyList<PlacedSticker>()
            selectedStickerIndex = null
            snackbarHostState?.let {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    it.showSnackbar("Immagine di debug caricata.")
                }
            }
        } else {
            snackbarHostState?.let {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    it.showSnackbar("Nessuna immagine di debug trovata")
                }
            }
        }
    }

    fun saveCurrentToGallery(context: Context, snackbarHostState: SnackbarHostState? = null) {
        val bitmap = currentBitmap
        if (bitmap != null) {
            val merged = mergeBitmapWithStickers(
                bitmap,
                placedStickers,
                detectedLandmarks?.getOrNull(currentFaceIndex),
                context
            )
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                com.example.faceswapapp.utils.ImageUtils.saveToGallery(context, merged) { ok ->
                    snackbarHostState?.let {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            it.showSnackbar(
                                if (ok) "Immagine con sticker salvata in Galleria!" else "Errore nel salvataggio!"
                            )
                        }
                    }
                }
            }
        }
    }

    fun resetSelectedSticker() {
        selectedStickerIndex?.let { idx ->
            placedStickers = placedStickers.mapIndexed { i, s ->
                if (i == idx) s.copy(x = 0f, y = 0f, scale = 1f, rotation = 0f) else s
            }
        }
    }

    fun updateShowLandmarks(show: Boolean) {
        showLandmarks = show
    }

    fun updateError(message: String?) {
        errorMessage = message
    }

    fun fullReset() {
        selectedImageUri = null
        detectedLandmarks = null
        currentBitmap = null
        currentFaceIndex = -1
        errorMessage = null
        bitmapVersion++
        isProcessing = false
        facePreviews = null
        placedStickers = emptyList<PlacedSticker>()
        selectedStickerIndex = null
        showLandmarks = true
        jobQueue = emptyList()
    }

    fun clearDebugImages(context: Context) {
        val filesDir = context.filesDir
        val files = filesDir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.name.startsWith("debug_") && file.name.endsWith(".png")) {
                    file.delete()
                }
            }
        }
        val logFile = File(filesDir, "face_swap_debug.txt")
        logFile.delete()
    }

    fun mergeBitmapWithStickers(
        baseBitmap: Bitmap,
        stickers: List<PlacedSticker>,
        landmarks: List<Offset>?,
        context: Context
    ): Bitmap {
        val result = Bitmap.createBitmap(baseBitmap.width, baseBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(baseBitmap, 0f, 0f, null)
        if (landmarks == null || landmarks.isEmpty()) {
            stickers.forEach { sticker ->
                val stickerBitmap = BitmapFactory.decodeResource(context.resources, sticker.sticker.resId)
                val centerX = baseBitmap.width / 2f + sticker.x
                val centerY = baseBitmap.height / 2f + sticker.y
                val matrix = Matrix().apply {
                    postTranslate(-stickerBitmap.width / 2f, -stickerBitmap.height / 2f)
                    postScale(sticker.scale, sticker.scale)
                    postRotate(sticker.rotation)
                    postTranslate(centerX, centerY)
                }
                canvas.drawBitmap(stickerBitmap, matrix, null)
            }
            return result
        }
        val stickerBitmaps = stickers.associate { it.sticker.resId to
                BitmapFactory.decodeResource(context.resources, it.sticker.resId)
        }
        stickers.forEach { placed ->
            val stickerBitmap = stickerBitmaps[placed.sticker.resId] ?: return@forEach
            val matrix = getStickerMatrixNoScale(
                sticker = placed.sticker,
                bitmap = stickerBitmap,
                landmarks = landmarks,
                offsetX = placed.x,
                offsetY = placed.y,
                userScale = placed.scale,
                userRotation = placed.rotation
            )
            canvas.drawBitmap(stickerBitmap, matrix, null)
        }
        return result
    }

    fun updateBitmap(bitmap: Bitmap?) {
        currentBitmap = bitmap
    }
    fun updateDetectedLandmarks(landmarks: List<List<Offset>>?) {
        detectedLandmarks = landmarks
    }
    fun updateCurrentFaceIndex(idx: Int) {
        currentFaceIndex = idx
    }
    fun updateFacePreviews(bmps: List<Bitmap>?) {
        facePreviews = bmps
    }
    fun updateIsProcessing(b: Boolean) {
        isProcessing = b
    }
    fun updateSelectedImageUri(uri: Uri?) {
        selectedImageUri = uri
    }
    fun updateStickerIndex(idx: Int?) {
        selectedStickerIndex = idx
    }

    fun startFaceSwapPipeline(context: Context) {
        val uri = selectedImageUri ?: return
        isProcessing = true
        errorMessage = null
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap == null) {
                errorMessage = "Immagine non valida o non caricata"
                isProcessing = false
                return
            }
            currentBitmap = bitmap

            detectedLandmarks = detectFakeLandmarks(bitmap)
            currentFaceIndex = if (detectedLandmarks?.isNotEmpty() == true) 0 else -1

            jobQueue = jobQueue + "Job at ${System.currentTimeMillis()}"

        } catch (e: Exception) {
            errorMessage = "Errore durante il caricamento: ${e.localizedMessage}"
        } finally {
            isProcessing = false
        }
    }

    suspend fun handleSwapFace(context: Context, uri: Uri) {
        clearDebugImages(context)
        fullReset()
        updateSelectedImageUri(uri)
        updateIsProcessing(true)
        val loaded = com.example.faceswapapp.utils.ImageUtils.loadBitmapFromUri(context, uri)
        updateBitmap(loaded)
        if (loaded != null) {
            com.example.faceswapapp.utils.ImageUtils.detectFaceLandmarksFromUri(context, uri) { landmarks ->
                updateDetectedLandmarks(landmarks)
                updateCurrentFaceIndex(-1)
                updateIsProcessing(false)
                if (!landmarks.isNullOrEmpty()) {
                    updateFacePreviews(landmarks.map { lmk ->
                        com.example.faceswapapp.utils.ImageUtils.cropFaceFromLandmarks(loaded, lmk)
                    })
                    updateError(null)
                } else {
                    updateFacePreviews(null)
                    updateError("""
                    Nessun volto rilevato nell'immagine.
                    Suggerimenti:
                    - Assicurati che il volto sia visibile e frontale.
                    - Evita occhiali grandi, cappelli o oggetti che coprono il viso.
                    - Usa immagini ben illuminate e non troppo scure o sfocate.
                    - Prova a cambiare immagine o scattarne una nuova.
                """.trimIndent())
                }
                placedStickers = emptyList<PlacedSticker>()
                selectedStickerIndex = null
            }
        } else {
            updateError("Errore nel caricamento dell'immagine.")
            updateIsProcessing(false)
        }
    }

    suspend fun handleFaceSwapOnFace(
        context: Context,
        uri: Uri,
        snackbarHostState: SnackbarHostState? = null
    ) {
        val baseBitmap = currentBitmap
        val landmarksAll = detectedLandmarks
        val faceIndex = currentFaceIndex
        if (baseBitmap == null || landmarksAll == null || faceIndex < 0 || isProcessing) {
            updateError("Errore: bitmap di base o landmarks non presenti.")
            bitmapVersion++
            return
        }
        updateIsProcessing(true)
        val faceBitmap = com.example.faceswapapp.utils.ImageUtils.loadBitmapFromUri(context, uri)
        if (faceBitmap != null) {
            val sourceLandmarksRaw = com.example.faceswapapp.utils.ImageUtils.detectLandmarksForFace(context, faceBitmap)
            val targetLandmarksRaw = landmarksAll.getOrNull(faceIndex)
            val sourceLandmarks468 = sourceLandmarksRaw.take(468)
            val targetLandmarks468 = targetLandmarksRaw?.take(468)
            if (
                sourceLandmarks468.size == 468 &&
                targetLandmarks468 != null &&
                targetLandmarks468.size == 468
            ) {
                val scaleX = baseBitmap.width.toFloat() / faceBitmap.width
                val scaleY = baseBitmap.height.toFloat() / faceBitmap.height
                val scaledSourceLandmarks = sourceLandmarks468.map { orig ->
                    androidx.compose.ui.geometry.Offset(orig.x * scaleX, orig.y * scaleY)
                }
                val targetLandmarksPoints = FaceSwapUtils.offsetsToPoints(targetLandmarks468)
                val sourceLandmarksPoints = FaceSwapUtils.offsetsToPoints(scaledSourceLandmarks)

                val imgW = baseBitmap.width.toDouble()
                val imgH = baseBitmap.height.toDouble()
                val filteredTargetPoints = targetLandmarksPoints.filter {
                    it.x.toDouble() in 0.0..imgW && it.y.toDouble() in 0.0..imgH
                }
                if (filteredTargetPoints.size < 3) {
                    updateError("Landmark non validi per la triangolazione. Riprova con un'altra immagine o assicurati che il volto sia ben visibile.")
                    updateIsProcessing(false)
                    bitmapVersion++
                    return
                }
                val triangles = try {
                    FaceSwapUtils.calculateDelaunayTriangles(baseBitmap.width, baseBitmap.height, filteredTargetPoints)
                } catch (e: Exception) {
                    updateError("Errore durante la triangolazione del volto (OpenCV): landmark fuori range. Prova con un'altra immagine.")
                    updateIsProcessing(false)
                    bitmapVersion++
                    return
                }

                val hullPoints = org.opencv.core.MatOfPoint(
                    *filteredTargetPoints.map { org.opencv.core.Point(it.x.toDouble(), it.y.toDouble()) }.toTypedArray()
                )
                val hullIndices = org.opencv.core.MatOfInt()
                org.opencv.imgproc.Imgproc.convexHull(hullPoints, hullIndices)
                val hullList = hullIndices.toArray().map { hullPoints.toArray()[it] }
                val hullMat = org.opencv.core.MatOfPoint(*hullList.toTypedArray())

                val debugBitmap = com.example.faceswapapp.utils.ImageUtils.loadLastDebugStep6Bitmap(
                    context, baseBitmap.width, baseBitmap.height
                )
                if (debugBitmap != null) {
                    updateBitmap(debugBitmap)
                } else {
                    val swapped = FaceSwapUtils.swapFaceWithTriangles(
                        source = faceBitmap,
                        dest = baseBitmap,
                        sourceLandmarks = sourceLandmarksPoints,
                        destLandmarks = filteredTargetPoints,
                        triangles = triangles,
                        maskHull = hullMat,
                        context = context
                    )
                    updateBitmap(android.graphics.Bitmap.createBitmap(swapped))
                }
                updateDetectedLandmarks(null)
                updateCurrentFaceIndex(-1)
                placedStickers = emptyList<PlacedSticker>()
                selectedStickerIndex = null
                showLandmarks = true
                updateError(null)
                bitmapVersion++
                updateFacePreviews(null)
            } else {
                updateError("""
                Landmark insufficienti per lo swap.
                Prova con immagini in cui il volto sia più visibile e frontale.
            """.trimIndent())
                bitmapVersion++
            }
        } else {
            updateError("Errore durante il caricamento dell'immagine per il face swap.")
            bitmapVersion++
        }
        updateIsProcessing(false)
    }

    private fun detectFakeLandmarks(bitmap: Bitmap): List<List<Offset>> {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        return listOf(
            listOf(
                Offset(w * 0.3f, h * 0.3f),
                Offset(w * 0.7f, h * 0.3f),
                Offset(w * 0.5f, h * 0.7f)
            )
        )
    }
}