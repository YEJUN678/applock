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
    PIN("숫자 PIN 비밀번호", "4~8자리 숫자 보안 암호"),
    PASSWORD("문자+숫자 암호", "영문 대소문자, 숫자 조합 비밀번호"),
    CALCULATOR("보안 계산기", "실제 계산기 기능 지원, 암호 입력 후 '=' 터치 시 잠금 해제"),
    KNOCK_CODE("노크 코드", "4분면 영역을 정해진 순서대로 터치하여 해제")
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
    val savedPassword: String = "admin1234", // Alphanumeric password
    val savedCalculatorCode: String = "1234", // Calculator unlock code
    val savedKnockCode: List<Int> = listOf(1, 2, 3, 4), // 4-quadrant knock sequence (1: TL, 2: TR, 3: BL, 4: BR)
    val biometricEnabled: Boolean = true,
    val backgroundTheme: BackgroundTheme = BackgroundTheme.CYBER_WALLPAPER,
    val customLockBackgroundUri: String? = null,
    val lockScreenMessage: String = "",
    val lockIconScale: Float = 1f,
    val lockBackgroundDim: Float = 0.82f,
    val lockTimeoutSeconds: Int = 30, // 0 = 즉시, 30 = 30초, 60 = 1분, 300 = 5분
    val isStealthPattern: Boolean = false, // Hide line while drawing pattern
    val isFakeCrashEnabled: Boolean = false, // Show fake crash dialog first
    val isVibrationEnabled: Boolean = true, // Haptic feedback on touch
    val isAppSelfProtectEnabled: Boolean = false, // Lock this app when opened
    val isIntruderSelfieEnabled: Boolean = true, // Capture photo on unauthorized attempt
    val intruderSelfieThreshold: Int = 1, // 1, 2, or 3 failed attempts
    val isUninstallProtectionEnabled: Boolean = false, // Prevent unauthorized app uninstallation / deletion
    val isRandomPinKeypad: Boolean = false, // Shuffle PIN digits to prevent shoulder surfing
    val isIntruderSirenEnabled: Boolean = false, // Sound alert buzzer on intrusion attempts
    val isPanicShakeEnabled: Boolean = false, // Instantly lock all when phone is vigorously shaken
    val panicShakeStrength: Int = 10, // 1..10; 10 is maximum sensitivity
    val isNotificationPrivacyEnabled: Boolean = false, // Hide notifications from locked apps
    val isScreenOffLockEnabled: Boolean = true, // Instant lock on screen off
    val isScheduleLockEnabled: Boolean = false, // Time schedule auto-lock
    val scheduleStartHour: Int = 9,
    val scheduleStartMinute: Int = 0,
    val scheduleEndHour: Int = 18,
    val scheduleEndMinute: Int = 0
)

data class EncryptedVaultFile(
    val id: String,
    val originalName: String,
    val encryptedFileName: String,
    val originalSizeBytes: Long,
    val mimeType: String,
    val encryptedAtMillis: Long,
    val formattedSize: String,
    val fileUriString: String? = null,
    val isExternalDriveFile: Boolean = false,
    val driveLocationPath: String? = null
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
