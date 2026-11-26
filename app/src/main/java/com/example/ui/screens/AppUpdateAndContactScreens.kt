package com.example.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.AdminContactProfile
import com.example.data.model.AppRole
import com.example.data.model.AppUpdateRelease
import com.example.data.model.CategoryIconConfig
import com.example.data.model.GroupMember
import com.example.data.model.PaperType
import com.example.data.model.RankCategory
import com.example.data.model.UserProfile
import com.example.ui.glass.AnimatedBase64Image
import com.example.ui.glass.AutoResizeSingleLineText
import com.example.ui.glass.CoolingGlassAvatar
import com.example.ui.glass.CoolingGlassBadge
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassConfirmationPopup
import com.example.ui.glass.CoolingGlassDialog
import com.example.ui.glass.CoolingGlassEmptyState
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.CoolingGlassSearchBar
import com.example.ui.glass.CoolingGlassStatusBanner
import com.example.ui.glass.CoolingGlassTabRow
import com.example.ui.glass.CoolingGlassTextField
import com.example.ui.glass.ProfilePhotoZoomDialog
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.GlacierBlue600
import com.example.ui.theme.IceCyan500
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.SoftLavender500
import com.example.util.ApkInspectionResult
import com.example.util.ApkInspector
import com.example.util.ApkPickerHelper
import com.example.util.ImageCodec
import com.example.util.IstClock
import com.example.util.SetIconEncoder
import com.example.util.SetIconOption
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val WhatsAppGreenStart = Color(0xFF16A34A)
private val WhatsAppGreenEnd = Color(0xFF22C55E)

// =========================================================================================
// 1. SET ICON / LOGO AVATAR, 7-CATEGORY DEFAULT ICONS & ADMIN-ONLY ICON / BANNER PICKER
// =========================================================================================

/**
 * Provides the active group's Admin-configured 7-category default set icons throughout the UI
 * so every test card automatically inherits its category icon without repeated per-set uploads.
 */
val LocalCategoryIcons = compositionLocalOf<Map<String, CategoryIconConfig>> { emptyMap() }

/**
 * Resolves the effective custom image Base64 and built-in [SetIconOption] for a set or category:
 * 1. Explicit per-set custom upload (`customIconBase64`)
 * 2. Explicit per-set built-in `iconKey` (if non-blank and differs from empty)
 * 3. Admin-configured 7-category default (`categoryIcons[category.name]`)
 * 4. Built-in default icon for the paper type / category
 */
fun resolveEffectiveSetIcon(
    iconKey: String?,
    customIconBase64: String?,
    paperType: PaperType,
    categoryIcons: Map<String, CategoryIconConfig> = emptyMap(),
    rankCategoryOverride: RankCategory? = null
): Pair<String?, SetIconOption> {
    if (!customIconBase64.isNullOrBlank()) {
        return customIconBase64 to SetIconOption.resolve(iconKey, paperType)
    }
    if (!iconKey.isNullOrBlank()) {
        return null to SetIconOption.resolve(iconKey, paperType)
    }
    val category = rankCategoryOverride ?: RankCategory.fromRaw(paperType.name)
    val catConfig = category?.let { categoryIcons[it.name] }
    if (catConfig != null) {
        if (!catConfig.customIconBase64.isNullOrBlank()) {
            return catConfig.customIconBase64 to SetIconOption.resolveForCategory(catConfig.iconKey, category)
        }
        if (!catConfig.iconKey.isNullOrBlank()) {
            return null to SetIconOption.resolveForCategory(catConfig.iconKey, category)
        }
    }
    val fallback = if (rankCategoryOverride != null) {
        SetIconOption.defaultForRankCategory(rankCategoryOverride)
    } else {
        SetIconOption.defaultForPaperType(paperType)
    }
    return null to fallback
}

@Composable
fun SetIconAvatar(
    iconKey: String?,
    customIconBase64: String?,
    paperType: PaperType,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
    categoryIconsOverride: Map<String, CategoryIconConfig>? = null,
    rankCategoryOverride: RankCategory? = null
) {
    val categoryIcons = categoryIconsOverride ?: LocalCategoryIcons.current
    val (effectiveBase64, resolvedOption) = remember(iconKey, customIconBase64, paperType, categoryIcons, rankCategoryOverride) {
        resolveEffectiveSetIcon(iconKey, customIconBase64, paperType, categoryIcons, rankCategoryOverride)
    }
    val shape = RoundedCornerShape(13.dp)
    val accent = resolvedOption.accentColor()

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.10f))
                )
            )
            .border(1.2.dp, accent.copy(alpha = 0.55f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (!effectiveBase64.isNullOrBlank()) {
            AnimatedBase64Image(
                base64 = effectiveBase64,
                contentDescription = "Question set custom icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Icon(
                        imageVector = resolvedOption.icon,
                        contentDescription = resolvedOption.label,
                        tint = accent,
                        modifier = Modifier.size(size * 0.54f)
                    )
                }
            )
        } else {
            Icon(
                imageVector = resolvedOption.icon,
                contentDescription = resolvedOption.label,
                tint = accent,
                modifier = Modifier.size(size * 0.54f)
            )
        }
    }
}

