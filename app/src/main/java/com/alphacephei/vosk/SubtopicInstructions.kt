package com.alphacephei.vosk

/**
 * Returns instruction or description for a subtopic (by actionKey). Shown when user taps the info button.
 * Extracted to reduce MainActivity.kt size.
 */
object SubtopicInstructions {

    fun getInstruction(actionKey: String): String {
        return when (actionKey) {
            "level1_alphabet_az" -> "A–Z letter alphabet: Learn the correct pronunciation of all 26 English letters. Tap letters in the table to hear Bengali pronunciation. You can also watch the introduction video."
            "level1_vowel_consonant" -> "Vowel vs Consonant: The vowels are A, E, I, O, U. The rest are consonants. See the alphabet table for pronunciation of each letter."
            "level1_noun" -> """কোনোও কিছুর নামকে নাউন বলা হয়। এটা হতে পারে কোনোও ব্যক্তির নাম। হতে পারে কোনোও বস্তুর নাম। হতে পারে কোনোও আবেগের নাম। হতে পারে কোনোও স্থানের নাম।
 হতে পারে সমষ্টিগত কোনোও কিছুর নাম।
 যেমন ব্যক্তির নাম বলতে বোঝায় রহিম করিম যদুমদু ইত্যাদি।
 আর স্থানের নাম বলতে  বুঝানো যায় বাংলাদেশ, ঢাকা, কুমিল্লা, আমেরিকা, লন্ডন ইত্যাদি।
 আর কোনোও  বস্তুর নাম বুঝাতে মনে হয়।
 চেয়ার টেবিল ইত্যাদি।
 তাছাড়া যেমন সমষ্টিগত নাম, যেমন একটা
 টিম,  ফ্যামিলি।
 আর্মি। এগুলো কোনোও  নির্দিষ্ট  ব্যক্তিকে বোঝানো হয় না, কিন্তু এটাকে সমষ্টিগত ভাবে, অনেকজনকে একসাথে বোঝানো হয়। সেজন্য এটাকে কালেক্টিভ নাউন ও বলা হয়।
 তাছাড়া  কিছু আবেগ, অনুভূতির নাম, যেমন,  ভালোবাসা, সততা,  ইত্যাদি নাউন হতে পারে।"""
            "level1_pronoun" -> "Pronoun: A pronoun is a word that takes the place of a noun (e.g. I, you, he, she, it). Tap to open the Parts of speech lesson."
            "intro_bengali" -> "Introduction in Bengali: Listen to the app read the introduction text. Tap the speaker button to hear it again."
            "mic_test" -> "Mic Test: Select your language (Bengali or English), then tap the mic to speak. The app will show what it heard. Use this to check that the microphone works."
            "conversation_first_meeting" -> "First meeting: Practice a simple conversation. Choose who starts (You or App). On your turn, tap the mic and speak; on the app's turn, the app will speak. Sentences are marked YOU or APP."
            "translation_practice" -> "Translation Practice: The app will say a word in Bengali. You say the English meaning. Tap the mic when you are ready to speak."
            "lesson_file" -> "Load lesson: Open a .txt lesson file from your device. Each line should have 4 pipe-separated fields: English Q | Bengali Q | English A | Bengali A."
            "lesson_introduce" -> "Introduce lesson: Load a lesson file and hear each question-answer pair. Good for first-time learning."
            "lesson_incorrect" -> "Practice incorrect words: Load a saved list of words you got wrong before, and practice them again."
            "verb_basic" -> "Learn verb (DO, HAVE, GO): Practice saying the English conjugation when you hear the Bengali. Choose a verb and subject."
            "verb_tenses" -> "Learn tenses: Practice 12 tenses for a verb. You hear Bengali, you say the English sentence."
            "verb_regular", "verb_irregular" -> "Verb lesson: You hear the Bengali meaning, you say the English verb. Three wrong answers and the app moves to the next word."
            "grammar_pos" -> "Parts of speech: Opens a lesson on nouns, verbs, adjectives, and other word types in English."
            "grammar_svo" -> "SVO sentences: Learn Subject–Verb–Object sentence structure."
            "diagram_1to3", "diagram_3to1" -> "Diagram: View a grammar diagram. Tap the speaker to hear the description."
            "tense_diagram" -> "Tenses hierarchy: View a diagram showing Future, Present, Past and their four aspects (Simple, Continuous, Perfect, Perfect Continuous)."
            "pron_short_i_long_ee" -> "Short i vs Long ee: Practice pronouncing word pairs (e.g. sit/seat, bit/beat). The app says the word; you repeat. Three tries per word."
            "svo:I" -> "SVO I: Practice sentences with subject I. Hear Bengali, say English."
            "sv_ribbon" -> "S-V-simple: Subject + Verb (e.g. I play, They play). Two ribbons: subject left, verb right. Learning / Practice, Start."
            "sv_I_four_sections" -> "Simple present I (4 sections): Four rows — Affirmative (I play), Question (Do I play?), Negative (I don't play), Negative question (Don't I play?). Learning: app speaks Bengali then English for each. Practice: you say the English for each; 3 wrong then next section. Start to begin."
            "sv_I_past_four_sections" -> "Simple past I (4 sections): Same layout as present — Affirmative (I played), Question (Did I play?), Negative (I didn't play), Negative question (Didn't I play?). Learning / Practice / Test. Start to begin."
            "sv_I_future_four_sections" -> "Simple future I (4 sections): Same layout as present — Affirmative (I will play), Question (Will I play?), Negative (I won't play), Negative question (Won't I play?). Learning / Practice / Test. Start to begin."
            "sv_I_past_cont_four_sections" -> "Past continuous I (4 sections): I was playing, Was I playing?, I wasn't playing, Wasn't I playing? Same layout. Learning / Practice / Test. Start to begin."
            "sv_I_future_cont_four_sections" -> "Future continuous I (4 sections): I will be playing, Will I be playing?, I won't be playing, Won't I be playing? Same layout. Learning / Practice / Test. Start to begin."
            "sv_ribbon_I_negative" -> "S-V-N-simple: Subject + don't/doesn't + verb (e.g. I don't play, He doesn't play). Three ribbons. Learning / Practice, Start."
            "sv_I_question" -> "Do I ...? (Triple): Triple conveyor with left = Do, middle = I, right = verbs. Sentence: Do I play? Learning / Practice, Start, 3 wrong then next."
            "sv_I_question_negative" -> "Don't I ...? (Triple): Triple conveyor with left = Don't, middle = I, right = verbs. Sentence: Don't I play? Learning / Practice, Start, 3 wrong then next."
            "sv_You_question" -> "Do you ...? (Triple): Triple conveyor with left = Do, middle = You, right = verbs. Sentence: Do you play? Learning / Practice, Start, 3 wrong then next."
            "sv_You_question_negative" -> "Don't you ...? (Triple): Triple conveyor with left = Don't, middle = you, right = verbs. Sentence: Don't you play? Learning / Practice, Start, 3 wrong then next."
            "sv_You_question_ing" -> "Are you ...ing? (Triple): Triple conveyor with left = Are, middle = you, right = -ing verbs. Sentence: Are you playing? Learning / Practice, Start, 3 wrong then next."
            "sv_subject_question" -> "S-V-Q-simple: Do/Does + subject + verb? (e.g. Do they play? Does he play?). Three ribbons. Learning / Practice, Start."
            "sv_subject_negative" -> "S-V-N-simple: Subject + don't/doesn't + verb (e.g. I don't play, He doesn't play). Three ribbons. Learning / Practice, Start."
            "sv_subject_question_negative" -> "S-V-N-Q-simple: Don't/Doesn't + subject + verb? (e.g. Don't they play? Doesn't he play?). Three ribbons. Learning / Practice, Start."
            "sv_cont" -> "S-V-cont: Present continuous. Subject + am/is/are + verb-ing (e.g. I am playing, They are playing). Three ribbons. Learning / Practice, Start."
            "sv_cont_question" -> "S-V-Q-cont: Am/Is/Are + subject + verb-ing? (e.g. Am I playing? Are they playing?). Three ribbons. Learning / Practice, Start."
            "sv_cont_negative" -> "S-V-N-cont: Subject + am not/isn't/aren't + verb-ing (e.g. I am not playing, He isn't playing). Three ribbons. Learning / Practice, Start."
            "sv_cont_question_negative" -> "S-V-N-Q-cont: Aren't/Isn't + subject + verb-ing? (e.g. Aren't they playing? Isn't he playing?). Three ribbons. Learning / Practice, Start."
            "sv_past" -> "S-V-past: Simple past. Subject + past verb (e.g. I played, He played). Two ribbons. Learning / Practice, Start."
            "sv_past_question" -> "S-V-Q-past: Did + subject + verb? (e.g. Did they play? Did he play?). Three ribbons. Learning / Practice, Start."
            "sv_past_negative" -> "S-V-N-past: Subject + didn't + verb (e.g. I didn't play, He didn't play). Three ribbons. Learning / Practice, Start."
            "sv_past_question_negative" -> "S-V-N-Q-past: Didn't + subject + verb? (e.g. Didn't they play? Didn't he play?). Three ribbons. Learning / Practice, Start."
            "sv_past_cont" -> "S-V-past-cont: Past continuous. Subject + was/were + verb-ing (e.g. I was playing, They were playing). Three ribbons. Learning / Practice, Start."
            "sv_past_cont_question" -> "S-V-Q-past-cont: Was/Were + subject + verb-ing? (e.g. Was I playing? Were they playing?). Three ribbons. Learning / Practice, Start."
            "sv_past_cont_negative" -> "S-V-N-past-cont: Subject + wasn't/weren't + verb-ing (e.g. I wasn't playing, He wasn't playing). Three ribbons. Learning / Practice, Start."
            "sv_past_cont_question_negative" -> "S-V-N-Q-past-cont: Wasn't/Weren't + subject + verb-ing? (e.g. Wasn't I playing? Weren't they playing?). Three ribbons. Learning / Practice, Start."
            "sv_future" -> "S-V-future: Simple future. Subject + will + verb (e.g. I will play, He will play). Two ribbons. Learning / Practice, Start."
            "sv_future_question" -> "S-V-Q-future: Will + subject + verb? (e.g. Will they play? Will he play?). Three ribbons. Learning / Practice, Start."
            "sv_future_negative" -> "S-V-N-future: Subject + won't + verb (e.g. I won't play, He won't play). Three ribbons. Learning / Practice, Start."
            "sv_future_question_negative" -> "S-V-N-Q-future: Won't + subject + verb? (e.g. Won't they play? Won't he play?). Three ribbons. Learning / Practice, Start."
            "sv_future_cont" -> "S-V-future-cont: Future continuous. Subject + will be + verb-ing (e.g. I will be playing, They will be playing). Three ribbons. Learning / Practice, Start."
            "sv_future_cont_question" -> "S-V-Q-future-cont: Will + subject + be + verb-ing? (e.g. Will I be playing? Will they be playing?). Three ribbons. Learning / Practice, Start."
            "sv_future_cont_negative" -> "S-V-N-future-cont: Subject + won't be + verb-ing (e.g. I won't be playing, He won't be playing). Three ribbons. Learning / Practice, Start."
            "sv_future_cont_question_negative" -> "S-V-N-Q-future-cont: Won't + subject + be + verb-ing? (e.g. Won't I be playing? Won't they be playing?). Three ribbons. Learning / Practice, Start."
            "conveyor_triple" -> "Subject–Aux–Verb: Learning mode shows three belts and speaks Bengali then English. Practice mode shows only Bengali and you say the English. Use Learning / Practice to switch."
            "sv_words" -> "SV Words: Learning = app speaks Bengali then English, you repeat. Practice = app speaks Bengali only, you say the English word. Belt shows words (learning) or Bengali (practice). Stats in practice. Start / Stop / Pause / Resume."
            else -> when {
                actionKey.startsWith("simple_") ->
                    "${SimpleSentenceUtils.simpleSentenceLessonTitle(actionKey)}: Loads Lessons/SVO/$actionKey.txt (English,Bengali,Pronunciation per line). Learning: two bubbles (Bengali, English), app speaks Bengali then English, you repeat; correct → next. Practice: Bengali only, you say English; app says Correct and keeps stats. Start to begin."
                actionKey == "preposition_plus" ->
                    "${SimpleSentenceUtils.simpleSentenceLessonTitle(actionKey)}: Loads Lessons/preposition_plus.txt (English,Bengali,Pronunciation per line). 3-column table: Learning / Practice / Test / V."
                actionKey in setOf("be_verb", "be_verb_plus", "have_verb") ->
                    "${SimpleSentenceUtils.simpleSentenceLessonTitle(actionKey)}: Loads Lessons/SVO/$actionKey.txt (English,Bengali,Pronunciation per line). 3-column table: Learning / Practice / Test / V."
                actionKey == "noun" ->
                    "${SimpleSentenceUtils.simpleSentenceLessonTitle(actionKey)}: Loads Lessons/SVO/noun.txt — English, Bengali, pronunciation; optional 4th field (e.g. noun type) is ignored. Same 3-column lesson shell as Where/How. Open under **SVO → Noun (sentences)**, not Level 1 Noun (categories)."
                else -> null
            }
        } ?: when (actionKey) {
            "svo_sentences", "svo_eat", "svo_play" -> "SVO Practice: Hear a sentence in Bengali and say it in English. Tap mic when ready."
            "table_test_2col", "table_test_3col", "table_test_4col" -> "Table test: View a sample table. Tap cells to hear the word (if supported)."
            else -> when {
                actionKey.startsWith("pron:") -> "Pronunciation lesson: The app will speak each word. You repeat. Your pronunciation is checked. Three wrong tries then next word."
                actionKey.startsWith("svo:") -> "SVO lesson: Hear Bengali, say the English sentence. Tap mic to speak."
                else -> "Tap to open this lesson. No additional instruction."
            }
        }
    }
}
