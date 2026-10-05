package com.example.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 앱 안의 특정 화면만 잠그기 위한 저장소.
 *
 * 앱을 통째로 잠그는 것과 달리, 여기서는 "카톡을 열되 특정 채팅방만 못 들어가게"를 쓴다.
 * 화면 목록은 사용자가 실제로 본 화면만 담긴다(추측으로 지어내지 않는다).
 */
data class ScreenBlockEntry(
    val packageName: String,
    val appName: String,
    /** Activity 클래스명. 같은 Activity 안에서 여러 화면일 수 있어 제목이 구분자로 쓰인다. */
    val className: String,
    /** 화면 제목(앱 상단에 보이는 이름). 못 읽으면 빈 문자열. */
    val title: String,
    val lastSeenAt: Long,
    val blocked: Boolean
) {
    /** 화면 표시용 이름. 제목이 있으면 제목을 우선한다. */
    val displayName: String get() = title.ifBlank { className.substringAfterLast('.') }

    /** 제목을 못 읽은 화면은 매칭 신뢰도가 낮다(확인 필요). */
    val needsTitleMatch: Boolean get() = title.isBlank()
}

/**
 * 사용자가 본 화면 목록과 잠금 설정을 보관한다.
 */
object ScreenBlockStore {

    private const val PREFS = "screen_block"
    private const val KEY_ENTRIES = "entries"
    /** 같은 화면을 반복해서 기록하면 목록만 부풀므로 상한을 둔다. */
    private const val MAX_ENTRIES = 800
    private const val MAX_PER_APP = 120

    /** 특정 앱의 화면 중 막힌 것만 돌려준다. */
    fun blockedScreens(context: Context): List<ScreenBlockEntry> =
        load(context).filter { it.blocked }

    fun screensOf(context: Context, packageName: String): List<ScreenBlockEntry> =
        load(context)
            .filter { it.packageName == packageName }
            // 최근에 본 화면을 먼저, 제목이 있는 것을 먼저
            .sortedWith(compareByDescending<ScreenBlockEntry> { it.lastSeenAt }.thenBy { it.needsTitleMatch })

    /** 앱 목록 후보. 화면을 하나 이상 본 앱만 대상으로 삼는다. */
    fun appsWithScreens(context: Context): List<Pair<String, String>> =
        load(context)
            .groupBy { entry -> entry.packageName to entry.appName }
            .toList()
            // 막은 화면이 많은 앱을 먼저 보여준다(설정에서 찾기 쉽게)
            .sortedByDescending { pair -> pair.second.count { it.blocked } }
            .map { pair -> pair.first }

    fun allScreens(context: Context): List<ScreenBlockEntry> = load(context)

    fun isBlocked(context: Context, packageName: String, className: String, title: String): ScreenBlockEntry? =
        load(context).firstOrNull { entry ->
            if (!entry.blocked || entry.packageName != packageName) return@firstOrNull false
            // 1순위: 클래스명이 정확히 같으면 확실한 매칭
            if (entry.className == className) {
                // 같은 Activity 안에서 화면이 갈릴 때만 제목을 본다.
                if (entry.title.isBlank()) return@firstOrNull true
                if (title.isBlank()) return@firstOrNull true
                return@firstOrNull title.contains(entry.title, ignoreCase = true) ||
                    entry.title.contains(title, ignoreCase = true)
            }
            false
        }

    /** 접근성 서비스가 화면 전환을 관찰할 때 호출한다. */
    @Synchronized
    fun record(context: Context, packageName: String, appName: String, className: String, title: String) {
        if (className.isBlank()) return
        val prefs = prefs(context)
        val entries = load(context).toMutableList()

        // 같은 앱 + 같은 클래스 + 같은 제목은 하나만 두고 시각만 갱신한다.
        val existing = entries.indexOfFirst {
            it.packageName == packageName && it.className == className && it.title.equals(title, ignoreCase = true)
        }
        if (existing >= 0) {
            entries[existing] = entries[existing].copy(lastSeenAt = System.currentTimeMillis())
        } else {
            entries.add(
                ScreenBlockEntry(
                    packageName = packageName,
                    appName = appName,
                    className = className,
                    title = title,
                    lastSeenAt = System.currentTimeMillis(),
                    blocked = false
                )
            )
        }

        // 앱별로 상한을 두고, 제목 없이 잡힌 오래된 항목은 정리한다.
        val trimmed = entries
            .groupBy { it.packageName }
            .flatMap { (pkg, list) -> list.sortedByDescending { it.lastSeenAt }.take(MAX_PER_APP) }
            .sortedByDescending { it.lastSeenAt }
            .take(MAX_ENTRIES)

        save(prefs, trimmed)
    }

    fun setBlocked(context: Context, packageName: String, className: String, title: String, blocked: Boolean) {
        val entries = load(context).map { entry ->
            if (entry.packageName == packageName && entry.className == className && entry.title.equals(title, true)) {
                entry.copy(blocked = blocked)
            } else {
                entry
            }
        }
        save(prefs(context), entries)
    }

    fun setAllBlocked(context: Context, packageName: String, blocked: Boolean) {
        val entries = load(context).map { entry ->
            if (entry.packageName == packageName) entry.copy(blocked = blocked) else entry
        }
        save(prefs(context), entries)
    }

    fun remove(context: Context, packageName: String, className: String, title: String) {
        val entries = load(context).filterNot { entry ->
            entry.packageName == packageName && entry.className == className && entry.title.equals(title, true)
        }
        save(prefs(context), entries)
    }

    /** 특정 앱의 기록을 모두 지운다(앱을 더 이상 관찰하지 않겠다는 뜻). */
    fun forgetApp(context: Context, packageName: String) {
        save(prefs(context), load(context).filterNot { it.packageName == packageName })
    }

    fun blockedCount(context: Context): Int = load(context).count { it.blocked }

    private fun load(context: Context): List<ScreenBlockEntry> = runCatching {
        val raw = prefs(context).getString(KEY_ENTRIES, null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            runCatching {
                val obj = array.getJSONObject(index)
                ScreenBlockEntry(
                    packageName = obj.optString("pkg"),
                    appName = obj.optString("name"),
                    className = obj.optString("cls"),
                    title = obj.optString("title"),
                    lastSeenAt = obj.optLong("seen"),
                    blocked = obj.optBoolean("blocked")
                )
            }.getOrNull()
        }
    }.getOrDefault(emptyList())

    private fun save(prefs: SharedPreferences, entries: List<ScreenBlockEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("pkg", entry.packageName)
                    put("name", entry.appName)
                    put("cls", entry.className)
                    put("title", entry.title)
                    put("seen", entry.lastSeenAt)
                    put("blocked", entry.blocked)
                }
            )
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}