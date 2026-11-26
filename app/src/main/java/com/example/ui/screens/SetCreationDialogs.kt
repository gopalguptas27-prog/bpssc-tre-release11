package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamTest
import com.example.data.model.LanguagePref
import com.example.data.model.PaperType
import com.example.domain.BulkQuestionParser
import com.example.domain.DocumentImportOutcome
import com.example.domain.OfficialStarterBank
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassBottomSheet
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassDialog
import com.example.ui.glass.CoolingGlassEmptyState
import com.example.ui.glass.CoolingGlassOptionCard
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.CoolingGlassStatusBanner
import com.example.ui.glass.CoolingGlassTextField
import com.example.ui.glass.OptionVisualState
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.GlacierBlue600
import com.example.ui.theme.IceCyan500
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.SoftLavender500
import com.example.ui.theme.appText
import com.example.util.IstClock
import com.example.util.TestSetDraft
import java.util.Date
import java.util.Locale

@Composable
fun QuestionPreviewDialog(
    questions: List<ExamQuestion>,
    initialIndex: Int,
    paperType: PaperType,
    onDismiss: () -> Unit,
    onEditQuestion: (Int) -> Unit
) {
    if (questions.isEmpty()) {
        onDismiss()
        return
    }
    val glass = LocalCoolingGlass.current
    var currentIndex by remember(initialIndex, questions.size) {
        mutableIntStateOf(initialIndex.coerceIn(0, questions.lastIndex))
    }
    val scrollState = rememberScrollState()
    LaunchedEffect(currentIndex) {
        scrollState.scrollTo(0)
    }
    val q = questions[currentIndex]
    val normalizedCorrect = q.correctOption.trim().uppercase()
    val sectionLabel = when {
        paperType == PaperType.COMBINED -> {
            if (q.resolvedSection() == ExamQuestion.SECTION_LANGUAGE) "Section B • Language Paper"
            else "Section A • Common Paper"
        }
        paperType.isFull150Paper() -> when (q.resolvedSection()) {
            ExamQuestion.SECTION_LANGUAGE -> "Part 1 • Language Paper (30 Q)"
            ExamQuestion.SECTION_SUBJECT -> if (paperType == PaperType.CLASS_6_8_MATH_SCIENCE) {
                "Part 3 • Class 6–8 Math & Science (80 Q)"
            } else {
                "Part 3 • Class 9–10 Mathematics (80 Q)"
            }
            else -> "Part 2 • Common Paper (40 Q)"
        }
        else -> null
    }

    CoolingGlassDialog(
        title = "Question Preview (${currentIndex + 1} / ${questions.size})",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 540.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (sectionLabel != null) {
                    CoolingGlassBadge(
                        text = sectionLabel,
                        color = when (q.resolvedSection()) {
                            ExamQuestion.SECTION_LANGUAGE -> SoftLavender500
                            ExamQuestion.SECTION_SUBJECT -> IceCyan500
                            else -> glass.accentPrimary
                        }
                    )
                }
                CoolingGlassBadge(text = q.subject.ifBlank { "General" }, color = glass.accentCyan)
                CoolingGlassBadge(
                    text = if (normalizedCorrect in listOf("A", "B", "C", "D")) "Correct Answer: $normalizedCorrect" else "Key Missing",
                    color = if (normalizedCorrect in listOf("A", "B", "C", "D")) glass.correct else glass.incorrect
                )
                if (q.source.isNotBlank()) {
                    CoolingGlassBadge(text = q.source, color = glass.review)
                }
            }

            if (q.topic.isNotBlank()) {
                Text(
                    text = "Topic: ${q.topic}",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textMuted
                )
            }

            if (q.passage.isNotBlank()) {
                CoolingGlassCard(
                    tintColor = SoftLavender500,
                    borderColor = SoftLavender500.copy(alpha = 0.5f),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Text(
                        text = appText("Passage", "Passage / गद्यांश"),
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftLavender500,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = q.passage,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textPrimary
                    )
                }
            }

            val activeLang = LocalAppLanguage.current
            CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(12.dp)) {
                Text(
                    text = "Q${currentIndex + 1}. ${q.displayText(activeLang)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Preview mode: NEVER auto-select any option (not A, not E).
            // Only the verified correctAnswer key receives the Correct Answer indicator.
            val options = q.optionsList(activeLang)
            options.forEach { (key, text) ->
                val isCorrect = key == normalizedCorrect && normalizedCorrect in listOf("A", "B", "C", "D")
                val state = if (isCorrect) OptionVisualState.CORRECT else OptionVisualState.DEFAULT
                val badge = when {
                    isCorrect -> "Correct Answer"
                    key == "E" -> "0 Marks"
                    else -> null
                }
                val badgeColor = when {
                    isCorrect -> glass.correct
                    key == "E" -> glass.dontKnow
                    else -> null
                }
                CoolingGlassOptionCard(
                    optionKey = key,
                    optionText = text,
                    state = state,
                    enabled = false,
                    badgeText = badge,
                    badgeColor = badgeColor,
                    onClick = {}
                )
            }

            CoolingGlassCard(
                tintColor = glass.accentCyan,
                contentPadding = PaddingValues(12.dp)
            ) {
                Text(
                    text = "Explanation",
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = q.explanation.ifBlank { "No explanation added yet. Tap Edit to add a solution explanation." },
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoolingGlassOutlinedButton(
                    text = "Prev",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f),
                    onClick = { currentIndex = (currentIndex - 1).coerceAtLeast(0) }
                )
                CoolingGlassOutlinedButton(
                    text = "Edit",
                    icon = Icons.Rounded.Edit,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onEditQuestion(currentIndex)
                    }
                )
                CoolingGlassButton(
                    text = "Next",
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    enabled = currentIndex < questions.lastIndex,
                    modifier = Modifier.weight(1f),
                    onClick = { currentIndex = (currentIndex + 1).coerceAtMost(questions.lastIndex) }
                )
            }
        }
    }
}

