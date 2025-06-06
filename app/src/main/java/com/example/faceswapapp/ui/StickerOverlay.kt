package com.example.faceswapapp.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.alpha
import com.example.faceswapapp.PlacedSticker
import com.example.faceswapapp.Sticker
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun StickerOverlay(
    photoBitmap: Bitmap,
    placedStickers: List<PlacedSticker>,
    landmarks: List<Offset>,
    imageWidth: Int,
    imageHeight: Int,
    showLandmarks: Boolean = true
) {
    val context = LocalContext.current

    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var renderScale by remember { mutableStateOf(1f to 1f) }
    var renderOffset by remember { mutableStateOf(Offset.Zero) }
    val stickerBitmaps = remember(placedStickers.map { it.sticker.resId }) {
        placedStickers.associate { it.sticker.resId to
                BitmapFactory.decodeResource(context.resources, it.sticker.resId)
        }
    }

    class StickerCanvasView(ctx: android.content.Context) : android.view.View(ctx) {
        var currentRenderScale: Pair<Float, Float> = 1f to 1f
        var currentRenderOffset: Offset = Offset.Zero
        var currentLandmarks: List<Offset> = emptyList()
        var currentPlacedStickers: List<PlacedSticker> = emptyList()
        var currentStickerBitmaps: Map<Int, Bitmap> = emptyMap()
        var currentShowLandmarks: Boolean = true

        override fun onDraw(canvas: android.graphics.Canvas) {
            val mappedLandmarks = currentLandmarks.map { p ->
                Offset(
                    p.x * currentRenderScale.first + currentRenderOffset.x,
                    p.y * currentRenderScale.second + currentRenderOffset.y
                )
            }
            // Draw landmarks if requested
            if (currentShowLandmarks) {
                val paintRed = Paint().apply {
                    color = Color.RED
                    style = Paint.Style.FILL
                    strokeWidth = 8f
                }
                mappedLandmarks.forEach {
                    canvas.drawCircle(it.x, it.y, 4f, paintRed)
                }
            }
            // Draw all stickers
            for (placed in currentPlacedStickers) {
                val sticker = placed.sticker
                val stickerBitmap = currentStickerBitmaps[sticker.resId] ?: continue

                val scaledOffsetX = placed.x * currentRenderScale.first
                val scaledOffsetY = placed.y * currentRenderScale.second

                val matrix = getStickerMatrixNoScale(
                    sticker, stickerBitmap, mappedLandmarks,
                    offsetX = scaledOffsetX, offsetY = scaledOffsetY,
                    userScale = placed.scale, userRotation = placed.rotation
                )
                canvas.drawBitmap(stickerBitmap, matrix, null)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = photoBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    imageSize = coordinates.size
                    val containerW = imageSize.width.toFloat()
                    val containerH = imageSize.height.toFloat()
                    val imgW = imageWidth.toFloat()
                    val imgH = imageHeight.toFloat()
                    val scale = minOf(containerW / imgW, containerH / imgH)
                    val drawnW = imgW * scale
                    val drawnH = imgH * scale
                    val dx = (containerW - drawnW) / 2f
                    val dy = (containerH - drawnH) / 2f
                    renderScale = scale to scale
                    renderOffset = Offset(dx, dy)
                }
                .fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        AndroidView(
            modifier = Modifier.fillMaxSize().alpha(0.99f),
            factory = { ctx ->
                StickerCanvasView(ctx)
            },
            update = { view ->
                view as StickerCanvasView
                view.currentRenderScale = renderScale
                view.currentRenderOffset = renderOffset
                view.currentLandmarks = landmarks
                view.currentPlacedStickers = placedStickers
                view.currentStickerBitmaps = stickerBitmaps
                view.currentShowLandmarks = showLandmarks
                view.invalidate()
            }
        )
    }
}

fun getStickerMatrixNoScale(
    sticker: Sticker,
    bitmap: Bitmap,
    landmarks: List<Offset>,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    userScale: Float = 1f,
    userRotation: Float = 0f
): Matrix {
    val matrix = Matrix()
    when (sticker.type) {
        com.example.faceswapapp.StickerType.GLASSES -> {
            if (landmarks.size > 263) {
                val leftEye = landmarks[33]
                val rightEye = landmarks[263]
                val faceCenter = Offset((leftEye.x + rightEye.x) / 2f, (leftEye.y + rightEye.y) / 2f)
                val faceEyeDist = distance(leftEye, rightEye)
                val stickerLeftEye = sticker.leftAnchor?.let {
                    Offset(it.x * bitmap.width, it.y * bitmap.height)
                } ?: Offset(bitmap.width * 0.25f, bitmap.height * 0.5f)
                val stickerRightEye = sticker.rightAnchor?.let {
                    Offset(it.x * bitmap.width, it.y * bitmap.height)
                } ?: Offset(bitmap.width * 0.75f, bitmap.height * 0.5f)
                val stickerEyeDist = distance(stickerLeftEye, stickerRightEye)
                val scale = faceEyeDist / stickerEyeDist * userScale
                val faceAngle = Math.toDegrees(atan2(rightEye.y - leftEye.y, rightEye.x - leftEye.x).toDouble()).toFloat()
                val stickerAngle = Math.toDegrees(
                    atan2(stickerRightEye.y - stickerLeftEye.y, stickerRightEye.x - stickerLeftEye.x).toDouble()
                ).toFloat()
                val rotation = faceAngle - stickerAngle + userRotation

                matrix.postTranslate(-stickerLeftEye.x, -stickerLeftEye.y)
                matrix.postScale(scale, scale)
                matrix.postRotate(rotation)
                matrix.postTranslate(faceCenter.x + offsetX, faceCenter.y + offsetY)
            }
        }
        com.example.faceswapapp.StickerType.HAT -> {
            if (landmarks.size > 338 && landmarks.size > 297) {
                val foreheadLeft = landmarks[338]
                val foreheadRight = landmarks[297]
                val faceForeheadDist = distance(foreheadLeft, foreheadRight)
                val stickerBaseLeft = Offset(bitmap.width * 0.25f, bitmap.height * 0.9f)
                val stickerBaseRight = Offset(bitmap.width * 0.75f, bitmap.height * 0.9f)
                val stickerBaseDist = distance(stickerBaseLeft, stickerBaseRight)
                val scale = faceForeheadDist / stickerBaseDist * userScale
                val faceAngle = Math.toDegrees(atan2(foreheadRight.y - foreheadLeft.y, foreheadRight.x - foreheadLeft.x).toDouble()).toFloat()
                val stickerAngle = Math.toDegrees(
                    atan2(stickerBaseRight.y - stickerBaseLeft.y, stickerBaseRight.x - stickerBaseLeft.x).toDouble()
                ).toFloat()
                val rotation = faceAngle - stickerAngle + userRotation

                matrix.postTranslate(-stickerBaseLeft.x, -stickerBaseLeft.y)
                matrix.postScale(scale, scale)
                matrix.postRotate(rotation)
                matrix.postTranslate(foreheadLeft.x + offsetX, foreheadLeft.y - (bitmap.height * scale * 0.9f) + offsetY)
            }
        }
        com.example.faceswapapp.StickerType.MUSTACHE -> {
            if (landmarks.size > 312 && landmarks.size > 82) {
                val mouthLeft = landmarks[82]
                val mouthRight = landmarks[312]
                val faceMouthDist = distance(mouthLeft, mouthRight)
                val stickerMouthLeft = Offset(bitmap.width * 0.2f, bitmap.height * 0.6f)
                val stickerMouthRight = Offset(bitmap.width * 0.8f, bitmap.height * 0.6f)
                val stickerMouthDist = distance(stickerMouthLeft, stickerMouthRight)
                val scale = faceMouthDist / stickerMouthDist * userScale
                val faceAngle = Math.toDegrees(atan2(mouthRight.y - mouthLeft.y, mouthRight.x - mouthLeft.x).toDouble()).toFloat()
                val stickerAngle = Math.toDegrees(
                    atan2(stickerMouthRight.y - stickerMouthLeft.y, stickerMouthRight.x - stickerMouthLeft.x).toDouble()
                ).toFloat()
                val rotation = faceAngle - stickerAngle + userRotation

                matrix.postTranslate(-stickerMouthLeft.x, -stickerMouthLeft.y)
                matrix.postScale(scale, scale)
                matrix.postRotate(rotation)
                matrix.postTranslate(mouthLeft.x + offsetX, mouthLeft.y + offsetY)
            }
        }
    }
    return matrix
}

private fun distance(a: Offset, b: Offset): Float {
    return hypot(a.x - b.x, a.y - b.y)
}