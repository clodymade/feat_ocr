/**
 * File: HiCardNumberListScreen.kt
 *
 * Description: Composable function to display a list of scanned card numbers in a lazy column
 *              layout with a top app bar for navigation.
 *
 * Author: netcanis
 * Created: 2024-11-19
 *
 * License: Apache 2.0
 *
 * References:
 * - Jetpack Compose LazyColumn: https://developer.android.com/jetpack/compose/lists
 * - Material3 TopAppBar: https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary
 */

package com.mwkg.ocr.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mwkg.ocr.model.HiCardNumber
import kotlinx.coroutines.flow.StateFlow

/**
 * Screen to display a list of scanned card numbers.
 *
 * @param cardNumbers StateFlow providing the list of card numbers.
 * @param onBackPressed Lambda invoked when the back button in the top app bar is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class) // Opt-in for experimental Material3 APIs
@Composable
fun HiCardNumberListScreen(
    cardNumbers: StateFlow<List<HiCardNumber>>, // StateFlow containing the list of scanned card numbers
    onBackPressed: () -> Unit // Action for back button press
) {
    // Collect the current list of card numbers as a state
    val cardNumberList = cardNumbers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Number List") }, // Title for the top app bar
                navigationIcon = {
                    IconButton(onClick = onBackPressed) { // Back button action
                        Icon(Icons.Default.Close, contentDescription = "Back") // Close icon
                    }
                }
            )
        }
    ) { paddingValues ->
        // Display the list of card numbers in a LazyColumn
        LazyColumn(
            contentPadding = PaddingValues(16.dp), // Padding around the list content
            modifier = Modifier.padding(paddingValues) // Adjust for scaffold padding
        ) {
            // Render each card number item
            items(cardNumberList.value) { cardNumber ->
                HiCardNumberItem(cardNumber) // Display individual card number item
            }
        }
    }
}