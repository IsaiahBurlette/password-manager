package com.securevault.app.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securevault.app.data.DecryptedEntry
import com.securevault.app.data.VaultEntry
import com.securevault.app.data.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(private val repository: VaultRepository) : ViewModel() {
    private val query = MutableStateFlow("")

    val entries: StateFlow<List<VaultEntry>> = combine(repository.observeEntries(), query) { list, q ->
        if (q.isBlank()) {
            list
        } else {
            list.filter {
                it.title.contains(q, ignoreCase = true) || it.username.contains(q, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery: StateFlow<String> = query

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleFavorite(entry: VaultEntry) {
        viewModelScope.launch { repository.toggleFavorite(entry) }
    }

    fun delete(entry: VaultEntry) {
        viewModelScope.launch { repository.delete(entry) }
    }

    fun deleteById(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteById(id)
            onDone()
        }
    }

    suspend fun loadDecrypted(id: Long): DecryptedEntry? = repository.getDecrypted(id)

    fun save(
        id: Long?,
        title: String,
        username: String,
        password: String,
        websiteUrl: String?,
        notes: String,
        isFavorite: Boolean,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.save(id, title, username, password, websiteUrl, notes, isFavorite)
            onDone()
        }
    }
}

class VaultViewModelFactory(private val repository: VaultRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = VaultViewModel(repository) as T
}
