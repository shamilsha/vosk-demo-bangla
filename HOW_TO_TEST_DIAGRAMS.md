# How to Test the 1-to-3 and 3-to-1 Diagrams

The app includes two HTML diagram layouts that load in the **Description** area (WebView). Use these steps to install and test them.

---

## 1. Install the app

From the project folder run:

```bash
.\gradlew installDebug
```

Or build and install from Android Studio. Ensure a device or emulator is connected.

---

## 2. Open the app and the Load menu

1. Launch **vosk-demo-bengali** on the device.
2. Tap the **folder icon** (Load lesson) in the bottom bar.
3. The **Load** dialog opens with a list of options.

---

## 3. Test the 1-to-3 diagram (Grammar Rules)

1. In the Load dialog, tap **"Diagram: 1-to-3 (Grammar Rules)"**.
2. A toast appears: *"Showing 1-to-3 diagram. Scroll down to Description."*
3. **Scroll down** on the screen to the **Description** section (below the Bengali and English meaning boxes).
4. You should see:
   - Red title bar: **"English Grammar Rules"**
   - A **2×3 grid**: each cell has a **colored oval** (pronoun: I, She, You, We, He, They) with **three arrows** to **am/is/are**, **do/does**, **have/has**.

---

## 4. Test the 3-to-1 diagram (Have/Has)

1. Tap the **folder icon** again to open the Load dialog.
2. Tap **"Diagram: 3-to-1 (Have/Has)"**.
3. Scroll down to the **Description** section.
4. You should see:
   - Blue title bar: **"The verb 'to have'"**
   - **Three rows**, each with:
     - A **rectangle** (subjects: I+You, He+She+It, We+They)
     - An **arrow**
     - A **circle** with **have** or **has**

---

## 5. Where the diagrams live in the app

- **Assets:** `app/src/main/assets/diagrams/`
  - `diagram-1to3.html` – Grammar Rules (1 pronoun → 3 verbs)
  - `diagram-3to1.html` – Have/Has (subject groups → verb form)
- **Loading in code:** `MainActivity.loadDiagramFromAssets(filename)` loads `file:///android_asset/diagrams/<filename>` into the description WebView.

---

## 6. Using these layouts from other “instructions”

Later you can drive which diagram loads from your own logic, for example:

- Lesson type or lesson file name → choose `diagram-1to3.html` or `diagram-3to1.html`.
- Call `loadDiagramFromAssets("diagram-1to3.html")` or `loadDiagramFromAssets("diagram-3to1.html")` from that logic instead of (or in addition to) the Load menu.

The same HTML files can be copied into **any Android app** and loaded in a WebView with:

```kotlin
webView.loadUrl("file:///android_asset/diagrams/diagram-1to3.html")
// or
webView.loadUrl("file:///android_asset/diagrams/diagram-3to1.html")
```
