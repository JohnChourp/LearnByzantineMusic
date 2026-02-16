package com.johnchourp.learnbyzantinemusic.scan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.max

class MelodyPathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5D4037")
        strokeWidth = dp(1.4f)
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        strokeWidth = dp(2.2f)
        style = Paint.Style.STROKE
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D47A1")
        style = Paint.Style.FILL
    }

    private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#212121")
        textSize = sp(12f)
        textAlign = Paint.Align.CENTER
    }

    private var notePath: List<String> = emptyList()
    private var modeHeights: Map<String, Float> = emptyMap()

    fun setNotePath(path: List<String>, modeHeights: Map<String, Float> = emptyMap()) {
        notePath = path
        this.modeHeights = modeHeights
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val notes = notePath
        if (notes.isEmpty()) {
            return
        }

        val phthongsOrder = listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω")
        val left = paddingLeft + dp(20f)
        val right = width - paddingRight - dp(16f)
        val top = paddingTop + dp(16f)
        val bottom = height - paddingBottom - dp(28f)
        if (right <= left || bottom <= top) {
            return
        }

        canvas.drawLine(left, bottom, right, bottom, axisPaint)
        canvas.drawLine(left, top, left, bottom, axisPaint)

        val stepX = if (notes.size <= 1) 0f else (right - left) / (notes.size - 1).toFloat()

        var previousX = 0f
        var previousY = 0f
        for (index in notes.indices) {
            val note = notes[index]
            val noteIndex = phthongsOrder.indexOf(note).let { if (it < 0) 0 else it }
            val normalized = modeHeights[note]
                ?: (noteIndex.toFloat() / (phthongsOrder.size - 1).toFloat())
            val y = bottom - normalized * (bottom - top)
            val x = left + index * stepX

            if (index > 0) {
                canvas.drawLine(previousX, previousY, x, y, linePaint)
            }
            canvas.drawCircle(x, y, dp(3.5f), pointPaint)
            canvas.drawText(note, x, y - dp(6f), notePaint)

            previousX = x
            previousY = y
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val desired = dp(190f).toInt()
        setMeasuredDimension(measuredWidth, resolveSize(max(desired, suggestedMinimumHeight), heightMeasureSpec))
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun sp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        value,
        resources.displayMetrics
    )
}
