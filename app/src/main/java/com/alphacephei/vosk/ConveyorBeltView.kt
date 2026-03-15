package com.alphacephei.vosk

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Reusable conveyor-belt UI matching the HTML "Centered Ribbon Conveyor" behavior:
 *
 * - Viewport: fixed height = visibleSlots * itemHeight, overflow hidden.
 * - Tray: [visibleSlots + 1] items; first item is above the viewport (like CSS top: -1 * rowHeight).
 * - On move: entire tray moves down by one row (like CSS transition + translateY(rowHeight));
 *   all items and their text move down together; new item enters from top, bottom item leaves.
 * - After animation (400ms, cubic-bezier 0.45,0.05,0.55,0.95): advance circular list and reset
 *   so the new first item is hidden above again (like HTML: remove last child, prepend new, remove transform).
 *
 * [centerSlotIndex] = 0-based center row (e.g. 2 = 3rd of 5, 3 = 4th of 7), highlighted.
 */
class ConveyorBeltView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val recyclerView: RecyclerView
    private val itemHeightDp = 40
    private var itemHeightPx: Int = 0

    var visibleSlots: Int = 7
        set(value) {
            field = value.coerceAtLeast(1)
        }

    /** 0-based index of the "center" visible row (e.g. 2 = 3rd row for 5-slot, 3 = 4th for 7-slot). */
    var centerSlotIndex: Int = 3
        set(value) {
            field = value.coerceIn(0, visibleSlots - 1)
        }

    private var adapter: ConveyorAdapter? = null
    private var currentAnimator: ValueAnimator? = null

    init {
        itemHeightPx = (itemHeightDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            setHasFixedSize(true)
            clipToPadding = false
            clipChildren = true
            overScrollMode = OVER_SCROLL_NEVER
            setLayerType(LAYER_TYPE_HARDWARE, null) // smooth animation
        }
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        clipChildren = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        if (h > 0) {
            recyclerView.translationY = -itemHeightPx.toFloat()
            recyclerView.layoutParams = (recyclerView.layoutParams as? LayoutParams)?.apply {
                height = h + itemHeightPx
                topMargin = 0
            } ?: LayoutParams(LayoutParams.MATCH_PARENT, h + itemHeightPx)
            recyclerView.requestLayout()
            adapter?.let { recyclerView.post { it.scrollToInitialState(recyclerView) } }
        }
    }

    /** Total number of items in the conveyor = visibleSlots + 1 (one hidden above). */
    private fun totalSlots(): Int = visibleSlots + 1

    /** Set the data list and reset to first item at center. */
    fun setData(items: List<String>) {
        val a = ConveyorAdapter(items, visibleSlots, centerSlotIndex, itemHeightPx)
        adapter = a
        recyclerView.adapter = a
        recyclerView.post {
            recyclerView.requestLayout()
            a.scrollToInitialState(recyclerView)
        }
    }

    /** Same as HTML: tray moves down (translateY), 0.4s, cubic-bezier(0.45,0.05,0.55,0.95); then remove bottom, prepend new, reset. */
    private val conveyorDurationMs = 400
    private val conveyorInterpolator = PathInterpolator(0.45f, 0.05f, 0.55f, 0.95f)

    fun moveToNext(onComplete: (() -> Unit)? = null) {
        val a = adapter ?: return
        if (a.itemCount == 0) {
            onComplete?.invoke()
            return
        }
        currentAnimator?.cancel()
        val scrollDy = itemHeightPx
        var lastValue = 0
        currentAnimator = ValueAnimator.ofInt(0, scrollDy).apply {
            duration = conveyorDurationMs.toLong()
            interpolator = conveyorInterpolator
            addUpdateListener { anim ->
                val value = anim.animatedValue as Int
                recyclerView.scrollBy(0, -(value - lastValue))
                lastValue = value
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentAnimator = null
                    a.advanceCurrent()
                    a.scrollToInitialState(recyclerView)
                    onComplete?.invoke()
                }
            })
        }
        currentAnimator?.start()
    }

    /** Current selected index in the data list (the one at center). */
    fun getCurrentIndex(): Int = adapter?.currentIndex ?: 0

    /** Adapter: holds visibleSlots+1 items; position 0 hidden above, positions 1..visibleSlots visible. Center row = centerSlotIndex+1. */
    private class ConveyorAdapter(
        private val data: List<String>,
        private val visibleSlots: Int,
        private val centerSlotIndex: Int,
        private val itemHeightPx: Int
    ) : RecyclerView.Adapter<ConveyorAdapter.Holder>() {

        var currentIndex: Int = 0
            private set

        private fun n() = data.size
        private fun totalSlots() = visibleSlots + 1

        /** Position 0 = hidden above (shows next item so it "enters from top"); positions 1..visibleSlots = current window; center = centerSlotIndex+1. */
        private fun dataIndex(adapterPosition: Int): Int {
            if (n() == 0) return 0
            fun mod(x: Int): Int {
                val s = n()
                return ((x % s) + s) % s
            }
            val base = currentIndex - centerSlotIndex
            return when {
                adapterPosition == 0 -> mod(base + 1)  // next item, enters from top when we scroll
                else -> mod(base + adapterPosition - 1) // positions 1..visibleSlots: top to bottom
            }
        }

        private fun isCenterPosition(adapterPosition: Int): Boolean =
            adapterPosition == centerSlotIndex + 1

        override fun getItemCount(): Int = totalSlots()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.layout_item_ribbon_button, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val text = data.getOrNull(dataIndex(position)).orEmpty()
            holder.button.text = text
            val isCenter = isCenterPosition(position)
            holder.button.setBackgroundResource(if (isCenter) R.drawable.bg_ribbon_btn_center else R.drawable.bg_ribbon_btn)
            holder.button.setTextColor(if (isCenter) 0xFFFFFFFF.toInt() else 0xFF212529.toInt())
        }

        fun advanceCurrent() {
            if (n() == 0) return
            currentIndex = (currentIndex + 1) % n().let { if (it < 0) it + n() else it }
            notifyDataSetChanged()
        }

        /** Scroll so position 0 is just above the top (hidden). */
        fun scrollToInitialState(rv: RecyclerView) {
            (rv.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, -itemHeightPx)
        }

        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val button: TextView = itemView.findViewById(R.id.ribbon_btn_text)
        }
    }
}
