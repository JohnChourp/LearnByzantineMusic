package com.johnchourp.learnbyzantinemusic.music

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How the app judges a sung voice — **one** profile for every screen that listens (ClickUp
 * `869f5x28t`, H1).
 *
 * Three screens tell a singer whether they are on the φθόγγος: «Πού είμαι» on the 8 Ήχοι page, the
 * Melody Trainer (the voice check, and φθόγγος + time) and the recording analysis (the summary, the
 * chips and the diagram). Each used to carry its own copy of the rule, and the copies had drifted:
 * two constants with the **same name**, `IN_TUNE_MORIA`, worth 3 on «Πού είμαι» and 4 in the
 * analysis, and a third worth 4 in the Trainer. The same voice was «μέσα» on one screen and «έξω»
 * on the next. The silence gate was declared twice, and the duration thresholds were counted in
 * frames of two different lengths. Everything now reads from here: `SingleIntonationSourceTest`
 * fails if a second copy appears, and `OneToleranceOnEveryScreenTest` if a screen judges differently.
 *
 * ## The values
 *
 * 1 μόριο = 1200 / 72 ≈ 16.7 cents. «Recording frames» are the analysis frames of a saved recording.
 *
 * | Value | Equals | Read by | Why |
 * |---|---|---|---|
 * | [IN_TUNE_MORIA] ±3 μόρια | ±50 cents | `LadderPitchMirror`, `PitchGreeningEvaluator`, `ComboPitchGate`, `SungNote` | one verdict on three screens — see *Why ±3* |
 * | [SILENCE_RMS] 0.012 | ≈ 38 dB below full scale | `TrainerPitchEngine`, `PitchTrackAnalyzer` | quieter is silence: hiss never becomes a note |
 * | [WINDOW_MS] 46.44 ms | 2048 samples @ 44.1 kHz = 1024 @ 22.05 kHz | `TrainerPitchEngine`, `PitchTrackAnalyzer` | a recording is heard as the live mic is heard |
 * | [LIVE_HOP_MS] = [WINDOW_MS] | 1 live frame | `PitchGreeningEvaluator` counts in these frames | the mic is read one window after another |
 * | [OFFLINE_HOP_MS] 23.22 ms | 512 samples @ 22.05 kHz | `PitchTrackAnalyzer`, `PhthongSegmenter` | offline, windows overlap by half: finer timing |
 * | [LIVE_MIN_STABLE_MS] 139 ms | 3 live frames | `PitchGreeningEvaluator` | held this long, a φθόγγος commits the check |
 * | [OFFLINE_MIN_NOTE_MS] 93 ms | 4 recording frames | `PhthongSegmenter` | shorter is a glide, a consonant or a blip |
 * | [OFFLINE_MAX_GAP_MS] 46 ms | 2 recording frames | `PhthongSegmenter` | a shorter gap does not split a note |
 * | [OFFLINE_STEADY_MS] 116 ms | 5 recording frames | `PhthongSegmenter.calibrate` | the first steady stretch is the starting note |
 * | [WAIT_HOLD_MS] 300 ms | 7 live frames, measured in capture time | `WaitModeEvaluator` | held this long, «Παραλλαγή με αναμονή» moves on |
 * | [WAIT_TOLERANCES_MORIA] ±3 → ±2.5 → ±2 | 50 → 42 → 33 cents | `WaitModeEvaluator` | see *The narrowing tolerance of J1* |
 *
 * The window is the same on both sides so that a recording is analysed with the ear the live
 * Trainer listens with; half of it (23.2 ms) is the longest lag YIN may search, which clears the
 * 14.3 ms period of its 70 Hz floor.
 *
 * **Milliseconds, converted where they are used.** The thresholds used to be written as frame
 * counts, and a frame is 46.4 ms live but 23.2 ms in a recording, so the same number meant two
 * different durations. They are now durations, and each consumer turns them into samples or frames
 * with [samplesIn] / [framesIn], which round to the nearest whole one. The values above were chosen
 * to reproduce **exactly** the counts in use before: 2048 / 1024 / 512 samples and 3 / 4 / 2 / 5
 * frames. `IntonationProfileTest` pins every one of them.
 *
 * ## Why ±3 — and why the Trainer and the analysis became stricter
 *
 * Every screen first finds the **nearest rung** and only then asks whether the voice is close
 * enough to it. A tolerance of at least half the gap to a neighbour can therefore never say «έξω»
 * on that side: any pitch between two rungs is within half a gap of one of them, and is simply
 * given to that one. The diatonic Βου–Γα and Ζω–Νη΄ steps are 8 μόρια, so the old ±4 sat exactly on
 * that limit: between Βου and Γα the Trainer and the analysis could call a voice the neighbouring
 * φθόγγος, but never out of tune. ±3 leaves a 2-μόρια band in the middle of every 8-μόρια step where
 * the verdict is «έξω»; a voice 3.5 μόρια above Βου is now out on all three screens
 * (`NearestRungFirstTest`). ±3 is also what «Πού είμαι» already used.
 *
 * **Known limit.** By the same rule, a step of 6 μόρια or less can never say «έξω» even at ±3: the
 * hard chromatic scale (πλ. Β΄) has 4 μόρια at Βου–Γα and Ζω–Νη΄ and 6 at Νη–Πα and Δι–Κε, and the
 * enharmonic one (Γ΄, Βαρύς) has 6 at Βου–Γα and Κε–Ζω. A voice between those rungs is always given
 * to one of them. This touches every screen that reads a mode's own scale: «Πού είμαι», the analysis,
 * and — since F2 (ClickUp `869f5x24v`) — the Trainer once a ήχος is chosen; «Διατονικός» has no step
 * under 8. Closing it needs a tolerance under ±2 μόρια or one per step, and neither is decided, so the
 * limit is written here and pinned by `NearestRungFirstTest`.
 *
 * ## The narrowing tolerance of J1
 *
 * «Παραλλαγή με αναμονή» (ClickUp `869f5x2cd`) waits on each φθόγγος until it is held for
 * [WAIT_HOLD_MS], and asks for more as the learner gets better: the first run is judged at ±3 — the
 * same as every other screen — and each run through the whole line **without a skip** narrows it one
 * level ([nextWaitLevel]), down to ±2. A run with a skip keeps the level; nothing ever widens it back
 * by itself, and a new visit to the Trainer starts again at ±3.
 *
 * The nearest-rung rule above decides where a level can matter: a level says «έξω» on a side of a step
 * only when it is **under half that step**.
 *
 * | Level | ± μόρια | «έξω» possible on a step of | So, by genus |
 * |---|---|---|---|
 * | 0 | 3 | more than 6 μόρια | diatonic and soft chromatic: every step (the smallest is 8) |
 * | 1 | 2.5 | more than 5 | also the 6-μόρια steps of the enharmonic and the hard chromatic |
 * | 2 | 2 | more than 4 | the same — the hard chromatic's 4-μόρια steps can never say «έξω» |
 *
 * So on the hard chromatic's Βου–Γα and Ζω–Νη΄ narrowing changes nothing: a voice past the middle of
 * that step is simply on the neighbour, at every level. `WaitModeToleranceTest` pins the table.
 *
 * [IN_TUNE_MORIA] is a whole number on purpose: the analysis summary prints it as «±N μόρια».
 *
 * The recording side used to gate silence on the Float `0.012f` and the live side on the Double
 * `0.012`. The two spellings differ by 10⁻¹⁰, far below the 3·10⁻⁵ step of a 16-bit sample, so one
 * Double serves both.
 */
