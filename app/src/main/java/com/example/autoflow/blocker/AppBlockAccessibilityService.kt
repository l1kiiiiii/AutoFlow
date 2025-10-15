package com.example.autoflow.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.autoflow.policy.BlockPolicy

class AppBlockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppBlockService"
        private const val THROTTLE_INTERVAL_MS = 1000L
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Only handle window state changes (app launches)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Ignore system UI and our own app
        if (packageName == "android" ||
            packageName == "com.android.systemui" ||
            packageName == this.packageName) {
            return
        }

        Log.d(TAG, "📱 App launched: $packageName")

        // ✅ FIXED: Use getBlockedPackages() and check if blocking is enabled
        if (BlockPolicy.isBlockingEnabled(this)) {
            val blockedPackages = BlockPolicy.getBlockedPackages(this)

            if (blockedPackages.contains(packageName)) {
                Log.w(TAG, "🚫 BLOCKING APP: $packageName")

                // Launch block screen
                val intent = Intent(this, BlockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    putExtra("blocked_package", packageName)
                }

                startActivity(intent)

                // Go back to previous app
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "✅ AppBlockAccessibilityService connected")

        // Log current blocking status
        val isEnabled = BlockPolicy.isBlockingEnabled(this)
        val blockedApps = BlockPolicy.getBlockedPackages(this)
        Log.d(TAG, "Blocking enabled: $isEnabled")
        Log.d(TAG, "Blocked apps: ${blockedApps.size} apps")
        blockedApps.forEach { pkg ->
            Log.d(TAG, "  - $pkg")
        }
    }
}
