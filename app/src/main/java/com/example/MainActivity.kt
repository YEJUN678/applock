package com.example

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import android.app.AlertDialog
import android.content.ComponentName
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.InputType
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.util.EncryptedBackupManager
import com.example.util.IntruderPhotoExporter
import com.example.util.PanicShakeDetector
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    fun showDuressPinSetup() {
        val field = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD; hint = "일반 PIN과 다른 4자리 PIN" }
        AlertDialog.Builder(this).setTitle("듀레스 PIN 설정").setMessage("입력하면 사생활 필름·침입 기록·즉시 재잠금이 실행됩니다.").setView(field)
            .setPositiveButton("저장") { _, _ ->
                val pin = field.text.toString()
                if (pin.length == 4 && pin != AppLockPreferences.getLockConfig(this).savedPin) { AppLockPreferences.setDuressPin(this, pin); Toast.makeText(this, "듀레스 PIN을 저장했습니다.", Toast.LENGTH_SHORT).show() }
                else Toast.makeText(this, "일반 PIN과 다른 4자리 숫자를 입력하세요.", Toast.LENGTH_LONG).show()
            }.setNegativeButton("취소", null).show()
    }
    fun setNotificationPrivacy(enabled: Boolean) {
        val updated = AppLockPreferences.getLockConfig(this).copy(isNotificationPrivacyEnabled = enabled)
        AppLockPreferences.saveLockConfig(this, updated)
        if (enabled) startActivity(android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
    fun showLauncherDisguiseChooser() {
        val options = arrayOf("기본 App Lock", "계산기", "메모장")
        AlertDialog.Builder(this).setTitle("위장 아이콘").setItems(options) { _, choice ->
            val pm = packageManager
            val default = ComponentName(this, "${packageName}.DefaultAlias")
            val calculator = ComponentName(this, "${packageName}.CalculatorAlias")
            val notes = ComponentName(this, "${packageName}.NotesAlias")
            listOf(default, calculator, notes).forEach { pm.setComponentEnabledSetting(it, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP) }
            val selected = listOf(default, calculator, notes)[choice]
            pm.setComponentEnabledSetting(selected, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            Toast.makeText(this, "위장 아이콘을 적용했습니다. 홈 화면에 반영되기까지 잠시 걸릴 수 있습니다.", Toast.LENGTH_LONG).show()
        }.show()
    }
    private var pendingBackupPassword: CharArray? = null
    private var pendingRestoreUri: android.net.Uri? = null
    private val createBackupFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val password = pendingBackupPassword
        if (uri != null && password != null) runCatching { EncryptedBackupManager.export(this, uri, password) }
            .onSuccess { Toast.makeText(this, "암호화 백업을 저장했습니다.", Toast.LENGTH_LONG).show() }
            .onFailure { Toast.makeText(this, "백업 실패: ${it.message}", Toast.LENGTH_LONG).show() }
        pendingBackupPassword = null
    }
    private val selectBackupFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { pendingRestoreUri = uri; askBackupPassword("백업 복원", false) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Do not expose vault, lock settings, or intruder logs through screenshots,
        // screen recording, casting, or the Android recent-apps preview.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
        if (!getSharedPreferences("backup_ui", MODE_PRIVATE).getBoolean("tutorial_seen", false)) showBackupTutorial()
    }

    private fun showBackupTutorial() = AlertDialog.Builder(this)
        .setTitle("재설치 백업 · 복원")
        .setMessage("백업 비밀번호로 암호화해 Google Drive 등 원하는 위치에 저장할 수 있습니다. 새 설치에서는 이 파일을 선택해 복원하거나 건너뛸 수 있습니다.")
        .setPositiveButton("백업 내보내기") { _, _ -> askBackupPassword("백업 비밀번호 설정", true) }
        .setNeutralButton("백업 복원") { _, _ -> selectBackupFile.launch(arrayOf("application/octet-stream", "*/*")) }
        .setNegativeButton("건너뛰기") { _, _ -> getSharedPreferences("backup_ui", MODE_PRIVATE).edit().putBoolean("tutorial_seen", true).apply() }
        .show()

    fun showBackupActions() = AlertDialog.Builder(this)
        .setTitle("재설치 백업 · 복원")
        .setMessage("백업 파일은 비밀번호로 암호화됩니다. Google Drive를 선택해 안전하게 보관하세요.")
        .setPositiveButton("백업 내보내기") { _, _ -> askBackupPassword("백업 비밀번호 설정", true) }
        .setNeutralButton("백업 복원") { _, _ -> selectBackupFile.launch(arrayOf("application/octet-stream", "*/*")) }
        .setNegativeButton("취소", null)
        .show()

    private fun askBackupPassword(title: String, exporting: Boolean) {
        val field = EditText(this).apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; hint = "6자 이상 백업 비밀번호" }
        AlertDialog.Builder(this).setTitle(title).setView(field)
            .setPositiveButton(if (exporting) "계속" else "복원") { _, _ ->
                val password = field.text.toString().toCharArray()
                if (exporting) { pendingBackupPassword = password; createBackupFile.launch("AppLockBackup-${System.currentTimeMillis()}.albk") }
                else { val uri = pendingRestoreUri; if (uri != null) runCatching { EncryptedBackupManager.restore(this, uri, password) }
                    .onSuccess { getSharedPreferences("backup_ui", MODE_PRIVATE).edit().putBoolean("tutorial_seen", true).apply(); recreate() }
                    .onFailure { Toast.makeText(this, "복원 실패: 비밀번호 또는 파일을 확인하세요.", Toast.LENGTH_LONG).show() } }
            }.setNegativeButton("취소", null).show()
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
                isDuressMode = AppLockPreferences.isDuressSession(context),
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
                onDownloadIntruderPhoto = { path ->
                    if (IntruderPhotoExporter.exportToDownloads(context, path)) onShowToast("침입자 사진을 Downloads/AppLock Intruder에 저장했습니다.")
                    else onShowToast("사진을 저장하지 못했습니다.")
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
                },
                onConfigureDisguise = { (context as? MainActivity)?.showLauncherDisguiseChooser() },
                onManageBackup = { (context as? MainActivity)?.showBackupActions() },
                onConfigureDuressPin = { (context as? MainActivity)?.showDuressPinSetup() },
                onToggleNotificationPrivacy = { enabled -> (context as? MainActivity)?.setNotificationPrivacy(enabled) }
            )
        }

        // Privacy Shade Filter Overlay (엿보기 방지 화면 가림막)
        PrivacyScreenFilter(
            isActive = isPrivacyFilterActive,
            onClose = { isPrivacyFilterActive = false }
        )
    }
}
