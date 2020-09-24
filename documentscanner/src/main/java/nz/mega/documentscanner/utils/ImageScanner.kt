package nz.mega.documentscanner.utils

import android.graphics.Bitmap
import android.graphics.PointF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nz.mega.documentscanner.data.BitmapCropResult
import nz.mega.documentscanner.data.CropResult
import nz.mega.documentscanner.openCV.NativeClass

class ImageScanner {

    private val nativeClass: NativeClass by lazy { NativeClass() }

    suspend fun getCropPoints(bitmap: Bitmap): CropResult? =
        withContext(Dispatchers.IO) {
            val cropPoints = calculateCropPoints(bitmap)

            if (!cropPoints.isNullOrEmpty()) {
                CropResult(
                    bitmap.width,
                    bitmap.height,
                    cropPoints
                )
            } else {
                null
            }
        }

    suspend fun getCroppedImage(bitmap: Bitmap, providedPoints: List<PointF>? = null): BitmapCropResult? =
        withContext(Dispatchers.IO) {
            val cropPoints = providedPoints ?: calculateCropPoints(bitmap)

            if (!cropPoints.isNullOrEmpty()) {
                val croppedBitmap = nativeClass.getScannedBitmap(
                    bitmap,
                    cropPoints[0].x,
                    cropPoints[0].y,
                    cropPoints[1].x,
                    cropPoints[1].y,
                    cropPoints[2].x,
                    cropPoints[2].y,
                    cropPoints[3].x,
                    cropPoints[3].y,
                )

                BitmapCropResult(
                    croppedBitmap,
                    bitmap.width,
                    bitmap.height,
                    cropPoints
                )
            } else {
                null
            }
        }

    private fun calculateCropPoints(bitmap: Bitmap): List<PointF>? =
        nativeClass.getPoint(bitmap)?.toArray()?.map { PointF(it.x.toFloat(), it.y.toFloat()) }
}
