package nz.mega.documentscanner.data

import android.graphics.Bitmap
import android.graphics.PointF

class BitmapCropResult(
    val bitmap: Bitmap,
    width: Int,
    height: Int,
    cropPoints: List<PointF>
) : CropResult(width, height, cropPoints)
