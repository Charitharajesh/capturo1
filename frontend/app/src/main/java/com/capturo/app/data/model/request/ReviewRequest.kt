package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class ReviewRequest(
    @SerializedName("booking_id") val bookingId: String,
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String? = null
)
