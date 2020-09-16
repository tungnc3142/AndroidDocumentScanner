package nz.mega.documentscanner.utils

import android.animation.ObjectAnimator
import androidx.core.view.postDelayed
import nz.mega.documentscanner.databinding.FragmentCameraBinding
import com.google.android.material.snackbar.Snackbar

object AnimationUtils {

    private const val ANIMATION_TIME = 100L
    private const val SNACKBAR_DELAY_TIME = 1000L

    fun FragmentCameraBinding.animateCaptureButton(height: Float) {
        ObjectAnimator.ofFloat(
            btnCapture,
            "translationY",
            height
        ).apply { duration = ANIMATION_TIME }.start()

        ObjectAnimator.ofFloat(
            progress,
            "translationY",
            height
        ).apply { duration = ANIMATION_TIME }.start()
    }

    fun Snackbar.dismissAndShow() {
        if (isShownOrQueued) {
            dismiss()
            view.postDelayed(SNACKBAR_DELAY_TIME) { show() }
        } else {
            show()
        }
    }
}
