package com.example.tripexplorer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tripexplorer.R
import com.example.tripexplorer.data.remote.PlaceFeature
import com.example.tripexplorer.databinding.ItemPlaceBinding

class PlacesAdapter(
    private val onItemClick: (PlaceFeature) -> Unit
) : ListAdapter<PlaceFeature, PlacesAdapter.ViewHolder>(PlaceDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPlaceBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(place: PlaceFeature) {
            binding.tvPlaceName.text = place.properties.name

            val kindsText = place.properties.kinds
                .replace("_", " ")
                .split(",")
                .take(3)
                .joinToString(" - ") { it.replaceFirstChar { char -> char.uppercase() } }
            binding.tvPlaceKinds.text = kindsText

            val rateText = place.properties.rate?.toString()
                ?: itemView.context.getString(R.string.not_available_short)
            binding.tvPlaceRate.text = itemView.context.getString(R.string.rating_format, rateText)
            binding.root.setOnClickListener { onItemClick(place) }
            com.bumptech.glide.Glide.with(itemView.context)
                .load(getCategoryIcon(place.properties.kinds))
                .into(binding.ivPlaceIcon)
        }
    }

    private fun getCategoryIcon(kinds: String?): Int {
        if (kinds == null) return R.drawable.ic_place_default
        return when {
            kinds.contains("museum", ignoreCase = true) -> R.drawable.ic_museum
            kinds.contains("nature", ignoreCase = true) ||
                kinds.contains("natural", ignoreCase = true) ||
                kinds.contains("park", ignoreCase = true) ||
                kinds.contains("garden", ignoreCase = true) ||
                kinds.contains("water", ignoreCase = true) ||
                kinds.contains("beach", ignoreCase = true) -> R.drawable.ic_nature
            kinds.contains("food", ignoreCase = true) ||
                kinds.contains("cafe", ignoreCase = true) ||
                kinds.contains("restaurant", ignoreCase = true) -> R.drawable.ic_food
            kinds.contains("historic", ignoreCase = true) ||
                kinds.contains("monument", ignoreCase = true) -> R.drawable.ic_historic
            else -> R.drawable.ic_place_default
        }
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
