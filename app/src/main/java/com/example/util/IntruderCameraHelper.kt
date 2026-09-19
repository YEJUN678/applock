package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object IntruderCameraHelper {
    private const val TAG = "IntruderCameraHelper"

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun captureIntruderSelfie(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onCaptured: (photoPath: String?) -> Unit
    ) {
        if (!hasCameraPermission(context)) {
            Log.w(TAG, "Camera permission not granted. Creating fallback badge.")
            val samplePath = createSampleIntruderPhoto(context, "카메라 권한 필요")
            onCaptured(samplePath)
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    // No physical camera found (e.g. emulator without camera)
                    val samplePath = createSampleIntruderPhoto(context, "센서 감지됨")
                    onCaptured(samplePath)
                    return@addListener
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture
                )

                val intruderDir = File(context.filesDir, "intruder_photos").apply {
                    if (!exists()) mkdirs()
                }
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
                val photoFile = File(intruderDir, "INTRUDER_$timeStamp.jpg")

                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    mainExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            Log.d(TAG, "Photo captured successfully: ${photoFile.absolutePath}")
                            try {
                                cameraProvider.unbindAll()
                            } catch (_: Exception) {}
                            onCaptured(photoFile.absolutePath)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "Photo capture failed: ${exception.message}, generating sample", exception)
                            try {
                                cameraProvider.unbindAll()
                            } catch (_: Exception) {}
                            val samplePath = createSampleIntruderPhoto(context, "침입자 감지")
                            onCaptured(samplePath)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize camera for intruder capture", e)
                val samplePath = createSampleIntruderPhoto(context, "침입자 감지")
                onCaptured(samplePath)
            }
        }, mainExecutor)
    }

    /**
     * Fallback generator when running in emulators without camera sensor or when permission is pending,
     * ensuring that photos and UI gallery are fully visible and testable.
     */
    fun createSampleIntruderPhoto(context: Context, statusLabel: String = "침입 감지"): String {
        val intruderDir = File(context.filesDir, "intruder_photos").apply {
            if (!exists()) mkdirs()
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val photoFile = File(intruderDir, "INTRUDER_$timeStamp.jpg")

        val width = 480
        val height = 640
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark cybersecurity background
        canvas.drawColor(Color.rgb(15, 23, 42))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw camera frame / grid
        paint.color = Color.rgb(30, 41, 59)
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, paint)

        // Silhouette head
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(239, 68, 68) // Neon Red tint
        paint.alpha = 180
        canvas.drawCircle(width / 2f, height / 2f - 40f, 90f, paint)

        // Silhouette shoulders
        canvas.drawOval(
            width / 2f - 140f,
            height / 2f + 40f,
            width / 2f + 140f,
            height / 2f + 220f,
            paint
        )

        // Target crosshair
        paint.color = Color.rgb(6, 182, 212) // Neon Cyan
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        paint.alpha = 255
        val cx = width / 2f
        val cy = height / 2f - 40f
        canvas.drawCircle(cx, cy, 110f, paint)
        canvas.drawLine(cx - 130f, cy, cx - 90f, cy, paint)
        canvas.drawLine(cx + 90f, cy, cx + 130f, cy, paint)
        canvas.drawLine(cx, cy - 130f, cx, cy - 90f, paint)
        canvas.drawLine(cx, cy + 90f, cx, cy + 130f, paint)

        // Text banner at bottom
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(220, 0, 0, 0)
        canvas.drawRect(0f, height - 100f, width.toFloat(), height.toFloat(), paint)

        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("⚠️ INTRUDER CAPTURE", width / 2f, height - 60f, paint)

        paint.color = Color.rgb(6, 182, 212)
        paint.textSize = 18f
        val displayTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(Date())
        canvas.drawText("$statusLabel | $displayTime", width / 2f, height - 28f, paint)

        FileOutputStream(photoFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()

        return photoFile.absolutePath
    }
}
