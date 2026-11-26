package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamTest
import com.example.data.model.TestAttempt
import com.example.domain.ExamScoringEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale

enum class PdfExportMode(val label: String) {
    COMPLETE_TEST("Complete Test PDF"),
    QUESTION_PAPER("Question Paper PDF"),
    ANSWER_KEY("Answer Key PDF"),
    MY_ATTEMPT("My Attempt PDF")
}

/**
 * Generates multi-page Unicode Hindi + English PDF documents from actual Firestore test & attempt data
 * using native Android PdfDocument + StaticLayout (zero cut-off text, clean pagination, page numbers).
 */
object TestPdfGenerator {

    private const val PAGE_WIDTH = 595   // A4 width in pt
    private const val PAGE_HEIGHT = 842  // A4 height in pt
    private const val MARGIN_X = 36f
    private const val MARGIN_TOP = 42f
    private const val MARGIN_BOTTOM = 44f
    private const val CONTENT_WIDTH = (PAGE_WIDTH - 72)

    suspend fun generateAndExportAsync(
        context: Context,
        test: ExamTest,
        attempt: TestAttempt?,
        aspirantName: String,
        mode: PdfExportMode,
        isAdmin: Boolean = false,
        shareAfterSave: Boolean = true
    ): Result<Uri> {
        val result = withContext(Dispatchers.IO) {
            generateAndExport(
                context = context,
                test = test,
                attempt = attempt,
                aspirantName = aspirantName,
                mode = mode,
                isAdmin = isAdmin,
                shareAfterSave = false
            )
        }
        if (shareAfterSave && result.isSuccess) {
            val shareUri = result.getOrThrow()
            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    putExtra(Intent.EXTRA_SUBJECT, "TRE 4.0 — ${test.title} (${mode.label})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share ${mode.label}").apply {
                    if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        }
        return result
    }

    fun generateAndExport(
        context: Context,
        test: ExamTest,
        attempt: TestAttempt?,
        aspirantName: String,
        mode: PdfExportMode,
        isAdmin: Boolean = false,
        shareAfterSave: Boolean = true
    ): Result<Uri> {
        val solutionsReleased = ExamScoringEngine.areSolutionsReleased(test, isAdmin)
        if (!solutionsReleased && mode == PdfExportMode.ANSWER_KEY) {
            return Result.failure(IllegalStateException("Answer Key is scheduled for later release and cannot be exported yet."))
        }
        val document = PdfDocument()
        return try {
            val questions = test.parsedQuestions()
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var cursorY = MARGIN_TOP

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(11, 31, 58)
                textSize = 18f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(2, 132, 199)
                textSize = 11.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 34, 58)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val boldBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 34, 58)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val mutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(224, 242, 254)
                style = Paint.Style.FILL
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(186, 230, 253)
                strokeWidth = 1f
            }

            fun drawFooter() {
                val footerText = "TRE 4.0 • Founded by Gopal Gupta   |   ${mode.label}   |   Page $pageNumber"
                canvas.drawLine(MARGIN_X, PAGE_HEIGHT - 30f, PAGE_WIDTH - MARGIN_X, PAGE_HEIGHT - 30f, linePaint)
                canvas.drawText(footerText, MARGIN_X, PAGE_HEIGHT - 16f, mutedPaint)
            }

            fun ensureSpace(neededHeight: Float) {
                if (cursorY + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    drawFooter()
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    cursorY = MARGIN_TOP
                }
            }

