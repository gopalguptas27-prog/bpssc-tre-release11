package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ExamTest
import com.example.data.model.GroupMember
import com.example.data.model.LiveExam
import com.example.data.model.LiveExamDynamicStatus
import com.example.data.model.LiveExamSession
import com.example.data.model.RankCategory
import com.example.data.model.TestAttempt
import com.example.domain.ExamScoringEngine
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassDialog
import com.example.ui.glass.CoolingGlassEmptyState
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.CoolingGlassSearchBar
import com.example.ui.glass.CoolingGlassTabRow
import com.example.ui.glass.CoolingGlassTextField
import com.example.ui.glass.StatPill
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.GlacierBlue600
import com.example.ui.theme.IceCyan400
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.SoftLavender500
import com.example.util.IstClock
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatLiveCountdown(remainingMillis: Long): String {
    val totalSec = (remainingMillis / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0) {
        String.format(Locale.US, "%02dh %02dm %02ds", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02dm %02ds", mins, secs)
    }
}

private fun statusBadgeColor(status: LiveExamDynamicStatus): Color = when (status) {
    LiveExamDynamicStatus.LIVE_NOW -> Color(0xFFDC2626)
    LiveExamDynamicStatus.STARTING_SOON -> Color(0xFFD97706)
    LiveExamDynamicStatus.REGISTRATION_OPEN -> Color(0xFF059669)
    LiveExamDynamicStatus.UPCOMING -> GlacierBlue600
    LiveExamDynamicStatus.REGISTRATION_CLOSED -> SoftLavender500
    LiveExamDynamicStatus.COMPLETED -> Color(0xFF475569)
    LiveExamDynamicStatus.CANCELLED -> Color(0xFF991B1B)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentLiveExamHubScreen(
    liveExams: List<LiveExam>,
    liveExamSessions: List<LiveExamSession>,
    tests: List<ExamTest>,
    attempts: List<TestAttempt>,
    currentUserId: String,
    isAdmin: Boolean,
    onRegisterLiveExam: (LiveExam, Boolean) -> Unit,
    onStartLiveExam: (LiveExam) -> Unit,
    onViewLiveExamResult: (ExamTest, TestAttempt) -> Unit,
    onOpenLiveExamRank: (ExamTest) -> Unit,
    onOpenAdminLiveExams: () -> Unit = {}
) {
    val glass = LocalCoolingGlass.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<RankCategory?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active & Upcoming, 1 = Completed & Past
    var waitingRoomExamId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val publishedExams = remember(liveExams) {
        liveExams.filter { it.status == "PUBLISHED" || it.status == "CANCELLED" }
    }

    val userSessionsByExamId = remember(liveExamSessions, currentUserId) {
        liveExamSessions
            .filter { it.userId == currentUserId }
            .associateBy { it.liveExamId }
    }

    val filteredExams = remember(publishedExams, searchQuery, selectedCategory, selectedTab, nowMillis, tests) {
        val q = searchQuery.trim().lowercase()
        publishedExams.filter { exam ->
            val dynStatus = exam.dynamicStatus(nowMillis)
            val matchesTab = if (selectedTab == 0) {
                dynStatus != LiveExamDynamicStatus.COMPLETED && dynStatus != LiveExamDynamicStatus.CANCELLED
            } else {
                dynStatus == LiveExamDynamicStatus.COMPLETED || dynStatus == LiveExamDynamicStatus.CANCELLED
            }
            val examRankCat = RankCategory.fromCategoryString(exam.category)
                ?: tests.firstOrNull { it.id == exam.sourceSetId }?.let { RankCategory.fromTest(it) }
            val matchesCat = selectedCategory == null || examRankCat == selectedCategory
            val matchesQuery = q.isBlank() ||
                exam.title.lowercase().contains(q) ||
                exam.sourceSetTitle.lowercase().contains(q) ||
                exam.category.lowercase().contains(q)
            matchesTab && matchesCat && matchesQuery
        }.sortedBy { it.startTimeEpochMillis }
    }

    val activeWaitingExam = remember(waitingRoomExamId, publishedExams) {
        publishedExams.firstOrNull { it.id == waitingRoomExamId }
    }

    // Auto-start when Waiting Room countdown reaches official start time
    LaunchedEffect(activeWaitingExam?.id, nowMillis) {
        val exam = activeWaitingExam ?: return@LaunchedEffect
        if (nowMillis >= exam.startTimeEpochMillis && nowMillis < exam.endTimeEpochMillis && exam.status == "PUBLISHED") {
            waitingRoomExamId = null
            onStartLiveExam(exam)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_live_exam_hub_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CoolingGlassCard(strongFrost = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Podcasts,
                            contentDescription = "Live Exam Broadcast",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TRE 4.0 Live Exam Arena",
                                style = MaterialTheme.typography.titleLarge,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "IST Clock: ${IstClock.formatTimeWithSeconds(nowMillis)} • Synchronized All-India Schedule",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.accentPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (isAdmin) {
                        CoolingGlassOutlinedButton(
                            text = "Admin Scheduler",
                            icon = Icons.Rounded.Tune,
                            testTagName = "open_admin_live_exam_scheduler_btn",
                            onClick = onOpenAdminLiveExams
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                CoolingGlassTabRow(
                    tabs = listOf(
                        "Live & Upcoming (${publishedExams.count { val s = it.dynamicStatus(nowMillis); s != LiveExamDynamicStatus.COMPLETED && s != LiveExamDynamicStatus.CANCELLED }})",
                        "Completed & Past (${publishedExams.count { val s = it.dynamicStatus(nowMillis); s == LiveExamDynamicStatus.COMPLETED || s == LiveExamDynamicStatus.CANCELLED }})"
                    ),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it }
                )
            }
        }

        item {
            CoolingGlassSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search live exams by title, set, or category..."
            )
        }

        // 7 Rank Categories Filter Bar
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_exam_category_filter_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val allSelected = selectedCategory == null
                    Box(
                        modifier = Modifier
                            .testTag("live_cat_filter_ALL")
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (allSelected) GlacierBlue600 else glass.glassSurfaceStrong)
                            .border(
                                1.dp,
                                if (allSelected) IceCyan400 else glass.glassBorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCategory = null }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "All Categories",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (allSelected) CrystalWhite else glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(RankCategory.entries) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .testTag("live_cat_filter_${cat.name}")
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) GlacierBlue600 else glass.glassSurfaceStrong)
                            .border(
                                1.dp,
                                if (isSelected) IceCyan400 else glass.glassBorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCategory = if (isSelected) null else cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) CrystalWhite else glass.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (filteredExams.isEmpty()) {
            item {
                CoolingGlassEmptyState(
                    icon = Icons.Rounded.Podcasts,
                    title = if (selectedTab == 0) "No Active or Upcoming Live Exams" else "No Past Live Exams",
                    subtitle = if (isAdmin) {
                        "Tap 'Admin Scheduler' above to link an existing question set and publish a scheduled Live Exam."
                    } else {
                        "Scheduled All-India Live Exams published by the Admin will appear here with real-time IST countdowns."
                    }
                )
            }
        } else {
            items(filteredExams, key = { it.id }) { exam ->
                val dynStatus = exam.dynamicStatus(nowMillis)
                val userSession = userSessionsByExamId[exam.id]
                val linkedTest = tests.firstOrNull { it.id == exam.sourceSetId }
                val userAttempt = attempts
                    .filter { it.userId == currentUserId && it.testId == exam.sourceSetId }
                    .maxByOrNull { it.submittedAt?.toDate()?.time ?: 0L }
                val isRegistered = userSession != null
                val attemptSubmitMillis = userAttempt?.submittedAt?.toDate()?.time ?: 0L
                val hasSubmitted = userSession?.status == "SUBMITTED" ||
                    userSession?.status == "APPROVED" ||
                    (userAttempt != null && attemptSubmitMillis >= exam.startTimeEpochMillis)
                val isDisqualified = userSession?.status == "DISQUALIFIED"

                CoolingGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_exam_card_${exam.id}"),
                    strongFrost = dynStatus == LiveExamDynamicStatus.LIVE_NOW
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CoolingGlassBadge(
                                    text = dynStatus.badgeLabel,
                                    color = statusBadgeColor(dynStatus)
                                )
                                CoolingGlassBadge(
                                    text = exam.resolvedRankCategory().displayName,
                                    color = glass.accentPrimary
                                )
                                if (exam.secureModeEnabled) {
                                    CoolingGlassBadge(
                                        text = "SECURE MODE",
                                        color = Color(0xFFDC2626)
                                    )
                                }
                                if (isRegistered) {
                                    CoolingGlassBadge(
                                        text = if (hasSubmitted) "SUBMITTED" else "REGISTERED",
                                        color = glass.correct
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = exam.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Linked Set: ${exam.sourceSetTitle.ifBlank { linkedTest?.title ?: exam.sourceSetId }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textSecondary
                            )
                            val desc = exam.description.orEmpty()
                            if (desc.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CoolingGlassBadge("${exam.totalQuestions} Qs • ${exam.totalMarks.toInt()} Marks", GlacierBlue600)
                        CoolingGlassBadge("${exam.durationMinutes} Min Common Window", SoftLavender500)
                        CoolingGlassBadge("Neg: ${exam.negativeMarkLabel()}", glass.correct)
                        CoolingGlassBadge(
                            if (exam.eligibilityType == "REGISTERED_ONLY") "Pre-Registered Only" else "Open to All Aspirants",
                            glass.accentCyan
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(glass.glassSurfaceStrong)
                            .border(1.dp, glass.glassBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Start (IST): ${IstClock.formatDateTime(exam.startTimeEpochMillis)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Common End (IST): ${IstClock.formatDateTime(exam.endTimeEpochMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                        val countdownLabel = when (dynStatus) {
                            LiveExamDynamicStatus.LIVE_NOW ->
                                "LIVE NOW • Common Auto-Submit in ${formatLiveCountdown(exam.endTimeEpochMillis - nowMillis)}"
                            LiveExamDynamicStatus.STARTING_SOON,
                            LiveExamDynamicStatus.REGISTRATION_OPEN,
                            LiveExamDynamicStatus.REGISTRATION_CLOSED,
                            LiveExamDynamicStatus.UPCOMING ->
                                "Starts in: ${formatLiveCountdown(exam.startTimeEpochMillis - nowMillis)}"
                            LiveExamDynamicStatus.COMPLETED ->
                                if (exam.isResultsReleased(nowMillis)) {
                                    "Results & Solutions Published"
                                } else {
                                    "Results Publish at ${IstClock.formatDateTime(exam.resultPublishEpochMillis)}"
                                }
                            LiveExamDynamicStatus.CANCELLED -> "Cancelled by Admin"
                        }
                        Text(
                            text = countdownLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (dynStatus == LiveExamDynamicStatus.LIVE_NOW) Color(0xFFDC2626) else glass.accentPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    when {
                        isDisqualified -> {
                            CoolingGlassBadge(
                                text = "Disqualified by Admin (${userSession.adminModerationNote?.ifBlank { "Security Policy Violation" } ?: "Security Policy Violation"})",
                                color = glass.incorrect,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        hasSubmitted -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (exam.isResultsReleased(nowMillis) && linkedTest != null && userAttempt != null) {
                                    CoolingGlassButton(
                                        text = "View Result & Solutions",
                                        icon = Icons.Rounded.Visibility,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onViewLiveExamResult(linkedTest, userAttempt) }
                                    )
                                } else {
                                    CoolingGlassOutlinedButton(
                                        text = "Submitted • Results at ${IstClock.formatTimeOnly(exam.resultPublishEpochMillis)}",
                                        icon = Icons.Rounded.Lock,
                                        modifier = Modifier.weight(1f),
                                        onClick = {}
                                    )
                                }
                                if (linkedTest != null) {
                                    CoolingGlassOutlinedButton(
                                        text = "Rank",
                                        icon = Icons.Rounded.EmojiEvents,
                                        onClick = { onOpenLiveExamRank(linkedTest) }
                                    )
                                }
                            }
                        }
                        dynStatus == LiveExamDynamicStatus.LIVE_NOW -> {
                            val canEnterLive = exam.canEnterLiveNow(isRegistered, nowMillis)
                            if (canEnterLive) {
                                CoolingGlassButton(
                                    text = "Start Live Exam Now (${formatLiveCountdown(exam.endTimeEpochMillis - nowMillis)} left)",
                                    icon = Icons.Rounded.PlayArrow,
                                    accentGradient = listOf(Color(0xFFDC2626), Color(0xFFEA580C)),
                                    testTagName = "start_live_exam_btn_${exam.id}",
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onStartLiveExam(exam) }
                                )
                            } else {
                                val cutoffEpoch = exam.startTimeEpochMillis + (exam.lateEntryCutoffMinutes * 60_000L)
                                val blockReason = when {
                                    exam.eligibilityType == "REGISTERED_ONLY" && !isRegistered ->
                                        "Entry Locked: Pre-registration was required for this Live Exam"
                                    !exam.lateEntryAllowed && nowMillis > exam.startTimeEpochMillis ->
                                        "Entry Locked: Late entry is disabled for this Live Exam"
                                    else ->
                                        "Entry Locked: Late entry cutoff (${IstClock.formatTimeOnly(cutoffEpoch)}) has passed"
                                }
                                CoolingGlassBadge(
                                    text = blockReason,
                                    color = glass.incorrect,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        dynStatus == LiveExamDynamicStatus.UPCOMING ||
                            dynStatus == LiveExamDynamicStatus.REGISTRATION_OPEN ||
                            dynStatus == LiveExamDynamicStatus.REGISTRATION_CLOSED ||
                            dynStatus == LiveExamDynamicStatus.STARTING_SOON -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isRegistered && dynStatus != LiveExamDynamicStatus.REGISTRATION_CLOSED) {
                                    CoolingGlassButton(
                                        text = "Register for Live Exam",
                                        icon = Icons.Rounded.HowToReg,
                                        testTagName = "register_live_exam_btn_${exam.id}",
                                        modifier = Modifier.weight(1f),
                                        onClick = { onRegisterLiveExam(exam, false) }
                                    )
                                }
                                if (isRegistered || exam.eligibilityType == "ALL_USERS") {
                                    CoolingGlassOutlinedButton(
                                        text = "Enter Waiting Room",
                                        icon = Icons.Rounded.Schedule,
                                        testTagName = "waiting_room_btn_${exam.id}",
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            onRegisterLiveExam(exam, true)
                                            waitingRoomExamId = exam.id
                                        }
                                    )
                                }
                            }
                        }
                        dynStatus == LiveExamDynamicStatus.COMPLETED -> {
                            if (linkedTest != null) {
                                CoolingGlassOutlinedButton(
                                    text = "View Live Exam Leaderboard",
                                    icon = Icons.Rounded.EmojiEvents,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onOpenLiveExamRank(linkedTest) }
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    // Waiting Room Modal (Never exposes questions prior to official start time)
    activeWaitingExam?.let { exam ->
        val remainingToStart = (exam.startTimeEpochMillis - nowMillis).coerceAtLeast(0L)
        CoolingGlassDialog(
            title = "Live Exam Waiting Room",
            onDismiss = { waitingRoomExamId = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_exam_waiting_room_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoolingGlassBadge("SYNCHRONIZED IST SCHEDULE", glass.accentPrimary)
                    if (exam.secureModeEnabled) {
                        CoolingGlassBadge("SECURE MODE", Color(0xFFDC2626))
                    }
                }
                Text(
                    text = exam.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${exam.totalQuestions} Questions • ${exam.totalMarks.toInt()} Marks • ${exam.durationMinutes} Minutes Common Window",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFDC2626).copy(alpha = 0.12f))
                        .border(1.2.dp, Color(0xFFDC2626), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "EXAM AUTOMATICALLY LAUNCHES IN",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatLiveCountdown(remainingToStart),
                        style = MaterialTheme.typography.headlineMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Official Start: ${IstClock.formatDateTime(exam.startTimeEpochMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(glass.glassSurfaceStrong)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Live Exam Security & Timing Rules:",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Common End Time: ${IstClock.formatTimeOnly(exam.endTimeEpochMillis)} (same deadline for all participants).",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    Text(
                        text = "• Final 5-Minute Alert triggers at 05:00 remaining; exam auto-submits at 00:00.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    if (exam.secureModeEnabled) {
                        Text(
                            text = "• Secure Mode Active: Screen capture lock (FLAG_SECURE), Dynamic Student Watermark, Navigation Exit Lock, and Focus-Loss detection (${exam.warningLimit} warnings max).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFDC2626)
                        )
                    }
                    Text(
                        text = "• Question content remains locked until the official start countdown reaches 00:00.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.correct
                    )
                }

                if (nowMillis >= exam.startTimeEpochMillis) {
                    CoolingGlassButton(
                        text = "Launch Live Exam Now",
                        icon = Icons.Rounded.PlayArrow,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            waitingRoomExamId = null
                            onStartLiveExam(exam)
                        }
                    )
                } else {
                    CoolingGlassOutlinedButton(
                        text = "Close Waiting Room",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { waitingRoomExamId = null }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminLiveExamManagementSection(
    tests: List<ExamTest>,
    liveExams: List<LiveExam>,
    liveExamSessions: List<LiveExamSession>,
    members: List<GroupMember>,
    onSaveOrScheduleLiveExam: (LiveExam, Boolean) -> Unit,
    onRemoveLiveExamUnlinkOnly: (LiveExam) -> Unit,
    onToggleLiveExamPublished: (LiveExam, Boolean) -> Unit,
    onCancelLiveExam: (LiveExam) -> Unit,
    onForceStartLiveExam: (LiveExam) -> Unit,
    onPublishLiveExamResultsNow: (LiveExam, Boolean) -> Unit,
    onModerateLiveExamSession: (LiveExamSession, String, String) -> Unit,
    onClearCompletedAndPastLiveExams: () -> Unit = {}
) {
    val glass = LocalCoolingGlass.current
    var subTab by remember { mutableIntStateOf(0) } // 0 = Set Selector, 1 = Scheduler & Security, 2 = Live Monitor
    var confirmClearCompletedStep by remember { mutableIntStateOf(0) }
    var setSearchQuery by remember { mutableStateOf("") }
    var setCategoryFilter by remember { mutableStateOf<RankCategory?>(null) }

    // Scheduler Form State
    var editingLiveExamId by remember { mutableStateOf("") }
    var selectedSourceTestId by remember { mutableStateOf(tests.firstOrNull()?.id.orEmpty()) }
    var liveTitle by remember { mutableStateOf("") }
    var liveDescription by remember { mutableStateOf("") }
    var selectedRankCategory by remember { mutableStateOf(RankCategory.LANGUAGE_COMMON) }
    var startOffsetMinutesText by remember { mutableStateOf("15") }
    var customStartIstText by remember { mutableStateOf("") } // Optional yyyy-MM-dd HH:mm in IST
    var durationMinutesText by remember { mutableStateOf("45") }
    var resultPublishDelayMinText by remember { mutableStateOf("15") }
    var negativeNumText by remember { mutableStateOf("1") }
    var negativeDenText by remember { mutableStateOf("3") }
    var eligibilityMode by remember { mutableStateOf("ALL_USERS") }
    var maxParticipantsText by remember { mutableStateOf("1000") }
    var lateEntryAllowed by remember { mutableStateOf(true) }
    var lateCutoffMinutesText by remember { mutableStateOf("15") }

    // Per-Exam Security Policy State
    var secureModeEnabled by remember { mutableStateOf(true) }
    var dynamicWatermark by remember { mutableStateOf(true) }
    var focusLossDetection by remember { mutableStateOf(true) }
    var copyPasteBlock by remember { mutableStateOf(true) }
    var navigationExitLock by remember { mutableStateOf(true) }
    var warningLimitText by remember { mutableStateOf("3") }

    // Live Monitor State
    var monitoredExamId by remember { mutableStateOf(liveExams.firstOrNull()?.id.orEmpty()) }
    var inspectingSession by remember { mutableStateOf<LiveExamSession?>(null) }
    var moderationNote by remember { mutableStateOf("") }

    fun populateFormFromSourceTest(test: ExamTest, existingExam: LiveExam? = null) {
        val cat = existingExam?.resolvedRankCategory()
            ?: RankCategory.fromTest(test)
            ?: RankCategory.COMMON
        editingLiveExamId = existingExam?.id.orEmpty()
        selectedSourceTestId = test.id
        liveTitle = existingExam?.title ?: "${test.title} (Live Exam)"
        liveDescription = existingExam?.description ?: "Official Scheduled Live Examination (${cat.displayName})"
        selectedRankCategory = cat
        durationMinutesText = (existingExam?.durationMinutes ?: test.durationMinutes).toString()
        negativeNumText = (existingExam?.negativeMarkNumerator ?: test.negativeMarkNumerator).toString()
        negativeDenText = (existingExam?.negativeMarkDenominator ?: test.negativeMarkDenominator).toString()
        eligibilityMode = existingExam?.eligibilityType ?: "ALL_USERS"
        maxParticipantsText = (existingExam?.maxParticipants ?: 1000L).toString()
        lateEntryAllowed = existingExam?.lateEntryAllowed ?: true
        lateCutoffMinutesText = (existingExam?.lateEntryCutoffMinutes ?: 15L).toString()
        secureModeEnabled = existingExam?.secureModeEnabled ?: true
        dynamicWatermark = existingExam?.dynamicWatermark ?: true
        focusLossDetection = existingExam?.focusLossDetection ?: true
        copyPasteBlock = existingExam?.copyPasteBlock ?: true
        navigationExitLock = existingExam?.navigationExitLock ?: true
        warningLimitText = (existingExam?.warningLimit ?: 3L).toString()
        if (existingExam != null && existingExam.startTimeEpochMillis > 0L) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                timeZone = IstClock.istTimeZone
            }
            customStartIstText = sdf.format(Date(existingExam.startTimeEpochMillis))
        } else {
            customStartIstText = ""
            startOffsetMinutesText = "15"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_live_exam_management_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CoolingGlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live Exam Management & Security Centre",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Link existing question sets without duplication • Common IST Schedule • Anti-Cheat Monitor",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
                CoolingGlassBadge("${liveExams.size} Live Exams", Color(0xFFDC2626))
            }
            Spacer(Modifier.height(10.dp))
            CoolingGlassTabRow(
                tabs = listOf(
                    "1. Set Selector (${tests.size})",
                    "2. Schedule & Security",
                    "3. Live Monitor (${liveExamSessions.size})"
                ),
                selectedIndex = subTab,
                onSelect = { subTab = it }
            )
        }

        // Completed & Past / All Live Exams Cleanup Card (Deletes completed/past trial Live Exams even if source set was deleted)
        val nowMillisForAdmin = System.currentTimeMillis()
        val completedAndPastExams = remember(liveExams, nowMillisForAdmin) {
            liveExams.filter { exam ->
                val dyn = exam.dynamicStatus(nowMillisForAdmin)
                dyn == LiveExamDynamicStatus.COMPLETED ||
                    dyn == LiveExamDynamicStatus.CANCELLED ||
                    exam.status in listOf("COMPLETED", "CANCELLED") ||
                    (exam.endTimeEpochMillis in 1..nowMillisForAdmin)
            }
        }
        val activeOrUpcomingExams = remember(liveExams, completedAndPastExams) {
            val completedIds = completedAndPastExams.map { it.id }.toSet()
            liveExams.filter { it.id !in completedIds }
        }

        CoolingGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_live_exams_cleanup_card"),
            strongFrost = true,
            tintColor = glass.incorrect,
            borderColor = glass.incorrect.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Completed & Past Live Exams Cleanup (${completedAndPastExams.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Remove Completed & Past trial Live Exams from the Live Exam taskbar tab while keeping original Question Sets safe.",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary
                    )
                }
                CoolingGlassBadge(
                    text = "${completedAndPastExams.size} Past • ${activeOrUpcomingExams.size} Active",
                    color = if (completedAndPastExams.isNotEmpty()) glass.incorrect else glass.correct
                )
            }

            if (completedAndPastExams.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                if (confirmClearCompletedStep == 0) {
                    CoolingGlassOutlinedButton(
                        text = "Delete All Completed & Past Live Exams (${completedAndPastExams.size})",
                        icon = Icons.Rounded.DeleteSweep,
                        tint = glass.incorrect,
                        testTagName = "clear_completed_past_live_exams_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { confirmClearCompletedStep = 1 }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoolingGlassOutlinedButton(
                            text = "Cancel",
                            icon = Icons.Rounded.Close,
                            modifier = Modifier.weight(0.8f),
                            onClick = { confirmClearCompletedStep = 0 }
                        )
                        CoolingGlassButton(
                            text = "Confirm Delete (${completedAndPastExams.size})",
                            icon = Icons.Rounded.DeleteForever,
                            accentGradient = listOf(Color(0xFFDC2626), Color(0xFFB91C1C)),
                            testTagName = "confirm_clear_completed_live_exams_btn",
                            modifier = Modifier.weight(1.2f),
                            onClick = {
                                confirmClearCompletedStep = 0
                                onClearCompletedAndPastLiveExams()
                            }
                        )
                    }
                }
            }

            if (liveExams.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "No Completed, Past, or Scheduled Live Exams currently in the Live Exam tab.",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textMuted
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (completedAndPastExams + activeOrUpcomingExams).forEach { lExam ->
                        val isPastOrCompleted = completedAndPastExams.any { it.id == lExam.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(glass.glassSurfaceStrong)
                                .border(
                                    1.dp,
                                    if (isPastOrCompleted) glass.incorrect.copy(alpha = 0.4f) else glass.glassBorderSubtle,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CoolingGlassBadge(
                                        text = if (isPastOrCompleted) "COMPLETED & PAST" else lExam.status,
                                        color = if (isPastOrCompleted) glass.incorrect else glass.correct
                                    )
                                    Text(
                                        text = lExam.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = glass.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${lExam.examDateIst} ${lExam.startTimeIst} • Set: ${lExam.sourceSetTitle.ifBlank { lExam.sourceSetId }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            CoolingGlassOutlinedButton(
                                text = "Delete",
                                icon = Icons.Rounded.DeleteOutline,
                                tint = glass.incorrect,
                                testTagName = "delete_live_exam_btn_${lExam.id}",
                                onClick = { onRemoveLiveExamUnlinkOnly(lExam) }
                            )
                        }
                    }
                }
            }
        }

        when (subTab) {
            0 -> {
                // SUB-TAB A: LIVE EXAM SET SELECTOR
                CoolingGlassSearchBar(
                    query = setSearchQuery,
                    onQueryChange = { setSearchQuery = it },
                    placeholder = "Search existing question sets to link or publish as Live Exam..."
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        val sel = setCategoryFilter == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) GlacierBlue600 else glass.glassSurfaceStrong)
                                .clickable { setCategoryFilter = null }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "All 7 Categories",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sel) CrystalWhite else glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(RankCategory.entries) { cat ->
                        val sel = setCategoryFilter == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) GlacierBlue600 else glass.glassSurfaceStrong)
                                .clickable { setCategoryFilter = if (sel) null else cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sel) CrystalWhite else glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                val filteredSets = remember(tests, setSearchQuery, setCategoryFilter) {
                    val q = setSearchQuery.trim().lowercase()
                    tests.filter { t ->
                        val cat = RankCategory.fromTest(t)
                        val matchCat = setCategoryFilter == null || cat == setCategoryFilter
                        val matchQ = q.isBlank() || t.title.lowercase().contains(q) || (cat?.displayName?.lowercase()?.contains(q) == true)
                        matchCat && matchQ
                    }
                }

                filteredSets.forEach { test ->
                    val linkedExams = liveExams.filter { it.sourceSetId == test.id }
                    val rankCat = RankCategory.fromTest(test)
                    CoolingGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_live_set_selector_card_${test.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CoolingGlassBadge(rankCat?.displayName ?: test.parsedPaperType().baseSetName, glass.accentPrimary)
                                    CoolingGlassBadge("${test.questionCount} Qs • ${test.durationMinutes}m", GlacierBlue600)
                                    if (linkedExams.isNotEmpty()) {
                                        CoolingGlassBadge(
                                            text = "LINKED (${linkedExams.size})",
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = test.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Source Set ID: ${test.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textMuted
                                )
                            }
                            CoolingGlassButton(
                                text = if (linkedExams.isEmpty()) "Add to Live Exam" else "Schedule New",
                                icon = Icons.Rounded.Add,
                                testTagName = "add_set_to_live_exam_btn_${test.id}",
                                onClick = {
                                    populateFormFromSourceTest(test, linkedExams.firstOrNull())
                                    subTab = 1
                                }
                            )
                        }

                        if (linkedExams.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            linkedExams.forEach { lExam ->
                                val isPub = lExam.status == "PUBLISHED"
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(glass.glassSurfaceStrong)
                                        .border(1.dp, glass.glassBorderSubtle, RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = lExam.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = glass.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Start: ${IstClock.formatDateTime(lExam.startTimeEpochMillis)} • Status: ${lExam.status}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = glass.textSecondary
                                            )
                                        }
                                        CoolingGlassBadge(
                                            text = lExam.status,
                                            color = if (isPub) glass.correct else glass.review
                                        )
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CoolingGlassOutlinedButton(
                                            text = if (isPub) "Unpublish" else "Publish",
                                            icon = if (isPub) Icons.Rounded.VisibilityOff else Icons.Rounded.Publish,
                                            tint = if (isPub) glass.review else glass.correct,
                                            testTagName = "toggle_publish_live_exam_${lExam.id}",
                                            onClick = { onToggleLiveExamPublished(lExam, !isPub) }
                                        )
                                        CoolingGlassOutlinedButton(
                                            text = "Edit Schedule",
                                            icon = Icons.Rounded.Edit,
                                            onClick = {
                                                populateFormFromSourceTest(test, lExam)
                                                subTab = 1
                                            }
                                        )
                                        CoolingGlassOutlinedButton(
                                            text = "Remove from Live Exam",
                                            icon = Icons.Rounded.LinkOff,
                                            tint = glass.incorrect,
                                            testTagName = "remove_live_exam_unlink_${lExam.id}",
                                            onClick = { onRemoveLiveExamUnlinkOnly(lExam) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // SUB-TAB B: LIVE EXAM SCHEDULER & SECURITY POLICY BUILDER
                val selectedTest = tests.firstOrNull { it.id == selectedSourceTestId } ?: tests.firstOrNull()
                CoolingGlassCard {
                    Text(
                        text = if (editingLiveExamId.isBlank()) "Schedule New Live Exam" else "Edit Scheduled Live Exam ($editingLiveExamId)",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Select Source Question Set (Links via source_set_id without duplicating questions):",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.textSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tests) { t ->
                            val isSel = t.id == selectedTest?.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) GlacierBlue600 else glass.glassSurfaceStrong)
                                    .border(
                                        1.dp,
                                        if (isSel) IceCyan400 else glass.glassBorderSubtle,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        populateFormFromSourceTest(t, null)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${t.title} (${t.questionCount}Q)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSel) CrystalWhite else glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    CoolingGlassTextField(
                        value = liveTitle,
                        onValueChange = { liveTitle = it },
                        label = "Live Exam Title"
                    )
                    Spacer(Modifier.height(8.dp))
                    CoolingGlassTextField(
                        value = liveDescription,
                        onValueChange = { liveDescription = it },
                        label = "Instructions / Description",
                        singleLine = false,
                        minLines = 2
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Category (Aligned with 7 Rank Categories):",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.textSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RankCategory.entries.forEach { cat ->
                            val isSel = selectedRankCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) GlacierBlue600 else glass.glassSurfaceStrong)
                                    .clickable { selectedRankCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSel) CrystalWhite else glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Quick Start Offset (Minutes from Now in IST) or Custom IST Date/Time:",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.textSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("0" to "Start Now", "5" to "+5 Min", "15" to "+15 Min", "60" to "+1 Hr", "1440" to "+1 Day").forEach { (mins, lbl) ->
                            val sel = customStartIstText.isBlank() && startOffsetMinutesText == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) GlacierBlue600 else glass.glassSurfaceStrong)
                                    .clickable {
                                        customStartIstText = ""
                                        startOffsetMinutesText = mins
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = lbl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (sel) CrystalWhite else glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoolingGlassTextField(
                            value = customStartIstText,
                            onValueChange = { customStartIstText = it },
                            label = "Custom IST (yyyy-MM-dd HH:mm)",
                            modifier = Modifier.weight(1.3f)
                        )
                        CoolingGlassTextField(
                            value = durationMinutesText,
                            onValueChange = { durationMinutesText = it.filter { ch -> ch.isDigit() } },
                            label = "Duration (Min)",
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoolingGlassTextField(
                            value = negativeNumText,
                            onValueChange = { negativeNumText = it.filter { ch -> ch.isDigit() } },
                            label = "Neg Num (0 or 1)",
                            modifier = Modifier.weight(0.8f)
                        )
                        CoolingGlassTextField(
                            value = negativeDenText,
                            onValueChange = { negativeDenText = it.filter { ch -> ch.isDigit() } },
                            label = "Neg Den (e.g. 3)",
                            modifier = Modifier.weight(0.8f)
                        )
                        CoolingGlassTextField(
                            value = resultPublishDelayMinText,
                            onValueChange = { resultPublishDelayMinText = it.filter { ch -> ch.isDigit() } },
                            label = "Result Delay (m)",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    CoolingGlassTabRow(
                        tabs = listOf("Open to All Users", "Registered Users Only"),
                        selectedIndex = if (eligibilityMode == "ALL_USERS") 0 else 1,
                        onSelect = { eligibilityMode = if (it == 0) "ALL_USERS" else "REGISTERED_ONLY" }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Late Entry (Shares Remaining Common Time)",
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Late joiners auto-submit at the exact same Common End Time.",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textSecondary
                            )
                        }
                        Switch(checked = lateEntryAllowed, onCheckedChange = { lateEntryAllowed = it })
                    }
                    if (lateEntryAllowed) {
                        CoolingGlassTextField(
                            value = lateCutoffMinutesText,
                            onValueChange = { lateCutoffMinutesText = it.filter { ch -> ch.isDigit() } },
                            label = "Late Entry Cutoff (Minutes after Start)"
                        )
                    }
                }

                // Per-Exam Security Policy Card
                CoolingGlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = "Security Policy",
                            tint = Color(0xFFDC2626)
                        )
                        Text(
                            text = "Per-Exam Secure Mode & Anti-Cheating Policy",
                            style = MaterialTheme.typography.titleSmall,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    @Composable
                    fun SecurityToggleRow(label: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.labelMedium, color = glass.textPrimary, fontWeight = FontWeight.SemiBold)
                                Text(sub, style = MaterialTheme.typography.labelSmall, color = glass.textSecondary)
                            }
                            Switch(checked = checked, onCheckedChange = onChange)
                        }
                    }

                    SecurityToggleRow(
                        label = "Enable Secure Exam Mode (FLAG_SECURE)",
                        sub = "Blocks screenshots, screen recording, and recent-apps preview on Android.",
                        checked = secureModeEnabled,
                        onChange = { secureModeEnabled = it }
                    )
                    SecurityToggleRow(
                        label = "Dynamic Diagonal Student Watermark",
                        sub = "Overlays Name, Masked Email, UID, Exam ID & IST Timestamp across exam screen.",
                        checked = dynamicWatermark,
                        onChange = { dynamicWatermark = it }
                    )
                    SecurityToggleRow(
                        label = "Focus-Loss & App-Switch Detection",
                        sub = "Detects backgrounding/tab switches: 1st Soft Warning, 2nd Strong Warning, 3rd Flagged.",
                        checked = focusLossDetection,
                        onChange = { focusLossDetection = it }
                    )
                    SecurityToggleRow(
                        label = "Navigation Exit Lock",
                        sub = "Intercepts hardware Back button until final submission.",
                        checked = navigationExitLock,
                        onChange = { navigationExitLock = it }
                    )
                    Spacer(Modifier.height(6.dp))
                    CoolingGlassTextField(
                        value = warningLimitText,
                        onValueChange = { warningLimitText = it.filter { ch -> ch.isDigit() } },
                        label = "Violation Warning Limit before Flagging (Default: 3)"
                    )

                    Spacer(Modifier.height(12.dp))

                    fun buildConfiguredLiveExam(publishNow: Boolean): LiveExam? {
                        val source = selectedTest ?: return null
                        val now = System.currentTimeMillis()
                        val parsedCustomStart = if (customStartIstText.isNotBlank()) {
                            IstClock.parse(customStartIstText.trim(), "yyyy-MM-dd HH:mm")?.time
                        } else null
                        val offsetMins = startOffsetMinutesText.toLongOrNull() ?: 15L
                        val startEpoch = parsedCustomStart ?: (now + offsetMins * 60_000L)
                        val durMins = (durationMinutesText.toLongOrNull() ?: source.durationMinutes).coerceAtLeast(5L)
                        val endEpoch = startEpoch + durMins * 60_000L
                        val lateCutoffMins = (lateCutoffMinutesText.toLongOrNull() ?: 15L).coerceIn(0L, durMins)
                        val resultDelayMins = (resultPublishDelayMinText.toLongOrNull() ?: 0L).coerceAtLeast(0L)
                        val resultPublishEpoch = endEpoch + resultDelayMins * 60_000L
                        val totalQ = source.questionCount.coerceAtLeast(source.questions.size.toLong()).coerceAtLeast(1L)
                        val totalM = totalQ.toDouble() * source.positiveMarks
                        val negNum = (negativeNumText.toLongOrNull() ?: source.negativeMarkNumerator).coerceAtLeast(0L)
                        val negDen = (negativeDenText.toLongOrNull() ?: source.negativeMarkDenominator).coerceAtLeast(1L)

                        return LiveExam(
                            id = editingLiveExamId,
                            groupId = source.groupId,
                            sourceSetId = source.id,
                            sourceSetTitle = source.title,
                            title = liveTitle.ifBlank { "${source.title} (Live Exam)" },
                            description = liveDescription.ifBlank { "Scheduled Live Examination" },
                            category = selectedRankCategory.name,
                            paperType = source.paperType,
                            examDateIst = IstClock.formatEpochMillis(startEpoch, "dd MMM yyyy"),
                            startTimeIst = IstClock.formatTimeOnly(startEpoch),
                            startTimeEpochMillis = startEpoch,
                            durationMinutes = durMins,
                            endTimeEpochMillis = endEpoch,
                            registrationOpenEpochMillis = now,
                            registrationCloseEpochMillis = startEpoch,
                            resultPublishEpochMillis = resultPublishEpoch,
                            totalQuestions = totalQ,
                            totalMarks = totalM,
                            negativeMarkNumerator = negNum,
                            negativeMarkDenominator = negDen,
                            eligibilityType = eligibilityMode,
                            maxParticipants = (maxParticipantsText.toLongOrNull() ?: 1000L).coerceAtLeast(1L),
                            lateEntryAllowed = lateEntryAllowed,
                            lateEntryCutoffMinutes = lateCutoffMins,
                            secureModeEnabled = secureModeEnabled,
                            dynamicWatermark = dynamicWatermark,
                            focusLossDetection = focusLossDetection,
                            copyPasteBlock = copyPasteBlock,
                            navigationExitLock = navigationExitLock,
                            warningLimit = (warningLimitText.toLongOrNull() ?: 3L).coerceIn(1L, 10L),
                            status = if (publishNow) "PUBLISHED" else "DRAFT"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoolingGlassOutlinedButton(
                            text = "Save as Draft",
                            icon = Icons.Rounded.Schedule,
                            testTagName = "save_live_exam_draft_btn",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                buildConfiguredLiveExam(publishNow = false)?.let {
                                    onSaveOrScheduleLiveExam(it, false)
                                }
                            }
                        )
                        CoolingGlassButton(
                            text = "Publish Live Exam",
                            icon = Icons.Rounded.Publish,
                            testTagName = "publish_live_exam_btn",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                buildConfiguredLiveExam(publishNow = true)?.let {
                                    onSaveOrScheduleLiveExam(it, true)
                                }
                            }
                        )
                    }
                }
            }

            2 -> {
                // SUB-TAB C: ADMIN LIVE MONITORING DASHBOARD
                if (liveExams.isEmpty()) {
                    CoolingGlassEmptyState(
                        icon = Icons.Rounded.Podcasts,
                        title = "No Live Exams Configured Yet",
                        subtitle = "Select a question set in Tab 1 to schedule and monitor a Live Exam."
                    )
                } else {
                    val activeExam = liveExams.firstOrNull { it.id == monitoredExamId } ?: liveExams.first()
                    val examSessions = remember(liveExamSessions, activeExam.id) {
                        liveExamSessions.filter { it.liveExamId == activeExam.id }
                    }
                    val waitingCount = examSessions.count { it.status == "WAITING_ROOM" }
                    val activeCount = examSessions.count { it.status == "ACTIVE" }
                    val submittedSessions = examSessions.filter { it.status == "SUBMITTED" || it.status == "APPROVED" }
                    val submittedScores = submittedSessions.mapNotNull { it.score }
                    val flaggedCount = examSessions.count { it.status == "FLAGGED" || it.violationCount >= activeExam.warningLimit }
                    val avgScore = if (submittedScores.isNotEmpty()) submittedScores.average() else 0.0
                    val maxScore = submittedScores.maxOrNull() ?: 0.0
                    val minScore = submittedScores.minOrNull() ?: 0.0

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(liveExams) { lExam ->
                            val isSel = lExam.id == activeExam.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) GlacierBlue600 else glass.glassSurfaceStrong)
                                    .clickable { monitoredExamId = lExam.id }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${lExam.title} (${lExam.status})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSel) CrystalWhite else glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    CoolingGlassCard {
                        Text(
                            text = activeExam.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Start: ${IstClock.formatDateTime(activeExam.startTimeEpochMillis)} • End: ${IstClock.formatDateTime(activeExam.endTimeEpochMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatPill("Eligible", "${members.size}", glass.accentPrimary, Modifier.weight(1f))
                            StatPill("Registered", "${examSessions.size}", GlacierBlue600, Modifier.weight(1f))
                            StatPill("Waiting", "$waitingCount", SoftLavender500, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatPill("Active Live", "$activeCount", Color(0xFF059669), Modifier.weight(1f))
                            StatPill("Submitted", "${submittedSessions.size}", glass.correct, Modifier.weight(1f))
                            StatPill("Flagged", "$flaggedCount", Color(0xFFDC2626), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatPill("Avg Score", ExamScoringEngine.formatScore(avgScore), glass.accentCyan, Modifier.weight(1f))
                            StatPill("Highest", ExamScoringEngine.formatScore(maxScore), glass.correct, Modifier.weight(1f))
                            StatPill("Lowest", ExamScoringEngine.formatScore(minScore), glass.review, Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CoolingGlassButton(
                                text = if (activeExam.resultsPublishedManually) "Results Released (Revert)" else "Manual Publish Results Now",
                                icon = Icons.Rounded.VerifiedUser,
                                testTagName = "manual_publish_live_results_btn",
                                onClick = { onPublishLiveExamResultsNow(activeExam, !activeExam.resultsPublishedManually) }
                            )
                            CoolingGlassOutlinedButton(
                                text = "Force Start Early",
                                icon = Icons.Rounded.PlayArrow,
                                onClick = { onForceStartLiveExam(activeExam) }
                            )
                            if (activeExam.status != "CANCELLED") {
                                CoolingGlassOutlinedButton(
                                    text = "Cancel Live Exam",
                                    icon = Icons.Rounded.Cancel,
                                    tint = glass.incorrect,
                                    onClick = { onCancelLiveExam(activeExam) }
                                )
                            }
                            CoolingGlassOutlinedButton(
                                text = "Delete Live Exam",
                                icon = Icons.Rounded.DeleteOutline,
                                tint = glass.incorrect,
                                testTagName = "monitor_delete_live_exam_btn_${activeExam.id}",
                                onClick = { onRemoveLiveExamUnlinkOnly(activeExam) }
                            )
                        }
                    }

                    Text(
                        text = "Participant Sessions & Anti-Cheat Violation Logs (${examSessions.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    if (examSessions.isEmpty()) {
                        CoolingGlassCard {
                            Text(
                                text = "No student sessions registered for this Live Exam yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    } else {
                        examSessions.forEach { session ->
                            val isFlagged = session.status == "FLAGGED" || session.violationCount >= activeExam.warningLimit
                            CoolingGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        inspectingSession = session
                                        moderationNote = session.adminModerationNote.orEmpty()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.userName.ifBlank { "Student (${session.userId.take(6)})" },
                                            style = MaterialTheme.typography.titleSmall,
                                            color = glass.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${session.userEmail.orEmpty()} • ID: ${session.userId.take(8)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glass.textSecondary
                                        )
                                    }
                                    CoolingGlassBadge(
                                        text = session.status,
                                        color = when (session.status) {
                                            "DISQUALIFIED" -> glass.incorrect
                                            "FLAGGED" -> Color(0xFFDC2626)
                                            "APPROVED", "SUBMITTED" -> glass.correct
                                            "ACTIVE" -> GlacierBlue600
                                            else -> SoftLavender500
                                        }
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Progress: ${session.answeredCount}/${session.totalQuestions} • Score: ${ExamScoringEngine.formatScore(session.score ?: 0.0)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = glass.textPrimary
                                    )
                                    Text(
                                        text = "Violations: ${session.violationCount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isFlagged) Color(0xFFDC2626) else glass.textSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (session.violationLogs.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Latest Log: ${session.violationLogs.last()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFDC2626),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Security Violation Log & Approve/Disqualify Moderation Modal
    inspectingSession?.let { session ->
        CoolingGlassDialog(
            title = "Security Violation Log • ${session.userName}",
            onDismiss = { inspectingSession = null }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Student ID: ${session.userId} • Status: ${session.status} • Violations: ${session.violationCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (session.violationLogs.isEmpty()) {
                    Text(
                        text = "No security violations recorded during this session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.correct
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(glass.glassSurfaceStrong)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        session.violationLogs.forEachIndexed { idx, logLine ->
                            Text(
                                text = "${idx + 1}. $logLine",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
                CoolingGlassTextField(
                    value = moderationNote,
                    onValueChange = { moderationNote = it },
                    label = "Admin Moderation Note"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoolingGlassButton(
                        text = "Approve Session",
                        icon = Icons.Rounded.CheckCircle,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onModerateLiveExamSession(session, "APPROVED", moderationNote.ifBlank { "Verified & Approved by Admin" })
                            inspectingSession = null
                        }
                    )
                    CoolingGlassOutlinedButton(
                        text = "Disqualify",
                        icon = Icons.Rounded.Block,
                        tint = glass.incorrect,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onModerateLiveExamSession(session, "DISQUALIFIED", moderationNote.ifBlank { "Disqualified due to security violations" })
                            inspectingSession = null
                        }
                    )
                }
            }
        }
    }
}
