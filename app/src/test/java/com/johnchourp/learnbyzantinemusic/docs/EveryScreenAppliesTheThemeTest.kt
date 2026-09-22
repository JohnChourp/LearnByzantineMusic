package com.johnchourp.learnbyzantinemusic.docs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every screen must render in the palette the user chose (ClickUp `869f5cnyq`).
 *
 * `LbmTheme` takes a palette and **defaults to the light one**, so `LbmTheme { … }` compiles, looks
 * right on the author's device and silently ignores dark mode. Dark mode arrived in PR #110 while
 * the Anastasimatarion (#96) and the recording analysis (#97) were already written and waiting;
 * both were merged on 2026-09-22 without being migrated, and all three of their screens rendered
 * light on a device set to dark.
 *
 * Nothing caught it: no unit test applies a theme, and a screenshot of a light screen looks like a
 * light screen. It was found by looking at the device. This test is what makes that unnecessary
 * next time — it is a source check, which is exactly right for a rule about how a screen is
 * *constructed*.
 */
class EveryScreenAppliesTheThemeTest {

    private val activities: List<File> by lazy {
        val root = listOf(File("app/src/main/java"), File("src/main/java")).firstOrNull { it.isDirectory }
            ?: error("Cannot locate the main source root from ${File("").absolutePath}")
        root.walkTopDown()
            .filter { it.isFile && it.name.endsWith("Activity.kt") && "LbmTheme" in it.readText() }
            .toList()
    }

    @Test
    fun theThemedScreensAreActuallyFound() {
        // Guards the slice: an empty list would make the assertion below trivially true.
        assertTrue("expected the app's themed screens, found ${activities.size}", activities.size >= 10)
    }

    @Test
    fun noScreenFallsBackToTheDefaultPalette() {
        val offenders = activities
            .filter { file ->
                val text = file.readText()
                // A screen may set up the theme more than once; every occurrence must pass a palette.
                val bare = Regex("""LbmTheme\s*\{""").findAll(text).count()
                bare > 0
            }
            .map { it.name }
            .sorted()
        assertEquals(
            "these call LbmTheme { } and so always render light, whatever the user chose",
            emptyList<String>(),
            offenders,
        )
    }

    @Test
    fun theCheckStillCatchesABareCall() {
        // Negative control: the regex must match the shape it is meant to reject, and must not
        // match the correct one — otherwise the assertion above passes by never matching anything.
        val bare = Regex("""LbmTheme\s*\{""")
        assertTrue(bare.containsMatchIn("setContent { LbmTheme { Screen() } }"))
        assertTrue(!bare.containsMatchIn("setContent { LbmTheme(palette = currentPalette()) { Screen() } }"))
    }
}
