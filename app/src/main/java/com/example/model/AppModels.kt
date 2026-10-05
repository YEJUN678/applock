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
    CALCULATOR("보안 계산기", "실제 계산기 기능 지원, 암호 입력 후 '=' 길게 누르면 잠금 해제"),
    KNOCK_CODE("노크 코드", "4분면 영역을 정해진 순서대로 터치하여 해제")
}

enum class BackgroundTheme(val title: String, val description: String) {
    CYBER_WALLPAPER("Cyber Matrix", "고해상도 네온 사이버 보안 배경"),
    DEEP_SPACE("Deep Space", "심해 우주 그라디언트"),
    NEON_NIGHT("Neon Pulse", "일렉트릭 바이올렛 & 네온 시안"),
    MINIMAL_STEALTH("Minimal Stealth", "다크 매트 블랙 & 미니멀")
}

enum class AiGuardFallback(val title: String) {
    DEVICE_CREDENTIAL("기기 화면 잠금"),
    BIOMETRIC("생체 인식"),
    REENTER_APP_LOCK("앱 잠금 다시 입력")
}

enum class LockClockStyle(val title: String) { LARGE("대형"), COMPACT("간결"), HIDDEN("숨김") }
enum class LockAccent(val title: String, val hex: Long) { CYAN("시안", 0xFF00E5FF), PURPLE("보라", 0xFFB388FF), GREEN("그린", 0xFF69F0AE), AMBER("앰버", 0xFFFFD740) }

/** One UI 8.5 스타일 잠금 화면 프리셋. 세부는 개별 옵션으로 더 조정한다. */
enum class LockPreset(
    val title: String,
    val description: String,
    val blur: Float,
    val corner: Float,
    val clockPosition: LockClockPosition,
    val iconShape: LockIconShape,
    val fontStyle: LockFontStyle,
    val panelAlpha: Float
) {
    ONE_UI("One UI 8.5", "블러 배경 · 은은한 패널 · 큰 시계", 0.55f, 28f, LockClockPosition.TOP_CENTER, LockIconShape.CIRCLE, LockFontStyle.SANS, 0.16f),
    MINIMAL("미니멀 매트", "무채색 · 얇은 라인 · 작은 시계", 0.15f, 16f, LockClockPosition.TOP_LEFT, LockIconShape.ROUNDED, LockFontStyle.SANS, 0.08f),
    GLASS("글래스 패널", "반투명 카드 · 강한 흐림", 0.75f, 32f, LockClockPosition.TOP_RIGHT, LockIconShape.ROUNDED, LockFontStyle.SANS, 0.24f),
    NEON_CYBER("네온 사이버", "고대비 네온 · 글로우 테두리", 0.35f, 12f, LockClockPosition.TOP_CENTER, LockIconShape.SQUARE, LockFontStyle.MONO, 0.12f),
    CLASSIC("클래식 안드로이드", "시스템 잠금 화면 느낌", 0.25f, 24f, LockClockPosition.TOP_CENTER, LockIconShape.CIRCLE, LockFontStyle.SERIF, 0.10f)
}

enum class LockClockPosition(val title: String) { TOP_CENTER("상단 중앙"), TOP_LEFT("좌측 상단"), TOP_RIGHT("우측 상단") }
enum class LockIconShape(val title: String) { CIRCLE("원형"), ROUNDED("둥근 사각형"), SQUARE("각진 사각형") }
enum class LockFontStyle(val title: String) { SANS("산세리프"), SERIF("세리프"), MONO("고정폭") }

/** 잠금 화면에 표시되는 충전 애니메이션 스타일. */
enum class LockChargingStyle(val title: String, val description: String) {
    NONE("표시 안 함", "충전 정보를 숨깁니다"),
    PULSE("맥동", "부드럽게 커졌다 줄어드는 빛"),
    RING("충전 링", "남은 충전량을 링으로 표시"),
    WAVE("전류 파형", "흐르는 파형 애니메이션")
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
    val lockClockStyle: LockClockStyle = LockClockStyle.LARGE,
    val lockAccent: LockAccent = LockAccent.CYAN,
    // 잠금 화면 꾸미기 (One UI 8.5 스타일 프리셋 + 세부 옵션)
    val lockPreset: LockPreset = LockPreset.ONE_UI,
    val lockBackgroundBlur: Float = 0.55f,          // 0..1
    val lockPanelAlpha: Float = 0.16f,              // 0..0.5
    val lockCornerRadius: Float = 28f,              // dp
    val lockClockPosition: LockClockPosition = LockClockPosition.TOP_CENTER,
    val lockIconShape: LockIconShape = LockIconShape.CIRCLE,
    val lockFontStyle: LockFontStyle = LockFontStyle.SANS,
    val lockChargingStyle: LockChargingStyle = LockChargingStyle.RING,
    val isLockQuickActionsEnabled: Boolean = true,
    val lockTimeoutSeconds: Int = 30, // 0 = 즉시, 30 = 30초, 60 = 1분, 300 = 5분
    val isStealthPattern: Boolean = false, // Hide line while drawing pattern
    val isFakeCrashEnabled: Boolean = false, // Show fake crash dialog first
    val isVibrationEnabled: Boolean = true, // Haptic feedback on touch
    val isAppSelfProtectEnabled: Boolean = false, // Lock this app when opened
    val isIntruderSelfieEnabled: Boolean = true, // Capture photo on unauthorized attempt
    val intruderSelfieThreshold: Int = 1, // 1, 2, or 3 failed attempts
    val isUninstallProtectionEnabled: Boolean = false, // Prevent unauthorized app uninstallation / deletion
    val isRandomPinKeypad: Boolean = false, // Shuffle PIN digits to prevent shoulder surfing
    val isAiGuardEnabled: Boolean = false, // Local behavioral anomaly detection
    val aiGuardSensitivity: Int = 2, // 1 low, 2 normal, 3 high
    val aiGuardFallback: AiGuardFallback = AiGuardFallback.DEVICE_CREDENTIAL,
    val isAiGuardVoiceRecordingEnabled: Boolean = false,
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

/**
 * 개인 확인 질문 기반 복구용 질문.
 * 답변은 해시로만 보관하므로 앱을 열어도 답을 그대로 읽을 수 없다.
 */
data class RecoveryQuestion(
    val id: String,
    val prompt: String,
    val answerHash: String,
    val salt: String,
    val hint: String? = null
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
    val photoPath: String? = null,
    val videoPath: String? = null,
    val audioPath: String? = null,
    // 실패가 일어난 위치 (로컬 전용, 서버 전송 없음)
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationText: String? = null,
    val placeName: String? = null
)
