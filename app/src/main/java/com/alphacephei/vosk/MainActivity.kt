package com.alphacephei.vosk

import android.Manifest.permission.INTERNET
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread
import kotlin.math.max
import java.util.Locale
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateLanguage


private const val TAG = "com.alphacephei.vosk"

// Set to true to test: mic will record but NOT call recognizer (to see if crash is in native lib).
// Set to false for normal use so speech recognition runs.
private const val SKIP_RECOGNITION_FOR_MIC = false

class MainActivity : AppCompatActivity() {

    private val permList: Array<String> = arrayOf(RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, INTERNET)

    companion object {
        private const val PERMISSION_CODE = 100
        private const val LIST_FILE_SUFFIX = ".json"
        private const val INCORRECT_LESSON_FILE = "incorrect_lesson.json"
    }

    /** A sentence in the list: text and whether it was spoken in Bengali (true) or English (false). */
    data class Sentence(val text: String, val isBengali: Boolean)

    /** One row from a lesson file: A=English Q, B=Bengali Q, C=English A, D=Bengali A (pipe-separated). */
    data class LessonRow(val engQ: String, val bnQ: String, val engA: String, val bnA: String)

    /** One row for verb conjugation: English sentence, Bengali sentence. */
    private data class VerbRow(val english: String, val bengali: String)

    /** Built-in verb conjugations: DO, HAVE, GO (Subject | English | Bengali). */
    private fun getVerbData(): Map<String, List<VerbRow>> = mapOf(
        "DO" to listOf(
            VerbRow("I do.", "আমি করি।"),
            VerbRow("You do.", "তুমি কর।"),
            VerbRow("He does.", "সে (ছেলে) করে।"),
            VerbRow("She does.", "সে (মেয়ে) করে।"),
            VerbRow("We do.", "আমরা করি।"),
            VerbRow("They do.", "তারা করে।")
        ),
        "HAVE" to listOf(
            VerbRow("I have.", "আমার আছে।"),
            VerbRow("You have.", "তোমার আছে।"),
            VerbRow("He has.", "তার আছে।"),
            VerbRow("She has.", "তার আছে।"),
            VerbRow("We have.", "আমাদের আছে।"),
            VerbRow("They have.", "তাদের আছে।")
        ),
        "GO" to listOf(
            VerbRow("I go.", "আমি যাই।"),
            VerbRow("You go.", "তুমি যাও।"),
            VerbRow("He goes.", "সে যায়।"),
            VerbRow("She goes.", "সে যায়।"),
            VerbRow("We go.", "আমরা যাই।"),
            VerbRow("They go.", "তারা যায়।")
        )
    )

    /** Build lesson rows from a verb: speak Bengali, user says English, match to English. */
    private fun buildLessonFromVerb(verbName: String): List<LessonRow> {
        val rows = getVerbData()[verbName] ?: return emptyList()
        return rows.map { v -> LessonRow(v.english, v.bengali, v.english, v.bengali) }
    }

    /** 12 tenses per (verb, subject): verb -> subject -> list of (English, Bengali). */
    private fun getTenseData(): Map<String, Map<String, List<VerbRow>>> {
        val eatI = listOf(
            VerbRow("I eat.", "আমি খাই।"),
            VerbRow("I am eating.", "আমি খাচ্ছি।"),
            VerbRow("I have eaten.", "আমি খেয়েছি।"),
            VerbRow("I have been eating.", "আমি (কিছুক্ষণ ধরে) খাচ্ছি।"),
            VerbRow("I ate.", "আমি খেয়েছিলাম।"),
            VerbRow("I was eating.", "আমি খাচ্ছিলাম।"),
            VerbRow("I had eaten.", "আমি খেয়েছিলাম (অন্য কাজের আগে)।"),
            VerbRow("I had been eating.", "আমি (কিছুক্ষণ ধরে) খাচ্ছিলাম।"),
            VerbRow("I will eat.", "আমি খাব।"),
            VerbRow("I will be eating.", "আমি খেতে থাকব।"),
            VerbRow("I will have eaten.", "আমি খেয়ে থাকব।"),
            VerbRow("I will have been eating.", "আমি (কিছুক্ষণ ধরে) খেতে থাকব।")
        )
        val eatYou = listOf(
            VerbRow("You eat.", "তুমি খাও।"),
            VerbRow("You are eating.", "তুমি খাচ্ছো।"),
            VerbRow("You have eaten.", "তুমি খেয়েছো।"),
            VerbRow("You have been eating.", "তুমি (কিছুক্ষণ ধরে) খাচ্ছো।"),
            VerbRow("You ate.", "তুমি খেয়েছিলে।"),
            VerbRow("You were eating.", "তুমি খাচ্ছিলে।"),
            VerbRow("You had eaten.", "তুমি খেয়েছিলে (অন্য কাজের আগে)।"),
            VerbRow("You had been eating.", "তুমি (কিছুক্ষণ ধরে) খাচ্ছিলে।"),
            VerbRow("You will eat.", "তুমি খাবে।"),
            VerbRow("You will be eating.", "তুমি খেতে থাকবে।"),
            VerbRow("You will have eaten.", "তুমি খেয়ে থাকবে।"),
            VerbRow("You will have been eating.", "তুমি (কিছুক্ষণ ধরে) খেতে থাকবে।")
        )
        val eatHe = listOf(
            VerbRow("He eats.", "সে খায়।"),
            VerbRow("He is eating.", "সে খাচ্ছে।"),
            VerbRow("He has eaten.", "সে খেয়েছে।"),
            VerbRow("He has been eating.", "সে (কিছুক্ষণ ধরে) খাচ্ছে।"),
            VerbRow("He ate.", "সে খেয়েছিল।"),
            VerbRow("He was eating.", "সে খাচ্ছিল।"),
            VerbRow("He had eaten.", "সে খেয়েছিল (অন্য কাজের আগে)।"),
            VerbRow("He had been eating.", "সে (কিছুক্ষণ ধরে) খাচ্ছিল।"),
            VerbRow("He will eat.", "সে খাবে।"),
            VerbRow("He will be eating.", "সে খেতে থাকবে।"),
            VerbRow("He will have eaten.", "সে খেয়ে থাকবে।"),
            VerbRow("He will have been eating.", "সে (কিছুক্ষণ ধরে) খেতে থাকবে।")
        )
        val eatShe = listOf(
            VerbRow("She eats.", "সে খায়।"),
            VerbRow("She is eating.", "সে খাচ্ছে।"),
            VerbRow("She has eaten.", "সে খেয়েছে।"),
            VerbRow("She has been eating.", "সে (কিছুক্ষণ ধরে) খাচ্ছে।"),
            VerbRow("She ate.", "সে খেয়েছিল।"),
            VerbRow("She was eating.", "সে খাচ্ছিল।"),
            VerbRow("She had eaten.", "সে খেয়েছিল (অন্য কাজের আগে)।"),
            VerbRow("She had been eating.", "সে (কিছুক্ষণ ধরে) খাচ্ছিল।"),
            VerbRow("She will eat.", "সে খাবে।"),
            VerbRow("She will be eating.", "সে খেতে থাকবে।"),
            VerbRow("She will have eaten.", "সে খেয়ে থাকবে।"),
            VerbRow("She will have been eating.", "সে (কিছুক্ষণ ধরে) খেতে থাকবে।")
        )
        val eatWe = listOf(
            VerbRow("We eat.", "আমরা খাই।"),
            VerbRow("We are eating.", "আমরা খাচ্ছি।"),
            VerbRow("We have eaten.", "আমরা খেয়েছি।"),
            VerbRow("We have been eating.", "আমরা (কিছুক্ষণ ধরে) খাচ্ছি।"),
            VerbRow("We ate.", "আমরা খেয়েছিলাম।"),
            VerbRow("We were eating.", "আমরা খাচ্ছিলাম।"),
            VerbRow("We had eaten.", "আমরা খেয়েছিলাম (অন্য কাজের আগে)।"),
            VerbRow("We had been eating.", "আমরা (কিছুক্ষণ ধরে) খাচ্ছিলাম।"),
            VerbRow("We will eat.", "আমরা খাব।"),
            VerbRow("We will be eating.", "আমরা খেতে থাকব।"),
            VerbRow("We will have eaten.", "আমরা খেয়ে থাকব।"),
            VerbRow("We will have been eating.", "আমরা (কিছুক্ষণ ধরে) খেতে থাকব।")
        )
        val eatThey = listOf(
            VerbRow("They eat.", "তারা খায়।"),
            VerbRow("They are eating.", "তারা খাচ্ছে।"),
            VerbRow("They have eaten.", "তারা খেয়েছে।"),
            VerbRow("They have been eating.", "তারা (কিছুক্ষণ ধরে) খাচ্ছে।"),
            VerbRow("They ate.", "তারা খেয়েছিল।"),
            VerbRow("They were eating.", "তারা খাচ্ছিল।"),
            VerbRow("They had eaten.", "তারা খেয়েছিল (অন্য কাজের আগে)।"),
            VerbRow("They had been eating.", "তারা (কিছুক্ষণ ধরে) খাচ্ছিল।"),
            VerbRow("They will eat.", "তারা খাবে।"),
            VerbRow("They will be eating.", "তারা খেতে থাকবে।"),
            VerbRow("They will have eaten.", "তারা খেয়ে থাকবে।"),
            VerbRow("They will have been eating.", "তারা (কিছুক্ষণ ধরে) খেতে থাকবে।")
        )
        val doI = listOf(
            VerbRow("I do.", "আমি করি।"),
            VerbRow("I am doing.", "আমি করছি।"),
            VerbRow("I have done.", "আমি করেছি।"),
            VerbRow("I have been doing.", "আমি (কিছুক্ষণ ধরে) করছি।"),
            VerbRow("I did.", "আমি করেছিলাম।"),
            VerbRow("I was doing.", "আমি করছিলাম।"),
            VerbRow("I had done.", "আমি করেছিলাম (অন্য কাজের আগে)।"),
            VerbRow("I had been doing.", "আমি (কিছুক্ষণ ধরে) করছিলাম।"),
            VerbRow("I will do.", "আমি করব।"),
            VerbRow("I will be doing.", "আমি করতে থাকব।"),
            VerbRow("I will have done.", "আমি করে থাকব।"),
            VerbRow("I will have been doing.", "আমি (কিছুক্ষণ ধরে) করতে থাকব।")
        )
        return mapOf(
            "EAT" to mapOf(
                "I" to eatI, "You" to eatYou, "He" to eatHe, "She" to eatShe, "We" to eatWe, "They" to eatThey
            ),
            "DO" to mapOf(
                "I" to doI,
                "You" to listOf(
                    VerbRow("You do.", "তুমি কর।"), VerbRow("You are doing.", "তুমি করছো।"), VerbRow("You have done.", "তুমি করেছো।"),
                    VerbRow("You have been doing.", "তুমি (কিছুক্ষণ ধরে) করছো।"), VerbRow("You did.", "তুমি করেছিলে।"), VerbRow("You were doing.", "তুমি করছিলে।"),
                    VerbRow("You had done.", "তুমি করেছিলে (অন্য কাজের আগে)।"), VerbRow("You had been doing.", "তুমি (কিছুক্ষণ ধরে) করছিলে।"),
                    VerbRow("You will do.", "তুমি করবে।"), VerbRow("You will be doing.", "তুমি করতে থাকবে।"), VerbRow("You will have done.", "তুমি করে থাকবে।"),
                    VerbRow("You will have been doing.", "তুমি (কিছুক্ষণ ধরে) করতে থাকবে।")
                ),
                "He" to listOf(
                    VerbRow("He does.", "সে করে।"), VerbRow("He is doing.", "সে করছে।"), VerbRow("He has done.", "সে করেছে।"),
                    VerbRow("He has been doing.", "সে (কিছুক্ষণ ধরে) করছে।"), VerbRow("He did.", "সে করেছিল।"), VerbRow("He was doing.", "সে করছিল।"),
                    VerbRow("He had done.", "সে করেছিল (অন্য কাজের আগে)।"), VerbRow("He had been doing.", "সে (কিছুক্ষণ ধরে) করছিল।"),
                    VerbRow("He will do.", "সে করবে।"), VerbRow("He will be doing.", "সে করতে থাকবে।"), VerbRow("He will have done.", "সে করে থাকবে।"),
                    VerbRow("He will have been doing.", "সে (কিছুক্ষণ ধরে) করতে থাকবে।")
                ),
                "She" to listOf(
                    VerbRow("She does.", "সে করে।"), VerbRow("She is doing.", "সে করছে।"), VerbRow("She has done.", "সে করেছে।"),
                    VerbRow("She has been doing.", "সে (কিছুক্ষণ ধরে) করছে।"), VerbRow("She did.", "সে করেছিল।"), VerbRow("She was doing.", "সে করছিল।"),
                    VerbRow("She had done.", "সে করেছিল (অন্য কাজের আগে)।"), VerbRow("She had been doing.", "সে (কিছুক্ষণ ধরে) করছিল।"),
                    VerbRow("She will do.", "সে করবে।"), VerbRow("She will be doing.", "সে করতে থাকবে।"), VerbRow("She will have done.", "সে করে থাকবে।"),
                    VerbRow("She will have been doing.", "সে (কিছুক্ষণ ধরে) করতে থাকবে।")
                ),
                "We" to listOf(
                    VerbRow("We do.", "আমরা করি।"), VerbRow("We are doing.", "আমরা করছি।"), VerbRow("We have done.", "আমরা করেছি।"),
                    VerbRow("We have been doing.", "আমরা (কিছুক্ষণ ধরে) করছি।"), VerbRow("We did.", "আমরা করেছিলাম।"), VerbRow("We were doing.", "আমরা করছিলাম।"),
                    VerbRow("We had done.", "আমরা করেছিলাম (অন্য কাজের আগে)।"), VerbRow("We had been doing.", "আমরা (কিছুক্ষণ ধরে) করছিলাম।"),
                    VerbRow("We will do.", "আমরা করব।"), VerbRow("We will be doing.", "আমরা করতে থাকব।"), VerbRow("We will have done.", "আমরা করে থাকব।"),
                    VerbRow("We will have been doing.", "আমরা (কিছুক্ষণ ধরে) করতে থাকব।")
                ),
                "They" to listOf(
                    VerbRow("They do.", "তারা করে।"), VerbRow("They are doing.", "তারা করছে।"), VerbRow("They have done.", "তারা করেছে।"),
                    VerbRow("They have been doing.", "তারা (কিছুক্ষণ ধরে) করছে।"), VerbRow("They did.", "তারা করেছিল।"), VerbRow("They were doing.", "তারা করছিল।"),
                    VerbRow("They had done.", "তারা করেছিল (অন্য কাজের আগে)।"), VerbRow("They had been doing.", "তারা (কিছুক্ষণ ধরে) করছিল।"),
                    VerbRow("They will do.", "তারা করবে।"), VerbRow("They will be doing.", "তারা করতে থাকবে।"), VerbRow("They will have done.", "তারা করে থাকবে।"),
                    VerbRow("They will have been doing.", "তারা (কিছুক্ষণ ধরে) করতে থাকবে।")
                )
            )
        )
    }

