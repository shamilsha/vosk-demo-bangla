package com.alphacephei.vosk

import android.content.res.AssetManager
import android.util.Log
import java.nio.charset.StandardCharsets

/**
 * Load conversation script from assets. Extracted to reduce MainActivity.kt size.
 */
object ConversationScriptLoader {

    private const val TAG = "ConversationScriptLoader"

    /** Format: one line per turn, colon-separated: P1 or P2 : English text : Bengali text. */
    fun loadConversationScript(assetManager: AssetManager, assetFileName: String): List<ConversationLine> {
        return try {
            assetManager.open(assetFileName).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                reader.readLines().mapNotNull { line ->
                    val parts = line.split(':', limit = 3)
                    if (parts.size >= 3) {
                        ConversationLine(
                            speaker = parts[0].trim(),
                            english = parts[1].trim(),
                            bengali = parts[2].trim()
                        )
                    } else if (parts.size == 2) {
                        ConversationLine(speaker = parts[0].trim(), english = parts[1].trim(), bengali = "")
                    } else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load conversation script: $assetFileName", e)
            emptyList()
        }
    }
}
