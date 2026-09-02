package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class CancelRequest(
    @SerializedName("reason") val reason: String
)
