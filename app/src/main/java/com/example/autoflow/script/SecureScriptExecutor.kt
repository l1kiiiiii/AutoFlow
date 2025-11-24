package com.example.autoflow.script

import android.content.Context
import android.util.Log
import org.mozilla.javascript.*
import java.util.concurrent.*

/**
 * Secure wrapper around ScriptExecutor with permission checking and sandboxing
 * Extends existing ScriptExecutor with minimal changes
 */
class SecureScriptExecutor(
    private val context: Context,
    private val securityPolicy: ScriptSecurityPolicy = ScriptSecurityPolicy.createRestrictivePolicy()
) {

    private val validator = ScriptValidator()
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    companion object {
        private const val TAG = "SecureScriptExecutor"
    }

    /**
     * Execute script with security checks
     */
    fun executeSecure(
        scriptCode: String,
        permissions: Set<ScriptPermission> = ScriptPermission.getDefaultPermissions()
    ): ScriptResult {

        // Step 1: Validate script
        val validationResult = validator.validate(scriptCode, securityPolicy)

        if (!validationResult.isValid) {
            val threatList = validationResult.threats.joinToString("\n") {
                "- ${it.description}"
            }
            Log.e(TAG, "Script validation failed:\n$threatList")
            return ScriptResult.Error("Security validation failed:\n$threatList")
        }

        // Log warnings if any
        validationResult.warnings.forEach {
            Log.w(TAG, "Script warning: $it")
        }

        // Step 2: Execute with timeout
        return try {
            val future = executorService.submit<ScriptResult> {
                executeWithPermissions(scriptCode, permissions)
            }

            // Enforce timeout
            future.get(securityPolicy.maxExecutionTimeMs, TimeUnit.MILLISECONDS)

        } catch (e: TimeoutException) {
            Log.e(TAG, "Script execution timeout")
            ScriptResult.Error("Script execution timeout (>${securityPolicy.maxExecutionTimeMs}ms)")
        } catch (e: ExecutionException) {
            Log.e(TAG, "Script execution error: ${e.cause?.message}")
            ScriptResult.Error("Execution error: ${e.cause?.message ?: e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}")
            ScriptResult.Error("Unexpected error: ${e.message}")
        }
    }

    /**
     * Execute script with permission-restricted context
     */
    private fun executeWithPermissions(
        scriptCode: String,
        permissions: Set<ScriptPermission>
    ): ScriptResult {
        return try {
            val rhinoContext = org.mozilla.javascript.Context.enter()
            rhinoContext.optimizationLevel = -1

            // Create restricted scope
            val scope = rhinoContext.initStandardObjects()

            // Add only permitted functions
            addPermittedFunctions(rhinoContext, scope, permissions)

            // Execute
            val result = rhinoContext.evaluateString(scope, scriptCode, "<secure-script>", 1, null)

            ScriptResult.Success(org.mozilla.javascript.Context.toString(result))

        } catch (e: RhinoException) {
            Log.e(TAG, "Rhino error: ${e.message}")
            ScriptResult.Error("Script error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Execution error: ${e.message}")
            ScriptResult.Error("Execution error: ${e.message}")
        } finally {
            org.mozilla.javascript.Context.exit()
        }
    }

    /**
     * Add only permitted utility functions based on granted permissions
     */
    private fun addPermittedFunctions(
        rhinoContext: org.mozilla.javascript.Context,
        scope: Scriptable,
        permissions: Set<ScriptPermission>
    ) {
        // Always safe: Log function (if permitted)
        if (permissions.contains(ScriptPermission.LOG_OUTPUT)) {
            val logFunction = object : BaseFunction() {
                override fun call(
                    cx: org.mozilla.javascript.Context?,
                    scope: Scriptable?,
                    thisObj: Scriptable?,
                    args: Array<out Any>?
                ): Any {
                    val message = args?.joinToString(" ") {
                        org.mozilla.javascript.Context.toString(it)
                    } ?: ""
                    Log.i("SecureUserScript", message)
                    return message
                }
            }
            ScriptableObject.putProperty(scope, "log", logFunction)
        }

        // Notification function (requires permission)
        if (permissions.contains(ScriptPermission.SEND_NOTIFICATION)) {
            val notifyFunction = object : BaseFunction() {
                override fun call(
                    cx: org.mozilla.javascript.Context?,
                    scope: Scriptable?,
                    thisObj: Scriptable?,
                    args: Array<out Any>?
                ): Any {
                    if (args != null && args.isNotEmpty()) {
                        val title = org.mozilla.javascript.Context.toString(args[0])
                        val message = if (args.size > 1) {
                            org.mozilla.javascript.Context.toString(args[1])
                        } else {
                            "Script Notification"
                        }

                        // Use existing NotificationHelper
                        com.example.autoflow.util.NotificationHelper.sendScriptNotification(
                            context, title, message
                        )
                        return "Notification sent: $title"
                    }
                    return "No message provided"
                }
            }
            ScriptableObject.putProperty(scope, "notify", notifyFunction)
        }

        // HTTP requests (requires permission and domain check)
        if (permissions.contains(ScriptPermission.HTTP_REQUEST)) {
            val httpFunction = object : BaseFunction() {
                override fun call(
                    cx: org.mozilla.javascript.Context?,
                    scope: Scriptable?,
                    thisObj: Scriptable?,
                    args: Array<out Any>?
                ): Any {
                    if (args != null && args.isNotEmpty()) {
                        val url = org.mozilla.javascript.Context.toString(args[0])

                        // Check allowed domains
                        if (!isUrlAllowed(url)) {
                            return "Error: Domain not in allowed list"
                        }

                        return makeSecureHttpRequest(url)
                    }
                    return "No URL provided"
                }
            }
            ScriptableObject.putProperty(scope, "httpGet", httpFunction)
        }

        // Time access (always safe if permitted)
        if (permissions.contains(ScriptPermission.ACCESS_TIME)) {
            val timeFunction = object : BaseFunction() {
                override fun call(
                    cx: org.mozilla.javascript.Context?,
                    scope: Scriptable?,
                    thisObj: Scriptable?,
                    args: Array<out Any>?
                ): Any {
                    return System.currentTimeMillis()
                }
            }
            ScriptableObject.putProperty(scope, "currentTime", timeFunction)
        }

        // System info (if permitted)
        if (permissions.contains(ScriptPermission.ACCESS_SYSTEM_INFO)) {
            val systemInfoFunction = object : BaseFunction() {
                override fun call(
                    cx: org.mozilla.javascript.Context?,
                    scope: Scriptable?,
                    thisObj: Scriptable?,
                    args: Array<out Any>?
                ): Any {
                    val info = mapOf(
                        "deviceModel" to android.os.Build.MODEL,
                        "androidVersion" to android.os.Build.VERSION.RELEASE,
                        "timestamp" to System.currentTimeMillis()
                    )
                    return info.toString()
                }
            }
            ScriptableObject.putProperty(scope, "getSystemInfo", systemInfoFunction)
        }
    }

    private fun isUrlAllowed(url: String): Boolean {
        if (securityPolicy.allowedDomains.isEmpty()) {
            return true // Allow all if no restrictions
        }

        return securityPolicy.allowedDomains.any { domain ->
            url.contains(domain, ignoreCase = true)
        }
    }

    private fun makeSecureHttpRequest(urlString: String): String {
        return try {
            val url = java.net.URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                connectTimeout = 5000 // 5 seconds
                readTimeout = 5000
                setRequestProperty("User-Agent", "AutoFlow-Secure/1.0")
            }

            val responseCode = connection.responseCode

            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                // Limit response size
                if (response.length > 10000) {
                    return "HTTP $responseCode: ${response.take(10000)}... (truncated)"
                }
                "HTTP $responseCode: $response"
            } else {
                connection.disconnect()
                "HTTP Error: $responseCode"
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP request failed: ${e.message}")
            "HTTP request failed: ${e.message}"
        }
    }

    fun cleanup() {
        executorService.shutdown()
    }
}
