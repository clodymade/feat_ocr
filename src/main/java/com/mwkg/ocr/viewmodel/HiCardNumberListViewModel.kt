/**
 * File: HiCardNumberListViewModel.kt
 *
 * Description: ViewModel for managing a list of card numbers. This ViewModel handles updating the card list
 *              while ensuring no duplicate card numbers are added.
 *
 * Author: netcanis
 * Created: 2024-11-19
 *
 * License: Apache 2.0
 */

package com.mwkg.ocr.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.mwkg.ocr.model.HiCardNumber

/**
 * ViewModel to manage the list of scanned card numbers.
 */
class HiCardNumberListViewModel : ViewModel() {
    // Internal state holding the list of card numbers
    private val _numbers = MutableStateFlow<List<HiCardNumber>>(emptyList())

    /**
     * Public read-only state flow for observing the list of card numbers.
     */
    val numbers: StateFlow<List<HiCardNumber>> get() = _numbers

    /**
     * Updates the list of card numbers with a new or updated entry.
     *
     * @param number The HiCardNumber object containing the card details.
     */
    fun update(number: HiCardNumber) {
        val currentNumbers = _numbers.value.toMutableList()

        // Check if the card number already exists in the list
        val existingIndex = currentNumbers.indexOfFirst {
            it.cardNumber == number.cardNumber
        }

        if (existingIndex >= 0) {
            // Replace the existing card with the updated data
            currentNumbers[existingIndex] = number
        } else {
            // Add the new card to the list
            currentNumbers.add(number)
        }

        // Update the state with an immutable list
        _numbers.value = currentNumbers.toList()
    }
}