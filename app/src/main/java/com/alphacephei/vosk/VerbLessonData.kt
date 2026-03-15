package com.alphacephei.vosk

/** Built-in verb conjugations: DO, HAVE, GO (Subject | English | Bengali). */
fun getVerbData(): Map<String, List<VerbRow>> = mapOf(
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

/** 12 tenses per (verb, subject): verb -> subject -> list of (English, Bengali). */
fun getTenseData(): Map<String, Map<String, List<VerbRow>>> {
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

/** Build lesson rows from a verb: speak Bengali, user says English, match to English. */
fun buildLessonFromVerb(verbName: String): List<LessonRow> {
    val rows = getVerbData()[verbName] ?: return emptyList()
    return rows.map { v -> LessonRow(v.english, v.bengali, v.english, v.bengali) }
}

/** Build lesson from (verb, subject): 12 tenses, speak Bengali, user says English. */
fun buildLessonFromTense(verbName: String, subjectName: String): List<LessonRow> {
    val rows = getTenseData()[verbName]?.get(subjectName) ?: return emptyList()
    return rows.map { v -> LessonRow(v.english, v.bengali, v.english, v.bengali) }
}
