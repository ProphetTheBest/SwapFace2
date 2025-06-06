package com.example.faceswapapp.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.faceswapapp.utils.FilterType

fun applyFilter(bitmap: Bitmap, type: FilterType, saturation: Float): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val filteredBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(filteredBitmap)
    val paint = Paint()

    when (type) {
        FilterType.BlackWhite -> {
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            paint.colorFilter = ColorMatrixColorFilter(matrix)
        }
        FilterType.Saturation -> {
            val matrix = ColorMatrix()
            matrix.setSaturation(saturation)
            paint.colorFilter = ColorMatrixColorFilter(matrix)
        }
        FilterType.Vintage -> {
            // semplice effetto "vintage": abbassa la saturazione, aumenta un po' il rosso
            val matrix = ColorMatrix(
                floatArrayOf(
                    1.2f, 0f, 0f, 0f, 30f,
                    0f, 1f, 0f, 0f, 10f,
                    0f, 0f, 0.8f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            paint.colorFilter = ColorMatrixColorFilter(matrix)
        }
        else -> {
            paint.colorFilter = null
        }
    }
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return filteredBitmap
}