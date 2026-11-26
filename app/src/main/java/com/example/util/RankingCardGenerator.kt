package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.GroupMember
import com.example.data.model.PaperType
import com.example.data.model.RankedAspirant
import com.example.data.model.TestAttempt
import com.example.domain.ExamScoringEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale

/**
 * Generates ultra-premium Cooling Glass 1080x1350 PNG Achievement & Group Ranking Cards
 * from live Firestore data, saves them to device Pictures/TRE4, and launches native Android
 * Sharesheet / WhatsApp sharing (user explicitly confirms sending inside WhatsApp).
 */
object RankingCardGenerator {

    suspend fun generateIndividualResultCardAsync(
        context: Context,
        member: GroupMember,
        overallRank: Int,
        individualTestRank: Int?,
        totalParticipants: Int,
        latestAttempt: TestAttempt?,
        testName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        generateIndividualResultCard(context, member, overallRank, individualTestRank, totalParticipants, latestAttempt, testName)
    }

    suspend fun generateGroupLeaderboardCardAsync(
        context: Context,
        groupName: String,
        periodLabel: String,
        ranked: List<RankedAspirant>
    ): Result<File> = withContext(Dispatchers.IO) {
        generateGroupLeaderboardCard(context, groupName, periodLabel, ranked)
    }

