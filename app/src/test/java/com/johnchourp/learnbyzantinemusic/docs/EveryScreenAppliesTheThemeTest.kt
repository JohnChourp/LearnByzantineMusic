package com.johnchourp.learnbyzantinemusic.docs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every screen must render in the palette the user chose (ClickUp `869f5cnyq`, widened by
 * `869f5x286`).
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
 *
 * ## Where the list of screens comes from (`869f5x286`)
 *
 * The first version found screens by file name — `*Activity.kt` — and then skipped every file that
 * never mentions `LbmTheme`. Both filters hid what this test exists to catch:
 * - eight screens are declared under other names (Ascents, Descents, Quality, Testimonies, Time,
 *   ClimbingCompositions, Duotrioquatro, PhthongsNames), so they were never looked at;
 * - a screen that forgot the theme *entirely* was skipped precisely because it forgot it.
 *
 * So the screens now come from the manifest — the list Android itself launches from — and a screen
 * without the theme fails instead of dropping out of the check. The two screens still built from
 * XML layouts are an explicit allow-list with the reason, and the allow-list is checked too, so a
 * screen that moves to Compose cannot keep hiding behind it. Comments are removed before anything
 * is matched ([KotlinSource.withoutComments]): a comment that *mentions* the right call is not the
 * right call.
 */
class EveryScreenAppliesTheThemeTest {

    /** A screen declared in a manifest, and the source file that defines its class. */
    private data class Screen(val className: String, val file: File?) {
        val simpleName: String get() = className.substringAfterLast('.')
    }

    /**
     * Declared screens that are Views, not Compose, and so cannot call `LbmTheme`. Their dark
     * colours are `values-night` resources — BaseActivity forces the night flag from the user's
     * choice, and ThemeXmlParityTest keeps those resources equal to the palette.
     */
    private val viewBasedScreens = mapOf(
        "ModeTheoryActivity" to "XML layout (layout_mode_theory); themed through values-night",
        "TheoryTopicActivity" to "XML layout (layout_theory_topic); themed through values-night",
    )

    private val themedCall = Regex("""\bLbmTheme\s*\(\s*palette\s*=\s*currentPalette\s*\(\s*\)\s*\)""")
    private val anyReference = Regex("""\bLbmTheme\b""")
    private val importLine = Regex("""(?m)^\s*import\s+[\w.]+(\s+as\s+\w+)?\s*$""")

    /** Why [source] does not render in the chosen palette, or null when it does. */
    private fun themeProblem(source: String): String? {
        val code = importLine.replace(KotlinSource.withoutComments(source), "")
        val references = anyReference.findAll(code).count()
        val withPalette = themedCall.findAll(code).count()
        return when {
            references == 0 -> "never applies LbmTheme"
            // A screen may set up the theme more than once; every occurrence must pass the palette.
            withPalette < references -> "calls LbmTheme without palette = currentPalette(), so it always renders light"
            else -> null
        }
    }

    /** `<activity>` names, in order. Comments go first; `<activity-alias>` is not an activity. */
    private fun declaredActivities(manifestXml: String): List<String> {
        val xml = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL).replace(manifestXml, "")
        return Regex("""<activity\s[^>]*>""").findAll(xml).map { tag ->
            Regex("""android:name\s*=\s*"([^"]+)"""").find(tag.value)?.groupValues?.get(1)
                ?: error("an <activity> without android:name: ${tag.value}")
        }.toList()
    }

