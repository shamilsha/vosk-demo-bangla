package com.alphacephei.vosk

import android.Manifest
import android.media.AudioFormat

/** Constants used by MainActivity. Extracted to reduce MainActivity.kt size. */
internal const val MAIN_ACTIVITY_TAG = "com.alphacephei.vosk"

/** Request code for ActivityCompat.requestPermissions (must be unique per activity). */
internal const val PERMISSION_REQUEST_CODE = 100

/** Permissions requested on startup (mic required; storage/network optional). */
internal val INITIAL_PERMISSIONS = arrayOf(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.INTERNET
)

// Set to true to test: mic will record but NOT call recognizer (to see if crash is in native lib).
internal const val SKIP_RECOGNITION_FOR_MIC = false

// Use system SpeechRecognizer for English verification. Set to false to use Vosk Indian English.
internal const val USE_SYSTEM_SPEECH_FOR_ENGLISH_VERIFICATION = true

/** Delay (ms) before starting mic after "Try again" if TTS onDone does not fire. */
internal const val TRY_AGAIN_LISTEN_FALLBACK_DELAY_MS = 2500L

/** AudioRecord: mono channel, 16-bit PCM (API 21 compatible). */
internal val AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
internal val AUDIO_FORMAT_PCM_16 = AudioFormat.ENCODING_PCM_16BIT

/** Min samples to feed before first recognizer call (1 s at 16 kHz); avoids native crash on first small chunk. */
internal const val MIN_SAMPLES_BEFORE_RECOGNIZER = 16000

/** Minimum recording buffer size in samples (e.g. 100 ms at 16 kHz). */
internal const val MIN_RECORDING_BUFFER_SAMPLES = 1600
