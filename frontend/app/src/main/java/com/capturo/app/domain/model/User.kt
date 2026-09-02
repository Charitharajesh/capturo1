package com.capturo.app.domain.model

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String? = null,
    val profilePicUrl: String? = null,
    val role: String
)
// Type alias or sub-models can represent specific role contexts if required by use cases
