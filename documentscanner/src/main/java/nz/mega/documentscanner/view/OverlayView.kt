package nz.mega.documentscanner.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import nz.mega.documentscanner.R

class OverlayView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.secondaryColor)
        strokeWidth = resources.getDimension(R.dimen.scan_overlay_stroke_width)
    }

    var lines: FloatArray? = null
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        lines?.let { canvas.drawLines(it, paint) }
    }
}
