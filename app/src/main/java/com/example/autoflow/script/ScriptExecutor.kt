// Create new file: ScriptExecutor.kt
package com.example.autoflow.script

import android.content.Context as AndroidContext
import android.util.Log
import com.example.autoflow.model.Action
import com.example.autoflow.util.NotificationHelper
import org.mozilla.javascript.*
import java.io.StringWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL

class ScriptExecutor(private val context: AndroidContext) {

    fun executeScript(scriptCode: String): ScriptResult {
        return try {
            // Initialize Rhino context (renamed to avoid conflict)
            val rhinoContext = org.mozilla.javascript.Context.enter()
            rhinoContext.optimizationLevel = -1 // Disable optimization for Android

            // Create a scope
            val scope = rhinoContext.initStandardObjects()

            // Add Android context to script scope
            ScriptableObject.putProperty(scope, "androidContext", context)

            // Add utility functions
            addUtilityFunctions(rhinoContext, scope)

            // Execute the script
            val result = rhinoContext.evaluateString(scope, scriptCode, "<script>", 1, null)

            ScriptResult.Success(org.mozilla.javascript.Context.toString(result))

        } catch (e: RhinoException) {
            Log.e("ScriptExecutor", "Script execution error: ${e.message}")
            ScriptResult.Error("Script Error: ${e.message}")
        } catch (e: Exception) {
            Log.e("ScriptExecutor", "Unexpected error: ${e.message}")
            ScriptResult.Error("Execution Error: ${e.message}")
        } finally {
            org.mozilla.javascript.Context.exit()
        }
    }

    private fun addUtilityFunctions(rhinoContext: org.mozilla.javascript.Context, scope: Scriptable) {
        // Add log function
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
                Log.i("UserScript", message)
                return message
            }
        }
        ScriptableObject.putProperty(scope, "log", logFunction)

        // Add notification function
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

                    // Send notification using NotificationHelper
                    NotificationHelper.sendScriptNotification(this@ScriptExecutor.context, title, message)
                    return "Notification sent: $title"
                }
                return "No message provided"
            }
        }
        ScriptableObject.putProperty(scope, "notify", notifyFunction)

        // Add HTTP request function
        val httpFunction = object : BaseFunction() {
            override fun call(
                cx: org.mozilla.javascript.Context?,
                scope: Scriptable?,
                thisObj: Scriptable?,
                args: Array<out Any>?
            ): Any {
                if (args != null && args.isNotEmpty()) {
                    val url = org.mozilla.javascript.Context.toString(args[0])
                    return makeHttpRequest(url)
                }
                return "No URL provided"
            }
        }
        ScriptableObject.putProperty(scope, "httpGet", httpFunction)

        // Add delay/sleep function
        val delayFunction = object : BaseFunction() {
            override fun call(
                cx: org.mozilla.javascript.Context?,
                scope: Scriptable?,
                thisObj: Scriptable?,
                args: Array<out Any>?
            ): Any {
                if (args != null && args.isNotEmpty()) {
                    try {
                        val milliseconds = org.mozilla.javascript.Context.toNumber(args[0]).toLong()
                        Thread.sleep(milliseconds)
                        return "Delayed for $milliseconds ms"
                    } catch (e: Exception) {
                        return "Invalid delay value"
                    }
                }
                return "No delay value provided"
            }
        }
        ScriptableObject.putProperty(scope, "delay", delayFunction)

        // Add current time function
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

        // Add system info function
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
                    "appPackage" to context.packageName,
                    "timestamp" to System.currentTimeMillis()
                )
                return info.toString()
            }
        }
        ScriptableObject.putProperty(scope, "getSystemInfo", systemInfoFunction)
    }

    private fun makeHttpRequest(urlString: String): String {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                connectTimeout = 10000 // 10 seconds
                readTimeout = 10000 // 10 seconds
                setRequestProperty("User-Agent", "AutoFlow-Script/1.0")
            }

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                "HTTP $responseCode: $response"
            } else {
                connection.disconnect()
                "HTTP Error: $responseCode"
            }
        } catch (e: Exception) {
            Log.e("ScriptExecutor", "HTTP request failed: ${e.message}")
            "HTTP request failed: ${e.message}"
        }
    }
}

sealed class ScriptResult {
    data class Success(val output: String) : ScriptResult()
    data class Error(val error: String) : ScriptResult()
}
// Updated ScriptActionExecutor
class ScriptActionExecutor {
    private val context: AndroidContext
    private val scriptExecutor: ScriptExecutor

    constructor(context: AndroidContext) {
        this.context = context
        this.scriptExecutor = ScriptExecutor(context)
    }

    fun executeScriptAction(action: Action) {
        val scriptCode = action.getValue()

        if (scriptCode.isNullOrBlank()) {
            Log.w("ScriptActionExecutor", "Empty script code")
            return
        }

        // Execute in background thread
        Thread {
            val result = scriptExecutor.executeScript(scriptCode)

            // Handle result on main thread
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                when (result) {
                    is ScriptResult.Success -> {
                        Log.i("ScriptActionExecutor", "Script executed successfully: ${result.output}")
                        NotificationHelper.sendScriptNotification(
                            context,
                            "Script Success",
                            result.output
                        )
                    }
                    is ScriptResult.Error -> {
                        Log.e("ScriptActionExecutor", "Script execution failed: ${result.error}")
                        NotificationHelper.sendErrorNotification(
                            context,
                            "Script Error",
                            result.error
                        )
                    }
                }
            }
        }.start()
    }
}
