package nz.mega.documentscanner.utils

import nz.mega.documentscanner.data.Document
import nz.mega.documentscanner.utils.PageUtils.deleteFiles

object DocumentUtils {

    suspend fun Document.deletePage(pagePosition: Int): Boolean? =
        pages.getOrNull(pagePosition)?.let { page ->
            page.deleteFiles()
            pages.remove(page)
        }

    suspend fun Document.deleteAllPages() {
        pages.forEach { it.deleteFiles() }
        pages.clear()
    }
}
