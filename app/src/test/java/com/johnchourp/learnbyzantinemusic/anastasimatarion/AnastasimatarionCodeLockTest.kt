package com.johnchourp.learnbyzantinemusic.anastasimatarion

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A hymn's code never moves to another hymn (ClickUp `869f5x2a9`).
 *
 * The code is a permanent key on the user's device, in two places:
 * - the hymn's recordings folder, `<code> <incipit>`, found again by its `"<code> "` prefix
 *   ([HymnFolders]);
 * - the hymn's analysis settings, stored under `hymn:<mode>:<code>` (`AnalysisSettingsStore`).
 *
 * The generator used to number the hymns in display order, so one hymn added early in a mode would
 * have moved every later code, and the user's recordings and settings would have re-attached to a
 * different hymn without a word. The catalog test even *required* that renumbering (01..N).
 *
 * `scripts/anastasimatarion-codes.lock.json` now maps each hymn's identity — service, group,
 * incipit, and occurrence for hymns sharing all three — to its code, and the generator honours it.
 * CI runs no Node, so this test is the gate: the shipped asset must match the lock exactly. As in
 * [AnastasimatarionAssetSchemaTest], every rule is proven to reject the mistake it exists for, by
 * mutating the real files and asserting the specific message.
 */
class AnastasimatarionCodeLockTest {

    private val assetText: String by lazy { read("app/src/main/assets/$ASSET_NAME", "src/main/assets/$ASSET_NAME") }
    private val lockText: String by lazy { read(LOCK, "../$LOCK") }

    private fun read(vararg candidates: String): String =
        (candidates.map(::File).firstOrNull { it.isFile }
            ?: error("${candidates.first()} not found from ${File("").absolutePath}")).readText()

    private fun assertRejects(because: String, asset: String = assetText, lock: String = lockText) {
        val problems = validate(asset, lock)
        assertTrue(
            "expected a problem mentioning \"$because\", got: ${problems.take(4)}",
            problems.any { it.contains(because) },
        )
    }

    /**
     * The mutated JSON — or a failure when the change touched nothing. Compared as JSON, not as
     * text: re-serialising alone changes the text, so a text comparison could not tell.
     */
    private fun mutated(text: String, change: (JSONObject) -> Unit): String {
        val json = JSONObject(text)
        change(json)
        assertFalse(
            "the mutation changed nothing — its anchor is not in the file",
            canonical(JSONObject(text)) == canonical(json),
        )
        return json.toString()
    }

    /** JSON as plain maps and lists, so two documents compare by content. */
    private fun canonical(node: Any?): Any? = when (node) {
        is JSONObject -> node.keys().asSequence().associateWith { canonical(node.get(it)) }
        is JSONArray -> (0 until node.length()).map { canonical(node.get(it)) }
        else -> node
    }

    // ---- the real files -----------------------------------------------------------------------

    @Test
    fun theFilesAreActuallyLoaded() {
        // Guards the slice: with nothing loaded, every rule below would pass over an empty list.
        val hymns = assetHymns(assetText)
        assertEquals(8, hymns.size)
        val count = hymns.values.sumOf { it.size }
        assertTrue("expected the full catalog, found $count hymns", count >= 360)
        val modes = JSONObject(lockText).getJSONObject("modes")
        assertEquals(count, modes.keys().asSequence().sumOf { modes.getJSONObject(it).getJSONArray("hymns").length() })
    }

    @Test
    fun theShippedAssetMatchesTheLock() {
        assertEquals(emptyList<String>(), validate(assetText, lockText))
    }

    // ---- the mistakes this exists for -----------------------------------------------------------

    @Test
    fun swappingTwoHymnsCodesIsRejected() {
        val swapped = mutated(assetText) { root ->
            val hymns = root.mode("first").group("stichera_anastasima").getJSONArray("hymns")
            val a = hymns.getJSONObject(0)
            val b = hymns.getJSONObject(1)
            val code = a.getString("code")
            a.put("code", b.getString("code"))
            b.put("code", code)
        }
        assertRejects("«Τὰς ἑσπερινὰς ἡμῶν εὐχάς» (vespers/stichera_anastasima) is locked as 02", asset = swapped)
    }

