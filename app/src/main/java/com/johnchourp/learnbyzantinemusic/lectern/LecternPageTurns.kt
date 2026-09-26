package com.johnchourp.learnbyzantinemusic.lectern

import android.view.KeyEvent

/** A page turn in the lectern's reader. */
enum class PageTurn { PREVIOUS, NEXT }

/**
 * Every way the reader turns a page, as plain rules (ClickUp `869f5x2e7`): a key, a tap, a swipe.
 *
 * **Keys and pedals.** A Bluetooth page-turner pedal is a keyboard: depending on its mode it sends the
 * arrow keys, Page Up/Down, the space bar or the volume keys. All of them are read ([forKey]) while the
 * reader is in front — forward is Page Down, → and ↓, space and volume down; back is Page Up, ← and ↑,
 * Shift+space and volume up. The reader takes such a key whole, down and up, so the system neither
 * changes the volume nor moves the focus with it; a held key turns one page, not a page per repeat
 * ([turnsOn]).
 *
 * **Taps.** The left third of the page goes back, the right third forward, and the middle shows or hides
 * the bars ([forTap]) — the page gets the whole screen while you chant.
 *
 * **Swipes.** Right to left is forward, as on paper ([forSwipe]); a drag shorter than the threshold turns
 * nothing, so scrolling a tall page cannot turn it by accident.
 */
object LecternPageTurns {

    /** Where a tap lands on the page. */
    enum class TapZone { PREVIOUS, BARS, NEXT }

    fun forKey(keyCode: Int, shiftPressed: Boolean): PageTurn? = when (keyCode) {
        KeyEvent.KEYCODE_PAGE_DOWN,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        -> PageTurn.NEXT

        KeyEvent.KEYCODE_PAGE_UP,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_VOLUME_UP,
        -> PageTurn.PREVIOUS

        KeyEvent.KEYCODE_SPACE -> if (shiftPressed) PageTurn.PREVIOUS else PageTurn.NEXT
        else -> null
    }

    /** The page turn one key event makes: only the first press — never the release, never a repeat. */
    fun turnsOn(keyCode: Int, shiftPressed: Boolean, isDown: Boolean, repeatCount: Int): PageTurn? =
        forKey(keyCode, shiftPressed)?.takeIf { isDown && repeatCount == 0 }

    fun forTap(x: Float, width: Float): TapZone = when {
        width <= 0f -> TapZone.BARS
        x < width / 3f -> TapZone.PREVIOUS
        x > width * 2f / 3f -> TapZone.NEXT
        else -> TapZone.BARS
    }

    /** [dragX] is the whole horizontal drag, positive to the right. */
    fun forSwipe(dragX: Float, thresholdPx: Float): PageTurn? = when {
        dragX <= -thresholdPx -> PageTurn.NEXT
        dragX >= thresholdPx -> PageTurn.PREVIOUS
        else -> null
    }

    /** [current] after [turn], kept inside a document of [pageCount] pages. */
    fun target(current: Int, turn: PageTurn, pageCount: Int): Int {
        if (pageCount <= 0) return 0
        val next = if (turn == PageTurn.NEXT) current + 1 else current - 1
        return next.coerceIn(0, pageCount - 1)
    }
}
