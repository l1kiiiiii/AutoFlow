package com.example.autoflow.policy

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * This is the recommended Android pattern
 */
object BlockPolicy {
    private const val TAG = "BlockPolicy"
    private const val PREFS = "block_policy_prefs"
    private const val KEY_ENABLED = "blocking_enabled"
    private const val KEY_PACKAGES = "blocked_packages_csv"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setBlockingEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, enabled).apply()
        Log.d(TAG, "Blocking ${if (enabled) "enabled" else "disabled"}")
    }

    fun isBlockingEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ENABLED, false)

    fun setBlockedPackages(ctx: Context, pkgs: Set<String>) {
        prefs(ctx).edit().putString(KEY_PACKAGES, pkgs.joinToString(",")).apply()
        Log.d(TAG, "🚫 Updated blocked packages: ${pkgs.size} apps")
    }

    fun getBlockedPackages(ctx: Context): Set<String> =
        prefs(ctx).getString(KEY_PACKAGES, "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

    fun clearBlockedPackages(ctx: Context) {
        prefs(ctx).edit().remove(KEY_PACKAGES).apply()
        Log.d(TAG, "🗑️ Cleared all blocked packages")
    }

    fun addBlockedPackages(ctx: Context, packages: List<String>) {
        val current = getBlockedPackages(ctx).toMutableSet()
        current.addAll(packages)
        setBlockedPackages(ctx, current)
        Log.d(TAG, "➕ Added ${packages.size} apps to block list")
    }

    fun removeBlockedPackages(ctx: Context, packages: List<String>) {
        val current = getBlockedPackages(ctx).toMutableSet()
        current.removeAll(packages.toSet())
        setBlockedPackages(ctx, current)
        Log.d(TAG, "➖ Removed ${packages.size} apps from block list")
    }
}
