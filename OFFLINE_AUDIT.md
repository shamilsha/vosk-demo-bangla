# Small offline audit (post–build)

**Build/install:** Success. APK installed on device.

**Goal:** App should work fully offline once installed.

---

## 1. Speech recognition (English)

| Component | Current setting | Offline? |
|-----------|-----------------|----------|
| **English verification** (SVO, lessons, pronunciation table) | `USE_SYSTEM_SPEECH_FOR_ENGLISH_VERIFICATION = true` (MainActivity.kt ~line 92) | **No.** System `SpeechRecognizer` often uses **cloud** on many devices. |
| **Vosk Indian English** | Used when the flag above is `false` | **Yes.** Model loaded from assets/filesDir; no network. |

**Recommendation:** For full offline, set:

```kotlin
private const val USE_SYSTEM_SPEECH_FOR_ENGLISH_VERIFICATION = false
```

Then English verification (SVO “I”, lesson mode 4, pronunciation table English match) uses Vosk only. Test on device to confirm quality is acceptable.

---

## 2. Translation (ML Kit)

| Usage | Offline? |
|-------|----------|
| `Translator` (Bn↔En) | **After first download.** `downloadModelIfNeeded()` needs network **once**; then translation can work offline. |
| Used in: Translate button, “translate and speak”, sentence list (Bengali→English), lesson mode 3, etc. | If user never has internet, model never downloads and these features will show “Download model (internet needed)” or similar. |

**Recommendation:** Either:

- Rely on “one-time download when online,” and document that translation works offline after that, or  
- Bundle translation models (if supported by your ML Kit setup) so no download is required.

---

## 3. Other

| Item | Status |
|------|--------|
| **INTERNET permission** | Declared in AndroidManifest. Needed for ML Kit model download and (if kept) system speech. For “offline-only” build you could remove it only if you also remove/bundle ML Kit and use only Vosk. |
| **Content (lessons, SVO, pronunciation)** | All from **assets**; fully offline. |
| **Bengali recognition** | Sherpa/Vosk from assets; offline. |
| **TTS** | System engine; offline if the language pack (e.g. Bengali/English) is installed on device. |

---

## 4. Summary

| Area | Fully offline today? | Change for full offline |
|------|---------------------|--------------------------|
| SVO “I”, pronunciation table, lesson verification (English) | Only if `USE_SYSTEM_SPEECH_FOR_ENGLISH_VERIFICATION = false` | Set flag to `false` and use Vosk for English. |
| Translation (Bn↔En) | After one-time model download | Optional: bundle models or document “connect once to download.” |
| Rest (assets, Bengali ASR, UI) | Yes | None. |

**Single code change for best offline behavior:** Set `USE_SYSTEM_SPEECH_FOR_ENGLISH_VERIFICATION = false` so all English speech verification uses on-device Vosk.
