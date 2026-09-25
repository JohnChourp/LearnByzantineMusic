package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.Moria

/**
 * The 8 Ήχοι ladder of a mode at a base shift, built without Compose (ClickUp `869f5x2dq`).
 *
 * The page built its ladder inside a private composable, which nothing outside the page can call —
 * and the ison has to be computed outside it now: by the launcher shortcut «Ίσο», and by the service
 * that keeps the ison playing in the background. Every one of them builds the ladder here, so the
 * background ison cannot drift from the page's diagram by a single μόριο: it is the same ladder,
 * made the same way, looked up by the same [IsonDrone.step].
 */
object ModeLadders {

    /** How many octaves the page's ladder spans. */
    const val OCTAVES = 3

    /** [scale]'s ladder, moved by [baseShiftMoria] — what the page's diagram is drawn from. */
    fun ladder(scale: ModeScaleDefinition, baseShiftMoria: Int): ModeLadder =
        scale.ladder(octaves = OCTAVES, baseShift = Moria(baseShiftMoria))

    /** [mode]'s ladder, through the canonical mode → scale table. */
    fun ladder(mode: Mode, baseShiftMoria: Int): ModeLadder =
        ladder(EightModeScaleDefinitions.SCALE_BY_MODE.getValue(mode), baseShiftMoria)
}
