package com.stem.heartsync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stem.heartsync.ui.theme.*

/**
 * Real-time Markdown syntax styling for Compose TextField without changing text length,
 * ensuring perfect cursor positioning and zero OffsetMapping desynchronization.
 */
class MarkdownVisualTransformation(
    private val textColor: Color = DeepAnchorNavy900,
    private val syntaxColor: Color = DeepAnchorNavy600.copy(alpha = 0.35f),
    private val accentColor: Color = ElectricBlue,
    private val quoteColor: Color = DeepAnchorNavy800
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val annotated = buildAnnotatedString {
            append(raw)

            // 1. Bold: **text**
            val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
            boldRegex.findAll(raw).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                // Syntax markers (** **) dimmed
                addStyle(SpanStyle(color = syntaxColor, fontSize = 13.sp), start, start + 2)
                addStyle(SpanStyle(color = syntaxColor, fontSize = 13.sp), end - 2, end)
                // Content bolded
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, color = textColor),
                    start + 2,
                    end - 2
                )
            }

            // 2. Italic: *text* (excluding **)
            val italicRegex = Regex("(?<!\\*)\\*([^*\\n]+?)\\*(?!\\*)")
            italicRegex.findAll(raw).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                addStyle(SpanStyle(color = syntaxColor, fontSize = 13.sp), start, start + 1)
                addStyle(SpanStyle(color = syntaxColor, fontSize = 13.sp), end - 1, end)
                addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic, color = textColor),
                    start + 1,
                    end - 1
                )
            }

            // 3. Headings: # Heading or ## Heading
            val headingRegex = Regex("(?m)^(#{1,3})\\s+(.+)$")
            headingRegex.findAll(raw).forEach { match ->
                val level = match.groupValues[1].length
                val hashEnd = match.range.first + level
                addStyle(SpanStyle(color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold), match.range.first, hashEnd)
                val headingSize = if (level == 1) 22.sp else if (level == 2) 19.sp else 17.sp
                addStyle(
                    SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = headingSize, color = textColor),
                    hashEnd,
                    match.range.last + 1
                )
            }

            // 4. Quotes: > Quote text
            val quoteRegex = Regex("(?m)^>\\s*(.*)$")
            quoteRegex.findAll(raw).forEach { match ->
                addStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold), match.range.first, match.range.first + 1)
                addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor),
                    match.range.first + 1,
                    match.range.last + 1
                )
            }

            // 5. Bullet Lists: • or -
            val bulletRegex = Regex("(?m)^[•\\-]\\s+")
            bulletRegex.findAll(raw).forEach { match ->
                addStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.ExtraBold), match.range.first, match.range.last + 1)
            }

            // 6. Numbered Lists: 1. 2.
            val numRegex = Regex("(?m)^(\\d+\\.)\\s+")
            numRegex.findAll(raw).forEach { match ->
                addStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            }

            // 7. Checklists: [ ] or [x]
            val checkRegex = Regex("(?m)^\\[([ xX])\\]\\s+")
            checkRegex.findAll(raw).forEach { match ->
                val isChecked = match.groupValues[1].equals("x", ignoreCase = true)
                addStyle(
                    SpanStyle(color = if (isChecked) CardiacCrimson600 else syntaxColor, fontWeight = FontWeight.Bold),
                    match.range.first,
                    match.range.last + 1
                )
                if (isChecked) {
                    val lineEnd = raw.indexOf('\n', match.range.last).let { if (it == -1) raw.length else it }
                    addStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough, color = syntaxColor),
                        match.range.last + 1,
                        lineEnd
                    )
                }
            }
        }

        return TransformedText(annotated, OffsetMapping.Identity)
    }
}

/**
 * Parses markdown string into clean, publication-ready AnnotatedString without raw markdown symbols.
 */
