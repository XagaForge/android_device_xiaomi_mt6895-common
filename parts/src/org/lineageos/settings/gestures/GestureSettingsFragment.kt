/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.gestures

import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragment
import org.lineageos.settings.R
import org.lineageos.settings.utils.dlog

/** Double tap side-fingerprint sensor, settings fragment. */
class GestureSettingsFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener {

    private val backDoubleTapPref by lazy { findPreference<ListPreference>(KEY_BACK_DOUBLE_TAP)!! }
    private val backTripleTapPref by lazy { findPreference<ListPreference>(KEY_BACK_TRIPLE_TAP)!! }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.gesture_settings)

        val isBackTapAvailable = GestureUtils.isBackTapAvailable(context)
        val backTapSummary =
            if (isBackTapAvailable) "%s" else context.getString(R.string.gesture_unavailable)

        backDoubleTapPref.apply {
            isEnabled = isBackTapAvailable
            summary = backTapSummary
        }

        backTripleTapPref.apply {
            isEnabled = isBackTapAvailable
            summary = backTapSummary
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean =
    when (preference.key) {
        KEY_BACK_DOUBLE_TAP,
        KEY_BACK_TRIPLE_TAP -> {
            // BackTapService reads shared prefs directly so we don't need to do anything here
            true
        }
        else -> false
    }

    companion object {
        private const val TAG = "GestureSettingsFragment"
        private const val KEY_BACK_DOUBLE_TAP = "back_double_tap"
        private const val KEY_BACK_TRIPLE_TAP = "back_triple_tap"
    }
}
