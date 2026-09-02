package com.capturo.app.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.capturo.app.R
import com.capturo.app.data.model.response.NotificationResponse
import com.capturo.app.databinding.ItemNotificationBinding
import com.capturo.app.databinding.ItemNotificationHeaderBinding
import com.capturo.app.utils.DateTimeUtils
import com.capturo.app.utils.toRelativeTime

sealed class NotificationListItem {
    data class Header(val title: String) : NotificationListItem()
    data class Item(val notification: NotificationResponse) : NotificationListItem()
}

class NotificationAdapter(
    private val onItemClick: (NotificationResponse) -> Unit
) : ListAdapter<NotificationListItem, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NotificationListItem.Header -> VIEW_TYPE_HEADER
            is NotificationListItem.Item -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemNotificationHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            VIEW_TYPE_ITEM -> {
                val binding = ItemNotificationBinding.inflate(inflater, parent, false)
                ItemViewHolder(binding, onItemClick)
            }
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is NotificationListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is NotificationListItem.Item -> (holder as ItemViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(
        private val binding: ItemNotificationHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: NotificationListItem.Header) {
            binding.textHeader.text = header.title
        }
    }

    class ItemViewHolder(
        private val binding: ItemNotificationBinding,
        private val onItemClick: (NotificationResponse) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationListItem.Item) {
            val notification = item.notification
            val context = binding.root.context

            binding.textTitle.text = notification.title
            binding.textBody.text = notification.body
            binding.textTime.text = notification.createdAt.toRelativeTime()

            // Background & Ripple decoration for Unread vs Read
            if (!notification.isRead) {
                binding.root.setBackgroundColor(Color.parseColor("#1A003388")) // Unread color
                binding.viewUnreadDot.visibility = View.VISIBLE
            } else {
                binding.root.setBackgroundColor(Color.TRANSPARENT) // Read color
                binding.viewUnreadDot.visibility = View.GONE
            }

            // Type-specific icon and background coloring
            val (iconRes, iconBgColorHex) = when (notification.notificationType) {
                "booking_confirmed" -> Pair(R.drawable.ic_check_circle, "#00E676")
                "new_message" -> Pair(R.drawable.ic_messages, "#40C4FF")
                "payment_captured" -> Pair(R.drawable.ic_payments, "#E040FB")
                "review_requested" -> Pair(R.drawable.ic_star, "#FFD700")
                "upload_ready" -> Pair(R.drawable.ic_cloud_done, "#7B2FBE")
                else -> Pair(R.drawable.ic_notifications, "#7B2FBE")
            }

            // Bind the correct icon
            binding.imgNotificationIcon.setImageResource(iconRes)
            binding.imgNotificationIcon.setColorFilter(Color.WHITE)

            // Setup type-specific color with 15% opacity on icon background circle
            val circleBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                val colorInt = Color.parseColor(iconBgColorHex)
                val alphaColor = Color.argb(
                    38, // 15% of 255
                    Color.red(colorInt),
                    Color.green(colorInt),
                    Color.blue(colorInt)
                )
                setColor(alphaColor)
            }
            binding.layoutIconContainer.background = circleBg

            binding.root.setOnClickListener {
                onItemClick(notification)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1

        private val DiffCallback = object : DiffUtil.ItemCallback<NotificationListItem>() {
            override fun areItemsTheSame(oldItem: NotificationListItem, newItem: NotificationListItem): Boolean {
                return when {
                    oldItem is NotificationListItem.Header && newItem is NotificationListItem.Header ->
                        oldItem.title == newItem.title
                    oldItem is NotificationListItem.Item && newItem is NotificationListItem.Item ->
                        oldItem.notification.id == newItem.notification.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: NotificationListItem, newItem: NotificationListItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
