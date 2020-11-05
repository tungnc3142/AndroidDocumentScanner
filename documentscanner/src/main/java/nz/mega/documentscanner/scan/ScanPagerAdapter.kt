package nz.mega.documentscanner.scan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import nz.mega.documentscanner.data.PageItem
import nz.mega.documentscanner.databinding.ItemScanBinding

class ScanPagerAdapter : ListAdapter<PageItem, ScanPagerViewHolder>(PageItem.ItemDiffUtil()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanPagerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemScanBinding.inflate(layoutInflater, parent, false)
        return ScanPagerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScanPagerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id
}
