package com.example.faceswapapp

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.faceswapapp.ui.FaceSwapScreen
import com.example.faceswapapp.ui.PhotoEditorActivity
import com.example.faceswapapp.utils.ImageUtils
import com.example.faceswapapp.OpenCVHelper // assicurati che OpenCVHelper sia nel package giusto

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OpenCVHelper.init(this)
        setContent {
            var showPickerDialog by remember { mutableStateOf(false) }
            var photoUriForCamera by remember { mutableStateOf<Uri?>(null) }

            // Galleria Launcher
            val galleryLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    openPhotoEditor(it)
                }
            }

            // Fotocamera Launcher
            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture()
            ) { success: Boolean ->
                if (success && photoUriForCamera != null) {
                    openPhotoEditor(photoUriForCamera!!)
                }
            }

            // UI principale
            FaceSwapScreen(
                onOpenPhotoEditor = {
                    showPickerDialog = true
                }
            )

            // Dialog di scelta tra Galleria e Fotocamera
            if (showPickerDialog) {
                AlertDialog(
                    onDismissRequest = { showPickerDialog = false },
                    title = { Text("Scegli immagine") },
                    text = { Spacer(Modifier.height(8.dp)) },
                    confirmButton = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            ElevatedButton(
                                onClick = {
                                    showPickerDialog = false
                                    galleryLauncher.launch("image/*")
                                },
                                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = "Galleria"
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Galleria")
                            }
                            ElevatedButton(
                                onClick = {
                                    val uri = ImageUtils.createImageUri(this@MainActivity)
                                    if (uri != null) {
                                        photoUriForCamera = uri
                                        showPickerDialog = false
                                        cameraLauncher.launch(uri)
                                    } else {
                                        showPickerDialog = false
                                    }
                                },
                                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Fotocamera"
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Fotocamera")
                            }
                        }
                    },
                    dismissButton = {}
                )
            }
        }
    }

    // Lancia l'editor passando la URI dell'immagine
    private fun openPhotoEditor(imageUri: Uri) {
        val intent = android.content.Intent(this, PhotoEditorActivity::class.java)
        intent.putExtra(PhotoEditorActivity.EXTRA_IMAGE_URI, imageUri.toString())
        startActivity(intent)
    }
}