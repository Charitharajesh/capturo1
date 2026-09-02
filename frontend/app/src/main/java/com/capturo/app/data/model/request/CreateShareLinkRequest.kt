package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class CreateShareLinkRequest(
    @SerializedName("expires_in_days") val expiresInDays: Int = 30
)
