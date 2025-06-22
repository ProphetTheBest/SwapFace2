package com.example.faceswapapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

@Composable
fun CartoonParamPanel(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    quantLevels: Float,
    onQuantLevelsChange: (Float) -> Unit,
    bilateralSize: Float,
    onBilateralSizeChange: (Float) -> Unit,
    edgeKernel: Float,
    onEdgeKernelChange: (Float) -> Unit,
    useCanny: Boolean,
    onUseCannyChange: (Boolean) -> Unit
) {
    Column(Modifier.padding(8.dp)) {
        Text("Parametri Cartoon", style = MaterialTheme.typography.titleSmall)
        Text("Luminosità: %.2f".format(brightness))
        Slider(value = brightness, onValueChange = onBrightnessChange, valueRange = 0.8f..1.5f)
        Text("Contrasto: %.2f".format(contrast))
        Slider(value = contrast, onValueChange = onContrastChange, valueRange = 0.8f..1.5f)
        Text("Livelli Colore: ${quantLevels.toInt()}")
        Slider(value = quantLevels, onValueChange = onQuantLevelsChange, valueRange = 3f..12f, steps = 9)
        Text("Bilateral Size: ${bilateralSize.toInt()}")
        Slider(value = bilateralSize, onValueChange = onBilateralSizeChange, valueRange = 3f..12f, steps = 9)
        Text("Spessore bordi: ${edgeKernel.toInt()}")
        Slider(value = edgeKernel, onValueChange = onEdgeKernelChange, valueRange = 1f..5f, steps = 4)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = useCanny, onCheckedChange = onUseCannyChange)
            Text("Edge Canny")
        }
    }
}