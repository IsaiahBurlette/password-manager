package com.securevault.app.data

import com.securevault.app.crypto.CryptoEngine
import com.securevault.app.crypto.MasterKeyHolder
import javax.crypto.SecretKey
import kotlinx.coroutines.flow.Flow

class VaultRepository(private val dao: VaultDao) {

    fun observeEntries(): Flow<List<VaultEntry>> = dao.observeAll()

    suspend fun getDecrypted(id: Long): DecryptedEntry? {
        val entry = dao.getById(id) ?: return null
        return decrypt(entry)
    }

    suspend fun save(
        id: Long?,
        title: String,
        username: String,
        password: String,
        websiteUrl: String?,
        notes: String,
        isFavorite: Boolean
    ) {
        val key = MasterKeyHolder.requireKey()
        val encryptedPassword = CryptoEngine.encrypt(key, password.toByteArray(Charsets.UTF_8))
        val encryptedNotes = if (notes.isNotEmpty()) {
            CryptoEngine.encrypt(key, notes.toByteArray(Charsets.UTF_8))
        } else null
        val now = System.currentTimeMillis()

        if (id == null) {
            dao.insert(
                VaultEntry(
                    title = title,
                    username = username,
                    websiteUrl = websiteUrl?.takeIf { it.isNotBlank() },
                    passwordIv = encryptedPassword.iv,
                    passwordCipherText = encryptedPassword.cipherText,
                    notesIv = encryptedNotes?.iv,
                    notesCipherText = encryptedNotes?.cipherText,
                    isFavorite = isFavorite,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            val existing = dao.getById(id) ?: return
            dao.update(
                existing.copy(
                    title = title,
                    username = username,
                    websiteUrl = websiteUrl?.takeIf { it.isNotBlank() },
                    passwordIv = encryptedPassword.iv,
                    passwordCipherText = encryptedPassword.cipherText,
                    notesIv = encryptedNotes?.iv,
                    notesCipherText = encryptedNotes?.cipherText,
                    isFavorite = isFavorite,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun toggleFavorite(entry: VaultEntry) {
        dao.update(entry.copy(isFavorite = !entry.isFavorite))
    }

    suspend fun delete(entry: VaultEntry) {
        dao.delete(entry)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    /** Re-encrypts every stored entry under [newKey] after a master password change. */
    suspend fun reencryptAll(oldKey: SecretKey, newKey: SecretKey) {
        dao.getAllOnce().forEach { entry ->
            val passwordPlain = CryptoEngine.decrypt(oldKey, entry.passwordIv, entry.passwordCipherText)
            val newPassword = CryptoEngine.encrypt(newKey, passwordPlain)
            val newNotes = if (entry.notesIv != null && entry.notesCipherText != null) {
                val notesPlain = CryptoEngine.decrypt(oldKey, entry.notesIv, entry.notesCipherText)
                CryptoEngine.encrypt(newKey, notesPlain)
            } else null
            dao.update(
                entry.copy(
                    passwordIv = newPassword.iv,
                    passwordCipherText = newPassword.cipherText,
                    notesIv = newNotes?.iv,
                    notesCipherText = newNotes?.cipherText
                )
            )
        }
    }

    suspend fun decryptPassword(entry: VaultEntry): String {
        val key = MasterKeyHolder.requireKey()
        val plain = CryptoEngine.decrypt(key, entry.passwordIv, entry.passwordCipherText)
        return String(plain, Charsets.UTF_8)
    }

    /** Decrypts every stored entry, for building a portable backup file. */
    suspend fun exportAll(): List<DecryptedEntry> = dao.getAllOnce().map { decrypt(it) }

    /** Inserts [entries] as new rows, encrypted under the current session key. */
    suspend fun importEntries(entries: List<DecryptedEntry>) {
        entries.forEach { entry ->
            save(
                id = null,
                title = entry.title,
                username = entry.username,
                password = entry.password,
                websiteUrl = entry.websiteUrl,
                notes = entry.notes,
                isFavorite = entry.isFavorite
            )
        }
    }

    private fun decrypt(entry: VaultEntry): DecryptedEntry {
        val key = MasterKeyHolder.requireKey()
        val password = String(
            CryptoEngine.decrypt(key, entry.passwordIv, entry.passwordCipherText),
            Charsets.UTF_8
        )
        val notes = if (entry.notesIv != null && entry.notesCipherText != null) {
            String(CryptoEngine.decrypt(key, entry.notesIv, entry.notesCipherText), Charsets.UTF_8)
        } else ""
        return DecryptedEntry(
            id = entry.id,
            title = entry.title,
            username = entry.username,
            password = password,
            websiteUrl = entry.websiteUrl,
            notes = notes,
            isFavorite = entry.isFavorite,
            updatedAt = entry.updatedAt
        )
    }
}
