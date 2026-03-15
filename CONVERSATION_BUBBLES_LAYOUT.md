# Conversation Bubbles Layout — Documentation

This document describes the **Conversation Bubbles** content layout: structure, behavior in each mode (Learning, Practice, Test), reusable pieces, and how to add or reuse it for new lessons.

---

## 1. Overview

The Conversation Bubbles layout shows a two-person dialogue as **chat-style bubbles**: Person A on the left, Person B on the right. Each bubble can show Bengali, English, and optional pronunciation. The layout supports three modes with different levels of scaffolding and feedback.

- **Layout ID:** `ContentLayout.CONVERSATION_BUBBLES`
- **Main layout file:** `app/src/main/res/layout/layout_conversation_bubbles.xml`
- **Adapter:** `ConversationBubbleAdapter.kt`
- **Data type:** `ConversationBubbleRow(speaker, english, bengali, pronunciation)` (see `MainActivityTypes.kt`)

---

## 2. Layout Structure

```
┌─────────────────────────────────────────────────────────┐
│  [Prev]  [Learning] [Practice] [Test]  [Next]            │  ← Mode bar + prev/next
├─────────────────────────────────────────────────────────┤
│  [Failed & untried only] (Practice only)                 │  ← Optional filter row
│  Initiator: [App starts] [User starts] (Test only)       │  ← Test options
├─────────────────────────────────────────────────────────┤
│  Person A (left) · Person B (right)            stat 0/0   │  ← Header + session stat
├─────────────────────────────────────────────────────────┤
│                                                          │
│   ┌──────────────┐         ┌──────────────┐              │
│   │ Bengali      │         │              │  English     │  ← RecyclerView of bubbles
│   │ English      │         │   Bengali    │              │
│   │ Pronunciation│         │   English    │              │
│   └──────────────┘         └──────────────┘              │
│   ...                      ...                            │
├─────────────────────────────────────────────────────────┤
│  [Start]  [Pause/Resume]                                 │  ← Control bar (activity-level)
└─────────────────────────────────────────────────────────┘
```

- **Mode bar:** Learning / Practice / Test tabs and Prev/Next row buttons.
- **Filter (Practice):** “Failed & untried only” narrows the list to rows not yet passed in Practice.
- **Test options:** “App starts” vs “User starts” sets who speaks the first line in Test.
- **RecyclerView:** One item per dialogue line; left/right depends on `ConversationBubbleRow.speaker` ("A" or "B").
- **Control bar:** Start/Stop and Pause/Resume are shown at **activity level** when this layout is active; the duplicate `<include>` inside the layout is set to `GONE`.

---

## 3. Modes and Behavior

### 3.1 Learning

- **Purpose:** Hear and see each line; then repeat for verification.
- **Display:** Each bubble shows Bengali, English, and pronunciation (if present).
- **Flow:**
  1. User taps **Start.** App speaks the **Bengali** of the current bubble, then starts the mic to listen for the **English**.
  2. If the user says the correct English → “Correct”, then after 1.5 s the app moves to the next bubble and speaks its Bengali.
  3. If wrong → “Incorrect”, then the app speaks the **correct English** (hint), then “Try again” and restarts listening for the same line. After **3 wrong attempts**, the correct English is shown in the bubble and the app advances to the next after 1.5 s.
- **Scrolling:** Current bubble is kept in view; once past a “center” index, the list scrolls by one bubble height (smooth). Same logic for any number of items.
- **Stats:** Session stat (e.g. 3/5) updates; per-row Practice/Test stats are not shown in UI in Learning but can be used for filtering later.

### 3.2 Practice

- **Purpose:** See only Bengali; say the English; get hints on failure.
- **Display:** Each bubble shows **Bengali** only until the user has attempted; then it shows what they said and a ✓/✗ badge.
- **Flow:**
  1. Start → app speaks **Bengali** of current bubble, then listens for **English**.
  2. Correct → advance to next (after 1.5 s).
  3. Wrong → app speaks the **correct English** (hint), then “Try again” and listens again. After **3 wrong**, the correct English is **shown** in the bubble and the app advances after 1.5 s (no TTS of the sentence).
- **Filter:** “Failed & untried only” shows only rows that are not yet passed in Practice (A stat = 0). Pass state is persisted per lesson.
- **Stats:** Practice pass/fail per row is saved (A in `convBubbleStats`); session stat updates.

### 3.3 Test

