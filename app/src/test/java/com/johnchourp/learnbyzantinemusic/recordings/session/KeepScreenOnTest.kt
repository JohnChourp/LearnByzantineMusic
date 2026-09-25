package com.johnchourp.learnbyzantinemusic.recordings.session

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import com.johnchourp.learnbyzantinemusic.recordings.RecordingStateUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The quick half of ClickUp `869f5x273`: while a recording runs or is paused, the «Ηχογραφήσεις»
 * screen keeps the display on — with it off, the app drops to the background, where Android 9+
 * hands it silence instead of the microphone.
 *
 * The rule is read from the session's state, not from the screen, so a re-created screen keeps it.
 */
class KeepScreenOnTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun theScreenStaysOnWhileRecordingOrPausedAndOnlyThen() {
        assertEquals(
            mapOf(
                RecordingStateUi.IDLE to false,
                RecordingStateUi.RECORDING to true,
                RecordingStateUi.PAUSED to true,
                RecordingStateUi.SAVING to false,
                RecordingStateUi.ERROR to false,
            ),
            RecordingStateUi.entries.associateWith { RecordingSessionState(phase = it).keepsScreenOn },
        )
    }

    @Test
    fun itFollowsARealRecordingFromStartToSaved() {
        val rig = SessionRig(tmp.root)
        try {
            assertFalse(rig.session.state.value.keepsScreenOn)
            rig.session.start(RecordingTarget())
            assertTrue(rig.session.state.value.keepsScreenOn)
            rig.session.pause()
            assertTrue("paused, the recording is still open", rig.session.state.value.keepsScreenOn)
            rig.session.resume()
            rig.session.stop()
            SessionTestKit.awaitPhase(rig.session, RecordingStateUi.IDLE)
            assertFalse(rig.session.state.value.keepsScreenOn)
        } finally {
            rig.close()
        }
    }

    @Test
    fun theScreenAppliesTheRuleWhereItRendersTheSession() {
        val code = KotlinSource.withoutComments(
            File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/recordings/RecordingsActivity.kt").readText()
        )
        val start = code.indexOf("private fun renderSession(")
        assertTrue("renderSession not found — this check reads nothing", start >= 0)
        val body = code.substring(start).let { rest ->
            val next = Regex("""\n {4}(private |override )*fun """).find(rest, startIndex = 1)
            if (next != null) rest.substring(0, next.range.first) else rest
        }
        assertTrue("the rule comes from the session's state", body.contains("state.keepsScreenOn"))
        assertTrue(body.contains("addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)"))
        assertTrue("and is lifted again once the recording is over", body.contains("clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)"))
    }
}
