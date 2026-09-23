package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.example.util.InstalledAppsManager
import com.example.util.IntruderCameraHelper
import com.example.util.LockNotificationHelper

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

    private fun completeUnlock() {
        AppLockPreferences.setDuressSession(this, false)
        if (targetPackage.isNotEmpty()) {
            AppLockPreferences.setTemporarilyUnlocked(targetPackage)
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

        // Handle Back Button: strictly block access to locked app and return to phone home screen
        onBackPressedDispatcher.addCallback(this) {
            goToHomeScreen()
        }

        setContent {
            MyApplicationTheme {
                val lockConfig = AppLockPreferences.getLockConfig(this@LockActivity)

                androidx.compose.runtime.LaunchedEffect(lockConfig.biometricEnabled) {
                    if (lockConfig.biometricEnabled && BiometricHelper.isBiometricAvailable(this@LockActivity)) {
                        kotlinx.coroutines.delay(300)
                        requestBiometricUnlock(
                            appName = appName,
                            onSuccess = {
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

                LockOverlayScreen(
                    appName = appName,
                    appIcon = appIcon,
                    lockType = lockConfig.lockType,
                    gridSize = lockConfig.gridSize,
                    targetPattern = lockConfig.savedPattern,
                    targetPin = lockConfig.savedPin,
                    targetDuressPin = AppLockPreferences.getDuressPin(this@LockActivity),
                    targetPassword = lockConfig.savedPassword,
                    targetCalculatorCode = lockConfig.savedCalculatorCode,
                    targetKnockCode = lockConfig.savedKnockCode,
                    backgroundTheme = lockConfig.backgroundTheme,
                    customBackgroundUri = lockConfig.customLockBackgroundUri,
                    emergencyContact = AppLockPreferences.getEmergencyContact(this@LockActivity),
                    lockMessage = lockConfig.lockScreenMessage,
                    lockIconScale = lockConfig.lockIconScale,
                    lockBackgroundDim = lockConfig.lockBackgroundDim,
                    biometricEnabled = lockConfig.biometricEnabled,
                    isStealthPattern = lockConfig.isStealthPattern,
                    isFakeCrashEnabled = lockConfig.isFakeCrashEnabled,
                    fakeScreenKind = fakeScreenKindFor(targetPackage, appName),
                    isVibrationEnabled = lockConfig.isVibrationEnabled,
                    isRandomPinKeypad = lockConfig.isRandomPinKeypad,
                    isIntruderSirenEnabled = lockConfig.isIntruderSirenEnabled,
                    onRequestBiometric = {
                        requestBiometricUnlock(
                            appName = appName,
                            onSuccess = {
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
                    onDuressUnlock = {
                        AppLockPreferences.setDuressSession(this@LockActivity, true)
                        AppLockPreferences.resetAllTemporaryUnlocks()
                        if (targetPackage.isNotEmpty()) AppLockPreferences.setTemporarilyUnlocked(targetPackage)
                        PrivacyShadeOverlayService.start(this@LockActivity)
                        AppLockPreferences.recordIntruderAttempt(this@LockActivity, targetPackage, appName, 1, "듀레스 PIN")
                        if (targetPackage.isNotEmpty() && targetPackage != packageName) InstalledAppsManager.launchApp(this@LockActivity, targetPackage)
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
                )

            }
        }
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
        if (attempts >= lockConfig.intruderSelfieThreshold && targetPackage.isNotEmpty()) {
            if (lockConfig.isIntruderSelfieEnabled) {
                IntruderCameraHelper.captureIntruderSelfie(
                    context = this@LockActivity,
                    lifecycleOwner = this@LockActivity
                ) { photoPath ->
                    AppLockPreferences.recordIntruderAttempt(
                        context = this@LockActivity,
                        packageName = targetPackage,
                        appName = appName,
                        attempts = attempts,
                        usedLockType = usedType,
                        photoPath = photoPath
                    )
                }
            } else {
                AppLockPreferences.recordIntruderAttempt(
                    context = this@LockActivity,
                    packageName = targetPackage,
                    appName = appName,
                    attempts = attempts,
                    usedLockType = usedType,
                    photoPath = null
                )
            }
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
