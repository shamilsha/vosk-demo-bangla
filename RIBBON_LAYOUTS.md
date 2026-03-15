# Ribbon / Conveyor layouts: 2-ribbon vs 3-ribbon and usage

## Reusable ribbon component

**Yes.** The app has a reusable ribbon component you can use in multiple layouts or multiple times in the same layout:

- **`ConveyorBeltView`** (custom view in `ConveyorBeltView.kt`) – A single conveyor belt: vertical list of items that scrolls one row at a time. Use it anywhere by adding `<com.alphacephei.vosk.ConveyorBeltView android:id="@+id/your_id" ... />` and calling `setData(list)`, `moveToNext { }`, `getCurrentIndex()` from code.
- **`layout_ribbon_pair.xml`** – A reusable **include** that contains **two** `ConveyorBeltView`s (`conveyor_left`, `conveyor_right`) and a connector line. Use it in any layout with `<include layout="@layout/layout_ribbon_pair" ... />`. You can include it multiple times in the same layout if you give the include an id and find views with `include.findViewById(R.id.conveyor_left)` from the included layout (note: include flattens, so ids must be unique per include or use merge with different parent ids).

So: use **one** ribbon in a screen → add one `ConveyorBeltView`. Use **two** ribbons (e.g. Subject + Verb) → use `layout_ribbon_pair` or add two `ConveyorBeltView`s with different ids.

---

## Native ribbon layouts (ConveyorBeltView)

| Layout | Ribbons | Description | **Used by app?** |
|--------|---------|-------------|-------------------|
| **layout_ribbon_pair.xml** | **2** | Two `ConveyorBeltView`s: `conveyor_left` (5 slots), `conveyor_right` (7 slots), with a connector line. Reusable piece. | Yes – included by `layout_sv_ribbon`. |
| **layout_sv_ribbon.xml** | **2** | Includes `layout_ribbon_pair` + title + hint. Subject + Verb (e.g. I play, They play). | **Yes** – `ContentLayout.SV_RIBBON` → this layout. Used for: S-V-simple (`sv_ribbon`), S-V-past (`sv_past`), S-V-future (`sv_future`). |

---

## WebView-based conveyor layouts (HTML)

| Layout | Intended ribbons | Description | **Used by app?** |
|--------|-------------------|-------------|-------------------|
| **layout_conveyor_triple.xml** | 3 (in HTML) | WebView loads triple-conveyor HTML. Learning / Practice bar. | **Yes** – `ContentLayout.CONVEYOR_TRIPLE` → this layout. Used for: S-V-O triple-ribbon lessons. |
| **layout_sv_words_conveyor.xml** | (vocabulary) | WebView for SV Words. Learning / Practice bar. | **Yes** – `ContentLayout.SV_WORDS_CONVEYOR` → this layout. Used for “SV Words (Vocabulary)”. |

---

## Control bar (Start/Stop, Pause/Resume)

The control bar is **mandatory for all custom subtopic layouts** that declare they use it. In `MainActivityTypes.kt`, `usesControlActions(layout)` is true for: `SIMPLE_SENTENCE`, `SV_RIBBON`, `CONVEYOR_TRIPLE`, `SV_WORDS_CONVEYOR`, `SV_I_FOUR_SECTIONS`, `CONVERSATION`, `MIC_SPEAKER_TEST`, `SPEECH_INPUT`, `PRACTICE_THREE_AREA`. For those layouts the bar is shown; for `SV_RIBBON` and `SIMPLE_SENTENCE` the buttons are wired (Start/Stop, Pause/Resume); others show the bar with default button state.

---

## Summary

- **2 ribbons (native):** `layout_sv_ribbon` (includes `layout_ribbon_pair`) for S-V-simple, S-V-past, S-V-future. Uses app color scheme from `colors.xml`.
- **3 ribbons:** `layout_conveyor_triple` (WebView) for triple S-V-O lessons.
- **Reusable:** `ConveyorBeltView` and `layout_ribbon_pair` can be used in multiple layouts or multiple times in the same layout.
