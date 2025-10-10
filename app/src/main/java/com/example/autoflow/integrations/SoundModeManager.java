package com.example.autoflow.integrations;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public class SoundModeManager {
    private static final String TAG = "SoundModeManager";
    private final Context context;
    private final AudioManager audioManager;
    private final NotificationManager notificationManager;

    public SoundModeManager(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Set sound mode
     * @param mode "Normal", "Silent", "Vibrate", or "DND"
     * @return true if successful, false otherwise
     */
    public boolean setSoundMode(String mode) {
        Log.d(TAG, "Setting sound mode to: " + mode);

        if (audioManager == null) {
            Log.e(TAG, "AudioManager is null");
            return false;
        }

        try {
            switch (mode) {
                case "Normal":
                    return setNormalMode();
                case "Silent":
                    return setSilentMode();
                case "Vibrate":
                    return setVibrateMode();
                case "DND":
                    return setDNDMode();
                default:
                    Log.w(TAG, "Unknown mode: " + mode);
                    return false;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage(), e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error setting sound mode: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Set Normal mode (ringer on, vibrate optional)
     */
    private boolean setNormalMode() {
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        Log.d(TAG, "✅ Set to Normal mode");
        return true;
    }

    /**
     * Set Silent mode (no sound, no vibrate)
     */
    private boolean setSilentMode() {
        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        Log.d(TAG, "✅ Set to Silent mode");
        return true;
    }

    /**
     * Set Vibrate mode (no sound, vibrate only)
     */
    private boolean setVibrateMode() {
        audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        Log.d(TAG, "✅ Set to Vibrate mode");
        return true;
    }

    /**
     * Set Do Not Disturb mode
     * Requires INTERRUPTION_FILTER permission (granted automatically on most devices)
     */
    private boolean setDNDMode() {
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager is null");
            return false;
        }

        // Check if app has DND permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                Log.w(TAG, "DND access not granted - opening settings");
                openDNDSettings();
                return false;
            }
        }

        // Set DND mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
            );
            Log.d(TAG, "✅ Set to DND mode");
            return true;
        } else {
            // Fallback to silent mode for older devices
            Log.w(TAG, "DND not available on this device, using Silent mode");
            return setSilentMode();
        }
    }

    /**
     * Get current sound mode
     */
    public String getCurrentMode() {
        if (audioManager == null) return "Unknown";

        int ringerMode = audioManager.getRingerMode();
        switch (ringerMode) {
            case AudioManager.RINGER_MODE_NORMAL:
                return "Normal";
            case AudioManager.RINGER_MODE_SILENT:
                return "Silent";
            case AudioManager.RINGER_MODE_VIBRATE:
                return "Vibrate";
            default:
                return "Unknown";
        }
    }

    /**
     * Check if DND is enabled
     */
    public boolean isDNDEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager != null) {
                int filter = notificationManager.getCurrentInterruptionFilter();
                return filter != NotificationManager.INTERRUPTION_FILTER_ALL;
            }
        }
        return false;
    }

    /**
     * Check if DND permission is granted
     */
    public boolean hasDNDPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return notificationManager != null &&
                    notificationManager.isNotificationPolicyAccessGranted();
        }
        return true; // Not needed on older versions
    }

    /**
     * Open DND settings for user to grant permission
     */
    public void openDNDSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleanup called");
        // Nothing to clean up for now
    }
}
