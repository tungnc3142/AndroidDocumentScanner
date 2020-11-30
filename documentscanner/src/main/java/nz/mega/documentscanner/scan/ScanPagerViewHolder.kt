package nz.mega.documentscanner.scan

import androidx.recyclerview.widget.RecyclerView
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.databinding.ItemScanBinding

class ScanPagerViewHolder(
    private val binding: ItemScanBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: Page) {
        val imageUri = item.transformImageUri ?: item.originalImageUri
        binding.imgItem.setImageURI(imageUri)
    }
}
