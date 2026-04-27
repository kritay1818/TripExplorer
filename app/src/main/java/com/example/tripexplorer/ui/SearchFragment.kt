package com.example.tripexplorer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.example.tripexplorer.R
import com.example.tripexplorer.databinding.FragmentSearchBinding
import com.example.tripexplorer.utils.ResultState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityExplorerViewModel by activityViewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var shouldNavigateToResults = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            fetchCurrentLocationAndSearch()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.location_permission_needed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupSearchClickListener()
        setupNearMeClickListener()
        observeSearchResults()
    }

    private fun setupSearchClickListener() {
        binding.btnSearch.setOnClickListener {
            val cityName = binding.etSearchCity.text?.toString()?.trim().orEmpty()

            if (cityName.isBlank()) {
                binding.tilSearchCity.error = getString(R.string.city_name_empty_error)
                return@setOnClickListener
            }

            binding.tilSearchCity.error = null
            shouldNavigateToResults = true
            viewModel.searchPlaces(cityName)
        }
    }

    private fun setupNearMeClickListener() {
        binding.btnNearMe.setOnClickListener {
            requestLocationPermissionsOrFetch()
        }
    }

    private fun requestLocationPermissionsOrFetch() {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchCurrentLocationAndSearch()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchCurrentLocationAndSearch() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        shouldNavigateToResults = true
                        viewModel.searchPlacesByLocation(location.latitude, location.longitude)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.location_unavailable_try_again),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.location_fetch_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (_: SecurityException) {
            Toast.makeText(
                requireContext(),
                getString(R.string.location_permission_required),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeSearchResults() {
        viewModel.searchResults.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ResultState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSearch.visibility = View.GONE
                    binding.btnNearMe.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                }

                is ResultState.Error -> {
                    shouldNavigateToResults = false
                    binding.progressBar.visibility = View.GONE
                    binding.btnSearch.visibility = View.VISIBLE
                    binding.btnNearMe.visibility = View.VISIBLE
                    binding.tvError.text = result.message
                    binding.tvError.visibility = View.VISIBLE
                }

                is ResultState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSearch.visibility = View.VISIBLE
                    binding.btnNearMe.visibility = View.VISIBLE
                    binding.tvError.visibility = View.GONE
                    if (shouldNavigateToResults) {
                        shouldNavigateToResults = false
                        findNavController().navigate(R.id.action_searchFragment_to_resultsFragment)
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
