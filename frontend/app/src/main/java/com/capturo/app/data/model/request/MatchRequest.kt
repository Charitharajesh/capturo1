package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class BookingContext(
    @SerializedName("event_type") val eventType: String,
    @SerializedName("location") val location: String,
    @SerializedName("budget_inr") val budgetInr: Double,
    @SerializedName("event_date") val eventDate: String,
    @SerializedName("notes") val notes: String? = null
)

data class CreatorInput(
    @SerializedName("id") val id: String,
    @SerializedName("specializations") val specializations: List<String>,
    @SerializedName("hourly_rate") val hourlyRate: Double,
    @SerializedName("avg_rating") val avgRating: Double,
    @SerializedName("distance_km") val distanceKm: Double
)

data class MatchRequest(
    @SerializedName("booking_context") val bookingContext: BookingContext,
    @SerializedName("creators") val creators: List<CreatorInput>
)
