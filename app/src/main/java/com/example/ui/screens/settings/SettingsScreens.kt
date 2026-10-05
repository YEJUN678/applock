package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiGuardFallback
import com.example.model.LockConfig
import com.example.model.LockType
import com.example.util.NotificationHighlighter
import com.example.ui.components.settings.SettingsCard
import com.example.ui.components.settings.SettingsChoiceSheet
import com.example.ui.components.settings.SettingsHubCard
import com.example.ui.components.settings.SettingsInfoRow
import com.example.ui.components.settings.SettingsRow
import com.example.ui.components.settings.SettingsScreenHeader
import com.example.ui.components.settings.SettingsSectionLabel
import com.example.ui.components.settings.SettingsStatusBanner
import com.example.ui.components.settings.SettingsToggleRow
import com.example.ui.theme.OneUi
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** 설정 화면의 큰 구획. 허브에서 여기로 들어온다. */
enum class SettingsSection(val title: String, val description: String) {
    SECURITY("보안 · 권한", "잠금이 실제로 작동하는 데 필요한 권한과 방어 설정"),
    LOCK("잠금 방식", "잠금 수단, 재잠금 시간, 실패 제한, 앱별 일정"),
    APPEARANCE("화면 꾸미기", "잠금 화면 스타일, 배경, 사생활 보호"),
    VAULT("데이터 · 금고", "금고 · 보안 메모 · 백업 · 클립보드 · 실행 기록"),
    STEALTH("위장 · 고급", "위장 아이콘, AI 가드, 침입 증거, 업데이트")
}

/** 설정 화면이 읽는 값 모음. */
data class SettingsUiState(
    val lockConfig: LockConfig,
    val lockedAppCount: Int = 0,
    val hasAccessibilityPermission: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val isLostModeActive: Boolean = false,
    val maxFailedAttempts: Int = 0,
    val lockoutMinutes: Int = 5,
    val clipboardAutoClearEnabled: Boolean = false,
    val clipboardClearSeconds: Int = 60,
    val isFaceDownProtectionEnabled: Boolean = false,
    val recoveryConfigured: Boolean = false,
    val recoveryKeyConfigured: Boolean = false,
    val recoveryKeyFailedAttempts: Int = 0,
    val sessionCount: Int = 0,
    val notificationHighlightEnabled: Boolean = false,
    val notificationHighlightStyle: NotificationHighlighter.Style = NotificationHighlighter.Style.BLUE_VIOLET,
    val aiSummaryEnabled: Boolean = false,
    val aiKeyConfigured: Boolean = false,
    val lockBackgroundGallery: List<String> = emptyList(),
    val lockBackgroundAutoRotate: Boolean = false,
    val lockBackgroundRotateSeconds: Int = 30,
    val aiMaskedKey: String = "설정되지 않음",
    val aiModelName: String = "",
    val screenBlockAppCount: Int = 0,
    val screenBlockCount: Int = 0
)

/** 설정 화면에서 쓰는 색상 열거형 별칭(UI 층이 util 타입을 직접 다루지 않도록 한다). */
typealias HighlightStyle = NotificationHighlighter.Style

