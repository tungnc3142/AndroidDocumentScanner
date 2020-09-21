package nz.mega.documentscanner.scan

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.databinding.ItemScanBinding

class ScanPagerViewHolder(
    private val binding: ItemScanBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: Page) {
        val imageUri = item.croppedImageUri ?: item.originalImageUri
        Glide.with(itemView)
            .load(imageUri)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(binding.imgItem)
    }
}
