package nz.mega.documentscanner.scan

import androidx.recyclerview.widget.RecyclerView
import nz.mega.documentscanner.data.PageItem
import nz.mega.documentscanner.databinding.ItemScanBinding

class ScanPagerViewHolder(
    private val binding: ItemScanBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: PageItem) {
        binding.imgItem.setImageBitmap(item.image)
    }
}
