package com.example.util

import android.content.Context
import com.example.data.model.ExamQuestion
import com.example.data.model.PaperType
import org.json.JSONArray
import org.json.JSONObject

data class TestSetDraft(
    val id: String,
    val title: String,
    val paperType: PaperType,
    val difficulty: String,
    val durationMinutes: Int,
    val unattemptedPenalty: Boolean,
    val answerReleaseMode: String,
    val questions: List<ExamQuestion>,
    val commonSectionQuestions: List<ExamQuestion> = emptyList(),
    val languageSectionQuestions: List<ExamQuestion> = emptyList(),
    val subjectSectionQuestions: List<ExamQuestion> = emptyList(),
    val savedAtMillis: Long = System.currentTimeMillis()
)

data class EditorAutoDraft(
    val editorKey: String,
    val rawJsonText: String,
    val title: String,
    val paperType: PaperType,
    val difficulty: String,
    val durationMinutes: Int,
    val questions: List<ExamQuestion>,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class JsonValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val errorLineNumber: Int? = null,
    val errorFieldName: String? = null,
    val parsedQuestions: List<ExamQuestion> = emptyList()
)

/**
 * Local SharedPreferences draft storage so Creators & Admins can save, resume, and delete
 * multiple in-progress TRE 4.0 test sets safely before publishing to Firestore,
 * plus automatic draft recovery and strict pre-save JSON validation.
 */
class TestDraftStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveDraft(draft: TestSetDraft) {
        val current = loadAllDrafts().filterNot { it.id == draft.id }.toMutableList()
        current.add(0, draft.copy(savedAtMillis = System.currentTimeMillis()))
        val capped = current.take(MAX_DRAFTS)
        val arr = JSONArray()
        capped.forEach { arr.put(serializeDraft(it)) }
        prefs.edit().putString(KEY_DRAFTS, arr.toString()).apply()
    }

    fun loadAllDrafts(): List<TestSetDraft> {
        val raw = prefs.getString(KEY_DRAFTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    deserializeDraft(arr.getJSONObject(i))?.let { add(it) }
                }
            }.sortedByDescending { it.savedAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun deleteDraft(draftId: String) {
        val remaining = loadAllDrafts().filterNot { it.id == draftId }
        val arr = JSONArray()
        remaining.forEach { arr.put(serializeDraft(it)) }
        prefs.edit().putString(KEY_DRAFTS, arr.toString()).apply()
    }

    fun saveAutoDraft(autoDraft: EditorAutoDraft) {
        val key = normalizeEditorKey(autoDraft.editorKey)
        val obj = JSONObject().apply {
            put("editorKey", key)
            put("rawJsonText", autoDraft.rawJsonText)
            put("title", autoDraft.title)
            put("paperType", autoDraft.paperType.name)
            put("difficulty", autoDraft.difficulty)
            put("durationMinutes", autoDraft.durationMinutes)
            put("questions", serializeQuestions(autoDraft.questions))
            put("updatedAtMillis", System.currentTimeMillis())
        }
        prefs.edit().putString("$KEY_AUTO_DRAFT_PREFIX$key", obj.toString()).apply()
    }

    fun loadAutoDraft(editorKey: String): EditorAutoDraft? {
        val key = normalizeEditorKey(editorKey)
        val raw = prefs.getString("$KEY_AUTO_DRAFT_PREFIX$key", null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val paperType = PaperType.fromString(obj.optString("paperType", PaperType.COMMON_GS.name))
            EditorAutoDraft(
                editorKey = obj.optString("editorKey", key),
                rawJsonText = obj.optString("rawJsonText", ""),
                title = obj.optString("title", ""),
                paperType = paperType,
                difficulty = obj.optString("difficulty", "MODERATE"),
                durationMinutes = obj.optInt("durationMinutes", paperType.defaultDurationMinutes),
                questions = deserializeQuestions(obj.optJSONArray("questions")),
                updatedAtMillis = obj.optLong("updatedAtMillis", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearAutoDraft(editorKey: String) {
        val key = normalizeEditorKey(editorKey)
        prefs.edit().remove("$KEY_AUTO_DRAFT_PREFIX$key").apply()
    }

    private fun normalizeEditorKey(rawKey: String): String =
        rawKey.trim().ifBlank { "new_set_editor" }.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

    private fun serializeDraft(draft: TestSetDraft): JSONObject = JSONObject().apply {
        put("id", draft.id)
        put("title", draft.title)
        put("paperType", draft.paperType.name)
        put("difficulty", draft.difficulty)
        put("durationMinutes", draft.durationMinutes)
        put("unattemptedPenalty", draft.unattemptedPenalty)
        put("answerReleaseMode", draft.answerReleaseMode)
        put("savedAtMillis", draft.savedAtMillis)
        put("questions", serializeQuestions(draft.questions))
        put("commonSectionQuestions", serializeQuestions(draft.commonSectionQuestions))
        put("languageSectionQuestions", serializeQuestions(draft.languageSectionQuestions))
        put("subjectSectionQuestions", serializeQuestions(draft.subjectSectionQuestions))
    }

    private fun deserializeDraft(obj: JSONObject): TestSetDraft? {
        val id = obj.optString("id", "")
        if (id.isBlank()) return null
        val paperType = PaperType.fromString(obj.optString("paperType", PaperType.COMMON_GS.name))
        return TestSetDraft(
            id = id,
            title = obj.optString("title", ""),
            paperType = paperType,
            difficulty = obj.optString("difficulty", "MODERATE"),
            durationMinutes = obj.optInt("durationMinutes", paperType.defaultDurationMinutes),
            unattemptedPenalty = obj.optBoolean("unattemptedPenalty", true),
            answerReleaseMode = obj.optString("answerReleaseMode", "IMMEDIATE"),
            questions = deserializeQuestions(obj.optJSONArray("questions")),
            commonSectionQuestions = deserializeQuestions(obj.optJSONArray("commonSectionQuestions")),
            languageSectionQuestions = deserializeQuestions(obj.optJSONArray("languageSectionQuestions")),
            subjectSectionQuestions = deserializeQuestions(obj.optJSONArray("subjectSectionQuestions")),
            savedAtMillis = obj.optLong("savedAtMillis", 0L)
        )
    }

    private fun serializeQuestions(list: List<ExamQuestion>): JSONArray {
        val arr = JSONArray()
        list.forEach { q ->
            arr.put(
                JSONObject().apply {
                    put("number", q.number)
                    put("subject", q.subject)
                    put("topic", q.topic)
                    put("source", q.source)
                    put("text", q.text)
                    put("optionA", q.optionA)
                    put("optionB", q.optionB)
                    put("optionC", q.optionC)
                    put("optionD", q.optionD)
                    put("optionE", q.optionE)
                    put("correctOption", q.correctOption)
                    put("explanation", q.explanation)
                    put("section", q.resolvedSection())
                    put("passage", q.passage)
                }
            )
        }
        return arr
    }

    private fun deserializeQuestions(arr: JSONArray?): List<ExamQuestion> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    ExamQuestion(
                        number = o.optInt("number", i + 1),
                        subject = o.optString("subject", "Mathematics"),
                        topic = o.optString("topic", ""),
                        source = o.optString("source", ""),
                        text = o.optString("text", ""),
                        optionA = o.optString("optionA", ""),
                        optionB = o.optString("optionB", ""),
                        optionC = o.optString("optionC", ""),
                        optionD = o.optString("optionD", ""),
                        optionE = ExamQuestion.normalizeOptionE(o.optString("optionE", ExamQuestion.DEFAULT_OPTION_E)),
                        correctOption = o.optString("correctOption", "A").uppercase(),
                        explanation = o.optString("explanation", ""),
                        section = o.optString("section", ""),
                        passage = o.optString("passage", "")
                    )
                )
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "tre4_test_drafts"
        private const val KEY_DRAFTS = "saved_test_drafts_v1"
        private const val KEY_AUTO_DRAFT_PREFIX = "auto_editor_draft_v1_"
        private const val MAX_DRAFTS = 15

        /**
         * Strict pre-save JSON validator for the Admin JSON Editor / Question Importer.
         * Accepts BOTH flat fields (optionA..optionD + optional optionE) and nested
         * options objects/arrays (options: {"A": "...", "B": "...", "C": "...", "D": "..."}),
         * normalizes subject variants and part/section tags, and reports exact line/field diagnostics.
         */
        fun validateRawJsonStrict(rawJson: String): JsonValidationResult {
            val trimmed = rawJson.removePrefix("\uFEFF").trim()
            if (trimmed.isEmpty()) {
                return JsonValidationResult(
                    isValid = false,
                    errorMessage = "JSON input is empty. Paste a valid JSON array or {\"questions\": [...]} object.",
                    errorLineNumber = 1,
                    errorFieldName = "root"
                )
            }
            return try {
                val entries = mutableListOf<Triple<JSONObject, String, String>>()
                if (trimmed.startsWith("[")) {
                    val arr = JSONArray(trimmed)
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: return JsonValidationResult(
                            isValid = false,
                            errorMessage = "Question #${i + 1} is not a valid JSON object.",
                            errorLineNumber = i + 1,
                            errorFieldName = "questions[$i]"
                        )
                        entries.add(Triple(item, "", ""))
                    }
                } else if (trimmed.startsWith("{")) {
                    val root = JSONObject(trimmed)
                    val directArr = root.optJSONArray("questions")
                        ?: root.optJSONArray("data")
                        ?: root.optJSONArray("items")
                    val sectionsArr = root.optJSONArray("sections")
                    when {
                        directArr != null -> {
                            val rootSubj = firstNonBlankJsonString(root, "subject", "category")
                            val rootPart = firstNonBlankJsonString(root, "part", "section", "paperSection")
                            for (i in 0 until directArr.length()) {
                                val item = directArr.optJSONObject(i) ?: return JsonValidationResult(
                                    isValid = false,
                                    errorMessage = "Question #${i + 1} is not a valid JSON object.",
                                    errorLineNumber = i + 1,
                                    errorFieldName = "questions[$i]"
                                )
                                entries.add(Triple(item, rootPart, rootSubj))
                            }
                        }
                        sectionsArr != null -> {
                            for (s in 0 until sectionsArr.length()) {
                                val secObj = sectionsArr.optJSONObject(s) ?: continue
                                val secHint = firstNonBlankJsonString(secObj, "part", "section", "type", "name")
                                val secSubjHint = firstNonBlankJsonString(secObj, "subject", "category")
                                val secQs = secObj.optJSONArray("questions") ?: continue
                                for (i in 0 until secQs.length()) {
                                    val item = secQs.optJSONObject(i) ?: return JsonValidationResult(
                                        isValid = false,
                                        errorMessage = "Section #${s + 1} Question #${i + 1} is not a valid JSON object.",
                                        errorLineNumber = i + 1,
                                        errorFieldName = "sections[$s].questions[$i]"
                                    )
                                    entries.add(Triple(item, secHint, secSubjHint))
                                }
                            }
                        }
                        root.has("questions") -> {
                            return JsonValidationResult(
                                isValid = false,
                                errorMessage = "Field 'questions' must be a JSON array.",
                                errorLineNumber = findApproximateLine(trimmed, "\"questions\""),
                                errorFieldName = "questions"
                            )
                        }
                        else -> {
                            return JsonValidationResult(
                                isValid = false,
                                errorMessage = "JSON object is missing required 'questions' array field.",
                                errorLineNumber = findApproximateLine(trimmed, "questions") ?: 1,
                                errorFieldName = "questions"
                            )
                        }
                    }
                } else {
                    return JsonValidationResult(
                        isValid = false,
                        errorMessage = "Invalid JSON syntax at Line 1: must start with '[' or '{'.",
                        errorLineNumber = 1,
                        errorFieldName = "root"
                    )
                }

                if (entries.isEmpty()) {
                    return JsonValidationResult(
                        isValid = false,
                        errorMessage = "JSON contains 0 questions. At least 1 valid question is required before saving.",
                        errorLineNumber = 1,
                        errorFieldName = "questions"
                    )
                }

                val parsedList = mutableListOf<ExamQuestion>()
                entries.forEachIndexed { i, (item, parentPartHint, parentSubjectHint) ->
                    val qNum = i + 1
                    val qHi = firstNonBlankJsonString(item, "question_hi", "questionHi")
                    val qEn = firstNonBlankJsonString(item, "question_en", "questionEn")
                    val qGeneric = firstNonBlankJsonString(item, "question", "text", "questionText", "question_text", "prompt")
                    val text = when {
                        qHi.isNotBlank() && qEn.isNotBlank() && qHi != qEn -> "$qHi\n$qEn"
                        qGeneric.isNotBlank() -> qGeneric
                        qHi.isNotBlank() -> qHi
                        else -> qEn
                    }
                    if (text.isBlank()) {
                        return JsonValidationResult(
                            isValid = false,
                            errorMessage = "Question #$qNum is missing question text ('question', 'text', 'question_hi', or 'question_en').",
                            errorLineNumber = findApproximateLine(trimmed, "\"id\"") ?: qNum,
                            errorFieldName = "questions[$i].question"
                        )
                    }

                    val normalizedOpts = com.example.domain.BulkQuestionParser.extractOptionsFromJsonObject(item)
                    val optA = normalizedOpts["A"].orEmpty()
                    val optB = normalizedOpts["B"].orEmpty()
                    val optC = normalizedOpts["C"].orEmpty()
                    val optD = normalizedOpts["D"].orEmpty()
                    if (optA.isBlank() || optB.isBlank() || optC.isBlank() || optD.isBlank()) {
                        val missingField = when {
                            optA.isBlank() -> "optionA"
                            optB.isBlank() -> "optionB"
                            optC.isBlank() -> "optionC"
                            else -> "optionD"
                        }
                        return JsonValidationResult(
                            isValid = false,
                            errorMessage = "Question #$qNum is missing required option '$missingField'. Provide flat optionA–optionD or nested options {\"A\"..\"D\"}.",
                            errorLineNumber = findApproximateLine(trimmed, text.take(20)) ?: qNum,
                            errorFieldName = "questions[$i].$missingField"
                        )
                    }

                    val ansLetter = com.example.domain.BulkQuestionParser.extractCorrectAnswerFromJsonObject(item)
                    if (ansLetter == null) {
                        val rawAns = firstNonBlankJsonString(
                            item,
                            "correctAnswer", "correctOption", "correct_answer", "correct_option",
                            "answer", "answerKey", "answer_key", "correct"
                        )
                        val detail = if (rawAns.isBlank()) "missing" else "'$rawAns'"
                        return JsonValidationResult(
                            isValid = false,
                            errorMessage = "Question #$qNum has invalid or missing correctAnswer ($detail). Must be one of A, B, C, or D.",
                            errorLineNumber = findApproximateLine(trimmed, text.take(20)) ?: qNum,
                            errorFieldName = "questions[$i].correctAnswer"
                        )
                    }

                    val optE = ExamQuestion.normalizeOptionE(normalizedOpts["E"].orEmpty())
                    val rawSubject = firstNonBlankJsonString(item, "subject", "category", "subjectName", "subject_name")
                        .ifBlank { parentSubjectHint }
                    val normalizedSubject = if (rawSubject.isBlank()) {
                        com.example.domain.BulkQuestionParser.UNCLASSIFIED_SUBJECT
                    } else {
                        com.example.domain.BulkQuestionParser.normalizeSubject(rawSubject)
                    }
                    val rawPartOrSection = firstNonBlankJsonString(
                        item,
                        "part", "section", "paperSection", "paper_section", "partName", "part_name", "paper"
                    ).ifBlank { parentPartHint }
                    val normalizedSection = com.example.domain.BulkQuestionParser.normalizePartOrSection(rawPartOrSection)

                    val explHi = firstNonBlankJsonString(item, "solution_hi", "explanation_hi")
                    val explEn = firstNonBlankJsonString(item, "solution_en", "explanation_en")
                    val explGeneric = firstNonBlankJsonString(item, "explanation", "solution", "rationale")
                    val explanation = when {
                        explHi.isNotBlank() && explEn.isNotBlank() && explHi != explEn -> "$explHi\n$explEn"
                        explGeneric.isNotBlank() -> explGeneric
                        explHi.isNotBlank() -> explHi
                        else -> explEn
                    }

                    parsedList.add(
                        ExamQuestion(
                            number = qNum,
                            subject = normalizedSubject,
                            topic = firstNonBlankJsonString(item, "topic", "chapter", "subtopic"),
                            source = firstNonBlankJsonString(item, "source", "reference"),
                            text = text,
                            optionA = optA,
                            optionB = optB,
                            optionC = optC,
                            optionD = optD,
                            optionE = optE,
                            correctOption = ansLetter,
                            explanation = explanation,
                            section = normalizedSection,
                            passage = firstNonBlankJsonString(item, "passage", "comprehension", "paragraph")
                        )
                    )
                }

                JsonValidationResult(
                    isValid = true,
                    parsedQuestions = parsedList
                )
            } catch (e: Exception) {
                val msg = e.message ?: "Malformed JSON syntax"
                val charIndex = Regex("""character\s+(\d+)""", RegexOption.IGNORE_CASE)
                    .find(msg)?.groupValues?.getOrNull(1)?.toIntOrNull()
                val lineNum = if (charIndex != null && charIndex <= trimmed.length) {
                    trimmed.substring(0, charIndex).count { it == '\n' } + 1
                } else {
                    1
                }
                JsonValidationResult(
                    isValid = false,
                    errorMessage = "JSON Syntax Error near Line $lineNum: $msg",
                    errorLineNumber = lineNum,
                    errorFieldName = "syntax"
                )
            }
        }

        private fun firstNonBlankJsonString(obj: JSONObject, vararg keys: String): String {
            for (key in keys) {
                if (obj.has(key) && !obj.isNull(key)) {
                    val raw = obj.opt(key)
                    if (raw is String && raw.isNotBlank()) return raw.trim()
                    if (raw is Number || raw is Boolean) return raw.toString().trim()
                }
            }
            return ""
        }

        private fun findApproximateLine(text: String, needle: String): Int? {
            if (needle.isBlank()) return null
            val idx = text.indexOf(needle)
            if (idx < 0) return null
            return text.substring(0, idx).count { it == '\n' } + 1
        }
    }
}
