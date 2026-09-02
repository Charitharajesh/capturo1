package com.capturo.app.domain.model

data class Creator(
    val id: String,
    val userId: String,
    val fullName: String,
    val specialization: String,
    val rating: Double,
    val hourlyRate: Double,
    val profilePicUrl: String? = null
)
