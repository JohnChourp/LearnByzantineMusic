package com.johnchourp.learnbyzantinemusic.anastasimatarion

import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ModeResources
import com.johnchourp.learnbyzantinemusic.music.Mode

/** Catalog keys → localized labels. Every key in the catalog must have an entry here (unit-tested). */
object AnastasimatarionLabels {
    /**
     * Mode keys in the order of the liturgical cycle, so index = LiturgicalToneCycle.toneIndex.
     * Derived from [Mode], whose entries ARE that order (ClickUp `869f5x299`) — the rule is no longer
     * kept true by hand, and a test walks the calendar to prove it.
     */
    val MODE_ORDER: List<String> = Mode.entries.map { it.key }

    val MODE_NAMES: Map<String, Int> = Mode.entries.associate { it.key to ModeResources.nameRes(it) }

    val SERVICE_NAMES: Map<String, Int> = mapOf(
        "vespers" to R.string.anastasimatarion_service_vespers,
        "orthros" to R.string.anastasimatarion_service_orthros,
        "liturgy" to R.string.anastasimatarion_service_liturgy,
    )

    val GROUP_NAMES: Map<String, Int> = mapOf(
        "kekragarion" to R.string.anastasimatarion_group_kekragarion,
        "stichera_anastasima" to R.string.anastasimatarion_group_stichera_anastasima,
        "anatolika" to R.string.anastasimatarion_group_anatolika,
        "dogmatikon" to R.string.anastasimatarion_group_dogmatikon,
        "aposticha" to R.string.anastasimatarion_group_aposticha,
        "apolytikion" to R.string.anastasimatarion_group_apolytikion,
        "kathismata" to R.string.anastasimatarion_group_kathismata,
        "ypakoe" to R.string.anastasimatarion_group_ypakoe,
        "anavathmoi" to R.string.anastasimatarion_group_anavathmoi,
        "prokeimenon" to R.string.anastasimatarion_group_prokeimenon,
        "heirmoi" to R.string.anastasimatarion_group_heirmoi,
        "kontakion" to R.string.anastasimatarion_group_kontakion,
        "ainoi_anastasima" to R.string.anastasimatarion_group_ainoi_anastasima,
        "ainoi_anatolika" to R.string.anastasimatarion_group_ainoi_anatolika,
        "makarismoi" to R.string.anastasimatarion_group_makarismoi,
    )

    @StringRes
    fun modeName(key: String): Int = MODE_NAMES[key] ?: R.string.anastasimatarion_title

    @StringRes
    fun serviceName(key: String): Int = SERVICE_NAMES[key] ?: R.string.anastasimatarion_title

    @StringRes
    fun groupName(key: String): Int = GROUP_NAMES[key] ?: R.string.anastasimatarion_title

    /** The catalog's Greek notes («Θεοτοκίο», «Ψαλμός 140», «Ωδή α΄») as a string resource + argument. */
    fun note(note: String): NoteLabel = when {
        note == "Θεοτοκίο" -> NoteLabel(R.string.anastasimatarion_note_theotokion, null)
        note == "Ψαλμός 140" -> NoteLabel(R.string.anastasimatarion_note_psalm140, null)
        note.startsWith("Ωδή ") -> NoteLabel(R.string.anastasimatarion_note_ode, note.removePrefix("Ωδή ").trim())
        else -> NoteLabel(null, note)
    }

    /** [res] formatted with [argument] when both exist; [argument] alone when there is no resource. */
    data class NoteLabel(@StringRes val res: Int?, val argument: String?)
}
