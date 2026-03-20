package com.alphacephei.vosk

import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView

/** Adapter for tense triplet rows (Present | Past | Future). */
class TenseTripletAdapter(
    private var items: List<TenseTripletRow>
) : RecyclerView.Adapter<TenseTripletAdapter.TripletViewHolder>() {
    enum class DisplayMode { LEARNING, PRACTICE, TEST, VOCAB }
    var displayMode: DisplayMode = DisplayMode.LEARNING
    private val rowResultMarks = mutableMapOf<Int, Triple<Boolean?, Boolean?, Boolean?>>()
    private val rowSpokenText = mutableMapOf<Int, Triple<String?, String?, String?>>()
    private var showPresentColumn: Boolean = true
    private var showPastColumn: Boolean = true
    private var showFutureColumn: Boolean = true
    private var currentIndex: Int = 0

    class TripletViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pCell: LinearLayout = view.findViewById(R.id.triplet_present_cell)
        val paCell: LinearLayout = view.findViewById(R.id.triplet_past_cell)
        val fCell: LinearLayout = view.findViewById(R.id.triplet_future_cell)
        val pEngRow: FrameLayout = view.findViewById(R.id.triplet_present_english_row)
        val pEng: TextView = view.findViewById(R.id.triplet_present_english)
        val pBn: TextView = view.findViewById(R.id.triplet_present_bengali)
        val pHint: TextView = view.findViewById(R.id.triplet_present_hint)
        val pEngMark: ImageView = view.findViewById(R.id.triplet_present_english_mark)
        val pBnMark: ImageView = view.findViewById(R.id.triplet_present_bengali_mark)

        val paEngRow: FrameLayout = view.findViewById(R.id.triplet_past_english_row)
        val paEng: TextView = view.findViewById(R.id.triplet_past_english)
        val paBn: TextView = view.findViewById(R.id.triplet_past_bengali)
        val paHint: TextView = view.findViewById(R.id.triplet_past_hint)
        val paEngMark: ImageView = view.findViewById(R.id.triplet_past_english_mark)
        val paBnMark: ImageView = view.findViewById(R.id.triplet_past_bengali_mark)

        val fEngRow: FrameLayout = view.findViewById(R.id.triplet_future_english_row)
        val fEng: TextView = view.findViewById(R.id.triplet_future_english)
        val fBn: TextView = view.findViewById(R.id.triplet_future_bengali)
        val fHint: TextView = view.findViewById(R.id.triplet_future_hint)
        val fEngMark: ImageView = view.findViewById(R.id.triplet_future_english_mark)
        val fBnMark: ImageView = view.findViewById(R.id.triplet_future_bengali_mark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripletViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_tense_triplet, parent, false)
        return TripletViewHolder(view)
    }

    override fun onViewRecycled(holder: TripletViewHolder) {
        hideAllMarks(holder)
        super.onViewRecycled(holder)
    }

    override fun onBindViewHolder(holder: TripletViewHolder, position: Int) {
        val row = items[position]
        clearTextViewCompoundDrawables(holder)

        holder.pEng.text = if (showPresentColumn) colorEnglish(row.present.english, tense = Tense.PRESENT) else ""
        holder.pBn.text = if (showPresentColumn) row.present.bengali else ""
        holder.pHint.text = if (showPresentColumn) row.present.hint else ""

        holder.paEng.text = if (showPastColumn) colorEnglish(row.past.english, tense = Tense.PAST) else ""
        holder.paBn.text = if (showPastColumn) row.past.bengali else ""
        holder.paHint.text = if (showPastColumn) row.past.hint else ""

        holder.fEng.text = if (showFutureColumn) colorEnglish(row.future.english, tense = Tense.FUTURE) else ""
        holder.fBn.text = if (showFutureColumn) row.future.bengali else ""
        holder.fHint.text = if (showFutureColumn) row.future.hint else ""
        val spoken = rowSpokenText[position] ?: Triple(null, null, null)

        when (displayMode) {
            DisplayMode.LEARNING, DisplayMode.VOCAB -> {
                holder.pBn.visibility = View.VISIBLE
                holder.paBn.visibility = View.VISIBLE
                holder.fBn.visibility = View.VISIBLE
                holder.pHint.visibility = View.VISIBLE
                holder.paHint.visibility = View.VISIBLE
                holder.fHint.visibility = View.VISIBLE
            }
            DisplayMode.PRACTICE -> {
                holder.pBn.visibility = View.VISIBLE
                holder.paBn.visibility = View.VISIBLE
                holder.fBn.visibility = View.VISIBLE
                holder.pHint.visibility = View.GONE
                holder.paHint.visibility = View.GONE
                holder.fHint.visibility = View.GONE
            }
            DisplayMode.TEST -> {
                // TEST: only Bengali line(s) — English row is hidden entirely (no blank band).
                holder.pEngRow.visibility = View.GONE
                holder.paEngRow.visibility = View.GONE
                holder.fEngRow.visibility = View.GONE
                holder.pBn.visibility = View.VISIBLE
                holder.paBn.visibility = View.VISIBLE
                holder.fBn.visibility = View.VISIBLE
                holder.pHint.visibility = View.GONE
                holder.paHint.visibility = View.GONE
                holder.fHint.visibility = View.GONE
                // Single line per cell: either lesson Bengali or replaced recognized text (never both).
                if (showPresentColumn && !spoken.first.isNullOrBlank()) holder.pBn.text = spoken.first
                if (showPastColumn && !spoken.second.isNullOrBlank()) holder.paBn.text = spoken.second
                if (showFutureColumn && !spoken.third.isNullOrBlank()) holder.fBn.text = spoken.third
            }
        }
        val marks = rowResultMarks[position] ?: Triple(null, null, null)
        hideAllMarks(holder)
        if (displayMode == DisplayMode.TEST) {
            applyResultMarkToImage(holder.pBnMark, if (showPresentColumn) marks.first else null)
            applyResultMarkToImage(holder.paBnMark, if (showPastColumn) marks.second else null)
            applyResultMarkToImage(holder.fBnMark, if (showFutureColumn) marks.third else null)
        } else {
            applyResultMarkToImage(holder.pEngMark, if (showPresentColumn) marks.first else null)
            applyResultMarkToImage(holder.paEngMark, if (showPastColumn) marks.second else null)
            applyResultMarkToImage(holder.fEngMark, if (showFutureColumn) marks.third else null)
        }
        val focused = position == currentIndex
        val bg = if (focused) R.drawable.bg_tense_triplet_cell_focus else R.drawable.bg_tense_triplet_cell
        holder.pCell.setBackgroundResource(bg)
        holder.paCell.setBackgroundResource(bg)
        holder.fCell.setBackgroundResource(bg)
        if (displayMode != DisplayMode.TEST) {
            holder.pEngRow.visibility = View.VISIBLE
            holder.paEngRow.visibility = View.VISIBLE
            holder.fEngRow.visibility = View.VISIBLE
            holder.pEng.visibility = View.VISIBLE
            holder.paEng.visibility = View.VISIBLE
            holder.fEng.visibility = View.VISIBLE
        }
    }

    private fun clearTextViewCompoundDrawables(holder: TripletViewHolder) {
        listOf(holder.pEng, holder.pBn, holder.paEng, holder.paBn, holder.fEng, holder.fBn).forEach {
            it.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
        }
    }

    private fun hideAllMarks(holder: TripletViewHolder) {
        listOf(
            holder.pEngMark, holder.pBnMark,
            holder.paEngMark, holder.paBnMark,
            holder.fEngMark, holder.fBnMark
        ).forEach {
            it.visibility = View.INVISIBLE
            it.clearColorFilter()
            it.setImageDrawable(null)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<TenseTripletRow>) {
        items = newItems
        clearMarksAndSpokenState()
    }

    /** Clears tick/cross and TEST "spoken" replacement text (e.g. tab switch or fresh lesson). */
    fun clearMarksAndSpokenState() {
        rowResultMarks.clear()
        rowSpokenText.clear()
        notifyDataSetChanged()
    }

    fun markCellResults(position: Int, present: Boolean?, past: Boolean?, future: Boolean?) {
        if (position !in items.indices) return
        rowResultMarks[position] = Triple(present, past, future)
        notifyItemChanged(position)
    }

    fun setColumnVisibility(showPresent: Boolean, showPast: Boolean, showFuture: Boolean) {
        showPresentColumn = showPresent
        showPastColumn = showPast
        showFutureColumn = showFuture
        notifyDataSetChanged()
    }

    fun setCurrentIndex(index: Int) {
        if (items.isEmpty()) return
        val newIndex = index.coerceIn(0, items.lastIndex)
        if (newIndex == currentIndex) return
        val old = currentIndex
        currentIndex = newIndex
        notifyItemChanged(old)
        notifyItemChanged(currentIndex)
    }

    fun setSpokenText(position: Int, presentSpoken: String?, pastSpoken: String?, futureSpoken: String?) {
        if (position !in items.indices) return
        rowSpokenText[position] = Triple(presentSpoken, pastSpoken, futureSpoken)
        notifyItemChanged(position)
    }

    private enum class Tense { PRESENT, PAST, FUTURE }

    /** Subject gets one color; verbs get tense-specific colors; helping verb "will" gets its own color. */
    private fun colorEnglish(text: String, tense: Tense): SpannableString {
        val s = SpannableString(text)
        val lower = text.lowercase()
        val subjectColor = Color.parseColor("#1565C0")
        val presentVerbColor = Color.parseColor("#2E7D32")
        val pastVerbColor = Color.parseColor("#E65100")
        val futureVerbColor = Color.parseColor("#6A1B9A")
        val helpingVerbColor = Color.parseColor("#C62828")

        val tokens = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return s

        // Color first token (subject).
        colorToken(s, text, tokens[0], subjectColor)

        when (tense) {
            Tense.PRESENT -> if (tokens.size >= 2) colorToken(s, text, tokens[1], presentVerbColor)
            Tense.PAST -> if (tokens.size >= 2) colorToken(s, text, tokens[1], pastVerbColor)
            Tense.FUTURE -> {
                if (lower.contains(" will ")) {
                    colorToken(s, text, "will", helpingVerbColor)
                    if (tokens.size >= 3) colorToken(s, text, tokens[2], futureVerbColor)
                } else if (tokens.size >= 2) {
                    colorToken(s, text, tokens[1], futureVerbColor)
                }
            }
        }
        return s
    }

    private fun colorToken(s: SpannableString, fullText: String, token: String, color: Int) {
        val start = fullText.indexOf(token, ignoreCase = true)
        if (start >= 0) {
            val end = start + token.length
            s.setSpan(ForegroundColorSpan(color), start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /** Tick/cross as overlay — does not affect TextView line width / wrapping. */
    private fun applyResultMarkToImage(markView: ImageView, match: Boolean?) {
        when (match) {
            true -> {
                markView.visibility = View.VISIBLE
                markView.clearColorFilter()
                val d = AppCompatResources.getDrawable(markView.context, R.drawable.ic_check_mark_plain)?.mutate()
                markView.setImageDrawable(d)
            }
            false -> {
                markView.visibility = View.VISIBLE
                val d = AppCompatResources.getDrawable(markView.context, R.drawable.ic_clear)?.mutate()
                if (d != null) {
                    DrawableCompat.setTint(d, Color.parseColor("#C62828"))
                }
                markView.setImageDrawable(d)
            }
            null -> {
                markView.visibility = View.INVISIBLE
                markView.clearColorFilter()
                markView.setImageDrawable(null)
            }
        }
    }
}
