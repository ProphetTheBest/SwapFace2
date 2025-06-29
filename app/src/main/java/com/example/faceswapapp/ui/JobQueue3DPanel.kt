package com.example.faceswapapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.faceswapapp.viewmodel.InpaintJob
import com.example.faceswapapp.viewmodel.InpaintJobStatus
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.foundation.Image
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.filled.Delete
import com.example.faceswapapp.utils.BrushMaskOverlayHelper
import com.example.faceswapapp.utils.ImageUtils

@Composable
fun JobQueue3DPanel(
    jobs: List<InpaintJob>,
    onDismiss: () -> Unit,
    onShowJob: (InpaintJob) -> Unit,
    onPollJob: (InpaintJob) -> Unit,
    onDeleteJob: (InpaintJob) -> Unit,
    onAddJob: (InpaintJob) -> Unit
) {
    // ...state invariato...

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(min = 320.dp, max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(Color.White)
                    .padding(18.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Coda Job HuggingFace",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.List, contentDescription = "Chiudi")
                    }
                }
                Divider(Modifier.padding(vertical = 8.dp))
                if (jobs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nessun job in coda", color = Color.Gray)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .widthIn(min = 340.dp, max = 600.dp)
                                    .fillMaxHeight()
                            ) {
                                items(jobs) { job ->
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        tonalElevation = 2.dp,
                                        color = Color(0xFFF4F9FA),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .widthIn(min = 320.dp, max = 580.dp)
                                            .padding(vertical = 10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .fillMaxWidth()
                                        ) {
                                            val thumb = remember(job.result, job.resultPath, job.mask) {
                                                job.result
                                                    ?: (job.resultPath?.let { com.example.faceswapapp.utils.ImageUtils.loadBitmapFromFile(it) })
                                                    ?: job.mask
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(16.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (thumb != null) {
                                                    Image(
                                                        bitmap = thumb.asImageBitmap(),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(68.dp)
                                                            .background(Color.White, RoundedCornerShape(14.dp))
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.ImageNotSupported,
                                                        contentDescription = null,
                                                        tint = Color(0xFFBBBBBB),
                                                        modifier = Modifier.size(40.dp)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(16.dp))

                                            Column(
                                                Modifier.weight(1f),
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "Prompt:",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color(0xFF888888),
                                                        modifier = Modifier.padding(end = 6.dp)
                                                    )
                                                    Text(
                                                        job.prompt,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.Black,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        "Job ID: ${job.jobId.take(8)}...",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF999999)
                                                    )
                                                    Spacer(Modifier.width(14.dp))
                                                    StatusBadge(job.status)
                                                }
                                                if (job.status == InpaintJobStatus.ERROR) {
                                                    Text(
                                                        job.error ?: "",
                                                        color = Color.Red,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(top = 3.dp)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(12.dp))

                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                modifier = Modifier
                                                    .padding(start = 2.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    when (job.status) {
                                                        InpaintJobStatus.COMPLETED -> {
                                                            // PATCH: ora chiama onShowJob e NON apre più la modale di editing qui!
                                                            Button(
                                                                onClick = {
                                                                    onShowJob(job)
                                                                },
                                                                modifier = Modifier.padding(vertical = 2.dp)
                                                            ) { Text("Show") }
                                                        }
                                                        InpaintJobStatus.QUEUED,
                                                        InpaintJobStatus.PROCESSING -> {
                                                            Button(
                                                                onClick = { onPollJob(job) },
                                                                modifier = Modifier.padding(vertical = 2.dp)
                                                            ) { Text("Carica risultato") }
                                                        }
                                                        else -> {}
                                                    }
                                                    IconButton(
                                                        onClick = { onDeleteJob(job) },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                                                            contentDescription = "Elimina job",
                                                            tint = Color.Red
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // PATCH: JobEditModal completamente eliminata da qui! Ora si gestisce SOLO dalla schermata principale.
}


@Composable
fun JobEditModal(
    job: InpaintJob,
    onApply: (String, List<Pair<List<Offset>, Float>>, Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    initialPrompt: String = "",
    initialSteps: Int = 15,
    initialBrushSize: Float = 40f
) {
    var prompt by remember { mutableStateOf(TextFieldValue(initialPrompt.ifBlank { job.prompt })) }
    var steps by remember { mutableStateOf(initialSteps) }
    var brushSize by remember { mutableStateOf(initialBrushSize) }
    var boxImageSize by remember { mutableStateOf(IntSize(1, 1)) }

    var brushPathList by remember { mutableStateOf<List<Pair<List<Offset>, Float>>>(job.maskPathList ?: listOf()) }
    var redoStack by remember { mutableStateOf(listOf<Pair<List<Offset>, Float>>()) }
    var showMask by remember { mutableStateOf(true) }

    LaunchedEffect(job) {
        brushPathList = job.maskPathList ?: listOf()
        redoStack = listOf()
    }

    AlertDialog(
        onDismissRequest = onCancel,
        shape = RoundedCornerShape(28.dp),
        confirmButton = {},
        dismissButton = {},
        title = { Text("Modifica Job") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (job.result != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                            .onGloballyPositioned { layoutCoordinates ->
                                boxImageSize = IntSize(
                                    layoutCoordinates.size.width,
                                    layoutCoordinates.size.height
                                )
                            }
                    ) {
                        Image(
                            bitmap = job.result.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                        )
                        if (showMask && brushPathList.isNotEmpty()) {
                            BrushMaskOverlay(
                                brushPathList = brushPathList,
                                onPathAdded = { points, thickness ->
                                    brushPathList = brushPathList + (points to thickness)
                                    redoStack = listOf()
                                },
                                brushSize = brushSize,
                                onBrushSizeChange = { brushSize = it },
                                onCanvasSizeChanged = {},
                                onImageBoxChanged = { _, _, _, _ -> },
                                imageOffset = Offset.Zero,
                                imageSize = boxImageSize,
                                bitmapWidth = job.result.width,
                                bitmapHeight = job.result.height,
                            )
                        }
                    }
                } else {
                    Text("Nessun risultato disponibile")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Prompt") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Steps: $steps", modifier = Modifier.weight(1f))
                    Slider(
                        value = steps.toFloat(),
                        onValueChange = { steps = it.toInt() },
                        valueRange = 1f..50f,
                        steps = 49,
                        modifier = Modifier.weight(3f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Brush: ${brushSize.toInt()} px", modifier = Modifier.weight(1f))
                    Slider(
                        value = brushSize,
                        onValueChange = { brushSize = it },
                        valueRange = 5f..100f,
                        steps = 19,
                        modifier = Modifier.weight(3f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Visualizza maschera", modifier = Modifier.weight(1f))
                    Switch(checked = showMask, onCheckedChange = { showMask = it })
                }
                if (showMask && job.result != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp)
                    ) {
                        Button(
                            onClick = {
                                if (brushPathList.isNotEmpty()) {
                                    redoStack = listOf(brushPathList.last()) + redoStack
                                    brushPathList = brushPathList.dropLast(1)
                                }
                            },
                            enabled = brushPathList.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) { Text("Undo") }
                        Button(
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    brushPathList = brushPathList + redoStack.first()
                                    redoStack = redoStack.drop(1)
                                }
                            },
                            enabled = redoStack.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) { Text("Redo") }
                        Button(
                            onClick = {
                                brushPathList = listOf()
                                redoStack = listOf()
                            },
                            enabled = brushPathList.isNotEmpty() || redoStack.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) { Text("Reset") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onApply(prompt.text, brushPathList, steps) },
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) { Text("Applica") }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) { Text("Salva") }
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) { Text("Chiudi") }
                }
            }
        }
    )
}

@Composable
private fun StatusBadge(status: InpaintJobStatus) {
    val (bg, text, emoji) = when (status) {
        InpaintJobStatus.QUEUED -> Triple(Color(0xFFB2DFDB), "In coda", "\u23F3")
        InpaintJobStatus.PROCESSING -> Triple(Color(0xFFFFF59D), "In elaborazione", "\uD83D\uDD03")
        InpaintJobStatus.COMPLETED -> Triple(Color(0xFFB9F6CA), "Completato", "\u2705")
        InpaintJobStatus.ERROR -> Triple(Color(0xFFFFCDD2), "Errore", "\u26A0\uFE0F")
    }
    Box(
        modifier = Modifier
            .background(bg, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text("$emoji $text", style = MaterialTheme.typography.labelMedium)
    }
}