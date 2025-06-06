package com.example.faceswapapp.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceswapapp.utils.ImageUtils
import com.example.faceswapapp.utils.BrushMaskOverlayHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.faceswapapp.viewmodel.RemoveBackend // enum in file separato

data class PhotoEditorUiState(
    val bitmap: Bitmap? = null,
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
    // Brush/Remove
    val isBrushRemoveMode: Boolean = false,
    val brushPathList: List<Pair<List<Offset>, Float>> = emptyList(),
    val redoStack: List<Pair<List<Offset>, Float>> = emptyList(),
    val currentBrushSize: Float = 40f,
    // Brush overlay/canvas size and offsets
    val brushCanvasSize: IntSize = IntSize(1, 1),
    val brushImageOffset: Pair<Float, Float> = 0f to 0f,
    val brushImageSize: Pair<Float, Float> = 1f to 1f,
    val brushPreviewPosition: Offset? = null,
    // Backend e prompt per rimozione oggetti
    val removeBackend: RemoveBackend = RemoveBackend.LAMA,
    val inpaintPrompt: String = "remove object",
    val showBrushSheet: Boolean = false,
    // Dialoghi/modal
    val showBackgroundDialog: Boolean = false,
    val photoUriForCamera: Uri? = null,
    val needsMaskReset: Boolean = false,
    val lastImageUri: Uri? = null,
    val sheetOffsetY: Float = 0f,
    val sheetHeight: Int = 0,
    val parentHeight: Int = 0
)

class PhotoEditorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoEditorUiState())
    val uiState: StateFlow<PhotoEditorUiState> = _uiState

    // ==== COMMON ====
    fun loadImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val bmp = ImageUtils.loadBitmapFromUri(context, uri)
            _uiState.update {
                it.copy(
                    bitmap = bmp,
                    isLoading = false,
                    compositeBitmap = null,
                    personBitmap = null,
                    backgroundBitmap = null,
                    needsMaskReset = true,
                    lastImageUri = uri
                )
            }
        }
    }

    fun showSnackbar(message: String) = _uiState.update { it.copy(snackbarMessage = message) }
    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }
    fun setBitmap(newBitmap: Bitmap) = _uiState.update { it.copy(bitmap = newBitmap) }

    // ==== ROTATE ====
    fun rotate() {
        val bmp = _uiState.value.bitmap ?: return
        val rotated = ImageUtils.rotateBitmap(bmp, 90f)
        _uiState.update {
            it.copy(
                bitmap = rotated,
                compositeBitmap = null,
                personBitmap = null,
                backgroundBitmap = null
            )
        }
    }

    // ==== CROP ====
    fun enableCrop() = _uiState.update { it.copy(isCropMode = true) }
    fun updateCropRect(newRect: Rect) = _uiState.update { it.copy(cropRect = newRect) }
    fun updateBoxSize(newSize: IntSize) = _uiState.update { it.copy(boxSize = newSize) }
    fun applyCrop() {
        val bmp = _uiState.value.bitmap ?: return
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
            _uiState.update {
                it.copy(
                    bitmap = cropped,
                    compositeBitmap = null,
                    personBitmap = null,
                    backgroundBitmap = null,
                    isCropMode = false,
                    cropRect = Rect(200f, 200f, 600f, 600f),
                    snackbarMessage = "Crop completato!"
                )
            }
        } else {
            _uiState.update { it.copy(snackbarMessage = "Seleziona un'area valida!", isCropMode = false) }
        }
    }

    // ==== FILTER ====
    fun showFilter() = _uiState.update { it.copy(showFilterScreen = true) }
    fun onFilterApplied(filtered: Bitmap) {
        _uiState.update {
            it.copy(
                bitmap = filtered,
                showFilterScreen = false,
                compositeBitmap = null,
                personBitmap = null,
                backgroundBitmap = null,
                snackbarMessage = "Filtro applicato!"
            )
        }
    }
    fun onBackFromFilter() = _uiState.update { it.copy(showFilterScreen = false) }

    // ==== SEGMENTATION / BACKGROUND ====
    fun startSegmentPerson(context: Context, removeBgOnly: Boolean = false) {
        val bmp = _uiState.value.bitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSegmenting = true) }
            val segmented = ImageUtils.segmentPersonBitmap(context, bmp)
            if (segmented != null) {
                if (removeBgOnly) {
                    _uiState.update {
                        it.copy(
                            bitmap = segmented,
                            compositeBitmap = null,
                            personBitmap = null,
                            backgroundBitmap = null,
                            isSegmenting = false,
                            snackbarMessage = "Sfondo rimosso!"
                        )
                    }
                } else {
                    _uiState.update { it.copy(personBitmap = segmented, isSegmenting = false) }
                }
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

    // Funzioni per dialog sfondo
    fun pickBackgroundFromGallery(context: Context) {
        showSnackbar("Implementa il launcher galleria nella UI.")
        showBackgroundDialog(false)
    }

    fun takeBackgroundPhoto(context: Context) {
        showSnackbar("Implementa il launcher fotocamera nella UI.")
        showBackgroundDialog(false)
    }

    // Compositing preview
    private fun tryComposite() {
        val person = _uiState.value.personBitmap
        val bg = _uiState.value.backgroundBitmap
        if (person != null && bg != null) {
            val composite = ImageUtils.compositePersonOnBackground(person, bg)
            _uiState.update { it.copy(compositeBitmap = composite) }
        }
    }

    fun applyCompositing() {
        val composite = _uiState.value.compositeBitmap ?: return
        _uiState.update {
            it.copy(
                bitmap = composite,
                compositeBitmap = null,
                personBitmap = null,
                backgroundBitmap = null,
                snackbarMessage = "Sfondo applicato!"
            )
        }
    }
    fun cancelCompositing() {
        _uiState.update { it.copy(compositeBitmap = null, personBitmap = null, backgroundBitmap = null) }
    }

    // ==== BRUSH/REMOVE ====
    fun enableBrushRemove() = _uiState.update { it.copy(isBrushRemoveMode = true, showBrushSheet = true) }
    fun disableBrushRemove() = _uiState.update { it.copy(isBrushRemoveMode = false, showBrushSheet = false, brushPathList = emptyList(), redoStack = emptyList()) }

    fun addBrushPath(pointList: List<Offset>, thickness: Float) {
        val current = _uiState.value
        _uiState.update {
            it.copy(
                brushPathList = current.brushPathList + (pointList to thickness),
                redoStack = emptyList()
            )
        }
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

    fun applyBrushRemove(context: Context) {
        val state = _uiState.value
        val bmp = state.bitmap ?: return
        if (state.brushPathList.isEmpty()) {
            showSnackbar("Disegna una maschera sull’oggetto/persona da rimuovere")
            return
        }
        val maskBitmap = BrushMaskOverlayHelper.generateMaskBitmap(
            bmp.width, bmp.height, state.brushPathList,
            state.brushCanvasSize, state.brushImageOffset, state.brushImageSize
        )
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (state.removeBackend) {
                RemoveBackend.LOCAL -> {
                    val result = ImageUtils.inpaintWithOpenCV(context, bmp, maskBitmap)
                    if (result != null) {
                        _uiState.update {
                            it.copy(
                                bitmap = result,
                                snackbarMessage = "Rimozione oggetto (locale OpenCV) completata!",
                                isLoading = false,
                                isBrushRemoveMode = false,
                                brushPathList = emptyList(),
                                redoStack = emptyList(),
                                showBrushSheet = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                snackbarMessage = "Errore rimozione locale",
                                isLoading = false
                            )
                        }
                    }
                }
                RemoveBackend.LAMA -> {
                    ImageUtils.sendMaskAndImageToLamaCleaner(
                        context = context,
                        image = bmp,
                        mask = maskBitmap,
                        onSuccess = { result ->
                            _uiState.update {
                                it.copy(
                                    bitmap = result,
                                    compositeBitmap = null,
                                    personBitmap = null,
                                    backgroundBitmap = null,
                                    snackbarMessage = "Oggetto rimosso (AI Lama-Cleaner)!",
                                    isBrushRemoveMode = false,
                                    brushPathList = emptyList(),
                                    redoStack = emptyList(),
                                    isLoading = false,
                                    showBrushSheet = false
                                )
                            }
                        },
                        onError = { errorMsg ->
                            _uiState.update {
                                it.copy(
                                    snackbarMessage = errorMsg,
                                    isLoading = false
                                )
                            }
                        }
                    )
                }
                RemoveBackend.HUGGINGFACE -> {
                    ImageUtils.sendMaskAndImageToHuggingFaceInpainting(
                        context = context,
                        image = bmp,
                        mask = maskBitmap,
                        prompt = state.inpaintPrompt.ifBlank { "remove object" },
                        numInferenceSteps = 40,
                        guidanceScale = 28,
                        onSuccess = { result ->
                            _uiState.update {
                                it.copy(
                                    bitmap = result,
                                    compositeBitmap = null,
                                    personBitmap = null,
                                    backgroundBitmap = null,
                                    snackbarMessage = "Oggetto rimosso (Hugging Face)!",
                                    isBrushRemoveMode = false,
                                    brushPathList = emptyList(),
                                    redoStack = emptyList(),
                                    isLoading = false,
                                    showBrushSheet = false
                                )
                            }
                        },
                        onError = { errorMsg ->
                            _uiState.update {
                                it.copy(
                                    snackbarMessage = errorMsg,
                                    isLoading = false
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // ==== SAVE ====
    fun save(context: Context) {
        val bmp = _uiState.value.compositeBitmap ?: _uiState.value.bitmap
        if (bmp != null) {
            ImageUtils.saveToGallery(context, bmp) { ok ->
                _uiState.update { it.copy(snackbarMessage = if (ok) "Immagine salvata!" else "Errore nel salvataggio") }
            }
        } else {
            _uiState.update { it.copy(snackbarMessage = "Nessuna immagine da salvare!") }
        }
    }
}