/** 설정 화면이 호출하는 동작 모음. */
data class SettingsUiActions(
    val requestAccessibility: () -> Unit = {},
    val requestOverlay: () -> Unit = {},
    val requestUsageStats: () -> Unit = {},
    val openPermissionWizard: () -> Unit = {},
    val toggleNotificationPrivacy: (Boolean) -> Unit = {},
    val toggleNotificationHighlight: (Boolean) -> Unit = {},
    val cycleHighlightStyle: () -> Unit = {},
    val toggleAiSummary: (Boolean) -> Unit = {},
    val addBackgroundToGallery: () -> Unit = {},
    val removeBackground: (String) -> Unit = {},
    val toggleBackgroundAutoRotate: (Boolean) -> Unit = {},
    val cycleBackgroundRotateSeconds: () -> Unit = {},
    val openAiKeyDialog: () -> Unit = {},
    val testAiKey: () -> Unit = {},
    val openScreenBlockManager: () -> Unit = {},
    val toggleAppSelfProtect: (Boolean) -> Unit = {},
    val toggleUninstallProtection: (Boolean) -> Unit = {},
    val toggleScreenOffLock: (Boolean) -> Unit = {},
    val toggleFaceDownProtection: (Boolean) -> Unit = {},
    val toggleLostMode: () -> Unit = {},
    val openLostModeMap: () -> Unit = {},

    val changeLockType: (LockType) -> Unit = {},
    val changePattern: () -> Unit = {},
    val changePin: () -> Unit = {},
    val changePassword: () -> Unit = {},
    val changeCalculatorCode: () -> Unit = {},
    val changeKnockCode: () -> Unit = {},
    val changeTimeout: (Int) -> Unit = {},
    val changeSchedule: (Boolean, Int, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
    val toggleBiometric: (Boolean) -> Unit = {},
    val toggleStealthPattern: (Boolean) -> Unit = {},
    val toggleRandomPin: (Boolean) -> Unit = {},
    val toggleVibration: (Boolean) -> Unit = {},
    val changeMaxFailedAttempts: (Int) -> Unit = {},
    val changeLockoutMinutes: (Int) -> Unit = {},
    val openAppSchedules: () -> Unit = {},
    val openExcludedApps: () -> Unit = {},
    val openRecoveryQuestions: () -> Unit = {},
    val openRecoveryKey: () -> Unit = {},

    val editLockStyle: () -> Unit = {},
    val changeBackgroundTheme: () -> Unit = {},
    val pickCustomLockBackground: () -> Unit = {},
    val togglePrivacyFilter: () -> Unit = {},
    val configureEmergencyContact: () -> Unit = {},

    val openVault: () -> Unit = {},
    val openSecureNotes: () -> Unit = {},
    val manageBackup: () -> Unit = {},
    val showRecoveryQr: () -> Unit = {},
    val scanRecoveryQr: () -> Unit = {},
    val toggleClipboardAutoClear: (Boolean) -> Unit = {},
    val changeClipboardSeconds: (Int) -> Unit = {},
    val clearSessionLog: () -> Unit = {},
    val showSessionLog: () -> Unit = {},

    val configureDisguise: () -> Unit = {},
    val toggleFakeCrash: (Boolean) -> Unit = {},
    val configureFakeScreen: () -> Unit = {},
    val configureDuressPin: () -> Unit = {},
    val toggleAiGuard: (Boolean) -> Unit = {},
    val changeAiGuardSensitivity: (Int) -> Unit = {},
    val changeAiGuardFallback: (AiGuardFallback) -> Unit = {},
    val toggleAiGuardVoice: (Boolean) -> Unit = {},
    val resetAiGuardLearning: () -> Unit = {},
    val toggleIntruderSelfie: (Boolean) -> Unit = {},
    val changeIntruderThreshold: (Int) -> Unit = {},
    val toggleIntruderSiren: (Boolean) -> Unit = {},
    val togglePanicShake: (Boolean) -> Unit = {},
    val checkForUpdates: () -> Unit = {}
)

/**
 * 설정 진입점: 허브와 5개 섹션 화면을 오간다.
 * LazyColumn 을 사용하고 항목은 '한 카드 = 한 item' 으로만 추가해 스크롤 성능을 지킨다.
 */
@Composable
fun SettingsRouter(
    state: SettingsUiState,
    actions: SettingsUiActions,
    modifier: Modifier = Modifier
) {
    var section by remember { mutableStateOf<SettingsSection?>(null) }
    BackHandler(enabled = section != null) { section = null }

    val current = section
    if (current == null) {
        SettingsHubScreen(
            state = state,
            actions = actions,
            onOpenSection = { section = it },
            modifier = modifier
        )
    } else {
        Column(modifier.fillMaxSize()) {
            when (current) {
                SettingsSection.SECURITY -> SecuritySectionScreen(state, actions) { section = null }
                SettingsSection.LOCK -> LockSectionScreen(state, actions) { section = null }
                SettingsSection.APPEARANCE -> AppearanceSectionScreen(state, actions) { section = null }
                SettingsSection.VAULT -> VaultSectionScreen(state, actions) { section = null }
                SettingsSection.STEALTH -> StealthSectionScreen(state, actions) { section = null }
            }
        }
    }
}

/**
 * 구획 화면 틀.
 *
 * 뒤로가기 버튼은 스크롤해도 따라오도록 리스트 밖(고정)에 둔다.
 * 안에 넣으면 아래로 내릴 때 사라져서 되돌아가기 어렵다.
 */
@Composable
internal fun SectionScaffold(
    section: SettingsSection,
    onBack: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        SettingsScreenHeader(title = section.title, subtitle = section.description, onBack = onBack, floating = true)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = OneUi.ScreenPadding,
                end = OneUi.ScreenPadding,
                top = 4.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(OneUi.CardSpacing)
        ) {
            content()
        }
    }
}

