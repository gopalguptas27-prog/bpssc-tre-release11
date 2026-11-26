package com.example.domain

import com.example.data.model.ExamQuestion
import com.example.data.model.PaperType
import org.json.JSONArray
import org.json.JSONObject

data class ValidationReport(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val subjectCounts: Map<String, Int>,
    val expectedSubjectCounts: Map<String, Int> = emptyMap(),
    val totalQuestions: Int = 0,
    val expectedTotal: Int = 0,
    val hindiCount: Int = 0,
    val englishCount: Int = 0,
    val commonCount: Int = 0,
    val subjectPaperCount: Int = 0,
    val missingQuestions: Int = 0,
    val duplicateQuestions: Int = 0,
    val unclassifiedQuestions: Int = 0,
    val missingCorrectAnswers: Int = 0,
    val invalidOptions: Int = 0,
    val part1LanguageCount: Int = 0,
    val part2CommonSubjectCounts: Map<String, Int> = emptyMap(),
    val part3SubjectCounts: Map<String, Int> = emptyMap()
)

data class CombinedAssemblyResult(
    val valid: Boolean,
    val questions: List<ExamQuestion>,
    val errors: List<String>,
    val commonCount: Int,
    val languageCount: Int,
    val subjectCount: Int = 0
)

data class SectionPartition(
    val language: List<ExamQuestion>,
    val common: List<ExamQuestion>,
    val subject: List<ExamQuestion>
)

/**
 * Smart Question Bank Importer & Blueprint Validation Engine for BPSSC TRE 4.0.
 *
 * Canonical Subjects (zero duplicate NCERT/BIP labels):
 *  - Language Paper (30 Q): Hindi (22), English (8)
 *  - Common Paper (40 Q): Mathematics (8), Science (8), History (8), Geography (8), Current Affairs (8)
 *  - Separate Subject Papers:
 *      - Class 6–8 Mathematics (40 Q Mathematics)
 *      - Class 6–8 Science (40 Q Science)
 *      - Class 9–10 Mathematics (80 Q Mathematics)
 *  - Combined Papers:
 *      - Language + Common (70 Q = 30 Language + 40 Common)
 *      - Class 6–8 Math & Science Full (150 Q = 30 Language + 40 Common + 40 Math + 40 Science)
 *      - Class 9–10 Math Full (150 Q = 30 Language + 40 Common + 80 Math)
 *
 * CRITICAL GUARANTEES:
 *  - NEVER defaults missing or unrecognized subjects to "Mathematics" — always marks "Unclassified".
 *  - NEVER silently defaults missing/invalid correctAnswer to "A".
 *  - NEVER requires Option E in JSON — automatically appends "प्रयास नहीं किया गया / Not Attempted".
 *  - NEVER dumps 70-Q or 150-Q imports into one subject or one section — auto-classifies each question by its true subject & section.
 */
object BulkQuestionParser {

    const val UNCLASSIFIED_SUBJECT = "Unclassified"

    val GS_SUBJECTS = listOf(
        "Mathematics",
        "Science",
        "History",
        "Geography",
        "Current Affairs"
    )

    val LANG_SUBJECTS = listOf("Hindi", "English")

    val ALL_STANDARD_SUBJECTS = LANG_SUBJECTS + GS_SUBJECTS

    val CLASS_6_8_SUBJECTS = listOf("Mathematics", "Science")
    val CLASS_9_10_SUBJECTS = listOf("Mathematics")

    /**
     * Exact expected per-subject counts for every PaperType (Blueprint-driven; never hardcoded vs total).
     */
    fun expectedBlueprint(paperType: PaperType): Map<String, Int> = when (paperType) {
        PaperType.LANGUAGE -> linkedMapOf("Hindi" to 22, "English" to 8)
        PaperType.COMMON_GS -> linkedMapOf(
            "Mathematics" to 8,
            "Science" to 8,
            "History" to 8,
            "Geography" to 8,
            "Current Affairs" to 8
        )
        PaperType.COMBINED -> linkedMapOf(
            "Hindi" to 22,
            "English" to 8,
            "Mathematics" to 8,
            "Science" to 8,
            "History" to 8,
            "Geography" to 8,
            "Current Affairs" to 8
        )
        PaperType.CLASS_6_8_MATH -> linkedMapOf("Mathematics" to 40)
        PaperType.CLASS_6_8_SCIENCE -> linkedMapOf("Science" to 40)
        PaperType.CLASS_9_10_MATH_80 -> linkedMapOf("Mathematics" to 80)
        PaperType.CLASS_6_8_MATH_SCIENCE -> linkedMapOf(
            "Hindi" to 22,
            "English" to 8,
            "Mathematics" to 48,
            "Science" to 48,
            "History" to 8,
            "Geography" to 8,
            "Current Affairs" to 8
        )
        PaperType.CLASS_9_10_MATH -> linkedMapOf(
            "Hindi" to 22,
            "English" to 8,
            "Mathematics" to 88,
            "Science" to 8,
            "History" to 8,
            "Geography" to 8,
            "Current Affairs" to 8
        )
        PaperType.PRACTICE -> emptyMap()
    }

    /**
     * Canonicalizes any English/Hindi/legacy subject alias cleanly.
     * Returns [UNCLASSIFIED_SUBJECT] when empty or unrecognized — NEVER defaults to "Mathematics"!
     */
    fun normalizeSubject(raw: String, paperType: PaperType = PaperType.COMBINED): String {
        val cleaned = raw.trim()
        if (cleaned.isBlank()) return UNCLASSIFIED_SUBJECT
        val s = cleaned.lowercase()

        if (s == "unclassified" || s == "unknown" || s == "none" || s == "other" || s == "n/a") {
            return UNCLASSIFIED_SUBJECT
        }

        return when {
            // Language subjects
            s.contains("hind") || s.contains("हिन्दी") || s.contains("हिंदी") || s.contains("व्याकरण") -> "Hindi"
            s.contains("eng") || s.contains("अंग्रेज") || s.contains("अंग्रेज़") -> "English"

            // Common & Subject Paper canonical subjects
            s.contains("math") || s.contains("ganit") || s.contains("गणित") || s.contains("arithmetic") || s.contains("algebra") || s.contains("geometry") || s.contains("trigonomet") || s.contains("mensuration") -> "Mathematics"
            s.contains("sci") || s.contains("vigyan") || s.contains("विज्ञान") || s.contains("physic") || s.contains("chem") || s.contains("biolog") || s.contains("भौतिक") || s.contains("रसायन") || s.contains("जीव") -> "Science"
            s.contains("hist") || s.contains("national movement") || s.contains("inm") || s.contains("1857") || s.contains("freedom") || s.contains("इतिहास") || s.contains("राष्ट्रीय आंदोलन") -> "History"
            s.contains("geo") || s.contains("bhugol") || s.contains("भूगोल") -> "Geography"
            s.contains("curr") || s.contains("pib") || s.contains("affairs") || s.contains("समसामयिक") || s.contains("करंट") || s.contains("general awareness") || s == "gk" || s == "g.k." || s.contains("सामान्य ज्ञान") -> "Current Affairs"

            else -> UNCLASSIFIED_SUBJECT
        }
    }

