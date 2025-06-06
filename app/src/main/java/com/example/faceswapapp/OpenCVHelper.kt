package com.example.faceswapapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.photo.Photo

object OpenCVHelper {
    fun init(context: Context) {
        try {
            System.loadLibrary("opencv_java4")
            Log.d("OpenCV", "Manual System.loadLibrary('opencv_java4') OK")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("OpenCV", "Manual System.loadLibrary('opencv_java4') FAILED: ${e.message}")
        }

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed!")
        } else {
            Log.d("OpenCV", "OpenCV initialized: " + org.opencv.core.Core.VERSION)
        }
    }

    fun inpaint(src: Bitmap, mask: Bitmap): Bitmap {
        // Converti src in Mat
        val srcMat0 = Mat()
        Utils.bitmapToMat(src, srcMat0)
        // Converti in 3 canali BGR se necessario
        val srcMat = if (srcMat0.channels() == 4) {
            val bgr = Mat()
            Imgproc.cvtColor(srcMat0, bgr, Imgproc.COLOR_RGBA2BGR)
            srcMat0.release()
            bgr
        } else if (srcMat0.channels() == 1) {
            val bgr = Mat()
            Imgproc.cvtColor(srcMat0, bgr, Imgproc.COLOR_GRAY2BGR)
            srcMat0.release()
            bgr
        } else {
            srcMat0
        }

        // Converti mask in Mat
        val maskMat0 = Mat()
        Utils.bitmapToMat(mask, maskMat0)
        // Assicurati che la maschera sia single-channel, 8-bit
        val maskMatGray = if (maskMat0.channels() > 1) {
            val singleChannel = Mat()
            Imgproc.cvtColor(maskMat0, singleChannel, Imgproc.COLOR_BGR2GRAY)
            maskMat0.release()
            singleChannel
        } else {
            maskMat0
        }
        val maskMat8u = Mat()
        maskMatGray.convertTo(maskMat8u, CvType.CV_8UC1)
        // PATCH: forza la maschera binaria
        Imgproc.threshold(maskMat8u, maskMat8u, 127.0, 255.0, Imgproc.THRESH_BINARY)
        // DEBUG: stampa la somma dei pixel della maschera
        Log.d("OpenCV", "maskMat8u sum: " + org.opencv.core.Core.sumElems(maskMat8u).`val`[0])

        // Inpainting
        val resultMat = Mat()
        Photo.inpaint(srcMat, maskMat8u, resultMat, 3.0, Photo.INPAINT_TELEA)

        // Bitmap output
        val resultBitmap = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        // Converti BGR->RGBA per output
        val resultMatRGBA = Mat()
        Imgproc.cvtColor(resultMat, resultMatRGBA, Imgproc.COLOR_BGR2RGBA)
        Utils.matToBitmap(resultMatRGBA, resultBitmap)

        // Libera risorse
        srcMat.release()
        maskMatGray.release()
        maskMat8u.release()
        resultMat.release()
        resultMatRGBA.release()

        return resultBitmap
    }
}