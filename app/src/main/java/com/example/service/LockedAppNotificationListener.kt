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
import com.example.util.NotificationSummaryQueue
import java.util.Collections

/**
 * 잠긴 앱 알림을 처리한다.
 *
 * - 강제 모드(미끼 세션): 알림을 지우고 중립적인 알림으로 대체
 * - 알림 보호 + 하이라이트 켬: 내용을 가린 파랑~보라 하이라이트 카드로 대체
 * - AI 키가 있으면 하이라이트 카드에 요약 한 줄을 덧붙임
 *
 * 주의: 교체를 취소 → 게시 순서로 하면 알림이 사라졌다 다시 뜨면서 깜빡인다.
 * 반드시 교체를 먼저 올리고 원본을 나중에 지워야 빈틈이 없다.
 */
class LockedAppNotificationListener : NotificationListenerService() {

    /** 이미 처리한 알림 키. 같은 알림이 여러 번 들어와도 한 번만 처리한다. */
    private val handledKeys: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        handledKeys.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val sbn = sbn ?: return
        val context = applicationContext

        // 앱 자신의 알림(highlighted_alerts 등)은 건드리면 안 된다.
        // 이것을 빼먹으면 하이라이트 알림을 또 하이라이트로 바꿔 무한 반복하며 깜빡인다.
        if (sbn.packageName == packageName) return

        val config = AppLockPreferences.getLockConfig(context)

        if (AppLockPreferences.isDuressSession(context)) {
            if (handledKeys.add(sbn.key)) {
                postNeutralNotification()
                // 중립 알림을 먼저 올린 뒤 원본을 지운다(그 반대 순서면 깜빡인다).
                cancelNotification(sbn.key)
            }
            return
        }

        if (!AppLockPreferences.isPackageLocked(context, sbn.packageName)) return
        if (!config.isNotificationPrivacyEnabled) return

        if (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0) {
            // 진행 중/技术服务 알림은 시스템이 취소를 막는다. 억지로 지우려 하지 않는다.
            return
        }
        if (!handledKeys.add(sbn.key)) return

        if (!NotificationHighlightPrefs.isEnabled(context)) {
            postMaskedNotification()
            cancelNotification(sbn.key)
            return
        }

        NotificationHighlighter.replaceWithHighlight(
            context = context,
            sbn = sbn,
            style = NotificationHighlightPrefs.style(context),
            // 요약은 배치 큐로 맡긴다. 알림마다 API 를 부르면 무료 티어 한도를 태운다.
            onQueueSummary = { pkg, label, alertTitle, alertText ->
                NotificationSummaryQueue.queue(context, pkg, label, alertTitle, alertText)
            },
            onCancelOriginal = { cancelNotification(sbn.key) }
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let { handledKeys.remove(it.key) }
    }

    /** 알림 접근 권한이 실제로 살아 있는지(자기 알림 말고 외부 알림을 받았는지). */
    companion object {
        @Volatile
        var isConnected: Boolean = false
            private set

        /** 하이라이트 처리 중이던 원본 알림 키를 정리용으로 노출. */
        internal fun forget(key: String) = Unit
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