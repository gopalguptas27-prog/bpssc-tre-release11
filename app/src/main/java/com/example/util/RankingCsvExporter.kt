package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.ExamTest
import com.example.data.model.RankedAspirant
import com.example.data.model.TestAttempt
import com.example.domain.ExamScoringEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Exports set / part rankings and attempt analytics to CSV with strict separation between
 * Merit Marks (Rank base) and Language Qualifying columns (NEVER merged into Merit or Rank).
 * Uses app cache + MediaStore.Downloads (API 29+) + FileProvider — zero storage permissions required.
 */
object RankingCsvExporter {

    fun buildCsvContent(
        scopeTitle: String,
        ranked: List<RankedAspirant>,
        attemptsByUserId: Map<String, TestAttempt> = emptyMap(),
        test: ExamTest? = null
    ): String {
        val sb = StringBuilder()
        sb.appendLine(
            "Rank,User Name,Scope / Set,Merit Score,Merit Max,Merit %,Accuracy %,Percentile,Time (sec),Language Hindi (/22),Language English (/8),Language Total (/30),Language Status (Qualifying Only),Attempted Sets"
        )
        ranked.forEach { entry ->
            val att = attemptsByUserId[entry.member.userId]
            val meritScore = if (entry.testsAttempted > 0) ExamScoringEngine.formatScore(entry.displayScore) else ""
            val meritMax = if (entry.maxMarks > 0.0) entry.maxMarks.toInt().toString() else "0"
            val meritPct = if (entry.testsAttempted > 0 && entry.maxMarks > 0.0) {
                String.format(Locale.US, "%.2f", (entry.displayScore / entry.maxMarks) * 100.0)
            } else ""
            val accStr = if (entry.testsAttempted > 0) String.format(Locale.US, "%.2f", entry.accuracyPercentage) else ""
            val pctStr = if (entry.testsAttempted > 0 && entry.rank > 0) String.format(Locale.US, "%.2f", entry.percentile) else ""
            val timeStr = if (entry.testsAttempted > 0) entry.timeSpentSeconds.toString() else ""

            val showLang = entry.showLanguageBadge || (test?.resolvedSetType()?.hasLanguageSection == true)
            val hindiVal = if (showLang && att != null) ExamScoringEngine.formatScore(att.hindiScore) else ""
            val engVal = if (showLang && att != null) ExamScoringEngine.formatScore(att.englishScore) else ""
            val langTotal = when {
                !showLang -> ""
                entry.setLanguageScore != null -> ExamScoringEngine.formatScore(entry.setLanguageScore)
                att != null -> ExamScoringEngine.formatScore(att.effectiveLanguageTotal())
                else -> ""
            }
            val langStatus = when {
                !showLang -> "N/A"
                entry.setLanguagePassed == true -> "PASS (Qualifying Only)"
                entry.setLanguagePassed == false -> "FAIL (Not Qualified)"
                att != null && att.langPassed -> "PASS (Qualifying Only)"
                att != null -> "FAIL (Not Qualified)"
                else -> "NOT ATTEMPTED"
            }

            val rankCell = if (entry.rank > 0 && entry.testsAttempted > 0) entry.rank.toString() else "—"
            val row = listOf(
                rankCell,
                entry.member.displayName,
                scopeTitle,
                meritScore,
                meritMax,
                meritPct,
                accStr,
                pctStr,
                timeStr,
                hindiVal,
                engVal,
                langTotal,
                langStatus,
                entry.testsAttempted.toString()
            ).joinToString(",") { escapeCsv(it) }
            sb.appendLine(row)
        }
        return sb.toString()
    }

    suspend fun exportAndShareAsync(
        context: Context,
        scopeTitle: String,
        ranked: List<RankedAspirant>,
        attemptsByUserId: Map<String, TestAttempt> = emptyMap(),
        test: ExamTest? = null,
        shareAfterSave: Boolean = true
    ): Result<Uri> {
        val csv = buildCsvContent(scopeTitle, ranked, attemptsByUserId, test)
        val result = withContext(Dispatchers.IO) {
            try {
                val safeTitle = scopeTitle.replace(Regex("[^a-zA-Z0-9]+"), "_").take(32)
                val fileName = "TRE4_Rankings_${safeTitle}_${System.currentTimeMillis()}.csv"
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val csvFile = File(exportDir, fileName)
                csvFile.writeText(csv, Charsets.UTF_8)
                saveToDownloads(context, csvFile, fileName)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)
                Result.success(uri)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        if (shareAfterSave && result.isSuccess) {
            val uri = result.getOrThrow()
            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "TRE 4.0 Rankings CSV — $scopeTitle")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share Rankings CSV").apply {
                    if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        }
        return result
    }

    private fun escapeCsv(raw: String): String {
        val cleaned = raw.replace("\r\n", " ").replace("\n", " ")
        return if (cleaned.contains(",") || cleaned.contains("\"")) {
            "\"${cleaned.replace("\"", "\"\"")}\""
        } else {
            cleaned
        }
    }

    private fun saveToDownloads(context: Context, sourceFile: File, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/TRE4")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { inp -> inp.copyTo(out) }
                    }
                }
            } catch (_: Exception) {
                // FileProvider cache URI still works cleanly
            }
        }
    }
}
