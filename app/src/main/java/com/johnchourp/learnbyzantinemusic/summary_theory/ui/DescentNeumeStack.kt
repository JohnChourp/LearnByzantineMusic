package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R

/** Drawable backing each [DescentNeume] glyph. */
@DrawableRes
internal fun DescentNeume.drawableRes(): Int = when (this) {
    DescentNeume.APOSTROPHE -> R.drawable.apostrophe
    DescentNeume.SLIGHT -> R.drawable.slight
    DescentNeume.SLIGHT_APOSTROPHE -> R.drawable.slight_apostrophe
    DescentNeume.LOW -> R.drawable.low
}

/**
 * Renders a [DescentForm] by layering its glyphs in a fixed-height box, reproducing the original
 * size + gravity + translation of each ImageView. [contentDescription] describes the whole stack
 * for TalkBack (the individual glyphs are decorative within it). Mirrors `NeumeStack` for the
 * descending model and reuses the shared `NeumeAlign.toAlignment()` mapping.
 */
@Composable
fun DescentNeumeStack(
    form: DescentForm,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(form.frameHeight.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        form.glyphs.forEach { glyph ->
            Image(
                painter = painterResource(glyph.neume.drawableRes()),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(glyph.align.toAlignment())
                    .offset(x = glyph.dx.dp, y = glyph.dy.dp)
                    .size(width = glyph.w.dp, height = glyph.h.dp),
            )
        }
    }
}
