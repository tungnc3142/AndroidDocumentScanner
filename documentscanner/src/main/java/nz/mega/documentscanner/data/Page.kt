package nz.mega.documentscanner.data

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil
import org.opencv.core.MatOfPoint2f

data class Page constructor(
    val id: Long = System.currentTimeMillis(),
    val originalImageUri: Uri,
    var transformImageUri: Uri?,
    var cropMat: MatOfPoint2f?
) {

    class ItemDiffUtil : DiffUtil.ItemCallback<Page>() {

        override fun areItemsTheSame(oldItem: Page, newItem: Page): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Page, newItem: Page): Boolean =
            oldItem == newItem
    }
}
