package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import com.johnchourp.learnbyzantinemusic.music.ShownBeats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Normalizer

/**
 * One name per sign, in every language, held by one string key (ClickUp `869f5x29j`, H4).
 *
 * Before H4 most signs had two name keys (`gorgo` and `name_gorgo`, `ison` and `name_ison` …) plus a
 * TalkBack string repeating the name, and the copies had drifted: «Γοργό» / «Γοργόν», «Ενδόφωνο»
 * without its ν, English «Even», «A little bit» and «Flyer» beside «Ison», «Oligon» and «Petasti» —
 * and in English the κέντημα and the κεντήματα were both «Embroidery». The decision (audit
 * 2026-09-25): Greek names in -ον, English names transliterated.
 */
class OneNamePerSignTest {

    private val languages = ShownBeats.languages

    private val named: List<Neume> = Neume.entries.filter { it.nameRes != 0 }

    /** Case, accents and surrounding space do not make two names different. */
    private fun key(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD).filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }

    private fun nameIn(folder: String, sign: Neume): String =
        languages.getValue(folder)[ShownBeats.nameOf(sign.nameRes)] ?: error("$folder has no name for $sign")

    @Test
    fun theSweepCoversEveryLanguageAndEveryNamedSign() {
        // Guards the slice: with no language or no sign loaded, every check below passes trivially.
        assertTrue("found ${languages.keys}", listOf("values", "values-en").all { it in languages })
        assertEquals("named signs", 28, named.size)
    }

    @Test
    fun noTwoSignsShareANameInAnyLanguage() {
        languages.keys.forEach { folder ->
            val clashes = named.groupBy { key(nameIn(folder, it)) }.filterValues { it.size > 1 }
            assertEquals("$folder: two signs, one name", emptyMap<String, List<Neume>>(), clashes)
        }
    }

    @Test
    fun everySignHasOneStringKey() {
        // No second key holds a sign's name: not a name_X twin, not a screen's own copy of it.
        languages.forEach { (folder, strings) ->
            named.forEach { sign ->
                val own = ShownBeats.nameOf(sign.nameRes)
                val name = key(nameIn(folder, sign))
                val copies = strings.filter { (k, text) -> k != own && key(text) == name }.keys
                assertEquals("$folder: «${nameIn(folder, sign)}» is also held by", emptySet<String>(), copies)
            }
        }
        val keys = languages.getValue("values").keys
        named.map { ShownBeats.nameOf(it.nameRes) }.forEach { own ->
            assertTrue("name_$own is back", "name_$own" !in keys)
            assertTrue("cd_$own is back: TalkBack reads the name through cd_neume / cd_quality_sign", "cd_$own" !in keys)
        }
    }

    @Test
    fun theTalkBackTextIsTheKindAndTheName() {
        NeumeKind.entries.forEach { kind ->
            languages.forEach { (folder, strings) ->
                val template = strings.getValue(ShownBeats.nameOf(kind.descriptionRes))
                val slot = "%1\$s"
                assertEquals("$folder/$kind «$template»", 1, Regex.fromLiteral(slot).findAll(template).count())
                assertTrue("$folder/$kind «$template» says nothing but the name", template.replace(slot, "").isNotBlank())
            }
        }
    }

    /** The decided names, (Greek, English), per sign. */
    private val decided: Map<Neume, Pair<String, String>> = mapOf(
        Neume.ISON to ("Ίσον" to "Ison"),
        Neume.OLIGON to ("Ολίγον" to "Oligon"),
        Neume.PETASTI to ("Πεταστή" to "Petasti"),
        Neume.KENTIMATA to ("Κεντήματα" to "Kentimata"),
        Neume.KENTIMA to ("Κέντημα" to "Kentima"),
        Neume.YPSILI to ("Υψηλή" to "Ypsili"),
        Neume.APOSTROPHOS to ("Απόστροφος" to "Apostrophos"),
        Neume.ELAFRON to ("Ελαφρόν" to "Elafron"),
        Neume.YPORROI to ("Υπορροή" to "Yporroi"),
        Neume.CHAMILI to ("Χαμηλή" to "Chamili"),
        Neume.KLASMA to ("Κλάσμα" to "Klasma"),
        Neume.APLI to ("Απλή" to "Apli"),
        Neume.DIPLI to ("Διπλή" to "Dipli"),
        Neume.TRIPLI to ("Τριπλή" to "Tripli"),
        Neume.GORGON to ("Γοργόν" to "Gorgon"),
        Neume.DIGORGON to ("Δίγοργον" to "Digorgon"),
        Neume.TRIGORGON to ("Τρίγοργον" to "Trigorgon"),
        Neume.ARGON to ("Αργόν" to "Argon"),
        Neume.DIARGON to ("Δίαργον" to "Diargon"),
        Neume.TRIARGON to ("Τρίαργον" to "Triargon"),
        Neume.VAREIA to ("Βαρεία" to "Vareia"),
        Neume.YFEN to ("Υφέν" to "Yfen"),
        Neume.SYNECHES_ELAFRON to ("Συνεχές ελαφρόν" to "Syneches Elafron"),
        Neume.PSIFISTON to ("Ψηφιστόν" to "Psifiston"),
        Neume.OMALON to ("Ομαλόν" to "Omalon"),
        Neume.ANTIKENOMA to ("Αντικένωμα" to "Antikenoma"),
        Neume.SYNDESMOS to ("Σύνδεσμος" to "Syndesmos"),
        Neume.ENDOFONON to ("Ενδόφωνον" to "Endofonon"),
    )

    @Test
    fun theNamesAreTheOnesDecided() {
        assertEquals("every named sign has a decided name", named.toSet(), decided.keys)
        decided.forEach { (sign, names) ->
            assertEquals("$sign in Greek", names.first, nameIn("values", sign))
            assertEquals("$sign in English", names.second, nameIn("values-en", sign))
        }
    }
}
