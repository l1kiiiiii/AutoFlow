// AppBlockAccessibilityService.kt (intercepts app launches)
package com.example.autoflow.blocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.util.Log
import com.example.autoflow.policy.BlockPolicy

class AppBlockAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip if blocking is disabled or this is our own app
        if (!BlockPolicy.isBlockingEnabled(this)) return
        if (packageName == applicationContext.packageName) return

        val blockedPackages = BlockPolicy.getBlockedPackages(this)
        if (packageName in blockedPackages) {
            Log.i("AppBlockService", "🚫 Blocking app: $packageName")

            // Option 1: Send user to home screen
            performGlobalAction(GLOBAL_ACTION_HOME)

            // Option 2: Show block screen (uncomment to use instead)
            // val blockIntent = Intent(this, BlockActivity::class.java)
            //     .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            //     .putExtra("blocked_app", packageName)
            // startActivity(blockIntent)
        }
    }

    override fun onInterrupt() {
        Log.d("AppBlockService", "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("AppBlockService", "App blocking service connected")
    }
}
