package com.johnchourp.learnbyzantinemusic.trainer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ui.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import kotlin.math.roundToInt

/**
 * «Ήχος και βάση» — the scale the Melody Trainer plays and listens on (ClickUp `869f5x24v`, F2).
 *
 * The default, «Διατονικός», is the Trainer as it always was: Νη = 220 Hz, no transposition. Picking
 * a ήχος puts playback and listening on that mode's ladder, and shows the «Μεταφορά βάσης» for it —
 * the same stored value the 8 Ήχοι page uses for that ήχος, so the card says so.
 *
 * These are the Trainer's own composables, not lifted out of the 8 Ήχοι screen: that screen is being
 * changed separately, and the two will be unified when the range itself is (ClickUp `869f5x2dd`).
 * The mode names and genera come from the same list the 8 Ήχοι page shows, [EIGHT_MODES].
 */
@Composable
internal fun TrainerScaleCard(
    scale: TrainerScaleUi,
    onSelect: (Mode?) -> Unit,
    onBaseShiftChange: (Int) -> Unit,
) {
    LessonCard(title = stringResource(R.string.melody_trainer_scale_title)) {
        Text(
            text = stringResource(R.string.melody_trainer_scale_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        ScalePicker(selected = scale.mode, enabled = scale.enabled, onSelect = onSelect)
        Spacer(Modifier.height(12.dp))
        if (scale.mode == null) {
            Text(
                text = stringResource(R.string.melody_trainer_base_shift_diatonic),
                style = MaterialTheme.typography.bodySmall,
                color = LbmTextSecondary,
            )
        } else {
            BaseShiftControls(
                moria = scale.baseShiftMoria,
                enabled = scale.baseShiftEditable,
                onChange = onBaseShiftChange,
            )
        }
    }
}

@Composable
private fun ScalePicker(selected: Mode?, enabled: Boolean, onSelect: (Mode?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedRow = EIGHT_MODES.firstOrNull { it.mode == selected }
    val openLabel = stringResource(R.string.melody_trainer_scale_open)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LbmPrimaryContainer)
                .border(1.dp, LbmOutline, RoundedCornerShape(14.dp))
                .clickable(enabled = enabled, onClickLabel = openLabel, role = Role.DropdownList) {
                    expanded = true
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (selectedRow == null) {
                        stringResource(R.string.melody_trainer_scale_default)
                    } else {
                        stringResource(selectedRow.nameRes)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmBrown,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (selectedRow == null) {
                        stringResource(R.string.melody_trainer_scale_default_detail)
                    } else {
                        stringResource(selectedRow.selectorGenusRes)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = LbmTextSecondary,
                )
            }
            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null, tint = LbmBrown)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    ScaleOption(
                        name = stringResource(R.string.melody_trainer_scale_default),
                        detail = stringResource(R.string.melody_trainer_scale_default_detail),
                    )
                },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            EIGHT_MODES.forEach { row ->
                val mode = row.mode ?: return@forEach
                DropdownMenuItem(
                    text = {
                        ScaleOption(
                            name = stringResource(row.nameRes),
                            detail = stringResource(row.selectorGenusRes),
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(mode)
                    },
                )
            }
        }
    }
}

@Composable
private fun ScaleOption(name: String, detail: String) {
    Column {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = LbmBrown,
            fontWeight = FontWeight.SemiBold,
        )
        Text(text = detail, style = MaterialTheme.typography.bodySmall, color = LbmTextSecondary)
    }
}

@Composable
private fun BaseShiftControls(moria: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val valueText = if (moria == 0) {
        stringResource(R.string.melody_trainer_base_shift_zero)
    } else {
        stringResource(R.string.melody_trainer_base_shift_value, moria)
    }
    Text(
        text = stringResource(R.string.melody_trainer_base_shift_title),
        style = MaterialTheme.typography.titleSmall,
        color = LbmBrown,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(LbmPrimaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = LbmBrown,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = { onChange(BaseShift.DEFAULT_MORIA) },
            enabled = enabled && moria != BaseShift.DEFAULT_MORIA,
        ) {
            Icon(
                imageVector = Icons.Filled.Restore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = LbmBrown,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.melody_trainer_base_shift_reset),
                color = LbmBrown,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    Slider(
        value = moria.toFloat(),
        onValueChange = { onChange(it.roundToInt()) },
        enabled = enabled,
        modifier = Modifier.semantics { stateDescription = valueText },
        valueRange = BaseShift.MIN_MORIA.toFloat()..BaseShift.MAX_MORIA.toFloat(),
        steps = (BaseShift.MAX_MORIA - BaseShift.MIN_MORIA) - 1,
        colors = SliderDefaults.colors(
            thumbColor = LbmBrown,
            activeTrackColor = LbmBrown,
            inactiveTrackColor = LbmOutline,
        ),
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.melody_trainer_base_shift_lower),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.melody_trainer_base_shift_higher),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )
    }
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.melody_trainer_base_shift_shared),
        style = MaterialTheme.typography.bodySmall,
        color = LbmTextSecondary,
    )
}