@Composable
fun SetIconPickerDialog(
    paperType: PaperType,
    currentIconKey: String?,
    currentCustomIconBase64: String?,
    currentBannerImageBase64: String? = null,
    showBannerEditor: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (iconKey: String?, customIconBase64: String?) -> Unit,
    onSaveWithBanner: ((iconKey: String?, customIconBase64: String?, bannerImageBase64: String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalCoolingGlass.current
    val categoryIcons = LocalCategoryIcons.current
    val category = remember(paperType) { RankCategory.fromRaw(paperType.name) }
    val hasCategoryDefault = remember(category, categoryIcons) {
        val cfg = category?.let { categoryIcons[it.name] }
        cfg != null && (!cfg.customIconBase64.isNullOrBlank() || !cfg.iconKey.isNullOrBlank())
    }

    var selectedKey by remember(currentIconKey, paperType) {
        mutableStateOf(currentIconKey?.takeIf { it.isNotBlank() })
    }
    var selectedCustomBase64 by remember(currentCustomIconBase64) {
        mutableStateOf(currentCustomIconBase64?.takeIf { it.isNotBlank() })
    }
    var selectedBannerBase64 by remember(currentBannerImageBase64) {
        mutableStateOf(currentBannerImageBase64?.takeIf { it.isNotBlank() })
    }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    SetIconEncoder.encodeSquareIconFromUri(context, uri)
                }
                res.onSuccess { b64 ->
                    selectedCustomBase64 = b64
                    selectedKey = null
                    uploadError = null
                }.onFailure { err ->
                    uploadError = err.message ?: "Invalid image file."
                }
            }
        }
    }

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    ImageCodec.encodeUriToBase64Result(
                        context = context,
                        uri = uri,
                        maxDimension = 720,
                        quality = 66,
                        allowGif = true,
                        maxBase64Len = ImageCodec.DEFAULT_MAX_BANNER_BASE64_LEN
                    )
                }
                res.onSuccess { b64 ->
                    selectedBannerBase64 = b64
                    uploadError = null
                }.onFailure { err ->
                    uploadError = err.message ?: "Invalid banner file."
                }
            }
        }
    }

    CoolingGlassDialog(
        title = if (showBannerEditor) "Set Icon & GIF/PNG Banner" else "Set Icon / Logo (Admin Only)",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Preview Card
            CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SetIconAvatar(
                        iconKey = selectedKey,
                        customIconBase64 = selectedCustomBase64,
                        paperType = paperType,
                        size = 54.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                !selectedCustomBase64.isNullOrBlank() && ImageCodec.isGifBase64(selectedCustomBase64) -> "Custom Animated GIF Icon"
                                !selectedCustomBase64.isNullOrBlank() && ImageCodec.isPngBase64(selectedCustomBase64) -> "Custom PNG Icon"
                                !selectedCustomBase64.isNullOrBlank() -> "Custom Uploaded Icon"
                                !selectedKey.isNullOrBlank() -> SetIconOption.resolve(selectedKey, paperType).label
                                hasCategoryDefault -> "Category Default (${category?.displayName ?: "Auto"})"
                                else -> SetIconOption.defaultForPaperType(paperType).label
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Supports live GIF, transparent PNG, JPG or Category Default.",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                    }
                }
            }

            // Custom Upload & Reset to Category Default Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoolingGlassOutlinedButton(
                    text = "GIF / PNG / JPG",
                    icon = Icons.Rounded.AddPhotoAlternate,
                    testTagName = "upload_custom_set_icon_btn",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
                CoolingGlassOutlinedButton(
                    text = if (hasCategoryDefault) "Category Default" else "Reset Default",
                    icon = Icons.Rounded.RestartAlt,
                    tint = SoftLavender500,
                    testTagName = "reset_default_set_icon_btn",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedKey = null
                        selectedCustomBase64 = null
                        uploadError = null
                    }
                )
            }

            if (showBannerEditor) {
                CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Set Banner (Live GIF / PNG / JPG)",
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.accentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (ImageCodec.isGifBase64(selectedBannerBase64)) {
                            CoolingGlassBadge(text = "LIVE GIF", color = glass.correct)
                        } else if (ImageCodec.isPngBase64(selectedBannerBase64)) {
                            CoolingGlassBadge(text = "PNG", color = IceCyan500)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (!selectedBannerBase64.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 6.2f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, glass.accentCyan.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        ) {
                            AnimatedBase64Image(
                                base64 = selectedBannerBase64,
                                contentDescription = "Set Banner Live Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoolingGlassOutlinedButton(
                            text = if (selectedBannerBase64.isNullOrBlank()) "Upload Banner GIF/PNG" else "Change Banner",
                            icon = Icons.Rounded.AddPhotoAlternate,
                            testTagName = "upload_set_banner_dialog_btn",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                        if (!selectedBannerBase64.isNullOrBlank()) {
                            CoolingGlassOutlinedButton(
                                text = "Remove",
                                icon = Icons.Rounded.DeleteOutline,
                                tint = glass.incorrect,
                                testTagName = "remove_set_banner_dialog_btn",
                                modifier = Modifier.weight(0.65f),
                                onClick = { selectedBannerBase64 = null }
                            )
                        }
                    }
                }
            }

            uploadError?.takeIf { it.isNotBlank() }?.let { err ->
                CoolingGlassStatusBanner(message = err, isError = true)
            }

            Text(
                text = "Built-in Exam Icon Library (10 Icons)",
                style = MaterialTheme.typography.labelLarge,
                color = glass.accentPrimary,
                fontWeight = FontWeight.Bold
            )

            // 2-column fixed-ratio grid of all 10 built-in icons
            SetIconOption.entries.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { option ->
                        val picked = selectedCustomBase64.isNullOrBlank() && selectedKey == option.key
                        val accent = option.accentColor()
                        val tileShape = RoundedCornerShape(13.dp)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("set_icon_opt_${option.key.lowercase()}")
                                .clip(tileShape)
                                .background(if (picked) accent.copy(alpha = 0.18f) else glass.glassSurfaceStrong)
                                .border(
                                    width = if (picked) 1.6.dp else 1.dp,
                                    color = if (picked) accent else glass.glassBorder,
                                    shape = tileShape
                                )
                                .clickable {
                                    selectedKey = option.key
                                    selectedCustomBase64 = null
                                    uploadError = null
                                }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = accent,
                                modifier = Modifier.size(19.dp)
                            )
                            AutoResizeSingleLineText(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxFontSize = 11.sp,
                                minFontSize = 8.5.sp,
                                color = glass.textPrimary,
                                fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            CoolingGlassButton(
                text = if (showBannerEditor) "Save Icon & Banner" else "Save Set Icon",
                icon = Icons.Rounded.CheckCircle,
                testTagName = "save_set_icon_btn",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (showBannerEditor && onSaveWithBanner != null) {
                        onSaveWithBanner(selectedKey, selectedCustomBase64, selectedBannerBase64)
                    } else {
                        onSave(selectedKey, selectedCustomBase64)
                    }
                    onDismiss()
                }
            )
        }
    }
}

