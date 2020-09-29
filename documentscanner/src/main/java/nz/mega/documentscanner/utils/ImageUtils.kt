package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.Rotate
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
        withContext(Dispatchers.IO) {
            val bitmap = getBitmap(context, degreesToRotate)

            createImageFromBitmap(context, bitmap).also {
                bitmap.recycle()
            }
        }

    suspend fun Image.deleteFile(): Boolean =
        withContext(Dispatchers.IO) {
            imageUri.toFile().delete()
        }

    suspend fun Image.getBitmap(context: Context, degreesToRotate: Int? = 90): Bitmap =
        withContext(Dispatchers.IO) {
            val requestBuilder = Glide.with(context)
                .asBitmap()
                .load(imageUri)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)

            degreesToRotate?.let { requestBuilder.transform(Rotate(it)) }

            requestBuilder.submit().get()
        }
}
