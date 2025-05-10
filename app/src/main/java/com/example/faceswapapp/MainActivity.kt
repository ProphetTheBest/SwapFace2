package com.example.faceswapapp

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.faceswapapp.ui.theme.FaceSwapAppTheme
import com.google.mediapipe.tasks.vision.core.MPImage
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FaceSwapAppTheme {
                UnifiedScreen()
            }
        }
    }
}

@Composable
fun UnifiedScreen() {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var detectedLandmarks by remember { mutableStateOf<List<List<Offset>>?>(null) }
    val context = LocalContext.current

    // Launcher per selezionare immagine dalla galleria
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            detectFaceLandmarksFromUri(context, uri) { landmarks ->
                detectedLandmarks = landmarks
            }
        }
    }

    // Launcher per richiedere permessi
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            galleryLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Permesso negato", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            selectedImageUri?.let { uri ->
                val bitmap = remember { loadBitmapFromUri(context, uri) }
                bitmap?.let { bmp ->
                    FaceLandmarkCanvas(bitmap = bmp, landmarks = detectedLandmarks)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    galleryLauncher.launch("image/*")
                }
            }) {
                Text(text = "Seleziona Immagine")
            }
        }
    }
}

@Composable
fun FaceLandmarkCanvas(bitmap: Bitmap, landmarks: List<List<Offset>>?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.size(300.dp)) {
            drawImage(bitmap.asImageBitmap()) // Disegna l'immagine
            landmarks?.forEach { faceLandmarks ->
                faceLandmarks.forEach { point ->
                    drawCircle(
                        color = Color.Red,
                        center = point,
                        radius = 4.dp.toPx()
                    ) // Disegna un punto per ogni landmark
                }
            }
        }

        // Aggiungi pulsanti "+" sotto ogni volto rilevato
        landmarks?.let { faces ->
            LazyColumn {
                items(faces) { face ->
                    Button(
                        onClick = { /* Logica per il Face Swap */ },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("+")
                    }
                }
            }
        }
    }
}

fun detectFaceLandmarksFromUri(
    context: android.content.Context,
    uri: Uri,
    callback: (List<List<Offset>>) -> Unit
) {
    val options = FaceLandmarker.FaceLandmarkerOptions.builder()
        .setBaseOptionsFromAsset("face_landmarker.task") // Assicurati di avere questo modello nella directory assets
        .setRunningMode(RunningMode.IMAGE)
        .setOutputFaceBlendshapes(false)
        .setOutputFacialTransformationMatrixes(false)
        .build()

    val faceLandmarker = FaceLandmarker.createFromOptions(context, options)

    val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    val mpImage = MPImage.create(bitmap)

    val results = faceLandmarker.detect(mpImage)
    faceLandmarker.close() // Libera risorse

    val landmarks = results.landmarks().map { faceLandmarks ->
        faceLandmarks.map {
            Offset(it.x() * bitmap.width, it.y() * bitmap.height) // Converti in coordinate pixel
        }
    }

    callback(landmarks)
}

fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FaceSwapAppTheme {
        UnifiedScreen()
    }
}