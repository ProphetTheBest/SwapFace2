package com.example.faceswapapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

private enum class Handle { TopLeft, TopRight, BottomLeft, BottomRight, Center }

@Composable
fun MovableCropBox(
    boxSize: IntSize,
    cropRect: Rect,
    onCropRectChange: (Rect) -> Unit
) {
    val handleRadius = 24f
    var draggingHandle by remember { mutableStateOf<Handle?>(null) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var rectStart by remember { mutableStateOf(cropRect) }

    fun detectHandle(offset: Offset): Handle? {
        val r = cropRect
        val handles = mapOf(
            Handle.TopLeft to Offset(r.left, r.top),
            Handle.TopRight to Offset(r.right, r.top),
            Handle.BottomLeft to Offset(r.left, r.bottom),
            Handle.BottomRight to Offset(r.right, r.bottom),
        )
        handles.entries.forEach { (handle, pos) ->
            if ((offset - pos).getDistance() < handleRadius * 1.2f)
                return handle
        }
        if (offset.x in r.left..r.right && offset.y in r.top..r.bottom)
            return Handle.Center
        return null
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(cropRect) {
                detectDragGestures(
                    onDragStart = { offset: Offset ->
                        draggingHandle = detectHandle(offset)
                        dragStart = offset
                        rectStart = cropRect
                    },
                    onDragEnd = { draggingHandle = null },
                    onDragCancel = { draggingHandle = null },
                    onDrag = { change: PointerInputChange, _ ->
                        val r = rectStart
                        val dx = change.position.x - dragStart.x
                        val dy = change.position.y - dragStart.y
                        when (draggingHandle) {
                            Handle.TopLeft -> {
                                val newLeft = (r.left + dx).coerceIn(0f, r.right - 50f)
                                val newTop = (r.top + dy).coerceIn(0f, r.bottom - 50f)
                                onCropRectChange(Rect(newLeft, newTop, r.right, r.bottom))
                            }
                            Handle.TopRight -> {
                                val newRight = (r.right + dx).coerceIn(r.left + 50f, boxSize.width.toFloat())
                                val newTop = (r.top + dy).coerceIn(0f, r.bottom - 50f)
                                onCropRectChange(Rect(r.left, newTop, newRight, r.bottom))
                            }
                            Handle.BottomLeft -> {
                                val newLeft = (r.left + dx).coerceIn(0f, r.right - 50f)
                                val newBottom = (r.bottom + dy).coerceIn(r.top + 50f, boxSize.height.toFloat())
                                onCropRectChange(Rect(newLeft, r.top, r.right, newBottom))
                            }
                            Handle.BottomRight -> {
                                val newRight = (r.right + dx).coerceIn(r.left + 50f, boxSize.width.toFloat())
                                val newBottom = (r.bottom + dy).coerceIn(r.top + 50f, boxSize.height.toFloat())
                                onCropRectChange(Rect(r.left, r.top, newRight, newBottom))
                            }
                            Handle.Center -> {
                                val w = r.width
                                val h = r.height
                                val newLeft = (r.left + dx).coerceIn(0f, boxSize.width - w)
                                val newTop = (r.top + dy).coerceIn(0f, boxSize.height - h)
                                onCropRectChange(Rect(newLeft, newTop, newLeft + w, newTop + h))
                            }
                            null -> Unit
                        }
                    }
                )
            }
    ) {
        drawRect(
            color = Color.Red,
            topLeft = Offset(cropRect.left, cropRect.top),
            size = androidx.compose.ui.geometry.Size(cropRect.width, cropRect.height),
            style = Stroke(width = 3.dp.toPx())
        )
        drawCircle(
            color = Color.Red,
            radius = handleRadius,
            center = Offset(cropRect.left, cropRect.top)
        )
        drawCircle(
            color = Color.Red,
            radius = handleRadius,
            center = Offset(cropRect.right, cropRect.top)
        )
        drawCircle(
            color = Color.Red,
            radius = handleRadius,
            center = Offset(cropRect.left, cropRect.bottom)
        )
        drawCircle(
            color = Color.Red,
            radius = handleRadius,
            center = Offset(cropRect.right, cropRect.bottom)
        )
    }
}