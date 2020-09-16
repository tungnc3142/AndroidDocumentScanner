package nz.mega.documentscanner.scan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import nz.mega.documentscanner.data.ScanDocument
import nz.mega.documentscanner.databinding.LayoutScanItemBinding

class ScanPagerAdapter : ListAdapter<ScanDocument, ScanPagerViewHolder>
    (ScanDocument.ItemDiffUtil()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanPagerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = LayoutScanItemBinding.inflate(layoutInflater, parent, false)
        return ScanPagerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScanPagerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long =
        getItem(position).hashCode().toLong()
}
