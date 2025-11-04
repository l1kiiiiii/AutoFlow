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
    private const val KEY_WORKFLOW_BLOCKS = "workflow_blocks"
    private const val KEY_LOCATION_BLOCKS = "location_blocks"
    private const val KEY_BLOCKING_REASONS = "blocking_reasons"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setBlockingEnabled(context: Context, enabled: Boolean) {
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(KEY_BLOCKING_ENABLED, enabled).apply()
        Log.d(TAG, "🚫 Blocking ${if (enabled) "enabled" else "disabled"}")

        if (!enabled) {
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

    /**
     * Block apps for a specific workflow with tracking
     */
    fun blockAppsForWorkflow(
        context: Context,
        workflowId: Long,
        packages: List<String>,
        reason: String = "workflow_triggered"
    ) {
        addBlockedPackages(context, packages)

        val workflowBlocks = getWorkflowBlocks(context).toMutableMap()
        workflowBlocks[workflowId] = packages
        saveWorkflowBlocks(context, workflowBlocks)

        Log.d(TAG, "🚫 Blocked ${packages.size} apps for workflow $workflowId (reason: $reason)")
    }

    /**
     * Unblock apps for a specific workflow
     */
    fun unblockAppsForWorkflow(context: Context, workflowId: Long): List<String> {
        val workflowBlocks = getWorkflowBlocks(context).toMutableMap()
        val packagesToUnblock = workflowBlocks[workflowId] ?: emptyList()

        if (packagesToUnblock.isNotEmpty()) {
            removeBlockedPackages(context, packagesToUnblock)
            workflowBlocks.remove(workflowId)
            saveWorkflowBlocks(context, workflowBlocks)

            Log.d(TAG, "✅ Unblocked ${packagesToUnblock.size} apps for workflow $workflowId")

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
            unblockAppsForWorkflow(context, locationBlock.workflowId)
        } else {
            emptyList()
        }

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

    // PRIVATE HELPER METHODS
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

    // DATA CLASSES
    data class LocationBlock(
        val workflowId: Long,
        val packages: List<String>,
        val timestamp: Long
    )
}
