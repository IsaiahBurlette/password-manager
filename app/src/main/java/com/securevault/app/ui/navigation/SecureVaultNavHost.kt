package com.securevault.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.securevault.app.crypto.AutoLockTracker
import com.securevault.app.crypto.MasterKeyHolder
import com.securevault.app.ui.generator.GeneratorScreen
import com.securevault.app.ui.onboarding.CreateMasterPasswordScreen
import com.securevault.app.ui.settings.SettingsScreen
import com.securevault.app.ui.unlock.UnlockScreen
import com.securevault.app.ui.vault.EntryEditScreen
import com.securevault.app.ui.vault.VaultListScreen
import com.securevault.app.ui.vault.VaultViewModel
import com.securevault.app.ui.vault.VaultViewModelFactory
import com.securevault.app.vaultApp

@Composable
fun SecureVaultNavHost() {
    val context = LocalContext.current
    val app = remember { context.vaultApp() }
    val rootNavController = rememberNavController()

    val startDestination = if (app.authManager.isVaultSetUp) Routes.UNLOCK else Routes.ONBOARDING

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> AutoLockTracker.onBackground()
                Lifecycle.Event.ON_START -> {
                    if (MasterKeyHolder.isUnlocked && AutoLockTracker.shouldLock(app.authManager.autoLockSeconds)) {
                        app.authManager.lock()
                        rootNavController.navigate(Routes.UNLOCK) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    AutoLockTracker.onForeground()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NavHost(navController = rootNavController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            CreateMasterPasswordScreen {
                rootNavController.navigate(Routes.MAIN) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            }
        }
        composable(Routes.UNLOCK) {
            UnlockScreen {
                rootNavController.navigate(Routes.MAIN) {
                    popUpTo(Routes.UNLOCK) { inclusive = true }
                }
            }
        }
        composable(Routes.MAIN) {
            MainScaffold(
                onLock = {
                    app.authManager.lock()
                    rootNavController.navigate(Routes.UNLOCK) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onVaultWiped = {
                    rootNavController.navigate(Routes.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
private fun MainScaffold(onLock: () -> Unit, onVaultWiped: () -> Unit) {
    val context = LocalContext.current
    val app = remember { context.vaultApp() }
    val innerNavController = rememberNavController()

    val vaultViewModel: VaultViewModel = viewModel(factory = remember { VaultViewModelFactory(app.repository) })

    val backStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute == Routes.VAULT_LIST || currentRoute == Routes.GENERATOR || currentRoute == Routes.SETTINGS) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.VAULT_LIST,
                        onClick = { navigateToTab(innerNavController, Routes.VAULT_LIST) },
                        icon = { Icon(Icons.Filled.Shield, contentDescription = "Vault") },
                        label = { Text("Vault") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.GENERATOR,
                        onClick = { navigateToTab(innerNavController, Routes.GENERATOR) },
                        icon = { Icon(Icons.Filled.Casino, contentDescription = "Generator") },
                        label = { Text("Generator") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = { navigateToTab(innerNavController, Routes.SETTINGS) },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = innerNavController,
            startDestination = Routes.VAULT_LIST,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.VAULT_LIST) {
                val entries by vaultViewModel.entries.collectAsState()
                val query by vaultViewModel.searchQuery.collectAsState()
                VaultListScreen(
                    entries = entries,
                    query = query,
                    onQueryChange = vaultViewModel::setQuery,
                    onEntryClick = { entry -> innerNavController.navigate(Routes.entryEdit(entry.id)) },
                    onToggleFavorite = vaultViewModel::toggleFavorite
                )
            }
            composable(Routes.GENERATOR) {
                GeneratorScreen(onSaveToVault = { innerNavController.navigate(Routes.entryEdit(null)) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onVaultWiped = onVaultWiped)
            }
            composable(
                route = Routes.ENTRY_EDIT_PATTERN,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
            ) { backStack ->
                val id = backStack.arguments?.getLong("id") ?: -1L
                EntryEditScreen(
                    entryId = if (id == -1L) null else id,
                    viewModel = vaultViewModel,
                    onBack = { innerNavController.popBackStack() },
                    onSaved = { innerNavController.popBackStack() },
                    onDeleted = { innerNavController.popBackStack() }
                )
            }
        }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
