package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.util.AppLockPreferences

/** Removes notification content from apps the owner has marked as locked. */
class LockedAppNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val config = AppLockPreferences.getLockConfig(applicationContext)
        if (AppLockPreferences.isDuressSession(applicationContext) && notification.packageName != packageName) {
            cancelNotification(notification.key)
            postNeutralNotification()
        } else if (config.isNotificationPrivacyEnabled && AppLockPreferences.isPackageLocked(applicationContext, notification.packageName)) {
            cancelNotification(notification.key)
            postMaskedNotification()
        }
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
