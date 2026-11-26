package com.example.ui.screens

import android.app.Activity
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.glass.AnimatedBase64Image
import com.example.ui.glass.AutoResizeSingleLineText
import com.example.util.ImageCodec
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.AppRole
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamTest
import com.example.data.model.GroupMember
import com.example.data.model.LanguagePref
import com.example.data.model.OptionEMode
import com.example.data.model.PaperType
import com.example.data.model.RankCategory
import com.example.data.model.RankedAspirant
import com.example.data.model.SetType
import com.example.data.model.SolutionReattemptRecord
import com.example.data.model.TestAttempt
import com.example.domain.BulkQuestionParser
import com.example.domain.ExamScoringEngine
import com.example.ui.glass.CoolingGlassAvatar
import com.example.ui.glass.CoolingGlassBackground
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassBottomSheet
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassConfirmationPopup
import com.example.ui.glass.CoolingGlassDialog
import com.example.ui.glass.CoolingGlassEmptyState
import com.example.ui.glass.CoolingGlassIconButton
import com.example.ui.glass.CoolingGlassOptionCard
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.CoolingGlassProgressIndicator
import com.example.ui.glass.CoolingGlassQuestionCard
import com.example.ui.glass.CoolingGlassResultCard
import com.example.ui.glass.CoolingGlassSearchBar
import com.example.ui.glass.CoolingGlassTabRow
import com.example.ui.glass.CoolingGlassTextField
import com.example.ui.glass.OptionVisualState
import com.example.ui.glass.StatPill
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.GlacierBlue600
import com.example.ui.theme.IceCyan500
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.SoftLavender500
import com.example.ui.theme.appText
import com.example.ui.viewmodel.ActiveExamState
import com.example.ui.viewmodel.QuestionTimingBenchmark
import com.example.util.IstClock
import com.example.util.PdfExportMode
import com.example.util.PersistedExamSession
import com.example.util.RankingCardGenerator
import com.example.util.TestPdfGenerator
import kotlinx.coroutines.launch
import java.util.Locale

enum class TestsCatalogMode {
    /**
     * Active Tests tab: strictly shows ONLY sets that have NOT been completed yet
     * (unattempted sets + paused sets). Completed sets live in [COMPLETED_ONLY].
     */
    PENDING_AND_PAUSED_ONLY,

    /**
     * Completed Sets tab: strictly shows ONLY sets completed/submitted by the user,
     * with all Tests session features (Search, Paper Tabs, 7 Rank Categories, View Rank, Result & Solutions, PDF).
     */
    COMPLETED_ONLY
}

