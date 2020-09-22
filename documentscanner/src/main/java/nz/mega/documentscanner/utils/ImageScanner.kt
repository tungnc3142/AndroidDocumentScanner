package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nz.mega.documentscanner.openCV.NativeClass
import nz.mega.documentscanner.utils.ImageUtils.toFile

class ImageScanner {

    private val nativeClass: NativeClass by lazy { NativeClass() }

    suspend fun processImage(context: Context, imageUri: Uri, points: List<PointF>? = null): ImageScannerResult =
        withContext(Dispatchers.IO) {
            val bitmap = Glide.with(context)
                .asBitmap()
                .load(imageUri)
                .submit().get()

            val imagePoints = points ?: getCropPoints(bitmap)

            val resultBitmap = nativeClass.getScannedBitmap(
                bitmap,
                imagePoints[0].x,
                imagePoints[0].y,
                imagePoints[1].x,
                imagePoints[1].y,
                imagePoints[2].x,
                imagePoints[2].y,
                imagePoints[3].x,
                imagePoints[3].y,
            )

            val imageFile = FileUtils.createPageFile(context).apply {
                resultBitmap.toFile(this)
            }

            ImageScannerResult(imageFile.toUri(), bitmap.width, bitmap.height, imagePoints)
        }

    suspend fun getCropPoints(bitmap: Bitmap): List<PointF> =
        withContext(Dispatchers.IO) {
            val points = nativeClass.getPoint(bitmap) ?: throw DocumentNotFoundException()

            points.toArray().map { PointF(it.x.toFloat(), it.y.toFloat()) }
        }

    class ImageScannerResult(val imageUri: Uri, val imageWidth: Int, val imageHeight: Int, val points: List<PointF>)

    class DocumentNotFoundException : IllegalStateException("Document not found")
}
