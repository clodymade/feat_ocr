/**
 * File: HiCardScanner.kt
 * Description: Utility object for managing OCR (Optical Character Recognition) operations using the device's camera.
 *              This includes initialization, scanning, and analyzing card information such as card number, holder name,
 *              expiry date, and issuing network.
 *
 * Author: Your Name
 * Created: 2024-11-19
 *
 * License: Apache 2.0
 *
 * References:
 * - Android CameraX API: https://developer.android.com/training/camerax
 * - ML Kit Text Recognition: https://developers.google.com/ml-kit/vision/text-recognition
 */

package com.mwkg.ocr.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.window.layout.WindowMetricsCalculator
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mwkg.hiocr.HiOCR
import com.mwkg.hiocr.HiRect
import com.mwkg.hiocr.HiTextInfo
import com.mwkg.ocr.model.HiOcrResult
import com.mwkg.ocr.util.HiOcrToolkit.hasPermissions
import com.mwkg.ocr.util.HiOcrToolkit.toMapOrList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

@SuppressLint("StaticFieldLeak")
object HiCardScanner {
    private var isScanning: Boolean = false
    private var callback: ((HiOcrResult) -> Unit)? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var textRecognizer: TextRecognizer? = null
    private var cameraControl: CameraControl? = null
    private var activity: Activity? = null
    private var screenSize: androidx.compose.ui.geometry.Size = androidx.compose.ui.geometry.Size.Zero
    private val hiocr: HiOCR = HiOCR()
    private var handle: Long = 0
    private var count: UByte = 0u // Counter ranging from 0 to 255
    private var preProcessedState = mutableStateOf<Bitmap?>(null)

    var lastScannedImage: Bitmap? = null

    init {
        Log.d("ModularX::HiCardScanner", "Card scanner initialized")
    }

    /**
     * Initializes the OCR scanner.
     */
    private fun initialize() {
        if (isScanning) return

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        if (cameraExecutor.isShutdown) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
        handle = hiocr.createNativeAnalyzer(0, "9597cbfa0d43a47b3e48842bb1025b409bf8ab3919a89d44acaab17b08bebd14|1737361504")
        if (handle == 0L) {
            Log.e("ModularX", "Failed to create native analyzer.")
            throw IllegalStateException("Native analyzer handle is invalid.")
        }

        Log.d("ModularX::HiCardScanner", "Card scanner initialization complete")
    }

