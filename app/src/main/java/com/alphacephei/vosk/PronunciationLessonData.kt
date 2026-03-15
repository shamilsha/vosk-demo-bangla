package com.alphacephei.vosk

/** Built-in pronunciation lessons: title → rows of [Word, Pronunciation, Meaning]. Meaning may be "". */
fun getPronunciationLessons(): List<Pair<String, List<List<String>>>> = listOf(
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
