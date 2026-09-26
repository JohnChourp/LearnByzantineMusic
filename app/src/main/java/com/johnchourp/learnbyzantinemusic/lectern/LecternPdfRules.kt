package com.johnchourp.learnbyzantinemusic.lectern

import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Why a PDF did not open (ClickUp `869f5x2e7`). Each has its own sentence on screen, in both languages,
 * so a password or a damaged file is a message — never a crash, never an empty page.
 */
enum class LecternOpenFailure {
    /** The grant is gone: revoked, lost, or never lasting. Opening the file again from the picker gives a new one. */
    NO_ACCESS,

    /** Moved, renamed or deleted where it lives. */
    NOT_FOUND,

    /** Protected by a password, which `PdfRenderer` cannot take. */
    PASSWORD_PROTECTED,

    /** Not a PDF `PdfRenderer` can read, or one with no pages. */
    DAMAGED,

    /** The file could not be read to the end — a provider or a storage error. Trying again may work. */
    UNREADABLE;

    companion object {
        /** A failure while getting at the file's bytes: the provider's words, not the PDF's. */
        fun whileReading(error: Throwable): LecternOpenFailure = when (error) {
            is FileNotFoundException -> NOT_FOUND
            is SecurityException -> NO_ACCESS
            else -> UNREADABLE
        }

        /**
         * A failure of `PdfRenderer` on bytes that were read: it throws `SecurityException` for a
         * password, `IOException` for what it cannot parse. The same `SecurityException` means
         * something else while reading ([whileReading]), which is why the two are kept apart.
         */
        fun whileParsing(error: Throwable): LecternOpenFailure = when (error) {
            is SecurityException -> PASSWORD_PROTECTED
            is IOException -> DAMAGED
            else -> DAMAGED
        }
    }
}

/**
 * How big the reader renders a page (ClickUp `869f5x2e7`): as wide as the screen shows it, so the neumes
 * are drawn at the screen's own resolution — but never more than [MAX_PIXELS]. A scanned book in
 * landscape on a tablet would otherwise ask for a 30 MB bitmap per page; the cap keeps one page near
 * 16 MB (ARGB, which `PdfRenderer` requires), and the reader holds at most three.
 */
object LecternRenderSize {

    /** 4 megapixels: 16 MB as ARGB_8888. */
    const val MAX_PIXELS = 4_000_000L

    /** Width and height in pixels for a page of [pageWidth] × [pageHeight] points shown [targetWidthPx] wide. */
    fun forPage(pageWidth: Int, pageHeight: Int, targetWidthPx: Int): Pair<Int, Int> {
        val width = targetWidthPx.coerceAtLeast(1).toDouble()
        val aspect = if (pageWidth > 0 && pageHeight > 0) pageHeight.toDouble() / pageWidth else 1.0
        val height = width * aspect
        val scale = if (width * height > MAX_PIXELS) sqrt(MAX_PIXELS / (width * height)) else 1.0
        return floor(width * scale).toInt().coerceAtLeast(1) to floor(height * scale).toInt().coerceAtLeast(1)
    }
}