fun parseMarkdownToAnnotatedString(
    rawText: String,
    baseColor: Color = DeepAnchorNavy900,
    accentColor: Color = ElectricBlue
): AnnotatedString {
    if (rawText.isBlank()) return AnnotatedString("")

    return buildAnnotatedString {
        val lines = rawText.lines()
        lines.forEachIndexed { index, line ->
            var currentLine = line
            var isHeading = false
            var headingLevel = 0
            var isQuote = false
            var isBullet = false
            var isNumbered = false
            var isChecked = false
            var isCheckbox = false
            var numberedPrefix = ""

            // Check line prefixes
            if (currentLine.startsWith("# ")) {
                isHeading = true
                headingLevel = 1
                currentLine = currentLine.removePrefix("# ")
            } else if (currentLine.startsWith("## ")) {
                isHeading = true
                headingLevel = 2
                currentLine = currentLine.removePrefix("## ")
            } else if (currentLine.startsWith("> ")) {
                isQuote = true
                currentLine = currentLine.removePrefix("> ")
            } else if (currentLine.startsWith("• ") || currentLine.startsWith("- ")) {
                isBullet = true
                currentLine = currentLine.removePrefix("• ").removePrefix("- ")
            } else if (Regex("^\\d+\\.\\s+").containsMatchIn(currentLine)) {
                isNumbered = true
                val match = Regex("^(\\d+\\.)\\s+").find(currentLine)!!
                numberedPrefix = match.value
                currentLine = currentLine.removePrefix(match.value)
            } else if (currentLine.startsWith("[x] ", ignoreCase = true)) {
                isCheckbox = true
                isChecked = true
                currentLine = currentLine.removePrefix("[x] ").removePrefix("[X] ")
            } else if (currentLine.startsWith("[ ] ")) {
                isCheckbox = true
                isChecked = false
                currentLine = currentLine.removePrefix("[ ] ")
            }

            val lineStart = length

            // Prepend prefix icons / markers
            if (isQuote) {
                append("▍ ")
            } else if (isBullet) {
                append("• ")
            } else if (isNumbered) {
                append(numberedPrefix)
            } else if (isCheckbox) {
                append(if (isChecked) "☑ " else "☐ ")
            }

            // Parse inline bold and italic
            // Pattern matches **bold** or *italic*
            var searchIndex = 0
            while (searchIndex < currentLine.length) {
                val boldIdx = currentLine.indexOf("**", searchIndex)
                val italicIdx = currentLine.indexOf("*", searchIndex)

                if (boldIdx != -1 && (italicIdx == -1 || boldIdx <= italicIdx)) {
                    // Append before bold
                    append(currentLine.substring(searchIndex, boldIdx))
                    val boldEnd = currentLine.indexOf("**", boldIdx + 2)
                    if (boldEnd != -1) {
                        val boldStart = length
                        append(currentLine.substring(boldIdx + 2, boldEnd))
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor), boldStart, length)
                        searchIndex = boldEnd + 2
                    } else {
                        append(currentLine.substring(boldIdx))
                        break
                    }
                } else if (italicIdx != -1) {
                    append(currentLine.substring(searchIndex, italicIdx))
                    val italicEnd = currentLine.indexOf("*", italicIdx + 1)
                    if (italicEnd != -1) {
                        val italicStart = length
                        append(currentLine.substring(italicIdx + 1, italicEnd))
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor), italicStart, length)
                        searchIndex = italicEnd + 1
                    } else {
                        append(currentLine.substring(italicIdx))
                        break
                    }
                } else {
                    append(currentLine.substring(searchIndex))
                    break
                }
            }

            val lineEnd = length

            // Apply line-level styles
            if (isHeading) {
                val size = if (headingLevel == 1) 22.sp else 18.sp
                addStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = size, color = baseColor), lineStart, lineEnd)
            } else if (isQuote) {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = DeepAnchorNavy800), lineStart, lineEnd)
            } else if (isNumbered) {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = accentColor), lineStart, (lineStart + numberedPrefix.length).coerceAtMost(lineEnd))
            } else if (isChecked) {
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = DeepAnchorNavy600.copy(alpha = 0.5f)), lineStart, lineEnd)
            }

            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

/**
 * Preview Composable rendering formatted rich text note without raw markdown symbols.
 */
@Composable
fun RichMarkdownPreview(
    content: String,
    placeholder: String = "No reflection written yet...",
    modifier: Modifier = Modifier
) {
    if (content.isBlank()) {
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyLarge,
            color = DeepAnchorNavy600.copy(alpha = 0.45f),
            fontStyle = FontStyle.Italic,
            modifier = modifier.padding(16.dp)
        )
    } else {
        val styled = parseMarkdownToAnnotatedString(content)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = styled,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    lineHeight = 28.sp,
                    color = DeepAnchorNavy900
                )
            )
        }
    }
}

