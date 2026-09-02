package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @SerializedName("id") val id: String,
    @SerializedName("booking_id") val bookingId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("content") val content: String? = null,
    @SerializedName("message_type") val messageType: String,
    @SerializedName("media_url") val mediaUrl: String? = null,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("read_at") val readAt: String? = null,
    @SerializedName("created_at") val createdAt: String
)
