package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamTest
import com.example.data.model.LanguagePref
import com.example.data.model.OptionEMode
import com.example.data.model.SolutionReattemptRecord
import com.example.data.model.TestAttempt
import com.example.domain.ExamScoringEngine
import com.example.ui.glass.CoolingGlassBackground
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassEmptyState
import com.example.ui.glass.CoolingGlassIconButton
import com.example.ui.glass.CoolingGlassOptionCard
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.OptionVisualState
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.GlacierBlue600
import com.example.ui.theme.IceCyan500
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.SoftLavender500
import com.example.ui.viewmodel.QuestionTimingBenchmark

enum class SolutionAnalysisFilter(val label: String) {
    ALL("All"),
    CORRECT("Correct"),
    WRONG("Incorrect"),
    UNATTEMPTED("Unattempted"),
    OVERTIME("Overtime"),
    BOOKMARKED("Bookmarked")
}

/**
 * Full-screen Testbook-style Post-Test Solution Analysis & No-Spoiler Interactive Re-attempt Panel.
 *
 * Guarantees:
 * - Preserves testTags `in_app_solution_viewer`, `solution_filter_WRONG`, `solution_prev_btn`, `solution_next_btn`.
 * - Re-attempt Mode (ON/OFF): when ON, hides original highlight, correct key, and explanation until
 *   user taps an option (ZERO SPOILERS). After selection, displays RIGHT/WRONG feedback, reveals
 *   correct answer + explanation, shows side-by-side FIRST ATTEMPT vs SOLUTION REATTEMPT vs ACTUAL CORRECT,
 *   and persists separately via `onRecordReattempt` without EVER altering Original Attempt score or rank.
 */
