package com.example.faceswapapp.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceswapapp.ui.JobQueueDataStore
import com.example.faceswapapp.utils.ImageUtils
import com.example.faceswapapp.utils.BrushMaskOverlayHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

private val TAG = "FSWAPTRACE"

enum class InpaintJobStatus { QUEUED, PROCESSING, COMPLETED, ERROR }

data class InpaintJob(
    val jobId: String,
    val prompt: String,
    val mask: Bitmap?,
    val original: Bitmap?,
    val status: InpaintJobStatus,
    val result: Bitmap? = null,
    val error: String? = null,
    val resultPath: String? = null,
    val maskPathList: List<Pair<List<Offset>, Float>>? = null
)

data class PhotoEditorUiState(
    val bitmap: Bitmap? = null,
    val bitmapInput: Bitmap? = null,
    val bitmapResult: Bitmap? = null,
    val isResultMode: Boolean = false,
    val isBrushRemoveMode: Boolean = false,
    val isLoading: Boolean = false,
    val showFilterScreen: Boolean = false,
    val snackbarMessage: String? = null,
    val isCropMode: Boolean = false,
    val cropRect: Rect = Rect(200f, 200f, 600f, 600f),
    val boxSize: IntSize = IntSize(1, 1),
    val isSegmenting: Boolean = false,
    val personBitmap: Bitmap? = null,
    val backgroundBitmap: Bitmap? = null,
    val compositeBitmap: Bitmap? = null,
    val brushPathList: List<Pair<List<Offset>, Float>> = emptyList(),
    val redoStack: List<Pair<List<Offset>, Float>> = emptyList(),
    val currentBrushSize: Float = 40f,
    val brushCanvasSize: IntSize = IntSize(1, 1),
    val brushImageOffset: Pair<Float, Float> = 0f to 0f,
    val brushImageSize: Pair<Float, Float> = 1f to 1f,
    val brushPreviewPosition: Offset? = null,
    val removeBackend: RemoveBackend = RemoveBackend.LAMA,
    val inpaintPrompt: String = "remove object",
    val numInferenceSteps: Int = 15,
    val showBrushSheet: Boolean = false,
    val showBackgroundDialog: Boolean = false,
    val photoUriForCamera: Uri? = null,
    val needsMaskReset: Boolean = false,
    val lastImageUri: Uri? = null,
    val sheetOffsetY: Float = 0f,
    val sheetHeight: Int = 0,
    val parentHeight: Int = 0,
    val inpaintOriginalBitmap: Bitmap? = null,
    val inpaintOriginalBrushPathList: List<Pair<List<Offset>, Float>> = emptyList(),
    val inpaintPreBackend: RemoveBackend? = null,
    val inpaintJobs: List<InpaintJob> = emptyList(),
    val currentJobId: String? = null // PATCH: campo per il job selezionato
)

class PhotoEditorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoEditorUiState())
    val uiState: StateFlow<PhotoEditorUiState> = _uiState

    // PATCH: funzione per cancellare un job dalla lista
    fun deleteJob(context: Context, jobId: String) {
        viewModelScope.launch {
            val newJobs = _uiState.value.inpaintJobs.filter { it.jobId != jobId }
            _uiState.update { it.copy(inpaintJobs = newJobs) }
            // aggiorna il DataStore
            val jobsPersistable = newJobs.map {
                InpaintJobPersistable(
                    jobId = it.jobId,
                    prompt = it.prompt,
                    status = when (it.status) {
                        InpaintJobStatus.QUEUED -> InpaintJobStatusPersistable.QUEUED
                        InpaintJobStatus.PROCESSING -> InpaintJobStatusPersistable.PROCESSING
                        InpaintJobStatus.COMPLETED -> InpaintJobStatusPersistable.COMPLETED
                        InpaintJobStatus.ERROR -> InpaintJobStatusPersistable.ERROR
                    },
                    error = it.error,
                    resultPath = it.resultPath,
                    maskPathList = it.maskPathList
                )
            }
            JobQueueDataStore.saveJobList(context, jobsPersistable)
        }
    }

    // PATCH: nuova funzione per impostare il job corrente
    fun setCurrentJobId(jobId: String?) {
        Log.d("DEBUG_BUTTON", "Imposto currentJobId = $jobId")
        _uiState.update { it.copy(currentJobId = jobId) }
    }

    // PATCH: Carica il risultato di un job dato un path
    fun loadJobResultByPath(resultPath: String) {
        val resultBitmap = com.example.faceswapapp.utils.ImageUtils.loadBitmapFromFile(resultPath)
        if (resultBitmap != null) {
            _uiState.update { it.copy(
                bitmap = resultBitmap,
                bitmapResult = resultBitmap,
                isResultMode = true,
                isBrushRemoveMode = false,
                snackbarMessage = "Risultato job caricato!"
            ) }
        }
    }

    fun loadPersistentJobs(context: Context) {
        viewModelScope.launch {
            val jobList = JobQueueDataStore.loadJobList(context)
            val jobs = jobList.map {
                val resultBitmap = it.resultPath?.let { path -> ImageUtils.loadBitmapFromFile(path) }
                InpaintJob(
                    jobId = it.jobId,
                    prompt = it.prompt,
                    mask = null,
                    original = null,
                    status = when (it.status) {
                        InpaintJobStatusPersistable.QUEUED -> InpaintJobStatus.QUEUED
                        InpaintJobStatusPersistable.PROCESSING -> InpaintJobStatus.PROCESSING
                        InpaintJobStatusPersistable.COMPLETED -> InpaintJobStatus.COMPLETED
                        InpaintJobStatusPersistable.ERROR -> InpaintJobStatus.ERROR
                    },
                    result = resultBitmap,
                    error = it.error,
                    resultPath = it.resultPath,
                    maskPathList = it.maskPathList
                )
            }
            _uiState.update { it.copy(inpaintJobs = jobs) }
        }
    }

    private fun persistJobsIfNeeded(context: Context) {
        viewModelScope.launch {
            val jobs = _uiState.value.inpaintJobs.map {
                InpaintJobPersistable(
                    jobId = it.jobId,
                    prompt = it.prompt,
                    status = when (it.status) {
                        InpaintJobStatus.QUEUED -> InpaintJobStatusPersistable.QUEUED
                        InpaintJobStatus.PROCESSING -> InpaintJobStatusPersistable.PROCESSING
                        InpaintJobStatus.COMPLETED -> InpaintJobStatusPersistable.COMPLETED
                        InpaintJobStatus.ERROR -> InpaintJobStatusPersistable.ERROR
                    },
                    error = it.error,
                    resultPath = it.resultPath,
                    maskPathList = it.maskPathList
                )
            }
            JobQueueDataStore.saveJobList(context, jobs)
        }
    }

    private fun resetEditModes(state: PhotoEditorUiState): PhotoEditorUiState {
        Log.d(TAG, "resetEditModes: called")
        return state.copy(
            isResultMode = false,
            isBrushRemoveMode = false,
            isCropMode = false,
            showFilterScreen = false,
            compositeBitmap = null,
            personBitmap = null,
            backgroundBitmap = null,
            showBrushSheet = false,
            brushPathList = emptyList(),
            redoStack = emptyList(),
            inpaintOriginalBitmap = null,
            inpaintOriginalBrushPathList = emptyList(),
            inpaintPreBackend = null,
            bitmapResult = null
        )
    }

    fun loadImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            Log.d(TAG, "loadImage: called with $uri")
            _uiState.update { resetEditModes(it).copy(isLoading = true) }
            val bmp = ImageUtils.loadBitmapFromUri(context, uri)
            _uiState.update {
                resetEditModes(it).copy(
                    bitmap = bmp,
                    bitmapInput = bmp,
                    isLoading = false,
                    needsMaskReset = true,
                    lastImageUri = uri
                )
            }
        }
    }

    fun showSnackbar(message: String) = _uiState.update { it.copy(snackbarMessage = message) }
    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }
    fun setBitmap(newBitmap: Bitmap) = _uiState.update { it.copy(bitmap = newBitmap, bitmapInput = newBitmap) }

    fun rotate() {
        val bmp = _uiState.value.bitmapInput ?: _uiState.value.bitmap ?: return
        val rotated = ImageUtils.rotateBitmap(bmp, 90f)
        Log.d(TAG, "rotate: rotating image")
        _uiState.update { resetEditModes(it).copy(bitmap = rotated, bitmapInput = rotated) }
    }

    fun enableCrop() = _uiState.update { it.copy(isCropMode = true) }
    fun updateCropRect(newRect: Rect) = _uiState.update { it.copy(cropRect = newRect) }
    fun updateBoxSize(newSize: IntSize) = _uiState.update { it.copy(boxSize = newSize) }
    fun applyCrop() {
        val bmp = _uiState.value.bitmapInput ?: _uiState.value.bitmap ?: return
        val cropRect = _uiState.value.cropRect
        val boxSize = _uiState.value.boxSize
        val scaleX = bmp.width.toFloat() / boxSize.width
        val scaleY = bmp.height.toFloat() / boxSize.height
        val left = (cropRect.left * scaleX).toInt().coerceIn(0, bmp.width - 1)
        val top = (cropRect.top * scaleY).toInt().coerceIn(0, bmp.height - 1)
        val right = (cropRect.right * scaleX).toInt().coerceIn(left + 1, bmp.width)
        val bottom = (cropRect.bottom * scaleY).toInt().coerceIn(top + 1, bmp.height)
        val androidRect = android.graphics.Rect(left, top, right, bottom)
        if (androidRect.width() > 0 && androidRect.height() > 0) {
            val cropped = Bitmap.createBitmap(
                bmp,
                androidRect.left,
                androidRect.top,
                androidRect.width(),
                androidRect.height()
            )
            Log.d(TAG, "applyCrop: crop done")
            _uiState.update { resetEditModes(it).copy(bitmap = cropped, bitmapInput = cropped, snackbarMessage = "Crop completato!") }
        } else {
            _uiState.update { it.copy(snackbarMessage = "Seleziona un'area valida!", isCropMode = false) }
        }
    }

    fun showFilter() = _uiState.update { it.copy(showFilterScreen = true) }
    fun onFilterApplied(filtered: Bitmap) {
        Log.d(TAG, "onFilterApplied: filter applied")
        _uiState.update { resetEditModes(it).copy(
            bitmap = filtered,
            bitmapInput = filtered,
            bitmapResult = filtered,
            isResultMode = true,
            snackbarMessage = "Filtro applicato!"
        ) }
    }
    fun onBackFromFilter() = _uiState.update { resetEditModes(it) }

    fun startSegmentPerson(context: Context, removeBgOnly: Boolean = false) {
        val bmp = _uiState.value.bitmapInput ?: _uiState.value.bitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSegmenting = true) }
            val segmented = ImageUtils.segmentPersonBitmap(context, bmp)
            if (segmented != null) {
                if (removeBgOnly) {
                    val personWithAlpha = ImageUtils.extractPersonWithAlpha(bmp, segmented)
                    _uiState.update { resetEditModes(it).copy(
                        bitmap = personWithAlpha,
                        bitmapInput = personWithAlpha,
                        bitmapResult = personWithAlpha,
                        isResultMode = true,
                        snackbarMessage = "Sfondo rimosso!",
                        isLoading = false,
                        isSegmenting = false
                    ) }
                } else {
                    _uiState.update { it.copy(personBitmap = segmented, isSegmenting = false) }
                }
            } else {
                _uiState.update { it.copy(snackbarMessage = "Errore nella segmentazione", isSegmenting = false) }
            }
        }
    }

    fun startSegmentPersonAndShowBackgroundDialog(context: Context) {
        val bmp = _uiState.value.bitmapInput ?: _uiState.value.bitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSegmenting = true) }
            val segmented = ImageUtils.segmentPersonBitmap(context, bmp)
            if (segmented != null) {
                _uiState.update { it.copy(personBitmap = segmented, isSegmenting = false, showBackgroundDialog = true) }
            } else {
                _uiState.update { it.copy(snackbarMessage = "Errore nella segmentazione", isSegmenting = false) }
            }
        }
    }

    fun setBackgroundBitmap(background: Bitmap?) {
        _uiState.update { it.copy(backgroundBitmap = background) }
        tryComposite()
    }
    fun showBackgroundDialog(show: Boolean) {
        _uiState.update { it.copy(showBackgroundDialog = show) }
    }

    fun setBackgroundImageFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val bitmap = ImageUtils.loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                _uiState.update { it.copy(backgroundBitmap = bitmap) }
                tryComposite()
            } else {
                showSnackbar("Impossibile caricare lo sfondo selezionato.")
            }
        }
    }

    private fun tryComposite() {
        val mask = _uiState.value.personBitmap
        val bg = _uiState.value.backgroundBitmap
        val original = _uiState.value.bitmapInput ?: _uiState.value.bitmap
        if (original != null && mask != null && bg != null) {
            val composite = ImageUtils.compositePersonOnBackground(
                originalBitmap = original,
                maskBitmap = mask,
                backgroundBitmap = bg
            )
            _uiState.update { it.copy(compositeBitmap = composite) }
        }
    }

    fun applyCompositing() {
        val composite = _uiState.value.compositeBitmap ?: return
        _uiState.update { resetEditModes(it).copy(bitmap = composite, bitmapInput = composite, snackbarMessage = "Sfondo applicato!") }
    }
    fun cancelCompositing() {
        _uiState.update { resetEditModes(it) }
    }

    fun enableBrushRemove() = _uiState.update { it.copy(isBrushRemoveMode = true, isResultMode = false, showBrushSheet = true) }
    fun disableBrushRemove() = _uiState.update { resetEditModes(it) }

    fun addBrushPath(pointList: List<Offset>, thickness: Float) {
        val current = _uiState.value
        _uiState.update { it.copy(brushPathList = current.brushPathList + (pointList to thickness), redoStack = emptyList()) }
    }

    fun undoBrush() {
        val current = _uiState.value
        if (current.brushPathList.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    brushPathList = current.brushPathList.dropLast(1),
                    redoStack = listOf(current.brushPathList.last()) + current.redoStack
                )
            }
        }
    }
    fun redoBrush() {
        val current = _uiState.value
        if (current.redoStack.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    brushPathList = current.brushPathList + current.redoStack.first(),
                    redoStack = current.redoStack.drop(1)
                )
            }
        }
    }
    fun resetBrush() {
        _uiState.update { it.copy(brushPathList = emptyList(), redoStack = emptyList()) }
    }
    fun updateBrushSize(newSize: Float) = _uiState.update { it.copy(currentBrushSize = newSize) }
    fun updateBrushCanvasSize(size: IntSize) = _uiState.update { it.copy(brushCanvasSize = size) }
    fun updateBrushImageBox(offset: Pair<Float, Float>, size: Pair<Float, Float>) = _uiState.update { it.copy(brushImageOffset = offset, brushImageSize = size) }
    fun updateBrushPreviewPosition(pos: Offset?) = _uiState.update { it.copy(brushPreviewPosition = pos) }
    fun setRemoveBackend(backend: RemoveBackend) = _uiState.update { it.copy(removeBackend = backend) }
    fun setInpaintPrompt(prompt: String) = _uiState.update { it.copy(inpaintPrompt = prompt) }
    fun setNumInferenceSteps(steps: Int) = _uiState.update { it.copy(numInferenceSteps = steps.coerceIn(1, 50)) }

    suspend fun translateIfNeeded(prompt: String): String {
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=en&dt=t&q=" +
                URLEncoder.encode(prompt, "UTF-8")
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext prompt
                val translated = body.split("\"")[1]
                Log.d(TAG, "TRADUZIONE: estratta: $translated")
                translated
            } catch (e: Exception) {
                prompt
            }
        }
    }

    fun applyBrushRemove(context: Context) {
        val state = _uiState.value
        val bmp = state.bitmapInput ?: state.bitmap ?: return
        if (state.brushPathList.isEmpty()) {
            showSnackbar("Disegna una maschera sull’oggetto/persona da rimuovere")
            return
        }
        val maskBitmap = BrushMaskOverlayHelper.generateMaskBitmap(
            bmp.width, bmp.height, state.brushPathList
        )
        viewModelScope.launch {
            when (state.removeBackend) {
                RemoveBackend.LOCAL -> {
                    _uiState.update { it.copy(isLoading = true) }
                    val result = ImageUtils.inpaintWithOpenCV(context, bmp, maskBitmap)
                    if (result != null) {
                        _uiState.update { it.copy(
                            bitmap = result,
                            bitmapResult = result,
                            isResultMode = true,
                            isBrushRemoveMode = false,
                            isLoading = false,
                            snackbarMessage = "Rimozione oggetto (locale OpenCV) completata!"
                        ) }
                    } else {
                        _uiState.update { it.copy(snackbarMessage = "Errore rimozione locale", isLoading = false) }
                    }
                }
                RemoveBackend.LAMA -> {
                    _uiState.update { it.copy(isLoading = true) }
                    ImageUtils.sendMaskAndImageToLamaCleaner(
                        context = context,
                        image = bmp,
                        mask = maskBitmap,
                        onSuccess = { result ->
                            _uiState.update { it.copy(
                                bitmap = result,
                                bitmapResult = result,
                                isResultMode = true,
                                isBrushRemoveMode = false,
                                isLoading = false,
                                snackbarMessage = "Oggetto rimosso (AI Lama-Cleaner)!"
                            ) }
                        },
                        onError = { errorMsg ->
                            _uiState.update { it.copy(snackbarMessage = errorMsg, isLoading = false) }
                        }
                    )
                }
                RemoveBackend.HUGGINGFACE -> {
                    val origPrompt = state.inpaintPrompt.ifBlank { "remove object" }
                    viewModelScope.launch {
                        val promptEng = translateIfNeeded(origPrompt)
                        ImageUtils.submitHuggingFaceJob(
                            context = context,
                            image = bmp,
                            mask = maskBitmap,
                            prompt = promptEng,
                            numInferenceSteps = state.numInferenceSteps,
                            onSuccess = { jobId ->
                                val newJob = InpaintJob(
                                    jobId = jobId,
                                    prompt = origPrompt,
                                    mask = maskBitmap,
                                    original = bmp,
                                    status = InpaintJobStatus.QUEUED,
                                    result = null,
                                    error = null,
                                    resultPath = null,
                                    maskPathList = state.brushPathList // PATCH: salva la maschera usata
                                )
                                Log.d(TAG, "applyBrushRemove: append job $jobId (prompt=$origPrompt). Now ${_uiState.value.inpaintJobs.size + 1} jobs")
                                _uiState.update { it.copy(
                                    inpaintJobs = it.inpaintJobs + newJob,
                                    snackbarMessage = "Job HuggingFace inviato! Apparirà nella lista job."
                                ) }
                                persistJobsIfNeeded(context)
                            },
                            onError = { errorMsg ->
                                Log.d(TAG, "applyBrushRemove: error $errorMsg")
                                _uiState.update { it.copy(snackbarMessage = errorMsg) }
                            }
                        )
                    }
                }
            }
        }
    }

    fun pollHuggingFaceJob(context: Context, jobId: String) {
        viewModelScope.launch {
            Log.d(TAG, "pollHuggingFaceJob: polling jobId=$jobId")
            _uiState.update { it.copy(
                inpaintJobs = it.inpaintJobs.map {
                    if (it.jobId == jobId) it.copy(status = InpaintJobStatus.PROCESSING) else it
                }
            )}
            ImageUtils.pollHuggingFaceJobResult(
                context = context,
                jobId = jobId,
                onSuccess = { resultBitmap ->
                    val path = ImageUtils.saveJobResultBitmapToFile(context, resultBitmap, jobId)
                    Log.d(TAG, "pollHuggingFaceJob: jobId=$jobId completed, resultPath=$path")
                    _uiState.update { it.copy(
                        inpaintJobs = it.inpaintJobs.map {
                            if (it.jobId == jobId) it.copy(
                                status = InpaintJobStatus.COMPLETED,
                                result = resultBitmap,
                                resultPath = path
                            ) else it
                        },
                        snackbarMessage = "Risultato pronto! Premi SHOW per vedere o salvare."
                    ) }
                    persistJobsIfNeeded(context)
                },
                onError = { errorMsg ->
                    Log.d(TAG, "pollHuggingFaceJob: error $errorMsg")
                    _uiState.update { it.copy(
                        inpaintJobs = it.inpaintJobs.map {
                            if (it.jobId == jobId) it.copy(
                                status = InpaintJobStatus.ERROR,
                                error = errorMsg
                            ) else it
                        },
                        snackbarMessage = errorMsg
                    )}
                    persistJobsIfNeeded(context)
                }
            )
        }
    }

    // PATCH: aggiorna anche currentJobId quando carichi un job!
    fun loadJobResultAsCurrent(job: InpaintJob) {
        val resultBitmap = job.result ?: (job.resultPath?.let { ImageUtils.loadBitmapFromFile(it) })
        Log.d(TAG, "loadJobResultAsCurrent: Loading result for jobId=${job.jobId}, bitmap loaded: ${resultBitmap != null}")
        if (resultBitmap != null) {
            _uiState.update { it.copy(
                bitmap = resultBitmap,
                bitmapResult = resultBitmap,
                isResultMode = true,
                isBrushRemoveMode = false,
                snackbarMessage = "Risultato caricato!",
                currentJobId = job.jobId // PATCH: salva qui il currentJobId!
            ) }
        }
    }

    fun restoreBrushEditing() = _uiState.update { it.copy(bitmap = it.bitmapInput, isBrushRemoveMode = true, isResultMode = false) }

    fun save(context: Context) {
        val bmp = _uiState.value.bitmapResult ?: _uiState.value.bitmapInput ?: _uiState.value.bitmap
        if (bmp != null) {
            ImageUtils.saveToGallery(context, bmp) { ok ->
                _uiState.update {
                    resetEditModes(it).copy(
                        snackbarMessage = if (ok) "Immagine salvata!" else "Errore nel salvataggio",
                        bitmap = bmp,
                        bitmapInput = bmp
                    )
                }
            }
        } else {
            _uiState.update { it.copy(snackbarMessage = "Nessuna immagine da salvare!") }
        }
    }

    fun addJob(context: Context, job: InpaintJob) {
        _uiState.update { it.copy(inpaintJobs = it.inpaintJobs + job) }
        persistJobsIfNeeded(context)
    }

    fun reapplyJobWithEdit(
        context: Context,
        job: InpaintJob,
        newPrompt: String,
        newBrushList: List<Pair<List<Offset>, Float>>,
        newSteps: Int
    ) {
        val bmp = job.original ?: _uiState.value.bitmapInput ?: _uiState.value.bitmap
        if (bmp == null) {
            showSnackbar("Bitmap originale non trovata!")
            return
        }
        if (newBrushList.isEmpty()) {
            showSnackbar("Disegna una nuova maschera!")
            return
        }
        val maskBitmap = BrushMaskOverlayHelper.generateMaskBitmap(
            bmp.width, bmp.height, newBrushList
        )
        viewModelScope.launch {
            val promptEng = translateIfNeeded(newPrompt)
            ImageUtils.submitHuggingFaceJob(
                context = context,
                image = bmp,
                mask = maskBitmap,
                prompt = promptEng,
                numInferenceSteps = newSteps,
                onSuccess = { jobId ->
                    val newJob = InpaintJob(
                        jobId = jobId,
                        prompt = newPrompt,
                        mask = maskBitmap,
                        original = bmp,
                        status = InpaintJobStatus.QUEUED,
                        result = null,
                        error = null,
                        resultPath = null,
                        maskPathList = newBrushList // PATCH: salva la nuova maschera
                    )
                    _uiState.update { it.copy(
                        inpaintJobs = it.inpaintJobs + newJob,
                        snackbarMessage = "Nuovo job inviato con le modifiche!"
                    ) }
                    persistJobsIfNeeded(context)
                },
                onError = { errorMsg ->
                    _uiState.update { it.copy(snackbarMessage = errorMsg) }
                }
            )
        }
    }

    fun saveJobResultToGallery(context: Context, job: InpaintJob) {
        val bmp = job.result ?: job.resultPath?.let { ImageUtils.loadBitmapFromFile(it) }
        if (bmp != null) {
            ImageUtils.saveToGallery(context, bmp) { ok ->
                _uiState.update {
                    it.copy(
                        snackbarMessage = if (ok) "Risultato salvato!" else "Errore nel salvataggio"
                    )
                }
            }
        } else {
            _uiState.update { it.copy(snackbarMessage = "Nessun risultato da salvare!") }
        }
    }
}