/*
 * SPDX-FileCopyrightText: 2019 The Android Open Source Project
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import org.lineageos.settings.gestures.GestureUtils
import com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SUMMARY

/* Provide preference summary for injected items. */
class SummaryProvider : ContentProvider() {

    override fun call(method: String, uri: String?, extras: Bundle?): Bundle =
        Bundle().apply {
            val summary =
                when (method) {
                    KEY_GESTURE_SETTINGS -> getGestureSettingsSummary()
                    else -> throw IllegalArgumentException("Unknown method: $method")
                }
            putString(META_DATA_PREFERENCE_SUMMARY, summary)
        }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = throw UnsupportedOperationException()

    override fun getType(uri: Uri): String? = throw UnsupportedOperationException()

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException()

    private fun getGestureSettingsSummary(): String {
        val context = context ?: return ""
        val backTapSummary = context.getString(R.string.gestures_summary_back_tap)
        return buildList {
                if (GestureUtils.isBackTapAvailable(context)) {
                    add(backTapSummary)
                }
            }
            .joinToString(", ")
    }

    companion object {
        private const val KEY_GESTURE_SETTINGS = "gesture_settings"
    }
}
