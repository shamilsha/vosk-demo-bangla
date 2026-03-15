# Modularization checklist (small steps to reduce MainActivity.kt size)

**Goal:** Split MainActivity.kt (~10.4k lines) into smaller files to avoid out-of-memory and restarts. Do one small task per session.

## Important: fix build first
The project currently fails to compile (Unresolved reference / "private is not applicable to local function"). This is likely **one extra `{`** somewhere so a function never closes. Fix brace balance first (e.g. run `python scripts/brace_check.py` and add the missing `}` where needed), then continue modularization.

## Done
- **SentenceAdapter** → `SentenceAdapter.kt`
- **Shared types** (ContentLayout, Sentence, LessonRow, SvEntry, DrawerItem, Topic, etc.) → `MainActivityTypes.kt`
- **Removed duplicate** `Topic` and `DrawerItem` from MainActivity (use MainActivityTypes)
- Layout XMLs: most already use `layout_*`, `content_*`, `item_*`, or `drawer_*` naming

## Next small tasks (pick one per session)

1. **Extract DrawerAdapter** – The drawer list adapter (builds DrawerItem views) is a large block in MainActivity. Move to `DrawerAdapter.kt`; pass activity reference or callbacks for clicks.
2. **Extract simple-sentence logic** – All `bindSimpleSentenceView`, `updateSimpleSentenceUi`, `runSimpleSentenceLearningCycle`, `setSimpleSentenceAvatarSpeaking`, etc. into `SimpleSentenceLayoutHelper.kt` (or similar); keep state in MainActivity, pass view + callbacks.
3. **Extract table HTML builder** – `buildTableHtml` and its local functions (large block with raw HTML string) → `TableHtmlBuilder.kt` or `TableDisplayHelper.kt`.
4. **Extract mic/Vosk helpers** – `startBengaliMic`, `stopMicRecording`, `processSamples`, `initMicrophone`, `checkHeadset` into `MicRecognitionHelper.kt`; use callbacks or interface for UI updates.

## Tips
- After each extraction, run `.\gradlew compileDebugKotlin` to confirm build.
- Prefer passing callbacks/lambdas over holding Activity reference to avoid leaks.
- Keep one logical group per file (one layout area or one feature).
