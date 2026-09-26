package com.johnchourp.learnbyzantinemusic.lectern

import kotlin.math.roundToInt

/**
 * The reader's night tint (ClickUp `869f5x2e7`): the page dark and the ink light, so a lit phone on the
 * ἀναλόγιο of a dim church does not glare — applied as a colour matrix on the rendered page, with no
 * second rendering.
 *
 * **Not a plain inversion.** Inverting turns the red of the martyries and φθορές cyan, and a red sign
 * reads as red for a reason. The matrix inverts the lightness and turns the hue half a circle back (an
 * «invert, then hue-rotate 180°», with the Rec. 709 weights of the usual hue-rotation matrix), so white
 * paper becomes black, black ink light, and red stays red. The ink is then dimmed a little and warmed
 * (×0.90 red, ×0.87 green, ×0.80 blue), because full white on black is its own glare.
 *
 * [MATRIX] is the row-major 4×5 form both `android.graphics.ColorMatrix` and Compose's `ColorMatrix` take:
 * channels 0–255, the fifth column an offset in that range, every result clamped to it.
 */
object LecternNightTint {

    private val DIM = floatArrayOf(0.90f, 0.87f, 0.80f)

    /** Invert, then rotate the hue by 180°: -H + 255, H being the hue rotation's matrix (rows sum to 1). */
    private val INVERT_KEEPING_HUE = arrayOf(
        floatArrayOf(0.574f, -1.430f, -0.144f),
        floatArrayOf(-0.426f, -0.430f, -0.144f),
        floatArrayOf(-0.426f, -1.430f, 0.856f),
    )

    val MATRIX: FloatArray = FloatArray(20).also { matrix ->
        for (row in 0 until 3) {
            for (column in 0 until 3) matrix[row * 5 + column] = INVERT_KEEPING_HUE[row][column] * DIM[row]
            matrix[row * 5 + 4] = 255f * DIM[row]
        }
        matrix[3 * 5 + 3] = 1f // alpha unchanged
    }

    /** What [MATRIX] makes of one opaque colour, clamped as the platform clamps it — for the tests. */
    fun apply(red: Int, green: Int, blue: Int): Triple<Int, Int, Int> {
        fun channel(row: Int): Int {
            val value = MATRIX[row * 5] * red + MATRIX[row * 5 + 1] * green + MATRIX[row * 5 + 2] * blue + MATRIX[row * 5 + 4]
            return value.roundToInt().coerceIn(0, 255)
        }
        return Triple(channel(0), channel(1), channel(2))
    }
}
