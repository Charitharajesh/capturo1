package com.capturo.app.domain.model

data class Booking(
    val id: String,
    val creatorId: String,
    val attendeeId: String,
    val eventType: String,
    val location: String,
    val eventDate: String,
    val startTime: String,
    val durationHours: Double,
    val totalAmount: Double,
    val status: String
)
