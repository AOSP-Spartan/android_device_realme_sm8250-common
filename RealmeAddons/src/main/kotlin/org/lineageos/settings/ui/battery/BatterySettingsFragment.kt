/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import org.lineageos.settings.R
import org.lineageos.settings.service.BypassChargingService

/**
 * Fragment for battery settings
 * Uses modern MVVM pattern with ViewModel
 *
 * Improvements over original:
 * - Null-safe getContext() calls
 * - ViewModel survives rotation
 * - Proper receiver registration tracking
 * - Better error handling
 */
class BatterySettingsFragment : SettingsBasePreferenceFragment() {

    private val viewModel: BatteryViewModel by viewModels()

    private var bypassChargingPreference: SwitchPreferenceCompat? = null
    private var receiverRegistered = false

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Update battery state in ViewModel
            viewModel.updateBatteryState()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.battery_settings, rootKey)

        setupBypassChargingPreference()
        observeViewModel()
    }

    /**
     * Setup bypass charging preference
     */
    private fun setupBypassChargingPreference() {
        bypassChargingPreference = findPreference<SwitchPreferenceCompat>("bypass_charging")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                viewModel.setBypassEnabled(enabled)

                // Start/stop service based on state
                context?.let { ctx ->
                    val serviceIntent = Intent(ctx, BypassChargingService::class.java)
                    if (enabled) {
                        ctx.startService(serviceIntent)
                    } else {
                        ctx.stopService(serviceIntent)
                    }
                }
                true
            }
        }
    }

    /**
     * Observe ViewModel LiveData
     * This is where UI updates happen based on data changes
     */
    private fun observeViewModel() {
        // Observe feature support
        viewModel.featureSupported.observe(this) { supported ->
            bypassChargingPreference?.apply {
                if (!supported) {
                    isEnabled = false
                    summary = getString(R.string.bypass_charging_not_supported)
                }
            }
        }

        // Observe bypass enabled state
        viewModel.bypassEnabled.observe(this) { enabled ->
            bypassChargingPreference?.isChecked = enabled
        }

        // Observe charging state
        viewModel.isCharging.observe(this) { charging ->
            updatePreferenceState(charging)
        }

        // Observe UI state for errors
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is BatteryViewModel.UiState.Error -> {
                    context?.let {
                        Toast.makeText(it, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
                else -> { /* Handle other states if needed */ }
            }
        }
    }

    /**
     * Update preference enabled state based on charging
     */
    private fun updatePreferenceState(charging: Boolean) {
        bypassChargingPreference?.apply {
            val supported = viewModel.featureSupported.value ?: false
            isEnabled = supported && charging

            summary = when {
                !supported -> getString(R.string.bypass_charging_not_supported)
                !charging -> getString(R.string.bypass_charging_not_charging)
                else -> getString(R.string.bypass_charging_summary)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerPowerReceiver()
        viewModel.updateBatteryState()
    }

    override fun onPause() {
        super.onPause()
        unregisterPowerReceiver()
    }

    /**
     * Register broadcast receiver with null safety
     */
    private fun registerPowerReceiver() {
        context?.let { ctx ->
            if (!receiverRegistered) {
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_POWER_CONNECTED)
                    addAction(Intent.ACTION_POWER_DISCONNECTED)
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                }
                ctx.registerReceiver(powerReceiver, filter)
                receiverRegistered = true
            }
        }
    }

    /**
     * Unregister broadcast receiver with proper error handling
     */
    private fun unregisterPowerReceiver() {
        if (receiverRegistered) {
            try {
                context?.unregisterReceiver(powerReceiver)
                receiverRegistered = false
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered, ignore
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up references to avoid memory leaks
        bypassChargingPreference = null
    }
}
