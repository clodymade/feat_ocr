/**
 * File: HiCardNumberPreviewScreen.kt
 *
 * Description: Composable function for displaying the live camera preview with a drawn ROI (Region of Interest)
 *              and additional UI controls for toggling the flashlight and closing the preview.
 *
 * Author: netcanis
 * Created: 2024-11-19
 *
 * License: Apache 2.0
 *
 * References:
 * - Jetpack Compose Canvas API: https://developer.android.com/jetpack/compose/canvas
 * - AndroidView integration: https://developer.android.com/jetpack/compose/integrate
 */

package com.mwkg.ocr.view

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mwkg.feat_ocr.R


/**
 * Displays the camera preview along with UI elements for controlling the flashlight,
 * closing the preview, and showing the Region of Interest (ROI).
 *
 * @param onToggleTorch Callback to toggle the flashlight state.
 * @param onClosePreview Callback to close the preview screen.
 * @param previewView The camera preview view.
 * @param roiState Mutable state for the ROI rectangle.
 * @param preProcessedState State containing the processed image for debugging.
 */
@Composable
fun HiCardNumberPreviewScreen(
    onToggleTorch: (Boolean) -> Unit,
    onClosePreview: () -> Unit,
    previewView: PreviewView,
    roiState: MutableState<Rect>,
    preProcessedState: State<Bitmap?>
) {
    // Remember flashlight toggle state
    var isTorchOn by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Render the PreviewView using AndroidView
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Draw overlay with ROI and dimming effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            val roiRect = drawCardRoiOverlay()
            roiState.value = roiRect // Update ROI state
        }

        // Display processed image for debugging
        preProcessedState.value?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Processed Bitmap",
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopCenter)
                    .size(340.dp)
            )
        }

        // Action buttons for flashlight toggle and closing preview
        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flashlight toggle button
            IconButton(onClick = {
                isTorchOn = !isTorchOn
                onToggleTorch(isTorchOn)
            }) {
                Icon(
                    painter = painterResource(
                        id = if (isTorchOn) R.drawable.ic_flashlight_on else R.drawable.ic_flashlight_off
                    ),
                    contentDescription = if (isTorchOn) "Flashlight On" else "Flashlight Off",
                    tint = Color.Unspecified // Retain original icon color
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Close preview button
            IconButton(onClick = onClosePreview) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Close Preview",
                    tint = Color.Unspecified // Retain original icon color
                )
            }
        }
    }
}

/**
 * Draws the Region of Interest (ROI) overlay on the canvas.
 *
 * @return The rectangle defining the ROI.
 */
fun DrawScope.drawCardRoiOverlay(): Rect {
    // Overlay color with dimming effect
    val overlayColor = Color.Black.copy(alpha = 0.6f)
    drawRect(color = overlayColor)

    // Calculate ROI dimensions
    val roiMargin = 10.dp.toPx()
    val roiWidth = size.width.toFloat() - (roiMargin * 2.0f)
    val roiHeight = roiWidth / 1.586f
    val roiLeft = roiMargin
    val roiTop = (size.height.toFloat() - roiHeight) / 2.0f
    val roiRight = roiLeft + roiWidth
    val roiBottom = roiTop + roiHeight

    // Define the ROI rectangle
    val roiRect = Rect(roiLeft.toInt(), roiTop.toInt(), roiRight.toInt(), roiBottom.toInt())

    // Clear the ROI area
    drawRect(
        color = Color.Transparent,
        topLeft = Offset(roiLeft, roiTop),
        size = Size(roiWidth, roiHeight),
        blendMode = BlendMode.Clear
    )

    // Draw corner lines around the ROI
    val cornerLength = 30.dp.toPx()
    val cornerStroke = 4.dp.toPx()
    val cornerColor = Color.Red

    // Top-left corner
    drawLine(color = cornerColor, start = Offset(roiLeft, roiTop), end = Offset(roiLeft + cornerLength, roiTop), strokeWidth = cornerStroke)
    drawLine(color = cornerColor, start = Offset(roiLeft, roiTop), end = Offset(roiLeft, roiTop + cornerLength), strokeWidth = cornerStroke)

    // Top-right corner
    drawLine(color = cornerColor, start = Offset(roiRight, roiTop), end = Offset(roiRight - cornerLength, roiTop), strokeWidth = cornerStroke)
    drawLine(color = cornerColor, start = Offset(roiRight, roiTop), end = Offset(roiRight, roiTop + cornerLength), strokeWidth = cornerStroke)

    // Bottom-left corner
    drawLine(color = cornerColor, start = Offset(roiLeft, roiBottom), end = Offset(roiLeft + cornerLength, roiBottom), strokeWidth = cornerStroke)
    drawLine(color = cornerColor, start = Offset(roiLeft, roiBottom), end = Offset(roiLeft, roiBottom - cornerLength), strokeWidth = cornerStroke)

    // Bottom-right corner
    drawLine(color = cornerColor, start = Offset(roiRight, roiBottom), end = Offset(roiRight - cornerLength, roiBottom), strokeWidth = cornerStroke)
    drawLine(color = cornerColor, start = Offset(roiRight, roiBottom), end = Offset(roiRight, roiBottom - cornerLength), strokeWidth = cornerStroke)

    return roiRect
}