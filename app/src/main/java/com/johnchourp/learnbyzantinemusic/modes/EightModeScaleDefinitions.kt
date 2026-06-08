package com.johnchourp.learnbyzantinemusic.modes

enum class ModeScaleGenus {
    DIATONIC,
    SOFT_CHROMATIC,
    HARD_CHROMATIC,
    ENHARMONIC
}

enum class ModeScaleBase(val phthong: String) {
    PA("Πα"),
    NI("Νη")
}

data class ModeScaleInterval(
    val from: String,
    val to: String,
    val moria: Int
)

data class ModeScaleDefinition(
    val genus: ModeScaleGenus,
    val base: ModeScaleBase,
    val intervals: List<Int>
) {
    val upperBase: String
        get() = "${base.phthong}΄"

    fun repeatedIntervals(octaves: Int): List<Int> {
        require(octaves > 0) { "octaves must be positive" }
        return buildList(intervals.size * octaves) {
            repeat(octaves) {
                addAll(intervals)
            }
        }
    }

    fun ascendingPhthongs(octaves: Int): List<String> =
        EightModeScaleDefinitions.ascendingPhthongs(base, octaves)

    fun intervalPairs(): List<ModeScaleInterval> {
        val phthongs = EightModeScaleDefinitions.singleOctavePhthongs(base)
        return intervals.mapIndexed { index, moria ->
            ModeScaleInterval(
                from = phthongs[index],
                to = phthongs[index + 1],
                moria = moria
            )
        }
    }

    fun intervalSummary(): String =
        intervalPairs().joinToString(separator = ", ") { "${it.from}-${it.to} ${it.moria}" }

    fun referenceMoriaFromBottom(referencePhthong: String, octaves: Int): Int {
        val labels = ascendingPhthongs(octaves)
        val referenceIndex = labels.indexOf(referencePhthong)
        require(referenceIndex >= 0) {
            "reference phthong $referencePhthong is not present in ${labels.joinToString()}"
        }
        return cumulativeMoria(octaves)[referenceIndex]
    }

    private fun cumulativeMoria(octaves: Int): List<Int> {
        val cumulative = mutableListOf(0)
        var current = 0
        for (interval in repeatedIntervals(octaves)) {
            current += interval
            cumulative.add(current)
        }
        return cumulative
    }
}

object EightModeScaleDefinitions {
    val DIATONIC = ModeScaleDefinition(
        genus = ModeScaleGenus.DIATONIC,
        base = ModeScaleBase.PA,
        intervals = listOf(10, 8, 12, 12, 10, 8, 12)
    )

    val SOFT_CHROMATIC = ModeScaleDefinition(
        genus = ModeScaleGenus.SOFT_CHROMATIC,
        base = ModeScaleBase.PA,
        intervals = listOf(6, 20, 4, 12, 6, 20, 4)
    )

    val HARD_CHROMATIC = ModeScaleDefinition(
        genus = ModeScaleGenus.HARD_CHROMATIC,
        base = ModeScaleBase.NI,
        intervals = listOf(8, 14, 8, 12, 8, 14, 8)
    )

    val ENHARMONIC = ModeScaleDefinition(
        genus = ModeScaleGenus.ENHARMONIC,
        base = ModeScaleBase.NI,
        intervals = listOf(12, 12, 6, 12, 12, 6, 12)
    )

    val MODE_SCALES: Map<String, ModeScaleDefinition> = linkedMapOf(
        "first" to DIATONIC,
        "second" to SOFT_CHROMATIC,
        "third" to ENHARMONIC,
        "fourth" to DIATONIC,
        "plagal_first" to DIATONIC,
        "plagal_second" to HARD_CHROMATIC,
        "varys" to ENHARMONIC,
        "plagal_fourth" to DIATONIC
    )

    fun ascendingPhthongs(base: ModeScaleBase, octaves: Int): List<String> {
        require(octaves > 0) { "octaves must be positive" }
        val startIndex = PHTHONGS_ORDER.indexOf(base.phthong)
        require(startIndex >= 0) { "Unknown base phthong ${base.phthong}" }

        var suffixLevel = LOW_OCTAVE_SUFFIX_LEVEL
        var currentIndex = startIndex
        val labels = mutableListOf(decoratePhthong(PHTHONGS_ORDER[currentIndex], suffixLevel))
        repeat(PHTHONGS_ORDER.size * octaves) {
            currentIndex = (currentIndex + 1) % PHTHONGS_ORDER.size
            val phthong = PHTHONGS_ORDER[currentIndex]
            if (phthong == NI) {
                suffixLevel += 1
            }
            labels.add(decoratePhthong(phthong, suffixLevel))
        }
        return labels
    }

    fun singleOctavePhthongs(base: ModeScaleBase): List<String> {
        val rotated = phthongsFrom(base.phthong)
        return rotated + "${base.phthong}΄"
    }

    private fun phthongsFrom(basePhthong: String): List<String> {
        val startIndex = PHTHONGS_ORDER.indexOf(basePhthong)
        require(startIndex >= 0) { "Unknown base phthong $basePhthong" }
        return PHTHONGS_ORDER.drop(startIndex) + PHTHONGS_ORDER.take(startIndex)
    }

    private fun decoratePhthong(phthong: String, suffixLevel: Int): String {
        val suffix = when {
            suffixLevel < 0 -> ","
            suffixLevel == 0 -> ""
            else -> "΄".repeat(suffixLevel)
        }
        return phthong + suffix
    }

    private const val NI = "Νη"
    private const val LOW_OCTAVE_SUFFIX_LEVEL = -1
    private val PHTHONGS_ORDER = listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω")
}
