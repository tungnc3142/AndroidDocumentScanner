package nz.mega.documentscanner.utils

import nz.mega.documentscanner.data.Document
import nz.mega.documentscanner.utils.ImageUtils.deleteFile

object DocumentUtils {

    suspend fun Document.deletePage(pagePosition: Int): Boolean? =
        pages.getOrNull(pagePosition)?.let { page ->
            page.originalImage.deleteFile()
            page.croppedImage?.deleteFile()
            pages.remove(page)
        }

    suspend fun Document.deleteAllPages() {
        pages.forEach { page ->
            page.originalImage.deleteFile()
            page.croppedImage?.deleteFile()
        }
        pages.clear()
    }
}
