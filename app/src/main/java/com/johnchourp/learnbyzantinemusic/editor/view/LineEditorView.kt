package com.johnchourp.learnbyzantinemusic.editor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatEditText
import com.johnchourp.learnbyzantinemusic.editor.model.CompositionLine
import kotlin.math.abs

class LineEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    interface Listener {
        fun onLineSelected(lineId: String, cursorIndex: Int)
        fun onLyricsChanged(lineId: String, lyrics: String, commitHistory: Boolean)
        fun onSymbolSelected(lineId: String, symbolId: String?)
        fun onSymbolMoveCommitted(lineId: String, symbolId: String, dxDp: Float, dyDp: Float)
    }

    private val editText = CursorAwareEditText(context)
    private val overlayView = object : View(context) {
        override fun onDraw(canvas: Canvas) {
            drawSymbols(canvas)
        }
    }

    private val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
    }
    private var symbolTypefaces: Map<String, Typeface> = emptyMap()

    private val symbolSelectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, 33, 150, 243)
        style = Paint.Style.FILL
    }

    private val symbolBounds = mutableMapOf<String, RectF>()
    private var line: CompositionLine? = null
    private var selectedSymbolId: String? = null
    private var listener: Listener? = null
    private var ignoreTextCallback = false

    private var draggingSymbolId: String? = null
    private var dragStarted = false
    private var dragDownX = 0f
    private var dragDownY = 0f
    private var dragStartDxPx = 0f
    private var dragStartDyPx = 0f

    init {
        isFocusable = true

        editText.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        editText.setBackgroundColor(Color.TRANSPARENT)
        editText.setPadding(dp(8f), dp(20f), dp(8f), dp(24f))
        editText.minLines = 2
        editText.maxLines = 6
        editText.setTextColor(Color.BLACK)

        overlayView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )

        addView(editText)
        addView(overlayView)

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (ignoreTextCallback) {
                    return
                }
                val currentLine = line ?: return
                val updatedLyrics = s?.toString().orEmpty()
                line = currentLine.copy(lyrics = updatedLyrics)
                listener?.onLyricsChanged(currentLine.id, updatedLyrics, false)
                overlayView.invalidate()
            }
        })

        editText.onSelectionChangedListener = selectionChanged@{ index ->
            val currentLine = line ?: return@selectionChanged
            listener?.onLineSelected(currentLine.id, index)
            selectedSymbolId = null
            overlayView.invalidate()
        }

        editText.setOnFocusChangeListener { _, hasFocus ->
            val currentLine = line ?: return@setOnFocusChangeListener
            if (!hasFocus) {
                listener?.onLyricsChanged(currentLine.id, editText.text?.toString().orEmpty(), true)
            } else {
                listener?.onLineSelected(currentLine.id, editText.selectionStart.coerceAtLeast(0))
            }
        }

        setPadding(dp(8f), dp(6f), dp(8f), dp(6f))
        setBackgroundColor(Color.argb(18, 0, 0, 0))
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun setSymbolTypefaces(typefaces: Map<String, Typeface>) {
        symbolTypefaces = typefaces
        overlayView.invalidate()
    }

    fun bind(line: CompositionLine, selectedSymbolId: String?) {
        this.line = line
        this.selectedSymbolId = selectedSymbolId

        ignoreTextCallback = true
        editText.setText(line.lyrics)
        val selection = editText.selectionStart.coerceAtMost(line.lyrics.length)
        editText.setSelection(selection)
        ignoreTextCallback = false

        overlayView.invalidate()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (handleSymbolTouch(event)) {
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    private fun handleSymbolTouch(event: MotionEvent): Boolean {
        val currentLine = line ?: return false
        if (symbolBounds.isEmpty()) {
            return false
        }

        val hitSymbolId = findHitSymbol(event.x, event.y)
        val activeDragId = draggingSymbolId

        if (activeDragId == null && hitSymbolId == null) {
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val symbolId = hitSymbolId ?: return false
                val symbol = currentLine.symbols.firstOrNull { it.id == symbolId } ?: return false

                draggingSymbolId = symbolId
                dragStarted = false
                dragDownX = event.x
                dragDownY = event.y
                dragStartDxPx = dpToPx(symbol.dxDp)
                dragStartDyPx = dpToPx(symbol.dyDp)
                selectedSymbolId = symbolId
                listener?.onLineSelected(currentLine.id, editText.selectionStart.coerceAtLeast(0))
                listener?.onSymbolSelected(currentLine.id, symbolId)
                overlayView.invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val symbolId = draggingSymbolId ?: return false
                val symbol = currentLine.symbols.firstOrNull { it.id == symbolId } ?: return false
                val deltaX = event.x - dragDownX
                val deltaY = event.y - dragDownY

                if (!dragStarted) {
                    val threshold = dp(3f).toFloat()
                    if (abs(deltaX) > threshold || abs(deltaY) > threshold) {
                        dragStarted = true
                    }
                }

                if (!dragStarted) {
                    return true
                }

                val movedDxDp = pxToDp(dragStartDxPx + deltaX)
                val movedDyDp = pxToDp(dragStartDyPx + deltaY)

                line = currentLine.copy(
                    symbols = currentLine.symbols.map { currentSymbol ->
                        if (currentSymbol.id == symbol.id) {
                            currentSymbol.copy(dxDp = movedDxDp, dyDp = movedDyDp)
                        } else {
                            currentSymbol
                        }
                    }
                )
                overlayView.invalidate()
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                val symbolId = draggingSymbolId
                val moved = dragStarted
                draggingSymbolId = null
                dragStarted = false

                if (symbolId == null) {
                    return false
                }

                if (moved) {
                    val movedSymbol = line?.symbols?.firstOrNull { it.id == symbolId }
                    if (movedSymbol != null) {
                        listener?.onSymbolMoveCommitted(
                            currentLine.id,
                            symbolId,
                            movedSymbol.dxDp,
                            movedSymbol.dyDp
                        )
                    }
                }
                overlayView.invalidate()
                return true
            }
        }
        return false
    }

    private fun findHitSymbol(x: Float, y: Float): String? {
        return symbolBounds.entries.firstOrNull { (_, rect) -> rect.contains(x, y) }?.key
    }

    private fun drawSymbols(canvas: Canvas) {
        symbolBounds.clear()
        val currentLine = line ?: return
        val layout = editText.layout ?: return

        val textLength = currentLine.lyrics.length

        currentLine.symbols.forEach { symbol ->
            val offset = symbol.charIndex.coerceIn(0, textLength)
            val safeOffset = when {
                offset < textLength -> offset
                textLength > 0 -> textLength - 1
                else -> 0
            }
            val lineIndex = layout.getLineForOffset(safeOffset)

            val anchorX = if (offset >= textLength) {
                layout.getLineRight(lineIndex)
            } else {
                layout.getPrimaryHorizontal(offset)
            }
            val baseline = layout.getLineBaseline(lineIndex).toFloat()

            val drawX = anchorX + editText.totalPaddingLeft - editText.scrollX + dpToPx(symbol.dxDp)
            val drawY = baseline + editText.totalPaddingTop - editText.scrollY + dpToPx(symbol.dyDp)

            val textSize = spToPx(32f) * symbol.scale.coerceAtLeast(0.4f)
            symbolPaint.textSize = textSize
            symbolPaint.typeface = symbolTypefaces[symbol.symbolFontId] ?: Typeface.DEFAULT
            val text = symbol.symbolText
            val width = symbolPaint.measureText(text)
            val bounds = RectF(
                drawX - dp(6f),
                drawY - textSize - dp(4f),
                drawX + width + dp(6f),
                drawY + dp(6f)
            )

            if (symbol.id == selectedSymbolId) {
                canvas.drawRoundRect(bounds, dp(6f).toFloat(), dp(6f).toFloat(), symbolSelectionPaint)
            }
            canvas.drawText(text, drawX, drawY, symbolPaint)
            symbolBounds[symbol.id] = bounds
        }
    }

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }

    private fun dpToPx(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        )
    }

    private fun pxToDp(value: Float): Float {
        val density = resources.displayMetrics.density
        return if (density == 0f) value else value / density
    }

    private fun spToPx(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            value,
            resources.displayMetrics
        )
    }

    private class CursorAwareEditText(context: Context) : AppCompatEditText(context) {
        var onSelectionChangedListener: ((Int) -> Unit)? = null

        override fun onSelectionChanged(selStart: Int, selEnd: Int) {
            super.onSelectionChanged(selStart, selEnd)
            onSelectionChangedListener?.invoke(selStart)
        }
    }
}
