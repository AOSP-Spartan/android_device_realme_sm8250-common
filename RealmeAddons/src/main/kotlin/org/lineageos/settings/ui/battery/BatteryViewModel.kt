/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.realme.ui.battery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.lineageos.settings.realme.data.repository.BypassChargingRepository

/**
 * ViewModel for battery settings screen
 *
 * Benefits over putting logic in Fragment:
 * - Survives configuration changes (rotation)
 * - Separates business logic from UI
 * - Manages coroutines lifecycle
 * - Testable (can mock repository)
 */
class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BypassChargingRepository(application)

    // Feature support state
    private val _featureSupported = MutableLiveData<Boolean>()
    val featureSupported: LiveData<Boolean> = _featureSupported

    // Bypass enabled state from DataStore (reactive)
    val bypassEnabled: LiveData<Boolean> = repository
        .getBypassEnabledFlow()
        .asLiveData(viewModelScope.coroutineContext)

    // Bypass threshold from DataStore (reactive)
    val bypassThreshold: LiveData<Int> = repository
        .getBypassThresholdFlow()
        .asLiveData(viewModelScope.coroutineContext)

    // Charging state
    private val _isCharging = MutableLiveData<Boolean>()
    val isCharging: LiveData<Boolean> = _isCharging

    // UI state for error handling
    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    init {
        checkFeatureSupport()
        updateBatteryState()
    }

    /**
     * Check if bypass charging is supported
     */
    private fun checkFeatureSupport() {
        val supported = repository.isSupported()
        _featureSupported.value = supported
    }

    /**
     * Update battery and charging state
     * Called from UI when battery broadcast is received
     */
    fun updateBatteryState() {
        _isCharging.value = repository.isCharging()
    }

    /**
     * Toggle bypass charging
     * Called from UI (switch toggle)
     */
    fun setBypassEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                // Save to DataStore
                repository.saveBypassEnabled(enabled)

                // Apply to hardware if charging
                if (repository.isCharging()) {
                    val success = repository.applyBypassToHardware(enabled)
                    if (!success) {
                        _uiState.value = UiState.Error("Failed to apply bypass to hardware")
                        return@launch
                    }
                }

                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Set bypass charging threshold
     * Called from UI (seekbar/slider)
     */
    fun setBypassThreshold(threshold: Int) {
        viewModelScope.launch {
            try {
                repository.saveBypassThreshold(threshold)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to save threshold")
            }
        }
    }

    /**
     * UI State for handling loading, success, and error states
     */
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
