package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Strictly two visible platform roles: ADMIN and USER.
 * Internal Firestore enum names ('ASPIRANT', 'GROUP_ADMIN', 'SUPER_ADMIN') are preserved
 * for 100% backward compatibility with existing Firestore records and security rules.
 */
enum class AppRole(val labelEn: String, val labelHi: String) {
    ASPIRANT("User", "यूज़र"),
    QUESTION_CREATOR("User", "यूज़र"),
    GROUP_ADMIN("Admin", "एडमिन"),
    SUPER_ADMIN("Main Admin", "मुख्य एडमिन");

    fun isAdminRole(): Boolean = this == GROUP_ADMIN || this == SUPER_ADMIN

    companion object {
        fun fromString(raw: String?): AppRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ASPIRANT
    }
}

enum class PaperType(
    val titleEn: String,
    val titleHi: String,
    val defaultDurationMinutes: Int,
    val expectedQuestionCount: Int,
    val baseSetName: String
) {
    LANGUAGE(
        titleEn = "Language Paper (30 Q • 15 Min)",
        titleHi = "भाषा पेपर (30 प्रश्न • 15 मिनट)",
        defaultDurationMinutes = 15,
        expectedQuestionCount = 30,
        baseSetName = "Language Paper"
    ),
    COMMON_GS(
        titleEn = "Common Paper (40 Q • 30 Min)",
        titleHi = "कॉमन पेपर (40 प्रश्न • 30 मिनट)",
        defaultDurationMinutes = 30,
        expectedQuestionCount = 40,
        baseSetName = "Common Paper"
    ),
    COMBINED(
        titleEn = "Language + Common Paper (70 Q • 45 Min)",
        titleHi = "भाषा + कॉमन पेपर (70 प्रश्न • 45 मिनट)",
        defaultDurationMinutes = 45,
        expectedQuestionCount = 70,
        baseSetName = "Language + Common Paper"
    ),
    CLASS_6_8_MATH(
        titleEn = "Class 6–8 Mathematics (40 Q • 40 Min)",
        titleHi = "कक्षा 6–8 गणित (40 प्रश्न • 40 मिनट)",
        defaultDurationMinutes = 40,
        expectedQuestionCount = 40,
        baseSetName = "Class 6–8 Mathematics"
    ),
    CLASS_6_8_SCIENCE(
        titleEn = "Class 6–8 Science (40 Q • 40 Min)",
        titleHi = "कक्षा 6–8 विज्ञान (40 प्रश्न • 40 मिनट)",
        defaultDurationMinutes = 40,
        expectedQuestionCount = 40,
        baseSetName = "Class 6–8 Science"
    ),
    CLASS_9_10_MATH_80(
        titleEn = "Class 9–10 Mathematics (80 Q • 80 Min)",
        titleHi = "कक्षा 9–10 गणित (80 प्रश्न • 80 मिनट)",
        defaultDurationMinutes = 80,
        expectedQuestionCount = 80,
        baseSetName = "Class 9–10 Mathematics (80 Q)"
    ),
    CLASS_6_8_MATH_SCIENCE(
        titleEn = "Class 6 to 8 Mathematics and Science (150 Q • 2 Hr)",
        titleHi = "कक्षा 6 से 8 गणित एवं विज्ञान (150 प्रश्न • 2 घंटे)",
        defaultDurationMinutes = 120,
        expectedQuestionCount = 150,
        baseSetName = "Class 6 to 8 Mathematics and Science"
    ),
    CLASS_9_10_MATH(
        titleEn = "Class 9-10 Mathematics (150 Q • 2 Hr 15 Min)",
        titleHi = "कक्षा 9-10 गणित (150 प्रश्न • 2 घंटे 15 मिनट)",
        defaultDurationMinutes = 135,
        expectedQuestionCount = 150,
        baseSetName = "Class 9-10 Mathematics"
    ),
    PRACTICE(
        titleEn = "Practice Set (Custom)",
        titleHi = "अभ्यास सेट",
        defaultDurationMinutes = 20,
        expectedQuestionCount = 0,
        baseSetName = "Practice Set"
    );

    fun hasLanguageSection(): Boolean =
        this == LANGUAGE || this == COMBINED || this == CLASS_6_8_MATH_SCIENCE || this == CLASS_9_10_MATH

    fun isFull150Paper(): Boolean =
        this == CLASS_6_8_MATH_SCIENCE || this == CLASS_9_10_MATH

    fun isSeparateSubjectPaper(): Boolean =
        this == CLASS_6_8_MATH || this == CLASS_6_8_SCIENCE || this == CLASS_9_10_MATH_80

    companion object {
        fun fromString(raw: String?): PaperType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: COMMON_GS
    }
}

/**
 * Complete 6-Part Testbook-grade Exam Set Architecture for BPSSC TRE 4.0,
 * mapped 1:1 to existing Firestore PaperType values for 100% backward compatibility.
 */
