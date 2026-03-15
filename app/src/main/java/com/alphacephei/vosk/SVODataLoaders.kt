package com.alphacephei.vosk

import android.content.res.AssetManager
import java.nio.charset.StandardCharsets

/**
 * Load SVO-style CSV from assets (English,Bengali,Pronunciation). Extracted to reduce MainActivity.kt size.
 */
object SVODataLoaders {

    /** Each line "English,Bengali,Pronunciation". Returns list of SvEntry with verb = full English. */
    fun loadFourSectionFile(assetManager: AssetManager, path: String): List<SvEntry> {
        val list = mutableListOf<SvEntry>()
        try {
            assetManager.open(path).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                for (line in reader.readLines()) {
                    val parts = line.split(',', limit = 3).map { it.trim() }
                    if (parts.size < 2 || parts[0].isBlank()) continue
                    list.add(SvEntry(parts[0], parts[1], parts.getOrNull(2).orEmpty()))
                }
            }
        } catch (_: Exception) { }
        return list
    }

    /** Load SV_words.txt: (words, meanings, pronunciations). */
    fun loadSvWordsData(assetManager: AssetManager): Triple<List<String>, List<String>, List<String>> {
        val words = mutableListOf<String>()
        val meanings = mutableListOf<String>()
        val prons = mutableListOf<String>()
        try {
            assetManager.open("Lessons/SVO/SV_words.txt").bufferedReader(StandardCharsets.UTF_8).use { reader ->
                for (line in reader.readLines()) {
                    val parts = line.split(',', limit = 3).map { it.trim() }
                    if (parts.isEmpty() || parts[0].isBlank()) continue
                    words.add(parts[0])
                    meanings.add(parts.getOrNull(1).orEmpty())
                    prons.add(parts.getOrNull(2).orEmpty())
                }
            }
        } catch (_: Exception) { }
        return Triple(words, meanings, prons)
    }

    /** Load SV_Ing.txt: -ing form verbs. Returns list of SvEntry with verb = -ing form. */
    fun loadSvIngData(assetManager: AssetManager): List<SvEntry> {
        val list = mutableListOf<SvEntry>()
        try {
            assetManager.open("Lessons/SVO/SV_Ing.txt").bufferedReader(StandardCharsets.UTF_8).use { reader ->
                for (line in reader.readLines()) {
                    val parts = line.split(',', limit = 3).map { it.trim() }
                    if (parts.size < 2) continue
                    val english = parts[0]
                    val bengali = parts[1]
                    val pronunciation = parts.getOrNull(2).orEmpty()
                    if (english.isBlank()) continue
                    val verb = english.split(Regex("\\s+")).lastOrNull()?.replaceFirstChar { it.uppercase() } ?: continue
                    list.add(SvEntry(verb, bengali, pronunciation))
                }
            }
        } catch (_: Exception) { }
        return list
    }

    private val svRibbonSubjectOrder = listOf("I", "You", "He", "She", "We", "They")

    /** Load SV ribbon from per-subject files SV_I.txt, SV_He.txt, etc. Format: "Subject verb,Bengali" or "Subject verb,Bengali,Pronunciation". */
    fun loadSvRibbonDataPerSubject(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svRibbonSubjectOrder) {
            val fileName = SVOLessonPaths.svRibbonFileName(subj)
            try {
                assetManager.open(fileName).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    val list = mutableListOf<SvEntry>()
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.split(Regex("\\s+"), limit = 2)
                        if (words.size >= 2) {
                            val verb = words[1].replaceFirstChar { it.uppercase() }
                            list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                    if (list.isNotEmpty()) dataPerSubject[subj] = list
                }
            } catch (_: Exception) { }
        }
        val subjects = svRibbonSubjectOrder.filter { it in dataPerSubject }.ifEmpty { dataPerSubject.keys.toList() }
        return if (subjects.isEmpty()) svRibbonSubjectOrder to emptyMap() else subjects to dataPerSubject
    }

    /** Load SV ribbon past: SV_X_past.txt. Same structure as loadSvRibbonDataPerSubject. */
    fun loadSvRibbonDataPast(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svRibbonSubjectOrder) {
            val fileName = SVOLessonPaths.svRibbonPastFileName(subj)
            try {
                assetManager.open(fileName).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    val list = mutableListOf<SvEntry>()
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.split(Regex("\\s+"), limit = 2)
                        if (words.size >= 2) {
                            val verb = words[1].replaceFirstChar { it.uppercase() }
                            list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                    if (list.isNotEmpty()) dataPerSubject[subj] = list
                }
            } catch (_: Exception) { }
        }
        val subjects = svRibbonSubjectOrder.filter { it in dataPerSubject }.ifEmpty { dataPerSubject.keys.toList() }
        return if (subjects.isEmpty()) svRibbonSubjectOrder to emptyMap() else subjects to dataPerSubject
    }

    /** Load SV ribbon future: SV_X_future.txt (Subject + will + verb). */
    fun loadSvRibbonDataFuture(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svRibbonSubjectOrder) {
            val fileName = SVOLessonPaths.svRibbonFutureFileName(subj)
            try {
                assetManager.open(fileName).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    val list = mutableListOf<SvEntry>()
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.split(Regex("\\s+"), limit = 3)
                        if (words.size >= 3 && words[1].equals("will", ignoreCase = true)) {
                            val verb = words[2].replaceFirstChar { it.uppercase() }
                            list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                    if (list.isNotEmpty()) dataPerSubject[subj] = list
                }
            } catch (_: Exception) { }
        }
        val subjects = svRibbonSubjectOrder.filter { it in dataPerSubject }.ifEmpty { dataPerSubject.keys.toList() }
        return if (subjects.isEmpty()) svRibbonSubjectOrder to emptyMap() else subjects to dataPerSubject
    }

    /** Legacy: (subjects, distinct verb list) from present ribbon; fallback default verbs if empty. */
    fun loadSvRibbonData(assetManager: AssetManager): Pair<List<String>, List<String>> {
        val (subjects, dataPerSubject) = loadSvRibbonDataPerSubject(assetManager)
        val verbList = dataPerSubject.values.flatten().map { it.verb }.distinct().toMutableList()
        if (verbList.isEmpty()) {
            verbList.addAll(listOf("Play", "Run", "Eat", "Do", "Walk", "Go", "Come", "Read", "Write", "Work", "Sleep", "Learn", "Cook", "Speak", "Make", "Study"))
        }
        return subjects to verbList
    }

    /** Load SV_I_negative.txt for Triple conveyor: subjects ["I"], middle "don't", right = verbs. Returns (subjects, dataPerSubject). */
    fun loadTripleDataINegative(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val list = mutableListOf<SvEntry>()
        try {
            val path = SVOLessonPaths.svTripleFileName("I", "negative")
            assetManager.open(path).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                for (line in reader.readLines()) {
                    val parts = line.split(',', limit = 3).map { it.trim() }
                    if (parts.size < 2) continue
                    val part = parts[0]
                    val bengali = parts[1]
                    val pronunciation = parts.getOrNull(2).orEmpty()
                    if (part.isBlank()) continue
                    val rest = part.removePrefix("I ").removePrefix("i ")
                    val verb = when {
                        rest.startsWith("don't ", ignoreCase = true) -> rest.drop(6)
                        else -> continue
                    }
                    if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                }
            }
        } catch (_: Exception) { }
        val subjects = if (list.isEmpty()) emptyList() else listOf("I")
        val dataPerSubject = if (list.isEmpty()) emptyMap() else mapOf("I" to list)
        return subjects to dataPerSubject
    }

    /** Load SV_I_question.txt for Triple conveyor: left = ["Do"], middle = ["I"], right = verbs. Returns (leftList, middleList, rightEntries). */
    fun loadTripleDataIQuestion(assetManager: AssetManager): Triple<List<String>, List<String>, List<SvEntry>> {
        val list = mutableListOf<SvEntry>()
        try {
            val path = SVOLessonPaths.svTripleFileName("I", "question")
            assetManager.open(path).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                for (line in reader.readLines()) {
                    val parts = line.split(',', limit = 3).map { it.trim() }
                    if (parts.size < 2) continue
                    val part = parts[0]
                    val bengali = parts[1]
                    val pronunciation = parts.getOrNull(2).orEmpty()
                    if (part.isBlank()) continue
                    val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                    val verb = when {
                        words.size >= 3 && words[0].equals("Do", ignoreCase = true) && words[1].equals("I", ignoreCase = true) -> words[2] + "?"
                        else -> continue
                    }
                    if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                }
            }
        } catch (_: Exception) { }
        return Triple(listOf("Do"), listOf("I"), list)
    }

    /** Load SV_I_question_negative.txt for Triple conveyor: left = ["Don't"], middle = ["I"], right = verbs. Returns (leftList, middleList, rightEntries). */
    fun loadTripleDataIQuestionNegative(assetManager: AssetManager): Triple<List<String>, List<String>, List<SvEntry>> {
        val list = mutableListOf<SvEntry>()
        try {
            val path = SVOLessonPaths.svTripleFileName("I", "question_negative")
            assetManager.open(path).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                for (line in reader.readLines()) {
                    val parts = line.split(',', limit = 3).map { it.trim() }
                    if (parts.size < 2) continue
                    val part = parts[0]
                    val bengali = parts[1]
                    val pronunciation = parts.getOrNull(2).orEmpty()
                    if (part.isBlank()) continue
                    val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                    val verb = when {
                        words.size >= 3 && words[0].equals("Don't", ignoreCase = true) && words[1].equals("I", ignoreCase = true) -> words[2] + "?"
                        else -> continue
                    }
                    if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                }
            }
        } catch (_: Exception) { }
        return Triple(listOf("Don't"), listOf("I"), list)
    }

    /** Load SV_You_question.txt for Triple conveyor: left = ["Do"], middle = ["You"], right = verbs. Returns (leftList, middleList, rightEntries). */
    fun loadTripleDataYouQuestion(assetManager: AssetManager): Triple<List<String>, List<String>, List<SvEntry>> {
        val list = mutableListOf<SvEntry>()
        try {
            val path = SVOLessonPaths.svTripleFileName("You", "question")
            assetManager.open(path).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                for (line in reader.readLines()) {
                    val parts = line.split(',', limit = 3).map { it.trim() }
                    if (parts.size < 2) continue
                    val part = parts[0]
                    val bengali = parts[1]
                    val pronunciation = parts.getOrNull(2).orEmpty()
                    if (part.isBlank()) continue
                    val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                    val verb = when {
                        words.size >= 3 && words[0].equals("Do", ignoreCase = true) && words[1].equals("You", ignoreCase = true) -> words[2] + "?"
                        else -> continue
                    }
                    if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                }
            }
        } catch (_: Exception) { }
        return Triple(listOf("Do"), listOf("You"), list)
    }

    /** Load SV_You_question_ing.txt: left = ["Are"], middle = ["you"], right = -ing verbs. */
    fun loadTripleDataYouQuestionIng(assetManager: AssetManager): Triple<List<String>, List<String>, List<SvEntry>> {
        val list = mutableListOf<SvEntry>()
        try {
            val path = SVOLessonPaths.svTripleFileName("You", "question_ing")
            assetManager.open(path).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                for (line in reader.readLines()) {
                    val parts = line.split(',', limit = 3).map { it.trim() }
                    if (parts.size < 2) continue
                    val part = parts[0]
                    val bengali = parts[1]
                    val pronunciation = parts.getOrNull(2).orEmpty()
                    if (part.isBlank()) continue
                    val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                    val verb = when {
                        words.size >= 3 && words[0].equals("Are", ignoreCase = true) && words[1].equals("You", ignoreCase = true) -> words[2] + "?"
                        else -> continue
                    }
                    if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                }
            }
        } catch (_: Exception) { }
        return Triple(listOf("Are"), listOf("you"), list)
    }

    /** Load SV_You_question_negative.txt: left = ["Don't"], middle = ["you"], right = verbs. */
    fun loadTripleDataYouQuestionNegative(assetManager: AssetManager): Triple<List<String>, List<String>, List<SvEntry>> {
        val list = mutableListOf<SvEntry>()
        try {
            val path = SVOLessonPaths.svTripleFileName("You", "question_negative")
            assetManager.open(path).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                for (line in reader.readLines()) {
                    val parts = line.split(',', limit = 3).map { it.trim() }
                    if (parts.size < 2) continue
                    val part = parts[0]
                    val bengali = parts[1]
                    val pronunciation = parts.getOrNull(2).orEmpty()
                    if (part.isBlank()) continue
                    val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                    val verb = when {
                        words.size >= 3 && words[0].equals("Don't", ignoreCase = true) && words[1].equals("You", ignoreCase = true) -> words[2] + "?"
                        else -> continue
                    }
                    if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                }
            }
        } catch (_: Exception) { }
        return Triple(listOf("Don't"), listOf("you"), list)
    }

    private val svTripleSubjectOrder = listOf("I", "You", "He", "She", "We", "They")

    /** Load SV_*_negative.txt for all subjects. Returns (subjects, dataPerSubject). */
    fun loadTripleDataSubjectNegative(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val prefix = when (subj) {
                "He", "She" -> "$subj doesn't "
                else -> "$subj don't "
            }
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "negative")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val rest = part.removePrefix(prefix).removePrefix(prefix.replaceFirstChar { it.lowercase() })
                        if (rest == part) continue
                        val verb = rest
                        if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_question.txt for all subjects. Returns (subjects, dataPerSubject). */
    fun loadTripleDataSubjectQuestion(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "question")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                        if (words.size >= 3) {
                            val verb = words[2] + "?"
                            if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_question_negative.txt for all subjects. Returns (subjects, dataPerSubject). */
    fun loadTripleDataSubjectQuestionNegative(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "question_negative")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                        if (words.size >= 3) {
                            val verb = words[2] + "?"
                            if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_Xing.txt for all subjects. Triple: left = subjects, middle = am/is/are, right = verb-ing. */
    fun loadTripleDataCont(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        val auxPrefix = mapOf("I" to "I am ", "He" to "He is ", "She" to "She is ", "You" to "You are ", "We" to "We are ", "They" to "They are ")
        for (subj in svTripleSubjectOrder) {
            val prefix = auxPrefix[subj] ?: continue
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.contIngFileName(subj)).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val verb = part.removePrefix(prefix).removePrefix(prefix.replaceFirstChar { it.lowercase() })
                        if (verb == part) continue
                        if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_cont_question.txt. Returns (subjects, dataPerSubject). */
    fun loadTripleDataContQuestion(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "cont_question")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                        if (words.size >= 3) {
                            val verb = words[2] + "?"
                            if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_cont_negative.txt. Triple: left = subjects, middle = am not/isn't/aren't, right = verb-ing. */
    fun loadTripleDataContNegative(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        val prefixMap = mapOf(
            "I" to "I am not ", "He" to "He isn't ", "She" to "She isn't ",
            "You" to "You aren't ", "We" to "We aren't ", "They" to "They aren't "
        )
        for (subj in svTripleSubjectOrder) {
            val prefix = prefixMap[subj] ?: continue
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "cont_negative")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val verb = part.removePrefix(prefix).removePrefix(prefix.replaceFirstChar { it.lowercase() })
                        if (verb == part) continue
                        if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_cont_question_negative.txt. Returns (subjects, dataPerSubject). */
    fun loadTripleDataContQuestionNegative(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "cont_question_negative")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                        if (words.size >= 3) {
                            val verb = words[2] + "?"
                            if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_past_question.txt. Triple: left = Did, middle = subjects, right = verb?. */
    fun loadTripleDataPastQuestion(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "past_question")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                        if (words.size >= 3) {
                            val verb = words[2] + "?"
                            if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_past_negative.txt. Triple: left = subjects, middle = didn't, right = verb. */
    fun loadTripleDataPastNegative(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val prefix = "$subj didn't "
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "past_negative")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val verb = part.removePrefix(prefix).removePrefix(prefix.replaceFirstChar { it.lowercase() })
                        if (verb == part) continue
                        if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_past_question_negative.txt. Returns (subjects, dataPerSubject). */
    fun loadTripleDataPastQuestionNegative(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "past_question_negative")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                        if (words.size >= 3) {
                            val verb = words[2] + "?"
                            if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_past_cont.txt. Triple: left = subjects, middle = was/were, right = verb-ing. */
    fun loadTripleDataPastCont(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        val auxPrefix = mapOf("I" to "I was ", "He" to "He was ", "She" to "She was ", "You" to "You were ", "We" to "We were ", "They" to "They were ")
        for (subj in svTripleSubjectOrder) {
            val prefix = auxPrefix[subj] ?: continue
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "past_cont")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val verb = part.removePrefix(prefix).removePrefix(prefix.replaceFirstChar { it.lowercase() })
                        if (verb == part) continue
                        if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_past_cont_question.txt. Returns (subjects, dataPerSubject). */
    fun loadTripleDataPastContQuestion(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "past_cont_question")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                        if (words.size >= 3) {
                            val verb = words[2] + "?"
                            if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_past_cont_negative.txt. Triple: left = subjects, middle = wasn't/weren't, right = verb-ing. */
    fun loadTripleDataPastContNegative(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        val prefixMap = mapOf("I" to "I wasn't ", "He" to "He wasn't ", "She" to "She wasn't ", "You" to "You weren't ", "We" to "We weren't ", "They" to "They weren't ")
        for (subj in svTripleSubjectOrder) {
            val prefix = prefixMap[subj] ?: continue
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "past_cont_negative")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val verb = part.removePrefix(prefix).removePrefix(prefix.replaceFirstChar { it.lowercase() })
                        if (verb == part) continue
                        if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_past_cont_question_negative.txt. Returns (subjects, dataPerSubject). */
    fun loadTripleDataPastContQuestionNegative(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "past_cont_question_negative")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                        if (words.size >= 3) {
                            val verb = words[2] + "?"
                            if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_future_question.txt. Returns (subjects, dataPerSubject). */
    fun loadTripleDataFutureQuestion(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "future_question")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                        if (words.size >= 3) {
                            val verb = words[2] + "?"
                            if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_future_negative.txt. Triple: left = subjects, middle = won't, right = verb. */
    fun loadTripleDataFutureNegative(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val prefix = "$subj won't "
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "future_negative")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val verb = part.removePrefix(prefix).removePrefix(prefix.replaceFirstChar { it.lowercase() })
                        if (verb == part) continue
                        if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_future_question_negative.txt. Returns (subjects, dataPerSubject). */
    fun loadTripleDataFutureQuestionNegative(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "future_question_negative")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                        if (words.size >= 3) {
                            val verb = words[2] + "?"
                            if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_future_cont.txt. Triple: left = subjects, middle = will be, right = verb-ing. */
    fun loadTripleDataFutureCont(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        val prefixMap = mapOf("I" to "I will be ", "He" to "He will be ", "She" to "She will be ", "You" to "You will be ", "We" to "We will be ", "They" to "They will be ")
        for (subj in svTripleSubjectOrder) {
            val prefix = prefixMap[subj] ?: continue
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "future_cont")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val verb = part.removePrefix(prefix).removePrefix(prefix.replaceFirstChar { it.lowercase() })
                        if (verb == part) continue
                        if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_future_cont_question.txt. Triple: left = Will, middle = subjects, right = be verb-ing?. */
    fun loadTripleDataFutureContQuestion(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "future_cont_question")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                        if (words.size >= 4) {
                            val verb = "be " + words[3] + "?"
                            if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_future_cont_negative.txt. Triple: left = subjects, middle = won't be, right = verb-ing. */
    fun loadTripleDataFutureContNegative(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        val prefixMap = mapOf("I" to "I won't be ", "He" to "He won't be ", "She" to "She won't be ", "You" to "You won't be ", "We" to "We won't be ", "They" to "They won't be ")
        for (subj in svTripleSubjectOrder) {
            val prefix = prefixMap[subj] ?: continue
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "future_cont_negative")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val verb = part.removePrefix(prefix).removePrefix(prefix.replaceFirstChar { it.lowercase() })
                        if (verb == part) continue
                        if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }

    /** Load SV_*_future_cont_question_negative.txt. Triple: left = Won't, middle = subjects, right = be verb-ing?. */
    fun loadTripleDataFutureContQuestionNegative(assetManager: AssetManager): Pair<List<String>, Map<String, List<SvEntry>>> {
        val dataPerSubject = mutableMapOf<String, MutableList<SvEntry>>()
        for (subj in svTripleSubjectOrder) {
            val list = mutableListOf<SvEntry>()
            try {
                assetManager.open(SVOLessonPaths.svTripleFileName(subj, "future_cont_question_negative")).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    for (line in reader.readLines()) {
                        val parts = line.split(',', limit = 3).map { it.trim() }
                        if (parts.size < 2) continue
                        val part = parts[0]
                        val bengali = parts[1]
                        val pronunciation = parts.getOrNull(2).orEmpty()
                        if (part.isBlank()) continue
                        val words = part.removeSuffix("?").trim().split(Regex("\\s+"))
                        if (words.size >= 4) {
                            val verb = "be " + words[3] + "?"
                            if (verb.isNotBlank()) list.add(SvEntry(verb, bengali, pronunciation))
                        }
                    }
                }
            } catch (_: Exception) { }
            if (list.isNotEmpty()) dataPerSubject[subj] = list
        }
        val subjects = svTripleSubjectOrder.filter { it in dataPerSubject }
        return subjects to dataPerSubject
    }
}
