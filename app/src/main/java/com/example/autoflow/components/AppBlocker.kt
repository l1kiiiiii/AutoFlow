package com.example.autoflow.policy

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * ✅ COMPLETE BlockPolicy - Manages app blocking state with workflow tracking
 */
object BlockPolicy {
    private const val TAG = "BlockPolicy"
    private const val PREFS_NAME = "block_policy"
    private const val KEY_BLOCKING_ENABLED = "blocking_enabled"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val KEY_WORKFLOW_BLOCKS = "workflow_blocks" // NEW: Track which workflow blocked which apps
    private const val KEY_LOCATION_BLOCKS = "location_blocks" // NEW: Track location-based blocks
    private const val KEY_BLOCKING_REASONS = "blocking_reasons" // NEW: Track why apps are blocked

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ✅ EXISTING METHODS (Enhanced)
    fun setBlockingEnabled(context: Context, enabled: Boolean) {
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(KEY_BLOCKING_ENABLED, enabled).apply()
        Log.d(TAG, "🚫 Blocking ${if (enabled) "enabled" else "disabled"}")

        if (!enabled) {
            // When blocking is disabled globally, clear all blocks
            clearAllBlocks(context)
        }
    }

    fun isBlockingEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCKING_ENABLED, false)
    }

    fun setBlockedPackages(context: Context, packages: Set<String>) {
        val prefs = getPrefs(context)
        val jsonArray = JSONArray()
        packages.forEach { jsonArray.put(it) }
        prefs.edit().putString(KEY_BLOCKED_PACKAGES, jsonArray.toString()).apply()
        Log.d(TAG, "📦 Set blocked packages: ${packages.size} apps")
    }

    fun getBlockedPackages(context: Context): Set<String> {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString(KEY_BLOCKED_PACKAGES, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val packages = mutableSetOf<String>()
        for (i in 0 until jsonArray.length()) {
            packages.add(jsonArray.getString(i))
        }
        return packages
    }

    fun addBlockedPackages(context: Context, packages: List<String>) {
        val currentPackages = getBlockedPackages(context).toMutableSet()
        currentPackages.addAll(packages)
        setBlockedPackages(context, currentPackages)
    }

    fun removeBlockedPackages(context: Context, packages: List<String>) {
        val currentPackages = getBlockedPackages(context).toMutableSet()
        currentPackages.removeAll(packages.toSet())
        setBlockedPackages(context, currentPackages)
    }

    fun clearBlockedPackages(context: Context) {
        setBlockedPackages(context, emptySet())
    }

    fun isPackageBlocked(context: Context, packageName: String): Boolean {
        return isBlockingEnabled(context) && getBlockedPackages(context).contains(packageName)
    }

    // ✅ NEW METHODS: Workflow-based blocking with tracking

    /**
     * Block apps for a specific workflow with tracking
     */
    fun blockAppsForWorkflow(
        context: Context,
        workflowId: Long,
        packages: List<String>,
        reason: String = "workflow_triggered"
    ) {
        val prefs = getPrefs(context)

        // Add to general blocked packages
        addBlockedPackages(context, packages)

        // Track which workflow blocked which packages
        val workflowBlocks = getWorkflowBlocks(context).toMutableMap()
        workflowBlocks[workflowId] = packages
        saveWorkflowBlocks(context, workflowBlocks)

        // Track blocking reasons
        val reasons = getBlockingReasons(context).toMutableMap()
        packages.forEach { pkg ->
            reasons[pkg] = BlockReason(
                workflowId = workflowId,
                reason = reason,
                timestamp = System.currentTimeMillis()
            )
        }
        saveBlockingReasons(context, reasons)

        Log.d(TAG, "🚫 Blocked ${packages.size} apps for workflow $workflowId (reason: $reason)")
    }

    /**
     * Unblock apps for a specific workflow
     */
    fun unblockAppsForWorkflow(context: Context, workflowId: Long): List<String> {
        val workflowBlocks = getWorkflowBlocks(context).toMutableMap()
        val packagesToUnblock = workflowBlocks[workflowId] ?: emptyList()

        if (packagesToUnblock.isNotEmpty()) {
            // Remove from general blocked packages
            removeBlockedPackages(context, packagesToUnblock)

            // Remove workflow tracking
            workflowBlocks.remove(workflowId)
            saveWorkflowBlocks(context, workflowBlocks)

            // Remove blocking reasons
            val reasons = getBlockingReasons(context).toMutableMap()
            packagesToUnblock.forEach { pkg ->
                if (reasons[pkg]?.workflowId == workflowId) {
                    reasons.remove(pkg)
                }
            }
            saveBlockingReasons(context, reasons)

            Log.d(TAG, "✅ Unblocked ${packagesToUnblock.size} apps for workflow $workflowId")

            // If no more blocked packages, disable blocking
            if (getBlockedPackages(context).isEmpty()) {
                setBlockingEnabled(context, false)
                Log.d(TAG, "🔓 All apps unblocked - disabling blocking system")
            }
        }

        return packagesToUnblock
    }

    /**
     * Block apps for location-based trigger
     */
    fun blockAppsForLocation(
        context: Context,
        locationId: String,
        packages: List<String>,
        workflowId: Long
    ) {
        blockAppsForWorkflow(context, workflowId, packages, "location_$locationId")

        // Track location-specific blocks
        val locationBlocks = getLocationBlocks(context).toMutableMap()
        locationBlocks[locationId] = LocationBlock(
            workflowId = workflowId,
            packages = packages,
            timestamp = System.currentTimeMillis()
        )
        saveLocationBlocks(context, locationBlocks)

        Log.d(TAG, "📍 Blocked ${packages.size} apps for location $locationId")
    }

    /**
     * Unblock apps when exiting location
     */
    fun unblockAppsForLocation(context: Context, locationId: String): List<String> {
        val locationBlocks = getLocationBlocks(context).toMutableMap()
        val locationBlock = locationBlocks[locationId]

        val packagesToUnblock = if (locationBlock != null) {
            // Unblock the workflow associated with this location
            unblockAppsForWorkflow(context, locationBlock.workflowId)
        } else {
            emptyList()
        }

        // Remove location tracking
        locationBlocks.remove(locationId)
        saveLocationBlocks(context, locationBlocks)

        Log.d(TAG, "🚪 Unblocked ${packagesToUnblock.size} apps for location exit $locationId")
        return packagesToUnblock
    }

    /**
     * Force unblock all apps and clear all tracking
     */
    fun clearAllBlocks(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit()
            .remove(KEY_BLOCKED_PACKAGES)
            .remove(KEY_WORKFLOW_BLOCKS)
            .remove(KEY_LOCATION_BLOCKS)
            .remove(KEY_BLOCKING_REASONS)
            .putBoolean(KEY_BLOCKING_ENABLED, false)
            .apply()

        Log.d(TAG, "🧹 Cleared all app blocks and tracking data")
    }

    /**
     * Get which workflow blocked a specific package
     */
    fun getWorkflowForBlockedPackage(context: Context, packageName: String): Long? {
        val reasons = getBlockingReasons(context)
        return reasons[packageName]?.workflowId
    }

    /**
     * Get all apps blocked by a specific workflow
     */
    fun getAppsBlockedByWorkflow(context: Context, workflowId: Long): List<String> {
        val workflowBlocks = getWorkflowBlocks(context)
        return workflowBlocks[workflowId] ?: emptyList()
    }

    // ✅ PRIVATE HELPER METHODS

    private fun getWorkflowBlocks(context: Context): Map<Long, List<String>> {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString(KEY_WORKFLOW_BLOCKS, "{}") ?: "{}"
        return try {
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<Long, List<String>>()
            jsonObject.keys().forEach { key ->
                val workflowId = key.toLongOrNull()
                if (workflowId != null) {
                    val packagesArray = jsonObject.getJSONArray(key)
                    val packages = mutableListOf<String>()
                    for (i in 0 until packagesArray.length()) {
                        packages.add(packagesArray.getString(i))
                    }
                    map[workflowId] = packages
                }
            }
            map
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing workflow blocks", e)
            emptyMap()
        }
    }

    private fun saveWorkflowBlocks(context: Context, workflowBlocks: Map<Long, List<String>>) {
        val prefs = getPrefs(context)
        val jsonObject = JSONObject()
        workflowBlocks.forEach { (workflowId, packages) ->
            val packagesArray = JSONArray()
            packages.forEach { packagesArray.put(it) }
            jsonObject.put(workflowId.toString(), packagesArray)
        }
        prefs.edit().putString(KEY_WORKFLOW_BLOCKS, jsonObject.toString()).apply()
    }

    private fun getLocationBlocks(context: Context): Map<String, LocationBlock> {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString(KEY_LOCATION_BLOCKS, "{}") ?: "{}"
        return try {
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<String, LocationBlock>()
            jsonObject.keys().forEach { locationId ->
                val locationData = jsonObject.getJSONObject(locationId)
                val workflowId = locationData.getLong("workflowId")
                val packagesArray = locationData.getJSONArray("packages")
                val packages = mutableListOf<String>()
                for (i in 0 until packagesArray.length()) {
                    packages.add(packagesArray.getString(i))
                }
                val timestamp = locationData.getLong("timestamp")

                map[locationId] = LocationBlock(workflowId, packages, timestamp)
            }
            map
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing location blocks", e)
            emptyMap()
        }
    }

    private fun saveLocationBlocks(context: Context, locationBlocks: Map<String, LocationBlock>) {
        val prefs = getPrefs(context)
        val jsonObject = JSONObject()
        locationBlocks.forEach { (locationId, locationBlock) ->
            val locationData = JSONObject()
            locationData.put("workflowId", locationBlock.workflowId)
            locationData.put("timestamp", locationBlock.timestamp)

            val packagesArray = JSONArray()
            locationBlock.packages.forEach { packagesArray.put(it) }
            locationData.put("packages", packagesArray)

            jsonObject.put(locationId, locationData)
        }
        prefs.edit().putString(KEY_LOCATION_BLOCKS, jsonObject.toString()).apply()
    }

    private fun getBlockingReasons(context: Context): Map<String, BlockReason> {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString(KEY_BLOCKING_REASONS, "{}") ?: "{}"
        return try {
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<String, BlockReason>()
            jsonObject.keys().forEach { packageName ->
                val reasonData = jsonObject.getJSONObject(packageName)
                val workflowId = reasonData.getLong("workflowId")
                val reason = reasonData.getString("reason")
                val timestamp = reasonData.getLong("timestamp")

                map[packageName] = BlockReason(workflowId, reason, timestamp)
            }
            map
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing blocking reasons", e)
            emptyMap()
        }
    }

    private fun saveBlockingReasons(context: Context, reasons: Map<String, BlockReason>) {
        val prefs = getPrefs(context)
        val jsonObject = JSONObject()
        reasons.forEach { (packageName, blockReason) ->
            val reasonData = JSONObject()
            reasonData.put("workflowId", blockReason.workflowId)
            reasonData.put("reason", blockReason.reason)
            reasonData.put("timestamp", blockReason.timestamp)
            jsonObject.put(packageName, reasonData)
        }
        prefs.edit().putString(KEY_BLOCKING_REASONS, jsonObject.toString()).apply()
    }

    // ✅ DATA CLASSES
    data class LocationBlock(
        val workflowId: Long,
        val packages: List<String>,
        val timestamp: Long
    )

    data class BlockReason(
        val workflowId: Long,
        val reason: String,
        val timestamp: Long
    )
}
