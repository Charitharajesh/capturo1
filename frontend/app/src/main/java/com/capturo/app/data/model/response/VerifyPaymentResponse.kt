package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class VerifyPaymentResponse(
    @SerializedName("verified") val verified: Boolean,
    @SerializedName("booking_status") val bookingStatus: String
)