- **Purpose:** Role-play with **no hints**; only “Correct”/“Incorrect” feedback.
- **Display:** Bubbles start **empty**. When the app finishes a line, that bubble shows the English. When the user finishes a line, that bubble shows what they said and ✓ or ✗.
- **Flow:**
  1. **App starts:** App speaks the first **app** line (English), then listens for the **user** line (English). Then app speaks next app line, etc.
  2. **User starts:** App listens for the first line (user); then app speaks the next line (app), etc.
  3. Correct → advance to next line (after 1.5 s).
  4. Wrong → **no** speaking of the correct sentence. Only “Incorrect” is spoken. After **3 wrong**, the correct English is **shown** in the bubble (so the user can read it) and the app advances after 1.5 s — still **no TTS** of the answer.
- **Initiator:** “App starts” = first line is an “app” line (Person A by default); “User starts” = first line is the user’s (e.g. Person B). Which lines are “app” vs “user” is derived from the first speaker and the A/B role.
- **Stats:** Test pass/fail per row is saved (B in `convBubbleStats`); session stat updates.

---

## 4. Reusable Features

These parts are designed so the same layout and logic can serve many lessons.

| Feature | Description | Where |
|--------|-------------|--------|
| **Single layout** | One XML and one adapter for all conversation-bubble lessons | `layout_conversation_bubbles.xml`, `ConversationBubbleAdapter` |
| **Lesson file format** | Same parser for every lesson; add new lessons by adding files and map entries | `LessonFileParsers.parseConversationBubbleFile`, `conversationBubbleLessonAssetPaths` |
| **Control bar** | Start/Stop and Pause/Resume are the same bar used by other layouts; visibility is toggled by content layout | `layout_control_actions.xml`, `activity_main` |
| **Verification pipeline** | Same flow: TTS → listen → compare with `MatchNormalizer` (bracket alternates, normalization) | `handleVerificationResult`, `startVerificationListening`, `onConvBubbleVerificationResult` |
| **Scroll behavior** | Center-index + one-bubble scroll; works for any list length and bubble height | `updateConvBubbleRowPositionText()` |
| **Stats persistence** | Per-row Practice (A) and Test (B) stored by lesson key | `LessonFileParsers.saveConversationBubbleStats`, `loadConversationBubbleStats` |
| **Bracket alternates** | Content can use `[alt1|alt2]`; display/speak use first option; matching accepts any | `MatchNormalizer.textForSpeakAndDisplay`, `matchesExpectedWithAlternates` |
| **Display sanitization** | User’s spoken text is cleaned for display (e.g. remove `$` before digits) | `MatchNormalizer.sanitizeSpokenTextForDisplay` |

---

## 5. Lesson File Format

- **Path:** Under `assets`, e.g. `Lessons/Conversation/conversation_1.txt`.
- **Encoding:** UTF-8.
- **Format (one line per dialogue line):**

```text
PersonA: English,Bengali,Pronunciation
PersonB: English,Bengali,Pronunciation
```

- **Speaker:** `PersonA` or `PersonB` (case-insensitive) → stored as `"A"` or `"B"` for left/right.
- **Fields:** Comma-separated: English, Bengali, Pronunciation. Pronunciation is optional (can be empty).
- **Optional columns 4–5:** For embedding saved stats in the file: `A,B` (0 or 1) for Practice and Test pass. If present, they are applied on load.
- **Bracket alternates:** In the English field you can use `[word1|word2]`; display and TTS use the first option; recognition accepts any option as correct.

Example:

```text
PersonA: What is your name?, আপনার নাম কি?, হোয়াট ইজ ইয়োর নেম?
PersonB: My name is Rahul., আমার নাম রাহুল।, মাই নেম ইজ রাহুল।
PersonB: Nice to meet you [too]., আপনার সাথে দেখা হয়েও ভালো লাগলো।, নাইস টু মিট ইউ টু।
```

---

## 6. How to Reuse / Add a New Lesson

### 6.1 Add a new conversation-bubble lesson

1. **Create the lesson file**  
   Put a `.txt` in `app/src/main/assets/` (e.g. `Lessons/Conversation/conversation_4.txt`) using the format above.

2. **Register asset path in MainActivity**  
   In `MainActivity.kt`, extend the map:

   ```kotlin
   private val conversationBubbleLessonAssetPaths: Map<String, String> = mapOf(
       "conv_bubble_first_meeting" to "Lessons/Conversation/conversation_1.txt",
       "conv_bubble_second_lesson" to "Lessons/Conversation/conversation_2.txt",
       "conv_bubble_third_lesson" to "Lessons/Conversation/conversation_3.txt",
       "conv_bubble_fourth_lesson" to "Lessons/Conversation/conversation_4.txt"  // new
   )
   ```

