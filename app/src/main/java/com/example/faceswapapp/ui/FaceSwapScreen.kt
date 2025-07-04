package com.example.faceswapapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.BoxWithConstraints
import com.example.faceswapapp.*
import com.example.faceswapapp.utils.ImageUtils
import com.example.faceswapapp.ui.theme.FaceSwapAppTheme
import kotlinx.coroutines.launch
import java.io.File
import com.example.faceswapapp.ui.StickerPicker
import com.example.faceswapapp.ui.PhotoEditorActivity
import com.example.faceswapapp.viewmodel.InpaintJob

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
        UnifiedScreen(
            appTheme = appTheme,
            onThemeChange = { appTheme = it },
            onOpenPhotoEditor = onOpenPhotoEditor
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedScreen(
    appTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    onOpenPhotoEditor: () -> Unit = {}
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var detectedLandmarks by remember { mutableStateOf<List<List<Offset>>?>(null) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentFaceIndex by remember { mutableStateOf(-1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorDialog by remember { mutableStateOf(false) }
    var bitmapVersion by remember { mutableStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var aboutDialog by remember { mutableStateOf(false) }
    var themeDialog by remember { mutableStateOf(false) }
    var saveDialog by remember { mutableStateOf(false) }
    var debugMenuDialog by remember { mutableStateOf(false) }
    var facePreviews by remember { mutableStateOf<List<Bitmap>?>(null) }
    var showLandmarks by remember { mutableStateOf(true) }
    var placedStickers by remember { mutableStateOf<List<PlacedSticker>>(listOf()) }
    var selectedStickerIndex by remember { mutableStateOf<Int?>(null) }
    val selectedSticker: PlacedSticker? = selectedStickerIndex?.let { idx -> placedStickers.getOrNull(idx) }
    var stickerPickerOpen by remember { mutableStateOf(false) }
    var stickerToolsDialogOpen by remember { mutableStateOf(false) }
    var previewBoxWidthPx by remember { mutableStateOf(0f) }
    var previewBoxHeightPx by remember { mutableStateOf(0f) }

    // PATCH: stato per mostrare il pannello JobQueue
    var showJobQueuePanel by remember { mutableStateOf(false) }
    // PATCH: stato per mostrare azioni su risultato job
    var selectedJob: InpaintJob? by remember { mutableStateOf(null) }
    var showResultActions by remember { mutableStateOf(false) }
    var showJobEditModal by remember { mutableStateOf(false) }

    // Importa il ViewModel dei job
    val editorViewModel = remember { com.example.faceswapapp.viewmodel.PhotoEditorViewModel() }
    val jobState by editorViewModel.uiState.collectAsState()
    val jobContext = LocalContext.current

    // PATCH: carica i job persistenti all'avvio della schermata principale
    LaunchedEffect(Unit) {
        editorViewModel.loadPersistentJobs(jobContext)
    }

    fun mergeBitmapWithStickers(
        baseBitmap: Bitmap,
        stickers: List<PlacedSticker>,
        landmarks: List<Offset>?,
        context: Context
    ): Bitmap {
        val result = Bitmap.createBitmap(baseBitmap.width, baseBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(baseBitmap, 0f, 0f, null)
        if (landmarks == null || landmarks.isEmpty()) {
            stickers.forEach { sticker ->
                val stickerBitmap = BitmapFactory.decodeResource(context.resources, sticker.sticker.resId)
                val centerX = baseBitmap.width / 2f + sticker.x
                val centerY = baseBitmap.height / 2f + sticker.y
                val matrix = Matrix().apply {
                    postTranslate(-stickerBitmap.width / 2f, -stickerBitmap.height / 2f)
                    postScale(sticker.scale, sticker.scale)
                    postRotate(sticker.rotation)
                    postTranslate(centerX, centerY)
                }
                canvas.drawBitmap(stickerBitmap, matrix, null)
            }
            return result
        }
        val stickerBitmaps = stickers.associate { it.sticker.resId to
                BitmapFactory.decodeResource(context.resources, it.sticker.resId)
        }
        stickers.forEach { placed ->
            val stickerBitmap = stickerBitmaps[placed.sticker.resId] ?: return@forEach
            val matrix = getStickerMatrixNoScale(
                sticker = placed.sticker,
                bitmap = stickerBitmap,
                landmarks = landmarks,
                offsetX = placed.x,
                offsetY = placed.y,
                userScale = placed.scale,
                userRotation = placed.rotation
            )
            canvas.drawBitmap(stickerBitmap, matrix, null)
        }
        return result
    }

    fun addSticker(sticker: Sticker) {
        placedStickers = placedStickers + PlacedSticker(sticker)
        selectedStickerIndex = placedStickers.lastIndex
        stickerToolsDialogOpen = true
    }
    fun selectSticker(idx: Int) {
        selectedStickerIndex = idx
        placedStickers = placedStickers.mapIndexed { i, s -> s.copy(isSelected = i == idx) }
        stickerToolsDialogOpen = true
    }
    fun removeSticker(idx: Int) {
        placedStickers = placedStickers.filterIndexed { i, _ -> i != idx }
        selectedStickerIndex = if (placedStickers.isNotEmpty()) {
            (if (idx == 0) 0 else idx - 1).coerceAtLeast(0)
        } else null
        stickerToolsDialogOpen = placedStickers.isNotEmpty() && selectedStickerIndex != null
    }
    fun updateSelectedSticker(x: Float? = null, y: Float? = null, scale: Float? = null, rot: Float? = null) {
        selectedStickerIndex?.let { idx ->
            placedStickers = placedStickers.mapIndexed { i, s ->
                if (i == idx) s.copy(
                    x = x ?: s.x,
                    y = y ?: s.y,
                    scale = scale ?: s.scale,
                    rotation = rot ?: s.rotation
                ) else s
            }
        }
    }
    fun resetSelectedSticker() {
        selectedStickerIndex?.let { idx ->
            placedStickers = placedStickers.mapIndexed { i, s ->
                if (i == idx) s.copy(x = 0f, y = 0f, scale = 1f, rotation = 0f) else s
            }
        }
    }
    fun showSnackbar(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) errorDialog = true
    }

    fun fullReset() {
        selectedImageUri = null
        detectedLandmarks = null
        currentBitmap = null
        currentFaceIndex = -1
        errorMessage = null
        bitmapVersion++
        isProcessing = false
        facePreviews = null
        placedStickers = listOf()
        selectedStickerIndex = null
        showLandmarks = true
    }

    fun clearDebugImages(context: Context) {
        val filesDir = context.filesDir
        val files = filesDir.listFiles { file ->
            file.name.startsWith("debug_") && file.name.endsWith(".png")
        } ?: return
        for (file in files) file.delete()
        val logFile = File(filesDir, "face_swap_debug.txt")
        logFile.delete()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        clearDebugImages(context)
        fullReset()
        val uri = result.data?.data
        selectedImageUri = uri
        if (uri != null) {
            isProcessing = true
            coroutineScope.launch {
                val loaded = ImageUtils.loadBitmapFromUri(context, uri)
                currentBitmap = loaded
                bitmapVersion++
                if (loaded != null) {
                    ImageUtils.detectFaceLandmarksFromUri(context, uri) { landmarks ->
                        detectedLandmarks = landmarks
                        currentFaceIndex = -1
                        isProcessing = false
                        if (!landmarks.isNullOrEmpty()) {
                            facePreviews = landmarks.map { lmk ->
                                ImageUtils.cropFaceFromLandmarks(loaded, lmk)
                            }
                            errorMessage = null
                        } else {
                            facePreviews = null
                            errorMessage = """
                                Nessun volto rilevato nell'immagine.
                                Suggerimenti:
                                - Assicurati che il volto sia visibile e frontale.
                                - Evita occhiali grandi, cappelli o oggetti che coprono il viso.
                                - Usa immagini ben illuminate e non troppo scure o sfocate.
                                - Prova a cambiare immagine o scattarne una nuova.
                            """.trimIndent()
                        }
                        bitmapVersion++
                        placedStickers = listOf()
                        selectedStickerIndex = null
                    }
                } else {
                    errorMessage = "Errore nel caricamento dell'immagine."
                    isProcessing = false
                    bitmapVersion++
                }
            }
        }
    }

    val faceSwapLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        uri?.let {
            coroutineScope.launch {
                val baseBitmap = currentBitmap
                if (baseBitmap == null || detectedLandmarks == null || currentFaceIndex < 0 || isProcessing) {
                    errorMessage = "Errore: bitmap di base o landmarks non presenti."
                    bitmapVersion++
                    return@launch
                }
                isProcessing = true
                val faceBitmap = ImageUtils.loadBitmapFromUri(context, it)
                if (faceBitmap != null) {
                    val sourceLandmarksRaw = ImageUtils.detectLandmarksForFace(context, faceBitmap)
                    val targetLandmarksRaw = detectedLandmarks?.get(currentFaceIndex)
                    val sourceLandmarks468 = sourceLandmarksRaw.take(468)
                    val targetLandmarks468 = targetLandmarksRaw?.take(468)
                    if (
                        sourceLandmarks468.size == 468 &&
                        targetLandmarks468 != null &&
                        targetLandmarks468.size == 468
                    ) {
                        val scaleX = baseBitmap.width.toFloat() / faceBitmap.width
                        val scaleY = baseBitmap.height.toFloat() / faceBitmap.height
                        val scaledSourceLandmarks = sourceLandmarks468.map { orig ->
                            Offset(orig.x * scaleX, orig.y * scaleY)
                        }
                        val targetLandmarksPoints = FaceSwapUtils.offsetsToPoints(targetLandmarks468)
                        val sourceLandmarksPoints = FaceSwapUtils.offsetsToPoints(scaledSourceLandmarks)

                        // PATCH: Filtra punti fuori dall'immagine per evitare crash OpenCV
                        val imgW = baseBitmap.width.toDouble()
                        val imgH = baseBitmap.height.toDouble()
                        val filteredTargetPoints = targetLandmarksPoints.filter {
                            it.x.toDouble() in 0.0..imgW && it.y.toDouble() in 0.0..imgH
                        }
                        if (filteredTargetPoints.size < 3) {
                            errorMessage = "Landmark non validi per la triangolazione. Riprova con un'altra immagine o assicurati che il volto sia ben visibile."
                            isProcessing = false
                            bitmapVersion++
                            return@launch
                        }
                        val triangles = try {
                            FaceSwapUtils.calculateDelaunayTriangles(baseBitmap.width, baseBitmap.height, filteredTargetPoints)
                        } catch (e: Exception) {
                            errorMessage = "Errore durante la triangolazione del volto (OpenCV): landmark fuori range. Prova con un'altra immagine."
                            isProcessing = false
                            bitmapVersion++
                            return@launch
                        }

                        // PATCH: Conversione esplicita a Double!
                        val hullPoints = org.opencv.core.MatOfPoint(
                            *filteredTargetPoints.map { org.opencv.core.Point(it.x.toDouble(), it.y.toDouble()) }.toTypedArray()
                        )
                        val hullIndices = org.opencv.core.MatOfInt()
                        org.opencv.imgproc.Imgproc.convexHull(hullPoints, hullIndices)
                        val hullList = hullIndices.toArray().map { hullPoints.toArray()[it] }
                        val hullMat = org.opencv.core.MatOfPoint(*hullList.toTypedArray())

                        val debugBitmap = ImageUtils.loadLastDebugStep6Bitmap(context, baseBitmap.width, baseBitmap.height)
                        if (debugBitmap != null) {
                            currentBitmap = debugBitmap
                        } else {
                            val swapped = FaceSwapUtils.swapFaceWithTriangles(
                                source = faceBitmap,
                                dest = baseBitmap,
                                sourceLandmarks = sourceLandmarksPoints,
                                destLandmarks = filteredTargetPoints,
                                triangles = triangles,
                                maskHull = hullMat,
                                context = context
                            )
                            currentBitmap = Bitmap.createBitmap(swapped)
                        }
                        detectedLandmarks = null
                        currentFaceIndex = -1
                        placedStickers = listOf()
                        selectedStickerIndex = null
                        showLandmarks = true
                        errorMessage = null
                        bitmapVersion++
                        facePreviews = null
                    } else {
                        errorMessage = """
                            Landmark insufficienti per lo swap.
                            Prova con immagini in cui il volto sia più visibile e frontale.
                        """.trimIndent()
                        bitmapVersion++
                    }
                } else {
                    errorMessage = "Errore durante il caricamento dell'immagine per il face swap."
                    bitmapVersion++
                }
                isProcessing = false
            }
        }
    }

    fun showLastDebugImage(context: Context) {
        val debugBitmap = ImageUtils.loadLastDebugStep6Bitmap(context, currentBitmap?.width ?: 300, currentBitmap?.height ?: 300)
        if (debugBitmap != null) {
            currentBitmap = debugBitmap
            bitmapVersion++
            detectedLandmarks = null
            facePreviews = null
            placedStickers = listOf()
            selectedStickerIndex = null
            showSnackbar("Immagine di debug caricata.")
        } else {
            showSnackbar("Nessuna immagine di debug trovata")
        }
    }

    var popupOffsetX by remember { mutableStateOf(0f) }
    var popupOffsetY by remember { mutableStateOf(0f) }

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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(top = 16.dp, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Begin PATCH: uniforma canvas come filterimg
                    if (currentBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = Color.White,
                            shadowElevation = 12.dp,
                            modifier = Modifier
                                .height(380.dp)
                                .widthIn(max = 600.dp)
                                .aspectRatio(currentBitmap!!.width.toFloat() / currentBitmap!!.height.toFloat())
                        ) {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (detectedLandmarks != null && currentFaceIndex in detectedLandmarks!!.indices) {
                                    StickerOverlay(
                                        photoBitmap = currentBitmap!!,
                                        placedStickers = placedStickers,
                                        landmarks = detectedLandmarks!![currentFaceIndex],
                                        imageWidth = currentBitmap!!.width,
                                        imageHeight = currentBitmap!!.height,
                                        showLandmarks = showLandmarks
                                    )
                                } else {
                                    Image(
                                        bitmap = currentBitmap!!.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    } else {
                        BoxWithConstraints(
                            modifier = Modifier
                                .size(380.dp)
                                .shadow(12.dp, RoundedCornerShape(32.dp))
                                .background(Color.White, RoundedCornerShape(32.dp))
                        ) {
                            val density = LocalDensity.current
                            val wPx = with(density) { maxWidth.toPx() }
                            val hPx = with(density) { maxHeight.toPx() }
                            LaunchedEffect(wPx, hPx) {
                                previewBoxWidthPx = wPx
                                previewBoxHeightPx = hPx
                            }
                            Box(
                                modifier = Modifier
                                    .width(maxWidth)
                                    .height(maxHeight)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 6.dp,
                                        modifier = Modifier.size(64.dp)
                                    )
                                } else {
                                    Text(
                                        text = errorMessage ?: "Seleziona un'immagine per iniziare",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                            }
                        }
                    }
                    // End PATCH
                }

                // BARRA PREVIEW FACCIA
                if (facePreviews != null && facePreviews!!.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 8.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        FaceButtonsBarWithPreview(
                            facePreviews = facePreviews,
                            enabled = !isProcessing,
                            onFaceButtonClick = { faceIndex ->
                                if (!isProcessing) {
                                    currentFaceIndex = faceIndex
                                    faceSwapLauncher.launch(Intent(Intent.ACTION_PICK).apply { type = "image/*" })
                                    selectedStickerIndex = null
                                    placedStickers = listOf()
                                }
                            }
                        )
                    }
                }

                // BARRA STICKER: estratta in composable dedicato per migliorare la manutenibilità
                if (detectedLandmarks != null && currentFaceIndex in detectedLandmarks!!.indices && (placedStickers.isNotEmpty() || currentBitmap != null)) {
                    StickerBar(
                        placedStickers = placedStickers,
                        selectedStickerIndex = selectedStickerIndex,
                        onSelectSticker = ::selectSticker,
                        onAddSticker = { stickerPickerOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // PANNELLO STRUMENTI STICKER: estratto in composable dedicato per migliorare la manutenibilità
                if (selectedSticker != null && stickerToolsDialogOpen) {
                    StickerToolsPanel(
                        selectedSticker = selectedSticker,
                        popupOffsetX = popupOffsetX,
                        popupOffsetY = popupOffsetY,
                        onOffsetChange = { newX, newY ->
                            popupOffsetX = newX
                            popupOffsetY = newY
                        },
                        onUpdateSticker = ::updateSelectedSticker,
                        onResetSticker = ::resetSelectedSticker,
                        onRemoveSticker = {
                            selectedStickerIndex?.let { idx ->
                                removeSticker(idx)
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
                                        addSticker(stk)
                                        stickerPickerOpen = false
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
                /*
                // PATCH: BOTTONI 3D EDIT/SALVA RISULTATO JOB
                if (selectedJob != null && showResultActions) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        GradientButton(
                            text = "Edit",
                            gradient = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF36D1C4), Color(0xFF5B86E5))
                            ),
                            onClick = {
                                showJobEditModal = true
                                showResultActions = false
                            },
                            enabled = true,
                            modifier = Modifier.weight(1f)
                        )
                        GradientButton(
                            text = "Salva",
                            gradient = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFF7971E), Color(0xFFFFD200))
                            ),
                            onClick = {
                                editorViewModel.saveJobResultToGallery(context, selectedJob!!)
                                showResultActions = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Immagine salvata in Galleria!")
                                }
                                selectedJob = null
                            },
                            enabled = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                */
                // AZIONI PRINCIPALI: bottoni in basso, equidistanti, gradient colorati chic!
                // ... codice precedente invariato ...

// AZIONI PRINCIPALI: bottoni in basso, equidistanti, gradient colorati chic!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GradientButton(
                            text = "Foto Editor",
                            gradient = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF70C1B3), Color(0xFFB2DBBF))
                            ),
                            onClick = { onOpenPhotoEditor() },
                            enabled = !isProcessing,
                            modifier = Modifier.weight(1f)
                        )
                        GradientButton(
                            text = "Swap Face",
                            gradient = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF5B86E5), Color(0xFF36D1C4))
                            ),
                            onClick = { if (!isProcessing) galleryLauncher.launch(Intent(Intent.ACTION_PICK).apply { type = "image/*" }) },
                            enabled = !isProcessing,
                            modifier = Modifier.weight(1f)
                        )
                        GradientButton(
                            text = "Show Job",
                            gradient = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFF7971E), Color(0xFFFFD200))
                            ),
                            onClick = {
                                // PATCH: Forza reload lista job dal DataStore prima di mostrare il pannello
                                editorViewModel.loadPersistentJobs(jobContext)
                                showJobQueuePanel = true
                            },
                            enabled = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // PATCH: Mostra JobQueue3DPanel se richiesto
        if (showJobQueuePanel) {
            JobQueue3DPanel(
                jobs = jobState.inpaintJobs,
                onShowJob = { job ->
                    //editorViewModel.loadJobResultAsCurrent(job)
                    selectedJob = job
                    showJobQueuePanel = false
                    showResultActions = true
                },
                onDeleteJob = { job -> editorViewModel.deleteJob(jobContext, job.jobId) },
                onPollJob = { job -> editorViewModel.pollHuggingFaceJob(jobContext, job.jobId) },
                onAddJob = { newJob -> editorViewModel.addJob(jobContext, newJob) },
                onDismiss = { showJobQueuePanel = false }
            )
        }

        // PATCH: Mostra JobQueue3DPanel se richiesto
        if (showJobQueuePanel) {
            JobQueue3DPanel(
                jobs = jobState.inpaintJobs,
                onShowJob = { job ->
                    //editorViewModel.loadJobResultAsCurrent(job)
                    // PATCH: Apri l'editor avanzato passando il path del risultato del job
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

        // PATCH: Mostra modale di editing job se richiesto
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

        if (errorDialog && errorMessage != null) {
            AlertDialog(
                onDismissRequest = { errorDialog = false },
                title = { Text("Errore") },
                text = { Text(errorMessage ?: "Errore sconosciuto") },
                confirmButton = {
                    TextButton(onClick = { errorDialog = false; errorMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }

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
                        if (currentBitmap != null) {
                            val merged = mergeBitmapWithStickers(
                                currentBitmap!!,
                                placedStickers,
                                detectedLandmarks?.getOrNull(currentFaceIndex),
                                context
                            )
                            coroutineScope.launch {
                                ImageUtils.saveToGallery(context, merged) { ok ->
                                    showSnackbar(
                                        if (ok) "Immagine con sticker salvata in Galleria!" else "Errore nel salvataggio!"
                                    )
                                }
                            }
                        }
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
                                showLastDebugImage(context)
                                debugMenuDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text("Mostra debug finale")
                        }
                        Button(
                            onClick = {
                                clearDebugImages(context)
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
                                checked = showLandmarks,
                                onCheckedChange = {
                                    showLandmarks = it
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

// --- Composable custom per bottone gradiente chic ---
@Composable
fun GradientButton(
    text: String,
    gradient: Brush,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = 6.dp)
            .shadow(9.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.LightGray.copy(alpha = 0.38f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 9.dp,
            pressedElevation = 3.dp,
            disabledElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .background(
                    brush = if (enabled) gradient else Brush.horizontalGradient(listOf(Color.LightGray, Color.Gray)),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (enabled) Color.White else Color.DarkGray,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 3.dp, horizontal = 8.dp)
            )
        }
    }
}