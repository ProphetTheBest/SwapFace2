package com.example.faceswapapp.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class PhotoEditorActivity : ComponentActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "image_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        setContent {
            if (uriString != null) {
                PhotoEditorScreen(imageUri = Uri.parse(uriString))
            }
        }
    }
}