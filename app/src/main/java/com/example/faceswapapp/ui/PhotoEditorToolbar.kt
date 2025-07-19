package com.example.faceswapapp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PhotoEditorToolbar(
    onRotate: () -> Unit,
    onCrop: () -> Unit,
    onFilter: () -> Unit,
    onChangeBackground: () -> Unit,
    onRemoveObject: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onBrushRemove: () -> Unit = {},
    brushRemoveEnabled: Boolean = true,
    onJobQueue: () -> Unit = {},
) {
    BottomAppBar(
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp) // <--- aumenta qui!
            ) {
                IconButton(onClick = onRotate, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.RotateRight,
                        contentDescription = "Ruota",
                        tint = Color(0xFF42A5F5),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onCrop, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Crop,
                        contentDescription = "Taglia",
                        tint = Color(0xFFAB47BC),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onFilter, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = "Filtri",
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onChangeBackground, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = "Cambia Sfondo",
                        tint = Color(0xFF26A69A),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onRemoveObject, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Photo,
                        contentDescription = "Rimuovi Sfondo",
                        tint = Color(0xFFEC407A),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onBrushRemove,
                    enabled = brushRemoveEnabled,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Brush,
                        contentDescription = "Pennello Rimuovi Oggetto",
                        tint = Color(0xFF66BB6A),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onJobQueue, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.List,
                        contentDescription = "Coda Job HuggingFace",
                        tint = Color(0xFFFFD600),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Save,
                        contentDescription = "Salva",
                        tint = Color(0xFF424242),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { onShare() }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Condividi",
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}