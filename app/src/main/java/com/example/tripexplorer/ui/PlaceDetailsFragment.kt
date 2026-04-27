package com.example.tripexplorer.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.tripexplorer.R
import com.example.tripexplorer.data.local.PlaceEntity
import com.example.tripexplorer.databinding.FragmentPlaceDetailsBinding
import com.example.tripexplorer.utils.ResultState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaceDetailsFragment : Fragment(R.layout.fragment_place_details) {

    private var _binding: FragmentPlaceDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityExplorerViewModel by activityViewModels()
    private val args: PlaceDetailsFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaceDetailsBinding.bind(view)

        binding.btnBackBottom.setOnClickListener {
            findNavController().navigateUp()
        }

        observePlaceDetails()
        viewModel.getPlaceDetails(args.xid)
    }

    private fun observePlaceDetails() {
        viewModel.placeDetails.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ResultState.Loading -> {
                    binding.progressBarDetails.visibility = View.VISIBLE
                }

                is ResultState.Error -> {
                    binding.progressBarDetails.visibility = View.GONE
                    binding.tvDetailDescription.text = result.message
                }

                is ResultState.Success -> {
                    binding.progressBarDetails.visibility = View.GONE
                    val data = result.data

                    Glide.with(this)
                        .load(data.preview?.source)
                        .into(binding.ivPlaceImage)

                    binding.tvDetailName.text = data.name
                    binding.tvDetailDescription.text =
                        data.wikipedia_extracts?.text ?: getString(R.string.no_description_available)

                    binding.btnSaveFavorite.setOnClickListener {
                        val placeEntity = PlaceEntity(
                            xid = data.xid,
                            name = data.name,
                            imageUrl = data.preview?.source,
                            userNote = null,
                            lat = 0.0,
                            lon = 0.0
                        )
                        viewModel.saveToFavorites(placeEntity)
                        Toast.makeText(requireContext(), getString(R.string.saved_to_favorites), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
