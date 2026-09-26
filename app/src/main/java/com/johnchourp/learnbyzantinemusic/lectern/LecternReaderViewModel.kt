package com.johnchourp.learnbyzantinemusic.lectern

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.johnchourp.learnbyzantinemusic.modes.ApichimaSequence
import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.modes.IsonPlaybackService
import com.johnchourp.learnbyzantinemusic.modes.PhthongTonePlayer
import com.johnchourp.learnbyzantinemusic.modes.ToneTimbre
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Phthong
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

/**
 * The digital lectern's reader, kept across a rotation (ClickUp `869f5x2e7`): the open PDF, the page,
 * that PDF's page → ήχος map, the night tint, and the ison bar's sound.
 *
 * **Why a ViewModel.** The reader is the one screen that turns to landscape. A rotation re-creates the
 * activity, and everything here would otherwise be lost with it — the PDF read and hashed again, and
 * the ison cut and restarted. Here it all outlives the rotation: the screen calls [onScreenStopped]
 * with `isChangingConfigurations`, and a rotation silences nothing.
 *
 * **The ison.** The bar sounds [State.sounding]: the setting that holds on the page
 * ([PageAssignments.resolve]), or a move of the background ison made from outside. Every pitch comes
 * from [LecternIson] — the 8 Ήχοι page's own lookup. Who plays it follows J5's decision exactly
 * (`IsonPlaybackService`): with «Συνέχισε στο παρασκήνιο» off, this reader plays it and silences it in
 * onStop — leaving the reader stops it; with it on, the service plays it from the first moment and the
 * reader only sends requests ([LecternBackgroundIson] keeps the two in step), so it plays on after the
 * reader, with the service's notification. If the service is refused, the reader plays it itself, as
 * the 8 Ήχοι page does. The service is only ever asked from a visible reader.
 *
 * **What is saved.** Each edit writes the PDF's map at once ([LecternPrefs.savePages]); the page the
 * reader is on goes to the library when the screen stops, so the PDF reopens there.
 */
class LecternReaderViewModel(application: Application) : AndroidViewModel(application) {

    enum class Phase { OPENING, READY, FAILED }

    data class State(
        val phase: Phase = Phase.OPENING,
        val failure: LecternOpenFailure? = null,
        val pageCount: Int = 0,
        val pageIndex: Int = 0,
        val assignments: List<PageAssignment> = emptyList(),
        /** The map was written by a newer app: nothing is shown for it, and nothing is saved over it. */
        val mapReadOnly: Boolean = false,
        val night: Boolean = false,
        val isonOn: Boolean = false,
        /**
         * The background ison as it was put from outside — notification −/+, or left playing by the 8 Ήχοι
         * page: sounded until a page with a setting of its own ([LecternBackgroundIson.afterPageTurn]).
         */
        val moved: IsonDrone.Request? = null,
        /** The απήχημα syllable sounding, or -1. */
        val apichimaStep: Int = -1,
    ) {
        /** The setting that holds on this page: its own, or the latest one before it. */
        val holding: PageAssignment? get() = PageAssignments.resolve(assignments, pageIndex)

        val setOnThisPage: Boolean get() = PageAssignments.isSetOn(assignments, pageIndex)

        /** What the ison sounds while it is on. */
        val sounding: IsonDrone.Request? get() = moved ?: holding?.request
    }

    private val prefs = LecternPrefs(application)
    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State> = mutableState.asStateFlow()

    private var uri: Uri? = null
    private var document: LecternDocument? = null
    private var nightChosen = false

