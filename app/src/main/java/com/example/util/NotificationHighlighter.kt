package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R

/**
 * 알림 하이라이트.
 *
 * 잠긴 앱의 알림을 그대로 두면 다른 사람 눈에 띄므로, 내용을 가린 알림을
 * 파랑~보라 그라디언트 카드로 바꿔 띄운다.
 * AI 요약은 배치 큐([NotificationSummaryQueue])가 나중에 붙인다.
 */
object NotificationHighlighter {

    const val CHANNEL_ID = "highlighted_alerts"
    const val NOTIFICATION_ID_BASE = 7700

    /** 하이라이트 스타일. */
    enum class Style { BLUE_VIOLET, VIOLET, TEAL }

    fun backgroundFor(style: Style): Int = when (style) {
        Style.BLUE_VIOLET -> R.drawable.notify_highlight_bg
        Style.VIOLET -> R.drawable.notify_highlight_bg_violet
        Style.TEAL -> R.drawable.notify_highlight_bg_teal
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "알림 하이라이트",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "잠긴 앱의 알림을 보호된 카드로 바꿔 보여줍니다."
                enableVibration(false)
                setShowBadge(true)
            }
        )
    }

    /**
     * 원본 알림을 취소하고 하이라이트 알림을 대신 띄운다.
     *
     * 요약은 즉시 호출하지 않고 [onQueueSummary] 로 넘긴다.
     * 알림마다 API 를 부르면 무료 티어 할당량을 금방 태우기 때문이다.
     */
    fun replaceWithHighlight(
        context: Context,
        sbn: StatusBarNotification,
        style: Style,
        summaryProvider: ((String, String) -> String?)? = null,
        onCancelOriginal: (() -> Unit)? = null,
        onQueueSummary: ((String, String, String, String) -> Unit)? = null
    ) {
        val notification = sbn.notification
        val label = appLabel(context, sbn.packageName)
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        ensureChannel(context)

        // 교체를 먼저 올린다. 원본을 먼저 지우면 알림이 사라졌다가 다시 떠서 깜빡인다.
        val built = buildNotification(context, label, title, text, null, style)
        NotificationManagerCompat.from(context).notify(idFor(sbn.packageName, title, text), built)
        onCancelOriginal?.invoke()

        // 소유자가 나중에 볼 수 있도록 모아 둔다(dismiss 해도 사라지지 않아야 한다).
        NotificationInbox.add(context, sbn.packageName, label, title, text)

        if (onQueueSummary != null) {
            onQueueSummary(sbn.packageName, label, title, text)
            return
        }

        // 배치 큐를 쓰지 않는 특수 경로만 즉시 요약한다.
        if (summaryProvider != null) {
            val prompt = "$label 알림\n제목: ${title.ifBlank { "(없음)" }}\n내용: ${text.ifBlank { "(없음)" }}"
            Thread {
                val summary = summaryProvider.invoke(label, prompt)?.takeIf { it.isNotBlank() } ?: return@Thread
                updatePostedSummary(context, sbn.packageName, title, text, summary)
                NotificationInbox.attachSummary(context, sbn.packageName, title, text, summary)
            }.start()
        }
    }

    /** 이미 떠 있는 하이라이트 알림에 요약을 붙여 갱신한다. */
    fun updatePostedSummary(
        context: Context,
        packageName: String,
        title: String,
        text: String,
        summary: String
    ) {
        val updated = buildNotification(
            context = context,
            appLabel = appLabel(context, packageName),
            title = title,
            text = text,
            summary = summary,
            style = NotificationHighlightPrefs.style(context)
        )
        NotificationManagerCompat.from(context).notify(idFor(packageName, title, text), updated)
    }

    /**
     * 알림 ID 규칙.
     * 게시와 갱신이 반드시 같은 ID 를 써야 카드가 새로 뜨지 않고 갱신된다.
     */
    private fun idFor(packageName: String, title: String, text: String): Int {
        val base = "$packageName|$title|$text"
        return NOTIFICATION_ID_BASE + (base.hashCode() and 0xFFFF)
    }

    private fun buildNotification(
        context: Context,
        appLabel: String,
        title: String,
        text: String,
        summary: String?,
        style: Style
    ): Notification {
        ensureChannel(context)
        val hasSummary = !summary.isNullOrBlank()
        val views = RemoteViews(context.packageName, R.layout.notify_highlight).apply {
            setTextViewText(R.id.highlight_app, appLabel)
            setTextViewText(R.id.highlight_title, title.ifBlank { "새 알림" })
            setTextViewText(R.id.highlight_text, text.ifBlank { "내용 없음" })
            setTextViewText(R.id.highlight_summary, summary.orEmpty())
            setViewVisibility(R.id.highlight_summary, if (hasSummary) View.VISIBLE else View.GONE)
            setInt(R.id.highlight_root, "setBackgroundResource", backgroundFor(style))
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setColor(0xFF4D7CFF.toInt())
            .setCustomContentView(views)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$text"))
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    private fun appLabel(context: Context, packageName: String): String = runCatching {
        val manager = context.getPackageManager()
        manager.getApplicationLabel(manager.getApplicationInfo(packageName, 0)).toString()
    }.getOrElse { packageName }
}