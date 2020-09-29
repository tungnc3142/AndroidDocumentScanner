package nz.mega.documentscanner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import nz.mega.documentscanner.databinding.ActivityDocumentScannerBinding
import nz.mega.documentscanner.utils.FileUtils
import nz.mega.documentscanner.utils.IntentUtils.extra
import nz.mega.documentscanner.utils.ViewUtils.hideKeyboard
import org.opencv.android.OpenCVLoader

class DocumentScannerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DocumentScannerActivity"
        private const val EXTRA_SAVE_DESTINATIONS = "EXTRA_SAVE_DESTINATIONS"
        private const val EXTRA_PICKED_SAVE_DESTINATION = "EXTRA_PICKED_SAVE_DESTINATION"

        @JvmStatic
        @JvmOverloads
        fun getIntent(context: Context, saveDestinations: Array<String>? = null): Intent =
            Intent(context, DocumentScannerActivity::class.java).apply {
                saveDestinations?.let { putExtra(EXTRA_SAVE_DESTINATIONS, it) }
            }
    }

    private lateinit var binding: ActivityDocumentScannerBinding

    private val viewModel: DocumentScannerViewModel by viewModels()
    private val saveDestinations: Array<String>? by extra(EXTRA_SAVE_DESTINATIONS)
    private val destinationChangedListener: NavController.OnDestinationChangedListener by lazy {
        NavController.OnDestinationChangedListener { _, _, _ -> currentFocus?.hideKeyboard() }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initOpenCV()
        clearExistingFiles()

        binding = ActivityDocumentScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupObservers()

        findNavController().addOnDestinationChangedListener(destinationChangedListener)
    }

    override fun onDestroy() {
        findNavController().removeOnDestinationChangedListener(destinationChangedListener)
        super.onDestroy()
    }

    private fun initOpenCV() {
        val result = OpenCVLoader.initDebug()
        Log.d(TAG, "OpenCV initialized: $result")
    }

    private fun clearExistingFiles() {
        val result = FileUtils.clearExistingFiles(this)
        Log.d(TAG, "Cleared existing files: $result")
    }

    private fun setupObservers() {
        viewModel.getResultDocument().observe(this, ::onResultDocument)

        if (!saveDestinations.isNullOrEmpty()) {
            viewModel.setSaveDestinations(saveDestinations!!)
        } else if (BuildConfig.DEBUG) {
            viewModel.setSaveDestinations(arrayOf("Cloud Drive", "Chat"))
        }
    }

    private fun onResultDocument(documentUri: Uri) {
        if (callingActivity != null) {
            val resultIntent = Intent().apply {
                putExtra(EXTRA_PICKED_SAVE_DESTINATION, viewModel.getSaveDestination())
                setDataAndType(documentUri, contentResolver.getType(documentUri))
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } else {
            val providerAuthority = FileUtils.getProviderAuthority(this)
            val fileUri = FileProvider.getUriForFile(this, providerAuthority, documentUri.toFile())
            val fileMimeType = contentResolver.getType(fileUri)
            val fileTitle = viewModel.getDocumentTitle().value

            val shareIntent = ShareCompat.IntentBuilder.from(this)
                .setType(fileMimeType)
                .setChooserTitle(fileTitle)
                .setStream(fileUri)
                .createChooserIntent()
                .apply { putExtra(EXTRA_PICKED_SAVE_DESTINATION, viewModel.getSaveDestination()) }

            startActivity(shareIntent)
        }
    }

    private fun findNavController(): NavController =
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
}
