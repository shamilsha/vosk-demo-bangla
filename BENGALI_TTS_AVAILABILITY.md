# How to Know If Bengali TTS Voice Data Is Available on the Device

## 1. In the app (automatic)

When the app starts, it initializes Text-to-Speech and checks whether **Bengali** is available. You will see one of these toasts:

| Toast message | Meaning |
|---------------|--------|
| **Bengali voice: Available (offline)** | Bengali TTS is installed. “Speak Bengali” will use Bengali voice and work offline. |
| **Bengali voice: Not installed – install in Settings → Text-to-speech** | Bengali voice data is not on the device. You can install it from system settings (see below). Until then, the app falls back to the default language voice. |
| **Bengali voice: Not supported on this device** | The current TTS engine does not support Bengali. |
| **Bengali voice: Unknown** | Unexpected status; check logcat tag `com.alphacephei.vosk` for the numeric code. |

So you know Bengali voice data is available on the device when you see **“Bengali voice: Available (offline)”**.

---

## 2. How the app checks (code)

The app uses Android’s TextToSpeech API:

1. **`TextToSpeech.isLanguageAvailable(Locale("bn"))`**  
   Returns one of:
   - `LANG_AVAILABLE` – Bengali is available.
   - `LANG_COUNTRY_AVAILABLE` – Bengali (with country variant) is available.
   - `LANG_COUNTRY_DEFAULT` – Bengali is available and is the default for a country.
   - `LANG_MISSING_DATA` – Bengali is supported but **voice data is not installed** (user must download it).
   - `LANG_NOT_SUPPORTED` – Bengali is not supported by the TTS engine.

2. **`TextToSpeech.setLanguage(Locale("bn"))`**  
   Actually sets the language for speaking. Its return value uses the same constants. If it returns `LANG_MISSING_DATA` or `LANG_NOT_SUPPORTED`, the app falls back to the default locale and shows the “Not installed” or “Not supported” message.

So: **Bengali voice data is available** when `isLanguageAvailable(Locale("bn"))` returns `LANG_AVAILABLE`, `LANG_COUNTRY_AVAILABLE`, or `LANG_COUNTRY_DEFAULT`, and `setLanguage(Locale("bn"))` does not return `LANG_MISSING_DATA` or `LANG_NOT_SUPPORTED`.

---

## 3. How to check (and install) on the device manually

You can see which voices are installed and add Bengali from system settings:

1. Open **Settings** on the phone.
2. Go to **Language & input** (or **General management** → **Language**, depending on device).
3. Open **Text-to-speech** (or **Text-to-speech output** / **Speech**).
4. Under **Preferred engine**, select the engine (e.g. **Google Text-to-speech**).
5. Tap **Languages** (or **Install voice data** / **Download languages**).
6. Look for **Bengali** (or **বাংলা**).  
   - If it is listed and **downloaded** (checkmark or “Downloaded”), Bengali voice data **is available** on the device.  
   - If it is listed but not downloaded, tap it to **download** (one-time, may need Wi‑Fi). After that, Bengali TTS will work offline.
7. If Bengali is not in the list, the engine may not support it; try another engine if the device has more than one.

After installing Bengali, restart the app (or reopen it); it will run the check again and show **“Bengali voice: Available (offline)”** if the engine now reports Bengali as available.

---

## 4. Summary

- **In the app:** You know Bengali voice data is available when you see the toast **“Bengali voice: Available (offline)”** at startup.
- **In code:** Bengali is considered available when `TextToSpeech.isLanguageAvailable(Locale("bn"))` returns `LANG_AVAILABLE`, `LANG_COUNTRY_AVAILABLE`, or `LANG_COUNTRY_DEFAULT`, and `setLanguage(Locale("bn"))` succeeds.
- **On the device:** Check **Settings → Language & input → Text-to-speech → Languages** (or equivalent) and ensure **Bengali** is listed and downloaded.
