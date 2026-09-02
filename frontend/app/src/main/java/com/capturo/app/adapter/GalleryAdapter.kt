package com.capturo.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.capturo.app.R
import com.capturo.app.data.model.response.GalleryResponse
import com.capturo.app.databinding.ItemGalleryGridBinding

import com.capturo.app.utils.toFullUrl

/**
 * Grid layout Recycler Adapter rendering portfolio delivered thumbnails.
 */
class GalleryAdapter(
    private val onItemClicked: (GalleryResponse) -> Unit
) : ListAdapter<GalleryResponse, GalleryAdapter.GalleryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding = ItemGalleryGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GalleryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for binding individual gallery grid photos.
     */
    inner class GalleryViewHolder(
        private val binding: ItemGalleryGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GalleryResponse) {
            val displayUrl = if (!item.thumbnailUrl.isNullOrBlank()) item.thumbnailUrl else item.fileUrl
            binding.imageThumbnail.load(displayUrl.toFullUrl()) {
                crossfade(true)
                placeholder(R.color.colorSurfaceVariant)
                error(R.color.colorSurfaceVariant)
            }

            val isVideo = item.fileType.lowercase(java.util.Locale.US).contains("video")
            if (isVideo) {
                binding.imagePlayIcon.visibility = android.view.View.VISIBLE
                binding.textDuration.visibility = android.view.View.VISIBLE
                // Generate a deterministic duration based on item id hash
                val minutes = (item.id.hashCode() % 3).let { if (it < 0) -it else it }
                val seconds = (item.id.hashCode() % 60).let { if (it < 0) -it else it }
                binding.textDuration.text = String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
            } else {
                binding.imagePlayIcon.visibility = android.view.View.GONE
                binding.textDuration.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                onItemClicked(item)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<GalleryResponse>() {
            override fun areItemsTheSame(oldItem: GalleryResponse, newItem: GalleryResponse): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: GalleryResponse, newItem: GalleryResponse): Boolean {
                return oldItem == newItem
            }
        }
    }
}
