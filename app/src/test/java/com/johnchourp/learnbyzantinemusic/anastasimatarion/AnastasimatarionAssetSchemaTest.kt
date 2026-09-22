package com.johnchourp.learnbyzantinemusic.anastasimatarion

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The confidential policy, enforced on the hymn catalog asset.
 *
 * `anastasimatarion_v1.json` must carry **no provenance**: no `source`/`url`-shaped key on any
 * object, and no `http(s)://` anywhere in the file — the same rule `CalendarDatasetSchema` already
 * enforces for the εορτολόγιο, decided for this asset in ClickUp `869dbkkwf` on 2026-09-22.
 *
 * Before this test existed the rule was enforced for exactly one asset, so this one could have
 * regained a source without anything turning red.
 *
 * Both halves matter: the **real shipped asset** must pass, and **every rule must be proven to
 * reject something**. Each mutation below is applied to the real asset and asserts the specific
 * message, so a rule cannot pass by being caught accidentally by another rule.
 */
class AnastasimatarionAssetSchemaTest {

    private val assetText: String by lazy {
        val candidates = listOf(
            File("app/src/main/assets/$ASSET_NAME"),
            File("src/main/assets/$ASSET_NAME"),
        )
        (candidates.firstOrNull { it.exists() }
            ?: error("$ASSET_NAME not found from ${File("").absolutePath}"))
            .readText()
    }

    /**
      * Asserts a mutation is rejected, and rejected *for the stated reason*.
      *
      * The [assertNotEquals] is not decoration. The first version of this file anchored a mutation
      * on `"title":`, a key that stopped existing the moment the `source` object was removed — the
      * transform silently did nothing and the test reported on the unmutated asset. A mutation that
      * does not mutate proves nothing about the rule it is supposed to exercise.
      */
    private fun assertRejects(because: String, transform: (String) -> String) {
        val mutated = transform(assetText)
        assertNotEquals("the mutation changed nothing — its anchor is not in the asset", assetText, mutated)
        val problems = validate(mutated)
        assertTrue(
            "expected a problem mentioning \"$because\", got: ${problems.take(3)}",
            problems.any { it.contains(because) },
        )
    }

    // ---- the real asset ----------------------------------------------------------------------

    @Test
    fun theAssetIsActuallyLoaded() {
        // Guards the slice: an empty string would be rejected as "not a JSON object" and every
        // mutation below would go green for the wrong reason.
        assertTrue("asset looks too small: ${assetText.length} chars", assetText.length > 20_000)
        assertTrue(assetText.trimStart().startsWith("{"))
        assertTrue("expected all 8 modes", JSONObject(assetText).getJSONArray("modes").length() == 8)
    }

    @Test
    fun theShippedAssetCarriesNoProvenance() {
        assertEquals(emptyList<String>(), validate(assetText))
    }

    // ---- each rule must reject something ------------------------------------------------------

    @Test
    fun aSourceObjectIsRejected() {
        assertRejects("forbidden provenance field: source") {
            it.replaceFirst("\"version\": 1,", "\"version\": 1,\n \"source\": {\"title\": \"x\"},")
        }
    }

    @Test
    fun aUrlKeyIsRejectedEvenNestedInsideAHymn() {
        // The policy is about the whole document, not only its header.
        assertRejects("forbidden provenance field: url") {
            it.replaceFirst("\"incipit\":", "\"url\": \"somewhere\", \"incipit\":")
        }
    }

    @Test
    fun aUrlAnywhereInTheTextIsRejectedEvenWithNoForbiddenKey() {
        // A bare value, under a perfectly allowed key name.
        // A key name that is allowed AND not already present on a hymn: org.json rejects duplicate
        // keys, so reusing `note` here would fail as malformed JSON instead of as a URL.
        assertRejects("contains a URL") {
            it.replaceFirst("\"code\":", "\"remark\": \"https://example.org\", \"code\":")
        }
    }

    @Test
    fun aFileThatIsNotJsonIsRejectedWithoutThrowing() {
        assertTrue(validate("not json at all").any { it.contains("not a JSON object") })
    }

    private companion object {
        const val ASSET_NAME = "anastasimatarion_v1.json"

        /** Provenance fields. Not allowed on any object in the file. */
        val FORBIDDEN_KEYS = setOf("source", "source_url", "source_label", "url", "domain")
        val URL = Regex("""https?://""")

        fun validate(text: String): List<String> {
            val problems = mutableListOf<String>()
            val root = try {
                JSONObject(text)
            } catch (_: Exception) {
                return listOf("$ASSET_NAME is not a JSON object")
            }
            if (URL.containsMatchIn(text)) problems += "$ASSET_NAME contains a URL"
            walk(root, problems)
            return problems
        }

        private fun walk(node: Any?, problems: MutableList<String>) {
            when (node) {
                is JSONObject -> node.keys().forEach { key ->
                    if (key.lowercase() in FORBIDDEN_KEYS) {
                        problems += "$ASSET_NAME has a forbidden provenance field: $key"
                    }
                    walk(node.get(key), problems)
                }
                is JSONArray -> (0 until node.length()).forEach { walk(node.get(it), problems) }
            }
        }
    }
}
