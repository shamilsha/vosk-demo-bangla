package com.alphacephei.vosk

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Uri/ContentResolver helpers. Extracted to reduce MainActivity.kt size.
 */
object UriUtils {

    fun getDisplayName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) return c.getString(i)
            }
        }
        return null
    }
}
