package nz.mega.documentscanner.data

import android.graphics.PointF

open class CropResult(
    val width: Int,
    val height: Int,
    val cropPoints: List<PointF>
)
