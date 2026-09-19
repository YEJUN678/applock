package com.example.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.service.AppLockAccessibilityService

object AppLockPermissionHelper {

    fun hasAccessibilityPermission(context: Context): Boolean {
        return AppLockAccessibilityService.isAccessibilityServiceEnabled(context)
    }

    fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getAppDetailsSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun getOverlayPermissionIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        if (appOps != null) {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            if (mode == AppOpsManager.MODE_ALLOWED) return true
        }

        // Secondary fallback check via UsageStatsManager query test
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            val now = System.currentTimeMillis()
            val stats = usageStatsManager?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 1000 * 60, now)
            !stats.isNullOrEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun getUsageStatsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Checks whether a given packageName belongs to an active or enabled Input Method (Soft Keyboard).
     * This prevents keyboard popups (Samsung Keyboard, Gboard, etc.) from falsely triggering
     * package switches while the user is typing in a protected app.
     */
    fun isInputMethodPackage(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        // Known common IME package substrings
        val lower = packageName.lowercase()
        if (lower.contains("inputmethod") ||
            lower.contains("latinime") ||
            lower.contains(".gboard") ||
            lower.contains(".swiftkey") ||
            lower.contains("samsung.ime") ||
            lower.contains("samsungime") ||
            lower.contains("honeyboard") ||
            lower.contains(".keyboard") ||
            lower.contains("hangul") ||
            lower.contains(".ime")
        ) {
            return true
        }

        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            val imList = imm?.enabledInputMethodList ?: return false
            imList.any { it.packageName == packageName }
        } catch (_: Exception) {
            false
        }
    }
}

