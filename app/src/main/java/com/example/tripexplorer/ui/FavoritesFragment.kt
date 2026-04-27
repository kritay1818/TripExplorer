package com.example.tripexplorer.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.tripexplorer.R
import com.example.tripexplorer.data.local.PlaceEntity
import com.example.tripexplorer.data.local.toPlaceFeature
import com.example.tripexplorer.databinding.FragmentFavoritesBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityExplorerViewModel by activityViewModels()

    private lateinit var placesAdapter: PlacesAdapter
    private var currentFavorites: List<PlaceEntity> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFavoritesBinding.bind(view)

        setupRecyclerView()
        observeFavorites()
    }

    private fun setupRecyclerView() {
        placesAdapter = PlacesAdapter(
            onItemClick = { placeFeature ->
                val direction =
                    FavoritesFragmentDirections.actionFavoritesFragmentToEditNoteFragment(
                        placeFeature.properties.xid
                    )
                findNavController().navigate(direction)
            },
            onFetchImage = { xid ->
                viewModel.fetchPlaceImageUrl(xid)
            }
        )
        binding.rvFavorites.adapter = placesAdapter

        val swipeCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val swipedPosition = viewHolder.bindingAdapterPosition
                val placeToDelete = currentFavorites.getOrNull(swipedPosition)

                if (placeToDelete != null) {
                    viewModel.removeFromFavorites(placeToDelete)
                    Snackbar.make(binding.root, getString(R.string.deleted_from_favorites), Snackbar.LENGTH_SHORT)
                        .show()
                } else {
                    placesAdapter.notifyItemChanged(swipedPosition)
                }
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvFavorites)
    }

    private fun observeFavorites() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favoritePlaces.collect { favorites ->
                    currentFavorites = favorites
                    placesAdapter.submitList(favorites.map { it.toPlaceFeature() })

                    val isEmpty = favorites.isEmpty()
                    binding.tvEmptyFavorites.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.rvFavorites.visibility = if (isEmpty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
