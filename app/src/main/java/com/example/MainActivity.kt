package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.GoogleAuthHelper
import com.example.data.firebase.TreRepository
import com.example.data.firebase.authStateFlow
import com.example.data.model.AppRole
import com.example.data.model.ContactAdminPolicy
import com.example.data.model.ContentViewRecord
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamTest
import com.example.data.model.LanguagePref
import com.example.data.model.RankCategory
import com.example.data.model.RankingPeriod
import com.example.data.model.TestAttempt
import com.example.data.model.UiState
import com.example.ui.glass.CoolingGlassAvatar
import com.example.ui.glass.CoolingGlassBackground
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassIconButton
import com.example.ui.glass.CoolingGlassLoadingCard
import com.example.ui.glass.CoolingGlassNavigationBar
import com.example.ui.glass.CoolingGlassStatusBanner
import com.example.ui.glass.CoolingGlassTopBar
import com.example.ui.glass.FounderBrandingCard
import com.example.ui.glass.GlassNavItem
import com.example.ui.glass.ProfilePhotoZoomDialog
import com.example.util.IstClock
import com.example.ui.screens.AboutFounderScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AspirantProfileSetupScreen
import com.example.ui.screens.ContactAdminScreen
import com.example.ui.screens.CoolingGlassAuthScreen
import com.example.ui.screens.DashboardDestination
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GroupOnboardingScreen
import com.example.ui.screens.LeaderboardAndAnalyticsScreen
import com.example.ui.screens.LiveExamScreen
import com.example.ui.screens.LocalCategoryIcons
import com.example.ui.screens.MandatoryUpdateDialog
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PollsAndDiscussionsScreen
import com.example.ui.screens.PreTestInstructionScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.QuestionBankScreen
import com.example.ui.screens.SetWiseRankScreen
import com.example.ui.screens.StudentLiveExamHubScreen
import com.example.ui.screens.TestResultAndReviewScreen
import com.example.ui.screens.TestsCatalogMode
import com.example.ui.screens.TestsCatalogScreen
import com.example.ui.screens.UserAppUpdateScreen
import com.example.ui.theme.CoolingGlassTheme
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.viewmodel.TreViewModel
import com.example.util.ExamSessionStore
import com.example.util.TestDraftStore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppSection(val navId: String, val title: String) {
    HOME("home", "TRE 4.0"),
    TESTS("tests", "Mock Tests"),
    COMPLETED_TESTS("completed_tests", "Completed Sets"),
    LIVE_EXAMS("live_exams", "Live Exam"),
    QUESTION_BANK("tests", "Question Bank"),
    POLLS("polls", "Photo Polls"),
    RANKS("ranks", "Rankings & Analytics"),
    PROFILE("profile", "My Profile"),
    NOTIFICATIONS("home", "Notifications"),
    ADMIN("home", "Admin Console"),
    CONTACT_ADMIN("profile", "Contact Admin"),
    APP_UPDATE("profile", "App Update"),
    ABOUT("profile", "About TRE 4.0")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Tre40RootApp()
        }
    }
}

