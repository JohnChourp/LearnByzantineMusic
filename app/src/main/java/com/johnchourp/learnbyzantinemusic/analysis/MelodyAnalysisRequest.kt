package com.johnchourp.learnbyzantinemusic.analysis

import android.graphics.Bitmap

data class MelodyAnalysisRequest(
    val modeId: String,
    val basePhthong: String,
    val bitmap: Bitmap
)
