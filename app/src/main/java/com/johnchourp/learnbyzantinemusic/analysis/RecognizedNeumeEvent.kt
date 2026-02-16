package com.johnchourp.learnbyzantinemusic.analysis

import android.graphics.Rect

data class RecognizedNeumeEvent(
    val baseSymbolId: String,
    val baseToken: String?,
    val modifiers: List<String>,
    val confidence: Float,
    val bbox: Rect,
    val deltaSteps: Int,
    val durationBeats: Float,
    val noteLabel: String,
    val displayNameEl: String,
    val warningFlags: List<String> = emptyList()
)
