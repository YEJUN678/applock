package com.example.util

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * 12자리 숫자 복구키.
 *
 * 잠금 비밀번호를 잊었을 때 질문 대신 입력할 수 있는 대체 수단입니다.
 * 12자리(10^12)는 사람이 직접 시도할 수 없는 범위이므로, 의도적으로
 * 시도 횟수 제한을 두지 않습니다. 다만 기기를 가진 사람이 이 화면을 알게 되면
 * 무제한 시도가 가능하므로, 앱에서 미리 경고하고 기기 접근 통제를 함께 쓰는 것을 권합니다.
 *
 * 키도 평문으로 저장하지 않고 salt + SHA-256 해시로만 보관합니다.
 */
object RecoveryKeyManager {

    const val KEY_LENGTH = 12

    private const val PREFS = "recovery_key"
    private const val KEY_HASH = "key_hash"
    private const val KEY_SALT = "key_salt"
    private const val KEY_ATTEMPTS = "failed_attempts"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasKey(context: Context): Boolean =
        prefs(context).getString(KEY_HASH, null) != null

    /** 숫자만 남기고 정확히 12자리가 아니면 false. */
    fun normalize(input: String): String = input.filter { it.isDigit() }

    fun isValidFormat(input: String): Boolean = normalize(input).length == KEY_LENGTH

    /** 설정 성공 시 true. 형식이 틀리면 false (기존 키는 그대로 유지). */
    fun setKey(context: Context, key: String): Boolean {
        val normalized = normalize(key)
        if (normalized.length != KEY_LENGTH) return false
        val salt = randomSalt()
        prefs(context).edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, hashOf(normalized, salt))
            .putInt(KEY_ATTEMPTS, 0)
            .apply()
        return true
    }

    fun clearKey(context: Context) {
        prefs(context).edit().remove(KEY_HASH).remove(KEY_SALT).remove(KEY_ATTEMPTS).apply()
    }

    /**
     * 복구키 대조. 길이 차이로 시간 정보가 새지 않도록 상수 시간 비교한다.
     * 실패 횟수는 기록만 하고(차단하지 않는다) 설정 화면에서 보여준다.
     */
    fun verifyKey(context: Context, input: String): Boolean {
        val p = prefs(context)
        val storedHash = p.getString(KEY_HASH, null) ?: return false
        val salt = p.getString(KEY_SALT, null) ?: return false
        val candidate = normalize(input)
        if (candidate.length != KEY_LENGTH) {
            registerFailure(context)
            return false
        }
        val matched = constantTimeEquals(hashOf(candidate, salt), storedHash)
        if (matched) p.edit().putInt(KEY_ATTEMPTS, 0).apply() else registerFailure(context)
        return matched
    }

    /** 시도 횟수는 막지 않고 기록만 남긴다. */
    fun registerFailure(context: Context) {
        val p = prefs(context)
        p.edit().putInt(KEY_ATTEMPTS, p.getInt(KEY_ATTEMPTS, 0) + 1).apply()
    }

    fun failedAttempts(context: Context): Int =
        prefs(context).getInt(KEY_ATTEMPTS, 0).coerceAtLeast(0)

    /** 설정 화면에서 미리 입력해 둘 수 있는 임의의 12자리 키 생성기. */
    fun generateRandomKey(): String {
        val random = SecureRandom()
        return buildString {
            repeat(KEY_LENGTH) { append(random.nextInt(10)) }
        }
    }

    private fun randomSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hashOf(normalized: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest("$salt::$normalized".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (index in a.indices) diff = diff or (a[index].code xor b[index].code)
        return diff == 0
    }
}