@Composable
private fun Tre40RootApp() {
    val context = LocalContext.current
    val sessionStore = remember(context) { ExamSessionStore(context.applicationContext) }
    val currentUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    var darkGlassMode by remember { mutableStateOf(sessionStore.getDarkGlassMode()) }

    CoolingGlassTheme(darkTheme = darkGlassMode) {
        val user = currentUser
        if (user == null) {
            CoolingGlassAuthScreen(onAuthSuccess = {})
        } else {
            key(user.uid) {
                val vm: TreViewModel = viewModel(
                    key = user.uid,
                    factory = viewModelFactory {
                        initializer {
                            val app = context.applicationContext
                            val dbId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(dbId)
                            TreViewModel(
                                repository = TreRepository(db, Firebase.auth),
                                currentUserId = user.uid,
                                sessionStore = sessionStore
                            )
                        }
                    }
                )
                val rootProfileState by vm.userProfileState.collectAsStateWithLifecycle()
                val rootCategoryIcons by vm.categoryIcons.collectAsStateWithLifecycle()
                val activeAppLanguage = (rootProfileState as? UiState.Success)?.data?.langPreference() ?: LanguagePref.BILINGUAL
                CompositionLocalProvider(
                    LocalAppLanguage provides activeAppLanguage,
                    LocalCategoryIcons provides rootCategoryIcons
                ) {
                    Tre40AuthenticatedShell(
                        viewModel = vm,
                        userDisplaySuggestion = user.displayName.orEmpty(),
                        darkGlassMode = darkGlassMode,
                        onToggleDarkMode = {
                            darkGlassMode = it
                            vm.toggleDarkGlassMode(it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Tre40AuthenticatedShell(
    viewModel: TreViewModel,
    userDisplaySuggestion: String,
    darkGlassMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val glass = LocalCoolingGlass.current

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val activeGroup by viewModel.activeGroup.collectAsStateWithLifecycle()
    val currentMember by viewModel.currentMember.collectAsStateWithLifecycle()
    val groupMembers by viewModel.groupMembers.collectAsStateWithLifecycle()
    val groupTests by viewModel.groupTests.collectAsStateWithLifecycle()
    val groupAttempts by viewModel.groupAttempts.collectAsStateWithLifecycle()
    val currentAffairs by viewModel.currentAffairs.collectAsStateWithLifecycle()
    val dailyTargets by viewModel.dailyTargets.collectAsStateWithLifecycle()
    val polls by viewModel.polls.collectAsStateWithLifecycle()
    val userPollVotes by viewModel.userPollVotes.collectAsStateWithLifecycle()
    val discussions by viewModel.discussions.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val adminContact by viewModel.adminContact.collectAsStateWithLifecycle()
    val adminUserProfiles by viewModel.adminUserProfiles.collectAsStateWithLifecycle()
    val appUpdates by viewModel.appUpdates.collectAsStateWithLifecycle()
    val contentViews by viewModel.contentViews.collectAsStateWithLifecycle()
    val clearedNotificationIds by viewModel.clearedNotificationIds.collectAsStateWithLifecycle()
    val latestAvailableUpdate by viewModel.latestAvailableUpdate.collectAsStateWithLifecycle()
    val rankedLeaderboard by viewModel.rankedLeaderboard.collectAsStateWithLifecycle()
    val overallLeaderboard by viewModel.overallLeaderboard.collectAsStateWithLifecycle()
    val rankingPeriod by viewModel.rankingPeriod.collectAsStateWithLifecycle()
    val selectedRankCategory by viewModel.selectedRankCategory.collectAsStateWithLifecycle()
    val selectedRankingTestId by viewModel.selectedRankingTestId.collectAsStateWithLifecycle()
    val activeSetRankTest by viewModel.activeSetRankTest.collectAsStateWithLifecycle()
    val activeSetRankEntries by viewModel.activeSetRankEntries.collectAsStateWithLifecycle()
    val activeSetRankLoading by viewModel.activeSetRankLoading.collectAsStateWithLifecycle()
    val activeExam by viewModel.activeExam.collectAsStateWithLifecycle()
    val pendingInstructionTest by viewModel.pendingInstructionTest.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val solutionReattempts by viewModel.solutionReattempts.collectAsStateWithLifecycle()
    val pausedSessions by viewModel.pausedSessions.collectAsStateWithLifecycle()
    val liveExams by viewModel.liveExams.collectAsStateWithLifecycle()
    val liveExamSessions by viewModel.liveExamSessions.collectAsStateWithLifecycle()
    val completedAttempt by viewModel.latestCompletedAttempt.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    // Live 1-second digital watch strictly in Indian Standard Time (IST — Asia/Kolkata) next to TRE 4.0 on Home
    var liveClockText by remember {
        mutableStateOf(IstClock.formatNow("hh:mm:ss a 'IST'"))
    }
    var istDayRolloverMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        val fmt = IstClock.newDateFormat("hh:mm:ss a 'IST'")
        var lastIstDayKey = IstClock.todayIstKey()
        while (true) {
            val nowMs = System.currentTimeMillis()
            liveClockText = fmt.format(java.util.Date(nowMs))
            val currentDayKey = IstClock.todayIstKey(nowMs)
            if (currentDayKey != lastIstDayKey) {
                lastIstDayKey = currentDayKey
                istDayRolloverMillis = nowMs
            }
            delay(1000L)
        }
    }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(4000L)
            viewModel.clearFeedback()
        }
    }

    // Automatically restore interrupted active exam after process death / restart
    LaunchedEffect(groupTests, groupAttempts) {
        if (groupTests.isNotEmpty()) {
            viewModel.tryRestorePersistedExam(groupTests, groupAttempts)
        }
    }

    // 1. Auth Profile & Group Onboarding Gates
    when (val pState = profileState) {
        is UiState.Loading -> {
            CoolingGlassBackground(showHeroVeil = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CoolingGlassLoadingCard("Loading your TRE 4.0 Aspirant Workspace...")
                }
            }
            return
        }

        is UiState.Error -> {
            CoolingGlassBackground(showHeroVeil = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CoolingGlassStatusBanner(message = pState.message, isError = true)
                }
            }
            return
        }

        is UiState.Success -> {
            val profile = pState.data
            if (profile == null) {
                AspirantProfileSetupScreen(
                    suggestedName = userDisplaySuggestion,
                    busy = busy,
                    errorText = feedback?.takeIf { it.isError }?.message,
                    onRegister = { name, photo, whatsapp -> viewModel.registerAspirant(name, photo, whatsapp) }
                )
                return
            }
            if (profile.activeGroupId.isNullOrBlank()) {
                GroupOnboardingScreen(
                    aspirantName = profile.displayName,
                    busy = busy,
                    errorText = feedback?.takeIf { it.isError }?.message,
                    onJoinGroup = { code -> viewModel.joinExamGroup(code) },
                    onCreateGroup = { name, desc -> viewModel.createExamGroup(name, desc) }
                )
                return
            }

            // Hoisted shell navigation state + One-Step Back History Stack + Test/Result -> Rank exact return preservation
            var currentSection by remember { mutableStateOf(AppSection.HOME) }
            val navHistoryStack = remember { mutableStateListOf<AppSection>() }
            var returnToResultAfterRank by remember { mutableStateOf<Pair<ExamTest, TestAttempt>?>(null) }
            var testsInitialTab by remember { mutableIntStateOf(0) }
            var testsSelectedCategory by remember { mutableStateOf<RankCategory?>(null) }
            val testsCatalogListState = rememberLazyListState()
            var testsScrollToTopTick by remember { mutableIntStateOf(0) }
            var completedTestsInitialTab by remember { mutableIntStateOf(0) }
            var completedTestsSelectedCategory by remember { mutableStateOf<RankCategory?>(null) }
            val completedTestsCatalogListState = rememberLazyListState()
            var pollsInitialTab by remember { mutableIntStateOf(0) }
            var ranksInitialTab by remember { mutableIntStateOf(0) }
            var adminInitialTab by remember { mutableIntStateOf(0) }
            var editingTestTarget by remember { mutableStateOf<ExamTest?>(null) }
            val draftStore = remember(context) { TestDraftStore(context.applicationContext) }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

            LaunchedEffect(currentSection, testsScrollToTopTick) {
                if (currentSection == AppSection.TESTS && testsScrollToTopTick > 0) {
                    testsCatalogListState.scrollToItem(0)
                }
            }

            fun navigateToSection(target: AppSection, pushHistory: Boolean = true) {
                if (target != currentSection) {
                    if (pushHistory && (navHistoryStack.isEmpty() || navHistoryStack.last() != currentSection)) {
                        navHistoryStack.add(currentSection)
                    }
                    currentSection = target
                }
            }

            fun popOneStepBack() {
                val savedResultTarget = returnToResultAfterRank
                when {
                    drawerState.isOpen -> {
                        scope.launch { drawerState.close() }
                    }
                    currentSection == AppSection.RANKS && savedResultTarget != null -> {
                        val (savedTest, savedAttempt) = savedResultTarget
                        returnToResultAfterRank = null
                        if (navHistoryStack.isNotEmpty()) {
                            currentSection = navHistoryStack.removeAt(navHistoryStack.lastIndex)
                        }
                        viewModel.openAttemptReview(savedTest, savedAttempt)
                    }
                    currentSection == AppSection.QUESTION_BANK && editingTestTarget != null -> {
                        editingTestTarget = null
                        if (navHistoryStack.isNotEmpty()) {
                            currentSection = navHistoryStack.removeAt(navHistoryStack.lastIndex)
                        } else {
                            currentSection = AppSection.TESTS
                        }
                    }
                    navHistoryStack.isNotEmpty() -> {
                        currentSection = navHistoryStack.removeAt(navHistoryStack.lastIndex)
                    }
                    currentSection != AppSection.HOME -> {
                        currentSection = AppSection.HOME
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
            // 2. Full-Screen Exam Engine Overlay
            val exam = activeExam
            val instrTest = pendingInstructionTest
            val setRankTarget = activeSetRankTest
            val reviewPair = completedAttempt
            if (exam != null) {
                val examBookmarkedIndices = remember(bookmarks, exam.test.id) {
                    bookmarks.filter { it.testId == exam.test.id }.map { it.questionIndex.toInt() }.toSet()
                }
                LiveExamScreen(
                    examState = exam,
                    onSelectOption = { idx, opt -> viewModel.selectAnswer(idx, opt) },
                    onClearResponse = { idx -> viewModel.clearResponse(idx) },
                    onToggleReview = { idx -> viewModel.toggleMarkForReview(idx) },
                    onNavigateQuestion = { idx -> viewModel.navigateToQuestion(idx) },
                    onPause = { viewModel.pauseActiveExam() },
                    onSubmit = { viewModel.submitActiveExam { _, _ -> } },
                    onAbort = { viewModel.abortExamWithoutSubmit() },
                    bookmarkedIndices = examBookmarkedIndices,
                    onToggleBookmark = { idx, q -> viewModel.toggleQuestionBookmark(exam.test, idx, q) },
                    onReportQuestion = { qIdx, type, msg ->
                        viewModel.postComment(
                            targetType = "TEST",
                            targetId = "${exam.test.id}_Q${qIdx + 1}",
                            message = "Q${qIdx + 1}: $msg",
                            commentType = type
                        )
                    },
                    onChangeLanguage = { lang -> viewModel.setActiveExamLanguage(lang) },
                    onAdjustFontScale = { delta -> viewModel.adjustExamFontScale(delta) },
                    studentName = currentMember?.displayName ?: profile.displayName,
                    studentEmail = profile.email.orEmpty(),
                    studentId = viewModel.currentUserId,
                    onSecurityViolation = { type, detail ->
                        viewModel.recordLiveExamSecurityViolation(type, detail)
                    },
                    onDismissFinalFiveMinAlert = {
                        viewModel.dismissFinalFiveMinAlertBanner()
                    }
                )
            } else if (instrTest != null) {
                // 2b. Full-Screen Pre-Test Instructions Overlay
                PreTestInstructionScreen(
                    test = instrTest,
                    defaultLanguage = LanguagePref.fromString(profile.languagePref),
                    onBack = { viewModel.dismissTestInstructions() },
                    onStartExam = { chosenLang ->
                        viewModel.beginExamFromInstructions(instrTest, chosenLang)
                    }
                )
            } else if (setRankTarget != null) {
                // 2c. Full-Screen Dedicated Set-Wise Rank Overlay (Strictly isolated from Main Rank Card)
                SetWiseRankScreen(
                    test = setRankTarget,
                    entries = activeSetRankEntries,
                    currentUserId = viewModel.currentUserId,
                    isLoading = activeSetRankLoading,
                    onBack = { viewModel.closeDedicatedSetRank() }
                )
            } else if (reviewPair != null) {
                // 3. Full-Screen Test Result & Answer Review Overlay
                val (test, attempt) = reviewPair
                val testBookmarkedIndices = remember(bookmarks, test.id) {
                    bookmarks.filter { it.testId == test.id }.map { it.questionIndex.toInt() }.toSet()
                }
                val savedReattempt = remember(solutionReattempts, test.id, viewModel.currentUserId) {
                    solutionReattempts.firstOrNull {
                        it.testId == test.id && it.userId == viewModel.currentUserId
                    }
                }
                TestResultAndReviewScreen(
                    test = test,
                    attempt = attempt,
                    member = currentMember,
                    overallRanking = overallLeaderboard,
                    allAttemptsForTest = groupAttempts.filter { it.testId == test.id },
                    onBack = {
                        returnToResultAfterRank = null
                        viewModel.closeAttemptReview()
                    },
                    onReportQuestion = { qIdx, type, msg ->
                        viewModel.postComment(
                            targetType = "TEST",
                            targetId = "${test.id}_Q${qIdx + 1}",
                            message = "Q${qIdx + 1}: $msg",
                            commentType = type
                        )
                    },
                    onBannerMessage = { msg, isErr -> viewModel.showBanner(msg, isErr) },
                    bookmarkedIndices = testBookmarkedIndices,
                    savedReattempt = savedReattempt,
                    onToggleBookmark = { idx, q -> viewModel.toggleQuestionBookmark(test, idx, q) },
                    onRecordReattempt = { reMap ->
                        viewModel.recordSolutionReattempt(test, reMap, test.parsedQuestions())
                    },
                    onOpenFullLeaderboard = {
                        viewModel.openDedicatedSetRank(test)
                    }
                )
            } else {
            // 4. Main Cooling Glass Application Shell with Drawer + Bottom Nav

            val isAdmin = currentMember?.appRole() in listOf(AppRole.GROUP_ADMIN, AppRole.SUPER_ADMIN)
            val showContactAdmin = remember(isAdmin, viewModel.currentUserId, adminContact) {
                ContactAdminPolicy.shouldShowContactAdmin(isAdmin, viewModel.currentUserId, adminContact)
            }
            val visibleNotifications = remember(notifications, clearedNotificationIds) {
                notifications.filterNot { it.id in clearedNotificationIds }
            }
            var showDrawerAvatarZoom by remember { mutableStateOf(false) }
            val myAttempts = remember(groupAttempts, viewModel.currentUserId) {
                groupAttempts.filter { it.userId == viewModel.currentUserId }
            }
            val myOverallRank = overallLeaderboard.firstOrNull {
                it.member.userId == viewModel.currentUserId && it.testsAttempted > 0
            }?.rank

            // One-Step BackHandler across all screens and overlays
            BackHandler(
                enabled = currentSection != AppSection.HOME ||
                    drawerState.isOpen ||
                    navHistoryStack.isNotEmpty() ||
                    returnToResultAfterRank != null
            ) {
                popOneStepBack()
            }

            val bottomNavItems = remember {
                listOf(
                    GlassNavItem("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
                    GlassNavItem("tests", "Tests", Icons.Rounded.Quiz, Icons.Outlined.Quiz),
                    GlassNavItem("completed_tests", "Completed", Icons.Rounded.TaskAlt, Icons.Outlined.TaskAlt),
                    GlassNavItem("live_exams", "Live Exam", Icons.Rounded.Podcasts, Icons.Outlined.Podcasts),
                    GlassNavItem("polls", "Polls", Icons.Rounded.Poll, Icons.Outlined.Poll),
                    GlassNavItem("ranks", "Ranks", Icons.Rounded.EmojiEvents, Icons.Outlined.EmojiEvents),
                    GlassNavItem("profile", "Profile", Icons.Rounded.Person, Icons.Outlined.Person)
                )
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                scrimColor = glass.scrimColor,
                drawerContent = {
                    val drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                    ModalDrawerSheet(
                        drawerContainerColor = glass.drawerSurface,
                        drawerTonalElevation = 0.dp,
                        drawerShape = drawerShape,
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .border(1.2.dp, glass.glassBorder, drawerShape)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(glass.overlaySurfaceTop, glass.overlaySurfaceBottom)
                                    )
                                )
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CoolingGlassAvatar(
                                    name = currentMember?.displayName ?: profile.displayName,
                                    photoBase64 = currentMember?.photoBase64 ?: profile.photoBase64,
                                    size = 54.dp,
                                    onClick = { showDrawerAvatarZoom = true }
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentMember?.displayName ?: profile.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = glass.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = activeGroup?.name ?: "TRE 4.0 Group",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.textSecondary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    CoolingGlassBadge(
                                        text = currentMember?.appRole()?.labelEn ?: "User",
                                        color = glass.accentPrimary
                                    )
                                }
                            }
                            if (showDrawerAvatarZoom) {
                                ProfilePhotoZoomDialog(
                                    name = currentMember?.displayName ?: profile.displayName,
                                    photoBase64 = currentMember?.photoBase64 ?: profile.photoBase64,
                                    subtitle = "${currentMember?.appRole()?.labelEn ?: "User"} • ${activeGroup?.name ?: "TRE 4.0"}",
                                    onDismiss = { showDrawerAvatarZoom = false }
                                )
                            }
                            Spacer(Modifier.height(10.dp))

                            DrawerGlassItem("Dashboard", Icons.Rounded.Home, currentSection == AppSection.HOME) {
                                navigateToSection(AppSection.HOME)
                                scope.launch { drawerState.close() }
                            }
                            DrawerGlassItem("Mock Tests (Pending & Paused)", Icons.Rounded.Quiz, currentSection == AppSection.TESTS) {
                                testsInitialTab = 0
                                navigateToSection(AppSection.TESTS)
                                scope.launch { drawerState.close() }
                            }
                            DrawerGlassItem("Completed Sets & PDFs", Icons.Rounded.TaskAlt, currentSection == AppSection.COMPLETED_TESTS) {
                                completedTestsInitialTab = 0
                                navigateToSection(AppSection.COMPLETED_TESTS)
                                scope.launch { drawerState.close() }
                            }
                            DrawerGlassItem("Live Exam Arena", Icons.Rounded.Podcasts, currentSection == AppSection.LIVE_EXAMS) {
                                navigateToSection(AppSection.LIVE_EXAMS)
                                scope.launch { drawerState.close() }
                            }
                            if (isAdmin) {
                                DrawerGlassItem("Question Bank & Import", Icons.AutoMirrored.Rounded.LibraryBooks, currentSection == AppSection.QUESTION_BANK) {
                                    editingTestTarget = null
                                    navigateToSection(AppSection.QUESTION_BANK)
                                    scope.launch { drawerState.close() }
                                }
                            }
                            DrawerGlassItem("Interactive Photo Polls", Icons.Rounded.Poll, currentSection == AppSection.POLLS && pollsInitialTab == 0) {
                                pollsInitialTab = 0
                                navigateToSection(AppSection.POLLS)
                                scope.launch { drawerState.close() }
                            }
                            DrawerGlassItem("Doubt Session & Solutions", Icons.Rounded.Forum, currentSection == AppSection.POLLS && pollsInitialTab == 1) {
                                pollsInitialTab = 1
                                navigateToSection(AppSection.POLLS)
                                scope.launch { drawerState.close() }
                            }
                            DrawerGlassItem("Rankings & Analytics", Icons.Rounded.Assessment, currentSection == AppSection.RANKS) {
                                ranksInitialTab = 0
                                navigateToSection(AppSection.RANKS)
                                scope.launch { drawerState.close() }
                            }
                            DrawerGlassItem("Notifications (${visibleNotifications.size})", Icons.Rounded.Notifications, currentSection == AppSection.NOTIFICATIONS) {
                                navigateToSection(AppSection.NOTIFICATIONS)
                                scope.launch { drawerState.close() }
                            }
                            if (showContactAdmin) {
                                DrawerGlassItem("Contact Admin (WhatsApp)", Icons.AutoMirrored.Rounded.Chat, currentSection == AppSection.CONTACT_ADMIN) {
                                    navigateToSection(AppSection.CONTACT_ADMIN)
                                    scope.launch { drawerState.close() }
                                }
                            }
                            DrawerGlassItem(
                                label = if (latestAvailableUpdate != null) "App Update (NEW)" else "App Update",
                                icon = Icons.Rounded.SystemUpdate,
                                selected = currentSection == AppSection.APP_UPDATE
                            ) {
                                navigateToSection(AppSection.APP_UPDATE)
                                scope.launch { drawerState.close() }
                            }
                            if (isAdmin) {
                                DrawerGlassItem("Admin Control Center", Icons.Rounded.AdminPanelSettings, currentSection == AppSection.ADMIN) {
                                    adminInitialTab = 0
                                    navigateToSection(AppSection.ADMIN)
                                    scope.launch { drawerState.close() }
                                }
                            }
                            DrawerGlassItem("Profile & Settings", Icons.Rounded.Person, currentSection == AppSection.PROFILE) {
                                navigateToSection(AppSection.PROFILE)
                                scope.launch { drawerState.close() }
                            }
                            DrawerGlassItem("About TRE 4.0 & Founder", Icons.Rounded.Info, currentSection == AppSection.ABOUT) {
                                navigateToSection(AppSection.ABOUT)
                                scope.launch { drawerState.close() }
                            }

                            Spacer(Modifier.height(12.dp))
                            FounderBrandingCard(compact = true)
                        }
                    }
                }
            ) {
                val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                CoolingGlassBackground {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = {
                            val isSubScreen = currentSection in listOf(
                                AppSection.QUESTION_BANK,
                                AppSection.NOTIFICATIONS,
                                AppSection.ADMIN,
                                AppSection.CONTACT_ADMIN,
                                AppSection.APP_UPDATE,
                                AppSection.ABOUT
                            ) || (currentSection == AppSection.RANKS && returnToResultAfterRank != null)
                            CoolingGlassTopBar(
                                title = currentSection.title,
                                subtitle = activeGroup?.let { "${it.name} • Code: ${it.inviteCode}" },
                                clockText = if (currentSection == AppSection.HOME) liveClockText else null,
                                onBack = if (isSubScreen) {
                                    { popOneStepBack() }
                                } else null,
                                navigationIcon = if (!isSubScreen) Icons.Rounded.Menu else null,
                                onNavigationClick = if (!isSubScreen) {
                                    { scope.launch { drawerState.open() } }
                                } else null,
                                actions = {
                                    CoolingGlassIconButton(
                                        icon = if (darkGlassMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                        contentDescription = "Toggle Cooling Glass Theme",
                                        onClick = { onToggleDarkMode(!darkGlassMode) }
                                    )
                                    CoolingGlassIconButton(
                                        icon = Icons.Rounded.Notifications,
                                        contentDescription = "Notifications",
                                        onClick = { navigateToSection(AppSection.NOTIFICATIONS) }
                                    )
                                }
                            )
                        },
                        bottomBar = {
                            if (!isImeVisible) {
                                CoolingGlassNavigationBar(
                                    items = bottomNavItems,
                                    selectedId = currentSection.navId,
                                    onSelect = { navId ->
                                        returnToResultAfterRank = null
                                        val target = when (navId) {
                                            "home" -> AppSection.HOME
                                            "tests" -> AppSection.TESTS
                                            "completed_tests" -> AppSection.COMPLETED_TESTS
                                            "live_exams" -> AppSection.LIVE_EXAMS
                                            "polls" -> {
                                                pollsInitialTab = 0
                                                AppSection.POLLS
                                            }
                                            "ranks" -> {
                                                ranksInitialTab = 0
                                                AppSection.RANKS
                                            }
                                            else -> AppSection.PROFILE
                                        }
                                        navigateToSection(target)
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                        ) {
                            when (currentSection) {
                                AppSection.HOME -> DashboardScreen(
                                    member = currentMember,
                                    group = activeGroup,
                                    overallRanking = overallLeaderboard,
                                    currentAffairs = currentAffairs,
                                    tests = groupTests,
                                    unreadNotificationsCount = visibleNotifications.size,
                                    hasAvailableUpdate = latestAvailableUpdate != null,
                                    showContactAdmin = showContactAdmin,
                                    contentViews = contentViews,
                                    adminUserProfiles = adminUserProfiles,
                                    onTrackBannerView = { itemId ->
                                        viewModel.trackContentView(itemId, ContentViewRecord.CONTENT_TYPE_BANNER)
                                    },
                                    pausedSessions = pausedSessions,
                                    onResumePausedTest = { test -> viewModel.startExam(test) },
                                    dailyTargets = dailyTargets,
                                    myAttempts = myAttempts,
                                    nowMillis = istDayRolloverMillis,
                                    onOpenTargetTest = { targetTest ->
                                        val existingAttempt = myAttempts
                                            .filter { it.testId == targetTest.id }
                                            .maxByOrNull { it.submittedAt?.seconds ?: 0L }
                                        if (existingAttempt != null) {
                                            viewModel.openAttemptReview(targetTest, existingAttempt)
                                        } else {
                                            viewModel.openTestInstructions(targetTest)
                                        }
                                    },
                                    onSaveDailyTarget = { existingId, test, dateIst, title, subtitle, imgBase64 ->
                                        viewModel.saveDailyTargetTask(
                                            existingId = existingId,
                                            test = test,
                                            targetDateIst = dateIst,
                                            bannerTitle = title,
                                            bannerSubtitle = subtitle,
                                            bannerImageBase64 = imgBase64
                                        )
                                    },
                                    onDeleteDailyTarget = { targetId ->
                                        viewModel.deleteDailyTargetTask(targetId)
                                    },
                                    onNavigate = { dest ->
                                        when (dest) {
                                            DashboardDestination.COMMON_GS_TESTS -> {
                                                testsInitialTab = 1
                                                navigateToSection(AppSection.TESTS)
                                            }
                                            DashboardDestination.LANGUAGE_TESTS -> {
                                                testsInitialTab = 2
                                                navigateToSection(AppSection.TESTS)
                                            }
                                            DashboardDestination.PREVIOUS_TESTS,
                                            DashboardDestination.PDF_DOWNLOADS -> {
                                                completedTestsInitialTab = 0
                                                navigateToSection(AppSection.COMPLETED_TESTS)
                                            }
                                            DashboardDestination.QUESTION_BANK,
                                            DashboardDestination.CREATE_SET -> {
                                                if (isAdmin) {
                                                    editingTestTarget = null
                                                    navigateToSection(AppSection.QUESTION_BANK)
                                                } else {
                                                    viewModel.showBanner("Only Admin can access Question Bank & Set Creation.", true)
                                                }
                                            }
                                            DashboardDestination.INTERACTIVE_POLLS -> {
                                                pollsInitialTab = 0
                                                navigateToSection(AppSection.POLLS)
                                            }
                                            DashboardDestination.DISCUSSIONS -> {
                                                pollsInitialTab = 1
                                                navigateToSection(AppSection.POLLS)
                                            }
                                            DashboardDestination.LEADERBOARD -> {
                                                ranksInitialTab = 0
                                                viewModel.setRankingPeriod(RankingPeriod.INDIVIDUAL_TEST)
                                                navigateToSection(AppSection.RANKS)
                                            }
                                            DashboardDestination.WEEKLY_MONTHLY_RANK -> {
                                                ranksInitialTab = 0
                                                viewModel.setRankingPeriod(RankingPeriod.WEEKLY)
                                                navigateToSection(AppSection.RANKS)
                                            }
                                            DashboardDestination.MONTHLY_REPORT -> {
                                                ranksInitialTab = 0
                                                viewModel.setRankingPeriod(RankingPeriod.MONTHLY)
                                                navigateToSection(AppSection.RANKS)
                                            }
                                            DashboardDestination.ALL_SCORES -> {
                                                ranksInitialTab = 1
                                                navigateToSection(AppSection.RANKS)
                                            }
                                            DashboardDestination.MY_PERFORMANCE -> {
                                                ranksInitialTab = 2
                                                navigateToSection(AppSection.RANKS)
                                            }
                                            DashboardDestination.NOTIFICATIONS -> navigateToSection(AppSection.NOTIFICATIONS)
                                            DashboardDestination.GROUP_INFO,
                                            DashboardDestination.PROFILE_SETTINGS -> navigateToSection(AppSection.PROFILE)
                                            DashboardDestination.CONTACT_ADMIN -> {
                                                if (showContactAdmin) {
                                                    navigateToSection(AppSection.CONTACT_ADMIN)
                                                }
                                            }
                                            DashboardDestination.APP_UPDATE -> navigateToSection(AppSection.APP_UPDATE)
                                            DashboardDestination.ADMIN_CONSOLE -> {
                                                if (isAdmin) {
                                                    adminInitialTab = 0
                                                    navigateToSection(AppSection.ADMIN)
                                                } else viewModel.showBanner("Admin access required.", true)
                                            }
                                            DashboardDestination.ABOUT_FOUNDER -> navigateToSection(AppSection.ABOUT)
                                        }
                                    },
                                    onPublishStarterSets = { viewModel.publishStarterOfficialPapers() },
                                    onPublishCurrentAffair = { t, s, d, src, cat, img ->
                                        viewModel.publishCurrentAffair(t, s, d, src, cat, img)
                                    },
                                    onUpdateCurrentAffair = { id, t, s, d, src, cat, img ->
                                        viewModel.updateCurrentAffair(id, t, s, d, src, cat, img)
                                    },
                                    onDeleteCurrentAffair = { id -> viewModel.deleteCurrentAffair(id) }
                                )

                                AppSection.TESTS -> TestsCatalogScreen(
                                    tests = groupTests,
                                    myAttempts = myAttempts,
                                    currentMember = currentMember,
                                    initialFilterTab = testsInitialTab,
                                    selectedCategoryState = testsSelectedCategory,
                                    onSelectCategoryState = { testsSelectedCategory = it },
                                    catalogListState = testsCatalogListState,
                                    pausedSessions = pausedSessions,
                                    catalogMode = TestsCatalogMode.PENDING_AND_PAUSED_ONLY,
                                    onStartTest = { test -> viewModel.openTestInstructions(test) },
                                    onViewResult = { test, att -> viewModel.openAttemptReview(test, att) },
                                    onViewTestRanking = { test ->
                                        viewModel.openDedicatedSetRank(test)
                                    },
                                    onPublishStarterSets = { viewModel.publishStarterOfficialPapers() },
                                    onBannerMessage = { msg, err -> viewModel.showBanner(msg, err) }
                                )

                                AppSection.COMPLETED_TESTS -> TestsCatalogScreen(
                                    tests = groupTests,
                                    myAttempts = myAttempts,
                                    currentMember = currentMember,
                                    initialFilterTab = completedTestsInitialTab,
                                    selectedCategoryState = completedTestsSelectedCategory,
                                    onSelectCategoryState = { completedTestsSelectedCategory = it },
                                    catalogListState = completedTestsCatalogListState,
                                    pausedSessions = pausedSessions,
                                    catalogMode = TestsCatalogMode.COMPLETED_ONLY,
                                    onStartTest = { test -> viewModel.openTestInstructions(test) },
                                    onViewResult = { test, att -> viewModel.openAttemptReview(test, att) },
                                    onViewTestRanking = { test ->
                                        viewModel.openDedicatedSetRank(test)
                                    },
                                    onPublishStarterSets = { viewModel.publishStarterOfficialPapers() },
                                    onBannerMessage = { msg, err -> viewModel.showBanner(msg, err) }
                                )

                                AppSection.LIVE_EXAMS -> StudentLiveExamHubScreen(
                                    liveExams = liveExams,
                                    liveExamSessions = liveExamSessions,
                                    tests = groupTests,
                                    attempts = groupAttempts,
                                    currentUserId = viewModel.currentUserId,
                                    isAdmin = isAdmin,
                                    onRegisterLiveExam = { liveExam, enterWaitingRoom ->
                                        viewModel.registerForLiveExam(liveExam, enterWaitingRoom)
                                    },
                                    onStartLiveExam = { liveExam ->
                                        viewModel.startScheduledLiveExam(liveExam)
                                    },
                                    onViewLiveExamResult = { test, att ->
                                        viewModel.openAttemptReview(test, att)
                                    },
                                    onOpenLiveExamRank = { test ->
                                        viewModel.openDedicatedSetRank(test)
                                    },
                                    onOpenAdminLiveExams = {
                                        adminInitialTab = 8
                                        navigateToSection(AppSection.ADMIN)
                                    }
                                )

                                AppSection.QUESTION_BANK -> QuestionBankScreen(
                                    currentMember = currentMember,
                                    busy = busy,
                                    existingTests = groupTests,
                                    editingTest = editingTestTarget,
                                    draftStore = draftStore,
                                    onCancelEdit = {
                                        popOneStepBack()
                                    },
                                    onCreateTestSet = { title, type, diff, dur, qs, pubNow, unattPen, releaseMode, iconKey, customIconB64, bannerImgB64 ->
                                        viewModel.createAndPublishTest(
                                            title = title,
                                            paperType = type,
                                            difficulty = diff,
                                            durationMinutes = dur,
                                            questions = qs,
                                            publishImmediately = pubNow,
                                            unattemptedPenalty = unattPen,
                                            answerReleaseMode = releaseMode,
                                            iconKey = iconKey,
                                            customIconBase64 = customIconB64,
                                            bannerImageBase64 = bannerImgB64,
                                            onDone = {
                                                testsInitialTab = 0
                                                testsSelectedCategory = null
                                                testsScrollToTopTick++
                                                navigateToSection(AppSection.TESTS)
                                            }
                                        )
                                    },
                                    onUpdateTestSet = { testId, title, diff, dur, qs, unattPen, releaseMode, iconKey, customIconB64, bannerImgB64 ->
                                        viewModel.updateExistingTest(
                                            testId = testId,
                                            title = title,
                                            difficulty = diff,
                                            durationMinutes = dur,
                                            questions = qs,
                                            unattemptedPenalty = unattPen,
                                            answerReleaseMode = releaseMode,
                                            iconKey = iconKey,
                                            customIconBase64 = customIconB64,
                                            bannerImageBase64 = bannerImgB64,
                                            onDone = {
                                                editingTestTarget = null
                                                popOneStepBack()
                                            }
                                        )
                                    },
                                    onBannerMessage = { msg, err -> viewModel.showBanner(msg, err) }
                                )

                                AppSection.POLLS -> PollsAndDiscussionsScreen(
                                    currentMember = currentMember,
                                    polls = polls,
                                    userPollVotes = userPollVotes,
                                    discussions = discussions,
                                    contentViews = contentViews,
                                    adminUserProfiles = adminUserProfiles,
                                    initialTab = pollsInitialTab,
                                    onTrackPollView = { pollId ->
                                        viewModel.trackContentView(pollId, ContentViewRecord.CONTENT_TYPE_POLL)
                                    },
                                    onLoadVote = { pollId -> viewModel.loadMyVoteForPoll(pollId) },
                                    onCreatePoll = { title, desc, img, opts, keyIdx ->
                                        viewModel.createQuestionPoll(title, desc, img, opts, keyIdx)
                                    },
                                    onCastVote = { poll, idx -> viewModel.castPollVote(poll, idx) },
                                    onFetchPrivateKey = { pollId, cb -> viewModel.fetchCreatorPrivatePollKey(pollId, cb) },
                                    onFinalizePoll = { pollId, idx, exp, st -> viewModel.finalizePoll(pollId, idx, exp, st) },
                                    onUploadSolution = { poll, img, txt -> viewModel.uploadSolutionToPoll(poll, img, txt) },
                                    onDeletePoll = { pollId -> viewModel.deletePoll(pollId) },
                                    onReportPoll = { pollId -> viewModel.reportPoll(pollId) },
                                    onPostComment = { tType, tId, msg, cType, pId, imgB64 ->
                                        viewModel.postComment(tType, tId, msg, cType, pId, imgB64)
                                    },
                                    onEditComment = { id, newMsg -> viewModel.editComment(id, newMsg) },
                                    onDeleteComment = { id -> viewModel.deleteComment(id) },
                                    onMarkHelpful = { id, helpful -> viewModel.markAnswerHelpful(id, helpful) }
                                )

                                AppSection.RANKS -> LeaderboardAndAnalyticsScreen(
                                    group = activeGroup,
                                    currentMember = currentMember,
                                    rankedList = rankedLeaderboard,
                                    overallRanked = overallLeaderboard,
                                    selectedPeriod = rankingPeriod,
                                    selectedCategory = selectedRankCategory,
                                    selectedTestId = selectedRankingTestId,
                                    tests = groupTests,
                                    allAttempts = groupAttempts,
                                    initialSubTab = ranksInitialTab,
                                    onSelectPeriod = { period -> viewModel.setRankingPeriod(period) },
                                    onSelectCategory = { cat -> viewModel.setRankCategory(cat, rankingPeriod) },
                                    onClearSetFilter = { viewModel.clearSelectedSetRanking() },
                                    onBannerMessage = { msg, err -> viewModel.showBanner(msg, err) },
                                    members = groupMembers,
                                    bookmarks = bookmarks,
                                    onRemoveBookmark = { bm ->
                                        val matchingTest = groupTests.firstOrNull { it.id == bm.testId }
                                            ?: ExamTest(id = bm.testId, groupId = bm.groupId, title = bm.testTitle)
                                        val qObj = ExamQuestion(
                                            number = bm.questionIndex.toInt() + 1,
                                            subject = bm.subject,
                                            text = bm.questionText,
                                            correctOption = bm.correctOption,
                                            explanation = bm.explanation
                                        )
                                        viewModel.toggleQuestionBookmark(matchingTest, bm.questionIndex.toInt(), qObj)
                                    }
                                )

                                AppSection.PROFILE -> ProfileAndSettingsScreen(
                                    profile = profile,
                                    member = currentMember,
                                    group = activeGroup,
                                    myRank = myOverallRank,
                                    myAttempts = myAttempts,
                                    myCreatedTestsCount = groupTests.count { it.creatorUid == viewModel.currentUserId },
                                    myPollsCount = polls.count { it.creatorUid == viewModel.currentUserId },
                                    tests = groupTests,
                                    darkGlassMode = darkGlassMode,
                                    busy = busy,
                                    hasAvailableUpdate = latestAvailableUpdate != null,
                                    showContactAdmin = showContactAdmin,
                                    onToggleDarkMode = onToggleDarkMode,
                                    onSaveProfile = { name, photo, lang, pubShare, whatsapp ->
                                        viewModel.updateProfile(name, photo, lang, pubShare, whatsapp)
                                    },
                                    onOpenAttemptReview = { test, att -> viewModel.openAttemptReview(test, att) },
                                    onOpenContactAdmin = { navigateToSection(AppSection.CONTACT_ADMIN) },
                                    onOpenAppUpdate = { navigateToSection(AppSection.APP_UPDATE) },
                                    onOpenAboutFounder = { navigateToSection(AppSection.ABOUT) },
                                    onSignOut = {
                                        GoogleAuthHelper.signOut(credentialManager, scope)
                                    },
                                    onBannerMessage = { msg, err -> viewModel.showBanner(msg, err) }
                                )

                                AppSection.NOTIFICATIONS -> NotificationsScreen(
                                    notifications = visibleNotifications,
                                    isAdmin = isAdmin,
                                    onSendNotification = { t, b, type -> viewModel.sendNotification(t, b, type) },
                                    onClearNotification = { notif -> viewModel.markNotificationViewedAndClear(notif.id) },
                                    onClearAllNotifications = {
                                        viewModel.clearAllNotificationHistory(visibleNotifications.map { it.id })
                                    },
                                    onNotificationClick = { notif ->
                                        viewModel.markNotificationViewedAndClear(notif.id)
                                        when (notif.type) {
                                            "APP_UPDATE" -> navigateToSection(AppSection.APP_UPDATE)
                                            "LIVE_EXAM" -> navigateToSection(AppSection.LIVE_EXAMS)
                                            "TEST" -> {
                                                testsInitialTab = 0
                                                navigateToSection(AppSection.TESTS)
                                            }
                                            "POLL" -> {
                                                pollsInitialTab = 0
                                                navigateToSection(AppSection.POLLS)
                                            }
                                            "RANKING" -> {
                                                ranksInitialTab = 0
                                                navigateToSection(AppSection.RANKS)
                                            }
                                        }
                                    }
                                )

                                AppSection.ADMIN -> AdminDashboardScreen(
                                    currentMember = currentMember,
                                    group = activeGroup,
                                    members = groupMembers,
                                    tests = groupTests,
                                    attempts = groupAttempts,
                                    polls = polls,
                                    overallRanking = overallLeaderboard,
                                    adminUserProfiles = adminUserProfiles,
                                    adminContact = adminContact,
                                    appUpdates = appUpdates,
                                    busy = busy,
                                    onApproveOrRejectTest = { tId, st -> viewModel.approveOrRejectTest(tId, st) },
                                    onApproveTestWithAccess = { tId, st, forAll, memberIds ->
                                        viewModel.approveTestWithAccess(tId, st, forAll, memberIds)
                                    },
                                    onEditTest = { test ->
                                        editingTestTarget = test
                                        navigateToSection(AppSection.QUESTION_BANK)
                                    },
                                    onUpdateTestIcon = { testId, iconKey, customB64 ->
                                        viewModel.updateTestSetIcon(testId, iconKey, customB64)
                                    },
                                    onDeleteTest = { testId -> viewModel.deleteTestSet(testId) },
                                    onClearAllOldSets = { viewModel.clearAllOldTestSets() },
                                    onWipeAllTrialData = { viewModel.wipeAllTrialData(draftStore) },
                                    onCreateNewSet = {
                                        editingTestTarget = null
                                        navigateToSection(AppSection.QUESTION_BANK)
                                    },
                                    onAssignRole = { uid, role -> viewModel.assignMemberRole(uid, role) },
                                    onSaveAdminContact = { name, photo, phone ->
                                        viewModel.saveAdminContactSettings(name, photo, phone)
                                    },
                                    onPublishAppUpdate = { vName, vCode, title, notes, type, minCode, url, size, sha, pkg ->
                                        viewModel.publishAppUpdateRelease(vName, vCode, title, notes, type, minCode, url, size, sha, pkg)
                                    },
                                    onToggleAppUpdatePublished = { updId, pub ->
                                        viewModel.toggleAppUpdatePublished(updId, pub)
                                    },
                                    onDeleteAppUpdate = { updId ->
                                        viewModel.deleteAppUpdateRelease(updId)
                                    },
                                    onPublishStarterSets = { viewModel.publishStarterOfficialPapers() },
                                    onSendNotification = { t, b, type -> viewModel.sendNotification(t, b, type) },
                                    onBannerMessage = { msg, err -> viewModel.showBanner(msg, err) },
                                    discussions = discussions,
                                    solutionReattempts = solutionReattempts,
                                    onAssembleFullLengthSet = { title, targetType, p1, p2, p3, p4, dur, pubNow, cutoff, pause ->
                                        viewModel.assembleFullLengthSet(
                                            title = title,
                                            targetSetType = targetType,
                                            part1CommonTest = p1,
                                            part2LanguageTest = p2,
                                            part3MathsTest = p3,
                                            fourthPartTest = p4,
                                            durationMinutes = dur,
                                            publishImmediately = pubNow,
                                            cutoffMarks = cutoff,
                                            pauseAllowed = pause
                                        )
                                    },
                                    onUpdateTestExamRules = { tId, pos, negN, negD, unatt, optE, cutoff, pause, langPass, minHi, minEn, langFail, tieBreak ->
                                        viewModel.updateTestExamRules(
                                            testId = tId,
                                            positiveMarks = pos,
                                            negativeNum = negN,
                                            negativeDen = negD,
                                            unattemptedPenalty = unatt,
                                            optionEMode = optE,
                                            cutoffMarks = cutoff,
                                            pauseAllowed = pause,
                                            langPassMarks = langPass,
                                            langMinHindi = minHi,
                                            langMinEnglish = minEn,
                                            langFailRankPolicy = langFail,
                                            tieBreakPolicy = tieBreak
                                        )
                                    },
                                    onResolveReportedIssue = { cId, res, note ->
                                        viewModel.resolveReportedIssue(cId, res, note)
                                    },
                                    liveExams = liveExams,
                                    liveExamSessions = liveExamSessions,
                                    onSaveOrScheduleLiveExam = { lExam, pubNow ->
                                        viewModel.saveOrScheduleLiveExam(lExam, pubNow)
                                    },
                                    onRemoveLiveExamUnlinkOnly = { lExam ->
                                        viewModel.removeLiveExamUnlinkOnly(lExam)
                                    },
                                    onToggleLiveExamPublished = { lExam, pub ->
                                        viewModel.toggleLiveExamPublished(lExam, pub)
                                    },
                                    onCancelLiveExam = { lExam ->
                                        viewModel.cancelScheduledLiveExam(lExam)
                                    },
                                    onForceStartLiveExam = { lExam ->
                                        viewModel.forceStartLiveExamEarly(lExam)
                                    },
                                    onPublishLiveExamResultsNow = { lExam, rel ->
                                        viewModel.publishLiveExamResultsNow(lExam, rel)
                                    },
                                    onModerateLiveExamSession = { sess, st, note ->
                                        viewModel.moderateLiveExamParticipantSession(sess, st, note)
                                    },
                                    onClearCompletedAndPastLiveExams = {
                                        viewModel.clearCompletedAndPastLiveExams()
                                    },
                                    onViewSetRank = { test ->
                                        viewModel.openDedicatedSetRank(test)
                                    },
                                    categoryIcons = LocalCategoryIcons.current,
                                    onSaveCategoryIcon = { cat, iconKey, customB64 ->
                                        viewModel.saveCategoryDefaultIcon(cat, iconKey, customB64)
                                    },
                                    onResetCategoryIcon = { cat ->
                                        viewModel.resetCategoryDefaultIcon(cat)
                                    },
                                    onUpdateTestMedia = { testId, iconKey, customB64, bannerB64 ->
                                        viewModel.updateTestSetIcon(
                                            testId = testId,
                                            iconKey = iconKey,
                                            customIconBase64 = customB64,
                                            bannerImageBase64 = bannerB64,
                                            updateBanner = true
                                        )
                                    },
                                    initialAdminTab = adminInitialTab
                                )

                                AppSection.CONTACT_ADMIN -> ContactAdminScreen(
                                    adminContact = adminContact,
                                    fallbackAdminName = activeGroup?.createdByName?.takeIf { it.isNotBlank() } ?: "Gopal Gupta",
                                    groupName = activeGroup?.name ?: "BPSSC TRE 4.0",
                                    onFeedback = { msg, err -> viewModel.showBanner(msg, err) }
                                )

                                AppSection.APP_UPDATE -> UserAppUpdateScreen(
                                    latestUpdate = latestAvailableUpdate,
                                    allPublishedUpdates = appUpdates.filter { it.published },
                                    onFeedback = { msg, err -> viewModel.showBanner(msg, err) }
                                )

                                AppSection.ABOUT -> AboutFounderScreen()
                            }

                            val mandatoryUpd = latestAvailableUpdate
                            if (!isAdmin && mandatoryUpd != null && mandatoryUpd.isMandatoryFor(BuildConfig.VERSION_CODE.toLong())) {
                                MandatoryUpdateDialog(
                                    update = mandatoryUpd,
                                    onFeedback = { msg, err -> viewModel.showBanner(msg, err) }
                                )
                            }

                        }
                    }
                }
            }
            } // end else (Main Shell)

            // Global Cooling Glass Feedback Banner Overlay — ALWAYS visible above both main shell AND full-screen overlays
            val banner = feedback
            if (banner != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .zIndex(30f)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    CoolingGlassStatusBanner(
                        message = banner.message,
                        isError = banner.isError,
                        onDismiss = { viewModel.clearFeedback() }
                    )
                }
            }
            } // end root Box
        }
    }
}

@Composable
private fun DrawerGlassItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) glass.accentPrimary.copy(alpha = 0.16f) else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) glass.accentCyan.copy(alpha = 0.55f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) glass.accentPrimary else glass.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) glass.accentPrimary else glass.textPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
