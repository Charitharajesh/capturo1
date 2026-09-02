package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class DisputeBookingRequest(
    @SerializedName("reason") val reason: String,
    @SerializedName("evidence_urls") val evidenceUrls: List<String>? = null
)
