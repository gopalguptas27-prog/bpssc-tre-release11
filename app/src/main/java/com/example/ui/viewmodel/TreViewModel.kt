package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.firebase.TreRepository
import com.example.data.model.AdminContactProfile
import com.example.data.model.AppRole
import com.example.data.model.AppUpdateRelease
import com.example.data.model.BookmarkedQuestion
import com.example.data.model.CategoryIconConfig
import com.example.data.model.ContentViewRecord
import com.example.data.model.CurrentAffairItem
import com.example.data.model.DailyTargetTask
import com.example.data.model.DiscussionComment
import com.example.data.model.ExamGroup
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamTest
import com.example.data.model.GroupMember
import com.example.data.model.GroupNotification
import com.example.data.model.LanguageFailRankPolicy
import com.example.data.model.LanguagePref
import com.example.data.model.LiveExam
import com.example.data.model.LiveExamDynamicStatus
import com.example.data.model.LiveExamSession
import com.example.data.model.OptionEMode
import com.example.data.model.PaperType
import com.example.data.model.QuestionPoll
import com.example.data.model.RankCategory
import com.example.data.model.RankedAspirant
import com.example.data.model.RankingPeriod
import com.example.data.model.SetRankEntry
import com.example.data.model.SetType
import com.example.data.model.SolutionReattemptRecord
import com.example.data.model.TestAttempt
import com.example.data.model.TieBreakPolicy
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.domain.BulkQuestionParser
import com.example.domain.ExamScoringEngine
import com.example.domain.OfficialStarterBank
import com.example.util.ExamSessionStore
import com.example.util.IstClock
import com.example.util.PersistedExamSession
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestionTimingBenchmark(
    val avgSecondsByQuestion: Map<Int, Long> = emptyMap(),
    val priorTimedAttemptsCount: Int = 0,
    val priorTotalAttemptsCount: Int = 0,
    val priorAvgTotalSeconds: Long = 0L
) {
    companion object {
        fun computeForTest(testId: String, attempts: List<TestAttempt>): QuestionTimingBenchmark {
            val matching = attempts.filter { it.testId == testId }
            if (matching.isEmpty()) return QuestionTimingBenchmark()

            val totalTimes = matching.map { it.timeSpentSeconds }.filter { it > 0L }
            val avgTotal = if (totalTimes.isNotEmpty()) totalTimes.sum() / totalTimes.size else 0L

            val sums = linkedMapOf<Int, Long>()
            val counts = linkedMapOf<Int, Int>()
            var timedAttempts = 0
            matching.forEach { att ->
                if (att.questionTimesSec.isNotEmpty()) {
                    timedAttempts++
                    att.questionTimesSec.forEach { (k, sec) ->
                        val idx = k.toIntOrNull()
                        if (idx != null && sec > 0L) {
                            sums[idx] = (sums[idx] ?: 0L) + sec
                            counts[idx] = (counts[idx] ?: 0) + 1
                        }
                    }
                }
            }
            val avgs = sums.mapValues { (idx, sum) ->
                val c = counts[idx] ?: 1
                (sum / c.coerceAtLeast(1)).coerceAtLeast(1L)
            }
            return QuestionTimingBenchmark(
                avgSecondsByQuestion = avgs,
                priorTimedAttemptsCount = timedAttempts,
                priorTotalAttemptsCount = matching.size,
                priorAvgTotalSeconds = avgTotal
            )
        }
    }
}

data class ActiveExamState(
    val test: ExamTest,
    val questions: List<ExamQuestion>,
    val currentIndex: Int = 0,
    val selectedAnswers: Map<Int, String> = emptyMap(),
    val markedForReview: Set<Int> = emptySet(),
    val visitedQuestions: Set<Int> = setOf(0),
    val remainingSeconds: Long = 0L,
    val totalDurationSeconds: Long = 0L,
    val endTimeEpochMillis: Long = 0L,
    val startedAtEpochMillis: Long = 0L,
    val questionTimesSec: Map<Int, Long> = emptyMap(),
    val priorQuestionAvgTimesSec: Map<Int, Long> = emptyMap(),
    val priorTimedAttemptsCount: Int = 0,
    val priorTotalAttemptsCount: Int = 0,
    val priorAvgTotalSeconds: Long = 0L,
    val activeLanguage: LanguagePref = LanguagePref.BILINGUAL,
    val fontScaleStep: Int = 0,
    val submitting: Boolean = false,
    // Live Exam & Secure Mode fields (all false/null for normal Practice & Test sets)
    val liveExamId: String? = null,
    val isLiveExam: Boolean = false,
    val secureModeEnabled: Boolean = false,
    val dynamicWatermark: Boolean = false,
    val focusLossDetection: Boolean = false,
    val copyPasteBlock: Boolean = false,
    val navigationExitLock: Boolean = false,
    val warningLimit: Int = 3,
    val violationCount: Int = 0,
    val violationLogs: List<String> = emptyList(),
    val finalFiveMinAlertShown: Boolean = false,
    val showFinalFiveMinAlertBanner: Boolean = false,
    val finalOneMinAlertShown: Boolean = false
)

