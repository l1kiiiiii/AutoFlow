package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.util.MeetingModeManager
import kotlinx.coroutines.*

class MeetingModeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MeetingModeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📡 Received meeting mode intent: ${intent.action}")

        val meetingModeId = intent.getLongExtra("meeting_mode_id", -1L)
        if (meetingModeId == -1L) {
            Log.e(TAG, "❌ Invalid meeting mode ID")
            return
        }

        val meetingModeManager = MeetingModeManager.getInstance(context)

        when (intent.action) {
            "START_MEETING_MODE" -> {
                Log.d(TAG, "🚀 Starting scheduled meeting mode: $meetingModeId")
                // Start the scheduled meeting mode
                // Implementation will trigger the meeting mode start
            }

            "END_MEETING_MODE" -> {
                Log.d(TAG, "🛑 Ending meeting mode: $meetingModeId")
                meetingModeManager.stopMeetingMode { success, message ->
                    if (success) {
                        Log.d(TAG, "✅ Meeting mode ended: $message")
                    } else {
                        Log.e(TAG, "❌ Failed to end meeting mode: $message")
                    }
                }
            }

            "CHECK_EXPIRED" -> {
                Log.d(TAG, "⏰ Checking for expired meeting modes")
                meetingModeManager.checkExpiredMeetingModes()
            }
        }
    }
}
