package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Icon
import com.example.model.AiGuardFallback
import com.example.model.LockType
import com.example.util.NotificationHighlighter
import com.example.ui.components.settings.SettingsCard
import com.example.ui.components.settings.SettingsChoiceSheet
import com.example.ui.components.settings.SettingsInfoRow
import com.example.ui.components.settings.SettingsRow
import com.example.ui.components.settings.SettingsSectionLabel
import com.example.ui.components.settings.SettingsStatusBanner
import com.example.ui.components.settings.SettingsToggleRow
import com.example.ui.theme.OneUi

/** 보안 · 권한 */
@Composable
fun SecuritySectionScreen(state: SettingsUiState, actions: SettingsUiActions, onBack: () -> Unit) {
    SectionScaffold(SettingsSection.SECURITY, onBack) {
        if (!state.hasAccessibilityPermission || !state.hasOverlayPermission) {
            item {
                SettingsStatusBanner(
                    tint = OneUi.DangerTint,
                    title = "필수 권한이 없습니다",
                    description = "접근성 서비스와 '다른 앱 위에 표시'는 빠짐없이 켜야 잠금이 작동합니다.",
                    actionLabel = "권한 설정 도우미 열기",
                    onAction = actions.openPermissionWizard
                )
            }
        }

        item { SettingsSectionLabel("권한") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.Security,
                    title = "접근성 서비스",
                    subtitle = "앱을 여는 즉시 잠금 화면 표시",
                    tint = if (state.hasAccessibilityPermission) OneUi.OkTint else OneUi.DangerTint,
                    value = if (state.hasAccessibilityPermission) "허용됨" else "필요",
                    showChevron = true,
                    onClick = actions.requestAccessibility
                )
                SettingsRow(
                    icon = Icons.Default.Lock,
                    title = "다른 앱 위에 표시",
                    subtitle = "접근성으로 못 잡는 순간에 잠금창을 덮음",
                    tint = if (state.hasOverlayPermission) OneUi.OkTint else OneUi.DangerTint,
                    value = if (state.hasOverlayPermission) "허용됨" else "필요",
                    showChevron = true,
                    onClick = actions.requestOverlay
                )
                SettingsRow(
                    icon = Icons.Default.PhoneAndroid,
                    title = "사용 정보 접근",
                    subtitle = "앱 실행을 감지하는 대체 경로",
                    tint = if (state.hasUsageStatsPermission) OneUi.OkTint else OneUi.WarnTint,
                    value = if (state.hasUsageStatsPermission) "허용됨" else "권장",
                    showChevron = true,
                    onClick = actions.requestUsageStats
                )
            }
        }

        item { SettingsSectionLabel("방어") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "잠긴 앱 알림 숨김",
                    subtitle = "잠긴 앱의 알림 내용을 가립니다",
                    tint = OneUi.InfoTint,
                    checked = state.lockConfig.isNotificationPrivacyEnabled,
                    onCheckedChange = actions.toggleNotificationPrivacy
                )
                SettingsToggleRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "알림 하이라이트",
                    subtitle = "보호된 알림을 파랑~보라 카드로 바꿔 보여줍니다",
                    tint = OneUi.AccentTint,
                    checked = state.notificationHighlightEnabled,
                    onCheckedChange = actions.toggleNotificationHighlight
                )
                if (state.notificationHighlightEnabled) {
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = "하이라이트 색상",
                        subtitle = when (state.notificationHighlightStyle) {
                            HighlightStyle.BLUE_VIOLET -> "파랑 → 보라"
                            HighlightStyle.VIOLET -> "보라 중심"
                            HighlightStyle.TEAL -> "청록 → 남색"
                        },
                        tint = OneUi.InfoTint,
                        showChevron = true,
                        onClick = actions.cycleHighlightStyle
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "AI 요약 붙이기",
                        subtitle = if (state.aiKeyConfigured) {
                            "알림 내용을 AI 로 한 줄 요약합니다 (본문 전송)"
                        } else {
                            "AI 키를 먼저 설정해 주세요 (증거 AI 분석 화면)"
                        },
                        tint = OneUi.OkTint,
                        checked = state.aiSummaryEnabled,
                        enabled = state.aiKeyConfigured,
                        onCheckedChange = actions.toggleAiSummary
                    )
                }
                SettingsToggleRow(
                    icon = Icons.Default.VerifiedUser,
                    title = "앱 자체 보호",
                    subtitle = "이 앱을 열 때도 잠금 적용",
                    tint = OneUi.AccentTint,
                    checked = state.lockConfig.isAppSelfProtectEnabled,
                    onCheckedChange = actions.toggleAppSelfProtect
                )
                SettingsToggleRow(
                    icon = Icons.Default.Security,
                    title = "잠금 해제 방어",
                    subtitle = "방향 권한 없이 앱이 삭제되지 않게 보호",
                    tint = OneUi.WarnTint,
                    checked = state.lockConfig.isUninstallProtectionEnabled,
                    onCheckedChange = actions.toggleUninstallProtection
                )
                SettingsToggleRow(
                    icon = Icons.Default.BatteryAlert,
                    title = "화면 꺼짐 시 즉시 잠금",
                    subtitle = "화면이 꺼지면 임시 해제를 회수",
                    tint = OneUi.AccentTint,
                    checked = state.lockConfig.isScreenOffLockEnabled,
                    onCheckedChange = actions.toggleScreenOffLock
                )
                SettingsToggleRow(
                    icon = Icons.Default.PhoneAndroid,
                    title = "뒤집기 보호",
                    subtitle = "휴대폰을 뒤집으면 즉시 잠금",
                    tint = OneUi.WarnTint,
                    checked = state.isFaceDownProtectionEnabled,
                    onCheckedChange = actions.toggleFaceDownProtection
                )
            }
        }

        item { SettingsSectionLabel("분실 대응") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.MyLocation,
                    title = "Lost Mode",
                    subtitle = if (state.isLostModeActive) "활성화되어 위치를 기록하고 있습니다" else "휴대폰을 분실했을 때 위치 기록을 시작",
                    tint = if (state.isLostModeActive) OneUi.DangerTint else OneUi.WarnTint,
                    value = if (state.isLostModeActive) "켜짐" else "꺼짐",
                    onClick = actions.toggleLostMode
                )
                SettingsRow(
                    icon = Icons.Default.MyLocation,
                    title = "최근 위치 지도에서 열기",
                    tint = OneUi.AccentTint,
                    showChevron = true,
                    onClick = actions.openLostModeMap
                )
            }
        }
    }
}

