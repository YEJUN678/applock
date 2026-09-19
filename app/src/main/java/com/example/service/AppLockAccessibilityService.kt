package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.example.LockActivity
import com.example.util.AppLockPermissionHelper
import com.example.util.AppLockPreferences

class AppLockAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var isServiceRunning = false
            private set

        @Volatile
        var instance: AppLockAccessibilityService? = null
            private set

        fun performGlobalHome(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        }

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            if (isServiceRunning) return true

            // 1. Official system API check
            try {
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                val enabledList = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                if (enabledList != null) {
                    for (info in enabledList) {
                        val sInfo = info.resolveInfo?.serviceInfo
                        if (sInfo != null && sInfo.packageName == context.packageName) {
                            return true
                        }
                    }
                }
            } catch (_: Exception) {}

            // 2. Settings.Secure string fallback
            try {
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false

                val myPackage = context.packageName
                if (enabledServices.contains(myPackage)) {
                    return true
                }
            } catch (_: Exception) {}

            return false
        }
    }

    private var lastForegroundPackage: String = ""
    private var lastEventTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        if (instance == this) {
            instance = null
        }
    }

    override fun onInterrupt() {
        // Nothing needed on interrupt
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }

        val now = System.currentTimeMillis()
        val packageName = event.packageName?.toString() ?: return

        // Skip self package
        if (packageName == applicationContext.packageName) {
            return
        }

        // Ignore system UI overlays (notifications shade, status bar, volume dialog)
        if (packageName == "com.android.systemui" || packageName == "android") {
            return
        }

        // CRITICAL FIX: Ignore soft keyboards / input methods (Samsung Keyboard, Gboard, etc.)
        // When a keyboard opens for user text input, it should NOT count as leaving the locked app!
        if (AppLockPermissionHelper.isInputMethodPackage(applicationContext, packageName)) {
            return
        }

        // Debounce frequent content changes for same package
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (packageName == lastForegroundPackage && now - lastEventTime < 250L) {
                // If user is actively typing or touching in the unlocked app, keep touch fresh
                AppLockPreferences.touchTemporarilyUnlocked(packageName)
                return
            }
        }
        lastEventTime = now

        // If switched to a new distinct app (and NOT an input method / system UI), check previous app
        if (lastForegroundPackage.isNotEmpty() && lastForegroundPackage != packageName) {
            if (AppLockPreferences.isPackageLocked(applicationContext, lastForegroundPackage)) {
                // Only clear if lock timeout is set to 0 (immediate)
                val config = AppLockPreferences.getLockConfig(applicationContext)
                if (config.lockTimeoutSeconds <= 0) {
                    AppLockPreferences.clearTemporarilyUnlocked(lastForegroundPackage)
                }
            }
        }
        lastForegroundPackage = packageName

        // Check if current foreground app is marked as locked
        if (AppLockPreferences.isPackageLocked(applicationContext, packageName)) {
            if (!AppLockPreferences.isTemporarilyUnlocked(packageName)) {
                // INSTANT INTERCEPT: Show Lock Screen immediately
                LockActivity.start(applicationContext, packageName)
            } else {
                // Keep the active session alive while interacting
                AppLockPreferences.touchTemporarilyUnlocked(packageName)
            }
        }

    }
}
