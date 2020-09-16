package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.AspectRatio
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

    private var points: List<PointF>? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        points?.forEachIndexed { index, point ->
            if (index < points!!.size - 1) {
                val nextPoint = index + 1
                canvas.drawLine(point.x, point.y, points!![nextPoint].x, points!![nextPoint].y, paint)
            } else {
                canvas.drawLine(point.x, point.y, points!![0].x, points!![0].y, paint)
            }
        }
    }

    fun setLines(
        points: List<PointF>?,
        maxWidth: Int,
        maxHeight: Int,
        @AspectRatio.Ratio aspectRatio: Int? = null
    ) {
        var calculatedWidth = measuredWidth.toFloat()
        var calculatedHeight = measuredHeight.toFloat()

        val ratio = when (aspectRatio) {
            AspectRatio.RATIO_16_9 -> 16f / 9f
            AspectRatio.RATIO_4_3 -> 4f / 3f
            else -> null
        }

        if (ratio != null) {
            if (measuredHeight > measuredWidth) {
                calculatedWidth = calculatedHeight / ratio
            } else {
                calculatedHeight = calculatedWidth / ratio
            }
        }

        val xFactor = calculatedWidth / maxWidth
        val yFactor = calculatedHeight / maxHeight

        this.points = points?.map { point ->
            point.apply { set(point.x * xFactor, point.y * yFactor) }
        }

        invalidate()
    }
}
