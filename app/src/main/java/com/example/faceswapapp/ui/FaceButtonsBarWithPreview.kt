package com.example.faceswapapp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun FaceButtonsBarWithPreview(
    facePreviews: List<Bitmap>?,
    enabled: Boolean = true,
    onFaceButtonClick: (Int) -> Unit
) {
    val numberOfFaces = facePreviews?.size ?: 0
    if (numberOfFaces > 0) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(numberOfFaces) { index ->
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .shadow(if (enabled) 8.dp else 0.dp, CircleShape)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    onClick = { if (enabled) onFaceButtonClick(index) },
                    enabled = enabled,
                    shape = CircleShape
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (facePreviews?.getOrNull(index) != null) {
                            Image(
                                painter = BitmapPainter(facePreviews[index].asImageBitmap()),
                                contentDescription = "Face $index",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}