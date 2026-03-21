package com.alphacephei.vosk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PrepositionBlocksAdapter(
    private var rows: List<PrepositionBlockRow>
) : RecyclerView.Adapter<PrepositionBlocksAdapter.VH>() {

    /** Row index with red border while that block is being spoken ([RecyclerView.NO_POSITION] = none). */
    private var highlightedPosition: Int = RecyclerView.NO_POSITION

    fun submitRows(newRows: List<PrepositionBlockRow>) {
        rows = newRows
        highlightedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    /** Same red boundary as other lessons ([R.drawable.bg_threecol_row_active]). */
    fun setHighlightedPosition(position: Int) {
        if (position == highlightedPosition) return
        val old = highlightedPosition
        highlightedPosition = position
        if (old != RecyclerView.NO_POSITION) notifyItemChanged(old)
        if (highlightedPosition != RecyclerView.NO_POSITION) notifyItemChanged(highlightedPosition)
    }

    fun clearHighlight() {
        if (highlightedPosition == RecyclerView.NO_POSITION) return
        val old = highlightedPosition
        highlightedPosition = RecyclerView.NO_POSITION
        notifyItemChanged(old)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_preposition_block, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(rows[position], position == highlightedPosition)
    }

    override fun getItemCount(): Int = rows.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headMeaning: TextView = itemView.findViewById(R.id.preposition_head_meaning)
        private val example1: TextView = itemView.findViewById(R.id.preposition_example_1)
        private val example2: TextView = itemView.findViewById(R.id.preposition_example_2)

        fun bind(row: PrepositionBlockRow, isHighlighted: Boolean) {
            headMeaning.text = "${row.preposition} - ${row.meaning}"
            example1.text = row.example1
            example2.text = row.example2
            itemView.setBackgroundResource(
                if (isHighlighted) R.drawable.bg_threecol_row_active else R.drawable.bg_conv_bubble_left
            )
        }
    }
}
