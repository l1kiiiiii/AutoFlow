package com.example.autoflow.integrations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import android.os.Build
import com.example.autoflow.util.AutoReplyManager
import java.util.concurrent.Executor

class PhoneStateManager private constructor(private val context: Context) {

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var isListening = false

    // ✅ ADD AUTO-REPLY MESSAGE STORAGE
    private var autoReplyMessage: String = "I'm currently in a meeting and will get back to you shortly."

    companion object {
        private const val TAG = "PhoneStateManager"
        @Volatile
        private var INSTANCE: PhoneStateManager? = null

        fun getInstance(context: Context): PhoneStateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PhoneStateManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ✅ ADD MISSING setAutoReplyMessage METHOD
    fun setAutoReplyMessage(message: String) {
        this.autoReplyMessage = message
        Log.d(TAG, "📱 Auto-reply message updated: $message")

        // Save to preferences for persistence
        val prefs = context.getSharedPreferences("phone_state_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("auto_reply_message", message).apply()
    }

    // ✅ ADD getAutoReplyMessage METHOD
    fun getAutoReplyMessage(): String {
        // Load from preferences if available
        val prefs = context.getSharedPreferences("phone_state_prefs", Context.MODE_PRIVATE)
        return prefs.getString("auto_reply_message", autoReplyMessage) ?: autoReplyMessage
    }

    fun startListening() {
        if (isListening) return

        if (!hasRequiredPermissions()) {
            Log.w(TAG, "Missing required permissions for phone state monitoring")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startModernListening()
            } else {
                startLegacyListening()
            }
            isListening = true
            Log.d(TAG, "📱 Phone state monitoring started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting phone listener", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting phone listener", e)
        }
    }

    fun stopListening() {
        if (!isListening) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let { callback ->
                    telephonyManager.unregisterTelephonyCallback(callback)
                    telephonyCallback = null
                }
            } else {
                phoneStateListener?.let { listener ->
                    @Suppress("DEPRECATION")
                    telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
                    phoneStateListener = null
                }
            }
            isListening = false
            Log.d(TAG, "📱 Phone state monitoring stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping phone listener", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun startLegacyListening() {
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                handleCallStateChange(state, phoneNumber)
            }
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun startModernListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    // Note: Phone number is not available in modern API for privacy
                    handleCallStateChange(state, null)
                }
            }
            val executor: Executor = context.mainExecutor
            telephonyManager.registerTelephonyCallback(executor, telephonyCallback!!)
        }
    }

    private fun handleCallStateChange(state: Int, phoneNumber: String?) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.d(TAG, "📞 Incoming call detected")
                onIncomingCall(phoneNumber)
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.d(TAG, "☎️ Call ended")
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.d(TAG, "📞 Call answered")
            }
        }
    }

    private fun onIncomingCall(phoneNumber: String?) {
        try {
            // Check if auto-reply should be triggered
            val autoReplyManager = AutoReplyManager.getInstance(context)
            autoReplyManager.handleIncomingCall(phoneNumber)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming call", e)
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun cleanup() {
        stopListening()
        INSTANCE = null
    }
}
