package com.example.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AI 기능 설정(API 키, 모델 이름).
 *
 * 키는 SharedPreferences에 평문으로 두지 않고 Keystore의 AES-GCM 키로 암호화해 저장한다.
 * 앱을 처음 실행하면 기본 키를 1회 암호화해서 넣으며, 설정 화면에서 언제든 교체/삭제할 수 있다.
 */
object AiSettings {

    private const val PREFS = "ai_settings"
    private const val KEY_ENCRYPTED = "api_key_encrypted"
    private const val KEY_MODEL = "model_name"

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "app_lock_ai_key_v1"

    /** 기본 모델 (신규 키는 2.5 이하가 차단되어 3.x flash 를 쓴다). */
    const val DEFAULT_MODEL = "gemini-3.8-flash"

    /**
     * 기본 키는 비워 둔다.
     * 저장소가 공개되어 있어 소스에 키를 넣으면 APK 를 분해한 누구나 그대로 쓸 수 있다.
     * 실제 키는 앱 안의 [AI 키] 화면에서 입력하면 Keystore 로 암호화되어 저장된다.
     */
    private const val DEFAULT_API_KEY = ""

    fun model(context: Context): String =
        prefs(context).getString(KEY_MODEL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL

    fun setModel(context: Context, value: String) {
        prefs(context).edit().putString(KEY_MODEL, value.trim()).apply()
    }

    fun isConfigured(context: Context): Boolean = apiKey(context) != null

    /** 암호화된 키를 읽고, 없으면 기본 키(비어 있음)가 채워진다. */
    fun apiKey(context: Context): String? {
        val stored = prefs(context).getString(KEY_ENCRYPTED, null)
        if (stored != null) return decrypt(stored)
        if (DEFAULT_API_KEY.isBlank()) return null
        // 기본 키가 있을 때만 최초 1회 암호화해서 보관한다.
        return runCatching {
            encrypt(DEFAULT_API_KEY)?.let { encrypted ->
                prefs(context).edit().putString(KEY_ENCRYPTED, encrypted).apply()
            }
            DEFAULT_API_KEY
        }.getOrNull()
    }

    fun setApiKey(context: Context, key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return false
        val encrypted = encrypt(trimmed) ?: return false
        return prefs(context).edit().putString(KEY_ENCRYPTED, encrypted).commit()
    }

    fun clearApiKey(context: Context) {
        prefs(context).edit().remove(KEY_ENCRYPTED).apply()
    }

    /** 키의 앞 6자만 보여준다(전체 노출 방지). */
    fun maskedKey(context: Context): String {
        val key = apiKey(context) ?: return "설정되지 않음"
        return if (key.length <= 10) "••••" else "${key.take(6)}••••••${key.takeLast(4)}"
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    private fun encrypt(value: String): String? = runCatching {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val payload = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        Base64.encodeToString(payload, Base64.NO_WRAP)
    }.getOrNull()

    private fun decrypt(stored: String): String? = runCatching {
        val payload = Base64.decode(stored, Base64.NO_WRAP)
        val iv = ByteArray(12)
        System.arraycopy(payload, 0, iv, 0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(payload, 12, payload.size - 12), Charsets.UTF_8)
    }.getOrNull()
}