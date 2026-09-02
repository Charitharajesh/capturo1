package com.capturo.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.capturo.app.R
import com.capturo.app.data.model.response.BookingResponse
import com.capturo.app.databinding.ItemBookingCardBinding
import com.capturo.app.utils.toIndianPrice
import com.capturo.app.utils.toFullUrl
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.preferences.UserRole
import java.util.Locale

/**
 * Recycler Adapter for binding reservation requests on the Active Bookings screen.
 * Employs clean architectural patterns and custom currency validation utilities.
 */
class BookingListAdapter(
    private val sessionManager: SessionManager,
    private val onBookingClicked: (BookingResponse) -> Unit,
    private val onChatClicked: (BookingResponse) -> Unit,
    private val onCallClicked: (BookingResponse) -> Unit
) : ListAdapter<BookingResponse, BookingListAdapter.BookingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for managing and displaying booking details on card views.
     */
    inner class BookingViewHolder(
        private val binding: ItemBookingCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: BookingResponse) {
            val context = binding.root.context

            // Bind creator metadata
            val role = sessionManager.getUserRole()
            val isCreator = role == UserRole.CREATOR

            if (isCreator) {
                val attendee = booking.attendee
                binding.textCreatorName.text = attendee?.fullName ?: "Attendee"
                binding.textSpecialty.text = "Client"
                binding.imageAvatar.load(attendee?.profilePicUrl.toFullUrl()) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                }
            } else {
                val creator = booking.creator
                binding.textCreatorName.text = creator?.fullName ?: "Photographer"
                binding.textSpecialty.text = creator?.email?.split("@")?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Photographer"
                binding.imageAvatar.load(creator?.profilePicUrl.toFullUrl()) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                }
            }

            // Bind booking details
            binding.textLocation.text = booking.location
            binding.textDate.text = booking.eventDate
            binding.textTime.text = booking.startTime.substring(0, 5) // HH:mm format
            
            // Format price using the custom currency formatting extension
            binding.textTotalAmount.text = booking.totalAmount.toIndianPrice()

            // Bind status color elements
            val status = booking.status.lowercase(Locale.US)
            val colorRes = when (status) {
                "confirmed", "paid" -> R.color.colorOnlineStatus
                "pending" -> R.color.colorWarning
                "completed" -> R.color.colorTextSecondary
                else -> R.color.colorError
            }
            val colorVal = ContextCompat.getColor(context, colorRes)

            binding.viewStatusStrip.setBackgroundColor(colorVal)
            binding.textStatusBadge.text = booking.status.uppercase(Locale.US)
            binding.textStatusBadge.setTextColor(colorVal)

            // Setup Badge Background Tint dynamically
            val bgTintRes = when (status) {
                "confirmed", "paid" -> R.color.colorSuccessBg
                "pending" -> R.color.colorWarningBg
                "completed" -> R.color.colorBottomNavIndicator
                else -> R.color.colorErrorBg
            }
            binding.cardStatusBadge.setCardBackgroundColor(ContextCompat.getColor(context, bgTintRes))

            // Action triggers
            binding.root.setOnClickListener {
                onBookingClicked(booking)
            }

            binding.buttonChat.setOnClickListener {
                onChatClicked(booking)
            }

            binding.buttonCall.setOnClickListener {
                onCallClicked(booking)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<BookingResponse>() {
            override fun areItemsTheSame(oldItem: BookingResponse, newItem: BookingResponse): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: BookingResponse, newItem: BookingResponse): Boolean {
                return oldItem == newItem
            }
        }
    }
}
