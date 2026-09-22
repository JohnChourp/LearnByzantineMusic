package com.johnchourp.learnbyzantinemusic.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The half of ClickUp `869f4tpwz` that keeps the registry true over time.
 *
 * `AppPrefsRegistryTest` proves the registry says the right thing; this one proves nothing bypasses
 * it. A key added straight at a call site compiles, works on the author's device and never shows up
 * in the registry — until someone renames the other copy.
 *
 * Two independent checks, because they fail for different reasons:
 * 1. no preferences **file** is opened outside [AppPrefs.open];
 * 2. no preference **key** is passed as a string literal to a SharedPreferences accessor.
 */
class NoUnregisteredPrefKeyTest {

    private val registryFile = "AppPrefs.kt"

    private val mainSources: List<File> by lazy {
        val root = listOf(File("app/src/main/java"), File("src/main/java")).firstOrNull { it.isDirectory }
            ?: error("Cannot locate the main source root from ${File("").absolutePath}")
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    @Test
    fun theSourceRootIsActuallyFound() {
        // Without this, the assertions below would pass over an empty file list.
        assertTrue("expected to scan real sources", mainSources.size > 50)
        assertTrue(mainSources.any { it.name == registryFile })
    }

    @Test
    fun onlyTheRegistryOpensAPreferencesFile() {
        assertEquals(
            "getSharedPreferences belongs to AppPrefs.open",
            listOf(registryFile),
            mainSources.filter { "getSharedPreferences(" in it.readText() }.map { it.name }.sorted()
        )
    }

    /**
     * `getString("x", …)` / `putInt("x", …)` / `remove("x")` / `contains("x")` with a literal first
     * argument.
     *
     * Scoped to files that could actually be holding a `SharedPreferences`. The pattern is purely
     * textual and `JSONObject` has the very same accessor names, so an unscoped version flags every
     * JSON parser in the app: `AnastasimatarionCatalog.kt` doing `modeJson.getString("key")` turned
     * it red while breaking nothing (measured 2026-09-22, ClickUp `869dbkkwf`).
     *
     * The narrowing does **not** open a hole. [onlyTheRegistryOpensAPreferencesFile] already proves
     * only `AppPrefs.kt` calls `getSharedPreferences`, so any other file holding one must name
     * `SharedPreferences` (as a type) or `AppPrefs` (to obtain it) somewhere in its text — there is
     * no third way to get the instance. [theScopeStillCatchesARealOffender] pins that.
     */
    private val literalKey = Regex(
        """\.(get|put)(String|StringSet|Int|Long|Boolean|Float)\(\s*"|""" +
            """\.(remove|contains)\(\s*""""
    )

    private fun couldHoldPreferences(text: String): Boolean =
        "SharedPreferences" in text || "AppPrefs" in text

    @Test
    fun noPreferenceAccessorTakesALiteralKey() {
        val offenders = mainSources
            .filter { it.name != registryFile }
            .filter { couldHoldPreferences(it.readText()) && literalKey.containsMatchIn(it.readText()) }
            .map { it.name }
            .sorted()
        assertEquals("these pass a literal preference key instead of an AppPrefs entry", emptyList<String>(), offenders)
    }

    /**
     * The negative control for the scoping above: a file that really does bypass the registry is
     * still caught, and a JSON parser really is let through. Without this, narrowing the scope could
     * silently turn the whole check into a no-op.
     */
    @Test
    fun theScopeStillCatchesARealOffender() {
        val offender = """
            import android.content.SharedPreferences
            fun load(p: SharedPreferences) = p.getString("font_step", null)
        """.trimIndent()
        assertTrue("a real bypass must still be flagged",
            couldHoldPreferences(offender) && literalKey.containsMatchIn(offender))

        val jsonParser = """
            import org.json.JSONObject
            fun parse(o: JSONObject) = o.getString("key")
        """.trimIndent()
        assertTrue("a JSON parser must not be flagged", !couldHoldPreferences(jsonParser))

        // And the scoping must not be what does the work on its own: the regex still has to match.
        assertTrue(literalKey.containsMatchIn(jsonParser))
    }

    @Test
    fun noFileOtherThanTheRegistryNamesAPreferencesFile() {
        val storeName = Regex(""""(learn_byzantine_music_\w+|eight_modes_base_shift_prefs)"""")
        assertEquals(
            listOf(registryFile),
            mainSources.filter { storeName.containsMatchIn(it.readText()) }.map { it.name }.sorted()
        )
    }
}
