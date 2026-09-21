package com.johnchourp.learnbyzantinemusic.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/**
 * "From scratch" card shown above the home sections: where you are in the lesson path and one
 * button that opens the next step.
 *
 * It never hides or reorders the sections below — an experienced chanter ignores it and goes
 * straight to the page they want.
 *
 * The progress bar keeps its own semantics rather than being silenced: it reports how many steps
 * are finished, which differs from the step label whenever the learner jumped ahead and came back.
 */
@Composable
fun LearningPathCard(state: LearningPathUi, modifier: Modifier = Modifier) {
    val progress = if (state.totalSteps == 0) 0f else state.completedCount.toFloat() / state.totalSteps
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "learning_progress")
    val stepLabel = stringResource(R.string.learning_path_step, state.stepNumber, state.totalSteps)

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
                Icon(Icons.Filled.School, contentDescription = null, tint = LbmBrown)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.learning_path_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = LbmTextSecondary,
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(state.nextTitleRes),
                style = MaterialTheme.typography.bodyLarge,
                color = LbmTextPrimary,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = LbmBrown,
                trackColor = LbmOutline,
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = state.onContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = LbmBrown),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.completedCount == 0) {
                            stringResource(R.string.learning_path_start)
                        } else {
                            stringResource(R.string.learning_path_continue)
                        },
                    )
                }
            }
        }
    }
}
