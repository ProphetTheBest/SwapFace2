package com.example.faceswapapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import com.example.faceswapapp.ui.AnimeGanTFLite
import com.example.faceswapapp.ui.adjustSaturation

/**
 * Classe AnimeGanTFLite patchata per accettare il nome del modello come parametro.
 */
class AnimeGanTFLite(context: Context, modelName: String) {
    private val interpreter: org.tensorflow.lite.Interpreter

    init {
        val assetFileDescriptor = context.assets.openFd(modelName)
        val fileInputStream = java.io.FileInputStream(assetFileDescriptor.fileDescriptor)
        fileInputStream.channel.position(assetFileDescriptor.startOffset)
        val modelBuffer = fileInputStream.channel.map(
            java.nio.channels.FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
        interpreter = org.tensorflow.lite.Interpreter(modelBuffer)
    }

    fun runOn(bitmap: Bitmap): Bitmap? {
        // Aspettati bitmap quadrato!
        if (bitmap.width != bitmap.height) {
            Log.w("AnimeGanTFLite", "ATTENZIONE: la bitmap passata NON è quadrata!")
        }
        val inputSize = 512
        // Ridimensiona se serve
        val resized = if (bitmap.width != inputSize || bitmap.height != inputSize) {
            Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        } else {
            bitmap
        }

        val input = java.nio.ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        input.order(java.nio.ByteOrder.nativeOrder())
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = resized.getPixel(x, y)
                input.putFloat(((pixel shr 16 and 0xFF) / 127.5f) - 1.0f) // R
                input.putFloat(((pixel shr 8 and 0xFF) / 127.5f) - 1.0f)  // G
                input.putFloat(((pixel and 0xFF) / 127.5f) - 1.0f)        // B
            }
        }
        input.rewind()

        val output = Array(1) { Array(inputSize) { Array(inputSize) { FloatArray(3) } } }
        try {
            interpreter.run(input, output)
        } catch (e: Exception) {
            Log.e("AnimeGanTFLite", "Errore TFLite: ${e.message}", e)
            return null
        }

        val result = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val r = ((output[0][y][x][0] + 1.0f) * 127.5f).toInt().coerceIn(0, 255)
                val g = ((output[0][y][x][1] + 1.0f) * 127.5f).toInt().coerceIn(0, 255)
                val b = ((output[0][y][x][2] + 1.0f) * 127.5f).toInt().coerceIn(0, 255)
                result.setPixel(x, y, (0xFF shl 24) or (r shl 16) or (g shl 8) or b)
            }
        }
        return result
    }
}

/**
 * Tutte le funzioni seguenti accettano il parametro modelName (default: FacePaint v2)
 */

fun applyAnimeGAN(
    bitmap: Bitmap,
    context: Context,
    modelName: String = "face_paint_512_v2_tf_nhwc_inout.tflite"
): Bitmap {
    val origWidth = bitmap.width
    val origHeight = bitmap.height
    val resized = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
    val animeGanResult: Bitmap? = AnimeGanTFLite(context, modelName).runOn(resized)
    if (animeGanResult == null) return bitmap
    return Bitmap.createScaledBitmap(animeGanResult, origWidth, origHeight, true)
}

/**
 * Applica AnimeGAN2 preservando dimensioni e proporzioni dell'immagine originale.
 * Solo la parte centrale risulterà in stile anime.
 */
fun applyAnimeGanPreservingSize(
    bitmap: Bitmap,
    context: Context,
    modelName: String = "face_paint_512_v2_tf_nhwc_inout.tflite"
): Bitmap {
    val origWidth = bitmap.width
    val origHeight = bitmap.height
    val cropSize = minOf(origWidth, origHeight)
    val cropX = (origWidth - cropSize) / 2
    val cropY = (origHeight - cropSize) / 2
    // Crop centrale quadrato
    val squareCrop = Bitmap.createBitmap(bitmap, cropX, cropY, cropSize, cropSize)
    // Filtro AnimeGAN su crop
    val animeGanResult: Bitmap? = AnimeGanTFLite(context, modelName).runOn(squareCrop)
    if (animeGanResult == null) return bitmap
    // (opzionale) smoothing & saturation
    var result = animeGanResult
    result = adjustSaturation(result, 0.85f)
    // Ridimensiona il risultato alle dimensioni del crop centrale
    result = Bitmap.createScaledBitmap(result, cropSize, cropSize, true)
    // Ricostruisci la foto finale: copia l’originale e inserisci il centro anime
    val finalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(finalBitmap)
    canvas.drawBitmap(result, cropX.toFloat(), cropY.toFloat(), null)
    return finalBitmap
}

fun applyFullAnimeGan(
    bitmap: Bitmap,
    context: Context,
    modelName: String = "face_paint_512_v2_tf_nhwc_inout.tflite"
): Bitmap {
    val origWidth = bitmap.width
    val origHeight = bitmap.height
    val resized = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
    val animeGanResult = AnimeGanTFLite(context, modelName).runOn(resized) ?: return bitmap
    return Bitmap.createScaledBitmap(animeGanResult, origWidth, origHeight, true)
}

fun applyFullFrameAnimeGan(
    bitmap: Bitmap,
    context: Context,
    modelName: String = "face_paint_512_v2_tf_nhwc_inout.tflite"
): Bitmap {
    val origWidth = bitmap.width
    val origHeight = bitmap.height
    val resized = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
    val animeGanResult = AnimeGanTFLite(context, modelName).runOn(resized) ?: return bitmap
    return Bitmap.createScaledBitmap(animeGanResult, origWidth, origHeight, true)
}