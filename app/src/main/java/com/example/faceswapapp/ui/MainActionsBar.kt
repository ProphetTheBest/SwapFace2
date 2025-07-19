package com.example.faceswapapp.ui


import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun MainActionsBar(
    onOpenPhotoEditor: () -> Unit,
    onSwapFace: () -> Unit,
    onShowJob: () -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientButton(
                text = "Foto Editor",
                gradient = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF70C1B3), Color(0xFFB2DBBF))
                ),
                onClick = onOpenPhotoEditor,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f)
            )
            GradientButton(
                text = "Swap Face",
                gradient = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF5B86E5), Color(0xFF36D1C4))
                ),
                onClick = { if (!isProcessing) onSwapFace() },
                enabled = !isProcessing,
                modifier = Modifier.weight(1f)
            )
            GradientButton(
                text = "Show Job",
                gradient = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFF7971E), Color(0xFFFFD200))
                ),
                onClick = onShowJob,
                enabled = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}