package com.alphacephei.vosk

/**
 * Match spoken text to expected (alternatives, placeholders, fuzzy similarity). Extracted to reduce MainActivity.kt size.
 */
object SentenceMatchUtils {

    /** Levenshtein edit distance between two strings (for fuzzy sentence match). */
    fun editDistance(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val curr = IntArray(b.length + 1)
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
            }
            prev = curr
        }
        return prev[b.length]
    }

    /** True if two normalized strings are similar enough for pronunciation variance (e.g. "i eat rise" vs "i eat rice"). */
    fun spokenSentenceSimilarity(normalizedSpoken: String, normalizedExpected: String): Boolean {
        if (normalizedSpoken.isEmpty() || normalizedExpected.isEmpty()) return false
        val maxLen = maxOf(normalizedSpoken.length, normalizedExpected.length)
        val dist = editDistance(normalizedSpoken, normalizedExpected)
        if (maxLen <= 8) return dist <= 1
        if (maxLen < 4) return false
        val similarity = 1.0 - dist.toDouble() / maxLen
        return similarity >= 0.88
    }

    /** Regex: token is a placeholder like [Riya] (brackets with content inside). */
    val placeholderTokenRegex = Regex("^\\[.+\\]$")

    /**
     * Parse expected into tokens. Tokens like [Riya] are placeholders (accept any one word when matching).
     */
    fun parseExpectedTokens(expected: String): List<String> {
        return expected.split(Regex("\\s+"))
            .map { token ->
                token.replace(Regex("^[.,!?;:\"']+|[.,!?;:\"']+$"), "").trim()
            }
            .filter { it.isNotEmpty() }
    }

    /** True if a single spoken word matches the expected token (normalized equality or similarity). */
    fun singleWordMatches(spokenWord: String, expectedToken: String): Boolean {
        val s = MatchNormalizer.normalizeForMatch(spokenWord)
        val e = MatchNormalizer.normalizeForMatch(expectedToken)
        if (s.isEmpty()) return false
        return s == e || spokenSentenceSimilarity(s, e)
    }

    /**
     * Match spoken to expected when expected may contain [placeholder] tokens (e.g. [Riya]).
     * Placeholders match any one word; other tokens must match normally.
     */
    fun matchesExpectedWithPlaceholders(spoken: String, expected: String): Boolean {
        val spokenWords = MatchNormalizer.normalizeForMatch(spoken).split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (spokenWords.isEmpty()) return false
        val tokens = parseExpectedTokens(expected)
        if (tokens.isEmpty()) return false
        var j = 0
        for (token in tokens) {
            if (j >= spokenWords.size) return false
            val isPlaceholder = placeholderTokenRegex.matches(token)
            if (isPlaceholder) {
                j++
            } else {
                if (!singleWordMatches(spokenWords[j], token)) return false
                j++
            }
        }
        return true
    }

    /**
     * Match spoken to expected; expected can contain alternatives separated by | or /.
     * @param strict When true, do not accept "expected contains spoken" or "spoken contains expected" — only exact or high similarity.
     */
    fun matchesExpectedWithAlternatives(spoken: String, expected: String, strict: Boolean = false): Boolean {
        val normalizedSpoken = MatchNormalizer.normalizeForMatch(spoken)
        if (normalizedSpoken.isEmpty()) return false
        if (expected.contains("[") && expected.contains("]")) {
            val alternatives = expected.split("|", "/", "অথবা").map { it.trim() }.filter { it.isNotEmpty() }
            val toTry = if (alternatives.isEmpty()) listOf(expected) else alternatives
            if (toTry.any { it.contains("[") && matchesExpectedWithPlaceholders(spoken, it) }) return true
        }
        val alternatives = expected.split("|", "/", "অথবা")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (alternatives.isEmpty()) {
            val normExpected = MatchNormalizer.normalizeForMatch(expected)
            return normalizedSpoken == normExpected || spokenSentenceSimilarity(normalizedSpoken, normExpected)
        }
        return alternatives.any { alt ->
            val normAlt = MatchNormalizer.normalizeForMatch(alt)
            when {
                strict -> normAlt == normalizedSpoken || spokenSentenceSimilarity(normalizedSpoken, normAlt)
                else -> normAlt == normalizedSpoken ||
                    normalizedSpoken.contains(normAlt) ||
                    normAlt.contains(normalizedSpoken) ||
                    spokenSentenceSimilarity(normalizedSpoken, normAlt)
            }
        }
    }
}