    @Test
    fun insertingAHymnEarlyAndRenumberingIsRejected() {
        // Exactly what the old generator did: a new hymn early in the mode, then 01..N in order.
        val renumbered = mutated(assetText) { root ->
            val mode = root.mode("first")
            mode.group("stichera_anastasima").prepend(JSONObject().put("code", "00").put("incipit", NEWCOMER))
            mode.allHymns().forEachIndexed { index, hymn -> hymn.put("code", "%02d".format(index + 1)) }
        }
        assertRejects("«$NEWCOMER» (vespers/stichera_anastasima) is not in the lock", asset = renumbered)
        assertRejects("«Τὰς ἑσπερινὰς ἡμῶν εὐχάς» (vespers/stichera_anastasima) is locked as 02", asset = renumbered)
    }

    @Test
    fun aNewHymnDisplayedEarlyWithTheNextFreeCodeIsAccepted() {
        // The right way to add a hymn must pass: displayed first in its group, coded at the end.
        val asset = mutated(assetText) { root ->
            root.mode("first").group("stichera_anastasima").prepend(JSONObject().put("code", "46").put("incipit", NEWCOMER))
        }
        val lock = mutated(lockText) { root ->
            root.getJSONObject("modes").getJSONObject("first").getJSONArray("hymns").put(
                JSONObject().put("code", "46").put("service", "vespers").put("group", "stichera_anastasima").put("incipit", NEWCOMER),
            )
        }
        assertEquals(emptyList<String>(), validate(asset, lock))
    }

    @Test
    fun aLockedHymnMissingFromTheAssetIsRejected() {
        val removed = mutated(assetText) { root ->
            root.mode("first").group("stichera_anastasima").getJSONArray("hymns").remove(1)
        }
        assertRejects("first 03: the locked hymn «Κυκλώσατε λαοὶ Σιών» (vespers/stichera_anastasima) is no longer in the asset", asset = removed)
    }

    @Test
    fun aRetiredCodeIsNeverUsedAgain() {
        // The lock retired 45; the asset still gives it out.
        val lock = mutated(lockText) { root ->
            val first = root.getJSONObject("modes").getJSONObject("first")
            val hymns = first.getJSONArray("hymns")
            val last = hymns.getJSONObject(hymns.length() - 1)
            hymns.remove(hymns.length() - 1)
            first.getJSONArray("retired").put(last)
        }
        assertRejects("first 45: retired, yet", lock = lock)
    }

    @Test
    fun aCodeBothLockedAndRetiredIsRejected() {
        val lock = mutated(lockText) { root ->
            val first = root.getJSONObject("modes").getJSONObject("first")
            first.getJSONArray("retired").put(JSONObject(first.getJSONArray("hymns").getJSONObject(0).toString()))
        }
        assertRejects("first 01: both locked and retired", lock = lock)
    }

    @Test
    fun aCodeLockedTwiceIsRejected() {
        val lock = mutated(lockText) { root ->
            root.getJSONObject("modes").getJSONObject("first").getJSONArray("hymns").getJSONObject(1).put("code", "01")
        }
        assertRejects("first 01: locked twice", lock = lock)
    }

    @Test
    fun anInstructionLeftInTheLockMeansTheGeneratorDidNotRun() {
        val lock = mutated(lockText) { root ->
            root.getJSONObject("modes").getJSONObject("first").put("retire", JSONArray().put(JSONObject().put("code", "03")))
        }
        assertRejects("first: pending \"retire\"", lock = lock)
    }

    @Test
    fun aCodeThatIsNotTwoDigitsIsRejected() {
        val asset = mutated(assetText) { root ->
            root.mode("first").group("kekragarion").getJSONArray("hymns").getJSONObject(0).put("code", "1")
        }
        assertRejects("has code \"1\", not two digits", asset = asset)
    }

