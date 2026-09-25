package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.Beats
import org.junit.Assert.assertEquals
import org.junit.Test

class MelodyTempoTest {

    @Test
    fun `millis per beat follows bpm`() {
        assertEquals(1000.0, MelodyTempo(60).millisPerBeat, 1e-6)
        assertEquals(500.0, MelodyTempo(120).millisPerBeat, 1e-6)
    }

    @Test
    fun `beats convert to milliseconds`() {
        val tempo = MelodyTempo(120) // 500 ms per beat
        assertEquals(500L, tempo.beatsToMillis(Beats.ONE))
        assertEquals(250L, tempo.beatsToMillis(Beats.of(1, 2)))
        assertEquals(1000L, tempo.beatsToMillis(Beats.whole(2)))
        assertEquals(0L, tempo.beatsToMillis(Beats.ZERO))
    }

    @Test
    fun `negative beats clamp to zero`() {
        assertEquals(0L, MelodyTempo(120).beatsToMillis(Beats.whole(-3)))
    }

    @Test
    fun `clampBpm keeps bpm inside the allowed range`() {
        assertEquals(MelodyTempo.MIN_BPM, MelodyTempo.clampBpm(1))
        assertEquals(MelodyTempo.MAX_BPM, MelodyTempo.clampBpm(10_000))
        assertEquals(80, MelodyTempo.clampBpm(80))
    }

    @Test
    fun `of builds a clamped tempo`() {
        assertEquals(MelodyTempo.MAX_BPM, MelodyTempo.of(10_000).beatsPerMinute)
        assertEquals(MelodyTempo.MIN_BPM, MelodyTempo.of(0).beatsPerMinute)
    }
}
