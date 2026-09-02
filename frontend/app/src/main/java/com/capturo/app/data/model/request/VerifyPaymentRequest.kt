package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class VerifyPaymentRequest(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("payment_id") val paymentId: String,
    @SerializedName("signature") val signature: String
)
