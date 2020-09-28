package nz.mega.documentscanner.utils

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nz.mega.documentscanner.data.BitmapCropResult
import nz.mega.documentscanner.openCV.NativeClass
import nz.mega.documentscanner.utils.OpenCvUtils.yuvToRgba
import org.opencv.core.Mat

class ImageScanner {

    private val nativeClass: NativeClass by lazy { NativeClass() }

    suspend fun getCroppedImage(bitmap: Bitmap, providedPoints: List<PointF>? = null): BitmapCropResult? =
        withContext(Dispatchers.IO) {
            val cropPoints = providedPoints ?: nativeClass.getPoint(bitmap)?.toArray()
                ?.map { PointF(it.x.toFloat(), it.y.toFloat()) }

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

    suspend fun getCropLines(imageProxy: ImageProxy, maxWidth: Int, maxHeight: Int): FloatArray? =
        withContext(Dispatchers.IO) {
            var array: FloatArray? = null
            val mat: Mat = imageProxy.yuvToRgba()
            val ratioX = maxWidth.toFloat() / mat.width()
            val ratioY = maxHeight.toFloat() / mat.height()

            nativeClass.getPoint(mat)?.toArray()
                ?.map { point ->
                    PointF(point.x.toFloat() * ratioX, point.y.toFloat() * ratioY)
                }
                ?.let { points ->
                    array = floatArrayOf(
                        points[0].x,
                        points[0].y,
                        points[1].x,
                        points[1].y,
                        points[1].x,
                        points[1].y,
                        points[2].x,
                        points[2].y,
                        points[2].x,
                        points[2].y,
                        points[3].x,
                        points[3].y,
                        points[3].x,
                        points[3].y,
                        points[0].x,
                        points[0].y,
                    )
                }

            mat.release()

            array
        }
}
