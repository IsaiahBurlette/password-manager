package com.securevault.app.crypto

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

/**
 * Stores the KDF salt, the password verifier blob, and the biometric-wrapped
 * master key. None of this is sensitive on its own: the verifier only proves
 * knowledge of the master password (protected by PBKDF2 + AES-GCM), and the
 * biometric wrap requires the Keystore-held key to unwrap.
 */
class VaultPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)

    var isInitialized: Boolean
        get() = prefs.contains(KEY_SALT)
        set(value) { prefs.edit().putBoolean(KEY_INITIALIZED, value).apply() }

    var kdfSalt: ByteArray?
        get() = prefs.getString(KEY_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
        set(value) { prefs.edit().putString(KEY_SALT, value?.let { Base64.encodeToString(it, Base64.NO_WRAP) }).apply() }

    var verifierIv: ByteArray?
        get() = prefs.getString(KEY_VERIFIER_IV, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
        set(value) { prefs.edit().putString(KEY_VERIFIER_IV, value?.let { Base64.encodeToString(it, Base64.NO_WRAP) }).apply() }

    var verifierCipherText: ByteArray?
        get() = prefs.getString(KEY_VERIFIER_CT, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
        set(value) { prefs.edit().putString(KEY_VERIFIER_CT, value?.let { Base64.encodeToString(it, Base64.NO_WRAP) }).apply() }

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIO_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_BIO_ENABLED, value).apply() }

    var biometricWrappedKeyIv: ByteArray?
        get() = prefs.getString(KEY_BIO_IV, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
        set(value) { prefs.edit().putString(KEY_BIO_IV, value?.let { Base64.encodeToString(it, Base64.NO_WRAP) }).apply() }

    var biometricWrappedKeyCipherText: ByteArray?
        get() = prefs.getString(KEY_BIO_CT, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
        set(value) { prefs.edit().putString(KEY_BIO_CT, value?.let { Base64.encodeToString(it, Base64.NO_WRAP) }).apply() }

    var autoLockSeconds: Int
        get() = prefs.getInt(KEY_AUTO_LOCK, 60)
        set(value) { prefs.edit().putInt(KEY_AUTO_LOCK, value).apply() }

    fun clearBiometric() {
        prefs.edit()
            .remove(KEY_BIO_ENABLED)
            .remove(KEY_BIO_IV)
            .remove(KEY_BIO_CT)
            .apply()
    }

    fun wipeAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_INITIALIZED = "initialized"
        private const val KEY_SALT = "kdf_salt"
        private const val KEY_VERIFIER_IV = "verifier_iv"
        private const val KEY_VERIFIER_CT = "verifier_ct"
        private const val KEY_BIO_ENABLED = "bio_enabled"
        private const val KEY_BIO_IV = "bio_iv"
        private const val KEY_BIO_CT = "bio_ct"
        private const val KEY_AUTO_LOCK = "auto_lock_seconds"
    }
}