/**
 * Smart formatting operations for editor toolbar.
 */
object RichEditorActions {

    fun toggleBold(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val sel = current.selection
        val selMin = sel.min
        val selMax = sel.max
        if (sel.collapsed) {
            val placeholder = "bold text"
            val newText = text.substring(0, selMin) + "**$placeholder**" + text.substring(selMax)
            val newStart = selMin + 2
            val newEnd = newStart + placeholder.length
            return TextFieldValue(newText, TextRange(newStart, newEnd))
        }

        val selected = text.substring(selMin, selMax)
        return if (selected.startsWith("**") && selected.endsWith("**") && selected.length >= 4) {
            // Un-bold
            val unwrapped = selected.substring(2, selected.length - 2)
            val newText = text.substring(0, selMin) + unwrapped + text.substring(selMax)
            TextFieldValue(newText, TextRange(selMin, selMin + unwrapped.length))
        } else {
            // Bold
            val wrapped = "**$selected**"
            val newText = text.substring(0, selMin) + wrapped + text.substring(selMax)
            TextFieldValue(newText, TextRange(selMin + 2, selMax + 2))
        }
    }

    fun toggleItalic(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val sel = current.selection
        val selMin = sel.min
        val selMax = sel.max
        if (sel.collapsed) {
            val placeholder = "italic text"
            val newText = text.substring(0, selMin) + "*$placeholder*" + text.substring(selMax)
            val newStart = selMin + 1
            val newEnd = newStart + placeholder.length
            return TextFieldValue(newText, TextRange(newStart, newEnd))
        }

        val selected = text.substring(selMin, selMax)
        return if (selected.startsWith("*") && selected.endsWith("*") && !selected.startsWith("**") && selected.length >= 2) {
            // Un-italic
            val unwrapped = selected.substring(1, selected.length - 1)
            val newText = text.substring(0, selMin) + unwrapped + text.substring(selMax)
            TextFieldValue(newText, TextRange(selMin, selMin + unwrapped.length))
        } else {
            // Italic
            val wrapped = "*$selected*"
            val newText = text.substring(0, selMin) + wrapped + text.substring(selMax)
            TextFieldValue(newText, TextRange(selMin + 1, selMax + 1))
        }
    }

    fun toggleHeading(current: TextFieldValue): TextFieldValue {
        return transformLines(current) { lines ->
            val allHeading = lines.filter { it.isNotBlank() }.isNotEmpty() &&
                lines.filter { it.isNotBlank() }.all { Regex("^\\s*#{1,3}\\s+").containsMatchIn(it) }
            if (allHeading) {
                lines.map { line ->
                    if (line.isBlank()) line else {
                        val (indent, clean) = getIndentAndCleanLine(line)
                        "$indent$clean"
                    }
                }
            } else {
                lines.map { line ->
                    if (line.isEmpty() && lines.size == 1) "# " else if (line.isBlank()) line else {
                        val (indent, clean) = getIndentAndCleanLine(line)
                        "$indent# $clean"
                    }
                }
            }
        }
    }

    fun toggleBullet(current: TextFieldValue): TextFieldValue {
        return transformLines(current) { lines ->
            val allBullet = lines.filter { it.isNotBlank() }.isNotEmpty() &&
                lines.filter { it.isNotBlank() }.all { Regex("^\\s*[•\\-]\\s+").containsMatchIn(it) }
            if (allBullet) {
                lines.map { line ->
                    if (line.isBlank()) line else {
                        val (indent, clean) = getIndentAndCleanLine(line)
                        "$indent$clean"
                    }
                }
            } else {
                lines.map { line ->
                    if (line.isEmpty() && lines.size == 1) "• " else if (line.isBlank()) line else {
                        val (indent, clean) = getIndentAndCleanLine(line)
                        "$indent• $clean"
                    }
                }
            }
        }
    }

