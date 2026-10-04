package com.example.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
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

    fun requestOverlayPermission(context: Context) {
        try {
            context.startActivity(getOverlayPermissionIntent(context))
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Exception) {}
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

    // --- 권한 설정 도우미(온보딩)에서 사용하는 상태 확인 ---

    /** 잠긴 앱 알림 차단용 알림 접근 권한. */
    fun hasNotificationListenerPermission(context: Context): Boolean = try {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    } catch (_: Exception) {
        false
    }

    fun getNotificationListenerSettingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    /** 감시 서비스가 죽지 않으려면 배터리 최적화 예외 대상이어야 한다. */
    fun hasBatteryOptimizationExemption(context: Context): Boolean = try {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isIgnoringBatteryOptimizations(context.packageName) == true
    } catch (_: Exception) {
        false
    }

    /**
     * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 권한 없이 목록 화면으로 안내한다.
     * (이 권한은 마켓 정책 위반 소지가 있어 앱에는 선언하지 않는다)
     */
    fun getBatteryOptimizationSettingsIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    fun canPostNotifications(context: Context): Boolean = try {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    } catch (_: Exception) {
        false
    }

    /** GitHub APK 업데이트 설치를 위한 '알 수 없는 소스' 허용 여부. */
    fun canRequestPackageInstalls(context: Context): Boolean = try {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    } catch (_: Exception) {
        false
    }

    fun getUnknownSourcesSettingsIntent(context: Context): Intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        data = Uri.parse("package:${context.packageName}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    fun launchSettings(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent) }.onFailure {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }
        }
    }
}

