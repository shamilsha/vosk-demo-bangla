package com.alphacephei.vosk

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

private const val TAG = "VoskEnInRecognizer"
private const val SAMPLE_RATE = 16000f
private const val ASSET_MODEL = "model-en-in"
private const val MODEL_DIR_NAME = "model-en-in"

/**
 * Vosk Indian English recognizer. Load model from assets (copied to filesDir);
 * feed 16 kHz mono PCM (short[]); get text and final flag.
 */
class VoskEnInRecognizer(private val context: Context) {

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var modelPath: String? = null

    val isReady: Boolean
        get() = recognizer != null

    fun getSampleRate(): Int = SAMPLE_RATE.toInt()

    /**
     * Copy model from assets to filesDir if needed; then load model and create recognizer.
     * Call from a background thread.
     */
    fun ensureModelReady(): Boolean {
        if (recognizer != null) return true
        val dir = File(context.filesDir, MODEL_DIR_NAME)
        if (!dir.exists() || !File(dir, "am").exists()) {
            val copied = copyAssetFolder(context, ASSET_MODEL, dir)
            if (!copied) {
                Log.e(TAG, "Failed to copy model from assets")
                return false
            }
        }
        modelPath = dir.absolutePath
        return loadModel()
    }

    private fun loadModel(): Boolean {
        val path = modelPath ?: return false
        return try {
            model = Model(path)
            recognizer = Recognizer(model, SAMPLE_RATE)
            Log.d(TAG, "Vosk Indian English model loaded from $path")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Vosk model", e)
            false
        }
    }

    private var processCallCount = 0

    /**
     * Feed PCM (16 kHz mono short) and return (recognized text, isFinal).
     * Call from a single background thread.
     */
    fun processSamples(samples: ShortArray): Pair<String, Boolean> {
        val rec = recognizer ?: return Pair("", false)
        val byteCount = samples.size * 2
        val bytes = ByteArray(byteCount)
        for (i in samples.indices) {
            val v = samples[i].toInt() and 0xFFFF
            bytes[i * 2] = (v and 0xFF).toByte()
            bytes[i * 2 + 1] = (v shr 8).toByte()
        }
        return try {
            val accepted = rec.acceptWaveForm(bytes, byteCount)
            val resultStr = rec.result
            val partialStr = rec.partialResult
            processCallCount++
            if (processCallCount <= 3 || processCallCount % 50 == 0) {
                Log.d(TAG, "Vosk raw: accepted=$accepted result=\"$resultStr\" partial=\"$partialStr\"")
            }
            val text = if (accepted) {
                parseResultText(resultStr)
            } else {
                parsePartialOrResult(partialStr)
            }
            Pair(text, accepted)
        } catch (e: Exception) {
            Log.e(TAG, "processSamples error", e)
            Pair("", false)
        }
    }

    /** Call after end of stream to get final result. */
    fun getFinalResult(): String {
        val rec = recognizer ?: return ""
        return try {
            parseResultText(rec.finalResult)
        } catch (e: Exception) {
            Log.e(TAG, "getFinalResult error", e)
            ""
        }
    }

    fun reset() {
        try {
            recognizer?.reset()
        } catch (e: Exception) {
            Log.e(TAG, "reset error", e)
        }
    }

    fun close() {
        try {
            recognizer?.close()
            model?.close()
        } catch (e: Exception) {
            Log.e(TAG, "close error", e)
        }
        recognizer = null
        model = null
    }

    /** Parse final result JSON: has "text" key. */
    private fun parseResultText(jsonStr: String): String {
        if (jsonStr.isBlank()) return ""
        return try {
            val obj = JSONObject(jsonStr)
            obj.optString("text", "").trim()
        } catch (e: Exception) {
            ""
        }
    }

    /** Parse partial result JSON: try "partial" then "text" (API varies). */
    private fun parsePartialOrResult(jsonStr: String): String {
        if (jsonStr.isBlank()) return ""
        return try {
            val obj = JSONObject(jsonStr)
            val partial = obj.optString("partial", "").trim()
            val text = obj.optString("text", "").trim()
            when {
                partial.isNotEmpty() -> partial
                text.isNotEmpty() -> text
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun copyAssetFolder(context: Context, assetPath: String, outDir: File): Boolean {
        return try {
            val list = context.assets.list(assetPath) ?: return false
            outDir.mkdirs()
            for (name in list) {
                val subAsset = "$assetPath/$name"
                val subFile = File(outDir, name)
                if (context.assets.list(subAsset)?.isNotEmpty() == true) {
                    copyAssetFolder(context, subAsset, subFile)
                } else {
                    subFile.parentFile?.mkdirs()
                    context.assets.open(subAsset).use { input ->
                        subFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "copyAssetFolder failed", e)
            false
        }
    }
}
