/**
 * File: HiOcrResult.kt
 *
 * Description: Data class representing the result of an OCR (Optical Character Recognition) scan.
 *              Includes card details such as card number, holder name, expiry month/year,
 *              issuing network, and error information, if any.
 *
 * Author: netcanis
 * Created: 2024-11-19
 *
 * License: MIT
 */

package com.mwkg.ocr.model

/**
 * Represents the result of an OCR scan.
 *
 * @property cardNumber The card number extracted from the OCR scan.
 * @property holderName The cardholder's name extracted from the OCR scan.
 * @property expiryMonth The expiration month of the card.
 * @property expiryYear The expiration year of the card.
 * @property issuingNetwork The network associated with the card (e.g., Visa, MasterCard, etc.).
 * @property error A string representing any error encountered during the OCR scan.
 */
data class HiOcrResult(
    val cardNumber: String,
    val holderName: String = "",
    val expiryMonth: String = "",
    val expiryYear: String = "",
    val issuingNetwork: String = "",
    val error: String = ""
)