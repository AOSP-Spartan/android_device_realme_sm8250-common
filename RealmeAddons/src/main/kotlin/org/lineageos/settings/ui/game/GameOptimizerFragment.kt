/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui.game

import android.os.Bundle
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import org.lineageos.settings.R

/**
 * Fragment for game optimizer settings
 * Empty placeholder - features will be added later
 */
class GameOptimizerFragment : SettingsBasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.game_optimizer_settings, rootKey)

        // Game optimizer features will be added here
    }
}
