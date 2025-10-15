package com.example.autoflow.integrations

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log

class SoundModeManager(context: Context) {

    private val context: Context = context
    private val audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val notificationManager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    companion object {
        private const val TAG = "SoundModeManager"
    }
    /**
     * ✅ Check if app has DND permission
     */
    fun hasDndPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager?.isNotificationPolicyAccessGranted ?: false // Safe call
        } else {
            true // Pre-Marshmallow doesn't need permission
        }
    }

    /**
     * ✅ Request DND permission
     */
    fun requestDndPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open DND settings", e)
            }
        }
    }

    /**
     * ✅ Set ringer mode with permission check
     */
    fun setRingerMode(mode: String): Boolean {
        // Check if audioManager is available
        if (audioManager == null) {
            Log.e(TAG, "❌ AudioManager is null")
            return false
        }

        // Check permission first
        if (!hasDndPermission()) {
            Log.w(TAG, "⚠️ Do Not Disturb permission not granted")
            return false
        }

        return try {
            val ringerMode = when (mode.uppercase()) {
                "SILENT" -> AudioManager.RINGER_MODE_SILENT
                "VIBRATE" -> AudioManager.RINGER_MODE_VIBRATE
                "NORMAL" -> AudioManager.RINGER_MODE_NORMAL
                "DND" -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
            }

            audioManager.ringerMode = ringerMode
            Log.d(TAG, "✅ Set ringer mode to: $mode")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException: Missing DND permission", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set ringer mode", e)
            false
        }
    }
    //  PUBLIC METHODS

    fun setSoundMode(mode: String): Boolean {
        Log.d(TAG, "Setting sound mode to: $mode")

        if (audioManager == null) {
            Log.e(TAG, "AudioManager is null")
            return false
        }

        return try {
            when (mode) {
                "Normal" -> setNormalMode()
                "Silent" -> setSilentMode()
                "Vibrate" -> setVibrateMode()
                "DND" -> setDNDMode()
                else -> {
                    Log.w(TAG, "Unknown mode: $mode")
                    false
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error setting sound mode: ${e.message}", e)
            false
        }
    }

    fun getCurrentMode(): String {
        return when (audioManager?.ringerMode) { //  Safe call
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            AudioManager.RINGER_MODE_NORMAL -> "Normal"
            else -> "Unknown"
        }
    }

    fun isDNDEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager?.let {
                return it.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            }
        }
        return false
    }

    fun hasDNDPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return notificationManager?.isNotificationPolicyAccessGranted == true
        }
        return true // Not needed on older versions
    }

    fun openDNDSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun cleanup() {
        Log.d(TAG, "Cleanup called")
    }

    //  PRIVATE HELPER METHODS

    private fun setNormalMode(): Boolean {
        audioManager!!.ringerMode = AudioManager.RINGER_MODE_NORMAL
        Log.d(TAG, "✅ Set to Normal mode")
        return true
    }

    private fun setSilentMode(): Boolean {
        audioManager!!.ringerMode = AudioManager.RINGER_MODE_SILENT
        Log.d(TAG, "✅ Set to Silent mode")
        return true
    }

    private fun setVibrateMode(): Boolean {
        audioManager!!.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        Log.d(TAG, "✅ Set to Vibrate mode")
        return true
    }

    private fun setDNDMode(): Boolean {
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager is null")
            return false
        }

        // Check DND permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                Log.w(TAG, "DND access not granted - opening settings")
                openDNDSettings()
                return false
            }

            // Set DND mode
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            Log.d(TAG, "✅ Set to DND mode")
            return true
        } else {
            // Fallback to silent mode for older devices
            Log.w(TAG, "DND not available, using Silent mode")
            return setSilentMode()
        }
    }
}
