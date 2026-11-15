package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChatQivoInk
import com.example.ui.theme.ChatQivoLine
import com.example.ui.theme.ChatQivoMuted
import com.example.ui.theme.ChatQivoSoft
import com.example.ui.theme.ChatQivoTeal
import com.example.ui.theme.ChatQivoTealDark
import com.example.ui.theme.CodeBlockBg
import com.example.ui.theme.CodeBlockBorder
import com.example.ui.theme.CodeBlockHeaderBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ManropeFontFamily
import kotlinx.coroutines.delay

fun containsRtlScript(text: String): Boolean {
    for (ch in text) {
        val code = ch.code
        if (code in 0x0600..0x06FF || code in 0x0750..0x077F || code in 0x08A0..0x08FF ||
            code in 0xFB50..0xFDFF || code in 0xFE70..0xFEFF || code in 0x0590..0x05FF
        ) {
            return true
        }
    }
    return false
}

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class BulletItem(val text: String) : MdBlock
    data class OrderedItem(val number: String, val text: String) : MdBlock
    data class Blockquote(val text: String) : MdBlock
    data class CodeBlock(val language: String, val code: String) : MdBlock
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : MdBlock
    data object Divider : MdBlock
}

@Composable
fun MarkdownMessageContent(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    SelectionContainer(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            blocks.forEach { block ->
                val textForRtl = when (block) {
                    is MdBlock.Heading -> block.text
                    is MdBlock.Paragraph -> block.text
                    is MdBlock.BulletItem -> block.text
                    is MdBlock.OrderedItem -> block.text
                    is MdBlock.Blockquote -> block.text
                    else -> ""
                }
                val blockDir = if (containsRtlScript(textForRtl)) LayoutDirection.Rtl else LayoutDirection.Ltr

                CompositionLocalProvider(LocalLayoutDirection provides blockDir) {
                    when (block) {
                        is MdBlock.Heading -> {
                            val size = when (block.level) {
                                1 -> 20.sp
                                2 -> 17.sp
                                else -> 15.sp
                            }
                            InlineRichText(
                                raw = block.text,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = ManropeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = size,
                                    lineHeight = size * 1.35f,
                                    color = ChatQivoInk
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        is MdBlock.Paragraph -> {
                            InlineRichText(
                                raw = block.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 23.sp,
                                    color = ChatQivoInk
                                )
                            )
                        }

                        is MdBlock.BulletItem -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    color = ChatQivoTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                                InlineRichText(
                                    raw = block.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                        color = ChatQivoInk
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        is MdBlock.OrderedItem -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${block.number}.",
                                    color = ChatQivoTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    lineHeight = 22.sp
                                )
                                InlineRichText(
                                    raw = block.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                        color = ChatQivoInk
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        is MdBlock.Blockquote -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ChatQivoSoft)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(22.dp)
                                        .background(ChatQivoTeal, RoundedCornerShape(2.dp))
                                )
                                InlineRichText(
                                    raw = block.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        lineHeight = 21.sp,
                                        color = Color(0xFF475752),
                                        fontStyle = FontStyle.Italic
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        is MdBlock.CodeBlock -> {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                CodeBlockCard(language = block.language, code = block.code)
                            }
                        }

                        is MdBlock.TableBlock -> {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                MarkdownTableCard(headers = block.headers, rows = block.rows)
                            }
                        }

                        MdBlock.Divider -> {
                            HorizontalDivider(
                                color = ChatQivoLine,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(language: String, code: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }

    val highlightedCode = remember(code, language) { highlightCodeSyntax(code) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CodeBlockBg,
        border = BorderStroke(1.dp, CodeBlockBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CodeBlockHeaderBg)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = language.ifBlank { "code" }.lowercase(),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = ChatQivoTealDark
                )
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    },
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = ChatQivoTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        fontSize = 11.sp,
                        color = ChatQivoTeal
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = highlightedCode,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun MarkdownTableCard(headers: List<String>, rows: List<List<String>>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ChatQivoLine)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Row(modifier = Modifier.background(ChatQivoSoft)) {
                headers.forEach { header ->
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .border(0.5.dp, ChatQivoLine)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = header,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = ChatQivoInk
                        )
                    }
                }
            }
            rows.forEach { row ->
                Row {
                    headers.indices.forEach { idx ->
                        val cell = row.getOrElse(idx) { "" }
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .border(0.5.dp, ChatQivoLine)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            InlineRichText(
                                raw = cell,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.5.sp,
                                    color = ChatQivoInk
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun InlineRichText(
    raw: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val annotated = remember(raw) { parseInlineMarkdown(raw) }
    ClickableText(
        text = annotated,
        style = style,
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    runCatching { uriHandler.openUri(annotation.item) }
                }
        }
    )
}

private fun parseInlineMarkdown(raw: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < raw.length) {
            // Link [text](url)
            if (raw[i] == '[') {
                val closeBracket = raw.indexOf(']', i + 1)
                if (closeBracket != -1 && closeBracket + 1 < raw.length && raw[closeBracket + 1] == '(') {
                    val closeParen = raw.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val label = raw.substring(i + 1, closeBracket)
                        val url = raw.substring(closeBracket + 2, closeParen)
                        pushStringAnnotation(tag = "URL", annotation = url)
                        withStyle(
                            SpanStyle(
                                color = ChatQivoTeal,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(label)
                        }
                        pop()
                        i = closeParen + 1
                        continue
                    }
                }
            }

            // Inline code `...`
            if (raw[i] == '`') {
                val end = raw.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            background = Color(0xFFEDF4F1),
                            color = ChatQivoTealDark,
                            fontSize = 12.5.sp
                        )
                    ) {
                        append(" ${raw.substring(i + 1, end)} ")
                    }
                    i = end + 1
                    continue
                }
            }

            // Bold **...**
            if (i + 1 < raw.length && raw[i] == '*' && raw[i + 1] == '*') {
                val end = raw.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(raw.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }

            // Italic *...*
            if (raw[i] == '*') {
                val end = raw.indexOf('*', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(raw.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }

            append(raw[i])
            i++
        }
    }
}

private fun highlightCodeSyntax(code: String): AnnotatedString {
    val keywords = setOf(
        "fun", "val", "var", "class", "object", "interface", "return", "if", "else", "when",
        "for", "while", "import", "package", "const", "let", "function", "async", "await",
        "export", "default", "from", "try", "catch", "throw", "new", "def", "True", "False",
        "None", "true", "false", "null", "undefined", "struct", "type", "suspend", "private",
        "public", "override", "data", "sealed"
    )
    return buildAnnotatedString {
        val lines = code.split("\n")
        lines.forEachIndexed { idx, line ->
            val trimmed = line.trimStart()
            if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
                withStyle(SpanStyle(color = ChatQivoMuted, fontStyle = FontStyle.Italic)) {
                    append(line)
                }
            } else {
                var pos = 0
                while (pos < line.length) {
                    val ch = line[pos]
                    if (ch == '"' || ch == '\'') {
                        val quote = ch
                        var end = pos + 1
                        while (end < line.length && line[end] != quote) {
                            if (line[end] == '\\' && end + 1 < line.length) end++
                            end++
                        }
                        val sliceEnd = (end + 1).coerceAtMost(line.length)
                        withStyle(SpanStyle(color = Color(0xFF0B7A5C))) {
                            append(line.substring(pos, sliceEnd))
                        }
                        pos = sliceEnd
                    } else if (ch.isLetter() || ch == '_') {
                        var end = pos + 1
                        while (end < line.length && (line[end].isLetterOrDigit() || line[end] == '_')) {
                            end++
                        }
                        val word = line.substring(pos, end)
                        if (word in keywords) {
                            withStyle(SpanStyle(color = Color(0xFF6B46C1), fontWeight = FontWeight.SemiBold)) {
                                append(word)
                            }
                        } else {
                            withStyle(SpanStyle(color = ChatQivoInk)) {
                                append(word)
                            }
                        }
                        pos = end
                    } else if (ch.isDigit()) {
                        var end = pos + 1
                        while (end < line.length && (line[end].isDigit() || line[end] == '.')) end++
                        withStyle(SpanStyle(color = Color(0xFFB45309))) {
                            append(line.substring(pos, end))
                        }
                        pos = end
                    } else {
                        withStyle(SpanStyle(color = ChatQivoInk)) {
                            append(ch)
                        }
                        pos++
                    }
                }
            }
            if (idx < lines.lastIndex) append("\n")
        }
    }
}

private fun parseMarkdownBlocks(markdown: String): List<MdBlock> {
    val lines = markdown.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<MdBlock>()
    val paragraphBuf = StringBuilder()

    fun flushParagraph() {
        val text = paragraphBuf.toString().trim()
        if (text.isNotEmpty()) {
            blocks.add(MdBlock.Paragraph(text))
        }
        paragraphBuf.clear()
    }

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // Fenced code block
        if (trimmed.startsWith("```")) {
            flushParagraph()
            val lang = trimmed.removePrefix("```").trim()
            val codeBuf = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                if (codeBuf.isNotEmpty()) codeBuf.append("\n")
                codeBuf.append(lines[i])
                i++
            }
            blocks.add(MdBlock.CodeBlock(lang, codeBuf.toString()))
            i++
            continue
        }

        // Table detection
        if (trimmed.startsWith("|") && i + 1 < lines.size && lines[i + 1].trim().matches(Regex("^\\|?[\\s:\\-|]+\\|?$"))) {
            flushParagraph()
            val headers = trimmed.trim('|').split("|").map { it.trim() }
            val rows = mutableListOf<List<String>>()
            i += 2
            while (i < lines.size && lines[i].trim().startsWith("|")) {
                rows.add(lines[i].trim().trim('|').split("|").map { it.trim() })
                i++
            }
            blocks.add(MdBlock.TableBlock(headers, rows))
            continue
        }

        when {
            trimmed.isEmpty() -> flushParagraph()
            trimmed == "---" || trimmed == "***" -> {
                flushParagraph()
                blocks.add(MdBlock.Divider)
            }
            trimmed.startsWith("### ") -> {
                flushParagraph()
                blocks.add(MdBlock.Heading(3, trimmed.removePrefix("### ").trim()))
            }
            trimmed.startsWith("## ") -> {
                flushParagraph()
                blocks.add(MdBlock.Heading(2, trimmed.removePrefix("## ").trim()))
            }
            trimmed.startsWith("# ") -> {
                flushParagraph()
                blocks.add(MdBlock.Heading(1, trimmed.removePrefix("# ").trim()))
            }
            trimmed.startsWith("> ") -> {
                flushParagraph()
                blocks.add(MdBlock.Blockquote(trimmed.removePrefix("> ").trim()))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushParagraph()
                blocks.add(MdBlock.BulletItem(trimmed.drop(2).trim()))
            }
            trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                flushParagraph()
                val dotIdx = trimmed.indexOf('.')
                val num = trimmed.substring(0, dotIdx)
                val body = trimmed.substring(dotIdx + 1).trim()
                blocks.add(MdBlock.OrderedItem(num, body))
            }
            else -> {
                if (paragraphBuf.isNotEmpty()) paragraphBuf.append("\n")
                paragraphBuf.append(line)
            }
        }
        i++
    }
    flushParagraph()
    return blocks
}
