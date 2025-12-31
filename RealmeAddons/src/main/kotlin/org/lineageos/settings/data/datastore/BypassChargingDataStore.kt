/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore for bypass charging preferences
 * Modern replacement for SharedPreferences
 */
class BypassChargingDataStore(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "bypass_charging_prefs"
        )

        private val BYPASS_ENABLED_KEY = booleanPreferencesKey("bypass_charging_enabled")
    }

    /**
     * Flow of bypass charging enabled state
     * UI can observe this for reactive updates
     */
    val bypassEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[BYPASS_ENABLED_KEY] ?: false
        }

    /**
     * Save bypass charging enabled state
     */
    suspend fun setBypassEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BYPASS_ENABLED_KEY] = enabled
        }
    }

    /**
     * Get current bypass enabled state (non-Flow)
     * Use this for one-time reads, prefer Flow for observation
     */
    suspend fun getBypassEnabled(): Boolean {
        return context.dataStore.data.first()[BYPASS_ENABLED_KEY] ?: false
    }
}
