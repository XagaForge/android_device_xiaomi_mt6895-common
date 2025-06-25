/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.gestures

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import android.os.UserHandle
import androidx.preference.PreferenceManager
import org.lineageos.settings.utils.dlog

/**
 * This service listens for double-tap and triple-tap on the back of the device and triggers
 * user-set gesture action
 */
class BackTapService : Service() {

    private lateinit var sensorManager: SensorManager
    private var backTapSensor: Sensor? = null
    private lateinit var actionHandler: GestureActionHandler
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var powerManager: PowerManager
    private var doubleTapAction = 0
    private var tripleTapAction = 0
    private var isSensorListening = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                dlog(TAG, "registering sensor")
                sensorManager.registerListener(
                    sensorListener,
                    backTapSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                )
            } else {
                dlog(TAG, "unregistering sensor")
                sensorManager.unregisterListener(sensorListener, backTapSensor)
            }
        }

    private val screenStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON,
                    Intent.ACTION_SCREEN_OFF -> {
                        dlog(TAG, "Received ${intent.action}")
                        maybeListenSensor()
                    }
                }
            }
        }

    private val sensorListener =
        object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent) {
                val value = event.values[0].toInt()
                dlog(TAG, "onSensorChanged: value=$value")

                when (value) {
                    2 -> actionHandler.performAction(doubleTapAction)
                    3 -> actionHandler.performAction(tripleTapAction)
                }
            }
        }

    private val sharedPrefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                GestureUtils.PREF_KEY_BACK_DOUBLE_TAP -> {
                    doubleTapAction = prefs.getString(key, "0")!!.toInt()
                    dlog(TAG, "Double tap action set to $doubleTapAction")
                    maybeListenSensor()
                }
                GestureUtils.PREF_KEY_BACK_TRIPLE_TAP -> {
                    tripleTapAction = prefs.getString(key, "0")!!.toInt()
                    dlog(TAG, "Triple tap action set to $tripleTapAction")
                    maybeListenSensor()
                }
            }
        }

    override fun onCreate() {
        dlog(TAG, "Creating service")
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dlog(TAG, "Starting service")
        backTapSensor = GestureUtils.getBackTapSensor(this)
        actionHandler = GestureActionHandler.getInstance(this)
        sharedPrefs =
            PreferenceManager.getDefaultSharedPreferences(this).apply {
                registerOnSharedPreferenceChangeListener(sharedPrefsListener)
            }
        registerReceiver(
            screenStateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
        )

        // start with initial values
        doubleTapAction =
            sharedPrefs.getString(GestureUtils.PREF_KEY_BACK_DOUBLE_TAP, "0")!!.toInt()
        tripleTapAction =
            sharedPrefs.getString(GestureUtils.PREF_KEY_BACK_TRIPLE_TAP, "0")!!.toInt()
        maybeListenSensor()
        return START_STICKY
    }

    override fun onDestroy() {
        dlog(TAG, "Destroying service")
        if (backTapSensor != null) {
            isSensorListening = false
            unregisterReceiver(screenStateReceiver)
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun maybeListenSensor() {
        val isInteractive = powerManager.isInteractive
        dlog(
            TAG,
            "maybeListenSensor: doubleAction=$doubleTapAction tripleAction=$tripleTapAction" +
                " isInteractive=$isInteractive"
        )
        isSensorListening = isInteractive && (doubleTapAction != 0 || tripleTapAction != 0)
    }

    companion object {
        private const val TAG = "BackTapService"

        fun startService(context: Context) {
            context.startServiceAsUser(
                Intent(context, BackTapService::class.java),
                UserHandle.CURRENT,
            )
        }
    }
}
