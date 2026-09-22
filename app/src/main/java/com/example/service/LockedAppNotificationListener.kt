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
        }
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
