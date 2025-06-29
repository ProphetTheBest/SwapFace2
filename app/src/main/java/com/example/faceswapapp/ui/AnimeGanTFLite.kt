package com.example.faceswapapp.ui

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import android.util.Log

class AnimeGanTFLite(context: Context, modelName: String) {
    private val interpreter: Interpreter

    init {
        val assetFileDescriptor = context.assets.openFd(modelName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        fileInputStream.channel.position(assetFileDescriptor.startOffset)
        val modelBuffer = fileInputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
        interpreter = Interpreter(modelBuffer)
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

        val input = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        input.order(ByteOrder.nativeOrder())
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