package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.model.BackgroundTheme
import com.example.model.IntruderLog
import com.example.model.LockConfig
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** 앱별 잠금 일정. 지정한 요일/시간 안에서만 그 앱을 잠근다. */
data class AppSchedule(
    val daysOfWeek: Set<Int>, // Calendar.DAY_OF_WEEK 값 (1=일요일)
    val startMinute: Int,      // 0..1439
    val endMinute: Int
)

/** 앱 잠금 해제 기록 한 건. 타임라인 화면에서 사용한다. */
data class AppSession(
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val method: String
)

object AppLockPreferences {
    private const val PREFS_NAME = "app_lock_prefs"
    private const val KEY_LOCKED_PACKAGES = "locked_packages"
    private const val KEY_LOCK_TYPE = "lock_type"
    private const val KEY_GRID_SIZE = "grid_size"
    private const val KEY_PATTERN = "saved_pattern"
    private const val KEY_PIN = "saved_pin"
    private const val KEY_PASSWORD = "saved_password"
    private const val KEY_CALCULATOR_CODE = "saved_calculator_code"
    private const val KEY_KNOCK_CODE = "saved_knock_code"
    private const val KEY_BIOMETRIC = "biometric_enabled"
    private const val KEY_THEME = "background_theme"
    private const val KEY_CUSTOM_LOCK_BACKGROUND_URI = "custom_lock_background_uri"
    private const val KEY_LOCK_BACKGROUND_URIS = "lock_background_uris"
    private const val KEY_LOCK_BG_AUTO_ROTATE = "lock_background_auto_rotate"
    private const val KEY_LOCK_BG_ROTATE_SECONDS = "lock_background_rotate_seconds"
    private const val KEY_LOCK_MESSAGE = "lock_screen_message"
    private const val KEY_LOCK_ICON_SCALE = "lock_icon_scale"
    private const val KEY_LOCK_DIM = "lock_background_dim"
    private const val KEY_LOCK_CLOCK_STYLE = "lock_clock_style"
    private const val KEY_LOCK_ACCENT = "lock_accent"
    private const val KEY_LOCK_PRESET = "lock_preset"
    private const val KEY_LOCK_BLUR = "lock_background_blur"
    private const val KEY_LOCK_PANEL_ALPHA = "lock_panel_alpha"
    private const val KEY_LOCK_CORNER = "lock_corner_radius"
    private const val KEY_LOCK_CLOCK_POSITION = "lock_clock_position"
    private const val KEY_LOCK_ICON_SHAPE = "lock_icon_shape"
    private const val KEY_LOCK_FONT_STYLE = "lock_font_style"
    private const val KEY_LOCK_CHARGING_STYLE = "lock_charging_style"
    private const val KEY_LOCK_QUICK_ACTIONS = "lock_quick_actions"
    private const val KEY_TIMEOUT = "lock_timeout_seconds"
    private const val KEY_STEALTH_PATTERN = "stealth_pattern"
    private const val KEY_FAKE_CRASH = "fake_crash"
    private const val KEY_FAKE_SCREEN_RULES = "fake_screen_rules"
    private const val KEY_VIBRATION = "vibration_enabled"
    private const val KEY_APP_SELF_PROTECT = "app_self_protect"
    private const val KEY_INTRUDER_SELFIE_ENABLED = "intruder_selfie_enabled"
    private const val KEY_INTRUDER_SELFIE_THRESHOLD = "intruder_selfie_threshold"
    private const val KEY_UNINSTALL_PROTECTION = "uninstall_protection_enabled"
    private const val KEY_RANDOM_PIN_KEYPAD = "random_pin_keypad_enabled"
    private const val KEY_AI_GUARD = "ai_guard_enabled"
    private const val KEY_AI_GUARD_SENSITIVITY = "ai_guard_sensitivity"
    private const val KEY_AI_GUARD_FALLBACK = "ai_guard_fallback"
    private const val KEY_AI_GUARD_VOICE_RECORDING = "ai_guard_voice_recording"
    private const val KEY_INTRUDER_SIREN = "intruder_siren_enabled"
    private const val KEY_PANIC_SHAKE = "panic_shake_enabled"
    private const val KEY_PANIC_SHAKE_STRENGTH = "panic_shake_strength"
    private const val KEY_DURESS_PIN = "duress_pin"
    private const val KEY_DECOY_PIN = "decoy_pin"
    private const val KEY_EMERGENCY_CONTACT = "emergency_contact"
    private const val KEY_NOTIFICATION_PRIVACY = "notification_privacy_enabled"
    private const val KEY_SCREEN_OFF_LOCK = "screen_off_lock_enabled"
    private const val KEY_SCHEDULE_LOCK = "schedule_lock_enabled"
    private const val KEY_SCHEDULE_START_HOUR = "schedule_start_hour"
    private const val KEY_SCHEDULE_START_MINUTE = "schedule_start_minute"
    private const val KEY_SCHEDULE_END_HOUR = "schedule_end_hour"
    private const val KEY_SCHEDULE_END_MINUTE = "schedule_end_minute"
    private const val KEY_DURESS_SESSION = "duress_session"
    private const val KEY_FACE_DOWN_PROTECTION = "face_down_protection"
    private const val KEY_INTRUDER_LOGS = "intruder_logs"
    private const val KEY_PRIVACY_AUTO_PACKAGES = "privacy_auto_packages"
    private const val KEY_EXCLUDED_PACKAGES = "excluded_packages"
    private const val KEY_SESSION_LOG = "session_log"
    private const val KEY_SESSION_RETENTION = "session_retention_days"
    private const val KEY_CLIPBOARD_LAST = "clipboard_last_copy"
    private const val KEY_CLIPBOARD_ENABLED = "clipboard_auto_clear"
    private const val KEY_CLIPBOARD_SECONDS = "clipboard_clear_seconds"
    private const val KEY_MAX_FAILED_ATTEMPTS = "max_failed_attempts"
    private const val KEY_LOCKOUT_MINUTES = "lockout_minutes"
    private const val KEY_FAILED_COUNTER = "failed_attempt_counter"
    private const val KEY_LOCKOUT_UNTIL = "lockout_until"
    private const val KEY_APP_SCHEDULES = "app_schedules"

