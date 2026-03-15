package com.alphacephei.vosk

/**
 * Maps SVO drawer action keys to asset path and display topic name.
 * Files are in Lessons/SVO ( .txt) format (English,Bengali[,Pronunciation]).
 */
object SvoDrawerAssets {
    fun get(actionKey: String): Pair<String, String>? = when (actionKey) {
        "svo:I" -> "Lessons/SVO/SV_I.txt" to "SVO I"
        "sv_ribbon" -> "Lessons/SVO/SV_I.txt" to "S-V simple (I)"
        "sv_I_four_sections" -> "Lessons/SVO/SV_I.txt" to "Simple present I (4 sections)"
        "sv_I_past_four_sections" -> "Lessons/SVO/SV_I_past.txt" to "Simple past I"
        "sv_I_future_four_sections" -> "Lessons/SVO/SV_I_future.txt" to "Simple future I"
        "sv_I_past_cont_four_sections" -> "Lessons/SVO/SV_I_past_cont.txt" to "Past continuous I"
        "sv_I_future_cont_four_sections" -> "Lessons/SVO/SV_I_future_cont.txt" to "Future continuous I"
        "sv_ribbon_I_negative" -> "Lessons/SVO/SV_I_negative.txt" to "S-V negative (I)"
        "sv_I_question" -> "Lessons/SVO/SV_You_question.txt" to "Do I ...?"
        "sv_I_question_negative" -> "Lessons/SVO/SV_I_question_negative.txt" to "Don't I ...?"
        "sv_You_question" -> "Lessons/SVO/SV_You_question.txt" to "Do you ...?"
        "sv_You_question_negative" -> "Lessons/SVO/SV_You_question_negative.txt" to "Don't you ...?"
        "sv_You_question_ing" -> "Lessons/SVO/SV_Youing.txt" to "Are you ...ing?"
        "sv_subject_question" -> "Lessons/SVO/SV_They_question.txt" to "Subject question"
        "sv_subject_negative" -> "Lessons/SVO/SV_They_cont_negative.txt" to "S-V negative"
        "sv_subject_question_negative" -> "Lessons/SVO/SV_They_cont_question_negative.txt" to "Subject Q negative"
        "sv_cont" -> "Lessons/SVO/SV_Iing.txt" to "S-V continuous (I)"
        "sv_cont_question" -> "Lessons/SVO/SV_I_cont_question.txt" to "S-V continuous question"
        "sv_cont_negative" -> "Lessons/SVO/SV_I_cont_negative.txt" to "S-V continuous negative"
        "sv_cont_question_negative" -> "Lessons/SVO/SV_They_cont_question_negative.txt" to "S-V continuous Q negative"
        "sv_past" -> "Lessons/SVO/SV_I_past.txt" to "S-V past (I)"
        "sv_past_question" -> "Lessons/SVO/SV_You_past_question.txt" to "S-V past question"
        "sv_past_negative" -> "Lessons/SVO/SV_I_past_negative.txt" to "S-V past negative"
        "sv_past_question_negative" -> "Lessons/SVO/SV_I_past_question_negative.txt" to "S-V past Q negative"
        "sv_past_cont" -> "Lessons/SVO/SV_I_past_cont.txt" to "S-V past continuous"
        "sv_past_cont_question" -> "Lessons/SVO/SV_You_past_cont_question.txt" to "S-V past cont question"
        "sv_past_cont_negative" -> "Lessons/SVO/SV_You_past_cont_negative.txt" to "S-V past cont negative"
        "sv_past_cont_question_negative" -> "Lessons/SVO/SV_You_past_cont_question_negative.txt" to "S-V past cont Q negative"
        "sv_future" -> "Lessons/SVO/SV_I_future.txt" to "S-V future (I)"
        "sv_future_question" -> "Lessons/SVO/SV_We_future_question.txt" to "S-V future question"
        "sv_future_negative" -> "Lessons/SVO/SV_I_future_negative.txt" to "S-V future negative"
        "sv_future_question_negative" -> "Lessons/SVO/SV_You_future_question_negative.txt" to "S-V future Q negative"
        "sv_future_cont" -> "Lessons/SVO/SV_I_future_cont.txt" to "S-V future continuous"
        "sv_future_cont_question" -> "Lessons/SVO/SV_You_future_cont_question.txt" to "S-V future cont question"
        "sv_future_cont_negative" -> "Lessons/SVO/SV_I_future_cont_negative.txt" to "S-V future cont negative"
        "sv_future_cont_question_negative" -> "Lessons/SVO/SV_They_future_cont_question_negative.txt" to "S-V future cont Q negative"
        "conveyor_triple" -> "Lessons/SVO/SV_I.txt" to "Subject–Aux–Verb (Triple)"
        "sv_words" -> "Lessons/SVO/SV_words.txt" to "SV Words"
        else -> null
    }
}
