package com.alphacephei.vosk

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class PartsOfSpeechActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PAGE = "page"
        const val PAGE_PARTS_OF_SPEECH = "parts-of-speech"
        const val PAGE_SVO_SENTENCES = "svo-sentences"
    }

    /**
     * Shows HTML from assets: parts-of-speech color guide or SVO sentence combinations.
     * Works fully offline. Use EXTRA_PAGE to choose which page (default: parts-of-speech).
     */
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parts_of_speech)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val page = intent?.getStringExtra(EXTRA_PAGE) ?: PAGE_PARTS_OF_SPEECH
        val (titleRes, htmlFile) = when (page) {
            PAGE_SVO_SENTENCES -> R.string.svo_sentences_title to "svo-sentences.html"
            else -> R.string.parts_of_speech_title to "parts-of-speech.html"
        }
        title = getString(titleRes)

        val webView = findViewById<WebView>(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/$htmlFile")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
