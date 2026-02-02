# How to Capture the Crash When "Start Microphone" Closes the App

The app may close (crash) when you tap **Start Microphone**. To find the **exact cause**, capture logcat **while** you reproduce the crash.

## Steps

1. **Connect the device via USB** and enable USB debugging.

2. **Start logcat with a filter for this app** (run in a terminal **before** tapping Start Microphone):

   ```bash
   adb logcat -c
   adb logcat -s "AndroidRuntime:E" "com.alphacephei.vosk:V" "*:E" > logcat-crash.txt
   ```

   Or on Windows PowerShell, to see output and save at the same time:

   ```powershell
   adb logcat -c
   adb logcat -v time *:E AndroidRuntime:E com.alphacephei.vosk:V 2>&1 | Tee-Object -FilePath logcat-crash.txt
   ```

3. **Tap "Start Microphone"** in the app so it crashes.

4. **Stop logcat** (Ctrl+C). Open `logcat-crash.txt` and look for:

   - **FATAL EXCEPTION** – Java crash (stack trace shows the line that failed).
   - **signal 11 (SIGSEGV)** or **backtrace** – native (JNI/C++) crash, often from missing `.so` or Sherpa-ONNX.
   - **Process: com.alphacephei.vosk** – confirms the crash is from this app.

5. **If the app closes with no Java exception**, it is likely a **native crash**. Then run:

   ```bash
   adb logcat -c
   adb logcat -v time 2>&1 | Tee-Object -FilePath logcat-full.txt
   ```

   Reproduce the crash, stop with Ctrl+C, and search `logcat-full.txt` for `alphacephei`, `FATAL`, `signal`, `backtrace`, or `tombstone`.

---

**Summary:** Capture logcat **while** you tap Start Microphone and the app closes, then check the log file for `FATAL EXCEPTION` or `signal`/`backtrace` to see why it closed.
