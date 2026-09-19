package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus(val message: String) {
    AVAILABLE("지문 또는 얼굴 인식을 사용할 수 있습니다"),
    NOT_ENROLLED("기기에 등록된 지문 또는 얼굴 인식이 없습니다. 기기 설정에서 등록해주세요"),
    NO_HARDWARE("기기에 생체 인식 센서(지문/얼굴)가 없습니다"),
    HW_UNAVAILABLE("생체 인식 센서를 일시적으로 사용할 수 없습니다"),
    UNAVAILABLE("생체 인식을 지원하지 않거나 사용할 수 없습니다")
}

object BiometricHelper {

    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun checkBiometricStatus(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HW_UNAVAILABLE
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    fun isBiometricAvailable(context: Context): Boolean {
        return checkBiometricStatus(context) == BiometricStatus.AVAILABLE
    }

    fun getBiometricEnrollIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    AUTHENTICATORS
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "생체 인증",
        subtitle: String = "지문 센서를 터치하거나 얼굴을 인식시켜 잠금을 해제하세요",
        negativeButtonText: String = "패턴/PIN으로 입력",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // errorCode 13 = ERROR_NEGATIVE_BUTTON (User chose pattern/pin)
                    // errorCode 10 = ERROR_USER_CANCELED
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        prompt.authenticate(promptInfo)
    }
}
