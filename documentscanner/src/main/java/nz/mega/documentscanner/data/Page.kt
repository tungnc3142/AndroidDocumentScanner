package nz.mega.documentscanner.data

import android.net.Uri
import nz.mega.documentscanner.utils.PageUtils.PAGE_ROTATION_DEGREES
import org.opencv.core.MatOfPoint2f

data class Page constructor(
    val id: Long = System.currentTimeMillis(),
    val width: Int,
    val height: Int,
    val imageUri: Uri,
    var cropMat: MatOfPoint2f?,
    var rotation: Int = 0
) {
    fun rotate(): Page {
        if (rotation + PAGE_ROTATION_DEGREES >= PAGE_ROTATION_DEGREES * 4) {
            rotation = 0
        } else {
            rotation += PAGE_ROTATION_DEGREES
        }
        return this
    }
}
