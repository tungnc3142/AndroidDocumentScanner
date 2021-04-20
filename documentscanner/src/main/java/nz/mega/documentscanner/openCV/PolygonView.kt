package nz.mega.documentscanner.openCV

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Magnifier
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
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

    companion object {
        private const val TAG = "PolygonView"
        private const val POINT_PADDING = 24
    }

    private val paint: Paint
    private val pointer1 = getPointView(0, 0)
    private val pointer2 = getPointView(width, 0)
    private val pointer3 = getPointView(0, height)
    private val pointer4 = getPointView(width, height)
    private val midPointer13 = getPointView(0, height / 2)
    private val midPointer12 = getPointView(0, width / 2)
    private val midPointer34 = getPointView(0, height / 2)
    private val midPointer24 = getPointView(0, height / 2)
    private var magnifier: Magnifier? = null
    private var validShapeListener: ((Boolean) -> Unit)? = null

    init {
        pointer1.setOnTouchListener(EdgePointTouchListener())
        pointer2.setOnTouchListener(EdgePointTouchListener())
        pointer3.setOnTouchListener(EdgePointTouchListener())
        pointer4.setOnTouchListener(EdgePointTouchListener())
        midPointer13.setOnTouchListener(MidPointTouchListener(pointer1, pointer3))
        midPointer12.setOnTouchListener(MidPointTouchListener(pointer1, pointer2))
        midPointer34.setOnTouchListener(MidPointTouchListener(pointer3, pointer4))
        midPointer24.setOnTouchListener(MidPointTouchListener(pointer2, pointer4))
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

        initMagnifier()
    }

    override fun onDetachedFromWindow() {
        dismissMag()
        super.onDetachedFromWindow()
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

    private fun getPointView(x: Int, y: Int): View =
        ImageView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(POINT_PADDING)
            setImageResource(R.drawable.ic_docscanner_oval)
            setX(x.toFloat())
            setY(y.toFloat())
        }

    private fun initMagnifier() {
        try {
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
        } catch (e: NoClassDefFoundError) {
            Log.w(TAG, "Device doesn't support Magnifier: ${e.stackTraceToString()}")
        }
    }

    private fun isValidShape(pointFMap: Map<Int, PointF>?): Boolean =
        pointFMap != null && pointFMap.size == 4

    fun setValidShapeListener(listener: ((Boolean) -> Unit)?) {
        validShapeListener = listener
    }

    private inner class EdgePointTouchListener : OnTouchListener {
        var downPT = PointF() // Record Mouse Position When Pressed Down
        var startPT = PointF() // Record Start Position of 'img'

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = PointF(event.x - downPT.x, event.y - downPT.y)
                    if (startPT.x + mv.x + view.width < width && startPT.y + mv.y + view.height < height && startPT.x + mv.x > 0 && startPT.y + mv.y > 0) {
                        view.x = (startPT.x + mv.x)
                        view.y = (startPT.y + mv.y)
                        startPT = PointF(view.x, view.y)
                        drawMag(startPT.x + 50, startPT.y + 50)
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    downPT.x = event.x
                    downPT.y = event.y
                    startPT = PointF(view.x, view.y)
                }
                MotionEvent.ACTION_UP -> {
                    val isValidShape = isValidShape(getPoints())

                    validShapeListener?.invoke(isValidShape)
                    paint.color = if (isValidShape) {
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

    private inner class MidPointTouchListener(
        private val mainPointer1: View,
        private val mainPointer2: View
    ) : OnTouchListener {
        var downPT = PointF() // Record Mouse Position When Pressed Down
        var startPT = PointF() // Record Start Position of 'img'
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = PointF(event.x - downPT.x, event.y - downPT.y)
                    if (abs(mainPointer1.x - mainPointer2.x) > abs(mainPointer1.y - mainPointer2.y)) {
                        if (mainPointer2.y + mv.y + view.height < height && mainPointer2.y + mv.y > 0) {
                            view.x = (startPT.y + mv.y)
                            startPT = PointF(view.x, view.y)
                            mainPointer2.y = (mainPointer2.y + mv.y)
                        }
                        if (mainPointer1.y + mv.y + view.height < height && mainPointer1.y + mv.y > 0) {
                            view.x = (startPT.y + mv.y)
                            startPT = PointF(view.x, view.y)
                            mainPointer1.y = (mainPointer1.y + mv.y)
                        }
                    } else {
                        if (mainPointer2.x + mv.x + view.width < width && mainPointer2.x + mv.x > 0) {
                            view.x = (startPT.x + mv.x)
                            startPT = PointF(view.x, view.y)
                            mainPointer2.x = (mainPointer2.x + mv.x)
                        }
                        if (mainPointer1.x + mv.x + view.width < width && mainPointer1.x + mv.x > 0) {
                            view.x = (startPT.x + mv.x)
                            startPT = PointF(view.x, view.y)
                            mainPointer1.x = (mainPointer1.x + mv.x)
                        }
                    }

                    val magX = mainPointer2.x - (mainPointer2.x - mainPointer1.x) / 2
                    val magY = mainPointer2.y - (mainPointer2.y - mainPointer1.y) / 2
                    drawMag(magX, magY)
                }
                MotionEvent.ACTION_DOWN -> {
                    downPT.x = event.x
                    downPT.y = event.y
                    startPT = PointF(view.x, view.y)
                }
                MotionEvent.ACTION_UP -> {
                    val isValidShape = isValidShape(getPoints())

                    validShapeListener?.invoke(isValidShape)
                    paint.color = if (isValidShape) {
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
