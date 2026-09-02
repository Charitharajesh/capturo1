package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class PaymentOrderResponse(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("amount") val amount: Int,
    @SerializedName("currency") val currency: String,
    @SerializedName("key_id") val keyId: String
)
