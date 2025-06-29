package com.example.faceswapapp.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import com.example.faceswapapp.utils.toAndroidPath

object BrushMaskOverlayHelper {
    /**
     * Genera la bitmap della maschera a partire dai tratti pennello,
     * assumendo che i punti siano già in coordinate bitmap.
     * Non effettua alcuna conversione/scalatura.
     */
    fun generateMaskBitmap(
        imageWidth: Int,
        imageHeight: Int,
        brushPathList: List<Pair<List<Offset>, Float>>
    ): Bitmap {
        val maskBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(maskBitmap)
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        brushPathList.forEach { (points, thickness) ->
            if (points.size >= 2) {
                val androidPath = points.toAndroidPath()
                paint.strokeWidth = thickness
                canvas.drawPath(androidPath, paint)
            }
        }
        return maskBitmap
    }
}