3. **Add a drawer subtopic**  
   In `DrawerTopicBuilders.kt`, add a subtopic with the same **action key** and `ContentLayout.CONVERSATION_BUBBLES`:

   ```kotlin
   Subtopic("Fourth lesson (bubbles)", "conv_bubble_fourth_lesson", ContentLayout.CONVERSATION_BUBBLES)
   ```

4. **No layout or adapter changes**  
   The same `layout_conversation_bubbles.xml` and `ConversationBubbleAdapter` are used. Loading is done by `handleSubtopicAction` → `loadConversationBubbleLesson(assetPath, actionKey, displayTitle)`.

### 6.2 Flow when user selects the lesson

1. User taps the subtopic (e.g. “Fourth lesson (bubbles)”).
2. `handleSubtopicAction` sees `convBubble_fourth_lesson` in `conversationBubbleLessonAssetPaths`, switches to `ContentLayout.CONVERSATION_BUBBLES` if needed, and calls `loadConversationBubbleLesson("Lessons/Conversation/conversation_4.txt", "conv_bubble_fourth_lesson", "Fourth lesson (bubbles)")`.
3. File is read and parsed with `LessonFileParsers.parseConversationBubbleFile`.
4. Adapter is created or updated with `convBubbleRows`; mode bar, filter, and Test options are wired; control bar is shown; scroll and stats are updated.

### 6.3 Reusing only parts of the layout

- **Different data source:** To drive the same UI from something other than the asset map (e.g. from network or a different file format), you still need to produce `List<ConversationBubbleRow>` and call the same loading path that sets `convBubbleRows` and refreshes the adapter (or add a separate entry point that does the same without the asset path map).
- **Same control bar:** The activity-level control bar is shown when `currentContentLayout == ContentLayout.CONVERSATION_BUBBLES`; no change needed for that.
- **Same verification and TTS:** All speaking and verification go through the same handlers; Learning/Practice/Test differ only by `convBubbleMode` and the rules above (e.g. no correct-sentence TTS in Test).

---

## 7. Key Code References

| Responsibility | Location (file: approximate area) |
|----------------|-------------------------------------|
| Layout inflation for CONVERSATION_BUBBLES | `MainActivityTypes.kt`: `ContentLayoutToLayoutId` |
| Lesson path map and load | `MainActivity.kt`: `conversationBubbleLessonAssetPaths`, `loadConversationBubbleLesson` |
| Mode and initiator logic | `MainActivity.kt`: `convBubbleMode`, `isConvBubbleRowAppByInitiator`, `convBubbleFirstIndexForInitiator` |
| Speak current / verification result | `MainActivity.kt`: `speakConvBubbleCurrent`, `onConvBubbleVerificationResult` |
| 3-strike advance and no hint in Test | `MainActivity.kt`: `handleVerificationResult` (CONVERSATION_BUBBLES block), `pendingSpeakCorrectWordAfterIncorrect` only when not TEST |
| Scroll and prev/next | `MainActivity.kt`: `updateConvBubbleRowPositionText`, `setupConvBubbleModeButtons` |
| Adapter and row binding | `ConversationBubbleAdapter.kt`: `learningMode`, `testMode`, `setSpokenText`, `markResult`, `revealEnglishForAppSpoke` |
| File format | `LessonFileParsers.kt`: `parseConversationBubbleFile` |
| Drawer subtopics | `DrawerTopicBuilders.kt`: Introduction topic subtopics with `ContentLayout.CONVERSATION_BUBBLES` |
| Bubbles UI | `layout_item_conv_bubble_left.xml`, `layout_item_conv_bubble_right.xml` |

---

## 8. Summary

- **One layout, one adapter:** All conversation-bubble lessons use the same XML and adapter.
- **Three modes:** Learning (hear Bengali, say English, with hints), Practice (see Bengali only, say English, with hints and “failed only” filter), Test (role-play, no hints; only “Correct”/“Incorrect” and, after 3 fails, showing the correct sentence without speaking it).
- **Reuse:** Add a new lesson by adding a `.txt` file, one map entry in `conversationBubbleLessonAssetPaths`, and one subtopic in `DrawerTopicBuilders.kt` with the same action key and `ContentLayout.CONVERSATION_BUBBLES`.
