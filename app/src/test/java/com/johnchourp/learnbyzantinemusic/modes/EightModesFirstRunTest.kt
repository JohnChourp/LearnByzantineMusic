package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import com.johnchourp.learnbyzantinemusic.modes.EightModesFirstRun.Show
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * What the 8 Ήχοι page shows by itself (ClickUp `869f5x2dd`, J4): the offer of «Βρες τη φωνή σου»,
 * then the four-step tour — each once, however it ends — and nothing over an ison that is sounding.
 */
class EightModesFirstRunTest {

    /** The two registered flags, marked the way the page marks them. */
    private class Flags {
        var offered = false
        var toured = false

        fun next(isonSounding: Boolean = false) = EightModesFirstRun.next(offered, toured, isonSounding)

        fun end(show: Show) {
            when (show) {
                Show.VOICE_TEST_OFFER -> offered = true
                Show.TOUR -> toured = true
                Show.NOTHING -> Unit
            }
        }
    }

    @Test
    fun theOfferComesFirstThenTheTourThenNeverAgain() {
        val flags = Flags()
        assertEquals(Show.VOICE_TEST_OFFER, flags.next())
        flags.end(Show.VOICE_TEST_OFFER) // taken or declined: either way it was offered
        assertEquals(Show.TOUR, flags.next())
        flags.end(Show.TOUR) // finished or skipped
        repeat(5) { assertEquals(Show.NOTHING, flags.next()) }
    }

    @Test
    fun anIsonAlreadySoundingGetsNeitherAndMarksNothing() {
        val flags = Flags()
        // The shortcut «Ίσο», «Αναπαραγωγή», or a page opened over a background ison.
        assertEquals(Show.NOTHING, flags.next(isonSounding = true))
        flags.end(flags.next(isonSounding = true))
        // The first ordinary opening still gets both, in order.
        assertEquals(Show.VOICE_TEST_OFFER, flags.next())
        flags.end(Show.VOICE_TEST_OFFER)
        assertEquals(Show.NOTHING, flags.next(isonSounding = true))
        assertEquals(Show.TOUR, flags.next())
    }

    @Test
    fun theTestTakenFromSettingsLeavesOnlyTheTour() {
        assertEquals(Show.TOUR, EightModesFirstRun.next(voiceTestOffered = true, tourShown = false))
    }

    @Test
    fun bothFlagsAreRegisteredBooleansOfTheEightModesStore() {
        listOf(AppPrefs.VoiceRangeOffered, AppPrefs.EightModesTourShown).forEach { key ->
            assertTrue("${key.name} is not in the registry", key in AppPrefs.all)
            assertEquals(key.name, AppPrefs.Store.EIGHT_MODES, key.store)
            assertEquals(key.name, AppPrefs.Type.BOOLEAN, key.type)
            assertEquals(key.name, "false", key.default)
        }
        assertEquals("voice_range_offered", AppPrefs.VoiceRangeOffered.name)
        assertEquals("eight_modes_tour_shown", AppPrefs.EightModesTourShown.name)
    }

    /**
     * The wiring the logic above relies on: each ending marks its flag. A tour whose «Παράλειψη» did
     * not mark it, or an offer closed without marking, would show again on every opening.
     */
    @Test
    fun everyEndingMarksItsFlag() {
        val modes = File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/modes")
        val activity = KotlinSource.withoutComments(File(modes, "EightModesActivity.kt").readText())
        listOf(
            "onVoiceOfferDone = { markShown(VOICE_RANGE_OFFERED_PREF_KEY) }",
            "onTourDone = { markShown(TOUR_SHOWN_PREF_KEY) }",
            "prefs.edit().putBoolean(flagKey, true).apply()",
            "VOICE_RANGE_OFFERED_PREF_KEY = AppPrefs.VoiceRangeOffered.name",
            "TOUR_SHOWN_PREF_KEY = AppPrefs.EightModesTourShown.name",
        ).forEach { wiring -> assertTrue("EightModesActivity: $wiring", wiring in activity) }

        val screen = KotlinSource.withoutComments(File(modes, "ui/EightModesScreen.kt").readText())
        listOf(
            "onClose = onVoiceOfferDone",
            "onSkip = onTourDone",
            "if (next == null) onTourDone() else tourStop = next",
        ).forEach { wiring -> assertTrue("EightModesScreen: $wiring", wiring in screen) }
    }
}
