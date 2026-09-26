package com.johnchourp.learnbyzantinemusic.lectern

import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ApichimaSequence
import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.modes.ModeLadders
import com.johnchourp.learnbyzantinemusic.modes.ui.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The lectern's ison bar sounds the 8 Ήχοι page's pitches, exactly (ClickUp `869f5x2e7`).
 *
 * The bar resolves a page's setting through [LecternIson]; the 8 Ήχοι page resolves its ison inside a
 * composable, on its own ladder. Two paths to one pitch is how a drone ends up a comma off the scale it
 * accompanies, so they are compared bit for bit for every ήχος, every φθόγγος «Ίσον σε…» offers and
 * every shift the 8 Ήχοι slider reaches — with the voice's global shift of «Βρες τη φωνή σου» (J4) added
 * the way the 8 Ήχοι page adds it, and also after the setting went through its stored form, which is
 * what a reopened PDF, or one imported on another phone, sounds. The απήχημα is held to the page's the
 * same way.
 */
class LecternIsonTest {

    /** Global shifts the voice test can leave: none, a small one, and ones that push past the range. */
    private val globals = listOf(0, 7, -20, BaseShift.MIN_MORIA, BaseShift.MAX_MORIA)

    @Test
    fun `the ison of a page's setting is the 8 Ήχοι page's pitch for every ήχος, φθόγγος and shift`() {
        var compared = 0
        EIGHT_MODES.forEach { row ->
            (BaseShift.MIN_MORIA..BaseShift.MAX_MORIA).forEach { shift ->
                globals.forEach { global ->
                    // Exactly what the 8 Ήχοι page does: the mode's own shift plus the voice's global
                    // one (BaseShift.combined), its ladder, its choices, its lookup.
                    val pageLadder = ModeLadders.ladder(row.scale, BaseShift.combined(shift, global))
                    val pageChoices = IsonDrone.choices(row.mode, pageLadder)
                    assertNotNull("${row.mode.key} shift=$shift global=$global", pageChoices)
                    pageChoices!!.all.forEach { choice ->
                        val where = "${row.mode.key} shift=$shift global=$global ${choice.label}"
                        val onThePage = IsonDrone.step(pageLadder, choice)!!.frequencyHz
                        // Set on a page of a PDF the way the bar sets it, then stored and read back.
                        val set = PageAssignments.chooseIson(
                            PageAssignments.chooseShift(PageAssignments.chooseMode(emptyList(), 7, row.mode, 0), 7, shift),
                            7,
                            choice,
                        ).single()
                        val reread = (LecternPagesCodec.decode(LecternPagesCodec.encode(listOf(set))) as LecternPagesCodec.Decoded.Assignments)
                            .assignments.single()
                        assertEquals(where, onThePage, LecternIson.held(set.request(global))!!.frequencyHz, 0.0)
                        assertEquals("$where, reread", onThePage, LecternIson.held(reread.request(global))!!.frequencyHz, 0.0)
                        assertEquals(where, choice, LecternIson.held(reread.request(global))!!.phthong)
                        assertEquals(where, pageChoices, LecternIson.choices(reread.request(global)))
                        compared++
                    }
                }
            }
        }
        // Guards the sweep: every ήχος × every shift × every global × every φθόγγος, or it proves nothing.
        assertEquals(
            EIGHT_MODES.size * (BaseShift.MAX_MORIA - BaseShift.MIN_MORIA + 1) * globals.size * PhthongName.entries.size,
            compared,
        )
    }

    @Test
    fun `the voice's global shift is added where the ison is built, and never stored with the page`() {
        val own = PageAssignment(3, row0Mode, 30)
        assertEquals(BaseShift.MAX_MORIA, own.request(20).baseShiftMoria)
        assertEquals(10, own.request(-20).baseShiftMoria)
        assertEquals("a global 0 is the page's own shift, exactly as before J4", 30, own.request(0).baseShiftMoria)
        // What is stored is the page's own shift alone, whatever the voice.
        val stored = LecternPagesCodec.encode(listOf(own))
        assertTrue(stored, stored.contains("\"shiftMoria\":30"))
    }

    private val row0Mode = EIGHT_MODES.first().mode

    @Test
    fun `a page's απήχημα is the 8 Ήχοι page's, at every shift`() {
        var compared = 0
        EIGHT_MODES.forEach { row ->
            val greek = greekStrings.getValue(nameOf(row.apichimaSyllablesRes))
            val steps = ApichimaSequence.playable(greek, greek)
            assertTrue(row.mode.key, steps.isNotEmpty())
            (BaseShift.MIN_MORIA..BaseShift.MAX_MORIA).forEach { shift ->
                globals.forEach { global ->
                    val onThePage = ApichimaSequence.frequencies(steps, ModeLadders.ladder(row.scale, BaseShift.combined(shift, global)))
                    assertNotNull("${row.mode.key} shift=$shift global=$global", onThePage)
                    val request = PageAssignment(0, row.mode, shift).request(global)
                    assertEquals("${row.mode.key} shift=$shift global=$global", onThePage, LecternIson.apichimaFrequencies(request, steps))
                    compared++
                }
            }
        }
        assertEquals(EIGHT_MODES.size * (BaseShift.MAX_MORIA - BaseShift.MIN_MORIA + 1) * globals.size, compared)
    }

    // ---- the Greek strings, as the 8 Ήχοι page reads them ------------------------------------------

    private val resourceNames: Map<Int, String> by lazy {
        R.string::class.java.fields.associate { it.getInt(null) to it.name }
    }

    private fun nameOf(id: Int): String = resourceNames[id] ?: error("no string resource has id $id")

    private val greekStrings: Map<String, String> by lazy {
        val res = listOf(File("app/src/main/res"), File("src/main/res")).firstOrNull { it.isDirectory }
            ?: error("Cannot locate the resources from ${File("").absolutePath}")
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(res, "values/strings.xml"))
            .getElementsByTagName("string")
        (0 until nodes.length).map { nodes.item(it) as Element }.associate { it.getAttribute("name") to it.textContent }
    }
}
