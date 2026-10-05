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
import com.example.util.ScreenBlockStore

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
    private var lastScreenBlockAt: Long = 0L
    private var lastRecordedKey: String = ""
    private var lastRecordedAt: Long = 0L

    

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

        // 화면 기록은 다른 판단보다 먼저 한다.
        // 아래 데바운스/잠금 판정에 걸려 반환되면 기록이 사라지므로, 위치가 늦으면 아무것도 안 쌓인다.
        recordScreenIfNeeded(packageName, event)

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
            return
        }

        // 막힌 화면 진입 여부 확인.
        // 앱 전체를 잠그지 않아도 특정 화면에서만 잠금 화면이 뜬다.
        val className = event.className?.toString().orEmpty()
        if (className.isNotBlank()) {
            val title = readWindowTitle()
            val hit = ScreenBlockStore.isBlocked(applicationContext, packageName, className, title)
            if (hit != null && now - lastScreenBlockAt > 1500L) {
                lastScreenBlockAt = now
                recordScreenBlockAttempt(packageName, hit.displayName)
                LockActivity.start(applicationContext, packageName)
            }
        }
    }

    /**
     * 사용자가 본 화면을 기록한다.
     *
     * 앱 전체 잠금이 꺼져 있고 시스템UI·키보드가 아닌 앱만 대상으로 한다.
     * 같은 화면을 계속 쌓지 않게 약간의 간격을 두지만, 목록이 비면 쓸모가 없으므로 간격은 짧게 둔다.
     */
    private fun recordScreenIfNeeded(packageName: String, event: AccessibilityEvent) {
        if (AppLockPreferences.isPackageLocked(applicationContext, packageName)) return
        if (packageName == applicationContext.packageName) return
        if (packageName == "com.android.systemui" || packageName == "android") return
        if (AppLockPermissionHelper.isInputMethodPackage(applicationContext, packageName)) return

        val className = event.className?.toString().orEmpty()
        // 이벤트에 클래스명이 없으면 현재 창의 루트에서 이름을 얻어 온다.
        val resolved = className.ifBlank { runCatching { windows?.firstOrNull()?.root?.className?.toString() }.getOrNull().orEmpty() }
        if (resolved.isBlank()) return

        // 컨테이너/데코레이션 뷰는 화면이 아니라 레이아웃이다. 기록해도 목록만 더러워진다.
        if (resolved.endsWith(".DecorView") || resolved.endsWith("ContentFrameLayout") ||
            resolved.endsWith("ViewPager") || resolved.endsWith("RecyclerView") ||
            resolved.endsWith("CoordinatorLayout") || resolved.endsWith("FrameLayout") ||
            resolved.endsWith("LinearLayout") || resolved.endsWith("RelativeLayout")
        ) return

        val now = System.currentTimeMillis()
        val key = "$packageName|$resolved"
        if (key == lastRecordedKey && now - lastRecordedAt < 400L) return
        lastRecordedKey = key
        lastRecordedAt = now

        val appLabel = runCatching {
            val pm = applicationContext.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)

        ScreenBlockStore.record(applicationContext, packageName, appLabel, resolved, readWindowTitle())
    }

    /** 막힌 화면 진입 시도를 잠금 화면에 전달한다(증거 기록용). */
    private fun recordScreenBlockAttempt(packageName: String, screenName: String) {
        val prefs = getSharedPreferences("screen_block_attempt", MODE_PRIVATE)
        prefs.edit()
            .putString("pkg", packageName)
            .putString("screen", screenName)
            .putLong("at", System.currentTimeMillis())
            .apply()
    }

    /**
     * 화면에 보이는 텍스트와 그 세로 위치를 모은다.
     *
     * AccessibilityNodeInfo 에는 트리 순회 API가 없어 재귀로 직접 내려간다.
     * 접근성 노드 수는 화면마다 수천 개라서 깊이를 제한하고 개수도 자른다(프레임 드롭 방지).
     */
    private fun collectVisibleTexts(node: android.view.accessibility.AccessibilityNodeInfo?): List<Pair<String, Int>> {
        if (node == null || collected >= maxTextNodes) return emptyList()
        collected = 0
        val result = mutableListOf<Pair<String, Int>>()
        val queue = ArrayDeque<Pair<android.view.accessibility.AccessibilityNodeInfo, Int>>()
        queue.add(node to 0)

        while (queue.isNotEmpty() && collected < maxTextNodes) {
            val (current, depth) = queue.removeFirst()
            collected++
            if (depth > maxTextDepth) continue

            val text = current.text?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                val rect = android.graphics.Rect()
                current.getBoundsInScreen(rect)
                result.add(text to rect.top)
            }
            for (i in 0 until current.childCount) {
                queue.add(current.getChild(i) to depth + 1)
            }
        }
        return result
    }

    private var collected: Int = 0

    /** 텍스트 수집은 성능을 위해 상한을 둔다. */
    private val maxTextNodes = 120
    private val maxTextDepth = 12

    /**
     * 화면 제목을 읽는다(채팅방 이름 같은 구분자).
     *
     * 표준 id(android:id/title 등)를 먼저 보고, 없으면 상단에 보이는 텍스트를 쓴다.
     * 카카오톡처럼 자체 레이아웃을 쓰는 앱에서는 id가 없기 때문에 두 번째 방법이 실질적으로 동작한다.
     */
    private fun readWindowTitle(): String {
        return try {
            val root = windows?.firstOrNull { it.root != null && it.isActive }?.root
                ?: windows?.firstNotNullOfOrNull { it.root }
                ?: return ""

            // 1) 표준 액션바 제목
            val byId = root.findAccessibilityNodeInfosByViewId("android:id/title")
                ?.firstNotNullOfOrNull { it.text?.toString()?.takeIf(String::isNotBlank) }
                ?: root.findAccessibilityNodeInfosByViewId("android:id/action_bar_title")
                    ?.firstNotNullOfOrNull { it.text?.toString()?.takeIf(String::isNotBlank) }
            if (byId != null) return byId.take(60)

            // 2) 상단 영역에 보이는 텍스트를 제목으로 쓴다.
            val bounds = android.graphics.Rect()
            root.getBoundsInScreen(bounds)
            val topLimit = bounds.top + (bounds.height() * 0.22f).toInt()
            collectVisibleTexts(root)
                .asSequence()
                .mapNotNull { (text, top) ->
                    if (text.length < 2 || text.length > 40) null else if (top <= topLimit) text else null
                }
                .firstOrNull()
                ?.take(60)
                .orEmpty()
        } catch (_: Exception) {
            ""
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
