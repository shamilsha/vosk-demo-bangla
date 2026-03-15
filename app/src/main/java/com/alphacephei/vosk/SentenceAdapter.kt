package com.alphacephei.vosk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** Adapter for the sentence list: text + delete; supports reorder via ItemTouchHelper. */
class SentenceAdapter(
    private val list: MutableList<Sentence>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<SentenceAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.item_sentence_text)
        val deleteButton: Button = itemView.findViewById(R.id.item_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_item_sentence, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = list[position].text
        holder.deleteButton.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(pos)
        }
    }

    override fun getItemCount(): Int = list.size
}