    fun generateIndividualResultCard(
        context: Context,
        member: GroupMember,
        overallRank: Int,
        individualTestRank: Int?,
        totalParticipants: Int,
        latestAttempt: TestAttempt?,
        testName: String
    ): Result<File> {
        var bmp: Bitmap? = null
        return try {
            val w = 1080
            val h = 1350
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            drawCoolingGlassBackdrop(canvas, w.toFloat(), h.toFloat())

            // Outer Frosted Glass Card
            val cardRect = RectF(64f, 68f, w - 64f, h - 68f)
            drawGlassPanel(canvas, cardRect, 48f)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(11, 31, 58)
                textSize = 56f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(2, 132, 199)
                textSize = 28f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105)
                textSize = 26f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText("TRE 4.0 — RESULT CARD", w / 2f, 152f, titlePaint)
            canvas.drawText("Aspirant Test & Ranking Platform • Founded by Gopal Gupta", w / 2f, 198f, subPaint)
            canvas.drawText(testName.take(44), w / 2f, 248f, mutedPaint)

            // Circular Profile Avatar
            val avatarCenterY = 365f
            val avatarRadius = 86f
            drawCircularAvatar(canvas, member.displayName, member.photoBase64, w / 2f, avatarCenterY, avatarRadius)

            // Aspirant Name & Rank Pill
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(11, 31, 58)
                textSize = 48f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(member.displayName, w / 2f, 514f, namePaint)

            val pillRect = RectF(260f, 542f, w - 260f, 624f)
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    pillRect.left, pillRect.top, pillRect.right, pillRect.bottom,
                    Color.rgb(2, 132, 199), Color.rgb(6, 182, 212), Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(pillRect, 40f, 40f, pillPaint)
            val pillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 36f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val rankDisplay = "OVERALL RANK: #${overallRank.toString().padStart(2, '0')}" +
                if (individualTestRank != null) "  |  TEST #$individualTestRank" else ""
            canvas.drawText(rankDisplay, w / 2f, 596f, pillText)

            // Primary Score Grid (2x2)
            val latestScoreStr = if (latestAttempt != null) {
                "${ExamScoringEngine.formatScore(latestAttempt.totalScore)} / ${latestAttempt.maxMarks.toInt()}"
            } else {
                "${ExamScoringEngine.formatScore(member.latestGsScore)} / 40"
            }
            val avgScoreStr = "${ExamScoringEngine.formatScore(member.gsAverageScore)} / 40"
            val hasLangSection = latestAttempt == null || PaperType.fromString(latestAttempt.paperType).hasLanguageSection()
            val c = latestAttempt?.correctCount ?: 0L
            val inc = latestAttempt?.incorrectCount ?: 0L
            val dk = latestAttempt?.dontKnowCount ?: 0L
            val na = latestAttempt?.notAttemptedCount ?: 0L
            val acc = ExamScoringEngine.computeAccuracy(c.toInt(), inc.toInt())
            val participantsStr = "$totalParticipants Aspirants"

            drawMetricTile(canvas, RectF(110f, 665f, 520f, 825f), "LATEST TEST SCORE", latestScoreStr, Color.rgb(2, 132, 199))
            drawMetricTile(canvas, RectF(560f, 665f, 970f, 825f), "OVERALL GS AVERAGE", avgScoreStr, Color.rgb(8, 145, 178))
            if (hasLangSection) {
                val langStr = ExamScoringEngine.languageBadgeText(member)
                drawMetricTile(canvas, RectF(110f, 855f, 520f, 1015f), "LANGUAGE QUALIFYING", langStr, Color.rgb(124, 58, 237))
            } else {
                val accStr = String.format(Locale.US, "%.1f%%", acc)
                drawMetricTile(canvas, RectF(110f, 855f, 520f, 1015f), "SET ACCURACY", accStr, Color.rgb(124, 58, 237))
            }
            drawMetricTile(canvas, RectF(560f, 855f, 970f, 1015f), "TOTAL PARTICIPANTS", participantsStr, Color.rgb(15, 118, 110))

            // 4-State Breakdown Row + Accuracy %
            val breakdownRect = RectF(110f, 1050f, 970f, 1165f)
            drawGlassPanel(canvas, breakdownRect, 24f)
            val bdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 34, 58)
                textSize = 24f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "Correct: $c • Wrong: $inc • Opt E: $dk • Blank: $na • Acc: ${String.format(Locale.US, "%.1f%%", acc)}",
                w / 2f,
                1118f,
                bdPaint
            )

            // Footer Date & Verified Stamp
            val dateStr = IstClock.formatDate(latestAttempt?.submittedAt?.toDate() ?: Date(), "dd MMM yyyy")
            canvas.drawText("Exam Date: $dateStr  •  Verified Centralized Cloud Result", w / 2f, 1235f, mutedPaint)

            val file = saveBitmapToFile(context, bmp, "TRE4_Result_${System.currentTimeMillis()}.png")
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            bmp?.recycle()
        }
    }

    fun generateGroupLeaderboardCard(
        context: Context,
        groupName: String,
        periodLabel: String,
        ranked: List<RankedAspirant>
    ): Result<File> {
        var bmp: Bitmap? = null
        return try {
            val w = 1080
            val h = 1350
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            drawCoolingGlassBackdrop(canvas, w.toFloat(), h.toFloat())
            drawGlassPanel(canvas, RectF(56f, 60f, w - 56f, h - 60f), 44f)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(11, 31, 58)
                textSize = 50f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(2, 132, 199)
                textSize = 27f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("TRE 4.0 — GROUP RANKING", w / 2f, 140f, titlePaint)
            canvas.drawText("$groupName  •  $periodLabel  •  Founded by Gopal Gupta", w / 2f, 186f, subPaint)

            // Header Row
            val showLang = ranked.any { it.showLanguageBadge }
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 82, 117)
                textSize = 24f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText("RANK", 104f, 252f, headerPaint)
            canvas.drawText("ASPIRANT", 215f, 252f, headerPaint)
            if (showLang) {
                canvas.drawText("SCORE", 620f, 252f, headerPaint)
                canvas.drawText("LANGUAGE", 775f, 252f, headerPaint)
            } else {
                canvas.drawText("SCORE", 760f, 252f, headerPaint)
            }

            val rowText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(11, 31, 58)
                textSize = 27f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(2, 132, 199)
                textSize = 28f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val langPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(109, 40, 217)
                textSize = 22f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }

            var y = 284f
            ranked.take(11).forEach { entry ->
                val rowRect = RectF(88f, y, w - 88f, y + 76f)
                drawGlassPanel(canvas, rowRect, 20f)
                canvas.drawText("#${entry.rank.toString().padStart(2, '0')}", 112f, y + 48f, scorePaint)
                canvas.drawText(entry.member.displayName.take(if (showLang) 20 else 28), 215f, y + 48f, rowText)
                val scoreDisplay = if (entry.testsAttempted > 0) {
                    "${ExamScoringEngine.formatScore(entry.displayScore)}/${entry.maxMarks.toInt()}"
                } else {
                    "—"
                }
                if (showLang) {
                    canvas.drawText(scoreDisplay, 620f, y + 48f, scorePaint)
                    val langText = ExamScoringEngine.setLanguageBadgeText(entry) ?: ""
                    if (langText.isNotBlank()) {
                        canvas.drawText(langText, 775f, y + 46f, langPaint)
                    }
                } else {
                    canvas.drawText(scoreDisplay, 760f, y + 48f, scorePaint)
                }
                y += 86f
            }

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105)
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            val dateStr = IstClock.formatNow("dd MMM yyyy, hh:mm a 'IST'")
            canvas.drawText("Total Aspirants: ${ranked.size}  •  Generated on $dateStr", w / 2f, 1255f, footerPaint)

            val file = saveBitmapToFile(context, bmp, "TRE4_GroupRanking_${System.currentTimeMillis()}.png")
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            bmp?.recycle()
        }
    }

    fun shareImage(context: Context, imageFile: File, preferWhatsApp: Boolean) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "TRE 4.0 Aspirant Test & Ranking Platform — Founded by Gopal Gupta")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        if (preferWhatsApp) {
            for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
                val waIntent = Intent(baseIntent).apply { setPackage(pkg) }
                if (waIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(waIntent)
                    return
                }
            }
        }
        val chooser = Intent.createChooser(baseIntent, "Share TRE 4.0 Ranking Card").apply {
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
    }

    private fun drawCoolingGlassBackdrop(canvas: Canvas, w: Float, h: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w, h,
                intArrayOf(
                    Color.rgb(219, 240, 255),
                    Color.rgb(238, 247, 255),
                    Color.rgb(232, 224, 255)
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)
    }

    private fun drawGlassPanel(canvas: Canvas, rect: RectF, radius: Float) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 147, 197, 253)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(rect, radius, radius, fill)
        canvas.drawRoundRect(rect, radius, radius, stroke)
    }

    private fun drawMetricTile(canvas: Canvas, rect: RectF, label: String, value: String, accentColor: Int) {
        drawGlassPanel(canvas, rect, 28f)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, rect.centerX(), rect.top + 54f, labelPaint)
        canvas.drawText(value, rect.centerX(), rect.top + 116f, valPaint)
    }

    private fun drawCircularAvatar(canvas: Canvas, name: String, photoBase64: String?, cx: Float, cy: Float, radius: Float) {
        val decoded = ImageCodec.decodeToBitmap(photoBase64)
        if (decoded != null) {
            val scaled = Bitmap.createScaledBitmap(decoded, (radius * 2).toInt(), (radius * 2).toInt(), true)
            try {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                }
                canvas.save()
                canvas.translate(cx - radius, cy - radius)
                canvas.drawCircle(radius, radius, radius, paint)
                canvas.restore()
            } finally {
                if (scaled !== decoded) scaled.recycle()
                decoded.recycle()
            }
        } else {
            val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    cx - radius, cy - radius, cx + radius, cy + radius,
                    Color.rgb(2, 132, 199), Color.rgb(139, 92, 246), Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy, radius, circlePaint)
            val initials = name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "A" }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 58f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(initials, cx, cy + 20f, textPaint)
        }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(cx, cy, radius, ring)
    }

    private fun saveBitmapToFile(context: Context, bmp: Bitmap, fileName: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TRE4")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { inp -> inp.copyTo(out) }
                    }
                }
            } catch (_: Exception) {
                // Cache file still valid for sharing
            }
        }
        return file
    }
}
