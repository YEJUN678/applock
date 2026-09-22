package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.util.AppLockPreferences

/** Removes notification content from apps the owner has marked as locked. */
class LockedAppNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val config = AppLockPreferences.getLockConfig(applicationContext)
        if (config.isNotificationPrivacyEnabled && AppLockPreferences.isPackageLocked(applicationContext, notification.packageName)) {
            cancelNotification(notification.key)
        }
    }
}