/** 잠금 방식 */
@Composable
fun LockSectionScreen(state: SettingsUiState, actions: SettingsUiActions, onBack: () -> Unit) {
    val config = state.lockConfig
    var sheet by remember { mutableStateOf<LockSheet?>(null) }

    SectionScaffold(SettingsSection.LOCK, onBack) {
        item { SettingsSectionLabel("잠금 수단") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.Lock,
                    title = "잠금 방식",
                    subtitle = config.lockType.description,
                    tint = OneUi.AccentTint,
                    value = config.lockType.title,
                    showChevron = true,
                    onClick = { sheet = LockSheet.TYPE }
                )
                SettingsRow(
                    icon = Icons.Default.Edit,
                    title = "패턴 등록",
                    subtitle = "${config.gridSize}x${config.gridSize} 그리드",
                    tint = OneUi.AccentTint,
                    showChevron = true,
                    onClick = actions.changePattern
                )
                SettingsRow(
                    icon = Icons.Default.Key,
                    title = "PIN 변경",
                    subtitle = "숫자 ${config.savedPin.length}자리",
                    tint = OneUi.AccentTint,
                    showChevron = true,
                    onClick = actions.changePin
                )
                SettingsRow(
                    icon = Icons.Default.Key,
                    title = "비밀번호 변경",
                    subtitle = "영문 · 숫자 조합",
                    tint = OneUi.AccentTint,
                    showChevron = true,
                    onClick = actions.changePassword
                )
                SettingsRow(
                    icon = Icons.Default.PhoneAndroid,
                    title = "계산기 코드 변경",
                    subtitle = "'=' 을 길게 눌러 제출",
                    tint = OneUi.AccentTint,
                    showChevron = true,
                    onClick = actions.changeCalculatorCode
                )
                SettingsRow(
                    icon = Icons.Default.Timer,
                    title = "노크 코드 변경",
                    tint = OneUi.AccentTint,
                    showChevron = true,
                    onClick = actions.changeKnockCode
                )
            }
        }

        item { SettingsSectionLabel("잠금 해제 정책") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.Timer,
                    title = "재잠금 시간",
                    subtitle = if (config.lockTimeoutSeconds <= 0) "앱을 떠나면 즉시 잠금" else "인증 후 ${config.lockTimeoutSeconds}초 유지",
                    tint = OneUi.AccentTint,
                    showChevron = true,
                    onClick = { sheet = LockSheet.TIMEOUT }
                )
                SettingsRow(
                    icon = Icons.Default.Schedule,
                    title = "시간대 자동 잠금",
                    subtitle = if (!config.isScheduleLockEnabled) "꺼짐" else "매일 %02d:%02d ~ %02d:%02d".format(
                        config.scheduleStartHour, config.scheduleStartMinute,
                        config.scheduleEndHour, config.scheduleEndMinute
                    ),
                    tint = if (config.isScheduleLockEnabled) OneUi.OkTint else OneUi.AccentTint,
                    showChevron = true,
                    onClick = { sheet = LockSheet.SCHEDULE }
                )
                SettingsRow(
                    icon = Icons.Default.Warning,
                    title = "실패 횟수 제한",
                    subtitle = if (state.maxFailedAttempts <= 0) "제한 없음" else "${state.maxFailedAttempts}회 실패 시 ${state.lockoutMinutes}분 차단",
                    tint = if (state.maxFailedAttempts > 0) OneUi.OkTint else OneUi.WarnTint,
                    showChevron = true,
                    onClick = { sheet = LockSheet.FAILED }
                )
            }
        }

        item { SettingsSectionLabel("편의") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsToggleRow(
                    icon = Icons.Default.Fingerprint,
                    title = "생체 인증",
                    subtitle = "지문 · 얼굴 인식으로 즉시 해제",
                    tint = OneUi.OkTint,
                    checked = config.biometricEnabled,
                    onCheckedChange = actions.toggleBiometric
                )
                SettingsToggleRow(
                    icon = Icons.Default.VisibilityOff,
                    title = "은밀한 패턴",
                    subtitle = "패턴을 그리는 동안 선을 숨깁니다",
                    tint = OneUi.InfoTint,
                    checked = config.isStealthPattern,
                    onCheckedChange = actions.toggleStealthPattern
                )
                SettingsToggleRow(
                    icon = Icons.Default.Key,
                    title = "무작위 키패드",
                    subtitle = "숫자 배열을 섞어 옆에서 못 보게 합니다",
                    tint = OneUi.InfoTint,
                    checked = config.isRandomPinKeypad,
                    onCheckedChange = actions.toggleRandomPin
                )
                SettingsToggleRow(
                    icon = Icons.Default.PhoneAndroid,
                    title = "터치 진동",
                    tint = OneUi.AccentTint,
                    checked = config.isVibrationEnabled,
                    onCheckedChange = actions.toggleVibration
                )
            }
        }

        item { SettingsSectionLabel("범위") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.Schedule,
                    title = "앱별 잠금 일정",
                    subtitle = "요일·시간 안에서만 잠금",
                    tint = OneUi.AccentTint,
                    showChevron = true,
                    onClick = actions.openAppSchedules
                )
                SettingsRow(
                    icon = Icons.Default.Security,
                    title = "잠금 제외 앱",
                    subtitle = "잠금을 적용하지 않을 앱",
                    tint = OneUi.WarnTint,
                    showChevron = true,
                    onClick = actions.openExcludedApps
                )
            }
        }

        item { SettingsSectionLabel("잠금 분실 대비") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.Psychology,
                    title = "개인 확인 질문",
                    subtitle = if (state.recoveryConfigured) "등록됨 · 답은 해시로 보관" else "등록하지 않았습니다",
                    tint = if (state.recoveryConfigured) OneUi.OkTint else OneUi.WarnTint,
                    showChevron = true,
                    onClick = actions.openRecoveryQuestions
                )
                SettingsRow(
                    icon = Icons.Default.Key,
                    title = "12자리 복구키",
                    subtitle = if (state.recoveryKeyConfigured) "등록됨 · 시도 제한 없음" else "등록하지 않았습니다",
                    tint = if (state.recoveryKeyConfigured) OneUi.OkTint else OneUi.WarnTint,
                    showChevron = true,
                    onClick = actions.openRecoveryKey
                )
            }
        }
    }

    // 값 선택 시트들
    when (sheet) {
        LockSheet.TYPE -> SettingsChoiceSheet(
            title = "잠금 방식",
            subtitle = "설정한 방식으로만 잠금을 해제할 수 있습니다.",
            options = LockType.entries.map { it to it.title },
            selectedIndex = LockType.entries.indexOf(config.lockType),
            onSelect = { actions.changeLockType(it); sheet = null },
            onDismiss = { sheet = null }
        )
        LockSheet.TIMEOUT -> SettingsChoiceSheet(
            title = "재잠금 시간",
            subtitle = "인증 후 이 시간이 지나면 다시 잠깁니다.",
            options = listOf(
                0 to "앱을 떠나면 즉시 잠금",
                30 to "30초",
                60 to "1분",
                300 to "5분",
                1800 to "30분"
            ),
            selectedIndex = listOf(0, 30, 60, 300, 1800).indexOf(config.lockTimeoutSeconds).coerceAtLeast(0),
            onSelect = { actions.changeTimeout(it); sheet = null },
            onDismiss = { sheet = null }
        )
        LockSheet.FAILED -> SettingsChoiceSheet(
            title = "실패 횟수 제한",
            subtitle = "연속 실패 시 입력 자체를 막아 추적을 어렵게 합니다.",
            options = listOf(
                0 to "제한 없음",
                3 to "3회 실패 → 차단",
                5 to "5회 실패 → 차단",
                10 to "10회 실패 → 차단"
            ),
            selectedIndex = listOf(0, 3, 5, 10).indexOf(state.maxFailedAttempts).coerceAtLeast(0),
            onSelect = {
                actions.changeMaxFailedAttempts(it)
                if (state.maxFailedAttempts > 0) sheet = LockSheet.LOCKOUT else sheet = null
            },
            onDismiss = { sheet = null }
        )
        LockSheet.LOCKOUT -> SettingsChoiceSheet(
            title = "차단 시간",
            options = listOf(1 to "1분", 5 to "5분", 15 to "15분", 60 to "1시간"),
            selectedIndex = listOf(1, 5, 15, 60).indexOf(state.lockoutMinutes).coerceAtLeast(0),
            onSelect = { actions.changeLockoutMinutes(it); sheet = null },
            onDismiss = { sheet = null }
        )
        LockSheet.SCHEDULE -> SettingsChoiceSheet(
            title = "시간대 자동 잠금",
            subtitle = "매일 같은 시간에 잠금을 켜고 끕니다.",
            options = listOf<Pair<String, String>>(
                "꺼짐" to "시간대 잠금을 사용하지 않습니다",
                "주간" to "09:00 ~ 18:00 동안 잠급니다",
                "심야" to "22:00 ~ 07:00 동안 잠급니다"
            ),
            selectedIndex = if (!config.isScheduleLockEnabled) 0
            else if (config.scheduleStartHour == 22) 2 else 1,
            onSelect = { preset ->
                when (preset) {
                    "꺼짐" -> actions.changeSchedule(false, 9, 0, 18, 0)
                    "주간" -> actions.changeSchedule(true, 9, 0, 18, 0)
                    else -> actions.changeSchedule(true, 22, 0, 7, 0)
                }
                sheet = null
            },
            onDismiss = { sheet = null }
        )
        null -> Unit
    }
}

