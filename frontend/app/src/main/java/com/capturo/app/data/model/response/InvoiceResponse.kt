package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class InvoiceResponse(
    @SerializedName("invoice_url") val invoiceUrl: String,
    @SerializedName("generated_at") val generatedAt: String
)
