package com.securevault.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securevault.app.crypto.MasterKeyHolder
import com.securevault.app.crypto.VaultAuthManager
import com.securevault.app.data.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
}

class SettingsViewModelFactory(
    private val authManager: VaultAuthManager,
    private val repository: VaultRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(authManager, repository) as T
}
