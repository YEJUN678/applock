package com.example.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 잠금 해제 후 바로 복귀할 화면 정보.
 *
 * 사용자가 잠금을 풀고 들어갔다가 잠그면, 예전에는 처음부터 다시 시작해야 했다.
 * 마지막으로 본 화면(앱 + Activity + 제목)을 기억해 두면
 * 잠금 화면에서 "방금 하던 화면으로"를 누를 수 있다.
 */
data class ResumePoint(
    val packageName: String,
    val appName: String,
    val className: String,
    val title: String,
    val savedAt: Long
) {
    val displayName: String get() = title.ifBlank { className.substringAfterLast('.') }
}

object ResumePointStore {

    private const val PREFS = "resume_point"
    private const val KEY_POINT = "point"

    /** 앱별로 하나씩 기억한다(여러 앱을 오갈 수 있으므로). */
    fun save(context: Context, packageName: String, appName: String, className: String, title: String) {
        if (packageName.isBlank() || className.isBlank()) return
        prefs(context).edit()
            .putString(KEY_POINT, toJson(packageName, appName, className, title))
            .apply()
    }

    fun latest(context: Context): ResumePoint? {
        val raw = prefs(context).getString(KEY_POINT, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            ResumePoint(
                packageName = obj.optString("pkg"),
                appName = obj.optString("name"),
                className = obj.optString("cls"),
                title = obj.optString("title"),
                savedAt = obj.optLong("at")
            )
        }.getOrNull()
    }

    /** 너무 오래된 지점은 무효로 본다(앱 구조가 바뀌었을 수 있다). */
    fun latestValid(context: Context, maxAgeMs: Long = 6L * 60 * 60 * 1000): ResumePoint? {
        val point = latest(context) ?: return null
        return if (System.currentTimeMillis() - point.savedAt <= maxAgeMs) point else null
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_POINT).apply()
    }

    private fun toJson(packageName: String, appName: String, className: String, title: String) = JSONObject().apply {
        put("pkg", packageName)
        put("name", appName)
        put("cls", className)
        put("title", title)
        put("at", System.currentTimeMillis())
    }.toString()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/**
 * 앱 잠금 사유 메모.
 *
 * "왜 잠갔는지"를 남겨 두면 통계를 문장으로 말할 수 있고,
 * AI 리포트에도 근거가 붙는다.
 */
object LockReasonStore {

    private const val PREFS = "lock_reason"
    private const val KEY_PREFIX = "reason_"

    fun reasonOf(context: Context, packageName: String): String =
        prefs(context).getString(KEY_PREFIX + packageName, "").orEmpty()

    fun setReason(context: Context, packageName: String, reason: String) {
        prefs(context).edit().putString(KEY_PREFIX + packageName, reason.trim()).apply()
    }

    /** 메모가 달린 잠긴 앱 목록 (사유 → 앱 목록 통계에 쓴다). */
    fun reasonsWithApps(context: Context): List<Pair<String, String>> {
        val locked = AppLockPreferences.getLockedPackages(context)
        return locked.map { it to reasonOf(context, it) }.filter { it.second.isNotBlank() }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}