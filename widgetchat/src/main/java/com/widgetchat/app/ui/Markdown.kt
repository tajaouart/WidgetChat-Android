package com.widgetchat.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text

/**
 * Lightweight markdown renderer: handles paragraphs, `* `/`- ` bullets, **bold**, *italic*,
 * `code`, and [links](url) — enough for chat replies without pulling a full markdown lib.
 */
@Composable
fun MarkdownText(markdown: String, color: Color, modifier: Modifier = Modifier) {
    val blocks = markdown.replace("\r\n", "\n").split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    Column(modifier) {
        blocks.forEach { line ->
            Text(
                text = renderLine(line),
                color = color,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}

private fun renderLine(line: String): AnnotatedString {
    var text = line
    var prefix = ""
    if (text.startsWith("* ") || text.startsWith("- ")) {
        prefix = "•  "
        text = text.drop(2)
    }
    // Strip leading markdown heading hashes.
    text = text.trimStart('#', ' ')

    return buildAnnotatedString {
        if (prefix.isNotEmpty()) append(prefix)
        appendInline(text)
    }
}

/** Parse inline **bold**, *italic*, `code`, and [label](url). */
private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInline(input: String) {
    var i = 0
    while (i < input.length) {
        when {
            input.startsWith("**", i) -> {
                val end = input.indexOf("**", i + 2)
                if (end > 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(input.substring(i + 2, end)) }
                    i = end + 2
                } else { append(input[i]); i++ }
            }
            input[i] == '*' -> {
                val end = input.indexOf('*', i + 1)
                if (end > 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(input.substring(i + 1, end)) }
                    i = end + 1
                } else { append(input[i]); i++ }
            }
            input[i] == '`' -> {
                val end = input.indexOf('`', i + 1)
                if (end > 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(input.substring(i + 1, end)) }
                    i = end + 1
                } else { append(input[i]); i++ }
            }
            input[i] == '[' -> {
                val close = input.indexOf(']', i)
                val open = if (close > 0) input.indexOf('(', close) else -1
                val paren = if (open > 0) input.indexOf(')', open) else -1
                if (close > 0 && open == close + 1 && paren > 0) {
                    withStyle(SpanStyle(color = Color(0xFF2F6FED))) { append(input.substring(i + 1, close)) }
                    i = paren + 1
                } else { append(input[i]); i++ }
            }
            else -> { append(input[i]); i++ }
        }
    }
}
