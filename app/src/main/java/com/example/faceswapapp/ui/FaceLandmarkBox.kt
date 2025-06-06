package com.example.faceswapapp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale

@Composable
fun FaceLandmarkBox(
    bitmap: Bitmap,
    landmarks: List<List<Offset>>?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        if (landmarks != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleX = size.width / bitmap.width
                val scaleY = size.height / bitmap.height
                landmarks.forEach { faceLandmarks ->
                    faceLandmarks.forEach { point ->
                        drawCircle(
                            color = Color.Red,
                            center = Offset(point.x * scaleX, point.y * scaleY),
                            radius = 4f
                        )
                    }
                }
            }
        }
    }
}