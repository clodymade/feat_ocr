/**
 * File: HiCardNumberListActivity.kt
 *
 * Description: Activity for managing and displaying a list of scanned card details using Jetpack Compose.
 *              Integrates the HiCardScanner for OCR-based card scanning and updates the ViewModel with
 *              scanned data. Also handles ROI (Region of Interest) and preprocessing states.
 *
 * Author: netcanis
 * Created: 2024-11-19
 *
 * License: Apache 2.0
 *
 * References:
 * - Jetpack Compose Activity Integration: https://developer.android.com/jetpack/compose
 * - Android CameraX Documentation: https://developer.android.com/training/camerax
 * - Mutable State in Compose: https://developer.android.com/jetpack/compose/state
 */

package com.mwkg.ocr.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import com.mwkg.ocr.model.HiCardNumber
import com.mwkg.ocr.util.HiCardScanner
import com.mwkg.ocr.util.HiCardScanner.handle
import com.mwkg.ocr.util.HiCardScanner.hiocr
import com.mwkg.ocr.util.HiOcrPermission
import com.mwkg.ocr.util.HiOcrPermissionReqCodes
import com.mwkg.ocr.util.HiOcrPermissionType
import com.mwkg.ocr.util.HiOcrToolkit.hasPermissions
import com.mwkg.ocr.util.HiOcrToolkit.toPrettyJsonString
import com.mwkg.ocr.viewmodel.HiCardNumberListViewModel

/**
 * Activity class for scanning and displaying card details using OCR.
 * It manages the CameraX preview, invokes HiCardScanner for OCR processing, and updates the ViewModel with scanned results.
 */
class HiCardNumberListActivity : ComponentActivity() {
    // ViewModel instance to manage the list of scanned card details
    private val viewModel: HiCardNumberListViewModel by viewModels()

    // State to manage the visibility of the camera preview
    private var isPreviewVisible = mutableStateOf(true)

    // State to manage the Region of Interest (ROI) for card scanning
    private val roiState = mutableStateOf(Rect(0, 0, 0, 0))

    // State to manage the preprocessed bitmap for debugging or additional processing
    private val preProcessedState = mutableStateOf<Bitmap?>(null)

    private lateinit var previewView: PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the CameraX preview view
        previewView = PreviewView(this)

        // Set up the content view with the Compose UI
        setContent {
            HiCardNumberListActivityScreen(
                cardNumbers = viewModel.numbers, // Pass the scanned card numbers to the UI
                previewView = previewView, // CameraX preview view
                onBackPressed = { finish() }, // Handle the back press
                onToggleTorch = { isOn -> HiCardScanner.toggleTorch(isOn) }, // Toggle the flashlight
                isPreviewVisible = isPreviewVisible, // Pass the preview visibility state
                roiState = roiState, // Pass the ROI state
                preProcessedState = preProcessedState, // Pass the preprocessed bitmap state
                onCardScanned = { cardNumber -> viewModel.update(cardNumber) } // Update the ViewModel with new scanned data
            )
        }

        startCardScanner()
    }

    private fun startCardScanner() {
        // Start the card scanner
        HiCardScanner.start(
            activity = this,
            previewView = previewView,
            roiState = roiState,
            preProcessedState = preProcessedState
        ) { result ->
            // Log the scanned result in JSON format
            Log.d("ModularX", result.toPrettyJsonString())

            // Extract relevant card information from the result map
            val cardNumber = result.cardNumber.let { hiocr.decryptionDataNative(handle, it) } ?: ""
            val holderName = result.holderName.let { hiocr.decryptionDataNative(handle, it) } ?: ""
            val expiryMonth = result.expiryMonth.let { hiocr.decryptionDataNative(handle, it) } ?: ""
            val expiryYear = result.expiryYear.let { hiocr.decryptionDataNative(handle, it) } ?: ""
            val issuingNetwork = result.issuingNetwork.let { hiocr.decryptionDataNative(handle, it) } ?: ""

            // If the card number is valid, update the ViewModel
            if (cardNumber.isNotEmpty()) {
                val code = HiCardNumber(
                    cardNumber = cardNumber,
                    holderName = holderName,
                    expiryMonth = expiryMonth,
                    expiryYear = expiryYear,
                    issuingNetwork = issuingNetwork,
                    scannedAt = System.currentTimeMillis() // Current timestamp
                )
                viewModel.update(code)

                // Stop the scanner and hide the preview
                HiCardScanner.stop()
                isPreviewVisible.value = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the scanner when the activity is destroyed
        HiCardScanner.stop()
    }
}