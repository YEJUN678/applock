package com.example

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import android.app.AlertDialog
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.provider.Settings
import android.provider.MediaStore
import android.os.Environment
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
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
import androidx.lifecycle.lifecycleScope
import com.example.model.AppItem
import com.example.model.IntruderLog
import com.example.service.AppLockMonitoringService
import com.example.service.PrivacyShadeOverlayService
import com.example.ui.components.PrivacyScreenFilter
import com.example.ui.components.CalculatorDisguiseLockView
import com.example.ui.components.UpdateInstallDialog
import com.example.ui.screens.AppLockerHomeScreen
import com.example.ui.screens.FileVaultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppLockPermissionHelper
import com.example.util.AppLockPreferences
import com.example.util.InstalledAppsManager
import com.example.util.IntruderCameraHelper
import com.example.util.EncryptedBackupManager
import com.example.util.AppUpdate
import com.example.util.AppUpdateManager
import com.example.util.IntruderPhotoExporter
import com.example.util.PanicShakeDetector
import com.example.util.BiometricHelper
import com.example.util.BiometricStatus
import com.example.util.QrRecoveryManager
import com.example.util.LostModeManager
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    private var availableUpdate by mutableStateOf<AppUpdate?>(null)
    private var isUpdateDownloading by mutableStateOf(false)
    private var updateDownloadProgress by mutableStateOf<Int?>(null)
    private var downloadedUpdateUri by mutableStateOf<Uri?>(null)
    private var pendingInstallerUri: Uri? = null
    private var incomingSharedUri by mutableStateOf<Uri?>(null)
    private var pendingLostModePackages: List<String>? = null
    private val requestLostModeLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        activateLostModeNow(pendingLostModePackages.orEmpty())
        pendingLostModePackages = null
    }

    fun activateLostMode(packages: List<String>) {
        pendingLostModePackages = packages
        requestLostModeLocation.launch(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.CAMERA))
    }

    private fun activateLostModeNow(packages: List<String>) {
        LostModeManager.setActive(this, true)
        AppLockPreferences.lockAll(this, packages)
        AppLockPreferences.resetAllTemporaryUnlocks()
        val location = LostModeManager.bestKnownLocation(this)
        IntruderCameraHelper.captureIntruderSelfie(this, this) { photoPath ->
            LostModeManager.record(this, location, photoPath)
            Toast.makeText(this, "Lost Mode가 활성화되었습니다. 잠금과 로컬 보안 기록을 적용했습니다.", Toast.LENGTH_LONG).show()
            LockActivity.start(this, packageName)
        }
    }

    private fun captureLostModeSnapshotOnOpen() {
        if (!LostModeManager.shouldCaptureOnOpen(this)) return
        val location = LostModeManager.bestKnownLocation(this)
        IntruderCameraHelper.captureIntruderSelfie(this, this) { photoPath ->
            LostModeManager.record(this, location, photoPath)
            Toast.makeText(this, "Lost Mode: 기기 열기 기록을 저장했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun disableLostMode() {
        LostModeManager.setActive(this, false)
        Toast.makeText(this, "Lost Mode를 해제했습니다. 앱 잠금 설정은 유지됩니다.", Toast.LENGTH_LONG).show()
    }
    fun showLostModeMap() {
        val event = LostModeManager.events(this).firstOrNull { it.location != null }
        val uri = LostModeManager.mapUri(event?.location)
        if (uri == null) {
            Toast.makeText(this, "지도에 열 수 있는 위치 기록이 없습니다. Lost Mode 위치 권한을 확인하세요.", Toast.LENGTH_LONG).show()
            return
        }
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            .onFailure { Toast.makeText(this, "지도 앱을 열 수 없습니다.", Toast.LENGTH_LONG).show() }
    }
    fun showRecoveryQr() {
        val bitmap = QrRecoveryManager.createRecoveryQr(this)
        val qrView = ImageView(this).apply {
            setImageBitmap(bitmap)
            adjustViewBounds = true
            setPadding(32, 16, 32, 16)
        }
        AlertDialog.Builder(this)
            .setTitle("오프라인 복구 QR")
            .setMessage("안전한 오프라인 장소에 보관하고 누구와도 공유하지 마세요. 새 QR을 만들면 이전 QR은 무효가 됩니다.")
            .setView(qrView)
            .setNeutralButton("기기에 저장") { _, _ ->
                if (saveRecoveryQr(bitmap)) Toast.makeText(this, "사진 보관함의 AppLockRecovery 폴더에 저장했습니다.", Toast.LENGTH_LONG).show()
                else Toast.makeText(this, "QR 이미지를 저장하지 못했습니다.", Toast.LENGTH_LONG).show()
            }
            .setPositiveButton("완료", null)
            .show()
    }

    private fun saveRecoveryQr(bitmap: android.graphics.Bitmap): Boolean = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "AppLock-Recovery-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AppLockRecovery")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        contentResolver.openOutputStream(uri)?.use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            ?: return false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            contentResolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        }
        true
    }.getOrDefault(false)

    fun scanRecoveryQr() {
        GmsBarcodeScanning.getClient(this).startScan()
            .addOnSuccessListener { barcode ->
                if (QrRecoveryManager.isValid(this, barcode.rawValue)) {
                    Toast.makeText(this, "복구 QR을 확인했습니다. 암호화 백업 파일을 선택하세요.", Toast.LENGTH_LONG).show()
                    selectBackupFile.launch(arrayOf("application/octet-stream", "*/*"))
                } else {
                    Toast.makeText(this, "유효하지 않거나 만료된 복구 QR입니다.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { Toast.makeText(this, "QR 스캔을 시작할 수 없습니다. Google Play 서비스를 확인하세요.", Toast.LENGTH_LONG).show() }
    }
    private val pickLockBackground = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            AppLockPreferences.saveLockConfig(this, AppLockPreferences.getLockConfig(this).copy(customLockBackgroundUri = uri.toString()))
            Toast.makeText(this, "사진을 잠금 배경으로 적용했습니다.", Toast.LENGTH_SHORT).show()
            recreate()
        }
    }
    fun chooseCustomLockBackground() {
        pickLockBackground.launch(arrayOf("image/*"))
    }
    fun showDuressPinSetup() {
        val field = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD; hint = "일반 PIN과 다른 4자리 PIN" }
        AlertDialog.Builder(this).setTitle("듀레스 PIN 설정").setMessage("입력하면 사생활 필름·침입 기록·즉시 재잠금이 실행됩니다.").setView(field)
            .setPositiveButton("저장") { _, _ ->
                val pin = field.text.toString()
                if (pin.length == 4 && pin != AppLockPreferences.getLockConfig(this).savedPin) { AppLockPreferences.setDuressPin(this, pin); Toast.makeText(this, "듀레스 PIN을 저장했습니다.", Toast.LENGTH_SHORT).show() }
                else Toast.makeText(this, "일반 PIN과 다른 4자리 숫자를 입력하세요.", Toast.LENGTH_LONG).show()
            }.setNegativeButton("취소", null).show()
    }
    fun showEmergencyContactSetup() {
        val input = EditText(this).apply { hint = "예: 홍길동 · 010-1234-5678" }
        input.setText(AppLockPreferences.getEmergencyContact(this))
        AlertDialog.Builder(this).setTitle("잠금 화면 비상 연락처").setMessage("잠금 화면에는 이 정보만 표시됩니다.").setView(input)
            .setPositiveButton("저장") { _, _ -> AppLockPreferences.setEmergencyContact(this, input.text.toString()); Toast.makeText(this, "비상 연락처를 저장했습니다.", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("취소", null).show()
    }
    fun showLockStyleEditor() {
        val options = arrayOf("선명 · 기본 아이콘", "다크 · 큰 아이콘", "은은함 · 작은 아이콘")
        AlertDialog.Builder(this).setTitle("잠금 화면 스타일")
            .setItems(options) { _, which ->
                val style = when (which) { 1 -> 1.2f to 0.92f; 2 -> 0.82f to 0.58f; else -> 1f to 0.82f }
                AppLockPreferences.saveLockConfig(this, AppLockPreferences.getLockConfig(this).copy(lockIconScale = style.first, lockBackgroundDim = style.second))
                Toast.makeText(this, "잠금 화면 스타일을 적용했습니다.", Toast.LENGTH_SHORT).show()
            }.show()
    }
    /** Destructive bulk-unlock is never authorized by the duress PIN. */
    fun authenticateBeforeUnlockAll(onConfirmed: () -> Unit) {
        val pinField = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "현재 PIN 입력"
        }
        AlertDialog.Builder(this)
            .setTitle("전체 잠금 해제 인증")
            .setMessage("생체 인증 또는 현재 PIN이 필요합니다. 듀레스 PIN은 사용할 수 없습니다.")
            .setView(pinField)
            .setPositiveButton("현재 PIN으로 해제") { _, _ ->
                if (pinField.text.toString() == AppLockPreferences.getLockConfig(this).savedPin) onConfirmed()
                else Toast.makeText(this, "현재 PIN이 올바르지 않습니다.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("취소", null)
            .setNeutralButton("생체 인증") { _, _ ->
                if (BiometricHelper.checkBiometricStatus(this) == BiometricStatus.AVAILABLE) {
                    BiometricHelper.authenticate(
                        activity = this,
                        title = "전체 잠금 해제",
                        subtitle = "본인 확인 후 모든 앱 잠금을 해제합니다.",
                        onSuccess = onConfirmed,
                        onError = { _, message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                    )
                } else Toast.makeText(this, "사용 가능한 생체 인증이 없습니다. 현재 PIN을 입력하세요.", Toast.LENGTH_LONG).show()
            }
            .show()
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
            val selected = listOf(default, calculator, notes)[choice]
            // Keep a launcher component enabled throughout the change so launchers
            // never treat the app as removed.
            pm.setComponentEnabledSetting(selected, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            listOf(default, calculator, notes).filter { it != selected }.forEach { pm.setComponentEnabledSetting(it, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP) }
            Toast.makeText(this, "위장 아이콘을 적용했습니다. 홈 화면을 새로고침하면 반영됩니다.", Toast.LENGTH_LONG).show()
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
        incomingSharedUri = intent.takeIf { it.action == Intent.ACTION_SEND }?.getParcelableExtra(Intent.EXTRA_STREAM)
        setContent {
            MyApplicationTheme {
                AppLockerApp(
                    incomingSharedUri = incomingSharedUri,
                    onIncomingShareHandled = { incomingSharedUri = null },
                    onShowToast = { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                )
                UpdateInstallDialog(
                    update = availableUpdate,
                    installedVersion = BuildConfig.VERSION_NAME,
                    isDownloading = isUpdateDownloading,
                    downloadProgress = updateDownloadProgress,
                    downloadedApkUri = downloadedUpdateUri,
                    onDismiss = {
                        availableUpdate = null
                        downloadedUpdateUri = null
                    },
                    onDownload = { update -> downloadUpdate(update) },
                    onBackUp = { showBackupActions() },
                    onInstall = { uri -> launchPackageInstaller(uri) }
                )
            }
        }
        if (!getSharedPreferences("backup_ui", MODE_PRIVATE).getBoolean("tutorial_seen", false)) showBackupTutorial()
        checkForAppUpdate(silent = true)
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

    fun checkForAppUpdate(silent: Boolean = false) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { AppUpdateManager.check() } }
            val update = result.getOrNull()
            when {
                update == null && !silent -> Toast.makeText(
                    this@MainActivity,
                    "업데이트 정보를 가져오지 못했습니다: ${result.exceptionOrNull()?.message ?: "인터넷 연결을 확인하세요."}",
                    Toast.LENGTH_LONG
                ).show()
                update == null -> Unit
                update.versionCode > BuildConfig.VERSION_CODE -> showUpdateAvailable(update)
                !silent -> {
                    // This is intentionally visible while testing releases: it makes the
                    // strict version-code comparison obvious rather than looking broken.
                    Toast.makeText(
                        this@MainActivity,
                        "최신 버전입니다. 설치됨 ${BuildConfig.VERSION_NAME} · 서버 ${update.versionName}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> Unit
            }
        }
    }

    private fun showUpdateAvailable(update: AppUpdate) {
        availableUpdate = update
    }

    private fun downloadUpdate(update: AppUpdate) {
        isUpdateDownloading = true
        updateDownloadProgress = 0
        downloadedUpdateUri = null
        val id = runCatching { AppUpdateManager.download(this, update) }.getOrElse {
            isUpdateDownloading = false
            Toast.makeText(this, "다운로드를 시작하지 못했습니다.", Toast.LENGTH_LONG).show(); return
        }
        lifecycleScope.launch {
            repeat(720) { // up to about 12 minutes, without blocking the UI
                val status = withContext(Dispatchers.IO) { AppUpdateManager.downloadStatus(this@MainActivity, id) }
                updateDownloadProgress = status.progressPercent
                if (status.failed) {
                    isUpdateDownloading = false
                    Toast.makeText(this@MainActivity, "업데이트 다운로드에 실패했습니다. 다시 시도해 주세요.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                if (status.apkUri != null) {
                    isUpdateDownloading = false
                    updateDownloadProgress = 100
                    downloadedUpdateUri = status.apkUri
                    return@launch
                }
                delay(1_000)
            }
            isUpdateDownloading = false
            Toast.makeText(this@MainActivity, "업데이트 다운로드가 완료되지 않았습니다.", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchPackageInstaller(apkUri: Uri) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            pendingInstallerUri = apkUri
            Toast.makeText(this, "이 출처의 앱 설치를 허용한 뒤 다시 업데이트를 눌러 주세요.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, "설치 화면을 열 수 없습니다.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        captureLostModeSnapshotOnOpen()
        val apkUri = pendingInstallerUri
        if (apkUri != null && (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls())) {
            pendingInstallerUri = null
            launchPackageInstaller(apkUri)
        }
    }
}

@Composable
fun AppLockerApp(
    incomingSharedUri: Uri?,
    onIncomingShareHandled: () -> Unit,
    onShowToast: (String) -> Unit
) {
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
    var isDuressMode by remember { mutableStateOf(AppLockPreferences.isDuressSession(context)) }
    var faceDownProtectionEnabled by remember { mutableStateOf(AppLockPreferences.isFaceDownProtectionEnabled(context)) }

    // Panic Shake Detector listener: immediately resets all temporary unlocks on vigorous shake
    DisposableEffect(lockConfig.isPanicShakeEnabled, lockConfig.panicShakeStrength, lifecycleOwner) {
        val shakeDetector = PanicShakeDetector(context, lockConfig.panicShakeStrength) {
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
                isDuressMode = AppLockPreferences.isDuressSession(context)
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
        if (isDuressMode) {
            // A duress unlock deliberately exposes no lock list, vault, settings, or logs.
            Box(modifier = Modifier.fillMaxSize()) {
                CalculatorDisguiseLockView(
                    targetCode = "__disabled__",
                    onCodeSubmitted = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (showVaultScreen || incomingSharedUri != null) {
            BackHandler { showVaultScreen = false; onIncomingShareHandled() }
            FileVaultScreen(
                onNavigateBack = { showVaultScreen = false },
                incomingShareUri = incomingSharedUri,
                onIncomingShareHandled = onIncomingShareHandled
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
                isDuressMode = false,
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
                    (context as? MainActivity)?.authenticateBeforeUnlockAll {
                        AppLockPreferences.unlockAll(context)
                        val updated = lockConfig.copy(isAppSelfProtectEnabled = false)
                        lockConfig = updated
                        AppLockPreferences.saveLockConfig(context, updated)
                        for (i in appsList.indices) {
                            appsList[i] = appsList[i].copy(isLocked = false)
                        }
                        onShowToast("모든 앱의 잠금이 해제되었습니다.")
                    }
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
                onOpenSecureNotes = {
                    context.startActivity(Intent(context, SecureNotesActivity::class.java))
                },
                isLostModeActive = LostModeManager.isActive(context),
                onActivateLostMode = { (context as? MainActivity)?.activateLostMode(appsList.map { it.packageName }) },
                onDisableLostMode = { (context as? MainActivity)?.disableLostMode() },
                onOpenLostModeMap = { (context as? MainActivity)?.showLostModeMap() },
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
                onShowRecoveryQr = { (context as? MainActivity)?.showRecoveryQr() },
                onScanRecoveryQr = { (context as? MainActivity)?.scanRecoveryQr() },
                onPickCustomLockBackground = { (context as? MainActivity)?.chooseCustomLockBackground() },
                onCheckForUpdates = { (context as? MainActivity)?.checkForAppUpdate() },
                onToggleScreenOffLock = { enabled ->
                    val updated = lockConfig.copy(isScreenOffLockEnabled = enabled)
                    lockConfig = updated
                    AppLockPreferences.saveLockConfig(context, updated)
                    onShowToast(if (enabled) "화면 끄기 즉시 재잠금이 켜졌습니다." else "화면 끄기 즉시 재잠금이 꺼졌습니다.")
                },
                onConfigureDuressPin = { (context as? MainActivity)?.showDuressPinSetup() },
                onConfigureEmergencyContact = { (context as? MainActivity)?.showEmergencyContactSetup() },
                onEditLockStyle = { (context as? MainActivity)?.showLockStyleEditor() },
                onToggleNotificationPrivacy = { enabled -> (context as? MainActivity)?.setNotificationPrivacy(enabled) },
                isFaceDownProtectionEnabled = faceDownProtectionEnabled,
                onToggleFaceDownProtection = { enabled ->
                    AppLockPreferences.setFaceDownProtectionEnabled(context, enabled)
                    faceDownProtectionEnabled = enabled
                    AppLockMonitoringService.startService(context)
                    onShowToast(if (enabled) "뒤집기 보호가 켜졌습니다." else "뒤집기 보호가 꺼졌습니다.")
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
