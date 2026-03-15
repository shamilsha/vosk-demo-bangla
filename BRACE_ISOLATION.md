# Isolating the "Missing '}'" in MainActivity.kt

MainActivity.kt has **one extra `{`** (brace count: 1805 open, 1804 close). The compiler reports "Missing '}'".

**Isolation attempts (summary):** Commenting out a large block (e.g. lines 3576–6050) with `// BRACE_ISOL` changed the error to "Expecting a top level declaration" because the remaining active code (6051+) was orphaned. Commenting out lines 1100–3000 broke the parse (middle of `when`). So block comment-out only works if you comment **whole functions** on boundaries. Adding an extra `}` after the `when` in `handleSubtopicAction` (after line 2927) balanced the file but produced "MainActivity.ContentLayout" vs "ContentLayout" and other errors, so that `}` likely closed the wrong scope. The missing `}` is likely inside a large block (e.g. `when`, `object : BaseAdapter()`, or a listener); finding it needs either manual inspection of such blocks or commenting out **entire functions** (from `private fun X` to its closing `}`) one at a time and re-building.

**Confirmed (comment-out test):** Commenting out the **entire body** of `handleSubtopicAction` made the "Missing '}'" error go away in that test run (different file state). A **brace-count binary search** on the current file (script skips `// BRACE_ISOL` lines) showed: full file has **one extra `{`** (Delta=+1). The extra `{` is **not** in `handleSubtopicAction` (2492–2724); it is in **lines 94–693**. Binary search narrowed it to **94–693**, then **394–693**, then **544–693**, then **619–693**, then **657–693**, then **676–693**. So the unclosed block is in the region **676–693** (around `parseVerbLessonFile` / KDoc). **Fix applied:** The duplicate `private class SentenceAdapter` at the end of MainActivity.kt was removed (use the one in SentenceAdapter.kt). After that removal, the file brace count was balanced and the temporary BRACE_FIX `}` was reverted. No extra `}` is needed. Scripts: `brace_balance.ps1` (balance; full-file delta when excluding BRACE_ISOL lines), `brace_binary_search.ps1 -CommentStart N -CommentEnd M`. Use script `comment_out_brace_isol.ps1` (or similar) to re-run the test; restore with `git checkout -- app/src/main/java/com/alphacephei/vosk/MainActivity.kt` afterward.

## Option 1: Comment out blocks to find the extra `{`

You can **comment out a large section** of the class body and rebuild to see whether the error moves:

1. **Comment out** a block of code (e.g. lines 3500–6000). Use block comment `/* ... */` around the range, or multi-line selection + toggle line comment in the IDE.
2. **Build.**  
   - If the error becomes **"Unmatched '}'"** or **"Too many '}'"**, the extra `{` was **inside** the block you commented out → the missing `}` is in that region.  
   - If the error stays **"Missing '}'"**, the extra `{` is **outside** that block (in the remaining code).
3. **Narrow down**: comment out half of the suspect region and repeat until you have a small range (e.g. one function).
4. In that range, look for:
   - A `when` or `if`/`else` block that is missing a closing `}`.
   - An `object : SomeListener {` or lambda `{` that was not closed.
   - Copy/paste or merge mistakes where a `}` was dropped.

## Option 2: Brace balance by line (script)

From the project root (PowerShell):

```powershell
$lines = Get-Content "app\src\main\java\com\alphacephei\vosk\MainActivity.kt"
$bal = 0
for ($i = 0; $i -lt $lines.Count; $i++) {
  $l = $lines[$i]
  $open = ([regex]::Matches($l, '\{')).Count
  $close = ([regex]::Matches($l, '\}')).Count
  $bal += $open - $close
  if ($bal -eq 1 -and $l -match '^\s*\}') { "Line $($i+1): $bal  $l" }
}
```

Lines where balance is 1 and the line is a closing `}` are candidates for “block just ended”; the missing `}` might be right after one of them.

## Option 3: Do not add a temporary `}` at the end

Adding an extra `}` at the very end (before the final `}`) closes the class too early and makes the rest of the file parse as "local" code, so the build fails with "Modifier 'private' is not applicable to 'local function'". The missing `}` must be inserted in the correct place inside the class (after the block that is missing it).
