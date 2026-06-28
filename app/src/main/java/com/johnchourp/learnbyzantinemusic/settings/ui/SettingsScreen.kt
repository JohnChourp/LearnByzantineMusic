package com.johnchourp.learnbyzantinemusic.settings.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.johnchourp.learnbyzantinemusic.AppFontScale
import com.johnchourp.learnbyzantinemusic.AppLanguage
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurfaceVariant
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import kotlin.math.roundToInt

/**
 * Strings for the language-confirmation dialog, pre-resolved by the host Activity in the
 * **target** language (so picking «English» from a Greek UI shows the prompt in English, and
 * vice-versa). Null hides the dialog.
 */
data class LanguagePrompt(
    val targetCode: String,
    val title: String,
    val message: String,
    val confirmLabel: String,
    val dismissLabel: String,
)

/** The discrete font-size stops the slider snaps to, shown as tick labels under the track. */
private val FontSteps = listOf(20, 40, 60, 80, 100)

private data class LanguageOption(val code: String, val nameRes: Int, val badge: String)

private val LanguageOptions = listOf(
    LanguageOption(AppLanguage.languageGreek, R.string.settings_language_option_greek, "ΕΛ"),
    LanguageOption(AppLanguage.languageEnglish, R.string.settings_language_option_english, "EN"),
)

/**
 * Redesigned «Ρυθμίσεις Εφαρμογής» (App Settings) screen — a pure renderer of the applied
 * font step + current language, plus callbacks. Mirrors the shared lesson design language
 * (hero, [LessonCard], [StaggeredAppear], warm palette).
 *
 * The font-size control previews live in-screen as you drag and only commits app-wide when you
 * press «Εφαρμογή», so we no longer recreate the Activity on every drag step. Language selection
 * routes through a themed confirmation dialog before the app restarts. All persistence /
 * Activity recreation / restart logic stays in the host [com.johnchourp.learnbyzantinemusic.SettingsActivity].
 */
