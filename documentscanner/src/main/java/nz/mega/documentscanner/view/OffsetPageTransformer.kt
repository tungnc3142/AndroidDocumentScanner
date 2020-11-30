package nz.mega.documentscanner.view

import android.view.View
import androidx.annotation.Px
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import nz.mega.documentscanner.utils.ViewUtils.isRtl

/**
 * Took from https://proandroiddev.com/look-deep-into-viewpager2-13eb8e06e419
 */
class OffsetPageTransformer(
    @Px private val pageOffsetPx: Int,
    @Px private val pageMarginPx: Int
) : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val viewPager = requireViewPager(page)
        val offset = position * -(2 * pageOffsetPx + pageMarginPx)

        if (viewPager.orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
            page.translationX = if (viewPager.isRtl()) {
                -offset
            } else {
                offset
            }
        } else {
            page.translationY = offset
        }
    }

    private fun requireViewPager(page: View): ViewPager2 {
        val parent = page.parent
        val parentParent = parent.parent
        if (parent is RecyclerView && parentParent is ViewPager2) {
            return parentParent
        }
        throw IllegalStateException(
            "Expected the page view to be managed by a ViewPager2 instance."
        )
    }
}
