package com.alphacephei.vosk

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Build table HTML for WebView (config-driven columns/headers/rows). Extracted to reduce MainActivity.kt size.
 */
object TableHtmlBuilder {

    fun makeTableLayoutHtml(intro: String, headers: List<String>, rows: List<List<String>>): String =
        makeTableLayoutHtmlInternal(intro, headers, rows, makeFirstColumnTappable = false)

    /** Pronunciation table: first column cells are links word://... so tap speaks the English word. */
    fun makePronunciationTableHtml(headers: List<String>, rows: List<List<String>>): String =
        makeTableLayoutHtmlInternal("", headers, rows, makeFirstColumnTappable = true)

    fun makeTableLayoutHtmlInternal(intro: String, headers: List<String>, rows: List<List<String>>, makeFirstColumnTappable: Boolean): String {
        fun htmlEsc(s: String): String = s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
        val colCount = maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0)
        val introHtml = if (intro.isNotEmpty()) "<div style=\"margin-bottom:8px;line-height:1.4;\">$intro</div>" else ""
        val headerCells = (0 until colCount).joinToString("") { i ->
            val text = headers.getOrNull(i) ?: ""
            "<th style=\"border:1px solid #000;padding:8px 10px;text-align:center;font-weight:bold;background:#f5f5f5;\">${htmlEsc(text)}</th>"
        }
        val bodyRows = rows.joinToString("") { row ->
            val cells = (0 until colCount).joinToString("") { c ->
                val text = row.getOrNull(c) ?: ""
                val escaped = htmlEsc(text)
                val cellContent = if (makeFirstColumnTappable && c == 0 && text.isNotEmpty()) {
                    val href = "word://" + URLEncoder.encode(text, StandardCharsets.UTF_8.name())
                    "<a href=\"$href\" style=\"color:#0066cc;text-decoration:underline;\">$escaped</a>"
                } else {
                    escaped
                }
                "<td style=\"border:1px solid #000;padding:8px 10px;text-align:center;font-weight:bold;\">$cellContent</td>"
            }
            "<tr>$cells</tr>"
        }
        return """
<!DOCTYPE html>
<html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="margin:0;padding:8px;font-family:sans-serif;background:transparent;font-size:14px;">
$introHtml
<div style="overflow-x:auto;">
<table style="width:100%;border-collapse:collapse;border:1px solid #000;">
<thead><tr>$headerCells</tr></thead>
<tbody>$bodyRows</tbody>
</table>
</div>
</body></html>"""
    }

    /**
     * Build the formatted interactive table title string.
     * Format: LETTER - 🎤  x/y - A/3  "expected" ≠ "actual"
     */
    fun buildTableTitle(
        letter: String,
        mic: Boolean = false,
        result: Boolean? = null,
        heard: String = "",
        expected: String = "",
        movingOn: Boolean = false,
        correctCount: Int,
        testedCount: Int,
        retryCount: Int,
        maxRetries: Int
    ): String {
        val sb = StringBuilder()
        sb.append(letter)
        if (mic) sb.append(" 🎤")
        sb.append("  $correctCount/$testedCount")
        if (retryCount > 0 || result == false) {
            sb.append(" - $retryCount/$maxRetries")
        }
        when (result) {
            true -> sb.append("  ✓ \"$heard\"")
            false -> {
                if (heard.isNotEmpty()) {
                    sb.append("  \"$expected\" ≠ \"$heard\"")
                } else {
                    sb.append("  \"$expected\" ≠ \"\"")
                }
                if (movingOn) sb.append("  →")
            }
            null -> { }
        }
        return sb.toString()
    }

    /**
     * Build an HTML table string for N columns (interactive mode, silent letter highlight, card style).
     * @param tappableCol column index that gets cell:// links
     * @param silentLetterStyle 0=none, 1=first letter, 2=final e, 3=final r, 4=middle r
     */
    fun buildTableHtml(
        columnCount: Int,
        headers: List<String>,
        rows: List<List<String>>,
        tappableCol: Int,
        interactive: Boolean = false,
        silentLetterStyle: Int = 0,
        showInfoInHeader: Boolean = false,
        cardStyle: Boolean = false
    ): String {
        fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        val silentSpanStyle = "font-weight:bold;color:#c62828;"
        fun formatWordWithSilentLetter(word: String): String {
            val e = esc(word)
            if (e.isEmpty()) return e
            when (silentLetterStyle) {
                1 -> return "<span style=\"$silentSpanStyle\">${e.first()}</span>${e.drop(1)}"
                2 -> if (word.last().equals('e', ignoreCase = true)) {
                    return e.dropLast(1) + "<span style=\"$silentSpanStyle\">${e.last()}</span>"
                }
                3 -> if (word.last().equals('r', ignoreCase = true)) {
                    return e.dropLast(1) + "<span style=\"$silentSpanStyle\">${e.last()}</span>"
                }
                4 -> {
                    val idx = word.indexOfFirst { it.equals('r', ignoreCase = true) }
                    if (idx in 1 until word.length - 1) {
                        return e.take(idx) + "<span style=\"$silentSpanStyle\">${e[idx]}</span>" + e.drop(idx + 1)
                    }
                }
            }
            return e
        }
        val iconColW = if (interactive) 36 else 0
        val headerCells = (0 until columnCount).joinToString("") { i ->
            val text = headers.getOrNull(i) ?: ""
            "<th style=\"background:#3949ab;color:#fff;padding:8px 6px;border:1px solid #555;text-align:center;font-weight:bold;\">${esc(text)}</th>"
        } + if (interactive) {
            if (showInfoInHeader) {
                "<th style=\"background:#E65100;color:#fff;padding:6px;border:1px solid #555;width:${iconColW}px;text-align:center;\"><a href=\"lessoninfo://\" style=\"display:inline-block;font-size:20px;font-weight:bold;color:#fff;text-decoration:none;\">ℹ</a></th>"
            } else {
                "<th style=\"background:#3949ab;color:#fff;padding:4px;border:1px solid #555;width:${iconColW}px;\"></th>"
            }
        } else ""
        val evenBg = "#ffffff"
        val oddBg = "#f5f8ff"
        val cellPadding = if (cardStyle) "14px 10px" else "8px 6px"
        val bodyRows = rows.mapIndexed { rowIdx, row ->
            val bg = if (rowIdx % 2 == 0) evenBg else oddBg
            val rowId = if (interactive) " id=\"row_$rowIdx\"" else ""
            val cells = (0 until columnCount).joinToString("") { c ->
                val raw = row.getOrNull(c) ?: ""
                val text = StringUtils.firstAlternative(raw)
                val displayText = if (c == 0 && silentLetterStyle != 0) formatWordWithSilentLetter(text) else esc(text)
                val content = if (c == tappableCol && text.isNotEmpty()) {
                    val href = "cell://" + URLEncoder.encode(text, StandardCharsets.UTF_8.name())
                    "<a href=\"$href\" style=\"color:#0066cc;text-decoration:underline;font-weight:bold;\">$displayText</a>"
                } else {
                    displayText
                }
                "<td style=\"border:1px solid #ccc;padding:$cellPadding;text-align:center;font-size:${if (interactive) "16px" else "inherit"};\">${content}</td>"
            }
            val iconCell = if (interactive) "<td id=\"icon_$rowIdx\" style=\"border:1px solid #ccc;padding:2px;text-align:center;width:${iconColW}px;font-size:20px;\"></td>" else ""
            val trExtraStyle = if (cardStyle) "border-radius:10px;box-shadow:0 2px 8px rgba(0,0,0,0.08);margin-bottom:6px;" else ""
            "<tr${rowId} style=\"background:$bg;transition:background 0.3s;$trExtraStyle\">$cells$iconCell</tr>"
        }.joinToString("")
        val jsBlock = if (interactive) """
<script>
var completedRows = {};
function highlightRow(idx, color) {
    var rows = document.querySelectorAll('tbody tr');
    for (var i = 0; i < rows.length; i++) {
        if (i === idx) {
            rows[i].style.background = color;
            rows[i].style.fontWeight = 'bold';
            rows[i].style.fontSize = '17px';
            if (color === '#c8e6c9') completedRows[i] = '#c8e6c9';
            else if (color === '#ffe0b2') completedRows[i] = '#ffe0b2';
        } else if (completedRows[i]) {
            rows[i].style.background = completedRows[i];
            rows[i].style.fontWeight = 'normal';
            rows[i].style.fontSize = 'inherit';
        } else {
            rows[i].style.background = (i % 2 === 0) ? '#ffffff' : '#f5f8ff';
            rows[i].style.fontWeight = 'normal';
            rows[i].style.fontSize = 'inherit';
        }
    }
}
function scrollToRow(idx) {
    var row = document.getElementById('row_' + idx);
    if (row) row.scrollIntoView({behavior:'smooth', block:'center'});
}
function resetAllRows() {
    completedRows = {};
    var rows = document.querySelectorAll('tbody tr');
    for (var i = 0; i < rows.length; i++) {
        rows[i].style.background = (i % 2 === 0) ? '#ffffff' : '#f5f8ff';
        rows[i].style.fontWeight = 'normal';
        rows[i].style.fontSize = 'inherit';
        var ic = document.getElementById('icon_' + i);
        if (ic) ic.innerHTML = '';
    }
}
function setRowIcon(idx, symbol, color) {
    var ic = document.getElementById('icon_' + idx);
    if (ic) {
        ic.innerHTML = '<span style="color:' + color + ';font-size:22px;font-weight:bold;">' + symbol + '</span>';
    }
}
function showOverlay(symbol, color) {
    var old = document.getElementById('result_overlay');
    if (old) old.remove();
    var d = document.createElement('div');
    d.id = 'result_overlay';
    d.textContent = symbol;
    d.style.cssText = 'position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);' +
        'font-size:120px;font-weight:bold;color:' + color + ';' +
        'background:rgba(255,255,255,0.85);border-radius:50%;width:160px;height:160px;' +
        'display:flex;align-items:center;justify-content:center;' +
        'box-shadow:0 4px 24px rgba(0,0,0,0.2);z-index:9999;' +
        'animation:fadeOut 1.2s ease-out forwards;';
    document.body.appendChild(d);
    setTimeout(function(){ if(d.parentNode) d.remove(); }, 1300);
}
</script>
<style>
@keyframes fadeOut {
    0%   { opacity:1; transform:translate(-50%,-50%) scale(1); }
    60%  { opacity:1; transform:translate(-50%,-50%) scale(1.1); }
    100% { opacity:0; transform:translate(-50%,-50%) scale(0.8); }
}
</style>""" else ""
        return """
<!DOCTYPE html>
<html><head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body { font-family:sans-serif; font-size:clamp(13px,3.2vw,17px); padding:6px; background:#fff; }
  table { width:100%; border:2px solid #333; ${if (cardStyle) "border-collapse:separate; border-spacing:0 10px;" else "border-collapse:collapse;"} }
  a { -webkit-tap-highlight-color:rgba(0,0,0,0.1); }
  tr { transition: background 0.3s ease; }
</style>
$jsBlock
</head><body>
<table>
<thead><tr>$headerCells</tr></thead>
<tbody>$bodyRows</tbody>
</table>
</body></html>"""
    }
}
