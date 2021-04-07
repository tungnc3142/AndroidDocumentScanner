package nz.mega.documentscanner.crop

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import nz.mega.documentscanner.DocumentScannerViewModel
import nz.mega.documentscanner.R
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.databinding.FragmentCropBinding
import nz.mega.documentscanner.utils.PageUtils.getOriginalDimensions
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point

class CropFragment : Fragment() {

    companion object {
        private const val TAG = "CropFragment"
    }

    private val viewModel: DocumentScannerViewModel by activityViewModels()

    private lateinit var binding: FragmentCropBinding
    private var ratioX = 1f
    private var ratioY = 1f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        binding.btnDone.setOnClickListener { saveCrop() }
    }

    private fun setupObservers() {
        viewModel.getCurrentPage().observe(viewLifecycleOwner, ::showCurrentPage)
    }

    private fun showCurrentPage(page: Page?) {
        if (page == null) {
            findNavController().popBackStack()
            return
        }

        binding.imgCrop.setImageURI(page.originalImageUri)

        binding.cropView.post {
            lifecycleScope.launch {
                val pageDimensions = page.getOriginalDimensions()
                val pageWidth = pageDimensions.first.toFloat()
                val pageHeight = pageDimensions.second.toFloat()

                ratioX = binding.cropView.width / pageWidth
                ratioY = binding.cropView.height / pageHeight

                val points = page.cropMat?.let { mat ->
                    val relativePoints = mat.toArray().map { point ->
                        PointF(
                            (point.x * ratioX).toFloat(),
                            (point.y * ratioY).toFloat()
                        )
                    }

                    binding.cropView.getOrderedPoints(relativePoints)
                }
                binding.cropView.setPoints(points)
            }
        }
    }

    private fun saveCrop() {
        binding.btnDone.isEnabled = false

        val relativePoints = binding.cropView.getPoints().map { point ->
            Point((point.value.x / ratioX).toDouble(), (point.value.y / ratioY).toDouble())
        }

        val cropMat = MatOfPoint2f().apply { fromList(relativePoints) }
        viewModel.cropPage(requireContext(), cropMat)

        binding.btnDone.isEnabled = true
        findNavController().popBackStack()
    }
}
