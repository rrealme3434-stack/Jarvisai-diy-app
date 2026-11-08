package com.Lia.assistant.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import android.view.KeyEvent

object DeviceController {
    private var isTorchOn = false

    var screenshotHandler: (() -> String)? = null
    var lockScreenHandler: (() -> String)? = null

    fun toggleFlashlight(context: Context, enable: Boolean? = null): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return "No flashlight hardware available, Boss"
            val targetState = enable ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, targetState)
            isTorchOn = targetState
            if (targetState) "Flashlight turned ON, Boss." else "Flashlight turned OFF, Boss."
        } catch (e: Exception) {
            "Unable to toggle flashlight: ${e.message}"
        }
    }

    fun isFlashlightOn(): Boolean = isTorchOn

    fun adjustVolume(context: Context, direction: Int): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return try {
            val flag = AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
            when (direction) {
                1 -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, flag)
                    "Volume increased, Boss."
                }
                -1 -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, flag)
                    "Volume decreased, Boss."
                }
                0 -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, flag)
                    "Phone muted, Boss."
                }
                else -> {
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, flag)
                    "Volume set to maximum, Boss."
                }
            }
        } catch (e: Exception) {
            "Volume adjustment error: ${e.message}"
        }
    }

    fun getBatteryInfo(context: Context): String {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
            val stateText = if (isCharging) "plugged in and charging" else "running on internal battery"
            "Power cell is at $pct%, $stateText, Boss."
        } catch (_: Exception) {
            "Battery telemetry nominal, Boss."
        }
    }

    fun openWifiSettings(context: Context): String {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            "Opening Wi-Fi controls, Boss."
        } catch (_: Exception) {
            "Unable to open Wi-Fi settings"
        }
    }

    fun openBluetoothSettings(context: Context): String {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            "Opening Bluetooth panel, Boss."
        } catch (_: Exception) {
            "Unable to open Bluetooth settings"
        }
    }

    fun openHotspotSettings(context: Context): String {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            "Opening Hotspot & Network configuration, Boss."
        } catch (_: Exception) {
            "Unable to open Network settings"
        }
    }

    fun setAlarm(context: Context, hour: Int, minute: Int, message: String = "Jarvis Alarm"): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            "Alarm scheduled for %02d:%02d, Boss.".format(hour, minute)
        } catch (_: Exception) {
            "Please check alarm permissions, Boss."
        }
    }

    fun setTimer(context: Context, seconds: Int, message: String = "Jarvis Timer"): String {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            "Timer set for ${seconds / 60} minutes, Boss."
        } catch (_: Exception) {
            "Unable to create timer, Boss."
        }
    }

    fun controlMedia(context: Context, command: String): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val keyCode = when (command.lowercase()) {
            "play", "resume" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause", "stop" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        return try {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            "Media command '$command' executed, Boss."
        } catch (e: Exception) {
            "Failed media control: ${e.message}"
        }
    }

    fun takeScreenshot(): String {
        return screenshotHandler?.invoke() ?: "Screenshot action requires Direct flavor accessibility permission, Boss."
    }

    fun lockScreen(): String {
        return lockScreenHandler?.invoke() ?: "Lock screen action requires Direct flavor accessibility permission, Boss."
    }
}
