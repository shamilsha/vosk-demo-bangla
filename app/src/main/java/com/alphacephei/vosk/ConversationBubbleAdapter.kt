package com.alphacephei.vosk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for conversation bubbles: left (Person A) and right (Person B).
 * Learning: show Bengali, English, Pronunciation.
 * Practice: show Bengali; after user speaks, show what they said + badge.
 * Test (role-play): initially all bubbles empty; when app or user finishes speaking, only English is shown in that bubble.
 */
class ConversationBubbleAdapter(
    private var items: List<ConversationBubbleRow>
) : RecyclerView.Adapter<ConversationBubbleAdapter.BubbleViewHolder>() {

    companion object {
        private const val VIEW_TYPE_LEFT = 0
        private const val VIEW_TYPE_RIGHT = 1
    }

    var learningMode: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var testMode: Boolean = false
        set(value) {
            field = value
            if (value) revealed = MutableList(items.size) { false }
            notifyDataSetChanged()
        }

    private var answered: MutableList<Boolean> = MutableList(items.size) { false }
    private var correct: MutableList<Boolean> = MutableList(items.size) { false }
    private var spoken: MutableList<String> = MutableList(items.size) { "" }
    /** In Test mode: indices where app has finished speaking (show English in that bubble). */
    private var revealed: MutableList<Boolean> = MutableList(items.size) { false }
    private var currentIndex: Int = 0

    abstract class BubbleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bengali: TextView = view.findViewById(R.id.conv_bubble_bengali)
        val english: TextView = view.findViewById(R.id.conv_bubble_english)
        val pronunciation: TextView = view.findViewById(R.id.conv_bubble_pronunciation)
        val cardView: View = view.findViewById(R.id.conv_bubble_card)
        val cornerBadge: TextView = view.findViewById(R.id.conv_bubble_corner_badge)
    }

    class LeftHolder(view: View) : BubbleViewHolder(view)
    class RightHolder(view: View) : BubbleViewHolder(view)

    override fun getItemViewType(position: Int): Int {
        return if (items.getOrNull(position)?.speaker == "A") VIEW_TYPE_LEFT else VIEW_TYPE_RIGHT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BubbleViewHolder {
        return if (viewType == VIEW_TYPE_LEFT) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.layout_item_conv_bubble_left, parent, false)
            LeftHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.layout_item_conv_bubble_right, parent, false)
            RightHolder(v)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: BubbleViewHolder, position: Int) {
        val row = items.getOrNull(position) ?: return
        val ctx = holder.itemView.context
        val successColor = ctx.getColor(R.color.control_success)
        val dangerColor = ctx.getColor(R.color.control_danger)

        holder.pronunciation.visibility = View.GONE

        if (learningMode) {
            holder.bengali.text = row.bengali
            holder.bengali.visibility = View.VISIBLE
            holder.english.text = MatchNormalizer.textForSpeakAndDisplay(row.english)
            holder.english.visibility = View.VISIBLE
            holder.pronunciation.text = row.pronunciation
            holder.pronunciation.visibility = if (row.pronunciation.isNotBlank()) View.VISIBLE else View.GONE
        } else if (testMode) {
            // Test role-play: initially empty; show only English when app or user has finished that line
            holder.bengali.visibility = View.GONE
            holder.bengali.text = ""
            val appRevealed = position in revealed.indices && revealed[position]
            val userAnswered = position in answered.indices && answered[position]
            when {
                appRevealed -> {
                    holder.english.text = MatchNormalizer.textForSpeakAndDisplay(row.english)
                    holder.english.visibility = View.VISIBLE
                    holder.english.setTextColor(ctx.getColor(R.color.text_primary))
                }
                userAnswered -> {
                    val said = spoken.getOrNull(position)?.takeIf { it.isNotBlank() } ?: "—"
                    holder.english.text = MatchNormalizer.sanitizeSpokenTextForDisplay(said)
                    holder.english.visibility = View.VISIBLE
                    holder.english.setTextColor(if (position in correct.indices && correct[position]) successColor else dangerColor)
                }
                else -> {
                    holder.english.text = ""
                    holder.english.visibility = View.GONE
                }
            }
        } else {
            // Practice: show Bengali; after answer show what user said
            holder.bengali.text = row.bengali
            holder.bengali.visibility = View.VISIBLE
            if (position in answered.indices && answered[position]) {
                val said = spoken.getOrNull(position)?.takeIf { it.isNotBlank() } ?: "—"
                holder.english.visibility = View.VISIBLE
                holder.english.text = MatchNormalizer.sanitizeSpokenTextForDisplay(said)
                holder.english.setTextColor(if (position in correct.indices && correct[position]) successColor else dangerColor)
            } else {
                holder.english.visibility = View.GONE
            }
        }

        // Corner badge overlay (no layout space): top-right for left, top-left for right; center at bubble corner
        if (position in answered.indices && answered[position]) {
            holder.cornerBadge.visibility = View.VISIBLE
            holder.cornerBadge.text = if (position in correct.indices && correct[position]) "✓" else "✗"
            holder.cornerBadge.setTextColor(if (position in correct.indices && correct[position]) successColor else dangerColor)
        } else {
            holder.cornerBadge.visibility = View.GONE
            holder.cornerBadge.text = ""
        }

        // Red border for current bubble; normal border otherwise (fixed layout space)
        val isLeft = holder is LeftHolder
        if (position == currentIndex) {
            holder.cardView.setBackgroundResource(if (isLeft) R.drawable.bg_conv_bubble_left_current else R.drawable.bg_conv_bubble_right_current)
            holder.itemView.alpha = 1f
        } else {
            holder.cardView.setBackgroundResource(if (isLeft) R.drawable.bg_conv_bubble_left else R.drawable.bg_conv_bubble_right)
            holder.itemView.alpha = 0.92f
        }
    }

    fun updateData(newItems: List<ConversationBubbleRow>) {
        items = newItems
        answered = MutableList(items.size) { false }
        correct = MutableList(items.size) { false }
        spoken = MutableList(items.size) { "" }
        revealed = MutableList(items.size) { false }
        notifyDataSetChanged()
    }

    /** Test mode: mark that app finished speaking this row; show English in bubble. */
    fun revealEnglishForAppSpoke(index: Int) {
        if (index !in items.indices || !testMode) return
        if (index >= revealed.size) revealed = MutableList(items.size) { i -> revealed.getOrNull(i) ?: false }
        revealed[index] = true
        notifyItemChanged(index)
    }

    fun applyStatsFrom(stats: List<IntArray>) {
        if (items.isEmpty()) return
        val size = items.size
        if (answered.size < size) answered = MutableList(size) { false }
        if (correct.size < size) correct = MutableList(size) { false }
        if (spoken.size < size) spoken = MutableList(size) { i -> spoken.getOrNull(i) ?: "" }
        for (i in 0 until size) {
            val r = stats.getOrNull(i) ?: continue
            val a = r.getOrNull(0) ?: 0
            val b = r.getOrNull(1) ?: 0
            answered[i] = (a != 0 || b != 0)
            correct[i] = (a != 0 || b != 0)
        }
        notifyDataSetChanged()
    }

    fun setSpokenText(index: Int, said: String) {
        if (index !in items.indices) return
        if (index >= spoken.size) spoken = MutableList(items.size) { i -> spoken.getOrNull(i) ?: "" }
        spoken[index] = said
        notifyItemChanged(index)
    }

    fun markResult(index: Int, isCorrect: Boolean) {
        if (index !in items.indices) return
        if (index >= answered.size) {
            answered = MutableList(items.size) { i -> answered.getOrNull(i) ?: false }
            correct = MutableList(items.size) { i -> correct.getOrNull(i) ?: false }
        }
        answered[index] = true
        correct[index] = isCorrect
        notifyItemChanged(index)
    }

    fun setCurrentIndex(index: Int) {
        if (index !in items.indices) return
        val oldIndex = currentIndex
        currentIndex = index
        // Notify only the two positions that changed (red border) so RecyclerView scroll is not reset
        if (oldIndex in items.indices) notifyItemChanged(oldIndex)
        if (index != oldIndex && index in items.indices) notifyItemChanged(index)
    }

    fun getStats(): Triple<Int, Int, Int> {
        val total = items.size
        val correctCount = correct.count { it }
        val testedCount = answered.count { it }
        return Triple(correctCount, testedCount, total)
    }
}
