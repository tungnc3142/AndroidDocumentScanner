package nz.mega.documentscanner.crop

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import nz.mega.documentscanner.DocumentScannerViewModel
import nz.mega.documentscanner.R
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.databinding.FragmentCropBinding
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

        binding.imgCrop.setImageURI(page.imageUri)

        binding.cropView.post {
            ratioX = binding.cropView.width / page.width.toFloat()
            ratioY = binding.cropView.height / page.height.toFloat()

            binding.cropView.points = page.cropMat?.let { mat ->
                val relativePoints = mat.toArray().map { point ->
                    PointF(
                        (point.x * ratioX).toFloat(),
                        (point.y * ratioY).toFloat()
                    )
                }

                binding.cropView.getOrderedPoints(relativePoints)
            }
        }
    }

    private fun saveCrop() {
        binding.btnDone.isEnabled = false

        val relativePoints = binding.cropView.points.map { point ->
            Point((point.value.x / ratioX).toDouble(), (point.value.y / ratioY).toDouble())
        }

        val cropMat = MatOfPoint2f().apply { fromList(relativePoints) }
        viewModel.cropPage(cropMat)

        binding.btnDone.isEnabled = true
        findNavController().popBackStack()
    }
}
