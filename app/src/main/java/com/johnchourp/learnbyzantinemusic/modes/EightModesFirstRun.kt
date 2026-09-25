package com.johnchourp.learnbyzantinemusic.modes

/**
 * What the 8 Ήχοι page shows by itself when it opens (ClickUp `869f5x2dd`, J4): first the offer of
 * «Βρες τη φωνή σου», then a four-step tour — each once, ever.
 *
 * Both are remembered by a registered flag (`AppPrefs.VoiceRangeOffered`, `AppPrefs.EightModesTourShown`)
 * that is set however they end: a declined offer counts as offered, a skipped tour as shown. Settings
 * keeps «Βρες τη φωνή σου» at hand for later; the page itself never asks twice.
 *
 * A page that opens with the ison already sounding shows neither: opened only to play it — the
 * launcher shortcut «Ίσο», the notification's «Αναπαραγωγή» — or opened over a background ison. A
 * dialog would be in the way, and the voice test would hear the drone along with the singer. Nothing
 * is marked then, so the first ordinary visit still gets them.
 */
object EightModesFirstRun {

    enum class Show {
        VOICE_TEST_OFFER,
        TOUR,
        NOTHING,
    }

    fun next(voiceTestOffered: Boolean, tourShown: Boolean, isonSounding: Boolean = false): Show = when {
        isonSounding -> Show.NOTHING
        !voiceTestOffered -> Show.VOICE_TEST_OFFER
        !tourShown -> Show.TOUR
        else -> Show.NOTHING
    }
}
