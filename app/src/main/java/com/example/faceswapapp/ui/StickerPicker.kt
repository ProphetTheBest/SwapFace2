package com.example.faceswapapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.faceswapapp.Sticker

@Composable
fun StickerPicker(
    stickers: List<Sticker>,
    selectedSticker: Sticker?,
    onStickerSelected: (Sticker?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clickable { onStickerSelected(null) }
                .padding(end = 8.dp),
            color = if (selectedSticker == null) MaterialTheme.colorScheme.primary else Color.LightGray,
            shape = CircleShape
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                contentDescription = "Nessuno"
            )
        }
        stickers.forEach { sticker ->
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clickable { onStickerSelected(sticker) }
                    .padding(end = 8.dp),
                color = if (selectedSticker?.id == sticker.id) MaterialTheme.colorScheme.primary else Color.LightGray,
                shape = CircleShape
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = sticker.resId),
                    contentDescription = sticker.label,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}