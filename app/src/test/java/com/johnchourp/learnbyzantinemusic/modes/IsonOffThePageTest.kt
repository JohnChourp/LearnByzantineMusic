package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.modes.ui.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.modes.ui.SCALE_OCTAVES
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Moria
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * The ison computed away from the page sounds the page's pitch, exactly (ClickUp `869f5x2dq`).
 *
 * The page resolves its ison inside a composable; the background service, and anything else that
 * sounds the ison without the page, goes through [IsonDrone.held]. Two paths to one pitch is how a
 * drone ends up a comma off the scale it accompanies, so they are compared bit-for-bit for every
 * mode, every φθόγγος «Ίσον σε…» offers, and every base shift the slider can reach — and both are held
 * to the diagram's own key.
 */
class IsonOffThePageTest {

    @Test
    fun theIsonOffThePageIsThePagesPitchForEveryModeChoiceAndShift() {
        var compared = 0
        EIGHT_MODES.forEach { row ->
            val mode = row.mode
            (BaseShift.MIN_MORIA..BaseShift.MAX_MORIA).forEach { shift ->
                // Exactly what the page does: its ladder, its choices, its lookup.
                val pageLadder = ModeLadders.ladder(row.scale, shift)
                val choices = IsonDrone.choices(mode, pageLadder)
                assertNotNull("${mode.key} shift=$shift: no choices", choices)
                // The diagram's key, from the independent label/frequency path.
                val labels = row.scale.ascendingPhthongs(SCALE_OCTAVES).reversed()
                val keys = ModeScaleFrequencies.topToBottom(
                    row.scale.repeatedIntervals(SCALE_OCTAVES),
                    row.scale.referenceMoriaFromBottom("Νη", SCALE_OCTAVES),
                    shift,
                )
                choices!!.all.forEach { choice ->
                    val where = "${mode.key} shift=$shift ${choice.label}"
                    val onThePage = IsonDrone.step(pageLadder, choice)!!.frequencyHz
                    // The base is asked for as «no choice», exactly as the page and the service do.
                    val request = IsonDrone.Request(mode, shift, choice.takeIf { it != choices.base })
                    val offThePage = IsonDrone.held(request)
                    assertNotNull(where, offThePage)
                    assertEquals(where, onThePage, offThePage!!.frequencyHz, 0.0)
                    assertEquals(where, keys[labels.indexOf(choice.label)], offThePage.frequencyHz, 0.0)
                    compared++
                }
            }
        }
        // Guards the sweep: every mode × every shift × every φθόγγος, or it proves nothing.
        assertEquals(
            EIGHT_MODES.size * (BaseShift.MAX_MORIA - BaseShift.MIN_MORIA + 1) * PhthongName.entries.size,
            compared,
        )
    }

    @Test
    fun theSharedLadderIsTheOneThePageDrew() {
        // ModeLadders took over the page's private builder; the page's octave count is its own.
        assertEquals(SCALE_OCTAVES, ModeLadders.OCTAVES)
        EIGHT_MODES.forEach { row ->
            val mode = row.mode
            listOf(BaseShift.MIN_MORIA, 0, BaseShift.MAX_MORIA).forEach { shift ->
                assertEquals(
                    "${mode.key} shift=$shift",
                    row.scale.ladder(octaves = SCALE_OCTAVES, baseShift = Moria(shift)).frequencies,
                    ModeLadders.ladder(mode, shift).frequencies,
                )
            }
        }
    }
}
