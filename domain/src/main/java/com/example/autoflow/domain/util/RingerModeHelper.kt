package com.example.autoflow.domain.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object RingerModeHelper {

    fun setSoundMode(context: Context, mode: String) {
        when (mode) {
            "ring" -> setRinger(context, AudioManager.RINGER_MODE_NORMAL)
            "vibrate" -> setRinger(context, AudioManager.RINGER_MODE_VIBRATE)
            "silent" -> setRinger(context, AudioManager.RINGER_MODE_SILENT)
            "dnd_none" -> setDnd(context, NotificationManager.INTERRUPTION_FILTER_NONE)
            "dnd_priority" -> setDnd(context, NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            "dnd_alarms" -> {
                // Alarms-only is exposed as ALARMS on newer Android; fallback to PRIORITY if needed
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= 28) {
                    setDnd(context, NotificationManager.INTERRUPTION_FILTER_ALARMS)
                } else {
                    setDnd(context, NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                }
            }
            "dnd_all" -> setDnd(context, NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }

    private fun setRinger(context: Context, ringerMode: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            am.ringerMode = ringerMode
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to change ringer mode: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("InlinedApi")
    private fun setDnd(context: Context, filter: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            // Do NOT try to open settings from here; return and let UI prompt in foreground
            return
        }
        // Minimal “quiet” policy; adjust categories as needed
        val policy = NotificationManager.Policy(
            NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS,
            NotificationManager.Policy.PRIORITY_SENDERS_ANY,
            NotificationManager.Policy.PRIORITY_SENDERS_ANY,
            NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK or
                    NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT or
                    NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS or
                    NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR or
                    NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST or
                    NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT,
            NotificationManager.Policy.CONVERSATION_SENDERS_IMPORTANT
        )
        nm.setNotificationPolicy(policy)
        nm.setInterruptionFilter(filter) // PRIORITY or ALARMS recommended for “silent but vibrate”
    }

    fun promptForDndAccess(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
