package com.johnchourp.learnbyzantinemusic.calendar

/**
 * How much of a month the offline εορτολόγιο dataset actually covers.
 *
 * Without this, the calendar cannot tell the user the difference between the two things an empty
 * panel might mean, and they look identical on screen (ClickUp `869f4tprf`):
 *
 * - **«Σήμερα δεν έχει γιορτή»** — the dataset covers this month and this day genuinely carries no
 *   special celebration;
 * - **«Δεν έχουμε ακόμη δεδομένα γι' αυτόν τον μήνα»** — nobody has filled it in yet.
 *
 * A third state exists and is the one a hand-written month list would get wrong: months outside the
 * filled range still carry the *immovable* feasts seeded across 1900–2100, so they are neither empty
 * nor complete. Promising a normal day there would be a lie — a movable feast in that month is simply
 * missing.
 *
 * Coverage is always computed from the dataset itself, never from a list of month names in the code:
 * a list would have to be edited every time a month is filled, and would be wrong the moment someone
 * forgot.
 */
enum class CalendarMonthCoverage {
    /** Every day of the month has an entry. An empty day here really is an ordinary day. */
    COMPLETE,

    /** Some days have entries — in practice the seeded immovable feasts. Absence proves nothing. */
    PARTIAL,

    /** No day of this month appears in the dataset at all. */
    NONE,
}
