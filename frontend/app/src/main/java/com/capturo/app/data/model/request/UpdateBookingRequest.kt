package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class UpdateBookingRequest(
    @SerializedName("location") val location: String? = null,
    @SerializedName("special_notes") val specialNotes: String? = null
)
