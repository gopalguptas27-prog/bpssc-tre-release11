package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.glass.AutoResizeSingleLineText
import com.example.data.model.BookmarkedQuestion
import com.example.data.model.ExamGroup
import com.example.data.model.ExamTest
import com.example.data.model.GroupMember
import com.example.data.model.LangQualifyingStatus
import com.example.data.model.PaperType
import com.example.data.model.RankCategory
import com.example.data.model.RankedAspirant
import com.example.data.model.RankingPeriod
import com.example.data.model.TestAttempt
import com.example.domain.ExamScoringEngine
import com.example.ui.glass.CoolingGlassAvatar
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassDialog
import com.example.ui.glass.CoolingGlassEmptyState
import com.example.ui.glass.CoolingGlassLeaderboardCard
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.CoolingGlassProgressIndicator
import com.example.ui.glass.CoolingGlassSearchBar
import com.example.ui.glass.CoolingGlassTabRow
import com.example.ui.glass.ProfilePhotoZoomDialog
import com.example.ui.glass.StatPill
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.GlacierBlue600
import com.example.ui.theme.IceCyan500
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.RankBronze
import com.example.ui.theme.RankGold
import com.example.ui.theme.RankSilver
import com.example.ui.theme.SoftLavender500
import com.example.ui.theme.appText
import com.example.util.IstClock
import com.example.util.RankingCardGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun LeaderboardAndAnalyticsScreen(
    group: ExamGroup?,
    currentMember: GroupMember?,
    rankedList: List<RankedAspirant>,
    overallRanked: List<RankedAspirant>,
    selectedPeriod: RankingPeriod,
    selectedCategory: RankCategory,
    tests: List<ExamTest>,
    allAttempts: List<TestAttempt>,
    initialSubTab: Int = 0, // 0 = Leaderboard, 1 = All Scores, 2 = My Stats, 3 = By Part
    selectedTestId: String? = null,
    onSelectPeriod: (RankingPeriod) -> Unit,
    onSelectCategory: (RankCategory) -> Unit,
    onClearSetFilter: () -> Unit = {},
    onBannerMessage: (String, Boolean) -> Unit,
    members: List<GroupMember> = emptyList(),
    bookmarks: List<BookmarkedQuestion> = emptyList(),
    onRemoveBookmark: (BookmarkedQuestion) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalCoolingGlass.current
    var activeSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    var searchQuery by remember { mutableStateOf("") }
    var shareTargetFile by remember { mutableStateOf<Pair<String, File>?>(null) }
    var selectedDetailState by remember { mutableStateOf<Pair<RankedAspirant, RankCategory>?>(null) }
    var zoomedDetailAvatar by remember { mutableStateOf<RankedAspirant?>(null) }
    var generatingCard by remember { mutableStateOf(false) }

    val selectedSetTest = remember(selectedTestId, selectedPeriod, tests) {
        if (selectedPeriod == RankingPeriod.INDIVIDUAL_TEST && !selectedTestId.isNullOrBlank()) {
            tests.firstOrNull { it.id == selectedTestId }
        } else null
    }
    val selectedSetHeading = remember(selectedSetTest) {
        selectedSetTest?.let { t ->
            t.resolvedSetNumber()?.let { "Set $it - Rank" } ?: "${t.title} - Rank"
        }
    }

    val showLangColumn = remember(rankedList) {
        rankedList.any { it.showLanguageBadge }
    }

    val currentMonthLabel = remember {
        IstClock.formatNow("MMMM yyyy")
    }

    val filteredRanked = remember(rankedList, searchQuery) {
        if (searchQuery.isBlank()) rankedList
        else rankedList.filter { it.member.displayName.contains(searchQuery, ignoreCase = true) }
    }

    val myAttempts = remember(allAttempts, currentMember?.userId) {
        allAttempts.filter { it.userId == currentMember?.userId }
    }
    val testsById = remember(tests) { tests.associateBy { it.id } }
    val myCategoryAttempts = remember(myAttempts, selectedCategory, testsById) {
        myAttempts
            .filter { RankCategory.fromAttempt(it, testsById) == selectedCategory }
            .sortedBy { it.submittedAt?.toDate()?.time ?: 0L }
    }
    val myGsAttempts = remember(myAttempts) {
        myAttempts.filter {
            it.paperType == PaperType.COMMON_GS.name ||
                it.paperType == PaperType.COMBINED.name ||
                PaperType.fromString(it.paperType).isFull150Paper()
        }.sortedBy { it.submittedAt?.toDate()?.time ?: 0L }
    }
    val myLatestGsAttempt = myGsAttempts.lastOrNull()
    val myOverallRank = overallRanked.firstOrNull { it.member.userId == currentMember?.userId }?.rank ?: 1
    val overallAccuracy = remember(myCategoryAttempts, myAttempts) {
        val base = if (myCategoryAttempts.isNotEmpty()) myCategoryAttempts else myAttempts
        ExamScoringEngine.computeAccuracy(
            correct = base.sumOf { it.correctCount.toInt() },
            incorrect = base.sumOf { it.incorrectCount.toInt() }
        )
    }
    val subjectAnalytics = remember(myCategoryAttempts, myAttempts) {
        val base = if (myCategoryAttempts.isNotEmpty()) myCategoryAttempts else myAttempts
        ExamScoringEngine.computeSubjectAnalytics(base)
    }

    val effectiveMembers = remember(members, overallRanked, rankedList) {
        when {
            members.isNotEmpty() -> members
            overallRanked.isNotEmpty() -> overallRanked.map { it.member }
            else -> rankedList.map { it.member }
        }
    }
    val partCategoryRanked = remember(selectedCategory, selectedPeriod, effectiveMembers, allAttempts, tests) {
        ExamScoringEngine.computeCategoryRankings(
            period = selectedPeriod,
            category = selectedCategory,
            members = effectiveMembers,
            attempts = allAttempts,
            tests = tests
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("leaderboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary Mode Switcher: Leaderboard | All Scores | My Stats | By Part
        item {
            CoolingGlassTabRow(
                tabs = listOf("Leaderboard", "All Scores", "My Stats", "By Part"),
                selectedIndex = activeSubTab,
                onSelect = { activeSubTab = it }
            )
        }

        if (activeSubTab == 0 || activeSubTab == 1) {
            // Non-Scrolling Compact Single-Row Icon Bar for Time Scopes (Set | Daily | Weekly | Monthly | Overall)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ranking_period_icon_bar"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RankingPeriod.entries.forEach { period ->
                        val selected = period == selectedPeriod
                        val (iconVec, shortLabel) = rankingPeriodIconAndLabel(period)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("ranking_period_${period.name}")
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    if (selected) Brush.verticalGradient(listOf(GlacierBlue600, IceCyan500))
                                    else Brush.verticalGradient(listOf(glass.glassSurfaceStrong, glass.glassSurface))
                                )
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) glass.accentCyan else glass.glassBorder,
                                    shape = RoundedCornerShape(13.dp)
                                )
                                .clickable { onSelectPeriod(period) }
                                .padding(horizontal = 3.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = iconVec,
                                contentDescription = period.label,
                                tint = if (selected) CrystalWhite else glass.accentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.height(3.dp))
                            AutoResizeSingleLineText(
                                text = shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                maxFontSize = 10.5.sp,
                                minFontSize = 7.8.sp,
                                color = if (selected) CrystalWhite else glass.textSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Central 7-Category Selector (Replaces individual test chips completely!)
            item {
                val activeCount = rankedList.count { it.testsAttempted > 0 }
                CoolingGlassCard(
                    modifier = Modifier.testTag("rank_category_selector_card"),
                    strongFrost = true,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AutoResizeSingleLineText(
                            text = "Select Rank Category (7 Categories)",
                            style = MaterialTheme.typography.labelLarge,
                            maxFontSize = 13.sp,
                            minFontSize = 10.sp,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        CoolingGlassBadge(
                            text = selectedCategory.meritBaseLabel,
                            color = if (selectedCategory.isQualifyingOnly) SoftLavender500 else glass.accentCyan
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RankCategory.entries.forEach { cat ->
                            val picked = cat == selectedCategory
                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("rank_category_${cat.name}")
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (picked) glass.accentPrimary else glass.glassSurfaceStrong)
                                    .border(
                                        width = if (picked) 1.5.dp else 1.dp,
                                        color = if (picked) glass.accentCyan else glass.glassBorder,
                                        shape = RoundedCornerShape(11.dp)
                                    )
                                    .clickable { onSelectCategory(cat) }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AutoResizeSingleLineText(
                                    text = cat.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxFontSize = 11.sp,
                                    minFontSize = 8.5.sp,
                                    color = if (picked) CrystalWhite else glass.textPrimary,
                                    fontWeight = if (picked) FontWeight.Bold else FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val periodScopeDesc = when {
                        selectedSetTest != null && selectedSetHeading != null ->
                            "$selectedSetHeading (${selectedSetTest.difficulty}) • ${selectedCategory.label} • $activeCount ranked user(s)"
                        selectedPeriod == RankingPeriod.INDIVIDUAL_TEST ->
                            "Set Scope • ${selectedCategory.label} • $activeCount ranked user(s)"
                        selectedPeriod == RankingPeriod.DAILY ->
                            "Daily Report (Last 24h) • ${selectedCategory.label} • $activeCount ranked user(s)"
                        selectedPeriod == RankingPeriod.WEEKLY ->
                            "Weekly Report (Last 7 Days) • ${selectedCategory.label} • $activeCount ranked user(s)"
                        selectedPeriod == RankingPeriod.MONTHLY ->
                            "Monthly Report ($currentMonthLabel) • ${selectedCategory.label} • $activeCount ranked user(s)"
                        else ->
                            "Overall Combined • ${selectedCategory.label} • $activeCount ranked user(s)"
                    }
                    Text(
                        text = periodScopeDesc,
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.accentPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (selectedSetTest != null && selectedSetHeading != null) {
                item {
                    val diffColor = when (selectedSetTest.difficulty) {
                        "HARD" -> glass.incorrect
                        "EASY" -> glass.correct
                        else -> glass.accentPrimary
                    }
                    CoolingGlassCard(
                        modifier = Modifier.testTag("set_rank_heading_card"),
                        strongFrost = true,
                        tintColor = diffColor,
                        borderColor = diffColor.copy(alpha = 0.65f),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedSetHeading,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.testTag("set_rank_heading_text")
                                )
                                Text(
                                    text = "${selectedSetTest.title} • ${selectedSetTest.questionCount} Q",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .testTag("clear_set_rank_filter_btn")
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(glass.glassSurfaceStrong)
                                    .border(1.dp, glass.glassBorder, RoundedCornerShape(10.dp))
                                    .clickable { onClearSetFilter() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "All Sets",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoolingGlassBadge(text = selectedSetTest.difficulty, color = diffColor)
                            CoolingGlassBadge(text = selectedCategory.label, color = glass.accentCyan)
                            CoolingGlassBadge(
                                text = "Exact Set Filter Active",
                                color = SoftLavender500
                            )
                        }
                    }
                }
            }

            // Export & Share Action Bar (Result PNG & Full Group Ranking PNG)
            item {
                CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(14.dp)) {
                    Text(
                        text = "Export Cooling Glass Ranking Cards",
                        style = MaterialTheme.typography.titleSmall,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (showLangColumn) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Language Qualifying marks are tracked separately and never merged into GS/Merit ranking.",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textMuted
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoolingGlassButton(
                            text = "My Result PNG",
                            icon = Icons.Rounded.Image,
                            loading = generatingCard,
                            enabled = !generatingCard && currentMember != null,
                            testTagName = "generate_my_result_png",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (currentMember != null) {
                                    generatingCard = true
                                    scope.launch {
                                        val testName = "${selectedCategory.label} • ${myLatestGsAttempt?.testTitle ?: group?.name ?: "TRE 4.0"}"
                                        val res = RankingCardGenerator.generateIndividualResultCardAsync(
                                            context = context,
                                            member = currentMember,
                                            overallRank = myOverallRank,
                                            individualTestRank = null,
                                            totalParticipants = overallRanked.size.coerceAtLeast(1),
                                            latestAttempt = myLatestGsAttempt,
                                            testName = testName
                                        )
                                        generatingCard = false
                                        res.onSuccess { file ->
                                            shareTargetFile = "My TRE 4.0 Result Card" to file
                                            onBannerMessage("1080×1350 Result Card saved to Pictures/TRE4!", false)
                                        }.onFailure { err ->
                                            onBannerMessage(err.localizedMessage ?: "Failed to generate card", true)
                                        }
                                    }
                                }
                            }
                        )
                        CoolingGlassOutlinedButton(
                            text = "Group Rank PNG",
                            icon = Icons.Rounded.Share,
                            enabled = !generatingCard,
                            testTagName = "generate_group_rank_png",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                generatingCard = true
                                scope.launch {
                                    val exportPeriodLabel = "${selectedCategory.label} (${selectedPeriod.label})"
                                    val res = RankingCardGenerator.generateGroupLeaderboardCardAsync(
                                        context = context,
                                        groupName = group?.name ?: "TRE 4.0 Group",
                                        periodLabel = exportPeriodLabel,
                                        ranked = rankedList
                                    )
                                    generatingCard = false
                                    res.onSuccess { file ->
                                        shareTargetFile = "Group Ranking Card ($exportPeriodLabel)" to file
                                        onBannerMessage("Group Ranking Card saved to Pictures/TRE4!", false)
                                    }.onFailure { err ->
                                        onBannerMessage(err.localizedMessage ?: "Failed to generate ranking image", true)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Search Bar
            item {
                CoolingGlassSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search user name..."
                )
            }

            val activePerformers = filteredRanked.filter { it.testsAttempted > 0 }
            val topPerformers = activePerformers.take(3)
            val showTop3Podium = activeSubTab == 0 && searchQuery.isBlank() && topPerformers.isNotEmpty()
            val podiumUserIds = topPerformers.map { it.member.userId }.toSet()
            val listRows = if (showTop3Podium) {
                filteredRanked.filterNot { it.member.userId in podiumUserIds }
            } else {
                filteredRanked
            }

            // Celebratory Podium for Top Performers (Leaderboard tab — Rank #1, #2, #3)
            if (showTop3Podium) {
                item {
                    CrystalPodiumCard(
                        topPerformers = topPerformers,
                        onClickAspirant = { selectedDetailState = it to selectedCategory }
                    )
                }
            }

            // Rank Scope, Merit Base, Percentile & Cutoff Summary Header
            if (rankedList.isNotEmpty()) {
                item {
                    val activeEntries = rankedList.filter { it.testsAttempted > 0 }
                    val myEntry = rankedList.firstOrNull { it.member.userId == currentMember?.userId }
                    val scopeLabel = rankedList.firstOrNull()?.rankScopeLabel?.ifBlank { selectedCategory.label } ?: selectedCategory.label
                    val meritBase = rankedList.firstOrNull()?.meritBaseLabel?.ifBlank { selectedCategory.meritBaseLabel } ?: selectedCategory.meritBaseLabel
                    val highestScore = activeEntries.maxOfOrNull { it.displayScore } ?: 0.0
                    val avgScore = if (activeEntries.isEmpty()) 0.0 else activeEntries.map { it.displayScore }.average()
                    CoolingGlassCard(
                        modifier = Modifier.testTag("rank_scope_summary_card"),
                        strongFrost = true,
                        tintColor = glass.accentPrimary,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = scopeLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            CoolingGlassBadge(text = meritBase, color = glass.accentCyan)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatPill(
                                "Your Rank",
                                if (myEntry != null && myEntry.testsAttempted > 0 && myEntry.rank > 0) "#${myEntry.rank}/${activeEntries.size.coerceAtLeast(1)}" else "—",
                                glass.accentPrimary,
                                Modifier.weight(1f)
                            )
                            StatPill(
                                "Percentile",
                                if (myEntry != null && myEntry.testsAttempted > 0 && myEntry.rank > 0) String.format(Locale.US, "%.1f%%", myEntry.percentile) else "—",
                                SoftLavender500,
                                Modifier.weight(1f)
                            )
                            StatPill(
                                "Highest",
                                if (activeEntries.isNotEmpty()) ExamScoringEngine.formatScore(highestScore) else "—",
                                glass.correct,
                                Modifier.weight(1f)
                            )
                            StatPill(
                                "Average",
                                if (activeEntries.isNotEmpty()) ExamScoringEngine.formatScore(avgScore) else "—",
                                glass.accentCyan,
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (filteredRanked.isEmpty()) {
                item {
                    CoolingGlassEmptyState(
                        title = "No Users Found",
                        subtitle = "Complete a live ${selectedCategory.label} test set to appear on this leaderboard.",
                        icon = Icons.Rounded.EmojiEvents
                    )
                }
            } else if (activeSubTab == 0) {
                if (showTop3Podium && listRows.isNotEmpty()) {
                    item {
                        Text(
                            text = if (topPerformers.size >= 3) "Ranks #4 and below" else "Remaining Users",
                            style = MaterialTheme.typography.titleSmall,
                            color = glass.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("ranks_below_podium_header")
                        )
                    }
                }
                items(listRows, key = { it.member.userId }) { entry ->
                    val langBadge = when {
                        entry.languageNotQualified -> "Lang: FAIL (Not Qualified)"
                        else -> ExamScoringEngine.setLanguageBadgeText(entry)
                    }
                    val langColor = when {
                        entry.languageNotQualified -> glass.incorrect
                        entry.setLanguagePassed == true -> glass.correct
                        entry.setLanguagePassed == false -> glass.incorrect
                        entry.member.langQualifying() == LangQualifyingStatus.PASS -> glass.correct
                        entry.member.langQualifying() == LangQualifyingStatus.FAIL -> glass.incorrect
                        else -> glass.textMuted
                    }
                    val maxMarksInt = entry.maxMarks.toInt().coerceAtLeast(1)
                    val scorePct = if (entry.testsAttempted > 0 && entry.maxMarks > 0.0) {
                        (entry.displayScore / entry.maxMarks) * 100.0
                    } else 0.0
                    val timeText = if (entry.testsAttempted > 0 && entry.timeSpentSeconds > 0L) {
                        " • ${formatDurationCompact(entry.timeSpentSeconds)}"
                    } else ""
                    val subtitleText = if (entry.testsAttempted > 0) {
                        "Rank #${entry.rank} • ${String.format(Locale.US, "%.1f%%", scorePct)}$timeText • Sets: ${entry.testsAttempted}/${entry.testsAvailable}"
                    } else {
                        "Not Attempted Yet • Sets: 0/${entry.testsAvailable}"
                    }
                    val subjectLine = remember(entry.subjectScores, entry.subjectCorrectCounts) {
                        if (entry.subjectScores.isEmpty()) null
                        else entry.subjectScores.entries.take(3).joinToString(" • ") { (subj, marks) ->
                            val shortSubj = subj.substringBefore(" /").take(8)
                            "$shortSubj ${ExamScoringEngine.formatScore(marks)}"
                        }
                    }
                    CoolingGlassLeaderboardCard(
                        rank = entry.rank,
                        aspirantName = entry.member.displayName,
                        photoBase64 = entry.member.photoBase64,
                        scoreText = if (entry.testsAttempted > 0) {
                            "${ExamScoringEngine.formatScore(entry.displayScore)} / $maxMarksInt"
                        } else {
                            "— / $maxMarksInt"
                        },
                        testsAttemptedText = subtitleText,
                        subjectSummaryText = subjectLine,
                        languageBadgeText = langBadge,
                        languageBadgeColor = langColor,
                        statusBadgeText = entry.attemptStatusLabel.takeIf { it.isNotBlank() },
                        statusBadgeColor = if (entry.testsAttempted > 0) glass.accentPrimary else glass.textMuted,
                        isCurrentUser = entry.member.userId == currentMember?.userId,
                        testTagName = "leaderboard_row_${entry.member.userId}",
                        onClick = { selectedDetailState = entry to selectedCategory }
                    )
                }
            } else {
                // Centralized All Users' Scores Table Sheet
                item {
                    CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(12.dp)) {
                        Text(
                            text = if (selectedSetTest != null && selectedSetHeading != null) {
                                "All Users' Score Sheet — $selectedSetHeading (${selectedSetTest.difficulty})"
                            } else {
                                "All Users' Score Sheet — ${selectedCategory.label} (${selectedPeriod.label})"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(glass.accentPrimary.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 7.dp)
                        ) {
                            Text("#", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = glass.accentPrimary, modifier = Modifier.width(34.dp))
                            Text("User", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = glass.accentPrimary, modifier = Modifier.weight(1.4f))
                            Text(
                                "Score",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = glass.accentPrimary,
                                modifier = Modifier.weight(0.75f),
                                textAlign = TextAlign.End
                            )
                            Text("Sets", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = glass.accentPrimary, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
                            if (showLangColumn) {
                                Text("Lang", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = glass.accentPrimary, modifier = Modifier.weight(0.95f), textAlign = TextAlign.End)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        filteredRanked.forEach { entry ->
                            val isMe = entry.member.userId == currentMember?.userId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isMe) glass.accentCyan.copy(alpha = 0.14f) else Color.Transparent)
                                    .clickable { selectedDetailState = entry to selectedCategory }
                                    .padding(horizontal = 8.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#${entry.rank}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = glass.accentPrimary, modifier = Modifier.width(34.dp))
                                Text(entry.member.displayName, style = MaterialTheme.typography.bodySmall, fontWeight = if (isMe) FontWeight.Bold else FontWeight.Medium, color = glass.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.4f))
                                Text(
                                    if (entry.testsAttempted > 0) ExamScoringEngine.formatScore(entry.displayScore) else "—",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = glass.textPrimary,
                                    modifier = Modifier.weight(0.75f),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    "${entry.testsAttempted}/${entry.testsAvailable}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textSecondary,
                                    modifier = Modifier.weight(0.6f),
                                    textAlign = TextAlign.End
                                )
                                if (showLangColumn) {
                                    val cellText = when {
                                        entry.setLanguagePassed == true -> "PASS"
                                        entry.setLanguagePassed == false -> "FAIL"
                                        entry.member.langQualifying() == LangQualifyingStatus.PASS -> "PASS"
                                        entry.member.langQualifying() == LangQualifyingStatus.FAIL -> "FAIL"
                                        else -> "N/A"
                                    }
                                    val cellColor = when (cellText) {
                                        "PASS" -> glass.correct
                                        "FAIL" -> glass.incorrect
                                        else -> glass.textMuted
                                    }
                                    Text(
                                        cellText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = cellColor,
                                        modifier = Modifier.weight(0.95f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeSubTab == 3) {
            // ================= 7-CATEGORY INDEPENDENT PART RANKINGS =================
            item {
                CoolingGlassCard(
                    modifier = Modifier.testTag("part_category_ranking_section"),
                    strongFrost = true,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Text(
                        text = "7-Category Independent Rankings",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Each category aggregates strictly isolated sets (${selectedPeriod.label}). Full Maths + Science (/120) never mixes with Full Maths (/120).",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RankCategory.entries.forEach { cat ->
                            val picked = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .testTag("part_rank_chip_${cat.name}")
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (picked) glass.accentPrimary else glass.glassSurfaceStrong)
                                    .border(
                                        1.dp,
                                        if (picked) glass.accentCyan else glass.glassBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectCategory(cat) }
                                    .padding(horizontal = 11.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = cat.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (picked) CrystalWhite else glass.textPrimary,
                                    fontWeight = if (picked) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CoolingGlassBadge(text = selectedCategory.label, color = glass.accentPrimary)
                        CoolingGlassBadge(
                            text = selectedCategory.meritBaseLabel,
                            color = if (selectedCategory.isQualifyingOnly) SoftLavender500 else glass.correct
                        )
                    }
                }
            }

            if (partCategoryRanked.isEmpty() || partCategoryRanked.all { it.testsAttempted == 0 }) {
                item {
                    CoolingGlassEmptyState(
                        title = "No Attempts in ${selectedCategory.label}",
                        subtitle = "Complete a ${selectedCategory.label} set to populate this category ranking table.",
                        icon = Icons.Rounded.EmojiEvents
                    )
                }
            } else {
                items(partCategoryRanked, key = { "part_${selectedCategory.name}_${it.member.userId}" }) { entry ->
                    val maxMarksInt = entry.maxMarks.toInt().coerceAtLeast(1)
                    val isQualifying = selectedCategory.isQualifyingOnly
                    val pct = if (entry.testsAttempted > 0 && entry.maxMarks > 0.0) {
                        (entry.displayScore / entry.maxMarks) * 100.0
                    } else 0.0
                    val subtitle = if (entry.testsAttempted == 0) {
                        "Not Attempted Yet"
                    } else if (isQualifying) {
                        val status = if (entry.setLanguagePassed == true) "PASS (≥9/30)" else "FAIL (<9/30)"
                        "Qualifying History • $status • ${String.format(Locale.US, "%.1f%%", pct)} • Sets: ${entry.testsAttempted}/${entry.testsAvailable}"
                    } else {
                        "Rank #${entry.rank} • ${String.format(Locale.US, "%.1f%%", pct)} • ${formatDurationCompact(entry.timeSpentSeconds)} • Sets: ${entry.testsAttempted}/${entry.testsAvailable}"
                    }
                    CoolingGlassLeaderboardCard(
                        rank = entry.rank,
                        aspirantName = entry.member.displayName,
                        photoBase64 = entry.member.photoBase64,
                        scoreText = if (entry.testsAttempted > 0) {
                            "${ExamScoringEngine.formatScore(entry.displayScore)} / $maxMarksInt"
                        } else "— / $maxMarksInt",
                        testsAttemptedText = subtitle,
                        languageBadgeText = ExamScoringEngine.setLanguageBadgeText(entry),
                        languageBadgeColor = if (entry.setLanguagePassed == false) glass.incorrect else glass.correct,
                        statusBadgeText = entry.attemptStatusLabel.takeIf { it.isNotBlank() },
                        isCurrentUser = entry.member.userId == currentMember?.userId,
                        onClick = { selectedDetailState = entry to selectedCategory }
                    )
                }
            }
        } else {
            // ================= MY PERFORMANCE ANALYTICS & BOOKMARKS =================
            item {
                CoolingGlassCard(strongFrost = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Assessment, contentDescription = null, tint = glass.accentPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "My Performance Overview",
                            style = MaterialTheme.typography.titleLarge,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill("GS Rank", if ((currentMember?.gsTestsAttempted ?: 0L) > 0L) "#$myOverallRank" else "—", glass.accentPrimary, Modifier.weight(1f))
                        StatPill("GS Avg", ExamScoringEngine.formatScore(currentMember?.gsAverageScore ?: 0.0), glass.accentCyan, Modifier.weight(1f))
                        StatPill("Highest", ExamScoringEngine.formatScore(currentMember?.gsHighestScore ?: 0.0), glass.correct, Modifier.weight(1f))
                        StatPill("Tests", "${currentMember?.gsTestsAttempted ?: 0L}", SoftLavender500, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill("Latest GS", ExamScoringEngine.formatScore(currentMember?.latestGsScore ?: 0.0), glass.accentPrimary, Modifier.weight(1f))
                        StatPill("Accuracy", String.format(Locale.US, "%.1f%%", overallAccuracy), glass.correct, Modifier.weight(1f))
                        val langBadgeColor = when (currentMember?.langQualifying()) {
                            LangQualifyingStatus.PASS -> glass.correct
                            LangQualifyingStatus.FAIL -> glass.incorrect
                            else -> glass.textMuted
                        }
                        StatPill(
                            "Language",
                            currentMember?.let { ExamScoringEngine.languageBadgeText(it) } ?: "N/A",
                            langBadgeColor,
                            Modifier.weight(1.4f)
                        )
                    }
                }
            }

            // Score Trend Line Chart
            item {
                CoolingGlassCard(strongFrost = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = glass.accentCyan)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Common GS Score Trend (Out of 40)",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (myGsAttempts.isEmpty()) {
                        Text(
                            text = "Attempt Common GS tests to unlock your score progression graph.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textMuted
                        )
                    } else {
                        val lineColor = glass.accentCyan
                        val dotColor = glass.accentPrimary
                        val gridColor = glass.glassBorderSubtle
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(8.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            // Reference lines at 0, 20, 40
                            listOf(0f, 0.5f, 1f).forEach { frac ->
                                val y = h * (1f - frac)
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1.5f
                                )
                            }
                            val pts = myGsAttempts.takeLast(10)
                            val stepX = if (pts.size > 1) w / (pts.size - 1) else w / 2f
                            val path = Path()
                            pts.forEachIndexed { idx, att ->
                                val x = if (pts.size > 1) idx * stepX else w / 2f
                                val norm = (att.totalScore.toFloat() / 40f).coerceIn(0f, 1f)
                                val y = h * (1f - norm)
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            if (pts.size > 1) {
                                drawPath(
                                    path = path,
                                    color = lineColor,
                                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                                )
                            }
                            pts.forEachIndexed { idx, att ->
                                val x = if (pts.size > 1) idx * stepX else w / 2f
                                val norm = (att.totalScore.toFloat() / 40f).coerceIn(0f, 1f)
                                val y = h * (1f - norm)
                                drawCircle(color = CrystalWhite, radius = 10f, center = Offset(x, y))
                                drawCircle(color = dotColor, radius = 6.5f, center = Offset(x, y))
                            }
                        }
                    }
                }
            }

            // Subject-Wise Performance Bar Analytics
            item {
                CoolingGlassCard(strongFrost = true) {
                    Text(
                        text = "Subject-Wise Accuracy (8 Marks Each)",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    if (subjectAnalytics.isEmpty()) {
                        Text(
                            text = "No subject breakdown yet. Complete a Common GS Mock Test to view Math, Science, History, Geography & PIB Current Affairs analytics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textMuted
                        )
                    } else {
                        val strongest = subjectAnalytics.first()
                        val weakest = subjectAnalytics.last()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CoolingGlassBadge("Strongest: ${strongest.subject}", glass.correct, Modifier.weight(1f))
                            CoolingGlassBadge("Focus Area: ${weakest.subject}", glass.review, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        // Visual Bar Chart Canvas
                        val barPrimary = glass.accentPrimary
                        val barCyan = glass.accentCyan
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .padding(horizontal = 6.dp)
                        ) {
                            val count = subjectAnalytics.size.coerceAtLeast(1)
                            val slotW = size.width / count
                            val barW = (slotW * 0.56f).coerceAtMost(52.dp.toPx())
                            subjectAnalytics.forEachIndexed { i, item ->
                                val frac = (item.averagePercentage / 100.0).toFloat().coerceIn(0.06f, 1f)
                                val barH = size.height * frac
                                val left = i * slotW + (slotW - barW) / 2f
                                drawRoundRect(
                                    brush = Brush.verticalGradient(listOf(barCyan, barPrimary)),
                                    topLeft = Offset(left, size.height - barH),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(14f, 14f)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        subjectAnalytics.forEach { subj ->
                            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(subj.subject, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = glass.textPrimary)
                                    Text(
                                        "${ExamScoringEngine.formatScore(subj.totalScore)} / 8 (${subj.averagePercentage.toInt()}%)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = glass.accentPrimary
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                CoolingGlassProgressIndicator(
                                    progress = (subj.averagePercentage / 100.0).toFloat()
                                )
                            }
                        }
                    }
                }
            }

            // Language Qualifying Separate Breakdown
            item {
                CoolingGlassCard(tintColor = SoftLavender500) {
                    Text(
                        text = "Language Qualifying Paper Status (Separate)",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Passing threshold: 9.00 / 30 (22 Hindi + 8 English). Qualifying score is displayed separately and never added to GS rank.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill("Hindi (22)", ExamScoringEngine.formatScore(currentMember?.langHindiScore ?: 0.0), glass.accentPrimary, Modifier.weight(1f))
                        StatPill("English (8)", ExamScoringEngine.formatScore(currentMember?.langEnglishScore ?: 0.0), SoftLavender500, Modifier.weight(1f))
                        StatPill("Total (30)", ExamScoringEngine.formatScore(currentMember?.langScore ?: 0.0), glass.accentCyan, Modifier.weight(1f))
                    }
                }
            }

            // Bookmarked Questions Revision Hub
            item {
                CoolingGlassCard(
                    modifier = Modifier.testTag("bookmarks_revision_card"),
                    strongFrost = true,
                    tintColor = glass.accentPrimary
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = glass.accentPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Saved Bookmarks for Revision",
                                style = MaterialTheme.typography.titleMedium,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        CoolingGlassBadge(text = "${bookmarks.size} Saved", color = glass.accentCyan)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (bookmarks.isEmpty()) {
                        Text(
                            text = "Tap the Bookmark icon during any Live Exam or Solution Analysis to save tricky questions for quick revision.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textMuted
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bookmarks.take(25).forEach { bm ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(glass.glassSurface)
                                        .border(1.dp, glass.glassBorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${bm.testTitle} • Q${bm.questionIndex + 1} (${bm.subject})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glass.accentPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.BookmarkRemove,
                                            contentDescription = "Remove Bookmark",
                                            tint = glass.incorrect,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .testTag("remove_bookmark_${bm.id}")
                                                .clickable { onRemoveBookmark(bm) }
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = bm.questionText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (bm.correctOption.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Correct Answer: Option ${bm.correctOption}" +
                                                if (bm.explanation.isNotBlank()) " — ${bm.explanation}" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glass.correct
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }

    // Share Card Dialog (WhatsApp or Any App)
    shareTargetFile?.let { (title, file) ->
        CoolingGlassDialog(title = title, onDismiss = { shareTargetFile = null }) {
            Text(
                text = "Your high-resolution 1080×1350 Cooling Glass PNG card is saved to Pictures/TRE4 and ready to share.",
                style = MaterialTheme.typography.bodyMedium,
                color = glass.textSecondary
            )
            Spacer(Modifier.height(14.dp))
            CoolingGlassButton(
                text = "Share on WhatsApp",
                icon = Icons.Rounded.Share,
                accentGradient = listOf(Color(0xFF16A34A), Color(0xFF059669)),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    RankingCardGenerator.shareImage(context, file, preferWhatsApp = true)
                    shareTargetFile = null
                }
            )
            Spacer(Modifier.height(8.dp))
            CoolingGlassOutlinedButton(
                text = "Share via Other Apps",
                icon = Icons.Rounded.Share,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    RankingCardGenerator.shareImage(context, file, preferWhatsApp = false)
                    shareTargetFile = null
                }
            )
        }
    }

    // Ranker Detail Breakdown Dialog (Tapping ANY ranked User on Podium, Leaderboard, All Scores, or By Part)
    selectedDetailState?.let { (item, detailCategory) ->
        val breakdown = remember(item, detailCategory, selectedPeriod, allAttempts, tests, selectedSetTest) {
            ExamScoringEngine.computeRankerDetailBreakdown(
                entry = item,
                category = detailCategory,
                period = selectedPeriod,
                attempts = allAttempts,
                tests = tests,
                selectedTestId = selectedSetTest?.id
            )
        }
        CoolingGlassDialog(title = item.member.displayName, onDismiss = { selectedDetailState = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("ranker_detail_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoolingGlassAvatar(
                        name = item.member.displayName,
                        photoBase64 = item.member.photoBase64,
                        size = 58.dp,
                        onClick = { zoomedDetailAvatar = item }
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        CoolingGlassBadge(
                            text = "Rank #${breakdown.rank} • ${breakdown.categoryLabel}",
                            color = glass.accentPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${item.member.appRole().labelEn} • ${breakdown.periodLabel} • ${breakdown.attemptStatusLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                    }
                }

                // Total Marks, Percentage, Time Taken, Attempts
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatPill(
                        "Total Marks",
                        if (breakdown.testsAttempted > 0) {
                            "${ExamScoringEngine.formatScore(breakdown.totalScore)}/${breakdown.maxMarks.toInt()}"
                        } else "—",
                        glass.accentPrimary,
                        Modifier.weight(1f)
                    )
                    StatPill(
                        "Percentage",
                        if (breakdown.testsAttempted > 0) String.format(Locale.US, "%.1f%%", breakdown.percentage) else "—",
                        SoftLavender500,
                        Modifier.weight(1f)
                    )
                    StatPill(
                        "Time",
                        if (breakdown.testsAttempted > 0) formatDurationCompact(breakdown.timeSpentSeconds) else "—",
                        glass.accentCyan,
                        Modifier.weight(1f)
                    )
                }

                // Correct / Incorrect / Unattempted Counts
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatPill("Correct", "${breakdown.correctCount}", glass.correct, Modifier.weight(1f))
                    StatPill("Incorrect", "${breakdown.incorrectCount}", glass.incorrect, Modifier.weight(1f))
                    StatPill("Unattempted", "${breakdown.unattemptedCount}", glass.review, Modifier.weight(1f))
                }

                // Section-wise Performance Breakdown
                if (breakdown.sectionStats.isNotEmpty()) {
                    Text(
                        text = "Section-Wise Performance",
                        style = MaterialTheme.typography.titleSmall,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    breakdown.sectionStats.forEach { sec ->
                        val hasCounts = sec.correctCount + sec.incorrectCount + sec.unattemptedCount > 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(glass.glassSurface)
                                .border(0.8.dp, glass.glassBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sec.sectionName, style = MaterialTheme.typography.bodySmall, color = glass.textPrimary, fontWeight = FontWeight.SemiBold)
                                if (hasCounts) {
                                    Text(
                                        text = "✓${sec.correctCount} Correct • ✕${sec.incorrectCount} Wrong • ○${sec.unattemptedCount} Unattempted",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.textSecondary
                                    )
                                }
                            }
                            CoolingGlassBadge(
                                text = "${ExamScoringEngine.formatScore(sec.marks)}/${sec.maxMarks.toInt().coerceAtLeast(1)}",
                                color = glass.accentCyan
                            )
                        }
                    }
                }

                // Subject-wise Marks & Number of Correct Questions
                if (breakdown.subjectStats.isNotEmpty()) {
                    Text(
                        text = "Subject-Wise Marks & Correct Questions",
                        style = MaterialTheme.typography.titleSmall,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    breakdown.subjectStats.forEach { stat ->
                        val correctLabel = if (stat.totalQuestions > 0) {
                            "${stat.correctCount}/${stat.totalQuestions} correct"
                        } else {
                            "${stat.correctCount} correct"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(glass.accentPrimary.copy(alpha = 0.08f))
                                .border(0.8.dp, glass.glassBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stat.subject,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = correctLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.correct,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            CoolingGlassBadge(
                                text = "${ExamScoringEngine.formatScore(stat.marks)} marks",
                                color = glass.accentPrimary
                            )
                        }
                    }
                }

                val detailLang = ExamScoringEngine.setLanguageBadgeText(item)
                if (!detailLang.isNullOrBlank()) {
                    Text(
                        text = "Language Qualifying: $detailLang",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    zoomedDetailAvatar?.let { u ->
        ProfilePhotoZoomDialog(
            name = u.member.displayName,
            photoBase64 = u.member.photoBase64,
            subtitle = "${u.member.appRole().labelEn} • Rank #${u.rank}",
            onDismiss = { zoomedDetailAvatar = null }
        )
    }
}

private fun rankingPeriodIconAndLabel(period: RankingPeriod): Pair<ImageVector, String> = when (period) {
    RankingPeriod.INDIVIDUAL_TEST -> Icons.Rounded.FormatListNumbered to "Set"
    RankingPeriod.DAILY -> Icons.Rounded.Today to "Daily"
    RankingPeriod.WEEKLY -> Icons.Rounded.DateRange to "Weekly"
    RankingPeriod.MONTHLY -> Icons.Rounded.CalendarMonth to "Monthly"
    RankingPeriod.OVERALL -> Icons.Rounded.EmojiEvents to "Overall"
}

@Composable
private fun CrystalPodiumCard(
    topPerformers: List<RankedAspirant>,
    onClickAspirant: (RankedAspirant) -> Unit
) {
    val glass = LocalCoolingGlass.current
    val first = topPerformers.getOrNull(0) ?: return
    val second = topPerformers.getOrNull(1)
    val third = topPerformers.getOrNull(2)

    CoolingGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("celebration_podium_card"),
        strongFrost = true,
        tintColor = RankGold,
        borderColor = RankGold.copy(alpha = 0.72f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Warm golden celebration radial spotlight behind Rank #1
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                RankGold.copy(alpha = 0.28f),
                                IceCyan500.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.38f),
                            radius = size.width * 0.62f
                        ),
                        radius = size.width * 0.62f,
                        center = Offset(size.width * 0.5f, size.height * 0.38f)
                    )
                    // Festive confetti dots & tilted ribbons
                    val confettiPalette = listOf(
                        RankGold,
                        IceCyan500,
                        SoftLavender500,
                        Color(0xFF10B981),
                        Color(0xFFF43F5E)
                    )
                    val dotCoords = listOf(
                        0.08f to 0.14f, 0.18f to 0.26f, 0.28f to 0.10f,
                        0.72f to 0.12f, 0.84f to 0.24f, 0.92f to 0.16f,
                        0.12f to 0.52f, 0.88f to 0.48f, 0.38f to 0.22f,
                        0.63f to 0.20f
                    )
                    dotCoords.forEachIndexed { idx, (fx, fy) ->
                        val c = confettiPalette[idx % confettiPalette.size]
                        val center = Offset(size.width * fx, size.height * fy)
                        if (idx % 2 == 0) {
                            drawCircle(color = c.copy(alpha = 0.48f), radius = if (idx % 3 == 0) 4.5.dp.toPx() else 3.dp.toPx(), center = center)
                        } else {
                            drawRoundRect(
                                color = c.copy(alpha = 0.42f),
                                topLeft = Offset(center.x - 5.dp.toPx(), center.y - 2.dp.toPx()),
                                size = Size(10.dp.toPx(), 4.dp.toPx()),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                    }
                    // Subtle 4-point celebration sparkles near the crown
                    val sparkleCenters = listOf(
                        Offset(size.width * 0.36f, size.height * 0.31f),
                        Offset(size.width * 0.64f, size.height * 0.30f),
                        Offset(size.width * 0.50f, size.height * 0.18f)
                    )
                    sparkleCenters.forEach { sp ->
                        val arm = 6.dp.toPx()
                        drawLine(RankGold.copy(alpha = 0.72f), Offset(sp.x - arm, sp.y), Offset(sp.x + arm, sp.y), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(RankGold.copy(alpha = 0.72f), Offset(sp.x, sp.y - arm), Offset(sp.x, sp.y + arm), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Celebratory Header Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(RankGold.copy(alpha = 0.22f))
                                .border(1.dp, RankGold.copy(alpha = 0.75f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Celebration,
                                contentDescription = "Celebration",
                                tint = RankGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Congratulations Top Performers!",
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = appText("Top Performers • Group Top Rankers", "बधाई हो • Group Top Rankers"),
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textSecondary
                            )
                        }
                    }
                    CoolingGlassBadge(text = "TOP ${topPerformers.size}", color = RankGold)
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (second != null) {
                        PodiumColumn(
                            entry = second,
                            medalColor = RankSilver,
                            pedestalHeight = 62.dp,
                            badgeTitle = "2ND",
                            isChampion = false,
                            onClick = { onClickAspirant(second) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(0.6f))
                    }

                    PodiumColumn(
                        entry = first,
                        medalColor = RankGold,
                        pedestalHeight = 86.dp,
                        badgeTitle = "CHAMPION",
                        isChampion = true,
                        onClick = { onClickAspirant(first) },
                        modifier = Modifier.weight(1.18f)
                    )

                    if (third != null) {
                        PodiumColumn(
                            entry = third,
                            medalColor = RankBronze,
                            pedestalHeight = 48.dp,
                            badgeTitle = "3RD",
                            isChampion = false,
                            onClick = { onClickAspirant(third) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    entry: RankedAspirant,
    medalColor: Color,
    pedestalHeight: androidx.compose.ui.unit.Dp,
    badgeTitle: String,
    isChampion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalCoolingGlass.current
    Column(
        modifier = modifier
            .testTag("podium_rank_${entry.rank}")
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isChampion) Icons.Rounded.WorkspacePremium else Icons.Rounded.MilitaryTech,
            contentDescription = "Rank ${entry.rank} Trophy",
            tint = medalColor,
            modifier = Modifier.size(if (isChampion) 26.dp else 20.dp)
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .border(
                    width = if (isChampion) 2.5.dp else 1.5.dp,
                    color = medalColor,
                    shape = CircleShape
                )
                .padding(3.dp)
        ) {
            CoolingGlassAvatar(
                name = entry.member.displayName,
                photoBase64 = entry.member.photoBase64,
                size = if (isChampion) 54.dp else 44.dp
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = entry.member.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = glass.textPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = ExamScoringEngine.formatScore(entry.displayScore),
            style = MaterialTheme.typography.labelMedium,
            color = if (isChampion) RankGold else glass.accentPrimary,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            medalColor.copy(alpha = if (isChampion) 0.42f else 0.28f),
                            medalColor.copy(alpha = 0.14f)
                        )
                    )
                )
                .border(
                    width = if (isChampion) 1.5.dp else 1.dp,
                    color = medalColor.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "#${entry.rank}",
                    style = if (isChampion) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    color = medalColor,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (isChampion) "CONGRATS!" else badgeTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textPrimary.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
