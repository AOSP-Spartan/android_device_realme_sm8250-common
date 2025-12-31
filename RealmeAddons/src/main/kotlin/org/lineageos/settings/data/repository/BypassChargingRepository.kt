/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.lineageos.settings.data.datastore.BypassChargingDataStore
import org.lineageos.settings.util.FileUtils

/**
 * Repository for bypass charging
 * Single source of truth - handles all data operations
 *
 * Benefits:
 * - Abstracts data sources (DataStore, file system, battery manager)
 * - Provides clean API for ViewModels
 * - Handles threading (IO operations on IO dispatcher)
 * - Easy to test by mocking
 */
class BypassChargingRepository(private val context: Context) {

    private val dataStore = BypassChargingDataStore(context)

    companion object {
        private const val BYPASS_CHARGING_NODE =
            "/sys/devices/virtual/oplus_chg/battery/mmi_charging_enable"

        // Hardware logic is inverted: 0 = bypass enabled, 1 = normal charging
        private const val HARDWARE_BYPASS_ENABLE = "0"
        private const val HARDWARE_BYPASS_DISABLE = "1"
    }

    /**
     * Check if bypass charging is supported by hardware
     */
    fun isSupported(): Boolean {
        return FileUtils.isFileReadable(BYPASS_CHARGING_NODE) &&
               FileUtils.isFileWritable(BYPASS_CHARGING_NODE)
    }

    /**
     * Get bypass enabled state as Flow for reactive UI
     */
    fun getBypassEnabledFlow(): Flow<Boolean> {
        return dataStore.bypassEnabledFlow
    }

    /**
     * Get current bypass enabled state from DataStore
     */
    suspend fun getBypassEnabled(): Boolean {
        return dataStore.getBypassEnabled()
    }

    /**
     * Save bypass enabled state to DataStore
     */
    suspend fun saveBypassEnabled(enabled: Boolean) {
        dataStore.setBypassEnabled(enabled)
    }

    /**
     * Apply bypass charging to hardware
     * This actually controls the charging circuit
     *
     * @return true if successful, false otherwise
     */
    suspend fun applyBypassToHardware(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!isSupported()) {
            return@withContext false
        }

        val value = if (enabled) HARDWARE_BYPASS_ENABLE else HARDWARE_BYPASS_DISABLE
        return@withContext FileUtils.writeLine(BYPASS_CHARGING_NODE, value)
    }

    /**
     * Check if device is currently charging
     */
    fun isCharging(): Boolean {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)

        return batteryStatus?.let { intent ->
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        } ?: false
    }

    /**
     * Get current battery level (0-100)
     */
    fun getBatteryLevel(): Int {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)

        return batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                (level * 100) / scale
            } else {
                0
            }
        } ?: 0
    }

    /**
     * Check if device is plugged in
     */
    fun isPlugged(): Boolean {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)

        return batteryStatus?.let { intent ->
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            plugged == BatteryManager.BATTERY_PLUGGED_AC ||
            plugged == BatteryManager.BATTERY_PLUGGED_USB ||
            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
        } ?: false
    }
}
