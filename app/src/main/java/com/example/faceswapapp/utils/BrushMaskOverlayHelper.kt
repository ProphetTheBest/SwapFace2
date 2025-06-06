package com.example.faceswapapp.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.example.faceswapapp.utils.toAndroidPath

object BrushMaskOverlayHelper {
    fun generateMaskBitmap(
        imageWidth: Int,
        imageHeight: Int,
        brushPathList: List<Pair<List<Offset>, Float>>,
        canvasSize: IntSize,
        imageOffset: Pair<Float, Float>,
        imageSize: Pair<Float, Float>
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
        val scaleX = imageWidth / imageSize.first
        val scaleY = imageHeight / imageSize.second
        brushPathList.forEach { (points, thickness) ->
            // Scala i punti e crea la Path
            val scaledPoints = points.map { pt ->
                Offset(
                    (pt.x - imageOffset.first) * scaleX,
                    (pt.y - imageOffset.second) * scaleY
                )
            }
            val androidPath = scaledPoints.toAndroidPath()
            paint.strokeWidth = thickness * scaleX
            canvas.drawPath(androidPath, paint)
        }
        return maskBitmap
    }
}