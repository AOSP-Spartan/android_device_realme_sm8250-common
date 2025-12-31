/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.realme.ui.game

import android.os.Bundle
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import org.lineageos.settings.realme.R

class GameOptimizerFragment : SettingsBasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.game_optimizer_settings, rootKey)
    }
}
