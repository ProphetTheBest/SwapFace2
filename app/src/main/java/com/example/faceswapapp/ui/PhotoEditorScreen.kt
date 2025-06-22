package com.example.faceswapapp.ui

import android.net.Uri
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.draw.shadow
import com.example.faceswapapp.utils.ImageUtils
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    imageUri: Uri,
    editorViewModel: PhotoEditorViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by editorViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var photoUriForCamera by remember { mutableStateOf<Uri?>(null) }

    val backgroundGalleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        editorViewModel.showBackgroundDialog(false)
        if (uri != null) {
            coroutineScope.launch {
                editorViewModel.setBackgroundImageFromUri(context, uri)
                editorViewModel.showSnackbar("Nuovo sfondo selezionato!")
            }
        } else {
            editorViewModel.showSnackbar("Selezione sfondo annullata.")
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        editorViewModel.showBackgroundDialog(false)
        if (success && photoUriForCamera != null) {
            coroutineScope.launch {
                editorViewModel.setBackgroundImageFromUri(context, photoUriForCamera!!)
                editorViewModel.showSnackbar("Nuova foto acquisita come sfondo!")
            }
        } else {
            editorViewModel.showSnackbar("Foto non acquisita.")
        }
    }

    LaunchedEffect(imageUri) {
        editorViewModel.loadImage(context, imageUri)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            editorViewModel.clearSnackbar()
        }
    }

    var imageSize by remember { mutableStateOf(IntSize(1, 1)) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            PhotoEditorToolbar(
                onRotate = { editorViewModel.rotate() },
                onCrop = { editorViewModel.enableCrop() },
                onFilter = { editorViewModel.showFilter() },
                onChangeBackground = {
                    if (editorViewModel.uiState.value.personBitmap == null) {
                        editorViewModel.startSegmentPersonAndShowBackgroundDialog(context)
                    } else {
                        editorViewModel.showBackgroundDialog(true)
                    }
                },
                onRemoveObject = { editorViewModel.startSegmentPerson(context, removeBgOnly = true) },
                onSave = { editorViewModel.save(context) },
                onBrushRemove = {
                    editorViewModel.enableBrushRemove()
                },
                brushRemoveEnabled = !state.isBrushRemoveMode && !state.isCropMode && !state.showFilterScreen
            )
        }
    ) { paddingValues ->
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            val compositeBitmap = state.compositeBitmap
            val bitmapToShow = when {
                state.isResultMode && state.bitmapResult != null -> state.bitmapResult
                state.isBrushRemoveMode && state.bitmapInput != null -> state.bitmapInput
                else -> state.bitmap
            }

            // MOSTRA SOLO UNO DEI DUE: o i filtri, o la card principale
            if (state.showFilterScreen && bitmapToShow != null) {
                // Mostra solo la schermata filtri (che include già la card decorata)
                PhotoFilterScreen(
                    originalBitmap = bitmapToShow,
                    onFilterApplied = { editorViewModel.onFilterApplied(it) },
                    onBack = { editorViewModel.onBackFromFilter() }
                )
            } else {
                // Mostra la card principale SOLO se non sei nei filtri!
                Box(
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp)
                        .height(380.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (!state.isLoading && !state.isSegmenting) {
                        when {
                            compositeBitmap != null -> {
                                BrushImageBox(
                                    bitmap = compositeBitmap,
                                    state = state,
                                    editorViewModel = editorViewModel,
                                    imageSizeSetter = { imageSize = it }
                                )
                            }
                            bitmapToShow != null -> {
                                BrushImageBox(
                                    bitmap = bitmapToShow,
                                    state = state,
                                    editorViewModel = editorViewModel,
                                    imageSizeSetter = { imageSize = it }
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

                    // Overlay di caricamento
                    if (state.isLoading || state.isSegmenting) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }

                    // Overlay Crop
                    if (state.isCropMode && bitmapToShow != null && imageSize.width > 0 && imageSize.height > 0) {
                        LaunchedEffect(imageSize) {
                            editorViewModel.updateBoxSize(imageSize)
                        }
                        MovableCropBox(
                            boxSize = imageSize,
                            cropRect = state.cropRect,
                            imageOffset = Offset.Zero,
                            onCropRectFinal = { editorViewModel.updateCropRect(it) }
                        )
                        Button(
                            onClick = { editorViewModel.applyCrop() },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(32.dp)
                        ) { Text("Applica Crop") }
                    }
                    /*
                    // DEBUG MASK: mostra la mask generata live in basso a destra SOLO in modalità pennello
                    if (state.isBrushRemoveMode && bitmapToShow != null) {
                        val maskBitmap = remember(
                            state.brushPathList,
                            state.brushCanvasSize,
                            state.brushImageOffset,
                            state.brushImageSize
                        ) {
                            com.example.faceswapapp.utils.BrushMaskOverlayHelper.generateMaskBitmap(
                                bitmapToShow.width, bitmapToShow.height, state.brushPathList,
                                state.brushCanvasSize, state.brushImageOffset, state.brushImageSize
                            )
                        }
                        Image(
                            bitmap = maskBitmap.asImageBitmap(),
                            contentDescription = "DEBUG MASK",
                            modifier = Modifier
                                .size(128.dp)
                                .align(Alignment.BottomEnd)
                                .background(Color.Black),
                            contentScale = ContentScale.Fit
                        )
                    }
                    */
                }
            }

            // Quando compare il pannello filtri o altri overlay, lascia SEMPRE spazio sotto la card
            if (state.compositeBitmap != null && !state.showFilterScreen) {
                Column(
                    Modifier.padding(bottom = 96.dp),
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

            if (state.showFilterScreen && bitmapToShow != null) {
                Spacer(Modifier.height(16.dp))
            }

            // Overlay Dialog Sostituzione Sfondo
            if (state.showBackgroundDialog) {
                BackgroundPickerDialog(
                    onDismiss = { editorViewModel.showBackgroundDialog(false) },
                    onSelectFromGallery = {
                        backgroundGalleryPickerLauncher.launch("image/*")
                    },
                    onTakePhoto = {
                        val uri = ImageUtils.createImageUri(context)
                        if (uri != null) {
                            photoUriForCamera = uri
                            cameraLauncher.launch(uri)
                        } else {
                            editorViewModel.showSnackbar("Impossibile creare file per la foto")
                            editorViewModel.showBackgroundDialog(false)
                        }
                    }
                )
            }

            // PANNELLO FISSO IN BASSO: Brush Remove, compatto e scrollabile
            if (state.showBrushSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .heightIn(max = 240.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        BrushRemoveBottomSheetExtended(
                            isBrushRemoveMode = state.isBrushRemoveMode,
                            isResultMode = state.isResultMode,
                            removeBackend = state.removeBackend,
                            onBackendChange = { editorViewModel.setRemoveBackend(it) },
                            prompt = state.inpaintPrompt,
                            onPromptChange = { editorViewModel.setInpaintPrompt(it) },
                            brushSize = state.currentBrushSize,
                            onBrushSizeChange = { editorViewModel.updateBrushSize(it) },
                            numInferenceSteps = state.numInferenceSteps,
                            onNumInferenceStepsChange = { editorViewModel.setNumInferenceSteps(it) },
                            onUndo = { editorViewModel.undoBrush() },
                            onRedo = { editorViewModel.redoBrush() },
                            onReset = { editorViewModel.resetBrush() },
                            onApply = { editorViewModel.applyBrushRemove(context) },
                            onCancel = { editorViewModel.disableBrushRemove() },
                            onRestoreBrushEditing = { editorViewModel.restoreBrushEditing() },
                            onSave = { editorViewModel.save(context) },
                            undoEnabled = state.brushPathList.isNotEmpty(),
                            redoEnabled = state.redoStack.isNotEmpty(),
                            resetEnabled = state.brushPathList.isNotEmpty() || state.redoStack.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrushImageBox(
    bitmap: Bitmap,
    state: com.example.faceswapapp.viewmodel.PhotoEditorUiState,
    editorViewModel: PhotoEditorViewModel,
    imageSizeSetter: (IntSize) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(12.dp, RoundedCornerShape(32.dp))
            .background(Color.White, RoundedCornerShape(32.dp))
            .padding(8.dp)
            .onGloballyPositioned { coordinates ->
                val size = coordinates.size
                imageSizeSetter(size)
                editorViewModel.updateBrushCanvasSize(size)
                editorViewModel.updateBrushImageBox(
                    offset = 0f to 0f,
                    size = size.width.toFloat() to size.height.toFloat()
                )
            }
    ) {
        Image3DPanel(
            bitmap = bitmap,
            modifier = Modifier.fillMaxSize()
        )
        if (state.isBrushRemoveMode) {
            BrushMaskOverlay(
                brushPathList = state.brushPathList,
                onPathAdded = { path, thickness -> editorViewModel.addBrushPath(path, thickness) },
                brushSize = state.currentBrushSize,
                onBrushSizeChange = { editorViewModel.updateBrushSize(it) },
                onCanvasSizeChanged = { editorViewModel.updateBrushCanvasSize(it) },
                onImageBoxChanged = { ox, oy, w, h ->
                    editorViewModel.updateBrushImageBox(ox to oy, w to h)
                },
                imageOffset = Offset.Zero,
                imageSize = state.brushCanvasSize,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun Image3DPanel(
    bitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}