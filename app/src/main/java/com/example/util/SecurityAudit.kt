package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * 안전 모드 자가 진단.
 *
 * 권한 스위치가 켜져 있어도 실제로 동작하지 않는 경우가 있다.
 * (알림 접근은 시스템 설정에서 꺼지는데 앱은 모르는 경우가 많다)
 * 그래서 "켜짐"이 아니라 "실제로 통하는가"를 확인한다.
 */
object SecurityAudit {

    data class Check(
        val title: String,
        val ok: Boolean,
        val detail: String,
        /** false 이면 사용자가 직접 고칠 수 있는 항목인지 */
        val fixable: Boolean
    )

    /** 반드시 있어야 하는 항목부터, 있으면 좋은 항목 순으로 돌려준다. */
    fun run(context: Context): List<Check> {
        val checks = mutableListOf<Check>()

        checks += checkAccessibility(context)
        checks += checkOverlay(context)
        checks += checkNotificationListener(context)
        checks += checkBatteryOptimization(context)
        checks += checkNotificationPermission(context)

        val config = AppLockPreferences.getLockConfig(context)
        checks += Check(
            title = "잠금 방식 설정",
            ok = when (config.lockType) {
                com.example.model.LockType.PATTERN -> config.savedPattern.size >= 4
                com.example.model.LockType.PIN -> config.savedPin.length >= 4
                com.example.model.LockType.PASSWORD -> config.savedPassword.length >= 6
                else -> true
            },
            detail = when (config.lockType) {
                com.example.model.LockType.PATTERN -> "패턴 ${config.savedPattern.size}칸"
                else -> "${config.lockType.title} 사용 중"
            },
            fixable = true
        )

        val locked = AppLockPreferences.getLockedPackages(context).size
        checks += Check(
            title = "잠긴 앱",
            ok = locked > 0,
            detail = if (locked > 0) "$locked 개 앱 잠김" else "아직 잠긴 앱이 없습니다",
            fixable = true
        )

        val hasDuress = AppLockPreferences.getDuressPin(context).isNotBlank()
        checks += Check(
            title = "미끼 PIN",
            ok = hasDuress,
            detail = if (hasDuress) "설정됨" else "긴급 시 위장할 수 있는 PIN 이 없습니다",
            fixable = true
        )

        val screenBlocks = ScreenBlockStore.blockedCount(context)
        checks += Check(
            title = "특정 화면 차단",
            ok = screenBlocks > 0,
            detail = if (screenBlocks > 0) "$screenBlocks 개 화면 잠금" else "없음",
            fixable = true
        )

        val hasKey = AiSettings.isConfigured(context)
        checks += Check(
            title = "AI 키",
            ok = hasKey,
            detail = if (hasKey) "설정됨" else "AI 요약과 증거 분석을 쓸 수 없습니다",
            fixable = true
        )

        return checks
    }

    /** 확보된 권한 수(진단 화면의 큰 숫자). */
    fun scoreOf(checks: List<Check>): Int {
        if (checks.isEmpty()) return 0
        val required = checks.count { it.title in setOf("접근성 서비스", "다른 앱 위에 표시", "잠금 방식 설정") }
        val requiredOk = checks.filter { it.title in setOf("접근성 서비스", "다른 앱 위에 표시", "잠금 방식 설정") }.count { it.ok }
        val rest = checks.count { it.ok }
        val base = if (required == 0) 0 else (requiredOk * 55 / required)
        return (base + rest * 45 / (checks.size - required).coerceAtLeast(1)).coerceIn(0, 100)
    }

    private fun checkAccessibility(context: Context): Check {
        val ok = AppLockPermissionHelper.hasAccessibilityPermission(context)
        return Check(
            title = "접근성 서비스",
            ok = ok,
            detail = if (ok) "켜짐 — 앱 실행을 즉시 가로챕니다" else "꺼짐 — 앱이 열려도 잠금이 안 걸립니다",
            fixable = true
        )
    }

    private fun checkOverlay(context: Context): Check {
        val ok = InstalledAppsManager.hasOverlayPermission(context)
        return Check(
            title = "다른 앱 위에 표시",
            ok = ok,
            detail = if (ok) "켜짐 — 최후 방어선이 동작합니다" else "꺼짐 — 알림창·분할 화면에서 잠금이 씌워지지 않을 수 있습니다",
            fixable = true
        )
    }

    private fun checkNotificationListener(context: Context): Check {
        val ok = try {
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        } catch (_: Exception) {
            false
        }
        return Check(
            title = "알림 접근",
            ok = ok,
            detail = if (ok) "켜짐 — 잠긴 앱 알림을 가립니다" else "꺼짐 — 잠긴 앱의 알림 내용이 그대로 보입니다",
            fixable = true
        )
    }

    private fun checkBatteryOptimization(context: Context): Check {
        val ok = try {
            (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
                ?.isIgnoringBatteryOptimizations(context.packageName) == true
        } catch (_: Exception) {
            false
        }
        return Check(
            title = "배터리 최적화 예외",
            ok = ok,
            detail = if (ok) "예외됨 — 감시 서비스가 유지됩니다" else "최적화되면 잠금이 풀릴 수 있습니다",
            fixable = true
        )
    }

    private fun checkNotificationPermission(context: Context): Check {
        val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            } catch (_: Exception) {
                true
            }
        } else true

        return Check(
            title = "알림 표시 권한",
            ok = ok,
            detail = if (ok) "켜짐" else "꺼짐 — 하이라이트 알림이 보이지 않습니다",
            fixable = true
        )
    }

    fun fixIntentFor(context: Context, title: String): Intent? = when (title) {
        "접근성 서비스" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        "다른 앱 위에 표시" -> InstalledAppsManager.getOverlayPermissionIntent(context)
        "알림 접근" -> AppLockPermissionHelper.getNotificationListenerSettingsIntent()
        "배터리 최적화 예외" -> AppLockPermissionHelper.getBatteryOptimizationSettingsIntent()
        "알림 표시 권한" -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        else -> null
    }
}