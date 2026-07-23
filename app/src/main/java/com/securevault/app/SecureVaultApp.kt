package com.securevault.app

import android.app.Application
import android.content.Context
import com.securevault.app.crypto.VaultAuthManager
import com.securevault.app.data.AppDatabase
import com.securevault.app.data.VaultRepository

class SecureVaultApp : Application() {
    lateinit var authManager: VaultAuthManager
        private set
    lateinit var repository: VaultRepository
        private set

    override fun onCreate() {
        super.onCreate()
        authManager = VaultAuthManager(this)
        repository = VaultRepository(AppDatabase.getInstance(this).vaultDao())
    }
}

fun Context.vaultApp(): SecureVaultApp = applicationContext as SecureVaultApp
