package com.example.util

import android.content.Context

/**
 * 알림 하이라이트 설정.
 * 잠금 설정(LockConfig)과 분리해 두는 이유:
 * 화면 꾸미기 쪽 성격이라 잠금 해제 조건과 무관하게 유지되어야 한다.
 */
object NotificationHighlightPrefs {

    private const val PREFS = "notification_highlight"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_STYLE = "style"
    private const val KEY_AI_SUMMARY = "ai_summary"

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun style(context: Context): NotificationHighlighter.Style {
        val stored = prefs(context).getString(KEY_STYLE, null) ?: return NotificationHighlighter.Style.BLUE_VIOLET
        return runCatching { NotificationHighlighter.Style.valueOf(stored) }
            .getOrDefault(NotificationHighlighter.Style.BLUE_VIOLET)
    }

    fun setStyle(context: Context, value: NotificationHighlighter.Style) {
        prefs(context).edit().putString(KEY_STYLE, value.name).apply()
    }

    /** AI 요약은 기본 꺼짐. 켜면 알림 본문이 클라우드로 전송된다. */
    fun isAiSummaryEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AI_SUMMARY, false)

    fun setAiSummaryEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_AI_SUMMARY, value).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}