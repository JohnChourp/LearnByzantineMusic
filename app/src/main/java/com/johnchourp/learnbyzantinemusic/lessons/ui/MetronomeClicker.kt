package com.johnchourp.learnbyzantinemusic.lessons.ui

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * The metronome's audible click (ClickUp `869f4tpg0`).
 *
 * A [ToneGenerator] rather than the app's [com.johnchourp.learnbyzantinemusic.modes.PhthongTonePlayer]:
 * that one streams a sustained pitch and is built for holding a φθόγγος, while a metronome needs a
 * short percussive burst with no pitch of its own — one that must not be mistaken for a note of the
 * scale being practised.
 *
 * The downbeat gets a different, higher tone so the ear can find the start of the grouping without
 * watching the screen, which is the point of a metronome on a page about δίσημος/τρίσημος/τετράσημος.
 *
 * Creation is lazy and every call is guarded: [ToneGenerator] throws when the device's audio session
 * is unavailable (another app holding it, or an emulator without an audio backend). A metronome that
 * crashes the lesson page is worse than one that is briefly silent, so a failure degrades to the
 * visual pulse alone.
 *
 * Its level is [volumePercent] of the tone generator's range, and [setVolume] changes it: «Ψάλλε
 * μαζί» in the Melody Trainer has a volume of its own for the click (ClickUp `869f5x2cv`). A
 * generator's level is fixed when it is made, so a new level makes a new one on the next click; at 0
 * there is no click at all. The Trainer clicks from its playback thread, so every call is locked.
 */
class MetronomeClicker(volumePercent: Int = VOLUME) {

    private val lock = Any()
    private var generator: ToneGenerator? = null
    private var unavailable = false
    private var volume = volumePercent.coerceIn(0, ToneGenerator.MAX_VOLUME)

    private fun obtain(): ToneGenerator? {
        if (unavailable) return null
        generator?.let { return it }
        return runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, volume) }
            .onFailure { unavailable = true }
            .getOrNull()
            ?.also { generator = it }
    }

    /** One click. [downbeat] picks the accented tone that marks the start of the grouping. */
    fun click(downbeat: Boolean) {
        val tone = if (downbeat) TONE_DOWNBEAT else TONE_BEAT
        synchronized(lock) {
            if (volume == 0) return
            runCatching { obtain()?.startTone(tone, DURATION_MS) }
        }
    }

    /**
     * Makes the tone generator now, so the first click is not late by the time it takes to make one.
     * Safe to skip: [click] makes it when it is missing.
     */
    fun warmUp() {
        synchronized(lock) {
            if (volume > 0) obtain()
        }
    }

    /** The click's level from now on, 0 … 100; 0 is silence. */
    fun setVolume(percent: Int) {
        val level = percent.coerceIn(0, ToneGenerator.MAX_VOLUME)
        synchronized(lock) {
            if (level == volume) return
            volume = level
            // Made again, at the new level, on the next click.
            runCatching { generator?.release() }
            generator = null
        }
    }

    fun release() {
        synchronized(lock) {
            runCatching { generator?.release() }
            generator = null
        }
    }

    private companion object {
        const val VOLUME = 80
        const val DURATION_MS = 45
        val TONE_DOWNBEAT = ToneGenerator.TONE_PROP_BEEP
        val TONE_BEAT = ToneGenerator.TONE_PROP_ACK
    }
}