/**
 * Admin Control Center Dialog for configuring default icons (GIF / PNG / JPG / Built-in)
 * ONCE for each of the 7 official TRE 4.0 test categories.
 */
@Composable
fun AdminCategoryDefaultIconsDialog(
    categoryIcons: Map<String, CategoryIconConfig>,
    busy: Boolean,
    onSaveCategoryIcon: (category: RankCategory, iconKey: String?, customIconBase64: String?) -> Unit,
    onResetCategoryIcon: (category: RankCategory) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalCoolingGlass.current

    var activeUploadCategory by remember { mutableStateOf<RankCategory?>(null) }
    var expandedBuiltInCategory by remember { mutableStateOf<RankCategory?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val targetCat = activeUploadCategory
        if (uri != null && targetCat != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    SetIconEncoder.encodeSquareIconFromUri(context, uri)
                }
                res.onSuccess { b64 ->
                    errorMsg = null
                    onSaveCategoryIcon(targetCat, null, b64)
                }.onFailure { err ->
                    errorMsg = err.message ?: "Unable to upload category icon."
                }
            }
        }
    }

    CoolingGlassDialog(
        title = "7-Category Default Set Icons",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .testTag("admin_category_default_icons_dialog"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Set an icon (GIF, PNG, JPG, or Built-in) once per category. Every test set in that category automatically shows this icon unless overridden.",
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary
            )

            errorMsg?.let { err ->
                CoolingGlassStatusBanner(message = err, isError = true)
            }

            RankCategory.entries.forEach { cat ->
                val cfg = categoryIcons[cat.name]
                val isCustomUpload = !cfg?.customIconBase64.isNullOrBlank()
                val isCustomBuiltIn = !isCustomUpload && !cfg?.iconKey.isNullOrBlank()
                val isExpanded = expandedBuiltInCategory == cat

                val statusLabel = when {
                    isCustomUpload && ImageCodec.isGifBase64(cfg?.customIconBase64) -> "LIVE GIF"
                    isCustomUpload && ImageCodec.isPngBase64(cfg?.customIconBase64) -> "PNG Icon"
                    isCustomUpload -> "Custom"
                    isCustomBuiltIn -> SetIconOption.resolveForCategory(cfg?.iconKey, cat).label.substringBefore(" /")
                    else -> "Default"
                }
                val statusColor = when {
                    isCustomUpload -> glass.correct
                    isCustomBuiltIn -> IceCyan500
                    else -> glass.textMuted
                }

                CoolingGlassCard(
                    modifier = Modifier.testTag("cat_icon_row_${cat.name.lowercase()}"),
                    strongFrost = isCustomUpload || isCustomBuiltIn,
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SetIconAvatar(
                            iconKey = null,
                            customIconBase64 = null,
                            paperType = PaperType.COMMON_GS,
                            size = 48.dp,
                            categoryIconsOverride = categoryIcons,
                            rankCategoryOverride = cat
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            AutoResizeSingleLineText(
                                text = cat.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                maxFontSize = 13.5.sp,
                                minFontSize = 10.sp,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(3.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoolingGlassBadge(text = cat.shortChip, color = glass.accentPrimary)
                                CoolingGlassBadge(text = statusLabel, color = statusColor)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CoolingGlassOutlinedButton(
                            text = "Upload GIF/PNG",
                            icon = Icons.Rounded.AddPhotoAlternate,
                            enabled = !busy,
                            testTagName = "upload_cat_icon_${cat.name.lowercase()}",
                            modifier = Modifier.weight(1.25f),
                            onClick = {
                                activeUploadCategory = cat
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                        CoolingGlassOutlinedButton(
                            text = if (isExpanded) "Hide" else "Icons",
                            icon = Icons.Rounded.CheckCircle,
                            tint = SoftLavender500,
                            enabled = !busy,
                            modifier = Modifier.weight(0.8f),
                            onClick = {
                                expandedBuiltInCategory = if (isExpanded) null else cat
                            }
                        )
                        if (isCustomUpload || isCustomBuiltIn) {
                            CoolingGlassOutlinedButton(
                                text = "Reset",
                                icon = Icons.Rounded.RestartAlt,
                                tint = glass.incorrect,
                                enabled = !busy,
                                testTagName = "reset_cat_icon_${cat.name.lowercase()}",
                                modifier = Modifier.weight(0.75f),
                                onClick = {
                                    errorMsg = null
                                    onResetCategoryIcon(cat)
                                }
                            )
                        }
                    }

                    if (isExpanded) {
                        Spacer(Modifier.height(8.dp))
                        SetIconOption.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                pair.forEach { opt ->
                                    val picked = !isCustomUpload && cfg?.iconKey == opt.key
                                    val accent = opt.accentColor()
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (picked) accent.copy(alpha = 0.18f) else glass.glassSurface)
                                            .border(
                                                width = if (picked) 1.4.dp else 0.8.dp,
                                                color = if (picked) accent else glass.glassBorder,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable(enabled = !busy) {
                                                expandedBuiltInCategory = null
                                                onSaveCategoryIcon(cat, opt.key, null)
                                            }
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(opt.icon, contentDescription = opt.label, tint = accent, modifier = Modifier.size(16.dp))
                                        AutoResizeSingleLineText(
                                            text = opt.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxFontSize = 10.5.sp,
                                            minFontSize = 8.sp,
                                            color = glass.textPrimary,
                                            fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================================
// 2. USER -> ADMIN OFFICIAL WHATSAPP CONTACT SCREEN
// =========================================================================================

@Composable
fun ContactAdminScreen(
    adminContact: AdminContactProfile?,
    fallbackAdminName: String,
    groupName: String,
    onFeedback: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val glass = LocalCoolingGlass.current
    val adminName = adminContact?.adminName?.takeIf { it.isNotBlank() } ?: fallbackAdminName.ifBlank { "Gopal Gupta (Admin)" }
    val normalizedPhone = remember(adminContact?.adminWhatsAppNumber) {
        WhatsAppHelper.normalizePhoneNumber(adminContact?.adminWhatsAppNumber)
    }
    var messageDraft by remember { mutableStateOf("Hello Admin, I have a query regarding BPSSC TRE 4.0.") }
    var showAdminAvatarZoom by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("contact_admin_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CoolingGlassCard(strongFrost = true, tintColor = WhatsAppGreenStart, borderColor = WhatsAppGreenStart.copy(alpha = 0.55f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CoolingGlassAvatar(
                        name = adminName,
                        photoBase64 = adminContact?.adminPhotoBase64,
                        size = 64.dp,
                        onClick = { showAdminAvatarZoom = true }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        CoolingGlassBadge("OFFICIAL ADMIN CONTACT", WhatsAppGreenStart)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = adminName,
                            style = MaterialTheme.typography.titleLarge,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = groupName.ifBlank { "BPSSC TRE 4.0 Aspirant Group" },
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(glass.glassSurfaceStrong)
                        .border(1.dp, glass.glassBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = "Admin WhatsApp",
                        tint = if (normalizedPhone != null) WhatsAppGreenStart else glass.textMuted
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Official WhatsApp Number",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textMuted
                        )
                        Text(
                            text = normalizedPhone ?: "WhatsApp Number Not Available",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (normalizedPhone != null) glass.textPrimary else glass.textMuted,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("admin_contact_phone_value")
                        )
                    }
                }

                if (normalizedPhone != null) {
                    Spacer(Modifier.height(12.dp))
                    CoolingGlassTextField(
                        value = messageDraft,
                        onValueChange = { messageDraft = it },
                        label = "Optional Pre-filled Message",
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4
                    )
                    Spacer(Modifier.height(12.dp))
                    CoolingGlassButton(
                        text = "Message Admin on WhatsApp",
                        icon = Icons.AutoMirrored.Rounded.Chat,
                        accentGradient = listOf(WhatsAppGreenStart, WhatsAppGreenEnd),
                        testTagName = "contact_admin_whatsapp_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val opened = WhatsAppHelper.openWhatsAppChat(
                                context = context,
                                phoneNumber = normalizedPhone,
                                prefillMessage = messageDraft
                            )
                            if (!opened) {
                                onFeedback("Could not launch WhatsApp. Please verify WhatsApp or a browser is installed.", true)
                            }
                        }
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                    CoolingGlassStatusBanner(
                        message = "Admin has not added an official WhatsApp number yet. Please check back later or use the Doubt Session.",
                        isError = false
                    )
                }
            }
        }

        item {
            CoolingGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = glass.accentPrimary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strict Privacy Guarantee",
                            style = MaterialTheme.typography.titleSmall,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Only Official Admin Contact is visible here. Regular Users can never view another User's profile, email, or WhatsApp number.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }
            }
        }
    }

    if (showAdminAvatarZoom) {
        ProfilePhotoZoomDialog(
            name = adminName,
            photoBase64 = adminContact?.adminPhotoBase64,
            subtitle = "Official Group Admin • ${groupName.ifBlank { "BPSSC TRE 4.0" }}",
            onDismiss = { showAdminAvatarZoom = false }
        )
    }
}

// =========================================================================================
// 3. ADMIN CONTACT SETTINGS + ADMIN MANAGE USERS (ADMIN ONLY)
// =========================================================================================

@Composable
fun AdminContactSettingsCard(
    adminContact: AdminContactProfile?,
    currentMember: GroupMember?,
    busy: Boolean,
    onSave: (adminName: String, adminPhotoBase64: String?, adminWhatsAppNumber: String?) -> Unit
) {
    val glass = LocalCoolingGlass.current
    var nameInput by remember(adminContact?.adminName, currentMember?.displayName) {
        mutableStateOf(adminContact?.adminName?.takeIf { it.isNotBlank() } ?: currentMember?.displayName ?: "Gopal Gupta")
    }
    var phoneInput by remember(adminContact?.adminWhatsAppNumber) {
        mutableStateOf(adminContact?.adminWhatsAppNumber.orEmpty())
    }
    val phoneValid = WhatsAppHelper.isValidOptionalInput(phoneInput)

    CoolingGlassCard(strongFrost = true, tintColor = WhatsAppGreenStart) {
        Text(
            text = "Official Admin WhatsApp Contact",
            style = MaterialTheme.typography.titleMedium,
            color = glass.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Visible on the 'Contact Admin' screen for all group Users. Leave blank to hide WhatsApp messaging.",
            style = MaterialTheme.typography.bodySmall,
            color = glass.textSecondary
        )
        Spacer(Modifier.height(10.dp))
        CoolingGlassTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = "Official Admin Display Name *",
            testTagName = "admin_contact_name_input"
        )
        Spacer(Modifier.height(8.dp))
        CoolingGlassTextField(
            value = phoneInput,
            onValueChange = { phoneInput = it },
            label = "Admin WhatsApp Number (e.g. +91 9876543210)",
            placeholder = "+91 98765 43210 (leave blank to clear)",
            leadingIcon = Icons.Rounded.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            testTagName = "admin_contact_whatsapp_input"
        )
        if (!phoneValid) {
            Spacer(Modifier.height(6.dp))
            CoolingGlassStatusBanner(
                message = "Enter a valid 10-digit Indian number or E.164 international number (e.g. +919876543210).",
                isError = true
            )
        }
        Spacer(Modifier.height(10.dp))
        CoolingGlassButton(
            text = "Save Official Admin Contact",
            icon = Icons.Rounded.CheckCircle,
            loading = busy,
            enabled = nameInput.trim().length >= 2 && phoneValid && !busy,
            accentGradient = listOf(WhatsAppGreenStart, WhatsAppGreenEnd),
            testTagName = "save_admin_contact_btn",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    nameInput.trim(),
                    adminContact?.adminPhotoBase64 ?: currentMember?.photoBase64,
                    phoneInput.trim().takeIf { it.isNotBlank() }
                )
            }
        )
    }
}

@Composable
fun AdminManageUsersSection(
    userProfiles: List<UserProfile>,
    groupMembers: List<GroupMember>,
    canAssignRoles: Boolean,
    currentUserId: String,
    onAssignRole: (targetUid: String, newRole: AppRole) -> Unit,
    onOpenUserDetail: (UserProfile, GroupMember?) -> Unit
) {
    val glass = LocalCoolingGlass.current
    var searchQuery by remember { mutableStateOf("") }

    val memberByUid = remember(groupMembers) { groupMembers.associateBy { it.userId } }
    val profileByUid = remember(userProfiles) {
        userProfiles.associateBy { it.userId.ifBlank { it.id } }
    }

    // Combine all registered group members with their private profile (WhatsApp, registration date)
    val mergedUsers = remember(groupMembers, userProfiles, searchQuery) {
        val allUids = (groupMembers.map { it.userId } + userProfiles.map { it.userId.ifBlank { it.id } })
            .filter { it.isNotBlank() }
            .distinct()
        val items = allUids.map { uid ->
            val member = memberByUid[uid]
            val profile = profileByUid[uid] ?: UserProfile(
                id = uid,
                userId = uid,
                displayName = member?.displayName ?: "User",
                photoBase64 = member?.photoBase64,
                createdAt = member?.joinedAt
            )
            profile to member
        }.sortedByDescending { (p, m) ->
            p.createdAt?.toDate()?.time ?: m?.joinedAt?.toDate()?.time ?: 0L
        }

        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) items
        else items.filter { (p, m) ->
            val resolvedId = p.userId.ifBlank { p.id }
            p.displayName.lowercase().contains(q) ||
                resolvedId.lowercase().contains(q) ||
                (m?.displayName?.lowercase()?.contains(q) == true) ||
                (p.whatsappNumber?.contains(q) == true)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CoolingGlassSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Search Users by Name or User ID..."
        )
        Text(
            text = "Registered Group Users (${mergedUsers.size}) • Tap any User to view profile & WhatsApp",
            style = MaterialTheme.typography.labelMedium,
            color = glass.textSecondary,
            fontWeight = FontWeight.SemiBold
        )

        if (mergedUsers.isEmpty()) {
            CoolingGlassEmptyState(
                title = "No Matching Users",
                subtitle = "Try a different name or User ID.",
                icon = Icons.Rounded.Person
            )
        } else {
            mergedUsers.forEach { (profile, member) ->
                val role = member?.appRole() ?: AppRole.ASPIRANT
                val hasWhatsApp = !WhatsAppHelper.normalizePhoneNumber(profile.whatsappNumber).isNullOrBlank()
                CoolingGlassCard(
                    contentPadding = PaddingValues(12.dp),
                    onClick = { onOpenUserDetail(profile, member) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CoolingGlassAvatar(
                            name = profile.displayName,
                            photoBase64 = profile.photoBase64 ?: member?.photoBase64,
                            size = 44.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.displayName.ifBlank { "User" },
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val resolvedUid = profile.userId.ifBlank { profile.id }
                            Text(
                                text = "ID: ${resolvedUid.take(10)}... • ${if (hasWhatsApp) "WhatsApp Linked" else "No WhatsApp"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasWhatsApp) WhatsAppGreenStart else glass.textMuted
                            )
                        }
                        CoolingGlassBadge(
                            text = role.labelEn,
                            color = if (role.isAdminRole()) SoftLavender500 else glass.accentPrimary
                        )
                    }

                    if (canAssignRoles && member != null && member.userId != currentUserId && role != AppRole.SUPER_ADMIN) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CoolingGlassOutlinedButton(
                                text = "View Profile",
                                icon = Icons.Rounded.Person,
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenUserDetail(profile, member) }
                            )
                            val targetNext = if (role == AppRole.GROUP_ADMIN) AppRole.ASPIRANT else AppRole.GROUP_ADMIN
                            CoolingGlassOutlinedButton(
                                text = if (targetNext == AppRole.GROUP_ADMIN) "Make Admin" else "Set as User",
                                icon = Icons.Rounded.AdminPanelSettings,
                                tint = if (targetNext == AppRole.GROUP_ADMIN) SoftLavender500 else glass.review,
                                modifier = Modifier.weight(1f),
                                onClick = { onAssignRole(member.userId, targetNext) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUserProfileDetailDialog(
    user: UserProfile,
    member: GroupMember?,
    onDismiss: () -> Unit,
    onFeedback: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val glass = LocalCoolingGlass.current
    val role = member?.appRole() ?: AppRole.ASPIRANT
    val normalizedPhone = remember(user.whatsappNumber) {
        WhatsAppHelper.normalizePhoneNumber(user.whatsappNumber)
    }
    val regDate = remember(user.createdAt, member?.joinedAt) {
        val dt = user.createdAt?.toDate() ?: member?.joinedAt?.toDate()
        if (dt != null) IstClock.formatDate(dt, "dd MMM yyyy, hh:mm a 'IST'")
        else "Not recorded"
    }
    val resolvedUid = user.userId.ifBlank { user.id }

    CoolingGlassDialog(title = "User Profile (Admin View)", onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoolingGlassAvatar(
                    name = user.displayName,
                    photoBase64 = user.photoBase64 ?: member?.photoBase64,
                    size = 58.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName.ifBlank { "User" },
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    CoolingGlassBadge(
                        text = role.labelEn,
                        color = if (role.isAdminRole()) SoftLavender500 else glass.accentPrimary
                    )
                }
            }

            CoolingGlassCard(strongFrost = true, contentPadding = PaddingValues(12.dp)) {
                Text("User ID", style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
                Text(resolvedUid, style = MaterialTheme.typography.bodySmall, color = glass.textPrimary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("Registration Date", style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
                Text(regDate, style = MaterialTheme.typography.bodySmall, color = glass.textPrimary, fontWeight = FontWeight.Medium)
                if (!user.email.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Email", style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = glass.textPrimary)
                }
                Spacer(Modifier.height(8.dp))
                Text("WhatsApp Number", style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
                Text(
                    text = normalizedPhone ?: "WhatsApp Number Not Available",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (normalizedPhone != null) WhatsAppGreenStart else glass.textMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("admin_user_detail_whatsapp")
                )
            }

            if (normalizedPhone != null) {
                CoolingGlassButton(
                    text = "Message User on WhatsApp",
                    icon = Icons.AutoMirrored.Rounded.Chat,
                    accentGradient = listOf(WhatsAppGreenStart, WhatsAppGreenEnd),
                    testTagName = "admin_message_user_whatsapp_btn",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val ok = WhatsAppHelper.openWhatsAppChat(
                            context = context,
                            phoneNumber = normalizedPhone,
                            prefillMessage = "Hello ${user.displayName}, regarding BPSSC TRE 4.0:"
                        )
                        if (!ok) onFeedback("Could not open WhatsApp on this device.", true)
                    }
                )
            }
        }
    }
}

// =========================================================================================
// 4. IN-APP UPDATE MANAGEMENT (ADMIN CONSOLE + USER CHECK/DOWNLOAD)
// =========================================================================================

@Composable
fun AdminAppUpdateManagementSection(
    updates: List<AppUpdateRelease>,
    busy: Boolean,
    onPublish: (
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
    ) -> Unit,
    onTogglePublished: (updateId: String, published: Boolean) -> Unit,
    onDelete: (updateId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val glass = LocalCoolingGlass.current

    var versionName by remember { mutableStateOf("1.4.0") }
    var versionCodeStr by remember { mutableStateOf((BuildConfig.VERSION_CODE + 1).toString()) }
    var minSupportedStr by remember { mutableStateOf(BuildConfig.VERSION_CODE.toString()) }
    var title by remember { mutableStateOf("BPSSC TRE 4.0 New Release") }
    var releaseNotes by remember { mutableStateOf("• Improved Question Bank validation\n• Performance and stability fixes") }
    var downloadUrl by remember { mutableStateOf("") }
    var updateTypeIdx by remember { mutableIntStateOf(0) } // 0 = NORMAL, 1 = MANDATORY
    var inspectingApk by remember { mutableStateOf(false) }
    var apkInspection by remember { mutableStateOf<ApkInspectionResult?>(null) }
    var pickerLaunchError by remember { mutableStateOf<String?>(null) }
    var copiedShaFeedback by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<AppUpdateRelease?>(null) }

    val apkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = ApkPickerHelper.extractUriFromResult(activityResult.data)
        if (uri == null) {
            pickerLaunchError = "No file was returned by the file picker. Please try again."
            return@rememberLauncherForActivityResult
        }
        pickerLaunchError = null
        copiedShaFeedback = false
        inspectingApk = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                ApkInspector.inspectPickedApk(
                    context = context,
                    uri = uri,
                    currentPackageName = context.packageName,
                    currentVersionCode = BuildConfig.VERSION_CODE.toLong()
                )
            }
            inspectingApk = false
            apkInspection = result
            if (result.validationError == null) {
                if (result.versionName.isNotBlank()) versionName = result.versionName
                if (result.versionCode > 0L) versionCodeStr = result.versionCode.toString()
            }
        }
    }

    val parsedVersionCode = versionCodeStr.toLongOrNull() ?: 0L
    val parsedMinCode = minSupportedStr.toLongOrNull() ?: 0L
    val urlValid = downloadUrl.trim().startsWith("https://", ignoreCase = true)
    val versionValid = parsedVersionCode > BuildConfig.VERSION_CODE.toLong()
    val apkHasBlockingError = apkInspection?.validationError != null
    val canPublish = !busy &&
        versionName.trim().isNotBlank() &&
        versionValid &&
        title.trim().length >= 3 &&
        releaseNotes.trim().length >= 5 &&
        urlValid &&
        !apkHasBlockingError

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CoolingGlassCard(strongFrost = true, tintColor = GlacierBlue600) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = glass.accentPrimary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Publish App Update (Admin Only)",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Installed App: v${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            CoolingGlassOutlinedButton(
                text = if (inspectingApk) "Verifying APK Signature & SHA-256..." else "Pick & Verify Signed .APK (SHA-256)",
                icon = Icons.Rounded.UploadFile,
                enabled = !inspectingApk,
                testTagName = "admin_pick_apk_btn",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    pickerLaunchError = null
                    try {
                        apkPicker.launch(ApkPickerHelper.createApkPickerChooser())
                    } catch (_: ActivityNotFoundException) {
                        pickerLaunchError = "No compatible file manager found on this device to pick an .apk file."
                    } catch (e: Exception) {
                        pickerLaunchError = "Could not open file picker: ${e.message ?: "Unknown error"}"
                    }
                }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Browse Internal Storage, Downloads, or Documents to select any signed .apk release.",
                style = MaterialTheme.typography.labelSmall,
                color = glass.textMuted
            )

            pickerLaunchError?.takeIf { it.isNotBlank() }?.let { err ->
                Spacer(Modifier.height(8.dp))
                CoolingGlassStatusBanner(message = err, isError = true)
            }

            if (inspectingApk) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Inspecting APK package signature & SHA-256...",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary
                    )
                }
            }

            apkInspection?.let { insp ->
                Spacer(Modifier.height(10.dp))
                if (insp.validationError != null) {
                    CoolingGlassCard(
                        tintColor = glass.incorrect,
                        borderColor = glass.incorrect.copy(alpha = 0.65f),
                        contentPadding = PaddingValues(12.dp),
                        modifier = Modifier.testTag("apk_inspection_error_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoolingGlassBadge("INVALID APK FILE", glass.incorrect)
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.incorrect,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .testTag("clear_apk_inspection_btn")
                                    .clickable { apkInspection = null }
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = insp.validationError,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "File: ${insp.fileName} • Size: ${ApkInspector.formatFileSize(insp.fileSizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                        if (insp.sha256Checksum.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "SHA-256: ${insp.sha256Checksum}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = glass.textMuted
                            )
                        }
                    }
                } else {
                    CoolingGlassCard(
                        tintColor = glass.correct,
                        borderColor = glass.correct.copy(alpha = 0.65f),
                        contentPadding = PaddingValues(12.dp),
                        modifier = Modifier.testTag("apk_inspection_result_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoolingGlassBadge("SIGNED APK VERIFIED", glass.correct)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .testTag("copy_apk_sha256_btn")
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            clipboard.setText(AnnotatedString(insp.sha256Checksum))
                                            copiedShaFeedback = true
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy SHA-256 checksum",
                                        tint = glass.accentPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (copiedShaFeedback) "Copied!" else "Copy SHA-256",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = glass.accentPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear selected APK",
                                    tint = glass.textMuted,
                                    modifier = Modifier
                                        .testTag("clear_apk_inspection_btn")
                                        .clip(CircleShape)
                                        .clickable { apkInspection = null }
                                        .padding(4.dp)
                                        .size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = insp.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Size: ${ApkInspector.formatFileSize(insp.fileSizeBytes)} • v${insp.versionName} (Code ${insp.versionCode})",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                        Text(
                            text = "Package: ${insp.packageName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                        Text(
                            text = "Signature: ${insp.signingSchemeInfo}",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.correct,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!insp.signerCertSha256.isNullOrBlank()) {
                            Text(
                                text = "Signer Cert: ${insp.signerCertSha256.take(35)}...",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = glass.textMuted
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "SHA-256 Checksum:",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = insp.sha256Checksum,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = glass.textPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoolingGlassTextField(
                    value = versionName,
                    onValueChange = { versionName = it },
                    label = "Version Name *",
                    placeholder = "1.2.0",
                    modifier = Modifier.weight(1f),
                    testTagName = "update_version_name_input"
                )
                CoolingGlassTextField(
                    value = versionCodeStr,
                    onValueChange = { versionCodeStr = it.filter { ch -> ch.isDigit() } },
                    label = "Version Code *",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    testTagName = "update_version_code_input"
                )
            }
            if (!versionValid && versionCodeStr.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Version Code must be greater than current (${BuildConfig.VERSION_CODE}).",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.incorrect
                )
            }

            Spacer(Modifier.height(8.dp))
            CoolingGlassTextField(
                value = downloadUrl,
                onValueChange = { downloadUrl = it },
                label = "HTTPS Download / Release URL *",
                placeholder = "https://play.google.com/... or hosted release link",
                testTagName = "update_download_url_input"
            )
            Spacer(Modifier.height(8.dp))
            CoolingGlassTextField(
                value = title,
                onValueChange = { title = it },
                label = "Update Title *"
            )
            Spacer(Modifier.height(8.dp))
            CoolingGlassTextField(
                value = releaseNotes,
                onValueChange = { releaseNotes = it },
                label = "Release Notes / What's New *",
                singleLine = false,
                minLines = 3,
                maxLines = 6
            )
            Spacer(Modifier.height(8.dp))
            CoolingGlassTabRow(
                tabs = listOf("Normal Update", "Mandatory Update"),
                selectedIndex = updateTypeIdx,
                onSelect = { updateTypeIdx = it }
            )
            if (updateTypeIdx == 1) {
                Spacer(Modifier.height(8.dp))
                CoolingGlassTextField(
                    value = minSupportedStr,
                    onValueChange = { minSupportedStr = it.filter { ch -> ch.isDigit() } },
                    label = "Min Supported Version Code",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(Modifier.height(10.dp))
            CoolingGlassButton(
                text = "Publish App Update & Notify Users",
                icon = Icons.Rounded.Publish,
                loading = busy,
                enabled = canPublish,
                testTagName = "publish_app_update_btn",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val insp = apkInspection
                    onPublish(
                        versionName.trim(),
                        parsedVersionCode,
                        title.trim(),
                        releaseNotes.trim(),
                        if (updateTypeIdx == 1) "MANDATORY" else "NORMAL",
                        if (updateTypeIdx == 1) parsedMinCode.coerceAtLeast(parsedVersionCode) else parsedMinCode,
                        downloadUrl.trim(),
                        insp?.fileSizeBytes ?: 0L,
                        insp?.sha256Checksum?.takeIf { it.isNotBlank() },
                        insp?.packageName?.takeIf { it.isNotBlank() } ?: context.packageName
                    )
                    downloadUrl = ""
                    apkInspection = null
                }
            )
        }

        // Previous Published Versions List
        Text(
            text = "Version Release History (${updates.size})",
            style = MaterialTheme.typography.titleSmall,
            color = glass.textPrimary,
            fontWeight = FontWeight.Bold
        )
        if (updates.isEmpty()) {
            CoolingGlassEmptyState(
                title = "No App Updates Published Yet",
                subtitle = "Publish a verified HTTPS release above to notify all Users.",
                icon = Icons.Rounded.Android
            )
        } else {
            val fmt = remember { IstClock.newDateFormat("dd MMM yyyy") }
            val activeReleaseId = remember(updates) {
                updates.firstOrNull { it.isActive && it.published }?.id
                    ?: updates.firstOrNull { it.isActive }?.id
                    ?: updates.filter { it.published }.maxByOrNull { it.versionCode }?.id
            }
            updates.forEach { rel ->
                val isActiveRelease = rel.id == activeReleaseId
                CoolingGlassCard(contentPadding = PaddingValues(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "v${rel.versionName} (Code ${rel.versionCode}) • ${rel.title}",
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${rel.updateType} • ${ApkInspector.formatFileSize(rel.fileSizeBytes)} • ${rel.createdAt?.toDate()?.let { fmt.format(it) } ?: "Synced"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textMuted
                            )
                        }
                        CoolingGlassBadge(
                            text = when {
                                isActiveRelease && rel.published -> "ACTIVE LIVE"
                                rel.published -> "LIVE"
                                else -> "DISABLED"
                            },
                            color = if (rel.published) glass.correct else glass.incorrect
                        )
                    }
                    if (!rel.sha256Checksum.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "SHA-256: ${rel.sha256Checksum.take(24)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoolingGlassOutlinedButton(
                            text = if (rel.published) "Disable Update" else "Re-Enable",
                            icon = if (rel.published) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            tint = if (rel.published) glass.review else glass.correct,
                            modifier = Modifier.weight(1f),
                            onClick = { onTogglePublished(rel.id, !rel.published) }
                        )
                        if (!isActiveRelease) {
                            CoolingGlassOutlinedButton(
                                text = "Delete",
                                icon = Icons.Rounded.DeleteOutline,
                                tint = glass.incorrect,
                                testTagName = "delete_app_update_${rel.id}",
                                modifier = Modifier.weight(0.9f),
                                onClick = { deleteTarget = rel }
                            )
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        CoolingGlassConfirmationPopup(
            title = "Delete Release History?",
            message = "Permanently remove version v${target.versionName} (Code ${target.versionCode}) from version release history?",
            confirmText = "Delete",
            confirmTestTag = "confirm_delete_app_update_btn",
            isDestructive = true,
            onConfirm = {
                val idToDelete = target.id
                deleteTarget = null
                onDelete(idToDelete)
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
fun UserAppUpdateScreen(
    latestUpdate: AppUpdateRelease?,
    allPublishedUpdates: List<AppUpdateRelease>,
    onFeedback: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val glass = LocalCoolingGlass.current
    val currentVerName = BuildConfig.VERSION_NAME
    val currentVerCode = BuildConfig.VERSION_CODE.toLong()
    val hasUpdate = latestUpdate != null && latestUpdate.versionCode > currentVerCode

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_app_update_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CoolingGlassCard(
                strongFrost = true,
                tintColor = if (hasUpdate) GlacierBlue600 else glass.correct,
                borderColor = if (hasUpdate) IceCyan500 else glass.correct
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background((if (hasUpdate) GlacierBlue600 else glass.correct).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasUpdate) Icons.Rounded.SystemUpdate else Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = if (hasUpdate) glass.accentPrimary else glass.correct,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasUpdate) "Update Available" else "Up to Date",
                            style = MaterialTheme.typography.titleLarge,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Current Installed: v$currentVerName (Build $currentVerCode)",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                if (!hasUpdate) {
                    CoolingGlassStatusBanner(
                        message = "You are using the latest version.",
                        isError = false
                    )
                } else {
                    val upd = latestUpdate
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CoolingGlassBadge("Latest: v${upd.versionName}", glass.accentPrimary)
                        CoolingGlassBadge("Build ${upd.versionCode}", SoftLavender500)
                        if (upd.isMandatoryFor(currentVerCode)) {
                            CoolingGlassBadge("MANDATORY", glass.incorrect)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = upd.title.ifBlank { "BPSSC TRE 4.0 v${upd.versionName}" },
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (upd.fileSizeBytes > 0L) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Package Size: ${ApkInspector.formatFileSize(upd.fileSizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                    }
                    if (!upd.sha256Checksum.isNullOrBlank()) {
                        Text(
                            text = "SHA-256: ${upd.sha256Checksum.take(28)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textMuted
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Release Notes", style = MaterialTheme.typography.labelLarge, color = glass.accentPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = upd.releaseNotes.ifBlank { "Performance improvements and bug fixes." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textPrimary
                    )
                    Spacer(Modifier.height(14.dp))
                    CoolingGlassButton(
                        text = "Download Update (v${upd.versionName})",
                        icon = Icons.Rounded.CloudDownload,
                        testTagName = "download_app_update_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val opened = openHttpsUpdateUrl(context, upd.downloadUrl)
                            if (!opened) {
                                onFeedback("Invalid or unreachable HTTPS update link.", true)
                            }
                        }
                    )
                }
            }
        }

        if (allPublishedUpdates.isNotEmpty()) {
            item {
                Text(
                    text = "Release Notes History",
                    style = MaterialTheme.typography.titleSmall,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            items(allPublishedUpdates.filter { it.published }, key = { it.id }) { rel ->
                CoolingGlassCard(contentPadding = PaddingValues(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("v${rel.versionName} (Build ${rel.versionCode})", style = MaterialTheme.typography.titleSmall, color = glass.textPrimary, fontWeight = FontWeight.Bold)
                        CoolingGlassBadge(rel.updateType, if (rel.updateType == "MANDATORY") glass.review else glass.accentPrimary)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(rel.releaseNotes, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                }
            }
        }
    }
}

@Composable
fun MandatoryUpdateDialog(
    update: AppUpdateRelease,
    onFeedback: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val glass = LocalCoolingGlass.current
    CoolingGlassDialog(title = "Mandatory Update Required", onDismiss = { /* Blocking */ }) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = glass.incorrect)
                Text(
                    text = "Version v${update.versionName} is required to continue using BPSSC TRE 4.0.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = update.releaseNotes.ifBlank { "Critical security & exam engine update." },
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary
            )
            CoolingGlassButton(
                text = "Download Update Now",
                icon = Icons.Rounded.CloudDownload,
                testTagName = "mandatory_download_update_btn",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!openHttpsUpdateUrl(context, update.downloadUrl)) {
                        onFeedback("Could not open HTTPS update link.", true)
                    }
                }
            )
        }
    }
}

private fun openHttpsUpdateUrl(context: android.content.Context, url: String): Boolean {
    val clean = url.trim()
    if (!clean.startsWith("https://", ignoreCase = true)) return false
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clean)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}
