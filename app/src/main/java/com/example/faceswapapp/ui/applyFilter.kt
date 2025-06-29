package com.example.faceswapapp.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.content.Context
import com.example.faceswapapp.utils.FilterType
import com.example.faceswapapp.utils.applyAnimeGAN
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGaussianBlurFilter
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.core.Point
import kotlin.math.*
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.core.CvType

fun applyFilter(
    bitmap: Bitmap,
    type: FilterType,
    context: Context,
    saturation: Float = 1f,
    animeGanModelName: String = "face_paint_512_v2_tf_nhwc_inout.tflite" // PATCH: aggiunto parametro modello
): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val filteredBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(filteredBitmap)
    val paint = Paint()

    return when (type) {
        FilterType.BlackWhite -> {
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            paint.colorFilter = ColorMatrixColorFilter(matrix)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            filteredBitmap
        }
        FilterType.Saturation -> {
            val matrix = ColorMatrix()
            matrix.setSaturation(saturation)
            paint.colorFilter = ColorMatrixColorFilter(matrix)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            filteredBitmap
        }
        FilterType.Vintage -> {
            val matrix = ColorMatrix(
                floatArrayOf(
                    1.2f, 0f, 0f, 0f, 30f,
                    0f, 1f, 0f, 0f, 10f,
                    0f, 0f, 0.8f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            paint.colorFilter = ColorMatrixColorFilter(matrix)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            filteredBitmap
        }
        FilterType.CartoonOpenGL -> {
            applyCartoonOpenGL(bitmap, context)
        }
        FilterType.AnimeGAN -> {
            applyAnimeGAN(bitmap, context, animeGanModelName) // PATCH: passa modello selezionato
        }
        FilterType.Cartoon -> {
            applyCartoonParametric(bitmap)
        }
        FilterType.Caricature -> {
            val caricatureAmount = saturation
            applyCaricature(bitmap, caricatureAmount)
        }
        else -> {
            paint.colorFilter = null
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            filteredBitmap
        }
    }
}

fun applySaturation(bitmap: Bitmap, context: Context, level: Float): Bitmap =
    applyFilter(bitmap, FilterType.Saturation, context, level)

fun applyVintage(bitmap: Bitmap, context: Context): Bitmap =
    applyFilter(bitmap, FilterType.Vintage, context, 1f)

fun applyBlackWhite(bitmap: Bitmap, context: Context): Bitmap =
    applyFilter(bitmap, FilterType.BlackWhite, context, 1f)

fun applyBlur(bitmap: Bitmap, context: Context, radius: Float): Bitmap = smoothBitmap(bitmap, context, radius)

/**
 * Blur cross-version sicuro: usa sempre GPUImage per compatibilità IDE/Sdk future.
 * In futuro, potrai aggiungere RenderEffect nativo qui se vorrai supportare solo Android 12+.
 */
fun smoothBitmap(bitmap: Bitmap, context: Context, radius: Float = 2f): Bitmap {
    val gpuImage = GPUImage(context)
    gpuImage.setImage(bitmap)
    gpuImage.setFilter(GPUImageGaussianBlurFilter(radius))
    return gpuImage.bitmapWithFilterApplied
}

fun applySharpen(bitmap: Bitmap, factor: Float): Bitmap {
    val mat = Mat()
    Utils.bitmapToMat(bitmap, mat)
    val kernel = Mat(3, 3, org.opencv.core.CvType.CV_32F)
    val sharpenFactor = factor.coerceIn(0f, 2f)
    // Kernel base per sharpen (più alto sharpenFactor = più nitido)
    val k = floatArrayOf(
        0f, -sharpenFactor, 0f,
        -sharpenFactor, 1f + 4f * sharpenFactor, -sharpenFactor,
        0f, -sharpenFactor, 0f
    )
    kernel.put(0, 0, k)
    val result = Mat()
    Imgproc.filter2D(mat, result, mat.depth(), kernel)
    val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), bitmap.config ?: Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(result, resultBitmap)
    mat.release()
    kernel.release()
    result.release()
    return resultBitmap
}