@Composable
fun SolutionAnalysisScreen(
    test: ExamTest,
    attempt: TestAttempt,
    questions: List<ExamQuestion>,
    priorBench: QuestionTimingBenchmark,
    isAdmin: Boolean,
    exporting: Boolean,
    bookmarkedIndices: Set<Int> = emptySet(),
    savedReattempt: SolutionReattemptRecord? = null,
    initialLanguage: LanguagePref = LocalAppLanguage.current,
    onToggleBookmark: (Int, ExamQuestion) -> Unit = { _, _ -> },
    onRecordReattempt: (Map<String, String>) -> Unit = {},
    onDirectDownloadPdf: () -> Unit,
    onReportQuestion: (Int) -> Unit,
    onClose: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    val appLanguage = LocalAppLanguage.current
    var activeFilter by remember { mutableStateOf(SolutionAnalysisFilter.ALL) }
    var activeSectionFilter by remember { mutableStateOf("ALL") }
    var activeLang by remember(initialLanguage, appLanguage) {
        mutableStateOf(
            if (appLanguage == LanguagePref.ENGLISH && initialLanguage == LanguagePref.BILINGUAL) LanguagePref.ENGLISH
            else initialLanguage
        )
    }
    var fontScaleStep by remember { mutableIntStateOf(0) }
    var reattemptMode by remember { mutableStateOf(false) }

    // Session reattempt answers seeded from any saved practice reattempt, resettable anytime
    val reattemptAnswers = remember(test.id) {
        mutableStateMapOf<Int, String>().apply {
            savedReattempt?.reattemptAnswers?.forEach { (k, v) ->
                k.toIntOrNull()?.let { put(it, v) }
            }
        }
    }
    // Track which questions were freshly answered during THIS reattempt toggle so switching ON hides spoilers cleanly
    val revealedInSession = remember(test.id) { mutableStateMapOf<Int, Boolean>() }

    val optionEMode = remember(test) { test.resolvedOptionEMode() }
    val availableSections = remember(questions) {
        val secs = questions.map { q ->
            when (q.resolvedSection()) {
                ExamQuestion.SECTION_LANGUAGE -> "Language"
                ExamQuestion.SECTION_SUBJECT -> "Subject"
                else -> "Common"
            }
        }.distinct()
        if (secs.size > 1) listOf("ALL") + secs else emptyList()
    }

    fun isQuestionCorrect(idx: Int): Boolean {
        val q = questions[idx]
        val ans = attempt.answers[idx.toString()]?.uppercase()?.trim()
        val key = q.correctOption.uppercase().trim()
        if (ans.isNullOrBlank() || key.isBlank()) return false
        if (ans == "E" && optionEMode == OptionEMode.NOT_ATTEMPTED) return false
        return ans == key
    }

    fun isQuestionWrong(idx: Int): Boolean {
        val q = questions[idx]
        val ans = attempt.answers[idx.toString()]?.uppercase()?.trim()
        val key = q.correctOption.uppercase().trim()
        if (ans.isNullOrBlank() || key.isBlank()) return false
        if (ans == "E" && optionEMode == OptionEMode.NOT_ATTEMPTED) return false
        return ans != key
    }

    fun isQuestionUnattempted(idx: Int): Boolean {
        val ans = attempt.answers[idx.toString()]?.uppercase()?.trim()
        return ans.isNullOrBlank() || (ans == "E" && optionEMode == OptionEMode.NOT_ATTEMPTED)
    }

    fun isQuestionOvertime(idx: Int): Boolean {
        val mySec = attempt.questionTimesSec[idx.toString()] ?: return false
        val avgSec = priorBench.avgSecondsByQuestion[idx]
            ?: if (priorBench.priorAvgTotalSeconds > 0L && questions.isNotEmpty()) {
                (priorBench.priorAvgTotalSeconds / questions.size).coerceAtLeast(15L)
            } else 45L
        return mySec > avgSec && mySec > 0L
    }

    fun matchesSection(idx: Int): Boolean {
        if (activeSectionFilter == "ALL") return true
        val sec = when (questions[idx].resolvedSection()) {
            ExamQuestion.SECTION_LANGUAGE -> "Language"
            ExamQuestion.SECTION_SUBJECT -> "Subject"
            else -> "Common"
        }
        return sec == activeSectionFilter
    }

    val filterCounts = remember(questions, attempt.answers, bookmarkedIndices, activeSectionFilter, priorBench) {
        val secIndices = questions.indices.filter { matchesSection(it) }
        mapOf(
            SolutionAnalysisFilter.ALL to secIndices.size,
            SolutionAnalysisFilter.CORRECT to secIndices.count { isQuestionCorrect(it) },
            SolutionAnalysisFilter.WRONG to secIndices.count { isQuestionWrong(it) },
            SolutionAnalysisFilter.UNATTEMPTED to secIndices.count { isQuestionUnattempted(it) },
            SolutionAnalysisFilter.OVERTIME to secIndices.count { isQuestionOvertime(it) },
            SolutionAnalysisFilter.BOOKMARKED to secIndices.count { it in bookmarkedIndices }
        )
    }

    val filteredIndices = remember(questions, attempt.answers, activeFilter, activeSectionFilter, bookmarkedIndices, priorBench) {
        questions.indices.filter { idx ->
            matchesSection(idx) && when (activeFilter) {
                SolutionAnalysisFilter.ALL -> true
                SolutionAnalysisFilter.CORRECT -> isQuestionCorrect(idx)
                SolutionAnalysisFilter.WRONG -> isQuestionWrong(idx)
                SolutionAnalysisFilter.UNATTEMPTED -> isQuestionUnattempted(idx)
                SolutionAnalysisFilter.OVERTIME -> isQuestionOvertime(idx)
                SolutionAnalysisFilter.BOOKMARKED -> idx in bookmarkedIndices
            }
        }
    }

    var selectedPosition by remember(activeFilter, activeSectionFilter) { mutableIntStateOf(0) }
    val safePos = selectedPosition.coerceIn(0, (filteredIndices.size - 1).coerceAtLeast(0))
    val currentQuestionIndex = filteredIndices.getOrNull(safePos)

    BackHandler { onClose() }

    CoolingGlassBackground(modifier = Modifier.testTag("in_app_solution_viewer")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Top Bar: Back, Title, Live Language Switcher, Font A-/A+, Download PDF
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(glass.glassSurfaceStrong)
                    .border(0.8.dp, glass.glassBorderSubtle)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoolingGlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Close Solution Viewer",
                    testTagName = "close_solution_viewer_btn",
                    onClick = onClose
                )
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Solution Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${test.title} • ${ExamScoringEngine.formatScore(attempt.totalScore)}/${attempt.maxMarks.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.accentPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Live Language Cycle Button (BI -> HI -> EN)
                Box(
                    modifier = Modifier
                        .testTag("solution_lang_toggle")
                        .clip(RoundedCornerShape(10.dp))
                        .background(glass.accentPrimary.copy(alpha = 0.14f))
                        .border(1.dp, glass.accentCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable {
                            activeLang = when (activeLang) {
                                LanguagePref.BILINGUAL -> LanguagePref.HINDI
                                LanguagePref.HINDI -> LanguagePref.ENGLISH
                                LanguagePref.ENGLISH -> LanguagePref.BILINGUAL
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (activeLang) {
                            LanguagePref.BILINGUAL -> "BI"
                            LanguagePref.HINDI -> "HI"
                            LanguagePref.ENGLISH -> "EN"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(4.dp))
                // Font A- / A+
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(glass.glassSurface)
                            .border(0.8.dp, glass.glassBorderSubtle, RoundedCornerShape(8.dp))
                            .clickable { fontScaleStep = (fontScaleStep - 1).coerceIn(-2, 3) }
                            .padding(horizontal = 7.dp, vertical = 6.dp)
                    ) {
                        Text("A-", style = MaterialTheme.typography.labelSmall, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(glass.glassSurface)
                            .border(0.8.dp, glass.glassBorderSubtle, RoundedCornerShape(8.dp))
                            .clickable { fontScaleStep = (fontScaleStep + 1).coerceIn(-2, 3) }
                            .padding(horizontal = 7.dp, vertical = 6.dp)
                    ) {
                        Text("A+", style = MaterialTheme.typography.labelSmall, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(4.dp))
                CoolingGlassIconButton(
                    icon = Icons.Rounded.Download,
                    contentDescription = "Direct Download Solution PDF",
                    tint = glass.accentCyan,
                    testTagName = "solution_viewer_direct_pdf_btn",
                    onClick = { if (!exporting) onDirectDownloadPdf() }
                )
            }

            // Re-attempt Mode Toggle Bar (Prominent ON/OFF pill + Immutability Notice)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (reattemptMode) SoftLavender500.copy(alpha = 0.14f) else glass.glassSurface)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (reattemptMode) Icons.Rounded.Replay else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = if (reattemptMode) SoftLavender500 else glass.accentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (reattemptMode) "Re-attempt Mode ON (No Spoilers)" else "Re-attempt Mode OFF (Full Solutions)",
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (reattemptMode) "Practice mode — original score & rank remain locked"
                            else "Toggle ON to solve questions fresh without seeing answers first",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .testTag("reattempt_mode_toggle")
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (reattemptMode) Brush.horizontalGradient(listOf(SoftLavender500, GlacierBlue600))
                            else Brush.horizontalGradient(listOf(glass.glassSurfaceStrong, glass.glassSurfaceStrong))
                        )
                        .border(
                            1.2.dp,
                            if (reattemptMode) SoftLavender500 else glass.glassBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            val next = !reattemptMode
                            reattemptMode = next
                            if (next) {
                                // Hide spoilers cleanly for fresh practice run
                                revealedInSession.clear()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (reattemptMode) "ON" else "OFF",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (reattemptMode) CrystalWhite else glass.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Section Filter Row (only for multi-section sets)
            if (availableSections.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(availableSections) { sec ->
                        val selected = sec == activeSectionFilter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) SoftLavender500 else glass.glassSurfaceStrong)
                                .border(1.dp, if (selected) SoftLavender500 else glass.glassBorderSubtle, RoundedCornerShape(10.dp))
                                .clickable {
                                    activeSectionFilter = sec
                                    selectedPosition = 0
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (sec == "ALL") "All Sections" else sec,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) CrystalWhite else glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Filter Chips with Live Counts (All, Correct, Incorrect, Unattempted, Overtime, Bookmarked)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(SolutionAnalysisFilter.entries) { filter ->
                    val selected = filter == activeFilter
                    val cnt = filterCounts[filter] ?: 0
                    Box(
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .testTag("solution_filter_${filter.name}")
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) glass.accentPrimary else glass.glassSurfaceStrong)
                            .border(
                                1.dp,
                                if (selected) glass.accentCyan else glass.glassBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                activeFilter = filter
                                selectedPosition = 0
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${filter.label} ($cnt)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) CrystalWhite else glass.textSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            // Numbered Question Grid Strip (exact original question numbering)
            if (filteredIndices.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(filteredIndices) { pos, qIdx ->
                        val isCurrent = pos == safePos
                        val isBookmarked = qIdx in bookmarkedIndices
                        val chipColor = when {
                            reattemptMode && revealedInSession[qIdx] != true -> glass.accentPrimary
                            isQuestionCorrect(qIdx) -> glass.correct
                            isQuestionWrong(qIdx) -> glass.incorrect
                            else -> Color(0xFF94A3B8)
                        }
                        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) chipColor else chipColor.copy(alpha = 0.16f))
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = if (isCurrent) CrystalWhite else chipColor,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedPosition = pos },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${qIdx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isCurrent) CrystalWhite else glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isBookmarked) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(glass.review)
                                        .border(1.dp, CrystalWhite, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // Main Question + Options + Explanation Area
            if (currentQuestionIndex == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CoolingGlassEmptyState(
                        title = "No Questions in This Filter",
                        subtitle = "Switch to 'All' or clear section filter to inspect all questions and explanations.",
                        icon = Icons.Rounded.Visibility
                    )
                }
            } else {
                val q = questions[currentQuestionIndex]
                val originalAns = attempt.answers[currentQuestionIndex.toString()]?.uppercase()?.trim()
                val normalizedCorrect = q.correctOption.trim().uppercase()
                val isBookmarked = currentQuestionIndex in bookmarkedIndices
                val qTimeSec = attempt.questionTimesSec[currentQuestionIndex.toString()]
                val qAvgSec = priorBench.avgSecondsByQuestion[currentQuestionIndex]
                val isOvertime = isQuestionOvertime(currentQuestionIndex)

                val reattemptPick = reattemptAnswers[currentQuestionIndex]
                val reattemptRevealed = !reattemptMode || revealedInSession[currentQuestionIndex] == true
                val hideSpoilers = reattemptMode && !reattemptRevealed

                val detailScroll = rememberScrollState()
                LaunchedEffect(currentQuestionIndex) { detailScroll.scrollTo(0) }

                val scaledQuestionSp = (16 + fontScaleStep * 2).coerceIn(13, 24).sp
                val scaledBodySp = (14 + fontScaleStep * 2).coerceIn(12, 22).sp

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("solution_detail_scroll")
                        .verticalScroll(detailScroll)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoolingGlassCard(strongFrost = true) {
                        // Question Header: Number, Subject, Bookmark, Report, Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                CoolingGlassBadge("Q${currentQuestionIndex + 1} • ${q.subject.ifBlank { "General" }}", glass.accentPrimary)
                                if (q.resolvedSection() == ExamQuestion.SECTION_LANGUAGE) {
                                    CoolingGlassBadge("Qualifying", SoftLavender500)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                CoolingGlassIconButton(
                                    icon = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                    contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark Question",
                                    tint = if (isBookmarked) glass.review else glass.textSecondary,
                                    testTagName = "solution_bookmark_btn",
                                    onClick = { onToggleBookmark(currentQuestionIndex, q) }
                                )
                                CoolingGlassIconButton(
                                    icon = Icons.Rounded.Flag,
                                    contentDescription = "Report Question Issue",
                                    tint = glass.review,
                                    testTagName = "solution_report_btn",
                                    onClick = { onReportQuestion(currentQuestionIndex) }
                                )
                            }
                        }

                        // Time Analytics Strip (Your Time vs Avg Time + Overtime Warning Badge)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(glass.glassSurface)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Timer, contentDescription = null, tint = glass.accentCyan, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                val timeStr = buildString {
                                    append("Your Time: ${if (qTimeSec != null) "${qTimeSec}s" else "—"}")
                                    if (qAvgSec != null) append("  |  Avg Time: ${qAvgSec}s")
                                }
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (isOvertime) {
                                CoolingGlassBadge("Overtime", glass.incorrect)
                            }
                        }

                        if (q.passage.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (activeLang == LanguagePref.ENGLISH || appLanguage == LanguagePref.ENGLISH) {
                                    "Passage: ${q.passage}"
                                } else {
                                    "Passage / गद्यांश: ${q.passage}"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = scaledBodySp),
                                color = glass.textSecondary
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = q.displayText(activeLang),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = scaledQuestionSp, lineHeight = (scaledQuestionSp.value + 6f).sp),
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(10.dp))

                        if (hideSpoilers) {
                            Text(
                                text = "Tap an option below to re-attempt this question (spoilers hidden until you answer):",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftLavender500,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                        }

                        // Interactive / Revealed Options List
                        q.optionsList(activeLang).forEach { (k, v) ->
                            val isCorrectKey = !hideSpoilers && k == normalizedCorrect && normalizedCorrect in listOf("A", "B", "C", "D", "E")
                            val isOriginalPick = !hideSpoilers && originalAns == k
                            val isReattemptPick = reattemptMode && reattemptRevealed && reattemptPick == k

                            val st = when {
                                hideSpoilers -> OptionVisualState.DEFAULT
                                isCorrectKey -> OptionVisualState.CORRECT
                                isReattemptPick && k != normalizedCorrect -> OptionVisualState.INCORRECT
                                isOriginalPick && k == "E" && optionEMode == OptionEMode.NOT_ATTEMPTED -> OptionVisualState.DONT_KNOW_SELECTED
                                isOriginalPick && !reattemptMode -> OptionVisualState.INCORRECT
                                else -> OptionVisualState.DEFAULT
                            }

                            val primaryBadge = when {
                                hideSpoilers -> null
                                isCorrectKey -> "Correct Answer"
                                isReattemptPick -> "Re-attempt Pick"
                                isOriginalPick && k == "E" -> "Original • 0 Marks"
                                isOriginalPick -> "First Attempt"
                                else -> null
                            }
                            val primaryBadgeColor = when {
                                isCorrectKey -> glass.correct
                                isReattemptPick -> if (k == normalizedCorrect) glass.correct else glass.incorrect
                                isOriginalPick && k == "E" -> glass.dontKnow
                                isOriginalPick -> glass.incorrect
                                else -> null
                            }
                            val secondaryBadge = when {
                                hideSpoilers -> null
                                isCorrectKey && isReattemptPick -> "Re-attempt ✓"
                                isCorrectKey && isOriginalPick -> "First Attempt ✓"
                                isReattemptPick && isOriginalPick -> "First Attempt"
                                else -> null
                            }

                            CoolingGlassOptionCard(
                                optionKey = k,
                                optionText = v,
                                state = st,
                                enabled = hideSpoilers,
                                badgeText = primaryBadge,
                                badgeColor = primaryBadgeColor,
                                secondaryBadgeText = secondaryBadge,
                                secondaryBadgeColor = glass.accentCyan,
                                onClick = {
                                    if (hideSpoilers) {
                                        reattemptAnswers[currentQuestionIndex] = k
                                        revealedInSession[currentQuestionIndex] = true
                                        val serialized = reattemptAnswers.entries.associate { it.key.toString() to it.value }
                                        onRecordReattempt(serialized)
                                    }
                                },
                                modifier = Modifier
                                    .padding(vertical = 3.dp)
                                    .testTag("solution_opt_$k")
                            )
                        }
                    }

                    // Instant Re-attempt Verdict + Side-by-Side Comparison Card (only after answering in Re-attempt mode)
                    if (reattemptMode && reattemptRevealed && reattemptPick != null) {
                        val isReattemptRight = reattemptPick.equals(normalizedCorrect, ignoreCase = true)
                        CoolingGlassCard(
                            strongFrost = true,
                            tintColor = if (isReattemptRight) glass.correct else glass.incorrect,
                            borderColor = if (isReattemptRight) glass.correct else glass.incorrect,
                            contentPadding = PaddingValues(14.dp),
                            modifier = Modifier.testTag("reattempt_verdict_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isReattemptRight) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                        contentDescription = null,
                                        tint = if (isReattemptRight) glass.correct else glass.incorrect,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isReattemptRight) "RIGHT ANSWER!" else "WRONG ANSWER",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isReattemptRight) glass.correct else glass.incorrect,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                CoolingGlassOutlinedButton(
                                    text = "Try Again",
                                    onClick = {
                                        revealedInSession[currentQuestionIndex] = false
                                    }
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            // Side-by-Side Comparison: FIRST ATTEMPT vs SOLUTION REATTEMPT vs ACTUAL CORRECT
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ComparisonPill(
                                    title = "FIRST ATTEMPT",
                                    value = originalAns ?: "Blank",
                                    color = when {
                                        originalAns == normalizedCorrect -> glass.correct
                                        originalAns == null || originalAns == "E" -> glass.textMuted
                                        else -> glass.incorrect
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ComparisonPill(
                                    title = "RE-ATTEMPT",
                                    value = reattemptPick,
                                    color = if (isReattemptRight) glass.correct else glass.incorrect,
                                    modifier = Modifier.weight(1f)
                                )
                                ComparisonPill(
                                    title = "CORRECT KEY",
                                    value = normalizedCorrect.ifBlank { "—" },
                                    color = glass.correct,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Practice saved separately — your Original Attempt score & leaderboard rank are NOT changed.",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textMuted
                            )
                        }
                    }

                    // Detailed Explanation Box (Hidden until revealed when Re-attempt Mode is ON!)
                    if (!hideSpoilers) {
                        CoolingGlassCard(
                            tintColor = glass.correct,
                            borderColor = glass.correct.copy(alpha = 0.55f),
                            contentPadding = PaddingValues(14.dp),
                            modifier = Modifier.testTag("solution_explanation_card")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Verified Solution",
                                    tint = glass.correct,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (activeLang == LanguagePref.ENGLISH || appLanguage == LanguagePref.ENGLISH) {
                                        "Solution (Correct: Option ${normalizedCorrect.ifBlank { "—" }})"
                                    } else {
                                        "Solution / समाधान (Correct: Option ${normalizedCorrect.ifBlank { "—" }})"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            val expText = q.displayExplanation(activeLang).ifBlank {
                                "Verified Correct Answer: Option $normalizedCorrect. Review the highlighted option above."
                            }
                            Text(
                                text = expText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = scaledBodySp, lineHeight = (scaledBodySp.value + 6f).sp),
                                color = glass.textPrimary
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }

            // Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(glass.glassSurfaceStrong)
                    .border(0.8.dp, glass.glassBorderSubtle)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoolingGlassOutlinedButton(
                    text = "Previous",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    enabled = safePos > 0,
                    testTagName = "solution_prev_btn",
                    modifier = Modifier.weight(1f),
                    onClick = { if (safePos > 0) selectedPosition = safePos - 1 }
                )
                Text(
                    text = if (filteredIndices.isEmpty()) "0 / 0" else "${safePos + 1} / ${filteredIndices.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                CoolingGlassButton(
                    text = "Next",
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    enabled = safePos < filteredIndices.lastIndex,
                    testTagName = "solution_next_btn",
                    modifier = Modifier.weight(1f),
                    onClick = { if (safePos < filteredIndices.lastIndex) selectedPosition = safePos + 1 }
                )
            }
        }
    }
}

@Composable
private fun ComparisonPill(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val glass = LocalCoolingGlass.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f))
            .border(0.8.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = glass.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
