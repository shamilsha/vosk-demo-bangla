# Vocabulary progress (0/1/2) — where and how to update in real time

The **master list** (`assets/vocabulary/master_word_list.txt`) is **read-only** at runtime. The app cannot change asset files. So the extra field (0/1/2) is **not** stored in that file.

Instead, progress is stored in **app private storage** and merged with the master list when you need it.

---

## 1. Where progress is stored

- **File:** `filesDir/vocabulary_progress.json`
- **Format:** `{ "version": 1, "progress": { "word1": 0, "word2": 1, "word3": 2 } }`
- **Key:** word in **lowercase** (same as in the master list).
- **Value:**
  - **0** = user never tried this word
  - **1** = user **passed** pronunciation test
  - **2** = user **tried but failed** pronunciation test

The app **can** read and write this file (it’s in the app’s `filesDir`), so you update it in real time when the user does a pronunciation test.

---

## 2. How to use it in the app

**When loading the master list (from assets):**

1. Read `assets/vocabulary/master_word_list.txt` (unchanged: word, category, bengali, pronunciation).
2. Call `LessonFileParsers.loadVocabularyProgress(filesDir)` → `Map<String, Int>` (word → 0/1/2).
3. For each row in the master list, the “progress” for that word is `progressMap[word.lowercase()] ?: 0`.

So you keep the master list as-is and **merge** progress in memory. No need to change the asset file.

**When the user finishes a pronunciation test for a word:**

1. Decide result: passed → 1, failed → 2.
2. Update your in-memory `Map<String, Int>`: `progress[word.lowercase()] = 1 or 2`.
3. Call `LessonFileParsers.saveVocabularyProgress(filesDir, progress)` so it persists.

Next time you load the master list and progress, that word will show 1 or 2.

---

## 3. Code reference

In `LessonFileParsers.kt`:

- **Constants:** `VOCABULARY_PROGRESS_FILE`, `VOCAB_PROGRESS_NEVER` (0), `VOCAB_PROGRESS_PASSED` (1), `VOCAB_PROGRESS_FAILED` (2).
- **Load:** `loadVocabularyProgress(filesDir: File): Map<String, Int>`
- **Save:** `saveVocabularyProgress(filesDir: File, progress: Map<String, Int>)`

You keep a single `Map<String, Int>` in memory (or reload it when needed), merge it with the master list for display/logic, and call `saveVocabularyProgress` whenever you update progress for one or more words.

---

## 4. Summary

| What            | Where                    | Writable at runtime? |
|-----------------|--------------------------|------------------------|
| Master list     | `assets/vocabulary/master_word_list.txt` | No (assets are read-only) |
| Per-word 0/1/2  | `filesDir/vocabulary_progress.json`      | Yes                    |

So you **don’t** add the 0/1/2 column to the master .txt file. You add it only in app storage and merge when loading.
