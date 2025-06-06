package com.example.faceswapapp.ui

import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.faceswapapp.ui.AppTheme

@Composable
fun ThemeRadioButton(current: AppTheme, value: AppTheme, label: String, onThemeChange: (AppTheme) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = current == value,
            onClick = { onThemeChange(value) }
        )
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}