package nz.mega.documentscanner.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import nz.mega.documentscanner.DocumentScannerViewModel
import nz.mega.documentscanner.R
import nz.mega.documentscanner.databinding.FragmentCameraBinding
import nz.mega.documentscanner.openCV.ImageScanner
import nz.mega.documentscanner.utils.AnimationUtils.animateCaptureButton
import nz.mega.documentscanner.utils.AnimationUtils.dismissAndShow
import nz.mega.documentscanner.utils.BitmapUtils.toBitmap
import nz.mega.documentscanner.utils.FileUtils
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
    private val snackBar: Snackbar by lazy { buildSnackBar() }

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
    ): View? {
        binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    private fun setupView() {
        binding.btnBack.setOnClickListener { activity?.finish() }

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
            binding.btnTorch.setOnClickListener { toggleTorch() }
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

        val photoFile = FileUtils.createPhotoFile(requireContext())
        val options = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture?.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                lifecycleScope.launch {
                    val photoBitmap = photoFile.toBitmap(requireContext())
                    photoFile.delete()

                    viewModel.addPage(requireContext(), photoBitmap).observe(viewLifecycleOwner)
                    { result ->
                        if (result) {
                            showSnackBar(null)
                            findNavController().navigate(CameraFragmentDirections.actionCameraFragmentToScanFragment())
                        } else {
                            showSnackBar("Picture process error", false)
                            showProgress(false)
                        }
                    }
                }
            }

            override fun onError(error: ImageCaptureException) {
                activity?.runOnUiThread {
                    Log.e(TAG, "Take Picture error: " + error.stackTraceToString())
                    photoFile.delete()
                    showSnackBar(error.message.toString(), false)
                    showProgress(false)
                }
            }
        })
    }

    private fun allPermissionsGranted(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_PERMISSIONS)
    }

    private fun toggleTorch() {
        camera?.let { camera ->
            val newTorchState = camera.cameraInfo.torchState.value != TorchState.ON
            val btnIcon = if (newTorchState) {
                R.drawable.ic_baseline_flash_on_24
            } else {
                R.drawable.ic_baseline_flash_off_24
            }

            camera.cameraControl.enableTorch(newTorchState)
            binding.btnTorch.setImageResource(btnIcon)
        }
    }

    private fun showProgress(show: Boolean) {
        binding.progress.isVisible = show
        binding.btnCapture.isEnabled = !show

        if (show) {
            binding.previewView.setImageBitmap(binding.cameraView.bitmap)
        } else {
            binding.previewView.setImageDrawable(null)
        }

        binding.previewView.isVisible = show
        binding.cameraView.isVisible = !show
    }

    private fun buildSnackBar(): Snackbar =
        Snackbar.make(binding.cameraView, "Scanning page...", Snackbar.LENGTH_INDEFINITE)
            .addCallback(
                object : Snackbar.Callback() {
                    override fun onShown(snackBar: Snackbar) {
                        binding.animateCaptureButton(-snackBar.view.height.toFloat())
                    }

                    override fun onDismissed(snackBar: Snackbar, event: Int) {
                        binding.animateCaptureButton(0f)
                    }
                }
            )

    private fun showSnackBar(message: String?, isIndefinite: Boolean = false) {
        if (message != null) {
            snackBar.setText(message)
            snackBar.duration = if (isIndefinite) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG
            snackBar.dismissAndShow()
        } else {
            snackBar.dismiss()
        }
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
