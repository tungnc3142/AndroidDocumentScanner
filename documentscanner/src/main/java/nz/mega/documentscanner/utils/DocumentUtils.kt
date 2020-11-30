package nz.mega.documentscanner.utils

import nz.mega.documentscanner.data.Document
import nz.mega.documentscanner.utils.PageUtils.delete

object DocumentUtils {

    suspend fun Document.deletePage(position: Int): Boolean? =
        pages.getOrNull(position)?.let { page ->
            page.delete()
            pages.remove(page)
        }

    suspend fun Document.deletePages() {
        pages.forEach { it.delete() }
        pages.clear()
    }
}
