package com.example.tripexplorer.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.tripexplorer.R
import com.example.tripexplorer.databinding.FragmentResultsBinding
import com.example.tripexplorer.utils.ResultState
import com.google.android.material.chip.Chip
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

        setupStartAnotherSearchButton()
        setupRecyclerView()
        setupCategoryFilterChips()
        observeSearchResults()
    }

    private fun setupStartAnotherSearchButton() {
        binding.btnStartAnotherSearch.setOnClickListener {
            val navController = findNavController()
            val navigatedBackToSearch = navController.popBackStack(R.id.searchFragment, false)
            if (!navigatedBackToSearch) {
                navController.navigate(R.id.searchFragment)
            }
        }
    }

    private fun setupRecyclerView() {
        placesAdapter = PlacesAdapter(
            onItemClick = { placeFeature ->
                val direction =
                    ResultsFragmentDirections.actionResultsFragmentToPlaceDetailsFragment(
                        placeFeature.properties.xid
                    )
                findNavController().navigate(direction)
            }
        )
        binding.rvPlaces.adapter = placesAdapter
    }

    private fun setupCategoryFilterChips() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedChip = checkedIds.firstOrNull()?.let { checkedId ->
                group.findViewById<Chip>(checkedId)
            }
            val selectedText = selectedChip?.text?.toString() ?: getString(R.string.chip_all)
            viewModel.filterPlacesByCategory(selectedText)
        }
    }

    private fun observeSearchResults() {
        viewModel.searchResults.observe(viewLifecycleOwner) { result ->
            if (result is ResultState.Success) {
                Log.d("TripExplorer", "Places found: ${result.data.size}")
                placesAdapter.submitList(result.data)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
