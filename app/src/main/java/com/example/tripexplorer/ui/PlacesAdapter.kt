package com.example.tripexplorer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tripexplorer.data.remote.PlaceFeature
import com.example.tripexplorer.databinding.ItemPlaceBinding
import kotlinx.coroutines.launch

class PlacesAdapter(
    private val onItemClick: (PlaceFeature) -> Unit,
    private val onFetchImage: suspend (String) -> String?
) : ListAdapter<PlaceFeature, PlacesAdapter.ViewHolder>(PlaceDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPlaceBinding) : RecyclerView.ViewHolder(binding.root) {
        private var fetchJob: kotlinx.coroutines.Job? = null

        fun bind(place: PlaceFeature) {
            // Bind text fields
            binding.tvPlaceName.text = place.properties.name

            val kindsText = place.properties.kinds
                .replace("_", " ")
                .split(",")
                .take(3)
                .joinToString(" • ") { it.replaceFirstChar { char -> char.uppercase() } }
            binding.tvPlaceKinds.text = kindsText

            binding.tvPlaceRate.text = "⭐ Rating: ${place.properties.rate ?: "N/A"}"
            binding.root.setOnClickListener { onItemClick(place) }

            // Reset image to loading state immediately to prevent flickering
            binding.ivPlaceIcon.setImageResource(android.R.drawable.ic_menu_mapmode)

            // Cancel any previous image fetch job for this recycled view
            fetchJob?.cancel()

            // Launch a new fetch job
            fetchJob = itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                val imageUrl = onFetchImage(place.properties.xid)
                if (imageUrl != null) {
                    // Mimic a real mobile browser to prevent Wikimedia 400 Bad Request blocks
                    val glideUrl = com.bumptech.glide.load.model.GlideUrl(
                        imageUrl,
                        com.bumptech.glide.load.model.LazyHeaders.Builder()
                            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
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

        fun recycle() {
            fetchJob?.cancel()
            fetchJob = null
            Glide.with(itemView).clear(binding.ivPlaceIcon)
            binding.ivPlaceIcon.setImageResource(android.R.drawable.ic_menu_mapmode)
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
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
