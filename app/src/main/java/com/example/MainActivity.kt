package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.AppItem
import com.example.model.IntruderLog
import com.example.service.AppLockMonitoringService
import com.example.service.PrivacyShadeOverlayService
import com.example.ui.components.PrivacyScreenFilter
import com.example.ui.screens.AppLockerHomeScreen
import com.example.ui.screens.FileVaultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppLockPermissionHelper
import com.example.util.AppLockPreferences
import com.example.util.InstalledAppsManager
import com.example.util.IntruderCameraHelper
import com.example.util.PanicShakeDetector
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppLockerApp(
                    onShowToast = { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun AppLockerApp(onShowToast: (String) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val appsList = remember { mutableStateListOf<AppItem>() }
    var isLoadingApps by remember { mutableStateOf(true) }

    var hasOverlayPermission by remember {
        mutableStateOf(AppLockPermissionHelper.hasOverlayPermission(context))
    }
    var hasAccessibilityPermission by remember {
        mutableStateOf(AppLockPermissionHelper.hasAccessibilityPermission(context))
    }
    var hasUsageStatsPermission by remember {
        mutableStateOf(AppLockPermissionHelper.hasUsageStatsPermission(context))
    }

    var lockConfig by remember {
        mutableStateOf(AppLockPreferences.getLockConfig(context))
    }

    var intruderLogs by remember {
        mutableStateOf(AppLockPreferences.getIntruderLogs(context))
    }

    var showVaultScreen by remember { mutableStateOf(false) }
    var isPrivacyFilterActive by remember { mutableStateOf(false) }

    // Panic Shake Detector listener: immediately resets all temporary unlocks on vigorous shake
    DisposableEffect(lockConfig.isPanicShakeEnabled, lifecycleOwner) {
        val shakeDetector = PanicShakeDetector(context) {
            AppLockPreferences.resetAllTemporaryUnlocks()
            onShowToast("🚨 긴급 흔들림 감지: 모든 앱이 즉시 재잠금되었습니다!")
            val current = AppLockPreferences.getLockConfig(context)
            if (current.isAppSelfProtectEnabled || AppLockPreferences.isPackageLocked(context, context.packageName)) {
                LockActivity.start(context, context.packageName)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (lockConfig.isPanicShakeEnabled) {
                if (event == Lifecycle.Event.ON_RESUME) {
                    shakeDetector.start()
                } else if (event == Lifecycle.Event.ON_PAUSE) {
                    shakeDetector.stop()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lockConfig.isPanicShakeEnabled) {
            shakeDetector.start()
        }
        onDispose {
            shakeDetector.stop()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Re-check permissions and reload configs whenever activity resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = AppLockPermissionHelper.hasOverlayPermission(context)
                hasAccessibilityPermission = AppLockPermissionHelper.hasAccessibilityPermission(context)
                hasUsageStatsPermission = AppLockPermissionHelper.hasUsageStatsPermission(context)
                intruderLogs = AppLockPreferences.getIntruderLogs(context)
                val currentConfig = AppLockPreferences.getLockConfig(context)
                lockConfig = currentConfig
                isPrivacyFilterActive = PrivacyShadeOverlayService.isRunning

                // App Self Protection check: if enabled and not temporarily unlocked, show Lock Screen
                val isSelfLocked = currentConfig.isAppSelfProtectEnabled || AppLockPreferences.isPackageLocked(context, context.packageName)
                if (isSelfLocked && !AppLockPreferences.isTemporarilyUnlocked(context.packageName)) {
                    LockActivity.start(context, context.packageName)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Function to load real apps from device
    fun refreshApps() {
        coroutineScope.launch {
            isLoadingApps = true
            val lockedPackages = AppLockPreferences.getLockedPackages(context)
            val realApps = InstalledAppsManager.loadInstalledApps(
                context = context,
                currentlyLockedPackages = lockedPackages
            )
            appsList.clear()
            appsList.addAll(realApps)
            isLoadingApps = false
        }
    }

    // Initial load and start background monitoring service
    LaunchedEffect(Unit) {
        refreshApps()
        try {
            AppLockMonitoringService.startService(context)
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        if (showVaultScreen) {
            BackHandler { showVaultScreen = false }
            FileVaultScreen(
                onNavigateBack = { showVaultScreen = false }
            )
        } else {
            AppLockerHomeScreen(
                apps = appsList,
                isLoadingApps = isLoadingApps,
                hasOverlayPermission = hasOverlayPermission,
                hasAccessibilityPermission = hasAccessibilityPermission,
                hasUsageStatsPermission = hasUsageStatsPermission,
                lockConfig = lockConfig,
                intruderLogs = intruderLogs,
                onRequestOverlayPermission = {
                    val intent = AppLockPermissionHelper.getOverlayPermissionIntent(context)
                    try {
                        context.startActivity(intent)
                        onShowToast("다른 앱 위에 표시 권한을 허용해 주세요.")
                    } catch (_: Exception) {
                        onShowToast("권한 설정 화면을 열 수 없습니다.")
                    }
                },
                onRequestAccessibilityPermission = {
                    val intent = AppLockPermissionHelper.getAccessibilitySettingsIntent()
                    try {
                        context.startActivity(intent)
                        onShowToast("접근성 목록에서 'App Lock & Vault'를 켜주세요.")
                    } catch (_: Exception) {
                        onShowToast("접근성 설정 화면을 열 수 없습니다.")
                    }
                },
                onRequestUsageStatsPermission = {
                    val intent = AppLockPermissionHelper.getUsageStatsSettingsIntent()
                    try {
                        context.startActivity(intent)
                        onShowToast("사용 정보 접근 허용 목록에서 'App Lock'을 허용해 주세요.")
                    } catch (_: Exception) {
                        onShowToast("사용 정보 설정 화면을 열 수 없습니다.")
                    }
                },
                onRequestAppDetailsSettings = {
                    val intent = AppLockPermissionHelper.getAppDetailsSettingsIntent(context)
                    try {
                        context.startActivity(intent)
                        onShowToast("우측 상단 점 3개 메뉴 > '제한된 설정 허용'을 눌러주세요.")
                    } catch (_: Exception) {
                        onShowToast("앱 정보 설정을 열 수 없습니다.")
                    }
                },
                onRefreshApps = {
                    refreshApps()
                    intruderLogs = AppLockPreferences.getIntruderLogs(context)
                    onShowToast("앱 목록과 상태를 새로고침했습니다.")
                },
                onToggleLock = { pkgName, newStatus ->
                    AppLockPreferences.setPackageLocked(context, pkgName, newStatus)
                    if (pkgName == context.packageName) {
                        val updated = lockConfig.copy(isAppSelfProtectEnabled = newStatus)
                        lockConfig = updated
                        AppLockPreferences.saveLockConfig(context, updated)
                    }
                    val idx = appsList.indexOfFirst { it.packageName == pkgName }
                    if (idx != -1) {
                        appsList[idx] = appsList[idx].copy(isLocked = newStatus)
                        val statusStr = if (newStatus) "잠금 설정되었습니다." else "잠금이 해제되었습니다."
                        onShowToast("${appsList[idx].name} 이(가) $statusStr")
                    }
                },
                onLockAll = {
                    val allPkgs = appsList.map { it.packageName }
                    AppLockPreferences.lockAll(context, allPkgs)
                    val updated = lockConfig.copy(isAppSelfProtectEnabled = true)
                    lockConfig = updated
                    AppLockPreferences.saveLockConfig(context, updated)
                    for (i in appsList.indices) {
                        appsList[i] = appsList[i].copy(isLocked = true)
                    }
                    onShowToast("모든 앱(${appsList.size}개)이 잠금 설정되었습니다.")
                },
                onUnlockAll = {
                    AppLockPreferences.unlockAll(context)
                    val updated = lockConfig.copy(isAppSelfProtectEnabled = false)
                    lockConfig = updated
                    AppLockPreferences.saveLockConfig(context, updated)
                    for (i in appsList.indices) {
                        appsList[i] = appsList[i].copy(isLocked = false)
                    }
                    onShowToast("모든 앱의 잠금이 해제되었습니다.")
                },
                onBatchLock = { packages, lock ->
                    for (pkg in packages) {
                        AppLockPreferences.setPackageLocked(context, pkg, lock)
                        if (pkg == context.packageName) {
                            val updated = lockConfig.copy(isAppSelfProtectEnabled = lock)
                            lockConfig = updated
                            AppLockPreferences.saveLockConfig(context, updated)
                        }
                        val idx = appsList.indexOfFirst { it.packageName == pkg }
                        if (idx != -1) {
                            appsList[idx] = appsList[idx].copy(isLocked = lock)
                        }
                    }
                    val actionStr = if (lock) "잠금" else "잠금 해제"
                    onShowToast("선택한 ${packages.size}개 앱이 일괄 ${actionStr}되었습니다.")
                },
                onBatchTimeout = { newTimeout ->
                    val newConfig = lockConfig.copy(lockTimeoutSeconds = newTimeout)
                    lockConfig = newConfig
                    AppLockPreferences.saveLockConfig(context, newConfig)
                    val timeoutStr = if (newTimeout == 0) "즉시 재잠금" else "${newTimeout}초 후 재잠금"
                    onShowToast("재잠금 지연 시간이 '${timeoutStr}'(으)로 일괄 적용되었습니다.")
                },
                onClearIntruderLogs = {
                    AppLockPreferences.clearIntruderLogs(context)
                    intruderLogs = emptyList()
                    onShowToast("침입 시도 기록과 사진이 모두 삭제되었습니다.")
                },
                onDeleteIntruderLog = { id ->
                    AppLockPreferences.deleteIntruderLog(context, id)
                    intruderLogs = AppLockPreferences.getIntruderLogs(context)
                    onShowToast("해당 침입자 기록이 삭제되었습니다.")
                },
                onCaptureTestSelfie = {
                    IntruderCameraHelper.captureIntruderSelfie(
                        context = context,
                        lifecycleOwner = lifecycleOwner
                    ) { photoPath ->
                        AppLockPreferences.recordIntruderAttempt(
                            context = context,
                            packageName = context.packageName,
                            appName = "카메라 보안 테스트",
                            attempts = 1,
                            usedLockType = "테스트 촬영",
                            photoPath = photoPath
                        )
                        intruderLogs = AppLockPreferences.getIntruderLogs(context)
                        onShowToast("전면 카메라 침입자 셀카가 보관함에 저장되었습니다!")
                    }
                },
                onLockConfigChanged = { newConfig ->
                    lockConfig = newConfig
                    AppLockPreferences.saveLockConfig(context, newConfig)
                    AppLockPreferences.setPackageLocked(context, context.packageName, newConfig.isAppSelfProtectEnabled)
                    val selfIdx = appsList.indexOfFirst { it.packageName == context.packageName }
                    if (selfIdx != -1) {
                        appsList[selfIdx] = appsList[selfIdx].copy(isLocked = newConfig.isAppSelfProtectEnabled)
                    }
                    AppLockMonitoringService.startService(context)
                    onShowToast("새 보안 설정이 저장되었습니다.")
                },
                onTestLaunchApp = { app ->
                    LockActivity.start(context, app.packageName)
                },
                onOpenVault = {
                    showVaultScreen = true
                },
                onTogglePrivacyFilter = {
                    if (!AppLockPermissionHelper.hasOverlayPermission(context)) {
                        AppLockPermissionHelper.requestOverlayPermission(context)
                        onShowToast("다른 앱 위에 가림막을 표시하려면 '다른 앱 위에 표시' 권한이 필요합니다.")
                    } else {
                        if (PrivacyShadeOverlayService.isRunning) {
                            PrivacyShadeOverlayService.stop(context)
                            isPrivacyFilterActive = false
                            onShowToast("엿보기 방지 가림막이 종료되었습니다.")
                        } else {
                            PrivacyShadeOverlayService.start(context)
                            isPrivacyFilterActive = true
                            onShowToast("🛡️ 엿보기 방지 가림막이 시작되었습니다! (모든 앱 위에 표시)")
                        }
                    }
                }
            )
        }

        // Privacy Shade Filter Overlay (엿보기 방지 화면 가림막)
        PrivacyScreenFilter(
            isActive = isPrivacyFilterActive,
            onClose = { isPrivacyFilterActive = false }
        )
    }
}
