package com.tagaev.trrcrm.data.accounts

import kotlin.random.Random
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString

internal object AccountPinCrypto {
    fun newAccountId(): String = "acc_" + randomHex(8)

    fun newSalt(): String = randomHex(16)

    fun hashPin(pin: String, salt: String): String =
        "$salt:$pin".encodeUtf8().sha256().hex()

    fun isFourDigitPin(pin: String): Boolean =
        pin.length == ACCOUNT_PIN_LENGTH && pin.all { it.isDigit() }

    private fun randomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        Random.nextBytes(bytes)
        return bytes.toByteString().hex()
    }
}
