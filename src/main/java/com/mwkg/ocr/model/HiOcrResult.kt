/**
 * File: HiOcrResult.kt
 *
 * Description: This sealed class defines various scan result types, including QR codes, NFC tags, BLE devices, Beacons, and OCR data.
 *              Each result class contains relevant details such as scanned data, error messages, and specific attributes.
 *
 * Author: netcanis
 * Created: 2024-11-19
 *
 * License: MIT
 *
 * References:
 * - Android BLE Overview: https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview
 */

package com.mwkg.ocr.model

/**
 * Represents the result of an OCR (Optical Character Recognition) scan.
 *
 * @property cardNumber The card number extracted from the OCR scan.
 * @property holderName The holder name extracted from the OCR scan.
 * @property expiryDate The expiry date extracted from the OCR scan.
 * @property issuingNetwork The issuing network of the card.
 * @property error The error message that occurred during scanning.
 */
data class HiOcrResult(
    val cardNumber: String,
    val holderName: String = "",
    val expiryDate: String = "",
    val issuingNetwork: String = "",
    val error: String = ""
)