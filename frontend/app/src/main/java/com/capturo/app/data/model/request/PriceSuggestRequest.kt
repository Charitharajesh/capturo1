package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class PriceSuggestRequest(
    @SerializedName("event_type") val eventType: String,
    @SerializedName("duration_hours") val durationHours: Double,
    @SerializedName("city") val city: String,
    @SerializedName("creator_rate") val creatorRate: Double
)
