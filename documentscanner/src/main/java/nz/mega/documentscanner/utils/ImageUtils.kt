package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nz.mega.documentscanner.data.Image
import nz.mega.documentscanner.utils.BitmapUtils.toFile

object ImageUtils {

    suspend fun createImageFromBitmap(context: Context, bitmap: Bitmap): Image =
        withContext(Dispatchers.IO) {
            val file = FileUtils.createPageFile(context).apply {
                bitmap.toFile(this)
            }

            Image(
                imageUri = file.toUri(),
                width = bitmap.width.toFloat(),
                height = bitmap.height.toFloat()
            )
        }

    suspend fun Image.rotate(context: Context, degreesToRotate: Int = 90): Image =
        withContext(Dispatchers.Default) {
            val bitmap = BitmapUtils.getBitmapFromUri(
                context = context,
                uri = imageUri,
                degreesToRotate = degreesToRotate
            )

            val rotatedImage = createImageFromBitmap(context, bitmap)
            bitmap.recycle()

            return@withContext rotatedImage
        }

    suspend fun Image.deleteFile(): Boolean =
        withContext(Dispatchers.IO) {
            imageUri.toFile().delete()
        }
}
