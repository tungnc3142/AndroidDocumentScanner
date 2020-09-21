package nz.mega.documentscanner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import nz.mega.documentscanner.databinding.ActivityDocumentScannerBinding
import nz.mega.documentscanner.utils.FileUtils
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

        viewModel.saveDestinations.value = saveDestinations
            ?: arrayOf("Cloud Drive", "Chat") // TODO Remove hardcoded destinations
    }

    private fun initOpenCV() {
        val result = OpenCVLoader.initDebug()
        Log.d(TAG, "OpenCV initialized: $result")
    }

    private fun clearExistingFiles() {
        val result = FileUtils.clearExistingFiles(this)
        Log.d(TAG, "Cleared existing files: $result")
    }
}