    /** Build lesson from (verb, subject): 12 tenses, speak Bengali, user says English. */
    private fun buildLessonFromTense(verbName: String, subjectName: String): List<LessonRow> {
        val rows = getTenseData()[verbName]?.get(subjectName) ?: return emptyList()
        return rows.map { v -> LessonRow(v.english, v.bengali, v.english, v.bengali) }
    }

    /** Show verb picker for tense lesson, then subject picker. */
    private fun showTenseVerbSelectorDialog() {
        val verbs = getTenseData().keys.toList().sorted()
        if (verbs.isEmpty()) {
            Toast.makeText(this, "No tense data", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_verb_title))
            .setItems(verbs.toTypedArray()) { _, which ->
                val verbName = verbs[which]
                val subjects = getTenseData()[verbName]?.keys?.toList()?.sorted() ?: return@setItems
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.select_subject_title))
                    .setItems(subjects.toTypedArray()) inner@{ _, subWhich ->
                        val subjectName = subjects[subWhich]
                        val rows = buildLessonFromTense(verbName, subjectName)
                        if (rows.isEmpty()) return@inner
                        lessonRows = rows
                        lessonName = "tenses_${verbName}_$subjectName"
                        lessonMode = 4
                        lessonIndex = 0
                        lessonPhase = "q"
                        lessonMode3Listening = false
                        lessonMode3SpokeAnswer = false
                        lessonIncorrectCount = 0
                        nextButton?.isEnabled = true
                        skipButton?.isEnabled = true
                        clearBothTextAreas()
                        textView.text = getString(R.string.lesson_loaded)
                        Toast.makeText(this, getString(R.string.lesson_loaded), Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Current lesson: rows + name + mode (1/2/3) + index + for mode 1: phase "q" or "a". */
    private var lessonRows: List<LessonRow>? = null
    private var lessonName: String? = null
    private var lessonMode: Int = 0
    private var lessonIndex: Int = 0
    private var lessonPhase: String = "q"
    /** Mode 3: true while waiting for user to speak question in English; on result we speak C. */
    private var lessonMode3Listening: Boolean = false
    /** Mode 3: true after we spoke C for current row; Next then advances to next row. */
    private var lessonMode3SpokeAnswer: Boolean = false
    /** Incorrect attempts for current lesson item; after 3 we auto-advance to next. */
    private var lessonIncorrectCount: Int = 0
    /** Rows that were answered incorrectly; saved to file and loadable as "Practice incorrect words". */
    private val incorrectLessonRows = mutableListOf<LessonRow>()
    /** Original lesson name for the incorrect list (saved to file); displayed as name + "_inc". */
    private var incorrectLessonSourceName: String? = null

    private val openLessonLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val content = InputStreamReader(stream).readText()
                val rows = parseLessonFile(content)
                if (rows.isEmpty()) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "No valid rows (need 4 columns per line)", Toast.LENGTH_SHORT).show() }
                    return@registerForActivityResult
                }
                val name = getDisplayName(uri)?.removeSuffix(".txt") ?: "lesson"
                runOnUiThread { showModeSelectorDialog(rows, name) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Read lesson failed", e)
            runOnUiThread { Toast.makeText(this@MainActivity, "Read failed: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun getDisplayName(uri: Uri): String? {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) return c.getString(i)
            }
        }
        return null
    }

    /** Parse pipe-separated lines into LessonRow. A|B|C|D = engQ|bnQ|engA|bnA. */
    private fun parseLessonFile(content: String): List<LessonRow> {
        val rows = mutableListOf<LessonRow>()
        content.lines().forEach { line ->
            val parts = line.split("|").map { it.trim() }
            if (parts.size >= 4) {
                rows.add(LessonRow(parts[0], parts[1], parts[2], parts[3]))
            }
        }
        return rows
    }

    /** Parse Regular_verbs.txt: tab-separated, first line header. Columns: Root (V1), Past (V2/V3), Bengali Meaning. */
    private fun parseRegularVerbsFile(content: String): List<LessonRow> {
        val rows = mutableListOf<LessonRow>()
        val lines = content.lines()
        if (lines.size < 2) return rows
        for (i in 1 until lines.size) {
            val parts = lines[i].split("\t").map { it.trim() }
            if (parts.size >= 3) {
                val root = parts[0]
                val bengali = parts[2]
                if (root.isNotBlank() && bengali.isNotBlank()) {
                    rows.add(LessonRow(root, bengali, root, bengali))
                }
            }
        }
        return rows
    }

    /** Parse Irregular_verbs.txt: comma-separated, first line header. Columns: Root (V1), Past (V2), Past Participle (V3), Bengali Meaning. */
    private fun parseIrregularVerbsFile(content: String): List<LessonRow> {
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

    private fun showModeSelectorDialog(rows: List<LessonRow>, name: String) {
        val modes = arrayOf(
            getString(R.string.mode_1_title) + "\n" + getString(R.string.mode_1_desc),
            getString(R.string.mode_2_title) + "\n" + getString(R.string.mode_2_desc),
            getString(R.string.mode_3_title) + "\n" + getString(R.string.mode_3_desc)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_mode_title))
            .setItems(modes) { _, which ->
                lessonRows = rows
                lessonName = name
                lessonMode = which + 1
                lessonIndex = 0
                lessonPhase = "q"
                lessonMode3Listening = false
                lessonMode3SpokeAnswer = false
                lessonIncorrectCount = 0
                nextButton?.isEnabled = true
                skipButton?.isEnabled = true
                clearBothTextAreas()
                textView.text = getString(R.string.lesson_loaded)
                Toast.makeText(this, getString(R.string.lesson_loaded), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Show verb picker (DO, HAVE, GO); on select build lesson and start mode 4 (hear Bengali, say English). */
    private fun showVerbSelectorDialog() {
        val verbs = getVerbData().keys.toList().sorted()
        if (verbs.isEmpty()) {
            Toast.makeText(this, "No verbs defined", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_verb_title))
            .setItems(verbs.toTypedArray()) { _, which ->
                val verbName = verbs[which]
                val rows = buildLessonFromVerb(verbName)
                if (rows.isEmpty()) return@setItems
                lessonRows = rows
                lessonName = "verb_$verbName"
                lessonMode = 4
                lessonIndex = 0
                lessonPhase = "q"
                lessonMode3Listening = false
                lessonMode3SpokeAnswer = false
                lessonIncorrectCount = 0
                nextButton?.isEnabled = true
                skipButton?.isEnabled = true
                clearBothTextAreas()
                textView.text = getString(R.string.lesson_loaded)
                Toast.makeText(this, getString(R.string.lesson_loaded), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request one permission at a time (Android allows only one request at a time)
        requestNextNeededPermission()

        onCreateSpotter()
        setupUI()
        initTextToSpeech()
        initTranslator()
        // When app loads, clear all text areas and sentence list
        clearBothTextAreas()
        clearSentenceListUi()
    }

    override fun onResume() {
        super.onResume()
        // When user returns to the app (from another app or home), clear all visible text and list
        clearBothTextAreas()
        clearSentenceListUi()
    }

    /** Request the first permission that is not yet granted. */
    private fun requestNextNeededPermission() {
        for (p in permList) {
            if (!checkForPermission(p)) {
                ActivityCompat.requestPermissions(this, arrayOf(p), PERMISSION_CODE)
                return
            }
        }
    }

    /** Check using Activity context so "While using this app" is recognized. */
    private fun checkForPermission(permission: String): Boolean {
        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "checkForPermission($permission)=$granted")
        return granted
    }

    private fun onPermissionResult(permission: String, granted: Boolean) {
        Log.d(TAG, "onPermissionResult($permission)=$granted")
        if (granted && permission == RECORD_AUDIO) {
            Toast.makeText(this, "Microphone allowed", Toast.LENGTH_SHORT).show()
        } else if (!granted && permission == RECORD_AUDIO) {
            // Only show "denied" for mic; Storage/Internet are optional and can be denied on startup
            Toast.makeText(this, "Microphone permission denied; enable it in Settings to use Start Microphone.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_CODE || grantResults.isEmpty() || permissions.isEmpty()) return
        // We request one permission at a time, so there is only one result
        val permission = permissions[0]
        val granted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        onPermissionResult(permission, granted)
        // If granted, ask for the next missing permission (or no-op if all granted)
        if (granted) requestNextNeededPermission()
    }


    enum class ModelStatus {
        MODEL_STATUS_INIT,
        MODEL_STATUS_START,
        MODEL_STATUS_READY
    }

    private lateinit var recognizer: Recognizer

    private var modelStatus = ModelStatus.MODEL_STATUS_INIT

    private var audioRecord: AudioRecord? = null
    private lateinit var micButton: ImageButton
    private lateinit var textView: TextView
    private lateinit var englishTextView: TextView
    private lateinit var translationLabel: TextView
    private lateinit var inputLanguageGroup: RadioGroup
    private lateinit var sentenceRecyclerView: RecyclerView
    private val sentenceList = mutableListOf<Sentence>()
    private lateinit var sentenceAdapter: SentenceAdapter
    private var currentNextIndex = 0
    private var nextButton: ImageButton? = null
    private var skipButton: ImageButton? = null
    private var recordingThread: Thread? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var translator: Translator? = null
    private var translatorEnToBn: Translator? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    /** When true, we're waiting for user to speak English to verify against expected translation. */
    @Volatile
    private var verificationMode = false
    private var expectedEnglishForVerification: String? = null
    /** Set before speaking Bengali for verification; when TTS onDone fires we start mic. */
    private var pendingVerificationExpectedEnglish: String? = null
    /** Guard: only process one verification result per listening session (avoids double-counting). */
    @Volatile
    private var verificationResultHandled = false
    /** When non-null, TTS onDone("incorrect_then_correct") will speak this word (correct pronunciation). */
    private var pendingSpeakCorrectWordAfterIncorrect: String? = null
    /** After speaking correct word, restart listening with this expected (so user can try again without pressing Next). */
    private var pendingRestartVerificationWith: String? = null
    /** Fallback: speak correct word if onDone does not fire. */
    private var incorrectFeedbackFallbackRunnable: Runnable? = null
    private val verificationHandler = Handler(Looper.getMainLooper())
    private var verificationTimeoutRunnable: Runnable? = null
    @Volatile
    private var currentRecordingIsBengali = true

    private val channelConfig = AudioFormat.CHANNEL_IN_MONO

    // Note: We don't use AudioFormat.ENCODING_PCM_FLOAT
    // since the AudioRecord.read(float[]) needs API level >= 23
    // but we are targeting API level >= 21
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var idx: Int = 0
    private var lastText: String = ""


    @Volatile
    private var isRecording: Boolean = false

    /** True when we are listening with SpeechRecognizer (English input) rather than Sherpa (Bengali). */
    @Volatile
    private var isEnglishMicActive = false

    @Volatile
    private var modelLoaded = false

    private fun onCreateSpotter() {

        recognizer = Recognizer()

        CoroutineScope(Dispatchers.IO).launch {
            modelStatus = ModelStatus.MODEL_STATUS_START
            try {
                withContext(Dispatchers.IO) {
                    recognizer.initModel(this@MainActivity)
                }
                modelLoaded = true
                Log.d(TAG, "Done creating model!!!")
                runOnUiThread {
                    modelStatus = ModelStatus.MODEL_STATUS_READY
                    micButton.isEnabled = true
                    textView.text = getText(R.string.ready_to_start)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Model load failed", e)
                runOnUiThread {
                    modelStatus = ModelStatus.MODEL_STATUS_INIT
                    micButton.isEnabled = false
                    textView.text = "Model failed to load:\n${e.message}\n\nCheck that native .so files are in jniLibs."
                    Toast.makeText(this@MainActivity, "Model load failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupUI() {

        micButton = findViewById(R.id.mic_button)
        micButton.setOnClickListener {
            Log.i(TAG, "MIC_BUTTON_CLICK (listener entered)")
            onDemo()
        }

        textView = findViewById(R.id.my_text)
        textView.movementMethod = ScrollingMovementMethod()

        englishTextView = findViewById(R.id.english_text)
        englishTextView.movementMethod = ScrollingMovementMethod()

        translationLabel = findViewById(R.id.translation_label)
        inputLanguageGroup = findViewById(R.id.input_language_group)
        updateTranslationLabelAndButton()
        inputLanguageGroup.setOnCheckedChangeListener { _, _ -> updateTranslationLabelAndButton() }

        sentenceRecyclerView = findViewById(R.id.sentence_list)
        sentenceRecyclerView.layoutManager = LinearLayoutManager(this)
        sentenceAdapter = SentenceAdapter(sentenceList) { position -> removeSentenceAt(position) }
        sentenceRecyclerView.adapter = sentenceAdapter
        ItemTouchHelper(sentenceDragCallback).attachToRecyclerView(sentenceRecyclerView)

        findViewById<ImageButton>(R.id.save_list_button).setOnClickListener { showSaveListDialog() }
        findViewById<ImageButton>(R.id.load_list_button).setOnClickListener { showLoadListDialog() }
        nextButton = findViewById(R.id.next_button)
        nextButton?.setOnClickListener {
            if (lessonRows != null) onNextLessonStep() else onNextSentence()
        }
        skipButton = findViewById(R.id.skip_button)
        skipButton?.setOnClickListener { onSkipWord() }

        if (modelStatus == ModelStatus.MODEL_STATUS_INIT || modelStatus == ModelStatus.MODEL_STATUS_START) {
            micButton.isEnabled = false
            textView.text = getText(R.string.hint)
        } else {
            micButton.isEnabled = true
            textView.text = getText(R.string.ready_to_start)
        }
        setMicButtonAppearance(recording = false)
    }

    /** Sets mic button icon and color: blue for Start (mic), red for Stop. */
    private fun setMicButtonAppearance(recording: Boolean) {
        if (recording) {
            micButton.setImageResource(R.drawable.ic_stop)
            micButton.setColorFilter(ContextCompat.getColor(this, R.color.mic_color_stop))
        } else {
            micButton.setImageResource(R.drawable.ic_mic)
            micButton.setColorFilter(ContextCompat.getColor(this, R.color.mic_color_start))
        }
    }

    /** Offline Bengali Text-to-Speech: uses system TTS with Bengali language (works offline if Bengali voice is installed). */
    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val bengaliLocale = Locale("bn")
                val availability = textToSpeech?.isLanguageAvailable(bengaliLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                val statusMessage = when (availability) {
                    TextToSpeech.LANG_AVAILABLE -> "Bengali voice: Available (offline)"
                    TextToSpeech.LANG_COUNTRY_AVAILABLE -> "Bengali voice: Available (offline)"
                    TextToSpeech.LANG_MISSING_DATA -> "Bengali voice: Not installed – install in Settings → Text-to-speech"
                    TextToSpeech.LANG_NOT_SUPPORTED -> "Bengali voice: Not supported on this device"
                    else -> "Bengali voice: Available (offline)" // LANG_COUNTRY_DEFAULT (API 21+) or other positive
                }
                Log.d(TAG, "TTS Bengali: $statusMessage")
                val result = textToSpeech?.setLanguage(bengaliLocale)
                when (result) {
                    TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                        Log.w(TAG, "Bengali TTS not available; trying default locale")
                        textToSpeech?.setLanguage(Locale.getDefault())
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, statusMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                    else -> {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, statusMessage, Toast.LENGTH_SHORT).show()
                        }
                        Log.d(TAG, "Bengali TTS ready (offline)")
                    }
                }
                ttsReady = true
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        when (utteranceId) {
                            "bengali_verification" -> {
                                runOnUiThread {
                                    val expected = pendingVerificationExpectedEnglish
                                    pendingVerificationExpectedEnglish = null
                                    if (expected != null && !isDestroyed) startVerificationListening(expected)
                                }
                            }
                            "lesson_verify" -> {
                                runOnUiThread {
                                    val expected = expectedEnglishForVerification
                                    if (expected != null && !isDestroyed) startVerificationListening(expected)
                                }
                            }
                            "incorrect_then_correct" -> {
                                runOnUiThread {
                                    incorrectFeedbackFallbackRunnable?.let { verificationHandler.removeCallbacks(it) }
                                    incorrectFeedbackFallbackRunnable = null
                                    val word = pendingSpeakCorrectWordAfterIncorrect
                                    pendingSpeakCorrectWordAfterIncorrect = null
                                    if (!word.isNullOrBlank() && !isDestroyed && ttsReady && textToSpeech != null) {
                                        textToSpeech?.setLanguage(Locale.US)
                                        textToSpeech?.speak(word, TextToSpeech.QUEUE_ADD, null, "english_segment_tts")
                                    }
                                    val toRestart = pendingRestartVerificationWith
                                    if (!toRestart.isNullOrBlank() && !isDestroyed) {
                                        pendingRestartVerificationWith = null
                                        verificationHandler.postDelayed({
                                            if (!isDestroyed && toRestart.isNotBlank()) startVerificationListening(toRestart)
                                        }, 1800)
                                    }
                                }
                            }
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        if (utteranceId == "bengali_verification" || utteranceId == "lesson_verify" || utteranceId == "incorrect_then_correct") {
                            runOnUiThread {
                                pendingVerificationExpectedEnglish = null
                                if (utteranceId == "lesson_verify") expectedEnglishForVerification = null
                                if (utteranceId == "incorrect_then_correct") pendingSpeakCorrectWordAfterIncorrect = null
                                if (!isDestroyed) Toast.makeText(this@MainActivity, "Speech failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                })
            } else {
                Log.e(TAG, "TTS init failed: $status")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Text-to-speech failed to initialize", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** Bengali→English and English→Bengali translators (ML Kit, offline after model download). */
    private fun initTranslator() {
        val bnToEn = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.BENGALI)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        translator = Translation.getClient(bnToEn)
        val enToBn = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.BENGALI)
            .build()
        translatorEnToBn = Translation.getClient(enToBn)
        Log.d(TAG, "Translators (Bn↔En) created")
    }

    private fun translateBengaliToEnglish() {
        translateBengaliToEnglishInternal(showToasts = true)
    }

    /** Translates current Bengali text to English and shows it. Optionally speaks English (used by button and by auto flow). */
    private fun translateBengaliToEnglishInternal(showToasts: Boolean) {
        val trans = translator
        if (trans == null) {
            if (showToasts) Toast.makeText(this, "Translator not ready", Toast.LENGTH_SHORT).show()
            return
        }
        val bengaliText = textView.text.toString().trim()
        if (bengaliText.isEmpty()) {
            if (showToasts) Toast.makeText(this, "No Bengali text to translate. Capture speech first.", Toast.LENGTH_SHORT).show()
            return
        }
        if (showToasts) Toast.makeText(this, "Translating… (downloading model if needed)", Toast.LENGTH_SHORT).show()
        trans.downloadModelIfNeeded()
            .addOnSuccessListener {
                trans.translate(bengaliText)
                    .addOnSuccessListener { translated ->
                        runOnUiThread {
                            englishTextView.text = translated
                            if (showToasts) Toast.makeText(this@MainActivity, "Translated", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Translated: $translated")
                            speakEnglishText()
                        }
                    }
                    .addOnFailureListener { e ->
                        runOnUiThread {
                            if (showToasts) Toast.makeText(this@MainActivity, "Translation failed: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e(TAG, "Translation failed", e)
                        }
                    }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    if (showToasts) Toast.makeText(this@MainActivity, "Model download failed. Try with internet: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Model download failed", e)
                }
            }
    }

    /** Called when mic detects end of speech (and we stop): translate full text and speak English. */
    private fun translateAndSpeakEnglish() {
        translateBengaliToEnglishInternal(showToasts = false)
    }

    /** Manual translate: English (textView) → Bengali (englishTextView), then speak Bengali. */
    private fun translateEnglishToBengali() {
        val englishText = textView.text.toString().trim()
        if (englishText.isEmpty()) {
            Toast.makeText(this, "No English text to translate. Speak or type first.", Toast.LENGTH_SHORT).show()
            return
        }
        val trans = translatorEnToBn ?: return
        Toast.makeText(this, "Translating… (downloading model if needed)", Toast.LENGTH_SHORT).show()
        trans.downloadModelIfNeeded()
            .addOnSuccessListener {
                trans.translate(englishText)
                    .addOnSuccessListener { bengaliText ->
                        runOnUiThread {
                            englishTextView.text = bengaliText
                            Toast.makeText(this@MainActivity, "Translated", Toast.LENGTH_SHORT).show()
                            speakBengaliString(bengaliText)
                        }
                    }
                    .addOnFailureListener { e ->
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Translation failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Download model (internet needed): ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /** Translate current sentence (segment) to English, append to display, and speak it. Mic keeps running. */
    private fun addSentenceToList(text: String, isBengali: Boolean) {
        if (text.isBlank()) return
        runOnUiThread {
            sentenceList.add(Sentence(text.trim(), isBengali))
            sentenceAdapter.notifyItemInserted(sentenceList.size - 1)
            sentenceRecyclerView.scrollToPosition(sentenceList.size - 1)
        }
    }

    private fun translateSegmentAndSpeakEnglish(bengaliSegment: String) {
        if (bengaliSegment.isBlank()) return
        addSentenceToList(bengaliSegment, isBengali = true)
        val trans = translator ?: return
        trans.downloadModelIfNeeded()
            .addOnSuccessListener {
                trans.translate(bengaliSegment)
                    .addOnSuccessListener { translated ->
                        runOnUiThread {
                            val current = englishTextView.text.toString().trim()
                            englishTextView.text = if (current.isEmpty()) translated else "$current\n$translated"
                            speakEnglishString(translated)
                            Log.d(TAG, "Segment translated and spoken: $translated")
                        }
                    }
                    .addOnFailureListener { e ->
                        runOnUiThread {
                            Log.e(TAG, "Segment translation failed", e)
                        }
                    }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    Log.e(TAG, "Segment model download failed", e)
                }
            }
    }

    /** Speak a single English string (uses QUEUE_ADD so multiple sentences can queue). */
    private fun speakEnglishString(englishText: String) {
        if (englishText.isBlank() || !ttsReady || textToSpeech == null) return
        textToSpeech?.setLanguage(Locale.US)
        textToSpeech?.speak(englishText, TextToSpeech.QUEUE_ADD, null, "english_segment_tts")
    }

    private fun speakBengaliText() {
        if (!ttsReady || textToSpeech == null) {
            Toast.makeText(this, "Speech not ready yet", Toast.LENGTH_SHORT).show()
            return
        }
        val text = textView.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "No text to speak. Capture speech with Start Microphone first.", Toast.LENGTH_SHORT).show()
            return
        }
        textToSpeech?.setLanguage(Locale("bn"))
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bengali_tts")
        Log.d(TAG, "Speaking Bengali: $text")
    }

    /** Speak the English translation using TTS (offline if English voice is on device). */
    private fun speakEnglishText() {
        if (!ttsReady || textToSpeech == null) {
            Toast.makeText(this, "Speech not ready yet", Toast.LENGTH_SHORT).show()
            return
        }
        val text = englishTextView.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "No English text to speak. Translate to English first.", Toast.LENGTH_SHORT).show()
            return
        }
        textToSpeech?.setLanguage(Locale.US)
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "english_tts")
        Log.d(TAG, "Speaking English: $text")
    }

    /** Speak a single Bengali string (e.g. after English→Bengali translation). */
    private fun speakBengaliString(bengaliText: String) {
        if (bengaliText.isBlank() || !ttsReady || textToSpeech == null) return
        textToSpeech?.setLanguage(Locale("bn"))
        textToSpeech?.speak(bengaliText, TextToSpeech.QUEUE_ADD, null, "bengali_segment_tts")
    }

    private fun startEnglishMic() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "English speech recognition not available on this device", Toast.LENGTH_LONG).show()
            return
        }
        if (!checkForPermission(RECORD_AUDIO)) {
            Toast.makeText(this, "Microphone permission required – please allow when prompted", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, arrayOf(RECORD_AUDIO), PERMISSION_CODE)
            return
        }
        initEnglishRecognizer()
        clearBothTextAreas()
        englishTextView.invalidate()
        englishTextView.requestLayout()
        setMicButtonAppearance(recording = true)
        isRecording = true
        isEnglishMicActive = true
        speechRecognizer?.startListening(recognizerIntent)
        Log.i(TAG, "Started English mic")
    }

    private fun stopEnglishMic() {
        if (!isEnglishMicActive) return
        isEnglishMicActive = false
        isRecording = false
        speechRecognizer?.stopListening()
        setMicButtonAppearance(recording = false)
        clearBothTextAreas()
        clearSentenceListUi()
        Log.i(TAG, "Stopped English mic")
    }

    private fun initEnglishRecognizer() {
        if (recognizerIntent == null) {
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
        }
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(createEnglishRecognitionListener())
        }
    }

    private fun createEnglishRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            Log.w(TAG, "English recognition error: $error")
            runOnUiThread {
                if (verificationMode) {
                    cancelVerificationTimeout()
                    verificationMode = false
                    val expected = expectedEnglishForVerification
                    expectedEnglishForVerification = null
                    if (!expected.isNullOrBlank() && ttsReady && textToSpeech != null) {
                        textToSpeech?.setLanguage(Locale.US)
                        textToSpeech?.speak(expected, TextToSpeech.QUEUE_FLUSH, null, "try_again_word")
                        textToSpeech?.speak(getString(R.string.try_again), TextToSpeech.QUEUE_ADD, null, "english_segment_tts")
                    } else {
                        speakEnglishString(getString(R.string.try_again))
                    }
                    Toast.makeText(this@MainActivity, getString(R.string.try_again), Toast.LENGTH_SHORT).show()
                }
                if (isEnglishMicActive) {
                    speechRecognizer?.startListening(recognizerIntent)
                }
            }
        }
        override fun onResults(results: android.os.Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
            val text = matches.firstOrNull()?.trim() ?: return
            if (text.isEmpty()) return
            if (verificationMode && !verificationResultHandled) {
                verificationResultHandled = true
                cancelVerificationTimeout()
                val expected = expectedEnglishForVerification ?: ""
                verificationMode = false
                expectedEnglishForVerification = null
                val match = normalizeForMatch(text) == normalizeForMatch(expected)
                runOnUiThread {
                    val resultWord = if (match) getString(R.string.correct) else getString(R.string.incorrect)
                    val inLesson = lessonRows != null
                    var shouldAdvanceToNext = false
                    if (!match && inLesson) {
                        lessonIncorrectCount++
                        if (lessonIncorrectCount >= 3) {
                            lessonIncorrectCount = 0
                            advanceLessonToNextRow()
                            shouldAdvanceToNext = true
                            pendingRestartVerificationWith = null
                        } else {
                            pendingRestartVerificationWith = if (expected.isNotBlank()) expected else null
                        }
                    }
                    if (!match && expected.isNotBlank()) {
                        pendingSpeakCorrectWordAfterIncorrect = expected
                        if (ttsReady && textToSpeech != null) {
                            textToSpeech?.setLanguage(Locale.US)
                            textToSpeech?.speak(resultWord, TextToSpeech.QUEUE_FLUSH, null, "incorrect_then_correct")
                            incorrectFeedbackFallbackRunnable = Runnable {
                                incorrectFeedbackFallbackRunnable = null
                                val word = pendingSpeakCorrectWordAfterIncorrect
                                pendingSpeakCorrectWordAfterIncorrect = null
                                if (!word.isNullOrBlank() && !isDestroyed && ttsReady && textToSpeech != null) {
                                    textToSpeech?.setLanguage(Locale.US)
                                    textToSpeech?.speak(word, TextToSpeech.QUEUE_ADD, null, "english_segment_tts")
                                }
                                val toRestart = pendingRestartVerificationWith
                                if (!toRestart.isNullOrBlank() && !isDestroyed) {
                                    pendingRestartVerificationWith = null
                                    verificationHandler.postDelayed({
                                        if (!isDestroyed) startVerificationListening(toRestart)
                                    }, 1500)
                                }
                            }
                            verificationHandler.postDelayed(incorrectFeedbackFallbackRunnable!!, 2800)
                        } else {
                            pendingSpeakCorrectWordAfterIncorrect = null
                            speakEnglishString(resultWord)
                            if (pendingRestartVerificationWith != null) {
                                val toRestart = pendingRestartVerificationWith
                                pendingRestartVerificationWith = null
                                verificationHandler.postDelayed({
                                    if (!isDestroyed && toRestart != null) startVerificationListening(toRestart)
                                }, 1500)
                            }
                        }
                    } else {
                        speakEnglishString(resultWord)
                    }
                    Toast.makeText(this@MainActivity, resultWord, Toast.LENGTH_SHORT).show()
                    if (match && inLesson) {
                        lessonIncorrectCount = 0
                        advanceLessonAfterMatch()
                        verificationHandler.postDelayed({
                            if (lessonRows != null && !isDestroyed) onNextLessonStep()
                        }, 1500)
                    } else if (match && sentenceList.isNotEmpty()) {
                        verificationHandler.postDelayed({ onNextSentence() }, 1500)
                    } else if (shouldAdvanceToNext) {
                        speakEnglishString("Moving to next.")
                        verificationHandler.postDelayed({
                            if (lessonRows != null && !isDestroyed) onNextLessonStep()
                        }, 1500)
                    } else if (!match) {
                        val rows = lessonRows
                        val idx = lessonIndex
                        if (rows != null && idx in rows.indices) {
                            val r = rows[idx]
                            textView.text = if (lessonMode == 1 && lessonPhase == "a") r.bnA else r.bnQ
                            if (incorrectLessonRows.none { it.engA == r.engA }) {
                                incorrectLessonSourceName = lessonName
                                incorrectLessonRows.add(r)
                                saveIncorrectLessonList()
                            }
                        }
                        englishTextView.text = getString(R.string.expected_label) + " " + expected + "\n\n" +
                            getString(R.string.you_said_label) + " " + text
                    }
                }
                return
            }
            if (lessonMode == 3 && lessonMode3Listening) {
                lessonMode3Listening = false
                lessonMode3SpokeAnswer = true
                val rows = lessonRows ?: return
                val idx = lessonIndex
                if (idx < rows.size) {
                    val engA = rows[idx].engA
                    runOnUiThread {
                        englishTextView.text = engA
                        speakEnglishString(engA)
                    }
                }
                return
            }
            addSentenceToList(text, isBengali = false)
            runOnUiThread {
                val current = textView.text.toString().trim()
                textView.text = if (current.isEmpty()) text else "$current\n$text"
                translateEnglishToBengaliAndSpeak(text)
            }
            if (isEnglishMicActive && !isDestroyed) {
                speechRecognizer?.startListening(recognizerIntent)
            }
        }
        override fun onPartialResults(partialResults: android.os.Bundle?) {}
        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    }

    private fun translateEnglishToBengaliAndSpeak(englishText: String) {
        if (englishText.isBlank()) return
        val trans = translatorEnToBn ?: return
        trans.downloadModelIfNeeded()
            .addOnSuccessListener {
                trans.translate(englishText)
                    .addOnSuccessListener { bengaliText ->
                        runOnUiThread {
                            val current = englishTextView.text.toString().trim()
                            englishTextView.text = if (current.isEmpty()) bengaliText else "$current\n$bengaliText"
                            speakBengaliString(bengaliText)
                            Log.d(TAG, "Translated to Bengali and speaking: $bengaliText")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "En→Bn translation failed", e)
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Translation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "En→Bn model download failed", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Download English→Bengali model (internet needed): ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /** Sanitize a list name for use as filename (alphanumeric and underscore only). */
    private fun sanitizeListName(name: String): String {
        val s = name.trim().replace(Regex("[^A-Za-z0-9_]+"), "_")
        return if (s.isEmpty()) "list" else s
    }

    private fun showSaveListDialog() {
        if (sentenceList.isEmpty()) {
            Toast.makeText(this, getString(R.string.list_empty), Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(this).apply {
            hint = getString(R.string.save_list_name_hint)
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.save_list_dialog_title))
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                val fileName = sanitizeListName(if (name.isEmpty()) "list" else name) + LIST_FILE_SUFFIX
                saveSentenceListToFile(File(filesDir, fileName))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun saveSentenceListToFile(file: File) {
        try {
            val arr = JSONArray()
            for (s in sentenceList) {
                val obj = JSONObject()
                obj.put("text", s.text)
                obj.put("isBengali", s.isBengali)
                arr.put(obj)
            }
            file.writeText(arr.toString())
            Toast.makeText(this, getString(R.string.list_saved), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Save list failed", e)
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoadListDialog() {
        val jsonFiles = filesDir.listFiles()?.filter { it.isFile && it.name.endsWith(LIST_FILE_SUFFIX) } ?: emptyList()
        val loadOptions = mutableListOf<String>()
        val loadActions = mutableListOf<() -> Unit>()
        loadOptions.add(getString(R.string.load_practice_incorrect))
        loadActions.add {
            val (sourceName, rows) = loadIncorrectLessonListFromFile()
            if (rows.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_incorrect_saved), Toast.LENGTH_SHORT).show()
                return@add
            }
            lessonRows = rows
            lessonName = (sourceName ?: "incorrect") + "_inc"
            lessonMode = 4
            lessonIndex = 0
            lessonPhase = "q"
            lessonMode3Listening = false
            lessonMode3SpokeAnswer = false
            lessonIncorrectCount = 0
            nextButton?.isEnabled = true
            skipButton?.isEnabled = true
            clearBothTextAreas()
            textView.text = getString(R.string.lesson_loaded)
            Toast.makeText(this, getString(R.string.lesson_loaded), Toast.LENGTH_SHORT).show()
        }
        loadOptions.add(getString(R.string.load_lesson))
        loadActions.add {
            openLessonLauncher.launch(arrayOf("text/plain", "application/octet-stream"))
        }
        loadOptions.add(getString(R.string.load_verb))
        loadActions.add { showVerbSelectorDialog() }
        loadOptions.add(getString(R.string.load_tenses))
        loadActions.add { showTenseVerbSelectorDialog() }
        loadOptions.add(getString(R.string.load_regular_verbs))
        loadActions.add {
            try {
                val content = assets.open("Lessons/Regular_verbs.txt").bufferedReader().readText()
                val rows = parseRegularVerbsFile(content)
                if (rows.isEmpty()) {
                    Toast.makeText(this, "No valid rows in Regular_verbs.txt", Toast.LENGTH_SHORT).show()
                    return@add
                }
                lessonRows = rows
                lessonName = "regular_verbs"
                lessonMode = 4
                lessonIndex = 0
                lessonPhase = "q"
                lessonMode3Listening = false
                lessonMode3SpokeAnswer = false
                lessonIncorrectCount = 0
                nextButton?.isEnabled = true
                skipButton?.isEnabled = true
                clearBothTextAreas()
                textView.text = getString(R.string.lesson_loaded)
                Toast.makeText(this, getString(R.string.lesson_loaded), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not load Regular_verbs.txt: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        loadOptions.add(getString(R.string.load_irregular_verbs))
        loadActions.add {
            try {
                val content = assets.open("Lessons/Irregular_verbs.txt").bufferedReader().readText()
                val rows = parseIrregularVerbsFile(content)
                if (rows.isEmpty()) {
                    Toast.makeText(this, "No valid rows in Irregular_verbs.txt", Toast.LENGTH_SHORT).show()
                    return@add
                }
                lessonRows = rows
                lessonName = "irregular_verbs"
                lessonMode = 4
                lessonIndex = 0
                lessonPhase = "q"
                lessonMode3Listening = false
                lessonMode3SpokeAnswer = false
                lessonIncorrectCount = 0
                nextButton?.isEnabled = true
                skipButton?.isEnabled = true
                clearBothTextAreas()
                textView.text = getString(R.string.lesson_loaded)
                Toast.makeText(this, getString(R.string.lesson_loaded), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not load Irregular_verbs.txt: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        loadOptions.add(getString(R.string.load_lesson) + " (introduce)")
        loadActions.add {
            try {
                val content = assets.open("introduce.txt").bufferedReader().readText()
                val rows = parseLessonFile(content)
                if (rows.isEmpty()) {
                    Toast.makeText(this, "No valid rows in introduce.txt", Toast.LENGTH_SHORT).show()
                    return@add
                }
                showModeSelectorDialog(rows, "introduce")
            } catch (e: Exception) {
                Toast.makeText(this, "Could not load introduce.txt: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        if (jsonFiles.isNotEmpty()) {
            loadOptions.add(getString(R.string.load_sentence_list))
            loadActions.add {
                val names = jsonFiles.map { it.name.removeSuffix(LIST_FILE_SUFFIX) }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.load_list_dialog_title))
                    .setItems(names) { _, which -> loadSentenceListFromFile(jsonFiles[which]) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.load_dialog_title))
            .setItems(loadOptions.toTypedArray()) { _, which ->
                if (which < loadActions.size) loadActions[which]()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Save incorrect lesson rows to file (so user can "Practice incorrect words" later). */
    private fun saveIncorrectLessonList() {
        try {
            val file = File(filesDir, INCORRECT_LESSON_FILE)
            val arr = JSONArray()
            for (r in incorrectLessonRows) {
                val obj = JSONObject()
                obj.put("engQ", r.engQ)
                obj.put("bnQ", r.bnQ)
                obj.put("engA", r.engA)
                obj.put("bnA", r.bnA)
                arr.put(obj)
            }
            val root = JSONObject()
            root.put("lessonName", incorrectLessonSourceName ?: "incorrect")
            root.put("rows", arr)
            file.writeText(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Save incorrect list failed", e)
        }
    }

    /** Load incorrect lesson rows from file; returns (original lesson name, rows) or (null, emptyList()) if missing/empty. */
    private fun loadIncorrectLessonListFromFile(): Pair<String?, List<LessonRow>> {
        return try {
            val file = File(filesDir, INCORRECT_LESSON_FILE)
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
                return Pair(sourceName, rows)
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
            Pair(null, rows)
        } catch (e: Exception) {
            Log.e(TAG, "Load incorrect list failed", e)
            Pair(null, emptyList())
        }
    }

    private fun loadSentenceListFromFile(file: File) {
        try {
            lessonRows = null
            lessonName = null
            lessonMode = 0
            val arr = JSONArray(file.readText())
            sentenceList.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                sentenceList.add(Sentence(obj.getString("text"), obj.getBoolean("isBengali")))
            }
            sentenceAdapter.notifyDataSetChanged()
            currentNextIndex = 0
            nextButton?.isEnabled = sentenceList.isNotEmpty()
            skipButton?.isEnabled = lessonRows != null
            Toast.makeText(this, getString(R.string.list_loaded), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Load list failed", e)
            Toast.makeText(this, "Load failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** Expand common contractions so "I'm" and "I am" match. Call after lowercasing. */
    private fun normalizeContractions(s: String): String {
        var t = s
        val contractions = mapOf(
            "i'm" to "i am", "i've" to "i have", "i'd" to "i would", "i'll" to "i will",
            "that's" to "that is", "it's" to "it is", "what's" to "what is", "there's" to "there is",
            "here's" to "here is", "who's" to "who is",
            "don't" to "do not", "doesn't" to "does not", "didn't" to "did not",
            "isn't" to "is not", "aren't" to "are not", "wasn't" to "was not", "weren't" to "were not",
            "haven't" to "have not", "hasn't" to "has not", "hadn't" to "had not",
            "won't" to "will not", "wouldn't" to "would not", "can't" to "cannot",
            "couldn't" to "could not", "shouldn't" to "should not",
            "we're" to "we are", "they're" to "they are", "you're" to "you are",
            "we've" to "we have", "they've" to "they have", "you've" to "you have",
            "we'll" to "we will", "they'll" to "they will", "you'll" to "you will"
        )
        for ((contraction, expansion) in contractions) {
            t = t.replace(Regex("\\b${Regex.escape(contraction)}\\b"), expansion)
        }
        return t
    }

    /** Number words to digits so "sixty" and "60" match. */
    private fun normalizeNumberWords(s: String): String {
        var t = s
        val numberWords = mapOf(
            "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
            "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
            "ten" to "10", "eleven" to "11", "twelve" to "12", "thirteen" to "13",
            "fourteen" to "14", "fifteen" to "15", "sixteen" to "16", "seventeen" to "17",
            "eighteen" to "18", "nineteen" to "19", "twenty" to "20", "thirty" to "30",
            "forty" to "40", "fifty" to "50", "sixty" to "60", "seventy" to "70",
            "eighty" to "80", "ninety" to "90", "hundred" to "100", "thousand" to "1000"
        )
        for ((word, digit) in numberWords) {
            t = t.replace(Regex("\\b$word\\b", RegexOption.IGNORE_CASE), digit)
        }
        return t
    }

    /** Normalize string for verification match: contractions, number words→digits, trim, lowercase, collapse spaces, remove punctuation. */
    private fun normalizeForMatch(s: String): String {
        val lower = s.trim().lowercase()
            .replace('\u2019', '\'')  // curly apostrophe → straight (so "i'm" from speech/translation matches)
        val expanded = normalizeContractions(lower)
        return normalizeNumberWords(expanded)
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9 ]"), "")
    }

    /** Cancel verification timeout (call when we get result or error). */
    private fun cancelVerificationTimeout() {
        verificationTimeoutRunnable?.let { verificationHandler.removeCallbacks(it) }
        verificationTimeoutRunnable = null
    }

    /** Start listening for user to speak English (verification mode); compare with expected and say Correct/Incorrect. */
    private fun startVerificationListening(expectedEnglish: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "English speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }
        if (!checkForPermission(RECORD_AUDIO)) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
            return
        }
        cancelVerificationTimeout()
        verificationMode = true
        verificationResultHandled = false
        expectedEnglishForVerification = expectedEnglish
        initEnglishRecognizer()
        speechRecognizer?.startListening(recognizerIntent)
        verificationTimeoutRunnable = Runnable {
            runOnUiThread {
                if (verificationMode) {
                    verificationMode = false
                    expectedEnglishForVerification = null
                    speechRecognizer?.stopListening()
                    speakEnglishString(getString(R.string.no_speech_detected))
                    Toast.makeText(this@MainActivity, getString(R.string.no_speech_detected), Toast.LENGTH_LONG).show()
                }
                verificationTimeoutRunnable = null
            }
        }
        verificationHandler.postDelayed(verificationTimeoutRunnable!!, 15000)
        Log.d(TAG, "Verification: listening for user to speak English")
    }

    /** After a correct match in lesson mode: advance phase (mode 1) or index (mode 2). */
    private fun advanceLessonAfterMatch() {
        val rows = lessonRows ?: return
        if (lessonIndex >= rows.size) return
        when (lessonMode) {
            1 -> if (lessonPhase == "q") lessonPhase = "a" else { lessonIndex++; lessonPhase = "q" }
            2 -> lessonIndex++
            4 -> lessonIndex++
            else -> { }
        }
    }

    /** Skip to next row (e.g. after 3 incorrect attempts). */
    private fun advanceLessonToNextRow() {
        lessonIndex++
        if (lessonMode == 1) lessonPhase = "q"
    }

    /** Skip current word and move to next (escape incorrect loop). Only active when a lesson is loaded. */
    private fun onSkipWord() {
        if (lessonRows == null) {
            Toast.makeText(this, getString(R.string.skip), Toast.LENGTH_SHORT).show()
            return
        }
        cancelVerificationTimeout()
        incorrectFeedbackFallbackRunnable?.let { verificationHandler.removeCallbacks(it) }
        incorrectFeedbackFallbackRunnable = null
        pendingSpeakCorrectWordAfterIncorrect = null
        pendingRestartVerificationWith = null
        verificationMode = false
        expectedEnglishForVerification = null
        verificationResultHandled = true
        speechRecognizer?.stopListening()
        advanceLessonToNextRow()
        speakEnglishString("Skipped.")
        onNextLessonStep()
    }

    /** Next step in lesson: dispatch by mode and phase; or show "Lesson done" if finished. */
    private fun onNextLessonStep() {
        val rows = lessonRows ?: return
        if (lessonIndex >= rows.size) {
            speakEnglishString(getString(R.string.lesson_done))
            Toast.makeText(this, getString(R.string.lesson_done), Toast.LENGTH_SHORT).show()
            lessonRows = null
            lessonName = null
            lessonMode = 0
            lessonIndex = 0
            lessonPhase = "q"
            lessonMode3Listening = false
            lessonMode3SpokeAnswer = false
            nextButton?.isEnabled = sentenceList.isNotEmpty()
            skipButton?.isEnabled = false
            clearBothTextAreas()
            textView.text = getString(R.string.lesson_done)
            return
        }
        if (lessonMode == 3 && lessonMode3SpokeAnswer) {
            lessonIndex++
            lessonMode3SpokeAnswer = false
            if (lessonIndex >= rows.size) {
                speakEnglishString(getString(R.string.lesson_done))
                Toast.makeText(this, getString(R.string.lesson_done), Toast.LENGTH_SHORT).show()
                lessonRows = null
                lessonName = null
                lessonMode = 0
                lessonIndex = 0
                lessonPhase = "q"
                lessonMode3Listening = false
                nextButton?.isEnabled = sentenceList.isNotEmpty()
                skipButton?.isEnabled = false
                clearBothTextAreas()
                textView.text = getString(R.string.lesson_done)
                return
            }
        }
        val row = rows[lessonIndex]
        when (lessonMode) {
            1 -> {
                if (lessonPhase == "q") {
                    textView.text = row.bnQ
                    englishTextView.text = row.engQ
                    expectedEnglishForVerification = row.engQ
                    textToSpeech?.setLanguage(Locale("bn"))
                    textToSpeech?.speak(row.bnQ, TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
                } else {
                    textView.text = row.bnA
                    englishTextView.text = row.engA
                    expectedEnglishForVerification = row.engA
                    textToSpeech?.setLanguage(Locale("bn"))
                    textToSpeech?.speak(row.bnA, TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
                }
            }
            2 -> {
                textView.text = row.bnQ
                englishTextView.text = ""
                expectedEnglishForVerification = row.engA
                textToSpeech?.setLanguage(Locale.ENGLISH)
                textToSpeech?.speak(row.engQ, TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
            }
            3 -> {
                lessonMode3Listening = true
                textView.text = row.bnQ
                englishTextView.text = "Say the question in English…"
                if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                    Toast.makeText(this, "English speech recognition not available", Toast.LENGTH_SHORT).show()
                    lessonMode3Listening = false
                    return
                }
                if (!checkForPermission(RECORD_AUDIO)) {
                    Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
                    lessonMode3Listening = false
                    return
                }
                initEnglishRecognizer()
                speechRecognizer?.startListening(recognizerIntent)
            }
            4 -> {
                textView.text = row.bnQ
                englishTextView.text = row.engA
                expectedEnglishForVerification = row.engA
                textToSpeech?.setLanguage(Locale("bn"))
                textToSpeech?.speak(row.bnQ, TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
            }
        }
    }

    /** Next: show sentence, speak it (Bengali), show translation, then listen for user to speak English and verify. */
    private fun onNextSentence() {
        if (sentenceList.isEmpty()) return
        if (currentNextIndex >= sentenceList.size) currentNextIndex = 0
        val sentence = sentenceList[currentNextIndex]
        currentNextIndex = (currentNextIndex + 1) % sentenceList.size

        textView.text = sentence.text
        if (sentence.isBengali) {
            val trans = translator ?: return
            trans.downloadModelIfNeeded()
                .addOnSuccessListener {
                    trans.translate(sentence.text)
                        .addOnSuccessListener { translated ->
                            runOnUiThread {
                                englishTextView.text = translated
                                pendingVerificationExpectedEnglish = translated
                                textToSpeech?.setLanguage(Locale("bn"))
                                textToSpeech?.speak(sentence.text, TextToSpeech.QUEUE_FLUSH, null, "bengali_verification")
                            }
                        }
                        .addOnFailureListener { e ->
                            runOnUiThread { Toast.makeText(this@MainActivity, "Translation failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                        }
                }
                .addOnFailureListener { e ->
                    runOnUiThread { Toast.makeText(this@MainActivity, "Model download failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
        } else {
            val trans = translatorEnToBn ?: return
            trans.downloadModelIfNeeded()
                .addOnSuccessListener {
                    trans.translate(sentence.text)
                        .addOnSuccessListener { translated ->
                            runOnUiThread {
                                englishTextView.text = translated
                                speakBengaliString(translated)
                            }
                        }
                        .addOnFailureListener { e ->
                            runOnUiThread { Toast.makeText(this@MainActivity, "Translation failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                        }
                }
                .addOnFailureListener { e ->
                    runOnUiThread { Toast.makeText(this@MainActivity, "Model download failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
        }
    }

    override fun onDestroy() {
        cancelVerificationTimeout()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        translator?.close()
        translator = null
        translatorEnToBn?.close()
        translatorEnToBn = null
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    private fun isInputBengali(): Boolean = findViewById<RadioButton>(R.id.radio_bengali).isChecked

    private fun updateTranslationLabelAndButton() {
        if (isInputBengali()) {
            translationLabel.text = getString(R.string.english_translation_label)
        } else {
            translationLabel.text = getString(R.string.bengali_translation_label)
        }
    }

    /** Clear both the main text and the translation (scrollable) areas and force refresh. */
    private fun clearBothTextAreas() {
        textView.setText("")
        textView.scrollTo(0, 0)
        englishTextView.setText("")
        englishTextView.scrollTo(0, 0)
        englishTextView.invalidate()
        englishTextView.requestLayout()
        lastText = ""
        idx = 0
    }

    /** Clear the sentence list in the UI (used when stopping microphone). */
    private fun clearSentenceListUi() {
        sentenceList.clear()
        sentenceAdapter.notifyDataSetChanged()
        currentNextIndex = 0
        nextButton?.isEnabled = false
        skipButton?.isEnabled = false
    }

    /** Remove sentence at position (for edit list). */
    private fun removeSentenceAt(position: Int) {
        if (position < 0 || position >= sentenceList.size) return
        sentenceList.removeAt(position)
        sentenceAdapter.notifyItemRemoved(position)
        if (currentNextIndex >= sentenceList.size) currentNextIndex = 0
        nextButton?.isEnabled = sentenceList.isNotEmpty()
        skipButton?.isEnabled = lessonRows != null
    }

    /** Drag-to-reorder callback for sentence list. Long-press and drag to reorder. */
    private val sentenceDragCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        0
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition
            if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
            val item = sentenceList.removeAt(from)
            sentenceList.add(to, item)
            sentenceAdapter.notifyItemMoved(from, to)
            when {
                currentNextIndex == from -> currentNextIndex = to
                from < to && currentNextIndex > from && currentNextIndex <= to -> currentNextIndex--
                to < from && currentNextIndex >= to && currentNextIndex < from -> currentNextIndex++
            }
            return true
        }
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    }

    private fun onDemo() {
        Log.i(TAG, "onDemo() called - Start Microphone button click received")
        if (!isRecording && !isEnglishMicActive) {
            // Clear both text areas immediately when user taps Start; post so UI refreshes
            clearBothTextAreas()
            englishTextView.post { clearBothTextAreas() }
            // Start: Bengali (Sherpa) or English (SpeechRecognizer)
            if (isInputBengali()) {
                startBengaliMic()
            } else {
                startEnglishMic()
            }
            return
        }
        // Stop
        if (isEnglishMicActive) {
            stopEnglishMic()
            return
        }
        stopMicRecording(speakBengali = false)
    }

    private fun startBengaliMic() {
        try {
            if (!modelLoaded && !SKIP_RECOGNITION_FOR_MIC) {
                Toast.makeText(this, "Model not loaded yet", Toast.LENGTH_SHORT).show()
                return
            }
            Log.i(TAG, "onDemo: checking RECORD_AUDIO permission")
            if (!checkForPermission(RECORD_AUDIO)) {
                Log.w(TAG, "RECORD_AUDIO not granted, requesting RECORD_AUDIO...")
                Toast.makeText(this, "Microphone permission required – please allow when prompted", Toast.LENGTH_LONG).show()
                ActivityCompat.requestPermissions(this, arrayOf(RECORD_AUDIO), PERMISSION_CODE)
                return
            }
            Log.i(TAG, "onDemo: calling initMicrophone()")
            val ret = initMicrophone()
                if (!ret) {
                    Log.e(TAG, "Failed to initialize microphone")
                    Toast.makeText(this, "Failed to open microphone", Toast.LENGTH_SHORT).show()
                    return
                }
                Log.i(TAG, "state: ${audioRecord?.state}")
                try {
                    audioRecord!!.startRecording()
                } catch (e: Exception) {
                    Log.e(TAG, "startRecording() failed", e)
                    Toast.makeText(this, "startRecording failed: ${e.message}", Toast.LENGTH_LONG).show()
                    audioRecord?.release()
                    audioRecord = null
                    return
                }
                if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    Log.e(TAG, "AudioRecord not in RECORDING state after startRecording()")
                    audioRecord?.release()
                    audioRecord = null
                    Toast.makeText(this, "Microphone failed to start", Toast.LENGTH_LONG).show()
                    return
                }
                setMicButtonAppearance(recording = true)
                isRecording = true
                if (!SKIP_RECOGNITION_FOR_MIC) recognizer.resetModel()
                clearBothTextAreas()
                englishTextView.invalidate()
                englishTextView.requestLayout()

                this.recordingThread = thread(true) {
                    processSamples()
                }
                Log.i(TAG, "Started recording")
            } catch (e: Throwable) {
                Log.e(TAG, "Start microphone failed", e)
                isRecording = false
                audioRecord?.release()
                audioRecord = null
                setMicButtonAppearance(recording = false)
                Toast.makeText(this, "Start mic failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /** Stops mic recording, updates UI, and optionally speaks the recognized Bengali text. */
    private fun stopMicRecording(speakBengali: Boolean) {
        if (!isRecording) return
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        setMicButtonAppearance(recording = false)
        clearBothTextAreas()
        clearSentenceListUi()
        Log.i(TAG, "Stopped recording")
        if (speakBengali) speakBengaliText()
    }

    // Accumulate at least this many samples before first recognizer call (1 sec at 16 kHz).
    // Some native engines crash on the first small chunk; feeding a full second first can help.
    private val minSamplesBeforeRecognizer = 16000

    private fun processSamples() {
        val sampleRate = if (SKIP_RECOGNITION_FOR_MIC) 16000 else recognizer.getSampleRate()
        val interval = 0.1 // 100 ms
        val bufferSize = (interval * sampleRate).toInt().coerceAtLeast(1600)
        val buffer = ShortArray(bufferSize)
        val preBuffer = mutableListOf<Short>()
        var sentFirstChunk = false
        val startTimeMs = System.currentTimeMillis()

        // Give startRecording() time to fully initialize on some devices
        try {
            Thread.sleep(150)
        } catch (_: InterruptedException) { }

        try {
            while (isRecording) {
                val ret = try {
                    audioRecord?.read(buffer, 0, buffer.size) ?: 0
                } catch (e: Exception) {
                    Log.e(TAG, "AudioRecord.read() failed", e)
                    runOnUiThread {
                        if (!isDestroyed) Toast.makeText(this@MainActivity, "Microphone read error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    break
                }
                if (ret <= 0) continue

                val chunk = if (ret < buffer.size) buffer.copyOf(ret) else buffer

                if (SKIP_RECOGNITION_FOR_MIC) {
                    // Test mode: only record, don't touch recognizer at all (avoids native crash in resetModel/processSamples).
                    if (!isDestroyed) {
                        runOnUiThread {
                            if (!isDestroyed) {
                                val sec = (System.currentTimeMillis() - startTimeMs) / 1000
                                textView.text = "Recording... (no recognizer) ${sec}s"
                            }
                        }
                    }
                    continue
                }

                if (!sentFirstChunk) {
                    preBuffer.addAll(chunk.toList())
                    if (preBuffer.size >= minSamplesBeforeRecognizer) {
                        sentFirstChunk = true
                        val firstChunk = preBuffer.take(minSamplesBeforeRecognizer).toShortArray()
                        val remainder = preBuffer.drop(minSamplesBeforeRecognizer)
                        preBuffer.clear()
                        preBuffer.addAll(remainder)
                        try {
                            recognizer.processSamples(firstChunk)
                        } catch (e: Exception) {
                            Log.e(TAG, "Recognizer first chunk error", e)
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Recognition error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    continue
                }

                if (chunk.size < 800) continue
                try {
                    val result = recognizer.processSamples(chunk)
                    val text = result.first
                    val endpoint = result.second

                    Log.d(TAG, "Recognized $text")
                    if (text.isNotBlank()) {
                        var textToDisplay: String
                        if (endpoint) {
                            lastText = if (lastText.isEmpty()) text else "${lastText}\n$text"
                            textToDisplay = lastText
                            idx += 1
                            runOnUiThread {
                                textView.text = textToDisplay
                                translateSegmentAndSpeakEnglish(text)
                            }
                        } else {
                            textToDisplay = if (lastText.isEmpty()) text else "${lastText}\n$text"
                            runOnUiThread {
                                textView.text = textToDisplay
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Recognizer error", e)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Recognition error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recording thread error", e)
            runOnUiThread {
                isRecording = false
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
                setMicButtonAppearance(recording = false)
                Toast.makeText(this@MainActivity, "Microphone error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun initMicrophone(): Boolean {
        Log.i(TAG, "initMicrophone() entered")
        if (!SKIP_RECOGNITION_FOR_MIC) {
            try {
                checkHeadset()
            } catch (e: Exception) {
                Log.w(TAG, "checkHeadset failed (non-fatal)", e)
            }
        }

        val sampleRateInHz = if (SKIP_RECOGNITION_FOR_MIC) 16000 else recognizer.getSampleRate()

        var numBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
        if (numBytes == AudioRecord.ERROR || numBytes == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "getMinBufferSize returned error: $numBytes, using 2x sample rate")
            numBytes = 2 * sampleRateInHz
        }
        numBytes = max((0.2 * sampleRateInHz).toInt(), numBytes)
        numBytes = numBytes * 2 // some devices need 2x min buffer to avoid native crash/underrun
        Log.i(
            TAG, "buffer size in bytes: $numBytes, ms: ${numBytes * 1000.0f / sampleRateInHz}"
        )

        val sourcesToTry = intArrayOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT
        )
        for (source in sourcesToTry) {
            try {
                audioRecord?.release()
                audioRecord = null
                audioRecord = AudioRecord(
                    source,
                    sampleRateInHz,
                    channelConfig,
                    audioFormat,
                    numBytes * 2
                )
                if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    Log.i(TAG, "AudioRecord initialized with source $source")
                    return true
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "AudioRecord SecurityException (source=$source)", e)
            } catch (e: Exception) {
                Log.e(TAG, "AudioRecord failed (source=$source)", e)
            }
        }
        Log.e(TAG, "AudioRecord failed with all sources")
        audioRecord?.release()
        audioRecord = null
        return false
    }


    @Suppress("deprecation")
    private fun checkHeadset() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val am = getSystemService(AUDIO_SERVICE) as AudioManager

        if (adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED) {
            Handler().postDelayed({
                am.startBluetoothSco()
                am.isMicrophoneMute = true
            }, 1000)
        } else {
            am.stopBluetoothSco()
            if (am.isWiredHeadsetOn) {
                am.setMicrophoneMute(true);
            } else {
                am.setMicrophoneMute(false);
            }
        }
    }

}

/** Adapter for the sentence list: text + delete; supports reorder via ItemTouchHelper. */
private class SentenceAdapter(
    private val list: MutableList<MainActivity.Sentence>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<SentenceAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.item_sentence_text)
        val deleteButton: Button = itemView.findViewById(R.id.item_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sentence, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = list[position].text
        holder.deleteButton.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(pos)
        }
    }

    override fun getItemCount(): Int = list.size
}

