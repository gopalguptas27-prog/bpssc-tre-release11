package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DiscussionComment
import com.example.data.model.ExamTest
import com.example.data.model.GroupMember
import com.example.data.model.LanguageFailRankPolicy
import com.example.data.model.OptionEMode
import com.example.data.model.SetType
import com.example.data.model.SolutionReattemptRecord
import com.example.data.model.TestAttempt
import com.example.data.model.TieBreakPolicy
import com.example.domain.ExamScoringEngine
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassDialog
import com.example.ui.glass.CoolingGlassEmptyState
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.CoolingGlassTabRow
import com.example.ui.glass.CoolingGlassTextField
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.SoftLavender500
import com.example.util.RankingCsvExporter
import kotlinx.coroutines.launch

/**
 * One-Click Full-Length Set Assembler (Part 5: 150Q Maths & Science, Part 6: 150Q Full Maths)
 * assembled from existing Part 1–4 sets while enforcing strict 30+40+40+40 = 150 blueprint
 * and strict Language Exclusion (Merit /120).
 */
@Composable
fun AdminSetAssemblerSection(
    tests: List<ExamTest>,
    busy: Boolean,
    onAssembleFullLengthSet: (
        title: String,
        targetSetType: SetType,
        part1Common: ExamTest,
        part2Language: ExamTest,
        part3Maths: ExamTest,
        fourthPart: ExamTest,
        durationMinutes: Int,
        publishImmediately: Boolean,
        cutoffMarks: Double?,
        pauseAllowed: Boolean
    ) -> Unit
) {
    val glass = LocalCoolingGlass.current
    var targetPartIndex by remember { mutableIntStateOf(0) } // 0 = Part 5 (M+S), 1 = Part 6 (Full Math)
    val targetSetType = if (targetPartIndex == 0) SetType.PART_5_FULL_MATH_SCI else SetType.PART_6_FULL_MATH

    val part1Options = remember(tests) { tests.filter { it.resolvedSetType() == SetType.PART_1_COMMON } }
    val part2Options = remember(tests) { tests.filter { it.resolvedSetType() == SetType.PART_2_LANGUAGE } }
    val part3Options = remember(tests) { tests.filter { it.resolvedSetType() == SetType.PART_3_MATHS } }
    val part4Options = remember(tests) { tests.filter { it.resolvedSetType() == SetType.PART_4_SCIENCE } }

    var pickedPart1Id by remember(part1Options) { mutableStateOf(part1Options.firstOrNull()?.id.orEmpty()) }
    var pickedPart2Id by remember(part2Options) { mutableStateOf(part2Options.firstOrNull()?.id.orEmpty()) }
    var pickedPart3Id by remember(part3Options) { mutableStateOf(part3Options.firstOrNull()?.id.orEmpty()) }
    var pickedFourthId by remember(targetPartIndex, part3Options, part4Options) {
        mutableStateOf(
            if (targetPartIndex == 0) part4Options.firstOrNull()?.id.orEmpty()
            else part3Options.getOrNull(1)?.id ?: part3Options.firstOrNull()?.id.orEmpty()
        )
    }

    var title by remember(targetPartIndex) {
        mutableStateOf(
            if (targetPartIndex == 0) "TRE 4.0 Part 5 Full Mock (Maths & Science 150Q)"
            else "TRE 4.0 Part 6 Full Mock (Mathematics 150Q)"
        )
    }
    var durationStr by remember(targetPartIndex) { mutableStateOf(if (targetPartIndex == 0) "120" else "135") }
    var cutoffStr by remember { mutableStateOf("72") }
    var pauseAllowed by remember { mutableStateOf(true) }
    var publishNow by remember { mutableStateOf(false) }

    val p1 = part1Options.firstOrNull { it.id == pickedPart1Id }
    val p2 = part2Options.firstOrNull { it.id == pickedPart2Id }
    val p3 = part3Options.firstOrNull { it.id == pickedPart3Id }
    val p4Pool = if (targetPartIndex == 0) part4Options else part3Options
    val p4 = p4Pool.firstOrNull { it.id == pickedFourthId }
    val canAssemble = !busy && title.trim().length >= 4 && p1 != null && p2 != null && p3 != null && p4 != null

    CoolingGlassCard(
        strongFrost = true,
        borderColor = SoftLavender500.copy(alpha = 0.55f),
        modifier = Modifier.testTag("admin_set_assembler_card")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesomeMotion, contentDescription = null, tint = SoftLavender500)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "One-Click 150Q Full-Length Set Assembler",
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Combines existing Part 1–4 sets • Language (30Q) Qualifying Only • Merit /120",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textMuted
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        CoolingGlassTabRow(
            tabs = listOf("Part 5 (150Q M+S)", "Part 6 (150Q Maths)"),
            selectedIndex = targetPartIndex,
            onSelect = { targetPartIndex = it }
        )
        Spacer(Modifier.height(10.dp))
        CoolingGlassTextField(value = title, onValueChange = { title = it }, label = "Full-Length Set Title")
        Spacer(Modifier.height(8.dp))

        AssemblerSetPickerRow("Part 1 – Common (40 Q)", part1Options, pickedPart1Id) { pickedPart1Id = it }
        AssemblerSetPickerRow("Part 2 – Language (30 Q Qualifying)", part2Options, pickedPart2Id) { pickedPart2Id = it }
        AssemblerSetPickerRow("Part 3 – Mathematics (40 Q)", part3Options, pickedPart3Id) { pickedPart3Id = it }
        AssemblerSetPickerRow(
            label = if (targetPartIndex == 0) "Part 4 – Science (40 Q)" else "Extra Mathematics Set (40 Q)",
            options = p4Pool,
            selectedId = pickedFourthId
        ) { pickedFourthId = it }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoolingGlassTextField(
                value = durationStr,
                onValueChange = { durationStr = it.filter(Char::isDigit) },
                label = "Duration (min)",
                modifier = Modifier.weight(1f)
            )
            CoolingGlassTextField(
                value = cutoffStr,
                onValueChange = { cutoffStr = it.filter { c -> c.isDigit() || c == '.' } },
                label = "Cutoff (/120)",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Allow Pause during exam", style = MaterialTheme.typography.bodySmall, color = glass.textPrimary)
            Switch(checked = pauseAllowed, onCheckedChange = { pauseAllowed = it })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Publish immediately to all users", style = MaterialTheme.typography.bodySmall, color = glass.textPrimary)
            Switch(checked = publishNow, onCheckedChange = { publishNow = it })
        }
        Spacer(Modifier.height(10.dp))
        CoolingGlassButton(
            text = "Assemble ${targetSetType.shortLabel} (150 Q • Merit /120)",
            icon = Icons.Rounded.AutoAwesomeMotion,
            enabled = canAssemble,
            loading = busy,
            testTagName = "assemble_full_150_btn",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (p1 != null && p2 != null && p3 != null && p4 != null) {
                    onAssembleFullLengthSet(
                        title.trim(),
                        targetSetType,
                        p1,
                        p2,
                        p3,
                        p4,
                        durationStr.toIntOrNull()?.coerceIn(30, 240) ?: 120,
                        publishNow,
                        cutoffStr.toDoubleOrNull(),
                        pauseAllowed
                    )
                }
            }
        )
    }
}

