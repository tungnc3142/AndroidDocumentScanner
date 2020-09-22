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
import nz.mega.documentscanner.utils.FileUtils.PROVIDER_AUTHORITY
import nz.mega.documentscanner.utils.IntentUtils.extra
import org.opencv.android.OpenCVLoader

class DocumentScannerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DocumentScannerActivity"
        private const val EXTRA_SAVE_DESTINATIONS = "EXTRA_SAVE_DESTINATIONS"

        @JvmStatic
        fun getIntent(context: Context, saveDestinations: Array<String>? = null): Intent =
            Intent(context, DocumentScannerActivity::class.java).apply {
                saveDestinations?.let { putExtra(EXTRA_SAVE_DESTINATIONS, it) }
            }
    }

    private lateinit var binding: ActivityDocumentScannerBinding

    private val viewModel: DocumentScannerViewModel by viewModels()
    private val saveDestinations: Array<String>? by extra(EXTRA_SAVE_DESTINATIONS)

    private val navController: NavController by lazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initOpenCV()
        clearExistingFiles()

        binding = ActivityDocumentScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupObservers()
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
        viewModel.setSaveDestinations(
            saveDestinations
                ?: arrayOf("Cloud Drive", "Chat")
        ) // TODO Remove hardcoded destinations
    }

    private fun onResultDocument(documentUri: Uri) {
        if (callingActivity != null) {
            val intent = Intent().apply {
                setDataAndType(documentUri, contentResolver.getType(documentUri))
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        } else {
            val fileUri = FileProvider.getUriForFile(this, PROVIDER_AUTHORITY, documentUri.toFile())
            val fileMimeType = contentResolver.getType(fileUri)
            val fileTitle = viewModel.getDocumentTitle().value

            val shareIntent = ShareCompat.IntentBuilder.from(this)
                .setType(fileMimeType)
                .setChooserTitle(fileTitle)
                .setStream(fileUri)
                .createChooserIntent()

            startActivity(shareIntent)
        }
    }
}
