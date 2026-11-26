package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.AppRole
import com.example.data.model.ContentViewRecord
import com.example.data.model.CurrentAffairItem
import com.example.data.model.DailyTargetTask
import com.example.data.model.ExamGroup
import com.example.data.model.ExamTest
import com.example.data.model.GroupMember
import com.example.data.model.LangQualifyingStatus
import com.example.data.model.PaperType
import com.example.data.model.RankedAspirant
import com.example.data.model.TestAttempt
import com.example.data.model.UserProfile
import com.example.domain.ExamScoringEngine
import com.example.ui.glass.AnimatedBase64Image
import com.example.ui.glass.AutoResizeSingleLineText
import com.example.ui.glass.CoolingGlassAvatar
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassBottomSheet
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassDialog
import com.example.ui.glass.CoolingGlassEmptyState
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.CoolingGlassProfileCard
import com.example.ui.glass.CoolingGlassStatusBanner
import com.example.ui.glass.CoolingGlassTextField
import com.example.ui.glass.FounderBrandingCard
import com.example.ui.glass.ProfilePhotoZoomDialog
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.GlacierBlue600
import com.example.ui.theme.IceCyan500
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.SoftLavender500
import com.example.ui.theme.appText
import com.example.util.ImageCodec
import com.example.util.IstClock
import com.example.util.PersistedExamSession
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class DashboardDestination {
    COMMON_GS_TESTS,
    LANGUAGE_TESTS,
    QUESTION_BANK,
    CREATE_SET,
    INTERACTIVE_POLLS,
    LEADERBOARD,
    WEEKLY_MONTHLY_RANK,
    MONTHLY_REPORT,
    MY_PERFORMANCE,
    ALL_SCORES,
    PREVIOUS_TESTS,
    PDF_DOWNLOADS,
    DISCUSSIONS,
    NOTIFICATIONS,
    GROUP_INFO,
    PROFILE_SETTINGS,
    CONTACT_ADMIN,
    APP_UPDATE,
    ADMIN_CONSOLE,
    ABOUT_FOUNDER
}

