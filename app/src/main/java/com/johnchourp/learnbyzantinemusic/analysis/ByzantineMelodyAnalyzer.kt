package com.johnchourp.learnbyzantinemusic.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.max

class ByzantineMelodyAnalyzer(
    context: Context
) {
    private val repository = ByzantineTemplateRepository(context)
    private val melodyMapper = MelodyMapper(repository.phthongsOrder, repository.modeProfiles)
    private val rhythmMapper = ByzantineRhythmMapper()

    fun analyze(request: MelodyAnalysisRequest): MelodyAnalysisResult {
        val downscaled = downscaleIfNeeded(request.bitmap)
        val deskewed = deskewSmallAngle(downscaled)

        val threshold = BinaryImageOps.estimateAdaptiveThreshold(deskewed)
        val binary = BinaryImageOps.applyMorphology(
            BinaryImageOps.denoise(BinaryImageOps.bitmapToBinary(deskewed, threshold))
        )

        val lineRect = BinaryImageOps.detectFirstLineRect(binary)
        val lineBinary = BinaryImageOps.crop(binary, lineRect)
        val components = BinaryImageOps.detectConnectedComponents(lineBinary, minArea = 10)
        val grouped = groupComponents(components)

        val parsedEvents = grouped.take(MAX_EVENTS)
            .map { group -> parseGroup(group, lineRect, lineBinary) }
            .toMutableList()

        val durations = rhythmMapper.map(
            parsedEvents.map { event ->
                NeumeRhythmInput(
                    modifiers = event.modifiers,
                    baseDurationBeats = event.baseDurationBeats
                )
            }
        )
        parsedEvents.forEachIndexed { index, event ->
            event.durationBeats = durations.getOrElse(index) { event.baseDurationBeats }
        }

        val traversal = melodyMapper.map(
            basePhthong = request.basePhthong,
            deltas = parsedEvents.map { it.deltaSteps },
            modeId = request.modeId
        )

        val finalEvents = parsedEvents.mapIndexed { index, event ->
            RecognizedNeumeEvent(
                baseSymbolId = event.baseSymbolId,
                baseToken = event.baseToken,
                modifiers = event.modifiers,
                confidence = event.confidence,
                bbox = event.bbox,
                deltaSteps = event.deltaSteps,
                durationBeats = event.durationBeats,
                noteLabel = traversal.notes.getOrElse(index) { request.basePhthong },
                displayNameEl = event.displayNameEl,
                warningFlags = event.warningFlags
            )
        }

        val cropBitmap = Bitmap.createBitmap(
            deskewed,
            lineRect.left,
            lineRect.top,
            lineRect.width().coerceAtLeast(1),
            lineRect.height().coerceAtLeast(1)
        )

        return MelodyAnalysisResult(
            cropRect = lineRect,
            cropBitmap = cropBitmap,
            events = finalEvents,
            notePath = traversal.notes,
            modeHeights = traversal.modeHeights,
            unknownCount = finalEvents.count { it.baseSymbolId == UNKNOWN_SYMBOL_ID },
            lowConfidenceCount = finalEvents.count { it.confidence < LOW_CONFIDENCE_THRESHOLD }
        )
    }

    private fun parseGroup(group: List<Rect>, lineRect: Rect, lineBinary: BinaryImage): MutableParsedEvent {
        val classified = group.map { component -> classifyComponent(component, lineRect, lineBinary) }

        val baseCandidate = classified
            .filter { it.coreMatch?.role == ROLE_BASE }
            .maxByOrNull { it.coreMatch?.confidence ?: 0f }

        val isBaseValid = (baseCandidate?.coreMatch?.confidence ?: 0f) >= BASE_MATCH_THRESHOLD
        val baseRule = if (isBaseValid) repository.coreRulesById[baseCandidate?.coreMatch?.symbolId] else null

        val modifiers = classified
            .mapNotNull { item ->
                val match = item.coreMatch
                if (match != null && match.role == ROLE_MODIFIER && match.confidence >= MODIFIER_MATCH_THRESHOLD) {
                    match.symbolId
                } else {
                    null
                }
            }
            .distinct()

        val warnings = mutableListOf<String>()
        if (baseRule == null) {
            warnings.add("unknown_base")
        }

        val unionRect = unionRects(classified.map { it.absoluteRect })

        val baseDurationBeats = (baseRule?.defaultDurationBeats ?: 1f).coerceAtLeast(0.5f)

        val fallbackToken = classified.maxByOrNull { it.mkMatch?.confidence ?: 0f }?.mkMatch?.token
        val baseToken = baseRule?.baseToken ?: fallbackToken
        val confidence = baseCandidate?.coreMatch?.confidence
            ?: classified.maxOfOrNull { it.coreMatch?.confidence ?: 0f }
            ?: 0f

        return MutableParsedEvent(
            baseSymbolId = baseRule?.id ?: UNKNOWN_SYMBOL_ID,
            baseToken = baseToken,
            modifiers = modifiers,
            confidence = confidence,
            bbox = unionRect,
            deltaSteps = baseRule?.deltaSteps ?: 0,
            baseDurationBeats = baseDurationBeats,
            durationBeats = baseDurationBeats,
            displayNameEl = repository.displayNameFor(baseRule?.id ?: UNKNOWN_SYMBOL_ID),
            warningFlags = warnings
        )
    }

    private fun classifyComponent(component: Rect, lineRect: Rect, lineBinary: BinaryImage): ClassifiedComponent {
        val padding = 1
        val localRect = Rect(
            (component.left - padding).coerceAtLeast(0),
            (component.top - padding).coerceAtLeast(0),
            (component.right + padding).coerceAtMost(lineBinary.width),
            (component.bottom + padding).coerceAtMost(lineBinary.height)
        )

        val absoluteRect = Rect(
            lineRect.left + localRect.left,
            lineRect.top + localRect.top,
            lineRect.left + localRect.right,
            lineRect.top + localRect.bottom
        )

        if (localRect.width() <= 0 || localRect.height() <= 0) {
            return ClassifiedComponent(absoluteRect, null, null)
        }

        val normalized = BinaryImageOps.normalize(BinaryImageOps.crop(lineBinary, localRect), TEMPLATE_SIZE)
        val coreMatch = repository.findBestCoreMatch(normalized)
        val mkMatch = if ((coreMatch?.confidence ?: 0f) < MK_FALLBACK_THRESHOLD) {
            repository.findBestFallbackMkMatch(normalized)
        } else {
            null
        }

        return ClassifiedComponent(
            absoluteRect = absoluteRect,
            coreMatch = coreMatch,
            mkMatch = mkMatch
        )
    }

    private fun groupComponents(components: List<Rect>): List<List<Rect>> {
        if (components.isEmpty()) {
            return emptyList()
        }
        val sorted = components.sortedBy { it.left }
        val widths = sorted.map { it.width() }.sorted()
        val medianWidth = widths[widths.size / 2].toFloat().coerceAtLeast(8f)
        val maxGap = (medianWidth * 0.65f).toInt().coerceAtLeast(6)

        val groups = mutableListOf<MutableList<Rect>>()
        var current = mutableListOf(sorted.first())
        var currentRight = sorted.first().right

        for (index in 1 until sorted.size) {
            val component = sorted[index]
            val gap = component.left - currentRight
            if (gap <= maxGap) {
                current.add(component)
                currentRight = max(currentRight, component.right)
            } else {
                groups.add(current)
                current = mutableListOf(component)
                currentRight = component.right
            }
        }
        groups.add(current)
        return groups
    }

    private fun unionRects(rects: List<Rect>): Rect {
        if (rects.isEmpty()) {
            return Rect(0, 0, 1, 1)
        }
        var left = rects.first().left
        var top = rects.first().top
        var right = rects.first().right
        var bottom = rects.first().bottom
        for (rect in rects.drop(1)) {
            if (rect.left < left) left = rect.left
            if (rect.top < top) top = rect.top
            if (rect.right > right) right = rect.right
            if (rect.bottom > bottom) bottom = rect.bottom
        }
        return Rect(left, top, right, bottom)
    }

    private fun deskewSmallAngle(bitmap: Bitmap): Bitmap {
        var bestBitmap = bitmap
        var bestScore = scoreHorizontalAlignment(bitmap)
        for (angle in -4..4) {
            if (angle == 0) continue
            val rotated = rotateBitmap(bitmap, angle.toFloat())
            val score = scoreHorizontalAlignment(rotated)
            if (score > bestScore) {
                bestScore = score
                bestBitmap = rotated
            }
        }
        return bestBitmap
    }

    private fun scoreHorizontalAlignment(bitmap: Bitmap): Float {
        val threshold = BinaryImageOps.estimateAdaptiveThreshold(bitmap)
        val binary = BinaryImageOps.bitmapToBinary(bitmap, threshold)
        val rowCounts = IntArray(binary.height)
        for (y in 0 until binary.height) {
            var count = 0
            for (x in 0 until binary.width) {
                if (binary.get(x, y)) count += 1
            }
            rowCounts[y] = count
        }
        val mean = rowCounts.average().toFloat()
        var variance = 0f
        for (value in rowCounts) {
            val diff = value - mean
            variance += diff * diff
        }
        return variance / rowCounts.size.coerceAtLeast(1)
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        if (abs(angle) < 0.0001f) {
            return source
        }
        val matrix = Matrix().apply { postRotate(angle) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            .copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun downscaleIfNeeded(bitmap: Bitmap): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= MAX_IMAGE_SIDE) {
            return bitmap
        }
        val scale = MAX_IMAGE_SIDE.toFloat() / maxSide.toFloat()
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            .copy(Bitmap.Config.ARGB_8888, false)
    }

    private data class MutableParsedEvent(
        val baseSymbolId: String,
        val baseToken: String?,
        val modifiers: List<String>,
        val confidence: Float,
        val bbox: Rect,
        val deltaSteps: Int,
        val baseDurationBeats: Float,
        var durationBeats: Float,
        val displayNameEl: String,
        val warningFlags: List<String>
    )

    private data class ClassifiedComponent(
        val absoluteRect: Rect,
        val coreMatch: TemplateMatch?,
        val mkMatch: MkTemplateMatch?
    )

    private companion object {
        const val TEMPLATE_SIZE = 64
        const val MAX_IMAGE_SIDE = 1600
        const val MAX_EVENTS = 200
        const val BASE_MATCH_THRESHOLD = 0.34f
        const val MODIFIER_MATCH_THRESHOLD = 0.30f
        const val MK_FALLBACK_THRESHOLD = 0.30f
        const val LOW_CONFIDENCE_THRESHOLD = 0.55f

        const val ROLE_BASE = "base"
        const val ROLE_MODIFIER = "modifier"

        const val UNKNOWN_SYMBOL_ID = "unknown"
    }
}
