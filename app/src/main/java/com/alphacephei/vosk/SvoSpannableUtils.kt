package com.alphacephei.vosk

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ReplacementSpan
import androidx.core.content.ContextCompat

/**
 * SVO-colored spannables and description text. Extracted to reduce MainActivity.kt size.
 */
object SvoSpannableUtils {

    /** Span that draws a word with colored background and white text, with horizontal padding. */
    class PaddedBackgroundSpan(
        private val bgColor: Int,
        private val textColor: Int,
        private val paddingPx: Int
    ) : ReplacementSpan() {
        override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
            return (paint.measureText(text, start, end) + 2 * paddingPx).toInt()
        }
        override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
            val textWidth = paint.measureText(text, start, end)
            val totalWidth = textWidth + 2 * paddingPx
            paint.style = Paint.Style.FILL
            paint.color = bgColor
            canvas.drawRect(x, top.toFloat(), x + totalWidth, bottom.toFloat(), paint)
            paint.color = textColor
            canvas.drawText(text, start, end, x + paddingPx, y.toFloat(), paint)
        }
    }

    private val questionStartWords = setOf(
        "where", "what", "who", "how", "when", "why", "which",
        "is", "are", "do", "does", "did", "can", "could", "will", "would", "have", "has", "had", "was", "were"
    )

    /**
     * Color-code an English sentence: each word has its own background (Subject=blue, Verb=green, Object=orange) and white text, with left/right padding.
     * Declarative: "I eat rice" → subject, verb, object.
     * Questions: [Wh/Aux]=object, [Verb]=verb, [Subject]=subject, rest=object.
     */
    fun makeSvoSpannable(context: Context, sentence: String): SpannableString {
        val s = sentence.trim()
        if (s.isEmpty()) return SpannableString("")
        val words = s.split(Regex("\\s+"))
        val subjectBg = ContextCompat.getColor(context, R.color.svo_subject)
        val verbBg = ContextCompat.getColor(context, R.color.svo_verb)
        val objectBg = ContextCompat.getColor(context, R.color.svo_object)
        val whiteText = Color.WHITE
        val paddingPx = (4 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val spannable = SpannableString(s)
        val isQuestion = words.size >= 3 && words[0].lowercase() in questionStartWords
        var start = 0
        for (i in words.indices) {
            val word = words[i]
            val end = start + word.length
            val bgColor = when {
                words.size == 1 -> subjectBg
                isQuestion -> when (i) {
                    0 -> objectBg
                    1 -> verbBg
                    2 -> subjectBg
                    else -> objectBg
                }
                else -> when {
                    i == 0 -> subjectBg
                    i == 1 -> verbBg
                    else -> objectBg
                }
            }
            if (end <= spannable.length) {
                spannable.setSpan(PaddedBackgroundSpan(bgColor, whiteText, paddingPx), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            start = end
            if (start < spannable.length && spannable[start] == ' ') start++
        }
        return spannable
    }

    /** Build description for SVO sentence: "Here subject is X, verb is Y and object is Z." + third-person -s/-es hint when applicable. */
    fun makeSvoDescription(context: Context, sentence: String): String {
        val s = sentence.trim()
        if (s.isEmpty()) return ""
        val words = s.split(Regex("\\s+"))
        val isQuestion = words.size >= 3 && words[0].lowercase() in questionStartWords
        val subject: String
        val verb: String
        val obj: String
        when {
            words.size == 1 -> {
                subject = words[0]
                verb = ""
                obj = ""
            }
            isQuestion -> {
                subject = if (words.size > 2) words[2] else ""
                verb = if (words.size > 1) words[1] else ""
                obj = if (words.size > 3) words.drop(3).joinToString(" ") else (if (words.isNotEmpty()) words[0] else "")
            }
            else -> {
                subject = words[0]
                verb = if (words.size > 1) words[1] else ""
                obj = if (words.size > 2) words.drop(2).joinToString(" ") else ""
            }
        }
        val svoLine = context.getString(R.string.description_svo_format, subject, verb, obj).trim()
        val needsThirdPersonHint = subject.lowercase() in setOf("he", "she", "it") ||
            (subject.isNotEmpty() && subject.lowercase() !in setOf("i", "you", "we", "they"))
        val hint = if (needsThirdPersonHint && verb.isNotEmpty()) context.getString(R.string.description_third_person_hint) else ""
        return if (hint.isNotEmpty()) "$svoLine $hint" else svoLine
    }

    /** Build "Expected: [SVO-colored sentence]\n\nHeard: [heard]". */
    fun makeExpectedAndHeardSpannable(context: Context, expected: String, heard: String): SpannableStringBuilder {
        return SpannableStringBuilder()
            .append(context.getString(R.string.expected_label)).append(" ")
            .append(makeSvoSpannable(context, StringUtils.firstAlternative(expected)))
            .append("\n\n")
            .append(context.getString(R.string.heard_label)).append(" ").append(MatchNormalizer.sanitizeSpokenTextForDisplay(heard))
    }

    /** Build "Expected: [SVO-colored sentence]\n\nYou said: [said]". */
    fun makeExpectedAndYouSaidSpannable(context: Context, expected: String, said: String): SpannableStringBuilder {
        return SpannableStringBuilder()
            .append(context.getString(R.string.expected_label)).append(" ")
            .append(makeSvoSpannable(context, StringUtils.firstAlternative(expected)))
            .append("\n\n")
            .append(context.getString(R.string.you_said_label)).append(" ").append(MatchNormalizer.sanitizeSpokenTextForDisplay(said))
    }
}
