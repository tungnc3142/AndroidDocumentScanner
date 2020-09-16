package nz.mega.documentscanner

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import nz.mega.documentscanner.databinding.ActivityDocumentScannerBinding

class DocumentScannerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DocumentScannerActivity"
    }

    private lateinit var binding: ActivityDocumentScannerBinding

    private val viewModel: DocumentScannerViewModel by viewModels()

    private val navController: NavController by lazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
