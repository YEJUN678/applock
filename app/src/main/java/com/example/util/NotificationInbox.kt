package com.example.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 잠긴 앱의 알림을 모아 두는 곳.
 *
 * 알림 하이라이트는 "다른 사람에게 안 보이게" 하는 기능이었다.
 * 그러면 소유자는 그 내용을 볼 수단이 필요한데, 알림을 dismiss 하면 사라져서
 * 결국 하이라이트를 꺼버리게 된다. 여기서 모아 두면 "가림"과 "보기"가 함께된다.
 *
 * 저장은 기기 안 SharedPreferences 에만 한다(외부 전송 없음).
 */
data class InboxNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val summary: String?,
    val receivedAt: Long,
    val read: Boolean
)

object NotificationInbox {

    private const val PREFS = "notification_inbox"
    private const val KEY_ITEMS = "items"
    /** 앱 용량과 정리 비용을 위해 상한을 둔다. */
    private const val MAX_ITEMS = 300

    fun all(context: Context): List<InboxNotification> = load(context)

    fun unreadCount(context: Context): Int = load(context).count { !it.read }

    /** 앱별 묶음(카톡 12건, 배민 3건 형태). */
    fun grouped(context: Context): List<Pair<String, List<InboxNotification>>> =
        load(context)
            .groupBy { it.packageName to it.appName }
            .toList()
            .sortedByDescending { pair -> pair.second.maxOfOrNull { it.receivedAt } ?: 0L }
            .map { pair -> pair.first.second to pair.second.sortedByDescending { it.receivedAt } }

    /**
     * 알림을 쌓는다.
     * 같은 앱의 알림이 짧은 시간 안에 반복되면 하나로 묶어 개수를 세어 준다
     * (연락처 알림처럼 같은 유형이 반복되는 경우 목록이 금방 넘치기 때문).
     */
    fun add(
        context: Context,
        packageName: String,
        appName: String,
        title: String,
        text: String,
        summary: String? = null
    ) {
        val now = System.currentTimeMillis()
        val items = load(context).toMutableList()
        val duplicate = items.firstOrNull {
            it.packageName == packageName && it.title == title && it.text == text && now - it.receivedAt < 60_000L
        }
        if (duplicate != null) {
            val idx = items.indexOf(duplicate)
            items[idx] = duplicate.copy(
                receivedAt = now,
                read = false,
                summary = summary ?: duplicate.summary
            )
        } else {
            items.add(
                0,
                InboxNotification(
                    id = "$packageName|$now",
                    packageName = packageName,
                    appName = appName,
                    title = title,
                    text = text,
                    summary = summary,
                    receivedAt = now,
                    read = false
                )
            )
        }
        save(context, items.take(MAX_ITEMS))
    }

    /** AI 요약이 나중에 붙어도 목록에서 바로 보여야 한다. */
    fun attachSummary(context: Context, packageName: String, title: String, text: String, summary: String) {
        val items = load(context).map { item ->
            if (item.packageName == packageName && item.title == title && item.text == text) {
                item.copy(summary = summary)
            } else item
        }
        save(context, items)
    }

    fun markAllRead(context: Context) {
        save(context, load(context).map { it.copy(read = true) })
    }

    fun remove(context: Context, id: String) {
        save(context, load(context).filterNot { it.id == id })
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_ITEMS).apply()
    }

    /** 보관 기간을 넘긴 것만 지운다. */
    fun pruneOlderThan(context: Context, days: Int): Int {
        if (days <= 0) return 0
        val cutoff = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
        val items = load(context)
        val kept = items.filter { it.receivedAt >= cutoff }
        if (kept.size == items.size) return 0
        save(context, kept)
        return items.size - kept.size
    }

    private fun load(context: Context): List<InboxNotification> = runCatching {
        val raw = prefs(context).getString(KEY_ITEMS, null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            runCatching {
                val obj = array.getJSONObject(index)
                InboxNotification(
                    id = obj.optString("id"),
                    packageName = obj.optString("pkg"),
                    appName = obj.optString("name"),
                    title = obj.optString("title"),
                    text = obj.optString("text"),
                    summary = if (obj.isNull("summary")) null else obj.optString("summary"),
                    receivedAt = obj.optLong("at"),
                    read = obj.optBoolean("read")
                )
            }.getOrNull()
        }
    }.getOrDefault(emptyList())

    private fun save(context: Context, items: List<InboxNotification>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("pkg", item.packageName)
                    put("name", item.appName)
                    put("title", item.title)
                    put("text", item.text)
                    put("summary", item.summary ?: JSONObject.NULL)
                    put("at", item.receivedAt)
                    put("read", item.read)
                }
            )
        }
        prefs(context).edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}