@Composable
fun EditQuestionDialog(
    question: ExamQuestion,
    questionNumber: Int,
    paperType: PaperType,
    forcedSection: String?,
    onDismiss: () -> Unit,
    onSave: (ExamQuestion) -> Unit
) {
    val glass = LocalCoolingGlass.current
    val effectivePaper = when (forcedSection) {
        ExamQuestion.SECTION_LANGUAGE -> PaperType.LANGUAGE
        ExamQuestion.SECTION_COMMON -> PaperType.COMMON_GS
        ExamQuestion.SECTION_SUBJECT -> paperType
        else -> paperType
    }
    val subjectOptions = when {
        forcedSection == ExamQuestion.SECTION_SUBJECT && paperType == PaperType.CLASS_6_8_MATH_SCIENCE ->
            BulkQuestionParser.CLASS_6_8_SUBJECTS
        forcedSection == ExamQuestion.SECTION_SUBJECT && paperType == PaperType.CLASS_9_10_MATH ->
            BulkQuestionParser.CLASS_9_10_SUBJECTS
        paperType == PaperType.CLASS_6_8_MATH -> listOf("Mathematics")
        paperType == PaperType.CLASS_6_8_SCIENCE -> listOf("Science")
        paperType == PaperType.CLASS_9_10_MATH_80 -> listOf("Mathematics")
        effectivePaper == PaperType.LANGUAGE -> BulkQuestionParser.LANG_SUBJECTS
        effectivePaper == PaperType.COMMON_GS -> BulkQuestionParser.GS_SUBJECTS
        else -> BulkQuestionParser.ALL_STANDARD_SUBJECTS
    }

    var editSubj by remember(question) {
        val current = BulkQuestionParser.normalizeSubject(question.subject, effectivePaper)
        mutableStateOf(if (current == BulkQuestionParser.UNCLASSIFIED_SUBJECT) BulkQuestionParser.UNCLASSIFIED_SUBJECT else current)
    }
    var editTopic by remember(question) { mutableStateOf(question.topic) }
    var editSource by remember(question) { mutableStateOf(question.source) }
    var editPassage by remember(question) { mutableStateOf(question.passage) }
    var editText by remember(question) { mutableStateOf(question.text) }
    var editA by remember(question) { mutableStateOf(question.optionA) }
    var editB by remember(question) { mutableStateOf(question.optionB) }
    var editC by remember(question) { mutableStateOf(question.optionC) }
    var editD by remember(question) { mutableStateOf(question.optionD) }
    val activeLang = LocalAppLanguage.current
    var editE by remember(question, activeLang) {
        val norm = ExamQuestion.normalizeOptionE(question.optionE)
        mutableStateOf(if (activeLang == LanguagePref.ENGLISH && norm == ExamQuestion.DEFAULT_OPTION_E) "Not Attempted" else norm)
    }
    var editAns by remember(question) {
        mutableStateOf(question.correctOption.trim().uppercase().takeIf { it in listOf("A", "B", "C", "D") } ?: "")
    }
    var editExp by remember(question) { mutableStateOf(question.explanation) }

    CoolingGlassDialog(title = "Edit Question Q$questionNumber", onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Subject",
                style = MaterialTheme.typography.labelMedium,
                color = glass.textSecondary,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                subjectOptions.forEach { subj ->
                    val selected = editSubj.equals(subj, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) GlacierBlue600 else glass.glassSurfaceStrong)
                            .border(
                                1.dp,
                                if (selected) IceCyan500 else glass.glassBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { editSubj = subj }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = subj.substringBefore(" /"),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) CrystalWhite else glass.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoolingGlassTextField(value = editTopic, onValueChange = { editTopic = it }, label = "Topic", modifier = Modifier.weight(1f))
                CoolingGlassTextField(value = editSource, onValueChange = { editSource = it }, label = "Source", modifier = Modifier.weight(1f))
            }
            if (effectivePaper == PaperType.LANGUAGE || editPassage.isNotBlank()) {
                CoolingGlassTextField(
                    value = editPassage,
                    onValueChange = { editPassage = it },
                    label = appText("Passage (Optional)", "Passage / गद्यांश (Optional)"),
                    singleLine = false,
                    minLines = 2
                )
            }
            CoolingGlassTextField(value = editText, onValueChange = { editText = it }, label = "Question Text", singleLine = false, minLines = 2)
            CoolingGlassTextField(value = editA, onValueChange = { editA = it }, label = "Option A", singleLine = false, maxLines = 4)
            CoolingGlassTextField(value = editB, onValueChange = { editB = it }, label = "Option B", singleLine = false, maxLines = 4)
            CoolingGlassTextField(value = editC, onValueChange = { editC = it }, label = "Option C", singleLine = false, maxLines = 4)
            CoolingGlassTextField(value = editD, onValueChange = { editD = it }, label = "Option D", singleLine = false, maxLines = 4)
            CoolingGlassTextField(value = editE, onValueChange = { editE = it }, label = "Option E (0 Marks / Not Attempted)", singleLine = false, maxLines = 2)

            Text(
                text = "Correct Answer Key (A–D) • Option E (Not Attempted) = 0 marks",
                style = MaterialTheme.typography.labelMedium,
                color = glass.textSecondary,
                fontWeight = FontWeight.Bold
            )
            AnswerKeySelectorRow(selectedKey = editAns, onSelect = { editAns = it })

            CoolingGlassTextField(
                value = editExp,
                onValueChange = { editExp = it },
                label = "Detailed Explanation",
                singleLine = false,
                minLines = 2
            )

            if (editSubj == BulkQuestionParser.UNCLASSIFIED_SUBJECT) {
                CoolingGlassStatusBanner(
                    message = "Subject is Unclassified. Please select a valid canonical subject above before saving.",
                    isError = true
                )
            }
            Spacer(Modifier.height(4.dp))
            CoolingGlassButton(
                text = "Save Question Changes",
                icon = Icons.Rounded.CheckCircle,
                enabled = editSubj != BulkQuestionParser.UNCLASSIFIED_SUBJECT &&
                    editText.isNotBlank() &&
                    editA.isNotBlank() &&
                    editB.isNotBlank() &&
                    editC.isNotBlank() &&
                    editD.isNotBlank() &&
                    editAns in listOf("A", "B", "C", "D"),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val normalizedSubj = BulkQuestionParser.normalizeSubject(editSubj, effectivePaper)
                    val resolvedSection = when {
                        forcedSection == ExamQuestion.SECTION_SUBJECT -> ExamQuestion.SECTION_SUBJECT
                        normalizedSubj == "Hindi" || normalizedSubj == "English" -> ExamQuestion.SECTION_LANGUAGE
                        paperType.isSeparateSubjectPaper() -> ExamQuestion.SECTION_SUBJECT
                        else -> ExamQuestion.SECTION_COMMON
                    }
                    onSave(
                        question.copy(
                            subject = normalizedSubj,
                            topic = editTopic.trim(),
                            source = editSource.trim(),
                            passage = editPassage.trim(),
                            text = editText.trim(),
                            optionA = editA.trim(),
                            optionB = editB.trim(),
                            optionC = editC.trim(),
                            optionD = editD.trim(),
                            optionE = ExamQuestion.normalizeOptionE(editE),
                            correctOption = editAns,
                            explanation = editExp.trim(),
                            section = resolvedSection
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun AnswerKeySelectorRow(
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalCoolingGlass.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("A", "B", "C", "D").forEach { key ->
            val picked = selectedKey.equals(key, ignoreCase = true)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (picked) glass.correct else glass.glassSurfaceStrong)
                    .border(
                        width = if (picked) 1.5.dp else 1.dp,
                        color = if (picked) glass.correct else glass.glassBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(key) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Opt $key",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (picked) CrystalWhite else glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SavedDraftsSheet(
    drafts: List<TestSetDraft>,
    onLoadDraft: (TestSetDraft) -> Unit,
    onDeleteDraft: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    val fmt = remember { IstClock.newDateFormat("dd MMM, hh:mm a 'IST'") }

    CoolingGlassBottomSheet(title = "Saved Test Set Drafts (${drafts.size})", onDismiss = onDismiss) {
        if (drafts.isEmpty()) {
            CoolingGlassEmptyState(
                title = "No Saved Drafts Yet",
                subtitle = "Tap 'Save Draft' while building a test set to resume editing anytime before publishing."
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                drafts.forEach { draft ->
                    val qCount = when {
                        draft.paperType == PaperType.COMBINED ->
                            draft.commonSectionQuestions.size + draft.languageSectionQuestions.size
                        draft.paperType.isFull150Paper() ->
                            draft.languageSectionQuestions.size + draft.commonSectionQuestions.size + draft.subjectSectionQuestions.size
                        else -> draft.questions.size
                    }
                    CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = draft.title.ifBlank { "Untitled ${draft.paperType.titleEn}" },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CoolingGlassBadge(draft.paperType.name.replace("_", " "), glass.accentPrimary)
                                    CoolingGlassBadge("$qCount Qs • ${draft.durationMinutes}m", SoftLavender500)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Saved ${fmt.format(Date(draft.savedAtMillis))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textMuted
                                )
                            }
                            IconButton(onClick = { onDeleteDraft(draft.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete draft", tint = glass.incorrect)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        CoolingGlassButton(
                            text = "Resume Draft",
                            icon = Icons.Rounded.Restore,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onLoadDraft(draft) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UploadedDocumentReviewDialog(
    outcome: DocumentImportOutcome,
    targetLabel: String,
    targetPaperType: PaperType,
    onDismiss: () -> Unit,
    onConfirmImport: (List<ExamQuestion>) -> Unit
) {
    val glass = LocalCoolingGlass.current
    var editableText by remember(outcome) { mutableStateOf(outcome.extractedText) }
    var parsedQuestions by remember(outcome) { mutableStateOf(outcome.questions) }
    var parseError by remember(outcome) { mutableStateOf(outcome.errorMessage) }

    CoolingGlassDialog(title = "${outcome.format.label} Review", onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoolingGlassBadge(text = outcome.format.name, color = glass.accentPrimary)
                CoolingGlassBadge(text = targetLabel, color = SoftLavender500)
                CoolingGlassBadge(
                    text = "${parsedQuestions.size} MCQs Found",
                    color = if (parsedQuestions.isNotEmpty()) glass.correct else glass.review
                )
            }
            Text(
                text = "File: ${outcome.fileName}",
                style = MaterialTheme.typography.labelMedium,
                color = glass.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val currentParseError = parseError
            if (!currentParseError.isNullOrBlank()) {
                CoolingGlassStatusBanner(message = currentParseError, isError = true)
            } else if (!outcome.warningMessage.isNullOrBlank() && parsedQuestions.isEmpty()) {
                CoolingGlassStatusBanner(message = outcome.warningMessage, isError = true)
            } else if (parsedQuestions.isNotEmpty()) {
                val subjectBreakdown = remember(parsedQuestions) {
                    parsedQuestions.groupingBy { BulkQuestionParser.normalizeSubject(it.subject, targetPaperType) }.eachCount()
                }
                val unclassifiedCnt = subjectBreakdown[BulkQuestionParser.UNCLASSIFIED_SUBJECT] ?: 0
                var reviewIdx by remember(parsedQuestions) { mutableIntStateOf(0) }
                val safeIdx = reviewIdx.coerceIn(0, parsedQuestions.lastIndex)
                val previewQ = parsedQuestions[safeIdx]

                CoolingGlassStatusBanner(
                    message = if (unclassifiedCnt > 0) {
                        "Parsed ${parsedQuestions.size} MCQs ($unclassifiedCnt Unclassified — fix via Bulk Subject Fix after import)."
                    } else {
                        "Parsed & auto-classified ${parsedQuestions.size} MCQs (Option E auto-added)."
                    },
                    isError = unclassifiedCnt > 0
                )

                // Per-Subject Breakdown Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjectBreakdown.forEach { (subj, cnt) ->
                        CoolingGlassBadge(
                            text = "$subj: $cnt",
                            color = when (subj) {
                                BulkQuestionParser.UNCLASSIFIED_SUBJECT -> glass.incorrect
                                "Hindi", "English" -> SoftLavender500
                                else -> glass.accentPrimary
                            }
                        )
                    }
                }

                // Interactive Pre-Import Question Preview Card (browse all imported MCQs)
                CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Preview Q${safeIdx + 1}/${parsedQuestions.size} • ${previewQ.subject}",
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.accentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        CoolingGlassBadge(
                            text = if (previewQ.correctOption in listOf("A", "B", "C", "D")) "Key: ${previewQ.correctOption}" else "Key Missing",
                            color = if (previewQ.correctOption in listOf("A", "B", "C", "D")) glass.correct else glass.incorrect
                        )
                    }
                    if (previewQ.topic.isNotBlank()) {
                        Text("Topic: ${previewQ.topic}", style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(previewQ.text, style = MaterialTheme.typography.bodySmall, color = glass.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    previewQ.optionsList().forEach { (k, v) ->
                        Text(
                            text = "$k) $v",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (k == previewQ.correctOption) glass.correct else glass.textSecondary,
                            fontWeight = if (k == previewQ.correctOption) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (previewQ.explanation.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Exp: ${previewQ.explanation}", style = MaterialTheme.typography.labelSmall, color = glass.textMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    if (parsedQuestions.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CoolingGlassOutlinedButton(
                                text = "Prev",
                                enabled = safeIdx > 0,
                                modifier = Modifier.weight(1f),
                                onClick = { reviewIdx = (safeIdx - 1).coerceAtLeast(0) }
                            )
                            CoolingGlassOutlinedButton(
                                text = "Next",
                                enabled = safeIdx < parsedQuestions.lastIndex,
                                modifier = Modifier.weight(1f),
                                onClick = { reviewIdx = (safeIdx + 1).coerceAtMost(parsedQuestions.lastIndex) }
                            )
                        }
                    }
                }
            }

            if (editableText.isNotBlank()) {
                CoolingGlassTextField(
                    value = editableText,
                    onValueChange = { editableText = it },
                    label = "Extracted Document Text (Editable)",
                    singleLine = false,
                    minLines = 5,
                    maxLines = 10
                )
                CoolingGlassOutlinedButton(
                    text = "Re-Parse Edited Text",
                    icon = Icons.Rounded.AutoAwesome,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val trimmed = editableText.trim()
                        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                            BulkQuestionParser.parseJson(trimmed, targetPaperType)
                                .onSuccess {
                                    parsedQuestions = it
                                    parseError = null
                                }
                                .onFailure { parseError = it.message }
                        } else if (outcome.format == com.example.domain.ImportFileFormat.CSV) {
                            val list = BulkQuestionParser.parseCsv(trimmed, targetPaperType)
                            parsedQuestions = list
                            parseError = if (list.isEmpty()) "No valid CSV rows matched." else null
                        } else {
                            val list = BulkQuestionParser.parseBulkText(trimmed, targetPaperType)
                            parsedQuestions = list
                            parseError = if (list.isEmpty()) "Format questions as: 1. Text / A) ... B) ... C) ... D) ... Answer: B" else null
                        }
                    }
                )
            }

            CoolingGlassButton(
                text = "Import ${parsedQuestions.size} Questions",
                icon = Icons.Rounded.CheckCircle,
                enabled = parsedQuestions.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                onClick = { onConfirmImport(parsedQuestions) }
            )
        }
    }
}

@Composable
fun CombinedAutoCombineDialog(
    commonPapers: List<ExamTest>,
    languagePapers: List<ExamTest>,
    isFull150: Boolean = false,
    onDismiss: () -> Unit,
    onApplyCombination: (common: List<ExamQuestion>, language: List<ExamQuestion>, suggestedTitle: String) -> Unit
) {
    val glass = LocalCoolingGlass.current
    // Index 0 = Official Starter Bank, 1..N = Group Papers
    var selectedCommonIdx by remember { mutableIntStateOf(if (commonPapers.isNotEmpty()) 1 else 0) }
    var selectedLangIdx by remember { mutableIntStateOf(if (languagePapers.isNotEmpty()) 1 else 0) }

    val commonOptions = remember(commonPapers) {
        listOf("Official TRE 4.0 Common Bank (40 Q)") + commonPapers.map { "${it.title} (${it.questionCount} Q)" }
    }
    val langOptions = remember(languagePapers) {
        listOf("Official TRE 4.0 Language Bank (30 Q)") + languagePapers.map { "${it.title} (${it.questionCount} Q)" }
    }

    CoolingGlassDialog(
        title = if (isFull150) "Load Language (30 Q) + Common (40 Q)" else "Auto-Combine 30 Language + 40 Common",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isFull150) {
                    "Auto-fill Part 1 (30 Language Qs) and Part 2 (40 Common Qs) into your 150-question paper without mixing sections or answer keys. Then add/import 80 Subject questions into Part 3."
                } else {
                    "Select one 30-Q Language Paper and one 40-Q Common Paper. Sections and answer keys remain strictly isolated."
                },
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary
            )

            Text(
                text = "Language Paper (30 Q • 8 En + 22 Hi)",
                style = MaterialTheme.typography.labelLarge,
                color = SoftLavender500,
                fontWeight = FontWeight.Bold
            )
            langOptions.forEachIndexed { idx, label ->
                val picked = selectedLangIdx == idx
                CoolingGlassCard(
                    strongFrost = picked,
                    tintColor = if (picked) SoftLavender500 else null,
                    borderColor = if (picked) SoftLavender500 else glass.glassBorder,
                    contentPadding = PaddingValues(10.dp),
                    onClick = { selectedLangIdx = idx }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textPrimary,
                            fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (picked) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = glass.correct)
                        }
                    }
                }
            }

            Text(
                text = "Common Paper (40 Q • Math/Sci/Geo/INM/PIB)",
                style = MaterialTheme.typography.labelLarge,
                color = glass.accentPrimary,
                fontWeight = FontWeight.Bold
            )
            commonOptions.forEachIndexed { idx, label ->
                val picked = selectedCommonIdx == idx
                CoolingGlassCard(
                    strongFrost = picked,
                    tintColor = if (picked) glass.accentPrimary else null,
                    borderColor = if (picked) glass.accentCyan else glass.glassBorder,
                    contentPadding = PaddingValues(10.dp),
                    onClick = { selectedCommonIdx = idx }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textPrimary,
                            fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (picked) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = glass.correct)
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            CoolingGlassButton(
                text = if (isFull150) "Apply 30 Lang + 40 Common (70/150)" else "Combine 70 Questions (45 Min)",
                icon = Icons.Rounded.Merge,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val commonQs = if (selectedCommonIdx == 0) {
                        OfficialStarterBank.commonGs40Questions()
                    } else {
                        commonPapers[selectedCommonIdx - 1].parsedQuestions()
                    }
                    val langQs = if (selectedLangIdx == 0) {
                        OfficialStarterBank.language30Questions()
                    } else {
                        languagePapers[selectedLangIdx - 1].parsedQuestions()
                    }
                    onApplyCombination(commonQs, langQs, "Language + Common Paper Set 1")
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun BulkSubjectCorrectionDialog(
    questions: List<ExamQuestion>,
    paperType: PaperType,
    onDismiss: () -> Unit,
    onAutoFixSections: (() -> Unit)? = null,
    onApplyReassignment: (targetSubject: String, selectedIndices: Set<Int>, onlyUnclassified: Boolean) -> Unit
) {
    val glass = LocalCoolingGlass.current
    val unclassifiedIndices = remember(questions) {
        questions.mapIndexedNotNull { idx, q ->
            if (BulkQuestionParser.normalizeSubject(q.subject, paperType) == BulkQuestionParser.UNCLASSIFIED_SUBJECT) idx else null
        }.toSet()
    }
    var selectedIndices by remember(questions) {
        mutableStateOf(unclassifiedIndices)
    }
    val availableSubjects = when (paperType) {
        PaperType.LANGUAGE -> BulkQuestionParser.LANG_SUBJECTS
        PaperType.COMMON_GS -> BulkQuestionParser.GS_SUBJECTS
        PaperType.CLASS_6_8_MATH -> listOf("Mathematics")
        PaperType.CLASS_6_8_SCIENCE -> listOf("Science")
        PaperType.CLASS_9_10_MATH_80 -> listOf("Mathematics")
        else -> BulkQuestionParser.ALL_STANDARD_SUBJECTS
    }
    var targetSubject by remember(availableSubjects) { mutableStateOf(availableSubjects.first()) }

    CoolingGlassDialog(title = "Bulk Subject & Section Fix", onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Auto-split Part 1 / Part 2 / Part 3 or assign selected/Unclassified questions to their canonical subject.",
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary
            )

            if (onAutoFixSections != null) {
                CoolingGlassButton(
                    text = "Fix Section / Auto-Fix (Part 1 • Part 2 • Part 3)",
                    icon = Icons.Rounded.AutoAwesome,
                    testTagName = "dialog_auto_fix_sections_btn",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onAutoFixSections()
                        onDismiss()
                    }
                )
            }

            Text(
                text = "Target Subject",
                style = MaterialTheme.typography.labelLarge,
                color = glass.accentPrimary,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableSubjects.forEach { subj ->
                    val picked = targetSubject == subj
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (picked) GlacierBlue600 else glass.glassSurfaceStrong)
                            .border(1.dp, if (picked) IceCyan500 else glass.glassBorder, RoundedCornerShape(12.dp))
                            .clickable { targetSubject = subj }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = subj,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (picked) CrystalWhite else glass.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (unclassifiedIndices.isNotEmpty()) {
                CoolingGlassButton(
                    text = "Fix All ${unclassifiedIndices.size} Unclassified -> $targetSubject",
                    icon = Icons.Rounded.AutoAwesome,
                    testTagName = "bulk_fix_unclassified_btn",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onApplyReassignment(targetSubject, unclassifiedIndices, true)
                        onDismiss()
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Questions (${selectedIndices.size}/${questions.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (selectedIndices.size == questions.size) "Clear All" else "Select All",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.accentPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            selectedIndices = if (selectedIndices.size == questions.size) emptySet()
                            else questions.indices.toSet()
                        }
                        .padding(4.dp)
                )
            }

            questions.forEachIndexed { idx, q ->
                val isSelected = idx in selectedIndices
                val isUnclassified = idx in unclassifiedIndices
                CoolingGlassCard(
                    strongFrost = isSelected,
                    tintColor = when {
                        isSelected -> GlacierBlue600
                        isUnclassified -> glass.incorrect
                        else -> null
                    },
                    borderColor = when {
                        isSelected -> IceCyan500
                        isUnclassified -> glass.incorrect
                        else -> glass.glassBorder
                    },
                    contentPadding = PaddingValues(10.dp),
                    onClick = {
                        selectedIndices = if (isSelected) selectedIndices - idx else selectedIndices + idx
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Q${idx + 1}: ${q.text}",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        CoolingGlassBadge(
                            text = q.subject,
                            color = if (isUnclassified) glass.incorrect else SoftLavender500
                        )
                    }
                }
            }

            CoolingGlassButton(
                text = "Apply '$targetSubject' to ${selectedIndices.size} Selected",
                icon = Icons.Rounded.CheckCircle,
                enabled = selectedIndices.isNotEmpty(),
                testTagName = "bulk_apply_selected_subject_btn",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onApplyReassignment(targetSubject, selectedIndices, false)
                    onDismiss()
                }
            )
        }
    }
}

