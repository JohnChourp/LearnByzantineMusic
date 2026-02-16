package com.johnchourp.learnbyzantinemusic.analysis

data class NeumeRhythmInput(
    val modifiers: List<String>,
    val baseDurationBeats: Float = 1f
)

class ByzantineRhythmMapper {
    fun map(inputs: List<NeumeRhythmInput>): List<Float> {
        val durations = inputs.map { input ->
            var duration = input.baseDurationBeats.coerceAtLeast(0.5f)
            if (input.modifiers.contains(MODIFIER_FRACTION)) {
                duration += 1f
            }
            if (input.modifiers.contains(MODIFIER_ANTIKENO) && input.modifiers.contains(MODIFIER_APLI)) {
                duration += 1f
            }
            duration
        }.toMutableList()

        for (index in inputs.indices) {
            val hasGorgo = inputs[index].modifiers.contains(MODIFIER_GORGO)
            if (!hasGorgo) {
                continue
            }
            durations[index] = 0.5f
            if (index > 0) {
                durations[index - 1] = maxOf(0.5f, durations[index - 1] - 0.5f)
            }
        }

        return durations
    }

    companion object {
        const val MODIFIER_FRACTION = "fraction"
        const val MODIFIER_GORGO = "gorgo"
        const val MODIFIER_ANTIKENO = "antikeno"
        const val MODIFIER_APLI = "apli"
    }
}
