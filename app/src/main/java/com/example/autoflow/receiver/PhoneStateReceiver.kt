package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import com.example.autoflow.util.AutoReplyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced broadcast receiver to detect incoming phone calls
 * Handles cases where phone number might be null initially
 */
class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PhoneStateReceiver"
    }

    // ✅ FIXED: Use SupervisorJob for proper lifecycle management
    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            Log.d(TAG, "📞 Phone state changed: $state, Number: $phoneNumber")

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    Log.d(TAG, "📱 Incoming call from: $phoneNumber")
                    // Call is ringing - we can prepare for potential auto-reply
                }

                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Call ended - check if we need to auto-reply
                    phoneNumber?.let { number ->
                        handleCallEnded(context, number)
                        Log.d(TAG, "📵 Call ended, checking auto-reply conditions for: $number")
                    }
                }

                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    Log.d(TAG, "📞 Call answered")
                    // Call was answered - no auto-reply needed
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in PhoneStateReceiver", e)
        }
    }

    private fun handleAnyIncomingCall(context: Context, phoneNumber: String?) {
        Log.d(TAG, "🔥 handleAnyIncomingCall called")
        Log.d(TAG, "   📱 Number: ${phoneNumber ?: "Unknown/Private/Company"}")

        try {
            val autoReplyManager = AutoReplyManager.getInstance(context)

            when {
                // Case 1: Known phone number
                !phoneNumber.isNullOrBlank() -> {
                    Log.d(TAG, "📞 Known caller: $phoneNumber")
                    autoReplyManager.handleIncomingCall(phoneNumber)
                }

                // Case 2: Unknown/Private/Company number
                else -> {
                    Log.d(TAG, "📞 Unknown/Private caller - triggering universal auto-reply")
                    autoReplyManager.handleUnknownIncomingCall()
                }
            }

            Log.d(TAG, "✅ Auto-reply manager notified of incoming call")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling incoming call", e)
        }
    }

    private fun handleIncomingCall(context: Context, phoneNumber: String) {
        Log.d(TAG, "🔥 handleIncomingCall called with number: $phoneNumber")

        try {
            val autoReplyManager = AutoReplyManager.getInstance(context)
            autoReplyManager.handleIncomingCall(phoneNumber)
            Log.d(TAG, "✅ Auto-reply manager notified of incoming call")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling incoming call", e)
        }
    }

    /**
     * ✅ FIXED: Handle calls when phone number is unknown/null with proper scope
     */
    private fun handleIncomingCallWithUnknownNumber(context: Context) {
        Log.d(TAG, "🔥 handleIncomingCallWithUnknownNumber called")

        try {
            // Check if auto-reply should be triggered (regardless of number)
            val autoReplyManager = AutoReplyManager.getInstance(context)

            // Use a placeholder number or "Unknown" caller
            val unknownNumber = "Unknown"

            // ✅ FIXED: Use receiverScope instead of GlobalScope
            receiverScope.launch {
                try {
                    // Add a small delay to see if number becomes available
                    delay(500) // Wait 500ms

                    Log.d(TAG, "📱 Triggering auto-reply for unknown caller")
                    autoReplyManager.handleIncomingCallFromUnknown(context)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error handling unknown caller", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling unknown caller", e)
        }
    }

    // ✅ FIXED: Use receiverScope instead of coroutineScope
    private fun handleCallEnded(context: Context, phoneNumber: String) {
        Log.d(TAG, "📵 Call ended, checking auto-reply conditions for: $phoneNumber")

        receiverScope.launch {
            try {
                val autoReplyManager = AutoReplyManager.getInstance(context)

                if (autoReplyManager.shouldAutoReply()) {
                    Log.d(TAG, "✅ Auto-reply conditions met - sending SMS")
                    autoReplyManager.handleMissedCall(phoneNumber)
                } else {
                    Log.d(TAG, "⏭️ Auto-reply conditions not met, skipping")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling auto-reply", e)
            }
        }
    }

    private fun checkAndSendAutoReply(context: Context, phoneNumber: String) {
        val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
        val autoReplyEnabled = prefs.getBoolean("auto_reply_enabled", false)
        val meetingMode = prefs.getBoolean("manual_meeting_mode", false)
        val message = prefs.getString("auto_reply_message", "I'm currently in a meeting and will get back to you soon.") ?: ""

        Log.d(TAG, "🔍 Direct check - auto_reply_enabled: $autoReplyEnabled")
        Log.d(TAG, "🔍 Direct check - manual_meeting_mode: $meetingMode")
        Log.d(TAG, "🔍 Direct check - message: $message")

        if (autoReplyEnabled && meetingMode && phoneNumber.isNotEmpty()) {
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Log.d("PhoneStateReceiver", "📩 Auto-reply SMS sent to: $phoneNumber")
            } catch (e: Exception) {
                Log.e("PhoneStateReceiver", "❌ Failed to send SMS: ${e.message}")
            }
        }
    }

    /**
     * ✅ NEW: Cleanup method to cancel coroutines when receiver is destroyed
     * Call this in your app's cleanup logic if needed
     */
    fun cleanup() {
        try {
            receiverScope.cancel()
            Log.d(TAG, "✅ PhoneStateReceiver scope cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cleaning up receiver scope", e)
        }
    }
}