@Composable
private fun AssemblerSetPickerRow(
    label: String,
    options: List<ExamTest>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    val glass = LocalCoolingGlass.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = glass.textSecondary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        if (options.isEmpty()) {
            Text(
                text = "No matching sets found yet. Create a set of this part in Question Bank first.",
                style = MaterialTheme.typography.labelSmall,
                color = glass.incorrect
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.take(4).forEach { opt ->
                    val picked = opt.id == selectedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (picked) glass.accentPrimary.copy(alpha = 0.16f) else glass.glassSurface)
                            .border(
                                1.dp,
                                if (picked) glass.accentCyan else glass.glassBorderSubtle,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelect(opt.id) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${opt.title} (${opt.questionCount} Q)",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textPrimary,
                            fontWeight = if (picked) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (picked) {
                            CoolingGlassBadge("Selected", glass.correct)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Per-Set Exam Rules Configuration Dialog (Admin Only).
 */
@Composable
fun AdminTestExamRulesDialog(
    test: ExamTest,
    onDismiss: () -> Unit,
    onSave: (
        positiveMarks: Double,
        negativeNum: Int,
        negativeDen: Int,
        unattemptedPenalty: Boolean,
        optionEMode: OptionEMode,
        cutoffMarks: Double?,
        pauseAllowed: Boolean,
        langPassMarks: Double,
        langMinHindi: Double,
        langMinEnglish: Double,
        langFailRankPolicy: LanguageFailRankPolicy,
        tieBreakPolicy: TieBreakPolicy
    ) -> Unit
) {
    val glass = LocalCoolingGlass.current
    var positiveStr by remember(test.id) { mutableStateOf(test.positiveMarks.toString()) }
    var negPresetIdx by remember(test.id) {
        mutableIntStateOf(
            when {
                test.negativeMarkNumerator <= 0L -> 0
                test.negativeMarkDenominator == 4L -> 2
                else -> 1
            }
        )
    }
    var unattemptedPenalty by remember(test.id) { mutableStateOf(test.unattemptedPenalty) }
    var optionEMode by remember(test.id) { mutableStateOf(test.resolvedOptionEMode()) }
    var cutoffStr by remember(test.id) { mutableStateOf(test.cutoffMarks?.toString().orEmpty()) }
    var pauseAllowed by remember(test.id) { mutableStateOf(test.pauseAllowed) }
    var langPassStr by remember(test.id) { mutableStateOf(test.langPassMarks.toString()) }
    var langMinHindiStr by remember(test.id) { mutableStateOf(test.langMinHindi.toString()) }
    var langMinEngStr by remember(test.id) { mutableStateOf(test.langMinEnglish.toString()) }
    var langFailPolicy by remember(test.id) { mutableStateOf(test.resolvedLangFailRankPolicy()) }
    var tieBreakPolicy by remember(test.id) { mutableStateOf(test.resolvedTieBreakPolicy()) }

    CoolingGlassDialog(title = "Exam Rules — ${test.title}", onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoolingGlassTextField(
                    value = positiveStr,
                    onValueChange = { positiveStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "Positive (+)",
                    modifier = Modifier.weight(1f)
                )
                CoolingGlassTextField(
                    value = cutoffStr,
                    onValueChange = { cutoffStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "Cutoff Marks",
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Negative Marking", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary, fontWeight = FontWeight.Bold)
            CoolingGlassTabRow(
                tabs = listOf("None (0)", "-1/3", "-1/4"),
                selectedIndex = negPresetIdx,
                onSelect = { negPresetIdx = it }
            )

            Text("Option E Behavior", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary, fontWeight = FontWeight.Bold)
            CoolingGlassTabRow(
                tabs = listOf("Not Attempted (0)", "Standard 5th Opt"),
                selectedIndex = if (optionEMode == OptionEMode.NOT_ATTEMPTED) 0 else 1,
                onSelect = { optionEMode = if (it == 0) OptionEMode.NOT_ATTEMPTED else OptionEMode.STANDARD_5TH_OPTION }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Blank Answer Negative Penalty", style = MaterialTheme.typography.bodySmall, color = glass.textPrimary)
                Switch(checked = unattemptedPenalty, onCheckedChange = { unattemptedPenalty = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Allow Pause & Resume", style = MaterialTheme.typography.bodySmall, color = glass.textPrimary)
                Switch(checked = pauseAllowed, onCheckedChange = { pauseAllowed = it })
            }

            if (test.resolvedSetType().hasLanguageSection) {
                Text("Language Qualifying Criteria (Never Added to Merit)", style = MaterialTheme.typography.labelMedium, color = SoftLavender500, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CoolingGlassTextField(
                        value = langPassStr,
                        onValueChange = { langPassStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = "Pass (/30)",
                        modifier = Modifier.weight(1f)
                    )
                    CoolingGlassTextField(
                        value = langMinHindiStr,
                        onValueChange = { langMinHindiStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = "Min Hindi",
                        modifier = Modifier.weight(1f)
                    )
                    CoolingGlassTextField(
                        value = langMinEngStr,
                        onValueChange = { langMinEngStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = "Min Eng",
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("If Language Fails:", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary, fontWeight = FontWeight.Bold)
                CoolingGlassTabRow(
                    tabs = listOf("Show Badge", "Exclude Rank"),
                    selectedIndex = if (langFailPolicy == LanguageFailRankPolicy.SHOW_WITH_NOT_QUALIFIED_BADGE) 0 else 1,
                    onSelect = {
                        langFailPolicy = if (it == 0) LanguageFailRankPolicy.SHOW_WITH_NOT_QUALIFIED_BADGE
                        else LanguageFailRankPolicy.EXCLUDE_FROM_MERIT_RANK
                    }
                )
            }

            Text("Ranking Tie-Break Policy", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary, fontWeight = FontWeight.Bold)
            CoolingGlassTabRow(
                tabs = listOf("Shared Rank", "Acc → Time"),
                selectedIndex = if (tieBreakPolicy == TieBreakPolicy.SHARED_EQUAL_RANK) 0 else 1,
                onSelect = {
                    tieBreakPolicy = if (it == 0) TieBreakPolicy.SHARED_EQUAL_RANK else TieBreakPolicy.ACCURACY_AND_TIME
                }
            )

            Spacer(Modifier.height(6.dp))
            CoolingGlassButton(
                text = "Save Exam Rules",
                icon = Icons.Rounded.Tune,
                testTagName = "save_exam_rules_btn",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val (negN, negD) = when (negPresetIdx) {
                        0 -> 0 to 1
                        2 -> 1 to 4
                        else -> 1 to 3
                    }
                    onSave(
                        positiveStr.toDoubleOrNull()?.coerceIn(0.25, 10.0) ?: 1.0,
                        negN,
                        negD,
                        unattemptedPenalty,
                        optionEMode,
                        cutoffStr.toDoubleOrNull(),
                        pauseAllowed,
                        langPassStr.toDoubleOrNull()?.coerceIn(0.0, 30.0) ?: 9.0,
                        langMinHindiStr.toDoubleOrNull()?.coerceIn(0.0, 22.0) ?: 0.0,
                        langMinEngStr.toDoubleOrNull()?.coerceIn(0.0, 8.0) ?: 0.0,
                        langFailPolicy,
                        tieBreakPolicy
                    )
                }
            )
        }
    }
}

/**
 * Reported Question & Answer Key Audit Queue for Admin.
 */
@Composable
fun AdminReportedIssuesSection(
    discussions: List<DiscussionComment>,
    onResolveIssue: (commentId: String, resolved: Boolean, note: String) -> Unit
) {
    val glass = LocalCoolingGlass.current
    val reportedTickets = remember(discussions) {
        discussions.filter { it.commentType == "REPORT_QUESTION" || it.commentType == "REPORT_ANSWER" }
            .sortedBy { it.resolved }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_reported_issues_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (reportedTickets.isEmpty()) {
            CoolingGlassEmptyState(
                title = "No Reported Question Issues",
                subtitle = "When users report a wrong question or answer key from the Live Exam or Solution Analysis screen, tickets appear here for Admin resolution.",
                icon = Icons.Rounded.CheckCircle
            )
        } else {
            reportedTickets.forEach { ticket ->
                CoolingGlassCard(
                    strongFrost = !ticket.resolved,
                    borderColor = if (ticket.resolved) glass.correct.copy(alpha = 0.5f) else glass.review.copy(alpha = 0.7f),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoolingGlassBadge(
                            text = if (ticket.commentType == "REPORT_ANSWER") "Answer Key Report" else "Question Report",
                            color = if (ticket.commentType == "REPORT_ANSWER") glass.incorrect else glass.review
                        )
                        CoolingGlassBadge(
                            text = if (ticket.resolved) "RESOLVED" else "OPEN",
                            color = if (ticket.resolved) glass.correct else glass.review
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Target: ${ticket.targetId} • Reported by ${ticket.authorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = ticket.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textPrimary
                    )
                    Spacer(Modifier.height(10.dp))
                    CoolingGlassOutlinedButton(
                        text = if (ticket.resolved) "Reopen Ticket" else "Mark Resolved",
                        icon = if (ticket.resolved) Icons.Rounded.Flag else Icons.Rounded.CheckCircle,
                        tint = if (ticket.resolved) glass.review else glass.correct,
                        testTagName = "resolve_issue_${ticket.id}",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onResolveIssue(ticket.id, !ticket.resolved, if (!ticket.resolved) "Verified by Admin" else "") }
                    )
                }
            }
        }
    }
}

/**
 * Admin Analytics Viewer (Original Attempt vs Solution Reattempt) + One-Tap CSV Export.
 */
@Composable
fun AdminAnalyticsAndCsvSection(
    tests: List<ExamTest>,
    members: List<GroupMember>,
    attempts: List<TestAttempt>,
    solutionReattempts: List<SolutionReattemptRecord>,
    onBannerMessage: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalCoolingGlass.current
    val publishedTests = remember(tests) { tests.filter { it.status == "PUBLISHED" } }
    var selectedTestId by remember(publishedTests) { mutableStateOf(publishedTests.firstOrNull()?.id.orEmpty()) }
    var exportingCsv by remember { mutableStateOf(false) }

    val selectedTest = publishedTests.firstOrNull { it.id == selectedTestId }
    val testAttempts = remember(attempts, selectedTestId) {
        attempts.filter { it.testId == selectedTestId }
    }
    val testReattempts = remember(solutionReattempts, selectedTestId) {
        solutionReattempts.filter { it.testId == selectedTestId }
    }
    val rankedForTest = remember(selectedTest, members, attempts, tests) {
        if (selectedTest == null) emptyList()
        else ExamScoringEngine.computeRankings(
            period = com.example.data.model.RankingPeriod.INDIVIDUAL_TEST,
            members = members,
            attempts = attempts,
            tests = tests,
            selectedTestId = selectedTest.id
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_analytics_csv_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Insights, contentDescription = null, tint = glass.accentCyan)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Set Analytics & CSV Export", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                    Text("Language Qualifying columns exported separately from Merit & Rank", style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (publishedTests.isEmpty()) {
                Text("Publish a test set first to view analytics and export CSV.", style = MaterialTheme.typography.bodySmall, color = glass.textMuted)
            } else {
                publishedTests.take(6).forEach { t ->
                    val picked = t.id == selectedTestId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (picked) glass.accentPrimary.copy(alpha = 0.16f) else glass.glassSurface)
                            .border(1.dp, if (picked) glass.accentCyan else glass.glassBorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { selectedTestId = t.id }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${t.title} (${t.resolvedSetType().shortLabel})",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textPrimary,
                            fontWeight = if (picked) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        CoolingGlassBadge("${attempts.count { it.testId == t.id }} att", glass.accentPrimary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                CoolingGlassButton(
                    text = "Export Rankings & Language CSV",
                    icon = Icons.Rounded.Download,
                    enabled = !exportingCsv && selectedTest != null,
                    loading = exportingCsv,
                    testTagName = "export_rankings_csv_btn",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (selectedTest != null) {
                            exportingCsv = true
                            scope.launch {
                                val byUid = testAttempts.associateBy { it.userId }
                                val res = RankingCsvExporter.exportAndShareAsync(
                                    context = context,
                                    scopeTitle = selectedTest.title,
                                    ranked = rankedForTest,
                                    attemptsByUserId = byUid,
                                    test = selectedTest
                                )
                                exportingCsv = false
                                res.onSuccess {
                                    onBannerMessage("Rankings CSV saved to Downloads/TRE4 & ready to share!", false)
                                }.onFailure { err ->
                                    onBannerMessage(err.localizedMessage ?: "CSV export failed", true)
                                }
                            }
                        }
                    }
                )
            }
        }

        // Original Attempt vs Solution Reattempt Inspection Card
        CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(14.dp)) {
            Text(
                text = "Original Attempt vs Solution Re-attempt (${testAttempts.size} Attempts • ${testReattempts.size} Practice)",
                style = MaterialTheme.typography.titleSmall,
                color = glass.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            if (testAttempts.isEmpty()) {
                Text("No locked attempts for this test yet.", style = MaterialTheme.typography.bodySmall, color = glass.textMuted)
            } else {
                val reattemptByUid = testReattempts.associateBy { it.userId }
                testAttempts.sortedByDescending { it.effectiveMeritScore() }.forEach { att ->
                    val practice = reattemptByUid[att.userId]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(glass.glassSurface)
                            .border(0.8.dp, glass.glassBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(att.aspirantName, style = MaterialTheme.typography.bodyMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                            CoolingGlassBadge(
                                text = "Original Merit: ${ExamScoringEngine.formatScore(att.effectiveMeritScore())}/${att.effectiveMeritMaxMarks().toInt()}",
                                color = glass.accentPrimary
                            )
                        }
                        if (selectedTest?.resolvedSetType()?.hasLanguageSection == true) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Language (Qualifying Only): ${ExamScoringEngine.formatScore(att.effectiveLanguageTotal())}/30 (${if (att.langPassed) "PASS" else "FAIL"}) • Hindi ${ExamScoringEngine.formatScore(att.hindiScore)}/22 • Eng ${ExamScoringEngine.formatScore(att.englishScore)}/8",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (att.langPassed) glass.correct else glass.incorrect
                            )
                        }
                        if (practice != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Solution Re-attempt Practice (Non-Ranking): ${practice.totalReattempted} Q practiced • ${practice.correctCount} Right • ${practice.wrongCount} Wrong",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftLavender500,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
