package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppRole
import com.example.data.model.ContentViewRecord
import com.example.data.model.DiscussionComment
import com.example.data.model.ExamQuestion
import com.example.data.model.GroupMember
import com.example.data.model.LanguagePref
import com.example.data.model.QuestionPoll
import com.example.data.model.UserProfile
import com.example.ui.glass.CoolingGlassAvatar
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassBottomSheet
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassConfirmationPopup
import com.example.ui.glass.CoolingGlassDialog
import com.example.ui.glass.CoolingGlassEmptyState
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.CoolingGlassProgressIndicator
import com.example.ui.glass.CoolingGlassTabRow
import com.example.ui.glass.CoolingGlassTextField
import com.example.ui.glass.ProfilePhotoZoomDialog
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.GlacierBlue600
import com.example.ui.theme.IceCyan500
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.SoftLavender500
import com.example.ui.theme.appText
import com.example.util.ImageCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PollsAndDiscussionsScreen(
    currentMember: GroupMember?,
    polls: List<QuestionPoll>,
    userPollVotes: Map<String, Int>,
    discussions: List<DiscussionComment>,
    contentViews: List<ContentViewRecord> = emptyList(),
    adminUserProfiles: List<UserProfile> = emptyList(),
    initialTab: Int = 0, // 0 = Interactive Photo Polls, 1 = Discussions & Solutions
    onTrackPollView: (String) -> Unit = {},
    onLoadVote: (String) -> Unit,
    onCreatePoll: (title: String, desc: String, imgBase64: String?, options: List<String>, correctIdx: Int) -> Unit,
    onCastVote: (QuestionPoll, Int) -> Unit,
    onFetchPrivateKey: (pollId: String, onLoaded: (Int?) -> Unit) -> Unit,
    onFinalizePoll: (pollId: String, correctIdx: Int, explanation: String, status: String) -> Unit,
    onUploadSolution: (QuestionPoll, String?, String) -> Unit,
    onDeletePoll: (pollId: String) -> Unit = {},
    onReportPoll: (pollId: String) -> Unit = {},
    onPostComment: (targetType: String, targetId: String, message: String, commentType: String, parentId: String?, imageBase64: String?) -> Unit,
    onEditComment: (commentId: String, newMessage: String) -> Unit = { _, _ -> },
    onDeleteComment: (commentId: String) -> Unit = {},
    onMarkHelpful: (commentId: String, helpful: Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalCoolingGlass.current
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var showCreatePollSheet by remember { mutableStateOf(false) }
    var finalizePollTarget by remember { mutableStateOf<QuestionPoll?>(null) }
    var uploadSolutionTarget by remember { mutableStateOf<QuestionPoll?>(null) }
    var deletePollTarget by remember { mutableStateOf<QuestionPoll?>(null) }
    var editingCommentTarget by remember { mutableStateOf<DiscussionComment?>(null) }
    var deleteCommentTarget by remember { mutableStateOf<DiscussionComment?>(null) }
    var zoomedImageBase64 by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pollViewersTarget by remember { mutableStateOf<QuestionPoll?>(null) }
    var zoomedAvatar by remember { mutableStateOf<Pair<String, String?>?>(null) }
    var discussionFilter by remember { mutableIntStateOf(0) }
    var newCommentText by remember { mutableStateOf("") }
    var doubtPhotoBase64 by remember { mutableStateOf<String?>(null) }
    var replyParent by remember { mutableStateOf<DiscussionComment?>(null) }

    val doubtGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                doubtPhotoBase64 = withContext(Dispatchers.IO) {
                    ImageCodec.encodeUriToBase64(context, uri, maxDimension = 680, quality = 65)
                }
            }
        }
    }
    val doubtCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            scope.launch {
                doubtPhotoBase64 = withContext(Dispatchers.IO) {
                    ImageCodec.encodeBitmapToBase64(bmp, maxDimension = 680, quality = 65)
                }
            }
        }
    }

    val isAdmin = currentMember?.appRole() in listOf(AppRole.GROUP_ADMIN, AppRole.SUPER_ADMIN)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("polls_discussions_list")
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CoolingGlassTabRow(
                tabs = listOf("5-Opt Polls (${polls.size})", "Doubt Session (${discussions.size})"),
                selectedIndex = activeTab,
                onSelect = { activeTab = it }
            )
        }

        if (activeTab == 0) {
            // Prominent CREATE POLL / सवाल पूछें CTA
            item {
                CoolingGlassCard(
                    strongFrost = true,
                    tintColor = SoftLavender500,
                    borderColor = glass.accentCyan.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Interactive Photo Question Polls",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Upload any book, handwritten, or screenshot question photo. Your correct answer key stays private until you finalize the poll.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    CoolingGlassButton(
                        text = appText("CREATE POLL", "CREATE POLL / सवाल पूछें"),
                        icon = Icons.Rounded.Poll,
                        testTagName = "create_poll_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showCreatePollSheet = true }
                    )
                }
            }

            if (polls.isEmpty()) {
                item {
                    CoolingGlassEmptyState(
                        title = "No Photo Polls Yet",
                        subtitle = "Be the first User to post a 5-option photo question poll for the group!",
                        icon = Icons.Rounded.HowToVote,
                        actionLabel = appText("CREATE POLL", "CREATE POLL / सवाल पूछें"),
                        onAction = { showCreatePollSheet = true }
                    )
                }
            } else {
                items(polls, key = { it.id }) { poll ->
                    LaunchedEffect(poll.id) {
                        onLoadVote(poll.id)
                        onTrackPollView(poll.id)
                    }
                    val myVote = userPollVotes[poll.id]
                    val canManage = poll.creatorUid == currentMember?.userId || isAdmin
                    val alreadyReported = currentMember?.userId != null && currentMember.userId in poll.reportedByUids
                    val pollViewCount = remember(contentViews, poll.id) {
                        contentViews.count {
                            it.contentType == ContentViewRecord.CONTENT_TYPE_POLL && it.contentId == poll.id
                        }
                    }
                    CoolingGlassPollCard(
                        poll = poll,
                        myVoteIndex = myVote,
                        canManage = canManage,
                        isAdmin = isAdmin,
                        isCreator = poll.creatorUid == currentMember?.userId,
                        alreadyReported = alreadyReported,
                        viewCount = pollViewCount,
                        onOpenViewers = { pollViewersTarget = poll },
                        onAvatarClick = { zoomedAvatar = poll.creatorName to poll.creatorPhotoBase64 },
                        onVote = { idx -> onCastVote(poll, idx) },
                        onZoomImage = { title, b64 -> zoomedImageBase64 = title to b64 },
                        onOpenFinalize = { finalizePollTarget = poll },
                        onClosePoll = { onFinalizePoll(poll.id, -1, poll.finalExplanation.orEmpty(), "CLOSED") },
                        onOpenUploadSolution = { uploadSolutionTarget = poll },
                        onDeletePoll = { deletePollTarget = poll },
                        onReportPoll = { onReportPoll(poll.id) }
                    )
                }
            }
        } else {
            // ================= DOUBT SESSION & PHOTO SOLUTIONS =================
            item {
                CoolingGlassCard(strongFrost = true) {
                    val activeReplyParent = replyParent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeReplyParent != null) "Solve / Reply to ${activeReplyParent.authorName}" else "Doubt Session (Photo & Screenshot)",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (activeReplyParent == null) {
                            CoolingGlassOutlinedButton(
                                text = "5-Opt Poll",
                                icon = Icons.Rounded.Poll,
                                onClick = { showCreatePollSheet = true }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (activeReplyParent != null) "Upload a handwritten or screenshot solution photo and explanation for this doubt."
                        else "Upload a question photo/screenshot to ask a doubt — anyone solving can attach their solution photo!",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary
                    )
                    if (activeReplyParent != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeReplyParent.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            CoolingGlassOutlinedButton(
                                text = "Cancel",
                                icon = Icons.Rounded.Close,
                                onClick = { replyParent = null }
                            )
                        }
                    }

                    val attachedPreview = remember(doubtPhotoBase64) { ImageCodec.decodeToImageBitmap(doubtPhotoBase64) }
                    if (attachedPreview != null) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            Image(
                                bitmap = attachedPreview,
                                contentDescription = if (replyParent != null) "Solution photo preview" else "Question doubt photo preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            CoolingGlassOutlinedButton(
                                text = "Remove",
                                icon = Icons.Rounded.Close,
                                tint = glass.incorrect,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp),
                                onClick = { doubtPhotoBase64 = null }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoolingGlassOutlinedButton(
                            text = if (replyParent != null) "Solution Screenshot" else "Question Screenshot",
                            icon = Icons.Rounded.PhotoLibrary,
                            testTagName = "doubt_upload_gallery_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { doubtGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        )
                        CoolingGlassOutlinedButton(
                            text = "Camera",
                            icon = Icons.Rounded.CameraAlt,
                            testTagName = "doubt_upload_camera_btn",
                            modifier = Modifier.weight(0.75f),
                            onClick = { doubtCameraLauncher.launch(null) }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    CoolingGlassTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        label = if (replyParent != null) "Write solution steps / explanation..." else "Describe your doubt or attach a question photo...",
                        singleLine = false,
                        minLines = 2,
                        testTagName = "discussion_input"
                    )
                    Spacer(Modifier.height(8.dp))
                    CoolingGlassButton(
                        text = when {
                            replyParent != null && doubtPhotoBase64 != null -> "Post Solution Photo & Reply"
                            replyParent != null -> "Post Solution / Reply"
                            doubtPhotoBase64 != null -> "Post Doubt with Photo"
                            else -> "Post Doubt to Group"
                        },
                        icon = Icons.AutoMirrored.Rounded.Send,
                        enabled = newCommentText.trim().length >= 2 || doubtPhotoBase64 != null,
                        testTagName = "post_discussion_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val targetType = replyParent?.targetType ?: "GENERAL"
                            val finalMsg = newCommentText.trim().ifBlank {
                                if (replyParent != null) "Uploaded solution screenshot/photo." else "Uploaded question screenshot/photo for doubt solving."
                            }
                            val finalType = when {
                                replyParent != null && doubtPhotoBase64 != null -> "SOLUTION"
                                replyParent != null -> "REPLY"
                                else -> "COMMENT"
                            }
                            onPostComment(
                                targetType,
                                replyParent?.targetId ?: "group",
                                finalMsg,
                                finalType,
                                replyParent?.id,
                                doubtPhotoBase64
                            )
                            newCommentText = ""
                            doubtPhotoBase64 = null
                            replyParent = null
                        }
                    )
                }
            }

            item {
                CoolingGlassTabRow(
                    tabs = listOf("All", "Solutions", "Reports"),
                    selectedIndex = discussionFilter,
                    onSelect = { discussionFilter = it }
                )
            }

            val filteredDiscussions = discussions.filter { comment ->
                when (discussionFilter) {
                    1 -> comment.commentType == "SOLUTION" || !comment.solutionImageBase64.isNullOrBlank()
                    2 -> comment.commentType.startsWith("REPORT") || comment.commentType == "CORRECTION_AUDIT"
                    else -> true
                }
            }

            if (filteredDiscussions.isEmpty()) {
                item {
                    CoolingGlassEmptyState(
                        title = "No Posts Here Yet",
                        subtitle = "Start a discussion, report a question error, or upload a handwritten solution.",
                        icon = Icons.Rounded.Forum
                    )
                }
            } else {
                val commentsById = discussions.associateBy { it.id }
                items(filteredDiscussions, key = { it.id }) { comment ->
                    val badgeColor = when {
                        comment.isHelpful -> glass.correct
                        comment.commentType == "SOLUTION" -> glass.correct
                        comment.commentType.startsWith("REPORT") -> glass.incorrect
                        comment.commentType == "CORRECTION_AUDIT" -> glass.review
                        else -> glass.accentPrimary
                    }
                    val isAuthor = comment.authorUid == currentMember?.userId
                    val parentComment = comment.parentCommentId?.let { commentsById[it] }
                    val canMarkHelpful = comment.parentCommentId != null &&
                        (isAdmin || parentComment?.authorUid == currentMember?.userId)

                    CoolingGlassCard(
                        borderColor = if (comment.isHelpful) glass.correct.copy(alpha = 0.65f) else null
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoolingGlassAvatar(
                                name = comment.authorName,
                                photoBase64 = comment.authorPhotoBase64,
                                size = 38.dp,
                                onClick = { zoomedAvatar = comment.authorName to comment.authorPhotoBase64 }
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(comment.authorName, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (comment.updatedAt != null) "${comment.targetType} • Edited" else comment.targetType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textMuted
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (comment.isHelpful) {
                                    CoolingGlassBadge(text = "HELPFUL", color = glass.correct)
                                }
                                CoolingGlassBadge(text = comment.commentType.replace("_", " "), color = badgeColor)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(comment.message, style = MaterialTheme.typography.bodyMedium, color = glass.textPrimary)
                        val solImg = remember(comment.solutionImageBase64) { ImageCodec.decodeToImageBitmap(comment.solutionImageBase64) }
                        if (solImg != null && comment.solutionImageBase64 != null) {
                            Spacer(Modifier.height(8.dp))
                            Image(
                                bitmap = solImg,
                                contentDescription = if (comment.commentType == "SOLUTION") "Uploaded solution photo" else "Uploaded doubt photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        val label = if (comment.commentType == "SOLUTION") "Solution by ${comment.authorName}" else "Doubt by ${comment.authorName}"
                                        zoomedImageBase64 = label to comment.solutionImageBase64
                                    }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CoolingGlassOutlinedButton(
                                text = "Solve / Reply",
                                onClick = { replyParent = comment }
                            )
                            if (canMarkHelpful) {
                                CoolingGlassOutlinedButton(
                                    text = if (comment.isHelpful) "Unmark Helpful" else "Mark Helpful",
                                    icon = Icons.Rounded.ThumbUp,
                                    tint = glass.correct,
                                    onClick = { onMarkHelpful(comment.id, !comment.isHelpful) }
                                )
                            }
                            if (isAuthor) {
                                CoolingGlassOutlinedButton(
                                    text = "Edit",
                                    icon = Icons.Rounded.Edit,
                                    onClick = { editingCommentTarget = comment }
                                )
                            }
                            if (isAuthor || isAdmin) {
                                CoolingGlassOutlinedButton(
                                    text = "Delete",
                                    icon = Icons.Rounded.DeleteOutline,
                                    tint = glass.incorrect,
                                    onClick = { deleteCommentTarget = comment }
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }

    // Create Poll Sheet
    if (showCreatePollSheet) {
        CreatePhotoPollSheet(
            onDismiss = { showCreatePollSheet = false },
            onPublish = { title, desc, img, opts, correctIdx ->
                onCreatePoll(title, desc, img, opts, correctIdx)
                showCreatePollSheet = false
            }
        )
    }

    // Finalize Poll Answer Dialog
    finalizePollTarget?.let { target ->
        FinalizePollDialog(
            poll = target,
            onFetchPrivateKey = onFetchPrivateKey,
            onDismiss = { finalizePollTarget = null },
            onFinalize = { idx, exp ->
                onFinalizePoll(target.id, idx, exp, "FINALIZED")
                finalizePollTarget = null
            }
        )
    }

    // Upload Handwritten Solution Sheet
    uploadSolutionTarget?.let { target ->
        UploadSolutionSheet(
            pollTitle = target.questionTitle,
            onDismiss = { uploadSolutionTarget = null },
            onSubmit = { imgBase64, text ->
                onUploadSolution(target, imgBase64, text)
                uploadSolutionTarget = null
            }
        )
    }

    // Full-screen Pinch-Zoom Image Dialog
    zoomedImageBase64?.let { (title, b64) ->
        var scale by remember { mutableFloatStateOf(1f) }
        val bmp = remember(b64) { ImageCodec.decodeToImageBitmap(b64) }
        CoolingGlassDialog(title = title, onDismiss = { zoomedImageBase64 = null }) {
            if (bmp != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoomChange, _ ->
                                scale = (scale * zoomChange).coerceIn(1f, 4f)
                            }
                        }
                ) {
                    Image(
                        bitmap = bmp,
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Pinch with two fingers to zoom in/out",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textMuted
                )
            }
        }
    }

    // Confirm Delete Poll
    deletePollTarget?.let { target ->
        CoolingGlassConfirmationPopup(
            title = "Delete Photo Poll?",
            message = "Permanently remove '${target.questionTitle}' and its votes?",
            confirmText = "Delete Poll",
            isDestructive = true,
            onConfirm = {
                onDeletePoll(target.id)
                deletePollTarget = null
            },
            onDismiss = { deletePollTarget = null }
        )
    }

    // Edit Own Doubt / Reply Dialog
    editingCommentTarget?.let { target ->
        var editedText by remember(target.id) { mutableStateOf(target.message) }
        CoolingGlassDialog(title = "Edit Your Post", onDismiss = { editingCommentTarget = null }) {
            CoolingGlassTextField(
                value = editedText,
                onValueChange = { editedText = it },
                label = "Updated Message",
                singleLine = false,
                minLines = 3
            )
            Spacer(Modifier.height(12.dp))
            CoolingGlassButton(
                text = "Save Changes",
                icon = Icons.Rounded.CheckCircle,
                enabled = editedText.trim().length >= 2,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onEditComment(target.id, editedText.trim())
                    editingCommentTarget = null
                }
            )
        }
    }

    // Confirm Delete Doubt / Reply
    deleteCommentTarget?.let { target ->
        CoolingGlassConfirmationPopup(
            title = "Delete Post?",
            message = "Remove this doubt/reply permanently?",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                onDeleteComment(target.id)
                deleteCommentTarget = null
            },
            onDismiss = { deleteCommentTarget = null }
        )
    }

    // Admin-Only Unique Poll Viewers Modal
    if (isAdmin) {
        pollViewersTarget?.let { targetPoll ->
            val pollViews = remember(contentViews, targetPoll.id) {
                contentViews.filter {
                    it.contentType == ContentViewRecord.CONTENT_TYPE_POLL && it.contentId == targetPoll.id
                }
            }
            ContentViewersDialog(
                title = targetPoll.questionTitle,
                contentTypeLabel = "Poll Viewers",
                views = pollViews,
                adminUserProfiles = adminUserProfiles,
                onDismiss = { pollViewersTarget = null }
            )
        }
    }

    // Clickable Large Profile Photo Zoom
    zoomedAvatar?.let { (aName, aPhoto) ->
        ProfilePhotoZoomDialog(
            name = aName,
            photoBase64 = aPhoto,
            onDismiss = { zoomedAvatar = null }
        )
    }
}

@Composable
private fun CoolingGlassPollCard(
    poll: QuestionPoll,
    myVoteIndex: Int?,
    canManage: Boolean,
    isAdmin: Boolean,
    isCreator: Boolean,
    alreadyReported: Boolean,
    viewCount: Int = 0,
    onOpenViewers: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onVote: (Int) -> Unit,
    onZoomImage: (String, String) -> Unit,
    onOpenFinalize: () -> Unit,
    onClosePoll: () -> Unit,
    onOpenUploadSolution: () -> Unit,
    onDeletePoll: () -> Unit,
    onReportPoll: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    val isFinalized = poll.status == "FINALIZED"
    val isOpen = poll.status == "OPEN"
    val showPercentages = myVoteIndex != null || !isOpen
    val decodedQuestionImg = remember(poll.imageBase64) { ImageCodec.decodeToImageBitmap(poll.imageBase64) }
    val statusColor = when (poll.status) {
        "FINALIZED" -> glass.correct
        "CLOSED" -> glass.review
        else -> glass.accentCyan
    }

    CoolingGlassCard(strongFrost = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoolingGlassAvatar(
                name = poll.creatorName,
                photoBase64 = poll.creatorPhotoBase64,
                size = 40.dp,
                onClick = onAvatarClick
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(poll.creatorName, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                Text("${poll.totalVotes} votes • ${poll.solutionImages.size} solutions", style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
            }
            CoolingGlassBadge(text = poll.status, color = statusColor)
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = poll.questionTitle,
            style = MaterialTheme.typography.titleMedium,
            color = glass.textPrimary,
            fontWeight = FontWeight.Bold
        )
        if (!poll.description.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(poll.description, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
        }

        if (decodedQuestionImg != null && poll.imageBase64 != null) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, glass.glassBorder, RoundedCornerShape(16.dp))
                    .clickable { onZoomImage(poll.questionTitle, poll.imageBase64) }
            ) {
                Image(
                    bitmap = decodedQuestionImg,
                    contentDescription = "Poll question photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xB30B1F3A))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Tap to Zoom", style = MaterialTheme.typography.labelSmall, color = CrystalWhite)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Poll Options A–E
        val keys = listOf("A", "B", "C", "D", "E")
        val total = poll.totalVotes.coerceAtLeast(1L)
        poll.options.forEachIndexed { idx, optText ->
            val count = poll.voteCounts.getOrNull(idx) ?: 0L
            val pct = if (poll.totalVotes > 0L) (count.toFloat() / total.toFloat()) else 0f
            val isMyChoice = myVoteIndex == idx
            val isCorrect = isFinalized && poll.correctOptionIndex.toInt() == idx
            val isWrongMyChoice = isFinalized && isMyChoice && poll.correctOptionIndex >= 0L && !isCorrect

            val accent = when {
                isCorrect -> glass.correct
                isWrongMyChoice -> glass.incorrect
                isMyChoice -> glass.accentCyan
                else -> glass.accentPrimary
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(glass.glassSurfaceStrong)
                    .background(
                        when {
                            isCorrect -> glass.correctBg
                            isWrongMyChoice -> glass.incorrectBg
                            isMyChoice -> glass.accentCyan.copy(alpha = 0.14f)
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        width = if (isCorrect || isMyChoice) 1.6.dp else 1.dp,
                        color = if (isCorrect || isMyChoice || isWrongMyChoice) accent else glass.glassBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = isOpen && myVoteIndex == null) { onVote(idx) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${keys.getOrElse(idx) { "${idx + 1}" }}. ",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                    val displayedOptText = if (LocalAppLanguage.current == LanguagePref.ENGLISH && optText == ExamQuestion.DEFAULT_OPTION_E) {
                        "Not Attempted"
                    } else {
                        optText
                    }
                    Text(
                        text = displayedOptText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textPrimary,
                        fontWeight = if (isCorrect || isMyChoice) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (isCorrect) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Correct option", tint = glass.correct, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    if (showPercentages) {
                        Text(
                            text = "${(pct * 100).toInt()}% ($count)",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (showPercentages) {
                    Spacer(Modifier.height(6.dp))
                    CoolingGlassProgressIndicator(
                        progress = pct,
                        height = 6.dp,
                        accentColors = listOf(accent, accent.copy(alpha = 0.7f))
                    )
                }
            }
        }

        // Finalized Explanation Banner
        if (isFinalized && !poll.finalExplanation.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            CoolingGlassCard(
                tintColor = glass.correct,
                borderColor = glass.correct.copy(alpha = 0.55f),
                contentPadding = PaddingValues(10.dp)
            ) {
                Text("Official Explanation", style = MaterialTheme.typography.labelMedium, color = glass.correct, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(poll.finalExplanation, style = MaterialTheme.typography.bodySmall, color = glass.textPrimary)
            }
        }

        // Handwritten Solutions Gallery
        if (poll.solutionImages.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Uploaded Handwritten Solutions (${poll.solutionImages.size})",
                style = MaterialTheme.typography.labelMedium,
                color = glass.accentPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                poll.solutionImages.forEachIndexed { i, solB64 ->
                    val solBmp = remember(solB64) { ImageCodec.decodeToImageBitmap(solB64) }
                    val contributor = poll.solutionContributors.getOrNull(i) ?: "Aspirant"
                    if (solBmp != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                bitmap = solBmp,
                                contentDescription = "Solution by $contributor",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, glass.glassBorder, RoundedCornerShape(14.dp))
                                    .clickable { onZoomImage("Solution by $contributor", solB64) }
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = contributor,
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Action Row: Upload Solution + Creator/Admin Finalize Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CoolingGlassOutlinedButton(
                text = "UPLOAD SOLUTION",
                icon = Icons.Rounded.UploadFile,
                modifier = Modifier.weight(1f),
                onClick = onOpenUploadSolution
            )
            if (canManage && !isFinalized) {
                CoolingGlassButton(
                    text = "Finalize Key",
                    icon = Icons.Rounded.CheckCircle,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenFinalize
                )
            }
        }
        if (canManage && isOpen) {
            Spacer(Modifier.height(6.dp))
            CoolingGlassOutlinedButton(
                text = "Close Voting Without Publishing Key",
                tint = glass.review,
                modifier = Modifier.fillMaxWidth(),
                onClick = onClosePoll
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isCreator) {
                CoolingGlassOutlinedButton(
                    text = if (alreadyReported) "Reported" else "Report Spam",
                    icon = Icons.Rounded.Flag,
                    tint = glass.review,
                    enabled = !alreadyReported,
                    modifier = Modifier.weight(1f),
                    onClick = onReportPoll
                )
            }
            if (isAdmin) {
                CoolingGlassOutlinedButton(
                    text = "Views ($viewCount)",
                    icon = Icons.Rounded.Visibility,
                    tint = glass.accentCyan,
                    testTagName = "poll_viewers_btn_${poll.id}",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenViewers
                )
            }
            if (isAdmin && poll.reportCount > 0L) {
                CoolingGlassBadge(text = "${poll.reportCount} Reports", color = glass.incorrect)
            }
            if (canManage) {
                CoolingGlassOutlinedButton(
                    text = "Delete Poll",
                    icon = Icons.Rounded.DeleteOutline,
                    tint = glass.incorrect,
                    modifier = Modifier.weight(1f),
                    onClick = onDeletePoll
                )
            }
        }
    }
}

@Composable
private fun CreatePhotoPollSheet(
    onDismiss: () -> Unit,
    onPublish: (title: String, desc: String, imgBase64: String?, options: List<String>, correctIdx: Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalCoolingGlass.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    val appLanguage = LocalAppLanguage.current
    val defaultOptE = if (appLanguage == LanguagePref.ENGLISH) "Not Attempted" else ExamQuestion.DEFAULT_OPTION_E
    val options = remember(appLanguage) {
        mutableStateListOf("Option A", "Option B", "Option C", "Option D", defaultOptE)
    }
    var privateCorrectIdx by remember { mutableIntStateOf(0) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                photoBase64 = withContext(Dispatchers.IO) {
                    ImageCodec.encodeUriToBase64(context, uri, maxDimension = 680, quality = 65)
                }
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            scope.launch {
                photoBase64 = withContext(Dispatchers.IO) {
                    ImageCodec.encodeBitmapToBase64(bmp, maxDimension = 680, quality = 65)
                }
            }
        }
    }

    CoolingGlassBottomSheet(title = appText("CREATE POLL", "CREATE POLL / सवाल पूछें"), onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Question Photo / Screenshot Controls
            val preview = remember(photoBase64) { ImageCodec.decodeToImageBitmap(photoBase64) }
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = "Poll question preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoolingGlassOutlinedButton(
                    text = "Gallery / Screenshot",
                    icon = Icons.Rounded.PhotoLibrary,
                    modifier = Modifier.weight(1.2f),
                    onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                )
                CoolingGlassOutlinedButton(
                    text = "Camera",
                    icon = Icons.Rounded.CameraAlt,
                    modifier = Modifier.weight(0.85f),
                    onClick = { cameraLauncher.launch(null) }
                )
                if (photoBase64 != null) {
                    CoolingGlassOutlinedButton(
                        text = "Rotate",
                        icon = Icons.AutoMirrored.Rounded.RotateRight,
                        onClick = {
                            val currentImg = photoBase64 ?: return@CoolingGlassOutlinedButton
                            scope.launch {
                                photoBase64 = withContext(Dispatchers.IO) {
                                    ImageCodec.rotateBase64(currentImg, 90f)
                                }
                            }
                        }
                    )
                }
            }

            CoolingGlassTextField(
                value = title,
                onValueChange = { title = it },
                label = appText("Question Title *", "Question Title / प्रश्न शीर्षक *"),
                placeholder = "e.g., Solve this BPSC Maths / Science question"
            )
            CoolingGlassTextField(
                value = description,
                onValueChange = { description = it },
                label = "Extra Context / Note (Optional)",
                singleLine = false,
                minLines = 2
            )

            Text(
                text = "Same 5 Exam Options (A–D + Option E: Not Attempted)",
                style = MaterialTheme.typography.labelLarge,
                color = glass.textPrimary,
                fontWeight = FontWeight.Bold
            )
            val labels = listOf("Option A", "Option B", "Option C", "Option D", "Option E (Not Attempted)")
            options.forEachIndexed { idx, value ->
                CoolingGlassTextField(
                    value = value,
                    onValueChange = { options[idx] = it },
                    label = labels[idx]
                )
            }

            Text(
                text = "Private Correct Answer (Hidden until you Finalize)",
                style = MaterialTheme.typography.labelMedium,
                color = glass.accentPrimary,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("A", "B", "C", "D").forEachIndexed { idx, key ->
                    val picked = privateCorrectIdx == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (picked) GlacierBlue600 else glass.glassSurface)
                            .border(1.dp, if (picked) IceCyan500 else glass.glassBorder, RoundedCornerShape(12.dp))
                            .clickable { privateCorrectIdx = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(key, color = if (picked) CrystalWhite else glass.textPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            CoolingGlassButton(
                text = "Publish Photo Poll",
                icon = Icons.Rounded.Poll,
                enabled = title.trim().length >= 3 && options.all { it.isNotBlank() },
                modifier = Modifier.fillMaxWidth(),
                onClick = { onPublish(title.trim(), description.trim(), photoBase64, options.map { it.trim() }, privateCorrectIdx) }
            )
        }
    }
}

@Composable
private fun FinalizePollDialog(
    poll: QuestionPoll,
    onFetchPrivateKey: (pollId: String, onLoaded: (Int?) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onFinalize: (correctIdx: Int, explanation: String) -> Unit
) {
    val glass = LocalCoolingGlass.current
    var selectedCorrect by remember { mutableIntStateOf(0) }
    var explanation by remember { mutableStateOf("") }

    LaunchedEffect(poll.id) {
        onFetchPrivateKey(poll.id) { saved ->
            if (saved != null && saved in poll.options.indices) {
                selectedCorrect = saved
            }
        }
    }

    CoolingGlassDialog(title = "Finalize & Publish Answer", onDismiss = onDismiss) {
        Text(
            text = "Confirm the official correct option for '${poll.questionTitle}'. Once published, all aspirants will see the correct answer and explanation.",
            style = MaterialTheme.typography.bodySmall,
            color = glass.textSecondary
        )
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val keys = listOf("A", "B", "C", "D", "E")
            poll.options.indices.forEach { idx ->
                val picked = selectedCorrect == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (picked) glass.correct else glass.glassSurface)
                        .border(1.dp, if (picked) glass.correct else glass.glassBorder, RoundedCornerShape(12.dp))
                        .clickable { selectedCorrect = idx }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(keys.getOrElse(idx) { "${idx + 1}" }, color = if (picked) CrystalWhite else glass.textPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        CoolingGlassTextField(
            value = explanation,
            onValueChange = { explanation = it },
            label = "Explanation / Solution Notes",
            singleLine = false,
            minLines = 3
        )
        Spacer(Modifier.height(14.dp))
        CoolingGlassButton(
            text = "Publish Official Answer",
            icon = Icons.Rounded.CheckCircle,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onFinalize(selectedCorrect, explanation.trim()) }
        )
    }
}

@Composable
private fun UploadSolutionSheet(
    pollTitle: String,
    onDismiss: () -> Unit,
    onSubmit: (imgBase64: String?, textExplanation: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var solutionImg by remember { mutableStateOf<String?>(null) }
    var textNotes by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                solutionImg = withContext(Dispatchers.IO) {
                    ImageCodec.encodeUriToBase64(context, uri, maxDimension = 680, quality = 65)
                }
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            scope.launch {
                solutionImg = withContext(Dispatchers.IO) {
                    ImageCodec.encodeBitmapToBase64(bmp, maxDimension = 680, quality = 65)
                }
            }
        }
    }

    CoolingGlassBottomSheet(title = appText("UPLOAD SOLUTION", "UPLOAD SOLUTION / समाधान अपलोड करें"), onDismiss = onDismiss) {
        Text(text = pollTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        val preview = remember(solutionImg) { ImageCodec.decodeToImageBitmap(solutionImg) }
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = "Solution photo preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.height(8.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoolingGlassOutlinedButton(
                text = "Gallery / Screenshot",
                icon = Icons.Rounded.AddPhotoAlternate,
                modifier = Modifier.weight(1.1f),
                onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            )
            CoolingGlassOutlinedButton(
                text = "Camera Photo",
                icon = Icons.Rounded.CameraAlt,
                modifier = Modifier.weight(0.9f),
                onClick = { cameraLauncher.launch(null) }
            )
        }
        Spacer(Modifier.height(10.dp))
        CoolingGlassTextField(
            value = textNotes,
            onValueChange = { textNotes = it },
            label = "Step-by-step Written Explanation",
            singleLine = false,
            minLines = 3
        )
        Spacer(Modifier.height(12.dp))
        CoolingGlassButton(
            text = "Submit Solution",
            icon = Icons.Rounded.UploadFile,
            enabled = solutionImg != null || textNotes.trim().length >= 3,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSubmit(solutionImg, textNotes.trim()) }
        )
    }
}
