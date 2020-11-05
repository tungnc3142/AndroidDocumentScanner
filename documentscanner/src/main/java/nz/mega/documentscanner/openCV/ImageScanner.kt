package nz.mega.documentscanner.openCV

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nz.mega.documentscanner.openCV.OpenCvUtils.crop
import nz.mega.documentscanner.openCV.OpenCvUtils.yuvToRgbaMat
import nz.mega.documentscanner.utils.BitmapUtils.toMat
import org.opencv.android.OpenCVLoader
import org.opencv.core.MatOfPoint2f

object ImageScanner {

    suspend fun init(): Boolean =
        withContext(Dispatchers.IO) {
            OpenCVLoader.initDebug()
        }

    suspend fun getCropPoints(bitmap: Bitmap): MatOfPoint2f? =
        withContext(Dispatchers.Default) {
            val bitmapMat = bitmap.toMat()
            CropDetector.detect(bitmapMat)
        }

    suspend fun getCroppedBitmap(bitmap: Bitmap, cropMat: MatOfPoint2f): Bitmap =
        withContext(Dispatchers.Default) {
            val bitmapMat = bitmap.toMat()
            bitmapMat.crop(cropMat)
        }

    suspend fun getCropLines(
        imageProxy: ImageProxy,
        maxWidth: Float,
        maxHeight: Float
    ): FloatArray? = withContext(Dispatchers.Default) {
        var resultArray: FloatArray? = null
        val imageMat = imageProxy.yuvToRgbaMat()
        val ratioX = maxWidth / imageMat.width()
        val ratioY = maxHeight / imageMat.height()
        val matOfPoints = CropDetector.detect(imageMat)

        if (matOfPoints != null) {
            val points = matOfPoints.toArray().map { point ->
                PointF(point.x.toFloat() * ratioX, point.y.toFloat() * ratioY)
            }

            resultArray = floatArrayOf(
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

        matOfPoints?.release()
        imageMat.release()

        resultArray
    }
}
