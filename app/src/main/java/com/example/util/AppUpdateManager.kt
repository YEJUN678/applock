package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.IOException
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
    private const val RAW_BASE = "https://raw.githubusercontent.com/YEJUN678/applock"

    /**
     * 릴리스 노트 파일 후보. main/master 어느 쪽에 올려도 갱신이 동작하도록 둘 다 조회한다.
     * (한쪽 브랜치에만 올리면 갱신 확인이 조용히 실패하던 문제)
     */
    val VERSION_FILE_URLS = listOf(
        "$RAW_BASE/main/versions.txt",
        "$RAW_BASE/master/versions.txt"
    )

    /** 네트워크 실패와 배포 파일 오류를 구분해 그대로 전달한다. */
    class UpdateCheckException(message: String, cause: Throwable? = null) : IOException(message, cause)

    /**
     * @throws UpdateCheckException 원인이 다른 예외. 호출부는 메시지만 그대로 보여주면 된다.
     */
    fun check(): AppUpdate {
        var lastError: UpdateCheckException = UpdateCheckException("업데이트 정보를 가져오지 못했습니다.")
        for (url in VERSION_FILE_URLS) {
            try {
                return fetch(url)
            } catch (error: UpdateCheckException) {
                lastError = error
            } catch (error: IOException) {
                lastError = UpdateCheckException("인터넷 연결을 확인하세요. ($url)", error)
            } catch (error: Exception) {
                lastError = UpdateCheckException("릴리스 노트를 해석하지 못했습니다. ($url)", error)
            }
        }
        throw lastError
    }

    private fun fetch(url: String): AppUpdate {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("Accept", "text/plain")
        }
        try {
            val status = connection.responseCode
            if (status == 404) throw UpdateCheckException("릴리스 노트(versions.txt)가 없습니다: $url")
            if (status !in 200..299) throw UpdateCheckException("업데이트 서버 응답 오류 (HTTP $status).")
            val values = connection.inputStream.bufferedReader().useLines { lines ->
                lines.map(String::trim)
                    .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
                    .associate { line ->
                        val (key, value) = line.split("=", limit = 2)
                        key.trim() to value.trim()
                    }
            }
            val rawVersionCode = values["versionCode"].orEmpty()
            val versionCode = parseVersionCode(rawVersionCode)
                ?: throw UpdateCheckException(
                    "릴리스 노트의 versionCode 형식이 올바르지 않습니다: '$rawVersionCode' (정수여야 합니다)."
                )
            val apkUrl = values["apkUrl"]?.takeIf { it.startsWith("https://") }
                ?: throw UpdateCheckException("릴리스 노트에 https 로 시작하는 apkUrl 이 없습니다.")
            val versionName = values["versionName"]?.takeIf { it.isNotBlank() } ?: versionCode.toString()
            return AppUpdate(versionCode, versionName, apkUrl, unescapeNotes(values["notes"].orEmpty()))
        } finally {
            connection.disconnect()
        }
    }

    /** "9", "9.1", "v12" 같은 값에서 정수 부분만 읽는다. 잘못된 값은 null. */
    fun parseVersionCode(raw: String): Int? =
        raw.trim().trimStart('v', 'V').substringBefore('.').toIntOrNull()

    /** "7.10" > "7.9" 처럼 이름만으로 새 버전인지 판별한다. */
    fun isVersionNameNewer(remoteName: String, localName: String): Boolean {
        fun parts(value: String) = value.split('.', '-', '_', '+')
            .map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val remote = parts(remoteName)
        val local = parts(localName)
        for (index in 0 until maxOf(remote.size, local.size)) {
            val r = remote.getOrElse(index) { 0 }
            val l = local.getOrElse(index) { 0 }
            if (r != l) return r > l
        }
        return false
    }

    /**
     * 설치된 APK보다 버전코드(또는 버전 이름)가 높아야 업데이트로 인정한다.
     * 버전코드는 낮추면 Android가 설치를 거부하므로 이름만으로는 승격하지 않는다.
     */
    fun isUpdateAvailable(update: AppUpdate, installedVersionCode: Int, installedVersionName: String): Boolean =
        update.versionCode > installedVersionCode ||
            (update.versionCode == installedVersionCode && isVersionNameNewer(update.versionName, installedVersionName))

    /** 배포 노트의 `\n` / 줄바꿈 이스케이프를 실제 줄바꿈으로 되돌린다. */
    private fun unescapeNotes(notes: String): String =
        notes.replace("\\n", "\n").replace("\\", "").trim()

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

    fun downloadedApkUri(context: Context, downloadId: Long): Uri? =
        downloadStatus(context, downloadId).apkUri

    /** Returns progress as well as terminal failure, which DownloadManager otherwise hides. */
    fun downloadStatus(context: Context, downloadId: Long): AppUpdateDownloadStatus {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            if (!cursor.moveToFirst()) return AppUpdateDownloadStatus(failed = true)
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                return AppUpdateDownloadStatus(
                    apkUri = manager.getUriForDownloadedFile(downloadId),
                    progressPercent = 100
                )
            }
            if (status == DownloadManager.STATUS_FAILED) return AppUpdateDownloadStatus(failed = true)
            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val progress = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 99) else null
            return AppUpdateDownloadStatus(progressPercent = progress)
        }
    }
}