package com.example.autoflow.util

import android.content.Context
import android.util.Log
import com.example.autoflow.model.Action
import kotlin.math.max
import kotlin.math.min

/**
 * Extensions to ActionExecutor for brightness and volume control
 * This file contains all the new brightness and volume control functionality
 */
object ActionExecutorExtensions {
    
    private const val TAG = "ActionExecutorExt"
    
    /**
     * Execute brightness and volume control actions
     */
    fun executeExtendedAction(context: Context, action: Action): Boolean {
        return try {
            when (action.type) {
                // ✅ BRIGHTNESS CONTROL ACTIONS
                Constants.ACTION_SET_BRIGHTNESS -> {
                    setBrightness(context, action)
                }
                
                Constants.ACTION_INCREASE_BRIGHTNESS -> {
                    increaseBrightness(context, action)
                }
                
                Constants.ACTION_DECREASE_BRIGHTNESS -> {
                    decreaseBrightness(context, action)
                }
                
                Constants.ACTION_ADJUST_BRIGHTNESS_TIME -> {
                    adjustBrightnessForTime(context)
                }
                
                Constants.ACTION_BRIGHTNESS_ENVIRONMENT -> {
                    setBrightnessForEnvironment(context, action)
                }
                
                Constants.ACTION_BRIGHTNESS_LEVEL -> {
                    setBrightnessLevel(context, action)
                }
                
                Constants.ACTION_RESTORE_BRIGHTNESS -> {
                    restoreBrightness(context)
                }
                
                // ✅ VOLUME CONTROL ACTIONS
                Constants.ACTION_SET_MEDIA_VOLUME -> {
                    setVolume(context, "media", action)
                }
                
                Constants.ACTION_SET_RING_VOLUME -> {
                    setVolume(context, "ring", action)
                }
                
                Constants.ACTION_SET_NOTIFICATION_VOLUME -> {
                    setVolume(context, "notification", action)
                }
                
                Constants.ACTION_SET_ALARM_VOLUME -> {
                    setVolume(context, "alarm", action)
                }
                
                Constants.ACTION_SET_CALL_VOLUME -> {
                    setVolume(context, "call", action)
                }
                
                Constants.ACTION_INCREASE_VOLUME -> {
                    increaseVolume(context, action)
                }
                
                Constants.ACTION_DECREASE_VOLUME -> {
                    decreaseVolume(context, action)
                }
                
                Constants.ACTION_MUTE_VOLUME -> {
                    muteVolume(context, action)
                }
                
                Constants.ACTION_UNMUTE_VOLUME -> {
                    unmuteVolume(context, action)
                }
                
                Constants.ACTION_SET_VOLUME_PROFILE -> {
                    setVolumeProfile(context, action)
                }
                
                Constants.ACTION_RESTORE_VOLUMES -> {
                    restoreVolumes(context)
                }
                
                else -> {
                    false // Action not handled by extensions
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error executing extended action ${action.type}", e)
            false
        }
    }
    
    // ==================== BRIGHTNESS CONTROL IMPLEMENTATIONS ====================
    
    /**
     * ✅ Set fixed brightness amount (0-100 percentage or 0-255 absolute)
     */
    private fun setBrightness(context: Context, action: Action): Boolean {
        return try {
            val brightnessManager = BrightnessManager(context)
            val brightness = action.value?.toIntOrNull() ?: 50 // Default 50%
            
            val success = if (brightness <= 100) {
                // Treat as percentage
                brightnessManager.setBrightnessByPercentage(brightness)
            } else {
                // Treat as absolute value (0-255)
                brightnessManager.setSystemBrightness(brightness)
            }
            
            if (success) {
                ActionExecutor.sendNotification(
                    context,
                    "💡 Brightness Set",
                    "Screen brightness adjusted to $brightness",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting brightness", e)
            false
        }
    }
    
    private fun increaseBrightness(context: Context, action: Action): Boolean {
        return try {
            val brightnessManager = BrightnessManager(context)
            val amount = action.value?.toIntOrNull() ?: 20 // Default increase by 20%
            val currentPercentage = brightnessManager.getCurrentBrightnessPercentage()
            
            val success = brightnessManager.setBrightnessByPercentage(
                min(currentPercentage + amount, 100)
            )
            
            if (success) {
                ActionExecutor.sendNotification(
                    context,
                    "📈 Brightness Increased",
                    "Brightness increased by $amount% (now ${min(currentPercentage + amount, 100)}%)",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error increasing brightness", e)
            false
        }
    }
    
    private fun decreaseBrightness(context: Context, action: Action): Boolean {
        return try {
            val brightnessManager = BrightnessManager(context)
            val amount = action.value?.toIntOrNull() ?: 20 // Default decrease by 20%
            val currentPercentage = brightnessManager.getCurrentBrightnessPercentage()
            
            val success = brightnessManager.setBrightnessByPercentage(
                max(currentPercentage - amount, 1)
            )
            
            if (success) {
                ActionExecutor.sendNotification(
                    context,
                    "📉 Brightness Decreased",
                    "Brightness decreased by $amount% (now ${max(currentPercentage - amount, 1)}%)",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error decreasing brightness", e)
            false
        }
    }
    
    private fun adjustBrightnessForTime(context: Context): Boolean {
        return try {
            val brightnessManager = BrightnessManager(context)
            val success = brightnessManager.adjustBrightnessForTimeOfDay()
            
            if (success) {
                ActionExecutor.sendNotification(
                    context,
                    "⏰ Time-Based Brightness",
                    "Brightness adjusted automatically for current time",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adjusting brightness for time", e)
            false
        }
    }
    
    private fun setBrightnessForEnvironment(context: Context, action: Action): Boolean {
        return try {
            val brightnessManager = BrightnessManager(context)
            val environment = action.value ?: "indoor"
            val success = brightnessManager.setBrightnessForEnvironment(environment)
            
            if (success) {
                ActionExecutor.sendNotification(
                    context,
                    "🌍 Environment Brightness",
                    "Brightness adjusted for environment: $environment",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting environment brightness", e)
            false
        }
    }
    
    private fun setBrightnessLevel(context: Context, action: Action): Boolean {
        return try {
            val brightnessManager = BrightnessManager(context)
            val level = action.value ?: "medium"
            val success = brightnessManager.setBrightnessLevel(level)
            
            if (success) {
                ActionExecutor.sendNotification(
                    context,
                    "📊 Brightness Level",
                    "Brightness set to $level level",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting brightness level", e)
            false
        }
    }
    
    private fun restoreBrightness(context: Context): Boolean {
        return try {
            val brightnessManager = BrightnessManager(context)
            val success = brightnessManager.restorePreviousBrightness()
            
            if (success) {
                ActionExecutor.sendNotification(
                    context,
                    "🔄 Brightness Restored",
                    "Previous brightness level restored",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error restoring brightness", e)
            false
        }
    }
    
    // ==================== VOLUME CONTROL IMPLEMENTATIONS ====================
    
    /**
     * ✅ Set volume for specific stream type
     */
    private fun setVolume(context: Context, streamType: String, action: Action): Boolean {
        return try {
            val volumeManager = VolumeManager(context)
            val volume = action.value?.toIntOrNull() ?: 50 // Default 50%
            val showUI = action.title?.equals("show_ui", true) ?: false
            
            val success = if (volume <= 100) {
                // Treat as percentage
                volumeManager.setVolumeByPercentage(streamType, volume, showUI)
            } else {
                // Treat as absolute value
                volumeManager.setVolume(streamType, volume, showUI)
            }
            
            if (success) {
                ActionExecutor.sendNotification(
                    context,
                    "🔊 ${streamType.replaceFirstChar { it.uppercaseChar() }} Volume Set",
                    "${streamType.replaceFirstChar { it.uppercaseChar() }} volume set to $volume",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting $streamType volume", e)
            false
        }
    }
    
    private fun increaseVolume(context: Context, action: Action): Boolean {
        return try {
            val volumeManager = VolumeManager(context)
            val streamType = action.title ?: "media" // Default to media
            val amount = action.value?.toIntOrNull() ?: 10 // Default increase by 10%
            val showUI = action.message?.equals("show_ui", true) ?: false
            
            val success = volumeManager.increaseVolume(streamType, amount, showUI)
            
            if (success) {
                val currentVolume = volumeManager.getCurrentVolumePercentage(streamType)
                ActionExecutor.sendNotification(
                    context,
                    "📈 Volume Increased",
                    "${streamType.replaceFirstChar { it.uppercaseChar() }} volume increased by $amount% (now $currentVolume%)",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error increasing volume", e)
            false
        }
    }
    
    private fun decreaseVolume(context: Context, action: Action): Boolean {
        return try {
            val volumeManager = VolumeManager(context)
            val streamType = action.title ?: "media" // Default to media
            val amount = action.value?.toIntOrNull() ?: 10 // Default decrease by 10%
            val showUI = action.message?.equals("show_ui", true) ?: false
            
            val success = volumeManager.decreaseVolume(streamType, amount, showUI)
            
            if (success) {
                val currentVolume = volumeManager.getCurrentVolumePercentage(streamType)
                ActionExecutor.sendNotification(
                    context,
                    "📉 Volume Decreased",
                    "${streamType.replaceFirstChar { it.uppercaseChar() }} volume decreased by $amount% (now $currentVolume%)",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error decreasing volume", e)
            false
        }
    }
    
    private fun muteVolume(context: Context, action: Action): Boolean {
        return try {
            val volumeManager = VolumeManager(context)
            val streamType = action.value ?: "media" // Default to media
            
            val success = volumeManager.setMute(streamType, true)
            
            if (success) {
                ActionExecutor.sendNotification(
                    context,
                    "🔇 Volume Muted",
                    "${streamType.replaceFirstChar { it.uppercaseChar() }} volume muted",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error muting volume", e)
            false
        }
    }
    
    private fun unmuteVolume(context: Context, action: Action): Boolean {
        return try {
            val volumeManager = VolumeManager(context)
            val streamType = action.value ?: "media" // Default to media
            
            val success = volumeManager.setMute(streamType, false)
            
            if (success) {
                ActionExecutor.sendNotification(
                    context,
                    "🔊 Volume Unmuted",
                    "${streamType.replaceFirstChar { it.uppercaseChar() }} volume unmuted",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unmuting volume", e)
            false
        }
    }
    
    private fun setVolumeProfile(context: Context, action: Action): Boolean {
        return try {
            val volumeManager = VolumeManager(context)
            val profile = action.value ?: "medium"
            
            val success = volumeManager.setVolumeProfile(profile)
            
            if (success) {
                ActionExecutor.sendNotification(
                    context,
                    "🎵 Volume Profile Set",
                    "Volume profile '$profile' applied to all audio streams",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting volume profile", e)
            false
        }
    }
    
    private fun restoreVolumes(context: Context): Boolean {
        return try {
            val volumeManager = VolumeManager(context)
            val success = volumeManager.restoreAllVolumes()
            
            if (success) {
                ActionExecutor.sendNotification(
                    context,
                    "🔄 Volumes Restored",
                    "All audio stream volumes restored to previous levels",
                    "Normal"
                )
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error restoring volumes", e)
            false
        }
    }
}