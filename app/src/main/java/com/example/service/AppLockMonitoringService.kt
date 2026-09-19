package com.example.service

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.example.LockActivity
import com.example.util.AppLockPermissionHelper
import com.example.util.AppLockPreferences
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
                context.startService(intent)
            } catch (_: Exception) {
                // Ignore if background restrictions apply
            }
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var lastForegroundPackage = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
                                delay(300)
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

                            if (AppLockPreferences.isPackageLocked(applicationContext, currentPackage)) {
                                if (!AppLockPreferences.isTemporarilyUnlocked(currentPackage)) {
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
                delay(300)
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
    }
}
