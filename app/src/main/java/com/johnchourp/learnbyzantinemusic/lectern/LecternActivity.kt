package com.johnchourp.learnbyzantinemusic.lectern

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.lectern.ui.LecternLibraryScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * «Ψηφιακό αναλόγιο» (ClickUp `869f5x2e7`): the library of the user's **own** PDFs — the app ships none,
 * so there is nothing to license — each opened on [LecternReaderActivity], with the ison bar that
 * remembers the ήχος of every page.
 *
 * **Flow.** «Άνοιγμα PDF» is the system picker (`ACTION_OPEN_DOCUMENT`, `application/pdf`): a PDF from
 * the phone, Downloads, Drive or any other document provider, with no storage permission. The PDF is
 * added to the library with a persisted READ grant ([LecternLibrary.add], taken inside `runCatching`),
 * then opened. A provider that gives no lasting grant, or a library already at
 * [LecternLibrary.MAX_ENTRIES], still opens the PDF — this time only — and says so. Removing an entry
 * releases its grant; the file and its page → ήχος map stay.
 *
 * **Why a home tile, not a card in «8 Ήχοι».** The lectern is a tool used every day at the ἀναλόγιο,
 * like the recordings and the notes, so it sits with them in «Εξάσκηση & Εργαλεία». The 8 Ήχοι page is
 * already the app's longest; a card low on it would be both hidden and in the way.
 *
 * **Touches:** `lectern_library` (read + write). Portrait, like every screen but the reader.
 */
class LecternActivity : BaseActivity() {

    private lateinit var prefs: LecternPrefs
    private var library by mutableStateOf(LecternLibrary.EMPTY)

    private val pickPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) addAndOpen(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = LecternPrefs(this)
        library = prefs.library()
        setContent {
            LbmTheme(palette = currentPalette()) {
                LecternLibraryScreen(
                    entries = library.entries,
                    onAdd = { pickPdf.launch(arrayOf(PDF_MIME_TYPE)) },
                    onOpen = { entry -> open(Uri.parse(entry.uri), entry.title) },
                    onRemove = { entry -> remove(entry.uri) },
                    onBack = ::finish,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The reader keeps the page it was left on.
        library = prefs.library()
    }

    private fun addAndOpen(uri: Uri) {
        lifecycleScope.launch {
            val title = withContext(Dispatchers.IO) { displayName(uri) } ?: getString(R.string.lectern_untitled)
            val result = prefs.library().add(uri.toString(), title, System.currentTimeMillis(), ContentResolverGrants(contentResolver))
            save(result.library)
            when (result.outcome) {
                LecternLibrary.Added.ADDED, LecternLibrary.Added.ALREADY_THERE -> Unit
                LecternLibrary.Added.FULL -> toast(getString(R.string.lectern_library_full, LecternLibrary.MAX_ENTRIES))
                LecternLibrary.Added.NOT_KEPT -> toast(getString(R.string.lectern_not_kept))
            }
            startActivity(LecternReaderActivity.intent(this@LecternActivity, uri, title))
        }
    }

    private fun open(uri: Uri, title: String) {
        save(prefs.library().opened(uri.toString(), System.currentTimeMillis()))
        startActivity(LecternReaderActivity.intent(this, uri, title))
    }

    private fun remove(uri: String) {
        save(prefs.library().remove(uri, ContentResolverGrants(contentResolver)))
        toast(getString(R.string.lectern_library_removed))
    }

    private fun save(next: LecternLibrary) {
        prefs.saveLibrary(next)
        library = next
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    /** The provider's name for the file, or null. */
    private fun displayName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private companion object {
        const val PDF_MIME_TYPE = "application/pdf"
    }
}