    /**
     * Normalizes explicit `part` or `section` values flexibly across JSON, CSV, and bulk text imports:
     * - "Part 1", "1", "Language" -> [ExamQuestion.SECTION_LANGUAGE]
     * - "Part 2", "2", "Common", "General Studies", "GS" -> [ExamQuestion.SECTION_COMMON]
     * - "Part 3", "3", "Subject", "Main Subject" -> [ExamQuestion.SECTION_SUBJECT]
     * Returns "" when blank or unrecognized so callers can apply smart auto-split fallback.
     */
    fun normalizePartOrSection(raw: String): String {
        val cleaned = raw.trim()
        if (cleaned.isBlank()) return ""
        val s = cleaned.lowercase()
            .replace(Regex("[_\\-:()]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return when {
            s == "1" || s == "part 1" || s == "part1" || s == "part i" || s == "i" ||
                s.startsWith("part 1 ") || s.contains("part 1") || s.contains("part1") ||
                s.contains("lang") || s.contains("भाषा") || s.contains("qualifying") -> ExamQuestion.SECTION_LANGUAGE

            s == "2" || s == "part 2" || s == "part2" || s == "part ii" || s == "ii" ||
                s.startsWith("part 2 ") || s.contains("part 2") || s.contains("part2") ||
                s.contains("common") || s.contains("general studies") || s == "gs" ||
                s.startsWith("gs ") || s.endsWith(" gs") || s.contains("कॉमन") || s.contains("सामान्य अध्ययन") -> ExamQuestion.SECTION_COMMON

            s == "3" || s == "part 3" || s == "part3" || s == "part iii" || s == "iii" ||
                s.startsWith("part 3 ") || s.contains("part 3") || s.contains("part3") ||
                s.contains("main subject") || s.contains("subject") || s.contains("विषय") -> ExamQuestion.SECTION_SUBJECT

            else -> ""
        }
    }

    fun isLanguageSubject(subject: String): Boolean {
        val norm = normalizeSubject(subject)
        return norm == "Hindi" || norm == "English"
    }

    fun isCommonSubject(subject: String): Boolean {
        val norm = normalizeSubject(subject)
        return norm in GS_SUBJECTS
    }

    /**
     * Core section router used by JSON, CSV, Bulk Text, and Fix Section / Auto-Fix:
     * - Respects explicit `part` / `section` strictly whenever provided.
     * - When `part` is missing, applies Smart Auto-Split Fallback for 150-Q papers:
     *   - Part 1 (Language - 30 Qs): Hindi (22) + English (8)
     *   - Part 2 (Common - 40 Qs): History (8), Geography (8), Current Affairs (8),
     *     FIRST 8 Mathematics, and FIRST 8 Science
     *   - Part 3 (Subject - 80 Qs): REMAINING 40 Mathematics and REMAINING 40 Science
     *     (or remaining 80 Mathematics for Class 9–10 Math).
     */
    private fun assignSectionsWithExplicitHints(
        entries: List<Pair<ExamQuestion, String>>,
        paperType: PaperType,
        existingCommonMathCount: Int = 0,
        existingCommonScienceCount: Int = 0
    ): List<ExamQuestion> {
        var commonMathCount = existingCommonMathCount
        var commonSciCount = existingCommonScienceCount

        // Account for any questions explicitly tagged Part 2 Common first so mixed batches fill remaining slots accurately.
        entries.forEach { (q, explicitSec) ->
            if (explicitSec == ExamQuestion.SECTION_COMMON) {
                when (normalizeSubject(q.subject, paperType)) {
                    "Mathematics" -> commonMathCount++
                    "Science" -> commonSciCount++
                }
            }
        }

        return entries.map { (rawQ, explicitSec) ->
            val normSubj = normalizeSubject(rawQ.subject, paperType)
            val targetSec = when {
                explicitSec == ExamQuestion.SECTION_LANGUAGE -> ExamQuestion.SECTION_LANGUAGE
                explicitSec == ExamQuestion.SECTION_COMMON -> ExamQuestion.SECTION_COMMON
                explicitSec == ExamQuestion.SECTION_SUBJECT -> ExamQuestion.SECTION_SUBJECT
                isLanguageSubject(normSubj) -> ExamQuestion.SECTION_LANGUAGE
                paperType == PaperType.CLASS_6_8_MATH_SCIENCE -> {
                    when (normSubj) {
                        "Mathematics" -> if (commonMathCount < 8) {
                            commonMathCount++
                            ExamQuestion.SECTION_COMMON
                        } else {
                            ExamQuestion.SECTION_SUBJECT
                        }
                        "Science" -> if (commonSciCount < 8) {
                            commonSciCount++
                            ExamQuestion.SECTION_COMMON
                        } else {
                            ExamQuestion.SECTION_SUBJECT
                        }
                        else -> ExamQuestion.SECTION_COMMON
                    }
                }
                paperType == PaperType.CLASS_9_10_MATH -> {
                    if (normSubj == "Mathematics") {
                        if (commonMathCount < 8) {
                            commonMathCount++
                            ExamQuestion.SECTION_COMMON
                        } else {
                            ExamQuestion.SECTION_SUBJECT
                        }
                    } else {
                        ExamQuestion.SECTION_COMMON
                    }
                }
                paperType.isSeparateSubjectPaper() -> ExamQuestion.SECTION_SUBJECT
                paperType == PaperType.LANGUAGE -> ExamQuestion.SECTION_LANGUAGE
                else -> ExamQuestion.SECTION_COMMON
            }

            rawQ.copy(
                subject = normSubj,
                optionE = ExamQuestion.normalizeOptionE(rawQ.optionE),
                section = targetSec
            )
        }
    }

    /**
     * Auto-partitions a batch of imported or already-loaded questions (e.g., 70-Q or 150-Q JSON/CSV)
     * into their true sections (Part 1 Language, Part 2 Common, Part 3 Subject).
     *
     * When [forceAutoSplit] is true (used by the "Fix Section / Auto-Fix" button), existing
     * section tags are ignored and every question is cleanly normalized and auto-split.
     */
    fun partitionImportedQuestions(
        questions: List<ExamQuestion>,
        targetPaperType: PaperType,
        forceAutoSplit: Boolean = false,
        existingCommonMathCount: Int = 0,
        existingCommonScienceCount: Int = 0
    ): SectionPartition {
        // Auto-detect legacy misrouted 150-Q loads where all 48 Math & 48 Science were dumped into Part 2 Common with 0 in Part 3
        val shouldResplitLegacyCommon = !forceAutoSplit && targetPaperType.isFull150Paper() && run {
            val subjectSectionCount = questions.count { normalizePartOrSection(it.section) == ExamQuestion.SECTION_SUBJECT }
            val commonMath = questions.count {
                normalizePartOrSection(it.section) == ExamQuestion.SECTION_COMMON &&
                    normalizeSubject(it.subject, targetPaperType) == "Mathematics"
            }
            val commonSci = questions.count {
                normalizePartOrSection(it.section) == ExamQuestion.SECTION_COMMON &&
                    normalizeSubject(it.subject, targetPaperType) == "Science"
            }
            subjectSectionCount == 0 && (commonMath > 8 || commonSci > 8)
        }

        val paired = questions.map { rawQ ->
            val explicitSec = when {
                forceAutoSplit -> ""
                shouldResplitLegacyCommon && normalizePartOrSection(rawQ.section) == ExamQuestion.SECTION_COMMON -> ""
                else -> normalizePartOrSection(rawQ.section)
            }
            rawQ to explicitSec
        }

        val routed = assignSectionsWithExplicitHints(
            entries = paired,
            paperType = targetPaperType,
            existingCommonMathCount = if (forceAutoSplit) 0 else existingCommonMathCount,
            existingCommonScienceCount = if (forceAutoSplit) 0 else existingCommonScienceCount
        )

        val lang = mutableListOf<ExamQuestion>()
        val common = mutableListOf<ExamQuestion>()
        val subject = mutableListOf<ExamQuestion>()

        routed.forEach { q ->
            when (q.section) {
                ExamQuestion.SECTION_LANGUAGE -> lang.add(q)
                ExamQuestion.SECTION_SUBJECT -> {
                    if (targetPaperType.isFull150Paper() || targetPaperType.isSeparateSubjectPaper()) {
                        subject.add(q)
                    } else {
                        common.add(q.copy(section = ExamQuestion.SECTION_COMMON))
                    }
                }
                else -> common.add(q.copy(section = ExamQuestion.SECTION_COMMON))
            }
        }

        return SectionPartition(
            language = lang.mapIndexed { idx, q -> q.copy(number = idx + 1) },
            common = common.mapIndexed { idx, q -> q.copy(number = idx + 1) },
            subject = subject.mapIndexed { idx, q -> q.copy(number = idx + 1) }
        )
    }

    /**
     * Reassigns all [UNCLASSIFIED_SUBJECT] (or selected indices) questions to a chosen canonical subject
     * and updates their section accordingly.
     */
    fun bulkReassignSubject(
        questions: List<ExamQuestion>,
        targetSubject: String,
        indices: Set<Int>? = null,
        onlyUnclassified: Boolean = false,
        paperType: PaperType = PaperType.COMBINED
    ): List<ExamQuestion> {
        val canonical = normalizeSubject(targetSubject, paperType)
        return questions.mapIndexed { idx, q ->
            val currentNorm = normalizeSubject(q.subject, paperType)
            val shouldUpdate = when {
                indices != null -> idx in indices
                onlyUnclassified -> currentNorm == UNCLASSIFIED_SUBJECT
                else -> true
            }
            if (!shouldUpdate) {
                q
            } else {
                val updatedSection = when {
                    isLanguageSubject(canonical) -> ExamQuestion.SECTION_LANGUAGE
                    paperType.isSeparateSubjectPaper() -> ExamQuestion.SECTION_SUBJECT
                    q.section.equals(ExamQuestion.SECTION_SUBJECT, ignoreCase = true) -> ExamQuestion.SECTION_SUBJECT
                    else -> ExamQuestion.SECTION_COMMON
                }
                q.copy(subject = canonical, section = updatedSection)
            }
        }
    }

    fun parseBulkText(rawText: String, defaultPaperType: PaperType): List<ExamQuestion> {
        val cleaned = rawText.replace("\r\n", "\n").trim()
        if (cleaned.isBlank()) return emptyList()

        val blocks = cleaned.split(Regex("(?m)^(?=\\s*(?:Q\\.?\\s*)?\\d+[.)]\\s+)"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val rawPairs = mutableListOf<Pair<ExamQuestion, String>>()
        var carryPassage = ""
        blocks.forEachIndexed { idx, block ->
            val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) return@forEachIndexed

            val qTextBuilder = StringBuilder()
            var optA = ""
            var optB = ""
            var optC = ""
            var optD = ""
            var optE = ExamQuestion.DEFAULT_OPTION_E
            var answer = ""
            var explanation = ""
            var rawSubject = ""
            var topic = ""
            var source = ""
            var passage = ""
            var explicitSection = ""

            var readingOptions = false

            lines.forEach { line ->
                val lower = line.lowercase()
                when {
                    Regex("^(?:\\(?A\\)?[.):\\-]\\s+)(.+)").containsMatchIn(line) -> {
                        readingOptions = true
                        optA = line.replaceFirst(Regex("^\\(?A\\)?[.):\\-]\\s+"), "").trim()
                    }
                    Regex("^(?:\\(?B\\)?[.):\\-]\\s+)(.+)").containsMatchIn(line) -> {
                        readingOptions = true
                        optB = line.replaceFirst(Regex("^\\(?B\\)?[.):\\-]\\s+"), "").trim()
                    }
                    Regex("^(?:\\(?C\\)?[.):\\-]\\s+)(.+)").containsMatchIn(line) -> {
                        readingOptions = true
                        optC = line.replaceFirst(Regex("^\\(?C\\)?[.):\\-]\\s+"), "").trim()
                    }
                    Regex("^(?:\\(?D\\)?[.):\\-]\\s+)(.+)").containsMatchIn(line) -> {
                        readingOptions = true
                        optD = line.replaceFirst(Regex("^\\(?D\\)?[.):\\-]\\s+"), "").trim()
                    }
                    Regex("^(?:\\(?E\\)?[.):\\-]\\s+)(.+)").containsMatchIn(line) -> {
                        readingOptions = true
                        optE = ExamQuestion.normalizeOptionE(
                            line.replaceFirst(Regex("^\\(?E\\)?[.):\\-]\\s+"), "").trim()
                        )
                    }
                    lower.startsWith("ans") || lower.startsWith("correct") || lower.startsWith("उत्तर") -> {
                        val cleanedAnsLine = line.replaceFirst(
                            Regex("^(?:correct\\s+answer|correct\\s+option|correct|answer|ans|उत्तर)\\s*[:\\-=]?\\s*", RegexOption.IGNORE_CASE),
                            ""
                        )
                        val match = Regex("\\b([A-Da-d])\\b|\\(([A-Da-d])\\)|^([A-Da-d])").find(cleanedAnsLine)
                        answer = match?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }?.uppercase() ?: ""
                    }
                    lower.startsWith("exp") || lower.startsWith("व्याख्या") || lower.startsWith("solution") -> {
                        explanation = line.substringAfter(":", "").trim()
                    }
                    lower.startsWith("subject") || lower.startsWith("विषय") -> {
                        rawSubject = line.substringAfter(":", "").trim()
                    }
                    lower.startsWith("topic") || lower.startsWith("अध्याय") -> {
                        topic = line.substringAfter(":", "").trim()
                    }
                    lower.startsWith("source") || lower.startsWith("स्रोत") -> {
                        source = line.substringAfter(":", "").trim()
                    }
                    lower.startsWith("passage") || lower.startsWith("गद्यांश") -> {
                        passage = line.substringAfter(":", "").trim()
                        if (passage.isNotBlank()) carryPassage = passage
                    }
                    lower.startsWith("part") || lower.startsWith("section") || lower.startsWith("खंड") || lower.startsWith("भाग") -> {
                        explicitSection = normalizePartOrSection(line.substringAfter(":", "").trim())
                    }
                    !readingOptions -> {
                        val stripped = line.replaceFirst(Regex("^(?:Q\\.?\\s*)?\\d+[.)]\\s+"), "").trim()
                        if (qTextBuilder.isNotEmpty()) qTextBuilder.append(" ")
                        qTextBuilder.append(stripped)
                    }
                }
            }

            if (qTextBuilder.isNotBlank() && optA.isNotBlank()) {
                val resolvedSubject = if (rawSubject.isBlank()) {
                    when (defaultPaperType) {
                        PaperType.CLASS_6_8_MATH, PaperType.CLASS_9_10_MATH_80 -> "Mathematics"
                        PaperType.CLASS_6_8_SCIENCE -> "Science"
                        else -> UNCLASSIFIED_SUBJECT
                    }
                } else {
                    normalizeSubject(rawSubject, defaultPaperType)
                }
                rawPairs.add(
                    ExamQuestion(
                        number = idx + 1,
                        subject = resolvedSubject,
                        topic = topic,
                        source = source,
                        text = qTextBuilder.toString().trim(),
                        optionA = optA,
                        optionB = optB,
                        optionC = optC,
                        optionD = optD,
                        optionE = ExamQuestion.normalizeOptionE(optE),
                        correctOption = answer, // NEVER silently default to "A"
                        explanation = explanation,
                        section = explicitSection,
                        passage = passage.ifBlank {
                            if (topic.contains("passage", ignoreCase = true) || topic.contains("गद्यांश")) carryPassage else ""
                        }
                    ) to explicitSection
                )
            }
        }
        return assignSectionsWithExplicitHints(rawPairs, defaultPaperType)
    }

    /**
     * Parses standard JSON (array or object with questions/data/items/sections) into ExamQuestion list.
     * - Supports object-based options {"A":...,"B":...,"C":...,"D":...} and array-based options.
     * - Option E is NOT required in JSON; automatically appended as "प्रयास नहीं किया गया / Not Attempted".
     * - Preserves exact subject mapping; unknown/missing subject is marked "Unclassified" (NEVER Mathematics).
     * - Strictly validates question text, Options A-D, and correctAnswer (A/B/C/D) without defaulting to A.
     */
    fun parseJson(rawJson: String, defaultPaperType: PaperType): Result<List<ExamQuestion>> {
        return try {
            val trimmed = rawJson.removePrefix("\uFEFF").trim()
            if (trimmed.isBlank()) {
                return Result.failure(IllegalArgumentException("JSON input is empty."))
            }

            val entries = mutableListOf<Triple<JSONObject, String, String>>()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i)
                        ?: return Result.failure(IllegalArgumentException("JSON item #${i + 1} is not a valid question object."))
                    entries.add(Triple(item, "", ""))
                }
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                val directArray = root.optJSONArray("questions")
                    ?: root.optJSONArray("data")
                    ?: root.optJSONArray("items")
                val sectionsArray = root.optJSONArray("sections")
                when {
                    directArray != null -> {
                        val rootSubj = safeJsonString(root, "subject", "category")
                        val rootPart = safeJsonString(root, "part", "section", "paperSection")
                        for (i in 0 until directArray.length()) {
                            val item = directArray.optJSONObject(i)
                                ?: return Result.failure(IllegalArgumentException("JSON item #${i + 1} is not a valid question object."))
                            entries.add(Triple(item, rootPart, rootSubj))
                        }
                    }
                    sectionsArray != null -> {
                        for (s in 0 until sectionsArray.length()) {
                            val secObj = sectionsArray.optJSONObject(s) ?: continue
                            val secHint = safeJsonString(secObj, "part", "section", "type", "name")
                            val secSubjHint = safeJsonString(secObj, "subject", "category")
                            val secQs = secObj.optJSONArray("questions") ?: continue
                            for (i in 0 until secQs.length()) {
                                val item = secQs.optJSONObject(i) ?: continue
                                entries.add(Triple(item, secHint, secSubjHint))
                            }
                        }
                    }
                    else -> {
                        return Result.failure(
                            IllegalArgumentException(
                                "JSON object must contain a 'questions' (or 'data' / 'sections') array."
                            )
                        )
                    }
                }
            } else {
                return Result.failure(IllegalArgumentException("Invalid JSON: must start with '[' or '{'."))
            }

