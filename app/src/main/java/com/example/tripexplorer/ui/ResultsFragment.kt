package com.example.tripexplorer.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.tripexplorer.R
import com.example.tripexplorer.databinding.FragmentResultsBinding
import com.example.tripexplorer.utils.ResultState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResultsFragment : Fragment(R.layout.fragment_results) {

    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityExplorerViewModel by activityViewModels()

    private lateinit var placesAdapter: PlacesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentResultsBinding.bind(view)

        setupRecyclerView()
        observeSearchResults()
    }

    private fun setupRecyclerView() {
        placesAdapter = PlacesAdapter { placeFeature ->
            val direction =
                ResultsFragmentDirections.actionResultsFragmentToPlaceDetailsFragment(
                    placeFeature.properties.xid
                )
            findNavController().navigate(direction)
        }
        binding.rvPlaces.adapter = placesAdapter
    }

    private fun observeSearchResults() {
        viewModel.searchResults.observe(viewLifecycleOwner) { result ->
            if (result is ResultState.Success) {
                placesAdapter.submitList(result.data)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
