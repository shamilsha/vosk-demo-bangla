package com.alphacephei.vosk

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for the reusable 3-column vocabulary list (Word | Pronunciation | Meaning) in the V tab.
 * Shows tick (pass) or cross (fail) when the user has attempted the word. Current row is not tinted (avoids layout noise).
 */
class LessonVocabAdapter(
    private var rows: List<LessonVocabRow>
) : RecyclerView.Adapter<LessonVocabAdapter.VocabViewHolder>() {

    var currentIndex: Int = 0
        set(value) {
            val old = field
            field = value.coerceIn(0, (rows.size - 1).coerceAtLeast(0))
            if (old != field) {
                if (old in rows.indices) notifyItemChanged(old)
                if (field in rows.indices) notifyItemChanged(field)
            }
        }

    /** Pass/fail per word (lowercase key): true = pass (tick), false = fail (cross). Not cleared on updateRows so marks persist when list is refreshed. */
    private val resultPerWord = mutableMapOf<String, Boolean>()
    /** What user spoke per word (lowercase key). */
    private val spokenPerWord = mutableMapOf<String, String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VocabViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_lesson_vocab_row, parent, false)
        return VocabViewHolder(v)
    }

    override fun onBindViewHolder(holder: VocabViewHolder, position: Int) {
        val row = rows.getOrNull(position) ?: return
        holder.word.text = row.word
        holder.pronunciation.text = row.pronunciation
        holder.meaning.text = row.meaning
        val wordKey = row.word.trim().lowercase()
        holder.spoken.text = spokenPerWord[wordKey].orEmpty()
        val cellBg = if (position == currentIndex) R.drawable.bg_tense_triplet_cell_focus else R.drawable.bg_tense_triplet_cell
        holder.word.setBackgroundResource(cellBg)
        holder.pronunciation.setBackgroundResource(cellBg)
        holder.meaning.setBackgroundResource(cellBg)
        holder.spokenCell.setBackgroundResource(cellBg)
        holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        val result = resultPerWord[wordKey]
        when (result) {
            true -> {
                holder.resultIcon.visibility = View.VISIBLE
                holder.resultIcon.setImageResource(R.drawable.ic_check_tick)
            }
            false -> {
                holder.resultIcon.visibility = View.VISIBLE
                holder.resultIcon.setImageResource(R.drawable.ic_cross)
            }
            null -> holder.resultIcon.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int = rows.size

    fun updateRows(newRows: List<LessonVocabRow>) {
        rows = newRows
        currentIndex = 0
        notifyDataSetChanged()
    }

    /** Clear in-row tick/cross and "You said" text (e.g. when switching lesson mode tabs). Progress file is unchanged. */
    fun clearSessionMarksAndSpoken() {
        resultPerWord.clear()
        spokenPerWord.clear()
        notifyDataSetChanged()
    }

    /** Mark word at position as pass (true) or fail (false); stored by word so marks persist when list is refreshed. */
    fun setResult(position: Int, passed: Boolean) {
        if (position !in rows.indices) return
        val wordKey = rows[position].word.trim().lowercase()
        resultPerWord[wordKey] = passed
        notifyItemChanged(position)
    }

    /** Save recognized user speech into fixed "You said" column for this word. */
    fun setSpokenText(position: Int, spoken: String) {
        if (position !in rows.indices) return
        val wordKey = rows[position].word.trim().lowercase()
        val cleaned = MatchNormalizer.sanitizeSpokenTextForDisplay(spoken).trim()
        val raw = spoken.trim()
        spokenPerWord[wordKey] = when {
            cleaned.isNotBlank() -> cleaned
            raw.isNotBlank() -> raw
            else -> "(no speech)"
        }
        notifyItemChanged(position)
    }

    class VocabViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val word: TextView = v.findViewById(R.id.lesson_vocab_word)
        val pronunciation: TextView = v.findViewById(R.id.lesson_vocab_pronunciation)
        val meaning: TextView = v.findViewById(R.id.lesson_vocab_meaning)
        val spoken: TextView = v.findViewById(R.id.lesson_vocab_spoken)
        val spokenCell: LinearLayout = v.findViewById(R.id.lesson_vocab_spoken_cell)
        val resultIcon: ImageView = v.findViewById(R.id.lesson_vocab_result_icon)
    }
}
