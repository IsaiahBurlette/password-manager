package com.securevault.app.ui.components

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService

/**
 * Copies [text] to the clipboard, marking it sensitive on API 33+ so it's
 * excluded from clipboard history/previews, and wipes it again after
 * [clearAfterMs] so a copied password doesn't linger.
 */
fun copySensitiveToClipboard(context: Context, label: String, text: String, clearAfterMs: Long = 30_000) {
    val clipboard = context.getSystemService<android.content.ClipboardManager>() ?: return
    val clip = ClipData.newPlainText(label, text)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundleCompat.sensitiveExtras()
    }
    clipboard.setPrimaryClip(clip)

    Handler(Looper.getMainLooper()).postDelayed({
        val current = clipboard.primaryClip
        val currentText = current?.getItemAt(0)?.coerceToText(context)?.toString()
        if (currentText == text) {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }, clearAfterMs)
}

private object PersistableBundleCompat {
    fun sensitiveExtras(): android.os.PersistableBundle {
        val bundle = android.os.PersistableBundle()
        bundle.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        return bundle
    }
}
