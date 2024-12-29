/**
 * File: HiCardNumberItem.kt
 *
 * Description: A composable function that renders card details in a card layout using Jetpack Compose.
 *              Displays card information such as the card number, holder name, expiry date,
 *              issuing network, and the scanned timestamp in a readable format.
 *
 * Author: netcanis
 * Created: 2024-11-19
 *
 * License: Apache 2.0
 *
 * References:
 * - Jetpack Compose Documentation: https://developer.android.com/jetpack/compose
 * - Date Formatting in Android: https://developer.android.com/reference/java/text/SimpleDateFormat
 */

package com.mwkg.ocr.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mwkg.ocr.model.HiCardNumber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Composable function to display card details in a card layout.
 *
 * This function creates a card UI component using Jetpack Compose, which displays
 * the details of a scanned card, including the card number, holder name, expiry date,
 * issuing network, and the date and time the card was scanned.
 *
 * @param cardNumber An instance of HiCardNumber containing the details of the card.
 */
@Composable
fun HiCardNumberItem(cardNumber: HiCardNumber) {
    // Formatter to convert the scanned timestamp to a readable date format
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val scannedDate = dateFormatter.format(Date(cardNumber.scannedAt))

    // Card layout to display the card details
    Card(
        modifier = Modifier
            .fillMaxWidth() // Makes the card take up the full width of its parent
            .padding(8.dp) // Adds padding around the card
    ) {
        // Column layout to stack text details vertically
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Card Number: ${cardNumber.cardNumber}") // Displays the card number
            Text(text = "Holder Name: ${cardNumber.holderName}") // Displays the holder's name
            Text(text = "Expiry Date: ${cardNumber.expiryDate}") // Displays the card's expiry date
            Text(text = "Issuing Network: ${cardNumber.issuingNetwork}") // Displays the card's issuing network
            Text(text = "Scanned At: $scannedDate") // Displays the scanned timestamp in a readable format
        }
    }
}
