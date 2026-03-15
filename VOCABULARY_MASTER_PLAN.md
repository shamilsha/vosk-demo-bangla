# Vocabulary Master List & Lesson Vocabulary — Plan

This document outlines a plan to add a **master vocabulary list** (with word type and user “known” state) and **per-lesson vocabulary** that gates or prompts practice for unknown words before any lesson. The behavior should apply to **all lessons** in the app.

---

## 1. Goals

- **Master vocabulary list:** One central list of words/phrases used in the app.
- **Categorization:** Each entry has a **type**: noun, verb, pronoun, adjective, auxiliary verb, phrase, etc.
- **User progress:** Each entry is marked per user as **known** or **not yet known**.
- **Per-lesson vocabulary:** Every lesson has an associated list of words/phrases.
- **Gating (optional but recommended):** When opening a lesson, if the lesson uses words the user does **not** know, the app prompts (or requires) the user to **practice those words first**, then allows continuing to the lesson.
- **Applicability:** Same logic for all lesson types (conversation bubbles, 3-col table, SV ribbon, simple sentence, legacy, pronunciation, etc.).

---

## 2. Data Model

### 2.1 Master vocabulary entry

Each item in the master list is a **vocabulary entry** with at least:

| Field       | Description |
|------------|-------------|
| **id**     | Stable unique ID (e.g. UUID or slug like `word_go`). |
| **text**   | The word or phrase in English (normalized, e.g. lowercase for words). |
| **type**   | Category: noun, verb, pronoun, adjective, adverb, auxiliary_verb, phrase, other. |
| **normalized** | Optional: normalized form for matching (e.g. "pick up" for "Pick up"). |

Optional later:

- Bengali equivalent(s), pronunciation, or link to existing lesson rows (for “show in context”).
- Difficulty or frequency (for ordering practice).

### 2.2 Word types (taxonomy)

Suggested enum or constants:

- **noun**
- **verb**
- **pronoun**
- **adjective**
- **adverb**
- **auxiliary_verb** (e.g. is, have, do, will)
- **phrase** (multi-word: “pick up”, “nice to meet you”)
- **other** (fallback)

You can add more (e.g. determiner, preposition) as needed.

### 2.3 User “known” state

- Stored **per device** (or per user account if you add auth later).
- For each vocabulary **id** (or normalized text): **known** (true) or **unknown** (false / not present).
- Persistence: **SharedPreferences**, **SQLite**, or a **JSON file** in `filesDir`. SQLite is better if the master list grows large or you want to query by type/lesson.

---

## 3. Where the master list lives

**Options:**

- **A. Single asset file**  
  e.g. `assets/vocabulary/master.json` or `assets/vocabulary/master.txt` (CSV/TSV).  
  - Pros: Simple, versioned with app.  
  - Cons: Updates require app update unless you add a “download updated list” path later.

- **B. SQLite database in assets**  
  One DB shipped with the app, copied to `filesDir` on first run; user progress in the same DB or a second one.  
  - Pros: Fast lookup, easy to extend.  
  - Cons: Slightly more setup.

- **C. Generated from lessons + manual overlay**  
  Script extracts all distinct words/phrases from lesson assets into a candidate list; you add types (and merge duplicates) in a separate file or DB.  
  - Pros: Master list stays in sync with lesson content.  
  - Cons: Need robust tokenization and phrase detection.

**Recommendation:** Start with **A (single asset file)** for the master list and a **simple persistence** (e.g. SharedPreferences or JSON in `filesDir`) for “known” flags. Migrate to **B** if the list or queries grow.

---

## 4. Per-lesson vocabulary

Each lesson must have a **vocabulary list** that can be compared to the master list.

### 4.1 How to get the list

- **Option 1 — From lesson content (on the fly):**  
  When loading a lesson, parse the lesson file (or in-memory rows) and extract English words/phrases.  
  - Pros: No extra files; always in sync with content.  
  - Cons: Need extraction logic per lesson type; phrases and normalization are trickier.

- **Option 2 — Stored per lesson:**  
  Each lesson has an associated file or DB table: e.g. `Lessons/Conversation/vocabulary_conversation_1.txt` or a row in a “lesson_vocabulary” table.  
  - Pros: Full control (include phrases, multi-word); same format for every lesson type.  
  - Cons: Must maintain these lists when lesson content changes.

- **Option 3 — Hybrid:**  
  Default: extract from content; allow override or append via a small “extra vocabulary” file per lesson.

