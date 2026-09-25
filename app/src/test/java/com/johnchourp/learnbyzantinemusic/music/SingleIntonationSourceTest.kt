package com.johnchourp.learnbyzantinemusic.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the "one profile" rule of ClickUp `869f5x28t` (H1), the way [SingleTuningSourceTest] guards
 * the tuning: it is not enough that [IntonationProfile] exists — nothing *else* may declare the
 * tolerance or the silence gate, or judge a deviation with its own comparison. A second copy compiles
 * and keeps every behavioural test green; it is exactly how «Πού είμαι» came to say 3 and the
 * analysis 4 under the same name.
 *
 * Each rule is asserted as "matches **only** in `IntonationProfile.kt`", which makes the profile
 * itself the positive control: a pattern that stopped matching anything would fail, not pass.
 *
 * Only **code** is scanned. Comments and string literals are blanked first, because they mention
 * these names on purpose — the profile's own KDoc tells the history of the two constants — and a
 * detector reading raw text would flag the explanation of a bug as the bug.
 */
class SingleIntonationSourceTest {

    private val profileFile = "IntonationProfile.kt"

    private fun kotlinFiles(vararg roots: String): List<File> {
        val base = listOf(File("app/src"), File("src")).firstOrNull { File(it, "main/java").isDirectory }
            ?: error("Cannot locate the source roots from ${File("").absolutePath}")
        return roots.flatMap { root ->
            File(base, root).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        }
    }

    private val mainSources: List<File> by lazy { kotlinFiles("main/java") }
    private val allSources: List<File> by lazy { kotlinFiles("main/java", "test/java") }

    private fun filesWhoseCodeMatches(files: List<File>, pattern: Regex): List<String> =
        files.filter { pattern.containsMatchIn(codeOnly(it.readText())) }.map { it.name }.sorted()

    /** A declaration of the tolerance under its own name, whatever its value. */
    private val inTuneDeclaration = Regex("""\b(?:val|var)\s+IN_TUNE_MORIA\b""")

    /** A declaration of the silence gate under its own name, whatever its value. */
    private val silenceDeclaration = Regex("""\b(?:val|var)\s+SILENCE_RMS\b""")

    /**
     * A μόρια tolerance bound to a number: `DEFAULT_TOLERANCE_MORIA = 4.0`, a parameter default
     * `toleranceMoria: Double = 4.0`, a named argument `toleranceMoria = 4.0`. The name must say both
     * "tolerance"/"in tune" and "moria", so `DEFAULT_ONSET_TOLERANCE_MILLIS` (rhythm, in ms) and
     * `STEADY_SPREAD_MORIA` (calibration) are not tolerances of intonation and are left alone.
     */
    private val toleranceLiteral = Regex(
        """(?i)\b(?=\w*(?:tolerance|in_?tune))(?=\w*moria)\w+\s*(?::\s*[\w.]+\??\s*)?=\s*[-+]?\.?\d"""
    )

    /** An in-tune verdict computed in place, `abs(x.deviationMoria) <= …`, instead of asking the profile. */
    private val localVerdict = Regex("""\babs\(\s*[\w.]*deviationMoria\s*\)\s*(?:<=|>=|<|>)""")

    /** The silence gate's number spelled out, which a declaration check alone would not see. */
    private val silenceLiteral = Regex("""(?<![\w.])0\.012[fF]?(?![\w.])""")

    @Test
    fun theSourceRootsAreActuallyFound() {
        // Without this, every assertion below would pass vacuously over an empty list.
        assertTrue("expected to scan real sources", mainSources.size > 50)
        assertTrue("expected tests to be scanned too", allSources.size > mainSources.size + 50)
        assertTrue("the profile itself must be among them", mainSources.any { it.name == profileFile })
    }

    @Test
    fun onlyTheProfileDeclaresTheInTuneTolerance() {
        // Tests are scanned too: "no second IN_TUNE_MORIA anywhere in the repository".
        assertEquals(
            "IN_TUNE_MORIA must be declared once, in $profileFile",
            listOf(profileFile),
            filesWhoseCodeMatches(allSources, inTuneDeclaration),
        )
    }

    @Test
    fun onlyTheProfileDeclaresTheSilenceGate() {
        assertEquals(
            "SILENCE_RMS must be declared once, in $profileFile",
            listOf(profileFile),
            filesWhoseCodeMatches(allSources, silenceDeclaration),
        )
    }

    @Test
    fun onlyTheProfileSpellsTheSilenceLevel() {
        assertEquals(
            "0.012 is the silence gate; read IntonationProfile.SILENCE_RMS, or name what else it is",
            listOf(profileFile),
            filesWhoseCodeMatches(mainSources, silenceLiteral),
        )
    }

    @Test
    fun noOtherFileBindsAToleranceToANumber() {
        // Main sources only: tests pass explicit tolerances on purpose, to show what ±4 used to do.
        assertEquals(
            "an intonation tolerance may be a number only in $profileFile",
            listOf(profileFile),
            filesWhoseCodeMatches(mainSources, toleranceLiteral),
        )
    }

    @Test
    fun noOtherFileJudgesADeviationItself() {
        assertEquals(
            "call IntonationProfile.isInTune instead of comparing |deviation| in place",
            listOf(profileFile),
            filesWhoseCodeMatches(mainSources, localVerdict),
        )
    }

