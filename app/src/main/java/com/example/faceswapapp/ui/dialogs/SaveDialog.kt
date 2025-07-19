package com.example.faceswapapp.ui.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun SaveDialog(
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Salva in Galleria") },
        text = { Text("Vuoi salvare l'immagine risultante nella galleria (con tutti gli sticker)?") },
        confirmButton = {
            TextButton(onClick = onSave) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}