package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class BookingCreatedResponse(
    @SerializedName("booking_id") val bookingId: String,
    @SerializedName("status") val status: String,
    @SerializedName("payment_order_id") val paymentOrderId: String,
    @SerializedName("payment_key_id") val paymentKeyId: String,
    @SerializedName("total_amount") val totalAmount: Double
)
