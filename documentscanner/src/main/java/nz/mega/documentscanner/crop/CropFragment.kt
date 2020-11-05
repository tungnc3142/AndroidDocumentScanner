package nz.mega.documentscanner.crop

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import nz.mega.documentscanner.DocumentScannerViewModel
import nz.mega.documentscanner.R
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.databinding.FragmentCropBinding
import nz.mega.documentscanner.utils.PageUtils.getCroppedBitmap

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
        binding.btnDone.setOnClickListener { saveCrop() }
    }

    private fun setupObservers() {
        viewModel.getCurrentPage().observe(viewLifecycleOwner, ::showCurrentPage)
    }

    private fun showCurrentPage(page: Page?) {
        if (page != null) {
            lifecycleScope.launch {
                val bitmap = page.getCroppedBitmap(requireContext())

                Glide.with(this@CropFragment)
                    .load(bitmap)
                    .into(binding.imgCrop)

                binding.cropView.post {
                    ratioX = binding.cropView.measuredWidth / bitmap.width.toFloat()
                    ratioY = binding.cropView.measuredHeight / bitmap.height.toFloat()

//                    binding.cropView.points = page.cropPoints?.let { cropPoints ->
//                        val relativePoints = cropPoints.map { point ->
//                            PointF(point.x * ratioX, point.y * ratioY)
//                        }
//
//                        binding.cropView.getOrderedPoints(relativePoints)
//                    }
                }
            }
        } else {
            Toast.makeText(requireContext(), "Unknown error", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun saveCrop() {
        showProgress(true)

        val relativePoints = binding.cropView.points.map { point ->
            PointF(point.value.x / ratioX, point.value.y / ratioY)
        }

//        viewModel.cropPage(requireContext(), relativePoints).observe(viewLifecycleOwner) {
//            showProgress(false)
//            findNavController().popBackStack()
//        }
    }

    private fun showProgress(show: Boolean) {
        binding.progress.isVisible = show
        binding.btnDone.isEnabled = !show
    }
}
