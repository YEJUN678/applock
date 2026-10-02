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
    fun recordFiveSeconds(context: Context, onSaved: (String?) -> Unit = {}) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { onSaved(null); return }
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
            onSaved(null)
            return
        }
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching { recorder.stop() }
            recorder.release()
            onSaved(output.takeIf { it.isFile && it.length() > 0 }?.absolutePath)
        }, 5_000L)
    }
}
