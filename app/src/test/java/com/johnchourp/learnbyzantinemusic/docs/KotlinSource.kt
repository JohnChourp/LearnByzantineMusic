package com.johnchourp.learnbyzantinemusic.docs

import java.io.File

/**
 * Source text for the tests that check how the code is *written* (ClickUp `869f5x286`).
 *
 * A check that matches raw text also matches comments — and in this codebase comments routinely
 * name the very mistake a check hunts for ("a hard-coded white", "`LbmTheme { }` always renders
 * light"). Read raw, such a comment makes a guard fire on code that is correct, or worse, satisfies
 * a guard on code that is wrong. [withoutComments] removes them first: line comments, block
 * comments (nested, as Kotlin allows), while leaving string and character literals intact so a
 * `"//"` inside a string is not mistaken for one.
 */
internal object KotlinSource {

    /** `app/src` from either working directory Gradle may run the tests in. */
    val srcDir: File by lazy {
        listOf(File("app/src"), File("src")).firstOrNull { File(it, "main/java").isDirectory }
            ?: error("Cannot locate app/src from ${File("").absolutePath}")
    }

    val mainRoot: File get() = File(srcDir, "main/java")

    fun withoutComments(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        while (i < source.length) {
            val c = source[i]
            val next = source.getOrNull(i + 1)
            when {
                c == '/' && next == '/' -> {
                    while (i < source.length && source[i] != '\n') i++
                }
                c == '/' && next == '*' -> {
                    var depth = 1
                    i += 2
                    while (i < source.length && depth > 0) {
                        when {
                            source.startsWith("/*", i) -> { depth++; i += 2 }
                            source.startsWith("*/", i) -> { depth--; i += 2 }
                            else -> {
                                if (source[i] == '\n') out.append('\n') // keeps line numbers stable
                                i++
                            }
                        }
                    }
                    out.append(' ')
                }
                source.startsWith("\"\"\"", i) -> {
                    val end = source.indexOf("\"\"\"", i + 3).let { if (it < 0) source.length else it + 3 }
                    out.append(source, i, end)
                    i = end
                }
                c == '"' || c == '\'' -> {
                    var j = i + 1
                    while (j < source.length && source[j] != c && source[j] != '\n') {
                        j += if (source[j] == '\\') 2 else 1
                    }
                    val end = minOf(j + 1, source.length)
                    out.append(source, i, end)
                    i = end
                }
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }
}
