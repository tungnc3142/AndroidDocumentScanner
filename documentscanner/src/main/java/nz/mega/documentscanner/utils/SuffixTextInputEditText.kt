package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText

/**
 * [TextInputEditText] with suffix support.
 *
 * Inspired by https://github.com/tobiasschuerg/android-prefix-suffix-edit-text/
 */
class SuffixTextInputEditText constructor(
    context: Context,
    attrs: AttributeSet
) : TextInputEditText(context, attrs) {

    // These are used to store details obtained from the EditText's rendering process
    private val firstLineBounds = Rect()
    private var isInitialized = false

    private val textPaint: TextPaint by lazy {
        TextPaint().apply {
            color = currentHintTextColor
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
            this.typeface = typeface
        }
    }

    var suffix: String? = null
        set(value) {
            field = value
            invalidate()
        }

    init {
        textPaint.textSize = textSize

        isInitialized = true
    }

    override fun setTypeface(typeface: Typeface?) {
        super.setTypeface(typeface)

        if (typeface != null && isInitialized) {
            // this is first called from the constructor when it's not initialized, yet
            textPaint.typeface = typeface
            postInvalidate()
        }
    }

    public override fun onDraw(c: Canvas) {
        textPaint.color = currentHintTextColor

        getLineBounds(0, firstLineBounds)

        super.onDraw(c)

        // Now we can calculate what we need!
        val text = text.toString()
        val textWidth = if (text.isNotEmpty()) {
            textPaint.measureText(text) + compoundPaddingStart
        } else if (!hint.isNullOrBlank()) {
            textPaint.measureText(hint?.toString()) + compoundPaddingStart
        } else {
            compoundPaddingStart.toFloat()
        }

        suffix?.let {
            // We need to draw this like this because
            // setting a right drawable doesn't work properly and we want this
            // just after the text we are editing (but untouchable)
            val y2 = firstLineBounds.bottom - textPaint.descent()
            c.drawText(it, textWidth, y2, textPaint)
        }
    }
}
