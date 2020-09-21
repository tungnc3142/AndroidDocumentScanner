package nz.mega.documentscanner.save

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import nz.mega.documentscanner.DocumentScannerViewModel
import nz.mega.documentscanner.databinding.FragmentSaveBinding
import nz.mega.documentscanner.databinding.ItemDestinationBinding

class SaveFragment : Fragment() {

    companion object {
        private const val TAG = "SaveFragment"
    }

    private val viewModel: DocumentScannerViewModel by activityViewModels()

    private lateinit var binding: FragmentSaveBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupObservers()
    }

    private fun setupView() {
        binding.btnSave.setOnClickListener {
            // Save config
            findNavController().popBackStack()
        }
    }

    private fun setupObservers() {
        viewModel.destinations.observe(viewLifecycleOwner, ::showDestinations)
    }

    private fun showDestinations(destinations: Array<String>?) {
        binding.groupDestination.isVisible = !destinations.isNullOrEmpty()

        destinations?.forEachIndexed { index, destination ->
            val chip = ItemDestinationBinding.inflate(layoutInflater, binding.chipGroupDestinations, false).root
            chip.text = destination
            chip.setOnClickListener { onDestinationClick(destination) }
            binding.chipGroupDestinations.addView(chip)

            if (index == 0) {
                binding.chipGroupDestinations.check(chip.id)
            }
        }
    }

    private fun onDestinationClick(item: String) {

    }
}
