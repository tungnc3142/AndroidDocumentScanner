package nz.mega.documentscanner.data

import android.graphics.PointF
import androidx.recyclerview.widget.DiffUtil

data class Page constructor(
    val id: Long = System.currentTimeMillis(),
    val originalImage: Image,
    var croppedImage: Image? = null,
    var cropPoints: List<PointF>? = null
) {

    fun getImageToPrint(): Image =
        croppedImage ?: originalImage

    class ItemDiffUtil : DiffUtil.ItemCallback<Page>() {

        override fun areItemsTheSame(oldItem: Page, newItem: Page): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Page, newItem: Page): Boolean =
            oldItem == newItem
    }
}
