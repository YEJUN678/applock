package com.example.util

import android.content.Context

/**
 * 알림 요약.
 *
 * AI 키가 없으면 아무것도 하지 않는다(요약 없이 하이라이트만 표시).
 * 키가 있으면 알림 내용을 Gemini 로 보내 한 줄 요약을 받는다.
 *
 * 프라이버시: 알림 본문은 이때만 클라우드로 나갑니다.
 * 설정에서 요약 기능을 꺼 두면 전송 자체가 일어나지 않습니다.
 */
object NotificationSummaryService {

    /** 요약 기능이 켜져 있고 AI 키도 있을 때만 true. */
    fun isAvailable(context: Context): Boolean =
        NotificationHighlightPrefs.isAiSummaryEnabled(context) && AiSettings.isConfigured(context)

    /**
     * 알림 내용을 한 줄로 요약한다. 실패하면 null 을 돌려주고,
     * 하이라이트 알림은 요약 없이 그대로 유지된다.
     */
    fun summarize(context: Context, appLabel: String, content: String): String? {
        if (!isAvailable(context)) return null
        val prompt = buildString {
            appendLine("아래는 사용자가 잠근 앱에서 도착한 알림이다.")
            appendLine("발신 앱: $appLabel")
            appendLine(content)
            appendLine()
            append("위 알림을 한국어 한 문장(40자 이내)으로 요약하라. 분류나 평가 없이 사실만 말하라.")
        }
        return when (val result = GeminiClient.ask(context, prompt)) {
            is GeminiClient.Result.Success -> result.text.trim().lineSequence().firstOrNull()?.take(120)
            is GeminiClient.Result.Failure -> null
        }
    }
}