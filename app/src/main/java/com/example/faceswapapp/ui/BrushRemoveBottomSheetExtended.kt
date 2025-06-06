package com.example.faceswapapp.ui

import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape

import com.example.faceswapapp.viewmodel.RemoveBackend

@Composable
fun BrushRemoveBottomSheetExtended(
    removeBackend: RemoveBackend,
    onBackendChange: (RemoveBackend) -> Unit,
    prompt: String,
    onPromptChange: (String) -> Unit,
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    undoEnabled: Boolean,
    redoEnabled: Boolean,
    resetEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 8.dp,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle in alto
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .width(48.dp)
                        .height(6.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(3.dp))
                )
            }
            Text("Strumenti Rimozione Oggetti", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            // Switch triplo per backend
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
            Spacer(Modifier.height(12.dp))
            // Prompt solo per Hugging Face
            if (removeBackend == RemoveBackend.HUGGINGFACE) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    label = { Text("Prompt (cosa generare al posto)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            // Slider brush size
            Text("Dimensione pennello: ${brushSize.toInt()} px")
            Slider(
                value = brushSize,
                onValueChange = onBrushSizeChange,
                valueRange = 10f..100f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            // Undo/Redo/Reset
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
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
            Spacer(Modifier.height(14.dp))
            // Applica/Annulla
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onApply, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CFC3))) {
                    Text("Applica rimozione")
                }
                Button(onClick = onCancel, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CFC3))) {
                    Text("Annulla")
                }
            }
        }
    }
}