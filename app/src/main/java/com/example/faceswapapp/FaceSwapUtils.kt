package com.example.faceswapapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Subdiv2D
import org.opencv.imgproc.Imgproc
import org.opencv.photo.Photo
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

fun appendDebugLog(context: Context, message: String) {
    Log.d("FaceSwapDebugFile", message)
    try {
        val file = File(context.filesDir, "face_swap_debug.txt")
        FileWriter(file, true).use { writer ->
            writer.appendLine("[${System.currentTimeMillis()}] $message")
        }
    } catch (e: Exception) {
        Log.e("FaceSwapDebugFile", "Errore scrittura file debug: ${e.localizedMessage}", e)
    }
}

// Salva SEMPRE una bitmap di debug
fun saveBitmapDebug(context: Context, bitmap: Bitmap, step: String): String {
    val file = File(context.filesDir, "debug_${step}_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return file.absolutePath
}

fun saveMatDebug(context: Context, mat: Mat, step: String): String {
    val bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(mat, bmp)
    return saveBitmapDebug(context, bmp, step)
}

// Trova indice punto più vicino (entro soglia) – fix per triangolazione MediaPipe
fun findClosestIndex(points: List<Point>, pt: org.opencv.core.Point, maxDistSquare: Double = 16.0): Int {
    var minIdx = -1
    var minDist = Double.MAX_VALUE
    points.forEachIndexed { idx, p ->
        val dist = (p.x - pt.x) * (p.x - pt.x) + (p.y - pt.y) * (p.y - pt.y)
        if (dist < minDist) {
            minDist = dist
            minIdx = idx
        }
    }
    return if (minDist < maxDistSquare) minIdx else -1
}

object FaceSwapUtils {

    fun calculateDelaunayTriangles(width: Int, height: Int, points: List<Point>): List<Triple<Int, Int, Int>> {
        val rect = Rect(0, 0, width, height)
        val subdiv = Subdiv2D(rect)
        points.forEach { subdiv.insert(org.opencv.core.Point(it.x.toDouble(), it.y.toDouble())) }
        val triangleListMat = MatOfFloat6()
        subdiv.getTriangleList(triangleListMat)
        val triangleList = triangleListMat.toArray()
        val triangles = mutableListOf<Triple<Int, Int, Int>>()
        for (i in triangleList.indices step 6) {
            val pts = listOf(
                org.opencv.core.Point(triangleList[i].toDouble(), triangleList[i + 1].toDouble()),
                org.opencv.core.Point(triangleList[i + 2].toDouble(), triangleList[i + 3].toDouble()),
                org.opencv.core.Point(triangleList[i + 4].toDouble(), triangleList[i + 5].toDouble())
            )
            if (pts.all { it.x >= 0 && it.x < width && it.y >= 0 && it.y < height }) {
                val idx = pts.map { pt -> findClosestIndex(points, pt) }
                if (idx.all { it >= 0 } && idx.distinct().size == 3) {
                    triangles.add(Triple(idx[0], idx[1], idx[2]))
                }
            }
        }
        Log.d("FaceSwapTriangles", "Delaunay triangles calculated: ${triangles.size} on $width x $height image")
        return triangles
    }

    fun swapFaceWithTriangles(
        source: Bitmap,
        dest: Bitmap,
        sourceLandmarks: List<Point>,
        destLandmarks: List<Point>,
        triangles: List<Triple<Int, Int, Int>>,
        maskHull: MatOfPoint? = null,
        context: Context? = null
    ): Bitmap {
        val srcMatCv = Mat()
        val destMatCv = Mat()
        val outputMat = Mat(dest.height, dest.width, CvType.CV_8UC3)
        outputMat.setTo(Scalar.all(0.0))

        val tempSrc = Bitmap.createScaledBitmap(source, dest.width, dest.height, true)
        Utils.bitmapToMat(tempSrc, srcMatCv)
        Utils.bitmapToMat(dest, destMatCv)
        if (srcMatCv.channels() == 4) Imgproc.cvtColor(srcMatCv, srcMatCv, Imgproc.COLOR_RGBA2BGR)
        if (destMatCv.channels() == 4) Imgproc.cvtColor(destMatCv, destMatCv, Imgproc.COLOR_RGBA2BGR)

        context?.let {
            val srcPath = saveBitmapDebug(it, tempSrc, "step1_source_scaled")
            appendDebugLog(it, "Immagine sorgente scalata salvata: $srcPath")
            val destPath = saveBitmapDebug(it, dest, "step2_dest_original")
            appendDebugLog(it, "Immagine destinazione originale salvata: $destPath")
        }

        val mask = Mat.zeros(dest.height, dest.width, CvType.CV_8UC1)
        val hullPoints = MatOfPoint(*destLandmarks.map { org.opencv.core.Point(it.x.toDouble(), it.y.toDouble()) }.toTypedArray())
        val hullIndices = MatOfInt()
        Imgproc.convexHull(hullPoints, hullIndices)
        val hullList = hullIndices.toArray().map { hullPoints.toArray()[it] }
        val hullMat = MatOfPoint(*hullList.toTypedArray())
        Imgproc.fillConvexPoly(mask, hullMat, Scalar(255.0))
        Log.d("FaceSwapTriangles", "Nonzero in mask: ${Core.countNonZero(mask)}")

        var trianglesCopied = 0
        var trianglesTotal = 0

        for (tri in triangles) {
            trianglesTotal++
            val srcTri = listOf(
                sourceLandmarks[tri.first],
                sourceLandmarks[tri.second],
                sourceLandmarks[tri.third]
            )
            val dstTri = listOf(
                destLandmarks[tri.first],
                destLandmarks[tri.second],
                destLandmarks[tri.third]
            )

            val dstTriMat = MatOfPoint(
                org.opencv.core.Point(dstTri[0].x.toDouble(), dstTri[0].y.toDouble()),
                org.opencv.core.Point(dstTri[1].x.toDouble(), dstTri[1].y.toDouble()),
                org.opencv.core.Point(dstTri[2].x.toDouble(), dstTri[2].y.toDouble())
            )

            val r = Imgproc.boundingRect(dstTriMat)
            val srcTriMat = MatOfPoint(
                org.opencv.core.Point(srcTri[0].x.toDouble(), srcTri[0].y.toDouble()),
                org.opencv.core.Point(srcTri[1].x.toDouble(), srcTri[1].y.toDouble()),
                org.opencv.core.Point(srcTri[2].x.toDouble(), srcTri[2].y.toDouble())
            )
            val srcR = Imgproc.boundingRect(srcTriMat)

            if (srcR.x < 0 || srcR.y < 0) continue
            if (srcR.x + srcR.width > srcMatCv.cols() || srcR.y + srcR.height > srcMatCv.rows()) continue
            if (r.x < 0 || r.y < 0) continue
            if (r.x + r.width > outputMat.cols() || r.y + r.height > outputMat.rows()) continue
            if (srcR.width <= 0 || srcR.height <= 0 || r.width <= 0 || r.height <= 0) continue

            val srcTriOffset = srcTri.map { Point(it.x - srcR.x, it.y - srcR.y) }
            val dstTriOffset = dstTri.map { Point(it.x - r.x, it.y - r.y) }

            val srcPatch = srcMatCv.submat(srcR).clone()
            val maskTri = Mat.zeros(r.height, r.width, CvType.CV_8UC1)
            val dstTriOffsetMat = MatOfPoint(
                org.opencv.core.Point(dstTriOffset[0].x.toDouble(), dstTriOffset[0].y.toDouble()),
                org.opencv.core.Point(dstTriOffset[1].x.toDouble(), dstTriOffset[1].y.toDouble()),
                org.opencv.core.Point(dstTriOffset[2].x.toDouble(), dstTriOffset[2].y.toDouble())
            )
            Imgproc.fillConvexPoly(maskTri, dstTriOffsetMat, Scalar(255.0))

            val srcTriMatf = MatOfPoint2f(
                org.opencv.core.Point(srcTriOffset[0].x.toDouble(), srcTriOffset[0].y.toDouble()),
                org.opencv.core.Point(srcTriOffset[1].x.toDouble(), srcTriOffset[1].y.toDouble()),
                org.opencv.core.Point(srcTriOffset[2].x.toDouble(), srcTriOffset[2].y.toDouble())
            )
            val dstTriMatf = MatOfPoint2f(
                org.opencv.core.Point(dstTriOffset[0].x.toDouble(), dstTriOffset[0].y.toDouble()),
                org.opencv.core.Point(dstTriOffset[1].x.toDouble(), dstTriOffset[1].y.toDouble()),
                org.opencv.core.Point(dstTriOffset[2].x.toDouble(), dstTriOffset[2].y.toDouble())
            )
            val warpMat = Imgproc.getAffineTransform(srcTriMatf, dstTriMatf)

            val warpedPatch = Mat.zeros(r.height, r.width, srcMatCv.type())
            Imgproc.warpAffine(srcPatch, warpedPatch, warpMat, warpedPatch.size(), Imgproc.INTER_LINEAR, Core.BORDER_REFLECT_101)

            val roi = outputMat.submat(r)
            warpedPatch.copyTo(roi, maskTri)

            trianglesCopied++
        }

        context?.let {
            val maskPath = saveMatDebug(it, mask, "step3_mask")
            appendDebugLog(it, "Maschera convex hull triangoli salvata: $maskPath")
            val outTriPath = saveMatDebug(it, outputMat, "step4_output_triangles")
            appendDebugLog(it, "Output dopo copia triangoli salvato: $outTriPath")
        }

        Log.i("FaceSwapTriangles", "swapFaceWithTriangles: triangles total=$trianglesTotal, triangles copied=$trianglesCopied")
        context?.let { appendDebugLog(it, "swapFaceWithTriangles: triangles total=$trianglesTotal, triangles copied=$trianglesCopied") }

        val hullCentroid = if (hullList.isNotEmpty()) {
            val x = hullList.map { it.x }.average()
            val y = hullList.map { it.y }.average()
            org.opencv.core.Point(x, y)
        } else {
            val centerX = destLandmarks.map { it.x }.average()
            val centerY = destLandmarks.map { it.y }.average()
            org.opencv.core.Point(centerX, centerY)
        }

        // PATCH: salva SEMPRE file debug finale, anche se fallisce!
        if (Core.countNonZero(mask) == 0 || trianglesCopied == 0) {
            Log.e("FaceSwapTriangles", "No triangles copied! Returning original dest image.")
            context?.let { appendDebugLog(it, "No triangles copied! Returning original dest image.") }
            val resultBitmap = Bitmap.createBitmap(dest.width, dest.height, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(destMatCv, resultBitmap)
            context?.let {
                val failPath = saveBitmapDebug(it, resultBitmap, "step5_fail_return_dest")
                appendDebugLog(it, "Nessun triangolo copiato: restituita l'immagine originale: $failPath")
            }
            return resultBitmap
        }

        val cloned = Mat()
        Photo.seamlessClone(outputMat, destMatCv, mask, hullCentroid, cloned, Photo.NORMAL_CLONE)
        val clonedBGRtoRGBA = Mat()
        Imgproc.cvtColor(cloned, clonedBGRtoRGBA, Imgproc.COLOR_BGR2RGBA)
        val resultBitmap = Bitmap.createBitmap(dest.width, dest.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(clonedBGRtoRGBA, resultBitmap)

        context?.let {
            val finalPath = saveBitmapDebug(it, resultBitmap, "step6_final_blended")
            appendDebugLog(it, "Risultato finale blended salvato: $finalPath")
        }
        return resultBitmap
    }

    fun offsetsToPoints(offsets: List<androidx.compose.ui.geometry.Offset>): List<Point> =
        offsets.map { Point(it.x.toInt(), it.y.toInt()) }

    fun opencvPointsToAndroid(points: List<org.opencv.core.Point>): List<Point> =
        points.map { Point(it.x.toInt(), it.y.toInt()) }
}