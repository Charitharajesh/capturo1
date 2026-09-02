package com.capturo.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.capturo.app.data.model.response.BookingResponse
import com.capturo.app.databinding.ItemBookingRequestBinding
import com.capturo.app.utils.DateTimeUtils
import com.capturo.app.utils.toIndianPrice

/**
 * ListAdapter mapping pending booking requests with action buttons for Accept/Decline operations.
 */
class BookingRequestAdapter(
    private val onAcceptClick: (BookingResponse) -> Unit,
    private val onDeclineClick: (BookingResponse) -> Unit
) : ListAdapter<BookingResponse, BookingRequestAdapter.BookingRequestViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingRequestViewHolder {
        val binding = ItemBookingRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingRequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingRequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for binding pending booking information.
     */
    inner class BookingRequestViewHolder(private val binding: ItemBookingRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: BookingResponse) {
            // Row 1: Event Type Chip + Price using our custom currency formatter extension
            binding.tvEventType.text = booking.eventType.replaceFirstChar { it.uppercase() }
            binding.tvPrice.text = booking.totalAmount.toIndianPrice()

            // Row 2: Client Info
            binding.tvClientName.text = "Client: ${booking.attendeeId?.take(8) ?: "N/A"}"
            binding.tvDuration.text = "${booking.durationHours.toInt()} hours"

            // Row 3: Calendar Date + Time range
            val bookingDateTimeStr = "${booking.eventDate}T${booking.startTime}Z"
            val displayDate = try {
                val eventZdt = DateTimeUtils.parseFromBackend(bookingDateTimeStr)
                DateTimeUtils.formatDisplayDate(eventZdt)
            } catch (e: Exception) {
                booking.eventDate
            }
            binding.tvDate.text = displayDate
            binding.tvTime.text = booking.startTime

            // Row 4: Location/Venue
            binding.tvVenue.text = booking.location

            // Actions Listeners
            binding.btnAccept.setOnClickListener { onAcceptClick(booking) }
            binding.btnDecline.setOnClickListener { onDeclineClick(booking) }
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
