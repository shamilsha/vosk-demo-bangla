package com.alphacephei.vosk

/**
 * Normalize strings for verification match (contractions, number words, punctuation). Extracted to reduce MainActivity.kt size.
 * Also handles expected text with [alt1|alt2] brackets: speak/display use first alternative, match accepts any alternative.
 */
object MatchNormalizer {

    private val BRACKET_ALTERNATES = Regex("""\[([^\]]+)\]""")

    /** Strip "$" from recognizer output when immediately followed by digits (e.g. "$150" or "cost $150 taka" -> "150" / "cost 150 taka"). Use for display only. */
    fun sanitizeSpokenTextForDisplay(s: String): String {
        return Regex("""\$\s*(?=\d)""").replace(s, "")
    }

    /** Text to speak and display: replace each [first|second|...] with the first alternative only. */
    fun textForSpeakAndDisplay(raw: String): String {
        return BRACKET_ALTERNATES.replace(raw) { match ->
            val inside = match.groupValues[1]
            val first = inside.substringBefore('|').trim()
            first
        }
    }

    /** True if normalized(said) equals any expansion of expected (each [a|b|c] replaced by a, b, or c). */
    fun matchesExpectedWithAlternates(expectedRaw: String, said: String): Boolean {
        val segments = parseSegments(expectedRaw) ?: return normalizeForMatch(said) == normalizeForMatch(expectedRaw)
        val normalizedSaid = normalizeForMatch(said)
        for (full in expandSegments(segments)) {
            if (normalizeForMatch(full) == normalizedSaid) return true
        }
        return false
    }

    private fun parseSegments(raw: String): List<Any>? {
        val segments = mutableListOf<Any>()
        var lastEnd = 0
        for (match in BRACKET_ALTERNATES.findAll(raw)) {
            if (match.range.first > lastEnd) segments.add(raw.substring(lastEnd, match.range.first))
            val inside = match.groupValues[1]
            val alts = inside.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            if (alts.isEmpty()) segments.add("")
            else segments.add(alts)
            lastEnd = match.range.last + 1
        }
        if (lastEnd < raw.length) segments.add(raw.substring(lastEnd))
        return if (segments.any { it is List<*> }) segments else null
    }

    @Suppress("UNCHECKED_CAST")
    private fun expandSegments(segments: List<Any>): List<String> {
        if (segments.isEmpty()) return listOf("")
        val rest = expandSegments(segments.drop(1))
        return when (val first = segments.first()) {
            is String -> rest.map { first + it }
            is List<*> -> (first as List<String>).flatMap { alt -> rest.map { alt + it } }
            else -> rest
        }
    }

    /** Normalize string for verification match: contractions, number words→digits, trim, lowercase, collapse spaces, remove punctuation. */
    fun normalizeForMatch(s: String): String {
        val lower = s.trim().lowercase()
            .replace('\u2019', '\'')
        val expanded = normalizeContractions(lower)
        return normalizeNumberWords(expanded)
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9 ]"), "")
    }

    /** Expand common contractions so "I'm" and "I am" match. Call after lowercasing. */
    fun normalizeContractions(s: String): String {
        var t = s
        val contractions = mapOf(
            "i'm" to "i am", "i've" to "i have", "i'd" to "i would", "i'll" to "i will",
            "that's" to "that is", "it's" to "it is", "what's" to "what is", "there's" to "there is",
            "here's" to "here is", "who's" to "who is",
            "don't" to "do not", "doesn't" to "does not", "didn't" to "did not",
            "isn't" to "is not", "aren't" to "are not", "wasn't" to "was not", "weren't" to "were not",
            "haven't" to "have not", "hasn't" to "has not", "hadn't" to "had not",
            "won't" to "will not", "wouldn't" to "would not", "can't" to "cannot",
            "couldn't" to "could not", "shouldn't" to "should not",
            "we're" to "we are", "they're" to "they are", "you're" to "you are",
            "we've" to "we have", "they've" to "they have", "you've" to "you have",
            "we'll" to "we will", "they'll" to "they will", "you'll" to "you will"
        )
        for ((contraction, expansion) in contractions) {
            t = t.replace(Regex("\\b${Regex.escape(contraction)}\\b"), expansion)
        }
        return t
    }

    /** Number words to digits so "sixty" and "60" match. */
    fun normalizeNumberWords(s: String): String {
        var t = s
        val numberWords = mapOf(
            "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
            "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
            "ten" to "10", "eleven" to "11", "twelve" to "12", "thirteen" to "13",
            "fourteen" to "14", "fifteen" to "15", "sixteen" to "16", "seventeen" to "17",
            "eighteen" to "18", "nineteen" to "19", "twenty" to "20", "thirty" to "30",
            "forty" to "40", "fifty" to "50", "sixty" to "60", "seventy" to "70",
            "eighty" to "80", "ninety" to "90", "hundred" to "100", "thousand" to "1000"
        )
        for ((word, digit) in numberWords) {
            t = t.replace(Regex("\\b$word\\b", RegexOption.IGNORE_CASE), digit)
        }
        return t
    }
}
