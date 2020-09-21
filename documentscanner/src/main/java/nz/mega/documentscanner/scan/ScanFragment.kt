package nz.mega.documentscanner.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import nz.mega.documentscanner.DocumentScannerViewModel
import nz.mega.documentscanner.R
import nz.mega.documentscanner.data.ScanDocument
import nz.mega.documentscanner.databinding.FragmentScanBinding
import nz.mega.documentscanner.save.SaveFragment
import nz.mega.documentscanner.utils.OffsetPageTransformer
import nz.mega.documentscanner.utils.ViewUtils.scrollToLastPosition

class ScanFragment : Fragment() {

    companion object {
        private const val TAG = "ScanFragment"
    }

    private val viewModel: DocumentScannerViewModel by activityViewModels()
    private val adapter: ScanPagerAdapter by lazy { ScanPagerAdapter() }
    private val viewPagerCallback: ViewPager2.OnPageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setCurrentDocumentPosition(position)
            }
        }
    }

    private lateinit var binding: FragmentScanBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        binding.viewPager.post { binding.viewPager.scrollToLastPosition() }
    }

    override fun onDestroyView() {
        binding.viewPager.unregisterOnPageChangeCallback(viewPagerCallback)
        super.onDestroyView()
    }

    private fun setupView() {
        val pageMargin = resources.getDimensionPixelOffset(R.dimen.scan_page_margin)
        val pageOffset = resources.getDimensionPixelOffset(R.dimen.scan_page_offset)

        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.setPageTransformer(OffsetPageTransformer(pageOffset, pageMargin))
        binding.viewPager.registerOnPageChangeCallback(viewPagerCallback)
        binding.viewPager.adapter = adapter
        binding.btnBack.setOnClickListener { findNavController().popBackStack(R.id.cameraFragment, false) }
        binding.btnAdd.setOnClickListener { findNavController().navigate(ScanFragmentDirections.actionScanFragmentToCameraFragment()) }
        binding.btnRotate.setOnClickListener { viewModel.rotateCurrentDocument() }
        binding.btnDelete.setOnClickListener { viewModel.deleteCurrentDocument() }
        binding.btnCrop.setOnClickListener { findNavController().navigate(ScanFragmentDirections.actionScanFragmentToCropFragment()) }
        binding.btnDone.setOnClickListener { findNavController().navigate(ScanFragmentDirections.actionScanFragmentToSaveFragment()) }
        binding.btnRetake.setOnClickListener {
            viewModel.deleteCurrentDocument()
            findNavController().navigate(ScanFragmentDirections.actionScanFragmentToCameraFragment())
        }
    }

    private fun setupObservers() {
        viewModel.getDocuments().observe(viewLifecycleOwner, ::showDocuments)
        viewModel.getCurrentDocument().observe(viewLifecycleOwner, ::showCurrentDocument)
        viewModel.getCurrentDocumentPosition().observe(viewLifecycleOwner, ::showDocumentPosition)
    }

    private fun showDocuments(items: List<ScanDocument>) {
        adapter.submitList(items)

        if (items.isEmpty()) {
            findNavController().popBackStack(R.id.cameraFragment, false)
        }
    }

    private fun showCurrentDocument(document: ScanDocument?) {
        binding.txtScanTitle.text = document?.title
    }

    private fun showDocumentPosition(position: Pair<Int, Int>) {
        val currentPosition = position.first
        val totalPositions = position.second
        binding.txtPageCount.text = "$currentPosition / $totalPositions"
    }
}
