package com.alphacephei.vosk

import android.content.Context
import android.content.res.ColorStateList
import android.widget.Button
import androidx.core.content.ContextCompat

/**
 * Control bar (Start/Stop, Pause/Resume) button styling. Extracted to reduce MainActivity.kt size.
 */
object ControlBarUtils {

    fun setControlStartStopButton(context: Context, btn: Button, isRunning: Boolean) {
        btn.text = if (isRunning) context.getString(R.string.stop) else context.getString(R.string.control_start)
        val color = ContextCompat.getColor(context, if (isRunning) R.color.control_danger else R.color.control_primary)
        btn.backgroundTintList = ColorStateList.valueOf(color)
    }

    fun setControlPauseResumeButton(context: Context, btn: Button, isPaused: Boolean) {
        btn.text = if (isPaused) context.getString(R.string.control_resume) else context.getString(R.string.control_pause)
        val color = ContextCompat.getColor(context, if (isPaused) R.color.control_success else R.color.control_warning)
        btn.backgroundTintList = ColorStateList.valueOf(color)
    }
}
