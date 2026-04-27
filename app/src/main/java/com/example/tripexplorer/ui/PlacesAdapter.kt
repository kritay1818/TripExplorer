package com.example.tripexplorer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.tripexplorer.R
import com.example.tripexplorer.data.remote.PlaceFeature
import com.example.tripexplorer.databinding.ItemPlaceBinding
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class PlacesAdapter(
    private val onItemClick: (PlaceFeature) -> Unit,
    private val onFetchImage: suspend (String) -> String?
) : ListAdapter<PlaceFeature, PlacesAdapter.PlaceViewHolder>(PlaceDiffCallback) {
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val imageUrlCache = mutableMapOf<String, String?>()
    private val fetchSemaphore = Semaphore(permits = 2)

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

        fun bind(item: PlaceFeature) {
            val context = binding.root.context
            binding.ivPlaceIcon.setImageResource(android.R.drawable.ic_menu_mapmode)
            binding.ivPlaceIcon.tag = item.properties.xid
            binding.tvPlaceName.text = item.properties.name.ifBlank { context.getString(R.string.unknown_place) }
            binding.tvPlaceKinds.text = formatKinds(item.properties.kinds)
            binding.tvPlaceRate.text =
                context.getString(R.string.rating_format, formatRate(item.properties.rate))
            binding.root.setOnClickListener { onItemClick(item) }

            val cachedUrl = imageUrlCache[item.properties.xid]
            if (imageUrlCache.containsKey(item.properties.xid)) {
                if (!cachedUrl.isNullOrBlank()) {
                    loadImageIntoIcon(item, cachedUrl)
                }
                return
            }

            adapterScope.launch {
                val imageUrl = fetchSemaphore.withPermit {
                    onFetchImage(item.properties.xid)
                }
                imageUrlCache[item.properties.xid] = imageUrl
                if (!imageUrl.isNullOrBlank()) {
                    loadImageIntoIcon(item, imageUrl)
                }
            }
        }

        private fun loadImageIntoIcon(item: PlaceFeature, imageUrl: String) {
            if (binding.ivPlaceIcon.tag != item.properties.xid) return

            val glideModel = GlideUrl(
                imageUrl,
                LazyHeaders.Builder()
                    .addHeader("User-Agent", "TripExplorer-Android/1.0")
                    .build()
            )

            Glide.with(binding.ivPlaceIcon)
                .load(glideModel)
                .placeholder(android.R.drawable.ic_menu_mapmode)
                .error(android.R.drawable.ic_menu_report_image)
                .fallback(android.R.drawable.ic_menu_mapmode)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        android.util.Log.e(
                            "TripExplorer_Image",
                            "Glide failed for xid=${item.properties.xid}, url=$imageUrl",
                            e
                        )
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean = false
                })
                .into(binding.ivPlaceIcon)
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

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapterScope.cancel()
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
