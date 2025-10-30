package com.example.autoflow.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import kotlin.math.max
import kotlin.math.min

/**
 * BrightnessManager - Comprehensive brightness control for AutoFlow
 * Supports both system-wide and app-specific brightness control
 */
class BrightnessManager(private val context: Context) {

    companion object {
        private const val TAG = "BrightnessManager"
        private const val MIN_BRIGHTNESS = 0
        private const val MAX_BRIGHTNESS = 255
        private const val PREFS_NAME = "brightness_prefs"
        private const val KEY_PREVIOUS_BRIGHTNESS = "previous_brightness"
        private const val KEY_AUTO_BRIGHTNESS = "auto_brightness_enabled"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Set system brightness (requires WRITE_SETTINGS permission)
     */
    fun setSystemBrightness(brightness: Int): Boolean {
        return try {
            if (!canWriteSettings()) {
                Log.w(TAG, "WRITE_SETTINGS permission not granted")
                return false
            }

            val clampedBrightness = clampBrightness(brightness)
            
            // Store current brightness before changing
            storePreviousBrightness()
            
            // Set system brightness
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                clampedBrightness
            )
            
            // Disable auto brightness if enabled
            if (isAutoBrightnessEnabled()) {
                setAutoBrightness(false)
            }
            
            Log.d(TAG, "✅ System brightness set to $clampedBrightness")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set system brightness", e)
            false
        }
    }

    /**
     * Adjust brightness by percentage (0-100)
     */
    fun setBrightnessByPercentage(percentage: Int): Boolean {
        val brightness = ((percentage / 100.0) * MAX_BRIGHTNESS).toInt()
        return setSystemBrightness(brightness)
    }

    /**
     * Increase brightness by amount
     */
    fun increaseBrightness(amount: Int): Boolean {
        val currentBrightness = getCurrentBrightness()
        val newBrightness = min(currentBrightness + amount, MAX_BRIGHTNESS)
        return setSystemBrightness(newBrightness)
    }

    /**
     * Decrease brightness by amount
     */
    fun decreaseBrightness(amount: Int): Boolean {
        val currentBrightness = getCurrentBrightness()
        val newBrightness = max(currentBrightness - amount, MIN_BRIGHTNESS)
        return setSystemBrightness(newBrightness)
    }

    /**
     * Get current system brightness
     */
    fun getCurrentBrightness(): Int {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get current brightness", e)
            128 // Default middle value
        }
    }

    /**
     * Get current brightness as percentage
     */
    fun getCurrentBrightnessPercentage(): Int {
        val brightness = getCurrentBrightness()
        return ((brightness.toDouble() / MAX_BRIGHTNESS) * 100).toInt()
    }

    /**
     * Restore previous brightness
     */
    fun restorePreviousBrightness(): Boolean {
        val previousBrightness = prefs.getInt(KEY_PREVIOUS_BRIGHTNESS, -1)
        return if (previousBrightness != -1) {
            setSystemBrightness(previousBrightness)
        } else {
            Log.w(TAG, "⚠️ No previous brightness stored")
            false
        }
    }

    /**
     * Set app-specific brightness (for current activity only)
     */
    fun setAppBrightness(activity: Activity, brightness: Int): Boolean {
        return try {
            val clampedBrightness = clampBrightness(brightness)
            val normalizedBrightness = clampedBrightness / 255.0f
            
            val layoutParams = activity.window.attributes
            layoutParams.screenBrightness = normalizedBrightness
            activity.window.attributes = layoutParams
            
            Log.d(TAG, "✅ App brightness set to $clampedBrightness")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set app brightness", e)
            false
        }
    }

    /**
     * Smart brightness adjustment based on time
     */
    fun adjustBrightnessForTimeOfDay(): Boolean {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        
        val targetPercentage = when (currentHour) {
            in 6..8 -> 40      // Morning - gentle
            in 9..11 -> 70     // Late morning - bright
            in 12..17 -> 80    // Afternoon - brightest
            in 18..20 -> 60    // Evening - dimming
            in 21..22 -> 30    // Night - dim
            else -> 10         // Late night/early morning - very dim
        }
        
        Log.d(TAG, "⏰ Time-based brightness: $targetPercentage% for hour $currentHour")
        return setBrightnessByPercentage(targetPercentage)
    }

    /**
     * Adaptive brightness for different environments
     */
    fun setBrightnessForEnvironment(environment: String): Boolean {
        val percentage = when (environment.lowercase()) {
            "outdoor", "sunny" -> 100
            "office", "work" -> 75
            "home", "indoor" -> 60
            "evening", "restaurant" -> 40
            "night", "bedroom" -> 20
            "cinema", "theater" -> 5
            else -> 50 // Default
        }
        
        Log.d(TAG, "🌍 Environment-based brightness: $percentage% for $environment")
        return setBrightnessByPercentage(percentage)
    }

    /**
     * Set brightness with predefined levels
     */
    fun setBrightnessLevel(level: String): Boolean {
        val percentage = when (level.lowercase()) {
            "minimum", "min" -> 1
            "very_low" -> 10
            "low" -> 25
            "medium_low" -> 40
            "medium" -> 50
            "medium_high" -> 65
            "high" -> 80
            "very_high" -> 95
            "maximum", "max" -> 100
            else -> 50 // Default medium
        }
        
        Log.d(TAG, "📊 Brightness level '$level' -> $percentage%")
        return setBrightnessByPercentage(percentage)
    }

    /**
     * Check if WRITE_SETTINGS permission is granted
     */
    fun canWriteSettings(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
    }

    /**
     * Request WRITE_SETTINGS permission
     */
    fun requestWriteSettingsPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            activity.startActivity(intent)
        }
    }

    // Private helper methods
    
    private fun clampBrightness(brightness: Int): Int {
        return max(MIN_BRIGHTNESS, min(brightness, MAX_BRIGHTNESS))
    }

    private fun storePreviousBrightness() {
        val currentBrightness = getCurrentBrightness()
        prefs.edit().putInt(KEY_PREVIOUS_BRIGHTNESS, currentBrightness).apply()
    }

    private fun isAutoBrightnessEnabled(): Boolean {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } catch (e: Exception) {
            false
        }
    }

    private fun setAutoBrightness(enabled: Boolean) {
        try {
            val mode = if (enabled) {
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            } else {
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            }
            
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                mode
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set auto brightness mode", e)
        }
    }
}