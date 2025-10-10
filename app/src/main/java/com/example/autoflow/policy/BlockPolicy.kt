//BlockPolicy.kt (shared state management)
package com.example.autoflow.policy

import android.content.Context
import android.content.SharedPreferences

object BlockPolicy {
    private const val PREFS = "block_policy_prefs"
    private const val KEY_ENABLED = "blocking_enabled"
    private const val KEY_PACKAGES = "blocked_packages_csv"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setBlockingEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isBlockingEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ENABLED, false)

    fun setBlockedPackages(ctx: Context, pkgs: List<String>) {
        prefs(ctx).edit().putString(KEY_PACKAGES, pkgs.joinToString(",")).apply()
    }

    fun getBlockedPackages(ctx: Context): Set<String> =
        prefs(ctx).getString(KEY_PACKAGES, "")?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
}
