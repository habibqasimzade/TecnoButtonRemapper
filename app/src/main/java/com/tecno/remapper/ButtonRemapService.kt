package com.tecno.remapper

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class ButtonRemapService : AccessibilityService() {

    private var lastVolDownTime: Long = 0
    private var lastVolUpTime: Long = 0
    private var lastPowerTime: Long = 0

    private val VOLUME_DOUBLE_PRESS_DELAY = 400L // 0.4 saniyə
    private val POWER_DOUBLE_PRESS_DELAY = 850L  // 0.85 saniyə

    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var isTorchOn = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            super.onTorchModeChanged(id, enabled)
            if (id == cameraId) {
                isTorchOn = enabled
            }
        }
    }

    // Güc (Power) düyməsinə 2 dəfə basılmasını ekranın sönüb-yanma siqnalı ilə tuturuq
    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF || intent?.action == Intent.ACTION_SCREEN_ON) {
                val actionCode = getSavedAction("ACTION_POWER", "GOOGLE_PAY")
                if (actionCode == "NONE") return

                val now = System.currentTimeMillis()
                if (now - lastPowerTime <= POWER_DOUBLE_PRESS_DELAY) {
                    lastPowerTime = 0L
                    vibrateShort()
                    executeAction(actionCode)
                } else {
                    lastPowerTime = now
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull()
            cameraManager.registerTorchCallback(torchCallback, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(powerReceiver, filter)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            val now = System.currentTimeMillis()

            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val actionCode = getSavedAction("ACTION_VOL_DOWN", "FLASHLIGHT")
                    if (actionCode != "NONE") {
                        if (now - lastVolDownTime <= VOLUME_DOUBLE_PRESS_DELAY) {
                            lastVolDownTime = 0L
                            vibrateShort()
                            executeAction(actionCode)
                            return true // Səsin azalmasının qarşısını alır
                        } else {
                            lastVolDownTime = now
                        }
                    }
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    val actionCode = getSavedAction("ACTION_VOL_UP", "NONE")
                    if (actionCode != "NONE") {
                        if (now - lastVolUpTime <= VOLUME_DOUBLE_PRESS_DELAY) {
                            lastVolUpTime = 0L
                            vibrateShort()
                            executeAction(actionCode)
                            return true // Səsin artmasının qarşısını alır
                        } else {
                            lastVolUpTime = now
                        }
                    }
                }
            }
        }
        return super.onKeyEvent(event)
    }

    private fun getSavedAction(key: String, defaultVal: String): String {
        val prefs = getSharedPreferences("RemapPrefs", Context.MODE_PRIVATE)
        return prefs.getString(key, defaultVal) ?: defaultVal
    }

    private fun executeAction(actionCode: String) {
        when (actionCode) {
            "FLASHLIGHT" -> toggleFlashlight()
            "GOOGLE_PAY" -> openGooglePay()
            "CAMERA" -> openCamera()
            "WHATSAPP" -> openAppByPackage("com.whatsapp", "WhatsApp")
            "MUTE" -> toggleMute()
        }
    }

    private fun toggleFlashlight() {
        try {
            cameraId?.let { id ->
                cameraManager.setTorchMode(id, !isTorchOn)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Fənər xətası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGooglePay() {
        openAppByPackage("com.google.android.apps.walletnfcrel", "Google Wallet (Pay)")
    }

    private fun openCamera() {
        try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Kamera açıla bilmədi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppByPackage(pkg: String, appName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "$appName telefonda tapılmadı!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleMute() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            Toast.makeText(this, "Vibrasiya rejimi", Toast.LENGTH_SHORT).show()
        } else {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            Toast.makeText(this, "Normal səs rejimi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun vibrateShort() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(60)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraManager.unregisterTorchCallback(torchCallback)
            unregisterReceiver(powerReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}