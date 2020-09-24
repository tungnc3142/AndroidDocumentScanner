package nz.mega.documentscanner.utils

import android.graphics.PointF
import android.util.DisplayMetrics
import android.view.Display
import android.view.View
import androidx.camera.core.AspectRatio
import androidx.core.view.ViewCompat
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ViewUtils {
    private const val RATIO_4_3_VALUE = 4.0 / 3.0
    private const val RATIO_16_9_VALUE = 16.0 / 9.0

    fun View.isRtl(): Boolean =
        ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL

    fun ViewPager2.scrollToLastPosition() {
        val lastPosition = adapter?.itemCount?.minus(1) ?: 0
        if (currentItem != lastPosition) {
            currentItem = lastPosition
        }
    }

    fun View.getContourPoints(): List<PointF> =
        listOf(
            PointF(0f, 0f),
            PointF(measuredWidth.toFloat(), 0f),
            PointF(0f, measuredHeight.toFloat()),
            PointF(measuredWidth.toFloat(), measuredHeight.toFloat())
        )

    @AspectRatio.Ratio
    fun Display.aspectRatio(): Int {
        val metrics = DisplayMetrics().also(::getRealMetrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }
}
