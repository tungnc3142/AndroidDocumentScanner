package nz.mega.documentscanner.openCV

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Magnifier
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import nz.mega.documentscanner.R
import java.util.ArrayList
import java.util.HashMap
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility", "NewApi")
class PolygonView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val paint: Paint
    private val pointer1 = getImageView(0, 0)
    private val pointer2 = getImageView(width, 0)
    private val pointer3 = getImageView(0, height)
    private val pointer4 = getImageView(width, height)
    private val midPointer13 = getImageView(0, height / 2)
    private val midPointer12 = getImageView(0, width / 2)
    private val midPointer34 = getImageView(0, height / 2)
    private val midPointer24 = getImageView(0, height / 2)
    private var magnifier: Magnifier? = null

    init {
        midPointer13.setOnTouchListener(MidPointTouchListenerImpl(pointer1, pointer3))
        midPointer12.setOnTouchListener(MidPointTouchListenerImpl(pointer1, pointer2))
        midPointer34.setOnTouchListener(MidPointTouchListenerImpl(pointer3, pointer4))
        midPointer24.setOnTouchListener(MidPointTouchListenerImpl(pointer2, pointer4))
        addView(pointer1)
        addView(pointer2)
        addView(midPointer13)
        addView(midPointer12)
        addView(midPointer34)
        addView(midPointer24)
        addView(pointer3)
        addView(pointer4)

        paint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.secondaryColor)
            strokeWidth = resources.getDimension(R.dimen.polygon_line_width)
            isAntiAlias = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val radius = resources.getDimension(R.dimen.polygon_magnifier_radius)
            val size = resources.getDimensionPixelSize(R.dimen.polygon_magnifier_size)
            val overlay = ResourcesCompat.getDrawable(resources, R.drawable.ic_docscanner_highlight, null)

            magnifier = Magnifier.Builder(this)
                .setSize(size, size)
                .setCornerRadius(radius)
                .setOverlay(overlay)
                .build()
        }
    }

    fun getPoints(): Map<Int, PointF> {
        val points: MutableList<PointF> = ArrayList()
        points.add(PointF(pointer1.x, pointer1.y))
        points.add(PointF(pointer2.x, pointer2.y))
        points.add(PointF(pointer3.x, pointer3.y))
        points.add(PointF(pointer4.x, pointer4.y))
        return getOrderedPoints(points)
    }

    fun setPoints(pointFMap: Map<Int, PointF>?) {
        if (pointFMap != null && pointFMap.size == 4) {
            setPointsCoordinates(pointFMap)
        } else {
            setPointsCoordinates(getContourPoints())
        }
    }

    fun getOrderedPoints(points: List<PointF>): Map<Int, PointF> {
        val centerPoint = PointF()
        val size = points.size
        for (pointF in points) {
            centerPoint.x += pointF.x / size
            centerPoint.y += pointF.y / size
        }
        val orderedPoints: MutableMap<Int, PointF> = HashMap()
        for (pointF in points) {
            val index =
                if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                    0
                } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                    1
                } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                    2
                } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                    3
                } else {
                    -1
                }
            orderedPoints[index] = pointF
        }
        return orderedPoints
    }

    private fun getContourPoints(): Map<Int, PointF> {
        val startX = pointer1.measuredWidth.toFloat()
        val startY = pointer1.measuredHeight.toFloat() * 2
        val endX = width - startX * 2
        val endY = height - startY * 2
        val points = listOf(
            PointF(startX, startY),
            PointF(endX, startY),
            PointF(startX, endY),
            PointF(endX, endY)
        )
        return getOrderedPoints(points)
    }

    fun setPointColor(color: Int) {
        paint.color = color
        invalidate()
    }

    private fun setPointsCoordinates(pointFMap: Map<Int, PointF>) {
        pointer1.x = pointFMap[0]!!.x
        pointer1.y = pointFMap[0]!!.y
        pointer2.x = pointFMap[1]!!.x
        pointer2.y = pointFMap[1]!!.y
        pointer3.x = pointFMap[2]!!.x
        pointer3.y = pointFMap[2]!!.y
        pointer4.x = pointFMap[3]!!.x
        pointer4.y = pointFMap[3]!!.y
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        canvas.drawLine(
            pointer1.x + pointer1.width / 2,
            pointer1.y + pointer1.height / 2,
            pointer3.x + pointer3.width / 2,
            pointer3.y + pointer3.height / 2,
            paint
        )
        canvas.drawLine(
            pointer1.x + pointer1.width / 2,
            pointer1.y + pointer1.height / 2,
            pointer2.x + pointer2.width / 2,
            pointer2.y + pointer2.height / 2,
            paint
        )
        canvas.drawLine(
            pointer2.x + pointer2.width / 2,
            pointer2.y + pointer2.height / 2,
            pointer4.x + pointer4.width / 2,
            pointer4.y + pointer4.height / 2,
            paint
        )
        canvas.drawLine(
            pointer3.x + pointer3.width / 2,
            pointer3.y + pointer3.height / 2,
            pointer4.x + pointer4.width / 2,
            pointer4.y + pointer4.height / 2,
            paint
        )
        midPointer13.x = pointer3.x - (pointer3.x - pointer1.x) / 2
        midPointer13.y = pointer3.y - (pointer3.y - pointer1.y) / 2
        midPointer24.x = pointer4.x - (pointer4.x - pointer2.x) / 2
        midPointer24.y = pointer4.y - (pointer4.y - pointer2.y) / 2
        midPointer34.x = pointer4.x - (pointer4.x - pointer3.x) / 2
        midPointer34.y = pointer4.y - (pointer4.y - pointer3.y) / 2
        midPointer12.x = pointer2.x - (pointer2.x - pointer1.x) / 2
        midPointer12.y = pointer2.y - (pointer2.y - pointer1.y) / 2
    }

    private fun drawMag(x: Float, y: Float) {
        magnifier?.show(x, y)
    }

    private fun dismissMag() {
        magnifier?.dismiss()
    }

    private fun getImageView(x: Int, y: Int): ImageView =
        ImageView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(10, 10, 10, 10)
            setImageResource(R.drawable.ic_docscanner_oval)
            setX(x.toFloat())
            setY(y.toFloat())
            setOnTouchListener(TouchListenerImpl())
        }

    fun isValidShape(pointFMap: Map<Int, PointF>?): Boolean =
        pointFMap != null && pointFMap.size == 4

    private inner class MidPointTouchListenerImpl(
        private val mainPointer1: ImageView,
        private val mainPointer2: ImageView
    ) : OnTouchListener {
        var downPT = PointF() // Record Mouse Position When Pressed Down
        var startPT = PointF() // Record Start Position of 'img'
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = PointF(event.x - downPT.x, event.y - downPT.y)
                    if (abs(mainPointer1.x - mainPointer2.x) > abs(mainPointer1.y - mainPointer2.y)) {
                        if (mainPointer2.y + mv.y + v.height < height && mainPointer2.y + mv.y > 0) {
                            v.x = (startPT.y + mv.y)
                            startPT = PointF(v.x, v.y)
                            mainPointer2.y = (mainPointer2.y + mv.y)
                        }
                        if (mainPointer1.y + mv.y + v.height < height && mainPointer1.y + mv.y > 0) {
                            v.x = (startPT.y + mv.y)
                            startPT = PointF(v.x, v.y)
                            mainPointer1.y = (mainPointer1.y + mv.y)
                        }
                    } else {
                        if (mainPointer2.x + mv.x + v.width < width && mainPointer2.x + mv.x > 0) {
                            v.x = (startPT.x + mv.x)
                            startPT = PointF(v.x, v.y)
                            mainPointer2.x = (mainPointer2.x + mv.x)
                        }
                        if (mainPointer1.x + mv.x + v.width < width && mainPointer1.x + mv.x > 0) {
                            v.x = (startPT.x + mv.x)
                            startPT = PointF(v.x, v.y)
                            mainPointer1.x = (mainPointer1.x + mv.x)
                        }
                    }
                    drawMag(startPT.x + 50, startPT.y + 50)
                }
                MotionEvent.ACTION_DOWN -> {
                    downPT.x = event.x
                    downPT.y = event.y
                    startPT = PointF(v.x, v.y)
                }
                MotionEvent.ACTION_UP -> {
                    paint.color = if (isValidShape(getPoints())) {
                        ContextCompat.getColor(context, R.color.secondaryColor)
                    } else {
                        ContextCompat.getColor(context, R.color.errorColor)
                    }
                    dismissMag()
                }
                else -> {
                }
            }
            invalidate()
            return true
        }
    }

    private inner class TouchListenerImpl : OnTouchListener {
        var downPT = PointF() // Record Mouse Position When Pressed Down
        var startPT = PointF() // Record Start Position of 'img'

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = PointF(event.x - downPT.x, event.y - downPT.y)
                    if (startPT.x + mv.x + v.width < width && startPT.y + mv.y + v.height < height && startPT.x + mv.x > 0 && startPT.y + mv.y > 0) {
                        v.x = (startPT.x + mv.x)
                        v.y = (startPT.y + mv.y)
                        startPT = PointF(v.x, v.y)
                        drawMag(startPT.x + 50, startPT.y + 50)
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    downPT.x = event.x
                    downPT.y = event.y
                    startPT = PointF(v.x, v.y)
                }
                MotionEvent.ACTION_UP -> {
                    paint.color = if (isValidShape(getPoints())) {
                        ContextCompat.getColor(context, R.color.secondaryColor)
                    } else {
                        ContextCompat.getColor(context, R.color.errorColor)
                    }
                    dismissMag()
                }
            }
            invalidate()
            return true
        }
    }
}
