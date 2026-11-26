package com.example.data.firebase

import android.content.Context
import com.example.R
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
import com.example.data.model.LangQualifyingStatus
import com.example.data.model.LanguageFailRankPolicy
import com.example.data.model.LanguagePref
import com.example.data.model.LiveExam
import com.example.data.model.LiveExamSession
import com.example.data.model.OptionEMode
import com.example.data.model.PaperType
import com.example.data.model.QuestionPoll
import com.example.data.model.RankCategory
import com.example.data.model.SetType
import com.example.data.model.SolutionReattemptRecord
import com.example.data.model.TestAttempt
import com.example.data.model.TieBreakPolicy
import com.example.data.model.UserProfile
import com.example.domain.EvaluationResult
import com.example.util.WhatsAppHelper
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.random.Random

/**
 * Centralized Cloud Firestore repository for TRE 4.0.
 * Always connects to R.string.firestore_database_id (never the unprovisioned default DB).
 */
class TreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val estimate = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Aspirant must sign in with Google before accessing Firestore.")

    private fun isBootstrappedSuperAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals("gopal.guptas27@gmail.com", ignoreCase = true) && user.isEmailVerified
    }

    // ==================== REALTIME OBSERVATION STREAMS ====================

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user private profile stream." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(UserProfile::class.java, estimate) else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeGroup(groupId: String): Flow<ExamGroup?> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId"
        emitAll(
            db.collection("groups").document(groupId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(ExamGroup::class.java, estimate) else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeGroupMembers(groupId: String): Flow<List<GroupMember>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/members"
        emitAll(
            db.collection("groups").document(groupId).collection("members")
                .whereEqualTo("groupId", groupId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GroupMember::class.java, estimate)
                        .sortedByDescending { it.gsAverageScore }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeGroupTests(groupId: String, includeUnpublished: Boolean = true): Flow<List<ExamTest>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/tests"
        val collectionRef = db.collection("groups").document(groupId).collection("tests")
        val alignedQuery = if (includeUnpublished) {
            collectionRef.whereEqualTo("groupId", groupId)
        } else {
            // Single-field array-contains-any matches firestore.rules with ZERO composite index required
            collectionRef.whereArrayContainsAny("visibleToUids", listOf("ALL", uid))
        }
        emitAll(
            alignedQuery
                .snapshots()
                .map { snap ->
                    snap.toObjects(ExamTest::class.java, estimate)
                        .filter { includeUnpublished || it.isVisibleToMember(uid, isAdmin = false) }
                        .sortedWith(ExamTest.newestFirstComparator)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeGroupAttempts(groupId: String): Flow<List<TestAttempt>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/attempts"
        emitAll(
            db.collection("groups").document(groupId).collection("attempts")
                .whereEqualTo("groupId", groupId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TestAttempt::class.java, estimate)
                        .sortedByDescending { it.submittedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    /**
     * Fetches ALL eligible submitted attempts for a specific [testId] with NO document limit
     * (supports 150, 300, 1000+ participants) and strictly isolated from other sets.
     */
    fun observeSetAttempts(groupId: String, testId: String): Flow<List<TestAttempt>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/attempts"
        emitAll(
            db.collection("groups").document(groupId).collection("attempts")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("testId", testId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TestAttempt::class.java, estimate)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeCurrentAffairs(groupId: String): Flow<List<CurrentAffairItem>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/currentAffairs"
        emitAll(
            db.collection("groups").document(groupId).collection("currentAffairs")
                .whereEqualTo("groupId", groupId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CurrentAffairItem::class.java, estimate)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observePolls(groupId: String): Flow<List<QuestionPoll>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/polls"
        emitAll(
            db.collection("groups").document(groupId).collection("polls")
                .whereEqualTo("groupId", groupId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(QuestionPoll::class.java, estimate)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeDiscussions(groupId: String): Flow<List<DiscussionComment>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/discussions"
        emitAll(
            db.collection("groups").document(groupId).collection("discussions")
                .whereEqualTo("groupId", groupId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DiscussionComment::class.java, estimate)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeNotifications(groupId: String): Flow<List<GroupNotification>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/notifications"
        emitAll(
            db.collection("groups").document(groupId).collection("notifications")
                .whereEqualTo("groupId", groupId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GroupNotification::class.java, estimate)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAdminContact(groupId: String): Flow<AdminContactProfile?> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/adminContact/official"
        emitAll(
            db.collection("groups").document(groupId)
                .collection("adminContact").document("official")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(AdminContactProfile::class.java, estimate) else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeCategoryIcons(groupId: String): Flow<Map<String, CategoryIconConfig>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/categoryIcons"
        emitAll(
            db.collection("groups").document(groupId).collection("categoryIcons")
                .whereEqualTo("groupId", groupId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CategoryIconConfig::class.java, estimate)
                        .associateBy { it.categoryKey.ifBlank { it.id } }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveCategoryDefaultIcon(
        groupId: String,
        category: RankCategory,
        iconKey: String?,
        customIconBase64: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("groups").document(groupId)
            .collection("categoryIcons").document(category.name)
        return try {
            val payload = linkedMapOf<String, Any?>(
                "groupId" to groupId,
                "categoryKey" to category.name,
                "iconKey" to iconKey?.trim()?.take(60)?.takeIf { it.isNotBlank() },
                "customIconBase64" to customIconBase64?.takeIf { it.isNotBlank() && it.length <= 260_000 },
                "updatedByUid" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategoryDefaultIcon(
        groupId: String,
        category: RankCategory
    ): Result<Unit> {
        val path = "groups/$groupId/categoryIcons/${category.name}"
        return try {
            requireUserId()
            db.collection("groups").document(groupId)
                .collection("categoryIcons").document(category.name)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeLiveExams(groupId: String): Flow<List<LiveExam>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/liveExams"
        emitAll(
            db.collection("groups").document(groupId).collection("liveExams")
                .whereEqualTo("groupId", groupId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LiveExam::class.java, estimate)
                        .sortedByDescending { it.startTimeEpochMillis }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeLiveExamSessions(groupId: String): Flow<List<LiveExamSession>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/liveExamSessions"
        emitAll(
            db.collection("groups").document(groupId).collection("liveExamSessions")
                .whereEqualTo("groupId", groupId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LiveExamSession::class.java, estimate)
                        .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    /**
     * ADMIN-ONLY stream of registered users in the active group (including optional WhatsApp numbers).
     * Never invoked for regular USER role; strictly guarded by firestore.rules.
     */
    fun observeRegisteredUsersForAdmin(groupId: String): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection("users")
                .whereEqualTo("activeGroupId", groupId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, estimate)
                        .sortedBy { it.displayName.lowercase(Locale.US) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAppUpdates(groupId: String?, isAdmin: Boolean): Flow<List<AppUpdateRelease>> = flow {
        val uid = requireUserId()
        val path = "appUpdates"
        val query = if (isAdmin && !groupId.isNullOrBlank()) {
            db.collection("appUpdates").whereEqualTo("groupId", groupId)
        } else {
            db.collection("appUpdates").whereEqualTo("published", true)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(AppUpdateRelease::class.java, estimate)
                        .sortedByDescending { it.versionCode }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    /**
     * ADMIN-ONLY stream of unique Banner and Poll content views for the active group.
     * Regular Users never collect this stream; guarded by firestore.rules.
     */
    fun observeContentViews(groupId: String): Flow<List<ContentViewRecord>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/contentViews"
        emitAll(
            db.collection("groups").document(groupId).collection("contentViews")
                .whereEqualTo("groupId", groupId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ContentViewRecord::class.java, estimate)
                        .sortedByDescending { it.viewedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    /**
     * Non-blocking deduplicated view recorder for Banners and Polls.
     * Creates a deterministic document `<TYPE>_<CONTENT_ID>_<UID>` only once per user per item.
     */
    suspend fun recordUniqueContentView(
        groupId: String,
        contentId: String,
        contentType: String,
        userName: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        if (groupId.isBlank() || contentId.isBlank()) return
        val normalizedType = if (contentType.equals(ContentViewRecord.CONTENT_TYPE_POLL, ignoreCase = true)) {
            ContentViewRecord.CONTENT_TYPE_POLL
        } else {
            ContentViewRecord.CONTENT_TYPE_BANNER
        }
        val docId = ContentViewRecord.docId(normalizedType, contentId, uid)
        val docRef = db.collection("groups").document(groupId)
            .collection("contentViews").document(docId)
        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) return
            val payload = mapOf(
                "groupId" to groupId,
                "contentId" to contentId.take(120),
                "contentType" to normalizedType,
                "userId" to uid,
                "userName" to userName.trim().ifBlank { "User" }.take(120),
                "viewedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Silently fail offline or on duplicate creation race without blocking UX
        }
    }

    // ==================== USER PROFILE & GROUP MUTATIONS ====================

    suspend fun registerUserProfile(
        displayName: String,
        photoBase64: String?,
        whatsappNumber: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val cleanName = displayName.trim()
            require(cleanName.length in 2..60) { "Display name must be between 2 and 60 characters." }
            val normalizedWa = if (whatsappNumber.isNullOrBlank()) null else {
                WhatsAppHelper.normalizePhoneNumber(whatsappNumber)
                    ?: return Result.failure(IllegalArgumentException("Enter a valid WhatsApp number (e.g. +919876543210)."))
            }
            val assignedRole = if (isBootstrappedSuperAdmin()) AppRole.SUPER_ADMIN.name else AppRole.ASPIRANT.name
            val payload = linkedMapOf<String, Any?>(
                "userId" to uid,
                "displayName" to cleanName,
                "email" to auth.currentUser?.email?.take(120),
                "whatsappNumber" to normalizedWa,
                "photoBase64" to photoBase64?.takeIf { it.length <= 220000 },
                "role" to assignedRole,
                "languagePref" to LanguagePref.BILINGUAL.name,
                "publicSharing" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.collection("users").document(uid).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        photoBase64: String?,
        languagePref: LanguagePref,
        publicSharing: Boolean,
        activeGroupId: String?,
        whatsappNumber: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val cleanName = displayName.trim()
            require(cleanName.length in 2..60) { "Display name must be 2–60 characters." }
            val normalizedWa: String = if (whatsappNumber.isNullOrBlank()) {
                ""
            } else {
                WhatsAppHelper.normalizePhoneNumber(whatsappNumber)
                    ?: return Result.failure(IllegalArgumentException("Enter a valid WhatsApp number (e.g. +919876543210)."))
            }
            val updates = linkedMapOf<String, Any?>(
                "displayName" to cleanName,
                "whatsappNumber" to normalizedWa,
                "languagePref" to languagePref.name,
                "publicSharing" to publicSharing,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            auth.currentUser?.email?.take(120)?.let { updates["email"] = it }
            if (photoBase64 != null && photoBase64.length <= 220000) {
                updates["photoBase64"] = photoBase64
            }
            db.collection("users").document(uid).update(updates.filterValues { it != null }).await()

            // Public GroupMember NEVER stores email or WhatsApp number (Strict Profile Privacy)
            if (!activeGroupId.isNullOrBlank()) {
                val memberUpdates = linkedMapOf<String, Any?>(
                    "displayName" to cleanName,
                    "publicSharing" to publicSharing,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (photoBase64 != null && photoBase64.length <= 220000) {
                    memberUpdates["photoBase64"] = photoBase64
                }
                db.collection("groups").document(activeGroupId)
                    .collection("members").document(uid)
                    .update(memberUpdates.filterValues { it != null })
                    .await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAdminContact(
        groupId: String,
        adminName: String,
        adminPhotoBase64: String?,
        adminWhatsAppNumber: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "groups/$groupId/adminContact/official"
        return try {
            val cleanName = adminName.trim()
            require(cleanName.length in 2..60) { "Admin display name must be 2–60 characters." }
            val normalizedWa = if (adminWhatsAppNumber.isNullOrBlank()) {
                ""
            } else {
                WhatsAppHelper.normalizePhoneNumber(adminWhatsAppNumber)
                    ?: return Result.failure(IllegalArgumentException("Enter a valid Admin WhatsApp number (e.g. +919876543210)."))
            }
            val payload = linkedMapOf<String, Any?>(
                "groupId" to groupId,
                "adminUid" to uid,
                "adminName" to cleanName,
                "adminPhotoBase64" to adminPhotoBase64?.takeIf { it.length <= 220000 },
                "adminWhatsAppNumber" to normalizedWa,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.collection("groups").document(groupId)
                .collection("adminContact").document("official")
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createGroup(
        name: String,
        description: String,
        creatorProfile: UserProfile
    ): Result<ExamGroup> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val groupRef = db.collection("groups").document()
        val groupId = groupRef.id
        return try {
            val cleanName = name.trim()
            val cleanDesc = description.trim().ifBlank { "Official TRE 4.0 Aspirant Examination & Ranking Group" }
            require(cleanName.length in 2..80) { "Group name must be 2–80 characters." }

            val inviteCode = generateInviteCode()
            val groupPayload = mapOf(
                "name" to cleanName,
                "description" to cleanDesc.take(400),
                "inviteCode" to inviteCode,
                "createdByUid" to uid,
                "createdByName" to creatorProfile.displayName,
                "memberCount" to 1L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            groupRef.set(groupPayload).await()

            val invitePayload = mapOf(
                "groupId" to groupId,
                "groupName" to cleanName,
                "createdByUid" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("inviteCodes").document(inviteCode).set(invitePayload).await()

            val memberRole = if (isBootstrappedSuperAdmin()) AppRole.SUPER_ADMIN.name else AppRole.GROUP_ADMIN.name
            val memberPayload = linkedMapOf<String, Any?>(
                "groupId" to groupId,
                "userId" to uid,
                "displayName" to creatorProfile.displayName,
                "photoBase64" to creatorProfile.photoBase64,
                "role" to memberRole,
                "gsTestsAttempted" to 0L,
                "gsTotalScore" to 0.0,
                "gsAverageScore" to 0.0,
                "gsHighestScore" to 0.0,
                "gsLowestScore" to 0.0,
                "latestGsScore" to 0.0,
                "weeklyGsTotal" to 0.0,
                "weeklyGsCount" to 0L,
                "monthlyGsTotal" to 0.0,
                "monthlyGsCount" to 0L,
                "langScore" to 0.0,
                "langHindiScore" to 0.0,
                "langEnglishScore" to 0.0,
                "langStatus" to LangQualifyingStatus.NOT_ATTEMPTED.name,
                "previousRank" to 0L,
                "publicSharing" to creatorProfile.publicSharing,
                "joinedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            groupRef.collection("members").document(uid).set(memberPayload).await()

            db.collection("users").document(uid).update(
                mapOf(
                    "activeGroupId" to groupId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(
                ExamGroup(
                    id = groupId,
                    name = cleanName,
                    description = cleanDesc,
                    inviteCode = inviteCode,
                    createdByUid = uid,
                    createdByName = creatorProfile.displayName,
                    memberCount = 1L
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "groups/$groupId")
            Result.failure(e)
        }
    }

    suspend fun joinGroupByCode(
        rawCode: String,
        profile: UserProfile
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val code = rawCode.trim().uppercase(Locale.US)
        if (!code.matches(Regex("^[A-Z0-9_\\-]{4,20}$"))) {
            return Result.failure(IllegalArgumentException("Please enter a valid 4–20 character invite code."))
        }
        return try {
            val inviteSnap = db.collection("inviteCodes").document(code).get().await()
            if (!inviteSnap.exists()) {
                return Result.failure(IllegalArgumentException("Invitation code '$code' not found."))
            }
            val groupId = inviteSnap.getString("groupId")
                ?: return Result.failure(IllegalStateException("Invite code is missing group reference."))

            val memberDocRef = db.collection("groups").document(groupId).collection("members").document(uid)
            val existingMember = memberDocRef.get().await()
            if (!existingMember.exists()) {
                val role = if (isBootstrappedSuperAdmin()) AppRole.SUPER_ADMIN.name else AppRole.ASPIRANT.name
                val memberPayload = linkedMapOf<String, Any?>(
                    "groupId" to groupId,
                    "userId" to uid,
                    "displayName" to profile.displayName,
                    "photoBase64" to profile.photoBase64,
                    "role" to role,
                    "gsTestsAttempted" to 0L,
                    "gsTotalScore" to 0.0,
                    "gsAverageScore" to 0.0,
                    "gsHighestScore" to 0.0,
                    "gsLowestScore" to 0.0,
                    "latestGsScore" to 0.0,
                    "weeklyGsTotal" to 0.0,
                    "weeklyGsCount" to 0L,
                    "monthlyGsTotal" to 0.0,
                    "monthlyGsCount" to 0L,
                    "langScore" to 0.0,
                    "langHindiScore" to 0.0,
                    "langEnglishScore" to 0.0,
                    "langStatus" to LangQualifyingStatus.NOT_ATTEMPTED.name,
                    "previousRank" to 0L,
                    "publicSharing" to profile.publicSharing,
                    "joinedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                memberDocRef.set(memberPayload).await()

                db.collection("groups").document(groupId).update(
                    mapOf(
                        "memberCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            db.collection("users").document(uid).update(
                mapOf(
                    "activeGroupId" to groupId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(groupId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "inviteCodes/$code")
            Result.failure(e)
        }
    }

    // ==================== TESTS & EXAM ATTEMPTS ====================

    suspend fun createTestSet(
        groupId: String,
        title: String,
        paperType: PaperType,
        difficulty: String,
        durationMinutes: Int,
        questions: List<ExamQuestion>,
        creator: GroupMember,
        publishImmediately: Boolean,
        negativeNum: Int = 1,
        negativeDen: Int = 3,
        unattemptedPenalty: Boolean = true,
        answerReleaseMode: String = "IMMEDIATE",
        approvedForAll: Boolean = publishImmediately,
        approvedMemberIds: List<String> = emptyList(),
        iconKey: String? = null,
        customIconBase64: String? = null,
        bannerImageBase64: String? = null,
        setType: SetType? = null,
        classLevel: String? = null,
        positiveMarks: Double = 1.0,
        optionEMode: OptionEMode = OptionEMode.NOT_ATTEMPTED,
        cutoffMarks: Double? = null,
        pauseAllowed: Boolean = true,
        langPassMarks: Double = 9.0,
        langMinHindi: Double = 0.0,
        langMinEnglish: Double = 0.0,
        langFailRankPolicy: LanguageFailRankPolicy = LanguageFailRankPolicy.SHOW_WITH_NOT_QUALIFIED_BADGE,
        tieBreakPolicy: TieBreakPolicy = TieBreakPolicy.SHARED_EQUAL_RANK,
        assembledFromTestIds: List<String> = emptyList()
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val testRef = db.collection("groups").document(groupId).collection("tests").document()
        val path = testRef.path
        return try {
            val cleanTitle = title.trim()
            require(cleanTitle.length in 3..140) { "Test title must be 3–140 characters." }
            require(questions.isNotEmpty()) { "Test must include at least one question." }
            val status = if (publishImmediately) "PUBLISHED" else "PENDING"
            val cleanApprovedIds = approvedMemberIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            val visibleTo = when {
                status != "PUBLISHED" -> emptyList()
                approvedForAll -> listOf("ALL")
                else -> cleanApprovedIds
            }
            val resolvedSetType = setType ?: SetType.fromPaperType(paperType)
            val payload = linkedMapOf<String, Any?>(
                "groupId" to groupId,
                "title" to cleanTitle,
                "paperType" to paperType.name,
                "difficulty" to difficulty,
                "status" to status,
                "creatorUid" to uid,
                "creatorName" to creator.displayName,
                "creatorPhotoBase64" to creator.photoBase64,
                "durationMinutes" to durationMinutes.coerceIn(5, 300).toLong(),
                "questionCount" to questions.size.toLong(),
                "negativeMarkNumerator" to negativeNum.coerceIn(0, 10).toLong(),
                "negativeMarkDenominator" to negativeDen.coerceIn(1, 10).toLong(),
                "unattemptedPenalty" to unattemptedPenalty,
                "questions" to questions.mapIndexed { i, q -> q.copy(number = i + 1).toMap() },
                "attemptCount" to 0L,
                "answerReleaseMode" to answerReleaseMode,
                "approvedForAll" to (status == "PUBLISHED" && approvedForAll),
                "approvedMemberIds" to if (status == "PUBLISHED" && !approvedForAll) cleanApprovedIds else emptyList<String>(),
                "visibleToUids" to visibleTo,
                "iconKey" to iconKey?.take(60)?.takeIf { it.isNotBlank() },
                "customIconBase64" to customIconBase64?.takeIf { it.isNotBlank() && it.length <= 260_000 },
                "bannerImageBase64" to bannerImageBase64?.takeIf { it.isNotBlank() && it.length <= 520_000 },
                "setType" to resolvedSetType.name,
                "classLevel" to (classLevel?.take(40) ?: resolvedSetType.classTag),
                "positiveMarks" to positiveMarks.coerceIn(0.25, 10.0),
                "optionEMode" to optionEMode.name,
                "cutoffMarks" to cutoffMarks,
                "pauseAllowed" to pauseAllowed,
                "langPassMarks" to langPassMarks.coerceIn(0.0, 30.0),
                "langMinHindi" to langMinHindi.coerceIn(0.0, 22.0),
                "langMinEnglish" to langMinEnglish.coerceIn(0.0, 8.0),
                "langFailRankPolicy" to langFailRankPolicy.name,
                "tieBreakPolicy" to tieBreakPolicy.name,
                "assembledFromTestIds" to assembledFromTestIds.take(10),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            testRef.set(payload).await()
            Result.success(testRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTestExamRules(
        groupId: String,
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
    ): Result<Unit> {
        val path = "groups/$groupId/tests/$testId"
        return try {
            requireUserId()
            val updates = linkedMapOf<String, Any?>(
                "positiveMarks" to positiveMarks.coerceIn(0.25, 10.0),
                "negativeMarkNumerator" to negativeNum.coerceIn(0, 10).toLong(),
                "negativeMarkDenominator" to negativeDen.coerceIn(1, 10).toLong(),
                "unattemptedPenalty" to unattemptedPenalty,
                "optionEMode" to optionEMode.name,
                "cutoffMarks" to cutoffMarks,
                "pauseAllowed" to pauseAllowed,
                "langPassMarks" to langPassMarks.coerceIn(0.0, 30.0),
                "langMinHindi" to langMinHindi.coerceIn(0.0, 22.0),
                "langMinEnglish" to langMinEnglish.coerceIn(0.0, 8.0),
                "langFailRankPolicy" to langFailRankPolicy.name,
                "tieBreakPolicy" to tieBreakPolicy.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("groups").document(groupId).collection("tests").document(testId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTestStatus(groupId: String, testId: String, newStatus: String): Result<Unit> {
        val publishAll = newStatus == "PUBLISHED"
        return approveTestWithAccess(
            groupId = groupId,
            testId = testId,
            newStatus = newStatus,
            approvedForAll = publishAll,
            approvedMemberIds = emptyList()
        )
    }

    suspend fun approveTestWithAccess(
        groupId: String,
        testId: String,
        newStatus: String,
        approvedForAll: Boolean,
        approvedMemberIds: List<String>
    ): Result<Unit> {
        val path = "groups/$groupId/tests/$testId"
        return try {
            requireUserId()
            val cleanIds = approvedMemberIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            if (newStatus == "PUBLISHED" && !approvedForAll && cleanIds.isEmpty()) {
                return Result.failure(IllegalArgumentException("Select at least one member or choose All Members."))
            }
            val visibleTo = when {
                newStatus != "PUBLISHED" -> emptyList()
                approvedForAll -> listOf("ALL")
                else -> cleanIds
            }
            db.collection("groups").document(groupId).collection("tests").document(testId)
                .update(
                    mapOf(
                        "status" to newStatus,
                        "approvedForAll" to (newStatus == "PUBLISHED" && approvedForAll),
                        "approvedMemberIds" to if (newStatus == "PUBLISHED" && !approvedForAll) cleanIds else emptyList<String>(),
                        "visibleToUids" to visibleTo,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTestSet(
        groupId: String,
        testId: String,
        title: String,
        difficulty: String,
        durationMinutes: Int,
        questions: List<ExamQuestion>,
        isAdmin: Boolean,
        unattemptedPenalty: Boolean = true,
        answerReleaseMode: String = "IMMEDIATE",
        iconKey: String? = null,
        customIconBase64: String? = null,
        bannerImageBase64: String? = null
    ): Result<Unit> {
        val path = "groups/$groupId/tests/$testId"
        return try {
            requireUserId()
            val cleanTitle = title.trim()
            require(cleanTitle.length in 3..140) { "Test title must be 3–140 characters." }
            require(questions.size in 1..150) { "Test must include between 1 and 150 questions." }
            val normalizedDifficulty = difficulty.trim().uppercase().let {
                if (it in listOf("EASY", "MODERATE", "HARD")) it else "MODERATE"
            }
            val normalizedReleaseMode = if (answerReleaseMode.trim().equals("SCHEDULED", ignoreCase = true)) {
                "SCHEDULED"
            } else {
                "IMMEDIATE"
            }
            val updates = linkedMapOf<String, Any>(
                "title" to cleanTitle,
                "difficulty" to normalizedDifficulty,
                "durationMinutes" to durationMinutes.coerceIn(1, 300).toLong(),
                "questionCount" to questions.size.toLong(),
                "questions" to questions.mapIndexed { i, q -> q.copy(number = i + 1).toMap() },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isAdmin) {
                updates["unattemptedPenalty"] = unattemptedPenalty
                updates["answerReleaseMode"] = normalizedReleaseMode
                if (iconKey != null) updates["iconKey"] = iconKey.take(60)
                if (customIconBase64 != null) updates["customIconBase64"] = customIconBase64.take(260_000)
                if (bannerImageBase64 != null) updates["bannerImageBase64"] = bannerImageBase64.take(520_000)
            }
            db.collection("groups").document(groupId).collection("tests").document(testId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTestIcon(
        groupId: String,
        testId: String,
        iconKey: String?,
        customIconBase64: String?,
        bannerImageBase64: String? = null,
        updateBanner: Boolean = false
    ): Result<Unit> {
        val path = "groups/$groupId/tests/$testId"
        return try {
            requireUserId()
            val updates = linkedMapOf<String, Any>(
                "iconKey" to (iconKey?.take(60) ?: ""),
                "customIconBase64" to (customIconBase64?.take(260_000) ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (updateBanner) {
                updates["bannerImageBase64"] = bannerImageBase64?.take(520_000) ?: ""
            }
            db.collection("groups").document(groupId).collection("tests").document(testId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTestSet(groupId: String, testId: String): Result<Unit> {
        val path = "groups/$groupId/tests/$testId"
        return try {
            requireUserId()
            db.collection("groups").document(groupId).collection("tests").document(testId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAllTestsInGroup(groupId: String, testIds: List<String>): Result<Int> {
        val path = "groups/$groupId/tests"
        return try {
            requireUserId()
            if (testIds.isEmpty()) return Result.success(0)
            var deleted = 0
            testIds.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.delete(db.collection("groups").document(groupId).collection("tests").document(id))
                }
                batch.commit().await()
                deleted += chunk.size
            }
            Result.success(deleted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    /**
     * Fresh Launch Reset (Group Admin / Super Admin only):
     * Permanently deletes ALL trial tests, ALL attempts (including Admin's trial attempts),
     * polls (+ votes & privateKey), current affairs banners, doubt discussions, and notifications,
     * and resets every group member's score & ranking stats back to zero while preserving accounts.
     */
    suspend fun wipeAllGroupTrialData(groupId: String): Result<String> {
        val groupDoc = db.collection("groups").document(groupId)
        val path = "groups/$groupId"
        return try {
            requireUserId()
            var deletedTests = 0
            var deletedAttempts = 0
            var deletedPolls = 0
            var deletedOther = 0
            var resetMembers = 0

            suspend fun deleteDocs(refs: List<com.google.firebase.firestore.DocumentReference>): Int {
                if (refs.isEmpty()) return 0
                refs.chunked(350).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it) }
                    batch.commit().await()
                }
                return refs.size
            }

            // 1. Delete all attempts (including Admin trial attempts)
            val attemptsSnap = groupDoc.collection("attempts").whereEqualTo("groupId", groupId).get().await()
            deletedAttempts = deleteDocs(attemptsSnap.documents.map { it.reference })

            // 2. Delete all tests
            val testsSnap = groupDoc.collection("tests").whereEqualTo("groupId", groupId).get().await()
            deletedTests = deleteDocs(testsSnap.documents.map { it.reference })

            // 3. Delete all polls + subcollections (votes & privateKey)
            val pollsSnap = groupDoc.collection("polls").whereEqualTo("groupId", groupId).get().await()
            for (pollDoc in pollsSnap.documents) {
                val votesSnap = pollDoc.reference.collection("votes").get().await()
                deleteDocs(votesSnap.documents.map { it.reference })
                val keySnap = pollDoc.reference.collection("privateKey").get().await()
                deleteDocs(keySnap.documents.map { it.reference })
            }
            deletedPolls = deleteDocs(pollsSnap.documents.map { it.reference })

            // 4. Delete current affairs, discussions, notifications, liveExams, liveExamSessions, dailyTargets, and contentViews
            val caSnap = groupDoc.collection("currentAffairs").whereEqualTo("groupId", groupId).get().await()
            deletedOther += deleteDocs(caSnap.documents.map { it.reference })

            val discSnap = groupDoc.collection("discussions").whereEqualTo("groupId", groupId).get().await()
            deletedOther += deleteDocs(discSnap.documents.map { it.reference })

            val notifSnap = groupDoc.collection("notifications").whereEqualTo("groupId", groupId).get().await()
            deletedOther += deleteDocs(notifSnap.documents.map { it.reference })

            val liveExamsSnap = groupDoc.collection("liveExams").whereEqualTo("groupId", groupId).get().await()
            val deletedLiveExams = deleteDocs(liveExamsSnap.documents.map { it.reference })

            val liveSessionsSnap = groupDoc.collection("liveExamSessions").whereEqualTo("groupId", groupId).get().await()
            deleteDocs(liveSessionsSnap.documents.map { it.reference })

            val dailyTargetsSnap = groupDoc.collection("dailyTargets").whereEqualTo("groupId", groupId).get().await()
            deletedOther += deleteDocs(dailyTargetsSnap.documents.map { it.reference })

            val contentViewsSnap = groupDoc.collection("contentViews").whereEqualTo("groupId", groupId).get().await()
            deleteDocs(contentViewsSnap.documents.map { it.reference })

            // 5. Reset every member's scores & ranking stats to 0
            val membersSnap = groupDoc.collection("members").whereEqualTo("groupId", groupId).get().await()
            val zeroPayload = mapOf(
                "gsTestsAttempted" to 0L,
                "gsTotalScore" to 0.0,
                "gsAverageScore" to 0.0,
                "gsHighestScore" to 0.0,
                "gsLowestScore" to 0.0,
                "latestGsScore" to 0.0,
                "latestGsTestTitle" to "",
                "weeklyGsTotal" to 0.0,
                "weeklyGsCount" to 0L,
                "monthlyGsTotal" to 0.0,
                "monthlyGsCount" to 0L,
                "langScore" to 0.0,
                "langHindiScore" to 0.0,
                "langEnglishScore" to 0.0,
                "langStatus" to LangQualifyingStatus.NOT_ATTEMPTED.name,
                "previousRank" to 0L,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            membersSnap.documents.chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.update(doc.reference, zeroPayload) }
                batch.commit().await()
                resetMembers += chunk.size
            }

            Result.success(
                "Fresh Launch Complete: cleared $deletedTests set(s), $deletedLiveExams live exam(s), $deletedAttempts attempt(s), $deletedPolls poll(s), $deletedOther post(s) & reset $resetMembers member stat(s) to 0."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun submitExamAttempt(
        groupId: String,
        test: ExamTest,
        member: GroupMember,
        selectedAnswers: Map<Int, String>,
        eval: EvaluationResult,
        timeSpentSeconds: Long,
        existingUserAttempts: List<TestAttempt>,
        questionTimesSec: Map<Int, Long> = emptyMap(),
        startedAtEpochMillis: Long = 0L,
        completionStatus: String = "COMPLETED"
    ): Result<TestAttempt> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val attemptId = "${test.id}_$uid"
        val attemptPath = "groups/$groupId/attempts/$attemptId"
        val attemptRef = db.collection("groups").document(groupId).collection("attempts").document(attemptId)
        val testRef = db.collection("groups").document(groupId).collection("tests").document(test.id)
        val memberRef = db.collection("groups").document(groupId).collection("members").document(uid)
        return try {
            val answersStringMap = selectedAnswers.mapKeys { it.key.toString() }
            val questionTimesStringMap = questionTimesSec
                .filterValues { it > 0L }
                .mapKeys { it.key.toString() }
            val resolvedSetType = test.resolvedSetType()
            val langStatusStr = when {
                !resolvedSetType.hasLanguageSection -> LangQualifyingStatus.NOT_ATTEMPTED.name
                eval.langPassed -> LangQualifyingStatus.PASS.name
                else -> LangQualifyingStatus.FAIL.name
            }
            val attemptPayload = linkedMapOf<String, Any?>(
                "groupId" to groupId,
                "testId" to test.id,
                "testTitle" to test.title,
                "paperType" to test.paperType,
                "userId" to uid,
                "aspirantName" to member.displayName,
                "aspirantPhotoBase64" to member.photoBase64,
                "answers" to answersStringMap,
                "correctCount" to eval.correctCount.toLong(),
                "incorrectCount" to eval.incorrectCount.toLong(),
                "dontKnowCount" to eval.dontKnowCount.toLong(),
                "notAttemptedCount" to eval.notAttemptedCount.toLong(),
                "totalScore" to eval.totalScore,
                "maxMarks" to eval.maxMarks,
                "percentage" to eval.percentage,
                "negativeDeducted" to eval.negativeDeducted,
                "subjectScores" to eval.subjectScores,
                "hindiScore" to eval.hindiScore,
                "englishScore" to eval.englishScore,
                "langPassed" to eval.langPassed,
                "timeSpentSeconds" to timeSpentSeconds.coerceIn(0L, 86400L),
                "questionTimesSec" to questionTimesStringMap,
                "setType" to resolvedSetType.name,
                "meritMarks" to eval.meritMarks,
                "totalMeritMarks" to eval.totalMeritMarks,
                "positiveMarksEarned" to eval.positiveMarksEarned,
                "languageTotalMarks" to eval.languageTotalMarks,
                "languageStatus" to langStatusStr,
                "meritCorrectCount" to eval.meritCorrectCount.toLong(),
                "meritIncorrectCount" to eval.meritIncorrectCount.toLong(),
                "meritUnattemptedCount" to (eval.meritDontKnowCount + eval.meritNotAttemptedCount).toLong(),
                "startedAtEpochMillis" to startedAtEpochMillis.coerceAtLeast(0L),
                "completionStatus" to completionStatus.take(30),
                "submittedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            // Atomic transaction prevents duplicate submissions & partial score/counter writes
            db.runTransaction { tx ->
                val existingAttempt = tx.get(attemptRef)
                if (existingAttempt.exists()) {
                    throw IllegalStateException("Duplicate submission prevented: you already submitted this test.")
                }
                val liveTestSnap = tx.get(testRef)
                val liveMemberSnap = tx.get(memberRef)
                val currentAttemptCount = liveTestSnap.getLong("attemptCount") ?: 0L

                tx.set(attemptRef, attemptPayload)
                tx.update(
                    testRef,
                    mapOf(
                        "attemptCount" to (currentAttemptCount + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val memberUpdates = linkedMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val liveWeeklyTotal = liveMemberSnap.getDouble("weeklyGsTotal") ?: member.weeklyGsTotal
                val liveWeeklyCount = liveMemberSnap.getLong("weeklyGsCount") ?: member.weeklyGsCount
                val liveMonthlyTotal = liveMemberSnap.getDouble("monthlyGsTotal") ?: member.monthlyGsTotal
                val liveMonthlyCount = liveMemberSnap.getLong("monthlyGsCount") ?: member.monthlyGsCount

                val allMeritTypes = setOf(
                    PaperType.COMMON_GS.name,
                    PaperType.COMBINED.name,
                    PaperType.CLASS_6_8_MATH.name,
                    PaperType.CLASS_6_8_SCIENCE.name,
                    PaperType.CLASS_9_10_MATH_80.name,
                    PaperType.CLASS_6_8_MATH_SCIENCE.name,
                    PaperType.CLASS_9_10_MATH.name
                )

                when (test.parsedPaperType()) {
                    PaperType.COMMON_GS,
                    PaperType.CLASS_6_8_MATH,
                    PaperType.CLASS_6_8_SCIENCE,
                    PaperType.CLASS_9_10_MATH_80 -> {
                        val meritScoreOnly = eval.meritMarks
                        val prevMeritScores = existingUserAttempts
                            .filter { it.userId == uid && it.paperType in allMeritTypes && it.testId != test.id }
                            .map { it.effectiveMeritScore() }
                        val allGsScores = prevMeritScores + meritScoreOnly
                        val count = allGsScores.size.toLong()
                        val total = allGsScores.sum()
                        val avg = if (count > 0) total / count.toDouble() else 0.0
                        val highest = allGsScores.maxOrNull() ?: meritScoreOnly
                        val lowest = allGsScores.minOrNull() ?: meritScoreOnly

                        memberUpdates["gsTestsAttempted"] = count
                        memberUpdates["gsTotalScore"] = total
                        memberUpdates["gsAverageScore"] = avg
                        memberUpdates["gsHighestScore"] = highest
                        memberUpdates["gsLowestScore"] = lowest
                        memberUpdates["latestGsScore"] = meritScoreOnly
                        memberUpdates["latestGsTestTitle"] = test.title.take(140)
                        memberUpdates["weeklyGsTotal"] = liveWeeklyTotal + meritScoreOnly
                        memberUpdates["weeklyGsCount"] = liveWeeklyCount + 1L
                        memberUpdates["monthlyGsTotal"] = liveMonthlyTotal + meritScoreOnly
                        memberUpdates["monthlyGsCount"] = liveMonthlyCount + 1L
                    }
                    PaperType.LANGUAGE -> {
                        val status = if (eval.langPassed) LangQualifyingStatus.PASS.name else LangQualifyingStatus.FAIL.name
                        memberUpdates["langScore"] = eval.languageTotalMarks
                        memberUpdates["langHindiScore"] = eval.hindiScore
                        memberUpdates["langEnglishScore"] = eval.englishScore
                        memberUpdates["langStatus"] = status
                    }
                    PaperType.COMBINED,
                    PaperType.CLASS_6_8_MATH_SCIENCE,
                    PaperType.CLASS_9_10_MATH -> {
                        val langScoreOnly = eval.languageTotalMarks
                        val meritScoreOnly = eval.meritMarks
                        val prevMeritScores = existingUserAttempts
                            .filter { it.userId == uid && it.paperType in allMeritTypes && it.testId != test.id }
                            .map { it.effectiveMeritScore() }
                        val allGsScores = prevMeritScores + meritScoreOnly
                        val count = allGsScores.size.toLong()
                        val total = allGsScores.sum()
                        val avg = if (count > 0) total / count.toDouble() else 0.0
                        memberUpdates["gsTestsAttempted"] = count
                        memberUpdates["gsTotalScore"] = total
                        memberUpdates["gsAverageScore"] = avg
                        memberUpdates["gsHighestScore"] = allGsScores.maxOrNull() ?: meritScoreOnly
                        memberUpdates["gsLowestScore"] = allGsScores.minOrNull() ?: meritScoreOnly
                        memberUpdates["latestGsScore"] = meritScoreOnly
                        memberUpdates["latestGsTestTitle"] = test.title.take(140)
                        memberUpdates["weeklyGsTotal"] = liveWeeklyTotal + meritScoreOnly
                        memberUpdates["weeklyGsCount"] = liveWeeklyCount + 1L
                        memberUpdates["monthlyGsTotal"] = liveMonthlyTotal + meritScoreOnly
                        memberUpdates["monthlyGsCount"] = liveMonthlyCount + 1L
                        memberUpdates["langScore"] = langScoreOnly
                        memberUpdates["langHindiScore"] = eval.hindiScore
                        memberUpdates["langEnglishScore"] = eval.englishScore
                        memberUpdates["langStatus"] = if (eval.langPassed) LangQualifyingStatus.PASS.name else LangQualifyingStatus.FAIL.name
                    }
                    PaperType.PRACTICE -> {
                        // Practice sets do not distort Official Merit average ranking
                    }
                }

                if (memberUpdates.size > 1 && liveMemberSnap.exists()) {
                    tx.update(memberRef, memberUpdates)
                }
                null
            }.await()

            Result.success(
                TestAttempt(
                    id = attemptId,
                    groupId = groupId,
                    testId = test.id,
                    testTitle = test.title,
                    paperType = test.paperType,
                    userId = uid,
                    aspirantName = member.displayName,
                    aspirantPhotoBase64 = member.photoBase64,
                    answers = answersStringMap,
                    correctCount = eval.correctCount.toLong(),
                    incorrectCount = eval.incorrectCount.toLong(),
                    dontKnowCount = eval.dontKnowCount.toLong(),
                    notAttemptedCount = eval.notAttemptedCount.toLong(),
                    totalScore = eval.totalScore,
                    maxMarks = eval.maxMarks,
                    percentage = eval.percentage,
                    negativeDeducted = eval.negativeDeducted,
                    subjectScores = eval.subjectScores,
                    hindiScore = eval.hindiScore,
                    englishScore = eval.englishScore,
                    langPassed = eval.langPassed,
                    timeSpentSeconds = timeSpentSeconds,
                    questionTimesSec = questionTimesStringMap,
                    setType = resolvedSetType.name,
                    meritMarks = eval.meritMarks,
                    totalMeritMarks = eval.totalMeritMarks,
                    positiveMarksEarned = eval.positiveMarksEarned,
                    languageTotalMarks = eval.languageTotalMarks,
                    languageStatus = langStatusStr,
                    meritCorrectCount = eval.meritCorrectCount.toLong(),
                    meritIncorrectCount = eval.meritIncorrectCount.toLong(),
                    meritUnattemptedCount = (eval.meritDontKnowCount + eval.meritNotAttemptedCount).toLong(),
                    startedAtEpochMillis = startedAtEpochMillis,
                    completionStatus = completionStatus,
                    submittedAt = Timestamp.now()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, attemptPath)
            Result.failure(e)
        }
    }

    // ==================== SOLUTION REATTEMPTS & BOOKMARKS ====================

    fun observeSolutionReattempts(groupId: String, uid: String, isAdmin: Boolean): Flow<List<SolutionReattemptRecord>> {
        val base = db.collection("groups").document(groupId).collection("solutionReattempts")
            .whereEqualTo("groupId", groupId)
        val query = if (isAdmin) base else base.whereEqualTo("userId", uid)
        return query.snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toObject(SolutionReattemptRecord::class.java) } }
            .catch { err ->
                logStreamError(err, OperationType.LIST, "groups/$groupId/solutionReattempts", uid)
                emit(emptyList())
            }
    }

    suspend fun saveSolutionReattempt(
        groupId: String,
        testId: String,
        testTitle: String,
        aspirantName: String,
        reattemptAnswers: Map<String, String>,
        correctCount: Long,
        wrongCount: Long
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docId = "${testId}_$uid"
        val ref = db.collection("groups").document(groupId).collection("solutionReattempts").document(docId)
        return try {
            val payload = linkedMapOf<String, Any>(
                "groupId" to groupId,
                "testId" to testId,
                "testTitle" to testTitle.take(140),
                "userId" to uid,
                "aspirantName" to aspirantName.take(60),
                "reattemptAnswers" to reattemptAnswers,
                "correctCount" to correctCount.coerceIn(0L, 150L),
                "wrongCount" to wrongCount.coerceIn(0L, 150L),
                "totalReattempted" to reattemptAnswers.size.toLong().coerceIn(0L, 150L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    fun observeUserBookmarks(groupId: String, uid: String): Flow<List<BookmarkedQuestion>> {
        return db.collection("groups").document(groupId).collection("bookmarks")
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(BookmarkedQuestion::class.java) }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { err ->
                logStreamError(err, OperationType.LIST, "groups/$groupId/bookmarks", uid)
                emit(emptyList())
            }
    }

    suspend fun toggleBookmark(
        groupId: String,
        testId: String,
        testTitle: String,
        questionIndex: Int,
        question: ExamQuestion,
        currentlyBookmarked: Boolean
    ): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docId = "${uid}_${testId}_${questionIndex + 1}"
        val ref = db.collection("groups").document(groupId).collection("bookmarks").document(docId)
        return try {
            if (currentlyBookmarked) {
                ref.delete().await()
                Result.success(false)
            } else {
                val payload = linkedMapOf<String, Any>(
                    "groupId" to groupId,
                    "userId" to uid,
                    "testId" to testId,
                    "testTitle" to testTitle.take(140),
                    "questionIndex" to questionIndex.toLong().coerceIn(0L, 149L),
                    "questionNumber" to (questionIndex + 1).toLong().coerceIn(1L, 150L),
                    "subject" to question.subject.take(80),
                    "questionText" to question.text.take(2000).ifBlank { "Question ${questionIndex + 1}" },
                    "correctOption" to question.correctOption.take(4),
                    "explanation" to question.explanation.take(3000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    // ==================== CURRENT AFFAIRS ====================

    suspend fun publishCurrentAffair(
        groupId: String,
        title: String,
        summary: String,
        dateLabel: String,
        source: String,
        category: String,
        imageBase64: String?,
        publisherName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("groups").document(groupId).collection("currentAffairs").document()
        return try {
            val payload = linkedMapOf<String, Any?>(
                "groupId" to groupId,
                "title" to title.trim().take(160),
                "summary" to summary.trim().take(1500),
                "dateLabel" to dateLabel.trim().take(40),
                "source" to source.trim().take(120),
                "imageBase64" to imageBase64?.takeIf { it.length <= 520_000 },
                "category" to category.trim().take(50),
                "publishedByUid" to uid,
                "publishedByName" to publisherName,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun updateCurrentAffair(
        groupId: String,
        itemId: String,
        title: String,
        summary: String,
        dateLabel: String,
        source: String,
        category: String,
        imageBase64: String?
    ): Result<Unit> {
        val path = "groups/$groupId/currentAffairs/$itemId"
        return try {
            requireUserId()
            val updates = linkedMapOf<String, Any?>(
                "title" to title.trim().take(160),
                "summary" to summary.trim().take(1500),
                "dateLabel" to dateLabel.trim().take(40),
                "source" to source.trim().take(120),
                "category" to category.trim().take(50),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (imageBase64 != null && imageBase64.length <= 520_000) {
                updates["imageBase64"] = imageBase64
            }
            db.collection("groups").document(groupId).collection("currentAffairs").document(itemId)
                .update(updates.filterValues { it != null })
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCurrentAffair(groupId: String, itemId: String): Result<Unit> {
        val path = "groups/$groupId/currentAffairs/$itemId"
        return try {
            requireUserId()
            db.collection("groups").document(groupId).collection("currentAffairs").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ==================== INTERACTIVE PHOTO POLLS & IMAGE SOLUTIONS ====================

    suspend fun createQuestionPoll(
        groupId: String,
        creator: GroupMember,
        questionTitle: String,
        description: String,
        imageBase64: String?,
        options: List<String>,
        privateCorrectIndex: Int
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val pollRef = db.collection("groups").document(groupId).collection("polls").document()
        return try {
            val cleanOptions = options.map { it.trim() }.filter { it.isNotBlank() }.take(5)
            require(cleanOptions.size in 2..5) { "Provide 2 to 5 options for the poll." }
            val safeCorrectIdx = privateCorrectIndex.coerceIn(0, cleanOptions.lastIndex)

            val payload = linkedMapOf<String, Any?>(
                "groupId" to groupId,
                "creatorUid" to uid,
                "creatorName" to creator.displayName,
                "creatorPhotoBase64" to creator.photoBase64,
                "questionTitle" to questionTitle.trim().take(300),
                "description" to description.trim().take(1000).ifBlank { null },
                "imageBase64" to imageBase64?.takeIf { it.length <= 260000 },
                "options" to cleanOptions,
                "status" to "OPEN",
                "correctOptionIndex" to -1L, // Strictly hidden until finalized
                "voteCounts" to List(cleanOptions.size) { 0L },
                "totalVotes" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            pollRef.set(payload).await()

            // Store hidden private correct answer key readable ONLY by creator & admins
            pollRef.collection("privateKey").document("answer").set(
                mapOf(
                    "groupId" to groupId,
                    "pollId" to pollRef.id,
                    "creatorUid" to uid,
                    "correctOptionIndex" to safeCorrectIdx.toLong(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, pollRef.path)
            Result.failure(e)
        }
    }

    suspend fun getMyPollVote(groupId: String, pollId: String): Int? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snap = db.collection("groups").document(groupId)
                .collection("polls").document(pollId)
                .collection("votes").document(uid)
                .get().await()
            if (snap.exists()) (snap.getLong("selectedOptionIndex"))?.toInt() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getPrivatePollCorrectIndex(groupId: String, pollId: String): Int? {
        return try {
            val snap = db.collection("groups").document(groupId)
                .collection("polls").document(pollId)
                .collection("privateKey").document("answer")
                .get().await()
            if (snap.exists()) snap.getLong("correctOptionIndex")?.toInt() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun voteOnPoll(
        groupId: String,
        poll: QuestionPoll,
        voterName: String,
        selectedOptionIndex: Int
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val pollRef = db.collection("groups").document(groupId).collection("polls").document(poll.id)
        val voteRef = pollRef.collection("votes").document(uid)
        return try {
            db.runTransaction { tx ->
                val livePollSnap = tx.get(pollRef)
                val liveStatus = livePollSnap.getString("status") ?: "CLOSED"
                if (liveStatus != "OPEN") {
                    throw IllegalStateException("This poll is closed for voting.")
                }
                val existingVote = tx.get(voteRef)
                if (existingVote.exists()) {
                    throw IllegalStateException("You have already voted in this poll.")
                }
                @Suppress("UNCHECKED_CAST")
                val liveOptions = (livePollSnap.get("options") as? List<String>) ?: poll.options
                @Suppress("UNCHECKED_CAST")
                val rawCounts = (livePollSnap.get("voteCounts") as? List<Number>)?.map { it.toLong() } ?: poll.voteCounts
                val updatedCounts = rawCounts.toMutableList()
                while (updatedCounts.size < liveOptions.size) updatedCounts.add(0L)
                if (selectedOptionIndex !in updatedCounts.indices) {
                    throw IllegalArgumentException("Invalid poll option selected.")
                }
                updatedCounts[selectedOptionIndex] = updatedCounts[selectedOptionIndex] + 1L
                val liveTotalVotes = livePollSnap.getLong("totalVotes") ?: 0L

                tx.set(
                    voteRef,
                    mapOf(
                        "groupId" to groupId,
                        "pollId" to poll.id,
                        "userId" to uid,
                        "voterName" to voterName,
                        "selectedOptionIndex" to selectedOptionIndex.toLong(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    pollRef,
                    mapOf(
                        "voteCounts" to updatedCounts,
                        "totalVotes" to (liveTotalVotes + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                null
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, voteRef.path)
            Result.failure(e)
        }
    }

    suspend fun finalizePollAnswer(
        groupId: String,
        pollId: String,
        correctOptionIndex: Int,
        explanation: String,
        newStatus: String = "FINALIZED"
    ): Result<Unit> {
        val path = "groups/$groupId/polls/$pollId"
        return try {
            requireUserId()
            db.collection("groups").document(groupId).collection("polls").document(pollId)
                .update(
                    mapOf(
                        "status" to newStatus,
                        "correctOptionIndex" to correctOptionIndex.toLong(),
                        "finalExplanation" to explanation.trim().take(2000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun uploadPollSolution(
        groupId: String,
        poll: QuestionPoll,
        contributorName: String,
        solutionImageBase64: String?,
        textExplanation: String
    ): Result<Unit> {
        val path = "groups/$groupId/polls/${poll.id}"
        val pollRef = db.collection("groups").document(groupId).collection("polls").document(poll.id)
        return try {
            requireUserId()
            db.runTransaction { tx ->
                val liveSnap = tx.get(pollRef)
                @Suppress("UNCHECKED_CAST")
                val images = ((liveSnap.get("solutionImages") as? List<String>) ?: poll.solutionImages).toMutableList()
                if (!solutionImageBase64.isNullOrBlank() && images.size < 3) {
                    images.add(solutionImageBase64)
                }
                @Suppress("UNCHECKED_CAST")
                val liveContributors = (liveSnap.get("solutionContributors") as? List<String>) ?: poll.solutionContributors
                val contributors = (liveContributors + contributorName).distinct().take(10)
                val existingExp = liveSnap.getString("finalExplanation")
                val combinedExp = buildString {
                    if (!existingExp.isNullOrBlank()) append(existingExp).append("\n\n")
                    if (textExplanation.isNotBlank()) append("$contributorName: ${textExplanation.trim()}")
                }.take(2000)

                val updates = linkedMapOf<String, Any>(
                    "solutionImages" to images,
                    "solutionContributors" to contributors,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (combinedExp.isNotBlank()) {
                    updates["finalExplanation"] = combinedExp
                }
                tx.update(pollRef, updates)
                null
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ==================== DISCUSSIONS, REPORTS, ROLES & NOTIFICATIONS ====================

    suspend fun postDiscussionComment(
        groupId: String,
        targetType: String,
        targetId: String,
        author: GroupMember,
        message: String,
        commentType: String = "COMMENT",
        parentCommentId: String? = null,
        solutionImageBase64: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("groups").document(groupId).collection("discussions").document()
        return try {
            val cleanMsg = message.trim()
            require(cleanMsg.isNotEmpty() || !solutionImageBase64.isNullOrBlank()) {
                "Enter a message or attach a question/solution photo."
            }
            val finalMsg = cleanMsg.ifBlank {
                if (commentType == "SOLUTION") "Uploaded solution photo" else "Uploaded question photo"
            }
            val payload = linkedMapOf<String, Any?>(
                "groupId" to groupId,
                "targetType" to targetType,
                "targetId" to targetId,
                "authorUid" to uid,
                "authorName" to author.displayName,
                "authorPhotoBase64" to author.photoBase64,
                "message" to finalMsg.take(1500),
                "commentType" to commentType,
                "parentCommentId" to parentCommentId,
                "solutionImageBase64" to solutionImageBase64?.takeIf { it.length <= 260000 },
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun deletePoll(groupId: String, pollId: String): Result<Unit> {
        val path = "groups/$groupId/polls/$pollId"
        return try {
            requireUserId()
            db.collection("groups").document(groupId).collection("polls").document(pollId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun reportPoll(groupId: String, pollId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val pollRef = db.collection("groups").document(groupId).collection("polls").document(pollId)
        return try {
            db.runTransaction { tx ->
                val snap = tx.get(pollRef)
                @Suppress("UNCHECKED_CAST")
                val existingReporters = (snap.get("reportedByUids") as? List<String>).orEmpty()
                if (uid in existingReporters) {
                    throw IllegalStateException("You have already reported this poll.")
                }
                val currentCount = snap.getLong("reportCount") ?: 0L
                tx.update(
                    pollRef,
                    mapOf(
                        "reportCount" to (currentCount + 1L),
                        "reportedByUids" to (existingReporters + uid).distinct().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                null
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, pollRef.path)
            Result.failure(e)
        }
    }

    suspend fun editDiscussionComment(groupId: String, commentId: String, newMessage: String): Result<Unit> {
        val path = "groups/$groupId/discussions/$commentId"
        return try {
            requireUserId()
            val clean = newMessage.trim()
            require(clean.isNotEmpty()) { "Message cannot be empty." }
            db.collection("groups").document(groupId).collection("discussions").document(commentId)
                .update(
                    mapOf(
                        "message" to clean.take(1500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDiscussionComment(groupId: String, commentId: String): Result<Unit> {
        val path = "groups/$groupId/discussions/$commentId"
        return try {
            requireUserId()
            db.collection("groups").document(groupId).collection("discussions").document(commentId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun markDiscussionHelpful(groupId: String, commentId: String, helpful: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "groups/$groupId/discussions/$commentId"
        return try {
            db.collection("groups").document(groupId).collection("discussions").document(commentId)
                .update(
                    mapOf(
                        "isHelpful" to helpful,
                        "helpfulMarkedBy" to if (helpful) uid else "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveReportedIssue(
        groupId: String,
        commentId: String,
        resolved: Boolean,
        note: String = ""
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "groups/$groupId/discussions/$commentId"
        return try {
            db.collection("groups").document(groupId).collection("discussions").document(commentId)
                .update(
                    mapOf(
                        "resolved" to resolved,
                        "resolvedByUid" to if (resolved) uid else "",
                        "resolvedNote" to note.trim().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMemberRole(groupId: String, targetMemberUid: String, newRole: AppRole): Result<Unit> {
        val path = "groups/$groupId/members/$targetMemberUid"
        return try {
            requireUserId()
            db.collection("groups").document(groupId).collection("members").document(targetMemberUid)
                .update(
                    mapOf(
                        "role" to newRole.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendGroupNotification(
        groupId: String,
        title: String,
        body: String,
        type: String,
        senderName: String,
        targetAction: String? = null,
        dedupKey: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val notifCollection = db.collection("groups").document(groupId).collection("notifications")
        val ref = if (!dedupKey.isNullOrBlank()) {
            val safeId = dedupKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(100)
            notifCollection.document(safeId)
        } else {
            notifCollection.document()
        }
        return try {
            val payload = linkedMapOf<String, Any?>(
                "groupId" to groupId,
                "title" to title.trim().take(120),
                "body" to body.trim().take(1000),
                "type" to type,
                "senderUid" to uid,
                "senderName" to senderName,
                "targetAction" to targetAction?.take(60),
                "dedupKey" to dedupKey?.take(120),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    // ==================== APP UPDATE RELEASES (ADMIN ONLY WRITE) ====================

    suspend fun publishAppUpdate(
        groupId: String,
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
        uploaderName: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanUrl = downloadUrl.trim()
        if (!cleanUrl.startsWith("https://")) {
            return Result.failure(IllegalArgumentException("Download URL must start with https://"))
        }
        val docId = "rel_${versionCode}_${System.currentTimeMillis()}"
        val ref = db.collection("appUpdates").document(docId)
        return try {
            val payload = linkedMapOf<String, Any?>(
                "versionName" to versionName.trim().take(40),
                "versionCode" to versionCode.coerceAtLeast(1L),
                "title" to title.trim().take(120),
                "releaseNotes" to releaseNotes.trim().take(4000),
                "updateType" to if (updateType.equals("MANDATORY", true)) "MANDATORY" else "NORMAL",
                "minSupportedVersionCode" to minSupportedVersionCode.coerceAtLeast(1L),
                "downloadUrl" to cleanUrl.take(1000),
                "fileSizeBytes" to fileSizeBytes.coerceAtLeast(0L),
                "sha256Checksum" to sha256Checksum?.trim()?.take(128)?.ifBlank { null },
                "packageName" to packageName?.trim()?.take(120)?.ifBlank { null },
                "published" to true,
                "isActive" to true,
                "uploadedByUid" to uid,
                "uploadedByName" to uploaderName.trim().take(60),
                "groupId" to groupId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            ref.set(payload).await()

            // Clear isActive on prior group updates so older history items can be safely deleted later
            val priorUpdates = db.collection("appUpdates")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()
            val batch = db.batch()
            var hasPrior = false
            priorUpdates.documents.forEach { priorDoc ->
                if (priorDoc.id != docId && priorDoc.getBoolean("isActive") == true) {
                    batch.update(
                        priorDoc.reference,
                        mapOf("isActive" to false, "updatedAt" to FieldValue.serverTimestamp())
                    )
                    hasPrior = true
                }
            }
            if (hasPrior) batch.commit().await()

            // Send single deduplicated in-app APP_UPDATE notification for this versionCode
            sendGroupNotification(
                groupId = groupId,
                title = "New App Update Available (v${versionName.trim()})",
                body = "${title.trim()} — ${releaseNotes.trim().take(240)}",
                type = "APP_UPDATE",
                senderName = uploaderName,
                targetAction = "APP_UPDATE",
                dedupKey = "app_update_v$versionCode"
            )

            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun setAppUpdatePublished(updateId: String, published: Boolean): Result<Unit> {
        val path = "appUpdates/$updateId"
        return try {
            requireUserId()
            db.collection("appUpdates").document(updateId)
                .update(
                    mapOf(
                        "published" to published,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAppUpdate(updateId: String): Result<Unit> {
        val path = "appUpdates/$updateId"
        return try {
            requireUserId()
            val docRef = db.collection("appUpdates").document(updateId)
            val snap = docRef.get().await()
            if (snap.getBoolean("isActive") == true) {
                return Result.failure(IllegalStateException("Cannot delete the currently active live app update."))
            }
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ==================== LIVE EXAMS & LIVE EXAM SESSIONS ====================

    suspend fun createOrUpdateLiveExam(
        groupId: String,
        existingId: String? = null,
        sourceSetId: String,
        sourceSetTitle: String,
        title: String,
        description: String?,
        category: String,
        paperType: String,
        examDateIst: String?,
        startTimeIst: String?,
        startTimeEpochMillis: Long,
        durationMinutes: Int,
        registrationOpenEpochMillis: Long,
        registrationCloseEpochMillis: Long,
        resultPublishEpochMillis: Long,
        resultsPublishedManually: Boolean = false,
        totalQuestions: Int,
        totalMarks: Double,
        negativeMarkNumerator: Int,
        negativeMarkDenominator: Int,
        eligibilityType: String = "ALL_USERS",
        maxParticipants: Int = 1000,
        lateEntryAllowed: Boolean = true,
        lateEntryCutoffMinutes: Int = 15,
        secureModeEnabled: Boolean = true,
        dynamicWatermark: Boolean = true,
        focusLossDetection: Boolean = true,
        copyPasteBlock: Boolean = true,
        navigationExitLock: Boolean = true,
        warningLimit: Int = 3,
        status: String = "PUBLISHED",
        creatorName: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanTitle = title.trim()
        if (cleanTitle.length !in 3..160) {
            return Result.failure(IllegalArgumentException("Live Exam title must be 3–160 characters."))
        }
        if (sourceSetId.isBlank()) {
            return Result.failure(IllegalArgumentException("Select a valid Question Set to link with this Live Exam."))
        }
        val safeDuration = durationMinutes.coerceIn(1, 360)
        val safeStart = startTimeEpochMillis.coerceAtLeast(1_000_000L)
        val safeEnd = safeStart + (safeDuration * 60_000L)
        val safeRegOpen = registrationOpenEpochMillis.coerceAtLeast(0L)
        val safeRegClose = maxOf(registrationCloseEpochMillis, safeRegOpen)
        val safeResultPublish = maxOf(resultPublishEpochMillis, safeStart)
        val collection = db.collection("groups").document(groupId).collection("liveExams")
        val docRef = if (!existingId.isNullOrBlank()) {
            collection.document(existingId.trim())
        } else {
            collection.document("live_${sourceSetId.take(40)}_${System.currentTimeMillis()}")
        }
        val isUpdate = !existingId.isNullOrBlank()
        return try {
            val basePayload = linkedMapOf<String, Any?>(
                "groupId" to groupId,
                "sourceSetId" to sourceSetId.trim(),
                "sourceSetTitle" to sourceSetTitle.trim().ifBlank { cleanTitle }.take(160),
                "title" to cleanTitle,
                "description" to description?.trim()?.take(2000),
                "category" to category.trim().ifBlank { RankCategory.COMMON.name }.take(80),
                "paperType" to paperType.trim().ifBlank { PaperType.COMMON_GS.name },
                "examDateIst" to examDateIst?.trim()?.take(40),
                "startTimeIst" to startTimeIst?.trim()?.take(40),
                "startTimeEpochMillis" to safeStart,
                "durationMinutes" to safeDuration.toLong(),
                "endTimeEpochMillis" to safeEnd,
                "registrationOpenEpochMillis" to safeRegOpen,
                "registrationCloseEpochMillis" to safeRegClose,
                "resultPublishEpochMillis" to safeResultPublish,
                "resultsPublishedManually" to resultsPublishedManually,
                "totalQuestions" to totalQuestions.coerceIn(1, 200).toLong(),
                "totalMarks" to totalMarks.coerceIn(1.0, 1000.0),
                "negativeMarkNumerator" to negativeMarkNumerator.coerceIn(0, 10).toLong(),
                "negativeMarkDenominator" to negativeMarkDenominator.coerceIn(1, 10).toLong(),
                "eligibilityType" to if (eligibilityType.equals("REGISTERED_ONLY", true)) "REGISTERED_ONLY" else "ALL_USERS",
                "maxParticipants" to maxParticipants.coerceIn(1, 1000000).toLong(),
                "lateEntryAllowed" to lateEntryAllowed,
                "lateEntryCutoffMinutes" to lateEntryCutoffMinutes.coerceIn(0, 360).toLong(),
                "secureModeEnabled" to secureModeEnabled,
                "dynamicWatermark" to dynamicWatermark,
                "focusLossDetection" to focusLossDetection,
                "copyPasteBlock" to copyPasteBlock,
                "navigationExitLock" to navigationExitLock,
                "warningLimit" to warningLimit.coerceIn(1, 20).toLong(),
                "status" to status.uppercase().takeIf { it in listOf("DRAFT", "PUBLISHED", "CANCELLED", "COMPLETED") }
                    .orEmpty().ifBlank { "PUBLISHED" },
                "createdByUid" to uid,
                "createdByName" to creatorName.trim().ifBlank { "Admin" }.take(80),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isUpdate) {
                val snap = docRef.get().await()
                if (snap.exists()) {
                    basePayload.remove("createdByUid")
                    basePayload.remove("createdByName")
                    docRef.update(basePayload.filterValues { it != null }).await()
                } else {
                    basePayload["createdAt"] = FieldValue.serverTimestamp()
                    docRef.set(basePayload.filterValues { it != null }).await()
                }
            } else {
                basePayload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(basePayload.filterValues { it != null }).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Unlinks/removes a scheduled or completed Live Exam WITHOUT ever deleting the original Question Set in `tests/{sourceSetId}`.
     * Also cleans up any participant session documents associated with this Live Exam.
     */
    suspend fun removeLiveExamUnlinkOnly(groupId: String, liveExamId: String): Result<Unit> {
        val path = "groups/$groupId/liveExams/$liveExamId"
        val groupDoc = db.collection("groups").document(groupId)
        return try {
            requireUserId()
            groupDoc.collection("liveExams").document(liveExamId).delete().await()
            val sessionsSnap = groupDoc.collection("liveExamSessions")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("liveExamId", liveExamId)
                .get()
                .await()
            sessionsSnap.documents.chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    /**
     * Bulk-deletes multiple Live Exams (such as Completed & Past trial Live Exams) and their associated participant sessions.
     */
    suspend fun deleteLiveExamsByIds(groupId: String, liveExamIds: List<String>): Result<Int> {
        val path = "groups/$groupId/liveExams"
        val groupDoc = db.collection("groups").document(groupId)
        val cleanIds = liveExamIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleanIds.isEmpty()) return Result.success(0)
        return try {
            requireUserId()
            val idSet = cleanIds.toSet()
            cleanIds.chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { examId ->
                    batch.delete(groupDoc.collection("liveExams").document(examId))
                }
                batch.commit().await()
            }
            val sessionsSnap = groupDoc.collection("liveExamSessions")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()
            val matchingSessions = sessionsSnap.documents.filter { doc ->
                doc.getString("liveExamId") in idSet
            }
            matchingSessions.chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(cleanIds.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun setLiveExamStatus(groupId: String, liveExamId: String, newStatus: String): Result<Unit> {
        val path = "groups/$groupId/liveExams/$liveExamId"
        val validStatus = newStatus.uppercase().takeIf {
            it in listOf("DRAFT", "PUBLISHED", "CANCELLED", "COMPLETED")
        } ?: "DRAFT"
        return try {
            requireUserId()
            db.collection("groups").document(groupId).collection("liveExams").document(liveExamId)
                .update(
                    mapOf(
                        "status" to validStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun forceStartLiveExamNow(groupId: String, liveExam: LiveExam): Result<Unit> {
        val path = "groups/$groupId/liveExams/${liveExam.id}"
        val nowMillis = System.currentTimeMillis()
        val endMillis = nowMillis + (liveExam.durationMinutes.coerceAtLeast(5L) * 60_000L)
        return try {
            requireUserId()
            db.collection("groups").document(groupId).collection("liveExams").document(liveExam.id)
                .update(
                    mapOf(
                        "status" to "PUBLISHED",
                        "startTimeEpochMillis" to nowMillis,
                        "endTimeEpochMillis" to endMillis,
                        "registrationOpenEpochMillis" to minOf(liveExam.registrationOpenEpochMillis, nowMillis - 3_600_000L),
                        "registrationCloseEpochMillis" to maxOf(liveExam.registrationCloseEpochMillis, nowMillis),
                        "resultPublishEpochMillis" to maxOf(liveExam.resultPublishEpochMillis, endMillis),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun publishLiveExamResultsManually(groupId: String, liveExamId: String, published: Boolean = true): Result<Unit> {
        val path = "groups/$groupId/liveExams/$liveExamId"
        return try {
            requireUserId()
            db.collection("groups").document(groupId).collection("liveExams").document(liveExamId)
                .update(
                    mapOf(
                        "resultsPublishedManually" to published,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun registerOrUpdateLiveExamSession(
        groupId: String,
        liveExam: LiveExam,
        userName: String,
        userEmail: String?,
        newStatus: String = "REGISTERED",
        answeredCount: Int = 0,
        currentQuestionIndex: Int = 0,
        savedAnswers: Map<String, String> = emptyMap(),
        violationCount: Int = 0,
        violationLogs: List<String> = emptyList(),
        finalFiveMinAlertShown: Boolean = false,
        score: Double? = null,
        maxMarks: Double? = null
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val sessionId = LiveExamSession.sessionId(liveExam.id, uid)
        val docRef = db.collection("groups").document(groupId).collection("liveExamSessions").document(sessionId)
        val nowMillis = System.currentTimeMillis()
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = linkedMapOf<String, Any?>(
                    "groupId" to groupId,
                    "liveExamId" to liveExam.id,
                    "sourceSetId" to liveExam.sourceSetId,
                    "userId" to uid,
                    "userName" to userName.trim().ifBlank { "Aspirant" }.take(80),
                    "userEmail" to userEmail?.trim()?.take(120),
                    "status" to newStatus,
                    "answeredCount" to answeredCount.coerceIn(0, 200).toLong(),
                    "totalQuestions" to liveExam.totalQuestions.coerceIn(1L, 200L),
                    "currentQuestionIndex" to currentQuestionIndex.coerceIn(0, 200).toLong(),
                    "savedAnswers" to savedAnswers,
                    "violationCount" to violationCount.coerceIn(0, 100).toLong(),
                    "violationLogs" to violationLogs.take(100),
                    "finalFiveMinAlertShown" to finalFiveMinAlertShown,
                    "score" to score,
                    "maxMarks" to maxMarks,
                    "registeredAtEpochMillis" to nowMillis,
                    "startedAtEpochMillis" to if (newStatus == "ACTIVE") nowMillis else null,
                    "submittedAtEpochMillis" to if (newStatus == "SUBMITTED") nowMillis else null,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val existingStatus = snap.getString("status") ?: "REGISTERED"
                if (existingStatus == "DISQUALIFIED") {
                    return Result.failure(IllegalStateException("This Live Exam session has been disqualified by Admin."))
                }
                val finalStatus = when {
                    existingStatus == "FLAGGED" && newStatus == "ACTIVE" -> "FLAGGED"
                    else -> newStatus
                }
                val updates = linkedMapOf<String, Any?>(
                    "status" to finalStatus,
                    "answeredCount" to answeredCount.coerceIn(0, 200).toLong(),
                    "currentQuestionIndex" to currentQuestionIndex.coerceIn(0, 200).toLong(),
                    "savedAnswers" to savedAnswers,
                    "violationCount" to violationCount.coerceIn(0, 100).toLong(),
                    "violationLogs" to violationLogs.take(100),
                    "finalFiveMinAlertShown" to finalFiveMinAlertShown,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (score != null) updates["score"] = score
                if (maxMarks != null) updates["maxMarks"] = maxMarks
                if (newStatus == "ACTIVE" && snap.getLong("startedAtEpochMillis") == null) {
                    updates["startedAtEpochMillis"] = nowMillis
                }
                if (newStatus == "SUBMITTED") {
                    updates["submittedAtEpochMillis"] = nowMillis
                }
                docRef.update(updates.filterValues { it != null }).await()
            }
            Result.success(sessionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun moderateLiveExamSession(
        groupId: String,
        sessionId: String,
        decisionStatus: String, // "APPROVED" | "DISQUALIFIED"
        adminNote: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "groups/$groupId/liveExamSessions/$sessionId"
        val cleanStatus = if (decisionStatus.equals("DISQUALIFIED", ignoreCase = true)) {
            "DISQUALIFIED"
        } else {
            "APPROVED"
        }
        return try {
            db.collection("groups").document(groupId).collection("liveExamSessions").document(sessionId)
                .update(
                    mapOf(
                        "status" to cleanStatus,
                        "adminModerationNote" to adminNote.trim().take(500),
                        "moderatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ==================== DAILY TARGET SET BANNERS (AAJ KA TARGET) ====================

    fun observeDailyTargets(groupId: String): Flow<List<DailyTargetTask>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/dailyTargets"
        emitAll(
            db.collection("groups").document(groupId).collection("dailyTargets")
                .whereEqualTo("groupId", groupId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DailyTargetTask::class.java, estimate)
                        .sortedWith(
                            compareByDescending<DailyTargetTask> { it.targetDateIst }
                                .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                        )
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveDailyTarget(
        groupId: String,
        existingId: String?,
        test: ExamTest,
        targetDateIst: String,
        bannerTitle: String,
        bannerSubtitle: String?,
        bannerImageBase64: String?,
        publisherName: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanDate = targetDateIst.trim()
        if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(cleanDate)) {
            return Result.failure(IllegalArgumentException("Target date must be in YYYY-MM-DD (IST) format."))
        }
        val cleanBannerTitle = bannerTitle.trim().ifBlank { "आज का टारगेट • Aaj Ka Target" }.take(120)
        val cleanTestTitle = test.title.trim().ifBlank { "TRE 4.0 Target Set" }.take(160)
        val cleanSubtitle = bannerSubtitle?.trim()?.take(240)?.ifBlank { null }
        val cleanImage = bannerImageBase64?.takeIf { it.isNotBlank() && it.length <= 520_000 }
        val cleanPublisher = publisherName.trim().let { if (it.length < 2) "Admin" else it.take(60) }

        val colRef = db.collection("groups").document(groupId).collection("dailyTargets")
        val docRef = if (!existingId.isNullOrBlank()) colRef.document(existingId.trim()) else colRef.document()

        return try {
            if (existingId.isNullOrBlank()) {
                val payload = linkedMapOf<String, Any?>(
                    "groupId" to groupId,
                    "testId" to test.id,
                    "testTitle" to cleanTestTitle,
                    "targetDateIst" to cleanDate,
                    "bannerTitle" to cleanBannerTitle,
                    "bannerSubtitle" to cleanSubtitle,
                    "bannerImageBase64" to cleanImage,
                    "publishedByUid" to uid,
                    "publishedByName" to cleanPublisher,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updates = linkedMapOf<String, Any?>(
                    "testId" to test.id,
                    "testTitle" to cleanTestTitle,
                    "targetDateIst" to cleanDate,
                    "bannerTitle" to cleanBannerTitle,
                    "bannerSubtitle" to cleanSubtitle,
                    "bannerImageBase64" to cleanImage,
                    "publishedByUid" to uid,
                    "publishedByName" to cleanPublisher,
                    "active" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteDailyTarget(groupId: String, targetId: String): Result<Unit> {
        val path = "groups/$groupId/dailyTargets/$targetId"
        return try {
            db.collection("groups").document(groupId)
                .collection("dailyTargets").document(targetId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..5).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "TRE4$suffix"
    }

    private fun logStreamError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }
}
