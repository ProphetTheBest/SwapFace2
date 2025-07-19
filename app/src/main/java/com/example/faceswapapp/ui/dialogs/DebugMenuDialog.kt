package com.example.faceswapapp.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

@Composable
fun DebugMenuDialog(
    onShowDebug: () -> Unit,
    onClearDebug: () -> Unit,
    showLandmarks: Boolean,
    onToggleLandmarks: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Debug & Avanzate") },
        text = {
            Column {
                Button(
                    onClick = {
                        onShowDebug()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("Mostra debug finale")
                }
                Button(
                    onClick = {
                        onClearDebug()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("Elimina debug")
                }
                Divider(Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mostra landmark", Modifier.padding(end = 8.dp))
                    Switch(
                        checked = showLandmarks,
                        onCheckedChange = onToggleLandmarks
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}