    @Test
    fun theLockCarriesNoSource() {
        // The confidential policy of the asset (AnastasimatarionAssetSchemaTest) holds for the lock,
        // which carries the same incipits.
        assertRejects("contains a URL", lock = lockText.replaceFirst("\"about\": \"", "\"about\": \"https://example.org "))
        assertRejects("forbidden provenance field: source", lock = mutated(lockText) { it.put("source", "x") })
        assertTrue(validate(assetText, "not json at all").any { it.contains("not a JSON object") })
    }

    // ---- identity ---------------------------------------------------------------------------------

    @Test
    fun theVarysTwinsAreTwoHymns() {
        // Same incipit, «Δεῦτε ἀγαλλιασώμεθα τῷ Κυρίῳ», in two groups: two identities, two codes.
        val twins = assetHymns(assetText).getValue("varys").filter { it.second.incipit == "Δεῦτε ἀγαλλιασώμεθα τῷ Κυρίῳ" }
        assertEquals(listOf("02", "42"), twins.map { it.first })
        assertNotEquals(twins[0].second, twins[1].second)
    }

    @Test
    fun twinsInsideOneGroupAreToldApartByOccurrence() {
        val catalog = """{"version": 1, "modes": [{"key": "m", "services": [{"key": "s", "groups": [
            {"key": "g", "hymns": [{"code": "01", "incipit": "X"}, {"code": "02", "incipit": "X"}]}]}]}]}"""
        assertEquals(listOf(1, 2), assetHymns(catalog).getValue("m").map { it.second.occurrence })
    }

