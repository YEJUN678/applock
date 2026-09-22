package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/** Triggers once when the phone is placed face-down on a surface. */
class FaceDownDetector(context: Context, private val onFaceDown: () -> Unit) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var listening = false
    private var wasFaceDown = false
    fun start() { if (!listening && sensor != null) { manager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL); listening = true } }
    fun stop() { if (listening) manager?.unregisterListener(this); listening = false; wasFaceDown = false }
    override fun onSensorChanged(event: SensorEvent?) {
        val down = event != null && event.values[2] < -8.2f
        if (down && !wasFaceDown) onFaceDown()
        wasFaceDown = down
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
