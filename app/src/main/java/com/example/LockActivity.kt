package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.service.AppLockAccessibilityService
import com.example.service.PrivacyShadeOverlayService
import com.example.ui.screens.LockOverlayScreen
import com.example.ui.screens.FakeScreenKind
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppLockPreferences
import com.example.util.BiometricHelper
import com.example.util.BiometricStatus
import com.example.util.BehavioralGuard
import com.example.util.BehavioralGuardResult
import com.example.util.BehavioralInputMetrics
import com.example.util.AiGuardAudioRecorder
import com.example.model.AiGuardFallback
import com.example.util.InstalledAppsManager
import com.example.util.IntruderCameraHelper
import com.example.util.LockNotificationHelper
import com.example.util.RecoveryQuestionManager
import com.example.util.IntruderLocationCapture
import com.example.util.CaptureLocation
import com.example.util.RecoveryKeyManager
import com.example.ui.components.RecoveryVerifyDialog
import com.example.ui.components.RecoveryMethodChooserDialog
import com.example.ui.components.RecoveryKeyVerifyDialog

class LockActivity : FragmentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_target_package_name"

        fun start(context: Context, packageName: String) {
            val intent = Intent(context, LockActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                // Background start restriction fallback: high-priority full-screen notification
                try {
                    LockNotificationHelper.launchFullScreenLock(context, packageName)
                } catch (_: Exception) {}
            }
        }
    }

    private var targetPackage by mutableStateOf("")
    private var appName by mutableStateOf("보호된 앱")
    private var appIcon by mutableStateOf<Drawable?>(null)
    private var deviceTiltDegrees by mutableStateOf(0f)
    private var additionalCredentialRequired by mutableStateOf(false)
    private var sensorManager: SensorManager? = null
    private val orientationListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
            val gravity = kotlin.math.sqrt(x * x + y * y + z * z).coerceAtLeast(0.1f)
            deviceTiltDegrees = Math.toDegrees(kotlin.math.acos((z / gravity).coerceIn(-1f, 1f)).toDouble()).toFloat()
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    // 복구 인증 다이얼로그 표시 여부 (잠금 비밀번호를 잊었을 때)
    private var showRecoveryVerify by mutableStateOf(false)
    private var recoveryStep by mutableStateOf(RecoveryStep.CHOOSER)

    private enum class RecoveryStep { CHOOSER, QUESTIONS, KEY }

    /** 복구 인증 성공 후 공통 처리. 비밀번호 변경을 강력히 권장한다. */
    private fun showRecoveredDialog(title: String, message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("확인") { _, _ ->
                completeUnlock()
                Toast.makeText(this, "복구 인증으로 잠금을 해제했습니다.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소") { _, _ -> showRecoveryVerify = false }
            .show()
    }

    /** 기록에 남길 해제 방식. 인증 분기마다 갱신된다. */
    private var usedUnlockMethod: String = "잠금 해제"

    private fun completeUnlock() {
        AppLockPreferences.setDuressSession(this, false)
        // 성공했으므로 실패 횟수와 잠금 페널티를 초기화한다.
        AppLockPreferences.clearFailedAttempts(this)
        if (targetPackage.isNotEmpty()) {
            AppLockPreferences.setTemporarilyUnlocked(targetPackage)
            // 실행 기록 타임라인에 남긴다 (잠금 해제 이력 추적용).
            runCatching {
                AppLockPreferences.recordSession(
                    context = this,
                    packageName = targetPackage,
                    appName = appName,
                    method = usedUnlockMethod
                )
            }
            if (AppLockPreferences.isPrivacyShadeAutoEnabled(this, targetPackage)) {
                PrivacyShadeOverlayService.start(this)
            }
            if (targetPackage != packageName) InstalledAppsManager.launchApp(this, targetPackage)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The authentication screen must never appear in captures or the recents thumbnail.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        enableEdgeToEdge()

        val initialPkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        updateTargetApp(initialPkg)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager?.registerListener(orientationListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Handle Back Button: strictly block access to locked app and return to phone home screen
        onBackPressedDispatcher.addCallback(this) {
            goToHomeScreen()
        }

        setContent {
            MyApplicationTheme {
                val lockConfig = AppLockPreferences.getLockConfig(this@LockActivity)

                // 복구 인증 처리
                if (showRecoveryVerify) {
                    val blockedMs = RecoveryQuestionManager.blockedRemainingMs(this@LockActivity)
                    val questions = RecoveryQuestionManager.getQuestions(this@LockActivity)
                    val hasKey = RecoveryKeyManager.hasKey(this@LockActivity)

                    // 복구 수단 선택 → 해당 수단으로 진행
                    if (recoveryStep == RecoveryStep.CHOOSER) {
                        RecoveryMethodChooserDialog(
                            hasQuestions = questions.isNotEmpty(),
                            hasKey = hasKey,
                            onUseQuestions = {
                                if (blockedMs > 0L) {
                                    val minutes = blockedMs / 60_000L + 1
                                    Toast.makeText(this@LockActivity, "복구 시도가 잠겨 있습니다. 약 ${minutes}분 후 다시 시도해 주세요.", Toast.LENGTH_LONG).show()
                                } else recoveryStep = RecoveryStep.QUESTIONS
                            },
                            onUseKey = { recoveryStep = RecoveryStep.KEY },
                            onDismiss = { showRecoveryVerify = false }
                        )
                    }

                    // 12자리 복구키: 추적이 어려우므로 시도 제한을 두지 않는다.
                    if (recoveryStep == RecoveryStep.KEY) {
                        RecoveryKeyVerifyDialog(
                            failedAttempts = RecoveryKeyManager.failedAttempts(this@LockActivity),
                            onSubmit = { input ->
                                if (RecoveryKeyManager.verifyKey(this@LockActivity, input)) {
                                    showRecoveryVerify = false
                                    usedUnlockMethod = "12자리 복구키"
                                    runCatching {
                                        AppLockPreferences.recordSession(
                                            context = this@LockActivity,
                                            packageName = targetPackage,
                                            appName = appName,
                                            method = "12자리 복구키 인증"
                                        )
                                        AppLockPreferences.recordIntruderAttempt(
                                            context = this@LockActivity,
                                            packageName = targetPackage,
                                            appName = appName,
                                            attempts = 0,
                                            usedLockType = "복구키로 잠금 해제"
                                        )
                                    }
                                    showRecoveredDialog("복구키 인증 완료", "복구키로 인증되었습니다. 지금 잠금 비밀번호를 새로 설정하는 것을 강력히 권장합니다.")
                                } else {
                                    val attempts = RecoveryKeyManager.failedAttempts(this@LockActivity)
                                    Toast.makeText(this@LockActivity, "복구키가 일치하지 않습니다. (누적 ${attempts}회)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDismiss = { recoveryStep = RecoveryStep.CHOOSER }
                        )
                    }

                    if (recoveryStep == RecoveryStep.QUESTIONS && showRecoveryVerify) {
                    when {
                        questions.isEmpty() -> {
                            showRecoveryVerify = false
                            Toast.makeText(this@LockActivity, "등록된 복구 질문이 없습니다. 설정에서 먼저 등록해 주세요.", Toast.LENGTH_LONG).show()
                        }
                        blockedMs > 0L -> {
                            showRecoveryVerify = false
                            val minutes = (blockedMs / 60_000L + 1)
                            Toast.makeText(this@LockActivity, "복구 시도가 잠겨 있습니다. 약 ${minutes}분 후에 다시 시도해 주세요.", Toast.LENGTH_LONG).show()
                        }
                        else -> RecoveryVerifyDialog(
                            questions = questions,
                            onSubmit = { answers ->
                                if (RecoveryQuestionManager.verify(this@LockActivity, answers)) {
                                    RecoveryQuestionManager.registerSuccess(this@LockActivity)
                                    showRecoveryVerify = false
                                    usedUnlockMethod = "개인 확인 질문 복구"
                                    runCatching {
                                        AppLockPreferences.recordSession(
                                            context = this@LockActivity,
                                            packageName = targetPackage,
                                            appName = appName,
                                            method = "개인 확인 질문 복구 인증"
                                        )
                                    }
                                    // 복구했다는 사실은 침입 기록에도 남긴다.
                                    runCatching {
                                        AppLockPreferences.recordIntruderAttempt(
                                            context = this@LockActivity,
                                            packageName = targetPackage,
                                            appName = appName,
                                            attempts = 0,
                                            usedLockType = "복구 인증으로 잠금 해제"
                                        )
                                    }
                                    showRecoveredDialog("복구 인증 완료", "개인 확인 질문으로 인증되었습니다. 지금 잠금 비밀번호를 새로 설정하는 것을 강력히 권장합니다.")
                                } else {
                                    val locked = RecoveryQuestionManager.registerFailure(this@LockActivity)
                                    Toast.makeText(
                                        this@LockActivity,
                                        if (locked) "답이 일치하지 않습니다. 복구 시도가 잠겨 1시간 동안 사용할 수 없습니다."
                                        else "답이 일치하지 않습니다.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onDismiss = { recoveryStep = RecoveryStep.CHOOSER }
                        )
                    }
                    }
                }

                androidx.compose.runtime.LaunchedEffect(lockConfig.biometricEnabled) {
                    if (lockConfig.biometricEnabled && BiometricHelper.isBiometricAvailable(this@LockActivity)) {
                        kotlinx.coroutines.delay(300)
                        requestBiometricUnlock(
                            appName = appName,
                            onSuccess = {
                                usedUnlockMethod = "생체 인증"
                                completeUnlock()
                                Toast.makeText(this@LockActivity, "생체 인증 성공!", Toast.LENGTH_SHORT).show()
                            },
                            onFailed = {
                                recordFailedIntruderAttempt(
                                    lockConfig = lockConfig,
                                    attempts = 1,
                                    usedType = "생체 인식"
                                )
                            }
                        )
                    }
                }

                key(additionalCredentialRequired) { LockOverlayScreen(
                    appName = appName,
                    appIcon = appIcon,
                    lockType = lockConfig.lockType,
                    gridSize = lockConfig.gridSize,
                    targetPattern = lockConfig.savedPattern,
                    targetPin = lockConfig.savedPin,
                    targetDuressPin = AppLockPreferences.getDuressPin(this@LockActivity),
                    targetDecoyPin = AppLockPreferences.getDecoyPin(this@LockActivity),
                    targetPassword = lockConfig.savedPassword,
                    targetCalculatorCode = lockConfig.savedCalculatorCode,
                    targetKnockCode = lockConfig.savedKnockCode,
                    backgroundTheme = lockConfig.backgroundTheme,
                    customBackgroundUri = lockConfig.customLockBackgroundUri,
                    emergencyContact = AppLockPreferences.getEmergencyContact(this@LockActivity),
                    lockMessage = lockConfig.lockScreenMessage,
                    lockIconScale = lockConfig.lockIconScale,
                    lockBackgroundDim = lockConfig.lockBackgroundDim,
                    lockClockStyle = lockConfig.lockClockStyle,
                    lockAccent = lockConfig.lockAccent,
                    lockPreset = lockConfig.lockPreset,
                    lockBackgroundBlur = lockConfig.lockBackgroundBlur,
                    lockPanelAlpha = lockConfig.lockPanelAlpha,
                    lockCornerRadius = lockConfig.lockCornerRadius,
                    lockClockPosition = lockConfig.lockClockPosition,
                    lockIconShape = lockConfig.lockIconShape,
                    lockFontStyle = lockConfig.lockFontStyle,
                    lockChargingStyle = lockConfig.lockChargingStyle,
                    isLockQuickActionsEnabled = lockConfig.isLockQuickActionsEnabled,
                    biometricEnabled = lockConfig.biometricEnabled,
                    isStealthPattern = lockConfig.isStealthPattern,
                    isFakeCrashEnabled = lockConfig.isFakeCrashEnabled,
                    fakeScreenKind = fakeScreenKindFor(targetPackage, appName),
                    isVibrationEnabled = lockConfig.isVibrationEnabled,
                    isRandomPinKeypad = lockConfig.isRandomPinKeypad,
                    initialLockoutRemainingMs = AppLockPreferences.getLockoutRemainingMs(this),
                    onRequestRecovery = {
                    recoveryStep = RecoveryStep.CHOOSER
                    showRecoveryVerify = true
                },
                    isIntruderSirenEnabled = lockConfig.isIntruderSirenEnabled,
                    deviceTiltDegrees = deviceTiltDegrees,
                    onRequestBiometric = {
                        requestBiometricUnlock(
                            appName = appName,
                            onSuccess = {
                                usedUnlockMethod = "생체 인증"
                                completeUnlock()
                                Toast.makeText(this@LockActivity, "생체 인증 성공!", Toast.LENGTH_SHORT).show()
                            },
                            onFailed = {
                                recordFailedIntruderAttempt(
                                    lockConfig = lockConfig,
                                    attempts = 1,
                                    usedType = "생체 인식"
                                )
                            }
                        )
                    },
                    onUnlockSuccess = {
                        completeUnlock()
                        Toast.makeText(this@LockActivity, "인증 성공!", Toast.LENGTH_SHORT).show()
                    },
                    onCredentialVerified = { metrics ->
                        if (additionalCredentialRequired) {
                            completeUnlock()
                            Toast.makeText(this@LockActivity, "추가 인증 성공!", Toast.LENGTH_SHORT).show()
                        } else {
                            handleCredentialVerification(lockConfig, metrics)
                        }
                    },
                    onDuressUnlock = {
                        AppLockPreferences.setDuressSession(this@LockActivity, true)
                        AppLockPreferences.resetAllTemporaryUnlocks()
                        PrivacyShadeOverlayService.start(this@LockActivity)
                        AppLockPreferences.recordIntruderAttempt(this@LockActivity, targetPackage, appName, 1, "듀레스 PIN")
                        if (targetPackage == packageName) {
                            startActivity(Intent(this@LockActivity, DisguisedEntryActivity::class.java).apply {
                                putExtra(DisguisedEntryActivity.EXTRA_SHOW_CALCULATOR, true)
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                            finish()
                        } else {
                            // A duress PIN must never grant access to another protected app.
                            goToHomeScreen()
                        }
                    },
                    onDecoyUnlock = {
                        AppLockPreferences.recordIntruderAttempt(this@LockActivity, targetPackage, appName, 1, "미끼 PIN")
                        startActivity(Intent(this@LockActivity, DecoyVaultActivity::class.java))
                        finish()
                    },
                    onFailedAttempt = { attempts ->
                        recordFailedIntruderAttempt(
                            lockConfig = lockConfig,
                            attempts = attempts,
                            usedType = lockConfig.lockType.title
                        )
                    },
                    onDismiss = {
                        goToHomeScreen()
                    },
                    modifier = Modifier.fillMaxSize()
                ) }

            }
        }
    }

    private fun handleCredentialVerification(lockConfig: com.example.model.LockConfig, metrics: BehavioralInputMetrics) {
        usedUnlockMethod = "AI 가드 인증"
        if (!lockConfig.isAiGuardEnabled) {
            usedUnlockMethod = "${lockConfig.lockType.title} 인증"
            completeUnlock()
            Toast.makeText(this, "인증 성공!", Toast.LENGTH_SHORT).show()
            return
        }
        when (BehavioralGuard.evaluateAndLearn(this, metrics, lockConfig.aiGuardSensitivity)) {
            BehavioralGuardResult.ADDITIONAL_AUTH_REQUIRED -> {
                usedUnlockMethod = "AI 가드 추가 인증"
                Toast.makeText(this, "AI 가드가 평소와 다른 입력 패턴을 감지했습니다. 추가 인증이 필요합니다.", Toast.LENGTH_LONG).show()
                if (lockConfig.isAiGuardVoiceRecordingEnabled) AiGuardAudioRecorder.recordFiveSeconds(this)
                when (lockConfig.aiGuardFallback) {
                    AiGuardFallback.DEVICE_CREDENTIAL -> BiometricHelper.authenticateDeviceCredential(this, onSuccess = {
                        usedUnlockMethod = "AI 가드 → 기기 인증"
                        completeUnlock()
                        Toast.makeText(this, "기기 인증 성공!", Toast.LENGTH_SHORT).show()
                    }, onUnavailable = { requireAppLockReentry() })
                    AiGuardFallback.BIOMETRIC -> requestBiometricUnlock(appName, onSuccess = {
                        usedUnlockMethod = "AI 가드 → 생체 인증"
                        completeUnlock()
                        Toast.makeText(this, "추가 인증 성공!", Toast.LENGTH_SHORT).show()
                    }, onFailed = { requireAppLockReentry() })
                    AiGuardFallback.REENTER_APP_LOCK -> requireAppLockReentry()
                }
            }
            BehavioralGuardResult.LEARNING -> {
                usedUnlockMethod = "AI 가드 학습 중 통과"
                completeUnlock()
                Toast.makeText(this, "AI 가드가 정상 입력 패턴을 학습 중입니다.", Toast.LENGTH_SHORT).show()
            }
            BehavioralGuardResult.TRUSTED -> {
                usedUnlockMethod = "AI 가드 신뢰 입력"
                completeUnlock()
                Toast.makeText(this, "AI 가드 인증 성공!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requireAppLockReentry() {
        additionalCredentialRequired = true
        Toast.makeText(this, "AI 가드 추가 인증: 잠금 정보를 한 번 더 입력하세요.", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        sensorManager?.unregisterListener(orientationListener)
        super.onDestroy()
    }

    private fun requestBiometricUnlock(
        appName: String,
        onSuccess: () -> Unit,
        onFailed: () -> Unit
    ) {
        val status = BiometricHelper.checkBiometricStatus(this)
        when (status) {
            BiometricStatus.AVAILABLE -> {
                BiometricHelper.authenticate(
                    activity = this,
                    title = "$appName 잠금 해제",
                    subtitle = "지문 센서를 터치하거나 얼굴을 인식시켜 주세요",
                    onSuccess = onSuccess,
                    onError = { code, msg ->
                        if (code != 13 && code != 10) {
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailed = {
                        Toast.makeText(this, "생체 인식에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        onFailed()
                    }
                )
            }
            BiometricStatus.NOT_ENROLLED -> {
                Toast.makeText(this, "등록된 지문 또는 얼굴이 없습니다. 기기 설정에서 등록하세요.", Toast.LENGTH_LONG).show()
                try {
                    startActivity(BiometricHelper.getBiometricEnrollIntent())
                } catch (_: Exception) {}
            }
            BiometricStatus.NO_HARDWARE, BiometricStatus.HW_UNAVAILABLE, BiometricStatus.UNAVAILABLE -> {
                Toast.makeText(this, status.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun recordFailedIntruderAttempt(lockConfig: com.example.model.LockConfig, attempts: Int, usedType: String) {
        // 실패 횟수 한도(선택)에 걸리면 정해진 시간 동안 입력이 막힌다.
        if (AppLockPreferences.recordFailedAttempt(this)) {
            val minutes = AppLockPreferences.getLockoutMinutes(this)
            Toast.makeText(
                this,
                "인증 ${AppLockPreferences.getMaxFailedAttempts(this)}회 실패로 ${minutes}분 동안 잠금이 차단됩니다.",
                Toast.LENGTH_LONG
            ).show()
        }
        // Evidence capture is independent of AI Guard: the first wrong credential can
        // create a short local audio/video record when the required permissions exist.
        if (attempts == 1 && targetPackage.isNotEmpty()) {
            // 실패가 일어난 그 순간의 위치를 한 번만 확보해 증거와 함께 남긴다.
            val location = IntruderLocationCapture.capture(this)
            if (lockConfig.isAiGuardVoiceRecordingEnabled) {
                AiGuardAudioRecorder.recordFiveSeconds(this) { audioPath ->
                    audioPath?.let {
                        AppLockPreferences.recordIntruderAttempt(
                            context = this@LockActivity,
                            packageName = targetPackage,
                            appName = appName,
                            attempts = attempts,
                            usedLockType = "$usedType · 5초 음성",
                            audioPath = it,
                            latitude = location?.latitude,
                            longitude = location?.longitude,
                            locationText = location?.coordinatesText,
                            placeName = location?.placeName
                        )
                    }
                }
            }
            IntruderCameraHelper.captureIntruderVideo(this, this) { videoPath ->
                videoPath?.let {
                    AppLockPreferences.recordIntruderAttempt(
                        context = this@LockActivity,
                        packageName = targetPackage,
                        appName = appName,
                        attempts = attempts,
                        usedLockType = "$usedType · 5초 영상",
                        videoPath = it,
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        locationText = location?.coordinatesText,
                        placeName = location?.placeName
                    )
                }
                recordPhotoEvidence(lockConfig, attempts, usedType, location)
            }
        } else recordPhotoEvidence(lockConfig, attempts, usedType, null)
    }

    private fun recordPhotoEvidence(
        lockConfig: com.example.model.LockConfig,
        attempts: Int,
        usedType: String,
        location: CaptureLocation?
    ) {
        if (attempts < lockConfig.intruderSelfieThreshold || targetPackage.isEmpty()) return
        if (lockConfig.isIntruderSelfieEnabled) {
            IntruderCameraHelper.captureIntruderSelfie(this@LockActivity, this@LockActivity) { photoPath ->
                AppLockPreferences.recordIntruderAttempt(
                    context = this@LockActivity,
                    packageName = targetPackage,
                    appName = appName,
                    attempts = attempts,
                    usedLockType = usedType,
                    photoPath = photoPath,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    locationText = location?.coordinatesText,
                    placeName = location?.placeName
                )
            }
        } else {
            AppLockPreferences.recordIntruderAttempt(
                context = this@LockActivity,
                packageName = targetPackage,
                appName = appName,
                attempts = attempts,
                usedLockType = usedType,
                latitude = location?.latitude,
                longitude = location?.longitude,
                locationText = location?.coordinatesText,
                placeName = location?.placeName
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        setIntent(intent)
        val newPkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        if (newPkg.isNotEmpty() && newPkg != targetPackage) {
            updateTargetApp(newPkg)
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun updateTargetApp(pkg: String) {
        targetPackage = pkg
        if (pkg.isNotEmpty()) {
            val pm = packageManager
            appName = try {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg, 0)
                }
                pm.getApplicationLabel(appInfo).toString().ifBlank { pkg }
            } catch (_: Exception) {
                pkg
            }

            appIcon = try {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg, 0)
                }
                pm.getApplicationIcon(appInfo)
            } catch (_: Exception) {
                null
            }
        } else {
            appName = "보호된 앱"
            appIcon = null
        }
    }

    private fun goToHomeScreen() {
        val didGlobalHome = AppLockAccessibilityService.performGlobalHome()
        if (!didGlobalHome) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            try {
                startActivity(homeIntent)
            } catch (_: Exception) {}
        }
        finish()
    }

    /**
     * Per-app disguises are deliberately chosen locally: no app names or package
     * information leave the device.  The checks cover the usual package/name forms
     * while still leaving every other protected app on the crash disguise.
     */
    private fun fakeScreenKindFor(pkg: String, label: String): FakeScreenKind {
        AppLockPreferences.fakeScreenFor(this, pkg)?.let { saved ->
            return runCatching { FakeScreenKind.valueOf(saved) }.getOrDefault(FakeScreenKind.CRASH)
        }
        val key = "$pkg $label".lowercase()
        return when {
            listOf("gallery", "album", "photos", "사진", "갤러리").any(key::contains) -> FakeScreenKind.EMPTY_ALBUM
            listOf("bank", "banking", "finance", "card", "은행", "금융", "카드").any(key::contains) -> FakeScreenKind.CALCULATOR
            else -> FakeScreenKind.CRASH // KakaoTalk and other apps use the error screen.
        }
    }
}
