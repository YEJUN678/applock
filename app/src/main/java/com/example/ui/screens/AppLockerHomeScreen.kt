package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
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
import com.example.ui.components.AppItemCard
import com.example.ui.components.BatchTimeoutDialog
import com.example.ui.components.ChangeCalculatorCodeModal
import com.example.ui.components.ChangeKnockCodeModal
import com.example.ui.components.ChangePasswordModal
import com.example.ui.components.ChangePinModal
import com.example.ui.components.NxNPatternLockView
import com.example.ui.components.SecurityMetric
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
    onTogglePrivacyFilter: () -> Unit = {},
    onConfigureDisguise: () -> Unit = {},
    onManageBackup: () -> Unit = {},
    onShowRecoveryQr: () -> Unit = {},
    onScanRecoveryQr: () -> Unit = {},
    onPickCustomLockBackground: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onToggleScreenOffLock: (Boolean) -> Unit = {},
    onConfigureDuressPin: () -> Unit = {},
    onToggleNotificationPrivacy: (Boolean) -> Unit = {},
    isFaceDownProtectionEnabled: Boolean = false,
    onToggleFaceDownProtection: (Boolean) -> Unit = {},
    isDuressMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(1) } // 0: Locked, 1: Unlocked, 2: Intruder Selfie, 3: Settings
    var searchQuery by remember { mutableStateOf("") }
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
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "더보기",
                                tint = TextPrimary
                            )
                        }
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { showMoreMenu = false; onOpenVault() }) { Icon(Icons.Default.EnhancedEncryption, "금고", tint = NeonGreen) }
                                IconButton(onClick = { showMoreMenu = false; onTogglePrivacyFilter() }) { Icon(Icons.Default.VisibilityOff, "사생활 필름", tint = NeonAmber) }
                                if (!isDuressMode) IconButton(onClick = { showMoreMenu = false; selectedTab = 2 }) { Icon(Icons.Default.PhotoCamera, "침입자 기록", tint = NeonRed) }
                                if (!isDuressMode) IconButton(onClick = { showMoreMenu = false; selectedTab = 3 }) { Icon(Icons.Default.Settings, "설정 & 기능", tint = NeonCyan) }
                                IconButton(onClick = { showMoreMenu = false; onRefreshApps() }) { Icon(Icons.Default.Refresh, "새로고침", tint = TextPrimary) }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (selectedTab == 0) "🔒 잠긴 앱 (${lockedApps.size}개)" else "📱 설치된 앱 (${unlockedApps.size}개)",
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
                    0 -> {
                        if (lockedApps.isEmpty()) {
                            EmptyAppsView(
                                title = "잠긴 앱이 없습니다",
                                desc = "'전체/안 잠긴 앱' 탭에서 잠그고 싶은 앱의 스위치를 켜거나, '전체 잠금'을 눌러보세요."
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(lockedApps, key = { it.id }) { app ->
                                    AppItemCard(
                                        app = app,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = selectedPackages.contains(app.packageName),
                                        onSelectToggle = {
                                            selectedPackages = if (selectedPackages.contains(app.packageName)) {
                                                selectedPackages - app.packageName
                                            } else {
                                                selectedPackages + app.packageName
                                            }
                                        },
                                        onToggleLock = { onToggleLock(app.packageName, it) },
                                        onTestLaunch = { onTestLaunchApp(app) }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        if (unlockedApps.isEmpty()) {
                            if (searchQuery.isNotEmpty()) {
                                EmptyAppsView(
                                    title = "'$searchQuery' 검색 결과 없음",
                                    desc = "해당 키워드와 일치하는 설치 앱이 없습니다."
                                )
                            } else {
                                EmptyAppsView(
                                    title = "표시할 앱이 없습니다",
                                    desc = "설치된 모든 앱이 잠겨 있거나 목록을 불러오지 못했습니다. 상단 새로고침을 눌러보세요."
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(unlockedApps, key = { it.id }) { app ->
                                    AppItemCard(
                                        app = app,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = selectedPackages.contains(app.packageName),
                                        onSelectToggle = {
                                            selectedPackages = if (selectedPackages.contains(app.packageName)) {
                                                selectedPackages - app.packageName
                                            } else {
                                                selectedPackages + app.packageName
                                            }
                                        },
                                        onToggleLock = { onToggleLock(app.packageName, it) },
                                        onTestLaunch = { onTestLaunchApp(app) }
                                    )
                                }
                            }
                        }
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
                            onToggleIntruderSelfie = {
                                onLockConfigChanged(lockConfig.copy(isIntruderSelfieEnabled = it))
                            },
                            onChangeIntruderSelfieThreshold = {
                                onLockConfigChanged(lockConfig.copy(intruderSelfieThreshold = it))
                            },
                            onOpenVault = onOpenVault,
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
                            onToggleNotificationPrivacy = onToggleNotificationPrivacy,
                            isFaceDownProtectionEnabled = isFaceDownProtectionEnabled,
                            onToggleFaceDownProtection = onToggleFaceDownProtection,
                            onToggleRandomPin = {
                                onLockConfigChanged(lockConfig.copy(isRandomPinKeypad = it))
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
    onToggleIntruderSelfie: (Boolean) -> Unit = {},
    onChangeIntruderSelfieThreshold: (Int) -> Unit = {},
    onOpenVault: () -> Unit = {},
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
    onToggleNotificationPrivacy: (Boolean) -> Unit = {},
    isFaceDownProtectionEnabled: Boolean = false,
    onToggleFaceDownProtection: (Boolean) -> Unit = {},
    onToggleRandomPin: (Boolean) -> Unit = {},
    onToggleIntruderSiren: (Boolean) -> Unit = {},
    onTogglePanicShake: (Boolean) -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Redesigned security dashboard: surface the three facts users need before
        // opening individual settings cards.
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("보안 제어 센터", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Text("현재 보호 상태를 빠르게 확인하세요", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SecurityMetric("보호 앱", "${lockedAppCount}개", NeonCyan, Modifier.weight(1f))
                        SecurityMetric("인증", lockConfig.lockType.title, NeonPurple, Modifier.weight(1f))
                        SecurityMetric("긴급 흔들기", if (lockConfig.isPanicShakeEnabled) "강도 ${lockConfig.panicShakeStrength}" else "꺼짐", NeonAmber, Modifier.weight(1f))
                    }
                }
            }
        }

        // 0. Restricted Settings Guide (Android 13/14+)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NeonAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "내 폰에서 접근성이 안 켜질 때 (해결 가이드)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "안드로이드 13/14+ 기기에서는 보안 정책상 다운로드된 앱의 접근성이 기본 차단('제한된 설정')됩니다.\n\n해결 방법:\n1. 아래 [앱 정보 열기] 터치\n2. 우측 상단 ⠇(점 3개 메뉴) 터치\n3. '제한된 설정 허용' 선택 후 생체/패턴 인증\n4. 접근성 화면으로 돌아와 스위치를 켜면 즉시 활성화됩니다!",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRequestAppDetailsSettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonAmber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("앱 정보 열기 (3점 메뉴 > 제한된 설정 허용)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // 1. Accessibility Service Permission card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hasAccessibilityPermission) NeonGreen else NeonCyan
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (hasAccessibilityPermission) NeonGreen else NeonCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "실시간 즉각 가로채기 (접근성 서비스)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasAccessibilityPermission)
                            "✓ 접근성 서비스가 켜져 있습니다. 잠긴 앱을 누르면 0.05초 만에 잠금 화면이 바로 뜨고, 뒤로가기 누르면 해당 앱이 꺼집니다."
                        else
                            "⚡ [필수 권한] 잠긴 앱 클릭 시 바로 잠금 화면을 띄우고 뒤로가기 시 실행을 원천 차단하려면 접근성 서비스가 켜져 있어야 합니다.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRequestAccessibilityPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasAccessibilityPermission) NeonGreen.copy(alpha = 0.2f) else NeonCyan,
                            contentColor = if (hasAccessibilityPermission) NeonGreen else Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (hasAccessibilityPermission) "접근성 서비스 정상 작동 중" else "접근성 설정 열기",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Overlay Permission card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hasOverlayPermission) NeonGreen else NeonAmber
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (hasOverlayPermission) NeonGreen else NeonAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "다른 앱 위에 표시 권한",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasOverlayPermission)
                            "✓ 오버레이 권한이 허용되어 있습니다. 다른 앱 위에 즉각적으로 보안 창을 덮어씌웁니다."
                        else
                            "잠긴 앱 화면 위로 보안 잠금창을 띄우기 위해 필요한 권한입니다.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRequestOverlayPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasOverlayPermission) NeonGreen.copy(alpha = 0.2f) else NeonAmber,
                            contentColor = if (hasOverlayPermission) NeonGreen else Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (hasOverlayPermission) "오버레이 권한 허용됨" else "다른 앱 위에 표시 설정 열기",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Usage Stats Permission card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hasUsageStatsPermission) NeonGreen else CyberBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasUsageStatsPermission) NeonGreen else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "사용정보 접근 권한 (보조 이중 감지)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasUsageStatsPermission)
                            "✓ 백그라운드 이중 감시가 활성화되어 있습니다."
                        else
                            "접근성 서비스와 별개로 백그라운드에서 실행 앱을 이중 확인하는 보조 권한입니다.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRequestUsageStatsPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasUsageStatsPermission) NeonGreen.copy(alpha = 0.2f) else CyberSurfaceDark,
                            contentColor = if (hasUsageStatsPermission) NeonGreen else TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (hasUsageStatsPermission) "사용정보 권한 허용됨" else "사용정보 접근 설정 열기",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3.5 Feature: Security File Vault (AES-256 File Crypto)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.EnhancedEncryption,
                                contentDescription = null,
                                tint = NeonGreen
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "보안 파일 금고 (AES-256 암호화/복호화)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "모든 용량의 사진, 영상, 문서를 앱 고유 키로 스트리밍 암호화합니다. 이 앱 없이는 복호화가 불가능하며 필요 시 즉시 원본으로 복구합니다.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenVault,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("보안 파일 금고 열기 (암호화/복호화 관리) ▶", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3.6 Feature: Anti-Peeping Privacy Filter
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = NeonAmber
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "엿보기 방지 화면 가림막 (Privacy Shade)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "중앙 읽기 영역은 선명하게, 주변은 어둡게 가려 한눈에 보이는 정보를 줄입니다. 앱 화면은 캡처·녹화·최근 앱 미리보기에서도 숨겨집니다. 빠른 설정 편집에서 ‘사생활 보호 화면’과 ‘즉시 잠금’ 타일을 추가할 수 있습니다.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onTogglePrivacyFilter,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("화면 가림막 켜기 / 끄기 토글", color = NeonAmber, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onConfigureDisguise, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber), modifier = Modifier.fillMaxWidth()) { Text("위장 아이콘 설정", color = NeonAmber, fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = onConfigureDuressPin, enabled = lockConfig.lockType == LockType.PIN, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed), modifier = Modifier.fillMaxWidth()) {
                        Text(if (lockConfig.lockType == LockType.PIN) "듀레스 PIN 설정 (긴급 보호)" else "듀레스 PIN: PIN 잠금 방식에서 사용 가능", color = NeonRed, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onManageBackup, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan), modifier = Modifier.fillMaxWidth()) { Text("재설치 백업 · 복원", color = NeonCyan, fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = onShowRecoveryQr, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen), modifier = Modifier.fillMaxWidth()) { Text("오프라인 복구 QR 만들기", color = NeonGreen, fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = onScanRecoveryQr, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber), modifier = Modifier.fillMaxWidth()) { Text("복구 QR 스캔", color = NeonAmber, fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = onCheckForUpdates, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen), modifier = Modifier.fillMaxWidth()) { Text("업데이트 확인", color = NeonGreen, fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = { onToggleNotificationPrivacy(!lockConfig.isNotificationPrivacyEnabled) }, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple), modifier = Modifier.fillMaxWidth()) { Text(if (lockConfig.isNotificationPrivacyEnabled) "잠긴 앱 알림 숨김: 켜짐" else "잠긴 앱 알림 숨김 켜기", color = NeonPurple, fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = { onToggleFaceDownProtection(!isFaceDownProtectionEnabled) }, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber), modifier = Modifier.fillMaxWidth()) { Text(if (isFaceDownProtectionEnabled) "뒤집기 보호: 켜짐" else "뒤집으면 즉시 잠금 켜기", color = NeonAmber, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // 3.7 Feature: Random PIN Keypad
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (lockConfig.isRandomPinKeypad) NeonCyan else CyberBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Dialpad,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PIN 키패드 무작위 재배치 (Random Keypad)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "PIN 인증 시 0~9 숫자 위치를 무작위로 섞어 주변 엿보기 및 손가락 이동 패턴 추적을 방지합니다.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = lockConfig.isRandomPinKeypad,
                            onCheckedChange = onToggleRandomPin,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }
                }
            }
        }

        // 3.8 Feature: Intruder Alarm Siren
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (lockConfig.isIntruderSirenEnabled) NeonRed else CyberBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = NeonRed
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "침입자 경보 사이렌 (Intruder Alarm Siren)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "잠금 해제 2회 이상 실패 시 큰 경보음을 울려 침입자를 쫓아냅니다.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = lockConfig.isIntruderSirenEnabled,
                            onCheckedChange = onToggleIntruderSiren,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonRed
                            )
                        )
                    }
                }
            }
        }

        // 3.9 Feature: Panic Shake to Lock
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (lockConfig.isPanicShakeEnabled) NeonPurple else CyberBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = NeonPurple
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "긴급 안심 흔들기 즉시 재잠금 (Panic Shake)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "위급한 순간 스마트폰을 강하게 흔들면 임시 해제된 모든 잠금이 즉시 재설정되어 데이터를 보호합니다.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = lockConfig.isPanicShakeEnabled,
                            onCheckedChange = onTogglePanicShake,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonPurple
                            )
                        )
                    }
                }
            }
        }

        // 4. Feature: Stealth Pattern (패턴 선 숨김)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "스텔스 패턴 모드 (선 숨김)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "패턴을 그릴 때 연결선이 보이지 않아 주변 훔쳐보기를 완벽 차단합니다.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = lockConfig.isStealthPattern,
                            onCheckedChange = onToggleStealthPattern,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }
                }
            }
        }

        // 5. Feature: Fake Crash Alert (오류 가림막)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = NeonRed
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "앱 실행 오류 가림막 (Crash Alert 보호)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "잠긴 앱 실행 시 시스템 오류 안내창을 띄워 접근을 방어합니다. 확인 버튼을 길게 누르면 인증 화면이 열립니다.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = lockConfig.isFakeCrashEnabled,
                            onCheckedChange = onToggleFakeCrash,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonRed
                            )
                        )
                    }
                }
            }
        }

        // 6. Feature: Biometric Authentication (Fingerprint & Face Recognition)
        item {
            val context = LocalContext.current
            val bioStatus = remember { BiometricHelper.checkBiometricStatus(context) }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (lockConfig.biometricEnabled) NeonCyan else CyberBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "생체 인식 인증 (지문 및 얼굴)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "안드로이드 시스템 지문 센서 및 안면 인식으로 즉시 잠금 해제합니다.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = lockConfig.biometricEnabled,
                            onCheckedChange = onToggleBiometric,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hardware status badge
                    val (statusColor, statusText) = when (bioStatus) {
                        BiometricStatus.AVAILABLE -> NeonGreen to "✓ 지문/얼굴 센서 연동 완료: 실제 하드웨어 인증 사용 가능"
                        BiometricStatus.NOT_ENROLLED -> NeonAmber to "⚠️ 기기에 지문/얼굴 미등록: 폰 설정에서 지문을 등록해주세요"
                        BiometricStatus.NO_HARDWARE -> Color(0xFF94A3B8) to "ℹ️ 하드웨어 센서 미감지: 테스트 시뮬레이션 모드가 동작합니다"
                        BiometricStatus.HW_UNAVAILABLE -> NeonAmber to "⚠️ 센서를 일시적으로 사용할 수 없습니다"
                        BiometricStatus.UNAVAILABLE -> Color(0xFF94A3B8) to "ℹ️ 생체 인식을 지원하지 않는 환경입니다"
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    if (bioStatus == BiometricStatus.NOT_ENROLLED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(BiometricHelper.getBiometricEnrollIntent())
                                } catch (e: Exception) {
                                    // fallback
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("기기 생체인식(지문/얼굴) 등록 설정 열기 ▶", color = NeonAmber, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 6.2 Feature: Intruder Selfie Setting Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (lockConfig.isIntruderSelfieEnabled) NeonRed.copy(alpha = 0.5f) else CyberBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = NeonRed
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "침입자 전면 카메라 셀카",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "잠금 실패 시 몰래 전면 카메라로 사진을 촬영하여 '침입자 셀카' 탭에 보관합니다.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = lockConfig.isIntruderSelfieEnabled,
                            onCheckedChange = onToggleIntruderSelfie,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonRed
                            )
                        )
                    }

                    if (lockConfig.isIntruderSelfieEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "촬영 기준 (연속 실패 횟수):",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(1 to "1회 실패 즉시", 2 to "2회 실패 시", 3 to "3회 실패 시").forEach { (threshold, label) ->
                                FilterChip(
                                    selected = lockConfig.intruderSelfieThreshold == threshold,
                                    onClick = { onChangeIntruderSelfieThreshold(threshold) },
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
            }
        }

        // 6.5 Security Type Selection: Pattern, PIN, Password, Calculator, Knock Code
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val lockTypeIcon = when (lockConfig.lockType) {
                            LockType.PIN -> Icons.Default.Dialpad
                            LockType.PATTERN -> Icons.Default.GridOn
                            LockType.PASSWORD -> Icons.Default.Password
                            LockType.CALCULATOR -> Icons.Default.Calculate
                            LockType.KNOCK_CODE -> Icons.Default.TouchApp
                        }
                        Icon(
                            imageVector = lockTypeIcon,
                            contentDescription = null,
                            tint = NeonCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "잠금 인증 방식 (${lockConfig.lockType.title})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "원하는 보안 잠금 형태를 선택하세요. 패턴, 숫자 PIN, 영문/숫자 비밀번호, 보안 계산기, 노크 코드 등을 지원합니다.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 1: Pattern, PIN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = lockConfig.lockType == LockType.PATTERN,
                            onClick = { onChangeLockType(LockType.PATTERN) },
                            label = { Text("패턴 (${lockConfig.gridSize}x${lockConfig.gridSize})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = lockConfig.lockType == LockType.PIN,
                            onClick = { onChangeLockType(LockType.PIN) },
                            label = { Text("PIN (숫자)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: Password, Calculator, Knock Code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = lockConfig.lockType == LockType.PASSWORD,
                            onClick = { onChangeLockType(LockType.PASSWORD) },
                            label = { Text("비밀번호", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = lockConfig.lockType == LockType.CALCULATOR,
                            onClick = { onChangeLockType(LockType.CALCULATOR) },
                            label = { Text("보안 계산기", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1.1f)
                        )
                        FilterChip(
                            selected = lockConfig.lockType == LockType.KNOCK_CODE,
                            onClick = { onChangeLockType(LockType.KNOCK_CODE) },
                            label = { Text("노크 코드", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Configuration Button depending on active LockType
                    when (lockConfig.lockType) {
                        LockType.PIN -> {
                            OutlinedButton(
                                onClick = onChangePin,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Dialpad, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("새 4자리 PIN 비밀번호 등록/변경", color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                        LockType.PASSWORD -> {
                            OutlinedButton(
                                onClick = onChangePassword,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Password, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("새 영문/숫자 비밀번호(Password) 등록/변경", color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                        LockType.CALCULATOR -> {
                            OutlinedButton(
                                onClick = onChangeCalculatorCode,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("계산기 해제 암호(=) 등록/변경 (현재: ${lockConfig.savedCalculatorCode})", color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                        LockType.KNOCK_CODE -> {
                            OutlinedButton(
                                onClick = onChangeKnockCode,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.TouchApp, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("4분면 노크 코드 터치 순서 등록/변경 (${lockConfig.savedKnockCode.size}회)", color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                        LockType.PATTERN -> {
                            OutlinedButton(
                                onClick = onChangePattern,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.GridOn, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("그리드 크기(${lockConfig.gridSize}x${lockConfig.gridSize}) 및 패턴 변경", color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 6.6 Lock Timeout Grace Period (타이핑/입력 중 재잠김 방지 & 재잠금 주기 설정)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = NeonAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "재잠금 지연 시간 (키보드 입력 보호)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "앱 해제 후 다른 작업(키보드 타이핑, 일시 전환 등) 중 비밀번호가 연속으로 다시 뜨지 않도록 유예 시간을 둡니다.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val timeoutOptions = listOf(
                        0 to "즉시",
                        15 to "15초",
                        30 to "30초",
                        60 to "1분",
                        300 to "5분"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timeoutOptions.forEach { (seconds, label) ->
                            FilterChip(
                                selected = lockConfig.lockTimeoutSeconds == seconds,
                                onClick = { onChangeLockTimeout(seconds) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonAmber,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 6.7 App Self Protection (본 앱 자체 보호)
        item {
            val context = LocalContext.current
            fun pickTime(isStart: Boolean) {
                val hour = if (isStart) lockConfig.scheduleStartHour else lockConfig.scheduleEndHour
                val minute = if (isStart) lockConfig.scheduleStartMinute else lockConfig.scheduleEndMinute
                TimePickerDialog(context, { _, selectedHour, selectedMinute ->
                    onUpdateConfig(if (isStart) lockConfig.copy(scheduleStartHour = selectedHour, scheduleStartMinute = selectedMinute)
                    else lockConfig.copy(scheduleEndHour = selectedHour, scheduleEndMinute = selectedMinute))
                }, hour, minute, true).show()
            }
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CyberCardDark), border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("시간대 자동 재잠금", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("설정한 시간에는 잠긴 앱을 다시 열 때마다 인증합니다.", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(checked = lockConfig.isScheduleLockEnabled, onCheckedChange = { onUpdateConfig(lockConfig.copy(isScheduleLockEnabled = it)) })
                    }
                    if (lockConfig.isScheduleLockEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { pickTime(true) }, modifier = Modifier.weight(1f)) { Text("시작 %02d:%02d".format(lockConfig.scheduleStartHour, lockConfig.scheduleStartMinute)) }
                            OutlinedButton(onClick = { pickTime(false) }, modifier = Modifier.weight(1f)) { Text("종료 %02d:%02d".format(lockConfig.scheduleEndHour, lockConfig.scheduleEndMinute)) }
                        }
                    }
                }
            }
        }

        // 6.7 App Self Protection (본 앱 자체 보호)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(18.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("화면을 끄면 즉시 재잠금", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("화면을 다시 켰을 때 잠긴 앱은 인증을 다시 요구합니다.", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(checked = lockConfig.isScreenOffLockEnabled, onCheckedChange = onToggleScreenOffLock)
                }
            }
        }

        // 6.7 App Self Protection (본 앱 자체 보호)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = NeonGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "앱 자체 보호 (App Locker 보안)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "타인이 App Lock & Vault를 켜서 잠금을 무단 해제하지 못하도록 본 앱 진입 시에도 잠금을 적용합니다.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = lockConfig.isAppSelfProtectEnabled,
                            onCheckedChange = onToggleAppSelfProtect,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonGreen
                            )
                        )
                    }
                }
            }
        }

        // 6.8 Feature: Uninstall Protection (앱 삭제/제거 원천 방지 보호 스위치)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (lockConfig.isUninstallProtectionEnabled) NeonRed else CyberBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = NeonRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "앱 삭제/제거 방지 보호 스위치",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "남이 어떤 앱도 임의로 삭제(Uninstall)하거나 시스템 패키지 관리자를 조작하지 못하도록 삭제 시도 화면을 즉각 원천 차단합니다.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = lockConfig.isUninstallProtectionEnabled,
                            onCheckedChange = onToggleUninstallProtection,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonRed
                            )
                        )
                    }
                }
            }
        }

        // 7. Grid Size and Pattern configuration
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "보안 패턴 및 그리드 크기",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "현재 설정: ${lockConfig.gridSize} x ${lockConfig.gridSize} (총 ${lockConfig.gridSize * lockConfig.gridSize}개 노드 / 3x3~10x10 지원)",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onChangePattern,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.GridOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("그리드 크기 및 새 패턴 등록", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 8. Theme configuration
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "잠금 화면 배경 테마",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "현재 적용된 배경: ${lockConfig.backgroundTheme.title}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = onChangeTheme,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = NeonPurple
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("배경 화면 테마 변경", color = NeonPurple, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onPickCustomLockBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (lockConfig.customLockBackgroundUri == null) "사진첩에서 배경 선택" else "선택한 사진 배경 바꾸기", color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 9. Vibration Feedback
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "터치 진동 피드백",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "패턴 연결 노드 터치 및 PIN 키패드 입력 시 햅틱 진동 피드백을 제공합니다.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = lockConfig.isVibrationEnabled,
                            onCheckedChange = onToggleVibration,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }
                }
            }
        }
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

    val photoLogsCount = remember(logs) { logs.count { it.photoPath != null } }

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
                                text = "침입자 셀카 보관함",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "사진 ${photoLogsCount}장 / 시도 ${logs.size}건",
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

        // 4. Intruder Cards List or Empty View
        if (logs.isEmpty()) {
            EmptyAppsView(
                title = "촬영된 침입자가 없습니다",
                desc = "다른 사람이 내 폰에서 잠긴 앱의 비밀번호나 패턴을 틀리면, 전면 카메라로 침입자의 얼굴을 몰래 캡처하여 여기에 사진으로 보관합니다.\n\n위 '전면 카메라 테스트 촬영' 버튼을 눌러 지금 테스트해볼 수 있습니다!"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs, key = { it.id }) { log ->
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
                            // Photo Thumbnail
                            if (log.photoPath != null) {
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
                                text = "침입자 포착 사진",
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

                    // Big Photo Display
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
                            onClick = { log.photoPath?.let(onDownloadIntruderPhoto) },
                            enabled = log.photoPath != null,
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
                            Text("사진 삭제", fontSize = 12.sp)
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
