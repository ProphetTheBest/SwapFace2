package com.example.faceswapapp.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.faceswapapp.ui.AppTheme
import androidx.compose.foundation.layout.Row

@Composable
fun ThemeDialog(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tema") },
        text = {
            Column {
                ThemeRadioButton(currentTheme, AppTheme.LIGHT, "Chiaro", onThemeChange)
                ThemeRadioButton(currentTheme, AppTheme.DARK, "Scuro", onThemeChange)
                ThemeRadioButton(currentTheme, AppTheme.SYSTEM, "Sistema", onThemeChange)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

@Composable
fun ThemeRadioButton(
    currentTheme: AppTheme,
    value: AppTheme,
    label: String,
    onThemeChange: (AppTheme) -> Unit
) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        RadioButton(
            selected = currentTheme == value,
            onClick = { onThemeChange(value) }
        )
        Text(label)
    }
}