    /**
     * The controls. Each pattern must flag the shape it exists to reject — every one of these was in
     * the code before H1 — and must not flag the corrected form, a comment or a string.
     */
    @Test
    fun theChecksStillCatchWhatH1Removed() {
        val before = """
            const val IN_TUNE_MORIA = 4.0
            private const val SILENCE_RMS = 0.012
            const val SILENCE_RMS = 0.012f
            const val DEFAULT_TOLERANCE_MORIA = 4.0
            fun gate(m: PitchMatch?, toleranceMoria: Double = 4.0) = m
            val bar = if (abs(note.deviationMoria) <= IN_TUNE_MORIA) green else orange
        """.trimIndent()
        val code = codeOnly(before)
        assertTrue(inTuneDeclaration.containsMatchIn(code))
        assertEquals(2, silenceDeclaration.findAll(code).count())
        assertEquals(2, silenceLiteral.findAll(code).count())
        // IN_TUNE_MORIA = 4.0, DEFAULT_TOLERANCE_MORIA = 4.0 and the parameter default.
        assertEquals(3, toleranceLiteral.findAll(code).count())
        assertTrue(localVerdict.containsMatchIn(code))

        val after = """
            fun gate(m: PitchMatch?, toleranceMoria: Double = IntonationProfile.IN_TUNE_MORIA) = m
            val bar = if (note.isInTune) green else orange
            const val DEFAULT_ONSET_TOLERANCE_MILLIS = 250L
            private const val STEADY_SPREAD_MORIA = 3.0
            val nearest = abs(deviation) < abs(bestDeviation)
            val louder = 0.0125
        """.trimIndent()
        listOf(inTuneDeclaration, silenceDeclaration, silenceLiteral, toleranceLiteral, localVerdict).forEach {
            assertFalse("$it flagged correct code", it.containsMatchIn(codeOnly(after)))
        }
    }

    @Test
    fun onlyCodeIsScannedNotCommentsOrStrings() {
        val prose = "/** There used to be two `const val IN_TUNE_MORIA = 4.0`. */\n" +
            "// private const val SILENCE_RMS = 0.012\n" +
            "/* outer /* nested: toleranceMoria = 4.0 */ still a comment: 0.012 */\n" +
            "val log = \"abs(x.deviationMoria) <= 3.0\"\n" +
            "val raw = \"\"\"const val IN_TUNE_MORIA = 4.0\"\"\"\n" +
            "val quote = '\"'\n" +
            "const val SILENCE_RMS = 0.012\n"
        val code = codeOnly(prose)
        assertEquals("line structure is kept", prose.count { it == '\n' }, code.count { it == '\n' })
        assertFalse(inTuneDeclaration.containsMatchIn(code))
        assertFalse(toleranceLiteral.containsMatchIn(code))
        assertFalse(localVerdict.containsMatchIn(code))
        // The char literal '"' must not open a string that swallows the real declaration after it.
        assertEquals(1, silenceDeclaration.findAll(code).count())
        assertEquals(1, silenceLiteral.findAll(code).count())
    }

    /**
     * [text] with every comment (line, block, KDoc — Kotlin block comments nest) and the contents of
     * every string and char literal replaced by spaces. Newlines are kept, so nothing moves lines.
     */
    private fun codeOnly(text: String): String {
        val out = StringBuilder(text.length)
        fun blank(from: Int, to: Int) {
            for (k in from until to) out.append(if (text[k] == '\n') '\n' else ' ')
        }
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("//", i) -> {
                    val end = text.indexOf('\n', i).let { if (it < 0) text.length else it }
                    blank(i, end)
                    i = end
                }
                text.startsWith("/*", i) -> {
                    var depth = 0
                    var j = i
                    while (j < text.length) {
                        if (text.startsWith("/*", j)) {
                            depth++
                            j += 2
                        } else if (text.startsWith("*/", j)) {
                            depth--
                            j += 2
                            if (depth == 0) break
                        } else {
                            j++
                        }
                    }
                    val end = minOf(j, text.length)
                    blank(i, end)
                    i = end
                }
                text.startsWith("\"\"\"", i) -> {
                    val close = text.indexOf("\"\"\"", i + 3)
                    val end = if (close < 0) text.length else close + 3
                    out.append("\"\"\"")
                    blank(i + 3, maxOf(i + 3, end - 3))
                    if (close >= 0) out.append("\"\"\"")
                    i = end
                }
                text[i] == '"' || text[i] == '\'' -> {
                    val quote = text[i]
                    var j = i + 1
                    while (j < text.length && text[j] != quote && text[j] != '\n') {
                        j += if (text[j] == '\\') 2 else 1
                    }
                    val stop = minOf(j, text.length)
                    val closed = stop < text.length && text[stop] == quote
                    out.append(quote)
                    blank(i + 1, stop)
                    if (closed) out.append(quote)
                    // An unclosed literal ends at the newline, which stays in the output.
                    i = if (closed) stop + 1 else stop
                }
                text[i] == '`' -> {
                    // A backticked name is code, but its text is not: `it's` must not open a char literal.
                    val close = text.indexOf('`', i + 1).takeIf { it >= 0 && '\n' !in text.substring(i, it) }
                    val end = if (close == null) i + 1 else close + 1
                    out.append('`')
                    blank(i + 1, maxOf(i + 1, end - 1))
                    if (close != null) out.append('`')
                    i = end
                }
                else -> {
                    out.append(text[i])
                    i++
                }
            }
        }
        return out.toString()
    }
}
