package com.alphacephei.vosk

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Parse lesson file content into LessonRow lists. Extracted to reduce MainActivity.kt size.
 */
object LessonFileParsers {

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
        content.lines().forEach { line ->
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
        content.lines().forEach { line ->
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
}
