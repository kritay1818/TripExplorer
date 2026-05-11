package com.example.tripexplorer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tripexplorer.R
import com.example.tripexplorer.data.remote.PlaceFeature
import com.example.tripexplorer.databinding.ItemPlaceBinding
import java.util.Locale
import kotlinx.coroutines.launch

class PlacesAdapter(
    private val onItemClick: (PlaceFeature) -> Unit,
    private val onFetchImage: suspend (String) -> String?
) : ListAdapter<PlaceFeature, PlacesAdapter.PlaceViewHolder>(PlaceDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaceViewHolder(
        val binding: ItemPlaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(place: PlaceFeature) {
            val context = binding.root.context
            binding.ivPlaceIcon.setImageResource(android.R.drawable.ic_menu_mapmode)
            binding.tvPlaceName.text = place.properties.name.ifBlank { context.getString(R.string.unknown_place) }
            binding.tvPlaceKinds.text = formatKinds(place.properties.kinds)
            binding.tvPlaceRate.text =
                context.getString(R.string.rating_format, formatRate(place.properties.rate))
            binding.root.setOnClickListener { onItemClick(place) }

            itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                val imageUrl = onFetchImage(place.properties.xid)
                if (imageUrl != null) {
                    val glideUrl = com.bumptech.glide.load.model.GlideUrl(
                        imageUrl,
                        com.bumptech.glide.load.model.LazyHeaders.Builder()
                            .addHeader("User-Agent", "CityExplorerApp/1.0")
                            .build()
                    )

                    com.bumptech.glide.Glide.with(itemView.context)
                        .load(glideUrl)
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_mapmode)
                        .error(android.R.drawable.ic_menu_gallery)
                        .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade())
                        .into(binding.ivPlaceIcon)
                } else {
                    binding.ivPlaceIcon.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        }

        private fun formatKinds(kinds: String): String {
            return kinds
                .replace("_", " ")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(3)
                .map { category ->
                    category.split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { word ->
                            word.lowercase(Locale.getDefault())
                                .replaceFirstChar { first ->
                                    if (first.isLowerCase()) {
                                        first.titlecase(Locale.getDefault())
                                    } else {
                                        first.toString()
                                    }
                                }
                        }
                }
                .joinToString(" • ")
                .ifBlank { binding.root.context.getString(R.string.general_category) }
        }

        private fun formatRate(rate: Double?): String {
            if (rate == null) return binding.root.context.getString(R.string.not_available_short)
            return if (rate % 1.0 == 0.0) rate.toInt().toString() else String.format("%.1f", rate)
        }
    }

    override fun onViewRecycled(holder: PlaceViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(holder.itemView).clear(holder.binding.ivPlaceIcon)
        holder.binding.ivPlaceIcon.setImageResource(android.R.drawable.ic_menu_mapmode)
    }

    private object PlaceDiffCallback : DiffUtil.ItemCallback<PlaceFeature>() {
        override fun areItemsTheSame(oldItem: PlaceFeature, newItem: PlaceFeature): Boolean {
            return oldItem.properties.xid == newItem.properties.xid
        }

        override fun areContentsTheSame(oldItem: PlaceFeature, newItem: PlaceFeature): Boolean {
            return oldItem == newItem
        }
    }
}
