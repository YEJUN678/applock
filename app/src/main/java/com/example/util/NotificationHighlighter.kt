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
 * 파랑~보라 그라디언트 카드로 바꿔 띄운다. AI 키가 설정돼 있으면
 * 요약 한 줄을 덧붙인다(키가 없으면 요약 없이 동작한다).
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
     * @return AI 요약을 붙였으면 true, 아니면 false
     */
    fun replaceWithHighlight(
        context: Context,
        sbn: StatusBarNotification,
        style: Style,
        summaryProvider: ((String, String) -> String?)? = null,
        onCancelOriginal: (() -> Unit)? = null
    ) {
        val notification = sbn.notification
        val appLabel = appLabel(context, sbn.packageName)
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        ensureChannel(context)

        // 알림 하나당 고유 ID 를 써야 여러 개가 서로 덮어쓰지 않는다.
        val id = NOTIFICATION_ID_BASE + (sbn.key.hashCode() and 0xFFFF)
        val views = RemoteViews(context.packageName, R.layout.notify_highlight).apply {
            setTextViewText(R.id.highlight_app, appLabel)
            setTextViewText(R.id.highlight_title, title.ifBlank { "새 알림" })
            setTextViewText(R.id.highlight_text, text.ifBlank { "내용 없음" })
            setTextViewText(R.id.highlight_summary, "")
            setViewVisibility(R.id.highlight_summary, View.GONE)
            setInt(R.id.highlight_root, "setBackgroundResource", backgroundFor(style))
        }

        val built = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setColor(0xFF4D7CFF.toInt())
            .setCustomContentView(views)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$text"))
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        // 교체를 먼저 올린다. 원본을 먼저 지우면 알림이 사라졌다가 다시 떠서 깜빡인다.
        NotificationManagerCompat.from(context).notify(id, built)
        onCancelOriginal?.invoke()

        // AI 요약은 네트워크가 필요하므로 알림을 띄운 뒤에 갱신한다.
        val prompt = "$appLabel 알림\n제목: ${title.ifBlank { "(없음)" }}\n내용: ${text.ifBlank { "(없음)" }}"
        Thread {
            val summary = summaryProvider?.invoke(appLabel, prompt)?.takeIf { it.isNotBlank() } ?: return@Thread
            val summaryViews = RemoteViews(context.packageName, R.layout.notify_highlight).apply {
                setTextViewText(R.id.highlight_app, appLabel)
                setTextViewText(R.id.highlight_title, title.ifBlank { "새 알림" })
                setTextViewText(R.id.highlight_text, text.ifBlank { "내용 없음" })
                setTextViewText(R.id.highlight_summary, summary)
                setViewVisibility(R.id.highlight_summary, View.VISIBLE)
                setInt(R.id.highlight_root, "setBackgroundResource", backgroundFor(style))
            }
            NotificationManagerCompat.from(context).notify(
                id,
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .setColor(0xFF4D7CFF.toInt())
                    .setCustomContentView(summaryViews)
                    .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$text"))
                    .setAutoCancel(true)
                    .setSilent(true)
                    .build()
            )
        }.start()
    }


    private fun appLabel(context: Context, packageName: String): String = runCatching {
        val manager = context.getPackageManager()
        manager.getApplicationLabel(manager.getApplicationInfo(packageName, 0)).toString()
    }.getOrElse { packageName }
}