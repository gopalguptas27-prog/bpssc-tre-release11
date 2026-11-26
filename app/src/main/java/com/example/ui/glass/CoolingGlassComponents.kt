package com.example.ui.glass

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.R
import com.example.ui.theme.CrystalWhite
import java.nio.ByteBuffer
import com.example.ui.theme.GlacierBlue600
import com.example.ui.theme.IceCyan500
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.SoftLavender500
import com.example.util.ImageCodec

/**
 * Full-screen Cooling Glass atmospheric background with subtle animated liquid-glass refraction
 * orbs and optional frosted artwork veil. Gracefully skips RenderEffect blur on API < 31.
 */
@Composable
fun CoolingGlassBackground(
    modifier: Modifier = Modifier,
    showHeroVeil: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalCoolingGlass.current
    val transition = rememberInfiniteTransition(label = "glass_ambient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glass_drift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(glass.bgTop, glass.bgMid, glass.bgBottom)
                )
            )
    ) {
        if (showHeroVeil) {
            val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(18.dp)
            } else {
                Modifier
            }
            Image(
                painter = painterResource(id = R.drawable.img_cooling_glass_hero_1795248454435),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (glass.isDark) 0.18f else 0.34f)
                    .then(blurMod)
            )
        }

        // Lightweight liquid-glass refraction aura
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.orbCyan, Color.Transparent),
                    center = Offset(w * (0.15f + 0.1f * drift), h * 0.14f),
                    radius = w * 0.62f
                ),
                radius = w * 0.62f,
                center = Offset(w * (0.15f + 0.1f * drift), h * 0.14f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.orbLavender, Color.Transparent),
                    center = Offset(w * (0.88f - 0.08f * drift), h * 0.42f),
                    radius = w * 0.58f
                ),
                radius = w * 0.58f,
                center = Offset(w * (0.88f - 0.08f * drift), h * 0.42f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.orbIce, Color.Transparent),
                    center = Offset(w * 0.32f, h * (0.86f - 0.06f * drift)),
                    radius = w * 0.68f
                ),
                radius = w * 0.68f,
                center = Offset(w * 0.32f, h * (0.86f - 0.06f * drift))
            )
        }

        content()
    }
}

/**
 * Core Frosted / Liquid Glass Surface Card with crisp specular border, high-opacity frosted base
 * (96–99%) preventing underlying text bleed-through, optional semantic tint layered over the
 * opaque base, soft ambient shadow, and optional ripple click interaction.
 */
@Composable
fun CoolingGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    strongFrost: Boolean = false,
    opaqueOverlay: Boolean = false,
    tintColor: Color? = null,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalCoolingGlass.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(stiffness = 500f),
        label = "glass_card_scale"
    )

    // Always paint a solid frosted backing first so underlying screens/cards can NEVER bleed through.
    val baseBrush = when {
        opaqueOverlay -> Brush.linearGradient(
            colors = listOf(glass.overlaySurfaceTop, glass.overlaySurfaceBottom)
        )
        strongFrost -> Brush.linearGradient(
            colors = listOf(glass.glassSurfaceStrong, glass.glassSurface)
        )
        else -> Brush.linearGradient(
            colors = listOf(glass.glassHighlightTop, glass.glassHighlightBottom)
        )
    }

    val tintOverlayBrush = tintColor?.let { tint ->
        Brush.linearGradient(
            colors = listOf(
                tint.copy(alpha = if (glass.isDark) 0.22f else 0.14f),
                tint.copy(alpha = if (glass.isDark) 0.08f else 0.04f)
            )
        )
    }

    val resolvedBorder = borderColor ?: glass.glassBorder
    val borderBrush = Brush.linearGradient(
        colors = listOf(resolvedBorder, glass.glassBorderSubtle)
    )

    val clickMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(color = glass.accentPrimary),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (opaqueOverlay) 18.dp else if (glass.isDark) 6.dp else 10.dp,
                shape = shape,
                ambientColor = glass.accentPrimary.copy(alpha = 0.16f),
                spotColor = glass.accentCyan.copy(alpha = 0.18f)
            )
            .clip(shape)
            .background(baseBrush)
            .then(if (tintOverlayBrush != null) Modifier.background(tintOverlayBrush) else Modifier)
            .border(width = 1.2.dp, brush = borderBrush, shape = shape)
            .then(clickMod)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Fit-to-ratio single-line text that shrinks cleanly down to [minFontSize] whenever a label
 * would otherwise overflow or clip inside a fixed-size icon pill, button, chip, or tab box.
 */
