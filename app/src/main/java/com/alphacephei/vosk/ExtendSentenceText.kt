package com.alphacephei.vosk

import androidx.core.text.HtmlCompat

/**
 * English column in extend-sentence `.txt` files may use **HTML-like markup** for the UI only.
 * TTS uses plain text (tags stripped).
 *
 * ### Supported (Android [HtmlCompat] subset)
 * - **Color:** `<font color="#1565C0">Subject</font>` — hex with `#` (6 digits)
 * - **Bold / italic / underline:** `<b>`, `<i>`, `<u>`
 * - Combine: `<font color="#C62828"><b>Verb</b></font>`
 *
 * ### Escaping (required if you use these characters in plain text)
 * - `&` → `&amp;` — e.g. `5 &amp; 10`
 * - `<` → `&lt;` (if not starting a tag)
 *
 * ### Example line in `.txt` (first comma-separated field)
 * ```
 * I<font color="#1565C0"> read</font> a book,আমি একটি বই পড়ি,আই রীড আ বুক,…
 * ```
 */
object ExtendSentenceText {

    /** Rich text for [TextView] (colors, bold, …). */
    fun englishToSpanned(english: String): CharSequence {
        val s = english.trim()
        if (!s.contains('<')) return s
        return HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    /** Plain string for spoken English (no tags). */
    fun englishPlainForSpeech(english: String): String {
        val s = english.trim()
        if (!s.contains('<')) return s
        return HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }
}
