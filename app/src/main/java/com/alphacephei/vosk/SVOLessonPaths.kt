package com.alphacephei.vosk

/**
 * Path helpers for SVO lesson assets. Extracted to reduce MainActivity.kt size.
 */
object SVOLessonPaths {

    /** Present continuous: file name for subject. I -> SV_Iing.txt, He -> SV_Heing.txt */
    fun contIngFileName(subj: String): String =
        "Lessons/SVO/SV_${if (subj == "I") "I" else subj}ing.txt"

    /** SV ribbon (present): SV_I.txt, SV_He.txt, etc. */
    fun svRibbonFileName(subj: String): String = "Lessons/SVO/SV_$subj.txt"

    /** SV ribbon past: SV_I_past.txt, etc. */
    fun svRibbonPastFileName(subj: String): String = "Lessons/SVO/SV_${subj}_past.txt"

    /** SV ribbon future: SV_I_future.txt, etc. */
    fun svRibbonFutureFileName(subj: String): String = "Lessons/SVO/SV_${subj}_future.txt"

    /** Triple conveyor: SV_I_negative.txt, SV_He_question.txt, etc. */
    fun svTripleFileName(subj: String, suffix: String): String = "Lessons/SVO/SV_${subj}_$suffix.txt"
}
