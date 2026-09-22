package com.johnchourp.learnbyzantinemusic.music

/**
 * The γένος of a scale — which family of intervals it is built from (ClickUp `869f4tpxj`).
 *
 * It lives in `music` rather than in the screen package because it is domain vocabulary, not a UI
 * concern: the colour the selector paints it is a *rendering* of the γένος, and that mapping stays
 * in the UI layer where it belongs.
 *
 * `modes.ModeScaleGenus` is a typealias onto this, so existing call sites and tests keep working
 * while there is only ever one enum.
 */
enum class Genus {
    DIATONIC,
    SOFT_CHROMATIC,
    HARD_CHROMATIC,
    ENHARMONIC,
}
