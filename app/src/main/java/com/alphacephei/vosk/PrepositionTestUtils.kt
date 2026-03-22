package com.alphacephei.vosk

/**
 * TEST-tab behaviour for the Prepositions 3-col lesson only: show/speak a cue, verify the full phrase.
 * Example: "Abide by" → display "Abide __", TTS speaks only "Abide".
 */
object PrepositionTestUtils {

    /** "Abide by" → "Abide __" (first word + " __"). No Bengali hint in UI. */
    fun maskedDisplayEnglish(canonicalEnglish: String): String {
        val t = canonicalEnglish.trim()
        if (t.isEmpty()) return ""
        val first = t.split(Regex("\\s+")).firstOrNull() ?: t
        return "$first __"
    }

    /** First whitespace-delimited word for TTS (the partial cue). */
    fun partialForTts(canonicalEnglish: String): String {
        val t = canonicalEnglish.trim()
        if (t.isEmpty()) return ""
        return t.split(Regex("\\s+")).firstOrNull() ?: t
    }
}
