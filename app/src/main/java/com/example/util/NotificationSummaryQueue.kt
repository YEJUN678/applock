package com.example.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 알림 요약의 배치 처리.
 *
 * 기존에는 알림 하나마다 Gemini 를 한 번씩 호출했다. 카톡 알림이 20개면
 * 20회 호출이라 무료 티어 한도를 금방 태웠다.
 *
 * 여기서는 세 가지를 같이 처리한다.
 *  1. 배치 — 5분 동안 모아서 한 번만 호출한다
 *  2. 캐시 — 같은 앱의 비슷한 알림은 직전 요약을 재사용한다
 *  3. 상한 — 하루 호출 횟수를 제한한다(한도를 다 쓰면 요약 없이 동작)
 *
 * AI 키가 없거나 요약 기능이 꺼져 있으면 아무 일도 하지 않는다.
 */
object NotificationSummaryQueue {

    /** 모아 두는 시간. 너무 길면 사용자가 요약을 보기 전에 알림이 사라진다. */
    private const val BATCH_DELAY_MS = 5L * 60 * 1000
    /** 이 시간 안에 같은 앱 알림이 또 오면 요약을 재사용한다. */
    private const val CACHE_WINDOW_MS = 30L * 60 * 1000
    /** 하루 최대 호출 횟수. */
    private const val DAILY_LIMIT = 60
    /** 한 번에 보낼 최대 알림 개수. */
    private const val MAX_BATCH = 20

    private data class Pending(
        val packageName: String,
        val appName: String,
        val title: String,
        val text: String,
        val queuedAt: Long
    )

    private val pending = ArrayDeque<Pending>()
    private val handler = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)

    /** 요약을 실��로 호출했는지 (하이라이트가 알림을 갱신할 때 쓴다). */
    fun queue(context: Context, packageName: String, appName: String, title: String, text: String) {
        if (!NotificationSummaryService.isAvailable(context)) return
        if (title.isBlank() && text.isBlank()) return

        val appContext = context.applicationContext
        synchronized(pending) {
            pending.addLast(Pending(packageName, appName, title, text, System.currentTimeMillis()))
            while (pending.size > MAX_BATCH) pending.removeFirst()
        }
        scheduleFlush(appContext)
    }

    private fun scheduleFlush(context: Context) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            flush(context)
        }, BATCH_DELAY_MS)
    }

    private fun flush(context: Context) {
        val batch = synchronized(pending) {
            val taken = pending.toList()
            pending.clear()
            taken
        }
        if (batch.isEmpty()) return

        // 같은 앱의 알림이 이미 요약돼 있으면 그걸 재사용한다(호출 없이).
        val uncached = mutableListOf<Pending>()
        batch.forEach { item ->
            val cached = SummaryCache.get(context, item.packageName, item.title, item.text)
            if (cached != null) {
                NotificationInbox.attachSummary(context, item.packageName, item.title, item.text, cached)
                // 하이라이트 알림에도 붙여 준다.
                runCatching { attachToPosted(context, item, cached) }
            } else {
                uncached.add(item)
            }
        }
        if (uncached.isEmpty()) return

        if (!CallBudget.tryConsume(context)) {
            // 한도 소진: 요약 없이도 앱은 정상 동작한다.
            return
        }
        if (running.getAndSet(true)) return
        running.set(false) // 다음 배치를 위해 즉시 해제 후 백그라운드에서 처리

        Thread {
            val grouped = uncached.groupBy { it.packageName }
            grouped.forEach { (pkg, items) ->
                val appName = items.first().appName
                val content = buildString {
                    items.forEach { item ->
                        appendLine("- 제목: ${item.title.ifBlank { "(없음)" }}")
                        appendLine("  내용: ${item.text.ifBlank { "(없음)" }}")
                    }
                }
                val prompt = buildString {
                    appendLine("아래는 사용자가 잠긴 앱에서 도착한 알림 ${items.size}건이다.")
                    appendLine("발신 앱: $appName")
                    appendLine(content)
                    appendLine()
                    append("각 알림을 한국어 한 문장(30자 이내)으로 요약하라. 번호를 붙여라. 분류나 평가 없이 사실만 말하라.")
                }
                val summary = GeminiClient.ask(context, prompt)
                    .let { if (it is GeminiClient.Result.Success) it.text else null }
                    ?: return@forEach

                // 여러 건을 한 번에 받았으므로 줄 단위로 나눠 각각에 붙인다.
                val lines = summary.lines().filter { it.isNotBlank() }
                items.forEachIndexed { index, item ->
                    val one = lines.getOrNull(index)?.replace(Regex("^\\s*\\d+[.)]\\s*"), "")?.trim()
                        ?: lines.firstOrNull()?.trim()
                        ?: return@forEach
                    NotificationInbox.attachSummary(context, pkg, item.title, item.text, one)
                    SummaryCache.put(context, pkg, item.title, item.text, one)
                    runCatching { attachToPosted(context, item, one) }
                }
            }
        }.start()
    }

    /** 이미 떠 있는 하이라이트 알림을 요약과 함께 다시 올린다. */
    private fun attachToPosted(context: Context, item: Pending, summary: String) {
        NotificationHighlighter.updatePostedSummary(
            context = context,
            packageName = item.packageName,
            title = item.title,
            text = item.text,
            summary = summary
        )
    }

    /** 앱을 종료해도 배치가 사라지지 않도록 즉시 비우기용. */
    fun flushNow(context: Context) {
        handler.removeCallbacksAndMessages(null)
        flush(context)
    }

    /** 남은 대기 개수(설정 화면 표시용). */
    fun pendingCount(): Int = synchronized(pending) { pending.size }

    /** 오늘 남은 호출 횟수(설정 화면 표시용). */
    fun remainingCalls(context: Context): Int =
        (DAILY_LIMIT - CallBudget.usedToday(context)).coerceAtLeast(0)
}

