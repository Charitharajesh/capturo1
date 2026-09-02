package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class GalleryResponse(
    @SerializedName("id") val id: String,
    @SerializedName("creator_id") val creatorId: String,
    @SerializedName("booking_id") val bookingId: String? = null,
    @SerializedName("file_url") val fileUrl: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("file_type") val fileType: String,
    @SerializedName("file_size_bytes") val fileSizeBytes: Long,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("is_portfolio") val isPortfolio: Boolean,
    @SerializedName("is_client_delivery") val isClientDelivery: Boolean,
    @SerializedName("views_count") val viewsCount: Int,
    @SerializedName("created_at") val createdAt: String
)
