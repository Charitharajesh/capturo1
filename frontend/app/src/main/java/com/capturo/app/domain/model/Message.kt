package com.capturo.app.domain.model

data class Message(
    val id: String,
    val bookingId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val isRead: Boolean,
    val createdAt: String
)
