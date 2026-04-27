package com.example.tripexplorer.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.tripexplorer.R
import com.example.tripexplorer.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityExplorerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        setupLanguageButtons()
        setupRadiusSlider()
        setupClearFavoritesButton()
    }

    private fun setupLanguageButtons() {
        binding.btnEnglish.setOnClickListener {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }

        binding.btnHebrew.setOnClickListener {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("iw"))
        }
    }

    private fun setupRadiusSlider() {
        val savedRadiusKm = viewModel.getSavedSearchRadiusKm()
        binding.sliderSearchRadius.value = savedRadiusKm.toFloat()
        binding.tvRadiusValue.text = getString(R.string.radius_value_km, savedRadiusKm)

        binding.sliderSearchRadius.addOnChangeListener { _, value, fromUser ->
            val radiusKm = value.toInt()
            binding.tvRadiusValue.text = getString(R.string.radius_value_km, radiusKm)
            if (fromUser) {
                viewModel.saveSearchRadiusKm(radiusKm)
            }
        }
    }

    private fun setupClearFavoritesButton() {
        binding.btnClearFavorites.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_favorites_title)
                .setMessage(R.string.clear_favorites_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear) { _, _ ->
                    viewModel.clearAllFavorites()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.favorites_cleared),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
