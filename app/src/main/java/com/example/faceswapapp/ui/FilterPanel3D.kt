package com.example.faceswapapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.faceswapapp.utils.FilterType
import com.example.faceswapapp.ui.CartoonParamPanel

@Composable
fun FilterPanel3D(
    modifier: Modifier = Modifier,
    activeFilters: Set<FilterType>,
    onFilterToggled: (FilterType, Boolean) -> Unit,
    filterParams: Map<FilterType, Float>,
    onParamChange: (FilterType, Float) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    cartoonBrightness: Float = 1.1f,
    onCartoonBrightnessChange: (Float) -> Unit = {},
    cartoonContrast: Float = 1.1f,
    onCartoonContrastChange: (Float) -> Unit = {},
    cartoonQuantLevels: Float = 6f,
    onCartoonQuantLevelsChange: (Float) -> Unit = {},
    cartoonBilateralSize: Float = 8f,
    onCartoonBilateralSizeChange: (Float) -> Unit = {},
    cartoonEdgeKernel: Float = 2f,
    onCartoonEdgeKernelChange: (Float) -> Unit = {},
    cartoonUseCanny: Boolean = false,
    onCartoonUseCannyChange: (Boolean) -> Unit = {},
    animeGanModelName: String = "face_paint_512_v2_tf_nhwc_inout.tflite", // PATCH: aggiunto
    onAnimeGanModelChange: (String) -> Unit = {} // PATCH: aggiunto
) {
    val panelShape: Shape = RoundedCornerShape(28.dp)
    val panelGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFB3E5FC), // azzurro chiaro
            Color(0xFF81D4FA), // azzurro intermedio
            Color(0xFF4FC3F7)  // azzurro più intenso
        )
    )

    // PATCH: lista modelli AnimeGAN
    val animeGanModels = listOf(
        "face_paint_512_v2_tf_nhwc_inout.tflite" to "FacePaint v2",
        "celeba_distill_tf_nhwc_inout.tflite" to "CelebA Distill",
        "face_paint_512_v1_tf_nhwc_inout.tflite" to "FacePaint v1",
        "paprika_tf_nhwc_inout.tflite" to "Paprika"
    )

    Surface(
        shape = panelShape,
        shadowElevation = 24.dp,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 8.dp)
            .shadow(elevation = 24.dp, shape = panelShape)
            .background(panelGradient, shape = panelShape)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 72.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Filtri avanzati",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF12527A)
                )
                Spacer(Modifier.height(14.dp))
                // Cicla solo sui filtri principali che devono avere checkbox
                FilterType.values().filter { it.showAsCheckbox }.forEach { filter ->
                    FilterRowMulti(
                        label = filter.label,
                        description = filter.description,
                        checked = activeFilters.contains(filter),
                        onCheckedChange = { checked -> onFilterToggled(filter, checked) }
                    )
                    // Parametri cartoon: mostra il pannello SOLO sotto il filtro Cartoon se attivo
                    if (filter == FilterType.Cartoon && activeFilters.contains(FilterType.Cartoon)) {
                        CartoonParamPanel(
                            brightness = cartoonBrightness,
                            onBrightnessChange = onCartoonBrightnessChange,
                            contrast = cartoonContrast,
                            onContrastChange = onCartoonContrastChange,
                            quantLevels = cartoonQuantLevels,
                            onQuantLevelsChange = onCartoonQuantLevelsChange,
                            bilateralSize = cartoonBilateralSize,
                            onBilateralSizeChange = onCartoonBilateralSizeChange,
                            edgeKernel = cartoonEdgeKernel,
                            onEdgeKernelChange = onCartoonEdgeKernelChange,
                            useCanny = cartoonUseCanny,
                            onUseCannyChange = onCartoonUseCannyChange
                        )
                    }
                    // PATCH: pannello modelli AnimeGAN solo se attivo
                    if (filter == FilterType.AnimeGAN && activeFilters.contains(FilterType.AnimeGAN)) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 10.dp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Scegli il modello AnimeGAN:", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(10.dp))
                                animeGanModels.forEach { (modelFile, label) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = animeGanModelName == modelFile,
                                            onClick = { onAnimeGanModelChange(modelFile) }
                                        )
                                        Text(label, Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                    // Slider per filtri che hanno parametro (eccetto Cartoon, che ha pannello dedicato)
                    if (filter.hasParameter && activeFilters.contains(filter) && filter != FilterType.Cartoon) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = filterParams[filter] ?: when (filter) {
                                    FilterType.Caricature -> 0.7f
                                    FilterType.Saturation -> 1f
                                    FilterType.Blur -> 0f
                                    FilterType.Sharpen -> 1f
                                    else -> 1f
                                },
                                onValueChange = { onParamChange(filter, it) },
                                valueRange = when (filter) {
                                    FilterType.Saturation -> 0f..2f
                                    FilterType.Blur -> 0f..25f
                                    FilterType.Sharpen -> 0f..2f
                                    FilterType.Caricature -> 0.3f..0.95f
                                    else -> 0f..1f
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                when (filter) {
                                    FilterType.Saturation -> "${((filterParams[filter] ?: 1f) * 100).toInt()}%"
                                    FilterType.Blur -> "${(filterParams[filter] ?: 0f).toInt()}"
                                    FilterType.Sharpen -> "${(filterParams[filter] ?: 1f)}x"
                                    FilterType.Caricature -> "${((filterParams[filter] ?: 0.7f) * 100).toInt()}%"
                                    else -> ""
                                },
                                Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCancel
                ) {
                    Text("Annulla")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF212121),
                        contentColor = Color.White
                    )
                ) {
                    Text("Applica")
                }
            }
        }
    }
}