package nz.mega.documentscanner.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nz.mega.documentscanner.data.Document
import nz.mega.documentscanner.utils.ImageUtils.deleteFile

object DocumentUtils {

    suspend fun Document.deletePage(pagePosition: Int): Boolean? =
        withContext(Dispatchers.IO) {
            pages.getOrNull(pagePosition)?.let { page ->
                page.originalImage.deleteFile()
                page.croppedImage?.deleteFile()
                pages.remove(page)
            }
        }

    suspend fun Document.deleteAllPages() {
        withContext(Dispatchers.IO) {
            pages.forEach { page ->
                page.originalImage.deleteFile()
                page.croppedImage?.deleteFile()
            }
            pages.clear()
        }
    }
}
