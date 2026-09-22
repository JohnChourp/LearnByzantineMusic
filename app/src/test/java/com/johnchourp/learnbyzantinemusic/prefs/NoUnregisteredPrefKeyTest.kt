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

    @Test
    fun noPreferenceAccessorTakesALiteralKey() {
        // getString("x", …) / putInt("x", …) / remove("x") / contains("x") with a literal first arg.
        val literalKey = Regex(
            """\.(get|put)(String|StringSet|Int|Long|Boolean|Float)\(\s*"|""" +
                """\.(remove|contains)\(\s*""""
        )
        val offenders = mainSources
            .filter { it.name != registryFile }
            .filter { literalKey.containsMatchIn(it.readText()) }
            .map { it.name }
            .sorted()
        assertEquals("these pass a literal preference key instead of an AppPrefs entry", emptyList<String>(), offenders)
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
