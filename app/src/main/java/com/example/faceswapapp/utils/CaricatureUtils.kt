package com.example.faceswapapp.utils

import android.graphics.Bitmap
import kotlin.math.*

data class CaricatureCenter(
    val x: Float,
    val y: Float,
    val radius: Float,
    val strength: Float
)

fun applyCaricatureMultiCenter(
    bitmap: Bitmap,
    centers: List<CaricatureCenter>
): Bitmap {
    println("applyCaricatureMultiCenter: chiamo con centri $centers")
    val width = bitmap.width
    val height = bitmap.height
    val result = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val resultPixels = IntArray(width * height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            // Distorsione forzata per test
            var srcX = x.toFloat()
            var srcY = y.toFloat()
            for (center in centers) {
                val dx = x - center.x
                val dy = y - center.y
                val distance = sqrt(dx * dx + dy * dy)
                if (distance < center.radius) {
                    val r = distance / center.radius
                    val theta = atan2(dy, dx)
                    val rn = r.toDouble().pow(1.0 - center.strength.toDouble()) // FORZA distorsione
                    val newX = center.x + rn * center.radius * cos(theta)
                    val newY = center.y + rn * center.radius * sin(theta)
                    srcX = newX.toFloat()
                    srcY = newY.toFloat()
                }
            }
            val finalX = srcX.roundToInt().coerceIn(0, width - 1)
            val finalY = srcY.roundToInt().coerceIn(0, height - 1)
            resultPixels[y * width + x] = pixels[finalY * width + finalX]
        }
    }
    // Piccolo test: inverti la bitmap ogni volta!
    // for (i in resultPixels.indices) resultPixels[i] = pixels[pixels.size - 1 - i]
    result.setPixels(resultPixels, 0, width, 0, 0, width, height)
    return result
}