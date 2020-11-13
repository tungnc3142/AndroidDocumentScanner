package nz.mega.documentscanner.utils

import android.content.Context
import nz.mega.documentscanner.data.Document
import nz.mega.documentscanner.data.PageItem
import nz.mega.documentscanner.utils.PageUtils.delete
import nz.mega.documentscanner.utils.PageUtils.getCroppedBitmap

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

    suspend fun Document.toPageItems(context: Context): List<PageItem> =
        pages.map { page ->
            PageItem(
                page.id,
                page.getCroppedBitmap(context)
            )
        }
}
