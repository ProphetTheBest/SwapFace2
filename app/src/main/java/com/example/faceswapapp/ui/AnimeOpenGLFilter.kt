package com.example.faceswapapp.ui

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import android.opengl.GLES20

class AnimeOpenGLFilter(
    private val textureWidth: Float,
    private val textureHeight: Float
) : GPUImageFilter(NO_FILTER_VERTEX_SHADER, ANIME_SHADER) {

    private var textureWidthUniform: Int = 0
    private var textureHeightUniform: Int = 0

    override fun onInit() {
        super.onInit()
        textureWidthUniform = GLES20.glGetUniformLocation(getProgram(), "textureWidth")
        textureHeightUniform = GLES20.glGetUniformLocation(getProgram(), "textureHeight")
    }

    override fun onDrawArraysPre() {
        super.onDrawArraysPre()
        setFloat(textureWidthUniform, textureWidth)
        setFloat(textureHeightUniform, textureHeight)
    }
}