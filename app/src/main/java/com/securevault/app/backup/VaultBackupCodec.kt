package com.securevault.app.backup

import com.securevault.app.crypto.CryptoEngine
import com.securevault.app.data.DecryptedEntry
import java.util.Base64
import javax.crypto.AEADBadTagException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Portable, password-protected vault backup: a JSON envelope holding an
 * AES-GCM ciphertext of the entry list, keyed by PBKDF2(exportPassword, salt).
 * Deliberately independent of the device's own master key/salt so the file
 * is still readable after restoring onto a different phone.
 */
object VaultBackupCodec {
    private const val FORMAT_VERSION = 1

    sealed class DecodeResult {
        data class Success(val entries: List<DecryptedEntry>) : DecodeResult()
        object WrongPassword : DecodeResult()
        object InvalidFile : DecodeResult()
    }

    fun encode(entries: List<DecryptedEntry>, exportPassword: CharArray): ByteArray {
        val plainJson = JSONArray().apply {
            entries.forEach { entry ->
                put(
                    JSONObject().apply {
                        put("title", entry.title)
                        put("username", entry.username)
                        put("password", entry.password)
                        put("websiteUrl", entry.websiteUrl ?: "")
                        put("notes", entry.notes)
                        put("favorite", entry.isFavorite)
                    }
                )
            }
        }

        val salt = CryptoEngine.randomBytes(16)
        val key = CryptoEngine.deriveKey(exportPassword, salt)
        val encrypted = CryptoEngine.encrypt(key, plainJson.toString().toByteArray(Charsets.UTF_8))

        val envelope = JSONObject().apply {
            put("app", "SecureVault")
            put("version", FORMAT_VERSION)
            put("kdfIterations", CryptoEngine.PBKDF2_ITERATIONS)
            put("salt", Base64.getEncoder().encodeToString(salt))
            put("iv", Base64.getEncoder().encodeToString(encrypted.iv))
            put("ciphertext", Base64.getEncoder().encodeToString(encrypted.cipherText))
        }
        return envelope.toString(2).toByteArray(Charsets.UTF_8)
    }

    fun decode(fileBytes: ByteArray, exportPassword: CharArray): DecodeResult {
        val envelope = try {
            JSONObject(String(fileBytes, Charsets.UTF_8))
        } catch (e: JSONException) {
            return DecodeResult.InvalidFile
        }

        val salt: ByteArray
        val iv: ByteArray
        val cipherText: ByteArray
        val iterations: Int
        try {
            salt = Base64.getDecoder().decode(envelope.getString("salt"))
            iv = Base64.getDecoder().decode(envelope.getString("iv"))
            cipherText = Base64.getDecoder().decode(envelope.getString("ciphertext"))
            iterations = envelope.optInt("kdfIterations", CryptoEngine.PBKDF2_ITERATIONS)
        } catch (e: JSONException) {
            return DecodeResult.InvalidFile
        } catch (e: IllegalArgumentException) {
            return DecodeResult.InvalidFile
        }

        val key = CryptoEngine.deriveKey(exportPassword, salt, iterations)
        val plain = try {
            CryptoEngine.decrypt(key, iv, cipherText)
        } catch (e: AEADBadTagException) {
            return DecodeResult.WrongPassword
        } catch (e: Exception) {
            return DecodeResult.WrongPassword
        }

        return try {
            val array = JSONArray(String(plain, Charsets.UTF_8))
            val entries = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                DecryptedEntry(
                    id = 0,
                    title = obj.getString("title"),
                    username = obj.optString("username", ""),
                    password = obj.getString("password"),
                    websiteUrl = obj.optString("websiteUrl", "").takeIf { it.isNotBlank() },
                    notes = obj.optString("notes", ""),
                    isFavorite = obj.optBoolean("favorite", false),
                    updatedAt = 0
                )
            }
            DecodeResult.Success(entries)
        } catch (e: JSONException) {
            DecodeResult.InvalidFile
        }
    }
}
