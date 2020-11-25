package nz.mega.documentscanner.utils

import android.util.Pair
import androidx.core.net.toFile
import com.facebook.imageutils.BitmapUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.utils.FileUtils.deleteSafely

object PageUtils {

    const val PAGE_ROTATION_DEGREES = 90

    suspend fun Page.delete() {
        originalImageUri.toFile().deleteSafely()
        cropMat?.release()
    }

    suspend fun Page.deleteTransformImage() {
        transformImageUri?.toFile()?.deleteSafely()
    }

    suspend fun Page.getOriginalDimensions(): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            BitmapUtil.decodeDimensions(originalImageUri)!!
        }

    fun Page.deleteCropMat() {
        cropMat?.release()
    }
}
