package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.anastasimatarion.HymnFolders
import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * One octave mark per direction — `,` below, `΄` (U+0384) above — and one renderer, `Phthong.label`
 * (ClickUp `869f5x291`, H2).
 *
 * There were four marks: the Melody Trainer built its own labels, the recording analysis marked the
 * low octave with `͵` (U+0375), and the English theory text wrote `Ni′` with a prime (U+2032). None
 * of that was stored — only [PhthongName] constant names ever are — so it was a display question,
 * and this is the check that keeps it answered. Three rules, because a mark can arrive three ways:
 *
 * 1. a φθόγγος name followed by a foreign mark, in the app's code **or** its strings (`Ni′`);
 * 2. a foreign mark anywhere in the app's code, which catches `name + "͵".repeat(…)` — a label
 *    assembled around a variable, with no φθόγγος name in the text for rule 1 to see;
 * 3. an octave mark repeated anywhere but `Phthong.label`, which is how a second renderer looks.
 *
 * Code means Kotlin with the comments removed and the string literals kept — the shared
 * `KotlinSource.withoutComments` — because a comment explaining the history is not a mark on
 * screen, while a literal is exactly what the scan hunts. The marks below are written as `\u`
 * escapes because two of them, U+02B9 and U+0374, cannot be told apart on screen.
 *
 * The `΄` in «Ήχος Α΄» is a Greek numeral sign after a mode letter, not an octave mark after a
 * φθόγγος — and those names are folders on users' devices. The last test pins them byte for byte.
 */
class OneOctaveMarkPerDirectionTest {

    private val appRoot: File get() = File(KotlinSource.srcDir, "main")

