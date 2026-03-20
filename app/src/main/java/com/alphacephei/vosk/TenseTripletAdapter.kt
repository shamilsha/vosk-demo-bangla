package com.alphacephei.vosk

import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.appcompat.content.res.AppCompatResources
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
        val pEng: TextView = view.findViewById(R.id.triplet_present_english)
        val pBn: TextView = view.findViewById(R.id.triplet_present_bengali)
        val pHint: TextView = view.findViewById(R.id.triplet_present_hint)

        val paEng: TextView = view.findViewById(R.id.triplet_past_english)
        val paBn: TextView = view.findViewById(R.id.triplet_past_bengali)
        val paHint: TextView = view.findViewById(R.id.triplet_past_hint)

        val fEng: TextView = view.findViewById(R.id.triplet_future_english)
        val fBn: TextView = view.findViewById(R.id.triplet_future_bengali)
        val fHint: TextView = view.findViewById(R.id.triplet_future_hint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripletViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_tense_triplet, parent, false)
        return TripletViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripletViewHolder, position: Int) {
        val row = items[position]
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
                // TEST mode: show Bengali prompt; when matched, replace that column with spoken English only.
                holder.pEng.visibility = View.GONE
                holder.paEng.visibility = View.GONE
                holder.fEng.visibility = View.GONE
                holder.pBn.visibility = View.VISIBLE
                holder.paBn.visibility = View.VISIBLE
                holder.fBn.visibility = View.VISIBLE
                holder.pHint.visibility = View.GONE
                holder.paHint.visibility = View.GONE
                holder.fHint.visibility = View.GONE
                if (showPresentColumn && !spoken.first.isNullOrBlank()) holder.pBn.text = spoken.first
                if (showPastColumn && !spoken.second.isNullOrBlank()) holder.paBn.text = spoken.second
                if (showFutureColumn && !spoken.third.isNullOrBlank()) holder.fBn.text = spoken.third
            }
        }
        val marks = rowResultMarks[position] ?: Triple(null, null, null)
        if (displayMode == DisplayMode.TEST) {
            applyResultMark(holder.pBn, if (showPresentColumn) marks.first else null)
            applyResultMark(holder.paBn, if (showPastColumn) marks.second else null)
            applyResultMark(holder.fBn, if (showFutureColumn) marks.third else null)
            applyResultMark(holder.pEng, null)
            applyResultMark(holder.paEng, null)
            applyResultMark(holder.fEng, null)
        } else {
            applyResultMark(holder.pEng, if (showPresentColumn) marks.first else null)
            applyResultMark(holder.paEng, if (showPastColumn) marks.second else null)
            applyResultMark(holder.fEng, if (showFutureColumn) marks.third else null)
            applyResultMark(holder.pBn, null)
            applyResultMark(holder.paBn, null)
            applyResultMark(holder.fBn, null)
        }
        val focused = position == currentIndex
        val bg = if (focused) R.drawable.bg_tense_triplet_cell_focus else R.drawable.bg_tense_triplet_cell
        holder.pCell.setBackgroundResource(bg)
        holder.paCell.setBackgroundResource(bg)
        holder.fCell.setBackgroundResource(bg)
        if (displayMode != DisplayMode.TEST) {
            holder.pEng.visibility = View.VISIBLE
            holder.paEng.visibility = View.VISIBLE
            holder.fEng.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<TenseTripletRow>) {
        items = newItems
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

    private fun applyResultMark(view: TextView, match: Boolean?) {
        view.setCompoundDrawablePadding(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, view.resources.displayMetrics).toInt()
        )
        val icon = when (match) {
            true -> AppCompatResources.getDrawable(view.context, R.drawable.ic_check_mark_plain)?.mutate()
            false -> AppCompatResources.getDrawable(view.context, R.drawable.ic_clear)?.mutate()?.also {
                DrawableCompat.setTint(it, Color.parseColor("#C62828"))
            }
            null -> null
        }
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, icon, null)
    }
}
