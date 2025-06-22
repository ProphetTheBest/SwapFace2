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
    brushPathList: List<Pair<List<Offset>, Float>>,
    onPathAdded: (List<Offset>, Float) -> Unit,
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onImageBoxChanged: (ox: Float, oy: Float, w: Float, h: Float) -> Unit,
    imageOffset: Offset,
    imageSize: IntSize,
    bitmapWidth: Int,
    bitmapHeight: Int,
    modifier: Modifier = Modifier
) {
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Calcola il rettangolo effettivo dell'immagine disegnata nel canvas/pannello
    val panelW = imageSize.width.toFloat()
    val panelH = imageSize.height.toFloat()
    val bmpW = bitmapWidth.toFloat()
    val bmpH = bitmapHeight.toFloat()
    val scale = minOf(panelW / bmpW, panelH / bmpH)
    val drawnW = bmpW * scale
    val drawnH = bmpH * scale
    val left = (panelW - drawnW) / 2f
    val top = (panelH - drawnH) / 2f
    val right = left + drawnW
    val bottom = top + drawnH

    // Notifica sempre la size del canvas e i parametri immagine (blindato)
    LaunchedEffect(imageSize) {
        onCanvasSizeChanged(imageSize)
        onImageBoxChanged(left, top, drawnW, drawnH)
    }

    // Helper per confinare le path all'immagine effettiva
    fun confineToRect(points: List<Offset>): List<Offset> =
        points.filter { it.x in left..right && it.y in top..bottom }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(brushSize, imageOffset, imageSize, bitmapWidth, bitmapHeight) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentStroke =
                            if (offset.x in left..right && offset.y in top..bottom)
                                listOf(offset) else emptyList()
                    },
                    onDrag = { change, _ ->
                        val pos = change.position
                        if (pos.x in left..right && pos.y in top..bottom) {
                            currentStroke = currentStroke + pos
                        }
                    },
                    onDragEnd = {
                        val filtered = confineToRect(currentStroke)
                        if (filtered.size >= 2) onPathAdded(filtered, brushSize)
                        currentStroke = emptyList()
                    },
                    onDragCancel = { currentStroke = emptyList() }
                )
            }
    ) {
        // DEBUG: rettangolo rosso dove il pennello PUÒ disegnare
        drawRect(
            color = Color.Red,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(drawnW, drawnH),
            style = Stroke(width = 3f)
        )

        // Path completate
        brushPathList.forEach { (points, thickness) ->
            val filtered = confineToRect(points)
            if (filtered.size >= 2) {
                val path = Path().apply {
                    moveTo(filtered[0].x, filtered[0].y)
                    filtered.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = Color.Green.copy(alpha = 0.5f),
                    style = Stroke(width = thickness)
                )
            }
        }
        // Path corrente
        val filteredCurrent = confineToRect(currentStroke)
        if (filteredCurrent.size >= 2) {
            val path = Path().apply {
                moveTo(filteredCurrent[0].x, filteredCurrent[0].y)
                filteredCurrent.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = path,
                color = Color.Green,
                style = Stroke(width = brushSize)
            )
        }
    }
}