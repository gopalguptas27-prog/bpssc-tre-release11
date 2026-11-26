package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AdminContactProfile
import com.example.data.model.AppRole
import com.example.data.model.AppUpdateRelease
import com.example.data.model.CategoryIconConfig
import com.example.data.model.ContentViewRecord
import com.example.data.model.DailyTargetTask
import com.example.data.model.DiscussionComment
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamTest
import com.example.data.model.GroupMember
import com.example.data.model.GroupNotification
import com.example.data.model.LiveExam
import com.example.data.model.LiveExamDynamicStatus
import com.example.data.model.LiveExamSession
import com.example.data.model.PaperType
import com.example.data.model.QuestionPoll
import com.example.data.model.RankCategory
import com.example.data.model.RankedAspirant
import com.example.data.model.RankingPeriod
import com.example.data.model.TestAttempt
import com.example.data.model.UserProfile
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import com.example.domain.BulkQuestionParser
import com.example.domain.DocumentQuestionImporter
import com.example.domain.ExamScoringEngine
import com.example.domain.ImportFileFormat
import com.example.domain.OfficialStarterBank
import com.example.ui.screens.AboutFounderScreen
import com.example.ui.screens.AdminCategoryDefaultIconsDialog
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ContactAdminScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditQuestionDialog
import com.example.ui.screens.ExamTimerUrgency
import com.example.ui.screens.LeaderboardAndAnalyticsScreen
import com.example.ui.screens.LiveExamScreen
import com.example.ui.screens.formatExamCountdown
import com.example.ui.screens.resolveEffectiveSetIcon
import com.example.ui.screens.resolveExamTimerUrgency
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PollsAndDiscussionsScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.QuestionBankScreen
import com.example.ui.screens.QuestionPreviewDialog
import com.example.ui.screens.SetWiseRankScreen
import com.example.ui.screens.StudentLiveExamHubScreen
import com.example.ui.screens.TestResultAndReviewScreen
import com.example.ui.screens.TestsCatalogScreen
import com.example.ui.screens.UserAppUpdateScreen
import com.example.ui.theme.CoolingGlassTheme
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.viewmodel.ActiveExamState
import com.example.util.EditorAutoDraft
import com.example.util.ExamSessionStore
import com.example.util.ImageCodec
import com.example.util.PdfExportMode
import com.example.util.PersistedExamSession
import com.example.util.RankingCardGenerator
import com.example.util.TestDraftStore
import com.example.util.TestPdfGenerator
import com.example.util.TestSetDraft
import org.json.JSONArray
import org.json.JSONObject
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import com.example.ui.screens.AdminAppUpdateManagementSection
import com.example.util.ApkInspector
import com.example.util.ApkPickerHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TreRobolectricAndUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `ExamSessionStore persists exam across restart and preserves wall-clock deadline`() {
        val store1 = ExamSessionStore(context)
        store1.clearExamSession()

        val endEpoch = 1_800_000_000_000L
        val session = PersistedExamSession(
            userId = "user_aspirant_1",
            groupId = "group_1",
            testId = "test_gs_01",
            endTimeEpochMillis = endEpoch,
            totalDurationSeconds = 1800L, // 30 minutes
            currentIndex = 5,
            selectedAnswers = mapOf(0 to "A", 1 to "E", 4 to "C"),
            markedForReview = setOf(2, 5)
        )
        store1.saveExamSession(session)
        store1.setDarkGlassMode(true)

        // Simulate app restart with fresh ExamSessionStore instance
        val store2 = ExamSessionStore(context)
        assertTrue(store2.getDarkGlassMode())
        assertNull(store2.loadExamSession("other_user"))

        val restored = store2.loadExamSession("user_aspirant_1")
        assertNotNull(restored)
        assertEquals("test_gs_01", restored!!.testId)
        assertEquals(endEpoch, restored.endTimeEpochMillis)
        assertEquals(5, restored.currentIndex)
        assertEquals("A", restored.selectedAnswers[0])
        assertEquals("E", restored.selectedAnswers[1])
        assertEquals("C", restored.selectedAnswers[4])
        assertTrue(2 in restored.markedForReview && 5 in restored.markedForReview)

        // Wall-clock remaining calculation after 10 min backgrounding
        val remaining = ((restored.endTimeEpochMillis - (endEpoch - 1_200_000L)) / 1000L).coerceAtLeast(0L)
        assertEquals(1200L, remaining)

        // Expired deadline clamps to 0
        val expiredRemaining = ((restored.endTimeEpochMillis - (endEpoch + 5_000L)) / 1000L).coerceAtLeast(0L)
        assertEquals(0L, expiredRemaining)

        store2.clearExamSession()
        assertNull(store2.loadExamSession("user_aspirant_1"))
    }

    @Test
    fun `JSON parser parses Android org_json template and PDF generator blocks locked scheduled Answer Key`() {
        val jsonResult = BulkQuestionParser.parseJson(BulkQuestionParser.JSON_TEMPLATE, PaperType.CLASS_6_8_MATH_SCIENCE)
        assertTrue("JSON parse failed: ${jsonResult.exceptionOrNull()}", jsonResult.isSuccess)
        val parsed = jsonResult.getOrThrow()
        assertEquals(3, parsed.size)
        assertEquals("Hindi", parsed[0].subject)
        assertEquals(ExamQuestion.SECTION_LANGUAGE, parsed[0].section)
        assertEquals("Mathematics", parsed[1].subject)
        assertEquals("300", parsed[1].optionB)
        assertEquals("B", parsed[1].correctOption)
        assertEquals(ExamQuestion.SECTION_COMMON, parsed[1].section)
        assertEquals("Science", parsed[2].subject)
        assertEquals(ExamQuestion.SECTION_SUBJECT, parsed[2].section)

        val gs40 = OfficialStarterBank.commonGs40Questions().take(5)
        val scheduledTest = ExamTest(
            id = "pdf_test_sched",
            title = "TRE 4.0 Scheduled Mock PDF Test",
            paperType = PaperType.COMMON_GS.name,
            durationMinutes = 30L,
            questionCount = gs40.size.toLong(),
            questions = gs40.map { it.toMap() },
            answerReleaseMode = "SCHEDULED",
            scheduledAt = (System.currentTimeMillis() + 3_600_000L).toString()
        )
        val attempt = TestAttempt(
            id = "pdf_test_sched_u1",
            testId = scheduledTest.id,
            testTitle = scheduledTest.title,
            paperType = PaperType.COMMON_GS.name,
            userId = "u1",
            aspirantName = "Rahul Kumar",
            answers = mapOf("0" to gs40[0].correctOption, "1" to "E"),
            correctCount = 1,
            incorrectCount = 0,
            dontKnowCount = 1,
            notAttemptedCount = 3,
            totalScore = 0.0,
            maxMarks = 5.0,
            percentage = 0.0,
            negativeDeducted = 1.0
        )

        // Scheduled future release MUST block Answer Key PDF for non-admin aspirant before touching PdfDocument
        val blockedKey = TestPdfGenerator.generateAndExport(
            context = context,
            test = scheduledTest,
            attempt = attempt,
            aspirantName = "Rahul Kumar",
            mode = PdfExportMode.ANSWER_KEY,
            isAdmin = false,
            shareAfterSave = false
        )
        assertTrue(blockedKey.isFailure)
        assertTrue(blockedKey.exceptionOrNull()?.message?.contains("scheduled", ignoreCase = true) == true)
    }

    @Test
    fun `RankingCardGenerator produces 1080x1350 PNG cards for individual and group`() {
        val member = GroupMember(
            userId = "u1",
            displayName = "Priya Kumari",
            gsAverageScore = 31.33,
            gsHighestScore = 34.0,
            gsTestsAttempted = 4,
            langStatus = "PASS",
            langScore = 24.0
        )
        val attempt = TestAttempt(
            testId = "t1",
            testTitle = "TRE 4.0 Common GS Set 01",
            userId = "u1",
            aspirantName = "Priya Kumari",
            correctCount = 33,
            incorrectCount = 3,
            dontKnowCount = 2,
            notAttemptedCount = 2,
            totalScore = 31.33,
            maxMarks = 40.0,
            percentage = 78.33,
            negativeDeducted = 1.67
        )

        val indiv = RankingCardGenerator.generateIndividualResultCard(
            context = context,
            member = member,
            overallRank = 1,
            individualTestRank = 1,
            totalParticipants = 25,
            latestAttempt = attempt,
            testName = "TRE 4.0 Common GS Set 01"
        )
        assertTrue("Individual card failed: ${indiv.exceptionOrNull()}", indiv.isSuccess)
        val indivFile = indiv.getOrThrow()
        assertTrue(indivFile.exists() && indivFile.length() > 1000L)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(indivFile.absolutePath, bounds)
        assertEquals(1080, bounds.outWidth)
        assertEquals(1350, bounds.outHeight)

        val groupCard = RankingCardGenerator.generateGroupLeaderboardCard(
            context = context,
            groupName = "BPSC TRE 4.0 Batch",
            periodLabel = "Overall",
            ranked = listOf(RankedAspirant(rank = 1, member = member, displayScore = 31.33, testsAttempted = 4, testsAvailable = 5))
        )
        assertTrue("Group card failed: ${groupCard.exceptionOrNull()}", groupCard.isSuccess)
        assertTrue(groupCard.getOrThrow().exists())
    }

    @Test
    fun `LiveExamScreen hides answers during active exam and handles option selection`() {
        val questions = OfficialStarterBank.commonGs40Questions().take(3)
        val test = ExamTest(
            id = "live_test_1",
            title = "TRE 4.0 Common Mock 01",
            paperType = PaperType.COMMON_GS.name,
            durationMinutes = 30L,
            questionCount = 3
        )
        var pickedOption: String? = null
        val examState = ActiveExamState(
            test = test,
            questions = questions,
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            remainingSeconds = 1800L, // 30:00
            totalDurationSeconds = 1800L
        )

        composeRule.setContent {
            CoolingGlassTheme {
                LiveExamScreen(
                    examState = examState,
                    onSelectOption = { _, opt -> pickedOption = opt },
                    onClearResponse = {},
                    onToggleReview = {},
                    onNavigateQuestion = {},
                    onSubmit = {},
                    onAbort = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Timer displays exact 30:00 for GS paper
        composeRule.onNodeWithText("30:00").assertIsDisplayed()
        // Secret explanation MUST NEVER appear during active exam
        assertEquals(0, composeRule.onAllNodesWithText(questions[0].explanation, substring = true).fetchSemanticsNodes().size)

        // Scroll to & click Option E (प्रयास नहीं किया गया / Not Attempted)
        composeRule.onNodeWithTag("option_card_E").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("E", pickedOption)
        composeRule.onNodeWithText(ExamQuestion.DEFAULT_OPTION_E, substring = true).assertExists()

        // Open Question Palette sheet
        composeRule.onNodeWithTag("open_palette_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Question Palette").assertIsDisplayed()
    }

    @Test
    fun `TestResultAndReviewScreen hides keys before scheduled release and shows accuracy`() {
        val questions = OfficialStarterBank.commonGs40Questions().take(2)
        val scheduledTest = ExamTest(
            id = "sched_test",
            title = "Scheduled GS Paper",
            paperType = PaperType.COMMON_GS.name,
            durationMinutes = 30L,
            questionCount = 2,
            questions = questions.map { it.toMap() },
            answerReleaseMode = "SCHEDULED",
            scheduledAt = (System.currentTimeMillis() + 7_200_000L).toString()
        )
        val attempt = TestAttempt(
            testId = scheduledTest.id,
            testTitle = scheduledTest.title,
            userId = "u1",
            aspirantName = "Ankit",
            answers = mapOf("0" to "A", "1" to "E"),
            correctCount = 3,
            incorrectCount = 1,
            dontKnowCount = 1,
            notAttemptedCount = 0,
            totalScore = 2.67,
            maxMarks = 5.0,
            percentage = 53.3,
            negativeDeducted = 0.33
        )
        val aspirant = GroupMember(userId = "u1", displayName = "Ankit", role = AppRole.ASPIRANT.name)

        composeRule.setContent {
            CoolingGlassTheme {
                TestResultAndReviewScreen(
                    test = scheduledTest,
                    attempt = attempt,
                    member = aspirant,
                    overallRanking = emptyList(),
                    allAttemptsForTest = listOf(attempt),
                    onBack = {},
                    onReportQuestion = { _, _, _ -> },
                    onBannerMessage = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        // Displays Accuracy 75.0% (3 / (3 + 1) * 100)
        composeRule.onNodeWithText("Acc: 75.0%", substring = true).assertIsDisplayed()
        // Scheduled key lock banner is shown & Answer Key export is disabled
        composeRule.onNodeWithText("Key Locked").assertIsNotEnabled()
        // Verified explanation is NOT leaked prematurely
        assertEquals(0, composeRule.onAllNodesWithText(questions[0].explanation, substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun `AdminDashboardScreen shows Clear Old Sets and PENDING tests in approval queue`() {
        val pendingTest = ExamTest(
            id = "pending_1",
            title = "Common Paper Set 1",
            creatorName = "Suman",
            status = "PENDING",
            questionCount = 40,
            durationMinutes = 30
        )
        val admin = GroupMember(userId = "admin1", displayName = "Gopal Admin", role = AppRole.GROUP_ADMIN.name)

        composeRule.setContent {
            CoolingGlassTheme {
                AdminDashboardScreen(
                    currentMember = admin,
                    group = null,
                    members = listOf(admin),
                    tests = listOf(pendingTest),
                    attempts = emptyList(),
                    polls = emptyList(),
                    overallRanking = emptyList(),
                    onApproveOrRejectTest = { _, _ -> },
                    onAssignRole = { _, _ -> },
                    onPublishStarterSets = {},
                    onSendNotification = { _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Sets (1)").assertIsDisplayed()
        composeRule.onNodeWithTag("clear_all_old_sets_btn").assertExists()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("approve_test_pending_1"))
        composeRule.onNodeWithText("Common Paper Set 1").assertExists()
        composeRule.onNodeWithTag("approve_test_pending_1").assertExists()
        composeRule.onNodeWithText("Edit").assertExists()
    }

    @Test
    fun `AboutFounderScreen displays exact TRE 4_0 and Founded by Gopal Gupta branding`() {
        composeRule.setContent {
            CoolingGlassTheme {
                AboutFounderScreen()
            }
        }
        composeRule.waitForIdle()

        assertTrue(composeRule.onAllNodesWithText("TRE 4.0", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("Founded by Gopal Gupta", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertEquals(0, composeRule.onAllNodesWithText("Student", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
    }

    @Test
    fun `TestDraftStore persists and deletes drafts including Combined Test separate sections`() {
        val store1 = TestDraftStore(context)
        store1.loadAllDrafts().forEach { store1.deleteDraft(it.id) }

        val draft = TestSetDraft(
            id = "draft_comb_01",
            title = "My Combined Draft 01",
            paperType = PaperType.COMBINED,
            difficulty = "HARD",
            durationMinutes = 45,
            unattemptedPenalty = true,
            answerReleaseMode = "IMMEDIATE",
            questions = emptyList(),
            commonSectionQuestions = OfficialStarterBank.commonGs40Questions().take(3),
            languageSectionQuestions = OfficialStarterBank.language30Questions().take(2)
        )
        store1.saveDraft(draft)

        val store2 = TestDraftStore(context)
        val loaded = store2.loadAllDrafts()
        assertEquals(1, loaded.size)
        assertEquals("My Combined Draft 01", loaded[0].title)
        assertEquals(PaperType.COMBINED, loaded[0].paperType)
        assertEquals(45, loaded[0].durationMinutes)
        assertEquals(3, loaded[0].commonSectionQuestions.size)
        assertEquals(2, loaded[0].languageSectionQuestions.size)

        store2.deleteDraft("draft_comb_01")
        assertTrue(store2.loadAllDrafts().isEmpty())
    }

    @Test
    fun `CoolingGlassTheme enforces 97 to 99 percent opaque overlays and scrim in BOTH Light and Dark modes`() {
        var lightTokens: com.example.ui.theme.CoolingGlassTokens? = null
        var darkTokens: com.example.ui.theme.CoolingGlassTokens? = null

        composeRule.setContent {
            CoolingGlassTheme(darkTheme = false) {
                lightTokens = LocalCoolingGlass.current
            }
            CoolingGlassTheme(darkTheme = true) {
                darkTokens = LocalCoolingGlass.current
            }
        }
        composeRule.waitForIdle()

        val lt = lightTokens!!
        val dt = darkTokens!!

        // Verify 97-99% foreground opacity in Light Mode (prevents background text bleed-through)
        assertTrue("Light overlaySurfaceTop alpha must be >= 0.97f", lt.overlaySurfaceTop.alpha >= 0.97f)
        assertTrue("Light overlaySurfaceBottom alpha must be >= 0.97f", lt.overlaySurfaceBottom.alpha >= 0.97f)
        assertTrue("Light drawerSurface alpha must be >= 0.97f", lt.drawerSurface.alpha >= 0.97f)
        assertTrue("Light navBarSurface alpha must be >= 0.97f", lt.navBarSurface.alpha >= 0.97f)
        assertTrue("Light glassSurfaceStrong alpha must be >= 0.97f", lt.glassSurfaceStrong.alpha >= 0.97f)
        assertTrue("Light scrimColor alpha must be >= 0.50f", lt.scrimColor.alpha >= 0.50f)

        // Verify 97-99% foreground opacity in Dark Mode (prevents background text bleed-through)
        assertTrue("Dark overlaySurfaceTop alpha must be >= 0.97f", dt.overlaySurfaceTop.alpha >= 0.97f)
        assertTrue("Dark overlaySurfaceBottom alpha must be >= 0.97f", dt.overlaySurfaceBottom.alpha >= 0.97f)
        assertTrue("Dark drawerSurface alpha must be >= 0.97f", dt.drawerSurface.alpha >= 0.97f)
        assertTrue("Dark navBarSurface alpha must be >= 0.97f", dt.navBarSurface.alpha >= 0.97f)
        assertTrue("Dark glassSurfaceStrong alpha must be >= 0.97f", dt.glassSurfaceStrong.alpha >= 0.97f)
        assertTrue("Dark scrimColor alpha must be >= 0.65f", dt.scrimColor.alpha >= 0.65f)
    }

    @Test
    fun `QuestionBankScreen supports all 5 official formats, auto-durations, unique Set 1 naming and upload modes`() {
        val admin = GroupMember(userId = "a1", displayName = "Gopal Admin", role = AppRole.GROUP_ADMIN.name)
        val draftStore = TestDraftStore(context)

        composeRule.setContent {
            CoolingGlassTheme(darkTheme = true) {
                QuestionBankScreen(
                    currentMember = admin,
                    busy = false,
                    existingTests = emptyList(),
                    draftStore = draftStore,
                    onCreateTestSet = { _, _, _, _, _, _, _, _, _, _, _ -> },
                    onBannerMessage = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        // All 5 official formats displayed
        composeRule.onNodeWithText("30 Q • 15 Min").assertIsDisplayed()
        composeRule.onNodeWithText("40 Q • 30 Min").assertIsDisplayed()
        composeRule.onNodeWithText("70 Q • 45 Min").assertIsDisplayed()
        composeRule.onNodeWithText("150 Q • 2 Hr").assertIsDisplayed()
        composeRule.onNodeWithText("150 Q • 2h 15m").assertIsDisplayed()

        // Click Language -> auto-assigns 15 min & Language Paper Set 1
        composeRule.onNodeWithTag("paper_type_language").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("15").assertExists()
        composeRule.onNodeWithText("Language Paper Set 1").assertExists()

        // Click Class 6-8 Math & Science -> auto-assigns 120 min & unique Set 1 title
        composeRule.onNodeWithTag("paper_type_6_8").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("120").assertExists()
        composeRule.onNodeWithText("Class 6 to 8 Mathematics and Science Set 1").assertExists()

        // Click Class 9-10 Math -> auto-assigns 135 min & unique Set 1 title
        composeRule.onNodeWithTag("paper_type_9_10").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("135").assertExists()
        composeRule.onNodeWithText("Class 9-10 Mathematics Set 1").assertExists()

        // Click Combined -> auto-assigns 45 min and shows dedicated Combined Section Builder
        composeRule.onNodeWithTag("paper_type_combined").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("45").assertExists()
        composeRule.onNodeWithTag("question_bank_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("auto_combine_btn"))
        composeRule.onNodeWithTag("auto_combine_btn").assertExists()

        // Scroll back to top and switch to Common -> auto-assigns 30 min & verify all 5 file upload tiles exist
        composeRule.onNodeWithTag("question_bank_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("paper_type_common"))
        composeRule.onNodeWithTag("paper_type_common").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("30").assertExists()
        composeRule.onNodeWithTag("question_bank_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("upload_pdf_btn"))
        composeRule.onNodeWithTag("upload_pdf_btn").assertExists()
        composeRule.onNodeWithTag("upload_doc_btn").assertExists()
        composeRule.onNodeWithTag("upload_docx_btn").assertExists()
        composeRule.onNodeWithTag("upload_json_btn").assertExists()
        composeRule.onNodeWithTag("upload_csv_btn").assertExists()
    }

    @Test
    fun `PollsAndDiscussionsScreen supports Doubt Session screenshot uploads for askers and solvers and Option E Not Attempted`() {
        val aspirant = GroupMember(userId = "u2", displayName = "Ritu Kumari", role = AppRole.ASPIRANT.name)
        val existingDoubt = DiscussionComment(
            id = "d1",
            authorUid = "u3",
            authorName = "Vikash",
            message = "Please solve this quadratic equation from Class 10 SCERT.",
            commentType = "DOUBT"
        )

        composeRule.setContent {
            CoolingGlassTheme {
                PollsAndDiscussionsScreen(
                    currentMember = aspirant,
                    polls = emptyList(),
                    userPollVotes = emptyMap(),
                    discussions = listOf(existingDoubt),
                    initialTab = 1,
                    onLoadVote = {},
                    onCreatePoll = { _, _, _, _, _ -> },
                    onCastVote = { _, _ -> },
                    onFetchPrivateKey = { _, _ -> },
                    onFinalizePoll = { _, _, _, _ -> },
                    onUploadSolution = { _, _, _ -> },
                    onPostComment = { _, _, _, _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        // Doubt asker sees Question Screenshot + Camera upload buttons
        composeRule.onNodeWithTag("polls_discussions_list").performScrollToNode(hasTestTag("doubt_upload_gallery_btn"))
        composeRule.onNodeWithTag("doubt_upload_gallery_btn").assertExists()
        composeRule.onNodeWithTag("doubt_upload_camera_btn").assertExists()
        composeRule.onNodeWithText("Question Screenshot").assertExists()

        // Solver scrolls to doubt card and taps Solve / Reply -> switches composer to Solution Screenshot mode
        composeRule.onNodeWithTag("polls_discussions_list").performScrollToNode(hasText("Solve / Reply"))
        composeRule.onNodeWithText("Solve / Reply").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("polls_discussions_list").performScrollToNode(hasTestTag("doubt_upload_gallery_btn"))
        composeRule.onNodeWithText("Solution Screenshot").assertExists()

        // Exit reply mode and open 5-Opt Poll sheet -> displays Option E = प्रयास नहीं किया गया / Not Attempted
        composeRule.onNodeWithTag("polls_discussions_list").performScrollToNode(hasText("Cancel"))
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("polls_discussions_list").performScrollToNode(hasText("5-Opt Polls", substring = true))
        composeRule.onNodeWithText("5-Opt Polls", substring = true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("polls_discussions_list").performScrollToNode(hasTestTag("create_poll_btn"))
        composeRule.onNodeWithTag("create_poll_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(ExamQuestion.DEFAULT_OPTION_E, substring = true).assertExists()
    }

    private fun buildFull40QuestionBilingualObjectJson(): String {
        val subjects = listOf(
            "Mathematics",
            "Science",
            "Indian Geography",
            "Indian National Movement (1857–1947)",
            "Current Affairs (PIB)"
        )
        val keys = listOf("C", "B", "D", "A")
        val arr = JSONArray()
        for (i in 0 until 40) {
            val qNum = i + 1
            val subj = subjects[i / 8]
            val optsObj = JSONObject()
            val correctKey: String
            val qText: String
            val expText: String
            when (qNum) {
                1 -> {
                    qText = "निम्नलिखित कथनों पर विचार कीजिए / Consider the following statements:\n1. √(a² + b²) ≥ a + b जब a, b > 0 असत्य है।\n2. प्रत्येक वास्तविक संख्या के लिए (x − y)² ≥ 0 सत्य है।"
                    optsObj.put("A", "केवल 1 / Only 1")
                    optsObj.put("B", "केवल 2 / Only 2")
                    optsObj.put("C", "1 और 2 दोनों / Both 1 and 2")
                    optsObj.put("D", "न तो 1 और न ही 2 / Neither 1 nor 2")
                    optsObj.put("E", "प्रयास नहीं किया गया / Not Attempted")
                    correctKey = "C"
                    expText = "दोनों कथन गणितीय रूप से सत्य हैं क्योंकि √(a²+b²) < a+b (a,b>0) और (x−y)² ≥ 0 सदैव मान्य है।"
                }
                2 -> {
                    // Long Hindi-English bilingual + mathematical symbols test question
                    qText = "दीर्घ द्विभाषी गणितीय प्रश्न Q2: यदि f(x) = ∑(k=1..n) k² = n(n+1)(2n+1)/6 तथा π ≈ 3.14159, तो निम्नलिखित में से कौन-सा कथन सभी प्राकृत संख्याओं n ≥ 1 के लिए सत्य है?"
                    optsObj.put("A", "प्रथम n प्राकृत संख्याओं के वर्गों का योग n(n+1)/2 के बराबर होता है जो केवल n = 1 के लिए सत्य है / Sum of squares equals n(n+1)/2 only for n = 1")
                    optsObj.put("B", "समीकरण x² + 2x + 5 = 0 के मूल वास्तविक और असमान होते हैं क्योंकि विविक्तकर Δ = b² − 4ac < 0 / Roots are real and unequal because discriminant Δ < 0")
                    optsObj.put("C", "अपरिमेय संख्या √2 + √3 का दशमलव प्रसार अनवसानी अनावर्ती होता है और π ≠ 22/7 (सन्निकट मान ±0.00126) / Decimal expansion of √2 + √3 is non-terminating non-repeating and π ≠ 22/7")
                    optsObj.put("D", "किसी त्रिभुज के आंतरिक कोणों का योग 360° होता है तथा समबाहु त्रिभुज का प्रत्येक कोण 90° होता है / Sum of interior angles of a triangle is 360°")
                    optsObj.put("E", "प्रयास नहीं किया गया / Not Attempted")
                    correctKey = "C"
                    expText = "√2 + √3 अपरिमेय है और π एक अपरिमेय संख्या है जिसका सन्निकट मान 22/7 है।"
                }
                else -> {
                    qText = "TRE 4.0 प्रश्न $qNum ($subj): NCERT/SCERT आधारित मानक द्विभाषी प्रश्न #$qNum (मान: x² + y² = ${qNum * 5}°)"
                    optsObj.put("A", "विकल्प A #$qNum / Option A value for Q$qNum")
                    optsObj.put("B", "विकल्प B #$qNum / Option B value for Q$qNum")
                    optsObj.put("C", "विकल्प C #$qNum / Option C value for Q$qNum")
                    optsObj.put("D", "विकल्प D #$qNum / Option D value for Q$qNum")
                    optsObj.put("E", "प्रयास नहीं किया गया / Not Attempted")
                    correctKey = keys[i % keys.size]
                    expText = "प्रश्न $qNum की विस्तृत व्याख्या (सही उत्तर: $correctKey)।"
                }
            }
            val item = JSONObject().apply {
                put("number", qNum)
                put("subject", subj)
                put("topic", "Topic $qNum")
                put("source", "NCERT/SCERT Class 6–10")
                put("question", qText)
                put("options", optsObj)
                put("correctAnswer", correctKey)
                put("explanation", expText)
            }
            arr.put(item)
        }
        return JSONObject().put("questions", arr).toString(2)
    }

    @Test
    fun `JSON import parses full 40Q object-options file, array-options format, and strictly rejects missing options or correctAnswer`() {
        // 1. Full 40-question object-based JSON import via DocumentQuestionImporter
        val json40 = buildFull40QuestionBilingualObjectJson()
        val outcome = DocumentQuestionImporter.importFromBytes(
            bytes = json40.toByteArray(Charsets.UTF_8),
            fileName = "tre4_common_40q.json",
            format = ImportFileFormat.JSON,
            targetPaperType = PaperType.COMMON_GS
        )
        assertNull("Expected zero import errors, got: ${outcome.errorMessage}", outcome.errorMessage)
        assertEquals(40, outcome.questions.size)

        // Verify ALL 40 questions and ALL 5 options per question are populated and valid
        outcome.questions.forEachIndexed { idx, q ->
            assertTrue("Q${idx + 1} text blank", q.text.isNotBlank())
            assertTrue("Q${idx + 1} optionA blank", q.optionA.isNotBlank())
            assertTrue("Q${idx + 1} optionB blank", q.optionB.isNotBlank())
            assertTrue("Q${idx + 1} optionC blank", q.optionC.isNotBlank())
            assertTrue("Q${idx + 1} optionD blank", q.optionD.isNotBlank())
            assertEquals("Q${idx + 1} optionE must preserve bilingual text", "प्रयास नहीं किया गया / Not Attempted", q.optionE)
            assertTrue("Q${idx + 1} correctOption invalid: '${q.correctOption}'", q.correctOption in listOf("A", "B", "C", "D"))
        }

        // Verify Question 1 exact option mapping & correctAnswer = C (never A!)
        val q1 = outcome.questions[0]
        assertEquals("केवल 1 / Only 1", q1.optionA)
        assertEquals("केवल 2 / Only 2", q1.optionB)
        assertEquals("1 और 2 दोनों / Both 1 and 2", q1.optionC)
        assertEquals("न तो 1 और न ही 2 / Neither 1 nor 2", q1.optionD)
        assertEquals("प्रयास नहीं किया गया / Not Attempted", q1.optionE)
        assertEquals("C", q1.correctOption)

        // Passes full 40-question TRE 4.0 Common Paper validation
        val validation = BulkQuestionParser.validate(outcome.questions, PaperType.COMMON_GS)
        assertTrue("40Q validation errors: ${validation.errors}", validation.valid)

        // Round-trip Firestore toMap -> fromMap AND direct object-options map fromMap
        val roundTrip = ExamQuestion.fromMap(q1.toMap())
        assertEquals(q1.optionA, roundTrip.optionA)
        assertEquals(q1.optionB, roundTrip.optionB)
        assertEquals(q1.optionC, roundTrip.optionC)
        assertEquals(q1.optionD, roundTrip.optionD)
        assertEquals(q1.optionE, roundTrip.optionE)
        assertEquals("C", roundTrip.correctOption)

        val directObjectMap = ExamQuestion.fromMap(
            mapOf(
                "number" to 1,
                "question" to "Test Q",
                "options" to mapOf(
                    "A" to "केवल 1 / Only 1",
                    "B" to "केवल 2 / Only 2",
                    "C" to "1 और 2 दोनों / Both 1 and 2",
                    "D" to "न तो 1 और न ही 2 / Neither 1 nor 2",
                    "E" to "प्रयास नहीं किया गया / Not Attempted"
                ),
                "correctAnswer" to "C"
            )
        )
        assertEquals("1 और 2 दोनों / Both 1 and 2", directObjectMap.optionC)
        assertEquals("C", directObjectMap.correctOption)

        // 2. Array-based options (both string array & object array)
        val arrayJson = """
            [
              {
                "number": 1,
                "question": "Array string options Q1",
                "options": ["Opt A1", "Opt B1", "Opt C1", "Opt D1", "प्रयास नहीं किया गया / Not Attempted"],
                "correctAnswer": "D",
                "explanation": "D is right"
              },
              {
                "number": 2,
                "question": "Array object options Q2",
                "options": [
                  {"key": "A", "text": "Obj A2"},
                  {"key": "B", "text": "Obj B2"},
                  {"key": "C", "text": "Obj C2"},
                  {"key": "D", "text": "Obj D2"}
                ],
                "correctAnswer": "Option B",
                "explanation": "B is right"
              }
            ]
        """.trimIndent()
        val arrayRes = BulkQuestionParser.parseJson(arrayJson, PaperType.PRACTICE)
        assertTrue("Array JSON failed: ${arrayRes.exceptionOrNull()}", arrayRes.isSuccess)
        val arrayList = arrayRes.getOrThrow()
        assertEquals(2, arrayList.size)
        assertEquals("Opt C1", arrayList[0].optionC)
        assertEquals("D", arrayList[0].correctOption)
        assertEquals("Obj B2", arrayList[1].optionB)
        assertEquals("B", arrayList[1].correctOption)

        // 3. Strict JSON validation rejects missing Option C on Q2 and missing correctAnswer on Q1 (NO silent blank or fallback to A)
        val brokenJson = """
            [
              {
                "number": 1,
                "question": "Missing correctAnswer question",
                "options": {"A": "One", "B": "Two", "C": "Three", "D": "Four"}
              },
              {
                "number": 2,
                "question": "Missing Option C question",
                "options": {"A": "One", "B": "Two", "D": "Four"},
                "correctAnswer": "B"
              }
            ]
        """.trimIndent()
        val brokenRes = BulkQuestionParser.parseJson(brokenJson, PaperType.PRACTICE)
        assertTrue("Expected failure on broken JSON", brokenRes.isFailure)
        val errMsg = brokenRes.exceptionOrNull()?.message.orEmpty()
        assertTrue("Error must identify Q1 missing correctAnswer: $errMsg", errMsg.contains("Q1") && errMsg.contains("correctAnswer"))
        assertTrue("Error must identify Q2 missing Option C: $errMsg", errMsg.contains("Q2") && errMsg.contains("Option C"))
    }

    @Test
    fun `QuestionPreviewDialog displays all 5 bilingual options for Q1, highlights ONLY C (not A), never auto-selects, and scrolls long options`() {
        val imported = BulkQuestionParser.parseJson(buildFull40QuestionBilingualObjectJson(), PaperType.COMMON_GS).getOrThrow()

        composeRule.setContent {
            CoolingGlassTheme {
                QuestionPreviewDialog(
                    questions = imported,
                    initialIndex = 0,
                    paperType = PaperType.COMMON_GS,
                    onDismiss = {},
                    onEditQuestion = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Verify Question 1 displays all 5 options accurately (NO blank cards)
        composeRule.onNodeWithTag("option_card_A").performScrollTo()
        composeRule.onNodeWithTag("option_text_A", useUnmergedTree = true).assertTextEquals("केवल 1 / Only 1")
        composeRule.onNodeWithTag("option_card_B").performScrollTo()
        composeRule.onNodeWithTag("option_text_B", useUnmergedTree = true).assertTextEquals("केवल 2 / Only 2")
        composeRule.onNodeWithTag("option_card_C").performScrollTo()
        composeRule.onNodeWithTag("option_text_C", useUnmergedTree = true).assertTextEquals("1 और 2 दोनों / Both 1 and 2")
        composeRule.onNodeWithTag("option_card_D").performScrollTo()
        composeRule.onNodeWithTag("option_text_D", useUnmergedTree = true).assertTextEquals("न तो 1 और न ही 2 / Neither 1 nor 2")
        composeRule.onNodeWithTag("option_card_E").performScrollTo()
        composeRule.onNodeWithTag("option_text_E", useUnmergedTree = true).assertTextEquals("प्रयास नहीं किया गया / Not Attempted")

        // Verify ONLY Option C is marked as Correct Answer — NEVER Option A!
        composeRule.onNodeWithTag("option_correct_icon_C", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("option_badge_C", useUnmergedTree = true).assertExists()
        assertEquals(0, composeRule.onAllNodesWithTag("option_correct_icon_A", useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("option_badge_A", useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("option_correct_icon_B", useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("option_correct_icon_D", useUnmergedTree = true).fetchSemanticsNodes().size)
        // Verify zero student-selected badges in Preview
        assertEquals(0, composeRule.onAllNodesWithText("Your Answer").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Selected").fetchSemanticsNodes().size)

        // Navigate to Question 2 (long Hindi-English bilingual + mathematical symbols) and verify scroll & full display
        composeRule.onNodeWithText("Next").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Question Preview (2 / 40)").assertExists()
        composeRule.onNodeWithTag("option_card_C").performScrollTo()
        composeRule.onNodeWithTag("option_text_C", useUnmergedTree = true)
            .assertTextEquals("अपरिमेय संख्या √2 + √3 का दशमलव प्रसार अनवसानी अनावर्ती होता है और π ≠ 22/7 (सन्निकट मान ±0.00126) / Decimal expansion of √2 + √3 is non-terminating non-repeating and π ≠ 22/7")
        composeRule.onNodeWithTag("option_correct_icon_C", useUnmergedTree = true).assertExists()
        assertEquals(0, composeRule.onAllNodesWithTag("option_correct_icon_A", useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test
    fun `EditQuestionDialog preserves all 5 bilingual options including Option E and exact correctAnswer C`() {
        val imported = BulkQuestionParser.parseJson(buildFull40QuestionBilingualObjectJson(), PaperType.COMMON_GS).getOrThrow()
        var savedQuestion: ExamQuestion? = null

        composeRule.setContent {
            CoolingGlassTheme {
                EditQuestionDialog(
                    question = imported[0],
                    questionNumber = 1,
                    paperType = PaperType.COMMON_GS,
                    forcedSection = ExamQuestion.SECTION_COMMON,
                    onDismiss = {},
                    onSave = { savedQuestion = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("केवल 1 / Only 1").assertExists()
        composeRule.onNodeWithText("1 और 2 दोनों / Both 1 and 2").assertExists()
        composeRule.onNodeWithText("प्रयास नहीं किया गया / Not Attempted").assertExists()

        composeRule.onNodeWithText("Save Question Changes").performScrollTo().performClick()
        composeRule.waitForIdle()

        val saved = savedQuestion
        assertNotNull(saved)
        assertEquals("C", saved!!.correctOption)
        assertEquals("1 और 2 दोनों / Both 1 and 2", saved.optionC)
        assertEquals("प्रयास नहीं किया गया / Not Attempted", saved.optionE)
    }

    @Test
    fun `LiveExamScreen and TestResultAndReviewScreen display imported 40Q options cleanly and separate Correct Answer vs Student Selection`() {
        val imported = BulkQuestionParser.parseJson(buildFull40QuestionBilingualObjectJson(), PaperType.COMMON_GS).getOrThrow()
        val test = ExamTest(
            id = "imported_40q_test",
            title = "Common Paper Set 1",
            paperType = PaperType.COMMON_GS.name,
            durationMinutes = 30L,
            questionCount = 40L,
            questions = imported.map { it.toMap() },
            answerReleaseMode = "IMMEDIATE"
        )

        // Verify scoring on Option E = 0 marks and Option C = +1 mark
        val eval = ExamScoringEngine.evaluate(
            test = test,
            questions = imported,
            selectedAnswers = mapOf(0 to "C", 1 to "E") + (2 until 40).associateWith { "E" }
        )
        assertEquals(1, eval.correctCount)
        assertEquals(0, eval.incorrectCount)
        assertEquals(39, eval.dontKnowCount)
        assertEquals(0, eval.notAttemptedCount)
        assertEquals(1.0, eval.totalScore, 1e-9)

        // 1. LiveExamScreen (Student Test Screen): Question 1 shows all 5 options, zero auto-selection, zero answer leak
        var studentChoice: String? = null
        val activeState = ActiveExamState(
            test = test,
            questions = imported,
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            remainingSeconds = 1800L,
            totalDurationSeconds = 1800L
        )
        var showResultScreen by androidx.compose.runtime.mutableStateOf(false)
        val attemptWrongA = TestAttempt(
            testId = test.id,
            testTitle = test.title,
            paperType = PaperType.COMMON_GS.name,
            userId = "u_student",
            aspirantName = "Aman Kumar",
            answers = mapOf("0" to "A", "1" to "C"), // Q1 student picked A (wrong), correct is C!
            correctCount = 1,
            incorrectCount = 1,
            dontKnowCount = 0,
            notAttemptedCount = 38,
            totalScore = 1.0 - 39.0 / 3.0,
            maxMarks = 40.0,
            percentage = 0.0,
            negativeDeducted = 13.0
        )

        composeRule.setContent {
            CoolingGlassTheme {
                if (!showResultScreen) {
                    LiveExamScreen(
                        examState = activeState,
                        onSelectOption = { _, opt -> studentChoice = opt },
                        onClearResponse = {},
                        onToggleReview = {},
                        onNavigateQuestion = {},
                        onSubmit = {},
                        onAbort = {}
                    )
                } else {
                    TestResultAndReviewScreen(
                        test = test,
                        attempt = attemptWrongA,
                        member = GroupMember(userId = "u_student", displayName = "Aman Kumar"),
                        overallRanking = emptyList(),
                        allAttemptsForTest = listOf(attemptWrongA),
                        onBack = {},
                        onReportQuestion = { _, _, _ -> },
                        onBannerMessage = { _, _ -> }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Verify all 5 options display cleanly on Student Test Screen
        composeRule.onNodeWithTag("option_card_A").performScrollTo()
        composeRule.onNodeWithTag("option_text_A", useUnmergedTree = true).assertTextEquals("केवल 1 / Only 1")
        composeRule.onNodeWithTag("option_card_B").performScrollTo()
        composeRule.onNodeWithTag("option_text_B", useUnmergedTree = true).assertTextEquals("केवल 2 / Only 2")
        composeRule.onNodeWithTag("option_card_C").performScrollTo()
        composeRule.onNodeWithTag("option_text_C", useUnmergedTree = true).assertTextEquals("1 और 2 दोनों / Both 1 and 2")
        composeRule.onNodeWithTag("option_card_D").performScrollTo()
        composeRule.onNodeWithTag("option_text_D", useUnmergedTree = true).assertTextEquals("न तो 1 और न ही 2 / Neither 1 nor 2")
        composeRule.onNodeWithTag("option_card_E").performScrollTo()
        composeRule.onNodeWithTag("option_text_E", useUnmergedTree = true).assertTextEquals("प्रयास नहीं किया गया / Not Attempted")
        assertEquals(0, composeRule.onAllNodesWithText("Correct Answer").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Selected").fetchSemanticsNodes().size)

        // Student taps C
        composeRule.onNodeWithTag("option_card_C").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("C", studentChoice)

        // 2. Switch to TestResultAndReviewScreen -> open View Solution -> verify separated Correct Answer (C) vs Your Answer (A)
        showResultScreen = true
        composeRule.waitForIdle()
        // Inline Question-by-Question options MUST NOT auto-display on summary Result page
        assertEquals(0, composeRule.onAllNodesWithTag("download_solution_pdf_btn").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("My Attempt PDF").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("view_solution_in_app_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("1 और 2 दोनों / Both 1 and 2").performScrollTo()
        composeRule.onNodeWithText("केवल 1 / Only 1").performScrollTo().assertExists()
        composeRule.onNodeWithText("1 और 2 दोनों / Both 1 and 2").assertExists()
        // Option A shows user's wrong selection ("First Attempt") while Option C shows "Correct Answer"
        composeRule.onAllNodesWithTag("option_incorrect_icon_A", useUnmergedTree = true)[0].assertExists()
        composeRule.onAllNodesWithTag("option_correct_icon_C", useUnmergedTree = true)[0].assertExists()
        assertFalse(composeRule.onAllNodesWithText("First Attempt").fetchSemanticsNodes().isEmpty())
        assertFalse(composeRule.onAllNodesWithText("Correct Answer").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `ExamSessionStore Pause and Resume preserves exact remaining seconds across restart without decay`() {
        val store1 = ExamSessionStore(context)
        store1.clearAllSessions()

        val pausedSession = PersistedExamSession(
            userId = "u_pause_1",
            groupId = "g1",
            testId = "test_pause_40",
            endTimeEpochMillis = 0L,
            totalDurationSeconds = 1800L,
            currentIndex = 12,
            selectedAnswers = mapOf(0 to "A", 1 to "C", 11 to "E"),
            markedForReview = setOf(4, 12),
            isPaused = true,
            pausedRemainingSeconds = 847L // 14:07 exact frozen remaining time
        )
        store1.savePausedSession(pausedSession)

        // Simulate app restart
        val store2 = ExamSessionStore(context)
        val restored = store2.loadPausedSession("u_pause_1", "test_pause_40")
        assertNotNull(restored)
        assertTrue(restored!!.isPaused)
        assertEquals(847L, restored.pausedRemainingSeconds)
        assertEquals(12, restored.currentIndex)
        assertEquals("C", restored.selectedAnswers[1])
        assertEquals("E", restored.selectedAnswers[11])
        assertTrue(4 in restored.markedForReview)

        val allPaused = store2.loadAllPausedSessions("u_pause_1")
        assertEquals(1, allPaused.size)
        assertEquals(847L, allPaused["test_pause_40"]?.pausedRemainingSeconds)

        store2.clearPausedSession("u_pause_1", "test_pause_40")
        assertNull(store2.loadPausedSession("u_pause_1", "test_pause_40"))
    }

    @Test
    fun `LiveExamScreen Pause button opens exact-time confirmation and invokes onPause`() {
        val questions = OfficialStarterBank.commonGs40Questions().take(2)
        val test = ExamTest(
            id = "pause_ui_test",
            title = "Common Paper Set 1",
            paperType = PaperType.COMMON_GS.name,
            durationMinutes = 30L,
            questionCount = 2
        )
        var pausedCalled = false
        val examState = ActiveExamState(
            test = test,
            questions = questions,
            currentIndex = 0,
            selectedAnswers = mapOf(0 to "B"),
            remainingSeconds = 1234L, // 20:34
            totalDurationSeconds = 1800L
        )

        composeRule.setContent {
            CoolingGlassTheme {
                LiveExamScreen(
                    examState = examState,
                    onSelectOption = { _, _ -> },
                    onClearResponse = {},
                    onToggleReview = {},
                    onNavigateQuestion = {},
                    onPause = { pausedCalled = true },
                    onSubmit = {},
                    onAbort = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("20:34").assertIsDisplayed()
        composeRule.onNodeWithTag("exam_pause_btn").performClick()
        composeRule.waitForIdle()

        // Confirmation dialog displays exact remaining time 20:34
        composeRule.onNodeWithText("Remaining time (20:34)", substring = true).assertExists()
        composeRule.onNodeWithTag("confirm_pause_exam_btn").performClick()
        composeRule.waitForIdle()
        assertTrue(pausedCalled)
    }

    @Test
    fun `QuestionBankScreen removes Clear button at set finalization`() {
        val admin = GroupMember(userId = "a1", displayName = "Gopal Admin", role = AppRole.GROUP_ADMIN.name)
        val existing = ExamTest(
            id = "edit_1",
            title = "Common Paper Set 1",
            paperType = PaperType.COMMON_GS.name,
            durationMinutes = 30L,
            questionCount = 40L,
            questions = OfficialStarterBank.commonGs40Questions().map { it.toMap() }
        )

        composeRule.setContent {
            CoolingGlassTheme {
                QuestionBankScreen(
                    currentMember = admin,
                    busy = false,
                    existingTests = listOf(existing),
                    editingTest = existing,
                    draftStore = null,
                    onCreateTestSet = { _, _, _, _, _, _, _, _, _, _, _ -> },
                    onBannerMessage = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("question_bank_list")
            .performScrollToNode(hasTestTag("publish_question_set_btn"))
        composeRule.onNodeWithTag("preview_set_btn").assertExists()
        // "Clear" button MUST NOT exist in the set finalization card
        assertEquals(0, composeRule.onAllNodesWithText("Clear").fetchSemanticsNodes().size)
    }

    @Test
    fun `LeaderboardAndAnalyticsScreen shows 7 categories, hides Language on Common, opens Weekly & Monthly, and opens Ranker Breakdown`() {
        val m1 = GroupMember(userId = "u1", displayName = "Ananya", langStatus = "PASS", langScore = 24.0)
        val m2 = GroupMember(userId = "u2", displayName = "Rohit", langStatus = "FAIL", langScore = 5.0)
        val commonTest = ExamTest(id = "set_c1", title = "Common Paper Set 1", paperType = PaperType.COMMON_GS.name, questionCount = 40, status = "PUBLISHED")
        val langTest = ExamTest(id = "set_l1", title = "Language Paper Set 1", paperType = PaperType.LANGUAGE.name, questionCount = 30, status = "PUBLISHED")
        val attempts = listOf(
            TestAttempt(id = "a1", testId = "set_c1", testTitle = "Common Paper Set 1", paperType = PaperType.COMMON_GS.name, userId = "u1", correctCount = 33, totalScore = 33.0, meritMarks = 33.0, maxMarks = 40.0, totalMeritMarks = 40.0),
            TestAttempt(id = "a2", testId = "set_l1", testTitle = "Language Paper Set 1", paperType = PaperType.LANGUAGE.name, userId = "u1", correctCount = 24, totalScore = 24.0, maxMarks = 30.0, hindiScore = 18.0, englishScore = 6.0, langPassed = true)
        )

        var currentPeriod by mutableStateOf(RankingPeriod.INDIVIDUAL_TEST)
        var currentCategory by mutableStateOf(RankCategory.COMMON)

        composeRule.setContent {
            val ranked = ExamScoringEngine.computeCategoryRankings(
                category = currentCategory,
                period = currentPeriod,
                members = listOf(m1, m2),
                attempts = attempts,
                tests = listOf(commonTest, langTest)
            )
            CoolingGlassTheme {
                LeaderboardAndAnalyticsScreen(
                    group = null,
                    currentMember = m1,
                    rankedList = ranked,
                    overallRanked = ranked,
                    selectedPeriod = currentPeriod,
                    selectedCategory = currentCategory,
                    tests = listOf(commonTest, langTest),
                    allAttempts = attempts,
                    onSelectPeriod = { currentPeriod = it },
                    onSelectCategory = { currentCategory = it },
                    onBannerMessage = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        // Displays ONLY 7 Category cards and keeps Language badge hidden on COMMON
        composeRule.onNodeWithTag("rank_category_selector_card").assertExists()
        RankCategory.entries.forEach { cat ->
            composeRule.onNodeWithTag("rank_category_${cat.name}").assertExists()
        }
        assertEquals(0, composeRule.onAllNodesWithText("24.00/30 PASS").fetchSemanticsNodes().size)

        // Switch to LANGUAGE category -> Ananya is #1 on podium; check All Scores sheet shows PASS
        composeRule.onNodeWithTag("rank_category_LANGUAGE").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasTestTag("podium_rank_1"))
        composeRule.onNodeWithTag("podium_rank_1").assertExists()

        // Tap ranker Ananya on podium -> opens detailed Ranker Breakdown dialog
        composeRule.onNodeWithTag("podium_rank_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ranker_detail_dialog", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Total Marks", substring = true).assertExists()
        composeRule.onNodeWithText("Correct").assertExists()
        composeRule.onAllNodesWithContentDescription("Close")[0].performClick()
        composeRule.waitForIdle()

        // Also verify All Scores sheet for LANGUAGE shows PASS badge
        composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasText("All Scores"))
        composeRule.onNodeWithText("All Scores").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasText("PASS"))
        composeRule.onNodeWithText("PASS").assertExists()
        composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasText("Leaderboard"))
        composeRule.onNodeWithText("Leaderboard").performClick()
        composeRule.waitForIdle()

        // Switch back to COMMON -> Open Weekly & Monthly separately
        composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasTestTag("rank_category_COMMON"))
        composeRule.onNodeWithTag("rank_category_COMMON").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasTestTag("ranking_period_WEEKLY"))
        composeRule.onNodeWithTag("ranking_period_WEEKLY").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasText("Weekly Report (Last 7 Days)", substring = true))
        composeRule.onNodeWithText("Weekly Report (Last 7 Days)", substring = true).assertExists()

        composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasTestTag("ranking_period_MONTHLY"))
        composeRule.onNodeWithTag("ranking_period_MONTHLY").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasText("Monthly Report (", substring = true))
        composeRule.onNodeWithText("Monthly Report (", substring = true).assertExists()
    }

    @Test
    fun `AdminDashboardScreen enforces Two-Step Delete, Per-Member Set Approval selection, and Fresh Launch Trial Wipe`() {
        val admin = GroupMember(userId = "u_admin", displayName = "Gopal Admin", role = AppRole.GROUP_ADMIN.name)
        val memberA = GroupMember(userId = "u_a", displayName = "Priya Kumari", role = AppRole.ASPIRANT.name)
        val memberB = GroupMember(userId = "u_b", displayName = "Rahul Verma", role = AppRole.ASPIRANT.name)
        val pendingTest = ExamTest(
            id = "p_set_1",
            title = "Common Paper Set 1",
            creatorName = "Gopal Admin",
            status = "PENDING",
            questionCount = 40,
            durationMinutes = 30
        )

        var deletedId: String? = null
        var approvedForAllResult: Boolean? = null
        var approvedMemberIdsResult: List<String>? = null
        var wipedTrialData = false

        composeRule.setContent {
            CoolingGlassTheme {
                AdminDashboardScreen(
                    currentMember = admin,
                    group = null,
                    members = listOf(admin, memberA, memberB),
                    tests = listOf(pendingTest),
                    attempts = emptyList(),
                    polls = emptyList(),
                    overallRanking = emptyList(),
                    onApproveOrRejectTest = { _, _ -> },
                    onApproveTestWithAccess = { _, _, forAll, ids ->
                        approvedForAllResult = forAll
                        approvedMemberIdsResult = ids
                    },
                    onDeleteTest = { deletedId = it },
                    onWipeAllTrialData = { wipedTrialData = true },
                    onAssignRole = { _, _ -> },
                    onSendNotification = { _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        // 1. Per-Member Approval Dialog shows ALL joined members and submits exact selected UIDs
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("approve_test_p_set_1"))
        composeRule.onNodeWithTag("approve_test_p_set_1").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("approve_mode_specific").performScrollTo().performClick()
        composeRule.waitForIdle()
        // Uncheck Rahul Verma (u_b) and Admin (u_admin) so only Priya Kumari (u_a) is selected
        composeRule.onNodeWithTag("clear_selected_members_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("member_select_u_a").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_member_approval_btn").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(false, approvedForAllResult)
        assertEquals(listOf("u_a"), approvedMemberIdsResult)

        // 2. Two-Step Delete: Step 1 does NOT delete yet; Step 2 deletes
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("delete_pending_test_p_set_1"))
        composeRule.onNodeWithTag("delete_pending_test_p_set_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("admin_delete_step1_btn").performClick()
        composeRule.waitForIdle()
        assertNull("Step 1 MUST NOT delete the set yet!", deletedId)
        composeRule.onNodeWithTag("admin_delete_step2_btn").performClick()
        composeRule.waitForIdle()
        assertEquals("p_set_1", deletedId)

        // 3. Two-Step Fresh Launch Trial Wipe
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("fresh_launch_wipe_btn"))
        composeRule.onNodeWithTag("fresh_launch_wipe_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wipe_trial_step1_btn").performClick()
        composeRule.waitForIdle()
        assertFalse("Step 1 MUST NOT wipe yet!", wipedTrialData)
        composeRule.onNodeWithTag("wipe_trial_step2_btn").performClick()
        composeRule.waitForIdle()
        assertTrue(wipedTrialData)
    }

    @Test
    fun `RBAC strictly blocks regular USER from QuestionBankScreen and removes edit-delete-create on TestsCatalogScreen while adding Eye Rank button`() {
        val regularUser = GroupMember(userId = "u_regular", displayName = "Rohan Kumar", role = AppRole.ASPIRANT.name)
        val eligibleTest = ExamTest(
            id = "cat_t1",
            title = "Common Paper Set 1",
            paperType = PaperType.COMMON_GS.name,
            questionCount = 40,
            durationMinutes = 30,
            status = "PUBLISHED"
        )
        var showCatalog by mutableStateOf(false)
        var viewedRankTestId: String? = null

        composeRule.setContent {
            CoolingGlassTheme {
                if (!showCatalog) {
                    QuestionBankScreen(
                        currentMember = regularUser,
                        busy = false,
                        existingTests = emptyList(),
                        draftStore = null,
                        onCreateTestSet = { _, _, _, _, _, _, _, _, _, _, _ -> },
                        onBannerMessage = { _, _ -> }
                    )
                } else {
                    TestsCatalogScreen(
                        tests = listOf(eligibleTest),
                        myAttempts = emptyList(),
                        currentMember = regularUser,
                        onStartTest = {},
                        onViewResult = { _, _ -> },
                        onViewTestRanking = { viewedRankTestId = it.id },
                        onBannerMessage = { _, _ -> }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Regular USER sees Admin Access Only guard on QuestionBankScreen
        composeRule.onNodeWithTag("question_bank_admin_only_guard").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("publish_question_set_btn").fetchSemanticsNodes().size)

        // TestsCatalogScreen hides Edit/Delete/Create Set and displays Eye Rank icon below difficulty
        showCatalog = true
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("catalog_create_set_btn").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("view_ranking_eye_cat_t1").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("cat_t1", viewedRankTestId)
    }

    @Test
    fun `ContactAdminScreen and UserAppUpdateScreen display accurate states for empty vs configured data`() {
        var contact by mutableStateOf<AdminContactProfile?>(null)
        var showUpdateScreen by mutableStateOf(false)
        var availableUpdate by mutableStateOf<AppUpdateRelease?>(null)

        composeRule.setContent {
            CoolingGlassTheme {
                if (!showUpdateScreen) {
                    ContactAdminScreen(
                        adminContact = contact,
                        fallbackAdminName = "Gopal Gupta",
                        groupName = "BPSSC TRE 4.0",
                        onFeedback = { _, _ -> }
                    )
                } else {
                    UserAppUpdateScreen(
                        latestUpdate = availableUpdate,
                        allPublishedUpdates = listOfNotNull(availableUpdate),
                        onFeedback = { _, _ -> }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // 1. Admin WhatsApp not configured -> shows Not Available & hides WhatsApp button
        composeRule.onNodeWithText("WhatsApp Number Not Available").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("contact_admin_whatsapp_btn").fetchSemanticsNodes().size)

        // 2. Admin WhatsApp configured -> shows normalized number & WhatsApp button
        contact = AdminContactProfile(
            adminName = "Gopal Gupta",
            adminWhatsAppNumber = "+919876543210"
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithText("+919876543210").assertIsDisplayed()
        composeRule.onNodeWithTag("contact_admin_whatsapp_btn").assertExists()

        // 3. UserAppUpdateScreen: Up to date -> shows exact message
        showUpdateScreen = true
        composeRule.waitForIdle()
        composeRule.onNodeWithText("You are using the latest version.").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("download_app_update_btn").fetchSemanticsNodes().size)

        // 4. UserAppUpdateScreen: Newer release available -> shows Download Update button
        availableUpdate = AppUpdateRelease(
            id = "upd_2",
            versionName = "2.0.0",
            versionCode = BuildConfig.VERSION_CODE.toLong() + 1L,
            title = "TRE 4.0 Major Update",
            releaseNotes = "Added WhatsApp support and 70Q validation.",
            downloadUrl = "https://example.com/tre4-v2.apk",
            published = true
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("download_app_update_btn").assertExists()
    }

    @Test
    fun `ApkPickerHelper uses ACTION_GET_CONTENT with wildcard MIME and no restrictive EXTRA_MIME_TYPES`() {
        val pickerIntent = ApkPickerHelper.createApkPickerIntent()
        assertEquals(Intent.ACTION_GET_CONTENT, pickerIntent.action)
        assertEquals("*/*", pickerIntent.type)
        assertTrue(pickerIntent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertFalse(pickerIntent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, true))
        assertNull(
            "EXTRA_MIME_TYPES must NOT be set because Android OEM pickers hide .apk files when MIME arrays are set",
            pickerIntent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)
        )
        assertTrue((pickerIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)

        val chooser = ApkPickerHelper.createApkPickerChooser("Select Signed .APK File")
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals("Select Signed .APK File", chooser.getStringExtra(Intent.EXTRA_TITLE))

        // extractUriFromResult supports both direct data URI and ClipData URI
        val directUri = Uri.parse("content://com.android.providers.downloads.documents/document/raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2Ftre4_v2.apk")
        assertEquals(directUri, ApkPickerHelper.extractUriFromResult(Intent().setData(directUri)))

        val clipUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fbpssc_release.apk")
        val clipIntent = Intent().apply {
            clipData = ClipData.newRawUri("apk", clipUri)
        }
        assertEquals(clipUri, ApkPickerHelper.extractUriFromResult(clipIntent))
        assertNull(ApkPickerHelper.extractUriFromResult(null))
    }

    @Test
    fun `ApkInspector resolves SAF display names and validates extension, ZIP structure, signature, package, version, and SHA-256`() {
        // 1. SAF encoded URI filename resolution (Downloads, Documents, Internal Storage)
        val downloadsUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2Ftre4_v2_signed.apk")
        assertEquals("tre4_v2_signed.apk", ApkInspector.resolveDisplayName(context, downloadsUri))

        val docUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fnotes.pdf")
        assertEquals("notes.pdf", ApkInspector.resolveDisplayName(context, docUri))

        val cacheDir = context.cacheDir

        // 2. Reject non-.apk extension clearly
        val pdfFile = File(cacheDir, "study_notes.pdf").apply { writeText("PDF content") }
        val nonApkRes = ApkInspector.inspectApkFile(
            context = context,
            apkFile = pdfFile,
            displayName = "study_notes.pdf",
            currentPackageName = context.packageName,
            currentVersionCode = 1L
        )
        assertNotNull(nonApkRes.validationError)
        assertTrue(nonApkRes.validationError!!.contains(".apk", ignoreCase = true))

        // 3. Reject renamed plain text file with .apk extension
        val fakeTextApk = File(cacheDir, "fake_text.apk").apply { writeText("This is not a zip archive".repeat(20)) }
        val fakeTextRes = ApkInspector.inspectApkFile(
            context = context,
            apkFile = fakeTextApk,
            displayName = "fake_text.apk",
            currentPackageName = context.packageName,
            currentVersionCode = 1L
        )
        assertNotNull(fakeTextRes.validationError)
        assertTrue(fakeTextRes.validationError!!.contains("not a valid Android APK archive", ignoreCase = true))
        assertEquals(64, fakeTextRes.sha256Checksum.length)

        // 4. Reject renamed ZIP archive missing AndroidManifest.xml
        val zipNoManifest = createTestZip(
            File(cacheDir, "renamed_zip.apk"),
            mapOf("readme.txt" to "Not an APK".repeat(25).toByteArray())
        )
        val noManifestRes = ApkInspector.inspectApkFile(
            context = context,
            apkFile = zipNoManifest,
            displayName = "renamed_zip.apk",
            currentPackageName = context.packageName,
            currentVersionCode = 1L
        )
        assertNotNull(noManifestRes.validationError)
        assertTrue(noManifestRes.validationError!!.contains("AndroidManifest.xml", ignoreCase = true))

        // 5. Reject unsigned APK (valid manifest + matching PackageInfo, but no v1/v2/v3 signature)
        val unsignedApk = createTestZip(
            File(cacheDir, "unsigned_release.apk"),
            mapOf("AndroidManifest.xml" to "compiled-manifest-placeholder".repeat(12).toByteArray())
        )
        val validPkgInfo = PackageInfo().apply {
            packageName = context.packageName
            versionName = "2.1.0"
            @Suppress("DEPRECATION")
            versionCode = BuildConfig.VERSION_CODE + 1
            longVersionCode = BuildConfig.VERSION_CODE.toLong() + 1L
        }
        shadowOf(context.packageManager).setPackageArchiveInfo(unsignedApk.absolutePath, validPkgInfo)
        val unsignedRes = ApkInspector.inspectApkFile(
            context = context,
            apkFile = unsignedApk,
            displayName = "unsigned_release.apk",
            currentPackageName = context.packageName,
            currentVersionCode = BuildConfig.VERSION_CODE.toLong()
        )
        assertFalse(unsignedRes.isSigned)
        assertNotNull(unsignedRes.validationError)
        assertTrue(
            "Expected Unsigned APK error, got: ${unsignedRes.validationError}",
            unsignedRes.validationError!!.contains("Unsigned APK", ignoreCase = true)
        )

        // 6. Accept valid signed APK (AndroidManifest.xml + META-INF/CERT.RSA + matching package + higher versionCode)
        val signedApk = createTestZip(
            File(cacheDir, "bpssc_tre4_v2.1.0.apk"),
            mapOf(
                "AndroidManifest.xml" to "compiled-manifest-placeholder".repeat(12).toByteArray(),
                "META-INF/CERT.RSA" to "pkcs7-signature-block-bytes".repeat(8).toByteArray()
            )
        )
        val expectedSha256 = MessageDigest.getInstance("SHA-256")
            .digest(signedApk.readBytes())
            .joinToString("") { "%02x".format(it) }
        shadowOf(context.packageManager).setPackageArchiveInfo(signedApk.absolutePath, validPkgInfo)
        val signedRes = ApkInspector.inspectApkFile(
            context = context,
            apkFile = signedApk,
            displayName = "bpssc_tre4_v2.1.0.apk",
            currentPackageName = context.packageName,
            currentVersionCode = BuildConfig.VERSION_CODE.toLong()
        )
        assertNull("Expected valid signed APK, got error: ${signedRes.validationError}", signedRes.validationError)
        assertTrue(signedRes.isSigned)
        assertTrue(signedRes.matchesCurrentPackage)
        assertTrue(signedRes.isNewerThanInstalled)
        assertEquals("bpssc_tre4_v2.1.0.apk", signedRes.fileName)
        assertEquals("2.1.0", signedRes.versionName)
        assertEquals(BuildConfig.VERSION_CODE.toLong() + 1L, signedRes.versionCode)
        assertEquals(signedApk.length(), signedRes.fileSizeBytes)
        assertEquals(expectedSha256, signedRes.sha256Checksum)

        // 7. Reject package mismatch & non-newer versionCode clearly
        val wrongPkgInfo = PackageInfo().apply {
            packageName = "com.other.unrelated"
            versionName = "2.1.0"
            @Suppress("DEPRECATION")
            versionCode = BuildConfig.VERSION_CODE + 1
            longVersionCode = BuildConfig.VERSION_CODE.toLong() + 1L
        }
        shadowOf(context.packageManager).setPackageArchiveInfo(signedApk.absolutePath, wrongPkgInfo)
        val mismatchRes = ApkInspector.inspectApkFile(
            context = context,
            apkFile = signedApk,
            displayName = "bpssc_tre4_v2.1.0.apk",
            currentPackageName = context.packageName,
            currentVersionCode = BuildConfig.VERSION_CODE.toLong()
        )
        assertNotNull(mismatchRes.validationError)
        assertTrue(mismatchRes.validationError!!.contains("Package mismatch", ignoreCase = true))

        val oldVerInfo = PackageInfo().apply {
            packageName = context.packageName
            versionName = BuildConfig.VERSION_NAME
            @Suppress("DEPRECATION")
            versionCode = BuildConfig.VERSION_CODE
            longVersionCode = BuildConfig.VERSION_CODE.toLong()
        }
        shadowOf(context.packageManager).setPackageArchiveInfo(signedApk.absolutePath, oldVerInfo)
        val oldVerRes = ApkInspector.inspectApkFile(
            context = context,
            apkFile = signedApk,
            displayName = "bpssc_tre4_v2.1.0.apk",
            currentPackageName = context.packageName,
            currentVersionCode = BuildConfig.VERSION_CODE.toLong()
        )
        assertNotNull(oldVerRes.validationError)
        assertTrue(oldVerRes.validationError!!.contains("must be greater", ignoreCase = true))

        // 8. Verify APK Signing Scheme v2/v3 magic ("APK Sig Block 42") detection
        val v2SignedFile = createZipWithApkSigBlockMagic(File(cacheDir, "v2_signed.apk"))
        assertTrue(ApkInspector.hasApkSigningBlockMagic(v2SignedFile))
        assertFalse(ApkInspector.hasApkSigningBlockMagic(unsignedApk))
    }

    @Test
    fun `AdminAppUpdateManagementSection renders APK picker button and version management controls`() {
        composeRule.setContent {
            CoolingGlassTheme {
                AdminAppUpdateManagementSection(
                    updates = emptyList(),
                    busy = false,
                    onPublish = { _, _, _, _, _, _, _, _, _, _ -> },
                    onTogglePublished = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("admin_pick_apk_btn").assertIsDisplayed()
        composeRule.onNodeWithText("Pick & Verify Signed .APK (SHA-256)", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("update_version_name_input").assertExists()
        composeRule.onNodeWithTag("update_version_code_input").assertExists()
        composeRule.onNodeWithTag("update_download_url_input").assertExists()
        composeRule.onNodeWithTag("publish_app_update_btn").assertExists()
    }

    private fun createTestZip(target: File, entries: Map<String, ByteArray>): File {
        ZipOutputStream(FileOutputStream(target)).use { zos ->
            entries.forEach { (name, data) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }
        return target
    }

    private fun createZipWithApkSigBlockMagic(target: File): File {
        val baseFile = createTestZip(
            File(target.parentFile, "base_${target.name}"),
            mapOf("AndroidManifest.xml" to "manifest-v2".repeat(16).toByteArray())
        )
        val raw = baseFile.readBytes()
        // Locate EOCD signature 0x06054b50
        var eocd = -1
        for (i in raw.size - 22 downTo 0) {
            if (raw[i] == 0x50.toByte() && raw[i + 1] == 0x4B.toByte() &&
                raw[i + 2] == 0x05.toByte() && raw[i + 3] == 0x06.toByte()
            ) {
                eocd = i
                break
            }
        }
        assertTrue("EOCD must exist in generated zip", eocd >= 0)
        val cdOffset = ((raw[eocd + 16].toInt() and 0xFF)) or
            ((raw[eocd + 17].toInt() and 0xFF) shl 8) or
            ((raw[eocd + 18].toInt() and 0xFF) shl 16) or
            ((raw[eocd + 19].toInt() and 0xFF) shl 24)

        val sigBlock = ByteArray(32)
        val magic = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)
        System.arraycopy(magic, 0, sigBlock, 16, 16)

        val patched = ByteArray(raw.size + sigBlock.size)
        System.arraycopy(raw, 0, patched, 0, cdOffset)
        System.arraycopy(sigBlock, 0, patched, cdOffset, sigBlock.size)
        System.arraycopy(raw, cdOffset, patched, cdOffset + sigBlock.size, raw.size - cdOffset)

        val newCdOffset = cdOffset + sigBlock.size
        val newEocd = eocd + sigBlock.size
        patched[newEocd + 16] = (newCdOffset and 0xFF).toByte()
        patched[newEocd + 17] = ((newCdOffset shr 8) and 0xFF).toByte()
        patched[newEocd + 18] = ((newCdOffset shr 16) and 0xFF).toByte()
        patched[newEocd + 19] = ((newCdOffset shr 24) and 0xFF).toByte()

        target.writeBytes(patched)
        baseFile.delete()
        return target
    }

    @Test
    fun `Version upgrade preserves applicationId, updates BuildConfig to 1_4_0 code 5, and keeps v1_0 and v1_1 local storage intact`() {
        // 1. Verify native Android BuildConfig values
        assertEquals("1.4.0", BuildConfig.VERSION_NAME)
        assertEquals(5, BuildConfig.VERSION_CODE)
        assertEquals("com.aistudio.tre4aspirant.kxpzqm", BuildConfig.APPLICATION_ID)

        // 2. Verify existing v1.0/v1.1 SharedPreferences keys (ExamSessionStore & TestDraftStore) remain readable with zero reset
        val sessionPrefs = context.getSharedPreferences("tre4_exam_session_prefs", Context.MODE_PRIVATE)
        sessionPrefs.edit().putBoolean("cooling_glass_dark_mode", true).commit()
        val store = ExamSessionStore(context)
        assertTrue("Existing theme preference from v1.0/v1.1 must be preserved", store.getDarkGlassMode())

        val v1Paused = PersistedExamSession(
            userId = "existing_user_uid",
            groupId = "existing_group_id",
            testId = "existing_set_1",
            endTimeEpochMillis = 0L,
            totalDurationSeconds = 1800L,
            currentIndex = 7,
            selectedAnswers = mapOf(0 to "C", 3 to "B"),
            markedForReview = setOf(5),
            isPaused = true,
            pausedRemainingSeconds = 1120L
        )
        store.savePausedSession(v1Paused)
        val restored = ExamSessionStore(context).loadPausedSession("existing_user_uid", "existing_set_1")
        assertNotNull(restored)
        assertEquals(1120L, restored!!.pausedRemainingSeconds)
        assertEquals("C", restored.selectedAnswers[0])

        // 3. Verify v1.4.0 (versionCode 5) recognizes release versionCode 5 as already up to date
        val relV140 = AppUpdateRelease(
            id = "rel_5",
            versionName = "1.4.0",
            versionCode = 5L,
            title = "BPSSC TRE 4.0 v1.4.0",
            releaseNotes = "Live Exam Arena, Secure Mode, 7 Test Categories, One-Step Back & Admin JSON Safety",
            downloadUrl = "https://example.com/bpssc-tre4-1.4.0.apk",
            published = true
        )
        assertFalse(relV140.isMandatoryFor(BuildConfig.VERSION_CODE.toLong()))

        composeRule.setContent {
            CoolingGlassTheme {
                UserAppUpdateScreen(
                    latestUpdate = relV140,
                    allPublishedUpdates = listOf(relV140),
                    onFeedback = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Current Installed: v1.4.0 (Build 5)", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("You are using the latest version.").assertIsDisplayed()
    }

    @Test
    fun `computeBannerAspectRatio adapts dynamically to landscape, square, portrait, and extreme uploads`() {
        assertEquals(16f / 9f, com.example.ui.screens.computeBannerAspectRatio(1600, 900), 0.01f)
        assertEquals(1.0f, com.example.ui.screens.computeBannerAspectRatio(720, 720), 0.01f)
        assertEquals(0.75f, com.example.ui.screens.computeBannerAspectRatio(600, 800), 0.01f)
        // Clamped extremes
        assertEquals(0.45f, com.example.ui.screens.computeBannerAspectRatio(100, 1000), 0.01f)
        assertEquals(3.2f, com.example.ui.screens.computeBannerAspectRatio(4000, 500), 0.01f)
        // Invalid dimensions fallback safely
        assertEquals(16f / 9f, com.example.ui.screens.computeBannerAspectRatio(0, 0), 0.01f)
    }

    @Test
    fun `QuestionTimingBenchmark and ExamSessionStore accurately compute prior averages and persist per-question timers`() {
        val a1 = TestAttempt(
            id = "t1_u1",
            testId = "t1",
            userId = "u1",
            timeSpentSeconds = 120L,
            questionTimesSec = mapOf("0" to 20L, "1" to 40L)
        )
        val a2 = TestAttempt(
            id = "t1_u2",
            testId = "t1",
            userId = "u2",
            timeSpentSeconds = 180L,
            questionTimesSec = mapOf("0" to 40L, "1" to 60L)
        )
        val otherTest = TestAttempt(
            id = "t2_u1",
            testId = "t2",
            userId = "u1",
            timeSpentSeconds = 999L,
            questionTimesSec = mapOf("0" to 500L)
        )

        val bench = com.example.ui.viewmodel.QuestionTimingBenchmark.computeForTest("t1", listOf(a1, a2, otherTest))
        assertEquals(2, bench.priorTimedAttemptsCount)
        assertEquals(2, bench.priorTotalAttemptsCount)
        assertEquals(150L, bench.priorAvgTotalSeconds)
        assertEquals(30L, bench.avgSecondsByQuestion[0])
        assertEquals(50L, bench.avgSecondsByQuestion[1])

        // Verify ExamSessionStore persists and restores per-question times across restart/pause
        val store = ExamSessionStore(context)
        val paused = PersistedExamSession(
            userId = "u_timer",
            groupId = "g1",
            testId = "t1",
            endTimeEpochMillis = 0L,
            totalDurationSeconds = 1800L,
            currentIndex = 1,
            selectedAnswers = mapOf(0 to "B"),
            markedForReview = emptySet(),
            isPaused = true,
            pausedRemainingSeconds = 1715L,
            questionTimesSec = mapOf(0 to 24L, 1 to 61L)
        )
        store.savePausedSession(paused)
        val restored = ExamSessionStore(context).loadPausedSession("u_timer", "t1")
        assertNotNull(restored)
        assertEquals(24L, restored!!.questionTimesSec[0])
        assertEquals(61L, restored.questionTimesSec[1])
    }

    @Test
    fun `LiveExamScreen displays live per-question timer alongside prior-attempt average time comparison`() {
        val sampleQuestions = listOf(
            ExamQuestion(
                number = 1,
                subject = "Mathematics",
                text = "What is 15 × 4?",
                optionA = "50",
                optionB = "60",
                optionC = "65",
                optionD = "70",
                optionE = "Not Attempted",
                correctOption = "B",
                explanation = "15 × 4 = 60."
            )
        )
        val test = ExamTest(
            id = "t_live_timer",
            title = "Live Timer Mock",
            questionCount = 1,
            durationMinutes = 30
        )
        val examState = ActiveExamState(
            test = test,
            questions = sampleQuestions,
            currentIndex = 0,
            remainingSeconds = 1750L,
            totalDurationSeconds = 1800L,
            questionTimesSec = mapOf(0 to 22L),
            priorQuestionAvgTimesSec = mapOf(0 to 30L),
            priorTimedAttemptsCount = 3,
            priorTotalAttemptsCount = 3,
            priorAvgTotalSeconds = 90L
        )

        composeRule.setContent {
            CoolingGlassTheme {
                LiveExamScreen(
                    examState = examState,
                    onSelectOption = { _, _ -> },
                    onClearResponse = {},
                    onToggleReview = {},
                    onNavigateQuestion = {},
                    onPause = {},
                    onSubmit = {},
                    onAbort = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("live_question_timer_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Q1 Time: 00:22", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Prev Q1 Avg: 00:30", substring = true).assertIsDisplayed()
    }

    @Test
    fun `TestResultAndReviewScreen exposes View Solution in-app viewer and direct Solution PDF download`() {
        val sampleQuestions = listOf(
            ExamQuestion(
                number = 1,
                subject = "Polity",
                text = "Which article deals with Equality before Law?",
                optionA = "Article 12",
                optionB = "Article 14",
                optionC = "Article 19",
                optionD = "Article 21",
                optionE = "Not Attempted",
                correctOption = "B",
                explanation = "Article 14 guarantees equality before law and equal protection of laws."
            ),
            ExamQuestion(
                number = 2,
                subject = "History",
                text = "Champaran Satyagraha took place in which year?",
                optionA = "1915",
                optionB = "1917",
                optionC = "1919",
                optionD = "1920",
                optionE = "Not Attempted",
                correctOption = "B",
                explanation = "Mahatma Gandhi led the Champaran Satyagraha in Bihar in 1917."
            )
        )
        val rawQ = sampleQuestions.map {
            mapOf(
                "number" to it.number,
                "subject" to it.subject,
                "text" to it.text,
                "optionA" to it.optionA,
                "optionB" to it.optionB,
                "optionC" to it.optionC,
                "optionD" to it.optionD,
                "optionE" to it.optionE,
                "correctOption" to it.correctOption,
                "explanation" to it.explanation
            )
        }
        val test = ExamTest(
            id = "t_sol_1",
            title = "TRE 4.0 Solution Viewer Test",
            paperType = PaperType.COMMON_GS.name,
            questionCount = 2,
            durationMinutes = 30,
            answerReleaseMode = "IMMEDIATE",
            questions = rawQ
        )
        val attempt = TestAttempt(
            id = "t_sol_1_u1",
            testId = "t_sol_1",
            testTitle = test.title,
            userId = "u1",
            aspirantName = "Ananya Kumari",
            answers = mapOf("0" to "B", "1" to "A"),
            questionTimesSec = mapOf("0" to 18L, "1" to 32L),
            correctCount = 1,
            incorrectCount = 1,
            totalScore = 0.67,
            maxMarks = 2.0,
            percentage = 33.5
        )
        val member = GroupMember(userId = "u1", displayName = "Ananya Kumari", role = AppRole.ASPIRANT.name)
        val ranking = ExamScoringEngine.computeRankings(
            period = RankingPeriod.INDIVIDUAL_TEST,
            members = listOf(member),
            attempts = listOf(attempt),
            tests = listOf(test),
            selectedTestId = test.id
        )

        composeRule.setContent {
            CoolingGlassTheme {
                TestResultAndReviewScreen(
                    test = test,
                    attempt = attempt,
                    member = member,
                    overallRanking = ranking,
                    allAttemptsForTest = listOf(attempt),
                    onBack = {},
                    onReportQuestion = { _, _, _ -> },
                    onBannerMessage = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        // Verify View Solution exists and removed 3 items (Solution PDF, My Attempt PDF, inline Q-by-Q) are gone
        composeRule.onNodeWithTag("view_solution_in_app_btn").assertExists()
        assertEquals(0, composeRule.onAllNodesWithTag("download_solution_pdf_btn").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("My Attempt PDF").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Question-by-Question Solutions", substring = true).fetchSemanticsNodes().size)

        // Open dedicated in-app View Solution screen
        composeRule.onNodeWithTag("view_solution_in_app_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("in_app_solution_viewer").assertIsDisplayed()
        composeRule.onNodeWithText("Article 14 guarantees equality before law", substring = true).assertExists()

        // Filter to Wrong answers only -> shows Q2 (Champaran Satyagraha) with its full solution explanation
        composeRule.onNodeWithTag("solution_filter_WRONG").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Champaran Satyagraha took place in which year?", substring = true).assertExists()
        composeRule.onNodeWithText("Mahatma Gandhi led the Champaran Satyagraha in Bihar in 1917.", substring = true).assertExists()
    }

    @Test
    fun `LeaderboardAndAnalyticsScreen shows non-scrolling 5-period icon bar, non-duplicated podium top 3, and Rank 4+ below`() {
        val m1 = GroupMember(userId = "u1", displayName = "Aarav Top", role = AppRole.ASPIRANT.name)
        val m2 = GroupMember(userId = "u2", displayName = "Sneha Second", role = AppRole.ASPIRANT.name)
        val m3 = GroupMember(userId = "u3", displayName = "Vikash Third", role = AppRole.ASPIRANT.name)
        val m4 = GroupMember(userId = "u4", displayName = "Nisha Fourth", role = AppRole.ASPIRANT.name)
        val test1 = ExamTest(id = "set_a", title = "Set 1", paperType = PaperType.COMMON_GS.name, status = "PUBLISHED", questionCount = 40)
        val attempts = listOf(
            TestAttempt(id = "a1", testId = "set_a", testTitle = "Set 1", paperType = PaperType.COMMON_GS.name, userId = "u1", totalScore = 38.0, meritMarks = 38.0, maxMarks = 40.0, totalMeritMarks = 40.0),
            TestAttempt(id = "a2", testId = "set_a", testTitle = "Set 1", paperType = PaperType.COMMON_GS.name, userId = "u2", totalScore = 34.5, meritMarks = 34.5, maxMarks = 40.0, totalMeritMarks = 40.0),
            TestAttempt(id = "a3", testId = "set_a", testTitle = "Set 1", paperType = PaperType.COMMON_GS.name, userId = "u3", totalScore = 31.0, meritMarks = 31.0, maxMarks = 40.0, totalMeritMarks = 40.0),
            TestAttempt(id = "a4", testId = "set_a", testTitle = "Set 1", paperType = PaperType.COMMON_GS.name, userId = "u4", totalScore = 27.0, meritMarks = 27.0, maxMarks = 40.0, totalMeritMarks = 40.0)
        )
        val ranked = ExamScoringEngine.computeCategoryRankings(
            category = RankCategory.COMMON,
            period = RankingPeriod.INDIVIDUAL_TEST,
            members = listOf(m1, m2, m3, m4),
            attempts = attempts,
            tests = listOf(test1)
        )

        composeRule.setContent {
            CoolingGlassTheme {
                LeaderboardAndAnalyticsScreen(
                    group = null,
                    currentMember = m1,
                    rankedList = ranked,
                    overallRanked = ranked,
                    selectedPeriod = RankingPeriod.INDIVIDUAL_TEST,
                    selectedCategory = RankCategory.COMMON,
                    tests = listOf(test1),
                    allAttempts = attempts,
                    onSelectPeriod = {},
                    onSelectCategory = {},
                    onBannerMessage = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        // All 5 period icon buttons are displayed simultaneously in a single non-scrolling bar
        composeRule.onNodeWithTag("ranking_period_icon_bar").assertIsDisplayed()
        RankingPeriod.entries.forEach { period ->
            composeRule.onNodeWithTag("ranking_period_${period.name}").assertIsDisplayed()
        }

        // Celebratory podium card shows top 3 and does NOT duplicate them below (only Rank #4 starts below)
        composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasTestTag("celebration_podium_card"))
        composeRule.onNodeWithTag("celebration_podium_card").assertIsDisplayed()
        composeRule.onNodeWithText("Congratulations Top Performers!", substring = true).assertIsDisplayed()

        composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasTestTag("ranks_below_podium_header"))
        composeRule.onNodeWithTag("leaderboard_row_u4").assertExists()
        assertEquals(0, composeRule.onAllNodesWithTag("leaderboard_row_u1").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("leaderboard_row_u2").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("leaderboard_row_u3").fetchSemanticsNodes().size)
    }

    @Test
    fun `PreTestInstructionScreen gates Start button behind declaration checkbox and passes selected language`() {
        val part5Test = ExamTest(
            id = "t_part5_instr",
            title = "TRE 4.0 Part 5 Full Mock",
            paperType = PaperType.CLASS_6_8_MATH_SCIENCE.name,
            setType = com.example.data.model.SetType.PART_5_FULL_MATH_SCI.name,
            questionCount = 150,
            durationMinutes = 120
        )
        var startedLang: com.example.data.model.LanguagePref? = null

        composeRule.setContent {
            CoolingGlassTheme {
                com.example.ui.screens.PreTestInstructionScreen(
                    test = part5Test,
                    defaultLanguage = com.example.data.model.LanguagePref.BILINGUAL,
                    onBack = {},
                    onStartExam = { lang -> startedLang = lang }
                )
            }
        }
        composeRule.waitForIdle()

        // Start button is disabled until declaration checkbox is checked
        composeRule.onNodeWithTag("pre_test_start_btn").assertIsNotEnabled()
        composeRule.onNodeWithTag("pre_test_declaration_checkbox").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pre_test_start_btn").assertIsEnabled()

        composeRule.onNodeWithTag("pre_test_start_btn").performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.data.model.LanguagePref.BILINGUAL, startedLang)
    }

    @Test
    fun `LiveExamScreen 5-status Question Palette toggles cleanly between Grid and Question List views`() {
        val qs = listOf(
            ExamQuestion(
                number = 1,
                subject = "Mathematics",
                text = "What is the value of 9 squared?",
                optionA = "72", optionB = "81", optionC = "90", optionD = "99",
                optionE = ExamQuestion.DEFAULT_OPTION_E,
                correctOption = "B",
                explanation = "9 × 9 = 81"
            ),
            ExamQuestion(
                number = 2,
                subject = "Science",
                text = "Which gas is most abundant in Earth's atmosphere?",
                optionA = "Oxygen", optionB = "Nitrogen", optionC = "Carbon Dioxide", optionD = "Argon",
                optionE = ExamQuestion.DEFAULT_OPTION_E,
                correctOption = "B",
                explanation = "Nitrogen is ~78%"
            )
        )
        val examState = ActiveExamState(
            test = ExamTest(id = "t_pal", title = "Palette Mock", questionCount = 2, durationMinutes = 30),
            questions = qs,
            currentIndex = 0,
            selectedAnswers = mapOf(0 to "B"),
            markedForReview = setOf(1),
            visitedQuestions = setOf(0, 1),
            remainingSeconds = 1800L,
            totalDurationSeconds = 1800L
        )

        composeRule.setContent {
            CoolingGlassTheme {
                LiveExamScreen(
                    examState = examState,
                    onSelectOption = { _, _ -> },
                    onClearResponse = {},
                    onToggleReview = {},
                    onNavigateQuestion = {},
                    onSubmit = {},
                    onAbort = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("open_palette_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Question Palette", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("palette_view_toggle").assertExists().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Which gas is most abundant", substring = true).assertExists()
    }

    @Test
    fun `SolutionAnalysisScreen No-Spoiler Re-attempt mode hides answer until picked and never mutates Original Attempt`() {
        val qs = listOf(
            ExamQuestion(
                number = 1,
                subject = "History",
                text = "Who founded the Maurya Empire?",
                optionA = "Ashoka",
                optionB = "Chandragupta Maurya",
                optionC = "Bindusara",
                optionD = "Bimbisara",
                optionE = ExamQuestion.DEFAULT_OPTION_E,
                correctOption = "B",
                explanation = "Chandragupta Maurya founded the Maurya Empire in 322 BCE."
            )
        )
        val test = ExamTest(
            id = "t_reatt",
            title = "Re-attempt Verification Mock",
            paperType = PaperType.COMMON_GS.name,
            questionCount = 1,
            answerReleaseMode = "IMMEDIATE"
        )
        val originalAttempt = TestAttempt(
            id = "t_reatt_u1",
            testId = "t_reatt",
            testTitle = test.title,
            userId = "u1",
            answers = mapOf("0" to "A"), // Wrong in original attempt!
            questionTimesSec = mapOf("0" to 45L),
            correctCount = 0,
            incorrectCount = 1,
            totalScore = -0.33,
            meritMarks = -0.33,
            maxMarks = 1.0,
            totalMeritMarks = 1.0
        )
        var savedReattemptMap: Map<String, String> = emptyMap()

        composeRule.setContent {
            CoolingGlassTheme {
                com.example.ui.screens.SolutionAnalysisScreen(
                    test = test,
                    attempt = originalAttempt,
                    questions = qs,
                    priorBench = com.example.ui.viewmodel.QuestionTimingBenchmark.computeForTest(test.id, listOf(originalAttempt)),
                    isAdmin = false,
                    exporting = false,
                    onToggleBookmark = { _, _ -> },
                    onRecordReattempt = { reMap -> savedReattemptMap = reMap },
                    onDirectDownloadPdf = {},
                    onReportQuestion = {},
                    onClose = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Normal Solution Analysis mode displays the explanation immediately
        composeRule.onNodeWithTag("solution_explanation_card").assertExists()

        // Toggle Re-attempt Mode ON -> No-Spoiler Mode hides explanation until an option is tapped!
        composeRule.onNodeWithTag("reattempt_mode_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("solution_explanation_card").assertDoesNotExist()
        composeRule.onNodeWithTag("reattempt_verdict_card").assertDoesNotExist()

        // Pick correct option B in Re-attempt Mode -> verdict & explanation unlock, separate map recorded, Original Attempt untouched!
        composeRule.onNodeWithTag("solution_opt_B").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reattempt_verdict_card").assertExists()
        composeRule.onNodeWithTag("solution_explanation_card").assertExists()
        assertEquals("B", savedReattemptMap["0"])
        assertEquals("A", originalAttempt.answers["0"])
        assertEquals(-0.33, originalAttempt.totalScore, 1e-9)
    }

    @Test
    fun `AdminReportedIssuesSection displays open question ticket and triggers Admin resolution`() {
        val ticket = com.example.data.model.DiscussionComment(
            id = "rep_ticket_1",
            targetType = "TEST",
            targetId = "set_1_Q12",
            authorUid = "u_user",
            authorName = "Riya Kumari",
            message = "Q12: Option B should be correct per NCERT Class 8.",
            commentType = "REPORT_ANSWER",
            resolved = false
        )
        var resolvedId: String? = null
        var resolvedFlag: Boolean? = null

        composeRule.setContent {
            CoolingGlassTheme {
                com.example.ui.screens.AdminReportedIssuesSection(
                    discussions = listOf(ticket),
                    onResolveIssue = { id, res, _ ->
                        resolvedId = id
                        resolvedFlag = res
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("admin_reported_issues_section").assertIsDisplayed()
        composeRule.onNodeWithTag("resolve_issue_rep_ticket_1").performClick()
        composeRule.waitForIdle()
        assertEquals("rep_ticket_1", resolvedId)
        assertEquals(true, resolvedFlag)
    }

    @Test
    fun `AdminAppUpdateManagementSection protects Active Live release from deletion and allows deleting older history`() {
        val activeRel = AppUpdateRelease(
            id = "upd_4",
            versionName = "1.3.0",
            versionCode = 4L,
            title = "Active Release",
            published = true,
            isActive = true
        )
        val oldRel = AppUpdateRelease(
            id = "upd_2",
            versionName = "1.1.0",
            versionCode = 2L,
            title = "Older Release",
            published = false,
            isActive = false
        )
        var deletedId: String? = null

        composeRule.setContent {
            CoolingGlassTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AdminAppUpdateManagementSection(
                        updates = listOf(activeRel, oldRel),
                        busy = false,
                        onPublish = { _, _, _, _, _, _, _, _, _, _ -> },
                        onTogglePublished = { _, _ -> },
                        onDelete = { deletedId = it }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Active release has NO delete button; older release DOES have delete button
        assertEquals(0, composeRule.onAllNodesWithTag("delete_app_update_upd_4").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("delete_app_update_upd_2").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_delete_app_update_btn").performClick()
        composeRule.waitForIdle()
        assertEquals("upd_2", deletedId)
    }

    @Test
    fun `NotificationsScreen supports clearing individual and all notifications`() {
        val n1 = GroupNotification(id = "n1", title = "New Test Published", body = "Common Set 1 is live", type = "TEST")
        val n2 = GroupNotification(id = "n2", title = "Rank Updated", body = "Category leaderboard updated", type = "RANKING")
        var clearedSingleId: String? = null
        var clearedAll = false

        composeRule.setContent {
            CoolingGlassTheme {
                NotificationsScreen(
                    notifications = listOf(n1, n2),
                    isAdmin = false,
                    onSendNotification = { _, _, _ -> },
                    onClearNotification = { clearedSingleId = it.id },
                    onClearAllNotifications = { clearedAll = true },
                    onNotificationClick = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("clear_notif_n1").performClick()
        composeRule.waitForIdle()
        assertEquals("n1", clearedSingleId)

        composeRule.onNodeWithTag("clear_all_notifications_btn").performClick()
        composeRule.waitForIdle()
        assertTrue(clearedAll)
    }

    @Test
    fun `PollsAndDiscussionsScreen shows Admin-only Views button on Polls and opens ContentViewersDialog with Mobile and Time`() {
        val admin = GroupMember(userId = "u_admin", displayName = "Gopal Admin", role = AppRole.GROUP_ADMIN.name)
        val poll = QuestionPoll(
            id = "poll_1",
            questionTitle = "Solve BPSC Math Q",
            creatorUid = "u_admin",
            creatorName = "Gopal Admin",
            options = listOf("10", "20", "30", "40", "Not Attempted"),
            status = "OPEN"
        )
        val viewerProfile = UserProfile(
            id = "u_viewer",
            userId = "u_viewer",
            displayName = "Suman Kumari",
            whatsappNumber = "+919876543210"
        )
        val viewRec = ContentViewRecord(
            id = "POLL_poll_1_u_viewer",
            contentId = "poll_1",
            contentType = ContentViewRecord.CONTENT_TYPE_POLL,
            userId = "u_viewer",
            userName = "Suman Kumari",
            viewedAt = com.google.firebase.Timestamp(java.util.Date(1_760_000_000_000L))
        )

        composeRule.setContent {
            CoolingGlassTheme {
                PollsAndDiscussionsScreen(
                    currentMember = admin,
                    polls = listOf(poll),
                    userPollVotes = emptyMap(),
                    discussions = emptyList(),
                    contentViews = listOf(viewRec),
                    adminUserProfiles = listOf(viewerProfile),
                    onLoadVote = {},
                    onCreatePoll = { _, _, _, _, _ -> },
                    onCastVote = { _, _ -> },
                    onFetchPrivateKey = { _, _ -> },
                    onFinalizePoll = { _, _, _, _ -> },
                    onUploadSolution = { _, _, _ -> },
                    onPostComment = { _, _, _, _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("polls_discussions_list").performScrollToNode(hasTestTag("poll_viewers_btn_poll_1"))
        composeRule.onNodeWithTag("poll_viewers_btn_poll_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Total Unique Viewers: 1").assertExists()
        composeRule.onNodeWithText("Suman Kumari").assertExists()
        composeRule.onNodeWithText("User ID: u_viewer").assertExists()
        composeRule.onNodeWithText("Mobile: +919876543210").assertExists()
    }

    @Test
    fun `LeaderboardAndAnalyticsScreen shows exact Set Number - Rank heading and filters strictly by selected set and difficulty, plus IST clock badge`() {
        val originalTz = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
            val set1Easy = ExamTest(
                id = "t_s1_easy",
                title = "Common Paper Set 1",
                paperType = PaperType.COMMON_GS.name,
                difficulty = "EASY",
                questionCount = 40L,
                status = "PUBLISHED"
            )
            val set2Hard = ExamTest(
                id = "t_s2_hard",
                title = "Common Paper Set 2",
                paperType = PaperType.COMMON_GS.name,
                difficulty = "HARD",
                questionCount = 40L,
                status = "PUBLISHED"
            )
            val m1 = GroupMember(userId = "u1", displayName = "Kiran Devi")
            val m2 = GroupMember(userId = "u2", displayName = "Mohit Singh")
            val attempts = listOf(
                TestAttempt(
                    id = "att_s1", testId = "t_s1_easy", testTitle = set1Easy.title,
                    paperType = PaperType.COMMON_GS.name, userId = "u1",
                    correctCount = 29, totalScore = 29.0, meritMarks = 29.0, maxMarks = 40.0, totalMeritMarks = 40.0
                ),
                TestAttempt(
                    id = "att_s2", testId = "t_s2_hard", testTitle = set2Hard.title,
                    paperType = PaperType.COMMON_GS.name, userId = "u2",
                    correctCount = 35, totalScore = 35.0, meritMarks = 35.0, maxMarks = 40.0, totalMeritMarks = 40.0
                )
            )

            var activeTestId by mutableStateOf<String?>("t_s1_easy")
            val istClockSample = com.example.util.IstClock.formatEpochMillis(1_781_524_800_000L, "hh:mm:ss a 'IST'")

            composeRule.setContent {
                val ranked = ExamScoringEngine.computeCategoryRankings(
                    period = RankingPeriod.INDIVIDUAL_TEST,
                    category = RankCategory.COMMON,
                    members = listOf(m1, m2),
                    attempts = attempts,
                    tests = listOf(set1Easy, set2Hard),
                    selectedTestId = activeTestId
                )
                CoolingGlassTheme {
                    Column {
                        com.example.ui.glass.CoolingGlassTopBar(
                            title = "TRE 4.0",
                            clockText = istClockSample
                        )
                        LeaderboardAndAnalyticsScreen(
                            group = null,
                            currentMember = m1,
                            rankedList = ranked,
                            overallRanked = ranked,
                            selectedPeriod = RankingPeriod.INDIVIDUAL_TEST,
                            selectedCategory = RankCategory.COMMON,
                            selectedTestId = activeTestId,
                            tests = listOf(set1Easy, set2Hard),
                            allAttempts = attempts,
                            onSelectPeriod = {},
                            onSelectCategory = {},
                            onClearSetFilter = { activeTestId = null },
                            onBannerMessage = { _, _ -> }
                        )
                    }
                }
            }
            composeRule.waitForIdle()

            // Verify IST clock in top bar displays 05:30:00 PM IST even when device timezone is UTC
            composeRule.onNodeWithTag("live_header_clock").assertExists()
            composeRule.onNodeWithText("05:30:00 PM IST").assertExists()

            // Verify "Set 1 - Rank" heading card & EASY badge appear
            composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasTestTag("set_rank_heading_card"))
            composeRule.onNodeWithTag("set_rank_heading_text").assertTextEquals("Set 1 - Rank")
            composeRule.onNodeWithText("EASY").assertExists()

            // Switch to All Scores -> heading strictly states Set 1 - Rank (EASY) and m2 (Set 2 Hard) is unattempted on Set 1
            composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasText("All Scores"))
            composeRule.onNodeWithText("All Scores").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasText("All Users' Score Sheet — Set 1 - Rank (EASY)"))
            composeRule.onNodeWithText("All Users' Score Sheet — Set 1 - Rank (EASY)").assertExists()

            // Click "All Sets" clears the exact-set filter
            composeRule.onNodeWithTag("leaderboard_list").performScrollToNode(hasTestTag("clear_set_rank_filter_btn"))
            composeRule.onNodeWithTag("clear_set_rank_filter_btn").performClick()
            composeRule.waitForIdle()
            assertEquals(null, activeTestId)
            assertEquals(0, composeRule.onAllNodesWithTag("set_rank_heading_card").fetchSemanticsNodes().size)
        } finally {
            java.util.TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `TestsCatalogScreen displays all 7 Rank Categories aligned with Ranks and filters sets accurately`() {
        val member = GroupMember(userId = "u1", displayName = "Rohan Kumar", role = AppRole.ASPIRANT.name)
        val tests = listOf(
            ExamTest(id = "t_lang", title = "Language Paper Set 1", paperType = PaperType.LANGUAGE.name, questionCount = 30L, status = "PUBLISHED", approvedForAll = true),
            ExamTest(id = "t_com", title = "Common Paper Set 1", paperType = PaperType.COMMON_GS.name, questionCount = 40L, status = "PUBLISHED", approvedForAll = true),
            ExamTest(id = "t_comb", title = "Language + Common Paper Set 3", paperType = PaperType.COMBINED.name, questionCount = 70L, status = "PUBLISHED", approvedForAll = true),
            ExamTest(id = "t_math", title = "Mathematics Set 1", paperType = PaperType.CLASS_6_8_MATH.name, questionCount = 40L, status = "PUBLISHED", approvedForAll = true)
        )
        var selectedCat by mutableStateOf<RankCategory?>(null)

        composeRule.setContent {
            CoolingGlassTheme {
                TestsCatalogScreen(
                    tests = tests,
                    myAttempts = emptyList(),
                    currentMember = member,
                    selectedCategoryState = selectedCat,
                    onSelectCategoryState = { selectedCat = it },
                    onStartTest = {},
                    onViewResult = { _, _ -> },
                    onViewTestRanking = {},
                    onBannerMessage = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        // Verify all 7 category chips exist in the Test Section filter bar
        composeRule.onNodeWithTag("six_part_filter_row").assertExists()
        composeRule.onNodeWithTag("test_category_chip_all").assertExists()
        RankCategory.entries.forEach { cat ->
            composeRule.onNodeWithTag("test_category_chip_${cat.name.lowercase()}").assertExists()
        }

        // Click "Language + Common" category chip -> filters to ONLY Language + Common Paper Set 3
        composeRule.onNodeWithTag("test_category_chip_language_common").performClick()
        composeRule.waitForIdle()
        assertEquals(RankCategory.LANGUAGE_COMMON, selectedCat)
        composeRule.onNodeWithText("Language + Common Paper Set 3").assertExists()
        assertEquals(0, composeRule.onAllNodesWithText("Language Paper Set 1").fetchSemanticsNodes().size)
    }

    @Test
    fun `StudentLiveExamHubScreen shows only PUBLISHED exams, opens Waiting Room without leaking questions, and Secure LiveExamScreen shows Watermark & 5-Min Alert`() {
        val now = System.currentTimeMillis()
        val sourceTest = ExamTest(
            id = "src_comb_3",
            title = "Language + Common Paper Set 3",
            paperType = PaperType.COMBINED.name,
            questionCount = 2L,
            durationMinutes = 45L,
            questions = OfficialStarterBank.commonGs40Questions().take(2).map { it.toMap() },
            status = "PUBLISHED"
        )
        val publishedUpcoming = LiveExam(
            id = "live_pub_1",
            groupId = "g1",
            sourceSetId = "src_comb_3",
            sourceSetTitle = sourceTest.title,
            title = "BPSC TRE 4.0 All-India Live Mock 1",
            category = RankCategory.LANGUAGE_COMMON.name,
            startTimeEpochMillis = now + 600_000L, // Starts in 10 min
            endTimeEpochMillis = now + 3_300_000L,
            durationMinutes = 45L,
            registrationOpenEpochMillis = now - 600_000L,
            registrationCloseEpochMillis = now + 600_000L,
            resultPublishEpochMillis = now + 3_900_000L,
            totalQuestions = 70L,
            totalMarks = 70.0,
            secureModeEnabled = true,
            dynamicWatermark = true,
            focusLossDetection = true,
            navigationExitLock = true,
            status = "PUBLISHED"
        )
        val unpublishedDraft = publishedUpcoming.copy(
            id = "live_draft_2",
            title = "Hidden Draft Live Exam",
            status = "DRAFT"
        )

        var showSecureExamEngine by mutableStateOf(false)
        val secureExamState = ActiveExamState(
            test = sourceTest,
            questions = OfficialStarterBank.commonGs40Questions().take(2),
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            remainingSeconds = 240L, // 04:00 (< 5 min -> Final 5-Min Alert active!)
            totalDurationSeconds = 2700L,
            liveExamId = publishedUpcoming.id,
            isLiveExam = true,
            secureModeEnabled = true,
            dynamicWatermark = true,
            focusLossDetection = true,
            copyPasteBlock = true,
            navigationExitLock = true,
            warningLimit = 3,
            finalFiveMinAlertShown = true,
            showFinalFiveMinAlertBanner = true
        )

        composeRule.setContent {
            CoolingGlassTheme {
                if (!showSecureExamEngine) {
                    StudentLiveExamHubScreen(
                        liveExams = listOf(publishedUpcoming, unpublishedDraft),
                        liveExamSessions = emptyList(),
                        tests = listOf(sourceTest),
                        attempts = emptyList(),
                        currentUserId = "u_student_1",
                        isAdmin = false,
                        onRegisterLiveExam = { _, _ -> },
                        onStartLiveExam = {},
                        onViewLiveExamResult = { _, _ -> },
                        onOpenLiveExamRank = {}
                    )
                } else {
                    LiveExamScreen(
                        examState = secureExamState,
                        onSelectOption = { _, _ -> },
                        onClearResponse = {},
                        onToggleReview = {},
                        onNavigateQuestion = {},
                        onSubmit = {},
                        onAbort = {},
                        studentName = "Ananya Verma",
                        studentEmail = "ananya.verma@gmail.com",
                        studentId = "u_student_1"
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // 1. StudentLiveExamHubScreen shows PUBLISHED exam and strictly hides DRAFT exam
        composeRule.onNodeWithTag("live_exam_card_live_pub_1").assertExists()
        assertEquals(0, composeRule.onAllNodesWithText("Hidden Draft Live Exam").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("SECURE MODE").assertExists()

        // 2. Enter Waiting Room -> shows countdown and rules without exposing any questions
        composeRule.onNodeWithTag("waiting_room_btn_live_pub_1").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_exam_waiting_room_dialog", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("EXAM AUTOMATICALLY LAUNCHES IN", useUnmergedTree = true).assertExists()
        assertEquals(0, composeRule.onAllNodesWithTag("option_card_A").fetchSemanticsNodes().size)

        // 3. Switch to active Secure LiveExamScreen -> verify SECURE LIVE badge, Dynamic Watermark, Final 5-Min Alert Banner, and no Pause button
        showSecureExamEngine = true
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SECURE LIVE").assertExists()
        composeRule.onNodeWithTag("final_five_min_alert_banner").assertExists()
        composeRule.onNodeWithTag("secure_dynamic_watermark").assertExists()
        assertEquals(0, composeRule.onAllNodesWithTag("exam_pause_btn").fetchSemanticsNodes().size)
    }

    @Test
    fun `TestDraftStore strict pre-save JSON validation and auto-draft recovery protect Admin JSON editor data`() {
        val draftStore = TestDraftStore(context)

        // 1. Validate empty JSON is blocked
        val emptyRes = TestDraftStore.validateRawJsonStrict("   ")
        assertFalse(emptyRes.isValid)

        // 2. Validate malformed syntax reports error and blocks save
        val syntaxErrRes = TestDraftStore.validateRawJsonStrict("[\n  {\"question\": \"Valid Q\",\n  \"broken\": ]\n]")
        assertFalse(syntaxErrRes.isValid)
        assertNotNull(syntaxErrRes.errorMessage)

        // 3. Validate missing required fields (e.g. optionC) reports exact question & field name
        val missingFieldJson = """
            [
              {
                "number": 1,
                "question": "What is 2 + 2?",
                "optionA": "3",
                "optionB": "4",
                "optionD": "6",
                "correctAnswer": "B"
              }
            ]
        """.trimIndent()
        val missingRes = TestDraftStore.validateRawJsonStrict(missingFieldJson)
        assertFalse(missingRes.isValid)
        assertNotNull(missingRes.errorFieldName)

        // 4. Valid JSON passes strict check
        val validJson = """
            {
              "exam": "BPSC TRE 4.0",
              "set_title": "Language + Common Paper Set 3",
              "questions": [
                {
                  "number": 1,
                  "subject": "Mathematics",
                  "question": "2 + 2 = ?",
                  "optionA": "3",
                  "optionB": "4",
                  "optionC": "5",
                  "optionD": "6",
                  "correctAnswer": "B"
                }
              ]
            }
        """.trimIndent()
        val validRes = TestDraftStore.validateRawJsonStrict(validJson)
        assertTrue("Expected valid JSON: ${validRes.errorMessage}", validRes.isValid)
        assertEquals(1, validRes.parsedQuestions.size)

        // 5. Auto-Draft save & recovery round-trip
        val autoDraft = EditorAutoDraft(
            editorKey = "set_3_edit",
            rawJsonText = validJson,
            title = "Language + Common Paper Set 3",
            paperType = PaperType.COMBINED,
            difficulty = "MODERATE",
            durationMinutes = 45,
            questions = OfficialStarterBank.commonGs40Questions().take(2)
        )
        draftStore.saveAutoDraft(autoDraft)
        val loaded = draftStore.loadAutoDraft("set_3_edit")
        assertNotNull(loaded)
        assertEquals("Language + Common Paper Set 3", loaded!!.title)
        assertEquals(2, loaded.questions.size)
        assertTrue(loaded.rawJsonText.contains("BPSC TRE 4.0"))

        draftStore.clearAutoDraft("set_3_edit")
        assertNull(draftStore.loadAutoDraft("set_3_edit"))
    }

    @Test
    fun `Set-Wise View Rank opens exact set leaderboard with all 150 participants, zero other-set leakage, full metrics, and preserves Set Session state on Back`() {
        val set1 = ExamTest(
            id = "set_1_common",
            groupId = "g1",
            title = "Common Paper Set 1",
            paperType = PaperType.COMMON_GS.name,
            difficulty = "MODERATE",
            questionCount = 40L,
            durationMinutes = 30L,
            setNumber = 1,
            status = "PUBLISHED"
        )
        val set2 = ExamTest(
            id = "set_2_common",
            groupId = "g1",
            title = "Common Paper Set 2",
            paperType = PaperType.COMMON_GS.name,
            difficulty = "HARD",
            questionCount = 40L,
            durationMinutes = 30L,
            setNumber = 2,
            status = "PUBLISHED"
        )
        val set3 = ExamTest(
            id = "set_3_math",
            groupId = "g1",
            title = "Mathematics Set 3",
            paperType = PaperType.CLASS_6_8_MATH.name,
            difficulty = "EASY",
            questionCount = 40L,
            durationMinutes = 30L,
            setNumber = 3,
            status = "PUBLISHED"
        )

        val baseTimeMs = 1764060000000L // Fixed timestamp in IST
        // Create 150 submitted attempts strictly for Set 1 (highest scorer = user_set1_001, lowest = user_set1_150)
        val set1Attempts = (1..150).map { i ->
            val score = (40.0 - (i - 1) * 0.25).coerceAtLeast(1.0)
            val correct = score.toLong().coerceIn(1L, 40L)
            val wrong = ((40L - correct) / 2L).coerceAtLeast(0L)
            val unatt = (40L - correct - wrong).coerceAtLeast(0L)
            val subDate = java.util.Date(baseTimeMs + i * 60_000L)
            TestAttempt(
                id = "${set1.id}_user_set1_$i",
                groupId = "g1",
                testId = set1.id,
                testTitle = set1.title,
                paperType = set1.paperType,
                userId = "user_set1_$i",
                aspirantName = "Set1 Aspirant $i",
                correctCount = correct,
                incorrectCount = wrong,
                notAttemptedCount = unatt,
                totalScore = score,
                maxMarks = 40.0,
                percentage = (score / 40.0) * 100.0,
                timeSpentSeconds = 600L + i * 5L,
                meritMarks = score,
                totalMeritMarks = 40.0,
                meritCorrectCount = correct,
                meritIncorrectCount = wrong,
                meritUnattemptedCount = unatt,
                completionStatus = "COMPLETED",
                submittedAt = com.google.firebase.Timestamp(subDate)
            )
        }

        // Create 25 attempts for Set 2 and 20 attempts for Set 3 that MUST NEVER appear in Set 1's rank
        val set2Attempts = (1..25).map { i ->
            TestAttempt(
                id = "${set2.id}_user_set2_$i",
                groupId = "g1",
                testId = set2.id,
                testTitle = set2.title,
                paperType = set2.paperType,
                userId = "user_set2_$i",
                aspirantName = "OtherSet2 User $i",
                correctCount = 40L,
                totalScore = 40.0,
                maxMarks = 40.0,
                meritMarks = 40.0,
                totalMeritMarks = 40.0,
                completionStatus = "COMPLETED",
                submittedAt = com.google.firebase.Timestamp(java.util.Date(baseTimeMs))
            )
        }
        val set3Attempts = (1..20).map { i ->
            TestAttempt(
                id = "${set3.id}_user_set3_$i",
                groupId = "g1",
                testId = set3.id,
                testTitle = set3.title,
                paperType = set3.paperType,
                userId = "user_set3_$i",
                aspirantName = "OtherSet3 User $i",
                correctCount = 40L,
                totalScore = 40.0,
                maxMarks = 40.0,
                meritMarks = 40.0,
                totalMeritMarks = 40.0,
                completionStatus = "COMPLETED",
                submittedAt = com.google.firebase.Timestamp(java.util.Date(baseTimeMs))
            )
        }

        val combinedPool = set1Attempts + set2Attempts + set3Attempts
        // Intentionally provide only 10 GroupMembers to prove users missing from groupMembers are STILL ranked!
        val partialMembers = (1..10).map { i ->
            GroupMember(userId = "user_set1_$i", displayName = "Set1 Aspirant $i", role = AppRole.ASPIRANT.name)
        }

        // 1. Engine Verification: EXACTLY 150 ranked users for Set 1, ZERO from Set 2 / Set 3
        val set1Rankings = ExamScoringEngine.computeSetWiseRankings(
            test = set1,
            attempts = combinedPool,
            members = partialMembers
        )
        assertEquals("Must include ALL 150 users who attempted Set 1 with zero cap", 150, set1Rankings.size)
        assertTrue("Must NEVER include any attempt from Set 2 or Set 3", set1Rankings.all { it.testId == set1.id })
        assertTrue(set1Rankings.none { it.userId.startsWith("user_set2_") || it.userId.startsWith("user_set3_") })
        assertEquals("user_set1_1", set1Rankings.first().userId)
        assertEquals(1, set1Rankings.first().rank)
        assertEquals("user_set1_150", set1Rankings.last().userId)
        assertTrue("Must be sorted highest score first", set1Rankings.zipWithNext().all { (a, b) -> a.score >= b.score })

        // 2. End-to-End UI Navigation Verification: Set Session -> Click View Rank -> Dedicated Set-Wise Rank Screen -> Back restores Set Session
        var activeSetRank by mutableStateOf<ExamTest?>(null)
        var selectedCatalogCat by mutableStateOf<RankCategory?>(RankCategory.COMMON)
        val member = GroupMember(userId = "user_set1_42", displayName = "Set1 Aspirant 42", role = AppRole.ASPIRANT.name)

        composeRule.setContent {
            CoolingGlassTheme {
                val target = activeSetRank
                if (target != null) {
                    SetWiseRankScreen(
                        test = target,
                        entries = ExamScoringEngine.computeSetWiseRankings(target, combinedPool, partialMembers),
                        currentUserId = member.userId,
                        onBack = { activeSetRank = null }
                    )
                } else {
                    TestsCatalogScreen(
                        tests = listOf(set1, set2, set3),
                        myAttempts = emptyList(),
                        currentMember = member,
                        selectedCategoryState = selectedCatalogCat,
                        onSelectCategoryState = { selectedCatalogCat = it },
                        onStartTest = {},
                        onViewResult = { _, _ -> },
                        onViewTestRanking = { clickedSet -> activeSetRank = clickedSet },
                        onBannerMessage = { _, _ -> }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Click "View Rank" button on Set 1 inside Set Session (Set 2 is now above Set 1 due to newest-first ordering)
        composeRule.onNodeWithTag("pending_tests_list")
            .performScrollToNode(hasTestTag("test_set_card_set_1_common"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("view_ranking_eye_set_1_common").performClick()
        composeRule.waitForIdle()

        // Verify Dedicated Set-Wise Rank Screen opened (NOT Main Rank Card!)
        composeRule.onNodeWithTag("set_wise_rank_screen").assertExists()
        assertEquals(0, composeRule.onAllNodesWithTag("rank_category_selector_card").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("set_wise_rank_title").assertTextEquals("Common Paper Set 1")
        composeRule.onNodeWithText("150 Users").assertExists()
        composeRule.onNodeWithText("Showing all 150 participants (Ranked highest score first)").assertExists()

        // Verify Rank #1 user and metrics are visible
        composeRule.onNodeWithTag("set_wise_rank_list")
            .performScrollToNode(hasTestTag("set_rank_row_user_set1_1"))
        composeRule.onNodeWithTag("set_rank_row_user_set1_1").assertExists()
        composeRule.onNodeWithText("Set1 Aspirant 1").assertExists()

        // Scroll all the way to the 150th participant and verify they exist (proving NO 10/20/50/100 cap!)
        composeRule.onNodeWithTag("set_wise_rank_list")
            .performScrollToNode(hasTestTag("set_rank_row_user_set1_150"))
        composeRule.onNodeWithTag("set_rank_row_user_set1_150").assertExists()
        composeRule.onNodeWithText("Set1 Aspirant 150").assertExists()

        // Verify ZERO users from Set 2 or Set 3 appear anywhere
        assertEquals(0, composeRule.onAllNodesWithText("OtherSet2 User 1").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("OtherSet3 User 1").fetchSemanticsNodes().size)

        // Press Back -> returns directly to Set Session preserving RankCategory.COMMON state
        composeRule.onNodeWithTag("set_wise_rank_back_btn").performClick()
        composeRule.waitForIdle()
        assertNull(activeSetRank)
        assertEquals(RankCategory.COMMON, selectedCatalogCat)
        composeRule.onNodeWithTag("pending_tests_list")
            .performScrollToNode(hasTestTag("test_set_card_set_1_common"))
        composeRule.onNodeWithTag("view_ranking_eye_set_1_common").assertExists()
    }

    @Test
    fun `DashboardScreen shows Aaj Ka Target clickable photo banner, rolls over yesterday missed task until completed, and lets Admin schedule next-day banner`() {
        val fixedNowMillis = 1_781_524_800_000L // 2026-06-15 17:30 IST
        val todayIst = com.example.util.IstClock.todayIstKey(fixedNowMillis)
        val yesterdayIst = com.example.util.IstClock.offsetIstKey(-1, fixedNowMillis)
        val tomorrowIst = com.example.util.IstClock.offsetIstKey(1, fixedNowMillis)

        val setToday = ExamTest(
            id = "test_today_target",
            title = "TRE 4.0 Common GS Set 7",
            paperType = PaperType.COMMON_GS.name,
            questionCount = 40L,
            durationMinutes = 30L,
            status = "PUBLISHED"
        )
        val setYesterday = ExamTest(
            id = "test_yesterday_target",
            title = "TRE 4.0 Language Paper Set 6",
            paperType = PaperType.LANGUAGE.name,
            questionCount = 30L,
            durationMinutes = 15L,
            status = "PUBLISHED"
        )
        val setTomorrow = ExamTest(
            id = "test_tomorrow_target",
            title = "TRE 4.0 Full Math + Science Set 8",
            paperType = PaperType.COMBINED.name,
            questionCount = 150L,
            durationMinutes = 120L,
            status = "PUBLISHED"
        )

        val targets = listOf(
            DailyTargetTask(
                id = "dt_today",
                groupId = "g1",
                testId = setToday.id,
                testTitle = setToday.title,
                targetDateIst = todayIst,
                bannerTitle = "आज का टारगेट • मिशन सेट 7",
                bannerSubtitle = "आज रात 11:59 बजे तक पूरा करें",
                active = true
            ),
            DailyTargetTask(
                id = "dt_yesterday",
                groupId = "g1",
                testId = setYesterday.id,
                testTitle = setYesterday.title,
                targetDateIst = yesterdayIst,
                bannerTitle = "कल का टारगेट सेट 6",
                active = true
            ),
            DailyTargetTask(
                id = "dt_tomorrow",
                groupId = "g1",
                testId = setTomorrow.id,
                testTitle = setTomorrow.title,
                targetDateIst = tomorrowIst,
                bannerTitle = "अगले दिन का महा-अभ्यास 8",
                active = true
            )
        )

        var currentMember by mutableStateOf(
            GroupMember(userId = "u_student", displayName = "Amit Kumar", role = AppRole.ASPIRANT.name)
        )
        var attemptsState by mutableStateOf<List<TestAttempt>>(emptyList())
        var openedTargetTest by mutableStateOf<ExamTest?>(null)
        var savedDateIst by mutableStateOf<String?>(null)
        var savedTestId by mutableStateOf<String?>(null)

        composeRule.setContent {
            CoolingGlassTheme {
                DashboardScreen(
                    member = currentMember,
                    group = null,
                    overallRanking = emptyList(),
                    currentAffairs = emptyList(),
                    tests = listOf(setToday, setYesterday, setTomorrow),
                    unreadNotificationsCount = 0,
                    dailyTargets = targets,
                    myAttempts = attemptsState,
                    nowMillis = fixedNowMillis,
                    onOpenTargetTest = { openedTargetTest = it },
                    onSaveDailyTarget = { _, test, dateIst, _, _, _ ->
                        savedTestId = test.id
                        savedDateIst = dateIst
                    },
                    onNavigate = {},
                    onPublishCurrentAffair = { _, _, _, _, _, _ -> },
                    onDeleteCurrentAffair = {}
                )
            }
        }
        composeRule.waitForIdle()

        // 1. Regular User sees Today's Target photo banner AND Yesterday's Missed Task banner, but NOT Tomorrow's pre-scheduled banner
        composeRule.onNodeWithTag("daily_target_today_banner_dt_today").assertExists()
        composeRule.onNodeWithText("आज का टारगेट • मिशन सेट 7").assertExists()
        composeRule.onNodeWithText("TRE 4.0 Common GS Set 7").assertExists()

        composeRule.onNodeWithTag("daily_target_yesterday_missed_dt_yesterday").assertExists()
        composeRule.onNodeWithText("कल का टारगेट सेट 6").assertExists()
        assertEquals(0, composeRule.onAllNodesWithText("अगले दिन का महा-अभ्यास 8").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("admin_add_daily_target_btn").fetchSemanticsNodes().size)

        // 2. Clicking Today's Photo Banner opens the exact target test set
        composeRule.onNodeWithTag("daily_target_today_banner_dt_today").performClick()
        composeRule.waitForIdle()
        assertEquals("test_today_target", openedTargetTest?.id)

        // 3. Gently scroll Yesterday's Missed Task banner into center of viewport and click it -> opens yesterday's missed set directly
        composeRule.onNodeWithTag("dashboard_list").performTouchInput {
            swipeUp(startY = height * 0.72f, endY = height * 0.30f)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("daily_target_yesterday_missed_dt_yesterday").performClick()
        composeRule.waitForIdle()
        assertEquals("test_yesterday_target", openedTargetTest?.id)

        // 4. Once user submits Yesterday's set, the missed-task banner automatically disappears!
        attemptsState = listOf(
            TestAttempt(id = "att_yest", testId = setYesterday.id, userId = "u_student")
        )
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("daily_target_yesterday_missed_dt_yesterday").fetchSemanticsNodes().size)

        // Verify "Group & Controls" section is completely removed from Home screen (covered by bottom bar & drawer)
        assertEquals(0, composeRule.onAllNodesWithText("Group & Controls").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("dash_group_info").fetchSemanticsNodes().size)

        // 5. Switch to Admin -> scroll back to header, verify "+ टारगेट सेट करें" button, upcoming preview, and strictly manual target selection
        currentMember = GroupMember(userId = "u_admin", displayName = "Gopal Admin", role = AppRole.GROUP_ADMIN.name)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_list").performTouchInput {
            swipeDown(startY = height * 0.30f, endY = height * 0.72f)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("admin_add_daily_target_btn").assertExists()
        composeRule.onNodeWithTag("admin_upcoming_targets_card").assertExists()
        composeRule.onNodeWithTag("admin_add_daily_target_btn").performClick()
        composeRule.waitForIdle()

        // No set is auto-selected -> Publish button MUST be disabled until Admin manually picks a target set
        composeRule.onNodeWithTag("save_daily_target_btn").performScrollTo().assertIsNotEnabled()

        // Admin manually chooses Tomorrow date and manually picks the target set -> Publish becomes enabled
        composeRule.onNodeWithTag("target_date_tomorrow_chip").performScrollTo().performClick()
        composeRule.onNodeWithTag("select_target_test_test_tomorrow_target").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_daily_target_btn").performScrollTo().assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals(tomorrowIst, savedDateIst)
        assertEquals("test_tomorrow_target", savedTestId)
    }

    @Test
    fun `LiveExamScreen renders real-time HH_MM_SS and MM_SS countdown with depleting bar, 5-min alert on normal tests, and critical 60-sec urgency banner`() {
        // 1. Unit verify HH:MM:SS & MM:SS formatting and 4 urgency tiers
        assertEquals("02:15:00", formatExamCountdown(8100L))
        assertEquals("29:59", formatExamCountdown(1799L))
        assertEquals("00:45", formatExamCountdown(45L))
        assertEquals("00:00", formatExamCountdown(0L))

        assertEquals(ExamTimerUrgency.CALM, resolveExamTimerUrgency(1500L, 1800L))
        assertEquals(ExamTimerUrgency.WARNING, resolveExamTimerUrgency(400L, 1800L))
        assertEquals(ExamTimerUrgency.URGENT_5_MIN, resolveExamTimerUrgency(240L, 1800L))
        assertEquals(ExamTimerUrgency.CRITICAL_1_MIN, resolveExamTimerUrgency(45L, 1800L))

        val normalTest = ExamTest(
            id = "normal_timed_1",
            title = "TRE 4.0 Full Length Mock 1",
            paperType = PaperType.COMMON_GS.name,
            questionCount = 2L,
            durationMinutes = 135L,
            status = "PUBLISHED"
        )
        val sampleQuestions = OfficialStarterBank.commonGs40Questions().take(2)
        var examState by mutableStateOf(
            ActiveExamState(
                test = normalTest,
                questions = sampleQuestions,
                currentIndex = 0,
                remainingSeconds = 8100L, // 02:15:00
                totalDurationSeconds = 8100L,
                isLiveExam = false
            )
        )

        composeRule.setContent {
            CoolingGlassTheme {
                LiveExamScreen(
                    examState = examState,
                    onSelectOption = { _, _ -> },
                    onClearResponse = {},
                    onToggleReview = {},
                    onNavigateQuestion = {},
                    onPause = {},
                    onSubmit = {},
                    onAbort = {}
                )
            }
        }
        composeRule.waitForIdle()

        // 2. Full-length exam displays HH:MM:SS tabular countdown and depleting progress bar
        composeRule.onNodeWithTag("live_exam_countdown_pill").assertExists()
        composeRule.onNodeWithTag("live_exam_countdown_text").assertTextEquals("02:15:00")
        composeRule.onNodeWithTag("exam_countdown_progress_bar").assertExists()
        assertEquals(0, composeRule.onAllNodesWithTag("final_five_min_alert_banner").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("critical_one_min_alert_banner").fetchSemanticsNodes().size)

        // 3. Tick down to 4 minutes remaining (240s) -> Final 5-Minute alert appears even on normal tests
        examState = examState.copy(remainingSeconds = 240L, showFinalFiveMinAlertBanner = true)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_exam_countdown_text").assertTextEquals("04:00")
        composeRule.onNodeWithTag("final_five_min_alert_banner").assertExists()

        // 4. Tick down to 45 seconds remaining -> escalates to Critical Final 60-Second pulsing urgency banner
        examState = examState.copy(remainingSeconds = 45L, showFinalFiveMinAlertBanner = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_exam_countdown_text").assertTextEquals("00:45")
        composeRule.onNodeWithTag("critical_one_min_alert_banner").assertExists()
        composeRule.onNodeWithText("FINAL 45 SEC LEFT • HURRY UP! (00:45)").assertExists()
    }

    @Test
    fun `Profile language ENGLISH renders entire application interface strictly in English without Hindi text`() {
        val nowMs = 1_781_500_000_000L
        val todayIst = com.example.util.IstClock.todayIstKey(nowMs)
        val adminMember = GroupMember(
            userId = "u_admin_en",
            displayName = "Gopal Gupta",
            role = AppRole.SUPER_ADMIN.name
        )
        val group = com.example.data.model.ExamGroup(id = "g1", name = "TRE 4.0 Official", inviteCode = "TRE400")
        val targetTest = ExamTest(
            id = "test_en_1",
            title = "Common Paper Set 1",
            paperType = PaperType.COMMON_GS.name,
            questionCount = 40L,
            durationMinutes = 30L,
            status = "PUBLISHED"
        )
        val todayTarget = DailyTargetTask(
            id = "dt_en_1",
            groupId = "g1",
            testId = targetTest.id,
            testTitle = targetTest.title,
            targetDateIst = todayIst,
            bannerTitle = "आज का टारगेट • Aaj Ka Target"
        )

        var screenMode by mutableStateOf("DASHBOARD")

        composeRule.setContent {
            CoolingGlassTheme(languagePref = com.example.data.model.LanguagePref.ENGLISH) {
                when (screenMode) {
                    "DASHBOARD" -> DashboardScreen(
                        member = adminMember,
                        group = group,
                        overallRanking = emptyList(),
                        currentAffairs = emptyList(),
                        tests = listOf(targetTest),
                        unreadNotificationsCount = 0,
                        dailyTargets = listOf(todayTarget),
                        myAttempts = emptyList(),
                        nowMillis = nowMs,
                        onNavigate = {},
                        onPublishStarterSets = {},
                        onPublishCurrentAffair = { _, _, _, _, _, _ -> },
                        onDeleteCurrentAffair = {}
                    )
                    "PRE_TEST" -> com.example.ui.screens.PreTestInstructionScreen(
                        test = targetTest.copy(paperType = PaperType.COMBINED.name),
                        onBack = {},
                        onStartExam = {}
                    )
                    "POLLS" -> PollsAndDiscussionsScreen(
                        currentMember = adminMember,
                        polls = emptyList(),
                        userPollVotes = emptyMap(),
                        discussions = emptyList(),
                        onLoadVote = {},
                        onCreatePoll = { _, _, _, _, _ -> },
                        onCastVote = { _, _ -> },
                        onFetchPrivateKey = { _, _ -> },
                        onFinalizePoll = { _, _, _, _ -> },
                        onUploadSolution = { _, _, _ -> },
                        onPostComment = { _, _, _, _, _, _ -> }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // 1. Dashboard & Daily Target Banner render strictly in English
        composeRule.onAllNodesWithText("Today's Target", substring = false)[0].assertExists()
        composeRule.onNodeWithText("+ Set Target").assertExists()
        composeRule.onNodeWithText("🎯 TODAY'S TARGET").assertExists()
        composeRule.onNodeWithText("Start Set").assertExists()
        composeRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasText("Exam Hall • 5-Option MCQ (-1/3 Negative, Opt E Not Attempted = 0)"))
        composeRule.onNodeWithText("Exam Hall • 5-Option MCQ (-1/3 Negative, Opt E Not Attempted = 0)").assertExists()
        assertEquals(0, composeRule.onAllNodesWithText("आज का टारगेट", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("परीक्षा कक्ष", substring = true).fetchSemanticsNodes().size)

        // 2. Pre-Test Instructions screen renders strictly in English
        screenMode = "PRE_TEST"
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Strict Language Qualifying Rule").assertExists()
        assertEquals(0, composeRule.onAllNodesWithText("अनिवार्य भाषा नियम", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("हिन्दी", substring = true).fetchSemanticsNodes().size)

        // 3. Polls screen renders strictly in English
        screenMode = "POLLS"
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("CREATE POLL")[0].assertExists()
        assertEquals(0, composeRule.onAllNodesWithText("सवाल पूछें", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun `Quarter-size 2x2 grid displays 4 Today Targets and 4 Current Affairs banners simultaneously and opens Current Affairs banner in full-size viewer on tap`() {
        val nowMillis = System.currentTimeMillis()
        val todayIst = com.example.util.IstClock.todayIstKey(nowMillis)
        val tests = (1..4).map { idx ->
            ExamTest(
                id = "test_t_$idx",
                title = "TRE 4.0 Target Set $idx",
                paperType = PaperType.COMMON_GS.name,
                setType = com.example.data.model.SetType.PART_1_COMMON.name,
                questionCount = 40L,
                durationMinutes = 30L,
                status = "PUBLISHED"
            )
        }
        val targets = (1..4).map { idx ->
            DailyTargetTask(
                id = "dt_$idx",
                testId = "test_t_$idx",
                testTitle = "TRE 4.0 Target Set $idx",
                targetDateIst = todayIst,
                bannerTitle = "Target Mission $idx",
                active = true
            )
        }
        val affairs = (1..4).map { idx ->
            com.example.data.model.CurrentAffairItem(
                id = "ca_$idx",
                title = "Bihar Current Update #$idx",
                summary = "Verified PIB Summary #$idx for TRE 4.0 exam.",
                dateLabel = "26 Nov",
                source = "PIB Official",
                publishedByName = "Gopal Admin"
            )
        }
        val member = GroupMember(
            userId = "u_aspirant_1",
            displayName = "Ravi Kumar",
            role = AppRole.ASPIRANT.name
        )

        composeRule.setContent {
            CoolingGlassTheme(darkTheme = false) {
                DashboardScreen(
                    group = com.example.data.model.ExamGroup(id = "grp_1", name = "TRE 4.0 Squad"),
                    member = member,
                    overallRanking = emptyList(),
                    currentAffairs = affairs,
                    tests = tests,
                    unreadNotificationsCount = 0,
                    dailyTargets = targets,
                    myAttempts = emptyList(),
                    nowMillis = nowMillis,
                    onNavigate = {},
                    onPublishCurrentAffair = { _, _, _, _, _, _ -> },
                    onDeleteCurrentAffair = {}
                )
            }
        }
        composeRule.waitForIdle()

        // 1. All 4 Today's Target banners render together in the 1/4-size 2x2 grid
        composeRule.onNodeWithTag("daily_target_compact_grid").assertExists()
        (1..4).forEach { idx ->
            composeRule.onNodeWithTag("daily_target_today_banner_dt_$idx").assertExists()
        }

        // 2. Scroll slightly to Current Affairs 2x2 compact grid and verify all 4 banners render simultaneously
        composeRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasTestTag("current_affairs_compact_pager"))
        composeRule.waitForIdle()
        (1..4).forEach { idx ->
            composeRule.onNodeWithTag("banner_aspect_box_ca_$idx").assertExists()
        }

        // 3. Tapping Current Affairs banner #2 opens the full-size banner viewer with its full details
        composeRule.onNodeWithTag("banner_aspect_box_ca_2").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("full_size_banner_viewer").assertExists()
        composeRule.onNodeWithText("Verified PIB Summary #2 for TRE 4.0 exam.").assertExists()

        // 4. Closing the full-size viewer returns cleanly to the compact grid
        composeRule.onNodeWithTag("full_size_banner_close_btn").performClick()
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("full_size_banner_viewer").fetchSemanticsNodes().size)
    }

    @Test
    fun `SetWiseRankScreen ranker tap opens separate Subject-Wise Marks, Right, Wrong, and Not Attempted breakdown`() {
        val questions = listOf(
            ExamQuestion(number = 1, subject = "Mathematics", section = ExamQuestion.SECTION_COMMON, text = "M1", correctOption = "A"),
            ExamQuestion(number = 2, subject = "Mathematics", section = ExamQuestion.SECTION_COMMON, text = "M2", correctOption = "B"),
            ExamQuestion(number = 3, subject = "Mathematics", section = ExamQuestion.SECTION_COMMON, text = "M3", correctOption = "C"),
            ExamQuestion(number = 4, subject = "History", section = ExamQuestion.SECTION_COMMON, text = "H1", correctOption = "D"),
            ExamQuestion(number = 5, subject = "History", section = ExamQuestion.SECTION_COMMON, text = "H2", correctOption = "A"),
            ExamQuestion(number = 6, subject = "Science", section = ExamQuestion.SECTION_COMMON, text = "S1", correctOption = "B"),
            ExamQuestion(number = 7, subject = "Science", section = ExamQuestion.SECTION_COMMON, text = "S2", correctOption = "C")
        )
        val test = ExamTest(
            id = "set_subj_breakdown_1",
            title = "TRE 4.0 Common GS Set 12",
            paperType = PaperType.COMMON_GS.name,
            setType = com.example.data.model.SetType.PART_1_COMMON.name,
            questionCount = questions.size.toLong(),
            durationMinutes = 30L,
            positiveMarks = 1.0,
            negativeMarkNumerator = 1L,
            negativeMarkDenominator = 3L,
            optionEMode = com.example.data.model.OptionEMode.NOT_ATTEMPTED.name,
            questions = questions.map { it.toMap() }
        )
        // Mathematics: 2 Right (0:A, 1:B), 1 Wrong (2:A) -> 2 - 1/3 = 1.67
        // History: 1 Right (3:D), 0 Wrong, 1 Not Attempted via Opt E (4:E) -> 1.00
        // Science: 0 Right, 1 Wrong (5:A), 1 Not Attempted via Blank (6 missing) -> -0.33
        val answers = mapOf(
            "0" to "A",
            "1" to "B",
            "2" to "A",
            "3" to "D",
            "4" to "E",
            "5" to "A"
        )
        val eval = ExamScoringEngine.evaluate(
            test = test,
            questions = questions,
            selectedAnswers = answers.mapKeys { it.key.toInt() }
        )
        val attempt = TestAttempt(
            id = "att_ranker_1",
            testId = test.id,
            userId = "u_ranker_1",
            aspirantName = "Priya Kumari",
            totalScore = eval.totalScore,
            maxMarks = eval.maxMarks,
            meritMarks = eval.meritMarks,
            totalMeritMarks = eval.totalMeritMarks,
            correctCount = eval.correctCount.toLong(),
            incorrectCount = eval.incorrectCount.toLong(),
            dontKnowCount = eval.dontKnowCount.toLong(),
            notAttemptedCount = eval.notAttemptedCount.toLong(),
            meritCorrectCount = eval.meritCorrectCount.toLong(),
            meritIncorrectCount = eval.meritIncorrectCount.toLong(),
            meritUnattemptedCount = (eval.meritDontKnowCount + eval.meritNotAttemptedCount).toLong(),
            answers = answers,
            subjectScores = eval.subjectScores,
            timeSpentSeconds = 640L
        )
        val entries = ExamScoringEngine.computeSetWiseRankings(
            test = test,
            attempts = listOf(attempt),
            members = listOf(GroupMember(userId = "u_ranker_1", displayName = "Priya Kumari"))
        )
        assertEquals(1, entries.size)
        val rankerEntry = entries.first()

        // 1. Unit verify exact separate subject-wise Number/Marks, Right, Wrong, Not Attempted
        val stats = ExamScoringEngine.computeSetRankerSubjectBreakdown(test, rankerEntry)
        assertEquals(3, stats.size)
        val mathStat = stats.first { it.subject == "Mathematics" }
        assertEquals(2, mathStat.rightCount)
        assertEquals(1, mathStat.wrongCount)
        assertEquals(0, mathStat.notAttemptedCount)
        assertEquals(2.0 - 1.0 / 3.0, mathStat.marks, 0.001)

        val histStat = stats.first { it.subject == "History" }
        assertEquals(1, histStat.rightCount)
        assertEquals(0, histStat.wrongCount)
        assertEquals(1, histStat.notAttemptedCount)
        assertEquals(1.0, histStat.marks, 0.001)

        val sciStat = stats.first { it.subject == "Science" }
        assertEquals(0, sciStat.rightCount)
        assertEquals(1, sciStat.wrongCount)
        assertEquals(1, sciStat.notAttemptedCount)
        // Default unattemptedPenalty=true: 1 wrong (-1/3) + 1 blank (-1/3) = -2/3 = -0.67
        assertEquals(-2.0 / 3.0, sciStat.marks, 0.001)
        assertEquals(eval.subjectScores["Science"] ?: 0.0, sciStat.marks, 0.001)

        // 2. UI verify tapping the ranker row in SetWiseRankScreen opens the subject breakdown sheet
        composeRule.setContent {
            CoolingGlassTheme(darkTheme = false) {
                SetWiseRankScreen(
                    test = test,
                    entries = entries,
                    currentUserId = "u_other",
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("set_rank_row_u_ranker_1").assertExists().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("set_ranker_subject_breakdown_sheet").assertExists()
        composeRule.onNodeWithTag("subject_breakdown_row_Mathematics").assertExists()
        composeRule.onNodeWithTag("subject_breakdown_row_History").assertExists()
        composeRule.onNodeWithTag("subject_breakdown_row_Science").assertExists()
        composeRule.onNodeWithText("1.67 / 3").assertExists()
        composeRule.onNodeWithText("1.00 / 2").assertExists()
        composeRule.onNodeWithText("-0.67 / 2").assertExists()
    }

    @Test
    fun `Bottom bar Completed tab shows only completed sets with all features, Tests tab shows only pending and paused sets, and newest Admin set always appears at top`() {
        val nowSec = System.currentTimeMillis() / 1000L
        val olderPending = ExamTest(
            id = "t_old_pending",
            title = "TRE 4.0 Common Set 1",
            paperType = PaperType.COMMON_GS.name,
            questionCount = 40L,
            durationMinutes = 30L,
            status = "PUBLISHED",
            createdAt = com.google.firebase.Timestamp(nowSec - 7200L, 0)
        )
        val pausedTest = ExamTest(
            id = "t_paused_mid",
            title = "TRE 4.0 Science Set 2",
            paperType = PaperType.CLASS_6_8_SCIENCE.name,
            questionCount = 40L,
            durationMinutes = 30L,
            status = "PUBLISHED",
            createdAt = com.google.firebase.Timestamp(nowSec - 3600L, 0)
        )
        val newestAdminAdded = ExamTest(
            id = "t_newest_admin",
            title = "TRE 4.0 Brand New Admin Set 9",
            paperType = PaperType.LANGUAGE.name,
            questionCount = 30L,
            durationMinutes = 20L,
            status = "PUBLISHED",
            createdAt = com.google.firebase.Timestamp(nowSec - 10L, 0)
        )
        val completedOlder = ExamTest(
            id = "t_done_old",
            title = "TRE 4.0 Completed Set 4",
            paperType = PaperType.COMMON_GS.name,
            questionCount = 40L,
            durationMinutes = 30L,
            status = "PUBLISHED",
            createdAt = com.google.firebase.Timestamp(nowSec - 5000L, 0)
        )
        val completedNewer = ExamTest(
            id = "t_done_new",
            title = "TRE 4.0 Completed Set 8",
            paperType = PaperType.CLASS_6_8_MATH.name,
            questionCount = 40L,
            durationMinutes = 30L,
            status = "PUBLISHED",
            createdAt = com.google.firebase.Timestamp(nowSec - 100L, 0)
        )

        // Deliberately pass scrambled oldest-first input to verify newest-first sorting
        val allTests = listOf(olderPending, completedOlder, pausedTest, completedNewer, newestAdminAdded)
        val sortedUnit = allTests.sortedWith(ExamTest.newestFirstComparator)
        assertEquals("t_newest_admin", sortedUnit.first().id)

        val myAttempts = listOf(
            TestAttempt(id = "att_done_1", testId = completedOlder.id, userId = "u1", totalScore = 25.0, maxMarks = 40.0),
            TestAttempt(id = "att_done_2", testId = completedNewer.id, userId = "u1", totalScore = 34.0, maxMarks = 40.0)
        )
        val pausedMap = mapOf(
            pausedTest.id to PersistedExamSession(
                userId = "u1",
                groupId = "g1",
                testId = pausedTest.id,
                endTimeEpochMillis = System.currentTimeMillis() + 900_000L,
                totalDurationSeconds = 1800L,
                currentIndex = 5,
                selectedAnswers = mapOf(0 to "A"),
                markedForReview = emptySet(),
                isPaused = true,
                pausedRemainingSeconds = 900L
            )
        )
        val member = GroupMember(userId = "u1", displayName = "Amit Kumar", role = AppRole.ASPIRANT.name)
        var selectedNav by mutableStateOf("tests")
        var clickedRankTestId by mutableStateOf<String?>(null)
        var clickedResultTestId by mutableStateOf<String?>(null)
        val pendingListState = LazyListState()
        val completedListState = LazyListState()

        composeRule.setContent {
            CoolingGlassTheme(darkTheme = false) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        TestsCatalogScreen(
                            tests = allTests,
                            myAttempts = myAttempts,
                            currentMember = member,
                            catalogListState = if (selectedNav == "completed_tests") completedListState else pendingListState,
                            pausedSessions = pausedMap,
                            catalogMode = if (selectedNav == "completed_tests") {
                                com.example.ui.screens.TestsCatalogMode.COMPLETED_ONLY
                            } else {
                                com.example.ui.screens.TestsCatalogMode.PENDING_AND_PAUSED_ONLY
                            },
                            onStartTest = {},
                            onViewResult = { t, _ -> clickedResultTestId = t.id },
                            onViewTestRanking = { clickedRankTestId = it.id },
                            onBannerMessage = { _, _ -> }
                        )
                    }
                    com.example.ui.glass.CoolingGlassNavigationBar(
                        items = listOf(
                            com.example.ui.glass.GlassNavItem("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
                            com.example.ui.glass.GlassNavItem("tests", "Tests", Icons.Rounded.Quiz, Icons.Outlined.Quiz),
                            com.example.ui.glass.GlassNavItem("completed_tests", "Completed", Icons.Rounded.TaskAlt, Icons.Outlined.TaskAlt),
                            com.example.ui.glass.GlassNavItem("live_exams", "Live Exam", Icons.Rounded.Podcasts, Icons.Outlined.Podcasts),
                            com.example.ui.glass.GlassNavItem("polls", "Polls", Icons.Rounded.Poll, Icons.Outlined.Poll),
                            com.example.ui.glass.GlassNavItem("ranks", "Ranks", Icons.Rounded.EmojiEvents, Icons.Outlined.EmojiEvents),
                            com.example.ui.glass.GlassNavItem("profile", "Profile", Icons.Rounded.Person, Icons.Outlined.Person)
                        ),
                        selectedId = selectedNav,
                        onSelect = { selectedNav = it }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // 1. All 7 bottom nav bar items exist including the new Completed tab
        listOf("nav_home", "nav_tests", "nav_completed_tests", "nav_live_exams", "nav_polls", "nav_ranks", "nav_profile").forEach { tag ->
            composeRule.onNodeWithTag(tag).assertExists()
        }

        // 2. Tests tab shows ONLY Pending & Paused sets — newest Admin set is FIRST (top), completed sets are excluded
        composeRule.onNodeWithTag("pending_tests_list").assertExists()
        composeRule.onNodeWithTag("pending_tests_list")
            .performScrollToNode(hasTestTag("test_set_card_t_newest_admin"))
        composeRule.waitForIdle()
        val newestItemIndex = pendingListState.layoutInfo.visibleItemsInfo.first { it.key == "t_newest_admin" }.index

        composeRule.onNodeWithTag("pending_tests_list")
            .performScrollToNode(hasTestTag("test_set_card_t_paused_mid"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("resume_test_t_paused_mid").assertExists()
        val pausedItemIndex = pendingListState.layoutInfo.visibleItemsInfo.first { it.key == "t_paused_mid" }.index

        composeRule.onNodeWithTag("pending_tests_list")
            .performScrollToNode(hasTestTag("test_set_card_t_old_pending"))
        composeRule.waitForIdle()
        val oldestItemIndex = pendingListState.layoutInfo.visibleItemsInfo.first { it.key == "t_old_pending" }.index

        assertTrue("Newest Admin set must appear strictly above older paused set", newestItemIndex < pausedItemIndex)
        assertTrue("Paused mid set must appear strictly above oldest pending set", pausedItemIndex < oldestItemIndex)
        assertEquals(0, composeRule.onAllNodesWithTag("test_set_card_t_done_old").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("test_set_card_t_done_new").fetchSemanticsNodes().size)

        // 3. Click Completed tab in bottom bar -> shows ONLY completed sets with ALL features (7 categories, View Rank, View Result, PDF)
        composeRule.onNodeWithTag("nav_completed_tests").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("completed_sets_list").assertExists()
        composeRule.onNodeWithTag("six_part_filter_row").assertExists()
        composeRule.onNodeWithTag("completed_sets_list")
            .performScrollToNode(hasTestTag("test_set_card_t_done_new"))
        composeRule.waitForIdle()
        val doneNewIndex = completedListState.layoutInfo.visibleItemsInfo.first { it.key == "t_done_new" }.index

        // Verify View Rank and View Result work directly inside Completed Sets tab
        composeRule.onNodeWithTag("view_ranking_eye_t_done_new").performClick()
        composeRule.waitForIdle()
        assertEquals("t_done_new", clickedRankTestId)
        composeRule.onAllNodesWithText("View Result & Solutions")[0].performClick()
        composeRule.waitForIdle()
        assertEquals("t_done_new", clickedResultTestId)

        composeRule.onNodeWithTag("completed_sets_list")
            .performScrollToNode(hasTestTag("test_set_card_t_done_old"))
        composeRule.waitForIdle()
        val doneOldIndex = completedListState.layoutInfo.visibleItemsInfo.first { it.key == "t_done_old" }.index
        assertTrue("Newest completed set must appear above older completed set", doneNewIndex < doneOldIndex)

        assertEquals(0, composeRule.onAllNodesWithTag("test_set_card_t_newest_admin").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("test_set_card_t_paused_mid").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("test_set_card_t_old_pending").fetchSemanticsNodes().size)
    }

    @Test
    fun `QuestionBankScreen 150Q Math and Science JSON import with nested options and missing part passes Live Validation with zero errors`() {
        val adminMember = GroupMember(
            groupId = "grp_150",
            userId = "admin_150",
            displayName = "Admin Sir",
            role = "GROUP_ADMIN"
        )
        val sb = StringBuilder("{\"questions\":[")
        var qNum = 1
        fun appendQ(subject: String, nested: Boolean) {
            if (qNum > 1) sb.append(",")
            val opts = if (nested) {
                "\"options\":{\"A\":\"A$qNum\",\"B\":\"B$qNum\",\"C\":\"C$qNum\",\"D\":\"D$qNum\"}"
            } else {
                "\"optionA\":\"A$qNum\",\"optionB\":\"B$qNum\",\"optionC\":\"C$qNum\",\"optionD\":\"D$qNum\""
            }
            sb.append("""{"subject":"$subject","question":"UI 150Q #$qNum ($subject)?",$opts,"correctAnswer":"A","explanation":"Exp $qNum"}""")
            qNum++
        }
        repeat(22) { appendQ("Hindi Language", it % 2 == 0) }
        repeat(8) { appendQ("English Language", it % 2 == 1) }
        repeat(8) { appendQ("Indian History", it % 2 == 0) }
        repeat(8) { appendQ("Indian Geography", it % 2 == 1) }
        repeat(8) { appendQ("Current Affairs", it % 2 == 0) }
        repeat(48) { appendQ("Mathematics", it % 2 == 0) }
        repeat(48) { appendQ("Science", it % 2 == 1) }
        sb.append("]}")

        val banners = mutableListOf<Pair<String, Boolean>>()

        composeRule.setContent {
            CoolingGlassTheme {
                QuestionBankScreen(
                    currentMember = adminMember,
                    busy = false,
                    existingTests = emptyList(),
                    onCreateTestSet = { _, _, _, _, _, _, _, _, _, _, _ -> },
                    onBannerMessage = { msg, isErr -> banners.add(msg to isErr) }
                )
            }
        }
        composeRule.waitForIdle()

        // 1. Select 6-8 Full Paper (150 Q)
        composeRule.onNodeWithTag("question_bank_list")
            .performScrollToNode(hasTestTag("paper_type_6_8"))
        composeRule.onNodeWithTag("paper_type_6_8").performClick()
        composeRule.waitForIdle()

        // 2. Switch to JSON / CSV tab
        composeRule.onNodeWithTag("question_bank_list")
            .performScrollToNode(hasText("JSON / CSV"))
        composeRule.onNodeWithText("JSON / CSV").performClick()
        composeRule.waitForIdle()

        // 3. Paste mixed flat/nested 150-Q JSON without `part` field & click Validate, Parse & Import
        composeRule.onNodeWithTag("question_bank_list")
            .performScrollToNode(hasTestTag("admin_json_csv_input"))
        composeRule.onNodeWithTag("admin_json_csv_input").performTextInput(sb.toString())
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("question_bank_list")
            .performScrollToNode(hasTestTag("parse_paste_json_csv_btn"))
        composeRule.onNodeWithTag("parse_paste_json_csv_btn").performClick()
        composeRule.waitForIdle()

        // 4. Verify Live Validation Dashboard shows 150/150 Qs and zero errors + Fix Section / Auto-Fix button works
        assertTrue(
            "Expected successful JSON import banner, got: $banners",
            banners.any { !it.second && it.first.contains("150") }
        )
        composeRule.onNodeWithTag("question_bank_list")
            .performScrollToNode(hasTestTag("live_validation_dashboard_card"))
        composeRule.waitForIdle()

        composeRule.onNodeWithText("150 / 150 Qs").assertExists()
        composeRule.onNodeWithText("Part 1 Lang: 30/30").assertExists()
        composeRule.onNodeWithText("Part 2 Common: 40/40").assertExists()
        composeRule.onNodeWithText("Part 3 Subject: 80/80").assertExists()
        composeRule.onNodeWithText("All BPSSC TRE 4.0 blueprint rules & answer keys verified! Ready to publish.").assertExists()
        composeRule.onNodeWithTag("auto_fix_sections_btn").assertExists()
    }

    @Test
    fun `150Q Math and Science JSON with explicit part variants, flat and nested options, and subject variants passes with zero errors`() {
        val sb = StringBuilder("{\"questions\":[")
        var qIndex = 1
        fun appendQ(part: String, subject: String, useNestedOptions: Boolean) {
            if (qIndex > 1) sb.append(",")
            val opts = if (useNestedOptions) {
                "\"options\":{\"A\":\"Opt A $qIndex\",\"B\":\"Opt B $qIndex\",\"C\":\"Opt C $qIndex\",\"D\":\"Opt D $qIndex\"}"
            } else {
                "\"optionA\":\"Opt A $qIndex\",\"optionB\":\"Opt B $qIndex\",\"optionC\":\"Opt C $qIndex\",\"optionD\":\"Opt D $qIndex\""
            }
            sb.append(
                """{"part":"$part","subject":"$subject","question":"TRE 4.0 Explicit Q#$qIndex ($subject)?",$opts,"correctAnswer":"B","explanation":"Step $qIndex"}"""
            )
            qIndex++
        }

        repeat(22) { i -> appendQ(if (i % 3 == 0) "Part 1" else if (i % 3 == 1) "1" else "Language", "Hindi Language", i % 2 == 0) }
        repeat(8) { i -> appendQ("Part 1", "English Language", i % 2 == 1) }
        repeat(8) { i -> appendQ(if (i % 2 == 0) "Part 2" else "General Studies", "Mathematics", i % 2 == 0) }
        repeat(8) { i -> appendQ(if (i % 2 == 0) "2" else "Common", "Science", i % 2 == 1) }
        repeat(8) { i -> appendQ("Part 2", "Indian History", i % 2 == 0) }
        repeat(8) { i -> appendQ("Common", "Indian Geography", i % 2 == 1) }
        repeat(8) { i -> appendQ("General Studies", "Current Affairs", i % 2 == 0) }
        repeat(40) { i -> appendQ(if (i % 3 == 0) "Part 3" else if (i % 3 == 1) "3" else "Main Subject", "Mathematics", i % 2 == 0) }
        repeat(40) { i -> appendQ(if (i % 2 == 0) "Subject" else "Part 3", "Science", i % 2 == 1) }
        sb.append("]}")

        val rawJson = sb.toString()

        val strict = TestDraftStore.validateRawJsonStrict(rawJson)
        assertTrue("Strict validator failed: ${strict.errorMessage}", strict.isValid)
        assertEquals(150, strict.parsedQuestions.size)

        val parsed = BulkQuestionParser.parseJson(rawJson, PaperType.CLASS_6_8_MATH_SCIENCE).getOrThrow()
        assertEquals(150, parsed.size)

        val report = BulkQuestionParser.validate(parsed, PaperType.CLASS_6_8_MATH_SCIENCE)
        assertTrue("Expected zero validation errors, got: ${report.errors}", report.valid)
        assertEquals(30, report.part1LanguageCount)
        assertEquals(22, report.hindiCount)
        assertEquals(8, report.englishCount)
        assertEquals(40, report.commonCount)
        assertEquals(8, report.part2CommonSubjectCounts["Mathematics"])
        assertEquals(8, report.part2CommonSubjectCounts["Science"])
        assertEquals(8, report.part2CommonSubjectCounts["History"])
        assertEquals(8, report.part2CommonSubjectCounts["Geography"])
        assertEquals(8, report.part2CommonSubjectCounts["Current Affairs"])
        assertEquals(80, report.subjectPaperCount)
        assertEquals(40, report.part3SubjectCounts["Mathematics"])
        assertEquals(40, report.part3SubjectCounts["Science"])
    }

    @Test
    fun `JSON Template and CSV Template stay synced with strict validator and parser including part field and optionA-D`() {
        val strict = TestDraftStore.validateRawJsonStrict(BulkQuestionParser.JSON_TEMPLATE)
        assertTrue("JSON_TEMPLATE failed strict validation: ${strict.errorMessage}", strict.isValid)
        assertEquals(3, strict.parsedQuestions.size)

        val parsedJson = BulkQuestionParser.parseJson(BulkQuestionParser.JSON_TEMPLATE, PaperType.CLASS_6_8_MATH_SCIENCE).getOrThrow()
        assertEquals(3, parsedJson.size)
        assertEquals(ExamQuestion.SECTION_LANGUAGE, parsedJson[0].section)
        assertEquals(ExamQuestion.SECTION_COMMON, parsedJson[1].section)
        assertEquals(ExamQuestion.SECTION_SUBJECT, parsedJson[2].section)

        val parsedCsv = BulkQuestionParser.parseCsv(BulkQuestionParser.CSV_TEMPLATE, PaperType.CLASS_6_8_MATH_SCIENCE)
        assertEquals(3, parsedCsv.size)
        assertEquals(ExamQuestion.SECTION_LANGUAGE, parsedCsv[0].section)
        assertEquals(ExamQuestion.SECTION_COMMON, parsedCsv[1].section)
        assertEquals(ExamQuestion.SECTION_SUBJECT, parsedCsv[2].section)
    }

    @Test
    fun `ImageCodec detects animated GIF and transparent PNG formats and decodes raw bytes safely`() {
        val gifDataUri = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"
        val pngDataUri = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
        val rawGifBase64 = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"

        assertTrue(ImageCodec.isGifBase64(gifDataUri))
        assertTrue(ImageCodec.isGifBase64(rawGifBase64))
        assertFalse(ImageCodec.isGifBase64(pngDataUri))

        assertTrue(ImageCodec.isPngBase64(pngDataUri))
        assertFalse(ImageCodec.isPngBase64(gifDataUri))

        val gifBytes = ImageCodec.decodeToBytes(gifDataUri)
        assertNotNull(gifBytes)
        assertTrue(ImageCodec.hasGifMagicBytes(gifBytes!!))
        assertFalse(ImageCodec.hasPngMagicBytes(gifBytes))

        val pngBytes = ImageCodec.decodeToBytes(pngDataUri)
        assertNotNull(pngBytes)
        assertTrue(ImageCodec.hasPngMagicBytes(pngBytes!!))
        assertFalse(ImageCodec.hasGifMagicBytes(pngBytes))
    }

    @Test
    fun `resolveEffectiveSetIcon inherits 7-category Admin default icons and respects per-set override precedence`() {
        val mathGif = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"
        val setOverridePng = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
        val categoryMap = mapOf(
            RankCategory.MATHEMATICS.name to CategoryIconConfig(
                id = RankCategory.MATHEMATICS.name,
                categoryKey = RankCategory.MATHEMATICS.name,
                customIconBase64 = mathGif
            ),
            RankCategory.LANGUAGE.name to CategoryIconConfig(
                id = RankCategory.LANGUAGE.name,
                categoryKey = RankCategory.LANGUAGE.name,
                iconKey = com.example.util.SetIconOption.TROPHY.key
            )
        )

        // 1. Set with no per-set icon inherits 7-category Admin GIF for MATHEMATICS
        val inheritedMath = resolveEffectiveSetIcon(
            iconKey = null,
            customIconBase64 = null,
            paperType = PaperType.COMMON_GS,
            categoryIcons = categoryMap,
            rankCategoryOverride = RankCategory.MATHEMATICS
        )
        assertEquals(mathGif, inheritedMath.first)

        // 2. Set with no per-set icon inherits Admin preset key for LANGUAGE
        val inheritedLang = resolveEffectiveSetIcon(
            iconKey = null,
            customIconBase64 = null,
            paperType = PaperType.LANGUAGE,
            categoryIcons = categoryMap,
            rankCategoryOverride = RankCategory.LANGUAGE
        )
        assertNull(inheritedLang.first)
        assertEquals(com.example.util.SetIconOption.TROPHY, inheritedLang.second)

        // 3. Per-set custom icon overrides 7-category Admin default
        val overriddenMath = resolveEffectiveSetIcon(
            iconKey = null,
            customIconBase64 = setOverridePng,
            paperType = PaperType.COMMON_GS,
            categoryIcons = categoryMap,
            rankCategoryOverride = RankCategory.MATHEMATICS
        )
        assertEquals(setOverridePng, overriddenMath.first)
    }

    @Test
    fun `Minimized ProfileAndSettingsScreen and AdminCategoryDefaultIconsDialog render cleanly with all 7 categories`() {
        val profile = UserProfile(
            userId = "u_prof_1",
            displayName = "Ananya Kumari",
            email = "ananya@example.com",
            whatsappNumber = "9876543210",
            languagePref = "BILINGUAL"
        )
        val member = GroupMember(
            userId = "u_prof_1",
            displayName = "Ananya Kumari",
            role = AppRole.GROUP_ADMIN.name,
            gsAverageScore = 34.5,
            gsTestsAttempted = 5
        )
        var showCategoryDialog by mutableStateOf(false)

        composeRule.setContent {
            CoolingGlassTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    ProfileAndSettingsScreen(
                        profile = profile,
                        member = member,
                        group = null,
                        myRank = 2,
                        myAttempts = emptyList(),
                        myCreatedTestsCount = 4,
                        myPollsCount = 2,
                        tests = emptyList(),
                        darkGlassMode = false,
                        busy = false,
                        hasAvailableUpdate = false,
                        showContactAdmin = true,
                        onToggleDarkMode = {},
                        onSaveProfile = { _, _, _, _, _ -> },
                        onOpenAttemptReview = { _, _ -> },
                        onOpenContactAdmin = {},
                        onOpenAppUpdate = {},
                        onOpenAboutFounder = {},
                        onSignOut = {},
                        onBannerMessage = { _, _ -> }
                    )
                    if (showCategoryDialog) {
                        AdminCategoryDefaultIconsDialog(
                            categoryIcons = emptyMap(),
                            busy = false,
                            onSaveCategoryIcon = { _, _, _ -> },
                            onResetCategoryIcon = {},
                            onDismiss = { showCategoryDialog = false }
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        // Verify minimized Profile screen renders cleanly with core tags & compact controls
        composeRule.onNodeWithTag("profile_screen_list").assertExists()
        composeRule.onNodeWithTag("profile_screen_list").performScrollToNode(hasTestTag("save_profile_btn"))
        composeRule.onNodeWithTag("profile_name_input").assertExists()
        composeRule.onNodeWithTag("profile_whatsapp_input").assertExists()
        composeRule.onNodeWithTag("save_profile_btn").assertExists()
        composeRule.onNodeWithTag("profile_screen_list").performScrollToNode(hasTestTag("sign_out_btn"))
        composeRule.onNodeWithTag("sign_out_btn").assertExists()

        // Open 7-Category Admin Icon Controller Dialog and verify all 7 RankCategory rows exist
        showCategoryDialog = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("admin_category_default_icons_dialog", useUnmergedTree = true).assertExists()
        RankCategory.entries.forEach { cat ->
            composeRule.onNodeWithTag("cat_icon_row_${cat.name.lowercase()}", useUnmergedTree = true).assertExists()
            composeRule.onNodeWithTag("upload_cat_icon_${cat.name.lowercase()}", useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun `AdminConsole places Fresh Launch at the very bottom and allows deleting Completed and Past trial Live Exams`() {
        val now = System.currentTimeMillis()
        val pastTrialExam1 = LiveExam(
            id = "past_trial_1",
            groupId = "grp_1",
            sourceSetId = "deleted_set_99",
            sourceSetTitle = "Deleted Trial Set 99",
            title = "Completed Trial Live Exam 1",
            startTimeEpochMillis = now - 7_200_000L,
            endTimeEpochMillis = now - 3_600_000L,
            durationMinutes = 60L,
            status = "PUBLISHED"
        )
        val pastTrialExam2 = LiveExam(
            id = "past_trial_2",
            groupId = "grp_1",
            sourceSetId = "set_1",
            sourceSetTitle = "Common Set 1",
            title = "Completed Trial Live Exam 2",
            startTimeEpochMillis = now - 10_800_000L,
            endTimeEpochMillis = now - 5_400_000L,
            durationMinutes = 45L,
            status = "COMPLETED"
        )
        val liveTests = (1..4).map { idx ->
            ExamTest(
                id = "live_set_$idx",
                groupId = "grp_1",
                title = "Approved Live Set $idx",
                paperType = PaperType.COMMON_GS.name,
                questionCount = 40L,
                durationMinutes = 45L,
                status = "PUBLISHED",
                approvedForAll = true
            )
        }
        var deletedSingleExamId: String? = null
        var bulkClearedCount = 0

        composeRule.setContent {
            CoolingGlassTheme {
                AdminDashboardScreen(
                    currentMember = GroupMember(
                        userId = "admin_1",
                        groupId = "grp_1",
                        displayName = "Super Admin",
                        role = AppRole.SUPER_ADMIN.name
                    ),
                    group = null,
                    members = emptyList(),
                    tests = liveTests,
                    attempts = emptyList(),
                    polls = emptyList(),
                    overallRanking = emptyList(),
                    onApproveOrRejectTest = { _, _ -> },
                    onApproveTestWithAccess = { _, _, _, _ -> },
                    onDeleteTest = {},
                    onWipeAllTrialData = {},
                    onAssignRole = { _, _ -> },
                    onSendNotification = { _, _, _ -> },
                    liveExams = listOf(pastTrialExam1, pastTrialExam2),
                    onRemoveLiveExamUnlinkOnly = { exam -> deletedSingleExamId = exam.id },
                    onClearCompletedAndPastLiveExams = { bulkClearedCount++ }
                )
            }
        }
        composeRule.waitForIdle()

        // 1. Fresh Launch button is NOT visible at the top before scrolling
        assertEquals(0, composeRule.onAllNodesWithTag("fresh_launch_wipe_btn").fetchSemanticsNodes().size)

        // 2. Scroll down to the Live Exam Completed & Past cleanup card on main Admin Console tab
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("admin_main_live_exam_cleanup_card"))
        composeRule.onNodeWithTag("admin_main_live_exam_cleanup_card").assertExists()
        composeRule.onNodeWithTag("admin_delete_live_exam_past_trial_1").performClick()
        assertEquals("past_trial_1", deletedSingleExamId)

        composeRule.onNodeWithTag("admin_clear_completed_live_exams_btn").performClick()
        assertEquals(1, bulkClearedCount)

        // 3. Scroll to the VERY BOTTOM to reach Fresh Launch — Clear All Trial Data
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("fresh_launch_wipe_btn"))
        composeRule.onNodeWithTag("fresh_launch_wipe_btn").assertExists()

        // 4. Open Live Exams tab via admin_open_live_exams_tab_btn and verify dedicated Completed & Past cleanup card there too
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("admin_open_live_exams_tab_btn"))
        composeRule.onNodeWithTag("admin_open_live_exams_tab_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("admin_live_exams_cleanup_card").performScrollTo().assertExists()
        composeRule.onNodeWithTag("delete_live_exam_btn_past_trial_2").performScrollTo().performClick()
        assertEquals("past_trial_2", deletedSingleExamId)

        composeRule.onNodeWithTag("clear_completed_past_live_exams_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_clear_completed_live_exams_btn").performScrollTo().performClick()
        assertEquals(2, bulkClearedCount)
    }
}

