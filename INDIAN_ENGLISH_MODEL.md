# Using Indian English for English Speech Recognition

## Can I use vosk-model-small-en-in-0.4 in this app?

The [Vosk models page](https://alphacephei.com/vosk/models) lists **vosk-model-small-en-in-0.4** (36MB) – a lightweight Indian English model for mobile (Apache 2.0).

**Important:** This app uses **Sherpa ONNX** for Bengali (ONNX files: `encoder.onnx`, `decoder.onnx`, `joiner.onnx` in `model-bn/`). The Indian English model from alphacephei is in **classic Vosk/Kaldi** format (different runtime and file layout). So you **cannot** drop that model into assets and use it with the current Sherpa ONNX recognizer – the formats are incompatible.

## Options

### Option 1: Use Android’s Indian English locale (implemented)

For “English” input and verification, the app can use the **system SpeechRecognizer** with **English (India)** locale (`en-IN`). Many devices support this; recognition then uses the device’s Indian English engine when available. This does **not** use the Vosk Indian English model, but gives Indian English behaviour without adding a second recognition stack.

### Option 2: Use the actual Vosk Indian English model (separate integration)

To use **vosk-model-small-en-in-0.4** you need the **Vosk Android SDK** (Kaldi-based), not Sherpa ONNX:

1. Add the [Vosk Android API](https://github.com/alphacep/vosk-api/tree/master/android) (e.g. the `vosk-android-demo` or the AAR) to your project.
2. Download and unpack [vosk-model-small-en-in-0.4](https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip) into `app/src/main/assets/` (e.g. `model-en-in/`).
3. Implement a **second recognizer** in your app: one for Bengali (current Sherpa ONNX) and one for Indian English (Vosk). When the user is in English mode or in verification, route audio to the Vosk Indian English recognizer instead of `SpeechRecognizer`.

So: **yes, it is possible** to use the Indian English model, but it requires adding the Vosk Android SDK and a second recognition path; it is not a drop-in for the current Sherpa ONNX setup.

## References

- [Vosk models](https://alphacephei.com/vosk/models) – Indian English: `vosk-model-small-en-in-0.4` (36M), `vosk-model-en-in-0.5` (1G).
- [Vosk Android](https://alphacephei.com/vosk/android) – how to integrate Vosk on Android.
