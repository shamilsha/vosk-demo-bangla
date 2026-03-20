# Reusable Layout Catalog

This document lists reusable lesson layouts, their names, purpose, and key features.

## 1) Conversation Bubbles

- **Layout file:** `app/src/main/res/layout/layout_conversation_bubbles.xml`
- **ContentLayout:** `CONVERSATION_BUBBLES`
- **Purpose:** Role-play style sentence practice with per-row verification.
- **Top bar features:**
  - Prev / Next arrows
  - Tabs: `Learning`, `Practice`, `Test`, `V`
  - Test options (initiator), weak-only filter (practice)
- **Bottom bar features:**
  - Shared control bar include (`layout_control_actions`)
  - Start/Stop + Pause/Resume + playback hold button
- **Content features:**
  - Bubble list with current-row focus
  - Per-row tick/cross in practice/test
  - Auto-scroll behavior to keep active row near center
- **Speech flow:**
  - Learning/Practice/Test: app speaks prompt, user responds, compare, advance
  - 3 incorrect attempts -> auto next (mode-dependent behavior)
- **V tab features:**
  - Vocabulary table with word testing flow
  - Show-all toggle, progress filtering

## 2) Three-Column Table (2-column display lesson)

- **Layout file:** `app/src/main/res/layout/layout_3coldata_2coldisplay.xml`
- **ContentLayout:** `THREECOL_TABLE`
- **Purpose:** Bengali->English practice with hint/stat support.
- **Top bar features:**
  - Prev / Next arrows
  - Tabs: `Learning`, `Practice`, `Test`, `V`
  - Weak-only toggle support
- **Bottom bar features:**
  - Shared control bar include (`layout_control_actions`)
  - Start/Stop + Pause/Resume + playback hold button
- **Content features:**
  - Recycler row list for sentence pairs
  - Current-row focus and per-row result mark
- **Speech flow:**
  - App prompt (Bengali / mode-dependent), user replies in English, compare, advance
  - Retry/3-strike flow for incorrect attempts
- **V tab features:**
  - Reuses same vocabulary testing component as conversation bubbles

## 3) Tense Triplets (Present/Past/Future)

- **Layout file:** `app/src/main/res/layout/layout_tense_triplets.xml`
- **ContentLayout:** `TENSE_TRIPLETS`
- **Purpose:** Triplet practice across present/past/future in one row.
- **Top bar features:**
  - Prev / Next arrows
  - Tabs: `Learning`, `Practice`, `Test`, `V`
  - Column checkboxes (Present/Past/Future) to control visibility and speaking
- **Bottom bar features:**
  - Shared control bar include (`layout_control_actions`)
  - Anchored at bottom regardless of content size
  - Start/Stop + Pause/Resume (+ V tab testing behavior)
- **Content features:**
  - 3-column row table (`layout_item_tense_triplet.xml`)
  - Active row highlighted with red border
  - Per-column tick/cross marks
  - Auto-scroll keeps focused row near middle when possible
- **Speech flow (Learning/Practice):**
  - App speaks selected columns in order (EN+BN per column)
  - User repeats selected English columns
  - Compare per column; on correct -> next row
  - On incorrect -> retry same row, up to 3 attempts, then auto next
- **Speech flow (Test):**
  - Table shows Bengali prompt side
  - App speaks Bengali prompts
  - User replies in English; compare against English targets
  - Matched columns update with English, then advance behavior as above
- **V tab features (this lesson):**
  - Unique English words are extracted from triplet lesson rows
  - Reuses shared vocabulary testing flow
  - 4th fixed column shows what user said + tick/cross

## 4) SV Ribbon

- **Layout file:** `app/src/main/res/layout/layout_sv_ribbon.xml`
- **ContentLayout:** `SV_RIBBON`
- **Purpose:** Subject-verb conveyor style drills.
- **Top bar features:**
  - Mode buttons (`Learning`, `Practice`) and lesson styling
- **Bottom bar features:**
  - Shared control bar include (`layout_control_actions`)
- **Content features:**
  - Ribbon/conveyor visuals for compact repeated patterns
- **Speech flow:**
  - App prompt then user response, compare, and step movement

## 5) Shared Reusable Includes

### 5.1 Control Actions Bar
- **Layout file:** `app/src/main/res/layout/layout_control_actions.xml`
- **Purpose:** Common bottom control row used by multiple lesson layouts.
- **Includes:**
  - Start/Stop button
  - Pause/Resume button
  - Playback hold/record button

### 5.2 Vocabulary Panel
- **Layout files:**
  - `app/src/main/res/layout/layout_lesson_vocab_pronunciation.xml`
  - `app/src/main/res/layout/layout_item_lesson_vocab_row.xml`
- **Purpose:** Reusable vocabulary testing table for `V` tab.
- **Features:**
  - Table columns: Word, Pronunciation, Meaning, You said
  - Tick/cross result icon with fixed layout (no flow jump)
  - Show-all toggle with progress filtering

## Notes

- Top mode bars use shared color token: `@color/lesson_mode_bar_background`.
- Lesson-specific logic is routed in `MainActivity.kt` by `currentContentLayout` and mode enum.
- For new lessons, prefer reusing these layouts and only changing parser/data source plus action key mapping.
