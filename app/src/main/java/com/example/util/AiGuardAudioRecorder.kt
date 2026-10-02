package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import java.io.File

/** Captures a short local evidence clip only after an AI Guard anomaly. */
object AiGuardAudioRecorder {
    fun recordFiveSeconds(context: Context) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        val folder = File(context.filesDir, "ai_guard_audio").apply { mkdirs() }
        folder.listFiles()?.sortedByDescending(File::lastModified)?.drop(9)?.forEach { it.delete() }
        val output = File(folder, "guard-${System.currentTimeMillis()}.m4a")
        val recorder = try {
            MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(output.absolutePath)
                prepare()
                start()
            }
        } catch (_: Exception) {
            output.delete()
            return
        }
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching { recorder.stop() }
            recorder.release()
        }, 5_000L)
    }
}
