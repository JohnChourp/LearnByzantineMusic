package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.PhthongName.KE
import com.johnchourp.learnbyzantinemusic.music.PhthongName.NI
import com.johnchourp.learnbyzantinemusic.music.PhthongName.PA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.VOU
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Expected melodies and starting φθόγγοι saved before ClickUp `869f5x291` (H2) were written through
 * the Melody Trainer's former `TrainerPhthong` enum. After the merge into [PhthongName] they must read
 * back exactly as they did.
 *
 * [legacyDecodeList] and [legacyDecode] are the old `AnalysisSettingsStore` reader, verbatim in
 * behaviour: split on commas, keep each token that equals a `TrainerPhthong` constant name — NI, PA,
 * VOU, GA, DI, KE, ZO — and drop the rest. They are spelled out here, not derived from [PhthongName],
 * so that renaming a constant cannot change both sides at once.
 */
class StoredPhthongsTest {

    private val legacyNames = listOf("NI", "PA", "VOU", "GA", "DI", "KE", "ZO")

    private fun legacyDecodeList(stored: String?): List<String> =
        stored?.split(',')?.mapNotNull { name -> legacyNames.firstOrNull { it == name } }.orEmpty()

    private fun legacyDecode(stored: String?): String? =
        stored?.let { name -> legacyNames.firstOrNull { it == name } }

    @Test
    fun aSavedMelodyReadsBackNoteForNote() {
        assertEquals(listOf(NI, PA, VOU), StoredPhthongs.decodeList("NI,PA,VOU"))
    }

    @Test
    fun aSavedStartingPhthongReadsBack() {
        assertEquals(KE, StoredPhthongs.decode("KE"))
        assertEquals(listOf(KE), StoredPhthongs.decodeList("KE"))
    }

    @Test
    fun anEmptyMelodyIsStoredAsAnEmptyStringAndReadsBackEmpty() {
        assertEquals("", StoredPhthongs.encodeList(emptyList()))
        assertEquals(emptyList<PhthongName>(), StoredPhthongs.decodeList(""))
        assertEquals("nothing stored yet", emptyList<PhthongName>(), StoredPhthongs.decodeList(null))
        assertNull(StoredPhthongs.decode(null))
    }

    @Test
    fun anUnknownTokenIsDroppedAndTheRestIsKept() {
        assertEquals(listOf(NI, PA), StoredPhthongs.decodeList("NI,XX,PA"))
        assertNull(StoredPhthongs.decode("XX"))
    }

    @Test
    fun tokensAreMatchedExactlyWithNoTrimmingOrCaseFolding() {
        // What every earlier version did: " PA" and "ni" are unknown, so they are dropped.
        assertEquals(listOf(VOU), StoredPhthongs.decodeList("ni, PA,VOU"))
        assertNull(StoredPhthongs.decode("ke"))
        assertNull(StoredPhthongs.decode(" KE"))
    }

    @Test
    fun everyStoredValueDecodesExactlyAsTheOldReaderDid() {
        val pairs = legacyNames.flatMap { a -> legacyNames.map { b -> "$a,$b" } }
        val corpus = listOf(
            null, "", ",", "NI,PA,VOU", "KE", "ZO,NI", "NI,XX,PA", "XX", "ni", " KE", "KE ",
            "NI,,PA", "Νη", "Ni΄", "VOU,VOU,VOU", legacyNames.joinToString(","),
        ) + pairs
        assertTrue("the corpus must exercise every pair of names", corpus.size > 60)
        corpus.forEach { stored ->
            assertEquals("list «$stored»", legacyDecodeList(stored), StoredPhthongs.decodeList(stored).map { it.name })
            assertEquals("single «$stored»", legacyDecode(stored), StoredPhthongs.decode(stored)?.name)
        }
    }

    @Test
    fun whatIsWrittenIsTheSameStringAsBefore() {
        assertEquals("NI,PA,VOU,GA,DI,KE,ZO", StoredPhthongs.encodeList(PhthongName.entries))
        assertEquals("KE", StoredPhthongs.encode(KE))
        PhthongName.entries.forEach { phthong ->
            assertEquals(phthong, StoredPhthongs.decode(StoredPhthongs.encode(phthong)))
        }
    }
}
