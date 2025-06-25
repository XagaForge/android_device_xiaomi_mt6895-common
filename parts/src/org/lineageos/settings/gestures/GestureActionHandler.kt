/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.gestures

import android.app.SearchManager
import android.app.StatusBarManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager.ScreenshotSource.SCREENSHOT_VENDOR_GESTURE
import org.lineageos.settings.utils.dlog
import com.android.internal.app.AssistUtils
import com.android.internal.util.ScreenshotHelper

class GestureActionHandler private constructor(private val context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)!!
    private val cameraManager = context.getSystemService(CameraManager::class.java)!!
    private val powerManager = context.getSystemService(PowerManager::class.java)!!
    private val statusBarManager = context.getSystemService(StatusBarManager::class.java)!!
    private val vibrator = context.getSystemService(Vibrator::class.java)!!

    private val handler = Handler(Looper.getMainLooper())
    private val screenshotHelper = ScreenshotHelper(context)

    private val vibrationEffect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
    private val vibrationAttrs =
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)

    private var torchOn = false

    init {
        cameraManager.registerTorchCallback(
            object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == REAR_CAMERA_ID) {
                        torchOn = enabled
                    }
                }
            },
            handler
        )
    }

    fun performAction(actionId: Int) {
        dlog(TAG, "performAction($actionId)")
        when (actionId) {
            1 -> takeScreenshot()
            2 -> launchAssist()
            3 -> playPauseMedia()
            4 -> showNotifications()
            5 -> launchCamera()
            6 -> toggleFlashlight()
            7 -> toggleRingerMode(AudioManager.RINGER_MODE_SILENT)
            8 -> toggleRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            9 -> showVolumePanel()
            10 -> goToSleep()
            // TODO launch custom app
            else -> Log.e(TAG, "unsupported action: $actionId")
        }
    }

    private fun takeScreenshot() {
        dlog(TAG, "takeScreenshot")
        screenshotHelper.takeScreenshot(SCREENSHOT_VENDOR_GESTURE, handler, null)
    }

    private fun launchAssist() {
        dlog(TAG, "launchAssist")
        val searchManager = context.getSystemService(SearchManager::class.java)
        if (searchManager == null) {
            dlog(TAG, "launchAssist: searchManager is null!")
            return
        }
        vibrate()
        val args = Bundle()
        args.putLong(Intent.EXTRA_TIME, SystemClock.uptimeMillis())
        args.putInt(AssistUtils.INVOCATION_TYPE_KEY, AssistUtils.INVOCATION_TYPE_PHYSICAL_GESTURE)
        searchManager.launchAssist(args)
    }

    private fun playPauseMedia() {
        dlog(TAG, "playPauseMedia")
        vibrate()
        val time = SystemClock.uptimeMillis()
        var event = KeyEvent(time, time, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0)
        audioManager.dispatchMediaKeyEvent(event)
        event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP)
        audioManager.dispatchMediaKeyEvent(event)
    }

    private fun showNotifications() {
        dlog(TAG, "showNotifications")
        statusBarManager.expandNotificationsPanel()
    }

    private fun launchCamera() {
        dlog(TAG, "launchCamera")
        vibrate()
        context.startActivity(
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun toggleFlashlight() {
        dlog(TAG, "toggleFlashlight: torchOn=$torchOn")
        vibrate()
        cameraManager.setTorchMode(REAR_CAMERA_ID, !torchOn)
    }

    private fun toggleRingerMode(ringerMode: Int) {
        val currentMode = audioManager.getRingerModeInternal()
        dlog(TAG, "toggleRingerMode: $ringerMode currentMode=$currentMode")
        vibrate()
        audioManager.setRingerModeInternal(
            if (currentMode != ringerMode) ringerMode else AudioManager.RINGER_MODE_NORMAL
        )
    }

    private fun showVolumePanel() {
        dlog(TAG, "showVolumePanel")
        audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }

    private fun goToSleep() {
        dlog(TAG, "goToSleep")
        vibrate()
        powerManager.goToSleep(SystemClock.uptimeMillis())
    }

    private fun vibrate() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(vibrationEffect, vibrationAttrs)
        }
    }

    companion object {
        private const val TAG = "GestureActionHandler"
        private const val REAR_CAMERA_ID = "0"

        @Volatile private var instance: GestureActionHandler? = null

        fun getInstance(context: Context) =
            instance
                ?: synchronized(this) {
                    instance
                        ?: GestureActionHandler(context.applicationContext).also { instance = it }
                }
    }
}