    fun toggleNumbered(current: TextFieldValue): TextFieldValue {
        return transformLines(current) { lines ->
            val allNumbered = lines.filter { it.isNotBlank() }.isNotEmpty() &&
                lines.filter { it.isNotBlank() }.all { Regex("^\\s*\\d+\\.\\s+").containsMatchIn(it) }
            if (allNumbered) {
                lines.map { line ->
                    if (line.isBlank()) line else {
                        val (indent, clean) = getIndentAndCleanLine(line)
                        "$indent$clean"
                    }
                }
            } else {
                var num = 1
                lines.map { line ->
                    if (line.isEmpty() && lines.size == 1) "1. " else if (line.isBlank()) line else {
                        val (indent, clean) = getIndentAndCleanLine(line)
                        "$indent${num++}. $clean"
                    }
                }
            }
        }
    }

    fun toggleQuote(current: TextFieldValue): TextFieldValue {
        return transformLines(current) { lines ->
            val allQuote = lines.filter { it.isNotBlank() }.isNotEmpty() &&
                lines.filter { it.isNotBlank() }.all { Regex("^\\s*>\\s*").containsMatchIn(it) }
            if (allQuote) {
                lines.map { line ->
                    if (line.isBlank()) line else {
                        val (indent, clean) = getIndentAndCleanLine(line)
                        "$indent$clean"
                    }
                }
            } else {
                lines.map { line ->
                    if (line.isEmpty() && lines.size == 1) "> " else if (line.isBlank()) line else {
                        val (indent, clean) = getIndentAndCleanLine(line)
                        "$indent> $clean"
                    }
                }
            }
        }
    }

    fun toggleChecklist(current: TextFieldValue): TextFieldValue {
        return transformLines(current) { lines ->
            val allChecklist = lines.filter { it.isNotBlank() }.isNotEmpty() &&
                lines.filter { it.isNotBlank() }.all { Regex("^\\s*\\[[ xX]\\]\\s+").containsMatchIn(it) }
            if (allChecklist) {
                lines.map { line ->
                    if (line.isBlank()) line else {
                        val (indent, clean) = getIndentAndCleanLine(line)
                        "$indent$clean"
                    }
                }
            } else {
                lines.map { line ->
                    if (line.isEmpty() && lines.size == 1) "[ ] " else if (line.isBlank()) line else {
                        val (indent, clean) = getIndentAndCleanLine(line)
                        "$indent[ ] $clean"
                    }
                }
            }
        }
    }

