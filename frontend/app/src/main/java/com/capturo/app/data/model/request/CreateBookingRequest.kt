package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class CreateBookingRequest(
    @SerializedName("creator_id") val creatorId: String,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("location") val location: String,
    @SerializedName("event_date") val eventDate: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("duration_hours") val durationHours: Double,
    @SerializedName("special_notes") val specialNotes: String? = null
)
