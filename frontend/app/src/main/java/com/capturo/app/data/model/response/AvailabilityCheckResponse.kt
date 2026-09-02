package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class AvailabilityCheckResponse(
    @SerializedName("is_available") val isAvailable: Boolean,
    @SerializedName("conflicting_bookings") val conflictingBookings: List<String> = emptyList()
)
