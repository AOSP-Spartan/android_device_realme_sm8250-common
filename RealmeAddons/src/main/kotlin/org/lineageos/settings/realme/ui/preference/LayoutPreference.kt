/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.realme.ui.preference

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

/**
 * A preference that displays a custom layout as its content.
 * Similar to SettingsLib's LayoutPreference but implemented locally.
 */
class LayoutPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    private var customView: View? = null

    init {
        // Get the layout resource from xml attributes
        val a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.layout))
        val layoutResId = a.getResourceId(0, 0)
        a.recycle()

        if (layoutResId != 0) {
            customView = LayoutInflater.from(context).inflate(layoutResId, null)
            // Remove the widget frame to use full width
            widgetLayoutResource = 0
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        customView?.let { view ->
            // Remove from old parent if exists
            (view.parent as? FrameLayout)?.removeAllViews()

            // Add to the preference view
            val container = holder.itemView as? FrameLayout
            container?.apply {
                removeAllViews()
                addView(view)
            }
        }
    }
}
