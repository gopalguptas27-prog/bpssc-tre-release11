package com.example.domain

import com.example.data.model.ExamQuestion
import com.example.data.model.ExamTest
import com.example.data.model.GroupMember
import com.example.data.model.LangQualifyingStatus
import com.example.data.model.LanguageFailRankPolicy
import com.example.data.model.OptionEMode
import com.example.data.model.PaperType
import com.example.data.model.RankCategory
import com.example.data.model.RankedAspirant
import com.example.data.model.RankerDetailBreakdown
import com.example.data.model.RankerSectionStat
import com.example.data.model.RankerSubjectStat
import com.example.data.model.RankingPeriod
import com.example.data.model.SetRankEntry
import com.example.data.model.SetType
import com.example.data.model.TestAttempt
import com.example.data.model.TieBreakPolicy
import com.example.util.IstClock
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

enum class AnswerOutcome {
    CORRECT,
    INCORRECT,
    DONT_KNOW,
    NOT_ATTEMPTED
}

data class EvaluationResult(
    val correctCount: Int,
    val incorrectCount: Int,
    val dontKnowCount: Int,
    val notAttemptedCount: Int,
    val totalScore: Double,
    val maxMarks: Double,
    val percentage: Double,
    val accuracyPercentage: Double,
    val negativeDeducted: Double,
    val subjectScores: Map<String, Double>,
    val hindiScore: Double,
    val englishScore: Double,
    val langPassed: Boolean,
    val outcomes: Map<Int, AnswerOutcome>,
    // Separated Merit vs Qualifying fields
    val meritMarks: Double = totalScore,
    val totalMeritMarks: Double = maxMarks,
    val positiveMarksEarned: Double = correctCount.toDouble(),
    val meritCorrectCount: Int = correctCount,
    val meritIncorrectCount: Int = incorrectCount,
    val meritDontKnowCount: Int = dontKnowCount,
    val meritNotAttemptedCount: Int = notAttemptedCount,
    val meritNegativeDeducted: Double = negativeDeducted,
    val languageTotalMarks: Double = hindiScore + englishScore,
    val languageCorrectCount: Int = 0,
    val languageIncorrectCount: Int = 0,
    val languageUnattemptedCount: Int = 0
)

data class SubjectAnalytics(
    val subject: String,
    val averagePercentage: Double,
    val totalScore: Double,
    val attemptsCount: Int
)

data class SectionPerformanceRow(
    val title: String,
    val isQualifying: Boolean,
    val isSubSubject: Boolean = false,
    val totalQuestions: Int,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val unattempted: Int,
    val score: Double,
    val maxMarks: Double,
    val accuracyPct: Double,
    val timeSpentSec: Long,
    val strengthTag: String
)

data class TopperVsAverageComparison(
    val yourMeritScore: Double,
    val topperMeritScore: Double,
    val averageMeritScore: Double,
    val meritMaxMarks: Double,
    val yourAccuracy: Double,
    val topperAccuracy: Double,
    val averageAccuracy: Double,
    val yourAttempted: Int,
    val topperAttempted: Int,
    val averageAttempted: Double,
    val yourTimeSec: Long,
    val topperTimeSec: Long,
    val averageTimeSec: Long,
    val totalParticipants: Int,
    val isQualifyingOnly: Boolean = false
)

/**
 * Separate subject-wise performance row for an individual student in a specific Set-Wise Rank entry.
 * Displays exact Marks (Number), Right, Wrong, and Not Attempted counts for each subject in that set.
 */
data class SetRankerSubjectStat(
    val subject: String,
    val isQualifying: Boolean,
    val marks: Double,
    val maxMarks: Double,
    val rightCount: Int,
    val wrongCount: Int,
    val notAttemptedCount: Int,
    val optionECount: Int = 0,
    val blankCount: Int = 0,
    val totalQuestions: Int = rightCount + wrongCount + notAttemptedCount,
    val hasDetailedCounts: Boolean = true
)

/**
 * Authoritative BPSSC TRE 4.0 Scoring, Section Analysis & Independent 6-Part Ranking Engine.
 *
 * Core Non-Negotiable Rules:
 *  1. Language Paper (30 Q: 8 English + 22 Hindi) is STRICTLY QUALIFYING ONLY (>= 9/30 PASS).
 *     Language marks are NEVER added to Merit Score, Percentage, Cutoff, Percentile, or Rank in
 *     Part 2 (0 Merit), Part 5 (120 Merit), Part 6 (120 Merit), or Legacy Combined 70Q (40 Merit).
 *  2. Configurable Marking per Set:
 *     - Positive marks (default +1.0)
 *     - Negative marking (default -1/3; supports 0, 1/3, 1/4, or custom numerator/denominator)
 *     - Option E Mode A: NOT_ATTEMPTED (0 penalty, tracked separately from blank)
 *     - Option E Mode B: STANDARD_5TH_OPTION (evaluated like A–D against correctOption)
 *  3. Original Attempt Immutability:
 *     - Rankings and analytics operate exclusively on locked Original Attempts, NEVER Solution Reattempts.
 */
object ExamScoringEngine {

    const val LANGUAGE_PASS_THRESHOLD = 9.0

    fun evaluate(
        test: ExamTest,
        questions: List<ExamQuestion>,
        selectedAnswers: Map<Int, String>
    ): EvaluationResult {
        val positiveMarks = if (test.positiveMarks > 0.0) test.positiveMarks else 1.0
        val penaltyPerWrong = when {
            test.negativeMarkNumerator <= 0L -> 0.0
            test.negativeMarkDenominator > 0L -> test.negativeMarkNumerator.toDouble() / test.negativeMarkDenominator.toDouble()
            else -> 1.0 / 3.0
        }
        val penaltyPerUnattempted = if (test.unattemptedPenalty && penaltyPerWrong > 0.0) penaltyPerWrong else 0.0
        val optionEMode = test.resolvedOptionEMode()
        val setType = test.resolvedSetType()
        val paperType = test.parsedPaperType()

        var correct = 0
        var incorrect = 0
        var dontKnow = 0
        var notAttempted = 0

        var meritCorrect = 0
        var meritIncorrect = 0
        var meritDontKnow = 0
        var meritNotAttempted = 0
        var meritQuestionCount = 0

        var langCorrect = 0
        var langIncorrect = 0
        var langDontKnow = 0
        var langNotAttempted = 0
        var hindiScore = 0.0
        var englishScore = 0.0
        var langRawScore = 0.0

        val subjectTotals = linkedMapOf<String, Double>()
        val outcomes = linkedMapOf<Int, AnswerOutcome>()

        questions.forEachIndexed { index, q ->
            val rawChoice = selectedAnswers[index]?.uppercase()?.trim()
            val correctKey = q.correctOption.uppercase().trim()
            val subjectKey = q.subject.ifBlank { "General" }
            val isLangQuestion = setType == SetType.PART_2_LANGUAGE ||
                (setType.hasLanguageSection && (
                    q.resolvedSection() == ExamQuestion.SECTION_LANGUAGE ||
                        subjectKey.contains("Hindi", ignoreCase = true) ||
                        subjectKey.contains("हिन्दी") ||
                        subjectKey.contains("हिंदी") ||
                        subjectKey.contains("English", ignoreCase = true)
                    ))

            val delta: Double
            val outcome: AnswerOutcome

            when {
                // Skip unverified / blank answer key safely so phantom marks aren't awarded/deducted
                correctKey.isBlank() && !rawChoice.isNullOrBlank() && rawChoice != "E" -> {
                    notAttempted++
                    delta = 0.0
                    outcome = AnswerOutcome.NOT_ATTEMPTED
                }
                rawChoice.isNullOrBlank() -> {
                    notAttempted++
                    delta = -penaltyPerUnattempted
                    outcome = AnswerOutcome.NOT_ATTEMPTED
                }
                rawChoice == "E" && optionEMode == OptionEMode.NOT_ATTEMPTED -> {
                    dontKnow++
                    delta = 0.0
                    outcome = AnswerOutcome.DONT_KNOW
                }
                rawChoice == correctKey -> {
                    correct++
                    delta = positiveMarks
                    outcome = AnswerOutcome.CORRECT
                }
                else -> {
                    incorrect++
                    delta = -penaltyPerWrong
                    outcome = AnswerOutcome.INCORRECT
                }
            }

            outcomes[index] = outcome
            subjectTotals[subjectKey] = (subjectTotals[subjectKey] ?: 0.0) + delta

            if (isLangQuestion) {
                langRawScore += delta
                when (outcome) {
                    AnswerOutcome.CORRECT -> langCorrect++
                    AnswerOutcome.INCORRECT -> langIncorrect++
                    AnswerOutcome.DONT_KNOW -> langDontKnow++
                    AnswerOutcome.NOT_ATTEMPTED -> langNotAttempted++
                }
                if (subjectKey.contains("Hindi", ignoreCase = true) || subjectKey.contains("हिन्दी") || subjectKey.contains("हिंदी")) {
                    hindiScore += delta
                } else if (subjectKey.contains("English", ignoreCase = true)) {
                    englishScore += delta
                }
            } else {
                meritQuestionCount++
                when (outcome) {
                    AnswerOutcome.CORRECT -> meritCorrect++
                    AnswerOutcome.INCORRECT -> meritIncorrect++
                    AnswerOutcome.DONT_KNOW -> meritDontKnow++
                    AnswerOutcome.NOT_ATTEMPTED -> meritNotAttempted++
                }
            }
        }

        val meritPositiveEarned = meritCorrect * positiveMarks
        val meritNegativeDeducted = (meritIncorrect * penaltyPerWrong) + (meritNotAttempted * penaltyPerUnattempted)
        val meritScore = meritPositiveEarned - meritNegativeDeducted
        val meritMaxMarks = if (setType.isQualifyingOnly) {
            0.0
        } else {
            (meritQuestionCount.coerceAtLeast(1) * positiveMarks)
        }

        val allNegativeDeducted = (incorrect * penaltyPerWrong) + (notAttempted * penaltyPerUnattempted)
        val langTotalForPass = if (setType == SetType.PART_2_LANGUAGE || paperType == PaperType.LANGUAGE) {
            langRawScore
        } else if (setType.hasLanguageSection) {
            langRawScore
        } else {
            0.0
        }

        val langThreshold = if (test.langPassMarks > 0.0) test.langPassMarks else LANGUAGE_PASS_THRESHOLD
        val langPassed = setType.hasLanguageSection &&
            langTotalForPass >= langThreshold &&
            (test.langMinHindi <= 0.0 || hindiScore >= test.langMinHindi) &&
            (test.langMinEnglish <= 0.0 || englishScore >= test.langMinEnglish)

        // CRITICAL INVARIANT:
        // - For Part 2 (Language Only): totalScore = Language Qualifying score out of 30; meritMarks = 0.
        // - For Part 5 (150 Q), Part 6 (150 Q), and Legacy Combined 70Q:
        //   totalScore = meritMarks STRICTLY EXCLUDING Language; maxMarks = meritMaxMarks (120 or 40).
        // - For Part 1, Part 3, Part 4, Legacy 80Q, Practice: totalScore = meritMarks out of meritMaxMarks.
        val displayedTotalScore = if (setType.isQualifyingOnly) langRawScore else meritScore
        val displayedMaxMarks = if (setType.isQualifyingOnly) {
            (questions.size.coerceAtLeast(1) * positiveMarks)
        } else {
            meritMaxMarks
        }
        val displayedNegative = if (setType.isQualifyingOnly) allNegativeDeducted else meritNegativeDeducted
        val displayedPositive = if (setType.isQualifyingOnly) (correct * positiveMarks) else meritPositiveEarned
        val percentage = if (displayedMaxMarks > 0.0) (displayedTotalScore / displayedMaxMarks) * 100.0 else 0.0
        val accuracy = if (setType.isQualifyingOnly) {
            computeAccuracy(correct, incorrect)
        } else {
            computeAccuracy(meritCorrect, meritIncorrect)
        }

        return EvaluationResult(
            correctCount = correct,
            incorrectCount = incorrect,
            dontKnowCount = dontKnow,
            notAttemptedCount = notAttempted,
            totalScore = displayedTotalScore,
            maxMarks = displayedMaxMarks,
            percentage = percentage,
            accuracyPercentage = accuracy,
            negativeDeducted = displayedNegative,
            subjectScores = subjectTotals,
            hindiScore = hindiScore,
            englishScore = englishScore,
            langPassed = langPassed,
            outcomes = outcomes,
            meritMarks = if (setType.isQualifyingOnly) 0.0 else meritScore,
            totalMeritMarks = meritMaxMarks,
            positiveMarksEarned = displayedPositive,
            meritCorrectCount = meritCorrect,
            meritIncorrectCount = meritIncorrect,
            meritDontKnowCount = meritDontKnow,
            meritNotAttemptedCount = meritNotAttempted,
            meritNegativeDeducted = meritNegativeDeducted,
            languageTotalMarks = langTotalForPass,
            languageCorrectCount = langCorrect,
            languageIncorrectCount = langIncorrect,
            languageUnattemptedCount = langDontKnow + langNotAttempted
        )
    }