    /**
     * Starts the OCR scanner with the specified activity, preview view, and callback.
     * @param activity The activity context to use.
     * @param previewView The camera preview view.
     * @param roiState Mutable state for the region of interest (ROI).
     * @param preProcessedState Mutable state for processed images.
     * @param callback Callback to handle the OCR result.
     */
    fun start(
        activity: Activity,
        previewView: PreviewView,
        roiState: MutableState<androidx.compose.ui.geometry.Rect>,
        preProcessedState: MutableState<Bitmap?>,
        callback: (HiOcrResult) -> Unit
    ) {
        if (isScanning) return

        HiCardScanner.activity = activity
        HiCardScanner.callback = callback

        initialize()

        val reqPermissions = HiOcrPermissionType.CAMERA.requiredPermissions()
        if (!activity.hasPermissions(reqPermissions)) {
            activity.requestPermissions(reqPermissions, HiOcrPermissionReqCodes.OCR)
            return
        }

        isScanning = true
        HiCardScanner.preProcessedState = preProcessedState
        screenSize = getScreenSizeInPx()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetRotation(previewView.display.rotation)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetRotation(previewView.display.rotation)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy, roiState.value)
                    }
                }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    activity as LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
                cameraControl = camera.cameraControl
            } catch (exc: Exception) {
                Log.e("ModularX::HiCardScanner", "Camera binding failed: ${exc.message}")
            }

        }, ContextCompat.getMainExecutor(activity))
    }

    /**
     * Stops the OCR scanner and releases resources.
     */
    fun stop() {
        if (!isScanning) return

        cameraExecutor.shutdown()
        textRecognizer?.close()
        isScanning = false
        Log.d("ModularX::HiCardScanner", "Card scanning stopped")
    }

    /**
     * Checks if the required permissions are granted.
     * @return True if permissions are granted, otherwise false.
     */
    fun hasRequiredPermissions(): Boolean {
        return activity?.let {
            val reqPermissions = HiOcrPermissionType.CAMERA.requiredPermissions()
            it.hasPermissions(reqPermissions)
        } ?: false
    }

    /**
     * Toggles the device's flashlight (torch).
     * @param isOn True to turn on the flashlight, false to turn it off.
     */
    fun toggleTorch(isOn: Boolean) {
        cameraControl?.enableTorch(isOn)
    }

    /**
     * Processes the image captured by the camera.
     * @param imageProxy The captured image.
     * @param scanBox The region of interest (ROI) for scanning.
     */
    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy, scanBox: androidx.compose.ui.geometry.Rect) {
        // Exit if the scanner is not active
        if (!isScanning) return

        try {
            // Convert YUV image to RGB Bitmap
            val rgbBitmap = imageProxy.toBitmap() // Default size or configured resolution
            // Rotate the image to match the device orientation
            val rotatedBitmap =
                rotateBitmap(rgbBitmap, imageProxy.imageInfo.rotationDegrees)
            // Crop the bitmap to the specified ROI (Region of Interest)
            val croppedImage =
                cropToCameraCoordinates(rotatedBitmap, scanBox, screenSize)
            // Resize the cropped image for consistent processing
            val resizedImage = resizeImageToHeight(croppedImage, 1000.0f)
            // Store the latest scanned image for reference
            lastScannedImage = resizedImage

            // Update a counter to track image processing cycles
            count = (count + 1u).toUByte()

            // Optionally preprocess every third image for better OCR accuracy
            var preprocessedImage = croppedImage
            if (count % 3u == 0u) {
                preprocessedImage = HiImageProcessor.processCardImageForEmbossedText(croppedImage)
                //preProcessedState.value = preprocessedImage // Update the debug state
            }

            // Create an InputImage for ML Kit from the preprocessed bitmap
            val inputImage = InputImage.fromBitmap(preprocessedImage, 0)

            // Perform OCR on the input image
            textRecognizer?.process(inputImage)
                ?.addOnSuccessListener { visionText ->
                    // Handle the successful OCR result
                    handleCardResult(visionText)
                }
                ?.addOnFailureListener { e ->
                    // Log OCR failure and invoke the callback with an error result
                    Log.e("ModularX::HiCardScanner", "OCR failed: ${e.message}")
                    callback?.invoke(HiOcrResult("", "", "", "", "OCR Failed"))
                }
                ?.addOnCompleteListener {
                    // Close the image proxy after processing
                    imageProxy.close()
                }
        } catch (e: Exception) {
            // Log and handle exceptions during image processing
            Log.e("ModularX::HiCardScanner", "Image processing failed: ${e.message}")
            imageProxy.close()
        }
    }

    /**
     * Handles the result from OCR processing.
     * Extracts relevant card information and invokes the callback with the result.
     */
    private fun handleCardResult(visionText: Text) {
        // List to store extracted text information
        val hiTextInfoList = mutableListOf<HiTextInfo>()

        // Process each block of text detected
        visionText.textBlocks.forEach { block ->
            block.boundingBox?.let { rect ->
                hiTextInfoList.add(
                    HiTextInfo(
                        block.text,
                        HiRect(
                            rect.left.toDouble(),
                            rect.top.toDouble(),
                            (rect.right - rect.left).toDouble(),
                            (rect.bottom - rect.top).toDouble()
                        )
                    )
                )
            }
            // Process each line within the block
            block.lines.forEach { line ->
                line.boundingBox?.let { rect ->
                    hiTextInfoList.add(
                        HiTextInfo(
                            line.text,
                            HiRect(
                                rect.left.toDouble(),
                                rect.top.toDouble(),
                                (rect.right - rect.left).toDouble(),
                                (rect.bottom - rect.top).toDouble()
                            )
                        )
                    )
                }
            }
        }

        // Exit if no text information was extracted
        if (hiTextInfoList.isEmpty()) return

        val textArray = hiTextInfoList.toTypedArray()

        try {
            // Analyze the extracted text data using the native HiOCR library
            val resultJsonString = hiocr.analyzeTextDataNative(handle, textArray)
            val resultMap = resultJsonString.toMapOrList() as? Map<String, String> ?: emptyMap()

            // Extract relevant card information from the result map
            val cardNumber = resultMap["card_number"]?.let { hiocr.decryptionDataNative(handle, it) } ?: ""
            val holderName = resultMap["holder_name"]?.let { hiocr.decryptionDataNative(handle, it) } ?: ""
            val expiryMonth = resultMap["expiry_month"]?.let { hiocr.decryptionDataNative(handle, it) } ?: ""
            val expiryYear = resultMap["expiry_year"]?.let { hiocr.decryptionDataNative(handle, it) } ?: ""
            val issuingNetwork = resultMap["issuing_network"]?.let { hiocr.decryptionDataNative(
                handle, it) } ?: ""

            // Format and invoke the result callback
            if (cardNumber.isNotEmpty()) {
                val expiryDate = "$expiryMonth/$expiryYear"
                val result = HiOcrResult(
                    cardNumber = cardNumber,
                    holderName = holderName,
                    expiryDate = expiryDate,
                    issuingNetwork = issuingNetwork
                )
                callback?.invoke(result)
            } else {
                callback?.invoke(HiOcrResult("", "", "", "", "No OCR results"))
            }
        } catch (e: Exception) {
            // Log and handle errors during result analysis
            Log.e("ModularX::HiCardScanner", "Error analyzing OCR results: ${e.message}")
            callback?.invoke(HiOcrResult("", "", "", "", "Analysis failed: ${e.message}"))
        }
    }

    /**
     * Retrieves the screen size in pixels using WindowMetrics.
     *
     * @return The screen size as an instance of androidx.compose.ui.geometry.Size.
     * The width and height are obtained from the device's current window bounds.
     */
    private fun getScreenSizeInPx(): androidx.compose.ui.geometry.Size {
        val windowMetrics: androidx.window.layout.WindowMetrics? = activity?.let {
            WindowMetricsCalculator
                .getOrCreate()
                .computeCurrentWindowMetrics(it) // Computes the current window metrics
        }

        // Get the bounds of the current window metrics
        val bounds = windowMetrics?.bounds
        val width = bounds?.width() ?: 0 // Extract screen width
        val height = bounds?.height() ?: 0 // Extract screen height

        // Return the screen size as a Size object with floating-point values
        return androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())
    }

    /**
     * Retrieves the maximum supported resolution of the device's default camera.
     *
     * @param context The application context to access the camera service.
     * @return The maximum supported resolution as a Size object.
     * If the resolution is unavailable, defaults to 1280x720.
     */
    private fun getMaxResolution(context: Context): Size {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.first() // Selects the first available camera
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)

        // Retrieve the supported output sizes for YUV_420_888 format
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val outputSizes = map?.getOutputSizes(ImageFormat.YUV_420_888)

        // Select the maximum resolution available or default to 1280x720
        return outputSizes?.maxByOrNull { it.width * it.height } ?: Size(1280, 720)
    }


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
}
