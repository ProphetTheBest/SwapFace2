package com.example.faceswapapp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

@Composable
fun PhotoEditorToolbar(
    onRotate: () -> Unit,
    onCrop: () -> Unit,
    onFilter: () -> Unit,
    onChangeBackground: () -> Unit,
    onRemoveObject: () -> Unit,
    onSave: () -> Unit,
    onBrushRemove: () -> Unit = {},
    brushRemoveEnabled: Boolean = true
) {
    BottomAppBar(
        actions = {
            IconButton(onClick = onRotate) {
                Icon(Icons.Filled.RotateRight, contentDescription = "Ruota")
            }
            IconButton(onClick = onCrop) {
                Icon(Icons.Filled.Crop, contentDescription = "Taglia")
            }
            IconButton(onClick = onFilter) {
                Icon(Icons.Filled.Tune, contentDescription = "Filtri")
            }
            IconButton(onClick = onChangeBackground) {
                Icon(Icons.Filled.Image, contentDescription = "Cambia Sfondo")
            }
            IconButton(onClick = onRemoveObject) {
                Icon(Icons.Filled.Photo, contentDescription = "Rimuovi Sfondo")
            }
            IconButton(
                onClick = onBrushRemove,
                enabled = brushRemoveEnabled
            ) {
                Icon(
                    Icons.Filled.Brush,
                    contentDescription = "Pennello Rimuovi Oggetto"
                )
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Filled.Save, contentDescription = "Salva")
            }
        }
    )
}