object IntonationProfile {

    /** Within this many μόρια of the nearest rung, the voice is on the φθόγγος. ±50 cents. */
    const val IN_TUNE_MORIA: Double = 3.0

    /** RMS of a window (samples scaled to −1..1) below which the window is silence, not a note. */
    const val SILENCE_RMS: Double = 0.012

    /** One analysis window, live and offline alike: 2048 samples at 44.1 kHz, 1024 at 22.05 kHz. */
    const val WINDOW_MS: Double = 46.44

    /** Live frames follow each other: the microphone is read one whole window after another. */
    const val LIVE_HOP_MS: Double = WINDOW_MS

    /** Offline frames start half a window apart: 512 samples at the decoder's 22.05 kHz. */
    const val OFFLINE_HOP_MS: Double = 23.22

    /** How long a φθόγγος must be held before the Trainer's voice check commits a verdict. */
    const val LIVE_MIN_STABLE_MS: Double = 139.0

    /** The shortest run on one degree that a recording's analysis counts as a note. */
    const val OFFLINE_MIN_NOTE_MS: Double = 93.0

    /** The longest silence bridged inside one note; a longer one separates a repeated note. */
    const val OFFLINE_MAX_GAP_MS: Double = 46.0

    /** How long the first steady stretch must last to calibrate a recording to the singer. */
    const val OFFLINE_STEADY_MS: Double = 116.0

    /** How long «Παραλλαγή με αναμονή» needs a φθόγγος held, in capture time, before the line moves on. */
    const val WAIT_HOLD_MS: Double = 300.0

    /** The narrowing levels of «Παραλλαγή με αναμονή», loosest first; the first is [IN_TUNE_MORIA]. */
    val WAIT_TOLERANCES_MORIA: List<Double> = listOf(IN_TUNE_MORIA, 2.5, 2.0)

    /** The ± μόρια of narrowing [level], held inside the table at both ends. */
    fun waitTolerance(level: Int): Double = WAIT_TOLERANCES_MORIA[level.coerceIn(0, WAIT_TOLERANCES_MORIA.lastIndex)]

    /** The level after a run through the whole line: one narrower after a run without a skip, else the same. */
    fun nextWaitLevel(level: Int, withoutSkips: Boolean): Int {
        val current = level.coerceIn(0, WAIT_TOLERANCES_MORIA.lastIndex)
        return if (withoutSkips) (current + 1).coerceAtMost(WAIT_TOLERANCES_MORIA.lastIndex) else current
    }

    /**
     * The one in-tune rule: [deviationMoria] from the nearest rung, inclusive at the boundary.
     * [toleranceMoria] exists for tests of the mechanics; every screen uses the default.
     */
    fun isInTune(deviationMoria: Double, toleranceMoria: Double = IN_TUNE_MORIA): Boolean =
        abs(deviationMoria) <= toleranceMoria

    /** [durationMs] as a whole number of samples at [sampleRate], rounded to the nearest. */
    fun samplesIn(durationMs: Double, sampleRate: Int): Int =
        (durationMs * sampleRate / 1000.0).roundToInt()

    /** [durationMs] as a whole number of analysis frames that start [hopMs] apart, rounded to the nearest. */
    fun framesIn(durationMs: Double, hopMs: Double): Int =
        (durationMs / hopMs).roundToInt()
}
