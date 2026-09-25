package com.johnchourp.learnbyzantinemusic.recordings.session

import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
import com.johnchourp.learnbyzantinemusic.recordings.RecordingStateUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.fail
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Fakes for the recording-session tests (ClickUp `869f5x26x`): a microphone the test feeds by hand,
 * a transcoder that can fail or wait, and a "user's folder" that is a real directory, so every
 * assertion is about bytes on disk. Shared by the per-topic test files; holds no tests itself.
 */
object SessionTestKit {
    /** Audio bytes with a recognisable pattern, so a copy that drops or reorders bytes shows. */
    fun pcm(size: Int, seed: Int = 7): ByteArray = ByteArray(size) { ((it * 31 + seed) % 251).toByte() }

    /** A capture file as the session leaves it: 44 zero bytes, then the audio. */
    fun captureFile(file: File, pcm: ByteArray): File {
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(WavFormat.HEADER_SIZE) + pcm)
        return file
    }

    /** What a finished WAV of [pcm] must be byte for byte. */
    fun finishedWav(scratch: File, pcm: ByteArray): ByteArray {
        captureFile(scratch, pcm)
        WavFormat.finalizeHeader(scratch)
        return scratch.readBytes().also { scratch.delete() }
    }

    fun waitUntil(what: String, timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) fail("timed out waiting for: $what")
            Thread.sleep(5)
        }
    }

    fun awaitPhase(session: RecordingSession, phase: RecordingStateUi) =
        waitUntil("phase $phase (now ${session.state.value.phase})") { session.state.value.phase == phase }

    fun ioScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

/** A microphone the test feeds: every [feed] is one read's worth of PCM. */
class FakePcmSource : PcmSource {
    override val bufferSize: Int = 4096
    private val chunks = LinkedBlockingQueue<ByteArray>()

    @Volatile
    var stopped = false
        private set

    @Volatile
    var released = false
        private set

    fun feed(bytes: ByteArray) {
        bytes.toList().chunked(bufferSize).forEach { chunks.put(it.toByteArray()) }
    }

    override fun read(buffer: ByteArray): Int {
        if (stopped) return -1
        val chunk = chunks.poll(10, TimeUnit.MILLISECONDS) ?: return 0
        chunk.copyInto(buffer)
        return chunk.size
    }

    override fun stop() {
        stopped = true
    }

    override fun release() {
        released = true
    }
}

/** Succeeds or fails on demand; can hold the save inside the transcode until [release]. */
class FakeTranscoder(
    private val log: MutableList<String> = Collections.synchronizedList(mutableListOf()),
    var succeed: Boolean = true,
    var writeOutput: Boolean = true,
    holdUntilReleased: Boolean = false,
) : Transcoder {
    private val gate = CountDownLatch(if (holdUntilReleased) 1 else 0)
    val entered = CountDownLatch(1)
    val calls = Collections.synchronizedList(mutableListOf<RecordingFormatOption>())

    fun release() = gate.countDown()

    override fun transcode(sourceWav: File, output: File, format: RecordingFormatOption): Boolean {
        log += "transcode:${format.name}"
        calls += format
        entered.countDown()
        gate.await(10, TimeUnit.SECONDS)
        if (writeOutput) output.writeBytes(encoded(sourceWav.readBytes(), format))
        return succeed
    }

    companion object {
        /** What the fake "encoder" writes: a marker, then the source reversed — never equal to the WAV. */
        fun encoded(wavBytes: ByteArray, format: RecordingFormatOption): ByteArray =
            "ENC:${format.name}:".toByteArray() + wavBytes.reversedArray()
    }
}

/**
 * The user's folder, backed by a real directory. Logs every step with whether [watch] (the capture
 * WAV) still existed at that moment, so the order of "create, copy, verify, delete" is observable.
 */
