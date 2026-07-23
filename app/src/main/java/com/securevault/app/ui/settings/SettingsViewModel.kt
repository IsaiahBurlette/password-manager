package com.securevault.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securevault.app.backup.VaultBackupCodec
import com.securevault.app.crypto.MasterKeyHolder
import com.securevault.app.crypto.VaultAuthManager
import com.securevault.app.data.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ImportOutcome {
    data class Success(val count: Int) : ImportOutcome()
    object WrongPassword : ImportOutcome()
    object InvalidFile : ImportOutcome()
}

class SettingsViewModel(
    private val authManager: VaultAuthManager,
    private val repository: VaultRepository
) : ViewModel() {

    /** Rotates the master password and re-encrypts every stored entry under the new key. */
    fun changeMasterPassword(newPassword: CharArray, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val (oldKey, newKey) = authManager.beginPasswordChange(newPassword)
                repository.reencryptAll(oldKey, newKey)
                MasterKeyHolder.unlock(newKey)
            }
            onDone()
        }
    }

    /** Decrypts every entry and re-encrypts them into a portable backup file, protected by [exportPassword]. */
    fun exportVault(exportPassword: CharArray, onResult: (ByteArray) -> Unit) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.Default) {
                val entries = repository.exportAll()
                VaultBackupCodec.encode(entries, exportPassword)
            }
            onResult(bytes)
        }
    }

    /** Decodes a backup file with [exportPassword] and inserts its entries into this vault. */
    fun importVault(fileBytes: ByteArray, exportPassword: CharArray, onResult: (ImportOutcome) -> Unit) {
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.Default) {
                when (val result = VaultBackupCodec.decode(fileBytes, exportPassword)) {
                    is VaultBackupCodec.DecodeResult.Success -> {
                        repository.importEntries(result.entries)
                        ImportOutcome.Success(result.entries.size)
                    }
                    VaultBackupCodec.DecodeResult.WrongPassword -> ImportOutcome.WrongPassword
                    VaultBackupCodec.DecodeResult.InvalidFile -> ImportOutcome.InvalidFile
                }
            }
            onResult(outcome)
        }
    }
}

class SettingsViewModelFactory(
    private val authManager: VaultAuthManager,
    private val repository: VaultRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(authManager, repository) as T
}
