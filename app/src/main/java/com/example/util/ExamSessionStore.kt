package com.example.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PersistedExamSession(
    val userId: String,
    val groupId: String,
    val testId: String,
    val endTimeEpochMillis: Long,
    val totalDurationSeconds: Long,
    val currentIndex: Int,
    val selectedAnswers: Map<Int, String>,
    val markedForReview: Set<Int>,
    val isPaused: Boolean = false,
    val pausedRemainingSeconds: Long = 0L,
    val questionTimesSec: Map<Int, Long> = emptyMap(),
    val visitedQuestions: Set<Int> = setOf(0),
    val startedAtEpochMillis: Long = 0L,
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
    val finalFiveMinAlertShown: Boolean = false
)

/**
 * Lightweight local persistence for active/paused examination sessions & Cooling Glass theme mode.
 * Ensures:
 *  - Active tests survive process death / restart using wall-clock endTimeEpochMillis.
 *  - Paused tests freeze exact pausedRemainingSeconds so aspirants can resume later
 *    from the exact remaining time without losing a single second.
 */
class ExamSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveExamSession(session: PersistedExamSession) {
        prefs.edit().putString(KEY_ACTIVE_EXAM, serializeSession(session).toString()).apply()
    }

    fun loadExamSession(userId: String): PersistedExamSession? {
        val raw = prefs.getString(KEY_ACTIVE_EXAM, null) ?: return null
        return parseSession(raw)?.takeIf { it.userId == userId }
    }

    fun clearExamSession() {
        prefs.edit().remove(KEY_ACTIVE_EXAM).apply()
    }

    fun savePausedSession(session: PersistedExamSession) {
        val paused = session.copy(
            isPaused = true,
            pausedRemainingSeconds = session.pausedRemainingSeconds.coerceAtLeast(1L)
        )
        val root = loadPausedRoot()
        root.put(pausedKey(paused.userId, paused.testId), serializeSession(paused))
        prefs.edit().putString(KEY_PAUSED_EXAMS, root.toString()).apply()
    }

    fun loadPausedSession(userId: String, testId: String): PersistedExamSession? {
        val root = loadPausedRoot()
        val obj = root.optJSONObject(pausedKey(userId, testId)) ?: return null
        return parseSession(obj.toString())?.takeIf { it.userId == userId && it.testId == testId && it.isPaused }
    }

    fun loadAllPausedSessions(userId: String): Map<String, PersistedExamSession> {
        val root = loadPausedRoot()
        val result = linkedMapOf<String, PersistedExamSession>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val obj = root.optJSONObject(k) ?: continue
            val parsed = parseSession(obj.toString()) ?: continue
            if (parsed.userId == userId && parsed.isPaused && parsed.pausedRemainingSeconds > 0L) {
                result[parsed.testId] = parsed
            }
        }
        return result
    }

    fun clearPausedSession(userId: String, testId: String) {
        val root = loadPausedRoot()
        val key = pausedKey(userId, testId)
        if (root.has(key)) {
            root.remove(key)
            prefs.edit().putString(KEY_PAUSED_EXAMS, root.toString()).apply()
        }
    }

    fun clearAllSessions() {
        prefs.edit()
            .remove(KEY_ACTIVE_EXAM)
            .remove(KEY_PAUSED_EXAMS)
            .apply()
    }

    fun setDarkGlassMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_GLASS_MODE, enabled).apply()
    }

    fun getDarkGlassMode(): Boolean = prefs.getBoolean(KEY_DARK_GLASS_MODE, false)

    fun getClearedNotificationIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        val raw = prefs.getString("${KEY_CLEARED_NOTIFS_PREFIX}_$userId", null) ?: return emptySet()
        return try {
            val arr = JSONArray(raw)
            buildSet {
                for (i in 0 until arr.length()) {
                    arr.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
                }
            }
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun markNotificationCleared(userId: String, notifId: String): Set<String> {
        if (userId.isBlank() || notifId.isBlank()) return getClearedNotificationIds(userId)
        val updated = (getClearedNotificationIds(userId) + notifId).toList().takeLast(500).toSet()
        saveClearedNotificationIds(userId, updated)
        return updated
    }

    fun markAllNotificationsCleared(userId: String, notifIds: Collection<String>): Set<String> {
        if (userId.isBlank()) return emptySet()
        val updated = (getClearedNotificationIds(userId) + notifIds.filter { it.isNotBlank() })
            .toList()
            .takeLast(500)
            .toSet()
        saveClearedNotificationIds(userId, updated)
        return updated
    }

    private fun saveClearedNotificationIds(userId: String, ids: Set<String>) {
        val arr = JSONArray()
        ids.forEach { arr.put(it) }
        prefs.edit().putString("${KEY_CLEARED_NOTIFS_PREFIX}_$userId", arr.toString()).apply()
    }

    private fun pausedKey(userId: String, testId: String): String = "$userId::$testId"

    private fun loadPausedRoot(): JSONObject {
        val raw = prefs.getString(KEY_PAUSED_EXAMS, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun serializeSession(session: PersistedExamSession): JSONObject = JSONObject().apply {
        put("userId", session.userId)
        put("groupId", session.groupId)
        put("testId", session.testId)
        put("endTimeEpochMillis", session.endTimeEpochMillis)
        put("totalDurationSeconds", session.totalDurationSeconds)
        put("currentIndex", session.currentIndex)
        put("isPaused", session.isPaused)
        put("pausedRemainingSeconds", session.pausedRemainingSeconds)
        val answersObj = JSONObject()
        session.selectedAnswers.forEach { (idx, opt) -> answersObj.put(idx.toString(), opt) }
        put("selectedAnswers", answersObj)
        val reviewArr = JSONArray()
        session.markedForReview.forEach { reviewArr.put(it) }
        put("markedForReview", reviewArr)
        val timesObj = JSONObject()
        session.questionTimesSec.forEach { (idx, sec) -> if (sec > 0L) timesObj.put(idx.toString(), sec) }
        put("questionTimesSec", timesObj)
        val visitedArr = JSONArray()
        session.visitedQuestions.forEach { visitedArr.put(it) }
        put("visitedQuestions", visitedArr)
        put("startedAtEpochMillis", session.startedAtEpochMillis)
        put("liveExamId", session.liveExamId ?: "")
        put("isLiveExam", session.isLiveExam)
        put("secureModeEnabled", session.secureModeEnabled)
        put("dynamicWatermark", session.dynamicWatermark)
        put("focusLossDetection", session.focusLossDetection)
        put("copyPasteBlock", session.copyPasteBlock)
        put("navigationExitLock", session.navigationExitLock)
        put("warningLimit", session.warningLimit)
        put("violationCount", session.violationCount)
        val violationsArr = JSONArray()
        session.violationLogs.forEach { violationsArr.put(it) }
        put("violationLogs", violationsArr)
        put("finalFiveMinAlertShown", session.finalFiveMinAlertShown)
    }

    private fun parseSession(raw: String): PersistedExamSession? {
        return try {
            val obj = JSONObject(raw)
            val savedUser = obj.optString("userId", "")
            val groupId = obj.optString("groupId", "")
            val testId = obj.optString("testId", "")
            if (savedUser.isBlank() || groupId.isBlank() || testId.isBlank()) return null

            val answersMap = linkedMapOf<Int, String>()
            val answersObj = obj.optJSONObject("selectedAnswers")
            if (answersObj != null) {
                val keys = answersObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val idx = k.toIntOrNull() ?: continue
                    val value = answersObj.optString(k, "")
                    if (value.isNotBlank()) answersMap[idx] = value
                }
            }

            val reviewSet = linkedSetOf<Int>()
            val reviewArr = obj.optJSONArray("markedForReview")
            if (reviewArr != null) {
                for (i in 0 until reviewArr.length()) {
                    reviewSet.add(reviewArr.optInt(i))
                }
            }

            val qTimesMap = linkedMapOf<Int, Long>()
            val qTimesObj = obj.optJSONObject("questionTimesSec")
            if (qTimesObj != null) {
                val tKeys = qTimesObj.keys()
                while (tKeys.hasNext()) {
                    val k = tKeys.next()
                    val idx = k.toIntOrNull() ?: continue
                    val sec = qTimesObj.optLong(k, 0L)
                    if (sec > 0L) qTimesMap[idx] = sec
                }
            }

            val visitedSet = linkedSetOf<Int>()
            val visitedArr = obj.optJSONArray("visitedQuestions")
            if (visitedArr != null) {
                for (i in 0 until visitedArr.length()) {
                    visitedSet.add(visitedArr.optInt(i))
                }
            }
            val currentIdx = obj.optInt("currentIndex", 0)
            if (visitedSet.isEmpty()) {
                visitedSet.add(0)
                visitedSet.add(currentIdx)
                visitedSet.addAll(answersMap.keys)
                visitedSet.addAll(reviewSet)
            }

            val violationList = mutableListOf<String>()
            val violationsArr = obj.optJSONArray("violationLogs")
            if (violationsArr != null) {
                for (i in 0 until violationsArr.length()) {
                    val v = violationsArr.optString(i, "")
                    if (v.isNotBlank()) violationList.add(v)
                }
            }

            PersistedExamSession(
                userId = savedUser,
                groupId = groupId,
                testId = testId,
                endTimeEpochMillis = obj.optLong("endTimeEpochMillis", 0L),
                totalDurationSeconds = obj.optLong("totalDurationSeconds", 0L),
                currentIndex = currentIdx,
                selectedAnswers = answersMap,
                markedForReview = reviewSet,
                isPaused = obj.optBoolean("isPaused", false),
                pausedRemainingSeconds = obj.optLong("pausedRemainingSeconds", 0L),
                questionTimesSec = qTimesMap,
                visitedQuestions = visitedSet,
                startedAtEpochMillis = obj.optLong("startedAtEpochMillis", 0L),
                liveExamId = obj.optString("liveExamId", "").takeIf { it.isNotBlank() },
                isLiveExam = obj.optBoolean("isLiveExam", false),
                secureModeEnabled = obj.optBoolean("secureModeEnabled", false),
                dynamicWatermark = obj.optBoolean("dynamicWatermark", false),
                focusLossDetection = obj.optBoolean("focusLossDetection", false),
                copyPasteBlock = obj.optBoolean("copyPasteBlock", false),
                navigationExitLock = obj.optBoolean("navigationExitLock", false),
                warningLimit = obj.optInt("warningLimit", 3),
                violationCount = obj.optInt("violationCount", 0),
                violationLogs = violationList,
                finalFiveMinAlertShown = obj.optBoolean("finalFiveMinAlertShown", false)
            )
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val PREFS_NAME = "tre4_exam_session_prefs"
        const val KEY_ACTIVE_EXAM = "active_exam_json_v1"
        const val KEY_PAUSED_EXAMS = "paused_exams_json_v2"
        const val KEY_DARK_GLASS_MODE = "cooling_glass_dark_mode"
        const val KEY_CLEARED_NOTIFS_PREFIX = "cleared_notifications_v1"
    }
}
