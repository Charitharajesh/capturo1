package com.capturo.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.capturo.app.R
import com.capturo.app.data.model.response.CreatorDistanceResponse
import com.capturo.app.databinding.ItemCreatorCardBinding
import com.capturo.app.utils.LocationUtils
import com.capturo.app.utils.toIndianPrice
import com.capturo.app.utils.toFullUrl
import java.util.Locale

/**
 * Premium Recycler Adapter for binding Creator cards on Search Lists and Home feeds.
 * Implements optimized ListAdapter DiffUtil operations and integrates custom formatting extensions.
 */
class CreatorListAdapter(
    private val onCreatorClicked: (CreatorDistanceResponse) -> Unit
) : ListAdapter<CreatorDistanceResponse, CreatorListAdapter.CreatorViewHolder>(DiffCallback) {

    // Dynamic callback lambda exposed for Fragment-level navigation operations
    var onCreatorClick: ((CreatorDistanceResponse) -> Unit)? = onCreatorClicked

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreatorViewHolder {
        val binding = ItemCreatorCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CreatorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CreatorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder managing layout views and data associations.
     */
    inner class CreatorViewHolder(
        private val binding: ItemCreatorCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CreatorDistanceResponse) {
            val creator = item.creator
            binding.textName.text = creator.fullName
            
            // Format price utilizing the new custom Extension function
            binding.textPrice.text = "${creator.hourlyRate.toIndianPrice()}/hr"
            binding.textRating.text = String.format(Locale.US, "%.1f", creator.avgRating)
            binding.textReviewsCount.text = "(${creator.totalReviews})"
            
            // Format distance utilizing the new LocationUtils Haversine calculations
            binding.textDistance.text = LocationUtils.formatDistance(item.distanceKm)

            // Set availability status and active green online indicator dot
            val isAvailable = creator.availabilityStatus.lowercase(Locale.US) == "available"
            binding.viewOnlineDot.visibility = if (isAvailable) View.VISIBLE else View.INVISIBLE
            binding.textAvailability.visibility = if (isAvailable) View.VISIBLE else View.INVISIBLE

            // Default specialty designation from email prefix or generic standard
            binding.textSpecialty.text = creator.email.split("@").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Photographer"

            // Async avatar load using Coil with crossfades and premium crop transformations
            binding.imageAvatar.load(creator.profilePicUrl.toFullUrl()) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_profile)
                error(R.drawable.ic_profile)
            }

            // Expose the click callback listener to root parent layout
            binding.root.setOnClickListener {
                onCreatorClick?.invoke(item)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CreatorDistanceResponse>() {
            override fun areItemsTheSame(oldItem: CreatorDistanceResponse, newItem: CreatorDistanceResponse): Boolean {
                return oldItem.creator.id == newItem.creator.id
            }

            override fun areContentsTheSame(oldItem: CreatorDistanceResponse, newItem: CreatorDistanceResponse): Boolean {
                return oldItem == newItem
            }
        }
    }
}
