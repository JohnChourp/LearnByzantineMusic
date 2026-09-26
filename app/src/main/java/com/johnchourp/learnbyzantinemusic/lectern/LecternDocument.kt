package com.johnchourp.learnbyzantinemusic.lectern

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * One PDF open in the lectern's reader (ClickUp `869f5x2e7`), read with `android.graphics.pdf.PdfRenderer`
 * — the platform's own renderer, there since Android 5: no library, no permission.
 *
 * **Opening ([open]).** The file is read once to take its SHA-256 ([LecternFileKey]), which names its
 * page → ήχος map, and `PdfRenderer` gets a seekable descriptor of it. Some providers — a cloud file
 * still streaming in, say — hand out a pipe, which cannot seek: the PDF is then copied into the app's
 * cache first, hashed on the way, and read from there; the copy is deleted when the document closes. A
 * password, a damaged file or a lost grant is an [Opened.Failed] with its [LecternOpenFailure] — never a
 * crash.
 *
 * **One thread.** `PdfRenderer` is not thread-safe and holds one page open at a time, so everything that
 * touches it — each [render], and [close] after the last one — runs on this document's own single thread.
 * A render that runs out of memory on a huge scanned page returns null; the reader says so on that page.
 */
internal class LecternDocument private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    private val cacheCopy: File?,
    private val executor: ExecutorService,
    val sha256Hex: String,
    val pageCount: Int,
) {
    private val thread: CoroutineDispatcher = executor.asCoroutineDispatcher()

    /** Touched only on [thread]. */
    private var closed = false

    /** Page [pageIndex] drawn [targetWidthPx] wide on white, or null when it cannot be drawn. */
    suspend fun render(pageIndex: Int, targetWidthPx: Int): Bitmap? = withContext(thread) {
        if (closed || pageIndex !in 0 until pageCount) return@withContext null
        try {
            val page = renderer.openPage(pageIndex)
            try {
                val (width, height) = LecternRenderSize.forPage(page.width, page.height, targetWidthPx)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                // PdfRenderer draws only what the page paints: a page with no background would be
                // transparent, and black under the night tint's inversion.
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } finally {
                page.close()
            }
        } catch (failure: Exception) {
            Log.w(TAG, "page $pageIndex could not be drawn", failure)
            null
        } catch (_: OutOfMemoryError) {
            Log.w(TAG, "page $pageIndex is too large to draw")
            null
        }
    }

    /** Closes the renderer after any render still running, then the file, then deletes a cache copy. */
    fun close() {
        executor.execute {
            if (closed) return@execute
            closed = true
            runCatching { renderer.close() }
            runCatching { descriptor.close() }
            cacheCopy?.delete()
        }
        executor.shutdown()
    }

    sealed interface Opened {
        data class Ready(val document: LecternDocument) : Opened
        data class Failed(val failure: LecternOpenFailure) : Opened
    }

    companion object {
        private const val TAG = "LbmLectern"
        private const val CACHE_DIR = "lectern"

        /** Opens [uri] on a new document thread. Blocking: call it off the main thread. */
        fun open(context: Context, uri: Uri): Opened {
            val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "LecternPdf") }
            val opened = runCatching { executor.submit<Opened> { openOnThisThread(context, uri, executor) }.get() }
                .getOrElse { failure ->
                    Log.w(TAG, "could not open the PDF", failure)
                    Opened.Failed(LecternOpenFailure.UNREADABLE)
                }
            if (opened !is Opened.Ready) executor.shutdown()
            return opened
        }

        private fun openOnThisThread(context: Context, uri: Uri, executor: ExecutorService): Opened {
            val resolver = context.contentResolver
            val descriptor = try {
                resolver.openFileDescriptor(uri, "r")
            } catch (failure: Exception) {
                return Opened.Failed(LecternOpenFailure.whileReading(failure))
            } ?: return Opened.Failed(LecternOpenFailure.UNREADABLE)

            // A regular file has a size; a pipe or a socket reports -1 and cannot seek.
            if (descriptor.statSize >= 0) {
                val sha = try {
                    // A dup shares the file offset, so it is put back at the start for PdfRenderer.
                    ParcelFileDescriptor.AutoCloseInputStream(descriptor.dup()).use(LecternFileKey::sha256Hex)
                        .also { Os.lseek(descriptor.fileDescriptor, 0L, OsConstants.SEEK_SET) }
                } catch (_: ErrnoException) {
                    // It has a size but will not seek: read it through a copy instead.
                    runCatching { descriptor.close() }
                    return openCopy(context, uri, executor)
                } catch (failure: IOException) {
                    runCatching { descriptor.close() }
                    return Opened.Failed(LecternOpenFailure.whileReading(failure))
                }
                when (val parsed = parse(descriptor)) {
                    is Parsed.Ok -> return ready(descriptor, parsed.renderer, null, executor, sha)
                    is Parsed.Failed -> {
                        runCatching { descriptor.close() }
                        return Opened.Failed(parsed.failure)
                    }
                    // Seekable by its size, not in fact: fall through to the copy.
                    Parsed.NotSeekable -> runCatching { descriptor.close() }
                }
            } else {
                runCatching { descriptor.close() }
            }
            return openCopy(context, uri, executor)
        }

        /** The provider could not give a seekable file: copy it to the cache, hashing it on the way. */
        private fun openCopy(context: Context, uri: Uri, executor: ExecutorService): Opened {
            val directory = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
            // Copies left by a reader the system killed before it could close.
            directory.listFiles()?.forEach { it.delete() }
            val copy = runCatching { File.createTempFile("pdf", ".pdf", directory) }.getOrElse {
                return Opened.Failed(LecternOpenFailure.UNREADABLE)
            }
            val sha = try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: return Opened.Failed(LecternOpenFailure.UNREADABLE).also { copy.delete() }
                input.use { stream -> FileOutputStream(copy).use { output -> LecternFileKey.copyAndHash(stream, output) } }
            } catch (failure: Exception) {
                copy.delete()
                return Opened.Failed(LecternOpenFailure.whileReading(failure))
            }
            val descriptor = runCatching { ParcelFileDescriptor.open(copy, ParcelFileDescriptor.MODE_READ_ONLY) }
                .getOrElse {
                    copy.delete()
                    return Opened.Failed(LecternOpenFailure.UNREADABLE)
                }
            return when (val parsed = parse(descriptor)) {
                is Parsed.Ok -> ready(descriptor, parsed.renderer, copy, executor, sha)
                is Parsed.Failed -> {
                    runCatching { descriptor.close() }
                    copy.delete()
                    Opened.Failed(parsed.failure)
                }
                Parsed.NotSeekable -> {
                    runCatching { descriptor.close() }
                    copy.delete()
                    Opened.Failed(LecternOpenFailure.DAMAGED)
                }
            }
        }

        private sealed interface Parsed {
            data class Ok(val renderer: PdfRenderer) : Parsed
            data class Failed(val failure: LecternOpenFailure) : Parsed
            data object NotSeekable : Parsed
        }

        private fun parse(descriptor: ParcelFileDescriptor): Parsed = try {
            Parsed.Ok(PdfRenderer(descriptor))
        } catch (_: IllegalArgumentException) {
            // PdfRenderer's own words for a descriptor it cannot seek.
            Parsed.NotSeekable
        } catch (failure: Exception) {
            Log.w(TAG, "PdfRenderer refused the file", failure)
            Parsed.Failed(LecternOpenFailure.whileParsing(failure))
        }

        private fun ready(
            descriptor: ParcelFileDescriptor,
            renderer: PdfRenderer,
            copy: File?,
            executor: ExecutorService,
            sha: String,
        ): Opened {
            val pages = renderer.pageCount
            if (pages <= 0) {
                runCatching { renderer.close() }
                runCatching { descriptor.close() }
                copy?.delete()
                return Opened.Failed(LecternOpenFailure.DAMAGED)
            }
            return Opened.Ready(LecternDocument(descriptor, renderer, copy, executor, sha, pages))
        }
    }
}
