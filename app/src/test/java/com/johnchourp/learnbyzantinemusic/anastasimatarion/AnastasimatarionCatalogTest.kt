package com.johnchourp.learnbyzantinemusic.anastasimatarion

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/** Integrity of the bundled catalog asset (app/src/main/assets/anastasimatarion_v1.json). */
class AnastasimatarionCatalogTest {

    companion object {
        private const val CODE_LOCK = "scripts/anastasimatarion-codes.lock.json"
        private lateinit var catalog: HymnCatalog

        @BeforeClass
        @JvmStatic
        fun loadAsset() {
            val asset = File("src/main/assets/${AnastasimatarionCatalogLoader.ASSET_NAME}")
            catalog = AnastasimatarionCatalogParser.parse(asset.readText(Charsets.UTF_8))
        }
    }

    @Test
    fun hasTheEightModesInCycleOrder() {
        assertEquals(AnastasimatarionLabels.MODE_ORDER, catalog.modes.map { it.key })
    }

    @Test
    fun everyModeFollowsTheSundayServiceStructure() {
        for (mode in catalog.modes) {
            assertEquals(mode.key, listOf("vespers", "orthros", "liturgy"), mode.services.map { it.key })
            val counts = mode.services.flatMap { it.groups }.associate { it.key to it.hymns.size }
            assertEquals(mode.key, 1, counts["kekragarion"])
            assertEquals(mode.key, 3, counts["stichera_anastasima"])
            assertEquals(mode.key, 4, counts["anatolika"])
            assertEquals(mode.key, 1, counts["dogmatikon"])
            assertEquals(mode.key, 5, counts["aposticha"])
            assertEquals(mode.key, 2, counts["apolytikion"])
            assertEquals(mode.key, 6, counts["kathismata"])
            assertEquals(mode.key, 1, counts["ypakoe"])
            assertEquals(mode.key, 1, counts["prokeimenon"])
            assertEquals(mode.key, 8, counts["heirmoi"])
            assertEquals(mode.key, 1, counts["kontakion"])
            assertEquals(mode.key, 4, counts["ainoi_anastasima"])
            assertEquals(mode.key, 4, counts["ainoi_anatolika"])
            assertEquals(mode.key, 1, counts["makarismoi"])
            // Three antiphons in every mode except πλ. Δ΄, which has four.
            assertEquals(mode.key, if (mode.key == "plagal_fourth") 4 else 3, counts["anavathmoi"])
        }
    }

    /**
     * Codes are permanent keys, not positions (ClickUp `869f5x2a9`). A hymn added early in a mode
     * takes the next free code at the END, so codes need not follow the display order: the rule
     * this replaces — 01..N in display order — demanded exactly the renumbering that re-attaches
     * recordings to another hymn. Which hymn each code names is AnastasimatarionCodeLockTest's job.
     */
    @Test
    fun codesAreTwoDigitUniquePerModeAndLocked() {
        val lockFile = listOf(File("../$CODE_LOCK"), File(CODE_LOCK)).firstOrNull { it.isFile }
            ?: error("$CODE_LOCK not found from ${File("").absolutePath}")
        val lockModes = JSONObject(lockFile.readText()).getJSONObject("modes")
        for (mode in catalog.modes) {
            val codes = mode.hymns.map { it.code }
            assertEquals("${mode.key}: not two digits", emptyList<String>(), codes.filterNot { Regex("""\d{2}""").matches(it) })
            assertEquals("${mode.key}: a code given twice", codes.size, codes.toSet().size)
            val lockedHymns = lockModes.getJSONObject(mode.key).getJSONArray("hymns")
            val locked = (0 until lockedHymns.length()).map { lockedHymns.getJSONObject(it).optString("code") }.toSet()
            assertEquals("${mode.key}: codes missing from the lock", emptyList<String>(), codes.filterNot { it in locked })
        }
    }

    @Test
    fun incipitsAreNonBlankAndStartWithACapital() {
        for (mode in catalog.modes) {
            for (hymn in mode.hymns) {
                assertTrue("${mode.key} ${hymn.code}", hymn.incipit.isNotBlank())
                val first = hymn.incipit.first()
                assertTrue("${mode.key} ${hymn.code}: ${hymn.incipit}", first == first.uppercaseChar())
                assertTrue("${mode.key} ${hymn.code}: ${hymn.incipit}", hymn.incipit.length <= 60)
            }
        }
    }

    @Test
    fun everyKeyAndNoteHasALabel() {
        for (mode in catalog.modes) {
            assertNotNull(mode.key, AnastasimatarionLabels.MODE_NAMES[mode.key])
            for (service in mode.services) {
                assertNotNull(service.key, AnastasimatarionLabels.SERVICE_NAMES[service.key])
                for (group in service.groups) {
                    assertNotNull(group.key, AnastasimatarionLabels.GROUP_NAMES[group.key])
                    for (hymn in group.hymns) {
                        val note = hymn.note ?: continue
                        assertNotNull("${mode.key} ${hymn.code} note «$note»", AnastasimatarionLabels.note(note).res)
                    }
                }
            }
        }
    }

    @Test
    fun heirmoiAreLabelledWithTheirOdes() {
        val expected = listOf("α΄", "γ΄", "δ΄", "ε΄", "ς΄", "ζ΄", "η΄", "θ΄").map { "Ωδή $it" }
        for (mode in catalog.modes) {
            val heirmoi = mode.services.flatMap { it.groups }.first { it.key == "heirmoi" }.hymns
            assertEquals(mode.key, expected, heirmoi.map { it.note })
        }
    }

    @Test
    fun findReturnsTheHymnWithItsPlace() {
        val ref = catalog.mode("first")!!.find("02")!!
        assertEquals("vespers", ref.serviceKey)
        assertEquals("stichera_anastasima", ref.groupKey)
        assertEquals("Τὰς ἑσπερινὰς ἡμῶν εὐχάς", ref.hymn.incipit)
        assertNull(catalog.mode("first")!!.find("99"))
    }

    /**
     * The catalog must not carry a source or a URL — the same confidential policy the εορτολόγιο
     * dataset follows (ClickUp `869dbkkwf`, decided 2026-09-22). This is the parsed-side check;
     * `AnastasimatarionAssetSchemaTest` guards the raw asset text.
     */
    @Test
    fun theParsedCatalogExposesNoSource() {
        val fields = HymnCatalog::class.java.declaredFields.map { it.name }
        assertFalse("sourceUrl" in fields)
        assertFalse("sourceTitle" in fields)
    }
}
