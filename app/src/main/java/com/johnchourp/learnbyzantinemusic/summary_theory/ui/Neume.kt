package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.music.TimeSign

/**
 * The one table of the signs the theory pages draw — «Ανιόντες», «Κατιόντες», «Συνθέσεις
 * ανάβασης», «Ποιότητος» and «Χαρακτήρες Χρόνου» (ClickUp `869f5x29j`, H4).
 *
 * Before H4 a sign lived in four enums (`Neume`, `DescentNeume`, `AscentCharacter`,
 * `DescentCharacter`) and in six per-page maps of drawable, name, TalkBack text and size, under two
 * naming systems: `DIGITAL`, `ALL_RIGHT`, `INTERCOM` and `VACCUM` translated the Greek name, while
 * `PETASTI`, `YPSILI` and `CHAMILI` transliterated it. The copies had drifted: «Γοργό» on the Time
 * page and «Γοργόν» on the next one, «Ενδόφωνο» without its ν, and in English the κέντημα and the
 * κεντήματα were both «Embroidery». Every page now reads a sign's glyph, name, TalkBack text, size,
 * φωνές and time value from here; `AscentCharacter` and `DescentCharacter` are views of it, and
 * `OneNeumeTableTest` / `OneNamePerSignTest` keep it the only copy.
 *
 * ## Columns
 *
 * | Column | Holds | Rule |
 * |---|---|---|
 * | constant | the code name | the transliteration the English page shows: `PSIFISTON` is «Ψηφιστόν» / Psifiston |
 * | [drawable] | the glyph | every image belongs to exactly one sign |
 * | [nameRes] | the name, in every language | one string key per sign, nowhere repeated. Greek in -ον (Γοργόν, Δίγοργον, Ενδόφωνον); English transliterated (Gorgon, Kentima, Kentimata, Klasma). 0 for the variants and composites no page names on their own |
 * | [kind] | ποσότητος, χρόνου or ποιότητος | picks the TalkBack text, which always says the name: «Νεύμα: Γοργόν», «Σημάδι ποιότητος: Βαρεία» |
 * | [width] × [height] | the natural size, dp | the size of the sign's ImageView style in the original layouts; a diagram may draw it at its own size |
 * | [voices] | φωνές | quantity signs only: + up, − down |
 * | [timeSign] | what it does to time | time signs only: the [TimeSign] the time rules apply — never a second copy of a duration |
 *
 * The rests (βαρεία with κουκίδες) have no [timeSign] yet: the time rules do not model silence.
 *
 * **Nothing stores a constant name.** Checked on 2026-09-26: no preference, file or database holds
 * a neume's name — the only stored ids nearby are the learning path's page ids and the 8 Ήχοι
 * topic keys — so the constants could all be renamed to one convention.
 */
