package com.example.faceswapapp.ui

import android.graphics.Bitmap
import android.content.Context
import com.example.faceswapapp.utils.FilterType
import com.example.faceswapapp.utils.applyAnimeGAN
import com.example.faceswapapp.utils.applyCaricatureMultiCenter
import com.example.faceswapapp.utils.CaricatureCenter

fun applyMultipleFilters(
    bitmap: Bitmap,
    activeFilters: Set<FilterType>,
    filterParams: Map<FilterType, Float>,
    context: Context,
    caricatureCenters: List<CaricatureCenter>? = null, // <-- nuovo parametro opzionale
    animeGanModelName: String = "face_paint_512_v2_tf_nhwc_inout.tflite" // PATCH: aggiunto parametro modello
): Bitmap {
    var result = bitmap
    if (activeFilters.contains(FilterType.Saturation)) {
        result = applySaturation(result, context, filterParams[FilterType.Saturation] ?: 1f)
    }
    if (activeFilters.contains(FilterType.Blur)) {
        result = applyBlur(result, context, filterParams[FilterType.Blur] ?: 0f)
    }
    if (activeFilters.contains(FilterType.Sharpen)) {
        result = applySharpen(result, filterParams[FilterType.Sharpen] ?: 1f)
    }
    if (activeFilters.contains(FilterType.Vintage)) {
        result = applyVintage(result, context)
    }
    if (activeFilters.contains(FilterType.BlackWhite)) {
        result = applyBlackWhite(result, context)
    }
    if (activeFilters.contains(FilterType.Cartoon)) {
        result = applyCartoonParametric(result)
    }
    if (activeFilters.contains(FilterType.CartoonOpenGL)) {
        result = applyCartoonOpenGL(result, context)
    }
    if (activeFilters.contains(FilterType.AnimeGAN)) {
        result = applyAnimeGAN(result, context, animeGanModelName) // PATCH: passa il modello selezionato
    }
    if (activeFilters.contains(FilterType.Caricature)) {
        // Applica la caricatura SOLO se ci sono centri inseriti dall'utente
        if (caricatureCenters != null && caricatureCenters.isNotEmpty()) {
            result = applyCaricatureMultiCenter(result, caricatureCenters)
        }
        // Altrimenti, NON applicare alcuna distorsione (immagine resta invariata finché non aggiungi un centro)
    }
    return result
}