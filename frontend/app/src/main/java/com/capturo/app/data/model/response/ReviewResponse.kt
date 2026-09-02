package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class ReviewResponse(
    @SerializedName("id") val id: String,
    @SerializedName("booking_id") val bookingId: String,
    @SerializedName("reviewer_id") val reviewerId: String,
    @SerializedName("creator_id") val creatorId: String,
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean,
    @SerializedName("created_at") val createdAt: String
)