enum class Neume(
    @DrawableRes val drawable: Int,
    @StringRes val nameRes: Int,
    val kind: NeumeKind,
    val width: Int,
    val height: Int,
    val voices: Int? = null,
    val timeSign: TimeSign? = null,
) {
    // Ποσότητος — ανιόντες.
    ISON(R.drawable.ison, R.string.ison, NeumeKind.QUANTITY, 62, 18, voices = 0),
    OLIGON(R.drawable.oligon, R.string.oligon, NeumeKind.QUANTITY, 70, 18, voices = 1),
    PETASTI(R.drawable.flyer, R.string.flyer, NeumeKind.QUANTITY, 62, 23, voices = 1),
    KENTIMATA(R.drawable.embroideries, R.string.embroideries, NeumeKind.QUANTITY, 30, 18, voices = 1),
    KENTIMA(R.drawable.embroidery, R.string.embroidery, NeumeKind.QUANTITY, 15, 18, voices = 2),
    YPSILI(R.drawable.high, R.string.high, NeumeKind.QUANTITY, 42, 36, voices = 4),

    // Ποσότητος — κατιόντες.
    APOSTROPHOS(R.drawable.apostrophe, R.string.apostrophe, NeumeKind.QUANTITY, 30, 18, voices = -1),
    ELAFRON(R.drawable.slight, R.string.slight, NeumeKind.QUANTITY, 52, 18, voices = -2),

    /** Ελαφρόν with απόστροφος: −3, drawn only inside the leaping descents. */
    ELAFRON_APOSTROPHOS(R.drawable.slight_apostrophe, 0, NeumeKind.QUANTITY, 42, 22, voices = -3),
    YPORROI(R.drawable.underflow, R.string.underflow, NeumeKind.QUANTITY, 19, 18, voices = -2),
    CHAMILI(R.drawable.low, R.string.low, NeumeKind.QUANTITY, 58, 36, voices = -4),

    // Χρόνου — they lengthen a note, divide a χρόνο, or rest.
    KLASMA(R.drawable.fraction, R.string.fraction, NeumeKind.TIME, 48, 18, timeSign = TimeSign.KLASMA),
    APLI(R.drawable.simple_dot, R.string.dot_simple, NeumeKind.TIME, 22, 18, timeSign = TimeSign.APLI),
    DIPLI(R.drawable.double_dots, R.string.dot_double, NeumeKind.TIME, 56, 18, timeSign = TimeSign.DIPLI),
    TRIPLI(R.drawable.triple_dots, R.string.dot_triple, NeumeKind.TIME, 70, 18, timeSign = TimeSign.TRIPLI),
    GORGON(R.drawable.gorgo, R.string.gorgo, NeumeKind.TIME, 18, 9, timeSign = TimeSign.GORGON),

    /** Παρεστιγμένο γοργόν, στιγμή αριστερά. Its card is titled with the family name. */
    GORGON_DOT_LEFT(R.drawable.presented_gorgo, 0, NeumeKind.TIME, 21, 10, timeSign = TimeSign.GORGON_DOT_LEFT),

    /** Παρεστιγμένο γοργόν, στιγμή δεξιά. */
    GORGON_DOT_RIGHT(R.drawable.gorgo_presented, 0, NeumeKind.TIME, 21, 10, timeSign = TimeSign.GORGON_DOT_RIGHT),
    DIGORGON(R.drawable.digorgo, R.string.digorgo, NeumeKind.TIME, 22, 15, timeSign = TimeSign.DIGORGON),

    /** Παρεστιγμένο δίγοργον, στιγμή κάτω. Its card is titled with the family name. */
    DIGORGON_DOT_BOTTOM(
        R.drawable.presented_bottom_digorgo, 0, NeumeKind.TIME, 35, 18, timeSign = TimeSign.DIGORGON_DOT_BOTTOM,
    ),

    /** Παρεστιγμένο δίγοργον, στιγμή στη μέση. */
    DIGORGON_DOT_MIDDLE(
        R.drawable.presented_middle_digorgo, 0, NeumeKind.TIME, 27, 18, timeSign = TimeSign.DIGORGON_DOT_MIDDLE,
    ),

    /** Παρεστιγμένο δίγοργον, στιγμή πάνω. */
    DIGORGON_DOT_TOP(R.drawable.presented_top_digorgo, 0, NeumeKind.TIME, 35, 18, timeSign = TimeSign.DIGORGON_DOT_TOP),
    TRIGORGON(R.drawable.trigorgo, R.string.trigorgo, NeumeKind.TIME, 36, 18, timeSign = TimeSign.TRIGORGON),
    ARGON(R.drawable.argo, R.string.argo, NeumeKind.TIME, 13, 10, timeSign = TimeSign.ARGON),
    DIARGON(R.drawable.diargo, R.string.diargo, NeumeKind.TIME, 17, 18, timeSign = TimeSign.DIARGON),
    TRIARGON(R.drawable.triargo, R.string.triargo, NeumeKind.TIME, 27, 18, timeSign = TimeSign.TRIARGON),

    /** Βαρεία with απλή: a rest of one χρόνο. The page names the rests by what they do. */
    VAREIA_APLI(R.drawable.heavy_simple_dot, 0, NeumeKind.TIME, 36, 54),

    /** Βαρεία with διπλή: a rest of two χρόνοι. */
    VAREIA_DIPLI(R.drawable.heavy_double_dots, 0, NeumeKind.TIME, 50, 54),

    /** Βαρεία with τριπλή: a rest of three χρόνοι. */
    VAREIA_TRIPLI(R.drawable.heavy_triple_dots, 0, NeumeKind.TIME, 66, 54),

    // Ποιότητος — they change how a note is sung.
    VAREIA(R.drawable.heavy, R.string.heavy, NeumeKind.QUALITY, 36, 54),
    YFEN(R.drawable.yfen, R.string.yfen, NeumeKind.QUALITY, 62, 18),
    SYNECHES_ELAFRON(R.drawable.slight_continuous, R.string.continuous_slight, NeumeKind.QUALITY, 70, 18),
    PSIFISTON(R.drawable.digital, R.string.digital, NeumeKind.QUALITY, 70, 18),
    OMALON(R.drawable.all_right, R.string.all_right, NeumeKind.QUALITY, 70, 18),
    ANTIKENOMA(R.drawable.vaccum, R.string.vaccum, NeumeKind.QUALITY, 70, 18),

    /** Αντικένωμα with απλή, drawn only inside an example of the αντικένωμα. */
    ANTIKENOMA_APLI(R.drawable.vaccum_simple, 0, NeumeKind.QUALITY, 70, 18),
    SYNDESMOS(R.drawable.link, R.string.link, NeumeKind.QUALITY, 65, 18),

    /** The ενδόφωνον came after the original layouts; it takes the 70 × 18 of the signs written under a note. */
    ENDOFONON(R.drawable.intercom, R.string.intercom, NeumeKind.QUALITY, 70, 18),
}

/** The three families of signs, which is also what TalkBack calls them. */
enum class NeumeKind(@StringRes val descriptionRes: Int) {
    /** Ποσότητος — ανιόντες and κατιόντες: they move the voice. «Νεύμα: …». */
    QUANTITY(R.string.cd_neume),

    /** Χρόνου: they lengthen a note, divide a χρόνο, or rest. «Νεύμα: …». */
    TIME(R.string.cd_neume),

    /** Ποιότητος: they change how a note is sung. «Σημάδι ποιότητος: …». */
    QUALITY(R.string.cd_quality_sign),
}
