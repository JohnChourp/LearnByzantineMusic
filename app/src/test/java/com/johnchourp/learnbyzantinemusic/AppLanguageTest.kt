package com.johnchourp.learnbyzantinemusic

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the pure language-code helpers the redesigned Settings language picker relies on:
 * normalization to a supported code and the native display name.
 */
class AppLanguageTest {

    @Test
    fun supported_codes_are_el_and_en() {
        assertEquals("el", AppLanguage.languageGreek)
        assertEquals("en", AppLanguage.languageEnglish)
    }

    @Test
    fun normalizeLanguageCode_keeps_supported_codes() {
        assertEquals("el", AppLanguage.normalizeLanguageCode("el"))
        assertEquals("en", AppLanguage.normalizeLanguageCode("en"))
    }

    @Test
    fun normalizeLanguageCode_falls_back_to_greek() {
        assertEquals("el", AppLanguage.normalizeLanguageCode(null))
        assertEquals("el", AppLanguage.normalizeLanguageCode(""))
        assertEquals("el", AppLanguage.normalizeLanguageCode("fr"))
        // Case-sensitive set membership: an upper-case code is not supported, so it falls back.
        assertEquals("el", AppLanguage.normalizeLanguageCode("EN"))
    }

    @Test
    fun getNativeLanguageName_returns_native_label() {
        assertEquals("English", AppLanguage.getNativeLanguageName("en"))
        assertEquals("Ελληνικά", AppLanguage.getNativeLanguageName("el"))
        // Unsupported codes normalize to Greek first.
        assertEquals("Ελληνικά", AppLanguage.getNativeLanguageName("fr"))
    }
}