    private val kotlinSources: List<File> by lazy {
        File(appRoot, "java").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private val stringFiles: List<File> by lazy {
        File(appRoot, "res").listFiles { dir -> dir.isDirectory && dir.name.startsWith("values") }.orEmpty()
            .map { File(it, "strings.xml") }
            .filter { it.isFile }
    }

    private val names = listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω") +
        // The English theory prose transliterates (the phthong_* strings), so its names are checked too.
        listOf("Ni", "Pa", "Vou", "Bou", "Ga", "Di", "Ke", "Zo")

    /** U+0375 ͵, U+2032 ′, U+02B9 ʹ, U+0374 ʹ, U+00B4 ´, U+2019 ’ — every look-alike of the two marks. */
    private val foreignMarks = "\u0375\u2032\u02B9\u0374\u00B4\u2019"

    /** Rule 1: a φθόγγος name, as a whole word, followed by a foreign mark. */
    private val nameThenForeignMark = Regex(
        "(?<!\\p{L})(?:" + names.joinToString("|") + ")[" + foreignMarks + "]"
    )

    /** Rule 2: the look-alikes that have no other use in the app's code. */
    private val foreignMarkInCode = Regex("[\u0375\u2032\u02B9\u0374]")

    /** Rule 3: an octave mark, as a literal, being repeated — a renderer. */
    private val repeatedMark = Regex("[\"'][,\u0384" + foreignMarks + "][\"']\\s*\\.repeat\\(")

    private fun codeOffenders(pattern: Regex): List<String> =
        kotlinSources
            .filter { pattern.containsMatchIn(KotlinSource.withoutComments(it.readText())) }
            .map { it.name }
            .sorted()

    private fun stringOffenders(pattern: Regex): List<String> =
        stringFiles.flatMap { file ->
            file.readLines().filter { pattern.containsMatchIn(it) }.map { "${file.parentFile.name}: ${it.trim()}" }
        }

    @Test
    fun theScanSeesTheCodeAndTheStrings() {
        // Guards the slice: every assertion below would pass over empty lists.
        assertTrue("expected the app's Kotlin sources", kotlinSources.size > 50)
        assertTrue(kotlinSources.any { it.name == "Phthong.kt" })
        assertEquals("values and values-en", setOf("values", "values-en"), stringFiles.map { it.parentFile.name }.toSet())
        // And the canonical high mark is really there to be seen, in both languages.
        stringFiles.forEach { file ->
            assertTrue("${file.parentFile.name} spells Νη\u0384/Ni\u0384 with U+0384", file.readText().contains("\u0384</string>"))
        }
    }

    @Test
    fun noPhthongNameCarriesAForeignMark() {
        assertEquals("in code", emptyList<String>(), codeOffenders(nameThenForeignMark))
        assertEquals("in strings", emptyList<String>(), stringOffenders(nameThenForeignMark))
    }

    @Test
    fun noForeignMarkAppearsInCode() {
        assertEquals(emptyList<String>(), codeOffenders(foreignMarkInCode))
    }

    @Test
    fun onlyPhthongLabelRepeatsAnOctaveMark() {
        // Phthong.kt repeats its LOW_SUFFIX / HIGH_SUFFIX constants, not a literal, so nothing matches.
        assertEquals("a second renderer", emptyList<String>(), codeOffenders(repeatedMark))
    }

    /** The controls: the exact code and text H2 removed must be caught, and the corrected forms not. */
    @Test
    fun theChecksCatchWhatH2Removed() {
        val analysisBefore = """
            fun phthongLabel(degree: Int): String {
                val octave = Math.floorDiv(degree, 7)
                val name = TrainerPhthong.ascending[Math.floorMod(degree, 7)].displayName
                return when {
                    octave > 0 -> name + "΄".repeat(octave)
                    octave < 0 -> name + "͵".repeat(-octave)
                    else -> name
                }
            }
        """.trimIndent()
        assertTrue("rule 2 sees the analysis's \u0375", foreignMarkInCode.containsMatchIn(KotlinSource.withoutComments(analysisBefore)))
        assertTrue("rule 3 sees its renderer", repeatedMark.containsMatchIn(KotlinSource.withoutComments(analysisBefore)))

        val trainerBefore = """
            private fun phthongDisplay(note: TrainerNote): String {
                val suffix = when {
                    note.octaveShift > 0 -> "΄".repeat(note.octaveShift)
                    note.octaveShift < 0 -> ",".repeat(-note.octaveShift)
                    else -> ""
                }
                return note.phthong.displayName + suffix
            }
        """.trimIndent()
        assertTrue("rule 3 sees the Trainer's renderer", repeatedMark.containsMatchIn(KotlinSource.withoutComments(trainerBefore)))

        assertTrue("rule 1 sees the English prime", nameThenForeignMark.containsMatchIn("<string name=\"phthong_ni_high\">Ni\u2032</string>"))
        listOf("Νη\u0375", "Πα\u02B9", "Δι\u0374", "Κε\u00B4", "Ζω\u2019").forEach {
            assertTrue("rule 1 sees $it", nameThenForeignMark.containsMatchIn(it))
        }

        val after = "fun phthongLabel(degree: Int): String = PhthongSegmenter.phthongAt(degree).label\n" +
            "val label = note.pitch.label\n" +
            "private const val LOW_SUFFIX = \",\"\n" +
            "private const val HIGH_SUFFIX = \"\u0384\"\n" +
            "else -> HIGH_SUFFIX.repeat(octave)\n"
        listOf(nameThenForeignMark, foreignMarkInCode, repeatedMark).forEach {
            assertFalse("$it flagged the corrected code", it.containsMatchIn(KotlinSource.withoutComments(after)))
        }
        listOf("<string name=\"phthong_ni_high\">Ni\u0384</string>", "Νη\u0384", "Πα,", "Α\u2019 Ήχος", "don\u2019t").forEach {
            assertFalse("rule 1 flagged «$it»", nameThenForeignMark.containsMatchIn(it))
        }

        val history = "/** The analysis drew Νη\u0375 with `\"\u0375\".repeat(-octave)`. */\n// Ni\u2032 was English\nval x = 1\n"
        listOf(nameThenForeignMark, foreignMarkInCode, repeatedMark).forEach {
            assertFalse("$it flagged a comment", it.containsMatchIn(KotlinSource.withoutComments(history)))
        }
    }

    @Test
    fun theAnastasimatarionFolderNamesAreUntouched() {
        // Stored on users' devices as folder names: an octave-mark clean-up must never reach them.
        val expected = mapOf(
            "first" to "Ήχος Α\u0384",
            "second" to "Ήχος Β\u0384",
            "third" to "Ήχος Γ\u0384",
            "fourth" to "Ήχος Δ\u0384",
            "plagal_first" to "Ήχος πλ. Α\u0384",
            "plagal_second" to "Ήχος πλ. Β\u0384",
            "varys" to "Ήχος Βαρύς",
            "plagal_fourth" to "Ήχος πλ. Δ\u0384",
        )
        assertEquals(expected.keys, HymnFolders.modeKeys)
        expected.forEach { (modeKey, folder) -> assertEquals(modeKey, folder, HymnFolders.modeFolder(modeKey)) }
        expected.values.forEach { assertFalse("the scan would flag «$it»", nameThenForeignMark.containsMatchIn(it)) }
    }
}
