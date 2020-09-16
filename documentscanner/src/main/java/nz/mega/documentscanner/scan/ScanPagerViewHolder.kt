package nz.mega.documentscanner.scan

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import nz.mega.documentscanner.data.ScanDocument
import nz.mega.documentscanner.databinding.LayoutScanItemBinding

class ScanPagerViewHolder(
    private val binding: LayoutScanItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ScanDocument) {
        val imageUri = item.croppedImageUri ?: item.originalImageUri
        Glide.with(itemView)
            .load(imageUri)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(binding.imgItem)
    }
}
