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

        val panelW = canvasSize.width.toFloat()
        val panelH = canvasSize.height.toFloat()
        val bmpW = imageWidth.toFloat()
        val bmpH = imageHeight.toFloat()
        val scale = minOf(panelW / bmpW, panelH / bmpH)
        val drawnW = bmpW * scale
        val drawnH = bmpH * scale
        val left = (panelW - drawnW) / 2f
        val top = (panelH - drawnH) / 2f

        fun mapCanvasPointToBitmap(pt: Offset): Offset? {
            if (pt.x !in left..(left + drawnW) || pt.y !in top..(top + drawnH)) return null
            val xNorm = (pt.x - left) / drawnW
            val yNorm = (pt.y - top) / drawnH
            return Offset(
                x = xNorm * bmpW,
                y = yNorm * bmpH
            )
        }

        brushPathList.forEach { (points, thickness) ->
            val mappedPoints = points.mapNotNull { pt -> mapCanvasPointToBitmap(pt) }
            if (mappedPoints.size >= 2) {
                val androidPath = mappedPoints.toAndroidPath()
                val thicknessOnBitmap = thickness * (bmpW / drawnW)
                paint.strokeWidth = thicknessOnBitmap
                canvas.drawPath(androidPath, paint)
            }
        }
        return maskBitmap
    }
}