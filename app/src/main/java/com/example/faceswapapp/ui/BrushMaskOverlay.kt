package com.example.faceswapapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun BrushMaskOverlay(
    imageBitmap: androidx.compose.ui.graphics.ImageBitmap,
    brushPathList: List<Pair<List<Offset>, Float>>, // PATCHED: lista punti, non Path
    onPathAdded: (List<Offset>, Float) -> Unit,     // PATCHED: lista punti, non Path
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onImageBoxChanged: (ox: Float, oy: Float, w: Float, h: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(brushSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentStroke = listOf(offset)
                    },
                    onDrag = { change, _ ->
                        currentStroke = currentStroke + change.position
                    },
                    onDragEnd = {
                        if (currentStroke.isNotEmpty()) {
                            onPathAdded(currentStroke, brushSize)
                        }
                        currentStroke = emptyList()
                    },
                    onDragCancel = { currentStroke = emptyList() }
                )
            }
    ) {
        // Disegna immagine
        drawImage(imageBitmap)
        // Disegna le path pennello già completate
        brushPathList.forEach { (points, thickness) ->
            if (points.isNotEmpty()) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = Color.Green.copy(alpha = 0.5f),
                    style = Stroke(width = thickness)
                )
            }
        }
        // Disegna la path corrente
        if (currentStroke.isNotEmpty()) {
            val path = Path().apply {
                moveTo(currentStroke[0].x, currentStroke[0].y)
                currentStroke.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = path,
                color = Color.Green,
                style = Stroke(width = brushSize)
            )
        }
    }
}