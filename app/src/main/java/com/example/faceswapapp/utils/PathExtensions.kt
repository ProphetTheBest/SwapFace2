package com.example.faceswapapp.utils

import android.graphics.Path as AndroidPath
import androidx.compose.ui.geometry.Offset

fun List<Offset>.toAndroidPath(): AndroidPath {
    val path = AndroidPath()
    if (this.isNotEmpty()) {
        path.moveTo(this[0].x, this[0].y)
        for (i in 1 until this.size) {
            path.lineTo(this[i].x, this[i].y)
        }
    }
    return path
}