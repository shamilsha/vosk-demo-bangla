package com.alphacephei.vosk

import android.app.Activity
import android.util.Log
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.getEndpointConfig
import com.k2fsa.sherpa.onnx.getFeatureConfig
import com.k2fsa.sherpa.onnx.getModelConfig
import com.k2fsa.sherpa.onnx.getOnlineLMConfig
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

private const val TAG = "com.alphacephei.vosk"

class Recognizer {

    private lateinit var model: OnlineRecognizer
    private lateinit var stream: OnlineStream
    private val sampleRateInHz = 16000
    private var inSpeech = false;

    private lateinit var audioData: ShortArray
    private var audioDataLastChunkSize: Int = 0

    /**
     * Initializes the model. Take a note that method is slow and has to be run in separate context
     *
     * @param activity - activity to access model assets
     */
    fun initModel(activity : Activity) {
        val type = 0
        println("Select model type $type")
        val config = OnlineRecognizerConfig(
            featConfig = getFeatureConfig(sampleRate = sampleRateInHz, featureDim = 80),
            modelConfig = getModelConfig(type = 0),
            lmConfig = getOnlineLMConfig(type = 0),
            endpointConfig = getEndpointConfig(),
            enableEndpoint = true,
        )

        model = OnlineRecognizer(
            assetManager = activity.assets,
            config = config,
        )
        stream = model.createStream()
    }

    fun getSampleRate(): Int {
        return sampleRateInHz
    }

    fun resetModel() {
        model.reset(stream)
        audioData = ShortArray(0)
        audioDataLastChunkSize = 0
        inSpeech = false;
    }

    /**
     * Takes a piece of data and returns current results
     *
     * @buffer : audio data
     * @return a pair of recognized text, a flag to end current utterance
     */
    fun processSamples(buffer: ShortArray): Pair<String, Boolean> {
        if (!inSpeech) {
            val norm = sqrt(buffer.sumOf { it * it.toDouble() } / buffer.size )
            if (norm < 300.0f) {
                audioDataLastChunkSize = min(audioDataLastChunkSize + buffer.size, 2 * buffer.size);
                audioData += buffer
                return Pair("", false)
            }

            inSpeech = true;
            if (audioDataLastChunkSize > 0) {
                val startIndex = max(0, audioData.size - this.audioDataLastChunkSize);
                val prevSamples = audioData.copyOfRange(startIndex, audioData.size)
                val prevFloatSamples = FloatArray(prevSamples.size) { prevSamples[it] / 32768.0f }
                stream.acceptWaveform(prevFloatSamples, sampleRate = sampleRateInHz)
            }
        }
        audioData += buffer

        val samples = FloatArray(buffer.size) { buffer[it] / 32768.0f }
        stream.acceptWaveform(samples, sampleRate = sampleRateInHz)
        var text: String
        var isEndpoint: Boolean
        val timeMillis = measureTimeMillis {
            while (model.isReady(stream)) {
                model.decode(stream)
            }
            isEndpoint = model.isEndpoint(stream)
            text = model.getResult(stream).text
        }
        Log.d(TAG, "Time $timeMillis")

        if (isEndpoint) {
            if (text.isNotBlank()) {

                model.reset(stream)
                inSpeech = false
                audioDataLastChunkSize = 0

                return Pair(text, true)
            }
        }
        return Pair(text, false)
    }

}
