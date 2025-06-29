package com.example.faceswapapp.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.faceswapapp.viewmodel.PhotoEditorViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

class PhotoEditorActivity : ComponentActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "image_uri"
        const val EXTRA_JOB_RESULT_PATH = "JOB_RESULT_PATH"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val jobResultPath = intent.getStringExtra(EXTRA_JOB_RESULT_PATH)

        setContent {
            val editorViewModel = remember { PhotoEditorViewModel() }

            // Se viene passato un risultato job, caricalo all'avvio tramite il ViewModel
            LaunchedEffect(jobResultPath) {
                if (!jobResultPath.isNullOrEmpty()) {
                    editorViewModel.loadJobResultByPath(jobResultPath)
                }
            }

            // Se c'è una imageUri usala, altrimenti lascia null (verrà già caricato dal ViewModel se job)
            PhotoEditorScreen(
                imageUri = uriString?.let { Uri.parse(it) },
                editorViewModel = editorViewModel // PATCH: usa il nome parametro giusto!
            )
        }
    }
}