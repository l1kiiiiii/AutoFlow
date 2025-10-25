package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.autoflow.util.AutoReplyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

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
                        Log.d(TAG, "📵 Call ended, checking auto-reply conditions for: $number")

                        coroutineScope.launch {
                            val autoReplyManager = AutoReplyManager.getInstance(context)

                            // ✅ Only auto-reply if in meeting mode context
                            if (autoReplyManager.shouldAutoReply()) {
                                Log.d(TAG, "✅ Meeting mode active, sending auto-reply")
                                autoReplyManager.handleMissedCall(number)
                            } else {
                                Log.d(TAG, "⏭️ Not in meeting mode, skipping auto-reply")
                            }
                        }
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
     * ✅ NEW: Handle calls when phone number is unknown/null
     * This will trigger auto-reply even without knowing the caller's number
     */
    private fun handleIncomingCallWithUnknownNumber(context: Context) {
        Log.d(TAG, "🔥 handleIncomingCallWithUnknownNumber called")

        try {
            // Check if auto-reply should be triggered (regardless of number)
            val autoReplyManager = AutoReplyManager.getInstance(context)

            // Use a placeholder number or "Unknown" caller
            val unknownNumber = "Unknown"

            // First check if we should send auto-reply (DND mode, etc.)
            CoroutineScope(Dispatchers.IO).launch {
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
}
