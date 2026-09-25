package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmMeasureBar

/** The sign's name in the current language: the sign table's one string key for it. */
@Composable
internal fun Neume.displayName(): String {
    check(nameRes != 0) { "$this is never named on its own" }
    return stringResource(nameRes)
}

/** What TalkBack says for the sign: its family and its name, e.g. «Νεύμα: Γοργόν». */
@Composable
internal fun Neume.contentDescription(): String = stringResource(kind.descriptionRes, displayName())

/** Maps the model's [NeumeAlign] to a Compose [Alignment] inside the form box. */
internal fun NeumeAlign.toAlignment(): Alignment = when (this) {
    NeumeAlign.CENTER -> Alignment.Center
    NeumeAlign.TOP_CENTER -> Alignment.TopCenter
    NeumeAlign.TOP_START -> Alignment.TopStart
    NeumeAlign.TOP_END -> Alignment.TopEnd
    NeumeAlign.BOTTOM_CENTER -> Alignment.BottomCenter
}

/**
 * Renders a [NeumeForm] by layering its glyphs in a fixed-height box, reproducing the original
 * size + gravity + translation of each ImageView. [contentDescription] describes the whole
 * stack for TalkBack (the individual glyphs are decorative within it). Every theory page draws its
 * signs here, ascending and descending alike, each with the glyph the sign table ([Neume]) gives it.
 *
 * Any glyph whose [Neume] is in [highlight] is tinted with [highlightColor] (default crimson) so a
 * teaching diagram can visually separate the quality sign from the plain black base neumes it
 * modifies. With the default empty set every glyph renders in its natural black, so existing
 * callers are unaffected.
 */
@Composable
fun NeumeStack(
    form: NeumeForm,
    contentDescription: String,
    modifier: Modifier = Modifier,
    highlight: Set<Neume> = emptySet(),
    highlightColor: Color = LbmMeasureBar,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(form.frameHeight.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        form.glyphs.forEach { glyph ->
            Image(
                painter = painterResource(glyph.neume.drawable),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = if (glyph.neume in highlight) ColorFilter.tint(highlightColor) else null,
                modifier = Modifier
                    .align(glyph.align.toAlignment())
                    .offset(x = glyph.dx.dp, y = glyph.dy.dp)
                    .size(width = glyph.w.dp, height = glyph.h.dp),
            )
        }
    }
}
