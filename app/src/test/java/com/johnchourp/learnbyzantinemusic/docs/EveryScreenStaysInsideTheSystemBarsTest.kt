package com.johnchourp.learnbyzantinemusic.docs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * targetSdk 36 makes every screen edge-to-edge, with no opt-out (ClickUp `869fhmxpt`). No screen
 * handles insets itself, so [com.johnchourp.learnbyzantinemusic.BaseActivity] does it for all of
 * them; this pins the pieces that keep the layout the app had at targetSdk 34. Source checks, like
 * `EveryScreenAppliesTheThemeTest`, because the rule is about how a screen is constructed — the
 * picture itself needs a device.
 */
class EveryScreenStaysInsideTheSystemBarsTest {
    private val base: String by lazy {
        KotlinSource.withoutComments(File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/BaseActivity.kt").readText())
    }

    private val appDir: File get() = KotlinSource.srcDir.absoluteFile.parentFile

    @Test
    fun edgeToEdgeIsSetUpBeforeTheWindowIsCreated() {
        val onCreate = Regex("""override fun onCreate\(savedInstanceState: Bundle\?\)\s*\{(.*?)\n    }""", RegexOption.DOT_MATCHES_ALL)
            .find(base)?.groupValues?.get(1) ?: error("BaseActivity.onCreate not found")
        val edge = onCreate.indexOf("enableEdgeToEdgeFor(")
        val superCall = onCreate.indexOf("super.onCreate(")
        assertTrue("edge-to-edge must be enabled in onCreate", edge >= 0)
        assertTrue("edge-to-edge must be enabled before super.onCreate", edge < superCall)
        assertTrue("the bars must be painted once the content exists", onCreate.indexOf("paintSystemBars(") > superCall)
        assertTrue(
            "the content view must get the insets",
            Regex("""applySystemBarInsets\(\s*findViewById\(\s*android\.R\.id\.content\s*\)\s*\)""").containsMatchIn(onCreate),
        )
    }

    @Test
    fun theContentKeepsOutOfBarsCutoutAndKeyboard() {
        for (type in listOf("systemBars()", "displayCutout()", "ime()")) {
            assertTrue("insets must include WindowInsetsCompat.Type.$type", base.contains("WindowInsetsCompat.Type.$type"))
        }
        assertTrue("the padding must be applied to the content view", Regex("""view\.setPadding\(\s*bars\.left,\s*bars\.top,\s*bars\.right,\s*bars\.bottom\s*\)""").containsMatchIn(base))
        assertTrue("the insets must be consumed, so no child pads twice", base.contains("WindowInsetsCompat.CONSUMED"))
        assertTrue(
            "the strip under the transparent bars must be the palette's page colour, not the platform window background",
            Regex("""setBackgroundColor\(\s*palette\.pageBg\.toArgb\(\)\s*\)""").containsMatchIn(base),
        )
        assertTrue(
            "the bar style must come from the palette (dark/light with the page colour), not SystemBarStyle.auto's scrim",
            base.contains("SystemBarStyle.dark(page)") && base.contains("SystemBarStyle.light(page,") && !base.contains("SystemBarStyle.auto"),
        )
    }

    @Test
    fun bothThemesResizeForTheKeyboard() {
        for (dir in listOf("values", "values-night")) {
            val theme = File(appDir, "src/main/res/$dir/themes.xml").readText()
            assertTrue("$dir/themes.xml must set adjustResize", Regex("""android:windowSoftInputMode">\s*adjustResize\s*<""").containsMatchIn(theme))
        }
    }

    @Test
    fun theLecternReaderRepaintsTheBarsForItsNightTint() {
        val reader = KotlinSource.withoutComments(
            File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/lectern/LecternReaderActivity.kt").readText(),
        )
        assertTrue(
            "the reader must paint the bars from the same palette its page uses",
            Regex("""LaunchedEffect\(\s*pagePalette\s*\)\s*\{\s*paintSystemBars\(\s*pagePalette\s*\)""").containsMatchIn(reader) &&
                reader.contains("readerPalette(currentPalette(), state.night)"),
        )
    }

    @Test
    fun tabletsKeepThePortraitLockUntilTheyAreDesignedFor() {
        val manifest = File(appDir, "src/main/AndroidManifest.xml").readText()
        assertTrue(
            "targetSdk 36 opt-out from forced large-screen resizability (remove at targetSdk 37, ClickUp 869fhz20m)",
            Regex("""android:name="android\.window\.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY"\s+android:value="true"""").containsMatchIn(manifest),
        )
    }

    @Test
    fun targetSdkIs36() {
        val gradle = File(appDir, "build.gradle.kts").readText()
        assertEquals("36", Regex("""targetSdk\s*=\s*(\d+)""").find(gradle)?.groupValues?.get(1))
    }
}
