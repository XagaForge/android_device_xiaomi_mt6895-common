/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.gestures

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.UserHandle
import android.util.Log
import org.lineageos.settings.utils.dlog

object GestureUtils {
    private const val TAG = "GestureUtils"

    const val ACTION_GESTURE_TRIGGER = "org.lineageos.settings.ACTION_GESTURE_TRIGGER"
    const val EXTRA_GESTURE_ACTION_ID = "action_id"

    const val PREF_KEY_BACK_DOUBLE_TAP = "back_double_tap"
    const val PREF_KEY_BACK_TRIPLE_TAP = "back_triple_tap"
    private const val SENSOR_TYPE_BACK_TAP = 33171045 // xiaomi.sensor.dbtap

    fun onBootCompleted(context: Context) {
        val backTapAvailable = isBackTapAvailable(context)
        dlog(TAG, "onBootCompleted: backTapAvailable=$backTapAvailable")

        if (!backTapAvailable) {
            dlog(TAG, "Disabling gesture settings")
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, GestureSettingsActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } else {
            BackTapService.startService(context)
        }
    }

    fun getBackTapSensor(context: Context) =
        context.getSystemService(SensorManager::class.java)!!.getDefaultSensor(SENSOR_TYPE_BACK_TAP)

    fun isBackTapAvailable(context: Context) = getBackTapSensor(context) != null
}
