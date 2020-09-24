package nz.mega.documentscanner.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import nz.mega.documentscanner.DocumentScannerViewModel
import nz.mega.documentscanner.R
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.databinding.FragmentScanBinding
import nz.mega.documentscanner.utils.DialogFactory
import nz.mega.documentscanner.utils.OffsetPageTransformer

class ScanFragment : Fragment() {

    companion object {
        private const val TAG = "ScanFragment"
    }

    private val viewModel: DocumentScannerViewModel by activityViewModels()
    private val adapter: ScanPagerAdapter by lazy { ScanPagerAdapter() }
    private val viewPagerCallback: ViewPager2.OnPageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setCurrentPagePosition(position)
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) { showDiscardDialog() }
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
        binding.btnBack.setOnClickListener { showDiscardDialog() }
        binding.btnAdd.setOnClickListener { findNavController().navigate(ScanFragmentDirections.actionScanFragmentToCameraFragment()) }
        binding.btnRotate.setOnClickListener { viewModel.rotateCurrentPage(requireContext()) }
        binding.btnDelete.setOnClickListener {
            DialogFactory.createDeleteCurrentScanDialog(requireContext()) {
                viewModel.deleteCurrentPage()
            }.show()
        }
        binding.btnCrop.setOnClickListener { findNavController().navigate(ScanFragmentDirections.actionScanFragmentToCropFragment()) }
        binding.btnDone.setOnClickListener { findNavController().navigate(ScanFragmentDirections.actionScanFragmentToSaveFragment()) }
        binding.btnRetake.setOnClickListener {
            viewModel.deleteCurrentPage()
            findNavController().navigate(ScanFragmentDirections.actionScanFragmentToCameraFragment())
        }
    }

    private fun setupObservers() {
        viewModel.getDocumentPages().observe(viewLifecycleOwner, ::showPages)
        viewModel.getDocumentTitle().observe(viewLifecycleOwner, ::showDocumentTitle)
        viewModel.getCurrentPagePosition().observe(viewLifecycleOwner, ::showPagePosition)
    }

    private fun showPages(items: List<Page>) {
        adapter.submitList(items)

        if (items.isEmpty()) {
            findNavController().popBackStack(R.id.cameraFragment, false)
        }
    }

    private fun showDocumentTitle(title: String) {
        binding.txtScanTitle.text = title
    }

    private fun showPagePosition(position: Pair<Int, Int>) {
        val currentPosition = position.first
        val totalPositions = position.second
        binding.txtPageCount.text = "$currentPosition / $totalPositions"
    }

    private fun showDiscardDialog() {
        val pagesCount = viewModel.getPagesCount()

        when {
            pagesCount == 1 -> {
                DialogFactory.createDiscardScanDialog(requireContext()) {
                    viewModel.deleteCurrentPage()
                    findNavController().popBackStack(R.id.cameraFragment, false)
                }.show()
            }
            pagesCount > 1 -> {
                DialogFactory.createDiscardScansDialog(requireContext()) {
                    viewModel.deleteAllPages()
                    findNavController().popBackStack(R.id.cameraFragment, false)
                }.show()
            }
            else -> {
                findNavController().popBackStack(R.id.cameraFragment, false)
            }
        }
    }
}
