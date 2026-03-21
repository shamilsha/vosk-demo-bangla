package com.alphacephei.vosk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** One section title + progressive sentence rows for extend-sentence lessons. */
sealed class ExtendSentenceListItem {
    data class SectionHeader(val title: String) : ExtendSentenceListItem()
    data class RowBlock(val row: ExtendSentenceRow) : ExtendSentenceListItem()
}

class ExtendSentenceAdapter(
    private var items: List<ExtendSentenceListItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_BLOCK = 1
    }

    /** Adapter position of the row block currently being spoken ([RecyclerView.NO_POSITION] = none). */
    private var highlightedAdapterPosition: Int = RecyclerView.NO_POSITION

    fun submitList(newItems: List<ExtendSentenceListItem>) {
        items = newItems
        highlightedAdapterPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    /** Red border while TTS reads that sentence block (same drawable as 3-col active row). */
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
                position == highlightedAdapterPosition
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
        private val english: TextView = itemView.findViewById(R.id.extend_block_english)
        private val bengali: TextView = itemView.findViewById(R.id.extend_block_bengali)
        private val pronunciation: TextView = itemView.findViewById(R.id.extend_block_pronunciation)
        fun bind(item: ExtendSentenceListItem.RowBlock, isHighlighted: Boolean) {
            val r = item.row
            english.text = ExtendSentenceText.englishToSpanned(r.english)
            bengali.text = r.bengali
            pronunciation.text = r.hint
            itemView.setBackgroundResource(
                if (isHighlighted) R.drawable.bg_threecol_row_active else R.drawable.bg_conv_bubble_left
            )
        }
    }
}

/** Build RecyclerView items and adapter positions where each group header starts. */
fun buildExtendSentenceListItems(groups: List<List<ExtendSentenceRow>>): Pair<List<ExtendSentenceListItem>, IntArray> {
    val items = mutableListOf<ExtendSentenceListItem>()
    val headerPos = IntArray(groups.size)
    val totalGroups = groups.size.coerceAtLeast(1)
    for ((gi, g) in groups.withIndex()) {
        headerPos[gi] = items.size
        items.add(ExtendSentenceListItem.SectionHeader("Part ${gi + 1} of $totalGroups (${g.size} lines)"))
        for (row in g) {
            items.add(ExtendSentenceListItem.RowBlock(row))
        }
    }
    return items to headerPos
}