@Composable
fun AutoResizeSingleLineText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    maxFontSize: TextUnit = style.fontSize,
    minFontSize: TextUnit = 8.5.sp,
    fontWeight: FontWeight? = style.fontWeight,
    textAlign: TextAlign? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified
) {
    val startSp = if (maxFontSize != TextUnit.Unspecified) maxFontSize.value else 12f
    val minSp = minFontSize.value.coerceAtMost(startSp)
    var currentSp by remember(text, startSp) { mutableFloatStateOf(startSp) }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        fontSize = currentSp.sp,
        lineHeight = (currentSp * 1.18f).sp,
        fontWeight = fontWeight,
        textAlign = textAlign,
        letterSpacing = letterSpacing,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result ->
            if ((result.hasVisualOverflow || result.didOverflowWidth) && currentSp > minSp) {
                currentSp = (currentSp * 0.9f).coerceAtLeast(minSp)
            }
        }
    )
}

/**
 * Renders any Base64-encoded image with live native GIF animation playback on Android 9+ (API 28+)
 * via [ImageDecoder] + [AnimatedImageDrawable], preserving PNG transparency and gracefully falling
 * back to a static [ImageBitmap] on older APIs or JVM test runners.
 */
@Composable
fun AnimatedBase64Image(
    base64: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable (() -> Unit)? = null
) {
    if (base64.isNullOrBlank()) {
        fallback?.invoke()
        return
    }
    val isGif = remember(base64) { ImageCodec.isGifBase64(base64) }
    val animatedDrawable: Drawable? = remember(base64, isGif) {
        if (isGif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val bytes = ImageCodec.decodeToBytes(base64) ?: return@runCatching null
                val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
                val drawable = ImageDecoder.decodeDrawable(source)
                if (drawable is AnimatedImageDrawable) {
                    drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                    drawable.start()
                }
                drawable
            }.getOrNull()
        } else null
    }

    if (animatedDrawable != null) {
        val scaleType = when (contentScale) {
            ContentScale.Fit, ContentScale.Inside -> ImageView.ScaleType.FIT_CENTER
            ContentScale.FillBounds -> ImageView.ScaleType.FIT_XY
            else -> ImageView.ScaleType.CENTER_CROP
        }
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    this.scaleType = scaleType
                    this.contentDescription = contentDescription
                    adjustViewBounds = false
                    setImageDrawable(animatedDrawable)
                    if (animatedDrawable is AnimatedImageDrawable && !animatedDrawable.isRunning) {
                        animatedDrawable.start()
                    }
                }
            },
            update = { view ->
                view.scaleType = scaleType
                view.contentDescription = contentDescription
                if (view.drawable !== animatedDrawable) {
                    view.setImageDrawable(animatedDrawable)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    animatedDrawable is AnimatedImageDrawable &&
                    !animatedDrawable.isRunning
                ) {
                    animatedDrawable.start()
                }
            },
            modifier = modifier
        )
    } else {
        val staticBitmap = remember(base64) { ImageCodec.decodeToImageBitmap(base64) }
        if (staticBitmap != null) {
            Image(
                bitmap = staticBitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        } else {
            fallback?.invoke()
        }
    }
}

