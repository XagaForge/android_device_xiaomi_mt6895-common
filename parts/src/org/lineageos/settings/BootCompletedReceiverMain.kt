/*
 * SPDX-FileCopyrightText: 2018 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.lineageos.settings.thermal.ThermalUtils
import org.lineageos.settings.gestures.GestureUtils

/** Everything begins at boot. */
class BootCompletedReceiverMain : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")
        if (intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        Log.i(TAG, "Boot completed, starting services")
        ThermalUtils.getInstance(context).startService()
        GestureUtils.onBootCompleted(context)
    }

    companion object {
        private const val TAG = "XiaomiParts-BCR"
    }
}