**Recommendation:** **Option 2 or 3.** For “applicable to all lessons”, a **uniform** lesson vocabulary list (one list per lesson key) is easier: same comparison and gating logic everywhere. Extraction can be added later for auto-generation or for lessons that don’t have a list yet.

### 4.2 Lesson vocabulary file format (if using files)

Example per lesson:

- **Option A:** One file per lesson: `vocabulary_<lessonKey>.txt` — one word/phrase per line, optionally with type if you want to override master.
- **Option B:** One manifest file mapping lesson key → list of vocabulary IDs or normalized texts that reference the master list.

Example (one word/phrase per line):

```text
# vocabulary_conv_bubble_first_meeting.txt
What is your name
My name is
Nice to meet you
live
```

Normalization (lowercase, trim) and phrase handling (multi-word lines) can be defined once and reused.

---

## 5. Comparing lesson vocabulary to master and user state

- **Input:** Lesson key (or lesson content) → **list of vocabulary items** (strings or IDs).
- **Steps:**
  1. Resolve each item to the **master list** (by normalized text or ID). If an item is not in the master list, you can either add it on the fly (with type “other”) or treat it as “unknown” and still prompt practice.
  2. For each resolved master entry, look up **user known state**.
  3. **Unknown set** = lesson vocabulary that is either not in master or not marked known by the user.

This gives you the set of “new to this user” words/phrases for that lesson.

---

## 6. Flow: “Practice unknown words first”

- **Trigger:** User selects a lesson (drawer subtopic or any entry point that opens a lesson).
- **Before** opening the lesson UI (conversation bubbles, 3-col, etc.):
  1. **Resolve** lesson vocabulary (from stored list or extracted from content).
  2. **Compare** with master list and user known state → list of **unknown** words/phrases.
  3. **If unknown list is empty:** proceed to load the lesson as today.
  4. **If unknown list is non-empty:**  
     - Show a **vocabulary gate** screen/dialog: e.g. “This lesson uses X words you haven’t learned yet: [list]. Practice them first?” with actions: **[Practice]** and **[Skip and continue]** (or **[Practice]** only if you want strict gating).
  5. **If user taps Practice:**  
     - Open a **vocabulary practice** flow (see below) for that set of words.  
     - When done (or cancelled), optionally mark practiced items as “known” or “practiced once”.  
     - Then either automatically open the lesson or return to the gate and show **[Continue to lesson]**.
  6. **If user taps Skip:**  
     - Optionally record “skipped” for analytics; then open the lesson without marking words as known.

You can make “Skip” optional (e.g. only for certain lesson types or after first time).

---

## 7. Vocabulary practice flow (for unknown words)

A small, reusable flow so the user can “practice this word/phrase first”:

- **Input:** List of vocabulary items (from master: id, text, type).
- **Options:**
  - **A. Minimal:** Show word/phrase (English); user taps “I know it” → mark known and remove from list; or “Practice” → TTS speaks it, user repeats (reuse existing recognition + match). After N correct or “I know it”, mark known.
  - **B. Flashcard-style:** Show English (and optionally Bengali from master or lesson); user says the English (or Bengali); app checks with existing recognizer; correct → next; wrong → retry or show correct (similar to current lesson flows).
  - **C. Reuse Translation Practice:** If your existing “Translation Practice” already shows Bengali → user says English (or vice versa), you can pass the unknown set as the word list and open that screen; on completion, mark those words as known.

**Recommendation:** Start with **A or C**. Reuse existing TTS + recognition + “Correct”/“Incorrect” so you don’t duplicate logic. A dedicated “Vocabulary drill” screen that takes a list of words and runs the same verification as one of your current lesson types is enough for “practice first.”

---

## 8. Applicable to all lessons

To make this **applicable to all lessons**, every entry point that opens a lesson should go through the same gate.

### 8.1 Single entry point

- **Central function:** e.g. `prepareAndOpenLesson(lessonKey, lessonType, ...)` (or similar).  
  - Resolves lesson vocabulary for `lessonKey` (and lesson type if needed).  
  - Compares with master + user known state.  
  - If unknown set non-empty → show vocabulary gate → after practice/skip → call existing loader (e.g. `loadConversationBubbleLesson`, `loadThreeColLessonFromAsset`, etc.).

- All places that currently load a lesson (drawer subtopic, deep link, etc.) should call this central function instead of loading the lesson directly.

### 8.2 Lesson vocabulary per lesson type

Each lesson type must be able to answer: **“What is the vocabulary list for this lesson?”**