    /**
     * Authoritative TRE 4.0 Accuracy Formula:
     * Accuracy = Correct Answers / (Correct Answers + Incorrect Answers) * 100
     * Strictly excludes Option E (Not Attempted) and completely blank responses.
     */
    fun computeAccuracy(correct: Int, incorrect: Int): Double {
        val attemptedStandard = correct + incorrect
        if (attemptedStandard <= 0) return 0.0
        return (correct.toDouble() / attemptedStandard.toDouble()) * 100.0
    }

    /**
     * Authoritative Percentile Formula:
     * Percentile = ((Total Participants - User Rank) / Total Participants) * 100
     * Safe 1-participant edge case returns 100.0.
     */
    fun computePercentile(rank: Int, totalParticipants: Int): Double {
        if (totalParticipants <= 0) return 0.0
        if (totalParticipants == 1) return 100.0
        val clampedRank = rank.coerceIn(1, totalParticipants)
        return (((totalParticipants - clampedRank).toDouble() / totalParticipants.toDouble()) * 100.0)
            .coerceIn(0.0, 100.0)
    }

    /**
     * Builds granular Section-wise & Sub-Subject Performance rows for the Result Dashboard.
     * Breaks Common Paper into its 5 fixed sub-subjects (Maths 8, Science 8, History 8, Geography 8, Current Affairs 8)
     * and Language into English (8) + Hindi (22).
     */
    fun computeSectionAndSubSubjectPerformance(
        test: ExamTest,
        questions: List<ExamQuestion>,
        attempt: TestAttempt
    ): List<SectionPerformanceRow> {
        if (questions.isEmpty()) return emptyList()
        val setType = test.resolvedSetType()
        val positiveMarks = if (test.positiveMarks > 0.0) test.positiveMarks else 1.0
        val penaltyPerWrong = when {
            test.negativeMarkNumerator <= 0L -> 0.0
            test.negativeMarkDenominator > 0L -> test.negativeMarkNumerator.toDouble() / test.negativeMarkDenominator.toDouble()
            else -> 1.0 / 3.0
        }
        val penaltyPerUnattempted = if (test.unattemptedPenalty && penaltyPerWrong > 0.0) penaltyPerWrong else 0.0
        val optionEMode = test.resolvedOptionEMode()

        data class Bucket(
            val title: String,
            val isQualifying: Boolean,
            val isSubSubject: Boolean,
            var totalQ: Int = 0,
            var correct: Int = 0,
            var incorrect: Int = 0,
            var unattempted: Int = 0,
            var score: Double = 0.0,
            var timeSec: Long = 0L
        )

        val buckets = linkedMapOf<String, Bucket>()
        fun addToBucket(key: String, title: String, isQual: Boolean, isSub: Boolean, outcome: AnswerOutcome, delta: Double, qTime: Long) {
            val b = buckets.getOrPut(key) { Bucket(title, isQual, isSub) }
            b.totalQ++
            when (outcome) {
                AnswerOutcome.CORRECT -> b.correct++
                AnswerOutcome.INCORRECT -> b.incorrect++
                AnswerOutcome.DONT_KNOW, AnswerOutcome.NOT_ATTEMPTED -> b.unattempted++
            }
            b.score += delta
            b.timeSec += qTime
        }

        questions.forEachIndexed { idx, q ->
            val rawChoice = attempt.answers[idx.toString()]?.uppercase()?.trim()
            val correctKey = q.correctOption.uppercase().trim()
            val qTime = attempt.questionTimesSec[idx.toString()] ?: 0L
            val outcome: AnswerOutcome
            val delta: Double
            when {
                rawChoice.isNullOrBlank() -> {
                    outcome = AnswerOutcome.NOT_ATTEMPTED
                    delta = -penaltyPerUnattempted
                }
                rawChoice == "E" && optionEMode == OptionEMode.NOT_ATTEMPTED -> {
                    outcome = AnswerOutcome.DONT_KNOW
                    delta = 0.0
                }
                correctKey.isNotBlank() && rawChoice == correctKey -> {
                    outcome = AnswerOutcome.CORRECT
                    delta = positiveMarks
                }
                correctKey.isBlank() -> {
                    outcome = AnswerOutcome.NOT_ATTEMPTED
                    delta = 0.0
                }
                else -> {
                    outcome = AnswerOutcome.INCORRECT
                    delta = -penaltyPerWrong
                }
            }

            val normSubj = BulkQuestionParser.normalizeSubject(q.subject).let {
                if (it == BulkQuestionParser.UNCLASSIFIED_SUBJECT) q.subject.ifBlank { "General" } else it
            }
            val sec = q.resolvedSection()
            val isLang = setType == SetType.PART_2_LANGUAGE ||
                (setType.hasLanguageSection && (sec == ExamQuestion.SECTION_LANGUAGE || normSubj == "Hindi" || normSubj == "English"))

            if (isLang) {
                addToBucket("SEC_LANG", "Language Paper (Qualifying)", true, false, outcome, delta, qTime)
                val langSub = if (normSubj == "English") "English (Qualifying)" else "Hindi (Qualifying)"
                addToBucket("SUB_LANG_$normSubj", langSub, true, true, outcome, delta, qTime)
            } else if (setType == SetType.PART_5_FULL_MATH_SCI && sec == ExamQuestion.SECTION_SUBJECT) {
                val subjLabel = if (normSubj == "Science") "Part 4 – Science (Subject)" else "Part 3 – Mathematics (Subject)"
                addToBucket("SEC_SUBJ_$normSubj", subjLabel, false, false, outcome, delta, qTime)
            } else if (setType == SetType.PART_6_FULL_MATH && sec == ExamQuestion.SECTION_SUBJECT) {
                addToBucket("SEC_SUBJ_MATH", "Subject – Mathematics (80 Q)", false, false, outcome, delta, qTime)
            } else if (setType == SetType.PART_3_MATHS) {
                addToBucket("SEC_MATH", "Part 3 – Mathematics", false, false, outcome, delta, qTime)
            } else if (setType == SetType.PART_4_SCIENCE) {
                addToBucket("SEC_SCI", "Part 4 – Science", false, false, outcome, delta, qTime)
            } else {
                addToBucket("SEC_COMMON", "Part 1 – Common Paper", false, false, outcome, delta, qTime)
                addToBucket("SUB_COMMON_$normSubj", normSubj, false, true, outcome, delta, qTime)
            }
        }

        return buckets.values.map { b ->
            val attempted = b.correct + b.incorrect
            val acc = computeAccuracy(b.correct, b.incorrect)
            val maxM = b.totalQ * positiveMarks
            val pct = if (maxM > 0.0) (b.score / maxM) * 100.0 else 0.0
            val strength = when {
                b.isQualifying -> "Qualifying"
                attempted == 0 -> "Not Attempted"
                acc >= 75.0 && pct >= 60.0 -> "Strong"
                acc < 50.0 || pct < 35.0 -> "Weak"
                else -> "Average"
            }
            SectionPerformanceRow(
                title = b.title,
                isQualifying = b.isQualifying,
                isSubSubject = b.isSubSubject,
                totalQuestions = b.totalQ,
                attempted = attempted,
                correct = b.correct,
                incorrect = b.incorrect,
                unattempted = b.unattempted,
                score = b.score,
                maxMarks = maxM,
                accuracyPct = acc,
                timeSpentSec = b.timeSec,
                strengthTag = strength
            )
        }
    }

