package com.example.faceswapapp.ui

import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.faceswapapp.viewmodel.RemoveBackend

@Composable
fun BrushRemoveBottomSheetExtended(
    isBrushRemoveMode: Boolean,
    isResultMode: Boolean,
    removeBackend: RemoveBackend,
    onBackendChange: (RemoveBackend) -> Unit,
    prompt: String,
    onPromptChange: (String) -> Unit,
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit,
    numInferenceSteps: Int, // PATCH: nuovo parametro
    onNumInferenceStepsChange: (Int) -> Unit, // PATCH: nuovo parametro
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    onRestoreBrushEditing: () -> Unit,
    onSave: () -> Unit,
    undoEnabled: Boolean,
    redoEnabled: Boolean,
    resetEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    // Limita l'altezza massima del pannello e consenti lo scroll se serve
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .heightIn(max = 320.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Strumenti Rimozione Oggetti",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
            )
            // Switch triplo per backend (solo in modalità pennello)
            if (isBrushRemoveMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = removeBackend == RemoveBackend.LAMA,
                        onClick = { onBackendChange(RemoveBackend.LAMA) }
                    )
                    Text("Lama Cleaner", modifier = Modifier.weight(1f))
                    RadioButton(
                        selected = removeBackend == RemoveBackend.HUGGINGFACE,
                        onClick = { onBackendChange(RemoveBackend.HUGGINGFACE) }
                    )
                    Text("Hugging Face", modifier = Modifier.weight(1f))
                    RadioButton(
                        selected = removeBackend == RemoveBackend.LOCAL,
                        onClick = { onBackendChange(RemoveBackend.LOCAL) }
                    )
                    Text("Locale", modifier = Modifier.weight(1f))
                }
            }
            // Prompt e steps solo per Hugging Face, solo in modalità pennello
            if (isBrushRemoveMode && removeBackend == RemoveBackend.HUGGINGFACE) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    label = { Text("Prompt (cosa generare al posto)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                // PATCH: slider steps
                Text("Steps: $numInferenceSteps", modifier = Modifier.padding(top = 2.dp))
                Slider(
                    value = numInferenceSteps.toFloat(),
                    onValueChange = { onNumInferenceStepsChange(it.toInt()) },
                    valueRange = 1f..50f,
                    steps = 49,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Slider brush size solo in modalità pennello
            if (isBrushRemoveMode) {
                Text("Dimensione pennello: ${brushSize.toInt()} px", modifier = Modifier.padding(top = 2.dp))
                Slider(
                    value = brushSize,
                    onValueChange = onBrushSizeChange,
                    valueRange = 10f..100f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                // Undo/Redo/Reset
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 2.dp)
                ) {
                    Button(onClick = onUndo, enabled = undoEnabled, modifier = Modifier.weight(1f)) {
                        Text("Undo")
                    }
                    Button(onClick = onRedo, enabled = redoEnabled, modifier = Modifier.weight(1f)) {
                        Text("Redo")
                    }
                    Button(onClick = onReset, enabled = resetEnabled, modifier = Modifier.weight(1f)) {
                        Text("Reset")
                    }
                }
            }
            // Modalità pennello: Applica/Annulla; Modalità risultato: Modifica maschera/Salva
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp)
            ) {
                if (isBrushRemoveMode) {
                    Button(
                        onClick = onApply,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CFC3))
                    ) {
                        Text("Applica rimozione")
                    }
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CFC3))
                    ) {
                        Text("Annulla")
                    }
                } else if (isResultMode) {
                    Button(
                        onClick = onRestoreBrushEditing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CFC3))
                    ) {
                        Text("Modifica maschera")
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CFC3))
                    ) {
                        Text("Salva")
                    }
                }
            }
        }
    }
}