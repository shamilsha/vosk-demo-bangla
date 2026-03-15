package com.alphacephei.vosk

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * A ViewGroup that lays out children left-to-right and wraps to the next line when
 * they don't fit in the available width. Uses the parent's width constraint from
 * MeasureSpec so wrapping works without horizontal scroll.
 */
class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val availableWidth = when (widthMode) {
            MeasureSpec.EXACTLY, MeasureSpec.AT_MOST -> widthSize - paddingLeft - paddingRight
            else -> Int.MAX_VALUE
        }

        var x = paddingLeft
        var y = paddingTop
        var rowHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue

            val lp = child.layoutParams as? MarginLayoutParams ?: MarginLayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
            val childWidthSpec = getChildMeasureSpec(
                widthMeasureSpec,
                paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin,
                lp.width
            )
            val childHeightSpec = getChildMeasureSpec(
                heightMeasureSpec,
                paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin,
                lp.height
            )
            child.measure(childWidthSpec, childHeightSpec)

            val childWidth = child.measuredWidth + lp.leftMargin + lp.rightMargin
            val childHeight = child.measuredHeight + lp.topMargin + lp.bottomMargin

            if (x + childWidth > paddingLeft + availableWidth && x > paddingLeft) {
                // Wrap to next line
                x = paddingLeft
                y += rowHeight
                rowHeight = 0
            }

            rowHeight = maxOf(rowHeight, childHeight)
            x += childWidth
        }

        val totalHeight = y + rowHeight + paddingBottom

        val finalHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> totalHeight.coerceAtMost(heightSize)
            else -> totalHeight
        }

        setMeasuredDimension(widthSize, finalHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val availableWidth = (r - l) - paddingLeft - paddingRight

        var x = paddingLeft
        var y = paddingTop
        var rowHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue

            val lp = child.layoutParams as? MarginLayoutParams ?: MarginLayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
            val childWidth = child.measuredWidth + lp.leftMargin + lp.rightMargin
            val childHeight = child.measuredHeight + lp.topMargin + lp.bottomMargin

            if (x + childWidth > paddingLeft + availableWidth && x > paddingLeft) {
                x = paddingLeft
                y += rowHeight
                rowHeight = 0
            }

            val left = x + lp.leftMargin
            val top = y + lp.topMargin
            child.layout(left, top, left + child.measuredWidth, top + child.measuredHeight)

            rowHeight = maxOf(rowHeight, childHeight)
            x += childWidth
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: LayoutParams): LayoutParams {
        return MarginLayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }
}
