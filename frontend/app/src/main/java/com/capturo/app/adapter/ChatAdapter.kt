package com.capturo.app.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.capturo.app.R
import com.capturo.app.data.local.entity.MessageEntity
import com.capturo.app.databinding.ItemMessageReceivedBinding
import com.capturo.app.databinding.ItemMessageSentBinding
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Premium Chat bubble Recycler Adapter rendering message streams.
 * Leverages dual ViewType layouts for incoming versus outgoing threads.
 */
class ChatAdapter(
    private val currentUserId: String
) : ListAdapter<MessageEntity, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2

        private val DiffCallback = object : DiffUtil.ItemCallback<MessageEntity>() {
            override fun areItemsTheSame(oldItem: MessageEntity, newItem: MessageEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MessageEntity, newItem: MessageEntity): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val binding = ItemMessageSentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            SentViewHolder(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            ReceivedViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SentViewHolder -> holder.bind(item)
            is ReceivedViewHolder -> holder.bind(item)
        }
    }

    /**
     * ViewHolder for outgoing messages sent by the current authenticated user.
     */
    inner class SentViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageEntity) {
            binding.textMessageContent.text = message.content
            binding.textTimestamp.text = formatTime(message.createdAt)
            
            // Toggle read receipt icon tick visibility & set color tint
            binding.imageReadReceipt.visibility = View.VISIBLE
            if (message.isRead) {
                // Blue tick for read messages
                binding.imageReadReceipt.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.colorAccent)
                )
            } else {
                // Gray tick for unread/sent messages
                binding.imageReadReceipt.setColorFilter(Color.GRAY)
            }
        }
    }

    /**
     * ViewHolder for incoming messages received from the counterparty.
     */
    inner class ReceivedViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageEntity) {
            binding.textMessageContent.text = message.content
            binding.textTimestamp.text = formatTime(message.createdAt)
        }
    }

    /**
     * Formats database ZonedDateTime strings into simple 12-hour formatted time (e.g. '10:30 AM').
     */
    private fun formatTime(isoString: String): String {
        return try {
            val zdt = ZonedDateTime.parse(isoString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            zdt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
        } catch (e: Exception) {
            "10:30 AM"
        }
    }
}
