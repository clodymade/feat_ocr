/**
 * File: HiCardNumber.kt
 * Description: A data class representing card information extracted through OCR (Optical Character Recognition).
 *              This includes details such as card number, holder's name, expiry date, issuing network, and scan timestamp.
 *
 * Author: Your Name
 * Created: 2024-11-19
 *
 * License: Apache 2.0
 *
 * References:
 * - Android Developer Documentation: https://developer.android.com/
 * - ML Kit OCR Documentation: https://developers.google.com/ml-kit/vision/text-recognition
 */

package com.mwkg.ocr.model

/**
 * Represents card information extracted through OCR processing.
 *
 * This data class encapsulates the details of a card obtained during the OCR scan, such as its
 * number, holder's name, expiry date, payment network, and the scan timestamp.
 *
 * @property cardNumber The number of the card.
 * @property holderName The name of the cardholder.
 * @property expiryDate The expiration date of the card in MM/YY format.
 * @property issuingNetwork The network associated with the card (e.g., Visa, MasterCard, etc.).
 * @property scannedAt The time when the card was scanned, represented as a Unix timestamp.
 */
data class HiCardNumber(
    val cardNumber: String, // The number of the card
    val holderName: String, // The name of the cardholder
    val expiryDate: String, // The expiration date in MM/YY
    val issuingNetwork: String, // The payment network associated with the card
    val scannedAt: Long // The timestamp of the scan in Unix time
)