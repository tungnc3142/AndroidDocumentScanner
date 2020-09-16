package nz.mega.documentscanner.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.viewpager2.widget.ViewPager2

object ViewUtils {

    fun View.isRtl(): Boolean =
        ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL

    fun ViewPager2.scrollToLastPosition() {
        val lastPosition = adapter?.itemCount?.minus(1) ?: 0
        if (currentItem != lastPosition) {
            currentItem = lastPosition
        }
    }
}
