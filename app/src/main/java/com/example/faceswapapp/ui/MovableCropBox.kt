package com.example.faceswapapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.max
import kotlin.math.min

private enum class Handle { TopLeft, TopRight, BottomLeft, BottomRight, Center }

private fun safeRect(left: Float, top: Float, right: Float, bottom: Float): Rect {
    return Rect(
        left = min(left, right),
        top = min(top, bottom),
        right = max(left, right),
        bottom = max(top, bottom)
    )
}

private fun safeCoerceIn(value: Float, min: Float, max: Float): Float {
    return if (min > max) min else value.coerceIn(min, max)
}

private fun Offset.toIntOffset(): IntOffset = IntOffset(x.toInt(), y.toInt())

@Composable
fun MovableCropBox(
    boxSize: IntSize,
    cropRect: Rect,
    imageOffset: Offset,
    onCropRectFinal: (Rect) -> Unit // chiamato a fine drag per notificare il VM
) {
    val handleRadius = 24f

    // Stato locale per crop fluido
    var localCropRect by remember { mutableStateOf(cropRect) }
    var draggingHandle by remember { mutableStateOf<Handle?>(null) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var rectStart by remember { mutableStateOf(cropRect) }

    // Se cropRect esterno cambia (es. reset/apply crop), aggiorna quello locale
    LaunchedEffect(cropRect) {
        localCropRect = cropRect
    }

    fun detectHandle(offset: Offset): Handle? {
        val r = localCropRect
        val handles = mapOf(
            Handle.TopLeft to Offset(r.left, r.top),
            Handle.TopRight to Offset(r.right, r.top),
            Handle.BottomLeft to Offset(r.left, r.bottom),
            Handle.BottomRight to Offset(r.right, r.bottom)
        )
        handles.entries.forEach { (handle, pos) ->
            if ((offset - pos).getDistance() < handleRadius * 1.5f)
                return handle
        }
        if (offset.x in r.left..r.right && offset.y in r.top..r.bottom)
            return Handle.Center
        return null
    }

    Box(
        modifier = Modifier
            .offset { imageOffset.toIntOffset() }
            .size(boxSize.width.dp, boxSize.height.dp)
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) { // Unit: non resettare la gesture ogni frame!
                    detectDragGestures(
                        onDragStart = { globalOffset ->
                            val offset = globalOffset - imageOffset
                            draggingHandle = detectHandle(offset)
                            dragStart = offset
                            rectStart = localCropRect
                        },
                        onDragEnd = {
                            draggingHandle = null
                            onCropRectFinal(localCropRect)
                        },
                        onDragCancel = {
                            draggingHandle = null
                            onCropRectFinal(localCropRect)
                        },
                        onDrag = { change, _ ->
                            val offset = change.position - imageOffset
                            val r = rectStart
                            val dx = offset.x - dragStart.x
                            val dy = offset.y - dragStart.y
                            val minBoxSize = 50f
                            val newRect = when (draggingHandle) {
                                Handle.TopLeft -> {
                                    val newLeft = safeCoerceIn(r.left + dx, 0f, r.right - minBoxSize)
                                    val newTop = safeCoerceIn(r.top + dy, 0f, r.bottom - minBoxSize)
                                    safeRect(newLeft, newTop, r.right, r.bottom)
                                }
                                Handle.TopRight -> {
                                    val newRight = safeCoerceIn(r.right + dx, r.left + minBoxSize, boxSize.width.toFloat())
                                    val newTop = safeCoerceIn(r.top + dy, 0f, r.bottom - minBoxSize)
                                    safeRect(r.left, newTop, newRight, r.bottom)
                                }
                                Handle.BottomLeft -> {
                                    val newLeft = safeCoerceIn(r.left + dx, 0f, r.right - minBoxSize)
                                    val newBottom = safeCoerceIn(r.bottom + dy, r.top + minBoxSize, boxSize.height.toFloat())
                                    safeRect(newLeft, r.top, r.right, newBottom)
                                }
                                Handle.BottomRight -> {
                                    val newRight = safeCoerceIn(r.right + dx, r.left + minBoxSize, boxSize.width.toFloat())
                                    val newBottom = safeCoerceIn(r.bottom + dy, r.top + minBoxSize, boxSize.height.toFloat())
                                    safeRect(r.left, r.top, newRight, newBottom)
                                }
                                Handle.Center -> {
                                    val w = r.width
                                    val h = r.height
                                    val newLeft = safeCoerceIn(r.left + dx, 0f, boxSize.width - w)
                                    val newTop = safeCoerceIn(r.top + dy, 0f, boxSize.height - h)
                                    safeRect(newLeft, newTop, newLeft + w, newTop + h)
                                }
                                null -> r
                            }
                            localCropRect = newRect
                        }
                    )
                }
        ) {
            drawRect(
                color = Color.Red,
                topLeft = Offset(localCropRect.left, localCropRect.top),
                size = localCropRect.size,
                style = Stroke(width = 3.dp.toPx())
            )
            val r = localCropRect
            drawCircle(
                color = Color.Red,
                radius = handleRadius,
                center = Offset(r.left, r.top)
            )
            drawCircle(
                color = Color.Red,
                radius = handleRadius,
                center = Offset(r.right, r.top)
            )
            drawCircle(
                color = Color.Red,
                radius = handleRadius,
                center = Offset(r.left, r.bottom)
            )
            drawCircle(
                color = Color.Red,
                radius = handleRadius,
                center = Offset(r.right, r.bottom)
            )
        }
    }
}