    // In-memory cache for fast accessibility lookup
    @Volatile
    private var cachedLockedPackages: MutableSet<String>? = null

    // Temporarily unlocked packages (packageName -> timestampMillis)
    private val temporarilyUnlockedMap = mutableMapOf<String, Long>()
    // Cached lock timeout seconds
    @Volatile
    private var cachedTimeoutMs: Long = 30_000L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun getLockedPackages(context: Context): Set<String> {
        cachedLockedPackages?.let { return it }
        val set = getPrefs(context).getStringSet(KEY_LOCKED_PACKAGES, emptySet()) ?: emptySet()
        val mutable = set.toMutableSet()
        cachedLockedPackages = mutable
        return mutable
    }

    @Synchronized
    fun isPackageLocked(context: Context, packageName: String): Boolean {
        // 잠금 제외 앱은 어떤 경로로도 잠금 판정을 하지 않는다 (단일 관문점).
        if (isPackageExcluded(context, packageName)) return false
        if (!getLockedPackages(context).contains(packageName)) return false
        // 앱별 일정이 있으면 "지정 시간 안에서만" 잠금한다.
        val schedule = getAppSchedule(context, packageName)
        return schedule == null || isScheduleActive(schedule)
    }

    // --- 실패 횟수 제한 / 잠금 페널티 ---

    /** 0 이면 제한 없음. */
    fun getMaxFailedAttempts(context: Context): Int =
        getPrefs(context).getInt(KEY_MAX_FAILED_ATTEMPTS, 0).coerceIn(0, 20)

    fun setMaxFailedAttempts(context: Context, attempts: Int) {
        getPrefs(context).edit().putInt(KEY_MAX_FAILED_ATTEMPTS, attempts.coerceIn(0, 20)).apply()
        if (attempts == 0) clearFailedAttempts(context)
    }

    fun getLockoutMinutes(context: Context): Int =
        getPrefs(context).getInt(KEY_LOCKOUT_MINUTES, 5).coerceIn(1, 1440)

