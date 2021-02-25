package nz.mega.documentscanner.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import nz.mega.documentscanner.DocumentScannerViewModel
import nz.mega.documentscanner.R
import nz.mega.documentscanner.databinding.FragmentCameraBinding
import nz.mega.documentscanner.openCV.ImageScanner
import nz.mega.documentscanner.utils.BitmapUtils.toBitmap
import nz.mega.documentscanner.utils.DialogFactory
import nz.mega.documentscanner.utils.ViewUtils.aspectRatio
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    companion object {
        private const val TAG = "CameraFragment"
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

    private val viewModel: DocumentScannerViewModel by activityViewModels()
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val screenAspectRatio: Int by lazy { binding.cameraView.display.aspectRatio() }

    private lateinit var binding: FragmentCameraBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) { showDiscardDialog() }
    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    private fun setupView() {
        binding.progress.setVisibilityAfterHide(View.GONE)
        binding.btnBack.setOnClickListener { showDiscardDialog() }

        if (allPermissionsGranted()) {
            binding.cameraView.post { setUpCamera() }
        } else {
            requestPermissions()
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply { setAnalyzer(cameraExecutor, ::analyzePreviewImage) }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .build()

            cameraProvider?.unbindAll()

            camera = cameraProvider?.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalyzer
            )

            preview?.setSurfaceProvider(binding.cameraView.surfaceProvider)

            binding.btnCapture.setOnClickListener { takePicture() }
            binding.btnFlash.setOnClickListener { toggleFlash() }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun analyzePreviewImage(imageProxy: ImageProxy) {
        lifecycleScope.launch {
            try {
                val maxWidth = binding.cameraOverlay.measuredWidth.toFloat()
                val maxHeight = binding.cameraOverlay.measuredHeight.toFloat()

                binding.cameraOverlay.scaleX = imageProxy.width / maxWidth
                binding.cameraOverlay.lines = ImageScanner.getCropLines(imageProxy, maxWidth, maxHeight)

                imageProxy.close()
            } catch (error: Exception) {
                Log.e(TAG, error.stackTraceToString())
            }
        }
    }

    private fun takePicture() {
        showProgress(true)
        imageAnalyzer?.clearAnalyzer()
        imageCapture?.takePicture(cameraExecutor, buildImageCapturedCallback())
    }

    private fun allPermissionsGranted(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_PERMISSIONS)
    }

    private fun toggleFlash() {
        imageCapture?.let { imageCapture ->
            val btnIcon: Int
            val flashMode: Int

            when (imageCapture.flashMode) {
                ImageCapture.FLASH_MODE_ON -> {
                    flashMode = ImageCapture.FLASH_MODE_OFF
                    btnIcon = R.drawable.ic_baseline_flash_off_24
                }
                ImageCapture.FLASH_MODE_AUTO -> {
                    flashMode = ImageCapture.FLASH_MODE_ON
                    btnIcon = R.drawable.ic_baseline_flash_on_24
                }
                else -> {
                    flashMode = ImageCapture.FLASH_MODE_AUTO
                    btnIcon = R.drawable.ic_baseline_flash_auto_24
                }
            }

            imageCapture.flashMode = flashMode
            binding.btnFlash.setImageResource(btnIcon)
        }
    }

    private fun showProgress(show: Boolean) {
        binding.btnCapture.isEnabled = !show

        if (show) {
            binding.progress.show()
            binding.previewView.setImageBitmap(binding.cameraView.bitmap)
        } else {
            binding.progress.hide()
            binding.previewView.setImageDrawable(null)
        }

        binding.previewView.isVisible = show
        binding.cameraView.isVisible = !show
    }

    /**
     * Build image captured callback required to take a picture for when an image capture
     * has been completed.
     *
     * @return built ImageCapture.OnImageCapturedCallback callback
     */
    private fun buildImageCapturedCallback(): ImageCapture.OnImageCapturedCallback =
        object : ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("UnsafeExperimentalUsageError")
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                lifecycleScope.launch {
                    val bitmap = imageProxy.toBitmap()

                    viewModel.addPage(requireContext(), bitmap).observe(viewLifecycleOwner) { result ->
                        if (result) {
                            findNavController().navigate(CameraFragmentDirections.actionCameraFragmentToScanFragment())
                        } else {
                            showToast("Picture process error")
                            showProgress(false)
                        }
                    }
                }
            }

            override fun onError(error: ImageCaptureException) {
                Log.e(TAG, error.stackTraceToString())
                showToast(error.message.toString())
                showProgress(false)
            }
        }

    private fun showDiscardDialog() {
        val pagesCount = viewModel.getPagesCount()

        when {
            pagesCount == 1 -> {
                DialogFactory.createDiscardScanDialog(requireContext()) {
                    viewModel.discardScan()
                }.show()
            }
            pagesCount > 1 -> {
                DialogFactory.createDiscardScansDialog(requireContext()) {
                    viewModel.discardScan()
                }.show()
            }
            else -> viewModel.discardScan()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if (allPermissionsGranted()) {
                    setUpCamera()
                } else {
                    showToast(getString(R.string.scan_requires_permission))
                    viewModel.discardScan()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onLowMemory() {
        imageAnalyzer?.apply {
            clearAnalyzer()
            cameraProvider?.unbind(this)
            binding.cameraOverlay.isVisible = false
        }
        super.onLowMemory()
    }
}
