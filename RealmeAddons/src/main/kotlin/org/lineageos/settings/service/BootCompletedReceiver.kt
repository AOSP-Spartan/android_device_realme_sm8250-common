/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.realme.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.lineageos.settings.realme.data.repository.BypassChargingRepository

/**
 * Broadcast receiver for boot completed events
 *
 * Improvements:
 * - Coroutine scope for async operations
 * - Repository pattern
 * - Support for both BOOT_COMPLETED and LOCKED_BOOT_COMPLETED
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.w(TAG, "Received null context or intent")
            return
        }

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.d(TAG, "Boot completed: ${intent.action}")
                handleBootCompleted(context)
            }
        }
    }

    /**
     * Handle boot completed event
     * Restore bypass charging state if it was enabled
     */
    private fun handleBootCompleted(context: Context) {
        // Use goAsync() to allow async work in BroadcastReceiver
        val pendingResult = goAsync()

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val repository = BypassChargingRepository(context)

                // Check if feature is supported
                if (!repository.isSupported()) {
                    Log.d(TAG, "Bypass charging not supported")
                    pendingResult.finish()
                    return@launch
                }

                // Get saved state
                val enabled = repository.getBypassEnabled()
                Log.d(TAG, "Restoring bypass charging state: $enabled")

                // Start service if enabled
                if (enabled) {
                    val serviceIntent = Intent(context, BypassChargingService::class.java)
                    context.startService(serviceIntent)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error handling boot completed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
