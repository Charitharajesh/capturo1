package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class VerifyEmailRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String
)
