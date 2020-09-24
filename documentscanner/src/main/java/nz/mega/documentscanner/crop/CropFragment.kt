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
    private var xFactor = 1f
    private var yFactor = 1f

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
        binding.btnDone.setOnClickListener { getCropPoints() }
    }

    private fun setupObservers() {
        viewModel.getCurrentPage().observe(viewLifecycleOwner, ::showCurrentPage)
    }

    private fun showCurrentPage(page: Page?) {
        if (page != null) {
            Glide.with(this)
                .load(page.originalImage.imageUri)
                .into(binding.imgCrop)

            setCropPoints(page.cropPoints, page.originalImage.width, page.originalImage.height)
        } else {
            Toast.makeText(requireContext(), "Unknown error", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun setCropPoints(points: List<PointF>?, maxWidth: Float, maxHeight: Float) {
        binding.cropView.post {
            xFactor = binding.cropView.measuredWidth / maxWidth
            yFactor = binding.cropView.measuredHeight / maxHeight

            binding.cropView.points = points?.let { cropPoints ->
                val relativePoints = cropPoints.map { point ->
                    PointF(point.x * xFactor, point.y * yFactor)
                }

                binding.cropView.getOrderedPoints(relativePoints)
            }
        }
    }

    private fun getCropPoints() {
        val relativePoints = binding.cropView.points.map { point ->
            PointF(point.value.x / xFactor, point.value.y / yFactor)
        }

        viewModel.cropCurrentPage(requireContext(), relativePoints)
        findNavController().popBackStack()
    }
}
