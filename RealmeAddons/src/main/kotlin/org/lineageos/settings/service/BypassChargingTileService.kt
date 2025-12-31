/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.realme.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.lineageos.settings.realme.R
import org.lineageos.settings.realme.data.repository.BypassChargingRepository

/**
 * Quick Settings Tile for bypass charging
 *
 * Improvements:
 * - Coroutine scope for async operations
 * - Repository pattern for data access
 * - Proper lifecycle management
 * - Better state handling
 */
class BypassChargingTileService : TileService() {

    private lateinit var repository: BypassChargingRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "BypassChargingTile"
        const val ACTION_UPDATE_TILE = "org.lineageos.settings.UPDATE_BYPASS_TILE"
    }

    override fun onCreate() {
        super.onCreate()
        repository = BypassChargingRepository(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "Tile started listening")
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        Log.d(TAG, "Tile clicked")

        serviceScope.launch {
            try {
                // Check if feature is supported
                if (!repository.isSupported()) {
                    Log.w(TAG, "Bypass charging not supported")
                    return@launch
                }

                // Check if charging
                if (!repository.isCharging()) {
                    Log.d(TAG, "Device not charging")
                    return@launch
                }

                // Toggle state
                val currentState = repository.getBypassEnabled()
                val newState = !currentState

                Log.d(TAG, "Toggling bypass from $currentState to $newState")

                // Save to DataStore
                repository.saveBypassEnabled(newState)

                // Apply to hardware
                repository.applyBypassToHardware(newState)

                // Start/stop service
                val serviceIntent = Intent(applicationContext, BypassChargingService::class.java)
                if (newState) {
                    startService(serviceIntent)
                } else {
                    stopService(serviceIntent)
                }

                // Update tile
                updateTileState()

            } catch (e: Exception) {
                Log.e(TAG, "Error toggling bypass charging", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_TILE) {
            Log.d(TAG, "Received tile update request")
            updateTileState()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * Update tile state based on current conditions
     */
    private fun updateTileState() {
        serviceScope.launch {
            try {
                val tile = qsTile ?: run {
                    Log.w(TAG, "Tile is null")
                    return@launch
                }

                val isCharging = repository.isCharging()
                val batteryLevel = repository.getBatteryLevel()

                // If not charging, show as unavailable
                if (!isCharging) {
                    tile.state = Tile.STATE_UNAVAILABLE
                    tile.subtitle = getString(R.string.bypass_charging_not_charging)
                    tile.updateTile()
                    return@launch
                }

                // Get enabled state
                val enabled = repository.getBypassEnabled()

                // Update tile based on state
                if (enabled) {
                    tile.state = Tile.STATE_ACTIVE
                    tile.subtitle = getString(R.string.bypass_charging_active_with_level, batteryLevel)
                } else {
                    tile.state = Tile.STATE_INACTIVE
                    tile.subtitle = getString(R.string.bypass_charging_inactive_with_level, batteryLevel)
                }

                tile.updateTile()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating tile state", e)
            }
        }
    }
}
