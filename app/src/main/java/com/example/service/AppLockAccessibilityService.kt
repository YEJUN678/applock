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
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            return
        }

        // CRITICAL FOR POP-UP VIEW & MULTI-WINDOW:
        // Inspect all interactive windows currently displayed (covers Samsung Pop-up View, Freeform, Split Screen)
        try {
            val currentWindows = windows
            if (!currentWindows.isNullOrEmpty()) {
                for (win in currentWindows) {
                    val root = win.root ?: continue
                    val winPkg = root.packageName?.toString() ?: continue
                    if (winPkg == applicationContext.packageName) continue
                    if (winPkg == "com.android.systemui" || winPkg == "android") continue
                    if (AppLockPermissionHelper.isInputMethodPackage(applicationContext, winPkg)) continue

                    if (AppLockPreferences.isPackageLocked(applicationContext, winPkg)) {
                        if (!AppLockPreferences.isTemporarilyUnlocked(winPkg)) {
                            LockActivity.start(applicationContext, winPkg)
                            return
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val now = System.currentTimeMillis()
        val packageName = event.packageName?.toString() ?: return

        // Track self package transitions (MainActivity handles its own lock screen upon resume)
        if (packageName == applicationContext.packageName) {
            lastForegroundPackage = packageName
            return
        }

        // Uninstall Protection: Intercept package installer and app uninstall dialogs
        val isUninstallPackage = packageName == "com.google.android.packageinstaller" ||
                packageName == "com.android.packageinstaller" ||
                packageName == "com.samsung.android.packageinstaller" ||
                packageName == "com.miui.packageinstaller" ||
                packageName == "com.coloros.packageinstaller" ||
                packageName == "com.vivo.packageinstaller" ||
                packageName.endsWith(".packageinstaller")

        val isUninstallAttempt = isUninstallPackage || (packageName == "com.android.settings" && isUninstallScreen(event))
        if (AppLockPreferences.isUninstallProtectionEnabled(applicationContext) && isUninstallAttempt) {
            if (!AppLockPreferences.isTemporarilyUnlocked(packageName)) {
                LockActivity.start(applicationContext, packageName)
                return
            }
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
            val isPrevLocked = AppLockPreferences.isPackageLocked(applicationContext, lastForegroundPackage) ||
                    (lastForegroundPackage == applicationContext.packageName && AppLockPreferences.getLockConfig(applicationContext).isAppSelfProtectEnabled)
            if (isPrevLocked) {
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
            if (AppLockPreferences.isScheduleLockActive(applicationContext) || !AppLockPreferences.isTemporarilyUnlocked(packageName)) {
                // INSTANT INTERCEPT: Show Lock Screen immediately
                LockActivity.start(applicationContext, packageName)
            } else {
                // Keep the active session alive while interacting
                AppLockPreferences.touchTemporarilyUnlocked(packageName)
            }
        }
    }

    private fun isUninstallScreen(event: AccessibilityEvent): Boolean {
        val className = event.className?.toString() ?: ""
        if (className.contains("Uninstall", ignoreCase = true) ||
            className.contains("Delete", ignoreCase = true) ||
            className.contains("PackageInstaller", ignoreCase = true) ||
            className.contains("InstalledAppDetails", ignoreCase = true)) {
            return true
        }
        val text = event.text.joinToString(" ")
        return text.contains("삭제") || text.contains("제거") || text.contains("Uninstall") || text.contains("Delete")
    }
}
