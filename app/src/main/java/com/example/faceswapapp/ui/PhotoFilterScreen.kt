package com.example.faceswapapp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.faceswapapp.utils.FilterType
import com.example.faceswapapp.utils.CaricatureCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape

fun Bitmap.downscale(maxSize: Int = 320): Bitmap {
    val ratio = width.coerceAtLeast(height).toFloat() / maxSize
    if (ratio <= 1.0f) return this
    val newW = (width / ratio).toInt()
    val newH = (height / ratio).toInt()
    return Bitmap.createScaledBitmap(this, newW, newH, true)
}

@Composable
fun PhotoFilterScreen(
    originalBitmap: Bitmap,
    onFilterApplied: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    var activeFilters by remember { mutableStateOf(setOf<FilterType>()) }
    var filterParams by remember {
        mutableStateOf(
            mapOf(
                FilterType.Saturation to 1f,
                FilterType.Blur to 0f,
                FilterType.Sharpen to 1f,
                FilterType.Caricature to 0.15f,
                FilterType.CartoonBrightness to 1.1f,
                FilterType.CartoonContrast to 1.1f,
                FilterType.CartoonQuantLevels to 6f,
                FilterType.CartoonBilateralSize to 8f,
                FilterType.CartoonEdgeKernel to 2f,
                FilterType.CartoonUseCanny to 0f // 0f = false, 1f = true
            )
        )
    }
    val context = LocalContext.current
    val applyScope = rememberCoroutineScope()

    val previewBitmap = remember(originalBitmap) { originalBitmap.downscale(320) }
    var filteredBitmap by remember { mutableStateOf<Bitmap?>(previewBitmap) }
    var isLoading by remember { mutableStateOf(false) }
    var finalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isApplying by remember { mutableStateOf(false) }

    var caricatureCenters by remember { mutableStateOf(listOf<CaricatureCenter>()) }
    var boxPxSize by remember { mutableStateOf(Size.Zero) }

    // Cartoon parametri locali per UI
    var cartoonBrightness by remember { mutableStateOf(filterParams[FilterType.CartoonBrightness] ?: 1.1f) }
    var cartoonContrast by remember { mutableStateOf(filterParams[FilterType.CartoonContrast] ?: 1.1f) }
    var cartoonQuantLevels by remember { mutableStateOf(filterParams[FilterType.CartoonQuantLevels] ?: 6f) }
    var cartoonBilateralSize by remember { mutableStateOf(filterParams[FilterType.CartoonBilateralSize] ?: 8f) }
    var cartoonEdgeKernel by remember { mutableStateOf(filterParams[FilterType.CartoonEdgeKernel] ?: 2f) }
    var cartoonUseCanny by remember { mutableStateOf((filterParams[FilterType.CartoonUseCanny] ?: 0f) > 0.5f) }

    fun updateAllCentersStrength(strength: Float) {
        caricatureCenters = caricatureCenters.map { it.copy(strength = strength) }
    }

    fun handleFilterToggle(filter: FilterType, checked: Boolean) {
        if (filter == FilterType.Caricature) {
            if (!checked) caricatureCenters = emptyList()
        }
        activeFilters = if (checked) activeFilters + filter else activeFilters - filter
    }

    // Ricostruzione filtri preview
    LaunchedEffect(
        previewBitmap,
        activeFilters,
        filterParams,
        caricatureCenters,
        context
    ) {
        isLoading = true
        val absoluteCenters = caricatureCenters.map {
            CaricatureCenter(
                x = it.x * previewBitmap.width,
                y = it.y * previewBitmap.height,
                radius = it.radius * previewBitmap.width.coerceAtMost(previewBitmap.height),
                strength = it.strength
            )
        }
        val result = withContext(Dispatchers.Default) {
            var bmp = previewBitmap
            if (activeFilters.contains(FilterType.Cartoon)) {
                bmp = applyCartoonParametric(
                    bmp,
                    (filterParams[FilterType.CartoonBrightness] ?: 1.1f).toDouble(),
                    (filterParams[FilterType.CartoonContrast] ?: 1.1f).toDouble(),
                    (filterParams[FilterType.CartoonQuantLevels] ?: 6f).toInt(),
                    (filterParams[FilterType.CartoonBilateralSize] ?: 8f).toInt(),
                    (filterParams[FilterType.CartoonEdgeKernel] ?: 2f).toInt(),
                    (filterParams[FilterType.CartoonUseCanny] ?: 0f) > 0.5f
                )
            }
            applyMultipleFilters(
                bmp,
                activeFilters - FilterType.Cartoon, // cartoon già applicato sopra
                filterParams,
                context,
                caricatureCenters = absoluteCenters
            )
        }
        filteredBitmap = result
        isLoading = false
    }

    var filterPanelVisible by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Contenitore immagine con proporzioni fisse
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.TopCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .height(380.dp)
                    .widthIn(max = 600.dp)
                    .aspectRatio(originalBitmap.width.toFloat() / originalBitmap.height.toFloat())
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            val sz = coordinates.size
                            boxPxSize = Size(sz.width.toFloat(), sz.height.toFloat())
                        }
                        .pointerInput(activeFilters, caricatureCenters, previewBitmap, boxPxSize) {
                            detectTapGestures { offset ->
                                if (activeFilters.contains(FilterType.Caricature)) {
                                    val boxWidth = boxPxSize.width
                                    val boxHeight = boxPxSize.height
                                    val imgAspect = previewBitmap.width.toFloat() / previewBitmap.height
                                    val boxAspect = boxWidth / boxHeight

                                    val drawWidth: Float
                                    val drawHeight: Float
                                    val offsetX: Float
                                    val offsetY: Float

                                    if (imgAspect > boxAspect) {
                                        drawWidth = boxWidth
                                        drawHeight = boxWidth / imgAspect
                                        offsetX = 0f
                                        offsetY = (boxHeight - drawHeight) / 2f
                                    } else {
                                        drawHeight = boxHeight
                                        drawWidth = boxHeight * imgAspect
                                        offsetY = 0f
                                        offsetX = (boxWidth - drawWidth) / 2f
                                    }

                                    val xInImage = ((offset.x - offsetX) / drawWidth).coerceIn(0f, 1f)
                                    val yInImage = ((offset.y - offsetY) / drawHeight).coerceIn(0f, 1f)

                                    if (xInImage in 0f..1f && yInImage in 0f..1f) {
                                        caricatureCenters = caricatureCenters + CaricatureCenter(
                                            x = xInImage,
                                            y = yInImage,
                                            radius = 0.35f,
                                            strength = filterParams[FilterType.Caricature] ?: 0.15f
                                        )
                                    }
                                }
                            }
                        }
                ) {
                    Image3DPanel(
                        bitmap = when {
                            isApplying && finalBitmap != null -> finalBitmap!!
                            filteredBitmap != null -> filteredBitmap!!
                            else -> previewBitmap
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (activeFilters.contains(FilterType.Caricature)) {
                        Canvas(Modifier.matchParentSize()) {
                            val boxWidth = size.width
                            val boxHeight = size.height
                            val imgAspect = previewBitmap.width.toFloat() / previewBitmap.height
                            val boxAspect = boxWidth / boxHeight

                            val drawWidth: Float
                            val drawHeight: Float
                            val offsetX: Float
                            val offsetY: Float

                            if (imgAspect > boxAspect) {
                                drawWidth = boxWidth
                                drawHeight = boxWidth / imgAspect
                                offsetX = 0f
                                offsetY = (boxHeight - drawHeight) / 2f
                            } else {
                                drawHeight = boxHeight
                                drawWidth = boxHeight * imgAspect
                                offsetY = 0f
                                offsetX = (boxWidth - drawWidth) / 2f
                            }

                            for (center in caricatureCenters) {
                                drawCircle(
                                    color = Color.Red,
                                    radius = 12f,
                                    center = Offset(
                                        offsetX + center.x * drawWidth,
                                        offsetY + center.y * drawHeight
                                    )
                                )
                            }
                        }
                    }
                    if (isLoading || isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        if (filterPanelVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 420.dp)
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.TopCenter
            ) {
                Column {
                    FilterPanel3D(
                        activeFilters = activeFilters,
                        onFilterToggled = { filter, checked -> handleFilterToggle(filter, checked) },
                        filterParams = filterParams,
                        onParamChange = { filter, value ->
                            val newValue = if (filter == FilterType.Caricature) {
                                value.coerceIn(0.05f, 0.5f)
                            } else value
                            filterParams = filterParams.toMutableMap().apply { put(filter, newValue) }
                            if (filter == FilterType.Caricature) updateAllCentersStrength(newValue)
                        },
                        onApply = {
                            isApplying = true
                            finalBitmap = null
                            applyScope.launch {
                                val absoluteCenters = caricatureCenters.map {
                                    CaricatureCenter(
                                        x = it.x * originalBitmap.width,
                                        y = it.y * originalBitmap.height,
                                        radius = it.radius * originalBitmap.width.coerceAtMost(originalBitmap.height),
                                        strength = it.strength
                                    )
                                }
                                val result = withContext(Dispatchers.Default) {
                                    var bmp = originalBitmap
                                    if (activeFilters.contains(FilterType.Cartoon)) {
                                        bmp = applyCartoonParametric(
                                            bmp,
                                            (filterParams[FilterType.CartoonBrightness] ?: 1.1f).toDouble(),
                                            (filterParams[FilterType.CartoonContrast] ?: 1.1f).toDouble(),
                                            (filterParams[FilterType.CartoonQuantLevels] ?: 6f).toInt(),
                                            (filterParams[FilterType.CartoonBilateralSize] ?: 8f).toInt(),
                                            (filterParams[FilterType.CartoonEdgeKernel] ?: 2f).toInt(),
                                            (filterParams[FilterType.CartoonUseCanny] ?: 0f) > 0.5f
                                        )
                                    }
                                    applyMultipleFilters(
                                        bmp,
                                        activeFilters - FilterType.Cartoon,
                                        filterParams,
                                        context,
                                        caricatureCenters = absoluteCenters
                                    )
                                }
                                finalBitmap = result
                                isApplying = false
                                filterPanelVisible = false
                                onFilterApplied(result)
                            }
                        },
                        onCancel = { filterPanelVisible = false },
                        cartoonBrightness = cartoonBrightness,
                        onCartoonBrightnessChange = {
                            cartoonBrightness = it
                            filterParams = filterParams.toMutableMap().apply { put(FilterType.CartoonBrightness, it) }
                        },
                        cartoonContrast = cartoonContrast,
                        onCartoonContrastChange = {
                            cartoonContrast = it
                            filterParams = filterParams.toMutableMap().apply { put(FilterType.CartoonContrast, it) }
                        },
                        cartoonQuantLevels = cartoonQuantLevels,
                        onCartoonQuantLevelsChange = {
                            cartoonQuantLevels = it
                            filterParams = filterParams.toMutableMap().apply { put(FilterType.CartoonQuantLevels, it) }
                        },
                        cartoonBilateralSize = cartoonBilateralSize,
                        onCartoonBilateralSizeChange = {
                            cartoonBilateralSize = it
                            filterParams = filterParams.toMutableMap().apply { put(FilterType.CartoonBilateralSize, it) }
                        },
                        cartoonEdgeKernel = cartoonEdgeKernel,
                        onCartoonEdgeKernelChange = {
                            cartoonEdgeKernel = it
                            filterParams = filterParams.toMutableMap().apply { put(FilterType.CartoonEdgeKernel, it) }
                        },
                        cartoonUseCanny = cartoonUseCanny,
                        onCartoonUseCannyChange = {
                            cartoonUseCanny = it
                            filterParams = filterParams.toMutableMap().apply { put(FilterType.CartoonUseCanny, if (it) 1f else 0f) }
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Button(
                            onClick = { caricatureCenters = emptyList() },
                            enabled = activeFilters.contains(FilterType.Caricature)
                        ) {
                            Text("Reset centri")
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Tocca l'immagine per aggiungere centri")
                    }
                }
            }
        }
    }
}