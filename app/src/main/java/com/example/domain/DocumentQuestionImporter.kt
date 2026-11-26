package com.example.domain

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.ExamQuestion
import com.example.data.model.PaperType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.Inflater
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory

enum class ImportFileFormat(
    val label: String,
    val extensionHint: String,
    val mimeTypes: Array<String>
) {
    PDF(
        label = "PDF Upload",
        extensionHint = ".pdf",
        mimeTypes = arrayOf("application/pdf")
    ),
    DOC(
        label = "DOC Upload",
        extensionHint = ".doc",
        mimeTypes = arrayOf(
            "application/msword",
            "application/rtf",
            "text/rtf",
            "application/octet-stream"
        )
    ),
    DOCX(
        label = "DOCX Upload",
        extensionHint = ".docx",
        mimeTypes = arrayOf(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/zip",
            "application/octet-stream"
        )
    ),
    JSON(
        label = "JSON Import",
        extensionHint = ".json",
        mimeTypes = arrayOf("application/json", "text/plain", "application/octet-stream")
    ),
    CSV(
        label = "CSV Import",
        extensionHint = ".csv",
        mimeTypes = arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel")
    )
}

data class DocumentImportOutcome(
    val fileName: String,
    val format: ImportFileFormat,
    val extractedText: String,
    val questions: List<ExamQuestion>,
    val errorMessage: String? = null,
    val warningMessage: String? = null
)

/**
 * Pure-Kotlin, zero-external-dependency document extractor & MCQ importer for TRE 4.0.
 * Supports PDF, DOC, DOCX, JSON, and CSV via Storage Access Framework (zero storage permissions).
 */
object DocumentQuestionImporter {

