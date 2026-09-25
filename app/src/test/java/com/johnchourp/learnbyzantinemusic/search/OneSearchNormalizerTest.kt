package com.johnchourp.learnbyzantinemusic.search

import com.johnchourp.learnbyzantinemusic.modes.TheorySearch
import com.johnchourp.learnbyzantinemusic.notes.NoteEntity
import com.johnchourp.learnbyzantinemusic.notes.NotesSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * One normaliser for the theory search and the notes search, not two. A second one compiles, keeps
 * every other test green, and lets a word be findable in one search and not in the other — the notes
 * search used to be exactly that, lowercasing the query only.
 *
 * Two halves:
 * - **behaviour:** theory and notes turn the same text into the same searchable form, and find the
 *   same things, over the cases each search exists for;
 * - **source:** no other main source file decomposes text or strips combining marks itself. Comments
 *   and string contents are removed before scanning, so prose that names them is not a finding.
 */
class OneSearchNormalizerTest {

    private val samples = listOf(
        "Κύριε ἐκέκραξα",
        "ΚΥΡΙΕ",
        "ΤΡΟΧΟΣ",
        "Έλξη",
        "πεταστή",
        "The Octave (Diapason)",
        "50% _x_ \\",
        "Απήχημα",
        "  με κενά  "
    )

    private fun noteOf(text: String) = NoteEntity("n", text, text, createdAtEpochMs = 1L, updatedAtEpochMs = 1L)

    @Test
    fun theoryAndNotesProduceTheSameSearchableText() {
        samples.forEach { text ->
            val expected = SearchNormalizer.normalize(text)
            assertEquals("theory, «$text»", expected, TheorySearch.normalize(text))
            val indexed = NotesSearch.index(listOf(noteOf(text))).single()
            assertEquals("notes title, «$text»", expected, indexed.title)
            assertEquals("notes body, «$text»", expected, indexed.body)
        }
    }

    @Test
    fun theoryAndNotesFindTheSameThings() {
        val queries = listOf("κυριε", "ΚΎΡΙΕ", "τροχος", "ελξη", "πεταστη", "octave", "50%", "_", "απηχημα", "κλάσμα", " ")
        samples.forEach { text ->
            queries.forEach { query ->
                assertEquals(
                    "«$query» in «$text»",
                    TheorySearch.matches(text, query),
                    NotesSearch.filter(NotesSearch.index(listOf(noteOf(text))), query).isNotEmpty()
                )
            }
        }
    }

    // ---- source ---------------------------------------------------------------------------------

    private val normalizerFile = "SearchNormalizer.kt"

    private val mainSources: List<File> by lazy {
        val roots = listOf(File("app/src/main/java"), File("src/main/java"))
        val root = roots.firstOrNull { it.isDirectory }
            ?: error("Cannot locate the main source root from ${File("").absolutePath}")
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    @Test
    fun theSourceRootIsActuallyFound() {
        // Without this, the scan below would pass vacuously over an empty list.
        assertTrue("expected to scan real sources", mainSources.size > 50)
        assertTrue("the normaliser itself must be scanned", mainSources.any { it.name == normalizerFile })
    }

    @Test
    fun onlySearchNormalizerDecomposesTextOrStripsCombiningMarks() {
        // Decomposing (Normalizer.normalize) or stripping marks (\p{Mn}, \p{M}) is what a second
        // normaliser would have to do. The combining-mark pattern lives in a string, so the scan keeps
        // string contents for that one check.
        val decomposes = Regex("""\bNormalizer\s*\.\s*normalize\s*\(""")
        val stripsMarks = Regex("""p\{Mn?}""")
        val found = mainSources.filter { file ->
            val code = KotlinSource.withoutComments(file.readText())
            decomposes.containsMatchIn(KotlinSource.withoutStrings(code)) || stripsMarks.containsMatchIn(code)
        }
        assertEquals(
            "text is made searchable in $normalizerFile only",
            listOf(normalizerFile),
            found.map { it.name }.sorted()
        )
    }

    @Test
    fun theCommentStripperKeepsCodeAndDropsProse() {
        // The scan is only as good as this: it must see code, and must not see comments.
        val source = """
            // Normalizer.normalize(x) in a line comment
            /* Normalizer.normalize(x) in a block /* nested */ comment */
            val url = "https://example.org/*not a comment*/"
            val kept = Normalizer.normalize(value, form)
        """.trimIndent()
        val code = KotlinSource.withoutComments(source)
        assertEquals(1, Regex("""Normalizer\.normalize\(""").findAll(code).count())
        assertTrue("a string that looks like a comment is code", code.contains("/*not a comment*/"))
    }
}

/** Just enough of Kotlin's lexical rules to tell code from comments and string contents. */
private object KotlinSource {

    /** [source] with every comment replaced by a space; block comments nest, as they do in Kotlin. */
    fun withoutComments(source: String): String = scan(source, keepStrings = true)

    /** [source] (already without comments) with the contents of every string literal removed. */
    fun withoutStrings(source: String): String = scan(source, keepStrings = false)

    private fun scan(source: String, keepStrings: Boolean): String {
        val out = StringBuilder(source.length)
        var i = 0
        fun startsAt(token: String) = source.startsWith(token, i)
        while (i < source.length) {
            when {
                startsAt("//") -> {
                    while (i < source.length && source[i] != '\n') i++
                    out.append(' ')
                }
                startsAt("/*") -> {
                    var depth = 0
                    while (i < source.length) {
                        if (startsAt("/*")) { depth++; i += 2; continue }
                        if (startsAt("*/")) { depth--; i += 2; if (depth == 0) break; continue }
                        i++
                    }
                    out.append(' ')
                }
                startsAt("\"\"\"") -> {
                    val end = source.indexOf("\"\"\"", i + 3).let { if (it < 0) source.length else it + 3 }
                    out.append(if (keepStrings) source.substring(i, end) else "\"\"")
                    i = end
                }
                source[i] == '"' || source[i] == '\'' -> {
                    val quote = source[i]
                    var j = i + 1
                    while (j < source.length && source[j] != quote && source[j] != '\n') {
                        j += if (source[j] == '\\') 2 else 1
                    }
                    val end = minOf(j + 1, source.length)
                    out.append(if (keepStrings) source.substring(i, end) else "$quote$quote")
                    i = end
                }
                else -> {
                    out.append(source[i])
                    i++
                }
            }
        }
        return out.toString()
    }
}
