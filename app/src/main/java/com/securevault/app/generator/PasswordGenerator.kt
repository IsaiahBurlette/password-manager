package com.securevault.app.generator

import java.security.SecureRandom

object PasswordGenerator {

    data class Options(
        val length: Int,
        val useUpper: Boolean = true,
        val useLower: Boolean = true,
        val useNumbers: Boolean = true,
        val useSymbols: Boolean = true,
        val excludeAmbiguous: Boolean = false
    )

    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val NUMBERS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>/?"
    private const val AMBIGUOUS = "0O1lI|"

    private val secureRandom = SecureRandom()

    fun generate(options: Options): String {
        val pools = buildList {
            if (options.useUpper) add(filterAmbiguous(UPPER, options.excludeAmbiguous))
            if (options.useLower) add(filterAmbiguous(LOWER, options.excludeAmbiguous))
            if (options.useNumbers) add(filterAmbiguous(NUMBERS, options.excludeAmbiguous))
            if (options.useSymbols) add(filterAmbiguous(SYMBOLS, options.excludeAmbiguous))
        }.filter { it.isNotEmpty() }

        if (pools.isEmpty()) return ""

        val length = options.length.coerceIn(pools.size, 128)
        val combinedPool = pools.joinToString("")

        val result = CharArray(length)

        // Guarantee at least one character from every selected category first.
        pools.forEachIndexed { index, pool ->
            result[index] = pool[secureRandom.nextInt(pool.length)]
        }
        for (i in pools.size until length) {
            result[i] = combinedPool[secureRandom.nextInt(combinedPool.length)]
        }

        // Fisher-Yates shuffle so the guaranteed chars aren't always up front.
        for (i in result.size - 1 downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val tmp = result[i]
            result[i] = result[j]
            result[j] = tmp
        }

        return String(result)
    }

    /** Size of the character pool implied by [options]; used for entropy estimation. */
    fun poolSize(options: Options): Int {
        var size = 0
        if (options.useUpper) size += filterAmbiguous(UPPER, options.excludeAmbiguous).length
        if (options.useLower) size += filterAmbiguous(LOWER, options.excludeAmbiguous).length
        if (options.useNumbers) size += filterAmbiguous(NUMBERS, options.excludeAmbiguous).length
        if (options.useSymbols) size += filterAmbiguous(SYMBOLS, options.excludeAmbiguous).length
        return size
    }

    private fun filterAmbiguous(pool: String, exclude: Boolean): String =
        if (exclude) pool.filterNot { AMBIGUOUS.contains(it) } else pool
}
