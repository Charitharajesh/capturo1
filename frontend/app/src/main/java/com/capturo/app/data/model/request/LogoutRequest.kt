package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class LogoutRequest(
    @SerializedName("refresh_token") val refreshToken: String
)