@Composable
fun DashboardScreen(
    member: GroupMember?,
    group: ExamGroup?,
    overallRanking: List<RankedAspirant>,
    currentAffairs: List<CurrentAffairItem>,
    tests: List<ExamTest>,
    unreadNotificationsCount: Int,
    hasAvailableUpdate: Boolean = false,
    showContactAdmin: Boolean = true,
    contentViews: List<ContentViewRecord> = emptyList(),
    adminUserProfiles: List<UserProfile> = emptyList(),
    onTrackBannerView: (String) -> Unit = {},
    pausedSessions: Map<String, PersistedExamSession> = emptyMap(),
    onResumePausedTest: (ExamTest) -> Unit = {},
    dailyTargets: List<DailyTargetTask> = emptyList(),
    myAttempts: List<TestAttempt> = emptyList(),
    nowMillis: Long = System.currentTimeMillis(),
    onOpenTargetTest: (ExamTest) -> Unit = {},
    onSaveDailyTarget: (
        existingId: String?,
        test: ExamTest,
        targetDateIst: String,
        bannerTitle: String,
        bannerSubtitle: String?,
        bannerImageBase64: String?
    ) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteDailyTarget: (String) -> Unit = {},
    onNavigate: (DashboardDestination) -> Unit,
    onPublishStarterSets: () -> Unit = {},
    onPublishCurrentAffair: (title: String, summary: String, date: String, source: String, category: String, img: String?) -> Unit,
    onUpdateCurrentAffair: (itemId: String, title: String, summary: String, date: String, source: String, category: String, img: String?) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteCurrentAffair: (String) -> Unit
) {
    val glass = LocalCoolingGlass.current
    val isAdmin = member?.appRole()?.isAdminRole() == true
    val myRankEntry = overallRanking.firstOrNull { it.member.userId == member?.userId }
    val myRankText = if (myRankEntry != null && myRankEntry.testsAttempted > 0) "#${myRankEntry.rank}" else "—"
    val langColor = when (member?.langQualifying()) {
        LangQualifyingStatus.PASS -> glass.correct
        LangQualifyingStatus.FAIL -> glass.incorrect
        else -> glass.textMuted
    }
    val publishedLiveCount = tests.count { it.status == "PUBLISHED" }
    val publishedCommonCount = tests.count {
        it.status == "PUBLISHED" && (it.parsedPaperType() == PaperType.COMMON_GS || it.parsedPaperType() == PaperType.COMBINED || it.parsedPaperType().isFull150Paper())
    }
    val publishedLangCount = tests.count { it.status == "PUBLISHED" && it.parsedPaperType() == PaperType.LANGUAGE }

    var zoomedAffair by remember { mutableStateOf<CurrentAffairItem?>(null) }
    var showUploadAffairSheet by remember { mutableStateOf(false) }
    var editingAffair by remember { mutableStateOf<CurrentAffairItem?>(null) }
    var showHeroAvatarZoom by remember { mutableStateOf(false) }
    var bannerViewersTarget by remember { mutableStateOf<CurrentAffairItem?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. User Profile & Live Rank Summary Card
        item {
            CoolingGlassProfileCard(
                displayName = member?.displayName ?: "User",
                photoBase64 = member?.photoBase64,
                roleLabel = member?.appRole()?.labelEn ?: "User",
                groupName = group?.name ?: "TRE 4.0 Group",
                rankText = myRankText,
                gsAverage = member?.gsAverageScore ?: 0.0,
                langBadgeText = member?.let { ExamScoringEngine.languageBadgeText(it) } ?: "NOT ATTEMPTED",
                langBadgeColor = langColor,
                onEditClick = { onNavigate(DashboardDestination.PROFILE_SETTINGS) },
                onAvatarClick = { showHeroAvatarZoom = true }
            )
        }

        // 1b. Paused Exam Quick-Resume Card (Exact Frozen Remaining Time)
        val resumablePairs = pausedSessions.entries.mapNotNull { (testId, session) ->
            tests.firstOrNull { it.id == testId && it.status == "PUBLISHED" }?.let { it to session }
        }
        if (resumablePairs.isNotEmpty()) {
            item {
                val (pausedTest, pausedSession) = resumablePairs.first()
                val timeLeft = formatExamCountdown(pausedSession.pausedRemainingSeconds)
                CoolingGlassCard(
                    strongFrost = true,
                    tintColor = glass.review,
                    borderColor = glass.review.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PauseCircleOutline, contentDescription = null, tint = glass.review)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Paused Test Ready to Resume",
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        CoolingGlassBadge(text = "$timeLeft left", color = glass.review)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${pausedTest.title} • ${pausedSession.selectedAnswers.size} answer(s) saved. Resumes from exact remaining time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    CoolingGlassButton(
                        text = "Resume Test ($timeLeft)",
                        icon = Icons.Rounded.PlayArrow,
                        accentGradient = listOf(Color(0xFFD97706), Color(0xFFF59E0B)),
                        testTagName = "dash_resume_paused_${pausedTest.id}",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onResumePausedTest(pausedTest) }
                    )
                }
            }
        }

        // 1c. Aaj Ka Target (Today's Set Work Photo Banner + Yesterday Missed Rollover + Admin Next-Day Creator)
        item {
            DailyTargetHomeSection(
                dailyTargets = dailyTargets,
                tests = tests,
                myAttempts = myAttempts,
                isAdmin = isAdmin,
                nowMillis = nowMillis,
                onOpenTargetTest = onOpenTargetTest,
                onSaveDailyTarget = onSaveDailyTarget,
                onDeleteDailyTarget = onDeleteDailyTarget
            )
        }

        // 2. Daily Current Affairs Horizontal Image Slider (Admin-only Add/Edit/Delete + View Analytics)
        item {
            CurrentAffairsSliderSection(
                items = currentAffairs,
                contentViews = contentViews,
                canManage = isAdmin,
                onBannerViewed = onTrackBannerView,
                onOpenViewers = { bannerViewersTarget = it },
                onAddClick = {
                    editingAffair = null
                    showUploadAffairSheet = true
                },
                onItemClick = { zoomedAffair = it }
            )
        }

        // Clean Fresh-Start Card when Admin has 0 live sets
        if (publishedLiveCount == 0 && isAdmin) {
            item {
                CoolingGlassCard(
                    tintColor = glass.accentCyan,
                    borderColor = glass.accentCyan.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "Start Fresh: Create New TRE 4.0 Test Sets",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Build Language (15m), Common (30m), Lang+Common (45m), Class 6–8 Math & Sci (2h), or Class 9–10 Math (2h 15m) sets. Approve 1–2 sets daily from Admin Console.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    CoolingGlassButton(
                        text = "Create Fresh Question Set",
                        icon = Icons.Rounded.PostAdd,
                        testTagName = "create_fresh_set_btn",
                        onClick = { onNavigate(DashboardDestination.CREATE_SET) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. Examination Arena Hero Tiles
        item {
            SectionHeader(
                title = "Examination Arena",
                subtitle = appText(
                    en = "Exam Hall • 5-Option MCQ (-1/3 Negative, Opt E Not Attempted = 0)",
                    defaultText = "परीक्षा कक्ष • 5-Option MCQ (-1/3 Negative, Opt E Not Attempted = 0)"
                )
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardTile(
                    title = "Common & 150-Q Tests",
                    subtitle = "$publishedCommonCount live • 30m • 45m • 2h",
                    icon = Icons.Rounded.Quiz,
                    accent = GlacierBlue600,
                    tag = "dash_common_gs",
                    compactIcon = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(DashboardDestination.COMMON_GS_TESTS) }
                )
                DashboardTile(
                    title = "Language Paper",
                    subtitle = "$publishedLangCount live • 30 Q • 15m",
                    icon = Icons.Rounded.Translate,
                    accent = SoftLavender500,
                    tag = "dash_language_test",
                    compactIcon = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(DashboardDestination.LANGUAGE_TESTS) }
                )
            }
        }

        // 4. Question Bank (Admin Only) + Photo Polls & Doubt Session (All Users)
        item {
            SectionHeader(
                title = if (isAdmin) "Question Bank & Doubt Session" else "Photo Polls & Doubt Session",
                subtitle = if (isAdmin) {
                    appText(
                        en = "Admin Set Creation, 5-Option Polls & Photo Doubts/Solutions",
                        defaultText = "Admin सेट क्रिएशन, 5-ऑप्शन पोल एवं फोटो डाउट/समाधान"
                    )
                } else {
                    appText(
                        en = "5-Option Photo Polls & Doubt/Solution Forum",
                        defaultText = "5-ऑप्शन फोटो पोल एवं डाउट/समाधान मंच"
                    )
                }
            )
            Spacer(Modifier.height(8.dp))
            if (isAdmin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardTile(
                        title = "Question Bank",
                        subtitle = "PDF / DOC / DOCX / JSON",
                        icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                        accent = IceCyan500,
                        tag = "dash_question_bank",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(DashboardDestination.QUESTION_BANK) }
                    )
                    DashboardTile(
                        title = "Create Question Set",
                        subtitle = "15m • 30m • 45m • 150Q",
                        icon = Icons.Rounded.PostAdd,
                        accent = GlacierBlue600,
                        tag = "dash_create_set",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(DashboardDestination.CREATE_SET) }
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardTile(
                    title = "Interactive Photo Poll",
                    subtitle = appText("Ask Questions • 5-Option Vote", "सवाल पूछें • 5-Option Vote"),
                    icon = Icons.Rounded.Poll,
                    accent = SoftLavender500,
                    tag = "dash_polls",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(DashboardDestination.INTERACTIVE_POLLS) }
                )
                DashboardTile(
                    title = "Doubt Session & Sol.",
                    subtitle = "Upload Q & Sol. Photos",
                    icon = Icons.Rounded.Forum,
                    accent = IceCyan500,
                    tag = "dash_discussions",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(DashboardDestination.DISCUSSIONS) }
                )
            }
        }

        // 5. Rankings, Scores & Performance Analytics
        item {
            SectionHeader(
                title = "Rankings & Performance",
                subtitle = appText("Live Rankings, Analytics & PDFs", "लाइव रैंकिंग, विश्लेषण एवं पीडीएफ")
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardTile(
                    title = "Category Ranking",
                    subtitle = "7 Categories • Set & Time",
                    icon = Icons.Rounded.EmojiEvents,
                    accent = Color(0xFFD97706),
                    tag = "dash_leaderboard",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(DashboardDestination.LEADERBOARD) }
                )
                DashboardTile(
                    title = "Weekly Report",
                    subtitle = "Last 7 Days Rank",
                    icon = Icons.Rounded.CalendarMonth,
                    accent = GlacierBlue600,
                    tag = "dash_weekly_monthly",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(DashboardDestination.WEEKLY_MONTHLY_RANK) }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardTile(
                    title = "Monthly Report",
                    subtitle = "This Month Combined",
                    icon = Icons.Rounded.CalendarMonth,
                    accent = SoftLavender500,
                    tag = "dash_monthly_report",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(DashboardDestination.MONTHLY_REPORT) }
                )
                DashboardTile(
                    title = "My Performance",
                    subtitle = "Subject Analytics",
                    icon = Icons.Rounded.Assessment,
                    accent = IceCyan500,
                    tag = "dash_performance",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(DashboardDestination.MY_PERFORMANCE) }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardTile(
                    title = "All Users' Scores",
                    subtitle = "Centralized Sheet",
                    icon = Icons.Rounded.AutoStories,
                    accent = SoftLavender500,
                    tag = "dash_all_scores",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(DashboardDestination.ALL_SCORES) }
                )
                DashboardTile(
                    title = "Previous Tests",
                    subtitle = "Attempt History",
                    icon = Icons.Rounded.History,
                    accent = GlacierBlue600,
                    tag = "dash_prev_tests",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(DashboardDestination.PREVIOUS_TESTS) }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardTile(
                    title = "PDF & Result Cards",
                    subtitle = "Export & WhatsApp",
                    icon = Icons.Rounded.Download,
                    accent = Color(0xFF059669),
                    tag = "dash_pdf_downloads",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(DashboardDestination.PDF_DOWNLOADS) }
                )
            }
        }

        // 6. Official Founder Branding Card
        item {
            FounderBrandingCard(
                compact = true,
                modifier = Modifier.clickable { onNavigate(DashboardDestination.ABOUT_FOUNDER) }
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    // Full-screen Avatar Photo Zoom Preview
    if (showHeroAvatarZoom) {
        ProfilePhotoZoomDialog(
            name = member?.displayName ?: "User",
            photoBase64 = member?.photoBase64,
            subtitle = "${member?.appRole()?.labelEn ?: "User"} • ${group?.name ?: "TRE 4.0"}",
            onDismiss = { showHeroAvatarZoom = false }
        )
    }

    // Admin-Only Unique Banner Viewers List Modal
    if (isAdmin) {
        bannerViewersTarget?.let { targetBanner ->
            val bannerViews = remember(contentViews, targetBanner.id) {
                contentViews.filter {
                    it.contentType == ContentViewRecord.CONTENT_TYPE_BANNER && it.contentId == targetBanner.id
                }
            }
            ContentViewersDialog(
                title = targetBanner.title,
                contentTypeLabel = "Banner Viewers",
                views = bannerViews,
                adminUserProfiles = adminUserProfiles,
                onDismiss = { bannerViewersTarget = null }
            )
        }
    }

    // Full-screen Zoom Dialog for Current Affairs Image (Admin can Edit/Update or Delete)
    zoomedAffair?.let { currentBanner ->
        LaunchedEffect(currentBanner.id) {
            onTrackBannerView(currentBanner.id)
        }
        CurrentAffairZoomDialog(
            item = currentBanner,
            canManage = isAdmin,
            onEdit = {
                zoomedAffair = null
                editingAffair = currentBanner
                showUploadAffairSheet = true
            },
            onDelete = {
                onDeleteCurrentAffair(currentBanner.id)
                zoomedAffair = null
            },
            onDismiss = { zoomedAffair = null }
        )
    }

    // Group Admin Only Current Affairs Add / Update Bottom Sheet
    if (showUploadAffairSheet && isAdmin) {
        val targetEdit = editingAffair
        UploadCurrentAffairSheet(
            editingItem = targetEdit,
            onDismiss = {
                showUploadAffairSheet = false
                editingAffair = null
            },
            onPublish = { title, summary, date, source, cat, img ->
                if (targetEdit != null) {
                    onUpdateCurrentAffair(targetEdit.id, title, summary, date, source, cat, img)
                } else {
                    onPublishCurrentAffair(title, summary, date, source, cat, img)
                }
                showUploadAffairSheet = false
                editingAffair = null
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    val glass = LocalCoolingGlass.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = glass.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = glass.textMuted
        )
    }
}

@Composable
private fun DashboardTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    compactIcon: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    val iconBoxSize = if (compactIcon) 30.dp else 36.dp
    val iconSize = if (compactIcon) 16.dp else 20.dp
    val iconCorner = if (compactIcon) 9.dp else 11.dp
    val cardPad = if (compactIcon) 10.dp else 12.dp
    val tileHeight = if (compactIcon) 88.dp else 98.dp

    CoolingGlassCard(
        modifier = modifier
            .height(tileHeight)
            .testTag(tag),
        contentPadding = PaddingValues(cardPad),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(iconBoxSize)
                    .clip(RoundedCornerShape(iconCorner))
                    .background(accent.copy(alpha = 0.16f))
                    .border(0.8.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(iconCorner)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(iconSize))
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                AutoResizeSingleLineText(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxFontSize = 13.5.sp,
                    minFontSize = 10.sp,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                AutoResizeSingleLineText(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    maxFontSize = 10.5.sp,
                    minFontSize = 8.5.sp,
                    color = glass.textSecondary
                )
            }
        }
    }
}

