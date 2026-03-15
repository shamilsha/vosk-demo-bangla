package com.alphacephei.vosk

import android.content.res.AssetManager

/**
 * Helpers for simple_XXX.txt lessons (drawer subtopics and lesson title).
 * Extracted to reduce MainActivity.kt size.
 */
object SimpleSentenceUtils {

    /** Discover simple_XXX.txt in Lessons/SVO/ and build one subtopic per file. */
    fun buildSimpleSentenceSubtopics(assetManager: AssetManager): List<Subtopic> {
        val files = try {
            assetManager.list("Lessons/SVO")?.filter { it.startsWith("simple_") && it.endsWith(".txt") }
                ?.sorted() ?: emptyList()
        } catch (_: Exception) { emptyList() }
        return files.map { filename ->
            val actionKey = filename.removeSuffix(".txt")
            val xxx = actionKey.removePrefix("simple_")
            val title = when (xxx) {
                "sentence" -> "Simple sentences"
                "let" -> "Let"
                else -> xxx.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            Subtopic(title, actionKey, ContentLayout.SIMPLE_SENTENCE)
        }
    }

    /** Display title for a simple-sentence lesson by actionKey (e.g. simple_let → "Let"). */
    fun simpleSentenceLessonTitle(actionKey: String): String {
        if (!actionKey.startsWith("simple_")) return actionKey
        val xxx = actionKey.removePrefix("simple_")
        return when (xxx) {
            "sentence" -> "Simple sentences"
            "let" -> "Let"
            else -> xxx.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