@Composable
fun SettingsScreen(
    appliedFontStep: Int,
    currentLanguageCode: String,
    languagePrompt: LanguagePrompt?,
    onApplyFontStep: (Int) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onConfirmLanguage: () -> Unit,
    onDismissLanguagePrompt: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(scroll),
    ) {
        LessonHero(
            title = stringResource(R.string.settings_page_title),
            subtitle = stringResource(R.string.settings_subtitle),
            onBack = onBack,
            icon = Icons.Filled.Settings,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            StaggeredAppear(delayMillis = 60) {
                FontSizeCard(
                    appliedFontStep = appliedFontStep,
                    onApplyFontStep = onApplyFontStep,
                )
            }
            StaggeredAppear(delayMillis = 140) {
                LanguageCard(
                    currentLanguageCode = currentLanguageCode,
                    onLanguageSelected = onLanguageSelected,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (languagePrompt != null) {
        LanguageConfirmDialog(
            prompt = languagePrompt,
            onConfirm = onConfirmLanguage,
            onDismiss = onDismissLanguagePrompt,
        )
    }
}

/* ----------------------------- Font size ----------------------------- */

@Composable
private fun FontSizeCard(
    appliedFontStep: Int,
    onApplyFontStep: (Int) -> Unit,
) {
    // Re-seed the previewed step from the applied one whenever it changes (e.g. after an apply
    // recreates the Activity), so the slider always starts in sync with what is really in effect.
    var selectedStep by remember(appliedFontStep) {
        mutableIntStateOf(AppFontScale.normalizeStep(appliedFontStep))
    }
    val applied = AppFontScale.normalizeStep(appliedFontStep)
    val isApplied = selectedStep == applied
    val percentText = stringResource(R.string.settings_font_size_value, selectedStep)

    LessonCard(title = stringResource(R.string.settings_font_size_label)) {
        Text(
            text = stringResource(R.string.settings_font_size_card_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(14.dp))

        // Big, crisp percent readout for the currently-chosen size. The value is announced to
        // TalkBack once, by the Slider's stateDescription below — keep it the single source so we
        // don't double-speak the percent on every drag step (matches TempoCard/EightModes sliders).
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = percentText,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = LbmBrown,
            )
        }
        Spacer(Modifier.height(6.dp))

        Slider(
            value = AppFontScale.stepToSeekBarIndex(selectedStep).toFloat(),
            onValueChange = { selectedStep = AppFontScale.seekBarIndexToStep(it.roundToInt()) },
            valueRange = 0f..(FontSteps.lastIndex).toFloat(),
            steps = FontSteps.size - 2, // internal ticks between the two ends → 5 stops total
            colors = SliderDefaults.colors(
                thumbColor = LbmBrown,
                activeTrackColor = LbmBrown,
                inactiveTrackColor = LbmOutline,
                activeTickColor = LbmSurface,
                inactiveTickColor = LbmBrown.copy(alpha = 0.35f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = percentText },
        )

        // Tick labels: highlight the currently-selected stop.
        Row(modifier = Modifier.fillMaxWidth()) {
            FontSteps.forEach { step ->
                val isSel = step == selectedStep
                Text(
                    text = step.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSel) LbmBrown else LbmTextSecondary,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    textAlign = when (step) {
                        FontSteps.first() -> TextAlign.Start
                        FontSteps.last() -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.settings_font_size_smaller),
                style = MaterialTheme.typography.labelSmall,
                color = LbmTextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.settings_font_size_larger),
                style = MaterialTheme.typography.labelSmall,
                color = LbmTextSecondary,
            )
        }

        Spacer(Modifier.height(16.dp))
        FontSizePreview(selectedStep = selectedStep, appliedStep = applied)

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_font_size_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )

        Spacer(Modifier.height(16.dp))
        Crossfade(targetState = isApplied, label = "applyState") { atApplied ->
            if (atApplied) {
                AppliedChip()
            } else {
                Button(
                    onClick = { onApplyFontStep(selectedStep) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LbmBrown,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_font_size_apply),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppliedChip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AccentGreenContainer)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = AccentGreenContent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.settings_font_size_applied),
            style = MaterialTheme.typography.titleMedium,
            color = AccentGreenContent,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FontSizePreview(selectedStep: Int, appliedStep: Int) {
    // The sample uses sp, which Compose already multiplies by the *applied* fontScale of this
    // screen. Multiplying the base size by (target / applied) therefore renders the sample at the
    // absolute size it will have once the selected step is applied — and lands exactly on the
    // screen's normal text size when selectedStep == appliedStep. So the preview is faithful
    // without recreating the Activity.
    val targetScale = AppFontScale.stepToFontScale(selectedStep)
    val appliedScale = AppFontScale.stepToFontScale(appliedStep)
    val ratio = if (appliedScale == 0f) 1f else targetScale / appliedScale
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "previewRatio",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LbmSurfaceVariant)
            .border(1.dp, LbmOutline, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_font_size_preview_label).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = LbmTextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_font_size_preview_sample),
            fontSize = (22f * animatedRatio).sp,
            lineHeight = (30f * animatedRatio).sp,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/* ----------------------------- Language ----------------------------- */

@Composable
private fun LanguageCard(
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
) {
    val current = AppLanguage.normalizeLanguageCode(currentLanguageCode)
    LessonCard(title = stringResource(R.string.settings_language_label)) {
        Text(
            text = stringResource(R.string.settings_language_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.settings_language_current_value,
                AppLanguage.getNativeLanguageName(current),
            ),
            style = MaterialTheme.typography.titleSmall,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LanguageOptions.forEach { option ->
                LanguageOptionRow(
                    option = option,
                    selected = option.code == current,
                    onClick = { onLanguageSelected(option.code) },
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    option: LanguageOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        targetValue = if (selected) LbmPrimaryContainer else LbmSurface,
        animationSpec = tween(220),
        label = "langContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) LbmBrown else LbmOutline,
        animationSpec = tween(220),
        label = "langBorder",
    )
    val name = stringResource(option.nameRes)
    val currentBadge = stringResource(R.string.settings_language_current_badge)
    // Both rows stay enabled so the active language is announced as "selected" (not "disabled");
    // tapping the current one is a harmless no-op (the Activity early-returns on the same code).
    // Role.RadioButton carries the selected / not-selected state to TalkBack for both rows, and the
    // visible «Τρέχουσα/Current» sub-label is the single extra cue — so we set no stateDescription.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .selectable(
                selected = selected,
                enabled = true,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) LbmBrown else LbmPrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = option.badge,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else LbmBrown,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = LbmTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            if (selected) {
                Text(
                    text = currentBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = LbmBrown,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        SelectionIndicator(selected = selected)
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean) {
    val ring by animateColorAsState(
        targetValue = if (selected) LbmBrown else LbmOutline,
        animationSpec = tween(220),
        label = "ring",
    )
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) LbmBrown else Color.Transparent)
            .border(2.dp, ring, CircleShape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/* ----------------------------- Confirm dialog ----------------------------- */

@Composable
private fun LanguageConfirmDialog(
    prompt: LanguagePrompt,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = prompt.confirmLabel,
                    color = LbmBrown,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = prompt.dismissLabel, color = LbmTextSecondary)
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Filled.Translate,
                contentDescription = null,
                tint = LbmBrown,
            )
        },
        title = {
            Text(
                text = prompt.title,
                color = LbmTextPrimary,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(text = prompt.message, color = LbmTextSecondary)
        },
        containerColor = LbmSurface,
        iconContentColor = LbmBrown,
        titleContentColor = LbmTextPrimary,
        textContentColor = LbmTextSecondary,
        shape = RoundedCornerShape(20.dp),
    )
}
