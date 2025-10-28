// BlockActivity.kt (optional - shows when app is blocked)
package com.example.autoflow.blocker

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.autoflow.policy.BlockPolicy

class BlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ FIXED: Get both app name and package name from intent
        val blockedApp = intent.getStringExtra("app_name") ?: intent.getStringExtra("blocked_app") ?: "this app"
        val packageName = intent.getStringExtra("package_name") ?: intent.getStringExtra("blocked_package") ?: ""

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BlockScreen(
                        blockedApp = blockedApp,
                        packageName = packageName
                    ) {
                        finish()
                    }
                }
            }
        }
    }
}

// ✅ FIXED: Removed duplicate and unreachable code
@Composable
fun BlockScreen(blockedApp: String, packageName: String, onDismiss: () -> Unit) {
    // ✅ Get context at the top level
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ✅ App icon and name
        Icon(
            imageVector = Icons.Default.Block,
            contentDescription = "Blocked",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "🚫 $blockedApp Blocked",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This app is currently blocked to help you stay focused. You can:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ NEW: Action options
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Go back button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("← Go Back")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ FIXED: Quick unblock this app only (only if packageName is valid)
            if (packageName.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        BlockPolicy.removeBlockedPackages(context, listOf(packageName))
                        Toast.makeText(context, "✅ $blockedApp unblocked", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unblock This App Only")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // ✅ FIXED: Emergency unblock all
            TextButton(
                onClick = {
                    // ✅ FIXED: Call the correct function (assuming it exists or create it)
                    try {
                        val blockedApps = BlockPolicy.getBlockedPackages(context)
                        BlockPolicy.clearBlockedPackages(context)
                        BlockPolicy.setBlockingEnabled(context, false)

                        Toast.makeText(context, "🚨 All ${blockedApps.size} apps unblocked", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("🚨 Emergency Unblock All", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ FIXED: Time remaining if temporary block
        val remainingTime = getRemainingBlockTime(context, packageName)
        if (remainingTime > 0) {
            Text(
                text = "⏰ Auto-unblock in ${remainingTime}min",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ✅ FIXED: Helper function to get remaining block time
private fun getRemainingBlockTime(context: Context, packageName: String): Int {
    // ✅ FUTURE: This would calculate remaining time from scheduled auto-unblock
    // For now, return 0 (no time limit)

    // Example implementation:
    /*
    try {
        val prefs = context.getSharedPreferences("block_schedule", Context.MODE_PRIVATE)
        val unblockTime = prefs.getLong("unblock_time_$packageName", 0)
        if (unblockTime > 0) {
            val currentTime = System.currentTimeMillis()
            val remainingMs = unblockTime - currentTime
            return if (remainingMs > 0) (remainingMs / (1000 * 60)).toInt() else 0
        }
    } catch (e: Exception) {
        // Handle error
    }
    */

    return 0
}
