/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.realme

import android.app.Application

/**
 * Application class for RealmeAddons
 * Initializes app-wide components
 */
class RealmeAddonsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize app-wide components here
        // e.g., DataStore, repositories, etc.
    }
}
