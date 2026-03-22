# Lesson / Topic / Subtopic Architecture (and Test Layout)

This document explains how the drawer’s **topics** and **subtopics** drive which **content layout** is shown and how **lesson data** is loaded, with **Test layout** as the main example.

---

## 1. High-level flow

```
Drawer (UI)          →    Topic / Subtopic (data)    →    Content layout (UI) + Lesson data
─────────────────────────────────────────────────────────────────────────────────────────
User taps "Test layout"   Subtopic("Test layout",        THREECOL_TABLE layout
under topic "SVO"          "test_layout",                 + load simple_what.txt
                           ContentLayout.THREECOL_TABLE)  + stats from test_layout_3col_stats.json
```

- **Topic** = group in the drawer (e.g. "SVO", "Verbs").
- **Subtopic** = one selectable row (e.g. "Test layout", "I", "S-V-simple").
- Each subtopic has: **title**, **actionKey**, and **layoutType** (which content layout to show).
- Selecting a subtopic: **switch content layout** (if needed) → **run the action** for that **actionKey** (load the right lesson).

---

## 2. Data types (MainActivityTypes.kt / DrawerTopicBuilders.kt)

- **`ContentLayout`**  
  Enum of all possible “screens” in the main content area:  
  `LEGACY`, `TEXT_DISPLAY`, `THREECOL_TABLE`, `SV_RIBBON`, `SIMPLE_SENTENCE`, etc.

- **`Subtopic(title, actionKey, layoutType)`**  
  - `title`: text in the drawer (e.g. `"Test layout"`).  
  - `actionKey`: string used to decide what to load (e.g. `"test_layout"`, `"sv_ribbon"`).  
  - `layoutType`: which `ContentLayout` to show (e.g. `ContentLayout.THREECOL_TABLE`).

- **`Topic(title, subtopics)`**  
  One drawer section with a list of subtopics.

- **`DrawerItem`**  
  What the drawer list actually shows: level header, topic header, or **SubtopicEntry(subtopic, ...)**.

- **`getContentLayoutResId(ContentLayout)`**  
  Maps enum → **root** layout XML. For shell lessons (3-col, tense triplets, bubbles, extend sentence, preposition), the enum maps to **`layout_lesson_base`**; **`MainActivity.switchContentLayout`** then inflates **`layout_threecol_content`** (etc.) into **`lesson_base_content`**. See **`LESSON_BASE_LAYOUT.md`** for the full catalog.

---

## 3. Where Test layout is defined

In **DrawerTopicBuilders.kt**, inside the **SVO** topic:

```kotlin
Topic("SVO", listOf(
    ...
    Subtopic("Test layout", "test_layout", ContentLayout.THREECOL_TABLE)
    ...
))
```

So:

- **Topic** = "SVO".
- **Subtopic** = "Test layout", **actionKey** = `"test_layout"`, **layoutType** = `THREECOL_TABLE`.

No other subtopic uses `THREECOL_TABLE` for this app; Test layout is the one you’re testing.

---

## 4. What happens when the user taps a subtopic

All drawer item clicks go through **performDrawerItemClick(position)**. For a **SubtopicEntry**:

1. Close the drawer.
2. If the subtopic’s **layoutType** is not LEGACY, call **`switchContentLayout(item.subtopic.layoutType)`** so the main area shows the right layout (e.g. `THREECOL_TABLE`).
3. Call **`handleSubtopicAction(item.subtopic.actionKey)`** to load the lesson (and any other side effects).

So for "Test layout":

- `layoutType` = `THREECOL_TABLE` → content area becomes the 3-column table layout.
- `actionKey` = `"test_layout"` → `handleSubtopicAction("test_layout")` runs.

---

## 5. handleSubtopicAction("test_layout")

In **MainActivity.kt**:

```kotlin
if (actionKey == "test_layout") {
    if (currentContentLayout != ContentLayout.THREECOL_TABLE)
        switchContentLayout(ContentLayout.THREECOL_TABLE)
    loadThreeColLessonFromAsset("Lessons/SVO/simple_what.txt", "test_layout")
    return
}
```

So for Test layout:

1. Ensure the current content layout is **THREECOL_TABLE** (switch if not).
2. Load the lesson from the asset **"Lessons/SVO/simple_what.txt"** with **lesson title/key** **"test_layout"**.

Other subtopics are handled by different branches (e.g. `simple_*`, `sv_ribbon`, SVO assets, pronunciation, etc.).

---

## 6. switchContentLayout(ContentLayout.THREECOL_TABLE)

In **MainActivity.kt**:

1. **Remove all views** from `contentFrame` (navigation always re-inflates).
2. **Inflate the lesson shell** via **`inflateLessonShellWithContent(R.layout.layout_threecol_content)`**: **`layout_lesson_base.xml`** (magenta mode bar + **`lesson_base_control_include`**) with **`layout_threecol_content.xml`** inside **`lesson_base_content`**.
3. **Add** that root to `contentFrame` and set **currentContentLayout = THREECOL_TABLE**.
4. **Control bar**: bind **`controlActionsBar`** to **`lesson_base_control_include`** inside the inflated view; hide the **activity-level** `control_actions_include` and **`bottom_bar`** (same pattern as tense triplets / extend sentence).
5. **Layout-specific setup**: for `THREECOL_TABLE`, reset three-col state and call **updateThreeColControlBar()** (no lesson data yet; that comes from `loadThreeColLessonFromAsset`).

