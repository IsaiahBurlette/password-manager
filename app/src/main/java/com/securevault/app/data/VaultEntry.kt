package com.securevault.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stored row for a vault item. [password] and [notes] are only ever kept as
 * AES-GCM ciphertext + IV; [title]/[username]/[websiteUrl] stay in the clear
 * so the list can render and be searched without decrypting anything.
 */
@Entity(tableName = "vault_entries")
data class VaultEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val username: String,
    val websiteUrl: String?,
    val passwordIv: ByteArray,
    val passwordCipherText: ByteArray,
    val notesIv: ByteArray?,
    val notesCipherText: ByteArray?,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VaultEntry) return false
        return id == other.id &&
            title == other.title &&
            username == other.username &&
            websiteUrl == other.websiteUrl &&
            passwordIv.contentEquals(other.passwordIv) &&
            passwordCipherText.contentEquals(other.passwordCipherText) &&
            (notesIv?.contentEquals(other.notesIv ?: ByteArray(0)) ?: (other.notesIv == null)) &&
            isFavorite == other.isFavorite &&
            createdAt == other.createdAt &&
            updatedAt == other.updatedAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        return result
    }
}
