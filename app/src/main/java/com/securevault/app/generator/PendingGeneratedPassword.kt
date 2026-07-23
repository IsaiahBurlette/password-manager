package com.securevault.app.generator

/** One-shot handoff so the generator's "Save to vault" action can prefill a new entry. */
object PendingGeneratedPassword {
    var value: String? = null

    fun consume(): String? {
        val current = value
        value = null
        return current
    }
}