@Composable
fun CoolingGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    accentGradient: List<Color>? = null,
    testTagName: String? = null
) {
    val glass = LocalCoolingGlass.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "glass_btn_scale"
    )
    val colors = accentGradient ?: listOf(GlacierBlue600, IceCyan500)
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .then(if (testTagName != null) Modifier.testTag(testTagName) else Modifier)
            .scale(scale)
            .heightIn(min = 48.dp)
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = shape,
                ambientColor = colors.first().copy(alpha = 0.35f),
                spotColor = colors.last().copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                if (enabled) Brush.horizontalGradient(colors)
                else Brush.horizontalGradient(
                    listOf(glass.textMuted.copy(alpha = 0.35f), glass.textMuted.copy(alpha = 0.25f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(CrystalWhite.copy(alpha = 0.65f), CrystalWhite.copy(alpha = 0.15f))
                ),
                shape = shape
            )
            .clickable(
                enabled = enabled && !loading,
                interactionSource = interactionSource,
                indication = ripple(color = CrystalWhite),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = CrystalWhite,
                strokeWidth = 2.4.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CrystalWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                AutoResizeSingleLineText(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    maxFontSize = 13.5.sp,
                    minFontSize = 9.5.sp,
                    color = CrystalWhite,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CoolingGlassOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    tint: Color? = null,
    testTagName: String? = null
) {
    val glass = LocalCoolingGlass.current
    val resolvedTint = tint ?: glass.accentPrimary
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .then(if (testTagName != null) Modifier.testTag(testTagName) else Modifier)
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(glass.glassSurface)
            .border(
                width = 1.2.dp,
                color = resolvedTint.copy(alpha = if (enabled) 0.55f else 0.25f),
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) resolvedTint else glass.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
            }
            AutoResizeSingleLineText(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxFontSize = 12.5.sp,
                minFontSize = 8.5.sp,
                color = if (enabled) resolvedTint else glass.textMuted,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CoolingGlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    testTagName: String? = null
) {
    val glass = LocalCoolingGlass.current
    Box(
        modifier = modifier
            .then(if (testTagName != null) Modifier.testTag(testTagName) else Modifier)
            .size(48.dp)
            .clip(CircleShape)
            .background(glass.glassSurface)
            .border(1.dp, glass.glassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: glass.textPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun CoolingGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTagName: String? = null
) {
    val glass = LocalCoolingGlass.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        placeholder = if (placeholder.isNotBlank()) {
            { Text(placeholder, color = glass.textMuted, style = MaterialTheme.typography.bodyMedium) }
        } else null,
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, contentDescription = null, tint = glass.accentPrimary) }
        } else null,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = glass.glassSurfaceStrong,
            unfocusedContainerColor = glass.glassSurface,
            focusedBorderColor = glass.accentCyan,
            unfocusedBorderColor = glass.glassBorderSubtle,
            focusedLabelColor = glass.accentPrimary,
            unfocusedLabelColor = glass.textSecondary,
            focusedTextColor = glass.textPrimary,
            unfocusedTextColor = glass.textPrimary,
            cursorColor = glass.accentCyan
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTagName != null) Modifier.testTag(testTagName) else Modifier)
    )
}

