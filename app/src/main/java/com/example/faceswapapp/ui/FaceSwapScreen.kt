package com.example.faceswapapp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.faceswapapp.PlacedSticker
import com.example.faceswapapp.Sticker
import com.example.faceswapapp.availableStickers
import com.example.faceswapapp.ui.sticker.StickerBar
import com.example.faceswapapp.ui.sticker.StickerToolsPanel
import com.example.faceswapapp.ui.theme.FaceSwapAppTheme
import com.example.faceswapapp.viewmodel.FaceSwapViewModel
import com.example.faceswapapp.viewmodel.PhotoEditorViewModel
import com.example.faceswapapp.viewmodel.InpaintJob
import kotlinx.coroutines.launch

enum class AppTheme { LIGHT, DARK, SYSTEM }

@Composable
fun FaceSwapScreen(
    onOpenPhotoEditor: () -> Unit = {}
) {
    var appTheme by remember { mutableStateOf(AppTheme.SYSTEM) }
    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    FaceSwapAppTheme(darkTheme = darkTheme) {
        UnifiedScreenVM(
            appTheme = appTheme,
            onThemeChange = { appTheme = it },
            onOpenPhotoEditor = onOpenPhotoEditor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedScreenVM(
    appTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    onOpenPhotoEditor: () -> Unit = {},
    faceSwapViewModel: FaceSwapViewModel = viewModel(),
    editorViewModel: PhotoEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val jobContext = context
    val jobState by editorViewModel.uiState.collectAsState()

    // Dialog state
    var aboutDialog by remember { mutableStateOf(false) }
    var themeDialog by remember { mutableStateOf(false) }
    var saveDialog by remember { mutableStateOf(false) }
    var debugMenuDialog by remember { mutableStateOf(false) }
    var errorDialog by remember { mutableStateOf(false) }

    // Job panel/modal state
    var showJobQueuePanel by remember { mutableStateOf(false) }
    var selectedJob: InpaintJob? by remember { mutableStateOf(null) }
    var showResultActions by remember { mutableStateOf(false) }
    var showJobEditModal by remember { mutableStateOf(false) }

    // Sticker/Panel state
    var stickerPickerOpen by remember { mutableStateOf(false) }
    var stickerToolsDialogOpen by remember { mutableStateOf(false) }
    var popupOffsetX by remember { mutableStateOf(0f) }
    var popupOffsetY by remember { mutableStateOf(0f) }

    // Face preview state
    var previewBoxWidthPx by remember { mutableStateOf(0f) }
    var previewBoxHeightPx by remember { mutableStateOf(0f) }

    val selectedSticker: PlacedSticker? = faceSwapViewModel.selectedStickerIndex?.let { idx ->
        faceSwapViewModel.placedStickers.getOrNull(idx)
    }

    // Launcher per carica nuova immagine (come "Swap Face" originale)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            coroutineScope.launch {
                faceSwapViewModel.handleSwapFace(context, uri)
            }
        }
    }

    // Launcher per face swap di una faccia specifica
    val faceSwapLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) coroutineScope.launch {
            faceSwapViewModel.handleFaceSwapOnFace(context, uri, snackbarHostState)
        }
    }

    // Carica job dal DataStore all'avvio
    LaunchedEffect(Unit) {
        editorViewModel.loadPersistentJobs(jobContext)
    }

    // Mostra error dialog se errore
    LaunchedEffect(faceSwapViewModel.errorMessage) {
        if (faceSwapViewModel.errorMessage != null) errorDialog = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FaceSwap", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { aboutDialog = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Info")
                    }
                    IconButton(onClick = { themeDialog = true }) {
                        Text(
                            when (appTheme) {
                                AppTheme.DARK -> "🌙"
                                AppTheme.LIGHT -> "🌞"
                                AppTheme.SYSTEM -> "🖥️"
                            }
                        )
                    }
                    IconButton(onClick = { debugMenuDialog = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // PREVIEW ZONE
                FacePreview(
                    currentBitmap = faceSwapViewModel.currentBitmap,
                    detectedLandmarks = faceSwapViewModel.detectedLandmarks,
                    currentFaceIndex = faceSwapViewModel.currentFaceIndex,
                    placedStickers = faceSwapViewModel.placedStickers,
                    showLandmarks = faceSwapViewModel.showLandmarks,
                    isProcessing = faceSwapViewModel.isProcessing,
                    errorMessage = faceSwapViewModel.errorMessage,
                    previewBoxWidthPx = { previewBoxWidthPx = it },
                    previewBoxHeightPx = { previewBoxHeightPx = it }
                )

                // BARRA PREVIEW FACCIA
                if (faceSwapViewModel.facePreviews != null && faceSwapViewModel.facePreviews!!.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 8.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (faceSwapViewModel.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            FaceButtonsBarWithPreview(
                                facePreviews = faceSwapViewModel.facePreviews,
                                enabled = true,
                                onFaceButtonClick = { faceIndex ->
                                    if (!faceSwapViewModel.isProcessing) {
                                        faceSwapViewModel.updateCurrentFaceIndex(faceIndex)
                                        faceSwapLauncher.launch(Intent(Intent.ACTION_PICK).apply { type = "image/*" })
                                        faceSwapViewModel.updateStickerIndex(null)
                                        faceSwapViewModel.placedStickers = emptyList<PlacedSticker>()
                                    }
                                }
                            )
                        }
                    }
                }

                // Sticker bar
                if (
                    faceSwapViewModel.detectedLandmarks != null &&
                    faceSwapViewModel.currentFaceIndex in (faceSwapViewModel.detectedLandmarks?.indices ?: 0..-1) &&
                    (faceSwapViewModel.placedStickers.isNotEmpty() || faceSwapViewModel.currentBitmap != null)
                ) {
                    StickerBar(
                        placedStickers = faceSwapViewModel.placedStickers,
                        selectedStickerIndex = faceSwapViewModel.selectedStickerIndex,
                        onSelectSticker = {
                            faceSwapViewModel.selectSticker(it)
                            stickerToolsDialogOpen = true
                        },
                        onAddSticker = { stickerPickerOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Sticker tools
                if (selectedSticker != null && stickerToolsDialogOpen) {
                    StickerToolsPanel(
                        selectedSticker = selectedSticker,
                        popupOffsetX = popupOffsetX,
                        popupOffsetY = popupOffsetY,
                        onOffsetChange = { newX, newY ->
                            popupOffsetX = newX
                            popupOffsetY = newY
                        },
                        onUpdateSticker = { x, y, scale, rot ->
                            faceSwapViewModel.updateSelectedSticker(x, y, scale, rot)
                        },
                        onResetSticker = { faceSwapViewModel.resetSelectedSticker() },
                        onRemoveSticker = {
                            faceSwapViewModel.selectedStickerIndex?.let { idx ->
                                faceSwapViewModel.removeSticker(idx)
                                stickerToolsDialogOpen = false
                            }
                        },
                        onClose = { stickerToolsDialogOpen = false }
                    )
                }

                if (stickerPickerOpen) {
                    AlertDialog(
                        onDismissRequest = { stickerPickerOpen = false },
                        title = { Text("Scegli uno sticker") },
                        text = {
                            StickerPicker(
                                stickers = availableStickers,
                                selectedSticker = null,
                                onStickerSelected = { stk ->
                                    if (stk != null) {
                                        faceSwapViewModel.addSticker(stk)
                                        stickerPickerOpen = false
                                        stickerToolsDialogOpen = true
                                    }
                                }
                            )
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { stickerPickerOpen = false }) { Text("Annulla") }
                        }
                    )
                }

                // AZIONI PRINCIPALI - PATCH: usa MainActionsBar!
                MainActionsBar(
                    onOpenPhotoEditor = { onOpenPhotoEditor() },
                    onSwapFace = {
                        if (!faceSwapViewModel.isProcessing)
                            galleryLauncher.launch(Intent(Intent.ACTION_PICK).apply { type = "image/*" })
                    },
                    onShowJob = {
                        editorViewModel.loadPersistentJobs(jobContext)
                        showJobQueuePanel = true
                    },
                    isProcessing = faceSwapViewModel.isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                )
            }
        }

        // JOB PANEL
        if (showJobQueuePanel) {
            JobQueue3DPanel(
                jobs = jobState.inpaintJobs,
                onShowJob = { job ->
                    val intent = Intent(context, PhotoEditorActivity::class.java).apply {
                        putExtra(PhotoEditorActivity.EXTRA_JOB_ID, job.jobId)
                        putExtra(PhotoEditorActivity.EXTRA_JOB_RESULT_PATH, job.resultPath ?: "")
                    }
                    context.startActivity(intent)
                    showJobQueuePanel = false
                },
                onDeleteJob = { job -> editorViewModel.deleteJob(jobContext, job.jobId) },
                onPollJob = { job -> editorViewModel.pollHuggingFaceJob(jobContext, job.jobId) },
                onAddJob = { newJob -> editorViewModel.addJob(jobContext, newJob) },
                onDismiss = { showJobQueuePanel = false }
            )
        }

        // JOB EDIT MODAL (stub)
        if (showJobEditModal && selectedJob != null) {
            JobEditModal(
                job = selectedJob!!,
                onApply = { prompt, brushPathList, steps ->
                    editorViewModel.reapplyJobWithEdit(
                        context = context,
                        job = selectedJob!!,
                        newPrompt = prompt,
                        newBrushList = brushPathList,
                        newSteps = steps
                    )
                    showJobEditModal = false
                    selectedJob = null
                },
                onSave = {
                    showJobEditModal = false
                    selectedJob = null
                },
                onCancel = {
                    showJobEditModal = false
                    selectedJob = null
                }
            )
        }

        // ERROR DIALOG
        if (errorDialog && faceSwapViewModel.errorMessage != null) {
            AlertDialog(
                onDismissRequest = { errorDialog = false; faceSwapViewModel.updateError(null) },
                title = { Text("Errore") },
                text = { Text(faceSwapViewModel.errorMessage ?: "Errore sconosciuto") },
                confirmButton = {
                    TextButton(onClick = { errorDialog = false; faceSwapViewModel.updateError(null) }) {
                        Text("OK")
                    }
                }
            )
        }

        // ABOUT/TEMA/SAVE/DEBUG DIALOGS
        if (aboutDialog) {
            AlertDialog(
                onDismissRequest = { aboutDialog = false },
                title = { Text("FaceSwapApp - Info") },
                text = {
                    Column {
                        Text("Autore: ProphetTheBest\nVersione: 1.0.0")
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ProphetTheBest/FaceSwapApp"))
                            context.startActivity(intent)
                        }) {
                            Text("Vai al repository su GitHub")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { aboutDialog = false }) { Text("OK") }
                }
            )
        }
        if (themeDialog) {
            AlertDialog(
                onDismissRequest = { themeDialog = false },
                title = { Text("Tema") },
                text = {
                    Column {
                        ThemeRadioButton(appTheme, AppTheme.LIGHT, "Chiaro", onThemeChange)
                        ThemeRadioButton(appTheme, AppTheme.DARK, "Scuro", onThemeChange)
                        ThemeRadioButton(appTheme, AppTheme.SYSTEM, "Sistema", onThemeChange)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { themeDialog = false }) { Text("OK") }
                }
            )
        }
        if (saveDialog) {
            AlertDialog(
                onDismissRequest = { saveDialog = false },
                title = { Text("Salva in Galleria") },
                text = { Text("Vuoi salvare l'immagine risultante nella galleria (con tutti gli sticker)?") },
                confirmButton = {
                    TextButton(onClick = {
                        saveDialog = false
                        faceSwapViewModel.saveCurrentToGallery(context, snackbarHostState)
                    }) { Text("Salva") }
                },
                dismissButton = {
                    TextButton(onClick = { saveDialog = false }) { Text("Annulla") }
                }
            )
        }
        if (debugMenuDialog) {
            AlertDialog(
                onDismissRequest = { debugMenuDialog = false },
                title = { Text("Debug & Avanzate") },
                text = {
                    Column {
                        Button(
                            onClick = {
                                faceSwapViewModel.showLastDebugImage(context, snackbarHostState)
                                debugMenuDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text("Mostra debug finale")
                        }
                        Button(
                            onClick = {
                                faceSwapViewModel.clearDebugImages(context)
                                debugMenuDialog = false
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
                                checked = faceSwapViewModel.showLandmarks,
                                onCheckedChange = {
                                    faceSwapViewModel.updateShowLandmarks(it)
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { debugMenuDialog = false }) { Text("OK") }
                }
            )
        }
    }
}