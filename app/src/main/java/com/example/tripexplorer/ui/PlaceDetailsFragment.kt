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
import com.bumptech.glide.Glide
import com.example.tripexplorer.R
import com.example.tripexplorer.data.local.PlaceEntity
import com.example.tripexplorer.data.remote.PlaceDetailsResponse
import com.example.tripexplorer.databinding.FragmentPlaceDetailsBinding
import com.example.tripexplorer.utils.ResultState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaceDetailsFragment : Fragment(R.layout.fragment_place_details) {

    private var _binding: FragmentPlaceDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityExplorerViewModel by activityViewModels()
    private val args: PlaceDetailsFragmentArgs by navArgs()
    private var currentPlaceDetails: PlaceDetailsResponse? = null
    private var existingFavorite: PlaceEntity? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaceDetailsBinding.bind(view)

        binding.btnBackBottom.setOnClickListener {
            findNavController().navigateUp()
        }

        setupFavoriteButton()
        observeFavoriteState()
        observePlaceDetails()
        viewModel.getPlaceDetails(args.xid)
    }

    private fun setupFavoriteButton() {
        binding.btnSaveFavorite.setOnClickListener {
            val details = currentPlaceDetails ?: return@setOnClickListener
            binding.btnSaveFavorite.isEnabled = false

            val favoriteToRemove = existingFavorite
            if (favoriteToRemove != null) {
                viewModel.removeFromFavorites(favoriteToRemove)
                binding.btnSaveFavorite.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    getString(R.string.deleted_from_favorites),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val placeEntity = PlaceEntity(
                xid = details.xid,
                name = details.name,
                imageUrl = details.preview?.source,
                userNote = null,
                lat = 0.0,
                lon = 0.0
            )
            viewModel.saveToFavorites(placeEntity) { isSaved ->
                if (!isAdded) return@saveToFavorites
                binding.btnSaveFavorite.isEnabled = true
                val message = if (isSaved) {
                    getString(R.string.saved_to_favorites)
                } else {
                    getString(R.string.already_in_favorites)
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeFavoriteState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favoritePlaces.collect { favorites ->
                    existingFavorite = favorites.firstOrNull { it.xid == args.xid }
                    updateFavoriteButtonState()
                }
            }
        }
    }

    private fun updateFavoriteButtonState() {
        binding.btnSaveFavorite.isEnabled = currentPlaceDetails != null
        binding.btnSaveFavorite.text = if (existingFavorite != null) {
            getString(R.string.remove_from_favorites)
        } else {
            getString(R.string.save_to_bucket_list)
        }
    }

    private fun observePlaceDetails() {
        viewModel.placeDetails.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ResultState.Loading -> {
                    binding.progressBarDetails.visibility = View.VISIBLE
                }

                is ResultState.Error -> {
                    binding.progressBarDetails.visibility = View.GONE
                    currentPlaceDetails = null
                    updateFavoriteButtonState()
                    binding.tvDetailDescription.text = result.message
                }

                is ResultState.Success -> {
                    binding.progressBarDetails.visibility = View.GONE
                    val data = result.data
                    currentPlaceDetails = data
                    updateFavoriteButtonState()

                    Glide.with(this)
                        .load(data.preview?.source)
                        .into(binding.ivPlaceImage)

                    binding.tvDetailName.text = data.name
                    binding.tvDetailDescription.text =
                        data.wikipedia_extracts?.text ?: getString(R.string.no_description_available)
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
