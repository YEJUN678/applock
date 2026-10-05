package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.util.BiometricHelper
import com.example.util.BiometricStatus
import com.example.util.IntruderCameraHelper
import java.io.File
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.TimePickerDialog
import com.example.model.AppItem
import com.example.model.BackgroundTheme
import com.example.model.IntruderLog
import com.example.model.LockConfig
import com.example.model.LockType
import com.example.model.AiGuardFallback
import com.example.util.BehavioralGuard
import com.example.util.AppSession
import android.widget.Toast
import android.widget.VideoView
import android.widget.MediaController
import android.net.Uri
import com.example.ui.components.AppGridCard
import com.example.ui.components.AppItemCard
import com.example.ui.components.AppListControlBar
import com.example.ui.components.AppSortOrder
import com.example.ui.components.AppViewMode
import com.example.ui.components.applyListOptions
import com.example.ui.components.BatchTimeoutDialog
import com.example.ui.components.ChangeCalculatorCodeModal
import com.example.ui.components.ChangeKnockCodeModal
import com.example.ui.components.ChangePasswordModal
import com.example.ui.components.ChangePinModal
import com.example.ui.components.AppScheduleSheet
import com.example.ui.components.ClipboardAutoClearCard
import com.example.ui.components.ExcludedAppsSheet
import com.example.ui.components.FailedAttemptLimitCard
import com.example.ui.components.NxNPatternLockView
import com.example.ui.components.SecurityMetric
import com.example.ui.components.SessionTimelineCard
import com.example.ui.screens.settings.SettingsRouter
import com.example.ui.screens.settings.SettingsUiActions
import com.example.ui.screens.settings.SettingsUiState
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Checklist
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockerHomeScreen(
    apps: List<AppItem>,
    isLoadingApps: Boolean,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    hasUsageStatsPermission: Boolean,
    lockConfig: LockConfig,
    intruderLogs: List<IntruderLog> = emptyList(),
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestUsageStatsPermission: () -> Unit,
    onRequestAppDetailsSettings: () -> Unit,
    onRefreshApps: () -> Unit,
    onToggleLock: (String, Boolean) -> Unit,
    onLockAll: () -> Unit,
    onUnlockAll: () -> Unit,
    onBatchLock: (Set<String>, Boolean) -> Unit = { _, _ -> },
    onBatchTimeout: (Int) -> Unit = {},
    onClearIntruderLogs: () -> Unit,
    onDeleteIntruderLog: (String) -> Unit = {},
    onDownloadIntruderPhoto: (String) -> Unit = {},
    onCaptureTestSelfie: () -> Unit = {},
    onLockConfigChanged: (LockConfig) -> Unit,
    onTestLaunchApp: (AppItem) -> Unit,
    onOpenVault: () -> Unit = {},
    onOpenSecureNotes: () -> Unit = {},
    isLostModeActive: Boolean = false,
    onActivateLostMode: () -> Unit = {},
    onDisableLostMode: () -> Unit = {},
    onOpenLostModeMap: () -> Unit = {},
    onTogglePrivacyFilter: () -> Unit = {},
    onConfigureDisguise: () -> Unit = {},
    onManageBackup: () -> Unit = {},
    onShowRecoveryQr: () -> Unit = {},
    onScanRecoveryQr: () -> Unit = {},
    onPickCustomLockBackground: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onToggleScreenOffLock: (Boolean) -> Unit = {},
    onConfigureFakeScreen: (List<AppItem>) -> Unit = {},
    onConfigureDuressPin: () -> Unit = {},
    onConfigureEmergencyContact: () -> Unit = {},
    onEditLockStyle: () -> Unit = {},
    onOpenPermissionWizard: () -> Unit = {},
    onOpenExcludedApps: () -> Unit = {},
    onOpenAppSchedules: () -> Unit = {},
    recoveryConfigured: Boolean = false,
    onOpenRecoverySetup: () -> Unit = {},
    recoveryKeyConfigured: Boolean = false,
    recoveryKeyFailedAttempts: Int = 0,
    onOpenRecoveryKeySetup: () -> Unit = {},
    maxFailedAttempts: Int = 0,
    lockoutMinutes: Int = 5,
    onChangeMaxFailedAttempts: (Int) -> Unit = {},
    onChangeLockoutMinutes: (Int) -> Unit = {},
    clipboardAutoClearEnabled: Boolean = false,
    clipboardClearSeconds: Int = 60,
    onToggleClipboardAutoClear: (Boolean) -> Unit = {},
    onChangeClipboardClearSeconds: (Int) -> Unit = {},
    sessionLog: List<AppSession> = emptyList(),
    onClearSessionLog: () -> Unit = {},
    onToggleNotificationPrivacy: (Boolean) -> Unit = {},
    isFaceDownProtectionEnabled: Boolean = false,
    onToggleFaceDownProtection: (Boolean) -> Unit = {},
    onRequestAiGuardAudioPermission: () -> Unit = {},
    notificationHighlightEnabled: Boolean = false,
    notificationHighlightStyle: com.example.util.NotificationHighlighter.Style = com.example.util.NotificationHighlighter.Style.BLUE_VIOLET,
    aiSummaryEnabled: Boolean = false,
    aiKeyConfigured: Boolean = false,
    onToggleNotificationHighlight: (Boolean) -> Unit = {},
    onCycleHighlightStyle: () -> Unit = {},
    onToggleAiSummary: (Boolean) -> Unit = {},
    onAddBackgroundToGallery: () -> Unit = {},
    onRemoveBackground: (String) -> Unit = {},
    onToggleBackgroundAutoRotate: (Boolean) -> Unit = {},
    onCycleBackgroundRotateSeconds: () -> Unit = {},
    isDuressMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val appContext = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(1) } // 0: 잠긴 앱, 1: 설치된 앱, 2: 침입 기록, 3: 설정, 4: 홈 대시보드
    var searchQuery by remember { mutableStateOf("") }
    var appViewMode by remember { mutableStateOf(AppViewMode.LIST) }
    var appSortOrder by remember { mutableStateOf(AppSortOrder.NAME_ASC) }
    var appCategoryFilter by remember { mutableStateOf<String?>(null) }
    var showStatsScreen by remember { mutableStateOf(false) }
    var showSetPatternSheet by remember { mutableStateOf(false) }
    var showSetPinSheet by remember { mutableStateOf(false) }
    var showSetPasswordSheet by remember { mutableStateOf(false) }
    var showSetCalculatorSheet by remember { mutableStateOf(false) }
    var showSetKnockSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showBatchTimeoutDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedPackages by remember { mutableStateOf(setOf<String>()) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }
    val lockedApps = remember(filteredApps) { filteredApps.filter { it.isLocked } }
    val unlockedApps = remember(filteredApps) { filteredApps.filter { !it.isLocked } }
    // 정렬 · 카테고리 필터를 적용한 실제 표시 목록
    val optionsApplied = remember(filteredApps, appSortOrder, appCategoryFilter) {
        filteredApps.applyListOptions(appSortOrder, appCategoryFilter)
    }
    val visibleLockedApps = remember(optionsApplied) { optionsApplied.filter { it.isLocked } }
    val visibleUnlockedApps = remember(optionsApplied) { optionsApplied.filter { !it.isLocked } }
    val appCategories = remember(filteredApps) {
        (listOf("설치된 앱") + filteredApps.map { it.category }.distinct()).take(6)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBgDark)
    ) {
        // Top Header
        Surface(
            color = CyberSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header Top Row: Title + Top Action Icons (Theme, Settings, Refresh)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Lock & Vault",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "내 폰 실제 앱 ${apps.size}개 감지됨 (${lockConfig.gridSize}x${lockConfig.gridSize} 그리드)",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }

                    // Compact folder: non-essential areas are grouped under one menu.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(onClick = { showMoreMenu = !showMoreMenu }, modifier = Modifier.size(38.dp)) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "더보기", tint = TextPrimary)
                            }
                            val moreScrollState = rememberScrollState()
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showMoreMenu,
                                enter = fadeIn() + scaleIn(initialScale = 0.78f),
                                exit = fadeOut() + scaleOut(targetScale = 0.82f),
                                modifier = Modifier.align(Alignment.TopEnd).padding(top = 42.dp)
                            ) {
                                Surface(
                                    // 화면이 좁을 때 2줄로 접힌 메뉴가 옆으로 넘치지 않게 가로 스크롤을 허용한다.
                                    modifier = Modifier.widthIn(max = 320.dp).horizontalScroll(moreScrollState),
                                    shape = RoundedCornerShape(18.dp),
                                    color = CyberCardDark,
                                    shadowElevation = 12.dp,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                                ) {
                                    // 항목이 많아서 한 줄로 넘치므로 두 줄로 접고, 화면이 더 좁으면 가로 스크롤된다.
                                    // 동작은 (라벨, () -> Unit) 로 담아둔다. @Composable 을 붙이면
                                    // onClick 안에서 호출할 수 없어 컴파일이 깨진다.
                                    val moreItems = buildList<Pair<String, () -> Unit>> {
                                        if (!isDuressMode) add("홈 대시보드" to { selectedTab = 4 })
                                        if (!isDuressMode) add("통계" to { selectedTab = 5 })
                                        if (!isDuressMode) add("증거 AI 분석" to { selectedTab = 6 })
                                        add("금고" to { onOpenVault() })
                                        add("보안 메모" to { onOpenSecureNotes() })
                                        add("사생활 필름" to { onTogglePrivacyFilter() })
                                        if (!isDuressMode) add("침입자 기록" to { selectedTab = 2 })
                                        if (!isDuressMode) add("설정 & 기능" to { selectedTab = 3 })
                                        add("새로고침" to { onRefreshApps() })
                                    }
                                    val moreItemIcons = listOf<androidx.compose.ui.graphics.vector.ImageVector>(
                                        Icons.Default.Home,
                                        Icons.Default.InsertChart,
                                        Icons.Default.Psychology,
                                        Icons.Default.EnhancedEncryption,
                                        Icons.Default.EnhancedEncryption,
                                        Icons.Default.VisibilityOff,
                                        Icons.Default.PhotoCamera,
                                        Icons.Default.Settings,
                                        Icons.Default.Refresh
                                    )
                                    val moreItemTints = listOf(
                                        NeonGreen, NeonCyan, NeonPurple, NeonGreen, NeonPurple,
                                        NeonAmber, NeonRed, NeonCyan, TextPrimary
                                    )
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        moreItems.chunked(5).forEachIndexed { rowIndex, rowItems ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                rowItems.forEachIndexed { indexInRow, entry ->
                                                    val absoluteIndex = rowIndex * 5 + indexInRow
                                                    MoreMenuItem(
                                                        label = entry.first,
                                                        icon = moreItemIcons[absoluteIndex],
                                                        tint = moreItemTints[absoluteIndex],
                                                        onClick = { showMoreMenu = false; entry.second() }
                                                    )
                                                }
                                            }
                                        }
                                        if (moreScrollState.maxValue > 0) {
                                            Text(
                                                text = "좌우로 밀어 보세요",
                                                color = TextSecondary,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(top = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Protection Status Badge
                val isFullyArmed = hasAccessibilityPermission && hasOverlayPermission
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = (if (isFullyArmed) NeonGreen else NeonAmber).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isFullyArmed) NeonGreen else NeonAmber
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (isFullyArmed) NeonGreen else NeonAmber,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFullyArmed) "실시간 즉각 잠금 감시 가동 중" else "실시간 잠금 권한 필요 (접근성/오버레이)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Quick toggle button if not armed
                        if (!isFullyArmed) {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    if (!hasAccessibilityPermission) onRequestAccessibilityPermission()
                                    else if (!hasOverlayPermission) onRequestOverlayPermission()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("권한 켜기 ▶", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Android 13/14+ "제한된 설정" guide banner if accessibility is blocked
                if (!hasAccessibilityPermission) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NeonCyan.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "실시간 앱 가로채기 (접근성 서비스)",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "잠긴 앱 실행 즉시 화면을 띄우고 뒤로가기 시 실행을 차단합니다. 접근성이 비활성화(회색)되어 있다면 안드로이드 13/14 '제한된 설정 허용'이 필요합니다.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = onRequestAccessibilityPermission,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonCyan,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("접근성 설정 열기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onRequestAppDetailsSettings,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.2f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("제한된 설정 허용 바로가기", color = NeonAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Overlay Permission Banner if not granted
                if (!hasOverlayPermission) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NeonAmber.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = NeonAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "다른 앱 위에 표시 권한 필요",
                                    color = NeonAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "잠긴 앱 실행 시 즉각 잠금 화면을 띄우려면 권한이 필요합니다.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onRequestOverlayPermission,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonAmber,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("권한 허용", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Actions: Batch Lock All & Unlock All
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onLockAll,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("전체 잠금", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onUnlockAll,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("전체 해제", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Segmented Tab Selector (0: Locked, 1: Unlocked, 2: Intruder Selfie 📸, 3: Settings)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!isDuressMode) FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("잠김 (${lockedApps.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonCyan,
                    selectedLabelColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("안 잠김 (${unlockedApps.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonCyan,
                    selectedLabelColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.1f)
            )

        }

        // Search Bar for apps (when in tab 0 or 1)
        if (selectedTab == 0 || selectedTab == 1) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("앱 이름 또는 패키지명 검색...", color = TextSecondary, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NeonCyan)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "검색어 지우기", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CyberCardDark,
                    unfocusedContainerColor = CyberCardDark,
                    focusedIndicatorColor = NeonCyan,
                    unfocusedIndicatorColor = CyberBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Multi-select Control Bar
            val currentTabApps = if (selectedTab == 0) lockedApps else unlockedApps
            Surface(
                color = if (isSelectionMode) CyberCardDark else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                border = if (isSelectionMode) androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                if (!isSelectionMode) {
                    Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (selectedTab == 0) "🔒 잠긴 앱 (${lockedApps.size}개)" else "📱 설치된 앱 (${unlockedApps.size}개)",
                            maxLines = 1,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(
                            onClick = {
                                isSelectionMode = true
                                selectedPackages = emptySet()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("다중 선택 모드", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // 보기 방식 · 정렬 · 카테고리 필터
                    if (selectedTab == 0 || selectedTab == 1) {
                        AppListControlBar(
                            title = if (selectedTab == 0) "잠긴 앱" else "설치된 앱",
                            count = if (selectedTab == 0) visibleLockedApps.size else visibleUnlockedApps.size,
                            viewMode = appViewMode,
                            onViewModeChange = { appViewMode = it },
                            sortOrder = appSortOrder,
                            onSortOrderChange = { appSortOrder = it },
                            categories = appCategories,
                            selectedCategory = appCategoryFilter,
                            onCategoryChange = { appCategoryFilter = it },
                            onBatchSelect = { isSelectionMode = true; selectedPackages = emptySet() }
                        )
                    }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "선택됨: ${selectedPackages.size}개",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (selectedPackages.size == currentTabApps.size && currentTabApps.isNotEmpty()) "전체 해제" else "전체 선택",
                                    color = NeonAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        selectedPackages = if (selectedPackages.size == currentTabApps.size && currentTabApps.isNotEmpty()) {
                                            emptySet()
                                        } else {
                                            currentTabApps.map { it.packageName }.toSet()
                                        }
                                    }
                                )
                            }

                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedPackages = emptySet()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "다중 선택 종료", tint = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Batch Action Buttons: Lock, Unlock, Re-lock Timeout
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    if (selectedPackages.isNotEmpty()) {
                                        onBatchLock(selectedPackages, true)
                                        isSelectionMode = false
                                        selectedPackages = emptySet()
                                    }
                                },
                                enabled = selectedPackages.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("일괄 잠금", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (selectedPackages.isNotEmpty()) {
                                        onBatchLock(selectedPackages, false)
                                        isSelectionMode = false
                                        selectedPackages = emptySet()
                                    }
                                },
                                enabled = selectedPackages.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("일괄 해제", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showBatchTimeoutDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("재잠금 시간", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isLoadingApps) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(color = NeonCyan)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "스마트폰에 설치된 앱 목록을 불러오는 중...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                when (selectedTab) {
                    4 -> {
                        if (!isDuressMode) {
                            HomeDashboardScreen(
                                lockConfig = lockConfig,
                                lockedAppCount = lockedApps.size,
                                totalAppCount = apps.size,
                                hasAccessibilityPermission = hasAccessibilityPermission,
                                hasOverlayPermission = hasOverlayPermission,
                                hasUsageStatsPermission = hasUsageStatsPermission,
                                sessionLog = sessionLog,
                                isLostModeActive = isLostModeActive,
                                onOpenPermissionWizard = { onOpenPermissionWizard() },
                                onOpenSettings = { selectedTab = 3 },
                                onOpenLogs = { selectedTab = 2 },
                                onOpenVault = { onOpenVault() }
                            )
                        } else {
                            EmptyAppsView(title = "홈", desc = "사용할 수 없습니다.")
                        }
                    }
                    0 -> {
                        AppListBody(
                            apps = visibleLockedApps,
                            isEmpty = lockedApps.isEmpty(),
                            emptyTitle = "잠긴 앱이 없습니다",
                            emptyDesc = "'설치된 앱'에서 잠그고 싶은 앱을 선택하거나, '전체 잠금'을 눌러보세요.",
                            viewMode = appViewMode,
                            isSelectionMode = isSelectionMode,
                            selectedPackages = selectedPackages,
                            onSelectToggle = { pkg ->
                                selectedPackages = if (selectedPackages.contains(pkg)) selectedPackages - pkg else selectedPackages + pkg
                            },
                            onToggleLock = onToggleLock,
                            onTestLaunch = onTestLaunchApp
                        )
                    }
                    1 -> {
                        AppListBody(
                            apps = visibleUnlockedApps,
                            isEmpty = unlockedApps.isEmpty(),
                            emptyTitle = if (searchQuery.isNotEmpty()) "'$searchQuery' 검색 결과 없음" else "표시할 앱이 없습니다",
                            emptyDesc = if (searchQuery.isNotEmpty()) "해당 키워드와 일치하는 설치 앱이 없습니다."
                            else "설치된 모든 앱이 잠겨 있거나 목록을 불러오지 못했습니다. 상단 새로고침을 눌러보세요.",
                            viewMode = appViewMode,
                            isSelectionMode = isSelectionMode,
                            selectedPackages = selectedPackages,
                            onSelectToggle = { pkg ->
                                selectedPackages = if (selectedPackages.contains(pkg)) selectedPackages - pkg else selectedPackages + pkg
                            },
                            onToggleLock = onToggleLock,
                            onTestLaunch = onTestLaunchApp
                        )
                    }
                    5 -> if (!isDuressMode) {
                        StatsScreen(
                            lockConfig = lockConfig,
                            sessionLog = sessionLog,
                            intruderLogs = intruderLogs,
                            lockedAppCount = lockedApps.size,
                            totalAppCount = apps.size,
                            onBack = { selectedTab = 1 }
                        )
                    }
                    6 -> if (!isDuressMode) {
                        EvidenceAiScreen(logs = intruderLogs, onBack = { selectedTab = 2 })
                    }
                    2 -> if (!isDuressMode) {
                        IntruderSelfieVaultView(
                            logs = intruderLogs,
                            lockConfig = lockConfig,
                            onClearLogs = onClearIntruderLogs,
                            onDeleteLog = onDeleteIntruderLog,
                            onCaptureTestSelfie = onCaptureTestSelfie,
                            onDownloadIntruderPhoto = onDownloadIntruderPhoto,
                            onUpdateConfig = onLockConfigChanged
                        )
                    }
                    3 -> if (!isDuressMode) {
                        SettingsView(
                            lockConfig = lockConfig,
                            lockedAppCount = lockedApps.size,
                            hasOverlayPermission = hasOverlayPermission,
                            hasAccessibilityPermission = hasAccessibilityPermission,
                            hasUsageStatsPermission = hasUsageStatsPermission,
                            onRequestOverlayPermission = onRequestOverlayPermission,
                            onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                            onRequestUsageStatsPermission = onRequestUsageStatsPermission,
                            onRequestAppDetailsSettings = onRequestAppDetailsSettings,
                            onChangeLockType = { newType ->
                                onLockConfigChanged(lockConfig.copy(lockType = newType))
                            },
                            onChangePattern = { showSetPatternSheet = true },
                            onChangePin = { showSetPinSheet = true },
                            onChangePassword = { showSetPasswordSheet = true },
                            onChangeCalculatorCode = { showSetCalculatorSheet = true },
                            onChangeKnockCode = { showSetKnockSheet = true },
                            onChangeLockTimeout = { newTimeout ->
                                onLockConfigChanged(lockConfig.copy(lockTimeoutSeconds = newTimeout))
                            },
                            onToggleVibration = {
                                onLockConfigChanged(lockConfig.copy(isVibrationEnabled = it))
                            },
                            onToggleUninstallProtection = {
                                onLockConfigChanged(lockConfig.copy(isUninstallProtectionEnabled = it))
                            },
                            onToggleAppSelfProtect = {
                                onLockConfigChanged(lockConfig.copy(isAppSelfProtectEnabled = it))
                            },
                            onChangeTheme = { showThemeSheet = true },
                            onToggleBiometric = {
                                onLockConfigChanged(lockConfig.copy(biometricEnabled = it))
                            },
                            onToggleStealthPattern = {
                                onLockConfigChanged(lockConfig.copy(isStealthPattern = it))
                            },
                            onToggleFakeCrash = {
                                onLockConfigChanged(lockConfig.copy(isFakeCrashEnabled = it))
                            },
                            onConfigureFakeScreen = { onConfigureFakeScreen(lockedApps) },
                            onToggleIntruderSelfie = {
                                onLockConfigChanged(lockConfig.copy(isIntruderSelfieEnabled = it))
                            },
                            onChangeIntruderSelfieThreshold = {
                                onLockConfigChanged(lockConfig.copy(intruderSelfieThreshold = it))
                            },
                            onOpenVault = onOpenVault,
                            onOpenSecureNotes = onOpenSecureNotes,
                            isLostModeActive = isLostModeActive,
                            onActivateLostMode = onActivateLostMode,
                            onDisableLostMode = onDisableLostMode,
                            onOpenLostModeMap = onOpenLostModeMap,
                            onTogglePrivacyFilter = onTogglePrivacyFilter,
                            onConfigureDisguise = onConfigureDisguise,
                            onManageBackup = onManageBackup,
                            onShowRecoveryQr = onShowRecoveryQr,
                            onScanRecoveryQr = onScanRecoveryQr,
                            onPickCustomLockBackground = onPickCustomLockBackground,
                            onCheckForUpdates = onCheckForUpdates,
                            onToggleScreenOffLock = onToggleScreenOffLock,
                            onUpdateConfig = onLockConfigChanged,
                            onConfigureDuressPin = onConfigureDuressPin,
                            onConfigureEmergencyContact = onConfigureEmergencyContact,
                            onEditLockStyle = onEditLockStyle,
                            onOpenPermissionWizard = onOpenPermissionWizard,
                            onOpenExcludedApps = onOpenExcludedApps,
                            onOpenAppSchedules = onOpenAppSchedules,
                            recoveryConfigured = recoveryConfigured,
                            onOpenRecoverySetup = onOpenRecoverySetup,
                            recoveryKeyConfigured = recoveryKeyConfigured,
                            recoveryKeyFailedAttempts = recoveryKeyFailedAttempts,
                            onOpenRecoveryKeySetup = onOpenRecoveryKeySetup,
                            maxFailedAttempts = maxFailedAttempts,
                            lockoutMinutes = lockoutMinutes,
                            onChangeMaxFailedAttempts = onChangeMaxFailedAttempts,
                            onChangeLockoutMinutes = onChangeLockoutMinutes,
                            clipboardAutoClearEnabled = clipboardAutoClearEnabled,
                            clipboardClearSeconds = clipboardClearSeconds,
                            onToggleClipboardAutoClear = onToggleClipboardAutoClear,
                            onChangeClipboardClearSeconds = onChangeClipboardClearSeconds,
                            sessionLog = sessionLog,
                            onClearSessionLog = onClearSessionLog,
                            onToggleNotificationPrivacy = onToggleNotificationPrivacy,
                            notificationHighlightEnabled = notificationHighlightEnabled,
                            notificationHighlightStyle = notificationHighlightStyle,
                            aiSummaryEnabled = aiSummaryEnabled,
                            aiKeyConfigured = aiKeyConfigured,
                            onToggleNotificationHighlight = onToggleNotificationHighlight,
                            onCycleHighlightStyle = onCycleHighlightStyle,
                            onToggleAiSummary = onToggleAiSummary,
                            onAddBackgroundToGallery = onAddBackgroundToGallery,
                            onRemoveBackground = onRemoveBackground,
                            onToggleBackgroundAutoRotate = onToggleBackgroundAutoRotate,
                            onCycleBackgroundRotateSeconds = onCycleBackgroundRotateSeconds,
                            isFaceDownProtectionEnabled = isFaceDownProtectionEnabled,
                            onToggleFaceDownProtection = onToggleFaceDownProtection,
                            onToggleRandomPin = {
                                onLockConfigChanged(lockConfig.copy(isRandomPinKeypad = it))
                            },
                            onToggleAiGuard = {
                                onLockConfigChanged(lockConfig.copy(isAiGuardEnabled = it))
                            },
                            onChangeAiGuardSensitivity = {
                                onLockConfigChanged(lockConfig.copy(aiGuardSensitivity = it))
                            },
                            onChangeAiGuardFallback = {
                                onLockConfigChanged(lockConfig.copy(aiGuardFallback = it))
                            },
                            onToggleAiGuardVoiceRecording = {
                                onLockConfigChanged(lockConfig.copy(isAiGuardVoiceRecordingEnabled = it))
                                if (it) onRequestAiGuardAudioPermission()
                            },
                            onRequestAiGuardAudioPermission = onRequestAiGuardAudioPermission,
                            onResetAiGuardLearning = {
                                BehavioralGuard.reset(appContext)
                                Toast.makeText(appContext, "AI 가드 학습 데이터를 초기화했습니다.", Toast.LENGTH_SHORT).show()
                            },
                            onToggleIntruderSiren = {
                                onLockConfigChanged(lockConfig.copy(isIntruderSirenEnabled = it))
                            },
                            onTogglePanicShake = {
                                onLockConfigChanged(lockConfig.copy(isPanicShakeEnabled = it))
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet: Change Pattern & Grid Size
    if (showSetPatternSheet) {
        ChangePatternModal(
            currentConfig = lockConfig,
            onDismiss = { showSetPatternSheet = false },
            onSave = { newSize, newPattern ->
                onLockConfigChanged(
                    lockConfig.copy(gridSize = newSize, savedPattern = newPattern)
                )
                showSetPatternSheet = false
            }
        )
    }

    // Modal Sheet: Change PIN
    if (showSetPinSheet) {
        ChangePinModal(
            currentPin = lockConfig.savedPin,
            onDismiss = { showSetPinSheet = false },
            onSave = { newPin ->
                onLockConfigChanged(
                    lockConfig.copy(savedPin = newPin)
                )
                showSetPinSheet = false
            }
        )
    }

    // Modal Sheet: Change Password
    if (showSetPasswordSheet) {
        ChangePasswordModal(
            currentPassword = lockConfig.savedPassword,
            onDismiss = { showSetPasswordSheet = false },
            onSave = { newPassword ->
                onLockConfigChanged(
                    lockConfig.copy(savedPassword = newPassword)
                )
                showSetPasswordSheet = false
            }
        )
    }

    // Modal Sheet: Change Calculator Code
    if (showSetCalculatorSheet) {
        ChangeCalculatorCodeModal(
            currentCode = lockConfig.savedCalculatorCode,
            onDismiss = { showSetCalculatorSheet = false },
            onSave = { newCode ->
                onLockConfigChanged(
                    lockConfig.copy(savedCalculatorCode = newCode)
                )
                showSetCalculatorSheet = false
            }
        )
    }

    // Modal Sheet: Change Knock Code
    if (showSetKnockSheet) {
        ChangeKnockCodeModal(
            currentCode = lockConfig.savedKnockCode,
            onDismiss = { showSetKnockSheet = false },
            onSave = { newKnockCode ->
                onLockConfigChanged(
                    lockConfig.copy(savedKnockCode = newKnockCode)
                )
                showSetKnockSheet = false
            }
        )
    }

    // Dialog: Batch Timeout Setting
    if (showBatchTimeoutDialog) {
        BatchTimeoutDialog(
            selectedCount = selectedPackages.size,
            currentTimeoutSeconds = lockConfig.lockTimeoutSeconds,
            onDismiss = { showBatchTimeoutDialog = false },
            onTimeoutSelected = { newTimeout ->
                onBatchTimeout(newTimeout)
                showBatchTimeoutDialog = false
            }
        )
    }

    // Modal Sheet: Change Theme
    if (showThemeSheet) {
        ChangeThemeModal(
            currentTheme = lockConfig.backgroundTheme,
            onDismiss = { showThemeSheet = false },
            onSelectTheme = { selectedTheme ->
                onLockConfigChanged(lockConfig.copy(backgroundTheme = selectedTheme))
                showThemeSheet = false
            }
        )
    }
}

@Composable
fun SettingsView(
    lockConfig: LockConfig,
    lockedAppCount: Int = 0,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    hasUsageStatsPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestUsageStatsPermission: () -> Unit,
    onRequestAppDetailsSettings: () -> Unit,
    onChangeLockType: (LockType) -> Unit,
    onChangePattern: () -> Unit,
    onChangePin: () -> Unit,
    onChangePassword: () -> Unit = {},
    onChangeCalculatorCode: () -> Unit = {},
    onChangeKnockCode: () -> Unit = {},
    onChangeLockTimeout: (Int) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onToggleUninstallProtection: (Boolean) -> Unit = {},
    onToggleAppSelfProtect: (Boolean) -> Unit,
    onChangeTheme: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onToggleStealthPattern: (Boolean) -> Unit,
    onToggleFakeCrash: (Boolean) -> Unit,
    onConfigureFakeScreen: () -> Unit = {},
    onToggleIntruderSelfie: (Boolean) -> Unit = {},
    onChangeIntruderSelfieThreshold: (Int) -> Unit = {},
    onOpenVault: () -> Unit = {},
    onOpenSecureNotes: () -> Unit = {},
    isLostModeActive: Boolean = false,
    onActivateLostMode: () -> Unit = {},
    onDisableLostMode: () -> Unit = {},
    onOpenLostModeMap: () -> Unit = {},
    onTogglePrivacyFilter: () -> Unit = {},
    onConfigureDisguise: () -> Unit = {},
    onManageBackup: () -> Unit = {},
    onShowRecoveryQr: () -> Unit = {},
    onScanRecoveryQr: () -> Unit = {},
    onPickCustomLockBackground: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onToggleScreenOffLock: (Boolean) -> Unit = {},
    onUpdateConfig: (LockConfig) -> Unit = {},
    onConfigureDuressPin: () -> Unit = {},
    onConfigureEmergencyContact: () -> Unit = {},
    onEditLockStyle: () -> Unit = {},
    onOpenPermissionWizard: () -> Unit = {},
    onOpenExcludedApps: () -> Unit = {},
    onOpenAppSchedules: () -> Unit = {},
    recoveryConfigured: Boolean = false,
    onOpenRecoverySetup: () -> Unit = {},
    recoveryKeyConfigured: Boolean = false,
    recoveryKeyFailedAttempts: Int = 0,
    onOpenRecoveryKeySetup: () -> Unit = {},
    maxFailedAttempts: Int = 0,
    lockoutMinutes: Int = 5,
    onChangeMaxFailedAttempts: (Int) -> Unit = {},
    onChangeLockoutMinutes: (Int) -> Unit = {},
    clipboardAutoClearEnabled: Boolean = false,
    clipboardClearSeconds: Int = 60,
    onToggleClipboardAutoClear: (Boolean) -> Unit = {},
    onChangeClipboardClearSeconds: (Int) -> Unit = {},
    sessionLog: List<AppSession> = emptyList(),
    onClearSessionLog: () -> Unit = {},
    onToggleNotificationPrivacy: (Boolean) -> Unit = {},
    isFaceDownProtectionEnabled: Boolean = false,
    onToggleFaceDownProtection: (Boolean) -> Unit = {},
    onToggleRandomPin: (Boolean) -> Unit = {},
    onToggleAiGuard: (Boolean) -> Unit = {},
    onChangeAiGuardSensitivity: (Int) -> Unit = {},
    onChangeAiGuardFallback: (AiGuardFallback) -> Unit = {},
    onToggleAiGuardVoiceRecording: (Boolean) -> Unit = {},
    onResetAiGuardLearning: () -> Unit = {},
    onRequestAiGuardAudioPermission: () -> Unit = {},
    onToggleIntruderSiren: (Boolean) -> Unit = {},
    onTogglePanicShake: (Boolean) -> Unit = {},
    notificationHighlightEnabled: Boolean = false,
    notificationHighlightStyle: com.example.util.NotificationHighlighter.Style = com.example.util.NotificationHighlighter.Style.BLUE_VIOLET,
    aiSummaryEnabled: Boolean = false,
    aiKeyConfigured: Boolean = false,
    onToggleNotificationHighlight: (Boolean) -> Unit = {},
    onCycleHighlightStyle: () -> Unit = {},
    onToggleAiSummary: (Boolean) -> Unit = {},
    onAddBackgroundToGallery: () -> Unit = {},
    onRemoveBackground: (String) -> Unit = {},
    onToggleBackgroundAutoRotate: (Boolean) -> Unit = {},
    onCycleBackgroundRotateSeconds: () -> Unit = {}
) {
    val state = SettingsUiState(
        lockConfig = lockConfig,
        lockedAppCount = lockedAppCount,
        hasAccessibilityPermission = hasAccessibilityPermission,
        hasOverlayPermission = hasOverlayPermission,
        hasUsageStatsPermission = hasUsageStatsPermission,
        isLostModeActive = isLostModeActive,
        maxFailedAttempts = maxFailedAttempts,
        lockoutMinutes = lockoutMinutes,
        clipboardAutoClearEnabled = clipboardAutoClearEnabled,
        clipboardClearSeconds = clipboardClearSeconds,
        isFaceDownProtectionEnabled = isFaceDownProtectionEnabled,
        recoveryConfigured = recoveryConfigured,
        recoveryKeyConfigured = recoveryKeyConfigured,
        recoveryKeyFailedAttempts = recoveryKeyFailedAttempts,
        sessionCount = sessionLog.size,
        notificationHighlightEnabled = notificationHighlightEnabled,
        notificationHighlightStyle = notificationHighlightStyle,
        aiSummaryEnabled = aiSummaryEnabled,
        aiKeyConfigured = aiKeyConfigured,
        lockBackgroundGallery = lockConfig.lockBackgroundUris,
        lockBackgroundAutoRotate = lockConfig.lockBackgroundAutoRotate,
        lockBackgroundRotateSeconds = lockConfig.lockBackgroundRotateSeconds
    )

    val actions = SettingsUiActions(
        requestAccessibility = onRequestAccessibilityPermission,
        requestOverlay = onRequestOverlayPermission,
        requestUsageStats = onRequestUsageStatsPermission,
        openPermissionWizard = onOpenPermissionWizard,
        toggleNotificationPrivacy = onToggleNotificationPrivacy,
        toggleNotificationHighlight = onToggleNotificationHighlight,
        cycleHighlightStyle = onCycleHighlightStyle,
        toggleAiSummary = onToggleAiSummary,
        addBackgroundToGallery = onAddBackgroundToGallery,
        removeBackground = onRemoveBackground,
        toggleBackgroundAutoRotate = onToggleBackgroundAutoRotate,
        cycleBackgroundRotateSeconds = onCycleBackgroundRotateSeconds,
        toggleAppSelfProtect = onToggleAppSelfProtect,
        toggleUninstallProtection = onToggleUninstallProtection,
        toggleScreenOffLock = onToggleScreenOffLock,
        toggleFaceDownProtection = onToggleFaceDownProtection,
        toggleLostMode = { if (isLostModeActive) onDisableLostMode() else onActivateLostMode() },
        openLostModeMap = onOpenLostModeMap,
        changeLockType = onChangeLockType,
        changePattern = onChangePattern,
        changePin = onChangePin,
        changePassword = onChangePassword,
        changeCalculatorCode = onChangeCalculatorCode,
        changeKnockCode = onChangeKnockCode,
        changeTimeout = onChangeLockTimeout,
        changeSchedule = { enabled, startHour, startMinute, endHour, endMinute ->
            onUpdateConfig(
                lockConfig.copy(
                    isScheduleLockEnabled = enabled,
                    scheduleStartHour = startHour,
                    scheduleStartMinute = startMinute,
                    scheduleEndHour = endHour,
                    scheduleEndMinute = endMinute
                )
            )
        },
        toggleBiometric = onToggleBiometric,
        toggleStealthPattern = onToggleStealthPattern,
        toggleRandomPin = onToggleRandomPin,
        toggleVibration = onToggleVibration,
        changeMaxFailedAttempts = onChangeMaxFailedAttempts,
        changeLockoutMinutes = onChangeLockoutMinutes,
        openAppSchedules = onOpenAppSchedules,
        openExcludedApps = onOpenExcludedApps,
        openRecoveryQuestions = onOpenRecoverySetup,
        openRecoveryKey = onOpenRecoveryKeySetup,
        editLockStyle = onEditLockStyle,
        changeBackgroundTheme = onChangeTheme,
        pickCustomLockBackground = onPickCustomLockBackground,
        togglePrivacyFilter = onTogglePrivacyFilter,
        configureEmergencyContact = onConfigureEmergencyContact,
        openVault = onOpenVault,
        openSecureNotes = onOpenSecureNotes,
        manageBackup = onManageBackup,
        showRecoveryQr = onShowRecoveryQr,
        scanRecoveryQr = onScanRecoveryQr,
        toggleClipboardAutoClear = onToggleClipboardAutoClear,
        changeClipboardSeconds = onChangeClipboardClearSeconds,
        clearSessionLog = onClearSessionLog,
        showSessionLog = { },
        configureDisguise = onConfigureDisguise,
        toggleFakeCrash = onToggleFakeCrash,
        configureFakeScreen = onConfigureFakeScreen,
        configureDuressPin = onConfigureDuressPin,
        toggleAiGuard = onToggleAiGuard,
        changeAiGuardSensitivity = onChangeAiGuardSensitivity,
        changeAiGuardFallback = onChangeAiGuardFallback,
        toggleAiGuardVoice = onToggleAiGuardVoiceRecording,
        resetAiGuardLearning = onResetAiGuardLearning,
        toggleIntruderSelfie = onToggleIntruderSelfie,
        changeIntruderThreshold = onChangeIntruderSelfieThreshold,
        toggleIntruderSiren = onToggleIntruderSiren,
        togglePanicShake = onTogglePanicShake,
        checkForUpdates = onCheckForUpdates
    )

    SettingsRouter(state = state, actions = actions)
}

/** 잠긴 앱 / 설치된 앱 탭이 공유하는 목록 본문. 리스트·그리드 전환을 여기서 처리한다. */
@Composable
private fun AppListBody(
    apps: List<AppItem>,
    isEmpty: Boolean,
    emptyTitle: String,
    emptyDesc: String,
    viewMode: AppViewMode,
    isSelectionMode: Boolean,
    selectedPackages: Set<String>,
    onSelectToggle: (String) -> Unit,
    onToggleLock: (String, Boolean) -> Unit,
    onTestLaunch: (AppItem) -> Unit
) {
    if (isEmpty) {
        EmptyAppsView(title = emptyTitle, desc = emptyDesc)
        return
    }
    if (viewMode == AppViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps, key = { it.id }) { app ->
                AppGridCard(
                    app = app,
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedPackages.contains(app.packageName),
                    onSelectToggle = { onSelectToggle(app.packageName) },
                    onToggleLock = { onToggleLock(app.packageName, it) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps, key = { it.id }) { app ->
                AppItemCard(
                    app = app,
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedPackages.contains(app.packageName),
                    onSelectToggle = { onSelectToggle(app.packageName) },
                    onToggleLock = { onToggleLock(app.packageName, it) },
                    onTestLaunch = { onTestLaunch(app) }
                )
            }
        }
    }
}

/** 더보기 메뉴의 한 항목. 아이콘 아래에 이름까지 보여줘서 무엇인지 바로 안다. */
@Composable
private fun MoreMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
fun IntruderSelfieVaultView(
    logs: List<IntruderLog>,
    lockConfig: LockConfig,
    onClearLogs: () -> Unit,
    onDeleteLog: (String) -> Unit,
    onCaptureTestSelfie: () -> Unit,
    onDownloadIntruderPhoto: (String) -> Unit = {},
    onUpdateConfig: (LockConfig) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA) }
    var hasCameraPerm by remember { mutableStateOf(IntruderCameraHelper.hasCameraPermission(context)) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPerm = isGranted
    }
    var viewingPhotoLog by remember { mutableStateOf<IntruderLog?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var mediaTab by remember { mutableIntStateOf(0) }

    val photoLogsCount = remember(logs) { logs.count { it.photoPath != null } }
    val videoLogsCount = remember(logs) { logs.count { it.videoPath != null } }
    val audioLogsCount = remember(logs) { logs.count { it.audioPath != null } }
    val visibleLogs = when (mediaTab) { 0 -> logs.filter { it.photoPath != null }; 1 -> logs.filter { it.videoPath != null }; else -> logs.filter { it.audioPath != null } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. Vault Header Banner
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = NeonRed.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = NeonRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "침입 증거 보관함",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "사진 ${photoLogsCount}장 · 영상 ${videoLogsCount}개 · 음성 ${audioLogsCount}개 / 시도 ${logs.size}건",
                                color = NeonRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Switch(
                        checked = lockConfig.isIntruderSelfieEnabled,
                        onCheckedChange = {
                            onUpdateConfig(lockConfig.copy(isIntruderSelfieEnabled = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonRed
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "비밀번호나 패턴을 틀린 사람의 얼굴을 전면 카메라로 몰래 무음 촬영하여 보관합니다.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Threshold selection row
                Text(
                    text = "촬영 발동 조건:",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(1 to "1회 실패 즉시", 2 to "2회 연속 실패", 3 to "3회 연속 실패").forEach { (threshold, label) ->
                        FilterChip(
                            selected = lockConfig.intruderSelfieThreshold == threshold,
                            onClick = {
                                onUpdateConfig(lockConfig.copy(intruderSelfieThreshold = threshold))
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonRed,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Camera Permission Status
        if (!hasCameraPerm) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NeonAmber.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NeonAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "전면 카메라 권한 필요",
                            color = NeonAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "침입자 셀카를 촬영하려면 전면 카메라 권한이 필요합니다.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonAmber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("권한 허용", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NeonGreen.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "전면 카메라 잠복 감시 활성화됨 (인증 실패 시 자동 촬영)",
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Quick Action Buttons Bar
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onCaptureTestSelfie,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = NeonCyan
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("📸 전면 카메라 테스트 촬영", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (logs.isNotEmpty()) {
                androidx.compose.material3.TextButton(
                    onClick = { showDeleteAllConfirm = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = NeonRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("전체 삭제", color = NeonRed, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = mediaTab == 0, onClick = { mediaTab = 0 }, label = { Text("사진 ($photoLogsCount)") }, modifier = Modifier.weight(1f), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonRed, selectedLabelColor = Color.White))
            FilterChip(selected = mediaTab == 1, onClick = { mediaTab = 1 }, label = { Text("동영상 ($videoLogsCount)") }, modifier = Modifier.weight(1f), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonPurple, selectedLabelColor = Color.White))
            FilterChip(selected = mediaTab == 2, onClick = { mediaTab = 2 }, label = { Text("음성 ($audioLogsCount)") }, modifier = Modifier.weight(1f), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonCyan, selectedLabelColor = Color.Black))
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 4. Intruder Cards List or Empty View
        if (visibleLogs.isEmpty()) {
            EmptyAppsView(
                title = when (mediaTab) { 0 -> "촬영된 침입자 사진이 없습니다"; 1 -> "새로 녹화된 침입자 영상이 없습니다"; else -> "저장된 음성 기록이 없습니다" },
                desc = when (mediaTab) { 0 -> "인증 실패 시 촬영된 사진이 여기에 표시됩니다."; 1 -> "비밀번호를 한 번 틀리면 5초 영상이 자동 녹화되어 여기에 표시됩니다."; else -> "음성 기록을 켜면 비밀번호 1회 실패 시 5초 음성이 여기에 저장됩니다." }
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(visibleLogs, key = { it.id }) { log ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (log.photoPath != null) NeonRed.copy(alpha = 0.5f) else CyberBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // Media thumbnail
                            if (mediaTab == 0 && log.photoPath != null) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F172A))
                                        .clickable { viewingPhotoLog = log },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(File(log.photoPath))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "침입자 사진",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Zoom hint icon
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = "확대",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            } else if (mediaTab == 1 && log.videoPath != null) {
                                Surface(shape = RoundedCornerShape(12.dp), color = NeonPurple.copy(alpha = 0.16f), modifier = Modifier.size(80.dp).clickable { viewingPhotoLog = log }) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("▶", color = NeonPurple, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                        Text("5초 영상", color = TextPrimary, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
                                    }
                                }
                            } else if (mediaTab == 2 && log.audioPath != null) {
                                Surface(shape = RoundedCornerShape(12.dp), color = NeonCyan.copy(alpha = 0.14f), modifier = Modifier.size(80.dp).clickable { viewingPhotoLog = log }) {
                                    Box(contentAlignment = Alignment.Center) { Text("♫", color = NeonCyan, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("5초 음성", color = TextPrimary, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) }
                                }
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = NeonRed.copy(alpha = 0.15f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = NeonRed,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = log.appName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = NeonRed.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed)
                                    ) {
                                        Text(
                                            text = "${log.attemptCount}회 실패",
                                            color = NeonRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = log.packageName,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = NeonPurple.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = log.usedLockType,
                                            color = NeonPurple,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = dateFormat.format(Date(log.timestamp)),
                                        color = NeonCyan,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Single delete action
                            IconButton(
                                onClick = { onDeleteLog(log.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "삭제",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Screen Photo Inspection Dialog
    if (viewingPhotoLog != null) {
        val log = viewingPhotoLog!!
        Dialog(onDismissRequest = { viewingPhotoLog = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(2.dp, NeonRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = NeonRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                            text = when { log.videoPath != null && log.photoPath == null -> "침입자 포착 영상"; log.audioPath != null -> "침입자 음성 기록"; else -> "침입자 포착 사진" },
                                color = NeonRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        IconButton(onClick = { viewingPhotoLog = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "닫기",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Photo/video player
                    if (log.photoPath != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(log.photoPath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "확대된 침입자 사진",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else if (log.videoPath != null) {
                        AndroidView(
                            factory = { VideoView(it).apply { setVideoURI(Uri.fromFile(File(log.videoPath))); setOnPreparedListener { player -> player.isLooping = true; start() } } },
                            modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black)
                        )
                    } else if (log.audioPath != null) {
                        AndroidView(
                            factory = { viewContext ->
                                VideoView(viewContext).apply {
                                    setVideoURI(Uri.fromFile(File(log.audioPath)))
                                    setMediaController(MediaController(viewContext).also { it.setAnchorView(this) })
                                    setOnPreparedListener { start() }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.Black)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "${log.appName} 침입 시도",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "시간: ${dateFormat.format(Date(log.timestamp))}",
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "결과: ${log.attemptCount}회 연속 인증 실패 (${log.usedLockType})",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { (log.photoPath ?: log.videoPath ?: log.audioPath)?.let(onDownloadIntruderPhoto) },
                            enabled = log.photoPath != null || log.videoPath != null || log.audioPath != null,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("다운로드", fontSize = 12.sp) }
                        OutlinedButton(
                            onClick = {
                                onDeleteLog(log.id)
                                viewingPhotoLog = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("기록 삭제", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewingPhotoLog = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("확인", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Confirm Delete All Dialog
    if (showDeleteAllConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text("침입자 기록 전체 삭제", color = TextPrimary) },
            text = { Text("보관된 모든 침입자 사진과 로그를 완전히 삭제하시겠습니까?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearLogs()
                        showDeleteAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                ) {
                    Text("삭제", color = Color.White)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteAllConfirm = false }) {
                    Text("취소", color = TextSecondary)
                }
            },
            containerColor = CyberCardDark
        )
    }
}

@Composable
fun EmptyAppsView(title: String, desc: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = CyberCardDark,
            modifier = Modifier.size(80.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePatternModal(
    currentConfig: LockConfig,
    onDismiss: () -> Unit,
    onSave: (Int, List<Int>) -> Unit
) {
    var selectedGridSize by remember { mutableIntStateOf(currentConfig.gridSize) }
    var recordedPattern by remember { mutableStateOf<List<Int>?>(null) }
    var stepMessage by remember { mutableStateOf("원하는 노드를 연결하여 새 패턴을 그리세요") }

    val gridOptions = listOf(3, 4, 5, 6, 7, 8, 9, 10)
    // Body scrolls while the action row stays pinned, so "이 패턴 저장" is never clipped away.
    val bodyMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.58f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberSurfaceDark
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = bodyMaxHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "그리드 크기 및 패턴 설정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "그리드 칸 수 선택 (3x3 ~ 10x10 커스텀):",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gridOptions.take(4).forEach { size ->
                        FilterChip(
                            selected = selectedGridSize == size,
                            onClick = {
                                selectedGridSize = size
                                recordedPattern = null
                                stepMessage = "${size}x${size} 패턴을 연결해 보세요"
                            },
                            label = { Text("${size}x${size}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gridOptions.drop(4).forEach { size ->
                        FilterChip(
                            selected = selectedGridSize == size,
                            onClick = {
                                selectedGridSize = size
                                recordedPattern = null
                                stepMessage = "${size}x${size} 패턴을 연결해 보세요"
                            },
                            label = { Text("${size}x${size}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPurple,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stepMessage,
                    color = if (recordedPattern != null) NeonGreen else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(10.dp))

                NxNPatternLockView(
                    gridSize = selectedGridSize,
                    onPatternCompleted = { pattern ->
                        if (pattern.size < 3) {
                            stepMessage = "보안을 위해 최소 3개 이상의 점을 연결하세요"
                        } else {
                            recordedPattern = pattern
                            stepMessage = "${pattern.size}개 노드가 성공적으로 연결되었습니다!"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(300.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("취소", color = TextSecondary)
                }

                Button(
                    onClick = {
                        recordedPattern?.let {
                            onSave(selectedGridSize, it)
                        }
                    },
                    enabled = recordedPattern != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("이 패턴 저장", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeThemeModal(
    currentTheme: BackgroundTheme,
    onDismiss: () -> Unit,
    onSelectTheme: (BackgroundTheme) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = CyberSurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Keeps every theme reachable on short screens instead of clipping the last cards.
                .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.72f).dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "잠금 화면 배경 테마 선택",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            BackgroundTheme.values().forEach { theme ->
                val isSelected = theme == currentTheme
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) NeonPurple.copy(alpha = 0.2f) else CyberCardDark
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) NeonPurple else CyberBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onSelectTheme(theme) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = theme.title,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeonPurple else TextPrimary
                            )
                            Text(
                                text = theme.description,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = NeonPurple
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
