package com.example.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrystalWhite
import com.example.ui.theme.GlacierBlue600
import com.example.ui.theme.IceCyan500
import com.example.ui.theme.LocalCoolingGlass
import com.example.ui.theme.RankBronze
import com.example.ui.theme.RankGold
import com.example.ui.theme.RankSilver
import com.example.domain.ExamScoringEngine
import com.example.ui.theme.SoftLavender500
import java.util.Locale

enum class OptionVisualState {
    DEFAULT,
    SELECTED,
    DONT_KNOW_SELECTED,
    CORRECT,
    INCORRECT
}

@Composable
fun CoolingGlassBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.8.dp, color.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        AutoResizeSingleLineText(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxFontSize = 10.5.sp,
            minFontSize = 8.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CoolingGlassProfileCard(
    displayName: String,
    photoBase64: String?,
    roleLabel: String,
    groupName: String,
    rankText: String,
    gsAverage: Double,
    langBadgeText: String,
    langBadgeColor: Color,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAvatarClick: (() -> Unit)? = null
) {
    val glass = LocalCoolingGlass.current
    CoolingGlassCard(
        modifier = modifier.fillMaxWidth(),
        strongFrost = true,
        borderColor = glass.accentCyan.copy(alpha = 0.65f),
        onClick = onEditClick,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoolingGlassAvatar(
                name = displayName,
                photoBase64 = photoBase64,
                size = 58.dp,
                onClick = onAvatarClick
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoolingGlassBadge(text = roleLabel, color = glass.accentPrimary)
                    CoolingGlassBadge(text = groupName, color = SoftLavender500)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = rankText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = glass.accentCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "GS Rank",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textMuted
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(glass.accentPrimary.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("GS Average", style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
                Text(
                    text = String.format(Locale.US, "%.2f / 40", gsAverage),
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Language Qualifying", style = MaterialTheme.typography.labelSmall, color = glass.textMuted)
                Spacer(Modifier.height(2.dp))
                CoolingGlassBadge(text = langBadgeText, color = langBadgeColor)
            }
        }
    }
}

@Composable
fun CoolingGlassQuestionCard(
    questionNumber: Int,
    totalQuestions: Int,
    subject: String,
    topic: String,
    source: String,
    questionText: String,
    markedForReview: Boolean,
    modifier: Modifier = Modifier
) {
    val glass = LocalCoolingGlass.current
    CoolingGlassCard(
        modifier = modifier.fillMaxWidth(),
        strongFrost = true,
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CoolingGlassBadge(
                    text = "Q $questionNumber / $totalQuestions",
                    color = glass.accentPrimary
                )
                CoolingGlassBadge(text = subject, color = SoftLavender500)
            }
            if (markedForReview) {
                CoolingGlassBadge(text = "Review", color = glass.review)
            }
        }
        if (topic.isNotBlank() || source.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = listOf(topic, source).filter { it.isNotBlank() }.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = glass.textMuted
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = questionText,
            style = MaterialTheme.typography.titleMedium,
            color = glass.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CoolingGlassOptionCard(
    optionKey: String,
    optionText: String,
    state: OptionVisualState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badgeText: String? = null,
    badgeColor: Color? = null,
    secondaryBadgeText: String? = null,
    secondaryBadgeColor: Color? = null
) {
    val glass = LocalCoolingGlass.current
    val shape = RoundedCornerShape(18.dp)
    val accent = when (state) {
        OptionVisualState.DEFAULT -> glass.accentPrimary
        OptionVisualState.SELECTED -> glass.accentCyan
        OptionVisualState.DONT_KNOW_SELECTED -> glass.dontKnow
        OptionVisualState.CORRECT -> glass.correct
        OptionVisualState.INCORRECT -> glass.incorrect
    }
    val tintOverlay = when (state) {
        OptionVisualState.DEFAULT -> Color.Transparent
        OptionVisualState.SELECTED -> glass.accentCyan.copy(alpha = if (glass.isDark) 0.24f else 0.14f)
        OptionVisualState.DONT_KNOW_SELECTED -> glass.dontKnowBg
        OptionVisualState.CORRECT -> glass.correctBg
        OptionVisualState.INCORRECT -> glass.incorrectBg
    }
    val borderWidth = if (state == OptionVisualState.DEFAULT) 1.dp else 2.dp
    val borderColor = if (state == OptionVisualState.DEFAULT) glass.glassBorder else accent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("option_card_$optionKey")
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(glass.glassSurfaceStrong)
            .background(tintOverlay)
            .border(borderWidth, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (state == OptionVisualState.DEFAULT) accent.copy(alpha = 0.14f)
                    else accent
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = optionKey,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (state == OptionVisualState.DEFAULT) accent else CrystalWhite
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyLarge,
                color = glass.textPrimary,
                fontWeight = if (state != OptionVisualState.DEFAULT) FontWeight.SemiBold else FontWeight.Normal,
                softWrap = true,
                modifier = Modifier.testTag("option_text_$optionKey")
            )
            if (!badgeText.isNullOrBlank() || !secondaryBadgeText.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!badgeText.isNullOrBlank()) {
                        CoolingGlassBadge(
                            text = badgeText,
                            color = badgeColor ?: accent,
                            modifier = Modifier.testTag("option_badge_$optionKey")
                        )
                    }
                    if (!secondaryBadgeText.isNullOrBlank()) {
                        CoolingGlassBadge(
                            text = secondaryBadgeText,
                            color = secondaryBadgeColor ?: glass.accentCyan,
                            modifier = Modifier.testTag("option_secondary_badge_$optionKey")
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        when (state) {
            OptionVisualState.CORRECT -> Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = "Correct Answer",
                tint = glass.correct,
                modifier = Modifier.testTag("option_correct_icon_$optionKey")
            )
            OptionVisualState.INCORRECT -> Icon(
                Icons.Rounded.Cancel,
                contentDescription = "Incorrect Selection",
                tint = glass.incorrect,
                modifier = Modifier.testTag("option_incorrect_icon_$optionKey")
            )
            OptionVisualState.DONT_KNOW_SELECTED -> Icon(
                Icons.AutoMirrored.Rounded.HelpOutline,
                contentDescription = "Not Attempted",
                tint = glass.dontKnow
            )
            else -> Unit
        }
    }
}

@Composable
fun CoolingGlassLeaderboardCard(
    rank: Int,
    aspirantName: String,
    photoBase64: String?,
    scoreText: String,
    testsAttemptedText: String,
    languageBadgeText: String? = null,
    languageBadgeColor: Color = Color.Unspecified,
    isCurrentUser: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subjectSummaryText: String? = null,
    statusBadgeText: String? = null,
    statusBadgeColor: Color = Color.Unspecified,
    testTagName: String? = null
) {
    val glass = LocalCoolingGlass.current
    val resolvedLangColor = if (languageBadgeColor == Color.Unspecified) glass.textMuted else languageBadgeColor
    val resolvedStatusColor = if (statusBadgeColor == Color.Unspecified) glass.accentPrimary else statusBadgeColor
    val rankColor = when (rank) {
        1 -> RankGold
        2 -> RankSilver
        3 -> RankBronze
        else -> glass.accentPrimary
    }

    CoolingGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTagName != null) Modifier.testTag(testTagName) else Modifier),
        strongFrost = isCurrentUser,
        tintColor = if (isCurrentUser) glass.accentCyan else null,
        borderColor = if (isCurrentUser) glass.accentCyan else null,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(rankColor.copy(alpha = 0.18f))
                    .border(1.dp, rankColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (rank in 1..3) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = "Rank $rank",
                        tint = rankColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelLarge,
                        color = rankColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            CoolingGlassAvatar(name = aspirantName, photoBase64 = photoBase64, size = 44.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = aspirantName,
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrentUser) {
                        Spacer(Modifier.width(6.dp))
                        CoolingGlassBadge(text = "YOU", color = glass.accentCyan)
                    }
                }
                Text(
                    text = testsAttemptedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textMuted
                )
                if (!subjectSummaryText.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subjectSummaryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = scoreText,
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (!languageBadgeText.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    CoolingGlassBadge(text = languageBadgeText, color = resolvedLangColor)
                }
                if (!statusBadgeText.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    CoolingGlassBadge(text = statusBadgeText, color = resolvedStatusColor)
                }
            }
        }
    }
}