    /**
     * Computes "Compare with Topper & Set Average" strictly on Merit Marks (excluding Language)
     * for Merit tests, or Qualifying score for Part 2 Language-only tests.
     */
    fun computeTopperVsAverage(
        test: ExamTest,
        myAttempt: TestAttempt,
        allAttemptsForTest: List<TestAttempt>
    ): TopperVsAverageComparison {
        val setType = test.resolvedSetType()
        val isQualOnly = setType.isQualifyingOnly
        val pool = allAttemptsForTest.ifEmpty { listOf(myAttempt) }

        fun scoreOf(a: TestAttempt): Double =
            if (isQualOnly) a.effectiveLanguageTotal() else a.effectiveMeritScore()

        fun attemptedOf(a: TestAttempt): Int =
            if (isQualOnly) (a.correctCount + a.incorrectCount).toInt()
            else ((a.meritCorrectCount ?: a.correctCount) + (a.meritIncorrectCount ?: a.incorrectCount)).toInt()

        fun accOf(a: TestAttempt): Double =
            if (isQualOnly) computeAccuracy(a.correctCount.toInt(), a.incorrectCount.toInt())
            else computeAccuracy((a.meritCorrectCount ?: a.correctCount).toInt(), (a.meritIncorrectCount ?: a.incorrectCount).toInt())

        val topper = pool.maxWithOrNull(
            compareBy<TestAttempt> { scoreOf(it) }
                .thenBy { accOf(it) }
                .thenByDescending { -it.timeSpentSeconds }
        ) ?: myAttempt

        val maxM = if (isQualOnly) 30.0 else myAttempt.effectiveMeritMaxMarks()
        return TopperVsAverageComparison(
            yourMeritScore = scoreOf(myAttempt),
            topperMeritScore = scoreOf(topper),
            averageMeritScore = pool.map { scoreOf(it) }.average(),
            meritMaxMarks = maxM,
            yourAccuracy = accOf(myAttempt),
            topperAccuracy = accOf(topper),
            averageAccuracy = pool.map { accOf(it) }.average(),
            yourAttempted = attemptedOf(myAttempt),
            topperAttempted = attemptedOf(topper),
            averageAttempted = pool.map { attemptedOf(it).toDouble() }.average(),
            yourTimeSec = myAttempt.timeSpentSeconds,
            topperTimeSec = topper.timeSpentSeconds,
            averageTimeSec = pool.map { it.timeSpentSeconds }.average().toLong(),
            totalParticipants = pool.size,
            isQualifyingOnly = isQualOnly
        )
    }

