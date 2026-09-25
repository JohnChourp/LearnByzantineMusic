package com.johnchourp.learnbyzantinemusic.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary

/**
 * «Tone of the week» card, first on the home screen (ClickUp `869f5x24r`): today's tone — or the
 * period's name when it has none — and, from the vespers hour on, a second line with the tone that
 * tonight's vespers begins. Two actions: «Άνοιξε στους 8 Ήχους», one-shot and only when there is a
 * tone to open, and «Αναστασιματάριο».
 *
 * Pure UI: [WeeklyToneUi.from] decides everything shown here. The actions wrap onto a second row on
 * a narrow screen or with large text, rather than squeezing their labels.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeeklyToneCard(state: WeeklyToneUi, modifier: Modifier = Modifier) {
    val headline = state.headlineToneRes
        ?.let { toneRes -> stringResource(state.headlineRes, stringResource(toneRes)) }
        ?: stringResource(state.headlineRes)

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = LbmPrimaryContainer,
            contentColor = LbmTextPrimary,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = LbmBrown)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }

            state.vespersToneRes?.let { vespersToneRes ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_weekly_tone_from_vespers, stringResource(vespersToneRes)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.onOpenEightModes?.let { openEightModes ->
                    Button(
                        onClick = openEightModes,
                        colors = ButtonDefaults.buttonColors(containerColor = LbmBrown),
                    ) {
                        Text(text = stringResource(R.string.home_weekly_tone_open_eight_modes))
                    }
                }
                OutlinedButton(onClick = state.onOpenAnastasimatarion) {
                    Text(text = stringResource(R.string.anastasimatarion_title))
                }
            }
        }
    }
}
