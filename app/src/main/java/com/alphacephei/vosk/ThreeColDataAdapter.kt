package com.alphacephei.vosk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for 3-col lesson table (layout_threecol_content in layout_lesson_base shell): Bengali | English + Hint rows.
 */
class ThreeColDataAdapter(
    private var items: List<ThreeColRow>
) : RecyclerView.Adapter<ThreeColDataAdapter.RowViewHolder>() {

    /** true = Learning (show hint + full English), false = Practice (hide until answered). */
    var learningMode: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /**
     * Prepositions lesson + TEST tab only: on the current row, show masked English (e.g. "Abide ---") until answered.
     */
    var prepositionsTestMasking: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /**
     * Prepositions TEST: show full English on this row only during the brief reveal before advancing
     * (correct answer, or after 3 wrong attempts). -1 = no reveal.
     */
    var prepositionsTestRevealFullRowIndex: Int = -1
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /** Per-row answer state for Practice mode. */
    private var answered: MutableList<Boolean> = MutableList(items.size) { false }
    private var correct: MutableList<Boolean> = MutableList(items.size) { false }
    /** Per-row spoken text (what user said) for Practice mode. */
    private var spoken: MutableList<String> = MutableList(items.size) { "" }

    /** Currently active row index for visual highlight. */
    private var currentIndex: Int = 0

    class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bengali: TextView = view.findViewById(R.id.threecol_bengali)
        val english: TextView = view.findViewById(R.id.threecol_english)
        val hint: TextView = view.findViewById(R.id.threecol_hint)
        val statusMark: TextView = view.findViewById(R.id.threecol_status_mark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.layout_item_threecol_row, parent, false)
        return RowViewHolder(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val row = items[position]
        val ctx = holder.itemView.context
        val baseEnglishColor = ctx.getColor(R.color.lesson_topic_bar_background)
        val successColor = ctx.getColor(R.color.control_success)
        val dangerColor = ctx.getColor(R.color.control_danger)
        val textPrimary = ctx.getColor(R.color.text_primary)

        // Highlight the active row; otherwise alternate row background for high contrast
        if (position == currentIndex) {
            holder.itemView.setBackgroundResource(R.drawable.bg_threecol_row_active)
        } else {
            val stripeColor = if (position % 2 == 0) ctx.getColor(R.color.threecol_row_even) else ctx.getColor(R.color.threecol_row_odd)
            holder.itemView.setBackgroundColor(stripeColor)
        }

        // Default: no status mark
        holder.statusMark.text = ""

        if (learningMode) {
            holder.bengali.text = row.bengali
            // Always show English + hint in Learning mode (display form: first alternative only).
            holder.english.text = MatchNormalizer.textForSpeakAndDisplay(row.english)
            holder.english.setTextColor(baseEnglishColor)
            holder.hint.text = row.hint
            holder.hint.visibility = if (row.hint.isNotBlank()) View.VISIBLE else View.GONE
            // Show tick/cross in the fixed-width mark for answered rows.
            if (position in answered.indices && answered[position]) {
                holder.statusMark.text = if (position in correct.indices && correct[position]) "✓" else "✗"
                holder.statusMark.setTextColor(if (position in correct.indices && correct[position]) successColor else dangerColor)
            }
        } else if (prepositionsTestMasking) {
            // Prepositions TEST: col1 = "Word __" until reveal; full English only on reveal row before next item.
            holder.hint.visibility = View.GONE
            when {
                position == prepositionsTestRevealFullRowIndex -> {
                    holder.bengali.text = MatchNormalizer.textForSpeakAndDisplay(row.english)
                    holder.bengali.setTextColor(textPrimary)
                    val said = spoken.getOrNull(position) ?: ""
                    holder.english.text = MatchNormalizer.sanitizeSpokenTextForDisplay(said)
                    holder.english.setTextColor(if (position in correct.indices && correct[position]) successColor else dangerColor)
                    holder.statusMark.text = if (position in correct.indices && correct[position]) "✓" else "✗"
                    holder.statusMark.setTextColor(if (position in correct.indices && correct[position]) successColor else dangerColor)
                }
                position in answered.indices && answered[position] -> {
                    val cue = MatchNormalizer.textForSpeakAndDisplay(row.english)
                    // Wrong + still on this row with retries left: keep "Word __". Once done (pass / 3 fails), show full; it stays for earlier rows after advancing.
                    val stillRetryingWrong =
                        position == currentIndex &&
                        position in correct.indices &&
                        !correct[position]
                    holder.bengali.text =
                        if (stillRetryingWrong) PrepositionTestUtils.maskedDisplayEnglish(cue) else cue
                    holder.bengali.setTextColor(textPrimary)
                    val said = spoken.getOrNull(position) ?: ""
                    holder.english.text = MatchNormalizer.sanitizeSpokenTextForDisplay(said)
                    holder.english.setTextColor(if (position in correct.indices && correct[position]) successColor else dangerColor)
                    holder.statusMark.text = if (position in correct.indices && correct[position]) "✓" else "✗"
                    holder.statusMark.setTextColor(if (position in correct.indices && correct[position]) successColor else dangerColor)
                }
                else -> {
                    val cue = MatchNormalizer.textForSpeakAndDisplay(row.english)
                    holder.bengali.text = PrepositionTestUtils.maskedDisplayEnglish(cue)
                    holder.bengali.setTextColor(textPrimary)
                    holder.english.text = ""
                    holder.english.setTextColor(baseEnglishColor)
                }
            }
        } else {
            holder.bengali.text = row.bengali
            // Practice / other TEST: hide English+hint until answered; then show and mark correct/incorrect.
            if (position in answered.indices && answered[position]) {
                val said = spoken.getOrNull(position) ?: ""
                holder.english.text = MatchNormalizer.sanitizeSpokenTextForDisplay(said)
                holder.english.setTextColor(if (position in correct.indices && correct[position]) successColor else dangerColor)
                holder.hint.text = ""
                holder.hint.visibility = View.GONE
                holder.statusMark.text = if (position in correct.indices && correct[position]) "✓" else "✗"
                holder.statusMark.setTextColor(if (position in correct.indices && correct[position]) successColor else dangerColor)
            } else {
                holder.english.text = ""
                holder.english.setTextColor(baseEnglishColor)
                holder.hint.text = ""
                holder.hint.visibility = View.GONE
            }
        }
    }

    fun updateData(newItems: List<ThreeColRow>) {
        items = newItems
        answered = MutableList(items.size) { false }
        correct = MutableList(items.size) { false }
        spoken = MutableList(items.size) { "" }
        prepositionsTestRevealFullRowIndex = -1
        notifyDataSetChanged()
    }

    /**
     * Apply persisted stats loaded from disk.
     * Each entry = [A, B] (practice pass, test pass). No attempts; correct when A or B is non-zero.
     */
    fun applyStatsFrom(stats: List<IntArray>) {
        if (items.isEmpty()) return
        val size = items.size
        if (answered.size < size) {
            answered = MutableList(size) { false }
        }
        if (correct.size < size) {
            correct = MutableList(size) { false }
        }
        for (i in 0 until size) {
            val row = stats.getOrNull(i) ?: continue
            val a = row.getOrNull(0) ?: 0
            val b = row.getOrNull(1) ?: 0
            answered[i] = (a != 0 || b != 0)
            correct[i] = (a != 0 || b != 0)
        }
        notifyDataSetChanged()
    }

    /** Mark row at index as answered with result; used by verification logic. */
    fun markResult(index: Int, isCorrect: Boolean) {
        if (index !in items.indices) return
        if (index >= answered.size) {
            // Resize defensively if data changed unexpectedly.
            answered = MutableList(items.size) { i -> answered.getOrNull(i) ?: false }
            correct = MutableList(items.size) { i -> correct.getOrNull(i) ?: false }
        }
        answered[index] = true
        correct[index] = isCorrect
        notifyItemChanged(index)
    }

    /** Update which row is the current one; used to highlight the active sentence. */
    fun setCurrentIndex(index: Int) {
        if (index !in items.indices) return
        val old = currentIndex
        currentIndex = index
        if (old in 0 until items.size) notifyItemChanged(old)
        notifyItemChanged(currentIndex)
    }

    /** Store what the user said for this row (Practice mode). */
    fun setSpokenText(index: Int, said: String) {
        if (index !in items.indices) return
        if (index >= spoken.size) {
            spoken = MutableList(items.size) { i -> spoken.getOrNull(i) ?: "" }
        }
        spoken[index] = said
        notifyItemChanged(index)
    }

    /** @return Triple(correctCount, testedCount, totalCount). */
    fun getStats(): Triple<Int, Int, Int> {
        val total = items.size
        val correctCount = correct.count { it }
        val testedCount = answered.count { it }
        return Triple(correctCount, testedCount, total)
    }
}

