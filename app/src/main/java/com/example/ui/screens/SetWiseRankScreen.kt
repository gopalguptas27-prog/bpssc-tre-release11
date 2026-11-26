package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.model.ExamTest
import com.example.data.model.RankCategory
import com.example.data.model.SetRankEntry
import com.example.domain.ExamScoringEngine
import com.example.domain.SetRankerSubjectStat
import com.example.ui.glass.CoolingGlassAvatar
import com.example.ui.glass.CoolingGlassBackground
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassBottomSheet
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassEmptyState
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.CoolingGlassSearchBar
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
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Dedicated Set-Wise Rank Screen.
 * - Strictly bound to a single [test] (`test.id`).
 * - Displays EVERY user who attempted/submitted this exact set with ZERO cap (150, 300, 1000+).
 * - Never mixes global/overall rankings or attempts from any other set.
 * - Pressing Back returns directly to the originating Set Session screen with state intact.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetWiseRankScreen(
    test: ExamTest,
    entries: List<SetRankEntry>,
    currentUserId: String,
    isLoading: Boolean = false,
    onBack: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedRanker by remember { mutableStateOf<SetRankEntry?>(null) }

    BackHandler {
        if (selectedRanker != null) {
            selectedRanker = null
        } else {
            onBack()
        }
    }

    // Defensive strict guard: keep ONLY entries matching this exact test.id
    val strictEntries = remember(entries, test.id) {
        entries.filter { it.testId == test.id }
    }

    val filteredEntries = remember(strictEntries, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) strictEntries
        else strictEntries.filter {
            it.studentName.contains(q, ignoreCase = true) ||
                "#${it.rank}".contains(q, ignoreCase = true)
        }
    }

    val totalParticipants = strictEntries.size
    val myEntry = remember(strictEntries, currentUserId) {
        strictEntries.firstOrNull { it.userId == currentUserId }
    }
    val highestScore = remember(strictEntries) { strictEntries.maxOfOrNull { it.score } }
    val avgScore = remember(strictEntries) {
        if (strictEntries.isEmpty()) null else strictEntries.map { it.score }.average()
    }
    val setNumber = remember(test) { test.resolvedSetNumber() }
    val rankCategory = remember(test) { RankCategory.fromTest(test) }
    val diffColor = when (test.difficulty) {
        "HARD" -> glass.incorrect
        "EASY" -> glass.correct
        else -> glass.accentPrimary
    }

    CoolingGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("set_wise_rank_screen")
        ) {
            // Top Bar with Back Navigation & Exact Set Name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(glass.glassSurfaceStrong)
                    .border(0.8.dp, glass.glassBorderSubtle)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("set_wise_rank_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Set Session",
                        tint = glass.textPrimary
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = test.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("set_wise_rank_title")
                    )
                    Text(
                        text = buildString {
                            if (setNumber != null) append("Set $setNumber • ")
                            append("Set-Wise Rank Leaderboard • IST")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.accentPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                CoolingGlassBadge(
                    text = "$totalParticipants Users",
                    color = glass.accentPrimary,
                    modifier = Modifier.testTag("set_wise_rank_header_count")
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("set_wise_rank_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Set Metadata & Total Participants Summary Card
                item(key = "set_rank_summary_header") {
                    CoolingGlassCard(
                        modifier = Modifier.testTag("set_wise_rank_summary_card"),
                        strongFrost = true,
                        tintColor = diffColor,
                        borderColor = diffColor.copy(alpha = 0.55f),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (setNumber != null) {
                                CoolingGlassBadge(text = "Set $setNumber - Rank", color = GlacierBlue600)
                            }
                            CoolingGlassBadge(text = test.difficulty, color = diffColor)
                            if (rankCategory != null) {
                                CoolingGlassBadge(text = rankCategory.displayName, color = SoftLavender500)
                            }
                            CoolingGlassBadge(
                                text = "${test.questionCount} Q • ${test.durationMinutes} min",
                                color = IceCyan500
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatPill(
                                label = "Total Participants",
                                value = "$totalParticipants",
                                color = glass.accentPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("set_wise_rank_total_participants")
                            )
                            StatPill(
                                label = "Your Rank",
                                value = myEntry?.let { "#${it.rank}/$totalParticipants" } ?: "—",
                                color = SoftLavender500,
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                label = "Highest",
                                value = highestScore?.let { ExamScoringEngine.formatScore(it) } ?: "—",
                                color = glass.correct,
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                label = "Average",
                                value = avgScore?.let { ExamScoringEngine.formatScore(it) } ?: "—",
                                color = glass.accentCyan,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (myEntry != null && searchQuery.isBlank()) {
                            Spacer(Modifier.height(10.dp))
                            CoolingGlassOutlinedButton(
                                text = "Jump to My Rank (#${myEntry.rank})",
                                icon = Icons.Rounded.MyLocation,
                                testTagName = "jump_to_my_set_rank_btn",
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val targetIdx = strictEntries.indexOfFirst { it.userId == currentUserId }
                                    if (targetIdx >= 0) {
                                        scope.launch {
                                            // Offset by 3 header items (summary card + search bar + participant count caption)
                                            listState.animateScrollToItem(targetIdx + 3)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Search Student in this Set
                item(key = "set_rank_search_bar") {
                    CoolingGlassSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search student name or #rank across all $totalParticipants participants...",
                        modifier = Modifier.testTag("set_wise_rank_search")
                    )
                }

                if (isLoading && strictEntries.isEmpty()) {
                    item(key = "set_rank_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = glass.accentPrimary)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Loading all participants for ${test.title}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                        }
                    }
                } else if (filteredEntries.isEmpty()) {
                    item(key = "set_rank_empty") {
                        CoolingGlassEmptyState(
                            icon = if (strictEntries.isEmpty()) Icons.Rounded.EmojiEvents else Icons.Rounded.Groups,
                            title = if (strictEntries.isEmpty()) "No Submissions Yet" else "No Matching Student",
                            subtitle = if (strictEntries.isEmpty()) {
                                "No user has submitted '${test.title}' yet. Complete this set to be the first on its leaderboard."
                            } else {
                                "No participant in '${test.title}' matches \"$searchQuery\"."
                            }
                        )
                    }
                } else {
                    item(key = "set_rank_count_caption") {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "Showing all $totalParticipants participants (Ranked highest score first)"
                                } else {
                                    "Showing ${filteredEntries.size} of $totalParticipants participants"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("set_wise_rank_showing_label")
                            )
                            Text(
                                text = appText(
                                    en = "Tap any student to view separate Subject-wise Marks, Right, Wrong & Not Attempted",
                                    defaultText = "किसी भी रैंकर पर क्लिक करें — विषयवार नंबर, सही, गलत व नॉट अटेम्प्ट देखें"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.accentPrimary
                            )
                        }
                    }

                    // Complete virtualized list of ALL participants — ZERO fixed cap (150, 300, 1000+)
                    items(
                        items = filteredEntries,
                        key = { "${it.testId}_${it.userId}" }
                    ) { entry ->
                        SetWiseRankRowCard(
                            entry = entry,
                            isCurrentUser = entry.userId == currentUserId,
                            onClick = { selectedRanker = entry }
                        )
                    }

                    item(key = "set_rank_bottom_spacer") {
                        Spacer(
                            modifier = Modifier
                                .height(24.dp)
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }

        selectedRanker?.let { activeEntry ->
            SetRankerSubjectBreakdownSheet(
                test = test,
                entry = activeEntry,
                onDismiss = { selectedRanker = null }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetWiseRankRowCard(
    entry: SetRankEntry,
    isCurrentUser: Boolean,
    onClick: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    val rankBadgeColor = when (entry.rank) {
        1 -> RankGold
        2 -> RankSilver
        3 -> RankBronze
        else -> glass.accentPrimary
    }
    val isTop3 = entry.rank in 1..3
    val timeTakenStr = formatSetRankDuration(entry.timeSpentSeconds)
    val submittedIstStr = if (entry.submittedAtEpochMillis > 0L) {
        IstClock.formatDateTime(entry.submittedAtEpochMillis)
    } else {
        "Submitted (IST)"
    }
    val maxMarksLabel = if (entry.totalMarks % 1.0 == 0.0) {
        entry.totalMarks.toInt().toString()
    } else {
        ExamScoringEngine.formatScore(entry.totalMarks)
    }

    CoolingGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("set_rank_row_${entry.userId}"),
        strongFrost = isCurrentUser || isTop3,
        tintColor = when {
            isCurrentUser -> glass.accentPrimary
            isTop3 -> rankBadgeColor
            else -> null
        },
        borderColor = when {
            isCurrentUser -> glass.accentCyan
            isTop3 -> rankBadgeColor.copy(alpha = 0.7f)
            else -> null
        },
        contentPadding = PaddingValues(12.dp),
        onClick = onClick
    ) {
        // Row 1: Rank Position + Avatar + Student Name + Score / Total Marks & Percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isTop3) {
                            Brush.linearGradient(listOf(rankBadgeColor, rankBadgeColor.copy(alpha = 0.75f)))
                        } else {
                            Brush.linearGradient(listOf(glass.accentPrimary.copy(alpha = 0.16f), glass.accentCyan.copy(alpha = 0.12f)))
                        }
                    )
                    .border(
                        width = if (isTop3) 1.5.dp else 1.dp,
                        color = if (isTop3) CrystalWhite.copy(alpha = 0.8f) else glass.glassBorder,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isTop3) CrystalWhite else glass.accentPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.width(10.dp))

            CoolingGlassAvatar(
                name = entry.studentName,
                photoBase64 = entry.photoBase64,
                size = 38.dp
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.studentName,
                        style = MaterialTheme.typography.titleSmall,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrentUser) {
                        CoolingGlassBadge(text = "YOU", color = glass.accentPrimary)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = "Submission Date and Time",
                        tint = glass.textMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = submittedIstStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${ExamScoringEngine.formatScore(entry.score)} / $maxMarksLabel",
                    style = MaterialTheme.typography.titleSmall,
                    color = glass.accentPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = String.format(Locale.US, "%.1f%%", entry.percentage),
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.correct,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Row 2: Correct, Incorrect, Unattempted, Time Taken (+ optional Language Qualifying status)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MetricChip(
                icon = Icons.Rounded.CheckCircle,
                text = "Correct: ${entry.correctCount}",
                color = glass.correct
            )
            MetricChip(
                icon = Icons.Rounded.Cancel,
                text = "Wrong: ${entry.incorrectCount}",
                color = glass.incorrect
            )
            MetricChip(
                icon = Icons.Rounded.RemoveCircleOutline,
                text = "Unattempted: ${entry.unattemptedCount}",
                color = glass.textSecondary
            )
            MetricChip(
                icon = Icons.Rounded.AccessTime,
                text = timeTakenStr,
                color = IceCyan500
            )
            if (entry.showLanguageBadge && entry.languageScore != null) {
                val pass = entry.languagePassed == true
                CoolingGlassBadge(
                    text = "Lang: ${ExamScoringEngine.formatScore(entry.languageScore)}/30 ${if (pass) "PASS" else "FAIL"}",
                    color = if (pass) glass.correct else glass.incorrect
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appText("Subject-wise Marks & Report", "विषयवार नंबर व विवरण देखें"),
                style = MaterialTheme.typography.labelSmall,
                color = glass.accentPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Open subject-wise report",
                tint = glass.accentPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SetRankerSubjectBreakdownSheet(
    test: ExamTest,
    entry: SetRankEntry,
    onDismiss: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    val subjectStats = remember(test, entry) {
        ExamScoringEngine.computeSetRankerSubjectBreakdown(test, entry)
    }
    val maxMarksLabel = if (entry.totalMarks % 1.0 == 0.0) {
        entry.totalMarks.toInt().toString()
    } else {
        ExamScoringEngine.formatScore(entry.totalMarks)
    }
    val timeTakenStr = formatSetRankDuration(entry.timeSpentSeconds)
    val submittedIstStr = if (entry.submittedAtEpochMillis > 0L) {
        IstClock.formatDateTime(entry.submittedAtEpochMillis)
    } else {
        "Submitted (IST)"
    }

    CoolingGlassBottomSheet(
        title = appText(
            en = "#${entry.rank} ${entry.studentName} • Subject Report",
            defaultText = "#${entry.rank} ${entry.studentName} • विषयवार रिपोर्ट"
        ),
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .testTag("set_ranker_subject_breakdown_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ranker Summary Header
            CoolingGlassCard(
                strongFrost = true,
                tintColor = glass.accentPrimary,
                borderColor = glass.accentPrimary.copy(alpha = 0.5f),
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoolingGlassAvatar(
                        name = entry.studentName,
                        photoBase64 = entry.photoBase64,
                        size = 44.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.studentName,
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${test.title} • $timeTakenStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = submittedIstStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textMuted
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${ExamScoringEngine.formatScore(entry.score)} / $maxMarksLabel",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.accentPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%%", entry.percentage),
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.correct,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatPill(
                        label = appText("Rank", "रैंक"),
                        value = "#${entry.rank}",
                        color = glass.accentPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        label = appText("Right", "सही"),
                        value = "${entry.correctCount}",
                        color = glass.correct,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        label = appText("Wrong", "गलत"),
                        value = "${entry.incorrectCount}",
                        color = glass.incorrect,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        label = appText("Not Attempted", "नॉट अटेम्प्ट"),
                        value = "${entry.unattemptedCount}",
                        color = glass.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Assessment,
                    contentDescription = null,
                    tint = glass.accentPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = appText(
                        en = "Separate Subject-Wise Breakdown (${subjectStats.size})",
                        defaultText = "विषयवार अलग-अलग नंबर, सही, गलत व नॉट अटेम्प्ट (${subjectStats.size})"
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (subjectStats.isEmpty()) {
                CoolingGlassEmptyState(
                    icon = Icons.Rounded.Assessment,
                    title = appText("Subject Breakdown Unavailable", "विषयवार विवरण उपलब्ध नहीं"),
                    subtitle = appText(
                        en = "Detailed subject-wise answer records were not saved for this older submission.",
                        defaultText = "इस पुराने सबमिशन में विषयवार उत्तर रिकॉर्ड सहेजा नहीं गया था।"
                    )
                )
            } else {
                subjectStats.forEach { stat ->
                    SubjectBreakdownRowCard(stat = stat)
                }
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun SubjectBreakdownRowCard(stat: SetRankerSubjectStat) {
    val glass = LocalCoolingGlass.current
    val marksStr = ExamScoringEngine.formatScore(stat.marks)
    val maxMarksStr = if (stat.maxMarks > 0.0) {
        if (stat.maxMarks % 1.0 == 0.0) stat.maxMarks.toInt().toString()
        else ExamScoringEngine.formatScore(stat.maxMarks)
    } else null
    val marksDisplay = if (maxMarksStr != null) "$marksStr / $maxMarksStr" else marksStr
    val accent = if (stat.isQualifying) SoftLavender500 else GlacierBlue600

    CoolingGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_breakdown_row_${stat.subject}"),
        strongFrost = false,
        tintColor = accent,
        borderColor = accent.copy(alpha = 0.4f),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stat.subject,
                        style = MaterialTheme.typography.titleSmall,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    CoolingGlassBadge(
                        text = if (stat.isQualifying) {
                            appText("Qualifying", "क्वालिफाइंग")
                        } else {
                            appText("Merit", "मेरिट")
                        },
                        color = accent
                    )
                }
                if (stat.totalQuestions > 0) {
                    Text(
                        text = appText(
                            en = "${stat.totalQuestions} Questions",
                            defaultText = "${stat.totalQuestions} प्रश्न"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = appText("Marks", "नंबर (Marks)"),
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textMuted
                )
                Text(
                    text = marksDisplay,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (stat.marks >= 0.0) glass.accentPrimary else glass.incorrect,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SubjectCountCell(
                label = appText("Right", "सही (Right)"),
                value = if (stat.hasDetailedCounts) "${stat.rightCount}" else "—",
                color = glass.correct,
                modifier = Modifier.weight(1f)
            )
            SubjectCountCell(
                label = appText("Wrong", "गलत (Wrong)"),
                value = if (stat.hasDetailedCounts) "${stat.wrongCount}" else "—",
                color = glass.incorrect,
                modifier = Modifier.weight(1f)
            )
            SubjectCountCell(
                label = appText("Not Attempted", "नॉट अटेम्प्ट"),
                value = if (stat.hasDetailedCounts) "${stat.notAttemptedCount}" else "—",
                color = glass.textSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SubjectCountCell(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.11f))
            .border(0.8.dp, color.copy(alpha = 0.34f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.9f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.8.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

private fun formatSetRankDuration(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val hrs = safe / 3600L
    val mins = (safe % 3600L) / 60L
    val secs = safe % 60L
    return when {
        hrs > 0L -> String.format(Locale.US, "%dh %02dm %02ds", hrs, mins, secs)
        mins > 0L -> String.format(Locale.US, "%dm %02ds", mins, secs)
        else -> "${secs}s"
    }
}
