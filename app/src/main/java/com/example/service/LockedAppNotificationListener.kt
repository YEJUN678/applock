package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.util.AppLockPreferences
import com.example.util.NotificationHighlightPrefs
import com.example.util.NotificationHighlighter
import com.example.util.NotificationSummaryService

/**
 * 잠긴 앱 알림을 처리한다.
 *
 * - 강제 모드(미끼 세션): 알림을 지우고 중립적인 알림으로 대체
 * - 알림 보호 + 하이라이트 켬: 내용을 가린 파랑~보라 하이라이트 카드로 대체
 * - AI 키가 있으면 하이라이트 카드에 요약 한 줄을 덧붙임
 */
class LockedAppNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        // 앱 자신의 알림(highlighted_alerts 등)은 재처리하면 안 된다.
        if (notification.packageName == packageName) return

        val context = applicationContext
        val config = AppLockPreferences.getLockConfig(context)

        if (AppLockPreferences.isDuressSession(context)) {
            cancelNotification(notification.key)
            postNeutralNotification()
            return
        }

        if (!AppLockPreferences.isPackageLocked(context, notification.packageName)) return
        if (!config.isNotificationPrivacyEnabled) return

        cancelNotification(notification.key)

        if (!NotificationHighlightPrefs.isEnabled(context)) {
            postMaskedNotification()
            return
        }

        NotificationHighlighter.replaceWithHighlight(
            context = context,
            sbn = notification,
            style = NotificationHighlightPrefs.style(context),
            summaryProvider = { appLabel, content ->
                NotificationSummaryService.summarize(context, appLabel, content)
            }
        )
    }

    private fun postMaskedNotification() {
        val channel = "locked_app_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(channel, "잠긴 앱 알림", NotificationManager.IMPORTANCE_DEFAULT).apply {
                lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
            }
        )
        NotificationManagerCompat.from(this).notify(4403, NotificationCompat.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("보호된 알림")
            .setContentText("잠긴 앱에서 새 알림이 있습니다. 잠금 해제 후 확인하세요.")
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_SECRET)
            .setSilent(true)
            .build())
    }

    private fun postNeutralNotification() {
        val channel = "device_status"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(channel, "기기 상태", NotificationManager.IMPORTANCE_LOW)
        )
        NotificationManagerCompat.from(this).notify(4402, NotificationCompat.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("기기 상태")
            .setContentText("배터리 최적화가 실행 중입니다.")
            .setSilent(true).build())
    }
}