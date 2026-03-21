# Lesson base layout

## Files (use these for every new lesson type)

| File | Role |
|------|------|
| **`layout_lesson_base.xml`** | **Single shell** inside `content_frame`. **Top:** magenta mode bar (`layout_lesson_mode_bar`). **Middle:** `lesson_base_content` (lesson-specific UI only). **Bottom:** **`lesson_base_control_include`** → **`layout_control_actions`** (START/STOP, PAUSE/RESUME, playback — the **lesson** control strip). Optional: `lesson_base_top_extra` (second row under the mode bar). |
| **`layout_lesson_mode_bar.xml`** | **Top lesson bar** — Learning / Practice / Test / V + block prev/next. **Included only from** `layout_lesson_base.xml`. Prev/next: **`ic_mode_bar_triangle_left`** / **`ic_mode_bar_triangle_right`**. Tab styling: **`bg_lesson_mode_tab_selected`** / **`bg_lesson_mode_tab_unselected`** — use **`setBackgroundResource`**, not **`setBackgroundColor`**. |
| **`layout_control_actions.xml`** | **Bottom lesson bar** — START/STOP, PAUSE/RESUME, last-playback, mic styling. **Included only once**, as **`lesson_base_control_include`** at the bottom of **`layout_lesson_base.xml`**. Lesson `*_content.xml` files must **not** `<include>` this. |

**Runtime:** `inflateLessonShellWithContent()`, `inflatePrepositionLessonShell()`, and `inflateExtendSentenceLessonShell()` inflate **`layout_lesson_base`** (magenta mode bar + bottom START/STOP), then attach lesson XML into **`lesson_base_content`** and optionally **`lesson_base_top_extra`**. **Extend sentence** keeps the **mode bar visible** (Learning / Practice / Test / V); part prev/next is in **`lesson_base_top_extra`**; START/STOP lesson audio runs in **Learning** only (same idea as preposition). The **activity** `bottom_bar` in `activity_main.xml` (prev/next **lesson**) is separate app chrome, not this strip.

Global app chrome (menu, prev/next **lesson**, title) lives in **`activity_main.xml`**, not here.

## Structure (`layout_lesson_base.xml`)

```
lesson_base_root
└── lesson_base_top_slot (vertical LinearLayout)
    ├── include: layout_lesson_mode_bar
    │     lesson_mode_bar_prev / lesson_mode_learning … / lesson_mode_bar_next
    └── lesson_base_top_extra (FrameLayout, default visibility=gone)
          optional second row — block title, filters, etc.
└── lesson_base_content (FrameLayout, weight 1)
└── lesson_base_control_include → layout_control_actions  ← bottom bar: START/STOP, PAUSE/RESUME, …
```

## New lesson checklist

1. Add **`layout_<your_lesson>_content.xml`** with **only** the middle UI — **no** top mode bar, **no** bottom START/STOP bar (`layout_control_actions` lives only in **`layout_lesson_base.xml`**).
2. In **`MainActivity.switchContentLayout`**, add a branch that calls **`inflateLessonShellWithContent(R.layout.layout_<your_lesson>_content)`** (same pattern as tense / 3-col / bubbles).
3. If you need a second row under the magenta bar, inflate into **`lesson_base_top_extra`** (see **`inflatePrepositionLessonShell()`** + **`layout_preposition_blocks_top_extra.xml`**).
4. Register **`ContentLayout`** in `MainActivityTypes.kt` ( **`getContentLayoutResId`** can map to **`layout_lesson_base`** for shell-based lessons).
5. Wire **`lesson_mode_*`** and **`control_*`**; add **`lesson_base_control_include`** to the **`activeBar`** `when` in `switchContentLayout` if the lesson uses the in-content control bar.

## Reference

- **Preposition:** `inflatePrepositionLessonShell()` — base + **`layout_preposition_blocks_top_extra`** + **`layout_preposition_blocks_content`**.
- **Extend sentence:** `inflateExtendSentenceLessonShell()` — base (mode bar visible) + **`layout_extend_sentence_top_extra`** + **`layout_extend_sentence_content`** (includes optional V tab include).
- Mockup: `app/src/main/assets/diagrams/lesson-base-layout-mockup.html` — illustrative only; the real control bar uses `layout_control_actions.xml` with **`ic_mic_angled`** (≈45°) so the mic matches the mockup better than the upright **`ic_mic`** used elsewhere (e.g. main record button).
