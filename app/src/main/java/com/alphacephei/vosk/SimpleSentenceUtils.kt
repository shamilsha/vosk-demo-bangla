package com.alphacephei.vosk

import android.content.res.AssetManager

/**
 * Helpers for simple_XXX.txt lessons (drawer subtopics and lesson title).
 * Extracted to reduce MainActivity.kt size.
 */
object SimpleSentenceUtils {

    /**
     * Keys registered in MainActivity [threeColLessonAssetPaths] that use [THREECOL_TABLE] (lesson base).
     * `test_layout` is only a drawer key (no `simple_test_layout.txt`); other keys match `simple_*.txt` files.
     * Must stay in sync with `threeColLessonAssetPaths` in MainActivity.
     *
     * For each matching `simple_*.txt`, [buildSimpleSentenceSubtopics] uses **THREECOL_TABLE** and short titles
     * ("What", "Who", …) so there is a **single** drawer row per file (no duplicate SIMPLE_SENTENCE row).
     */
    val SIMPLE_TXT_ACTION_KEYS_USING_THREE_COL_TABLE: Set<String> = setOf(
        "test_layout",
        "simple_what",
        "simple_where",
        "simple_how",
        "simple_let",
        "simple_when",
        "simple_who",
        "simple_why",
        "can"
    )

    /** Discover simple_XXX.txt in Lessons/SVO/ — one subtopic per file; 3-col keys use [ContentLayout.THREECOL_TABLE]. */
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
            val layout = if (actionKey in SIMPLE_TXT_ACTION_KEYS_USING_THREE_COL_TABLE) {
                ContentLayout.THREECOL_TABLE
            } else {
                ContentLayout.SIMPLE_SENTENCE
            }
            Subtopic(title, actionKey, layout)
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
