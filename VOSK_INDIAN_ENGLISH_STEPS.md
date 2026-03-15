# Steps to Add Vosk Indian English Model

This guide lists the steps to add **vosk-model-small-en-in-0.4** (Indian English) to your app so English input and verification use Vosk instead of the system SpeechRecognizer.

---

## Step 1: Add Vosk Android dependency

In **`app/build.gradle`**, add:

```gradle
dependencies {
    // ... existing deps ...
    implementation 'com.alphacephei:vosk-android:0.3.75'
}
```

Sync the project. The library uses JNA; Gradle will pull it in.

**Reference:** [Vosk Android](https://alphacephei.com/vosk/android), [Maven Central](https://central.sonatype.com/artifact/com.alphacephei/vosk-android)

---

## Step 2: Download and add the Indian English model

1. **Download** the model:  
   [vosk-model-small-en-in-0.4.zip](https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip) (~36 MB).

2. **Unzip** the archive. You should get a folder (e.g. `vosk-model-small-en-in-0.4`) containing:
   - `am/` (acoustic model)
   - `graph/`
   - `conf/`
   - etc.

3. **Put the model in the app:**
   - **Option A (simplest):** Copy the **contents** of that folder into  
     `app/src/main/assets/model-en-in/`  
     (so you have `app/src/main/assets/model-en-in/am/`, `model-en-in/graph/`, etc.).  
     This increases APK size by ~36 MB.
   - **Option B (smaller APK):** Do not put the model in assets. In Step 4, add a “download model on first run” flow: download the zip, unzip to `getFilesDir()/model-en-in/`, then load from that path. Document the download URL in the app or README.

4. **Rebuild** so assets are packaged (if using Option A).

---

## Step 3: Copy model from assets to internal storage (if using Option A)

Vosk’s Android API often expects a **file path** on disk, not an asset path. On first launch (or when the English model is first used):

1. Check if `getFilesDir()/model-en-in/` (or similar) already exists.
2. If not, copy from `assets/model-en-in/` to that directory (e.g. file-by-file or by unzipping an asset zip).
3. Use this path (e.g. `File(filesDir, "model-en-in")`) when loading the model in Step 4.

If you use Option B (download at runtime), the model is already on disk after download; skip asset copy and use the path where you extracted it.

---

## Step 4: Create a Vosk Indian English recognizer wrapper

1. **Check the Vosk Android API** in your project (from `com.alphacephei:vosk-android`). Typical classes:
   - `org.vosk.Model` – load from path (string).
   - `org.vosk.Recognizer` – create from `Model`, sample rate (e.g. 16000).
   - `recognizer.acceptWaveform(byte[] or short[])` – feed PCM.
   - `recognizer.getResult()` / `recognizer.getFinalResult()` – get JSON with `"text"`.

2. **Create a wrapper class** (e.g. `VoskEnInRecognizer.kt`) that:
   - Loads the model from the path you got in Step 3 (background thread).
   - Exposes a method like `processSamples(buffer: ShortArray): Pair<String, Boolean>` (text + whether it’s final).
   - Uses 16 kHz mono PCM (same as your Sherpa pipeline).
   - Handles `acceptWaveform` and result parsing (JSON with `"text"`).
   - Runs on a background thread so it doesn’t block the UI.

3. **Lifecycle:** Create/destroy the Vosk `Model` and `Recognizer` when entering/leaving the screen or when switching language, and reuse the same instance while “English (Vosk)” is active.

---

## Step 5: Use the same mic path for English as for Bengali

Right now:
- **Bengali:** `AudioRecord` → Sherpa ONNX (`recognizer.processSamples()`).
- **English:** Android `SpeechRecognizer` (system handles the mic).

To use Vosk for English:

1. **Reuse the same `AudioRecord` pipeline** you use for Bengali (same sample rate, buffer size, etc.), but in “English” mode feed the PCM to your **Vosk Indian English wrapper** instead of Sherpa.
2. **Replace or branch `startEnglishMic()`:**  
   Instead of starting `SpeechRecognizer`, start your recording thread and pass each PCM chunk to the Vosk wrapper; when you get a final result, run the same “verification” or “add to list” logic you currently run in `SpeechRecognizer`’s `onResults`.
3. **Threading:** Run Vosk `acceptWaveform` and `getResult()` on a background thread (e.g. the same thread that currently runs `processSamples` for Sherpa), and post results to the main thread for UI and verification.

---

## Step 6: Wire “English” mode to Vosk

1. **Model load:** On first use of English (or at app start), ensure the Indian English model is loaded (Step 3 + Step 4). Show a “Loading…” state if needed.
2. **When user selects “English” and taps mic:**  
   Call your new “start English mic” path that uses `AudioRecord` + Vosk (Step 5), not `SpeechRecognizer`.
3. **Verification mode (lesson):** When the app is waiting for the user to speak the English word, use the same Vosk English path (same `AudioRecord` + Vosk pipeline) and pass the recognized text into your existing “compare with expected / correct / incorrect” logic.
4. **Fallback:** Optionally keep a fallback to `SpeechRecognizer` if Vosk model fails to load (e.g. no space for model) or for older devices; you can hide this behind a setting or use it only on error.

---

## Step 7: Testing and cleanup

1. **Test:**  
   - English input mode: speak and confirm text appears.  
   - Lesson/verification: speak the expected word and confirm Correct/Incorrect.  
   - Bengali mode unchanged (still Sherpa).
2. **ProGuard:** If you use minification, keep Vosk/JNA classes (add rules if the library doesn’t ship them).
3. **APK size:** If you shipped the model in assets (Option A), APK will be ~36 MB larger unless you use App Bundles and optional delivery.

---

## Summary checklist

- [ ] Add `com.alphacephei:vosk-android:0.3.75` in `app/build.gradle`.
- [ ] Download and add Indian English model (assets **or** download at runtime).
- [ ] Copy model to internal storage if using assets; have a single “model path” for loading.
- [ ] Implement Vosk wrapper (load Model, Recognizer, `acceptWaveform`, result parsing, 16 kHz mono).
- [ ] Use `AudioRecord` + Vosk for “English” mic and verification instead of `SpeechRecognizer`.
- [ ] Test English and Bengali flows; add fallback or error handling if needed.

Once these are done, the app will use the Vosk Indian English model for English input and for lesson verification. If you tell me which step you’re on (e.g. “Step 4” or “Vosk wrapper”), I can give concrete code snippets for your project structure.
