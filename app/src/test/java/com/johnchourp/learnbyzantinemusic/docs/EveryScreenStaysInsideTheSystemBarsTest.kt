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
        val edge = onCreate.indexOf("enableEdgeToEdge()")
        val superCall = onCreate.indexOf("super.onCreate(")
        assertTrue("enableEdgeToEdge() must be called in onCreate", edge >= 0)
        assertTrue("enableEdgeToEdge() must come before super.onCreate", edge < superCall)
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
    }

    @Test
    fun bothThemesResizeForTheKeyboard() {
        for (dir in listOf("values", "values-night")) {
            val theme = File(appDir, "src/main/res/$dir/themes.xml").readText()
            assertTrue("$dir/themes.xml must set adjustResize", Regex("""android:windowSoftInputMode">\s*adjustResize\s*<""").containsMatchIn(theme))
        }
    }

    @Test
    fun targetSdkIs36() {
        val gradle = File(appDir, "build.gradle.kts").readText()
        assertEquals("36", Regex("""targetSdk\s*=\s*(\d+)""").find(gradle)?.groupValues?.get(1))
    }
}
