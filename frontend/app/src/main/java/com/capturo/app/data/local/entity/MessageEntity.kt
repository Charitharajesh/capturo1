package com.capturo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val bookingId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val isRead: Boolean,
    val createdAt: String
)
