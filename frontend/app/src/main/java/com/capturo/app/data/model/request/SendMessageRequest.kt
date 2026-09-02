package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class SendMessageRequest(
    @SerializedName("booking_id") val bookingId: String,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("content") val content: String? = null,
    @SerializedName("message_type") val messageType: String = "text",
    @SerializedName("media_url") val mediaUrl: String? = null
)
