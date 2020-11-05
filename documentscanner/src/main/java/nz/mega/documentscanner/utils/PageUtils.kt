package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toFile
import androidx.core.net.toUri
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.openCV.ImageScanner
import nz.mega.documentscanner.openCV.OpenCvUtils.rotate
import nz.mega.documentscanner.utils.FileUtils.deleteSafely
import org.opencv.core.MatOfPoint2f

object PageUtils {

    suspend fun Page.getCroppedBitmap(context: Context): Bitmap {
        val currentBitmap = BitmapUtils.getBitmapFromUri(
            context = context,
            uri = imageUri
        )

        return if (cropMat != null) {
            val croppedBitmap = ImageScanner.getCroppedBitmap(currentBitmap, cropMat!!)
            currentBitmap.recycle()
            croppedBitmap
        } else {
            currentBitmap
        }
    }

    suspend fun Page.rotate(context: Context, degreesToRotate: Int = 90): Page {
        val bitmap = BitmapUtils.getBitmapFromUri(
            context = context,
            uri = imageUri,
            degreesToRotate = degreesToRotate
        )

        val rotatedFile = FileUtils.createPageFile(context, bitmap)
        val rotatedCropMat = cropMat?.rotate()

        imageUri.toFile().deleteSafely()
        cropMat?.release()
        bitmap.recycle()

        return copy(
            imageUri = rotatedFile.toUri(),
            cropMat = rotatedCropMat
        )
    }

    suspend fun Page.crop(context: Context, cropMat: MatOfPoint2f): Page {
        val currentBitmap = BitmapUtils.getBitmapFromUri(
            context = context,
            uri = imageUri
        )

        val croppedBitmap = ImageScanner.getCroppedBitmap(currentBitmap, cropMat)
        val croppedFile = FileUtils.createPageFile(context, croppedBitmap)

        imageUri.toFile().deleteSafely()
        currentBitmap.recycle()
        croppedBitmap.recycle()

        return copy(
            imageUri = croppedFile.toUri(),
            cropMat = cropMat
        )
    }
}
