package com.alphacephei.vosk

import android.content.res.AssetManager
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Parse lesson file content into LessonRow lists. Extracted to reduce MainActivity.kt size.
 */
object LessonFileParsers {

    /** Begin marker for embedded vocabulary list in a lesson file (content below is ignored when parsing). */
    const val VOCABULARY_BLOCK_START = "# --- VOCABULARY ---"
    /** End marker for embedded vocabulary list. */
    const val VOCABULARY_BLOCK_END = "# --- END VOCABULARY ---"

    /** Strip the embedded vocabulary block so parsers only see lesson content. */
    fun stripVocabularyBlock(content: String): String {
        val startMarker = VOCABULARY_BLOCK_START
        val lines = content.lines()
        val sb = StringBuilder()
        for (line in lines) {
            if (line.trim() == startMarker) break
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append(line)
        }
        return sb.toString()
    }

    /**
     * Extract all English words from a conversation file (PersonA/PersonB lines).
     * Takes the English part (first segment) of each line, splits on non-letters, lowercases, returns distinct words.
     */
    fun extractWordsFromConversationContent(content: String): List<String> {
        val words = mutableSetOf<String>()
        stripVocabularyBlock(content).lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            val colonIdx = trimmed.indexOf(':')
            if (colonIdx <= 0) return@forEach
            val rest = trimmed.substring(colonIdx + 1).trim()
            val parts = rest.split(",", limit = 3).map { it.trim() }
            if (parts.isEmpty()) return@forEach
            val english = parts[0]
            english.split(Regex("[^a-zA-Z]+")).forEach { token ->
                val w = token.trim().lowercase()
                if (w.isNotEmpty()) words.add(w)
            }
        }
        return words.toList().sorted()
    }

    /** Extract the embedded vocabulary list (lines between VOCABULARY markers). Blank lines and # lines are skipped. */
    fun extractVocabularyBlock(content: String): List<String> {
        val startMarker = VOCABULARY_BLOCK_START
        val endMarker = VOCABULARY_BLOCK_END
        val lines = content.lines().toList()
        var inBlock = false
        val words = mutableListOf<String>()
        for (line in lines) {
            val t = line.trim()
            when {
                t == startMarker -> inBlock = true
                t == endMarker -> break
                inBlock && t.isNotEmpty() && !t.startsWith("#") -> words.add(t)
            }
        }
        return words
    }

    /** Incorrect list file name = original lesson name + this suffix (e.g. regular_verbs_inc.json). */
    const val INCORRECT_LESSON_SUFFIX = "_inc.json"
    /** Three-column stats file name suffix: {safeLessonKey}_3col_stats.json. */
    const val THREECOL_STATS_SUFFIX = "_3col_stats.json"
    /** Conversation-bubble stats file name suffix: {safeLessonKey}_conv_bubble_stats.json. */
    const val CONVERSATION_BUBBLE_STATS_SUFFIX = "_conv_bubble_stats.json"
    private const val TAG = "LessonFileParsers"

    /**
     * Parse 3- or 5-column lesson CSV for THREECOL_TABLE.
     * Format: English,Bengali,Hint  or  English,Bengali,Hint,A,B
     * A = practice pass (1) or fail (0), B = test pass (1) or fail (0).
     * Returns (rows, initialABPerRow) where initialABPerRow[i] = [A,B] if line had 5 columns, else null.
     */
    fun parseThreeColLessonFile(content: String): Pair<List<ThreeColRow>, List<IntArray?>> {
        val rows = mutableListOf<ThreeColRow>()
        val initialAB = mutableListOf<IntArray?>()
        stripVocabularyBlock(content).lines().forEach { line ->
            val parts = line.split(",").map { it.trim() }
            if (parts.size < 3) return@forEach
            val eng = parts[0]
            val bn = parts[1]
            val hint = parts[2]
            if (eng.isBlank() && bn.isBlank()) return@forEach
            rows.add(ThreeColRow(eng, bn, hint))
            val ab = if (parts.size >= 5) {
                val a = parts[3].toIntOrNull()?.coerceIn(0, 1) ?: 0
                val b = parts[4].toIntOrNull()?.coerceIn(0, 1) ?: 0
                intArrayOf(a, b)
            } else null
            initialAB.add(ab)
        }
        return rows to initialAB
    }

    /**
     * Parse conversation-bubble lesson file.
     * Format per line: PersonA: English,Bengali,Pronunciation  or  PersonB: English,Bengali,Pronunciation
     * Optional 5th/6th columns: A,B (practice/test pass 0 or 1).
     * Returns (rows, initialABPerRow) where initialABPerRow[i] = [A,B] if line had 5+ columns, else null.
     */
    fun parseConversationBubbleFile(content: String): Pair<List<ConversationBubbleRow>, List<IntArray?>> {
        val rows = mutableListOf<ConversationBubbleRow>()
        val initialAB = mutableListOf<IntArray?>()
        stripVocabularyBlock(content).lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            val colonIdx = trimmed.indexOf(':')
            if (colonIdx <= 0) return@forEach
            val speakerLabel = trimmed.substring(0, colonIdx).trim()
            val speaker = when (speakerLabel.uppercase()) {
                "PERSONA" -> "A"
                "PERSONB" -> "B"
                else -> return@forEach
            }
            val rest = trimmed.substring(colonIdx + 1).trim()
            val parts = rest.split(",", limit = 3).map { it.trim() }
            if (parts.size < 3) return@forEach
            val english = parts[0]
            val bengali = parts[1]
            val pronunciation = parts.getOrNull(2) ?: ""
            if (english.isBlank() && bengali.isBlank()) return@forEach
            rows.add(ConversationBubbleRow(speaker, english, bengali, pronunciation))
            val ab = if (parts.size >= 5) {
                val a = parts[3].toIntOrNull()?.coerceIn(0, 1) ?: 0
                val b = parts[4].toIntOrNull()?.coerceIn(0, 1) ?: 0
                intArrayOf(a, b)
            } else null
            initialAB.add(ab)
        }
        return rows to initialAB
    }

    /** Parse pipe-separated lines into LessonRow. A|B|C|D = engQ|bnQ|engA|bnA. */
    fun parseLessonFile(content: String): List<LessonRow> {
        val rows = mutableListOf<LessonRow>()
        content.lines().forEach { line ->
            val parts = line.split("|").map { it.trim() }
            if (parts.size >= 4) {
                rows.add(LessonRow(parts[0], parts[1], parts[2], parts[3]))
            }
        }
        return rows
    }

    /**
     * Parse SVO lesson file: first line = topic, then each line = "Bengali,English".
     * Returns (topic, rows). LessonRow: bnQ = upper box, engA = lower box.
     */
    fun parseSvoLessonFile(content: String): Pair<String, List<LessonRow>> {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return "" to emptyList()
        val topic = lines[0]
        val rows = mutableListOf<LessonRow>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            val commaIndex = line.indexOf(',')
            if (commaIndex < 0) continue
            val firstPart = line.substring(0, commaIndex).trim()
            val secondPart = line.substring(commaIndex + 1).trim()
            if (firstPart.isNotBlank() && secondPart.isNotBlank()) {
                rows.add(LessonRow(secondPart, firstPart, secondPart, firstPart))
            }
        }
        return topic to rows
    }

    /**
     * Parse SVO folder format: each line = "English,Bengali" or "English,Bengali,Pronunciation".
     * Skips lines starting with "Topic:". Returns (topicName, rows). LessonRow: bnQ = Bengali (upper), engA = English (answer).
     */
    fun parseSvoLessonFileEnglishFirst(content: String, topicName: String): Pair<String, List<LessonRow>> {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val rows = mutableListOf<LessonRow>()
        for (line in lines) {
            if (line.startsWith("Topic:", ignoreCase = true)) continue
            val parts = line.split(",").map { it.trim() }
            if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                val english = parts[0]
                val bengali = parts[1]
                rows.add(LessonRow(english, bengali, english, bengali))
            }
        }
        return topicName to rows
    }

    /**
     * Parse SVO folder CSV with header line (e.g. "Bengali Meaning,English Sentence").
     * Uses topicName as topic; skips header line; each data line = "Bengali,English".
     */
    fun parseSvoLessonFileWithHeader(content: String, topicName: String): Pair<String, List<LessonRow>> {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val rows = mutableListOf<LessonRow>()
        var startIndex = 0
        if (lines.isNotEmpty() && (lines[0].contains("Meaning", ignoreCase = true) || lines[0].contains("Sentence", ignoreCase = true))) {
            startIndex = 1
        }
        for (i in startIndex until lines.size) {
            val line = lines[i]
            val commaIndex = line.indexOf(',')
            if (commaIndex < 0) continue
            val firstPart = line.substring(0, commaIndex).trim()
            val secondPart = line.substring(commaIndex + 1).trim()
            if (firstPart.isNotBlank() && secondPart.isNotBlank()) {
                rows.add(LessonRow(secondPart, firstPart, secondPart, firstPart))
            }
        }
        return topicName to rows
    }

    /**
     * Parse verb lesson file (Regular_verbs.txt or Irregular_verbs.txt).
     * Comma-separated, first line header. Columns: Root, Past, V3, Bengali Meaning, ...
     * Uses columns 0 (root) and 3 (Bengali) for the lesson.
     */
    fun parseVerbLessonFile(content: String): List<LessonRow> {
        val rows = mutableListOf<LessonRow>()
        val lines = content.lines()
        if (lines.size < 2) return rows
        for (i in 1 until lines.size) {
            val parts = lines[i].split(",").map { it.trim() }
            if (parts.size >= 4) {
                val root = parts[0]
                val bengali = parts[3]
                if (root.isNotBlank() && bengali.isNotBlank()) {
                    rows.add(LessonRow(root, bengali, root, bengali))
                }
            }
        }
        return rows
    }

    /** Fallback lesson description in Bengali when the file has no INFO_START/INFO_END block. */
    fun getLessonInfoBengali(@Suppress("UNUSED_PARAMETER") filename: String): String? =
        "এই পাঠে ইংরেজি শব্দের উচ্চারণ অনুশীলন করুন। অ্যাপ শব্দ বলবে, আপনি একই শব্দ বলুন।"

    /**
     * Parse Short_i_vs_Long_ee.txt: each row has 8 columns. We use 1st, 3rd, 4th, 5th, 7th, 8th (0-indexed: 0,2,3,4,6,7).
     * Returns flattened rows for display: two "cards" per data row — first card = [word_short, pron_short, meaning_short],
     * second card = [word_long, pron_long, meaning_long]. Each returned row has 3 elements.
     */
    fun parseShortIVsLongEeFile(content: String): List<List<String>> {
        val lines = content.lines()
        val result = mutableListOf<List<String>>()
        for (line in lines.drop(1)) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val cols = trimmed.split(",").map { it.trim() }
            if (cols.size < 8) continue
            val shortWord = cols.getOrNull(0) ?: ""
            val shortPron = cols.getOrNull(2) ?: ""
            val shortMeaning = cols.getOrNull(3) ?: ""
            val longWord = cols.getOrNull(4) ?: ""
            val longPron = cols.getOrNull(6) ?: ""
            val longMeaning = cols.getOrNull(7) ?: ""
            if (shortWord.isNotEmpty()) result.add(listOf(shortWord, shortPron, shortMeaning))
            if (longWord.isNotEmpty()) result.add(listOf(longWord, longPron, longMeaning))
        }
        return result
    }

    /**
     * Parse lesson file that may have INFO_START ... INFO_END block at top.
     * Returns (infoText or null, csvContent). If no block, returns (null, full content trimmed).
     */
    fun parseLessonFileWithInfo(content: String): Pair<String?, String> {
        val lines = content.lines()
        var startIdx = -1
        var endIdx = -1
        for (i in lines.indices) {
            val t = lines[i].trim()
            val u = t.uppercase()
            if (u.startsWith("INFO_START")) {
                startIdx = i
                break
            }
        }
        if (startIdx < 0) return null to content.trim()
        for (j in startIdx + 1 until lines.size) {
            if (lines[j].trim().uppercase() == "INFO_END") {
                endIdx = j
                break
            }
        }
        if (endIdx < 0) return null to content.trim()
        val infoText = lines.subList(startIdx + 1, endIdx).joinToString("\n").trim()
        val csvLines = lines.take(startIdx) + lines.drop(endIdx + 1)
        val csvContent = csvLines.joinToString("\n").trim()
        return (if (infoText.isNotEmpty()) infoText else null) to csvContent
    }

    /**
     * Load incorrect lesson rows from a specific _inc.json file; returns (list name for display, rows) or (null, emptyList()) if missing/empty.
     */
    fun loadIncorrectLessonListFromFile(filesDir: File, fileName: String): Pair<String?, List<LessonRow>> {
        return try {
            val file = File(filesDir, fileName)
            if (!file.exists()) return Pair(null, emptyList())
            val text = file.readText()
            val rows = mutableListOf<LessonRow>()
            val firstChar = text.trimStart().firstOrNull()
            if (firstChar == '{') {
                val root = JSONObject(text)
                val sourceName = if (root.has("lessonName")) root.getString("lessonName") else null
                val arr = root.getJSONArray("rows")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    rows.add(LessonRow(
                        obj.getString("engQ"),
                        obj.getString("bnQ"),
                        obj.getString("engA"),
                        obj.getString("bnA")
                    ))
                }
                val displayName = sourceName?.let { it + "_inc" } ?: fileName.removeSuffix(INCORRECT_LESSON_SUFFIX)
                return Pair(displayName, rows)
            }
            val arr = JSONArray(text)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                rows.add(LessonRow(
                    obj.getString("engQ"),
                    obj.getString("bnQ"),
                    obj.getString("engA"),
                    obj.getString("bnA")
                ))
            }
            val displayName = fileName.removeSuffix(INCORRECT_LESSON_SUFFIX)
            Pair(displayName, rows)
        } catch (e: Exception) {
            Log.e(TAG, "Load incorrect list failed", e)
            Pair(null, emptyList())
        }
    }

    private fun makeSafeName(sourceName: String): String =
        sourceName.replace(Regex("[\\\\/:*?\"<>|]"), "_")

    /** Save incorrect lesson rows to file named {sanitized source name}_inc.json. */
    fun saveIncorrectLessonListToFile(filesDir: File, sourceName: String, rows: List<LessonRow>) {
        val safeName = makeSafeName(sourceName)
        val fileName = safeName + INCORRECT_LESSON_SUFFIX
        try {
            val file = File(filesDir, fileName)
            val arr = JSONArray()
            for (r in rows) {
                val obj = JSONObject()
                obj.put("engQ", r.engQ)
                obj.put("bnQ", r.bnQ)
                obj.put("engA", r.engA)
                obj.put("bnA", r.bnA)
                arr.put(obj)
            }
            val root = JSONObject()
            root.put("lessonName", sourceName)
            root.put("rows", arr)
            file.writeText(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Save incorrect list failed", e)
        }
    }

    /**
     * Load three-column lesson stats from {safeLessonKey}_3col_stats.json.
     * If file is missing or a row is missing, that row is [0, 0] (initially failed).
     * JSON shape: { "version": 1, "lessonKey": "...", "rows": [[A,B], ...] }
     */
    fun loadThreeColStats(filesDir: File, lessonKey: String, rowCount: Int): MutableList<IntArray> {
        val safeName = makeSafeName(lessonKey)
        val file = File(filesDir, safeName + THREECOL_STATS_SUFFIX)
        val stats = MutableList(rowCount) { IntArray(2) { 0 } }
        if (!file.exists()) return stats
        return try {
            val text = file.readText().trim()
            if (text.isEmpty()) return stats
            val root = JSONObject(text)
            val arr = root.optJSONArray("rows") ?: return stats
            val limit = minOf(arr.length(), rowCount)
            for (i in 0 until limit) {
                val row = arr.optJSONArray(i) ?: continue
                val p = row.optInt(0, 0)
                val t = row.optInt(1, 0)
                stats[i][0] = if (p != 0) 1 else 0
                stats[i][1] = if (t != 0) 1 else 0
            }
            stats
        } catch (e: Exception) {
            Log.e(TAG, "Load three-col stats failed", e)
            stats
        }
    }

    /** Save three-column lesson stats to {safeLessonKey}_3col_stats.json. Each row: [A, B] only. */
    fun saveThreeColStats(filesDir: File, lessonKey: String, rows: List<IntArray>) {
        val safeName = makeSafeName(lessonKey)
        val file = File(filesDir, safeName + THREECOL_STATS_SUFFIX)
        try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("lessonKey", lessonKey)
            val arr = JSONArray()
            for (r in rows) {
                val row = JSONArray()
                row.put(if ((r.getOrNull(0) ?: 0) != 0) 1 else 0)
                row.put(if ((r.getOrNull(1) ?: 0) != 0) 1 else 0)
                arr.put(row)
            }
            root.put("rows", arr)
            file.writeText(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Save three-col stats failed", e)
        }
    }

    /**
     * Load conversation-bubble lesson stats from {safeLessonKey}_conv_bubble_stats.json.
     * Same JSON shape as three-col: { "version": 1, "lessonKey": "...", "rows": [[A,B], ...] }
     */
    fun loadConversationBubbleStats(filesDir: File, lessonKey: String, rowCount: Int): MutableList<IntArray> {
        val safeName = makeSafeName(lessonKey)
        val file = File(filesDir, safeName + CONVERSATION_BUBBLE_STATS_SUFFIX)
        val stats = MutableList(rowCount) { IntArray(2) { 0 } }
        if (!file.exists()) return stats
        return try {
            val text = file.readText().trim()
            if (text.isEmpty()) return stats
            val root = JSONObject(text)
            val arr = root.optJSONArray("rows") ?: return stats
            val limit = minOf(arr.length(), rowCount)
            for (i in 0 until limit) {
                val row = arr.optJSONArray(i) ?: continue
                stats[i][0] = if (row.optInt(0, 0) != 0) 1 else 0
                stats[i][1] = if (row.optInt(1, 0) != 0) 1 else 0
            }
            stats
        } catch (e: Exception) {
            Log.e(TAG, "Load conversation-bubble stats failed", e)
            stats
        }
    }

    /** Save conversation-bubble lesson stats. Each row: [A, B] only. */
    fun saveConversationBubbleStats(filesDir: File, lessonKey: String, rows: List<IntArray>) {
        val safeName = makeSafeName(lessonKey)
        val file = File(filesDir, safeName + CONVERSATION_BUBBLE_STATS_SUFFIX)
        try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("lessonKey", lessonKey)
            val arr = JSONArray()
            for (r in rows) {
                val row = JSONArray()
                row.put(if ((r.getOrNull(0) ?: 0) != 0) 1 else 0)
                row.put(if ((r.getOrNull(1) ?: 0) != 0) 1 else 0)
                arr.put(row)
            }
            root.put("rows", arr)
            file.writeText(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Save conversation-bubble stats failed", e)
        }
    }

    // ─── Vocabulary master list: per-word progress (stored in app storage; assets are read-only) ───
    /** File name for vocabulary progress: word -> 0/1/2 (0=never tried, 1=passed pronunciation, 2=tried but failed). */
    const val VOCABULARY_PROGRESS_FILE = "vocabulary_progress.json"
    const val VOCAB_PROGRESS_NEVER = 0
    const val VOCAB_PROGRESS_PASSED = 1
    const val VOCAB_PROGRESS_FAILED = 2

    /**
     * Load per-word progress from filesDir. Key = word (lowercase), value = 0/1/2.
     * Merge with master list in memory: for each word, progress = loaded[word] ?: 0.
     */
    fun loadVocabularyProgress(filesDir: File): Map<String, Int> {
        val file = File(filesDir, VOCABULARY_PROGRESS_FILE)
        if (!file.exists()) return emptyMap()
        return try {
            val text = file.readText().trim()
            if (text.isEmpty()) return emptyMap()
            val root = JSONObject(text)
            val obj = root.optJSONObject("progress") ?: return emptyMap()
            val keys = obj.keys()
            val map = mutableMapOf<String, Int>()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optInt(k, VOCAB_PROGRESS_NEVER).coerceIn(VOCAB_PROGRESS_NEVER, VOCAB_PROGRESS_FAILED)
            }
            map
        } catch (e: Exception) {
            Log.e(TAG, "Load vocabulary progress failed", e)
            emptyMap()
        }
    }

    /** Save per-word progress to filesDir. Call after user passes or fails a pronunciation test for a word. */
    fun saveVocabularyProgress(filesDir: File, progress: Map<String, Int>) {
        val file = File(filesDir, VOCABULARY_PROGRESS_FILE)
        try {
            val root = JSONObject()
            root.put("version", 1)
            val obj = JSONObject()
            for ((word, value) in progress) {
                obj.put(word, value.coerceIn(VOCAB_PROGRESS_NEVER, VOCAB_PROGRESS_FAILED))
            }
            root.put("progress", obj)
            file.writeText(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Save vocabulary progress failed", e)
        }
    }

    /** File in filesDir for words added from lessons (not in asset master list). Same format as master list. */
    const val VOCABULARY_MASTER_ADDITIONS_FILE = "vocabulary_master_additions.txt"

    /**
     * Load master list additions from filesDir (words added from lessons). Format per line: "word, category, meaning, pronunciation"
     */
    fun loadMasterListAdditions(filesDir: java.io.File): Map<String, Pair<String, String>> {
        val file = java.io.File(filesDir, VOCABULARY_MASTER_ADDITIONS_FILE)
        if (!file.exists()) return emptyMap()
        val map = mutableMapOf<String, Pair<String, String>>()
        try {
            file.readText().lines().forEach { line ->
                val t = line.trim()
                if (t.isEmpty() || t.startsWith("#")) return@forEach
                val parts = t.split(", ", limit = 4)
                if (parts.size < 4) return@forEach
                val word = parts[0].trim().lowercase()
                val meaning = parts[2].trim()
                val pronunciation = parts[3].trim()
                if (word.isNotEmpty()) map[word] = meaning to pronunciation
            }
        } catch (e: Exception) {
            Log.e(TAG, "Load master list additions failed", e)
        }
        return map
    }

    /** Append one word to the master list additions file. Use "—" for meaning/pronunciation if unknown. */
    fun saveMasterListAddition(filesDir: java.io.File, word: String, meaning: String, pronunciation: String) {
        val file = java.io.File(filesDir, VOCABULARY_MASTER_ADDITIONS_FILE)
        val key = word.trim().lowercase()
        if (key.isEmpty()) return
        val line = "$key, lesson, $meaning, $pronunciation\n"
        try {
            file.appendText(line)
        } catch (e: Exception) {
            Log.e(TAG, "Save master list addition failed", e)
        }
    }

    /**
     * Load master word list from assets. Returns map: word (lowercase) -> (meaning, pronunciation).
     * Format per line: "word, category, bengali_meaning, bengali_pronunciation"
     */
    fun loadMasterWordList(assets: AssetManager, path: String = "vocabulary/master_word_list.txt"): Map<String, Pair<String, String>> {
        val map = mutableMapOf<String, Pair<String, String>>()
        try {
            assets.open(path).bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    val t = line.trim()
                    if (t.isEmpty() || t.startsWith("#")) return@forEachLine
                    val parts = t.split(", ", limit = 4)
                    if (parts.size < 4) return@forEachLine
                    val word = parts[0].trim().lowercase()
                    val meaning = parts[2].trim()
                    val pronunciation = parts[3].trim()
                    if (word.isNotEmpty()) map[word] = meaning to pronunciation
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Load master word list failed", e)
        }
        return map
    }

    /**
     * Build lesson vocab rows from list of words and master list lookup.
     * For each word, look up (meaning, pronunciation); use "—" if not in master.
     */
    fun buildLessonVocabRows(
        words: List<String>,
        masterMap: Map<String, Pair<String, String>>
    ): List<LessonVocabRow> = words.map { w ->
        val key = w.trim().lowercase()
        val (meaning, pronunciation) = masterMap[key] ?: ("—" to "—")
        LessonVocabRow(word = w.trim(), pronunciation = pronunciation, meaning = meaning)
    }

    /**
     * Build lesson vocab rows only for words that are in the master list (dictionary words).
     * Excludes names, place names, river names, etc. that are not in the master list.
     * Use this for the V tab so only dictionary words are shown.
     * Cross-checks each word with the master map; only includes if the word (lowercase) exists there.
     */
    fun buildLessonVocabRowsOnlyInMaster(
        words: List<String>,
        masterMap: Map<String, Pair<String, String>>
    ): List<LessonVocabRow> = words.mapNotNull { w ->
        val key = w.trim().lowercase()
        if (key.isEmpty()) return@mapNotNull null
        if (!masterMap.containsKey(key)) return@mapNotNull null
        val pair = masterMap[key]!!
        val (meaning, pronunciation) = pair
        LessonVocabRow(word = w.trim(), pronunciation = pronunciation, meaning = meaning)
    }

    /**
     * Filter lesson vocab rows to only those whose word exists in the master list.
     * Use when populating the V tab to ensure names/places (e.g. Dhaka, Rahul) are never shown.
     */
    fun filterLessonVocabRowsByMaster(
        rows: List<LessonVocabRow>,
        masterMap: Map<String, Pair<String, String>>?
    ): List<LessonVocabRow> {
        val map = masterMap ?: return emptyList()
        return rows.filter { map.containsKey(it.word.trim().lowercase()) }
    }

    /**
     * Filter to only rows that still need testing: progress is 0 (never seen) or 2 (tested but failed).
     * Excludes words that already passed (progress == 1). Missing key in progressMap is treated as 0.
     */
    fun filterLessonVocabRowsNeedingTest(
        rows: List<LessonVocabRow>,
        progressMap: Map<String, Int>
    ): List<LessonVocabRow> = rows.filter { row ->
        (progressMap[row.word.trim().lowercase()] ?: VOCAB_PROGRESS_NEVER) != VOCAB_PROGRESS_PASSED
    }
}