| Lesson type            | How to get vocabulary list |
|------------------------|-----------------------------|
| Conversation bubbles   | From `conversationBubbleLessonAssetPaths` → load file → parse rows → extract English (and optional phrase chunks); or from `vocabulary_conv_bubble_<key>.txt`. |
| 3-col table            | From `threeColLessonAssetPaths` → rows → English column; or from `vocabulary_threecol_<key>.txt`. |
| Simple sentence        | From lesson file or `vocabulary_simple_<key>.txt`. |
| SV ribbon / conveyor   | From sentence/word list used for that lesson; or `vocabulary_sv_<key>.txt`. |
| Legacy / pipe-separated| From lesson rows (English columns); or per-lesson vocabulary file. |
| Pronunciation          | From word list for that lesson; or `vocabulary_pron_<key>.txt`. |

If you use **stored per-lesson lists** (Option 2 or 3), the format is the same for all: e.g. one file per lesson key in a known folder, or one DB table `lesson_vocabulary(lesson_key, vocabulary_id)`.

---

## 9. Implementation phases (suggested)

### Phase 1 — Foundation

- Define **word type** enum/constants.
- Define **master vocabulary** format and load it from one asset file (e.g. JSON or CSV).
- Implement **user known state** (persist by vocabulary id or normalized text).
- **No UI yet:** only data structures and persistence.

### Phase 2 — Lesson vocabulary

- Define **lesson vocabulary** format (e.g. one file per lesson: `vocabulary_<lessonKey>.txt` in a known path).
- Implement **resolution**: lesson key → list of words/phrases → match to master (by normalized text or id).
- Implement **unknown set**: lesson vocabulary minus user-known.

### Phase 3 — Gate and practice

- Add **vocabulary gate** screen/dialog (before lesson): show unknown list, [Practice] and [Skip].
- Add **vocabulary practice** flow (reuse TTS + recognition); on success or “I know it”, mark as known.
- **Central entry point:** one function that runs “resolve lesson vocab → unknown set → gate → load lesson” and call it from drawer (and any other lesson launcher).

### Phase 4 — Wired to all lessons

- For each lesson type, either:
  - Add a `vocabulary_<key>.txt` (or DB rows) for each lesson, **or**
  - Implement extraction from lesson content and (optionally) merge with a small override file.
- Ensure every lesson open path goes through the central entry point.

### Phase 5 — Polish and optional features

- Settings: “Require vocabulary practice before lessons” (strict vs allow skip).
- Master list: add types for all entries (manual or script).
- Optional: “Vocabulary” screen to browse master list and mark words known/unknown.
- Optional: progress stats (e.g. “You know 120 of 500 words”).

---

## 10. File / storage sketch

- **Assets:**  
  - `vocabulary/master.json` (or `master.txt`) — id, text, type, normalized.  
  - `vocabulary/lessons/<lessonKey>.txt` — one word/phrase per line for that lesson (optional; can derive from content later).

- **App storage (filesDir or SharedPreferences):**  
  - `vocabulary_known.json` or SharedPreferences keys `vocab_known_<id>` — set of vocabulary ids (or normalized texts) the user knows.

- **Code:**  
  - `VocabularyRepository` (or similar): load master, load/save user known state, resolve lesson key → lesson vocabulary, compute unknown set.  
  - `VocabularyGateActivity` or fragment: show unknown list, [Practice] / [Skip].  
  - `VocabularyPracticeFragment` or reuse existing practice layout: list of words, TTS + recognition, mark known.

---

## 11. Summary

| Item | Recommendation |
|------|----------------|
| Master list | Single asset file (e.g. JSON); each entry: id, text, type, normalized. |
| User known state | Persist in SharedPreferences or JSON in filesDir (later SQLite if needed). |
| Lesson vocabulary | Stored list per lesson (e.g. `vocabulary_<lessonKey>.txt`) for uniformity; optional extraction from content later. |
| Word types | noun, verb, pronoun, adjective, adverb, auxiliary_verb, phrase, other. |
| Flow | Before opening any lesson → resolve vocab → unknown set → gate (Practice / Skip) → practice flow if needed → then load lesson. |
| Practice flow | Reuse TTS + recognition; mark known when user succeeds or taps “I know it”. |
| Applicable to all lessons | One central “prepare and open lesson” path; each lesson type supplies vocabulary list for its key. |

This plan keeps the master list and “known” state clear, makes per-lesson vocabulary consistent across lesson types, and uses one gate and one practice flow so the behavior is the same for every lesson. You can implement in the phases above and adjust (e.g. strict vs skip, or extraction vs stored lists) as you go.
