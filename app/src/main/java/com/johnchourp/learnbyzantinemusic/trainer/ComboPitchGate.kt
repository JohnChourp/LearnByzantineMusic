package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.music.PhthongName

/**
 * Intonation gate for the combined "phthong + time" exercise (Mode 3). It turns a raw
 * detected pitch into the phthong the singer is actually holding *in tune*: the nearest
 * phthong is returned only when the pitch is within [toleranceMoria] of it, otherwise null
 * (read as silence by the timing engine). The engine then matches that phthong against the
 * scheduled note, so a note greens only when the right phthong is sung in tune at the right
 * time.
 */
object ComboPitchGate {
    /** The default is the app's one tolerance, [IntonationProfile]: the voice check and every other screen agree. */
    fun inTunePhthong(match: PitchMatch?, toleranceMoria: Double = IntonationProfile.IN_TUNE_MORIA): PhthongName? {
        if (match == null) return null
        return if (IntonationProfile.isInTune(match.deviationMoria, toleranceMoria)) match.phthong else null
    }
}
