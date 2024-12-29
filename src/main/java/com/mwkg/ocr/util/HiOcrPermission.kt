/**
 * File: HiOcrPermission.kt
 *
 * Description: This class handles permission requests and checks for the app.
 *              It provides functions to merge multiple permission types, request permissions,
 *              and check if the required permissions have been granted.
 *              The class supports various permission types such as camera, storage, BLE, etc.
 *
 * Author: netcanis
 * Created: 2024-12-24
 *
 * License: MIT
 */

package com.mwkg.ocr.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Handles permission requests and checks.
 * Provides utility functions to request and verify permissions for various tasks, including Bluetooth, Camera, Storage, etc.
 */
internal object HiOcrPermission {

    /**
     * Merges multiple permission types into a single list, removing duplicates.
     *
     * This function is useful when different permission types (like BLE and Beacon) require similar permissions.
     * It combines the permissions and removes duplicates before returning the final array.
     *
     * @param types The permission types (HiPermissionType) to be merged.
     * @return A deduplicated array of required permissions (Array<String>).
     *
     * Example usage:
     * ```
     * val permissions = HiPermission.getMergedPermissions(HiPermissionType.BLE, HiPermissionType.BEACON)
     * if (!HiPermission.hasPermissions(activity, permissions)) {
     *      HiPermission.requestPermissions(activity, permissions, PermissionReqCodes.BLE_BEACON)
     *      return
     * }
     * ```
     */
    fun getMergedPermissions(vararg types: HiOcrPermissionType): Array<String> {
        return types.flatMap { it.requiredPermissions().asList() }
            .distinct()
            .toTypedArray()
    }

    /**
     * Requests the specified permissions.
     *
     * @param context The context from which the request is made (should be an Activity).
     * @param permissions The list of permissions to request.
     * @param requestCode The request code for the permission request.
     */
    fun requestPermissions(context: Context, permissions: Array<String>, requestCode: Int) {
        if (context is Activity) {
            ActivityCompat.requestPermissions(context, permissions, requestCode)
        } else {
            throw IllegalArgumentException("Context must be an Activity.")
        }
    }

    /**
     * Checks whether all the specified permissions have been granted.
     *
     * @param context The context used to check permissions.
     * @param permissions The list of permissions to check.
     * @return `true` if all permissions are granted, otherwise `false`.
     */
    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}

/**
 * Enum class defining different permission types.
 * Each type returns the required permissions associated with it.
 */
internal enum class HiOcrPermissionType {
    PUSH,       // Push notifications permission
    CAMERA,     // Camera permission
    STORAGE,    // Storage permission
    PHONE,      // Phone state permission
    SMS,        // SMS permission
    LOCATION,   // Location permission
    BLE,        // Bluetooth permission
    BEACON,     // Beacon permission
    RECORD,     // Microphone permission (audio recording)
    CONTACTS;   // Contacts permission

    /**
     * Returns the required permissions for each permission type.
     *
     * @return An array of required permissions (Array<String>).
     */
    fun requiredPermissions(): Array<String> {
        return when (this) {
            PUSH -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                arrayOf()
            }
            CAMERA -> arrayOf(Manifest.permission.CAMERA)
            STORAGE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
            PHONE -> arrayOf(Manifest.permission.READ_PHONE_STATE)
            SMS -> arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS)
            LOCATION -> arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            BLE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
            BEACON -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            RECORD -> arrayOf(Manifest.permission.RECORD_AUDIO)
            CONTACTS -> arrayOf(Manifest.permission.READ_CONTACTS)
        }
    }
}

/**
 * Object containing request codes for different permission requests.
 * These codes are used when requesting permissions in the app.
 */
internal object HiOcrPermissionReqCodes {
    const val PUSH = 44440          // Push notification permission
    const val CAMERA = 44441        // Camera permission
    const val STORAGE = 44442       // Storage permission
    const val PHONE = 44443         // Phone state permission
    const val SMS = 44444           // SMS permission
    const val LOCATION = 44445      // Location permission
    const val BLE = 44446           // Bluetooth permission
    const val BEACON = 44447        // Beacon permission
    const val RECORD = 44448        // Microphone permission (audio recording)
    const val CONTACTS = 44449      // Contacts permission

    const val BLE_BEACON = 44450    // Bluetooth and Beacon permissions
    const val OCR = 44451           // Camera permission for OCR

    const val RESERVED_PERMISSION_CODE_1 = 44461 // Reserved Permission Code 1
    const val RESERVED_PERMISSION_CODE_2 = 44462 // Reserved Permission Code 2
    const val RESERVED_PERMISSION_CODE_3 = 44463 // Reserved Permission Code 3
    const val RESERVED_PERMISSION_CODE_4 = 44464 // Reserved Permission Code 4
    const val RESERVED_PERMISSION_CODE_5 = 44465 // Reserved Permission Code 5
}
