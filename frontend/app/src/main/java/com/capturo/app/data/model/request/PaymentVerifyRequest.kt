package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class PaymentVerifyRequest(
    @SerializedName("payment_id") val paymentId: String,
    @SerializedName("payment_signature") val paymentSignature: String
)
