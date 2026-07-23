package com.securevault.app.crypto

import javax.crypto.SecretKey

/**
 * Holds the derived master AES key in memory only while the vault is unlocked.
 * Nothing here ever touches disk; [lock] drops the reference so entry data
 * becomes unreadable again until the user re-authenticates.
 */
object MasterKeyHolder {
    var sessionKey: SecretKey? = null
        private set

    val isUnlocked: Boolean
        get() = sessionKey != null

    fun unlock(key: SecretKey) {
        sessionKey = key
    }

    fun lock() {
        sessionKey = null
    }

    fun requireKey(): SecretKey =
        sessionKey ?: throw IllegalStateException("Vault is locked")
}
