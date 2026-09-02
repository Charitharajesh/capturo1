package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class CreatePaymentOrderRequest(
    @SerializedName("booking_id") val bookingId: String
)