So the **structure** of the screen (tabs, table, in-content control bar) is created here; the **rows** and **stats** are filled in by the loader. Full layout reference: **`LESSON_BASE_LAYOUT.md`**.

---

## 7. loadThreeColLessonFromAsset("Lessons/SVO/simple_what.txt", "test_layout")

This is where **lesson data** and **persisted stats** meet the **Test layout** UI:

1. **Read asset** `Lessons/SVO/simple_what.txt` and parse lines as CSV: **English, Bengali, Hint** → list of **ThreeColRow**.
2. **Base data**:  
   - **threeColBaseRows** = parsed rows (never reordered; used as “sentence list by index”).  
   - **threeColDisplayToBaseIndex** = `0, 1, 2, ...` (one-to-one with base rows).  
   - **threeColRows** = same as base rows for now (filter/shuffle applied later when user changes mode or filter).
3. **Persisted stats**:  
   **threeColStats** = **LessonFileParsers.loadThreeColStats(filesDir, "test_layout", rows.size)**  
   - File: **test_layout_3col_stats.json** in app internal storage.  
   - One entry per base row: `[practicePassed, testPassed, attempts]`.  
   - Used for: **x/y/z** display and **“Failed & untried only”** filter.
4. **State**:  
   - Mode = Learning, filter off, current index 0.  
   - **lessonName** = `"test_layout"` (for title and for stats key).
5. **UI (post to content frame)**:  
   - Find **RecyclerView** `threecol_recycler` in the already-inflated THREECOL_TABLE view.  
   - Create or reuse **ThreeColDataAdapter** with **threeColRows**, set **LayoutManager** and **adapter**.  
   - **setupThreeColModeButtons(root)** → wires Learning / Practice / Test tabs, prev/next, and “Failed & untried only” switch.  
   - **updateThreeColStats()** and **updateThreeColRowPositionText()** so the header shows x/y/z and arrow state.

So: **Topic/Subtopic** only decide *which* layout and *which* loader; the **loader** (here `loadThreeColLessonFromAsset`) decides *which asset* and *which stats file* (via the lesson title/key **"test_layout"**).

---

## 8. End-to-end flow for Test layout (summary)

| Step | Where | What |
|------|--------|------|
| 1 | DrawerTopicBuilders | **Topic "SVO"** contains **Subtopic("Test layout", "test_layout", THREECOL_TABLE)**. |
| 2 | User taps "Test layout" | Drawer list fires **performDrawerItemClick** for that **SubtopicEntry**. |
| 3 | performDrawerItemClick | Calls **switchContentLayout(THREECOL_TABLE)** then **handleSubtopicAction("test_layout")**. |
| 4 | switchContentLayout | Inflates **`layout_lesson_base` + `layout_threecol_content`**, sets **currentContentLayout**, binds in-content control bar, hides bottom bar, runs THREECOL_TABLE setup (control bar state only). |
| 5 | handleSubtopicAction | Sees **actionKey == "test_layout"** → calls **loadThreeColLessonFromAsset("Lessons/SVO/simple_what.txt", "test_layout")**. |
| 6 | loadThreeColLessonFromAsset | Parses asset → **threeColBaseRows**; loads **test_layout_3col_stats.json** → **threeColStats**; sets **threeColRows** and mode; in **contentFrame.post**: attaches **ThreeColDataAdapter** to **threecol_recycler**, calls **setupThreeColModeButtons**, **updateThreeColStats**, **updateThreeColRowPositionText**. |
| 7 | Later (Start / Practice / Test / filter) | All use **threeColRows**, **threeColDisplayToBaseIndex**, **threeColStats**; verification updates **threeColStats** and saves back to **test_layout_3col_stats.json**. |

---

## 9. How this differs from other subtopics

- **Same flow**: drawer → **SubtopicEntry** → **switchContentLayout(layoutType)** → **handleSubtopicAction(actionKey)**.
- **Different layout / loader**:
  - **test_layout** → `THREECOL_TABLE` + `loadThreeColLessonFromAsset("Lessons/SVO/simple_what.txt", "test_layout")`.
  - **sv_ribbon** → `SV_RIBBON` + `loadSvRibbonLesson("sv_ribbon")`.
  - **simple_*** → `SIMPLE_SENTENCE` + `loadSimpleSentenceLesson(actionKey)`.
  - Many SVO items → `LEGACY` + `loadSvoFromAssetEnglishFirst(...)`.

So: **one** drawer/topic/subtopic pipeline; **many** layout + loader combinations keyed by **actionKey** and **layoutType**. Test layout is the one that uses **THREECOL_TABLE** and **simple_what.txt** with lesson key **"test_layout"** and stats file **test_layout_3col_stats.json**.