    private const val MAX_FILE_BYTES = 12 * 1024 * 1024 // 12 MB safety ceiling

    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        requestedFormat: ImportFileFormat,
        targetPaperType: PaperType
    ): DocumentImportOutcome = withContext(Dispatchers.IO) {
        val fileName = resolveDisplayName(context, uri) ?: "uploaded_file${requestedFormat.extensionHint}"
        val effectiveFormat = inferFormat(fileName, requestedFormat)
        try {
            val bytes = readBoundedBytes(context, uri)
                ?: return@withContext DocumentImportOutcome(
                    fileName = fileName,
                    format = effectiveFormat,
                    extractedText = "",
                    questions = emptyList(),
                    errorMessage = "Could not open '$fileName'. Please select a valid local or cloud document."
                )

            if (bytes.isEmpty()) {
                return@withContext DocumentImportOutcome(
                    fileName = fileName,
                    format = effectiveFormat,
                    extractedText = "",
                    questions = emptyList(),
                    errorMessage = "The selected file '$fileName' is empty."
                )
            }

            importFromBytes(bytes, fileName, effectiveFormat, targetPaperType)
        } catch (e: Exception) {
            DocumentImportOutcome(
                fileName = fileName,
                format = effectiveFormat,
                extractedText = "",
                questions = emptyList(),
                errorMessage = e.localizedMessage ?: "Failed to read '$fileName'."
            )
        }
    }

    fun importFromBytes(
        bytes: ByteArray,
        fileName: String,
        format: ImportFileFormat,
        targetPaperType: PaperType
    ): DocumentImportOutcome {
        return when (format) {
            ImportFileFormat.JSON -> {
                val text = decodeUtf8WithBomStrip(bytes)
                val parsed = BulkQuestionParser.parseJson(text, targetPaperType)
                parsed.fold(
                    onSuccess = { list ->
                        DocumentImportOutcome(
                            fileName = fileName,
                            format = format,
                            extractedText = text,
                            questions = list,
                            warningMessage = if (list.isEmpty()) "JSON parsed cleanly, but contained 0 questions." else null
                        )
                    },
                    onFailure = { err ->
                        DocumentImportOutcome(
                            fileName = fileName,
                            format = format,
                            extractedText = text,
                            questions = emptyList(),
                            errorMessage = err.message ?: "Invalid JSON structure."
                        )
                    }
                )
            }
            ImportFileFormat.CSV -> {
                val text = decodeUtf8WithBomStrip(bytes)
                val list = BulkQuestionParser.parseCsv(text, targetPaperType)
                DocumentImportOutcome(
                    fileName = fileName,
                    format = format,
                    extractedText = text,
                    questions = list,
                    errorMessage = if (list.isEmpty()) "No valid CSV MCQ rows found. Ensure header + at least 8 columns (subject,topic,source,text,A,B,C,D,E,answer,explanation)." else null
                )
            }
            ImportFileFormat.DOCX -> {
                val extracted = extractFromDocxBytes(bytes)
                extracted.fold(
                    onSuccess = { text ->
                        val parsed = BulkQuestionParser.parseBulkText(text, targetPaperType)
                        DocumentImportOutcome(
                            fileName = fileName,
                            format = format,
                            extractedText = text,
                            questions = parsed,
                            warningMessage = if (parsed.isEmpty()) {
                                "Text was extracted from '$fileName' (${text.length} chars), but no numbered MCQs matched automatically. Review and adjust the extracted text below."
                            } else null
                        )
                    },
                    onFailure = { err ->
                        DocumentImportOutcome(
                            fileName = fileName,
                            format = format,
                            extractedText = "",
                            questions = emptyList(),
                            errorMessage = err.message ?: "Unable to read DOCX file."
                        )
                    }
                )
            }
            ImportFileFormat.DOC -> {
                val extracted = extractFromDocBytes(bytes)
                extracted.fold(
                    onSuccess = { text ->
                        val parsed = BulkQuestionParser.parseBulkText(text, targetPaperType)
                        DocumentImportOutcome(
                            fileName = fileName,
                            format = format,
                            extractedText = text,
                            questions = parsed,
                            warningMessage = if (parsed.isEmpty()) {
                                "Text extracted from '$fileName' (${text.length} chars). Review and format numbered MCQs (1. ... A) ... B) ... Answer: B) before importing."
                            } else null
                        )
                    },
                    onFailure = { err ->
                        DocumentImportOutcome(
                            fileName = fileName,
                            format = format,
                            extractedText = "",
                            questions = emptyList(),
                            errorMessage = err.message ?: "Unable to read DOC file."
                        )
                    }
                )
            }
            ImportFileFormat.PDF -> {
                val extracted = extractFromPdfBytes(bytes)
                extracted.fold(
                    onSuccess = { text ->
                        val parsed = BulkQuestionParser.parseBulkText(text, targetPaperType)
                        DocumentImportOutcome(
                            fileName = fileName,
                            format = format,
                            extractedText = text,
                            questions = parsed,
                            warningMessage = if (parsed.isEmpty()) {
                                "Text extracted from PDF '${fileName}' (${text.length} chars), but MCQ blocks need minor cleanup. Review & re-parse below."
                            } else null
                        )
                    },
                    onFailure = { err ->
                        DocumentImportOutcome(
                            fileName = fileName,
                            format = format,
                            extractedText = "",
                            questions = emptyList(),
                            errorMessage = err.message ?: "Unable to extract text from PDF."
                        )
                    }
                )
            }
        }
    }

    // ==================== DOCX EXTRACTOR (OpenXML ZIP + word/document.xml) ====================

    fun extractFromDocxBytes(bytes: ByteArray): Result<String> {
        if (bytes.size < 4 || bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) {
            return Result.failure(IllegalArgumentException("Not a valid DOCX archive (missing PK ZIP header)."))
        }
        return try {
            var documentXmlBytes: ByteArray? = null
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.equals("word/document.xml", ignoreCase = true)) {
                        documentXmlBytes = zis.readBytes()
                        break
                    }
                    entry = zis.nextEntry
                }
            }
            val xmlBytes = documentXmlBytes
                ?: return Result.failure(IllegalArgumentException("DOCX archive does not contain word/document.xml."))

            val factory = SAXParserFactory.newInstance().apply {
                isNamespaceAware = false
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            }
            val parser = factory.newSAXParser()
            val out = StringBuilder()
            val paraBuilder = StringBuilder()
            var inText = false

            val handler = object : DefaultHandler() {
                override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                    val tag = (qName ?: localName ?: "").substringAfter(':')
                    when (tag) {
                        "p" -> paraBuilder.clear()
                        "t" -> inText = true
                        "br", "cr" -> paraBuilder.append('\n')
                        "tab" -> paraBuilder.append(' ')
                    }
                }

                override fun characters(ch: CharArray?, start: Int, length: Int) {
                    if (inText && ch != null && length > 0) {
                        paraBuilder.append(ch, start, length)
                    }
                }

                override fun endElement(uri: String?, localName: String?, qName: String?) {
                    val tag = (qName ?: localName ?: "").substringAfter(':')
                    when (tag) {
                        "t" -> inText = false
                        "p" -> {
                            val line = paraBuilder.toString().trim()
                            if (line.isNotEmpty()) {
                                if (out.isNotEmpty()) out.append('\n')
                                out.append(line)
                            }
                            paraBuilder.clear()
                        }
                    }
                }
            }

            parser.parse(InputSource(InputStreamReader(ByteArrayInputStream(xmlBytes), StandardCharsets.UTF_8)), handler)
            val text = out.toString().trim()
            if (text.isBlank()) {
                Result.failure(IllegalArgumentException("DOCX document contained no readable text."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("DOCX parsing error: ${e.message}"))
        }
    }

    // ==================== DOC EXTRACTOR (DOCX-alias, RTF, Plain Text, Legacy OLE2) ====================

    fun extractFromDocBytes(bytes: ByteArray): Result<String> {
        if (bytes.isEmpty()) return Result.failure(IllegalArgumentException("DOC file is empty."))

        // Case 1: Actually a DOCX saved with .doc extension
        if (bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
            return extractFromDocxBytes(bytes)
        }

        val headerAscii = String(bytes, 0, minOf(bytes.size, 64), StandardCharsets.ISO_8859_1)

        // Case 2: RTF saved as .doc (very common from Word / LibreOffice)
        if (headerAscii.trimStart().startsWith("{\\rtf", ignoreCase = true)) {
            val rtfText = decodeRtf(String(bytes, StandardCharsets.ISO_8859_1))
            return if (rtfText.isNotBlank()) Result.success(rtfText)
            else Result.failure(IllegalArgumentException("RTF DOC contained no readable text."))
        }

        // Case 3: Legacy Word 97-2003 OLE2 binary (.doc: D0 CF 11 E0 A1 B1 1A E1)
        val isOle2 = bytes.size >= 8 &&
            (bytes[0].toInt() and 0xFF) == 0xD0 &&
            (bytes[1].toInt() and 0xFF) == 0xCF &&
            (bytes[2].toInt() and 0xFF) == 0x11 &&
            (bytes[3].toInt() and 0xFF) == 0xE0

        if (isOle2) {
            val extracted = extractTextFromLegacyOle2Doc(bytes)
            return if (extracted.isNotBlank()) {
                Result.success(extracted)
            } else {
                Result.failure(IllegalArgumentException("Could not extract readable text from this legacy binary .doc file. Please re-save as .docx or .pdf and upload again."))
            }
        }

        // Case 4: Plain UTF-8 / UTF-16 text saved with .doc extension
        val plain = decodeUtf8WithBomStrip(bytes).trim()
        return if (plain.isNotBlank() && plain.count { it.code < 32 && it != '\n' && it != '\r' && it != '\t' } < plain.length / 8) {
            Result.success(plain)
        } else {
            Result.failure(IllegalArgumentException("Unsupported binary .doc encoding. Please save as .docx, .pdf, .json, or .csv."))
        }
    }

    private fun decodeRtf(rtf: String): String {
        val out = StringBuilder()
        var i = 0
        var skipGroupDepth = 0
        var braceDepth = 0
        val skipTargets = setOf("fonttbl", "colortbl", "stylesheet", "info", "pict", "header", "footer")

        while (i < rtf.length) {
            val ch = rtf[i]
            when (ch) {
                '{' -> {
                    braceDepth++
                    if (skipGroupDepth == 0) {
                        val lookahead = rtf.substring(i + 1, minOf(rtf.length, i + 24))
                        if (lookahead.startsWith("\\*") || skipTargets.any { lookahead.startsWith("\\$it") }) {
                            skipGroupDepth = braceDepth
                        }
                    }
                    i++
                }
                '}' -> {
                    if (skipGroupDepth == braceDepth) skipGroupDepth = 0
                    braceDepth = (braceDepth - 1).coerceAtLeast(0)
                    i++
                }
                '\\' -> {
                    i++
                    if (i >= rtf.length) break
                    val next = rtf[i]
                    if (next == '\\' || next == '{' || next == '}') {
                        if (skipGroupDepth == 0) out.append(next)
                        i++
                    } else if (next == '\'' && i + 2 < rtf.length) {
                        val hex = rtf.substring(i + 1, i + 3)
                        val code = hex.toIntOrNull(16)
                        if (skipGroupDepth == 0 && code != null) out.append(code.toChar())
                        i += 3
                    } else if (next == '\n' || next == '\r') {
                        if (skipGroupDepth == 0) out.append('\n')
                        i++
                    } else {
                        val wordStart = i
                        while (i < rtf.length && rtf[i].isLetter()) i++
                        val word = rtf.substring(wordStart, i)
                        val numStart = i
                        if (i < rtf.length && rtf[i] == '-') i++
                        while (i < rtf.length && rtf[i].isDigit()) i++
                        val numStr = rtf.substring(numStart, i)
                        if (i < rtf.length && rtf[i] == ' ') i++

                        if (skipGroupDepth == 0) {
                            when (word) {
                                "par", "line" -> out.append('\n')
                                "tab" -> out.append(' ')
                                "u" -> {
                                    val rawCode = numStr.toIntOrNull()
                                    if (rawCode != null) {
                                        val unicode = if (rawCode < 0) rawCode + 65536 else rawCode
                                        out.append(unicode.toChar())
                                        // Skip fallback replacement char (typically '?')
                                        if (i < rtf.length && rtf[i] == '?') i++
                                    }
                                }
                            }
                        }
                    }
                }
                '\r', '\n' -> i++
                else -> {
                    if (skipGroupDepth == 0) out.append(ch)
                    i++
                }
            }
        }
        return out.toString()
            .replace(Regex("[ \t]+"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun extractTextFromLegacyOle2Doc(bytes: ByteArray): String {
        // Scan for UTF-16LE runs (supports both English and Hindi Devanagari 0x0900..0x097F)
        val utf16Runs = mutableListOf<String>()
        val cur = StringBuilder()
        var idx = 0
        while (idx + 1 < bytes.size) {
            val lo = bytes[idx].toInt() and 0xFF
            val hi = bytes[idx + 1].toInt() and 0xFF
            val code = (hi shl 8) or lo
            val ch = code.toChar()
            if (isWordReadableChar(code)) {
                cur.append(if (code == 0x0D || code == 0x0B) '\n' else ch)
            } else {
                if (cur.length >= 12) {
                    val cleaned = cur.toString().trim()
                    if (isLikelyQuestionText(cleaned)) utf16Runs.add(cleaned)
                }
                cur.clear()
            }
            idx += 2
        }
        if (cur.length >= 12) {
            val cleaned = cur.toString().trim()
            if (isLikelyQuestionText(cleaned)) utf16Runs.add(cleaned)
        }
        if (utf16Runs.isNotEmpty()) {
            return utf16Runs.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
        }

        // Fallback: printable ASCII / CP1252 runs inside WordDocument stream
        val asciiRuns = mutableListOf<String>()
        val sb = StringBuilder()
        for (b in bytes) {
            val code = b.toInt() and 0xFF
            if (code in 0x20..0x7E || code == 0x0D || code == 0x0A || code == 0x09) {
                sb.append(if (code == 0x0D) '\n' else code.toChar())
            } else {
                if (sb.length >= 16) {
                    val s = sb.toString().trim()
                    if (isLikelyQuestionText(s)) asciiRuns.add(s)
                }
                sb.clear()
            }
        }
        if (sb.length >= 16) {
            val s = sb.toString().trim()
            if (isLikelyQuestionText(s)) asciiRuns.add(s)
        }
        return asciiRuns.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun isWordReadableChar(code: Int): Boolean {
        return code in 0x0020..0x007E ||
            code in 0x0900..0x097F || // Devanagari (Hindi)
            code == 0x000A || code == 0x000D || code == 0x0009 || code == 0x000B ||
            code == 0x2013 || code == 0x2014 || code == 0x2018 || code == 0x2019 ||
            code == 0x201C || code == 0x201D || code == 0x20B9 || code == 0x00D7 || code == 0x00B0
    }

    private fun isLikelyQuestionText(text: String): Boolean {
        if (text.length < 8) return false
        val lower = text.lowercase()
        if (lower.startsWith("root entry") || lower.startsWith("worddocument") ||
            lower.startsWith("microsoft word") || lower.startsWith("normal.dot") ||
            lower.startsWith("times new roman") || lower.startsWith("calibri")
        ) return false
        val letterOrDigit = text.count { it.isLetterOrDigit() }
        return letterOrDigit >= text.length / 3
    }

    // ==================== PURE-KOTLIN PDF TEXT EXTRACTOR ====================

    fun extractFromPdfBytes(bytes: ByteArray): Result<String> {
        val header = String(bytes, 0, minOf(bytes.size, 32), StandardCharsets.ISO_8859_1)
        if (!header.contains("%PDF-")) {
            return Result.failure(IllegalArgumentException("Not a valid PDF file (missing %PDF- header)."))
        }

        return try {
            val rawLatin = String(bytes, StandardCharsets.ISO_8859_1)
            val streams = extractAllPdfStreams(bytes, rawLatin)
            if (streams.isEmpty()) {
                return Result.failure(IllegalArgumentException("No readable content streams found in this PDF. Scanned image-only PDFs cannot be text-extracted automatically."))
            }

            // First pass: collect ToUnicode CMap mappings across streams
            val unicodeMap = linkedMapOf<Int, String>()
            streams.forEach { stream ->
                if (stream.contains("beginbfchar") || stream.contains("beginbfrange")) {
                    parseToUnicodeCMap(stream, unicodeMap)
                }
            }

            // Second pass: extract text blocks (BT ... ET) from page content streams
            val pagesOut = StringBuilder()
            streams.forEach { stream ->
                if (stream.contains("BT") && (stream.contains("Tj") || stream.contains("TJ") || stream.contains("'"))) {
                    val pageText = extractTextFromContentStream(stream, unicodeMap)
                    if (pageText.isNotBlank()) {
                        if (pagesOut.isNotEmpty()) pagesOut.append("\n\n")
                        pagesOut.append(pageText)
                    }
                }
            }

            val finalText = pagesOut.toString()
                .replace(Regex("[ \t]+"), " ")
                .replace(Regex(" *\n *"), "\n")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()

            if (finalText.isBlank()) {
                Result.failure(
                    IllegalArgumentException(
                        "No selectable text found in this PDF. It may be a scanned/image-only PDF or use encrypted streams. Please upload a text-based PDF, DOCX, JSON, or CSV."
                    )
                )
            } else {
                Result.success(finalText)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("PDF extraction error: ${e.message}"))
        }
    }

    private fun extractAllPdfStreams(bytes: ByteArray, rawLatin: String): List<String> {
        val results = mutableListOf<String>()
        var searchFrom = 0
        while (searchFrom < rawLatin.length) {
            val streamIdx = rawLatin.indexOf("stream", searchFrom)
            if (streamIdx < 0) break

            // Verify 'stream' is preceded by whitespace or '>' (end of dictionary '>>')
            val prevChar = if (streamIdx > 0) rawLatin[streamIdx - 1] else ' '
            val afterStream = streamIdx + 6
            if (afterStream >= rawLatin.length || (prevChar != '>' && !prevChar.isWhitespace())) {
                searchFrom = afterStream
                continue
            }

            // PDF spec: 'stream' must be followed by \r\n or \n
            var dataStart = afterStream
            if (dataStart < rawLatin.length && rawLatin[dataStart] == '\r') dataStart++
            if (dataStart < rawLatin.length && rawLatin[dataStart] == '\n') {
                dataStart++
            } else {
                searchFrom = afterStream
                continue
            }

            val endStreamIdx = rawLatin.indexOf("endstream", dataStart)
            if (endStreamIdx < 0) break

            var dataEnd = endStreamIdx
            while (dataEnd > dataStart && (bytes[dataEnd - 1] == '\n'.code.toByte() || bytes[dataEnd - 1] == '\r'.code.toByte())) {
                dataEnd--
            }

            val dictStart = (streamIdx - 512).coerceAtLeast(0)
            val dictHeader = rawLatin.substring(dictStart, streamIdx)
            val isFlate = dictHeader.contains("/FlateDecode")
            val isImage = dictHeader.contains("/Subtype /Image") || dictHeader.contains("/Subtype/Image")

            if (!isImage && dataEnd > dataStart) {
                val slice = bytes.copyOfRange(dataStart, dataEnd)
                val decodedBytes = if (isFlate) inflateSafely(slice) else slice
                if (decodedBytes != null && decodedBytes.isNotEmpty()) {
                    results.add(String(decodedBytes, StandardCharsets.ISO_8859_1))
                }
            }

            searchFrom = endStreamIdx + 9
        }
        return results
    }

    private fun inflateSafely(compressed: ByteArray): ByteArray? {
        return try {
            val inflater = Inflater()
            try {
                inflater.setInput(compressed)
                val baos = ByteArrayOutputStream()
                val buf = ByteArray(4096)
                while (!inflater.finished()) {
                    val count = inflater.inflate(buf)
                    if (count == 0) {
                        if (inflater.needsInput() || inflater.needsDictionary()) break
                    } else {
                        baos.write(buf, 0, count)
                        if (baos.size() > MAX_FILE_BYTES) break
                    }
                }
                baos.toByteArray()
            } finally {
                inflater.end()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseToUnicodeCMap(cmap: String, out: MutableMap<Int, String>) {
        // Parse beginbfchar ... endbfchar
        val bfCharRegex = Regex("beginbfchar(.*?)endbfchar", RegexOption.DOT_MATCHES_ALL)
        val pairRegex = Regex("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>")
        bfCharRegex.findAll(cmap).forEach { block ->
            pairRegex.findAll(block.groupValues[1]).forEach { m ->
                val src = m.groupValues[1].toIntOrNull(16) ?: return@forEach
                val dst = hexToUtf16String(m.groupValues[2])
                if (dst.isNotEmpty()) out[src] = dst
            }
        }
        // Parse beginbfrange ... endbfrange
        val bfRangeRegex = Regex("beginbfrange(.*?)endbfrange", RegexOption.DOT_MATCHES_ALL)
        val tripleRegex = Regex("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>")
        bfRangeRegex.findAll(cmap).forEach { block ->
            tripleRegex.findAll(block.groupValues[1]).forEach { m ->
                val start = m.groupValues[1].toIntOrNull(16) ?: return@forEach
                val end = m.groupValues[2].toIntOrNull(16) ?: return@forEach
                val dstStart = m.groupValues[3].toIntOrNull(16) ?: return@forEach
                if (end >= start && (end - start) <= 1024) {
                    for (code in start..end) {
                        out[code] = (dstStart + (code - start)).toChar().toString()
                    }
                }
            }
        }
    }

    private fun extractTextFromContentStream(stream: String, cmap: Map<Int, String>): String {
        val sb = StringBuilder()
        val btRegex = Regex("\\bBT\\b(.*?)\\bET\\b", RegexOption.DOT_MATCHES_ALL)
        btRegex.findAll(stream).forEach { match ->
            val block = match.groupValues[1]
            val tokens = tokenizePdfBlock(block)
            val operandStack = mutableListOf<PdfToken>()

            for (tok in tokens) {
                when (tok) {
                    is PdfToken.Op -> {
                        when (tok.name) {
                            "Tj" -> {
                                val strTok = operandStack.lastOrNull()
                                if (strTok is PdfToken.LiteralStr) sb.append(strTok.value)
                                else if (strTok is PdfToken.HexStr) sb.append(decodePdfHex(strTok.hex, cmap))
                                operandStack.clear()
                            }
                            "'" -> {
                                sb.append('\n')
                                val strTok = operandStack.lastOrNull()
                                if (strTok is PdfToken.LiteralStr) sb.append(strTok.value)
                                else if (strTok is PdfToken.HexStr) sb.append(decodePdfHex(strTok.hex, cmap))
                                operandStack.clear()
                            }
                            "\"" -> {
                                sb.append('\n')
                                val strTok = operandStack.lastOrNull()
                                if (strTok is PdfToken.LiteralStr) sb.append(strTok.value)
                                else if (strTok is PdfToken.HexStr) sb.append(decodePdfHex(strTok.hex, cmap))
                                operandStack.clear()
                            }
                            "TJ" -> {
                                val arr = operandStack.lastOrNull() as? PdfToken.ArrayTok
                                arr?.items?.forEach { item ->
                                    when (item) {
                                        is PdfToken.LiteralStr -> sb.append(item.value)
                                        is PdfToken.HexStr -> sb.append(decodePdfHex(item.hex, cmap))
                                        is PdfToken.Num -> {
                                            // Negative kerning <= -180 typically indicates a word space
                                            if (item.value <= -180.0 && sb.isNotEmpty() && sb.last() != ' ') {
                                                sb.append(' ')
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                                operandStack.clear()
                            }
                            "T*" -> {
                                sb.append('\n')
                                operandStack.clear()
                            }
                            "Td", "TD" -> {
                                val ty = (operandStack.lastOrNull() as? PdfToken.Num)?.value ?: 0.0
                                if (kotlin.math.abs(ty) >= 2.0 && sb.isNotEmpty() && sb.last() != '\n') {
                                    sb.append('\n')
                                } else if (sb.isNotEmpty() && sb.last() != ' ' && sb.last() != '\n') {
                                    sb.append(' ')
                                }
                                operandStack.clear()
                            }
                            "Tm" -> {
                                if (sb.isNotEmpty() && sb.last() != '\n') sb.append('\n')
                                operandStack.clear()
                            }
                            else -> operandStack.clear()
                        }
                    }
                    else -> operandStack.add(tok)
                }
            }
            if (sb.isNotEmpty() && sb.last() != '\n') sb.append('\n')
        }
        return sb.toString()
    }

    private sealed interface PdfToken {
        data class LiteralStr(val value: String) : PdfToken
        data class HexStr(val hex: String) : PdfToken
        data class Num(val value: Double) : PdfToken
        data class ArrayTok(val items: List<PdfToken>) : PdfToken
        data class Op(val name: String) : PdfToken
    }

    private fun tokenizePdfBlock(block: String): List<PdfToken> {
        val tokens = mutableListOf<PdfToken>()
        var i = 0
        val n = block.length

        fun readLiteralString(): PdfToken.LiteralStr {
            i++ // skip '('
            val out = StringBuilder()
            var depth = 1
            while (i < n && depth > 0) {
                val c = block[i++]
                when {
                    c == '\\' && i < n -> {
                        val esc = block[i++]
                        when (esc) {
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            '(', ')', '\\' -> out.append(esc)
                            '\r' -> if (i < n && block[i] == '\n') i++
                            '\n' -> {}
                            in '0'..'7' -> {
                                val oct = StringBuilder().append(esc)
                                repeat(2) {
                                    if (i < n && block[i] in '0'..'7') oct.append(block[i++])
                                }
                                out.append(oct.toString().toInt(8).toChar())
                            }
                            else -> out.append(esc)
                        }
                    }
                    c == '(' -> {
                        depth++
                        out.append(c)
                    }
                    c == ')' -> {
                        depth--
                        if (depth > 0) out.append(c)
                    }
                    else -> out.append(c)
                }
            }
            return PdfToken.LiteralStr(out.toString())
        }

        fun readHexString(): PdfToken.HexStr {
            i++ // skip '<'
            val hex = StringBuilder()
            while (i < n && block[i] != '>') {
                val c = block[i++]
                if (c.isDigit() || c in 'a'..'f' || c in 'A'..'F') hex.append(c)
            }
            if (i < n && block[i] == '>') i++
            return PdfToken.HexStr(hex.toString())
        }

        while (i < n) {
            val c = block[i]
            when {
                c.isWhitespace() -> i++
                c == '%' -> {
                    while (i < n && block[i] != '\n' && block[i] != '\r') i++
                }
                c == '(' -> tokens.add(readLiteralString())
                c == '<' -> {
                    if (i + 1 < n && block[i + 1] == '<') {
                        i += 2
                    } else {
                        tokens.add(readHexString())
                    }
                }
                c == '>' -> i++
                c == '[' -> {
                    i++
                    val items = mutableListOf<PdfToken>()
                    while (i < n && block[i] != ']') {
                        val ch = block[i]
                        when {
                            ch.isWhitespace() -> i++
                            ch == '(' -> items.add(readLiteralString())
                            ch == '<' -> items.add(readHexString())
                            ch == '-' || ch == '+' || ch == '.' || ch.isDigit() -> {
                                val start = i
                                i++
                                while (i < n && (block[i].isDigit() || block[i] == '.')) i++
                                block.substring(start, i).toDoubleOrNull()?.let { items.add(PdfToken.Num(it)) }
                            }
                            else -> i++
                        }
                    }
                    if (i < n && block[i] == ']') i++
                    tokens.add(PdfToken.ArrayTok(items))
                }
                c == '/' -> {
                    i++
                    while (i < n && !block[i].isWhitespace() && block[i] !in "()<>[]{}/%") i++
                }
                c == '-' || c == '+' || c == '.' || c.isDigit() -> {
                    val start = i
                    i++
                    while (i < n && (block[i].isDigit() || block[i] == '.')) i++
                    block.substring(start, i).toDoubleOrNull()?.let { tokens.add(PdfToken.Num(it)) }
                }
                c == '\'' || c == '"' -> {
                    tokens.add(PdfToken.Op(c.toString()))
                    i++
                }
                else -> {
                    val start = i
                    while (i < n && !block[i].isWhitespace() && block[i] !in "()<>[]{}/%") i++
                    if (i > start) tokens.add(PdfToken.Op(block.substring(start, i)))
                }
            }
        }
        return tokens
    }

    private fun decodePdfHex(hex: String, cmap: Map<Int, String>): String {
        if (hex.isEmpty()) return ""
        val padded = if (hex.length % 2 == 1) "${hex}0" else hex
        // UTF-16BE BOM check (FEFF)
        if (padded.startsWith("FEFF", ignoreCase = true) && padded.length >= 8) {
            return hexToUtf16String(padded.substring(4))
        }
        if (cmap.isNotEmpty() && padded.length % 4 == 0) {
            val mapped = StringBuilder()
            var hits = 0
            var j = 0
            while (j + 4 <= padded.length) {
                val code = padded.substring(j, j + 4).toIntOrNull(16)
                val replacement = code?.let { cmap[it] }
                if (replacement != null) {
                    mapped.append(replacement)
                    hits++
                } else if (code != null && code in 0x20..0x7E) {
                    mapped.append(code.toChar())
                }
                j += 4
            }
            if (hits > 0) return mapped.toString()
        }
        // Try 2-byte single-byte CMap or ISO-8859-1
        val out = StringBuilder()
        var j = 0
        while (j + 2 <= padded.length) {
            val code = padded.substring(j, j + 2).toIntOrNull(16) ?: 0
            val mapped = cmap[code]
            if (mapped != null) out.append(mapped)
            else if (code in 0x20..0x7E || code == 0x0A || code == 0x09) out.append(code.toChar())
            j += 2
        }
        return out.toString()
    }

    private fun hexToUtf16String(hex: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i + 4 <= hex.length) {
            val code = hex.substring(i, i + 4).toIntOrNull(16)
            if (code != null) sb.append(code.toChar())
            i += 4
        }
        if (sb.isEmpty() && hex.length == 2) {
            hex.toIntOrNull(16)?.let { sb.append(it.toChar()) }
        }
        return sb.toString()
    }

    // ==================== HELPERS ====================

    private fun decodeUtf8WithBomStrip(bytes: ByteArray): String {
        val raw = String(bytes, StandardCharsets.UTF_8)
        return raw.removePrefix("\uFEFF")
    }

    private fun inferFormat(fileName: String, fallback: ImportFileFormat): ImportFileFormat {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".pdf") -> ImportFileFormat.PDF
            lower.endsWith(".docx") -> ImportFileFormat.DOCX
            lower.endsWith(".doc") || lower.endsWith(".rtf") -> ImportFileFormat.DOC
            lower.endsWith(".json") -> ImportFileFormat.JSON
            lower.endsWith(".csv") -> ImportFileFormat.CSV
            else -> fallback
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    private fun readBoundedBytes(context: Context, uri: Uri): ByteArray? {
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        return stream.use { input ->
            val baos = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(buf)
                if (read <= 0) break
                total += read
                if (total > MAX_FILE_BYTES) {
                    throw IllegalArgumentException("File exceeds 12 MB upload limit.")
                }
                baos.write(buf, 0, read)
            }
            baos.toByteArray()
        }
    }
}