    private val namespace: String by lazy {
        val gradle = File(KotlinSource.srcDir.absoluteFile.parentFile, "build.gradle.kts")
        Regex("""namespace\s*=\s*"([^"]+)"""").find(gradle.readText())?.groupValues?.get(1)
            ?: error("no namespace in $gradle")
    }

    private fun qualified(name: String): String = when {
        name.startsWith(".") -> namespace + name
        '.' !in name -> "$namespace.$name"
        else -> name
    }

    /** Every source set except the test ones: main, and build types such as debug. */
    private val appSourceSets: List<File> by lazy {
        KotlinSource.srcDir.listFiles().orEmpty()
            .filter { it.isDirectory && !it.name.startsWith("test") && !it.name.startsWith("androidTest") }
            .sortedBy { it.name }
    }

    /** Each app source file with its comments already removed — read once, searched per screen. */
    private val sources: List<Pair<File, String>> by lazy {
        appSourceSets.map { File(it, "java") }.filter { it.isDirectory }.flatMap { root ->
            root.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .map { it to KotlinSource.withoutComments(it.readText()) }
                .toList()
        }
    }

    private fun sourceOf(className: String): File? {
        val pkg = className.substringBeforeLast('.')
        val simple = className.substringAfterLast('.')
        val declaresClass = Regex("""\bclass\s+${Regex.escape(simple)}\b""")
        val inPackage = Regex("""(?m)^\s*package\s+${Regex.escape(pkg)}\s*;?\s*$""")
        return sources.firstOrNull { (_, code) ->
            inPackage.containsMatchIn(code) && declaresClass.containsMatchIn(code)
        }?.first
    }

    private val screens: List<Screen> by lazy {
        appSourceSets.map { File(it, "AndroidManifest.xml") }.filter { it.isFile }
            .flatMap { declaredActivities(it.readText()) }
            .map(::qualified)
            .distinct()
            .map { Screen(it, sourceOf(it)) }
    }

    private val composeScreens: List<Screen> get() = screens.filter { it.simpleName !in viewBasedScreens }

    @Test
    fun theScreensAreActuallyFound() {
        // Guards the slice: an empty or shrunken list would make every check below pass. The old
        // file-name filter found 12 screens; the manifest declares 22, 20 of them Compose.
        assertTrue("expected the manifest's Compose screens, found ${composeScreens.size}", composeScreens.size >= 20)
    }

    @Test
    fun everyDeclaredScreenHasItsSource() {
        assertEquals(
            "declared in a manifest, but no source file defines the class",
            emptyList<String>(),
            screens.filter { it.file == null }.map { it.className },
        )
    }

    @Test
    fun everyComposeScreenRendersInTheChosenPalette() {
        val offenders = composeScreens
            .mapNotNull { screen -> screen.file?.let { file -> themeProblem(file.readText())?.let { "${screen.simpleName}: $it" } } }
            .sorted()
        assertEquals(
            "every screen must wrap its content in LbmTheme(palette = currentPalette())",
            emptyList<String>(),
            offenders,
        )
    }

    @Test
    fun theViewBasedAllowListIsStillTrue() {
        val declared = screens.map { it.simpleName }.toSet()
        assertEquals("allow-listed but no longer declared", emptyList<String>(), viewBasedScreens.keys.filter { it !in declared })

        val setContent = Regex("""\bsetContent\s*[{(]""")
        val setContentView = Regex("""\bsetContentView\s*\(""")
        screens.filter { it.simpleName in viewBasedScreens }.forEach { screen ->
            val file = screen.file ?: error("no source for ${screen.className}")
            val code = KotlinSource.withoutComments(file.readText())
            assertTrue("${screen.simpleName} is allow-listed as View-based but never inflates a layout", setContentView.containsMatchIn(code))
            assertTrue(
                "${screen.simpleName} now uses Compose: remove it from the allow-list so its theme is checked",
                !setContent.containsMatchIn(code),
            )
        }
    }

    @Test
    fun theCheckCatchesEveryWayToGetItWrong() {
        // Negative controls: the shapes the check exists to reject must be rejected, and the right
        // one accepted — otherwise the assertions above pass by never matching anything.
        val right = "setContent {\n    LbmTheme(palette = currentPalette()) { Screen() }\n}"
        assertNull(themeProblem(right))
        assertNull(themeProblem("import x.LbmTheme\nsetContent { LbmTheme( palette = currentPalette( ) ) {\n Screen() } }"))

        assertNotNull("bare call", themeProblem("setContent { LbmTheme { Screen() } }"))
        assertNotNull("empty argument list", themeProblem("setContent { LbmTheme() { Screen() } }"))
        assertNotNull("a fixed palette", themeProblem("setContent { LbmTheme(palette = LbmPalette.light) { Screen() } }"))
        assertNotNull("no theme at all", themeProblem("setContent { Screen() }"))
        assertNotNull("only imported", themeProblem("import x.LbmTheme\nsetContent { Screen() }"))
        assertNotNull("only in a comment", themeProblem("// LbmTheme(palette = currentPalette())\nsetContent { Screen() }"))
        assertNotNull(
            "one of two calls without the palette",
            themeProblem("setContent { LbmTheme(palette = currentPalette()) { A() } }\nfun b() { LbmTheme { B() } }"),
        )
    }

    @Test
    fun theManifestReaderSeesWhatAndroidSees() {
        val manifest = """
            <application>
                <activity android:name=".a.First" android:exported="false" />
                <!-- <activity android:name=".a.CommentedOut" /> -->
                <activity-alias android:name=".Alias" android:targetActivity=".a.First" />
                <activity
                    android:exported="true"
                    android:name=".Second">
                    <intent-filter />
                </activity>
            </application>
        """.trimIndent()
        assertEquals(listOf(".a.First", ".Second"), declaredActivities(manifest))
    }
}
