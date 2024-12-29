/**
 * File: HiOcrPreProcessImage.kt
 *
 * Description: This utility class provides a variety of image preprocessing functions for OCR and
 *              other image processing tasks. It includes methods for YUV-to-Bitmap conversion,
 *              cropping, resizing, rotation, contrast and brightness adjustments, and adaptive color
 *              inversion. These functions are designed to enhance image quality for text recognition
 *              and other applications.
 *
 * Author: netcanis
 * Created: 2024-12-24
 *
 * License: MIT
 */

package com.mwkg.ocr.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

/**
 * Utility object for preprocessing images, with a focus on enhancing OCR capabilities.
 * Includes functions for cropping, rotating, resizing, and optimizing brightness and contrast.
 */
internal object HiOcrPreProcessImage {

    /**
     * Crops the provided Bitmap to match the region of interest (ROI).
     *
     * @param bitmap The source Bitmap to crop.
     * @param scanBox The rectangular region defining the area of interest.
     * @param screenSize The size of the screen to calculate scaling.
     * @return A cropped Bitmap.
     */
    fun cropToCameraCoordinates(
        bitmap: Bitmap,
        scanBox: androidx.compose.ui.geometry.Rect,
        screenSize: androidx.compose.ui.geometry.Size
    ): Bitmap {
        val cameraWidth = bitmap.width
        val cameraHeight = bitmap.height

        val widthScale = cameraWidth / screenSize.width
        val heightScale = cameraHeight / screenSize.height
        val minScale = min(widthScale, heightScale)

        val cropHeight = scanBox.height * minScale
        val cropWidth = cropHeight * 1.586 // Aspect ratio
        val cropOriginX = (cameraWidth - cropWidth) / 2
        val cropOriginY = (cameraHeight - (scanBox.top + scanBox.height) * minScale)

        return Bitmap.createBitmap(
            bitmap,
            cropOriginX.toInt(),
            cropOriginY.toInt(),
            cropWidth.toInt(),
            cropHeight.toInt()
        )
    }

