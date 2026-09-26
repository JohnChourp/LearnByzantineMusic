package com.johnchourp.learnbyzantinemusic.lectern

import com.johnchourp.learnbyzantinemusic.modes.IsonDrone

/**
 * The reader's side of «Συνέχισε στο παρασκήνιο» (ClickUp `869f5x2e7`, over J5's `869f5x2dq`): with it
 * on, `IsonPlaybackService` plays the lectern's ison, exactly as it plays the 8 Ήχοι page's, and the
 * reader only sends it requests. This decides the two things that can go wrong in between — pure, so
 * the ordering can be tested without the service.
 *
 * **What to send ([send]).** Only a request the service is not already going to play: what it last
 * published, or — while requests are still on their way — the last one sent. A page turned back and
 * forth sends nothing twice.
 *
 * **What a publication means ([onPublished]).** The service publishes everything it plays, the
 * reader's own requests included, a moment later. So each request sent waits in a queue until it comes
 * back; a publication found there is an echo and changes nothing — else a stop echoed after a quick
 * «off, on» would switch the ison off again. While a request of the reader's is still on its way,
 * anything else is overtaken by it, and ignored too. Only then does a publication come from outside:
 * [Heard.Stopped] — «Στάση» in the notification, the hour, the headphones, the option turned off — or
 * [Heard.Moved], φθόγγος −/+ in the notification, or an ison the 8 Ήχοι page left playing. The reader
 * sounds a move until it turns to a page with a setting of its own ([afterPageTurn]), and never writes
 * it into the page's setting: a φθόγγος picked in the shade is not a choice made for that page.
 */
class LecternBackgroundIson {

    /** What the service did, as far as the reader is concerned. */
    sealed interface Heard {
        /** Nothing to change: the reader's own request coming back, or nothing new. */
        data object Nothing : Heard

        /** Stopped from outside. */
        data object Stopped : Heard

        /** Playing [request], put there from outside. */
        data class Moved(val request: IsonDrone.Request) : Heard
    }

    /** Sent and not yet seen published, oldest first. */
    private val pending = ArrayDeque<IsonDrone.Request?>()

    /** The last thing the service was seen playing; null when it plays nothing. */
    private var published: IsonDrone.Request? = null

    /** What the service will play once everything sent has arrived. */
    private val expected: IsonDrone.Request?
        get() = if (pending.isEmpty()) published else pending.last()

    /**
     * Starts following a service found playing [playingNow] (null: silent). A service already playing
     * is [Heard.Moved]: the reader shows that ison, as the 8 Ήχοι page does when it opens over one.
     */
    fun start(playingNow: IsonDrone.Request?): Heard {
        pending.clear()
        published = playingNow
        return playingNow?.let { Heard.Moved(it) } ?: Heard.Nothing
    }

    /** True when [wanted] (null: silence) must be sent; it is then expected back. */
    fun send(wanted: IsonDrone.Request?): Boolean {
        if (wanted == expected) return false
        pending.addLast(wanted)
        // A request that never comes back — the service gone — must not hold the queue forever.
        while (pending.size > MAX_PENDING) pending.removeFirst()
        return true
    }

    fun onPublished(now: IsonDrone.Request?): Heard {
        val before = published
        published = now
        val echo = pending.lastIndexOf(now)
        if (echo >= 0) {
            repeat(echo + 1) { pending.removeFirst() }
            return Heard.Nothing
        }
        if (pending.isNotEmpty() || now == before) return Heard.Nothing
        return now?.let { Heard.Moved(it) } ?: Heard.Stopped
    }

    /** The service would not play what was sent and stepped aside: the reader plays it itself. */
    fun onRefused() {
        pending.clear()
        published = null
    }

    companion object {
        private const val MAX_PENDING = 16

        /**
         * What is left of [moved] — an ison put there from outside — once the reader is on a page where
         * [holding] holds: nothing when that page has a setting, which then sounds; the move itself on a
         * page with none, so paging through a PDF with no settings never silences an ison left playing.
         */
        fun afterPageTurn(moved: IsonDrone.Request?, holding: PageAssignment?): IsonDrone.Request? =
            moved.takeIf { holding == null }
    }
}
