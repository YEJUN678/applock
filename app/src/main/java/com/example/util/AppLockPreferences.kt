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
    private const val KEY_BIOMETRIC = "biometric_enabled"
    private const val KEY_THEME = "background_theme"
    private const val KEY_TIMEOUT = "lock_timeout_seconds"
    private const val KEY_STEALTH_PATTERN = "stealth_pattern"
    private const val KEY_FAKE_CRASH = "fake_crash"
    private const val KEY_VIBRATION = "vibration_enabled"
    private const val KEY_APP_SELF_PROTECT = "app_self_protect"
    private const val KEY_INTRUDER_SELFIE_ENABLED = "intruder_selfie_enabled"
    private const val KEY_INTRUDER_SELFIE_THRESHOLD = "intruder_selfie_threshold"
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

        return LockConfig(
            lockType = lockType,
            gridSize = gridSize,
            savedPattern = pattern.ifEmpty { listOf(0, 1, 2, 5, 8) },
            savedPin = pin.ifBlank { "1234" },
            biometricEnabled = biometric,
            backgroundTheme = theme,
            lockTimeoutSeconds = timeoutSeconds,
            isStealthPattern = stealth,
            isFakeCrashEnabled = fakeCrash,
            isVibrationEnabled = vibration,
            isAppSelfProtectEnabled = appSelfProtect,
            isIntruderSelfieEnabled = intruderSelfie,
            intruderSelfieThreshold = intruderThreshold
        )
    }

    fun saveLockConfig(context: Context, config: LockConfig) {
        val patternStr = config.savedPattern.joinToString(",")
        cachedTimeoutMs = config.lockTimeoutSeconds * 1000L
        getPrefs(context).edit()
            .putString(KEY_LOCK_TYPE, config.lockType.name)
            .putInt(KEY_GRID_SIZE, config.gridSize)
            .putString(KEY_PATTERN, patternStr)
            .putString(KEY_PIN, config.savedPin)
            .putBoolean(KEY_BIOMETRIC, config.biometricEnabled)
            .putString(KEY_THEME, config.backgroundTheme.name)
            .putInt(KEY_TIMEOUT, config.lockTimeoutSeconds)
            .putBoolean(KEY_STEALTH_PATTERN, config.isStealthPattern)
            .putBoolean(KEY_FAKE_CRASH, config.isFakeCrashEnabled)
            .putBoolean(KEY_VIBRATION, config.isVibrationEnabled)
            .putBoolean(KEY_APP_SELF_PROTECT, config.isAppSelfProtectEnabled)
            .putBoolean(KEY_INTRUDER_SELFIE_ENABLED, config.isIntruderSelfieEnabled)
            .putInt(KEY_INTRUDER_SELFIE_THRESHOLD, config.intruderSelfieThreshold)
            .apply()
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
