package com.example.autoflow.util

import android.content.Context
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class VolumeManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "VolumeManager"

        @Volatile
        private var INSTANCE: VolumeManager? = null

        fun getInstance(context: Context): VolumeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolumeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    suspend fun setMediaVolume(
        percentage: Int,
        showUI: Boolean = false,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.Main) {
        setVolumeByPercentage("media", percentage, showUI, scope)
    }

    suspend fun setNotificationSoundVolume(
        percentage: Int,
        showUI: Boolean = false,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.Main) {
        setVolumeByPercentage("notification", percentage, showUI, scope)
    }

    suspend fun setPhoneVolume(
        percentage: Int,
        showUI: Boolean = false,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.Main) {
        setVolumeByPercentage("call", percentage, showUI, scope)
    }

    suspend fun setVolumeByPercentage(
        streamType: String,
        percentage: Int,
        showUI: Boolean = false,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.Main) {
        try {
            val audioStreamType = getAudioStreamType(streamType)
            val maxVolume = audioManager.getStreamMaxVolume(audioStreamType)
            val volume = ((percentage / 100.0) * maxVolume).toInt()
            val clampedVolume = max(0, min(volume, maxVolume))

            val flags = if (showUI) AudioManager.FLAG_SHOW_UI else 0
            audioManager.setStreamVolume(audioStreamType, clampedVolume, flags)

            Log.d(TAG, "✅ $streamType volume set to $clampedVolume/$maxVolume ($percentage%)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set $streamType volume", e)
            false
        }
    }

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
}
