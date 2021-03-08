package nz.mega.documentscanner.utils

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.Display
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.camera.core.AspectRatio
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.ChipGroup
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

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    fun EditText.selectLastCharacter() {
        requestFocus()
        setSelection(text?.length ?: 0)
        showSoftKeyboard()
    }

    fun View.showSoftKeyboard() {
        if (requestFocus()) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun ChipGroup.setChildrenEnabled(enable: Boolean) {
        children.forEach { it.isEnabled = enable }
    }
}
