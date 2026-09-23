package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.app.NotificationManager

/** Alerts only that an app changed; it never uploads the package list. */
class AppChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.data?.schemeSpecificPart == context.packageName || intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
        val label = when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> "새 앱이 설치되었습니다"
            Intent.ACTION_PACKAGE_REMOVED -> "앱이 삭제되었습니다"
            else -> return
        }
        val channel = "security_changes"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(channel, "보안 변경 감시", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        NotificationManagerCompat.from(context).notify(5107, NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("보안 점검 필요")
            .setContentText("$label. App Lock에서 잠금 여부를 확인하세요.")
            .setAutoCancel(true)
            .build())
    }
}
