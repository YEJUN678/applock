package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects sudden vigorous device shaking (Panic Mode) to immediately re-lock all protected apps.
 */
class PanicShakeDetector(
    private val context: Context,
    strength: Int = 10,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastShakeTimestamp = 0L
    private var isListening = false
    // Level 10 is deliberately decisive: an intentional sharp shake locks
    // immediately, while the cooldown prevents repeated triggers.
    private val shakeThresholdGravity = 3.4f - (strength.coerceIn(1, 10) - 1) * 0.2f

    fun start() {
        if (!isListening && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            isListening = true
        }
    }

    fun stop() {
        if (isListening) {
            sensorManager?.unregisterListener(this)
            isListening = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Gravity normalization (Earth gravity is ~9.8 m/s^2)
        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // Net g-force
        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (gForce > shakeThresholdGravity) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTimestamp > SHAKE_COOLDOWN_MS) {
                lastShakeTimestamp = now
                onShakeDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    companion object {
        private const val SHAKE_COOLDOWN_MS = 1500L
    }
}
