package nz.mega.documentscanner.save

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import nz.mega.documentscanner.DocumentScannerViewModel
import nz.mega.documentscanner.R
import nz.mega.documentscanner.data.Document
import nz.mega.documentscanner.databinding.FragmentSaveBinding
import nz.mega.documentscanner.databinding.ItemDestinationBinding
import nz.mega.documentscanner.utils.FileUtils.FILE_NAME_PATTERN
import nz.mega.documentscanner.utils.ViewUtils.selectAllCharacters

class SaveFragment : Fragment() {

    companion object {
        private const val TAG = "SaveFragment"
    }

    private lateinit var binding: FragmentSaveBinding

    private val viewModel: DocumentScannerViewModel by activityViewModels()
    private val progressDialog: AlertDialog by lazy {
        MaterialAlertDialogBuilder(requireContext())
            .setView(R.layout.dialog_progress)
            .setMessage(
                getString(
                    R.string.scan_dialog_progress,
                    binding.editFileName.suffix!!.split(".")[1]
                )
            )
            .setCancelable(false)
            .show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupObservers()
    }

    private fun setupView() {
        binding.editFileName.doAfterTextChanged { editable ->
            viewModel.setDocumentTitle(editable?.toString())
        }

        binding.editFileName.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                view.clearFocus()
            }
            false
        }

        binding.editFileName.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            binding.imgRename.isVisible = !hasFocus
        }

        binding.imgRename.setOnClickListener { binding.editFileName.selectAllCharacters() }

        binding.chipGroupFileType.setOnCheckedChangeListener { _, checkedId ->
            val fileType = when (checkedId) {
                R.id.chip_file_type_pdf -> Document.FileType.PDF
                R.id.chip_file_type_jpg -> Document.FileType.JPG
                else -> error("Unrecognized document file type")
            }

            viewModel.setDocumentFileType(fileType)
        }

        binding.chipGroupQuality.setOnCheckedChangeListener { _, checkedId ->
            val quality = when (checkedId) {
                R.id.chip_quality_low -> Document.Quality.LOW
                R.id.chip_quality_medium -> Document.Quality.MEDIUM
                R.id.chip_quality_high -> Document.Quality.HIGH
                else -> error("Unrecognized document quality")
            }

            viewModel.setDocumentQuality(quality)
        }

        binding.chipGroupDestinations.setOnCheckedChangeListener { group, checkedId ->
            group.children.firstOrNull { it.id == checkedId }
                ?.let { child ->
                    val destination = (child as Chip).text.toString()
                    viewModel.setDocumentSaveDestination(destination)
                }
        }

        binding.groupFileType.isVisible = viewModel.getPagesCount() == 1
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnSave.setOnClickListener { createDocument() }
    }

    private fun setupObservers() {
        viewModel.getSaveDestinations().observe(viewLifecycleOwner, ::showSaveDestinations)
        viewModel.getDocumentFileType().observe(viewLifecycleOwner, ::showDocumentFileType)
        viewModel.getDocumentTitle().observe(viewLifecycleOwner, ::showDocumentTitle)
        viewModel.getDocumentQuality().observe(viewLifecycleOwner, ::showDocumentQuality)
    }

    private fun showSaveDestinations(destinations: List<Pair<String, Boolean>>) {
        binding.chipGroupDestinations.removeAllViews()
        binding.groupDestination.isVisible = destinations.isNotEmpty()

        destinations.forEach { destination ->
            val chip = ItemDestinationBinding.inflate(layoutInflater, binding.chipGroupDestinations, false).root
            chip.text = destination.first

            binding.chipGroupDestinations.addView(chip)
            if (destination.second) {
                binding.chipGroupDestinations.check(chip.id)
            }
        }
    }

    private fun showDocumentTitle(title: String?) {
        when {
            title.isNullOrBlank() -> {
                binding.inputFileName.error = getString(R.string.scan_incorrect_name)
            }
            FILE_NAME_PATTERN.toRegex().containsMatchIn(title) -> {
                binding.inputFileName.error = getString(R.string.scan_invalid_characters)
            }
            title != binding.editFileName.text.toString() -> {
                binding.editFileName.setText(title)
                binding.inputFileName.error = null
            }
            else -> {
                binding.inputFileName.error = null
            }
        }
    }

    private fun showDocumentFileType(fileType: Document.FileType) {
        val chipResId: Int
        val imageResId: Int

        when (fileType) {
            Document.FileType.PDF -> {
                chipResId = R.id.chip_file_type_pdf
                imageResId = R.drawable.ic_docscanner_pdf
            }
            Document.FileType.JPG -> {
                chipResId = R.id.chip_file_type_jpg
                imageResId = R.drawable.ic_docscanner_jpeg
            }
        }

        binding.editFileName.suffix = " ${fileType.suffix}"
        binding.imgFileType.setImageResource(imageResId)

        if (binding.chipGroupFileType.checkedChipId != chipResId) {
            binding.chipGroupFileType.check(chipResId)
        }
    }

    private fun showDocumentQuality(quality: Document.Quality) {
        val chipResId = when (quality) {
            Document.Quality.LOW -> R.id.chip_quality_low
            Document.Quality.MEDIUM -> R.id.chip_quality_medium
            Document.Quality.HIGH -> R.id.chip_quality_high
        }

        if (binding.chipGroupQuality.checkedChipId != chipResId) {
            binding.chipGroupQuality.check(chipResId)
        }
    }

    private fun createDocument() {
        when (binding.inputFileName.error) {
            getString(R.string.scan_incorrect_name) -> {
                showSnackbar(R.string.scan_snackbar_incorrect_name)
            }
            getString(R.string.scan_invalid_characters) -> {
                showSnackbar(R.string.scan_snackbar_invalid_characters)
            }
            else -> {
                progressDialog.show()
                viewModel.generateDocument(requireContext()).observe(viewLifecycleOwner) {
                    progressDialog.dismiss()
                }
            }
        }
    }

    private fun showSnackbar(@StringRes errorMessage: Int) {
        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
    }
}
