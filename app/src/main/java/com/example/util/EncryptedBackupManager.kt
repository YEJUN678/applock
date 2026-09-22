package com.example.util

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Password-encrypted, user-exported backup that remains usable after an uninstall. */
object EncryptedBackupManager {
    private const val MAGIC = "ALBK2"
    private const val PREFS = "app_lock_prefs"
    private const val VAULT_KEY_PREFS = "applock_vault_crypto_pref"
    private const val ITERATIONS = 210_000

    fun export(context: Context, destination: Uri, password: CharArray) {
        require(password.size >= 6) { "백업 비밀번호는 6자 이상이어야 합니다." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val key = key(password, salt)
        context.contentResolver.openOutputStream(destination)?.use { raw ->
            val header = DataOutputStream(raw)
            header.writeUTF(MAGIC); header.write(salt); header.write(iv); header.flush()
            DataOutputStream(CipherOutputStream(raw, cipher(Cipher.ENCRYPT_MODE, key, iv))).use { out ->
                out.writeUTF(preferencesJson(context, PREFS).toString())
                out.writeUTF(preferencesJson(context, VAULT_KEY_PREFS).toString())
                val files = backupFiles(context)
                out.writeInt(files.size)
                files.forEach { (kind, file) ->
                    out.writeUTF(kind); out.writeUTF(file.name); out.writeLong(file.length())
                    file.inputStream().use { it.copyTo(out) }
                }
            }
        } ?: error("백업 파일을 만들 수 없습니다.")
    }

    fun restore(context: Context, source: Uri, password: CharArray) {
        context.contentResolver.openInputStream(source)?.use { raw ->
            val header = DataInputStream(raw)
            require(header.readUTF() == MAGIC) { "지원하지 않는 백업 파일입니다." }
            val salt = ByteArray(16).also { header.readFully(it) }
            val iv = ByteArray(12).also { header.readFully(it) }
            DataInputStream(CipherInputStream(raw, cipher(Cipher.DECRYPT_MODE, key(password, salt), iv))).use { input ->
                restorePreferences(context, PREFS, JSONObject(input.readUTF()))
                restorePreferences(context, VAULT_KEY_PREFS, JSONObject(input.readUTF()))
                repeat(input.readInt()) {
                    val kind = input.readUTF(); val name = input.readUTF(); val size = input.readLong()
                    require(name == File(name).name) { "잘못된 백업 파일 이름입니다." }
                    val dir = File(context.filesDir, if (kind == "vault") "secure_vault_files" else "intruder_photos").apply { mkdirs() }
                    File(dir, name).outputStream().use { output ->
                        var left = size; val buffer = ByteArray(64 * 1024)
                        while (left > 0) { val n = input.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt()); require(n > 0); output.write(buffer, 0, n); left -= n }
                    }
                }
            }
        } ?: error("백업 파일을 열 수 없습니다.")
        AppLockPreferences.resetAllTemporaryUnlocks()
        AppLockPreferences.invalidateCachedState()
    }

    private fun key(password: CharArray, salt: ByteArray) = SecretKeySpec(
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password, salt, ITERATIONS, 256)).encoded, "AES"
    )
    private fun cipher(mode: Int, key: SecretKeySpec, iv: ByteArray) = Cipher.getInstance("AES/GCM/NoPadding").apply { init(mode, key, GCMParameterSpec(128, iv)) }
    private fun backupFiles(context: Context): List<Pair<String, File>> = listOf("vault" to File(context.filesDir, "secure_vault_files"), "photo" to File(context.filesDir, "intruder_photos")).flatMap { (kind, dir) -> dir.listFiles()?.filter { it.isFile }?.map { kind to it } ?: emptyList() }
    private fun preferencesJson(context: Context, prefsName: String): JSONObject = JSONObject().also { root ->
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).all.forEach { (k, v) -> when (v) {
            is String -> root.put(k, JSONObject().put("s", v)); is Int -> root.put(k, JSONObject().put("i", v)); is Long -> root.put(k, JSONObject().put("l", v)); is Boolean -> root.put(k, JSONObject().put("b", v)); is Set<*> -> root.put(k, JSONObject().put("set", JSONArray(v.filterIsInstance<String>())) )
        } }
    }
    private fun restorePreferences(context: Context, prefsName: String, json: JSONObject) {
        val edit = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear()
        json.keys().forEach { k -> val v = json.getJSONObject(k); when { v.has("s") -> edit.putString(k, v.getString("s")); v.has("i") -> edit.putInt(k, v.getInt("i")); v.has("l") -> edit.putLong(k, v.getLong("l")); v.has("b") -> edit.putBoolean(k, v.getBoolean("b")); v.has("set") -> edit.putStringSet(k, (0 until v.getJSONArray("set").length()).map { v.getJSONArray("set").getString(it) }.toSet()) } }
        edit.commit()
    }
}
