package com.example.autoflow.script



/**
 * Enum representing permissions that scripts can request
 * Follows principle of least privilege
 */
enum class ScriptPermission {
    // Notification permissions
    SEND_NOTIFICATION,

    // Network permissions
    HTTP_REQUEST,

    // System permissions
    ACCESS_SYSTEM_INFO,
    ACCESS_TIME,

    // Device control (more sensitive)
    MODIFY_SETTINGS,
    ACCESS_LOCATION,

    // Logging
    LOG_OUTPUT;

    companion object {
        /**
         * Default safe permissions granted to all scripts
         */
        fun getDefaultPermissions(): Set<ScriptPermission> {
            return setOf(
                LOG_OUTPUT,
                ACCESS_TIME,
                ACCESS_SYSTEM_INFO
            )
        }

        /**
         * Get user-friendly description for permission
         */
        fun getDescription(permission: ScriptPermission): String {
            return when (permission) {
                SEND_NOTIFICATION -> "Send notifications to your device"
                HTTP_REQUEST -> "Make network requests to external servers"
                ACCESS_SYSTEM_INFO -> "Read device model and Android version"
                ACCESS_TIME -> "Access current time"
                MODIFY_SETTINGS -> "Modify device settings"
                ACCESS_LOCATION -> "Access your location"
                LOG_OUTPUT -> "Write to application logs"
            }
        }
    }
}

/**
 * Security policy for script execution
 */
data class ScriptSecurityPolicy(
    val allowedPermissions: Set<ScriptPermission> = ScriptPermission.getDefaultPermissions(),
    val maxExecutionTimeMs: Long = 5000, // 5 seconds
    val maxMemoryMb: Int = 50,
    val allowLoops: Boolean = true,
    val maxLoopIterations: Int = 10000,
    val allowedDomains: Set<String> = emptySet() // Empty = allow all
) {
    companion object {
        /**
         * Safe default policy for untrusted scripts
         */
        fun createRestrictivePolicy(): ScriptSecurityPolicy {
            return ScriptSecurityPolicy(
                allowedPermissions = ScriptPermission.getDefaultPermissions(),
                maxExecutionTimeMs = 3000,
                maxMemoryMb = 30,
                allowLoops = true,
                maxLoopIterations = 1000,
                allowedDomains = emptySet()
            )
        }

        /**
         * Permissive policy for trusted scripts
         */
        fun createPermissivePolicy(): ScriptSecurityPolicy {
            return ScriptSecurityPolicy(
                allowedPermissions = ScriptPermission.values().toSet(),
                maxExecutionTimeMs = 10000,
                maxMemoryMb = 100,
                allowLoops = true,
                maxLoopIterations = 50000,
                allowedDomains = emptySet()
            )
        }
    }

    fun hasPermission(permission: ScriptPermission): Boolean {
        return allowedPermissions.contains(permission)
    }
}
