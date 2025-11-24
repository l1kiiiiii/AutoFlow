package com.example.autoflow.script


import android.util.Log
import java.util.regex.Pattern

/**
 * Validates scripts for security threats before execution
 * Static analysis to detect dangerous patterns
 */
class ScriptValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val threats: List<SecurityThreat> = emptyList(),
        val warnings: List<String> = emptyList()
    ) {
        fun isSafe(): Boolean = isValid && threats.isEmpty()
    }

    data class SecurityThreat(
        val severity: Severity,
        val description: String,
        val lineNumber: Int? = null,
        val recommendation: String = ""
    ) {
        enum class Severity {
            LOW, MEDIUM, HIGH, CRITICAL
        }
    }

    companion object {
        private const val TAG = "ScriptValidator"

        // Dangerous patterns to detect
        private val DANGEROUS_PATTERNS = mapOf(
            "eval\\s*\\(" to SecurityThreat(
                SecurityThreat.Severity.CRITICAL,
                "Usage of eval() detected - allows arbitrary code execution",
                recommendation = "Remove eval() usage"
            ),
            "Function\\s*\\(" to SecurityThreat(
                SecurityThreat.Severity.HIGH,
                "Dynamic function creation detected",
                recommendation = "Use regular function declarations"
            ),
            "while\\s*\\(\\s*true" to SecurityThreat(
                SecurityThreat.Severity.HIGH,
                "Potential infinite loop detected",
                recommendation = "Add exit condition or use for loop with limit"
            ),
            "for\\s*\\(.*\\)" to SecurityThreat(
                SecurityThreat.Severity.LOW,
                "Loop detected - ensure it has reasonable bounds",
                recommendation = "Verify loop iteration count"
            )
        )

        // Suspicious network patterns
        private val NETWORK_PATTERNS = listOf(
            "http://",
            "https://",
            "ws://",
            "wss://",
            "ftp://"
        )
    }

    /**
     * Validate script against security policy
     */
    fun validate(
        scriptCode: String,
        policy: ScriptSecurityPolicy
    ): ValidationResult {
        val threats = mutableListOf<SecurityThreat>()
        val warnings = mutableListOf<String>()

        // 1. Check for dangerous patterns
        DANGEROUS_PATTERNS.forEach { (pattern, baseThreat) ->
            val matcher = Pattern.compile(pattern).matcher(scriptCode)
            if (matcher.find()) {
                threats.add(baseThreat.copy(
                    lineNumber = getLineNumber(scriptCode, matcher.start())
                ))
            }
        }

        // 2. Check network requests
        val hasNetworkRequests = NETWORK_PATTERNS.any { scriptCode.contains(it) }
        if (hasNetworkRequests && !policy.hasPermission(ScriptPermission.HTTP_REQUEST)) {
            threats.add(SecurityThreat(
                SecurityThreat.Severity.HIGH,
                "Script attempts network access without permission",
                recommendation = "Grant HTTP_REQUEST permission or remove network calls"
            ))
        }

        // 3. Check for excessive complexity
        val lineCount = scriptCode.lines().size
        if (lineCount > 500) {
            warnings.add("Script is very long ($lineCount lines) - consider breaking into smaller scripts")
        }

        // 4. Check for nested loops (potential performance issue)
        val nestedLoopCount = countNestedLoops(scriptCode)
        if (nestedLoopCount > 2) {
            warnings.add("Deeply nested loops detected - may impact performance")
        }

        // 5. Basic syntax check
        if (scriptCode.trim().isEmpty()) {
            threats.add(SecurityThreat(
                SecurityThreat.Severity.LOW,
                "Script is empty"
            ))
        }

        // Log validation results
        if (threats.isNotEmpty()) {
            Log.w(TAG, "Validation found ${threats.size} threats")
        }

        val isValid = threats.none { it.severity in listOf(
            SecurityThreat.Severity.CRITICAL,
            SecurityThreat.Severity.HIGH
        )}

        return ValidationResult(isValid, threats, warnings)
    }

    /**
     * Quick validation for basic safety
     */
    fun isBasicallySafe(scriptCode: String): Boolean {
        return validate(scriptCode, ScriptSecurityPolicy.createRestrictivePolicy()).isSafe()
    }

    private fun getLineNumber(code: String, position: Int): Int {
        return code.substring(0, position).count { it == '\n' } + 1
    }

    private fun countNestedLoops(code: String): Int {
        var maxDepth = 0
        var currentDepth = 0

        val loopKeywords = listOf("for", "while", "do")
        val lines = code.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (loopKeywords.any { trimmed.startsWith(it) }) {
                currentDepth++
                maxDepth = maxOf(maxDepth, currentDepth)
            }
            if (trimmed.contains("}")) {
                currentDepth = maxOf(0, currentDepth - 1)
            }
        }

        return maxDepth
    }
}
