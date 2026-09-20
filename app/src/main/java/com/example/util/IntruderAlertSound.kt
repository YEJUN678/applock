package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Triggers an audible siren / warning buzzer when an intruder fails authentication repeatedly.
 */
object IntruderAlertSound {
    private const val TAG = "IntruderAlertSound"

    fun playAlertSiren() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 95)
                // Emit alternating high-frequency siren beeps
                repeat(4) {
                    toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 220)
                    delay(260)
                    toneGen.startTone(ToneGenerator.TONE_SUP_ERROR, 220)
                    delay(260)
                }
                toneGen.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to play intruder alert tone", e)
            }
        }
    }
}
