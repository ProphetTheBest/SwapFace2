package com.example.faceswapapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.faceswapapp.PlacedSticker

/**
 * Composable estratto da FaceSwapScreen: barra degli sticker con pulsante "Aggiungi Sticker"
 * Mostra gli sticker piazzati e permette la selezione e l'aggiunta di nuovi sticker
 */
@Composable
fun StickerBar(
    placedStickers: List<PlacedSticker>,
    selectedStickerIndex: Int?,
    onSelectSticker: (Int) -> Unit,
    onAddSticker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sticker:", modifier = Modifier.padding(end = 8.dp))
            placedStickers.forEachIndexed { idx, sticker ->
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(56.dp)
                        .border(
                            border = androidx.compose.foundation.BorderStroke(
                                if (idx == selectedStickerIndex) 3.dp else 1.dp,
                                if (idx == selectedStickerIndex) MaterialTheme.colorScheme.primary else Color.LightGray
                            ),
                            shape = CircleShape
                        )
                        .clickable { onSelectSticker(idx) },
                    shape = CircleShape,
                    color = if (idx == selectedStickerIndex) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = sticker.sticker.resId),
                            contentDescription = sticker.sticker.label,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onAddSticker) {
                Text("Aggiungi Sticker")
            }
        }
    }
}