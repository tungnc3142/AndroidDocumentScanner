package nz.mega.documentscanner.data

import android.graphics.Bitmap
import androidx.recyclerview.widget.DiffUtil

data class PageItem constructor(
    val id: Long,
    val image: Bitmap
) {

    class ItemDiffUtil : DiffUtil.ItemCallback<PageItem>() {

        override fun areItemsTheSame(oldItem: PageItem, newItem: PageItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PageItem, newItem: PageItem): Boolean =
            oldItem == newItem
    }
}
