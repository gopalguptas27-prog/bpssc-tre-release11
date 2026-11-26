package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Drafts
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppRole
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamTest
import com.example.data.model.GroupMember
import com.example.data.model.PaperType
import com.example.data.model.RankCategory
import com.example.data.model.TestSetNaming
import com.example.domain.BulkQuestionParser
import com.example.domain.DocumentImportOutcome
import com.example.domain.DocumentQuestionImporter
import com.example.domain.ImportFileFormat
import com.example.domain.OfficialStarterBank
import com.example.ui.glass.AnimatedBase64Image
import com.example.ui.glass.AutoResizeSingleLineText
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassConfirmationPopup
import com.example.ui.glass.CoolingGlassDialog
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.CoolingGlassStatusBanner
import com.example.ui.glass.CoolingGlassTabRow
import com.example.ui.glass.CoolingGlassTextField
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.GlacierBlue600
import com.example.ui.theme.IceCyan500
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.SoftLavender500
import com.example.ui.theme.appText
import com.example.util.EditorAutoDraft
import com.example.util.ImageCodec
import com.example.util.JsonValidationResult
import com.example.util.SetIconOption
import com.example.util.TestDraftStore
import com.example.util.TestSetDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun QuestionBankScreen(
    currentMember: GroupMember?,
    busy: Boolean,
    existingTests: List<ExamTest> = emptyList(),
    editingTest: ExamTest? = null,
    draftStore: TestDraftStore? = null,
    onCancelEdit: () -> Unit = {},
    onCreateTestSet: (
        title: String,
        paperType: PaperType,
        difficulty: String,
        durationMinutes: Int,
        questions: List<ExamQuestion>,
        publishNow: Boolean,
        unattemptedPenalty: Boolean,
        answerReleaseMode: String,
        iconKey: String?,
        customIconBase64: String?,
        bannerImageBase64: String?
    ) -> Unit,
    onUpdateTestSet: (
        testId: String,
        title: String,
        difficulty: String,
        durationMinutes: Int,
        questions: List<ExamQuestion>,
        unattemptedPenalty: Boolean,
        answerReleaseMode: String,
        iconKey: String?,
        customIconBase64: String?,
        bannerImageBase64: String?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onBannerMessage: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalCoolingGlass.current
    val clipboard = LocalClipboardManager.current
    val isAdmin = currentMember?.appRole()?.isAdminRole() == true

    // STRICT ROLE-BASED ACCESS CONTROL: Only ADMIN can access Question Bank & Set Creation
    if (!isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .testTag("question_bank_admin_only_guard"),
            contentAlignment = Alignment.Center
        ) {
            CoolingGlassCard(strongFrost = true, borderColor = glass.incorrect) {
                Text(
                    text = "Admin Access Only",
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Question Bank, Set Creation, JSON/CSV/PDF/DOCX Import, and Question Editing are restricted exclusively to Admin accounts. You can attempt all published test sets from the Tests tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
            }
        }
        return
    }

    var activeDraftId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var importModeTab by remember { mutableIntStateOf(0) } // 0=Upload, 1=Manual, 2=JSON/CSV, 3=Bulk/Bank
    var selectedPaperType by remember { mutableStateOf(PaperType.COMBINED) }
    var setTitle by remember { mutableStateOf(TestSetNaming.nextUniqueTitle(PaperType.COMBINED, existingTests)) }
    var userEditedTitle by remember { mutableStateOf(false) }
    var difficultyIndex by remember { mutableIntStateOf(1) }
    val difficulty = listOf("EASY", "MODERATE", "HARD")[difficultyIndex]
    var durationStr by remember { mutableStateOf(PaperType.COMBINED.defaultDurationMinutes.toString()) }
    var unattemptedPenalty by remember { mutableStateOf(true) }
    var releaseModeIndex by remember { mutableIntStateOf(0) }
    val answerReleaseMode = if (releaseModeIndex == 0) "IMMEDIATE" else "SCHEDULED"
    var publishImmediately by remember { mutableStateOf(true) }
    // Null by default so newly created sets automatically inherit the Admin's 7-category default icon!
    var selectedIconKey by remember { mutableStateOf<String?>(null) }
    var selectedCustomIconBase64 by remember { mutableStateOf<String?>(null) }
    var selectedBannerImageBase64 by remember { mutableStateOf<String?>(null) }
    var showIconPickerDialog by remember { mutableStateOf(false) }
    var showBulkSubjectDialog by remember { mutableStateOf(false) }

    val setBannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    ImageCodec.encodeUriToBase64Result(
                        context = context,
                        uri = uri,
                        maxDimension = 720,
                        quality = 66,
                        allowGif = true,
                        maxBase64Len = ImageCodec.DEFAULT_MAX_BANNER_BASE64_LEN
                    )
                }
                res.onSuccess { b64 ->
                    selectedBannerImageBase64 = b64
                    val kind = if (ImageCodec.isGifBase64(b64)) "Animated GIF" else if (ImageCodec.isPngBase64(b64)) "PNG" else "Image"
                    onBannerMessage("Set Banner ($kind) attached! Preview is live below.", false)
                }.onFailure { err ->
                    onBannerMessage(err.message ?: "Unable to load set banner image.", true)
                }
            }
        }
    }

    // Section isolation:
    // - COMBINED: 0 = Common (40), 1 = Language (30)
    // - FULL 150-Q: 0 = Language (30), 1 = Common (40), 2 = Subject (80)
    var combinedTargetSection by remember { mutableIntStateOf(0) }
    var full150TargetSection by remember { mutableIntStateOf(2) }
    val commonSectionDraft = remember { mutableStateListOf<ExamQuestion>() }
    val languageSectionDraft = remember { mutableStateListOf<ExamQuestion>() }
    val subjectSectionDraft = remember { mutableStateListOf<ExamQuestion>() }
    val singlePaperDraft = remember { mutableStateListOf<ExamQuestion>() }

    // Input fields
    var bulkInput by remember { mutableStateOf("") }
    var jsonCsvInput by remember { mutableStateOf("") }
    var qSubject by remember { mutableStateOf("Mathematics") }
    var qTopic by remember { mutableStateOf("") }
    var qSource by remember { mutableStateOf("NCERT Class 8") }
    var qPassage by remember { mutableStateOf("") }
    var qText by remember { mutableStateOf("") }
    var qOptA by remember { mutableStateOf("") }
    var qOptB by remember { mutableStateOf("") }
    var qOptC by remember { mutableStateOf("") }
    var qOptD by remember { mutableStateOf("") }
    var qAnswer by remember { mutableStateOf("A") }
    var qExp by remember { mutableStateOf("") }

    // Dialog / sheet states
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    var showDraftsSheet by remember { mutableStateOf(false) }
    var showCombineDialog by remember { mutableStateOf(false) }
    var importingFile by remember { mutableStateOf(false) }
    var pendingFormat by remember { mutableStateOf(ImportFileFormat.PDF) }
    var uploadReviewOutcome by remember { mutableStateOf<DocumentImportOutcome?>(null) }
    var savedDrafts by remember(draftStore) { mutableStateOf(draftStore?.loadAllDrafts().orEmpty()) }

    // Admin JSON Editor Data Protection: Auto-Save, Recovery, Dirty Tracking & Strict JSON Validation
    val autoDraftKey = remember(editingTest?.id) { editingTest?.id ?: "new_set_editor" }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showUnsavedDiscardConfirm by remember { mutableStateOf(false) }
    var strictJsonValidationError by remember { mutableStateOf<JsonValidationResult?>(null) }
    var recoveredAutoDraft by remember(draftStore, autoDraftKey) {
        mutableStateOf(draftStore?.loadAutoDraft(autoDraftKey))
    }

    // Intercept Back press when the Admin has unsaved changes in the editor
    BackHandler(enabled = hasUnsavedChanges) {
        showUnsavedDiscardConfirm = true
    }

    // Keep unique sequential set name synced whenever existingTests loads (if not manually edited)
    LaunchedEffect(existingTests.size, selectedPaperType, editingTest?.id) {
        if (editingTest == null && !userEditedTitle) {
            setTitle = TestSetNaming.nextUniqueTitle(selectedPaperType, existingTests)
        }
    }

    // Load editing test if opened in Edit Test mode
    LaunchedEffect(editingTest?.id) {
        if (editingTest != null) {
            setTitle = editingTest.title
            userEditedTitle = true
            val pType = editingTest.parsedPaperType()
            selectedPaperType = pType
            durationStr = editingTest.durationMinutes.toString()
            difficultyIndex = listOf("EASY", "MODERATE", "HARD").indexOf(editingTest.difficulty).coerceAtLeast(1)
            unattemptedPenalty = editingTest.unattemptedPenalty
            releaseModeIndex = if (editingTest.answerReleaseMode == "SCHEDULED") 1 else 0
            selectedIconKey = editingTest.iconKey?.takeIf { it.isNotBlank() }
            selectedCustomIconBase64 = editingTest.customIconBase64?.takeIf { it.isNotBlank() }
            selectedBannerImageBase64 = editingTest.bannerImageBase64?.takeIf { it.isNotBlank() }
            val parsed = editingTest.parsedQuestions()
            if (pType == PaperType.COMBINED) {
                val part = BulkQuestionParser.partitionImportedQuestions(parsed, pType)
                commonSectionDraft.clear()
                languageSectionDraft.clear()
                commonSectionDraft.addAll(part.common)
                languageSectionDraft.addAll(part.language)
            } else if (pType.isFull150Paper()) {
                val part = BulkQuestionParser.partitionImportedQuestions(parsed, pType)
                languageSectionDraft.clear()
                commonSectionDraft.clear()
                subjectSectionDraft.clear()
                languageSectionDraft.addAll(part.language)
                commonSectionDraft.addAll(part.common)
                subjectSectionDraft.addAll(part.subject)
            } else {
                singlePaperDraft.clear()
                singlePaperDraft.addAll(parsed)
            }
        }
    }

    // Active questions list
    val effectiveQuestions: List<ExamQuestion> = when {
        selectedPaperType == PaperType.COMBINED -> BulkQuestionParser.combineSections(
            commonSectionDraft.toList(),
            languageSectionDraft.toList()
        ).questions
        selectedPaperType.isFull150Paper() -> BulkQuestionParser.combineFull150Sections(
            languageQuestions = languageSectionDraft.toList(),
            commonQuestions = commonSectionDraft.toList(),
            subjectQuestions = subjectSectionDraft.toList(),
            targetType = selectedPaperType
        ).questions
        else -> singlePaperDraft.mapIndexed { idx, q -> q.copy(number = idx + 1) }
    }

    // Automatically persist in-progress edits to TestDraftStore so keyboard close / rotation / reload never loses data
    LaunchedEffect(
        hasUnsavedChanges,
        setTitle,
        selectedPaperType,
        difficulty,
        durationStr,
        jsonCsvInput,
        bulkInput,
        effectiveQuestions.size
    ) {
        if (hasUnsavedChanges && draftStore != null) {
            val rawToSave = jsonCsvInput.ifBlank { bulkInput }
            draftStore.saveAutoDraft(
                EditorAutoDraft(
                    editorKey = autoDraftKey,
                    rawJsonText = rawToSave,
                    title = setTitle,
                    paperType = selectedPaperType,
                    difficulty = difficulty,
                    durationMinutes = durationStr.toIntOrNull() ?: selectedPaperType.defaultDurationMinutes,
                    questions = effectiveQuestions
                )
            )
        }
    }

    val activeImportPaperType = when {
        selectedPaperType == PaperType.COMBINED && combinedTargetSection == 0 -> PaperType.COMMON_GS
        selectedPaperType == PaperType.COMBINED && combinedTargetSection == 1 -> PaperType.LANGUAGE
        selectedPaperType.isFull150Paper() && full150TargetSection == 0 -> PaperType.LANGUAGE
        selectedPaperType.isFull150Paper() && full150TargetSection == 1 -> PaperType.COMMON_GS
        else -> selectedPaperType
    }

    val activeTargetLabel = when {
        selectedPaperType == PaperType.COMBINED && combinedTargetSection == 0 -> "Section A: Common Paper (40 Q)"
        selectedPaperType == PaperType.COMBINED && combinedTargetSection == 1 -> "Section B: Language Paper (30 Q)"
        selectedPaperType.isFull150Paper() && full150TargetSection == 0 -> "Part 1: Language Paper (30 Q)"
        selectedPaperType.isFull150Paper() && full150TargetSection == 1 -> "Part 2: Common Paper (40 Q)"
        selectedPaperType == PaperType.CLASS_6_8_MATH_SCIENCE && full150TargetSection == 2 -> "Part 3: Class 6–8 Math & Science (80 Q)"
        selectedPaperType == PaperType.CLASS_9_10_MATH && full150TargetSection == 2 -> "Part 3: Class 9–10 Mathematics (80 Q)"
        else -> selectedPaperType.baseSetName
    }

    val combinedCheck = remember(
        commonSectionDraft.toList(),
        languageSectionDraft.toList(),
        subjectSectionDraft.toList(),
        selectedPaperType
    ) {
        when {
            selectedPaperType == PaperType.COMBINED ->
                BulkQuestionParser.combineSections(commonSectionDraft.toList(), languageSectionDraft.toList())
            selectedPaperType.isFull150Paper() ->
                BulkQuestionParser.combineFull150Sections(
                    languageSectionDraft.toList(),
                    commonSectionDraft.toList(),
                    subjectSectionDraft.toList(),
                    selectedPaperType
                )
            else -> null
        }
    }
    val validation = remember(effectiveQuestions, selectedPaperType) {
        BulkQuestionParser.validate(effectiveQuestions, selectedPaperType)
    }
    val isDuplicateTitle = remember(setTitle, existingTests, editingTest?.id) {
        TestSetNaming.isDuplicateTitle(setTitle, existingTests, excludeTestId = editingTest?.id)
    }

    fun appendQuestionsToTarget(incoming: List<ExamQuestion>, autoPartitionBulk: Boolean = true) {
        if (incoming.isEmpty()) return
        when {
            selectedPaperType == PaperType.COMBINED -> {
                if (autoPartitionBulk) {
                    // ROOT-CAUSE FIX: Auto-classify 70-Q / mixed batches into Language (30) & Common (40)
                    // without ever dumping all questions into Mathematics or one section!
                    val part = BulkQuestionParser.partitionImportedQuestions(incoming, PaperType.COMBINED)
                    languageSectionDraft.addAll(part.language)
                    commonSectionDraft.addAll(part.common)
                    if (part.subject.isNotEmpty()) {
                        commonSectionDraft.addAll(part.subject.map { it.copy(section = ExamQuestion.SECTION_COMMON) })
                    }
                } else {
                    if (combinedTargetSection == 0) {
                        commonSectionDraft.addAll(incoming.map { it.copy(section = ExamQuestion.SECTION_COMMON) })
                    } else {
                        languageSectionDraft.addAll(incoming.map { it.copy(section = ExamQuestion.SECTION_LANGUAGE) })
                    }
                }
            }
            selectedPaperType.isFull150Paper() -> {
                val hasLanguage = incoming.any { BulkQuestionParser.isLanguageSubject(it.subject) }
                val hasNonLang = incoming.any { !BulkQuestionParser.isLanguageSubject(it.subject) }
                val distinctSections = incoming.map { BulkQuestionParser.normalizePartOrSection(it.section) }
                    .filter { it.isNotBlank() }
                    .distinct()
                val existingCommonMath = commonSectionDraft.count {
                    BulkQuestionParser.normalizeSubject(it.subject, selectedPaperType) == "Mathematics"
                }
                val existingCommonSci = commonSectionDraft.count {
                    BulkQuestionParser.normalizeSubject(it.subject, selectedPaperType) == "Science"
                }
                val shouldAutoRoute = autoPartitionBulk && (
                    incoming.size > 40 ||
                        (hasLanguage && hasNonLang) ||
                        distinctSections.size > 1 ||
                        distinctSections.any { it == ExamQuestion.SECTION_LANGUAGE || it == ExamQuestion.SECTION_SUBJECT } ||
                        (distinctSections.singleOrNull() == ExamQuestion.SECTION_COMMON && incoming.any {
                            val s = BulkQuestionParser.normalizeSubject(it.subject, selectedPaperType)
                            s == "History" || s == "Geography" || s == "Current Affairs"
                        })
                    )
                if (shouldAutoRoute) {
                    val part = BulkQuestionParser.partitionImportedQuestions(
                        questions = incoming,
                        targetPaperType = selectedPaperType,
                        forceAutoSplit = false,
                        existingCommonMathCount = existingCommonMath,
                        existingCommonScienceCount = existingCommonSci
                    )
                    languageSectionDraft.addAll(part.language)
                    commonSectionDraft.addAll(part.common)
                    subjectSectionDraft.addAll(part.subject)
                } else {
                    when (full150TargetSection) {
                        0 -> languageSectionDraft.addAll(
                            incoming.map { q ->
                                q.copy(
                                    subject = BulkQuestionParser.normalizeSubject(q.subject, selectedPaperType),
                                    optionE = ExamQuestion.normalizeOptionE(q.optionE),
                                    section = ExamQuestion.SECTION_LANGUAGE
                                )
                            }
                        )
                        1 -> commonSectionDraft.addAll(
                            incoming.map { q ->
                                q.copy(
                                    subject = BulkQuestionParser.normalizeSubject(q.subject, selectedPaperType),
                                    optionE = ExamQuestion.normalizeOptionE(q.optionE),
                                    section = ExamQuestion.SECTION_COMMON
                                )
                            }
                        )
                        else -> subjectSectionDraft.addAll(
                            incoming.map { q ->
                                val norm = BulkQuestionParser.normalizeSubject(q.subject, selectedPaperType)
                                q.copy(
                                    subject = norm,
                                    optionE = ExamQuestion.normalizeOptionE(q.optionE),
                                    section = ExamQuestion.SECTION_SUBJECT
                                )
                            }
                        )
                    }
                }
            }
            else -> {
                val sec = when {
                    selectedPaperType == PaperType.LANGUAGE -> ExamQuestion.SECTION_LANGUAGE
                    selectedPaperType.isSeparateSubjectPaper() -> ExamQuestion.SECTION_SUBJECT
                    else -> ExamQuestion.SECTION_COMMON
                }
                singlePaperDraft.addAll(
                    incoming.map { q ->
                        val norm = BulkQuestionParser.normalizeSubject(q.subject, selectedPaperType)
                        q.copy(
                            subject = norm,
                            optionE = ExamQuestion.normalizeOptionE(q.optionE),
                            section = sec
                        )
                    }
                )
            }
        }
    }

    fun applySmartAutoFixSections() {
        if (effectiveQuestions.isEmpty()) return
        when {
            selectedPaperType == PaperType.COMBINED -> {
                val part = BulkQuestionParser.partitionImportedQuestions(
                    questions = effectiveQuestions,
                    targetPaperType = PaperType.COMBINED,
                    forceAutoSplit = true
                )
                languageSectionDraft.clear()
                languageSectionDraft.addAll(part.language)
                commonSectionDraft.clear()
                commonSectionDraft.addAll(part.common)
                onBannerMessage(
                    "Auto-Fixed sections: Part 1 Language (${part.language.size}/30) & Part 2 Common (${part.common.size}/40)!",
                    false
                )
            }
            selectedPaperType.isFull150Paper() -> {
                val part = BulkQuestionParser.partitionImportedQuestions(
                    questions = effectiveQuestions,
                    targetPaperType = selectedPaperType,
                    forceAutoSplit = true
                )
                languageSectionDraft.clear()
                languageSectionDraft.addAll(part.language)
                commonSectionDraft.clear()
                commonSectionDraft.addAll(part.common)
                subjectSectionDraft.clear()
                subjectSectionDraft.addAll(part.subject)
                onBannerMessage(
                    "Auto-Fixed sections: Part 1 (${part.language.size}/30), Part 2 Common (${part.common.size}/40), Part 3 Subject (${part.subject.size}/80)!",
                    false
                )
            }
            else -> {
                val targetSec = when {
                    selectedPaperType == PaperType.LANGUAGE -> ExamQuestion.SECTION_LANGUAGE
                    selectedPaperType.isSeparateSubjectPaper() -> ExamQuestion.SECTION_SUBJECT
                    else -> ExamQuestion.SECTION_COMMON
                }
                val normalized = singlePaperDraft.mapIndexed { idx, q ->
                    q.copy(
                        number = idx + 1,
                        subject = BulkQuestionParser.normalizeSubject(q.subject, selectedPaperType),
                        optionE = ExamQuestion.normalizeOptionE(q.optionE),
                        section = targetSec
                    )
                }
                singlePaperDraft.clear()
                singlePaperDraft.addAll(normalized)
                onBannerMessage("Normalized subjects & sections for ${normalized.size} questions!", false)
            }
        }
        hasUnsavedChanges = true
    }

    fun selectPaperType(type: PaperType) {
        if (editingTest != null) return
        selectedPaperType = type
        durationStr = type.defaultDurationMinutes.toString()
        setTitle = TestSetNaming.nextUniqueTitle(type, existingTests)
        userEditedTitle = false
        if (selectedCustomIconBase64.isNullOrBlank()) {
            selectedIconKey = null
        }
        if (type == PaperType.CLASS_9_10_MATH || type == PaperType.CLASS_9_10_MATH_80) {
            difficultyIndex = 2
        }
        qSubject = when {
            type == PaperType.LANGUAGE -> "English"
            type == PaperType.CLASS_6_8_SCIENCE -> "Science"
            type == PaperType.COMBINED && combinedTargetSection == 1 -> "Hindi"
            type.isFull150Paper() && full150TargetSection == 0 -> "Hindi"
            else -> "Mathematics"
        }
    }

    // Zero-permission Storage Access Framework file picker for PDF, DOC, DOCX, JSON, CSV
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importingFile = true
            scope.launch {
                val outcome = DocumentQuestionImporter.importFromUri(
                    context = context,
                    uri = uri,
                    requestedFormat = pendingFormat,
                    targetPaperType = selectedPaperType
                )
                importingFile = false
                uploadReviewOutcome = outcome
            }
        }
    }

    val groupCommonPapers = remember(existingTests) {
        existingTests.filter { it.parsedPaperType() == PaperType.COMMON_GS && it.questionCount == 40L }
    }
    val groupLanguagePapers = remember(existingTests) {
        existingTests.filter { it.parsedPaperType() == PaperType.LANGUAGE && it.questionCount == 30L }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("question_bank_list")
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Auto-Draft Recovery Banner (if an unsaved session was interrupted by rotation / reload)
        recoveredAutoDraft?.let { autoDraft ->
            if (autoDraft.rawJsonText.isNotBlank() || autoDraft.questions.isNotEmpty()) {
                item {
                    CoolingGlassCard(
                        strongFrost = true,
                        tintColor = IceCyan500,
                        borderColor = IceCyan500,
                        modifier = Modifier.testTag("recovered_auto_draft_banner")
                    ) {
                        Text(
                            text = "Unsaved Editor Draft Recovered",
                            style = MaterialTheme.typography.titleSmall,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Found auto-saved work for '${autoDraft.title}' (${autoDraft.questions.size} parsed questions${if (autoDraft.rawJsonText.isNotBlank()) " + raw JSON/text buffer" else ""}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CoolingGlassButton(
                                text = "Restore Unsaved Draft",
                                icon = Icons.Rounded.Drafts,
                                testTagName = "restore_auto_draft_btn",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    setTitle = autoDraft.title
                                    userEditedTitle = true
                                    if (editingTest == null) {
                                        selectedPaperType = autoDraft.paperType
                                    }
                                    durationStr = autoDraft.durationMinutes.toString()
                                    if (autoDraft.rawJsonText.trim().startsWith("[") || autoDraft.rawJsonText.trim().startsWith("{")) {
                                        jsonCsvInput = autoDraft.rawJsonText
                                        importModeTab = 2
                                    } else if (autoDraft.rawJsonText.isNotBlank()) {
                                        bulkInput = autoDraft.rawJsonText
                                        importModeTab = 3
                                    }
                                    if (autoDraft.questions.isNotEmpty()) {
                                        singlePaperDraft.clear()
                                        commonSectionDraft.clear()
                                        languageSectionDraft.clear()
                                        subjectSectionDraft.clear()
                                        appendQuestionsToTarget(autoDraft.questions, autoPartitionBulk = true)
                                    }
                                    hasUnsavedChanges = true
                                    recoveredAutoDraft = null
                                    onBannerMessage("Restored unsaved editor draft!", false)
                                }
                            )
                            CoolingGlassOutlinedButton(
                                text = "Discard",
                                icon = Icons.Rounded.DeleteOutline,
                                tint = glass.incorrect,
                                testTagName = "discard_auto_draft_btn",
                                onClick = {
                                    draftStore?.clearAutoDraft(autoDraftKey)
                                    recoveredAutoDraft = null
                                }
                            )
                        }
                    }
                }
            }
        }

        // Edit Mode Banner (when editing an existing test)
        if (editingTest != null) {
            item {
                CoolingGlassCard(strongFrost = true, tintColor = glass.review, borderColor = glass.review) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Editing Existing Test Set", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                            Text("Paper type is locked (${editingTest.parsedPaperType().titleEn}). Update questions, answer keys, explanations, or duration.", style = MaterialTheme.typography.labelSmall, color = glass.textSecondary)
                        }
                        CoolingGlassOutlinedButton(
                            text = "Cancel",
                            icon = Icons.Rounded.Close,
                            tint = glass.incorrect,
                            onClick = {
                                if (hasUnsavedChanges) {
                                    showUnsavedDiscardConfirm = true
                                } else {
                                    onCancelEdit()
                                }
                            }
                        )
                    }
                }
            }
        }

        // 1. Official Test Type Selector + Auto-Duration + Draft Controls
        item {
            CoolingGlassCard(strongFrost = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (editingTest != null) "Edit Test Configuration" else "TRE 4.0 Set Creation Studio",
                            style = MaterialTheme.typography.titleLarge,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Auto-timed test types • PDF / DOC / DOCX / JSON / CSV / Manual",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                    }
                    if (draftStore != null && editingTest == null) {
                        CoolingGlassOutlinedButton(
                            text = "Drafts (${savedDrafts.size})",
                            icon = Icons.Rounded.Drafts,
                            testTagName = "open_drafts_btn",
                            onClick = {
                                savedDrafts = draftStore.loadAllDrafts()
                                showDraftsSheet = true
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 5 Official TRE 4.0 Test Formats (Auto-Assigns Official Duration & Unique Set Number)
                Text(
                    text = "Select Official Test Session (Auto-Assigns Time & Unique Set #)",
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TestTypeOptionTile(
                        title = "Language",
                        detail = "30 Q • 15 Min",
                        selected = selectedPaperType == PaperType.LANGUAGE,
                        enabled = editingTest == null,
                        tag = "paper_type_language",
                        modifier = Modifier.weight(1f),
                        onClick = { selectPaperType(PaperType.LANGUAGE) }
                    )
                    TestTypeOptionTile(
                        title = "Common",
                        detail = "40 Q • 30 Min",
                        selected = selectedPaperType == PaperType.COMMON_GS,
                        enabled = editingTest == null,
                        tag = "paper_type_common",
                        modifier = Modifier.weight(1f),
                        onClick = { selectPaperType(PaperType.COMMON_GS) }
                    )
                    TestTypeOptionTile(
                        title = "Lang + Com",
                        detail = "70 Q • 45 Min",
                        selected = selectedPaperType == PaperType.COMBINED,
                        enabled = editingTest == null,
                        tag = "paper_type_combined",
                        modifier = Modifier.weight(1f),
                        onClick = { selectPaperType(PaperType.COMBINED) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TestTypeOptionTile(
                        title = "6–8 Math",
                        detail = "40 Q • 40 Min",
                        selected = selectedPaperType == PaperType.CLASS_6_8_MATH,
                        enabled = editingTest == null,
                        tag = "paper_type_6_8_math",
                        modifier = Modifier.weight(1f),
                        onClick = { selectPaperType(PaperType.CLASS_6_8_MATH) }
                    )
                    TestTypeOptionTile(
                        title = "6–8 Science",
                        detail = "40 Q • 40 Min",
                        selected = selectedPaperType == PaperType.CLASS_6_8_SCIENCE,
                        enabled = editingTest == null,
                        tag = "paper_type_6_8_science",
                        modifier = Modifier.weight(1f),
                        onClick = { selectPaperType(PaperType.CLASS_6_8_SCIENCE) }
                    )
                    TestTypeOptionTile(
                        title = "9–10 Math",
                        detail = "80 Q • 80 Min",
                        selected = selectedPaperType == PaperType.CLASS_9_10_MATH_80,
                        enabled = editingTest == null,
                        tag = "paper_type_9_10_math_80",
                        modifier = Modifier.weight(1f),
                        onClick = { selectPaperType(PaperType.CLASS_9_10_MATH_80) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TestTypeOptionTile(
                        title = "6–8 Full Paper",
                        detail = "150 Q • 2 Hr",
                        selected = selectedPaperType == PaperType.CLASS_6_8_MATH_SCIENCE,
                        enabled = editingTest == null,
                        tag = "paper_type_6_8",
                        modifier = Modifier.weight(1f),
                        onClick = { selectPaperType(PaperType.CLASS_6_8_MATH_SCIENCE) }
                    )
                    TestTypeOptionTile(
                        title = "9–10 Full Paper",
                        detail = "150 Q • 2h 15m",
                        selected = selectedPaperType == PaperType.CLASS_9_10_MATH,
                        enabled = editingTest == null,
                        tag = "paper_type_9_10",
                        modifier = Modifier.weight(1f),
                        onClick = { selectPaperType(PaperType.CLASS_9_10_MATH) }
                    )
                }

                Spacer(Modifier.height(8.dp))
                // Set Icon / Logo Selector Row (Auto-inherits 7-Category Admin Default unless overridden)
                val categoryIcons = LocalCategoryIcons.current
                val currentCat = RankCategory.fromRaw(selectedPaperType.name)
                val catCfg = currentCat?.let { categoryIcons[it.name] }
                val usingCategoryDefault = selectedCustomIconBase64.isNullOrBlank() && selectedIconKey.isNullOrBlank() &&
                    catCfg != null && (!catCfg.customIconBase64.isNullOrBlank() || !catCfg.iconKey.isNullOrBlank())

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(glass.glassSurfaceStrong)
                        .border(1.dp, glass.glassBorder, RoundedCornerShape(14.dp))
                        .clickable { showIconPickerDialog = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SetIconAvatar(
                        iconKey = selectedIconKey,
                        customIconBase64 = selectedCustomIconBase64,
                        paperType = selectedPaperType,
                        size = 42.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        AutoResizeSingleLineText(
                            text = "Set Icon / Logo",
                            style = MaterialTheme.typography.labelLarge,
                            maxFontSize = 13.sp,
                            minFontSize = 10.sp,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        AutoResizeSingleLineText(
                            text = when {
                                !selectedCustomIconBase64.isNullOrBlank() -> "Custom Per-Set Upload"
                                !selectedIconKey.isNullOrBlank() -> SetIconOption.resolve(selectedIconKey, selectedPaperType).label
                                usingCategoryDefault -> "Auto: ${currentCat?.displayName} Default ✓"
                                else -> "Auto: ${SetIconOption.defaultForPaperType(selectedPaperType).label}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            maxFontSize = 11.sp,
                            minFontSize = 8.5.sp,
                            color = if (usingCategoryDefault) glass.correct else glass.textSecondary
                        )
                    }
                    CoolingGlassOutlinedButton(
                        text = "Change",
                        testTagName = "change_set_icon_btn",
                        modifier = Modifier.width(92.dp),
                        onClick = { showIconPickerDialog = true }
                    )
                }

                Spacer(Modifier.height(8.dp))
                // Set Banner (Animated GIF / Transparent PNG / JPG) Uploader with Live Preview
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(glass.glassSurfaceStrong)
                        .border(1.dp, glass.glassBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AutoResizeSingleLineText(
                                text = "Set Banner (Live GIF / PNG / JPG)",
                                style = MaterialTheme.typography.labelLarge,
                                maxFontSize = 13.sp,
                                minFontSize = 10.sp,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Upload an animated GIF or PNG so users see live visuals on this test card.",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textSecondary,
                                maxLines = 2
                            )
                        }
                        if (ImageCodec.isGifBase64(selectedBannerImageBase64)) {
                            CoolingGlassBadge(text = "LIVE GIF", color = glass.correct)
                        } else if (ImageCodec.isPngBase64(selectedBannerImageBase64)) {
                            CoolingGlassBadge(text = "PNG", color = IceCyan500)
                        }
                    }

                    if (!selectedBannerImageBase64.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 5.8f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, glass.accentCyan.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        ) {
                            AnimatedBase64Image(
                                base64 = selectedBannerImageBase64,
                                contentDescription = "Set Banner Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoolingGlassOutlinedButton(
                            text = if (selectedBannerImageBase64.isNullOrBlank()) "Upload GIF / PNG Banner" else "Change Banner",
                            icon = Icons.Rounded.AddPhotoAlternate,
                            testTagName = "upload_set_banner_btn",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                setBannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                        if (!selectedBannerImageBase64.isNullOrBlank()) {
                            CoolingGlassOutlinedButton(
                                text = "Remove",
                                icon = Icons.Rounded.DeleteOutline,
                                tint = glass.incorrect,
                                testTagName = "remove_set_banner_btn",
                                modifier = Modifier.width(96.dp),
                                onClick = { selectedBannerImageBase64 = null }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                // Official Syllabus Blueprint Summary Strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(glass.accentPrimary.copy(alpha = 0.08f))
                        .border(0.8.dp, glass.accentCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = when (selectedPaperType) {
                            PaperType.LANGUAGE -> "Blueprint: 30 Q (15 Min) • Hindi (22) + English (8)"
                            PaperType.COMMON_GS -> "Blueprint: 40 Q (30 Min) • Mathematics (8) + Science (8) + History (8) + Geography (8) + Current Affairs (8)"
                            PaperType.COMBINED -> "Blueprint: 70 Q (45 Min) • Language (30: 22 Hindi + 8 English) + Common (40: 8 each Math/Sci/Hist/Geo/CA)"
                            PaperType.CLASS_6_8_MATH -> "Blueprint: 40 Q (40 Min) • Class 6–8 Mathematics (40 Q)"
                            PaperType.CLASS_6_8_SCIENCE -> "Blueprint: 40 Q (40 Min) • Class 6–8 Science (40 Q)"
                            PaperType.CLASS_9_10_MATH_80 -> "Blueprint: 80 Q (80 Min) • Class 9–10 Mathematics (80 Q)"
                            PaperType.CLASS_6_8_MATH_SCIENCE -> "Blueprint: 150 Q (120m) • 30 Lang + 40 Common + 40 Math + 40 Science"
                            PaperType.CLASS_9_10_MATH -> "Blueprint: 150 Q (135m) • 30 Lang + 40 Common + 80 Mathematics"
                            PaperType.PRACTICE -> "Custom Practice Set • Flexible question count & duration"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textPrimary
                    )
                }

                if (editingTest == null) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Marking: +1 Correct • -1/3 Wrong • 0 Opt E (Not Attempted) • -1/3 Blank",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textMuted,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (selectedPaperType == PaperType.PRACTICE) "Practice Active" else "Custom Practice?",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.accentPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectPaperType(
                                        if (selectedPaperType == PaperType.PRACTICE) PaperType.COMMON_GS else PaperType.PRACTICE
                                    )
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                CoolingGlassTextField(
                    value = setTitle,
                    onValueChange = {
                        setTitle = it
                        userEditedTitle = true
                    },
                    label = "Unique Set Name (Auto Set 1, Set 2, Set 3...) *",
                    placeholder = TestSetNaming.nextUniqueTitle(selectedPaperType, existingTests),
                    testTagName = "set_title_input"
                )
                if (isDuplicateTitle) {
                    Spacer(Modifier.height(6.dp))
                    CoolingGlassStatusBanner(
                        message = "Duplicate set name! Every set must have a unique name (e.g. ${TestSetNaming.nextUniqueTitle(selectedPaperType, existingTests)}).",
                        isError = true
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoolingGlassTabRow(
                        tabs = listOf("Easy", "Moderate", "Hard"),
                        selectedIndex = difficultyIndex,
                        onSelect = { difficultyIndex = it },
                        modifier = Modifier.weight(1.4f)
                    )
                    CoolingGlassTextField(
                        value = durationStr,
                        onValueChange = { durationStr = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = "Min",
                        testTagName = "set_duration_input",
                        modifier = Modifier.weight(0.6f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unattempted Penalty (-1/3 on blank)",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textPrimary
                    )
                    Switch(checked = unattemptedPenalty, onCheckedChange = { unattemptedPenalty = it })
                }
                if (isAdmin) {
                    Spacer(Modifier.height(6.dp))
                    CoolingGlassTabRow(
                        tabs = listOf("Immediate Answer Key", "Scheduled Release"),
                        selectedIndex = releaseModeIndex,
                        onSelect = { releaseModeIndex = it }
                    )
                    if (editingTest == null) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (publishImmediately) "Approve & Show to Members Now" else "Save to Daily Approval Queue (Recommended)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (publishImmediately) "Invited members will see this set immediately."
                                    else "Hidden from invited members until you approve 1–2 sets per day in Admin Console.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textMuted
                                )
                            }
                            Switch(checked = publishImmediately, onCheckedChange = { publishImmediately = it })
                        }
                    }
                }
            }
        }

        // 2. Multi-Section Manager (Combined 70-Q OR Full 150-Q Papers — Never Mixed!)
        if (selectedPaperType == PaperType.COMBINED) {
            item {
                CoolingGlassCard(strongFrost = true, tintColor = SoftLavender500, borderColor = SoftLavender500) {
                    Text(
                        text = "Language + Common Section Builder (40 Common + 30 Language)",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sections and answer keys stay strictly separate: Q1–Q40 = Common Paper, Q41–Q70 = Language Paper.",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoolingGlassBadge(
                            text = "Sec A Common: ${commonSectionDraft.size}/40",
                            color = if (commonSectionDraft.size == 40) glass.correct else glass.accentPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        CoolingGlassBadge(
                            text = "Sec B Language: ${languageSectionDraft.size}/30",
                            color = if (languageSectionDraft.size == 30) glass.correct else SoftLavender500,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    CoolingGlassButton(
                        text = "Auto-Combine 40 Common + 30 Language Sets",
                        icon = Icons.Rounded.Merge,
                        testTagName = "auto_combine_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showCombineDialog = true }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Active Import / Manual Entry Target:",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    CoolingGlassTabRow(
                        tabs = listOf("Section A: Common (${commonSectionDraft.size}/40)", "Section B: Language (${languageSectionDraft.size}/30)"),
                        selectedIndex = combinedTargetSection,
                        onSelect = {
                            combinedTargetSection = it
                            qSubject = if (it == 1) "Hindi" else "Mathematics"
                        }
                    )
                }
            }
        } else if (selectedPaperType.isFull150Paper()) {
            item {
                val part3Name = if (selectedPaperType == PaperType.CLASS_6_8_MATH_SCIENCE) {
                    "Math(40)+Sci(40)"
                } else {
                    "9–10 Math (80)"
                }
                CoolingGlassCard(strongFrost = true, tintColor = IceCyan500, borderColor = IceCyan500) {
                    Text(
                        text = "${selectedPaperType.baseSetName} (150 Q • ${selectedPaperType.defaultDurationMinutes} Min)",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "3 Strictly Isolated Parts: Part 1 Language (30 Q) + Part 2 Common Paper (40 Q) + Part 3 Subject (80 Q).",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CoolingGlassBadge(
                            text = "P1 Lang: ${languageSectionDraft.size}/30",
                            color = if (languageSectionDraft.size == 30) glass.correct else SoftLavender500,
                            modifier = Modifier.weight(1f)
                        )
                        CoolingGlassBadge(
                            text = "P2 Com: ${commonSectionDraft.size}/40",
                            color = if (commonSectionDraft.size == 40) glass.correct else glass.accentPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        CoolingGlassBadge(
                            text = "P3 Sub: ${subjectSectionDraft.size}/80",
                            color = if (subjectSectionDraft.size == 80) glass.correct else IceCyan500,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    CoolingGlassButton(
                        text = "Auto-Fill Part 1 (30 Lang) + Part 2 (40 Common)",
                        icon = Icons.Rounded.Merge,
                        testTagName = "auto_combine_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showCombineDialog = true }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Active Import / Manual Entry Target:",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    CoolingGlassTabRow(
                        tabs = listOf(
                            "P1 Lang (${languageSectionDraft.size}/30)",
                            "P2 Com (${commonSectionDraft.size}/40)",
                            "P3 $part3Name (${subjectSectionDraft.size}/80)"
                        ),
                        selectedIndex = full150TargetSection,
                        onSelect = {
                            full150TargetSection = it
                            qSubject = when (it) {
                                0 -> "Hindi"
                                else -> "Mathematics"
                            }
                        }
                    )
                }
            }
        }

        // 3. Question Creation & Import Mode Switcher
        item {
            CoolingGlassTabRow(
                tabs = listOf("Upload File", "Manual", "JSON / CSV", "Bulk / Bank"),
                selectedIndex = importModeTab,
                onSelect = { importModeTab = it }
            )
        }

        // 4. Mode-Specific Builder / Importer Card
        item {
            when (importModeTab) {
                0 -> CoolingGlassCard(strongFrost = true) {
                    Text("Upload Question File (PDF, DOC, DOCX, JSON, CSV)", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                    Text("Target: $activeTargetLabel • Imports securely via Android Document Picker (zero storage permission).", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                    Spacer(Modifier.height(12.dp))

                    if (importingFile) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = glass.accentCyan)
                            Spacer(Modifier.width(10.dp))
                            Text("Extracting & parsing questions...", style = MaterialTheme.typography.bodyMedium, color = glass.textPrimary)
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UploadFormatTile(
                                title = "PDF Upload",
                                ext = ".pdf",
                                icon = Icons.Rounded.PictureAsPdf,
                                accent = Color(0xFFDC2626),
                                tag = "upload_pdf_btn",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    pendingFormat = ImportFileFormat.PDF
                                    documentPickerLauncher.launch(ImportFileFormat.PDF.mimeTypes)
                                }
                            )
                            UploadFormatTile(
                                title = "DOC Upload",
                                ext = ".doc",
                                icon = Icons.Rounded.Description,
                                accent = GlacierBlue600,
                                tag = "upload_doc_btn",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    pendingFormat = ImportFileFormat.DOC
                                    documentPickerLauncher.launch(ImportFileFormat.DOC.mimeTypes)
                                }
                            )
                            UploadFormatTile(
                                title = "DOCX Upload",
                                ext = ".docx",
                                icon = Icons.Rounded.UploadFile,
                                accent = IceCyan500,
                                tag = "upload_docx_btn",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    pendingFormat = ImportFileFormat.DOCX
                                    documentPickerLauncher.launch(ImportFileFormat.DOCX.mimeTypes)
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UploadFormatTile(
                                title = "JSON Import",
                                ext = ".json file",
                                icon = Icons.Rounded.DataObject,
                                accent = SoftLavender500,
                                tag = "upload_json_btn",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    pendingFormat = ImportFileFormat.JSON
                                    documentPickerLauncher.launch(ImportFileFormat.JSON.mimeTypes)
                                }
                            )
                            UploadFormatTile(
                                title = "CSV Import",
                                ext = ".csv file",
                                icon = Icons.Rounded.TableChart,
                                accent = glass.correct,
                                tag = "upload_csv_btn",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    pendingFormat = ImportFileFormat.CSV
                                    documentPickerLauncher.launch(ImportFileFormat.CSV.mimeTypes)
                                }
                            )
                        }
                    }
                }

                1 -> CoolingGlassCard(strongFrost = true) {
                    Text("Manual Question Entry ($activeTargetLabel)", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val subjectChips = when {
                        selectedPaperType == PaperType.CLASS_6_8_MATH_SCIENCE && full150TargetSection == 2 ->
                            BulkQuestionParser.CLASS_6_8_SUBJECTS
                        selectedPaperType == PaperType.CLASS_9_10_MATH && full150TargetSection == 2 ->
                            BulkQuestionParser.CLASS_9_10_SUBJECTS
                        selectedPaperType == PaperType.CLASS_6_8_MATH -> listOf("Mathematics")
                        selectedPaperType == PaperType.CLASS_6_8_SCIENCE -> listOf("Science")
                        selectedPaperType == PaperType.CLASS_9_10_MATH_80 -> listOf("Mathematics")
                        activeImportPaperType == PaperType.LANGUAGE -> BulkQuestionParser.LANG_SUBJECTS
                        else -> BulkQuestionParser.GS_SUBJECTS
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjectChips.forEach { subj ->
                            val picked = qSubject.equals(subj, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (picked) GlacierBlue600 else glass.glassSurfaceStrong)
                                    .border(1.dp, if (picked) IceCyan500 else glass.glassBorder, RoundedCornerShape(12.dp))
                                    .clickable { qSubject = subj }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text(subj.substringBefore(" /"), style = MaterialTheme.typography.labelSmall, color = if (picked) CrystalWhite else glass.textPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoolingGlassTextField(value = qTopic, onValueChange = { qTopic = it }, label = "Topic", modifier = Modifier.weight(1f))
                        CoolingGlassTextField(value = qSource, onValueChange = { qSource = it }, label = "Source (NCERT/SCERT/PIB)", modifier = Modifier.weight(1f))
                    }
                    if (activeImportPaperType == PaperType.LANGUAGE) {
                        Spacer(Modifier.height(8.dp))
                        CoolingGlassTextField(
                            value = qPassage,
                            onValueChange = { qPassage = it },
                            label = appText("Passage (for Passage Questions)", "Passage / गद्यांश (for Passage Questions)"),
                            singleLine = false,
                            minLines = 2
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    CoolingGlassTextField(value = qText, onValueChange = { qText = it }, label = "Question Text (Hindi / English) *", singleLine = false, minLines = 2, testTagName = "manual_q_text")
                    Spacer(Modifier.height(6.dp))
                    CoolingGlassTextField(value = qOptA, onValueChange = { qOptA = it }, label = "Option A *", singleLine = false, maxLines = 4)
                    Spacer(Modifier.height(6.dp))
                    CoolingGlassTextField(value = qOptB, onValueChange = { qOptB = it }, label = "Option B *", singleLine = false, maxLines = 4)
                    Spacer(Modifier.height(6.dp))
                    CoolingGlassTextField(value = qOptC, onValueChange = { qOptC = it }, label = "Option C *", singleLine = false, maxLines = 4)
                    Spacer(Modifier.height(6.dp))
                    CoolingGlassTextField(value = qOptD, onValueChange = { qOptD = it }, label = "Option D *", singleLine = false, maxLines = 4)
                    Spacer(Modifier.height(8.dp))
                    Text("Select Correct Answer Key (Option E = Not Attempted, 0 marks)", style = MaterialTheme.typography.labelSmall, color = glass.textSecondary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    AnswerKeySelectorRow(selectedKey = qAnswer, onSelect = { qAnswer = it })
                    Spacer(Modifier.height(8.dp))
                    CoolingGlassTextField(value = qExp, onValueChange = { qExp = it }, label = "Detailed Explanation", singleLine = false, minLines = 2)
                    Spacer(Modifier.height(10.dp))
                    CoolingGlassButton(
                        text = "Add Question to $activeTargetLabel",
                        icon = Icons.Rounded.Add,
                        enabled = qText.isNotBlank() && qOptA.isNotBlank() && qOptB.isNotBlank() && qOptC.isNotBlank() && qOptD.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        testTagName = "manual_add_q_btn",
                        onClick = {
                            val normalizedSubj = BulkQuestionParser.normalizeSubject(qSubject, activeImportPaperType)
                            val targetSec = when {
                                selectedPaperType.isFull150Paper() && full150TargetSection == 2 -> ExamQuestion.SECTION_SUBJECT
                                selectedPaperType.isSeparateSubjectPaper() -> ExamQuestion.SECTION_SUBJECT
                                activeImportPaperType == PaperType.LANGUAGE -> ExamQuestion.SECTION_LANGUAGE
                                else -> ExamQuestion.SECTION_COMMON
                            }
                            val newQ = ExamQuestion(
                                number = effectiveQuestions.size + 1,
                                subject = normalizedSubj,
                                topic = qTopic.trim(),
                                source = qSource.trim(),
                                passage = qPassage.trim(),
                                text = qText.trim(),
                                optionA = qOptA.trim(),
                                optionB = qOptB.trim(),
                                optionC = qOptC.trim(),
                                optionD = qOptD.trim(),
                                optionE = ExamQuestion.DEFAULT_OPTION_E,
                                correctOption = qAnswer.uppercase().ifBlank { "A" },
                                explanation = qExp.trim(),
                                section = targetSec
                            )
                            appendQuestionsToTarget(listOf(newQ), autoPartitionBulk = false)
                            qText = ""; qOptA = ""; qOptB = ""; qOptC = ""; qOptD = ""; qExp = ""
                            onBannerMessage("Question added to $activeTargetLabel!", false)
                        }
                    )
                }

                2 -> CoolingGlassCard(strongFrost = true) {
                    Text("JSON & CSV Import ($activeTargetLabel)", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoolingGlassOutlinedButton(
                            text = "JSON Template",
                            icon = Icons.Rounded.ContentCopy,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                clipboard.setText(AnnotatedString(BulkQuestionParser.JSON_TEMPLATE))
                                onBannerMessage("JSON template copied!", false)
                            }
                        )
                        CoolingGlassOutlinedButton(
                            text = "CSV Template",
                            icon = Icons.Rounded.ContentCopy,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                clipboard.setText(AnnotatedString(BulkQuestionParser.CSV_TEMPLATE))
                                onBannerMessage("CSV template copied!", false)
                            }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    CoolingGlassTextField(
                        value = jsonCsvInput,
                        onValueChange = {
                            jsonCsvInput = it
                            hasUnsavedChanges = true
                            strictJsonValidationError = null
                        },
                        label = "Paste JSON Array / Object or CSV Rows",
                        testTagName = "admin_json_csv_input",
                        singleLine = false,
                        minLines = 5,
                        maxLines = 10
                    )
                    strictJsonValidationError?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("strict_json_validation_error_card")
                                .clip(RoundedCornerShape(12.dp))
                                .background(glass.incorrectBg)
                                .border(1.dp, glass.incorrect, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Pre-Save JSON Validation Failed — Database Write Blocked",
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.incorrect,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = buildString {
                                    if (err.errorLineNumber != null) append("Line ${err.errorLineNumber} • ")
                                    if (!err.errorFieldName.isNullOrBlank()) append("Field '${err.errorFieldName}': ")
                                    append(err.errorMessage ?: "Invalid JSON structure")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    CoolingGlassButton(
                        text = "Validate, Parse & Import JSON / CSV",
                        testTagName = "parse_paste_json_csv_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val trimmed = jsonCsvInput.trim()
                            if (trimmed.isEmpty()) {
                                val emptyErr = TestDraftStore.validateRawJsonStrict(trimmed)
                                strictJsonValidationError = emptyErr
                                onBannerMessage(emptyErr.errorMessage ?: "JSON cannot be empty", true)
                            } else if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                                val strictCheck = TestDraftStore.validateRawJsonStrict(trimmed)
                                if (!strictCheck.isValid) {
                                    strictJsonValidationError = strictCheck
                                    onBannerMessage(strictCheck.errorMessage ?: "Invalid JSON schema", true)
                                } else {
                                    strictJsonValidationError = null
                                    BulkQuestionParser.parseJson(trimmed, selectedPaperType)
                                        .onSuccess { list ->
                                            appendQuestionsToTarget(list, autoPartitionBulk = true)
                                            jsonCsvInput = ""
                                            hasUnsavedChanges = true
                                            onBannerMessage("Validated & imported ${list.size} JSON questions!", false)
                                        }
                                        .onFailure {
                                            strictJsonValidationError = JsonValidationResult(
                                                isValid = false,
                                                errorMessage = it.localizedMessage ?: "Invalid JSON"
                                            )
                                            onBannerMessage(it.localizedMessage ?: "Invalid JSON", true)
                                        }
                                }
                            } else {
                                val list = BulkQuestionParser.parseCsv(trimmed, selectedPaperType)
                                if (list.isEmpty()) onBannerMessage("No valid CSV rows found.", true)
                                else {
                                    strictJsonValidationError = null
                                    appendQuestionsToTarget(list, autoPartitionBulk = true)
                                    jsonCsvInput = ""
                                    hasUnsavedChanges = true
                                    onBannerMessage("Auto-classified & imported ${list.size} CSV questions!", false)
                                }
                            }
                        }
                    )
                }

                else -> CoolingGlassCard(strongFrost = true) {
                    Text("Bulk Text Paste & Sample Blueprints", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    CoolingGlassTextField(
                        value = bulkInput,
                        onValueChange = { bulkInput = it },
                        label = "Paste Numbered MCQs for $activeTargetLabel",
                        placeholder = appText(
                            en = "1. Question...\nA) ...\nB) ...\nC) ...\nD) ...\nE) Not Attempted\nAnswer: B\nPassage: (optional)\nExplanation: ...",
                            defaultText = "1. Question...\nA) ...\nB) ...\nC) ...\nD) ...\nE) Not Attempted / नॉट अटेम्प्ट\nAnswer: B\nPassage: (optional)\nExplanation: ..."
                        ),
                        singleLine = false,
                        minLines = 4,
                        maxLines = 8
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoolingGlassOutlinedButton(
                            text = "Copy Format",
                            icon = Icons.Rounded.ContentCopy,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                clipboard.setText(AnnotatedString(BulkQuestionParser.BULK_TEXT_TEMPLATE))
                                onBannerMessage("Bulk MCQ format copied!", false)
                            }
                        )
                        CoolingGlassButton(
                            text = "Parse Text",
                            icon = Icons.Rounded.AutoAwesome,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val parsed = BulkQuestionParser.parseBulkText(bulkInput, selectedPaperType)
                                if (parsed.isEmpty()) {
                                    onBannerMessage("Could not parse questions. Check template format.", true)
                                } else {
                                    appendQuestionsToTarget(parsed, autoPartitionBulk = true)
                                    bulkInput = ""
                                    onBannerMessage("Auto-classified & imported ${parsed.size} questions!", false)
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Load Reference Blueprints into Draft", style = MaterialTheme.typography.labelLarge, color = glass.accentPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    CoolingGlassOutlinedButton(
                        text = "Load 30-Q Language Blueprint (15 Min)",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (editingTest == null) selectPaperType(PaperType.LANGUAGE)
                            singlePaperDraft.clear()
                            singlePaperDraft.addAll(OfficialStarterBank.language30Questions())
                            onBannerMessage("Loaded 30 Language questions (8 En + 22 Hi with passages) into draft!", false)
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    CoolingGlassOutlinedButton(
                        text = "Load 40-Q Common Paper Blueprint (30 Min)",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (editingTest == null) selectPaperType(PaperType.COMMON_GS)
                            singlePaperDraft.clear()
                            singlePaperDraft.addAll(OfficialStarterBank.commonGs40Questions())
                            onBannerMessage("Loaded 40 Common Paper questions (30 min) into draft!", false)
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    CoolingGlassOutlinedButton(
                        text = "Load 70-Q Language + Common Blueprint (45 Min)",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (editingTest == null) selectPaperType(PaperType.COMBINED)
                            commonSectionDraft.clear()
                            commonSectionDraft.addAll(OfficialStarterBank.commonGs40Questions())
                            languageSectionDraft.clear()
                            languageSectionDraft.addAll(OfficialStarterBank.language30Questions())
                            onBannerMessage("Loaded 70 Combined questions: 40 Common + 30 Language (45 min)!", false)
                        }
                    )
                }
            }
        }

        // 5. Live Validation Dashboard, Bulk Subject Correction, Save Draft & Publish / Update Card
        item {
            val allErrors = (validation.errors + (combinedCheck?.errors.orEmpty())).distinct()
            val isValidReady = validation.valid && (combinedCheck?.valid != false) && allErrors.isEmpty() && effectiveQuestions.isNotEmpty()
            CoolingGlassCard(
                strongFrost = true,
                borderColor = if (isValidReady) glass.correct else glass.review,
                modifier = Modifier.testTag("live_validation_dashboard_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Live Validation Dashboard", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                    CoolingGlassBadge(
                        text = if (validation.expectedTotal > 0) "${validation.totalQuestions} / ${validation.expectedTotal} Qs"
                        else "${effectiveQuestions.size} Qs • ${durationStr.ifBlank { selectedPaperType.defaultDurationMinutes.toString() }}m",
                        color = if (isValidReady) glass.correct else glass.review
                    )
                }

                // Live Section Breakdown Strip
                if (selectedPaperType == PaperType.COMBINED || selectedPaperType == PaperType.LANGUAGE || selectedPaperType.isFull150Paper()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (selectedPaperType.isFull150Paper()) {
                            CoolingGlassBadge("Part 1 Lang: ${validation.part1LanguageCount}/30", if (validation.part1LanguageCount == 30) glass.correct else SoftLavender500)
                        }
                        CoolingGlassBadge("Hindi: ${validation.hindiCount}/22", if (validation.hindiCount == 22) glass.correct else SoftLavender500)
                        CoolingGlassBadge("English: ${validation.englishCount}/8", if (validation.englishCount == 8) glass.correct else SoftLavender500)
                        if (selectedPaperType != PaperType.LANGUAGE) {
                            CoolingGlassBadge("Part 2 Common: ${validation.commonCount}/40", if (validation.commonCount == 40) glass.correct else glass.accentPrimary)
                        }
                        if (selectedPaperType.isFull150Paper()) {
                            CoolingGlassBadge("Part 3 Subject: ${validation.subjectPaperCount}/80", if (validation.subjectPaperCount == 80) glass.correct else IceCyan500)
                        }
                    }
                }

                // Live Per-Part & Per-Subject Blueprint Counters
                if (selectedPaperType.isFull150Paper()) {
                    Spacer(Modifier.height(8.dp))
                    val p1Ok = validation.part1LanguageCount == 30 && validation.hindiCount == 22 && validation.englishCount == 8
                    Text(
                        text = "Part 1 Language (${validation.part1LanguageCount}/30): Hindi ${validation.hindiCount}/22 • English ${validation.englishCount}/8",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (p1Ok) glass.correct else glass.textSecondary,
                        fontWeight = if (p1Ok) FontWeight.SemiBold else FontWeight.Medium
                    )
                    val p2Math = validation.part2CommonSubjectCounts["Mathematics"] ?: 0
                    val p2Sci = validation.part2CommonSubjectCounts["Science"] ?: 0
                    val p2Hist = validation.part2CommonSubjectCounts["History"] ?: 0
                    val p2Geo = validation.part2CommonSubjectCounts["Geography"] ?: 0
                    val p2Ca = validation.part2CommonSubjectCounts["Current Affairs"] ?: 0
                    val p2Ok = validation.commonCount == 40 && p2Math == 8 && p2Sci == 8 && p2Hist == 8 && p2Geo == 8 && p2Ca == 8
                    Text(
                        text = "Part 2 Common (${validation.commonCount}/40): Math $p2Math/8 • Sci $p2Sci/8 • Hist $p2Hist/8 • Geo $p2Geo/8 • CA $p2Ca/8",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (p2Ok) glass.correct else glass.textSecondary,
                        fontWeight = if (p2Ok) FontWeight.SemiBold else FontWeight.Medium
                    )
                    val p3Math = validation.part3SubjectCounts["Mathematics"] ?: 0
                    val p3Sci = validation.part3SubjectCounts["Science"] ?: 0
                    if (selectedPaperType == PaperType.CLASS_6_8_MATH_SCIENCE) {
                        val p3Ok = validation.subjectPaperCount == 80 && p3Math == 40 && p3Sci == 40
                        Text(
                            text = "Part 3 Subject (${validation.subjectPaperCount}/80): Mathematics $p3Math/40 • Science $p3Sci/40",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (p3Ok) glass.correct else glass.textSecondary,
                            fontWeight = if (p3Ok) FontWeight.SemiBold else FontWeight.Medium
                        )
                    } else {
                        val p3Ok = validation.subjectPaperCount == 80 && p3Math == 80
                        Text(
                            text = "Part 3 Subject (${validation.subjectPaperCount}/80): Mathematics $p3Math/80",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (p3Ok) glass.correct else glass.textSecondary,
                            fontWeight = if (p3Ok) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                    if (validation.unclassifiedQuestions > 0) {
                        Text(
                            text = "• Unclassified: ${validation.unclassifiedQuestions}",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.incorrect,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (validation.expectedSubjectCounts.isNotEmpty() || validation.subjectCounts.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    val allKeys = (validation.expectedSubjectCounts.keys + validation.subjectCounts.keys).distinct()
                    allKeys.forEach { subj ->
                        val actual = validation.subjectCounts[subj] ?: 0
                        val expected = validation.expectedSubjectCounts[subj]
                        val matched = expected == null || actual == expected
                        val lineColor = when {
                            subj == BulkQuestionParser.UNCLASSIFIED_SUBJECT && actual > 0 -> glass.incorrect
                            matched -> glass.correct
                            else -> glass.textSecondary
                        }
                        Text(
                            text = if (expected != null) "• $subj: $actual / $expected" else "• $subj: $actual",
                            style = MaterialTheme.typography.bodySmall,
                            color = lineColor,
                            fontWeight = if (!matched || subj == BulkQuestionParser.UNCLASSIFIED_SUBJECT) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                // Live Diagnostic Issue Counters
                if (effectiveQuestions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (validation.missingQuestions > 0) {
                            CoolingGlassBadge("Missing: ${validation.missingQuestions}", glass.review)
                        }
                        if (validation.unclassifiedQuestions > 0) {
                            CoolingGlassBadge("Unclassified: ${validation.unclassifiedQuestions}", glass.incorrect)
                        }
                        if (validation.duplicateQuestions > 0) {
                            CoolingGlassBadge("Duplicates: ${validation.duplicateQuestions}", glass.incorrect)
                        }
                        if (validation.missingCorrectAnswers > 0) {
                            CoolingGlassBadge("Missing Keys: ${validation.missingCorrectAnswers}", glass.incorrect)
                        }
                        if (validation.invalidOptions > 0) {
                            CoolingGlassBadge("Invalid Opts: ${validation.invalidOptions}", glass.incorrect)
                        }
                    }
                }

                if (allErrors.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    allErrors.take(5).forEach { err ->
                        Text("✕ $err", style = MaterialTheme.typography.labelSmall, color = glass.incorrect)
                    }
                } else if (effectiveQuestions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    CoolingGlassStatusBanner(message = "All BPSSC TRE 4.0 blueprint rules & answer keys verified! Ready to publish.", isError = false)
                }

                if (effectiveQuestions.isNotEmpty() && (selectedPaperType.isFull150Paper() || selectedPaperType == PaperType.COMBINED)) {
                    Spacer(Modifier.height(10.dp))
                    CoolingGlassOutlinedButton(
                        text = "Fix Section / Auto-Fix",
                        icon = Icons.Rounded.AutoAwesome,
                        tint = if (!isValidReady) glass.accentPrimary else glass.correct,
                        testTagName = "auto_fix_sections_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { applySmartAutoFixSections() }
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (effectiveQuestions.isNotEmpty()) {
                        CoolingGlassOutlinedButton(
                            text = "Preview",
                            icon = Icons.Rounded.Visibility,
                            testTagName = "preview_set_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { previewIndex = 0 }
                        )
                        CoolingGlassOutlinedButton(
                            text = "Fix Subjects",
                            icon = Icons.Rounded.AutoAwesome,
                            tint = if (validation.unclassifiedQuestions > 0) glass.incorrect else SoftLavender500,
                            testTagName = "open_bulk_subject_fix_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { showBulkSubjectDialog = true }
                        )
                    }
                    if (draftStore != null && editingTest == null) {
                        CoolingGlassOutlinedButton(
                            text = "Draft",
                            icon = Icons.Rounded.Save,
                            testTagName = "save_draft_btn",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val draft = TestSetDraft(
                                    id = activeDraftId,
                                    title = setTitle.trim(),
                                    paperType = selectedPaperType,
                                    difficulty = difficulty,
                                    durationMinutes = durationStr.toIntOrNull()?.coerceIn(5, 300) ?: selectedPaperType.defaultDurationMinutes,
                                    unattemptedPenalty = unattemptedPenalty,
                                    answerReleaseMode = answerReleaseMode,
                                    questions = singlePaperDraft.toList(),
                                    commonSectionQuestions = commonSectionDraft.toList(),
                                    languageSectionQuestions = languageSectionDraft.toList(),
                                    subjectSectionQuestions = subjectSectionDraft.toList()
                                )
                                draftStore.saveDraft(draft)
                                savedDrafts = draftStore.loadAllDrafts()
                                onBannerMessage("Draft saved locally (${effectiveQuestions.size} questions)!", false)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                CoolingGlassButton(
                    text = when {
                        editingTest != null -> "Save Test Changes"
                        publishImmediately -> "Approve & Publish Live Now"
                        else -> "Save Set to Approval Queue"
                    },
                    icon = if (publishImmediately || editingTest != null) Icons.Rounded.Publish else Icons.Rounded.CheckCircle,
                    loading = busy,
                    enabled = isValidReady && setTitle.trim().length >= 3 && !isDuplicateTitle && !busy,
                    modifier = Modifier.fillMaxWidth(),
                    testTagName = "publish_question_set_btn",
                    onClick = {
                        // Block save if unparsed invalid JSON is still sitting in the JSON editor box
                        if (jsonCsvInput.isNotBlank()) {
                            val check = TestDraftStore.validateRawJsonStrict(jsonCsvInput)
                            if (!check.isValid) {
                                strictJsonValidationError = check
                                onBannerMessage(check.errorMessage ?: "Please fix or clear unparsed JSON before saving.", true)
                                return@CoolingGlassButton
                            }
                        }
                        val finalDuration = durationStr.toIntOrNull()?.coerceIn(5, 300)
                            ?: selectedPaperType.defaultDurationMinutes
                        if (editingTest != null) {
                            onUpdateTestSet(
                                editingTest.id,
                                setTitle.trim(),
                                difficulty,
                                finalDuration,
                                effectiveQuestions,
                                unattemptedPenalty,
                                answerReleaseMode,
                                selectedIconKey,
                                selectedCustomIconBase64,
                                selectedBannerImageBase64 ?: ""
                            )
                            hasUnsavedChanges = false
                            draftStore?.clearAutoDraft(autoDraftKey)
                        } else {
                            onCreateTestSet(
                                setTitle.trim(),
                                selectedPaperType,
                                difficulty,
                                finalDuration,
                                effectiveQuestions,
                                publishImmediately,
                                unattemptedPenalty,
                                answerReleaseMode,
                                selectedIconKey,
                                selectedCustomIconBase64,
                                selectedBannerImageBase64
                            )
                            hasUnsavedChanges = false
                            draftStore?.clearAutoDraft(autoDraftKey)
                            draftStore?.deleteDraft(activeDraftId)
                            savedDrafts = draftStore?.loadAllDrafts().orEmpty()
                            activeDraftId = UUID.randomUUID().toString()
                            singlePaperDraft.clear()
                            commonSectionDraft.clear()
                            languageSectionDraft.clear()
                            subjectSectionDraft.clear()
                            selectedBannerImageBase64 = null
                            userEditedTitle = false
                        }
                    }
                )
            }
        }

        // 6. Question List with Preview, Edit & Delete (Section-grouped for Multi-Section Papers)
        if (effectiveQuestions.isNotEmpty()) {
            itemsIndexed(effectiveQuestions) { idx, q ->
                if (selectedPaperType == PaperType.COMBINED && idx == 0) {
                    CoolingGlassBadge("SECTION A: COMMON PAPER (Q1–Q${commonSectionDraft.size})", glass.accentPrimary)
                    Spacer(Modifier.height(6.dp))
                } else if (selectedPaperType == PaperType.COMBINED && idx == commonSectionDraft.size) {
                    CoolingGlassBadge("SECTION B: LANGUAGE PAPER (Q${commonSectionDraft.size + 1}–Q${effectiveQuestions.size})", SoftLavender500)
                    Spacer(Modifier.height(6.dp))
                } else if (selectedPaperType.isFull150Paper()) {
                    if (idx == 0 && languageSectionDraft.isNotEmpty()) {
                        CoolingGlassBadge("PART 1: LANGUAGE PAPER (Q1–Q${languageSectionDraft.size})", SoftLavender500)
                        Spacer(Modifier.height(6.dp))
                    } else if (idx == languageSectionDraft.size && commonSectionDraft.isNotEmpty()) {
                        CoolingGlassBadge("PART 2: COMMON PAPER (Q${idx + 1}–Q${idx + commonSectionDraft.size})", glass.accentPrimary)
                        Spacer(Modifier.height(6.dp))
                    } else if (idx == languageSectionDraft.size + commonSectionDraft.size && subjectSectionDraft.isNotEmpty()) {
                        val subLabel = if (selectedPaperType == PaperType.CLASS_6_8_MATH_SCIENCE) {
                            "PART 3: CLASS 6–8 MATH & SCIENCE"
                        } else {
                            "PART 3: CLASS 9–10 MATHEMATICS"
                        }
                        CoolingGlassBadge("$subLabel (Q${idx + 1}–Q${effectiveQuestions.size})", IceCyan500)
                        Spacer(Modifier.height(6.dp))
                    }
                }

                CoolingGlassCard(contentPadding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CoolingGlassBadge("Q${idx + 1}", glass.accentPrimary)
                                CoolingGlassBadge(q.subject.substringBefore(" /"), SoftLavender500)
                                CoolingGlassBadge("Key: ${q.correctOption}", glass.correct)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = q.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { previewIndex = idx }) {
                            Icon(Icons.Rounded.Visibility, contentDescription = "Preview question", tint = glass.accentCyan)
                        }
                        IconButton(onClick = { editingIndex = idx }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit question", tint = glass.accentPrimary)
                        }
                        IconButton(onClick = {
                            when {
                                selectedPaperType == PaperType.COMBINED -> {
                                    if (idx < commonSectionDraft.size) commonSectionDraft.removeAt(idx)
                                    else {
                                        val langIdx = idx - commonSectionDraft.size
                                        if (langIdx in languageSectionDraft.indices) languageSectionDraft.removeAt(langIdx)
                                    }
                                }
                                selectedPaperType.isFull150Paper() -> {
                                    when {
                                        idx < languageSectionDraft.size -> languageSectionDraft.removeAt(idx)
                                        idx < languageSectionDraft.size + commonSectionDraft.size -> {
                                            val cIdx = idx - languageSectionDraft.size
                                            if (cIdx in commonSectionDraft.indices) commonSectionDraft.removeAt(cIdx)
                                        }
                                        else -> {
                                            val sIdx = idx - languageSectionDraft.size - commonSectionDraft.size
                                            if (sIdx in subjectSectionDraft.indices) subjectSectionDraft.removeAt(sIdx)
                                        }
                                    }
                                }
                                idx in singlePaperDraft.indices -> singlePaperDraft.removeAt(idx)
                            }
                        }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete question", tint = glass.incorrect)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    // Full Question Preview Dialog
    val pIdx = previewIndex
    if (pIdx != null && effectiveQuestions.isNotEmpty()) {
        QuestionPreviewDialog(
            questions = effectiveQuestions,
            initialIndex = pIdx,
            paperType = selectedPaperType,
            onDismiss = { previewIndex = null },
            onEditQuestion = { idx -> editingIndex = idx }
        )
    }

    // Inline Edit Question Dialog
    val eIdx = editingIndex
    if (eIdx != null && eIdx in effectiveQuestions.indices) {
        val forcedSec = when {
            selectedPaperType == PaperType.COMBINED -> {
                if (eIdx < commonSectionDraft.size) ExamQuestion.SECTION_COMMON else ExamQuestion.SECTION_LANGUAGE
            }
            selectedPaperType.isFull150Paper() -> when {
                eIdx < languageSectionDraft.size -> ExamQuestion.SECTION_LANGUAGE
                eIdx < languageSectionDraft.size + commonSectionDraft.size -> ExamQuestion.SECTION_COMMON
                else -> ExamQuestion.SECTION_SUBJECT
            }
            else -> null
        }
        EditQuestionDialog(
            question = effectiveQuestions[eIdx],
            questionNumber = eIdx + 1,
            paperType = selectedPaperType,
            forcedSection = forcedSec,
            onDismiss = { editingIndex = null },
            onSave = { updated ->
                when {
                    selectedPaperType == PaperType.COMBINED -> {
                        if (eIdx < commonSectionDraft.size) commonSectionDraft[eIdx] = updated
                        else {
                            val langIdx = eIdx - commonSectionDraft.size
                            if (langIdx in languageSectionDraft.indices) languageSectionDraft[langIdx] = updated
                        }
                    }
                    selectedPaperType.isFull150Paper() -> when {
                        eIdx < languageSectionDraft.size -> languageSectionDraft[eIdx] = updated
                        eIdx < languageSectionDraft.size + commonSectionDraft.size -> {
                            val cIdx = eIdx - languageSectionDraft.size
                            if (cIdx in commonSectionDraft.indices) commonSectionDraft[cIdx] = updated
                        }
                        else -> {
                            val sIdx = eIdx - languageSectionDraft.size - commonSectionDraft.size
                            if (sIdx in subjectSectionDraft.indices) subjectSectionDraft[sIdx] = updated
                        }
                    }
                    eIdx in singlePaperDraft.indices -> singlePaperDraft[eIdx] = updated
                }
                editingIndex = null
                onBannerMessage("Updated Q${eIdx + 1}!", false)
            }
        )
    }

    // Saved Drafts Bottom Sheet
    if (showDraftsSheet && draftStore != null) {
        SavedDraftsSheet(
            drafts = savedDrafts,
            onLoadDraft = { draft ->
                activeDraftId = draft.id
                setTitle = draft.title
                userEditedTitle = draft.title.isNotBlank()
                selectedPaperType = draft.paperType
                durationStr = draft.durationMinutes.toString()
                difficultyIndex = listOf("EASY", "MODERATE", "HARD").indexOf(draft.difficulty).coerceAtLeast(1)
                unattemptedPenalty = draft.unattemptedPenalty
                releaseModeIndex = if (draft.answerReleaseMode == "SCHEDULED") 1 else 0
                singlePaperDraft.clear()
                singlePaperDraft.addAll(draft.questions)
                commonSectionDraft.clear()
                commonSectionDraft.addAll(draft.commonSectionQuestions)
                languageSectionDraft.clear()
                languageSectionDraft.addAll(draft.languageSectionQuestions)
                subjectSectionDraft.clear()
                subjectSectionDraft.addAll(draft.subjectSectionQuestions)
                showDraftsSheet = false
                onBannerMessage("Resumed draft '${draft.title.ifBlank { draft.paperType.baseSetName }}'!", false)
            },
            onDeleteDraft = { id ->
                draftStore.deleteDraft(id)
                savedDrafts = draftStore.loadAllDrafts()
            },
            onDismiss = { showDraftsSheet = false }
        )
    }

    // Uploaded Document Review Dialog (PDF / DOC / DOCX / JSON / CSV)
    val outcome = uploadReviewOutcome
    if (outcome != null) {
        UploadedDocumentReviewDialog(
            outcome = outcome,
            targetLabel = activeTargetLabel,
            targetPaperType = selectedPaperType,
            onDismiss = { uploadReviewOutcome = null },
            onConfirmImport = { imported ->
                appendQuestionsToTarget(imported, autoPartitionBulk = true)
                uploadReviewOutcome = null
                onBannerMessage("Auto-classified & imported ${imported.size} questions from ${outcome.fileName}!", false)
            }
        )
    }

    // Set Icon Picker Dialog (Admin Only)
    if (showIconPickerDialog) {
        SetIconPickerDialog(
            paperType = selectedPaperType,
            currentIconKey = selectedIconKey,
            currentCustomIconBase64 = selectedCustomIconBase64,
            onDismiss = { showIconPickerDialog = false },
            onSave = { key, customB64 ->
                selectedIconKey = key
                selectedCustomIconBase64 = customB64
            }
        )
    }

    // Bulk Subject Correction Dialog (Admin Only)
    if (showBulkSubjectDialog && effectiveQuestions.isNotEmpty()) {
        BulkSubjectCorrectionDialog(
            questions = effectiveQuestions,
            paperType = selectedPaperType,
            onDismiss = { showBulkSubjectDialog = false },
            onAutoFixSections = if (selectedPaperType.isFull150Paper() || selectedPaperType == PaperType.COMBINED) {
                { applySmartAutoFixSections() }
            } else null,
            onApplyReassignment = { targetSubj, indices, onlyUnclassified ->
                val updated = BulkQuestionParser.bulkReassignSubject(
                    questions = effectiveQuestions,
                    targetSubject = targetSubj,
                    indices = if (onlyUnclassified) null else indices,
                    onlyUnclassified = onlyUnclassified,
                    paperType = selectedPaperType
                )
                when {
                    selectedPaperType == PaperType.COMBINED -> {
                        val part = BulkQuestionParser.partitionImportedQuestions(updated, PaperType.COMBINED)
                        languageSectionDraft.clear()
                        languageSectionDraft.addAll(part.language)
                        commonSectionDraft.clear()
                        commonSectionDraft.addAll(part.common)
                    }
                    selectedPaperType.isFull150Paper() -> {
                        val part = BulkQuestionParser.partitionImportedQuestions(updated, selectedPaperType)
                        languageSectionDraft.clear()
                        languageSectionDraft.addAll(part.language)
                        commonSectionDraft.clear()
                        commonSectionDraft.addAll(part.common)
                        subjectSectionDraft.clear()
                        subjectSectionDraft.addAll(part.subject)
                    }
                    else -> {
                        singlePaperDraft.clear()
                        singlePaperDraft.addAll(updated)
                    }
                }
                onBannerMessage("Bulk updated subjects to '$targetSubj' and re-validated!", false)
            }
        )
    }

    // Auto-Combine Existing 40 Common + 30 Language Papers Dialog
    if (showCombineDialog) {
        CombinedAutoCombineDialog(
            commonPapers = groupCommonPapers,
            languagePapers = groupLanguagePapers,
            isFull150 = selectedPaperType.isFull150Paper(),
            onDismiss = { showCombineDialog = false },
            onApplyCombination = { commonQs, langQs, _ ->
                commonSectionDraft.clear()
                commonSectionDraft.addAll(commonQs.map { it.copy(section = ExamQuestion.SECTION_COMMON) })
                languageSectionDraft.clear()
                languageSectionDraft.addAll(langQs.map { it.copy(section = ExamQuestion.SECTION_LANGUAGE) })
                durationStr = selectedPaperType.defaultDurationMinutes.toString()
                if (!userEditedTitle || setTitle.isBlank()) {
                    setTitle = TestSetNaming.nextUniqueTitle(selectedPaperType, existingTests)
                }
                onBannerMessage(
                    if (selectedPaperType.isFull150Paper()) "Loaded 30 Language + 40 Common! Now add 80 Subject questions into Part 3."
                    else "Combined 40 Common + 30 Language questions cleanly (45 min)!",
                    false
                )
            }
        )
    }

    // Unsaved Changes Confirmation Guard ("Discard unsaved changes?" / "Keep Editing")
    if (showUnsavedDiscardConfirm) {
        CoolingGlassConfirmationPopup(
            title = "Discard unsaved changes?",
            message = "You have unsaved edits in the Question / JSON Editor. Your progress is also backed up in Auto-Draft, or you can tap 'Keep Editing' to continue working.",
            confirmText = "Discard Changes",
            cancelText = "Keep Editing",
            confirmTestTag = "confirm_discard_unsaved_json_btn",
            onConfirm = {
                showUnsavedDiscardConfirm = false
                hasUnsavedChanges = false
                draftStore?.clearAutoDraft(autoDraftKey)
                jsonCsvInput = ""
                bulkInput = ""
                if (editingTest != null) {
                    onCancelEdit()
                }
            },
            onDismiss = {
                showUnsavedDiscardConfirm = false
            }
        )
    }
}

@Composable
private fun TestTypeOptionTile(
    title: String,
    detail: String,
    selected: Boolean,
    enabled: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .testTag(tag)
            .clip(shape)
            .background(if (selected) GlacierBlue600 else glass.glassSurfaceStrong)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) IceCyan500 else glass.glassBorder,
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) CrystalWhite else glass.textPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        AutoResizeSingleLineText(
            text = detail,
            style = MaterialTheme.typography.labelSmall,
            maxFontSize = 10.5.sp,
            minFontSize = 8.sp,
            color = if (selected) CrystalWhite.copy(alpha = 0.92f) else glass.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UploadFormatTile(
    title: String,
    ext: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .height(92.dp)
            .testTag(tag)
            .clip(shape)
            .background(glass.glassSurfaceStrong)
            .border(1.2.dp, accent.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(5.dp))
        AutoResizeSingleLineText(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            maxFontSize = 11.5.sp,
            minFontSize = 8.5.sp,
            color = glass.textPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        AutoResizeSingleLineText(
            text = ext,
            style = MaterialTheme.typography.labelSmall,
            maxFontSize = 10.sp,
            minFontSize = 8.sp,
            color = glass.textMuted,
            textAlign = TextAlign.Center
        )
    }
}