    /**
     * Intercepts newline insertions inside Compose TextField to:
     * 1. Continue numbered lists with sequential numbers (e.g. 1. -> 2. -> 3.)
     * 2. Continue bullets, checklists, and quotes automatically.
     * 3. Cleanly exit lists and remove empty prefixes when pressing Enter on an empty list item.
     */
    fun handleNewlineOrValueChange(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
        // Intercept when a single newline was inserted
        if (newValue.text.length != oldValue.text.length - oldValue.selection.length + 1) {
            return newValue
        }
        val selMin = oldValue.selection.min
        if (selMin < 0 || selMin >= newValue.text.length) {
            return newValue
        }
        if (newValue.text[selMin] != '\n') {
            return newValue
        }

        val nlPos = selMin
        val prevLineStart = newValue.text.lastIndexOf('\n', (nlPos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val prevLine = newValue.text.substring(prevLineStart, nlPos)

        // 1. Numbered list: e.g. "1. " or "  2. Item"
        val numMatch = Regex("^(\\s*)(\\d+)\\.\\s*(.*)$").matchEntire(prevLine)
        if (numMatch != null) {
            val indent = numMatch.groupValues[1]
            val num = numMatch.groupValues[2].toLongOrNull() ?: 1L
            val content = numMatch.groupValues[3]
            return if (content.isBlank()) {
                // Empty item: exit list by removing prefix
                val newText = newValue.text.substring(0, prevLineStart) + newValue.text.substring((nlPos + 1).coerceAtMost(newValue.text.length))
                TextFieldValue(newText, TextRange(prevLineStart))
            } else {
                val nextPrefix = "$indent${num + 1}. "
                val newText = newValue.text.substring(0, nlPos + 1) + nextPrefix + newValue.text.substring(nlPos + 1)
                val newCursor = nlPos + 1 + nextPrefix.length
                TextFieldValue(newText, TextRange(newCursor))
            }
        }

        // 2. Bullet list: e.g. "• " or "- "
        val bulletMatch = Regex("^(\\s*)([•\\-])\\s*(.*)$").matchEntire(prevLine)
        if (bulletMatch != null) {
            val indent = bulletMatch.groupValues[1]
            val marker = bulletMatch.groupValues[2]
            val content = bulletMatch.groupValues[3]
            return if (content.isBlank()) {
                val newText = newValue.text.substring(0, prevLineStart) + newValue.text.substring((nlPos + 1).coerceAtMost(newValue.text.length))
                TextFieldValue(newText, TextRange(prevLineStart))
            } else {
                val nextPrefix = "$indent$marker "
                val newText = newValue.text.substring(0, nlPos + 1) + nextPrefix + newValue.text.substring(nlPos + 1)
                val newCursor = nlPos + 1 + nextPrefix.length
                TextFieldValue(newText, TextRange(newCursor))
            }
        }

        // 3. Checklist: e.g. "[ ] " or "[x] "
        val checkMatch = Regex("^(\\s*)\\[([ xX])\\]\\s*(.*)$").matchEntire(prevLine)
        if (checkMatch != null) {
            val indent = checkMatch.groupValues[1]
            val content = checkMatch.groupValues[3]
            return if (content.isBlank()) {
                val newText = newValue.text.substring(0, prevLineStart) + newValue.text.substring((nlPos + 1).coerceAtMost(newValue.text.length))
                TextFieldValue(newText, TextRange(prevLineStart))
            } else {
                val nextPrefix = "$indent[ ] "
                val newText = newValue.text.substring(0, nlPos + 1) + nextPrefix + newValue.text.substring(nlPos + 1)
                val newCursor = nlPos + 1 + nextPrefix.length
                TextFieldValue(newText, TextRange(newCursor))
            }
        }

        // 4. Quote: e.g. "> "
        val quoteMatch = Regex("^(\\s*)>\\s*(.*)$").matchEntire(prevLine)
        if (quoteMatch != null) {
            val indent = quoteMatch.groupValues[1]
            val content = quoteMatch.groupValues[2]
            return if (content.isBlank()) {
                val newText = newValue.text.substring(0, prevLineStart) + newValue.text.substring((nlPos + 1).coerceAtMost(newValue.text.length))
                TextFieldValue(newText, TextRange(prevLineStart))
            } else {
                val nextPrefix = "$indent> "
                val newText = newValue.text.substring(0, nlPos + 1) + nextPrefix + newValue.text.substring(nlPos + 1)
                val newCursor = nlPos + 1 + nextPrefix.length
                TextFieldValue(newText, TextRange(newCursor))
            }
        }

        return newValue
    }

    private fun getIndentAndCleanLine(line: String): Pair<String, String> {
        val indent = line.takeWhile { it == ' ' || it == '\t' }
        val trimmed = line.substring(indent.length)
        val stripped = trimmed
            .replace(Regex("^#{1,3}\\s+"), "")
            .replace(Regex("^>\\s*"), "")
            .replace(Regex("^[•\\-]\\s+"), "")
            .replace(Regex("^\\d+\\.\\s+"), "")
            .replace(Regex("^\\[[ xX]\\]\\s+"), "")
        return Pair(indent, stripped)
    }

    private fun transformLines(
        current: TextFieldValue,
        transform: (lines: List<String>) -> List<String>
    ): TextFieldValue {
        val text = current.text
        val sel = current.selection
        val selMin = sel.min
        val selMax = sel.max

        val blockStart = if (selMin == 0) 0 else text.lastIndexOf('\n', (selMin - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val searchEnd = if (selMax > selMin && selMax > 0 && text[selMax - 1] == '\n') selMax - 1 else selMax
        val blockEnd = text.indexOf('\n', searchEnd).let { if (it == -1) text.length else it }

        val selectedBlock = text.substring(blockStart, blockEnd)
        val lines = selectedBlock.split('\n')
        val transformedLines = transform(lines)
        val newBlock = transformedLines.joinToString("\n")

        val newText = text.substring(0, blockStart) + newBlock + text.substring(blockEnd)

        val newSelection = if (sel.collapsed) {
            val offsetDelta = newBlock.length - selectedBlock.length
            val targetPos = (sel.start + offsetDelta).coerceIn(blockStart, blockStart + newBlock.length)
            TextRange(targetPos)
        } else {
            TextRange(blockStart, blockStart + newBlock.length)
        }

        return TextFieldValue(newText, newSelection)
    }
}
