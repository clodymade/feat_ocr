/**
 * File: HiCardNumberListActivityScreen.kt
 *
 * Description: Composable function that serves as the main UI for the card number scanning activity.
 *              It toggles between the CameraX preview screen and the scanned card number list screen
 *              based on the state of `isPreviewVisible`.
 *
 * Author: netcanis
 * Created: 2024-11-19
 *
 * License: Apache 2.0
 *
 * References:
 * - Jetpack Compose State Management: https://developer.android.com/jetpack/compose/state
 * - Android CameraX Preview Integration: https://developer.android.com/training/camerax
 */

package com.mwkg.ocr.view

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.StateFlow
import com.mwkg.ocr.model.HiCardNumber

/**
 * Main screen for the card number scanning activity. It switches between the CameraX preview screen
 * and the list of scanned card numbers.
 *
 * @param cardNumbers StateFlow providing the list of scanned card numbers.
 * @param previewView The CameraX PreviewView instance.
 * @param onBackPressed Lambda invoked when the back button is pressed.
 * @param onToggleTorch Lambda to toggle the flashlight.
 * @param isPreviewVisible State controlling whether the CameraX preview is visible.
 * @param roiState Mutable state to manage the Region of Interest (ROI) information.
 * @param preProcessedState Mutable state to manage the processed bitmap for debugging.
 * @param onCardScanned Lambda invoked when a card is successfully scanned.
 */
@Composable
fun HiCardNumberListActivityScreen(
    cardNumbers: StateFlow<List<HiCardNumber>>, // List of scanned card numbers
    previewView: PreviewView, // CameraX PreviewView
    onBackPressed: () -> Unit, // Action for back button press
    onToggleTorch: (Boolean) -> Unit, // Action to toggle flashlight
    isPreviewVisible: MutableState<Boolean>, // State for preview visibility
    roiState: MutableState<Rect>, // ROI state
    preProcessedState: MutableState<Bitmap?>, // State for debugging processed images
    onCardScanned: (HiCardNumber) -> Unit // Action when a card is scanned
) {
    if (isPreviewVisible.value) {
        // Display the CameraX preview screen
        HiCardNumberPreviewScreen(
            onToggleTorch = onToggleTorch, // Pass flashlight toggle action
            onClosePreview = { isPreviewVisible.value = false }, // Action to close the preview
            previewView = previewView, // Pass the CameraX PreviewView
            roiState = roiState, // Pass the ROI state
            preProcessedState = preProcessedState // Pass the processed image state
        )
    } else {
        // Display the list of scanned card numbers
        HiCardNumberListScreen(
            cardNumbers = cardNumbers, // Pass the scanned card numbers
            onBackPressed = onBackPressed // Action for back button press
        )
    }
}