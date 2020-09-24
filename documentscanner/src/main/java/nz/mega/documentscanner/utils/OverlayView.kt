package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
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

    fun setLines(linePoints: List<PointF>) {
        points = linePoints
        invalidate()
    }

    fun setLines(linePoints: List<PointF>, maxWidth: Int, maxHeight: Int) {
        val viewWidth = measuredWidth.toFloat()
        val viewHeight = measuredHeight.toFloat()

        val ratioX = viewWidth / maxWidth // Not OK
        val ratioY = viewHeight / maxHeight

        setLines(linePoints.map { point ->
            PointF(point.x * ratioX, point.y * ratioY)
        })
    }
}
