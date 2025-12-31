/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.realme.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.lineageos.settings.realme.R
import org.lineageos.settings.realme.data.repository.BypassChargingRepository

/**
 * Service to manage bypass charging state
 *
 * Improvements over original:
 * - Uses LifecycleService for coroutine support
 * - Foreground service for reliability (Android 14+)
 * - Proper receiver registration tracking
 * - Repository pattern for data access
 * - Better error handling and logging
 */
class BypassChargingService : LifecycleService() {

    private lateinit var repository: BypassChargingRepository

    private var batteryReceiverRegistered = false
    private var powerReceiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Battery state changed")
            updateBypassState()
        }
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Power state changed: ${intent?.action}")
            updateBypassState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        repository = BypassChargingRepository(applicationContext)

        // Start as foreground service for reliability
        startForeground(NOTIFICATION_ID, createNotification())

        registerReceivers()
        updateBypassState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "Service started")

        // Update notification if needed
        val notification = createNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")

        unregisterReceivers()

        // Restore normal charging when service stops
        lifecycleScope.launch {
            repository.applyBypassToHardware(false)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    /**
     * Register broadcast receivers with tracking
     */
    private fun registerReceivers() {
        if (!batteryReceiverRegistered) {
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(batteryReceiver, batteryFilter)
            batteryReceiverRegistered = true
            Log.d(TAG, "Battery receiver registered")
        }

        if (!powerReceiverRegistered) {
            val powerFilter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            registerReceiver(powerReceiver, powerFilter)
            powerReceiverRegistered = true
            Log.d(TAG, "Power receiver registered")
        }
    }

    /**
     * Unregister receivers with proper error handling
     */
    private fun unregisterReceivers() {
        if (batteryReceiverRegistered) {
            try {
                unregisterReceiver(batteryReceiver)
                batteryReceiverRegistered = false
                Log.d(TAG, "Battery receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Battery receiver was not registered")
            }
        }

        if (powerReceiverRegistered) {
            try {
                unregisterReceiver(powerReceiver)
                powerReceiverRegistered = false
                Log.d(TAG, "Power receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Power receiver was not registered")
            }
        }
    }

    /**
     * Update bypass state based on current conditions
     */
    private fun updateBypassState() {
        lifecycleScope.launch {
            try {
                val isEnabled = repository.getBypassEnabledFlow().first()
                val isCharging = repository.isCharging()

                Log.d(TAG, "Update bypass - enabled: $isEnabled, charging: $isCharging")

                if (isEnabled && isCharging) {
                    // Feature enabled and device is charging - activate bypass
                    repository.applyBypassToHardware(true)
                } else {
                    // Feature disabled or not charging - normal charging
                    repository.applyBypassToHardware(false)
                }

                // Update notification
                updateNotification()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating bypass state", e)
            }
        }
    }

    /**
     * Create notification for foreground service
     */
    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.bypass_charging_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.bypass_charging_channel_description)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(channel)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.bypass_charging_title))
            .setContentText(getString(R.string.bypass_charging_active))
            .setSmallIcon(R.drawable.ic_bypass_charging)
            .setOngoing(true)
            .build()
    }

    /**
     * Update foreground notification
     */
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "BypassChargingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "bypass_charging_service"
    }
}
