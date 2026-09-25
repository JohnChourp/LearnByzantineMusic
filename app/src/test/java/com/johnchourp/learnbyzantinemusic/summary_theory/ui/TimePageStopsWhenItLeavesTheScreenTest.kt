package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Nothing sounds once the «Χαρακτήρες Χρόνου» page leaves the screen, and one example plays at a time
 * (ClickUp `869f5x25n`, F4). The metronome of the «Δίσημος» page kept ticking in the background until
 * J3 stopped it on `ON_STOP`; the time examples do the same from the start.
 *
 * The stop itself is the player's and needs Android; this checks the lines no JVM test can run — the
 * page stops its player on `ON_STOP`, releases it when it leaves, and the player stops the example
 * before it starts another. Comments are stripped with [KotlinSource] first.
 */
class TimePageStopsWhenItLeavesTheScreenTest {

    private fun code(path: String): String =
        KotlinSource.withoutComments(File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/$path").readText())

    private val screen by lazy { code("summary_theory/ui/TimeScreen.kt") }
    private val player by lazy { code("summary_theory/ui/TimeExamplePlayer.kt") }

    /** The block after [opening] up to its matching brace; null when [opening] is not there. */
    private fun block(code: String, opening: String): String? {
        val start = code.indexOf(opening)
        if (start < 0) return null
        val brace = code.indexOf('{', start + opening.length - 1)
        var depth = 0
        for (i in brace until code.length) {
            when (code[i]) {
                '{' -> depth++
                '}' -> if (--depth == 0) return code.substring(brace + 1, i)
            }
        }
        return null
    }

    private val onStopEffect = "LifecycleEventEffect(Lifecycle.Event.ON_STOP) {"

    @Test
    fun thePageStopsItsExampleOnStop() {
        val body = block(screen, onStopEffect) ?: error("TimeScreen has no ON_STOP effect")
        assertTrue("on ON_STOP it must stop what plays: «$body»", body.contains(".stop()"))
    }

    @Test
    fun thePageReleasesThePlayerWhenItLeaves() {
        val body = block(screen, "onDispose {") ?: error("TimeScreen releases nothing on dispose")
        assertTrue("«$body»", body.contains(".release()"))
    }

    @Test
    fun anExampleStopsTheOneBeforeIt() {
        val play = block(player, "fun play(") ?: error("TimeExamplePlayer has no play")
        assertTrue("play must begin by stopping: «${play.take(80)}»", play.trimStart().startsWith("stop()"))
        // One player for the whole page, so «stop the one before» covers every example on it.
        assertEquals(1, Regex("""TimeExamplePlayer\(""").findAll(screen).count())
    }

    @Test
    fun theChecksNoticeAPageWithoutThem() {
        // Negative controls: an effect that does something else, a page with no effect at all, and a
        // play that starts without stopping.
        val other = "LifecycleEventEffect(Lifecycle.Event.ON_STOP) { counter++ }\n"
        assertFalse(block(other, onStopEffect)!!.contains(".stop()"))
        assertEquals(null, block("LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { listening.stop() }", onStopEffect))
        val commented = KotlinSource.withoutComments("// LifecycleEventEffect(Lifecycle.Event.ON_STOP) { listening.stop() }\n")
        assertEquals("a comment is not the effect", null, block(commented, onStopEffect))
        val lazyPlay = "fun play(cues: List<Cue>, listener: Listener) {\n    val token = session\n    stop()\n}"
        assertFalse(block(lazyPlay, "fun play(")!!.trimStart().startsWith("stop()"))
    }
}
