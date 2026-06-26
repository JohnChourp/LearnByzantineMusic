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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R

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
 */
@Composable
fun NeumeStack(
    form: NeumeForm,
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