    /** Rendered pages by (page, width): the one on screen and its neighbours. */
    private val pages = object : LinkedHashMap<Pair<Int, Int>, Bitmap>(CACHED_PAGES + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<Int, Int>, Bitmap>?): Boolean =
            size > CACHED_PAGES
    }
    private val rendering = HashMap<Pair<Int, Int>, Deferred<Bitmap?>>()

    // ---- sound -----------------------------------------------------------------------------------

    /** The ison's own player, apart from the απήχημα's, so a phrase never cuts the drone. */
    private val drone = PhthongTonePlayer()
    private val phrase = PhthongTonePlayer()
    private var droneHz: Double? = null
    private var apichimaJob: Job? = null
    private var timbre = ToneTimbre.CLEAN
    private var started = false
    private var inBackground = false
    private var following = false
    private val background = LecternBackgroundIson()

    /** A request the service refused, which this reader plays itself until the next change. */
    private var playingInstead: IsonDrone.Request? = null

    init {
        viewModelScope.launch { IsonPlaybackService.playing.collect(::onBackgroundPublished) }
        viewModelScope.launch { IsonPlaybackService.refused.collect(::onBackgroundRefused) }
    }

    // ---- the document ----------------------------------------------------------------------------

    /** Opens [uri] once; a re-created screen finds it open. */
    fun open(uri: Uri) {
        if (this.uri != null) return
        this.uri = uri
        val resumeAt = prefs.library().entry(uri.toString())?.lastPageIndex ?: 0
        viewModelScope.launch {
            // Owned here until the state takes it, so a reader left while opening does not leak the file.
            val unclaimed = AtomicReference<LecternDocument?>()
            try {
                val opened = withContext(Dispatchers.IO) {
                    LecternDocument.open(getApplication(), uri).also { unclaimed.set((it as? LecternDocument.Opened.Ready)?.document) }
                }
                when (opened) {
                    is LecternDocument.Opened.Failed -> mutableState.update { it.copy(phase = Phase.FAILED, failure = opened.failure) }
                    is LecternDocument.Opened.Ready -> {
                        val ready = opened.document
                        val map = withContext(Dispatchers.IO) { prefs.pages(ready.sha256Hex) }
                        document = ready
                        unclaimed.set(null)
                        mutableState.update {
                            it.copy(
                                phase = Phase.READY,
                                pageCount = ready.pageCount,
                                pageIndex = resumeAt.coerceIn(0, ready.pageCount - 1),
                                assignments = (map as? LecternPagesCodec.Decoded.Assignments)?.assignments.orEmpty(),
                                mapReadOnly = map is LecternPagesCodec.Decoded.Newer,
                            )
                        }
                        if (inBackground) startFollowing()
                        applyIson()
                    }
                }
            } finally {
                unclaimed.getAndSet(null)?.close()
            }
        }
    }

    /** The page drawn [widthPx] wide; rendered once, then kept with its neighbours. */
    suspend fun page(pageIndex: Int, widthPx: Int): Bitmap? {
        val ready = document ?: return null
        val key = pageIndex to widthPx
        pages[key]?.let { return it }
        val job = rendering.getOrPut(key) {
            viewModelScope.async {
                ready.render(pageIndex, widthPx).also { bitmap ->
                    rendering.remove(key)
                    if (bitmap != null) pages[key] = bitmap
                }
            }
        }
        return job.await()
    }

    /** Draws [pageIndex] ahead, so turning to it shows it at once. */
    fun prefetch(pageIndex: Int, widthPx: Int) {
        if (pageIndex !in 0 until mutableState.value.pageCount) return
        viewModelScope.launch { page(pageIndex, widthPx) }
    }

    // ---- pages -----------------------------------------------------------------------------------

    fun turn(turn: PageTurn) {
        val now = mutableState.value
        goTo(LecternPageTurns.target(now.pageIndex, turn, now.pageCount))
    }

    fun goTo(pageIndex: Int) {
        val now = mutableState.value
        if (now.phase != Phase.READY) return
        val target = pageIndex.coerceIn(0, now.pageCount - 1)
        if (target == now.pageIndex) return
        stopApichima()
        // A page with a setting of its own sounds it; one without keeps an ison put there from outside.
        mutableState.update {
            it.copy(
                pageIndex = target,
                moved = LecternBackgroundIson.afterPageTurn(it.moved, PageAssignments.resolve(it.assignments, target)),
            )
        }
        applyIson()
    }

    /** The night tint starts as the app's theme — dark theme, dark page — and is then the user's. */
    fun defaultNight(dark: Boolean) {
        if (nightChosen) return
        nightChosen = true
        mutableState.update { it.copy(night = dark) }
    }

    fun setNight(on: Boolean) {
        nightChosen = true
        mutableState.update { it.copy(night = on) }
    }

    // ---- the page → ήχος map ---------------------------------------------------------------------

    fun chooseMode(mode: Mode) = edit { assignments, page ->
        PageAssignments.chooseMode(assignments, page, mode, prefs.eightModesShift(mode))
    }

    fun chooseShift(shiftMoria: Int) = edit { assignments, page -> PageAssignments.chooseShift(assignments, page, shiftMoria) }

    fun chooseIson(choice: Phthong?) = edit { assignments, page -> PageAssignments.chooseIson(assignments, page, choice) }

    /** The page drops its own setting and follows the pages before it again. */
    fun clearPage() = edit { assignments, page -> PageAssignments.clear(assignments, page) }

    /** Back to the page's own ison after a move made from the notification. */
    fun usePageSetting() {
        if (mutableState.value.moved == null) return
        mutableState.update { it.copy(moved = null) }
        applyIson()
    }

    private fun edit(change: (List<PageAssignment>, Int) -> List<PageAssignment>) {
        val now = mutableState.value
        val sha = document?.sha256Hex ?: return
        if (now.phase != Phase.READY || now.mapReadOnly) return
        val next = change(now.assignments, now.pageIndex)
        if (next == now.assignments && now.moved == null) return
        // A new ήχος must not go on with the old one's απήχημα.
        stopApichima()
        mutableState.update { it.copy(assignments = next, moved = null) }
        prefs.savePages(sha, next)
        applyIson()
    }

    // ---- the ison --------------------------------------------------------------------------------

    fun setIson(on: Boolean) {
        val now = mutableState.value
        if (on && now.sounding == null) return
        mutableState.update { it.copy(isonOn = on, moved = if (on) it.moved else null) }
        applyIson()
    }

    /** «Άκου το απήχημα» of the ήχος that sounds, at its shift; [steps] come from the 8 Ήχοι's strings. */
    fun playApichima(steps: List<ApichimaSequence.Step>) {
        val request = mutableState.value.sounding ?: return
        val tones = LecternIson.apichimaFrequencies(request, steps) ?: return
        apichimaJob?.cancel()
        apichimaJob = viewModelScope.launch {
            try {
                tones.forEachIndexed { index, hz ->
                    mutableState.update { it.copy(apichimaStep = index) }
                    phrase.start(hz, timbre)
                    delay(ApichimaSequence.Speed.SHORT.millisPerStep)
                }
            } finally {
                // On completion and on cancellation alike, so a stop mid-phrase leaves nothing sounding.
                mutableState.update { it.copy(apichimaStep = -1) }
                phrase.stop()
            }
        }
    }

    fun stopApichima() {
        apichimaJob?.cancel()
        apichimaJob = null
    }

    // ---- the screen's lifecycle ------------------------------------------------------------------

    /** onStart: the choices of the 8 Ήχοι page may have changed while the reader was hidden. */
    fun onScreenStarted() {
        started = true
        timbre = prefs.timbre()
        val wasInBackground = inBackground
        inBackground = prefs.isonInBackground()
        if (!inBackground) {
            following = false
        } else if (!wasInBackground || !following) {
            silenceLocal()
            if (mutableState.value.phase == Phase.READY) startFollowing()
        }
        applyIson()
    }

    /**
     * onStop. A rotation ([changingConfigurations]) silences nothing. Otherwise the reader's own sound
     * stops — leaving it stops the ison, unless the background service plays it — and the page is kept.
     */
    fun onScreenStopped(changingConfigurations: Boolean) {
        started = false
        if (changingConfigurations) return
        stopApichima()
        // Played here in the service's place: it stops with the screen, and the service is asked again on return.
        playingInstead = null
        silenceLocal()
        val at = uri?.toString() ?: return
        if (mutableState.value.phase == Phase.READY) {
            prefs.saveLibrary(prefs.library().withLastPage(at, mutableState.value.pageIndex))
        }
    }

    override fun onCleared() {
        apichimaJob?.cancel()
        drone.release()
        phrase.release()
        document?.close()
        document = null
        pages.clear()
    }

    /** Brings the sound in line with the state: the service or this reader, never both. */
    private fun applyIson() {
        val now = mutableState.value
        val wanted = if (now.isonOn && now.phase == Phase.READY) now.sounding else null
        if (!inBackground) {
            playLocally(if (started) wanted else null)
            return
        }
        // Played here after a refusal: until the ison changes. Then the service is asked again.
        if (playingInstead != null) {
            if (playingInstead == wanted) return
            playingInstead = null
            silenceLocal()
        }
        if (!started || !background.send(wanted)) return
        if (wanted == null) {
            IsonPlaybackService.stop(getApplication())
        } else if (!IsonPlaybackService.play(getApplication(), wanted, timbre)) {
            background.onRefused()
            playInstead(wanted)
        }
    }

    private fun startFollowing() {
        following = true
        onHeard(background.start(IsonPlaybackService.playing.value))
    }

    private fun onBackgroundPublished(now: IsonDrone.Request?) {
        if (!following) return
        onHeard(background.onPublished(now))
    }

    private fun onHeard(heard: LecternBackgroundIson.Heard) {
        when (heard) {
            LecternBackgroundIson.Heard.Nothing -> Unit
            LecternBackgroundIson.Heard.Stopped -> mutableState.update { it.copy(isonOn = false, moved = null) }
            is LecternBackgroundIson.Heard.Moved -> mutableState.update {
                it.copy(isonOn = true, moved = heard.request.takeIf { request -> request != it.holding?.request })
            }
        }
    }

    private fun onBackgroundRefused(refused: IsonDrone.Request) {
        if (!following) return
        background.onRefused()
        // Only a visible reader sounds anything: a refusal met after leaving plays nowhere.
        if (started && refused == mutableState.value.sounding && mutableState.value.isonOn) playInstead(refused)
    }

    private fun playInstead(request: IsonDrone.Request) {
        playingInstead = request
        playLocally(request)
    }

    private fun silenceLocal() = playLocally(null)

    /**
     * This reader's own drone, at the one lookup's pitch. A sounding drone is moved with a glide inside
     * the same stream — another page, another φθόγγος — so a page turn neither clicks nor gaps.
     */
    private fun playLocally(request: IsonDrone.Request?) {
        val hz = request?.let { LecternIson.held(it)?.frequencyHz }
        if (hz == droneHz) return
        droneHz = hz
        when {
            hz == null -> drone.stop()
            drone.retune(hz) -> Unit
            else -> drone.start(hz, timbre)
        }
    }

    private companion object {
        /** The page on screen, the next and the previous: at most ~48 MB at the render cap. */
        const val CACHED_PAGES = 3
    }
}
