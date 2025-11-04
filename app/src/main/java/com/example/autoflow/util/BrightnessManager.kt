package com.example.autoflow.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min

/**
 * ✅ COMPLETE Enhanced BrightnessManager - Comprehensive brightness control for AutoFlow
 * Supports both system-wide and app-specific brightness control with scheduled overrides
 * Fixed brightness amount across scheduled workflows functionality
 */
class BrightnessManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BrightnessManager"
        private const val MIN_BRIGHTNESS = 0
        private const val MAX_BRIGHTNESS = 255
        private const val PREFS_NAME = "brightness_prefs"

        // Existing preference keys
        private const val KEY_PREVIOUS_BRIGHTNESS = "previous_brightness"
        private const val KEY_AUTO_BRIGHTNESS = "auto_brightness_enabled"

        // ✅ NEW: Fixed brightness override keys for scheduled workflows
        private const val KEY_FIXED_BRIGHTNESS_MODE = "fixed_brightness_mode"
        private const val KEY_FIXED_BRIGHTNESS_VALUE = "fixed_brightness_value"
        private const val KEY_BRIGHTNESS_OVERRIDE_ACTIVE = "brightness_override_active"
        private const val KEY_SCHEDULED_BRIGHTNESS_PROFILE = "scheduled_brightness_profile"
        private const val KEY_IGNORE_TIME_ADJUSTMENTS = "ignore_time_adjustments"

        @Volatile
        private var INSTANCE: BrightnessManager? = null

        fun getInstance(context: Context): BrightnessManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BrightnessManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * ✅ Set system brightness with coroutine support (requires WRITE_SETTINGS permission)
     */
    suspend fun setSystemBrightness(
        brightness: Int,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!canWriteSettings()) {
                Log.w(TAG, "WRITE_SETTINGS permission not granted")
                return@withContext false
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
     * ✅ NEW: Set fixed brightness override for scheduled workflows
     * This prevents time-based or automatic brightness adjustments
     */
    suspend fun setFixedBrightnessForScheduled(
        percentage: Int,
        ignoreTimeBasedAdjustment: Boolean = true,
        profile: String = "default",
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val clampedPercentage = max(1, min(percentage, 100))

            // Store fixed brightness settings
            prefs.edit()
                .putBoolean(KEY_FIXED_BRIGHTNESS_MODE, true)
                .putInt(KEY_FIXED_BRIGHTNESS_VALUE, clampedPercentage)
                .putBoolean(KEY_BRIGHTNESS_OVERRIDE_ACTIVE, ignoreTimeBasedAdjustment)
                .putString(KEY_SCHEDULED_BRIGHTNESS_PROFILE, profile)
                .putBoolean(KEY_IGNORE_TIME_ADJUSTMENTS, ignoreTimeBasedAdjustment)
                .apply()

            // Set the actual brightness
            val success = setBrightnessByPercentage(clampedPercentage, scope)

            if (success) {
                Log.d(TAG, "🔒 Fixed brightness set to $clampedPercentage% for scheduled workflow (profile: $profile)")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set fixed brightness for scheduled workflow", e)
            false
        }
    }

    /**
     * ✅ Adjust brightness by percentage (0-100) with coroutine support
     */
    suspend fun setBrightnessByPercentage(
        percentage: Int,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.IO) {
        // Check if override is active and should be maintained
        if (isBrightnessOverrideActive() && shouldIgnoreTimeAdjustments()) {
            val fixedPercentage = prefs.getInt(KEY_FIXED_BRIGHTNESS_VALUE, percentage)
            Log.d(TAG, "🔒 Using fixed brightness override: $fixedPercentage%")
            val brightness = ((fixedPercentage / 100.0) * MAX_BRIGHTNESS).toInt()
            return@withContext setSystemBrightness(brightness, scope)
        }

        val brightness = ((percentage / 100.0) * MAX_BRIGHTNESS).toInt()
        return@withContext setSystemBrightness(brightness, scope)
    }

    /**
     * ✅ Increase brightness by amount with coroutine support
     */
    suspend fun increaseBrightness(
        amount: Int,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.IO) {
        val currentBrightness = getCurrentBrightness()
        val newBrightness = min(currentBrightness + amount, MAX_BRIGHTNESS)
        return@withContext setSystemBrightness(newBrightness, scope)
    }

    /**
     * ✅ Decrease brightness by amount with coroutine support
     */
    suspend fun decreaseBrightness(
        amount: Int,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.IO) {
        val currentBrightness = getCurrentBrightness()
        val newBrightness = max(currentBrightness - amount, MIN_BRIGHTNESS)
        return@withContext setSystemBrightness(newBrightness, scope)
    }

    /**
     * ✅ Get current system brightness
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
     * ✅ Get current brightness as percentage
     */
    fun getCurrentBrightnessPercentage(): Int {
        val brightness = getCurrentBrightness()
        return ((brightness.toDouble() / MAX_BRIGHTNESS) * 100).toInt()
    }

    /**
     * ✅ Restore previous brightness with coroutine support
     */
    suspend fun restorePreviousBrightness(scope: CoroutineScope): Boolean = withContext(Dispatchers.IO) {
        val previousBrightness = prefs.getInt(KEY_PREVIOUS_BRIGHTNESS, -1)
        return@withContext if (previousBrightness != -1) {
            // Clear any brightness override when restoring
            clearBrightnessOverride(scope)
            setSystemBrightness(previousBrightness, scope)
        } else {
            Log.w(TAG, "⚠️ No previous brightness stored")
            false
        }
    }

    /**
     * ✅ Set app-specific brightness (for current activity only) with coroutine support
     */
    suspend fun setAppBrightness(
        activity: Activity,
        brightness: Int,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.Main) {
        try {
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
     * ✅ ENHANCED: Smart brightness adjustment based on time with override protection
     */
    suspend fun adjustBrightnessForTimeOfDay(scope: CoroutineScope): Boolean = withContext(Dispatchers.IO) {
        // Check if we should ignore time-based adjustments
        if (shouldIgnoreTimeAdjustments()) {
            Log.d(TAG, "⏰ Skipping time-based brightness adjustment (override active)")
            return@withContext false
        }

        val currentHour = LocalDateTime.now().hour

        val targetPercentage = when (currentHour) {
            in 6..8 -> 40      // Morning - gentle
            in 9..11 -> 70     // Late morning - bright
            in 12..17 -> 80    // Afternoon - brightest
            in 18..20 -> 60    // Evening - dimming
            in 21..22 -> 30    // Night - dim
            else -> 10         // Late night/early morning - very dim
        }

        Log.d(TAG, "⏰ Time-based brightness: $targetPercentage% for hour $currentHour")
        return@withContext setBrightnessByPercentage(targetPercentage, scope)
    }

    /**
     * ✅ Adaptive brightness for different environments with override protection
     */
    suspend fun setBrightnessForEnvironment(
        environment: String,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.IO) {
        // Check if we should ignore environment adjustments during scheduled workflows
        if (isBrightnessOverrideActive()) {
            Log.d(TAG, "🌍 Using fixed brightness instead of environment adjustment (override active)")
            val fixedPercentage = prefs.getInt(KEY_FIXED_BRIGHTNESS_VALUE, 50)
            return@withContext setBrightnessByPercentage(fixedPercentage, scope)
        }

        val percentage = when (environment.lowercase()) {
            "outdoor", "sunny" -> 100
            "office", "work" -> 75
            "home", "indoor" -> 60
            "evening", "restaurant" -> 40
            "night", "bedroom" -> 20
            "cinema", "theater" -> 5
            "meeting", "conference" -> 65
            "car", "driving" -> 85
            else -> 50 // Default
        }

        Log.d(TAG, "🌍 Environment-based brightness: $percentage% for $environment")
        return@withContext setBrightnessByPercentage(percentage, scope)
    }

    /**
     * ✅ Set brightness with predefined levels
     */
    suspend fun setBrightnessLevel(
        level: String,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.IO) {
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
        return@withContext setBrightnessByPercentage(percentage, scope)
    }

    /**
     * ✅ NEW: Set brightness profile for specific scenarios
     */
    suspend fun setBrightnessProfile(
        profile: String,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.IO) {
        val percentage = when (profile.lowercase()) {
            "sleep", "night_mode" -> 5
            "reading" -> 30
            "work", "office" -> 65
            "gaming" -> 80
            "outdoor" -> 95
            "movie", "video" -> 45
            "meeting" -> 70
            "presentation" -> 85
            "energy_save" -> 25
            "auto" -> {
                // Use time-based adjustment
                return@withContext adjustBrightnessForTimeOfDay(scope)
            }
            "scheduled_fixed" -> {
                // Use fixed value from preferences
                prefs.getInt(KEY_FIXED_BRIGHTNESS_VALUE, 50)
            }
            else -> {
                Log.w(TAG, "Unknown brightness profile: $profile")
                return@withContext false
            }
        }

        Log.d(TAG, "📋 Brightness profile '$profile' -> $percentage%")
        return@withContext setBrightnessByPercentage(percentage, scope)
    }

    /**
     * ✅ NEW: Check if brightness override is active
     */
    fun isBrightnessOverrideActive(): Boolean {
        return prefs.getBoolean(KEY_BRIGHTNESS_OVERRIDE_ACTIVE, false)
    }

    /**
     * ✅ NEW: Check if time-based adjustments should be ignored
     */
    fun shouldIgnoreTimeAdjustments(): Boolean {
        return prefs.getBoolean(KEY_IGNORE_TIME_ADJUSTMENTS, false)
    }

    /**
     * ✅ NEW: Clear brightness override
     */
    suspend fun clearBrightnessOverride(scope: CoroutineScope): Boolean = withContext(Dispatchers.IO) {
        try {
            prefs.edit()
                .putBoolean(KEY_FIXED_BRIGHTNESS_MODE, false)
                .putBoolean(KEY_BRIGHTNESS_OVERRIDE_ACTIVE, false)
                .putBoolean(KEY_IGNORE_TIME_ADJUSTMENTS, false)
                .remove(KEY_FIXED_BRIGHTNESS_VALUE)
                .remove(KEY_SCHEDULED_BRIGHTNESS_PROFILE)
                .apply()

            Log.d(TAG, "✅ Brightness override cleared")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to clear brightness override", e)
            false
        }
    }

    /**
     * ✅ FIXED: Get brightness override info with proper return type
     */
    fun getBrightnessOverrideInfo(): Map<String, Any> {
        return mapOf(
            "isActive" to isBrightnessOverrideActive(),
            "fixedValue" to prefs.getInt(KEY_FIXED_BRIGHTNESS_VALUE, -1),
            "profile" to (prefs.getString(KEY_SCHEDULED_BRIGHTNESS_PROFILE, "none") ?: "none"),
            "ignoreTimeAdjustments" to shouldIgnoreTimeAdjustments()
        )
    }

    /**
     * ✅ Check if WRITE_SETTINGS permission is granted
     */
    fun canWriteSettings(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
    }

    /**
     * ✅ Request WRITE_SETTINGS permission with coroutine support
     */
    suspend fun requestWriteSettingsPermission(
        activity: Activity,
        scope: CoroutineScope
    ): Unit = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            activity.startActivity(intent)
        }
    }

    /**
     * ✅ NEW: Adaptive brightness based on battery level
     */
    suspend fun adjustBrightnessForBattery(
        batteryLevel: Int,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.IO) {
        if (shouldIgnoreTimeAdjustments()) {
            Log.d(TAG, "🔋 Skipping battery-based brightness adjustment (override active)")
            return@withContext false
        }

        val percentage = when {
            batteryLevel <= 10 -> 20  // Very low battery - save power
            batteryLevel <= 25 -> 40  // Low battery - reduce brightness
            batteryLevel <= 50 -> 60  // Medium battery - moderate brightness
            else -> 80               // Good battery - normal brightness
        }

        Log.d(TAG, "🔋 Battery-based brightness: $percentage% (battery: $batteryLevel%)")
        return@withContext setBrightnessByPercentage(percentage, scope)
    }

    // ✅ PRIVATE HELPER METHODS

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
