package com.johnchourp.learnbyzantinemusic.lectern

import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.random.Random

/**
 * A PDF's page → ήχος map is named by the SHA-256 of the file's bytes (ClickUp `869f5x2e7`) — never by
 * its URI — so the same file finds its map from any folder, any provider and any phone.
 */
class LecternFileKeyTest {

    private fun sha(bytes: ByteArray) = LecternFileKey.sha256Hex(ByteArrayInputStream(bytes))

    @Test
    fun `it is SHA-256, in lowercase hex`() {
        // FIPS 180-2 test vectors.
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha(ByteArray(0)))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha("abc".toByteArray()))
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            sha("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".toByteArray()),
        )
    }

    @Test
    fun `it reads the whole file, past its buffer`() {
        val big = Random(7).nextBytes(300_000)
        val changedAtTheEnd = big.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertNotEquals(sha(big), sha(changedAtTheEnd))
    }

    @Test
    fun `the same bytes are the same key wherever they come from, and other bytes another key`() {
        val pdf = "%PDF-1.7 a hymn".toByteArray()
        // Two reads of the same content — say from Downloads and from Drive — name the same map.
        assertEquals(AppPrefs.lecternPagesKeyName(sha(pdf)), AppPrefs.lecternPagesKeyName(sha(pdf.copyOf())))
        assertNotEquals(sha(pdf), sha("%PDF-1.7 another hymn".toByteArray()))
    }

    @Test
    fun `a copy made to read a PDF is byte for byte the PDF, and hashes the same`() {
        val pdf = Random(11).nextBytes(200_001)
        val copy = ByteArrayOutputStream()
        val hash = LecternFileKey.copyAndHash(ByteArrayInputStream(pdf), copy)
        assertArrayEquals(pdf, copy.toByteArray())
        assertEquals(sha(pdf), hash)
    }

    @Test
    fun `the key is the registered prefix and the hash, which only a hash satisfies`() {
        val hash = sha("abc".toByteArray())
        assertEquals("lectern_pages_$hash", AppPrefs.lecternPagesKeyName(hash))
        assertTrue(AppPrefs.LecternPageModes.name.startsWith(AppPrefs.LECTERN_PAGES_KEY_PREFIX))
        assertTrue(LecternFileKey.isSha256Hex(hash))
        listOf(
            "",
            hash.uppercase(),
            hash.dropLast(1),
            hash + "0",
            hash.replaceFirst('b', 'g'),
            "content://com.android.providers.downloads.documents/document/42",
        ).forEach { assertFalse(it, LecternFileKey.isSha256Hex(it)) }
    }
}
