package com.example.service

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.LockActivity
import com.example.util.AppLockPermissionHelper
import com.example.util.AppLockPreferences
import com.example.util.PanicShakeDetector
import com.example.util.FaceDownDetector
import com.example.service.PrivacyShadeOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppLockMonitoringService : Service() {

    companion object {
        fun startService(context: Context) {
            try {
                val intent = Intent(context, AppLockMonitoringService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
            } catch (_: Exception) {
                // Ignore if background restrictions apply
            }
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var lastForegroundPackage = ""
    private var panicShakeDetector: PanicShakeDetector? = null
    private var faceDownDetector: FaceDownDetector? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                // Users may turn this off when a short re-unlock grace period is preferred.
                if (AppLockPreferences.getLockConfig(applicationContext).isScreenOffLockEnabled) {
                    AppLockPreferences.resetAllTemporaryUnlocks()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        promoteToForeground()
        try {
            registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        } catch (_: Exception) {}
        updateShakeDetector()
        updateFaceDownDetector()
    }

    private fun updateFaceDownDetector() {
        if (AppLockPreferences.isFaceDownProtectionEnabled(applicationContext)) {
            if (faceDownDetector == null) {
                faceDownDetector = FaceDownDetector(applicationContext) {
                    AppLockPreferences.resetAllTemporaryUnlocks()
                    PrivacyShadeOverlayService.start(applicationContext)
                    if (lastForegroundPackage.isNotBlank()) LockActivity.start(applicationContext, lastForegroundPackage)
                }.also { it.start() }
            }
        } else { faceDownDetector?.stop(); faceDownDetector = null }
    }

    private fun promoteToForeground() {
        val channelId = "app_lock_monitoring"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(channelId, "앱 잠금 보호", NotificationManager.IMPORTANCE_MIN)
            )
        }
        startForeground(3101, NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("앱 잠금 보호 작동 중")
            .setContentText("잠긴 앱과 긴급 흔들기 기능을 감시합니다.")
            .setOngoing(true)
            .build())
    }

    private fun updateShakeDetector() {
        val config = AppLockPreferences.getLockConfig(applicationContext)
        if (config.isPanicShakeEnabled) {
            if (panicShakeDetector == null) {
                panicShakeDetector = PanicShakeDetector(applicationContext, config.panicShakeStrength) {
                    // Triggered when shaken in ANY app across the entire system!
                    AppLockPreferences.resetAllTemporaryUnlocks()

                    // Emergency haptic vibration
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 80, 50, 150), -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(250)
                    }

                    // Return to home screen immediately or launch lock screen
                    val kicked = AppLockAccessibilityService.performGlobalHome()
                    if (!kicked && lastForegroundPackage.isNotEmpty()) {
                        LockActivity.start(applicationContext, lastForegroundPackage)
                    }

                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(applicationContext, "🚨 긴급 안심 흔들기 감지: 모든 앱이 즉시 잠금 상태로 전환되었습니다!", Toast.LENGTH_SHORT).show()
                    }
                }
                panicShakeDetector?.start()
            }
        } else {
            panicShakeDetector?.stop()
            panicShakeDetector = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateShakeDetector()
        updateFaceDownDetector()
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            while (isActive) {
                try {
                    if (AppLockPermissionHelper.hasUsageStatsPermission(applicationContext) && usageStatsManager != null) {
                        val currentPackage = getTopPackage(usageStatsManager)
                        if (currentPackage.isNotEmpty() && currentPackage != applicationContext.packageName) {
                            // Ignore keyboard / IME popups
                            if (AppLockPermissionHelper.isInputMethodPackage(applicationContext, currentPackage)) {
                                delay(150)
                                continue
                            }

                            if (lastForegroundPackage.isNotEmpty() && lastForegroundPackage != currentPackage) {
                                if (AppLockPreferences.isPackageLocked(applicationContext, lastForegroundPackage)) {
                                    val config = AppLockPreferences.getLockConfig(applicationContext)
                                    if (config.lockTimeoutSeconds <= 0) {
                                        AppLockPreferences.clearTemporarilyUnlocked(lastForegroundPackage)
                                    }
                                }
                            }
                            lastForegroundPackage = currentPackage

                            val isUninstallAttempt = AppLockPreferences.isUninstallProtectionEnabled(applicationContext) &&
                                    (currentPackage.contains("packageinstaller") || currentPackage == "com.google.android.packageinstaller" || currentPackage == "com.android.packageinstaller")

                            if (AppLockPreferences.isPackageLocked(applicationContext, currentPackage) || isUninstallAttempt) {
                                if (AppLockPreferences.isScheduleLockActive(applicationContext) || !AppLockPreferences.isTemporarilyUnlocked(currentPackage)) {
                                    LockActivity.start(applicationContext, currentPackage)
                                } else {
                                    AppLockPreferences.touchTemporarilyUnlocked(currentPackage)
                                }
                            }
                        }

                    }
                } catch (_: Exception) {
                    // Safe guard
                }
                delay(150)
            }
        }
    }

    private fun getTopPackage(usageStatsManager: UsageStatsManager): String {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10_000
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime) ?: return ""
        val event = UsageEvents.Event()
        var lastPkg = ""
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && event.eventType == UsageEvents.Event.ACTIVITY_PAUSED)) {
                lastPkg = event.packageName
            }
        }
        return lastPkg
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        panicShakeDetector?.stop()
        faceDownDetector?.stop()
        panicShakeDetector = null
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
    }
}
