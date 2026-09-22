package com.johnchourp.learnbyzantinemusic.anastasimatarion

import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R

/** Catalog keys → localized labels. Every key in the catalog must have an entry here (unit-tested). */
object AnastasimatarionLabels {
    /** Mode keys in the order of the liturgical cycle; index = LiturgicalToneCycle.toneIndex. */
    val MODE_ORDER = listOf(
        "first", "second", "third", "fourth", "plagal_first", "plagal_second", "varys", "plagal_fourth",
    )

    val MODE_NAMES: Map<String, Int> = mapOf(
        "first" to R.string.mode_first,
        "second" to R.string.mode_second,
        "third" to R.string.mode_third,
        "fourth" to R.string.mode_fourth,
        "plagal_first" to R.string.mode_plagal_first,
        "plagal_second" to R.string.mode_plagal_second,
        "varys" to R.string.mode_varys,
        "plagal_fourth" to R.string.mode_plagal_fourth,
    )

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
