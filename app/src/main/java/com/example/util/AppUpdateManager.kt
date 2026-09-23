package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(val versionCode: Int, val versionName: String, val apkUrl: String, val notes: String = "")
data class AppUpdateDownloadStatus(
    val apkUri: Uri? = null,
    val progressPercent: Int? = null,
    val failed: Boolean = false
)

/** Small, dependency-free updater for APKs distributed outside Play Store. */
object AppUpdateManager {
    const val VERSION_FILE_URL = "https://raw.githubusercontent.com/YEJUN678/applock/main/versions.txt"

    fun check(): AppUpdate? {
        val connection = (URL(VERSION_FILE_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val values = connection.inputStream.bufferedReader().useLines { lines ->
                lines.map(String::trim)
                    .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
                    .associate { line ->
                        val (key, value) = line.split("=", limit = 2)
                        key.trim() to value.trim()
                    }
            }
            val versionCode = values["versionCode"]?.toIntOrNull() ?: return null
            val apkUrl = values["apkUrl"]?.takeIf { it.startsWith("https://") } ?: return null
            AppUpdate(versionCode, values["versionName"] ?: versionCode.toString(), apkUrl, values["notes"] ?: "")
        } finally {
            connection.disconnect()
        }
    }

    fun download(context: Context, update: AppUpdate): Long {
        val request = DownloadManager.Request(Uri.parse(update.apkUrl))
            .setTitle("App Lock ${update.versionName}")
            .setDescription("업데이트 파일을 다운로드하는 중입니다")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "AppLockUpdates/AppLock-${update.versionCode}.apk"
            )
        return (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
    }

    fun downloadedApkUri(context: Context, downloadId: Long): Uri? {
        return downloadStatus(context, downloadId).apkUri
    }

    /** Returns progress as well as terminal failure, which DownloadManager otherwise hides. */
    fun downloadStatus(context: Context, downloadId: Long): AppUpdateDownloadStatus {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            if (!cursor.moveToFirst()) return AppUpdateDownloadStatus(failed = true)
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                return AppUpdateDownloadStatus(apkUri = manager.getUriForDownloadedFile(downloadId), progressPercent = 100)
            }
            if (status == DownloadManager.STATUS_FAILED) return AppUpdateDownloadStatus(failed = true)
            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val progress = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 99) else null
            return AppUpdateDownloadStatus(progressPercent = progress)
        }
    }
}
