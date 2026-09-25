package com.johnchourp.learnbyzantinemusic.ui.theme

import androidx.compose.ui.graphics.Color
import java.io.File

/**
 * Reads the app's colour resources the way Android resolves them, for pure-JVM tests
 * (ClickUp `869f5x286`).
 *
 * The View-based screens and the pages menu take their colours from XML, which no unit test can
 * inflate. This reads the XML itself:
 * - `values-night` wins when it defines the name, `values` otherwise — Android's own lookup;
 * - `@color/` references are followed, so a drawable pointing at a night-aware colour resolves to
 *   the night value;
 * - XML comments are removed before anything is matched, so a commented-out colour is never read
 *   as a live one.
 */
internal object AndroidColorResources {

    private val resDir: File by lazy {
        listOf(File("app/src/main/res"), File("src/main/res")).firstOrNull { it.isDirectory }
            ?: error("Cannot locate the res directory from ${File("").absolutePath}")
    }

    fun withoutXmlComments(xml: String): String =
        Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL).replace(xml, "")

    /** The `<color>` entries of `[valuesDir]/colors.xml` exactly as written: a literal or a reference. */
    fun rawColors(valuesDir: String): Map<String, String> {
        val file = File(resDir, "$valuesDir/colors.xml")
        if (!file.isFile) return emptyMap()
        val xml = withoutXmlComments(file.readText())
        return Regex("""<color\s+name="([^"]+)"\s*>\s*([^<\s]+)\s*</color>""")
            .findAll(xml)
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    /** What `R.color.[name]` resolves to with the night flag set to [night]. */
    fun color(name: String, night: Boolean): Color {
        val value = (if (night) rawColors("values-night")[name] else null)
            ?: rawColors("values")[name]
            ?: error("no colour resource named $name")
        return resolve(value, night)
    }

    /** The `<solid android:color>` of shape drawable [name], resolved with the night flag at [night]. */
    fun drawableSolid(name: String, night: Boolean): Color {
        val file = listOfNotNull(
            if (night) File(resDir, "drawable-night/$name.xml") else null,
            File(resDir, "drawable/$name.xml"),
        ).firstOrNull { it.isFile } ?: error("no drawable named $name")
        val xml = withoutXmlComments(file.readText())
        val value = Regex("""<solid\b[^>]*android:color="([^"]+)"""").find(xml)?.groupValues?.get(1)
            ?: error("$name has no <solid android:color>")
        return resolve(value, night)
    }

    fun resolve(value: String, night: Boolean): Color = when {
        value.startsWith("@color/") -> color(value.removePrefix("@color/"), night)
        value.startsWith("#") -> parseHex(value)
        else -> error("unsupported colour value: $value")
    }

    /** `#RRGGBB` or `#AARRGGBB`; anything else fails loudly rather than being guessed at. */
    fun parseHex(value: String): Color {
        val digits = value.removePrefix("#")
        val argb = when (digits.length) {
            6 -> "FF$digits"
            8 -> digits
            else -> error("unsupported colour literal: $value")
        }
        return Color(argb.toLong(16))
    }
}
