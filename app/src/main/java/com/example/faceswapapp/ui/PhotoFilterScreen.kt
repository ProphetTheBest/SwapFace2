package com.example.faceswapapp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.faceswapapp.utils.FilterType

@Composable
fun PhotoFilterScreen(
    originalBitmap: Bitmap,
    onFilterApplied: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(FilterType.None) }
    var saturation by remember { mutableStateOf(1f) }

    val filteredBitmap: Bitmap = remember(originalBitmap, selectedFilter, saturation) {
        applyFilter(originalBitmap, selectedFilter, saturation)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Image(
            bitmap = filteredBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        )
        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = { selectedFilter = FilterType.None }) { Text("Normale") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { selectedFilter = FilterType.BlackWhite }) { Text("B/N") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { selectedFilter = FilterType.Vintage }) { Text("Vintage") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { selectedFilter = FilterType.Saturation }) { Text("Saturazione") }
        }
        if (selectedFilter == FilterType.Saturation) {
            Slider(
                value = saturation,
                onValueChange = { saturation = it },
                valueRange = 0f..2f,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = { onBack() }) { Text("Annulla") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onFilterApplied(filteredBitmap) }) { Text("Applica") }
        }
    }
}