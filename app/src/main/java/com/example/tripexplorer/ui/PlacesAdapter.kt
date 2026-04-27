package com.example.tripexplorer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tripexplorer.data.remote.PlaceFeature
import com.example.tripexplorer.databinding.ItemPlaceBinding

class PlacesAdapter(
    private val onItemClick: (PlaceFeature) -> Unit
) : ListAdapter<PlaceFeature, PlacesAdapter.PlaceViewHolder>(PlaceDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaceViewHolder(
        private val binding: ItemPlaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlaceFeature) {
            binding.tvPlaceName.text = item.properties.name.ifBlank { "Unknown place" }
            binding.tvPlaceKinds.text = item.properties.kinds
            binding.tvPlaceRate.text = "Rate: ${item.properties.rate ?: "N/A"}"
            binding.root.setOnClickListener { onItemClick(item) }
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