class FakeSink(
    val dir: File,
    private val log: MutableList<String> = Collections.synchronizedList(mutableListOf()),
    var watch: File? = null,
) : RecordingSink {
    var refuseCreate = false

    /** Throw after this many bytes of a copy — a write cut short. */
    var failAfterBytes: Long? = null

    /** What the folder claims the file's size is; default: the truth. */
    var reportSize: (actual: Long) -> Long? = { it }
    var sizeQueryThrows = false

    val created = Collections.synchronizedList(mutableListOf<String>())
    val registered = Collections.synchronizedList(mutableListOf<String>())

    private fun wavState() = watch?.let { if (it.exists()) " wav=present" else " wav=gone" }.orEmpty()

    override fun createFile(displayName: String, mimeType: String): SinkFile? {
        log += "create:$displayName${wavState()}"
        if (refuseCreate) return null
        dir.mkdirs()
        // Like a SAF provider: a taken name gets " (1)", " (2)" … before the extension.
        var file = File(dir, displayName)
        var copy = 1
        while (!file.createNewFile()) {
            file = File(dir, "${displayName.substringBeforeLast('.')} ($copy).${displayName.substringAfterLast('.')}")
            copy++
        }
        created += file.name
        return FakeSinkFile(file)
    }

    override suspend fun register(file: SinkFile) {
        log += "register:${file.name}"
        registered += file.name
    }

    inner class FakeSinkFile(val file: File) : SinkFile {
        override val name: String get() = file.name

        override fun openOutputStream(): OutputStream {
            log += "write:${file.name}${wavState()}"
            val limit = failAfterBytes
            val real = FileOutputStream(file)
            if (limit == null) return real
            return object : OutputStream() {
                var written = 0L
                override fun write(b: Int) {
                    if (written >= limit) throw IOException("disk full (fake)")
                    real.write(b)
                    written++
                }
                override fun write(b: ByteArray, off: Int, len: Int) {
                    for (i in off until off + len) write(b[i].toInt())
                }
                override fun close() = real.close()
            }
        }

        override fun reportedSize(): Long? {
            log += "verify:${file.name}${wavState()}"
            if (sizeQueryThrows) throw IOException("provider gone (fake)")
            return reportSize(file.length())
        }

        override fun delete(): Boolean {
            log += "delete-target:${file.name}"
            return file.delete()
        }
    }
}

/** One session over temporary folders, with fakes for everything the device provides. */
class SessionRig(
    root: File,
    format: RecordingFormatOption = RecordingFormatOption.FLAC,
    holdTranscode: Boolean = false,
) {
    val log: MutableList<String> = Collections.synchronizedList(mutableListOf())
    val captureDir = File(root, "recordings_capture")
    val pendingDir = File(root, "recordings_pending")
    val workDir = File(root, "cache").apply { mkdirs() }
    val folder = FakeSink(File(root, "folder"), log)
    val transcoder = FakeTranscoder(log, holdUntilReleased = holdTranscode)

    /** The microphone of the latest recording: like `AudioRecord`, every start opens a new one. */
    @Volatile
    var microphone = FakePcmSource()
        private set
    val events: MutableList<RecordingEvent> = Collections.synchronizedList(mutableListOf())
    val clock = AtomicLong(1_000_000L)
    val pending = PendingRecordings(pendingDir)

    /** Null = the folder is gone (permission lost); a function, so a test can change it between saves. */
    @Volatile
    var sink: () -> RecordingSink? = { folder }

    @Volatile
    var microphoneFails = false

    @Volatile
    var selectedFormat: RecordingFormatOption = format

    val session = RecordingSession(
        scope = SessionTestKit.ioScope(),
        captureDir = captureDir,
        pending = pending,
        saver = RecordingSaver(transcoder, pending, workDir),
        openMicrophone = {
            if (microphoneFails) throw IllegalStateException("audio_record_not_initialized (fake)")
            FakePcmSource().also { microphone = it }
        },
        openSink = { sink() },
        selectedFormat = { selectedFormat },
        elapsedClock = { clock.get() },
        wallClock = { WALL_CLOCK },
        onEvent = { events += it },
    )

    fun captureFiles(): List<File> = captureDir.listFiles { f -> f.extension == "wav" }.orEmpty().toList()

    /** The one capture file of the recording in progress. */
    fun liveCapture(): File = captureFiles().single()

    fun record(bytes: ByteArray) {
        val before = liveCapture().length()
        microphone.feed(bytes)
        SessionTestKit.waitUntil("${bytes.size} bytes captured") { liveCapture().length() == before + bytes.size }
    }

    /** Ends a capture a failed test left running, so no capture thread outlives the test. */
    fun close() {
        if (session.state.value.phase == RecordingStateUi.RECORDING || session.state.value.phase == RecordingStateUi.PAUSED) {
            session.discard()
        }
        transcoder.release()
    }

    companion object {
        /** A fixed instant; expected names come from [RecordingFiles.baseNameFor], so the zone does not matter. */
        const val WALL_CLOCK = 1_790_000_000_000L
        val baseName: String get() = RecordingFiles.baseNameFor(WALL_CLOCK)
    }
}