private enum class LockSheet { TYPE, TIMEOUT, FAILED, LOCKOUT, SCHEDULE }

/** 화면 꾸미기 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AppearanceSectionScreen(state: SettingsUiState, actions: SettingsUiActions, onBack: () -> Unit) {
    val config = state.lockConfig
    SectionScaffold(SettingsSection.APPEARANCE, onBack) {
        item { SettingsSectionLabel("잠금 화면") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.Palette,
                    title = "잠금 화면 편집기",
                    subtitle = "프리셋 5종 · 흐림 · 모서리 · 시계 위치 · 글꼴",
                    tint = OneUi.AccentTint,
                    value = config.lockPreset.title,
                    showChevron = true,
                    onClick = actions.editLockStyle
                )
                SettingsRow(
                    icon = Icons.Default.Image,
                    title = "배경 테마",
                    subtitle = config.backgroundTheme.description,
                    tint = OneUi.InfoTint,
                    value = config.backgroundTheme.title,
                    showChevron = true,
                    onClick = actions.changeBackgroundTheme
                )
                SettingsRow(
                    icon = Icons.Default.Image,
                    title = "사진 배경 지정",
                    subtitle = "갤러리에서 사진 하나를 고르면 잠금 배경으로 씁니다",
                    tint = OneUi.AccentTint,
                    showChevron = true,
                    onClick = actions.pickCustomLockBackground
                )
                SettingsRow(
                    icon = Icons.Default.AddPhotoAlternate,
                    title = "배경 여러 장 등록",
                    subtitle = if (state.lockBackgroundGallery.isEmpty()) {
                        "사진을 2장 이상 넣으면 자동 전환할 수 있습니다"
                    } else {
                        "등록 ${state.lockBackgroundGallery.size}장"
                    },
                    tint = OneUi.AccentTint,
                    showChevron = true,
                    onClick = actions.addBackgroundToGallery
                )
                // 등록된 사진을 칩 형태로 보여 주고 누르면 지워진다.
                if (state.lockBackgroundGallery.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.lockBackgroundGallery.forEach { uri ->
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                    .clickable { actions.removeBackground(uri) }
                            ) {
                                coil.compose.AsyncImage(
                                    model = uri,
                                    contentDescription = "배경 ${state.lockBackgroundGallery.indexOf(uri) + 1}번 (누르면 삭제)",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(18.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                }
                            }
                        }
                    }
                    SettingsToggleRow(
                        icon = Icons.Default.AutoAwesomeMotion,
                        title = "배경 자동 전환",
                        subtitle = if (state.lockBackgroundAutoRotate) {
                            "현재 ${state.lockBackgroundRotateSeconds}초마다 전환"
                        } else {
                            "2장 이상 등록해야 켤 수 있습니다"
                        },
                        tint = OneUi.OkTint,
                        checked = state.lockBackgroundAutoRotate,
                        enabled = state.lockBackgroundGallery.size > 1,
                        onCheckedChange = actions.toggleBackgroundAutoRotate
                    )
                    if (state.lockBackgroundAutoRotate) {
                        SettingsRow(
                            icon = Icons.Default.Timer,
                            title = "전환 간격",
                            subtitle = "짧게 누를 때마다 다음 값으로 바뀝니다",
                            tint = OneUi.InfoTint,
                            value = "${state.lockBackgroundRotateSeconds}초",
                            showChevron = true,
                            onClick = actions.cycleBackgroundRotateSeconds
                        )
                    }
                }
                SettingsRow(
                    icon = Icons.Default.PhoneAndroid,
                    title = "잠금 화면 비상 연락처",
                    tint = OneUi.WarnTint,
                    showChevron = true,
                    onClick = actions.configureEmergencyContact
                )
            }
        }

        item { SettingsSectionLabel("사생활 보호") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.VisibilityOff,
                    title = "사생활 보호 화면 켜기 · 끄기",
                    subtitle = "가림막으로 화면을 숨기고 캡처·녹화를 막습니다 (퀵세팅 타일에서도 사용 가능)",
                    tint = OneUi.WarnTint,
                    showChevron = true,
                    onClick = actions.togglePrivacyFilter
                )
            }
        }
    }
}

/** 데이터 · 금고 */
@Composable
fun VaultSectionScreen(state: SettingsUiState, actions: SettingsUiActions, onBack: () -> Unit) {
    var clipboardSheet by remember { mutableStateOf(false) }
    SectionScaffold(SettingsSection.VAULT, onBack) {
        item { SettingsSectionLabel("자세히 막기") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.ViewCarousel,
                    title = "앱 안 특정 화면만 잠그기",
                    subtitle = if (state.screenBlockAppCount == 0) {
                        "아직 기록된 화면이 없습니다"
                    } else {
                        "${state.screenBlockAppCount}개 앱 · ${state.screenBlockCount}개 화면 잠금"
                    },
                    tint = OneUi.InfoTint,
                    showChevron = true,
                    onClick = actions.openScreenBlockManager
                )
            }
        }
        item { SettingsSectionLabel("AI 설정") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.Key,
                    title = "AI 키 입력 · 변경",
                    subtitle = if (state.aiKeyConfigured) {
                        "현재 ${state.aiMaskedKey} · 모델 ${state.aiModelName}"
                    } else {
                        "아직 설정되지 않았습니다"
                    },
                    tint = if (state.aiKeyConfigured) OneUi.OkTint else OneUi.WarnTint,
                    showChevron = true,
                    onClick = actions.openAiKeyDialog
                )
                SettingsRow(
                    icon = Icons.Default.Psychology,
                    title = "AI 키 테스트",
                    subtitle = "키가 실제로 통하는지 한 번 확인합니다",
                    tint = OneUi.InfoTint,
                    showChevron = state.aiKeyConfigured,
                    enabled = state.aiKeyConfigured,
                    onClick = actions.testAiKey
                )
                SettingsStatusBanner(
                    tint = if (state.aiKeyConfigured) OneUi.OkTint else OneUi.DangerTint,
                    title = if (state.aiKeyConfigured) "AI 기능을 쓸 수 있습니다" else "AI 키가 없습니다",
                    description = if (state.aiKeyConfigured) {
                        "증거 AI 분석, 알림 요약, 잠금 해제 리포트 요약이 이 키를 씁니다."
                    } else {
                        "키를 넣으면 증거 AI 분석과 알림 요약이 동작합니다. 키는 Keystore 로 암호화되어 기기 안에만 저장됩니다."
                    },
                    actionLabel = if (state.aiKeyConfigured) null else "지금 입력",
                    onAction = actions.openAiKeyDialog
                )
            }
        }

        item { SettingsSectionLabel("보관") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.Lock,
                    title = "파일 금고",
                    subtitle = "암호화해서 기기 안에만 보관",
                    tint = OneUi.OkTint,
                    showChevron = true,
                    onClick = actions.openVault
                )
                SettingsRow(
                    icon = Icons.Default.Edit,
                    title = "보안 메모",
                    subtitle = "AES-GCM 암호화 메모",
                    tint = OneUi.InfoTint,
                    showChevron = true,
                    onClick = actions.openSecureNotes
                )
                SettingsRow(
                    icon = Icons.Default.CloudUpload,
                    title = "암호화 백업 · 복원",
                    subtitle = "재설치 대비 백업 파일 만들기",
                    tint = OneUi.AccentTint,
                    showChevron = true,
                    onClick = actions.manageBackup
                )
            }
        }

        item { SettingsSectionLabel("복구") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.VerifiedUser,
                    title = "오프라인 복구 QR 만들기",
                    subtitle = "비밀번호를 종이에 남겨 두고 QR 로 보관",
                    tint = OneUi.OkTint,
                    showChevron = true,
                    onClick = actions.showRecoveryQr
                )
                SettingsRow(
                    icon = Icons.Default.QrCodeScanner,
                    title = "복구 QR 스캔",
                    tint = OneUi.AccentTint,
                    showChevron = true,
                    onClick = actions.scanRecoveryQr
                )
            }
        }

        item { SettingsSectionLabel("개인정보") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsToggleRow(
                    icon = Icons.Default.Edit,
                    title = "클립보드 자동 삭제",
                    subtitle = "복사한 내용을 정해진 시간 뒤 지웁니다",
                    tint = OneUi.WarnTint,
                    checked = state.clipboardAutoClearEnabled,
                    onCheckedChange = actions.toggleClipboardAutoClear
                )
                if (state.clipboardAutoClearEnabled) {
                    SettingsRow(
                        icon = Icons.Default.Timer,
                        title = "삭제까지 시간",
                        tint = OneUi.AccentTint,
                        value = secondsLabel(state.clipboardClearSeconds),
                        showChevron = true,
                        onClick = { clipboardSheet = true }
                    )
                }
                SettingsRow(
                    icon = Icons.Default.Psychology,
                    title = "앱 실행 기록",
                    subtitle = if (state.sessionCount == 0) "기록 없음" else "최근 ${state.sessionCount}건",
                    tint = OneUi.InfoTint,
                    showChevron = true,
                    onClick = actions.showSessionLog
                )
                SettingsRow(
                    icon = Icons.Default.Delete,
                    title = "실행 기록 지우기",
                    tint = OneUi.DangerTint,
                    showChevron = true,
                    onClick = actions.clearSessionLog
                )
            }
        }
    }

    if (clipboardSheet) {
        SettingsChoiceSheet(
            title = "클립보드 삭제까지 시간",
            options = listOf(30 to "30초", 60 to "1분", 180 to "3분", 300 to "5분"),
            selectedIndex = listOf(30, 60, 180, 300).indexOf(state.clipboardClearSeconds).coerceAtLeast(0),
            onSelect = { actions.changeClipboardSeconds(it); clipboardSheet = false },
            onDismiss = { clipboardSheet = false }
        )
    }
}

