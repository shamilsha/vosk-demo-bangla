# Reusable Top and Bottom Bars for All Lessons

All lessons that support Learning / Practice / Test (and the V tab) should use **the same top bar** and **the same bottom bar**. Prev/Next arrows work in **all situations** — they move within the current content (bubbles, 3-col rows, or vocabulary list) depending on the active lesson and mode.

---

## 1. Top bar (same for all lessons)

- **Contents:** Prev arrow | Learning | Practice | Test | V | Next arrow  
- **Same layout and IDs** so one code path can wire clicks and update appearance (which tab is selected).
- **Used by:** Conversation bubbles, 3-col table, and any future lesson type that has these modes.
- **Behavior:**
  - **Tabs:** Switch mode (Learning / Practice / Test / V). Only the **middle content** changes; top and bottom bars stay.
  - **Prev:** Moves to the previous item in the current context:
    - Conversation bubbles → previous bubble/row
    - 3-col → previous row
    - V (vocabulary pronunciation) → previous word in the lesson vocabulary list
  - **Next:** Same idea for next item in the current context.

So: **one top bar UI**, **one set of click handlers**; inside the handlers, branch on **current layout** and **current mode** and call the right logic (conv prev/next, threecol prev/next, or vocab prev/next).

---

## 2. Bottom bar (same for all lessons)

- **Contents:** Start/Stop and Pause/Resume (the existing control bar from `layout_control_actions` or equivalent).
- **Same for:** Conversation bubbles, 3-col, and V tab. When the user is in V, Start/Stop runs vocabulary pronunciation practice (speak word, listen, verify); when in Learning/Practice/Test it runs the existing lesson flow.
- **Visibility:** Shown whenever the current lesson uses it (no duplicate bar per layout).

So: **one bottom bar**; its behavior (what “Start” does) depends on current layout and mode, but the bar itself is the same.

---

## 3. Middle content (what changes)

- **Conversation bubbles:** Header + bubble RecyclerView (or, when V is selected, the 3-column vocabulary list).
- **3-col lesson:** Table RecyclerView (or, when V is selected, the same 3-column vocabulary list).
- **V tab:** Always the **reusable 3-column layout** (Word | Pronunciation | Meaning), populated from the **current lesson’s** vocabulary and master list.

So the **only** part that changes per lesson type and mode is the **middle area**. Top bar and bottom bar are shared and stay the same.

---

## 4. Implementation approach

1. **Single top bar layout**  
   Define one layout (or one include) for: Prev | Learning | Practice | Test | V | Next. Use it in both conversation-bubbles and 3-col (and any future lesson) so the view hierarchy and IDs are identical. If today the two layouts each have their own mode bar, refactor so both use the same include (e.g. `layout_lesson_mode_bar.xml`).

2. **Single bottom bar**  
   Already using a shared control bar; ensure it’s the only one visible for these lessons and that it’s the same view (e.g. activity-level include), not a copy inside each layout.

3. **Prev/Next in one place**  
   In code, attach prev/next to the **same** top bar views. In the click handler:
   - If current layout is CONVERSATION_BUBBLES: if mode is V, call “vocab prev/next”; else call existing conv bubble prev/next.
   - If current layout is THREECOL_TABLE: if mode is V, call “vocab prev/next”; else call existing threecol prev/next.
   So prev/next “work for all situations” by delegating to the right logic based on layout + mode.

4. **New lesson types**  
   When you add a new lesson that uses these modes, add it to the same top/bottom bar and add a branch in the prev/next (and Start/Stop) logic for the new layout. The bars themselves don’t change.

---

## 5. Summary

| Element      | Same for all lessons? | Notes |
|-------------|------------------------|--------|
| Top bar     | Yes                    | Tabs + Prev/Next; one layout, one handler that branches by layout + mode. |
| Bottom bar  | Yes                    | Start/Stop, Pause/Resume; one bar, behavior depends on layout + mode. |
| Middle area | No                     | Bubbles, 3-col table, or V 3-column list; only this part switches. |

Prev/Next arrows work in all situations because the single top bar’s handlers delegate to the correct prev/next logic for the current lesson and mode (including V).
