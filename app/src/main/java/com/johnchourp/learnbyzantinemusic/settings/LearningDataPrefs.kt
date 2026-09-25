package com.johnchourp.learnbyzantinemusic.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract
import com.johnchourp.learnbyzantinemusic.notes.NotesPrefs
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs

/**
 * The Android side of the «Δεδομένα μάθησης» file ([LearningDataFile]): reading every preferences
 * file for an export, writing an accepted import, and the two system pickers.
 *
 * The pickers open in the notes backup folder when the learner has one (it exists only once the
 * Notes screen has been opened), so the file lands next to the notes snapshots; otherwise Android
 * chooses where they open.
 */
internal object LearningDataPrefs {

    /** Every preferences file as it is now; [LearningDataFile.encode] keeps only what travels. */
    fun snapshot(context: Context): Map<AppPrefs.Store, Map<String, Any?>> =
        AppPrefs.Store.entries.associateWith { store -> AppPrefs.open(context, store).all.toMap() }

    /** One `commit()` per preferences file, so the restart that follows sees everything written. */
    fun writer(context: Context) = LearningDataFile.Writer { store, values ->
        val editor = AppPrefs.open(context, store).edit()
        values.forEach { (name, value) ->
            when (value) {
                is Int -> editor.putInt(name, value)
                is Long -> editor.putLong(name, value)
                is Boolean -> editor.putBoolean(name, value)
                is String -> editor.putString(name, value)
                is Set<*> -> editor.putStringSet(name, value.map { it.toString() }.toSet())
                else -> return@Writer false
            }
        }
        editor.commit()
    }

    /** The notes folder as a place a picker can open in, or null. */
    fun startFolder(context: Context): Uri? = NotesPrefs(context).getFolderUri()?.let { tree ->
        runCatching { DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree)) }
            .getOrNull()
    }
}

/** «Save as» for the file, opening in [Request.folder] when there is one. */
internal class SaveLearningDataFile : ActivityResultContract<SaveLearningDataFile.Request, Uri?>() {

    data class Request(val fileName: String, val folder: Uri?)

    override fun createIntent(context: Context, input: Request): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(LearningDataFile.MIME_TYPE)
            .putExtra(Intent.EXTRA_TITLE, input.fileName)
            .startingIn(input.folder)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}

/** «Open» for the file, opening in the given folder when there is one. */
internal class OpenLearningDataFile : ActivityResultContract<Uri?, Uri?>() {

    override fun createIntent(context: Context, input: Uri?): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            // Some file managers label a .json file text/plain or give it no type at all.
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(LearningDataFile.MIME_TYPE, "text/plain", "*/*"))
            .startingIn(input)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}

/** `EXTRA_INITIAL_URI` exists from Android 8; before that the picker opens where it chooses. */
private fun Intent.startingIn(folder: Uri?): Intent = apply {
    if (folder != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        putExtra(DocumentsContract.EXTRA_INITIAL_URI, folder)
    }
}
