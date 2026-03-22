package com.alphacephei.vosk

import android.Manifest.permission.INTERNET
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.ToneGenerator
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ReplacementSpan
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.BaseAdapter
import android.widget.ListView
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.drawerlayout.widget.DrawerLayout
import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.URLDecoder
import java.net.URLEncoder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread
import kotlin.math.max
import java.util.Locale
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateLanguage


private const val TAG = "com.alphacephei.vosk"

class MainActivity : AppCompatActivity() {

    /** Built-in pronunciation lessons: title → rows of [Word, Pronunciation, Meaning]. Meaning may be "". */
    private fun getPronunciationLessons(): List<Pair<String, List<List<String>>>> = listOf(
        "P words" to listOf(
            listOf("Pencil", "ফেন্সিল্", ""),
            listOf("Party", "ফা:র্টি", ""),
            listOf("Pen", "ফেন্ট", ""),
            listOf("Please", "প্লীয", ""),
            listOf("Power", "ফাওয়ায়ঃ", ""),
            listOf("Person", "ফা:র্সন", ""),
            listOf("Paper", "ফেইফায়ঃ", ""),
            listOf("Popular", "ফফিউলা:", ""),
            listOf("Public", "ফাবলিখ", ""),
            listOf("Private", "প্রাইভেট্ঠ", "")
        ),
        "T words" to listOf(
            listOf("Tall", "ঠল্", ""),
            listOf("Talk", "ঠক", ""),
            listOf("Town", "ঠাউন", ""),
            listOf("Tower", "ঠাওয়ায়ঃ", ""),
            listOf("Table", "ঠেইবল্", ""),
            listOf("Teaching", "ঠিচিং", ""),
            listOf("Team", "ঠীম", ""),
            listOf("Technology", "ঠেকনোলজি", ""),
            listOf("Time", "ঠাইম্", ""),
            listOf("Topic", "ঠফিখ", "")
        ),
        "C / K words" to listOf(
            listOf("Cute", "কিউট্ঠ", ""),
            listOf("Keyboard", "খীবোর্ড", ""),
            listOf("Clean", "ক্লিন", ""),
            listOf("Kick", "খীক", ""),
            listOf("Careful", "খেয়া:ফুল", ""),
            listOf("King", "খীং", ""),
            listOf("Queen", "খ্যুইন", ""),
            listOf("Quick", "কুইখ", ""),
            listOf("Question", "খোয়েশ্চেন্", ""),
            listOf("Quality", "খোয়ালিটি", "")
        ),
        "Rule 23: silent G (Design, Resign…)" to listOf(
            listOf("Design", "ডীযাইন", "নকশা"),
            listOf("Resign", "রীযাইন", "পদত্যাগ করা"),
            listOf("Campaign", "খ্যামফেইন", "অভিযান"),
            listOf("Reign", "রেইন", "শাসন করা"),
            listOf("Foreign", "ফরেন", "বিদেশ")
        ),
        "Rule 24: Days (-ei sound)" to listOf(
            listOf("Saturday", "স্যাঠারডেই", ""),
            listOf("Sunday", "সানডেই", ""),
            listOf("Monday", "মানডেই", ""),
            listOf("Tuesday", "টিউযডেই", ""),
            listOf("Wednesday", "ওয়েন্জডেই", ""),
            listOf("Thursday", "থারযডেই", ""),
            listOf("Friday", "ফ্রাইডেই", "")
        ),
        "Rules 25–30: Suffixes" to listOf(
            listOf("-age", "ইজ্", "Village, Courage"),
            listOf("-ate", "আট্ / এট্", "Certificate, Private"),
            listOf("-ite", "আইট্", "Polite, Site"),
            listOf("-sure", "ঝায়ঃ (Zher)", "Pleasure, Measure"),
            listOf("-ture", "চায়ঃ (Cher)", "Future, Nature"),
            listOf("-cian", "শান্", "Musician, Optician")
        ),
        "-tion words" to listOf(
            listOf("Nation", "নেশন", "জাতি"),
            listOf("Pronunciation", "প্রোনাউন্সিয়েশন", "উচ্চারণ"),
            listOf("Situation", "সিচুয়েশন", "অবস্থা"),
            listOf("Education", "এডুকেশন", "শিক্ষা"),
            listOf("Presentation", "প্রেজেন্টেশন", "উপস্থাপন")
        ),
        "-ly words" to listOf(
            listOf("Automatically", "অটোমেটিকালী", "সয়ংক্রিয়ভাবে"),
            listOf("Basically", "বেইসিকালী", "মূলত"),
            listOf("Politically", "পলিটিকালী", "রাজনৈতিকভাবে"),
            listOf("Specifically", "স্পেসিফিকালী", "বিশেষভাবে")
        ),
        "-ial words (1)" to listOf(
            listOf("Name", "নেইম", "নাম"),
            listOf("Basic", "বেইসিক", "মৌলিক"),
            listOf("Beneficial", "বেনিফিশিয়াল", "উপকারী"),
            listOf("Artificial", "আর্টিফিশিয়াল", "কৃত্রিম"),
            listOf("Official", "অফিসিয়াল", "দাপ্তরিক"),
            listOf("Residential", "রেসিডেন্সিয়াল", "আবাসিক"),
            listOf("Nature", "নেইচার", "প্রকৃতি"),
            listOf("Future", "ফিউচার", "ভবিষ্যৎ")
        ),
        "-day / -ay words" to listOf(
            listOf("Day", "ডেই", "দিন"),
            listOf("Today", "টুডেই", "আজ"),
            listOf("Monday", "মানডেই", "সোমবার"),
            listOf("Sunday", "সানডেই", "রবিবার"),
            listOf("Birthday", "বার্থডেই", "জন্মদিন"),
            listOf("Holiday", "হলিডেই", "ছুটির দিন"),
            listOf("Way", "ওয়েই", "পথ"),
            listOf("Say", "সেই", "বলা"),
            listOf("Play", "প্লেই", "খেলা"),
            listOf("Pray", "প্রেই", "প্রার্থনা"),
            listOf("May", "মেই", "মে মাস"),
            listOf("Stay", "স্টেই", "অবস্থান")
        ),
        "-sion words" to listOf(
            listOf("Conclusion", "কনক্লুশান", "উপসংহার"),
            listOf("Decision", "ডিসিশান", "সিদ্ধান্ত"),
            listOf("Vision", "ভিশান", "দৃষ্টি"),
            listOf("Television", "টেলিভিশন", "টেলিভিশন")
        ),
        "-ture words" to listOf(
            listOf("Nature", "নেইচার", "প্রকৃতি"),
            listOf("Future", "ফিউচার", "ভবিষ্যৎ"),
            listOf("Picture", "পিকচার", "ছবি"),
            listOf("Culture", "কালচার", "সংস্কৃতি"),
            listOf("Furniture", "ফার্নিচার", "আসবাবপত্র"),
            listOf("Structure", "স্ট্রাকচার", "কাঠামো")
        ),
        "-ial words (2)" to listOf(
            listOf("Official", "অফিসিয়াল", "দাপ্তরিক"),
            listOf("Social", "সোশ্যাল", "সামাজিক"),
            listOf("Special", "স্পেশাল", "বিশেষ"),
            listOf("Essential", "এসেনশিয়াল", "অপরিহার্য"),
            listOf("Potential", "পটেনশিয়াল", "সম্ভাবনা"),
            listOf("Partial", "পার্শিয়াল", "আংশিক")
        ),
        "-ous words" to listOf(
            listOf("Famous", "ফেইমাস", "বিখ্যাত"),
            listOf("Pious", "ফাইয়াস", "ধার্মিক"),
            listOf("Serious", "সিরিয়াস", "গুরুতর"),
            listOf("Continuous", "কন্টিনিউয়াস", "নিরবিচ্ছিন্ন"),
            listOf("Dangerous", "ডেইঞ্জারাস", "বিপজ্জনক")
        ),
        "-ment words" to listOf(
            listOf("Government", "গাভার্নমান্ট", "সরকার"),
            listOf("Development", "ডিভেলাপমান্ট", "উন্নয়ন"),
            listOf("Movement", "মুভমান্ট", "আন্দোলন"),
            listOf("Management", "ম্যানেজমান্ট", "ব্যবস্থাপনা"),
            listOf("Environment", "এনভায়রনমান্ট", "পরিবেশ")
        ),
        "-fully words" to listOf(
            listOf("Beautifully", "বিউটিফুলি", "সুন্দরভাবে"),
            listOf("Carefully", "খেয়ারফুলি", "সতর্কভাবে"),
            listOf("Successfully", "সাকসেসফুলি", "সফলভাবে"),
            listOf("Faithfully", "ফেইথফুলি", "বিশ্বস্তভাবে")
        ),
        "Silent e (a-e)" to listOf(
            listOf("Name", "নেইম", "নাম"),
            listOf("Come", "কাম", "আসা"),
            listOf("Take", "টেইক", "নেওয়া"),
            listOf("Make", "মেইক", "তৈরি করা"),
            listOf("Change", "চেইঞ্জ", "পরিবর্তন")
        ),
        "Silent G (gn at end)" to listOf(
            listOf("Design", "ডিজাইন", "নকশা"),
            listOf("Resign", "রিজাইন", "পদত্যাগ করা"),
            listOf("Campaign", "ক্যাম্পেইন", "অভিযান"),
            listOf("Foreign", "ফরেন", "বিদেশ")
        ),
        "Silent B" to listOf(
            listOf("Bomb", "বাম", "বোমা"),
            listOf("Comb", "কোম", "চিরুনি"),
            listOf("Thumb", "থাম", "হাতের বৃদ্ধাঙ্গুলি"),
            listOf("Climb", "ক্লাইম", "আরোহণ করা"),
            listOf("Dumb", "ডাম", "বোবা")
        ),
        "Silent W" to listOf(
            listOf("Write", "রাইট", "লেখা"),
            listOf("Wrong", "রং", "ভুল"),
            listOf("Wrist", "রিস্ট", "কবজি"),
            listOf("Wrap", "র‍্যাপ", "মোড়ানো")
        ),
        "Silent K" to listOf(
            listOf("Know", "নো", "জানা"),
            listOf("Knee", "নী", "হাঁটু"),
            listOf("Knife", "নাইফ", "ছুরি"),
            listOf("Knowledge", "নলেজ", "জ্ঞান"),
            listOf("Knight", "নাইট", "বীর যোদ্ধা")
        ),
        "-al words" to listOf(
            listOf("National", "ন্যাশানাল", "জাতীয়"),
            listOf("Political", "পলিটিকাল", "রাজনৈতিক"),
            listOf("Normal", "নরমাল", "স্বাভাবিক"),
            listOf("Natural", "ন্যাচারাল", "প্রাকৃতিক"),
            listOf("Formal", "ফরমাল", "আনুষ্ঠানিক")
        ),
        "-sure words" to listOf(
            listOf("Pleasure", "প্লেঝার", "আনন্দ"),
            listOf("Measure", "মেঝার", "পরিমাপ"),
            listOf("Treasure", "ট্রেঝার", "সম্পদ/গুপ্তধন"),
            listOf("Leisure", "লেঝার", "অবসর")
        ),
        "-age words" to listOf(
            listOf("Village", "ভিলিজ", "গ্রাম"),
            listOf("Courage", "কারিজ", "সাহস"),
            listOf("Marriage", "ম্যারিজ", "বিবাহ"),
            listOf("Language", "ল্যাংগুয়েজ", "ভাষা"),
            listOf("Message", "মেসেজ", "বার্তা")
        ),
        "-tion (Mention, Action…)" to listOf(
            listOf("Mention", "মেনশন", "উল্লেখ করা"),
            listOf("Fiction", "ফিকশন", "কথাসাহিত্য"),
            listOf("Condition", "কন্ডিশন", "অবস্থা"),
            listOf("Action", "অ্যাকশন", "কাজ"),
            listOf("Relation", "রিলেশন", "সম্পর্ক")
        )
    )

    /** Alphabet table: English letter → Bengali pronunciation (even column). Used when user taps a highlighted letter. */
    private val alphabetBengaliPronunciation = mapOf(
        "A" to "এই",
        "F" to "এফ",
        "J" to "জ্বেই",
        "K" to "খেই",
        "O" to "ওউ",
        "P" to "ফী",
        "Q" to "খিউ",
        "T" to "ঠী",
        "V" to "ভী",
        "Z" to "জী / যেড"
    )

    /** One row from a lesson file: A=English Q, B=Bengali Q, C=English A, D=Bengali A (pipe-separated). */
    data class LessonRow(val engQ: String, val bnQ: String, val engA: String, val bnA: String)

    /** One row for verb conjugation: English sentence, Bengali sentence. */
    private data class VerbRow(val english: String, val bengali: String)

    /** Built-in verb conjugations: DO, HAVE, GO (Subject | English | Bengali). */
    private fun getVerbData(): Map<String, List<VerbRow>> = mapOf(
        "DO" to listOf(
            VerbRow("I do.", "আমি করি।"),
            VerbRow("You do.", "তুমি কর।"),
            VerbRow("He does.", "সে (ছেলে) করে।"),
            VerbRow("She does.", "সে (মেয়ে) করে।"),
            VerbRow("We do.", "আমরা করি।"),
            VerbRow("They do.", "তারা করে।")
        ),
        "HAVE" to listOf(
            VerbRow("I have.", "আমার আছে।"),
            VerbRow("You have.", "তোমার আছে।"),
            VerbRow("He has.", "তার আছে।"),
            VerbRow("She has.", "তার আছে।"),
            VerbRow("We have.", "আমাদের আছে।"),
            VerbRow("They have.", "তাদের আছে।")
        ),
        "GO" to listOf(
            VerbRow("I go.", "আমি যাই।"),
            VerbRow("You go.", "তুমি যাও।"),
            VerbRow("He goes.", "সে যায়।"),
            VerbRow("She goes.", "সে যায়।"),
            VerbRow("We go.", "আমরা যাই।"),
            VerbRow("They go.", "তারা যায়।")
        )
    )

    /** Build lesson rows from a verb: speak Bengali, user says English, match to English. */
    private fun buildLessonFromVerb(verbName: String): List<LessonRow> {
        val rows = getVerbData()[verbName] ?: return emptyList()
        return rows.map { v -> LessonRow(v.english, v.bengali, v.english, v.bengali) }
    }

    /** 12 tenses per (verb, subject): verb -> subject -> list of (English, Bengali). */
    private fun getTenseData(): Map<String, Map<String, List<VerbRow>>> {
        val eatI = listOf(
            VerbRow("I eat.", "আমি খাই।"),
            VerbRow("I am eating.", "আমি খাচ্ছি।"),
            VerbRow("I have eaten.", "আমি খেয়েছি।"),
            VerbRow("I have been eating.", "আমি (কিছুক্ষণ ধরে) খাচ্ছি।"),
            VerbRow("I ate.", "আমি খেয়েছিলাম।"),
            VerbRow("I was eating.", "আমি খাচ্ছিলাম।"),
            VerbRow("I had eaten.", "আমি খেয়েছিলাম (অন্য কাজের আগে)।"),
            VerbRow("I had been eating.", "আমি (কিছুক্ষণ ধরে) খাচ্ছিলাম।"),
            VerbRow("I will eat.", "আমি খাব।"),
            VerbRow("I will be eating.", "আমি খেতে থাকব।"),
            VerbRow("I will have eaten.", "আমি খেয়ে থাকব।"),
            VerbRow("I will have been eating.", "আমি (কিছুক্ষণ ধরে) খেতে থাকব।")
        )
        val eatYou = listOf(
            VerbRow("You eat.", "তুমি খাও।"),
            VerbRow("You are eating.", "তুমি খাচ্ছো।"),
            VerbRow("You have eaten.", "তুমি খেয়েছো।"),
            VerbRow("You have been eating.", "তুমি (কিছুক্ষণ ধরে) খাচ্ছো।"),
            VerbRow("You ate.", "তুমি খেয়েছিলে।"),
            VerbRow("You were eating.", "তুমি খাচ্ছিলে।"),
            VerbRow("You had eaten.", "তুমি খেয়েছিলে (অন্য কাজের আগে)।"),
            VerbRow("You had been eating.", "তুমি (কিছুক্ষণ ধরে) খাচ্ছিলে।"),
            VerbRow("You will eat.", "তুমি খাবে।"),
            VerbRow("You will be eating.", "তুমি খেতে থাকবে।"),
            VerbRow("You will have eaten.", "তুমি খেয়ে থাকবে।"),
            VerbRow("You will have been eating.", "তুমি (কিছুক্ষণ ধরে) খেতে থাকবে।")
        )
        val eatHe = listOf(
            VerbRow("He eats.", "সে খায়।"),
            VerbRow("He is eating.", "সে খাচ্ছে।"),
            VerbRow("He has eaten.", "সে খেয়েছে।"),
            VerbRow("He has been eating.", "সে (কিছুক্ষণ ধরে) খাচ্ছে।"),
            VerbRow("He ate.", "সে খেয়েছিল।"),
            VerbRow("He was eating.", "সে খাচ্ছিল।"),
            VerbRow("He had eaten.", "সে খেয়েছিল (অন্য কাজের আগে)।"),
            VerbRow("He had been eating.", "সে (কিছুক্ষণ ধরে) খাচ্ছিল।"),
            VerbRow("He will eat.", "সে খাবে।"),
            VerbRow("He will be eating.", "সে খেতে থাকবে।"),
            VerbRow("He will have eaten.", "সে খেয়ে থাকবে।"),
            VerbRow("He will have been eating.", "সে (কিছুক্ষণ ধরে) খেতে থাকবে।")
        )
        val eatShe = listOf(
            VerbRow("She eats.", "সে খায়।"),
            VerbRow("She is eating.", "সে খাচ্ছে।"),
            VerbRow("She has eaten.", "সে খেয়েছে।"),
            VerbRow("She has been eating.", "সে (কিছুক্ষণ ধরে) খাচ্ছে।"),
            VerbRow("She ate.", "সে খেয়েছিল।"),
            VerbRow("She was eating.", "সে খাচ্ছিল।"),
            VerbRow("She had eaten.", "সে খেয়েছিল (অন্য কাজের আগে)।"),
            VerbRow("She had been eating.", "সে (কিছুক্ষণ ধরে) খাচ্ছিল।"),
            VerbRow("She will eat.", "সে খাবে।"),
            VerbRow("She will be eating.", "সে খেতে থাকবে।"),
            VerbRow("She will have eaten.", "সে খেয়ে থাকবে।"),
            VerbRow("She will have been eating.", "সে (কিছুক্ষণ ধরে) খেতে থাকবে।")
        )
        val eatWe = listOf(
            VerbRow("We eat.", "আমরা খাই।"),
            VerbRow("We are eating.", "আমরা খাচ্ছি।"),
            VerbRow("We have eaten.", "আমরা খেয়েছি।"),
            VerbRow("We have been eating.", "আমরা (কিছুক্ষণ ধরে) খাচ্ছি।"),
            VerbRow("We ate.", "আমরা খেয়েছিলাম।"),
            VerbRow("We were eating.", "আমরা খাচ্ছিলাম।"),
            VerbRow("We had eaten.", "আমরা খেয়েছিলাম (অন্য কাজের আগে)।"),
            VerbRow("We had been eating.", "আমরা (কিছুক্ষণ ধরে) খাচ্ছিলাম।"),
            VerbRow("We will eat.", "আমরা খাব।"),
            VerbRow("We will be eating.", "আমরা খেতে থাকব।"),
            VerbRow("We will have eaten.", "আমরা খেয়ে থাকব।"),
            VerbRow("We will have been eating.", "আমরা (কিছুক্ষণ ধরে) খেতে থাকব।")
        )
        val eatThey = listOf(
            VerbRow("They eat.", "তারা খায়।"),
            VerbRow("They are eating.", "তারা খাচ্ছে।"),
            VerbRow("They have eaten.", "তারা খেয়েছে।"),
            VerbRow("They have been eating.", "তারা (কিছুক্ষণ ধরে) খাচ্ছে।"),
            VerbRow("They ate.", "তারা খেয়েছিল।"),
            VerbRow("They were eating.", "তারা খাচ্ছিল।"),
            VerbRow("They had eaten.", "তারা খেয়েছিল (অন্য কাজের আগে)।"),
            VerbRow("They had been eating.", "তারা (কিছুক্ষণ ধরে) খাচ্ছিল।"),
            VerbRow("They will eat.", "তারা খাবে।"),
            VerbRow("They will be eating.", "তারা খেতে থাকবে।"),
            VerbRow("They will have eaten.", "তারা খেয়ে থাকবে।"),
            VerbRow("They will have been eating.", "তারা (কিছুক্ষণ ধরে) খেতে থাকবে।")
        )
        val doI = listOf(
            VerbRow("I do.", "আমি করি।"),
            VerbRow("I am doing.", "আমি করছি।"),
            VerbRow("I have done.", "আমি করেছি।"),
            VerbRow("I have been doing.", "আমি (কিছুক্ষণ ধরে) করছি।"),
            VerbRow("I did.", "আমি করেছিলাম।"),
            VerbRow("I was doing.", "আমি করছিলাম।"),
            VerbRow("I had done.", "আমি করেছিলাম (অন্য কাজের আগে)।"),
            VerbRow("I had been doing.", "আমি (কিছুক্ষণ ধরে) করছিলাম।"),
            VerbRow("I will do.", "আমি করব।"),
            VerbRow("I will be doing.", "আমি করতে থাকব।"),
            VerbRow("I will have done.", "আমি করে থাকব।"),
            VerbRow("I will have been doing.", "আমি (কিছুক্ষণ ধরে) করতে থাকব।")
        )
        return mapOf(
            "EAT" to mapOf(
                "I" to eatI, "You" to eatYou, "He" to eatHe, "She" to eatShe, "We" to eatWe, "They" to eatThey
            ),
            "DO" to mapOf(
                "I" to doI,
                "You" to listOf(
                    VerbRow("You do.", "তুমি কর।"), VerbRow("You are doing.", "তুমি করছো।"), VerbRow("You have done.", "তুমি করেছো।"),
                    VerbRow("You have been doing.", "তুমি (কিছুক্ষণ ধরে) করছো।"), VerbRow("You did.", "তুমি করেছিলে।"), VerbRow("You were doing.", "তুমি করছিলে।"),
                    VerbRow("You had done.", "তুমি করেছিলে (অন্য কাজের আগে)।"), VerbRow("You had been doing.", "তুমি (কিছুক্ষণ ধরে) করছিলে।"),
                    VerbRow("You will do.", "তুমি করবে।"), VerbRow("You will be doing.", "তুমি করতে থাকবে।"), VerbRow("You will have done.", "তুমি করে থাকবে।"),
                    VerbRow("You will have been doing.", "তুমি (কিছুক্ষণ ধরে) করতে থাকবে।")
                ),
                "He" to listOf(
                    VerbRow("He does.", "সে করে।"), VerbRow("He is doing.", "সে করছে।"), VerbRow("He has done.", "সে করেছে।"),
                    VerbRow("He has been doing.", "সে (কিছুক্ষণ ধরে) করছে।"), VerbRow("He did.", "সে করেছিল।"), VerbRow("He was doing.", "সে করছিল।"),
                    VerbRow("He had done.", "সে করেছিল (অন্য কাজের আগে)।"), VerbRow("He had been doing.", "সে (কিছুক্ষণ ধরে) করছিল।"),
                    VerbRow("He will do.", "সে করবে।"), VerbRow("He will be doing.", "সে করতে থাকবে।"), VerbRow("He will have done.", "সে করে থাকবে।"),
                    VerbRow("He will have been doing.", "সে (কিছুক্ষণ ধরে) করতে থাকবে।")
                ),
                "She" to listOf(
                    VerbRow("She does.", "সে করে।"), VerbRow("She is doing.", "সে করছে।"), VerbRow("She has done.", "সে করেছে।"),
                    VerbRow("She has been doing.", "সে (কিছুক্ষণ ধরে) করছে।"), VerbRow("She did.", "সে করেছিল।"), VerbRow("She was doing.", "সে করছিল।"),
                    VerbRow("She had done.", "সে করেছিল (অন্য কাজের আগে)।"), VerbRow("She had been doing.", "সে (কিছুক্ষণ ধরে) করছিল।"),
                    VerbRow("She will do.", "সে করবে।"), VerbRow("She will be doing.", "সে করতে থাকবে।"), VerbRow("She will have done.", "সে করে থাকবে।"),
                    VerbRow("She will have been doing.", "সে (কিছুক্ষণ ধরে) করতে থাকবে।")
                ),
                "We" to listOf(
                    VerbRow("We do.", "আমরা করি।"), VerbRow("We are doing.", "আমরা করছি।"), VerbRow("We have done.", "আমরা করেছি।"),
                    VerbRow("We have been doing.", "আমরা (কিছুক্ষণ ধরে) করছি।"), VerbRow("We did.", "আমরা করেছিলাম।"), VerbRow("We were doing.", "আমরা করছিলাম।"),
                    VerbRow("We had done.", "আমরা করেছিলাম (অন্য কাজের আগে)।"), VerbRow("We had been doing.", "আমরা (কিছুক্ষণ ধরে) করছিলাম।"),
                    VerbRow("We will do.", "আমরা করব।"), VerbRow("We will be doing.", "আমরা করতে থাকব।"), VerbRow("We will have done.", "আমরা করে থাকব।"),
                    VerbRow("We will have been doing.", "আমরা (কিছুক্ষণ ধরে) করতে থাকব।")
                ),
                "They" to listOf(
                    VerbRow("They do.", "তারা করে।"), VerbRow("They are doing.", "তারা করছে।"), VerbRow("They have done.", "তারা করেছে।"),
                    VerbRow("They have been doing.", "তারা (কিছুক্ষণ ধরে) করছে।"), VerbRow("They did.", "তারা করেছিল।"), VerbRow("They were doing.", "তারা করছিল।"),
                    VerbRow("They had done.", "তারা করেছিল (অন্য কাজের আগে)।"), VerbRow("They had been doing.", "তারা (কিছুক্ষণ ধরে) করছিল।"),
                    VerbRow("They will do.", "তারা করবে।"), VerbRow("They will be doing.", "তারা করতে থাকবে।"), VerbRow("They will have done.", "তারা করে থাকবে।"),
                    VerbRow("They will have been doing.", "তারা (কিছুক্ষণ ধরে) করতে থাকবে।")
                )
            )
        )
    }

    /** Build lesson from (verb, subject): 12 tenses, speak Bengali, user says English. */
    private fun buildLessonFromTense(verbName: String, subjectName: String): List<LessonRow> {
        val rows = getTenseData()[verbName]?.get(subjectName) ?: return emptyList()
        return rows.map { v -> LessonRow(v.english, v.bengali, v.english, v.bengali) }
    }

    /** Show verb picker for tense lesson, then subject picker. */
    private fun showTenseVerbSelectorDialog() {
        val verbs = getTenseData().keys.toList().sorted()
        if (verbs.isEmpty()) {
            Toast.makeText(this, "No tense data", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_verb_title))
            .setItems(verbs.toTypedArray()) { _, which ->
                val verbName = verbs[which]
                val subjects = getTenseData()[verbName]?.keys?.toList()?.sorted() ?: return@setItems
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.select_subject_title))
                    .setItems(subjects.toTypedArray()) inner@{ _, subWhich ->
                        val subjectName = subjects[subWhich]
                        val rows = buildLessonFromTense(verbName, subjectName)
                        if (rows.isEmpty()) return@inner
                        clearPronunciationLessonState()
                        lessonRows = rows
                        lessonName = "tenses_${verbName}_$subjectName"
                        incorrectLessonRows.clear()
                        incorrectLessonSourceName = null
                        lessonCorrectCount = 0
                        lessonMode = 4
                        lessonIndex = 0
                        lessonPhase = "q"
                        lessonMode3Listening = false
                        lessonMode3SpokeAnswer = false
                        lessonIncorrectCount = 0
                        nextButton?.isEnabled = true
                        skipButton?.isEnabled = true
                        clearBothTextAreas()
                        updateLessonStatistic()
                        showVerbDiagram(verbName)
                        textView.text = getString(R.string.lesson_loaded)
                        Toast.makeText(this, getString(R.string.lesson_loaded), Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Current lesson: rows + name + mode (1/2/3) + index + for mode 1: phase "q" or "a". */
    private var lessonRows: List<LessonRow>? = null
    private var lessonName: String? = null
    private var lessonMode: Int = 0
    private var lessonIndex: Int = 0
    private var lessonPhase: String = "q"
    /** Mode 3: true while waiting for user to speak question in English; on result we speak C. */
    private var lessonMode3Listening: Boolean = false
    /** Mode 3: true after we spoke C for current row; Next then advances to next row. */
    private var lessonMode3SpokeAnswer: Boolean = false
    /** Incorrect attempts for current lesson item; after 3 we auto-advance to next. */
    private var lessonIncorrectCount: Int = 0
    /** Correct answers so far in the current lesson (for live statistic x/y). */
    private var lessonCorrectCount: Int = 0
    /** Rows that were answered incorrectly; saved to file and loadable as "Practice incorrect words". */
    private val incorrectLessonRows = mutableListOf<LessonRow>()
    /** Original lesson name for the incorrect list (saved to file); displayed as name + "_inc". */
    private var incorrectLessonSourceName: String? = null
    /** Optional pronunciation hint per row for simple-sentence layout (third column in Lessons/SVO/simple_*.txt). */
    private var simpleSentencePronunciations: List<String>? = null
    /** true = Practice (hide English, say after Bengali); false = Learning (show English). */
    private var simpleSentencePracticeMode: Boolean = false
    /** In Learning mode: after TTS speaks Bengali we speak this English phrase, then start verification. */
    private var pendingSimpleSentenceEnglishForLearning: String? = null
    /** True when TTS or mic is active for simple-sentence lesson (show Stop). */
    private var simpleSentenceControlRunning: Boolean = false
    /** True when user tapped Pause (show Resume). */
    private var simpleSentenceControlPaused: Boolean = false
    private var controlActionsBar: View? = null
    private var controlStartStopButton: Button? = null
    private var controlPauseResumeButton: Button? = null
    private var controlPlaybackLastButton: ImageButton? = null
    @Volatile
    private var playbackHoldRecording = false
    private var playbackHoldAudioRecord: AudioRecord? = null
    private var playbackHoldThread: Thread? = null
    private var playbackHoldPcm: ByteArrayOutputStream? = null
    private var playbackHoldSampleRate = 16000
    private var playbackHoldAutoPaused = false
    private var playbackHoldResumeAfterPlayback = false
    private var playbackHoldPausedLayout: ContentLayout? = null
    private var playbackHoldAutoPausedByClick = false
    private var playbackHoldPauseButtonToToggle: Button? = null
    /** For SV_RIBBON layout: left labels (subjects), right labels (verbs), Bengali for TTS; same length. */
    private var svRibbonBengali: List<String>? = null
    /** Pronunciation hint per index (Learning mode). Same length as svRibbonBengali. */
    private var svRibbonPronunciation: List<String>? = null
    /** Expected English "Subject Verb" per index for verification. Same length. */
    private var svRibbonExpectedEnglish: List<String>? = null
    /** true = Learning (pronunciation hint, TTS Bengali then English, then verify). false = Practice (Bengali only, then verify). */
    private var svRibbonLearningMode: Boolean = true
    /** Incorrect count for current ribbon item; after 3 we move to next. */
    private var svRibbonIncorrectCount: Int = 0
    private var svRibbonControlRunning: Boolean = false
    private var svRibbonControlPaused: Boolean = false

    /** For THREECOL_TABLE layout: base rows, current rows (possibly shuffled/filtered), adapter, mode, playback state, and persisted stats. */
    private var threeColBaseRows: List<ThreeColRow> = emptyList()
    private var threeColRows: List<ThreeColRow> = emptyList()
    /** For each display index, the base row index (for stats). Same size as threeColRows. */
    private var threeColDisplayToBaseIndex: List<Int> = emptyList()
    private var threeColAdapter: ThreeColDataAdapter? = null
    private var threeColLearningMode: Boolean = true
    private enum class ThreeColMode { LEARNING, PRACTICE, TEST, VOCAB }
    private var threeColMode: ThreeColMode = ThreeColMode.LEARNING
    private var threeColStats: MutableList<IntArray> = mutableListOf()
    private var threeColWeakOnlyFilter: Boolean = false
    /** Session-only stats for UI: reset to 0/0 on load; internal threeColStats kept for filter and persist. */
    private var threeColSessionCorrect: Int = 0
    private var threeColSessionAttempted: Int = 0

    /** Apply persisted stats (in display order) to the adapter so ticks and counts reflect history. */
    private fun applyThreeColStatsToAdapter() {
        val adapter = threeColAdapter ?: return
        if (threeColStats.isEmpty() || threeColDisplayToBaseIndex.isEmpty()) return
        val displayOrderStats = threeColDisplayToBaseIndex.map { baseIdx ->
            threeColStats.getOrNull(baseIdx) ?: intArrayOf(0, 0)
        }
        adapter.applyStatsFrom(displayOrderStats)
    }

    /** True if base row at baseIdx is failed in current mode (Practice: A==0; Test: B==0). Uses in-memory A,B. */
    private fun isThreeColWeakOrUntried(baseIdx: Int): Boolean {
        val row = threeColStats.getOrNull(baseIdx) ?: intArrayOf(0, 0)
        val a = row.getOrNull(0) ?: 0
        val b = row.getOrNull(1) ?: 0
        return when (threeColMode) {
            ThreeColMode.PRACTICE -> a == 0
            ThreeColMode.TEST -> b == 0
            ThreeColMode.LEARNING -> a == 0 || b == 0
            ThreeColMode.VOCAB -> false
        }
    }

    /** Apply filter for current mode: show all rows, or only "did not pass" (weak & untried). */
    private fun applyThreeColFilterForCurrentMode() {
        if (threeColBaseRows.isEmpty()) return
        val total = threeColBaseRows.size
        // Ensure stats list matches base row count so filtering is correct
        while (threeColStats.size < total) {
            threeColStats.add(IntArray(2) { 0 })
        }
        val baseIndices = threeColBaseRows.indices
        threeColDisplayToBaseIndex = if (!threeColWeakOnlyFilter) {
            baseIndices.toList()
        } else {
            val filtered = baseIndices.filter { isThreeColWeakOrUntried(it) }
            if (filtered.isEmpty()) {
                Toast.makeText(this, getString(R.string.threecol_filter_all_passed), Toast.LENGTH_SHORT).show()
                emptyList()
            } else {
                Toast.makeText(this, getString(R.string.threecol_filter_showing_n, filtered.size, total), Toast.LENGTH_SHORT).show()
                filtered
            }
        }
        if (threeColMode == ThreeColMode.TEST) {
            threeColDisplayToBaseIndex = threeColDisplayToBaseIndex.shuffled()
        }
        threeColRows = threeColDisplayToBaseIndex.map { threeColBaseRows[it] }
        threeColCurrentIndex = 0
        threeColLastScrolledPosition = -1
        threeColCachedRowHeightPx = -1
        threeColAdapter?.updateData(threeColRows)
        threeColAdapter?.learningMode = threeColLearningMode
        threeColAdapter?.setCurrentIndex(0)
        // Reset live stats on any transition (tab or toggle) so display is 0/0/total and never exceeds total
        threeColSessionCorrect = 0
        threeColSessionAttempted = 0
        updateThreeColStats()
        updateThreeColRowPositionText()
        contentFrame.getChildAt(0)?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.threecol_recycler)?.requestLayout()
        // Clear verification so we match the new first sentence, not the previous one; if mic was on, restart for first sentence
        if (currentContentLayout == ContentLayout.THREECOL_TABLE) {
            textToSpeech?.stop()
            incorrectFeedbackFallbackRunnable?.let { verificationHandler.removeCallbacks(it) }
            incorrectFeedbackFallbackRunnable = null
            tryAgainListenFallbackRunnable?.let { verificationHandler.removeCallbacks(it) }
            tryAgainListenFallbackRunnable = null
            cancelVerificationTimeout()
            if (verificationMode) {
                verificationMode = false
                expectedEnglishForVerification = null
                try { speechRecognizer?.stopListening() } catch (_: Exception) { }
                stopEnglishVoskRecording()
                setMicButtonAppearance(recording = false)
            }
            isEnglishMicActive = false
            isRecording = false
            if (threeColControlRunning && threeColRows.isNotEmpty()) {
                speakThreeColCurrent()
            } else {
                threeColTtsGeneration++
                expectedEnglishForVerification = null
            }
        }
    }

    /**
     * THREECOL_TABLE lessons: actionKey → asset path. Same layout, V tab, and **auto-scroll** ([updateThreeColAutoScrollPosition]) for all —
     * e.g. What, Where, How, When, Who, Why, Let, can, test_layout; only the .txt file differs.
     * To add a new similar lesson: (1) add entry here, (2) add Subtopic in DrawerTopicBuilders with this actionKey, (3) add .txt in assets.
     * Also add the actionKey to [SimpleSentenceUtils.SIMPLE_TXT_ACTION_KEYS_USING_THREE_COL_TABLE] so [SimpleSentenceUtils.buildSimpleSentenceSubtopics] does not create a duplicate SIMPLE_SENTENCE row for the same file.
     */
    private val threeColLessonAssetPaths: Map<String, String> = mapOf(
        "test_layout" to "Lessons/SVO/simple_what.txt",
        "simple_what" to "Lessons/SVO/simple_what.txt",
        "simple_where" to "Lessons/SVO/simple_where.txt",
        "simple_how" to "Lessons/SVO/simple_how.txt",
        "simple_let" to "Lessons/SVO/simple_let.txt",
        "simple_when" to "Lessons/SVO/simple_when.txt",
        "simple_who" to "Lessons/SVO/simple_who.txt",
        "simple_why" to "Lessons/SVO/simple_why.txt",
        "can" to "Lessons/SVO/can.txt",
        "may" to "Lessons/SVO/may.txt",
        "wish" to "Lessons/SVO/wish.txt",
        "how_about" to "Lessons/SVO/how_about.txt",
        "feels_like" to "Lessons/SVO/feels_like.txt",
        "need_to" to "Lessons/SVO/need_to.txt",
        "must" to "Lessons/SVO/must.txt",
        "should" to "Lessons/SVO/should.txt",
        "used_to" to "Lessons/SVO/used_to.txt",
        "make" to "Lessons/SVO/make.txt"
    )
    /** Lesson key for current 3col lesson; used for JSON stats file ({key}_3col_stats.json). Set when loading. */
    private var threeColCurrentLessonKey: String = "test_layout"
    private fun threeColLessonKey(): String = threeColCurrentLessonKey
    private var threeColCurrentIndex: Int = 0
    private var threeColIncorrectCount: Int = 0
    private var threeColControlRunning: Boolean = false
    private var threeColControlPaused: Boolean = false
    /** Auto-scroll: last focused row (smooth scroll-by-row, same idea as [tenseTripletLastScrolledPosition]). */
    private var threeColLastScrolledPosition: Int = -1
    /** Cached typical row height px (updated from measured row views). */
    private var threeColCachedRowHeightPx: Int = -1
    /** Bumped on each new 3-col TTS utterance / cancel so [UtteranceProgressListener] ignores stale callbacks after tab/filter/stop. */
    private var threeColTtsGeneration: Int = 0

    /** Tense triplets: rows parsed from Lessons/Tense/simple_tense.txt and shown in 3-column table style. */
    private var tenseTripletRows: List<TenseTripletRow> = emptyList()
    private var tenseTripletAdapter: TenseTripletAdapter? = null
    private enum class TenseTripletMode { LEARNING, PRACTICE, TEST, VOCAB }
    private var tenseTripletMode: TenseTripletMode = TenseTripletMode.LEARNING
    private var tenseTripletCurrentIndex: Int = 0
    private var tenseTripletControlRunning: Boolean = false
    private var tenseTripletControlPaused: Boolean = false
    private var tenseTripletBengaliFirst: Boolean = false
    private var tenseTripletAdjectiveDualMode: Boolean = false
    private var tenseTripletIncorrectCount: Int = 0
    private var tenseTripletShowPresent: Boolean = true
    private var tenseTripletShowPast: Boolean = true
    private var tenseTripletShowFuture: Boolean = true
    private var tenseTripletLastScrolledPosition: Int = -1
    private var tenseTripletCachedRowHeightPx: Int = -1
    /** Which inner layout is inflated into [R.id.lesson_base_content] for [ContentLayout.TENSE_TRIPLETS]. */
    private var tenseTripletInflatedContentRes: Int = -1

    /**
     * Conversation bubbles: actionKey → asset path. Same layout and V tab for all; only the .txt file differs.
     * To add a new similar lesson: (1) add entry here, (2) add a Subtopic in DrawerTopicBuilders with this actionKey.
     */
    private val conversationBubbleLessonAssetPaths: Map<String, String> = mapOf(
        "conv_bubble_first_meeting" to "Lessons/Conversation/conversation_1.txt",
        "conv_bubble_second_lesson" to "Lessons/Conversation/conversation_2.txt",
        "conv_bubble_third_lesson" to "Lessons/Conversation/conversation_3.txt",
        "conv_bubble_fourth_lesson" to "Lessons/Conversation/conversation_4.txt"
    )
    private var convBubbleCurrentLessonKey: String = ""
    private var convBubbleBaseRows: List<ConversationBubbleRow> = emptyList()
    private var convBubbleRows: List<ConversationBubbleRow> = emptyList()
    private var convBubbleStats: MutableList<IntArray> = mutableListOf()
    private var convBubbleDisplayToBaseIndex: List<Int> = emptyList()
    private var convBubbleMode: ConversationMode = ConversationMode.LEARNING
    private var convBubbleLearningMode: Boolean = true
    private var convBubbleWeakOnlyFilter: Boolean = false
    private var convBubbleCurrentIndex: Int = 0
    /** Incremented each time conversation bubble TTS starts; embedded in utterance ids so completion ignores stale callbacks. */
    /** Extend sentence: groups of progressive lines (English / Bengali / pronunciation). */
    private var extendSentenceGroups: List<List<ExtendSentenceRow>> = emptyList()
    private var extendSentenceHeaderAdapterPositions: IntArray = intArrayOf()
    private var extendSentenceCurrentGroupIndex: Int = 0
    private var extendSentenceAdapter: ExtendSentenceAdapter? = null
    private var extendSentenceRecycler: RecyclerView? = null
    private var extendSentenceControlRunning: Boolean = false
    private var extendSentenceControlPaused: Boolean = false
    /** Learning / Practice / Test / V for extend-sentence shell (same enum as bubbles / preposition). */
    private var extendSentenceMode: ConversationMode = ConversationMode.LEARNING
    private var extendSentenceRoot: View? = null
    /** Bumps each time extend-sentence group TTS starts; stale callbacks ignored. */
    private var extendSentenceTtsGeneration: Int = 0
    /** Practice: sentence index within current group; app speaks English then verifies user repeat. */
    private var extendSentencePracticeRowIndex: Int = 0
    private var extendSentencePracticeGen: Int = 0
    private var extendSentencePracticeIncorrectStreak: Int = 0
    /** TEST: user speaks from row index 1 onward; row 0 is model-only. */
    private var extendSentenceTestListeningRowIndex: Int = 1
    private var extendSentenceTestGen: Int = 0
    private var extendSentenceTestIncorrectStreak: Int = 0
    /** False until instruction TTS finishes and mic should start; used for pause/resume. */
    private var extendSentenceTestIntroCompleted: Boolean = false
    private val extendPracticePromptRegex = Regex("^extend_practice_(\\d+)_(\\d+)_(\\d+)$")
    private val extendTestUtteranceRegex = Regex("^extend_test_(\\d+)_(\\d+)_(only|first|instr)$")
    private val extendSentenceUtteranceSegmentRegex = Regex("^extend_sent_(\\d+)_(\\d+)_(en|bn|ex)_(\\d+)$")
    private val extendSentenceUtteranceEnStartRegex = Regex("^extend_sent_(\\d+)_(\\d+)_en_(\\d+)$")
    private val prepBlockHeadUtteranceRegex = Regex("^prep_block_(\\d+)_(\\d+)_head$")
    /** Preposition block lesson: each block has heading+meaning, 2 examples, and hidden spoken guidance. */
    private var prepositionBlockRows: List<PrepositionBlockRow> = emptyList()
    private var prepositionBlockCurrentIndex: Int = 0
    private var prepositionBlocksAdapter: PrepositionBlocksAdapter? = null
    private var prepositionBlocksRecycler: RecyclerView? = null
    /** Root of preposition lesson shell ([layout_lesson_base] + top extra + content). */
    private var prepositionBlocksRoot: View? = null
    private val prepositionBlockUtteranceRegex = Regex("^prep_block_(\\d+)_(\\d+)_(head|meaning|ex1|ex2|guidance)$")
    private var prepositionBlocksControlRunning: Boolean = false
    private var prepositionBlocksControlPaused: Boolean = false
    /** Bumps each time a preposition block speak sequence starts; stale TTS callbacks ignored. */
    private var prepositionBlocksTtsGeneration: Int = 0
    private var prepositionBlocksAutoAdvanceRunnable: Runnable? = null
    /** Learning / Practice / Test / V — same as conversation bubbles magenta bar. */
    private var prepositionBlocksMode: ConversationMode = ConversationMode.LEARNING

    private var convBubbleTtsGeneration: Int = 0
    private val convBubbleBengaliUtteranceRegex = Regex("^conv_bubble_bengali_(\\d+)_(\\d+)$")
    private val convBubbleEnglishUtteranceRegex = Regex("^conv_bubble_english_(\\d+)_(\\d+)$")
    private var convBubbleSessionCorrect: Int = 0
    private var convBubbleSessionAttempted: Int = 0
    private var convBubbleIncorrectCount: Int = 0
    private var convBubbleControlRunning: Boolean = false
    private var convBubbleControlPaused: Boolean = false
    /** Test mode: row index we are currently listening for (user's line). */
    private var convBubbleListeningForRowIndex: Int = -1
    /** Test mode: true = app speaks first; false = user speaks first. */
    private var convBubbleTestInitiatorApp: Boolean = true
    private var convBubbleAdapter: ConversationBubbleAdapter? = null
    /** Last adapter position we scrolled to (for smooth scroll-by-one-bubble in Learning). */
    private var convBubbleLastScrolledPosition: Int = -1
    /** Cached height of one bubble in px (measured after list is shown in Learning mode). */
    private var convBubbleCachedItemHeightPx: Int = -1
    /** Pending runnable for "advance to next bubble" after correct/3-fail; cancelled when user taps prev/next. */
    private var convBubbleAdvanceRunnable: Runnable? = null

    /** V tab: words from current lesson's embedded vocabulary block. */
    private var currentLessonVocabWords: List<String> = emptyList()
    /** V tab: rows (word, pronunciation, meaning) for current lesson. */
    private var lessonVocabRows: List<LessonVocabRow> = emptyList()
    /** Master word list (lazy): word lowercase -> (meaning, pronunciation). */
    private var masterWordListMap: Map<String, Pair<String, String>>? = null
    /** Adapter for V tab 3-column list; shared between conv and 3col layouts. */
    private var lessonVocabAdapter: LessonVocabAdapter? = null
    /** V tab: incorrect attempts for current word; advance to next after 3. */
    private var vocabIncorrectCount: Int = 0
    /** Per-word progress for master list: 0=never seen, 1=passed, 2=tested but failed. Loaded/saved to filesDir. */
    private var vocabularyProgress: MutableMap<String, Int> = mutableMapOf()
    /** V tab: rows actually shown (filtered by master then by need-test or all). Use this for current word / size in VOCAB. */
    private var currentVTabRows: List<LessonVocabRow> = emptyList()
    /** V tab: when false show only untested/failed (0,2); when true show all words in lesson (from master). */
    private var vocabShowAllWords: Boolean = false

    private val openLessonLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val content = InputStreamReader(stream).readText()
                val rows = parseLessonFile(content)
                if (rows.isEmpty()) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "No valid rows (need 4 columns per line)", Toast.LENGTH_SHORT).show() }
                    return@registerForActivityResult
                }
                val name = getDisplayName(uri)?.removeSuffix(".txt") ?: "lesson"
                runOnUiThread { clearPronunciationLessonState(); showModeSelectorDialog(rows, name) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Read lesson failed", e)
            runOnUiThread { Toast.makeText(this@MainActivity, "Read failed: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    /** Load introduction.txt (or any text file) from Downloads; show Bengali text and speak it naturally. */
    private val openIntroductionLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val content = InputStreamReader(stream, StandardCharsets.UTF_8).readText().trim()
                runOnUiThread {
                    showIntroductionContent(content)
                    if (content.isNotEmpty()) {
                        textToSpeech?.stop()
                        descriptionWebView.postDelayed({
                            speakIntroductionBengali(content)
                        }, 500)
                    } else {
                        Toast.makeText(this@MainActivity, "File is empty.", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: runOnUiThread {
                Toast.makeText(this@MainActivity, "Could not read file.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Read introduction failed", e)
            runOnUiThread { Toast.makeText(this@MainActivity, "Read failed: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun getDisplayName(uri: Uri): String? {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) return c.getString(i)
            }
        }
        return null
    }

    /**
     * Parse SVO lesson file: first line = topic (show in top row), then each line = "Bengali,English"
     * (first part before comma = upper box / Bengali meaning, second part = lower box / English meaning).
     * Returns (topic, rows). LessonRow: bnQ = upper box, engA = lower box.
     */
    private fun parseSvoLessonFile(content: String): Pair<String, List<LessonRow>> {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return "" to emptyList()
        val topic = lines[0]
        val rows = mutableListOf<LessonRow>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            val commaIndex = line.indexOf(',')
            if (commaIndex < 0) continue
            val firstPart = line.substring(0, commaIndex).trim()   // Bengali → upper box (bnQ)
            val secondPart = line.substring(commaIndex + 1).trim() // English → lower box (engA)
            if (firstPart.isNotBlank() && secondPart.isNotBlank()) {
                rows.add(LessonRow(secondPart, firstPart, secondPart, firstPart))
            }
        }
        return topic to rows
    }

    /** Parse pipe-separated lines into LessonRow. A|B|C|D = engQ|bnQ|engA|bnA. */
    private fun parseLessonFile(content: String): List<LessonRow> {
        val rows = mutableListOf<LessonRow>()
        content.lines().forEach { line ->
            val parts = line.split("|").map { it.trim() }
            if (parts.size >= 4) {
                rows.add(LessonRow(parts[0], parts[1], parts[2], parts[3]))
            }
        }
        return rows
    }

    /**
     * Parse verb lesson file (Regular_verbs.txt or Irregular_verbs.txt).
     * Both files use the same format: comma-separated, first line header.
     * Columns: Root (V1), Past (V2), Past Participle (V3), Bengali Meaning, Common Local Sentence (5th).
     * We use columns 0 (root) and 3 (Bengali meaning) for the lesson; 5th column is ignored.
     * Lesson flow: ask Bengali meaning (bnQ), user replies with English meaning (engA = root); compare and give feedback; 3 wrong → next word.
     */
    private fun parseVerbLessonFile(content: String): List<LessonRow> {
        val rows = mutableListOf<LessonRow>()
        val lines = content.lines()
        if (lines.size < 2) return rows
        for (i in 1 until lines.size) {
            val parts = lines[i].split(",").map { it.trim() }
            if (parts.size >= 4) {
                val root = parts[0]
                val bengali = parts[3]
                if (root.isNotBlank() && bengali.isNotBlank()) {
                    rows.add(LessonRow(root, bengali, root, bengali))
                }
            }
        }
        return rows
    }

    private fun showModeSelectorDialog(rows: List<LessonRow>, name: String) {
        val modes = arrayOf(
            getString(R.string.mode_1_title) + "\n" + getString(R.string.mode_1_desc),
            getString(R.string.mode_2_title) + "\n" + getString(R.string.mode_2_desc),
            getString(R.string.mode_3_title) + "\n" + getString(R.string.mode_3_desc)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_mode_title))
            .setItems(modes) { _, which ->
                clearPronunciationLessonState()
                lessonRows = rows
                lessonName = name
                incorrectLessonRows.clear()
                incorrectLessonSourceName = null
                lessonCorrectCount = 0
                lessonMode = which + 1
                lessonIndex = 0
                lessonPhase = "q"
                lessonMode3Listening = false
                lessonMode3SpokeAnswer = false
                lessonIncorrectCount = 0
                nextButton?.isEnabled = true
                skipButton?.isEnabled = true
                clearBothTextAreas()
                setSentenceListVisibility(false)
                updateLessonStatistic()
                updateLessonTopicDisplay()
                showVerbDiagram(verbForLessonDiagram(name))
                textView.text = getString(R.string.lesson_loaded)
                Toast.makeText(this, getString(R.string.lesson_loaded), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Show verb picker (DO, HAVE, GO); on select build lesson and start mode 4 (hear Bengali, say English). */
    private fun showVerbSelectorDialog() {
        val verbs = getVerbData().keys.toList().sorted()
        if (verbs.isEmpty()) {
            Toast.makeText(this, "No verbs defined", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_verb_title))
            .setItems(verbs.toTypedArray()) { _, which ->
                val verbName = verbs[which]
                val rows = buildLessonFromVerb(verbName)
                if (rows.isEmpty()) return@setItems
                clearPronunciationLessonState()
                lessonRows = rows
                lessonName = "verb_$verbName"
                incorrectLessonRows.clear()
                incorrectLessonSourceName = null
                lessonCorrectCount = 0
                lessonMode = 4
                lessonIndex = 0
                lessonPhase = "q"
                lessonMode3Listening = false
                lessonMode3SpokeAnswer = false
                lessonIncorrectCount = 0
                nextButton?.isEnabled = true
                skipButton?.isEnabled = true
                clearBothTextAreas()
                updateLessonStatistic()
                showVerbDiagram(verbName)
                textView.text = getString(R.string.lesson_loaded)
                Toast.makeText(this, getString(R.string.lesson_loaded), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request one permission at a time (Android allows only one request at a time)
        requestNextNeededPermission()

        proficiencyPrefs = getSharedPreferences("proficiency", MODE_PRIVATE)
        onCreateSpotter()
        setupUI()
        setupDrawer()
        initTextToSpeech()
        initTranslator()
        // When app loads, clear all text areas and sentence list
        clearBothTextAreas()
        clearSentenceListUi()

        // POC Home screen: show a small tile-based topic menu on first open.
        // This does not change how lessons work; drawer navigation still loads lessons the same way.
        val homeTopics = buildPocHomeTopics()
        val homeView = switchContentLayout(ContentLayout.POC_BUTTON_MENU)
        setupPocHomeTopics(homeView, homeTopics)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_parts_of_speech -> {
                loadReferenceHtmlPage("parts-of-speech.html", getString(R.string.parts_of_speech_title))
                return true
            }
            R.id.action_svo_sentences -> {
                loadReferenceHtmlPage("svo-sentences.html", getString(R.string.svo_sentences_title))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        // When user returns to the app (from another app or home), clear all visible text and list
        clearBothTextAreas()
        clearSentenceListUi()
    }

    /** Request the first permission that is not yet granted. */
    private fun requestNextNeededPermission() {
        for (p in INITIAL_PERMISSIONS) {
            if (!checkForPermission(p)) {
                ActivityCompat.requestPermissions(this, arrayOf(p), PERMISSION_REQUEST_CODE)
                return
            }
        }
    }

    /** Check using Activity context so "While using this app" is recognized. */
    private fun checkForPermission(permission: String): Boolean {
        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "checkForPermission($permission)=$granted")
        return granted
    }

    private fun onPermissionResult(permission: String, granted: Boolean) {
        Log.d(TAG, "onPermissionResult($permission)=$granted")
        if (granted && permission == RECORD_AUDIO) {
            Toast.makeText(this, "Microphone allowed", Toast.LENGTH_SHORT).show()
        } else if (!granted && permission == RECORD_AUDIO) {
            // Only show "denied" for mic; Storage/Internet are optional and can be denied on startup
            Toast.makeText(this, "Microphone permission denied; enable it in Settings to use Start Microphone.", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST_CODE || grantResults.isEmpty() || permissions.isEmpty()) return
        // We request one permission at a time, so there is only one result
        val permission = permissions[0]
        val granted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        onPermissionResult(permission, granted)
        // If granted, ask for the next missing permission (or no-op if all granted)
        if (granted) requestNextNeededPermission()
    }


    enum class ModelStatus {
        MODEL_STATUS_INIT,
        MODEL_STATUS_START,
        MODEL_STATUS_READY
    }

    private lateinit var recognizer: Recognizer

    private var modelStatus = ModelStatus.MODEL_STATUS_INIT

    private var audioRecord: AudioRecord? = null
    private lateinit var micButton: ImageButton
    private lateinit var textView: TextView
    private lateinit var englishTextView: TextView
    private lateinit var lessonStatTextView: TextView
    private var lessonTopicText: TextView? = null
    private var topBarPrevLesson: ImageButton? = null
    private var topBarNextLesson: ImageButton? = null
    /** Set when user opens a lesson from the drawer; used for top-bar prev/next lesson. */
    private var currentDrawerActionKey: String? = null
    /** Navigation drawer */
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerList: ListView
    private lateinit var proficiencyPrefs: SharedPreferences
    /** Content area: switchable layouts */
    private lateinit var contentFrame: FrameLayout
    /** Which content layout is currently shown */
    private var currentContentLayout: ContentLayout = ContentLayout.LEGACY

    /** Text-display TTS state: idle / speaking / paused */
    private enum class TtsPlayState { IDLE, SPEAKING, PAUSED }
    private var ttsPlayState = TtsPlayState.IDLE
    private var textDisplaySpeakButton: ImageButton? = null
    private var textDisplayBodyText: String = ""
    /** Segments for resume-from-pause */
    private var introSegments: List<String> = emptyList()
    private var introSegmentIndex: Int = 0   // index of segment currently being spoken
    /** Reference to the active speech-input layout view (null when not using that layout) */
    private var speechInputView: View? = null
    /** True when speech-input layout is using system SpeechRecognizer for English continuous listening */
    private var speechInputEnglishListening = false
    /** Practice 3-area layout state */
    private var practiceView: View? = null
    private var practiceWordList: List<Pair<String, String>> = emptyList()  // (Bengali, English)
    private var practiceWordIndex = 0
    private var practiceListening = false
    // ── Interactive Table Mode state ──
    private var tableInteractiveWebView: WebView? = null
    private var tableInteractiveRows: List<List<String>> = emptyList()
    private var tableInteractiveIndex: Int = 0
    private var tableInteractiveSpeakCol: Int = 1   // column whose text is spoken
    private var tableInteractiveMatchCol: Int = 1   // column to match user speech against
    private var tableInteractiveLocale: Locale = Locale("bn")       // locale for speech recognition (listening)
    private var tableInteractiveSpeakLocale: Locale = Locale.US    // locale for TTS (speaking)
    private var tableInteractiveListening = false
    private var tableInteractiveActive = false
    private var tableInteractiveMaxRetries = 3
    private var tableInteractiveRetryCount = 0
    private var tableInteractiveNoMatchCount = 0
    private var tableInteractiveMaxNoMatch = 3
    private var tableInteractiveCorrectCount = 0   // how many rows answered correctly so far
    private var tableInteractiveTestedCount = 0    // how many rows tested so far (correct + failed)
    private var tableInteractiveSpeaking = false   // guard: true while TTS is speaking the letter
    private var tablePageFinishedFired = false      // guard: only trigger from onPageFinished once
    private var tableListeningStartTime = 0L        // timestamp when listening started (for early-error guard)
    private var tableSpeechRecognizer: SpeechRecognizer? = null
    private lateinit var translationLabel: TextView
    private lateinit var descriptionWebView: WebView
    private lateinit var descriptionSpeakerButton: ImageButton
    /** Hidden instruction paragraph for the description area; spoken when user taps the speaker icon. */
    private var descriptionInstructionText: String? = null
    private var descriptionInstructionLocale: Locale? = null
    /** Pronunciation lesson: 3-column table (Word, Pronunciation, Meaning). When set, lecture button speaks each word in sequence. */
    private var pronunciationLessonRows: List<List<String>>? = null
    private var pronunciationLessonTitle: String? = null
    /** When true, lecture flow is speak word → listen → check (3 attempts per word) → next. */
    private var pronunciationPracticeActive = false
    private var pronunciationPracticeWordIndex = 0
    private var pronunciationPracticeAttempt = 0
    /** Guard: only process one recognition result per listen in pronunciation practice. */
    private var pronunciationPracticeResultHandled = false
    /** Word just spoken by TTS; when onDone fires we start listening for this word. */
    private var pendingPronunciationPracticeWord: String? = null
    private lateinit var inputLanguageGroup: RadioGroup
    private lateinit var sentenceRecyclerView: RecyclerView
    private val sentenceList = mutableListOf<Sentence>()
    private lateinit var sentenceAdapter: SentenceAdapter
    private var currentNextIndex = 0
    /** Wrong-answer count for current SVO sentence; after 3 we move to next. */
    private var svoSentenceStrikes = 0
    private var nextButton: ImageButton? = null
    private var skipButton: ImageButton? = null
    private var recordingThread: Thread? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var translator: Translator? = null
    private var translatorEnToBn: Translator? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    /** Vosk Indian English recognizer; loaded on first use. */
    private var voskEnInRecognizer: VoskEnInRecognizer? = null
    private var englishAudioRecord: AudioRecord? = null
    private var englishVoskRecordingThread: Thread? = null
    private var lastUtteranceWavFile: File? = null
    private var lastUtteranceMediaPlayer: MediaPlayer? = null
    /** When true, we're waiting for user to speak English to verify against expected translation. */
    @Volatile
    private var verificationMode = false
    private var expectedEnglishForVerification: String? = null
    /** Set before speaking Bengali for verification; when TTS onDone fires we start mic. */
    private var pendingVerificationExpectedEnglish: String? = null
    /** Guard: only process one verification result per listening session (avoids double-counting). */
    @Volatile
    private var verificationResultHandled = false
    // Last non-empty partial/final heard while verification is active; used as fallback on timeout.
    private var verificationLastHeardText: String = ""
    /** When non-null, TTS onDone("incorrect_then_correct") will speak this word (correct pronunciation). */
    private var pendingSpeakCorrectWordAfterIncorrect: String? = null
    /** After speaking correct word, restart listening with this expected (so user can try again without pressing Next). */
    private var pendingRestartVerificationWith: String? = null
    /** For mode 4 incorrect: speak this Bengali again then start listening (don't clear text areas). */
    private var pendingBengaliAfterIncorrect: String? = null
    /** Fallback: speak correct word if onDone does not fire. */
    private var incorrectFeedbackFallbackRunnable: Runnable? = null
    /** Fallback: start mic after "Try again" if TTS onDone("try_again_then_listen") does not fire. */
    private var tryAgainListenFallbackRunnable: Runnable? = null
    private val tryAgainListenFallbackDelayMs = 2500L
    /** Lazy so Handler is not created during Activity <init> (avoids getMainLooper() on not-yet-ready context on some devices). */
    private val verificationHandler by lazy { Handler(Looper.getMainLooper()) }
    private var verificationTimeoutRunnable: Runnable? = null
    @Volatile
    private var currentRecordingIsBengali = true

    private val channelConfig = AudioFormat.CHANNEL_IN_MONO

    // Note: We don't use AudioFormat.ENCODING_PCM_FLOAT
    // since the AudioRecord.read(float[]) needs API level >= 23
    // but we are targeting API level >= 21
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var idx: Int = 0
    private var lastText: String = ""


    @Volatile
    private var isRecording: Boolean = false

    /** True when we are listening with SpeechRecognizer (English input) rather than Sherpa (Bengali). */
    @Volatile
    private var isEnglishMicActive = false

    @Volatile
    private var modelLoaded = false

    private fun onCreateSpotter() {

        recognizer = Recognizer()

        CoroutineScope(Dispatchers.IO).launch {
            modelStatus = ModelStatus.MODEL_STATUS_START
            try {
                withContext(Dispatchers.IO) {
                    recognizer.initModel(this@MainActivity)
                }
                modelLoaded = true
                Log.d(TAG, "Done creating model!!!")
                runOnUiThread {
                    modelStatus = ModelStatus.MODEL_STATUS_READY
                    micButton.isEnabled = true
                    val rows = lessonRows
                    if (rows != null && lessonMode == 4 && lessonIndex in rows.indices) {
                        val r = rows[lessonIndex]
                        val upperBox = findViewById<TextView>(R.id.my_text)
                        upperBox.setBackgroundColor(Color.WHITE)
                        upperBox.setTextColor(Color.BLACK)
                        upperBox.text = r.bnQ
                    } else if (rows == null) {
                        // Only upper box (textView) is set here; englishTextView is never touched, so only Bengali disappears when this ran unconditionally.
                        textView.text = getText(R.string.ready_to_start)
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Model load failed", e)
                runOnUiThread {
                    modelStatus = ModelStatus.MODEL_STATUS_INIT
                    micButton.isEnabled = false
                    textView.text = "Model failed to load:\n${e.message}\n\nCheck that native .so files are in jniLibs."
                    Toast.makeText(this@MainActivity, "Model load failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupUI() {
        contentFrame = findViewById(R.id.content_frame)

        micButton = findViewById(R.id.mic_button)
        micButton.setOnClickListener {
            Log.i(TAG, "MIC_BUTTON_CLICK (listener entered)")
            onDemo()
        }

        textView = findViewById(R.id.my_text)
        textView.movementMethod = ScrollingMovementMethod()

        englishTextView = findViewById(R.id.english_text)
        englishTextView.movementMethod = ScrollingMovementMethod()

        lessonStatTextView = findViewById(R.id.lesson_stat_text)
        lessonTopicText = findViewById(R.id.lesson_topic_text)
        topBarPrevLesson = findViewById(R.id.top_bar_prev_lesson)
        topBarNextLesson = findViewById(R.id.top_bar_next_lesson)
        topBarPrevLesson?.setOnClickListener { navigateAdjacentDrawerLesson(-1) }
        topBarNextLesson?.setOnClickListener { navigateAdjacentDrawerLesson(1) }
        updateLessonTopicDisplay()

        translationLabel = findViewById(R.id.translation_label)
        descriptionWebView = findViewById(R.id.description_webview)
        descriptionWebView.settings.javaScriptEnabled = false
        descriptionWebView.setBackgroundColor(0)
        descriptionWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        descriptionWebView.webViewClient = object : WebViewClient() {
            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null && url.startsWith("letter://")) {
                    val letter = url.removePrefix("letter://").take(1).uppercase()
                    if (letter.isNotEmpty()) {
                        val bengaliPronunciation = alphabetBengaliPronunciation[letter]
                        if (bengaliPronunciation != null) {
                            textToSpeech?.setLanguage(Locale("bn"))
                            textToSpeech?.speak(bengaliPronunciation, TextToSpeech.QUEUE_FLUSH, null, "letter_pronunciation")
                            Toast.makeText(this@MainActivity, "$letter → $bengaliPronunciation", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return true
                }
                if (url != null && url.startsWith("word://")) {
                    val encoded = url.removePrefix("word://")
                    val word = try {
                        URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
                    } catch (_: Exception) {
                        encoded
                    }
                    if (word.isNotEmpty()) {
                        textToSpeech?.setLanguage(Locale.US)
                        textToSpeech?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "pronunciation_word")
                        Toast.makeText(this@MainActivity, word, Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                return false
            }
        }
        descriptionSpeakerButton = findViewById(R.id.description_speaker_button)
        descriptionSpeakerButton.setOnClickListener {
            val text = descriptionInstructionText
            if (!text.isNullOrEmpty()) {
                val locale = descriptionInstructionLocale ?: Locale("bn")
                textToSpeech?.setLanguage(locale)
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "description_instruction")
                Toast.makeText(this, getString(R.string.speak_instruction), Toast.LENGTH_SHORT).show()
            }
        }
        inputLanguageGroup = findViewById(R.id.input_language_group)
        updateTranslationLabelAndButton()
        inputLanguageGroup.setOnCheckedChangeListener { _, _ -> updateTranslationLabelAndButton() }

        sentenceRecyclerView = findViewById(R.id.sentence_list)
        sentenceRecyclerView.layoutManager = LinearLayoutManager(this)
        sentenceAdapter = SentenceAdapter(sentenceList) { position -> removeSentenceAt(position) }
        sentenceRecyclerView.adapter = sentenceAdapter
        ItemTouchHelper(sentenceDragCallback).attachToRecyclerView(sentenceRecyclerView)

        findViewById<ImageButton>(R.id.save_list_button).setOnClickListener { showSaveListDialog() }
        findViewById<ImageButton>(R.id.load_list_button).setOnClickListener { showLoadListDialog() }
        controlActionsBar = findViewById(R.id.control_actions_include)
        // Bind from the activity-level bar so START/STOP work when this bar is shown (content_frame may contain another include with same ids)
        controlStartStopButton = controlActionsBar?.findViewById(R.id.control_start_stop)
        controlPauseResumeButton = controlActionsBar?.findViewById(R.id.control_pause_resume)
        controlPlaybackLastButton = controlActionsBar?.findViewById(R.id.control_playback_last)
        controlActionsBar?.visibility = View.GONE
        setupHoldToRecordPlaybackButton(controlPlaybackLastButton)
        bindControlBarListeners()
        nextButton = findViewById(R.id.next_button)
        nextButton?.setOnClickListener {
            if (currentContentLayout == ContentLayout.SV_RIBBON && svRibbonBengali != null) {
                moveSvRibbonNext()
                return@setOnClickListener
            }
            if (lessonRows != null) onNextLessonStep() else onNextSentence()
        }
        skipButton = findViewById(R.id.skip_button)
        skipButton?.setOnClickListener { onSkipWord() }
        findViewById<ImageButton>(R.id.speak_english_button).setOnClickListener { onSpeakEnglishButton() }

        if (modelStatus == ModelStatus.MODEL_STATUS_INIT || modelStatus == ModelStatus.MODEL_STATUS_START) {
            micButton.isEnabled = false
            textView.text = getText(R.string.hint)
        } else {
            micButton.isEnabled = true
            if (lessonRows == null) textView.text = getText(R.string.ready_to_start)
        }
        setMicButtonAppearance(recording = false)
    }

    private fun setupHoldToRecordPlaybackButton(btn: ImageButton?) {
        btn ?: return
        // Press-and-hold to record; release to playback immediately.
        btn.isEnabled = true
        btn.alpha = 1f
        btn.isClickable = true
        btn.isFocusable = true
        btn.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    btn.isPressed = true
                    btn.alpha = 1f
                    btn.scaleX = 0.96f
                    btn.scaleY = 0.96f
                    btn.translationY = (2f * resources.displayMetrics.density)
                    val pauseBtn = (btn.parent as? View)?.findViewById<Button>(R.id.control_pause_resume)
                    startHoldToRecordPlayback(pauseBtn)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    btn.isPressed = false
                    btn.alpha = 1f
                    btn.scaleX = 1f
                    btn.scaleY = 1f
                    btn.translationY = 0f
                    stopHoldToRecordPlaybackAndPlay()
                    true
                }
                else -> false
            }
        }
    }

    /** Attach Start/Stop and Pause/Resume click logic to the current control bar buttons. Call after rebinding controlStartStopButton/controlPauseResumeButton so the visible bar always has the same behavior. */
    private fun bindControlBarListeners() {
        controlStartStopButton?.setOnClickListener {
            when {
                currentContentLayout == ContentLayout.SIMPLE_SENTENCE && lessonRows != null -> onSimpleSentenceStartStop()
                currentContentLayout == ContentLayout.SV_RIBBON && svRibbonBengali != null -> onSvRibbonStartStop()
                currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletMode == TenseTripletMode.VOCAB && lessonVocabRows.isNotEmpty() -> onVocabStartStop()
                currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletRows.isNotEmpty() -> onTenseTripletStartStop()
                currentContentLayout == ContentLayout.CONVERSATION_BUBBLES && convBubbleMode == ConversationMode.VOCAB && lessonVocabRows.isNotEmpty() -> onVocabStartStop()
                currentContentLayout == ContentLayout.CONVERSATION_BUBBLES && convBubbleRows.isNotEmpty() -> onConvBubbleStartStop()
                currentContentLayout == ContentLayout.THREECOL_TABLE && threeColMode == ThreeColMode.VOCAB && lessonVocabRows.isNotEmpty() -> onVocabStartStop()
                currentContentLayout == ContentLayout.THREECOL_TABLE && threeColRows.isNotEmpty() -> onThreeColStartStop()
                currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceMode == ConversationMode.VOCAB && lessonVocabRows.isNotEmpty() -> onVocabStartStop()
                currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceGroups.isNotEmpty() -> onExtendSentenceStartStop()
                currentContentLayout == ContentLayout.PREPOSITION_BLOCKS && prepositionBlockRows.isNotEmpty() -> onPrepositionBlocksStartStop()
                else -> dispatchControlStartStop()
            }
        }
        controlPauseResumeButton?.setOnClickListener {
            when {
                currentContentLayout == ContentLayout.SIMPLE_SENTENCE && lessonRows != null -> onSimpleSentencePauseResume()
                currentContentLayout == ContentLayout.SV_RIBBON && svRibbonBengali != null -> onSvRibbonPauseResume()
                currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletMode == TenseTripletMode.VOCAB -> { /* V tab: no pause/resume */ }
                currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletRows.isNotEmpty() -> onTenseTripletPauseResume()
                currentContentLayout == ContentLayout.CONVERSATION_BUBBLES && convBubbleMode == ConversationMode.VOCAB -> { /* V tab: no pause/resume for POC */ }
                currentContentLayout == ContentLayout.CONVERSATION_BUBBLES && convBubbleRows.isNotEmpty() -> onConvBubblePauseResume()
                currentContentLayout == ContentLayout.THREECOL_TABLE && threeColMode == ThreeColMode.VOCAB -> { /* V tab: no pause/resume for POC */ }
                currentContentLayout == ContentLayout.THREECOL_TABLE && threeColRows.isNotEmpty() -> onThreeColPauseResume()
                currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceMode == ConversationMode.VOCAB -> { /* V tab */ }
                currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceGroups.isNotEmpty() -> onExtendSentencePauseResume()
                currentContentLayout == ContentLayout.PREPOSITION_BLOCKS && prepositionBlockRows.isNotEmpty() -> onPrepositionBlocksPauseResume()
                else -> dispatchControlPauseResume()
            }
        }
    }

    /** Update the topic bar text from current lesson/sentence list name. */
    private fun updateLessonTopicDisplay() {
        lessonTopicText?.text = when {
            lessonName != null -> lessonName
            pronunciationLessonTitle != null -> pronunciationLessonTitle
            sentenceList.isNotEmpty() -> getString(R.string.svo_topic_title)
            else -> getString(R.string.lesson_topic_hint)
        }
        updateTopBarLessonNavButtons()
    }

    private fun updateTopBarLessonNavButtons() {
        val all = DrawerTopicBuilders.getAllSubtopicsInNavigationOrder(assets)
        val key = currentDrawerActionKey
        val idx = if (key != null) all.indexOfFirst { it.actionKey == key } else -1
        topBarPrevLesson?.isEnabled = idx > 0
        topBarNextLesson?.isEnabled = idx >= 0 && idx < all.lastIndex
    }

    /** Prev/next lesson in the same order as Level 1 + main topic list (see DrawerTopicBuilders). */
    private fun navigateAdjacentDrawerLesson(delta: Int) {
        val all = DrawerTopicBuilders.getAllSubtopicsInNavigationOrder(assets)
        val key = currentDrawerActionKey
        if (key == null) {
            Toast.makeText(this, getString(R.string.lesson_nav_no_context), Toast.LENGTH_SHORT).show()
            return
        }
        val idx = all.indexOfFirst { it.actionKey == key }
        if (idx < 0) {
            Toast.makeText(this, getString(R.string.lesson_nav_no_context), Toast.LENGTH_SHORT).show()
            return
        }
        val n = idx + delta
        if (n !in all.indices) {
            Toast.makeText(this, getString(if (delta < 0) R.string.lesson_nav_first else R.string.lesson_nav_last), Toast.LENGTH_SHORT).show()
            return
        }
        val sub = all[n]
        drawerLayout.closeDrawers()
        if (sub.layoutType != ContentLayout.LEGACY) switchContentLayout(sub.layoutType)
        else if (currentContentLayout != ContentLayout.LEGACY) switchContentLayout(ContentLayout.LEGACY)
        handleSubtopicAction(sub)
    }

    /** Show or hide the sentence list area (for SVO: list of sentences to translate to Bengali). No-op when legacy layout not in content (e.g. SIMPLE_SENTENCE). */
    private fun setSentenceListVisibility(visible: Boolean) {
        findViewById<View>(R.id.sentence_list_label)?.visibility = if (visible) View.VISIBLE else View.GONE
        sentenceRecyclerView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /** Sets mic button icon and color: blue for Start (mic), red for Stop. */
    private fun setMicButtonAppearance(recording: Boolean) {
        if (recording) {
            micButton.setImageResource(R.drawable.ic_stop)
            micButton.setColorFilter(ContextCompat.getColor(this, R.color.mic_color_stop))
        } else {
            micButton.setImageResource(R.drawable.ic_mic)
            micButton.setColorFilter(ContextCompat.getColor(this, R.color.mic_color_start))
        }
        // Update speech-input layout status if active
        updateSpeechInputStatus(recording)
    }

    /** Update the status indicator in the speech-input layout. */
    private fun updateSpeechInputStatus(listening: Boolean) {
        val view = speechInputView ?: return
        if (currentContentLayout != ContentLayout.SPEECH_INPUT) return
        val statusText = view.findViewById<TextView>(R.id.speech_status_text) ?: return
        val statusDot = view.findViewById<View>(R.id.speech_status_dot) ?: return
        if (listening) {
            statusText.text = "Listening…"
            statusDot.background?.setTint(0xFFE53935.toInt()) // red dot
        } else {
            statusText.text = "Ready"
            statusDot.background?.setTint(0xFF4CAF50.toInt()) // green dot
        }
    }

    /** Offline Bengali Text-to-Speech: uses system TTS with Bengali language (works offline if Bengali voice is installed). */
    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Slightly slower rate and default pitch can sound less robotic; punctuation in text helps prosody (?, !, .)
                textToSpeech?.setSpeechRate(0.92f)  // 1.0 = normal; 0.85–0.95 often sounds calmer and clearer
                textToSpeech?.setPitch(1.0f)      // 1.0 = normal
                val bengaliLocale = Locale("bn")
                val availability = textToSpeech?.isLanguageAvailable(bengaliLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                val statusMessage = when (availability) {
                    TextToSpeech.LANG_AVAILABLE -> "Bengali voice: Available (offline)"
                    TextToSpeech.LANG_COUNTRY_AVAILABLE -> "Bengali voice: Available (offline)"
                    TextToSpeech.LANG_MISSING_DATA -> "Bengali voice: Not installed – install in Settings → Text-to-speech"
                    TextToSpeech.LANG_NOT_SUPPORTED -> "Bengali voice: Not supported on this device"
                    else -> "Bengali voice: Available (offline)" // LANG_COUNTRY_DEFAULT (API 21+) or other positive
                }
                Log.d(TAG, "TTS Bengali: $statusMessage")
                val result = textToSpeech?.setLanguage(bengaliLocale)
                when (result) {
                    TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                        Log.w(TAG, "Bengali TTS not available; trying default locale")
                        textToSpeech?.setLanguage(Locale.getDefault())
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, statusMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                    else -> {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, statusMessage, Toast.LENGTH_SHORT).show()
                        }
                        Log.d(TAG, "Bengali TTS ready (offline)")
                    }
                }
                ttsReady = true
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        if (utteranceId != null) {
                            runOnUiThread { handleUtteranceStartBlockHighlight(utteranceId) }
                        }
                    }
                    override fun onDone(utteranceId: String?) {
                        when (utteranceId) {
                            "bengali_verification" -> {
                                runOnUiThread {
                                    val expected = pendingVerificationExpectedEnglish
                                    pendingVerificationExpectedEnglish = null
                                    if (expected != null && !isDestroyed) startVerificationListening(expected)
                                }
                            }
                            "lesson_verify_bengali" -> {
                                runOnUiThread {
                                    val eng = pendingSimpleSentenceEnglishForLearning
                                    pendingSimpleSentenceEnglishForLearning = null
                                    if (!eng.isNullOrBlank() && !isDestroyed && ttsReady && textToSpeech != null) {
                                        textToSpeech?.setLanguage(Locale.US)
                                        textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(eng), TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
                                    } else {
                                        val expected = expectedEnglishForVerification
                                        if (expected != null && !isDestroyed) startVerificationListening(expected)
                                    }
                                }
                            }
                            "sv_ribbon_learning_bengali" -> {
                                runOnUiThread {
                                    val expected = expectedEnglishForVerification
                                    if (!expected.isNullOrBlank() && !isDestroyed && ttsReady && textToSpeech != null) {
                                        textToSpeech?.setLanguage(Locale.US)
                                        textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(expected), TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
                                    } else if (expected != null && !isDestroyed) {
                                        startVerificationListening(expected)
                                    }
                                }
                            }
                            "sv_ribbon_practice_bengali" -> {
                                runOnUiThread {
                                    val expected = expectedEnglishForVerification
                                    if (!expected.isNullOrBlank() && !isDestroyed && ttsReady && textToSpeech != null) {
                                        textToSpeech?.setLanguage(Locale.US)
                                        textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(expected), TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
                                    } else if (expected != null && !isDestroyed) {
                                        startVerificationListening(expected)
                                    }
                                }
                            }
                            "tense_triplet_p_eng" -> {
                                runOnUiThread {
                                    if (!tenseTripletControlRunning || currentContentLayout != ContentLayout.TENSE_TRIPLETS || tenseTripletRows.isEmpty()) return@runOnUiThread
                                    val row = tenseTripletRows.getOrNull(tenseTripletCurrentIndex.coerceIn(0, tenseTripletRows.lastIndex)) ?: return@runOnUiThread
                                    if (tenseTripletShowPresent) {
                                        textToSpeech?.setLanguage(Locale("bn"))
                                        textToSpeech?.speak(row.present.bengali, TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_p_bn")
                                    } else if (tenseTripletShowPast) {
                                        if (tenseTripletMode == TenseTripletMode.TEST) {
                                            textToSpeech?.setLanguage(Locale("bn"))
                                            textToSpeech?.speak(row.past.bengali, TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_pa_bn")
                                        } else {
                                            textToSpeech?.setLanguage(Locale.US)
                                            textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(row.past.english), TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_pa_eng")
                                        }
                                    } else if (tenseTripletShowFuture) {
                                        if (tenseTripletMode == TenseTripletMode.TEST) {
                                            textToSpeech?.setLanguage(Locale("bn"))
                                            textToSpeech?.speak(row.future.bengali, TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_f_bn")
                                        } else {
                                            textToSpeech?.setLanguage(Locale.US)
                                            textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(row.future.english), TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_f_eng")
                                        }
                                    } else {
                                        val expected = expectedEnglishForVerification
                                        if (!expected.isNullOrBlank()) startVerificationListening(expected)
                                    }
                                }
                            }
                            "tense_triplet_p_bn" -> {
                                runOnUiThread {
                                    if (!tenseTripletControlRunning || currentContentLayout != ContentLayout.TENSE_TRIPLETS || tenseTripletRows.isEmpty()) return@runOnUiThread
                                    if (tenseTripletAdjectiveDualMode) {
                                        val expected = expectedEnglishForVerification
                                        if (!expected.isNullOrBlank()) startVerificationListening(expected)
                                        return@runOnUiThread
                                    }
                                    val row = tenseTripletRows.getOrNull(tenseTripletCurrentIndex.coerceIn(0, tenseTripletRows.lastIndex)) ?: return@runOnUiThread
                                    if (tenseTripletShowPast) {
                                        if (tenseTripletMode == TenseTripletMode.TEST) {
                                            textToSpeech?.setLanguage(Locale("bn"))
                                            textToSpeech?.speak(row.past.bengali, TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_pa_bn")
                                        } else {
                                            textToSpeech?.setLanguage(Locale.US)
                                            textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(row.past.english), TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_pa_eng")
                                        }
                                    } else if (tenseTripletShowFuture) {
                                        if (tenseTripletMode == TenseTripletMode.TEST) {
                                            textToSpeech?.setLanguage(Locale("bn"))
                                            textToSpeech?.speak(row.future.bengali, TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_f_bn")
                                        } else {
                                            textToSpeech?.setLanguage(Locale.US)
                                            textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(row.future.english), TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_f_eng")
                                        }
                                    } else {
                                        val expected = expectedEnglishForVerification
                                        if (!expected.isNullOrBlank()) startVerificationListening(expected)
                                    }
                                }
                            }
                            "tense_triplet_pa_eng" -> {
                                runOnUiThread {
                                    if (!tenseTripletControlRunning || currentContentLayout != ContentLayout.TENSE_TRIPLETS || tenseTripletRows.isEmpty()) return@runOnUiThread
                                    if (tenseTripletAdjectiveDualMode) {
                                        val expected = expectedEnglishForVerification
                                        if (!expected.isNullOrBlank()) startVerificationListening(expected)
                                        return@runOnUiThread
                                    }
                                    val row = tenseTripletRows.getOrNull(tenseTripletCurrentIndex.coerceIn(0, tenseTripletRows.lastIndex)) ?: return@runOnUiThread
                                    if (tenseTripletShowPast) {
                                        textToSpeech?.setLanguage(Locale("bn"))
                                        textToSpeech?.speak(row.past.bengali, TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_pa_bn")
                                    } else if (tenseTripletShowFuture) {
                                        if (tenseTripletMode == TenseTripletMode.TEST) {
                                            textToSpeech?.setLanguage(Locale("bn"))
                                            textToSpeech?.speak(row.future.bengali, TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_f_bn")
                                        } else {
                                            textToSpeech?.setLanguage(Locale.US)
                                            textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(row.future.english), TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_f_eng")
                                        }
                                    } else {
                                        val expected = expectedEnglishForVerification
                                        if (!expected.isNullOrBlank()) startVerificationListening(expected)
                                    }
                                }
                            }
                            "tense_triplet_pa_bn" -> {
                                runOnUiThread {
                                    if (!tenseTripletControlRunning || currentContentLayout != ContentLayout.TENSE_TRIPLETS || tenseTripletRows.isEmpty()) return@runOnUiThread
                                    val row = tenseTripletRows.getOrNull(tenseTripletCurrentIndex.coerceIn(0, tenseTripletRows.lastIndex)) ?: return@runOnUiThread
                                    if (tenseTripletShowFuture) {
                                        if (tenseTripletMode == TenseTripletMode.TEST) {
                                            textToSpeech?.setLanguage(Locale("bn"))
                                            textToSpeech?.speak(row.future.bengali, TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_f_bn")
                                        } else {
                                            textToSpeech?.setLanguage(Locale.US)
                                            textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(row.future.english), TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_f_eng")
                                        }
                                    } else {
                                        val expected = expectedEnglishForVerification
                                        if (!expected.isNullOrBlank()) startVerificationListening(expected)
                                    }
                                }
                            }
                            "tense_triplet_f_eng" -> {
                                runOnUiThread {
                                    if (!tenseTripletControlRunning || currentContentLayout != ContentLayout.TENSE_TRIPLETS || tenseTripletRows.isEmpty()) return@runOnUiThread
                                    val row = tenseTripletRows.getOrNull(tenseTripletCurrentIndex.coerceIn(0, tenseTripletRows.lastIndex)) ?: return@runOnUiThread
                                    if (tenseTripletShowFuture) {
                                        textToSpeech?.setLanguage(Locale("bn"))
                                        textToSpeech?.speak(row.future.bengali, TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_f_bn")
                                    } else {
                                        val expected = expectedEnglishForVerification
                                        if (!expected.isNullOrBlank()) startVerificationListening(expected)
                                    }
                                }
                            }
                            "tense_triplet_f_bn" -> {
                                runOnUiThread {
                                    if (!tenseTripletControlRunning || currentContentLayout != ContentLayout.TENSE_TRIPLETS) return@runOnUiThread
                                    val expected = expectedEnglishForVerification
                                    if (!expected.isNullOrBlank()) startVerificationListening(expected)
                                }
                            }
                            "lesson_verify" -> {
                                runOnUiThread {
                                    if (!isDestroyed && lessonMode == 4 && currentContentLayout == ContentLayout.LEGACY) {
                                        val rows = lessonRows
                                        val idx = lessonIndex
                                        if (rows != null && idx in rows.indices) {
                                            val upperBox = findViewById<TextView>(R.id.my_text)
                                            upperBox.setBackgroundColor(Color.WHITE)
                                            upperBox.setTextColor(Color.BLACK)
                                            upperBox.text = rows[idx].bnQ
                                        }
                                    }
                                    val expected = expectedEnglishForVerification
                                    if (expected != null && !isDestroyed) startVerificationListening(expected)
                                }
                            }
                            "incorrect_then_sentence" -> {
                                runOnUiThread {
                                    incorrectFeedbackFallbackRunnable?.let { verificationHandler.removeCallbacks(it) }
                                    incorrectFeedbackFallbackRunnable = null
                                    val bengali = pendingBengaliAfterIncorrect
                                    val toRestart = pendingRestartVerificationWith
                                    pendingBengaliAfterIncorrect = null
                                    if (bengali != null && toRestart != null && !isDestroyed && ttsReady && textToSpeech != null) {
                                        expectedEnglishForVerification = toRestart
                                        textToSpeech?.setLanguage(Locale("bn"))
                                        textToSpeech?.speak(bengali, TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
                                    } else if (toRestart != null && !isDestroyed) {
                                        pendingRestartVerificationWith = null
                                        startVerificationListening(toRestart)
                                    }
                                }
                            }
                            "incorrect_then_correct" -> {
                                runOnUiThread {
                                    incorrectFeedbackFallbackRunnable?.let { verificationHandler.removeCallbacks(it) }
                                    incorrectFeedbackFallbackRunnable = null
                                    val word = pendingSpeakCorrectWordAfterIncorrect
                                    if (!word.isNullOrBlank() && !isDestroyed && ttsReady && textToSpeech != null) {
                                        textToSpeech?.setLanguage(Locale.US)
                                        textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(word), TextToSpeech.QUEUE_ADD, null, "correct_word_then_try_again")
                                    } else {
                                        pendingSpeakCorrectWordAfterIncorrect = null
                                        val toRestart = pendingRestartVerificationWith
                                        if (!toRestart.isNullOrBlank() && !isDestroyed) {
                                            pendingRestartVerificationWith = null
                                            startVerificationListening(toRestart)
                                        }
                                    }
                                }
                            }
                            "correct_word_then_try_again" -> {
                                runOnUiThread {
                                    pendingSpeakCorrectWordAfterIncorrect = null
                                    if (!isDestroyed && ttsReady && textToSpeech != null) {
                                        tryAgainListenFallbackRunnable?.let { verificationHandler.removeCallbacks(it) }
                                        tryAgainListenFallbackRunnable = Runnable {
                                            tryAgainListenFallbackRunnable = null
                                            val toRestart = pendingRestartVerificationWith
                                            if (!toRestart.isNullOrBlank() && !isDestroyed) {
                                                pendingRestartVerificationWith = null
                                                startVerificationListening(toRestart)
                                            }
                                        }
                                        verificationHandler.postDelayed(tryAgainListenFallbackRunnable!!, tryAgainListenFallbackDelayMs)
                                        textToSpeech?.setLanguage(Locale.US)
                                        textToSpeech?.speak(getString(R.string.try_again), TextToSpeech.QUEUE_ADD, null, "try_again_then_listen")
                                    } else {
                                        val toRestart = pendingRestartVerificationWith
                                        if (!toRestart.isNullOrBlank() && !isDestroyed) {
                                            pendingRestartVerificationWith = null
                                            startVerificationListening(toRestart)
                                        }
                                    }
                                }
                            }
                            "try_again_then_listen" -> {
                                runOnUiThread {
                                    tryAgainListenFallbackRunnable?.let { verificationHandler.removeCallbacks(it) }
                                    tryAgainListenFallbackRunnable = null
                                    val toRestart = pendingRestartVerificationWith
                                    pendingRestartVerificationWith = null
                                    if (!toRestart.isNullOrBlank() && !isDestroyed) startVerificationListening(toRestart)
                                }
                            }
                            "pronunciation_practice_word" -> {
                                runOnUiThread {
                                    val expected = pendingPronunciationPracticeWord
                                    pendingPronunciationPracticeWord = null
                                    if (expected != null && pronunciationPracticeActive && !isDestroyed) {
                                        pronunciationPracticeResultHandled = false
                                        startVerificationListening(expected)
                                    }
                                }
                            }
                            "pronunciation_practice_try_again" -> {
                                runOnUiThread {
                                    val expected = pendingPronunciationPracticeWord
                                    pendingPronunciationPracticeWord = null
                                    if (expected != null && pronunciationPracticeActive && !isDestroyed) {
                                        pronunciationPracticeResultHandled = false
                                        startVerificationListening(expected)
                                    }
                                }
                            }
                            "intro_done" -> {
                                runOnUiThread {
                                    textToSpeech?.setSpeechRate(0.92f)
                                    ttsPlayState = TtsPlayState.IDLE
                                    introSegmentIndex = 0
                                    updateSpeakButtonIcon()
                                }
                            }
                        }
                        // 3-col table: ids are threecol_{learning|practice}_bengali_{generation} — stale callbacks ignored via [threeColTtsGeneration].
                        if (utteranceId != null && utteranceId.startsWith("threecol_learning_bengali_")) {
                            runOnUiThread { handleThreeColLearningBengaliUtteranceDone(utteranceId) }
                        }
                        if (utteranceId != null && utteranceId.startsWith("threecol_practice_bengali_")) {
                            runOnUiThread { handleThreeColPracticeBengaliUtteranceDone(utteranceId) }
                        }
                        // Conversation bubble TTS: ids are conv_bubble_{bengali|english}_{rowIdx}_{generation} — handle outside when() so stale callbacks are ignored.
                        if (utteranceId != null && utteranceId.startsWith("conv_bubble_bengali_")) {
                            runOnUiThread { handleConvBubbleBengaliUtteranceFinished(utteranceId) }
                        }
                        if (utteranceId != null && utteranceId.startsWith("conv_bubble_english_")) {
                            runOnUiThread { handleConvBubbleEnglishUtteranceFinished(utteranceId) }
                        }
                        if (utteranceId != null && utteranceId.startsWith("prep_block_")) {
                            runOnUiThread { handlePrepositionBlockUtteranceFinished(utteranceId, fromError = false) }
                        }
                        if (utteranceId != null && utteranceId.startsWith("extend_sent_")) {
                            runOnUiThread { handleExtendSentenceUtteranceDoneForHighlight(utteranceId) }
                        }
                        if (utteranceId != null && utteranceId.startsWith("extend_practice_")) {
                            runOnUiThread { handleExtendSentencePracticePromptDone(utteranceId) }
                        }
                        if (utteranceId != null && utteranceId.startsWith("extend_test_")) {
                            runOnUiThread { handleExtendSentenceTestUtteranceDone(utteranceId) }
                        }
                        // Track segment progress for resume: "intro_0", "intro_1", etc.
                        if (utteranceId != null && utteranceId.startsWith("intro_") && utteranceId != "intro_done") {
                            val idx = utteranceId.removePrefix("intro_").toIntOrNull()
                            if (idx != null) {
                                introSegmentIndex = idx + 1  // next segment to speak on resume
                            }
                        }
                    }
                    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                    override fun onError(utteranceId: String?) {
                        if (utteranceId != null && utteranceId.startsWith("intro_")) {
                            runOnUiThread {
                                textToSpeech?.setSpeechRate(0.92f)
                                ttsPlayState = TtsPlayState.IDLE
                                updateSpeakButtonIcon()
                            }
                        }
                        if (utteranceId == "pronunciation_practice_word" || utteranceId == "pronunciation_practice_try_again") {
                            runOnUiThread {
                                pendingPronunciationPracticeWord = null
                                if (pronunciationPracticeActive) {
                                    pronunciationPracticeResultHandled = false
                                    val rows = pronunciationLessonRows
                                    val idx = pronunciationPracticeWordIndex
                                    if (rows != null && idx in rows.indices) {
                                        val word = rows[idx].getOrNull(0)?.trim()
                                        if (!word.isNullOrEmpty()) startVerificationListening(word)
                                    }
                                }
                            }
                        }
                        if (utteranceId == "sv_ribbon_learning_bengali") {
                            runOnUiThread {
                                val expected = expectedEnglishForVerification
                                if (!expected.isNullOrBlank() && !isDestroyed && ttsReady && textToSpeech != null) {
                                    textToSpeech?.setLanguage(Locale.US)
                                    textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(expected), TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
                                } else if (expected != null && !isDestroyed) {
                                    startVerificationListening(expected)
                                }
                            }
                        }
                        if (utteranceId == "sv_ribbon_practice_bengali") {
                            runOnUiThread {
                                val expected = expectedEnglishForVerification
                                if (!expected.isNullOrBlank() && !isDestroyed && ttsReady && textToSpeech != null) {
                                    textToSpeech?.setLanguage(Locale.US)
                                    textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(expected), TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
                                } else if (expected != null && !isDestroyed) {
                                    startVerificationListening(expected)
                                }
                            }
                        }
                        if (utteranceId != null && utteranceId.startsWith("threecol_learning_bengali_")) {
                            runOnUiThread { handleThreeColLearningBengaliUtteranceDone(utteranceId) }
                        }
                        if (utteranceId != null && utteranceId.startsWith("threecol_practice_bengali_")) {
                            runOnUiThread { handleThreeColPracticeBengaliUtteranceDone(utteranceId) }
                        }
                        if (utteranceId != null && utteranceId.startsWith("conv_bubble_bengali_")) {
                            runOnUiThread { handleConvBubbleBengaliUtteranceFinished(utteranceId) }
                        }
                        if (utteranceId != null && utteranceId.startsWith("conv_bubble_english_")) {
                            runOnUiThread { handleConvBubbleEnglishUtteranceFinished(utteranceId) }
                        }
                        if (utteranceId != null && utteranceId.startsWith("prep_block_")) {
                            runOnUiThread {
                                handlePrepositionBlockUtteranceFinished(utteranceId, fromError = true)
                                prepositionBlocksAdapter?.clearHighlight()
                            }
                        }
                        if (utteranceId != null && utteranceId.startsWith("extend_sent_")) {
                            runOnUiThread { extendSentenceAdapter?.clearBlockHighlight() }
                        }
                        if (utteranceId != null && utteranceId.startsWith("extend_practice_")) {
                            runOnUiThread {
                                cancelExtendSentencePracticeVerification()
                                extendSentenceAdapter?.clearBlockHighlight()
                            }
                        }
                        if (utteranceId != null && utteranceId.startsWith("extend_test_")) {
                            runOnUiThread {
                                cancelExtendSentencePracticeVerification()
                                extendSentenceAdapter?.clearBlockHighlight()
                            }
                        }
                        if (utteranceId == "lesson_verify_bengali") {
                            runOnUiThread {
                                pendingSimpleSentenceEnglishForLearning = null
                                val expected = expectedEnglishForVerification
                                if (expected != null && !isDestroyed) startVerificationListening(expected)
                            }
                        }
                        if (utteranceId == "bengali_verification" || utteranceId == "lesson_verify" || utteranceId == "incorrect_then_sentence" ||
                            utteranceId == "incorrect_then_correct" || utteranceId == "correct_word_then_try_again" || utteranceId == "try_again_then_listen") {
                            runOnUiThread {
                                pendingVerificationExpectedEnglish = null
                                if (utteranceId == "lesson_verify") expectedEnglishForVerification = null
                                if (utteranceId == "incorrect_then_sentence") pendingBengaliAfterIncorrect = null
                                if (utteranceId == "incorrect_then_correct" || utteranceId == "correct_word_then_try_again") pendingSpeakCorrectWordAfterIncorrect = null
                                if (utteranceId == "incorrect_then_sentence") {
                                    pendingBengaliAfterIncorrect = null
                                    val toRestart = pendingRestartVerificationWith
                                    if (!toRestart.isNullOrBlank() && !isDestroyed) {
                                        pendingRestartVerificationWith = null
                                        startVerificationListening(toRestart)
                                    }
                                }
                                if (utteranceId == "correct_word_then_try_again" || utteranceId == "try_again_then_listen") {
                                    tryAgainListenFallbackRunnable?.let { verificationHandler.removeCallbacks(it) }
                                    tryAgainListenFallbackRunnable = null
                                    val toRestart = pendingRestartVerificationWith
                                    pendingRestartVerificationWith = null
                                    if (!toRestart.isNullOrBlank() && !isDestroyed) startVerificationListening(toRestart)
                                }
                                if (!isDestroyed) Toast.makeText(this@MainActivity, "Speech failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                })
            } else {
                Log.e(TAG, "TTS init failed: $status")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Text-to-speech failed to initialize", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** Bengali→English and English→Bengali translators (ML Kit, offline after model download). */
    private fun initTranslator() {
        val bnToEn = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.BENGALI)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        translator = Translation.getClient(bnToEn)
        val enToBn = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.BENGALI)
            .build()
        translatorEnToBn = Translation.getClient(enToBn)
        Log.d(TAG, "Translators (Bn↔En) created")
    }

    private fun translateBengaliToEnglish() {
        translateBengaliToEnglishInternal(showToasts = true)
    }

    /** Translates current Bengali text to English and shows it. Optionally speaks English (used by button and by auto flow). */
    private fun translateBengaliToEnglishInternal(showToasts: Boolean) {
        val trans = translator
        if (trans == null) {
            if (showToasts) Toast.makeText(this, "Translator not ready", Toast.LENGTH_SHORT).show()
            return
        }
        val bengaliText = textView.text.toString().trim()
        if (bengaliText.isEmpty()) {
            if (showToasts) Toast.makeText(this, "No Bengali text to translate. Capture speech first.", Toast.LENGTH_SHORT).show()
            return
        }
        if (showToasts) Toast.makeText(this, "Translating… (downloading model if needed)", Toast.LENGTH_SHORT).show()
        trans.downloadModelIfNeeded()
            .addOnSuccessListener {
                trans.translate(bengaliText)
                    .addOnSuccessListener { translated ->
                        runOnUiThread {
                            englishTextView.text = translated
                            if (showToasts) Toast.makeText(this@MainActivity, "Translated", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Translated: $translated")
                            speakEnglishText()
                        }
                    }
                    .addOnFailureListener { e ->
                        runOnUiThread {
                            if (showToasts) Toast.makeText(this@MainActivity, "Translation failed: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e(TAG, "Translation failed", e)
                        }
                    }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    if (showToasts) Toast.makeText(this@MainActivity, "Model download failed. Try with internet: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Model download failed", e)
                }
            }
    }

    /** Called when mic detects end of speech (and we stop): translate full text and speak English. */
    private fun translateAndSpeakEnglish() {
        translateBengaliToEnglishInternal(showToasts = false)
    }

    /** Manual translate: English (textView) → Bengali (englishTextView), then speak Bengali. */
    private fun translateEnglishToBengali() {
        val englishText = textView.text.toString().trim()
        if (englishText.isEmpty()) {
            Toast.makeText(this, "No English text to translate. Speak or type first.", Toast.LENGTH_SHORT).show()
            return
        }
        val trans = translatorEnToBn ?: return
        Toast.makeText(this, "Translating… (downloading model if needed)", Toast.LENGTH_SHORT).show()
        trans.downloadModelIfNeeded()
            .addOnSuccessListener {
                trans.translate(englishText)
                    .addOnSuccessListener { bengaliText ->
                        runOnUiThread {
                            textView.text = bengaliText
                            englishTextView.text = englishText
                            Toast.makeText(this@MainActivity, "Translated", Toast.LENGTH_SHORT).show()
                            speakBengaliString(bengaliText)
                        }
                    }
                    .addOnFailureListener { e ->
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Translation failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Download model (internet needed): ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /** Translate current sentence (segment) to English, append to display, and speak it. Mic keeps running. */
    private fun addSentenceToList(text: String, isBengali: Boolean) {
        if (text.isBlank()) return
        runOnUiThread {
            sentenceList.add(Sentence(text.trim(), isBengali))
            sentenceAdapter.notifyItemInserted(sentenceList.size - 1)
            sentenceRecyclerView.scrollToPosition(sentenceList.size - 1)
        }
    }

    private fun translateSegmentAndSpeakEnglish(bengaliSegment: String) {
        if (bengaliSegment.isBlank()) return
        addSentenceToList(bengaliSegment, isBengali = true)
        val trans = translator ?: return
        trans.downloadModelIfNeeded()
            .addOnSuccessListener {
                trans.translate(bengaliSegment)
                    .addOnSuccessListener { translated ->
                        runOnUiThread {
                            if (!isDestroyed) {
                                val current = englishTextView.text.toString().trim()
                                englishTextView.text = if (current.isEmpty()) translated else "$current\n$translated"
                                speakEnglishString(translated)
                                Log.d(TAG, "Segment translated and spoken: $translated")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        runOnUiThread {
                            Log.e(TAG, "Segment translation failed", e)
                        }
                    }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    Log.e(TAG, "Segment model download failed", e)
                }
            }
    }

    /** Speak a single English string (uses QUEUE_ADD so multiple sentences can queue). */
    /** Speak the current English sentence (e.g. when user taps the speaker icon next to English translation). */
     private fun onSpeakEnglishButton() {
        val toSpeak = when {
             lessonRows != null && lessonIndex in lessonRows!!.indices -> lessonRows!![lessonIndex].engA
                                       !expectedEnglishForVerification.isNullOrBlank() -> expectedEnglishForVerification!!
            else -> englishTextView.text.toString().trim()
        }
        if (toSpeak.isNotBlank()) speakEnglishString(toSpeak)
        else Toast.makeText(this, getString(R.string.speak_english), Toast.LENGTH_SHORT).show()
    }

    private fun speakEnglishString(englishText: String) {
        if (englishText.isBlank() || !ttsReady || textToSpeech == null) return
        textToSpeech?.setLanguage(Locale.US)
        textToSpeech?.speak(englishText, TextToSpeech.QUEUE_ADD, null, "english_segment_tts")
    }

    private fun speakBengaliText() {
        if (!ttsReady || textToSpeech == null) {
            Toast.makeText(this, "Speech not ready yet", Toast.LENGTH_SHORT).show()
            return
        }
        val text = textView.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "No text to speak. Capture speech with Start Microphone first.", Toast.LENGTH_SHORT).show()
            return
        }
        textToSpeech?.setLanguage(Locale("bn"))
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bengali_tts")
        Log.d(TAG, "Speaking Bengali: $text")
    }

    /** Speak the English translation using TTS (offline if English voice is on device). */
    private fun speakEnglishText() {
        if (!ttsReady || textToSpeech == null) {
            Toast.makeText(this, "Speech not ready yet", Toast.LENGTH_SHORT).show()
            return
        }
        val text = englishTextView.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "No English text to speak. Translate to English first.", Toast.LENGTH_SHORT).show()
            return
        }
        textToSpeech?.setLanguage(Locale.US)
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "english_tts")
        Log.d(TAG, "Speaking English: $text")
    }

    /** Speak a single Bengali string (e.g. after English→Bengali translation). */
    private fun speakBengaliString(bengaliText: String) {
        if (bengaliText.isBlank() || !ttsReady || textToSpeech == null) return
        textToSpeech?.setLanguage(Locale("bn"))
        textToSpeech?.speak(bengaliText, TextToSpeech.QUEUE_ADD, null, "bengali_segment_tts")
    }

    private fun startEnglishMic() {
        if (!checkForPermission(RECORD_AUDIO)) {
            Toast.makeText(this, "Microphone permission required – please allow when prompted", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, arrayOf(RECORD_AUDIO), PERMISSION_REQUEST_CODE)
            return
        }
        if (currentContentLayout == ContentLayout.LEGACY) {
            clearBothTextAreas()
            englishTextView.invalidate()
            englishTextView.requestLayout()
        }
        setMicButtonAppearance(recording = true)
        isRecording = true
        isEnglishMicActive = true
        verificationMode = false
        CoroutineScope(Dispatchers.IO).launch {
            val recognizer = voskEnInRecognizer ?: VoskEnInRecognizer(this@MainActivity).also { voskEnInRecognizer = it }
            val ready = recognizer.ensureModelReady()
            withContext(Dispatchers.Main) {
                if (!isEnglishMicActive || isDestroyed) return@withContext
                if (!ready) {
                    isEnglishMicActive = false
                    isRecording = false
                    setMicButtonAppearance(recording = false)
                    Toast.makeText(this@MainActivity, "Vosk Indian English model failed to load", Toast.LENGTH_LONG).show()
                    return@withContext
                }
                startEnglishVoskRecording()
            }
        }
        Log.i(TAG, "Started English mic (Vosk)")
    }

    private fun stopEnglishMic() {
        if (!isEnglishMicActive) return
        stopEnglishVoskRecording()
        isEnglishMicActive = false
        isRecording = false
        setMicButtonAppearance(recording = false)
        if (currentContentLayout == ContentLayout.LEGACY) {
            clearBothTextAreas()
            clearSentenceListUi()
        }
        Log.i(TAG, "Stopped English mic")
    }

    // ───────────────────── Playback (used by hold-to-record) ─────────────────────
    private fun stopLastUtterancePlayback() {
        lastUtteranceMediaPlayer?.let { mp ->
            try { mp.stop() } catch (_: Exception) {}
            try { mp.release() } catch (_: Exception) {}
        }
        lastUtteranceMediaPlayer = null
    }

    private fun playLastUtteranceRecording() {
        val file = lastUtteranceWavFile
        if (file == null || !file.exists()) {
            Toast.makeText(this, "No recording available yet.", Toast.LENGTH_SHORT).show()
            return
        }
        stopLastUtterancePlayback()

        val mp = MediaPlayer()
        lastUtteranceMediaPlayer = mp
        try {
            mp.setDataSource(file.absolutePath)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener {
                try { it.release() } catch (_: Exception) {}
                if (lastUtteranceMediaPlayer === it) lastUtteranceMediaPlayer = null
                if (playbackHoldResumeAfterPlayback) {
                    playbackHoldResumeAfterPlayback = false
                    autoResumeAfterPlaybackHoldIfNeeded()
                }
            }
            mp.setOnErrorListener { mpErr, _, _ ->
                try { mpErr.release() } catch (_: Exception) {}
                if (lastUtteranceMediaPlayer === mpErr) lastUtteranceMediaPlayer = null
                if (playbackHoldResumeAfterPlayback) {
                    playbackHoldResumeAfterPlayback = false
                    autoResumeAfterPlaybackHoldIfNeeded()
                }
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            try { mp.release() } catch (_: Exception) {}
            lastUtteranceMediaPlayer = null
            Toast.makeText(this, "Playback failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeWavPcm16Mono(outFile: File, pcm16le: ByteArray, sampleRate: Int) {
        // WAV header (PCM16 mono):
        // RIFF chunk size = 36 + dataSize
        // byteRate = sampleRate * channels * bits/8
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcm16le.size
        val riffSize = 36 + dataSize

        val header = ByteArray(44)
        fun putString(offset: Int, s: String) {
            for (i in s.indices) header[offset + i] = s[i].code.toByte()
        }
        fun putIntLE(offset: Int, v: Int) {
            header[offset] = (v and 0xFF).toByte()
            header[offset + 1] = ((v shr 8) and 0xFF).toByte()
            header[offset + 2] = ((v shr 16) and 0xFF).toByte()
            header[offset + 3] = ((v shr 24) and 0xFF).toByte()
        }
        fun putShortLE(offset: Int, v: Int) {
            header[offset] = (v and 0xFF).toByte()
            header[offset + 1] = ((v shr 8) and 0xFF).toByte()
        }

        putString(0, "RIFF")
        putIntLE(4, riffSize)
        putString(8, "WAVE")
        putString(12, "fmt ")
        putIntLE(16, 16) // fmt chunk size
        putShortLE(20, 1) // audioFormat = 1 (PCM)
        putShortLE(22, channels)
        putIntLE(24, sampleRate)
        putIntLE(28, byteRate)
        putShortLE(32, blockAlign)
        putShortLE(34, bitsPerSample)
        putString(36, "data")
        putIntLE(40, dataSize)

        FileOutputStream(outFile).use { fos ->
            fos.write(header)
            fos.write(pcm16le)
            fos.flush()
        }
    }

    // ───────────────────── Hold-to-record Playback Button ─────────────────────
    private fun autoPauseForPlaybackHoldIfNeeded(pauseBtn: Button?) {
        playbackHoldAutoPaused = false
        playbackHoldAutoPausedByClick = false
        playbackHoldPausedLayout = null
        playbackHoldPauseButtonToToggle = null
        // CONVERSATION_BUBBLES uses its own control bar in content but is not in usesControlActions; still handle it.
        if (!usesControlActions(currentContentLayout) && currentContentLayout != ContentLayout.CONVERSATION_BUBBLES) return

        when (currentContentLayout) {
            ContentLayout.SIMPLE_SENTENCE -> {
                if (simpleSentenceControlRunning && !simpleSentenceControlPaused) {
                    onSimpleSentencePauseResume()
                    playbackHoldAutoPaused = true
                }
            }
            ContentLayout.SV_RIBBON -> {
                if (svRibbonControlRunning && !svRibbonControlPaused) {
                    onSvRibbonPauseResume()
                    playbackHoldAutoPaused = true
                }
            }
            ContentLayout.THREECOL_TABLE -> {
                if (threeColControlRunning && !threeColControlPaused) {
                    onThreeColPauseResume()
                    playbackHoldAutoPaused = true
                }
            }
            ContentLayout.CONVERSATION_BUBBLES -> {
                if (convBubbleControlRunning && !convBubbleControlPaused) {
                    onConvBubblePauseResume()
                    playbackHoldAutoPaused = true
                }
            }
            else -> {
                // Fallback: trigger the real Pause/Resume click so the UI toggles to Resume.
                // This covers layouts that use the shared control bar but don't have dedicated paused flags here.
                val btn = pauseBtn ?: controlPauseResumeButton
                if (btn != null && btn.isShown && btn.isEnabled) {
                    btn.performClick()
                    playbackHoldAutoPaused = true
                    playbackHoldAutoPausedByClick = true
                    playbackHoldPauseButtonToToggle = btn
                }
            }
        }
        if (playbackHoldAutoPaused) {
            playbackHoldPausedLayout = currentContentLayout
            // Ensure UI reflects the paused state immediately.
            when (currentContentLayout) {
                ContentLayout.SIMPLE_SENTENCE -> updateSimpleSentenceControlBar()
                ContentLayout.SV_RIBBON -> updateSvRibbonControlBar()
                ContentLayout.THREECOL_TABLE -> updateThreeColControlBar()
                ContentLayout.CONVERSATION_BUBBLES -> updateConvBubbleControlBar()
                else -> {}
            }
        }
    }

    private fun autoResumeAfterPlaybackHoldIfNeeded() {
        if (!playbackHoldAutoPaused) return
        val pausedLayout = playbackHoldPausedLayout
        val pausedByClick = playbackHoldAutoPausedByClick
        val btnToToggle = playbackHoldPauseButtonToToggle
        playbackHoldAutoPaused = false
        playbackHoldAutoPausedByClick = false
        playbackHoldPausedLayout = null
        playbackHoldPauseButtonToToggle = null

        // Only resume if user is still on the same layout.
        if (pausedLayout == null || pausedLayout != currentContentLayout) return

        when (currentContentLayout) {
            ContentLayout.SIMPLE_SENTENCE -> if (simpleSentenceControlPaused) onSimpleSentencePauseResume()
            ContentLayout.SV_RIBBON -> if (svRibbonControlPaused) onSvRibbonPauseResume()
            ContentLayout.THREECOL_TABLE -> if (threeColControlPaused) onThreeColPauseResume()
            ContentLayout.CONVERSATION_BUBBLES -> if (convBubbleControlPaused) onConvBubblePauseResume()
            else -> {
                // Fallback: press Resume (same button) only if we paused via click.
                if (pausedByClick) {
                    val btn = btnToToggle ?: controlPauseResumeButton
                    if (btn != null && btn.isShown && btn.isEnabled) btn.performClick()
                }
            }
        }
        // Refresh UI after resume.
        when (currentContentLayout) {
            ContentLayout.SIMPLE_SENTENCE -> updateSimpleSentenceControlBar()
            ContentLayout.SV_RIBBON -> updateSvRibbonControlBar()
            ContentLayout.THREECOL_TABLE -> updateThreeColControlBar()
            ContentLayout.CONVERSATION_BUBBLES -> updateConvBubbleControlBar()
            else -> {}
        }
    }

    private fun startHoldToRecordPlayback(pauseBtn: Button?) {
        if (playbackHoldRecording) return
        if (!checkForPermission(RECORD_AUDIO)) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
            return
        }

        // If something is running, auto-pause it (so Pause button becomes Resume).
        autoPauseForPlaybackHoldIfNeeded(pauseBtn)
        playbackHoldResumeAfterPlayback = playbackHoldAutoPaused

        // Stop any ongoing speech/listening/playback so the user's recording is clean.
        textToSpeech?.stop()
        cancelVerificationTimeout()
        verificationMode = false
        expectedEnglishForVerification = null
        pendingRestartVerificationWith = null
        pendingSpeakCorrectWordAfterIncorrect = null
        pendingBengaliAfterIncorrect = null
        try { speechRecognizer?.cancel() } catch (_: Exception) {}
        try { speechRecognizer?.stopListening() } catch (_: Exception) {}
        stopEnglishVoskRecording()
        setMicButtonAppearance(recording = false)

        stopLastUtterancePlayback()
        playbackHoldPcm = ByteArrayOutputStream()

        val sampleRate = playbackHoldSampleRate
        val bufferSizeBytes = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).let {
            if (it == AudioRecord.ERROR || it == AudioRecord.ERROR_BAD_VALUE) sampleRate * 2 else it
        }.coerceAtLeast(2048)

        val rec = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSizeBytes)
        } catch (_: Exception) {
            null
        }
        if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) {
            try { rec?.release() } catch (_: Exception) {}
            playbackHoldAudioRecord = null
            playbackHoldPcm = null
            Toast.makeText(this, "Audio capture failed", Toast.LENGTH_SHORT).show()
            return
        }

        playbackHoldAudioRecord = rec
        playbackHoldRecording = true
        try { rec.startRecording() } catch (_: Exception) {}

        playbackHoldThread = thread(start = true) {
            val out = playbackHoldPcm ?: return@thread
            val buf = ShortArray((bufferSizeBytes / 2).coerceAtLeast(800))
            try {
                while (playbackHoldRecording) {
                    val read = rec.read(buf, 0, buf.size)
                    if (read <= 0) continue
                    for (i in 0 until read) {
                        val s = buf[i].toInt()
                        out.write(s and 0xFF)
                        out.write((s shr 8) and 0xFF)
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { rec.stop() } catch (_: Exception) {}
                try { rec.release() } catch (_: Exception) {}
            }
        }
    }

    private fun stopHoldToRecordPlaybackAndPlay() {
        if (!playbackHoldRecording) return
        playbackHoldRecording = false

        val t = playbackHoldThread
        playbackHoldThread = null
        val pcmStream = playbackHoldPcm
        playbackHoldPcm = null
        playbackHoldAudioRecord = null

        Thread {
            try { t?.join(1500) } catch (_: Exception) {}
            val bytes = pcmStream?.toByteArray() ?: ByteArray(0)
            if (bytes.isEmpty()) return@Thread
            val outFile = File(filesDir, "last_utterance.wav")
            try {
                writeWavPcm16Mono(outFile, bytes, playbackHoldSampleRate)
                lastUtteranceWavFile = outFile
                runOnUiThread {
                    // Resume after playback completes (only if we auto-paused).
                    playbackHoldResumeAfterPlayback = playbackHoldResumeAfterPlayback && playbackHoldAutoPaused
                    playLastUtteranceRecording()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (playbackHoldResumeAfterPlayback) autoResumeAfterPlaybackHoldIfNeeded()
                    playbackHoldResumeAfterPlayback = false
                }
            }
        }.start()
    }

    /** Start AudioRecord + thread that feeds Vosk Indian English recognizer. Call on main thread after Vosk is ready. */
    private fun startEnglishVoskRecording() {
        // Ensure no previous session is still running (avoid two threads reading same AudioRecord -> releaseBuffer crash)
        stopEnglishVoskRecording()
        val recognizer = voskEnInRecognizer ?: return
        val sampleRate = recognizer.getSampleRate()
        val numBytes = max(
            (0.2 * sampleRate).toInt(),
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).let { if (it == AudioRecord.ERROR || it == AudioRecord.ERROR_BAD_VALUE) 2 * sampleRate else it }
        ) * 2
        @SuppressLint("MissingPermission")
        englishAudioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                numBytes * 2
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "English AudioRecord SecurityException", e)
            null
        } ?: try {
            AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, numBytes * 2)
        } catch (e: SecurityException) {
            Log.e(TAG, "English AudioRecord MIC SecurityException", e)
            null
        }
        if (englishAudioRecord == null || englishAudioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            englishAudioRecord?.release()
            englishAudioRecord = null
            isEnglishMicActive = false
            isRecording = false
            setMicButtonAppearance(recording = false)
            Toast.makeText(this, "Failed to open microphone for English", Toast.LENGTH_LONG).show()
            return
        }
        recognizer.reset()
        englishAudioRecord?.startRecording()
        englishVoskRecordingThread = thread(start = true) { processEnglishVoskSamples() }
        Log.d(TAG, "English Vosk recording thread started")
    }

    /** Stop English Vosk recording thread. Stops AudioRecord first to unblock read(), then signals thread to exit. */
    private fun stopEnglishVoskRecording() {
        isEnglishMicActive = false
        isRecording = false
        // Stop AudioRecord to unblock any blocking read() call in the recording thread
        try { englishAudioRecord?.stop() } catch (_: Exception) {}
        // Don't block UI thread - let the recording thread exit on its own
        val t = englishVoskRecordingThread
        englishVoskRecordingThread = null
        if (t != null) {
            Thread { try { t.join(2000) } catch (_: Exception) {} }.start()
        }
    }

    /** Silence timeout in ms: if in verification mode and we have partial text but no final for this long, treat as answer. */
    private val verificationSilenceTimeoutMs = 1400L

    /** Runs on background thread: read from englishAudioRecord, feed Vosk, post results to main. This thread must stop/release AudioRecord on exit to avoid "releaseBuffer: mUnreleased out of range" native crash. */
    private fun processEnglishVoskSamples() {
        val recognizer = voskEnInRecognizer ?: return
        val sampleRate = recognizer.getSampleRate()
        // Use 0.5s chunks so Vosk has enough audio to produce partials (small models often need more context)
        val interval = 0.5
        val bufferSize = (interval * sampleRate).toInt().coerceAtLeast(1600)
        val buffer = ShortArray(bufferSize)
        var lastPartialText = ""
        var lastPartialTimeMs = 0L
        var chunkCount = 0
        var lastLogMs = 0L
        var everGotPartial = false
        try {
            Thread.sleep(150)
        } catch (_: InterruptedException) { }
        Log.d(TAG, "English Vosk recording loop started (sampleRate=$sampleRate bufferSize=$bufferSize)")
        try {
            while (isEnglishMicActive) {
                val rec = englishAudioRecord ?: break
                val read = try { rec.read(buffer, 0, buffer.size) } catch (e: Exception) {
                    Log.e(TAG, "Mic read error", e)
                    -1
                }
                if (read <= 0) {
                    if (chunkCount == 0 || (System.currentTimeMillis() - lastLogMs) >= 2000) {
                        Log.w(TAG, "Mic: read=$read (no data or error; recordingState=${rec.recordingState})")
                        lastLogMs = System.currentTimeMillis()
                    }
                    continue
                }
                chunkCount++
                val chunk = if (read < buffer.size) buffer.copyOf(read) else buffer
                // Log mic level: max abs sample and rough RMS to see if we're capturing sound
                var maxAbs = 0
                var sumSq = 0L
                for (i in chunk.indices) {
                    val s = chunk[i].toInt()
                    val abs = if (s >= 0) s else -s
                    if (abs > maxAbs) maxAbs = abs
                    sumSq += s.toLong() * s
                }
                val rms = if (chunk.isNotEmpty()) kotlin.math.sqrt((sumSq / chunk.size).toDouble()).toInt() else 0
                val (text, isFinal) = recognizer.processSamples(chunk)
                val nowMs = System.currentTimeMillis()
                if (text.isNotBlank()) everGotPartial = true
                if (chunkCount <= 5 || (nowMs - lastLogMs) >= 3000) {
                    Log.d(TAG, "Mic: chunks=$chunkCount read=$read maxAbs=$maxAbs rms=$rms | Vosk partial=\"$text\" final=$isFinal")
                    lastLogMs = nowMs
                }
                if (isFinal && text.isNotBlank()) {
                    lastPartialText = ""
                    lastPartialTimeMs = 0
                    runOnUiThread { if (!isDestroyed) onEnglishVoskFinalResult(text) }
                    recognizer.reset()
                } else if (text.isNotBlank()) {
                    if (text != lastPartialText) {
                        lastPartialText = text
                        lastPartialTimeMs = nowMs
                    }
                    if (verificationMode) verificationLastHeardText = text.trim()
                    runOnUiThread {
                        if (!isDestroyed && !verificationMode) {
                            if (!feedSpeechInputText(text, isFinal = false)) {
                                textView.text = text
                            }
                        }
                    }
                }
                // In verification mode: if partial text has been stable (no change) for long enough, treat as answer (Vosk may rarely return final)
                if (verificationMode && lastPartialText.isNotBlank() && lastPartialTimeMs > 0 && (nowMs - lastPartialTimeMs) >= verificationSilenceTimeoutMs) {
                    val finalText = recognizer.getFinalResult().trim().ifBlank { lastPartialText.trim() }
                    if (finalText.isNotBlank()) {
                        lastPartialText = ""
                        lastPartialTimeMs = 0
                        runOnUiThread { if (!isDestroyed) onEnglishVoskFinalResult(finalText) }
                        recognizer.reset()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "English Vosk recording thread error", e)
            runOnUiThread {
                if (!isDestroyed) {
                    stopEnglishVoskRecording()
                    setMicButtonAppearance(recording = false)
                    Toast.makeText(this@MainActivity, "English recognition error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } finally {
            Log.d(TAG, "English Vosk recording loop ended: chunks=$chunkCount everGotPartial=$everGotPartial")
            // Release AudioRecord on the same thread that was reading (avoids releaseBuffer native assert)
            try {
                englishAudioRecord?.stop()
            } catch (_: Exception) { }
            try {
                englishAudioRecord?.release()
            } catch (_: Exception) { }
            englishAudioRecord = null
        }
    }

    /** Called on main thread when Vosk English recognizer produces a final result. */
    private fun onEnglishVoskFinalResult(text: String) {
        if (isDestroyed) return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        verificationLastHeardText = trimmed
        if (pronunciationPracticeActive && !pronunciationPracticeResultHandled) {
            pronunciationPracticeResultHandled = true
            cancelVerificationTimeout()
            verificationMode = false
            expectedEnglishForVerification = null
            stopEnglishVoskRecording()
            setMicButtonAppearance(recording = false)
            val rows = pronunciationLessonRows ?: return
            val idx = pronunciationPracticeWordIndex
            if (idx !in rows.indices) return
            val expectedWord = rows[idx].getOrNull(0)?.trim() ?: ""
            val match = MatchNormalizer.matchesExpectedWithAlternates(expectedWord, trimmed)
            handlePronunciationPracticeResult(match, expectedWord, trimmed)
            return
        }
        if (verificationMode && !verificationResultHandled) {
            if (normalizeForMatch(trimmed) == "skip") {
                runOnUiThread { onSkipWord() }
                return
            }
            verificationResultHandled = true
            cancelVerificationTimeout()
            val expected = expectedEnglishForVerification ?: ""
            verificationMode = false
            expectedEnglishForVerification = null
            stopEnglishVoskRecording()
            setMicButtonAppearance(recording = false)
            val match = MatchNormalizer.matchesExpectedWithAlternates(expected, trimmed)
            handleVerificationResult(match, expected, trimmed)
            return
        }
        if (lessonMode == 3 && lessonMode3Listening) {
            lessonMode3Listening = false
            lessonMode3SpokeAnswer = true
            stopEnglishVoskRecording()
            setMicButtonAppearance(recording = false)
            val rows = lessonRows ?: return
            val idx = lessonIndex
            if (idx < rows.size) {
                val engA = rows[idx].engA
                englishTextView.setText(makeSvoSpannable(engA))
                speakEnglishString(engA)
            }
            return
        }
        if (feedSpeechInputText(trimmed, isFinal = true)) return
        addSentenceToList(trimmed, isBengali = false)
        translateEnglishToBengaliAndSpeak(trimmed)
    }

    /** Shared verification UI logic (Correct/Incorrect, 3-strikes, TTS, etc.). */
    private fun handleVerificationResult(match: Boolean, expected: String, said: String) {
        if (isDestroyed) return
        if (currentContentLayout == ContentLayout.SIMPLE_SENTENCE) setSimpleSentenceYouSaid(said)
        if (currentContentLayout == ContentLayout.SV_RIBBON) setSvRibbonYouSaid(said)
        if (currentContentLayout == ContentLayout.THREECOL_TABLE) {
            onThreeColVerificationResult(match, said)
            // Main 3-col table already handles TTS/strikes in [onThreeColVerificationResult]. Do not run shared
            // [handleVerificationResult] chains (incorrect_then_correct / lesson_verify) — they duplicate audio and use stale expected.
            if (threeColMode != ThreeColMode.VOCAB) return
        }
        if (currentContentLayout == ContentLayout.CONVERSATION_BUBBLES) onConvBubbleVerificationResult(match, said)
        if (currentContentLayout == ContentLayout.TENSE_TRIPLETS) {
            onTenseTripletVerificationResult(match, said)
            return
        }
        if (currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceMode == ConversationMode.VOCAB) {
            onExtendSentenceVocabVerificationResult(match, said)
            return
        }
        if (currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceMode == ConversationMode.TEST) {
            onExtendSentenceTestVerificationResult(match, said)
            return
        }
        if (currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceMode == ConversationMode.PRACTICE) {
            onExtendSentencePracticeVerificationResult(match, said)
            return
        }
        if ((currentContentLayout == ContentLayout.CONVERSATION_BUBBLES && convBubbleMode == ConversationMode.VOCAB) ||
            (currentContentLayout == ContentLayout.THREECOL_TABLE && threeColMode == ThreeColMode.VOCAB)) return
        val resultWord = if (match) getString(R.string.correct) else getString(R.string.incorrect)
        val inLesson = lessonRows != null
        val inSvoSentenceList = sentenceList.isNotEmpty() && !inLesson
        var shouldAdvanceToNext = false

        if (!match && inLesson) {
            lessonIncorrectCount++
            if (lessonIncorrectCount >= 3) {
                lessonIncorrectCount = 0
                advanceLessonToNextRow()
                shouldAdvanceToNext = true
                pendingRestartVerificationWith = null
            } else {
                pendingRestartVerificationWith = if (expected.isNotBlank()) expected else null
            }
        }
        if (!match && inSvoSentenceList) {
            svoSentenceStrikes++
            if (svoSentenceStrikes >= 3) {
                svoSentenceStrikes = 0
                currentNextIndex = (currentNextIndex + 1) % sentenceList.size
                shouldAdvanceToNext = true
                pendingRestartVerificationWith = null
            } else {
                pendingRestartVerificationWith = if (expected.isNotBlank()) expected else null
            }
        }
        if (!match && currentContentLayout == ContentLayout.SV_RIBBON && svRibbonBengali != null) {
            svRibbonIncorrectCount++
            if (svRibbonIncorrectCount >= 3) {
                svRibbonIncorrectCount = 0
                shouldAdvanceToNext = true
                pendingRestartVerificationWith = null
            } else {
                pendingRestartVerificationWith = if (expected.isNotBlank()) expected else null
            }
        }

        // THREECOL main table: strikes + retry listening live in [onThreeColVerificationResult] (early return above).
        if (!match && currentContentLayout == ContentLayout.CONVERSATION_BUBBLES && convBubbleRows.isNotEmpty()) {
            convBubbleIncorrectCount++
            if (convBubbleIncorrectCount >= 3) {
                convBubbleIncorrectCount = 0
                shouldAdvanceToNext = true
                pendingRestartVerificationWith = null
            } else {
                pendingRestartVerificationWith = if (expected.isNotBlank()) expected else null
            }
        }

        if (!match && expected.isNotBlank()) {
            if (inLesson && lessonMode == 4 && pendingRestartVerificationWith != null) {
                val rows = lessonRows
                val idx = lessonIndex
                if (rows != null && idx in rows.indices) {
                    pendingBengaliAfterIncorrect = rows[idx].bnQ
                }
            } else if (inLesson && pendingRestartVerificationWith != null) {
                pendingSpeakCorrectWordAfterIncorrect = expected
            } else if (currentContentLayout == ContentLayout.SV_RIBBON && pendingRestartVerificationWith != null) {
                pendingSpeakCorrectWordAfterIncorrect = expected
            } else if (currentContentLayout == ContentLayout.CONVERSATION_BUBBLES && pendingRestartVerificationWith != null && convBubbleMode != ConversationMode.TEST) {
                pendingSpeakCorrectWordAfterIncorrect = expected
            }
            if (ttsReady && textToSpeech != null) {
                textToSpeech?.setLanguage(Locale.US)
                val utteranceId = if (pendingBengaliAfterIncorrect != null) "incorrect_then_sentence" else "incorrect_then_correct"
                textToSpeech?.speak(resultWord, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } else {
                pendingSpeakCorrectWordAfterIncorrect = null
                pendingBengaliAfterIncorrect = null
                speakEnglishString(resultWord)
                val toRestart = pendingRestartVerificationWith
                if (toRestart != null && !isDestroyed) {
                    pendingRestartVerificationWith = null
                    startVerificationListening(toRestart)
                }
            }
        } else {
            speakEnglishString(resultWord)
        }

        if (match && currentContentLayout == ContentLayout.SV_RIBBON && svRibbonBengali != null) {
            verificationHandler.postDelayed({
                if (!isDestroyed && currentContentLayout == ContentLayout.SV_RIBBON) moveSvRibbonNextAfterCorrect()
            }, 1500)
        } else if (match && inLesson) {
            lessonIncorrectCount = 0
            lessonCorrectCount++
            advanceLessonAfterMatch()
            updateLessonStatistic()
            verificationHandler.postDelayed({
                if (lessonRows != null && !isDestroyed) onNextLessonStep()
            }, 1500)
        } else if (match && inSvoSentenceList) {
            svoSentenceStrikes = 0
            currentNextIndex = (currentNextIndex + 1) % sentenceList.size
            verificationHandler.postDelayed({
                if (!isDestroyed && sentenceList.isNotEmpty()) runOnUiThread { onNextSentence() }
            }, 1500)
        } else if (shouldAdvanceToNext && inLesson) {
            updateLessonStatistic()
            speakEnglishString("Moving to next.")
            verificationHandler.postDelayed({
                if (lessonRows != null && !isDestroyed) onNextLessonStep()
            }, 1500)
        } else if (shouldAdvanceToNext && inSvoSentenceList) {
            speakEnglishString("Moving to next.")
            verificationHandler.postDelayed({ onNextSentence() }, 1500)
        } else if (shouldAdvanceToNext && currentContentLayout == ContentLayout.SV_RIBBON && svRibbonBengali != null) {
            speakEnglishString("Moving to next.")
            verificationHandler.postDelayed({
                if (!isDestroyed && currentContentLayout == ContentLayout.SV_RIBBON) moveSvRibbonToNextAndSpeak()
            }, 1500)
        } else if (shouldAdvanceToNext && currentContentLayout == ContentLayout.CONVERSATION_BUBBLES && convBubbleRows.isNotEmpty()) {
            val idx = if (convBubbleMode == ConversationMode.TEST && convBubbleListeningForRowIndex in convBubbleRows.indices)
                convBubbleListeningForRowIndex else convBubbleCurrentIndex.coerceIn(0, convBubbleRows.lastIndex)
            val row = convBubbleRows[idx]
            val correctEnglish = MatchNormalizer.textForSpeakAndDisplay(row.english)
            if (convBubbleMode == ConversationMode.PRACTICE || convBubbleMode == ConversationMode.TEST) {
                convBubbleAdapter?.setSpokenText(idx, correctEnglish)
                convBubbleAdapter?.markResult(idx, false)
            }
            convBubbleAdvanceRunnable?.let { verificationHandler.removeCallbacks(it) }
            convBubbleAdvanceRunnable = Runnable {
                convBubbleAdvanceRunnable = null
                if (!isDestroyed && currentContentLayout == ContentLayout.CONVERSATION_BUBBLES) {
                    if (convBubbleMode == ConversationMode.TEST && convBubbleListeningForRowIndex in convBubbleRows.indices) {
                        convBubbleCurrentIndex = convBubbleListeningForRowIndex + 1
                        convBubbleListeningForRowIndex = -1
                    } else {
                        convBubbleCurrentIndex++
                    }
                    convBubbleAdapter?.setCurrentIndex(convBubbleCurrentIndex.coerceIn(0, convBubbleRows.lastIndex))
                    updateConvBubbleRowPositionText()
                    if (convBubbleCurrentIndex <= convBubbleRows.lastIndex && convBubbleControlRunning) {
                        speakConvBubbleCurrent()
                    } else {
                        convBubbleControlRunning = false
                        convBubbleControlPaused = false
                        updateConvBubbleControlBar()
                    }
                }
            }
            // TEST mode: no hints — show correct text only, do not speak. PRACTICE: show then advance after delay.
            verificationHandler.postDelayed(convBubbleAdvanceRunnable!!, 1500)
        } else if (!match) {
            val rows = lessonRows
            val idx = lessonIndex
            if (rows != null && idx in rows.indices) {
                val r = rows[idx]
                if (incorrectLessonRows.none { it.engA == r.engA }) {
                    incorrectLessonSourceName = lessonName
                    incorrectLessonRows.add(r)
                    saveIncorrectLessonList()
                }
            }
        }
    }

    /** Handle result of user repeating a pronunciation word: correct → next word; incorrect → 3 chances then next. */
    private fun handlePronunciationPracticeResult(match: Boolean, expectedWord: String, @Suppress("UNUSED_PARAMETER") said: String) {
        if (!pronunciationPracticeActive || isDestroyed) return
        val rows = pronunciationLessonRows ?: return
        val idx = pronunciationPracticeWordIndex
        if (idx !in rows.indices) return

        if (match) {
            Toast.makeText(this, getString(R.string.correct), Toast.LENGTH_SHORT).show()
            if (ttsReady && textToSpeech != null) {
                textToSpeech?.setLanguage(Locale.US)
                textToSpeech?.speak(getString(R.string.correct), TextToSpeech.QUEUE_FLUSH, null, "pronunciation_correct_feedback")
            }
            pronunciationPracticeAttempt = 0
            pronunciationPracticeWordIndex++
            while (pronunciationPracticeWordIndex < rows.size && rows[pronunciationPracticeWordIndex].getOrNull(0)?.trim().isNullOrEmpty()) {
                pronunciationPracticeWordIndex++
            }
            if (pronunciationPracticeWordIndex >= rows.size) {
                pronunciationPracticeActive = false
                Toast.makeText(this, getString(R.string.lesson_done), Toast.LENGTH_SHORT).show()
                if (ttsReady && textToSpeech != null) {
                    textToSpeech?.setLanguage(Locale.US)
                    textToSpeech?.speak(getString(R.string.lesson_done), TextToSpeech.QUEUE_FLUSH, null, null)
                }
                return
            }
            val nextWord = rows[pronunciationPracticeWordIndex].getOrNull(0)?.trim() ?: ""
            pendingPronunciationPracticeWord = nextWord
            textToSpeech?.setLanguage(Locale.US)
            textToSpeech?.speak(nextWord, TextToSpeech.QUEUE_FLUSH, null, "pronunciation_practice_word")
            return
        }

        pronunciationPracticeAttempt++
        if (pronunciationPracticeAttempt >= 3) {
            Toast.makeText(this, getString(R.string.pronunciation_moving_next), Toast.LENGTH_SHORT).show()
            if (ttsReady && textToSpeech != null) {
                textToSpeech?.setLanguage(Locale.US)
                textToSpeech?.speak(getString(R.string.pronunciation_moving_next), TextToSpeech.QUEUE_FLUSH, null, "pronunciation_moving_next_done")
            }
            pronunciationPracticeAttempt = 0
            pronunciationPracticeWordIndex++
            if (pronunciationPracticeWordIndex >= rows.size) {
                pronunciationPracticeActive = false
                verificationHandler.postDelayed({
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, getString(R.string.lesson_done), Toast.LENGTH_SHORT).show()
                        if (ttsReady && textToSpeech != null) {
                            textToSpeech?.setLanguage(Locale.US)
                            textToSpeech?.speak(getString(R.string.lesson_done), TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    }
                }, 1500)
                return
            }
            var nextIdx = pronunciationPracticeWordIndex
            while (nextIdx < rows.size && rows[nextIdx].getOrNull(0)?.trim().isNullOrEmpty()) nextIdx++
            pronunciationPracticeWordIndex = nextIdx
            if (pronunciationPracticeWordIndex < rows.size) {
                val nextWord = rows[pronunciationPracticeWordIndex].getOrNull(0)?.trim() ?: ""
                if (nextWord.isNotEmpty()) {
                    verificationHandler.postDelayed({
                        runOnUiThread {
                            if (!pronunciationPracticeActive || isDestroyed) return@runOnUiThread
                            pendingPronunciationPracticeWord = nextWord
                            textToSpeech?.setLanguage(Locale.US)
                            textToSpeech?.speak(nextWord, TextToSpeech.QUEUE_FLUSH, null, "pronunciation_practice_word")
                        }
                    }, 1500)
                }
            } else {
                pronunciationPracticeActive = false
                verificationHandler.postDelayed({
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, getString(R.string.lesson_done), Toast.LENGTH_SHORT).show()
                        if (ttsReady && textToSpeech != null) {
                            textToSpeech?.setLanguage(Locale.US)
                            textToSpeech?.speak(getString(R.string.lesson_done), TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    }
                }, 1500)
            }
            return
        }

        val chancesLeft = 3 - pronunciationPracticeAttempt
        Toast.makeText(this, getString(R.string.pronunciation_try_again_chances, chancesLeft), Toast.LENGTH_SHORT).show()
        pendingPronunciationPracticeWord = expectedWord
        textToSpeech?.setLanguage(Locale.US)
        textToSpeech?.speak(getString(R.string.try_again), TextToSpeech.QUEUE_FLUSH, null, "pronunciation_practice_try_again")
    }

    private fun initEnglishRecognizer() {
        if (recognizerIntent == null) {
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
        }
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(createEnglishRecognitionListener())
        }
    }

    private fun createEnglishRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            Log.w(TAG, "English recognition error (legacy): $error")
            runOnUiThread {
                if (pronunciationPracticeActive) {
                    cancelVerificationTimeout()
                    verificationMode = false
                    val expected = expectedEnglishForVerification ?: ""
                    expectedEnglishForVerification = null
                    if (USE_SYSTEM_SPEECH_FOR_ENGLISH_VERIFICATION) {
                        isEnglishMicActive = false
                        isRecording = false
                    }
                    setMicButtonAppearance(recording = false)
                    pronunciationPracticeResultHandled = true
                    if (expected.isNotBlank() && ttsReady && textToSpeech != null) {
                        pendingPronunciationPracticeWord = expected
                        textToSpeech?.setLanguage(Locale.US)
                        textToSpeech?.speak(getString(R.string.try_again), TextToSpeech.QUEUE_FLUSH, null, "pronunciation_practice_try_again")
                    }
                    Toast.makeText(this@MainActivity, getString(R.string.try_again), Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                if (verificationMode) {
                    // Treat recognition error as an incorrect attempt for the current expected English.
                    cancelVerificationTimeout()
                    verificationMode = false
                    val expected = expectedEnglishForVerification ?: ""
                    expectedEnglishForVerification = null
                    if (USE_SYSTEM_SPEECH_FOR_ENGLISH_VERIFICATION) {
                        isEnglishMicActive = false
                        isRecording = false
                    }
                    setMicButtonAppearance(recording = false)
                    if (expected.isNotBlank()) {
                        handleVerificationResult(false, expected, "")
                    } else {
                        speakEnglishString(getString(R.string.try_again))
                        Toast.makeText(this@MainActivity, getString(R.string.try_again), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        override fun onResults(results: android.os.Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
            val text = matches.firstOrNull()?.trim() ?: return
            if (text.isEmpty()) return
            if (pronunciationPracticeActive && !pronunciationPracticeResultHandled) {
                pronunciationPracticeResultHandled = true
                cancelVerificationTimeout()
                verificationMode = false
                expectedEnglishForVerification = null
                if (USE_SYSTEM_SPEECH_FOR_ENGLISH_VERIFICATION) {
                    isEnglishMicActive = false
                    isRecording = false
                }
                val rows = pronunciationLessonRows ?: return
                val idx = pronunciationPracticeWordIndex
                if (idx !in rows.indices) return
                val expectedWord = rows[idx].getOrNull(0)?.trim() ?: ""
                val match = MatchNormalizer.matchesExpectedWithAlternates(expectedWord, text)
                runOnUiThread {
                    setMicButtonAppearance(recording = false)
                    handlePronunciationPracticeResult(match, expectedWord, text)
                }
                return
            }
            if (verificationMode && !verificationResultHandled) {
                if (normalizeForMatch(text) == "skip") {
                    runOnUiThread { onSkipWord() }
                    return
                }
                verificationResultHandled = true
                cancelVerificationTimeout()
                val expected = expectedEnglishForVerification ?: ""
                verificationMode = false
                expectedEnglishForVerification = null
                if (USE_SYSTEM_SPEECH_FOR_ENGLISH_VERIFICATION) {
                    isEnglishMicActive = false
                    isRecording = false
                }
                val match = MatchNormalizer.matchesExpectedWithAlternates(expected, text)
                runOnUiThread {
                    if (USE_SYSTEM_SPEECH_FOR_ENGLISH_VERIFICATION) setMicButtonAppearance(recording = false)
                    handleVerificationResult(match, expected, text)
                }
                return
            }
            if (lessonMode == 3 && lessonMode3Listening) {
                lessonMode3Listening = false
                lessonMode3SpokeAnswer = true
                val rows = lessonRows ?: return
                val idx = lessonIndex
                if (idx < rows.size) {
                    val engA = rows[idx].engA
                    runOnUiThread {
                        englishTextView.setText(makeSvoSpannable(engA))
                        speakEnglishString(engA)
                    }
                }
                return
            }
            if (feedSpeechInputText(text, isFinal = true)) {
                if (isEnglishMicActive && !isDestroyed) {
                    speechRecognizer?.startListening(recognizerIntent)
                }
                return
            }
            addSentenceToList(text, isBengali = false)
            runOnUiThread { translateEnglishToBengaliAndSpeak(text) }
            if (isEnglishMicActive && !isDestroyed) {
                speechRecognizer?.startListening(recognizerIntent)
            }
        }
        override fun onPartialResults(partialResults: android.os.Bundle?) {}
        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    }

    private fun translateEnglishToBengaliAndSpeak(englishText: String) {
        if (englishText.isBlank()) return
        val trans = translatorEnToBn ?: return
        trans.downloadModelIfNeeded()
            .addOnSuccessListener {
                trans.translate(englishText)
                    .addOnSuccessListener { bengaliText ->
                        runOnUiThread {
                            if (!isDestroyed) {
                                val currentTop = textView.text.toString().trim()
                                val currentBottom = englishTextView.text.toString().trim()
                                textView.text = if (currentTop.isEmpty()) bengaliText else "$currentTop\n$bengaliText"
                                englishTextView.text = if (currentBottom.isEmpty()) englishText else "$currentBottom\n$englishText"
                                speakBengaliString(bengaliText)
                                Log.d(TAG, "Translated to Bengali and speaking: $bengaliText")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "En→Bn translation failed", e)
                        runOnUiThread {
                            if (!isDestroyed) Toast.makeText(this@MainActivity, "Translation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "En→Bn model download failed", e)
                runOnUiThread {
                    if (!isDestroyed) Toast.makeText(this@MainActivity, "Download English→Bengali model (internet needed): ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /** Sanitize a list name for use as filename (alphanumeric and underscore only). */
    private fun sanitizeListName(name: String): String {
        val s = name.trim().replace(Regex("[^A-Za-z0-9_]+"), "_")
        return if (s.isEmpty()) "list" else s
    }

    private fun showSaveListDialog() {
        if (sentenceList.isEmpty()) {
            Toast.makeText(this, getString(R.string.list_empty), Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(this).apply {
            hint = getString(R.string.save_list_name_hint)
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.save_list_dialog_title))
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                val fileName = sanitizeListName(if (name.isEmpty()) "list" else name) + StringUtils.LIST_FILE_SUFFIX
                saveSentenceListToFile(File(filesDir, fileName))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun saveSentenceListToFile(file: File) {
        try {
            val arr = JSONArray()
            for (s in sentenceList) {
                val obj = JSONObject()
                obj.put("text", s.text)
                obj.put("isBengali", s.isBengali)
                arr.put(obj)
            }
            file.writeText(arr.toString())
            Toast.makeText(this, getString(R.string.list_saved), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Save list failed", e)
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ───────────────────── Navigation Drawer (hamburger menu) ─────────────────────

    /** Get proficiency for a subtopic (0–100). */
    private fun getProficiency(actionKey: String): Int = proficiencyPrefs.getInt(actionKey, 0)

    /** Set proficiency for a subtopic (0–100). */
    fun setProficiency(actionKey: String, value: Int) {
        proficiencyPrefs.edit().putInt(actionKey, value.coerceIn(0, 100)).apply()
        refreshDrawerList()
    }

    /** Get average proficiency for a topic (average of its subtopics). */
    private fun getTopicProficiency(topic: Topic): Int {
        if (topic.subtopics.isEmpty()) return 0
        val sum = topic.subtopics.sumOf { getProficiency(it.actionKey) }
        return sum / topic.subtopics.size
    }

    private var drawerItems = mutableListOf<DrawerItem>()

    private fun refreshDrawerList() {
        (drawerList.adapter as? BaseAdapter)?.notifyDataSetChanged()
    }

    private fun setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        drawerList = findViewById(R.id.drawer_list)
        val topics = DrawerTopicBuilders.getTopicList(assets)
        drawerItems = DrawerTopicBuilders.buildDrawerItems(topics).toMutableList()

        fun performDrawerItemClick(position: Int) {
            if (position < 0 || position >= drawerItems.size) return
            val item = drawerItems[position]
            when (item) {
                is DrawerItem.LevelHeader -> {
                    if (item.expanded) {
                        item.expanded = false
                        var toRemove = item.topics.size
                        var idx = position + 1
                        while (toRemove > 0 && idx < drawerItems.size) {
                            when (drawerItems[idx]) {
                                is DrawerItem.TopicHeader -> { drawerItems.removeAt(idx); toRemove-- }
                                is DrawerItem.SubtopicEntry -> drawerItems.removeAt(idx)
                                else -> break
                            }
                        }
                    } else {
                        item.expanded = true
                        for ((j, topic) in item.topics.withIndex()) {
                            drawerItems.add(position + 1 + j, DrawerItem.TopicHeader(topic, j, expanded = false))
                        }
                    }
                    (drawerList.adapter as? BaseAdapter)?.notifyDataSetChanged()
                }
                is DrawerItem.TopicHeader -> {
                    if (item.topic.title == "POC Menu") {
                        drawerLayout.closeDrawers()
                        val view = switchContentLayout(ContentLayout.POC_BUTTON_MENU)
                        setupPocButtonMenu(view, item.topic)
                    } else if (item.expanded) {
                        item.expanded = false
                        var toRemove = item.topic.subtopics.size
                        var idx = position + 1
                        while (toRemove > 0 && idx < drawerItems.size) {
                            when (drawerItems[idx]) {
                                is DrawerItem.SubtopicEntry -> { drawerItems.removeAt(idx); toRemove-- }
                                is DrawerItem.TopicHeader -> break
                                else -> break
                            }
                        }
                    } else {
                        item.expanded = true
                        for ((si, sub) in item.topic.subtopics.withIndex()) {
                            drawerItems.add(position + 1 + si, DrawerItem.SubtopicEntry(sub, item.topicIndex))
                        }
                    }
                    (drawerList.adapter as? BaseAdapter)?.notifyDataSetChanged()
                }
                is DrawerItem.SubtopicEntry -> {
                    drawerLayout.closeDrawers()
                    if (item.subtopic.layoutType != ContentLayout.LEGACY) {
                        switchContentLayout(item.subtopic.layoutType)
                    } else if (currentContentLayout != ContentLayout.LEGACY) {
                        switchContentLayout(ContentLayout.LEGACY)
                    }
                    handleSubtopicAction(item.subtopic)
                }
            }
        }

        val adapter = object : BaseAdapter() {
            override fun getCount() = drawerItems.size
            override fun getItem(position: Int) = drawerItems[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getViewTypeCount() = 3
            override fun getItemViewType(position: Int) = when (drawerItems[position]) {
                is DrawerItem.LevelHeader -> 0
                is DrawerItem.TopicHeader -> 1
                is DrawerItem.SubtopicEntry -> 2
            }

            override fun getView(position: Int, convertView: android.view.View?, parent: ViewGroup): android.view.View {
                val item = drawerItems[position]
                return when (item) {
                    is DrawerItem.LevelHeader -> {
                        val view = convertView ?: LayoutInflater.from(this@MainActivity).inflate(R.layout.layout_drawer_topic_item, parent, false)
                        val titleView = view.findViewById<TextView>(R.id.topic_title)
                        val badgeView = view.findViewById<TextView>(R.id.topic_badge)
                        val expandIcon = view.findViewById<android.widget.ImageView>(R.id.topic_expand_icon)
                        titleView.text = item.title
                        badgeView.text = ""
                        expandIcon.setImageResource(if (item.expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
                        view.tag = position
                        view.setOnClickListener { performDrawerItemClick((it.tag as? Int) ?: return@setOnClickListener) }
                        view
                    }
                    is DrawerItem.TopicHeader -> {
                        val view = convertView ?: LayoutInflater.from(this@MainActivity).inflate(R.layout.layout_drawer_topic_item, parent, false)
                        val titleView = view.findViewById<TextView>(R.id.topic_title)
                        val badgeView = view.findViewById<TextView>(R.id.topic_badge)
                        val expandIcon = view.findViewById<android.widget.ImageView>(R.id.topic_expand_icon)
                        titleView.text = item.topic.title
                        badgeView.text = DrawerTopicBuilders.getTopicProgressSummary(item.topic) { getProficiency(it) }
                        expandIcon.setImageResource(if (item.expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
                        view.tag = position
                        view.setOnClickListener { performDrawerItemClick((it.tag as? Int) ?: return@setOnClickListener) }
                        view
                    }
                    is DrawerItem.SubtopicEntry -> {
                        val view = convertView ?: LayoutInflater.from(this@MainActivity).inflate(R.layout.layout_drawer_subtopic_item, parent, false)
                        val titleView = view.findViewById<TextView>(R.id.subtopic_title)
                        val badgeView = view.findViewById<TextView>(R.id.subtopic_badge)
                        titleView.text = item.subtopic.title
                        badgeView.text = "${getProficiency(item.subtopic.actionKey)}"
                        view.tag = position
                        view.setOnClickListener { performDrawerItemClick((it.tag as? Int) ?: return@setOnClickListener) }
                        view
                    }
                }
            }
        }

        drawerList.adapter = adapter
        drawerList.setOnItemClickListener { _, _, position, _ -> performDrawerItemClick(position) }

        findViewById<ImageButton>(R.id.hamburger_button).setOnClickListener {
            if (drawerLayout.isDrawerOpen(findViewById<android.view.View>(R.id.nav_drawer))) {
                drawerLayout.closeDrawers()
            } else {
                drawerLayout.openDrawer(findViewById<android.view.View>(R.id.nav_drawer))
            }
        }
    }

    /** Map subtopic actionKey → the actual load action (reuses existing showLoadListDialog actions). */
    private fun handleSubtopicAction(subtopic: Subtopic) {
        currentDrawerActionKey = subtopic.actionKey
        val actionKey = subtopic.actionKey
        // CONVERSATION_BUBBLES (Person A / Person B bubble format)
        val convBubbleAssetPath = conversationBubbleLessonAssetPaths[actionKey]
        if (convBubbleAssetPath != null) {
            // Layout switch happens inside loader only after file parses successfully (avoids empty shell + bars).
            loadConversationBubbleLesson(convBubbleAssetPath, actionKey, subtopic.title)
            return
        }
        // THREECOL_TABLE lessons (so e.g. simple_where uses 3col layout, not simple-sentence)
        val threeColAssetPath = threeColLessonAssetPaths[actionKey]
        if (threeColAssetPath != null) {
            loadThreeColLessonFromAsset(threeColAssetPath, actionKey, subtopic.title)
            return
        }
        if (actionKey == "extend_sentence") {
            loadExtendSentenceLesson("Lessons/Tense/extend_sentence.txt", subtopic.title)
            return
        }
        if (actionKey == "preposition_time_blocks") {
            loadPrepositionBlocksLesson("Lessons/Tense/preposition_time.txt", subtopic.title)
            return
        }
        // Special key must be handled before generic "simple_*" routing.
        if (actionKey == "simple_tense_triplets") {
            if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
            loadSimpleTenseTripletLessonFromAsset("Lessons/Tense/simple_tense.txt", "Simple tense triplets")
            return
        }
        if (actionKey == "simple_continuous_triplets") {
            if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
            loadSimpleTenseTripletLessonFromAsset("Lessons/Tense/simple_continuous.txt", "Simple continuous triplets")
            return
        }
        if (actionKey == "simple_perfect_triplets") {
            if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
            loadSimpleTenseTripletLessonFromAsset("Lessons/Tense/simple_perfect.txt", "Simple perfect triplets")
            return
        }
        if (actionKey == "simple_question_triplets") {
            if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
            loadSimpleTenseTripletLessonFromAsset("Lessons/Tense/simple_question.txt", "Simple question triplets")
            return
        }
        if (actionKey == "simple_continuous_question_triplets") {
            if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
            loadSimpleTenseTripletLessonFromAsset("Lessons/Tense/simple_continuous_question.txt", "Simple continuous question triplets")
            return
        }
        if (actionKey == "present_negative_duplex") {
            if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
            loadSimpleTenseDuplexLessonFromAsset("Lessons/Tense/present_negative.txt", "Present negative (duplex)")
            return
        }
        if (actionKey == "past_negative_duplex") {
            if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
            loadSimpleTenseDuplexLessonFromAsset("Lessons/Tense/past_negative.txt", "Past negative (duplex)")
            return
        }
        if (actionKey == "future_negative_duplex") {
            if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
            loadSimpleTenseDuplexLessonFromAsset("Lessons/Tense/future_negative.txt", "Future negative (duplex)")
            return
        }
        if (actionKey == "perfect_question_duplex") {
            if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
            loadSimpleTenseDuplexLessonFromAsset("Lessons/Tense/perfect_question.txt", "Perfect question")
            return
        }
        // Adjective-style lessons: load parses file first; loader switches to TENSE_TRIPLETS only after valid rows (avoids empty shell + bars).
        if (actionKey == "simple_adjective_dual") {
            loadSimpleAdjectiveDualLessonFromAsset("Lessons/Adjective/simple_adjective.txt", "Simple adjective")
            return
        }
        if (actionKey == "simple_adverb_dual") {
            loadSimpleAdjectiveDualLessonFromAsset("Lessons/Adjective/simple_adverb.txt", "Simple adverb")
            return
        }
        if (actionKey == "simple_preposition_dual") {
            loadSimpleAdjectiveDualLessonFromAsset("Lessons/Adjective/simple_preposition.txt", "Simple preposition")
            return
        }
        // Simple-sentence lessons (Let, How, Who, When, etc.): keep SIMPLE_SENTENCE layout, load into two bubbles
        if (actionKey.startsWith("simple_")) {
            if (currentContentLayout != ContentLayout.SIMPLE_SENTENCE) switchContentLayout(ContentLayout.SIMPLE_SENTENCE)
            loadSimpleSentenceLesson(actionKey)
            return
        }
        // 2-ribbon conveyor lessons: use native layout_sv_ribbon (conveyor_left + conveyor_right)
        if (actionKey == "sv_ribbon" || actionKey == "sv_past" || actionKey == "sv_future") {
            if (currentContentLayout != ContentLayout.SV_RIBBON) switchContentLayout(ContentLayout.SV_RIBBON)
            loadSvRibbonLesson(actionKey)
            return
        }
        // SVO drawer subtopics: load from Lessons/SVO .txt and show on LEGACY so data is visible
        val svoAsset = SvoDrawerAssets.get(actionKey)
        if (svoAsset != null) {
            val (path, topicName) = svoAsset
            if (currentContentLayout != ContentLayout.LEGACY) switchContentLayout(ContentLayout.LEGACY)
            loadSvoFromAssetEnglishFirst(path, topicName)
            return
        }
        val pronLessons = getPronunciationLessons()
        when (actionKey) {
            "mic_test" -> {
                // Stop any active mic/TTS first
                textToSpeech?.stop()
                if (isEnglishMicActive) stopEnglishMic()
                if (isRecording) stopMicRecording(speakBengali = false)
                clearPronunciationLessonState()
                lessonRows = null
                lessonName = "Mic Test"
                updateLessonTopicDisplay()
                val view = showSpeechInputLayout()
                speechInputView = view
                setupSpeechInputLangToggle(view)
                // Clear any previous text
                view.findViewById<TextView>(R.id.speech_recognized_text)?.text = ""
                val statusText = view.findViewById<TextView>(R.id.speech_status_text)
                statusText.text = "Select language, then tap mic"
                Toast.makeText(this, "Select language, then tap mic to speak.", Toast.LENGTH_SHORT).show()
            }
            "translation_practice" -> {
                stopAllMic()
                textToSpeech?.stop()
                clearPronunciationLessonState()
                lessonRows = null
                lessonName = "Translation Practice"
                updateLessonTopicDisplay()
                val view = showPracticeThreeAreaLayout()
                practiceView = view
                // Sample word pairs: Bengali, English
                practiceWordList = listOf(
                    Pair("বই", "book"),
                    Pair("পানি", "water"),
                    Pair("খাবার", "food"),
                    Pair("বাড়ি", "home"),
                    Pair("স্কুল", "school"),
                    Pair("বন্ধু", "friend"),
                    Pair("শিক্ষক", "teacher"),
                    Pair("আকাশ", "sky"),
                    Pair("সূর্য", "sun"),
                    Pair("চাঁদ", "moon")
                )
                practiceWordIndex = 0
                showPracticeWord()
                Toast.makeText(this, "Say the English meaning. Tap mic to speak.", Toast.LENGTH_SHORT).show()
            }
            "intro_bengali" -> {
                try {
                    val content = assets.open("introduction.txt").bufferedReader(StandardCharsets.UTF_8).readText().trim()
                    if (content.isEmpty()) {
                        Toast.makeText(this, "introduction.txt is empty", Toast.LENGTH_SHORT).show()
                        return
                    }
                    clearPronunciationLessonState()
                    lessonRows = null
                    lessonName = getString(R.string.introduction_topic)
                    updateLessonTopicDisplay()
                    // Use the new TEXT_DISPLAY layout (already switched by the drawer click)
                    showTextDisplayLayout(getString(R.string.introduction_topic), content, speakBengali = true)
                    Toast.makeText(this, getString(R.string.introduction_loaded), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not load introduction.txt", Toast.LENGTH_SHORT).show()
                }
            }
            "alphabet_az" -> {
                loadDiagramFromAssets("alphabet-pronunciation-table.html")
                setDescriptionInstruction("এই শব্দ গুলোর শুদ্ধ উচ্চারণ না জানলে এই এপ ভালভাবে কাজ করবে না। সেইজন্য এই অক্ষর গুলোর উচ্চারণ জানা জরুরী", Locale("bn"))
            }
            "pron_p_words" -> loadPronunciationByTitle("P words", pronLessons)
            "pron_t_words" -> loadPronunciationByTitle("T words", pronLessons)
            "pron_ck_words" -> loadPronunciationByTitle("C / K words", pronLessons)
            "pron_days" -> loadPronunciationByTitle("Rule 24: Days (-ei sound)", pronLessons)
            "pron_suffixes" -> loadPronunciationByTitle("Rules 25–30: Suffixes", pronLessons)
            "pron_tion" -> loadPronunciationByTitle("-tion words", pronLessons)
            "pron_ly" -> loadPronunciationByTitle("-ly words", pronLessons)
            "pron_ial1" -> loadPronunciationByTitle("-ial words (1)", pronLessons)
            "pron_day_ay" -> loadPronunciationByTitle("-day / -ay words", pronLessons)
            "pron_sion" -> loadPronunciationByTitle("-sion words", pronLessons)
            "pron_ture" -> loadPronunciationByTitle("-ture words", pronLessons)
            "pron_ial2" -> loadPronunciationByTitle("-ial words (2)", pronLessons)
            "pron_ous" -> loadPronunciationByTitle("-ous words", pronLessons)
            "pron_ment" -> loadPronunciationByTitle("-ment words", pronLessons)
            "pron_fully" -> loadPronunciationByTitle("-fully words", pronLessons)
            "pron_silent_e" -> loadPronunciationByTitle("Silent e (a-e)", pronLessons)
            "pron_silent_g" -> loadPronunciationByTitle("Silent G (gn at end)", pronLessons)
            "pron_silent_b" -> loadPronunciationByTitle("Silent B", pronLessons)
            "pron_silent_w" -> loadPronunciationByTitle("Silent W", pronLessons)
            "pron_silent_k" -> loadPronunciationByTitle("Silent K", pronLessons)
            "pron_rule23" -> loadPronunciationByTitle("Rule 23: silent G (Design, Resign…)", pronLessons)
            "pron_al" -> loadPronunciationByTitle("-al words", pronLessons)
            "pron_sure" -> loadPronunciationByTitle("-sure words", pronLessons)
            "pron_age" -> loadPronunciationByTitle("-age words", pronLessons)
            "pron_tion2" -> loadPronunciationByTitle("-tion (Mention, Action…)", pronLessons)
            "verb_basic" -> showVerbSelectorDialog()
            "verb_tenses" -> showTenseVerbSelectorDialog()
            "verb_regular" -> loadVerbLessonFromAsset("Lessons/Regular_verbs.txt", "regular_verbs")
            "verb_irregular" -> loadVerbLessonFromAsset("Lessons/Irregular_verbs.txt", "irregular_verbs")
            "grammar_pos" -> loadReferenceHtmlPage("parts-of-speech.html", getString(R.string.parts_of_speech_title))
            "grammar_svo" -> loadReferenceHtmlPage("svo-sentences.html", getString(R.string.svo_sentences_title))
            "diagram_1to3" -> { loadDiagramFromAssets("diagram-1to3.html"); setDescriptionInstruction(null, null) }
            "diagram_3to1" -> { loadDiagramFromAssets("diagram-3to1.html"); setDescriptionInstruction(null, null) }
            "lesson_file" -> openLessonLauncher.launch(arrayOf("text/plain", "application/octet-stream"))
            "lesson_introduce" -> {
                try {
                    val content = assets.open("introduce.txt").bufferedReader().readText()
                    val rows = parseLessonFile(content)
                    if (rows.isNotEmpty()) { clearPronunciationLessonState(); showModeSelectorDialog(rows, "introduce") }
                } catch (_: Exception) {}
            }
            "lesson_incorrect" -> showLoadIncorrectDialog()
            "svo_sentences" -> loadSvoFromAsset("Lessons/svo_sentences_list.txt")
            "svo_eat" -> loadSvoFromAsset("Lessons/SVO_eat.txt")
            "svo_play" -> loadSvoFromAsset("Lessons/SVO_play.txt")
            "tense_diagram" -> {
                pronunciationLessonRows = null
                pronunciationLessonTitle = null
                descriptionWebView.loadUrl("file:///android_asset/tenses_hierarchy.html")
                setDescriptionInstruction(null, null)
            }
            "simple_tense_triplets" -> {
                if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
                loadSimpleTenseTripletLessonFromAsset("Lessons/Tense/simple_tense.txt", "Simple tense triplets")
            }
            "simple_continuous_triplets" -> {
                if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
                loadSimpleTenseTripletLessonFromAsset("Lessons/Tense/simple_continuous.txt", "Simple continuous triplets")
            }
            "simple_perfect_triplets" -> {
                if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
                loadSimpleTenseTripletLessonFromAsset("Lessons/Tense/simple_perfect.txt", "Simple perfect triplets")
            }
            "simple_question_triplets" -> {
                if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
                loadSimpleTenseTripletLessonFromAsset("Lessons/Tense/simple_question.txt", "Simple question triplets")
            }
            "simple_continuous_question_triplets" -> {
                if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
                loadSimpleTenseTripletLessonFromAsset("Lessons/Tense/simple_continuous_question.txt", "Simple continuous question triplets")
            }
            "present_negative_duplex" -> {
                if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
                loadSimpleTenseDuplexLessonFromAsset("Lessons/Tense/present_negative.txt", "Present negative (duplex)")
            }
            "past_negative_duplex" -> {
                if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
                loadSimpleTenseDuplexLessonFromAsset("Lessons/Tense/past_negative.txt", "Past negative (duplex)")
            }
            "future_negative_duplex" -> {
                if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
                loadSimpleTenseDuplexLessonFromAsset("Lessons/Tense/future_negative.txt", "Future negative (duplex)")
            }
            "perfect_question_duplex" -> {
                if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) switchContentLayout(ContentLayout.TENSE_TRIPLETS)
                loadSimpleTenseDuplexLessonFromAsset("Lessons/Tense/perfect_question.txt", "Perfect question")
            }

            // ── Table Display tests ──
            "table_alphabet_sound" -> {
                stopAllMic(); textToSpeech?.stop()
                lessonName = "Alphabet Sound (A–Z)"
                updateLessonTopicDisplay()
                try {
                    val csv = assets.open("Lessons/alphabet_sound.txt")
                        .bufferedReader(StandardCharsets.UTF_8).readText().trim()
                    // First line is the header; rest are data rows
                    val lines = csv.lines()
                    val headerLine = lines.firstOrNull() ?: "Letter,Bengali Pronunciation"
                    val headerCols = headerLine.split(",").map { it.trim() }
                    val dataText = lines.drop(1).joinToString("\n")
                    showTableDisplayLayout(
                        title = "Alphabet Sound (A–Z)",
                        columnCount = 2,
                        headers = headerCols.take(2),
                        csvText = dataText,
                        tappableColumn = 1,
                        tappableLocale = Locale("bn"),
                        interactive = true,
                        speakCol = 0,       // speak English letter name (column 0)
                        matchCol = 1,       // match user speech against Bengali pronunciation (column 1)
                        interactiveLocale = Locale("bn"),  // listen in Bengali
                        speakLocale = Locale.US            // speak in English
                    )
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not load alphabet_sound.txt", Toast.LENGTH_SHORT).show()
                }
            }
            "table_test_2col" -> {
                stopAllMic(); textToSpeech?.stop()
                lessonName = "2-Column Table"
                updateLessonTopicDisplay()
                showTableDisplayLayout(
                    title = "English – Bengali",
                    columnCount = 2,
                    headers = listOf("English", "বাংলা"),
                    csvText = """
                        book,বই
                        water,পানি
                        food,খাবার
                        home,বাড়ি
                        school,স্কুল
                        friend,বন্ধু
                        teacher,শিক্ষক
                        sky,আকাশ
                        sun,সূর্য
                        moon,চাঁদ
                    """.trimIndent(),
                    tappableColumn = 0,
                    tappableLocale = Locale.US
                )
            }
            "table_test_3col" -> {
                stopAllMic(); textToSpeech?.stop()
                lessonName = "3-Column Table"
                updateLessonTopicDisplay()
                showTableDisplayLayout(
                    title = "Word – Pronunciation – Meaning",
                    columnCount = 3,
                    headers = listOf("Word", "Pronunciation", "বাংলা অর্থ"),
                    csvText = """
                        pen,পেন,কলম
                        paper,পেইপার,কাগজ
                        people,পিপল,মানুষ
                        place,প্লেইস,জায়গা
                        picture,পিকচার,ছবি
                        party,পার্টি,দল
                        play,প্লেই,খেলা
                        phone,ফোন,ফোন
                        police,পুলিশ,পুলিশ
                        park,পার্ক,পার্ক
                    """.trimIndent(),
                    tappableColumn = 0,
                    tappableLocale = Locale.US
                )
            }
            "table_test_4col" -> {
                stopAllMic(); textToSpeech?.stop()
                lessonName = "4-Column Table"
                updateLessonTopicDisplay()
                showTableDisplayLayout(
                    title = "Verb Conjugation",
                    columnCount = 4,
                    headers = listOf("Verb", "Past", "Past Participle", "বাংলা"),
                    csvText = """
                        go,went,gone,যাওয়া
                        eat,ate,eaten,খাওয়া
                        come,came,come,আসা
                        do,did,done,করা
                        see,saw,seen,দেখা
                        give,gave,given,দেওয়া
                        take,took,taken,নেওয়া
                        make,made,made,তৈরি করা
                        read,read,read,পড়া
                        write,wrote,written,লেখা
                    """.trimIndent(),
                    tappableColumn = 0,
                    tappableLocale = Locale.US
                )
            }
            // ── Dynamic pronunciation file lessons (from assets/Lessons/pronunciation/) ──
            // Action keys have the form "pron:filename.txt"
            else -> if (actionKey.startsWith("pron:")) {
                val filename = actionKey.removePrefix("pron:")
                val title = filename.removeSuffix(".txt")
                    .replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { w ->
                        w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                loadSoundFile("pronunciation/$filename", title, englishMatchMode = true)
            } else {
                // Layout may have been switched; show feedback for unhandled keys (e.g. svo:I, sv_ribbon, conversation_first_meeting)
                Toast.makeText(this, getString(R.string.not_available_yet), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Progressive extend-sentence lesson: blank-line groups; each line English, Bengali, hint, optional spoken explanation. */
    private fun loadExtendSentenceLesson(assetPath: String, displayTitle: String) {
        val content = try {
            assets.open(assetPath).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not load $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        val groups = LessonFileParsers.parseExtendSentenceGroups(content)
        if (groups.isEmpty()) {
            Toast.makeText(this, "No valid lines in $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        if (masterWordListMap == null) {
            masterWordListMap = LessonFileParsers.loadMasterWordList(assets)
        }
        val embeddedVocab = LessonFileParsers.extractVocabularyBlock(content)
        currentLessonVocabWords = if (embeddedVocab.isNotEmpty()) {
            embeddedVocab
        } else {
            LessonFileParsers.extractVocabWordsFromExtendSentenceLines(content)
        }
        lessonVocabRows = LessonFileParsers.filterLessonVocabRowsByMaster(
            LessonFileParsers.buildLessonVocabRowsOnlyInMaster(currentLessonVocabWords, masterWordListMap ?: emptyMap()),
            masterWordListMap
        )
        extendSentenceMode = ConversationMode.LEARNING
        lessonName = displayTitle
        updateLessonTopicDisplay()
        val view = switchContentLayout(ContentLayout.EXTEND_SENTENCE)
        setupExtendSentenceLayout(view, groups)
    }

    private fun setupExtendSentenceLayout(root: View, groups: List<List<ExtendSentenceRow>>) {
        extendSentenceRoot = root
        extendSentenceGroups = groups
        extendSentenceCurrentGroupIndex = 0
        val build = buildExtendSentenceListItems(groups)
        extendSentenceHeaderAdapterPositions = build.headerPositions
        val recycler = root.findViewById<RecyclerView>(R.id.extend_sentence_recycler)
        extendSentenceRecycler = recycler
        recycler.layoutManager = LinearLayoutManager(this)
        extendSentenceAdapter = ExtendSentenceAdapter(build.items).also {
            it.submitList(build)
            recycler.adapter = it
        }
        val titleTv = root.findViewById<TextView>(R.id.extend_sentence_group_title)
        val prev = root.findViewById<ImageButton>(R.id.extend_sentence_prev_group)
        val next = root.findViewById<ImageButton>(R.id.extend_sentence_next_group)
        val vocabInclude = root.findViewById<View>(R.id.extend_sentence_vocab_include)
        setupVocabTabForCurrentLesson(vocabInclude) { }
        fun updateGroupNav() {
            val gc = groups.size
            titleTv?.text = "Part ${extendSentenceCurrentGroupIndex + 1} of $gc"
            prev?.isEnabled = extendSentenceCurrentGroupIndex > 0
            next?.isEnabled = extendSentenceCurrentGroupIndex < gc - 1
            root.findViewById<ImageButton>(R.id.lesson_mode_bar_prev)?.isEnabled = prev?.isEnabled == true
            root.findViewById<ImageButton>(R.id.lesson_mode_bar_next)?.isEnabled = next?.isEnabled == true
        }
        fun navigateGroup(delta: Int) {
            if (delta < 0 && extendSentenceCurrentGroupIndex > 0) {
                extendSentenceCurrentGroupIndex--
            } else if (delta > 0 && extendSentenceCurrentGroupIndex < groups.size - 1) {
                extendSentenceCurrentGroupIndex++
            } else {
                return
            }
            if ((extendSentenceMode == ConversationMode.PRACTICE || extendSentenceMode == ConversationMode.TEST) &&
                (extendSentenceControlRunning || verificationMode)
            ) {
                textToSpeech?.stop()
                cancelExtendSentencePracticeVerification()
                extendSentenceControlRunning = false
                extendSentenceControlPaused = false
                updateExtendSentenceControlBar()
            }
            extendSentencePracticeRowIndex = 0
            extendSentencePracticeIncorrectStreak = 0
            extendSentenceTestListeningRowIndex = 1
            extendSentenceTestIncorrectStreak = 0
            extendSentenceTestIntroCompleted = false
            extendSentenceAdapter?.clearSessionFeedback()
            extendSentenceAdapter?.setTestListeningRowInGroup(1)
            extendSentenceAdapter?.setTestActiveGroupIndex(extendSentenceCurrentGroupIndex)
            val pos = extendSentenceHeaderAdapterPositions.getOrNull(extendSentenceCurrentGroupIndex) ?: 0
            recycler.smoothScrollToPosition(pos)
            updateGroupNav()
        }
        prev?.setOnClickListener { navigateGroup(-1) }
        next?.setOnClickListener { navigateGroup(1) }
        updateGroupNav()
        setupExtendSentenceModeBar(root, ::navigateGroup)
        root.findViewById<View>(R.id.lesson_base_control_include)?.visibility = View.VISIBLE
        updateExtendSentenceControlBar()
    }

    private fun setupExtendSentenceModeBar(root: View, navigateGroup: (Int) -> Unit) {
        val learningBtn = root.findViewById<android.widget.Button>(R.id.lesson_mode_learning)
        val practiceBtn = root.findViewById<android.widget.Button>(R.id.lesson_mode_practice)
        val testBtn = root.findViewById<android.widget.Button>(R.id.lesson_mode_test)
        val vocabBtn = root.findViewById<android.widget.Button>(R.id.lesson_mode_vocab)
        fun stopPlaybackIfModeSwitch() {
            if (extendSentenceControlRunning || extendSentenceControlPaused) {
                textToSpeech?.stop()
                cancelExtendSentencePracticeVerification()
                extendSentenceControlRunning = false
                extendSentenceControlPaused = false
                extendSentenceAdapter?.clearBlockHighlight()
                updateExtendSentenceControlBar()
            }
            if (convBubbleControlRunning) {
                textToSpeech?.stop()
                cancelVerificationTimeout()
                if (verificationMode) {
                    verificationMode = false
                    expectedEnglishForVerification = null
                    try { speechRecognizer?.stopListening() } catch (_: Exception) { }
                    stopEnglishVoskRecording()
                    setMicButtonAppearance(recording = false)
                }
                isEnglishMicActive = false
                isRecording = false
                convBubbleControlRunning = false
                updateExtendSentenceControlBar()
            }
        }
        fun setMode(mode: ConversationMode) {
            if (extendSentenceMode != mode) stopPlaybackIfModeSwitch()
            extendSentenceMode = mode
            if (mode == ConversationMode.PRACTICE) {
                extendSentencePracticeRowIndex = 0
                extendSentencePracticeIncorrectStreak = 0
                extendSentenceAdapter?.clearSessionFeedback()
            }
            if (mode == ConversationMode.TEST) {
                extendSentenceTestListeningRowIndex = 1
                extendSentenceTestIncorrectStreak = 0
                extendSentenceTestIntroCompleted = false
                extendSentenceAdapter?.clearSessionFeedback()
                extendSentenceAdapter?.setTestListeningRowInGroup(1)
            }
            updateExtendSentenceModeTabAppearance(root)
        }
        learningBtn?.setOnClickListener { setMode(ConversationMode.LEARNING) }
        practiceBtn?.setOnClickListener { setMode(ConversationMode.PRACTICE) }
        testBtn?.setOnClickListener { setMode(ConversationMode.TEST) }
        vocabBtn?.setOnClickListener { setMode(ConversationMode.VOCAB) }
        root.findViewById<ImageButton>(R.id.lesson_mode_bar_prev)?.setOnClickListener { navigateGroup(-1) }
        root.findViewById<ImageButton>(R.id.lesson_mode_bar_next)?.setOnClickListener { navigateGroup(1) }
        updateExtendSentenceModeTabAppearance(root)
    }

    private fun updateExtendSentenceModeTabAppearance(root: View) {
        val learningBtn = root.findViewById<android.widget.TextView>(R.id.lesson_mode_learning)
        val practiceBtn = root.findViewById<android.widget.TextView>(R.id.lesson_mode_practice)
        val testBtn = root.findViewById<android.widget.TextView>(R.id.lesson_mode_test)
        val vocabBtn = root.findViewById<android.widget.TextView>(R.id.lesson_mode_vocab)
        val recycler = root.findViewById<View>(R.id.extend_sentence_recycler)
        val vocabInclude = root.findViewById<View>(R.id.extend_sentence_vocab_include)
        val white = 0xFFFFFFFF.toInt()
        val darkText = 0xFF555555.toInt()
        val m = extendSentenceMode
        val sel = R.drawable.bg_lesson_mode_tab_selected
        val unsel = R.drawable.bg_lesson_mode_tab_unselected
        learningBtn?.setBackgroundResource(if (m == ConversationMode.LEARNING) sel else unsel)
        learningBtn?.setTextColor(if (m == ConversationMode.LEARNING) white else darkText)
        practiceBtn?.setBackgroundResource(if (m == ConversationMode.PRACTICE) sel else unsel)
        practiceBtn?.setTextColor(if (m == ConversationMode.PRACTICE) white else darkText)
        testBtn?.setBackgroundResource(if (m == ConversationMode.TEST) sel else unsel)
        testBtn?.setTextColor(if (m == ConversationMode.TEST) white else darkText)
        vocabBtn?.setBackgroundResource(if (m == ConversationMode.VOCAB) sel else unsel)
        vocabBtn?.setTextColor(if (m == ConversationMode.VOCAB) white else darkText)
        val isVocab = m == ConversationMode.VOCAB
        recycler?.visibility = if (isVocab) View.GONE else View.VISIBLE
        vocabInclude?.visibility = if (isVocab) View.VISIBLE else View.GONE
        // Base lesson bar always shows V; vocab list may be empty if no master matches (fallback still fills rows when possible).
        vocabBtn?.visibility = View.VISIBLE
        extendSentenceAdapter?.setDisplayMode(
            when (m) {
                ConversationMode.LEARNING -> ExtendSentenceDisplayMode.LEARNING
                ConversationMode.PRACTICE -> ExtendSentenceDisplayMode.PRACTICE
                ConversationMode.TEST -> ExtendSentenceDisplayMode.TEST
                ConversationMode.VOCAB -> ExtendSentenceDisplayMode.LEARNING
            }
        )
        extendSentenceAdapter?.setTestActiveGroupIndex(extendSentenceCurrentGroupIndex)
        if (m != ConversationMode.PRACTICE && m != ConversationMode.TEST) {
            extendSentenceAdapter?.clearSessionFeedback()
        }
        updateExtendSentenceControlBar()
    }

    private fun updateExtendSentenceControlBar() {
        if (extendSentenceMode == ConversationMode.VOCAB) {
            controlStartStopButton?.let { ControlBarUtils.setControlStartStopButton(this, it, convBubbleControlRunning) }
            controlPauseResumeButton?.let { ControlBarUtils.setControlPauseResumeButton(this, it, false) }
        } else {
            controlStartStopButton?.let { ControlBarUtils.setControlStartStopButton(this, it, extendSentenceControlRunning) }
            controlPauseResumeButton?.let { ControlBarUtils.setControlPauseResumeButton(this, it, extendSentenceControlPaused) }
        }
    }

    /** V tab verification for extend sentence (same flow as tense triplets vocab). */
    private fun onExtendSentenceVocabVerificationResult(match: Boolean, spokenTextIgnored: String) {
        if (currentContentLayout != ContentLayout.EXTEND_SENTENCE || extendSentenceMode != ConversationMode.VOCAB || currentVTabRows.isEmpty()) return
        runOnUiThread { Toast.makeText(this, if (match) getString(R.string.correct) else getString(R.string.incorrect), Toast.LENGTH_SHORT).show() }
        val curIdx = (lessonVocabAdapter?.currentIndex ?: 0).coerceIn(0, currentVTabRows.lastIndex)
        val row = currentVTabRows.getOrNull(curIdx) ?: return
        runOnUiThread { lessonVocabAdapter?.setSpokenText(curIdx, spokenTextIgnored) }
        val wordKey = row.word.trim().lowercase()
        val advance: Boolean = if (match) {
            vocabIncorrectCount = 0
            true
        } else {
            vocabIncorrectCount++
            vocabIncorrectCount >= 3
        }
        if (advance) {
            runOnUiThread { lessonVocabAdapter?.setResult(curIdx, match) }
            vocabIncorrectCount = 0
            vocabularyProgress[wordKey] = if (match) LessonFileParsers.VOCAB_PROGRESS_PASSED else LessonFileParsers.VOCAB_PROGRESS_FAILED
            LessonFileParsers.saveVocabularyProgress(filesDir, vocabularyProgress)
            if (match && !vocabShowAllWords) {
                val v = LessonFileParsers.filterLessonVocabRowsByMaster(lessonVocabRows, masterWordListMap)
                currentVTabRows = LessonFileParsers.filterLessonVocabRowsNeedingTest(v, vocabularyProgress)
                runOnUiThread { lessonVocabAdapter?.updateRows(currentVTabRows) }
            }
            val nextIdx = if (match && !vocabShowAllWords) curIdx.coerceAtMost((currentVTabRows.size - 1).coerceAtLeast(0))
            else (curIdx + 1).coerceAtMost((currentVTabRows.size - 1).coerceAtLeast(0))
            lessonVocabAdapter?.currentIndex = nextIdx
            extendSentenceRoot?.findViewById<View>(R.id.extend_sentence_vocab_include)
                ?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.lesson_vocab_recycler)
                ?.scrollToPosition(nextIdx)
            if (currentVTabRows.isNotEmpty() && convBubbleControlRunning) {
                verificationHandler.postDelayed({
                    if (!isDestroyed && currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceMode == ConversationMode.VOCAB && convBubbleControlRunning && currentVTabRows.isNotEmpty()) {
                        val idx = (lessonVocabAdapter?.currentIndex ?: nextIdx).coerceIn(0, currentVTabRows.lastIndex)
                        val nextRow = currentVTabRows.getOrNull(idx) ?: return@postDelayed
                        textToSpeech?.stop()
                        textToSpeech?.setLanguage(Locale.US)
                        textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(nextRow.word), TextToSpeech.QUEUE_FLUSH, null, "vocab_word")
                        expectedEnglishForVerification = nextRow.word
                        verificationHandler.postDelayed({
                            if (!isDestroyed && currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceMode == ConversationMode.VOCAB && convBubbleControlRunning) {
                                startVerificationListening(nextRow.word)
                            }
                        }, 800)
                    }
                }, 1500)
            } else {
                convBubbleControlRunning = false
                runOnUiThread { updateExtendSentenceControlBar() }
            }
        } else {
            verificationHandler.postDelayed({
                if (!isDestroyed && currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceMode == ConversationMode.VOCAB && convBubbleControlRunning && currentVTabRows.isNotEmpty()) {
                    val idx = (lessonVocabAdapter?.currentIndex ?: curIdx).coerceIn(0, currentVTabRows.lastIndex)
                    val retryRow = currentVTabRows.getOrNull(idx) ?: return@postDelayed
                    textToSpeech?.stop()
                    textToSpeech?.setLanguage(Locale.US)
                    textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(retryRow.word), TextToSpeech.QUEUE_FLUSH, null, "vocab_word_after_fail")
                    expectedEnglishForVerification = retryRow.word
                    verificationHandler.postDelayed({
                        if (!isDestroyed && currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceMode == ConversationMode.VOCAB && convBubbleControlRunning) {
                            startVerificationListening(retryRow.word)
                        }
                    }, 800)
                }
            }, 1500)
        }
    }

    private fun cancelExtendSentencePracticeVerification() {
        cancelVerificationTimeout()
        if (verificationMode) {
            verificationMode = false
            expectedEnglishForVerification = null
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            stopEnglishVoskRecording()
            setMicButtonAppearance(recording = false)
        }
        isEnglishMicActive = false
        isRecording = false
    }

    private fun onExtendSentenceStartStop() {
        if (extendSentenceGroups.isEmpty()) return
        when (extendSentenceMode) {
            ConversationMode.PRACTICE -> {
                if (extendSentenceControlRunning) {
                    textToSpeech?.stop()
                    cancelExtendSentencePracticeVerification()
                    extendSentenceControlRunning = false
                    extendSentenceControlPaused = false
                    extendSentenceAdapter?.clearBlockHighlight()
                    updateExtendSentenceControlBar()
                    return
                }
                extendSentencePracticeRowIndex = 0
                extendSentencePracticeIncorrectStreak = 0
                extendSentenceAdapter?.clearSessionFeedback()
                extendSentenceControlRunning = true
                extendSentenceControlPaused = false
                updateExtendSentenceControlBar()
                speakExtendSentencePracticeCurrentRow()
            }
            ConversationMode.TEST -> {
                if (extendSentenceControlRunning) {
                    textToSpeech?.stop()
                    cancelExtendSentencePracticeVerification()
                    extendSentenceControlRunning = false
                    extendSentenceControlPaused = false
                    extendSentenceTestIntroCompleted = false
                    extendSentenceAdapter?.clearBlockHighlight()
                    updateExtendSentenceControlBar()
                    return
                }
                extendSentenceTestListeningRowIndex = 1
                extendSentenceTestIncorrectStreak = 0
                extendSentenceTestIntroCompleted = false
                extendSentenceAdapter?.clearSessionFeedback()
                extendSentenceAdapter?.setTestListeningRowInGroup(1)
                extendSentenceAdapter?.setTestActiveGroupIndex(extendSentenceCurrentGroupIndex)
                extendSentenceControlRunning = true
                extendSentenceControlPaused = false
                updateExtendSentenceControlBar()
                speakExtendSentenceTestIntro()
            }
            ConversationMode.LEARNING -> {
                if (extendSentenceControlRunning) {
                    textToSpeech?.stop()
                    extendSentenceControlRunning = false
                    extendSentenceControlPaused = false
                    extendSentenceAdapter?.clearBlockHighlight()
                    updateExtendSentenceControlBar()
                    return
                }
                extendSentenceControlRunning = true
                extendSentenceControlPaused = false
                updateExtendSentenceControlBar()
                speakExtendSentenceCurrentGroup()
            }
            else -> {
                Toast.makeText(this, getString(R.string.extend_sentence_use_learning_or_practice), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onExtendSentencePauseResume() {
        if (extendSentenceGroups.isEmpty()) return
        if (extendSentenceMode == ConversationMode.PRACTICE) {
            if (!extendSentenceControlRunning) return
            if (extendSentenceControlPaused) {
                extendSentenceControlPaused = false
                updateExtendSentenceControlBar()
                speakExtendSentencePracticeCurrentRow()
            } else {
                textToSpeech?.stop()
                cancelExtendSentencePracticeVerification()
                extendSentenceControlPaused = true
                extendSentenceAdapter?.clearBlockHighlight()
                updateExtendSentenceControlBar()
            }
            return
        }
        if (extendSentenceMode == ConversationMode.TEST) {
            if (!extendSentenceControlRunning) return
            if (extendSentenceControlPaused) {
                extendSentenceControlPaused = false
                updateExtendSentenceControlBar()
                if (!extendSentenceTestIntroCompleted) {
                    speakExtendSentenceTestIntro()
                } else {
                    startExtendSentenceTestListeningForCurrentRow()
                }
            } else {
                textToSpeech?.stop()
                cancelExtendSentencePracticeVerification()
                extendSentenceControlPaused = true
                extendSentenceAdapter?.clearBlockHighlight()
                updateExtendSentenceControlBar()
            }
            return
        }
        if (extendSentenceMode != ConversationMode.LEARNING) return
        if (!extendSentenceControlRunning) return
        if (extendSentenceControlPaused) {
            extendSentenceControlPaused = false
            updateExtendSentenceControlBar()
            speakExtendSentenceCurrentGroup()
        } else {
            textToSpeech?.stop()
            extendSentenceControlPaused = true
            extendSentenceAdapter?.clearBlockHighlight()
            updateExtendSentenceControlBar()
        }
    }

    /** Speaks English → Bengali → optional 4th segment (Bengali; not shown in UI). Hint column is display-only. */
    private fun speakExtendSentenceCurrentGroup() {
        if (extendSentenceMode != ConversationMode.LEARNING) return
        val gi = extendSentenceCurrentGroupIndex.coerceIn(0, (extendSentenceGroups.size - 1).coerceAtLeast(0))
        val g = extendSentenceGroups.getOrNull(gi) ?: return
        if (!ttsReady || textToSpeech == null) {
            extendSentenceControlRunning = false
            extendSentenceAdapter?.clearBlockHighlight()
            updateExtendSentenceControlBar()
            return
        }
        extendSentenceTtsGeneration++
        val gen = extendSentenceTtsGeneration
        val bnLocale = Locale("bn")
        var first = true
        for ((ri, row) in g.withIndex()) {
            val q = if (first) {
                first = false
                TextToSpeech.QUEUE_FLUSH
            } else {
                TextToSpeech.QUEUE_ADD
            }
            textToSpeech?.setLanguage(Locale.US)
            textToSpeech?.speak(ExtendSentenceText.englishPlainForSpeech(row.english), q, null, "extend_sent_${gen}_${gi}_en_$ri")
            textToSpeech?.setLanguage(bnLocale)
            textToSpeech?.speak(row.bengali, TextToSpeech.QUEUE_ADD, null, "extend_sent_${gen}_${gi}_bn_$ri")
            if (row.speakAfterBengali.isNotBlank()) {
                textToSpeech?.setLanguage(bnLocale)
                textToSpeech?.speak(row.speakAfterBengali, TextToSpeech.QUEUE_ADD, null, "extend_sent_${gen}_${gi}_ex_$ri")
            }
        }
    }

    /** Practice: English TTS only for current line; [handleExtendSentencePracticePromptDone] starts mic after prompt. */
    private fun speakExtendSentencePracticeCurrentRow() {
        if (extendSentenceMode != ConversationMode.PRACTICE) return
        val gi = extendSentenceCurrentGroupIndex.coerceIn(0, (extendSentenceGroups.size - 1).coerceAtLeast(0))
        val g = extendSentenceGroups.getOrNull(gi) ?: return
        val ri = extendSentencePracticeRowIndex
        if (ri !in g.indices) {
            extendSentenceControlRunning = false
            extendSentenceAdapter?.clearBlockHighlight()
            updateExtendSentenceControlBar()
            return
        }
        if (!ttsReady || textToSpeech == null) {
            extendSentenceControlRunning = false
            extendSentenceAdapter?.clearBlockHighlight()
            updateExtendSentenceControlBar()
            return
        }
        val row = g[ri]
        // Strip HTML first; then bracket alternates for TTS. Match against rawPlain so we don't compare HTML tag noise.
        val rawPlain = ExtendSentenceText.englishPlainForSpeech(row.english)
        val toSpeak = MatchNormalizer.textForSpeakAndDisplay(rawPlain)
        expectedEnglishForVerification = rawPlain
        extendSentencePracticeGen++
        val gen = extendSentencePracticeGen
        val header = extendSentenceHeaderAdapterPositions.getOrNull(gi) ?: return
        val pos = header + 1 + ri
        extendSentenceRecycler?.smoothScrollToPosition(pos)
        extendSentenceAdapter?.setHighlightedAdapterPosition(pos)
        textToSpeech?.stop()
        cancelExtendSentencePracticeVerification()
        textToSpeech?.setLanguage(Locale.US)
        textToSpeech?.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "extend_practice_${gen}_${gi}_$ri")
    }

    private fun handleExtendSentencePracticePromptDone(utteranceId: String) {
        val m = extendPracticePromptRegex.matchEntire(utteranceId) ?: return
        val gen = m.groupValues[1].toInt()
        val gi = m.groupValues[2].toInt()
        val ri = m.groupValues[3].toInt()
        if (gen != extendSentencePracticeGen) return
        if (gi != extendSentenceCurrentGroupIndex) return
        if (ri != extendSentencePracticeRowIndex) return
        if (currentContentLayout != ContentLayout.EXTEND_SENTENCE || extendSentenceMode != ConversationMode.PRACTICE) return
        if (!extendSentenceControlRunning || extendSentenceControlPaused) return
        val g = extendSentenceGroups.getOrNull(gi) ?: return
        val row = g.getOrNull(ri) ?: return
        val rawPlain = ExtendSentenceText.englishPlainForSpeech(row.english)
        startVerificationListening(rawPlain)
    }

    private fun onExtendSentencePracticeVerificationResult(@Suppress("UNUSED_PARAMETER") match: Boolean, said: String) {
        if (currentContentLayout != ContentLayout.EXTEND_SENTENCE || extendSentenceMode != ConversationMode.PRACTICE) return
        val gi = extendSentenceCurrentGroupIndex.coerceIn(0, (extendSentenceGroups.size - 1).coerceAtLeast(0))
        val g = extendSentenceGroups.getOrNull(gi) ?: return
        val ri = extendSentencePracticeRowIndex
        if (ri !in g.indices) return
        val row = g[ri]
        val rawPlain = ExtendSentenceText.englishPlainForSpeech(row.english)
        val matchResolved = MatchNormalizer.matchesExpectedWithAlternates(rawPlain, said)
        runOnUiThread {
            Toast.makeText(
                this,
                if (matchResolved) getString(R.string.correct) else getString(R.string.incorrect),
                Toast.LENGTH_SHORT
            ).show()
        }
        val header = extendSentenceHeaderAdapterPositions.getOrNull(gi) ?: return
        val adapterPos = header + 1 + ri
        runOnUiThread {
            extendSentenceAdapter?.setPracticeSpokenAt(adapterPos, MatchNormalizer.sanitizeSpokenTextForDisplay(said))
            extendSentenceAdapter?.setPracticeResultAt(adapterPos, matchResolved)
        }

        if (matchResolved) {
            extendSentencePracticeIncorrectStreak = 0
            if (ri < g.lastIndex) {
                extendSentencePracticeRowIndex = ri + 1
                verificationHandler.postDelayed({
                    if (!isDestroyed && currentContentLayout == ContentLayout.EXTEND_SENTENCE &&
                        extendSentenceMode == ConversationMode.PRACTICE && extendSentenceControlRunning && !extendSentenceControlPaused
                    ) {
                        speakExtendSentencePracticeCurrentRow()
                    }
                }, 1500)
            } else {
                extendSentenceControlRunning = false
                extendSentenceAdapter?.clearBlockHighlight()
                runOnUiThread { updateExtendSentenceControlBar() }
            }
        } else {
            extendSentencePracticeIncorrectStreak++
            if (extendSentencePracticeIncorrectStreak >= 3) {
                extendSentencePracticeIncorrectStreak = 0
                if (ri < g.lastIndex) {
                    extendSentencePracticeRowIndex = ri + 1
                    verificationHandler.postDelayed({
                        if (!isDestroyed && currentContentLayout == ContentLayout.EXTEND_SENTENCE &&
                            extendSentenceMode == ConversationMode.PRACTICE && extendSentenceControlRunning && !extendSentenceControlPaused
                        ) {
                            speakExtendSentencePracticeCurrentRow()
                        }
                    }, 1500)
                } else {
                    extendSentenceControlRunning = false
                    extendSentenceAdapter?.clearBlockHighlight()
                    runOnUiThread { updateExtendSentenceControlBar() }
                }
            } else {
                verificationHandler.postDelayed({
                    if (!isDestroyed && currentContentLayout == ContentLayout.EXTEND_SENTENCE &&
                        extendSentenceMode == ConversationMode.PRACTICE && extendSentenceControlRunning && !extendSentenceControlPaused
                    ) {
                        speakExtendSentencePracticeCurrentRow()
                    }
                }, 1500)
            }
        }
    }

    /** TEST: TTS only first English line + instruction; then mic for remaining lines. */
    private fun speakExtendSentenceTestIntro() {
        if (extendSentenceMode != ConversationMode.TEST) return
        val gi = extendSentenceCurrentGroupIndex.coerceIn(0, (extendSentenceGroups.size - 1).coerceAtLeast(0))
        val g = extendSentenceGroups.getOrNull(gi) ?: return
        extendSentenceTestIntroCompleted = false
        extendSentenceTestGen++
        val gen = extendSentenceTestGen
        if (!ttsReady || textToSpeech == null) {
            extendSentenceControlRunning = false
            extendSentenceAdapter?.clearBlockHighlight()
            updateExtendSentenceControlBar()
            return
        }
        val header = extendSentenceHeaderAdapterPositions.getOrNull(gi) ?: return
        textToSpeech?.stop()
        cancelExtendSentencePracticeVerification()
        textToSpeech?.setLanguage(Locale.US)
        if (g.size == 1) {
            val row = g[0]
            val rawPlain = ExtendSentenceText.englishPlainForSpeech(row.english)
            val toSpeak = MatchNormalizer.textForSpeakAndDisplay(rawPlain)
            val pos0 = header + 1 + 0
            extendSentenceRecycler?.smoothScrollToPosition(pos0)
            extendSentenceAdapter?.setHighlightedAdapterPosition(pos0)
            textToSpeech?.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "extend_test_${gen}_${gi}_only")
            return
        }
        val row0 = g[0]
        val raw0 = ExtendSentenceText.englishPlainForSpeech(row0.english)
        val toSpeak0 = MatchNormalizer.textForSpeakAndDisplay(raw0)
        val pos0 = header + 1 + 0
        extendSentenceRecycler?.smoothScrollToPosition(pos0)
        extendSentenceAdapter?.setHighlightedAdapterPosition(pos0)
        textToSpeech?.speak(toSpeak0, TextToSpeech.QUEUE_FLUSH, null, "extend_test_${gen}_${gi}_first")
        textToSpeech?.speak(
            getString(R.string.extend_sentence_test_instruction),
            TextToSpeech.QUEUE_ADD,
            null,
            "extend_test_${gen}_${gi}_instr"
        )
    }

    private fun handleExtendSentenceTestUtteranceDone(utteranceId: String) {
        val m = extendTestUtteranceRegex.matchEntire(utteranceId) ?: return
        val gen = m.groupValues[1].toInt()
        val gi = m.groupValues[2].toInt()
        val kind = m.groupValues[3]
        if (gen != extendSentenceTestGen) return
        if (gi != extendSentenceCurrentGroupIndex) return
        if (currentContentLayout != ContentLayout.EXTEND_SENTENCE || extendSentenceMode != ConversationMode.TEST) return
        if (!extendSentenceControlRunning || extendSentenceControlPaused) return
        when (kind) {
            "only" -> {
                extendSentenceControlRunning = false
                extendSentenceAdapter?.clearBlockHighlight()
                runOnUiThread { updateExtendSentenceControlBar() }
            }
            "first" -> { /* wait for instruction utterance */ }
            "instr" -> {
                extendSentenceTestIntroCompleted = true
                startExtendSentenceTestListeningForCurrentRow()
            }
        }
    }

    private fun startExtendSentenceTestListeningForCurrentRow() {
        if (extendSentenceMode != ConversationMode.TEST) return
        if (!extendSentenceControlRunning || extendSentenceControlPaused) return
        val gi = extendSentenceCurrentGroupIndex.coerceIn(0, (extendSentenceGroups.size - 1).coerceAtLeast(0))
        val g = extendSentenceGroups.getOrNull(gi) ?: return
        val ri = extendSentenceTestListeningRowIndex
        if (ri !in g.indices) {
            extendSentenceControlRunning = false
            extendSentenceAdapter?.clearBlockHighlight()
            runOnUiThread { updateExtendSentenceControlBar() }
            return
        }
        val row = g[ri]
        val rawPlain = ExtendSentenceText.englishPlainForSpeech(row.english)
        expectedEnglishForVerification = rawPlain
        val header = extendSentenceHeaderAdapterPositions.getOrNull(gi) ?: return
        val pos = header + 1 + ri
        extendSentenceRecycler?.smoothScrollToPosition(pos)
        extendSentenceAdapter?.setHighlightedAdapterPosition(pos)
        extendSentenceAdapter?.setTestListeningRowInGroup(ri)
        textToSpeech?.stop()
        cancelExtendSentencePracticeVerification()
        startVerificationListening(rawPlain)
    }

    private fun onExtendSentenceTestVerificationResult(@Suppress("UNUSED_PARAMETER") match: Boolean, said: String) {
        if (currentContentLayout != ContentLayout.EXTEND_SENTENCE || extendSentenceMode != ConversationMode.TEST) return
        val gi = extendSentenceCurrentGroupIndex.coerceIn(0, (extendSentenceGroups.size - 1).coerceAtLeast(0))
        val g = extendSentenceGroups.getOrNull(gi) ?: return
        val ri = extendSentenceTestListeningRowIndex
        if (ri !in g.indices) return
        val row = g[ri]
        val rawPlain = ExtendSentenceText.englishPlainForSpeech(row.english)
        val matchResolved = MatchNormalizer.matchesExpectedWithAlternates(rawPlain, said)
        runOnUiThread {
            Toast.makeText(
                this,
                if (matchResolved) getString(R.string.correct) else getString(R.string.incorrect),
                Toast.LENGTH_SHORT
            ).show()
        }
        val header = extendSentenceHeaderAdapterPositions.getOrNull(gi) ?: return
        val adapterPos = header + 1 + ri
        runOnUiThread {
            extendSentenceAdapter?.setSessionSpokenAt(adapterPos, MatchNormalizer.sanitizeSpokenTextForDisplay(said))
            extendSentenceAdapter?.setSessionResultAt(adapterPos, matchResolved)
        }

        if (matchResolved) {
            extendSentenceTestIncorrectStreak = 0
            if (ri < g.lastIndex) {
                extendSentenceTestListeningRowIndex = ri + 1
                extendSentenceAdapter?.setTestListeningRowInGroup(extendSentenceTestListeningRowIndex)
                verificationHandler.postDelayed({
                    if (!isDestroyed && currentContentLayout == ContentLayout.EXTEND_SENTENCE &&
                        extendSentenceMode == ConversationMode.TEST && extendSentenceControlRunning && !extendSentenceControlPaused
                    ) {
                        startExtendSentenceTestListeningForCurrentRow()
                    }
                }, 1500)
            } else {
                extendSentenceControlRunning = false
                extendSentenceAdapter?.clearBlockHighlight()
                runOnUiThread { updateExtendSentenceControlBar() }
            }
        } else {
            extendSentenceTestIncorrectStreak++
            if (extendSentenceTestIncorrectStreak >= 3) {
                extendSentenceTestIncorrectStreak = 0
                if (ri < g.lastIndex) {
                    extendSentenceTestListeningRowIndex = ri + 1
                    extendSentenceAdapter?.setTestListeningRowInGroup(extendSentenceTestListeningRowIndex)
                    verificationHandler.postDelayed({
                        if (!isDestroyed && currentContentLayout == ContentLayout.EXTEND_SENTENCE &&
                            extendSentenceMode == ConversationMode.TEST && extendSentenceControlRunning && !extendSentenceControlPaused
                        ) {
                            startExtendSentenceTestListeningForCurrentRow()
                        }
                    }, 1500)
                } else {
                    extendSentenceControlRunning = false
                    extendSentenceAdapter?.clearBlockHighlight()
                    runOnUiThread { updateExtendSentenceControlBar() }
                }
            } else {
                verificationHandler.postDelayed({
                    if (!isDestroyed && currentContentLayout == ContentLayout.EXTEND_SENTENCE &&
                        extendSentenceMode == ConversationMode.TEST && extendSentenceControlRunning && !extendSentenceControlPaused
                    ) {
                        startExtendSentenceTestListeningForCurrentRow()
                    }
                }, 1500)
            }
        }
    }

    /** Preposition lesson blocks: each block has 5 lines in asset; line 5 is spoken-only guidance. */
    private fun loadPrepositionBlocksLesson(assetPath: String, displayTitle: String) {
        val content = try {
            assets.open(assetPath).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not load $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        val rows = LessonFileParsers.parsePrepositionBlocks(content)
        if (rows.isEmpty()) {
            Toast.makeText(this, "No valid blocks in $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        lessonName = displayTitle
        updateLessonTopicDisplay()
        val view = switchContentLayout(ContentLayout.PREPOSITION_BLOCKS)
        setupPrepositionBlocksLayout(view, rows)
    }

    private fun setupPrepositionBlocksLayout(root: View, rows: List<PrepositionBlockRow>) {
        prepositionBlockRows = rows
        prepositionBlockCurrentIndex = 0
        prepositionBlocksMode = ConversationMode.LEARNING
        prepositionBlocksRoot = root
        val recycler = root.findViewById<RecyclerView>(R.id.preposition_blocks_recycler)
        prepositionBlocksRecycler = recycler
        recycler.layoutManager = LinearLayoutManager(this)
        prepositionBlocksAdapter = PrepositionBlocksAdapter(rows).also { recycler.adapter = it }
        val prev = root.findViewById<ImageButton>(R.id.preposition_blocks_prev)
        val next = root.findViewById<ImageButton>(R.id.preposition_blocks_next)
        prev.visibility = View.GONE
        next.visibility = View.GONE
        fun navigateBlock(delta: Int) {
            val target = prepositionBlockCurrentIndex + delta
            if (target < 0 || target > rows.lastIndex) return
            cancelPrepositionBlocksAutoAdvance()
            prepositionBlockCurrentIndex = target
            recycler.smoothScrollToPosition(prepositionBlockCurrentIndex)
            updatePrepositionBlocksNavUI()
        }
        prev.setOnClickListener { navigateBlock(-1) }
        next.setOnClickListener { navigateBlock(1) }
        updatePrepositionBlocksNavUI()
        setupPrepositionModeBar(root, ::navigateBlock)
        root.findViewById<View>(R.id.lesson_base_control_include)?.visibility = View.VISIBLE
        updatePrepositionBlocksControlBar()
    }

    private fun setupPrepositionModeBar(root: View, navigateBlock: (Int) -> Unit) {
        val learningBtn = root.findViewById<Button>(R.id.lesson_mode_learning)
        val practiceBtn = root.findViewById<Button>(R.id.lesson_mode_practice)
        val testBtn = root.findViewById<Button>(R.id.lesson_mode_test)
        val vocabBtn = root.findViewById<Button>(R.id.lesson_mode_vocab)
        val prepPrev = root.findViewById<ImageButton>(R.id.lesson_mode_bar_prev)
        val prepNext = root.findViewById<ImageButton>(R.id.lesson_mode_bar_next)
        fun stopPlaybackIfModeSwitch() {
            if (prepositionBlocksControlRunning || prepositionBlocksControlPaused) {
                cancelPrepositionBlocksAutoAdvance()
                textToSpeech?.stop()
                prepositionBlocksControlRunning = false
                prepositionBlocksControlPaused = false
                prepositionBlocksAdapter?.clearHighlight()
                updatePrepositionBlocksControlBar()
            }
        }
        fun setMode(mode: ConversationMode) {
            if (prepositionBlocksMode != mode) stopPlaybackIfModeSwitch()
            prepositionBlocksMode = mode
            updatePrepositionModeTabAppearance(root)
        }
        learningBtn?.setOnClickListener { setMode(ConversationMode.LEARNING) }
        practiceBtn?.setOnClickListener { setMode(ConversationMode.PRACTICE) }
        testBtn?.setOnClickListener { setMode(ConversationMode.TEST) }
        vocabBtn?.setOnClickListener { setMode(ConversationMode.VOCAB) }
        prepPrev?.setOnClickListener { navigateBlock(-1) }
        prepNext?.setOnClickListener { navigateBlock(1) }
        updatePrepositionModeTabAppearance(root)
    }

    private fun updatePrepositionModeTabAppearance(root: View) {
        val learningBtn = root.findViewById<TextView>(R.id.lesson_mode_learning)
        val practiceBtn = root.findViewById<TextView>(R.id.lesson_mode_practice)
        val testBtn = root.findViewById<TextView>(R.id.lesson_mode_test)
        val vocabBtn = root.findViewById<TextView>(R.id.lesson_mode_vocab)
        val white = 0xFFFFFFFF.toInt()
        val darkText = 0xFF555555.toInt()
        val m = prepositionBlocksMode
        val sel = R.drawable.bg_lesson_mode_tab_selected
        val unsel = R.drawable.bg_lesson_mode_tab_unselected
        learningBtn?.setBackgroundResource(if (m == ConversationMode.LEARNING) sel else unsel)
        learningBtn?.setTextColor(if (m == ConversationMode.LEARNING) white else darkText)
        practiceBtn?.setBackgroundResource(if (m == ConversationMode.PRACTICE) sel else unsel)
        practiceBtn?.setTextColor(if (m == ConversationMode.PRACTICE) white else darkText)
        testBtn?.setBackgroundResource(if (m == ConversationMode.TEST) sel else unsel)
        testBtn?.setTextColor(if (m == ConversationMode.TEST) white else darkText)
        vocabBtn?.setBackgroundResource(if (m == ConversationMode.VOCAB) sel else unsel)
        vocabBtn?.setTextColor(if (m == ConversationMode.VOCAB) white else darkText)
    }

    private fun updatePrepositionBlocksControlBar() {
        controlStartStopButton?.let { ControlBarUtils.setControlStartStopButton(this, it, prepositionBlocksControlRunning) }
        controlPauseResumeButton?.let { ControlBarUtils.setControlPauseResumeButton(this, it, prepositionBlocksControlPaused) }
    }

    private fun onPrepositionBlocksStartStop() {
        if (prepositionBlockRows.isEmpty()) return
        if (prepositionBlocksControlRunning) {
            cancelPrepositionBlocksAutoAdvance()
            textToSpeech?.stop()
            prepositionBlocksControlRunning = false
            prepositionBlocksControlPaused = false
            prepositionBlocksAdapter?.clearHighlight()
            updatePrepositionBlocksControlBar()
            return
        }
        if (prepositionBlocksMode != ConversationMode.LEARNING) {
            Toast.makeText(this, "Switch to Learning to play audio", Toast.LENGTH_SHORT).show()
            return
        }
        prepositionBlocksControlRunning = true
        prepositionBlocksControlPaused = false
        updatePrepositionBlocksControlBar()
        speakPrepositionCurrentBlock()
    }

    private fun onPrepositionBlocksPauseResume() {
        if (prepositionBlockRows.isEmpty() || !prepositionBlocksControlRunning) return
        if (prepositionBlocksControlPaused) {
            prepositionBlocksControlPaused = false
            updatePrepositionBlocksControlBar()
            speakPrepositionCurrentBlock()
        } else {
            cancelPrepositionBlocksAutoAdvance()
            textToSpeech?.stop()
            prepositionBlocksControlPaused = true
            prepositionBlocksAdapter?.clearHighlight()
            updatePrepositionBlocksControlBar()
        }
    }

    private fun cancelPrepositionBlocksAutoAdvance() {
        prepositionBlocksAutoAdvanceRunnable?.let { verificationHandler.removeCallbacks(it) }
        prepositionBlocksAutoAdvanceRunnable = null
    }

    private fun updatePrepositionBlocksNavUI() {
        val root = prepositionBlocksRoot ?: return
        if (currentContentLayout != ContentLayout.PREPOSITION_BLOCKS) return
        val rows = prepositionBlockRows
        if (rows.isEmpty()) return
        val titleTv = root.findViewById<TextView>(R.id.preposition_blocks_title) ?: return
        val prev = root.findViewById<ImageButton>(R.id.preposition_blocks_prev)
        val next = root.findViewById<ImageButton>(R.id.preposition_blocks_next)
        val total = rows.size.coerceAtLeast(1)
        titleTv.text = "Block ${prepositionBlockCurrentIndex + 1} / $total"
        prev?.isEnabled = prepositionBlockCurrentIndex > 0
        next?.isEnabled = prepositionBlockCurrentIndex < rows.lastIndex
        val prepPrev = root.findViewById<ImageButton>(R.id.lesson_mode_bar_prev)
        val prepNext = root.findViewById<ImageButton>(R.id.lesson_mode_bar_next)
        prepPrev?.isEnabled = prepositionBlockCurrentIndex > 0
        prepNext?.isEnabled = prepositionBlockCurrentIndex < rows.lastIndex
    }

    /** Magenta-bar lessons: red border on the row/block while TTS speaks it ([bg_threecol_row_active]). */
    private fun handleUtteranceStartBlockHighlight(utteranceId: String) {
        extendSentenceUtteranceEnStartRegex.matchEntire(utteranceId)?.let { m ->
            val gen = m.groupValues[1].toInt()
            val gi = m.groupValues[2].toInt()
            val ri = m.groupValues[3].toInt()
            if (gen != extendSentenceTtsGeneration) return
            if (gi != extendSentenceCurrentGroupIndex) return
            val header = extendSentenceHeaderAdapterPositions.getOrNull(gi) ?: return
            val pos = header + 1 + ri
            val count = extendSentenceAdapter?.itemCount ?: 0
            if (pos in 0 until count) {
                extendSentenceRecycler?.smoothScrollToPosition(pos)
                extendSentenceAdapter?.setHighlightedAdapterPosition(pos)
            }
        }
        prepBlockHeadUtteranceRegex.matchEntire(utteranceId)?.let { m ->
            val gen = m.groupValues[1].toInt()
            val idx = m.groupValues[2].toInt()
            if (gen != prepositionBlocksTtsGeneration) return
            prepositionBlocksAdapter?.setHighlightedPosition(idx)
            prepositionBlocksRecycler?.smoothScrollToPosition(idx)
        }
    }

    /** Clear red border when the last TTS segment of the current extend-sentence group finishes. */
    private fun handleExtendSentenceUtteranceDoneForHighlight(utteranceId: String) {
        if (currentContentLayout != ContentLayout.EXTEND_SENTENCE) return
        val m = extendSentenceUtteranceSegmentRegex.matchEntire(utteranceId) ?: return
        val gen = m.groupValues[1].toInt()
        val gi = m.groupValues[2].toInt()
        val part = m.groupValues[3]
        val ri = m.groupValues[4].toInt()
        if (gen != extendSentenceTtsGeneration) return
        val g = extendSentenceGroups.getOrNull(gi) ?: return
        if (ri != g.lastIndex) return
        val row = g[ri]
        val isLastSegmentOfGroup = when (part) {
            "ex" -> true
            "bn" -> row.speakAfterBengali.isBlank()
            else -> false
        }
        if (isLastSegmentOfGroup) {
            extendSentenceAdapter?.clearBlockHighlight()
        }
    }

    /** After the last TTS segment of a block, auto-advance to the next block after 2s (or finish). */
    private fun handlePrepositionBlockUtteranceFinished(utteranceId: String, @Suppress("UNUSED_PARAMETER") fromError: Boolean) {
        if (isDestroyed || currentContentLayout != ContentLayout.PREPOSITION_BLOCKS) return
        val m = prepositionBlockUtteranceRegex.matchEntire(utteranceId) ?: return
        val gen = m.groupValues[1].toInt()
        val idx = m.groupValues[2].toInt()
        val part = m.groupValues[3]
        if (gen != prepositionBlocksTtsGeneration) return
        if (idx != prepositionBlockCurrentIndex) return
        if (prepositionBlocksMode != ConversationMode.LEARNING) return
        if (!prepositionBlocksControlRunning || prepositionBlocksControlPaused) return
        val row = prepositionBlockRows.getOrNull(idx) ?: return
        val isLast = when {
            row.spokenGuidance.isNotBlank() -> part == "guidance"
            else -> part == "ex2"
        }
        if (!isLast) return
        schedulePrepositionBlocksAutoAdvanceAfterBlock(gen)
    }

    private fun schedulePrepositionBlocksAutoAdvanceAfterBlock(gen: Int) {
        prepositionBlocksAutoAdvanceRunnable?.let { verificationHandler.removeCallbacks(it) }
        prepositionBlocksAutoAdvanceRunnable = Runnable {
            prepositionBlocksAutoAdvanceRunnable = null
            if (isDestroyed || currentContentLayout != ContentLayout.PREPOSITION_BLOCKS) return@Runnable
            if (prepositionBlocksMode != ConversationMode.LEARNING) return@Runnable
            if (!prepositionBlocksControlRunning || prepositionBlocksControlPaused) return@Runnable
            if (gen != prepositionBlocksTtsGeneration) return@Runnable
            val rows = prepositionBlockRows
            if (rows.isEmpty()) return@Runnable
            if (prepositionBlockCurrentIndex >= rows.lastIndex) {
                prepositionBlocksControlRunning = false
                prepositionBlocksControlPaused = false
                textToSpeech?.stop()
                prepositionBlocksAdapter?.clearHighlight()
                updatePrepositionBlocksControlBar()
                return@Runnable
            }
            prepositionBlockCurrentIndex++
            prepositionBlocksRecycler?.smoothScrollToPosition(prepositionBlockCurrentIndex)
            updatePrepositionBlocksNavUI()
            speakPrepositionCurrentBlock()
        }
        verificationHandler.postDelayed(prepositionBlocksAutoAdvanceRunnable!!, 2000)
    }

    /** Speak current block: line1+2+3+4 shown in UI; line5 hidden but spoken in practice. */
    private fun speakPrepositionCurrentBlock() {
        val idx = prepositionBlockCurrentIndex.coerceIn(0, (prepositionBlockRows.size - 1).coerceAtLeast(0))
        val row = prepositionBlockRows.getOrNull(idx) ?: return
        if (!ttsReady || textToSpeech == null) {
            prepositionBlocksControlRunning = false
            updatePrepositionBlocksControlBar()
            return
        }
        cancelPrepositionBlocksAutoAdvance()
        prepositionBlocksTtsGeneration++
        val gen = prepositionBlocksTtsGeneration
        prepositionBlocksAdapter?.setHighlightedPosition(idx)
        prepositionBlocksRecycler?.smoothScrollToPosition(idx)
        val idSuffix = { part: String -> "prep_block_${gen}_${idx}_$part" }
        textToSpeech?.setLanguage(Locale.US)
        textToSpeech?.speak(row.preposition, TextToSpeech.QUEUE_FLUSH, null, idSuffix("head"))
        textToSpeech?.setLanguage(Locale("bn"))
        textToSpeech?.speak(row.meaning, TextToSpeech.QUEUE_ADD, null, idSuffix("meaning"))
        textToSpeech?.setLanguage(Locale.US)
        textToSpeech?.speak(row.example1, TextToSpeech.QUEUE_ADD, null, idSuffix("ex1"))
        textToSpeech?.speak(row.example2, TextToSpeech.QUEUE_ADD, null, idSuffix("ex2"))
        if (row.spokenGuidance.isNotBlank()) {
            textToSpeech?.setLanguage(Locale("bn"))
            textToSpeech?.speak(row.spokenGuidance, TextToSpeech.QUEUE_ADD, null, idSuffix("guidance"))
        }
    }

    /** Load simple tense triplets and show as table-like rows: Present | Past | Future. */
    private fun loadSimpleTenseTripletLessonFromAsset(assetPath: String, displayTitle: String) {
        val content = try {
            assets.open(assetPath).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not load $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        val rows = LessonFileParsers.parseSimpleTenseTriplets(content)
        if (rows.isEmpty()) {
            Toast.makeText(this, "No valid triplets in $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        tenseTripletRows = rows
        lessonVocabRows = buildTenseTripletVocabRows(rows)
        tenseTripletMode = TenseTripletMode.LEARNING
        tenseTripletCurrentIndex = 0
        tenseTripletLastScrolledPosition = -1
        tenseTripletCachedRowHeightPx = -1
        tenseTripletShowPresent = true
        tenseTripletShowPast = true
        tenseTripletShowFuture = true
        tenseTripletBengaliFirst = false
        tenseTripletAdjectiveDualMode = false
        lessonName = displayTitle
        lessonRows = null
        clearPronunciationLessonState()
        updateLessonTopicDisplay()

        val root = ensureTenseLessonRoot(R.layout.layout_tense_triplets_content)
        val recycler = root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tense_triplet_recycler)
        if (recycler != null) {
            tenseTripletAdapter = TenseTripletAdapter(tenseTripletRows, R.layout.layout_item_tense_triplet, alwaysShowFutureCell = false, adjectiveDualMode = false)
            recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            recycler.adapter = tenseTripletAdapter
            applyTenseTripletModeToAdapter()
            tenseTripletAdapter?.setColumnVisibility(tenseTripletShowPresent, tenseTripletShowPast, tenseTripletShowFuture)
            tenseTripletAdapter?.setCurrentIndex(tenseTripletCurrentIndex)
            updateTenseTripletAutoScrollPosition(forceTop = true)
        }
        // Fresh lesson: no stale V-tab ticks / "You said" from another screen.
        lessonVocabAdapter?.clearSessionMarksAndSpoken()
        setupTenseTripletModeButtons(root)
        setupTenseTripletColumnToggles(root)
        // Keep default universal bars: top bar always visible, bottom bar remains visible for this layout.
    }

    /** Load duplex tense rows (Positive | Negative) using same tense tabs/control behavior. */
    private fun loadSimpleTenseDuplexLessonFromAsset(assetPath: String, displayTitle: String) {
        val content = try {
            assets.open(assetPath).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not load $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        val rows = LessonFileParsers.parseSimpleTenseDuplex(content)
        if (rows.isEmpty()) {
            Toast.makeText(this, "No valid duplex rows in $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        tenseTripletRows = rows
        lessonVocabRows = buildTenseTripletVocabRows(rows)
        tenseTripletMode = TenseTripletMode.LEARNING
        tenseTripletCurrentIndex = 0
        tenseTripletLastScrolledPosition = -1
        tenseTripletCachedRowHeightPx = -1
        tenseTripletShowPresent = true
        tenseTripletShowPast = true
        tenseTripletShowFuture = false
        tenseTripletBengaliFirst = false
        tenseTripletAdjectiveDualMode = false
        lessonName = displayTitle
        lessonRows = null
        clearPronunciationLessonState()
        updateLessonTopicDisplay()

        val root = ensureTenseLessonRoot(R.layout.layout_tense_duplex_content)
        val recycler = root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tense_triplet_recycler)
        if (recycler != null) {
            tenseTripletAdapter = TenseTripletAdapter(tenseTripletRows, R.layout.layout_item_tense_triplet, alwaysShowFutureCell = false, adjectiveDualMode = false)
            recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            recycler.adapter = tenseTripletAdapter
            applyTenseTripletModeToAdapter()
            tenseTripletAdapter?.setColumnVisibility(tenseTripletShowPresent, tenseTripletShowPast, tenseTripletShowFuture)
            tenseTripletAdapter?.setCurrentIndex(tenseTripletCurrentIndex)
            updateTenseTripletAutoScrollPosition(forceTop = true)
        }
        lessonVocabAdapter?.clearSessionMarksAndSpoken()
        setupTenseTripletModeButtons(root)
        setupTenseTripletColumnToggles(root)
    }

    private fun loadSimpleAdjectiveDualLessonFromAsset(assetPath: String, displayTitle: String) {
        val content = try {
            assets.open(assetPath).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not load $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        val rows = LessonFileParsers.parseSimpleAdjectiveDual(content)
        if (rows.isEmpty()) {
            Toast.makeText(this, "No valid rows in $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        tenseTripletRows = rows
        lessonVocabRows = buildTenseTripletVocabRows(rows)
        tenseTripletMode = TenseTripletMode.LEARNING
        tenseTripletCurrentIndex = 0
        tenseTripletLastScrolledPosition = -1
        tenseTripletCachedRowHeightPx = -1
        tenseTripletShowPresent = true
        tenseTripletShowPast = true
        tenseTripletShowFuture = false
        tenseTripletBengaliFirst = true
        tenseTripletAdjectiveDualMode = true
        lessonName = displayTitle
        lessonRows = null
        clearPronunciationLessonState()
        updateLessonTopicDisplay()

        val root = ensureTenseLessonRoot(R.layout.layout_tense_adjective_dual_content, forceReplace = true)
        val recycler = root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tense_triplet_recycler)
        if (recycler != null) {
            tenseTripletAdapter = TenseTripletAdapter(
                tenseTripletRows,
                R.layout.layout_item_tense_adjective_dual,
                alwaysShowFutureCell = true,
                adjectiveDualMode = true
            )
            recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            recycler.adapter = tenseTripletAdapter
            applyTenseTripletModeToAdapter()
            tenseTripletAdapter?.setColumnVisibility(tenseTripletShowPresent, tenseTripletShowPast, tenseTripletShowFuture)
            tenseTripletAdapter?.setCurrentIndex(tenseTripletCurrentIndex)
            updateTenseTripletAutoScrollPosition(forceTop = true)
        }
        lessonVocabAdapter?.clearSessionMarksAndSpoken()
        setupTenseTripletModeButtons(root)
        setupTenseTripletColumnToggles(root)
    }

    /**
     * Ensures the tense-triplet lesson shell ([layout_lesson_base]) with the given **content** layout
     * inside [R.id.lesson_base_content]. Mode bar + bottom control always come from the base shell.
     */
    private fun ensureTenseLessonRoot(layoutRes: Int, forceReplace: Boolean = false): View {
        if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) {
            switchContentLayout(ContentLayout.TENSE_TRIPLETS)
        }
        val root = contentFrame.getChildAt(0) ?: return contentFrame
        if (!forceReplace && tenseTripletInflatedContentRes == layoutRes) {
            return root
        }
        val host = root.findViewById<android.widget.FrameLayout>(R.id.lesson_base_content)
            ?: return root
        host.removeAllViews()
        layoutInflater.inflate(layoutRes, host, true)
        tenseTripletInflatedContentRes = layoutRes
        val activeBar = root.findViewById<View>(R.id.lesson_base_control_include)
        controlActionsBar = activeBar
        controlStartStopButton = activeBar?.findViewById(R.id.control_start_stop)
        controlPauseResumeButton = activeBar?.findViewById(R.id.control_pause_resume)
        controlPlaybackLastButton = activeBar?.findViewById(R.id.control_playback_last)
        setupHoldToRecordPlaybackButton(controlPlaybackLastButton)
        bindControlBarListeners()
        findViewById<View>(R.id.control_actions_include)?.visibility = View.GONE
        activeBar?.visibility = View.VISIBLE
        findViewById<View>(R.id.bottom_bar)?.visibility = View.GONE
        updateTenseTripletControlBar()
        return root
    }

    private fun setupTenseTripletColumnToggles(root: View) {
        val presentCheck = root.findViewById<CheckBox>(R.id.tense_triplet_show_present)
        val pastCheck = root.findViewById<CheckBox>(R.id.tense_triplet_show_past)
        val futureCheck = root.findViewById<CheckBox>(R.id.tense_triplet_show_future)
        presentCheck?.setOnCheckedChangeListener(null)
        pastCheck?.setOnCheckedChangeListener(null)
        futureCheck?.setOnCheckedChangeListener(null)
        presentCheck?.isChecked = tenseTripletShowPresent
        pastCheck?.isChecked = tenseTripletShowPast
        futureCheck?.isChecked = tenseTripletShowFuture
        val listener = CompoundButton.OnCheckedChangeListener { _, _ ->
            tenseTripletShowPresent = presentCheck?.isChecked == true
            tenseTripletShowPast = pastCheck?.isChecked == true
            tenseTripletShowFuture = futureCheck?.isChecked == true
            tenseTripletAdapter?.setColumnVisibility(tenseTripletShowPresent, tenseTripletShowPast, tenseTripletShowFuture)
            if (!tenseTripletShowPresent && !tenseTripletShowPast && !tenseTripletShowFuture) {
                tenseTripletControlRunning = false
                tenseTripletControlPaused = false
                expectedEnglishForVerification = null
                textToSpeech?.stop()
                if (verificationMode) {
                    verificationMode = false
                    try {
                        speechRecognizer?.stopListening()
                    } catch (_: Exception) { }
                    stopEnglishVoskRecording()
                    setMicButtonAppearance(recording = false)
                }
                updateTenseTripletControlBar()
            }
        }
        presentCheck?.setOnCheckedChangeListener(listener)
        pastCheck?.setOnCheckedChangeListener(listener)
        futureCheck?.setOnCheckedChangeListener(listener)
    }

    private fun buildTenseTripletVocabRows(rows: List<TenseTripletRow>): List<LessonVocabRow> {
        val wordRegex = Regex("[A-Za-z']+")
        val orderedWords = LinkedHashSet<String>()
        for (r in rows) {
            listOf(r.present.english, r.past.english, r.future.english).forEach { sentence ->
                wordRegex.findAll(sentence).forEach { m ->
                    val w = m.value.trim().lowercase()
                    if (w.isNotBlank()) orderedWords.add(w)
                }
            }
        }
        val master = masterWordListMap ?: LessonFileParsers.loadMasterWordList(assets).also { masterWordListMap = it }
        return orderedWords.map { w ->
            val meta = master[w]
            LessonVocabRow(
                word = w,
                pronunciation = meta?.second.orEmpty(),
                meaning = meta?.first.orEmpty()
            )
        }
    }

    private fun setupTenseTripletModeButtons(root: View) {
        val learningBtn = root.findViewById<Button>(R.id.lesson_mode_learning)
        val practiceBtn = root.findViewById<Button>(R.id.lesson_mode_practice)
        val testBtn = root.findViewById<Button>(R.id.lesson_mode_test)
        val vocabBtn = root.findViewById<Button>(R.id.lesson_mode_vocab)
        val prevBtn = root.findViewById<ImageButton>(R.id.lesson_mode_bar_prev)
        val nextBtn = root.findViewById<ImageButton>(R.id.lesson_mode_bar_next)
        val mainContent = root.findViewById<View>(R.id.tense_triplet_main_content)
        val vocabInclude = root.findViewById<View>(R.id.tense_triplet_vocab_include)
        val vocabRecycler = vocabInclude?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.lesson_vocab_recycler)

        setupVocabTabForCurrentLesson(vocabInclude) { updateTenseTripletAutoScrollPosition() }

        learningBtn?.setOnClickListener {
            if (tenseTripletMode == TenseTripletMode.LEARNING) return@setOnClickListener
            resetTenseTripletListToTop()
            tenseTripletMode = TenseTripletMode.LEARNING
            applyTenseTripletModeToAdapter()
            updateTenseTripletModeTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, mainContent, vocabInclude)
        }
        practiceBtn?.setOnClickListener {
            if (tenseTripletMode == TenseTripletMode.PRACTICE) return@setOnClickListener
            resetTenseTripletListToTop()
            tenseTripletMode = TenseTripletMode.PRACTICE
            applyTenseTripletModeToAdapter()
            updateTenseTripletModeTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, mainContent, vocabInclude)
        }
        testBtn?.setOnClickListener {
            if (tenseTripletMode == TenseTripletMode.TEST) return@setOnClickListener
            resetTenseTripletListToTop()
            tenseTripletMode = TenseTripletMode.TEST
            applyTenseTripletModeToAdapter()
            updateTenseTripletModeTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, mainContent, vocabInclude)
        }
        vocabBtn?.setOnClickListener {
            if (tenseTripletMode == TenseTripletMode.VOCAB) return@setOnClickListener
            resetTenseTripletListToTop()
            tenseTripletMode = TenseTripletMode.VOCAB
            lessonVocabAdapter?.currentIndex = 0
            vocabRecycler?.scrollToPosition(0)
            applyTenseTripletModeToAdapter()
            updateTenseTripletModeTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, mainContent, vocabInclude)
        }
        prevBtn?.setOnClickListener { moveTenseTripletBy(-1) }
        nextBtn?.setOnClickListener { moveTenseTripletBy(1) }

        updateTenseTripletModeTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, mainContent, vocabInclude)
    }

    /** Any mode tab (L/P/T/V) starts the triplet list at row 0 so scroll position does not carry over. */
    private fun resetTenseTripletListToTop() {
        // V tab session UI; triplet tick/cross is cleared in applyTenseTripletModeToAdapter().
        lessonVocabAdapter?.clearSessionMarksAndSpoken()
        if (tenseTripletRows.isEmpty()) return
        tenseTripletCurrentIndex = 0
        tenseTripletIncorrectCount = 0
        tenseTripletLastScrolledPosition = -1
        tenseTripletAdapter?.setCurrentIndex(0)
        val root = if (contentFrame.childCount > 0) contentFrame.getChildAt(0) else return
        root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tense_triplet_recycler)?.scrollToPosition(0)
        updateTenseTripletAutoScrollPosition(forceTop = true)
    }

    private fun applyTenseTripletModeToAdapter() {
        val adapter = tenseTripletAdapter ?: return
        adapter.displayMode = when (tenseTripletMode) {
            TenseTripletMode.LEARNING -> TenseTripletAdapter.DisplayMode.LEARNING
            TenseTripletMode.PRACTICE -> TenseTripletAdapter.DisplayMode.PRACTICE
            TenseTripletMode.TEST -> TenseTripletAdapter.DisplayMode.TEST
            TenseTripletMode.VOCAB -> TenseTripletAdapter.DisplayMode.VOCAB
        }
        // Single notify: drop tick/cross + TEST replacements whenever mode is applied (tab or lesson load).
        adapter.clearMarksAndSpokenState()
    }

    private fun moveTenseTripletBy(delta: Int) {
        if (tenseTripletRows.isEmpty()) return
        val root = if (contentFrame.childCount > 0) contentFrame.getChildAt(0) else return
        val recycler = root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tense_triplet_recycler) ?: return
        tenseTripletCurrentIndex = (tenseTripletCurrentIndex + delta).coerceIn(0, tenseTripletRows.lastIndex)
        tenseTripletAdapter?.setCurrentIndex(tenseTripletCurrentIndex)
        recycler.smoothScrollToPosition(tenseTripletCurrentIndex)
        updateTenseTripletAutoScrollPosition()
    }

    /**
     * Auto-scroll tense triplets similar to conversation bubbles:
     * keep focus row near middle while invisible rows still remain below.
     */
    private fun updateTenseTripletAutoScrollPosition(forceTop: Boolean = false) {
        if (currentContentLayout != ContentLayout.TENSE_TRIPLETS || tenseTripletRows.isEmpty()) return
        val root = if (contentFrame.childCount > 0) contentFrame.getChildAt(0) else return
        val recycler = root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tense_triplet_recycler) ?: return
        val lm = recycler.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager ?: return
        val n = tenseTripletRows.size
        val pos = tenseTripletCurrentIndex.coerceIn(0, n - 1)
        recycler.postDelayed({
            val visibleAreaHeight = recycler.height
            if (visibleAreaHeight <= 0) return@postDelayed
            var rowHeight = tenseTripletCachedRowHeightPx
            if (rowHeight <= 0) {
                val viewAtPos = lm.findViewByPosition(pos)
                val viewAt0 = lm.findViewByPosition(0)
                rowHeight = when {
                    viewAtPos != null && viewAtPos.height > 0 -> viewAtPos.height
                    viewAt0 != null && viewAt0.height > 0 -> viewAt0.height
                    else -> (visibleAreaHeight / 6).coerceAtLeast(1)
                }
                if (rowHeight > 0) tenseTripletCachedRowHeightPx = rowHeight
            }
            val centerIndex = (visibleAreaHeight / 2 / rowHeight).toInt().coerceIn(0, (n - 1).coerceAtLeast(0))
            if (forceTop || pos == 0) {
                lm.scrollToPositionWithOffset(0, 0)
                tenseTripletLastScrolledPosition = 0
                return@postDelayed
            }
            if (pos <= centerIndex) {
                lm.scrollToPositionWithOffset(0, 0)
                tenseTripletLastScrolledPosition = pos
                return@postDelayed
            }
            val lastPos = tenseTripletLastScrolledPosition
            var dy = (pos - lastPos) * rowHeight
            val maxScroll = (recycler.computeVerticalScrollRange() - recycler.computeVerticalScrollExtent()).coerceAtLeast(0)
            val currentScrollY = recycler.computeVerticalScrollOffset()
            dy = dy.coerceIn(-currentScrollY, maxScroll - currentScrollY)
            if (dy != 0) {
                var last = 0
                ValueAnimator.ofInt(0, dy).apply {
                    duration = 280
                    interpolator = DecelerateInterpolator()
                    addUpdateListener {
                        val v = it.animatedValue as Int
                        recycler.scrollBy(0, v - last)
                        last = v
                    }
                    start()
                }
            }
            tenseTripletLastScrolledPosition = pos
        }, 80)
    }

    private fun updateTenseTripletModeTabAppearance(
        learningBtn: View?,
        practiceBtn: View?,
        testBtn: View?,
        vocabBtn: View?,
        mainContent: View?,
        vocabInclude: View?
    ) {
        val white = 0xFFFFFFFF.toInt()
        val darkText = 0xFF555555.toInt()
        val sel = R.drawable.bg_lesson_mode_tab_selected
        val unsel = R.drawable.bg_lesson_mode_tab_unselected
        fun setBtn(btn: View?, selected: Boolean) {
            (btn as? TextView)?.let {
                it.setBackgroundResource(if (selected) sel else unsel)
                it.setTextColor(if (selected) white else darkText)
            }
        }
        setBtn(learningBtn, tenseTripletMode == TenseTripletMode.LEARNING)
        setBtn(practiceBtn, tenseTripletMode == TenseTripletMode.PRACTICE)
        setBtn(testBtn, tenseTripletMode == TenseTripletMode.TEST)
        setBtn(vocabBtn, tenseTripletMode == TenseTripletMode.VOCAB)
        val isVocab = tenseTripletMode == TenseTripletMode.VOCAB
        mainContent?.visibility = if (isVocab) View.GONE else View.VISIBLE
        vocabInclude?.visibility = if (isVocab) View.VISIBLE else View.GONE
        vocabBtn?.visibility = if (lessonVocabRows.isEmpty()) View.GONE else View.VISIBLE
        // Table was GONE on V tab: force rebind when shown again so tick/cross ImageViews are not stale.
        if (!isVocab) {
            mainContent?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tense_triplet_recycler)
                ?.post { tenseTripletAdapter?.notifyDataSetChanged() }
        }
    }

    /**
     * Load 3-col lesson into THREECOL_TABLE.
     *
     * DATA LOAD & IN-MEMORY STRUCTURE:
     * 1) TXT (e.g. simple_what.txt): Parsed by LessonFileParsers.parseThreeColLessonFile() → (rows, initialABPerRow).
     *    - rows: List<ThreeColRow> (english, bengali, hint). Optional 5th/6th columns = A, B (0/1).
     * 2) JSON (test_layout_3col_stats.json): LessonFileParsers.loadThreeColStats() → MutableList<IntArray>, each [A, B].
     *    - No file or missing row → default [0, 0] (initially failed).
     * 3) In memory: threeColBaseRows = rows from txt; threeColStats[i] = [A, B] (same index as base rows).
     *    Merge: if txt had A,B for row i, overwrite threeColStats[i] with that.
     *
     * UPDATE IN MEMORY: On each Practice/Test result, threeColStats[baseIdx][0]=1|0 (Practice) or [1]=1|0 (Test);
     * then save to JSON. Live stat (0/0/10 on tab start) is threeColSessionCorrect / threeColSessionAttempted / total.
     *
     * FILTER WHEN TOGGLE: "Failed only" uses in-memory A,B. Practice: show rows where A==0; Test: B==0.
     * If none (all passed), display list = empty and Toast "All passed".
     */
    private fun loadThreeColLessonFromAsset(assetPath: String, lessonKey: String, displayTitle: String? = null) {
        val content = try {
            assets.open(assetPath).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not load $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        val (rows, initialABPerRow) = LessonFileParsers.parseThreeColLessonFile(content)
        if (rows.isEmpty()) {
            Toast.makeText(this, "No rows in $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        // Always re-inflate 3-col shell when loading a lesson so switching e.g. Simple What → Simple Where
        // cannot leave a stale RecyclerView / adapter (empty area with bars still visible). Same pattern as
        // [loadConversationBubbleLesson].
        switchContentLayout(ContentLayout.THREECOL_TABLE)
        threeColAdapter = null
        threeColCurrentLessonKey = lessonKey
        threeColBaseRows = rows
        if (masterWordListMap == null) {
            masterWordListMap = LessonFileParsers.loadMasterWordList(assets)
        }
        currentLessonVocabWords = LessonFileParsers.extractVocabularyBlock(content)
        lessonVocabRows = LessonFileParsers.filterLessonVocabRowsByMaster(
            LessonFileParsers.buildLessonVocabRowsOnlyInMaster(currentLessonVocabWords, masterWordListMap ?: emptyMap()),
            masterWordListMap
        )
        threeColStats = LessonFileParsers.loadThreeColStats(filesDir, lessonKey, rows.size)
        // Merge optional A,B from lesson file when present
        for (i in rows.indices) {
            val ab = initialABPerRow.getOrNull(i)
            if (ab != null) {
                while (threeColStats.size <= i) threeColStats.add(IntArray(2) { 0 })
                threeColStats[i][0] = ab.getOrNull(0)?.coerceIn(0, 1) ?: 0
                threeColStats[i][1] = ab.getOrNull(1)?.coerceIn(0, 1) ?: 0
            }
        }
        threeColDisplayToBaseIndex = rows.indices.toList()
        threeColRows = rows
        threeColLearningMode = true
        threeColMode = ThreeColMode.LEARNING
        threeColWeakOnlyFilter = false
        threeColCurrentIndex = 0
        threeColIncorrectCount = 0
        threeColSessionCorrect = 0
        threeColSessionAttempted = 0
        threeColLastScrolledPosition = -1
        threeColCachedRowHeightPx = -1
        lessonName = displayTitle ?: lessonKey
        updateLessonTopicDisplay()
        contentFrame.post {
            if (currentContentLayout != ContentLayout.THREECOL_TABLE || contentFrame.childCount == 0) return@post
            val root = contentFrame.getChildAt(0)
            val recycler = root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.threecol_recycler)
            if (recycler != null) {
                threeColAdapter = ThreeColDataAdapter(threeColRows)
                recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
                recycler.adapter = threeColAdapter
                // Bottom padding so current row stays fully visible above the START/STOP control bar
                val controlBar = root.findViewById<View>(R.id.lesson_base_control_include)
                val bottomPaddingPx = if (controlBar != null && controlBar.height > 0) controlBar.height
                    else (72 * resources.displayMetrics.density).toInt()
                recycler.setPadding(0, recycler.paddingTop, 0, bottomPaddingPx)
                recycler.clipToPadding = true
                // Do not restore tick/cross from previous sessions; marks show only for attempts in this session.
                threeColAdapter?.learningMode = threeColLearningMode
                threeColAdapter?.setCurrentIndex(threeColCurrentIndex)
            }
            setupThreeColModeButtons(root)
            updateThreeColStats()
            updateThreeColRowPositionText()
            // Initial scroll: [updateThreeColRowPositionText] → [updateThreeColAutoScrollPosition]; pos==0 pins to top.
            // Use the control bar inside this layout; hide the activity-level bar.
            findViewById<View>(R.id.control_actions_include)?.visibility = View.GONE
            root.findViewById<View>(R.id.lesson_base_control_include)?.visibility = View.VISIBLE
        }
    }

    private fun loadConversationBubbleLesson(assetPath: String, lessonKey: String, displayTitle: String?) {
        val content = try {
            assets.open(assetPath).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not load $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        val (rows, initialABPerRow) = LessonFileParsers.parseConversationBubbleFile(content)
        if (rows.isEmpty()) {
            Toast.makeText(this, "No rows in $assetPath", Toast.LENGTH_SHORT).show()
            return
        }
        // Always re-inflate bubble shell when loading a lesson so switching e.g. First meeting → Second lesson
        // cannot leave a stale RecyclerView / tab visibility (empty area with bars still visible).
        switchContentLayout(ContentLayout.CONVERSATION_BUBBLES)
        convBubbleAdapter = null
        convBubbleCurrentLessonKey = lessonKey
        convBubbleBaseRows = rows
        val assetMaster = LessonFileParsers.loadMasterWordList(assets)
        val additions = LessonFileParsers.loadMasterListAdditions(filesDir)
        val mergedMaster = assetMaster.toMutableMap()
        mergedMaster.putAll(additions)
        val extractedWords = LessonFileParsers.extractWordsFromConversationContent(content)
        for (word in extractedWords) {
            if (word !in mergedMaster) {
                LessonFileParsers.saveMasterListAddition(filesDir, word, "—", "—")
                mergedMaster[word] = "—" to "—"
            }
        }
        masterWordListMap = mergedMaster
        currentLessonVocabWords = extractedWords
        lessonVocabRows = LessonFileParsers.buildLessonVocabRowsOnlyInMaster(extractedWords, mergedMaster)
        convBubbleStats = LessonFileParsers.loadConversationBubbleStats(filesDir, lessonKey, rows.size)
        for (i in rows.indices) {
            val ab = initialABPerRow.getOrNull(i)
            if (ab != null) {
                while (convBubbleStats.size <= i) convBubbleStats.add(IntArray(2) { 0 })
                convBubbleStats[i][0] = ab.getOrNull(0)?.coerceIn(0, 1) ?: 0
                convBubbleStats[i][1] = ab.getOrNull(1)?.coerceIn(0, 1) ?: 0
            }
        }
        convBubbleDisplayToBaseIndex = rows.indices.toList()
        convBubbleRows = rows
        convBubbleLearningMode = true
        convBubbleMode = ConversationMode.LEARNING
        convBubbleWeakOnlyFilter = false
        convBubbleCurrentIndex = 0
        convBubbleListeningForRowIndex = -1
        convBubbleAdvanceRunnable?.let { verificationHandler.removeCallbacks(it) }
        convBubbleAdvanceRunnable = null
        convBubbleIncorrectCount = 0
        convBubbleSessionCorrect = 0
        convBubbleSessionAttempted = 0
        lessonName = displayTitle ?: lessonKey
        updateLessonTopicDisplay()
        contentFrame.post {
            if (currentContentLayout != ContentLayout.CONVERSATION_BUBBLES || contentFrame.childCount == 0) return@post
            val root = contentFrame.getChildAt(0)
            val recycler = root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.conv_bubble_recycler)
            if (recycler != null) {
                convBubbleAdapter = ConversationBubbleAdapter(convBubbleRows)
                recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
                recycler.adapter = convBubbleAdapter
                recycler.setPadding(0, 0, 0, 0)
                recycler.clipToPadding = false
                convBubbleAdapter?.learningMode = convBubbleLearningMode
                convBubbleAdapter?.testMode = (convBubbleMode == ConversationMode.TEST)
                // Do not apply saved stats on load so bubbles start with no tick/cross marks
                convBubbleAdapter?.setCurrentIndex(convBubbleCurrentIndex)
                convBubbleCachedItemHeightPx = -1
                recycler.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        recycler.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        if (convBubbleCachedItemHeightPx <= 0) {
                            val viewAt0 = (recycler.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)?.findViewByPosition(0)
                            if (viewAt0 != null && viewAt0.height > 0) {
                                convBubbleCachedItemHeightPx = viewAt0.height
                            } else if (recycler.childCount > 0) {
                                val firstChild = recycler.getChildAt(0)
                                if (firstChild != null && firstChild.height > 0) {
                                    convBubbleCachedItemHeightPx = firstChild.height
                                }
                            }
                        }
                    }
                })
            }
            root.findViewById<android.widget.Switch>(R.id.conv_bubble_weak_only)?.isChecked = convBubbleWeakOnlyFilter
            setupConvBubbleModeButtons(root)
            updateConvBubbleStats()
            updateConvBubbleRowPositionText()
            // Use the control bar inside this layout; hide the activity-level bar.
            findViewById<View>(R.id.control_actions_include)?.visibility = View.GONE
            root.findViewById<View>(R.id.lesson_base_control_include)?.visibility = View.VISIBLE
            updateConvBubbleControlBar()
        }
    }

    private fun isConvBubbleWeakOrUntried(baseIdx: Int): Boolean {
        val stat = convBubbleStats.getOrNull(baseIdx) ?: intArrayOf(0, 0)
        val a = stat.getOrNull(0) ?: 0
        val b = stat.getOrNull(1) ?: 0
        return when (convBubbleMode) {
            ConversationMode.PRACTICE -> a == 0
            ConversationMode.TEST -> b == 0
            ConversationMode.LEARNING -> a == 0 || b == 0
            ConversationMode.VOCAB -> false
        }
    }

    /** Test mode: who speaks this row? App starts: A=app, B=user. User starts: user speaks first line's role (row 0), app speaks the other role (all consecutive lines of that role). */
    private fun isConvBubbleRowAppByInitiator(row: ConversationBubbleRow, @Suppress("UNUSED_PARAMETER") index: Int): Boolean {
        return if (convBubbleTestInitiatorApp) {
            row.speaker == "A"
        } else {
            val firstSpeaker = convBubbleRows.firstOrNull()?.speaker
            row.speaker != firstSpeaker
        }
    }

    /** Test mode: index to focus and start from. App starts = first app row (left); User starts = 0 (first bubble left, user speaks). */
    private fun convBubbleFirstIndexForInitiator(): Int {
        return if (convBubbleTestInitiatorApp) {
            convBubbleRows.indexOfFirst { it.speaker == "A" }.takeIf { it >= 0 } ?: 0
        } else {
            0
        }
    }

    private fun applyConvBubbleFilterForCurrentMode() {
        cancelVerificationTimeout()
        if (verificationMode) {
            verificationMode = false
            expectedEnglishForVerification = null
            try { speechRecognizer?.stopListening() } catch (_: Exception) { }
            stopEnglishVoskRecording()
            setMicButtonAppearance(recording = false)
        }
        isEnglishMicActive = false
        isRecording = false
        val indices = if (convBubbleMode == ConversationMode.TEST) {
            // Test: show all rows (no failed-only filter)
            convBubbleBaseRows.indices.toList()
        } else if (!convBubbleWeakOnlyFilter) {
            convBubbleBaseRows.indices.toList()
        } else {
            convBubbleBaseRows.indices.filter { isConvBubbleWeakOrUntried(it) }
        }
        convBubbleDisplayToBaseIndex = indices
        convBubbleRows = indices.map { convBubbleBaseRows[it] }
        convBubbleCurrentIndex = if (convBubbleMode == ConversationMode.TEST) convBubbleFirstIndexForInitiator() else 0
        convBubbleListeningForRowIndex = -1
        if (convBubbleRows.isEmpty()) {
            Toast.makeText(this, "All passed", Toast.LENGTH_SHORT).show()
        }
        convBubbleAdapter?.updateData(convBubbleRows)
        convBubbleAdapter?.learningMode = convBubbleLearningMode
        convBubbleAdapter?.testMode = (convBubbleMode == ConversationMode.TEST)
        // Do not restore saved stats when switching tab/initiator so ticks/crosses start from beginning
        convBubbleAdapter?.setCurrentIndex(convBubbleCurrentIndex.coerceIn(0, (convBubbleRows.size - 1).coerceAtLeast(0)))
        convBubbleLastScrolledPosition = -1
        convBubbleCachedItemHeightPx = -1
        convBubbleSessionCorrect = 0
        convBubbleSessionAttempted = 0
        updateConvBubbleStats()
        updateConvBubbleRowPositionText()
        if (convBubbleControlRunning && convBubbleRows.isNotEmpty()) speakConvBubbleCurrent()
    }

    /**
     * Shared V tab setup: uses current lesson's [lessonVocabRows] (set by loadConversationBubbleLesson or loadThreeColLessonFromAsset).
     * Same layout (layout_lesson_vocab_pronunciation) and behaviour for all similar lessons; only data differs per lesson file.
     */
    private fun setupVocabTabForCurrentLesson(vocabInclude: View?, onUpdateRowPosition: () -> Unit) {
        val vocabRecycler = vocabInclude?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.lesson_vocab_recycler)
        if (lessonVocabAdapter == null) {
            lessonVocabAdapter = LessonVocabAdapter(lessonVocabRows.toMutableList())
        }
        val vTabRows = LessonFileParsers.filterLessonVocabRowsByMaster(lessonVocabRows, masterWordListMap)
        // Always load latest progress so words already passed in any lesson are hidden unless "Show all words" is on
        vocabularyProgress = LessonFileParsers.loadVocabularyProgress(filesDir).toMutableMap()
        currentVTabRows = if (vocabShowAllWords) vTabRows else LessonFileParsers.filterLessonVocabRowsNeedingTest(vTabRows, vocabularyProgress)
        lessonVocabAdapter?.updateRows(currentVTabRows)
        vocabRecycler?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        vocabRecycler?.adapter = lessonVocabAdapter
        val showAllSwitch = vocabInclude?.findViewById<CompoundButton>(R.id.lesson_vocab_show_all)
        showAllSwitch?.isChecked = vocabShowAllWords
        showAllSwitch?.setOnCheckedChangeListener { _, isChecked ->
            vocabShowAllWords = isChecked
            val v = LessonFileParsers.filterLessonVocabRowsByMaster(lessonVocabRows, masterWordListMap)
            currentVTabRows = if (vocabShowAllWords) v else LessonFileParsers.filterLessonVocabRowsNeedingTest(v, vocabularyProgress)
            runOnUiThread {
                lessonVocabAdapter?.updateRows(currentVTabRows)
                lessonVocabAdapter?.currentIndex = 0
                vocabRecycler?.scrollToPosition(0)
                onUpdateRowPosition()
            }
        }
    }

    private fun setupConvBubbleModeButtons(root: View) {
        val learningBtn = root.findViewById<Button>(R.id.lesson_mode_learning)
        val practiceBtn = root.findViewById<Button>(R.id.lesson_mode_practice)
        val testBtn = root.findViewById<Button>(R.id.lesson_mode_test)
        val vocabBtn = root.findViewById<Button>(R.id.lesson_mode_vocab)
        val prevRowBtn = root.findViewById<ImageButton>(R.id.lesson_mode_bar_prev)
        val nextRowBtn = root.findViewById<ImageButton>(R.id.lesson_mode_bar_next)
        val weakOnlyCheck = root.findViewById<Switch>(R.id.conv_bubble_weak_only)
        val weakOnlyRow = root.findViewById<View>(R.id.conv_bubble_row_nav)
        val testOptionsRow = root.findViewById<View>(R.id.conv_bubble_test_options_row)
        val mainContent = root.findViewById<View>(R.id.conv_bubble_main_content)
        val vocabInclude = root.findViewById<View>(R.id.conv_bubble_vocab_include)
        val vocabRecycler = vocabInclude?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.lesson_vocab_recycler)
        val initiatorAppRadio = root.findViewById<android.widget.RadioButton>(R.id.conv_bubble_initiator_app)
        val initiatorUserRadio = root.findViewById<android.widget.RadioButton>(R.id.conv_bubble_initiator_user)

        setupVocabTabForCurrentLesson(vocabInclude) { updateConvBubbleRowPositionText() }

        learningBtn?.setOnClickListener {
            convBubbleMode = ConversationMode.LEARNING
            convBubbleLearningMode = true
            convBubbleWeakOnlyFilter = weakOnlyCheck?.isChecked == true
            convBubbleIncorrectCount = 0
            convBubbleSessionCorrect = 0
            convBubbleSessionAttempted = 0
            applyConvBubbleFilterForCurrentMode()
            updateConvBubbleTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, weakOnlyCheck, weakOnlyRow, testOptionsRow, mainContent, vocabInclude)
        }
        practiceBtn?.setOnClickListener {
            convBubbleMode = ConversationMode.PRACTICE
            convBubbleLearningMode = false
            convBubbleWeakOnlyFilter = weakOnlyCheck?.isChecked == true
            convBubbleIncorrectCount = 0
            convBubbleSessionCorrect = 0
            convBubbleSessionAttempted = 0
            applyConvBubbleFilterForCurrentMode()
            updateConvBubbleTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, weakOnlyCheck, weakOnlyRow, testOptionsRow, mainContent, vocabInclude)
        }
        testBtn?.setOnClickListener {
            convBubbleMode = ConversationMode.TEST
            convBubbleLearningMode = false
            convBubbleTestInitiatorApp = initiatorAppRadio?.isChecked != false
            convBubbleIncorrectCount = 0
            convBubbleSessionCorrect = 0
            convBubbleSessionAttempted = 0
            applyConvBubbleFilterForCurrentMode()
            updateConvBubbleTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, weakOnlyCheck, weakOnlyRow, testOptionsRow, mainContent, vocabInclude)
            initiatorAppRadio?.isChecked = convBubbleTestInitiatorApp
            initiatorUserRadio?.isChecked = !convBubbleTestInitiatorApp
        }
        vocabBtn?.setOnClickListener {
            convBubbleMode = ConversationMode.VOCAB
            lessonVocabAdapter?.currentIndex = 0
            vocabRecycler?.scrollToPosition(0)
            updateConvBubbleTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, weakOnlyCheck, weakOnlyRow, testOptionsRow, mainContent, vocabInclude)
            updateConvBubbleRowPositionText()
        }
        weakOnlyCheck?.setOnCheckedChangeListener { _, isChecked ->
            convBubbleWeakOnlyFilter = isChecked
            applyConvBubbleFilterForCurrentMode()
        }
        initiatorAppRadio?.setOnClickListener {
            convBubbleTestInitiatorApp = true
            if (convBubbleMode == ConversationMode.TEST) applyConvBubbleFilterForCurrentMode()
        }
        initiatorUserRadio?.setOnClickListener {
            convBubbleTestInitiatorApp = false
            if (convBubbleMode == ConversationMode.TEST) applyConvBubbleFilterForCurrentMode()
        }
        updateConvBubbleTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, weakOnlyCheck, weakOnlyRow, testOptionsRow, mainContent, vocabInclude)

        prevRowBtn?.setOnClickListener {
            if (convBubbleMode == ConversationMode.VOCAB) {
                val n = currentVTabRows.size
                if (n == 0) return@setOnClickListener
                val idx = (lessonVocabAdapter?.currentIndex ?: 0).coerceIn(0, n - 1)
                val newIdx = (idx - 1).coerceAtLeast(0)
                lessonVocabAdapter?.currentIndex = newIdx
                vocabRecycler?.scrollToPosition(newIdx)
                updateConvBubbleRowPositionText()
                return@setOnClickListener
            }
            if (convBubbleRows.isEmpty()) return@setOnClickListener
            clearConvBubbleSpeechAndVerificationState()
            convBubbleCurrentIndex = (convBubbleCurrentIndex - 1).coerceAtLeast(0)
            convBubbleAdapter?.setCurrentIndex(convBubbleCurrentIndex)
            updateConvBubbleRowPositionText()
            if (convBubbleControlRunning && !convBubbleControlPaused) speakConvBubbleCurrent()
        }
        nextRowBtn?.setOnClickListener {
            if (convBubbleMode == ConversationMode.VOCAB) {
                val n = currentVTabRows.size
                if (n == 0) return@setOnClickListener
                val idx = lessonVocabAdapter?.currentIndex ?: 0
                val newIdx = (idx + 1).coerceAtMost(n - 1)
                lessonVocabAdapter?.currentIndex = newIdx
                vocabRecycler?.scrollToPosition(newIdx)
                updateConvBubbleRowPositionText()
                return@setOnClickListener
            }
            if (convBubbleRows.isEmpty()) return@setOnClickListener
            clearConvBubbleSpeechAndVerificationState()
            convBubbleCurrentIndex = (convBubbleCurrentIndex + 1).coerceAtMost(convBubbleRows.lastIndex)
            convBubbleAdapter?.setCurrentIndex(convBubbleCurrentIndex)
            updateConvBubbleRowPositionText()
            if (convBubbleControlRunning && !convBubbleControlPaused) speakConvBubbleCurrent()
        }
    }

    private fun updateConvBubbleTabAppearance(learningBtn: View?, practiceBtn: View?, testBtn: View?, vocabBtn: View?, weakOnlyCheck: View?, weakOnlyRow: View?, testOptionsRow: View?, mainContent: View?, vocabInclude: View?) {
        val white = 0xFFFFFFFF.toInt()
        val darkText = 0xFF555555.toInt()
        val isLearning = convBubbleMode == ConversationMode.LEARNING
        val isPractice = convBubbleMode == ConversationMode.PRACTICE
        val isTest = convBubbleMode == ConversationMode.TEST
        val isVocab = convBubbleMode == ConversationMode.VOCAB
        val sel = R.drawable.bg_lesson_mode_tab_selected
        val unsel = R.drawable.bg_lesson_mode_tab_unselected
        (learningBtn as? TextView)?.setBackgroundResource(if (isLearning) sel else unsel)
        (learningBtn as? TextView)?.setTextColor(if (isLearning) white else darkText)
        (practiceBtn as? TextView)?.setBackgroundResource(if (isPractice) sel else unsel)
        (practiceBtn as? TextView)?.setTextColor(if (isPractice) white else darkText)
        (testBtn as? TextView)?.setBackgroundResource(if (isTest) sel else unsel)
        (testBtn as? TextView)?.setTextColor(if (isTest) white else darkText)
        (vocabBtn as? TextView)?.setBackgroundResource(if (isVocab) sel else unsel)
        (vocabBtn as? TextView)?.setTextColor(if (isVocab) white else darkText)
        mainContent?.visibility = if (isVocab) View.GONE else View.VISIBLE
        vocabInclude?.visibility = if (isVocab) View.VISIBLE else View.GONE
        vocabBtn?.visibility = if (lessonVocabRows.isEmpty()) View.GONE else View.VISIBLE
        // Practice: show Failed & untried only; Test: show role + initiator; Learning/Vocab: hide both
        weakOnlyRow?.visibility = if (isPractice) View.VISIBLE else View.GONE
        weakOnlyCheck?.visibility = if (isPractice) View.VISIBLE else View.GONE
        testOptionsRow?.visibility = if (isTest) View.VISIBLE else View.GONE
    }

    /** Reusable scroll logic for any lesson: on load we determine the center index; once past it, always scroll one bubble height at a time (smooth). In V mode, prev/next refer to vocab list. */
    private fun updateConvBubbleRowPositionText() {
        if (currentContentLayout != ContentLayout.CONVERSATION_BUBBLES) return
        val root = contentFrame.getChildAt(0) ?: return
        val prevBtn = root.findViewById<ImageButton>(R.id.lesson_mode_bar_prev)
        val nextBtn = root.findViewById<ImageButton>(R.id.lesson_mode_bar_next)
        if (convBubbleMode == ConversationMode.VOCAB) {
            val n = currentVTabRows.size
            val cur = (lessonVocabAdapter?.currentIndex ?: 0).coerceIn(0, (n - 1).coerceAtLeast(0))
            prevBtn?.isEnabled = n > 0 && cur > 0
            nextBtn?.isEnabled = n > 0 && cur < n - 1
            val vocabInclude = root.findViewById<View>(R.id.conv_bubble_vocab_include)
            val vocabRecycler = vocabInclude?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.lesson_vocab_recycler)
            vocabRecycler?.scrollToPosition(cur)
            return
        }
        val n = convBubbleRows.size
        val cur = convBubbleCurrentIndex.coerceIn(0, (n - 1).coerceAtLeast(0))
        prevBtn?.isEnabled = n > 0 && cur > 0
        nextBtn?.isEnabled = n > 0 && cur < n - 1
        val recycler = root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.conv_bubble_recycler)
        val lm = recycler?.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
        if (lm != null && n > 0) {
            val pos = convBubbleCurrentIndex.coerceIn(0, convBubbleRows.lastIndex)
            recycler.postDelayed({
                // 1) Visible area height — same for all lessons.
                val visibleAreaHeight = recycler.height
                if (visibleAreaHeight <= 0) return@postDelayed

                // 2) Single bubble height — same formula: from measured view when available, else generic fallback.
                var bubbleHeight = convBubbleCachedItemHeightPx
                if (bubbleHeight <= 0) {
                    val viewAt0 = lm.findViewByPosition(0)
                    if (viewAt0 != null && viewAt0.height > 0) {
                        bubbleHeight = viewAt0.height
                        convBubbleCachedItemHeightPx = bubbleHeight
                    } else {
                        val viewAtPos = lm.findViewByPosition(pos)
                        if (viewAtPos != null && viewAtPos.height > 0) {
                            bubbleHeight = viewAtPos.height
                            convBubbleCachedItemHeightPx = bubbleHeight
                        } else {
                            bubbleHeight = (visibleAreaHeight / 8).coerceAtLeast(1)
                        }
                    }
                }
                val effectiveBubbleHeight = if (convBubbleMode == ConversationMode.LEARNING) bubbleHeight else (bubbleHeight * 2)

                // 3) Center index: the bubble index that can be in the middle (same formula for 10 or 100 items).
                val centerIndex = (visibleAreaHeight / 2 / bubbleHeight).toInt().coerceIn(0, (n - 1).coerceAtLeast(0))

                if (pos == 0) {
                    lm.scrollToPositionWithOffset(0, 0)
                    convBubbleLastScrolledPosition = 0
                    return@postDelayed
                }

                if (pos <= centerIndex) {
                    // Before passing the center bubble: keep list at top.
                    lm.scrollToPositionWithOffset(0, 0)
                    convBubbleLastScrolledPosition = pos
                    return@postDelayed
                }

                // Past the center bubble: always scroll one bubble height at a time (smooth), until no more invisible items.
                val lastPos = convBubbleLastScrolledPosition
                var dy = (pos - lastPos) * effectiveBubbleHeight
                val maxScroll = (recycler.computeVerticalScrollRange() - recycler.computeVerticalScrollExtent()).coerceAtLeast(0)
                val currentScrollY = recycler.computeVerticalScrollOffset()
                dy = dy.coerceIn(-currentScrollY, maxScroll - currentScrollY)
                if (dy != 0) {
                    var last = 0
                    ValueAnimator.ofInt(0, dy).apply {
                        duration = 280
                        interpolator = DecelerateInterpolator()
                        addUpdateListener {
                            val v = it.animatedValue as Int
                            recycler.scrollBy(0, v - last)
                            last = v
                        }
                        start()
                    }
                }
                convBubbleLastScrolledPosition = pos
            }, 80)
        }
    }

    private fun updateConvBubbleStats() {
        if (currentContentLayout != ContentLayout.CONVERSATION_BUBBLES) return
        val root = if (contentFrame.childCount > 0) contentFrame.getChildAt(0) else null
        val statView = root?.findViewById<TextView>(R.id.conv_bubble_stat) ?: return
        val total = convBubbleBaseRows.size
        val c = convBubbleSessionCorrect.coerceIn(0, total)
        val a = convBubbleSessionAttempted.coerceIn(0, total)
        statView.text = "$c/$a/$total"
    }

    private fun updateConvBubbleControlBar() {
        controlStartStopButton?.let { ControlBarUtils.setControlStartStopButton(this, it, convBubbleControlRunning) }
        controlPauseResumeButton?.let { ControlBarUtils.setControlPauseResumeButton(this, it, convBubbleControlPaused) }
    }

    /** Call when user taps prev/next: stop TTS and verification, cancel pending advance, clear expected so only the new bubble will speak/listen. */
    private fun clearConvBubbleSpeechAndVerificationState() {
        if (currentContentLayout != ContentLayout.CONVERSATION_BUBBLES) return
        convBubbleAdvanceRunnable?.let { verificationHandler.removeCallbacks(it) }
        convBubbleAdvanceRunnable = null
        textToSpeech?.stop()
        cancelVerificationTimeout()
        if (verificationMode) {
            verificationMode = false
            expectedEnglishForVerification = null
            try { speechRecognizer?.stopListening() } catch (_: Exception) { }
            stopEnglishVoskRecording()
            setMicButtonAppearance(recording = false)
        }
        expectedEnglishForVerification = null
        pendingRestartVerificationWith = null
        pendingSpeakCorrectWordAfterIncorrect = null
        pendingBengaliAfterIncorrect = null
        convBubbleListeningForRowIndex = -1
        isEnglishMicActive = false
        isRecording = false
    }

    private fun onConvBubbleStartStop() {
        if (convBubbleControlRunning) {
            textToSpeech?.stop()
            cancelVerificationTimeout()
            if (verificationMode) {
                verificationMode = false
                expectedEnglishForVerification = null
                try { speechRecognizer?.stopListening() } catch (_: Exception) { }
                stopEnglishVoskRecording()
                setMicButtonAppearance(recording = false)
            }
            isEnglishMicActive = false
            isRecording = false
            convBubbleControlRunning = false
        } else {
            if (convBubbleRows.isEmpty()) return
            convBubbleControlPaused = false
            convBubbleControlRunning = true
            speakConvBubbleCurrent()
        }
        updateConvBubbleControlBar()
    }

    /** V tab: Start = speak current word (TTS) then listen for it; Stop = stop TTS and recognition. Used from both CONVERSATION_BUBBLES and THREECOL_TABLE when in V mode. */
    private fun onVocabStartStop() {
        if (convBubbleControlRunning) {
            textToSpeech?.stop()
            cancelVerificationTimeout()
            if (verificationMode) {
                verificationMode = false
                expectedEnglishForVerification = null
                try { speechRecognizer?.stopListening() } catch (_: Exception) { }
                stopEnglishVoskRecording()
                setMicButtonAppearance(recording = false)
            }
            isEnglishMicActive = false
            isRecording = false
            convBubbleControlRunning = false
            if (currentContentLayout == ContentLayout.THREECOL_TABLE) threeColControlRunning = false
            if (currentContentLayout == ContentLayout.TENSE_TRIPLETS) tenseTripletControlRunning = false
            if (currentContentLayout == ContentLayout.EXTEND_SENTENCE) updateExtendSentenceControlBar()
        } else {
            if (currentVTabRows.isEmpty()) return
            val idx = (lessonVocabAdapter?.currentIndex ?: 0).coerceIn(0, currentVTabRows.lastIndex)
            val row = currentVTabRows.getOrNull(idx) ?: return
            vocabIncorrectCount = 0
            convBubbleControlRunning = true
            if (currentContentLayout == ContentLayout.THREECOL_TABLE) threeColControlRunning = true
            if (currentContentLayout == ContentLayout.TENSE_TRIPLETS) tenseTripletControlRunning = true
            textToSpeech?.stop()
            textToSpeech?.setLanguage(Locale.US)
            textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(row.word), TextToSpeech.QUEUE_FLUSH, null, "vocab_word")
            expectedEnglishForVerification = row.word
            verificationHandler.postDelayed({
                if (!isDestroyed && convBubbleControlRunning) {
                    val inConvV = currentContentLayout == ContentLayout.CONVERSATION_BUBBLES && convBubbleMode == ConversationMode.VOCAB
                    val inThreeV = currentContentLayout == ContentLayout.THREECOL_TABLE && threeColMode == ThreeColMode.VOCAB
                    val inTenseV = currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletMode == TenseTripletMode.VOCAB
                    val inExtendV = currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceMode == ConversationMode.VOCAB
                    if (inConvV || inThreeV || inTenseV || inExtendV) startVerificationListening(row.word)
                }
            }, 800)
        }
        if (currentContentLayout == ContentLayout.CONVERSATION_BUBBLES) updateConvBubbleControlBar()
        else if (currentContentLayout == ContentLayout.THREECOL_TABLE) updateThreeColControlBar()
        else if (currentContentLayout == ContentLayout.TENSE_TRIPLETS) updateTenseTripletControlBar()
        else if (currentContentLayout == ContentLayout.EXTEND_SENTENCE) updateExtendSentenceControlBar()
    }

    private fun onConvBubblePauseResume() {
        if (convBubbleControlPaused) {
            if (convBubbleRows.isEmpty()) return
            convBubbleControlPaused = false
            convBubbleControlRunning = true
            speakConvBubbleCurrent()
        } else {
            textToSpeech?.stop()
            cancelVerificationTimeout()
            if (verificationMode) {
                verificationMode = false
                expectedEnglishForVerification = null
                try { speechRecognizer?.stopListening() } catch (_: Exception) { }
                stopEnglishVoskRecording()
                setMicButtonAppearance(recording = false)
            }
            isEnglishMicActive = false
            isRecording = false
            convBubbleControlRunning = false
            convBubbleControlPaused = true
        }
        updateConvBubbleControlBar()
    }

    /** Speak current row. Test: app row = speak English; user row = listen. Role/initiator set by toggle and radio. Practice/Learning: speak Bengali then listen. */
    private fun speakConvBubbleCurrent() {
        if (currentContentLayout != ContentLayout.CONVERSATION_BUBBLES || convBubbleRows.isEmpty()) return
        if (convBubbleMode == ConversationMode.VOCAB) return
        val idx = convBubbleCurrentIndex.coerceIn(0, convBubbleRows.lastIndex)
        val row = convBubbleRows[idx]
        convBubbleAdapter?.setCurrentIndex(idx)
        textToSpeech?.stop()
        convBubbleTtsGeneration++
        val gen = convBubbleTtsGeneration
        val utteranceBengali = "conv_bubble_bengali_${idx}_$gen"
        if (convBubbleMode == ConversationMode.TEST) {
            if (isConvBubbleRowAppByInitiator(row, idx)) {
                // App's line: speak English (no compare for app)
                expectedEnglishForVerification = convBubbleRows.getOrNull(idx + 1)?.english
                textToSpeech?.setLanguage(Locale.US)
                textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(row.english), TextToSpeech.QUEUE_FLUSH, null, utteranceBengali)
            } else {
                // User's line: don't speak, start listening and compare
                expectedEnglishForVerification = row.english
                convBubbleListeningForRowIndex = idx
                startVerificationListening(row.english)
            }
            return
        }
        expectedEnglishForVerification = row.english
        textToSpeech?.setLanguage(Locale("bn"))
        textToSpeech?.speak(row.bengali, TextToSpeech.QUEUE_FLUSH, null, utteranceBengali)
    }

    /** After Bengali (or TEST app English) finishes; [utteranceId] encodes row index + generation. */
    private fun handleConvBubbleBengaliUtteranceFinished(utteranceId: String) {
        if (isDestroyed || currentContentLayout != ContentLayout.CONVERSATION_BUBBLES) return
        val m = convBubbleBengaliUtteranceRegex.matchEntire(utteranceId) ?: return
        val rowIdx = m.groupValues[1].toInt()
        val gen = m.groupValues[2].toInt()
        if (gen != convBubbleTtsGeneration) return
        // Test role-play: app (A) just spoke; no compare. If next is A again, speak it; only if next is B, listen and compare.
        if (convBubbleMode == ConversationMode.TEST) {
            convBubbleAdapter?.revealEnglishForAppSpoke(rowIdx.coerceIn(0, convBubbleRows.lastIndex))
            val nextIdx = rowIdx + 1
            val nextRow = convBubbleRows.getOrNull(nextIdx)
            if (nextRow == null) {
                convBubbleControlRunning = false
                convBubbleControlPaused = false
                updateConvBubbleControlBar()
                return
            }
            if (isConvBubbleRowAppByInitiator(nextRow, nextIdx)) {
                convBubbleCurrentIndex = nextIdx
                convBubbleAdapter?.setCurrentIndex(convBubbleCurrentIndex)
                updateConvBubbleRowPositionText()
                speakConvBubbleCurrent()
            } else {
                convBubbleCurrentIndex = nextIdx
                convBubbleAdapter?.setCurrentIndex(convBubbleCurrentIndex)
                updateConvBubbleRowPositionText()
                convBubbleListeningForRowIndex = nextIdx
                startVerificationListening(nextRow.english)
            }
            return
        }
        val currentRow = convBubbleRows.getOrNull(
            rowIdx.coerceIn(0, (convBubbleRows.size - 1).coerceAtLeast(0))
        ) ?: return
        val expected = currentRow.english
        // Practice: speak Bengali only, then listen. Learning: speak English then listen.
        if (!convBubbleLearningMode) {
            startVerificationListening(expected)
        } else if (!expected.isNullOrBlank() && ttsReady && textToSpeech != null) {
            textToSpeech?.setLanguage(Locale.US)
            textToSpeech?.speak(
                MatchNormalizer.textForSpeakAndDisplay(expected),
                TextToSpeech.QUEUE_FLUSH,
                null,
                "conv_bubble_english_${rowIdx}_$gen"
            )
        } else {
            startVerificationListening(expected)
        }
    }

    /** Learning mode: after English line finishes, listen for user to repeat. */
    private fun handleConvBubbleEnglishUtteranceFinished(utteranceId: String) {
        val m = convBubbleEnglishUtteranceRegex.matchEntire(utteranceId) ?: return
        val rowIdx = m.groupValues[1].toInt()
        val gen = m.groupValues[2].toInt()
        if (gen != convBubbleTtsGeneration) return
        val expected = convBubbleRows.getOrNull(
            rowIdx.coerceIn(0, (convBubbleRows.size - 1).coerceAtLeast(0))
        )?.english
        if (expected != null && !isDestroyed && currentContentLayout == ContentLayout.CONVERSATION_BUBBLES && convBubbleLearningMode) {
            startVerificationListening(expected)
        }
    }

    private fun onConvBubbleVerificationResult(match: Boolean, said: String) {
        if (currentContentLayout != ContentLayout.CONVERSATION_BUBBLES) return
        if (convBubbleMode == ConversationMode.VOCAB && currentVTabRows.isNotEmpty()) {
            runOnUiThread {
                Toast.makeText(this, if (match) getString(R.string.correct) else getString(R.string.incorrect), Toast.LENGTH_SHORT).show()
            }
            val curIdx = (lessonVocabAdapter?.currentIndex ?: 0).coerceIn(0, currentVTabRows.lastIndex)
            val row = currentVTabRows.getOrNull(curIdx) ?: return
            runOnUiThread { lessonVocabAdapter?.setSpokenText(curIdx, said) }
            val wordKey = row.word.trim().lowercase()
            val advance: Boolean = if (match) {
                vocabIncorrectCount = 0
                true
            } else {
                vocabIncorrectCount++
                vocabIncorrectCount >= 3
            }
            if (advance) {
                runOnUiThread { lessonVocabAdapter?.setResult(curIdx, match) }
                vocabIncorrectCount = 0
                vocabularyProgress[wordKey] = if (match) LessonFileParsers.VOCAB_PROGRESS_PASSED else LessonFileParsers.VOCAB_PROGRESS_FAILED
                LessonFileParsers.saveVocabularyProgress(filesDir, vocabularyProgress)
                if (match && !vocabShowAllWords) {
                    val vTabRows = LessonFileParsers.filterLessonVocabRowsByMaster(lessonVocabRows, masterWordListMap)
                    currentVTabRows = LessonFileParsers.filterLessonVocabRowsNeedingTest(vTabRows, vocabularyProgress)
                    runOnUiThread { lessonVocabAdapter?.updateRows(currentVTabRows) }
                }
                val nextIdx = if (match && !vocabShowAllWords) curIdx.coerceAtMost((currentVTabRows.size - 1).coerceAtLeast(0)) else (curIdx + 1).coerceAtMost((currentVTabRows.size - 1).coerceAtLeast(0))
                lessonVocabAdapter?.currentIndex = nextIdx
                val root = contentFrame.getChildAt(0)
                root?.findViewById<View>(R.id.conv_bubble_vocab_include)?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.lesson_vocab_recycler)?.scrollToPosition(nextIdx)
                root?.findViewById<View>(R.id.threecol_vocab_include)?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.lesson_vocab_recycler)?.scrollToPosition(nextIdx)
                if (currentVTabRows.isNotEmpty() && convBubbleControlRunning) {
                    verificationHandler.postDelayed({
                        if (!isDestroyed && convBubbleControlRunning && currentVTabRows.isNotEmpty()) {
                            val idx = (lessonVocabAdapter?.currentIndex ?: nextIdx).coerceIn(0, currentVTabRows.lastIndex)
                            val nextRow = currentVTabRows.getOrNull(idx) ?: return@postDelayed
                            textToSpeech?.stop()
                            textToSpeech?.setLanguage(Locale.US)
                            textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(nextRow.word), TextToSpeech.QUEUE_FLUSH, null, "vocab_word")
                            expectedEnglishForVerification = nextRow.word
                            verificationHandler.postDelayed({
                                if (!isDestroyed && convBubbleControlRunning) startVerificationListening(nextRow.word)
                            }, 800)
                        }
                    }, 1500)
                } else {
                    convBubbleControlRunning = false
                    if (currentContentLayout == ContentLayout.THREECOL_TABLE) threeColControlRunning = false
                    runOnUiThread {
                        if (currentContentLayout == ContentLayout.CONVERSATION_BUBBLES) updateConvBubbleControlBar()
                        else if (currentContentLayout == ContentLayout.THREECOL_TABLE) updateThreeColControlBar()
                    }
                }
            } else {
                verificationHandler.postDelayed({
                    if (!isDestroyed && convBubbleControlRunning && currentVTabRows.isNotEmpty()) {
                        val idx = (lessonVocabAdapter?.currentIndex ?: curIdx).coerceIn(0, currentVTabRows.lastIndex)
                        val retryRow = currentVTabRows.getOrNull(idx) ?: return@postDelayed
                        textToSpeech?.stop()
                        textToSpeech?.setLanguage(Locale.US)
                        textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(retryRow.word), TextToSpeech.QUEUE_FLUSH, null, "vocab_word_after_fail")
                        expectedEnglishForVerification = retryRow.word
                        verificationHandler.postDelayed({
                            if (!isDestroyed && convBubbleControlRunning) startVerificationListening(retryRow.word)
                        }, 800)
                    }
                }, 1500)
            }
            return
        }
        if (convBubbleRows.isEmpty()) return
        // Test role-play: result applies to the row we were listening for (left=app spoke then we listened for right; or right=we only listened).
        val userRowIdx = if (convBubbleMode == ConversationMode.TEST) {
            if (convBubbleListeningForRowIndex in convBubbleRows.indices) convBubbleListeningForRowIndex
            else (convBubbleCurrentIndex + 1).coerceIn(0, convBubbleRows.lastIndex)
        } else {
            convBubbleCurrentIndex.coerceIn(0, convBubbleRows.lastIndex)
        }
        convBubbleSessionAttempted++
        if (match) convBubbleSessionCorrect++
        val baseIdx = convBubbleDisplayToBaseIndex.getOrNull(userRowIdx) ?: userRowIdx
        if (baseIdx in convBubbleBaseRows.indices && !convBubbleLearningMode) {
            while (convBubbleStats.size <= baseIdx) convBubbleStats.add(IntArray(2) { 0 })
            val rowStat = convBubbleStats[baseIdx]
            when (convBubbleMode) {
                ConversationMode.PRACTICE -> rowStat[0] = if (match) 1 else 0
                ConversationMode.TEST -> rowStat[1] = if (match) 1 else 0
                else -> {}
            }
            LessonFileParsers.saveConversationBubbleStats(filesDir, convBubbleCurrentLessonKey, convBubbleStats)
        }
        convBubbleAdapter?.setSpokenText(userRowIdx, said)
        convBubbleAdapter?.markResult(userRowIdx, match)
        updateConvBubbleStats()
        if (convBubbleMode == ConversationMode.TEST) {
            // Only advance when correct; when wrong, 3-strike logic in handleVerificationResult will advance after 3 tries.
            if (match) {
                convBubbleIncorrectCount = 0
                convBubbleCurrentIndex = userRowIdx + 1
                convBubbleListeningForRowIndex = -1
                updateConvBubbleRowPositionText()
                convBubbleAdapter?.setCurrentIndex(convBubbleCurrentIndex.coerceIn(0, convBubbleRows.lastIndex))
                if (convBubbleCurrentIndex <= convBubbleRows.lastIndex && convBubbleControlRunning) {
                    verificationHandler.postDelayed({
                        if (!isDestroyed && currentContentLayout == ContentLayout.CONVERSATION_BUBBLES) speakConvBubbleCurrent()
                    }, 1500)
                } else {
                    convBubbleControlRunning = false
                    convBubbleControlPaused = false
                    updateConvBubbleControlBar()
                }
            } else {
                updateConvBubbleRowPositionText()
            }
            return
        }
        updateConvBubbleRowPositionText()
        if (match) {
            convBubbleIncorrectCount = 0
            if (convBubbleCurrentIndex < convBubbleRows.lastIndex) {
                convBubbleCurrentIndex++
                convBubbleAdapter?.setCurrentIndex(convBubbleCurrentIndex)
                updateConvBubbleRowPositionText()
                if (convBubbleControlRunning) {
                    verificationHandler.postDelayed({
                        if (!isDestroyed && currentContentLayout == ContentLayout.CONVERSATION_BUBBLES) speakConvBubbleCurrent()
                    }, 1500)
                }
            } else {
                convBubbleControlRunning = false
                convBubbleControlPaused = false
                updateConvBubbleControlBar()
            }
        }
    }

    /** Update prev/next button enabled state for THREECOL_TABLE and scroll so current row is visible. In V mode use vocab list. */
    private fun updateThreeColRowPositionText() {
        if (currentContentLayout != ContentLayout.THREECOL_TABLE) return
        val root = contentFrame.getChildAt(0) ?: return
        val prevBtn = root.findViewById<ImageButton>(R.id.lesson_mode_bar_prev)
        val nextBtn = root.findViewById<ImageButton>(R.id.lesson_mode_bar_next)
        if (threeColMode == ThreeColMode.VOCAB) {
            val n = currentVTabRows.size
            val cur = (lessonVocabAdapter?.currentIndex ?: 0).coerceIn(0, (n - 1).coerceAtLeast(0))
            prevBtn?.isEnabled = n > 0 && cur > 0
            nextBtn?.isEnabled = n > 0 && cur < n - 1
            root.findViewById<View>(R.id.threecol_vocab_include)?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.lesson_vocab_recycler)?.scrollToPosition(cur)
            return
        }
        val n = threeColRows.size
        val cur = threeColCurrentIndex.coerceIn(0, (n - 1).coerceAtLeast(0))
        prevBtn?.isEnabled = n > 0 && cur > 0
        nextBtn?.isEnabled = n > 0 && cur < n - 1
        updateThreeColAutoScrollPosition()
    }

    /**
     * Auto-scroll for every `ContentLayout.THREECOL_TABLE` lesson from [loadThreeColLessonFromAsset]
     * (How, Let, What, Where, When, Who, Why, can, test_layout, … — same code path for all).
     * Same behavior as [updateTenseTripletAutoScrollPosition]: center band, row-height steps, smooth [ValueAnimator].
     */
    private fun updateThreeColAutoScrollPosition(forceTop: Boolean = false) {
        if (currentContentLayout != ContentLayout.THREECOL_TABLE || threeColRows.isEmpty()) return
        if (threeColMode == ThreeColMode.VOCAB) return
        val root = contentFrame.getChildAt(0) ?: return
        val recycler = root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.threecol_recycler) ?: return
        val lm = recycler.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager ?: return
        val n = threeColRows.size
        val pos = threeColCurrentIndex.coerceIn(0, n - 1)
        recycler.postDelayed({
            val visibleAreaHeight = recycler.height
            if (visibleAreaHeight <= 0) return@postDelayed
            var rowHeight = threeColCachedRowHeightPx
            if (rowHeight <= 0) {
                val viewAtPos = lm.findViewByPosition(pos)
                val viewAt0 = lm.findViewByPosition(0)
                val hPos = viewAtPos?.height ?: 0
                val h0 = viewAt0?.height ?: 0
                rowHeight = maxOf(hPos, h0).takeIf { it > 0 } ?: (visibleAreaHeight / 6).coerceAtLeast(1)
                if (rowHeight > 0) threeColCachedRowHeightPx = rowHeight
            }
            val centerIndex = (visibleAreaHeight / 2 / rowHeight).toInt().coerceIn(0, (n - 1).coerceAtLeast(0))
            if (forceTop || pos == 0) {
                lm.scrollToPositionWithOffset(0, 0)
                threeColLastScrolledPosition = 0
                return@postDelayed
            }
            if (pos <= centerIndex) {
                lm.scrollToPositionWithOffset(0, 0)
                threeColLastScrolledPosition = pos
                return@postDelayed
            }
            val lastPos = threeColLastScrolledPosition
            val prevForDy = if (lastPos < 0) 0 else lastPos
            var dy = (pos - prevForDy) * rowHeight
            val maxScroll = (recycler.computeVerticalScrollRange() - recycler.computeVerticalScrollExtent()).coerceAtLeast(0)
            val currentScrollY = recycler.computeVerticalScrollOffset()
            dy = dy.coerceIn(-currentScrollY, maxScroll - currentScrollY)
            if (dy != 0) {
                var last = 0
                ValueAnimator.ofInt(0, dy).apply {
                    duration = 280
                    interpolator = DecelerateInterpolator()
                    addUpdateListener {
                        val v = it.animatedValue as Int
                        recycler.scrollBy(0, v - last)
                        last = v
                    }
                    start()
                }
            }
            threeColLastScrolledPosition = pos
        }, 80)
    }

    private fun loadPronunciationByTitle(title: String, lessons: List<Pair<String, List<List<String>>>>) {
        val match = lessons.find { it.first == title }
        if (match != null) {
            showPronunciationLesson(match.first, match.second)
            Toast.makeText(this, "Loaded: ${match.first}. Tap play to hear words.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadVerbLessonFromAsset(assetPath: String, name: String) {
        try {
            val content = assets.open(assetPath).bufferedReader().readText()
            val rows = parseVerbLessonFile(content)
            if (rows.isEmpty()) { Toast.makeText(this, "No valid rows in $assetPath", Toast.LENGTH_SHORT).show(); return }
            clearPronunciationLessonState()
            lessonRows = rows; lessonName = name
            incorrectLessonRows.clear(); incorrectLessonSourceName = null
            lessonCorrectCount = 0; lessonMode = 4; lessonIndex = 0; lessonPhase = "q"
            lessonMode3Listening = false; lessonMode3SpokeAnswer = false; lessonIncorrectCount = 0
            nextButton?.isEnabled = true; skipButton?.isEnabled = true
            clearBothTextAreas(); setSentenceListVisibility(false)
            updateLessonStatistic(); updateLessonTopicDisplay()
            showVerbDiagram(verbForLessonDiagram(lessonName))
            textView.text = getString(R.string.lesson_loaded)
            contentFrame.post { if (lessonRows != null && !isDestroyed) onNextLessonStep() }
        } catch (e: Exception) { Toast.makeText(this, "Could not load $assetPath", Toast.LENGTH_SHORT).show() }
    }

    private fun loadSvoFromAsset(assetPath: String) {
        try {
            val content = assets.open(assetPath).bufferedReader(StandardCharsets.UTF_8).readText()
            val (topic, rows) = parseSvoLessonFile(content)
            if (rows.isEmpty()) { Toast.makeText(this, "No valid rows in $assetPath", Toast.LENGTH_SHORT).show(); return }
            clearPronunciationLessonState()
            lessonRows = rows; lessonName = topic
            lessonCorrectCount = 0; lessonMode = 4; lessonIndex = 0; lessonPhase = "q"
            lessonMode3Listening = false; lessonMode3SpokeAnswer = false; lessonIncorrectCount = 0
            incorrectLessonRows.clear(); incorrectLessonSourceName = null
            nextButton?.isEnabled = true; skipButton?.isEnabled = true
            clearBothTextAreas(); setSentenceListVisibility(false)
            updateLessonStatistic(); updateLessonTopicDisplay()
            textView.text = getString(R.string.lesson_loaded)
            contentFrame.post { if (lessonRows != null && !isDestroyed) onNextLessonStep() }
        } catch (e: Exception) { Toast.makeText(this, "Could not load $assetPath", Toast.LENGTH_SHORT).show() }
    }

    /** Load SVO lesson from Lessons/SVO (files .txt) format (English,Bengali[,Pronunciation]). Shows on LEGACY layout. */
    private fun loadSvoFromAssetEnglishFirst(assetPath: String, topicName: String) {
        try {
            val content = assets.open(assetPath).bufferedReader(StandardCharsets.UTF_8).readText()
            val (topic, parsedRows) = LessonFileParsers.parseSvoLessonFileEnglishFirst(content, topicName)
            if (parsedRows.isEmpty()) { Toast.makeText(this, "No valid rows in $assetPath", Toast.LENGTH_SHORT).show(); return }
            clearPronunciationLessonState()
            lessonRows = parsedRows.map { LessonRow(it.engQ, it.bnQ, it.engA, it.bnA) }; lessonName = topic
            lessonCorrectCount = 0; lessonMode = 4; lessonIndex = 0; lessonPhase = "q"
            lessonMode3Listening = false; lessonMode3SpokeAnswer = false; lessonIncorrectCount = 0
            incorrectLessonRows.clear(); incorrectLessonSourceName = null
            nextButton?.isEnabled = true; skipButton?.isEnabled = true
            clearBothTextAreas(); setSentenceListVisibility(false)
            updateLessonStatistic(); updateLessonTopicDisplay()
            textView.text = getString(R.string.lesson_loaded)
            contentFrame.post { if (lessonRows != null && !isDestroyed) onNextLessonStep() }
        } catch (e: Exception) { Toast.makeText(this, "Could not load $assetPath", Toast.LENGTH_SHORT).show() }
    }

    /** Load 2-ribbon lesson (sv_ribbon / sv_past / sv_future) into layout_sv_ribbon; populates conveyor_left and conveyor_right. */
    private fun loadSvRibbonLesson(actionKey: String) {
        val (subjects, dataPerSubject) = when (actionKey) {
            "sv_ribbon" -> SVODataLoaders.loadSvRibbonDataPerSubject(assets)
            "sv_past" -> SVODataLoaders.loadSvRibbonDataPast(assets)
            "sv_future" -> SVODataLoaders.loadSvRibbonDataFuture(assets)
            else -> return
        }
        val leftList = mutableListOf<String>()
        val rightList = mutableListOf<String>()
        val bengaliList = mutableListOf<String>()
        val pronList = mutableListOf<String>()
        for (s in subjects) {
            for (e in dataPerSubject[s] ?: emptyList()) {
                leftList.add(s)
                rightList.add(e.verb)
                bengaliList.add(e.bengali)
                pronList.add(e.pronunciation)
            }
        }
        if (leftList.isEmpty()) {
            Toast.makeText(this, "No ribbon data for $actionKey", Toast.LENGTH_SHORT).show()
            return
        }
        svRibbonBengali = bengaliList
        svRibbonPronunciation = pronList
        svRibbonExpectedEnglish = leftList.zip(rightList) { subj, verb -> "$subj $verb" }
        val title = when (actionKey) {
            "sv_ribbon" -> "Subject + Verb"
            "sv_past" -> "Subject + Verb (Past)"
            "sv_future" -> "Subject + Verb (Future)"
            else -> "S-V"
        }
        svRibbonIncorrectCount = 0
        contentFrame.post {
            if (currentContentLayout != ContentLayout.SV_RIBBON || contentFrame.childCount == 0) return@post
            val root = contentFrame.getChildAt(0)
            val conveyorLeft = root.findViewById<ConveyorBeltView>(R.id.conveyor_left)
            val conveyorRight = root.findViewById<ConveyorBeltView>(R.id.conveyor_right)
            val titleView = root.findViewById<TextView>(R.id.sv_ribbon_title)
            if (conveyorLeft != null && conveyorRight != null) {
                conveyorLeft.setData(leftList)
                conveyorRight.setData(rightList)
                titleView?.text = title
                nextButton?.isEnabled = true
                skipButton?.isEnabled = false
                lessonName = title
                updateLessonTopicDisplay()
                setupSvRibbonModeButtons(root)
                updateSvRibbonView(0)
                expectedEnglishForVerification = svRibbonExpectedEnglish?.getOrNull(0)
                val bengali = svRibbonBengali?.getOrNull(0)
                if (!bengali.isNullOrBlank() && ttsReady) {
                    textToSpeech?.setLanguage(Locale("bn"))
                    val utteranceId = if (svRibbonLearningMode) "sv_ribbon_learning_bengali" else "sv_ribbon_practice_bengali"
                    textToSpeech?.speak(bengali, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                    svRibbonControlRunning = true
                    svRibbonControlPaused = false
                    updateSvRibbonControlBar()
                }
            }
        }
    }

    /** Advance both conveyors with smooth animation, then speak next and start verification (Next = skip, or after correct). */
    private fun moveSvRibbonToNextAndSpeak() {
        svRibbonIncorrectCount = 0
        val root = contentFrame.getChildAt(0) ?: return
        val conveyorLeft = root.findViewById<ConveyorBeltView>(R.id.conveyor_left) ?: return
        val conveyorRight = root.findViewById<ConveyorBeltView>(R.id.conveyor_right) ?: return
        val bengaliList = svRibbonBengali ?: return
        conveyorLeft.moveToNext {
            conveyorRight.moveToNext {
                runOnUiThread {
                    if (currentContentLayout != ContentLayout.SV_RIBBON || isDestroyed) return@runOnUiThread
                    val idx = conveyorLeft.getCurrentIndex()
                    updateSvRibbonView(idx)
                    expectedEnglishForVerification = svRibbonExpectedEnglish?.getOrNull(idx)
                    val bengali = bengaliList.getOrNull(idx)
                    if (!bengali.isNullOrBlank() && ttsReady) {
                        textToSpeech?.setLanguage(Locale("bn"))
                        val utteranceId = if (svRibbonLearningMode) "sv_ribbon_learning_bengali" else "sv_ribbon_practice_bengali"
                        textToSpeech?.speak(bengali, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                        svRibbonControlRunning = true
                        svRibbonControlPaused = false
                        updateSvRibbonControlBar()
                    }
                }
            }
        }
    }

    /** Called when user taps Next on SV_RIBBON: skip current, move conveyor, speak next, start verification. */
    private fun moveSvRibbonNext() {
        if (currentContentLayout != ContentLayout.SV_RIBBON || svRibbonBengali == null) return
        moveSvRibbonToNextAndSpeak()
    }

    /** After correct answer: delay already done by caller; animate conveyor to next, then speak and start verification. */
    private fun moveSvRibbonNextAfterCorrect() {
        moveSvRibbonToNextAndSpeak()
    }

    private fun setupSvRibbonModeButtons(root: View) {
        val learningBtn = root.findViewById<Button>(R.id.sv_ribbon_mode_learning)
        val practiceBtn = root.findViewById<Button>(R.id.sv_ribbon_mode_practice)
        learningBtn?.setOnClickListener {
            svRibbonLearningMode = true
            updateSvRibbonViewFromCurrentIndex()
            updateSvRibbonTabAppearance(learningBtn, practiceBtn)
        }
        practiceBtn?.setOnClickListener {
            svRibbonLearningMode = false
            updateSvRibbonViewFromCurrentIndex()
            updateSvRibbonTabAppearance(learningBtn, practiceBtn)
        }
        updateSvRibbonTabAppearance(learningBtn, practiceBtn)
    }

    private fun updateSvRibbonTabAppearance(learningBtn: View?, practiceBtn: View?) {
        val blue = ContextCompat.getColor(this, R.color.lesson_topic_bar_background)
        val gray = 0xFFE0E0E0.toInt()
        val white = 0xFFFFFFFF.toInt()
        val darkText = 0xFF555555.toInt()
        (learningBtn as? TextView)?.let {
            it.setBackgroundColor(if (svRibbonLearningMode) blue else gray)
            it.setTextColor(if (svRibbonLearningMode) white else darkText)
        }
        (practiceBtn as? TextView)?.let {
            it.setBackgroundColor(if (!svRibbonLearningMode) blue else gray)
            it.setTextColor(if (!svRibbonLearningMode) white else darkText)
        }
    }

    private fun updateSvRibbonView(idx: Int) {
        if (currentContentLayout != ContentLayout.SV_RIBBON) return
        val root = contentFrame.getChildAt(0) ?: return
        val pronView = root.findViewById<TextView>(R.id.sv_ribbon_pronunciation)
        val youSaidView = root.findViewById<TextView>(R.id.sv_ribbon_you_said)
        pronView?.text = svRibbonPronunciation?.getOrNull(idx) ?: ""
        pronView?.visibility = if (svRibbonLearningMode) View.VISIBLE else View.GONE
        youSaidView?.text = ""
    }

    private fun updateSvRibbonViewFromCurrentIndex() {
        val root = contentFrame.getChildAt(0) ?: return
        val conveyorLeft = root.findViewById<ConveyorBeltView>(R.id.conveyor_left) ?: return
        updateSvRibbonView(conveyorLeft.getCurrentIndex())
    }

    private fun setSvRibbonYouSaid(text: String) {
        if (currentContentLayout != ContentLayout.SV_RIBBON) return
        val root = contentFrame.getChildAt(0) ?: return
        root.findViewById<TextView>(R.id.sv_ribbon_you_said)?.text = MatchNormalizer.sanitizeSpokenTextForDisplay(text)
    }

    /** Wire Learning / Practice / Test / V tabs, prev/next row, and Weak-only filter for THREECOL_TABLE. */
    private fun setupThreeColModeButtons(root: View) {
        val learningBtn = root.findViewById<Button>(R.id.lesson_mode_learning)
        val practiceBtn = root.findViewById<Button>(R.id.lesson_mode_practice)
        val testBtn = root.findViewById<Button>(R.id.lesson_mode_test)
        val vocabBtn = root.findViewById<Button>(R.id.lesson_mode_vocab)
        val prevRowBtn = root.findViewById<ImageButton>(R.id.lesson_mode_bar_prev)
        val nextRowBtn = root.findViewById<ImageButton>(R.id.lesson_mode_bar_next)
        val weakOnlyCheck = root.findViewById<Switch>(R.id.threecol_weak_only)
        val mainContent = root.findViewById<View>(R.id.threecol_main_content)
        val vocabInclude = root.findViewById<View>(R.id.threecol_vocab_include)
        val vocabRecycler = vocabInclude?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.lesson_vocab_recycler)

        setupVocabTabForCurrentLesson(vocabInclude) { updateThreeColRowPositionText() }

        learningBtn?.setOnClickListener {
            threeColMode = ThreeColMode.LEARNING
            threeColLearningMode = true
            threeColWeakOnlyFilter = weakOnlyCheck?.isChecked == true
            threeColIncorrectCount = 0
            threeColSessionCorrect = 0
            threeColSessionAttempted = 0
            applyThreeColFilterForCurrentMode()
            updateThreeColTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, weakOnlyCheck, mainContent, vocabInclude)
        }
        practiceBtn?.setOnClickListener {
            threeColMode = ThreeColMode.PRACTICE
            threeColLearningMode = false
            threeColWeakOnlyFilter = weakOnlyCheck?.isChecked == true
            threeColIncorrectCount = 0
            threeColSessionCorrect = 0
            threeColSessionAttempted = 0
            applyThreeColFilterForCurrentMode()
            updateThreeColTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, weakOnlyCheck, mainContent, vocabInclude)
        }
        testBtn?.setOnClickListener {
            threeColMode = ThreeColMode.TEST
            threeColLearningMode = false
            threeColWeakOnlyFilter = weakOnlyCheck?.isChecked == true
            threeColIncorrectCount = 0
            threeColSessionCorrect = 0
            threeColSessionAttempted = 0
            applyThreeColFilterForCurrentMode()
            updateThreeColTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, weakOnlyCheck, mainContent, vocabInclude)
        }
        vocabBtn?.setOnClickListener {
            textToSpeech?.stop()
            incorrectFeedbackFallbackRunnable?.let { verificationHandler.removeCallbacks(it) }
            incorrectFeedbackFallbackRunnable = null
            tryAgainListenFallbackRunnable?.let { verificationHandler.removeCallbacks(it) }
            tryAgainListenFallbackRunnable = null
            threeColTtsGeneration++
            cancelVerificationTimeout()
            if (verificationMode) {
                verificationMode = false
                expectedEnglishForVerification = null
                try { speechRecognizer?.stopListening() } catch (_: Exception) { }
                stopEnglishVoskRecording()
                setMicButtonAppearance(recording = false)
            }
            isEnglishMicActive = false
            isRecording = false
            threeColMode = ThreeColMode.VOCAB
            lessonVocabAdapter?.currentIndex = 0
            vocabRecycler?.scrollToPosition(0)
            updateThreeColTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, weakOnlyCheck, mainContent, vocabInclude)
            updateThreeColRowPositionText()
        }

        weakOnlyCheck?.setOnCheckedChangeListener { _, isChecked ->
            threeColWeakOnlyFilter = isChecked
            applyThreeColFilterForCurrentMode()
        }

        prevRowBtn?.setOnClickListener {
            if (threeColMode == ThreeColMode.VOCAB) {
                val n = currentVTabRows.size
                if (n == 0) return@setOnClickListener
                val idx = (lessonVocabAdapter?.currentIndex ?: 0).coerceIn(0, n - 1)
                val newIdx = (idx - 1).coerceAtLeast(0)
                lessonVocabAdapter?.currentIndex = newIdx
                vocabRecycler?.scrollToPosition(newIdx)
                updateThreeColRowPositionText()
                return@setOnClickListener
            }
            if (threeColRows.isEmpty()) return@setOnClickListener
            threeColCurrentIndex = (threeColCurrentIndex - 1).coerceAtLeast(0)
            threeColAdapter?.setCurrentIndex(threeColCurrentIndex)
            updateThreeColRowPositionText()
            if (threeColControlRunning && !threeColControlPaused) speakThreeColCurrent()
        }
        nextRowBtn?.setOnClickListener {
            if (threeColMode == ThreeColMode.VOCAB) {
                val n = currentVTabRows.size
                if (n == 0) return@setOnClickListener
                val idx = lessonVocabAdapter?.currentIndex ?: 0
                val newIdx = (idx + 1).coerceAtMost(n - 1)
                lessonVocabAdapter?.currentIndex = newIdx
                vocabRecycler?.scrollToPosition(newIdx)
                updateThreeColRowPositionText()
                return@setOnClickListener
            }
            if (threeColRows.isEmpty()) return@setOnClickListener
            threeColCurrentIndex = (threeColCurrentIndex + 1).coerceAtMost(threeColRows.lastIndex)
            threeColAdapter?.setCurrentIndex(threeColCurrentIndex)
            updateThreeColRowPositionText()
            if (threeColControlRunning && !threeColControlPaused) speakThreeColCurrent()
        }

        updateThreeColTabAppearance(learningBtn, practiceBtn, testBtn, vocabBtn, weakOnlyCheck, mainContent, vocabInclude)
    }

    private fun updateThreeColTabAppearance(learningBtn: View?, practiceBtn: View?, testBtn: View?, vocabBtn: View?, weakOnlyCheck: View?, mainContent: View?, vocabInclude: View?) {
        val white = 0xFFFFFFFF.toInt()
        val darkText = 0xFF555555.toInt()
        val sel = R.drawable.bg_lesson_mode_tab_selected
        val unsel = R.drawable.bg_lesson_mode_tab_unselected
        val isLearning = threeColMode == ThreeColMode.LEARNING
        val isPractice = threeColMode == ThreeColMode.PRACTICE
        val isTest = threeColMode == ThreeColMode.TEST
        val isVocab = threeColMode == ThreeColMode.VOCAB
        (learningBtn as? TextView)?.let {
            it.setBackgroundResource(if (isLearning) sel else unsel)
            it.setTextColor(if (isLearning) white else darkText)
        }
        (practiceBtn as? TextView)?.let {
            it.setBackgroundResource(if (isPractice) sel else unsel)
            it.setTextColor(if (isPractice) white else darkText)
        }
        (testBtn as? TextView)?.let {
            it.setBackgroundResource(if (isTest) sel else unsel)
            it.setTextColor(if (isTest) white else darkText)
        }
        (vocabBtn as? TextView)?.let {
            it.setBackgroundResource(if (isVocab) sel else unsel)
            it.setTextColor(if (isVocab) white else darkText)
        }
        mainContent?.visibility = if (isVocab) View.GONE else View.VISIBLE
        vocabInclude?.visibility = if (isVocab) View.VISIBLE else View.GONE
        vocabBtn?.visibility = if (lessonVocabRows.isEmpty()) View.GONE else View.VISIBLE
        val showToggle = !isLearning && !isVocab
        weakOnlyCheck?.visibility = if (showToggle) View.VISIBLE else View.GONE
        (weakOnlyCheck?.parent as? View)?.visibility = if (showToggle) View.VISIBLE else View.GONE
    }

    /** Update Start/Stop + Pause/Resume bar for THREECOL_TABLE. */
    private fun updateThreeColControlBar() {
        controlStartStopButton?.let { ControlBarUtils.setControlStartStopButton(this, it, threeColControlRunning) }
        controlPauseResumeButton?.let { ControlBarUtils.setControlPauseResumeButton(this, it, threeColControlPaused) }
    }

    /** Start/Stop for THREECOL_TABLE: play from current row or stop playback + verification. */
    private fun onThreeColStartStop() {
        if (threeColControlRunning) {
            textToSpeech?.stop()
            threeColTtsGeneration++
            cancelVerificationTimeout()
            if (verificationMode) {
                verificationMode = false
                expectedEnglishForVerification = null
                try {
                    speechRecognizer?.stopListening()
                } catch (_: Exception) { }
                stopEnglishVoskRecording()
                setMicButtonAppearance(recording = false)
            }
            isEnglishMicActive = false
            isRecording = false
            threeColControlRunning = false
        } else {
            if (threeColRows.isEmpty()) return
            threeColControlPaused = false
            threeColControlRunning = true
            speakThreeColCurrent()
        }
        updateThreeColControlBar()
    }

    /** Pause/Resume for THREECOL_TABLE: stop current playback or resume from current row. */
    private fun onThreeColPauseResume() {
        if (threeColControlPaused) {
            if (threeColRows.isEmpty()) return
            threeColControlPaused = false
            threeColControlRunning = true
            speakThreeColCurrent()
        } else {
            textToSpeech?.stop()
            threeColTtsGeneration++
            cancelVerificationTimeout()
            if (verificationMode) {
                verificationMode = false
                expectedEnglishForVerification = null
                try {
                    speechRecognizer?.stopListening()
                } catch (_: Exception) { }
                stopEnglishVoskRecording()
                setMicButtonAppearance(recording = false)
            }
            isEnglishMicActive = false
            isRecording = false
            threeColControlRunning = false
            threeColControlPaused = true
        }
        updateThreeColControlBar()
    }

    /** Speak Bengali then English for current THREECOL row, then verification. */
    private fun speakThreeColCurrent() {
        if (currentContentLayout != ContentLayout.THREECOL_TABLE) return
        if (threeColMode == ThreeColMode.VOCAB) return
        if (threeColRows.isEmpty()) return
        val idx = threeColCurrentIndex.coerceIn(0, threeColRows.lastIndex)
        val row = threeColRows[idx]
        if (row.bengali.isBlank() || row.english.isBlank()) return
        threeColAdapter?.setCurrentIndex(idx)
        updateThreeColAutoScrollPosition()
        expectedEnglishForVerification = row.english
        textToSpeech?.stop()
        threeColTtsGeneration++
        val gen = threeColTtsGeneration
        textToSpeech?.setLanguage(Locale("bn"))
        val utteranceId = if (threeColLearningMode) "threecol_learning_bengali_$gen" else "threecol_practice_bengali_$gen"
        textToSpeech?.speak(row.bengali, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /** After Bengali TTS for current row (Learning: then English; Practice: listen only). Uses [threeColCurrentIndex] row so audio matches the UI. */
    private fun handleThreeColLearningBengaliUtteranceDone(utteranceId: String) {
        val gen = utteranceId.substringAfterLast('_').toIntOrNull() ?: return
        if (gen != threeColTtsGeneration) return
        if (isDestroyed || currentContentLayout != ContentLayout.THREECOL_TABLE) return
        if (threeColMode == ThreeColMode.VOCAB) return
        if (threeColRows.isEmpty()) return
        val idx = threeColCurrentIndex.coerceIn(0, threeColRows.lastIndex)
        val row = threeColRows.getOrNull(idx) ?: return
        val expected = row.english.trim()
        if (expected.isEmpty()) return
        expectedEnglishForVerification = expected
        if (ttsReady && textToSpeech != null) {
            textToSpeech?.setLanguage(Locale.US)
            textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(expected), TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
        } else {
            startVerificationListening(expected)
        }
    }

    private fun handleThreeColPracticeBengaliUtteranceDone(utteranceId: String) {
        val gen = utteranceId.substringAfterLast('_').toIntOrNull() ?: return
        if (gen != threeColTtsGeneration) return
        if (isDestroyed || currentContentLayout != ContentLayout.THREECOL_TABLE) return
        if (threeColMode == ThreeColMode.VOCAB) return
        if (threeColRows.isEmpty()) return
        val idx = threeColCurrentIndex.coerceIn(0, threeColRows.lastIndex)
        val row = threeColRows.getOrNull(idx) ?: return
        val expected = row.english.trim()
        if (expected.isEmpty()) return
        expectedEnglishForVerification = expected
        startVerificationListening(expected)
    }

    /** Handle verification result for THREECOL_TABLE: reveal second column, update stats, and advance when appropriate. V tab: Toast, advance to next word or stop. */
    private fun onThreeColVerificationResult(match: Boolean, said: String) {
        if (currentContentLayout != ContentLayout.THREECOL_TABLE) return
        if (threeColMode == ThreeColMode.VOCAB && currentVTabRows.isNotEmpty()) {
            runOnUiThread {
                Toast.makeText(this, if (match) getString(R.string.correct) else getString(R.string.incorrect), Toast.LENGTH_SHORT).show()
            }
            val curIdx = (lessonVocabAdapter?.currentIndex ?: 0).coerceIn(0, currentVTabRows.lastIndex)
            val row = currentVTabRows.getOrNull(curIdx) ?: return
            runOnUiThread { lessonVocabAdapter?.setSpokenText(curIdx, said) }
            val wordKey = row.word.trim().lowercase()
            val advance: Boolean = if (match) {
                vocabIncorrectCount = 0
                true
            } else {
                vocabIncorrectCount++
                vocabIncorrectCount >= 3
            }
            if (advance) {
                runOnUiThread { lessonVocabAdapter?.setResult(curIdx, match) }
                vocabIncorrectCount = 0
                vocabularyProgress[wordKey] = if (match) LessonFileParsers.VOCAB_PROGRESS_PASSED else LessonFileParsers.VOCAB_PROGRESS_FAILED
                LessonFileParsers.saveVocabularyProgress(filesDir, vocabularyProgress)
                if (match && !vocabShowAllWords) {
                    val vTabRows = LessonFileParsers.filterLessonVocabRowsByMaster(lessonVocabRows, masterWordListMap)
                    currentVTabRows = LessonFileParsers.filterLessonVocabRowsNeedingTest(vTabRows, vocabularyProgress)
                    runOnUiThread { lessonVocabAdapter?.updateRows(currentVTabRows) }
                }
                val nextIdx = if (match && !vocabShowAllWords) curIdx.coerceAtMost((currentVTabRows.size - 1).coerceAtLeast(0)) else (curIdx + 1).coerceAtMost((currentVTabRows.size - 1).coerceAtLeast(0))
                lessonVocabAdapter?.currentIndex = nextIdx
                contentFrame.getChildAt(0)?.findViewById<View>(R.id.threecol_vocab_include)?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.lesson_vocab_recycler)?.scrollToPosition(nextIdx)
                if (currentVTabRows.isNotEmpty() && convBubbleControlRunning) {
                    verificationHandler.postDelayed({
                        if (!isDestroyed && currentContentLayout == ContentLayout.THREECOL_TABLE && threeColMode == ThreeColMode.VOCAB && convBubbleControlRunning && currentVTabRows.isNotEmpty()) {
                            val idx = (lessonVocabAdapter?.currentIndex ?: nextIdx).coerceIn(0, currentVTabRows.lastIndex)
                            val nextRow = currentVTabRows.getOrNull(idx) ?: return@postDelayed
                            textToSpeech?.stop()
                            textToSpeech?.setLanguage(Locale.US)
                            textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(nextRow.word), TextToSpeech.QUEUE_FLUSH, null, "vocab_word")
                            expectedEnglishForVerification = nextRow.word
                            verificationHandler.postDelayed({
                                if (!isDestroyed && currentContentLayout == ContentLayout.THREECOL_TABLE && threeColMode == ThreeColMode.VOCAB && convBubbleControlRunning) startVerificationListening(nextRow.word)
                            }, 800)
                        }
                    }, 1500)
                } else {
                    convBubbleControlRunning = false
                    threeColControlRunning = false
                    runOnUiThread { updateThreeColControlBar() }
                }
            } else {
                verificationHandler.postDelayed({
                    if (!isDestroyed && currentContentLayout == ContentLayout.THREECOL_TABLE && threeColMode == ThreeColMode.VOCAB && convBubbleControlRunning && currentVTabRows.isNotEmpty()) {
                        val idx = (lessonVocabAdapter?.currentIndex ?: curIdx).coerceIn(0, currentVTabRows.lastIndex)
                        val retryRow = currentVTabRows.getOrNull(idx) ?: return@postDelayed
                        textToSpeech?.stop()
                        textToSpeech?.setLanguage(Locale.US)
                        textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(retryRow.word), TextToSpeech.QUEUE_FLUSH, null, "vocab_word_after_fail")
                        expectedEnglishForVerification = retryRow.word
                        verificationHandler.postDelayed({
                            if (!isDestroyed && currentContentLayout == ContentLayout.THREECOL_TABLE && threeColMode == ThreeColMode.VOCAB && convBubbleControlRunning) startVerificationListening(retryRow.word)
                        }, 800)
                    }
                }, 1500)
            }
            return
        }
        if (threeColRows.isEmpty()) return
        val idx = threeColCurrentIndex.coerceIn(0, threeColRows.lastIndex)
        if (!threeColLearningMode) {
            threeColAdapter?.setSpokenText(idx, said)
        }
        // Live stats (session): update in all modes (Learning, Practice, Test)
        threeColSessionAttempted++
        if (match) threeColSessionCorrect++
        // Per-row A,B and persist: only in Practice/Test
        val baseIdx = threeColDisplayToBaseIndex.getOrNull(idx) ?: idx
        if (baseIdx in threeColBaseRows.indices && !threeColLearningMode) {
            while (threeColStats.size <= baseIdx) {
                threeColStats.add(IntArray(2) { 0 })
            }
            val rowStat = threeColStats[baseIdx]
            when (threeColMode) {
                ThreeColMode.PRACTICE -> rowStat[0] = if (match) 1 else 0
                ThreeColMode.TEST -> rowStat[1] = if (match) 1 else 0
                else -> {}
            }
            val toSave = (0 until threeColBaseRows.size).map { i ->
                threeColStats.getOrNull(i) ?: intArrayOf(0, 0)
            }
            LessonFileParsers.saveThreeColStats(filesDir, threeColLessonKey(), toSave)
        }
        threeColAdapter?.markResult(idx, match)
        updateThreeColStats()
        updateThreeColRowPositionText()
        if (match) {
            threeColIncorrectCount = 0
            if (threeColCurrentIndex < threeColRows.lastIndex) {
                threeColCurrentIndex++
                threeColAdapter?.setCurrentIndex(threeColCurrentIndex)
                updateThreeColRowPositionText()
                if (threeColControlRunning) {
                    verificationHandler.postDelayed({
                        if (!isDestroyed && currentContentLayout == ContentLayout.THREECOL_TABLE) {
                            speakThreeColCurrent()
                        }
                    }, 1500)
                }
            } else {
                // Last row; stop control bar.
                threeColControlRunning = false
                threeColControlPaused = false
                updateThreeColControlBar()
            }
        } else {
            // Wrong answer: 3 tries per row before advancing (was in shared [handleVerificationResult] before THREECOL early return).
            threeColIncorrectCount++
            val rowEnglish = threeColRows.getOrNull(idx)?.english?.trim().orEmpty()
            if (threeColIncorrectCount >= 3) {
                threeColIncorrectCount = 0
                if (threeColMode == ThreeColMode.PRACTICE) {
                    threeColRows.getOrNull(idx)?.let { failRow ->
                        threeColAdapter?.setSpokenText(idx, MatchNormalizer.textForSpeakAndDisplay(failRow.english))
                        threeColAdapter?.markResult(idx, false)
                        updateThreeColStats()
                    }
                }
                verificationHandler.postDelayed({
                    if (!isDestroyed && currentContentLayout == ContentLayout.THREECOL_TABLE) {
                        if (threeColCurrentIndex < threeColRows.lastIndex) {
                            threeColCurrentIndex++
                            threeColAdapter?.setCurrentIndex(threeColCurrentIndex)
                            updateThreeColRowPositionText()
                            if (threeColControlRunning) speakThreeColCurrent()
                        } else {
                            threeColControlRunning = false
                            threeColControlPaused = false
                            updateThreeColControlBar()
                        }
                    }
                }, 1500)
            } else {
                val chancesLeft = 3 - threeColIncorrectCount
                if (chancesLeft > 0) {
                    Toast.makeText(this, getString(R.string.pronunciation_try_again_chances, chancesLeft), Toast.LENGTH_SHORT).show()
                }
                if (threeColControlRunning && rowEnglish.isNotBlank()) {
                    verificationHandler.postDelayed({
                        if (!isDestroyed && currentContentLayout == ContentLayout.THREECOL_TABLE &&
                            threeColControlRunning && threeColCurrentIndex == idx) {
                            startVerificationListening(rowEnglish)
                        }
                    }, 500)
                }
            }
        }
    }

    /** Update Start/Stop + Pause/Resume bar for TENSE_TRIPLETS. */
    private fun updateTenseTripletControlBar() {
        controlStartStopButton?.let { ControlBarUtils.setControlStartStopButton(this, it, tenseTripletControlRunning) }
        controlPauseResumeButton?.let { ControlBarUtils.setControlPauseResumeButton(this, it, tenseTripletControlPaused) }
    }

    /** Start/Stop for TENSE_TRIPLETS. */
    private fun onTenseTripletStartStop() {
        if (tenseTripletControlRunning) {
            textToSpeech?.stop()
            cancelVerificationTimeout()
            if (verificationMode) {
                verificationMode = false
                expectedEnglishForVerification = null
                try {
                    speechRecognizer?.stopListening()
                } catch (_: Exception) { }
                stopEnglishVoskRecording()
                setMicButtonAppearance(recording = false)
            }
            isEnglishMicActive = false
            isRecording = false
            tenseTripletControlRunning = false
            tenseTripletControlPaused = false
        } else {
            if (tenseTripletRows.isEmpty()) return
            tenseTripletControlPaused = false
            tenseTripletControlRunning = true
            speakTenseTripletCurrent()
        }
        updateTenseTripletControlBar()
    }

    /** Pause/Resume for TENSE_TRIPLETS. */
    private fun onTenseTripletPauseResume() {
        if (tenseTripletControlPaused) {
            if (tenseTripletRows.isEmpty()) return
            tenseTripletControlPaused = false
            tenseTripletControlRunning = true
            speakTenseTripletCurrent()
        } else {
            textToSpeech?.stop()
            cancelVerificationTimeout()
            if (verificationMode) {
                verificationMode = false
                expectedEnglishForVerification = null
                try {
                    speechRecognizer?.stopListening()
                } catch (_: Exception) { }
                stopEnglishVoskRecording()
                setMicButtonAppearance(recording = false)
            }
            isEnglishMicActive = false
            isRecording = false
            tenseTripletControlRunning = false
            tenseTripletControlPaused = true
        }
        updateTenseTripletControlBar()
    }

    /** Speak triplet columns in order (EN+BN for each), then listen for all 3 English sentences. */
    private fun speakTenseTripletCurrent() {
        if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) return
        if (tenseTripletRows.isEmpty()) return
        if (!tenseTripletShowPresent && !tenseTripletShowPast && !tenseTripletShowFuture) return
        val idx = tenseTripletCurrentIndex.coerceIn(0, tenseTripletRows.lastIndex)
        tenseTripletAdapter?.setCurrentIndex(idx)
        val row = tenseTripletRows[idx]
        val expectedCombined = buildList {
            if (tenseTripletShowPresent) add(row.present.english)
            if (tenseTripletShowPast) add(row.past.english)
            if (tenseTripletShowFuture) add(row.future.english)
        }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
        if (expectedCombined.isBlank()) return
        expectedEnglishForVerification = expectedCombined
        textToSpeech?.stop()
        when {
            tenseTripletShowPresent -> {
                if (tenseTripletMode == TenseTripletMode.TEST || tenseTripletBengaliFirst) {
                    textToSpeech?.setLanguage(Locale("bn"))
                    textToSpeech?.speak(row.present.bengali, TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_p_bn")
                } else {
                    textToSpeech?.setLanguage(Locale.US)
                    textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(row.present.english), TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_p_eng")
                }
            }
            tenseTripletShowPast -> {
                if (tenseTripletMode == TenseTripletMode.TEST) {
                    textToSpeech?.setLanguage(Locale("bn"))
                    textToSpeech?.speak(row.past.bengali, TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_pa_bn")
                } else {
                    textToSpeech?.setLanguage(Locale.US)
                    textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(row.past.english), TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_pa_eng")
                }
            }
            tenseTripletShowFuture -> {
                if (tenseTripletMode == TenseTripletMode.TEST) {
                    textToSpeech?.setLanguage(Locale("bn"))
                    textToSpeech?.speak(row.future.bengali, TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_f_bn")
                } else {
                    textToSpeech?.setLanguage(Locale.US)
                    textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(row.future.english), TextToSpeech.QUEUE_FLUSH, null, "tense_triplet_f_eng")
                }
            }
        }
    }

    /** Verification flow for TENSE_TRIPLETS: compare 3 English cells and mark each with tick/cross. */
    private fun onTenseTripletVerificationResult(match: Boolean, spokenTextIgnored: String) {
        if (currentContentLayout != ContentLayout.TENSE_TRIPLETS) return
        if (tenseTripletMode == TenseTripletMode.VOCAB && currentVTabRows.isNotEmpty()) {
            runOnUiThread { Toast.makeText(this, if (match) getString(R.string.correct) else getString(R.string.incorrect), Toast.LENGTH_SHORT).show() }
            val curIdx = (lessonVocabAdapter?.currentIndex ?: 0).coerceIn(0, currentVTabRows.lastIndex)
            val row = currentVTabRows.getOrNull(curIdx) ?: return
            runOnUiThread { lessonVocabAdapter?.setSpokenText(curIdx, spokenTextIgnored) }
            val wordKey = row.word.trim().lowercase()
            val advance: Boolean = if (match) {
                vocabIncorrectCount = 0
                true
            } else {
                vocabIncorrectCount++
                vocabIncorrectCount >= 3
            }
            if (advance) {
                runOnUiThread { lessonVocabAdapter?.setResult(curIdx, match) }
                vocabIncorrectCount = 0
                vocabularyProgress[wordKey] = if (match) LessonFileParsers.VOCAB_PROGRESS_PASSED else LessonFileParsers.VOCAB_PROGRESS_FAILED
                LessonFileParsers.saveVocabularyProgress(filesDir, vocabularyProgress)
                if (match && !vocabShowAllWords) {
                    val v = LessonFileParsers.filterLessonVocabRowsByMaster(lessonVocabRows, masterWordListMap)
                    currentVTabRows = LessonFileParsers.filterLessonVocabRowsNeedingTest(v, vocabularyProgress)
                    runOnUiThread { lessonVocabAdapter?.updateRows(currentVTabRows) }
                }
                val nextIdx = if (match && !vocabShowAllWords) curIdx.coerceAtMost((currentVTabRows.size - 1).coerceAtLeast(0))
                else (curIdx + 1).coerceAtMost((currentVTabRows.size - 1).coerceAtLeast(0))
                lessonVocabAdapter?.currentIndex = nextIdx
                contentFrame.getChildAt(0)?.findViewById<View>(R.id.tense_triplet_vocab_include)
                    ?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.lesson_vocab_recycler)
                    ?.scrollToPosition(nextIdx)
                if (currentVTabRows.isNotEmpty() && convBubbleControlRunning) {
                    verificationHandler.postDelayed({
                        if (!isDestroyed && currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletMode == TenseTripletMode.VOCAB && convBubbleControlRunning && currentVTabRows.isNotEmpty()) {
                            val idx = (lessonVocabAdapter?.currentIndex ?: nextIdx).coerceIn(0, currentVTabRows.lastIndex)
                            val nextRow = currentVTabRows.getOrNull(idx) ?: return@postDelayed
                            textToSpeech?.stop()
                            textToSpeech?.setLanguage(Locale.US)
                            textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(nextRow.word), TextToSpeech.QUEUE_FLUSH, null, "vocab_word")
                            expectedEnglishForVerification = nextRow.word
                            verificationHandler.postDelayed({
                                if (!isDestroyed && currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletMode == TenseTripletMode.VOCAB && convBubbleControlRunning) startVerificationListening(nextRow.word)
                            }, 800)
                        }
                    }, 1500)
                } else {
                    convBubbleControlRunning = false
                    tenseTripletControlRunning = false
                    runOnUiThread { updateTenseTripletControlBar() }
                }
            } else {
                verificationHandler.postDelayed({
                    if (!isDestroyed && currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletMode == TenseTripletMode.VOCAB && convBubbleControlRunning && currentVTabRows.isNotEmpty()) {
                        val idx = (lessonVocabAdapter?.currentIndex ?: curIdx).coerceIn(0, currentVTabRows.lastIndex)
                        val retryRow = currentVTabRows.getOrNull(idx) ?: return@postDelayed
                        textToSpeech?.stop()
                        textToSpeech?.setLanguage(Locale.US)
                        textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(retryRow.word), TextToSpeech.QUEUE_FLUSH, null, "vocab_word_after_fail")
                        expectedEnglishForVerification = retryRow.word
                        verificationHandler.postDelayed({
                            if (!isDestroyed && currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletMode == TenseTripletMode.VOCAB && convBubbleControlRunning) startVerificationListening(retryRow.word)
                        }, 800)
                    }
                }, 1500)
            }
            return
        }
        if (tenseTripletRows.isEmpty()) return
        @Suppress("UNUSED_VARIABLE")
        val legacyMatchIgnored = match
        val idx = tenseTripletCurrentIndex.coerceIn(0, tenseTripletRows.lastIndex)
        val row = tenseTripletRows[idx]
        if (tenseTripletAdjectiveDualMode) {
            val englishOnlyMatch = tenseTripletSentenceMatchStrict(row.past.english, spokenTextIgnored)
            val spokenEnglish = if (tenseTripletMode == TenseTripletMode.TEST) spokenTextIgnored else if (englishOnlyMatch) MatchNormalizer.textForSpeakAndDisplay(row.past.english) else null
            tenseTripletAdapter?.setSpokenText(idx, null, spokenEnglish, null)
            tenseTripletAdapter?.markCellResults(idx, null, englishOnlyMatch, null)
            if (tenseTripletMode != TenseTripletMode.TEST) {
                speakEnglishString(if (englishOnlyMatch) getString(R.string.correct) else getString(R.string.incorrect))
            } else if (!englishOnlyMatch) {
                speakEnglishString("try again")
            }
            if (englishOnlyMatch) {
                tenseTripletIncorrectCount = 0
                if (tenseTripletCurrentIndex < tenseTripletRows.lastIndex) {
                    tenseTripletCurrentIndex++
                    tenseTripletAdapter?.setCurrentIndex(tenseTripletCurrentIndex)
                    updateTenseTripletAutoScrollPosition()
                    if (tenseTripletControlRunning) {
                        verificationHandler.postDelayed({
                            if (!isDestroyed && currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletControlRunning) speakTenseTripletCurrent()
                        }, 1500)
                    }
                } else {
                    tenseTripletControlRunning = false
                    tenseTripletControlPaused = false
                    updateTenseTripletControlBar()
                }
            } else {
                tenseTripletIncorrectCount++
                if (tenseTripletControlRunning) {
                    if (tenseTripletIncorrectCount >= 3) {
                        tenseTripletIncorrectCount = 0
                        if (tenseTripletCurrentIndex < tenseTripletRows.lastIndex) {
                            tenseTripletCurrentIndex++
                            tenseTripletAdapter?.setCurrentIndex(tenseTripletCurrentIndex)
                            updateTenseTripletAutoScrollPosition()
                            verificationHandler.postDelayed({
                                if (!isDestroyed && currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletControlRunning) speakTenseTripletCurrent()
                            }, 1500)
                        } else {
                            tenseTripletControlRunning = false
                            tenseTripletControlPaused = false
                            updateTenseTripletControlBar()
                        }
                    } else {
                        verificationHandler.postDelayed({
                            if (!isDestroyed && currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletControlRunning) speakTenseTripletCurrent()
                        }, 1500)
                    }
                }
            }
            return
        }
        val presentMatch = if (tenseTripletShowPresent) tenseTripletSentenceMatch(row.present.english, spokenTextIgnored) else null
        val pastMatch = if (tenseTripletShowPast) tenseTripletSentenceMatch(row.past.english, spokenTextIgnored) else null
        val futureMatch = if (tenseTripletShowFuture) tenseTripletSentenceMatch(row.future.english, spokenTextIgnored) else null
        val (presentSpoken, pastSpoken, futureSpoken) = if (tenseTripletMode == TenseTripletMode.TEST) {
            // TEST: user speaks all columns in one go — split utterance into one substring per column in order.
            tenseTripletSplitSpokenIntoColumns(
                spokenTextIgnored,
                row,
                tenseTripletShowPresent,
                tenseTripletShowPast,
                tenseTripletShowFuture
            )
        } else {
            Triple(
                if (presentMatch == true) MatchNormalizer.textForSpeakAndDisplay(row.present.english) else null,
                if (pastMatch == true) MatchNormalizer.textForSpeakAndDisplay(row.past.english) else null,
                if (futureMatch == true) MatchNormalizer.textForSpeakAndDisplay(row.future.english) else null
            )
        }
        tenseTripletAdapter?.setSpokenText(idx, presentSpoken, pastSpoken, futureSpoken)
        tenseTripletAdapter?.markCellResults(idx, presentMatch, pastMatch, futureMatch)
        val enabledMatches = listOf(presentMatch, pastMatch, futureMatch).filterNotNull()
        val effectiveMatch = enabledMatches.isNotEmpty() && enabledMatches.all { it }
        speakEnglishString(if (effectiveMatch) getString(R.string.correct) else getString(R.string.incorrect))
        if (effectiveMatch) {
            tenseTripletIncorrectCount = 0
            if (tenseTripletCurrentIndex < tenseTripletRows.lastIndex) {
                tenseTripletCurrentIndex++
                tenseTripletAdapter?.setCurrentIndex(tenseTripletCurrentIndex)
                updateTenseTripletAutoScrollPosition()
                if (tenseTripletControlRunning) {
                    verificationHandler.postDelayed({
                        if (!isDestroyed && currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletControlRunning) {
                            speakTenseTripletCurrent()
                        }
                    }, 1500)
                }
            } else {
                tenseTripletControlRunning = false
                tenseTripletControlPaused = false
                updateTenseTripletControlBar()
            }
        } else {
            tenseTripletIncorrectCount++
            if (tenseTripletControlRunning) {
                if (tenseTripletIncorrectCount >= 3) {
                    tenseTripletIncorrectCount = 0
                    if (tenseTripletCurrentIndex < tenseTripletRows.lastIndex) {
                        tenseTripletCurrentIndex++
                        tenseTripletAdapter?.setCurrentIndex(tenseTripletCurrentIndex)
                        updateTenseTripletAutoScrollPosition()
                        verificationHandler.postDelayed({
                            if (!isDestroyed && currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletControlRunning) {
                                speakTenseTripletCurrent()
                            }
                        }, 1500)
                    } else {
                        tenseTripletControlRunning = false
                        tenseTripletControlPaused = false
                        updateTenseTripletControlBar()
                    }
                } else {
                    // Retry same row: repeat full speak -> listen -> compare cycle.
                    verificationHandler.postDelayed({
                        if (!isDestroyed && currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletControlRunning) {
                            speakTenseTripletCurrent()
                        }
                    }, 1500)
                }
            }
        }
    }

    private fun tenseTripletSentenceMatch(expectedSentence: String, spokenCombined: String): Boolean {
        if (expectedSentence.isBlank() || spokenCombined.isBlank()) return false
        if (MatchNormalizer.matchesExpectedWithAlternates(expectedSentence, spokenCombined)) return true
        val expectedNorm = normalizeForMatch(expectedSentence)
        val spokenNorm = normalizeForMatch(spokenCombined)
        if (expectedNorm.isBlank() || spokenNorm.isBlank()) return false
        return spokenNorm.contains(expectedNorm) || expectedNorm.contains(spokenNorm)
    }

    /** Adjective mode match: spoken may be longer, but must contain full expected sentence. */
    private fun tenseTripletSentenceMatchStrict(expectedSentence: String, spokenCombined: String): Boolean {
        if (expectedSentence.isBlank() || spokenCombined.isBlank()) return false
        val expectedNorm = normalizeForMatch(expectedSentence)
        val spokenNorm = normalizeForMatch(spokenCombined)
        if (expectedNorm.isBlank() || spokenNorm.isBlank()) return false
        return spokenNorm.contains(expectedNorm)
    }

    /**
     * Split one recognition string into present / past / future display slices by matching each expected
     * English sentence **in speak order** (left to right). Fills gaps between good matches so each
     * column gets a different part of the utterance.
     */
    private fun tenseTripletSplitSpokenIntoColumns(
        spokenRaw: String,
        row: TenseTripletRow,
        showP: Boolean,
        showPa: Boolean,
        showF: Boolean
    ): Triple<String?, String?, String?> {
        val spoken = MatchNormalizer.sanitizeSpokenTextForDisplay(spokenRaw).trim()
        if (spoken.isBlank()) return Triple(null, null, null)

        data class Col(val key: String, val english: String)
        val cols = buildList {
            if (showP) add(Col("p", row.present.english))
            if (showPa) add(Col("pa", row.past.english))
            if (showF) add(Col("f", row.future.english))
        }
        if (cols.isEmpty()) return Triple(null, null, null)
        if (cols.size == 1) {
            val only = spoken
            return when (cols[0].key) {
                "p" -> Triple(only, null, null)
                "pa" -> Triple(null, only, null)
                else -> Triple(null, null, only)
            }
        }

        val ranges = Array<IntRange?>(cols.size) { null }
        var cursor = 0
        for (i in cols.indices) {
            val r = findTripletPhraseRangeSequential(spoken, cols[i].english, cursor)
            ranges[i] = r
            if (r != null) {
                cursor = (r.last + 1).coerceAtMost(spoken.length)
                while (cursor < spoken.length && spoken[cursor].isWhitespace()) cursor++
            }
        }

        // Assign text between successful phrase matches to any column that failed (middle of utterance).
        tenseTripletFillMissingColumnRanges(spoken, ranges)

        fun textOrNull(r: IntRange?): String? {
            if (r == null || r.isEmpty()) return null
            val a = r.first.coerceIn(0, spoken.length)
            val b = (r.last + 1).coerceIn(a, spoken.length)
            return spoken.substring(a, b).trim().ifBlank { null }
        }

        val byKey = cols.mapIndexed { i, c -> c.key to ranges[i] }.toMap()
        return Triple(
            if (showP) textOrNull(byKey["p"]) else null,
            if (showPa) textOrNull(byKey["pa"]) else null,
            if (showF) textOrNull(byKey["f"]) else null
        )
    }

    /** Finds [expected] in [spoken] starting at or after [minStart], matching word-by-word in order. */
    private fun findTripletPhraseRangeSequential(spoken: String, expected: String, minStart: Int): IntRange? {
        if (expected.isBlank()) return null
        val words = expected.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotBlank() }
            .map { w -> w.trimEnd('.', ',', '!', '?', ';', ':', '"', '\'') }
            .filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        var firstIdx = -1
        var pos = minStart.coerceIn(0, spoken.length)
        for (w in words) {
            val i = tenseTripletFindWordStart(spoken, w, pos)
            if (i < 0) return null
            if (firstIdx < 0) firstIdx = i
            pos = (i + w.length).coerceAtMost(spoken.length)
        }
        if (firstIdx < 0) return null
        return firstIdx until pos
    }

    private fun tenseTripletFindWordStart(spoken: String, word: String, from: Int): Int {
        if (word.length <= 2) {
            val m = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE).find(spoken, from)
            return m?.range?.first ?: -1
        }
        return spoken.indexOf(word, from, ignoreCase = true)
    }

    /**
     * For columns where sequential match failed, use gaps between neighbours, then split prefix/suffix,
     * so the utterance is partitioned across columns instead of repeating the full text.
     */
    private fun tenseTripletFillMissingColumnRanges(spoken: String, ranges: Array<IntRange?>) {
        val n = ranges.size
        if (n == 0) return
        val firstNonNull = ranges.indexOfFirst { it != null }
        if (firstNonNull < 0) {
            tenseTripletSplitEvenCharSlices(spoken, ranges, (0 until n).toList(), 0, spoken.length)
            return
        }
        var changed = true
        while (changed) {
            changed = false
            for (i in 0 until n) {
                if (ranges[i] != null) continue
                val leftEnd = (i - 1 downTo 0).mapNotNull { ranges[it]?.last }.firstOrNull()?.plus(1)
                val rightStart = (i + 1 until n).mapNotNull { ranges[it]?.first }.firstOrNull()
                if (leftEnd != null && rightStart != null && rightStart > leftEnd) {
                    ranges[i] = leftEnd until rightStart
                    changed = true
                }
            }
        }
        if (firstNonNull > 0) {
            val end = ranges[firstNonNull]!!.first
            tenseTripletSplitEvenCharSlices(
                spoken,
                ranges,
                (0 until firstNonNull).filter { ranges[it] == null },
                0,
                end
            )
        }
        val lastNonNull = ranges.indexOfLast { it != null }
        if (lastNonNull >= 0 && lastNonNull < n - 1) {
            val start = ranges[lastNonNull]!!.last + 1
            tenseTripletSplitEvenCharSlices(
                spoken,
                ranges,
                (lastNonNull + 1 until n).filter { ranges[it] == null },
                start,
                spoken.length
            )
        }
        // Any column still null: keep Bengali there (avoid re-splitting whole utterance over good ranges).
    }

    private fun tenseTripletSplitEvenCharSlices(
        @Suppress("UNUSED_PARAMETER") spoken: String,
        ranges: Array<IntRange?>,
        colIndices: List<Int>,
        start: Int,
        end: Int
    ) {
        val targets = colIndices.filter { ranges[it] == null }
        if (targets.isEmpty() || end <= start) return
        val k = targets.size
        var pos = start
        for (i in targets.indices) {
            val remainingCols = k - i
            val remainingLen = end - pos
            val len = (remainingLen + remainingCols - 1) / remainingCols
            val e = (pos + len).coerceAtMost(end)
            if (e > pos) ranges[targets[i]] = pos until e
            pos = e
        }
    }

    /** Update THREECOL_TABLE statistics text: session correct / session attempted / total. Capped so never exceeds total. */
    private fun updateThreeColStats() {
        if (currentContentLayout != ContentLayout.THREECOL_TABLE) return
        val root = if (contentFrame.childCount > 0) contentFrame.getChildAt(0) else null
        val statView = root?.findViewById<TextView>(R.id.threecol_stat) ?: return
        val total = threeColBaseRows.size
        val c = threeColSessionCorrect.coerceIn(0, total)
        val a = threeColSessionAttempted.coerceIn(0, total)
        statView.text = "$c/$a/$total"
    }

    /** Load simple-sentence lesson (Let, How, Who, etc.) into SIMPLE_SENTENCE layout; keeps two-bubble Learning/Practice UI. */
    private fun loadSimpleSentenceLesson(actionKey: String) {
        // Resolve path from actual asset list so we open the same file the drawer discovered (avoids path/case issues)
        val expectedName = "$actionKey.txt"
        val resolvedSvoPath = assets.list("Lessons/SVO")?.firstOrNull { it == expectedName }
            ?.let { "Lessons/SVO/$it" }
        val candidatePaths = buildList {
            if (resolvedSvoPath != null) add(resolvedSvoPath)
            add("Lessons/SVO/$actionKey.txt")
            add("Lessons/$actionKey.txt")
        }.distinct()
        var content: String? = null
        var usedPath: String? = null
        for (assetPath in candidatePaths) {
            try {
                content = assets.open(assetPath).bufferedReader(StandardCharsets.UTF_8).readText()
                usedPath = assetPath
                break
            } catch (_: Exception) { }
        }
        if (content == null || usedPath == null) {
            Toast.makeText(this, "Could not load $actionKey from Lessons/SVO or Lessons/", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val rows = mutableListOf<LessonRow>()
            val pronunciations = mutableListOf<String>()
            for (line in lines) {
                if (line.startsWith("Topic:", ignoreCase = true)) continue
                val parts = line.split(",").map { it.trim() }
                if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    val english = parts[0]
                    val bengali = parts[1]
                    rows.add(LessonRow(english, bengali, english, bengali))
                    pronunciations.add(parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "")
                }
            }
            if (rows.isEmpty()) { Toast.makeText(this, "No valid rows in $usedPath", Toast.LENGTH_SHORT).show(); return }
            clearPronunciationLessonState()
            lessonRows = rows
            simpleSentencePronunciations = pronunciations
            lessonName = SimpleSentenceUtils.simpleSentenceLessonTitle(actionKey)
            lessonCorrectCount = 0
            lessonMode = 4
            lessonIndex = 0
            lessonPhase = "q"
            lessonMode3Listening = false
            lessonMode3SpokeAnswer = false
            lessonIncorrectCount = 0
            incorrectLessonRows.clear()
            incorrectLessonSourceName = null
            nextButton?.isEnabled = true
            skipButton?.isEnabled = true
            setSentenceListVisibility(false)
            updateLessonStatistic()
            updateLessonTopicDisplay()
            contentFrame.post {
                updateSimpleSentenceView()
                updateSimpleSentenceTabAppearance(
                    contentFrame.getChildAt(0)?.findViewById(R.id.simple_sentence_mode_learning),
                    contentFrame.getChildAt(0)?.findViewById(R.id.simple_sentence_mode_practice)
                )
            }
            expectedEnglishForVerification = rows[0].engA
            textToSpeech?.setLanguage(Locale("bn"))
            simpleSentenceControlRunning = true
            simpleSentenceControlPaused = false
            contentFrame.post { updateSimpleSentenceControlBar() }
            if (simpleSentencePracticeMode) {
                textToSpeech?.speak(rows[0].bnQ, TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
            } else {
                pendingSimpleSentenceEnglishForLearning = rows[0].engA
                textToSpeech?.speak(rows[0].bnQ, TextToSpeech.QUEUE_FLUSH, null, "lesson_verify_bengali")
            }
        } catch (e: Exception) { Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    /** Set "You said:" recognized text in simple-sentence layout (call after verification). */
    private fun setSimpleSentenceYouSaid(text: String) {
        if (currentContentLayout != ContentLayout.SIMPLE_SENTENCE) return
        val root: View? = if (contentFrame.childCount > 0) contentFrame.getChildAt(0) else null
        val youSaid = root?.findViewById<TextView>(R.id.simple_sentence_you_said) ?: findViewById(R.id.simple_sentence_you_said) as? TextView
        youSaid?.text = MatchNormalizer.sanitizeSpokenTextForDisplay(text)
    }

    /** Update the two-bubble simple-sentence view from current lesson row (call when layout is SIMPLE_SENTENCE). */
    private fun updateSimpleSentenceView() {
        if (currentContentLayout != ContentLayout.SIMPLE_SENTENCE) return
        val root: View? = if (contentFrame.childCount > 0) contentFrame.getChildAt(0) else null
        fun find(id: Int): View? = root?.findViewById(id) ?: findViewById(id)
        val row = lessonRows?.getOrNull(lessonIndex)
        if (row != null) {
            find(R.id.simple_sentence_bengali)?.let { (it as? TextView)?.text = row.bnQ }
            find(R.id.simple_sentence_english)?.let { v ->
                (v as? TextView)?.text = row.engA
                v.visibility = if (simpleSentencePracticeMode) View.GONE else View.VISIBLE
            }
            find(R.id.simple_sentence_pronunciation)?.let { v ->
                val tv = v as? TextView
                tv?.text = simpleSentencePronunciations?.getOrNull(lessonIndex) ?: ""
                v.visibility = if (simpleSentencePracticeMode) View.GONE else View.VISIBLE
            }
            find(R.id.simple_sentence_you_said)?.let { (it as? TextView)?.text = "" }
            find(R.id.simple_sentence_badge)?.visibility = View.GONE
        }
        val rows = lessonRows
        val statView = find(R.id.simple_sentence_stat) as? TextView
        if (rows != null && !rows.isEmpty() && statView != null) {
            statView.visibility = View.VISIBLE
            val asked = lessonIndex
            val pct = if (asked > 0) (100 * lessonCorrectCount / asked) else 0
            statView.text = "$lessonCorrectCount/$asked ($pct%)"
        } else {
            statView?.visibility = View.GONE
        }
    }

    private fun showLoadIncorrectDialog() {
        val incFiles = filesDir.listFiles()?.filter { it.isFile && it.name.endsWith(LessonFileParsers.INCORRECT_LESSON_SUFFIX) }?.sortedBy { it.name } ?: emptyList()
        if (incFiles.isEmpty()) { Toast.makeText(this, getString(R.string.no_incorrect_saved), Toast.LENGTH_SHORT).show(); return }
        val names = incFiles.map { it.name.removeSuffix(LessonFileParsers.INCORRECT_LESSON_SUFFIX) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.load_practice_incorrect))
            .setItems(names) { _, which ->
                val fileName = incFiles[which].name
                val (displayName, rows) = loadIncorrectLessonListFromFile(fileName)
                if (rows.isEmpty()) { Toast.makeText(this, getString(R.string.no_incorrect_saved), Toast.LENGTH_SHORT).show(); return@setItems }
                clearPronunciationLessonState()
                lessonRows = rows; lessonName = displayName ?: names[which]
                lessonCorrectCount = 0; lessonMode = 4; lessonIndex = 0; lessonPhase = "q"
                lessonMode3Listening = false; lessonMode3SpokeAnswer = false; lessonIncorrectCount = 0
                nextButton?.isEnabled = true; skipButton?.isEnabled = true
                clearBothTextAreas(); setSentenceListVisibility(false)
                updateLessonStatistic(); updateLessonTopicDisplay()
                showVerbDiagram(verbForLessonDiagram(lessonName))
                textView.text = getString(R.string.lesson_loaded)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ───────────────────── Content Layout Switching ─────────────────────

    /** Inflates [R.layout.layout_lesson_base] and attaches [contentLayoutRes] into [R.id.lesson_base_content]. */
    private fun inflateLessonShellWithContent(contentLayoutRes: Int): View {
        val shell = layoutInflater.inflate(R.layout.layout_lesson_base, contentFrame, false)
        val host = shell.findViewById<android.widget.FrameLayout>(R.id.lesson_base_content)
        layoutInflater.inflate(contentLayoutRes, host, true)
        return shell
    }

    /** Same shell as [inflateLessonShellWithContent], plus block title row in [R.id.lesson_base_top_extra]. */
    private fun inflatePrepositionLessonShell(): View {
        val shell = layoutInflater.inflate(R.layout.layout_lesson_base, contentFrame, false)
        val topExtra = shell.findViewById<android.widget.FrameLayout>(R.id.lesson_base_top_extra)
        topExtra.visibility = View.VISIBLE
        layoutInflater.inflate(R.layout.layout_preposition_blocks_top_extra, topExtra, true)
        val host = shell.findViewById<android.widget.FrameLayout>(R.id.lesson_base_content)
        layoutInflater.inflate(R.layout.layout_preposition_blocks_content, host, true)
        return shell
    }

    /**
     * [layout_lesson_base] with magenta mode bar visible; part navigation in [lesson_base_top_extra].
     */
    private fun inflateExtendSentenceLessonShell(): View {
        val shell = layoutInflater.inflate(R.layout.layout_lesson_base, contentFrame, false)
        val topExtra = shell.findViewById<android.widget.FrameLayout>(R.id.lesson_base_top_extra)
        topExtra.visibility = View.VISIBLE
        layoutInflater.inflate(R.layout.layout_extend_sentence_top_extra, topExtra, true)
        val host = shell.findViewById<android.widget.FrameLayout>(R.id.lesson_base_content)
        layoutInflater.inflate(R.layout.layout_extend_sentence_content, host, true)
        return shell
    }

    /**
     * Switch the content area to one of the predefined layouts.
     * Returns the inflated View so the caller can populate it.
     * Switching to LEGACY restores the original layout with all existing views.
     */
    fun switchContentLayout(layout: ContentLayout): View {
        // Always re-inflate when requested from drawer/subtopic navigation.
        // Reusing the existing same-layout view can leave stale/empty content in some paths.
        if (currentContentLayout == ContentLayout.PREPOSITION_BLOCKS) {
            cancelPrepositionBlocksAutoAdvance()
            prepositionBlocksRecycler = null
            prepositionBlocksRoot = null
        }
        if (currentContentLayout == ContentLayout.EXTEND_SENTENCE) {
            extendSentenceRoot = null
            extendSentenceRecycler = null
        }
        contentFrame.removeAllViews()
        // Clear references to previous layout-specific views
        if (currentContentLayout == ContentLayout.SPEECH_INPUT) speechInputView = null
        if (currentContentLayout == ContentLayout.TEXT_DISPLAY) textDisplaySpeakButton = null
        if (currentContentLayout == ContentLayout.PRACTICE_THREE_AREA) practiceView = null

        val view = when (layout) {
            ContentLayout.TENSE_TRIPLETS -> inflateLessonShellWithContent(R.layout.layout_tense_triplets_content)
            ContentLayout.THREECOL_TABLE -> inflateLessonShellWithContent(R.layout.layout_threecol_content)
            ContentLayout.CONVERSATION_BUBBLES -> inflateLessonShellWithContent(R.layout.layout_conversation_bubbles_content)
            ContentLayout.PREPOSITION_BLOCKS -> inflatePrepositionLessonShell()
            ContentLayout.EXTEND_SENTENCE -> inflateExtendSentenceLessonShell()
            else -> {
                val layoutRes = getContentLayoutResId(layout)
                LayoutInflater.from(this).inflate(layoutRes, contentFrame, false)
            }
        }
        contentFrame.addView(view)
        currentContentLayout = layout
        if (layout == ContentLayout.TENSE_TRIPLETS) {
            tenseTripletInflatedContentRes = R.layout.layout_tense_triplets_content
        } else {
            tenseTripletInflatedContentRes = -1
        }

        // App top bar (hamburger, lesson title) is shared across all content layouts — keep it visible.
        findViewById<View>(R.id.top_bar)?.visibility = View.VISIBLE

        // Rebind control bar to the one that is visible for this layout, so PAUSE/RESUME text and clicks apply to the same bar the user sees.
        val activeBar = when (layout) {
            ContentLayout.CONVERSATION_BUBBLES,
            ContentLayout.THREECOL_TABLE,
            ContentLayout.TENSE_TRIPLETS,
            ContentLayout.PREPOSITION_BLOCKS,
            ContentLayout.EXTEND_SENTENCE -> view.findViewById<View>(R.id.lesson_base_control_include)
            else -> findViewById(R.id.control_actions_include)
        }
        controlActionsBar = activeBar
        controlStartStopButton = activeBar?.findViewById(R.id.control_start_stop)
        controlPauseResumeButton = activeBar?.findViewById(R.id.control_pause_resume)
        controlPlaybackLastButton = activeBar?.findViewById(R.id.control_playback_last)
        setupHoldToRecordPlaybackButton(controlPlaybackLastButton)
        bindControlBarListeners()

        // Main bar (activity-level) visibility: these layouts use their own bar in content.
        if (layout == ContentLayout.CONVERSATION_BUBBLES || layout == ContentLayout.THREECOL_TABLE || layout == ContentLayout.TENSE_TRIPLETS || layout == ContentLayout.EXTEND_SENTENCE || layout == ContentLayout.PREPOSITION_BLOCKS) {
            findViewById<View>(R.id.control_actions_include)?.visibility = View.GONE
            activeBar?.visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.control_actions_include)?.visibility = if (usesControlActions(layout)) View.VISIBLE else View.GONE
        }
        // Hide bottom bar when layout has its own in-content control bar.
        findViewById<View>(R.id.bottom_bar)?.visibility =
            if (layout == ContentLayout.THREECOL_TABLE || layout == ContentLayout.CONVERSATION_BUBBLES || layout == ContentLayout.TENSE_TRIPLETS || layout == ContentLayout.EXTEND_SENTENCE || layout == ContentLayout.PREPOSITION_BLOCKS) View.GONE else View.VISIBLE
        when (layout) {
            ContentLayout.CONVERSATION_BUBBLES -> {
                convBubbleControlRunning = false
                convBubbleControlPaused = false
                convBubbleIncorrectCount = 0
                updateConvBubbleControlBar()
            }
            ContentLayout.SIMPLE_SENTENCE -> {
                setupSimpleSentenceButtons(view)
                simpleSentenceControlRunning = false
                simpleSentenceControlPaused = false
                updateSimpleSentenceControlBar()
            }
            ContentLayout.SV_RIBBON -> {
                setupSvRibbonModeButtons(view)
                svRibbonControlRunning = false
                svRibbonControlPaused = false
                updateSvRibbonControlBar()
            }
            ContentLayout.THREECOL_TABLE -> {
                threeColControlRunning = false
                threeColControlPaused = false
                threeColIncorrectCount = 0
                updateThreeColControlBar()
            }
            ContentLayout.TENSE_TRIPLETS -> {
                tenseTripletControlRunning = false
                tenseTripletControlPaused = false
                tenseTripletIncorrectCount = 0
                updateTenseTripletControlBar()
            }
            ContentLayout.EXTEND_SENTENCE -> {
                convBubbleControlRunning = false
                extendSentenceControlRunning = false
                extendSentenceControlPaused = false
                updateExtendSentenceControlBar()
            }
            ContentLayout.PREPOSITION_BLOCKS -> {
                prepositionBlocksControlRunning = false
                prepositionBlocksControlPaused = false
                updatePrepositionBlocksControlBar()
            }
            else -> if (usesControlActions(layout)) updateGenericControlBar()
        }

        // If switching back to legacy, re-bind all the existing view references
        if (layout == ContentLayout.LEGACY) {
            rebindLegacyViews()
        }
        return view
    }

    /** POC: Fill the button menu.
     *  - If [onBackToHome] is provided, show a "Back" tile that restores the home topic grid.
     *  - Tap a tile navigates to that subtopic using [handleSubtopicAction].
     */
    private fun setupPocButtonMenu(root: View, topic: Topic, onBackToHome: (() -> Unit)? = null) {
        root.findViewById<TextView>(R.id.poc_menu_title)?.text = topic.title
        val container = root.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.poc_button_container) ?: return
        container.removeAllViews()

        val dp = resources.displayMetrics.density
        val density = resources.displayMetrics.density
        val scrollPaddingSidePx = (20 * density).toInt() // matches layout_poc_button_menu.xml
        val availableWidthPx = (resources.displayMetrics.widthPixels - scrollPaddingSidePx * 2).coerceAtLeast(1)
        val gapPx = (12 * dp).toInt()
        val columns = if (availableWidthPx >= (800 * dp).toInt()) 4 else 3
        val minTilePx = (96 * dp).toInt()
        val tileSizePx = ((availableWidthPx - gapPx * (columns - 1)) / columns).coerceAtLeast(minTilePx)

        // Optional back tile (used only by POC home).
        if (onBackToHome != null) {
            val backBtn = Button(this).apply {
                text = "Back"
                setBackgroundResource(R.drawable.bg_poc_menu_button)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.control_btn_text))
                textSize = 14f
                setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setAllCaps(false)
                gravity = android.view.Gravity.CENTER
                setOnClickListener { onBackToHome.invoke() }
            }
            val lp = com.google.android.flexbox.FlexboxLayout.LayoutParams(tileSizePx, tileSizePx).apply {
                setMargins(gapPx / 2, gapPx / 2, gapPx / 2, gapPx / 2)
            }
            container.addView(backBtn, lp)
        }

        for (sub in topic.subtopics) {
            val btn = Button(this).apply {
                text = sub.title
                setBackgroundResource(R.drawable.bg_poc_menu_button)
                // POC buttons use a blue background (see bg_poc_menu_button.xml), so keep text readable.
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.control_btn_text))
                textSize = 14f
                setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
                maxLines = 3
                // Keep the tile shape; truncate if title is too long.
                ellipsize = android.text.TextUtils.TruncateAt.END
                // Avoid Android's default all-caps styling.
                setAllCaps(false)
                gravity = android.view.Gravity.CENTER
                setOnClickListener {
                    if (sub.layoutType != ContentLayout.LEGACY) {
                        switchContentLayout(sub.layoutType)
                    } else if (currentContentLayout != ContentLayout.LEGACY) {
                        switchContentLayout(ContentLayout.LEGACY)
                    }
                    handleSubtopicAction(sub)
                }
            }
            val lp = com.google.android.flexbox.FlexboxLayout.LayoutParams(tileSizePx, tileSizePx).apply {
                // Margins simulate "gap" between tiles.
                setMargins(gapPx / 2, gapPx / 2, gapPx / 2, gapPx / 2)
            }
            container.addView(btn, lp)
        }
    }

    /** POC Home: first-level tiles.
     * Clicking a topic replaces the grid with its subtopics (square tiles).
     */
    private fun setupPocHomeTopics(root: View, homeTopics: List<Topic>) {
        root.findViewById<TextView>(R.id.poc_menu_title)?.text = "POC Home"
        val container = root.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.poc_button_container) ?: return
        container.removeAllViews()

        val dp = resources.displayMetrics.density
        val density = resources.displayMetrics.density
        val scrollPaddingSidePx = (20 * density).toInt()
        val availableWidthPx = (resources.displayMetrics.widthPixels - scrollPaddingSidePx * 2).coerceAtLeast(1)
        val gapPx = (12 * dp).toInt()
        val columns = if (availableWidthPx >= (800 * dp).toInt()) 4 else 3
        val minTilePx = (96 * dp).toInt()
        val tileSizePx = ((availableWidthPx - gapPx * (columns - 1)) / columns).coerceAtLeast(minTilePx)

        for (topic in homeTopics) {
            val btn = Button(this).apply {
                text = topic.title
                setBackgroundResource(R.drawable.bg_poc_menu_button)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.control_btn_text))
                textSize = 14f
                setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
                maxLines = 3
                ellipsize = android.text.TextUtils.TruncateAt.END
                setAllCaps(false)
                gravity = android.view.Gravity.CENTER
                setOnClickListener {
                    // Show subtopics as square tiles and allow returning back home.
                    setupPocButtonMenu(root, topic) { setupPocHomeTopics(root, homeTopics) }
                }
            }
            val lp = com.google.android.flexbox.FlexboxLayout.LayoutParams(tileSizePx, tileSizePx).apply {
                setMargins(gapPx / 2, gapPx / 2, gapPx / 2, gapPx / 2)
            }
            container.addView(btn, lp)
        }
    }

    /** Builds the initial "home screen" POC topics (test only). */
    private fun buildPocHomeTopics(): List<Topic> {
        return listOf(
            Topic(
                "POC Topic A",
                listOf(
                    Subtopic("Introduction (Bengali)", "intro_bengali", ContentLayout.TEXT_DISPLAY),
                    Subtopic("Mic Test", "mic_test", ContentLayout.MIC_SPEAKER_TEST),
                    Subtopic("Translation Practice", "translation_practice", ContentLayout.PRACTICE_THREE_AREA)
                )
            ),
            Topic(
                "POC Topic B",
                listOf(
                    Subtopic("First meeting (bubbles)", "conv_bubble_first_meeting", ContentLayout.CONVERSATION_BUBBLES),
                    Subtopic("Second lesson (bubbles)", "conv_bubble_second_lesson", ContentLayout.CONVERSATION_BUBBLES),
                    Subtopic("Mic Test", "mic_test", ContentLayout.MIC_SPEAKER_TEST)
                )
            )
        )
    }

    /** Wire Learning / Practice tab buttons and stat for simple-sentence layout. */
    private fun setupSimpleSentenceButtons(root: View) {
        val learningBtn = root.findViewById<Button>(R.id.simple_sentence_mode_learning)
        val practiceBtn = root.findViewById<Button>(R.id.simple_sentence_mode_practice)
        learningBtn?.setOnClickListener {
            simpleSentencePracticeMode = false
            updateSimpleSentenceView()
            updateSimpleSentenceTabAppearance(learningBtn, practiceBtn)
        }
        practiceBtn?.setOnClickListener {
            simpleSentencePracticeMode = true
            updateSimpleSentenceView()
            updateSimpleSentenceTabAppearance(learningBtn, practiceBtn)
        }
        updateSimpleSentenceTabAppearance(learningBtn, practiceBtn)
    }

    private fun updateSimpleSentenceTabAppearance(learningBtn: View?, practiceBtn: View?) {
        val blue = ContextCompat.getColor(this, R.color.lesson_topic_bar_background)
        val white = 0xFFFFFFFF.toInt()
        val gray = 0xFFE0E0E0.toInt()
        val darkText = 0xFF555555.toInt()
        val learningSelected = !simpleSentencePracticeMode
        (learningBtn as? TextView)?.let {
            it.setBackgroundColor(if (learningSelected) blue else gray)
            it.setTextColor(if (learningSelected) white else darkText)
        }
        (practiceBtn as? TextView)?.let {
            it.setBackgroundColor(if (simpleSentencePracticeMode) blue else gray)
            it.setTextColor(if (simpleSentencePracticeMode) white else darkText)
        }
    }

    private fun updateSimpleSentenceControlBar() {
        controlStartStopButton?.let { ControlBarUtils.setControlStartStopButton(this, it, simpleSentenceControlRunning) }
        controlPauseResumeButton?.let { ControlBarUtils.setControlPauseResumeButton(this, it, simpleSentenceControlPaused) }
    }

    private fun onSimpleSentenceStartStop() {
        if (simpleSentenceControlRunning) {
            textToSpeech?.stop()
            cancelVerificationTimeout()
            if (verificationMode) {
                verificationMode = false
                expectedEnglishForVerification = null
                try { speechRecognizer?.stopListening() } catch (_: Exception) { }
                stopEnglishVoskRecording()
                setMicButtonAppearance(recording = false)
            }
            isEnglishMicActive = false
            isRecording = false
            simpleSentenceControlRunning = false
        } else {
            simpleSentenceControlPaused = false
            simpleSentenceControlRunning = true
            updateSimpleSentenceControlBar()
            onNextLessonStep()
        }
        updateSimpleSentenceControlBar()
    }

    private fun onSimpleSentencePauseResume() {
        if (simpleSentenceControlPaused) {
            simpleSentenceControlPaused = false
            simpleSentenceControlRunning = true
            updateSimpleSentenceControlBar()
            onNextLessonStep()
        } else {
            textToSpeech?.stop()
            cancelVerificationTimeout()
            if (verificationMode) {
                verificationMode = false
                expectedEnglishForVerification = null
                try { speechRecognizer?.stopListening() } catch (_: Exception) { }
                stopEnglishVoskRecording()
                setMicButtonAppearance(recording = false)
            }
            isEnglishMicActive = false
            isRecording = false
            simpleSentenceControlRunning = false
            simpleSentenceControlPaused = true
        }
        updateSimpleSentenceControlBar()
    }

    /** SV_RIBBON: Start = speak current line, Stop = stop TTS; Pause/Resume = stop / speak current again. */
    private fun updateSvRibbonControlBar() {
        controlStartStopButton?.let { ControlBarUtils.setControlStartStopButton(this, it, svRibbonControlRunning) }
        controlPauseResumeButton?.let { ControlBarUtils.setControlPauseResumeButton(this, it, svRibbonControlPaused) }
    }

    private fun onSvRibbonStartStop() {
        if (svRibbonControlRunning) {
            textToSpeech?.stop()
            cancelVerificationTimeout()
            if (verificationMode) {
                verificationMode = false
                expectedEnglishForVerification = null
                try { speechRecognizer?.stopListening() } catch (_: Exception) { }
                stopEnglishVoskRecording()
                setMicButtonAppearance(recording = false)
            }
            isEnglishMicActive = false
            isRecording = false
            svRibbonControlRunning = false
        } else {
            svRibbonControlPaused = false
            svRibbonControlRunning = true
            speakCurrentSvRibbonBengali()
        }
        updateSvRibbonControlBar()
    }

    private fun onSvRibbonPauseResume() {
        if (svRibbonControlPaused) {
            svRibbonControlPaused = false
            svRibbonControlRunning = true
            speakCurrentSvRibbonBengali()
        } else {
            textToSpeech?.stop()
            cancelVerificationTimeout()
            if (verificationMode) {
                verificationMode = false
                expectedEnglishForVerification = null
                try { speechRecognizer?.stopListening() } catch (_: Exception) { }
                stopEnglishVoskRecording()
                setMicButtonAppearance(recording = false)
            }
            isEnglishMicActive = false
            isRecording = false
            svRibbonControlRunning = false
            svRibbonControlPaused = true
        }
        updateSvRibbonControlBar()
    }

    /** Speak the Bengali line at the current conveyor index (SV_RIBBON). */
    private fun speakCurrentSvRibbonBengali() {
        val list = svRibbonBengali ?: return
        val root = contentFrame.getChildAt(0) ?: return
        val conveyorLeft = root.findViewById<ConveyorBeltView>(R.id.conveyor_left) ?: return
        val idx = conveyorLeft.getCurrentIndex()
        val bengali = list.getOrNull(idx)
        if (!bengali.isNullOrBlank() && ttsReady && !isDestroyed) {
            textToSpeech?.setLanguage(Locale("bn"))
            textToSpeech?.speak(bengali, TextToSpeech.QUEUE_FLUSH, null, "sv_ribbon")
        }
    }

    /** Dispatch Start/Stop, Pause/Resume for layouts that use the shared control bar. */
    private fun dispatchControlStartStop() {
        if (currentContentLayout == ContentLayout.CONVERSATION_BUBBLES && convBubbleRows.isNotEmpty()) {
            onConvBubbleStartStop()
        } else if (currentContentLayout == ContentLayout.THREECOL_TABLE && threeColRows.isNotEmpty()) {
            onThreeColStartStop()
        } else if (currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletRows.isNotEmpty()) {
            onTenseTripletStartStop()
        } else if (currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceGroups.isNotEmpty()) {
            onExtendSentenceStartStop()
        } else if (currentContentLayout == ContentLayout.PREPOSITION_BLOCKS && prepositionBlockRows.isNotEmpty()) {
            onPrepositionBlocksStartStop()
        }
    }

    private fun dispatchControlPauseResume() {
        if (currentContentLayout == ContentLayout.CONVERSATION_BUBBLES && convBubbleRows.isNotEmpty()) {
            onConvBubblePauseResume()
        } else if (currentContentLayout == ContentLayout.THREECOL_TABLE && threeColRows.isNotEmpty()) {
            onThreeColPauseResume()
        } else if (currentContentLayout == ContentLayout.TENSE_TRIPLETS && tenseTripletRows.isNotEmpty()) {
            onTenseTripletPauseResume()
        } else if (currentContentLayout == ContentLayout.EXTEND_SENTENCE && extendSentenceGroups.isNotEmpty()) {
            onExtendSentencePauseResume()
        } else if (currentContentLayout == ContentLayout.PREPOSITION_BLOCKS && prepositionBlockRows.isNotEmpty()) {
            onPrepositionBlocksPauseResume()
        }
    }

    /** Set control bar to Start + Pause for layouts that show the bar but have no running state. */
    private fun updateGenericControlBar() {
        controlStartStopButton?.let { ControlBarUtils.setControlStartStopButton(this, it, false) }
        controlPauseResumeButton?.let { ControlBarUtils.setControlPauseResumeButton(this, it, false) }
    }

    /** After switching back to legacy layout, re-bind all views that setupUI originally found. */
    private fun rebindLegacyViews() {
        textView = findViewById(R.id.my_text)
        textView.movementMethod = ScrollingMovementMethod()
        englishTextView = findViewById(R.id.english_text)
        englishTextView.movementMethod = ScrollingMovementMethod()
        translationLabel = findViewById(R.id.translation_label)
        descriptionWebView = findViewById(R.id.description_webview)
        descriptionWebView.settings.javaScriptEnabled = false
        descriptionWebView.setBackgroundColor(0)
        descriptionWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        descriptionWebView.webViewClient = object : WebViewClient() {
            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null && url.startsWith("letter://")) {
                    val letter = url.removePrefix("letter://").take(1).uppercase()
                    if (letter.isNotEmpty()) {
                        val bengaliPronunciation = alphabetBengaliPronunciation[letter]
                        if (bengaliPronunciation != null) {
                            textToSpeech?.setLanguage(Locale("bn"))
                            textToSpeech?.speak(bengaliPronunciation, TextToSpeech.QUEUE_FLUSH, null, "letter_pronunciation")
                            Toast.makeText(this@MainActivity, "$letter → $bengaliPronunciation", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return true
                }
                if (url != null && url.startsWith("word://")) {
                    val encoded = url.removePrefix("word://")
                    val word = try {
                        URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
                    } catch (_: Exception) { encoded }
                    if (word.isNotEmpty()) {
                        textToSpeech?.setLanguage(Locale.US)
                        textToSpeech?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "pronunciation_word")
                        Toast.makeText(this@MainActivity, word, Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                return false
            }
        }
        inputLanguageGroup = findViewById(R.id.input_language_group)
        sentenceRecyclerView = findViewById(R.id.sentence_list)
        sentenceRecyclerView.layoutManager = LinearLayoutManager(this)
        sentenceRecyclerView.adapter = sentenceAdapter
        descriptionSpeakerButton = findViewById(R.id.description_speaker_button)
        descriptionSpeakerButton.setOnClickListener {
            val text = descriptionInstructionText
            if (!text.isNullOrEmpty()) {
                val locale = descriptionInstructionLocale ?: Locale("bn")
                textToSpeech?.setLanguage(locale)
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "description_instruction")
                Toast.makeText(this, getString(R.string.speak_instruction), Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<ImageButton>(R.id.speak_english_button).setOnClickListener { onSpeakEnglishButton() }
    }

    /** Show text-display layout with a title and body text. Optionally speak it. */
    fun showTextDisplayLayout(title: String?, bodyText: String, speakBengali: Boolean = false) {
        val view = switchContentLayout(ContentLayout.TEXT_DISPLAY)
        val titleView = view.findViewById<TextView>(R.id.text_display_title)
        val bodyView = view.findViewById<TextView>(R.id.text_display_body)
        val speakBtn = view.findViewById<ImageButton>(R.id.text_display_speak_button)
        textDisplaySpeakButton = speakBtn
        textDisplayBodyText = bodyText

        if (title.isNullOrBlank()) {
            titleView.visibility = View.GONE
        } else {
            titleView.visibility = View.VISIBLE
            titleView.text = title
        }
        bodyView.text = bodyText

        // Reset state
        ttsPlayState = TtsPlayState.IDLE
        introSegments = emptyList()
        introSegmentIndex = 0
        updateSpeakButtonIcon()

        speakBtn.setOnClickListener {
            when (ttsPlayState) {
                TtsPlayState.IDLE -> {
                    // Start speaking from the beginning
                    if (textDisplayBodyText.isNotBlank() && ttsReady && textToSpeech != null) {
                        textToSpeech?.stop()
                        introSegmentIndex = 0
                        ttsPlayState = TtsPlayState.SPEAKING
                        updateSpeakButtonIcon()
                        speakIntroductionBengali(textDisplayBodyText, fromSegment = 0)
                    }
                }
                TtsPlayState.SPEAKING -> {
                    // Pause: stop TTS but remember where we were
                    textToSpeech?.stop()
                    ttsPlayState = TtsPlayState.PAUSED
                    updateSpeakButtonIcon()
                    Toast.makeText(this, "Paused. Tap to resume.", Toast.LENGTH_SHORT).show()
                }
                TtsPlayState.PAUSED -> {
                    // Resume from the segment where we paused
                    if (textDisplayBodyText.isNotBlank() && ttsReady && textToSpeech != null) {
                        ttsPlayState = TtsPlayState.SPEAKING
                        updateSpeakButtonIcon()
                        speakIntroductionBengali(textDisplayBodyText, fromSegment = introSegmentIndex)
                    }
                }
            }
        }

        // Long-press to stop completely and reset to beginning
        speakBtn.setOnLongClickListener {
            stopTextDisplaySpeaking()
            Toast.makeText(this, "Stopped.", Toast.LENGTH_SHORT).show()
            true
        }

        if (speakBengali && bodyText.isNotBlank()) {
            textToSpeech?.stop()
            ttsPlayState = TtsPlayState.SPEAKING
            updateSpeakButtonIcon()
            view.postDelayed({ speakIntroductionBengali(bodyText) }, 500)
        }
    }

    /** Update the speaker icon based on current TTS play state. */
    private fun updateSpeakButtonIcon() {
        val btn = textDisplaySpeakButton ?: return
        when (ttsPlayState) {
            TtsPlayState.IDLE -> btn.setImageResource(R.drawable.ic_speak_english)       // black/default
            TtsPlayState.SPEAKING -> btn.setImageResource(R.drawable.ic_speak_active)    // blue with waves
            TtsPlayState.PAUSED -> btn.setImageResource(R.drawable.ic_speak_paused)      // orange with pause bars
        }
    }

    /** Stop TTS reading and reset icon to idle, reset position to beginning. */
    private fun stopTextDisplaySpeaking() {
        textToSpeech?.stop()
        ttsPlayState = TtsPlayState.IDLE
        introSegmentIndex = 0
        updateSpeakButtonIcon()
    }

    /** Show speech-input layout. Returns the view for further customization. */
    fun showSpeechInputLayout(): View {
        return switchContentLayout(ContentLayout.SPEECH_INPUT)
    }

    /** If speech-input layout is active, append recognized text there. Returns true if handled. */
    private fun feedSpeechInputText(text: String, isFinal: Boolean): Boolean {
        if (currentContentLayout != ContentLayout.SPEECH_INPUT) return false
        val view = speechInputView ?: return false
        val tv = view.findViewById<TextView>(R.id.speech_recognized_text) ?: return false
        val statusText = view.findViewById<TextView>(R.id.speech_status_text) ?: return false
        if (isFinal) {
            // Add দাঁড়ি (।) for Bengali, period for English
            val punctuated = if (speechInputLangBengali) {
                if (text.endsWith("।") || text.endsWith("৷")) text else "$text।"
            } else {
                if (text.endsWith(".") || text.endsWith("?") || text.endsWith("!")) text else "$text."
            }
            val existing = tv.text.toString().trim()
            tv.text = if (existing.isEmpty()) punctuated else "$existing\n$punctuated"
            statusText.text = "Ready"
        } else {
            // Show partial in status bar
            statusText.text = "Listening: $text"
        }
        return true
    }

    /** Track which language is selected in the speech-input layout. */
    private var speechInputLangBengali = true

    /** Check which language is selected in the speech-input layout. Returns true for Bengali. */
    private fun isSpeechInputBengali(): Boolean = speechInputLangBengali

    /** Set up the language toggle buttons in the speech-input layout. */
    private fun setupSpeechInputLangToggle(view: View) {
        val btnBangla = view.findViewById<TextView>(R.id.speech_lang_bangla)
        val btnEnglish = view.findViewById<TextView>(R.id.speech_lang_english)
        speechInputLangBengali = true
        updateLangToggleAppearance(btnBangla, btnEnglish, bengaliSelected = true)

        btnBangla.setOnClickListener {
            if (!speechInputLangBengali) {
                // Stop any active mic before switching language
                stopAllMic()
                speechInputLangBengali = true
                updateLangToggleAppearance(btnBangla, btnEnglish, bengaliSelected = true)
                // Clear previous text
                view.findViewById<TextView>(R.id.speech_recognized_text)?.text = ""
                view.findViewById<TextView>(R.id.speech_status_text)?.text = "Bangla selected. Tap mic to speak."
            }
        }
        btnEnglish.setOnClickListener {
            if (speechInputLangBengali) {
                // Stop any active mic before switching language
                stopAllMic()
                speechInputLangBengali = false
                updateLangToggleAppearance(btnBangla, btnEnglish, bengaliSelected = false)
                // Clear previous text
                view.findViewById<TextView>(R.id.speech_recognized_text)?.text = ""
                view.findViewById<TextView>(R.id.speech_status_text)?.text = "English selected. Tap mic to speak."
            }
        }
    }

    /** Stop all active microphone recording (Bengali + English + System recognizer). */
    private fun stopAllMic() {
        // English Vosk
        if (isEnglishMicActive) {
            stopEnglishVoskRecording()
        }
        isEnglishMicActive = false

        // Bengali AudioRecord
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null

        // Google System SpeechRecognizer
        speechInputEnglishListening = false
        practiceListening = false
        try { speechRecognizer?.stopListening() } catch (_: Exception) {}
        try { speechRecognizer?.cancel() } catch (_: Exception) {}

        // Interactive Table SpeechRecognizer
        stopTableInteractiveMode()

        isRecording = false
        setMicButtonAppearance(recording = false)
        updateSpeechInputStatus(false)
    }

    /** Update the visual state of the two language toggle buttons. */
    private fun updateLangToggleAppearance(btnBangla: TextView, btnEnglish: TextView, bengaliSelected: Boolean) {
        if (bengaliSelected) {
            btnBangla.setBackgroundColor(0xFF1565C0.toInt())   // blue = selected
            btnBangla.setTextColor(0xFFFFFFFF.toInt())
            btnBangla.setTypeface(null, android.graphics.Typeface.BOLD)
            btnEnglish.setBackgroundColor(0xFFE0E0E0.toInt())  // grey = unselected
            btnEnglish.setTextColor(0xFF555555.toInt())
            btnEnglish.setTypeface(null, android.graphics.Typeface.NORMAL)
        } else {
            btnEnglish.setBackgroundColor(0xFF1565C0.toInt())
            btnEnglish.setTextColor(0xFFFFFFFF.toInt())
            btnEnglish.setTypeface(null, android.graphics.Typeface.BOLD)
            btnBangla.setBackgroundColor(0xFFE0E0E0.toInt())
            btnBangla.setTextColor(0xFF555555.toInt())
            btnBangla.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    /** Show practice 3-area layout. Returns the view for further customization. */
    fun showPracticeThreeAreaLayout(): View {
        return switchContentLayout(ContentLayout.PRACTICE_THREE_AREA)
    }

    /** Switch back to the legacy (original) layout. */
    fun showLegacyLayout() {
        switchContentLayout(ContentLayout.LEGACY)
    }

    /**
     * Generic loader for sound pronunciation files (e.g. T_sound.txt, N_sound.txt).
     * Reads CSV from assets/Lessons/, displays as 2-column table with interactive mode.
     *
     * @param englishMatchMode  If true, always listen in English and compare against
     *                          the English word in column 0 (ignoring Bengali column).
     *                          If false, compare against Bengali (column 1), with
     *                          automatic fallback to English when Bengali is empty.
     */
    private fun loadSoundFile(filename: String, title: String, englishMatchMode: Boolean = false) {
        stopAllMic(); textToSpeech?.stop()
        lessonName = title
        updateLessonTopicDisplay()
        try {
            val csv = assets.open("Lessons/$filename")
                .bufferedReader(StandardCharsets.UTF_8).readText().trim()
            val lines = csv.lines()
            val headerLine = lines.firstOrNull() ?: "English Word,Pronunciation in Bengali"
            val headerCols = headerLine.split(",").map { it.trim() }
            val dataText = lines.drop(1).joinToString("\n")
            showTableDisplayLayout(
                title = title,
                columnCount = 2,
                headers = headerCols.take(2),
                csvText = dataText,
                tappableColumn = -1,
                interactive = true,
                speakCol = 0,                                                   // speak English word
                matchCol = if (englishMatchMode) 0 else 1,                      // compare against col 0 (English) or col 1 (Bengali)
                interactiveLocale = if (englishMatchMode) Locale.US else Locale("bn"),
                speakLocale = Locale.US                                          // always speak in English
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Could not load $filename", Toast.LENGTH_SHORT).show()
        }
    }

    // ───────────────────── Table Display Layout Logic ─────────────────────

    /**
     * Show a reusable N-column table in the TABLE_DISPLAY layout.
     *
     * @param title       Optional title shown above the table. Pass null or "" to hide.
     * @param columnCount Number of columns (2, 3, 4, 6, etc.)
     * @param headers     Column header labels (e.g. listOf("Word", "Pronunciation", "Meaning")).
     *                    If fewer than columnCount, remaining headers will be empty.
     * @param csvText     Rows as CSV-style text. Each line is one row, columns separated by comma.
     *                    Lines starting with "-" or blank lines are ignored.
     *                    Example: "book,বই,বই\nwater,পানি,পানি"
     * @param tappableColumn  Which column index (0-based) should be tappable to speak via TTS.
     *                        Pass -1 for none. Default -1.
     * @param tappableLocale  The locale used for TTS when a tappable cell is tapped.
     *                        Default Locale.US (English). Use Locale("bn") for Bengali.
     * @param interactive     If true, enable interactive drill mode: auto-speak each row,
     *                        wait for user pronunciation, check, advance.
     * @param speakCol        Column index whose text is spoken in interactive mode (0-based). Default 1.
     * @param matchCol        Column index to match user speech against in interactive mode (0-based). Default 1.
     * @param interactiveLocale Locale for speech recognition (listening) in interactive mode.
     * @param speakLocale      Locale for TTS (speaking) in interactive mode. Defaults to Locale.US.
     */
    fun showTableDisplayLayout(
        title: String?,
        columnCount: Int,
        headers: List<String>,
        csvText: String,
        tappableColumn: Int = -1,
        tappableLocale: Locale = Locale.US,
        interactive: Boolean = false,
        speakCol: Int = 1,
        matchCol: Int = 1,
        interactiveLocale: Locale = Locale("bn"),
        speakLocale: Locale = Locale.US
    ) {
        // Stop any previous interactive session
        stopTableInteractiveMode()

        val view = switchContentLayout(ContentLayout.TABLE_DISPLAY)
        val titleView = view.findViewById<TextView>(R.id.table_display_title)
        val webView = view.findViewById<WebView>(R.id.table_display_webview)

        // Title
        if (title.isNullOrBlank()) {
            titleView.visibility = View.GONE
        } else {
            titleView.visibility = View.VISIBLE
            titleView.text = title
        }

        // Parse CSV text into rows
        val rows = mutableListOf<List<String>>()
        for (line in csvText.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("-")) continue
            val cols = trimmed.split(",").map { it.trim() }
            if (cols.any { it.isNotEmpty() }) {
                rows.add(cols)
            }
        }

        // Build HTML table
        val html = buildTableHtml(columnCount, headers, rows, tappableColumn, interactive)
        webView.settings.javaScriptEnabled = interactive  // JS needed for row highlighting
        webView.setBackgroundColor(0)
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)

        // Handle tappable cell clicks
        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Using deprecated for broad compatibility")
            override fun shouldOverrideUrlLoading(v: WebView?, url: String?): Boolean {
                if (url != null && url.startsWith("cell://")) {
                    val encoded = url.removePrefix("cell://")
                    val word = try {
                        URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
                    } catch (_: Exception) { encoded }
                    if (word.isNotEmpty()) {
                        textToSpeech?.setLanguage(tappableLocale)
                        textToSpeech?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "table_cell_tap")
                        Toast.makeText(this@MainActivity, word, Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                return false
            }

            override fun onPageFinished(v: WebView?, url: String?) {
                super.onPageFinished(v, url)
                // Start interactive drill after page is fully rendered — only once
                if (interactive && tableInteractiveActive && !tablePageFinishedFired) {
                    tablePageFinishedFired = true
                    v?.postDelayed({ speakTableRow() }, 600)
                }
            }
        }

        // Set up interactive mode if requested
        if (interactive) {
            tableInteractiveWebView = webView
            tableInteractiveRows = rows
            tableInteractiveIndex = 0
            tableInteractiveSpeakCol = speakCol
            tableInteractiveMatchCol = matchCol
            tableInteractiveLocale = interactiveLocale
            tableInteractiveSpeakLocale = speakLocale
            tableInteractiveActive = true
            tableInteractiveSpeaking = false
            tablePageFinishedFired = false
            tableInteractiveRetryCount = 0

            // Show control buttons
            val controls = view.findViewById<LinearLayout>(R.id.table_interactive_controls)
            controls?.visibility = View.VISIBLE

            val btnNext = view.findViewById<TextView>(R.id.table_btn_next)
            val btnRestart = view.findViewById<TextView>(R.id.table_btn_restart)

            btnNext?.setOnClickListener {
                if (!tableInteractiveActive) return@setOnClickListener
                // Stop any active TTS / mic, skip to next row
                textToSpeech?.stop()
                try { tableSpeechRecognizer?.cancel() } catch (_: Exception) {}
                tableInteractiveListening = false
                tableInteractiveSpeaking = false
                // Mark current row as skipped (orange + ▶ icon)
                highlightTableRow(tableInteractiveIndex, "'#ffe0b2'")
                setTableRowIcon(tableInteractiveIndex, "▶", "#E65100")
                tableInteractiveTestedCount++
                tableInteractiveRetryCount = 0
                tableInteractiveNoMatchCount = 0
                tableInteractiveIndex++
                webView.postDelayed({ speakTableRow() }, 300)
            }

            btnRestart?.setOnClickListener {
                // Stop everything and restart from row 0
                textToSpeech?.stop()
                try { tableSpeechRecognizer?.cancel() } catch (_: Exception) {}
                tableInteractiveListening = false
                tableInteractiveSpeaking = false
                tableInteractiveRetryCount = 0
                tableInteractiveNoMatchCount = 0
                tableInteractiveCorrectCount = 0
                tableInteractiveTestedCount = 0
                tableInteractiveIndex = 0
                tableInteractiveActive = true
                // Reset all row colors
                webView.evaluateJavascript("resetAllRows();", null)
                webView.postDelayed({ speakTableRow() }, 400)
            }
        } else {
            // Hide control buttons for non-interactive tables
            val controls = view.findViewById<LinearLayout>(R.id.table_interactive_controls)
            controls?.visibility = View.GONE
        }
    }

    /** Speak the current row's text, highlight it, and wait for user input afterwards. */
    private fun speakTableRow() {
        if (!tableInteractiveActive) return
        // Guard: prevent double-speaking if called again while TTS is still active
        if (tableInteractiveSpeaking) return
        tableInteractiveSpeaking = true

        val idx = tableInteractiveIndex
        if (idx >= tableInteractiveRows.size) {
            // All rows done
            tableInteractiveActive = false
            tableInteractiveSpeaking = false
            val titleView = tableInteractiveWebView?.rootView?.findViewById<TextView>(R.id.table_display_title)
            titleView?.text = "Done!  ${tableInteractiveCorrectCount}/${tableInteractiveTestedCount} correct"
            Toast.makeText(this, "Done! ${tableInteractiveCorrectCount}/${tableInteractiveTestedCount} correct", Toast.LENGTH_LONG).show()
            return
        }
        val row = tableInteractiveRows[idx]
        val textToSpeak = row.getOrNull(tableInteractiveSpeakCol) ?: ""
        val letterLabel = row.getOrNull(0) ?: ""

        // Highlight current row in gold/yellow
        highlightTableRow(idx, "'#fff9c4'")
        scrollTableToRow(idx)

        // Update title
        val titleView = tableInteractiveWebView?.rootView?.findViewById<TextView>(R.id.table_display_title)
        titleView?.text = buildTableTitle(letterLabel)

        if (textToSpeak.isEmpty()) {
            // Skip empty cells
            tableInteractiveSpeaking = false
            tableInteractiveIndex++
            tableInteractiveWebView?.postDelayed({ speakTableRow() }, 300)
            return
        }

        // Stop any previous TTS first
        textToSpeech?.stop()

        // Speak the column text using the speak locale (e.g. English for letter names)
        textToSpeech?.setLanguage(tableInteractiveSpeakLocale)
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId == "table_interactive_speak") {
                    runOnUiThread {
                        tableInteractiveSpeaking = false
                        // After speaking, start listening for user's pronunciation
                        if (tableInteractiveActive) {
                            tableInteractiveWebView?.postDelayed({ startTableListening() }, 400)
                        }
                    }
                }
            }
            @Deprecated("Deprecated") override fun onError(utteranceId: String?) {}
        })
        textToSpeech?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "table_interactive_speak")
    }

    /** Highlight a specific row in the WebView table. colorStr must be a JS string like '#fff9c4'. */
    private fun highlightTableRow(rowIdx: Int, colorStr: String) {
        tableInteractiveWebView?.evaluateJavascript("highlightRow($rowIdx, $colorStr);", null)
    }

    /** Scroll the WebView so the given row is visible. */
    private fun scrollTableToRow(rowIdx: Int) {
        tableInteractiveWebView?.evaluateJavascript("scrollToRow($rowIdx);", null)
    }

    /**
     * Check if the current row's match column is empty → use English recognition instead.
     */
    private fun isCurrentRowEnglishMode(): Boolean {
        val row = tableInteractiveRows.getOrNull(tableInteractiveIndex) ?: return false
        val matchText = row.getOrNull(tableInteractiveMatchCol)?.trim() ?: ""
        return matchText.isEmpty()
    }

    /** Start listening for user's pronunciation using Google SpeechRecognizer.
     *  Auto-detects language: if Bengali column is empty, listens in English. */
    private fun startTableListening() {
        if (!tableInteractiveActive) return
        tableInteractiveListening = true

        // Change row highlight to light blue = "your turn"
        highlightTableRow(tableInteractiveIndex, "'#bbdefb'")

        try {
            tableSpeechRecognizer?.cancel()
            tableSpeechRecognizer?.destroy()
        } catch (_: Exception) {}

        tableSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        // If match column is empty for this row, listen in English
        val useEnglish = isCurrentRowEnglishMode()
        val langTag = if (useEnglish) "en-US" else {
            if (tableInteractiveLocale.language == "bn") "bn-BD" else tableInteractiveLocale.toLanguageTag()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf<String>())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
        }

        tableListeningStartTime = System.currentTimeMillis()
        tableSpeechRecognizer?.setRecognitionListener(createTableRecognitionListener())
        tableSpeechRecognizer?.startListening(intent)
    }

    /** Create a RecognitionListener for the interactive table drill. */
    private fun createTableRecognitionListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            val idx = tableInteractiveIndex
            val letterLabel = tableInteractiveRows.getOrNull(idx)?.getOrNull(0) ?: ""
            val titleView = tableInteractiveWebView?.rootView?.findViewById<TextView>(R.id.table_display_title)
            titleView?.text = buildTableTitle(letterLabel, mic = true)
            playStartBeep()
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { tableInteractiveListening = false }
        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
            val titleView = tableInteractiveWebView?.rootView?.findViewById<TextView>(R.id.table_display_title)
            val idx = tableInteractiveIndex
            val letterLabel = tableInteractiveRows.getOrNull(idx)?.getOrNull(0) ?: ""
            val expected = tableInteractiveRows.getOrNull(idx)?.getOrNull(tableInteractiveMatchCol)?.split("|")?.firstOrNull()?.trim() ?: ""
            titleView?.text = buildTableTitle(letterLabel, mic = true, result = false, heard = partial, expected = expected)
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onError(error: Int) {
            tableInteractiveListening = false
            if (!tableInteractiveActive) return

            val elapsed = System.currentTimeMillis() - tableListeningStartTime
            // If error fires within 2 seconds, the user hasn't had a chance to speak yet.
            // Silently restart the recognizer instead of counting it wrong.
            if (elapsed < 2000) {
                tableInteractiveWebView?.postDelayed({ startTableListening() }, 300)
                return
            }

            // After 2+ seconds → count as wrong answer (no speech detected)
            checkTableAnswer("", emptyList())
        }
        override fun onResults(results: Bundle?) {
            tableInteractiveListening = false
            if (!tableInteractiveActive) return
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
            val spoken = matches.firstOrNull()?.trim() ?: ""
            if (spoken.isNotEmpty()) {
                checkTableAnswer(spoken, matches)
            } else {
                checkTableAnswer("", emptyList())
            }
        }
    }

    /**
     * Compare user's spoken text against the expected column text (lenient for Bengali).
     * Supports alternate accepted pronunciations via "|" separator in the data.
     * e.g. "এই|এ" means either "এই" or "এ" is accepted.
     */
    private fun checkTableAnswer(spoken: String, allMatches: List<String> = listOf(spoken)) {
        val idx = tableInteractiveIndex
        val row = tableInteractiveRows.getOrNull(idx) ?: return
        val matchColText = row.getOrNull(tableInteractiveMatchCol)?.trim() ?: ""
        val letterLabel = row.getOrNull(0) ?: ""
        // If Bengali pronunciation is empty, compare against English word (column 0) instead
        val expected = if (matchColText.isEmpty()) letterLabel else matchColText

        // Detect whether this row is English-mode comparison
        val englishMode = isCurrentRowEnglishMode() ||
            (tableInteractiveMatchCol == 0)  // matchCol explicitly set to col 0

        // Normalize for Bengali: strip diacritics/punctuation
        fun normalizeBn(s: String) = s.trim()
            .replace("\\s+".toRegex(), "")
            .replace("।", "").replace(".", "").replace(",", "")
            .replace("্", "")     // hasanta (virama)
            .replace("ং", "ঙ")    // common variant
            .replace("ঁ", "")      // chandrabindu
            .replace("়", "")      // nukta
            .replace("্", "")     // double check hasanta
            .replace("ঃ", "")     // visarga

        // Normalize for English: lowercase, strip punctuation
        fun normalizeEn(s: String) = s.trim().lowercase()
            .replace("[^a-z ]".toRegex(), "")
            .replace("\\s+".toRegex(), " ")

        // Split on "|", "অথবা", "/" to get all accepted alternatives
        val expectedAlternatives = expected.split("|", "অথবা", "/")
            .map { if (englishMode) normalizeEn(it) else normalizeBn(it) }
            .filter { it.isNotEmpty() }

        // Check if ANY recognition alternative matches ANY expected alternative
        val isCorrect = allMatches.any { match ->
            val normSpoken = if (englishMode) normalizeEn(match) else normalizeBn(match)
            if (normSpoken.isEmpty()) return@any false
            expectedAlternatives.any { expAlt ->
                if (englishMode) {
                    // English: case-insensitive exact or contains
                    normSpoken == expAlt ||
                        normSpoken.contains(expAlt) ||
                        expAlt.contains(normSpoken)
                } else {
                    // Bengali: lenient matching
                    normSpoken == expAlt ||
                        normSpoken.contains(expAlt) ||
                        expAlt.contains(normSpoken) ||
                        (normSpoken.length >= 2 && expAlt.length >= 2 &&
                            normSpoken.take(2) == expAlt.take(2)) ||
                        (expAlt.length == 1 && normSpoken.startsWith(expAlt))
                }
            }
        }

        val titleView = tableInteractiveWebView?.rootView?.findViewById<TextView>(R.id.table_display_title)

        // Get the first expected alternative (without | separators) for display
        val expectedDisplay = expected.split("|").firstOrNull()?.trim() ?: expected

        if (isCorrect) {
            // Correct — green highlight + persistent ✓ icon in row
            highlightTableRow(idx, "'#c8e6c9'")
            setTableRowIcon(idx, "✓", "#4CAF50")
            tableInteractiveCorrectCount++
            tableInteractiveTestedCount++
            titleView?.text = buildTableTitle(letterLabel, result = true, heard = spoken)
            tableInteractiveRetryCount = 0
            tableInteractiveNoMatchCount = 0
            tableInteractiveIndex++
            tableInteractiveWebView?.postDelayed({ speakTableRow() }, 1200)
        } else {
            tableInteractiveRetryCount++
            if (tableInteractiveRetryCount >= tableInteractiveMaxRetries) {
                // Max retries — persistent ✗ icon, orange highlight, move on
                highlightTableRow(idx, "'#ffe0b2'")
                setTableRowIcon(idx, "✗", "#E65100")
                tableInteractiveTestedCount++
                titleView?.text = buildTableTitle(letterLabel, result = false, heard = spoken, expected = expectedDisplay, movingOn = true)
                tableInteractiveRetryCount = 0
                tableInteractiveNoMatchCount = 0
                tableInteractiveIndex++
                tableInteractiveWebView?.postDelayed({ speakTableRow() }, 2200)
            } else {
                // Wrong — red highlight, then re-speak and retry
                highlightTableRow(idx, "'#ffcdd2'")
                titleView?.text = buildTableTitle(letterLabel, result = false, heard = spoken, expected = expectedDisplay)
                tableInteractiveWebView?.postDelayed({
                    if (tableInteractiveActive) {
                        highlightTableRow(idx, "'#fff9c4'")
                        speakTableRow()
                    }
                }, 2000)
            }
        }
    }

    /** Show a brief ✓ or ✗ overlay on the WebView that fades out. */
    private fun showTableOverlay(symbol: String, colorStr: String) {
        val js = "showOverlay('$symbol', $colorStr);"
        tableInteractiveWebView?.evaluateJavascript(js, null)
    }

    /** Set a persistent icon (✓ or ✗) in the icon column of the given row. */
    private fun setTableRowIcon(rowIdx: Int, symbol: String, color: String) {
        tableInteractiveWebView?.evaluateJavascript("setRowIcon($rowIdx, '$symbol', '$color');", null)
    }

    /**
     * Build the formatted interactive title string.
     * Format: LETTER - 🎤  x/y - A/3  "expected" ≠ "actual"
     * @param letter    Current letter label (e.g. "A")
     * @param mic       True to show mic icon (listening state)
     * @param result    null = no result yet, true = correct, false = wrong
     * @param heard     What the recognizer heard (empty if nothing)
     * @param expected  Expected pronunciation (for display when wrong)
     * @param movingOn  True if max retries reached
     */
    private fun buildTableTitle(
        letter: String,
        mic: Boolean = false,
        result: Boolean? = null,
        heard: String = "",
        expected: String = "",
        movingOn: Boolean = false
    ): String {
        val sb = StringBuilder()
        sb.append(letter)
        if (mic) sb.append(" 🎤")
        // Score: correct/tested
        sb.append("  ${tableInteractiveCorrectCount}/${tableInteractiveTestedCount}")
        // Attempt: which chance out of max
        if (tableInteractiveRetryCount > 0 || result == false) {
            sb.append(" - ${tableInteractiveRetryCount}/$tableInteractiveMaxRetries")
        }
        // Result info
        when (result) {
            true -> sb.append("  ✓ \"$heard\"")
            false -> {
                if (heard.isNotEmpty()) {
                    sb.append("  \"$expected\" ≠ \"$heard\"")
                } else {
                    sb.append("  \"$expected\" ≠ \"\"")
                }
                if (movingOn) sb.append("  →")
            }
            null -> { /* speaking or listening, no result yet */ }
        }
        return sb.toString()
    }

    /** Play a short distinct beep to signal "start speaking now". */
    private fun playStartBeep() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            android.os.Handler(mainLooper).postDelayed({ toneGen.release() }, 300)
        } catch (_: Exception) {}
    }

    /** Stop the interactive table drill cleanly. */
    private fun stopTableInteractiveMode() {
        tableInteractiveActive = false
        tableInteractiveListening = false
        tableInteractiveSpeaking = false
        tablePageFinishedFired = false
        tableInteractiveIndex = 0
        tableInteractiveRetryCount = 0
        tableInteractiveNoMatchCount = 0
        tableInteractiveCorrectCount = 0
        tableInteractiveTestedCount = 0
        tableInteractiveRows = emptyList()
        try {
            tableSpeechRecognizer?.cancel()
            tableSpeechRecognizer?.destroy()
        } catch (_: Exception) {}
        tableSpeechRecognizer = null
        tableInteractiveWebView = null
    }

    /**
     * Build an HTML table string for N columns.
     * @param tappableCol column index whose cells become tap-to-speak links. -1 = none.
     * @param interactive if true, include row IDs and JavaScript highlight/scroll functions.
     */
    private fun buildTableHtml(
        columnCount: Int,
        headers: List<String>,
        rows: List<List<String>>,
        tappableCol: Int,
        interactive: Boolean = false
    ): String {
        fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

        // In interactive mode, add a narrow icon column at the end for ✓/✗
        val iconColW = if (interactive) 36 else 0  // fixed pixel width for icon col
        val headerCells = (0 until columnCount).joinToString("") { i ->
            val text = headers.getOrNull(i) ?: ""
            "<th style=\"background:#3949ab;color:#fff;padding:8px 6px;border:1px solid #555;text-align:center;font-weight:bold;\">${esc(text)}</th>"
        } + if (interactive) "<th style=\"background:#3949ab;color:#fff;padding:4px;border:1px solid #555;width:${iconColW}px;\"></th>" else ""
        val evenBg = "#ffffff"
        val oddBg = "#f5f8ff"
        val bodyRows = rows.mapIndexed { rowIdx, row ->
            val bg = if (rowIdx % 2 == 0) evenBg else oddBg
            val rowId = if (interactive) " id=\"row_$rowIdx\"" else ""
            val cells = (0 until columnCount).joinToString("") { c ->
                val text = row.getOrNull(c) ?: ""
                val escaped = esc(text)
                val content = if (c == tappableCol && text.isNotEmpty()) {
                    val href = "cell://" + URLEncoder.encode(text, StandardCharsets.UTF_8.name())
                    "<a href=\"$href\" style=\"color:#0066cc;text-decoration:underline;font-weight:bold;\">$escaped</a>"
                } else {
                    escaped
                }
                "<td style=\"border:1px solid #ccc;padding:8px 6px;text-align:center;font-size:${if (interactive) "16px" else "inherit"};\">${content}</td>"
            }
            val iconCell = if (interactive) "<td id=\"icon_$rowIdx\" style=\"border:1px solid #ccc;padding:2px;text-align:center;width:${iconColW}px;font-size:20px;\"></td>" else ""
            "<tr${rowId} style=\"background:$bg;transition:background 0.3s;\">$cells$iconCell</tr>"
        }.joinToString("")

        val jsBlock = if (interactive) """
<script>
var completedRows = {};
function highlightRow(idx, color) {
    var rows = document.querySelectorAll('tbody tr');
    for (var i = 0; i < rows.length; i++) {
        if (i === idx) {
            rows[i].style.background = color;
            rows[i].style.fontWeight = 'bold';
            rows[i].style.fontSize = '17px';
            if (color === '#c8e6c9') completedRows[i] = '#c8e6c9';
            else if (color === '#ffe0b2') completedRows[i] = '#ffe0b2';
        } else if (completedRows[i]) {
            rows[i].style.background = completedRows[i];
            rows[i].style.fontWeight = 'normal';
            rows[i].style.fontSize = 'inherit';
        } else {
            rows[i].style.background = (i % 2 === 0) ? '#ffffff' : '#f5f8ff';
            rows[i].style.fontWeight = 'normal';
            rows[i].style.fontSize = 'inherit';
        }
    }
}
function scrollToRow(idx) {
    var row = document.getElementById('row_' + idx);
    if (row) row.scrollIntoView({behavior:'smooth', block:'center'});
}
function resetAllRows() {
    completedRows = {};
    var rows = document.querySelectorAll('tbody tr');
    for (var i = 0; i < rows.length; i++) {
        rows[i].style.background = (i % 2 === 0) ? '#ffffff' : '#f5f8ff';
        rows[i].style.fontWeight = 'normal';
        rows[i].style.fontSize = 'inherit';
        var ic = document.getElementById('icon_' + i);
        if (ic) ic.innerHTML = '';
    }
}
function setRowIcon(idx, symbol, color) {
    var ic = document.getElementById('icon_' + idx);
    if (ic) {
        ic.innerHTML = '<span style="color:' + color + ';font-size:22px;font-weight:bold;">' + symbol + '</span>';
    }
}
function showOverlay(symbol, color) {
    var old = document.getElementById('result_overlay');
    if (old) old.remove();
    var d = document.createElement('div');
    d.id = 'result_overlay';
    d.textContent = symbol;
    d.style.cssText = 'position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);' +
        'font-size:120px;font-weight:bold;color:' + color + ';' +
        'background:rgba(255,255,255,0.85);border-radius:50%;width:160px;height:160px;' +
        'display:flex;align-items:center;justify-content:center;' +
        'box-shadow:0 4px 24px rgba(0,0,0,0.2);z-index:9999;' +
        'animation:fadeOut 1.2s ease-out forwards;';
    document.body.appendChild(d);
    setTimeout(function(){ if(d.parentNode) d.remove(); }, 1300);
}
</script>
<style>
@keyframes fadeOut {
    0%   { opacity:1; transform:translate(-50%,-50%) scale(1); }
    60%  { opacity:1; transform:translate(-50%,-50%) scale(1.1); }
    100% { opacity:0; transform:translate(-50%,-50%) scale(0.8); }
}
</style>""" else ""

        return """
<!DOCTYPE html>
<html><head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body { font-family:sans-serif; font-size:clamp(13px,3.2vw,17px); padding:6px; background:#fff; }
  table { width:100%; border-collapse:collapse; border:2px solid #333; }
  a { -webkit-tap-highlight-color:rgba(0,0,0,0.1); }
  tr { transition: background 0.3s ease; }
</style>
$jsBlock
</head><body>
<table>
<thead><tr>$headerCells</tr></thead>
<tbody>$bodyRows</tbody>
</table>
</body></html>"""
    }

    // ───────────────────── End Table Display Layout ─────────────────────

    // ───────────────────── Practice 3-Area Layout Logic ─────────────────────

    /** Show the current practice word in the 3-area layout. */
    private fun showPracticeWord() {
        val view = practiceView ?: return
        val bengaliTv = view.findViewById<TextView>(R.id.practice_bengali_text) ?: return
        val englishTv = view.findViewById<TextView>(R.id.practice_english_text) ?: return
        val userTv = view.findViewById<TextView>(R.id.practice_user_text) ?: return
        val badge = view.findViewById<TextView>(R.id.practice_result_badge) ?: return

        if (practiceWordIndex >= practiceWordList.size) {
            bengaliTv.text = "🎉  Practice complete!"
            englishTv.text = ""
            userTv.text = ""
            badge.visibility = View.GONE
            Toast.makeText(this, "All words done!", Toast.LENGTH_SHORT).show()
            return
        }
        val (bengali, _) = practiceWordList[practiceWordIndex]
        bengaliTv.text = bengali
        englishTv.text = "?"   // hidden until user answers
        userTv.text = ""
        badge.visibility = View.GONE

        // Update topic bar with progress
        lessonStatTextView.text = "${practiceWordIndex + 1}/${practiceWordList.size}"
        lessonStatTextView.visibility = View.VISIBLE
    }

    /** Handle mic tap in practice layout: listen for English answer. */
    private fun startPracticeListening() {
        if (!checkForPermission(RECORD_AUDIO)) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, arrayOf(RECORD_AUDIO), PERMISSION_REQUEST_CODE)
            return
        }
        practiceListening = true
        isRecording = true
        setMicButtonAppearance(recording = true)

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }
        speechRecognizer?.setRecognitionListener(createPracticeRecognitionListener())
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    /** RecognitionListener for the practice 3-area layout. */
    private fun createPracticeRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            Log.w(TAG, "Practice recognition error: $error")
            runOnUiThread {
                practiceListening = false
                isRecording = false
                setMicButtonAppearance(recording = false)
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Toast.makeText(this@MainActivity, "Didn't hear anything. Tap mic to try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        override fun onResults(results: android.os.Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim() ?: ""
            runOnUiThread {
                practiceListening = false
                isRecording = false
                setMicButtonAppearance(recording = false)
                if (text.isNotBlank()) {
                    checkPracticeAnswer(text)
                }
            }
        }
        override fun onPartialResults(partialResults: android.os.Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim() ?: ""
            if (text.isNotBlank()) {
                runOnUiThread {
                    practiceView?.findViewById<TextView>(R.id.practice_user_text)?.text = text
                }
            }
        }
        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    }

    /** Compare user's spoken answer with the expected English word. */
    private fun checkPracticeAnswer(spokenText: String) {
        val view = practiceView ?: return
        if (practiceWordIndex >= practiceWordList.size) return
        val (_, expectedEnglish) = practiceWordList[practiceWordIndex]
        val userTv = view.findViewById<TextView>(R.id.practice_user_text) ?: return
        val englishTv = view.findViewById<TextView>(R.id.practice_english_text) ?: return
        val badge = view.findViewById<TextView>(R.id.practice_result_badge) ?: return

        userTv.text = MatchNormalizer.sanitizeSpokenTextForDisplay(spokenText)
        // Reveal the correct answer (display form: first alternative only)
        englishTv.text = MatchNormalizer.textForSpeakAndDisplay(expectedEnglish)

        val match = MatchNormalizer.matchesExpectedWithAlternates(expectedEnglish, spokenText)
        badge.visibility = View.VISIBLE
        if (match) {
            badge.text = " Correct ✓ "
            badge.setTextColor(0xFFFFFFFF.toInt())
            badge.setBackgroundColor(0xFF4CAF50.toInt())
            // Auto-advance after a short delay
            view.postDelayed({
                practiceWordIndex++
                showPracticeWord()
            }, 1500)
        } else {
            badge.text = " Try again ✗ "
            badge.setTextColor(0xFFFFFFFF.toInt())
            badge.setBackgroundColor(0xFFE53935.toInt())
            Toast.makeText(this, "Expected: $expectedEnglish", Toast.LENGTH_SHORT).show()
        }
    }

    // ───────────────────── End Practice 3-Area Layout ─────────────────────

    // ───────────────────── End Content Layout Switching ─────────────────────

    // ───────────────────── End Navigation Drawer ─────────────────────

    private fun showLoadListDialog() {
        val jsonFiles = filesDir.listFiles()?.filter { it.isFile && it.name.endsWith(StringUtils.LIST_FILE_SUFFIX) } ?: emptyList()
        val loadOptions = mutableListOf<String>()
        val loadActions = mutableListOf<() -> Unit>()
        loadOptions.add("Diagram: 1-to-3 (Grammar Rules)")
        loadActions.add {
            loadDiagramFromAssets("diagram-1to3.html")
            setDescriptionInstruction(null, null)
            Toast.makeText(this, "Showing 1-to-3 diagram. Scroll down to Description.", Toast.LENGTH_SHORT).show()
        }
        loadOptions.add("Diagram: 3-to-1 (Have/Has)")
        loadActions.add {
            loadDiagramFromAssets("diagram-3to1.html")
            setDescriptionInstruction(null, null)
            Toast.makeText(this, "Showing 3-to-1 diagram. Scroll down to Description.", Toast.LENGTH_SHORT).show()
        }
        loadOptions.add("Alphabet pronunciation (A–Z)")
        loadActions.add {
            loadDiagramFromAssets("alphabet-pronunciation-table.html")
            setDescriptionInstruction(
                "এই শব্দ গুলোর শুদ্ধ উচ্চারণ না জানলে এই এপ ভালভাবে কাজ করবে না। সেইজন্য এই অক্ষর গুলোর উচ্চারণ জানা জরুরী",
                Locale("bn")
            )
            Toast.makeText(this, "Showing alphabet table. Scroll down to Description.", Toast.LENGTH_SHORT).show()
        }
        loadOptions.add(getString(R.string.load_introduction))
        loadActions.add {
            try {
                val content = assets.open("introduction.txt").bufferedReader(StandardCharsets.UTF_8).readText().trim()
                showIntroductionContent(content)
                if (content.isNotEmpty()) {
                    textToSpeech?.stop()
                    descriptionWebView.postDelayed({ speakIntroductionBengali(content) }, 500)
                } else {
                    Toast.makeText(this, "Introduction file is empty.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Load introduction from assets failed", e)
                Toast.makeText(this, "Could not load introduction.txt from app.", Toast.LENGTH_SHORT).show()
            }
        }
        loadOptions.add("Table: 3 columns (test)")
        loadActions.add {
            showTableInDescription(
                intro = "<strong>Subject, Object, Possessive</strong> — three forms for each pronoun.",
                headers = listOf("Subject Pronouns", "Object Pronouns", "Possessive"),
                rows = listOf(
                    listOf("I", "Me", "My"),
                    listOf("You", "You", "Your"),
                    listOf("He", "Him", "His"),
                    listOf("She", "Her", "Her"),
                    listOf("It", "It", "Its"),
                    listOf("We", "Us", "Our"),
                    listOf("You", "You", "Your"),
                    listOf("They", "Them", "Their")
                )
            )
            Toast.makeText(this, "Showing 3-column table. Scroll down to Description.", Toast.LENGTH_SHORT).show()
        }
        loadOptions.add("Pronunciation (word lists)")
        loadActions.add {
            val lessons = getPronunciationLessons()
            val titles = lessons.map { it.first }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.pronunciation_lesson_choose))
                .setItems(titles) { _, which ->
                    val (title, rows) = lessons[which]
                    showPronunciationLesson(title, rows)
                    Toast.makeText(this, "Loaded: $title. Tap play (lecture) to hear words.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        loadOptions.add("Table: 4 columns (test)")
        loadActions.add {
            showTableInDescription(
                intro = "<strong>Pronouns</strong> replace nouns. It is used when something or a person has been mentioned before.",
                headers = listOf("Subject Pronouns", "Object Pronouns", "Possessive Adjectives", "Possessive Pronouns"),
                rows = listOf(
                    listOf("I", "Me", "My", "Mine"),
                    listOf("You", "You", "Your", "Yours"),
                    listOf("He", "Him", "His", "His"),
                    listOf("She", "Her", "Her", "Hers"),
                    listOf("It", "It", "Its", "Its"),
                    listOf("We", "Us", "Our", "Ours"),
                    listOf("You", "You", "Your", "Yours"),
                    listOf("They", "Them", "Their", "Theirs")
                )
            )
            Toast.makeText(this, "Showing 4-column table. Scroll down to Description.", Toast.LENGTH_SHORT).show()
        }
        loadOptions.add(getString(R.string.load_practice_incorrect))
        loadActions.add {
            val incFiles = filesDir.listFiles()?.filter { it.isFile && it.name.endsWith(LessonFileParsers.INCORRECT_LESSON_SUFFIX) }?.sortedBy { it.name } ?: emptyList()
            if (incFiles.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_incorrect_saved), Toast.LENGTH_SHORT).show()
                return@add
            }
            val names = incFiles.map { it.name.removeSuffix(LessonFileParsers.INCORRECT_LESSON_SUFFIX) }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.load_practice_incorrect))
                .setItems(names) { _, which ->
                    val fileName = incFiles[which].name
                    val (displayName, rows) = loadIncorrectLessonListFromFile(fileName)
                    if (rows.isEmpty()) {
                        Toast.makeText(this, getString(R.string.no_incorrect_saved), Toast.LENGTH_SHORT).show()
                        return@setItems
                    }
                clearPronunciationLessonState()
                lessonRows = rows
                lessonName = displayName ?: names[which]
                lessonCorrectCount = 0
                lessonMode = 4
                lessonIndex = 0
                lessonPhase = "q"
                lessonMode3Listening = false
                lessonMode3SpokeAnswer = false
                lessonIncorrectCount = 0
                nextButton?.isEnabled = true
                skipButton?.isEnabled = true
                clearBothTextAreas()
                setSentenceListVisibility(false)
                updateLessonStatistic()
                updateLessonTopicDisplay()
                showVerbDiagram(verbForLessonDiagram(lessonName))
                textView.text = getString(R.string.lesson_loaded)
                Toast.makeText(this, getString(R.string.lesson_loaded), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        loadOptions.add(getString(R.string.load_lesson))
        loadActions.add {
            openLessonLauncher.launch(arrayOf("text/plain", "application/octet-stream"))
        }
        loadOptions.add(getString(R.string.load_verb))
        loadActions.add { showVerbSelectorDialog() }
        loadOptions.add(getString(R.string.load_tenses))
        loadActions.add { showTenseVerbSelectorDialog() }
        loadOptions.add(getString(R.string.load_regular_verbs))
        loadActions.add {
            try {
                val content = assets.open("Lessons/Regular_verbs.txt").bufferedReader().readText()
                val rows = parseVerbLessonFile(content)
                if (rows.isEmpty()) {
                    Toast.makeText(this, "No valid rows in Regular_verbs.txt", Toast.LENGTH_SHORT).show()
                    return@add
                }
                clearPronunciationLessonState()
                lessonRows = rows
                lessonName = "regular_verbs"
                incorrectLessonRows.clear()
                incorrectLessonSourceName = null
                lessonCorrectCount = 0
                lessonMode = 4
                lessonIndex = 0
                lessonPhase = "q"
                lessonMode3Listening = false
                lessonMode3SpokeAnswer = false
                lessonIncorrectCount = 0
                nextButton?.isEnabled = true
                skipButton?.isEnabled = true
                clearBothTextAreas()
                setSentenceListVisibility(false)
                updateLessonStatistic()
                updateLessonTopicDisplay()
                showVerbDiagram(verbForLessonDiagram(lessonName))
                textView.text = getString(R.string.lesson_loaded)
                Toast.makeText(this, getString(R.string.lesson_loaded), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not load Regular_verbs.txt: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        loadOptions.add(getString(R.string.load_irregular_verbs))
        loadActions.add {
            try {
                val content = assets.open("Lessons/Irregular_verbs.txt").bufferedReader().readText()
                val rows = parseVerbLessonFile(content)
                if (rows.isEmpty()) {
                    Toast.makeText(this, "No valid rows in Irregular_verbs.txt", Toast.LENGTH_SHORT).show()
                    return@add
                }
                clearPronunciationLessonState()
                lessonRows = rows
                lessonName = "irregular_verbs"
                incorrectLessonRows.clear()
                incorrectLessonSourceName = null
                lessonCorrectCount = 0
                lessonMode = 4
                lessonIndex = 0
                lessonPhase = "q"
                lessonMode3Listening = false
                lessonMode3SpokeAnswer = false
                lessonIncorrectCount = 0
                nextButton?.isEnabled = true
                skipButton?.isEnabled = true
                clearBothTextAreas()
                setSentenceListVisibility(false)
                updateLessonStatistic()
                updateLessonTopicDisplay()
                showVerbDiagram(verbForLessonDiagram(lessonName))
                textView.text = getString(R.string.lesson_loaded)
                Toast.makeText(this, getString(R.string.lesson_loaded), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not load Irregular_verbs.txt: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        loadOptions.add(getString(R.string.load_lesson) + " (introduce)")
        loadActions.add {
            try {
                val content = assets.open("introduce.txt").bufferedReader().readText()
                val rows = parseLessonFile(content)
                if (rows.isEmpty()) {
                    Toast.makeText(this, "No valid rows in introduce.txt", Toast.LENGTH_SHORT).show()
                    return@add
                }
                showModeSelectorDialog(rows, "introduce")
            } catch (e: Exception) {
                Toast.makeText(this, "Could not load introduce.txt: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        loadOptions.add(getString(R.string.load_svo_sentences))
        loadActions.add {
            try {
                val content = assets.open("Lessons/svo_sentences_list.txt").bufferedReader().readText()
                sentenceList.clear()
                content.lines().forEach { line ->
                    val s = line.trim()
                    if (s.isNotEmpty() && !s.startsWith("#")) {
                        sentenceList.add(Sentence(s, isBengali = false))
                    }
                }
                sentenceAdapter.notifyDataSetChanged()
                currentNextIndex = 0
                svoSentenceStrikes = 0
                lessonRows = null
                lessonName = null
                lessonMode = 0
                lessonIndex = 0
                lessonPhase = "q"
                lessonStatTextView.visibility = View.GONE
                nextButton?.isEnabled = sentenceList.isNotEmpty()
                skipButton?.isEnabled = false
                clearBothTextAreas()
                findViewById<TextView>(R.id.sentence_list_label).text = getString(R.string.svo_list_label)
                setSentenceListVisibility(true)
                updateLessonTopicDisplay()
                Toast.makeText(this, getString(R.string.lesson_loaded) + " (${sentenceList.size} sentences)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not load svo_sentences_list.txt: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        loadOptions.add("SVO Eat (Bengali, English)")
        loadActions.add {
            try {
                val content = assets.open("Lessons/SVO_eat.txt").bufferedReader(StandardCharsets.UTF_8).readText()
                val (topic, rows) = parseSvoLessonFile(content)
                if (rows.isEmpty()) {
                    Toast.makeText(this, "No valid rows in SVO_eat.txt", Toast.LENGTH_SHORT).show()
                    return@add
                }
                clearPronunciationLessonState()
                lessonRows = rows
                lessonName = topic
                incorrectLessonRows.clear()
                incorrectLessonSourceName = null
                lessonCorrectCount = 0
                lessonMode = 4
                lessonIndex = 0
                lessonPhase = "q"
                lessonMode3Listening = false
                lessonMode3SpokeAnswer = false
                lessonIncorrectCount = 0
                nextButton?.isEnabled = true
                skipButton?.isEnabled = true
                clearBothTextAreas()
                setSentenceListVisibility(false)
                updateLessonStatistic()
                updateLessonTopicDisplay()
                showVerbDiagram("EAT")
                onNextLessonStep()
                Toast.makeText(this, getString(R.string.lesson_loaded) + " (${rows.size} pairs)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not load SVO_eat.txt: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        loadOptions.add("SVO Play (Bengali, English)")
        loadActions.add {
            try {
                val content = assets.open("Lessons/SVO_play.txt").bufferedReader(StandardCharsets.UTF_8).readText()
                val (topic, rows) = parseSvoLessonFile(content)
                if (rows.isEmpty()) {
                    Toast.makeText(this, "No valid rows in SVO_play.txt", Toast.LENGTH_SHORT).show()
                    return@add
                }
                clearPronunciationLessonState()
                lessonRows = rows
                lessonName = topic
                incorrectLessonRows.clear()
                incorrectLessonSourceName = null
                lessonCorrectCount = 0
                lessonMode = 4
                lessonIndex = 0
                lessonPhase = "q"
                lessonMode3Listening = false
                lessonMode3SpokeAnswer = false
                lessonIncorrectCount = 0
                nextButton?.isEnabled = true
                skipButton?.isEnabled = true
                clearBothTextAreas()
                setSentenceListVisibility(false)
                updateLessonStatistic()
                updateLessonTopicDisplay()
                showVerbDiagram("PLAY")
                onNextLessonStep()
                Toast.makeText(this, getString(R.string.lesson_loaded) + " (${rows.size} pairs)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not load SVO_play.txt: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        if (jsonFiles.isNotEmpty()) {
            loadOptions.add(getString(R.string.load_sentence_list))
            loadActions.add {
                val names = jsonFiles.map { it.name.removeSuffix(StringUtils.LIST_FILE_SUFFIX) }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.load_list_dialog_title))
                    .setItems(names) { _, which -> loadSentenceListFromFile(jsonFiles[which]) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.load_dialog_title))
            .setItems(loadOptions.toTypedArray()) { _, which ->
                if (which < loadActions.size) loadActions[which]()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Save incorrect lesson rows to file named {original lesson name}_inc.json (e.g. regular_verbs_inc.json). */
    private fun saveIncorrectLessonList() {
        val sourceName = incorrectLessonSourceName ?: return
        val safeName = sourceName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val fileName = safeName + LessonFileParsers.INCORRECT_LESSON_SUFFIX
        try {
            val file = File(filesDir, fileName)
            val arr = JSONArray()
            for (r in incorrectLessonRows) {
                val obj = JSONObject()
                obj.put("engQ", r.engQ)
                obj.put("bnQ", r.bnQ)
                obj.put("engA", r.engA)
                obj.put("bnA", r.bnA)
                arr.put(obj)
            }
            val root = JSONObject()
            root.put("lessonName", sourceName)
            root.put("rows", arr)
            file.writeText(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Save incorrect list failed", e)
        }
    }

    /** Load incorrect lesson rows from a specific _inc.json file; returns (list name for display, rows) or (null, emptyList()) if missing/empty. */
    private fun loadIncorrectLessonListFromFile(fileName: String): Pair<String?, List<LessonRow>> {
        return try {
            val file = File(filesDir, fileName)
            if (!file.exists()) return Pair(null, emptyList())
            val text = file.readText()
            val rows = mutableListOf<LessonRow>()
            val firstChar = text.trimStart().firstOrNull()
            if (firstChar == '{') {
                val root = JSONObject(text)
                val sourceName = if (root.has("lessonName")) root.getString("lessonName") else null
                val arr = root.getJSONArray("rows")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    rows.add(LessonRow(
                        obj.getString("engQ"),
                        obj.getString("bnQ"),
                        obj.getString("engA"),
                        obj.getString("bnA")
                    ))
                }
                val displayName = sourceName?.let { it + "_inc" } ?: fileName.removeSuffix(LessonFileParsers.INCORRECT_LESSON_SUFFIX)
                return Pair(displayName, rows)
            }
            val arr = JSONArray(text)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                rows.add(LessonRow(
                    obj.getString("engQ"),
                    obj.getString("bnQ"),
                    obj.getString("engA"),
                    obj.getString("bnA")
                ))
            }
            val displayName = fileName.removeSuffix(LessonFileParsers.INCORRECT_LESSON_SUFFIX)
            Pair(displayName, rows)
        } catch (e: Exception) {
            Log.e(TAG, "Load incorrect list failed", e)
            Pair(null, emptyList())
        }
    }

    private fun loadSentenceListFromFile(file: File) {
        try {
            lessonRows = null
            lessonName = null
            lessonMode = 0
            updateLessonStatistic()
            updateLessonTopicDisplay()
            val arr = JSONArray(file.readText())
            sentenceList.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                sentenceList.add(Sentence(obj.getString("text"), obj.getBoolean("isBengali")))
            }
            sentenceAdapter.notifyDataSetChanged()
            currentNextIndex = 0
            nextButton?.isEnabled = sentenceList.isNotEmpty()
            skipButton?.isEnabled = lessonRows != null
            Toast.makeText(this, getString(R.string.list_loaded), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Load list failed", e)
            Toast.makeText(this, "Load failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** Expand common contractions so "I'm" and "I am" match. Call after lowercasing. */
    private fun normalizeContractions(s: String): String {
        var t = s
        val contractions = mapOf(
            "i'm" to "i am", "i've" to "i have", "i'd" to "i would", "i'll" to "i will",
            "that's" to "that is", "it's" to "it is", "what's" to "what is", "there's" to "there is",
            "here's" to "here is", "who's" to "who is",
            "don't" to "do not", "doesn't" to "does not", "didn't" to "did not",
            "isn't" to "is not", "aren't" to "are not", "wasn't" to "was not", "weren't" to "were not",
            "haven't" to "have not", "hasn't" to "has not", "hadn't" to "had not",
            "won't" to "will not", "wouldn't" to "would not", "can't" to "cannot",
            "couldn't" to "could not", "shouldn't" to "should not",
            "we're" to "we are", "they're" to "they are", "you're" to "you are",
            "we've" to "we have", "they've" to "they have", "you've" to "you have",
            "we'll" to "we will", "they'll" to "they will", "you'll" to "you will"
        )
        for ((contraction, expansion) in contractions) {
            t = t.replace(Regex("\\b${Regex.escape(contraction)}\\b"), expansion)
        }
        return t
    }

    /** Number words to digits so "sixty" and "60" match. */
    private fun normalizeNumberWords(s: String): String {
        var t = s
        val numberWords = mapOf(
            "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
            "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
            "ten" to "10", "eleven" to "11", "twelve" to "12", "thirteen" to "13",
            "fourteen" to "14", "fifteen" to "15", "sixteen" to "16", "seventeen" to "17",
            "eighteen" to "18", "nineteen" to "19", "twenty" to "20", "thirty" to "30",
            "forty" to "40", "fifty" to "50", "sixty" to "60", "seventy" to "70",
            "eighty" to "80", "ninety" to "90", "hundred" to "100", "thousand" to "1000"
        )
        for ((word, digit) in numberWords) {
            t = t.replace(Regex("\\b$word\\b", RegexOption.IGNORE_CASE), digit)
        }
        return t
    }

    /** Span that draws a word with colored background and white text, with horizontal padding. */
    private inner class PaddedBackgroundSpan(
        private val bgColor: Int,
        private val textColor: Int,
        private val paddingPx: Int
    ) : ReplacementSpan() {
        override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: android.graphics.Paint.FontMetricsInt?): Int {
            return (paint.measureText(text, start, end) + 2 * paddingPx).toInt()
        }
        override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
            val textWidth = paint.measureText(text, start, end)
            val totalWidth = textWidth + 2 * paddingPx
            paint.style = Paint.Style.FILL
            paint.color = bgColor
            canvas.drawRect(x, top.toFloat(), x + totalWidth, bottom.toFloat(), paint)
            paint.color = textColor
            canvas.drawText(text, start, end, x + paddingPx, y.toFloat(), paint)
        }
    }

    /**
     * Color-code an English sentence: each word has its own background (Subject=blue, Verb=green, Object=orange) and white text, with left/right padding.
     * Declarative: "I eat rice" → subject, verb, object.
     * Questions: [Wh/Aux]=object, [Verb]=verb, [Subject]=subject, rest=object.
     */
    private fun makeSvoSpannable(sentence: String): SpannableString {
        val s = sentence.trim()
        if (s.isEmpty()) return SpannableString("")
        val words = s.split(Regex("\\s+"))
        val subjectBg = ContextCompat.getColor(this, R.color.svo_subject)
        val verbBg = ContextCompat.getColor(this, R.color.svo_verb)
        val objectBg = ContextCompat.getColor(this, R.color.svo_object)
        val whiteText = Color.WHITE
        val paddingPx = (4 * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val spannable = SpannableString(s)
        val isQuestion = words.size >= 3 && words[0].lowercase() in setOf(
            "where", "what", "who", "how", "when", "why", "which",
            "is", "are", "do", "does", "did", "can", "could", "will", "would", "have", "has", "had", "was", "were"
        )
        var start = 0
        for (i in words.indices) {
            val word = words[i]
            val end = start + word.length
            val bgColor = when {
                words.size == 1 -> subjectBg
                isQuestion -> when (i) {
                    0 -> objectBg
                    1 -> verbBg
                    2 -> subjectBg
                    else -> objectBg
                }
                else -> when {
                    i == 0 -> subjectBg
                    i == 1 -> verbBg
                    else -> objectBg
                }
            }
            if (end <= spannable.length) {
                spannable.setSpan(PaddedBackgroundSpan(bgColor, whiteText, paddingPx), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            start = end
            if (start < spannable.length && spannable[start] == ' ') start++
        }
        return spannable
    }

    /** Build description for SVO sentence: "Here subject is X, verb is Y and object is Z." + third-person -s/-es hint when applicable. */
    private fun makeSvoDescription(sentence: String): String {
        val s = sentence.trim()
        if (s.isEmpty()) return ""
        val words = s.split(Regex("\\s+"))
        val isQuestion = words.size >= 3 && words[0].lowercase() in setOf(
            "where", "what", "who", "how", "when", "why", "which",
            "is", "are", "do", "does", "did", "can", "could", "will", "would", "have", "has", "had", "was", "were"
        )
        val subject: String
        val verb: String
        val obj: String
        when {
            words.size == 1 -> {
                subject = words[0]
                verb = ""
                obj = ""
            }
            isQuestion -> {
                subject = if (words.size > 2) words[2] else ""
                verb = if (words.size > 1) words[1] else ""
                obj = if (words.size > 3) words.drop(3).joinToString(" ") else (if (words.isNotEmpty()) words[0] else "")
            }
            else -> {
                subject = words[0]
                verb = if (words.size > 1) words[1] else ""
                obj = if (words.size > 2) words.drop(2).joinToString(" ") else ""
            }
        }
        val svoLine = getString(R.string.description_svo_format, subject, verb, obj).trim()
        val needsThirdPersonHint = subject.lowercase() in setOf("he", "she", "it") ||
            (subject.isNotEmpty() && subject.lowercase() !in setOf("i", "you", "we", "they"))
        val hint = if (needsThirdPersonHint && verb.isNotEmpty()) getString(R.string.description_third_person_hint) else ""
        return if (hint.isNotEmpty()) "$svoLine $hint" else svoLine
    }

    /** Compact HTML for verb conjugation diagram (central verb + arrows to I/You/We/They/He/She/It). Fits in description area. */
    private fun makeVerbDiagramHtml(verb: String): String {
        val v = verb.uppercase().ifEmpty { "HAVE" }
        val third = when (v) {
            "HAVE" -> "has"
            "DO" -> "does"
            "GO" -> "goes"
            else -> "${v.lowercase()}s"
        }
        val phrases = listOf("I ${v.lowercase()}", "You ${v.lowercase()}", "We ${v.lowercase()}", "They ${v.lowercase()}", "He $third", "She $third", "It $third")
        val color = "#c2185b"
        val ys = listOf(15, 28, 41, 54, 67, 80, 93)
        val arrowLines = ys.joinToString("") { y -> "<line x1=\"64\" y1=\"54\" x2=\"118\" y2=\"$y\" stroke=\"#000\" stroke-width=\"1\" marker-end=\"url(#ar)\"/>" }
        val phraseTexts = phrases.mapIndexed { i, p -> "<text x=\"145\" y=\"${ys[i] + 4}\" text-anchor=\"start\" font-weight=\"600\" fill=\"$color\" font-size=\"9\" font-family=\"sans-serif\">$p</text>" }.joinToString("")
        return """
<!DOCTYPE html>
<html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="margin:0;padding:2px;background:transparent;">
<div style="text-align:center;background:$color;color:#fff;padding:2px 6px;margin-bottom:4px;border-radius:3px;font-weight:bold;font-size:11px;font-family:sans-serif;">The Verb '$v'</div>
<svg viewBox="0 0 200 108" style="width:100%;height:100%;min-height:100px;display:block;" preserveAspectRatio="xMidYMid meet">
<defs><marker id="ar" markerWidth="5" markerHeight="4" refX="4" refY="2" orient="auto"><polygon points="0 0,5 2,0 4" fill="#000"/></marker></defs>
<circle cx="42" cy="54" r="20" fill="#fff" stroke="$color" stroke-width="2"/>
<text x="42" y="58" text-anchor="middle" font-weight="bold" fill="$color" font-size="12" font-family="sans-serif">$v</text>
$arrowLines
$phraseTexts
</svg>
</body></html>"""
    }

    /** Pick which verb to show in the diagram from the current lesson name (e.g. verb_GO -> GO, regular_verbs -> EAT). */
    private fun verbForLessonDiagram(lessonName: String?): String {
        if (lessonName.isNullOrEmpty()) return "HAVE"
        return when {
            lessonName.startsWith("verb_") -> lessonName.removePrefix("verb_")
            lessonName == "regular_verbs" -> "EAT"
            lessonName == "irregular_verbs" -> "GO"
            else -> "HAVE"
        }
    }

    private fun showVerbDiagram(verb: String) {
        setDescriptionInstruction(null, null)
        pronunciationLessonRows = null
        pronunciationLessonTitle = null
        val html = makeVerbDiagramHtml(verb)
        descriptionWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun clearDescriptionWebView() {
        descriptionWebView.loadUrl("about:blank")
        setDescriptionInstruction(null, null)
    }

    /** Set the hidden instruction paragraph for the description area (spoken when user taps the speaker icon). Use null to clear. */
    private fun setDescriptionInstruction(text: String?, locale: Locale?) {
        descriptionInstructionText = text
        descriptionInstructionLocale = locale
    }

    /** Load a diagram HTML from assets/diagrams/ (e.g. diagram-1to3.html, diagram-3to1.html). Used for testing and context-based layout. */
    private fun loadDiagramFromAssets(filename: String) {
        pronunciationLessonRows = null
        pronunciationLessonTitle = null
        descriptionWebView.loadUrl("file:///android_asset/diagrams/$filename")
    }

    /**
     * Reference pages in assets root (e.g. parts-of-speech.html, svo-sentences.html).
     * Uses [ContentLayout.DIAGRAM_ONLY] WebView inside the main activity (hamburger + title stay in activity_main).
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun loadReferenceHtmlPage(assetFileName: String, displayTitle: String) {
        clearPronunciationLessonState()
        lessonName = displayTitle
        updateLessonTopicDisplay()
        val root = switchContentLayout(ContentLayout.DIAGRAM_ONLY)
        root.findViewById<View>(R.id.diagram_info_button)?.visibility = View.GONE
        val webView = root.findViewById<WebView>(R.id.diagram_webview) ?: return
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/$assetFileName")
    }

    /** One config-driven table layout: columns/headers/rows come from config. Builds pure HTML (no JavaScript) so it works with WebView JS disabled. */
    private fun makeTableLayoutHtml(intro: String, headers: List<String>, rows: List<List<String>>): String {
        return makeTableLayoutHtmlInternal(intro, headers, rows, makeFirstColumnTappable = false)
    }

    /** Pronunciation table: same as above but first column cells are links word://... so tap speaks the English word. */
    private fun makePronunciationTableHtml(headers: List<String>, rows: List<List<String>>): String {
        return makeTableLayoutHtmlInternal("", headers, rows, makeFirstColumnTappable = true)
    }

    private fun makeTableLayoutHtmlInternal(intro: String, headers: List<String>, rows: List<List<String>>, makeFirstColumnTappable: Boolean): String {
        fun htmlEsc(s: String): String = s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
        val colCount = maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0)
        val introHtml = if (intro.isNotEmpty()) "<div style=\"margin-bottom:8px;line-height:1.4;\">$intro</div>" else ""
        val headerCells = (0 until colCount).joinToString("") { i ->
            val text = headers.getOrNull(i) ?: ""
            "<th style=\"border:1px solid #000;padding:8px 10px;text-align:center;font-weight:bold;background:#f5f5f5;\">${htmlEsc(text)}</th>"
        }
        val bodyRows = rows.joinToString("") { row ->
            val cells = (0 until colCount).joinToString("") { c ->
                val text = row.getOrNull(c) ?: ""
                val escaped = htmlEsc(text)
                val cellContent = if (makeFirstColumnTappable && c == 0 && text.isNotEmpty()) {
                    val href = "word://" + URLEncoder.encode(text, StandardCharsets.UTF_8.name())
                    "<a href=\"$href\" style=\"color:#0066cc;text-decoration:underline;\">$escaped</a>"
                } else {
                    escaped
                }
                "<td style=\"border:1px solid #000;padding:8px 10px;text-align:center;font-weight:bold;\">$cellContent</td>"
            }
            "<tr>$cells</tr>"
        }
        return """
<!DOCTYPE html>
<html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="margin:0;padding:8px;font-family:sans-serif;background:transparent;font-size:14px;">
$introHtml
<div style="overflow-x:auto;">
<table style="width:100%;border-collapse:collapse;border:1px solid #000;">
<thead><tr>$headerCells</tr></thead>
<tbody>$bodyRows</tbody>
</table>
</div>
</body></html>"""
    }

    /** Show config-driven table in description area (3 or 4 columns based on config). For testing. */
    private fun showTableInDescription(intro: String, headers: List<String>, rows: List<List<String>>) {
        setDescriptionInstruction(null, null)
        pronunciationLessonRows = null
        pronunciationLessonTitle = null
        val html = makeTableLayoutHtml(intro, headers, rows)
        descriptionWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    /** Clear pronunciation-lesson state so lecture button won't speak the word list. */
    private fun clearPronunciationLessonState() {
        pronunciationLessonRows = null
        pronunciationLessonTitle = null
        pronunciationPracticeActive = false
        pendingPronunciationPracticeWord = null
        cancelVerificationTimeout()
    }

    /** Show introduction text in description area and set topic. Call when user loads introduction.txt from file. */
    private fun showIntroductionContent(bengaliText: String) {
        clearPronunciationLessonState()
        lessonRows = null
        lessonName = getString(R.string.introduction_topic)
        setDescriptionInstruction(null, null)
        pronunciationLessonRows = null
        pronunciationLessonTitle = null
        clearBothTextAreas()
        setSentenceListVisibility(false)
        updateLessonTopicDisplay()
        val escaped = bengaliText
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br>")
        val html = """
<!DOCTYPE html>
<html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="margin:0;padding:12px;font-family:sans-serif;background:transparent;font-size:16px;line-height:1.6;color:#111;">
<div style="white-space:pre-wrap;word-wrap:break-word;">$escaped</div>
</body></html>"""
        descriptionWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        textView.text = ""
        Toast.makeText(this, getString(R.string.introduction_loaded), Toast.LENGTH_SHORT).show()
    }

    /**
     * Split Bengali text into small segments for natural reading and fine-grained pause/resume.
     * Splits on: | (pipe), । ৷ (Bengali full stops), newlines, period+space, commas, semicolons.
     * Each segment is kept short (~a few seconds of speech) so resume after pause
     * continues very close to where the user paused.
     */
    private fun splitIntroductionSegments(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val segments = mutableListOf<String>()
        // First split on major delimiters: pipe, Bengali stops, newlines, period+space
        val majorParts = text.split(Regex("[|।৷]|\\n+|\\.\\s+"))
        for (major in majorParts) {
            val trimmed = major.trim()
            if (trimmed.isEmpty()) continue
            // If the part is short enough (<60 chars), keep it as one segment
            if (trimmed.length < 60) {
                segments.add(trimmed)
            } else {
                // Further split on commas and semicolons for finer granularity
                val subParts = trimmed.split(Regex("(?<=[,;،])|(?<=[,;،])\\s+"))
                for (sub in subParts) {
                    val s = sub.trim()
                    if (s.isNotEmpty()) segments.add(s)
                }
            }
        }
        if (segments.isEmpty()) segments.add(text.trim())
        return segments
    }

    /**
     * Speak introduction text in Bengali with natural pacing.
     * @param fromSegment segment index to start from (0 = beginning). Used for resume after pause.
     */
    private fun speakIntroductionBengali(fullText: String, fromSegment: Int = 0) {
        if (fullText.isBlank() || !ttsReady || textToSpeech == null) return
        // Build / reuse segments
        if (introSegments.isEmpty() || textDisplayBodyText != fullText) {
            introSegments = splitIntroductionSegments(fullText)
        }
        if (introSegments.isEmpty()) return
        val startIdx = fromSegment.coerceIn(0, introSegments.size - 1)
        introSegmentIndex = startIdx
        textToSpeech?.setLanguage(Locale("bn"))
        textToSpeech?.setSpeechRate(0.88f)
        var first = true
        for (i in startIdx until introSegments.size) {
            val utteranceId = if (i == introSegments.size - 1) "intro_done" else "intro_$i"
            textToSpeech?.speak(
                introSegments[i],
                if (first) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                utteranceId
            )
            first = false
        }
    }

    /** Show a pronunciation lesson: 3-column table (Word, Pronunciation, Meaning). Empty string for missing 3rd column. Tapping lecture speaks each word in sequence. */
    private fun showPronunciationLesson(title: String, rows: List<List<String>>) {
        lessonRows = null
        lessonName = null
        pronunciationLessonRows = rows
        pronunciationLessonTitle = title
        setDescriptionInstruction(null, null)
        clearBothTextAreas()
        setSentenceListVisibility(false)
        val threeColRows = rows.map { row -> listOf(row.getOrNull(0) ?: "", row.getOrNull(1) ?: "", row.getOrNull(2) ?: "") }
        val html = makePronunciationTableHtml(listOf("Word", "Pronunciation", "Meaning"), threeColRows)
        descriptionWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        textView.text = getString(R.string.pronunciation_lesson_hint)
        updateLessonTopicDisplay()
    }

    /** Build "Expected: [SVO-colored sentence]\n\nHeard: [heard]". */
    private fun makeExpectedAndHeardSpannable(expected: String, heard: String): SpannableStringBuilder {
        return SpannableStringBuilder().append(getString(R.string.expected_label)).append(" ").append(makeSvoSpannable(expected)).append("\n\n").append(getString(R.string.heard_label)).append(" ").append(MatchNormalizer.sanitizeSpokenTextForDisplay(heard))
    }

    /** Build "Expected: [SVO-colored sentence]\n\nYou said: [said]". */
    private fun makeExpectedAndYouSaidSpannable(expected: String, said: String): SpannableStringBuilder {
        return SpannableStringBuilder().append(getString(R.string.expected_label)).append(" ").append(makeSvoSpannable(expected)).append("\n\n").append(getString(R.string.you_said_label)).append(" ").append(MatchNormalizer.sanitizeSpokenTextForDisplay(said))
    }

    /** Normalize string for verification match: contractions, number words→digits, trim, lowercase, collapse spaces, remove punctuation. */
    private fun normalizeForMatch(s: String): String {
        val lower = s.trim().lowercase()
            .replace('\u2019', '\'')  // curly apostrophe → straight (so "i'm" from speech/translation matches)
        val expanded = normalizeContractions(lower)
        return normalizeNumberWords(expanded)
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9 ]"), "")
    }

    /** Cancel verification timeout (call when we get result or error). */
    private fun cancelVerificationTimeout() {
        verificationTimeoutRunnable?.let { verificationHandler.removeCallbacks(it) }
        verificationTimeoutRunnable = null
    }

    /** Start listening for user to speak English (verification mode); compare with expected and say Correct/Incorrect. */
    private fun startVerificationListening(expectedEnglish: String) {
        if (!checkForPermission(RECORD_AUDIO)) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
            return
        }
        cancelVerificationTimeout()
        verificationMode = true
        verificationResultHandled = false
        verificationLastHeardText = ""
        expectedEnglishForVerification = expectedEnglish
        isEnglishMicActive = true
        isRecording = true
        verificationTimeoutRunnable = Runnable {
            runOnUiThread {
                if (verificationMode || pronunciationPracticeActive) {
                    verificationMode = false
                    val expected = expectedEnglishForVerification
                    expectedEnglishForVerification = null
                    if (USE_SYSTEM_SPEECH_FOR_ENGLISH_VERIFICATION) {
                        speechRecognizer?.stopListening()
                    } else {
                        stopEnglishVoskRecording()
                    }
                    setMicButtonAppearance(recording = false)
                    if (pronunciationPracticeActive && expected != null) {
                        // For pronunciation practice, keep existing 3-tries flow.
                        pronunciationPracticeResultHandled = true
                        pronunciationPracticeAttempt = 2
                        handlePronunciationPracticeResult(false, expected, "")
                    } else if (!expected.isNullOrBlank()) {
                        // For lessons (including 3-col, simple-sentence, etc.), treat silence as an incorrect attempt
                        // so the same Correct/Incorrect + advance logic runs and the mic will be re-armed or the
                        // lesson will move to the next sentence instead of staying idle.
                        handleVerificationResult(false, expected, verificationLastHeardText)
                    } else {
                        // Fallback: no expected text; just inform the user.
                        speakEnglishString(getString(R.string.no_speech_detected))
                        Toast.makeText(this@MainActivity, getString(R.string.no_speech_detected), Toast.LENGTH_LONG).show()
                    }
                }
                verificationTimeoutRunnable = null
            }
        }
        verificationHandler.postDelayed(verificationTimeoutRunnable!!, 15000)
        // Only show "recording" once the mic is actually listening.
        setMicButtonAppearance(recording = false)
        if (USE_SYSTEM_SPEECH_FOR_ENGLISH_VERIFICATION) {
            initEnglishRecognizer()
            setMicButtonAppearance(recording = true)
            speechRecognizer?.startListening(recognizerIntent)
            Log.d(TAG, "Verification: listening for user to speak English (system SpeechRecognizer)")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val recognizer = voskEnInRecognizer ?: VoskEnInRecognizer(this@MainActivity).also { voskEnInRecognizer = it }
            val ready = recognizer.ensureModelReady()
            withContext(Dispatchers.Main) {
                if (!verificationMode || isDestroyed) return@withContext
                if (!ready) {
                    verificationMode = false
                    expectedEnglishForVerification = null
                    isEnglishMicActive = false
                    isRecording = false
                    Toast.makeText(this@MainActivity, "Vosk Indian English model failed to load", Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                // Now it's safe to show mic active and give an audible cue.
                setMicButtonAppearance(recording = true)
                playStartBeep()
                startEnglishVoskRecording()
            }
        }
        Log.d(TAG, "Verification: listening for user to speak English (Vosk)")
    }

    /** Update live lesson statistic: correct / questions asked so far (p%). */
    private fun updateLessonStatistic() {
        val rows = lessonRows
        if (rows == null || rows.isEmpty()) {
            lessonStatTextView.visibility = View.GONE
            return
        }
        lessonStatTextView.visibility = View.VISIBLE
        val asked = lessonIndex
        val pct = if (asked > 0) (100 * lessonCorrectCount / asked) else 0
        lessonStatTextView.text = "$lessonCorrectCount/$asked ($pct%)"
    }

    /** After a correct match in lesson mode: advance phase (mode 1) or index (mode 2). */
    private fun advanceLessonAfterMatch() {
        val rows = lessonRows ?: return
        if (lessonIndex >= rows.size) return
        when (lessonMode) {
            1 -> if (lessonPhase == "q") lessonPhase = "a" else { lessonIndex++; lessonPhase = "q" }
            2 -> lessonIndex++
            4 -> lessonIndex++
            else -> { }
        }
    }

    /** Skip to next row (e.g. after 3 incorrect attempts). */
    private fun advanceLessonToNextRow() {
        lessonIndex++
        if (lessonMode == 1) lessonPhase = "q"
    }

    /** Skip current word and move to next (escape incorrect loop). Only active when a lesson is loaded. */
    private fun onSkipWord() {
        if (lessonRows == null) {
            Toast.makeText(this, getString(R.string.skip), Toast.LENGTH_SHORT).show()
            return
        }
        cancelVerificationTimeout()
        incorrectFeedbackFallbackRunnable?.let { verificationHandler.removeCallbacks(it) }
        incorrectFeedbackFallbackRunnable = null
        tryAgainListenFallbackRunnable?.let { verificationHandler.removeCallbacks(it) }
        tryAgainListenFallbackRunnable = null
        pendingSpeakCorrectWordAfterIncorrect = null
        pendingRestartVerificationWith = null
        verificationMode = false
        expectedEnglishForVerification = null
        verificationResultHandled = true
        if (USE_SYSTEM_SPEECH_FOR_ENGLISH_VERIFICATION) {
            speechRecognizer?.stopListening()
        } else {
            stopEnglishVoskRecording()
        }
        setMicButtonAppearance(recording = false)
        advanceLessonToNextRow()
        updateLessonStatistic()
        speakEnglishString("Skipped.")
        onNextLessonStep()
    }

    /** Next step in lesson: dispatch by mode and phase; or show "Lesson done" if finished. */
    private fun onNextLessonStep() {
        val rows = lessonRows ?: return
        if (lessonIndex >= rows.size) {
            val finalCorrect = lessonCorrectCount
            val finalAsked = rows.size
            val finalPct = if (finalAsked > 0) 100 * finalCorrect / finalAsked else 0
            val scoreText = getString(R.string.lesson_done) + "\n\nScore: $finalCorrect/$finalAsked ($finalPct%)"
            speakEnglishString(getString(R.string.lesson_done))
            Toast.makeText(this, scoreText, Toast.LENGTH_LONG).show()
            if (currentContentLayout == ContentLayout.SIMPLE_SENTENCE) {
                simpleSentenceControlRunning = false
                simpleSentenceControlPaused = false
                updateSimpleSentenceControlBar()
            }
            lessonRows = null
            simpleSentencePronunciations = null
            lessonName = null
            lessonMode = 0
            lessonIndex = 0
            lessonPhase = "q"
            lessonMode3Listening = false
            updateLessonTopicDisplay()
            lessonMode3SpokeAnswer = false
            nextButton?.isEnabled = sentenceList.isNotEmpty()
            skipButton?.isEnabled = false
            clearBothTextAreas()
            textView.text = scoreText
            lessonStatTextView.visibility = View.VISIBLE
            lessonStatTextView.text = "Score: $finalCorrect/$finalAsked ($finalPct%)"
            return
        }
        if (lessonMode == 3 && lessonMode3SpokeAnswer) {
            lessonIndex++
            lessonMode3SpokeAnswer = false
            if (lessonIndex >= rows.size) {
                val finalCorrect = lessonCorrectCount
                val finalAsked = rows.size
                val finalPct = if (finalAsked > 0) 100 * finalCorrect / finalAsked else 0
                val scoreText = getString(R.string.lesson_done) + "\n\nScore: $finalCorrect/$finalAsked ($finalPct%)"
                speakEnglishString(getString(R.string.lesson_done))
                Toast.makeText(this, scoreText, Toast.LENGTH_LONG).show()
                lessonRows = null
                simpleSentencePronunciations = null
                lessonName = null
                lessonMode = 0
                lessonIndex = 0
                lessonPhase = "q"
                lessonMode3Listening = false
                updateLessonTopicDisplay()
                nextButton?.isEnabled = sentenceList.isNotEmpty()
                skipButton?.isEnabled = false
                clearBothTextAreas()
                textView.text = scoreText
                lessonStatTextView.visibility = View.VISIBLE
                lessonStatTextView.text = "Score: $finalCorrect/$finalAsked ($finalPct%)"
                return
            }
        }
        val row = rows[lessonIndex]
        if (currentContentLayout == ContentLayout.SIMPLE_SENTENCE) {
            updateSimpleSentenceView()
            expectedEnglishForVerification = row.engA
            simpleSentenceControlRunning = true
            simpleSentenceControlPaused = false
            updateSimpleSentenceControlBar()
            textToSpeech?.setLanguage(Locale("bn"))
            if (simpleSentencePracticeMode) {
                textToSpeech?.speak(row.bnQ, TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
            } else {
                pendingSimpleSentenceEnglishForLearning = row.engA
                textToSpeech?.speak(row.bnQ, TextToSpeech.QUEUE_FLUSH, null, "lesson_verify_bengali")
            }
            return
        }
        when (lessonMode) {
            1 -> {
                if (lessonPhase == "q") {
                    textView.text = row.bnQ
                    englishTextView.setText(makeSvoSpannable(row.engQ))
                    expectedEnglishForVerification = row.engQ
                    textToSpeech?.setLanguage(Locale("bn"))
                    textToSpeech?.speak(row.bnQ, TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
                } else {
                    textView.text = row.bnA
                    englishTextView.setText(makeSvoSpannable(row.engA))
                    expectedEnglishForVerification = row.engA
                    textToSpeech?.setLanguage(Locale("bn"))
                    textToSpeech?.speak(row.bnA, TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
                }
            }
            2 -> {
                textView.text = row.bnQ
                englishTextView.text = ""
                expectedEnglishForVerification = row.engA
                textToSpeech?.setLanguage(Locale.ENGLISH)
                textToSpeech?.speak(MatchNormalizer.textForSpeakAndDisplay(row.engQ), TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
            }
            3 -> {
                lessonMode3Listening = true
                textView.text = row.bnQ
                englishTextView.text = "Say the question in English…"
                if (!checkForPermission(RECORD_AUDIO)) {
                    Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
                    lessonMode3Listening = false
                    return
                }
                isEnglishMicActive = true
                isRecording = true
                CoroutineScope(Dispatchers.IO).launch {
                    val recognizer = voskEnInRecognizer ?: VoskEnInRecognizer(this@MainActivity).also { voskEnInRecognizer = it }
                    val ready = recognizer.ensureModelReady()
                    withContext(Dispatchers.Main) {
                        if (!lessonMode3Listening || isDestroyed) return@withContext
                        if (!ready) {
                            lessonMode3Listening = false
                            isEnglishMicActive = false
                            isRecording = false
                            Toast.makeText(this@MainActivity, "Vosk Indian English model failed to load", Toast.LENGTH_SHORT).show()
                            return@withContext
                        }
                        startEnglishVoskRecording()
                    }
                }
            }
            4 -> {
                val bengaliStr = row.bnQ
                val englishStr = row.engA
                if (bengaliStr.length < 5) {
                    Toast.makeText(this, "Debug: Bengali text short or empty (length=${bengaliStr.length})", Toast.LENGTH_LONG).show()
                }
                val upperBox = findViewById<TextView>(R.id.my_text)
                upperBox.setBackgroundColor(Color.WHITE)
                upperBox.setTextColor(Color.BLACK)
                upperBox.text = bengaliStr
                textView.text = bengaliStr
                textView.post {
                    upperBox.setBackgroundColor(Color.WHITE)
                    upperBox.setTextColor(Color.BLACK)
                    upperBox.text = bengaliStr
                }
                englishTextView.setText(makeSvoSpannable(englishStr))
                // Description area shows verb diagram when lesson loaded (set at load time)
                expectedEnglishForVerification = row.engA
                textToSpeech?.setLanguage(Locale("bn"))
                textToSpeech?.speak(row.bnQ, TextToSpeech.QUEUE_FLUSH, null, "lesson_verify")
            }
        }
    }

    /** Next: show sentence. If Bengali, show translation and listen for English. If English (SVO), speak sentence then listen and verify (correct → next; 3 wrong → next). */
    private fun onNextSentence() {
        if (sentenceList.isEmpty()) return
        if (currentNextIndex >= sentenceList.size) currentNextIndex = 0
        val sentence = sentenceList[currentNextIndex]

        if (sentence.isBengali) {
            currentNextIndex = (currentNextIndex + 1) % sentenceList.size
            textView.text = sentence.text
            val trans = translator ?: return
            trans.downloadModelIfNeeded()
                .addOnSuccessListener {
                    trans.translate(sentence.text)
                        .addOnSuccessListener { translated ->
                            runOnUiThread {
                                englishTextView.text = translated
                                pendingVerificationExpectedEnglish = translated
                                textToSpeech?.setLanguage(Locale("bn"))
                                textToSpeech?.speak(sentence.text, TextToSpeech.QUEUE_FLUSH, null, "bengali_verification")
                            }
                        }
                        .addOnFailureListener { e ->
                            runOnUiThread { Toast.makeText(this@MainActivity, "Translation failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                        }
                }
                .addOnFailureListener { e ->
                    runOnUiThread { Toast.makeText(this@MainActivity, "Model download failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
        } else {
            // English (SVO list): show sentence, speak it, then listen for user to repeat; do not advance until correct or 3 strikes
            textView.setText(makeSvoSpannable(sentence.text))
            englishTextView.setText(makeSvoSpannable(sentence.text))
            pendingVerificationExpectedEnglish = sentence.text
            textToSpeech?.setLanguage(Locale.US)
            textToSpeech?.speak(sentence.text, TextToSpeech.QUEUE_FLUSH, null, "bengali_verification")
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // If drawer is open, close it first
        if (drawerLayout.isDrawerOpen(findViewById<View>(R.id.nav_drawer))) {
            drawerLayout.closeDrawers()
            return
        }
        // Fully exit the app — kill process so next launch is fresh
        finishAndRemoveTask()
    }

    override fun onPause() {
        super.onPause()
        // ── Stop ALL TTS ──
        textToSpeech?.stop()
        ttsPlayState = TtsPlayState.IDLE
        updateSpeakButtonIcon()

        // ── Stop ALL mic / speech recognition ──
        stopAllMic()
        cancelVerificationTimeout()
    }

    override fun onDestroy() {
        cancelVerificationTimeout()
        stopEnglishVoskRecording()
        voskEnInRecognizer?.close()
        voskEnInRecognizer = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        translator?.close()
        translator = null
        translatorEnToBn?.close()
        translatorEnToBn = null
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    private fun isInputBengali(): Boolean {
        // If speech-input layout is active, use its language selector
        if (currentContentLayout == ContentLayout.SPEECH_INPUT) {
            return isSpeechInputBengali()
        }
        return findViewById<RadioButton?>(R.id.radio_bengali)?.isChecked ?: true
    }

    private fun updateTranslationLabelAndButton() {
        if (isInputBengali()) {
            translationLabel.text = getString(R.string.english_translation_label)
        } else {
            translationLabel.text = getString(R.string.bengali_translation_label)
        }
    }

    /** Clear both the main text and the translation (scrollable) areas and force refresh. */
    private fun clearBothTextAreas() {
        // Only touch legacy views if they are present in the current layout
        if (currentContentLayout == ContentLayout.LEGACY) {
            textView.setText("")
            textView.scrollTo(0, 0)
            englishTextView.setText("")
            englishTextView.scrollTo(0, 0)
            englishTextView.invalidate()
            englishTextView.requestLayout()
            clearDescriptionWebView()
        }
        lastText = ""
        idx = 0
    }

    /** Clear the sentence list in the UI (used when stopping microphone). */
    private fun clearSentenceListUi() {
        sentenceList.clear()
        if (currentContentLayout == ContentLayout.LEGACY) {
            sentenceAdapter.notifyDataSetChanged()
        }
        currentNextIndex = 0
        nextButton?.isEnabled = false
        skipButton?.isEnabled = false
    }

    /** Remove sentence at position (for edit list). */
    private fun removeSentenceAt(position: Int) {
        if (position < 0 || position >= sentenceList.size) return
        sentenceList.removeAt(position)
        sentenceAdapter.notifyItemRemoved(position)
        if (currentNextIndex >= sentenceList.size) currentNextIndex = 0
        nextButton?.isEnabled = sentenceList.isNotEmpty()
        skipButton?.isEnabled = lessonRows != null
    }

    /** Drag-to-reorder callback for sentence list. Long-press and drag to reorder. */
    private val sentenceDragCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        0
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
            val item = sentenceList.removeAt(from)
            sentenceList.add(to, item)
            sentenceAdapter.notifyItemMoved(from, to)
            when {
                currentNextIndex == from -> currentNextIndex = to
                from < to && currentNextIndex > from && currentNextIndex <= to -> currentNextIndex--
                to < from && currentNextIndex >= to && currentNextIndex < from -> currentNextIndex++
            }
            return true
        }
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    }

    private fun onDemo() {
        Log.i(TAG, "onDemo() called - Start Microphone button click received")

        // ── Special handling for speech-input layout ──
        if (currentContentLayout == ContentLayout.SPEECH_INPUT) {
            if (isRecording || isEnglishMicActive || speechInputEnglishListening) {
                // Stop
                stopAllMic()
                speechInputEnglishListening = false
                Toast.makeText(this, "Mic stopped", Toast.LENGTH_SHORT).show()
            } else {
                // Start
                setMicButtonAppearance(recording = true)
                if (isInputBengali()) {
                    startBengaliMic()
                } else {
                    startSpeechInputEnglish()
                }
            }
            return
        }

        // ── Special handling for practice 3-area layout ──
        if (currentContentLayout == ContentLayout.PRACTICE_THREE_AREA) {
            if (isRecording || practiceListening) {
                stopAllMic()
                practiceListening = false
                Toast.makeText(this, "Mic stopped", Toast.LENGTH_SHORT).show()
            } else {
                if (practiceWordIndex < practiceWordList.size) {
                    startPracticeListening()
                } else {
                    Toast.makeText(this, "Practice complete! Reload to try again.", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        // ── Normal flow for legacy/other layouts ──
        if (!isRecording && !isEnglishMicActive) {
            clearBothTextAreas()
            englishTextView.post { clearBothTextAreas() }
            if (isInputBengali()) {
                startBengaliMic()
            } else {
                startEnglishMic()
            }
            return
        }
        // Stop
        if (isEnglishMicActive) {
            stopEnglishMic()
            return
        }
        stopMicRecording(speakBengali = false)
    }

    /** Start Google System SpeechRecognizer for English in the speech-input layout (continuous). */
    private fun startSpeechInputEnglish() {
        if (!checkForPermission(RECORD_AUDIO)) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, arrayOf(RECORD_AUDIO), PERMISSION_REQUEST_CODE)
            return
        }
        speechInputEnglishListening = true
        isRecording = true
        setMicButtonAppearance(recording = true)

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }
        speechRecognizer?.setRecognitionListener(createSpeechInputEnglishListener())
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
        updateSpeechInputStatus(true)
    }

    /** RecognitionListener for English in speech-input layout. */
    private fun createSpeechInputEnglishListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {
            runOnUiThread { updateSpeechInputStatus(true) }
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            Log.w(TAG, "SpeechInput English recognition error: $error")
            runOnUiThread {
                if (speechInputEnglishListening && !isDestroyed) {
                    // Auto-restart on timeout (error 6) or no match (error 7)
                    if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                        })
                    } else {
                        speechInputEnglishListening = false
                        isRecording = false
                        setMicButtonAppearance(recording = false)
                        updateSpeechInputStatus(false)
                        Toast.makeText(this@MainActivity, "Recognition error ($error). Tap mic to retry.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        override fun onResults(results: android.os.Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim() ?: ""
            runOnUiThread {
                if (text.isNotBlank()) {
                    feedSpeechInputText(text, isFinal = true)
                }
                // Auto-restart for continuous listening
                if (speechInputEnglishListening && !isDestroyed) {
                    speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    })
                }
            }
        }
        override fun onPartialResults(partialResults: android.os.Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim() ?: ""
            if (text.isNotBlank()) {
                runOnUiThread { feedSpeechInputText(text, isFinal = false) }
            }
        }
        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    }

    private fun startBengaliMic() {
        try {
            if (!modelLoaded && !SKIP_RECOGNITION_FOR_MIC) {
                Toast.makeText(this, "Model not loaded yet", Toast.LENGTH_SHORT).show()
                return
            }
            Log.i(TAG, "onDemo: checking RECORD_AUDIO permission")
            if (!checkForPermission(RECORD_AUDIO)) {
                Log.w(TAG, "RECORD_AUDIO not granted, requesting RECORD_AUDIO...")
                Toast.makeText(this, "Microphone permission required – please allow when prompted", Toast.LENGTH_LONG).show()
                ActivityCompat.requestPermissions(this, arrayOf(RECORD_AUDIO), PERMISSION_REQUEST_CODE)
                return
            }
            Log.i(TAG, "onDemo: calling initMicrophone()")
            val ret = initMicrophone()
                if (!ret) {
                    Log.e(TAG, "Failed to initialize microphone")
                    Toast.makeText(this, "Failed to open microphone", Toast.LENGTH_SHORT).show()
                    return
                }
                Log.i(TAG, "state: ${audioRecord?.state}")
                try {
                    audioRecord!!.startRecording()
                } catch (e: Exception) {
                    Log.e(TAG, "startRecording() failed", e)
                    Toast.makeText(this, "startRecording failed: ${e.message}", Toast.LENGTH_LONG).show()
                    audioRecord?.release()
                    audioRecord = null
                    return
                }
                if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    Log.e(TAG, "AudioRecord not in RECORDING state after startRecording()")
                    audioRecord?.release()
                    audioRecord = null
                    Toast.makeText(this, "Microphone failed to start", Toast.LENGTH_LONG).show()
                    return
                }
                setMicButtonAppearance(recording = true)
                isRecording = true
                if (!SKIP_RECOGNITION_FOR_MIC) recognizer.resetModel()
                clearBothTextAreas()
                englishTextView.invalidate()
                englishTextView.requestLayout()

                this.recordingThread = thread(true) {
                    processSamples()
                }
                Log.i(TAG, "Started recording")
            } catch (e: Throwable) {
                Log.e(TAG, "Start microphone failed", e)
                isRecording = false
                audioRecord?.release()
                audioRecord = null
                setMicButtonAppearance(recording = false)
                Toast.makeText(this, "Start mic failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /** Stops mic recording, updates UI, and optionally speaks the recognized Bengali text. */
    private fun stopMicRecording(speakBengali: Boolean) {
        if (!isRecording) return
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        setMicButtonAppearance(recording = false)
        if (currentContentLayout == ContentLayout.LEGACY) {
            clearBothTextAreas()
            clearSentenceListUi()
        }
        Log.i(TAG, "Stopped recording")
        if (speakBengali) speakBengaliText()
    }

    // Accumulate at least this many samples before first recognizer call (1 sec at 16 kHz).
    // Some native engines crash on the first small chunk; feeding a full second first can help.
    private val minSamplesBeforeRecognizer = 16000

    private fun processSamples() {
        val sampleRate = if (SKIP_RECOGNITION_FOR_MIC) 16000 else recognizer.getSampleRate()
        val interval = 0.1 // 100 ms
        val bufferSize = (interval * sampleRate).toInt().coerceAtLeast(1600)
        val buffer = ShortArray(bufferSize)
        val preBuffer = mutableListOf<Short>()
        var sentFirstChunk = false
        val startTimeMs = System.currentTimeMillis()

        // Give startRecording() time to fully initialize on some devices
        try {
            Thread.sleep(150)
        } catch (_: InterruptedException) { }

        try {
            while (isRecording) {
                val ret = try {
                    audioRecord?.read(buffer, 0, buffer.size) ?: 0
                } catch (e: Exception) {
                    Log.e(TAG, "AudioRecord.read() failed", e)
                    runOnUiThread {
                        if (!isDestroyed) Toast.makeText(this@MainActivity, "Microphone read error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    break
                }
                if (ret <= 0) continue

                val chunk = if (ret < buffer.size) buffer.copyOf(ret) else buffer

                if (SKIP_RECOGNITION_FOR_MIC) {
                    // Test mode: only record, don't touch recognizer at all (avoids native crash in resetModel/processSamples).
                    if (!isDestroyed) {
                        runOnUiThread {
                            if (!isDestroyed) {
                                val sec = (System.currentTimeMillis() - startTimeMs) / 1000
                                textView.text = "Recording... (no recognizer) ${sec}s"
                            }
                        }
                    }
                    continue
                }

                if (!sentFirstChunk) {
                    preBuffer.addAll(chunk.toList())
                    if (preBuffer.size >= minSamplesBeforeRecognizer) {
                        sentFirstChunk = true
                        val firstChunk = preBuffer.take(minSamplesBeforeRecognizer).toShortArray()
                        val remainder = preBuffer.drop(minSamplesBeforeRecognizer)
                        preBuffer.clear()
                        preBuffer.addAll(remainder)
                        try {
                            recognizer.processSamples(firstChunk)
                        } catch (e: Exception) {
                            Log.e(TAG, "Recognizer first chunk error", e)
                            runOnUiThread {
                                if (!isDestroyed) Toast.makeText(this@MainActivity, "Recognition error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    continue
                }

                if (chunk.size < 800) continue
                try {
                    val result = recognizer.processSamples(chunk)
                    val text = result.first
                    val endpoint = result.second

                    Log.d(TAG, "Recognized $text")
                    if (text.isNotBlank()) {
                        var textToDisplay: String
                        if (endpoint) {
                            lastText = if (lastText.isEmpty()) text else "${lastText}\n$text"
                            textToDisplay = lastText
                            idx += 1
                            runOnUiThread {
                                if (!isDestroyed) {
                                    if (!feedSpeechInputText(text, isFinal = true)) {
                                        textView.text = textToDisplay
                                        translateSegmentAndSpeakEnglish(text)
                                    }
                                }
                            }
                        } else {
                            textToDisplay = if (lastText.isEmpty()) text else "${lastText}\n$text"
                            runOnUiThread {
                                if (!isDestroyed) {
                                    if (!feedSpeechInputText(text, isFinal = false)) {
                                        textView.text = textToDisplay
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Recognizer error", e)
                    runOnUiThread {
                        if (!isDestroyed) Toast.makeText(this@MainActivity, "Recognition error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recording thread error", e)
            runOnUiThread {
                if (!isDestroyed) {
                    isRecording = false
                    audioRecord?.stop()
                    audioRecord?.release()
                    audioRecord = null
                    setMicButtonAppearance(recording = false)
                    Toast.makeText(this@MainActivity, "Microphone error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun initMicrophone(): Boolean {
        Log.i(TAG, "initMicrophone() entered")
        if (!SKIP_RECOGNITION_FOR_MIC) {
            try {
                checkHeadset()
            } catch (e: Exception) {
                Log.w(TAG, "checkHeadset failed (non-fatal)", e)
            }
        }

        val sampleRateInHz = if (SKIP_RECOGNITION_FOR_MIC) 16000 else recognizer.getSampleRate()

        var numBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
        if (numBytes == AudioRecord.ERROR || numBytes == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "getMinBufferSize returned error: $numBytes, using 2x sample rate")
            numBytes = 2 * sampleRateInHz
        }
        numBytes = max((0.2 * sampleRateInHz).toInt(), numBytes)
        numBytes = numBytes * 2 // some devices need 2x min buffer to avoid native crash/underrun
        Log.i(
            TAG, "buffer size in bytes: $numBytes, ms: ${numBytes * 1000.0f / sampleRateInHz}"
        )

        val sourcesToTry = intArrayOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT
        )
        for (source in sourcesToTry) {
            try {
                audioRecord?.release()
                audioRecord = null
                audioRecord = AudioRecord(
                    source,
                    sampleRateInHz,
                    channelConfig,
                    audioFormat,
                    numBytes * 2
                )
                if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    Log.i(TAG, "AudioRecord initialized with source $source")
                    return true
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "AudioRecord SecurityException (source=$source)", e)
            } catch (e: Exception) {
                Log.e(TAG, "AudioRecord failed (source=$source)", e)
            }
        }
        Log.e(TAG, "AudioRecord failed with all sources")
        audioRecord?.release()
        audioRecord = null
        return false
    }


    @Suppress("deprecation")
    private fun checkHeadset() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val am = getSystemService(AUDIO_SERVICE) as AudioManager

        if (adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED) {
            Handler().postDelayed({
                am.startBluetoothSco()
                am.isMicrophoneMute = true
            }, 1000)
        } else {
            am.stopBluetoothSco()
            if (am.isWiredHeadsetOn) {
                am.setMicrophoneMute(true);
            } else {
                am.setMicrophoneMute(false);
            }
        }
    }

}