/**
 * 같은 앱 알림의 요약 캐시.
 *
 * 반복되는 알림(연락처 저장 알림, 자동回复 같은 것)은 요약을 재사용한다.
 */
private object SummaryCache {
    /** 캐시 재사용 시간. 같은 앱 알림이 이 안에 또 오면 요약을 다시 만들지 않는다. */
    private const val CACHE_WINDOW_MS = 30L * 60 * 1000

    fun get(context: Context, packageName: String, title: String, text: String): String? = runCatching {
        val prefs = context.getSharedPreferences("summary_cache", Context.MODE_PRIVATE)
        val key = "$packageName|${text.ifBlank { title }}"
        val savedAt = prefs.getLong(key, 0L)
        if (savedAt == 0L) return@runCatching null
        if (System.currentTimeMillis() - savedAt > CACHE_WINDOW_MS) {
            prefs.edit().remove(key).apply()
            return@runCatching null
        }
        prefs.getString(key, null)
    }.getOrNull()

    fun put(context: Context, packageName: String, title: String, text: String, summary: String) {
        runCatching {
            val prefs = context.getSharedPreferences("summary_cache", Context.MODE_PRIVATE)
            val key = "$packageName|${text.ifBlank { title }}"
            prefs.edit().putString(key, summary).putLong(key, System.currentTimeMillis()).apply()
            // 캐시가 무한정 커지지 않도록 오래된 것부터 정리한다.
            val cutoff = System.currentTimeMillis() - 2L * 60 * 60 * 1000
            prefs.all.keys.filter { k ->
                (prefs.getLong(k, 0L) < cutoff)
            }.forEach { prefs.edit().remove(it).apply() }
        }
    }
}

/** 하루 호출 횟수 제한. */
private object CallBudget {
    private const val PREFS = "ai_call_budget"
    private const val KEY_DAY = "day"
    private const val KEY_COUNT = "count"

    /** 하루 최대 호출 횟수. */
    private const val DAILY_LIMIT = 60

    fun usedToday(context: Context): Int = runCatching {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.KOREA).format(java.util.Date())
        if (prefs.getString(KEY_DAY, null) != today) return@runCatching 0
        prefs.getInt(KEY_COUNT, 0)
    }.getOrDefault(0)

    fun tryConsume(context: Context): Boolean = runCatching {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.KOREA).format(java.util.Date())
        val isNewDay = prefs.getString(KEY_DAY, null) != today
        val current = if (isNewDay) 0 else prefs.getInt(KEY_COUNT, 0)
        if (current >= 60) return@runCatching false
        prefs.edit().putString(KEY_DAY, today).putInt(KEY_COUNT, current + 1).apply()
        true
    }.getOrDefault(false)
}