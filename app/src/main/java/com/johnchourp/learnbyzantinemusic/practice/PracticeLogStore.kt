package com.johnchourp.learnbyzantinemusic.practice

import android.content.Context
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import java.time.Clock
import java.time.Instant

/**
 * [PracticeLog] in the device's preferences — `AppPrefs.Store.PRACTICE`, key `practice_log`, as
 * [PracticeLogCodec] JSON (ClickUp `869f5x2dy`). Local only: no account, no server. The rules that
 * depend on the clock are [PracticeCalendar]'s; this class only reads and writes.
 *
 * Its own preferences file, apart from the learning path's progress: resetting «Από το μηδέν» must
 * never take the streak with it.
 */
class PracticeLogStore(context: Context, clock: Clock? = null) {
    private val prefs = AppPrefs.open(context.applicationContext, AppPrefs.Store.PRACTICE)
    private val calendar = PracticeCalendar(clock)

    fun read(): PracticeLog = PracticeLogCodec.decode(prefs.getString(AppPrefs.PracticeLogJson.name, null))

    fun now(): Instant = calendar.now()

    /** Records one completed session that started at [startedAt], and returns the new log. */
    fun recordCompletedSession(startedAt: Instant): PracticeLog {
        val updated = calendar.complete(read(), startedAt)
        prefs.edit().putString(AppPrefs.PracticeLogJson.name, PracticeLogCodec.encode(updated)).apply()
        return updated
    }

    fun summary(): PracticeSummary = calendar.summary(read())
}
