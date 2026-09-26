package com.johnchourp.learnbyzantinemusic.lectern

import com.johnchourp.learnbyzantinemusic.modes.ApichimaSequence
import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.modes.ModeLadders
import com.johnchourp.learnbyzantinemusic.music.ModeLadder

/**
 * Every pitch the lectern's ison bar sounds, from the 8 Ήχοι page's own parts (ClickUp `869f5x2e7`).
 *
 * The bar has no arithmetic of its own: the ison is [IsonDrone.held] — the page's lookup on the page's
 * ladder, [ModeLadders] — «Ίσον σε…» offers [IsonDrone.choices], and the απήχημα is
 * [ApichimaSequence.frequencies] on that same ladder. So a ήχος, a shift and a φθόγγος sound on the
 * lectern exactly as they do on the 8 Ήχοι page, to the last bit (`LecternIsonTest`).
 */
object LecternIson {

    /** The rung [request] holds — its φθόγγος and pitch — or null when its ladder does not hold it. */
    fun held(request: IsonDrone.Request): ModeLadder.Step? = IsonDrone.held(request)

    /** What «Ίσον σε…» offers for [request]'s ήχος. */
    fun choices(request: IsonDrone.Request): IsonDrone.Choices? =
        IsonDrone.choices(request.mode, ModeLadders.ladder(request.mode, request.baseShiftMoria))

    /** The απήχημα's pitches in [request]'s ήχος and shift; null when any step is off the ladder. */
    fun apichimaFrequencies(request: IsonDrone.Request, steps: List<ApichimaSequence.Step>): List<Double>? =
        ApichimaSequence.frequencies(steps, ModeLadders.ladder(request.mode, request.baseShiftMoria))
}
