package com.johnchourp.learnbyzantinemusic.lectern

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

/**
 * Which PDF a page → ήχος map belongs to (ClickUp `869f5x2e7`): the **SHA-256 of the file's bytes**,
 * as 64 lowercase hex digits — the tail of `AppPrefs.lecternPagesKeyName`.
 *
 * Never the file's URI. A `content://` URI names a grant on this phone: the same PDF opened from Drive
 * and from Downloads has two different ones, and on another phone neither means anything. The bytes are
 * the same everywhere, so the map follows the file — to a second copy of it, to a new phone through the
 * «Δεδομένα μάθησης» file, back into the library after it was removed. An edited PDF is a different file
 * and starts with no map: the one thing a hash cannot follow, and the right answer when pages may have
 * moved.
 */
object LecternFileKey {

    private const val BUFFER_BYTES = 64 * 1024

    /** SHA-256 of everything [input] yields. Reads to the end; the caller closes it. */
    fun sha256Hex(input: InputStream): String = copyAndHash(input, null)

    /**
     * Copies [input] to [output] (when there is one) and returns the SHA-256 of what went through — one
     * pass for a PDF that must be copied before it can be read. The caller closes both.
     */
    fun copyAndHash(input: InputStream, output: OutputStream?): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
            output?.write(buffer, 0, read)
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    /** True for exactly what [sha256Hex] returns: 64 lowercase hex digits. */
    fun isSha256Hex(value: String): Boolean =
        value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' }
}
