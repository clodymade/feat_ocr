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
import android.graphics.*
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
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
    var licenseKey: String = ""
    val hiocr: HiOCR = HiOCR()
    var handle: Long = 0
    var lastScannedImage: Bitmap? = null

    private var isScanning: Boolean = false
    private var callback: ((HiOcrResult) -> Unit)? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var textRecognizer: TextRecognizer? = null
    private var cameraControl: CameraControl? = null
    private var activity: Activity? = null

    private var screenSize: Size = Size(0, 0)
    private var count: UByte = 0u // Counter ranging from 0 to 255

    /**
     * Initializes the OCR scanner and its components such as ML Kit and thread executors.
     */
    private fun initialize() {
        if (isScanning) return

        if (textRecognizer == null) {
            textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }

        if (cameraExecutor.isShutdown || cameraExecutor.isTerminated) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }

        handle = hiocr.createNativeAnalyzer(0, licenseKey)
        if (handle == 0L) {
            Log.e("HiCardScanner", "Failed to create native analyzer.")
            throw IllegalStateException("Native analyzer handle is invalid.")
        }

        Log.d("HiCardScanner", "Card scanner initialization complete")
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
        roiState: MutableState<Rect> = mutableStateOf(Rect(0, 0, 0, 0)),
        preProcessedState: MutableState<Bitmap?>? = null,
        callback: (HiOcrResult) -> Unit
    ) {
        if (isScanning) return

        this.activity = activity
        this.callback = callback

        initialize()

        val reqPermissions = HiOcrPermissionType.CAMERA.requiredPermissions()

        // Check if the permissions are granted
        if (!activity.hasPermissions(reqPermissions)) {
            if (activity is AppCompatActivity) {
                ActivityCompat.requestPermissions(
                    activity,
                    reqPermissions,
                    HiOcrPermissionReqCodes.OCR
                )
            } else if (activity is ComponentActivity) {
                val launcher = activity.registerForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val allGranted = permissions.all { it.value }
                    if (allGranted) {
                        startScanner(previewView, roiState, preProcessedState)
                    } else {
                        Log.e("HiCardScanner", "Permissions not granted.")
                    }
                }
                launcher.launch(reqPermissions)
            } else {
                throw IllegalArgumentException("Unsupported activity type")
            }
            return
        }

        // Start scanning if permissions are already granted
        startScanner(previewView, roiState, preProcessedState)
    }

    /**
     * Handles the core logic to start the scanner once permissions are granted.
     */
    private fun startScanner(
        previewView: PreviewView,
        roiState: MutableState<Rect>,
        preProcessedState: MutableState<Bitmap?>?
    ) {
        isScanning = true
        screenSize = getScreenSizeInPx()

        // Assign default values to the ROI rectangle if not already set.
        if (roiState.value.isEmpty) {
            val roiMargin = 10.0f
            val roiWidth = screenSize.width - (roiMargin * 2.0f)
            val roiHeight = roiWidth / 1.586f
            roiState.value = Rect(
                roiMargin.toInt(),
                ((screenSize.height - roiHeight) / 2.0f).toInt(),
                (roiWidth + roiMargin).toInt(),
                (((screenSize.height + roiHeight) / 2.0f)).toInt()
            )
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity!!)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val rotation = previewView.display?.rotation ?: Surface.ROTATION_0

            val preview = Preview.Builder()
                .setTargetRotation(rotation)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetRotation(rotation)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy, roiState.value, preProcessedState)
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
                Log.e("HiCardScanner", "Camera binding failed: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(activity!!))
    }

    /**
     * Stops the OCR scanner and releases resources.
     */
    fun stop() {
        if (!isScanning) return
        cameraExecutor.shutdown()

        textRecognizer?.let {
            it.close()
            textRecognizer = null
        }

        isScanning = false
        Log.d("HiCardScanner", "Card scanning stopped")
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
     * @param preProcessedState Mutable state for processed images.
     */
    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(
        imageProxy: ImageProxy,
        scanBox: Rect,
        preProcessedState: MutableState<Bitmap?>?
    ) {
        if (!isScanning) return

        try {
            // Convert YUV image to RGB Bitmap
            val rgbBitmap = imageProxy.toBitmap()
            val rotatedBitmap = rotateBitmap(rgbBitmap, imageProxy.imageInfo.rotationDegrees)
            val croppedImage = cropToCameraCoordinates(rotatedBitmap, scanBox, screenSize)
            val resizedImage = resizeImageToHeight(croppedImage, 1000.0f)
            lastScannedImage = resizedImage

            var preprocessedImage = resizedImage
            if (count % 3u == 0u) {
                preprocessedImage = HiImageProcessor.processCardImage(croppedImage)
                //preProcessedState?.value = preprocessedImage
            }

            val inputImage = InputImage.fromBitmap(preprocessedImage, 0)
            textRecognizer?.process(inputImage)
                ?.addOnSuccessListener { visionText ->
                    handleCardResult(visionText)
                }
                ?.addOnFailureListener { e ->
                    Log.e("HiCardScanner", "OCR failed: ${e.message}")
                    callback?.invoke(HiOcrResult("", "", "", "", "OCR Failed"))
                }
                ?.addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (e: Exception) {
            Log.e("HiCardScanner", "Image processing failed: ${e.message}")
            imageProxy.close()
        }
        count = (count + 1u).toUByte()
    }

    /**
     * Handles the result from OCR processing.
     * Extracts relevant card information and invokes the callback with the result.
     */
    private fun handleCardResult(visionText: Text) {
        val textInfoList = mutableListOf<HiTextInfo>()
        visionText.textBlocks.forEach { block ->
            block.boundingBox?.let { rect ->
                textInfoList.add(
                    HiTextInfo(
                        block.text,
                        HiRect(rect.left.toDouble(), rect.top.toDouble(), rect.width().toDouble(), rect.height().toDouble())
                    )
                )
            }
        }
        if (textInfoList.isEmpty()) return

        val textArray = textInfoList.toTypedArray()
        try {
            val resultJsonString = hiocr.analyzeTextDataNative(handle, textArray)
            val resultMap = resultJsonString.toMapOrList() as? Map<String, String> ?: emptyMap()
            if (resultMap.isNotEmpty()) {
                val result = HiOcrResult(
                    cardNumber = resultMap["card_number"] ?: "",
                    holderName = resultMap["holder_name"] ?: "",
                    expiryMonth = resultMap["expiry_month"] ?: "",
                    expiryYear = resultMap["expiry_year"] ?: "",
                    issuingNetwork = resultMap["issuing_network"] ?: ""
                )
                callback?.invoke(result)
            }
        } catch (e: Exception) {
            Log.e("HiCardScanner", "Error analyzing OCR results: ${e.message}")
            callback?.invoke(HiOcrResult("", "", "", "", "Analysis failed: ${e.message}"))
        }
    }

    /**
     * Retrieves the screen size in pixels using WindowMetrics.
     * @return The screen size as an instance of android.util.Size.
     */
    private fun getScreenSizeInPx(): Size {
        val windowMetrics = activity?.let {
            WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(it)
        }
        val bounds = windowMetrics?.bounds
        return Size(bounds?.width() ?: 0, bounds?.height() ?: 0)
    }

    /**
     * Crops the provided Bitmap to match the region of interest (ROI).
     * @param bitmap The source Bitmap to crop.
     * @param scanBox The rectangular region defining the area of interest.
     * @param screenSize The size of the screen to calculate scaling.
     * @return A cropped Bitmap.
     */
    private fun cropToCameraCoordinates(bitmap: Bitmap, scanBox: Rect, screenSize: Size): Bitmap {
        val cameraWidth = bitmap.width.toFloat()
        val cameraHeight = bitmap.height.toFloat()

        val widthScale = cameraWidth / screenSize.width
        val heightScale = cameraHeight / screenSize.height
        val minScale = min(widthScale, heightScale)

        val cropWidth = scanBox.width() * minScale
        val cropHeight = cropWidth / 1.586f
        val cropLeft = (cameraWidth - cropWidth) / 2
        val cropTop = (cameraHeight - cropHeight) / 2

        return Bitmap.createBitmap(
            bitmap,
            cropLeft.toInt(),
            cropTop.toInt(),
            cropWidth.toInt(),
            cropHeight.toInt()
        )
    }

    /**
     * Rotates a Bitmap by a specified degree.
     * @param bitmap The Bitmap to rotate.
     * @param rotationDegrees The degree of rotation (e.g., 90, 180, 270).
     * @return A rotated Bitmap.
     */
    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Resizes a Bitmap to a specified target height while maintaining the aspect ratio.
     * @param bitmap The Bitmap to resize.
     * @param targetHeight The desired height of the resized Bitmap.
     * @return A resized Bitmap.
     */
    private fun resizeImageToHeight(bitmap: Bitmap, targetHeight: Float): Bitmap {
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