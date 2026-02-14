package com.johnchourp.learnbyzantinemusic.modes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

enum class PhthongTouchAction {
    DOWN,
    MOVE,
    UP,
    CANCEL,
    EXIT
}

data class PhthongTouchEvent(
    val indexTopToBottom: Int,
    val action: PhthongTouchAction
)

class ScaleDiagramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var phthongsTopToBottom: List<String> = emptyList()
    private var intervalsTopToBottom: List<Int> = emptyList()

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#424242")
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#616161")
        style = Paint.Style.STROKE
        strokeWidth = dp(1.3f)
    }

    private val intervalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1f1f1f")
        textSize = sp(18f)
        textAlign = Paint.Align.CENTER
    }

    private val phthongTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1f1f1f")
        textSize = sp(19f)
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }

    private val phthongActiveTextPaint = Paint(phthongTextPaint).apply {
        color = Color.parseColor("#0D47A1")
    }

    private var phthongTouchListener: ((PhthongTouchEvent) -> Unit)? = null
    private var labelHitAreasTopToBottom: List<RectF> = emptyList()
    private var activeLabelIndexTopToBottom: Int = -1
    private var isTouchSequenceActive: Boolean = false

    fun setDiagramData(phthongsTopToBottom: List<String>, intervalsTopToBottom: List<Int>) {
        this.phthongsTopToBottom = phthongsTopToBottom
        this.intervalsTopToBottom = intervalsTopToBottom
        if (activeLabelIndexTopToBottom !in phthongsTopToBottom.indices) {
            activeLabelIndexTopToBottom = -1
        }
        invalidate()
    }

    fun setOnPhthongTouchListener(listener: ((PhthongTouchEvent) -> Unit)?) {
        phthongTouchListener = listener
    }

    fun clearTouchState() {
        isTouchSequenceActive = false
        if (activeLabelIndexTopToBottom != -1) {
            activeLabelIndexTopToBottom = -1
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (intervalsTopToBottom.isEmpty() || phthongsTopToBottom.size != intervalsTopToBottom.size + 1) {
            labelHitAreasTopToBottom = emptyList()
            return
        }

        val insets = dp(12f)
        val labelGap = dp(12f)
        val maxLabelWidth = phthongsTopToBottom.maxOfOrNull { phthongTextPaint.measureText(it) } ?: 0f
        val labelHalfHeight = phthongTextPaint.fontSpacing / 2f

        val chartLeft = paddingLeft + insets
        val chartTop = paddingTop + insets + labelHalfHeight
        val chartRight = width - paddingRight - insets - maxLabelWidth - labelGap
        val chartBottom = height - paddingBottom - insets - labelHalfHeight

        if (chartRight <= chartLeft || chartBottom <= chartTop) {
            labelHitAreasTopToBottom = emptyList()
            return
        }

        canvas.drawRect(chartLeft, chartTop, chartRight, chartBottom, borderPaint)

        val segmentHeights = calculateSegmentHeights(chartTop, chartBottom)
        if (segmentHeights.isEmpty()) {
            labelHitAreasTopToBottom = emptyList()
            return
        }
        val boundaries = buildBoundaries(chartTop, segmentHeights)

        for (index in 1 until boundaries.lastIndex) {
            val y = boundaries[index]
            canvas.drawLine(chartLeft, y, chartRight, y, linePaint)
        }

        for (index in intervalsTopToBottom.indices) {
            val centerY = (boundaries[index] + boundaries[index + 1]) / 2f
            canvas.drawText(
                intervalsTopToBottom[index].toString(),
                (chartLeft + chartRight) / 2f,
                centerY + centeredBaseline(intervalTextPaint),
                intervalTextPaint
            )
        }

        val hitAreaPadding = dp(8f)
        val hitAreas = mutableListOf<RectF>()
        for (index in phthongsTopToBottom.indices) {
            val boundaryY = boundaries[index]
            val text = phthongsTopToBottom[index]
            val baselineY = boundaryY + centeredBaseline(phthongTextPaint)
            val drawPaint = if (index == activeLabelIndexTopToBottom) phthongActiveTextPaint else phthongTextPaint
            canvas.drawText(
                text,
                chartRight + labelGap,
                baselineY,
                drawPaint
            )

            val textWidth = phthongTextPaint.measureText(text)
            val hitRect = RectF(
                chartRight + labelGap - hitAreaPadding,
                baselineY + phthongTextPaint.ascent() - hitAreaPadding,
                chartRight + labelGap + textWidth + hitAreaPadding,
                baselineY + phthongTextPaint.descent() + hitAreaPadding
            )
            hitAreas.add(hitRect)
        }
        labelHitAreasTopToBottom = hitAreas
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (labelHitAreasTopToBottom.isEmpty()) {
            return super.onTouchEvent(event)
        }
        val touchedIndex = findLabelIndex(event.x, event.y)
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleDown(touchedIndex)
            MotionEvent.ACTION_MOVE -> handleMove(touchedIndex)
            MotionEvent.ACTION_UP -> handleUp()
            MotionEvent.ACTION_CANCEL -> handleCancel()
            else -> super.onTouchEvent(event)
        }
    }

    private fun handleDown(touchedIndex: Int): Boolean {
        if (touchedIndex < 0) {
            return false
        }
        isTouchSequenceActive = true
        activeLabelIndexTopToBottom = touchedIndex
        phthongTouchListener?.invoke(PhthongTouchEvent(touchedIndex, PhthongTouchAction.DOWN))
        parent?.requestDisallowInterceptTouchEvent(true)
        invalidate()
        return true
    }

    private fun handleMove(touchedIndex: Int): Boolean {
        if (!isTouchSequenceActive) {
            return false
        }
        val activeIndex = activeLabelIndexTopToBottom
        if (activeIndex < 0) {
            if (touchedIndex >= 0) {
                activeLabelIndexTopToBottom = touchedIndex
                phthongTouchListener?.invoke(PhthongTouchEvent(touchedIndex, PhthongTouchAction.MOVE))
                invalidate()
            }
            return true
        }
        if (touchedIndex == activeIndex) {
            return true
        }
        if (touchedIndex >= 0) {
            activeLabelIndexTopToBottom = touchedIndex
            phthongTouchListener?.invoke(PhthongTouchEvent(touchedIndex, PhthongTouchAction.MOVE))
        } else {
            activeLabelIndexTopToBottom = -1
            phthongTouchListener?.invoke(PhthongTouchEvent(activeIndex, PhthongTouchAction.EXIT))
        }
        invalidate()
        return true
    }

    private fun handleUp(): Boolean {
        val activeIndex = activeLabelIndexTopToBottom
        isTouchSequenceActive = false
        if (activeIndex < 0) {
            return true
        }
        phthongTouchListener?.invoke(PhthongTouchEvent(activeIndex, PhthongTouchAction.UP))
        activeLabelIndexTopToBottom = -1
        invalidate()
        return true
    }

    private fun handleCancel(): Boolean {
        val activeIndex = activeLabelIndexTopToBottom
        isTouchSequenceActive = false
        if (activeIndex < 0) {
            return true
        }
        phthongTouchListener?.invoke(PhthongTouchEvent(activeIndex, PhthongTouchAction.CANCEL))
        activeLabelIndexTopToBottom = -1
        invalidate()
        return true
    }

    private fun findLabelIndex(x: Float, y: Float): Int =
        labelHitAreasTopToBottom.indexOfFirst { it.contains(x, y) }

    private fun calculateSegmentHeights(chartTop: Float, chartBottom: Float): List<Float> {
        if (intervalsTopToBottom.isEmpty()) {
            return emptyList()
        }

        val totalHeight = chartBottom - chartTop
        if (totalHeight <= 0f) {
            return emptyList()
        }

        val minSegmentHeight = dp(12f)
        val freeHeight = max(0f, totalHeight - minSegmentHeight * intervalsTopToBottom.size)
        val totalInterval = intervalsTopToBottom.sum().toFloat()

        return if (totalInterval > 0f) {
            intervalsTopToBottom.map { interval ->
                minSegmentHeight + (freeHeight * (interval / totalInterval))
            }
        } else {
            val equalHeight = totalHeight / intervalsTopToBottom.size
            List(intervalsTopToBottom.size) { equalHeight }
        }
    }

    private fun buildBoundaries(chartTop: Float, segmentHeights: List<Float>): List<Float> {
        val boundaries = MutableList(segmentHeights.size + 1) { 0f }
        boundaries[0] = chartTop
        var current = chartTop
        for (index in segmentHeights.indices) {
            current += segmentHeights[index]
            boundaries[index + 1] = current
        }
        return boundaries
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val desiredHeight = dp(520f).toInt()
        setMeasuredDimension(measuredWidth, resolveSize(max(desiredHeight, suggestedMinimumHeight), heightMeasureSpec))
    }

    private fun centeredBaseline(paint: Paint): Float = -(paint.ascent() + paint.descent()) / 2f

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun sp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        value,
        resources.displayMetrics
    )
}
