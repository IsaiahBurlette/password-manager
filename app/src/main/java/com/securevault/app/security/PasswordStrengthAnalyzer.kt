package com.securevault.app.security

import kotlin.math.log2
import kotlin.math.pow

enum class StrengthLabel(val label: String) {
    VERY_WEAK("Very Weak"),
    WEAK("Weak"),
    FAIR("Fair"),
    STRONG("Strong"),
    VERY_STRONG("Very Strong")
}

data class CrackTimeEstimate(
    val scenario: String,
    val guessesPerSecond: Double,
    val displayTime: String
)

data class PasswordStrengthResult(
    val entropyBits: Double,
    val score: Int,
    val label: StrengthLabel,
    val warnings: List<String>,
    val crackTimes: List<CrackTimeEstimate>
)

/**
 * Lightweight, dependency-free strength estimator: pool-size entropy with
 * penalties for common passwords / runs / repeats, then translated into
 * brute-force crack-time estimates under a few attacker scenarios.
 */
object PasswordStrengthAnalyzer {

    private val COMMON_PASSWORDS = setOf(
        "123456", "password", "12345678", "qwerty", "123456789", "12345",
        "1234", "111111", "1234567", "dragon", "123123", "baseball",
        "abc123", "football", "monkey", "letmein", "shadow", "master",
        "666666", "qwertyuiop", "123321", "mustang", "1234567890", "michael",
        "654321", "superman", "1qaz2wsx", "7777777", "121212", "000000",
        "qazwsx", "123qwe", "killer", "trustno1", "jennifer", "zxcvbnm",
        "asdfgh", "hunter", "buster", "soccer", "harley", "batman",
        "andrew", "tigger", "sunshine", "iloveyou", "fuckyou", "2000",
        "charlie", "robert", "thomas", "hockey", "ranger", "daniel",
        "starwars", "klaster", "112233", "george", "computer", "michelle",
        "jessica", "pepper", "1111", "zzzzzz", "ginger", "princess",
        "1qaz2wsx3edc", "password1", "admin", "welcome", "login", "passw0rd",
        "changeme", "letmein1", "freedom", "whatever", "qwerty123", "solo"
    )

    private const val SCENARIO_ONLINE = "Online attempt (throttled login, ~100 guesses/sec)"
    private const val SCENARIO_OFFLINE_SLOW = "Offline attack, salted+hashed (~10K guesses/sec)"
    private const val SCENARIO_OFFLINE_FAST = "Offline attack, GPU cluster (~10B guesses/sec)"

    fun analyze(password: String): PasswordStrengthResult {
        if (password.isEmpty()) {
            return PasswordStrengthResult(
                entropyBits = 0.0,
                score = 0,
                label = StrengthLabel.VERY_WEAK,
                warnings = listOf("Enter a password to analyze"),
                crackTimes = scenarios(0.0)
            )
        }

        val warnings = mutableListOf<String>()
        var poolSize = 0
        if (password.any { it.isLowerCase() }) poolSize += 26
        if (password.any { it.isUpperCase() }) poolSize += 26
        if (password.any { it.isDigit() }) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32
        if (poolSize == 0) poolSize = 26

        var entropyBits = password.length * log2(poolSize.toDouble())

        val lower = password.lowercase()
        if (COMMON_PASSWORDS.contains(lower)) {
            warnings.add("This is one of the most commonly used passwords in the world")
            entropyBits = minOf(entropyBits, 6.0)
        } else if (COMMON_PASSWORDS.any { lower.contains(it) && it.length >= 5 }) {
            warnings.add("Contains a common password or word")
            entropyBits *= 0.5
        }

        if (hasSequentialRun(lower, 4)) {
            warnings.add("Contains a sequential pattern (e.g. abcd, 1234)")
            entropyBits *= 0.7
        }

        if (hasRepeatedRun(password, 3)) {
            warnings.add("Contains repeated characters (e.g. aaa, 111)")
            entropyBits *= 0.7
        }

        if (password.length < 8) {
            warnings.add("Shorter than the recommended minimum of 8 characters")
        }

        entropyBits = entropyBits.coerceAtLeast(0.0)

        val label = labelFor(entropyBits)
        val score = scoreFor(entropyBits)

        return PasswordStrengthResult(
            entropyBits = entropyBits,
            score = score,
            label = label,
            warnings = warnings,
            crackTimes = scenarios(entropyBits)
        )
    }

    private fun labelFor(bits: Double): StrengthLabel = when {
        bits < 28 -> StrengthLabel.VERY_WEAK
        bits < 36 -> StrengthLabel.WEAK
        bits < 60 -> StrengthLabel.FAIR
        bits < 80 -> StrengthLabel.STRONG
        else -> StrengthLabel.VERY_STRONG
    }

    private fun scoreFor(bits: Double): Int = (bits / 128.0 * 100).coerceIn(0.0, 100.0).toInt()

    private fun scenarios(entropyBits: Double): List<CrackTimeEstimate> {
        val combinations = 2.0.pow(entropyBits)
        return listOf(
            Triple(SCENARIO_ONLINE, 100.0, combinations),
            Triple(SCENARIO_OFFLINE_SLOW, 1e4, combinations),
            Triple(SCENARIO_OFFLINE_FAST, 1e10, combinations)
        ).map { (scenario, rate, combos) ->
            val seconds = combos / rate / 2.0
            CrackTimeEstimate(scenario, rate, formatDuration(seconds))
        }
    }

    private fun hasSequentialRun(text: String, minRun: Int): Boolean {
        if (text.length < minRun) return false
        var ascRun = 1
        var descRun = 1
        for (i in 1 until text.length) {
            val prev = text[i - 1]
            val curr = text[i]
            if (curr.code == prev.code + 1) {
                ascRun++
                descRun = 1
            } else if (curr.code == prev.code - 1) {
                descRun++
                ascRun = 1
            } else {
                ascRun = 1
                descRun = 1
            }
            if (ascRun >= minRun || descRun >= minRun) return true
        }
        return false
    }

    private fun hasRepeatedRun(text: String, minRun: Int): Boolean {
        if (text.length < minRun) return false
        var run = 1
        for (i in 1 until text.length) {
            run = if (text[i] == text[i - 1]) run + 1 else 1
            if (run >= minRun) return true
        }
        return false
    }

    private fun formatDuration(seconds: Double): String {
        if (seconds.isNaN() || seconds.isInfinite()) return "essentially forever"
        if (seconds < 1) return "less than a second"

        val minute = 60.0
        val hour = 3600.0
        val day = 86400.0
        val year = 365.25 * day

        return when {
            seconds < minute -> "${seconds.toInt()} seconds"
            seconds < hour -> "${(seconds / minute).toInt()} minutes"
            seconds < day -> "${(seconds / hour).toInt()} hours"
            seconds < year -> "${(seconds / day).toInt()} days"
            seconds < year * 1e3 -> "${(seconds / year).toInt()} years"
            seconds < year * 1e6 -> "${(seconds / (year * 1e3)).toInt()} thousand years"
            seconds < year * 1e9 -> "${(seconds / (year * 1e6)).toInt()} million years"
            seconds < year * 1e12 -> "${(seconds / (year * 1e9)).toInt()} billion years"
            seconds < year * 1e15 -> "${(seconds / (year * 1e12)).toInt()} trillion years"
            else -> "trillions of trillions of years"
        }
    }
}
