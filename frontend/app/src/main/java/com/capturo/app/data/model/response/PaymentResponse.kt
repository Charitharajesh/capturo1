package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class PaymentResponse(
    @SerializedName("id") val id: String,
    @SerializedName("booking_id") val bookingId: String,
    @SerializedName("payer_id") val payerId: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("refund_amount") val refundAmount: Double,
    @SerializedName("currency") val currency: String,
    @SerializedName("gateway") val gateway: String,
    @SerializedName("gateway_order_id") val gatewayOrderId: String? = null,
    @SerializedName("gateway_payment_id") val gatewayPaymentId: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("captured_at") val capturedAt: String? = null,
    @SerializedName("refunded_at") val refundedAt: String? = null,
    @SerializedName("created_at") val createdAt: String
)
