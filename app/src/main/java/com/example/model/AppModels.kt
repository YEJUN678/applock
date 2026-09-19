package com.example.model

import android.graphics.drawable.Drawable

data class AppItem(
    val id: String,
    val name: String,
    val packageName: String,
    val category: String = "설치된 앱",
    val isLocked: Boolean = false,
    val isSystemApp: Boolean = false,
    val iconDrawable: Drawable? = null
)

enum class LockType(val title: String, val description: String) {
    PATTERN("그리드 패턴", "3x3 ~ 10x10 커스텀 그리드 연결"),
    PIN("숫자 PIN 비밀번호", "4~8자리 숫자 보안 암호")
}

enum class BackgroundTheme(val title: String, val description: String) {
    CYBER_WALLPAPER("Cyber Matrix", "고해상도 네온 사이버 보안 배경"),
    DEEP_SPACE("Deep Space", "심해 우주 그라디언트"),
    NEON_NIGHT("Neon Pulse", "일렉트릭 바이올렛 & 네온 시안"),
    MINIMAL_STEALTH("Minimal Stealth", "다크 매트 블랙 & 미니멀")
}

data class LockConfig(
    val lockType: LockType = LockType.PATTERN,
    val gridSize: Int = 3, // 3 to 10
    val savedPattern: List<Int> = listOf(0, 1, 2, 5, 8), // Default N-grid indices
    val savedPin: String = "1234", // Default 4-digit PIN
    val biometricEnabled: Boolean = true,
    val backgroundTheme: BackgroundTheme = BackgroundTheme.CYBER_WALLPAPER,
    val lockTimeoutSeconds: Int = 30, // 0 = 즉시, 30 = 30초, 60 = 1분, 300 = 5분
    val isStealthPattern: Boolean = false, // Hide line while drawing pattern
    val isFakeCrashEnabled: Boolean = false, // Show fake crash dialog first
    val isVibrationEnabled: Boolean = true, // Haptic feedback on touch
    val isAppSelfProtectEnabled: Boolean = false, // Lock this app when opened
    val isIntruderSelfieEnabled: Boolean = true, // Capture photo on unauthorized attempt
    val intruderSelfieThreshold: Int = 1 // 1, 2, or 3 failed attempts
)

data class IntruderLog(
    val id: String,
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val attemptCount: Int,
    val usedLockType: String = "패턴",
    val photoPath: String? = null
)