@Composable
fun CoolingGlassResultCard(
    testTitle: String,
    score: Double,
    maxMarks: Double,
    percentage: Double,
    correct: Int,
    incorrect: Int,
    dontKnow: Int,
    notAttempted: Int,
    negativeDeducted: Double,
    modifier: Modifier = Modifier,
    accuracyPercentage: Double = ExamScoringEngine.computeAccuracy(correct, incorrect)
) {
    val glass = LocalCoolingGlass.current
    CoolingGlassCard(
        modifier = modifier.fillMaxWidth(),
        strongFrost = true,
        borderColor = glass.accentCyan.copy(alpha = 0.7f),
        contentPadding = PaddingValues(18.dp)
    ) {
        Text(
            text = testTitle,
            style = MaterialTheme.typography.titleLarge,
            color = glass.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(GlacierBlue600, IceCyan500)))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Final Score", style = MaterialTheme.typography.labelMedium, color = CrystalWhite.copy(alpha = 0.85f))
                Text(
                    text = String.format(Locale.US, "%.2f / %.0f", score, maxMarks),
                    style = MaterialTheme.typography.displayMedium,
                    color = CrystalWhite,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "%.1f%%", percentage),
                    style = MaterialTheme.typography.headlineMedium,
                    color = CrystalWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "Acc: %.1f%% • -%.2f Neg", accuracyPercentage, negativeDeducted),
                    style = MaterialTheme.typography.labelSmall,
                    color = CrystalWhite.copy(alpha = 0.92f)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatPill("Correct", "$correct", glass.correct, Modifier.weight(1f))
            StatPill("Wrong", "$incorrect", glass.incorrect, Modifier.weight(1f))
            StatPill("Opt E", "$dontKnow", glass.dontKnow, Modifier.weight(1f))
            StatPill("Skipped", "$notAttempted", glass.review, Modifier.weight(1f))
        }
    }
}

@Composable
fun StatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val glass = LocalCoolingGlass.current
    Column(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.8.dp, color.copy(alpha = 0.45f), RoundedCornerShape(13.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AutoResizeSingleLineText(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            maxFontSize = 15.sp,
            minFontSize = 10.5.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        AutoResizeSingleLineText(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxFontSize = 10.5.sp,
            minFontSize = 8.sp,
            color = glass.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