    fun setLockoutMinutes(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_LOCKOUT_MINUTES, minutes.coerceIn(1, 1440)).apply()
    }

    fun getFailedAttemptCounter(context: Context): Int =
        getPrefs(context).getInt(KEY_FAILED_COUNTER, 0).coerceAtLeast(0)

    /** 실패를 기록하고, 한도에 도달해 잠금이 걸렸으면 true 를 돌려준다. */
    @Synchronized
    fun recordFailedAttempt(context: Context): Boolean {
        val prefs = getPrefs(context)
        val limit = getMaxFailedAttempts(context)
        if (limit <= 0) return false
        val attempts = prefs.getInt(KEY_FAILED_COUNTER, 0) + 1
        if (attempts < limit) {
            prefs.edit().putInt(KEY_FAILED_COUNTER, attempts).apply()
            return false
        }
        prefs.edit()
            .putInt(KEY_FAILED_COUNTER, 0)
            .putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + getLockoutMinutes(context) * 60_000L)
            .apply()
        return true
    }

    /** 남은 잠금 시간. 0 이면 걸려 있지 않다. */
    fun getLockoutRemainingMs(context: Context): Long {
        val until = getPrefs(context).getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (until <= 0L) return 0L
        val remaining = until - System.currentTimeMillis()
        if (remaining <= 0L) {
            getPrefs(context).edit().remove(KEY_LOCKOUT_UNTIL).apply()
            return 0L
        }
        return remaining
    }

    /** 잠금 해제 성공 시 실패 기록을 초기화한다. */
    fun clearFailedAttempts(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_FAILED_COUNTER)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    // --- 앱별 잠금 일정 ---

    fun getAppSchedule(context: Context, packageName: String): AppSchedule? =
        getAppSchedules(context)[packageName]

    fun getAppSchedules(context: Context): Map<String, AppSchedule> {
        val raw = getPrefs(context).getString(KEY_APP_SCHEDULES, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, AppSchedule>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val item = obj.getJSONObject(key)
                val days = item.optJSONArray("days")?.let { array ->
                    (0 until array.length()).map { array.getInt(it) }.toSet()
                } ?: emptySet()
                result[key] = AppSchedule(days, item.optInt("start", 0), item.optInt("end", 0))
            }
            result
        }.getOrDefault(emptyMap())
    }

    /** schedule 이 null 이면 일정을 삭제한다. */
    fun setAppSchedule(context: Context, packageName: String, schedule: AppSchedule?) {
        val all = getAppSchedules(context).toMutableMap()
        if (schedule == null) all.remove(packageName) else all[packageName] = schedule
        if (all.isEmpty()) {
            getPrefs(context).edit().remove(KEY_APP_SCHEDULES).apply()
            return
        }
        val obj = JSONObject()
        all.forEach { (pkg, value) ->
            obj.put(pkg, JSONObject().apply {
                put("days", JSONArray(value.daysOfWeek.toList()))
                put("start", value.startMinute)
                put("end", value.endMinute)
            })
        }
        getPrefs(context).edit().putString(KEY_APP_SCHEDULES, obj.toString()).apply()
    }

    /** 자정을 넘는 구간도 처리한다. */
    private fun isScheduleActive(schedule: AppSchedule): Boolean = isScheduleActive(schedule, Calendar.getInstance())

    fun isScheduleActive(schedule: AppSchedule, now: Calendar): Boolean {
        if (schedule.daysOfWeek.isEmpty()) return false
        if (!schedule.daysOfWeek.contains(now.get(Calendar.DAY_OF_WEEK))) return false
        val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = schedule.startMinute.coerceIn(0, 1439)
        val end = schedule.endMinute.coerceIn(0, 1439)
        return if (start == end) true else if (start < end) minute in start until end else minute >= start || minute < end
    }

    /** 지정 요일이면서 지금 잠금 구간이면 true. */
    fun isAppScheduleLocked(context: Context, packageName: String, now: Calendar = Calendar.getInstance()): Boolean {
        val schedule = getAppSchedule(context, packageName) ?: return false
        return isScheduleActive(schedule, now)
    }

    // --- 잠금 제외(제외 목록) 앱 ---

    @Synchronized
    fun getExcludedPackages(context: Context): Set<String> {
        val set = getPrefs(context).getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()
        return set.toSet()
    }

    @Synchronized
    fun isPackageExcluded(context: Context, packageName: String): Boolean =
        getPrefs(context).getStringSet(KEY_EXCLUDED_PACKAGES, emptySet())?.contains(packageName) == true

    /** 제외 목록에 있는 앱은 잠금 목록에 남아 있어도 잠금하지 않는다. */
    @Synchronized
    fun setPackageExcluded(context: Context, packageName: String, excluded: Boolean) {
        val current = getExcludedPackages(context).toMutableSet()
        if (excluded) current.add(packageName) else current.remove(packageName)
        getPrefs(context).edit().putStringSet(KEY_EXCLUDED_PACKAGES, current).apply()
    }

    // --- 클립보드 자동 삭제 ---

    fun isClipboardAutoClearEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_CLIPBOARD_ENABLED, false)

    fun clipboardClearSeconds(context: Context): Int =
        getPrefs(context).getInt(KEY_CLIPBOARD_SECONDS, 60).coerceIn(15, 600)

    fun setClipboardAutoClear(context: Context, enabled: Boolean, seconds: Int = clipboardClearSeconds(context)) {
        getPrefs(context).edit()
            .putBoolean(KEY_CLIPBOARD_ENABLED, enabled)
            .putInt(KEY_CLIPBOARD_SECONDS, seconds.coerceIn(15, 600))
            .apply()
    }

    fun recordClipboardCopy(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        val existing = prefs.getString(KEY_CLIPBOARD_LAST, null)
        // Android 10+ 는 백그라운드 클립보드 읽기를 막아 값이 없을 수 있다.
        // 이 경우에도 시각만 남겨 나중에 지울 수 있게 한다.
        val label = try {
            context.getSystemService(Context.CLIPBOARD_SERVICE)
                ?.let { it as? android.content.ClipboardManager }
                ?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
                ?: existing?.let { JSONObject(it).optString("text") }
        } catch (_: Exception) {
            existing?.let { JSONObject(it).optString("text") }
        }
        val json = JSONObject().apply {
            put("package", packageName)
            put("at", System.currentTimeMillis())
            if (label != null) put("text", label.take(120))
        }
        prefs.edit().putString(KEY_CLIPBOARD_LAST, json.toString()).apply()
    }

    /** 마지막 복사 시각. 지울 기한이 지났으면 내용을 반환한다. */
    fun pendingClipboardClear(context: Context): Pair<Long, String>? {
        val raw = getPrefs(context).getString(KEY_CLIPBOARD_LAST, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            val at = obj.getLong("at")
            val deadline = at + clipboardClearSeconds(context) * 1000L
            if (System.currentTimeMillis() >= deadline) at to obj.optString("package") else null
        }.getOrNull()
    }

    fun clearClipboardRecord(context: Context) {
        getPrefs(context).edit().remove(KEY_CLIPBOARD_LAST).apply()
    }

    // --- 앱 실행 기록(타임라인) ---

    fun recordSession(context: Context, packageName: String, appName: String, method: String) {
        val prefs = getPrefs(context)
        val existing = prefs.getString(KEY_SESSION_LOG, null)
        val array = runCatching { JSONArray(existing ?: "[]") }.getOrDefault(JSONArray())
        val entry = JSONObject().apply {
            put("package", packageName)
            put("name", appName)
            put("at", System.currentTimeMillis())
            put("method", method)
        }
        array.put(entry)
        // 최근 100건만 유지 ( prefs 는 무제한으로 커지면 안 된다).
        while (array.length() > 100) array.remove(0)
        prefs.edit().putString(KEY_SESSION_LOG, array.toString()).apply()
    }

    fun getSessionLog(context: Context): List<AppSession> {
        val raw = getPrefs(context).getString(KEY_SESSION_LOG, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val list = mutableListOf<AppSession>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AppSession(
                        packageName = obj.optString("package"),
                        appName = obj.optString("name"),
                        timestamp = obj.optLong("at"),
                        method = obj.optString("method", "잠금 해제")
                    )
                )
            }
            list.reversed()
        }.getOrDefault(emptyList())
    }

    fun clearSessionLog(context: Context) {
        getPrefs(context).edit().remove(KEY_SESSION_LOG).apply()
    }

    /** 실행 기록 보관 기간(일). 0 이면 영구 보관. */
    fun getSessionRetentionDays(context: Context): Int =
        getPrefs(context).getInt(KEY_SESSION_RETENTION, 0)

    fun setSessionRetentionDays(context: Context, days: Int) {
        getPrefs(context).edit().putInt(KEY_SESSION_RETENTION, days.coerceAtLeast(0)).apply()
    }

    /** 보관 기간을 넘긴 실행 기록만 지운다(통계용 요약은 남는다). */
    fun pruneSessionLog(context: Context, keepDays: Int) {
        if (keepDays <= 0) {
            clearSessionLog(context)
            return
        }
        val cutoff = System.currentTimeMillis() - keepDays * 24L * 60L * 60L * 1000L
        val kept = getSessionLog(context).filter { it.timestamp >= cutoff }
        if (kept.size == getSessionLog(context).size) return
        val array = JSONArray()
        kept.reversed().forEach { session ->
            array.put(JSONObject().apply {
                put("package", session.packageName)
                put("name", session.appName)
                put("at", session.timestamp)
                put("method", session.method)
            })
        }
        getPrefs(context).edit().putString(KEY_SESSION_LOG, array.toString()).apply()
    }

    @Synchronized
    fun setPackageLocked(context: Context, packageName: String, isLocked: Boolean) {
        val current = getLockedPackages(context).toMutableSet()
        if (isLocked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
            temporarilyUnlockedMap.remove(packageName)
        }
        cachedLockedPackages = current
        getPrefs(context).edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
    }

    fun fakeScreenFor(context: Context, packageName: String): String? {
        val rules = getPrefs(context).getStringSet(KEY_FAKE_SCREEN_RULES, emptySet()) ?: emptySet()
        return rules.firstOrNull { it.substringBefore('|') == packageName }?.substringAfter('|')
    }

    fun setFakeScreenFor(context: Context, packageName: String, screen: String?) {
        val prefs = getPrefs(context)
        val rules = (prefs.getStringSet(KEY_FAKE_SCREEN_RULES, emptySet()) ?: emptySet())
            .filterNot { it.substringBefore('|') == packageName }.toMutableSet()
        if (screen != null) rules.add("$packageName|$screen")
        prefs.edit().putStringSet(KEY_FAKE_SCREEN_RULES, rules).apply()
    }

    @Synchronized
    fun lockAll(context: Context, packageNames: Collection<String>) {
        val current = getLockedPackages(context).toMutableSet()
        current.addAll(packageNames)
        cachedLockedPackages = current
        getPrefs(context).edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
    }

    @Synchronized
    fun unlockAll(context: Context) {
        val current = mutableSetOf<String>()
        cachedLockedPackages = current
        temporarilyUnlockedMap.clear()
        getPrefs(context).edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
    }

    @Synchronized
    fun lockPackagesBatch(context: Context, packageNames: Collection<String>) {
        val current = getLockedPackages(context).toMutableSet()
        current.addAll(packageNames)
        cachedLockedPackages = current
        getPrefs(context).edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
    }

    @Synchronized
    fun unlockPackagesBatch(context: Context, packageNames: Collection<String>) {
        val current = getLockedPackages(context).toMutableSet()
        current.removeAll(packageNames.toSet())
        packageNames.forEach { temporarilyUnlockedMap.remove(it) }
        cachedLockedPackages = current
        getPrefs(context).edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
    }

    @Synchronized
    fun isUninstallProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_UNINSTALL_PROTECTION, false)
    }

    @Synchronized
    fun setUninstallProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_UNINSTALL_PROTECTION, enabled).apply()
        // If enabled, automatically protect package installer
        if (enabled) {
            val installers = listOf(
                "com.google.android.packageinstaller",
                "com.android.packageinstaller",
                "com.samsung.android.packageinstaller"
            )
            lockPackagesBatch(context, installers)
        }
    }

    @Synchronized
    fun isTemporarilyUnlocked(packageName: String): Boolean {
        val timestamp = temporarilyUnlockedMap[packageName] ?: return false
        val now = System.currentTimeMillis()
        if (cachedTimeoutMs <= 0L) {
            // Immediate lock on leave: if currently in-memory and valid for 5 seconds minimum to allow activity transition
            if (now - timestamp < 5_000L) {
                return true
            }
            temporarilyUnlockedMap.remove(packageName)
            return false
        }
        if (now - timestamp < cachedTimeoutMs) {
            return true
        }
        temporarilyUnlockedMap.remove(packageName)
        return false
    }

    @Synchronized
    fun touchTemporarilyUnlocked(packageName: String) {
        if (temporarilyUnlockedMap.containsKey(packageName)) {
            temporarilyUnlockedMap[packageName] = System.currentTimeMillis()
        }
    }

    @Synchronized
    fun setTemporarilyUnlocked(packageName: String) {
        temporarilyUnlockedMap[packageName] = System.currentTimeMillis()
    }

    @Synchronized
    fun clearTemporarilyUnlocked(packageName: String) {
        temporarilyUnlockedMap.remove(packageName)
    }

    fun getDuressPin(context: Context): String = getPrefs(context).getString(KEY_DURESS_PIN, "") ?: ""
    fun setDuressPin(context: Context, pin: String) = getPrefs(context).edit().putString(KEY_DURESS_PIN, pin).apply()
    fun getDecoyPin(context: Context): String = getPrefs(context).getString(KEY_DECOY_PIN, "") ?: ""
    fun setDecoyPin(context: Context, pin: String) = getPrefs(context).edit().putString(KEY_DECOY_PIN, pin).apply()
    fun getEmergencyContact(context: Context): String = getPrefs(context).getString(KEY_EMERGENCY_CONTACT, "") ?: ""
    fun setEmergencyContact(context: Context, contact: String) = getPrefs(context).edit().putString(KEY_EMERGENCY_CONTACT, contact.trim()).apply()
    fun isDuressSession(context: Context): Boolean = getPrefs(context).getBoolean(KEY_DURESS_SESSION, false)
    fun setDuressSession(context: Context, active: Boolean) = getPrefs(context).edit().putBoolean(KEY_DURESS_SESSION, active).apply()
    fun isFaceDownProtectionEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_FACE_DOWN_PROTECTION, false)
    fun setFaceDownProtectionEnabled(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(KEY_FACE_DOWN_PROTECTION, enabled).apply()

    fun isPrivacyShadeAutoEnabled(context: Context, packageName: String): Boolean =
        (getPrefs(context).getStringSet(KEY_PRIVACY_AUTO_PACKAGES, emptySet()) ?: emptySet()).contains(packageName)

    fun setPrivacyShadeAutoEnabled(context: Context, packageName: String, enabled: Boolean) {
        val packages = (getPrefs(context).getStringSet(KEY_PRIVACY_AUTO_PACKAGES, emptySet()) ?: emptySet()).toMutableSet()
        if (enabled) packages.add(packageName) else packages.remove(packageName)
        getPrefs(context).edit().putStringSet(KEY_PRIVACY_AUTO_PACKAGES, packages).apply()
    }

    @Synchronized
    fun resetAllTemporaryUnlocks() {
        temporarilyUnlockedMap.clear()
    }

    /** Required after an encrypted backup restores SharedPreferences in the current process. */
    @Synchronized
    fun invalidateCachedState() {
        cachedLockedPackages = null
        temporarilyUnlockedMap.clear()
    }

    fun getLockConfig(context: Context): LockConfig {
        val prefs = getPrefs(context)
        val lockTypeName = prefs.getString(KEY_LOCK_TYPE, com.example.model.LockType.PATTERN.name)
        val lockType = try {
            com.example.model.LockType.valueOf(lockTypeName ?: com.example.model.LockType.PATTERN.name)
        } catch (_: Exception) {
            com.example.model.LockType.PATTERN
        }
        val gridSize = prefs.getInt(KEY_GRID_SIZE, 3)
        val patternStr = prefs.getString(KEY_PATTERN, "0,1,2,5,8") ?: "0,1,2,5,8"
        val pattern = try {
            patternStr.split(",").mapNotNull { it.trim().toIntOrNull() }
        } catch (_: Exception) {
            listOf(0, 1, 2, 5, 8)
        }
        val pin = prefs.getString(KEY_PIN, "1234") ?: "1234"
        val password = prefs.getString(KEY_PASSWORD, "admin1234") ?: "admin1234"
        val calcCode = prefs.getString(KEY_CALCULATOR_CODE, "1234") ?: "1234"
        val knockStr = prefs.getString(KEY_KNOCK_CODE, "1,2,3,4") ?: "1,2,3,4"
        val knockCode = try {
            knockStr.split(",").mapNotNull { it.trim().toIntOrNull() }.ifEmpty { listOf(1, 2, 3, 4) }
        } catch (_: Exception) {
            listOf(1, 2, 3, 4)
        }
        val biometric = prefs.getBoolean(KEY_BIOMETRIC, true)
        val themeName = prefs.getString(KEY_THEME, BackgroundTheme.CYBER_WALLPAPER.name)
        val theme = try {
            BackgroundTheme.valueOf(themeName ?: BackgroundTheme.CYBER_WALLPAPER.name)
        } catch (_: Exception) {
            BackgroundTheme.CYBER_WALLPAPER
        }
        val customBackgroundUri = prefs.getString(KEY_CUSTOM_LOCK_BACKGROUND_URI, null)
        val backgroundUris = prefs.getStringSet(KEY_LOCK_BACKGROUND_URIS, emptySet()).orEmpty().toList()
        val bgAutoRotate = prefs.getBoolean(KEY_LOCK_BG_AUTO_ROTATE, false)
        val bgRotateSeconds = prefs.getInt(KEY_LOCK_BG_ROTATE_SECONDS, 30).coerceIn(5, 600)
        val lockMessage = prefs.getString(KEY_LOCK_MESSAGE, "") ?: ""
        val iconScale = prefs.getFloat(KEY_LOCK_ICON_SCALE, 1f).coerceIn(0.75f, 1.35f)
        val dim = prefs.getFloat(KEY_LOCK_DIM, 0.82f).coerceIn(0.45f, 0.95f)
        val clockStyle = runCatching { com.example.model.LockClockStyle.valueOf(prefs.getString(KEY_LOCK_CLOCK_STYLE, com.example.model.LockClockStyle.LARGE.name)!!) }.getOrDefault(com.example.model.LockClockStyle.LARGE)
        val accent = runCatching { com.example.model.LockAccent.valueOf(prefs.getString(KEY_LOCK_ACCENT, com.example.model.LockAccent.CYAN.name)!!) }.getOrDefault(com.example.model.LockAccent.CYAN)
        val preset = runCatching { com.example.model.LockPreset.valueOf(prefs.getString(KEY_LOCK_PRESET, com.example.model.LockPreset.ONE_UI.name)!!) }.getOrDefault(com.example.model.LockPreset.ONE_UI)
        val blur = prefs.getFloat(KEY_LOCK_BLUR, preset.blur).coerceIn(0f, 1f)
        val panelAlpha = prefs.getFloat(KEY_LOCK_PANEL_ALPHA, preset.panelAlpha).coerceIn(0f, 0.5f)
        val corner = prefs.getFloat(KEY_LOCK_CORNER, preset.corner).coerceIn(8f, 40f)
        val clockPosition = runCatching { com.example.model.LockClockPosition.valueOf(prefs.getString(KEY_LOCK_CLOCK_POSITION, preset.clockPosition.name)!!) }.getOrDefault(com.example.model.LockClockPosition.TOP_CENTER)
        val iconShape = runCatching { com.example.model.LockIconShape.valueOf(prefs.getString(KEY_LOCK_ICON_SHAPE, preset.iconShape.name)!!) }.getOrDefault(com.example.model.LockIconShape.CIRCLE)
        val fontStyle = runCatching { com.example.model.LockFontStyle.valueOf(prefs.getString(KEY_LOCK_FONT_STYLE, com.example.model.LockFontStyle.SANS.name)!!) }.getOrDefault(com.example.model.LockFontStyle.SANS)
        val chargingStyle = runCatching { com.example.model.LockChargingStyle.valueOf(prefs.getString(KEY_LOCK_CHARGING_STYLE, com.example.model.LockChargingStyle.RING.name)!!) }.getOrDefault(com.example.model.LockChargingStyle.RING)
        val quickActions = prefs.getBoolean(KEY_LOCK_QUICK_ACTIONS, true)
        val timeoutSeconds = prefs.getInt(KEY_TIMEOUT, 30)
        cachedTimeoutMs = timeoutSeconds * 1000L
        val stealth = prefs.getBoolean(KEY_STEALTH_PATTERN, false)
        val fakeCrash = prefs.getBoolean(KEY_FAKE_CRASH, false)
        val vibration = prefs.getBoolean(KEY_VIBRATION, true)
        val appSelfProtect = prefs.getBoolean(KEY_APP_SELF_PROTECT, false)
        val intruderSelfie = prefs.getBoolean(KEY_INTRUDER_SELFIE_ENABLED, true)
        val intruderThreshold = prefs.getInt(KEY_INTRUDER_SELFIE_THRESHOLD, 1)
        val uninstallProtection = prefs.getBoolean(KEY_UNINSTALL_PROTECTION, false)
        val randomPin = prefs.getBoolean(KEY_RANDOM_PIN_KEYPAD, false)
        val aiGuard = prefs.getBoolean(KEY_AI_GUARD, false)
        val aiGuardSensitivity = prefs.getInt(KEY_AI_GUARD_SENSITIVITY, 2).coerceIn(1, 3)
        val aiGuardFallback = runCatching { com.example.model.AiGuardFallback.valueOf(prefs.getString(KEY_AI_GUARD_FALLBACK, com.example.model.AiGuardFallback.DEVICE_CREDENTIAL.name)!!) }.getOrDefault(com.example.model.AiGuardFallback.DEVICE_CREDENTIAL)
        val aiGuardVoiceRecording = prefs.getBoolean(KEY_AI_GUARD_VOICE_RECORDING, false)
        val siren = prefs.getBoolean(KEY_INTRUDER_SIREN, false)
        val panicShake = prefs.getBoolean(KEY_PANIC_SHAKE, false)
        val panicShakeStrength = prefs.getInt(KEY_PANIC_SHAKE_STRENGTH, 10).coerceIn(1, 10)
        val notificationPrivacy = prefs.getBoolean(KEY_NOTIFICATION_PRIVACY, false)
        val screenOffLock = prefs.getBoolean(KEY_SCREEN_OFF_LOCK, true)
        val scheduleLock = prefs.getBoolean(KEY_SCHEDULE_LOCK, false)
        val startHour = prefs.getInt(KEY_SCHEDULE_START_HOUR, 9)
        val startMinute = prefs.getInt(KEY_SCHEDULE_START_MINUTE, 0)
        val endHour = prefs.getInt(KEY_SCHEDULE_END_HOUR, 18)
        val endMinute = prefs.getInt(KEY_SCHEDULE_END_MINUTE, 0)

        return LockConfig(
            lockType = lockType,
            gridSize = gridSize,
            savedPattern = pattern.ifEmpty { listOf(0, 1, 2, 5, 8) },
            savedPin = pin.ifBlank { "1234" },
            savedPassword = password.ifBlank { "admin1234" },
            savedCalculatorCode = calcCode.ifBlank { "1234" },
            savedKnockCode = knockCode,
            biometricEnabled = biometric,
            backgroundTheme = theme,
            customLockBackgroundUri = customBackgroundUri,
            lockBackgroundUris = backgroundUris,
            lockBackgroundAutoRotate = bgAutoRotate,
            lockBackgroundRotateSeconds = bgRotateSeconds,
            lockScreenMessage = lockMessage,
            lockIconScale = iconScale,
            lockBackgroundDim = dim,
            lockClockStyle = clockStyle,
            lockAccent = accent,
            lockPreset = preset,
            lockBackgroundBlur = blur,
            lockPanelAlpha = panelAlpha,
            lockCornerRadius = corner,
            lockClockPosition = clockPosition,
            lockIconShape = iconShape,
            lockFontStyle = fontStyle,
            lockChargingStyle = chargingStyle,
            isLockQuickActionsEnabled = quickActions,
            lockTimeoutSeconds = timeoutSeconds,
            isStealthPattern = stealth,
            isFakeCrashEnabled = fakeCrash,
            isVibrationEnabled = vibration,
            isAppSelfProtectEnabled = appSelfProtect,
            isIntruderSelfieEnabled = intruderSelfie,
            intruderSelfieThreshold = intruderThreshold,
            isUninstallProtectionEnabled = uninstallProtection,
            isRandomPinKeypad = randomPin,
            isAiGuardEnabled = aiGuard,
            aiGuardSensitivity = aiGuardSensitivity,
            aiGuardFallback = aiGuardFallback,
            isAiGuardVoiceRecordingEnabled = aiGuardVoiceRecording,
            isIntruderSirenEnabled = siren,
            isPanicShakeEnabled = panicShake,
            panicShakeStrength = panicShakeStrength,
            isNotificationPrivacyEnabled = notificationPrivacy,
            isScreenOffLockEnabled = screenOffLock,
            isScheduleLockEnabled = scheduleLock,
            scheduleStartHour = startHour,
            scheduleStartMinute = startMinute,
            scheduleEndHour = endHour,
            scheduleEndMinute = endMinute
        )
    }

    fun saveLockConfig(context: Context, config: LockConfig) {
        val patternStr = config.savedPattern.joinToString(",")
        val knockStr = config.savedKnockCode.joinToString(",")
        cachedTimeoutMs = config.lockTimeoutSeconds * 1000L
        getPrefs(context).edit()
            .putString(KEY_LOCK_TYPE, config.lockType.name)
            .putInt(KEY_GRID_SIZE, config.gridSize)
            .putString(KEY_PATTERN, patternStr)
            .putString(KEY_PIN, config.savedPin)
            .putString(KEY_PASSWORD, config.savedPassword)
            .putString(KEY_CALCULATOR_CODE, config.savedCalculatorCode)
            .putString(KEY_KNOCK_CODE, knockStr)
            .putBoolean(KEY_BIOMETRIC, config.biometricEnabled)
            .putString(KEY_THEME, config.backgroundTheme.name)
            .putString(KEY_CUSTOM_LOCK_BACKGROUND_URI, config.customLockBackgroundUri)
            .putStringSet(KEY_LOCK_BACKGROUND_URIS, config.lockBackgroundUris.toSet())
            .putBoolean(KEY_LOCK_BG_AUTO_ROTATE, config.lockBackgroundAutoRotate)
            .putInt(KEY_LOCK_BG_ROTATE_SECONDS, config.lockBackgroundRotateSeconds.coerceIn(5, 600))
            .putString(KEY_LOCK_MESSAGE, config.lockScreenMessage)
            .putFloat(KEY_LOCK_ICON_SCALE, config.lockIconScale)
            .putFloat(KEY_LOCK_DIM, config.lockBackgroundDim)
            .putString(KEY_LOCK_CLOCK_STYLE, config.lockClockStyle.name)
            .putString(KEY_LOCK_ACCENT, config.lockAccent.name)
            .putString(KEY_LOCK_PRESET, config.lockPreset.name)
            .putFloat(KEY_LOCK_BLUR, config.lockBackgroundBlur)
            .putFloat(KEY_LOCK_PANEL_ALPHA, config.lockPanelAlpha)
            .putFloat(KEY_LOCK_CORNER, config.lockCornerRadius)
            .putString(KEY_LOCK_CLOCK_POSITION, config.lockClockPosition.name)
            .putString(KEY_LOCK_ICON_SHAPE, config.lockIconShape.name)
            .putString(KEY_LOCK_FONT_STYLE, config.lockFontStyle.name)
            .putString(KEY_LOCK_CHARGING_STYLE, config.lockChargingStyle.name)
            .putBoolean(KEY_LOCK_QUICK_ACTIONS, config.isLockQuickActionsEnabled)
            .putInt(KEY_TIMEOUT, config.lockTimeoutSeconds)
            .putBoolean(KEY_STEALTH_PATTERN, config.isStealthPattern)
            .putBoolean(KEY_FAKE_CRASH, config.isFakeCrashEnabled)
            .putBoolean(KEY_VIBRATION, config.isVibrationEnabled)
            .putBoolean(KEY_APP_SELF_PROTECT, config.isAppSelfProtectEnabled)
            .putBoolean(KEY_INTRUDER_SELFIE_ENABLED, config.isIntruderSelfieEnabled)
            .putInt(KEY_INTRUDER_SELFIE_THRESHOLD, config.intruderSelfieThreshold)
            .putBoolean(KEY_UNINSTALL_PROTECTION, config.isUninstallProtectionEnabled)
            .putBoolean(KEY_RANDOM_PIN_KEYPAD, config.isRandomPinKeypad)
            .putBoolean(KEY_AI_GUARD, config.isAiGuardEnabled)
            .putInt(KEY_AI_GUARD_SENSITIVITY, config.aiGuardSensitivity.coerceIn(1, 3))
            .putString(KEY_AI_GUARD_FALLBACK, config.aiGuardFallback.name)
            .putBoolean(KEY_AI_GUARD_VOICE_RECORDING, config.isAiGuardVoiceRecordingEnabled)
            .putBoolean(KEY_INTRUDER_SIREN, config.isIntruderSirenEnabled)
            .putBoolean(KEY_PANIC_SHAKE, config.isPanicShakeEnabled)
            .putInt(KEY_PANIC_SHAKE_STRENGTH, config.panicShakeStrength.coerceIn(1, 10))
            .putBoolean(KEY_NOTIFICATION_PRIVACY, config.isNotificationPrivacyEnabled)
            .putBoolean(KEY_SCREEN_OFF_LOCK, config.isScreenOffLockEnabled)
            .putBoolean(KEY_SCHEDULE_LOCK, config.isScheduleLockEnabled)
            .putInt(KEY_SCHEDULE_START_HOUR, config.scheduleStartHour)
            .putInt(KEY_SCHEDULE_START_MINUTE, config.scheduleStartMinute)
            .putInt(KEY_SCHEDULE_END_HOUR, config.scheduleEndHour)
            .putInt(KEY_SCHEDULE_END_MINUTE, config.scheduleEndMinute)
            .apply()

        // Also sync uninstall protection state
        if (config.isUninstallProtectionEnabled) {
            setUninstallProtectionEnabled(context, true)
        }
    }

    /** True during the configured protection window, including windows spanning midnight. */
    fun isScheduleLockActive(context: Context, now: Calendar = Calendar.getInstance()): Boolean {
        val config = getLockConfig(context)
        if (!config.isScheduleLockEnabled) return false
        val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = config.scheduleStartHour * 60 + config.scheduleStartMinute
        val end = config.scheduleEndHour * 60 + config.scheduleEndMinute
        return if (start == end) true else if (start < end) minute in start until end else minute >= start || minute < end
    }

    fun getIntruderLogs(context: Context): List<IntruderLog> {
        val jsonStr = getPrefs(context).getString(KEY_INTRUDER_LOGS, null) ?: return emptyList()
        val list = mutableListOf<IntruderLog>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    IntruderLog(
                        id = obj.getString("id"),
                        packageName = obj.getString("packageName"),
                        appName = obj.getString("appName"),
                        timestamp = obj.getLong("timestamp"),
                        attemptCount = obj.getInt("attemptCount"),
                        usedLockType = obj.optString("usedLockType", "패턴"),
                        photoPath = obj.optString("photoPath").takeIf { it.isNotEmpty() },
                        videoPath = obj.optString("videoPath").takeIf { it.isNotEmpty() },
                        audioPath = obj.optString("audioPath").takeIf { it.isNotEmpty() },
                        latitude = if (obj.has("lat")) obj.optDouble("lat") else null,
                        longitude = if (obj.has("lng")) obj.optDouble("lng") else null,
                        locationText = obj.optString("locText").takeIf { it.isNotEmpty() },
                        placeName = obj.optString("place").takeIf { it.isNotEmpty() }
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun recordIntruderAttempt(
        context: Context,
        packageName: String,
        appName: String,
        attempts: Int,
        usedLockType: String = "패턴",
        photoPath: String? = null,
        videoPath: String? = null,
        audioPath: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationText: String? = null,
        placeName: String? = null
    ) {
        val logs = getIntruderLogs(context).toMutableList()
        val newLog = IntruderLog(
            id = System.currentTimeMillis().toString(),
            packageName = packageName,
            appName = appName,
            timestamp = System.currentTimeMillis(),
            attemptCount = attempts,
            usedLockType = usedLockType,
            photoPath = photoPath,
            videoPath = videoPath,
            audioPath = audioPath,
            latitude = latitude,
            longitude = longitude,
            locationText = locationText,
            placeName = placeName
        )
        logs.add(0, newLog)
        // Keep last 40 logs
        val trimmed = logs.take(40)
        try {
            val jsonArray = JSONArray()
            for (log in trimmed) {
                val obj = JSONObject().apply {
                    put("id", log.id)
                    put("packageName", log.packageName)
                    put("appName", log.appName)
                    put("timestamp", log.timestamp)
                    put("attemptCount", log.attemptCount)
                    put("usedLockType", log.usedLockType)
                    log.photoPath?.let { put("photoPath", it) }
                    log.videoPath?.let { put("videoPath", it) }
                    log.audioPath?.let { put("audioPath", it) }
                    log.latitude?.let { put("lat", it) }
                    log.longitude?.let { put("lng", it) }
                    log.locationText?.let { put("locText", it) }
                    log.placeName?.let { put("place", it) }
                }
                jsonArray.put(obj)
            }
            getPrefs(context).edit().putString(KEY_INTRUDER_LOGS, jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    fun deleteIntruderLog(context: Context, id: String) {
        val logs = getIntruderLogs(context).toMutableList()
        val target = logs.find { it.id == id }
        target?.photoPath?.let { path ->
            try {
                val file = java.io.File(path)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        target?.videoPath?.let { path -> runCatching { java.io.File(path).delete() } }
        target?.audioPath?.let { path -> runCatching { java.io.File(path).delete() } }
        logs.removeAll { it.id == id }
        try {
            val jsonArray = JSONArray()
            for (log in logs) {
                val obj = JSONObject().apply {
                    put("id", log.id)
                    put("packageName", log.packageName)
                    put("appName", log.appName)
                    put("timestamp", log.timestamp)
                    put("attemptCount", log.attemptCount)
                    put("usedLockType", log.usedLockType)
                    log.photoPath?.let { put("photoPath", it) }
                    log.videoPath?.let { put("videoPath", it) }
                    log.audioPath?.let { put("audioPath", it) }
                    log.latitude?.let { put("lat", it) }
                    log.longitude?.let { put("lng", it) }
                    log.locationText?.let { put("locText", it) }
                    log.placeName?.let { put("place", it) }
                }
                jsonArray.put(obj)
            }
            getPrefs(context).edit().putString(KEY_INTRUDER_LOGS, jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    fun clearIntruderLogs(context: Context) {
        // Delete all photo files
        try {
            val dir = java.io.File(context.filesDir, "intruder_photos")
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
        try {
            val dir = java.io.File(context.filesDir, "intruder_videos")
            if (dir.exists()) dir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
        try {
            val dir = java.io.File(context.filesDir, "ai_guard_audio")
            if (dir.exists()) dir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
        getPrefs(context).edit().remove(KEY_INTRUDER_LOGS).apply()
    }
}
