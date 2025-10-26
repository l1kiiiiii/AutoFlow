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
            notificationManager?.isNotificationPolicyAccessGranted ?: false
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
     * ✅ FIXED: Set ringer mode with permission check
     */
    fun setRingerMode(mode: String): Boolean {
        if (audioManager == null) {
            Log.e(TAG, "❌ AudioManager is null")
            return false
        }

        // Check permission first for DND
        if (mode.uppercase() == "DND" && !hasDndPermission()) {
            Log.w(TAG, "⚠️ Do Not Disturb permission not granted")
            return false
        }

        return try {
            val result = when (mode.uppercase()) {
                "SILENT" -> setSilentMode()
                "VIBRATE" -> setVibrateMode()
                "NORMAL" -> setNormalMode()
                "DND" -> setDNDMode()
                else -> setNormalMode()
            }

            if (result) {
                Log.d(TAG, "✅ Set ringer mode to: $mode")
            } else {
                Log.e(TAG, "❌ Failed to set ringer mode to: $mode")
            }

            result
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
            when (mode.trim().uppercase()) {
                "NORMAL", "RING" -> setNormalMode()
                "SILENT" -> setSilentMode()
                "VIBRATE" -> setVibrateMode()
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
        return when (audioManager?.ringerMode) {
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
        return try {
            // First restore DND interruption filter if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
                val currentFilter = notificationManager.currentInterruptionFilter
                if (currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    Log.d(TAG, "🔔 DND interruption filter restored to ALL")
                }
            }

            // Then restore normal ringer mode
            audioManager?.ringerMode = AudioManager.RINGER_MODE_NORMAL
            Log.d(TAG, "🔊 Normal mode activated")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set normal mode", e)
            false
        }
    }

    private fun setSilentMode(): Boolean {
        return try {
            audioManager?.ringerMode = AudioManager.RINGER_MODE_SILENT
            Log.d(TAG, "🔇 Silent mode activated")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set silent mode", e)
            false
        }
    }

    private fun setVibrateMode(): Boolean {
        return try {
            audioManager?.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            Log.d(TAG, "📳 Vibrate mode activated")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set vibrate mode", e)
            false
        }
    }

    fun isDNDActive(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
                val filter = notificationManager.currentInterruptionFilter
                val isActive = filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
                        filter == NotificationManager.INTERRUPTION_FILTER_NONE
                Log.d(TAG, "🔍 DND status: Filter=$filter, Active=$isActive")
                return isActive
            } else {
                // For older versions, check if ringer is silent
                val isSilent = audioManager?.ringerMode == AudioManager.RINGER_MODE_SILENT
                Log.d(TAG, "🔍 Silent mode (legacy DND): $isSilent")
                return isSilent
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking DND status", e)
            false
        }
    }

    /**
     * ✅ FIXED: Properly activate Do Not Disturb mode
     */
    fun setDNDMode(): Boolean {
        if (notificationManager == null) {
            Log.e(TAG, "❌ NotificationManager is null")
            return false
        }

        // Check DND permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                Log.w(TAG, "❌ DND access not granted")
                requestDndPermission()
                return false
            }
        }

        return try {
            // Step 1: Set ringer to silent first
            audioManager?.ringerMode = AudioManager.RINGER_MODE_SILENT
            Log.d(TAG, "🔇 Ringer set to silent for DND")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Step 2: Configure DND policy - only allow alarms
                val dndPolicy = NotificationManager.Policy(
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS, // Only alarms allowed
                    0,    // ✅ FIXED: Use 0 for no priority senders
                    0 // No suppressed visual effects
                )

                notificationManager.setNotificationPolicy(dndPolicy)
                Log.d(TAG, "📋 DND policy configured")

                // Step 3: Activate DND interruption filter
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                Log.d(TAG, "🔕 DND interruption filter activated")

                // Step 4: Verify DND is actually active
                val currentFilter = notificationManager.currentInterruptionFilter
                val isActive = currentFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
                Log.d(TAG, "🔍 DND verification - Current filter: $currentFilter, Active: $isActive")

                if (!isActive) {
                    Log.e(TAG, "❌ DND activation failed - filter not set properly")
                    return false
                }

                return true
            } else {
                // For older Android versions, silent mode is the best we can do
                Log.d(TAG, "🔕 DND activated (legacy silent mode)")
                return true
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException: DND permission required", e)
            requestDndPermission()
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to activate DND", e)
            false
        }
    }
}