data class FeedbackBanner(
    val message: String,
    val isError: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class TreViewModel(
    private val repository: TreRepository,
    val currentUserId: String,
    private val sessionStore: ExamSessionStore? = null,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _feedback = MutableStateFlow<FeedbackBanner?>(null)
    val feedback: StateFlow<FeedbackBanner?> = _feedback.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _darkGlassMode = MutableStateFlow(sessionStore?.getDarkGlassMode() ?: false)
    val darkGlassMode: StateFlow<Boolean> = _darkGlassMode.asStateFlow()

    private var restoredSessionAttempted = false

    private val _activeExam = MutableStateFlow<ActiveExamState?>(null)
    val activeExam: StateFlow<ActiveExamState?> = _activeExam.asStateFlow()

    private val _pendingInstructionTest = MutableStateFlow<ExamTest?>(null)
    val pendingInstructionTest: StateFlow<ExamTest?> = _pendingInstructionTest.asStateFlow()

    private val _latestCompletedAttempt = MutableStateFlow<Pair<ExamTest, TestAttempt>?>(null)
    val latestCompletedAttempt: StateFlow<Pair<ExamTest, TestAttempt>?> = _latestCompletedAttempt.asStateFlow()

    private val _pausedSessions = MutableStateFlow<Map<String, PersistedExamSession>>(
        sessionStore?.loadAllPausedSessions(currentUserId).orEmpty()
    )
    val pausedSessions: StateFlow<Map<String, PersistedExamSession>> = _pausedSessions.asStateFlow()

    private val _rankingPeriod = MutableStateFlow(RankingPeriod.INDIVIDUAL_TEST)
    val rankingPeriod: StateFlow<RankingPeriod> = _rankingPeriod.asStateFlow()

    private val _selectedRankCategory = MutableStateFlow(RankCategory.COMMON)
    val selectedRankCategory: StateFlow<RankCategory> = _selectedRankCategory.asStateFlow()

    private val _selectedRankingTestId = MutableStateFlow<String?>(null)
    val selectedRankingTestId: StateFlow<String?> = _selectedRankingTestId.asStateFlow()

    // Dedicated Set-Wise Rank screen state (Strictly isolated from Main Rank Card / Global Leaderboard)
    private val _activeSetRankTest = MutableStateFlow<ExamTest?>(null)
    val activeSetRankTest: StateFlow<ExamTest?> = _activeSetRankTest.asStateFlow()

    private val _activeSetRankLoading = MutableStateFlow(false)
    val activeSetRankLoading: StateFlow<Boolean> = _activeSetRankLoading.asStateFlow()

    private val _clearedNotificationIds = MutableStateFlow(
        sessionStore?.getClearedNotificationIds(currentUserId).orEmpty()
    )
    val clearedNotificationIds: StateFlow<Set<String>> = _clearedNotificationIds.asStateFlow()

    private val trackedViewKeys = mutableSetOf<String>()

    private val _userPollVotes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val userPollVotes: StateFlow<Map<String, Int>> = _userPollVotes.asStateFlow()

    private var timerJob: Job? = null

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error -> handleStreamCatch("userProfile", error) { emit(UiState.Error(it)) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val activeGroupIdFlow = userProfileState
        .map { (it as? UiState.Success)?.data?.activeGroupId }
        .distinctUntilChanged()

    val activeGroup: StateFlow<ExamGroup?> = activeGroupIdFlow
        .flatMapLatest { gid ->
            if (gid.isNullOrBlank()) flowOf(null)
            else repository.observeGroup(gid).catch { err -> unwrapOrThrow(err); emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val groupMembers: StateFlow<List<GroupMember>> = activeGroupIdFlow
        .flatMapLatest { gid ->
            if (gid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeGroupMembers(gid).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val currentMember: StateFlow<GroupMember?> = groupMembers
        .map { members -> members.firstOrNull { it.userId == currentUserId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val groupTests: StateFlow<List<ExamTest>> = combine(
        activeGroupIdFlow,
        currentMember.map { member ->
            when {
                member == null -> null
                member.appRole() in listOf(AppRole.GROUP_ADMIN, AppRole.SUPER_ADMIN) -> true
                else -> false
            }
        }.distinctUntilChanged()
    ) { gid, isAdmin -> gid to isAdmin }
        .flatMapLatest { (gid, isAdmin) ->
            if (gid.isNullOrBlank() || isAdmin == null) flowOf(emptyList())
            else repository.observeGroupTests(gid, includeUnpublished = isAdmin)
                .catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val groupAttempts: StateFlow<List<TestAttempt>> = activeGroupIdFlow
        .flatMapLatest { gid ->
            if (gid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeGroupAttempts(gid).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val currentAffairs: StateFlow<List<CurrentAffairItem>> = activeGroupIdFlow
        .flatMapLatest { gid ->
            if (gid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeCurrentAffairs(gid).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val dailyTargets: StateFlow<List<DailyTargetTask>> = activeGroupIdFlow
        .flatMapLatest { gid ->
            if (gid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeDailyTargets(gid).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val polls: StateFlow<List<QuestionPoll>> = activeGroupIdFlow
        .flatMapLatest { gid ->
            if (gid.isNullOrBlank()) flowOf(emptyList())
            else repository.observePolls(gid).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val discussions: StateFlow<List<DiscussionComment>> = activeGroupIdFlow
        .flatMapLatest { gid ->
            if (gid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeDiscussions(gid).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<GroupNotification>> = activeGroupIdFlow
        .flatMapLatest { gid ->
            if (gid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeNotifications(gid).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminContact: StateFlow<AdminContactProfile?> = activeGroupIdFlow
        .flatMapLatest { gid ->
            if (gid.isNullOrBlank()) flowOf(null)
            else repository.observeAdminContact(gid).catch { err -> unwrapOrThrow(err); emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val categoryIcons: StateFlow<Map<String, CategoryIconConfig>> = activeGroupIdFlow
        .flatMapLatest { gid ->
            if (gid.isNullOrBlank()) flowOf(emptyMap())
            else repository.observeCategoryIcons(gid).catch { err -> unwrapOrThrow(err); emit(emptyMap()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val liveExams: StateFlow<List<LiveExam>> = activeGroupIdFlow
        .flatMapLatest { gid ->
            if (gid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeLiveExams(gid).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val liveExamSessions: StateFlow<List<LiveExamSession>> = activeGroupIdFlow
        .flatMapLatest { gid ->
            if (gid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeLiveExamSessions(gid).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    /**
     * Strictly ADMIN-ONLY stream of registered users' private profiles (including optional WhatsApp numbers).
     * Emits emptyList() and never attaches a Firestore listener for regular USER role.
     */
    val adminUserProfiles: StateFlow<List<UserProfile>> = combine(
        activeGroupIdFlow,
        currentMember.map { it?.appRole()?.isAdminRole() == true }.distinctUntilChanged()
    ) { gid, isAdmin -> gid to isAdmin }
        .flatMapLatest { (gid, isAdmin) ->
            if (gid.isNullOrBlank() || !isAdmin) flowOf(emptyList())
            else repository.observeRegisteredUsersForAdmin(gid).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val bookmarks: StateFlow<List<BookmarkedQuestion>> = activeGroupIdFlow
        .flatMapLatest { gid ->
            if (gid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeUserBookmarks(gid, currentUserId).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val solutionReattempts: StateFlow<List<SolutionReattemptRecord>> = combine(
        activeGroupIdFlow,
        currentMember.map { it?.appRole()?.isAdminRole() == true }.distinctUntilChanged()
    ) { gid, isAdmin -> gid to isAdmin }
        .flatMapLatest { (gid, isAdmin) ->
            if (gid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeSolutionReattempts(gid, currentUserId, isAdmin).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val appUpdates: StateFlow<List<AppUpdateRelease>> = combine(
        activeGroupIdFlow,
        currentMember.map { it?.appRole()?.isAdminRole() == true }.distinctUntilChanged()
    ) { gid, isAdmin -> gid to isAdmin }
        .flatMapLatest { (gid, isAdmin) ->
            repository.observeAppUpdates(gid, isAdmin).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    /**
     * Strictly ADMIN-ONLY stream of deduplicated Banner & Poll view logs.
     */
    val contentViews: StateFlow<List<ContentViewRecord>> = combine(
        activeGroupIdFlow,
        currentMember.map { it?.appRole()?.isAdminRole() == true }.distinctUntilChanged()
    ) { gid, isAdmin -> gid to isAdmin }
        .flatMapLatest { (gid, isAdmin) ->
            if (gid.isNullOrBlank() || !isAdmin) flowOf(emptyList())
            else repository.observeContentViews(gid).catch { err -> unwrapOrThrow(err); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val latestAvailableUpdate: StateFlow<AppUpdateRelease?> = appUpdates
        .map { list ->
            val currentCode = BuildConfig.VERSION_CODE.toLong()
            list.filter { it.published && it.versionCode > currentCode }
                .maxByOrNull { it.versionCode }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val rankedLeaderboard: StateFlow<List<RankedAspirant>> = combine(
        combine(_rankingPeriod, _selectedRankCategory, _selectedRankingTestId, ::Triple),
        groupMembers,
        groupAttempts,
        groupTests
    ) { (period, category, selectedTestId), members, attempts, tests ->
        val activeSetTestId = if (period == RankingPeriod.INDIVIDUAL_TEST) selectedTestId else null
        ExamScoringEngine.computeCategoryRankings(
            period = period,
            category = category,
            members = members,
            attempts = attempts,
            tests = tests,
            selectedTestId = activeSetTestId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val overallLeaderboard: StateFlow<List<RankedAspirant>> = combine(
        groupMembers,
        groupAttempts,
        groupTests
    ) { members, attempts, tests ->
        ExamScoringEngine.computeRankings(RankingPeriod.OVERALL, members, attempts, tests)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Direct unlimited Firestore stream for the exact selected setId (NEVER uses global/other sets' data)
    private val directSetRankAttempts: StateFlow<List<TestAttempt>> = combine(
        activeGroupIdFlow,
        _activeSetRankTest
    ) { gid, test -> gid to test }
        .flatMapLatest { (gid, test) ->
            if (gid.isNullOrBlank() || test == null || test.id.isBlank()) {
                _activeSetRankLoading.value = false
                flowOf(emptyList())
            } else {
                _activeSetRankLoading.value = true
                repository.observeSetAttempts(gid, test.id)
                    .map { list ->
                        _activeSetRankLoading.value = false
                        list
                    }
                    .catch { err ->
                        _activeSetRankLoading.value = false
                        unwrapOrThrow(err)
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeSetRankEntries: StateFlow<List<SetRankEntry>> = combine(
        _activeSetRankTest,
        directSetRankAttempts,
        groupAttempts,
        groupMembers
    ) { setTest, directAttempts, allGroupAttempts, members ->
        if (setTest == null || setTest.id.isBlank()) {
            emptyList()
        } else {
            // Merge direct setId query results with groupAttempts strictly matching setTest.id
            // so all submitted users appear immediately with zero cap and zero cross-set leakage.
            val mergedByKey = LinkedHashMap<String, TestAttempt>()
            allGroupAttempts.asSequence()
                .filter { it.testId == setTest.id }
                .forEach { att ->
                    val k = att.id.ifBlank { "${att.testId}_${att.userId}" }
                    mergedByKey[k] = att
                }
            directAttempts.asSequence()
                .filter { it.testId == setTest.id }
                .forEach { att ->
                    val k = att.id.ifBlank { "${att.testId}_${att.userId}" }
                    mergedByKey[k] = att
                }
            ExamScoringEngine.computeSetWiseRankings(
                test = setTest,
                attempts = mergedByKey.values.toList(),
                members = members
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun clearFeedback() {
        _feedback.value = null
    }

    fun showBanner(message: String, isError: Boolean = false) {
        _feedback.value = FeedbackBanner(message, isError)
    }

    fun toggleDarkGlassMode(enabled: Boolean) {
        _darkGlassMode.value = enabled
        sessionStore?.setDarkGlassMode(enabled)
    }

    fun setRankCategory(category: RankCategory, period: RankingPeriod = _rankingPeriod.value) {
        _selectedRankingTestId.value = null
        _selectedRankCategory.value = category
        _rankingPeriod.value = period
    }

    fun setRankingPeriod(period: RankingPeriod, testId: String? = null) {
        _rankingPeriod.value = period
        if (!testId.isNullOrBlank()) {
            _selectedRankingTestId.value = testId
            groupTests.value.firstOrNull { it.id == testId }
                ?.let { RankCategory.fromTest(it) }
                ?.let { _selectedRankCategory.value = it }
        } else {
            _selectedRankingTestId.value = null
        }
    }

    fun openSetRanking(test: ExamTest): Boolean {
        val cat = RankCategory.fromTest(test) ?: return false
        _selectedRankCategory.value = cat
        _rankingPeriod.value = RankingPeriod.INDIVIDUAL_TEST
        _selectedRankingTestId.value = test.id
        return true
    }

    fun clearSelectedSetRanking() {
        _selectedRankingTestId.value = null
    }

    fun openDedicatedSetRank(test: ExamTest) {
        _activeSetRankTest.value = test
    }

    fun closeDedicatedSetRank() {
        _activeSetRankTest.value = null
        _activeSetRankLoading.value = false
    }

    fun trackContentView(contentId: String, contentType: String) {
        val gid = activeGroup.value?.id ?: return
        if (contentId.isBlank()) return
        val key = "${gid}_${contentType}_$contentId"
        if (!trackedViewKeys.add(key)) return
        val displayName = currentMember.value?.displayName
            ?: (userProfileState.value as? UiState.Success)?.data?.displayName
            ?: "User"
        viewModelScope.launch {
            try {
                repository.recordUniqueContentView(
                    groupId = gid,
                    contentId = contentId,
                    contentType = contentType,
                    userName = displayName
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Non-blocking telemetry failure must never interrupt UI
            }
        }
    }

    fun markNotificationViewedAndClear(notifId: String) {
        if (notifId.isBlank()) return
        val updated = sessionStore?.markNotificationCleared(currentUserId, notifId)
            ?: (_clearedNotificationIds.value + notifId)
        _clearedNotificationIds.value = updated
    }

    fun clearAllNotificationHistory(notifIds: List<String>) {
        if (notifIds.isEmpty()) return
        val updated = sessionStore?.markAllNotificationsCleared(currentUserId, notifIds)
            ?: (_clearedNotificationIds.value + notifIds)
        _clearedNotificationIds.value = updated
    }

    // ==================== ONBOARDING & PROFILE ====================

    fun registerAspirant(displayName: String, photoBase64: String?, whatsappNumber: String? = null) {
        if (_busy.value) return
        launchAction("User profile saved!") {
            repository.registerUserProfile(displayName, photoBase64, whatsappNumber)
        }
    }

    fun updateProfile(
        displayName: String,
        photoBase64: String?,
        languagePref: LanguagePref,
        publicSharing: Boolean,
        whatsappNumber: String? = null
    ) {
        if (_busy.value) return
        val groupId = activeGroup.value?.id
        launchAction("Profile settings updated!") {
            repository.updateUserProfile(displayName, photoBase64, languagePref, publicSharing, groupId, whatsappNumber)
        }
    }

    fun saveAdminContactSettings(
        adminName: String,
        adminPhotoBase64: String?,
        adminWhatsAppNumber: String?
    ) {
        if (_busy.value) return
        val gid = activeGroup.value?.id ?: return
        val isAdmin = currentMember.value?.appRole()?.isAdminRole() == true
        if (!isAdmin) {
            showBanner("Only Admin can update official Admin contact settings.", isError = true)
            return
        }
        launchAction("Official Admin WhatsApp Contact updated!") {
            repository.saveAdminContact(gid, adminName, adminPhotoBase64, adminWhatsAppNumber)
        }
    }

    fun createExamGroup(name: String, description: String) {
        if (_busy.value) return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _busy.value = true
            val res = repository.createGroup(name, description, profile)
            _busy.value = false
            res.onSuccess { grp ->
                showBanner("Group '${grp.name}' created! Invite Code: ${grp.inviteCode}")
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Failed to create group", isError = true)
            }
        }
    }

    fun joinExamGroup(inviteCode: String) {
        if (_busy.value) return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        launchAction("Joined TRE 4.0 group successfully!") {
            repository.joinGroupByCode(inviteCode, profile).map { }
        }
    }

    // ==================== QUESTION SETS & OFFICIAL STARTER PUBLISH ====================

    fun createAndPublishTest(
        title: String,
        paperType: PaperType,
        difficulty: String,
        durationMinutes: Int,
        questions: List<ExamQuestion>,
        publishImmediately: Boolean,
        unattemptedPenalty: Boolean = true,
        answerReleaseMode: String = "IMMEDIATE",
        iconKey: String? = null,
        customIconBase64: String? = null,
        bannerImageBase64: String? = null,
        onDone: () -> Unit = {}
    ) {
        if (_busy.value) return
        val gid = activeGroup.value?.id ?: return
        val member = currentMember.value ?: return
        val isAdmin = member.appRole().isAdminRole()
        if (!isAdmin) {
            showBanner("Access Denied: Only Admin can create or publish question sets.", isError = true)
            return
        }
        val existing = groupTests.value
        val finalTitle = if (com.example.data.model.TestSetNaming.isDuplicateTitle(title, existing)) {
            com.example.data.model.TestSetNaming.nextUniqueTitle(paperType, existing)
        } else {
            title.trim()
        }
        viewModelScope.launch {
            _busy.value = true
            val res = repository.createTestSet(
                groupId = gid,
                title = finalTitle,
                paperType = paperType,
                difficulty = difficulty,
                durationMinutes = durationMinutes,
                questions = questions,
                creator = member,
                publishImmediately = publishImmediately,
                unattemptedPenalty = unattemptedPenalty,
                answerReleaseMode = answerReleaseMode,
                iconKey = iconKey,
                customIconBase64 = customIconBase64,
                bannerImageBase64 = bannerImageBase64
            )
            _busy.value = false
            res.onSuccess {
                if (publishImmediately) {
                    repository.sendGroupNotification(
                        groupId = gid,
                        title = "New Test Published: $finalTitle",
                        body = "${questions.size} Questions • $durationMinutes Min. Attempt now!",
                        type = "TEST",
                        senderName = member.displayName,
                        targetAction = "TESTS"
                    )
                }
                showBanner(
                    if (publishImmediately) "Published '$finalTitle' to all group users!"
                    else "Saved '$finalTitle' to Admin Queue (hidden from users until published)."
                )
                onDone()
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Could not save question set", isError = true)
            }
        }
    }

    fun updateExistingTest(
        testId: String,
        title: String,
        difficulty: String,
        durationMinutes: Int,
        questions: List<ExamQuestion>,
        unattemptedPenalty: Boolean = true,
        answerReleaseMode: String = "IMMEDIATE",
        iconKey: String? = null,
        customIconBase64: String? = null,
        bannerImageBase64: String? = null,
        onDone: () -> Unit = {}
    ) {
        if (_busy.value) return
        val gid = activeGroup.value?.id ?: return
        val member = currentMember.value ?: return
        val isAdmin = member.appRole().isAdminRole()
        if (!isAdmin) {
            showBanner("Access Denied: Only Admin can edit question sets.", isError = true)
            return
        }
        viewModelScope.launch {
            _busy.value = true
            val res = repository.updateTestSet(
                groupId = gid,
                testId = testId,
                title = title,
                difficulty = difficulty,
                durationMinutes = durationMinutes,
                questions = questions,
                isAdmin = true,
                unattemptedPenalty = unattemptedPenalty,
                answerReleaseMode = answerReleaseMode,
                iconKey = iconKey,
                customIconBase64 = customIconBase64,
                bannerImageBase64 = bannerImageBase64
            )
            _busy.value = false
            res.onSuccess {
                showBanner("Test '$title' updated successfully!")
                onDone()
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Could not update test set", isError = true)
            }
        }
    }

    fun updateTestSetIcon(
        testId: String,
        iconKey: String?,
        customIconBase64: String?,
        bannerImageBase64: String? = null,
        updateBanner: Boolean = false
    ) {
        val gid = activeGroup.value?.id ?: return
        if (currentMember.value?.appRole()?.isAdminRole() != true) {
            showBanner("Access Denied: Only Admin can change set icons or banners.", isError = true)
            return
        }
        launchAction(if (updateBanner) "Set icon & banner updated!" else "Set icon updated!") {
            repository.updateTestIcon(gid, testId, iconKey, customIconBase64, bannerImageBase64, updateBanner)
        }
    }

    fun saveCategoryDefaultIcon(
        category: RankCategory,
        iconKey: String?,
        customIconBase64: String?
    ) {
        val gid = activeGroup.value?.id ?: return
        if (currentMember.value?.appRole()?.isAdminRole() != true) {
            showBanner("Access Denied: Only Admin can manage category icons.", isError = true)
            return
        }
        launchAction("${category.displayName} default icon saved for all sets!") {
            repository.saveCategoryDefaultIcon(gid, category, iconKey, customIconBase64)
        }
    }

    fun resetCategoryDefaultIcon(category: RankCategory) {
        val gid = activeGroup.value?.id ?: return
        if (currentMember.value?.appRole()?.isAdminRole() != true) {
            showBanner("Access Denied: Only Admin can reset category icons.", isError = true)
            return
        }
        launchAction("${category.displayName} icon reset to built-in default.") {
            repository.deleteCategoryDefaultIcon(gid, category)
        }
    }

    fun publishStarterOfficialPapers() {
        if (_busy.value) return
        val gid = activeGroup.value?.id ?: return
        val member = currentMember.value ?: return
        val canPublishDirect = member.appRole() in listOf(AppRole.GROUP_ADMIN, AppRole.SUPER_ADMIN)
        viewModelScope.launch {
            _busy.value = true
            val gsRes = repository.createTestSet(
                groupId = gid,
                title = "TRE 4.0 Official Common Paper Mock 01",
                paperType = PaperType.COMMON_GS,
                difficulty = "MODERATE",
                durationMinutes = PaperType.COMMON_GS.defaultDurationMinutes,
                questions = OfficialStarterBank.commonGs40Questions(),
                creator = member,
                publishImmediately = canPublishDirect
            )
            val langRes = repository.createTestSet(
                groupId = gid,
                title = "TRE 4.0 Language Qualifying Paper 01",
                paperType = PaperType.LANGUAGE,
                difficulty = "MODERATE",
                durationMinutes = PaperType.LANGUAGE.defaultDurationMinutes,
                questions = OfficialStarterBank.language30Questions(),
                creator = member,
                publishImmediately = canPublishDirect
            )
            val combinedRes = repository.createTestSet(
                groupId = gid,
                title = "TRE 4.0 Combined Test Paper 01",
                paperType = PaperType.COMBINED,
                difficulty = "MODERATE",
                durationMinutes = PaperType.COMBINED.defaultDurationMinutes,
                questions = OfficialStarterBank.combined70Questions(),
                creator = member,
                publishImmediately = canPublishDirect
            )
            _busy.value = false
            if (gsRes.isSuccess && langRes.isSuccess && combinedRes.isSuccess) {
                showBanner("Published 30m Common (40Q), 15m Language (30Q) & 45m Combined (70Q) Official Sets!")
            } else {
                showBanner(
                    gsRes.exceptionOrNull()?.localizedMessage
                        ?: langRes.exceptionOrNull()?.localizedMessage
                        ?: combinedRes.exceptionOrNull()?.localizedMessage
                        ?: "Failed to load starter sets",
                    isError = true
                )
            }
        }
    }

    fun approveOrRejectTest(testId: String, status: String) {
        val gid = activeGroup.value?.id ?: return
        val msg = when (status) {
            "PUBLISHED" -> "Set approved & made visible to all invited group aspirants!"
            "PENDING" -> "Set moved back to Pending Queue (hidden from members)."
            else -> "Test status updated to $status"
        }
        launchAction(msg) {
            repository.updateTestStatus(gid, testId, status)
        }
    }

    fun approveTestWithAccess(
        testId: String,
        status: String,
        approvedForAll: Boolean,
        approvedMemberIds: List<String>
    ) {
        val gid = activeGroup.value?.id ?: return
        val msg = when {
            status != "PUBLISHED" -> "Set moved back to Pending Queue (hidden from members)."
            approvedForAll -> "Set approved & made visible to ALL group members!"
            else -> "Set approved for ${approvedMemberIds.distinct().size} selected member(s) only!"
        }
        launchAction(msg) {
            repository.approveTestWithAccess(gid, testId, status, approvedForAll, approvedMemberIds)
        }
    }

    fun deleteTestSet(testId: String) {
        val gid = activeGroup.value?.id ?: return
        sessionStore?.clearPausedSession(currentUserId, testId)
        refreshPausedSessions()
        if (_selectedRankingTestId.value == testId) {
            _selectedRankingTestId.value = null
        }
        launchAction("Test set permanently removed.") {
            repository.deleteTestSet(gid, testId)
        }
    }

    fun clearAllOldTestSets() {
        if (_busy.value) return
        val gid = activeGroup.value?.id ?: return
        val ids = groupTests.value.map { it.id }.filter { it.isNotBlank() }
        if (ids.isEmpty()) {
            showBanner("No old test sets found to remove.")
            return
        }
        viewModelScope.launch {
            _busy.value = true
            val res = repository.deleteAllTestsInGroup(gid, ids)
            _busy.value = false
            res.onSuccess { count ->
                sessionStore?.clearAllSessions()
                refreshPausedSessions()
                _selectedRankingTestId.value = null
                showBanner("Removed $count old test set(s). Ready to add fresh sets!")
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Could not clear old sets", isError = true)
            }
        }
    }

    fun wipeAllTrialData(draftStore: com.example.util.TestDraftStore? = null) {
        if (_busy.value) return
        val gid = activeGroup.value?.id ?: return
        viewModelScope.launch {
            _busy.value = true
            timerJob?.cancel()
            val res = repository.wipeAllGroupTrialData(gid)
            _busy.value = false
            res.onSuccess { summary ->
                sessionStore?.clearAllSessions()
                draftStore?.loadAllDrafts()?.forEach { draftStore.deleteDraft(it.id) }
                refreshPausedSessions()
                _activeExam.value = null
                _latestCompletedAttempt.value = null
                _selectedRankingTestId.value = null
                showBanner(summary)
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Could not clear trial data", isError = true)
            }
        }
    }

    // ==================== LIVE ONLINE EXAMINATION ENGINE ====================

    fun tryRestorePersistedExam(tests: List<ExamTest>, attempts: List<TestAttempt>) {
        // Prune paused sessions for deleted or already-submitted tests
        val submittedIds = attempts.filter { it.userId == currentUserId }.map { it.testId }.toSet()
        val liveIds = tests.map { it.id }.toSet()
        val existingPaused = sessionStore?.loadAllPausedSessions(currentUserId).orEmpty()
        var pruned = false
        existingPaused.keys.forEach { tId ->
            if (tId in submittedIds || (tests.isNotEmpty() && tId !in liveIds)) {
                sessionStore?.clearPausedSession(currentUserId, tId)
                pruned = true
            }
        }
        if (pruned || _pausedSessions.value != existingPaused) {
            refreshPausedSessions()
        }

        if (restoredSessionAttempted || _activeExam.value != null) return
        val saved = sessionStore?.loadExamSession(currentUserId) ?: return
        if (saved.isPaused) return
        if (tests.isEmpty()) return
        restoredSessionAttempted = true

        val alreadySubmitted = attempts.any { it.testId == saved.testId && it.userId == currentUserId }
        if (alreadySubmitted) {
            sessionStore.clearExamSession()
            return
        }
        val matchingTest = tests.firstOrNull { it.id == saved.testId && it.groupId == saved.groupId } ?: return
        val questions = matchingTest.parsedQuestions()
        if (questions.isEmpty()) {
            sessionStore.clearExamSession()
            return
        }

        val remaining = ((saved.endTimeEpochMillis - clockMillis()) / 1000L).coerceAtLeast(0L)
        val bench = QuestionTimingBenchmark.computeForTest(matchingTest.id, attempts)
        val initialLang = (userProfileState.value as? UiState.Success)?.data?.langPreference() ?: LanguagePref.BILINGUAL
        val safeIdx = saved.currentIndex.coerceIn(0, questions.lastIndex)
        _activeExam.value = ActiveExamState(
            test = matchingTest,
            questions = questions,
            currentIndex = safeIdx,
            selectedAnswers = saved.selectedAnswers,
            markedForReview = saved.markedForReview,
            visitedQuestions = saved.visitedQuestions + safeIdx,
            remainingSeconds = remaining,
            totalDurationSeconds = saved.totalDurationSeconds.coerceAtLeast(60L),
            endTimeEpochMillis = saved.endTimeEpochMillis,
            startedAtEpochMillis = saved.startedAtEpochMillis.takeIf { it > 0L } ?: clockMillis(),
            questionTimesSec = saved.questionTimesSec,
            priorQuestionAvgTimesSec = bench.avgSecondsByQuestion,
            priorTimedAttemptsCount = bench.priorTimedAttemptsCount,
            priorTotalAttemptsCount = bench.priorTotalAttemptsCount,
            priorAvgTotalSeconds = bench.priorAvgTotalSeconds,
            activeLanguage = initialLang,
            liveExamId = saved.liveExamId,
            isLiveExam = saved.isLiveExam,
            secureModeEnabled = saved.secureModeEnabled,
            dynamicWatermark = saved.dynamicWatermark,
            focusLossDetection = saved.focusLossDetection,
            copyPasteBlock = saved.copyPasteBlock,
            navigationExitLock = saved.navigationExitLock,
            warningLimit = saved.warningLimit,
            violationCount = saved.violationCount,
            violationLogs = saved.violationLogs,
            finalFiveMinAlertShown = saved.finalFiveMinAlertShown
        )
        if (remaining <= 0L) {
            submitActiveExam { _, _ -> }
        } else {
            startCountdownTimer()
        }
    }

    /**
     * Opens the Testbook-style Pre-Test Instructions screen before starting a fresh attempt.
     * If the user already submitted or has a paused session, handles smoothly without losing time.
     */
    fun openTestInstructions(test: ExamTest) {
        val alreadySubmitted = groupAttempts.value.firstOrNull {
            it.testId == test.id && it.userId == currentUserId
        }
        if (alreadySubmitted != null) {
            sessionStore?.clearExamSession()
            sessionStore?.clearPausedSession(currentUserId, test.id)
            refreshPausedSessions()
            _latestCompletedAttempt.value = test to alreadySubmitted
            showBanner("You have already attempted this test. Showing your locked result.")
            return
        }
        val paused = sessionStore?.loadPausedSession(currentUserId, test.id)
        if (paused != null && paused.pausedRemainingSeconds > 0L) {
            startExam(test)
            return
        }
        _pendingInstructionTest.value = test
    }

    fun dismissTestInstructions() {
        _pendingInstructionTest.value = null
    }

    fun beginExamFromInstructions(test: ExamTest, chosenLanguage: LanguagePref) {
        _pendingInstructionTest.value = null
        startExam(test, initialLanguageOverride = chosenLanguage)
    }

    fun startExam(test: ExamTest, initialLanguageOverride: LanguagePref? = null) {
        val questions = test.parsedQuestions()
        if (questions.isEmpty()) {
            showBanner("This test has no questions.", isError = true)
            return
        }
        val alreadySubmitted = groupAttempts.value.firstOrNull {
            it.testId == test.id && it.userId == currentUserId
        }
        if (alreadySubmitted != null) {
            sessionStore?.clearExamSession()
            sessionStore?.clearPausedSession(currentUserId, test.id)
            refreshPausedSessions()
            _latestCompletedAttempt.value = test to alreadySubmitted
            showBanner("You have already attempted this test. Showing your locked result.")
            return
        }

        val totalSec = (test.durationMinutes.coerceAtLeast(1L)) * 60L
        val paused = sessionStore?.loadPausedSession(currentUserId, test.id)
        val savedActive = sessionStore?.loadExamSession(currentUserId)
        val bench = QuestionTimingBenchmark.computeForTest(test.id, groupAttempts.value)
        val resolvedLang = initialLanguageOverride
            ?: (userProfileState.value as? UiState.Success)?.data?.langPreference()
            ?: LanguagePref.BILINGUAL

        val state = when {
            paused != null && paused.pausedRemainingSeconds > 0L -> {
                val exactRemaining = paused.pausedRemainingSeconds.coerceIn(1L, totalSec)
                val newEndEpoch = clockMillis() + exactRemaining * 1000L
                val safeIdx = paused.currentIndex.coerceIn(0, questions.lastIndex)
                sessionStore.clearPausedSession(currentUserId, test.id)
                refreshPausedSessions()
                ActiveExamState(
                    test = test,
                    questions = questions,
                    currentIndex = safeIdx,
                    selectedAnswers = paused.selectedAnswers,
                    markedForReview = paused.markedForReview,
                    visitedQuestions = paused.visitedQuestions + safeIdx,
                    remainingSeconds = exactRemaining,
                    totalDurationSeconds = totalSec,
                    endTimeEpochMillis = newEndEpoch,
                    startedAtEpochMillis = paused.startedAtEpochMillis.takeIf { it > 0L } ?: clockMillis(),
                    questionTimesSec = paused.questionTimesSec,
                    priorQuestionAvgTimesSec = bench.avgSecondsByQuestion,
                    priorTimedAttemptsCount = bench.priorTimedAttemptsCount,
                    priorTotalAttemptsCount = bench.priorTotalAttemptsCount,
                    priorAvgTotalSeconds = bench.priorAvgTotalSeconds,
                    activeLanguage = resolvedLang,
                    finalFiveMinAlertShown = paused.finalFiveMinAlertShown,
                    showFinalFiveMinAlertBanner = !paused.finalFiveMinAlertShown && exactRemaining in 1L..300L && totalSec > 300L,
                    finalOneMinAlertShown = exactRemaining <= 60L
                )
            }
            savedActive != null && savedActive.testId == test.id && savedActive.groupId == test.groupId -> {
                val remaining = ((savedActive.endTimeEpochMillis - clockMillis()) / 1000L).coerceAtLeast(0L)
                val safeIdx = savedActive.currentIndex.coerceIn(0, questions.lastIndex)
                ActiveExamState(
                    test = test,
                    questions = questions,
                    currentIndex = safeIdx,
                    selectedAnswers = savedActive.selectedAnswers,
                    markedForReview = savedActive.markedForReview,
                    visitedQuestions = savedActive.visitedQuestions + safeIdx,
                    remainingSeconds = remaining,
                    totalDurationSeconds = totalSec,
                    endTimeEpochMillis = savedActive.endTimeEpochMillis,
                    startedAtEpochMillis = savedActive.startedAtEpochMillis.takeIf { it > 0L } ?: clockMillis(),
                    questionTimesSec = savedActive.questionTimesSec,
                    priorQuestionAvgTimesSec = bench.avgSecondsByQuestion,
                    priorTimedAttemptsCount = bench.priorTimedAttemptsCount,
                    priorTotalAttemptsCount = bench.priorTotalAttemptsCount,
                    priorAvgTotalSeconds = bench.priorAvgTotalSeconds,
                    activeLanguage = resolvedLang,
                    liveExamId = savedActive.liveExamId,
                    isLiveExam = savedActive.isLiveExam,
                    secureModeEnabled = savedActive.secureModeEnabled,
                    dynamicWatermark = savedActive.dynamicWatermark,
                    focusLossDetection = savedActive.focusLossDetection,
                    copyPasteBlock = savedActive.copyPasteBlock,
                    navigationExitLock = savedActive.navigationExitLock,
                    warningLimit = savedActive.warningLimit,
                    violationCount = savedActive.violationCount,
                    violationLogs = savedActive.violationLogs,
                    finalFiveMinAlertShown = savedActive.finalFiveMinAlertShown
                )
            }
            else -> {
                val now = clockMillis()
                val endEpoch = now + totalSec * 1000L
                ActiveExamState(
                    test = test,
                    questions = questions,
                    currentIndex = 0,
                    selectedAnswers = emptyMap(),
                    markedForReview = emptySet(),
                    visitedQuestions = setOf(0),
                    remainingSeconds = totalSec,
                    totalDurationSeconds = totalSec,
                    endTimeEpochMillis = endEpoch,
                    startedAtEpochMillis = now,
                    questionTimesSec = emptyMap(),
                    priorQuestionAvgTimesSec = bench.avgSecondsByQuestion,
                    priorTimedAttemptsCount = bench.priorTimedAttemptsCount,
                    priorTotalAttemptsCount = bench.priorTotalAttemptsCount,
                    priorAvgTotalSeconds = bench.priorAvgTotalSeconds,
                    activeLanguage = resolvedLang
                )
            }
        }

        _activeExam.value = state
        persistActiveExam(state)
        if (state.remainingSeconds <= 0L) {
            submitActiveExam { _, _ -> }
        } else {
            startCountdownTimer()
        }
    }

    fun pauseActiveExam() {
        val exam = _activeExam.value ?: return
        if (exam.submitting) return
        if (exam.isLiveExam) {
            showBanner("Synchronized Live Exams share a common deadline and cannot be paused.", isError = true)
            return
        }
        if (!exam.test.pauseAllowed) {
            showBanner("Pause is disabled for this test by Admin.", isError = true)
            return
        }
        timerJob?.cancel()
        val exactRemaining = ((exam.endTimeEpochMillis - clockMillis()) / 1000L)
            .coerceIn(1L, exam.totalDurationSeconds.coerceAtLeast(1L))
        sessionStore?.savePausedSession(
            PersistedExamSession(
                userId = currentUserId,
                groupId = exam.test.groupId,
                testId = exam.test.id,
                endTimeEpochMillis = 0L,
                totalDurationSeconds = exam.totalDurationSeconds,
                currentIndex = exam.currentIndex,
                selectedAnswers = exam.selectedAnswers,
                markedForReview = exam.markedForReview,
                isPaused = true,
                pausedRemainingSeconds = exactRemaining,
                questionTimesSec = exam.questionTimesSec,
                visitedQuestions = exam.visitedQuestions,
                startedAtEpochMillis = exam.startedAtEpochMillis,
                finalFiveMinAlertShown = exam.finalFiveMinAlertShown
            )
        )
        sessionStore?.clearExamSession()
        refreshPausedSessions()
        _activeExam.value = null
        val hrs = exactRemaining / 3600L
        val mins = (exactRemaining % 3600L) / 60L
        val secs = exactRemaining % 60L
        val timeFormatted = if (hrs > 0L) {
            String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
        }
        showBanner("Test paused with $timeFormatted left! Resume anytime from this exact time.")
    }

    private fun refreshPausedSessions() {
        _pausedSessions.value = sessionStore?.loadAllPausedSessions(currentUserId).orEmpty()
    }

    fun selectAnswer(questionIndex: Int, optionKey: String) {
        _activeExam.update { state ->
            if (state == null || state.submitting) state
            else state.copy(
                selectedAnswers = state.selectedAnswers + (questionIndex to optionKey),
                visitedQuestions = state.visitedQuestions + questionIndex
            ).also { persistActiveExam(it) }
        }
    }

    fun clearResponse(questionIndex: Int) {
        _activeExam.update { state ->
            if (state == null || state.submitting) state
            else state.copy(selectedAnswers = state.selectedAnswers - questionIndex)
                .also { persistActiveExam(it) }
        }
    }

    fun toggleMarkForReview(questionIndex: Int) {
        _activeExam.update { state ->
            if (state == null || state.submitting) state
            else {
                val next = if (questionIndex in state.markedForReview) state.markedForReview - questionIndex
                else state.markedForReview + questionIndex
                state.copy(markedForReview = next, visitedQuestions = state.visitedQuestions + questionIndex)
                    .also { persistActiveExam(it) }
            }
        }
    }

    fun navigateToQuestion(index: Int) {
        _activeExam.update { state ->
            if (state == null || state.submitting) state
            else {
                val safe = index.coerceIn(0, state.questions.lastIndex)
                state.copy(
                    currentIndex = safe,
                    visitedQuestions = state.visitedQuestions + safe
                ).also { persistActiveExam(it) }
            }
        }
    }

    fun setActiveExamLanguage(lang: LanguagePref) {
        _activeExam.update { state -> state?.copy(activeLanguage = lang) }
    }

    fun adjustExamFontScale(delta: Int) {
        _activeExam.update { state ->
            state?.copy(fontScaleStep = (state.fontScaleStep + delta).coerceIn(-2, 3))
        }
    }

    fun submitActiveExam(onCompleted: (ExamTest, TestAttempt) -> Unit) {
        val exam = _activeExam.value ?: return
        if (exam.submitting) return
        val gid = activeGroup.value?.id ?: exam.test.groupId
        val member = currentMember.value ?: run {
            val prof = (userProfileState.value as? UiState.Success)?.data
            if (prof != null) {
                GroupMember(
                    groupId = gid,
                    userId = currentUserId,
                    displayName = prof.displayName.ifBlank { "Student" },
                    photoBase64 = prof.photoBase64,
                    role = prof.role
                )
            } else {
                showBanner("Profile is still syncing. Please tap Submit again.", isError = true)
                return
            }
        }

        timerJob?.cancel()
        _activeExam.update { it?.copy(submitting = true) }

        val eval = ExamScoringEngine.evaluate(exam.test, exam.questions, exam.selectedAnswers)
        val remainingNow = ((exam.endTimeEpochMillis - clockMillis()) / 1000L).coerceAtLeast(0L)
        val spentSec = (exam.totalDurationSeconds - remainingNow).coerceIn(1L, exam.totalDurationSeconds)
        val completionStatus = if (remainingNow <= 0L) "AUTO_SUBMITTED" else "COMPLETED"

        viewModelScope.launch {
            val res = repository.submitExamAttempt(
                groupId = gid,
                test = exam.test,
                member = member,
                selectedAnswers = exam.selectedAnswers,
                eval = eval,
                timeSpentSeconds = spentSec,
                existingUserAttempts = groupAttempts.value,
                questionTimesSec = exam.questionTimesSec,
                startedAtEpochMillis = exam.startedAtEpochMillis,
                completionStatus = completionStatus
            )
            res.onSuccess { attempt ->
                // If this was a scheduled Live Exam, update the participant's LiveExamSession to SUBMITTED (or keep FLAGGED if already flagged)
                val liveExamId = exam.liveExamId
                val liveExamObj = if (exam.isLiveExam && !liveExamId.isNullOrBlank()) {
                    liveExams.value.firstOrNull { it.id == liveExamId }
                } else null
                if (liveExamObj != null) {
                    val userEmail = (userProfileState.value as? UiState.Success)?.data?.email
                    val finalStatus = if (exam.violationCount >= exam.warningLimit) "FLAGGED" else "SUBMITTED"
                    repository.registerOrUpdateLiveExamSession(
                        groupId = gid,
                        liveExam = liveExamObj,
                        userName = member.displayName,
                        userEmail = userEmail,
                        newStatus = finalStatus,
                        answeredCount = exam.selectedAnswers.size,
                        currentQuestionIndex = exam.currentIndex,
                        savedAnswers = exam.selectedAnswers.mapKeys { it.key.toString() },
                        violationCount = exam.violationCount,
                        violationLogs = exam.violationLogs,
                        finalFiveMinAlertShown = exam.finalFiveMinAlertShown,
                        score = attempt.totalScore,
                        maxMarks = attempt.maxMarks
                    )
                }
                sessionStore?.clearExamSession()
                sessionStore?.clearPausedSession(currentUserId, exam.test.id)
                refreshPausedSessions()
                _activeExam.value = null
                if (liveExamObj != null && !liveExamObj.isResultsReleased(clockMillis())) {
                    showBanner(
                        "Live Exam submitted! Results & solutions unlock at ${IstClock.formatDateTime(liveExamObj.resultPublishEpochMillis)}."
                    )
                } else {
                    _latestCompletedAttempt.value = exam.test to attempt
                    showBanner("Test evaluated & synced to live ranking!")
                }
                onCompleted(exam.test, attempt)
            }.onFailure { err ->
                _activeExam.update { it?.copy(submitting = false) }
                if (remainingNow > 0L) startCountdownTimer()
                showBanner(err.localizedMessage ?: "Submission failed. Retry when connected.", isError = true)
            }
        }
    }

    fun openAttemptReview(test: ExamTest, attempt: TestAttempt) {
        _latestCompletedAttempt.value = test to attempt
    }

    fun closeAttemptReview() {
        _latestCompletedAttempt.value = null
    }

    fun abortExamWithoutSubmit() {
        val exam = _activeExam.value
        if (exam?.isLiveExam == true && exam.secureModeEnabled && exam.navigationExitLock) {
            showBanner("Exit is locked during a Secure Live Exam. Submit the test to finish.", isError = true)
            return
        }
        val testId = exam?.test?.id
        timerJob?.cancel()
        sessionStore?.clearExamSession()
        if (testId != null) {
            sessionStore?.clearPausedSession(currentUserId, testId)
            refreshPausedSessions()
        }
        _activeExam.value = null
    }

    fun dismissFinalFiveMinAlertBanner() {
        _activeExam.update { it?.copy(showFinalFiveMinAlertBanner = false) }
    }

    fun recordLiveExamSecurityViolation(violationType: String, detailMessage: String): Int {
        val current = _activeExam.value ?: return 0
        if (!current.isLiveExam || !current.secureModeEnabled || current.submitting) return current.violationCount
        val nextCount = current.violationCount + 1
        val istStamp = IstClock.formatTimeWithSeconds(clockMillis())
        val entry = "$istStamp — $violationType ($nextCount/${current.warningLimit}): $detailMessage"
        val nextLogs = (current.violationLogs + entry).takeLast(50)
        val updatedState = current.copy(
            violationCount = nextCount,
            violationLogs = nextLogs
        )
        _activeExam.value = updatedState
        persistActiveExam(updatedState)

        val gid = activeGroup.value?.id ?: current.test.groupId
        val member = currentMember.value
        val liveExamObj = liveExams.value.firstOrNull { it.id == current.liveExamId }
        if (gid.isNotBlank() && member != null && liveExamObj != null) {
            val userEmail = (userProfileState.value as? UiState.Success)?.data?.email
            val newStatus = if (nextCount >= current.warningLimit) "FLAGGED" else "ACTIVE"
            viewModelScope.launch {
                repository.registerOrUpdateLiveExamSession(
                    groupId = gid,
                    liveExam = liveExamObj,
                    userName = member.displayName,
                    userEmail = userEmail,
                    newStatus = newStatus,
                    answeredCount = updatedState.selectedAnswers.size,
                    currentQuestionIndex = updatedState.currentIndex,
                    savedAnswers = updatedState.selectedAnswers.mapKeys { it.key.toString() },
                    violationCount = nextCount,
                    violationLogs = nextLogs,
                    finalFiveMinAlertShown = updatedState.finalFiveMinAlertShown
                )
            }
        }
        return nextCount
    }

    fun startScheduledLiveExam(liveExam: LiveExam) {
        val sourceTest = groupTests.value.firstOrNull { it.id == liveExam.sourceSetId }
        if (sourceTest == null) {
            showBanner("Linked Question Set (${liveExam.sourceSetTitle}) is not available.", isError = true)
            return
        }
        val questions = sourceTest.parsedQuestions()
        if (questions.isEmpty()) {
            showBanner("Linked Question Set has no questions.", isError = true)
            return
        }
        val alreadySubmitted = groupAttempts.value.firstOrNull {
            it.testId == sourceTest.id && it.userId == currentUserId
        }
        if (alreadySubmitted != null) {
            _latestCompletedAttempt.value = sourceTest to alreadySubmitted
            showBanner("You have already submitted this Live Exam set.")
            return
        }
        val mySession = liveExamSessions.value.firstOrNull {
            it.liveExamId == liveExam.id && it.userId == currentUserId
        }
        if (mySession?.status == "DISQUALIFIED") {
            showBanner("Your session for this Live Exam was disqualified by Admin.", isError = true)
            return
        }

        val now = clockMillis()
        if (now < liveExam.startTimeEpochMillis) {
            showBanner("Live Exam has not started yet. Please wait in the Waiting Room until official start time.", isError = true)
            return
        }
        if (liveExam.endTimeEpochMillis > 0L && now >= liveExam.endTimeEpochMillis) {
            showBanner("This Live Exam window has already ended.", isError = true)
            return
        }

        val commonEndEpoch = if (liveExam.endTimeEpochMillis > now) {
            liveExam.endTimeEpochMillis
        } else {
            now + (liveExam.durationMinutes.coerceAtLeast(1L) * 60_000L)
        }
        val remainingSec = ((commonEndEpoch - now) / 1000L).coerceAtLeast(1L)
        val totalDurationSec = (liveExam.durationMinutes.coerceAtLeast(1L)) * 60L
        val bench = QuestionTimingBenchmark.computeForTest(sourceTest.id, groupAttempts.value)
        val resolvedLang = (userProfileState.value as? UiState.Success)?.data?.langPreference() ?: LanguagePref.BILINGUAL

        // Recover any previously saved answers from local store or Firestore LiveExamSession
        val savedActive = sessionStore?.loadExamSession(currentUserId)
            ?.takeIf { it.testId == sourceTest.id && it.liveExamId == liveExam.id }
        val sessionAnswers = mySession?.savedAnswers
            ?.mapNotNull { (k, v) -> k.toIntOrNull()?.let { idx -> idx to v } }
            ?.toMap()
            .orEmpty()
        val mergedAnswers = if (savedActive != null && savedActive.selectedAnswers.isNotEmpty()) {
            sessionAnswers + savedActive.selectedAnswers
        } else {
            sessionAnswers
        }
        val safeIdx = (savedActive?.currentIndex ?: mySession?.currentQuestionIndex?.toInt() ?: 0)
            .coerceIn(0, questions.lastIndex)
        val restoredViolations = maxOf(savedActive?.violationCount ?: 0, mySession?.violationCount?.toInt() ?: 0)
        val restoredLogs = savedActive?.violationLogs?.ifEmpty { mySession?.violationLogs.orEmpty() }
            ?: mySession?.violationLogs.orEmpty()
        val alreadyAlerted = (savedActive?.finalFiveMinAlertShown == true) || (mySession?.finalFiveMinAlertShown == true)
        val triggerAlertNow = !alreadyAlerted && remainingSec <= 300L

        val state = ActiveExamState(
            test = sourceTest.copy(
                title = liveExam.title.ifBlank { sourceTest.title },
                durationMinutes = liveExam.durationMinutes,
                negativeMarkNumerator = liveExam.negativeMarkNumerator,
                negativeMarkDenominator = liveExam.negativeMarkDenominator,
                pauseAllowed = false
            ),
            questions = questions,
            currentIndex = safeIdx,
            selectedAnswers = mergedAnswers,
            markedForReview = savedActive?.markedForReview ?: emptySet(),
            visitedQuestions = (savedActive?.visitedQuestions ?: setOf(0)) + safeIdx + mergedAnswers.keys,
            remainingSeconds = remainingSec,
            totalDurationSeconds = totalDurationSec,
            endTimeEpochMillis = commonEndEpoch,
            startedAtEpochMillis = savedActive?.startedAtEpochMillis?.takeIf { it > 0L } ?: now,
            questionTimesSec = savedActive?.questionTimesSec ?: emptyMap(),
            priorQuestionAvgTimesSec = bench.avgSecondsByQuestion,
            priorTimedAttemptsCount = bench.priorTimedAttemptsCount,
            priorTotalAttemptsCount = bench.priorTotalAttemptsCount,
            priorAvgTotalSeconds = bench.priorAvgTotalSeconds,
            activeLanguage = resolvedLang,
            liveExamId = liveExam.id,
            isLiveExam = true,
            secureModeEnabled = liveExam.secureModeEnabled,
            dynamicWatermark = liveExam.secureModeEnabled && liveExam.dynamicWatermark,
            focusLossDetection = liveExam.secureModeEnabled && liveExam.focusLossDetection,
            copyPasteBlock = liveExam.secureModeEnabled && liveExam.copyPasteBlock,
            navigationExitLock = liveExam.secureModeEnabled && liveExam.navigationExitLock,
            warningLimit = liveExam.warningLimit.toInt().coerceIn(1, 20),
            violationCount = restoredViolations,
            violationLogs = restoredLogs,
            finalFiveMinAlertShown = alreadyAlerted || triggerAlertNow,
            showFinalFiveMinAlertBanner = triggerAlertNow
        )

        _activeExam.value = state
        persistActiveExam(state)
        startCountdownTimer()

        val gid = activeGroup.value?.id ?: liveExam.groupId
        val member = currentMember.value
        if (gid.isNotBlank() && member != null) {
            val userEmail = (userProfileState.value as? UiState.Success)?.data?.email
            viewModelScope.launch {
                repository.registerOrUpdateLiveExamSession(
                    groupId = gid,
                    liveExam = liveExam,
                    userName = member.displayName,
                    userEmail = userEmail,
                    newStatus = if (restoredViolations >= state.warningLimit) "FLAGGED" else "ACTIVE",
                    answeredCount = state.selectedAnswers.size,
                    currentQuestionIndex = state.currentIndex,
                    savedAnswers = state.selectedAnswers.mapKeys { it.key.toString() },
                    violationCount = state.violationCount,
                    violationLogs = state.violationLogs,
                    finalFiveMinAlertShown = state.finalFiveMinAlertShown
                )
            }
        }
    }

    fun registerForLiveExam(liveExam: LiveExam, enterWaitingRoom: Boolean = false) {
        val gid = activeGroup.value?.id ?: liveExam.groupId
        val member = currentMember.value ?: return
        val userEmail = (userProfileState.value as? UiState.Success)?.data?.email
        val targetStatus = if (enterWaitingRoom) "WAITING_ROOM" else "REGISTERED"
        viewModelScope.launch {
            _busy.value = true
            val res = repository.registerOrUpdateLiveExamSession(
                groupId = gid,
                liveExam = liveExam,
                userName = member.displayName,
                userEmail = userEmail,
                newStatus = targetStatus
            )
            _busy.value = false
            res.onSuccess {
                showBanner(
                    if (enterWaitingRoom) "Entered Live Exam Waiting Room! Exam will auto-start at scheduled IST time."
                    else "Registered for ${liveExam.title}!"
                )
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Could not register for Live Exam", isError = true)
            }
        }
    }

    fun saveOrScheduleLiveExam(
        existingId: String? = null,
        sourceTest: ExamTest,
        title: String,
        description: String?,
        category: RankCategory,
        examDateIst: String,
        startTimeIst: String,
        startTimeEpochMillis: Long,
        durationMinutes: Int,
        registrationOpenEpochMillis: Long,
        registrationCloseEpochMillis: Long,
        resultPublishEpochMillis: Long,
        eligibilityType: String,
        maxParticipants: Int,
        lateEntryAllowed: Boolean,
        lateEntryCutoffMinutes: Int,
        secureModeEnabled: Boolean,
        dynamicWatermark: Boolean,
        focusLossDetection: Boolean,
        copyPasteBlock: Boolean,
        navigationExitLock: Boolean,
        warningLimit: Int,
        status: String,
        onSaved: (String) -> Unit = {}
    ) {
        val gid = activeGroup.value?.id ?: sourceTest.groupId
        val member = currentMember.value ?: return
        if (!member.appRole().isAdminRole()) {
            showBanner("Only Admin can schedule or modify Live Exams.", isError = true)
            return
        }
        val qCount = sourceTest.questionCount.toInt().coerceAtLeast(sourceTest.questions.size.coerceAtLeast(1))
        val totalMarks = (qCount * sourceTest.positiveMarks).coerceAtLeast(1.0)
        viewModelScope.launch {
            _busy.value = true
            val res = repository.createOrUpdateLiveExam(
                groupId = gid,
                existingId = existingId,
                sourceSetId = sourceTest.id,
                sourceSetTitle = sourceTest.title,
                title = title,
                description = description,
                category = category.name,
                paperType = sourceTest.paperType,
                examDateIst = examDateIst,
                startTimeIst = startTimeIst,
                startTimeEpochMillis = startTimeEpochMillis,
                durationMinutes = durationMinutes,
                registrationOpenEpochMillis = registrationOpenEpochMillis,
                registrationCloseEpochMillis = registrationCloseEpochMillis,
                resultPublishEpochMillis = resultPublishEpochMillis,
                totalQuestions = qCount,
                totalMarks = totalMarks,
                negativeMarkNumerator = sourceTest.negativeMarkNumerator.toInt(),
                negativeMarkDenominator = sourceTest.negativeMarkDenominator.toInt(),
                eligibilityType = eligibilityType,
                maxParticipants = maxParticipants,
                lateEntryAllowed = lateEntryAllowed,
                lateEntryCutoffMinutes = lateEntryCutoffMinutes,
                secureModeEnabled = secureModeEnabled,
                dynamicWatermark = dynamicWatermark,
                focusLossDetection = focusLossDetection,
                copyPasteBlock = copyPasteBlock,
                navigationExitLock = navigationExitLock,
                warningLimit = warningLimit,
                status = status,
                creatorName = member.displayName
            )
            _busy.value = false
            res.onSuccess { id ->
                val actionLabel = when (status.uppercase()) {
                    "DRAFT" -> "saved as Draft"
                    "CANCELLED" -> "Cancelled"
                    else -> "scheduled & Published"
                }
                showBanner("Live Exam '$title' $actionLabel!")
                onSaved(id)
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Failed to save Live Exam", isError = true)
            }
        }
    }

    fun saveOrScheduleLiveExam(liveExam: LiveExam, publishImmediately: Boolean) {
        val sourceTest = groupTests.value.firstOrNull { it.id == liveExam.sourceSetId }
        if (sourceTest == null) {
            showBanner("Linked source question set not found.", isError = true)
            return
        }
        val resolvedCategory = RankCategory.fromCategoryString(liveExam.category)
            ?: RankCategory.fromTest(sourceTest)
            ?: RankCategory.COMMON
        saveOrScheduleLiveExam(
            existingId = liveExam.id.ifBlank { null },
            sourceTest = sourceTest,
            title = liveExam.title,
            description = liveExam.description,
            category = resolvedCategory,
            examDateIst = liveExam.examDateIst.orEmpty(),
            startTimeIst = liveExam.startTimeIst.orEmpty(),
            startTimeEpochMillis = liveExam.startTimeEpochMillis,
            durationMinutes = liveExam.durationMinutes.toInt().coerceAtLeast(1),
            registrationOpenEpochMillis = liveExam.registrationOpenEpochMillis,
            registrationCloseEpochMillis = liveExam.registrationCloseEpochMillis,
            resultPublishEpochMillis = liveExam.resultPublishEpochMillis,
            eligibilityType = liveExam.eligibilityType,
            maxParticipants = liveExam.maxParticipants.toInt().coerceAtLeast(0),
            lateEntryAllowed = liveExam.lateEntryAllowed,
            lateEntryCutoffMinutes = liveExam.lateEntryCutoffMinutes.toInt().coerceAtLeast(0),
            secureModeEnabled = liveExam.secureModeEnabled,
            dynamicWatermark = liveExam.dynamicWatermark,
            focusLossDetection = liveExam.focusLossDetection,
            copyPasteBlock = liveExam.copyPasteBlock,
            navigationExitLock = liveExam.navigationExitLock,
            warningLimit = liveExam.warningLimit.toInt().coerceAtLeast(1),
            status = if (publishImmediately) "PUBLISHED" else liveExam.status
        )
    }

    fun removeLiveExamUnlinkOnly(liveExam: LiveExam) {
        val gid = activeGroup.value?.id ?: liveExam.groupId
        if (currentMember.value?.appRole()?.isAdminRole() != true) {
            showBanner("Only Admin can remove a Live Exam.", isError = true)
            return
        }
        launchAction("Removed '${liveExam.title}' from Live Exams. Original Set '${liveExam.sourceSetTitle}' is 100% preserved.") {
            repository.removeLiveExamUnlinkOnly(gid, liveExam.id)
        }
    }

    fun clearCompletedAndPastLiveExams() {
        val gid = activeGroup.value?.id ?: return
        if (currentMember.value?.appRole()?.isAdminRole() != true) {
            showBanner("Only Admin can clear Completed & Past Live Exams.", isError = true)
            return
        }
        val now = System.currentTimeMillis()
        val targetExams = liveExams.value.filter { exam ->
            val dyn = exam.dynamicStatus(now)
            dyn == LiveExamDynamicStatus.COMPLETED ||
                dyn == LiveExamDynamicStatus.CANCELLED ||
                exam.status in listOf("COMPLETED", "CANCELLED") ||
                (exam.endTimeEpochMillis in 1..now)
        }
        if (targetExams.isEmpty()) {
            showBanner("No Completed & Past Live Exams found to delete.")
            return
        }
        val targetIds = targetExams.map { it.id }
        launchAction("Deleted ${targetIds.size} Completed & Past Live Exam(s) and their trial sessions.") {
            repository.deleteLiveExamsByIds(gid, targetIds).map { }
        }
    }

    fun toggleLiveExamPublished(liveExam: LiveExam, publish: Boolean) {
        val gid = activeGroup.value?.id ?: liveExam.groupId
        if (currentMember.value?.appRole()?.isAdminRole() != true) {
            showBanner("Only Admin can publish or unpublish Live Exams.", isError = true)
            return
        }
        val nextStatus = if (publish) "PUBLISHED" else "DRAFT"
        launchAction(
            if (publish) "Live Exam '${liveExam.title}' is now PUBLISHED & visible to students!"
            else "Live Exam '${liveExam.title}' moved to Unpublished (hidden from students)."
        ) {
            repository.setLiveExamStatus(gid, liveExam.id, nextStatus)
        }
    }

    fun cancelScheduledLiveExam(liveExam: LiveExam) {
        val gid = activeGroup.value?.id ?: liveExam.groupId
        if (currentMember.value?.appRole()?.isAdminRole() != true) return
        launchAction("Live Exam '${liveExam.title}' marked as CANCELLED.") {
            repository.setLiveExamStatus(gid, liveExam.id, "CANCELLED")
        }
    }

    fun forceStartLiveExamEarly(liveExam: LiveExam) {
        val gid = activeGroup.value?.id ?: liveExam.groupId
        if (currentMember.value?.appRole()?.isAdminRole() != true) return
        launchAction("Live Exam '${liveExam.title}' force-started NOW in IST!") {
            repository.forceStartLiveExamNow(gid, liveExam)
        }
    }

    fun publishLiveExamResultsNow(liveExam: LiveExam, published: Boolean = true) {
        val gid = activeGroup.value?.id ?: liveExam.groupId
        if (currentMember.value?.appRole()?.isAdminRole() != true) return
        launchAction(
            if (published) "Results & solutions for '${liveExam.title}' published to all participants!"
            else "Manual result release reverted to scheduled publication time."
        ) {
            repository.publishLiveExamResultsManually(gid, liveExam.id, published)
        }
    }

    fun moderateLiveExamParticipantSession(
        session: LiveExamSession,
        decisionStatus: String,
        adminNote: String
    ) {
        val gid = activeGroup.value?.id ?: session.groupId
        if (currentMember.value?.appRole()?.isAdminRole() != true) return
        launchAction("Session for ${session.userName} marked as $decisionStatus.") {
            repository.moderateLiveExamSession(gid, session.id, decisionStatus, adminNote)
        }
    }

    fun toggleQuestionBookmark(test: ExamTest, questionIndex: Int, question: ExamQuestion) {
        val gid = activeGroup.value?.id ?: test.groupId
        if (gid.isBlank()) return
        val existing = bookmarks.value.any { it.testId == test.id && it.questionIndex.toInt() == questionIndex }
        viewModelScope.launch {
            val res = repository.toggleBookmark(gid, test.id, test.title, questionIndex, question, existing)
            res.onSuccess { added ->
                showBanner(if (added) "Question bookmarked for revision!" else "Bookmark removed.")
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Could not update bookmark", isError = true)
            }
        }
    }

    fun recordSolutionReattempt(
        test: ExamTest,
        reattemptAnswers: Map<String, String>,
        questions: List<ExamQuestion>
    ) {
        val gid = activeGroup.value?.id ?: test.groupId
        val member = currentMember.value ?: return
        var correct = 0L
        var wrong = 0L
        reattemptAnswers.forEach { (idxStr, picked) ->
            val q = idxStr.toIntOrNull()?.let { questions.getOrNull(it) } ?: return@forEach
            val key = q.correctOption.uppercase().trim()
            if (key.isNotBlank()) {
                if (picked.uppercase().trim() == key) correct++ else wrong++
            }
        }
        viewModelScope.launch {
            repository.saveSolutionReattempt(
                groupId = gid,
                testId = test.id,
                testTitle = test.title,
                aspirantName = member.displayName,
                reattemptAnswers = reattemptAnswers,
                correctCount = correct,
                wrongCount = wrong
            )
        }
    }

    fun resolveReportedIssue(commentId: String, resolved: Boolean, note: String = "") {
        val gid = activeGroup.value?.id ?: return
        launchAction(if (resolved) "Reported issue marked Resolved!" else "Issue reopened.") {
            repository.resolveReportedIssue(gid, commentId, resolved, note)
        }
    }

    fun updateTestExamRules(
        testId: String,
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
    ) {
        val gid = activeGroup.value?.id ?: return
        launchAction("Exam rules updated successfully!") {
            repository.updateTestExamRules(
                groupId = gid,
                testId = testId,
                positiveMarks = positiveMarks,
                negativeNum = negativeNum,
                negativeDen = negativeDen,
                unattemptedPenalty = unattemptedPenalty,
                optionEMode = optionEMode,
                cutoffMarks = cutoffMarks,
                pauseAllowed = pauseAllowed,
                langPassMarks = langPassMarks,
                langMinHindi = langMinHindi,
                langMinEnglish = langMinEnglish,
                langFailRankPolicy = langFailRankPolicy,
                tieBreakPolicy = tieBreakPolicy
            )
        }
    }

    /**
     * One-Click Full Length Set Assembler for Part 5 (150 Q Maths & Science) and Part 6 (150 Q Maths).
     * Strictly validates exact question counts (30 Lang + 40 Common + 40 Maths + 40 Science/Extra Maths = 150)
     * and preserves all section tags so Language (30 Q) is strictly excluded from Merit (/120).
     */
    fun assembleFullLengthSet(
        title: String,
        targetSetType: SetType,
        part1CommonTest: ExamTest,
        part2LanguageTest: ExamTest,
        part3MathsTest: ExamTest,
        fourthPartTest: ExamTest,
        durationMinutes: Int,
        publishImmediately: Boolean,
        positiveMarks: Double = 1.0,
        negativeNum: Int = 1,
        negativeDen: Int = 3,
        unattemptedPenalty: Boolean = true,
        optionEMode: OptionEMode = OptionEMode.NOT_ATTEMPTED,
        cutoffMarks: Double? = null,
        pauseAllowed: Boolean = true,
        langPassMarks: Double = 9.0,
        langFailRankPolicy: LanguageFailRankPolicy = LanguageFailRankPolicy.SHOW_WITH_NOT_QUALIFIED_BADGE,
        tieBreakPolicy: TieBreakPolicy = TieBreakPolicy.SHARED_EQUAL_RANK,
        onSuccess: () -> Unit = {}
    ) {
        val gid = activeGroup.value?.id ?: return
        val member = currentMember.value ?: return
        if (!member.appRole().isAdminRole()) {
            showBanner("Only Admin can assemble full-length sets.", isError = true)
            return
        }
        val langQs = part2LanguageTest.parsedQuestions()
        val commonQs = part1CommonTest.parsedQuestions()
        val mathAQs = part3MathsTest.parsedQuestions()
        val fourthQs = fourthPartTest.parsedQuestions()

        val assembled = BulkQuestionParser.combineFull150Sections(
            languageQuestions = langQs,
            commonQuestions = commonQs,
            subjectQuestions = mathAQs + fourthQs,
            targetType = targetSetType.mappedPaperType
        )
        if (!assembled.valid) {
            showBanner(assembled.errors.firstOrNull() ?: "Set assembly validation failed.", isError = true)
            return
        }
        viewModelScope.launch {
            _busy.value = true
            val res = repository.createTestSet(
                groupId = gid,
                title = title,
                paperType = targetSetType.mappedPaperType,
                difficulty = "MODERATE",
                durationMinutes = durationMinutes,
                questions = assembled.questions,
                creator = member,
                publishImmediately = publishImmediately,
                negativeNum = negativeNum,
                negativeDen = negativeDen,
                unattemptedPenalty = unattemptedPenalty,
                approvedForAll = publishImmediately,
                setType = targetSetType,
                classLevel = targetSetType.classTag,
                positiveMarks = positiveMarks,
                optionEMode = optionEMode,
                cutoffMarks = cutoffMarks,
                pauseAllowed = pauseAllowed,
                langPassMarks = langPassMarks,
                langFailRankPolicy = langFailRankPolicy,
                tieBreakPolicy = tieBreakPolicy,
                assembledFromTestIds = listOf(part1CommonTest.id, part2LanguageTest.id, part3MathsTest.id, fourthPartTest.id)
            )
            _busy.value = false
            res.onSuccess {
                showBanner("Assembled ${targetSetType.shortLabel} ($title • 150 Q • Merit /120)!")
                onSuccess()
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Failed to assemble full-length set", isError = true)
            }
        }
    }

    private fun persistActiveExam(state: ActiveExamState) {
        sessionStore?.saveExamSession(
            PersistedExamSession(
                userId = currentUserId,
                groupId = state.test.groupId,
                testId = state.test.id,
                endTimeEpochMillis = state.endTimeEpochMillis,
                totalDurationSeconds = state.totalDurationSeconds,
                currentIndex = state.currentIndex,
                selectedAnswers = state.selectedAnswers,
                markedForReview = state.markedForReview,
                questionTimesSec = state.questionTimesSec,
                visitedQuestions = state.visitedQuestions,
                startedAtEpochMillis = state.startedAtEpochMillis,
                liveExamId = state.liveExamId,
                isLiveExam = state.isLiveExam,
                secureModeEnabled = state.secureModeEnabled,
                dynamicWatermark = state.dynamicWatermark,
                focusLossDetection = state.focusLossDetection,
                copyPasteBlock = state.copyPasteBlock,
                navigationExitLock = state.navigationExitLock,
                warningLimit = state.warningLimit,
                violationCount = state.violationCount,
                violationLogs = state.violationLogs,
                finalFiveMinAlertShown = state.finalFiveMinAlertShown
            )
        )
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val current = _activeExam.value ?: break
                if (current.submitting) break
                val remaining = ((current.endTimeEpochMillis - clockMillis()) / 1000L).coerceAtLeast(0L)
                val deltaSec = (current.remainingSeconds - remaining).coerceAtLeast(0L)
                if (remaining <= 0L) {
                    val updatedTimes = if (deltaSec > 0L) {
                        val prev = current.questionTimesSec[current.currentIndex] ?: 0L
                        current.questionTimesSec + (current.currentIndex to (prev + deltaSec))
                    } else {
                        current.questionTimesSec
                    }
                    _activeExam.update { it?.copy(remainingSeconds = 0L, questionTimesSec = updatedTimes) }
                    submitActiveExam { _, _ -> }
                    break
                } else if (deltaSec > 0L) {
                    val triggerFinalFiveMin = !current.finalFiveMinAlertShown &&
                        remaining in 1L..300L &&
                        (current.isLiveExam || current.totalDurationSeconds > 300L)
                    val triggerFinalOneMin = !current.finalOneMinAlertShown &&
                        remaining in 1L..60L
                    _activeExam.update { st ->
                        if (st == null || st.submitting) st
                        else {
                            val prev = st.questionTimesSec[st.currentIndex] ?: 0L
                            val nextTimes = st.questionTimesSec + (st.currentIndex to (prev + deltaSec))
                            st.copy(
                                remainingSeconds = remaining,
                                questionTimesSec = nextTimes,
                                finalFiveMinAlertShown = st.finalFiveMinAlertShown || triggerFinalFiveMin,
                                showFinalFiveMinAlertBanner = st.showFinalFiveMinAlertBanner || triggerFinalFiveMin,
                                finalOneMinAlertShown = st.finalOneMinAlertShown || triggerFinalOneMin
                            )
                        }
                    }
                    // Persist per-question timing & sync LiveExamSession every 5 seconds so crash recovery keeps accurate timers
                    if (triggerFinalFiveMin || remaining % 5L == 0L) {
                        _activeExam.value?.let { st ->
                            persistActiveExam(st)
                            if (st.isLiveExam && !st.liveExamId.isNullOrBlank()) {
                                val gid = activeGroup.value?.id ?: st.test.groupId
                                val member = currentMember.value
                                val liveExamObj = liveExams.value.firstOrNull { it.id == st.liveExamId }
                                if (gid.isNotBlank() && member != null && liveExamObj != null) {
                                    val userEmail = (userProfileState.value as? UiState.Success)?.data?.email
                                    viewModelScope.launch {
                                        repository.registerOrUpdateLiveExamSession(
                                            groupId = gid,
                                            liveExam = liveExamObj,
                                            userName = member.displayName,
                                            userEmail = userEmail,
                                            newStatus = if (st.violationCount >= st.warningLimit) "FLAGGED" else "ACTIVE",
                                            answeredCount = st.selectedAnswers.size,
                                            currentQuestionIndex = st.currentIndex,
                                            savedAnswers = st.selectedAnswers.mapKeys { it.key.toString() },
                                            violationCount = st.violationCount,
                                            violationLogs = st.violationLogs,
                                            finalFiveMinAlertShown = st.finalFiveMinAlertShown
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                delay(500L)
            }
        }
    }

    // ==================== CURRENT AFFAIRS, POLLS, DISCUSSIONS & ADMIN ====================

    fun publishCurrentAffair(
        title: String,
        summary: String,
        dateLabel: String,
        source: String,
        category: String,
        imageBase64: String?
    ) {
        val gid = activeGroup.value?.id ?: return
        val name = currentMember.value?.displayName ?: "Admin"
        launchAction("Current Affairs banner published!") {
            repository.publishCurrentAffair(gid, title, summary, dateLabel, source, category, imageBase64, name)
        }
    }

    fun updateCurrentAffair(
        itemId: String,
        title: String,
        summary: String,
        dateLabel: String,
        source: String,
        category: String,
        imageBase64: String?
    ) {
        val gid = activeGroup.value?.id ?: return
        launchAction("Daily Banner updated by Group Admin!") {
            repository.updateCurrentAffair(gid, itemId, title, summary, dateLabel, source, category, imageBase64)
        }
    }

    fun deleteCurrentAffair(itemId: String) {
        val gid = activeGroup.value?.id ?: return
        launchAction("Current Affairs item deleted.") {
            repository.deleteCurrentAffair(gid, itemId)
        }
    }

    fun createQuestionPoll(
        questionTitle: String,
        description: String,
        imageBase64: String?,
        options: List<String>,
        privateCorrectIndex: Int
    ) {
        val gid = activeGroup.value?.id ?: return
        val member = currentMember.value ?: return
        launchAction("Question Poll published! Correct key kept private until you finalize.") {
            repository.createQuestionPoll(gid, member, questionTitle, description, imageBase64, options, privateCorrectIndex)
        }
    }

    fun loadMyVoteForPoll(pollId: String) {
        val gid = activeGroup.value?.id ?: return
        if (pollId in _userPollVotes.value) return
        viewModelScope.launch {
            val vote = repository.getMyPollVote(gid, pollId)
            if (vote != null) {
                _userPollVotes.update { it + (pollId to vote) }
            }
        }
    }

    fun castPollVote(poll: QuestionPoll, optionIndex: Int) {
        if (_busy.value) return
        val gid = activeGroup.value?.id ?: return
        val member = currentMember.value ?: return
        viewModelScope.launch {
            _busy.value = true
            val res = repository.voteOnPoll(gid, poll, member.displayName, optionIndex)
            _busy.value = false
            res.onSuccess {
                _userPollVotes.update { it + (poll.id to optionIndex) }
                showBanner("Vote recorded!")
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Vote could not be submitted", isError = true)
            }
        }
    }

    fun finalizePoll(pollId: String, correctOptionIndex: Int, explanation: String, status: String = "FINALIZED") {
        val gid = activeGroup.value?.id ?: return
        launchAction(if (status == "FINALIZED") "Poll answer finalized & published!" else "Poll closed.") {
            repository.finalizePollAnswer(gid, pollId, correctOptionIndex, explanation, status)
        }
    }

    fun fetchCreatorPrivatePollKey(pollId: String, onLoaded: (Int?) -> Unit) {
        val gid = activeGroup.value?.id ?: return
        viewModelScope.launch {
            val idx = repository.getPrivatePollCorrectIndex(gid, pollId)
            onLoaded(idx)
        }
    }

    fun uploadSolutionToPoll(poll: QuestionPoll, solutionImageBase64: String?, textExplanation: String) {
        val gid = activeGroup.value?.id ?: return
        val member = currentMember.value ?: return
        launchAction("Solution uploaded to poll!") {
            repository.uploadPollSolution(gid, poll, member.displayName, solutionImageBase64, textExplanation)
        }
    }

    fun deletePoll(pollId: String) {
        val gid = activeGroup.value?.id ?: return
        launchAction("Poll deleted.") {
            repository.deletePoll(gid, pollId)
        }
    }

    fun reportPoll(pollId: String) {
        val gid = activeGroup.value?.id ?: return
        launchAction("Poll reported for Admin review.") {
            repository.reportPoll(gid, pollId)
        }
    }

    fun postComment(
        targetType: String,
        targetId: String,
        message: String,
        commentType: String = "COMMENT",
        parentCommentId: String? = null,
        solutionImageBase64: String? = null
    ) {
        val gid = activeGroup.value?.id ?: return
        val member = currentMember.value ?: return
        launchAction(if (commentType == "SOLUTION") "Solution posted to doubt thread!" else "Posted to doubt session!") {
            repository.postDiscussionComment(gid, targetType, targetId, member, message, commentType, parentCommentId, solutionImageBase64)
        }
    }

    fun editComment(commentId: String, newMessage: String) {
        val gid = activeGroup.value?.id ?: return
        launchAction("Post updated!") {
            repository.editDiscussionComment(gid, commentId, newMessage)
        }
    }

    fun deleteComment(commentId: String) {
        val gid = activeGroup.value?.id ?: return
        launchAction("Post removed.") {
            repository.deleteDiscussionComment(gid, commentId)
        }
    }

    fun markAnswerHelpful(commentId: String, helpful: Boolean) {
        val gid = activeGroup.value?.id ?: return
        launchAction(if (helpful) "Marked as Helpful Answer!" else "Removed Helpful mark.") {
            repository.markDiscussionHelpful(gid, commentId, helpful)
        }
    }

    fun assignMemberRole(targetUid: String, newRole: AppRole) {
        val gid = activeGroup.value?.id ?: return
        launchAction("User role updated to ${newRole.labelEn}") {
            repository.updateMemberRole(gid, targetUid, newRole)
        }
    }

    fun sendNotification(title: String, body: String, type: String) {
        val gid = activeGroup.value?.id ?: return
        val name = currentMember.value?.displayName ?: "Admin"
        launchAction("Notification broadcast to group!") {
            repository.sendGroupNotification(gid, title, body, type, name)
        }
    }

    fun publishAppUpdateRelease(
        versionName: String,
        versionCode: Long,
        title: String,
        releaseNotes: String,
        updateType: String,
        minSupportedVersionCode: Long,
        downloadUrl: String,
        fileSizeBytes: Long,
        sha256Checksum: String?,
        packageName: String?,
        onDone: () -> Unit = {}
    ) {
        if (_busy.value) return
        val gid = activeGroup.value?.id ?: return
        val member = currentMember.value ?: return
        if (!member.appRole().isAdminRole()) {
            showBanner("Access Denied: Only Admin can publish app updates.", isError = true)
            return
        }
        viewModelScope.launch {
            _busy.value = true
            val res = repository.publishAppUpdate(
                groupId = gid,
                versionName = versionName,
                versionCode = versionCode,
                title = title,
                releaseNotes = releaseNotes,
                updateType = updateType,
                minSupportedVersionCode = minSupportedVersionCode,
                downloadUrl = downloadUrl,
                fileSizeBytes = fileSizeBytes,
                sha256Checksum = sha256Checksum,
                packageName = packageName,
                uploaderName = member.displayName
            )
            _busy.value = false
            res.onSuccess {
                showBanner("App Update v$versionName published & users notified!")
                onDone()
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Failed to publish app update", isError = true)
            }
        }
    }

    fun toggleAppUpdatePublished(updateId: String, published: Boolean) {
        if (currentMember.value?.appRole()?.isAdminRole() != true) {
            showBanner("Access Denied: Only Admin can manage app updates.", isError = true)
            return
        }
        launchAction(if (published) "App update re-enabled." else "App update disabled / withdrawn.") {
            repository.setAppUpdatePublished(updateId, published)
        }
    }

    fun deleteAppUpdateRelease(updateId: String) {
        if (currentMember.value?.appRole()?.isAdminRole() != true) {
            showBanner("Access Denied: Only Admin can delete app updates.", isError = true)
            return
        }
        launchAction("Old app update release removed from history.") {
            repository.deleteAppUpdate(updateId)
        }
    }

    fun saveDailyTargetTask(
        existingId: String?,
        test: ExamTest,
        targetDateIst: String,
        bannerTitle: String,
        bannerSubtitle: String?,
        bannerImageBase64: String?
    ) {
        val gid = activeGroup.value?.id ?: return
        val member = currentMember.value ?: return
        if (!member.appRole().isAdminRole()) {
            showBanner("Access Denied: Only Admin can assign Daily Target sets.", isError = true)
            return
        }
        val todayIst = IstClock.todayIstKey()
        val isEnglish = (userProfileState.value as? UiState.Success)?.data?.langPreference() == LanguagePref.ENGLISH
        val label = when {
            targetDateIst == todayIst -> if (isEnglish) "Today's Target" else "आज का टारगेट (Today)"
            targetDateIst > todayIst -> if (isEnglish) "Upcoming Target ($targetDateIst)" else "आगामी टारगेट ($targetDateIst)"
            else -> if (isEnglish) "Target ($targetDateIst)" else "टारगेट ($targetDateIst)"
        }
        viewModelScope.launch {
            _busy.value = true
            val res = repository.saveDailyTarget(
                groupId = gid,
                existingId = existingId,
                test = test,
                targetDateIst = targetDateIst,
                bannerTitle = bannerTitle,
                bannerSubtitle = bannerSubtitle,
                bannerImageBase64 = bannerImageBase64,
                publisherName = member.displayName
            )
            _busy.value = false
            res.onSuccess {
                val msg = if (isEnglish) {
                    "$label banner saved! It is now visible on the Home screen for all users."
                } else {
                    "$label बैनर सेव हो गया! सभी यूज़र्स को होम स्क्रीन पर दिखेगा।"
                }
                showBanner(msg)
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Daily Target save failed", isError = true)
            }
        }
    }

    fun deleteDailyTargetTask(targetId: String) {
        val gid = activeGroup.value?.id ?: return
        if (currentMember.value?.appRole()?.isAdminRole() != true) {
            showBanner("Access Denied: Only Admin can delete Daily Target banners.", isError = true)
            return
        }
        val isEnglish = (userProfileState.value as? UiState.Success)?.data?.langPreference() == LanguagePref.ENGLISH
        val msg = if (isEnglish) "Daily Target banner deleted." else "Daily Target बैनर हटा दिया गया।"
        launchAction(msg) {
            repository.deleteDailyTarget(gid, targetId)
        }
    }

    private fun launchAction(successMsg: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _busy.value = true
            val res = block()
            _busy.value = false
            res.onSuccess {
                showBanner(successMsg)
            }.onFailure { err ->
                showBanner(err.localizedMessage ?: "Operation failed", isError = true)
            }
        }
    }

    private suspend fun handleStreamCatch(
        streamName: String,
        error: Throwable,
        emitError: suspend (String) -> Unit
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $streamName", rootError)
        emitError(rootError.localizedMessage ?: "Failed to sync $streamName")
    }

    private fun unwrapOrThrow(error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Stream warning", firestoreError ?: error)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    private companion object {
        const val TAG = "TreViewModel"
    }
}
