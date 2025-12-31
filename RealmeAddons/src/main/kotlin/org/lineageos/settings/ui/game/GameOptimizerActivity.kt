/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui.game

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity

/**
 * Activity for game optimizer settings
 */
class GameOptimizerActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load fragment
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                    GameOptimizerFragment()
                )
                .commit()
        }
    }
}
