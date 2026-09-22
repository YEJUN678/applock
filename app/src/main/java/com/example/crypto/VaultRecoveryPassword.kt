package com.example.crypto

/** Process-only recovery password. It is deliberately never saved on the device. */
object VaultRecoveryPassword {
    @Volatile private var value: CharArray? = null

    fun set(password: CharArray) {
        require(password.size >= 8) { "복구 비밀번호는 8자 이상이어야 합니다." }
        value?.fill('\u0000')
        value = password.copyOf()
    }

    fun require(): CharArray = value?.copyOf()
        ?: throw IllegalStateException("금고 복구 비밀번호를 먼저 입력하세요.")

    fun isSet(): Boolean = value != null
    fun clear() { value?.fill('\u0000'); value = null }
}
