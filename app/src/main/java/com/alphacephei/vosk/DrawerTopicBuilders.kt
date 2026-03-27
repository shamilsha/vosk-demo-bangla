package com.alphacephei.vosk

import android.content.res.AssetManager

/**
 * Builds topic lists for the drawer. Extracted to reduce MainActivity.kt size.
 */
object DrawerTopicBuilders {

    /** Main drawer topic list (depends on assets for SVO/pronunciation subtopics). */
    fun getTopicList(assetManager: AssetManager): List<Topic> = listOf(
        Topic("Introduction", listOf(
            Subtopic("Introduction (Bengali)", "intro_bengali", ContentLayout.TEXT_DISPLAY),
            Subtopic("Mic Test", "mic_test", ContentLayout.MIC_SPEAKER_TEST),
            Subtopic("Translation Practice", "translation_practice", ContentLayout.PRACTICE_THREE_AREA),
            Subtopic("First meeting (conversation)", "conversation_first_meeting", ContentLayout.CONVERSATION),
            // Same layout + V tab for all; actionKey must match conversationBubbleLessonAssetPaths in MainActivity.
            Subtopic("First meeting (bubbles)", "conv_bubble_first_meeting", ContentLayout.CONVERSATION_BUBBLES),
            Subtopic("Second lesson (bubbles)", "conv_bubble_second_lesson", ContentLayout.CONVERSATION_BUBBLES),
            Subtopic("Third lesson (bubbles)", "conv_bubble_third_lesson", ContentLayout.CONVERSATION_BUBBLES),
            Subtopic("Fourth lesson (bubbles)", "conv_bubble_fourth_lesson", ContentLayout.CONVERSATION_BUBBLES),
            Subtopic("Buy a shirt (bubbles)", "conv_bubble_buy_shirt", ContentLayout.CONVERSATION_BUBBLES)
        )),
        Topic("Silent Letters", listOf(
            Subtopic("Silent e (a-e)", "pron_silent_e"),
            Subtopic("Silent G (gn at end)", "pron_silent_g"),
            Subtopic("Silent B", "pron_silent_b"),
            Subtopic("Silent W", "pron_silent_w"),
            Subtopic("Silent K", "pron_silent_k"),
            Subtopic("Rule 23: silent G", "pron_rule23")
        )),
        Topic("Verbs", listOf(
            Subtopic("Learn verb (DO, HAVE, GO)", "verb_basic"),
            Subtopic("Learn tenses (12 tenses)", "verb_tenses"),
            Subtopic("Regular verbs", "verb_regular"),
            Subtopic("Irregular verbs", "verb_irregular")
        )),
        Topic("Grammar", listOf(
            Subtopic("Parts of speech", "grammar_pos"),
            Subtopic("SVO sentences", "grammar_svo"),
            Subtopic("Simple adjective", "simple_adjective_dual", ContentLayout.TENSE_TRIPLETS),
            Subtopic("Simple adverb", "simple_adverb_dual", ContentLayout.TENSE_TRIPLETS),
            Subtopic("Simple preposition", "simple_preposition_dual", ContentLayout.TENSE_TRIPLETS)
        )),
        Topic("Diagrams", listOf(
            Subtopic("1-to-3 (Grammar Rules)", "diagram_1to3"),
            Subtopic("3-to-1 (Have/Has)", "diagram_3to1")
        )),
        Topic("Tense", listOf(
            Subtopic("Tenses hierarchy", "tense_diagram"),
            Subtopic("Simple tense", "simple_tense_triplets", ContentLayout.TENSE_TRIPLETS),
            Subtopic("Simple continuous", "simple_continuous_triplets", ContentLayout.TENSE_TRIPLETS),
            Subtopic("Simple perfect", "simple_perfect_triplets", ContentLayout.TENSE_TRIPLETS),
            Subtopic("Simple question", "simple_question_triplets", ContentLayout.TENSE_TRIPLETS),
            Subtopic("Simple continuous question", "simple_continuous_question_triplets", ContentLayout.TENSE_TRIPLETS),
            Subtopic("Present negative", "present_negative_duplex", ContentLayout.TENSE_TRIPLETS),
            Subtopic("Past negative", "past_negative_duplex", ContentLayout.TENSE_TRIPLETS),
            Subtopic("Future negative", "future_negative_duplex", ContentLayout.TENSE_TRIPLETS),
            Subtopic("Perfect question", "perfect_question_duplex", ContentLayout.TENSE_TRIPLETS),
            Subtopic("Extend sentence", "extend_sentence", ContentLayout.EXTEND_SENTENCE),
            Subtopic("Preposition (time blocks)", "preposition_time_blocks", ContentLayout.PREPOSITION_BLOCKS)
        )),
        Topic("Lessons", listOf(
            Subtopic("Load lesson (.txt)", "lesson_file"),
            Subtopic("Introduce lesson", "lesson_introduce"),
            Subtopic("Practice incorrect words", "lesson_incorrect")
        )),
        Topic("SVO", listOf(
            Subtopic("I", "svo:I", ContentLayout.LEGACY),
            Subtopic("S-V-simple", "sv_ribbon", ContentLayout.SV_RIBBON),
            Subtopic("Simple present I (4 sections)", "sv_I_four_sections", ContentLayout.SV_I_FOUR_SECTIONS),
            Subtopic("Simple past I (4 sections)", "sv_I_past_four_sections", ContentLayout.SV_I_FOUR_SECTIONS),
            Subtopic("Simple future I (4 sections)", "sv_I_future_four_sections", ContentLayout.SV_I_FOUR_SECTIONS),
            Subtopic("Past continuous I (4 sections)", "sv_I_past_cont_four_sections", ContentLayout.SV_I_FOUR_SECTIONS),
            Subtopic("Future continuous I (4 sections)", "sv_I_future_cont_four_sections", ContentLayout.SV_I_FOUR_SECTIONS),
            Subtopic("S-V-N-simple", "sv_ribbon_I_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("Do I ...? (Triple)", "sv_I_question", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("Don't I ...? (Triple)", "sv_I_question_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("Do you ...? (Triple)", "sv_You_question", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("Don't you ...? (Triple)", "sv_You_question_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("Are you ...ing? (Triple)", "sv_You_question_ing", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-Q-simple", "sv_subject_question", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-N-simple", "sv_subject_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-N-Q-simple", "sv_subject_question_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-cont", "sv_cont", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-Q-cont", "sv_cont_question", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-N-cont", "sv_cont_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-N-Q-cont", "sv_cont_question_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-past", "sv_past", ContentLayout.SV_RIBBON),
            Subtopic("S-V-Q-past", "sv_past_question", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-N-past", "sv_past_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-N-Q-past", "sv_past_question_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-past-cont", "sv_past_cont", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-Q-past-cont", "sv_past_cont_question", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-N-past-cont", "sv_past_cont_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-N-Q-past-cont", "sv_past_cont_question_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-future", "sv_future", ContentLayout.SV_RIBBON),
            Subtopic("S-V-Q-future", "sv_future_question", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-N-future", "sv_future_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-N-Q-future", "sv_future_question_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-future-cont", "sv_future_cont", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-Q-future-cont", "sv_future_cont_question", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-N-future-cont", "sv_future_cont_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("S-V-N-Q-future-cont", "sv_future_cont_question_negative", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("Subject–Aux–Verb (Triple)", "conveyor_triple", ContentLayout.CONVEYOR_TRIPLE),
            Subtopic("SV Words (Vocabulary)", "sv_words", ContentLayout.SV_WORDS_CONVEYOR),
            // "Test layout" uses key test_layout (same asset as simple_what — see threeColLessonAssetPaths).
            // Other simple_*.txt lessons (What, Where, How, …) come from [SimpleSentenceUtils.buildSimpleSentenceSubtopics] with THREECOL_TABLE.
            Subtopic("Test layout", "test_layout", ContentLayout.THREECOL_TABLE),
            Subtopic("Can", "can", ContentLayout.THREECOL_TABLE),
            Subtopic("May", "may", ContentLayout.THREECOL_TABLE),
            Subtopic("Wish", "wish", ContentLayout.THREECOL_TABLE),
            Subtopic("How about", "how_about", ContentLayout.THREECOL_TABLE),
            Subtopic("Feels like", "feels_like", ContentLayout.THREECOL_TABLE),
            Subtopic("Need to", "need_to", ContentLayout.THREECOL_TABLE),
            Subtopic("Must", "must", ContentLayout.THREECOL_TABLE),
            Subtopic("Should", "should", ContentLayout.THREECOL_TABLE),
            Subtopic("Used to", "used_to", ContentLayout.THREECOL_TABLE),
            Subtopic("Make", "make", ContentLayout.THREECOL_TABLE),
            Subtopic("It", "it", ContentLayout.THREECOL_TABLE),
            Subtopic("There", "there", ContentLayout.THREECOL_TABLE),
            Subtopic("This / That", "this_that", ContentLayout.THREECOL_TABLE),
            Subtopic("These / Those", "these_those", ContentLayout.THREECOL_TABLE),
            Subtopic("Preposition plus", "preposition_plus", ContentLayout.THREECOL_TABLE),
            Subtopic("Be verb", "be_verb", ContentLayout.THREECOL_TABLE),
            Subtopic("Be verb plus", "be_verb_plus", ContentLayout.THREECOL_TABLE),
            Subtopic("Have verb", "have_verb", ContentLayout.THREECOL_TABLE),
            Subtopic("Noun (sentences)", "noun", ContentLayout.THREECOL_TABLE),
            Subtopic("Single command lecture", "single_command_lecture", ContentLayout.LECTURE),
            Subtopic("Prepositions", "prepositions", ContentLayout.THREECOL_TABLE)
        ) + SimpleSentenceUtils.buildSimpleSentenceSubtopics(assetManager)),
        Topic("SVO Practice", listOf(
            Subtopic("SVO sentences list", "svo_sentences"),
            Subtopic("SVO Eat", "svo_eat"),
            Subtopic("SVO Play", "svo_play")
        )),
        Topic("Pronunciation", listOf(Subtopic("Short i vs Long ee", "pron_short_i_long_ee", ContentLayout.TABLE_DISPLAY)) + buildPronunciationSubtopics(assetManager)),
        Topic("Table Tests", listOf(
            Subtopic("2-Column Table", "table_test_2col", ContentLayout.TABLE_DISPLAY),
            Subtopic("3-Column Table", "table_test_3col", ContentLayout.TABLE_DISPLAY),
            Subtopic("4-Column Table", "table_test_4col", ContentLayout.TABLE_DISPLAY)
        )),
        // POC: tap topic to show subtopics as iPhone-style buttons in content (no drawer expansion).
        Topic("POC Menu", listOf(
            Subtopic("Introduction (Bengali)", "intro_bengali", ContentLayout.TEXT_DISPLAY),
            Subtopic("Mic Test", "mic_test", ContentLayout.MIC_SPEAKER_TEST),
            Subtopic("Translation Practice", "translation_practice", ContentLayout.PRACTICE_THREE_AREA)
        ))
    )

    /** Level 1: Alphabet and Noun & Pronoun topics with subtopics. */
    fun getLevel1Topics(): List<Topic> = listOf(
        Topic("Alphabet", listOf(
            Subtopic("A-Z letter alphabet", "level1_alphabet_az"),
            Subtopic("Vowel vs Consonant", "level1_vowel_consonant")
        )),
        Topic("Noun & Pronoun", listOf(
            // Distinct title from SVO "Noun (sentences)" — different layout (tabs vs 3-column table).
            Subtopic("Noun (categories)", "level1_noun", ContentLayout.NOUN_TABS),
            Subtopic("Pronoun", "level1_pronoun")
        ))
    )

    /** Pronunciation: each *_sound.txt in Lessons/pronunciation becomes a subtopic. */
    fun buildPronunciationSubtopics(assetManager: AssetManager): List<Subtopic> {
        val files = try {
            assetManager.list("Lessons/pronunciation")?.filter { it.endsWith("_sound.txt") }
                ?.sorted() ?: emptyList()
        } catch (_: Exception) { emptyList() }
        return files.map { filename ->
            val title = filename.removeSuffix(".txt")
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
            Subtopic(title, "pron:$filename", ContentLayout.TABLE_DISPLAY)
        }
    }

    /**
     * Flat list of all subtopics in drawer order: Level 1 topics first, then [getTopicList] in order.
     * Used for prev/next lesson in the app top bar.
     */
    fun getAllSubtopicsInNavigationOrder(assetManager: AssetManager): List<Subtopic> = buildList {
        for (topic in getLevel1Topics()) addAll(topic.subtopics)
        for (topic in getTopicList(assetManager)) addAll(topic.subtopics)
    }

    /** Build flat drawer list: Level 1 header + topic headers for the given topics. */
    fun buildDrawerItems(topics: List<Topic>): List<DrawerItem> {
        val items = mutableListOf<DrawerItem>()
        val level1Topics = getLevel1Topics()
        items.add(DrawerItem.LevelHeader("Level 1", level1Topics, expanded = false))
        for ((i, topic) in topics.withIndex()) {
            items.add(DrawerItem.TopicHeader(topic, level1Topics.size + i, expanded = false))
        }
        return items
    }

    /** Topic-level badge: "x/y" where x = subtopics attempted (prof > 0), y = total subtopics. */
    fun getTopicProgressSummary(topic: Topic, getProficiency: (String) -> Int): String {
        val total = topic.subtopics.size
        if (total == 0) return "0/0"
        val attempted = topic.subtopics.count { getProficiency(it.actionKey) > 0 }
        return "$attempted/$total"
    }
}