            if (entries.isEmpty()) {
                return Result.failure(IllegalArgumentException("JSON contains 0 questions."))
            }

            val rawPairs = mutableListOf<Pair<ExamQuestion, String>>()
            val validationErrors = mutableListOf<String>()

            entries.forEachIndexed { i, (obj, parentSectionHint, parentSubjectHint) ->
                val rawNum = obj.optInt("number", obj.optInt("questionNumber", obj.optInt("qNo", obj.optInt("id", i + 1))))
                val qNum = if (rawNum > 0) rawNum else (i + 1)

                val qHi = safeJsonString(obj, "question_hi", "questionHi")
                val qEn = safeJsonString(obj, "question_en", "questionEn")
                val qGeneric = safeJsonString(obj, "question", "text", "questionText", "question_text", "prompt")
                val qText = when {
                    qHi.isNotBlank() && qEn.isNotBlank() && qHi != qEn -> "$qHi\n$qEn"
                    qGeneric.isNotBlank() -> qGeneric
                    qHi.isNotBlank() -> qHi
                    else -> qEn
                }

                val extractedOptions = extractOptionsFromJsonObject(obj)
                val optA = extractedOptions["A"].orEmpty()
                val optB = extractedOptions["B"].orEmpty()
                val optC = extractedOptions["C"].orEmpty()
                val optD = extractedOptions["D"].orEmpty()
                val rawOptE = extractedOptions["E"].orEmpty()
                val optE = ExamQuestion.normalizeOptionE(rawOptE)

                val correctKey = extractCorrectAnswerFromJsonObject(obj)

                if (qText.isBlank()) validationErrors.add("Q$qNum: Missing question text.")
                if (optA.isBlank()) validationErrors.add("Q$qNum: Missing Option A.")
                if (optB.isBlank()) validationErrors.add("Q$qNum: Missing Option B.")
                if (optC.isBlank()) validationErrors.add("Q$qNum: Missing Option C.")
                if (optD.isBlank()) validationErrors.add("Q$qNum: Missing Option D.")
                if (correctKey == null) {
                    val rawAnsDisplay = safeJsonString(
                        obj,
                        "correctAnswer", "correctOption", "correct_answer", "correct_option",
                        "answer", "answerKey", "answer_key", "correct"
                    )
                    val detail = if (rawAnsDisplay.isBlank()) "missing" else "'$rawAnsDisplay'"
                    validationErrors.add("Q$qNum: Invalid or missing correctAnswer ($detail — must be A, B, C, or D).")
                }

                val rawSubject = safeJsonString(obj, "subject", "category", "subjectName", "subject_name")
                    .ifBlank { parentSubjectHint }
                val finalSubj = if (rawSubject.isBlank()) {
                    when (defaultPaperType) {
                        PaperType.CLASS_6_8_MATH, PaperType.CLASS_9_10_MATH_80 -> "Mathematics"
                        PaperType.CLASS_6_8_SCIENCE -> "Science"
                        else -> UNCLASSIFIED_SUBJECT
                    }
                } else {
                    normalizeSubject(rawSubject, defaultPaperType)
                }

                val rawPartOrSection = safeJsonString(
                    obj,
                    "part", "section", "paperSection", "paper_section", "partName", "part_name", "paper"
                ).ifBlank { parentSectionHint }
                val normalizedExplicitSec = normalizePartOrSection(rawPartOrSection)

                val explHi = safeJsonString(obj, "solution_hi", "explanation_hi")
                val explEn = safeJsonString(obj, "solution_en", "explanation_en")
                val explGeneric = safeJsonString(obj, "explanation", "solution", "rationale")
                val finalExplanation = when {
                    explHi.isNotBlank() && explEn.isNotBlank() && explHi != explEn -> "$explHi\n$explEn"
                    explGeneric.isNotBlank() -> explGeneric
                    explHi.isNotBlank() -> explHi
                    else -> explEn
                }

                rawPairs.add(
                    ExamQuestion(
                        number = qNum,
                        subject = finalSubj,
                        topic = safeJsonString(obj, "topic", "chapter", "subtopic"),
                        source = safeJsonString(obj, "source", "reference"),
                        text = qText,
                        optionA = optA,
                        optionB = optB,
                        optionC = optC,
                        optionD = optD,
                        optionE = optE,
                        correctOption = correctKey.orEmpty(),
                        explanation = finalExplanation,
                        section = normalizedExplicitSec,
                        passage = safeJsonString(obj, "passage", "comprehension", "paragraph")
                    ) to normalizedExplicitSec
                )
            }