// Cartoon filter function
fun applyCartoonParametric(
    bitmap: Bitmap,
    brightness: Double = 1.1,
    contrast: Double = 1.1,
    quantLevels: Int = 10,
    bilateralSize: Int = 9,
    edgeKernel: Int = 2,
    useCanny: Boolean = true
): Bitmap {
    // Step 1: Bitmap -> Mat RGBA
    val matRGBA = Mat()
    Utils.bitmapToMat(bitmap, matRGBA)

    // Step 2: Convert to Mat BGR (3 channels)
    val matBGR = Mat()
    when (matRGBA.channels()) {
        4 -> Imgproc.cvtColor(matRGBA, matBGR, Imgproc.COLOR_RGBA2BGR)
        1 -> Imgproc.cvtColor(matRGBA, matBGR, Imgproc.COLOR_GRAY2BGR)
        3 -> matRGBA.copyTo(matBGR)
        else -> Imgproc.cvtColor(matRGBA, matBGR, Imgproc.COLOR_RGBA2BGR)
    }

    // Step 3: Brightness and Contrast
    val bright = Mat()
    matBGR.convertTo(bright, CvType.CV_8UC3, contrast, (brightness - 1.0) * 128.0)

    // Step 4: Bilateral Filter (using separate Mats for input and output)
    val smooth = Mat()
    Imgproc.bilateralFilter(bright, smooth, bilateralSize or 1, 150.0, 150.0)
    val smooth2 = Mat()
    Imgproc.bilateralFilter(smooth, smooth2, bilateralSize or 1, 150.0, 150.0)

    // Step 5: Color Quantization
    val quantized = Mat()
    smooth2.convertTo(quantized, -1, 1.0, 0.0)
    val levels = quantLevels.coerceIn(3, 12)
    val div = 256 / levels
    val data = ByteArray((quantized.total() * quantized.channels()).toInt())
    quantized.get(0, 0, data)
    for (i in data.indices) {
        val v = (data[i].toInt() and 0xFF) / div * div + div / 2
        data[i] = v.coerceIn(0, 255).toByte()
    }
    quantized.put(0, 0, data)
    Imgproc.medianBlur(quantized, quantized, 3)

    // Step 6: Edge Detection
    val gray = Mat()
    Imgproc.cvtColor(bright, gray, Imgproc.COLOR_BGR2GRAY)
    Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
    val edges = Mat()
    if (useCanny) {
        Imgproc.Canny(gray, edges, 80.0, 160.0)
    } else {
        Imgproc.adaptiveThreshold(
            gray,
            edges,
            255.0,
            Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY,
            9,
            2.0
        )
    }
    Imgproc.dilate(
        edges,
        edges,
        Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(edgeKernel.toDouble(), edgeKernel.toDouble()))
    )

    // Step 7: Invert and Convert Edges to BGR
    val edgesInv = Mat()
    Core.bitwise_not(edges, edgesInv)
    val edgesColor = Mat()
    Imgproc.cvtColor(edgesInv, edgesColor, Imgproc.COLOR_GRAY2BGR)

    // Step 8: Merge Colors and Edges
    val cartoon = Mat()
    Core.addWeighted(quantized, 0.92, edgesColor, 0.08, 0.0, cartoon)
    Imgproc.medianBlur(cartoon, cartoon, 3)

    // Step 9: BGR -> RGB for output Bitmap
    val cartoonRGB = Mat()
    Imgproc.cvtColor(cartoon, cartoonRGB, Imgproc.COLOR_BGR2RGB)
    val result = Bitmap.createBitmap(cartoonRGB.cols(), cartoonRGB.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(cartoonRGB, result)

    // Step 10: Release resources
    matRGBA.release()
    matBGR.release()
    bright.release()
    smooth.release()
    smooth2.release()
    quantized.release()
    gray.release()
    edges.release()
    edgesInv.release()
    edgesColor.release()
    cartoon.release()
    cartoonRGB.release()

    return result
}

