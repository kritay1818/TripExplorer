package com.example.tripexplorer.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.tripexplorer.R
import com.example.tripexplorer.data.local.PlaceEntity
import com.example.tripexplorer.databinding.FragmentEditNoteBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditNoteFragment : Fragment(R.layout.fragment_edit_note) {

    private var _binding: FragmentEditNoteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityExplorerViewModel by activityViewModels()
    private val args: EditNoteFragmentArgs by navArgs()

    private var selectedPlace: PlaceEntity? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditNoteBinding.bind(view)

        observeSelectedFavorite()
        setupSaveClickListener()
    }

    private fun observeSelectedFavorite() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favoritePlaces.collect { favorites ->
                    val matchedPlace = favorites.firstOrNull { it.xid == args.xid }
                    selectedPlace = matchedPlace

                    if (matchedPlace != null) {
                        binding.tvEditNoteTitle.text = matchedPlace.name
                        if (binding.etUserNote.text.isNullOrBlank()) {
                            binding.etUserNote.setText(matchedPlace.userNote.orEmpty())
                        }
                    }
                }
            }
        }
    }

    private fun setupSaveClickListener() {
        binding.btnSaveNote.setOnClickListener {
            val currentPlace = selectedPlace
            if (currentPlace == null) {
                Toast.makeText(requireContext(), getString(R.string.place_not_found), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val input = binding.etUserNote.text?.toString().orEmpty()
            val trimmed = input.trim()

            if (trimmed.isEmpty()) {
                binding.tilUserNote.error = getString(R.string.note_empty_error)
                return@setOnClickListener
            }

            if (trimmed.length > 500) {
                binding.tilUserNote.error = getString(R.string.note_too_long_error)
                return@setOnClickListener
            }

            binding.tilUserNote.error = null
            val updatedEntity = currentPlace.copy(userNote = trimmed)
            viewModel.updatePlace(updatedEntity)
            Toast.makeText(requireContext(), getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
