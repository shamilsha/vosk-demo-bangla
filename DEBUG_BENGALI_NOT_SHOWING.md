# Analysis: Why Bengali Sentence Is Not Shown in the Upper Text Area

## Summary

**Root cause:** The Bengali Vosk model loads **asynchronously** in the background. When it finishes loading, the code **unconditionally** sets the upper text area (`my_text`) to the string `"Ready"`. If you load an SVO lesson (e.g. "SVO Eat") **before** or **around the same time** as the model finishes loading, the lesson correctly sets the Bengali text in the upper box—but then the **model-load completion callback runs** and overwrites it with `"Ready"`. So you end up seeing **"Ready"** (or an empty box if timing differs) instead of the Bengali sentence.

---

## 1. Layout and View IDs (Verified)

- **Upper box:** `android:id="@+id/my_text"` — label above it: "Bengali meaning" (`bengali_label`).
- **Lower box:** `android:id="@+id/english_text"` — label: "English meaning" (`translation_label`).
- In code, `textView = findViewById(R.id.my_text)` and `englishTextView = findViewById(R.id.english_text)`.
- There is only one `activity_main.xml` (no layout variants), so the same hierarchy is always used.

So the **correct** view is being targeted for the Bengali text.

---

## 2. SVO Load Flow (Correct)

When you tap **Load** → **"SVO Eat (Bengali, English)"**:

1. Asset `Lessons/SVO_eat.txt` is read with **UTF-8**.
2. `parseSvoLessonFile(content)` returns `(topic, rows)`. For each line after the first, the part **before** the first comma is `firstPart` (Bengali), **after** is `secondPart` (English). `LessonRow(secondPart, firstPart, secondPart, firstPart)` so `row.bnQ = firstPart` (e.g. `"আমি ভাত খাই।"`).
3. `clearBothTextAreas()` runs → `textView.setText("")`, `englishTextView.setText("")`.
4. `onNextLessonStep()` runs:
   - `row = rows[lessonIndex]` (first row).
   - For `lessonMode == 4`: `bengaliBox.text = row.bnQ`, `englishTextView.setText(makeSvoSpannable(row.engA))`.
5. Toast: "Lesson loaded... (N pairs)".

So at the end of this sequence, the **upper box** is set to the Bengali sentence and the **lower** to the colored English. The parser and assignment are correct.

---

## 3. What Overwrites the Upper Box

The only place that sets the **upper** box to a **non–lesson** string after the UI is set up is the **model load completion** in `onCreateSpotter()`:

```kotlin
// MainActivity.kt, ~line 670
private fun onCreateSpotter() {
    recognizer = Recognizer()
    CoroutineScope(Dispatchers.IO).launch {
        modelStatus = ModelStatus.MODEL_STATUS_START
        try {
            withContext(Dispatchers.IO) { recognizer.initModel(this@MainActivity) }
            modelLoaded = true
            runOnUiThread {
                modelStatus = ModelStatus.MODEL_STATUS_READY
                micButton.isEnabled = true
                textView.text = getText(R.string.ready_to_start)   // <-- OVERWRITES UPPER BOX
            }
        } catch (e: Throwable) { ... }
    }
}
```

- `onCreateSpotter()` is called from `onCreate()`, so the Bengali model starts loading at app start.
- Load can take several seconds (device- and model-dependent).
- When it finishes, `runOnUiThread { ... textView.text = getText(R.string.ready_to_start) }` runs **unconditionally**.
- So:
  - If you load "SVO Eat" and the model is still loading, the upper box is correctly set to the Bengali sentence.
  - When the model load completes **after** that, this callback runs and sets the upper box to **"Ready"**, replacing the Bengali.

So the Bengali is set correctly, then **overwritten** by the model-ready callback.

**Why only Bengali disappears and not English?** The overwriting code does only `textView.text = getText(R.string.ready_to_start)`. That updates **only** the upper box (`textView` = `my_text`). It never touches `englishTextView` (the lower box). So the "clear" is not a generic clear-both; it is a single assignment to the upper box. That is why only the Bengali sentence disappears and the English sentence stays visible.

---

## 4. Other Places That Set `textView` (None Explain Missing Bengali)

- **setupUI()** (onCreate): sets `textView` to hint or "Ready" only at startup; after that, the next thing that runs is SVO load when you tap it.
- **clearBothTextAreas()**: sets `textView.setText("")`; we call it **before** `onNextLessonStep()`, so the next assignment is the Bengali in step 4 above.
- **handleVerificationResult()**: when showing incorrect/correct it sets `textView.text = r.bnQ` (or `r.bnA` for mode 1) to **restore** the lesson’s Bengali in the upper box. So it does not replace Bengali with something else.
- **TTS onDone("lesson_verify")**: we re-apply `findViewById<TextView>(R.id.my_text).text = rows[idx].bnQ` before starting the mic, which is correct.
- **Vosk partial results**: when **not** in verification mode we set `textView.text = text` (partial recognition). During SVO verification we **are** in verification mode, so only the lower box is updated there.

So the only **unconditional overwrite** of the upper box with a fixed string after you’ve set the lesson is the **model load callback** above.

---

## 5. Conclusion and Fix (Applied)

- **Why Bengali doesn’t show:** The upper box is set to the Bengali sentence when you load the lesson, but the **Bengali Vosk model load completion** callback later sets the same upper box to **"Ready"**. So the Bengali is replaced soon after (or the model was already ready at startup and `setupUI()` set "Ready" before the user loaded).
- **Fix applied in code:**
  1. **Model load callback** (`onCreateSpotter()` success): Only set the upper box to `"Ready"` when `lessonRows == null`. When a lesson is active (`lessonRows != null` and `lessonMode == 4`), **restore** the current row’s Bengali: `findViewById<TextView>(R.id.my_text).text = rows[lessonIndex].bnQ`.
  2. **setupUI():** When the model is already ready at startup, set `textView.text = "Ready"` only when `lessonRows == null`, so we never overwrite an already-loaded lesson.
  3. **Mode 4 display:** Set both `textView` and `findViewById<TextView>(R.id.my_text)` to the Bengali string and re-apply in a `post` so the upper box keeps showing Bengali even if something else runs first.

After rebuilding and installing, the Bengali sentence should stay visible in the upper text area.