fun applyCartoonImproved(bitmap: Bitmap): Bitmap {
    val mat = Mat()
    Utils.bitmapToMat(bitmap, mat)

    // Conversione sicura a BGR (8 bit, 3 canali)
    val matBGR = Mat()
    when (mat.type()) {
        CvType.CV_8UC3 -> mat.copyTo(matBGR)
        CvType.CV_8UC4 -> Imgproc.cvtColor(mat, matBGR, Imgproc.COLOR_RGBA2BGR)
        CvType.CV_8UC1 -> Imgproc.cvtColor(mat, matBGR, Imgproc.COLOR_GRAY2BGR)
        else -> Imgproc.cvtColor(mat, matBGR, Imgproc.COLOR_RGBA2BGR)
    }

    // 1. Bilateral filter (mai in-place!)
    val color = Mat()
    Imgproc.bilateralFilter(matBGR, color, 9, 75.0, 75.0)
    val color2 = Mat()
    Imgproc.bilateralFilter(color, color2, 7, 45.0, 45.0)

    // 2. Quantizzazione colori
    val quantized = Mat()
    color2.convertTo(quantized, -1, 1.0, 0.0)
    val levels = 8
    val div = 256 / levels
    val data = ByteArray((quantized.total() * quantized.channels()).toInt())
    quantized.get(0, 0, data)
    for (i in data.indices) {
        val v = (data[i].toInt() and 0xFF) / div * div + div / 2
        data[i] = v.coerceIn(0, 255).toByte()
    }
    quantized.put(0, 0, data)
    Imgproc.medianBlur(quantized, quantized, 3)

    // 3. Edge detection
    val gray = Mat()
    Imgproc.cvtColor(matBGR, gray, Imgproc.COLOR_BGR2GRAY)
    Imgproc.medianBlur(gray, gray, 7)
    val edges = Mat()
    Imgproc.adaptiveThreshold(
        gray,
        edges,
        255.0,
        Imgproc.ADAPTIVE_THRESH_MEAN_C,
        Imgproc.THRESH_BINARY,
        9,
        2.0
    )
    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
    Imgproc.dilate(edges, edges, kernel)

    // 4. Bordi neri
    val edgesColor = Mat()
    Imgproc.cvtColor(edges, edgesColor, Imgproc.COLOR_GRAY2BGR)
    Core.bitwise_not(edgesColor, edgesColor)

    // 5. Overlay bordi su quantized
    val cartoon = Mat()
    Core.bitwise_and(quantized, edgesColor, cartoon)
    Imgproc.medianBlur(cartoon, cartoon, 3)

    val resultBitmap = Bitmap.createBitmap(cartoon.cols(), cartoon.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(cartoon, resultBitmap)

    // Cleanup
    mat.release()
    matBGR.release()
    color.release()
    color2.release()
    quantized.release()
    gray.release()
    edges.release()
    edgesColor.release()
    cartoon.release()
    kernel.release()
    return resultBitmap
}

fun applyCartoon(bitmap: Bitmap): Bitmap {
    val mat = Mat()
    Utils.bitmapToMat(bitmap, mat)

    if (mat.channels() == 4) Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
    if (mat.channels() == 1) Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2BGR)

    val bright = Mat()
    mat.convertTo(bright, -1, 1.08, 8.0)

    val color = Mat()
    Imgproc.bilateralFilter(bright, color, 7, 45.0, 45.0)

    val quantized = Mat()
    color.convertTo(quantized, -1, 1.0, 0.0)
    val levels = 16
    val div = 256 / levels
    val data = ByteArray((quantized.total() * quantized.channels()).toInt())
    quantized.get(0, 0, data)
    for (i in data.indices) {
        val v = (data[i].toInt() and 0xFF) / div * div + div / 2
        data[i] = v.coerceIn(0, 255).toByte()
    }
    quantized.put(0, 0, data)

    val gray = Mat()
    Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
    Imgproc.medianBlur(gray, gray, 5)
    val edges = Mat()
    Imgproc.adaptiveThreshold(
        gray,
        edges,
        255.0,
        Imgproc.ADAPTIVE_THRESH_MEAN_C,
        Imgproc.THRESH_BINARY,
        7,
        2.0
    )

    val edgesColor = Mat()
    Imgproc.cvtColor(edges, edgesColor, Imgproc.COLOR_GRAY2BGR)
    val cartoon = Mat()
    Core.addWeighted(quantized, 0.84, edgesColor, 0.16, 0.0, cartoon)

    val resultBitmap = Bitmap.createBitmap(cartoon.cols(), cartoon.rows(), bitmap.config ?: Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(cartoon, resultBitmap)

    mat.release()
    bright.release()
    color.release()
    quantized.release()
    gray.release()
    edges.release()
    edgesColor.release()
    cartoon.release()
    return resultBitmap
}

fun applyCartoonOpenGL(bitmap: Bitmap, context: Context): Bitmap {
    val gpuImage = GPUImage(context)
    gpuImage.setImage(bitmap)
    gpuImage.setFilter(
        CartoonOpenGLFilter(
            bitmap.width.toFloat(),
            bitmap.height.toFloat()
        )
    )
    return gpuImage.bitmapWithFilterApplied
}

fun adjustSaturation(bitmap: Bitmap, saturation: Float): Bitmap {
    val cm = ColorMatrix()
    cm.setSaturation(saturation)
    val paint = Paint()
    paint.colorFilter = ColorMatrixColorFilter(cm)
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return result
}

/**
 * Applica un effetto caricatura (balloon/warping) usando OpenCV.
 * amount: 0.3f (molto forte) ... 0.95f (quasi nulla)
 */
fun applyCaricature(bitmap: Bitmap, amount: Float = 0.7f): Bitmap {
    val mat = Mat()
    Utils.bitmapToMat(bitmap, mat)
    val width = mat.cols()
    val height = mat.rows()
    val centerX = width / 2.0
    val centerY = height / 2.0
    val radius = min(width, height) / 2.0 * 0.8

    val mapX = Mat(height, width, org.opencv.core.CvType.CV_32FC1)
    val mapY = Mat(height, width, org.opencv.core.CvType.CV_32FC1)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val dx = x - centerX
            val dy = y - centerY
            val distance = sqrt(dx*dx + dy*dy)
            if (distance < radius) {
                val r = distance / radius
                val theta = atan2(dy, dx)
                val rn = r.pow(1.0 - amount.toDouble())
                val newX = centerX + rn * radius * cos(theta)
                val newY = centerY + rn * radius * sin(theta)
                mapX.put(y, x, floatArrayOf(newX.toFloat()))
                mapY.put(y, x, floatArrayOf(newY.toFloat()))
            } else {
                mapX.put(y, x, floatArrayOf(x.toFloat()))
                mapY.put(y, x, floatArrayOf(y.toFloat()))
            }
        }
    }

    val warped = Mat()
    Imgproc.remap(mat, warped, mapX, mapY, Imgproc.INTER_LINEAR)
    val result = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(warped, result)

    mat.release()
    mapX.release()
    mapY.release()
    warped.release()
    return result
}