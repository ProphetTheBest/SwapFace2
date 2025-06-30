package com.example.faceswapapp.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.faceswapapp.viewmodel.PhotoEditorViewModel
import androidx.activity.viewModels

class PhotoEditorActivity : ComponentActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "image_uri"
        const val EXTRA_JOB_RESULT_PATH = "JOB_RESULT_PATH"
        const val EXTRA_JOB_ID = "extra_job_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val jobResultPath = intent.getStringExtra(EXTRA_JOB_RESULT_PATH)

        val editorViewModel: PhotoEditorViewModel by viewModels()

        setContent {
            // Forza SEMPRE il reload della job list dal DataStore quando parte la schermata!
            androidx.compose.runtime.LaunchedEffect(Unit) {
                editorViewModel.loadPersistentJobs(applicationContext)
            }

            // Se viene passato il path di un risultato, caricalo all'avvio.
            androidx.compose.runtime.LaunchedEffect(jobResultPath) {
                if (!jobResultPath.isNullOrEmpty()) {
                    editorViewModel.loadJobResultByPath(jobResultPath)
                }
            }

            PhotoEditorScreen(
                imageUri = uriString?.let { Uri.parse(it) },
                editorViewModel = editorViewModel
            )
        }
    }
}