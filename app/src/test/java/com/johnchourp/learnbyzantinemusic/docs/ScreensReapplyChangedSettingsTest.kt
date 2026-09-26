package com.johnchourp.learnbyzantinemusic.docs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A screen shows the language, font size and theme saved **now**, not the ones it was created with
 * (reported on a device, 2026-09-26).
 *
 * `BaseActivity` applies the three at attach time, and Settings recreates only itself when one
 * changes, so the home screen waiting behind it came back in the old theme until the app restarted.
 * The fix lives in one place: BaseActivity remembers what it was attached with and recreates itself
 * in `onResume` when that has changed. Every screen extends BaseActivity; `EveryScreenAppliesTheThemeTest`
 * already requires `currentPalette()`, which only BaseActivity provides.
 *
 * No unit test here can start an Activity (there is no Robolectric), so this is a source check of
 * that one place, with comments removed first ([KotlinSource.withoutComments]):
 * - `attachBaseContext` records the snapshot, and applies exactly the settings the snapshot reads.
 *   A setting applied there but not remembered would come back stale;
 * - `onResume` calls `super.onResume()`, reads the snapshot again and recreates only when it differs.
 */
class ScreensReapplyChangedSettingsTest {

    /** Each setting BaseActivity applies at attach time, and the read that must remember it. */
    private val remembered = mapOf(
        "AppLanguage.wrapContextWithLocale" to "AppLanguage.getSavedLanguageCode",
        "AppFontScale.wrapContextWithFontScale" to "AppFontScale.getSavedStep",
        "wrapContextWithThemeMode" to "AppThemeMode.saved",
    )

    private val code: String by lazy {
        val file = File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/BaseActivity.kt")
        KotlinSource.withoutComments(file.readText())
    }

    private val attachDeclaration = Regex("""override\s+fun\s+attachBaseContext\s*\(""")
    private val resumeDeclaration = Regex("""override\s+fun\s+onResume\s*\(\s*\)""")
    private val snapshotDeclaration = Regex("""\bclass\s+AttachedSettings\b""")
    private val wrapCall = Regex("""(?:\b[A-Z]\w*\.)?\bwrapContextWith\w+(?=\s*\()""")
    private val recordsSnapshot = Regex("""\battachedWith\s*=\s*[^\n]*\bAttachedSettings\s*(?:::|\.)\s*read\b""")
    private val recreateWhenChanged = Regex(
        """if\s*\((.*?)\)\s*\{\s*recreate\s*\(\s*\)""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val comparison = Regex(
        """AttachedSettings\.read\(\s*this\s*\)\s*!=\s*attachedWith\b|\battachedWith\s*!=\s*AttachedSettings\.read\(\s*this\s*\)""",
    )

    /** The body of the first declaration matching [declaration], braces balanced; null when absent. */
    private fun body(source: String, declaration: Regex): String? {
        val match = declaration.find(source) ?: return null
        val open = source.indexOf('{', match.range.last + 1)
        if (open < 0) return null
        var depth = 0
        for (i in open until source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> if (--depth == 0) return source.substring(open + 1, i)
            }
        }
        return null
    }

    /** Why [resumeBody] does not recreate the screen on a changed setting, or null when it does. */
    private fun resumeProblem(resumeBody: String): String? {
        val condition = recreateWhenChanged.find(resumeBody)?.groupValues?.get(1)
        return when {
            !Regex("""\bsuper\.onResume\s*\(\s*\)""").containsMatchIn(resumeBody) -> "does not call super.onResume()"
            condition == null -> "never calls recreate() under a condition"
            !comparison.containsMatchIn(condition) -> "recreates without comparing AttachedSettings.read(this) with attachedWith"
            else -> null
        }
    }

    @Test
    fun attachBaseContextRemembersExactlyWhatItApplies() {
        val attach = body(code, attachDeclaration)
        assertNotNull("BaseActivity no longer overrides attachBaseContext", attach)
        assertTrue("attachBaseContext does not record AttachedSettings.read into attachedWith", recordsSnapshot.containsMatchIn(attach!!))

        val applied = wrapCall.findAll(attach).map { it.value }.toSet()
        assertEquals("the settings attachBaseContext applies must be the ones remembered", remembered.keys, applied)

        val snapshot = body(code, snapshotDeclaration)
        assertNotNull("AttachedSettings is gone", snapshot)
        remembered.values.forEach { read ->
            assertTrue("AttachedSettings.read does not read $read", Regex("""\b${Regex.escape(read)}\s*\(""").containsMatchIn(snapshot!!))
        }
    }

    @Test
    fun onResumeRecreatesTheScreenWhenASettingChanged() {
        val resume = body(code, resumeDeclaration)
        assertNotNull("BaseActivity no longer overrides onResume", resume)
        assertNull(resumeProblem(resume!!))
    }

    @Test
    fun theChecksCatchEveryWayToGetItWrong() {
        // Negative controls: without them a helper that never matches would pass every assertion above.
        assertEquals("a { b } c", body("fun f() {a { b } c} fun g() {}", Regex("""fun\s+f""")))
        assertNull(body("fun f() = 1", Regex("""fun\s+g""")))

        assertEquals(
            setOf("AppLanguage.wrapContextWithLocale", "wrapContextWithThemeMode"),
            wrapCall.findAll("val a = AppLanguage.wrapContextWithLocale(b)\nsuper.attach(wrapContextWithThemeMode(a))").map { it.value }.toSet(),
        )
        assertFalse(recordsSnapshot.containsMatchIn("val attachedWith = null"))

        val right = "super.onResume()\nif (attachedWith != null && AttachedSettings.read(this) != attachedWith) {\n recreate()\n}"
        assertNull(resumeProblem(right))
        assertNotNull("always recreates", resumeProblem("super.onResume()\nrecreate()"))
        assertNotNull("recreates on another condition", resumeProblem("super.onResume()\nif (isFinishing) { recreate() }"))
        assertNotNull("never recreates", resumeProblem("super.onResume()\nval now = AttachedSettings.read(this)"))
        assertNotNull("skips super", resumeProblem("if (AttachedSettings.read(this) != attachedWith) { recreate() }"))
    }
}
