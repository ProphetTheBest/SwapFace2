package com.example.faceswapapp.viewmodel

import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
enum class InpaintJobStatusPersistable { QUEUED, PROCESSING, COMPLETED, ERROR }

@Serializable
data class InpaintJobPersistable(
    val jobId: String,
    val prompt: String,
    val status: InpaintJobStatusPersistable,
    val error: String? = null,
    val resultPath: String? = null,
    @Contextual
    val maskPathList: List<Pair<List<@Contextual Offset>, Float>>? = null
)