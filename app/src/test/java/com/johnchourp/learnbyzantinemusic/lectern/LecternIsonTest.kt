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
 * every shift the 8 Ήχοι slider reaches — also after the setting went through its stored form, which is
 * what a reopened PDF, or one imported on another phone, sounds. The απήχημα is held to the page's the
 * same way.
 */
class LecternIsonTest {

    @Test
    fun `the ison of a page's setting is the 8 Ήχοι page's pitch for every ήχος, φθόγγος and shift`() {
        var compared = 0
        EIGHT_MODES.forEach { row ->
            (BaseShift.MIN_MORIA..BaseShift.MAX_MORIA).forEach { shift ->
                // Exactly what the 8 Ήχοι page does: its ladder, its choices, its lookup.
                val pageLadder = ModeLadders.ladder(row.scale, shift)
                val pageChoices = IsonDrone.choices(row.mode, pageLadder)
                assertNotNull("${row.mode.key} shift=$shift", pageChoices)
                pageChoices!!.all.forEach { choice ->
                    val where = "${row.mode.key} shift=$shift ${choice.label}"
                    val onThePage = IsonDrone.step(pageLadder, choice)!!.frequencyHz
                    // Set on a page of a PDF the way the bar sets it, then stored and read back.
                    val set = PageAssignments.chooseIson(
                        PageAssignments.chooseShift(PageAssignments.chooseMode(emptyList(), 7, row.mode, 0), 7, shift),
                        7,
                        choice,
                    ).single()
                    val reread = (LecternPagesCodec.decode(LecternPagesCodec.encode(listOf(set))) as LecternPagesCodec.Decoded.Assignments)
                        .assignments.single()
                    assertEquals(where, onThePage, LecternIson.held(set.request)!!.frequencyHz, 0.0)
                    assertEquals("$where, reread", onThePage, LecternIson.held(reread.request)!!.frequencyHz, 0.0)
                    assertEquals(where, choice, LecternIson.held(reread.request)!!.phthong)
                    assertEquals(where, pageChoices, LecternIson.choices(reread.request))
                    compared++
                }
            }
        }
        // Guards the sweep: every ήχος × every shift × every φθόγγος, or it proves nothing.
        assertEquals(EIGHT_MODES.size * (BaseShift.MAX_MORIA - BaseShift.MIN_MORIA + 1) * PhthongName.entries.size, compared)
    }

    @Test
    fun `a page's απήχημα is the 8 Ήχοι page's, at every shift`() {
        var compared = 0
        EIGHT_MODES.forEach { row ->
            val greek = greekStrings.getValue(nameOf(row.apichimaSyllablesRes))
            val steps = ApichimaSequence.playable(greek, greek)
            assertTrue(row.mode.key, steps.isNotEmpty())
            (BaseShift.MIN_MORIA..BaseShift.MAX_MORIA).forEach { shift ->
                val onThePage = ApichimaSequence.frequencies(steps, ModeLadders.ladder(row.scale, shift))
                assertNotNull("${row.mode.key} shift=$shift", onThePage)
                val request = PageAssignment(0, row.mode, shift).request
                assertEquals("${row.mode.key} shift=$shift", onThePage, LecternIson.apichimaFrequencies(request, steps))
                compared++
            }
        }
        assertEquals(EIGHT_MODES.size * (BaseShift.MAX_MORIA - BaseShift.MIN_MORIA + 1), compared)
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
