package com.mwkg.ocr.util

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

object HiImageProcessor {

    init {
        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("HiImageProcessor", "OpenCV initialization failed")
        } else {
            Log.d("HiImageProcessor", "OpenCV initialized successfully")
        }
    }

    // Process Card Image
    fun processCardImage(bitmap: Bitmap): Bitmap {
        // Convert Bitmap to Mat
        val originalMat = Mat()
        Utils.bitmapToMat(bitmap, originalMat)

        // Convert to grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Apply Gaussian blur
        val blurredMat = Mat()
        Imgproc.GaussianBlur(grayMat, blurredMat, Size(21.0, 21.0), 0.0)

        // Subtract blurred image from grayscale
        val diffMat = Mat()
        Core.subtract(grayMat, blurredMat, diffMat)

        // Apply CLAHE
        val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        val claheMat = Mat()
        clahe.apply(diffMat, claheMat)

        // Edge detection with Canny
        val edgeMat = Mat()
        Imgproc.Canny(claheMat, edgeMat, 50.0, 150.0)

        // Morphological close operation
        val morphMat = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.morphologyEx(edgeMat, morphMat, Imgproc.MORPH_CLOSE, kernel)

        // Adaptive thresholding
        val thresholdMat = Mat()
        Imgproc.adaptiveThreshold(
            morphMat, thresholdMat, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY, 15, -2.0
        )

        // Convert Mat back to Bitmap
        val processedBitmap = Bitmap.createBitmap(thresholdMat.cols(), thresholdMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(thresholdMat, processedBitmap)

        // Release Mat resources
        originalMat.release()
        grayMat.release()
        blurredMat.release()
        diffMat.release()
        claheMat.release()
        edgeMat.release()
        morphMat.release()
        thresholdMat.release()

        return processedBitmap
    }

    // Process Card Image for Embossed Text
    fun processCardImageForEmbossedText(bitmap: Bitmap): Bitmap {
        // Convert Bitmap to Mat
        val originalMat = Mat()
        Utils.bitmapToMat(bitmap, originalMat)

        // Convert color format to BGR if necessary
        when (originalMat.type()) {
            CvType.CV_8UC4 -> Imgproc.cvtColor(originalMat, originalMat, Imgproc.COLOR_RGBA2BGR)
            CvType.CV_8UC3 -> Imgproc.cvtColor(originalMat, originalMat, Imgproc.COLOR_RGB2BGR)
            else -> return bitmap
        }

        // Convert to HSV
        val hsvMat = Mat()
        Imgproc.cvtColor(originalMat, hsvMat, Imgproc.COLOR_BGR2HSV)

        // Reshape for K-means clustering
        val reshapedMat = hsvMat.reshape(1, hsvMat.rows() * hsvMat.cols())
        reshapedMat.convertTo(reshapedMat, CvType.CV_32F)

        // Perform K-means clustering
        val labels = Mat()
        val centers = Mat()
        val K = 5
        Core.kmeans(
            reshapedMat, K, labels,
            TermCriteria(TermCriteria.EPS + TermCriteria.COUNT, 10, 1.0),
            3, Core.KMEANS_PP_CENTERS, centers
        )

        // Determine dominant color
        val counts = IntArray(K)
        for (i in 0 until labels.rows()) {
            counts[labels[i, 0][0].toInt()]++
        }
        val dominantColorIndex = counts.indices.maxByOrNull { counts[it] } ?: 0
        val backgroundColor = centers.get(dominantColorIndex, 0)

        // Convert background color to Scalar
        val backgroundScalar = Scalar(backgroundColor[0], backgroundColor[1], backgroundColor[2])

        // Convert to grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Create mask for background color range
        val lowerBound = Scalar(backgroundScalar.`val`[0] - 10, backgroundScalar.`val`[1] - 40, backgroundScalar.`val`[2] - 40)
        val upperBound = Scalar(backgroundScalar.`val`[0] + 10, backgroundScalar.`val`[1] + 40, backgroundScalar.`val`[2] + 40)
        val mask = Mat()
        Core.inRange(hsvMat, lowerBound, upperBound, mask)

        // Adjust non-background pixels
        val processedMat = originalMat.clone()
        for (y in 0 until processedMat.rows()) {
            for (x in 0 until processedMat.cols()) {
                if (mask[y, x][0] == 0.0) {
                    val brightness = grayMat[y, x][0].toInt()
                    val adjustedColor = Scalar(
                        backgroundScalar.`val`[0] * (brightness / 255.0),
                        backgroundScalar.`val`[1] * (brightness / 255.0),
                        backgroundScalar.`val`[2] * (brightness / 255.0)
                    )
                    processedMat.put(y, x, adjustedColor.`val`[0], adjustedColor.`val`[1], adjustedColor.`val`[2])
                }
            }
        }

        // Convert back to RGB
        val rgbMat = Mat()
        Imgproc.cvtColor(processedMat, rgbMat, Imgproc.COLOR_BGR2RGB)

        // Convert Mat to Bitmap
        val processedBitmap = Bitmap.createBitmap(rgbMat.cols(), rgbMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgbMat, processedBitmap)

        // Release Mat resources
        originalMat.release()
        hsvMat.release()
        reshapedMat.release()
        labels.release()
        centers.release()
        grayMat.release()
        mask.release()
        processedMat.release()
        rgbMat.release()

        return processedBitmap
    }
}