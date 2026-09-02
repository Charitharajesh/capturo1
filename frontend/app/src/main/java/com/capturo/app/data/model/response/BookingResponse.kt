package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class BookingResponse(
    @SerializedName("id") val id: String,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("location") val location: String,
    @SerializedName("event_date") val eventDate: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("duration_hours") val durationHours: Double,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("attendee_id") val attendeeId: String? = null,
    @SerializedName("creator_id") val creatorId: String? = null,
    @SerializedName("special_notes") val specialNotes: String? = null,
    @SerializedName("invoice_url") val invoiceUrl: String? = null,
    @SerializedName("cancelled_by") val cancelledBy: String? = null,
    @SerializedName("cancellation_reason") val cancellationReason: String? = null,
    @SerializedName("creator") val creator: CreatorResponse? = null,
    @SerializedName("attendee") val attendee: UserResponse? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

