package com.securevault.app.crypto

import android.content.Context
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Orchestrates master-password setup/verification and optional biometric
 * quick-unlock on top of [CryptoEngine], [VaultPrefs] and [BiometricKeyManager].
 */
class VaultAuthManager(context: Context) {
    private val prefs = VaultPrefs(context.applicationContext)
    private val biometricKeyManager = BiometricKeyManager()

    private val verifierPlainText = "SECUREVAULT_OK".toByteArray(Charsets.UTF_8)

    val isVaultSetUp: Boolean get() = prefs.isInitialized
    val isBiometricEnabled: Boolean get() = prefs.biometricEnabled
    var autoLockSeconds: Int
        get() = prefs.autoLockSeconds
        set(value) { prefs.autoLockSeconds = value }

    /** Creates a brand-new vault protected by [password]. Unlocks it immediately. */
    fun createMasterPassword(password: CharArray) {
        val salt = CryptoEngine.randomBytes(16)
        val key = CryptoEngine.deriveKey(password, salt)
        val encrypted = CryptoEngine.encrypt(key, verifierPlainText)

        prefs.kdfSalt = salt
        prefs.verifierIv = encrypted.iv
        prefs.verifierCipherText = encrypted.cipherText
        prefs.isInitialized = true

        MasterKeyHolder.unlock(key)
    }

    /** Returns true and unlocks the session if [password] matches the stored vault. */
    fun verifyMasterPassword(password: CharArray): Boolean {
        val salt = prefs.kdfSalt ?: return false
        val iv = prefs.verifierIv ?: return false
        val cipherText = prefs.verifierCipherText ?: return false

        val key = CryptoEngine.deriveKey(password, salt)
        return try {
            val decrypted = CryptoEngine.decrypt(key, iv, cipherText)
            if (decrypted.contentEquals(verifierPlainText)) {
                MasterKeyHolder.unlock(key)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Rotates the KDF salt/verifier to [newPassword] and returns (oldKey, newKey).
     * The caller must re-encrypt every stored entry with the new key (see
     * VaultRepository.reencryptAll) and only then call [MasterKeyHolder.unlock]
     * with the new key -- otherwise existing entries become unreadable.
     */
    fun beginPasswordChange(newPassword: CharArray): Pair<SecretKey, SecretKey> {
        val oldKey = MasterKeyHolder.requireKey()

        val salt = CryptoEngine.randomBytes(16)
        val newKey = CryptoEngine.deriveKey(newPassword, salt)
        val encrypted = CryptoEngine.encrypt(newKey, verifierPlainText)

        prefs.kdfSalt = salt
        prefs.verifierIv = encrypted.iv
        prefs.verifierCipherText = encrypted.cipherText
        // Master key changed: any previous biometric wrap is now stale.
        disableBiometric()

        return oldKey to newKey
    }

    fun lock() {
        MasterKeyHolder.lock()
    }

    fun wipeVault() {
        prefs.wipeAll()
        biometricKeyManager.removeKey()
        MasterKeyHolder.lock()
    }

    // --- Biometric quick-unlock ---

    /** Cipher the caller must run through BiometricPrompt to enable quick-unlock. */
    fun biometricEnrollCipher(): Cipher = biometricKeyManager.encryptCipher()

    /** Call after the BiometricPrompt succeeds using [biometricEnrollCipher]. */
    fun finishBiometricEnroll(authenticatedCipher: Cipher) {
        val sessionKey = MasterKeyHolder.requireKey()
        val wrapped = authenticatedCipher.doFinal(sessionKey.encoded)
        prefs.biometricWrappedKeyIv = authenticatedCipher.iv
        prefs.biometricWrappedKeyCipherText = wrapped
        prefs.biometricEnabled = true
    }

    fun disableBiometric() {
        prefs.clearBiometric()
        biometricKeyManager.removeKey()
    }

    /** Cipher the caller must run through BiometricPrompt to unlock via biometrics. */
    fun biometricUnlockCipher(): Cipher? {
        val iv = prefs.biometricWrappedKeyIv ?: return null
        return biometricKeyManager.decryptCipher(iv)
    }

    /** Call after the BiometricPrompt succeeds using [biometricUnlockCipher]. */
    fun finishBiometricUnlock(authenticatedCipher: Cipher): Boolean {
        val wrapped = prefs.biometricWrappedKeyCipherText ?: return false
        return try {
            val keyBytes = authenticatedCipher.doFinal(wrapped)
            MasterKeyHolder.unlock(SecretKeySpec(keyBytes, "AES"))
            true
        } catch (e: Exception) {
            false
        }
    }
}