@Composable
fun CoolingGlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val glass = LocalCoolingGlass.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, color = glass.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = glass.accentPrimary) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = glass.textMuted)
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = glass.glassSurfaceStrong,
            unfocusedContainerColor = glass.glassSurface,
            focusedBorderColor = glass.accentCyan,
            unfocusedBorderColor = glass.glassBorder,
            focusedTextColor = glass.textPrimary,
            unfocusedTextColor = glass.textPrimary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun CoolingGlassTopBar(
    title: String,
    subtitle: String? = null,
    clockText: String? = null,
    onBack: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val glass = LocalCoolingGlass.current
    val topBarShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(4f)
            .shadow(
                elevation = 8.dp,
                shape = topBarShape,
                ambientColor = glass.accentPrimary.copy(alpha = 0.14f),
                spotColor = glass.accentCyan.copy(alpha = 0.16f)
            )
            .clip(topBarShape)
            .background(
                Brush.verticalGradient(
                    listOf(glass.navBarSurface, glass.overlaySurfaceBottom)
                )
            )
            .border(
                BorderStroke(1.dp, glass.glassBorderSubtle),
                topBarShape
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            CoolingGlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                testTagName = "top_bar_back"
            )
            Spacer(Modifier.width(10.dp))
        } else if (navigationIcon != null && onNavigationClick != null) {
            CoolingGlassIconButton(
                icon = navigationIcon,
                contentDescription = "Menu",
                onClick = onNavigationClick,
                testTagName = "top_bar_menu"
            )
            Spacer(Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!clockText.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .testTag("live_header_clock")
                            .clip(RoundedCornerShape(10.dp))
                            .background(glass.accentPrimary.copy(alpha = 0.14f))
                            .border(0.8.dp, glass.accentCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = "Live Clock",
                            tint = glass.accentCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = clockText,
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.accentPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

data class GlassNavItem(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun CoolingGlassNavigationBar(
    items: List<GlassNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalCoolingGlass.current
    val navShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(4f),
        shape = navShape,
        color = glass.navBarSurface,
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, glass.glassBorder)
    ) {
        val compactBar = items.size >= 7
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = if (compactBar) 4.dp else 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = item.id == selectedId
                val pillBg = if (selected) {
                    Brush.horizontalGradient(listOf(GlacierBlue600.copy(alpha = 0.2f), IceCyan500.copy(alpha = 0.24f)))
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_${item.id}")
                        .height(52.dp)
                        .clip(RoundedCornerShape(if (compactBar) 12.dp else 16.dp))
                        .background(pillBg)
                        .border(
                            width = if (selected) 1.dp else 0.dp,
                            color = if (selected) glass.accentCyan.copy(alpha = 0.45f) else Color.Transparent,
                            shape = RoundedCornerShape(if (compactBar) 12.dp else 16.dp)
                        )
                        .clickable { onSelect(item.id) }
                        .padding(
                            vertical = 4.dp,
                            horizontal = if (compactBar) 2.dp else 4.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (selected) glass.accentPrimary else glass.textMuted,
                        modifier = Modifier.size(if (compactBar) 19.dp else 21.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    AutoResizeSingleLineText(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxFontSize = if (compactBar) 9.5.sp else 11.sp,
                        minFontSize = 7.2.sp,
                        letterSpacing = if (compactBar) (-0.25).sp else 0.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) glass.accentPrimary else glass.textMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun CoolingGlassTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalCoolingGlass.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(glass.glassSurface)
            .border(1.dp, glass.glassBorder, RoundedCornerShape(16.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (selected) Brush.horizontalGradient(listOf(GlacierBlue600, IceCyan500))
                        else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                AutoResizeSingleLineText(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    maxFontSize = if (tabs.size >= 5) 11.sp else 12.sp,
                    minFontSize = 8.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) CrystalWhite else glass.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CoolingGlassDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalCoolingGlass.current
    val dialogShape = RoundedCornerShape(26.dp)
    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(glass.scrimColor)
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                    onClick = onDismiss
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .zIndex(10f)
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null,
                        onClick = {}
                    ),
                shape = dialogShape,
                color = glass.overlaySurfaceTop,
                tonalElevation = 0.dp,
                shadowElevation = 24.dp,
                border = BorderStroke(
                    width = 1.3.dp,
                    brush = Brush.linearGradient(listOf(glass.glassBorder, glass.accentCyan.copy(alpha = 0.55f)))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(glass.overlaySurfaceTop, glass.overlaySurfaceBottom)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = glass.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun CoolingGlassConfirmationPopup(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String = "Cancel",
    confirmTestTag: String? = null,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    CoolingGlassDialog(title = title, onDismiss = onDismiss) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = glass.textSecondary
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CoolingGlassOutlinedButton(
                text = cancelText,
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            CoolingGlassButton(
                text = confirmText,
                onClick = onConfirm,
                testTagName = confirmTestTag,
                accentGradient = if (isDestructive) listOf(Color(0xFFDC2626), Color(0xFFB91C1C)) else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoolingGlassBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalCoolingGlass.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = glass.overlaySurfaceTop,
        contentColor = glass.textPrimary,
        scrimColor = glass.scrimColor,
        tonalElevation = 0.dp,
        shape = sheetShape,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(glass.accentPrimary.copy(alpha = 0.42f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(glass.overlaySurfaceTop, glass.overlaySurfaceBottom)
                    )
                )
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close sheet", tint = glass.textSecondary)
                }
            }
            Spacer(Modifier.height(8.dp))
            content()
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun CoolingGlassAvatar(
    name: String,
    photoBase64: String?,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val glass = LocalCoolingGlass.current
    val decoded = remember(photoBase64) { ImageCodec.decodeToImageBitmap(photoBase64) }
    val initials = remember(name) {
        name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "A" }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(GlacierBlue600, IceCyan500, SoftLavender500))
            )
            .border(2.dp, glass.glassBorder, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (decoded != null) {
            Image(
                bitmap = decoded,
                contentDescription = "$name photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                color = CrystalWhite,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProfilePhotoZoomDialog(
    name: String,
    photoBase64: String?,
    subtitle: String? = null,
    onDismiss: () -> Unit
) {
    val glass = LocalCoolingGlass.current
    val decoded = remember(photoBase64) { ImageCodec.decodeToImageBitmap(photoBase64) }
    CoolingGlassDialog(title = name.ifBlank { "Profile Photo" }, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_photo_zoom_dialog"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 340.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(GlacierBlue600, IceCyan500, SoftLavender500)))
                    .border(2.dp, glass.glassBorder, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (decoded != null) {
                    Image(
                        bitmap = decoded,
                        contentDescription = "$name zoomed profile photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "No profile photo uploaded",
                            tint = CrystalWhite,
                            modifier = Modifier.size(84.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No Profile Photo Uploaded",
                            style = MaterialTheme.typography.labelLarge,
                            color = CrystalWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CoolingGlassProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 9.dp,
    accentColors: List<Color> = listOf(GlacierBlue600, IceCyan500)
) {
    val glass = LocalCoolingGlass.current
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(glass.accentPrimary.copy(alpha = 0.14f))
            .border(0.6.dp, glass.glassBorder, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(height)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(accentColors))
        )
    }
}

@Composable
fun CoolingGlassLoadingCard(message: String = "Syncing with TRE 4.0 Cloud...") {
    val glass = LocalCoolingGlass.current
    CoolingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = glass.accentCyan,
                strokeWidth = 3.dp
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = glass.textSecondary
            )
        }
    }
}

@Composable
fun CoolingGlassEmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Rounded.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val glass = LocalCoolingGlass.current
    CoolingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(glass.accentPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = glass.accentPrimary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = glass.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                CoolingGlassButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun CoolingGlassStatusBanner(
    message: String,
    isError: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    val glass = LocalCoolingGlass.current
    val accent = if (isError) glass.incorrect else glass.correct
    CoolingGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(20f),
        opaqueOverlay = true,
        tintColor = accent,
        borderColor = accent.copy(alpha = 0.72f),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.Verified,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = glass.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = glass.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * Official Founder Branding Card — displays exact official text:
 * TRE 4.0
 * Founded by Gopal Gupta
 */
@Composable
fun FounderBrandingCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val glass = LocalCoolingGlass.current
    CoolingGlassCard(
        modifier = modifier.fillMaxWidth(),
        tintColor = SoftLavender500,
        borderColor = glass.accentCyan.copy(alpha = 0.55f),
        contentPadding = PaddingValues(if (compact) 14.dp else 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (compact) 42.dp else 54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(GlacierBlue600, IceCyan500, SoftLavender500))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Verified,
                    contentDescription = "Official Founder Badge",
                    tint = CrystalWhite,
                    modifier = Modifier.size(if (compact) 24.dp else 30.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TRE 4.0",
                    style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Founded by Gopal Gupta",
                    style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                    color = glass.accentPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (!compact) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Aspirant Test & Ranking Platform • Official Cooling Glass Edition",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
            }
        }
    }
}
