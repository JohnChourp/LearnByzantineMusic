package com.johnchourp.learnbyzantinemusic.analysis

import android.content.Context
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject

data class CoreSymbolRule(
    val id: String,
    val role: String,
    val templateDrawable: String,
    val baseToken: String?,
    val deltaSteps: Int,
    val defaultDurationBeats: Float,
    val durationDeltaBeats: Float,
    val setDurationBeats: Float?,
    val redistributeFromPreviousBeats: Float
)

data class TemplateMatch(
    val symbolId: String,
    val role: String,
    val confidence: Float,
    val baseToken: String?,
    val origin: String
)

data class MkTemplateMatch(
    val token: String,
    val confidence: Float,
    val templateAssetPath: String
)

private data class MkTemplateSymbol(
    val token: String,
    val templateAssetPath: String
)

class ByzantineTemplateRepository(
    private val context: Context
) {
    private val templateCache = mutableMapOf<String, BinaryImage>()

    val phthongsOrder: List<String> by lazy {
        loadModeRulesJson().optJSONArray("phthongsOrder")?.toStringList()
            ?: listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω")
    }

    val modeProfiles: Map<String, ModeProfile> by lazy {
        val modeArray = loadModeRulesJson().optJSONArray("modes") ?: JSONArray()
        buildMap {
            for (index in 0 until modeArray.length()) {
                val row = modeArray.optJSONObject(index) ?: continue
                val id = row.optString("id")
                if (id.isBlank()) continue
                val heightsObj = row.optJSONObject("noteHeights") ?: JSONObject()
                val heights = buildMap<String, Float> {
                    heightsObj.keys().forEach { note ->
                        put(note, heightsObj.optDouble(note, 0.0).toFloat())
                    }
                }
                put(id, ModeProfile(id = id, noteHeights = heights))
            }
        }
    }

    val coreRules: List<CoreSymbolRule> by lazy {
        val symbols = loadCoreRulesJson().optJSONArray("symbols") ?: JSONArray()
        buildList {
            for (index in 0 until symbols.length()) {
                val row = symbols.optJSONObject(index) ?: continue
                val id = row.optString("id")
                val role = row.optString("role")
                val drawable = row.optString("templateDrawable")
                if (id.isBlank() || role.isBlank() || drawable.isBlank()) {
                    continue
                }
                add(
                    CoreSymbolRule(
                        id = id,
                        role = role,
                        templateDrawable = drawable,
                        baseToken = row.optString("baseToken").ifBlank { null },
                        deltaSteps = row.optInt("deltaSteps", 0),
                        defaultDurationBeats = row.optDouble("defaultDurationBeats", 1.0).toFloat(),
                        durationDeltaBeats = row.optDouble("durationDeltaBeats", 0.0).toFloat(),
                        setDurationBeats = if (row.has("setDurationBeats")) row.optDouble("setDurationBeats").toFloat() else null,
                        redistributeFromPreviousBeats = row.optDouble("redistributeFromPreviousBeats", 0.0).toFloat()
                    )
                )
            }
        }
    }

    val coreRulesById: Map<String, CoreSymbolRule> by lazy {
        coreRules.associateBy { it.id }
    }

    val displayNames: Map<String, String> by lazy {
        val obj = loadDisplayNamesJson().optJSONObject("symbolNames") ?: JSONObject()
        buildMap {
            obj.keys().forEach { key ->
                val value = obj.optString(key)
                if (value.isNotBlank()) {
                    put(key, value)
                }
            }
        }
    }

    private val mkFallbackSymbols: List<MkTemplateSymbol> by lazy {
        val catalog = loadMkCatalogJson()
        buildList {
            for (index in 0 until catalog.length()) {
                val row = catalog.optJSONObject(index) ?: continue
                val token = row.optString("key").ifBlank { row.optString("token") }
                val templateAssetPath = row.optString("templateAssetPath")
                if (token.isBlank() || templateAssetPath.isBlank()) {
                    continue
                }
                add(MkTemplateSymbol(token = token, templateAssetPath = templateAssetPath))
            }
        }
    }

    fun findBestCoreMatch(target: BinaryImage): TemplateMatch? {
        var best: TemplateMatch? = null
        for (rule in coreRules) {
            val template = loadCoreTemplateBinary(rule.templateDrawable) ?: continue
            val score = BinaryImageOps.foregroundF1Score(target, template)
            if (best == null || score > best.confidence) {
                best = TemplateMatch(
                    symbolId = rule.id,
                    role = rule.role,
                    confidence = score,
                    baseToken = rule.baseToken,
                    origin = "core"
                )
            }
        }
        return best
    }

    fun findBestFallbackMkMatch(target: BinaryImage): MkTemplateMatch? {
        var best: MkTemplateMatch? = null
        for (symbol in mkFallbackSymbols) {
            val template = loadAssetTemplateBinary(symbol.templateAssetPath) ?: continue
            val score = BinaryImageOps.foregroundF1Score(target, template)
            if (best == null || score > best.confidence) {
                best = MkTemplateMatch(
                    token = symbol.token,
                    confidence = score,
                    templateAssetPath = symbol.templateAssetPath
                )
            }
        }
        return best
    }

    fun displayNameFor(symbolId: String): String =
        displayNames[symbolId]
            ?: coreRulesById[symbolId]?.id
            ?: symbolId

    private fun loadCoreTemplateBinary(drawableName: String): BinaryImage? {
        return templateCache.getOrPut("drawable:$drawableName") {
            val resId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
            if (resId == 0) {
                return null
            }
            val bitmap = BitmapFactory.decodeResource(context.resources, resId) ?: return null
            val threshold = BinaryImageOps.estimateAdaptiveThreshold(bitmap)
            val binary = BinaryImageOps.bitmapToBinary(bitmap, threshold)
            BinaryImageOps.normalize(BinaryImageOps.applyMorphology(BinaryImageOps.denoise(binary)), NORMALIZED_SIZE)
        }
    }

    private fun loadAssetTemplateBinary(path: String): BinaryImage? {
        return templateCache.getOrPut("asset:$path") {
            val bitmap = runCatching {
                context.assets.open(path).use { input -> BitmapFactory.decodeStream(input) }
            }.getOrNull() ?: return null

            val threshold = BinaryImageOps.estimateAdaptiveThreshold(bitmap)
            val binary = BinaryImageOps.bitmapToBinary(bitmap, threshold)
            BinaryImageOps.normalize(BinaryImageOps.applyMorphology(BinaryImageOps.denoise(binary)), NORMALIZED_SIZE)
        }
    }

    private fun loadMkCatalogJson(): JSONArray {
        val content = runCatching {
            context.assets.open(MK_CATALOG_ASSET).bufferedReader().use { it.readText() }
        }.getOrDefault("[]")
        return JSONArray(content)
    }

    private fun loadCoreRulesJson(): JSONObject {
        val content = runCatching {
            context.assets.open(CORE_RULES_ASSET).bufferedReader().use { it.readText() }
        }.getOrDefault("{}")
        return JSONObject(content)
    }

    private fun loadDisplayNamesJson(): JSONObject {
        val content = runCatching {
            context.assets.open(DISPLAY_NAMES_ASSET).bufferedReader().use { it.readText() }
        }.getOrDefault("{}")
        return JSONObject(content)
    }

    private fun loadModeRulesJson(): JSONObject {
        val content = runCatching {
            context.assets.open(MODE_RULES_ASSET).bufferedReader().use { it.readText() }
        }.getOrDefault("{}")
        return JSONObject(content)
    }

    private fun JSONArray.toStringList(): List<String> =
        buildList {
            for (index in 0 until length()) {
                val value = optString(index)
                if (value.isNotBlank()) {
                    add(value)
                }
            }
        }

    private companion object {
        const val MK_CATALOG_ASSET = "byzantine_symbols_catalog_v1.json"
        const val CORE_RULES_ASSET = "byzantine_core_symbol_rules_v2.json"
        const val DISPLAY_NAMES_ASSET = "byzantine_display_names_v1.json"
        const val MODE_RULES_ASSET = "byzantine_mode_rules_v1.json"
        const val NORMALIZED_SIZE = 64
    }
}
