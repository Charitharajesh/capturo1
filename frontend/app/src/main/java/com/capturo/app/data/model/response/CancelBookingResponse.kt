package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class CancelBookingResponse(
    @SerializedName("status") val status: String,
    @SerializedName("refund_amount") val refundAmount: Double,
    @SerializedName("refund_eta_days") val refundEtaDays: Int = 5
)
