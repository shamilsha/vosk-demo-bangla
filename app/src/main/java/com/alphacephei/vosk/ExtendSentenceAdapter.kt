package com.alphacephei.vosk

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/** One section title + progressive sentence rows for extend-sentence lessons. */
sealed class ExtendSentenceListItem {
    data class SectionHeader(val title: String) : ExtendSentenceListItem()
    data class RowBlock(val row: ExtendSentenceRow) : ExtendSentenceListItem()
}

enum class ExtendSentenceDisplayMode {
    LEARNING,
    PRACTICE,
    TEST
}

/** Items + header start indices + per-item (groupIndex, rowIndex) for row blocks (null = header). */
data class ExtendSentenceListBuild(
    val items: List<ExtendSentenceListItem>,
    val headerPositions: IntArray,
    val rowMeta: List<Pair<Int, Int>?>
)

class ExtendSentenceAdapter(
    private var items: List<ExtendSentenceListItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_BLOCK = 1
    }

    private var rowMeta: List<Pair<Int, Int>?> = emptyList()

    /** Adapter position of the row block currently being spoken / focused. */
    private var highlightedAdapterPosition: Int = RecyclerView.NO_POSITION

    private var displayMode: ExtendSentenceDisplayMode = ExtendSentenceDisplayMode.LEARNING

    /** TEST: which part uses the test layout (matches [extendSentenceCurrentGroupIndex]). */
    private var testActiveGroupIndex: Int = 0

    /** TEST: which row index in that part the user must speak next (1 = second sentence). */
    private var testListeningRowIndexInGroup: Int = 1

    /** PRACTICE/TEST: per-row spoken text and ✓/✗. */
    private val sessionSpokenByPosition: MutableMap<Int, String> = mutableMapOf()
    private val sessionResultByPosition: MutableMap<Int, Boolean> = mutableMapOf()

    fun submitList(build: ExtendSentenceListBuild) {
        submitList(build.items, build.rowMeta)
    }

    fun submitList(newItems: List<ExtendSentenceListItem>, meta: List<Pair<Int, Int>?>) {
        items = newItems
        rowMeta = meta
        highlightedAdapterPosition = RecyclerView.NO_POSITION
        sessionSpokenByPosition.clear()
        sessionResultByPosition.clear()
        notifyDataSetChanged()
    }

    fun setDisplayMode(mode: ExtendSentenceDisplayMode) {
        if (displayMode == mode) return
        displayMode = mode
        if (mode == ExtendSentenceDisplayMode.LEARNING) {
            sessionSpokenByPosition.clear()
            sessionResultByPosition.clear()
        }
        notifyDataSetChanged()
    }

    fun setTestActiveGroupIndex(gi: Int) {
        if (testActiveGroupIndex == gi) return
        testActiveGroupIndex = gi
        if (displayMode == ExtendSentenceDisplayMode.TEST) notifyDataSetChanged()
    }

    fun setTestListeningRowInGroup(rowIndex: Int) {
        if (testListeningRowIndexInGroup == rowIndex) return
        testListeningRowIndexInGroup = rowIndex
        if (displayMode == ExtendSentenceDisplayMode.TEST) notifyDataSetChanged()
    }

    fun clearSessionFeedback() {
        sessionSpokenByPosition.clear()
        sessionResultByPosition.clear()
        notifyDataSetChanged()
    }

    fun setSessionSpokenAt(adapterPosition: Int, spoken: String) {
        sessionSpokenByPosition[adapterPosition] = spoken
        notifyItemChanged(adapterPosition)
    }

    fun setSessionResultAt(adapterPosition: Int, correct: Boolean) {
        sessionResultByPosition[adapterPosition] = correct
        notifyItemChanged(adapterPosition)
    }

    /** @deprecated Use [setSessionSpokenAt] */
    fun setPracticeSpokenAt(adapterPosition: Int, spoken: String) = setSessionSpokenAt(adapterPosition, spoken)

    /** @deprecated Use [setSessionResultAt] */
    fun setPracticeResultAt(adapterPosition: Int, correct: Boolean) = setSessionResultAt(adapterPosition, correct)

    /** @deprecated Use [setDisplayMode] */
    fun setPracticeEnglishOnly(englishOnly: Boolean) {
        setDisplayMode(if (englishOnly) ExtendSentenceDisplayMode.PRACTICE else ExtendSentenceDisplayMode.LEARNING)
    }

    /** @deprecated Use [clearSessionFeedback] */
    fun clearPracticeSession() = clearSessionFeedback()

    fun setHighlightedAdapterPosition(position: Int) {
        if (position == highlightedAdapterPosition) return
        val old = highlightedAdapterPosition
        highlightedAdapterPosition = position
        if (old != RecyclerView.NO_POSITION) notifyItemChanged(old)
        if (highlightedAdapterPosition != RecyclerView.NO_POSITION) notifyItemChanged(highlightedAdapterPosition)
    }

    fun clearBlockHighlight() {
        if (highlightedAdapterPosition == RecyclerView.NO_POSITION) return
        val old = highlightedAdapterPosition
        highlightedAdapterPosition = RecyclerView.NO_POSITION
        notifyItemChanged(old)
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ExtendSentenceListItem.SectionHeader -> TYPE_HEADER
        is ExtendSentenceListItem.RowBlock -> TYPE_BLOCK
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val v = inflater.inflate(R.layout.item_extend_sentence_header, parent, false)
                HeaderVH(v)
            }
            else -> {
                val v = inflater.inflate(R.layout.item_extend_sentence_block, parent, false)
                BlockVH(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ExtendSentenceListItem.SectionHeader -> (holder as HeaderVH).bind(item)
            is ExtendSentenceListItem.RowBlock -> (holder as BlockVH).bind(
                item,
                position,
                rowMeta.getOrNull(position),
                position == highlightedAdapterPosition,
                displayMode,
                testActiveGroupIndex,
                testListeningRowIndexInGroup,
                sessionSpokenByPosition[position],
                sessionResultByPosition[position]
            )
        }
    }

    override fun getItemCount(): Int = items.size

    private class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.extend_sentence_header_text)
        fun bind(item: ExtendSentenceListItem.SectionHeader) {
            text.text = item.title
        }
    }

    private class BlockVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val content: LinearLayout = itemView.findViewById(R.id.extend_block_content)
        private val english: TextView = itemView.findViewById(R.id.extend_block_english)
        private val bengali: TextView = itemView.findViewById(R.id.extend_block_bengali)
        private val pronunciation: TextView = itemView.findViewById(R.id.extend_block_pronunciation)
        private val resultBadge: TextView = itemView.findViewById(R.id.extend_block_practice_result_badge)

        fun bind(
            item: ExtendSentenceListItem.RowBlock,
            adapterPosition: Int,
            meta: Pair<Int, Int>?,
            isHighlighted: Boolean,
            displayMode: ExtendSentenceDisplayMode,
            testActiveGroupIndex: Int,
            testListeningRowIndexInGroup: Int,
            sessionSpoken: String?,
            sessionResult: Boolean?
        ) {
            val r = item.row
            val ctx = itemView.context
            val primaryColor = ContextCompat.getColor(ctx, R.color.text_primary)
            val placeholderColor = ContextCompat.getColor(ctx, R.color.text_secondary)

            fun applyBadge(res: Boolean?) {
                when (res) {
                    null -> resultBadge.visibility = View.GONE
                    true -> {
                        resultBadge.visibility = View.VISIBLE
                        resultBadge.text = "✓"
                        resultBadge.setTextColor(ContextCompat.getColor(ctx, R.color.control_success))
                    }
                    false -> {
                        resultBadge.visibility = View.VISIBLE
                        resultBadge.text = "✗"
                        resultBadge.setTextColor(ContextCompat.getColor(ctx, R.color.control_danger))
                    }
                }
            }

            when (displayMode) {
                ExtendSentenceDisplayMode.LEARNING -> {
                    english.text = ExtendSentenceText.englishToSpanned(r.english)
                    english.setTextColor(primaryColor)
                    english.typeface = Typeface.DEFAULT_BOLD
                    bengali.visibility = View.VISIBLE
                    bengali.text = r.bengali
                    pronunciation.visibility = if (r.hint.isNotBlank()) View.VISIBLE else View.GONE
                    pronunciation.text = r.hint
                    resultBadge.visibility = View.GONE
                }
                ExtendSentenceDisplayMode.PRACTICE -> {
                    english.text = ExtendSentenceText.englishToSpanned(r.english)
                    english.setTextColor(primaryColor)
                    english.typeface = Typeface.DEFAULT_BOLD
                    bengali.visibility = View.GONE
                    if (!sessionSpoken.isNullOrBlank()) {
                        pronunciation.visibility = View.VISIBLE
                        pronunciation.text = ctx.getString(R.string.extend_sentence_you_said, sessionSpoken)
                    } else {
                        pronunciation.visibility = View.GONE
                    }
                    applyBadge(sessionResult)
                }
                ExtendSentenceDisplayMode.TEST -> {
                    val gi = meta?.first ?: 0
                    val ri = meta?.second ?: 0
                    // Every part uses the same pattern: line 1 = English only; rest = placeholders / what user said.
                    // Never show Bengali or pronunciation hints for non-active parts (same as active part).
                    if (ri == 0) {
                        english.text = ExtendSentenceText.englishToSpanned(r.english)
                        english.setTextColor(primaryColor)
                        english.typeface = Typeface.DEFAULT_BOLD
                        bengali.visibility = View.GONE
                        pronunciation.visibility = View.GONE
                        resultBadge.visibility = View.GONE
                    } else {
                        bengali.visibility = View.GONE
                        pronunciation.visibility = View.GONE
                        when {
                            !sessionSpoken.isNullOrBlank() -> {
                                english.text = sessionSpoken
                                english.setTextColor(primaryColor)
                                english.typeface = Typeface.DEFAULT
                                applyBadge(sessionResult)
                            }
                            gi != testActiveGroupIndex -> {
                                // Not the part currently being tested: no answers, ellipsis until user works that part
                                english.text = "…"
                                english.setTextColor(placeholderColor)
                                english.typeface = Typeface.DEFAULT
                                resultBadge.visibility = View.GONE
                            }
                            ri > testListeningRowIndexInGroup -> {
                                english.text = "…"
                                english.setTextColor(placeholderColor)
                                english.typeface = Typeface.DEFAULT
                                resultBadge.visibility = View.GONE
                            }
                            else -> {
                                english.text = ctx.getString(R.string.extend_sentence_test_placeholder)
                                english.setTextColor(placeholderColor)
                                english.typeface = Typeface.DEFAULT
                                resultBadge.visibility = View.GONE
                            }
                        }
                    }
                }
            }

            content.setBackgroundResource(
                if (isHighlighted) R.drawable.bg_threecol_row_active else R.drawable.bg_conv_bubble_left
            )
        }
    }
}

fun buildExtendSentenceListItems(groups: List<List<ExtendSentenceRow>>): ExtendSentenceListBuild {
    val items = mutableListOf<ExtendSentenceListItem>()
    val headerPos = IntArray(groups.size)
    val meta = mutableListOf<Pair<Int, Int>?>()
    val totalGroups = groups.size.coerceAtLeast(1)
    for ((gi, g) in groups.withIndex()) {
        headerPos[gi] = items.size
        items.add(ExtendSentenceListItem.SectionHeader("Part ${gi + 1} of $totalGroups (${g.size} lines)"))
        meta.add(null)
        for (ri in g.indices) {
            items.add(ExtendSentenceListItem.RowBlock(g[ri]))
            meta.add(gi to ri)
        }
    }
    return ExtendSentenceListBuild(items, headerPos, meta)
}