enum class SetType(
    val shortLabel: String,
    val titleEn: String,
    val titleHi: String,
    val expectedQuestions: Int,
    val meritMaxMarks: Double,
    val isQualifyingOnly: Boolean,
    val hasLanguageSection: Boolean,
    val mappedPaperType: PaperType,
    val classTag: String
) {
    PART_1_COMMON(
        shortLabel = "Part 1",
        titleEn = "Part 1 – Common Paper (40 Q)",
        titleHi = "भाग 1 – कॉमन पेपर (40 प्रश्न)",
        expectedQuestions = 40,
        meritMaxMarks = 40.0,
        isQualifyingOnly = false,
        hasLanguageSection = false,
        mappedPaperType = PaperType.COMMON_GS,
        classTag = "All Classes"
    ),
    PART_2_LANGUAGE(
        shortLabel = "Part 2",
        titleEn = "Part 2 – Language Paper (30 Q • Qualifying)",
        titleHi = "भाग 2 – भाषा पेपर (30 प्रश्न • क्वालिफाइंग)",
        expectedQuestions = 30,
        meritMaxMarks = 0.0,
        isQualifyingOnly = true,
        hasLanguageSection = true,
        mappedPaperType = PaperType.LANGUAGE,
        classTag = "Qualifying"
    ),
    PART_3_MATHS(
        shortLabel = "Part 3",
        titleEn = "Part 3 – Mathematics (40 Q)",
        titleHi = "भाग 3 – गणित (40 प्रश्न)",
        expectedQuestions = 40,
        meritMaxMarks = 40.0,
        isQualifyingOnly = false,
        hasLanguageSection = false,
        mappedPaperType = PaperType.CLASS_6_8_MATH,
        classTag = "Class 6–8"
    ),
    PART_4_SCIENCE(
        shortLabel = "Part 4",
        titleEn = "Part 4 – Science (40 Q)",
        titleHi = "भाग 4 – विज्ञान (40 प्रश्न)",
        expectedQuestions = 40,
        meritMaxMarks = 40.0,
        isQualifyingOnly = false,
        hasLanguageSection = false,
        mappedPaperType = PaperType.CLASS_6_8_SCIENCE,
        classTag = "Class 6–8"
    ),
    PART_5_FULL_MATH_SCI(
        shortLabel = "Part 5",
        titleEn = "Part 5 – Full Length Maths & Science (150 Q • Merit /120)",
        titleHi = "भाग 5 – फुल लेंथ गणित एवं विज्ञान (150 प्रश्न • मेरिट /120)",
        expectedQuestions = 150,
        meritMaxMarks = 120.0,
        isQualifyingOnly = false,
        hasLanguageSection = true,
        mappedPaperType = PaperType.CLASS_6_8_MATH_SCIENCE,
        classTag = "Class 6–8"
    ),
    PART_6_FULL_MATH(
        shortLabel = "Part 6",
        titleEn = "Part 6 – Full Length Mathematics (150 Q • Merit /120)",
        titleHi = "भाग 6 – फुल लेंथ गणित (150 प्रश्न • मेरिट /120)",
        expectedQuestions = 150,
        meritMaxMarks = 120.0,
        isQualifyingOnly = false,
        hasLanguageSection = true,
        mappedPaperType = PaperType.CLASS_9_10_MATH,
        classTag = "Class 9–10"
    ),
    LEGACY_COMBINED_70(
        shortLabel = "70Q Combined",
        titleEn = "Language + Common Paper (70 Q • Merit /40)",
        titleHi = "भाषा + कॉमन पेपर (70 प्रश्न • मेरिट /40)",
        expectedQuestions = 70,
        meritMaxMarks = 40.0,
        isQualifyingOnly = false,
        hasLanguageSection = true,
        mappedPaperType = PaperType.COMBINED,
        classTag = "Combined"
    ),
    LEGACY_MATH_80(
        shortLabel = "80Q Maths",
        titleEn = "Class 9–10 Mathematics (80 Q)",
        titleHi = "कक्षा 9–10 गणित (80 प्रश्न)",
        expectedQuestions = 80,
        meritMaxMarks = 80.0,
        isQualifyingOnly = false,
        hasLanguageSection = false,
        mappedPaperType = PaperType.CLASS_9_10_MATH_80,
        classTag = "Class 9–10"
    ),
    PRACTICE(
        shortLabel = "Practice",
        titleEn = "Practice Set",
        titleHi = "अभ्यास सेट",
        expectedQuestions = 0,
        meritMaxMarks = 0.0,
        isQualifyingOnly = false,
        hasLanguageSection = false,
        mappedPaperType = PaperType.PRACTICE,
        classTag = "Practice"
    );

    companion object {
        fun fromPaperType(paperType: PaperType, explicitSetType: String? = null): SetType {
            if (!explicitSetType.isNullOrBlank()) {
                entries.firstOrNull { it.name.equals(explicitSetType.trim(), ignoreCase = true) }?.let { return it }
            }
            return when (paperType) {
                PaperType.COMMON_GS -> PART_1_COMMON
                PaperType.LANGUAGE -> PART_2_LANGUAGE
                PaperType.CLASS_6_8_MATH -> PART_3_MATHS
                PaperType.CLASS_6_8_SCIENCE -> PART_4_SCIENCE
                PaperType.CLASS_6_8_MATH_SCIENCE -> PART_5_FULL_MATH_SCI
                PaperType.CLASS_9_10_MATH -> PART_6_FULL_MATH
                PaperType.COMBINED -> LEGACY_COMBINED_70
                PaperType.CLASS_9_10_MATH_80 -> LEGACY_MATH_80
                PaperType.PRACTICE -> PRACTICE
            }
        }

        fun fromRaw(paperTypeRaw: String?, explicitSetType: String? = null): SetType =
            fromPaperType(PaperType.fromString(paperTypeRaw), explicitSetType)
    }
}

enum class OptionEMode(val label: String) {
    NOT_ATTEMPTED("Not Attempted (0 Marks)"),
    STANDARD_5TH_OPTION("Standard 5th Option (E)");

    companion object {
        fun fromString(raw: String?): OptionEMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NOT_ATTEMPTED
    }
}

enum class LanguageFailRankPolicy(val label: String) {
    SHOW_WITH_NOT_QUALIFIED_BADGE("Show with 'Language Not Qualified' badge"),
    EXCLUDE_FROM_MERIT_RANK("Exclude from Merit Rank");

    companion object {
        fun fromString(raw: String?): LanguageFailRankPolicy =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SHOW_WITH_NOT_QUALIFIED_BADGE
    }
}

enum class TieBreakPolicy(val label: String) {
    ACCURACY_AND_TIME("Higher Accuracy → Less Time"),
    SHARED_EQUAL_RANK("Shared Equal Rank (1, 2, 2, 4)");

    companion object {
        fun fromString(raw: String?): TieBreakPolicy =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SHARED_EQUAL_RANK
    }
}

object TestSetNaming {
    fun nextUniqueTitle(paperType: PaperType, existingTests: List<ExamTest>): String {
        val base = paperType.baseSetName
        val existingTitles = existingTests.map { it.title.trim().lowercase() }.toSet()
        val sameTypeTests = existingTests.filter { it.parsedPaperType() == paperType }
        val maxExistingNum = sameTypeTests.mapNotNull { test ->
            Regex("""(?:set|सेट)\s*#?\s*(\d+)$""", RegexOption.IGNORE_CASE)
                .find(test.title.trim())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
        }.maxOrNull() ?: 0

        var nextNum = maxOf(maxExistingNum + 1, sameTypeTests.size + 1)
        var candidate = "$base Set $nextNum"
        while (candidate.lowercase() in existingTitles) {
            nextNum++
            candidate = "$base Set $nextNum"
        }
        return candidate
    }

    fun isDuplicateTitle(
        candidateTitle: String,
        existingTests: List<ExamTest>,
        excludeTestId: String? = null
    ): Boolean {
        val normalized = candidateTitle.trim().lowercase()
        if (normalized.isBlank()) return false
        return existingTests.any {
            it.id != excludeTestId && it.title.trim().lowercase() == normalized
        }
    }