    private companion object {
        const val ASSET_NAME = "anastasimatarion_v1.json"
        const val LOCK = "scripts/anastasimatarion-codes.lock.json"
        const val NEWCOMER = "Νέος ύμνος πρώτος στη σειρά"

        val CODE = Regex("""\d{2}""")
        val URL = Regex("""https?://""")

        /** The same provenance keys the asset may not carry. */
        val FORBIDDEN_KEYS = setOf("source", "source_url", "source_label", "url", "domain")

        /** What a hymn is — never where it is displayed. */
        data class Identity(val service: String, val group: String, val incipit: String, val occurrence: Int) {
            override fun toString() = "«$incipit» ($service/$group${if (occurrence > 1) ", occurrence $occurrence" else ""})"
        }

        /** Each mode's hymns in display order, as code and identity — read by the app's own parser. */
        fun assetHymns(assetText: String): Map<String, List<Pair<String, Identity>>> =
            AnastasimatarionCatalogParser.parse(assetText).modes.associate { mode ->
                val seen = mutableMapOf<Triple<String, String, String>, Int>()
                mode.key to mode.services.flatMap { service ->
                    service.groups.flatMap { group ->
                        group.hymns.map { hymn ->
                            val shared = Triple(service.key, group.key, hymn.incipit)
                            val occurrence = (seen[shared] ?: 0) + 1
                            seen[shared] = occurrence
                            hymn.code to Identity(service.key, group.key, hymn.incipit, occurrence)
                        }
                    }
                }
            }

        fun validate(assetText: String, lockText: String): List<String> {
            val lock = try {
                JSONObject(lockText)
            } catch (_: Exception) {
                return listOf("$LOCK is not a JSON object")
            }
            val problems = mutableListOf<String>()
            if (URL.containsMatchIn(lockText)) problems += "$LOCK contains a URL"
            walk(lock, problems)
            if (lock.optInt("version") != 1) problems += "$LOCK is not version 1"
            val modes = lock.optJSONObject("modes") ?: return problems + "$LOCK has no modes"
            val asset = assetHymns(assetText)
            val lockedModes = modes.keys().asSequence().toSet()
            if (lockedModes != asset.keys) problems += "$LOCK has the modes ${lockedModes.sorted()}, the asset ${asset.keys.sorted()}"
            for ((modeKey, hymns) in asset) {
                problems += checkMode(modeKey, hymns, modes.optJSONObject(modeKey) ?: continue)
            }
            return problems
        }

        private fun checkMode(modeKey: String, hymns: List<Pair<String, Identity>>, entry: JSONObject): List<String> {
            val problems = mutableListOf<String>()
            for (instruction in listOf("rename", "retire")) {
                if ((entry.optJSONArray(instruction)?.length() ?: 0) > 0) {
                    problems += "$modeKey: pending \"$instruction\" in the lock — run the generator, which applies and records it"
                }
            }
            val locked = linkedMapOf<String, Identity>()
            val codeOf = mutableMapOf<Identity, String>()
            for (hymn in entry.objects("hymns")) {
                val code = hymn.optString("code")
                val identity = identityOf(hymn)
                if (!CODE.matches(code)) problems += "$modeKey: locked code \"$code\" is not two digits"
                if (locked.put(code, identity) != null) problems += "$modeKey $code: locked twice"
                codeOf.put(identity, code)?.let { problems += "$modeKey: $identity is locked twice, as $it and $code" }
            }
            val retired = mutableSetOf<String>()
            for (hymn in entry.objects("retired")) {
                val code = hymn.optString("code")
                if (!retired.add(code)) problems += "$modeKey $code: retired twice"
                if (code in locked) problems += "$modeKey $code: both locked and retired — a retired code is never used again"
            }
            val given = mutableSetOf<String>()
            for ((code, identity) in hymns) {
                if (!CODE.matches(code)) problems += "$modeKey: $identity has code \"$code\", not two digits"
                if (!given.add(code)) problems += "$modeKey $code: given to two hymns"
                if (code in retired) problems += "$modeKey $code: retired, yet $identity uses it"
                when (val lockedCode = codeOf[identity]) {
                    null -> problems += "$modeKey $code: $identity is not in the lock — the generator gives a new hymn the next free code"
                    code -> Unit
                    else -> problems += "$modeKey $code: $identity is locked as $lockedCode — its recordings and analysis " +
                        "settings would re-attach to another hymn"
                }
            }
            val present = hymns.map { it.second }.toSet()
            for ((code, identity) in locked) {
                if (identity !in present) {
                    problems += "$modeKey $code: the locked hymn $identity is no longer in the asset — the generator " +
                        "records a removed hymn under \"retired\""
                }
            }
            return problems
        }

        private fun identityOf(json: JSONObject) = Identity(
            service = json.optString("service"),
            group = json.optString("group"),
            incipit = json.optString("incipit"),
            occurrence = json.optInt("occurrence", 1),
        )

        private fun walk(node: Any?, problems: MutableList<String>) {
            when (node) {
                is JSONObject -> node.keys().forEach { key ->
                    if (key.lowercase() in FORBIDDEN_KEYS) problems += "$LOCK has a forbidden provenance field: $key"
                    walk(node.get(key), problems)
                }
                is JSONArray -> (0 until node.length()).forEach { walk(node.get(it), problems) }
            }
        }

        private fun JSONObject.objects(name: String): List<JSONObject> {
            val array = optJSONArray(name) ?: return emptyList()
            return (0 until array.length()).map { array.getJSONObject(it) }
        }

        fun JSONObject.mode(key: String): JSONObject =
            getJSONArray("modes").objectsIn().first { it.getString("key") == key }

        fun JSONObject.group(key: String): JSONObject =
            getJSONArray("services").objectsIn()
                .flatMap { it.getJSONArray("groups").objectsIn() }
                .first { it.getString("key") == key }

        fun JSONObject.allHymns(): List<JSONObject> =
            getJSONArray("services").objectsIn()
                .flatMap { it.getJSONArray("groups").objectsIn() }
                .flatMap { it.getJSONArray("hymns").objectsIn() }

        /** JSONArray has no insert: rebuilds the group's hymns with [hymn] first. */
        fun JSONObject.prepend(hymn: JSONObject) {
            val rebuilt = JSONArray().put(hymn)
            getJSONArray("hymns").objectsIn().forEach { rebuilt.put(it) }
            put("hymns", rebuilt)
        }

        private fun JSONArray.objectsIn(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    }
}
