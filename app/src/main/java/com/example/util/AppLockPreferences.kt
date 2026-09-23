package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.model.BackgroundTheme
import com.example.model.IntruderLog
import com.example.model.LockConfig
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

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
    private const val KEY_LOCK_MESSAGE = "lock_screen_message"
    private const val KEY_LOCK_ICON_SCALE = "lock_icon_scale"
    private const val KEY_LOCK_DIM = "lock_background_dim"
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
    private const val KEY_INTRUDER_SIREN = "intruder_siren_enabled"
    private const val KEY_PANIC_SHAKE = "panic_shake_enabled"
    private const val KEY_PANIC_SHAKE_STRENGTH = "panic_shake_strength"
    private const val KEY_DURESS_PIN = "duress_pin"
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
        return getLockedPackages(context).contains(packageName)
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
        val lockMessage = prefs.getString(KEY_LOCK_MESSAGE, "") ?: ""
        val iconScale = prefs.getFloat(KEY_LOCK_ICON_SCALE, 1f).coerceIn(0.75f, 1.35f)
        val dim = prefs.getFloat(KEY_LOCK_DIM, 0.82f).coerceIn(0.45f, 0.95f)
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
            lockScreenMessage = lockMessage,
            lockIconScale = iconScale,
            lockBackgroundDim = dim,
            lockTimeoutSeconds = timeoutSeconds,
            isStealthPattern = stealth,
            isFakeCrashEnabled = fakeCrash,
            isVibrationEnabled = vibration,
            isAppSelfProtectEnabled = appSelfProtect,
            isIntruderSelfieEnabled = intruderSelfie,
            intruderSelfieThreshold = intruderThreshold,
            isUninstallProtectionEnabled = uninstallProtection,
            isRandomPinKeypad = randomPin,
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
            .putString(KEY_LOCK_MESSAGE, config.lockScreenMessage)
            .putFloat(KEY_LOCK_ICON_SCALE, config.lockIconScale)
            .putFloat(KEY_LOCK_DIM, config.lockBackgroundDim)
            .putInt(KEY_TIMEOUT, config.lockTimeoutSeconds)
            .putBoolean(KEY_STEALTH_PATTERN, config.isStealthPattern)
            .putBoolean(KEY_FAKE_CRASH, config.isFakeCrashEnabled)
            .putBoolean(KEY_VIBRATION, config.isVibrationEnabled)
            .putBoolean(KEY_APP_SELF_PROTECT, config.isAppSelfProtectEnabled)
            .putBoolean(KEY_INTRUDER_SELFIE_ENABLED, config.isIntruderSelfieEnabled)
            .putInt(KEY_INTRUDER_SELFIE_THRESHOLD, config.intruderSelfieThreshold)
            .putBoolean(KEY_UNINSTALL_PROTECTION, config.isUninstallProtectionEnabled)
            .putBoolean(KEY_RANDOM_PIN_KEYPAD, config.isRandomPinKeypad)
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
                        photoPath = obj.optString("photoPath").takeIf { it.isNotEmpty() }
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
        photoPath: String? = null
    ) {
        val logs = getIntruderLogs(context).toMutableList()
        val newLog = IntruderLog(
            id = System.currentTimeMillis().toString(),
            packageName = packageName,
            appName = appName,
            timestamp = System.currentTimeMillis(),
            attemptCount = attempts,
            usedLockType = usedLockType,
            photoPath = photoPath
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
        getPrefs(context).edit().remove(KEY_INTRUDER_LOGS).apply()
    }
}
