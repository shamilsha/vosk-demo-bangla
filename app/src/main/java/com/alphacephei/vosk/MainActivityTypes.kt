package com.alphacephei.vosk

import android.content.Context
import com.alphacephei.vosk.R

/**
 * Shared types used by MainActivity. Extracted to reduce MainActivity.kt size and memory pressure.
 */

/** Which content layout is currently shown in the main content area. */
enum class ContentLayout {
    LEGACY, TEXT_DISPLAY, SPEECH_INPUT, PRACTICE_THREE_AREA, TABLE_DISPLAY, DIAGRAM_ONLY,
    MIC_SPEAKER_TEST, NOUN_TABS, NOUN_TEST, CONVERSATION, CONVERSATION_BUBBLES, SV_RIBBON, CONVEYOR_TRIPLE,
    SV_WORDS_CONVEYOR, SV_I_FOUR_SECTIONS, SIMPLE_SENTENCE, THREECOL_TABLE,
    /** POC: subtopics shown as iPhone-style buttons; tap navigates to that subtopic. */
    POC_BUTTON_MENU,
    /** Table-like triplets: Present/Past/Future columns. */
    TENSE_TRIPLETS
}

/** Layout resource ID for the given content layout. */
fun getContentLayoutResId(layout: ContentLayout): Int = when (layout) {
    ContentLayout.LEGACY -> R.layout.layout_content_legacy
    ContentLayout.TEXT_DISPLAY -> R.layout.layout_text_display
    ContentLayout.SPEECH_INPUT -> R.layout.layout_speech_input
    ContentLayout.PRACTICE_THREE_AREA -> R.layout.layout_practice_three_area
    ContentLayout.TABLE_DISPLAY -> R.layout.layout_table_display
    ContentLayout.DIAGRAM_ONLY -> R.layout.layout_content_diagram_only
    ContentLayout.MIC_SPEAKER_TEST -> R.layout.layout_mic_speaker_test
    ContentLayout.NOUN_TABS -> R.layout.layout_noun_tabs
    ContentLayout.NOUN_TEST -> R.layout.layout_noun_test
    ContentLayout.CONVERSATION -> R.layout.layout_conversation
    ContentLayout.CONVERSATION_BUBBLES -> R.layout.layout_conversation_bubbles
    ContentLayout.SV_RIBBON -> R.layout.layout_sv_ribbon
    ContentLayout.CONVEYOR_TRIPLE -> R.layout.layout_conveyor_triple
    ContentLayout.SV_WORDS_CONVEYOR -> R.layout.layout_sv_words_conveyor
    ContentLayout.SV_I_FOUR_SECTIONS -> R.layout.layout_sv_i_four_sections
    ContentLayout.SIMPLE_SENTENCE -> R.layout.layout_simple_sentence
    ContentLayout.THREECOL_TABLE -> R.layout.layout_3coldata_2coldisplay
    ContentLayout.POC_BUTTON_MENU -> R.layout.layout_poc_button_menu
    ContentLayout.TENSE_TRIPLETS -> R.layout.layout_tense_triplets
}

/** True if this layout shows the reusable Start / Stop / Pause / Resume bar. */
fun usesControlActions(layout: ContentLayout): Boolean = layout in setOf(
    ContentLayout.CONVERSATION,
    ContentLayout.MIC_SPEAKER_TEST,
    ContentLayout.SPEECH_INPUT,
    ContentLayout.PRACTICE_THREE_AREA,
    ContentLayout.SV_RIBBON,
    ContentLayout.CONVEYOR_TRIPLE,
    ContentLayout.SV_WORDS_CONVEYOR,
    ContentLayout.SV_I_FOUR_SECTIONS,
    ContentLayout.SIMPLE_SENTENCE,
    ContentLayout.THREECOL_TABLE,
    ContentLayout.TENSE_TRIPLETS
)

/** One ribbon item height in pixels (stripHeightDp default 40). For scroll-by-one-row. */
fun ribbonItemHeightPx(context: Context, stripHeightDp: Int = 40): Int =
    (stripHeightDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

/** Alphabet table: English letter → Bengali pronunciation (for highlighted letters). */
val alphabetBengaliPronunciation = mapOf(
    "A" to "এই",
    "F" to "এফ",
    "J" to "জ্বেই",
    "K" to "খেই",
    "O" to "ওউ",
    "P" to "ফী",
    "Q" to "খিউ",
    "T" to "ঠী",
    "V" to "ভী",
    "Z" to "জী / যেড"
)

/** A sentence in the list: text and whether it was spoken in Bengali (true) or English (false). */
data class Sentence(val text: String, val isBengali: Boolean)

/** One row from a lesson file: A=English Q, B=Bengali Q, C=English A, D=Bengali A (pipe-separated). */
data class LessonRow(val engQ: String, val bnQ: String, val engA: String, val bnA: String)

/** One line in a conversation script: speaker (P1/P2), English text, Bengali meaning. */
data class ConversationLine(val speaker: String, val english: String, val bengali: String)

enum class ConversationMode { LEARNING, PRACTICE, TEST, VOCAB }

/** One row for verb/tense display: English and Bengali. */
data class VerbRow(val english: String, val bengali: String)

/** One row of generic 3-column lesson data: English, Bengali, Pronunciation/Hint. */
data class ThreeColRow(val english: String, val bengali: String, val hint: String)

/** One cell in a tense triplet (single column). */
data class TenseTripletCell(val english: String, val bengali: String, val hint: String)

/** One triplet row: present + past + future columns. */
data class TenseTripletRow(
    val present: TenseTripletCell,
    val past: TenseTripletCell,
    val future: TenseTripletCell
)

/** One turn in conversation-bubble format: PersonA (left) or PersonB (right), then English, Bengali, Pronunciation. */
data class ConversationBubbleRow(val speaker: String, val english: String, val bengali: String, val pronunciation: String)

/** One row for lesson vocabulary pronunciation (V tab): word, pronunciation hint, meaning from master list. */
data class LessonVocabRow(val word: String, val pronunciation: String, val meaning: String)

enum class ModelStatus { MODEL_STATUS_INIT, MODEL_STATUS_START, MODEL_STATUS_READY }

/** A drawer subtopic: title, action key, and which content layout to use. */
data class Subtopic(
    val title: String,
    val actionKey: String,
    val layoutType: ContentLayout = ContentLayout.LEGACY
)

/** A topic with subtopics in the drawer. */
data class Topic(val title: String, val subtopics: List<Subtopic>)

/** Flat list item for drawer: level header, topic header, or subtopic. */
sealed class DrawerItem {
    data class LevelHeader(val title: String, val topics: List<Topic>, var expanded: Boolean) : DrawerItem()
    data class TopicHeader(val topic: Topic, val topicIndex: Int, var expanded: Boolean, val indentLevel: Int = 0) : DrawerItem()
    data class SubtopicEntry(val subtopic: Subtopic, val topicIndex: Int, val indentLevel: Int = 0) : DrawerItem()
}

/** Per-subject/SVO entry: verb (for display), Bengali meaning, and optional romanized pronunciation. */
data class SvEntry(val verb: String, val bengali: String, val pronunciation: String = "")
