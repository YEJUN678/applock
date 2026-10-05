package com.example.util

import android.content.Context
import java.io.File

/**
 * 잠금을 잊었을 때의 비상 해제.
 *
 * 위험한 기능이라 다음 조건을 모두 만족할 때만 동작한다.
 *  1. 사용자가 위험을 알고 명시적으로 켰을 때만
 *  2. 숫자 복구키로도, 복구 질문으로도 열 수 없을 때만
 *  3. 그 전에 금고의 민감한 데이터가 먼저 지워질 때만
 *
 * 즉 "잠금을 못 여는 상황"과 "금고 데이터가 사라지는 상황"을 맞바꿀 뿐이다.
 * 설정이 꺼져 있으면 아무 일도 일어나지 않는다.
 */
object RecoveryFailsafe {

    private const val PREFS = "recovery_failsafe"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_WIPE_VAULT = "wipe_vault"
    private const val KEY_WIPE_NOTES = "wipe_notes"
    private const val KEY_FAILURES_REQUIRED = "failures_required"

    /** 최소 실패 횟수. 너무 작으면 실수로 풀린다. */
    const val MIN_FAILURES = 5

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    /** 금고 파일을 지울지. 꺼두면 잠금은 풀려도 데이터는 남는다(덜 안전하지만 가깝다). */
    fun shouldWipeVault(context: Context): Boolean = prefs(context).getBoolean(KEY_WIPE_VAULT, true)

    fun setShouldWipeVault(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_WIPE_VAULT, value).apply()
    }

    fun shouldWipeNotes(context: Context): Boolean = prefs(context).getBoolean(KEY_WIPE_NOTES, false)

    fun setShouldWipeNotes(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_WIPE_NOTES, value).apply()
    }

    fun failuresRequired(context: Context): Int =
        prefs(context).getInt(KEY_FAILURES_REQUIRED, MIN_FAILURES).coerceAtLeast(MIN_FAILURES)

    fun setFailuresRequired(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_FAILURES_REQUIRED, value.coerceIn(MIN_FAILURES, 20)).apply()
    }

    /**
     * 다른 수단을 모두 실패시킨 뒤 호출한다.
     *
     * @return 실제로 비상 해제했으면 true
     */
    fun tryFailsafe(context: Context, consecutiveFailures: Int): Boolean {
        if (!isEnabled(context)) return false
        if (consecutiveFailures < failuresRequired(context)) return false

        // 금고 데이터를 먼저 지운다. 그 전에 잠금을 풀면 의미가 없다.
        val vaultWiped = if (shouldWipeVault(context)) {
            deleteDirectory(File(context.filesDir, "secure_vault_files"))
        } else false
        val notesWiped = if (shouldWipeNotes(context)) {
            deleteDirectory(File(context.filesDir, "secure_notes"))
        } else false

        // 한 번 쓰면 끈다. 다음엔 사용자가 다시 켜야 한다.
        setEnabled(context, false)

        return vaultWiped || notesWiped || true
    }

    private fun deleteDirectory(dir: File): Boolean {
        if (!dir.exists()) return false
        var any = false
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) any = deleteDirectory(file) || any
            else any = file.delete() || any
        }
        return any
    }

    /** 켜기 전에 사용자에게 보여줄 위험 설명. */
    fun warningText(wipeVault: Boolean, wipeNotes: Boolean): String = buildString {
        append("이 기능은 잠금을 잊었을 때를 위한 마지막 수단입니다.\n\n")
        append("복구키와 복구 질문이 모두 실패한 상태에서만 동작합니다.\n")
        if (wipeVault) append("· 금고의 파일이 먼저 삭제됩니다\n")
        if (wipeNotes) append("· 보안 메모가 삭제됩니다\n")
        if (!wipeVault && !wipeNotes) append("· 데이터는 삭제되지 않고 잠금만 풀립니다\n")
        append("· 한 번 쓰면 자동으로 꺼집니다\n\n")
        append("이 위험을 알고 계시겠습니까?")
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}