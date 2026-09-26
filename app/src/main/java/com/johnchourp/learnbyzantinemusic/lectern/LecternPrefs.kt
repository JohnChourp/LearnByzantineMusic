package com.johnchourp.learnbyzantinemusic.lectern

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.johnchourp.learnbyzantinemusic.modes.ToneTimbre
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs

/**
 * Everything the lectern keeps, through the registry (ClickUp `869f5x2e7`): its library
 * ([AppPrefs.LecternLibraryEntries], this phone only) and the page → ήχος map of each PDF
 * ([AppPrefs.LecternPageModes], named by the file's SHA-256, travels in «Δεδομένα μάθησης»).
 *
 * It also **reads**, never writes, three choices of the 8 Ήχοι page, so the lectern's ison is that
 * page's ison: the chosen timbre, «Συνέχισε στο παρασκήνιο», and each ήχος's «Μεταφορά βάσης» — the
 * shift a ήχος starts with when it is first set on a page, which the page then keeps as its own.
 */
internal class LecternPrefs(context: Context) {

    private val library = AppPrefs.open(context, AppPrefs.Store.LECTERN_LIBRARY)
    private val pages = AppPrefs.open(context, AppPrefs.Store.LECTERN_PAGES)
    private val eightModes = AppPrefs.open(context, AppPrefs.Store.EIGHT_MODES)

    fun library(): LecternLibrary = LecternLibrary.decode(library.getString(AppPrefs.LecternLibraryEntries.name, null))

    fun saveLibrary(value: LecternLibrary) {
        library.edit().putString(AppPrefs.LecternLibraryEntries.name, value.encode()).apply()
    }

    fun pages(sha256Hex: String): LecternPagesCodec.Decoded =
        LecternPagesCodec.decode(pages.getString(AppPrefs.lecternPagesKeyName(sha256Hex), null))

    /** Writes the map of one PDF; an empty map removes its key rather than storing nothing. */
    fun savePages(sha256Hex: String, assignments: List<PageAssignment>) {
        val name = AppPrefs.lecternPagesKeyName(sha256Hex)
        val editor = pages.edit()
        if (assignments.isEmpty()) editor.remove(name) else editor.putString(name, LecternPagesCodec.encode(assignments))
        editor.apply()
    }

    /** The 8 Ήχοι page's «Μεταφορά βάσης» for [mode], inside the shared range. */
    fun eightModesShift(mode: Mode): Int =
        BaseShift.clamp(eightModes.getInt(AppPrefs.baseShiftKeyName(mode.key), BaseShift.DEFAULT_MORIA))

    fun timbre(): ToneTimbre {
        val stored = eightModes.getString(AppPrefs.SelectedToneTimbre.name, ToneTimbre.CLEAN.name)
        return ToneTimbre.entries.firstOrNull { it.name == stored } ?: ToneTimbre.CLEAN
    }

    fun isonInBackground(): Boolean = eightModes.getBoolean(AppPrefs.IsonInBackground.name, false)
}

/** The library's grants, taken and released through the platform; a refusal is a false, never a crash. */
internal class ContentResolverGrants(private val resolver: ContentResolver) : LecternLibrary.Grants {

    override fun take(uri: String): Boolean = runCatching {
        resolver.takePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }.isSuccess

    override fun release(uri: String) {
        runCatching { resolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
}
