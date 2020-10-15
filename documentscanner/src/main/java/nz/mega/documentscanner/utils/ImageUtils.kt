package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toFile
import androidx.core.net.toUri
import nz.mega.documentscanner.data.Image
import nz.mega.documentscanner.utils.FileUtils.deleteSafely
import nz.mega.documentscanner.utils.FileUtils.toFile

object ImageUtils {

    suspend fun createImageFromBitmap(context: Context, bitmap: Bitmap): Image {
        val file = FileUtils.createPageFile(context).apply {
            bitmap.toFile(this)
        }

        return Image(
            imageUri = file.toUri(),
            width = bitmap.width.toFloat(),
            height = bitmap.height.toFloat()
        )
    }

    suspend fun Image.rotate(context: Context, degreesToRotate: Int = 90): Image {
        val bitmap = BitmapUtils.getBitmapFromUri(
            context = context,
            uri = imageUri,
            degreesToRotate = degreesToRotate
        )

        val rotatedImage = createImageFromBitmap(context, bitmap)
        bitmap.recycle()

        return rotatedImage
    }

    suspend fun Image.deleteFile(): Boolean =
        imageUri.toFile().deleteSafely()
}
