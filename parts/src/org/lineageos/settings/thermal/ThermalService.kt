/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal

import android.app.ActivityTaskManager
import android.app.Service
import android.app.TaskStackListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import org.lineageos.settings.utils.dlog

/** Service to monitor current top (foreground) app and set thermal profile accordingly. */
class ThermalService : Service() {

    private lateinit var thermalUtils: ThermalUtils

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentApp: String? = null
    private var destroyed = false

    private val taskListener =
        object : TaskStackListener() {
            override fun onTaskStackChanged() {
                mainHandler.post { updateThermalProfile() }
            }
        }

    private val intentReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON -> updateThermalProfile()
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        dlog(TAG, "Creating service")
        thermalUtils = ThermalUtils.getInstance(this)
        runCatching { ActivityTaskManager.getService().registerTaskStackListener(taskListener) }
            .onFailure { Log.e(TAG, "Could not register task stack listener", it) }
        registerReceiver(
            intentReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
        )
    }

    override fun onDestroy() {
        dlog(TAG, "Destroying service")
        destroyed = true
        unregisterReceiver(intentReceiver)
        runCatching { ActivityTaskManager.getService().unregisterTaskStackListener(taskListener) }
            .onFailure { Log.e(TAG, "Could not unregister task stack listener", it) }
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dlog(TAG, "Starting service")
        if (!thermalUtils.enabled) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        updateThermalProfile(force = true)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateThermalProfile(force: Boolean = false) {
        if (destroyed || !thermalUtils.enabled) return

        val packageName = if (getSystemService(PowerManager::class.java).isInteractive) {
            runCatching {
                ActivityTaskManager.getService().focusedRootTaskInfo?.topActivity?.packageName
            }.onFailure {
                Log.e(TAG, "Could not get foreground app", it)
            }.getOrElse { return }.orEmpty()
        } else {
            ""
        }
        if (!force && packageName == currentApp) return

        val applied = if (packageName.isEmpty()) {
            thermalUtils.setDefaultThermalProfile()
        } else {
            thermalUtils.setThermalProfile(packageName)
        }
        currentApp = if (applied) packageName else null
    }

    companion object {
        private const val TAG = "ThermalService"
    }
}
