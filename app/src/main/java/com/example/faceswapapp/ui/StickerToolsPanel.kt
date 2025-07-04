package com.example.faceswapapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.faceswapapp.PlacedSticker
import kotlin.math.roundToInt

/**
 * Composable estratto da FaceSwapScreen: pannello strumenti per la modifica degli sticker
 * Mostra sliders per X/Y/Scala/Rotazione e pulsanti Reset/Elimina per lo sticker selezionato
 */
@Composable
fun StickerToolsPanel(
    selectedSticker: PlacedSticker,
    popupOffsetX: Float,
    popupOffsetY: Float,
    onOffsetChange: (Float, Float) -> Unit,
    onUpdateSticker: (x: Float?, y: Float?, scale: Float?, rot: Float?) -> Unit,
    onResetSticker: () -> Unit,
    onRemoveSticker: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxSize()
            .zIndex(1f)
    ) {
        val scrollState = rememberScrollState()
        Surface(
            modifier = Modifier
                .offset { IntOffset(popupOffsetX.roundToInt(), popupOffsetY.roundToInt() - 80) }
                .widthIn(min = 340.dp, max = 400.dp)
                .heightIn(min = 380.dp, max = 540.dp)
                .align(Alignment.TopCenter)
                .shadow(12.dp, RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consumeAllChanges()
                        onOffsetChange(
                            popupOffsetX + dragAmount.x,
                            popupOffsetY + dragAmount.y
                        )
                    }
                },
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            Box {
                Column(
                    Modifier
                        .padding(14.dp)
                        .widthIn(min = 340.dp, max = 380.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = "Scorri verso il basso per più opzioni",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consumeAllChanges()
                                    onOffsetChange(
                                        popupOffsetX + dragAmount.x,
                                        popupOffsetY + dragAmount.y
                                    )
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Strumenti Sticker: ${selectedSticker.sticker.label}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Info, contentDescription = "Chiudi")
                        }
                    }
                    Divider(Modifier.padding(vertical = 6.dp))
                    
                    // X Slider
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("X:", Modifier.width(32.dp))
                        Slider(
                            value = selectedSticker.x,
                            onValueChange = { onUpdateSticker(x = it, y = null, scale = null, rot = null) },
                            valueRange = -200f..200f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        Text(selectedSticker.x.toInt().toString(), Modifier.width(42.dp), textAlign = TextAlign.End)
                    }
                    
                    // Y Slider
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("Y:", Modifier.width(32.dp))
                        Slider(
                            value = selectedSticker.y,
                            onValueChange = { onUpdateSticker(x = null, y = it, scale = null, rot = null) },
                            valueRange = -200f..200f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        Text(selectedSticker.y.toInt().toString(), Modifier.width(42.dp), textAlign = TextAlign.End)
                    }
                    
                    // Scale Slider
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("Scala", Modifier.width(48.dp))
                        Slider(
                            value = selectedSticker.scale,
                            onValueChange = { onUpdateSticker(x = null, y = null, scale = it, rot = null) },
                            valueRange = 0.2f..3.0f,
                            steps = 18,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        Text("${"%.2f".format(selectedSticker.scale)}x", Modifier.width(48.dp), textAlign = TextAlign.End)
                    }
                    
                    // Rotation Slider
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("Rot:", Modifier.width(48.dp))
                        Slider(
                            value = selectedSticker.rotation,
                            onValueChange = { onUpdateSticker(x = null, y = null, scale = null, rot = it) },
                            valueRange = -180f..180f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        Text("${selectedSticker.rotation.toInt()}°", Modifier.width(48.dp), textAlign = TextAlign.End)
                    }
                    
                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Button(
                            onClick = onResetSticker,
                            modifier = Modifier.weight(1f)
                        ) { Text("Reset") }
                        Button(
                            onClick = onRemoveSticker,
                            modifier = Modifier.weight(1f)
                        ) { Text("Elimina") }
                    }
                }
                // Fade trasparente in basso per suggerire lo scroll
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.10f))
                            )
                        )
                )
            }
        }
    }
}