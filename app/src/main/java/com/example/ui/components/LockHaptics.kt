package com.example.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 잠금 입력 피드백 (촉각 + 청각).
 *
 * 손가락이 지나간 지점을 세는 것이 패턴 입력의 절반이다.
 * 화면을 보지 않아도 그릴 수 있게 만드는 것이 목적이라,
 * 눈으로 확인되는 정보보다 손으로 느껴지는 정보를 우선한다.
 *
 *   노드 하나    아주 짧고 낮은 틱 (연속으로 지날 때 조금 더 낮게)
 *   패턴 완성    낮은 thud 하나
 *   오답        낮고 둔탁한 톤 + 짧은 진동 두 번
 *
 * 진동을 끄면 소리만, 소리를 끄면 진동만 남는다.
 * 둘 다 끌 수 있다(터치 진동 설정).
 */
class LockHaptics private constructor(
    private val vibrator: Vibrator,
    private val tone: ToneGenerator?
) {

    private var lastTickAt: Long = 0L

    /** 노드 하나를 지났다. */
    fun onNodePassed() {
        val now = System.currentTimeMillis()
        val rapid = now - lastTickAt < 70L
        lastTickAt = now

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 짧을수록, 앰플리튜드가 낮을수록 "조용하고 무거운" 느낌이다.
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        if (rapid) 8L else 14L,
                        if (rapid) 60 else 110
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (rapid) 8 else 14)
            }
        }

        // 빠른 연속일 때 음을 살짝 낮춰 끊기지 않게 한다.
        tone?.startTone(
            if (rapid) TONE_PROP_BEEP2 else TONE_PROP_BEEP,
            if (rapid) 40 else 25
        )
    }

    /** 패턴이 완성됐다. 낮은 thud. */
    fun onCompleted() {
        lastTickAt = 0L
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 짧고 강한 두 물결이 "끝났다" 는 느낌을 준다.
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 26, 60, 40),
                        intArrayOf(0, 160, 0, 110),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 26, 60, 40), -1)
            }
        }
        tone?.startTone(TONE_PROP_ACK, 90)
    }

    /** 오답. 성공과 확실히 구분되도록 길고 낮게. */
    fun onError() {
        lastTickAt = 0L
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 55, 90, 70),
                        intArrayOf(0, 200, 0, 200),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 55, 90, 70), -1)
            }
        }
        tone?.startTone(TONE_PROP_NACK, 220)
    }

    /** 오답 처리 후 잠금을 거부할 때 호출(짧게 한 번 더). */
    fun onCancel() {
        lastTickAt = 0L
    }

    fun release() {
        runCatching { tone?.release() }
    }

    companion object {
        private const val TONE_PROP_BEEP = ToneGenerator.TONE_PROP_BEEP
        private const val TONE_PROP_BEEP2 = ToneGenerator.TONE_PROP_BEEP2
        private const val TONE_PROP_ACK = ToneGenerator.TONE_PROP_ACK
        private const val TONE_PROP_NACK = ToneGenerator.TONE_PROP_NACK

        fun create(context: Context, withSound: Boolean): LockHaptics {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val tone = if (withSound) {
                runCatching {
                    @Suppress("DEPRECATION")
                    ToneGenerator(AudioManager.STREAM_SYSTEM, 60)
                }.getOrNull()
            } else null
            return LockHaptics(vibrator, tone)
        }
    }
}