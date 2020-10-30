package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toFile
import androidx.core.net.toUri
import nz.mega.documentscanner.data.Image
import nz.mega.documentscanner.utils.FileUtils.deleteSafely
import nz.mega.documentscanner.utils.FileUtils.toFile

object ImageUtils {

    suspend fun Bitmap.toImage(context: Context): Image {
        val file = FileUtils.createPageFile(context).apply {
            toFile(this)
        }

        return Image(
            imageUri = file.toUri(),
            width = width.toFloat(),
            height = height.toFloat()
        )
    }

    suspend fun Image.rotate(context: Context, degreesToRotate: Int = 90): Image {
        val bitmap = BitmapUtils.getBitmapFromUri(
            context = context,
            uri = imageUri,
            degreesToRotate = degreesToRotate
        )

        val rotatedImage = bitmap.toImage(context)

        bitmap.recycle()
        return rotatedImage
    }

    suspend fun Image.deleteFile(): Boolean =
        imageUri.toFile().deleteSafely()
}
