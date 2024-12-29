/**
 * File: HiOcrToolkit.kt
 *
 * Description: This utility module provides a set of extension functions for `Context` and other common utilities.
 *              It includes functions for loading and saving data using SharedPreferences, managing permissions,
 *              sanitizing strings, converting JSON to maps or lists, and more.
 *              These utilities aim to simplify common operations and improve code readability.
 *
 * Author: netcanis
 * Created: 2024-12-24
 *
 * License: MIT
 */

package com.mwkg.ocr.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.Charset

/**
 * Utility class for context-related extensions and common operations.
 * Internal to the feat_ble module.
 */
internal object HiOcrToolkit {

    /**
     * Loads a value from SharedPreferences.
     *
     * @param key The key to identify the stored value.
     * @param defaultValue The value to return if the key does not exist (default is an empty string).
     * @return The stored value if it exists, or the default value if not.
     *
     * Example usage:
     * ```
     * val username = context.load("user_name", "defaultUser")
     * ```
     */
    internal fun Context.load(key: String, defaultValue: String = ""): String {
        return HiOcrPreferences.load(this, key, defaultValue)
    }

    /**
     * Saves a value to SharedPreferences.
     *
     * @param key The key to associate with the value.
     * @param value The value to be stored.
     *
     * Example usage:
     * ```
     * context.save("user_name", "JohnDoe")
     * ```
     */
    internal fun Context.save(key: String, value: String) {
        HiOcrPreferences.save(this, key, value)
    }

    /**
     * Requests the specified permissions.
     *
     * @param permissions An array of permissions to request.
     * @param requestCode The request code for the permission request.
     *
     * Example usage:
     * ```
     * context.requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
     * ```
     */
    internal fun Context.requestPermissions(permissions: Array<String>, requestCode: Int) {
        HiOcrPermission.requestPermissions(this, permissions, requestCode)
    }

    /**
     * Checks whether all specified permissions are granted.
     *
     * @param permissions An array of permissions to check.
     * @return `true` if all permissions are granted, otherwise `false`.
     *
     * Example usage:
     * ```
     * val allGranted = context.hasPermissions(arrayOf(Manifest.permission.CAMERA))
     * ```
     */
    internal fun Context.hasPermissions(permissions: Array<String>): Boolean {
        return HiOcrPermission.hasPermissions(this, permissions)
    }

    /**
     * Removes control characters from a string.
     *
     * @return A sanitized string with control characters removed.
     *
     * Example usage:
     * ```
     * val sanitized = "Hello\u0001World".sanitize()
     * ```
     */
    internal fun String.sanitize(): String {
        return this.replace(Regex("\\p{Cntrl}"), "")
    }

    /**
     * Converts a ByteArray to a string and removes control characters.
     *
     * @param charset The charset to use for conversion (default is UTF-8).
     * @return A sanitized string with control characters removed.
     *
     * Example usage:
     * ```
     * val sanitizedString = byteArray.sanitizeToString(Charset.forName("UTF-8"))
     * ```
     */
    internal fun ByteArray.sanitizeToString(charset: Charset = Charset.forName("UTF-8")): String {
        return String(this, charset).sanitize()
    }

    /**
     * Converts an object to a pretty-formatted JSON string.
     *
     * @return A JSON string formatted with indentation for better readability.
     *
     * Example usage:
     * ```
     * val prettyJson = jsonObject.toPrettyJsonString()
     * ```
     */
    internal fun Any.toPrettyJsonString(): String {
        return when (this) {
            is JSONObject -> this.toString(4)
            is JSONArray -> this.toString(4)
            else -> this.toString()
        }
    }

    /**
     * Converts an object to a Map or List representation.
     *
     * @return A `Map<String, Any>` or `List<Any>` depending on the input type, or the original value if not convertible.
     *
     * Example usage:
     * ```
     * val mapOrList = jsonString.toMapOrList()
     * ```
     */
    internal fun Any.toMapOrList(): Any {
        return when (this) {
            is JSONObject -> {
                val map = mutableMapOf<String, Any>()
                this.keys().forEach { key ->
                    val value = this[key]
                    map[key] = value.toMapOrList()
                }
                map
            }
            is JSONArray -> {
                (0 until this.length()).map { index ->
                    this[index].toMapOrList()
                }
            }
            is String -> {
                try {
                    when {
                        this.trim().startsWith("{") -> JSONObject(this).toMapOrList()
                        this.trim().startsWith("[") -> JSONArray(this).toMapOrList()
                        else -> this
                    }
                } catch (e: Exception) {
                    this
                }
            }
            else -> this
        }
    }

    /**
     * Converts a JSONObject to a Map<String, Any>.
     *
     * @return A map representation of the JSON object.
     *
     * Example usage:
     * ```
     * val map = jsonObject.toMap()
     * ```
     */
    internal fun JSONObject.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        this.keys().forEach { key ->
            val value = this[key]
            map[key] = when (value) {
                is JSONObject -> value.toMap()
                is JSONArray -> value.toList()
                else -> value
            }
        }
        return map
    }

    /**
     * Converts a JSONArray to a List<Any>.
     *
     * @return A list representation of the JSON array.
     *
     * Example usage:
     * ```
     * val list = jsonArray.toList()
     * ```
     */
    internal fun JSONArray.toList(): List<Any> {
        return (0 until this.length()).map { index ->
            val value = this[index]
            when (value) {
                is JSONObject -> value.toMap()
                is JSONArray -> value.toList()
                else -> value
            }
        }
    }
}