package com.example

import com.example.data.model.AdminContactProfile
import com.example.data.model.AppUpdateRelease
import com.example.data.model.ContactAdminPolicy
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamTest
import com.example.data.model.GroupMember
import com.example.data.model.LanguageFailRankPolicy
import com.example.data.model.OptionEMode
import com.example.data.model.PaperType
import com.example.data.model.RankCategory
import com.example.data.model.RankingPeriod
import com.example.data.model.SetType
import com.example.data.model.TestAttempt
import com.example.data.model.TestSetNaming
import com.example.data.model.TieBreakPolicy
import com.example.domain.BulkQuestionParser
import com.example.domain.ExamScoringEngine
import com.example.domain.OfficialStarterBank
import com.example.util.RankingCsvExporter
import com.example.util.SetIconOption
import com.example.util.WhatsAppHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamEngineAndScoringTest {

    private fun sampleQuestions(count: Int = 6): List<ExamQuestion> {
        val subjects = listOf("Mathematics", "Science", "Indian National Movement (1857–1947)", "Indian Geography", "Current Affairs (PIB)")
        return (0 until count).map { idx ->
            ExamQuestion(
                number = idx + 1,
                subject = subjects[idx % subjects.size],
                text = "Sample Question ${idx + 1}",
                optionA = "Opt A",
                optionB = "Opt B",
                optionC = "Opt C",
                optionD = "Opt D",
                optionE = ExamQuestion.DEFAULT_OPTION_E,
                correctOption = listOf("A", "B", "C", "D")[idx % 4],
                explanation = "Official verified explanation ${idx + 1}"
            )
        }
    }

    private fun defaultTest(paperType: PaperType = PaperType.COMMON_GS): ExamTest = ExamTest(
        id = "test_unit",
        title = "Unit Test Paper",
        paperType = paperType.name,
        negativeMarkNumerator = 1L,
        negativeMarkDenominator = 3L,
        unattemptedPenalty = true
    )

    @Test
    fun `exact 5-option scoring rules and separate Option E vs blank storage`() {
        val questions = sampleQuestions(6)
        // Q0 (correct A) -> A (+1)
        // Q1 (correct B) -> B (+1)
        // Q2 (correct C) -> A (-1/3)
        // Q3 (correct D) -> E (0)
        // Q4 (correct A) -> blank (-1/3)
        // Q5 (correct B) -> blank (-1/3)
        val answers = mapOf(
            0 to "A",
            1 to "B",
            2 to "A",
            3 to "E"
        )

        val result = ExamScoringEngine.evaluate(
            test = defaultTest(PaperType.COMMON_GS),
            questions = questions,
            selectedAnswers = answers
        )

        assertEquals(2, result.correctCount)
        assertEquals(1, result.incorrectCount)
        assertEquals(1, result.dontKnowCount)
        assertEquals(2, result.notAttemptedCount)

        // Exact fractional math: 2*(+1) + 1*(-1/3) + 1*(0) + 2*(-1/3) = 2 - 1 = 1.0 exactly (no rounding drift!)
        assertEquals(1.0, result.totalScore, 1e-9)
        assertEquals(1.0, result.negativeDeducted, 1e-9)
        assertEquals(6.0, result.maxMarks, 1e-9)

        // Accuracy strictly excludes Option E and completely blank: 2 / (2 + 1) * 100 = 66.666666...
        assertEquals(200.0 / 3.0, result.accuracyPercentage, 1e-9)
    }

    @Test
    fun `fractional scoring precision without intermediate rounding`() {
        val questions = sampleQuestions(3)
        // 1 wrong, 1 Opt E, 1 blank => score = -2/3 = -0.666666666...
        val result = ExamScoringEngine.evaluate(
            test = defaultTest(PaperType.COMMON_GS),
            questions = questions,
            selectedAnswers = mapOf(0 to "D", 1 to "E")
        )

        assertEquals(0, result.correctCount)
        assertEquals(1, result.incorrectCount)
        assertEquals(1, result.dontKnowCount)
        assertEquals(1, result.notAttemptedCount)
        assertEquals(-2.0 / 3.0, result.totalScore, 1e-9)
        assertEquals(2.0 / 3.0, result.negativeDeducted, 1e-9)
        assertEquals(0.0, result.accuracyPercentage, 1e-9)
    }

    @Test
    fun `changing and clearing answers updates score cleanly`() {
        val questions = sampleQuestions(2) // Q0=A, Q1=B
        val test = defaultTest(PaperType.COMMON_GS)
        val initial = mutableMapOf(0 to "A", 1 to "B")
        val r1 = ExamScoringEngine.evaluate(test, questions, initial)
        assertEquals(2.0, r1.totalScore, 1e-9)
        assertEquals(100.0, r1.accuracyPercentage, 1e-9)

        // Change Q1 to Option E -> 0 penalty
        initial[1] = "E"
        val r2 = ExamScoringEngine.evaluate(test, questions, initial)
        assertEquals(1.0, r2.totalScore, 1e-9)
        assertEquals(1, r2.dontKnowCount)
        assertEquals(0, r2.notAttemptedCount)
        assertEquals(100.0, r2.accuracyPercentage, 1e-9)

        // Clear Q1 -> completely blank -> -1/3 penalty
        initial.remove(1)
        val r3 = ExamScoringEngine.evaluate(test, questions, initial)
        assertEquals(2.0 / 3.0, r3.totalScore, 1e-9)
        assertEquals(0, r3.dontKnowCount)
        assertEquals(1, r3.notAttemptedCount)
        assertEquals(100.0, r3.accuracyPercentage, 1e-9)
    }

    @Test
    fun `accuracy returns zero when only Option E and blank responses exist`() {
        assertEquals(0.0, ExamScoringEngine.computeAccuracy(0, 0), 1e-9)
        assertEquals(75.0, ExamScoringEngine.computeAccuracy(3, 1), 1e-9)
        assertEquals(100.0, ExamScoringEngine.computeAccuracy(10, 0), 1e-9)
        assertEquals(0.0, ExamScoringEngine.computeAccuracy(0, 5), 1e-9)
    }

    @Test
    fun `language qualifying threshold pass at 9 and fail below 9`() {
        val langQuestions = OfficialStarterBank.language30Questions()
        val langTest = defaultTest(PaperType.LANGUAGE)

        // Answer 9 Hindi correctly, 21 Option E => exact 9.0 -> PASS
        val passAnswers = mutableMapOf<Int, String>()
        langQuestions.forEachIndexed { idx, q ->
            passAnswers[idx] = if (idx < 9) q.correctOption else "E"
        }
        val passEval = ExamScoringEngine.evaluate(langTest, langQuestions, passAnswers)
        assertEquals(9.0, passEval.totalScore, 1e-9)
        assertTrue(passEval.langPassed)

        // Answer 9 Hindi correctly, 1 wrong, 20 Option E => 8.666... -> FAIL
        val failAnswers = passAnswers.toMutableMap()
        failAnswers[9] = if (langQuestions[9].correctOption == "A") "B" else "A"
        val failEval = ExamScoringEngine.evaluate(langTest, langQuestions, failAnswers)
        assertEquals(9.0 - 1.0 / 3.0, failEval.totalScore, 1e-9)
        assertFalse(failEval.langPassed)
    }

    @Test
    fun `combined 70Q paper evaluates GS and Language qualifying pass independently`() {
        val combQuestions = OfficialStarterBank.combined70Questions()
        val combTest = defaultTest(PaperType.COMBINED)
        // Answer all 40 GS correctly + 8 English (indices 40..47) + 2 Hindi (indices 48..49), remaining 20 Option E
        val answers = mutableMapOf<Int, String>()
        combQuestions.forEachIndexed { idx, q ->
            answers[idx] = if (idx < 50) q.correctOption else "E"
        }
        val eval = ExamScoringEngine.evaluate(combTest, combQuestions, answers)
        // Strict Language Exclusion: Merit score is ONLY out of 40 Common marks (never includes 10 Language marks!)
        assertEquals(40.0, eval.totalScore, 1e-9)
        assertEquals(40.0, eval.meritMarks, 1e-9)
        assertEquals(40.0, eval.totalMeritMarks, 1e-9)
        assertEquals(10.0, eval.languageTotalMarks, 1e-9)
        assertEquals(8.0, eval.englishScore, 1e-9)
        assertEquals(2.0, eval.hindiScore, 1e-9)
        assertTrue(eval.langPassed)
    }

    @Test
    fun `competition ranking tie breaking 1-2-2-4 and excludes language scores`() {
        val m1 = GroupMember(userId = "u1", displayName = "Ananya", gsAverageScore = 32.0, gsTestsAttempted = 1, gsHighestScore = 32.0)
        val m2 = GroupMember(userId = "u2", displayName = "Bharat", gsAverageScore = 28.0, gsTestsAttempted = 1, gsHighestScore = 28.0)
        val m3 = GroupMember(userId = "u3", displayName = "Chetan", gsAverageScore = 28.0, gsTestsAttempted = 1, gsHighestScore = 28.0)
        val m4 = GroupMember(userId = "u4", displayName = "Deepak", gsAverageScore = 22.0, gsTestsAttempted = 1, gsHighestScore = 22.0)

        val gsTest = ExamTest(id = "t_gs", paperType = PaperType.COMMON_GS.name, status = "PUBLISHED")
        val langTest = ExamTest(id = "t_lang", paperType = PaperType.LANGUAGE.name, status = "PUBLISHED")

        val attempts = listOf(
            TestAttempt(id = "a1", testId = "t_gs", paperType = PaperType.COMMON_GS.name, userId = "u1", totalScore = 32.0),
            TestAttempt(id = "a2", testId = "t_gs", paperType = PaperType.COMMON_GS.name, userId = "u2", totalScore = 28.0),
            TestAttempt(id = "a3", testId = "t_gs", paperType = PaperType.COMMON_GS.name, userId = "u3", totalScore = 28.0),
            TestAttempt(id = "a4", testId = "t_gs", paperType = PaperType.COMMON_GS.name, userId = "u4", totalScore = 22.0),
            // Deepak scores 30/30 in Language — MUST NOT inflate GS rank!
            TestAttempt(id = "a5", testId = "t_lang", paperType = PaperType.LANGUAGE.name, userId = "u4", totalScore = 30.0)
        )

        val ranked = ExamScoringEngine.computeRankings(
            period = RankingPeriod.OVERALL,
            members = listOf(m4, m3, m1, m2),
            attempts = attempts,
            tests = listOf(gsTest, langTest)
        )

        assertEquals(4, ranked.size)
        assertEquals(1, ranked[0].rank)
        assertEquals("u1", ranked[0].member.userId)
        assertEquals(2, ranked[1].rank)
        assertEquals(2, ranked[2].rank)
        assertEquals(4, ranked[3].rank)
        assertEquals("u4", ranked[3].member.userId)
    }

    @Test
    fun `scheduled solution release security blocks premature access for aspirants`() {
        val now = 1_700_000_000_000L
        val immediateTest = ExamTest(answerReleaseMode = "IMMEDIATE")
        assertTrue(ExamScoringEngine.areSolutionsReleased(immediateTest, isAdmin = false, nowMillis = now))

        val futureScheduled = ExamTest(
            answerReleaseMode = "SCHEDULED",
            scheduledAt = (now + 3_600_000L).toString()
        )
        assertFalse(ExamScoringEngine.areSolutionsReleased(futureScheduled, isAdmin = false, nowMillis = now))
        assertTrue(ExamScoringEngine.areSolutionsReleased(futureScheduled, isAdmin = true, nowMillis = now))

        val pastScheduled = ExamTest(
            answerReleaseMode = "SCHEDULED",
            scheduledAt = (now - 60_000L).toString()
        )
        assertTrue(ExamScoringEngine.areSolutionsReleased(pastScheduled, isAdmin = false, nowMillis = now))
    }

    @Test
    fun `bulk parser extracts Answer B without colon accurately and validates all official banks`() {
        val bulk = """
            1. What is the chemical symbol of Gold?
            A) Ag
            B) Au
            C) Gd
            D) Go
            E) Don't Know / पता नहीं
            Answer B
            Subject: General Science
            Explanation: Au comes from Latin Aurum.

            2. Which state launched the Kanya Utthan Yojana?
            A) Uttar Pradesh
            B) Jharkhand
            C) Bihar
            D) Odisha
            Answer: C
            Subject: Current Affairs
            Explanation: Bihar Government flagship scheme.
        """.trimIndent()

        val parsed = BulkQuestionParser.parseBulkText(bulk, PaperType.PRACTICE)
        assertEquals(2, parsed.size)
        assertEquals("B", parsed[0].correctOption)
        assertEquals("C", parsed[1].correctOption)
        assertEquals("Science", parsed[0].subject)
        assertEquals("Current Affairs", parsed[1].subject)
        // Legacy Option E text is automatically normalized to ExamQuestion.DEFAULT_OPTION_E
        assertEquals(ExamQuestion.DEFAULT_OPTION_E, parsed[0].optionE)
        assertEquals(ExamQuestion.DEFAULT_OPTION_E, parsed[1].optionE)

        // Verify all 3 official starter banks pass strict validation with 0 errors
        val gs40 = OfficialStarterBank.commonGs40Questions()
        val gsVal = BulkQuestionParser.validate(gs40, PaperType.COMMON_GS)
        assertTrue("GS 40-Q errors: ${gsVal.errors}", gsVal.valid)
        assertEquals(40, gs40.size)

        val lang30 = OfficialStarterBank.language30Questions()
        val langVal = BulkQuestionParser.validate(lang30, PaperType.LANGUAGE)
        assertTrue("Lang 30-Q errors: ${langVal.errors}", langVal.valid)
        assertEquals(30, lang30.size)

        val comb70 = OfficialStarterBank.combined70Questions()
        val combVal = BulkQuestionParser.validate(comb70, PaperType.COMBINED)
        assertTrue("Combined 70-Q errors: ${combVal.errors}", combVal.valid)
        assertEquals(70, comb70.size)
    }

    @Test
    fun `CSV importer parses valid 5-option MCQs`() {
        val csvList = BulkQuestionParser.parseCsv(BulkQuestionParser.CSV_TEMPLATE, PaperType.CLASS_6_8_MATH_SCIENCE)
        assertEquals(3, csvList.size)
        assertEquals("English", csvList[0].subject)
        assertEquals(ExamQuestion.SECTION_LANGUAGE, csvList[0].section)
        assertEquals("Geography", csvList[1].subject)
        assertEquals(ExamQuestion.SECTION_COMMON, csvList[1].section)
        assertEquals("Kosi", csvList[1].optionB)
        assertEquals("B", csvList[1].correctOption)
        assertEquals(ExamQuestion.DEFAULT_OPTION_E, csvList[1].optionE)
        assertEquals("Mathematics", csvList[2].subject)
        assertEquals(ExamQuestion.SECTION_SUBJECT, csvList[2].section)
    }

    @Test
    fun `official PaperType durations and question counts match all 5 TRE 4_0 formats`() {
        assertEquals(15, PaperType.LANGUAGE.defaultDurationMinutes)
        assertEquals(30, PaperType.LANGUAGE.expectedQuestionCount)

        assertEquals(30, PaperType.COMMON_GS.defaultDurationMinutes)
        assertEquals(40, PaperType.COMMON_GS.expectedQuestionCount)

        assertEquals(45, PaperType.COMBINED.defaultDurationMinutes)
        assertEquals(70, PaperType.COMBINED.expectedQuestionCount)

        assertEquals(120, PaperType.CLASS_6_8_MATH_SCIENCE.defaultDurationMinutes)
        assertEquals(150, PaperType.CLASS_6_8_MATH_SCIENCE.expectedQuestionCount)

        assertEquals(135, PaperType.CLASS_9_10_MATH.defaultDurationMinutes)
        assertEquals(150, PaperType.CLASS_9_10_MATH.expectedQuestionCount)
    }

    @Test
    fun `Language 30Q blueprint enforces 8 English (1 passage x 4Q) and 22 Hindi (2 passages x 4Q + 14 Grammar)`() {
        val lang30 = OfficialStarterBank.language30Questions()
        assertEquals(30, lang30.size)

        val english = lang30.filter { it.subject == "English" }
        val hindi = lang30.filter { it.subject == "Hindi" }
        assertEquals(8, english.size)
        assertEquals(22, hindi.size)

        val englishPassages = english.filter { it.passage.isNotBlank() }
        assertEquals(4, englishPassages.size)
        assertEquals(1, englishPassages.map { it.passage }.distinct().size)

        val hindiPassages = hindi.filter { it.passage.isNotBlank() }
        assertEquals(8, hindiPassages.size)
        assertEquals(2, hindiPassages.map { it.passage }.distinct().size)

        val hindiGrammar = hindi.filter { it.passage.isBlank() }
        assertEquals(14, hindiGrammar.size)

        assertTrue(lang30.all { it.optionE == ExamQuestion.DEFAULT_OPTION_E })
    }

    @Test
    fun `TestSetNaming generates unique auto-incrementing Set 1, Set 2, Set 3 names and detects duplicates`() {
        val existing = mutableListOf<ExamTest>()

        val firstCommon = TestSetNaming.nextUniqueTitle(PaperType.COMMON_GS, existing)
        assertEquals("Common Paper Set 1", firstCommon)
        existing.add(ExamTest(id = "t1", title = firstCommon, paperType = PaperType.COMMON_GS.name))

        val secondCommon = TestSetNaming.nextUniqueTitle(PaperType.COMMON_GS, existing)
        assertEquals("Common Paper Set 2", secondCommon)
        existing.add(ExamTest(id = "t2", title = secondCommon, paperType = PaperType.COMMON_GS.name))

        val thirdCommon = TestSetNaming.nextUniqueTitle(PaperType.COMMON_GS, existing)
        assertEquals("Common Paper Set 3", thirdCommon)

        // Independent numbering for 150-Q formats
        assertEquals("Class 6 to 8 Mathematics and Science Set 1", TestSetNaming.nextUniqueTitle(PaperType.CLASS_6_8_MATH_SCIENCE, existing))
        assertEquals("Class 9-10 Mathematics Set 1", TestSetNaming.nextUniqueTitle(PaperType.CLASS_9_10_MATH, existing))
        assertEquals("Language Paper Set 1", TestSetNaming.nextUniqueTitle(PaperType.LANGUAGE, existing))

        assertTrue(TestSetNaming.isDuplicateTitle("  common paper set 1 ", existing))
        assertFalse(TestSetNaming.isDuplicateTitle("Common Paper Set 1", existing, excludeTestId = "t1"))
        assertFalse(TestSetNaming.isDuplicateTitle("Common Paper Set 3", existing))
    }

    @Test
    fun `combineFull150Sections validates Class 6-8 Math & Science (120m) and Class 9-10 Math (135m) with exact scoring`() {
        val lang30 = OfficialStarterBank.language30Questions()
        val common40 = OfficialStarterBank.commonGs40Questions()

        val math40 = (1..40).map { i ->
            ExamQuestion(
                number = i,
                subject = "Mathematics",
                topic = "Class 6-8 SCERT/NCERT Math",
                source = "Bihar SCERT + NCERT Class 6-8",
                text = "Class 6-8 Math Subject Question #$i: Solve x + $i = ${i + 10}",
                optionA = "10", optionB = "11", optionC = "12", optionD = "9",
                optionE = ExamQuestion.DEFAULT_OPTION_E,
                correctOption = "A",
                explanation = "x = 10",
                section = ExamQuestion.SECTION_SUBJECT
            )
        }
        val sci40 = (1..40).map { i ->
            ExamQuestion(
                number = 40 + i,
                subject = "Science",
                topic = "Class 6-8 SCERT/NCERT Science",
                source = "Bihar SCERT + NCERT Class 6-8",
                text = "Class 6-8 Science Subject Question #$i: Property of matter #$i",
                optionA = "Opt A", optionB = "Opt B", optionC = "Opt C", optionD = "Opt D",
                optionE = ExamQuestion.DEFAULT_OPTION_E,
                correctOption = "B",
                explanation = "Verified NCERT Class 6-8 concept.",
                section = ExamQuestion.SECTION_SUBJECT
            )
        }

        val full68 = BulkQuestionParser.combineFull150Sections(
            languageQuestions = lang30,
            commonQuestions = common40,
            subjectQuestions = math40 + sci40,
            targetType = PaperType.CLASS_6_8_MATH_SCIENCE
        )
        assertTrue("Class 6-8 150Q errors: ${full68.errors}", full68.valid)
        assertEquals(150, full68.questions.size)
        assertEquals(30, full68.languageCount)
        assertEquals(40, full68.commonCount)
        assertEquals(80, full68.subjectCount)

        // Score 150-Q paper: 10 Language right + 20 Language Opt E, 40 Common right, 60 Subject right + 3 Subject wrong + 17 Subject Opt E
        val answers68 = mutableMapOf<Int, String>()
        full68.questions.forEachIndexed { idx, q ->
            answers68[idx] = when {
                idx < 10 -> q.correctOption // 10 Lang correct (+10, PASS >= 9)
                idx < 30 -> "E" // 20 Lang Not Attempted (0)
                idx < 70 -> q.correctOption // 40 Common correct (+40)
                idx < 130 -> q.correctOption // 60 Subject correct (+60)
                idx < 133 -> if (q.correctOption == "A") "B" else "A" // 3 Subject wrong (-1.0)
                else -> "E" // 17 Subject Not Attempted (0)
            }
        }
        val eval68 = ExamScoringEngine.evaluate(
            test = defaultTest(PaperType.CLASS_6_8_MATH_SCIENCE),
            questions = full68.questions,
            selectedAnswers = answers68
        )
        assertEquals(110, eval68.correctCount)
        assertEquals(3, eval68.incorrectCount)
        assertEquals(37, eval68.dontKnowCount)
        assertEquals(0, eval68.notAttemptedCount)
        // Strict Language Exclusion: Merit score = 40 Common + 60 Subject - 3*(1/3) = 99.0 / 120.0 (Language 10/30 tracked separately!)
        assertEquals(99.0, eval68.totalScore, 1e-9)
        assertEquals(99.0, eval68.meritMarks, 1e-9)
        assertEquals(120.0, eval68.totalMeritMarks, 1e-9)
        assertEquals(10.0, eval68.languageTotalMarks, 1e-9)
        assertTrue(eval68.langPassed)

        // Class 9-10 Mathematics (80 Mathematics questions)
        val math80 = (1..80).map { i ->
            ExamQuestion(
                number = i,
                subject = "Mathematics",
                topic = "Class 6-10 Bihar Board SCERT/NCERT Math",
                source = "Bihar Board SCERT + NCERT Class 6-10",
                text = "Class 9-10 Hard Math Problem #$i: Find roots of x^2 - ${2 * i}x + ${i * i} = 0",
                optionA = "$i", optionB = "${i + 1}", optionC = "${i + 2}", optionD = "${i + 3}",
                optionE = ExamQuestion.DEFAULT_OPTION_E,
                correctOption = "A",
                explanation = "(x - $i)^2 = 0 => x = $i",
                section = ExamQuestion.SECTION_SUBJECT
            )
        }
        val full910 = BulkQuestionParser.combineFull150Sections(
            languageQuestions = lang30,
            commonQuestions = common40,
            subjectQuestions = math80,
            targetType = PaperType.CLASS_9_10_MATH
        )
        assertTrue("Class 9-10 150Q errors: ${full910.errors}", full910.valid)
        assertEquals(150, full910.questions.size)
        assertEquals(80, full910.subjectCount)
    }

    @Test
    fun `combineSections strictly separates 40 Common and 30 Language without mixing answer keys`() {
        val gs40 = OfficialStarterBank.commonGs40Questions()
        val lang30 = OfficialStarterBank.language30Questions()

        val assembled = BulkQuestionParser.combineSections(gs40, lang30)
        assertTrue("Expected valid Combined set, got errors: ${assembled.errors}", assembled.valid)
        assertEquals(70, assembled.questions.size)
        assertEquals(40, assembled.commonCount)
        assertEquals(30, assembled.languageCount)

        // Verify Q1..Q40 are strictly COMMON and preserve every single answer key
        for (i in 0 until 40) {
            assertEquals(i + 1, assembled.questions[i].number)
            assertEquals(ExamQuestion.SECTION_COMMON, assembled.questions[i].resolvedSection())
            assertEquals(gs40[i].correctOption, assembled.questions[i].correctOption)
            assertEquals(gs40[i].text, assembled.questions[i].text)
        }
        // Verify Q41..Q70 are strictly LANGUAGE and preserve every single answer key
        for (i in 0 until 30) {
            val q = assembled.questions[40 + i]
            assertEquals(41 + i, q.number)
            assertEquals(ExamQuestion.SECTION_LANGUAGE, q.resolvedSection())
            assertEquals(lang30[i].correctOption, q.correctOption)
            assertEquals(lang30[i].text, q.text)
        }

        // Reject mixed sections (putting a Language question in Section A)
        val mixedCommon = gs40.dropLast(1) + lang30.first()
        val rejected = BulkQuestionParser.combineSections(mixedCommon, lang30)
        assertFalse(rejected.valid)
        assertTrue(rejected.errors.any { it.contains("Section A", ignoreCase = true) || it.contains("mixed", ignoreCase = true) })
    }

    @Test
    fun `DocumentQuestionImporter extracts and parses MCQs from real DOCX ZIP archive`() {
        val baos = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("word/document.xml"))
            val xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:r><w:t>1. बिहार की राजधानी क्या है?</w:t></w:r></w:p>
                    <w:p><w:r><w:t>A) गया</w:t></w:r></w:p>
                    <w:p><w:r><w:t>B) पटना</w:t></w:r></w:p>
                    <w:p><w:r><w:t>C) भागलपुर</w:t></w:r></w:p>
                    <w:p><w:r><w:t>D) मुजफ्फरपुर</w:t></w:r></w:p>
                    <w:p><w:r><w:t>Answer: B</w:t></w:r></w:p>
                    <w:p><w:r><w:t>Subject: Geography</w:t></w:r></w:p>
                    <w:p><w:r><w:t>Explanation: पटना बिहार की राजधानी है।</w:t></w:r></w:p>
                  </w:body>
                </w:document>
            """.trimIndent()
            zos.write(xml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val outcome = com.example.domain.DocumentQuestionImporter.importFromBytes(
            bytes = baos.toByteArray(),
            fileName = "bihar_set.docx",
            format = com.example.domain.ImportFileFormat.DOCX,
            targetPaperType = PaperType.COMMON_GS
        )
        assertEquals(null, outcome.errorMessage)
        assertEquals(1, outcome.questions.size)
        assertEquals("B", outcome.questions[0].correctOption)
        assertEquals("पटना", outcome.questions[0].optionB)
        assertEquals("Geography", outcome.questions[0].subject)
    }

    @Test
    fun `DocumentQuestionImporter extracts and parses MCQs from raw and FlateDecode PDF streams`() {
        val contentStream = """
            BT
            /F1 12 Tf
            72 720 Td
            (1. SI unit of Force is?) Tj
            0 -16 Td
            (A\) Joule) Tj
            0 -16 Td
            (B\) Newton) Tj
            0 -16 Td
            (C\) Watt) Tj
            0 -16 Td
            (D\) Pascal) Tj
            0 -16 Td
            [(Answer: ) -200 (B)] TJ
            0 -16 Td
            (Subject: General Science) Tj
            ET
        """.trimIndent().toByteArray(Charsets.ISO_8859_1)

        // Compress using Deflate (FlateDecode)
        val deflater = java.util.zip.Deflater()
        deflater.setInput(contentStream)
        deflater.finish()
        val compressedOut = java.io.ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            compressedOut.write(buf, 0, n)
        }
        deflater.end()
        val compressed = compressedOut.toByteArray()

        val pdfBytes = java.io.ByteArrayOutputStream().apply {
            write("%PDF-1.4\n1 0 obj\n<< /Length ${compressed.size} /Filter /FlateDecode >>\nstream\n".toByteArray(Charsets.ISO_8859_1))
            write(compressed)
            write("\nendstream\nendobj\n%%EOF".toByteArray(Charsets.ISO_8859_1))
        }.toByteArray()

        val outcome = com.example.domain.DocumentQuestionImporter.importFromBytes(
            bytes = pdfBytes,
            fileName = "science_mock.pdf",
            format = com.example.domain.ImportFileFormat.PDF,
            targetPaperType = PaperType.COMMON_GS
        )
        assertEquals(null, outcome.errorMessage)
        assertEquals(1, outcome.questions.size)
        assertEquals("B", outcome.questions[0].correctOption)
        assertEquals("Newton", outcome.questions[0].optionB)
        assertEquals("Science", outcome.questions[0].subject)
    }

    @Test
    fun `DocumentQuestionImporter extracts and parses MCQs from RTF DOC and CSV bytes`() {
        val rtfDoc = """
            {\rtf1\ansi\deff0
            1. What is 12 x 12?\par
            A) 124\par
            B) 144\par
            C) 154\par
            D) 164\par
            Answer: B\par
            Subject: Mathematics\par
            }
        """.trimIndent().toByteArray(Charsets.ISO_8859_1)

        val docOutcome = com.example.domain.DocumentQuestionImporter.importFromBytes(
            bytes = rtfDoc,
            fileName = "math_paper.doc",
            format = com.example.domain.ImportFileFormat.DOC,
            targetPaperType = PaperType.COMMON_GS
        )
        assertEquals(null, docOutcome.errorMessage)
        assertEquals(1, docOutcome.questions.size)
        assertEquals("B", docOutcome.questions[0].correctOption)
        assertEquals("144", docOutcome.questions[0].optionB)

        val csvOutcome = com.example.domain.DocumentQuestionImporter.importFromBytes(
            bytes = BulkQuestionParser.CSV_TEMPLATE.toByteArray(Charsets.UTF_8),
            fileName = "geo.csv",
            format = com.example.domain.ImportFileFormat.CSV,
            targetPaperType = PaperType.CLASS_6_8_MATH_SCIENCE
        )
        assertEquals(null, csvOutcome.errorMessage)
        assertEquals(3, csvOutcome.questions.size)
        assertEquals("B", csvOutcome.questions[1].correctOption)
    }

    @Test
    fun `per-set ranking hides Language badge for Common GS and shows it for Combined and Language sets`() {
        val m1 = GroupMember(userId = "u1", displayName = "Ananya", langStatus = "PASS", langScore = 22.0)
        val m2 = GroupMember(userId = "u2", displayName = "Rohit", langStatus = "FAIL", langScore = 6.0)

        val commonTest = ExamTest(
            id = "set_common_1",
            title = "Common Paper Set 1",
            paperType = PaperType.COMMON_GS.name,
            questionCount = 40,
            status = "PUBLISHED"
        )
        val combinedTest = ExamTest(
            id = "set_combined_1",
            title = "Language + Common Paper Set 1",
            paperType = PaperType.COMBINED.name,
            questionCount = 70,
            status = "PUBLISHED"
        )
        val langTest = ExamTest(
            id = "set_lang_1",
            title = "Language Paper Set 1",
            paperType = PaperType.LANGUAGE.name,
            questionCount = 30,
            status = "PUBLISHED"
        )

        val attempts = listOf(
            TestAttempt(id = "a1", testId = "set_common_1", paperType = PaperType.COMMON_GS.name, userId = "u1", totalScore = 34.0, maxMarks = 40.0),
            TestAttempt(id = "a2", testId = "set_common_1", paperType = PaperType.COMMON_GS.name, userId = "u2", totalScore = 29.0, maxMarks = 40.0),
            TestAttempt(id = "a3", testId = "set_combined_1", paperType = PaperType.COMBINED.name, userId = "u1", totalScore = 58.0, maxMarks = 70.0, hindiScore = 16.0, englishScore = 6.0, langPassed = true),
            TestAttempt(id = "a4", testId = "set_combined_1", paperType = PaperType.COMBINED.name, userId = "u2", totalScore = 41.0, maxMarks = 70.0, hindiScore = 5.0, englishScore = 2.0, langPassed = false),
            TestAttempt(id = "a5", testId = "set_lang_1", paperType = PaperType.LANGUAGE.name, userId = "u1", totalScore = 24.0, maxMarks = 30.0, hindiScore = 18.0, englishScore = 6.0, langPassed = true)
        )

        // 1. Common GS Set -> showLanguageBadge MUST be false and setLanguageBadgeText MUST be null
        val commonRanked = ExamScoringEngine.computeRankings(
            period = RankingPeriod.INDIVIDUAL_TEST,
            members = listOf(m2, m1),
            attempts = attempts,
            tests = listOf(commonTest, combinedTest, langTest),
            selectedTestId = "set_common_1"
        )
        assertEquals(2, commonRanked.size)
        assertEquals("u1", commonRanked[0].member.userId)
        assertEquals(1, commonRanked[0].rank)
        assertEquals(34.0, commonRanked[0].displayScore, 1e-9)
        assertEquals(40.0, commonRanked[0].maxMarks, 1e-9)
        assertFalse(commonRanked[0].showLanguageBadge)
        assertFalse(commonRanked[1].showLanguageBadge)
        assertEquals(null, ExamScoringEngine.setLanguageBadgeText(commonRanked[0]))
        assertEquals(null, ExamScoringEngine.setLanguageBadgeText(commonRanked[1]))

        // 2. Combined Set (70 Q with Language) -> Merit maxMarks MUST be 40.0 (Language excluded from Merit), badge shows PASS/FAIL
        val combinedRanked = ExamScoringEngine.computeRankings(
            period = RankingPeriod.INDIVIDUAL_TEST,
            members = listOf(m2, m1),
            attempts = attempts,
            tests = listOf(commonTest, combinedTest, langTest),
            selectedTestId = "set_combined_1"
        )
        assertEquals(40.0, combinedRanked[0].maxMarks, 1e-9)
        assertTrue(combinedRanked[0].showLanguageBadge)
        assertTrue(combinedRanked[1].showLanguageBadge)
        assertEquals("22.00/30 PASS", ExamScoringEngine.setLanguageBadgeText(combinedRanked[0]))
        assertEquals("7.00/30 FAIL", ExamScoringEngine.setLanguageBadgeText(combinedRanked[1]))

        // 3. Language Set (30 Q) -> per-set ranking works for Language papers too
        val langRanked = ExamScoringEngine.computeRankings(
            period = RankingPeriod.INDIVIDUAL_TEST,
            members = listOf(m2, m1),
            attempts = attempts,
            tests = listOf(commonTest, combinedTest, langTest),
            selectedTestId = "set_lang_1"
        )
        assertEquals("u1", langRanked[0].member.userId)
        assertEquals(24.0, langRanked[0].displayScore, 1e-9)
        assertEquals(30.0, langRanked[0].maxMarks, 1e-9)
        assertTrue(langRanked[0].showLanguageBadge)
        assertEquals("24.00/30 PASS", ExamScoringEngine.setLanguageBadgeText(langRanked[0]))
    }

    @Test
    fun `ExamTest per-member visibility strictly restricts unselected members while allowing approved members and admins`() {
        val allMembersSet = ExamTest(
            id = "t_all",
            status = "PUBLISHED",
            approvedForAll = true,
            visibleToUids = listOf("ALL")
        )
        assertTrue(allMembersSet.isVisibleToMember("u1", isAdmin = false))
        assertTrue(allMembersSet.isVisibleToMember("u2", isAdmin = false))
        assertEquals("All Users", allMembersSet.accessSummaryLabel())

        val specificSet = ExamTest(
            id = "t_spec",
            status = "PUBLISHED",
            approvedForAll = false,
            approvedMemberIds = listOf("u1", "u3"),
            visibleToUids = listOf("u1", "u3")
        )
        assertTrue(specificSet.isVisibleToMember("u1", isAdmin = false))
        assertFalse(specificSet.isVisibleToMember("u2", isAdmin = false))
        assertTrue(specificSet.isVisibleToMember("u3", isAdmin = false))
        assertTrue(specificSet.isVisibleToMember("u_admin", isAdmin = true))
        assertEquals("2 Selected Users", specificSet.accessSummaryLabel())

        val pendingSet = ExamTest(
            id = "t_pend",
            status = "PENDING",
            approvedForAll = false,
            visibleToUids = emptyList()
        )
        assertFalse(pendingSet.isVisibleToMember("u1", isAdmin = false))
        assertTrue(pendingSet.isVisibleToMember("u_admin", isAdmin = true))
    }

    @Test
    fun `missing or unknown subject maps strictly to Unclassified (never Mathematics) and bulkReassignSubject repairs it`() {
        val unknownSubject = BulkQuestionParser.normalizeSubject("Random Mystery Tag", PaperType.COMBINED)
        assertEquals(BulkQuestionParser.UNCLASSIFIED_SUBJECT, unknownSubject)
        val blankSubject = BulkQuestionParser.normalizeSubject("", PaperType.COMMON_GS)
        assertEquals(BulkQuestionParser.UNCLASSIFIED_SUBJECT, blankSubject)

        // Build a 40Q Common set where 1 Geography question is Unclassified -> validation MUST fail
        val gs40 = OfficialStarterBank.commonGs40Questions().toMutableList()
        val geoIdx = gs40.indexOfFirst { it.subject == "Geography" }
        assertTrue(geoIdx >= 0)
        gs40[geoIdx] = gs40[geoIdx].copy(subject = BulkQuestionParser.UNCLASSIFIED_SUBJECT)

        val badVal = BulkQuestionParser.validate(gs40, PaperType.COMMON_GS)
        assertFalse(badVal.valid)
        assertEquals(1, badVal.unclassifiedQuestions)

        // Repair with bulkReassignSubject -> validation MUST pass
        val repaired = BulkQuestionParser.bulkReassignSubject(gs40, "Geography", setOf(geoIdx))
        val goodVal = BulkQuestionParser.validate(repaired, PaperType.COMMON_GS)
        assertTrue("Repaired 40Q errors: ${goodVal.errors}", goodVal.valid)
        assertEquals(0, goodVal.unclassifiedQuestions)
    }

    @Test
    fun `70Q partitionImportedQuestions separates 22 Hindi, 8 English and 40 Common subjects accurately`() {
        val mixed = OfficialStarterBank.language30Questions() + OfficialStarterBank.commonGs40Questions()
        val partitioned = BulkQuestionParser.partitionImportedQuestions(mixed, PaperType.COMBINED)
        assertEquals(30, partitioned.language.size)
        assertEquals(40, partitioned.common.size)
        assertEquals(0, partitioned.subject.size)

        val assembled = BulkQuestionParser.combineSections(
            commonQuestions = partitioned.common,
            languageQuestions = partitioned.language
        )
        assertTrue("Assembled 70Q errors: ${assembled.errors}", assembled.valid)
        assertEquals(70, assembled.questions.size)
        val report = BulkQuestionParser.validate(assembled.questions, PaperType.COMBINED)
        assertTrue("Combined validation errors: ${report.errors}", report.valid)
        assertEquals(22, report.subjectCounts["Hindi"])
        assertEquals(8, report.subjectCounts["English"])
        assertEquals(8, report.subjectCounts["Mathematics"])
        assertEquals(8, report.subjectCounts["Science"])
        assertEquals(8, report.subjectCounts["History"])
        assertEquals(8, report.subjectCounts["Geography"])
        assertEquals(8, report.subjectCounts["Current Affairs"])
    }

    @Test
    fun `separate subject paper blueprints enforce exact question counts and durations`() {
        assertEquals(40, PaperType.CLASS_6_8_MATH.expectedQuestionCount)
        assertEquals(40, PaperType.CLASS_6_8_MATH.defaultDurationMinutes)
        assertEquals(40, PaperType.CLASS_6_8_SCIENCE.expectedQuestionCount)
        assertEquals(40, PaperType.CLASS_6_8_SCIENCE.defaultDurationMinutes)
        assertEquals(80, PaperType.CLASS_9_10_MATH_80.expectedQuestionCount)
        assertEquals(80, PaperType.CLASS_9_10_MATH_80.defaultDurationMinutes)

        val math40 = (1..40).map { i ->
            ExamQuestion(
                number = i,
                subject = "Mathematics",
                text = "Class 6-8 Math Q$i",
                optionA = "1", optionB = "2", optionC = "3", optionD = "4",
                optionE = ExamQuestion.DEFAULT_OPTION_E,
                correctOption = "B",
                section = ExamQuestion.SECTION_SUBJECT
            )
        }
        val valMath40 = BulkQuestionParser.validate(math40, PaperType.CLASS_6_8_MATH)
        assertTrue("Class 6-8 Math 40Q errors: ${valMath40.errors}", valMath40.valid)
    }

    @Test
    fun `WhatsAppHelper normalizes Indian and international phone numbers and builds safe wa_me URLs`() {
        assertEquals("+919876543210", WhatsAppHelper.normalizePhoneNumber("98765 43210"))
        assertEquals("+919876543210", WhatsAppHelper.normalizePhoneNumber("+91-98765-43210"))
        assertEquals("+14155552671", WhatsAppHelper.normalizePhoneNumber("+1 (415) 555-2671"))
        assertNull(WhatsAppHelper.normalizePhoneNumber("12345"))
        assertTrue(WhatsAppHelper.isValidOptionalInput(""))
        assertTrue(WhatsAppHelper.isValidOptionalInput("9876543210"))
        assertFalse(WhatsAppHelper.isValidOptionalInput("123"))

        val url = WhatsAppHelper.buildWhatsAppUrl("+91 9876543210", "Hello Admin")
        assertEquals("https://wa.me/919876543210?text=Hello%20Admin", url)
    }

    @Test
    fun `SetIconOption defaults and AppUpdateRelease mandatory checks work accurately`() {
        assertEquals(SetIconOption.LANGUAGE_BOOK, SetIconOption.defaultForPaperType(PaperType.LANGUAGE))
        assertEquals(SetIconOption.CALCULATOR, SetIconOption.defaultForPaperType(PaperType.CLASS_9_10_MATH_80))
        assertEquals(SetIconOption.SCIENCE_FLASK, SetIconOption.defaultForPaperType(PaperType.CLASS_6_8_SCIENCE))

        val normalUpd = AppUpdateRelease(versionCode = 2L, updateType = "NORMAL", minSupportedVersionCode = 1L, published = true)
        assertFalse(normalUpd.isMandatoryFor(currentVersionCode = 1L))

        val mandatoryUpd = AppUpdateRelease(versionCode = 3L, updateType = "MANDATORY", minSupportedVersionCode = 2L, published = true)
        assertTrue(mandatoryUpd.isMandatoryFor(currentVersionCode = 1L))
        assertFalse(mandatoryUpd.isMandatoryFor(currentVersionCode = 3L))
    }

    @Test
    fun `Option E Mode A vs Mode B and configurable negative marking (-1_4 and 0) calculate accurately`() {
        val qs = listOf(
            ExamQuestion(number = 1, subject = "Mathematics", text = "Q1", optionA = "1", optionB = "2", optionC = "3", optionD = "4", optionE = ExamQuestion.DEFAULT_OPTION_E, correctOption = "A"),
            ExamQuestion(number = 2, subject = "Mathematics", text = "Q2", optionA = "1", optionB = "2", optionC = "3", optionD = "4", optionE = ExamQuestion.DEFAULT_OPTION_E, correctOption = "B"),
            ExamQuestion(number = 3, subject = "Mathematics", text = "Q3", optionA = "1", optionB = "2", optionC = "3", optionD = "4", optionE = "More than one of the above", correctOption = "E")
        )
        // Mode A (Not Attempted): picking E on Q2 gives 0 penalty; wrong on Q1 gives -0.25 with -1/4 negative
        val modeATest = ExamTest(
            id = "t_mode_a",
            paperType = PaperType.COMMON_GS.name,
            positiveMarks = 2.0,
            negativeMarkNumerator = 1L,
            negativeMarkDenominator = 4L,
            unattemptedPenalty = false,
            optionEMode = OptionEMode.NOT_ATTEMPTED.name
        )
        val evalA = ExamScoringEngine.evaluate(modeATest, qs, mapOf(0 to "A", 1 to "E"))
        assertEquals(2.0, evalA.meritMarks, 1e-9) // Q0=+2, Q1=0(Opt E), Q2=0(blank, no penalty)
        assertEquals(1, evalA.dontKnowCount)
        assertEquals(1, evalA.notAttemptedCount)

        // Mode B (Standard 5th Option): picking E on Q3 where key=E gives +1, picking E on Q2 where key=B gives -0.25
        val modeBTest = ExamTest(
            id = "t_mode_b",
            paperType = PaperType.COMMON_GS.name,
            positiveMarks = 1.0,
            negativeMarkNumerator = 1L,
            negativeMarkDenominator = 4L,
            unattemptedPenalty = false,
            optionEMode = OptionEMode.STANDARD_5TH_OPTION.name
        )
        val evalB = ExamScoringEngine.evaluate(modeBTest, qs, mapOf(0 to "A", 1 to "E", 2 to "E"))
        assertEquals(2, evalB.correctCount)
        assertEquals(1, evalB.incorrectCount)
        assertEquals(1.75, evalB.meritMarks, 1e-9)

        // Zero negative marking preset
        val zeroNegTest = modeBTest.copy(negativeMarkNumerator = 0L, negativeMarkDenominator = 1L)
        val evalZero = ExamScoringEngine.evaluate(zeroNegTest, qs, mapOf(0 to "A", 1 to "D", 2 to "E"))
        assertEquals(2.0, evalZero.meritMarks, 1e-9)
        assertEquals(0.0, evalZero.negativeDeducted, 1e-9)
    }

    @Test
    fun `Language Fail Rank Policies, Tie-Break Policies, and Percentile calculation work accurately`() {
        val m1 = GroupMember(userId = "u1", displayName = "Asha")
        val m2 = GroupMember(userId = "u2", displayName = "Bikash")
        val m3 = GroupMember(userId = "u3", displayName = "Chandan")

        // Tied on Merit score (90/120), but u2 has higher accuracy & faster time; u3 failed Language (6/30)
        val testShowBadge = ExamTest(
            id = "p5_1",
            title = "Part 5 Full Mock 1",
            paperType = PaperType.CLASS_6_8_MATH_SCIENCE.name,
            setType = SetType.PART_5_FULL_MATH_SCI.name,
            status = "PUBLISHED",
            langFailRankPolicy = LanguageFailRankPolicy.SHOW_WITH_NOT_QUALIFIED_BADGE.name,
            tieBreakPolicy = TieBreakPolicy.ACCURACY_AND_TIME.name
        )
        val attempts = listOf(
            TestAttempt(
                id = "p5_1_u1", testId = "p5_1", paperType = PaperType.CLASS_6_8_MATH_SCIENCE.name,
                setType = SetType.PART_5_FULL_MATH_SCI.name, userId = "u1",
                totalScore = 90.0, meritMarks = 90.0, maxMarks = 120.0, totalMeritMarks = 120.0,
                meritCorrectCount = 95, meritIncorrectCount = 15, timeSpentSeconds = 6000,
                hindiScore = 15.0, englishScore = 5.0, languageTotalMarks = 20.0, langPassed = true
            ),
            TestAttempt(
                id = "p5_1_u2", testId = "p5_1", paperType = PaperType.CLASS_6_8_MATH_SCIENCE.name,
                setType = SetType.PART_5_FULL_MATH_SCI.name, userId = "u2",
                totalScore = 90.0, meritMarks = 90.0, maxMarks = 120.0, totalMeritMarks = 120.0,
                meritCorrectCount = 92, meritIncorrectCount = 6, timeSpentSeconds = 5200,
                hindiScore = 12.0, englishScore = 4.0, languageTotalMarks = 16.0, langPassed = true
            ),
            TestAttempt(
                id = "p5_1_u3", testId = "p5_1", paperType = PaperType.CLASS_6_8_MATH_SCIENCE.name,
                setType = SetType.PART_5_FULL_MATH_SCI.name, userId = "u3",
                totalScore = 110.0, meritMarks = 110.0, maxMarks = 120.0, totalMeritMarks = 120.0,
                meritCorrectCount = 110, meritIncorrectCount = 0, timeSpentSeconds = 4500,
                hindiScore = 4.0, englishScore = 2.0, languageTotalMarks = 6.0, langPassed = false
            )
        )

        // Policy 1: SHOW_WITH_NOT_QUALIFIED_BADGE + ACCURACY_AND_TIME tie-break
        val rankedShow = ExamScoringEngine.computeRankings(
            period = RankingPeriod.INDIVIDUAL_TEST,
            members = listOf(m1, m2, m3),
            attempts = attempts,
            tests = listOf(testShowBadge),
            selectedTestId = "p5_1"
        )
        // u3 has 110 Merit but flagged languageNotQualified; u2 beats u1 on tie-break (higher accuracy & lower time)
        assertEquals("u3", rankedShow[0].member.userId)
        assertTrue(rankedShow[0].languageNotQualified)
        assertEquals("u2", rankedShow[1].member.userId)
        assertEquals(2, rankedShow[1].rank)
        assertEquals("u1", rankedShow[2].member.userId)
        assertEquals(3, rankedShow[2].rank)

        // Policy 2: EXCLUDE_FROM_MERIT_RANK excludes u3 from merit rank (rank == 0)
        val testExclude = testShowBadge.copy(
            langFailRankPolicy = LanguageFailRankPolicy.EXCLUDE_FROM_MERIT_RANK.name,
            tieBreakPolicy = TieBreakPolicy.SHARED_EQUAL_RANK.name
        )
        val rankedExclude = ExamScoringEngine.computeRankings(
            period = RankingPeriod.INDIVIDUAL_TEST,
            members = listOf(m1, m2, m3),
            attempts = attempts,
            tests = listOf(testExclude),
            selectedTestId = "p5_1"
        )
        val u3Row = rankedExclude.first { it.member.userId == "u3" }
        assertEquals(0, u3Row.rank)
        assertTrue(u3Row.languageNotQualified)
        // With SHARED_EQUAL_RANK, u1 and u2 both share Rank #1!
        val u1Row = rankedExclude.first { it.member.userId == "u1" }
        val u2Row = rankedExclude.first { it.member.userId == "u2" }
        assertEquals(1, u1Row.rank)
        assertEquals(1, u2Row.rank)

        // Percentile formula verification: ((N - rank) / N) * 100, with N == 1 -> 100.0
        assertEquals(100.0, ExamScoringEngine.computePercentile(1, 1), 1e-9)
        assertEquals(90.0, ExamScoringEngine.computePercentile(1, 10), 1e-9)
        assertEquals(80.0, ExamScoringEngine.computePercentile(2, 10), 1e-9)
    }

    @Test
    fun `Independent 6-Part Category Rankings never mix Part 5 with Part 6 and keep Part 2 Qualifying Only`() {
        val m1 = GroupMember(userId = "u1", displayName = "Neha")
        val m2 = GroupMember(userId = "u2", displayName = "Ravi")
        val tPart5 = ExamTest(id = "t_p5", title = "Part 5 Set 1", paperType = PaperType.CLASS_6_8_MATH_SCIENCE.name, setType = SetType.PART_5_FULL_MATH_SCI.name, status = "PUBLISHED")
        val tPart6 = ExamTest(id = "t_p6", title = "Part 6 Set 1", paperType = PaperType.CLASS_9_10_MATH.name, setType = SetType.PART_6_FULL_MATH.name, status = "PUBLISHED")
        val tPart2 = ExamTest(id = "t_p2", title = "Part 2 Lang 1", paperType = PaperType.LANGUAGE.name, setType = SetType.PART_2_LANGUAGE.name, status = "PUBLISHED")

        val attempts = listOf(
            TestAttempt(id = "a1", testId = "t_p5", paperType = tPart5.paperType, setType = tPart5.setType, userId = "u1", totalScore = 88.0, meritMarks = 88.0, totalMeritMarks = 120.0, langPassed = true),
            TestAttempt(id = "a2", testId = "t_p6", paperType = tPart6.paperType, setType = tPart6.setType, userId = "u2", totalScore = 104.0, meritMarks = 104.0, totalMeritMarks = 120.0, langPassed = true),
            TestAttempt(id = "a3", testId = "t_p2", paperType = tPart2.paperType, setType = tPart2.setType, userId = "u1", totalScore = 22.0, languageTotalMarks = 22.0, hindiScore = 16.0, englishScore = 6.0, langPassed = true)
        )

        val p5Ranked = ExamScoringEngine.computePartCategoryRankings(SetType.PART_5_FULL_MATH_SCI, listOf(m1, m2), attempts, listOf(tPart5, tPart6, tPart2))
        assertEquals("u1", p5Ranked.first().member.userId)
        assertEquals(1, p5Ranked.first().testsAttempted)
        assertEquals(0, p5Ranked.first { it.member.userId == "u2" }.testsAttempted) // Ravi's Part 6 attempt MUST NOT leak into Part 5!

        val p6Ranked = ExamScoringEngine.computePartCategoryRankings(SetType.PART_6_FULL_MATH, listOf(m1, m2), attempts, listOf(tPart5, tPart6, tPart2))
        assertEquals("u2", p6Ranked.first().member.userId)
        assertEquals(1, p6Ranked.first().testsAttempted)
        assertEquals(0, p6Ranked.first { it.member.userId == "u1" }.testsAttempted)

        val p2History = ExamScoringEngine.computePartCategoryRankings(SetType.PART_2_LANGUAGE, listOf(m1, m2), attempts, listOf(tPart5, tPart6, tPart2))
        assertTrue("Part 2 Language MUST NEVER assign a Merit rank", p2History.all { it.rank == 0 })
    }

    @Test
    fun `Topper vs Average, Section Sub-Subject Performance, and RankingCsvExporter keep Language separate from Merit`() {
        val qs = OfficialStarterBank.commonGs40Questions()
        val test = ExamTest(id = "t_gs40", title = "Part 1 Common Mock", paperType = PaperType.COMMON_GS.name, setType = SetType.PART_1_COMMON.name, status = "PUBLISHED")
        val a1 = TestAttempt(
            id = "t_gs40_u1", testId = "t_gs40", paperType = test.paperType, setType = test.setType,
            userId = "u1", aspirantName = "Topper",
            answers = qs.indices.associate { it.toString() to qs[it].correctOption },
            correctCount = 40, incorrectCount = 0, totalScore = 40.0, meritMarks = 40.0, maxMarks = 40.0, totalMeritMarks = 40.0,
            timeSpentSeconds = 1200
        )
        val a2 = TestAttempt(
            id = "t_gs40_u2", testId = "t_gs40", paperType = test.paperType, setType = test.setType,
            userId = "u2", aspirantName = "Learner",
            answers = qs.indices.associate { it.toString() to if (it < 20) qs[it].correctOption else "E" },
            correctCount = 20, incorrectCount = 0, dontKnowCount = 20, totalScore = 20.0, meritMarks = 20.0, maxMarks = 40.0, totalMeritMarks = 40.0,
            timeSpentSeconds = 1500
        )

        val compare = ExamScoringEngine.computeTopperVsAverage(test, a2, listOf(a1, a2))
        assertEquals(20.0, compare.yourMeritScore, 1e-9)
        assertEquals(40.0, compare.topperMeritScore, 1e-9)
        assertEquals(30.0, compare.averageMeritScore, 1e-9)

        val secRows = ExamScoringEngine.computeSectionAndSubSubjectPerformance(test, qs, a2)
        assertTrue(secRows.any { it.title.contains("Common", ignoreCase = true) })
        assertEquals(5, secRows.count { it.isSubSubject })

        val members = listOf(GroupMember(userId = "u1", displayName = "Topper"), GroupMember(userId = "u2", displayName = "Learner"))
        val ranked = ExamScoringEngine.computeRankings(RankingPeriod.INDIVIDUAL_TEST, members, listOf(a1, a2), listOf(test), selectedTestId = "t_gs40")
        val csv = RankingCsvExporter.buildCsvContent(
            scopeTitle = test.title,
            ranked = ranked,
            attemptsByUserId = mapOf("u1" to a1, "u2" to a2),
            test = test
        )
        assertTrue(csv.contains("Merit Score,Merit Max"))
        assertTrue(csv.contains("Language Total (/30),Language Status (Qualifying Only)"))
        assertTrue(csv.contains("Topper"))
        assertTrue(csv.contains("Learner"))
    }

    @Test
    fun `7 RankCategory mapping, first-attempt deduplication, and RankerDetailBreakdown subject correct counts work accurately`() {
        assertEquals(RankCategory.LANGUAGE, RankCategory.fromTest(ExamTest(paperType = PaperType.LANGUAGE.name)))
        assertEquals(RankCategory.COMMON, RankCategory.fromTest(ExamTest(paperType = PaperType.COMMON_GS.name)))
        assertEquals(RankCategory.LANGUAGE_COMMON, RankCategory.fromTest(ExamTest(paperType = PaperType.COMBINED.name)))
        assertEquals(RankCategory.MATHEMATICS, RankCategory.fromTest(ExamTest(paperType = PaperType.CLASS_6_8_MATH.name)))
        assertEquals(RankCategory.SCIENCE, RankCategory.fromTest(ExamTest(paperType = PaperType.CLASS_6_8_SCIENCE.name)))
        assertEquals(RankCategory.FULL_MATHS_SCIENCE, RankCategory.fromTest(ExamTest(paperType = PaperType.CLASS_6_8_MATH_SCIENCE.name)))
        assertEquals(RankCategory.FULL_MATHS, RankCategory.fromTest(ExamTest(paperType = PaperType.CLASS_9_10_MATH.name)))
        assertEquals(RankCategory.MATHEMATICS, RankCategory.fromTest(ExamTest(paperType = PaperType.CLASS_9_10_MATH_80.name)))

        val qs = listOf(
            ExamQuestion(number = 1, subject = "Mathematics", text = "M1", optionA = "1", optionB = "2", optionC = "3", optionD = "4", correctOption = "A", section = ExamQuestion.SECTION_COMMON),
            ExamQuestion(number = 2, subject = "Mathematics", text = "M2", optionA = "1", optionB = "2", optionC = "3", optionD = "4", correctOption = "B", section = ExamQuestion.SECTION_COMMON),
            ExamQuestion(number = 3, subject = "Science", text = "S1", optionA = "1", optionB = "2", optionC = "3", optionD = "4", correctOption = "C", section = ExamQuestion.SECTION_COMMON)
        )
        val commonTest = ExamTest(
            id = "c1",
            title = "Common Set 1",
            paperType = PaperType.COMMON_GS.name,
            setType = SetType.PART_1_COMMON.name,
            status = "PUBLISHED",
            questions = qs.map { it.toMap() }
        )
        val mathTest = ExamTest(
            id = "m1",
            title = "Math Set 1",
            paperType = PaperType.CLASS_6_8_MATH.name,
            setType = SetType.PART_3_MATHS.name,
            status = "PUBLISHED"
        )
        val m1 = GroupMember(userId = "u1", displayName = "Kiran")
        val m2 = GroupMember(userId = "u2", displayName = "Deepak")

        val now = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 60_000L))
        val later = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 10_000L))
        // u1 has two attempts on same Common test -> FIRST valid attempt (1.67) must be kept over second attempt (3.0)
        val attempts = listOf(
            TestAttempt(
                id = "a1_first", testId = "c1", paperType = PaperType.COMMON_GS.name, setType = SetType.PART_1_COMMON.name,
                userId = "u1", answers = mapOf("0" to "A", "1" to "B", "2" to "D"),
                correctCount = 2, incorrectCount = 1, totalScore = 1.67, meritMarks = 1.67, maxMarks = 3.0, totalMeritMarks = 3.0,
                timeSpentSeconds = 95L, submittedAt = now
            ),
            TestAttempt(
                id = "a1_second_gaming", testId = "c1", paperType = PaperType.COMMON_GS.name, setType = SetType.PART_1_COMMON.name,
                userId = "u1", answers = mapOf("0" to "A", "1" to "B", "2" to "C"),
                correctCount = 3, incorrectCount = 0, totalScore = 3.0, meritMarks = 3.0, maxMarks = 3.0, totalMeritMarks = 3.0,
                timeSpentSeconds = 40L, submittedAt = later
            ),
            TestAttempt(
                id = "a2_math", testId = "m1", paperType = PaperType.CLASS_6_8_MATH.name, setType = SetType.PART_3_MATHS.name,
                userId = "u2", totalScore = 35.0, meritMarks = 35.0, maxMarks = 40.0, totalMeritMarks = 40.0,
                timeSpentSeconds = 1200L, submittedAt = now
            )
        )

        val commonRanked = ExamScoringEngine.computeCategoryRankings(
            category = RankCategory.COMMON,
            period = RankingPeriod.INDIVIDUAL_TEST,
            members = listOf(m1, m2),
            attempts = attempts,
            tests = listOf(commonTest, mathTest)
        )
        val u1Row = commonRanked.first { it.member.userId == "u1" }
        val u2Row = commonRanked.first { it.member.userId == "u2" }
        assertEquals(1, u1Row.testsAttempted)
        assertEquals(1.67, u1Row.displayScore, 1e-6)
        // u2's Math attempt must NOT bleed into COMMON category
        assertEquals(0, u2Row.testsAttempted)

        val breakdown = ExamScoringEngine.computeRankerDetailBreakdown(
            entry = u1Row,
            category = RankCategory.COMMON,
            period = RankingPeriod.INDIVIDUAL_TEST,
            attempts = attempts,
            tests = listOf(commonTest, mathTest)
        )
        assertEquals(2, breakdown.correctCount)
        assertEquals(1, breakdown.incorrectCount)
        assertEquals(0, breakdown.unattemptedCount)
        assertEquals(95L, breakdown.timeSpentSeconds)
        val mathStat = breakdown.subjectStats.first { it.subject == "Mathematics" }
        assertEquals(2, mathStat.correctCount)
        assertEquals(2, mathStat.totalQuestions)
        val sciStat = breakdown.subjectStats.first { it.subject == "Science" }
        assertEquals(0, sciStat.correctCount)
        assertEquals(1, sciStat.totalQuestions)
    }

    @Test
    fun `ContactAdminPolicy hides WhatsApp from primary admin unless another secondary admin contact exists`() {
        val singleAdmin = AdminContactProfile(adminUid = "admin_1", adminName = "Gopal", adminWhatsAppNumber = "+919876543210")
        val secondaryAdmin = AdminContactProfile(adminUid = "admin_2", adminName = "Co-Admin", adminWhatsAppNumber = "+919811111111")

        // Regular users ALWAYS see Contact Admin
        assertTrue(ContactAdminPolicy.shouldShowContactAdmin(isAdmin = false, currentUserId = "user_1", adminContact = null))
        assertTrue(ContactAdminPolicy.shouldShowContactAdmin(isAdmin = false, currentUserId = "user_1", adminContact = singleAdmin))

        // Admin logged in as self does NOT see Contact Admin
        assertFalse(ContactAdminPolicy.shouldShowContactAdmin(isAdmin = true, currentUserId = "admin_1", adminContact = singleAdmin))
        assertFalse(ContactAdminPolicy.shouldShowContactAdmin(isAdmin = true, currentUserId = "admin_1", adminContact = null))

        // Admin sees Contact Admin only when secondary/tertiary Admin has WhatsApp configured
        assertTrue(ContactAdminPolicy.shouldShowContactAdmin(isAdmin = true, currentUserId = "admin_1", adminContact = secondaryAdmin))
    }

    @Test
    fun `selectedTestId strictly filters rankings and RankerDetailBreakdown by exact set number and difficulty`() {
        val set1Easy = ExamTest(
            id = "t_easy_s1",
            title = "Common Paper Set 1",
            paperType = PaperType.COMMON_GS.name,
            setType = SetType.PART_1_COMMON.name,
            difficulty = "EASY",
            status = "PUBLISHED"
        )
        val set1Hard = ExamTest(
            id = "t_hard_s1",
            title = "Common Paper Set 1",
            paperType = PaperType.COMMON_GS.name,
            setType = SetType.PART_1_COMMON.name,
            difficulty = "HARD",
            status = "PUBLISHED"
        )
        val set2Easy = ExamTest(
            id = "t_easy_s2",
            title = "Common Paper Set 2",
            paperType = PaperType.COMMON_GS.name,
            setType = SetType.PART_1_COMMON.name,
            difficulty = "EASY",
            status = "PUBLISHED"
        )
        assertEquals(1, set1Easy.resolvedSetNumber())
        assertEquals(2, set2Easy.resolvedSetNumber())
        assertEquals(3, ExamTest(title = "Math सेट #3 (Hard)").resolvedSetNumber())
        assertEquals(5, ExamTest(title = "Custom Name", setNumber = 5).resolvedSetNumber())

        val u1 = GroupMember(userId = "u1", displayName = "Ritu")
        val u2 = GroupMember(userId = "u2", displayName = "Aman")
        val now = com.google.firebase.Timestamp.now()
        val attempts = listOf(
            TestAttempt(
                id = "a_u1_s1_easy", testId = "t_easy_s1", paperType = PaperType.COMMON_GS.name,
                userId = "u1", correctCount = 30, incorrectCount = 4, totalScore = 28.67, meritMarks = 28.67,
                maxMarks = 40.0, totalMeritMarks = 40.0, timeSpentSeconds = 900L, submittedAt = now
            ),
            TestAttempt(
                id = "a_u2_s1_hard", testId = "t_hard_s1", paperType = PaperType.COMMON_GS.name,
                userId = "u2", correctCount = 36, incorrectCount = 0, totalScore = 36.0, meritMarks = 36.0,
                maxMarks = 40.0, totalMeritMarks = 40.0, timeSpentSeconds = 800L, submittedAt = now
            ),
            TestAttempt(
                id = "a_u2_s2_easy", testId = "t_easy_s2", paperType = PaperType.COMMON_GS.name,
                userId = "u2", correctCount = 38, incorrectCount = 0, totalScore = 38.0, meritMarks = 38.0,
                maxMarks = 40.0, totalMeritMarks = 40.0, timeSpentSeconds = 700L, submittedAt = now
            )
        )

        val set1EasyRanked = ExamScoringEngine.computeCategoryRankings(
            period = RankingPeriod.INDIVIDUAL_TEST,
            category = RankCategory.COMMON,
            members = listOf(u1, u2),
            attempts = attempts,
            tests = listOf(set1Easy, set1Hard, set2Easy),
            selectedTestId = "t_easy_s1"
        )
        val rituRow = set1EasyRanked.first { it.member.userId == "u1" }
        val amanRow = set1EasyRanked.first { it.member.userId == "u2" }

        assertEquals(1, rituRow.testsAttempted)
        assertEquals(1, rituRow.testsAvailable)
        assertEquals(28.67, rituRow.displayScore, 1e-6)
        assertTrue(rituRow.rankScopeLabel.startsWith("Set 1 - Rank"))
        assertTrue(rituRow.rankScopeLabel.contains("EASY"))
        // Aman's Set 1 HARD & Set 2 EASY attempts MUST NOT appear inside Set 1 EASY rank!
        assertEquals(0, amanRow.testsAttempted)
        assertEquals(0.0, amanRow.displayScore, 1e-6)

        val rituDetail = ExamScoringEngine.computeRankerDetailBreakdown(
            entry = rituRow,
            category = RankCategory.COMMON,
            period = RankingPeriod.INDIVIDUAL_TEST,
            attempts = attempts,
            tests = listOf(set1Easy, set1Hard, set2Easy),
            selectedTestId = "t_easy_s1"
        )
        assertEquals("Set 1 (EASY)", rituDetail.periodLabel)
        assertEquals(30, rituDetail.correctCount)
        assertEquals(4, rituDetail.incorrectCount)
    }

    @Test
    fun `IstClock strictly formats and parses Indian Standard Time (Asia Kolkata UTC+5_30) regardless of device timezone`() {
        val originalDefault = java.util.TimeZone.getDefault()
        try {
            // Simulate device running in US Eastern or UTC
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"))
            assertEquals("Asia/Kolkata", com.example.util.IstClock.istTimeZone.id)

            // 2026-06-15 12:00:00 UTC -> exactly 05:30:00 PM IST (UTC+5:30)
            val utcNoonMillis = 1_781_524_800_000L
            assertEquals(
                "05:30:00 PM IST",
                com.example.util.IstClock.formatEpochMillis(utcNoonMillis, "hh:mm:ss a 'IST'")
            )
            assertEquals(
                "15 Jun 2026, 05:30 PM IST",
                com.example.util.IstClock.formatEpochMillis(utcNoonMillis, "dd MMM yyyy, hh:mm a 'IST'")
            )

            // Parse scheduled release timestamp as IST (2026-06-15T17:30 IST == 12:00:00 UTC)
            val parsedIst = com.example.util.IstClock.parse("2026-06-15T17:30", "yyyy-MM-dd'T'HH:mm")
            org.junit.Assert.assertNotNull(parsedIst)
            assertEquals(utcNoonMillis, parsedIst!!.time)
        } finally {
            java.util.TimeZone.setDefault(originalDefault)
        }
    }

    @Test
    fun `150Q Math and Science CSV with explicit part variants, missing part auto-split, and Fix Section Auto-Fix passes with zero errors`() {
        // 1. Flexible part/section normalization & subject variant normalization
        assertEquals(ExamQuestion.SECTION_LANGUAGE, BulkQuestionParser.normalizePartOrSection("Part 1"))
        assertEquals(ExamQuestion.SECTION_LANGUAGE, BulkQuestionParser.normalizePartOrSection("1"))
        assertEquals(ExamQuestion.SECTION_LANGUAGE, BulkQuestionParser.normalizePartOrSection("Language"))
        assertEquals(ExamQuestion.SECTION_COMMON, BulkQuestionParser.normalizePartOrSection("Part 2"))
        assertEquals(ExamQuestion.SECTION_COMMON, BulkQuestionParser.normalizePartOrSection("2"))
        assertEquals(ExamQuestion.SECTION_COMMON, BulkQuestionParser.normalizePartOrSection("Common"))
        assertEquals(ExamQuestion.SECTION_COMMON, BulkQuestionParser.normalizePartOrSection("General Studies"))
        assertEquals(ExamQuestion.SECTION_SUBJECT, BulkQuestionParser.normalizePartOrSection("Part 3"))
        assertEquals(ExamQuestion.SECTION_SUBJECT, BulkQuestionParser.normalizePartOrSection("3"))
        assertEquals(ExamQuestion.SECTION_SUBJECT, BulkQuestionParser.normalizePartOrSection("Subject"))
        assertEquals(ExamQuestion.SECTION_SUBJECT, BulkQuestionParser.normalizePartOrSection("Main Subject"))

        assertEquals("English", BulkQuestionParser.normalizeSubject("English Language"))
        assertEquals("Hindi", BulkQuestionParser.normalizeSubject("Hindi Language"))
        assertEquals("History", BulkQuestionParser.normalizeSubject("Indian History"))
        assertEquals("Geography", BulkQuestionParser.normalizeSubject("Indian Geography"))

        // 2. CSV WITHOUT `part` column -> Smart Auto-Split Fallback
        val csvNoPart = StringBuilder("subject,topic,question,optionA,optionB,optionC,optionD,correctAnswer,explanation\n")
        var qNum = 1
        fun addCsvRow(subject: String) {
            csvNoPart.append("$subject,Topic$qNum,\"AutoSplit CSV Q#$qNum ($subject)?\",A$qNum,B$qNum,C$qNum,D$qNum,C,Exp $qNum\n")
            qNum++
        }
        repeat(22) { addCsvRow("Hindi Language") }
        repeat(8) { addCsvRow("English Language") }
        repeat(8) { addCsvRow("Indian History") }
        repeat(8) { addCsvRow("Indian Geography") }
        repeat(8) { addCsvRow("Current Affairs") }
        repeat(48) { addCsvRow("Mathematics") }
        repeat(48) { addCsvRow("Science") }

        val parsedCsv = BulkQuestionParser.parseCsv(csvNoPart.toString(), PaperType.CLASS_6_8_MATH_SCIENCE)
        val csvReport = BulkQuestionParser.validate(parsedCsv, PaperType.CLASS_6_8_MATH_SCIENCE)
        assertTrue("CSV auto-split errors: ${csvReport.errors}", csvReport.valid)
        assertEquals(30, csvReport.part1LanguageCount)
        assertEquals(40, csvReport.commonCount)
        assertEquals(80, csvReport.subjectPaperCount)
        assertEquals(8, csvReport.part2CommonSubjectCounts["Mathematics"])
        assertEquals(8, csvReport.part2CommonSubjectCounts["Science"])
        assertEquals(40, csvReport.part3SubjectCounts["Mathematics"])
        assertEquals(40, csvReport.part3SubjectCounts["Science"])

        // 3. CSV_TEMPLATE stays synced with parser (Part 1, Part 2, Part 3)
        val parsedTemplateCsv = BulkQuestionParser.parseCsv(BulkQuestionParser.CSV_TEMPLATE, PaperType.CLASS_6_8_MATH_SCIENCE)
        assertEquals(3, parsedTemplateCsv.size)
        assertEquals(ExamQuestion.SECTION_LANGUAGE, parsedTemplateCsv[0].section)
        assertEquals(ExamQuestion.SECTION_COMMON, parsedTemplateCsv[1].section)
        assertEquals(ExamQuestion.SECTION_SUBJECT, parsedTemplateCsv[2].section)

        // 4. Fix Section / Auto-Fix on misrouted already-loaded questions (all 48 Math + 48 Science dumped into COMMON)
        val misrouted = parsedCsv.map { q ->
            if (q.subject == "Mathematics" || q.subject == "Science") q.copy(section = ExamQuestion.SECTION_COMMON) else q
        }
        val badReport = BulkQuestionParser.validate(misrouted, PaperType.CLASS_6_8_MATH_SCIENCE)
        assertFalse("Misrouted 150-Q set must fail before Auto-Fix", badReport.valid)

        val fixedPart = BulkQuestionParser.partitionImportedQuestions(
            questions = misrouted,
            targetPaperType = PaperType.CLASS_6_8_MATH_SCIENCE,
            forceAutoSplit = true
        )
        val recombined = BulkQuestionParser.combineFull150Sections(
            languageQuestions = fixedPart.language,
            commonQuestions = fixedPart.common,
            subjectQuestions = fixedPart.subject,
            targetType = PaperType.CLASS_6_8_MATH_SCIENCE
        )
        assertTrue("Auto-Fix must resolve all section errors: ${recombined.errors}", recombined.valid)
        assertEquals(30, recombined.languageCount)
        assertEquals(40, recombined.commonCount)
        assertEquals(80, recombined.subjectCount)
    }
}
