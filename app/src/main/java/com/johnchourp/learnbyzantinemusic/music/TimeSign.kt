package com.johnchourp.learnbyzantinemusic.music

/**
 * The time signs (χαρακτήρες χρόνου) a note can carry — typed, where they used to be free strings
 * («gorgo», «fraction», «antikeno», «apli») that a typo would have turned into a silent no-op
 * (ClickUp `869f5x29r`, H5).
 *
 * What a sign does is data here, applied by [ByzantineRhythmMapper], whose KDoc is the written
 * table of the rules:
 * - an **additive** sign lengthens the note it is written on by [addsBeats] χρόνοι;
 * - a **divider** makes a group of neighbouring notes share **one** χρόνο, in the proportions of
 *   [shares], in sung order. The group starts [firstShareOffset] notes away from the sign's own
 *   note: −1 is the previous note, −2 the one before it.
 *
 * The αργόν family is both: the two notes before it share a χρόνο, and its own note is lengthened.
 *
 * [id] is the name a sign is stored under. `gorgo` and `fraction` are the names the Melody Trainer
 * has always used for γοργόν and κλάσμα; they stay, so a melody saved with them (F6) reads back.
 * An id, once written anywhere, never changes — the constant names may, the ids may not.
 */
enum class TimeSign(
    val id: String,
    val addsBeats: Int = 0,
    val shares: List<Beats> = emptyList(),
    val firstShareOffset: Int = 0,
) {
    /** Κλάσμα: +1 χρόνος. */
    KLASMA("fraction", addsBeats = 1),

    /** Απλή (μία κουκίδα): +1 χρόνος. */
    APLI("apli", addsBeats = 1),

    /** Διπλή: +2 χρόνοι. */
    DIPLI("dipli", addsBeats = 2),

    /** Τριπλή: +3 χρόνοι. */
    TRIPLI("tripli", addsBeats = 3),

    /** Γοργόν: the previous note and this one share a χρόνο, ½ + ½. */
    GORGON("gorgo", shares = listOf(Beats.of(1, 2), Beats.of(1, 2)), firstShareOffset = -1),

    /** Παρεστιγμένο γοργόν, στιγμή αριστερά: the dot's side, the previous note, gets ¾. */
    GORGON_DOT_LEFT("gorgo_dot_left", shares = listOf(Beats.of(3, 4), Beats.of(1, 4)), firstShareOffset = -1),

    /** Παρεστιγμένο γοργόν, στιγμή δεξιά: this note gets ¾. */
    GORGON_DOT_RIGHT("gorgo_dot_right", shares = listOf(Beats.of(1, 4), Beats.of(3, 4)), firstShareOffset = -1),

    /** Δίγοργον: the previous note, this one and the next share a χρόνο, ⅓ each. */
    DIGORGON("digorgo", shares = listOf(Beats.of(1, 3), Beats.of(1, 3), Beats.of(1, 3)), firstShareOffset = -1),

    /** Παρεστιγμένο δίγοργον, στιγμή κάτω: the previous note ½, this one and the next ¼. */
    DIGORGON_DOT_BOTTOM(
        "digorgo_dot_bottom",
        shares = listOf(Beats.of(1, 2), Beats.of(1, 4), Beats.of(1, 4)),
        firstShareOffset = -1,
    ),

    /** Παρεστιγμένο δίγοργον, στιγμή στη μέση: this note ½, its two neighbours ¼. */
    DIGORGON_DOT_MIDDLE(
        "digorgo_dot_middle",
        shares = listOf(Beats.of(1, 4), Beats.of(1, 2), Beats.of(1, 4)),
        firstShareOffset = -1,
    ),

    /** Παρεστιγμένο δίγοργον, στιγμή πάνω: the next note ½, the previous one and this ¼. */
    DIGORGON_DOT_TOP(
        "digorgo_dot_top",
        shares = listOf(Beats.of(1, 4), Beats.of(1, 4), Beats.of(1, 2)),
        firstShareOffset = -1,
    ),

    /** Τρίγοργον: the previous note, this one and the next two share a χρόνο, ¼ each. */
    TRIGORGON(
        "trigorgo",
        shares = listOf(Beats.of(1, 4), Beats.of(1, 4), Beats.of(1, 4), Beats.of(1, 4)),
        firstShareOffset = -1,
    ),

    /** Αργόν: the two notes before it share a χρόνο, ½ + ½; its own note (the ολίγον) lasts 2. */
    ARGON("argo", addsBeats = 1, shares = listOf(Beats.of(1, 2), Beats.of(1, 2)), firstShareOffset = -2),

    /** Δίαργον: as αργόν, and its own note lasts 3. */
    DIARGON("diargo", addsBeats = 2, shares = listOf(Beats.of(1, 2), Beats.of(1, 2)), firstShareOffset = -2),

    /** Τρίαργον: as αργόν, and its own note lasts 4. */
    TRIARGON("triargo", addsBeats = 3, shares = listOf(Beats.of(1, 2), Beats.of(1, 2)), firstShareOffset = -2);

    /** True for the signs that share one χρόνο among a group of notes. */
    val isDivider: Boolean get() = shares.isNotEmpty()

    /** How many notes a divider needs before its own: 1 for the γοργόν family, 2 for the αργόν. */
    val notesBefore: Int get() = if (isDivider) -firstShareOffset else 0

    /** How many notes a divider needs after its own: 1 for the δίγοργον family, 2 for the τρίγοργον. */
    val notesAfter: Int get() = if (isDivider) maxOf(0, firstShareOffset + shares.size - 1) else 0

    companion object {
        /** The sign stored as [id], or null for a name the app does not know (or no longer knows). */
        fun fromId(id: String): TimeSign? = entries.firstOrNull { it.id == id }
    }
}
