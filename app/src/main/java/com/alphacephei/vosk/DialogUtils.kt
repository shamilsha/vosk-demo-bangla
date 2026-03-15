package com.alphacephei.vosk

import android.content.Context
import android.app.AlertDialog

/**
 * Simple dialog helpers. Extracted to reduce MainActivity.kt size.
 */
object DialogUtils {

    /** Show info dialog (title + message, OK button). Call onDismiss when dialog is closed (e.g. to stop TTS). */
    fun showInfoDialog(context: Context, title: String, message: String, onDismiss: (() -> Unit)? = null) {
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.setOnDismissListener { onDismiss?.invoke() }
        dialog.show()
    }
}