    fun extractSetNumberFromTitle(title: String): Int? =
        Regex("""(?:set|सेट)\s*#?\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(title.trim())
            ?.groupValues?.getOrNull(1)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
}

enum class LanguagePref(val label: String) {
    BILINGUAL("Hindi + English"),
    HINDI("हिन्दी"),
    ENGLISH("English");

    fun displayLabel(uiLanguage: LanguagePref = BILINGUAL): String = when (this) {
        HINDI -> if (uiLanguage == ENGLISH) "Hindi" else label
        else -> label
    }

    companion object {
        fun fromString(raw: String?): LanguagePref =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: BILINGUAL
    }
}

enum class LangQualifyingStatus {
    NOT_ATTEMPTED,
    PASS,
    FAIL;

    companion object {
        fun fromString(raw: String?): LangQualifyingStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NOT_ATTEMPTED
    }
}

enum class RankingPeriod(val label: String) {
    INDIVIDUAL_TEST("Set"),
    WEEKLY("Weekly Report"),
    MONTHLY("Monthly Report"),
    OVERALL("Overall"),
    DAILY("Daily")
}

/**
 * Central 7 ranking categories for BPSSC TRE 4.0 RANKS hub.
 * Strict mapping from existing test/attempt metadata with zero cross-category mixing.
 */
enum class RankCategory(
    val label: String,
    val displayName: String,
    val shortChip: String,
    val meritBaseLabel: String,
    val isQualifyingOnly: Boolean,
    val hasLanguageSection: Boolean,
    val defaultMaxMarks: Double
) {
    LANGUAGE("LANGUAGE", "Language", "30 Q • Qual", "Qualifying /30", true, true, 30.0),
    COMMON("COMMON", "Common", "40 Q • /40", "Merit /40", false, false, 40.0),
    LANGUAGE_COMMON("LANGUAGE + COMMON", "Language + Common", "70 Q • /40", "Merit /40", false, true, 40.0),
    MATHEMATICS("MATHEMATICS", "Mathematics", "40/80 Q", "Merit /40", false, false, 40.0),
    SCIENCE("SCIENCE", "Science", "40 Q • /40", "Merit /40", false, false, 40.0),
    FULL_MATHS_SCIENCE("FULL MATHS + SCIENCE", "Full Math + Science", "150 Q • /120", "Merit /120", false, true, 120.0),
    FULL_MATHS("FULL MATHS", "Full Maths", "150 Q • /120", "Merit /120", false, true, 120.0);

    companion object {
        fun fromCategoryString(raw: String?): RankCategory? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            return entries.firstOrNull {
                it.name.equals(trimmed, ignoreCase = true) ||
                    it.label.equals(trimmed, ignoreCase = true) ||
                    it.displayName.equals(trimmed, ignoreCase = true)
            } ?: fromRaw(trimmed, trimmed)
        }

        fun fromRaw(paperTypeRaw: String?, setTypeRaw: String? = null): RankCategory? {
            val explicitSet = setTypeRaw?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
                SetType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            }
            val explicitPaper = paperTypeRaw?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
                PaperType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            }
            val resolvedSet = explicitSet ?: explicitPaper?.let { SetType.fromPaperType(it) } ?: return null
            return when (resolvedSet) {
                SetType.PART_2_LANGUAGE -> LANGUAGE
                SetType.PART_1_COMMON -> COMMON
                SetType.LEGACY_COMBINED_70 -> LANGUAGE_COMMON
                SetType.PART_3_MATHS, SetType.LEGACY_MATH_80 -> MATHEMATICS
                SetType.PART_4_SCIENCE -> SCIENCE
                SetType.PART_5_FULL_MATH_SCI -> FULL_MATHS_SCIENCE
                SetType.PART_6_FULL_MATH -> FULL_MATHS
                SetType.PRACTICE -> null
            }
        }

        fun fromTest(test: ExamTest): RankCategory? = fromRaw(test.paperType, test.setType)

        fun fromSetType(setType: SetType): RankCategory? = when (setType) {
            SetType.PART_2_LANGUAGE -> LANGUAGE
            SetType.PART_1_COMMON -> COMMON
            SetType.LEGACY_COMBINED_70 -> LANGUAGE_COMMON
            SetType.PART_3_MATHS, SetType.LEGACY_MATH_80 -> MATHEMATICS
            SetType.PART_4_SCIENCE -> SCIENCE
            SetType.PART_5_FULL_MATH_SCI -> FULL_MATHS_SCIENCE
            SetType.PART_6_FULL_MATH -> FULL_MATHS
            SetType.PRACTICE -> null
        }

        fun fromAttempt(attempt: TestAttempt, testsById: Map<String, ExamTest> = emptyMap()): RankCategory? {
            val liveTest = testsById[attempt.testId]
            return if (liveTest != null) {
                fromTest(liveTest)
            } else {
                fromRaw(attempt.paperType, attempt.setType)
            }
        }
    }
}

object ContactAdminPolicy {
    /**
     * Regular Users always see Contact Admin when in a group.
     * Logged-in Admins do NOT see self-contact WhatsApp unless a secondary/other Admin
     * is configured as the official contact recipient.
     */
    fun shouldShowContactAdmin(
        isAdmin: Boolean,
        currentUserId: String,
        adminContact: AdminContactProfile?
    ): Boolean {
        if (!isAdmin) return true
        val contact = adminContact ?: return false
        val hasPhone = !contact.adminWhatsAppNumber.isNullOrBlank()
        val hasOtherUid = contact.adminUid.isNotBlank() && contact.adminUid != currentUserId
        return hasPhone && hasOtherUid
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val whatsappNumber: String? = null,
    val photoBase64: String? = null,
    val activeGroupId: String? = null,
    val role: String = AppRole.ASPIRANT.name,
    val languagePref: String = LanguagePref.BILINGUAL.name,
    val publicSharing: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun appRole(): AppRole = AppRole.fromString(role)
    fun langPreference(): LanguagePref = LanguagePref.fromString(languagePref)
}

@Keep
@IgnoreExtraProperties
data class AdminContactProfile(
    @DocumentId val id: String = "official",
    val groupId: String = "",
    val adminUid: String = "",
    val adminName: String = "",
    val adminPhotoBase64: String? = null,
    val adminWhatsAppNumber: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExamGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val inviteCode: String = "",
    val createdByUid: String = "",
    val createdByName: String = "",
    val memberCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupMember(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val photoBase64: String? = null,
    val role: String = AppRole.ASPIRANT.name,
    val gsTestsAttempted: Long = 0L,
    val gsTotalScore: Double = 0.0,
    val gsAverageScore: Double = 0.0,
    val gsHighestScore: Double = 0.0,
    val gsLowestScore: Double = 0.0,
    val latestGsScore: Double = 0.0,
    val latestGsTestTitle: String? = null,
    val weeklyGsTotal: Double = 0.0,
    val weeklyGsCount: Long = 0L,
    val monthlyGsTotal: Double = 0.0,
    val monthlyGsCount: Long = 0L,
    val langScore: Double = 0.0,
    val langHindiScore: Double = 0.0,
    val langEnglishScore: Double = 0.0,
    val langStatus: String = LangQualifyingStatus.NOT_ATTEMPTED.name,
    val previousRank: Long = 0L,
    val publicSharing: Boolean = true,
    @ServerTimestamp val joinedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun appRole(): AppRole = AppRole.fromString(role)
    fun langQualifying(): LangQualifyingStatus = LangQualifyingStatus.fromString(langStatus)
}

@Keep
@IgnoreExtraProperties
data class ExamQuestion(
    val number: Int = 1,
    val subject: String = "",
    val topic: String = "",
    val source: String = "",
    val text: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val optionE: String = DEFAULT_OPTION_E,
    val correctOption: String = "",
    val explanation: String = "",
    val section: String = "",
    val passage: String = "",
    val subSubject: String = "",
    val textEn: String = "",
    val textHi: String = "",
    val optionAEn: String = "",
    val optionAHi: String = "",
    val optionBEn: String = "",
    val optionBHi: String = "",
    val optionCEn: String = "",
    val optionCHi: String = "",
    val optionDEn: String = "",
    val optionDHi: String = "",
    val explanationEn: String = "",
    val explanationHi: String = "",
    val imageBase64: String = ""
) {
    fun resolvedSection(): String {
        val explicit = section.trim().uppercase()
        if (explicit == SECTION_COMMON || explicit == SECTION_LANGUAGE || explicit == SECTION_SUBJECT) {
            return explicit
        }
        val s = subject.trim().lowercase()
        return if (s.contains("hind") || s.contains("हिन्दी") || s.contains("हिंदी") || s.contains("eng") || s.contains("अंग्रे")) {
            SECTION_LANGUAGE
        } else {
            SECTION_COMMON
        }
    }

    fun displayText(language: LanguagePref): String = when (language) {
        LanguagePref.ENGLISH -> textEn.ifBlank { extractLocalizedSegment(text, preferHindi = false) }
        LanguagePref.HINDI -> textHi.ifBlank { extractLocalizedSegment(text, preferHindi = true) }
        LanguagePref.BILINGUAL -> when {
            textHi.isNotBlank() && textEn.isNotBlank() && textHi != textEn -> "$textHi\n$textEn"
            else -> text.ifBlank { textHi.ifBlank { textEn } }
        }
    }

    fun displayOption(key: String, language: LanguagePref): String {
        val (base, en, hi) = when (key.uppercase()) {
            "A" -> Triple(optionA, optionAEn, optionAHi)
            "B" -> Triple(optionB, optionBEn, optionBHi)
            "C" -> Triple(optionC, optionCEn, optionCHi)
            "D" -> Triple(optionD, optionDEn, optionDHi)
            "E" -> Triple(normalizeOptionE(optionE), "Not Attempted", "प्रयास नहीं किया गया")
            else -> Triple("", "", "")
        }
        return when (language) {
            LanguagePref.ENGLISH -> en.ifBlank { extractLocalizedSegment(base, preferHindi = false) }
            LanguagePref.HINDI -> hi.ifBlank { extractLocalizedSegment(base, preferHindi = true) }
            LanguagePref.BILINGUAL -> when {
                hi.isNotBlank() && en.isNotBlank() && key.uppercase() != "E" && hi != en -> "$hi / $en"
                else -> base.ifBlank { hi.ifBlank { en } }
            }
        }
    }

    fun displayExplanation(language: LanguagePref): String = when (language) {
        LanguagePref.ENGLISH -> explanationEn.ifBlank { extractLocalizedSegment(explanation, preferHindi = false) }
        LanguagePref.HINDI -> explanationHi.ifBlank { extractLocalizedSegment(explanation, preferHindi = true) }
        LanguagePref.BILINGUAL -> when {
            explanationHi.isNotBlank() && explanationEn.isNotBlank() && explanationHi != explanationEn -> "$explanationHi\n$explanationEn"
            else -> explanation.ifBlank { explanationHi.ifBlank { explanationEn } }
        }
    }

    fun optionsList(language: LanguagePref = LanguagePref.BILINGUAL): List<Pair<String, String>> = listOf(
        "A" to displayOption("A", language),
        "B" to displayOption("B", language),
        "C" to displayOption("C", language),
        "D" to displayOption("D", language),
        "E" to displayOption("E", language)
    )

    fun toMap(): Map<String, Any> {
        val map = linkedMapOf<String, Any>(
            "number" to number,
            "subject" to subject,
            "topic" to topic,
            "source" to source,
            "text" to text,
            "optionA" to optionA,
            "optionB" to optionB,
            "optionC" to optionC,
            "optionD" to optionD,
            "optionE" to normalizeOptionE(optionE),
            "correctOption" to correctOption.uppercase().trim(),
            "explanation" to explanation,
            "section" to resolvedSection()
        )
        if (passage.isNotBlank()) map["passage"] = passage.trim()
        if (subSubject.isNotBlank()) map["subSubject"] = subSubject.trim()
        if (textEn.isNotBlank()) map["textEn"] = textEn.trim()
        if (textHi.isNotBlank()) map["textHi"] = textHi.trim()
        if (optionAEn.isNotBlank()) map["optionAEn"] = optionAEn.trim()
        if (optionAHi.isNotBlank()) map["optionAHi"] = optionAHi.trim()
        if (optionBEn.isNotBlank()) map["optionBEn"] = optionBEn.trim()
        if (optionBHi.isNotBlank()) map["optionBHi"] = optionBHi.trim()
        if (optionCEn.isNotBlank()) map["optionCEn"] = optionCEn.trim()
        if (optionCHi.isNotBlank()) map["optionCHi"] = optionCHi.trim()
        if (optionDEn.isNotBlank()) map["optionDEn"] = optionDEn.trim()
        if (optionDHi.isNotBlank()) map["optionDHi"] = optionDHi.trim()
        if (explanationEn.isNotBlank()) map["explanationEn"] = explanationEn.trim()
        if (explanationHi.isNotBlank()) map["explanationHi"] = explanationHi.trim()
        if (imageBase64.isNotBlank()) map["imageBase64"] = imageBase64.trim()
        return map
    }

    companion object {
        const val SECTION_COMMON = "COMMON"
        const val SECTION_LANGUAGE = "LANGUAGE"
        const val SECTION_SUBJECT = "SUBJECT"
        const val DEFAULT_OPTION_E = "प्रयास नहीं किया गया / Not Attempted"

        fun normalizeOptionE(raw: String?): String {
            val trimmed = raw?.trim().orEmpty()
            return if (trimmed.isBlank() ||
                trimmed.equals("Don't Know / पता नहीं", ignoreCase = true) ||
                trimmed.equals("Don't Know", ignoreCase = true) ||
                trimmed.equals("Not Attempted / नॉट अटेम्प्ट", ignoreCase = true) ||
                trimmed.equals("Not Attempted", ignoreCase = true)
            ) {
                DEFAULT_OPTION_E
            } else {
                trimmed
            }
        }

        private fun readStringOption(
            map: Map<String, Any?>,
            flatKeys: List<String>,
            optionsObj: Map<*, *>?,
            optionsArr: List<*>?,
            letter: String,
            index: Int
        ): String {
            for (k in flatKeys) {
                val v = (map[k] as? String)?.trim()
                if (!v.isNullOrBlank()) return v
            }
            if (optionsObj != null) {
                val direct = listOf(letter, letter.lowercase(), "option$letter", "option_${letter.lowercase()}")
                    .firstNotNullOfOrNull { key -> (optionsObj[key] as? String)?.trim()?.takeIf { it.isNotBlank() } }
                if (direct != null) return direct
            }
            if (optionsArr != null) {
                val byIndex = optionsArr.getOrNull(index)
                when (byIndex) {
                    is String -> if (byIndex.isNotBlank()) return byIndex.trim()
                    is Map<*, *> -> {
                        val txt = ((byIndex["text"] ?: byIndex["value"] ?: byIndex["option"]) as? String)?.trim()
                        if (!txt.isNullOrBlank()) return txt
                    }
                }
                for (entry in optionsArr) {
                    if (entry is Map<*, *>) {
                        val entryKey = ((entry["key"] ?: entry["label"] ?: entry["id"]) as? String)?.trim()?.uppercase()
                        if (entryKey == letter) {
                            val txt = ((entry["text"] ?: entry["value"] ?: entry["option"]) as? String)?.trim()
                            if (!txt.isNullOrBlank()) return txt
                        }
                    }
                }
            }
            return ""
        }

        fun fromMap(map: Map<String, Any?>): ExamQuestion {
            val subject = (map["subject"] as? String).orEmpty()
            val rawSection = (map["section"] as? String).orEmpty()
            val optionsRaw = map["options"] ?: map["choices"]
            val optionsObj = optionsRaw as? Map<*, *>
            val optionsArr = optionsRaw as? List<*>
            val questionText = listOf("text", "question", "questionText", "question_text")
                .firstNotNullOfOrNull { k -> (map[k] as? String)?.trim()?.takeIf { it.isNotBlank() } }
                .orEmpty()
            val rawCorrect = listOf("correctOption", "correctAnswer", "correct_answer", "correct_option", "answer", "answerKey")
                .firstNotNullOfOrNull { k -> (map[k] as? String)?.trim()?.takeIf { it.isNotBlank() } }
                .orEmpty()
                .uppercase()
            val resolvedCorrect = when {
                rawCorrect in listOf("A", "B", "C", "D") -> rawCorrect
                else -> Regex("""\b([A-D])\b""").find(rawCorrect)?.groupValues?.getOrNull(1) ?: ""
            }
            return ExamQuestion(
                number = (map["number"] as? Number)?.toInt() ?: (map["id"] as? Number)?.toInt() ?: 1,
                subject = subject,
                topic = (map["topic"] as? String).orEmpty(),
                source = (map["source"] as? String).orEmpty(),
                text = questionText,
                optionA = readStringOption(map, listOf("optionA", "option_a"), optionsObj, optionsArr, "A", 0),
                optionB = readStringOption(map, listOf("optionB", "option_b"), optionsObj, optionsArr, "B", 1),
                optionC = readStringOption(map, listOf("optionC", "option_c"), optionsObj, optionsArr, "C", 2),
                optionD = readStringOption(map, listOf("optionD", "option_d"), optionsObj, optionsArr, "D", 3),
                optionE = normalizeOptionE(readStringOption(map, listOf("optionE", "option_e"), optionsObj, optionsArr, "E", 4)),
                correctOption = resolvedCorrect,
                explanation = (map["explanation"] as? String ?: map["solution"] as? String).orEmpty(),
                section = rawSection,
                passage = (map["passage"] as? String).orEmpty(),
                subSubject = (map["subSubject"] as? String).orEmpty(),
                textEn = (map["textEn"] as? String ?: map["questionEn"] as? String).orEmpty(),
                textHi = (map["textHi"] as? String ?: map["questionHi"] as? String).orEmpty(),
                optionAEn = (map["optionAEn"] as? String).orEmpty(),
                optionAHi = (map["optionAHi"] as? String).orEmpty(),
                optionBEn = (map["optionBEn"] as? String).orEmpty(),
                optionBHi = (map["optionBHi"] as? String).orEmpty(),
                optionCEn = (map["optionCEn"] as? String).orEmpty(),
                optionCHi = (map["optionCHi"] as? String).orEmpty(),
                optionDEn = (map["optionDEn"] as? String).orEmpty(),
                optionDHi = (map["optionDHi"] as? String).orEmpty(),
                explanationEn = (map["explanationEn"] as? String).orEmpty(),
                explanationHi = (map["explanationHi"] as? String).orEmpty(),
                imageBase64 = (map["imageBase64"] as? String).orEmpty()
            )
        }

        private fun hasDevanagari(text: String): Boolean =
            text.any { it in '\u0900'..'\u097F' }

        private fun hasLatin(text: String): Boolean =
            text.any { it in 'A'..'Z' || it in 'a'..'z' }

        /**
         * Gracefully extracts Hindi or English side from legacy combined bilingual strings
         * formatted as "Hindi / English" or "English / Hindi" or newline-separated pairs.
         */
        fun extractLocalizedSegment(raw: String, preferHindi: Boolean): String {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return ""
            val slashParts = trimmed.split(" / ").map { it.trim() }.filter { it.isNotBlank() }
            if (slashParts.size == 2) {
                val p0Hi = hasDevanagari(slashParts[0])
                val p1Hi = hasDevanagari(slashParts[1])
                if (p0Hi != p1Hi) {
                    return if (preferHindi) {
                        if (p0Hi) slashParts[0] else slashParts[1]
                    } else {
                        if (!p0Hi) slashParts[0] else slashParts[1]
                    }
                }
            }
            val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.size >= 2) {
                val hiLines = lines.filter { hasDevanagari(it) }
                val enLines = lines.filter { !hasDevanagari(it) && hasLatin(it) }
                if (hiLines.isNotEmpty() && enLines.isNotEmpty()) {
                    return if (preferHindi) hiLines.joinToString("\n") else enLines.joinToString("\n")
                }
            }
            return trimmed
        }
    }
}

@Keep
@IgnoreExtraProperties
data class ExamTest(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val paperType: String = PaperType.COMMON_GS.name,
    val difficulty: String = "MODERATE",
    val status: String = "PUBLISHED",
    val creatorUid: String = "",
    val creatorName: String = "",
    val creatorPhotoBase64: String? = null,
    val durationMinutes: Long = 30L,
    val questionCount: Long = 40L,
    val negativeMarkNumerator: Long = 1L,
    val negativeMarkDenominator: Long = 3L,
    val unattemptedPenalty: Boolean = true,
    val questions: List<Map<String, Any?>> = emptyList(),
    val attemptCount: Long = 0L,
    val answerReleaseMode: String = "IMMEDIATE",
    val scheduledAt: String? = null,
    val approvedForAll: Boolean = true,
    val approvedMemberIds: List<String> = emptyList(),
    val visibleToUids: List<String> = listOf("ALL"),
    val iconKey: String? = null,
    val customIconBase64: String? = null,
    val customIconUrl: String? = null,
    val bannerImageBase64: String? = null,
    // Configurable Testbook-style Exam Rules (all optional for 100% legacy Firestore compatibility)
    val setType: String? = null,
    val classLevel: String? = null,
    val positiveMarks: Double = 1.0,
    val optionEMode: String = OptionEMode.NOT_ATTEMPTED.name,
    val cutoffMarks: Double? = null,
    val pauseAllowed: Boolean = true,
    val langPassMarks: Double = 9.0,
    val langMinHindi: Double = 0.0,
    val langMinEnglish: Double = 0.0,
    val langFailRankPolicy: String = LanguageFailRankPolicy.SHOW_WITH_NOT_QUALIFIED_BADGE.name,
    val tieBreakPolicy: String = TieBreakPolicy.SHARED_EQUAL_RANK.name,
    val assembledFromTestIds: List<String> = emptyList(),
    val setNumber: Int? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedQuestions(): List<ExamQuestion> = questions.map { ExamQuestion.fromMap(it) }
    fun parsedPaperType(): PaperType = PaperType.fromString(paperType)
    fun resolvedSetType(): SetType = SetType.fromPaperType(parsedPaperType(), setType)
    fun resolvedSetNumber(): Int? = setNumber?.takeIf { it > 0 } ?: TestSetNaming.extractSetNumberFromTitle(title)
    fun resolvedOptionEMode(): OptionEMode = OptionEMode.fromString(optionEMode)
    fun resolvedLangFailRankPolicy(): LanguageFailRankPolicy = LanguageFailRankPolicy.fromString(langFailRankPolicy)
    fun resolvedTieBreakPolicy(): TieBreakPolicy = TieBreakPolicy.fromString(tieBreakPolicy)

    fun negativeMarkLabel(): String {
        if (negativeMarkNumerator <= 0L || negativeMarkDenominator <= 0L) return "0"
        return if (negativeMarkDenominator == 1L) "-$negativeMarkNumerator"
        else "-$negativeMarkNumerator/$negativeMarkDenominator"
    }

    fun isVisibleToMember(uid: String, isAdmin: Boolean): Boolean {
        if (isAdmin) return true
        if (status != "PUBLISHED") return false
        if (approvedForAll || "ALL" in visibleToUids) return true
        return uid in visibleToUids || uid in approvedMemberIds
    }

    fun accessSummaryLabel(): String = when {
        status != "PUBLISHED" -> "Hidden (Queue)"
        approvedForAll || "ALL" in visibleToUids -> "All Users"
        else -> {
            val count = approvedMemberIds.ifEmpty { visibleToUids }.distinct().size
            "$count Selected User${if (count == 1) "" else "s"}"
        }
    }

    companion object {
        /**
         * Authoritative newest-first comparator so any newly added Admin set ALWAYS appears at the top.
         * Primary: creation time (createdAt, falling back to updatedAt if createdAt is missing) descending.
         * Secondary: parsed set number descending (so Set 10 > Set 9 when timestamps are equal).
         * Tertiary: updatedAt descending.
         */
        val newestFirstComparator: Comparator<ExamTest> =
            compareByDescending<ExamTest> {
                it.createdAt?.toDate()?.time ?: it.updatedAt?.toDate()?.time ?: 0L
            }.thenByDescending {
                it.resolvedSetNumber() ?: Int.MIN_VALUE
            }.thenByDescending {
                it.updatedAt?.toDate()?.time ?: 0L
            }
    }
}

@Keep
@IgnoreExtraProperties
data class TestAttempt(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val testId: String = "",
    val testTitle: String = "",
    val paperType: String = PaperType.COMMON_GS.name,
    val userId: String = "",
    val aspirantName: String = "",
    val aspirantPhotoBase64: String? = null,
    val answers: Map<String, String> = emptyMap(),
    val correctCount: Long = 0L,
    val incorrectCount: Long = 0L,
    val dontKnowCount: Long = 0L,
    val notAttemptedCount: Long = 0L,
    val totalScore: Double = 0.0,
    val maxMarks: Double = 40.0,
    val percentage: Double = 0.0,
    val negativeDeducted: Double = 0.0,
    val subjectScores: Map<String, Double> = emptyMap(),
    val hindiScore: Double = 0.0,
    val englishScore: Double = 0.0,
    val langPassed: Boolean = false,
    val timeSpentSeconds: Long = 0L,
    val questionTimesSec: Map<String, Long> = emptyMap(),
    // Separated Merit & Qualifying analytics fields (optional for 100% legacy backward compatibility)
    val setType: String? = null,
    val meritMarks: Double? = null,
    val totalMeritMarks: Double? = null,
    val positiveMarksEarned: Double? = null,
    val languageTotalMarks: Double? = null,
    val languageStatus: String? = null,
    val meritCorrectCount: Long? = null,
    val meritIncorrectCount: Long? = null,
    val meritUnattemptedCount: Long? = null,
    val startedAtEpochMillis: Long? = null,
    val completionStatus: String? = null,
    @ServerTimestamp val submittedAt: Timestamp? = null
) {
    fun resolvedSetType(): SetType = SetType.fromRaw(paperType, setType)

    fun effectiveLanguageTotal(): Double = languageTotalMarks ?: (hindiScore + englishScore)

    /**
     * Authoritative Merit Score strictly excluding Language Qualifying marks.
     * Backward-compatible with legacy attempts saved before v1.2.0 where totalScore included Language.
     */
    fun effectiveMeritScore(): Double {
        if (meritMarks != null) return meritMarks
        val st = resolvedSetType()
        if (st == SetType.PART_2_LANGUAGE) return 0.0
        if (!st.hasLanguageSection) return totalScore
        // Legacy Combined 70Q (maxMarks == 70) or legacy 150Q (maxMarks == 150) had Language inside totalScore
        val looksLikeLegacyCombinedTotal = (st == SetType.LEGACY_COMBINED_70 && maxMarks > 45.0) ||
            ((st == SetType.PART_5_FULL_MATH_SCI || st == SetType.PART_6_FULL_MATH) && maxMarks > 125.0)
        return if (looksLikeLegacyCombinedTotal) {
            totalScore - (hindiScore + englishScore)
        } else {
            totalScore
        }
    }

    /**
     * Authoritative Merit Max Marks strictly excluding 30 Language Qualifying marks.
     * Part 5 & Part 6 = 120, Legacy Combined 70Q = 40, Part 1/3/4 = 40, Part 2 = 0 (Qualifying Only).
     */
    fun effectiveMeritMaxMarks(): Double {
        if (totalMeritMarks != null) return totalMeritMarks
        val st = resolvedSetType()
        return when (st) {
            SetType.PART_2_LANGUAGE -> 0.0
            SetType.PART_5_FULL_MATH_SCI, SetType.PART_6_FULL_MATH -> 120.0
            SetType.LEGACY_COMBINED_70 -> 40.0
            SetType.PART_1_COMMON, SetType.PART_3_MATHS, SetType.PART_4_SCIENCE -> maxMarks.takeIf { it > 0.0 } ?: 40.0
            SetType.LEGACY_MATH_80 -> maxMarks.takeIf { it > 0.0 } ?: 80.0
            SetType.PRACTICE -> maxMarks.takeIf { it > 0.0 } ?: 40.0
        }
    }
}

/**
 * Strictly isolated Solution Reattempt record.
 * NEVER modifies the immutable Original Attempt score, accuracy, Language status, or leaderboard rank.
 */
@Keep
@IgnoreExtraProperties
data class SolutionReattemptRecord(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val testId: String = "",
    val testTitle: String = "",
    val userId: String = "",
    val aspirantName: String = "",
    val reattemptAnswers: Map<String, String> = emptyMap(),
    val correctCount: Long = 0L,
    val wrongCount: Long = 0L,
    val totalReattempted: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Student-bookmarked revision question saved during live exam or post-test solution analysis.
 */
@Keep
@IgnoreExtraProperties
data class BookmarkedQuestion(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val userId: String = "",
    val testId: String = "",
    val testTitle: String = "",
    val questionIndex: Long = 0L,
    val questionNumber: Long = 1L,
    val subject: String = "",
    val questionText: String = "",
    val correctOption: String = "",
    val explanation: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CurrentAffairItem(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val summary: String = "",
    val dateLabel: String = "",
    val source: String = "",
    val imageBase64: String? = null,
    val category: String = "National",
    val publishedByUid: String = "",
    val publishedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuestionPoll(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val creatorUid: String = "",
    val creatorName: String = "",
    val creatorPhotoBase64: String? = null,
    val questionTitle: String = "",
    val description: String? = null,
    val imageBase64: String? = null,
    val options: List<String> = emptyList(),
    val status: String = "OPEN",
    val correctOptionIndex: Long = -1L,
    val finalExplanation: String? = null,
    val solutionImages: List<String> = emptyList(),
    val solutionContributors: List<String> = emptyList(),
    val voteCounts: List<Long> = emptyList(),
    val totalVotes: Long = 0L,
    val reportCount: Long = 0L,
    val reportedByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PollVote(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val pollId: String = "",
    val userId: String = "",
    val voterName: String = "",
    val selectedOptionIndex: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DiscussionComment(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val targetType: String = "GENERAL",
    val targetId: String = "group",
    val authorUid: String = "",
    val authorName: String = "",
    val authorPhotoBase64: String? = null,
    val message: String = "",
    val commentType: String = "COMMENT",
    val parentCommentId: String? = null,
    val solutionImageBase64: String? = null,
    val isHelpful: Boolean = false,
    val helpfulMarkedBy: String? = null,
    val resolved: Boolean = false,
    val resolvedByUid: String? = null,
    val resolvedNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupNotification(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "ANNOUNCEMENT",
    val senderUid: String = "",
    val senderName: String = "",
    val targetAction: String? = null,
    val dedupKey: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppUpdateRelease(
    @DocumentId val id: String = "",
    val versionName: String = "",
    val versionCode: Long = 1L,
    val title: String = "",
    val releaseNotes: String = "",
    val updateType: String = "NORMAL",
    val minSupportedVersionCode: Long = 1L,
    val downloadUrl: String = "",
    val fileSizeBytes: Long = 0L,
    val sha256Checksum: String? = null,
    val packageName: String? = null,
    val published: Boolean = true,
    val isActive: Boolean = false,
    val uploadedByUid: String = "",
    val uploadedByName: String = "",
    val groupId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isMandatoryFor(currentVersionCode: Long): Boolean =
        published && versionCode > currentVersionCode &&
            (updateType.equals("MANDATORY", ignoreCase = true) || currentVersionCode < minSupportedVersionCode)
}

@Keep
@IgnoreExtraProperties
data class ContentViewRecord(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val contentId: String = "",
    val contentType: String = CONTENT_TYPE_BANNER,
    val userId: String = "",
    val userName: String = "",
    @ServerTimestamp val viewedAt: Timestamp? = null
) {
    companion object {
        const val CONTENT_TYPE_BANNER = "BANNER"
        const val CONTENT_TYPE_POLL = "POLL"

        fun docId(contentType: String, contentId: String, userId: String): String {
            val cleanType = if (contentType.equals(CONTENT_TYPE_POLL, ignoreCase = true)) {
                CONTENT_TYPE_POLL
            } else {
                CONTENT_TYPE_BANNER
            }
            val cleanContentId = contentId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val cleanUserId = userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            return "${cleanType}_${cleanContentId}_$cleanUserId".take(128)
        }
    }
}

data class RankerSubjectStat(
    val subject: String,
    val marks: Double,
    val correctCount: Int,
    val totalQuestions: Int
)

data class RankerSectionStat(
    val sectionName: String,
    val marks: Double,
    val maxMarks: Double,
    val correctCount: Int,
    val incorrectCount: Int,
    val unattemptedCount: Int
)

data class RankerDetailBreakdown(
    val userId: String,
    val userName: String,
    val rank: Int,
    val categoryLabel: String,
    val periodLabel: String,
    val totalScore: Double,
    val maxMarks: Double,
    val percentage: Double,
    val correctCount: Int,
    val incorrectCount: Int,
    val unattemptedCount: Int,
    val timeSpentSeconds: Long,
    val testsAttempted: Int,
    val attemptStatusLabel: String,
    val subjectStats: List<RankerSubjectStat>,
    val sectionStats: List<RankerSectionStat>
)

data class RankedAspirant(
    val rank: Int,
    val member: GroupMember,
    val displayScore: Double,
    val testsAttempted: Int,
    val testsAvailable: Int,
    val maxMarks: Double = 40.0,
    val showLanguageBadge: Boolean = true,
    val setLanguageScore: Double? = null,
    val setLanguagePassed: Boolean? = null,
    val percentile: Double = 0.0,
    val accuracyPercentage: Double = 0.0,
    val timeSpentSeconds: Long = 0L,
    val languageNotQualified: Boolean = false,
    val rankScopeLabel: String = "",
    val meritBaseLabel: String = "",
    val subjectScores: Map<String, Double> = emptyMap(),
    val subjectCorrectCounts: Map<String, Int> = emptyMap(),
    val subjectTotalCounts: Map<String, Int> = emptyMap(),
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val unattemptedCount: Int = 0,
    val hindiScore: Double = 0.0,
    val englishScore: Double = 0.0,
    val attemptStatusLabel: String = ""
)

/**
 * Complete per-participant record for the dedicated Set-Wise Rank leaderboard.
 * Contains every user who submitted an eligible attempt for a specific [testId] with zero cap.
 */
data class SetRankEntry(
    val rank: Int,
    val userId: String,
    val studentName: String,
    val photoBase64: String? = null,
    val testId: String,
    val score: Double,
    val totalMarks: Double,
    val percentage: Double,
    val correctCount: Int,
    val incorrectCount: Int,
    val unattemptedCount: Int,
    val accuracyPercentage: Double,
    val timeSpentSeconds: Long,
    val submittedAtEpochMillis: Long,
    val showLanguageBadge: Boolean = false,
    val languageScore: Double? = null,
    val languagePassed: Boolean? = null,
    val attempt: TestAttempt
)

/**
 * Dynamic real-time status badge for a scheduled Live Exam evaluated in IST/UTC epoch milliseconds.
 */
enum class LiveExamDynamicStatus(val badgeLabel: String) {
    UPCOMING("UPCOMING"),
    REGISTRATION_OPEN("REGISTRATION OPEN"),
    REGISTRATION_CLOSED("REGISTRATION CLOSED"),
    STARTING_SOON("STARTING SOON"),
    LIVE_NOW("LIVE NOW"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED")
}

/**
 * Scheduled synchronized Live Exam linked to an existing question set via [sourceSetId].
 * Removing a Live Exam NEVER deletes the source [ExamTest].
 */
@Keep
@IgnoreExtraProperties
data class LiveExam(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val sourceSetId: String = "",
    val sourceSetTitle: String = "",
    val title: String = "",
    val description: String? = null,
    val category: String = RankCategory.COMMON.name,
    val paperType: String = PaperType.COMMON_GS.name,
    val examDateIst: String? = null,
    val startTimeIst: String? = null,
    val startTimeEpochMillis: Long = 0L,
    val durationMinutes: Long = 30L,
    val endTimeEpochMillis: Long = 0L,
    val registrationOpenEpochMillis: Long = 0L,
    val registrationCloseEpochMillis: Long = 0L,
    val resultPublishEpochMillis: Long = 0L,
    val resultsPublishedManually: Boolean = false,
    val totalQuestions: Long = 40L,
    val totalMarks: Double = 40.0,
    val negativeMarkNumerator: Long = 0L,
    val negativeMarkDenominator: Long = 3L,
    val eligibilityType: String = "ALL_USERS", // ALL_USERS | REGISTERED_ONLY
    val maxParticipants: Long = 1000L,
    val lateEntryAllowed: Boolean = true,
    val lateEntryCutoffMinutes: Long = 15L,
    val secureModeEnabled: Boolean = true,
    val dynamicWatermark: Boolean = true,
    val focusLossDetection: Boolean = true,
    val copyPasteBlock: Boolean = true,
    val navigationExitLock: Boolean = true,
    val warningLimit: Long = 3L,
    val status: String = "PUBLISHED", // DRAFT | PUBLISHED | CANCELLED | COMPLETED
    val createdByUid: String = "",
    val createdByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedRankCategory(): RankCategory =
        RankCategory.fromCategoryString(category)
            ?: RankCategory.fromRaw(paperType)
            ?: RankCategory.COMMON

    fun dynamicStatus(nowEpochMillis: Long = System.currentTimeMillis()): LiveExamDynamicStatus {
        if (status.equals("CANCELLED", ignoreCase = true)) return LiveExamDynamicStatus.CANCELLED
        if (status.equals("COMPLETED", ignoreCase = true) || (endTimeEpochMillis > 0L && nowEpochMillis >= endTimeEpochMillis)) {
            return LiveExamDynamicStatus.COMPLETED
        }
        if (startTimeEpochMillis > 0L && nowEpochMillis in startTimeEpochMillis until endTimeEpochMillis) {
            return LiveExamDynamicStatus.LIVE_NOW
        }
        // Within 15 minutes of start time
        val startingSoonWindowMillis = 15L * 60L * 1000L
        if (startTimeEpochMillis > 0L && nowEpochMillis in (startTimeEpochMillis - startingSoonWindowMillis) until startTimeEpochMillis) {
            return LiveExamDynamicStatus.STARTING_SOON
        }
        if (registrationOpenEpochMillis > 0L && registrationCloseEpochMillis > 0L) {
            if (nowEpochMillis in registrationOpenEpochMillis..registrationCloseEpochMillis) {
                return LiveExamDynamicStatus.REGISTRATION_OPEN
            }
            if (nowEpochMillis > registrationCloseEpochMillis && nowEpochMillis < startTimeEpochMillis) {
                return LiveExamDynamicStatus.REGISTRATION_CLOSED
            }
        }
        return LiveExamDynamicStatus.UPCOMING
    }

    fun isResultsReleased(nowEpochMillis: Long = System.currentTimeMillis()): Boolean =
        resultsPublishedManually || (resultPublishEpochMillis > 0L && nowEpochMillis >= resultPublishEpochMillis)

    fun canEnterLiveNow(isRegistered: Boolean, nowEpochMillis: Long = System.currentTimeMillis()): Boolean {
        val ds = dynamicStatus(nowEpochMillis)
        if (ds != LiveExamDynamicStatus.LIVE_NOW) return false
        if (eligibilityType.equals("REGISTERED_ONLY", ignoreCase = true) && !isRegistered) return false
        if (!lateEntryAllowed && nowEpochMillis > startTimeEpochMillis + 60_000L) return false
        if (lateEntryAllowed && lateEntryCutoffMinutes > 0L) {
            val cutoffMillis = startTimeEpochMillis + (lateEntryCutoffMinutes * 60_000L)
            if (nowEpochMillis > cutoffMillis && !isRegistered) return false
        }
        return true
    }

    fun negativeMarkLabel(): String {
        if (negativeMarkNumerator <= 0L || negativeMarkDenominator <= 0L) return "0"
        return if (negativeMarkDenominator == 1L) "-$negativeMarkNumerator"
        else "-$negativeMarkNumerator/$negativeMarkDenominator"
    }
}

/**
 * Student registration, waiting room status, active live exam progress, security violation log,
 * and Admin moderation status keyed by "${liveExamId}_${userId}".
 */
@Keep
@IgnoreExtraProperties
data class LiveExamSession(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val liveExamId: String = "",
    val sourceSetId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String? = null,
    val status: String = "REGISTERED", // REGISTERED | WAITING_ROOM | ACTIVE | SUBMITTED | FLAGGED | APPROVED | DISQUALIFIED
    val answeredCount: Long = 0L,
    val totalQuestions: Long = 40L,
    val currentQuestionIndex: Long = 0L,
    val savedAnswers: Map<String, String> = emptyMap(),
    val violationCount: Long = 0L,
    val violationLogs: List<String> = emptyList(),
    val finalFiveMinAlertShown: Boolean = false,
    val score: Double? = null,
    val maxMarks: Double? = null,
    val adminModerationNote: String? = null,
    val moderatedByUid: String? = null,
    val registeredAtEpochMillis: Long? = null,
    val startedAtEpochMillis: Long? = null,
    val submittedAtEpochMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        fun sessionId(liveExamId: String, userId: String): String =
            "${liveExamId.trim()}_${userId.trim()}"
    }
}

/**
 * Admin-assigned Daily Target Set ("आज का टारगेट / Aaj Ka Target") with optional custom photo banner.
 * - Matched against IST date keys ("yyyy-MM-dd"):
 *   - targetDateIst == todayIst -> Shown prominently on Home as Today's Target banner.
 *   - targetDateIst == yesterdayIst -> Automatically shown as "बीते हुए कल का टास्क" to users who missed it.
 *   - targetDateIst > todayIst -> Pre-scheduled Next Day banner ready to go live automatically at 12:00 AM IST.
 */
@Keep
@IgnoreExtraProperties
data class DailyTargetTask(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val testId: String = "",
    val testTitle: String = "",
    val targetDateIst: String = "", // yyyy-MM-dd in IST (Asia/Kolkata)
    val bannerTitle: String = "आज का टारगेट • Aaj Ka Target",
    val bannerSubtitle: String? = null,
    val bannerImageBase64: String? = null,
    val publishedByUid: String = "",
    val publishedByName: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun displayBannerTitle(language: LanguagePref): String {
        val raw = bannerTitle.trim().ifBlank { "आज का टारगेट • Aaj Ka Target" }
        if (language != LanguagePref.ENGLISH) return raw
        return when (raw) {
            "आज का टारगेट • Aaj Ka Target", "आज का टारगेट" -> "Today's Target"
            "आज का प्रैक्टिस सेट • Today's Set" -> "Today's Practice Set"
            "कल का स्पेशल टारगेट • Tomorrow's Target" -> "Tomorrow's Special Target"
            else -> {
                val dotParts = raw.split("•").map { it.trim() }.filter { it.isNotBlank() }
                if (dotParts.size == 2) {
                    val p0Hi = dotParts[0].any { it in '\u0900'..'\u097F' }
                    val p1Hi = dotParts[1].any { it in '\u0900'..'\u097F' }
                    when {
                        p0Hi && !p1Hi -> if (dotParts[1].equals("Aaj Ka Target", ignoreCase = true)) "Today's Target" else dotParts[1]
                        !p0Hi && p1Hi -> dotParts[0]
                        else -> raw
                    }
                } else {
                    ExamQuestion.extractLocalizedSegment(raw, preferHindi = false)
                }
            }
        }
    }
}

/**
 * Admin-managed default icon/logo for each of the 7 official TRE 4.0 test set categories.
 * Stored at /groups/{groupId}/categoryIcons/{categoryKey} so Admin uploads once per category
 * and every set in that category inherits the icon automatically unless overridden per set.
 */
@Keep
@IgnoreExtraProperties
data class CategoryIconConfig(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val categoryKey: String = "",
    val iconKey: String? = null,
    val customIconBase64: String? = null,
    val updatedByUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)



