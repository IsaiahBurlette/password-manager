package com.securevault.app.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val UNLOCK = "unlock"
    const val MAIN = "main"

    const val VAULT_LIST = "vault_list"
    const val GENERATOR = "generator"
    const val SETTINGS = "settings"
    const val ENTRY_EDIT = "entry_edit"

    fun entryEdit(id: Long? = null) = "$ENTRY_EDIT?id=${id ?: -1L}"
    const val ENTRY_EDIT_PATTERN = "$ENTRY_EDIT?id={id}"
}
