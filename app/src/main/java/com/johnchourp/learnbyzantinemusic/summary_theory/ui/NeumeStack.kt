package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmMeasureBar

/** Drawable backing each [Neume] glyph. */
@DrawableRes
internal fun Neume.drawableRes(): Int = when (this) {
    Neume.OLIGON -> R.drawable.oligon
    Neume.FLYER -> R.drawable.flyer
    Neume.EMBROIDERY -> R.drawable.embroidery
    Neume.EMBROIDERIES -> R.drawable.embroideries
    Neume.HIGH -> R.drawable.high
    Neume.APOSTROPHE -> R.drawable.apostrophe
    Neume.ISON -> R.drawable.ison
    Neume.UNDERFLOW -> R.drawable.underflow
    Neume.GORGO -> R.drawable.gorgo
    Neume.DIGORGO -> R.drawable.digorgo
    Neume.SIMPLE_DOT -> R.drawable.simple_dot
    Neume.FRACTION -> R.drawable.fraction
    // «Ποιότητος» quality signs + their example glyphs.
    Neume.HEAVY -> R.drawable.heavy
    Neume.HEAVY_SIMPLE_DOT -> R.drawable.heavy_simple_dot
    Neume.HEAVY_DOUBLE_DOTS -> R.drawable.heavy_double_dots
    Neume.HEAVY_TRIPLE_DOTS -> R.drawable.heavy_triple_dots
    Neume.YFEN -> R.drawable.yfen
    Neume.SLIGHT_CONTINUOUS -> R.drawable.slight_continuous
    Neume.DIGITAL -> R.drawable.digital
    Neume.ALL_RIGHT -> R.drawable.all_right
    Neume.VACCUM -> R.drawable.vaccum
    Neume.VACCUM_SIMPLE -> R.drawable.vaccum_simple
    Neume.LINK -> R.drawable.link
    Neume.INTERCOM -> R.drawable.intercom
    Neume.PRESENTED_GORGO -> R.drawable.presented_gorgo
    Neume.DOUBLE_DOTS -> R.drawable.double_dots
    Neume.TRIPLE_DOTS -> R.drawable.triple_dots
    Neume.SLIGHT -> R.drawable.slight
    Neume.ARGO -> R.drawable.argo
    Neume.TRIGORGO -> R.drawable.trigorgo
}

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
 * stack for TalkBack (the individual glyphs are decorative within it).
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
                painter = painterResource(glyph.neume.drawableRes()),
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
