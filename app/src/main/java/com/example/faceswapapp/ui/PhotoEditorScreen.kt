package com.example.faceswapapp.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.faceswapapp.viewmodel.PhotoEditorViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot

@Composable
fun PhotoEditorScreen(
    imageUri: Uri,
    editorViewModel: PhotoEditorViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by editorViewModel.uiState.collectAsState()

    // Caricamento immagine iniziale
    LaunchedEffect(imageUri) {
        editorViewModel.loadImage(context, imageUri)
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            editorViewModel.clearSnackbar()
        }
    }

    // Traccia la posizione e la dimensione effettiva dell'immagine visualizzata
    var imageOffset by remember { mutableStateOf(Offset.Zero) }
    var imageSize by remember { mutableStateOf(IntSize(1, 1)) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            PhotoEditorToolbar(
                onRotate = { editorViewModel.rotate() },
                onCrop = { editorViewModel.enableCrop() },
                onFilter = { editorViewModel.showFilter() },
                onChangeBackground = { editorViewModel.showBackgroundDialog(true) },
                onRemoveObject = { editorViewModel.startSegmentPerson(context, removeBgOnly = true) },
                onSave = { editorViewModel.save(context) },
                onBrushRemove = { editorViewModel.enableBrushRemove() },
                brushRemoveEnabled = !state.isBrushRemoveMode && !state.isCropMode && !state.showFilterScreen
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Overlay di caricamento
            if (state.isLoading || state.isSegmenting) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            val compositeBitmap = state.compositeBitmap
            val bitmap = state.bitmap

            if (!state.isLoading && !state.isSegmenting) {
                when {
                    compositeBitmap != null -> {
                        Image(
                            bitmap = compositeBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                    imageOffset = coordinates.positionInRoot()
                                    imageSize = coordinates.size
                                },
                            contentScale = ContentScale.Fit // PATCH: nessun taglio, mostra sempre tutta l'immagine
                        )
                        Column(
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = { editorViewModel.applyCompositing() },
                                modifier = Modifier.padding(8.dp)
                            ) { Text("Applica sfondo") }
                            Button(
                                onClick = { editorViewModel.cancelCompositing() }
                            ) { Text("Annulla") }
                        }
                    }
                    bitmap != null -> {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                    imageOffset = coordinates.positionInRoot()
                                    imageSize = coordinates.size
                                },
                            contentScale = ContentScale.Fit // PATCH: nessun taglio, mostra sempre tutta l'immagine
                        )
                    }
                    else -> {
                        Text(
                            "Nessuna immagine caricata",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            // Overlay Crop
            if (state.isCropMode && bitmap != null && imageSize.width > 0 && imageSize.height > 0) {
                LaunchedEffect(imageSize) {
                    editorViewModel.updateBoxSize(imageSize)
                }
                MovableCropBox(
                    boxSize = imageSize,
                    cropRect = state.cropRect,
                    imageOffset = imageOffset,
                    onCropRectFinal = { editorViewModel.updateCropRect(it) }
                )
                Button(
                    onClick = { editorViewModel.applyCrop() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                ) { Text("Applica Crop") }
            }

            // Overlay Brush Remove
            if (state.isBrushRemoveMode && bitmap != null) {
                BrushMaskOverlay(
                    imageBitmap = bitmap.asImageBitmap(),
                    brushPathList = state.brushPathList,
                    onPathAdded = { path, thickness ->
                        editorViewModel.addBrushPath(path, thickness)
                    },
                    brushSize = state.currentBrushSize,
                    onBrushSizeChange = { editorViewModel.updateBrushSize(it) },
                    onCanvasSizeChanged = { editorViewModel.updateBrushCanvasSize(it) },
                    onImageBoxChanged = { ox, oy, w, h ->
                        editorViewModel.updateBrushImageBox(ox to oy, w to h)
                    },
                    modifier = Modifier.fillMaxSize()
                )
                BrushRemoveBottomSheetExtended(
                    removeBackend = state.removeBackend,
                    onBackendChange = { editorViewModel.setRemoveBackend(it) },
                    prompt = state.inpaintPrompt,
                    onPromptChange = { editorViewModel.setInpaintPrompt(it) },
                    brushSize = state.currentBrushSize,
                    onBrushSizeChange = { editorViewModel.updateBrushSize(it) },
                    onUndo = { editorViewModel.undoBrush() },
                    onRedo = { editorViewModel.redoBrush() },
                    onReset = { editorViewModel.resetBrush() },
                    onApply = { editorViewModel.applyBrushRemove(context) },
                    onCancel = { editorViewModel.disableBrushRemove() },
                    undoEnabled = state.brushPathList.isNotEmpty(),
                    redoEnabled = state.redoStack.isNotEmpty(),
                    resetEnabled = state.brushPathList.isNotEmpty() || state.redoStack.isNotEmpty()
                )
            }

            // Overlay Filtri
            if (state.showFilterScreen && bitmap != null) {
                PhotoFilterScreen(
                    originalBitmap = bitmap,
                    onFilterApplied = { editorViewModel.onFilterApplied(it) },
                    onBack = { editorViewModel.onBackFromFilter() }
                )
            }

            // Overlay Dialog Sostituzione Sfondo
            if (state.showBackgroundDialog) {
                BackgroundPickerDialog(
                    onDismiss = { editorViewModel.showBackgroundDialog(false) },
                    onSelectFromGallery = { editorViewModel.pickBackgroundFromGallery(context) },
                    onTakePhoto = { editorViewModel.takeBackgroundPhoto(context) }
                )
            }
        }
    }
}