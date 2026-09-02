package com.capturo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val creatorId: String? = null,
    val status: String,
    val totalAmount: Double,
    val date: String
)
