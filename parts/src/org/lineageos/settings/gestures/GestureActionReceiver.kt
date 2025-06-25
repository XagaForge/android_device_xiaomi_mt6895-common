/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.gestures

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class GestureActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != GestureUtils.ACTION_GESTURE_TRIGGER) return
        val action = intent.getIntExtra(GestureUtils.EXTRA_GESTURE_ACTION_ID, -1)
        Log.d(TAG, "Received gesture action: $action")
        GestureActionHandler.getInstance(context).performAction(action)
    }

    companion object {
        private const val TAG = "GestureActionReceiver"
    }
}