/** 검색 인덱스: 설정 항목 검색 시 어느 섹션으로 안내할지 알려준다. */
private data class SettingEntry(
    val section: SettingsSection,
    val label: String,
    val keywords: String,
    /** 왜 이 항목이 걸렸는지 보여줄 설명. 없으면 구획 이름만 보여준다. */
    val reason: String = ""
)



private val SEARCH_INDEX = listOf(
    SettingEntry(SettingsSection.SECURITY, "접근성 서비스", "권한 실시간 잠금 가로채기"),
    SettingEntry(SettingsSection.SECURITY, "다른 앱 위에 표시", "권한 overlay 잠금창"),
    SettingEntry(SettingsSection.SECURITY, "사용 정보 접근", "권한 앱 실행 감지"),
    SettingEntry(SettingsSection.SECURITY, "잠긴 앱 알림 숨김", "권한 알림 개인정보"),
    SettingEntry(SettingsSection.SECURITY, "알림 하이라이트", "알림 파랑 보라 카드"),
    SettingEntry(SettingsSection.SECURITY, "하이라이트 색상", "알림 파랑 보라 스타일"),
    SettingEntry(SettingsSection.SECURITY, "AI 요약 붙이기", "알림 AI 요약 cloud"),
    SettingEntry(SettingsSection.SECURITY, "앱 자체 보호", "이 앱도 잠금"),
    SettingEntry(SettingsSection.SECURITY, "미해제 방지", "잠금 제거 보호"),
    SettingEntry(SettingsSection.SECURITY, "Lost Mode", "분실 위치 추적"),
    SettingEntry(SettingsSection.SECURITY, "뒤집기 보호", "뒤집으면 잠금"),
    SettingEntry(SettingsSection.SECURITY, "화면 꺼짐 잠금", "화면 끄면 잠금"),
    SettingEntry(SettingsSection.LOCK, "잠금 방식", "패턴 pin 비밀번호 계산기 노크"),
    SettingEntry(SettingsSection.LOCK, "패턴 등록", "그리드 패턴"),
    SettingEntry(SettingsSection.LOCK, "PIN 변경", "숫자 비밀번호"),
    SettingEntry(SettingsSection.LOCK, "비밀번호 변경", "문자 숫자"),
    SettingEntry(SettingsSection.LOCK, "계산기 코드", "계산기 암호"),
    SettingEntry(SettingsSection.LOCK, "노크 코드", "노크"),
    SettingEntry(SettingsSection.LOCK, "재잠금 시간", "timeoutgrace"),
    SettingEntry(SettingsSection.LOCK, "시간대 자동 잠금", "요일 시간 스케줄"),
    SettingEntry(SettingsSection.LOCK, "생체 인증", "지문 얼굴"),
    SettingEntry(SettingsSection.LOCK, "은밀한 패턴", "stealth"),
    SettingEntry(SettingsSection.LOCK, "무작위 키패드", " shoulder surfing"),
    SettingEntry(SettingsSection.LOCK, "터치 진동", "햅틱"),
    SettingEntry(SettingsSection.LOCK, "실패 횟수 제한", "bruteforce 잠금 페널티"),
    SettingEntry(SettingsSection.LOCK, "앱별 잠금 일정", "요일 시간 앱별"),
    SettingEntry(SettingsSection.LOCK, "잠금 제외 앱", "allowlist"),
    SettingEntry(SettingsSection.LOCK, "복구 질문", "비밀번호 분실 복구"),
    SettingEntry(SettingsSection.LOCK, "12자리 복구키", "복구키"),
    SettingEntry(SettingsSection.APPEARANCE, "잠금 화면 편집기", "스타일 꾸미기 프리셋 one ui"),
    SettingEntry(SettingsSection.APPEARANCE, "배경 테마", "배경 사진", "색상 그라디언트 프리셋"),
    SettingEntry(SettingsSection.APPEARANCE, "배경 여러 장 등록", "배경 사진 갤러리 여러 장 슬라이드쇼", "갤러리 사진 여러 장 자동 전환"),
    SettingEntry(SettingsSection.APPEARANCE, "배경 자동 전환", "배경 슬라이드쇼 전환 시간 라이브 배경", "자동 전환 간격 초"),
    SettingEntry(SettingsSection.APPEARANCE, "사생활 보호 화면", "프라이버시 필터 가림막"),
    SettingEntry(SettingsSection.APPEARANCE, "비상 연락처", "잠금 화면 연락처"),
    SettingEntry(SettingsSection.VAULT, "AI 키 입력", "ai 키 gemini apikey 입력 변경", "AI 키를 입력하거나 교체합니다"),
    SettingEntry(SettingsSection.VAULT, "AI 키 테스트", "ai 키 확인 점검", "키가 실제로 통하는지 점검"),
    SettingEntry(SettingsSection.VAULT, "앱 안 특정 화면만 잠그기", "자세히 막기 화면 chatroom 프래그먼트 activity", "앱 안에서 특정 화면만 잠급니다"),
    SettingEntry(SettingsSection.VAULT, "파일 금고", "암호화 보관함"),
    SettingEntry(SettingsSection.VAULT, "보안 메모", "aes 메모"),
    SettingEntry(SettingsSection.VAULT, "암호화 백업", "백업 복원"),
    SettingEntry(SettingsSection.VAULT, "복구 QR", "오프라인 복구"),
    SettingEntry(SettingsSection.VAULT, "클립보드 자동 삭제", "개인정보 클립보드"),
    SettingEntry(SettingsSection.VAULT, "앱 실행 기록", "타임라인 기록"),
    SettingEntry(SettingsSection.STEALTH, "위장 아이콘", "계산기 메모 위장"),
    SettingEntry(SettingsSection.STEALTH, "가짜 오류 화면", "위장 crash"),
    SettingEntry(SettingsSection.STEALTH, "듀레스 · 미끼 PIN", "비상 핀 위장"),
    SettingEntry(SettingsSection.STEALTH, "AI 가드", "이상 행동 학습"),
    SettingEntry(SettingsSection.STEALTH, "침입 증거 촬영", "셀카 영상 음성"),
    SettingEntry(SettingsSection.STEALTH, "긴급 흔들기", "패닉 셰이크"),
    SettingEntry(SettingsSection.STEALTH, "업데이트 확인", "새 버전")
)