            if (validationErrors.isNotEmpty()) {
                val preview = validationErrors.take(8).joinToString("\n• ", prefix = "• ")
                val more = if (validationErrors.size > 8) "\n...and ${validationErrors.size - 8} more issue(s)." else ""
                return Result.failure(
                    IllegalArgumentException("JSON validation failed (${validationErrors.size} issue(s)):\n$preview$more")
                )
            }

            val routed = assignSectionsWithExplicitHints(rawPairs, defaultPaperType)
            Result.success(routed)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid JSON format: ${e.message}"))
        }
    }

    private fun safeJsonString(obj: JSONObject, vararg keys: String): String {
        for (key in keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                val raw = obj.opt(key)
                if (raw is String && raw.isNotBlank()) return raw.trim()
                if (raw is Number || raw is Boolean) return raw.toString().trim()
            }
        }
        return ""
    }

    /**
     * Extracts Options A–E from BOTH flat keys (optionA..optionE, option_a..option_e, optA..optE, A..E, a..e)
     * AND nested objects/arrays (options: {"A": "...", "B": "...", "C": "...", "D": "..."} or choices: [...]).
     */
    fun extractOptionsFromJsonObject(obj: JSONObject): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val letters = listOf("A", "B", "C", "D", "E")

        val optionsObj = obj.optJSONObject("options") ?: obj.optJSONObject("choices")
        if (optionsObj != null) {
            letters.forEachIndexed { idx, letter ->
                val value = safeJsonString(
                    optionsObj,
                    letter,
                    letter.lowercase(),
                    "option$letter",
                    "option_${letter.lowercase()}",
                    "opt$letter",
                    (idx + 1).toString()
                )
                if (value.isNotBlank()) result[letter] = value
            }
        }

        val optionsArr = obj.optJSONArray("options") ?: obj.optJSONArray("choices")
        if (optionsArr != null) {
            for (idx in 0 until optionsArr.length()) {
                val elem = optionsArr.opt(idx)
                when (elem) {
                    is JSONObject -> {
                        val keyRaw = safeJsonString(elem, "key", "label", "id", "optionKey", "code").uppercase()
                        val mappedLetter = when {
                            keyRaw in letters -> keyRaw
                            idx in letters.indices -> letters[idx]
                            else -> null
                        }
                        val textVal = safeJsonString(elem, "text", "value", "option", "labelText", "content")
                        if (mappedLetter != null && textVal.isNotBlank() && result[mappedLetter].isNullOrBlank()) {
                            result[mappedLetter] = textVal
                        }
                    }
                    is String, is Number, is Boolean -> {
                        val textVal = elem.toString().trim()
                        if (idx in letters.indices && textVal.isNotBlank() && result[letters[idx]].isNullOrBlank()) {
                            result[letters[idx]] = textVal
                        }
                    }
                }
            }
        }

        letters.forEach { letter ->
            if (result[letter].isNullOrBlank()) {
                val flat = safeJsonString(
                    obj,
                    "option$letter",
                    "option_${letter.lowercase()}",
                    "opt$letter",
                    letter,
                    letter.lowercase()
                )
                if (flat.isNotBlank()) result[letter] = flat
            }
        }

        return result
    }

    fun extractCorrectAnswerFromJsonObject(obj: JSONObject): String? {
        val candidateKeys = listOf(
            "correctAnswer",
            "correctOption",
            "correct_answer",
            "correct_option",
            "answer",
            "answerKey",
            "answer_key",
            "correct"
        )
        for (key in candidateKeys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val raw = obj.opt(key)
            when (raw) {
                is Number -> {
                    val idx = raw.toInt()
                    if (idx in 0..3) return listOf("A", "B", "C", "D")[idx]
                }
                is String -> {
                    val parsed = normalizeAnswerKey(raw)
                    if (parsed != null) return parsed
                }
            }
        }
        return null
    }

    private fun normalizeAnswerKey(raw: String): String? {
        val cleaned = raw.trim().uppercase()
        if (cleaned in listOf("A", "B", "C", "D")) return cleaned
        val stripped = cleaned
            .removePrefix("OPTION")
            .removePrefix("OPT")
            .trim()
            .trim('(', ')', '.', ':', '-', ' ')
        return if (stripped in listOf("A", "B", "C", "D")) stripped else null
    }

    /**
     * Header-aware CSV importer with full backward compatibility for legacy positional CSVs.
     * Supports explicit `part` / `section` columns (normalized flexibly) and Smart Auto-Split fallback
     * when `part` is omitted.
     */
    fun parseCsv(rawCsv: String, defaultPaperType: PaperType): List<ExamQuestion> {
        val lines = rawCsv.removePrefix("\uFEFF").replace("\r\n", "\n").lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val headerCols = splitCsvRow(lines.first()).map { col ->
            col.lowercase().replace(Regex("[^a-z0-9]"), "")
        }

        fun findCol(vararg aliases: String): Int =
            headerCols.indexOfFirst { it in aliases }

        val idxPart = findCol("part", "section", "papersection", "partname", "paper")
        val idxSubject = findCol("subject", "category", "subjectname")
        val idxTopic = findCol("topic", "chapter", "subtopic")
        val idxSource = findCol("source", "reference")
        val idxQuestion = findCol("question", "text", "questiontext", "prompt")
        val idxOptA = findCol("optiona", "opta", "a")
        val idxOptB = findCol("optionb", "optb", "b")
        val idxOptC = findCol("optionc", "optc", "c")
        val idxOptD = findCol("optiond", "optd", "d")
        val idxOptE = findCol("optione", "opte", "e")
        val idxAnswer = findCol("correctanswer", "correctoption", "answer", "answerkey", "correct")
        val idxExplanation = findCol("explanation", "solution", "rationale")
        val idxPassage = findCol("passage", "comprehension", "paragraph")

        val useHeaderMapping = idxQuestion >= 0 && idxOptA >= 0 && idxOptB >= 0 && idxOptC >= 0 && idxOptD >= 0

        fun colValue(cols: List<String>, idx: Int): String =
            if (idx >= 0 && idx < cols.size) cols[idx].trim() else ""

        val rawPairs = mutableListOf<Pair<ExamQuestion, String>>()
        lines.drop(1).forEachIndexed { idx, line ->
            val cols = splitCsvRow(line)
            val minCols = if (useHeaderMapping) 5 else 8
            if (cols.size >= minCols) {
                val rawSubj = if (useHeaderMapping) colValue(cols, idxSubject) else cols.getOrElse(0) { "" }.trim()
                val finalSubj = if (rawSubj.isBlank()) {
                    when (defaultPaperType) {
                        PaperType.CLASS_6_8_MATH, PaperType.CLASS_9_10_MATH_80 -> "Mathematics"
                        PaperType.CLASS_6_8_SCIENCE -> "Science"
                        else -> UNCLASSIFIED_SUBJECT
                    }
                } else {
                    normalizeSubject(rawSubj, defaultPaperType)
                }

                val rawPartOrSection = if (useHeaderMapping) {
                    colValue(cols, idxPart)
                } else {
                    cols.getOrElse(11) { "" }.trim()
                }
                val explicitSec = normalizePartOrSection(rawPartOrSection)

                val rawAns = if (useHeaderMapping) {
                    colValue(cols, idxAnswer)
                } else {
                    cols.getOrElse(9) { "" }.trim()
                }
                val validAns = normalizeAnswerKey(rawAns).orEmpty()

                val qText = if (useHeaderMapping) colValue(cols, idxQuestion) else cols.getOrElse(3) { "" }
                val optA = if (useHeaderMapping) colValue(cols, idxOptA) else cols.getOrElse(4) { "" }
                val optB = if (useHeaderMapping) colValue(cols, idxOptB) else cols.getOrElse(5) { "" }
                val optC = if (useHeaderMapping) colValue(cols, idxOptC) else cols.getOrElse(6) { "" }
                val optD = if (useHeaderMapping) colValue(cols, idxOptD) else cols.getOrElse(7) { "" }
                val optE = ExamQuestion.normalizeOptionE(
                    if (useHeaderMapping) colValue(cols, idxOptE) else cols.getOrElse(8) { "" }
                )
                val topic = if (useHeaderMapping) colValue(cols, idxTopic) else cols.getOrElse(1) { "" }
                val source = if (useHeaderMapping) colValue(cols, idxSource) else cols.getOrElse(2) { "" }
                val explanation = if (useHeaderMapping) colValue(cols, idxExplanation) else cols.getOrElse(10) { "" }
                val passage = if (useHeaderMapping) colValue(cols, idxPassage) else cols.getOrElse(12) { "" }

                if (qText.isNotBlank() || optA.isNotBlank()) {
                    rawPairs.add(
                        ExamQuestion(
                            number = idx + 1,
                            subject = finalSubj,
                            topic = topic,
                            source = source,
                            text = qText,
                            optionA = optA,
                            optionB = optB,
                            optionC = optC,
                            optionD = optD,
                            optionE = optE,
                            correctOption = validAns,
                            explanation = explanation,
                            section = explicitSec,
                            passage = passage
                        ) to explicitSec
                    )
                }
            }
        }
        return assignSectionsWithExplicitHints(rawPairs, defaultPaperType)
    }

    /**
     * Strictly combines 40 Common Paper questions and 30 Language Qualifying questions
     * without mixing sections or altering any question's answer key.
     */
    fun combineSections(
        commonQuestions: List<ExamQuestion>,
        languageQuestions: List<ExamQuestion>
    ): CombinedAssemblyResult {
        val errors = mutableListOf<String>()

        if (commonQuestions.size != 40) {
            errors.add("Common Paper section must contain exactly 40 questions (currently ${commonQuestions.size}).")
        }
        if (languageQuestions.size != 30) {
            errors.add("Language Paper section must contain exactly 30 questions (currently ${languageQuestions.size}).")
        }

        val badCommonSubjects = commonQuestions.filter { isLanguageSubject(it.subject) }
        if (badCommonSubjects.isNotEmpty()) {
            errors.add("Common Paper section contains ${badCommonSubjects.size} Language question(s). Sections must not be mixed.")
        }

        val badLangSubjects = languageQuestions.filter { !isLanguageSubject(it.subject) }
        if (badLangSubjects.isNotEmpty()) {
            errors.add("Language Paper section contains ${badLangSubjects.size} non-Language question(s). Sections must not be mixed.")
        }

        val taggedCommon = commonQuestions.mapIndexed { idx, q ->
            q.copy(
                number = idx + 1,
                subject = normalizeSubject(q.subject, PaperType.COMMON_GS),
                optionE = ExamQuestion.normalizeOptionE(q.optionE),
                correctOption = q.correctOption.uppercase().trim(),
                section = ExamQuestion.SECTION_COMMON
            )
        }
        val taggedLang = languageQuestions.mapIndexed { idx, q ->
            q.copy(
                number = taggedCommon.size + idx + 1,
                subject = normalizeSubject(q.subject, PaperType.LANGUAGE),
                optionE = ExamQuestion.normalizeOptionE(q.optionE),
                correctOption = q.correctOption.uppercase().trim(),
                section = ExamQuestion.SECTION_LANGUAGE
            )
        }

        val combined = taggedCommon + taggedLang
        val validation = validate(combined, PaperType.COMBINED)
        val allErrors = (errors + validation.errors).distinct()

        return CombinedAssemblyResult(
            valid = allErrors.isEmpty(),
            questions = combined,
            errors = allErrors,
            commonCount = taggedCommon.size,
            languageCount = taggedLang.size
        )
    }

    /**
     * Strictly combines 30 Language + 40 Common + 80 Subject questions into a 150-question paper
     * without mixing sections or answer keys.
     */
    fun combineFull150Sections(
        languageQuestions: List<ExamQuestion>,
        commonQuestions: List<ExamQuestion>,
        subjectQuestions: List<ExamQuestion>,
        targetType: PaperType
    ): CombinedAssemblyResult {
        require(targetType.isFull150Paper()) { "Target type must be a 150-question paper." }
        val errors = mutableListOf<String>()

        if (languageQuestions.size != 30) {
            errors.add("Part 1 (Language Paper) must contain exactly 30 questions (currently ${languageQuestions.size}).")
        }
        if (commonQuestions.size != 40) {
            errors.add("Part 2 (Common Paper) must contain exactly 40 questions (currently ${commonQuestions.size}).")
        }
        if (subjectQuestions.size != 80) {
            errors.add("Part 3 (Subject Paper) must contain exactly 80 questions (currently ${subjectQuestions.size}).")
        }

        val taggedLang = languageQuestions.mapIndexed { idx, q ->
            q.copy(
                number = idx + 1,
                subject = normalizeSubject(q.subject, PaperType.LANGUAGE),
                optionE = ExamQuestion.normalizeOptionE(q.optionE),
                correctOption = q.correctOption.uppercase().trim(),
                section = ExamQuestion.SECTION_LANGUAGE
            )
        }
        val taggedCommon = commonQuestions.mapIndexed { idx, q ->
            q.copy(
                number = taggedLang.size + idx + 1,
                subject = normalizeSubject(q.subject, PaperType.COMMON_GS),
                optionE = ExamQuestion.normalizeOptionE(q.optionE),
                correctOption = q.correctOption.uppercase().trim(),
                section = ExamQuestion.SECTION_COMMON
            )
        }
        val taggedSubject = subjectQuestions.mapIndexed { idx, q ->
            val normSubj = normalizeSubject(q.subject, targetType)
            q.copy(
                number = taggedLang.size + taggedCommon.size + idx + 1,
                subject = normSubj,
                optionE = ExamQuestion.normalizeOptionE(q.optionE),
                correctOption = q.correctOption.uppercase().trim(),
                section = ExamQuestion.SECTION_SUBJECT
            )
        }

        val combined = taggedLang + taggedCommon + taggedSubject
        val validation = validate(combined, targetType)
        val allErrors = (errors + validation.errors).distinct()

        return CombinedAssemblyResult(
            valid = allErrors.isEmpty(),
            questions = combined,
            errors = allErrors,
            commonCount = taggedCommon.size,
            languageCount = taggedLang.size,
            subjectCount = taggedSubject.size
        )
    }

    /**
     * Blueprint-driven live validation for every BPSSC TRE 4.0 paper structure.
     */
    fun validate(questions: List<ExamQuestion>, paperType: PaperType): ValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val seenTexts = mutableSetOf<String>()
        val subjectCounts = linkedMapOf<String, Int>()
        val expectedMap = expectedBlueprint(paperType)

        var duplicateCount = 0
        var unclassifiedCount = 0
        var missingAnswersCount = 0
        var invalidOptionsCount = 0
        var hindiCount = 0
        var englishCount = 0
        var commonCount = 0
        var subjectPaperCount = 0
        var part1LanguageCount = 0
        val part2CommonSubjectCounts = linkedMapOf<String, Int>()
        val part3SubjectCounts = linkedMapOf<String, Int>()

        if (questions.isEmpty()) {
            errors.add("No questions found. Add or import questions before saving.")
        }

        questions.forEachIndexed { idx, q ->
            val num = idx + 1
            if (q.text.isBlank()) errors.add("Q$num: Missing question text.")
            if (q.optionA.isBlank() || q.optionB.isBlank() || q.optionC.isBlank() || q.optionD.isBlank()) {
                invalidOptionsCount++
                errors.add("Q$num: Options A, B, C, and D are all required.")
            }
            if (q.correctOption.uppercase().trim() !in listOf("A", "B", "C", "D")) {
                missingAnswersCount++
                errors.add("Q$num: Missing or invalid answer key '${q.correctOption}' (must be A, B, C, or D).")
            }
            if (q.explanation.isBlank()) {
                warnings.add("Q$num: Missing explanation.")
            }
            val normText = q.text.trim().lowercase()
            if (normText.isNotBlank() && !seenTexts.add(normText)) {
                duplicateCount++
                errors.add("Q$num: Duplicate question detected.")
            }

            val subj = normalizeSubject(q.subject, paperType)
            subjectCounts[subj] = (subjectCounts[subj] ?: 0) + 1
            if (subj == UNCLASSIFIED_SUBJECT) {
                unclassifiedCount++
            }
            when (subj) {
                "Hindi" -> hindiCount++
                "English" -> englishCount++
            }
            when (q.resolvedSection()) {
                ExamQuestion.SECTION_LANGUAGE -> part1LanguageCount++
                ExamQuestion.SECTION_COMMON -> {
                    if (!isLanguageSubject(subj)) commonCount++
                    part2CommonSubjectCounts[subj] = (part2CommonSubjectCounts[subj] ?: 0) + 1
                }
                ExamQuestion.SECTION_SUBJECT -> {
                    subjectPaperCount++
                    part3SubjectCounts[subj] = (part3SubjectCounts[subj] ?: 0) + 1
                }
            }
        }

        if (unclassifiedCount > 0) {
            errors.add("$unclassifiedCount question(s) are Unclassified. Use Bulk Subject Correction or Edit before publishing.")
        }

        fun checkLanguageBlueprint(langQs: List<ExamQuestion>, label: String) {
            val langCounts = langQs.groupingBy { normalizeSubject(it.subject, PaperType.LANGUAGE) }.eachCount()
            val hiCount = langCounts["Hindi"] ?: 0
            val enCount = langCounts["English"] ?: 0
            val otherCount = langQs.size - hiCount - enCount
            if (hiCount != 22) errors.add("$label requires 22 Hindi questions (currently $hiCount).")
            if (enCount != 8) errors.add("$label requires 8 English questions (currently $enCount).")
            if (otherCount > 0) errors.add("$label contains $otherCount non-Language question(s).")

            val enPassageCount = langQs.count {
                normalizeSubject(it.subject, PaperType.LANGUAGE) == "English" &&
                    (it.passage.isNotBlank() || it.topic.contains("passage", ignoreCase = true))
            }
            if (enCount == 8 && enPassageCount < 4) {
                warnings.add("$label: English recommends 1 short passage with 4 questions (detected $enPassageCount).")
            }
            val hiPassageCount = langQs.count {
                normalizeSubject(it.subject, PaperType.LANGUAGE) == "Hindi" &&
                    (it.passage.isNotBlank() || it.topic.contains("passage", ignoreCase = true) || it.topic.contains("गद्यांश"))
            }
            if (hiCount == 22 && hiPassageCount < 8) {
                warnings.add("$label: Hindi recommends 2 passages with 4 questions each (detected $hiPassageCount).")
            }
        }

        fun checkCommonBlueprint(commonQs: List<ExamQuestion>, label: String) {
            val commonCounts = commonQs.groupingBy { normalizeSubject(it.subject, PaperType.COMMON_GS) }.eachCount()
            GS_SUBJECTS.forEach { subj ->
                val count = commonCounts[subj] ?: 0
                if (count != 8) {
                    errors.add("$label requires 8 questions in $subj (currently $count).")
                }
            }
            val strayLang = (commonCounts["Hindi"] ?: 0) + (commonCounts["English"] ?: 0)
            if (strayLang > 0) {
                errors.add("$label contains $strayLang Language question(s).")
            }
        }

        when (paperType) {
            PaperType.LANGUAGE -> {
                if (questions.size != 30) {
                    errors.add("Language Paper requires exactly 30 questions (currently ${questions.size}).")
                }
                checkLanguageBlueprint(questions, "Language Paper")
            }
            PaperType.COMMON_GS -> {
                if (questions.size != 40) {
                    errors.add("Common Paper requires exactly 40 questions (currently ${questions.size}).")
                }
                checkCommonBlueprint(questions, "Common Paper")
            }
            PaperType.COMBINED -> {
                if (questions.size != 70) {
                    errors.add("Language + Common Paper requires exactly 70 questions: 30 Language + 40 Common (currently ${questions.size}).")
                }
                val langQs = questions.filter { it.resolvedSection() == ExamQuestion.SECTION_LANGUAGE || isLanguageSubject(it.subject) }
                val commonQs = questions.filter { it !in langQs }
                if (langQs.size != 30) errors.add("Language Section requires 30 questions (currently ${langQs.size}).")
                if (commonQs.size != 40) errors.add("Common Paper Section requires 40 questions (currently ${commonQs.size}).")
                checkLanguageBlueprint(langQs, "Language Section")
                checkCommonBlueprint(commonQs, "Common Section")
            }
            PaperType.CLASS_6_8_MATH -> {
                if (questions.size != 40) {
                    errors.add("Class 6–8 Mathematics requires exactly 40 questions (currently ${questions.size}).")
                }
                val mathCount = subjectCounts["Mathematics"] ?: 0
                if (mathCount != 40) {
                    errors.add("Class 6–8 Mathematics requires 40 Mathematics questions (currently $mathCount).")
                }
            }
            PaperType.CLASS_6_8_SCIENCE -> {
                if (questions.size != 40) {
                    errors.add("Class 6–8 Science requires exactly 40 questions (currently ${questions.size}).")
                }
                val sciCount = subjectCounts["Science"] ?: 0
                if (sciCount != 40) {
                    errors.add("Class 6–8 Science requires 40 Science questions (currently $sciCount).")
                }
            }
            PaperType.CLASS_9_10_MATH_80 -> {
                if (questions.size != 80) {
                    errors.add("Class 9–10 Mathematics requires exactly 80 questions (currently ${questions.size}).")
                }
                val mathCount = subjectCounts["Mathematics"] ?: 0
                if (mathCount != 80) {
                    errors.add("Class 9–10 Mathematics requires 80 Mathematics questions (currently $mathCount).")
                }
            }
            PaperType.CLASS_6_8_MATH_SCIENCE -> {
                if (questions.size != 150) {
                    errors.add("Class 6–8 Math & Science (150 Q) requires 30 Language + 40 Common + 40 Math + 40 Science (currently ${questions.size}).")
                }
                val langQs = questions.filter { it.resolvedSection() == ExamQuestion.SECTION_LANGUAGE }
                val commonQs = questions.filter { it.resolvedSection() == ExamQuestion.SECTION_COMMON }
                val subjectQs = questions.filter { it.resolvedSection() == ExamQuestion.SECTION_SUBJECT }
                if (langQs.size != 30) errors.add("Part 1 (Language) requires 30 questions (currently ${langQs.size}).")
                if (commonQs.size != 40) errors.add("Part 2 (Common) requires 40 questions (currently ${commonQs.size}).")
                if (subjectQs.size != 80) errors.add("Part 3 (Subject) requires 80 questions (currently ${subjectQs.size}).")
                checkLanguageBlueprint(langQs, "Part 1 Language")
                checkCommonBlueprint(commonQs, "Part 2 Common")
                val subMath = subjectQs.count { normalizeSubject(it.subject) == "Mathematics" }
                val subSci = subjectQs.count { normalizeSubject(it.subject) == "Science" }
                if (subMath != 40) errors.add("Part 3 (Class 6–8) requires 40 Mathematics questions (currently $subMath).")
                if (subSci != 40) errors.add("Part 3 (Class 6–8) requires 40 Science questions (currently $subSci).")
            }
            PaperType.CLASS_9_10_MATH -> {
                if (questions.size != 150) {
                    errors.add("Class 9–10 Mathematics (150 Q) requires 30 Language + 40 Common + 80 Mathematics (currently ${questions.size}).")
                }
                val langQs = questions.filter { it.resolvedSection() == ExamQuestion.SECTION_LANGUAGE }
                val commonQs = questions.filter { it.resolvedSection() == ExamQuestion.SECTION_COMMON }
                val subjectQs = questions.filter { it.resolvedSection() == ExamQuestion.SECTION_SUBJECT }
                if (langQs.size != 30) errors.add("Part 1 (Language) requires 30 questions (currently ${langQs.size}).")
                if (commonQs.size != 40) errors.add("Part 2 (Common) requires 40 questions (currently ${commonQs.size}).")
                if (subjectQs.size != 80) errors.add("Part 3 (Class 9–10 Math) requires 80 Mathematics questions (currently ${subjectQs.size}).")
                checkLanguageBlueprint(langQs, "Part 1 Language")
                checkCommonBlueprint(commonQs, "Part 2 Common")
                val subMath = subjectQs.count { normalizeSubject(it.subject) == "Mathematics" }
                if (subMath != 80) errors.add("Part 3 (Class 9–10) requires 80 Mathematics questions (currently $subMath).")
            }
            PaperType.PRACTICE -> {
                if (questions.size < 5) {
                    warnings.add("Practice sets work best with at least 5 questions.")
                }
            }
        }

        val expectedTotal = paperType.expectedQuestionCount
        val missingCount = if (expectedTotal > 0) (expectedTotal - questions.size).coerceAtLeast(0) else 0

        return ValidationReport(
            valid = errors.isEmpty(),
            errors = errors.distinct(),
            warnings = warnings.distinct(),
            subjectCounts = subjectCounts,
            expectedSubjectCounts = expectedMap,
            totalQuestions = questions.size,
            expectedTotal = expectedTotal,
            hindiCount = hindiCount,
            englishCount = englishCount,
            commonCount = commonCount,
            subjectPaperCount = subjectPaperCount,
            missingQuestions = missingCount,
            duplicateQuestions = duplicateCount,
            unclassifiedQuestions = unclassifiedCount,
            missingCorrectAnswers = missingAnswersCount,
            invalidOptions = invalidOptionsCount,
            part1LanguageCount = part1LanguageCount,
            part2CommonSubjectCounts = part2CommonSubjectCounts,
            part3SubjectCounts = part3SubjectCounts
        )
    }

    private fun splitCsvRow(line: String): List<String> {
        val cols = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    cur.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    cols.add(cur.toString().trim())
                    cur.clear()
                }
                else -> cur.append(ch)
            }
            i++
        }
        cols.add(cur.toString().trim())
        return cols
    }

    const val BULK_TEXT_TEMPLATE = """1. बिहार में 1857 के विद्रोह का नेतृत्व जगदीशपुर से किसने किया था?
A) पीर अली
B) वीर कुँवर सिंह
C) अमर सिंह
D) तात्या टोपे
Answer: B
Part: Part 2
Subject: History
Topic: Revolt of 1857
Explanation: जगदीशपुर (भोजपुर) के जमींदार बाबू वीर कुँवर सिंह ने 1857 के स्वतंत्रता संग्राम में बिहार का नेतृत्व किया।"""

    const val JSON_TEMPLATE = """{
  "questions": [
    {
      "part": "Part 1",
      "subject": "Hindi",
      "topic": "पर्यायवाची",
      "question": "'कमल' शब्द का पर्यायवाची शब्द कौन-सा है?",
      "optionA": "जलज",
      "optionB": "अनिल",
      "optionC": "पावक",
      "optionD": "नभ",
      "correctAnswer": "A",
      "explanation": "'जलज' कमल का पर्यायवाची शब्द है।"
    },
    {
      "part": "Part 2",
      "subject": "Mathematics",
      "topic": "Percentage",
      "question": "If 25% of a number is 75, what is the number?",
      "optionA": "250",
      "optionB": "300",
      "optionC": "325",
      "optionD": "350",
      "correctAnswer": "B",
      "explanation": "0.25 × x = 75 => x = 75 / 0.25 = 300."
    },
    {
      "part": "Part 3",
      "subject": "Science",
      "topic": "Electricity",
      "question": "What is the SI unit of electrical resistance?",
      "optionA": "Volt",
      "optionB": "Ampere",
      "optionC": "Ohm",
      "optionD": "Watt",
      "correctAnswer": "C",
      "explanation": "The SI unit of electrical resistance is Ohm (Ω)."
    }
  ]
}"""

    const val CSV_TEMPLATE =
        "part,subject,topic,source,question,optionA,optionB,optionC,optionD,optionE,correctAnswer,explanation\n" +
            "Part 1,English,Vocabulary,NCERT Class 8,\"Choose the correct synonym of 'Rapid'.\",Slow,Fast,Weak,Calm,\"प्रयास नहीं किया गया / Not Attempted\",B,\"'Fast' is the closest synonym of 'Rapid'.\"\n" +
            "Part 2,Geography,Rivers of Bihar,Bihar SCERT Class 8,\"Which river is known as the Sorrow of Bihar?\",Ganga,Kosi,Son,Gandak,\"प्रयास नहीं किया गया / Not Attempted\",B,\"Kosi frequently changes course causing floods in North Bihar.\"\n" +
            "Part 3,Mathematics,Algebra,NCERT Class 8,\"If 3x + 5 = 20, what is the value of x?\",3,4,5,6,\"प्रयास नहीं किया गया / Not Attempted\",C,\"3x = 15, so x = 5.\""
}
