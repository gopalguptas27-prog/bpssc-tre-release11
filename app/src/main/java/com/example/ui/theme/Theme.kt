package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.LanguagePref

@Immutable
data class CoolingGlassTokens(
    val isDark: Boolean,
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val orbCyan: Color,
    val orbLavender: Color,
    val orbIce: Color,
    val glassSurface: Color,
    val glassSurfaceStrong: Color,
    val glassHighlightTop: Color,
    val glassHighlightBottom: Color,
    val overlaySurfaceTop: Color,
    val overlaySurfaceBottom: Color,
    val drawerSurface: Color,
    val scrimColor: Color,
    val glassBorder: Color,
    val glassBorderSubtle: Color,
    val navBarSurface: Color,
    val accentPrimary: Color,
    val accentCyan: Color,
    val accentLavender: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val correct: Color,
    val correctBg: Color,
    val incorrect: Color,
    val incorrectBg: Color,
    val review: Color,
    val reviewBg: Color,
    val dontKnow: Color,
    val dontKnowBg: Color
)

private val LightGlassTokens = CoolingGlassTokens(
    isDark = false,
    bgTop = Color(0xFFE6F4FF),
    bgMid = Color(0xFFF2F8FF),
    bgBottom = Color(0xFFECE7FE),
    orbCyan = Color(0x4538BDF8),
    orbLavender = Color(0x3DC4B5FD),
    orbIce = Color(0x59BAE6FD),
    // High-opacity frosted crystal cards (96%–99%) eliminate underlying text bleed-through
    glassSurface = Color(0xF5F7FBFF),
    glassSurfaceStrong = Color(0xFCFFFFFF),
    glassHighlightTop = Color(0xFAFFFFFF),
    glassHighlightBottom = Color(0xF5EDF6FF),
    // 98%–99% frosted foreground panels for Drawers, Dialogs, Bottom Sheets, Popups & Overlays
    overlaySurfaceTop = Color(0xFCFAFDFF),
    overlaySurfaceBottom = Color(0xF8ECF5FF),
    drawerSurface = Color(0xFCF3F9FF),
    scrimColor = Color(0x8C071A30),
    glassBorder = Color(0xFFFFFFFF),
    glassBorderSubtle = Color(0x9993C5FD),
    navBarSurface = Color(0xFAF2F9FF),
    accentPrimary = GlacierBlue600,
    accentCyan = IceCyan600,
    accentLavender = SoftLavender500,
    textPrimary = GlacierInkPrimary,
    textSecondary = GlacierInkSecondary,
    textMuted = GlacierInkMuted,
    correct = ExamCorrect,
    correctBg = ExamCorrectBg,
    incorrect = ExamIncorrect,
    incorrectBg = ExamIncorrectBg,
    review = ExamReview,
    reviewBg = ExamReviewBg,
    dontKnow = ExamDontKnow,
    dontKnowBg = ExamDontKnowBg
)

private val DarkGlassTokens = CoolingGlassTokens(
    isDark = true,
    bgTop = DeepIceBgStart,
    bgMid = DeepIceBgMid,
    bgBottom = DeepIceBgEnd,
    orbCyan = Color(0x3606B6D4),
    orbLavender = Color(0x308B5CF6),
    orbIce = Color(0x2E38BDF8),
    // High-opacity frosted midnight-ice cards (96%–99%) prevent overlapping text in Dark Mode
    glassSurface = Color(0xF50E2642),
    glassSurfaceStrong = Color(0xFC133054),
    glassHighlightTop = Color(0xFA16365D),
    glassHighlightBottom = Color(0xF50C213B),
    // 98%–99% frosted foreground panels for Drawers, Dialogs, Bottom Sheets, Popups & Overlays
    overlaySurfaceTop = Color(0xFC102948),
    overlaySurfaceBottom = Color(0xFA0A1C33),
    drawerSurface = Color(0xFC0A1B32),
    scrimColor = Color(0xB8020A16),
    glassBorder = Color(0x7A7DD3FC),
    glassBorderSubtle = Color(0x47BAE6FD),
    navBarSurface = Color(0xFC091A30),
    accentPrimary = IceBlue300,
    accentCyan = IceCyan400,
    accentLavender = SoftLavender400,
    textPrimary = DeepInkPrimary,
    textSecondary = DeepInkSecondary,
    textMuted = DeepInkMuted,
    correct = Color(0xFF34D399),
    correctBg = Color(0x3810B981),
    incorrect = Color(0xFFFB7185),
    incorrectBg = Color(0x38F43F5E),
    review = Color(0xFFFBBF24),
    reviewBg = Color(0x38F59E0B),
    dontKnow = Color(0xFFA5B4FC),
    dontKnowBg = Color(0x38818CF8)
)

val LocalCoolingGlass = staticCompositionLocalOf { LightGlassTokens }
val LocalAppLanguage = staticCompositionLocalOf { LanguagePref.BILINGUAL }

@Composable
fun appText(en: String, defaultText: String): String =
    if (LocalAppLanguage.current == LanguagePref.ENGLISH) en else defaultText

fun appTextFor(lang: LanguagePref, en: String, defaultText: String): String =
    if (lang == LanguagePref.ENGLISH) en else defaultText

private val CoolingLightColorScheme = lightColorScheme(
    primary = GlacierBlue600,
    onPrimary = CrystalWhite,
    primaryContainer = IceBlue100,
    onPrimaryContainer = GlacierInkPrimary,
    secondary = IceCyan600,
    onSecondary = CrystalWhite,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = GlacierInkPrimary,
    tertiary = SoftLavender500,
    onTertiary = CrystalWhite,
    tertiaryContainer = SoftLavender100,
    onTertiaryContainer = GlacierInkPrimary,
    background = IceBlue50,
    onBackground = GlacierInkPrimary,
    surface = CrystalWhite,
    onSurface = GlacierInkPrimary,
    surfaceVariant = IceBlue100,
    onSurfaceVariant = GlacierInkSecondary,
    outline = Color(0xFF93C5FD),
    error = ExamIncorrect,
    onError = CrystalWhite
)

private val CoolingDarkColorScheme = darkColorScheme(
    primary = IceBlue300,
    onPrimary = DeepIceBgStart,
    primaryContainer = Color(0xFF14385E),
    onPrimaryContainer = DeepInkPrimary,
    secondary = IceCyan400,
    onSecondary = DeepIceBgStart,
    secondaryContainer = Color(0xFF0E3E52),
    onSecondaryContainer = DeepInkPrimary,
    tertiary = SoftLavender400,
    onTertiary = DeepIceBgStart,
    tertiaryContainer = Color(0xFF2B2256),
    onTertiaryContainer = DeepInkPrimary,
    background = DeepIceBgStart,
    onBackground = DeepInkPrimary,
    surface = DeepIceBgMid,
    onSurface = DeepInkPrimary,
    surfaceVariant = Color(0xFF16355C),
    onSurfaceVariant = DeepInkSecondary,
    outline = Color(0xFF38BDF8),
    error = Color(0xFFFB7185),
    onError = DeepIceBgStart
)

val CoolingGlassShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun CoolingGlassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    languagePref: LanguagePref = LocalAppLanguage.current,
    content: @Composable () -> Unit
) {
    val tokens = if (darkTheme) DarkGlassTokens else LightGlassTokens
    val colorScheme = if (darkTheme) CoolingDarkColorScheme else CoolingLightColorScheme

    CompositionLocalProvider(
        LocalCoolingGlass provides tokens,
        LocalAppLanguage provides languagePref
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = CoolingGlassShapes,
            content = content
        )
    }
}
