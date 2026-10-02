package com.example.util

import android.content.Context
import org.json.JSONObject
import kotlin.math.abs

/** A small, local-only behavioural model. It stores aggregate features, never credentials. */
data class BehavioralInputMetrics(
    val durationMs: Long,
    val averageIntervalMs: Long,
    val averagePressure: Float,
    val deviceTiltDegrees: Float
)

enum class BehavioralGuardResult { LEARNING, TRUSTED, ADDITIONAL_AUTH_REQUIRED }

object BehavioralGuard {
    private const val PREFS = "behavioral_guard"
    private const val KEY_MODEL = "local_model_v1"
    private const val MIN_SAMPLES = 5

    fun evaluateAndLearn(context: Context, sample: BehavioralInputMetrics, sensitivity: Int): BehavioralGuardResult {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val model = prefs.getString(KEY_MODEL, null)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject()
        val count = model.optInt("count", 0)
        if (count >= MIN_SAMPLES && isOutlier(model, sample, sensitivity)) {
            return BehavioralGuardResult.ADDITIONAL_AUTH_REQUIRED
        }

        // Exponential moving average adapts gradually without retaining individual attempts.
        val alpha = if (count == 0) 1.0 else 0.18
        fun update(name: String, value: Double) {
            val old = model.optDouble(name, value)
            model.put(name, old + (value - old) * alpha)
        }
        update("duration", sample.durationMs.coerceIn(100L, 20_000L).toDouble())
        update("interval", sample.averageIntervalMs.coerceIn(20L, 10_000L).toDouble())
        update("pressure", sample.averagePressure.coerceIn(0.1f, 2f).toDouble())
        update("tilt", sample.deviceTiltDegrees.coerceIn(0f, 90f).toDouble())
        model.put("count", count + 1)
        prefs.edit().putString(KEY_MODEL, model.toString()).apply()
        return if (count + 1 < MIN_SAMPLES) BehavioralGuardResult.LEARNING else BehavioralGuardResult.TRUSTED
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_MODEL).apply()
    }

    private fun isOutlier(model: JSONObject, sample: BehavioralInputMetrics, sensitivity: Int): Boolean {
        // Higher sensitivity means a narrower acceptable range. Values are deliberately
        // broad enough to avoid locking out users during normal one-handed use changes.
        val tolerance = when (sensitivity.coerceIn(1, 3)) { 3 -> 0.30; 2 -> 0.48; else -> 0.70 }
        fun differs(value: Double, baseline: Double, floor: Double) = abs(value - baseline) / maxOf(abs(baseline), floor) > tolerance
        val mismatches = listOf(
            differs(sample.durationMs.toDouble(), model.optDouble("duration"), 400.0),
            differs(sample.averageIntervalMs.toDouble(), model.optDouble("interval"), 80.0),
            differs(sample.averagePressure.toDouble(), model.optDouble("pressure"), 0.2),
            differs(sample.deviceTiltDegrees.toDouble(), model.optDouble("tilt"), 8.0)
        ).count { it }
        return mismatches >= 2
    }
}
