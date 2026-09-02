package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("notification_type") val notificationType: String,
    @SerializedName("reference_id") val referenceId: String? = null,
    @SerializedName("reference_type") val referenceType: String? = null,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("read_at") val readAt: String? = null,
    @SerializedName("created_at") val createdAt: String
)