@Composable
private fun SettingsHubScreen(
    state: SettingsUiState,
    actions: SettingsUiActions,
    onOpenSection: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val missingRequired = !state.hasAccessibilityPermission || !state.hasOverlayPermission
    val matches = remember(query) {
        if (query.isBlank()) emptyList()
        else SEARCH_INDEX.filter { entry ->
            entry.label.contains(query, true) || entry.keywords.contains(query, true)
        }.distinctBy { it.label }
    }

    Column(modifier.fillMaxSize()) {
        // 하단 구획 카드가 많으므로 제목 줄은 고정해 둔다.
        SettingsScreenHeader(
            title = "설정",
            subtitle = "잠금 ${state.lockedAppCount}개 · App Lock & Vault",
            floating = true
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = OneUi.ScreenPadding,
                end = OneUi.ScreenPadding,
                top = 4.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(OneUi.CardSpacing)
        ) {
        // 권한이 없으면 무엇이 막혔는지 먼저 보여준다.
        if (missingRequired) {
            item {
                SettingsStatusBanner(
                    tint = OneUi.DangerTint,
                    title = "권한이 없어 잠금이 작동하지 않습니다",
                    description = buildString {
                        if (!state.hasAccessibilityPermission) append("접근성 서비스와 ")
                        if (!state.hasOverlayPermission) append("다른 앱 위에 표시 권한")
                    }.trim() + "이 필요합니다.",
                    actionLabel = "권한 설정 도우미 열기",
                    onAction = actions.openPermissionWizard
                )
            }
        }

        // 검색
        item {
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("설정 검색 (예: 알림, 패턴, AI 가드)", color = TextSecondary, fontSize = 13.sp) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OneUi.AccentTint,
                    unfocusedBorderColor = OneUi.Divider,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = OneUi.AccentTint
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (query.isNotBlank()) {
            if (matches.isEmpty()) {
                item { Text("일치하는 설정이 없습니다.", color = TextSecondary, fontSize = 13.sp) }
            } else {
                item { SettingsSectionLabel("검색 결과 ${matches.size}건") }
                item {
                    SettingsCard(accent = OneUi.CardSurface) {
                        matches.take(12).forEach { entry ->
                            SettingsRow(
                                icon = null,
                                title = entry.label,
                                subtitle = entry.reason.ifBlank { entry.section.title },
                                tint = OneUi.AccentTint,
                                showChevron = true,
                                highlightQuery = query,
                                onClick = {
                                    query = ""
                                    onOpenSection(entry.section)
                                }
                            )
                        }
                    }
                }
            }
        } else {
            item { SettingsSectionLabel("구획") }
            items2(SettingsSection.entries) { section ->
                SettingsHubCard(
                    icon = sectionIcon(section),
                    title = section.title,
                    description = section.description,
                    tint = sectionTint(section),
                    badge = sectionBadge(section, state),
                    onClick = { onOpenSection(section) }
                )
            }

            item { SettingsSectionLabel("바로 가기") }
            item {
                SettingsCard(accent = OneUi.CardSurface) {
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = "잠금 화면 꾸미기",
                        subtitle = "프리셋과 세부 스타일",
                        tint = OneUi.AccentTint,
                        showChevron = true,
                        onClick = { onOpenSection(SettingsSection.APPEARANCE) }
                    )
                    SettingsRow(
                        icon = Icons.Default.VerifiedUser,
                        title = "권한 설정 도우미",
                        subtitle = if (missingRequired) "필수 권한이 비어 있습니다" else "모든 필수 권한이 준비되었습니다",
                        tint = if (missingRequired) OneUi.DangerTint else OneUi.OkTint,
                        showChevron = true,
                        onClick = actions.openPermissionWizard
                    )
                    SettingsRow(
                        icon = Icons.Default.DownloadDone,
                        title = "업데이트 확인",
                        subtitle = "GitHub 릴리스에서 새 버전 받기",
                        tint = OneUi.OkTint,
                        showChevron = true,
                        onClick = actions.checkForUpdates
                    )
                }
            }
        }
        }
    }
}

