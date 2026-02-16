package com.johnchourp.learnbyzantinemusic.analysis

data class ModeProfile(
    val id: String,
    val noteHeights: Map<String, Float>
)

data class MelodyTraversal(
    val notes: List<String>,
    val modeHeights: Map<String, Float>
)

class MelodyMapper(
    private val phthongsOrder: List<String>,
    private val modeProfiles: Map<String, ModeProfile>
) {
    fun map(basePhthong: String, deltas: List<Int>, modeId: String): MelodyTraversal {
        if (phthongsOrder.isEmpty()) {
            return MelodyTraversal(emptyList(), emptyMap())
        }
        var currentIndex = phthongsOrder.indexOf(basePhthong).let { if (it < 0) 0 else it }
        val notes = mutableListOf<String>()
        for (delta in deltas) {
            currentIndex = wrapIndex(currentIndex + delta, phthongsOrder.size)
            notes.add(phthongsOrder[currentIndex])
        }

        val profile = modeProfiles[modeId]
        val modeHeights = if (profile != null && profile.noteHeights.isNotEmpty()) {
            phthongsOrder.associateWith { note ->
                profile.noteHeights[note] ?: fallbackHeight(note)
            }
        } else {
            defaultHeights()
        }

        return MelodyTraversal(notes = notes, modeHeights = modeHeights)
    }

    private fun fallbackHeight(note: String): Float {
        val index = phthongsOrder.indexOf(note).let { if (it < 0) 0 else it }
        return if (phthongsOrder.size <= 1) 0f else index.toFloat() / (phthongsOrder.size - 1).toFloat()
    }

    private fun defaultHeights(): Map<String, Float> =
        if (phthongsOrder.size <= 1) {
            phthongsOrder.associateWith { 0f }
        } else {
            phthongsOrder.mapIndexed { index, note ->
                note to (index.toFloat() / (phthongsOrder.size - 1).toFloat())
            }.toMap()
        }

    private fun wrapIndex(value: Int, size: Int): Int {
        val mod = value % size
        return if (mod < 0) mod + size else mod
    }
}
