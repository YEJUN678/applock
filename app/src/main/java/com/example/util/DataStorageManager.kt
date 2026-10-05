package com.example.util

import android.content.Context
import java.io.File

/**
 * 앱이 차지하는 용량과 정리 기능.
 *
 * 침입 증거(사진·영상·음성)는 계속 쌓이는데, 지금은 얼마나 쓰는지 알 방법이 없다.
 * 금고가 왜 비어 보이지 않거나, 저장 공간이 왜 차는지 알 수 있게 한다.
 */
object DataStorageManager {

    data class Usage(
        val intruderPhotos: Long,
        val intruderVideos: Long,
        val aiGuardAudio: Long,
        val vault: Long,
        val other: Long
    ) {
        val total: Long get() = intruderPhotos + intruderVideos + aiGuardAudio + vault + other
    }

    fun usage(context: Context): Usage {
        fun sizeOf(name: String): Long {
            val dir = File(context.filesDir, name)
            if (!dir.exists()) return 0
            return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        val known = setOf("intruder_photos", "intruder_videos", "ai_guard_audio", "secure_vault_files")
        val other = context.filesDir.listFiles()
            ?.filter { it.isDirectory && it.name !in known }
            ?.sumOf { dir -> dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } }
            ?: 0L

        return Usage(
            intruderPhotos = sizeOf("intruder_photos"),
            intruderVideos = sizeOf("intruder_videos"),
            aiGuardAudio = sizeOf("ai_guard_audio"),
            vault = sizeOf("secure_vault_files"),
            other = other
        )
    }

    fun formatSize(bytes: Long): String = when {
        bytes >= 1024L * 1024L * 1024L -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format("%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }

    /**
     * 오래된 증거를 지운다.
     * 기록 자체(메타데이터)는 남기고 파일만 지워서, "언제 몇 번 시도했는가"는 보존한다.
     */
    fun deleteEvidenceOlderThan(context: Context, days: Int): Int {
        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        var deleted = 0
        listOf("intruder_photos", "intruder_videos", "ai_guard_audio").forEach { name ->
            val dir = File(context.filesDir, name)
            if (!dir.exists()) return@forEach
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoff && file.delete()) deleted++
            }
        }
        return deleted
    }

    /** 증거 파일만 전부 지운다(메타데이터는 유지). */
    fun deleteAllEvidenceFiles(context: Context): Int {
        var deleted = 0
        listOf("intruder_photos", "intruder_videos", "ai_guard_audio").forEach { name ->
            val dir = File(context.filesDir, name)
            if (!dir.exists()) return@forEach
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.delete()) deleted++
            }
        }
        return deleted
    }

    /** 실행 기록을 n일 지난 것부터 지운다. 0 이면 전부 지운다. */
    fun pruneSessions(context: Context, keepDays: Int): Int {
        val before = AppLockPreferences.getSessionLog(context).size
        AppLockPreferences.pruneSessionLog(context, keepDays)
        return before - AppLockPreferences.getSessionLog(context).size
    }
}