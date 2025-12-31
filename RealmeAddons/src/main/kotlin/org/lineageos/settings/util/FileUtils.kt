/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.util

import android.util.Log
import java.io.File
import java.io.IOException

/**
 * File utilities for sysfs operations
 *
 * Improvements over original:
 * - Kotlin idioms (object, use blocks, etc.)
 * - Extension functions
 * - Better error handling with Result type
 * - No resource leaks
 */
object FileUtils {

    private const val TAG = "FileUtils"

    /**
     * Read the first line from a file
     *
     * @param filePath Path to the file
     * @return The first line, or null if file doesn't exist or can't be read
     */
    fun readOneLine(filePath: String): String? {
        return try {
            File(filePath).takeIf { it.exists() && it.canRead() }
                ?.bufferedReader()
                ?.use { it.readLine() }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read from $filePath", e)
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception reading $filePath", e)
            null
        }
    }

    /**
     * Write a line to a file
     *
     * @param filePath Path to the file
     * @param value The value to write
     * @return true if successful, false otherwise
     */
    fun writeLine(filePath: String, value: String): Boolean {
        return try {
            File(filePath).takeIf { it.exists() && it.canWrite() }
                ?.bufferedWriter()
                ?.use {
                    it.write(value)
                    it.flush()
                    true
                } ?: run {
                Log.w(TAG, "File $filePath does not exist or is not writable")
                false
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write to $filePath", e)
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception writing to $filePath", e)
            false
        }
    }

    /**
     * Check if file exists
     */
    fun fileExists(filePath: String): Boolean {
        return try {
            File(filePath).exists()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception checking existence of $filePath", e)
            false
        }
    }

    /**
     * Check if file is readable
     */
    fun isFileReadable(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.exists() && file.canRead()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception checking readability of $filePath", e)
            false
        }
    }

    /**
     * Check if file is writable
     */
    fun isFileWritable(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.exists() && file.canWrite()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception checking writability of $filePath", e)
            false
        }
    }
}
