package com.alphacephei.vosk

/**
 * Simple string helpers. Extracted to reduce MainActivity.kt size.
 */
object StringUtils {

    /** Suffix for saved sentence list files (used with sanitizeListName). */
    const val LIST_FILE_SUFFIX = ".json"

    /** For "a|b|c" return first alternative only (for display/TTS). */
    fun firstAlternative(s: String): String = s.split("|").firstOrNull()?.trim() ?: s

    /** Strip [placeholder] to placeholder for display (e.g. "[Riya]" → "Riya"). */
    fun stripPlaceholderBracketsForDisplay(s: String): String =
        s.replace(Regex("\\[([^]]+)]"), "$1")

    /** Sanitize a list name for use as filename (alphanumeric and underscore only). */
    fun sanitizeListName(name: String): String {
        val s = name.trim().replace(Regex("[^A-Za-z0-9_]+"), "_")
        return if (s.isEmpty()) "list" else s
    }

    /**
     * Split Bengali text into small segments for natural reading and fine-grained pause/resume.
     * Splits on: | (pipe), । ৷ (Bengali full stops), newlines, period+space, commas, semicolons.
     */
    fun splitIntroductionSegments(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val segments = mutableListOf<String>()
        val majorParts = text.split(Regex("[|।৷]|\\n+|\\.\\s+"))
        for (major in majorParts) {
            val trimmed = major.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.length < 60) {
                segments.add(trimmed)
            } else {
                val subParts = trimmed.split(Regex("(?<=[,;،])|(?<=[,;،])\\s+"))
                for (sub in subParts) {
                    val s = sub.trim()
                    if (s.isNotEmpty()) segments.add(s)
                }
            }
        }
        if (segments.isEmpty()) segments.add(text.trim())
        return segments
    }

    /** Escape text for safe use inside HTML body (e.g. intro text). */
    fun escapeForHtmlBody(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")

    /** Wrap body text in a minimal HTML document for WebView (intro style). */
    fun makeIntroductionHtml(bodyText: String): String {
        val escaped = escapeForHtmlBody(bodyText)
        return """
<!DOCTYPE html>
<html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="margin:0;padding:12px;font-family:sans-serif;background:transparent;font-size:16px;line-height:1.6;color:#111;">
<div style="white-space:pre-wrap;word-wrap:break-word;">$escaped</div>
</body></html>"""
    }
}
