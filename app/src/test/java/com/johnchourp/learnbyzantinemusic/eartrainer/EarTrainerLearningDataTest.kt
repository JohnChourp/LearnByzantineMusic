package com.johnchourp.learnbyzantinemusic.eartrainer

import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Result.Accepted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «Μάντεψε τον ήχο» in the «Δεδομένα μάθησης» file (ClickUp `869f4tqb9`): the scores and the level
 * go with the learner to a new phone, in the one form the app writes; an empty record or an unknown
 * level is never carried, so an import cannot wipe or corrupt what the new phone has.
 */
class EarTrainerLearningDataTest {

    private fun exported(stored: Map<String, Any>): Map<AppPrefs.Store, Map<String, Any>> {
        val file = LearningDataFile.encode(mapOf(AppPrefs.Store.EAR_TRAINER to stored), 1L, "1.19.0")
        val result = LearningDataFile.decode(file)
        assertTrue("expected the export to import back, got $result", result is Accepted)
        return (result as Accepted).changes
    }

    @Test
    fun theScoresAndTheLevelTravelInTheFormTheAppWritesThem() {
        // The same ήχος twice: what a damaged or hand-edited preference could hold.
        val stored = """{"schemaVersion":1,"modes":[{"mode":"varys","answered":1,"correct":0},{"mode":"varys","answered":2,"correct":2}]}"""
        val asTheAppWritesIt = EarTrainerScoresCodec.encode(EarTrainerScores(mapOf(Mode.VARYS to ModeScore(3, 2))))

        val changes = exported(mapOf("ear_trainer_scores" to stored, "ear_trainer_level" to "all_without_ison"))

        assertEquals(
            mapOf(
                AppPrefs.Store.EAR_TRAINER to mapOf(
                    "ear_trainer_scores" to asTheAppWritesIt,
                    "ear_trainer_level" to "all_without_ison",
                ),
            ),
            changes,
        )
    }

    @Test
    fun anEmptyRecordOrAnUnknownLevelIsNotCarried() {
        listOf("", "not json", """{"schemaVersion":1,"modes":[]}""").forEach { stored ->
            assertEquals("stored: $stored", emptyMap<AppPrefs.Store, Map<String, Any>>(), exported(mapOf("ear_trainer_scores" to stored)))
        }
        assertEquals(emptyMap<AppPrefs.Store, Map<String, Any>>(), exported(mapOf("ear_trainer_level" to "expert")))
    }
}
