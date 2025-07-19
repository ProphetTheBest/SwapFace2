package com.example.faceswapapp.ui

import android.content.Context
import android.util.Log
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
import com.example.faceswapapp.viewmodel.InpaintJob
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
import com.example.faceswapapp.utils.ImageUtils
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import java.io.File
import java.io.FileOutputStream

private const val TAG = "FSWAPTRACE"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    imageUri: Uri? = null,
    editorViewModel: PhotoEditorViewModel,
    onShare: (File?) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by editorViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var photoUriForCamera by remember { mutableStateOf<Uri?>(null) }
    var showJobQueuePanel by remember { mutableStateOf(false) }
    var showJobEditModal by remember { mutableStateOf(false) }
    var jobToEdit by remember { mutableStateOf<InpaintJob?>(null) }

    val activity = context as? android.app.Activity
    val jobId = activity?.intent?.getStringExtra(PhotoEditorActivity.EXTRA_JOB_ID)
    val resultPath = activity?.intent?.getStringExtra(PhotoEditorActivity.EXTRA_JOB_RESULT_PATH)
    LaunchedEffect(jobId, resultPath) {
        if (jobId != null) {
            editorViewModel.setCurrentJobId(jobId)
        }
        if (resultPath != null && resultPath.isNotBlank()) {
            editorViewModel.loadJobResultByPath(resultPath)
        }
    }

    LaunchedEffect(showJobQueuePanel) {
        if (showJobQueuePanel) {
            editorViewModel.loadPersistentJobs(context)
        }
    }

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

    LaunchedEffect(true) {
        Log.d(TAG, "PhotoEditorScreen: calling loadPersistentJobs")
        editorViewModel.loadPersistentJobs(context)
    }

    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            editorViewModel.loadImage(context, imageUri)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            editorViewModel.clearSnackbar()
        }
    }

    var imageSize by remember { mutableStateOf(IntSize(1, 1)) }

    // bitmap effettivamente mostrata nel canvas
    val bitmapToShow: Bitmap? = when {
        state.isResultMode && state.bitmapResult != null -> state.bitmapResult
        state.isBrushRemoveMode && state.bitmapInput != null -> state.bitmapInput
        else -> state.bitmap
    }

    // funzione per salvare la bitmap su file temporaneo
    fun saveBitmapToTempFile(context: Context, bitmap: Bitmap): File? {
        return try {
            val file = File(context.cacheDir, "share_image_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // mostra la modale di editing job completato se richiesta
    if (showJobEditModal && jobToEdit != null) {
        JobEditModal(
            job = jobToEdit!!,
            onApply = { newPrompt, newMask, newSteps ->
                editorViewModel.reapplyJobWithEdit(context, jobToEdit!!, newPrompt, newMask, newSteps)
                showJobEditModal = false
            },
            onSave = {
                editorViewModel.saveJobResultToGallery(context, jobToEdit!!)
                showJobEditModal = false
            },
            onCancel = { showJobEditModal = false }
        )
    }

    if (showJobQueuePanel) {
        JobQueue3DPanel(
            jobs = state.inpaintJobs,
            onDismiss = { showJobQueuePanel = false },
            onShowJob = { job ->
                editorViewModel.loadJobResultAsCurrent(job)
                editorViewModel.setCurrentJobId(job.jobId)
                showJobEditModal = true
                jobToEdit = job
                showJobQueuePanel = false
            },
            onDeleteJob = { job ->
                editorViewModel.deleteJob(context, job.jobId)
                editorViewModel.loadPersistentJobs(context)
            },
            onPollJob = { job ->
                editorViewModel.pollHuggingFaceJob(context, job.jobId)
            },
            onAddJob = { newJob ->
                editorViewModel.addJob(context, newJob)
            }
        )
    }

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
                onShare = {
                    bitmapToShow?.let { bmp ->
                        val file = saveBitmapToTempFile(context, bmp)
                        onShare(file)
                    }
                },
                onBrushRemove = {
                    editorViewModel.enableBrushRemove()
                },
                brushRemoveEnabled = !state.isBrushRemoveMode && !state.isCropMode && !state.showFilterScreen,
                onJobQueue = { showJobQueuePanel = true }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val compositeBitmap = state.compositeBitmap
            val showBitmap = bitmapToShow

            if (state.showFilterScreen && showBitmap != null) {
                PhotoFilterScreen(
                    originalBitmap = showBitmap,
                    onFilterApplied = { editorViewModel.onFilterApplied(it) },
                    onBack = { editorViewModel.onBackFromFilter() }
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp, start = 24.dp, end = 24.dp)
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
                            showBitmap != null -> {
                                BrushImageBox(
                                    bitmap = showBitmap,
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

                    if (state.isLoading || state.isSegmenting) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }

                    if (state.isCropMode && showBitmap != null && imageSize.width > 0 && imageSize.height > 0) {
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
                }

                if (state.bitmapResult != null && state.isResultMode) {
                    LaunchedEffect(state.bitmapResult) {
                        if (jobToEdit == null) {
                            val foundJob = state.inpaintJobs.find { it.result == state.bitmapResult }
                            if (foundJob != null) jobToEdit = foundJob
                        }
                    }
                    Row(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        GradientButton(
                            text = "Edit",
                            gradient = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF36D1C4), Color(0xFF5B86E5))
                            ),
                            onClick = {
                                jobToEdit = state.inpaintJobs.find { it.jobId == state.currentJobId }
                                if (jobToEdit != null) {
                                    showJobEditModal = true
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Nessun job selezionato per l'edit!")
                                    }
                                }
                            },
                            enabled = state.currentJobId != null,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(24.dp))

                        GradientButton(
                            text = "Salva",
                            gradient = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFF7971E), Color(0xFFFFD200))
                            ),
                            onClick = {
                                val job = state.inpaintJobs.find { it.jobId == state.currentJobId }
                                if (job != null) {
                                    editorViewModel.saveJobResultToGallery(context, job)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Immagine salvata in Galleria!")
                                    }
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Errore: nessun job da salvare!")
                                    }
                                }
                            },
                            enabled = state.currentJobId != null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

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

            if (state.showFilterScreen && showBitmap != null) {
                Spacer(Modifier.height(16.dp))
            }

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
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = Modifier
            .height(380.dp)
            .widthIn(max = 600.dp)
            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
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
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
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