package com.example.util

import android.content.Context
import com.example.model.GuardPriority
import com.example.model.GuardProfile
import com.example.model.GuardStrength
import com.example.model.LockConfig

/**
 * 세 가지 답을 실제 잠금 설정으로 바꿔 준다.
 *
 * 스위치가 20개 넘게 쌓여 있으면 아무도 다 고르지 않는다.
 * 그래서 "누구에게 / 얼마나 / 무엇을" 세 가지만 답하게 하고,
 * 나머지는 여기서 자동으로 맞추게 한다.
 *
 * 사용자가 개별 스위치를 직접 바꾼 경우 그 값을 덮어쓰지 않도록,
 * 자동 적용은 "한 번만" 하고 그 뒤로는 사용자가 관리하게 한다.
 */
object GuardPresetStore {

    private const val PREFS = "guard_preset"
    private const val KEY_PROFILE = "profile"
    private const val KEY_STRENGTH = "strength"
    private const val KEY_PRIORITY = "priority"
    private const val KEY_APPLIED = "applied"

    fun profile(context: Context): GuardProfile =
        GuardProfile.fromName(prefs(context).getString(KEY_PROFILE, null))

    fun strength(context: Context): GuardStrength =
        GuardStrength.fromName(prefs(context).getString(KEY_STRENGTH, null))

    fun priority(context: Context): GuardPriority =
        GuardPriority.fromName(prefs(context).getString(KEY_PRIORITY, null))

    fun setProfile(context: Context, value: GuardProfile) {
        prefs(context).edit().putString(KEY_PROFILE, value.name).apply()
    }

    fun setStrength(context: Context, value: GuardStrength) {
        prefs(context).edit().putString(KEY_STRENGTH, value.name).apply()
    }

    fun setPriority(context: Context, value: GuardPriority) {
        prefs(context).edit().putString(KEY_PRIORITY, value.name).apply()
    }

    /** 세 답이 모두 갖춰졌는지. 하나라도 비면 자동 설정을 적용하지 않는다. */
    fun isComplete(context: Context): Boolean {
        val p = prefs(context)
        return p.contains(KEY_PROFILE) && p.contains(KEY_STRENGTH) && p.contains(KEY_PRIORITY)
    }

    /** 이미 한 번 자동 적용했는지. 한 번만 적용하고 그 뒤는 사용자가 직접 관리한다. */
    fun alreadyApplied(context: Context): Boolean = prefs(context).getBoolean(KEY_APPLIED, false)

    fun markApplied(context: Context) {
        prefs(context).edit().putBoolean(KEY_APPLIED, true).apply()
    }

    /**
     * 세 답을 실제 LockConfig 로 변환한다.
     *
     * 결정의 무게를 세 가지로 줄이는 것이 목적이고, 개별 스위치는 그대로 두고
     * 강한 쪽으로만 켠다(사용자가 꺼 둔 것을 임의로 끄지 않는다).
     */
    fun apply(
        context: Context,
        config: LockConfig,
        profile: GuardProfile,
        strength: GuardStrength,
        priority: GuardPriority
    ): LockConfig {
        var next = config

        // 강도: 신호 안 보임일수록 위장 기능을 켠다.
        // 가족 앞이면 오히려 눈치 보이므로 흔들림 대응은 끈다.
        val wantPanicShake = when {
            strength == GuardStrength.STEALTH -> true
            profile == GuardProfile.FAMILY -> false
            else -> config.isPanicShakeEnabled
        }
        next = next.copy(
            isStealthPattern = strength != GuardStrength.NORMAL || config.isStealthPattern,
            isRandomPinKeypad = strength == GuardStrength.STEALTH || config.isRandomPinKeypad,
            isPanicShakeEnabled = wantPanicShake
        )

        // 무엇을 지키는가: 돈이 최우선이면 알림까지 가려야 한다(금액이 새는 건 알림이다).
        if (priority == GuardPriority.MONEY) {
            next = next.copy(isNotificationPrivacyEnabled = true)
        } else {
            // 사진·메신저는 "내용"이 아니라 "접근"을 막으면 된다.
            next = next.copy(isNotificationPrivacyEnabled = config.isNotificationPrivacyEnabled)
        }

        // 동료 앞이면 재잠금 시간을 짧게 두어야 새 화면을 못 연다.
        next = next.copy(
            lockTimeoutSeconds = when {
                profile == GuardProfile.FAMILY -> 300
                profile == GuardProfile.COWORKER -> 30
                else -> 15
            }
        )

        // 신호 안 보임이면 실패 페널티를 강하게 둔다(무한 추측 방지).
        // maxFailedAttempts / lockoutMinutes 는 LockConfig 가 아니라 잠금 상태 저장소에 있으므로
        // 여기서는 켜고 끄는 쪽만 다룬다.
        next = next.copy(
            isAppSelfProtectEnabled = when (strength) {
                GuardStrength.SIGNAL -> config.isAppSelfProtectEnabled
                GuardStrength.NORMAL -> true
                GuardStrength.STEALTH -> true
            }
        )

        // 입력 피드백은 항상 켠다. 어두운 곳에서 그릴 수 있게 하는 기본값이다.
        next = next.copy(isVibrationEnabled = true)

        // 가족 앞이면 위장 기능이 오히려 수상해진다(계산기를 왜 써).
        if (profile == GuardProfile.FAMILY) {
            next = next.copy(isFakeCrashEnabled = false)
        }

        return next
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}