            fun drawWrapped(text: String, paint: TextPaint, indent: Float = 0f, extraBottom: Float = 4f) {
                val width = (CONTENT_WIDTH - indent.toInt()).coerceAtLeast(100)
                val layout = StaticLayout.Builder
                    .obtain(text, 0, text.length, paint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(2f, 1.05f)
                    .build()
                ensureSpace(layout.height + extraBottom)
                canvas.save()
                canvas.translate(MARGIN_X + indent, cursorY)
                layout.draw(canvas)
                canvas.restore()
                cursorY += layout.height + extraBottom
            }

            // Dynamic Header Banner (measured height prevents multi-line title overlap)
            val dateStr = IstClock.formatDate(attempt?.submittedAt?.toDate() ?: Date(), "dd MMM yyyy, hh:mm a 'IST'")
            val titleStr = "TRE 4.0 — ${test.title}"
            val subStr = "Aspirant Test & Ranking Platform • Founded by Gopal Gupta"
            val metaStr = "Document: ${mode.label}  |  Creator: ${test.creatorName}  |  Aspirant: $aspirantName  |  Date: $dateStr"
            val headerW = (CONTENT_WIDTH - 24).coerceAtLeast(100)
            val l1 = StaticLayout.Builder.obtain(titleStr, 0, titleStr.length, titlePaint, headerW).setLineSpacing(2f, 1.05f).build()
            val l2 = StaticLayout.Builder.obtain(subStr, 0, subStr.length, subPaint, headerW).setLineSpacing(2f, 1.05f).build()
            val l3 = StaticLayout.Builder.obtain(metaStr, 0, metaStr.length, mutedPaint, headerW).setLineSpacing(2f, 1.05f).build()
            val bannerHeight = l1.height + l2.height + l3.height + 24f
            canvas.drawRoundRect(MARGIN_X, cursorY - 8f, PAGE_WIDTH - MARGIN_X, cursorY + bannerHeight, 12f, 12f, accentPaint)
            drawWrapped(titleStr, titlePaint, indent = 12f, extraBottom = 2f)
            drawWrapped(subStr, subPaint, indent = 12f, extraBottom = 4f)
            drawWrapped(metaStr, mutedPaint, indent = 12f, extraBottom = 18f)

            // Score Summary Block (for Complete Test or My Attempt)
            if (attempt != null && (mode == PdfExportMode.COMPLETE_TEST || mode == PdfExportMode.MY_ATTEMPT)) {
                val accuracy = ExamScoringEngine.computeAccuracy(
                    attempt.correctCount.toInt(),
                    attempt.incorrectCount.toInt()
                )
                drawWrapped("RESULT SUMMARY, ACCURACY & NEGATIVE MARKING BREAKDOWN", subPaint, extraBottom = 4f)
                drawWrapped(
                    "Total Score: ${ExamScoringEngine.formatScore(attempt.totalScore)} / ${ExamScoringEngine.formatScore(attempt.maxMarks)}  (${String.format(Locale.US, "%.2f%%", attempt.percentage)})  |  Accuracy (A-D): ${String.format(Locale.US, "%.2f%%", accuracy)}  |  Negative: -${ExamScoringEngine.formatScore(attempt.negativeDeducted)}",
                    boldBodyPaint,
                    extraBottom = 3f
                )
                drawWrapped(
                    "Correct (+1): ${attempt.correctCount}   |   Incorrect (-1/3): ${attempt.incorrectCount}   |   Option E Not Attempted (0): ${attempt.dontKnowCount}   |   Blank (-1/3): ${attempt.notAttemptedCount}",
                    bodyPaint,
                    extraBottom = 4f
                )
                if (attempt.subjectScores.isNotEmpty()) {
                    val subjectLine = attempt.subjectScores.entries.joinToString("  •  ") {
                        "${it.key}: ${ExamScoringEngine.formatScore(it.value)}"
                    }
                    drawWrapped("Subject-wise Score: $subjectLine", mutedPaint, extraBottom = 10f)
                }
                canvas.drawLine(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY, linePaint)
                cursorY += 10f
            }

            // Render Questions
            questions.forEachIndexed { idx, q: ExamQuestion ->
                val userChoice = attempt?.answers?.get(idx.toString())
                val statusLabel = when {
                    userChoice.isNullOrBlank() -> "BLANK / UNSELECTED (-1/3)"
                    userChoice.equals("E", ignoreCase = true) -> "OPTION E - NOT ATTEMPTED (0)"
                    !solutionsReleased -> "RECORDED"
                    userChoice.equals(q.correctOption, ignoreCase = true) -> "CORRECT (+1)"
                    else -> "INCORRECT (-1/3)"
                }

                if (mode == PdfExportMode.ANSWER_KEY) {
                    drawWrapped(
                        "Q${idx + 1}. [${q.subject}] Correct: (${q.correctOption})  —  ${q.explanation.ifBlank { "Verified answer key." }}",
                        bodyPaint,
                        extraBottom = 6f
                    )
                } else {
                    if (q.passage.isNotBlank()) {
                        drawWrapped("Passage: ${q.passage}", mutedPaint, extraBottom = 3f)
                    }
                    drawWrapped("Q${idx + 1}. [${q.subject}${if (q.source.isNotBlank()) " • ${q.source}" else ""}] ${q.text}", boldBodyPaint, extraBottom = 3f)
                    q.optionsList().forEach { (optKey, optText) ->
                        val paint = if (optKey == "E") mutedPaint else bodyPaint
                        drawWrapped("$optKey) $optText", paint, indent = 12f, extraBottom = if (optKey == "E") 3f else 2f)
                    }

                    if (mode == PdfExportMode.COMPLETE_TEST || mode == PdfExportMode.MY_ATTEMPT) {
                        val chosenDisplay = userChoice ?: "None"
                        val keyText = if (solutionsReleased) "Correct Answer: ${q.correctOption}" else "Answer Key: Scheduled"
                        drawWrapped(
                            "Your Answer: $chosenDisplay   |   $keyText   |   Status: $statusLabel",
                            subPaint,
                            indent = 12f,
                            extraBottom = 2f
                        )
                    }
                    if (solutionsReleased && mode == PdfExportMode.COMPLETE_TEST && q.explanation.isNotBlank()) {
                        drawWrapped("Explanation: ${q.explanation}", mutedPaint, indent = 12f, extraBottom = 8f)
                    } else {
                        cursorY += 6f
                    }
                }
            }

            drawFooter()
            document.finishPage(page)

            val safeName = "TRE4_${test.title.replace(Regex("[^a-zA-Z0-9]+"), "_").take(28)}_${mode.name}_${System.currentTimeMillis()}.pdf"
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val pdfFile = File(exportDir, safeName)
            FileOutputStream(pdfFile).use { fos -> document.writeTo(fos) }

            // Also copy into public Downloads on Android 10+ (zero permissions required via MediaStore!)
            saveToDownloads(context, pdfFile, safeName, "application/pdf")

            val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            if (shareAfterSave) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    putExtra(Intent.EXTRA_SUBJECT, "TRE 4.0 — ${test.title} (${mode.label})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share ${mode.label}").apply {
                    if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
            Result.success(shareUri)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { document.close() }
        }
    }

    private fun saveToDownloads(context: Context, sourceFile: File, fileName: String, mimeType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/TRE4")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { inp -> inp.copyTo(out) }
                    }
                }
            } catch (_: Exception) {
                // Cache copy + FileProvider remains available even if MediaStore rejects
            }
        }
    }
}
