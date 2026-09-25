package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.R
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Test-only: what the app *shows* for a length of time, read back as exact [Beats] (ClickUp
 * `869f5x29r`, H5).
 *
 * The pages print beat values as text — «½», «⅓», «1 χρόνο», «2» — in every language they ship.
 * The tests that hold those pages to the time rules read that text rather than the resource names,
 * so a label is judged by what the reader sees, in each language: a `time_1_by_4` whose text were
 * «½» would be caught too. The sign-name tests (H4) read [languages] and [nameOf] the same way.
 */
internal object ShownBeats {

    private val res: File by lazy {
        listOf(File("app/src/main/res"), File("src/main/res")).firstOrNull { it.isDirectory }
            ?: error("Cannot locate the resources from ${File("").absolutePath}")
    }

    /** Every `values*` folder that ships a strings.xml, by folder name: string name → text. */
    val languages: Map<String, Map<String, String>> by lazy {
        res.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values") }
            .sortedBy { it.name }
            .mapNotNull { folder ->
                val xml = File(folder, "strings.xml").takeIf { it.isFile } ?: return@mapNotNull null
                folder.name to stringsOf(xml)
            }
            .toMap()
    }

    private val resourceNames: Map<Int, String> by lazy {
        R.string::class.java.fields.associate { it.getInt(null) to it.name }
    }

    fun nameOf(id: Int): String = resourceNames[id] ?: error("no string resource has id $id")

    /** The text string [id] shows in [folder]: its own, or the default folder's, as Android resolves it. */
    fun text(folder: String, id: Int): String {
        val name = nameOf(id)
        return languages.getValue(folder)[name]
            ?: languages.getValue("values")[name]
            ?: error("no values folder ships $name")
    }

    private val fractions: Map<Char, Beats> = mapOf(
        '½' to Beats.of(1, 2),
        '⅓' to Beats.of(1, 3),
        '⅔' to Beats.of(2, 3),
        '¼' to Beats.of(1, 4),
        '¾' to Beats.of(3, 4),
    )

    /** Every vulgar-fraction character the pages print. */
    val fractionGlyphs: Set<Char> get() = fractions.keys

    private val length = Regex("""^\s*(\d+)?\s*([½⅓⅔¼¾])?""")

    /**
     * The length a label shows: a whole number, a vulgar fraction, or both — «2», «½», «1 χρόνο»,
     * «1½». Null when the text starts with neither.
     */
    fun parse(label: String): Beats? {
        val match = length.find(label) ?: return null
        val whole = match.groupValues[1].takeIf { it.isNotEmpty() }?.toInt()
        val fraction = match.groupValues[2].takeIf { it.isNotEmpty() }?.let { fractions.getValue(it[0]) }
        if (whole == null && fraction == null) return null
        return Beats.whole(whole ?: 0) + (fraction ?: Beats.ZERO)
    }

    /** The vulgar fraction the pages print for [beats], a fraction of one χρόνος. */
    fun glyphOf(beats: Beats): Char =
        fractions.entries.firstOrNull { it.value == beats }?.key ?: error("the pages print no glyph for $beats")

    private fun stringsOf(file: File): Map<String, String> {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            .getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .associate { it.getAttribute("name") to it.textContent }
    }
}