private fun secondsLabel(seconds: Int): String = when {
    seconds < 60 -> "${seconds}초"
    else -> "${seconds / 60}분"
}

/** 위장 · 고급 */
@Composable
fun StealthSectionScreen(state: SettingsUiState, actions: SettingsUiActions, onBack: () -> Unit) {
    val config = state.lockConfig
    var sensitivitySheet by remember { mutableStateOf(false) }
    var fallbackSheet by remember { mutableStateOf(false) }
    var thresholdSheet by remember { mutableStateOf(false) }

    SectionScaffold(SettingsSection.STEALTH, onBack) {
        item { SettingsSectionLabel("위장") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.PhoneAndroid,
                    title = "위장 아이콘",
                    subtitle = "홈 화면에서 계산기 · 메모장으로 보입니다",
                    tint = OneUi.WarnTint,
                    showChevron = true,
                    onClick = actions.configureDisguise
                )
                SettingsToggleRow(
                    icon = Icons.Default.BugReport,
                    title = "가짜 오류 화면",
                    subtitle = "앱 실행 시 시스템 오류창으로 가장",
                    tint = OneUi.DangerTint,
                    checked = config.isFakeCrashEnabled,
                    onCheckedChange = actions.toggleFakeCrash
                )
                SettingsRow(
                    icon = Icons.Default.BugReport,
                    title = "앱별 가짜 화면",
                    subtitle = "빈 앨범 · 계산기 · 서비스 점검 등",
                    tint = OneUi.DangerTint,
                    showChevron = true,
                    onClick = actions.configureFakeScreen
                )
                SettingsRow(
                    icon = Icons.Default.Key,
                    title = "듀레스 · 미끼 PIN",
                    subtitle = "비상 상황용 다른 PIN 설정",
                    tint = OneUi.WarnTint,
                    showChevron = true,
                    onClick = actions.configureDuressPin
                )
            }
        }

        item { SettingsSectionLabel("AI 가드") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsToggleRow(
                    icon = Icons.Default.Psychology,
                    title = "이상 행동 감지",
                    subtitle = "평소와 다른 입력 패턴이면 추가 인증",
                    tint = OneUi.InfoTint,
                    checked = config.isAiGuardEnabled,
                    onCheckedChange = actions.toggleAiGuard
                )
                if (config.isAiGuardEnabled) {
                    SettingsRow(
                        icon = Icons.Default.Timer,
                        title = "감지 민감도",
                        subtitle = when (config.aiGuardSensitivity) {
                            1 -> "느슨하게 (오탐 적음)"
                            3 -> "엄격하게"
                            else -> "보통"
                        },
                        tint = OneUi.AccentTint,
                        showChevron = true,
                        onClick = { sensitivitySheet = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Security,
                        title = "추가 인증 방식",
                        subtitle = config.aiGuardFallback.title,
                        tint = OneUi.AccentTint,
                        showChevron = true,
                        onClick = { fallbackSheet = true }
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.Mic,
                        title = "음성 기록",
                        subtitle = "인증 실패 시 5초 음성을 남깁니다",
                        tint = OneUi.DangerTint,
                        checked = config.isAiGuardVoiceRecordingEnabled,
                        onCheckedChange = actions.toggleAiGuardVoice
                    )
                    SettingsRow(
                        icon = Icons.Default.Delete,
                        title = "학습 데이터 초기화",
                        tint = OneUi.DangerTint,
                        showChevron = true,
                        onClick = actions.resetAiGuardLearning
                    )
                }
            }
        }

        item { SettingsSectionLabel("침입 대응") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsToggleRow(
                    icon = Icons.Default.VisibilityOff,
                    title = "침입 증거 촬영",
                    subtitle = "실패할 때 selfie · 동영상 · 음성 기록",
                    tint = OneUi.DangerTint,
                    checked = config.isIntruderSelfieEnabled,
                    onCheckedChange = actions.toggleIntruderSelfie
                )
                if (config.isIntruderSelfieEnabled) {
                    SettingsRow(
                        icon = Icons.Default.Timer,
                        title = "촬영 기준",
                        subtitle = "${config.intruderSelfieThreshold}회 실패 시",
                        tint = OneUi.AccentTint,
                        showChevron = true,
                        onClick = { thresholdSheet = true }
                    )
                }
                SettingsToggleRow(
                    icon = Icons.Default.BugReport,
                    title = "침입 경보음",
                    subtitle = "실패가 반복되면 큰 소리로 알립니다",
                    tint = OneUi.WarnTint,
                    checked = config.isIntruderSirenEnabled,
                    onCheckedChange = actions.toggleIntruderSiren
                )
                SettingsToggleRow(
                    icon = Icons.Default.PhoneAndroid,
                    title = "긴급 흔들기",
                    subtitle = "세게 흔들면 모든 잠금을 즉시 재설정",
                    tint = OneUi.DangerTint,
                    checked = config.isPanicShakeEnabled,
                    onCheckedChange = actions.togglePanicShake
                )
            }
        }

        item { SettingsSectionLabel("앱") }
        item {
            SettingsCard(accent = OneUi.CardSurface) {
                SettingsRow(
                    icon = Icons.Default.DownloadDone,
                    title = "업데이트 확인",
                    subtitle = "GitHub 릴리스에서 새 버전을 받습니다",
                    tint = OneUi.OkTint,
                    showChevron = true,
                    onClick = actions.checkForUpdates
                )
                SettingsRow(
                    icon = Icons.Default.Security,
                    title = "권한 설정 도우미",
                    tint = OneUi.AccentTint,
                    showChevron = true,
                    onClick = actions.openPermissionWizard
                )
            }
        }
    }

    if (sensitivitySheet) {
        SettingsChoiceSheet(
            title = "감지 민감도",
            options = listOf(1 to "느슨하게", 2 to "보통", 3 to "엄격하게"),
            selectedIndex = (config.aiGuardSensitivity - 1).coerceIn(0, 2),
            onSelect = { actions.changeAiGuardSensitivity(it + 1); sensitivitySheet = false },
            onDismiss = { sensitivitySheet = false }
        )
    }
    if (fallbackSheet) {
        SettingsChoiceSheet(
            title = "추가 인증 방식",
            options = AiGuardFallback.entries.map { it to it.title },
            selectedIndex = AiGuardFallback.entries.indexOf(config.aiGuardFallback),
            onSelect = { actions.changeAiGuardFallback(it); fallbackSheet = false },
            onDismiss = { fallbackSheet = false }
        )
    }
    if (thresholdSheet) {
        SettingsChoiceSheet(
            title = "침입 증거 촬영 기준",
            options = listOf(1 to "1회 실패", 2 to "2회 실패", 3 to "3회 실패"),
            selectedIndex = (config.intruderSelfieThreshold - 1).coerceIn(0, 2),
            onSelect = { actions.changeIntruderThreshold(it + 1); thresholdSheet = false },
            onDismiss = { thresholdSheet = false }
        )
    }
}