    /**
     * Rotates a Bitmap by a specified degree.
     *
     * @param bitmap The Bitmap to rotate.
     * @param rotationDegrees The degree of rotation (e.g., 90, 180, 270).
     * @return A rotated Bitmap.
     */
    fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Resizes a Bitmap to a specified target height while maintaining the aspect ratio.
     *
     * @param bitmap The Bitmap to resize.
     * @param targetHeight The desired height of the resized Bitmap.
     * @return A resized Bitmap.
     */
    fun resizeImageToHeight(bitmap: Bitmap, targetHeight: Float): Bitmap {
        val scale = targetHeight / bitmap.height
        val targetWidth = bitmap.width * scale

        val resizedBitmap = Bitmap.createBitmap(
            targetWidth.toInt(),
            targetHeight.toInt(),
            bitmap.config ?: Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(resizedBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return resizedBitmap
    }

    /**
     * Prepares a Bitmap for text recognition by converting it to grayscale and optimizing it.
     *
     * @param original The original Bitmap to preprocess.
     * @return A preprocessed Bitmap optimized for OCR tasks.
     */
    fun preprocessForTextRecognition(original: Bitmap): Bitmap {
        val grayscaleBitmap =
            Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(original, 0f, 0f, paint)

        return analyzeAndProcessImage(grayscaleBitmap)
    }

    /**
     * Analyzes and adjusts the brightness and contrast of a Bitmap.
     *
     * @param bitmap The Bitmap to analyze.
     * @return A processed Bitmap with adjusted brightness and contrast.
     */
    private fun analyzeAndProcessImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        var totalBrightness = 0L
        var minBrightness = 255
        var maxBrightness = 0

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        pixels.forEach { pixel ->
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val brightness = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            totalBrightness += brightness
            minBrightness = min(minBrightness, brightness)
            maxBrightness = maxOf(maxBrightness, brightness)
        }

        val averageBrightness = totalBrightness / (width * height)
        val isBrightBackground = averageBrightness > 192
        val contrastFactor = calculateContrastFactor(minBrightness, maxBrightness)
        val brightnessOffset = calculateBrightnessOffset(averageBrightness, isBrightBackground)

        val adjustedBitmap = adjustContrastAndBrightness(bitmap, contrastFactor, brightnessOffset)
        return if (isBrightBackground) invertColors(adjustedBitmap) else adjustedBitmap
    }

    /**
     * Adjusts the contrast and brightness of a Bitmap.
     *
     * @param bitmap The Bitmap to adjust.
     * @param contrastFactor The factor to adjust the contrast.
     * @param brightnessOffset The offset to adjust the brightness.
     * @return A Bitmap with adjusted contrast and brightness.
     */
    private fun adjustContrastAndBrightness(
        bitmap: Bitmap,
        contrastFactor: Float,
        brightnessOffset: Int
    ): Bitmap {
        val adjustedBitmap =
            Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(adjustedBitmap)
        val paint = Paint()
        val contrastMatrix = ColorMatrix().apply {
            set(
                floatArrayOf(
                    contrastFactor, 0f, 0f, 0f, brightnessOffset.toFloat(),
                    0f, contrastFactor, 0f, 0f, brightnessOffset.toFloat(),
                    0f, 0f, contrastFactor, 0f, brightnessOffset.toFloat(),
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return adjustedBitmap
    }

    /**
     * Inverts the colors of a Bitmap, emphasizing dark text on a light background.
     *
     * @param bitmap The Bitmap to invert.
     * @return A color-inverted Bitmap.
     */
    fun invertColors(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val invertedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val r = 255 - Color.red(pixel)
                val g = 255 - Color.green(pixel)
                val b = 255 - Color.blue(pixel)
                val a = Color.alpha(pixel)

                if (r + g + b < 400) {
                    invertedBitmap.setPixel(x, y, Color.BLACK)
                } else {
                    invertedBitmap.setPixel(x, y, Color.WHITE)
                }
            }
        }
        return invertedBitmap
    }

    /**
     * Calculates a sigmoid adjustment factor for brightness correction.
     *
     * @param brightness The brightness of the current pixel.
     * @param midPoint The midpoint for brightness adjustment.
     * @param maxBrightness The maximum brightness value.
     * @return A corrected brightness value.
     */
    private fun sigmoidAdjustmentEnhanced(brightness: Double, midPoint: Double, maxBrightness: Double): Double {
        val steepness = 10.0
        val proximityFactor = 2.0

        val distanceFromMid = abs(brightness - midPoint)
        val normalizedDistance = distanceFromMid / (maxBrightness / 2.0)

        val adjustmentFactor = exp(-proximityFactor * normalizedDistance)
        return if (brightness >= midPoint) {
            adjustmentFactor * (1 / (1 + exp(-steepness * ((brightness - midPoint) / maxBrightness))))
        } else {
            adjustmentFactor * (1 - (1 / (1 + exp(-steepness * ((brightness - midPoint) / maxBrightness)))))
        }
    }

    /**
     * Calculates the contrast factor based on the minimum and maximum brightness values.
     *
     * @param minBrightness The minimum brightness value in the image.
     * @param maxBrightness The maximum brightness value in the image.
     * @return The contrast adjustment factor.
     */
    private fun calculateContrastFactor(minBrightness: Int, maxBrightness: Int): Float {
        val range = maxBrightness - minBrightness
        return if (range > 0) {
            255f / range
        } else {
            1f // No adjustment needed if there's no brightness range.
        }
    }

    /**
     * Calculates the brightness offset based on the average brightness and background type.
     *
     * @param averageBrightness The average brightness value in the image.
     * @param isBrightBackground Whether the background is bright or not.
     * @return The brightness offset for adjustment.
     */
    private fun calculateBrightnessOffset(averageBrightness: Long, isBrightBackground: Boolean): Int {
        return if (isBrightBackground) {
            -15 // Slightly darken bright backgrounds.
        } else {
            (128 - averageBrightness).toInt() // Brighten dark backgrounds.
        }
    }
}