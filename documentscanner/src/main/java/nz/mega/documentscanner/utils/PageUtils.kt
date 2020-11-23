package nz.mega.documentscanner.utils

import android.graphics.Bitmap
import androidx.core.net.toFile
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.openCV.ImageScanner
import nz.mega.documentscanner.utils.BitmapUtils.rotate
import nz.mega.documentscanner.utils.FileUtils.deleteSafely

object PageUtils {

    const val PAGE_ROTATION_DEGREES = 90

    suspend fun Page.getCroppedBitmap(quality: Int = 100): Bitmap {
        val currentBitmap = BitmapUtils.getBitmapFromUri(
            imageUri = imageUri,
            quality = quality
        )

        val resultBitmap = if (cropMat != null) {
            val croppedBitmap = ImageScanner.getCroppedBitmap(currentBitmap, cropMat!!)
            currentBitmap.recycle()
            croppedBitmap
        } else {
            currentBitmap
        }

        return resultBitmap.rotate(rotation)
    }

    suspend fun Page.delete() {
        imageUri.toFile().deleteSafely()
        cropMat?.release()
    }
}
