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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamTest
import com.example.data.model.LanguagePref
import com.example.data.model.OptionEMode
import com.example.data.model.SetType
import com.example.domain.ExamScoringEngine
import com.example.ui.glass.CoolingGlassBackground
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassIconButton
import com.example.ui.glass.CoolingGlassTabRow
import com.example.ui.glass.StatPill
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.SoftLavender500

private data class InstructionSectionRow(
    val name: String,
    val questionCount: Int,
    val marksText: String,
    val note: String,
    val isQualifying: Boolean = false
)

@Composable
fun PreTestInstructionScreen(
    test: ExamTest,
    defaultLanguage: LanguagePref = LocalAppLanguage.current,
    onBack: () -> Unit,
    onStartExam: (LanguagePref) -> Unit
) {
    val glass = LocalCoolingGlass.current
    val appLanguage = LocalAppLanguage.current
    var selectedLanguage by remember(defaultLanguage) { mutableStateOf(defaultLanguage) }
    var agreed by remember { mutableStateOf(false) }
    val isEnglishUi = appLanguage == LanguagePref.ENGLISH || selectedLanguage == LanguagePref.ENGLISH

    BackHandler { onBack() }

    val setType = remember(test) { test.resolvedSetType() }
    val posMarks = if (test.positiveMarks > 0.0) test.positiveMarks else 1.0
    val negLabel = test.negativeMarkLabel()
    val optionEMode = test.resolvedOptionEMode()

    val meritMaxDisplay = when {
        setType.isQualifyingOnly -> "0 (Qualifying /30)"
        setType == SetType.PART_5_FULL_MATH_SCI || setType == SetType.PART_6_FULL_MATH -> "120 Marks"
        setType == SetType.LEGACY_COMBINED_70 -> "40 Marks"
        else -> "${(test.questionCount * posMarks).toInt()} Marks"
    }

    val sectionBreakdown = remember(test, setType) {
        when (setType) {
            SetType.PART_1_COMMON -> listOf(
                InstructionSectionRow("Mathematics (Common)", 8, "8 Merit", "Included in Merit"),
                InstructionSectionRow("Science (Common)", 8, "8 Merit", "Included in Merit"),
                InstructionSectionRow("History / INM", 8, "8 Merit", "Included in Merit"),
                InstructionSectionRow("Geography / Indian Geo", 8, "8 Merit", "Included in Merit"),
                InstructionSectionRow("Current Affairs / GK", 8, "8 Merit", "Included in Merit")
            )
            SetType.PART_2_LANGUAGE -> listOf(
                InstructionSectionRow("English Language", 8, "8 Qualifying", "Qualifying Only", true),
                InstructionSectionRow("Hindi Language", 22, "22 Qualifying", "Qualifying Only", true)
            )
            SetType.PART_3_MATHS -> listOf(
                InstructionSectionRow("Part 3 – Mathematics", 40, "40 Merit", "Merit & Rank Base")
            )
            SetType.PART_4_SCIENCE -> listOf(
                InstructionSectionRow("Part 4 – Science", 40, "40 Merit", "Merit & Rank Base")
            )
            SetType.PART_5_FULL_MATH_SCI -> listOf(
                InstructionSectionRow("Language Paper (8 Eng + 22 Hin)", 30, "Qualifying /30", ">= ${test.langPassMarks.toInt()}/30 PASS • Excluded from Merit", true),
                InstructionSectionRow("Part 1 – Common Paper (5 Subjects)", 40, "40 Merit", "Included in 120 Merit"),
                InstructionSectionRow("Part 3 – Mathematics", 40, "40 Merit", "Included in 120 Merit"),
                InstructionSectionRow("Part 4 – Science", 40, "40 Merit", "Included in 120 Merit")
            )
            SetType.PART_6_FULL_MATH -> listOf(
                InstructionSectionRow("Language Paper (8 Eng + 22 Hin)", 30, "Qualifying /30", ">= ${test.langPassMarks.toInt()}/30 PASS • Excluded from Merit", true),
                InstructionSectionRow("Part 1 – Common Paper (5 Subjects)", 40, "40 Merit", "Included in 120 Merit"),
                InstructionSectionRow("Mathematics (Full Subject)", 80, "80 Merit", "Included in 120 Merit")
            )
            SetType.LEGACY_COMBINED_70 -> listOf(
                InstructionSectionRow("Language Paper (8 Eng + 22 Hin)", 30, "Qualifying /30", ">= ${test.langPassMarks.toInt()}/30 PASS • Excluded from Merit", true),
                InstructionSectionRow("Common Paper (5 Subjects)", 40, "40 Merit", "Merit & Rank out of 40")
            )
            else -> listOf(
                InstructionSectionRow(setType.titleEn, test.questionCount.toInt(), "${(test.questionCount * posMarks).toInt()} Merit", "Standard Evaluation")
            )
        }
    }

    CoolingGlassBackground(modifier = Modifier.testTag("pre_test_instruction_screen")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(glass.navBarSurface)
                    .border(0.8.dp, glass.glassBorderSubtle)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoolingGlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back from instructions",
                    testTagName = "pre_test_back_btn",
                    onClick = onBack
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pre-Test Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${setType.shortLabel} • ${setType.classTag}",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.accentPrimary
                    )
                }
                CoolingGlassBadge(text = "${test.durationMinutes} min", color = SoftLavender500)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero Test Identity Card
                CoolingGlassCard(strongFrost = true, borderColor = glass.accentCyan.copy(alpha = 0.6f)) {
                    Text(
                        text = test.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = setType.titleEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.accentPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill("Questions", "${test.questionCount}", glass.accentPrimary, Modifier.weight(1f))
                        StatPill("Merit Base", meritMaxDisplay.substringBefore(" "), glass.correct, Modifier.weight(1f))
                        StatPill("Duration", "${test.durationMinutes}m", SoftLavender500, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CoolingGlassBadge("Marking: +${ExamScoringEngine.formatScore(posMarks)} | $negLabel", glass.accentCyan)
                        CoolingGlassBadge(
                            if (test.pauseAllowed) "Pause Enabled" else "Pause Disabled",
                            if (test.pauseAllowed) glass.correct else glass.incorrect
                        )
                    }
                    if (test.cutoffMarks != null && !setType.isQualifyingOnly) {
                        Spacer(Modifier.height(6.dp))
                        CoolingGlassBadge(
                            text = "Target Cutoff: ${ExamScoringEngine.formatScore(test.cutoffMarks)} Merit Marks",
                            color = glass.review
                        )
                    }
                }

                // Explicit Strict Language Exclusion Warning Banner
                if (setType.hasLanguageSection) {
                    CoolingGlassCard(
                        strongFrost = true,
                        tintColor = SoftLavender500,
                        borderColor = SoftLavender500.copy(alpha = 0.65f),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = SoftLavender500, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isEnglishUi) {
                                    "Strict Language Qualifying Rule"
                                } else {
                                    "Strict Language Qualifying Rule / अनिवार्य भाषा नियम"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (setType.isQualifyingOnly) {
                                "Language Paper (30 Q: 8 English + 22 Hindi) is QUALIFYING ONLY. Minimum ${ExamScoringEngine.formatScore(test.langPassMarks)} / 30 marks required to PASS. It is never added to Merit or Rank."
                            } else {
                                "Language Section (30 Q: 8 English + 22 Hindi) is QUALIFYING ONLY (>= ${ExamScoringEngine.formatScore(test.langPassMarks)} / 30 to PASS). Language marks are STRICTLY EXCLUDED from your Merit Score, Percentage, Cutoff, Percentile, and Rank (${meritMaxDisplay})."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }

                // Section & Scoring Breakdown Table
                CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = glass.accentPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Section & Marking Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    sectionBreakdown.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (row.isQualifying) SoftLavender500.copy(alpha = 0.10f)
                                    else glass.glassSurface
                                )
                                .border(
                                    0.8.dp,
                                    if (row.isQualifying) SoftLavender500.copy(alpha = 0.45f) else glass.glassBorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = row.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (row.isQualifying) SoftLavender500 else glass.textMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${row.questionCount} Q",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = glass.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = row.marksText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (row.isQualifying) SoftLavender500 else glass.correct
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val optionELabel = if (isEnglishUi) "Not Attempted" else ExamQuestion.DEFAULT_OPTION_E
                    Text(
                        text = "• Correct Answer: +${ExamScoringEngine.formatScore(posMarks)} mark\n" +
                            "• Wrong Answer: $negLabel mark\n" +
                            "• Option E ($optionELabel): ${
                                if (optionEMode == OptionEMode.NOT_ATTEMPTED) "0 marks (No negative penalty)"
                                else "Evaluated as standard 5th option"
                            }\n" +
                            "• Completely Unselected / Blank: ${
                                if (test.unattemptedPenalty && test.negativeMarkNumerator > 0L) "$negLabel mark penalty"
                                else "0 marks"
                            }\n" +
                            "• Original Attempt Lock: Once submitted, your score & rank are permanently locked. You may practice again inside Solution Re-attempt Mode without altering your official rank.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }

                // 5-Status Question Palette Color Legend
                CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(14.dp)) {
                    Text(
                        text = "Question Palette Color Legend",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaletteLegendItem(color = Color(0xFF94A3B8), label = "Not Visited — You have not visited this question yet.")
                        PaletteLegendItem(color = glass.incorrect, label = "Not Answered — Visited but left without saving an answer.")
                        PaletteLegendItem(color = glass.correct, label = "Answered — Answer saved and will be evaluated.")
                        PaletteLegendItem(color = SoftLavender500, label = "Marked for Review — Flagged for revisit (not answered yet).")
                        PaletteLegendItem(
                            color = SoftLavender500,
                            showGreenDot = true,
                            label = "Answered & Marked for Review — Answer saved AND flagged for revisit."
                        )
                    }
                }

                // Default Exam Language Selector
                CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Translate, contentDescription = null, tint = glass.accentCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Select Default Exam Language",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "You can also switch live between Hindi / English / Bilingual anytime during the test without losing answers.",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    val langs = listOf(LanguagePref.BILINGUAL, LanguagePref.HINDI, LanguagePref.ENGLISH)
                    CoolingGlassTabRow(
                        tabs = if (isEnglishUi) listOf("Bilingual", "Hindi", "English") else listOf("Bilingual", "हिन्दी", "English"),
                        selectedIndex = langs.indexOf(selectedLanguage).coerceAtLeast(0),
                        onSelect = { selectedLanguage = langs[it] }
                    )
                }

                // Declaration Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pre_test_declaration_checkbox")
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (agreed) glass.correctBg else glass.glassSurfaceStrong)
                        .border(
                            1.2.dp,
                            if (agreed) glass.correct else glass.glassBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { agreed = !agreed }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (agreed) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                        contentDescription = "Agree to instructions",
                        tint = if (agreed) glass.correct else glass.accentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "I have read and understood all instructions, negative marking rules, and the Language Qualifying exclusion policy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(6.dp))
            }

            // Sticky Bottom CTA Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(glass.navBarSurface)
                    .border(0.8.dp, glass.glassBorderSubtle)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                CoolingGlassButton(
                    text = "I am Ready — START TEST",
                    icon = Icons.Rounded.PlayArrow,
                    enabled = agreed,
                    testTagName = "pre_test_start_btn",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onStartExam(selectedLanguage) }
                )
            }
        }
    }
}

@Composable
private fun PaletteLegendItem(
    color: Color,
    label: String,
    showGreenDot: Boolean = false
) {
    val glass = LocalCoolingGlass.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.22f))
                    .border(1.5.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("1", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            }
            if (showGreenDot) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(glass.correct)
                        .border(1.dp, CrystalWhite, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = glass.textSecondary
        )
    }
}