private fun sectionIcon(section: SettingsSection) = when (section) {
    SettingsSection.SECURITY -> Icons.Default.Security
    SettingsSection.LOCK -> Icons.Default.Lock
    SettingsSection.APPEARANCE -> Icons.Default.Palette
    SettingsSection.VAULT -> Icons.Default.Folder
    SettingsSection.STEALTH -> Icons.Default.BugReport
}

private fun sectionTint(section: SettingsSection) = when (section) {
    SettingsSection.SECURITY -> OneUi.OkTint
    SettingsSection.LOCK -> OneUi.AccentTint
    SettingsSection.APPEARANCE -> IconsTintPurple
    SettingsSection.VAULT -> OneUi.WarnTint
    SettingsSection.STEALTH -> OneUi.DangerTint
}

private val IconsTintPurple = OneUi.InfoTint

private fun sectionBadge(section: SettingsSection, state: SettingsUiState): String? = when (section) {
    SettingsSection.SECURITY -> if (!state.hasAccessibilityPermission || !state.hasOverlayPermission) "권한 필요" else null
    SettingsSection.LOCK -> "${state.lockedAppCount}개 앱"
    SettingsSection.VAULT -> null
    else -> null
}

/** LazyListScope.items(enumEntries) 래퍼 (Kotlin 2.x enum.entries 용). */
private inline fun <T> androidx.compose.foundation.lazy.LazyListScope.items2(
    items: List<T>,
    crossinline itemContent: @Composable androidx.compose.foundation.lazy.LazyItemScope.(T) -> Unit
) = items.forEach { item ->
    item { itemContent(item) }
}