@Composable
private fun CurrentAffairsSliderSection(
    items: List<CurrentAffairItem>,
    contentViews: List<ContentViewRecord>,
    canManage: Boolean,
    onBannerViewed: (String) -> Unit,
    onOpenViewers: (CurrentAffairItem) -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (CurrentAffairItem) -> Unit
) {
    val glass = LocalCoolingGlass.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Daily Current Affairs",
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = appText(
                        en = "Daily Current Affairs • PIB & Bihar Govt Official Updates",
                        defaultText = "दैनिक समसामयिकी • PIB & Bihar Govt Official Updates"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textMuted
                )
            }
            if (canManage) {
                CoolingGlassOutlinedButton(
                    text = "Add",
                    icon = Icons.Rounded.AddPhotoAlternate,
                    onClick = onAddClick
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (items.isEmpty()) {
            CoolingGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ca_default_banner_1795248467047),
                        contentDescription = "Daily Current Affairs Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x330B1F3A), Color(0xD90B1F3A))
                                )
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            CoolingGlassBadge(text = "PIB • BIHAR GOVT", color = IceCyan500)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Daily Current Affairs Image Feed",
                                style = MaterialTheme.typography.titleSmall,
                                color = CrystalWhite,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (canManage) "Tap 'Add' to publish today's image-based Current Affairs for all group Users."
                                else "Verified PIB & Bihar Current Affairs images published by Group Admin appear here in real time.",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrystalWhite.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            // Compact 1/4-Size 2x2 Grid (up to 4 banners visible simultaneously per page)
            val pages = remember(items) { items.chunked(4) }
            val pagerState = rememberPagerState(pageCount = { pages.size })
            LaunchedEffect(pagerState.currentPage, pages) {
                pages.getOrNull(pagerState.currentPage)?.forEach { visibleBanner ->
                    onBannerViewed(visibleBanner.id)
                }
            }
            LaunchedEffect(pages.size) {
                if (pages.size > 1) {
                    while (true) {
                        delay(6000L)
                        val next = (pagerState.currentPage + 1) % pages.size
                        pagerState.animateScrollToPage(next)
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .testTag("current_affairs_compact_pager"),
                pageSpacing = 10.dp
            ) { pageIndex ->
                val pageItems = pages[pageIndex]
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pageItems.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                val hasBannerImg = !item.imageBase64.isNullOrBlank()
                                val isGifBanner = remember(item.imageBase64) {
                                    ImageCodec.isGifBase64(item.imageBase64)
                                }
                                val uniqueViewerCount = remember(contentViews, item.id) {
                                    contentViews.count {
                                        it.contentType == ContentViewRecord.CONTENT_TYPE_BANNER && it.contentId == item.id
                                    }
                                }
                                CoolingGlassCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("banner_aspect_box_${item.id}"),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    onClick = { onItemClick(item) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 10f)
                                    ) {
                                        if (hasBannerImg) {
                                            AnimatedBase64Image(
                                                base64 = item.imageBase64,
                                                contentDescription = item.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                fallback = {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.img_ca_default_banner_1795248467047),
                                                        contentDescription = item.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(id = R.drawable.img_ca_default_banner_1795248467047),
                                                contentDescription = item.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        // Subtle Full-Size Expand Cue + optional LIVE GIF pill (Top-Left)
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xA6081B33)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.OpenInFull,
                                                    contentDescription = "Open full size banner",
                                                    tint = CrystalWhite,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                            if (isGifBanner) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .background(Color(0xD9059669))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "GIF",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 8.sp,
                                                        color = CrystalWhite,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                        }

                                        // Admin Viewers Counter Chip (Top-Right)
                                        if (canManage) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(5.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xCC081B33))
                                                    .border(0.8.dp, IceCyan500.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                                                    .testTag("banner_viewers_btn_${item.id}")
                                                    .clickable { onOpenViewers(item) }
                                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Visibility,
                                                    contentDescription = "View unique banner viewers",
                                                    tint = IceCyan500,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "$uniqueViewerCount",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    color = CrystalWhite,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Compact Gradient Caption Overlay (Bottom)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomStart)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color(0xE6081B33))
                                                    )
                                                )
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.BottomStart
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = item.dateLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 8.5.sp,
                                                        color = IceCyan500,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontSize = 10.5.sp,
                                                    lineHeight = 13.sp,
                                                    color = CrystalWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            if (pages.size > 1) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (i == pagerState.currentPage) 9.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == pagerState.currentPage) glass.accentCyan
                                    else glass.textMuted.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentAffairZoomDialog(
    item: CurrentAffairItem,
    canManage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    var zoom by remember(item.id) { mutableFloatStateOf(1f) }
    var offsetX by remember(item.id) { mutableFloatStateOf(0f) }
    var offsetY by remember(item.id) { mutableFloatStateOf(0f) }
    val hasBannerImg = !item.imageBase64.isNullOrBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF5040A16))
                .testTag("full_size_banner_viewer")
        ) {
            // Full-Size Edge-to-Edge Banner Image/GIF Canvas (Pinch-to-Zoom 1x..4x + Drag/Pan)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(item.id) {
                        detectTransformGestures { _, pan, zoomChange, _ ->
                            val nextZoom = (zoom * zoomChange).coerceIn(1f, 4f)
                            zoom = nextZoom
                            if (nextZoom > 1.02f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val transformModifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = zoom,
                        scaleY = zoom,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                if (hasBannerImg) {
                    AnimatedBase64Image(
                        base64 = item.imageBase64,
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        modifier = transformModifier,
                        fallback = {
                            Image(
                                painter = painterResource(id = R.drawable.img_ca_default_banner_1795248467047),
                                contentDescription = item.title,
                                contentScale = ContentScale.Fit,
                                modifier = transformModifier
                            )
                        }
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_ca_default_banner_1795248467047),
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        modifier = transformModifier
                    )
                }
            }

            // Floating Top Action Bar (Date, Source, Admin Edit/Delete, Close)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xE6040A16), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoolingGlassBadge(text = item.dateLabel, color = IceCyan500)
                    CoolingGlassBadge(text = item.source, color = SoftLavender500)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (canManage) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(CircleShape)
                                .background(Color(0x990A1A30))
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Update banner (Admin)",
                                tint = IceCyan500
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(CircleShape)
                                .background(Color(0x990A1A30))
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete banner",
                                tint = glass.incorrect
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC0A1A30))
                            .testTag("full_size_banner_close_btn")
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close full-size banner",
                            tint = CrystalWhite
                        )
                    }
                }
            }

            // Floating Bottom Details Bar (Headline, Summary, Zoom Reset)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xEB040A16), Color(0xFA040A16))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = CrystalWhite,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (zoom > 1.05f) {
                        Spacer(Modifier.width(8.dp))
                        CoolingGlassBadge(
                            text = appText("Reset Zoom", "ज़ूम रीसेट"),
                            color = IceCyan500,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    zoom = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                        )
                    }
                }
                if (item.summary.isNotBlank()) {
                    Text(
                        text = item.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = CrystalWhite.copy(alpha = 0.9f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = appText(
                        en = "Pinch to zoom • Drag to pan • Managed by Group Admin (${item.publishedByName})",
                        defaultText = "ज़ूम करने के लिए पिंच करें • पैन करने के लिए ड्रैग करें • Group Admin (${item.publishedByName})"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = CrystalWhite.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun UploadCurrentAffairSheet(
    editingItem: CurrentAffairItem? = null,
    onDismiss: () -> Unit,
    onPublish: (title: String, summary: String, date: String, source: String, category: String, img: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember(editingItem?.id) { mutableStateOf(editingItem?.title ?: "") }
    var summary by remember(editingItem?.id) { mutableStateOf(editingItem?.summary ?: "") }
    var dateLabel by remember(editingItem?.id) {
        mutableStateOf(editingItem?.dateLabel?.ifBlank { null } ?: IstClock.formatNow("dd MMM yyyy"))
    }
    var source by remember(editingItem?.id) { mutableStateOf(editingItem?.source?.ifBlank { null } ?: "PIB Official Release") }
    var category by remember(editingItem?.id) { mutableStateOf(editingItem?.category?.ifBlank { null } ?: "National & Bihar") }
    var imageBase64 by remember(editingItem?.id) { mutableStateOf(editingItem?.imageBase64) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    ImageCodec.encodeUriToBase64Result(
                        context = context,
                        uri = uri,
                        maxDimension = 720,
                        quality = 65,
                        allowGif = true
                    )
                }
                res.onSuccess { encoded ->
                    imageBase64 = encoded
                    uploadError = null
                }.onFailure { err ->
                    uploadError = err.localizedMessage ?: "Could not load selected GIF/PNG banner."
                }
            }
        }
    }

    CoolingGlassBottomSheet(
        title = if (editingItem != null) "Update Daily Banner (Admin Only)" else "Publish Daily Banner (Admin Only)",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            CoolingGlassTextField(value = title, onValueChange = { title = it }, label = "Headline (Hindi / English)")
            Spacer(Modifier.height(10.dp))
            CoolingGlassTextField(value = summary, onValueChange = { summary = it }, label = "Key Facts / Summary", singleLine = false, minLines = 2)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoolingGlassTextField(value = dateLabel, onValueChange = { dateLabel = it }, label = "Date", modifier = Modifier.weight(1f))
                CoolingGlassTextField(value = source, onValueChange = { source = it }, label = "Source (PIB/Bihar)", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            uploadError?.let { errMsg ->
                CoolingGlassStatusBanner(
                    message = errMsg,
                    isError = true,
                    onDismiss = { uploadError = null }
                )
                Spacer(Modifier.height(8.dp))
            }
            CoolingGlassOutlinedButton(
                text = if (imageBase64 == null) "Select Banner (GIF / PNG / JPG)" else "Banner Selected ✓ (Change GIF/PNG)",
                icon = Icons.Rounded.AddPhotoAlternate,
                modifier = Modifier.fillMaxWidth(),
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            )
            if (!imageBase64.isNullOrBlank()) {
                val previewBitmap = remember(imageBase64) { ImageCodec.decodeToImageBitmap(imageBase64) }
                val isGif = remember(imageBase64) { ImageCodec.isGifBase64(imageBase64) }
                val previewRatio = remember(previewBitmap) {
                    if (previewBitmap != null) {
                        computeBannerAspectRatio(previewBitmap.width, previewBitmap.height)
                    } else {
                        16f / 9f
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(previewRatio)
                        .heightIn(max = 180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, LocalCoolingGlass.current.accentCyan.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                ) {
                    AnimatedBase64Image(
                        base64 = imageBase64,
                        contentDescription = "Selected Banner Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isGif) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xD9059669))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE GIF",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = CrystalWhite,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isGif) {
                        "Live animated GIF banner ready for all users"
                    } else if (previewBitmap != null) {
                        "Auto-adjusted banner ratio: ${previewBitmap.width}×${previewBitmap.height}"
                    } else {
                        "Banner image ready"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalCoolingGlass.current.textSecondary
                )
            }
            Spacer(Modifier.height(14.dp))
            CoolingGlassButton(
                text = if (editingItem != null) "Save Banner Update" else "Publish to All Users",
                enabled = title.trim().length >= 3,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onPublish(title, summary, dateLabel, source, category, imageBase64) }
            )
        }
    }
}

/**
 * Admin-only modal displaying unique viewers for a Banner or Poll with User Name,
 * User ID, Mobile Number, and Viewing Date & Time.
 */
@Composable
fun ContentViewersDialog(
    title: String,
    contentTypeLabel: String,
    views: List<ContentViewRecord>,
    adminUserProfiles: List<UserProfile>,
    onDismiss: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    val profilesByUid = remember(adminUserProfiles) {
        adminUserProfiles.associateBy { it.userId.ifBlank { it.id } }
    }
    val dateFmt = remember { IstClock.newDateFormat("dd MMM yyyy, hh:mm a 'IST'") }

    CoolingGlassDialog(title = "$contentTypeLabel (${views.size})", onDismiss = onDismiss) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = glass.textPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        CoolingGlassBadge(
            text = "Total Unique Viewers: ${views.size}",
            color = glass.accentCyan
        )
        Spacer(Modifier.height(10.dp))

        if (views.isEmpty()) {
            CoolingGlassEmptyState(
                title = "No Views Recorded Yet",
                subtitle = "Unique viewers appear here automatically when Users view this content.",
                icon = Icons.Rounded.Visibility
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                views.forEach { rec ->
                    val matchedProfile = profilesByUid[rec.userId]
                    val resolvedName = rec.userName.ifBlank { matchedProfile?.displayName ?: "User" }
                    val resolvedPhone = WhatsAppHelper.normalizePhoneNumber(matchedProfile?.whatsappNumber)
                        ?: "Not Linked"
                    val viewedDate = rec.viewedAt?.toDate()
                    val viewedStr = viewedDate?.let { dateFmt.format(it) } ?: "Synced"

                    CoolingGlassCard(
                        strongFrost = true,
                        contentPadding = PaddingValues(12.dp),
                        modifier = Modifier.testTag("viewer_row_${rec.userId}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CoolingGlassAvatar(
                                name = resolvedName,
                                photoBase64 = matchedProfile?.photoBase64,
                                size = 40.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = resolvedName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "User ID: ${rec.userId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Mobile: $resolvedPhone",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (resolvedPhone != "Not Linked") Color(0xFF16A34A) else glass.textSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Viewed: $viewedStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.accentPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Computes a safe natural aspect ratio (width / height) for uploaded Current Affairs banners
 * so the banner container automatically adapts to landscape, square, or portrait uploads
 * without fixed-height cropping or distortion.
 */
fun computeBannerAspectRatio(width: Int, height: Int): Float {
    if (width <= 0 || height <= 0) return 16f / 9f
    return (width.toFloat() / height.toFloat()).coerceIn(0.45f, 3.2f)
}
