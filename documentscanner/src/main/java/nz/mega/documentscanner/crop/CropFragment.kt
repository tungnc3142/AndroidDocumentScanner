package nz.mega.documentscanner.crop

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import nz.mega.documentscanner.DocumentScannerViewModel
import nz.mega.documentscanner.R
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.databinding.FragmentCropBinding

class CropFragment : Fragment() {

    companion object {
        private const val TAG = "CropFragment"
    }

    private val viewModel: DocumentScannerViewModel by activityViewModels()

    private lateinit var binding: FragmentCropBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCropBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupObservers()
    }

    private fun setupView() {
        binding.cropView.setPointColor(ContextCompat.getColor(requireContext(), R.color.secondaryColor))
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnDone.setOnClickListener {
            val points = binding.cropView.points.map(Map.Entry<Int, PointF>::value)
            viewModel.cropCurrentPage(requireContext(), points)
            findNavController().popBackStack()
        }
    }

    private fun setupObservers() {
        viewModel.getCurrentPage().observe(viewLifecycleOwner, ::showCurrentPage)
    }

    private fun showCurrentPage(page: Page?) {
        if (page != null) {
            Glide.with(this)
                .load(page.originalImageUri)
                .into(binding.imgCrop)

            binding.cropView.post {
                showCropPoints(
                    page.cropPoints ?: page.getContourPoints(),
                    page.width,
                    page.height
                )
            }
        } else {
            Toast.makeText(requireContext(), "Unknown error", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun showCropPoints(points: List<PointF>, maxWidth: Int, maxHeight: Int) {
        val cropViewWidth = binding.cropView.measuredWidth.toFloat()
        val cropViewHeight = binding.cropView.measuredHeight.toFloat()

        val xFactor = cropViewWidth / maxWidth
        val yFactor = cropViewHeight / maxHeight

        val relativePoints = points.map { point ->
            point.apply {
                point.x *= xFactor
                point.y *= yFactor
            }
        }

        binding.cropView.points = binding.cropView.getOrderedPoints(relativePoints)
        binding.cropView.invalidate()
    }
}