    /**
     * Enforces solution security: verified answers & explanations are only visible after submission
     * and once answerReleaseMode allows (IMMEDIATE, or SCHEDULED once scheduledAt has elapsed).
     */
    fun areSolutionsReleased(
        test: ExamTest,
        isAdmin: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (isAdmin) return true
        if (!test.answerReleaseMode.equals("SCHEDULED", ignoreCase = true)) return true
        val scheduledRaw = test.scheduledAt?.trim().orEmpty()
        if (scheduledRaw.isBlank()) return false
        val epochMs = scheduledRaw.toLongOrNull()
        if (epochMs != null) return nowMillis >= epochMs
        return try {
            val parsed = IstClock.parse(scheduledRaw, "yyyy-MM-dd'T'HH:mm")?.time ?: return false
            nowMillis >= parsed
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Strictly isolated 6-Part Category Ranking (Part 1, Part 3, Part 4, Part 5, Part 6).
     * NEVER mixes Part 5 (Maths & Science) with Part 6 (Full Maths) and NEVER includes Part 2 Language in Merit.
     */
    fun computePartCategoryRankings(
        targetSetType: SetType,
        members: List<GroupMember>,
        attempts: List<TestAttempt>,
        tests: List<ExamTest>
    ): List<RankedAspirant> {
        if (targetSetType == SetType.PART_2_LANGUAGE) {
            // Part 2 is Qualifying Only — no Merit Rank; return Qualifying History ordered by latest Language score
            val langTestIds = tests.filter { it.resolvedSetType() == SetType.PART_2_LANGUAGE }.map { it.id }.toSet()
            val langAttempts = attempts.filter { it.resolvedSetType() == SetType.PART_2_LANGUAGE || it.testId in langTestIds }
            val byUser = langAttempts.groupBy { it.userId }
            return members.mapNotNull { member ->
                val list = byUser[member.userId].orEmpty()
                if (list.isEmpty()) return@mapNotNull null
                val latest = list.maxByOrNull { it.submittedAt?.toDate()?.time ?: 0L } ?: list.last()
                RankedAspirant(
                    rank = 0, // Strictly 0 / unranked — Qualifying Only
                    member = member,
                    displayScore = latest.effectiveLanguageTotal(),
                    testsAttempted = list.size,
                    testsAvailable = tests.count { it.status == "PUBLISHED" && it.resolvedSetType() == SetType.PART_2_LANGUAGE },
                    maxMarks = 30.0,
                    showLanguageBadge = true,
                    setLanguageScore = latest.effectiveLanguageTotal(),
                    setLanguagePassed = latest.langPassed,
                    percentile = 0.0,
                    accuracyPercentage = computeAccuracy(latest.correctCount.toInt(), latest.incorrectCount.toInt()),
                    timeSpentSeconds = latest.timeSpentSeconds,
                    languageNotQualified = !latest.langPassed,
                    rankScopeLabel = "${targetSetType.shortLabel} – Qualifying History (No Merit Rank)",
                    meritBaseLabel = "Qualifying /30"
                )
            }.sortedByDescending { it.displayScore }
        }

        val matchingTests = tests.filter { it.status == "PUBLISHED" && it.resolvedSetType() == targetSetType }
        val matchingTestIds = matchingTests.map { it.id }.toSet()
        val matchingAttempts = attempts.filter {
            it.resolvedSetType() == targetSetType || it.testId in matchingTestIds
        }
        val byUser = matchingAttempts.groupBy { it.userId }

        data class PartCandidate(
            val member: GroupMember,
            val avgMerit: Double,
            val highestMerit: Double,
            val avgAccuracy: Double,
            val avgTimeSec: Long,
            val attempted: Int,
            val avgMax: Double,
            val latestLangScore: Double?,
            val latestLangPassed: Boolean?
        )

        val candidates = members.map { m ->
            val uAtts = byUser[m.userId].orEmpty()
            val count = uAtts.size
            val avgMerit = if (count > 0) uAtts.sumOf { it.effectiveMeritScore() } / count else Double.NEGATIVE_INFINITY
            val highest = uAtts.maxOfOrNull { it.effectiveMeritScore() } ?: Double.NEGATIVE_INFINITY
            val avgAcc = if (count > 0) {
                uAtts.map { computeAccuracy((it.meritCorrectCount ?: it.correctCount).toInt(), (it.meritIncorrectCount ?: it.incorrectCount).toInt()) }.average()
            } else 0.0
            val avgTime = if (count > 0) uAtts.map { it.timeSpentSeconds }.average().toLong() else Long.MAX_VALUE
            val avgMax = if (count > 0) uAtts.map { it.effectiveMeritMaxMarks() }.average() else targetSetType.meritMaxMarks
            val latest = uAtts.maxByOrNull { it.submittedAt?.toDate()?.time ?: 0L }
            PartCandidate(
                member = m,
                avgMerit = avgMerit,
                highestMerit = highest,
                avgAccuracy = avgAcc,
                avgTimeSec = avgTime,
                attempted = count,
                avgMax = avgMax,
                latestLangScore = if (targetSetType.hasLanguageSection && latest != null) latest.effectiveLanguageTotal() else null,
                latestLangPassed = if (targetSetType.hasLanguageSection && latest != null) latest.langPassed else null
            )
        }.sortedWith(
            compareByDescending<PartCandidate> { it.attempted > 0 }
                .thenByDescending { it.avgMerit }
                .thenByDescending { it.avgAccuracy }
                .thenBy { it.avgTimeSec }
                .thenBy { it.member.displayName.lowercase(Locale.US) }
        )

        val activeTotal = candidates.count { it.attempted > 0 }
        val ranked = mutableListOf<RankedAspirant>()
        var currentRank = 0
        var prevScore: Double? = null
        val meritLabel = "Merit out of ${targetSetType.meritMaxMarks.toInt()}"

        candidates.forEachIndexed { idx, c ->
            val score = if (c.attempted > 0) c.avgMerit else 0.0
            val prev = prevScore
            if (c.attempted > 0) {
                if (prev == null || abs(score - prev) > 0.0001) {
                    currentRank = idx + 1
                    prevScore = score
                }
            } else {
                currentRank = idx + 1
            }
            ranked.add(
                RankedAspirant(
                    rank = currentRank,
                    member = c.member,
                    displayScore = score,
                    testsAttempted = c.attempted,
                    testsAvailable = matchingTests.size,
                    maxMarks = c.avgMax,
                    showLanguageBadge = targetSetType.hasLanguageSection,
                    setLanguageScore = c.latestLangScore,
                    setLanguagePassed = c.latestLangPassed,
                    percentile = if (c.attempted > 0) computePercentile(currentRank, activeTotal) else 0.0,
                    accuracyPercentage = c.avgAccuracy,
                    timeSpentSeconds = if (c.attempted > 0) c.avgTimeSec else 0L,
                    languageNotQualified = targetSetType.hasLanguageSection && c.latestLangPassed == false,
                    rankScopeLabel = "${targetSetType.titleEn} ($meritLabel)",
                    meritBaseLabel = meritLabel
                )
            )
        }
        return ranked
    }

    /**
     * Competition ranking (1, 2, 2, 4 or Accuracy/Time tie-break).
     * - INDIVIDUAL_TEST ("By Set"): ranks aspirants strictly on the selected set using MERIT MARKS
     *   (Language strictly excluded in Part 5 /120, Part 6 /120, and Legacy Combined /40!).
     *   For Part 2 (Language Qualifying Only), displays Qualifying scores without mixing into Merit.
     * - WEEKLY / MONTHLY / DAILY / OVERALL: aggregates Merit scores strictly excluding Language.
     */
    fun computeRankings(
        period: RankingPeriod,
        members: List<GroupMember>,
        attempts: List<TestAttempt>,
        tests: List<ExamTest>,
        selectedTestId: String? = null
    ): List<RankedAspirant> {
        val nowMillis = System.currentTimeMillis()
        val dayWindow = 24L * 60L * 60L * 1000L
        val weekWindow = 7L * dayWindow
        val monthStartMillis = IstClock.calendar(nowMillis).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (period == RankingPeriod.INDIVIDUAL_TEST) {
            val selectedTest = tests.firstOrNull { it.id == selectedTestId }
            val testAttempts = if (selectedTestId.isNullOrBlank()) emptyList()
            else attempts.filter { it.testId == selectedTestId }

            val resolvedPaperType = selectedTest?.parsedPaperType()
                ?: testAttempts.firstOrNull()?.let { PaperType.fromString(it.paperType) }
                ?: PaperType.COMMON_GS
            val resolvedSetType = selectedTest?.resolvedSetType()
                ?: testAttempts.firstOrNull()?.resolvedSetType()
                ?: SetType.fromPaperType(resolvedPaperType)
            val showLang = resolvedSetType.hasLanguageSection
            val isQualifyingOnly = resolvedSetType.isQualifyingOnly
            val langFailPolicy = selectedTest?.resolvedLangFailRankPolicy() ?: LanguageFailRankPolicy.SHOW_WITH_NOT_QUALIFIED_BADGE
            val tieBreakPolicy = selectedTest?.resolvedTieBreakPolicy() ?: TieBreakPolicy.SHARED_EQUAL_RANK

            val defaultMax = when {
                isQualifyingOnly -> 30.0
                resolvedSetType == SetType.PART_5_FULL_MATH_SCI || resolvedSetType == SetType.PART_6_FULL_MATH -> 120.0
                resolvedSetType == SetType.LEGACY_COMBINED_70 -> 40.0
                else -> selectedTest?.questionCount?.toDouble()?.takeIf { it > 0.0 }
                    ?: testAttempts.firstOrNull()?.effectiveMeritMaxMarks()?.takeIf { it > 0.0 }
                    ?: 40.0
            }

            val attemptByUser = testAttempts.associateBy { it.userId }

            data class SetCandidate(
                val member: GroupMember,
                val attempt: TestAttempt?,
                val score: Double,
                val accuracy: Double,
                val timeSec: Long,
                val langFailed: Boolean,
                val excludedByLangFail: Boolean
            )

            val candidates = members.map { member ->
                val att = attemptByUser[member.userId]
                val scoreVal = when {
                    att == null -> Double.NEGATIVE_INFINITY
                    isQualifyingOnly -> att.effectiveLanguageTotal()
                    else -> att.effectiveMeritScore()
                }
                val accVal = if (att == null) 0.0 else {
                    if (isQualifyingOnly) computeAccuracy(att.correctCount.toInt(), att.incorrectCount.toInt())
                    else computeAccuracy((att.meritCorrectCount ?: att.correctCount).toInt(), (att.meritIncorrectCount ?: att.incorrectCount).toInt())
                }
                val failedLang = att != null && showLang && !isQualifyingOnly && !att.langPassed
                val excluded = failedLang && langFailPolicy == LanguageFailRankPolicy.EXCLUDE_FROM_MERIT_RANK
                SetCandidate(
                    member = member,
                    attempt = att,
                    score = if (excluded) Double.NEGATIVE_INFINITY else scoreVal,
                    accuracy = accVal,
                    timeSec = att?.timeSpentSeconds ?: Long.MAX_VALUE,
                    langFailed = failedLang,
                    excludedByLangFail = excluded
                )
            }.sortedWith(
                compareByDescending<SetCandidate> { it.attempt != null && !it.excludedByLangFail }
                    .thenByDescending { it.score }
                    .thenByDescending { it.accuracy }
                    .thenBy { it.timeSec }
                    .thenBy { it.member.displayName.lowercase(Locale.US) }
            )

            val activeRankedParticipants = candidates.count { it.attempt != null && !it.excludedByLangFail }
            val ranked = mutableListOf<RankedAspirant>()
            var currentRank = 0
            var prevScore: Double? = null
            var prevAcc: Double? = null
            var prevTime: Long? = null

            val meritBase = if (isQualifyingOnly) "Qualifying /30" else "Merit out of ${defaultMax.toInt()}"
            val scopeTitle = "${selectedTest?.title ?: resolvedSetType.shortLabel} ($meritBase)"

            candidates.forEachIndexed { idx, c ->
                val attempted = if (c.attempt != null) 1 else 0
                val rawScore = when {
                    c.attempt == null -> 0.0
                    isQualifyingOnly -> c.attempt.effectiveLanguageTotal()
                    else -> c.attempt.effectiveMeritScore()
                }
                if (c.attempt != null && !c.excludedByLangFail) {
                    val prev = prevScore
                    val isTie = when (tieBreakPolicy) {
                        TieBreakPolicy.SHARED_EQUAL_RANK -> prev != null && abs(rawScore - prev) <= 0.0001
                        TieBreakPolicy.ACCURACY_AND_TIME -> prev != null &&
                            abs(rawScore - prev) <= 0.0001 &&
                            prevAcc != null && abs(c.accuracy - prevAcc) <= 0.0001 &&
                            prevTime != null && c.timeSec == prevTime
                    }
                    if (!isTie) {
                        currentRank = idx + 1
                        prevScore = rawScore
                        prevAcc = c.accuracy
                        prevTime = c.timeSec
                    }
                } else {
                    currentRank = idx + 1
                }
                val langScoreForSet = if (showLang && c.attempt != null) {
                    c.attempt.effectiveLanguageTotal()
                } else null

                val effectiveMax = if (isQualifyingOnly) {
                    30.0
                } else {
                    c.attempt?.effectiveMeritMaxMarks()?.takeIf { it > 0.0 } ?: defaultMax
                }

                val rankToAssign = if (isQualifyingOnly || c.excludedByLangFail) 0 else currentRank
                val pct = if (!isQualifyingOnly && c.attempt != null && !c.excludedByLangFail) {
                    computePercentile(currentRank, activeRankedParticipants)
                } else 0.0

                ranked.add(
                    RankedAspirant(
                        rank = if (isQualifyingOnly) currentRank else rankToAssign,
                        member = c.member,
                        displayScore = rawScore,
                        testsAttempted = attempted,
                        testsAvailable = 1,
                        maxMarks = effectiveMax,
                        showLanguageBadge = showLang,
                        setLanguageScore = langScoreForSet,
                        setLanguagePassed = if (showLang && c.attempt != null) c.attempt.langPassed else null,
                        percentile = pct,
                        accuracyPercentage = c.accuracy,
                        timeSpentSeconds = c.attempt?.timeSpentSeconds ?: 0L,
                        languageNotQualified = c.langFailed,
                        rankScopeLabel = scopeTitle,
                        meritBaseLabel = meritBase
                    )
                )
            }
            return ranked
        }

        val meritPaperTypes = setOf(
            PaperType.COMMON_GS.name,
            PaperType.COMBINED.name,
            PaperType.CLASS_6_8_MATH.name,
            PaperType.CLASS_6_8_SCIENCE.name,
            PaperType.CLASS_9_10_MATH_80.name,
            PaperType.CLASS_6_8_MATH_SCIENCE.name,
            PaperType.CLASS_9_10_MATH.name
        )
        val eligibleMeritTests = tests.filter {
            it.status == "PUBLISHED" && it.paperType in meritPaperTypes
        }
        val availableCount = eligibleMeritTests.size

        val meritAttempts = attempts.filter { it.paperType in meritPaperTypes }
        val filteredAttempts = when (period) {
            RankingPeriod.OVERALL -> meritAttempts
            RankingPeriod.DAILY -> meritAttempts.filter {
                val ts = it.submittedAt?.toDate()?.time ?: nowMillis
                nowMillis - ts <= dayWindow
            }
            RankingPeriod.WEEKLY -> meritAttempts.filter {
                val ts = it.submittedAt?.toDate()?.time ?: nowMillis
                nowMillis - ts <= weekWindow
            }
            RankingPeriod.MONTHLY -> meritAttempts.filter {
                val ts = it.submittedAt?.toDate()?.time ?: nowMillis
                ts >= monthStartMillis
            }
            RankingPeriod.INDIVIDUAL_TEST -> emptyList()
        }

        val anyLanguageContent = tests.any { it.status == "PUBLISHED" && it.parsedPaperType().hasLanguageSection() } ||
            attempts.any { PaperType.fromString(it.paperType).hasLanguageSection() }

        val attemptsByUser = filteredAttempts.groupBy { it.userId }

        data class Candidate(
            val member: GroupMember,
            val score: Double,
            val highest: Double,
            val accuracy: Double,
            val avgTimeSec: Long,
            val attempted: Int,
            val avgMaxMarks: Double
        )

        val candidates = members.map { member ->
            val userAttempts = attemptsByUser[member.userId].orEmpty()
            val attemptedCount: Int
            val avgScore: Double
            val highestScore: Double
            val avgMax: Double
            val avgAcc: Double
            val avgTime: Long

            if (period == RankingPeriod.OVERALL && userAttempts.isEmpty() && member.gsTestsAttempted > 0L) {
                attemptedCount = member.gsTestsAttempted.toInt()
                avgScore = member.gsAverageScore
                highestScore = member.gsHighestScore
                avgMax = 40.0
                avgAcc = 0.0
                avgTime = 0L
            } else {
                attemptedCount = userAttempts.size
                avgScore = if (attemptedCount > 0) {
                    userAttempts.sumOf { it.effectiveMeritScore() } / attemptedCount.toDouble()
                } else {
                    Double.NEGATIVE_INFINITY
                }
                highestScore = userAttempts.maxOfOrNull { it.effectiveMeritScore() } ?: Double.NEGATIVE_INFINITY
                avgMax = if (attemptedCount > 0) {
                    userAttempts.sumOf { it.effectiveMeritMaxMarks() } / attemptedCount.toDouble()
                } else {
                    40.0
                }
                avgAcc = if (attemptedCount > 0) {
                    userAttempts.map {
                        computeAccuracy((it.meritCorrectCount ?: it.correctCount).toInt(), (it.meritIncorrectCount ?: it.incorrectCount).toInt())
                    }.average()
                } else 0.0
                avgTime = if (attemptedCount > 0) userAttempts.map { it.timeSpentSeconds }.average().toLong() else Long.MAX_VALUE
            }
            Candidate(member, avgScore, highestScore, avgAcc, avgTime, attemptedCount, avgMax)
        }.sortedWith(
            compareByDescending<Candidate> { it.attempted > 0 }
                .thenByDescending { it.score }
                .thenByDescending { it.highest }
                .thenByDescending { it.attempted }
                .thenBy { it.member.displayName.lowercase(Locale.US) }
        )

        val activeTotal = candidates.count { it.attempted > 0 }
        val ranked = mutableListOf<RankedAspirant>()
        var currentRank = 0
        var prevScore: Double? = null

        candidates.forEachIndexed { idx, candidate ->
            val normalizedScore = if (candidate.attempted > 0) candidate.score else 0.0
            val prev = prevScore
            if (candidate.attempted > 0) {
                if (prev == null || abs(normalizedScore - prev) > 0.0001) {
                    currentRank = idx + 1
                    prevScore = normalizedScore
                }
            } else {
                currentRank = idx + 1
            }
            ranked.add(
                RankedAspirant(
                    rank = currentRank,
                    member = candidate.member,
                    displayScore = normalizedScore,
                    testsAttempted = candidate.attempted,
                    testsAvailable = availableCount,
                    maxMarks = candidate.avgMaxMarks,
                    showLanguageBadge = anyLanguageContent,
                    percentile = if (candidate.attempted > 0) computePercentile(currentRank, activeTotal) else 0.0,
                    accuracyPercentage = candidate.accuracy,
                    timeSpentSeconds = if (candidate.attempted > 0) candidate.avgTimeSec else 0L,
                    rankScopeLabel = "${period.label} (Merit Only — Language Excluded)",
                    meritBaseLabel = "Merit Avg"
                )
            )
        }
        return ranked
    }

    fun formatScore(value: Double): String = String.format(Locale.US, "%.2f", value)

    fun languageBadgeText(member: GroupMember): String = when (member.langQualifying()) {
        LangQualifyingStatus.NOT_ATTEMPTED -> "NOT ATTEMPTED"
        LangQualifyingStatus.PASS -> "${formatScore(member.langScore)}/30 PASS"
        LangQualifyingStatus.FAIL -> "${formatScore(member.langScore)}/30 FAIL"
    }

    /**
     * Returns Language badge text ONLY when [entry.showLanguageBadge] is true.
     * For Common GS / Separate Subject / Practice sets without Language, strictly returns null.
     */
    fun setLanguageBadgeText(entry: RankedAspirant): String? {
        if (!entry.showLanguageBadge) return null
        val setScore = entry.setLanguageScore
        val setPassed = entry.setLanguagePassed
        if (setScore != null && setPassed != null) {
            return "${formatScore(setScore)}/30 ${if (setPassed) "PASS" else "FAIL"}"
        }
        return if (entry.testsAttempted == 0 && entry.setLanguageScore == null) {
            if (entry.testsAvailable == 1) "NOT ATTEMPTED" else languageBadgeText(entry.member)
        } else {
            languageBadgeText(entry.member)
        }
    }

    fun computeSubjectAnalytics(userAttempts: List<TestAttempt>): List<SubjectAnalytics> {
        val gsAttempts = userAttempts.filter {
            it.paperType == PaperType.COMMON_GS.name ||
                it.paperType == PaperType.COMBINED.name ||
                it.paperType == PaperType.CLASS_6_8_MATH.name ||
                it.paperType == PaperType.CLASS_6_8_SCIENCE.name ||
                it.paperType == PaperType.CLASS_9_10_MATH_80.name ||
                it.paperType == PaperType.CLASS_6_8_MATH_SCIENCE.name ||
                it.paperType == PaperType.CLASS_9_10_MATH.name
        }
        if (gsAttempts.isEmpty()) return emptyList()
        val totals = linkedMapOf<String, MutableList<Double>>()
        gsAttempts.forEach { attempt ->
            attempt.subjectScores.forEach { (subject, score) ->
                if (!subject.equals("Hindi", ignoreCase = true) && !subject.equals("English", ignoreCase = true)) {
                    val canonical = BulkQuestionParser.normalizeSubject(subject)
                    val label = if (canonical == BulkQuestionParser.UNCLASSIFIED_SUBJECT) subject else canonical
                    totals.getOrPut(label) { mutableListOf() }.add(score)
                }
            }
        }
        return totals.map { (subject, scores) ->
            val avg = scores.average()
            val pct = ((avg / 8.0) * 100.0).coerceIn(0.0, 100.0)
            SubjectAnalytics(
                subject = subject,
                averagePercentage = pct,
                totalScore = avg,
                attemptsCount = scores.size
            )
        }.sortedByDescending { it.totalScore }
    }

    /**
     * Central 7-Category x 5-Scope Ranking Engine for RANKS Hub:
     * Categories: LANGUAGE, COMMON, LANGUAGE_COMMON, MATHEMATICS, SCIENCE, FULL_MATHS_SCIENCE, FULL_MATHS.
     * - Strictly maps each test/attempt using RankCategory.fromTest / fromAttempt (never mixes categories).
     * - Excludes tests/attempts with missing/invalid category metadata or PRACTICE mode.
     * - Enforces First Valid Attempt per (userId, testId) so retakes cannot inflate rankings.
     * - Combined Category + Time scoping across SET, DAILY, WEEKLY, MONTHLY, OVERALL.
     */
    fun computeCategoryRankings(
        period: RankingPeriod,
        category: RankCategory,
        members: List<GroupMember>,
        attempts: List<TestAttempt>,
        tests: List<ExamTest>,
        nowMillis: Long = System.currentTimeMillis(),
        selectedTestId: String? = null
    ): List<RankedAspirant> {
        val testsById = tests.associateBy { it.id }
        val targetTest = selectedTestId?.trim()?.takeIf { it.isNotBlank() }?.let { testsById[it] }
        val categoryTests = tests.filter {
            it.status == "PUBLISHED" && RankCategory.fromTest(it) == category
        }

        val dayWindow = 24L * 60L * 60L * 1000L
        val weekWindow = 7L * dayWindow
        val monthStartMillis = IstClock.calendar(nowMillis).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // First valid attempt per (userId, testId) strictly within this category (or exact set testId when filtered)
        val validCategoryAttempts = attempts
            .filter { att ->
                if (!selectedTestId.isNullOrBlank()) {
                    att.testId == selectedTestId
                } else {
                    RankCategory.fromAttempt(att, testsById) == category
                }
            }
            .sortedBy { it.submittedAt?.toDate()?.time ?: Long.MAX_VALUE }
            .distinctBy { "${it.userId}::${it.testId}" }

        val scopedAttempts = when (period) {
            RankingPeriod.INDIVIDUAL_TEST, RankingPeriod.OVERALL -> validCategoryAttempts
            RankingPeriod.DAILY -> validCategoryAttempts.filter {
                val ts = it.submittedAt?.toDate()?.time ?: nowMillis
                nowMillis - ts <= dayWindow
            }
            RankingPeriod.WEEKLY -> validCategoryAttempts.filter {
                val ts = it.submittedAt?.toDate()?.time ?: nowMillis
                nowMillis - ts <= weekWindow
            }
            RankingPeriod.MONTHLY -> validCategoryAttempts.filter {
                val ts = it.submittedAt?.toDate()?.time ?: nowMillis
                ts >= monthStartMillis
            }
        }

        val attemptsByUser = scopedAttempts.groupBy { it.userId }

        data class CatCandidate(
            val member: GroupMember,
            val userAttempts: List<TestAttempt>,
            val totalScore: Double,
            val maxMarks: Double,
            val percentage: Double,
            val accuracy: Double,
            val timeSpentSeconds: Long,
            val attempted: Int,
            val correctCount: Int,
            val incorrectCount: Int,
            val unattemptedCount: Int,
            val subjectScores: Map<String, Double>,
            val subjectCorrectCounts: Map<String, Int>,
            val subjectTotalCounts: Map<String, Int>,
            val hindiScore: Double,
            val englishScore: Double,
            val latestLangScore: Double?,
            val latestLangPassed: Boolean?
        )

        val candidates = members.map { member ->
            val uAtts = attemptsByUser[member.userId].orEmpty()
            val count = uAtts.size
            val totalScore = if (count == 0) 0.0 else {
                uAtts.sumOf {
                    if (category.isQualifyingOnly) it.effectiveLanguageTotal() else it.effectiveMeritScore()
                }
            }
            val defaultTargetMax = if (targetTest != null) {
                if (category.isQualifyingOnly) 30.0
                else targetTest.resolvedSetType().meritMaxMarks.takeIf { it > 0.0 }
                    ?: targetTest.questionCount.toDouble().coerceAtLeast(1.0)
            } else {
                category.defaultMaxMarks
            }
            val totalMax = if (count == 0) defaultTargetMax else {
                uAtts.sumOf {
                    if (category.isQualifyingOnly) 30.0
                    else it.effectiveMeritMaxMarks().takeIf { m -> m > 0.0 } ?: defaultTargetMax
                }.coerceAtLeast(1.0)
            }
            val pct = if (count > 0 && totalMax > 0.0) (totalScore / totalMax) * 100.0 else 0.0

            var sumCorrect = 0
            var sumIncorrect = 0
            var sumUnattempted = 0
            var sumTime = 0L
            var sumHindi = 0.0
            var sumEnglish = 0.0
            val subjMarksMap = linkedMapOf<String, Double>()
            val subjCorrectMap = linkedMapOf<String, Int>()
            val subjTotalMap = linkedMapOf<String, Int>()

            uAtts.forEach { att ->
                val c = if (category.isQualifyingOnly) att.correctCount.toInt()
                else (att.meritCorrectCount ?: att.correctCount).toInt()
                val w = if (category.isQualifyingOnly) att.incorrectCount.toInt()
                else (att.meritIncorrectCount ?: att.incorrectCount).toInt()
                val u = if (category.isQualifyingOnly) (att.dontKnowCount + att.notAttemptedCount).toInt()
                else (att.meritUnattemptedCount ?: (att.dontKnowCount + att.notAttemptedCount)).toInt()

                sumCorrect += c
                sumIncorrect += w
                sumUnattempted += u
                sumTime += att.timeSpentSeconds
                sumHindi += att.hindiScore
                sumEnglish += att.englishScore

                att.subjectScores.forEach { (rawSubj, score) ->
                    val canonical = BulkQuestionParser.normalizeSubject(rawSubj)
                        .let { if (it == BulkQuestionParser.UNCLASSIFIED_SUBJECT) rawSubj.ifBlank { "General" } else it }
                    subjMarksMap[canonical] = (subjMarksMap[canonical] ?: 0.0) + score
                }

                val testObj = testsById[att.testId]
                if (testObj != null && testObj.questions.isNotEmpty()) {
                    val optMode = testObj.resolvedOptionEMode()
                    val parsedQs = testObj.parsedQuestions()
                    parsedQs.forEachIndexed { qIdx, q ->
                        val canonical = BulkQuestionParser.normalizeSubject(q.subject)
                            .let { if (it == BulkQuestionParser.UNCLASSIFIED_SUBJECT) q.subject.ifBlank { "General" } else it }
                        subjTotalMap[canonical] = (subjTotalMap[canonical] ?: 0) + 1
                        val chosen = att.answers[qIdx.toString()]?.uppercase()?.trim()
                        val key = q.correctOption.uppercase().trim()
                        val isUnattempted = chosen.isNullOrBlank() || (chosen == "E" && optMode == OptionEMode.NOT_ATTEMPTED)
                        if (!isUnattempted && key.isNotBlank() && chosen == key) {
                            subjCorrectMap[canonical] = (subjCorrectMap[canonical] ?: 0) + 1
                        }
                    }
                }
            }

            val acc = if (count > 0) computeAccuracy(sumCorrect, sumIncorrect) else 0.0
            val latest = uAtts.maxByOrNull { it.submittedAt?.toDate()?.time ?: 0L }
            val latestLang = if (category.hasLanguageSection && latest != null) latest.effectiveLanguageTotal() else null
            val latestLangPassed = if (category.hasLanguageSection && latest != null) latest.langPassed else null

            CatCandidate(
                member = member,
                userAttempts = uAtts,
                totalScore = if (count > 0) totalScore else Double.NEGATIVE_INFINITY,
                maxMarks = totalMax,
                percentage = pct,
                accuracy = acc,
                timeSpentSeconds = if (count > 0) sumTime else Long.MAX_VALUE,
                attempted = count,
                correctCount = sumCorrect,
                incorrectCount = sumIncorrect,
                unattemptedCount = sumUnattempted,
                subjectScores = subjMarksMap,
                subjectCorrectCounts = subjCorrectMap,
                subjectTotalCounts = subjTotalMap,
                hindiScore = sumHindi,
                englishScore = sumEnglish,
                latestLangScore = latestLang,
                latestLangPassed = latestLangPassed
            )
        }.sortedWith(
            compareByDescending<CatCandidate> { it.attempted > 0 }
                .thenByDescending { it.totalScore }
                .thenByDescending { it.accuracy }
                .thenBy { it.timeSpentSeconds }
                .thenBy { it.member.displayName.lowercase(Locale.US) }
        )

        val activeTotal = candidates.count { it.attempted > 0 }
        val ranked = mutableListOf<RankedAspirant>()
        var currentRank = 0
        var prevScore: Double? = null
        var prevAcc: Double? = null
        var prevTime: Long? = null

        val targetSetNum = targetTest?.resolvedSetNumber()
        val periodPrefix = if (period == RankingPeriod.INDIVIDUAL_TEST) "Set" else period.label
        val scopeLabel = if (targetTest != null) {
            val heading = targetSetNum?.let { "Set $it - Rank" } ?: "${targetTest.title} - Rank"
            "$heading • ${targetTest.difficulty} • ${category.label}"
        } else {
            "${category.label} • $periodPrefix (${category.meritBaseLabel})"
        }
        val availableCount = if (targetTest != null) 1 else categoryTests.size

        candidates.forEachIndexed { idx, c ->
            val cleanScore = if (c.attempted > 0) c.totalScore else 0.0
            if (c.attempted > 0) {
                val pScore = prevScore
                val pAcc = prevAcc
                val pTime = prevTime
                val isTie = pScore != null &&
                    abs(cleanScore - pScore) <= 0.0001 &&
                    pAcc != null && abs(c.accuracy - pAcc) <= 0.0001 &&
                    pTime != null && c.timeSpentSeconds == pTime
                if (!isTie) {
                    currentRank = idx + 1
                    prevScore = cleanScore
                    prevAcc = c.accuracy
                    prevTime = c.timeSpentSeconds
                }
            } else {
                currentRank = idx + 1
            }

            val setOrCountSuffix = if (targetTest != null) {
                targetSetNum?.let { "Set $it" } ?: targetTest.title
            } else {
                "${c.attempted} Set${if (c.attempted > 1) "s" else ""}"
            }
            val statusLabel = when {
                c.attempted == 0 -> "Not Attempted"
                category.hasLanguageSection && c.latestLangPassed == false -> "Lang Not Qualified"
                category.hasLanguageSection -> "Qualified • $setOrCountSuffix"
                else -> "Completed • $setOrCountSuffix"
            }

            ranked.add(
                RankedAspirant(
                    rank = currentRank,
                    member = c.member,
                    displayScore = cleanScore,
                    testsAttempted = c.attempted,
                    testsAvailable = availableCount,
                    maxMarks = c.maxMarks,
                    showLanguageBadge = category.hasLanguageSection,
                    setLanguageScore = c.latestLangScore,
                    setLanguagePassed = c.latestLangPassed,
                    percentile = if (c.attempted > 0) computePercentile(currentRank, activeTotal) else 0.0,
                    accuracyPercentage = c.accuracy,
                    timeSpentSeconds = if (c.attempted > 0) c.timeSpentSeconds else 0L,
                    languageNotQualified = category.hasLanguageSection && c.latestLangPassed == false,
                    rankScopeLabel = scopeLabel,
                    meritBaseLabel = category.meritBaseLabel,
                    subjectScores = c.subjectScores,
                    subjectCorrectCounts = c.subjectCorrectCounts,
                    subjectTotalCounts = c.subjectTotalCounts,
                    correctCount = c.correctCount,
                    incorrectCount = c.incorrectCount,
                    unattemptedCount = c.unattemptedCount,
                    hindiScore = c.hindiScore,
                    englishScore = c.englishScore,
                    attemptStatusLabel = statusLabel
                )
            )
        }
        return ranked
    }

    /**
     * Strict Set-Wise Ranking Engine for a single [ExamTest].
     * - Strictly filters attempts by [test.id] — NEVER mixes global/overall or other sets' attempts.
     * - Includes EVERY user who submitted an eligible attempt for [test.id] (even if missing from [members]).
     * - Applies NO participant cap (supports 150, 300, 1000+ participants seamlessly).
     * - Enforces First Valid Attempt per userId so duplicates cannot inflate or double-list users.
     * - Sorts highest score first and applies the test's configured tie-breaking & language policy.
     */
    fun computeSetWiseRankings(
        test: ExamTest,
        attempts: List<TestAttempt>,
        members: List<GroupMember> = emptyList()
    ): List<SetRankEntry> {
        val targetTestId = test.id.trim()
        if (targetTestId.isBlank()) return emptyList()

        val membersById = members.associateBy { it.userId }
        val setType = test.resolvedSetType()
        val isQualifyingOnly = setType.isQualifyingOnly
        val hasLanguageSection = setType.hasLanguageSection
        val tiePolicy = test.resolvedTieBreakPolicy()
        val langFailPolicy = test.resolvedLangFailRankPolicy()

        val defaultMaxMarks = if (isQualifyingOnly) {
            test.questionCount.toDouble().takeIf { it > 0.0 } ?: 30.0
        } else {
            setType.meritMaxMarks.takeIf { it > 0.0 }
                ?: (test.questionCount.toDouble() * test.positiveMarks).takeIf { it > 0.0 }
                ?: test.questionCount.toDouble().coerceAtLeast(1.0)
        }

        // Strict filter by exact testId + eligible submitted status + First Valid Attempt per userId
        val eligibleAttempts = attempts
            .filter { att ->
                att.testId == targetTestId &&
                    att.userId.isNotBlank() &&
                    att.completionStatus?.uppercase(Locale.US) !in setOf("INCOMPLETE", "CANCELLED", "DISQUALIFIED")
            }
            .sortedBy { it.submittedAt?.toDate()?.time ?: it.startedAtEpochMillis ?: Long.MAX_VALUE }
            .distinctBy { it.userId }

        if (eligibleAttempts.isEmpty()) return emptyList()

        data class RawSetCandidate(
            val userId: String,
            val studentName: String,
            val photoBase64: String?,
            val score: Double,
            val totalMarks: Double,
            val percentage: Double,
            val correctCount: Int,
            val incorrectCount: Int,
            val unattemptedCount: Int,
            val accuracy: Double,
            val timeSpentSeconds: Long,
            val submittedAtEpochMillis: Long,
            val languageScore: Double?,
            val languagePassed: Boolean?,
            val langDisqualified: Boolean,
            val attempt: TestAttempt
        )

        val candidates = eligibleAttempts.map { att ->
            val member = membersById[att.userId]
            val name = member?.displayName?.takeIf { it.isNotBlank() }
                ?: att.aspirantName.takeIf { it.isNotBlank() }
                ?: "Student"
            val photo = member?.photoBase64?.takeIf { it.isNotBlank() }
                ?: att.aspirantPhotoBase64?.takeIf { it.isNotBlank() }

            val score = if (isQualifyingOnly) att.effectiveLanguageTotal() else att.effectiveMeritScore()
            val totalMarks = if (isQualifyingOnly) {
                att.maxMarks.takeIf { it > 0.0 } ?: defaultMaxMarks
            } else {
                att.effectiveMeritMaxMarks().takeIf { it > 0.0 } ?: defaultMaxMarks
            }.coerceAtLeast(1.0)
            val pct = (score / totalMarks) * 100.0

            val correct = if (isQualifyingOnly) att.correctCount.toInt()
            else (att.meritCorrectCount ?: att.correctCount).toInt()
            val incorrect = if (isQualifyingOnly) att.incorrectCount.toInt()
            else (att.meritIncorrectCount ?: att.incorrectCount).toInt()
            val unattempted = if (isQualifyingOnly) (att.dontKnowCount + att.notAttemptedCount).toInt()
            else (att.meritUnattemptedCount ?: (att.dontKnowCount + att.notAttemptedCount)).toInt()
            val acc = computeAccuracy(correct, incorrect)
            val timeSec = att.timeSpentSeconds.coerceAtLeast(0L)
            val subMillis = att.submittedAt?.toDate()?.time ?: att.startedAtEpochMillis ?: 0L
            val langScore = if (hasLanguageSection) att.effectiveLanguageTotal() else null
            val langPass = if (hasLanguageSection) att.langPassed else null
            val langDisq = hasLanguageSection &&
                langPass == false &&
                langFailPolicy == LanguageFailRankPolicy.EXCLUDE_FROM_MERIT_RANK

            RawSetCandidate(
                userId = att.userId,
                studentName = name,
                photoBase64 = photo,
                score = score,
                totalMarks = totalMarks,
                percentage = pct,
                correctCount = correct,
                incorrectCount = incorrect,
                unattemptedCount = unattempted,
                accuracy = acc,
                timeSpentSeconds = timeSec,
                submittedAtEpochMillis = subMillis,
                languageScore = langScore,
                languagePassed = langPass,
                langDisqualified = langDisq,
                attempt = att
            )
        }.sortedWith(
            compareBy<RawSetCandidate> { it.langDisqualified }
                .thenByDescending { it.score }
                .thenByDescending { it.accuracy }
                .thenBy { it.timeSpentSeconds }
                .thenBy { if (it.submittedAtEpochMillis > 0L) it.submittedAtEpochMillis else Long.MAX_VALUE }
                .thenBy { it.studentName.lowercase(Locale.US) }
        )

        val ranked = ArrayList<SetRankEntry>(candidates.size)
        var currentRank = 0
        var prevScore: Double? = null
        var prevAcc: Double? = null
        var prevTime: Long? = null
        var prevDisq: Boolean? = null

        candidates.forEachIndexed { idx, c ->
            val pScore = prevScore
            val pAcc = prevAcc
            val pTime = prevTime
            val isTie = when (tiePolicy) {
                TieBreakPolicy.SHARED_EQUAL_RANK -> {
                    pScore != null &&
                        prevDisq == c.langDisqualified &&
                        abs(c.score - pScore) <= 0.0001
                }
                TieBreakPolicy.ACCURACY_AND_TIME -> {
                    pScore != null &&
                        prevDisq == c.langDisqualified &&
                        abs(c.score - pScore) <= 0.0001 &&
                        pAcc != null && abs(c.accuracy - pAcc) <= 0.0001 &&
                        pTime != null && c.timeSpentSeconds == pTime
                }
            }
            if (!isTie) {
                currentRank = idx + 1
                prevScore = c.score
                prevAcc = c.accuracy
                prevTime = c.timeSpentSeconds
                prevDisq = c.langDisqualified
            }

            ranked.add(
                SetRankEntry(
                    rank = currentRank,
                    userId = c.userId,
                    studentName = c.studentName,
                    photoBase64 = c.photoBase64,
                    testId = targetTestId,
                    score = c.score,
                    totalMarks = c.totalMarks,
                    percentage = c.percentage,
                    correctCount = c.correctCount,
                    incorrectCount = c.incorrectCount,
                    unattemptedCount = c.unattemptedCount,
                    accuracyPercentage = c.accuracy,
                    timeSpentSeconds = c.timeSpentSeconds,
                    submittedAtEpochMillis = c.submittedAtEpochMillis,
                    showLanguageBadge = hasLanguageSection,
                    languageScore = c.languageScore,
                    languagePassed = c.languagePassed,
                    attempt = c.attempt
                )
            )
        }
        return ranked
    }

    /**
     * Computes exact separate Subject-Wise Marks (Number), Right, Wrong, and Not Attempted counts
     * for a specific ranker ([entry]) in a specific test set ([test]), strictly mirroring [evaluate].
     * Falls back cleanly to saved [TestAttempt.subjectScores] when inline questions are absent.
     */
    fun computeSetRankerSubjectBreakdown(
        test: ExamTest,
        entry: SetRankEntry
    ): List<SetRankerSubjectStat> {
        val attempt = entry.attempt
        val questions = test.parsedQuestions()
        val positiveMarks = if (test.positiveMarks > 0.0) test.positiveMarks else 1.0
        val penaltyPerWrong = when {
            test.negativeMarkNumerator <= 0L -> 0.0
            test.negativeMarkDenominator > 0L ->
                test.negativeMarkNumerator.toDouble() / test.negativeMarkDenominator.toDouble()
            else -> 1.0 / 3.0
        }
        val penaltyPerUnattempted = if (test.unattemptedPenalty && penaltyPerWrong > 0.0) penaltyPerWrong else 0.0
        val optionEMode = test.resolvedOptionEMode()
        val setType = test.resolvedSetType()

        if (questions.isNotEmpty() && attempt.answers.isNotEmpty()) {
            data class MutableSubjectAcc(
                val subject: String,
                var isQualifying: Boolean,
                var marks: Double = 0.0,
                var maxMarks: Double = 0.0,
                var right: Int = 0,
                var wrong: Int = 0,
                var optionE: Int = 0,
                var blank: Int = 0
            )

            val bySubject = linkedMapOf<String, MutableSubjectAcc>()
            questions.forEachIndexed { index, q ->
                val subjectKey = q.subject.trim().ifBlank { "General" }
                val isLangQuestion = setType == SetType.PART_2_LANGUAGE ||
                    (setType.hasLanguageSection && (
                        q.resolvedSection() == ExamQuestion.SECTION_LANGUAGE ||
                            subjectKey.contains("Hindi", ignoreCase = true) ||
                            subjectKey.contains("हिन्दी") ||
                            subjectKey.contains("हिंदी") ||
                            subjectKey.contains("English", ignoreCase = true)
                        ))
                val acc = bySubject.getOrPut(subjectKey) {
                    MutableSubjectAcc(subject = subjectKey, isQualifying = isLangQuestion)
                }
                if (isLangQuestion) acc.isQualifying = true

                val rawChoice = attempt.answers[index.toString()]?.uppercase()?.trim()
                val correctKey = q.correctOption.uppercase().trim()

                acc.maxMarks += positiveMarks
                when {
                    correctKey.isBlank() && !rawChoice.isNullOrBlank() && rawChoice != "E" -> {
                        acc.blank++
                    }
                    rawChoice.isNullOrBlank() -> {
                        acc.blank++
                        acc.marks -= penaltyPerUnattempted
                    }
                    rawChoice == "E" && optionEMode == OptionEMode.NOT_ATTEMPTED -> {
                        acc.optionE++
                    }
                    rawChoice == correctKey -> {
                        acc.right++
                        acc.marks += positiveMarks
                    }
                    else -> {
                        acc.wrong++
                        acc.marks -= penaltyPerWrong
                    }
                }
            }

            val meritRows = bySubject.values
                .filter { !it.isQualifying }
                .map {
                    SetRankerSubjectStat(
                        subject = it.subject,
                        isQualifying = false,
                        marks = it.marks,
                        maxMarks = it.maxMarks,
                        rightCount = it.right,
                        wrongCount = it.wrong,
                        notAttemptedCount = it.optionE + it.blank,
                        optionECount = it.optionE,
                        blankCount = it.blank,
                        totalQuestions = it.right + it.wrong + it.optionE + it.blank,
                        hasDetailedCounts = true
                    )
                }
            val qualifyingRows = bySubject.values
                .filter { it.isQualifying }
                .map {
                    SetRankerSubjectStat(
                        subject = it.subject,
                        isQualifying = true,
                        marks = it.marks,
                        maxMarks = it.maxMarks,
                        rightCount = it.right,
                        wrongCount = it.wrong,
                        notAttemptedCount = it.optionE + it.blank,
                        optionECount = it.optionE,
                        blankCount = it.blank,
                        totalQuestions = it.right + it.wrong + it.optionE + it.blank,
                        hasDetailedCounts = true
                    )
                }
            return meritRows + qualifyingRows
        }

        // Fallback for legacy summary-only attempts (no inline questions or saved answers map)
        if (attempt.subjectScores.isNotEmpty()) {
            val entries = attempt.subjectScores.entries.toList()
            val singleSubject = entries.size == 1
            return entries.map { (subjectName, subjMarks) ->
                val isQual = setType == SetType.PART_2_LANGUAGE ||
                    (setType.hasLanguageSection && (
                        subjectName.contains("Hindi", ignoreCase = true) ||
                            subjectName.contains("हिन्दी") ||
                            subjectName.contains("हिंदी") ||
                            subjectName.contains("English", ignoreCase = true)
                        ))
                SetRankerSubjectStat(
                    subject = subjectName.trim().ifBlank { "General" },
                    isQualifying = isQual,
                    marks = subjMarks,
                    maxMarks = if (singleSubject) entry.totalMarks else 0.0,
                    rightCount = if (singleSubject) entry.correctCount else 0,
                    wrongCount = if (singleSubject) entry.incorrectCount else 0,
                    notAttemptedCount = if (singleSubject) entry.unattemptedCount else 0,
                    totalQuestions = if (singleSubject) {
                        entry.correctCount + entry.incorrectCount + entry.unattemptedCount
                    } else 0,
                    hasDetailedCounts = singleSubject
                )
            }.sortedBy { it.isQualifying }
        }

        // Fallback when only Hindi / English scores were recorded on a Language paper
        val fallbackRows = mutableListOf<SetRankerSubjectStat>()
        if (setType.hasLanguageSection && (attempt.hindiScore != 0.0 || attempt.englishScore != 0.0)) {
            fallbackRows += SetRankerSubjectStat(
                subject = "Hindi",
                isQualifying = true,
                marks = attempt.hindiScore,
                maxMarks = 22.0 * positiveMarks,
                rightCount = 0,
                wrongCount = 0,
                notAttemptedCount = 0,
                totalQuestions = 22,
                hasDetailedCounts = false
            )
            fallbackRows += SetRankerSubjectStat(
                subject = "English",
                isQualifying = true,
                marks = attempt.englishScore,
                maxMarks = 8.0 * positiveMarks,
                rightCount = 0,
                wrongCount = 0,
                notAttemptedCount = 0,
                totalQuestions = 8,
                hasDetailedCounts = false
            )
        }
        return fallbackRows
    }

    /**
     * Builds full genuine breakdown for any ranked User when tapped in Leaderboard / All Scores.
     */
    fun computeRankerDetailBreakdown(
        entry: RankedAspirant,
        category: RankCategory,
        period: RankingPeriod,
        attempts: List<TestAttempt>,
        tests: List<ExamTest>,
        nowMillis: Long = System.currentTimeMillis(),
        selectedTestId: String? = null
    ): RankerDetailBreakdown {
        val testsById = tests.associateBy { it.id }
        val targetTest = selectedTestId?.trim()?.takeIf { it.isNotBlank() }?.let { testsById[it] }
        val dayWindow = 24L * 60L * 60L * 1000L
        val weekWindow = 7L * dayWindow
        val monthStartMillis = IstClock.calendar(nowMillis).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val userScopedAttempts = attempts
            .filter { att ->
                att.userId == entry.member.userId && if (!selectedTestId.isNullOrBlank()) {
                    att.testId == selectedTestId
                } else {
                    RankCategory.fromAttempt(att, testsById) == category
                }
            }
            .sortedBy { it.submittedAt?.toDate()?.time ?: Long.MAX_VALUE }
            .distinctBy { it.testId }
            .filter {
                val ts = it.submittedAt?.toDate()?.time ?: nowMillis
                when (period) {
                    RankingPeriod.INDIVIDUAL_TEST, RankingPeriod.OVERALL -> true
                    RankingPeriod.DAILY -> nowMillis - ts <= dayWindow
                    RankingPeriod.WEEKLY -> nowMillis - ts <= weekWindow
                    RankingPeriod.MONTHLY -> ts >= monthStartMillis
                }
            }

        val sectionAcc = linkedMapOf<String, Triple<Double, Double, Triple<Int, Int, Int>>>()
        val subjectCorrect = entry.subjectCorrectCounts.toMutableMap()
        val subjectTotals = entry.subjectTotalCounts.toMutableMap()

        if (userScopedAttempts.isNotEmpty()) {
            userScopedAttempts.forEach { att ->
                val testObj = testsById[att.testId]
                if (testObj != null && testObj.questions.isNotEmpty()) {
                    val parsedQs = testObj.parsedQuestions()
                    val posMarks = testObj.positiveMarks.takeIf { it > 0.0 } ?: 1.0
                    val negMarks = if (testObj.negativeMarkDenominator > 0L) {
                        testObj.negativeMarkNumerator.toDouble() / testObj.negativeMarkDenominator.toDouble()
                    } else 0.0
                    val optMode = testObj.resolvedOptionEMode()

                    parsedQs.forEachIndexed { idx, q ->
                        val secName = when (q.resolvedSection()) {
                            ExamQuestion.SECTION_LANGUAGE -> "Language (Qualifying)"
                            ExamQuestion.SECTION_SUBJECT -> "Core Subject"
                            else -> "Common / GS"
                        }
                        val ans = att.answers[idx.toString()]?.uppercase()?.trim()
                        val key = q.correctOption.uppercase().trim()
                        val isUnatt = ans.isNullOrBlank() || (ans == "E" && optMode == OptionEMode.NOT_ATTEMPTED)
                        val isRight = !isUnatt && key.isNotBlank() && ans == key
                        val delta = when {
                            isUnatt -> 0.0
                            isRight -> posMarks
                            else -> -negMarks
                        }
                        val prev = sectionAcc[secName] ?: Triple(0.0, 0.0, Triple(0, 0, 0))
                        val prevCounts = prev.third
                        sectionAcc[secName] = Triple(
                            prev.first + delta,
                            prev.second + posMarks,
                            Triple(
                                prevCounts.first + if (isRight) 1 else 0,
                                prevCounts.second + if (!isUnatt && !isRight) 1 else 0,
                                prevCounts.third + if (isUnatt) 1 else 0
                            )
                        )
                    }
                } else {
                    // Attempt stored without inline test questions — construct section stats cleanly from saved fields
                    if (category.hasLanguageSection) {
                        val prev = sectionAcc["Language (Qualifying)"] ?: Triple(0.0, 0.0, Triple(0, 0, 0))
                        sectionAcc["Language (Qualifying)"] = Triple(
                            prev.first + att.effectiveLanguageTotal(),
                            prev.second + 30.0,
                            prev.third
                        )
                    }
                    if (!category.isQualifyingOnly) {
                        val secTitle = if (category == RankCategory.COMMON || category == RankCategory.LANGUAGE_COMMON) {
                            "Common / GS"
                        } else {
                            category.label
                        }
                        val prev = sectionAcc[secTitle] ?: Triple(0.0, 0.0, Triple(0, 0, 0))
                        sectionAcc[secTitle] = Triple(
                            prev.first + att.effectiveMeritScore(),
                            prev.second + att.effectiveMeritMaxMarks(),
                            Triple(
                                prev.third.first + (att.meritCorrectCount ?: att.correctCount).toInt(),
                                prev.third.second + (att.meritIncorrectCount ?: att.incorrectCount).toInt(),
                                prev.third.third + (att.meritUnattemptedCount ?: (att.dontKnowCount + att.notAttemptedCount)).toInt()
                            )
                        )
                    }
                }
            }
        }

        val allSubjects = (entry.subjectScores.keys + subjectCorrect.keys + subjectTotals.keys).distinct()
        val subjectStats = allSubjects.map { subj ->
            RankerSubjectStat(
                subject = subj,
                marks = entry.subjectScores[subj] ?: 0.0,
                correctCount = subjectCorrect[subj] ?: 0,
                totalQuestions = subjectTotals[subj] ?: 0
            )
        }.sortedByDescending { it.marks }

        val sectionStats = sectionAcc.map { (name, data) ->
            RankerSectionStat(
                sectionName = name,
                marks = data.first,
                maxMarks = data.second,
                correctCount = data.third.first,
                incorrectCount = data.third.second,
                unattemptedCount = data.third.third
            )
        }

        val pct = if (entry.testsAttempted > 0 && entry.maxMarks > 0.0) {
            (entry.displayScore / entry.maxMarks) * 100.0
        } else 0.0

        val resolvedPeriodLabel = if (targetTest != null) {
            val sNum = targetTest.resolvedSetNumber()
            if (sNum != null) "Set $sNum (${targetTest.difficulty})" else "${targetTest.title} (${targetTest.difficulty})"
        } else if (period == RankingPeriod.INDIVIDUAL_TEST) {
            "Set"
        } else {
            period.label
        }

        return RankerDetailBreakdown(
            userId = entry.member.userId,
            userName = entry.member.displayName,
            rank = entry.rank,
            categoryLabel = category.label,
            periodLabel = resolvedPeriodLabel,
            totalScore = entry.displayScore,
            maxMarks = entry.maxMarks,
            percentage = pct,
            correctCount = entry.correctCount,
            incorrectCount = entry.incorrectCount,
            unattemptedCount = entry.unattemptedCount,
            timeSpentSeconds = entry.timeSpentSeconds,
            testsAttempted = entry.testsAttempted,
            attemptStatusLabel = entry.attemptStatusLabel.ifBlank {
                if (entry.testsAttempted > 0) "Completed" else "Not Attempted"
            },
            subjectStats = subjectStats,
            sectionStats = sectionStats
        )
    }
}
