package com.johnchourp.learnbyzantinemusic.voice

/**
 * The steps of «Βρες τη φωνή σου», as values (ClickUp `869f5x2dd`, J4): the singer holds the lowest
 * comfortable note, then the highest, and gets a suggestion — which nothing applies until they accept.
 *
 * Immutable on purpose: the dialog keeps one of these in its state and replaces it, and a test can
 * walk the same steps with no microphone. The listening itself is the host's; this only collects what
 * it hears while a step listens ([hear]), and decides when a step has heard enough ([canFinishStep]).
 */
data class VoiceRangeTest(
    val step: Step = Step.INTRO,
    /** What was heard during the current step, in Hz. */
    val heardHz: List<Double> = emptyList(),
    val lowestHz: Double? = null,
    val advice: VoiceRangeAdvisor.Advice? = null,
) {
    enum class Step {
        /** What the test is, and «Ξεκίνα». The microphone is asked for only from here. */
        INTRO,

        /** Hold the lowest note you sing comfortably. */
        LOWEST,

        /** Now the highest. */
        HIGHEST,

        /** The suggestion, to accept or not. */
        RESULT,

        /** The two notes do not make a range: sing them again. */
        RETRY,
    }

    /** Whether the microphone should be on. */
    val listening: Boolean get() = step == Step.LOWEST || step == Step.HIGHEST

    /** A step can end once it has heard a held note. */
    val canFinishStep: Boolean get() = listening && VoiceRangeAdvisor.heldPitchHz(heardHz) != null

    /** From the introduction, or after a failed try: listen for the lowest note. */
    fun start(): VoiceRangeTest = VoiceRangeTest(step = Step.LOWEST)

    /** One detection from the pitch engine; ignored unless a step is listening. */
    fun hear(frequencyHz: Double?): VoiceRangeTest {
        if (!listening || frequencyHz == null) return this
        if (frequencyHz !in VoiceRangeAdvisor.LOWEST_HEARD_HZ..VoiceRangeAdvisor.HIGHEST_HEARD_HZ) return this
        return copy(heardHz = (heardHz + frequencyHz).takeLast(VoiceRangeAdvisor.MAX_SAMPLES))
    }

    /** Ends the listening step: the lowest note leads to the highest, the highest to the result. */
    fun finishStep(): VoiceRangeTest {
        val held = VoiceRangeAdvisor.heldPitchHz(heardHz) ?: return this
        return when (step) {
            Step.LOWEST -> copy(step = Step.HIGHEST, heardHz = emptyList(), lowestHz = held)
            Step.HIGHEST -> {
                val advice = lowestHz?.let { VoiceRangeAdvisor.advise(it, held) }
                if (advice == null) {
                    VoiceRangeTest(step = Step.RETRY)
                } else {
                    copy(step = Step.RESULT, heardHz = emptyList(), advice = advice)
                }
            }
            else -> this
        }
    }
}
