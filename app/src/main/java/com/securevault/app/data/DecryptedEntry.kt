package com.securevault.app.data

/** Fully decrypted view of a [VaultEntry], only ever held in memory while the vault is unlocked. */
data class DecryptedEntry(
    val id: Long,
    val title: String,
    val username: String,
    val password: String,
    val websiteUrl: String?,
    val notes: String,
    val isFavorite: Boolean,
    val updatedAt: Long
)
