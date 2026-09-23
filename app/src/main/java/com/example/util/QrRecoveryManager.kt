package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.security.MessageDigest
import java.security.SecureRandom

/** Stores only a digest locally; the recoverable secret exists solely in the owner's QR image. */
object QrRecoveryManager {
    private const val KEY_RECOVERY_TOKEN_HASH = "qr_recovery_token_hash"
    private const val PREFIX = "APPLOCK-RECOVERY:"

    fun createRecoveryQr(context: Context): Bitmap {
        val token = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encoded = Base64.encodeToString(token, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        context.getSharedPreferences("app_lock_prefs", Context.MODE_PRIVATE).edit()
            .putString(KEY_RECOVERY_TOKEN_HASH, sha256(encoded)).apply()
        val matrix = QRCodeWriter().encode(PREFIX + encoded, BarcodeFormat.QR_CODE, 900, 900, mapOf(EncodeHintType.MARGIN to 1))
        return Bitmap.createBitmap(900, 900, Bitmap.Config.ARGB_8888).also { bitmap ->
            for (x in 0 until 900) for (y in 0 until 900) bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }

    fun isValid(context: Context, rawValue: String?): Boolean {
        val token = rawValue?.removePrefix(PREFIX) ?: return false
        if (token == rawValue) return false
        val stored = context.getSharedPreferences("app_lock_prefs", Context.MODE_PRIVATE).getString(KEY_RECOVERY_TOKEN_HASH, null)
        return stored != null && constantTimeEquals(stored, sha256(token))
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (index in a.indices) diff = diff or (a[index].code xor b[index].code)
        return diff == 0
    }
}
