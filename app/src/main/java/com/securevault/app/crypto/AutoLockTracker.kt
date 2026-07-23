package com.securevault.app.crypto

/** Tracks how long the app has been backgrounded so it can auto-lock on return. */
object AutoLockTracker {
    private var backgroundedAt: Long? = null

    fun onBackground() {
        backgroundedAt = System.currentTimeMillis()
    }

    fun onForeground() {
        backgroundedAt = null
    }

    /** Never for negative timeouts; otherwise true once the elapsed background time exceeds it. */
    fun shouldLock(timeoutSeconds: Int): Boolean {
        val since = backgroundedAt ?: return false
        if (timeoutSeconds < 0) return false
        val elapsedSeconds = (System.currentTimeMillis() - since) / 1000
        return elapsedSeconds >= timeoutSeconds
    }
}
