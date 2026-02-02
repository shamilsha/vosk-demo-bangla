# Vosk Demo Bengali – Problem Summary and Fixes

## Symptoms

1. **App closed when tapping "Start Microphone" or "Start File"** – The app would crash or close immediately when either button was pressed, even when the speech recognition part was bypassed for testing.
2. **Permission dialog not showing or confusing** – The app either did not show the microphone permission prompt, or showed "Permission denied" on startup before the user had tapped Start Microphone, even after granting "While using this app."
3. **"Recording .. (no recognizer)"** – After fixing the crash, the app recorded but did not run the recognizer because it was intentionally disabled for debugging.

---

## Root Causes and Fixes

### 1. Permission flow: "Can request only one set of permissions at a time"

**Problem:**  
In `onCreate`, the app called `askPermission(permList)` up to three times in a row (once for RECORD_AUDIO, once for WRITE_EXTERNAL_STORAGE, once for INTERNET) if each was not granted. Android only allows **one** permission request at a time. The second and third calls were ignored, so the user might never see the microphone (or other) dialog, and logcat showed:

```text
W/Activity: Can request only one set of permissions at a time
```

**Fix:**  
- Introduced `requestNextNeededPermission()` that requests **one** permission at a time: it finds the first permission in the list that is not granted and calls `ActivityCompat.requestPermissions(this, arrayOf(p), PERMISSION_CODE)`.
- `onCreate` now calls `requestNextNeededPermission()` once.
- In `onRequestPermissionsResult`, after the user allows or denies, we call `requestNextNeededPermission()` again (only when granted) to request the next missing permission, and so on until all are handled.

---

### 2. Permission result handling: crash / wrong behavior

**Problem:**  
- `onRequestPermissionsResult` used `grantResults[1]` even when only one permission had been requested, which could cause an `ArrayIndexOutOfBoundsException`.
- When the user denied a permission, the code called `askPermission(permList)` again (requesting all three at once), which triggered the same "one set at a time" issue.

**Fix:**  
- Handle only the first result: `permissions[0]` and `grantResults[0]`, since we now request one permission at a time.
- When denied, we no longer re-request all permissions; we only show a toast for microphone denial (see below).
- Removed the unused `askPermission(permissions)` method and replaced any remaining use with `requestNextNeededPermission()` or a single-permission request (e.g. RECORD_AUDIO when tapping Start Microphone without permission).

---

### 3. Permission check: "While using this app" not recognized as granted

**Problem:**  
The app used `ContextCompat.checkSelfPermission(applicationContext, permission)` to check if a permission was granted. On some devices, the **Activity** is the context that receives the runtime permission result. Using `applicationContext` could make the check return "not granted" even after the user chose "While using this app."

**Fix:**  
- Use the Activity context for the check: `ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED`.
- When the user taps Start Microphone without mic permission, we now request only RECORD_AUDIO (not the full list) and show a clear toast: "Microphone permission required – please allow when prompted."

---

### 4. Misleading "Permission denied" on startup

**Problem:**  
If the user denied **Storage** or **Internet** when the app requested them one-by-one on startup, the app still showed a generic "Permission denied; some features may not work." toast. That made it look like something was wrong before the user had even tapped Start Microphone, even though microphone was already allowed and the app would work.

**Fix:**  
- Show the "Permission denied" toast **only** when the user denies **RECORD_AUDIO** (microphone), with a specific message: "Microphone permission denied; enable it in Settings to use Start Microphone."
- Do **not** show any toast when the user denies Storage or Internet on startup.
- Keep showing "Microphone allowed" when the user grants the microphone permission.

---

### 5. Recognizer disabled for debugging

**Problem:**  
To isolate the crash, `SKIP_RECOGNITION_FOR_MIC` had been set to `true`, so the app recorded audio but did not run the Sherpa-ONNX recognizer. The UI showed "recording .. (no recognizer)" and no recognized text.

**Fix:**  
Set `SKIP_RECOGNITION_FOR_MIC = false` so that when the user taps Start Microphone, the recognizer runs and Bengali speech recognition works as intended.

---

## Additional improvements made during debugging

- **Defensive try/catch** – Wrapped model initialization, `onDemo()`, `onDemoFile()`, `AudioRecord.startRecording()`, `AudioRecord.read()`, and file processing in try/catch so Java exceptions are caught and shown via Toast instead of crashing.
- **Logging** – Added `Log.i`/`Log.d` at key points (e.g. `MIC_BUTTON_CLICK`, `onDemo()`, `initMicrophone()`, `checkForPermission()`, `onPermissionResult()`) to trace flow and permission state in logcat.
- **Model guard** – Used a `modelLoaded` flag and checks so the recognizer is only used after the model has loaded successfully.
- **Log capture script** – Added `capture-crash-log.ps1` to clear logcat and stream full logcat to a timestamped file for capturing native crashes and backtraces.

---

## Summary table

| Symptom                          | Cause                                              | Fix                                                                 |
|----------------------------------|----------------------------------------------------|---------------------------------------------------------------------|
| App closes on Start Mic/File     | Addressed via permission fixes and defensive code  | One-at-a-time permissions, correct permission check, try/catch      |
| Permission dialog not showing    | Multiple permission requests at once               | `requestNextNeededPermission()` – request one permission at a time  |
| "Permission denied" on startup   | Toast shown for any denied permission              | Show "denied" toast only when RECORD_AUDIO is denied                |
| "While using" not recognized     | Checking with `applicationContext`                 | Check with Activity context (`this`)                                |
| "Recording (no recognizer)"     | Recognizer turned off for debugging                | Set `SKIP_RECOGNITION_FOR_MIC = false`                              |

---

## Files modified

- **`app/src/main/java/com/alphacephei/vosk/MainActivity.kt`**  
  Permission flow, permission check context, toast logic, recognizer flag, try/catch, and logging.

- **`capture-crash-log.ps1`** (if present)  
  Script to capture logcat for crash analysis.

---

*Document generated from troubleshooting session. App: Vosk Demo Bengali (Sherpa-ONNX, Kotlin).*
