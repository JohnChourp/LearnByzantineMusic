package com.johnchourp.learnbyzantinemusic.music

/**
 * The eight ήχοι, as a closed set (ClickUp `869f4tpxj`).
 *
 * ## Why an enum and not a map key
 *
 * The modes were addressed by string keys (`"first"`, `"plagal_second"`), and every lookup returned
 * a nullable that each call site handled differently — some `?: error(...)`, some falling back to
 * the first mode. A typo produced a runtime surprise rather than a compile error, and "is this
 * string a mode key, a topic key or a theory key?" was answerable only by reading.
 *
 * There are exactly eight and there will not be a ninth, so the set belongs in the type system.
 *
 * [key] remains the stored/lookup spelling, unchanged, because it appears in preferences
 * (`mode_base_shift_moria_<key>`) and in the existing catalog map. Renaming one would silently
 * reset a user's «Μεταφορά βάσης» for that mode.
 */
enum class Mode(val key: String) {
    FIRST("first"),
    SECOND("second"),
    THIRD("third"),
    FOURTH("fourth"),
    PLAGAL_FIRST("plagal_first"),
    PLAGAL_SECOND("plagal_second"),
    VARYS("varys"),
    PLAGAL_FOURTH("plagal_fourth");

    companion object {
        /** Boundary parser: a stored or catalog key → the mode, or null when it names none. */
        fun fromKey(key: String?): Mode? = entries.firstOrNull { it.key == key }
    }
}
