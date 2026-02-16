package com.johnchourp.learnbyzantinemusic.analysis

import android.graphics.Bitmap
import android.graphics.Rect

data class MelodyAnalysisResult(
    val cropRect: Rect,
    val cropBitmap: Bitmap,
    val events: List<RecognizedNeumeEvent>,
    val notePath: List<String>,
    val modeHeights: Map<String, Float>,
    val unknownCount: Int,
    val lowConfidenceCount: Int
)
