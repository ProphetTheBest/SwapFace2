package com.example.faceswapapp.ui

import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BackgroundPickerDialog(
    onDismiss: () -> Unit,
    onSelectFromGallery: () -> Unit,
    onTakePhoto: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scegli il nuovo sfondo") },
        text = {
            Text(
                "Seleziona un'immagine dalla galleria o scatta una nuova foto da usare come sfondo.",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                ElevatedButton(
                    onClick = onSelectFromGallery
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = "Galleria"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Galleria")
                }
                ElevatedButton(
                    onClick = onTakePhoto
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Fotocamera"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Fotocamera")
                }
            }
        },
        dismissButton = {}
    )
}