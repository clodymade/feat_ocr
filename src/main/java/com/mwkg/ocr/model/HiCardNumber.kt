/**
 * File: HiCardNumber.kt
 *
 * Description: Data class representing card information extracted through OCR.
 *              Includes card number, holder name, expiry date components, issuing network, and scan timestamp.
 *
 * Author: Your Name
 * Created: 2024-11-19
 *
 * License: Apache 2.0
 */

package com.mwkg.ocr.model

/**
 * Represents card information extracted from OCR processing.
 *
 * @property cardNumber The card number extracted from the OCR scan.
 * @property holderName The cardholder's name extracted from the OCR scan.
 * @property expiryMonth The expiration month of the card in MM format.
 * @property expiryYear The expiration year of the card in YY format.
 * @property issuingNetwork The network associated with the card (e.g., Visa, MasterCard, etc.).
 * @property scannedAt The timestamp of when the card was scanned, in Unix time.
 */
data class HiCardNumber(
    val cardNumber: String, // The number of the card
    val holderName: String, // The name of the cardholder
    val expiryMonth: String, // The expiration month of the card
    val expiryYear: String, // The expiration year of the card
    val issuingNetwork: String, // The payment network associated with the card
    val scannedAt: Long // The timestamp of the scan in Unix time
)