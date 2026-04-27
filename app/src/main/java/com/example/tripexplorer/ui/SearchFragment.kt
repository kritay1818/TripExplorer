package com.example.tripexplorer.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.tripexplorer.R
import com.example.tripexplorer.databinding.FragmentSearchBinding
import com.example.tripexplorer.utils.ResultState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityExplorerViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)

        setupSearchClickListener()
        observeSearchResults()
    }

    private fun setupSearchClickListener() {
        binding.btnSearch.setOnClickListener {
            val cityName = binding.etSearchCity.text?.toString()?.trim().orEmpty()

            if (cityName.isBlank()) {
                binding.tilSearchCity.error = "City name cannot be empty"
                return@setOnClickListener
            }

            binding.tilSearchCity.error = null
            viewModel.searchPlaces(cityName)
        }
    }

    private fun observeSearchResults() {
        viewModel.searchResults.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ResultState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSearch.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                }

                is ResultState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSearch.visibility = View.VISIBLE
                    binding.tvError.text = result.message
                    binding.tvError.visibility = View.VISIBLE
                }

                is ResultState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSearch.visibility = View.VISIBLE
                    binding.tvError.visibility = View.GONE
                    findNavController().navigate(R.id.action_searchFragment_to_resultsFragment)
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