@Composable
fun TestsCatalogScreen(
    tests: List<ExamTest>,
    myAttempts: List<TestAttempt>,
    currentMember: GroupMember?,
    initialFilterTab: Int = 0,
    selectedCategoryState: RankCategory? = null,
    onSelectCategoryState: ((RankCategory?) -> Unit)? = null,
    catalogListState: LazyListState = rememberLazyListState(),
    pausedSessions: Map<String, PersistedExamSession> = emptyMap(),
    catalogMode: TestsCatalogMode = TestsCatalogMode.PENDING_AND_PAUSED_ONLY,
    onStartTest: (ExamTest) -> Unit,
    onViewResult: (ExamTest, TestAttempt) -> Unit,
    onViewTestRanking: (ExamTest) -> Unit = {},
    onPublishStarterSets: () -> Unit = {},
    onBannerMessage: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalCoolingGlass.current
    val isCompletedMode = catalogMode == TestsCatalogMode.COMPLETED_ONLY
    var selectedTab by remember(initialFilterTab, catalogMode) {
        mutableIntStateOf(initialFilterTab.coerceIn(0, 3))
    }
    var internalSelectedCategory by remember { mutableStateOf<RankCategory?>(selectedCategoryState) }
    val activeCategory = if (onSelectCategoryState != null) selectedCategoryState else internalSelectedCategory
    val setActiveCategory: (RankCategory?) -> Unit = { cat ->
        internalSelectedCategory = cat
        onSelectCategoryState?.invoke(cat)
    }
    var searchQuery by remember { mutableStateOf("") }

    val attemptByTestId = remember(myAttempts) {
        myAttempts
            .groupBy { it.testId }
            .mapValues { (_, list) -> list.maxBy { it.submittedAt?.seconds ?: 0L } }
    }
    val isAdmin = currentMember?.appRole()?.isAdminRole() == true

    // Strictly separate Pending/Paused Sets vs Completed Sets, and ALWAYS sort newest Admin-added set to the very top!
    val filteredTests = remember(tests, selectedTab, activeCategory, searchQuery, attemptByTestId, pausedSessions, catalogMode) {
        tests.filter { t ->
            val isCompleted = attemptByTestId.containsKey(t.id)
            val isPaused = !isCompleted && pausedSessions.containsKey(t.id)
            val matchesMode = when (catalogMode) {
                TestsCatalogMode.PENDING_AND_PAUSED_ONLY ->
                    (t.status == "PUBLISHED" || isPaused) && !isCompleted
                TestsCatalogMode.COMPLETED_ONLY ->
                    isCompleted
            }
            if (!matchesMode) return@filter false

            val matchesQuery = searchQuery.isBlank() ||
                t.title.contains(searchQuery, ignoreCase = true) ||
                t.creatorName.contains(searchQuery, ignoreCase = true)
            val pType = t.parsedPaperType()
            val resolvedCat = RankCategory.fromTest(t)
            val matchesCategory = activeCategory == null || resolvedCat == activeCategory
            val matchesTab = if (activeCategory != null) {
                true
            } else {
                when (selectedTab) {
                    0 -> true // All sets (newest Admin-added set always at top)
                    1 -> pType == PaperType.COMMON_GS || pType == PaperType.COMBINED || pType == PaperType.PRACTICE
                    2 -> pType == PaperType.LANGUAGE
                    else -> pType.isFull150Paper() || pType.isSeparateSubjectPaper()
                }
            }
            matchesQuery && matchesTab && matchesCategory
        }.sortedWith(ExamTest.newestFirstComparator)
    }

    val nowMillis = remember { System.currentTimeMillis() }

    LazyColumn(
        state = catalogListState,
        modifier = Modifier
            .fillMaxSize()
            .testTag(if (isCompletedMode) "completed_sets_list" else "pending_tests_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tests_catalog_mode_header"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCompletedMode) {
                            appText(
                                en = "Completed Sets (${filteredTests.size}) • Newest First",
                                defaultText = "पूर्ण किए गए सेट (${filteredTests.size}) • नया सेट सबसे ऊपर"
                            )
                        } else {
                            appText(
                                en = "Pending & Paused Sets (${filteredTests.size}) • Newest First",
                                defaultText = "बाकी व पॉज़ सेट (${filteredTests.size}) • नया सेट सबसे ऊपर"
                            )
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCompletedMode) glass.correct else glass.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                CoolingGlassSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = if (isCompletedMode) {
                        appText("Search completed sets...", "पूर्ण किए गए सेट खोजें...")
                    } else {
                        appText("Search test sets...", "टेस्ट सेट खोजें...")
                    }
                )
                CoolingGlassTabRow(
                    tabs = listOf("All", "Common/70Q", "Language", "Subject/150Q"),
                    selectedIndex = selectedTab,
                    onSelect = {
                        selectedTab = it
                        setActiveCategory(null)
                    }
                )
            }
        }
        // Exact 7 Rank Categories Filter Bar — Aligned 1:1 with RANKS section
        item {
            val categoryFilters = remember {
                listOf<Pair<String, RankCategory?>>(
                    "All Categories" to null,
                    RankCategory.LANGUAGE.displayName to RankCategory.LANGUAGE,
                    RankCategory.COMMON.displayName to RankCategory.COMMON,
                    RankCategory.LANGUAGE_COMMON.displayName to RankCategory.LANGUAGE_COMMON,
                    RankCategory.MATHEMATICS.displayName to RankCategory.MATHEMATICS,
                    RankCategory.SCIENCE.displayName to RankCategory.SCIENCE,
                    RankCategory.FULL_MATHS_SCIENCE.displayName to RankCategory.FULL_MATHS_SCIENCE,
                    RankCategory.FULL_MATHS.displayName to RankCategory.FULL_MATHS
                )
            }
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("six_part_filter_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categoryFilters.forEach { (label, cat) ->
                    val picked = activeCategory == cat
                    val tagSuffix = cat?.name?.lowercase() ?: "all"
                    Box(
                        modifier = Modifier
                            .testTag("test_category_chip_$tagSuffix")
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (picked) SoftLavender500 else glass.glassSurfaceStrong)
                            .border(
                                1.dp,
                                if (picked) SoftLavender500 else glass.glassBorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { setActiveCategory(cat) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (picked) CrystalWhite else glass.textPrimary,
                            fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (filteredTests.isEmpty()) {
            item {
                CoolingGlassEmptyState(
                    title = when {
                        activeCategory != null && isCompletedMode -> "No Completed ${activeCategory.displayName} Sets Yet"
                        activeCategory != null -> "No Pending ${activeCategory.displayName} Sets"
                        isCompletedMode -> appText("No Completed Sets Yet", "अभी कोई सेट पूरा नहीं हुआ है")
                        else -> appText("All Published Sets Completed!", "सभी सेट पूरे हो चुके हैं!")
                    },
                    subtitle = if (isCompletedMode) {
                        appText(
                            en = "Complete any test set from the Tests tab to view your score, solutions, rank, and PDFs here.",
                            defaultText = "Tests सेक्शन से कोई भी सेट पूरा करें — आपका स्कोर, हल, रैंक और PDF यहाँ दिखेंगे।"
                        )
                    } else {
                        appText(
                            en = "Unattempted and paused sets appear here with the newest Admin set at the top. Completed sets are in the Completed tab.",
                            defaultText = "अधूरे व पॉज़ किए गए सेट यहाँ दिखते हैं (नया सेट सबसे ऊपर)। पूरे हो चुके सेट Completed सेक्शन में हैं।"
                        )
                    }
                )
            }
        } else {
            items(filteredTests, key = { it.id }) { test ->
                val attempt = attemptByTestId[test.id]
                val pausedSession = if (attempt == null) pausedSessions[test.id] else null
                val pausedTimeStr = pausedSession?.let {
                    String.format(Locale.US, "%02d:%02d", it.pausedRemainingSeconds / 60, it.pausedRemainingSeconds % 60)
                }
                val createdMs = test.createdAt?.toDate()?.time
                val isRecentlyAdded = !isCompletedMode && createdMs != null && (nowMillis - createdMs) in 0L..(48L * 60L * 60L * 1000L)
                val dateStr = test.createdAt?.toDate()?.let {
                    IstClock.formatDate(it, "dd MMM yyyy")
                } ?: "Live"

                CoolingGlassCard(
                    modifier = Modifier.testTag("test_set_card_${test.id}"),
                    strongFrost = attempt != null || pausedSession != null,
                    borderColor = if (pausedSession != null) glass.review.copy(alpha = 0.65f) else null
                ) {
                    // Optional Set Banner (Live Animated GIF / Transparent PNG / JPG)
                    if (!test.bannerImageBase64.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 5.8f)
                                .clip(RoundedCornerShape(13.dp))
                                .border(0.9.dp, glass.accentCyan.copy(alpha = 0.45f), RoundedCornerShape(13.dp))
                                .testTag("test_set_banner_${test.id}")
                        ) {
                            AnimatedBase64Image(
                                base64 = test.bannerImageBase64,
                                contentDescription = "${test.title} live banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (ImageCodec.isGifBase64(test.bannerImageBase64)) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xCC06162B))
                                        .border(0.8.dp, IceCyan500.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIVE GIF",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = IceCyan500,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        SetIconAvatar(
                            iconKey = test.iconKey,
                            customIconBase64 = test.customIconBase64,
                            paperType = test.parsedPaperType(),
                            size = 46.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = test.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isRecentlyAdded) {
                                    CoolingGlassBadge(text = "NEW", color = glass.accentCyan)
                                }
                            }
                            Text(
                                text = "Created by ${test.creatorName} • $dateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CoolingGlassBadge(
                                text = test.difficulty,
                                color = when (test.difficulty) {
                                    "HARD" -> glass.incorrect
                                    "EASY" -> glass.correct
                                    else -> glass.accentPrimary
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(glass.accentPrimary.copy(alpha = 0.14f))
                                    .border(1.dp, glass.accentCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .clickable { onViewTestRanking(test) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("view_ranking_eye_${test.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Visibility,
                                        contentDescription = "View Rank",
                                        tint = glass.accentPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    AutoResizeSingleLineText(
                                        text = "View Rank",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxFontSize = 10.5.sp,
                                        minFontSize = 8.5.sp,
                                        color = glass.accentPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    val setType = test.resolvedSetType()
                    val meritLabel = when {
                        setType.isQualifyingOnly -> "Qualifying /30"
                        setType == SetType.PART_5_FULL_MATH_SCI || setType == SetType.PART_6_FULL_MATH -> "Merit /120"
                        setType == SetType.LEGACY_COMBINED_70 -> "Merit /40"
                        else -> "Merit /${test.questionCount}"
                    }
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        CoolingGlassBadge(text = "${setType.shortLabel} • ${test.questionCount} Q", color = glass.accentPrimary)
                        CoolingGlassBadge(text = meritLabel, color = if (setType.isQualifyingOnly) SoftLavender500 else glass.correct)
                        CoolingGlassBadge(text = "${test.durationMinutes} min", color = IceCyan500)
                        if (attempt != null) {
                            CoolingGlassBadge(
                                text = "${ExamScoringEngine.formatScore(attempt.totalScore)}/${attempt.maxMarks.toInt()}",
                                color = glass.correct
                            )
                        } else if (pausedTimeStr != null) {
                            CoolingGlassBadge(
                                text = "PAUSED • $pausedTimeStr",
                                color = glass.review
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (attempt == null) {
                            if (pausedTimeStr != null) {
                                CoolingGlassButton(
                                    text = "Resume Test ($pausedTimeStr)",
                                    icon = Icons.Rounded.PlayArrow,
                                    accentGradient = listOf(Color(0xFFD97706), Color(0xFFF59E0B)),
                                    modifier = Modifier.weight(1f),
                                    testTagName = "resume_test_${test.id}",
                                    onClick = { onStartTest(test) }
                                )
                            } else {
                                CoolingGlassButton(
                                    text = "Start Examination",
                                    icon = Icons.Rounded.PlayArrow,
                                    modifier = Modifier.weight(1f),
                                    testTagName = "start_test_${test.id}",
                                    onClick = { onStartTest(test) }
                                )
                            }
                        } else {
                            CoolingGlassButton(
                                text = "View Result & Solutions",
                                icon = Icons.Rounded.CheckCircle,
                                modifier = Modifier.weight(1f),
                                onClick = { onViewResult(test, attempt) }
                            )
                            CoolingGlassOutlinedButton(
                                text = "PDF",
                                icon = Icons.Rounded.PictureAsPdf,
                                onClick = {
                                    scope.launch {
                                        val res = TestPdfGenerator.generateAndExportAsync(
                                            context = context,
                                            test = test,
                                            attempt = attempt,
                                            aspirantName = currentMember?.displayName ?: "User",
                                            mode = PdfExportMode.COMPLETE_TEST,
                                            isAdmin = isAdmin
                                        )
                                        res.onSuccess {
                                            onBannerMessage("Complete Test PDF saved & ready to share!", false)
                                        }.onFailure {
                                            onBannerMessage(it.localizedMessage ?: "PDF export failed", true)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private data class ExamSectionTab(
    val label: String,
    val firstIndex: Int,
    val indices: List<Int>,
    val isQualifying: Boolean = false
)

object AndroidSecureMode {
    fun enableSecureMode(activity: Activity?) {
        try {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } catch (_: Throwable) {
        }
    }

    fun disableSecureMode(activity: Activity?) {
        try {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } catch (_: Throwable) {
        }
    }
}

private fun maskEmailForWatermark(email: String): String {
    val clean = email.trim()
    val atIdx = clean.indexOf('@')
    if (atIdx <= 1) return clean.ifBlank { "student@tre4" }
    val local = clean.substring(0, atIdx)
    val domain = clean.substring(atIdx)
    val visiblePrefix = local.take(2)
    return "$visiblePrefix***$domain"
}

@Composable
fun LiveExamScreen(
    examState: ActiveExamState,
    onSelectOption: (Int, String) -> Unit,
    onClearResponse: (Int) -> Unit,
    onToggleReview: (Int) -> Unit,
    onNavigateQuestion: (Int) -> Unit,
    onPause: () -> Unit = {},
    onSubmit: () -> Unit,
    onAbort: () -> Unit,
    bookmarkedIndices: Set<Int> = emptySet(),
    onToggleBookmark: (Int, ExamQuestion) -> Unit = { _, _ -> },
    onReportQuestion: (Int, String, String) -> Unit = { _, _, _ -> },
    onChangeLanguage: (LanguagePref) -> Unit = {},
    onAdjustFontScale: (Int) -> Unit = {},
    studentName: String = "",
    studentEmail: String = "",
    studentId: String = "",
    onSecurityViolation: (violationType: String, detailMessage: String) -> Int = { _, _ -> 0 },
    onDismissFinalFiveMinAlert: () -> Unit = {}
) {
    val glass = LocalCoolingGlass.current
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showPalette by remember { mutableStateOf(false) }
    var paletteListMode by remember { mutableStateOf(false) }
    var showPauseConfirm by remember { mutableStateOf(false) }
    var showSubmitConfirm by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showSecureNavLockModal by remember { mutableStateOf(false) }
    var activeSecurityWarningCount by remember { mutableStateOf<Int?>(null) }
    var activeSecurityWarningDetail by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportType by remember { mutableStateOf("REPORT_QUESTION") }
    var reportMsg by remember { mutableStateOf("") }

    val isSecureLiveExam = examState.isLiveExam && examState.secureModeEnabled
    val isNavigationLocked = isSecureLiveExam && examState.navigationExitLock

    // Native Android FLAG_SECURE for Secure Live Exams only
    DisposableEffect(isSecureLiveExam) {
        val activity = context as? Activity
        if (isSecureLiveExam) {
            AndroidSecureMode.enableSecureMode(activity)
        }
        onDispose {
            if (isSecureLiveExam) {
                AndroidSecureMode.disableSecureMode(activity)
            }
        }
    }

    // Lifecycle Focus-Loss / Tab-Switch / App-Backgrounding Detection for Secure Live Exams
    DisposableEffect(lifecycleOwner, isSecureLiveExam, examState.focusLossDetection, examState.submitting) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP &&
                isSecureLiveExam &&
                examState.focusLossDetection &&
                !examState.submitting
            ) {
                val activity = context as? Activity
                val isChangingConfig = activity?.isChangingConfigurations == true
                if (!isChangingConfig) {
                    val newCount = onSecurityViolation(
                        "FOCUS_LOSS_OR_APP_SWITCH",
                        "App backgrounded or focus lost during Live Exam"
                    )
                    activeSecurityWarningDetail = "Focus loss / app switch detected at ${IstClock.formatTimeWithSeconds(System.currentTimeMillis())}"
                    activeSecurityWarningCount = newCount
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // One-time subtle haptic cues when crossing Final 5-Minute or Final 60-Second marks
    LaunchedEffect(examState.showFinalFiveMinAlertBanner) {
        if (examState.showFinalFiveMinAlertBanner) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            } catch (_: Throwable) {
            }
        }
    }
    val isFinalMinute = examState.remainingSeconds in 1L..60L
    LaunchedEffect(isFinalMinute) {
        if (isFinalMinute) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            } catch (_: Throwable) {
            }
        }
    }

    val appLanguage = LocalAppLanguage.current
    val initialExamLang = if (appLanguage == LanguagePref.ENGLISH && examState.activeLanguage == LanguagePref.BILINGUAL) {
        LanguagePref.ENGLISH
    } else {
        examState.activeLanguage
    }
    // Local fallback overrides so LiveExamScreen works identically in standalone UI tests too
    var localLang by remember(examState.activeLanguage, appLanguage) { mutableStateOf(initialExamLang) }
    var localFontStep by remember(examState.fontScaleStep) { mutableIntStateOf(examState.fontScaleStep) }

    BackHandler {
        if (isNavigationLocked) {
            showSecureNavLockModal = true
        } else {
            showExitConfirm = true
        }
    }

    if (examState.questions.isEmpty()) {
        LaunchedEffect(Unit) { onAbort() }
        return
    }
    val qIndex = examState.currentIndex.coerceIn(0, examState.questions.lastIndex)
    val question = examState.questions[qIndex]
    val selectedOption = examState.selectedAnswers[qIndex]
    val marked = qIndex in examState.markedForReview
    val isBookmarked = qIndex in bookmarkedIndices
    val setType = remember(examState.test) { examState.test.resolvedSetType() }
    val optionEMode = remember(examState.test) { examState.test.resolvedOptionEMode() }

    // Build 5-status palette counts (Testbook-grade exact state tracking)
    val visitedSet = examState.visitedQuestions + qIndex
    val totalQ = examState.questions.size
    var countAnsweredOnly = 0
    var countAnsweredAndMarked = 0
    var countMarkedOnly = 0
    var countNotAnsweredVisited = 0
    var countNotVisited = 0
    for (i in 0 until totalQ) {
        val hasAns = examState.selectedAnswers.containsKey(i)
        val isRev = i in examState.markedForReview
        val isVis = i in visitedSet
        when {
            hasAns && isRev -> countAnsweredAndMarked++
            hasAns -> countAnsweredOnly++
            isRev -> countMarkedOnly++
            isVis -> countNotAnsweredVisited++
            else -> countNotVisited++
        }
    }
    val answeredStandardCount = examState.selectedAnswers.count { it.value != "E" }
    val dontKnowCount = examState.selectedAnswers.count { it.value == "E" }
    val unansweredCount = (totalQ - examState.selectedAnswers.size).coerceAtLeast(0)
    val reviewCount = examState.markedForReview.size

    // Build dynamic Section Switcher Tabs based on SetType
    val sectionTabs = remember(examState.questions, setType) {
        val grouped = linkedMapOf<String, Pair<Boolean, MutableList<Int>>>()
        examState.questions.forEachIndexed { idx, q ->
            val sec = q.resolvedSection()
            val normSubj = BulkQuestionParser.normalizeSubject(q.subject).let {
                if (it == BulkQuestionParser.UNCLASSIFIED_SUBJECT) q.subject.ifBlank { "General" } else it
            }
            val (tabName, isQual) = when (setType) {
                SetType.PART_1_COMMON -> normSubj to false
                SetType.PART_2_LANGUAGE -> (if (normSubj == "English") "English" else "Hindi") to true
                SetType.PART_5_FULL_MATH_SCI -> when (sec) {
                    ExamQuestion.SECTION_LANGUAGE -> "Language (Qual)" to true
                    ExamQuestion.SECTION_SUBJECT -> (if (normSubj == "Science") "Science" else "Maths") to false
                    else -> "Common" to false
                }
                SetType.PART_6_FULL_MATH -> when (sec) {
                    ExamQuestion.SECTION_LANGUAGE -> "Language (Qual)" to true
                    ExamQuestion.SECTION_SUBJECT -> "Maths (80Q)" to false
                    else -> "Common" to false
                }
                SetType.LEGACY_COMBINED_70 -> when (sec) {
                    ExamQuestion.SECTION_LANGUAGE -> "Language (Qual)" to true
                    else -> "Common (40Q)" to false
                }
                else -> normSubj to false
            }
            grouped.getOrPut(tabName) { isQual to mutableListOf() }.second.add(idx)
        }
        grouped.entries.map { (name, pair) ->
            ExamSectionTab(name, pair.second.first(), pair.second, pair.first)
        }
    }

    val timerText = formatExamCountdown(examState.remainingSeconds)
    val timerUrgency = resolveExamTimerUrgency(
        remainingSeconds = examState.remainingSeconds,
        totalDurationSeconds = examState.totalDurationSeconds
    )
    val remainingTimeFraction = if (examState.totalDurationSeconds > 0L) {
        (examState.remainingSeconds.toFloat() / examState.totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedTimeFraction by animateFloatAsState(
        targetValue = remainingTimeFraction,
        animationSpec = tween(durationMillis = 450),
        label = "examCountdownFraction"
    )
    val timerTargetColor = when (timerUrgency) {
        ExamTimerUrgency.CRITICAL_1_MIN -> Color(0xFFDC2626)
        ExamTimerUrgency.URGENT_5_MIN -> glass.incorrect
        ExamTimerUrgency.WARNING -> glass.review
        ExamTimerUrgency.CALM -> glass.accentPrimary
    }
    val animatedTimerColor by animateColorAsState(
        targetValue = timerTargetColor,
        animationSpec = tween(durationMillis = 350),
        label = "examCountdownColor"
    )
    val criticalPulseTransition = rememberInfiniteTransition(label = "criticalTimerPulse")
    val criticalScale by criticalPulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (timerUrgency == ExamTimerUrgency.CRITICAL_1_MIN) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "criticalTimerScale"
    )
    val criticalAlpha by criticalPulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (timerUrgency == ExamTimerUrgency.CRITICAL_1_MIN) 0.72f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "criticalTimerAlpha"
    )

    val posMarksLabel = "+${if (examState.test.positiveMarks % 1.0 == 0.0) examState.test.positiveMarks.toInt().toString() else ExamScoringEngine.formatScore(examState.test.positiveMarks)}"
    val negMarksLabel = examState.test.negativeMarkLabel()

    CoolingGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Premium Cooling Glass Exam Header + Live Countdown Timer + Language + Font + Pause + Palette
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(4f)
                    .shadow(
                        elevation = 6.dp,
                        ambientColor = if (glass.isDark) Color.Black.copy(alpha = 0.45f) else glass.accentPrimary.copy(alpha = 0.18f),
                        spotColor = if (glass.isDark) Color.Black.copy(alpha = 0.55f) else glass.accentPrimary.copy(alpha = 0.22f)
                    )
                    .background(glass.navBarSurface)
                    .border(0.8.dp, glass.glassBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoolingGlassIconButton(
                        icon = if (isNavigationLocked) Icons.Rounded.Lock else Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = if (isNavigationLocked) "Navigation Locked during Live Exam" else "Exit exam",
                        onClick = {
                            if (isNavigationLocked) {
                                showSecureNavLockModal = true
                            } else {
                                showExitConfirm = true
                            }
                        }
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = examState.test.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isSecureLiveExam) {
                                CoolingGlassBadge("SECURE LIVE", Color(0xFFDC2626))
                            }
                        }
                        Text(
                            text = "Ans: ${countAnsweredOnly + countAnsweredAndMarked} • Rev: $reviewCount • Left: $unansweredCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    // Real-Time Tabular Countdown Timer Pill (Blue -> Amber -> Red -> Pulsing Crimson < 60s)
                    val pillBg = when (timerUrgency) {
                        ExamTimerUrgency.CRITICAL_1_MIN -> Color(0xFFDC2626).copy(alpha = 0.22f)
                        ExamTimerUrgency.URGENT_5_MIN -> glass.incorrectBg
                        ExamTimerUrgency.WARNING -> glass.reviewBg
                        ExamTimerUrgency.CALM -> glass.accentPrimary.copy(alpha = 0.14f)
                    }
                    Row(
                        modifier = Modifier
                            .scale(if (timerUrgency == ExamTimerUrgency.CRITICAL_1_MIN) criticalScale else 1f)
                            .testTag("live_exam_countdown_pill")
                            .clip(RoundedCornerShape(12.dp))
                            .background(pillBg)
                            .border(
                                width = if (timerUrgency == ExamTimerUrgency.CRITICAL_1_MIN) 1.5.dp else 1.dp,
                                color = animatedTimerColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "Remaining Time",
                            tint = animatedTimerColor,
                            modifier = Modifier
                                .size(15.dp)
                                .alpha(if (timerUrgency == ExamTimerUrgency.CRITICAL_1_MIN) criticalAlpha else 1f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = timerText,
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                            color = if (timerUrgency == ExamTimerUrgency.CALM) glass.textPrimary else animatedTimerColor,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("live_exam_countdown_text")
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    // Live Language Switcher (BI -> HI -> EN without losing answers)
                    Box(
                        modifier = Modifier
                            .testTag("live_exam_lang_toggle")
                            .clip(RoundedCornerShape(10.dp))
                            .background(glass.accentPrimary.copy(alpha = 0.14f))
                            .border(1.dp, glass.accentCyan.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .clickable {
                                val next = when (localLang) {
                                    LanguagePref.BILINGUAL -> LanguagePref.HINDI
                                    LanguagePref.HINDI -> LanguagePref.ENGLISH
                                    LanguagePref.ENGLISH -> LanguagePref.BILINGUAL
                                }
                                localLang = next
                                onChangeLanguage(next)
                            }
                            .padding(horizontal = 7.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (localLang) {
                                LanguagePref.BILINGUAL -> "BI"
                                LanguagePref.HINDI -> "HI"
                                LanguagePref.ENGLISH -> "EN"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.accentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (examState.test.pauseAllowed && !examState.isLiveExam) {
                        Spacer(Modifier.width(3.dp))
                        CoolingGlassIconButton(
                            icon = Icons.Rounded.PauseCircleOutline,
                            contentDescription = "Pause Exam & Save Exact Time",
                            tint = glass.review,
                            testTagName = "exam_pause_btn",
                            onClick = { showPauseConfirm = true }
                        )
                    }
                    Spacer(Modifier.width(3.dp))
                    CoolingGlassIconButton(
                        icon = Icons.Rounded.GridView,
                        contentDescription = "Question Palette",
                        testTagName = "open_palette_btn",
                        onClick = { showPalette = true }
                    )
                }

                // Real-Time Depleting Exam Countdown Bar (Visual Urgency Indicator)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .testTag("exam_countdown_progress_bar")
                        .background(glass.glassBorderSubtle.copy(alpha = 0.35f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedTimeFraction)
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(
                                    when (timerUrgency) {
                                        ExamTimerUrgency.CRITICAL_1_MIN -> listOf(Color(0xFF991B1B), Color(0xFFEF4444))
                                        ExamTimerUrgency.URGENT_5_MIN -> listOf(Color(0xFFEA580C), Color(0xFFEF4444))
                                        ExamTimerUrgency.WARNING -> listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                                        ExamTimerUrgency.CALM -> listOf(GlacierBlue600, IceCyan500)
                                    }
                                )
                            )
                    )
                }
            }

            // Section Switcher Bar with live attempted counts
            if (sectionTabs.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_section_switcher")
                        .padding(top = 6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(sectionTabs) { tab ->
                        val active = qIndex in tab.indices
                        val attemptedInSec = tab.indices.count { examState.selectedAnswers.containsKey(it) }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        active && tab.isQualifying -> SoftLavender500
                                        active -> GlacierBlue600
                                        else -> glass.glassSurfaceStrong
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (active) glass.accentCyan else glass.glassBorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onNavigateQuestion(tab.firstIndex) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${tab.label} ($attemptedInSec/${tab.indices.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) CrystalWhite else glass.textPrimary,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Progress Bar
            CoolingGlassProgressIndicator(
                progress = (qIndex + 1).toFloat() / totalQ.coerceAtLeast(1).toFloat(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                height = 5.dp
            )

            // Non-blocking Escalating Countdown Urgency Banner (Final 60s Critical OR Final 5-Min Alert across ALL Active Tests)
            if (timerUrgency == ExamTimerUrgency.CRITICAL_1_MIN) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("critical_one_min_alert_banner")
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFDC2626).copy(alpha = 0.22f))
                        .border(1.5.dp, Color(0xFFDC2626).copy(alpha = criticalAlpha), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Final 60 Seconds Warning",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier
                                .size(19.dp)
                                .alpha(criticalAlpha)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "FINAL ${examState.remainingSeconds} SEC LEFT • HURRY UP! ($timerText)",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (localLang == LanguagePref.ENGLISH || appLanguage == LanguagePref.ENGLISH) {
                                    "Final 1 minute! The exam will auto-submit at 00:00."
                                } else {
                                    "अंतिम 1 मिनट! 00:00 पर परीक्षा अपने आप सबमिट हो जाएगी।"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textPrimary
                            )
                        }
                    }
                }
            } else if (examState.showFinalFiveMinAlertBanner || (examState.remainingSeconds in 61..300 && !examState.finalFiveMinAlertShown && (examState.isLiveExam || examState.totalDurationSeconds > 300L))) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("final_five_min_alert_banner")
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFDC2626).copy(alpha = 0.16f))
                        .border(1.2.dp, Color(0xFFDC2626), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Final 5 Minutes Alert",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "FINAL 5 MINUTES REMAINING ($timerText)",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Exam will auto-submit at 00:00. All answers are auto-saved in real time.",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textPrimary
                            )
                        }
                    }
                    if (examState.showFinalFiveMinAlertBanner) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFDC2626))
                                .clickable { onDismissFinalFiveMinAlert() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "OK",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Live Per-Question Timer & Prior-Attempt Benchmark Comparison Strip + Marks/Font/Bookmark/Report
            val currentQSeconds = examState.questionTimesSec[qIndex] ?: 0L
            val totalElapsedSec = (examState.totalDurationSeconds - examState.remainingSeconds).coerceAtLeast(0L)
            val priorQAvg = examState.priorQuestionAvgTimesSec[qIndex]
            val estimatedPace = if (examState.priorTotalAttemptsCount > 0 && examState.priorAvgTotalSeconds > 0 && totalQ > 0) {
                (examState.priorAvgTotalSeconds / totalQ).coerceAtLeast(1L)
            } else null
            val compareTargetSec = priorQAvg ?: estimatedPace
            val paceColor = when {
                compareTargetSec == null -> glass.accentPrimary
                currentQSeconds <= compareTargetSec -> glass.correct
                else -> glass.review
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp)
                    .testTag("live_question_timer_bar")
                    .clip(RoundedCornerShape(14.dp))
                    .background(glass.glassSurfaceStrong)
                    .border(1.dp, paceColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = "Live Question Timer",
                        tint = glass.accentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Q${qIndex + 1} Time: ${formatDurationCompact(currentQSeconds)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("live_question_timer_text")
                        )
                        Text(
                            text = "Elapsed: ${formatDurationCompact(totalElapsedSec)} • Marks: $posMarksLabel | $negMarksLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textMuted
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.End) {
                    val benchmarkLabel = when {
                        priorQAvg != null -> "Prev Q${qIndex + 1} Avg: ${formatDurationCompact(priorQAvg)}"
                        estimatedPace != null -> "Prev Set Pace: ~${formatDurationCompact(estimatedPace)}/Q"
                        else -> "First Timed Attempt"
                    }
                    Text(
                        text = benchmarkLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = paceColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("prior_question_benchmark_text")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "A-",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.accentPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(glass.glassSurface)
                                .clickable {
                                    localFontStep = (localFontStep - 1).coerceIn(-2, 3)
                                    onAdjustFontScale(-1)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "A+",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.accentPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(glass.glassSurface)
                                .clickable {
                                    localFontStep = (localFontStep + 1).coerceIn(-2, 3)
                                    onAdjustFontScale(1)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Scrollable Question + 5 Options (A, B, C, D, E)
            val questionScrollState = rememberScrollState()
            LaunchedEffect(qIndex) { questionScrollState.scrollTo(0) }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(questionScrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Question Actions Row (Section Tag + Bookmark + Report Issue)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val secBadgeText = when {
                        question.resolvedSection() == ExamQuestion.SECTION_LANGUAGE -> "LANGUAGE • QUALIFYING ONLY"
                        setType.isQualifyingOnly -> "QUALIFYING PAPER"
                        question.resolvedSection() == ExamQuestion.SECTION_SUBJECT -> "SUBJECT MERIT SECTION"
                        else -> "MERIT SECTION • ${question.subject.ifBlank { "General" }}"
                    }
                    CoolingGlassBadge(
                        text = secBadgeText,
                        color = if (question.resolvedSection() == ExamQuestion.SECTION_LANGUAGE) SoftLavender500 else glass.accentPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CoolingGlassIconButton(
                            icon = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Bookmark question",
                            tint = if (isBookmarked) glass.review else glass.textSecondary,
                            testTagName = "live_exam_bookmark_btn",
                            onClick = { onToggleBookmark(qIndex, question) }
                        )
                        CoolingGlassIconButton(
                            icon = Icons.Rounded.Flag,
                            contentDescription = "Report question",
                            tint = glass.review,
                            testTagName = "live_exam_report_btn",
                            onClick = {
                                reportMsg = ""
                                showReportDialog = true
                            }
                        )
                    }
                }

                if (question.passage.isNotBlank()) {
                    CoolingGlassCard(
                        tintColor = SoftLavender500,
                        borderColor = SoftLavender500.copy(alpha = 0.55f),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = if (localLang == LanguagePref.ENGLISH || appLanguage == LanguagePref.ENGLISH) {
                                "Reading Passage"
                            } else {
                                "Reading Passage / गद्यांश"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftLavender500,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = question.passage,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 + localFontStep * 2).coerceIn(12, 22).sp),
                            color = glass.textPrimary
                        )
                    }
                }

                val localizedQuestionText = question.displayText(localLang)
                if (localFontStep == 0) {
                    CoolingGlassQuestionCard(
                        questionNumber = qIndex + 1,
                        totalQuestions = totalQ,
                        subject = question.subject,
                        topic = question.topic,
                        source = question.source,
                        questionText = localizedQuestionText,
                        markedForReview = marked
                    )
                } else {
                    CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CoolingGlassBadge("Q ${qIndex + 1} / $totalQ", glass.accentPrimary)
                                CoolingGlassBadge(question.subject.ifBlank { "General" }, SoftLavender500)
                            }
                            if (marked) CoolingGlassBadge("Review", glass.review)
                        }
                        Spacer(Modifier.height(10.dp))
                        val scaledSp = (16 + localFontStep * 2).coerceIn(13, 24).sp
                        Text(
                            text = localizedQuestionText,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = scaledSp, lineHeight = (scaledSp.value + 6f).sp),
                            color = glass.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                val optionsList = question.optionsList(localLang)
                optionsList.forEach { (key, text) ->
                    val isPicked = selectedOption == key
                    val visualState = when {
                        isPicked && key == "E" && optionEMode == OptionEMode.NOT_ATTEMPTED -> OptionVisualState.DONT_KNOW_SELECTED
                        isPicked -> OptionVisualState.SELECTED
                        else -> OptionVisualState.DEFAULT
                    }
                    val badge = when {
                        isPicked && key == "E" && optionEMode == OptionEMode.NOT_ATTEMPTED -> "Selected • 0 Marks"
                        isPicked -> "Selected"
                        key == "E" && optionEMode == OptionEMode.NOT_ATTEMPTED -> "0 Marks"
                        else -> null
                    }
                    CoolingGlassOptionCard(
                        optionKey = key,
                        optionText = text,
                        state = visualState,
                        enabled = !examState.submitting,
                        badgeText = badge,
                        badgeColor = if (key == "E") glass.dontKnow else glass.accentCyan,
                        onClick = { onSelectOption(qIndex, key) }
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Clear Response & Mark for Review & Next Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoolingGlassOutlinedButton(
                        text = "Clear",
                        icon = Icons.Rounded.Clear,
                        enabled = selectedOption != null && !examState.submitting,
                        modifier = Modifier.weight(0.9f),
                        onClick = { onClearResponse(qIndex) }
                    )
                    CoolingGlassOutlinedButton(
                        text = if (marked) "Unmark Review" else "Mark & Next",
                        icon = if (marked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        tint = SoftLavender500,
                        enabled = !examState.submitting,
                        modifier = Modifier.weight(1.4f),
                        onClick = {
                            onToggleReview(qIndex)
                            if (!marked && qIndex < examState.questions.lastIndex) {
                                onNavigateQuestion(qIndex + 1)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(10.dp))
            }

            // Bottom Glass Action Bar: Previous, Save & Next, Submit Test
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(4f)
                    .shadow(
                        elevation = 8.dp,
                        ambientColor = if (glass.isDark) Color.Black.copy(alpha = 0.45f) else glass.accentPrimary.copy(alpha = 0.18f),
                        spotColor = if (glass.isDark) Color.Black.copy(alpha = 0.55f) else glass.accentPrimary.copy(alpha = 0.24f)
                    )
                    .background(glass.navBarSurface)
                    .border(0.8.dp, glass.glassBorderSubtle)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoolingGlassOutlinedButton(
                    text = "Prev",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    enabled = qIndex > 0 && !examState.submitting,
                    onClick = { onNavigateQuestion(qIndex - 1) }
                )
                if (qIndex < examState.questions.lastIndex) {
                    CoolingGlassButton(
                        text = "Save & Next",
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        enabled = !examState.submitting,
                        modifier = Modifier.weight(1f),
                        testTagName = "exam_next_btn",
                        onClick = { onNavigateQuestion(qIndex + 1) }
                    )
                } else {
                    CoolingGlassButton(
                        text = "Submit Test",
                        icon = Icons.Rounded.DoneAll,
                        loading = examState.submitting,
                        enabled = !examState.submitting,
                        modifier = Modifier.weight(1f),
                        testTagName = "exam_submit_btn",
                        accentGradient = listOf(Color(0xFF059669), IceCyan500),
                        onClick = { showSubmitConfirm = true }
                    )
                }
                if (qIndex < examState.questions.lastIndex) {
                    CoolingGlassOutlinedButton(
                        text = "Submit",
                        tint = glass.correct,
                        enabled = !examState.submitting,
                        testTagName = "exam_quick_submit_btn",
                        onClick = { showSubmitConfirm = true }
                    )
                }
            }
        }

        // Dynamic Diagonal Student Watermark Overlay (Non-clickable, only for Secure Live Exams)
        if (isSecureLiveExam && examState.dynamicWatermark) {
            val watermarkIdentity = remember(studentName, studentEmail, studentId, examState.liveExamId, examState.test.id) {
                val safeName = studentName.ifBlank { "ASPIRANT" }.uppercase(Locale.US)
                val maskedMail = maskEmailForWatermark(studentEmail)
                val shortUid = studentId.take(8).ifBlank { "UID" }
                val examTag = (examState.liveExamId ?: examState.test.id).take(12)
                "$safeName • $maskedMail • ID:$shortUid • EXAM:$examTag"
            }
            val watermarkTimestamp = remember(examState.remainingSeconds / 30) {
                IstClock.formatDateTime(System.currentTimeMillis())
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(3f)
                    .alpha(0.09f)
                    .testTag("secure_dynamic_watermark"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(4) {
                        Text(
                            text = "$watermarkIdentity • $watermarkTimestamp",
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.rotate(-22f)
                        )
                    }
                }
            }
        }
    }

    if (showSecureNavLockModal) {
        CoolingGlassDialog(
            title = "Navigation Locked • Secure Live Exam",
            onDismiss = { showSecureNavLockModal = false }
        ) {
            Text(
                text = "Back navigation and leaving the examination screen are locked while Secure Live Exam Mode is active.\n\nTo finish early, review your answers and tap 'Submit Final Examination'.",
                style = MaterialTheme.typography.bodyMedium,
                color = glass.textSecondary
            )
            Spacer(Modifier.height(14.dp))
            CoolingGlassButton(
                text = "Return to Live Exam",
                icon = Icons.Rounded.Security,
                modifier = Modifier.fillMaxWidth(),
                onClick = { showSecureNavLockModal = false }
            )
            Spacer(Modifier.height(8.dp))
            CoolingGlassOutlinedButton(
                text = "Submit Final Examination Now",
                tint = glass.correct,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    showSecureNavLockModal = false
                    showSubmitConfirm = true
                }
            )
        }
    }

    activeSecurityWarningCount?.let { count ->
        val limit = examState.warningLimit.coerceAtLeast(1)
        val isFlagged = count >= limit
        val modalTitle = when {
            isFlagged -> "Session Flagged for Admin Review ($count/$limit)"
            count == 1 -> "Security Warning 1/$limit (Soft Warning)"
            else -> "Security Warning $count/$limit (Strong Warning)"
        }
        val modalDesc = when {
            isFlagged -> "$activeSecurityWarningDetail.\n\nYour session has reached $count/$limit security warnings and has been flagged for Admin Review. Your exam has NOT been auto-submitted so you can finish your attempt—please remain on this screen until submission."
            count == 1 -> "$activeSecurityWarningDetail.\n\nSwitching apps, backgrounding the exam, or losing window focus is monitored during Secure Live Exams. Please stay on the exam screen."
            else -> "$activeSecurityWarningDetail.\n\nRepeated focus loss or app switching will flag your session for Admin Review and potential disqualification ($count/$limit warnings used)."
        }
        CoolingGlassDialog(
            title = modalTitle,
            onDismiss = { activeSecurityWarningCount = null }
        ) {
            Text(
                text = modalDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFlagged) glass.incorrect else glass.textSecondary
            )
            Spacer(Modifier.height(14.dp))
            CoolingGlassButton(
                text = "I Understand — Continue Exam",
                icon = Icons.Rounded.Warning,
                accentGradient = if (isFlagged) {
                    listOf(Color(0xFFDC2626), Color(0xFFB91C1C))
                } else {
                    listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                },
                modifier = Modifier.fillMaxWidth(),
                onClick = { activeSecurityWarningCount = null }
            )
        }
    }

    // 5-Status Question Palette Bottom Sheet with Grid / List View Toggle
    if (showPalette) {
        CoolingGlassBottomSheet(title = "Question Palette", onDismiss = { showPalette = false }) {
            // 5-Status Summary Counts + Grid/List Toggle
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CoolingGlassBadge("Answered: $countAnsweredOnly", glass.correct, Modifier.weight(1f))
                    CoolingGlassBadge("Not Ans: $countNotAnsweredVisited", glass.incorrect, Modifier.weight(1f))
                    CoolingGlassBadge("Not Visited: $countNotVisited", Color(0xFF94A3B8), Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoolingGlassBadge("Marked: $countMarkedOnly", SoftLavender500, Modifier.weight(1f))
                    CoolingGlassBadge("Ans+Marked: $countAnsweredAndMarked", SoftLavender500, Modifier.weight(1f))
                    CoolingGlassOutlinedButton(
                        text = if (paletteListMode) "Grid" else "List",
                        icon = if (paletteListMode) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                        testTagName = "palette_view_toggle",
                        onClick = { paletteListMode = !paletteListMode }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!paletteListMode) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(totalQ) { i ->
                        val hasAns = examState.selectedAnswers.containsKey(i)
                        val isRev = i in examState.markedForReview
                        val isVis = i in visitedSet
                        val isCurrent = i == qIndex
                        val statusColor = when {
                            hasAns && isRev -> SoftLavender500
                            hasAns -> glass.correct
                            isRev -> SoftLavender500
                            isVis -> glass.incorrect
                            else -> Color(0xFF94A3B8)
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    onNavigateQuestion(i)
                                    showPalette = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) GlacierBlue600 else statusColor.copy(alpha = 0.20f))
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.2.dp,
                                        color = if (isCurrent) CrystalWhite else statusColor,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${i + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isCurrent) CrystalWhite else statusColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (hasAns && isRev) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(glass.correct)
                                        .border(1.5.dp, CrystalWhite, CircleShape)
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(totalQ) { i ->
                        val qItem = examState.questions[i]
                        val ans = examState.selectedAnswers[i]
                        val isRev = i in examState.markedForReview
                        val isVis = i in visitedSet
                        val (stLabel, stColor) = when {
                            ans != null && isRev -> "Ans ($ans) + Marked" to SoftLavender500
                            ans != null -> "Answered ($ans)" to glass.correct
                            isRev -> "Marked" to SoftLavender500
                            isVis -> "Not Answered" to glass.incorrect
                            else -> "Not Visited" to Color(0xFF94A3B8)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (i == qIndex) glass.accentPrimary.copy(alpha = 0.14f) else glass.glassSurface)
                                .border(0.8.dp, if (i == qIndex) glass.accentCyan else glass.glassBorderSubtle, RoundedCornerShape(12.dp))
                                .clickable {
                                    onNavigateQuestion(i)
                                    showPalette = false
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Q${i + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = glass.accentPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(38.dp)
                            )
                            Text(
                                text = qItem.displayText(localLang),
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(6.dp))
                            CoolingGlassBadge(stLabel, stColor)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            CoolingGlassButton(
                text = "Submit Final Examination",
                icon = Icons.Rounded.DoneAll,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    showPalette = false
                    showSubmitConfirm = true
                }
            )
        }
    }

    if (showSubmitConfirm) {
        val sectionSummary = if (sectionTabs.size > 1) {
            "\n\nSection Breakdown:\n" + sectionTabs.joinToString("\n") { t ->
                val done = t.indices.count { examState.selectedAnswers.containsKey(it) }
                "• ${t.label}: $done/${t.indices.size} answered"
            }
        } else ""
        CoolingGlassConfirmationPopup(
            title = "Submit Examination?",
            message = "Time Remaining: $timerText\n\nAnswered: $countAnsweredOnly\nAnswered & Marked: $countAnsweredAndMarked\nMarked for Review (Unanswered): $countMarkedOnly\nNot Answered (Visited): $countNotAnsweredVisited\nNot Visited: $countNotVisited\nOption E (Not Attempted): $dontKnowCount$sectionSummary\n\nOnce submitted, your Original Attempt is permanently locked and evaluated for official ranking.",
            confirmText = "Lock & Submit",
            onConfirm = {
                showSubmitConfirm = false
                onSubmit()
            },
            onDismiss = { showSubmitConfirm = false }
        )
    }

    if (showPauseConfirm) {
        CoolingGlassConfirmationPopup(
            title = if (localLang == LanguagePref.ENGLISH || appLanguage == LanguagePref.ENGLISH) {
                "Pause Examination?"
            } else {
                "Pause Examination? / सेट पॉज करें?"
            },
            message = "Remaining time ($timerText) and all ${examState.selectedAnswers.size} selected answer(s) will be frozen & saved.\n\nWhen you resume later, the timer will continue from the exact same remaining time ($timerText) with zero time lost.",
            confirmText = "Pause & Save Time",
            cancelText = "Continue Exam",
            confirmTestTag = "confirm_pause_exam_btn",
            onConfirm = {
                showPauseConfirm = false
                onPause()
            },
            onDismiss = { showPauseConfirm = false }
        )
    }

    if (showExitConfirm) {
        CoolingGlassDialog(title = "Pause or Leave Test?", onDismiss = { showExitConfirm = false }) {
            Text(
                text = if (examState.test.pauseAllowed) {
                    "Need to step away? Pause this test to freeze your exact remaining time ($timerText) and resume anytime without losing your progress."
                } else {
                    "Pause is disabled for this test. Leaving without submitting will discard your unsaved session."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = glass.textSecondary
            )
            Spacer(Modifier.height(14.dp))
            if (examState.test.pauseAllowed) {
                CoolingGlassButton(
                    text = "Pause & Save Time ($timerText)",
                    icon = Icons.Rounded.PauseCircleOutline,
                    accentGradient = listOf(Color(0xFFD97706), Color(0xFFF59E0B)),
                    testTagName = "exit_dialog_pause_btn",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showExitConfirm = false
                        onPause()
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
            CoolingGlassOutlinedButton(
                text = "Resume Test",
                modifier = Modifier.fillMaxWidth(),
                onClick = { showExitConfirm = false }
            )
            Spacer(Modifier.height(8.dp))
            CoolingGlassOutlinedButton(
                text = "Discard Without Submitting",
                tint = glass.incorrect,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    showExitConfirm = false
                    onAbort()
                }
            )
        }
    }

    if (showReportDialog) {
        CoolingGlassDialog(title = "Report Q${qIndex + 1}", onDismiss = { showReportDialog = false }) {
            CoolingGlassTabRow(
                tabs = listOf("Wrong Question", "Wrong Answer"),
                selectedIndex = if (reportType == "REPORT_QUESTION") 0 else 1,
                onSelect = { reportType = if (it == 0) "REPORT_QUESTION" else "REPORT_ANSWER" }
            )
            Spacer(Modifier.height(10.dp))
            CoolingGlassTextField(
                value = reportMsg,
                onValueChange = { reportMsg = it },
                label = "Issue Details",
                singleLine = false,
                minLines = 3
            )
            Spacer(Modifier.height(12.dp))
            CoolingGlassButton(
                text = "Submit Report",
                enabled = reportMsg.trim().length >= 5,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onReportQuestion(qIndex, reportType, reportMsg.trim())
                    showReportDialog = false
                }
            )
        }
    }
}

@Composable
fun TestResultAndReviewScreen(
    test: ExamTest,
    attempt: TestAttempt,
    member: GroupMember?,
    overallRanking: List<RankedAspirant>,
    allAttemptsForTest: List<TestAttempt>,
    onBack: () -> Unit,
    onReportQuestion: (questionIndex: Int, reportType: String, message: String) -> Unit,
    onBannerMessage: (String, Boolean) -> Unit,
    bookmarkedIndices: Set<Int> = emptySet(),
    savedReattempt: SolutionReattemptRecord? = null,
    onToggleBookmark: (Int, ExamQuestion) -> Unit = { _, _ -> },
    onRecordReattempt: (Map<String, String>) -> Unit = {},
    onOpenFullLeaderboard: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalCoolingGlass.current
    val questions = remember(test) { test.parsedQuestions() }
    val setType = remember(test) { test.resolvedSetType() }
    val isAdmin = member?.appRole() in listOf(AppRole.GROUP_ADMIN, AppRole.SUPER_ADMIN)
    val solutionsReleased = remember(test, isAdmin) {
        ExamScoringEngine.areSolutionsReleased(test, isAdmin = isAdmin)
    }
    val accuracy = remember(attempt, setType) {
        if (setType.isQualifyingOnly) {
            ExamScoringEngine.computeAccuracy(attempt.correctCount.toInt(), attempt.incorrectCount.toInt())
        } else {
            ExamScoringEngine.computeAccuracy(
                (attempt.meritCorrectCount ?: attempt.correctCount).toInt(),
                (attempt.meritIncorrectCount ?: attempt.incorrectCount).toInt()
            )
        }
    }
    val sectionPerfRows = remember(test, questions, attempt) {
        ExamScoringEngine.computeSectionAndSubSubjectPerformance(test, questions, attempt)
    }
    val topperCompare = remember(test, attempt, allAttemptsForTest) {
        ExamScoringEngine.computeTopperVsAverage(test, attempt, allAttemptsForTest)
    }
    var reportQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var reportMessage by remember { mutableStateOf("") }
    var reportType by remember { mutableStateOf("REPORT_QUESTION") }
    var exporting by remember { mutableStateOf(false) }
    var showInAppSolutionViewer by remember { mutableStateOf(false) }
    val priorBench = remember(test.id, allAttemptsForTest) {
        QuestionTimingBenchmark.computeForTest(test.id, allAttemptsForTest)
    }

    if (showInAppSolutionViewer && solutionsReleased) {
        SolutionAnalysisScreen(
            test = test,
            attempt = attempt,
            questions = questions,
            priorBench = priorBench,
            isAdmin = isAdmin,
            exporting = exporting,
            bookmarkedIndices = bookmarkedIndices,
            savedReattempt = savedReattempt,
            onToggleBookmark = onToggleBookmark,
            onRecordReattempt = onRecordReattempt,
            onDirectDownloadPdf = {
                exporting = true
                scope.launch {
                    val res = TestPdfGenerator.generateAndExportAsync(
                        context = context,
                        test = test,
                        attempt = attempt,
                        aspirantName = attempt.aspirantName,
                        mode = PdfExportMode.COMPLETE_TEST,
                        isAdmin = isAdmin,
                        shareAfterSave = false
                    )
                    exporting = false
                    res.onSuccess {
                        onBannerMessage("Solution PDF directly saved to Downloads/TRE4!", false)
                    }.onFailure {
                        onBannerMessage(it.localizedMessage ?: "PDF download failed", true)
                    }
                }
            },
            onReportQuestion = { qIdx ->
                reportQuestionIndex = qIdx
                reportMessage = ""
            },
            onClose = { showInAppSolutionViewer = false }
        )
    } else {

    BackHandler { onBack() }

    val overallRank = overallRanking.firstOrNull { it.member.userId == attempt.userId }?.rank ?: 1
    val sortedTestAttempts = allAttemptsForTest.sortedByDescending {
        if (setType.isQualifyingOnly) it.effectiveLanguageTotal() else it.effectiveMeritScore()
    }
    val testRank = (sortedTestAttempts.indexOfFirst { it.userId == attempt.userId } + 1).coerceAtLeast(1)
    val totalParticipants = sortedTestAttempts.size.coerceAtLeast(1)
    val percentile = ExamScoringEngine.computePercentile(testRank, totalParticipants)
    val meritDisplayScore = if (setType.isQualifyingOnly) attempt.effectiveLanguageTotal() else attempt.effectiveMeritScore()
    val meritDisplayMax = if (setType.isQualifyingOnly) 30.0 else attempt.effectiveMeritMaxMarks()
    val meritDisplayPct = if (meritDisplayMax > 0.0) (meritDisplayScore / meritDisplayMax) * 100.0 else 0.0

    CoolingGlassBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoolingGlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            if (solutionsReleased) "Performance & Result Dashboard" else "Score Summary",
                            style = MaterialTheme.typography.titleLarge,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        val rankSubtitle = if (setType.isQualifyingOnly) {
                            "Part 2 Language Qualifying Paper • No Merit Rank"
                        } else {
                            "Set Rank #$testRank / $totalParticipants • ${String.format(Locale.US, "%.1f%%ile", percentile)} • Overall #$overallRank"
                        }
                        Text(rankSubtitle, style = MaterialTheme.typography.labelSmall, color = glass.accentPrimary)
                    }
                }
            }

            item {
                CoolingGlassResultCard(
                    testTitle = test.title,
                    score = meritDisplayScore,
                    maxMarks = meritDisplayMax,
                    percentage = meritDisplayPct,
                    correct = (if (setType.isQualifyingOnly) attempt.correctCount else (attempt.meritCorrectCount ?: attempt.correctCount)).toInt(),
                    incorrect = (if (setType.isQualifyingOnly) attempt.incorrectCount else (attempt.meritIncorrectCount ?: attempt.incorrectCount)).toInt(),
                    dontKnow = attempt.dontKnowCount.toInt(),
                    notAttempted = attempt.notAttemptedCount.toInt(),
                    negativeDeducted = attempt.negativeDeducted,
                    accuracyPercentage = accuracy
                )
            }

            // Unified Instant In-App Solutions & Reattempt, Direct PDF Downloads & WhatsApp Card (kept high in viewport)
            item {
                CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(14.dp)) {
                    Text(
                        text = if (solutionsReleased) "Solutions, Re-attempt, Direct PDF & Share" else "Export PDF & Share Result Card",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    // Row 1: Dedicated In-App "View Solution" (no Solution PDF button)
                    CoolingGlassButton(
                        text = if (solutionsReleased) "View Solution" else "Solutions Locked",
                        icon = if (solutionsReleased) Icons.Rounded.Visibility else Icons.Rounded.LockClock,
                        enabled = solutionsReleased,
                        testTagName = "view_solution_in_app_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showInAppSolutionViewer = true }
                    )
                    Spacer(Modifier.height(8.dp))
                    // Row 2: Question Paper + Answer Key / Key Locked (direct download)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoolingGlassOutlinedButton(
                            text = "Question Paper",
                            icon = Icons.Rounded.Description,
                            enabled = !exporting,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                exporting = true
                                scope.launch {
                                    val res = TestPdfGenerator.generateAndExportAsync(
                                        context, test, attempt, attempt.aspirantName, PdfExportMode.QUESTION_PAPER, isAdmin,
                                        shareAfterSave = false
                                    )
                                    exporting = false
                                    res.onSuccess { onBannerMessage("Question Paper PDF saved to Downloads/TRE4!", false) }
                                        .onFailure { onBannerMessage(it.localizedMessage ?: "Error", true) }
                                }
                            }
                        )
                        CoolingGlassOutlinedButton(
                            text = if (solutionsReleased) "Answer Key" else "Key Locked",
                            icon = if (solutionsReleased) Icons.Rounded.Description else Icons.Rounded.LockClock,
                            enabled = solutionsReleased && !exporting,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                exporting = true
                                scope.launch {
                                    val res = TestPdfGenerator.generateAndExportAsync(
                                        context, test, attempt, attempt.aspirantName, PdfExportMode.ANSWER_KEY, isAdmin,
                                        shareAfterSave = false
                                    )
                                    exporting = false
                                    res.onSuccess { onBannerMessage("Answer Key PDF saved to Downloads/TRE4!", false) }
                                        .onFailure { onBannerMessage(it.localizedMessage ?: "Error", true) }
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    // Row 3: Download Complete Test PDF
                    CoolingGlassButton(
                        text = if (solutionsReleased) "DOWNLOAD COMPLETE TEST PDF" else "DOWNLOAD ATTEMPT SUMMARY PDF",
                        icon = Icons.Rounded.PictureAsPdf,
                        loading = exporting,
                        enabled = !exporting,
                        testTagName = "download_complete_pdf_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            exporting = true
                            scope.launch {
                                val res = TestPdfGenerator.generateAndExportAsync(
                                    context, test, attempt, attempt.aspirantName, PdfExportMode.COMPLETE_TEST, isAdmin,
                                    shareAfterSave = false
                                )
                                exporting = false
                                res.onSuccess {
                                    onBannerMessage("Complete Test PDF directly saved to Downloads/TRE4!", false)
                                }.onFailure {
                                    onBannerMessage(it.localizedMessage ?: "PDF generation failed", true)
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    // Row 4: WhatsApp Card (My Attempt PDF removed)
                    CoolingGlassButton(
                        text = "WhatsApp Card",
                        icon = Icons.Rounded.Share,
                        accentGradient = listOf(Color(0xFF059669), Color(0xFF10B981)),
                        enabled = !exporting,
                        modifier = Modifier.fillMaxWidth(),
                        testTagName = "share_result_whatsapp_btn",
                        onClick = {
                            exporting = true
                            scope.launch {
                                val targetMember = member ?: GroupMember(
                                    userId = attempt.userId,
                                    displayName = attempt.aspirantName,
                                    photoBase64 = attempt.aspirantPhotoBase64,
                                    latestGsScore = attempt.totalScore,
                                    gsAverageScore = attempt.totalScore
                                )
                                val res = RankingCardGenerator.generateIndividualResultCardAsync(
                                    context = context,
                                    member = targetMember,
                                    overallRank = overallRank,
                                    individualTestRank = testRank,
                                    totalParticipants = overallRanking.size.coerceAtLeast(1),
                                    latestAttempt = attempt,
                                    testName = test.title
                                )
                                exporting = false
                                res.onSuccess { file ->
                                    RankingCardGenerator.shareImage(context, file, preferWhatsApp = true)
                                }.onFailure {
                                    onBannerMessage(it.localizedMessage ?: "Image generation failed", true)
                                }
                            }
                        }
                    )
                    if (onOpenFullLeaderboard != null && RankCategory.fromTest(test) != null) {
                        Spacer(Modifier.height(8.dp))
                        CoolingGlassOutlinedButton(
                            text = "View Leaderboard",
                            icon = Icons.Rounded.EmojiEvents,
                            testTagName = "result_open_leaderboard_btn",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onOpenFullLeaderboard
                        )
                    }
                }
            }

            // CARD A: Separate Language Qualifying Card (STRICTLY EXCLUDED from Merit / Rank)
            if (setType.hasLanguageSection) {
                item {
                    val passed = attempt.langPassed
                    CoolingGlassCard(
                        strongFrost = true,
                        tintColor = if (passed) glass.correct else glass.incorrect,
                        borderColor = if (passed) glass.correct else glass.incorrect,
                        modifier = Modifier.testTag("result_language_qualifying_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Language Qualifying Card", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Hindi: ${ExamScoringEngine.formatScore(attempt.hindiScore)}/22  •  English: ${ExamScoringEngine.formatScore(attempt.englishScore)}/8  •  Total: ${ExamScoringEngine.formatScore(attempt.effectiveLanguageTotal())}/30",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "Qualifying Only — Strictly NOT added to Merit Score, Cutoff, or Rank.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SoftLavender500,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            CoolingGlassBadge(
                                text = if (passed) "PASS (>= ${test.langPassMarks.toInt()})" else "FAIL (< ${test.langPassMarks.toInt()})",
                                color = if (passed) glass.correct else glass.incorrect
                            )
                        }
                    }
                }
            }

            // CARD B: Merit Insights, Cutoff Status, Rank & Percentile Card
            item {
                CoolingGlassCard(
                    strongFrost = true,
                    modifier = Modifier.testTag("result_merit_insights_card"),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Text(
                        text = if (setType.isQualifyingOnly) "Qualifying Summary & Timing" else "Merit Rank, Percentile & Cutoff Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill("Set Rank", if (setType.isQualifyingOnly) "Qual" else "#$testRank/$totalParticipants", glass.accentPrimary, Modifier.weight(1f))
                        StatPill("Percentile", if (setType.isQualifyingOnly) "—" else String.format(Locale.US, "%.1f%%", percentile), SoftLavender500, Modifier.weight(1f))
                        StatPill("Time", formatDurationCompact(attempt.timeSpentSeconds), glass.accentCyan, Modifier.weight(1f))
                    }
                    if (!setType.isQualifyingOnly && test.cutoffMarks != null) {
                        Spacer(Modifier.height(10.dp))
                        val diff = meritDisplayScore - test.cutoffMarks
                        val cleared = diff >= 0.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (cleared) glass.correctBg else glass.incorrectBg)
                                .border(1.dp, if (cleared) glass.correct else glass.incorrect, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (cleared) "Cutoff Cleared (+${ExamScoringEngine.formatScore(diff)})"
                                else "Missed Cutoff by ${ExamScoringEngine.formatScore(-diff)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (cleared) glass.correct else glass.incorrect,
                                fontWeight = FontWeight.Bold
                            )
                            CoolingGlassBadge("Cutoff: ${ExamScoringEngine.formatScore(test.cutoffMarks)}", if (cleared) glass.correct else glass.incorrect)
                        }
                    }
                    if (savedReattempt != null && savedReattempt.totalReattempted > 0L) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Solution Re-attempt Practice (Separate, Non-Ranking): ${savedReattempt.correctCount} Right • ${savedReattempt.wrongCount} Wrong (${savedReattempt.totalReattempted} Q practiced)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftLavender500,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // CARD C: Compare with Topper & Set Average Card
            item {
                CoolingGlassCard(
                    strongFrost = true,
                    modifier = Modifier.testTag("result_topper_compare_card"),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Text(
                        text = "Compare with Topper & Set Average (${topperCompare.totalParticipants} User${if (topperCompare.totalParticipants == 1) "" else "s"})",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    CompareMetricRow(
                        label = if (topperCompare.isQualifyingOnly) "Score (/30)" else "Merit (/${topperCompare.meritMaxMarks.toInt()})",
                        you = ExamScoringEngine.formatScore(topperCompare.yourMeritScore),
                        avg = ExamScoringEngine.formatScore(topperCompare.averageMeritScore),
                        topper = ExamScoringEngine.formatScore(topperCompare.topperMeritScore)
                    )
                    CompareMetricRow(
                        label = "Accuracy",
                        you = String.format(Locale.US, "%.1f%%", topperCompare.yourAccuracy),
                        avg = String.format(Locale.US, "%.1f%%", topperCompare.averageAccuracy),
                        topper = String.format(Locale.US, "%.1f%%", topperCompare.topperAccuracy)
                    )
                    CompareMetricRow(
                        label = "Attempted",
                        you = "${topperCompare.yourAttempted}",
                        avg = String.format(Locale.US, "%.1f", topperCompare.averageAttempted),
                        topper = "${topperCompare.topperAttempted}"
                    )
                    CompareMetricRow(
                        label = "Time",
                        you = formatDurationCompact(topperCompare.yourTimeSec),
                        avg = formatDurationCompact(topperCompare.averageTimeSec),
                        topper = formatDurationCompact(topperCompare.topperTimeSec)
                    )
                }
            }

            // CARD D: Granular Section-wise & 5 Sub-Subject Performance Table (Weak / Strong badges)
            if (sectionPerfRows.isNotEmpty()) {
                item {
                    CoolingGlassCard(
                        strongFrost = true,
                        modifier = Modifier.testTag("result_section_breakdown_card"),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = "Section & Sub-Subject Performance",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        sectionPerfRows.forEach { row ->
                            val badgeColor = when (row.strengthTag) {
                                "Strong" -> glass.correct
                                "Weak" -> glass.incorrect
                                "Qualifying" -> SoftLavender500
                                else -> glass.accentPrimary
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (row.isSubSubject) glass.glassSurface else glass.accentPrimary.copy(alpha = 0.08f))
                                    .border(0.8.dp, glass.glassBorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (row.isSubSubject) "↳ ${row.title}" else row.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textPrimary,
                                        fontWeight = if (row.isSubSubject) FontWeight.SemiBold else FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CoolingGlassBadge(row.strengthTag, badgeColor)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Score: ${ExamScoringEngine.formatScore(row.score)}/${row.maxMarks.toInt()}  •  ✓${row.correct}  ✕${row.incorrect}  ○${row.unattempted}  •  Acc: ${String.format(Locale.US, "%.0f%%", row.accuracyPct)}" +
                                        if (row.timeSpentSec > 0L) "  •  ${formatDurationCompact(row.timeSpentSec)}" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Solution Release Security Banner when SCHEDULED & not yet unlocked
            if (!solutionsReleased) {
                item {
                    CoolingGlassCard(
                        tintColor = glass.review,
                        borderColor = glass.review.copy(alpha = 0.65f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LockClock, contentDescription = "Answer Key Scheduled", tint = glass.review)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Verified Answer Key Scheduled",
                                style = MaterialTheme.typography.titleMedium,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        val releaseDateText = test.scheduledAt?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
                            raw.toLongOrNull()?.let { ms ->
                                IstClock.formatEpochMillis(ms, "dd MMM yyyy, hh:mm a 'IST'")
                            } ?: raw
                        } ?: "the scheduled release window"
                        Text(
                            text = "Your attempt is locked and evaluated. Verified answer keys and explanations will unlock automatically at $releaseDateText.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
    }

    reportQuestionIndex?.let { qIdx ->
        CoolingGlassDialog(
            title = "Report Q${qIdx + 1}",
            onDismiss = { reportQuestionIndex = null }
        ) {
            CoolingGlassTabRow(
                tabs = listOf("Wrong Question", "Wrong Answer"),
                selectedIndex = if (reportType == "REPORT_QUESTION") 0 else 1,
                onSelect = { reportType = if (it == 0) "REPORT_QUESTION" else "REPORT_ANSWER" }
            )
            Spacer(Modifier.height(10.dp))
            CoolingGlassTextField(
                value = reportMessage,
                onValueChange = { reportMessage = it },
                label = "Correction Details & Source Reference",
                singleLine = false,
                minLines = 3
            )
            Spacer(Modifier.height(14.dp))
            CoolingGlassButton(
                text = "Submit Audit Report",
                enabled = reportMessage.trim().length >= 5,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onReportQuestion(qIdx, reportType, reportMessage.trim())
                    reportQuestionIndex = null
                }
            )
        }
    }
}

@Composable
private fun CompareMetricRow(
    label: String,
    you: String,
    avg: String,
    topper: String
) {
    val glass = LocalCoolingGlass.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(glass.glassSurface)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = glass.textSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text("You", style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
            Text(you, style = MaterialTheme.typography.labelMedium, color = glass.accentPrimary, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text("Average", style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
            Text(avg, style = MaterialTheme.typography.labelMedium, color = glass.textPrimary, fontWeight = FontWeight.SemiBold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text("Topper", style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
            Text(topper, style = MaterialTheme.typography.labelMedium, color = glass.correct, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Formats seconds as compact mm:ss for live per-question timers and benchmarks.
 */
fun formatDurationCompact(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
}

enum class ExamTimerUrgency {
    CALM,
    WARNING,
    URGENT_5_MIN,
    CRITICAL_1_MIN
}

/**
 * Formats active exam remaining seconds into HH:MM:SS for exams >= 1 hour
 * and MM:SS for exams under 1 hour.
 */
fun formatExamCountdown(remainingSeconds: Long): String {
    val safe = remainingSeconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val minutes = (safe % 3600L) / 60L
    val seconds = safe % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Resolves real-time exam urgency level based on remaining seconds and total exam duration.
 */
fun resolveExamTimerUrgency(
    remainingSeconds: Long,
    totalDurationSeconds: Long
): ExamTimerUrgency {
    val safeRemaining = remainingSeconds.coerceAtLeast(0L)
    val safeTotal = totalDurationSeconds.coerceAtLeast(1L)
    val fraction = safeRemaining.toDouble() / safeTotal.toDouble()
    return when {
        safeRemaining in 1L..60L -> ExamTimerUrgency.CRITICAL_1_MIN
        safeRemaining == 0L -> ExamTimerUrgency.CRITICAL_1_MIN
        safeRemaining <= 300L -> ExamTimerUrgency.URGENT_5_MIN
        fraction <= 0.25 -> ExamTimerUrgency.WARNING
        else -> ExamTimerUrgency.CALM
    }
}


