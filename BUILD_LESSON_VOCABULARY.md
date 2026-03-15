# How to Build the Prebuilt Vocabulary List for Each Lesson

Every lesson has a **prebuilt** vocabulary/phrase list. You build it **once** when the lesson is created (or via a special action). At **load time**, the app loads that list, compares it with the master list, and finds words the user has **not seen before**.

This document gives you **concrete ideas** for building that list: where to put it, what format, and three ways to generate it.

---

## 1. Where to Store the Prebuilt List

**Convention:** One file per lesson, keyed by **lesson key** (the same key used in the drawer and in `conversationBubbleLessonAssetPaths` / `threeColLessonAssetPaths`, etc.).

**Suggested paths (choose one and stick to it):**

| Option | Path pattern | Example |
|--------|------------------|---------|
| **A. Next to the lesson file** | Same folder as lesson, prefix `vocabulary_` | `Lessons/Conversation/vocabulary_conversation_1.txt` |
| **B. Central vocabulary folder** | One folder for all lesson vocab, filename = lesson key | `vocabulary/lessons/conv_bubble_first_meeting.txt` |

**Recommendation:** **B** — one folder `assets/vocabulary/lessons/` and files named by **lesson key** (e.g. `conv_bubble_first_meeting.txt`, `simple_what.txt`). That way the app only needs the lesson key to load: `vocabulary/lessons/{lessonKey}.txt`.

---

## 2. File Format for the Prebuilt List

Keep it simple so you can **hand-edit** or **script-generate**.

**One entry per line.** Optional: type override (so the lesson list can say “this is a phrase” without touching the master list).

**Minimal (recommended):**
```text
# vocabulary/lessons/conv_bubble_first_meeting.txt
# One word or phrase per line. Blank lines and # are ignored.
What is your name
My name is
Nice to meet you
live
Dhaka
What do you do
```

**With optional type (if you want to tag in the lesson file):**
```text
# word or phrase	optional_type
What is your name	phrase
name	noun
live	verb
Nice to meet you	phrase
```

**Parsing rule:** Strip leading/trailing whitespace; lowercase for matching if you want (or keep original and normalize in code). Skip empty lines and lines starting with `#`.

---

## 3. Three Ways to Build the List

### Option 1 — Manual (when you create the lesson)

When you add a new lesson:

1. Create the lesson file (e.g. `conversation_4.txt`).
2. Create the vocabulary file (e.g. `vocabulary/lessons/conv_bubble_fourth_lesson.txt`).
3. List each **word** and **phrase** that the user should know before the lesson — one per line. Include:
   - Important phrases as whole lines: `What is your name`, `My name is`, `Nice to meet you`.
   - Important single words: `name`, `live`, `Dhaka`, `do`.

**Pros:** Full control; you decide what counts as vocabulary.  
**Cons:** Slower; easy to miss a word.

---

### Option 2 — One-time script when the lesson is created (recommended)

Run a **small script** (e.g. Python) that:

1. Reads the **lesson file** (same format the app uses).
2. **Extracts** all English text (e.g. first column for conversation/3-col).
3. **Splits** into words and optionally detects **phrases** (e.g. full sentence or common chunks).
4. **Deduplicates** and writes `vocabulary/lessons/<lesson_key>.txt`.

You run this script **once per lesson** when you add or change the lesson (e.g. on your PC, then commit the generated file to assets).

**Pros:** Repeatable; same logic for all lessons of the same type; no app code for “build”.  
**Cons:** You need to run it when the lesson changes.

**Example script:** Use `scripts/build_lesson_vocabulary.py` (see repo). It parses conversation-style and 3-col-style lesson files and writes `app/src/main/assets/vocabulary/lessons/<lesson_key>.txt` with phrases and Latin-only words. You can then hand-trim or add phrases. If a lesson line has English and Bengali mixed in the first column, edit the generated file to fix that phrase.

---

### Option 3 — Special action in the app (e.g. “Build vocabulary for this lesson”)

A **button or menu action** in the app (e.g. in developer/settings, or long-press on a lesson):

1. Loads the **current lesson** content (already in memory or from asset).
2. Runs the **same extraction logic** (extract English → words/phrases → dedupe).
3. Writes the result to a file.

**Where to write:**  
- **Option A:** App’s `filesDir` (e.g. `filesDir/vocabulary/lessons/conv_bubble_first_meeting.txt`). Then you’d need to copy that file back to your project’s `assets` and commit (e.g. via ADB pull or a share/export).  
- **Option B:** Only show the result in a dialog or share intent so the user can paste into the asset file manually.

**Pros:** No separate script; can be used on device.  
**Cons:** Writing into assets from the app is not possible on a normal device; you only get a file in app storage or text to copy. So “special action” is best for **generating the list and showing it / exporting it**, then you add the file to assets on your dev machine.

---

## 4. Extraction Ideas (for script or in-app)

What to extract from each lesson type:

| Lesson type | Source | What to extract |
|-------------|--------|------------------|
| **Conversation bubbles** | `PersonA: English,Bengali,Pronunciation` | English part (first column); optionally split into words. |
| **3-col / simple** | `English,Bengali,Pronunciation` per line | English part (first column). |
| **Legacy (pipe)** | `engQ\|bnQ\|engA\|bnA` | engQ and engA (or only engA). |

**Words:** Split English on spaces and punctuation (e.g. `[ .,?!;:'"]`), then lowercase and dedupe. Drop very short tokens (e.g. length &lt; 2) if you want to skip “I”, “a”, “is” (or keep them and let the master list / type decide).

**Phrases:** Either:
- **Keep full sentences** as phrases (one line = one phrase), and optionally also output single words from the same sentence, or
- **Only single words** (no phrases), or
- **Define a phrase list** (e.g. “nice to meet you”, “what is your name”) and match those in the text first; the rest become words.

**Practical approach:**  
- Output **both** full English sentences (as phrase candidates) **and** single words (from splitting each sentence).  
- You (or a second script) can then **curate**: delete trivial words, keep important phrases.  
- Or use the script output as the prebuilt list and later refine by hand.

---

## 5. Using the provided script

Run from the project root:

```bash
python scripts/build_lesson_vocabulary.py <lesson_file> <lesson_key>
```

Examples:

```bash
# Conversation lesson
python scripts/build_lesson_vocabulary.py app/src/main/assets/Lessons/Conversation/conversation_1.txt conv_bubble_first_meeting

# 3-col / simple lesson
python scripts/build_lesson_vocabulary.py app/src/main/assets/Lessons/SVO/simple_what.txt simple_what
```

Output is written to `app/src/main/assets/vocabulary/lessons/<lesson_key>.txt`. Options: `--phrases-only`, `--words-only`, `--keep-short`, `--include-non-latin`, `--output-dir DIR`. Run it once when you create or change a lesson, then commit the file.

---

## 6. Load-time flow (reminder)

1. User selects a lesson (lesson key known).
2. App loads **prebuilt list** from `vocabulary/lessons/<lessonKey>.txt` (if missing, treat as empty or “no gate”).
3. For each entry in the list, resolve to the **master list** (by normalized text or id).
4. For each resolved entry, check **user “seen” / “known”** state.
5. **Words the user has not seen before** = entries that are in the lesson list and (not in master or not known by user). Use this set to show the vocabulary gate or to prompt practice.

This way, the prebuilt list is built **once** (manually or by script/special action); load only does comparison with master and user state.
