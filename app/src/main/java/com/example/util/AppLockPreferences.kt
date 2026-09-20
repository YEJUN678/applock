package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.model.BackgroundTheme
import com.example.model.IntruderLog
import com.example.model.LockConfig
import org.json.JSONArray
import org.json.JSONObject

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
    private const val KEY_TIMEOUT = "lock_timeout_seconds"
    private const val KEY_STEALTH_PATTERN = "stealth_pattern"
    private const val KEY_FAKE_CRASH = "fake_crash"
    private const val KEY_VIBRATION = "vibration_enabled"
    private const val KEY_APP_SELF_PROTECT = "app_self_protect"
    private const val KEY_INTRUDER_SELFIE_ENABLED = "intruder_selfie_enabled"
    private const val KEY_INTRUDER_SELFIE_THRESHOLD = "intruder_selfie_threshold"
    private const val KEY_UNINSTALL_PROTECTION = "uninstall_protection_enabled"
    private const val KEY_RANDOM_PIN_KEYPAD = "random_pin_keypad_enabled"
    private const val KEY_INTRUDER_SIREN = "intruder_siren_enabled"
    private const val KEY_PANIC_SHAKE = "panic_shake_enabled"
    private const val KEY_INTRUDER_LOGS = "intruder_logs"

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

    @Synchronized
    fun resetAllTemporaryUnlocks() {
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
            isPanicShakeEnabled = panicShake
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
            .apply()

        // Also sync uninstall protection state
        if (config.isUninstallProtectionEnabled) {
            setUninstallProtectionEnabled(context, true)
        }
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
