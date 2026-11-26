package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DailyTargetTask
import com.example.data.model.ExamTest
import com.example.data.model.LanguagePref
import com.example.data.model.RankCategory
import com.example.data.model.TestAttempt
import com.example.ui.glass.AnimatedBase64Image
import com.example.ui.glass.AutoResizeSingleLineText
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassBottomSheet
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.CoolingGlassStatusBanner
import com.example.ui.glass.CoolingGlassTextField
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.GlacierBlue600
import com.example.ui.theme.IceCyan500
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.appText
import com.example.util.ImageCodec
import com.example.util.IstClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Home screen section for Admin-assigned Daily Target Set ("आज का टारगेट / Aaj Ka Target"):
 * - Shows Today's active target(s) as clickable Photo Banners (tap banner -> directly opens target set).
 * - Automatically rolls over Yesterday's unattempted target as "बीते हुए कल का टास्क • Yesterday's Missed Task".
 * - Gives Admin direct "+ नया / कल का बैनर" button and Edit controls to schedule Today or Next Day banners.
 */
@Composable
fun DailyTargetHomeSection(
    dailyTargets: List<DailyTargetTask>,
    tests: List<ExamTest>,
    myAttempts: List<TestAttempt>,
    isAdmin: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
    onOpenTargetTest: (ExamTest) -> Unit,
    onSaveDailyTarget: (
        existingId: String?,
        test: ExamTest,
        targetDateIst: String,
        bannerTitle: String,
        bannerSubtitle: String?,
        bannerImageBase64: String?
    ) -> Unit,
    onDeleteDailyTarget: (String) -> Unit
) {
    val glass = LocalCoolingGlass.current
    val todayIst = remember(nowMillis) { IstClock.todayIstKey(nowMillis) }
    val yesterdayIst = remember(nowMillis) { IstClock.offsetIstKey(-1, nowMillis) }
    val tomorrowIst = remember(nowMillis) { IstClock.offsetIstKey(1, nowMillis) }

    val testsById = remember(tests) { tests.associateBy { it.id } }
    val attemptedTestIds = remember(myAttempts) { myAttempts.map { it.testId }.toSet() }

    val todayTargets = remember(dailyTargets, todayIst, testsById, isAdmin) {
        dailyTargets.filter {
            it.active &&
                it.targetDateIst == todayIst &&
                (isAdmin || testsById[it.testId] != null)
        }
    }
    val yesterdayMissedTargets = remember(dailyTargets, yesterdayIst, attemptedTestIds, testsById, isAdmin) {
        dailyTargets.filter {
            it.active &&
                it.targetDateIst == yesterdayIst &&
                it.testId !in attemptedTestIds &&
                (isAdmin || testsById[it.testId] != null)
        }
    }
    val upcomingTargets = remember(dailyTargets, todayIst) {
        dailyTargets.filter { it.active && it.targetDateIst > todayIst }
            .sortedBy { it.targetDateIst }
    }

    var showEditorSheet by remember { mutableStateOf(false) }
    var editingTarget by remember { mutableStateOf<DailyTargetTask?>(null) }
    var defaultEditorDateIst by remember { mutableStateOf(todayIst) }

    val shouldRenderSection = todayTargets.isNotEmpty() || yesterdayMissedTargets.isNotEmpty() || isAdmin
    if (!shouldRenderSection) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_target_home_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appText("Today's Target", "आज का टारगेट • Today's Target"),
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = appText(
                        en = "${IstClock.formatIstKeyReadable(todayIst)} (IST) • Tap banner to start target set",
                        defaultText = "${IstClock.formatIstKeyReadable(todayIst)} (IST) • बैनर पर क्लिक करके सेट शुरू करें"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isAdmin) {
                Spacer(Modifier.width(8.dp))
                CoolingGlassBadge(
                    text = appText("+ Set Target", "+ टारगेट सेट करें"),
                    color = GlacierBlue600,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 36.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            editingTarget = null
                            defaultEditorDateIst = if (todayTargets.isNotEmpty()) tomorrowIst else todayIst
                            showEditorSheet = true
                        }
                        .testTag("admin_add_daily_target_btn")
                )
            }
        }

        // 1 & 2. Compact 1/4-Size 2-Column Grid for Today's Target + Yesterday's Missed Task Banners
        // Up to 4 targets fit together in a 2x2 grid (~1/4 size per banner vs full-width banner).
        val activeBannerItems = remember(todayTargets, yesterdayMissedTargets) {
            todayTargets.map { it to false } + yesterdayMissedTargets.map { it to true }
        }
        if (activeBannerItems.isNotEmpty()) {
            val pages = remember(activeBannerItems) { activeBannerItems.chunked(4) }
            if (pages.size == 1) {
                DailyTargetCompactGridPage(
                    pageItems = pages.first(),
                    testsById = testsById,
                    attemptedTestIds = attemptedTestIds,
                    isAdmin = isAdmin,
                    onOpenTargetTest = onOpenTargetTest,
                    onEditTarget = { target ->
                        editingTarget = target
                        defaultEditorDateIst = target.targetDateIst
                        showEditorSheet = true
                    }
                )
            } else {
                val pagerState = rememberPagerState(pageCount = { pages.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    pageSpacing = 8.dp
                ) { pageIndex ->
                    DailyTargetCompactGridPage(
                        pageItems = pages[pageIndex],
                        testsById = testsById,
                        attemptedTestIds = attemptedTestIds,
                        isAdmin = isAdmin,
                        onOpenTargetTest = onOpenTargetTest,
                        onEditTarget = { target ->
                            editingTarget = target
                            defaultEditorDateIst = target.targetDateIst
                            showEditorSheet = true
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (i == pagerState.currentPage) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == pagerState.currentPage) glass.accentCyan
                                    else glass.textMuted.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }
        }

        if (todayTargets.isEmpty() && isAdmin) {
            // Admin Quick-Create Prompt when Today has no Target Banner yet
            CoolingGlassCard(
                tintColor = GlacierBlue600,
                borderColor = GlacierBlue600.copy(alpha = 0.45f),
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.testTag("admin_empty_daily_target_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlacierBlue600.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Flag,
                            contentDescription = "Assign Today's Target",
                            tint = GlacierBlue600,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appText("Select Today's Target Set (Admin)", "आज का टारगेट सेट चुनें (Admin)"),
                            style = MaterialTheme.typography.titleSmall,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = appText(
                                en = "Select any published set so all users see Today's Target photo banner on Home.",
                                defaultText = "कोई भी सेट सेलेक्ट करें ताकि सभी यूज़र्स को होम स्क्रीन पर फोटो बैनर दिखे।"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoolingGlassButton(
                        text = appText("Create Today's Banner", "आज का बैनर बनाएं"),
                        icon = Icons.Rounded.Add,
                        testTagName = "admin_create_today_target_btn",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            editingTarget = null
                            defaultEditorDateIst = todayIst
                            showEditorSheet = true
                        }
                    )
                    CoolingGlassOutlinedButton(
                        text = appText("For Tomorrow", "कल के लिए"),
                        icon = Icons.Rounded.CalendarMonth,
                        testTagName = "admin_create_tomorrow_target_btn",
                        modifier = Modifier.weight(0.85f),
                        onClick = {
                            editingTarget = null
                            defaultEditorDateIst = tomorrowIst
                            showEditorSheet = true
                        }
                    )
                }
            }
        }

        // 3. Admin-only Scheduled Next-Day / Upcoming Banners Strip
        if (isAdmin && upcomingTargets.isNotEmpty()) {
            CoolingGlassCard(
                tintColor = IceCyan500,
                borderColor = IceCyan500.copy(alpha = 0.4f),
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.testTag("admin_upcoming_targets_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = IceCyan500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = appText(
                                en = "Scheduled for Next Day (${upcomingTargets.size})",
                                defaultText = "अगले दिन के लिए शेड्यूल्ड (${upcomingTargets.size})"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Admin Preview",
                        style = MaterialTheme.typography.labelSmall,
                        color = IceCyan500,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(6.dp))
                val currentLang = LocalAppLanguage.current
                upcomingTargets.take(3).forEach { nextTarget ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                editingTarget = nextTarget
                                defaultEditorDateIst = nextTarget.targetDateIst
                                showEditorSheet = true
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${IstClock.formatIstKeyReadable(nextTarget.targetDateIst)} • ${nextTarget.displayBannerTitle(currentLang)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Set: ${nextTarget.testTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "Edit scheduled target",
                            tint = glass.accentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showEditorSheet && isAdmin) {
        DailyTargetEditorSheet(
            editingTarget = editingTarget,
            defaultDateIst = defaultEditorDateIst,
            allTargets = dailyTargets,
            tests = tests,
            nowMillis = nowMillis,
            onSave = { existingId, selectedTest, dateIst, headline, subtitle, imgBase64 ->
                onSaveDailyTarget(existingId, selectedTest, dateIst, headline, subtitle, imgBase64)
                showEditorSheet = false
                editingTarget = null
            },
            onDelete = { targetId ->
                onDeleteDailyTarget(targetId)
                showEditorSheet = false
                editingTarget = null
            },
            onSelectExistingForEdit = { existing ->
                editingTarget = existing
                defaultEditorDateIst = existing.targetDateIst
            },
            onDismiss = {
                showEditorSheet = false
                editingTarget = null
            }
        )
    }
}

@Composable
private fun DailyTargetCompactGridPage(
    pageItems: List<Pair<DailyTargetTask, Boolean>>,
    testsById: Map<String, ExamTest>,
    attemptedTestIds: Set<String>,
    isAdmin: Boolean,
    onOpenTargetTest: (ExamTest) -> Unit,
    onEditTarget: (DailyTargetTask) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_target_compact_grid"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pageItems.chunked(2).forEach { rowPairs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPairs.forEach { (target, isMissedYesterday) ->
                    val linkedTest = testsById[target.testId]
                    val isCompleted = !isMissedYesterday && target.testId in attemptedTestIds
                    val bannerTag = if (isMissedYesterday) {
                        "daily_target_yesterday_missed_${target.id}"
                    } else {
                        "daily_target_today_banner_${target.id}"
                    }
                    DailyTargetPhotoBannerCard(
                        target = target,
                        linkedTest = linkedTest,
                        isCompleted = isCompleted,
                        isMissedYesterday = isMissedYesterday,
                        isAdmin = isAdmin,
                        tag = bannerTag,
                        compact = true,
                        modifier = Modifier.weight(1f),
                        onOpen = {
                            if (linkedTest != null) {
                                onOpenTargetTest(linkedTest)
                            }
                        },
                        onEdit = { onEditTarget(target) }
                    )
                }
                if (rowPairs.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DailyTargetPhotoBannerCard(
    target: DailyTargetTask,
    linkedTest: ExamTest?,
    isCompleted: Boolean,
    isMissedYesterday: Boolean,
    isAdmin: Boolean,
    tag: String,
    compact: Boolean = true,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    val hasCustomBanner = !target.bannerImageBase64.isNullOrBlank()
    val isGifBanner = remember(target.bannerImageBase64) {
        ImageCodec.isGifBase64(target.bannerImageBase64)
    }
    val accentColor = when {
        isMissedYesterday -> Color(0xFFF59E0B)
        isCompleted -> Color(0xFF10B981)
        else -> Color(0xFF38BDF8)
    }
    val topBadgeText = if (compact) {
        when {
            isMissedYesterday -> appText("⏰ YESTERDAY'S MISSED TASK", "⏰ कल का टास्क")
            isCompleted -> appText("✅ TODAY'S TARGET COMPLETED", "✅ पूरा हुआ")
            else -> appText("🎯 TODAY'S TARGET", "🎯 आज का टारगेट")
        }
    } else {
        when {
            isMissedYesterday -> appText("⏰ YESTERDAY'S MISSED TASK", "⏰ बीते हुए कल का टास्क • YESTERDAY'S MISSED TASK")
            isCompleted -> appText("✅ TODAY'S TARGET COMPLETED", "✅ आज का टारगेट पूरा • COMPLETED")
            else -> appText("🎯 TODAY'S TARGET", "🎯 आज का टारगेट • AAJ KA TARGET")
        }
    }
    val ctaText = if (compact) {
        when {
            linkedTest == null -> if (isAdmin) appText("Edit", "Edit") else appText("N/A", "उपलब्ध नहीं")
            isMissedYesterday -> appText("Complete Set", "पूरा करें")
            isCompleted -> appText("View Result", "रिज़ल्ट")
            else -> appText("Start Set", "शुरू करें")
        }
    } else {
        when {
            linkedTest == null -> if (isAdmin) {
                appText("⚠️ Set Not Published — Edit", "⚠️ सेट पब्लिश्ड नहीं है — Edit करें")
            } else {
                appText("Set Not Available", "सेट उपलब्ध नहीं")
            }
            isMissedYesterday -> appText("Complete Set", "अभी पूरा करें • Complete Set")
            isCompleted -> appText("View Result", "रिज़ल्ट व हल देखें • View Result")
            else -> appText("Start Set", "सेट शुरू करें • Start Set")
        }
    }

    val cornerRadius = if (compact) 14.dp else 22.dp
    val cardAspect = when {
        compact -> 16f / 10.4f
        isMissedYesterday -> 16f / 8.8f
        else -> 16f / 9.4f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cardAspect)
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = if (isMissedYesterday) 1.4.dp else 1.1.dp,
                color = accentColor.copy(alpha = 0.78f),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable(enabled = linkedTest != null, onClick = onOpen)
            .testTag(tag)
    ) {
        // Photo / Animated GIF / Transparent PNG Layer (Custom Admin Media or Bundled HD Target Artwork)
        if (hasCustomBanner) {
            AnimatedBase64Image(
                base64 = target.bannerImageBase64,
                contentDescription = target.bannerTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Image(
                        painter = painterResource(id = R.drawable.img_aaj_ka_target_banner_1795657931767),
                        contentDescription = target.bannerTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_aaj_ka_target_banner_1795657931767),
                contentDescription = target.bannerTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Rich Gradient Scrim for High-Contrast Poster Typography
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = if (isMissedYesterday) {
                            listOf(
                                Color(0xEB2A1404),
                                Color(0xCC451A03),
                                Color(0x661E1B4B)
                            )
                        } else {
                            listOf(
                                Color(0xEB07142A),
                                Color(0xC80C2348),
                                Color(0x5507142A)
                            )
                        }
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xA6040C1B)
                        )
                    )
                )
        )

        // Foreground Poster Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (compact) 8.dp else 15.dp,
                    vertical = if (compact) 7.dp else 13.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Pill Row + Admin Edit Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(50))
                        .background(accentColor.copy(alpha = 0.28f))
                        .border(0.8.dp, accentColor.copy(alpha = 0.85f), RoundedCornerShape(50))
                        .padding(
                            horizontal = if (compact) 6.dp else 10.dp,
                            vertical = if (compact) 2.dp else 4.dp
                        )
                ) {
                    AutoResizeSingleLineText(
                        text = topBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        maxFontSize = if (compact) 8.5.sp else 11.sp,
                        minFontSize = 7.sp,
                        color = CrystalWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isGifBanner) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xD9059669))
                                .padding(horizontal = if (compact) 4.dp else 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GIF",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = if (compact) 7.5.sp else 9.sp,
                                color = CrystalWhite,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    if (!compact) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x99081427))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = IstClock.formatIstKeyReadable(target.targetDateIst),
                                style = MaterialTheme.typography.labelSmall,
                                color = CrystalWhite.copy(alpha = 0.92f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (isAdmin) {
                        Box(
                            modifier = Modifier
                                .size(if (compact) 24.dp else 34.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC081427))
                                .clickable(onClick = onEdit)
                                .testTag("edit_daily_target_${target.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Edit Daily Target Banner",
                                tint = CrystalWhite,
                                modifier = Modifier.size(if (compact) 13.dp else 16.dp)
                            )
                        }
                    }
                }
            }

            // Middle Poster Headline & Target Set Name
            Column(verticalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 3.dp)) {
                Text(
                    text = target.displayBannerTitle(currentLang),
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleLarge,
                    fontSize = if (compact) 11.5.sp else 20.sp,
                    lineHeight = if (compact) 14.sp else 24.sp,
                    color = CrystalWhite,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = linkedTest?.title ?: target.testTitle,
                    style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleSmall,
                    fontSize = if (compact) 9.5.sp else 14.sp,
                    lineHeight = if (compact) 12.sp else 18.sp,
                    color = Color(0xFFFDE68A),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!compact) {
                    val defaultMissedSubtitle = appText(
                        en = "You missed this target yesterday — tap to complete it today!",
                        defaultText = "कल का यह टारगेट छूट गया था — टैप करके आज पूरा करें!"
                    )
                    val subtitleLine = when {
                        isMissedYesterday -> target.bannerSubtitle?.takeIf { it.isNotBlank() }
                            ?: defaultMissedSubtitle
                        !target.bannerSubtitle.isNullOrBlank() -> target.bannerSubtitle
                        else -> null
                    }
                    if (!subtitleLine.isNullOrBlank()) {
                        Text(
                            text = subtitleLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = CrystalWhite.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom Meta + Action Button Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (linkedTest != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x99081427))
                            .padding(
                                horizontal = if (compact) 5.dp else 8.dp,
                                vertical = if (compact) 2.dp else 4.dp
                            )
                    ) {
                        Text(
                            text = if (compact) {
                                "${linkedTest.questionCount}Q•${linkedTest.durationMinutes}m"
                            } else {
                                "${linkedTest.questionCount} Q • ${linkedTest.durationMinutes} min"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = if (compact) 8.5.sp else 11.sp,
                            color = CrystalWhite,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                when {
                                    isMissedYesterday -> listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                                    isCompleted -> listOf(Color(0xFF059669), Color(0xFF10B981))
                                    else -> listOf(GlacierBlue600, IceCyan500)
                                }
                            )
                        )
                        .padding(
                            horizontal = if (compact) 7.dp else 12.dp,
                            vertical = if (compact) 3.5.dp else 7.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = when {
                            isCompleted -> Icons.Rounded.CheckCircle
                            isMissedYesterday -> Icons.Rounded.History
                            else -> Icons.Rounded.PlayArrow
                        },
                        contentDescription = null,
                        tint = CrystalWhite,
                        modifier = Modifier.size(if (compact) 11.dp else 15.dp)
                    )
                    AutoResizeSingleLineText(
                        text = ctaText,
                        style = MaterialTheme.typography.labelSmall,
                        maxFontSize = if (compact) 9.sp else 11.sp,
                        minFontSize = 7.5.sp,
                        color = CrystalWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (!compact) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = CrystalWhite,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTargetEditorSheet(
    editingTarget: DailyTargetTask?,
    defaultDateIst: String,
    allTargets: List<DailyTargetTask>,
    tests: List<ExamTest>,
    nowMillis: Long,
    onSave: (
        existingId: String?,
        test: ExamTest,
        targetDateIst: String,
        bannerTitle: String,
        bannerSubtitle: String?,
        bannerImageBase64: String?
    ) -> Unit,
    onDelete: (String) -> Unit,
    onSelectExistingForEdit: (DailyTargetTask) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalCoolingGlass.current

    val todayIst = remember(nowMillis) { IstClock.todayIstKey(nowMillis) }
    val tomorrowIst = remember(nowMillis) { IstClock.offsetIstKey(1, nowMillis) }
    val dayAfterIst = remember(nowMillis) { IstClock.offsetIstKey(2, nowMillis) }

    val availableTests = remember(tests) {
        tests
            .filter { it.status == "PUBLISHED" }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    var targetDateIst by remember(editingTarget?.id, defaultDateIst) {
        mutableStateOf(editingTarget?.targetDateIst?.ifBlank { null } ?: defaultDateIst)
    }
    // Admin MUST manually choose the target set for new banners (never auto-picked)
    var selectedTestId by remember(editingTarget?.id) {
        mutableStateOf(
            editingTarget?.testId?.takeIf { id -> availableTests.any { it.id == id } }.orEmpty()
        )
    }
    var selectedCategoryFilter by remember { mutableStateOf<RankCategory?>(null) }
    val currentLang = LocalAppLanguage.current
    val defaultBannerTitle = if (currentLang == LanguagePref.ENGLISH) "Today's Target" else "आज का टारगेट • Aaj Ka Target"
    var bannerTitle by remember(editingTarget?.id, currentLang) {
        mutableStateOf(editingTarget?.bannerTitle?.ifBlank { null } ?: defaultBannerTitle)
    }
    var bannerSubtitle by remember(editingTarget?.id) {
        mutableStateOf(editingTarget?.bannerSubtitle.orEmpty())
    }
    var bannerImageBase64 by remember(editingTarget?.id) {
        mutableStateOf(editingTarget?.bannerImageBase64)
    }
    var bannerUploadError by remember { mutableStateOf<String?>(null) }
    var testSearchQuery by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    ImageCodec.encodeUriToBase64Result(
                        context = context,
                        uri = uri,
                        maxDimension = 720,
                        quality = 65,
                        allowGif = true
                    )
                }
                res.onSuccess { encoded ->
                    bannerImageBase64 = encoded
                    bannerUploadError = null
                }.onFailure { err ->
                    bannerUploadError = err.localizedMessage ?: "Could not load selected GIF/PNG image."
                }
            }
        }
    }

    val selectedTest = availableTests.firstOrNull { it.id == selectedTestId }
    val filteredTests = remember(availableTests, testSearchQuery, selectedCategoryFilter) {
        val q = testSearchQuery.trim().lowercase()
        availableTests.filter { test ->
            val matchesCat = selectedCategoryFilter == null || RankCategory.fromTest(test) == selectedCategoryFilter
            val matchesQuery = q.isBlank() ||
                test.title.lowercase().contains(q) ||
                test.paperType.lowercase().contains(q)
            matchesCat && matchesQuery
        }
    }
    val dateValid = Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(targetDateIst.trim())

    CoolingGlassBottomSheet(
        title = if (editingTarget != null) {
            appText("Update Target Banner (Admin Manual)", "टारगेट बैनर अपडेट करें (Admin Manual)")
        } else {
            appText("Set Manual Target Banner (Admin)", "मैन्युअल टारगेट बैनर सेट करें (Admin)")
        },
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Target Day Quick Picker (Today / Tomorrow / +2 Days / Custom IST Date)
            Text(
                text = appText(
                    en = "1. Select Target Date (IST Date)",
                    defaultText = "1. किस दिन के लिए टारगेट सेट करना है? (IST Date)"
                ),
                style = MaterialTheme.typography.labelLarge,
                color = glass.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateQuickChip(
                    label = appText("Today", "आज (Today)"),
                    selected = targetDateIst == todayIst,
                    tag = "target_date_today_chip",
                    onClick = { targetDateIst = todayIst }
                )
                DateQuickChip(
                    label = appText("Tomorrow", "कल / अगला दिन (Tomorrow)"),
                    selected = targetDateIst == tomorrowIst,
                    tag = "target_date_tomorrow_chip",
                    onClick = { targetDateIst = tomorrowIst }
                )
                DateQuickChip(
                    label = appText("+2 Days", "परसों (+2 Days)"),
                    selected = targetDateIst == dayAfterIst,
                    tag = "target_date_dayafter_chip",
                    onClick = { targetDateIst = dayAfterIst }
                )
            }
            CoolingGlassTextField(
                value = targetDateIst,
                onValueChange = { targetDateIst = it },
                label = appText("IST Date (YYYY-MM-DD, e.g. $todayIst)", "IST Date (YYYY-MM-DD, जैसे $todayIst)"),
                singleLine = true
            )

            // 2. Manual Target Set Selection (Category Filter + Search + Manual Tap)
            Text(
                text = appText(
                    en = "2. Select Target Set Manually (${availableTests.size} available)",
                    defaultText = "2. टारगेट सेट मैन्युअल चुनें (${availableTests.size} उपलब्ध)"
                ),
                style = MaterialTheme.typography.labelLarge,
                color = glass.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (selectedTest != null) {
                    appText("✓ Selected Target Set: ${selectedTest.title}", "✓ चुना गया टारगेट सेट: ${selectedTest.title}")
                } else {
                    appText(
                        en = "Please manually select a target set below (Required)",
                        defaultText = "कृपया नीचे से मैन्युअल रूप से एक टारगेट सेट चुनें (अनिवार्य)"
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (selectedTest != null) glass.correct else glass.review,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("manual_target_selection_status")
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DateQuickChip(
                    label = "All",
                    selected = selectedCategoryFilter == null,
                    tag = "target_cat_all",
                    onClick = { selectedCategoryFilter = null }
                )
                RankCategory.entries.forEach { cat ->
                    DateQuickChip(
                        label = cat.displayName,
                        selected = selectedCategoryFilter == cat,
                        tag = "target_cat_${cat.name.lowercase()}",
                        onClick = { selectedCategoryFilter = cat }
                    )
                }
            }
            if (availableTests.size > 4) {
                CoolingGlassTextField(
                    value = testSearchQuery,
                    onValueChange = { testSearchQuery = it },
                    label = appText("Search Set Name", "सेट खोजें (Search Set Name)"),
                    singleLine = true
                )
            }
            if (filteredTests.isEmpty()) {
                Text(
                    text = appText(
                        en = "No sets found in this category. Select another category or create a new set in Question Bank.",
                        defaultText = "इस कैटेगरी में कोई सेट नहीं मिला। अन्य कैटेगरी चुनें या Question Bank से नया सेट बनाएं।"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.incorrect
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredTests.forEach { test ->
                        val isSelected = test.id == selectedTestId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) GlacierBlue600.copy(alpha = 0.18f)
                                    else glass.glassSurface.copy(alpha = 0.72f)
                                )
                                .border(
                                    width = if (isSelected) 1.3.dp else 0.7.dp,
                                    color = if (isSelected) GlacierBlue600 else glass.glassBorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTestId = test.id }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                                .testTag("select_target_test_${test.id}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = test.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${test.questionCount} Q • ${test.durationMinutes}m • ${test.status}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected Set",
                                    tint = GlacierBlue600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Manual Banner Headline & Target Goal
            Text(
                text = appText(
                    en = "3. Banner Title & Target Goal (Manual)",
                    defaultText = "3. बैनर पर लिखने वाला टारगेट टाइटल व लक्ष्य (Manual)"
                ),
                style = MaterialTheme.typography.labelLarge,
                color = glass.textPrimary,
                fontWeight = FontWeight.Bold
            )
            val presets = if (currentLang == LanguagePref.ENGLISH) {
                listOf(
                    "Today's Target",
                    "Today's Mega Practice Set",
                    "Today's Mission Set",
                    "Tomorrow's Special Target"
                )
            } else {
                listOf(
                    "आज का टारगेट • Aaj Ka Target",
                    "आज का महा-अभ्यास सेट",
                    "Today's Mission Set",
                    "अगले दिन का विशेष टारगेट"
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    DateQuickChip(
                        label = preset,
                        selected = bannerTitle == preset,
                        tag = "preset_${preset.hashCode()}",
                        onClick = { bannerTitle = preset }
                    )
                }
            }
            CoolingGlassTextField(
                value = bannerTitle,
                onValueChange = { bannerTitle = it.take(120) },
                label = appText("Banner Heading", "बैनर हेडिंग (Hindi / English)"),
                singleLine = true
            )
            CoolingGlassTextField(
                value = bannerSubtitle,
                onValueChange = { bannerSubtitle = it.take(240) },
                label = appText(
                    en = "Target Goal / Instructions (e.g. Target 35+ Marks • Complete by 11:59 PM)",
                    defaultText = "टारगेट लक्ष्य / निर्देश (जैसे: लक्ष्य 35+ अंक • रात 11:59 तक पूरा करें)"
                ),
                singleLine = true
            )

            // 4. Banner Photo / Animated GIF / PNG Selection + Live Poster Preview
            Text(
                text = appText("4. Choose Banner (GIF / PNG / Photo • Live Preview)", "4. फोटो / GIF / PNG बैनर चुनें (Live Preview)"),
                style = MaterialTheme.typography.labelLarge,
                color = glass.textPrimary,
                fontWeight = FontWeight.Bold
            )
            bannerUploadError?.let { errMsg ->
                CoolingGlassStatusBanner(
                    message = errMsg,
                    isError = true,
                    onDismiss = { bannerUploadError = null }
                )
            }
            DailyTargetPhotoBannerCard(
                target = DailyTargetTask(
                    id = editingTarget?.id ?: "preview",
                    testId = selectedTest?.id.orEmpty(),
                    testTitle = selectedTest?.title ?: appText("Select a target set above", "ऊपर से टारगेट सेट चुनें"),
                    targetDateIst = if (dateValid) targetDateIst.trim() else todayIst,
                    bannerTitle = bannerTitle.ifBlank { defaultBannerTitle },
                    bannerSubtitle = bannerSubtitle.ifBlank { null },
                    bannerImageBase64 = bannerImageBase64
                ),
                linkedTest = selectedTest,
                isCompleted = false,
                isMissedYesterday = false,
                isAdmin = false,
                tag = "daily_target_editor_preview",
                compact = false,
                onOpen = {},
                onEdit = {}
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoolingGlassOutlinedButton(
                    text = if (bannerImageBase64 == null) {
                        appText("Upload GIF / PNG / Photo", "GIF / PNG / फोटो चुनें")
                    } else {
                        appText("Change GIF / PNG ✓", "GIF / PNG बदलें ✓")
                    },
                    icon = Icons.Rounded.AddPhotoAlternate,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
                if (bannerImageBase64 != null) {
                    CoolingGlassOutlinedButton(
                        text = appText("Default HD", "डिफ़ॉल्ट HD"),
                        icon = Icons.Rounded.Image,
                        modifier = Modifier.weight(0.75f),
                        onClick = {
                            bannerImageBase64 = null
                            bannerUploadError = null
                        }
                    )
                }
            }

            // 5. Save & Delete Controls
            Spacer(Modifier.height(4.dp))
            CoolingGlassButton(
                text = if (editingTarget != null) {
                    appText("Save Banner Update", "बैनर अपडेट सेव करें")
                } else {
                    appText("Publish Target Banner", "टारगेट बैनर पब्लिश करें")
                },
                icon = Icons.Rounded.Flag,
                enabled = selectedTest != null && dateValid && bannerTitle.trim().length >= 2,
                testTagName = "save_daily_target_btn",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    selectedTest?.let { test ->
                        onSave(
                            editingTarget?.id,
                            test,
                            targetDateIst.trim(),
                            bannerTitle.trim(),
                            bannerSubtitle.trim().ifBlank { null },
                            bannerImageBase64
                        )
                    }
                }
            )
            if (editingTarget != null) {
                CoolingGlassOutlinedButton(
                    text = appText("Delete This Target Banner", "यह टारगेट बैनर हटाएं (Delete)"),
                    icon = Icons.Rounded.DeleteOutline,
                    testTagName = "delete_daily_target_btn",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onDelete(editingTarget.id) }
                )
            }

            // 6. Existing Targets Quick Switcher (Today / Upcoming / Yesterday)
            if (allTargets.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = appText("All Saved Target Banners (${allTargets.size})", "सभी सेव किए गए टारगेट बैनर (${allTargets.size})"),
                    style = MaterialTheme.typography.labelLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                allTargets.take(8).forEach { item ->
                    val statusLabel = when {
                        item.targetDateIst == todayIst -> "TODAY"
                        item.targetDateIst > todayIst -> "NEXT DAY"
                        else -> "PAST"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(glass.glassSurface.copy(alpha = 0.72f))
                            .clickable { onSelectExistingForEdit(item) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.targetDateIst} ($statusLabel) • ${item.displayBannerTitle(currentLang)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.testTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onDelete(item.id) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete target",
                                tint = glass.incorrect,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateQuickChip(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) GlacierBlue600 else glass.glassSurface.copy(alpha = 0.78f))
            .border(
                width = 0.9.dp,
                color = if (selected) GlacierBlue600 else glass.glassBorderSubtle,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) CrystalWhite else glass.textPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}
