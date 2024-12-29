/**
 * File: HiOcrPreferences.kt
 *
 * Description: This utility class manages the SharedPreferences for storing and retrieving data
 *              in a persistent storage across app sessions. It provides methods to save and load
 *              values using keys. The class uses a predefined `HiPreferences` file name for all operations.
 *
 * Author: netcanis
 * Created: 2024-12-24
 *
 * License: MIT
 */

package com.mwkg.ocr.util

import android.content.Context

/**
 * Utility class for handling SharedPreferences operations, including saving and loading values.
 * It uses a predefined `HiPreferences` file name for storing the data.
 */
internal object HiOcrPreferences {

    // The name of the SharedPreferences file used for storing data
    private const val PREFERENCES_NAME = "HiPreferences"

    /**
     * Loads a value from SharedPreferences.
     *
     * @param context The context used to access the SharedPreferences.
     * @param key The key associated with the value to retrieve.
     * @param defaultValue The default value to return if the key does not exist (default is an empty string).
     * @return The stored value if it exists, or the default value if not.
     *
     * Example usage:
     * ```
     * val username = HiPreferences.load(context, "user_name", "defaultUser")
     * ```
     */
    fun load(context: Context, key: String, defaultValue: String = ""): String {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * Saves a value to SharedPreferences.
     *
     * @param context The context used to access the SharedPreferences.
     * @param key The key to associate with the value.
     * @param value The value to store in SharedPreferences.
     *
     * Example usage:
     * ```
     * HiPreferences.save(context, "user_name", "JohnDoe")
     * ```
     */
    fun save(context: Context, key: String, value: String) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(key, value)
            apply() // Asynchronous saving
        }
    }
}