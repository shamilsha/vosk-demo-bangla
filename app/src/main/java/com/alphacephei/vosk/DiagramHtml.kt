package com.alphacephei.vosk

/**
 * HTML for small diagrams (e.g. verb conjugation). Extracted to reduce MainActivity.kt size.
 */
object DiagramHtml {

    /** Compact HTML for verb conjugation diagram (central verb + arrows to I/You/We/They/He/She/It). Fits in description area. */
    fun makeVerbDiagramHtml(verb: String): String {
        val v = verb.uppercase().ifEmpty { "HAVE" }
        val third = when (v) {
            "HAVE" -> "has"
            "DO" -> "does"
            "GO" -> "goes"
            else -> "${v.lowercase()}s"
        }
        val phrases = listOf("I ${v.lowercase()}", "You ${v.lowercase()}", "We ${v.lowercase()}", "They ${v.lowercase()}", "He $third", "She $third", "It $third")
        val color = "#c2185b"
        val ys = listOf(15, 28, 41, 54, 67, 80, 93)
        val arrowLines = ys.joinToString("") { y -> "<line x1=\"64\" y1=\"54\" x2=\"118\" y2=\"$y\" stroke=\"#000\" stroke-width=\"1\" marker-end=\"url(#ar)\"/>" }
        val phraseTexts = phrases.mapIndexed { i, p -> "<text x=\"145\" y=\"${ys[i] + 4}\" text-anchor=\"start\" font-weight=\"600\" fill=\"$color\" font-size=\"9\" font-family=\"sans-serif\">$p</text>" }.joinToString("")
        return """
<!DOCTYPE html>
<html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="margin:0;padding:2px;background:transparent;">
<div style="text-align:center;background:$color;color:#fff;padding:2px 6px;margin-bottom:4px;border-radius:3px;font-weight:bold;font-size:11px;font-family:sans-serif;">The Verb '$v'</div>
<svg viewBox="0 0 200 108" style="width:100%;height:100%;min-height:100px;display:block;" preserveAspectRatio="xMidYMid meet">
<defs><marker id="ar" markerWidth="5" markerHeight="4" refX="4" refY="2" orient="auto"><polygon points="0 0,5 2,0 4" fill="#000"/></marker></defs>
<circle cx="42" cy="54" r="20" fill="#fff" stroke="$color" stroke-width="2"/>
<text x="42" y="58" text-anchor="middle" font-weight="bold" fill="$color" font-size="12" font-family="sans-serif">$v</text>
$arrowLines
$phraseTexts
</svg>
</body></html>"""
    }

    /** Pick which verb to show in the diagram from the current lesson name (e.g. verb_GO -> GO, regular_verbs -> EAT). */
    fun verbForLessonDiagram(lessonName: String?): String {
        if (lessonName.isNullOrEmpty()) return "HAVE"
        return when {
            lessonName.startsWith("verb_") -> lessonName.removePrefix("verb_")
            lessonName == "regular_verbs" -> "EAT"
            lessonName == "irregular_verbs" -> "GO"
            else -> "HAVE"
        }
    }
}
