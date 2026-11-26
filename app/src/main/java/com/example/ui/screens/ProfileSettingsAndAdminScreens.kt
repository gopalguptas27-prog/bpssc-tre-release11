package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminContactProfile
import com.example.data.model.AppRole
import com.example.data.model.AppUpdateRelease
import com.example.data.model.CategoryIconConfig
import com.example.data.model.DiscussionComment
import com.example.data.model.ExamGroup
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
import com.example.data.model.SetType
import com.example.data.model.SolutionReattemptRecord
import com.example.data.model.TestAttempt
import com.example.data.model.TieBreakPolicy
import com.example.data.model.UserProfile
import com.example.domain.ExamScoringEngine
import com.example.ui.glass.AutoResizeSingleLineText
import com.example.ui.glass.CoolingGlassAvatar
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassConfirmationPopup
import com.example.ui.glass.CoolingGlassDialog
import com.example.ui.glass.CoolingGlassEmptyState
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.CoolingGlassStatusBanner
import com.example.ui.glass.CoolingGlassTabRow
import com.example.ui.glass.CoolingGlassTextField
import com.example.ui.glass.FounderBrandingCard
import com.example.ui.glass.ProfilePhotoZoomDialog
import com.example.ui.glass.StatPill
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.GlacierBlue600
import com.example.ui.theme.IceCyan500
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.SoftLavender500
import com.example.ui.theme.appText
import com.example.util.ImageCodec
import com.example.util.IstClock
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun ProfileAndSettingsScreen(
    profile: UserProfile?,
    member: GroupMember?,
    group: ExamGroup?,
    myRank: Int?,
    myAttempts: List<TestAttempt>,
    myCreatedTestsCount: Int,
    myPollsCount: Int,
    tests: List<ExamTest>,
    darkGlassMode: Boolean,
    busy: Boolean,
    hasAvailableUpdate: Boolean = false,
    showContactAdmin: Boolean = true,
    onToggleDarkMode: (Boolean) -> Unit,
    onSaveProfile: (name: String, photoBase64: String?, langPref: LanguagePref, publicSharing: Boolean, whatsappNumber: String?) -> Unit,
    onOpenAttemptReview: (ExamTest, TestAttempt) -> Unit,
    onOpenContactAdmin: () -> Unit = {},
    onOpenAppUpdate: () -> Unit = {},
    onOpenAboutFounder: () -> Unit,
    onSignOut: () -> Unit,
    onBannerMessage: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val glass = LocalCoolingGlass.current

    val appLanguage = LocalAppLanguage.current
    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: member?.displayName ?: "") }
    var photoBase64 by remember(profile?.photoBase64) { mutableStateOf(profile?.photoBase64 ?: member?.photoBase64) }
    var whatsappInput by remember(profile?.whatsappNumber) { mutableStateOf(profile?.whatsappNumber.orEmpty()) }
    var languagePref by remember(profile?.languagePref, appLanguage) {
        mutableStateOf(profile?.langPreference() ?: appLanguage)
    }
    var publicSharing by remember(profile?.publicSharing) { mutableStateOf(profile?.publicSharing ?: true) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showAvatarZoom by remember { mutableStateOf(false) }

    val whatsappValid = WhatsAppHelper.isValidOptionalInput(whatsappInput)
    val regDateFormatted = remember(profile?.createdAt, member?.joinedAt) {
        val dt = profile?.createdAt?.toDate() ?: member?.joinedAt?.toDate()
        if (dt != null) IstClock.formatDate(dt, "dd MMM yyyy") else "Synced"
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                photoBase64 = withContext(Dispatchers.IO) {
                    ImageCodec.encodeUriToBase64(context, uri, maxDimension = 420, quality = 68)
                }
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            scope.launch {
                photoBase64 = withContext(Dispatchers.IO) {
                    ImageCodec.encodeBitmapToBase64(bmp, maxDimension = 420, quality = 68)
                }
            }
        }
    }

    var showAllHistory by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("profile_screen_list"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // 1. Compact Fit-to-Ratio Profile Header + Quick Photo Actions + 4-Pill Stats Strip
        item {
            CoolingGlassCard(
                strongFrost = true,
                borderColor = glass.accentCyan.copy(alpha = 0.55f),
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoolingGlassAvatar(
                        name = displayName.ifBlank { "User" },
                        photoBase64 = photoBase64,
                        size = 56.dp,
                        onClick = { showAvatarZoom = true }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        AutoResizeSingleLineText(
                            text = displayName.ifBlank { "User" },
                            style = MaterialTheme.typography.titleMedium,
                            maxFontSize = 16.sp,
                            minFontSize = 12.sp,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoolingGlassBadge(text = member?.appRole()?.labelEn ?: "User", color = glass.accentPrimary)
                            CoolingGlassBadge(text = if (myRank != null) "Rank #$myRank" else "Unranked", color = glass.accentCyan)
                        }
                        Spacer(Modifier.height(3.dp))
                        AutoResizeSingleLineText(
                            text = "ID: ${(profile?.userId?.takeIf { it.isNotBlank() } ?: profile?.id ?: member?.userId ?: "").take(10)} • Joined $regDateFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            maxFontSize = 10.5.sp,
                            minFontSize = 8.5.sp,
                            color = glass.textMuted
                        )
                    }
                }

                Spacer(Modifier.height(9.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CoolingGlassOutlinedButton(
                        text = "Gallery",
                        icon = Icons.Rounded.PhotoLibrary,
                        modifier = Modifier.weight(1f),
                        onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    )
                    CoolingGlassOutlinedButton(
                        text = "Camera",
                        icon = Icons.Rounded.CameraAlt,
                        modifier = Modifier.weight(1f),
                        onClick = { cameraLauncher.launch(null) }
                    )
                    if (photoBase64 != null) {
                        CoolingGlassOutlinedButton(
                            text = "Rotate",
                            icon = Icons.AutoMirrored.Rounded.RotateRight,
                            modifier = Modifier.weight(0.95f),
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

                Spacer(Modifier.height(9.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatPill("GS Avg", ExamScoringEngine.formatScore(member?.gsAverageScore ?: 0.0), glass.accentPrimary, Modifier.weight(1f))
                    StatPill("Attempts", "${myAttempts.size}", glass.accentCyan, Modifier.weight(1f))
                    StatPill("Sets", "$myCreatedTestsCount", SoftLavender500, Modifier.weight(1f))
                    StatPill("Polls", "$myPollsCount", glass.correct, Modifier.weight(1f))
                }
            }
        }

        // 2. Minimized Profile Form & Preferences Card
        item {
            CoolingGlassCard(
                strongFrost = true,
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AutoResizeSingleLineText(
                        text = "Profile & Preferences",
                        style = MaterialTheme.typography.titleSmall,
                        maxFontSize = 14.5.sp,
                        minFontSize = 11.5.sp,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    CoolingGlassBadge(text = "Private", color = glass.accentCyan)
                }
                Spacer(Modifier.height(8.dp))
                CoolingGlassTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = "Display Name",
                    testTagName = "profile_name_input"
                )
                Spacer(Modifier.height(7.dp))
                CoolingGlassTextField(
                    value = whatsappInput,
                    onValueChange = { whatsappInput = it },
                    label = "WhatsApp (Optional • e.g. +91 9876543210)",
                    placeholder = "Leave blank to remove",
                    leadingIcon = Icons.Rounded.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    testTagName = "profile_whatsapp_input"
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visible only to you & Admin.",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (whatsappInput.isNotBlank()) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.incorrect,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .testTag("clear_profile_whatsapp_btn")
                                .clickable { whatsappInput = "" }
                                .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                        )
                    }
                }
                if (!whatsappValid) {
                    Spacer(Modifier.height(5.dp))
                    CoolingGlassStatusBanner(
                        message = "Enter a valid 10-digit Indian number or +91 format.",
                        isError = true
                    )
                }

                Spacer(Modifier.height(8.dp))
                CoolingGlassTabRow(
                    tabs = LanguagePref.entries.map { it.displayLabel(languagePref) },
                    selectedIndex = LanguagePref.entries.indexOf(languagePref).coerceAtLeast(0),
                    onSelect = { idx -> languagePref = LanguagePref.entries[idx] }
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(glass.glassSurface)
                        .border(0.8.dp, glass.glassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (darkGlassMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                            contentDescription = null,
                            tint = glass.accentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        AutoResizeSingleLineText(
                            text = "Midnight Ice Dark Mode",
                            style = MaterialTheme.typography.bodySmall,
                            maxFontSize = 13.sp,
                            minFontSize = 10.sp,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Switch(checked = darkGlassMode, onCheckedChange = onToggleDarkMode)
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(glass.glassSurface)
                        .border(0.8.dp, glass.glassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AutoResizeSingleLineText(
                        text = "Public Result Card Sharing",
                        style = MaterialTheme.typography.bodySmall,
                        maxFontSize = 13.sp,
                        minFontSize = 10.sp,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = publicSharing, onCheckedChange = { publicSharing = it })
                }

                Spacer(Modifier.height(9.dp))
                CoolingGlassButton(
                    text = "Save Profile Settings",
                    icon = Icons.Rounded.Save,
                    loading = busy,
                    enabled = displayName.trim().length >= 2 && whatsappValid,
                    testTagName = "save_profile_btn",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onSaveProfile(
                            displayName.trim(),
                            photoBase64,
                            languagePref,
                            publicSharing,
                            whatsappInput.trim().takeIf { it.isNotBlank() }
                        )
                    }
                )
            }
        }

        // 3. Compact Support, Update & Group Invite Card (Unified & Fit-to-Ratio)
        item {
            CoolingGlassCard(contentPadding = PaddingValues(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showContactAdmin) {
                        CoolingGlassOutlinedButton(
                            text = "Contact Admin",
                            icon = Icons.AutoMirrored.Rounded.Chat,
                            tint = Color(0xFF16A34A),
                            testTagName = "profile_contact_admin_btn",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenContactAdmin
                        )
                    }
                    CoolingGlassOutlinedButton(
                        text = if (hasAvailableUpdate) "Update Ready" else "App Update",
                        icon = Icons.Rounded.SystemUpdate,
                        tint = if (hasAvailableUpdate) glass.accentPrimary else SoftLavender500,
                        testTagName = "profile_app_update_btn",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAppUpdate
                    )
                }

                if (group != null) {
                    Spacer(Modifier.height(9.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(glass.glassSurfaceStrong)
                            .border(0.8.dp, SoftLavender500.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = null,
                            tint = SoftLavender500,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            AutoResizeSingleLineText(
                                text = group.name,
                                style = MaterialTheme.typography.labelMedium,
                                maxFontSize = 12.5.sp,
                                minFontSize = 9.5.sp,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            AutoResizeSingleLineText(
                                text = "Code: ${group.inviteCode}",
                                style = MaterialTheme.typography.labelSmall,
                                maxFontSize = 11.5.sp,
                                minFontSize = 9.sp,
                                color = glass.accentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        CoolingGlassOutlinedButton(
                            text = "Copy",
                            icon = Icons.Rounded.ContentCopy,
                            modifier = Modifier.width(82.dp),
                            onClick = {
                                clipboard.setText(AnnotatedString(group.inviteCode))
                                onBannerMessage("Invite code ${group.inviteCode} copied!", false)
                            }
                        )
                        CoolingGlassOutlinedButton(
                            text = "Share",
                            icon = Icons.Rounded.Share,
                            modifier = Modifier.width(84.dp),
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join our TRE 4.0 Aspirant Test & Ranking Group '${group.name}' using Invite Code: ${group.inviteCode} (TRE 4.0 — Founded by Gopal Gupta)"
                                    )
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Group Invite Code"))
                            }
                        )
                    }
                }
            }
        }

        // 4. Compact Test History Card
        item {
            CoolingGlassCard(contentPadding = PaddingValues(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = glass.accentPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        AutoResizeSingleLineText(
                            text = "My Test History (${myAttempts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            maxFontSize = 14.sp,
                            minFontSize = 11.sp,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (myAttempts.size > 5) {
                        CoolingGlassBadge(
                            text = if (showAllHistory) "Show Less" else "All (${myAttempts.size})",
                            color = glass.accentCyan,
                            modifier = Modifier.clickable { showAllHistory = !showAllHistory }
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
                if (myAttempts.isEmpty()) {
                    Text(
                        text = "No completed tests yet. Attempt any paper to build your verified history.",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted
                    )
                } else {
                    val fmt = remember { IstClock.newDateFormat("dd MMM, hh:mm a") }
                    val visibleList = if (showAllHistory) myAttempts.take(20) else myAttempts.take(5)
                    visibleList.forEach { att ->
                        val matchedTest = tests.firstOrNull { it.id == att.testId }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(glass.glassSurface)
                                .border(0.8.dp, glass.glassBorder, RoundedCornerShape(11.dp))
                                .clickable(enabled = matchedTest != null) {
                                    if (matchedTest != null) onOpenAttemptReview(matchedTest, att)
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = att.testTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${att.paperType.replace("_", " ")} • ${att.submittedAt?.toDate()?.let { fmt.format(it) } ?: "Synced"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            CoolingGlassBadge(
                                text = "${ExamScoringEngine.formatScore(att.totalScore)}/${att.maxMarks.toInt()}",
                                color = glass.accentPrimary
                            )
                        }
                    }
                }
            }
        }

        // 5. Compact Founder & Sign Out
        item {
            FounderBrandingCard(
                compact = true,
                modifier = Modifier.clickable(onClick = onOpenAboutFounder)
            )
        }

        item {
            CoolingGlassOutlinedButton(
                text = "Sign Out of Google Account",
                icon = Icons.AutoMirrored.Rounded.Logout,
                tint = glass.incorrect,
                testTagName = "sign_out_btn",
                modifier = Modifier.fillMaxWidth(),
                onClick = { showSignOutConfirm = true }
            )
            Spacer(Modifier.height(14.dp))
        }
    }

    if (showAvatarZoom) {
        ProfilePhotoZoomDialog(
            name = displayName.ifBlank { "User" },
            photoBase64 = photoBase64,
            subtitle = "${member?.appRole()?.labelEn ?: "User"} • ${group?.name ?: "TRE 4.0"}",
            onDismiss = { showAvatarZoom = false }
        )
    }

    if (showSignOutConfirm) {
        CoolingGlassConfirmationPopup(
            title = "Sign Out?",
            message = "Your scores, rankings, and group membership are safely stored in TRE 4.0 Cloud. You can sign back in anytime.",
            confirmText = "Sign Out",
            onConfirm = {
                showSignOutConfirm = false
                onSignOut()
            },
            onDismiss = { showSignOutConfirm = false }
        )
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<GroupNotification>,
    isAdmin: Boolean,
    onSendNotification: (title: String, body: String, type: String) -> Unit,
    onClearNotification: (GroupNotification) -> Unit = {},
    onClearAllNotifications: () -> Unit = {},
    onNotificationClick: (GroupNotification) -> Unit = {}
) {
    val glass = LocalCoolingGlass.current
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    val fmt = remember { IstClock.newDateFormat("dd MMM yyyy, hh:mm a 'IST'") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isAdmin) {
            item {
                CoolingGlassCard(strongFrost = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = glass.accentCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Broadcast Group Alert", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    CoolingGlassTextField(value = title, onValueChange = { title = it }, label = "Alert Title")
                    Spacer(Modifier.height(8.dp))
                    CoolingGlassTextField(value = body, onValueChange = { body = it }, label = "Announcement Message", singleLine = false, minLines = 2)
                    Spacer(Modifier.height(10.dp))
                    CoolingGlassButton(
                        text = "Send to All Users",
                        icon = Icons.AutoMirrored.Rounded.Send,
                        enabled = title.trim().length >= 3 && body.trim().length >= 3,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onSendNotification(title.trim(), body.trim(), "ANNOUNCEMENT")
                            title = ""
                            body = ""
                        }
                    )
                }
            }
        }

        if (notifications.isNotEmpty()) {
            item {
                CoolingGlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alerts (${notifications.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap a notification to view & clear it automatically",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textMuted
                            )
                        }
                        CoolingGlassOutlinedButton(
                            text = "Clear History",
                            icon = Icons.Rounded.DeleteOutline,
                            tint = glass.incorrect,
                            testTagName = "clear_all_notifications_btn",
                            onClick = onClearAllNotifications
                        )
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                CoolingGlassEmptyState(
                    title = "No Notifications Yet",
                    subtitle = "New test alerts, app updates, ranking updates, and admin broadcasts will appear here.",
                    icon = Icons.Rounded.Notifications
                )
            }
        } else {
            items(notifications, key = { it.id }) { notif ->
                CoolingGlassCard(
                    modifier = Modifier.testTag("notif_card_${notif.id}"),
                    onClick = { onNotificationClick(notif) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoolingGlassBadge(
                            text = notif.type.replace("_", " "),
                            color = if (notif.type == "APP_UPDATE") SoftLavender500 else glass.accentCyan
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = notif.createdAt?.toDate()?.let { fmt.format(it) } ?: "Just now",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textMuted
                            )
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.incorrect,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onClearNotification(notif) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("clear_notif_${notif.id}")
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(notif.title, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(notif.body, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                    Spacer(Modifier.height(6.dp))
                    Text("— ${notif.senderName}", style = MaterialTheme.typography.labelSmall, color = glass.accentPrimary)
                }
            }
        }
    }
}

@Composable
fun AdminDashboardScreen(
    currentMember: GroupMember?,
    group: ExamGroup?,
    members: List<GroupMember>,
    tests: List<ExamTest>,
    attempts: List<TestAttempt>,
    polls: List<QuestionPoll>,
    overallRanking: List<RankedAspirant>,
    adminUserProfiles: List<UserProfile> = emptyList(),
    adminContact: AdminContactProfile? = null,
    appUpdates: List<AppUpdateRelease> = emptyList(),
    busy: Boolean = false,
    onApproveOrRejectTest: (testId: String, status: String) -> Unit,
    onApproveTestWithAccess: (testId: String, status: String, approvedForAll: Boolean, approvedMemberIds: List<String>) -> Unit = { tId, st, _, _ ->
        onApproveOrRejectTest(tId, st)
    },
    onEditTest: (ExamTest) -> Unit = {},
    onViewSetRank: (ExamTest) -> Unit = {},
    onUpdateTestIcon: (testId: String, iconKey: String?, customIconBase64: String?) -> Unit = { _, _, _ -> },
    onDeleteTest: (testId: String) -> Unit = {},
    onClearAllOldSets: () -> Unit = {},
    onWipeAllTrialData: () -> Unit = {},
    onCreateNewSet: () -> Unit = {},
    onAssignRole: (targetUid: String, role: AppRole) -> Unit,
    onSaveAdminContact: (adminName: String, adminPhotoBase64: String?, adminWhatsAppNumber: String?) -> Unit = { _, _, _ -> },
    onPublishAppUpdate: (
        versionName: String,
        versionCode: Long,
        title: String,
        releaseNotes: String,
        updateType: String,
        minSupportedVersionCode: Long,
        downloadUrl: String,
        fileSizeBytes: Long,
        sha256Checksum: String?,
        packageName: String?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onToggleAppUpdatePublished: (updateId: String, published: Boolean) -> Unit = { _, _ -> },
    onDeleteAppUpdate: (updateId: String) -> Unit = {},
    onPublishStarterSets: () -> Unit = {},
    onSendNotification: (title: String, body: String, type: String) -> Unit,
    onBannerMessage: (String, Boolean) -> Unit = { _, _ -> },
    discussions: List<DiscussionComment> = emptyList(),
    solutionReattempts: List<SolutionReattemptRecord> = emptyList(),
    onAssembleFullLengthSet: (
        title: String,
        targetSetType: SetType,
        part1Common: ExamTest,
        part2Language: ExamTest,
        part3Maths: ExamTest,
        fourthPart: ExamTest,
        durationMinutes: Int,
        publishImmediately: Boolean,
        cutoffMarks: Double?,
        pauseAllowed: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onUpdateTestExamRules: (
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
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onResolveReportedIssue: (commentId: String, resolved: Boolean, note: String) -> Unit = { _, _, _ -> },
    liveExams: List<LiveExam> = emptyList(),
    liveExamSessions: List<LiveExamSession> = emptyList(),
    onSaveOrScheduleLiveExam: (LiveExam, Boolean) -> Unit = { _, _ -> },
    onRemoveLiveExamUnlinkOnly: (LiveExam) -> Unit = {},
    onToggleLiveExamPublished: (LiveExam, Boolean) -> Unit = { _, _ -> },
    onCancelLiveExam: (LiveExam) -> Unit = {},
    onForceStartLiveExam: (LiveExam) -> Unit = {},
    onPublishLiveExamResultsNow: (LiveExam, Boolean) -> Unit = { _, _ -> },
    onModerateLiveExamSession: (LiveExamSession, String, String) -> Unit = { _, _, _ -> },
    onClearCompletedAndPastLiveExams: () -> Unit = {},
    categoryIcons: Map<String, CategoryIconConfig> = emptyMap(),
    onSaveCategoryIcon: (RankCategory, String?, String?) -> Unit = { _, _, _ -> },
    onResetCategoryIcon: (RankCategory) -> Unit = {},
    onUpdateTestMedia: (testId: String, iconKey: String?, customIconBase64: String?, bannerImageBase64: String?) -> Unit = { tId, k, b, _ ->
        onUpdateTestIcon(tId, k, b)
    },
    initialAdminTab: Int = 0
) {
    val glass = LocalCoolingGlass.current
    var adminTab by remember(initialAdminTab) { mutableIntStateOf(initialAdminTab) }
    var rolePickerTarget by remember { mutableStateOf<GroupMember?>(null) }
    var approvalDialogTest by remember { mutableStateOf<ExamTest?>(null) }
    var iconEditTarget by remember { mutableStateOf<ExamTest?>(null) }
    var showCategoryIconsDialog by remember { mutableStateOf(false) }
    var rulesEditTarget by remember { mutableStateOf<ExamTest?>(null) }
    var selectedUserDetail by remember { mutableStateOf<Pair<UserProfile, GroupMember?>?>(null) }
    var clearOldSetsStep by remember { mutableIntStateOf(0) }
    var wipeTrialStep by remember { mutableIntStateOf(0) }
    var deleteTestTarget by remember { mutableStateOf<ExamTest?>(null) }
    var deleteTestStep by remember { mutableIntStateOf(0) }
    var notifTitle by remember { mutableStateOf("") }
    var notifBody by remember { mutableStateOf("") }

    val pendingTests = remember(tests) {
        tests.filter { it.status != "PUBLISHED" }.sortedWith(ExamTest.newestFirstComparator)
    }
    val publishedTests = remember(tests) {
        tests.filter { it.status == "PUBLISHED" }.sortedWith(ExamTest.newestFirstComparator)
    }
    val openIssuesCount = remember(discussions) {
        discussions.count { (it.commentType == "REPORT_QUESTION" || it.commentType == "REPORT_ANSWER") && !it.resolved }
    }
    val groupAvg = remember(members) {
        val active = members.filter { it.gsTestsAttempted > 0L }
        if (active.isEmpty()) 0.0 else active.map { it.gsAverageScore }.average()
    }
    val topUser = overallRanking.firstOrNull { it.testsAttempted > 0 }?.member?.displayName ?: "—"
    val isSuperAdmin = currentMember?.appRole() == AppRole.SUPER_ADMIN

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CoolingGlassCard(strongFrost = true, tintColor = SoftLavender500) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = SoftLavender500, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Admin Control Center", style = MaterialTheme.typography.titleLarge, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                        Text("${group?.name ?: "Group"} • Role: ${currentMember?.appRole()?.labelEn ?: "Admin"}", style = MaterialTheme.typography.labelSmall, color = glass.textSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Users", "${members.size}", glass.accentPrimary, Modifier.weight(1f))
                    StatPill("Live Sets", "${publishedTests.size}", glass.correct, Modifier.weight(1f))
                    StatPill("Queue", "${pendingTests.size}", glass.review, Modifier.weight(1f))
                    StatPill("Attempts", "${attempts.size}", SoftLavender500, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Group Avg", ExamScoringEngine.formatScore(groupAvg), glass.accentCyan, Modifier.weight(1f))
                    StatPill("Polls", "${polls.size}", glass.accentPrimary, Modifier.weight(1f))
                    StatPill("Top #1", topUser, glass.correct, Modifier.weight(1.4f))
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CoolingGlassTabRow(
                    tabs = listOf("Sets (${tests.size})", "Live Exams (${liveExams.size})", "Users (${members.size})", "150Q Set"),
                    selectedIndex = when (adminTab) {
                        0 -> 0
                        8 -> 1
                        1 -> 2
                        5 -> 3
                        else -> -1
                    },
                    onSelect = {
                        adminTab = when (it) {
                            0 -> 0
                            1 -> 8
                            2 -> 1
                            else -> 5
                        }
                    }
                )
                CoolingGlassTabRow(
                    tabs = listOf("Issues ($openIssuesCount)", "Analytics", "Updates", "WhatsApp", "Alerts"),
                    selectedIndex = when (adminTab) {
                        6 -> 0
                        7 -> 1
                        2 -> 2
                        3 -> 3
                        4 -> 4
                        else -> -1
                    },
                    onSelect = {
                        adminTab = when (it) {
                            0 -> 6
                            1 -> 7
                            2 -> 2
                            3 -> 3
                            else -> 4
                        }
                    }
                )
            }
        }

        when (adminTab) {
            0 -> {
                item {
                    CoolingGlassCard(strongFrost = true) {
                        Text(
                            text = "Daily Set Approval & Reset Controls",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Invited members (joined via Invite Code) ONLY see sets you Approve below. Keep new sets in Queue and approve 1–2 sets per day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CoolingGlassButton(
                                text = "Create New Set",
                                icon = Icons.Rounded.PostAdd,
                                testTagName = "admin_create_new_set_btn",
                                modifier = Modifier.weight(1f),
                                onClick = onCreateNewSet
                            )
                            CoolingGlassOutlinedButton(
                                text = "Clear Old Sets",
                                icon = Icons.Rounded.DeleteOutline,
                                tint = glass.incorrect,
                                enabled = tests.isNotEmpty(),
                                testTagName = "clear_all_old_sets_btn",
                                modifier = Modifier.weight(1f),
                                onClick = { clearOldSetsStep = 1 }
                            )
                        }
                    }
                }

                // 7-Category Default Set Icons Controller Card (Upload once per category!)
                item {
                    CoolingGlassCard(
                        strongFrost = true,
                        tintColor = SoftLavender500,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                AutoResizeSingleLineText(
                                    text = "7-Category Default Set Icons",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxFontSize = 14.5.sp,
                                    minFontSize = 11.sp,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Set GIF / PNG / JPG icon once for each of the 7 categories — auto-applied to all sets.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textSecondary
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            CoolingGlassOutlinedButton(
                                text = "Manage 7 Icons",
                                icon = Icons.Rounded.Image,
                                tint = SoftLavender500,
                                testTagName = "admin_manage_category_icons_btn",
                                modifier = Modifier.width(134.dp),
                                onClick = { showCategoryIconsDialog = true }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RankCategory.entries.forEach { cat ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showCategoryIconsDialog = true },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    SetIconAvatar(
                                        iconKey = null,
                                        customIconBase64 = null,
                                        paperType = PaperType.COMMON_GS,
                                        size = 34.dp,
                                        categoryIconsOverride = categoryIcons,
                                        rankCategoryOverride = cat
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    AutoResizeSingleLineText(
                                        text = cat.displayName.substringBefore(" "),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxFontSize = 8.5.sp,
                                        minFontSize = 7.sp,
                                        color = glass.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 1: Pending Approval Queue (Hidden from invited members)
                item {
                    Text(
                        text = "Pending Approval Queue (${pendingTests.size}) — Hidden from Members",
                        style = MaterialTheme.typography.titleSmall,
                        color = glass.review,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (pendingTests.isEmpty()) {
                    item {
                        CoolingGlassEmptyState(
                            title = "Approval Queue is Empty",
                            subtitle = "Create sets into the Queue and approve 1–2 sets daily for All Members or Specific Selected Members.",
                            icon = Icons.Rounded.CheckCircle
                        )
                    }
                } else {
                    items(pendingTests, key = { "pending_${it.id}" }) { test ->
                        CoolingGlassCard(strongFrost = true, borderColor = glass.review.copy(alpha = 0.55f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoolingGlassBadge(text = test.parsedPaperType().baseSetName, color = glass.accentPrimary)
                                CoolingGlassBadge(text = "QUEUE • HIDDEN", color = glass.review)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SetIconAvatar(
                                    iconKey = test.iconKey,
                                    customIconBase64 = test.customIconBase64,
                                    paperType = test.parsedPaperType(),
                                    size = 44.dp,
                                    modifier = Modifier.clickable { iconEditTarget = test }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(test.title, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                                    Text("By ${test.creatorName} • ${test.questionCount} Q • ${test.durationMinutes} min", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CoolingGlassOutlinedButton(
                                    text = "Icon",
                                    icon = Icons.Rounded.Image,
                                    modifier = Modifier.weight(0.75f),
                                    onClick = { iconEditTarget = test }
                                )
                                CoolingGlassOutlinedButton(
                                    text = "Edit",
                                    icon = Icons.Rounded.Edit,
                                    modifier = Modifier.weight(0.75f),
                                    onClick = { onEditTest(test) }
                                )
                                CoolingGlassOutlinedButton(
                                    text = "Rules",
                                    icon = Icons.Rounded.Tune,
                                    tint = SoftLavender500,
                                    testTagName = "rules_pending_test_${test.id}",
                                    modifier = Modifier.weight(0.8f),
                                    onClick = { rulesEditTarget = test }
                                )
                                CoolingGlassOutlinedButton(
                                    text = "Del",
                                    icon = Icons.Rounded.DeleteOutline,
                                    tint = glass.incorrect,
                                    testTagName = "delete_pending_test_${test.id}",
                                    modifier = Modifier.weight(0.7f),
                                    onClick = {
                                        deleteTestTarget = test
                                        deleteTestStep = 1
                                    }
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            CoolingGlassButton(
                                text = "Approve & Publish",
                                icon = Icons.Rounded.CheckCircle,
                                accentGradient = listOf(Color(0xFF059669), IceCyan500),
                                testTagName = "approve_test_${test.id}",
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { approvalDialogTest = test }
                            )
                        }
                    }
                }

                // Section 2: Live Approved Sets (Visible to Approved Members)
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Live Approved Sets (${publishedTests.size}) — Member Access Controlled",
                        style = MaterialTheme.typography.titleSmall,
                        color = glass.correct,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (publishedTests.isEmpty()) {
                    item {
                        CoolingGlassCard {
                            Text(
                                text = "No sets are live for members yet. Tap 'Approve' on any queued set above to select members and release it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textMuted
                            )
                        }
                    }
                } else {
                    items(publishedTests, key = { "live_${it.id}" }) { test ->
                        CoolingGlassCard(strongFrost = true, borderColor = glass.correct.copy(alpha = 0.45f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoolingGlassBadge(text = test.parsedPaperType().baseSetName, color = glass.accentPrimary)
                                CoolingGlassBadge(text = "LIVE • APPROVED", color = glass.correct)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SetIconAvatar(
                                    iconKey = test.iconKey,
                                    customIconBase64 = test.customIconBase64,
                                    paperType = test.parsedPaperType(),
                                    size = 44.dp,
                                    modifier = Modifier.clickable { iconEditTarget = test }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(test.title, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                                    Text("${test.questionCount} Q • ${test.durationMinutes} min • ${test.attemptCount} attempts", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            CoolingGlassBadge(
                                text = test.accessSummaryLabel(),
                                color = if (test.approvedForAll) IceCyan500 else SoftLavender500
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CoolingGlassOutlinedButton(
                                    text = "Access",
                                    icon = Icons.Rounded.People,
                                    tint = glass.accentPrimary,
                                    testTagName = "manage_access_${test.id}",
                                    modifier = Modifier.weight(1f),
                                    onClick = { approvalDialogTest = test }
                                )
                                CoolingGlassOutlinedButton(
                                    text = "Hide",
                                    icon = Icons.Rounded.VisibilityOff,
                                    tint = glass.review,
                                    testTagName = "hide_live_test_${test.id}",
                                    modifier = Modifier.weight(0.9f),
                                    onClick = { onApproveTestWithAccess(test.id, "PENDING", false, emptyList()) }
                                )
                                CoolingGlassOutlinedButton(
                                    text = "Edit",
                                    icon = Icons.Rounded.Edit,
                                    modifier = Modifier.weight(0.85f),
                                    onClick = { onEditTest(test) }
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CoolingGlassOutlinedButton(
                                    text = "Rules",
                                    icon = Icons.Rounded.Tune,
                                    tint = glass.accentCyan,
                                    testTagName = "rules_live_test_${test.id}",
                                    modifier = Modifier.weight(0.95f),
                                    onClick = { rulesEditTarget = test }
                                )
                                CoolingGlassOutlinedButton(
                                    text = "Icon / Banner",
                                    icon = Icons.Rounded.Image,
                                    tint = SoftLavender500,
                                    testTagName = "admin_icon_banner_${test.id}",
                                    modifier = Modifier.weight(1.05f),
                                    onClick = { iconEditTarget = test }
                                )
                                CoolingGlassOutlinedButton(
                                    text = "Delete Set",
                                    icon = Icons.Rounded.DeleteOutline,
                                    tint = glass.incorrect,
                                    testTagName = "delete_live_test_${test.id}",
                                    modifier = Modifier.weight(1.1f),
                                    onClick = {
                                        deleteTestTarget = test
                                        deleteTestStep = 1
                                    }
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            CoolingGlassOutlinedButton(
                                text = "View Rank (All Participants)",
                                icon = Icons.Rounded.EmojiEvents,
                                tint = glass.accentPrimary,
                                testTagName = "admin_view_rank_${test.id}",
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onViewSetRank(test) }
                            )
                        }
                    }
                }

                // Section 3: Live Exam — Completed & Past (Trial) Cleanup inside Main Admin Console
                item {
                    val nowMillis = System.currentTimeMillis()
                    val completedOrPastLiveExams = liveExams.filter { exam ->
                        val dyn = exam.dynamicStatus(nowMillis)
                        dyn == LiveExamDynamicStatus.COMPLETED ||
                            dyn == LiveExamDynamicStatus.CANCELLED ||
                            exam.status in listOf("COMPLETED", "CANCELLED") ||
                            (exam.endTimeEpochMillis in 1..nowMillis)
                    }
                    CoolingGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_main_live_exam_cleanup_card"),
                        strongFrost = true,
                        tintColor = SoftLavender500,
                        borderColor = glass.incorrect.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Live Exam — Completed & Past Trial Data (${completedOrPastLiveExams.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Delete or remove Completed & Past trial Live Exams shown in the bottom taskbar Live Exam tab.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            CoolingGlassOutlinedButton(
                                text = "Live Exams",
                                icon = Icons.Rounded.Tune,
                                tint = SoftLavender500,
                                testTagName = "admin_open_live_exams_tab_btn",
                                onClick = { adminTab = 8 }
                            )
                        }
                        if (completedOrPastLiveExams.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            CoolingGlassOutlinedButton(
                                text = "Delete All Completed & Past Live Exams (${completedOrPastLiveExams.size})",
                                icon = Icons.Rounded.DeleteForever,
                                tint = glass.incorrect,
                                testTagName = "admin_clear_completed_live_exams_btn",
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onClearCompletedAndPastLiveExams
                            )
                            Spacer(Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                completedOrPastLiveExams.forEach { exam ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(glass.glassSurfaceStrong)
                                            .border(1.dp, glass.incorrect.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = exam.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = glass.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Completed/Past • ${exam.examDateIst} ${exam.startTimeIst}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = glass.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        CoolingGlassOutlinedButton(
                                            text = "Delete",
                                            icon = Icons.Rounded.DeleteOutline,
                                            tint = glass.incorrect,
                                            testTagName = "admin_delete_live_exam_${exam.id}",
                                            onClick = { onRemoveLiveExamUnlinkOnly(exam) }
                                        )
                                    }
                                }
                            }
                        } else if (liveExams.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                liveExams.forEach { exam ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(glass.glassSurfaceStrong)
                                            .border(1.dp, glass.glassBorderSubtle, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = exam.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = glass.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${exam.status} • ${exam.examDateIst} ${exam.startTimeIst}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = glass.textSecondary
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        CoolingGlassOutlinedButton(
                                            text = "Delete",
                                            icon = Icons.Rounded.DeleteOutline,
                                            tint = glass.incorrect,
                                            testTagName = "admin_delete_live_exam_${exam.id}",
                                            onClick = { onRemoveLiveExamUnlinkOnly(exam) }
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "No Completed or Past Live Exams present.",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textMuted
                            )
                        }
                    }
                }
            }

            1 -> {
                item {
                    AdminManageUsersSection(
                        userProfiles = adminUserProfiles,
                        groupMembers = members,
                        canAssignRoles = true,
                        currentUserId = currentMember?.userId.orEmpty(),
                        onAssignRole = onAssignRole,
                        onOpenUserDetail = { userProfile, memberObj ->
                            selectedUserDetail = userProfile to memberObj
                        }
                    )
                }
            }

            2 -> {
                item {
                    AdminAppUpdateManagementSection(
                        updates = appUpdates,
                        busy = busy,
                        onPublish = onPublishAppUpdate,
                        onTogglePublished = onToggleAppUpdatePublished,
                        onDelete = onDeleteAppUpdate
                    )
                }
            }

            3 -> {
                item {
                    AdminContactSettingsCard(
                        adminContact = adminContact,
                        currentMember = currentMember,
                        busy = busy,
                        onSave = onSaveAdminContact
                    )
                }
            }

            4 -> {
                item {
                    CoolingGlassCard(strongFrost = true) {
                        Text("Broadcast Announcement to Group", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        CoolingGlassTextField(value = notifTitle, onValueChange = { notifTitle = it }, label = "Announcement Heading")
                        Spacer(Modifier.height(8.dp))
                        CoolingGlassTextField(value = notifBody, onValueChange = { notifBody = it }, label = "Announcement Details", singleLine = false, minLines = 3)
                        Spacer(Modifier.height(12.dp))
                        CoolingGlassButton(
                            text = "Publish Group Notification",
                            icon = Icons.AutoMirrored.Rounded.Send,
                            enabled = notifTitle.trim().length >= 3 && notifBody.trim().length >= 3,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onSendNotification(notifTitle.trim(), notifBody.trim(), "ANNOUNCEMENT")
                                notifTitle = ""
                                notifBody = ""
                            }
                        )
                    }
                }
            }

            5 -> {
                item {
                    AdminSetAssemblerSection(
                        tests = tests,
                        busy = busy,
                        onAssembleFullLengthSet = onAssembleFullLengthSet
                    )
                }
            }

            6 -> {
                item {
                    AdminReportedIssuesSection(
                        discussions = discussions,
                        onResolveIssue = onResolveReportedIssue
                    )
                }
            }

            8 -> {
                item {
                    AdminLiveExamManagementSection(
                        tests = tests,
                        liveExams = liveExams,
                        liveExamSessions = liveExamSessions,
                        members = members,
                        onSaveOrScheduleLiveExam = onSaveOrScheduleLiveExam,
                        onRemoveLiveExamUnlinkOnly = onRemoveLiveExamUnlinkOnly,
                        onToggleLiveExamPublished = onToggleLiveExamPublished,
                        onCancelLiveExam = onCancelLiveExam,
                        onForceStartLiveExam = onForceStartLiveExam,
                        onPublishLiveExamResultsNow = onPublishLiveExamResultsNow,
                        onModerateLiveExamSession = onModerateLiveExamSession,
                        onClearCompletedAndPastLiveExams = onClearCompletedAndPastLiveExams
                    )
                }
            }

            else -> {
                item {
                    AdminAnalyticsAndCsvSection(
                        tests = tests,
                        members = members,
                        attempts = attempts,
                        solutionReattempts = solutionReattempts,
                        onBannerMessage = onBannerMessage
                    )
                }
            }
        }

        // Fresh Launch — Complete Trial Data Wipe Card (Always placed at the VERY BOTTOM of Admin Console on scroll)
        item {
            Spacer(Modifier.height(8.dp))
            CoolingGlassCard(
                strongFrost = true,
                tintColor = glass.incorrect,
                borderColor = glass.incorrect.copy(alpha = 0.5f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = glass.incorrect)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Fresh Launch — Clear All Trial Data",
                        style = MaterialTheme.typography.titleSmall,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Wipes ALL trial sets, ALL attempts (including Admin trial attempts), Live Exams, polls, doubts, banners & notifications, and resets all member scores/ranks to 0 while keeping the group & accounts intact.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
                Spacer(Modifier.height(10.dp))
                CoolingGlassOutlinedButton(
                    text = "Clear All Trial Data & Reset Rankings",
                    icon = Icons.Rounded.DeleteForever,
                    tint = glass.incorrect,
                    testTagName = "fresh_launch_wipe_btn",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { wipeTrialStep = 1 }
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }

    selectedUserDetail?.let { (userProf, memObj) ->
        AdminUserProfileDetailDialog(
            user = userProf,
            member = memObj,
            onDismiss = { selectedUserDetail = null },
            onFeedback = onBannerMessage
        )
    }

    iconEditTarget?.let { target ->
        SetIconPickerDialog(
            paperType = target.parsedPaperType(),
            currentIconKey = target.iconKey,
            currentCustomIconBase64 = target.customIconBase64,
            currentBannerImageBase64 = target.bannerImageBase64,
            showBannerEditor = true,
            onDismiss = { iconEditTarget = null },
            onSave = { key, b64 ->
                onUpdateTestIcon(target.id, key, b64)
                iconEditTarget = null
            },
            onSaveWithBanner = { key, b64, bannerB64 ->
                onUpdateTestMedia(target.id, key, b64, bannerB64)
                iconEditTarget = null
            }
        )
    }

    if (showCategoryIconsDialog) {
        AdminCategoryDefaultIconsDialog(
            categoryIcons = categoryIcons,
            busy = busy,
            onSaveCategoryIcon = onSaveCategoryIcon,
            onResetCategoryIcon = onResetCategoryIcon,
            onDismiss = { showCategoryIconsDialog = false }
        )
    }

    rolePickerTarget?.let { target ->
        val assignableRoles = if (isSuperAdmin) {
            listOf(AppRole.ASPIRANT, AppRole.GROUP_ADMIN, AppRole.SUPER_ADMIN)
        } else {
            listOf(AppRole.ASPIRANT, AppRole.GROUP_ADMIN)
        }
        CoolingGlassDialog(title = "Assign Role — ${target.displayName}", onDismiss = { rolePickerTarget = null }) {
            assignableRoles.forEach { role ->
                val current = target.appRole() == role
                CoolingGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    tintColor = if (current) glass.accentCyan else null,
                    contentPadding = PaddingValues(12.dp),
                    onClick = {
                        onAssignRole(target.userId, role)
                        rolePickerTarget = null
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(role.labelEn, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                            if (LocalAppLanguage.current != LanguagePref.ENGLISH) {
                                Text(role.labelHi, style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
                            }
                        }
                        if (current) {
                            CoolingGlassBadge("Current", glass.correct)
                        }
                    }
                }
            }
        }
    }

    // Per-Member Approval Access Dialog
    approvalDialogTest?.let { targetTest ->
        TestApprovalMembersDialog(
            test = targetTest,
            members = members,
            onDismiss = { approvalDialogTest = null },
            onConfirm = { approvedForAll, selectedIds ->
                approvalDialogTest = null
                onApproveTestWithAccess(targetTest.id, "PUBLISHED", approvedForAll, selectedIds)
            }
        )
    }

    // Two-Step Confirmation: Clear All Old Sets
    if (clearOldSetsStep == 1) {
        CoolingGlassConfirmationPopup(
            title = "Step 1 of 2: Clear All Old Sets?",
            message = "This will remove all ${tests.size} existing test set(s) in this group. You will be asked for final confirmation next.",
            confirmText = "Continue",
            confirmTestTag = "clear_old_step1_btn",
            isDestructive = true,
            onConfirm = { clearOldSetsStep = 2 },
            onDismiss = { clearOldSetsStep = 0 }
        )
    }
    if (clearOldSetsStep == 2) {
        CoolingGlassConfirmationPopup(
            title = "Step 2 of 2: Final Permanent Delete",
            message = appText(
                en = "FINAL WARNING: Permanently delete ALL ${tests.size} test sets from the group? This cannot be undone.",
                defaultText = "FINAL WARNING: Permanently delete ALL ${tests.size} test sets from the group? This cannot be undone / सभी पुराने सेट हमेशा के लिए हट जाएंगे।"
            ),
            confirmText = "Yes, Delete All Sets",
            confirmTestTag = "clear_old_step2_btn",
            isDestructive = true,
            onConfirm = {
                clearOldSetsStep = 0
                onClearAllOldSets()
            },
            onDismiss = { clearOldSetsStep = 0 }
        )
    }

    // Two-Step Confirmation: Fresh Launch — Clear All Trial Data
    if (wipeTrialStep == 1) {
        CoolingGlassConfirmationPopup(
            title = "Step 1 of 2: Wipe All Trial Data?",
            message = "This will permanently wipe ALL test sets, ALL attempts (including your own Admin trial attempts), polls, doubts, banners & notifications, and reset every member's score & ranking to 0.",
            confirmText = "Continue",
            confirmTestTag = "wipe_trial_step1_btn",
            isDestructive = true,
            onConfirm = { wipeTrialStep = 2 },
            onDismiss = { wipeTrialStep = 0 }
        )
    }
    if (wipeTrialStep == 2) {
        CoolingGlassConfirmationPopup(
            title = "Step 2 of 2: Final Fresh Launch Wipe",
            message = "FINAL WARNING: Permanently erase ALL trial data & reset all member rankings to 0? Your group and member accounts will remain intact so you can launch clean.",
            confirmText = "Yes, Wipe Everything",
            confirmTestTag = "wipe_trial_step2_btn",
            isDestructive = true,
            onConfirm = {
                wipeTrialStep = 0
                onWipeAllTrialData()
            },
            onDismiss = { wipeTrialStep = 0 }
        )
    }

    // Two-Step Confirmation: Delete Single Test Set
    deleteTestTarget?.let { target ->
        if (deleteTestStep == 1) {
            CoolingGlassConfirmationPopup(
                title = "Step 1 of 2: Delete Test Set?",
                message = "Are you sure you want to delete '${target.title}' (${target.questionCount} Q)? You will be asked for final confirmation next.",
                confirmText = "Continue",
                confirmTestTag = "admin_delete_step1_btn",
                isDestructive = true,
                onConfirm = { deleteTestStep = 2 },
                onDismiss = {
                    deleteTestTarget = null
                    deleteTestStep = 0
                }
            )
        } else if (deleteTestStep == 2) {
            CoolingGlassConfirmationPopup(
                title = "Step 2 of 2: Final Permanent Delete",
                message = appText(
                    en = "FINAL WARNING: Permanently delete '${target.title}' from the group? This cannot be undone.",
                    defaultText = "FINAL WARNING: Permanently delete '${target.title}' from the group? This cannot be undone / यह सेट हमेशा के लिए डिलीट हो जाएगा।"
                ),
                confirmText = "Yes, Delete Permanently",
                confirmTestTag = "admin_delete_step2_btn",
                isDestructive = true,
                onConfirm = {
                    val id = target.id
                    deleteTestTarget = null
                    deleteTestStep = 0
                    onDeleteTest(id)
                },
                onDismiss = {
                    deleteTestTarget = null
                    deleteTestStep = 0
                }
            )
        }
    }

    rulesEditTarget?.let { target ->
        AdminTestExamRulesDialog(
            test = target,
            onDismiss = { rulesEditTarget = null },
            onSave = { pos, negN, negD, unatt, optE, cutoff, pause, langPass, minHi, minEn, langFail, tieBreak ->
                rulesEditTarget = null
                onUpdateTestExamRules(
                    target.id, pos, negN, negD, unatt, optE, cutoff, pause,
                    langPass, minHi, minEn, langFail, tieBreak
                )
            }
        )
    }
}

@Composable
private fun TestApprovalMembersDialog(
    test: ExamTest,
    members: List<GroupMember>,
    onDismiss: () -> Unit,
    onConfirm: (approvedForAll: Boolean, selectedMemberIds: List<String>) -> Unit
) {
    val glass = LocalCoolingGlass.current
    var approveForAll by remember(test.id) {
        mutableStateOf(if (test.status == "PUBLISHED") test.approvedForAll else true)
    }
    var selectedIds by remember(test.id, members) {
        mutableStateOf(
            if (test.status == "PUBLISHED" && !test.approvedForAll) {
                test.approvedMemberIds.toSet()
            } else {
                members.map { it.userId }.toSet()
            }
        )
    }

    CoolingGlassDialog(
        title = appText("Approve Set Access", "Approve Set Access / मेंबर चुनें"),
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = test.title,
                style = MaterialTheme.typography.titleSmall,
                color = glass.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose whether ALL group members or ONLY selected members can view & attempt this set.",
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("approve_mode_all")
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (approveForAll) glass.accentPrimary else glass.glassSurfaceStrong)
                        .border(
                            1.dp,
                            if (approveForAll) glass.accentCyan else glass.glassBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { approveForAll = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "All Members",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (approveForAll) CrystalWhite else glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("approve_mode_specific")
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!approveForAll) SoftLavender500 else glass.glassSurfaceStrong)
                        .border(
                            1.dp,
                            if (!approveForAll) SoftLavender500 else glass.glassBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { approveForAll = false }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Specific Members",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (!approveForAll) CrystalWhite else glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (!approveForAll) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedIds.size} of ${members.size} members selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Select All",
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.accentPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .testTag("select_all_members_btn")
                                .clickable { selectedIds = members.map { it.userId }.toSet() }
                        )
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.incorrect,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .testTag("clear_selected_members_btn")
                                .clickable { selectedIds = emptySet() }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Unselected members will NOT see or access this test set.",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textMuted
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    members.forEach { member ->
                        val checked = member.userId in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_select_${member.userId}")
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (checked) glass.accentCyan.copy(alpha = 0.14f) else glass.glassSurfaceStrong)
                                .border(
                                    1.dp,
                                    if (checked) glass.accentCyan else glass.glassBorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedIds = if (checked) selectedIds - member.userId else selectedIds + member.userId
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null
                            )
                            Spacer(Modifier.width(6.dp))
                            CoolingGlassAvatar(name = member.displayName, photoBase64 = member.photoBase64, size = 34.dp)
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = member.appRole().labelEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textMuted
                                )
                            }
                        }
                    }
                }
            } else {
                CoolingGlassCard(contentPadding = PaddingValues(12.dp)) {
                    Text(
                        text = "All ${members.size} joined group member(s) (and any future members who join via Invite Code) will be able to view & attempt this set.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textPrimary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            CoolingGlassButton(
                text = if (approveForAll) {
                    "Approve for All Members"
                } else {
                    "Approve for ${selectedIds.size} Selected Member(s)"
                },
                icon = Icons.Rounded.CheckCircle,
                enabled = approveForAll || selectedIds.isNotEmpty(),
                accentGradient = listOf(Color(0xFF059669), IceCyan500),
                testTagName = "confirm_member_approval_btn",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onConfirm(approveForAll, if (approveForAll) emptyList() else selectedIds.toList())
                }
            )
        }
    }
}

/**
 * Official About Us, App Information, Credits & Founder Branding Screen.
 * Displays exact official text:
 * TRE 4.0
 * Founded by Gopal Gupta
 */
@Composable
fun AboutFounderScreen() {
    val glass = LocalCoolingGlass.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            FounderBrandingCard(compact = false)
        }

        item {
            CoolingGlassCard(strongFrost = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Verified, contentDescription = null, tint = glass.accentCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("Official App Information", style = MaterialTheme.typography.titleLarge, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                InfoRow("Application Name", "TRE 4.0")
                InfoRow("Platform Type", "Aspirant Test & Ranking Platform")
                InfoRow("Founder", "Gopal Gupta")
                InfoRow("Official Branding", "Founded by Gopal Gupta")
                InfoRow("Visual Design System", "Cooling Glass / Frosted & Liquid Glass (Material 3)")
                InfoRow("Cloud Architecture", "Centralized Real-Time Firestore + Google Sign-In")
            }
        }

        item {
            CoolingGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = glass.accentPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Official TRE 4.0 Examination Rules", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                val hindiGrammarWord = appText("Grammar", "व्याकरण")
                val optENotAttemptedLabel = appText("Not Attempted", "Not Attempted / नॉट अटेम्प्ट")
                Text(
                    text = "• Part 1 — Language Paper (30 Q • 15 Min): 8 English (1 short passage × 4 Q + 4 Grammar) + 22 Hindi (2 passages × 4 Q + 14 $hindiGrammarWord). Qualifying Pass >= 9.00/30.\n\n" +
                        "• Part 2 — Common Paper (40 Q • 30 Min): 8 Mathematics (Class 6–10 NCERT/SCERT) + 8 Science (Class 6–10 NCERT + Bihar SCERT) + 8 Indian Geography + 8 Indian National Movement (1857–1947) + 8 Current Affairs (PIB Facts).\n\n" +
                        "• Part 3 — Language + Common Paper (70 Q • 45 Min): 30 Language + 40 Common with strict section & answer-key separation.\n\n" +
                        "• Part 4a — Class 6 to 8 Mathematics and Science (150 Q • 2 Hours): 30 Language + 40 Common + 40 Math + 40 Science (Class 6–8 Bihar Board SCERT + NCERT).\n\n" +
                        "• Part 4b — Class 9-10 Mathematics (150 Q • 2 Hours 15 Min): 30 Language + 40 Common + 80 Mathematics (Class 6–10 Bihar Board SCERT + NCERT, Moderate & Hard).\n\n" +
                        "• 5-Option MCQ Marking Scheme: Correct = +1, Wrong = -1/3, Option E ($optENotAttemptedLabel) = 0, Completely Blank = -1/3.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
            }
        }

        item {
            CoolingGlassCard(tintColor = SoftLavender500) {
                Text("Official Credits & Role Separation", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "TRE 4.0 — Founded by Gopal Gupta.\n\n" +
                        "Founder identity represents official platform attribution and remains strictly independent from Firebase Authentication and Role-Based Access Control (Admin and User roles).",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val glass = LocalCoolingGlass.current
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = glass.textPrimary, fontWeight = FontWeight.SemiBold)
    }
}
