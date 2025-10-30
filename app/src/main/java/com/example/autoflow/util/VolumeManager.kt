package com.example.autoflow.util

import android.content.Context
import android.media.AudioManager
import android.util.Log
import kotlin.math.max
import kotlin.math.min

/**
 * VolumeManager - Comprehensive volume control for AutoFlow
 * Supports all audio stream types: media, ring, notification, alarm, call
 */
class VolumeManager(private val context: Context) {

    companion object {
        private const val TAG = "VolumeManager"
        private const val PREFS_NAME = "volume_prefs"
        private const val KEY_PREVIOUS_MEDIA_VOLUME = "previous_media_volume"
        private const val KEY_PREVIOUS_RING_VOLUME = "previous_ring_volume"
        private const val KEY_PREVIOUS_NOTIFICATION_VOLUME = "previous_notification_volume"
        private const val KEY_PREVIOUS_ALARM_VOLUME = "previous_alarm_volume"
        private const val KEY_PREVIOUS_CALL_VOLUME = "previous_call_volume"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Set volume for specific stream type
     */
    fun setVolume(streamType: String, volume: Int, showUI: Boolean = false): Boolean {
        return try {
            val audioStreamType = getAudioStreamType(streamType)
            val maxVolume = audioManager.getStreamMaxVolume(audioStreamType)
            val clampedVolume = clampVolume(volume, maxVolume)
            
            // Store current volume before changing
            storePreviousVolume(streamType, audioManager.getStreamVolume(audioStreamType))
            
            // Set volume
            val flags = if (showUI) AudioManager.FLAG_SHOW_UI else 0
            audioManager.setStreamVolume(audioStreamType, clampedVolume, flags)
            
            Log.d(TAG, "✅ $streamType volume set to $clampedVolume/$maxVolume")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set $streamType volume", e)
            false
        }
    }

    /**
     * Set volume by percentage (0-100)
     */
    fun setVolumeByPercentage(streamType: String, percentage: Int, showUI: Boolean = false): Boolean {
        val audioStreamType = getAudioStreamType(streamType)
        val maxVolume = audioManager.getStreamMaxVolume(audioStreamType)
        val volume = ((percentage / 100.0) * maxVolume).toInt()
        return setVolume(streamType, volume, showUI)
    }

    /**
     * Increase volume by amount
     */
    fun increaseVolume(streamType: String, amount: Int, showUI: Boolean = false): Boolean {
        val audioStreamType = getAudioStreamType(streamType)
        val currentVolume = audioManager.getStreamVolume(audioStreamType)
        val maxVolume = audioManager.getStreamMaxVolume(audioStreamType)
        val newVolume = min(currentVolume + amount, maxVolume)
        return setVolume(streamType, newVolume, showUI)
    }

    /**
     * Decrease volume by amount
     */
    fun decreaseVolume(streamType: String, amount: Int, showUI: Boolean = false): Boolean {
        val audioStreamType = getAudioStreamType(streamType)
        val currentVolume = audioManager.getStreamVolume(audioStreamType)
        val newVolume = max(currentVolume - amount, 0)
        return setVolume(streamType, newVolume, showUI)
    }

    /**
     * Get current volume for stream type
     */
    fun getCurrentVolume(streamType: String): Int {
        return try {
            val audioStreamType = getAudioStreamType(streamType)
            audioManager.getStreamVolume(audioStreamType)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get $streamType volume", e)
            0
        }
    }

    /**
     * Get current volume as percentage
     */
    fun getCurrentVolumePercentage(streamType: String): Int {
        return try {
            val audioStreamType = getAudioStreamType(streamType)
            val currentVolume = audioManager.getStreamVolume(audioStreamType)
            val maxVolume = audioManager.getStreamMaxVolume(audioStreamType)
            if (maxVolume > 0) {
                ((currentVolume.toDouble() / maxVolume) * 100).toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get $streamType volume percentage", e)
            0
        }
    }

    /**
     * Get maximum volume for stream type
     */
    fun getMaxVolume(streamType: String): Int {
        return try {
            val audioStreamType = getAudioStreamType(streamType)
            audioManager.getStreamMaxVolume(audioStreamType)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get max $streamType volume", e)
            15 // Default reasonable max
        }
    }

    /**
     * Mute/unmute specific stream
     */
    fun setMute(streamType: String, mute: Boolean): Boolean {
        return try {
            val audioStreamType = getAudioStreamType(streamType)
            
            if (mute) {
                // Store current volume before muting
                val currentVolume = audioManager.getStreamVolume(audioStreamType)
                storePreviousVolume(streamType, currentVolume)
                audioManager.setStreamVolume(audioStreamType, 0, 0)
            } else {
                // Restore previous volume
                val previousVolume = getPreviousVolume(streamType)
                if (previousVolume > 0) {
                    audioManager.setStreamVolume(audioStreamType, previousVolume, 0)
                } else {
                    // Set to 50% if no previous volume stored
                    val maxVolume = audioManager.getStreamMaxVolume(audioStreamType)
                    audioManager.setStreamVolume(audioStreamType, maxVolume / 2, 0)
                }
            }
            
            Log.d(TAG, "✅ $streamType ${if (mute) "muted" else "unmuted"}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to ${if (mute) "mute" else "unmute"} $streamType", e)
            false
        }
    }

    /**
     * Set volume profile (all stream types at once)
     */
    fun setVolumeProfile(profile: String): Boolean {
        return try {
            when (profile.lowercase()) {
                "silent" -> {
                    setVolumeByPercentage("media", 0)
                    setVolumeByPercentage("ring", 0)
                    setVolumeByPercentage("notification", 0)
                    setVolumeByPercentage("alarm", 10) // Keep alarm slightly audible
                }
                "low" -> {
                    setVolumeByPercentage("media", 25)
                    setVolumeByPercentage("ring", 25)
                    setVolumeByPercentage("notification", 20)
                    setVolumeByPercentage("alarm", 50)
                }
                "medium" -> {
                    setVolumeByPercentage("media", 50)
                    setVolumeByPercentage("ring", 50)
                    setVolumeByPercentage("notification", 50)
                    setVolumeByPercentage("alarm", 70)
                }
                "high" -> {
                    setVolumeByPercentage("media", 75)
                    setVolumeByPercentage("ring", 75)
                    setVolumeByPercentage("notification", 75)
                    setVolumeByPercentage("alarm", 90)
                }
                "max", "maximum" -> {
                    setVolumeByPercentage("media", 100)
                    setVolumeByPercentage("ring", 100)
                    setVolumeByPercentage("notification", 100)
                    setVolumeByPercentage("alarm", 100)
                }
                "meeting" -> {
                    // Meeting profile - silent media, low notifications, normal alarms
                    setVolumeByPercentage("media", 0)
                    setVolumeByPercentage("ring", 0)
                    setVolumeByPercentage("notification", 0)
                    setVolumeByPercentage("alarm", 70)
                }
                "work" -> {
                    // Work profile - medium media, low ring, normal notifications
                    setVolumeByPercentage("media", 40)
                    setVolumeByPercentage("ring", 30)
                    setVolumeByPercentage("notification", 50)
                    setVolumeByPercentage("alarm", 80)
                }
                "night" -> {
                    // Night profile - very low media, silent ring, low notifications
                    setVolumeByPercentage("media", 20)
                    setVolumeByPercentage("ring", 0)
                    setVolumeByPercentage("notification", 10)
                    setVolumeByPercentage("alarm", 60)
                }
                else -> {
                    Log.w(TAG, "Unknown volume profile: $profile")
                    return false
                }
            }
            
            Log.d(TAG, "✅ Volume profile '$profile' applied")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set volume profile '$profile'", e)
            false
        }
    }

    /**
     * Restore all volumes to previous levels
     */
    fun restoreAllVolumes(): Boolean {
        return try {
            val streamTypes = listOf("media", "ring", "notification", "alarm", "call")
            var success = true
            
            streamTypes.forEach { streamType ->
                val previousVolume = getPreviousVolume(streamType)
                if (previousVolume >= 0) {
                    if (!setVolume(streamType, previousVolume)) {
                        success = false
                    }
                }
            }
            
            Log.d(TAG, "✅ All volumes restored")
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to restore volumes", e)
            false
        }
    }

    /**
     * Get all current volume levels
     */
    fun getAllVolumeLevels(): Map<String, Int> {
        val streamTypes = listOf("media", "ring", "notification", "alarm", "call")
        return streamTypes.associateWith { getCurrentVolumePercentage(it) }
    }

    // Private helper methods
    
    private fun getAudioStreamType(streamType: String): Int {
        return when (streamType.lowercase()) {
            "media", "music" -> AudioManager.STREAM_MUSIC
            "ring", "ringer" -> AudioManager.STREAM_RING
            "notification", "notifications" -> AudioManager.STREAM_NOTIFICATION
            "alarm", "alarms" -> AudioManager.STREAM_ALARM
            "call", "voice_call" -> AudioManager.STREAM_VOICE_CALL
            "system" -> AudioManager.STREAM_SYSTEM
            else -> {
                Log.w(TAG, "Unknown stream type: $streamType, defaulting to MUSIC")
                AudioManager.STREAM_MUSIC
            }
        }
    }

    private fun clampVolume(volume: Int, maxVolume: Int): Int {
        return max(0, min(volume, maxVolume))
    }

    private fun storePreviousVolume(streamType: String, volume: Int) {
        val key = when (streamType.lowercase()) {
            "media", "music" -> KEY_PREVIOUS_MEDIA_VOLUME
            "ring", "ringer" -> KEY_PREVIOUS_RING_VOLUME
            "notification", "notifications" -> KEY_PREVIOUS_NOTIFICATION_VOLUME
            "alarm", "alarms" -> KEY_PREVIOUS_ALARM_VOLUME
            "call", "voice_call" -> KEY_PREVIOUS_CALL_VOLUME
            else -> "previous_${streamType}_volume"
        }
        prefs.edit().putInt(key, volume).apply()
    }

    private fun getPreviousVolume(streamType: String): Int {
        val key = when (streamType.lowercase()) {
            "media", "music" -> KEY_PREVIOUS_MEDIA_VOLUME
            "ring", "ringer" -> KEY_PREVIOUS_RING_VOLUME
            "notification", "notifications" -> KEY_PREVIOUS_NOTIFICATION_VOLUME
            "alarm", "alarms" -> KEY_PREVIOUS_ALARM_VOLUME
            "call", "voice_call" -> KEY_PREVIOUS_CALL_VOLUME
            else -> "previous_${streamType}_volume"
        }
        return prefs.getInt(key, -1)
    }
}