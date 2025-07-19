package com.example.faceswapapp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxWithConstraints
import com.example.faceswapapp.PlacedSticker

@Composable
fun FacePreview(
    currentBitmap: Bitmap?,
    detectedLandmarks: List<List<Offset>>?,
    currentFaceIndex: Int,
    placedStickers: List<PlacedSticker>,
    showLandmarks: Boolean,
    isProcessing: Boolean,
    errorMessage: String?,
    previewBoxWidthPx: (Float) -> Unit = {},
    previewBoxHeightPx: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 16.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Begin PATCH: uniforma canvas come filterimg
        if (currentBitmap != null) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .height(380.dp)
                    .widthIn(max = 600.dp)
                    .aspectRatio(currentBitmap.width.toFloat() / currentBitmap.height.toFloat())
            ) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (detectedLandmarks != null && currentFaceIndex in detectedLandmarks.indices) {
                        StickerOverlay(
                            photoBitmap = currentBitmap,
                            placedStickers = placedStickers,
                            landmarks = detectedLandmarks[currentFaceIndex],
                            imageWidth = currentBitmap.width,
                            imageHeight = currentBitmap.height,
                            showLandmarks = showLandmarks
                        )
                    } else {
                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .size(380.dp)
                    .shadow(12.dp, RoundedCornerShape(32.dp))
                    .background(Color.White, RoundedCornerShape(32.dp))
            ) {
                val density = LocalDensity.current
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }
                previewBoxWidthPx(wPx)
                previewBoxHeightPx(hPx)
                Box(
                    modifier = Modifier
                        .width(maxWidth)
                        .height(maxHeight)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp,
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        Text(
                            text = errorMessage ?: "Seleziona un'immagine per iniziare",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
        }
        // End PATCH
    }
}