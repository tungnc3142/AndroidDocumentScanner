package nz.mega.documentscanner.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import nz.mega.documentscanner.DocumentScannerViewModel
import nz.mega.documentscanner.R
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.databinding.FragmentScanBinding
import nz.mega.documentscanner.utils.DialogFactory
import nz.mega.documentscanner.view.OffsetPageTransformer

class ScanFragment : Fragment() {

    companion object {
        private const val TAG = "ScanFragment"
    }

    private lateinit var binding: FragmentScanBinding
    private val viewModel: DocumentScannerViewModel by activityViewModels()
    private val adapter: ScanPagerAdapter by lazy { ScanPagerAdapter() }
    private val viewPagerCallback: ViewPager2.OnPageChangeCallback by lazy { buildViewPagerCallback() }
    private val pageTransformer: OffsetPageTransformer by lazy { buildPageTransformer() }
    private var callbackRegistered = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        callbackRegistered = false
        super.onDestroyView()
    }

    private fun setupView() {
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.adapter = adapter
        binding.viewPager.setPageTransformer(pageTransformer)
        binding.btnBack.setOnClickListener { showDiscardDialog() }
        binding.btnAdd.setOnClickListener {
            findNavController().navigate(ScanFragmentDirections.actionScanFragmentToCameraFragment())
        }
        binding.btnRotate.setOnClickListener { viewModel.rotatePage(requireContext()) }
        binding.btnDelete.setOnClickListener {
            DialogFactory.createDeleteCurrentScanDialog(requireContext()) {
                viewModel.deletePage()
            }.show()
        }
        binding.btnCrop.setOnClickListener { findNavController().navigate(ScanFragmentDirections.actionScanFragmentToCropFragment()) }
        binding.btnDone.setOnClickListener { findNavController().navigate(ScanFragmentDirections.actionScanFragmentToSaveFragment()) }
        binding.btnRetake.setOnClickListener {
            viewModel.retakePage()
            findNavController().navigate(ScanFragmentDirections.actionScanFragmentToCameraFragment())
        }
    }

    private fun setupObservers() {
        viewModel.getDocumentTitle().observe(viewLifecycleOwner, ::showDocumentTitle)
        viewModel.getDocumentPages().observe(viewLifecycleOwner, ::showPages)
        viewModel.getCurrentPagePosition().observe(viewLifecycleOwner, ::showPageCount)
    }

    private fun showDocumentTitle(title: String) {
        binding.txtScanTitle.text = title
    }

    private fun showPages(items: List<Page>) {
        adapter.submitList(items)

        if (items.isNotEmpty()) {
            binding.btnDelete.isVisible = items.size != 1

            binding.viewPager.post {
                val currentPosition = viewModel.getCurrentPagePosition().value ?: 0
                if (binding.viewPager.currentItem != currentPosition) {
                    binding.viewPager.currentItem = currentPosition
                }

                if (!callbackRegistered) {
                    binding.viewPager.registerOnPageChangeCallback(viewPagerCallback)
                    callbackRegistered = true
                }
            }
        } else {
            navigateBack()
        }
    }

    /**
     * Show current page count position out of the total pages
     *
     * @param currentPosition Current page position
     */
    private fun showPageCount(currentPosition: Int) {
        binding.txtPageCount.text = String.format(
            getString(R.string.scan_format_page_count),
            currentPosition + 1,
            viewModel.getPagesCount()
        )
    }

    private fun showDiscardDialog() {
        val pagesCount = viewModel.getPagesCount()

        when {
            pagesCount == 1 -> {
                DialogFactory.createDiscardScanDialog(requireContext()) {
                    viewModel.deletePage()
                    navigateBack()
                }.show()
            }
            pagesCount > 1 -> {
                DialogFactory.createDiscardScansDialog(requireContext()) {
                    viewModel.resetDocument()
                    navigateBack()
                }.show()
            }
            else -> navigateBack()
        }
    }

    private fun navigateBack() {
        findNavController().popBackStack(R.id.cameraFragment, false)
    }

    /**
     * Build view pager callback required to track the current page selected
     * has been completed.
     *
     * @return built ViewPager2.OnPageChangeCallback callback
     */
    private fun buildViewPagerCallback(): ViewPager2.OnPageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setCurrentPagePosition(position)
            }
        }

    /**
     * Build ViewPager Page Transformer to add required offset between pages
     *
     * @return built ViewPager2.PageTransformer
     */
    private fun buildPageTransformer(): OffsetPageTransformer =
        OffsetPageTransformer(
            resources.getDimensionPixelOffset(R.dimen.scan_page_margin),
            resources.getDimensionPixelOffset(R.dimen.scan_page_offset)
        )
}
