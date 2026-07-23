package com.securevault.app.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Low level AES-GCM + PBKDF2 primitives shared by the auth and vault-entry layers. */
object CryptoEngine {
    const val PBKDF2_ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = PBKDF2_ITERATIONS): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val secretBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(secretBytes, "AES")
    }

    data class Encrypted(val iv: ByteArray, val cipherText: ByteArray)

    fun encrypt(key: SecretKey, plainText: ByteArray): Encrypted {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = randomBytes(GCM_IV_BYTES)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return Encrypted(iv, cipher.doFinal(plainText))
    }

    fun decrypt(key: SecretKey, iv: ByteArray, cipherText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherText)
    }
}
