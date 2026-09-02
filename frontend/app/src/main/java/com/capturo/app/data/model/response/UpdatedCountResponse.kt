package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class UpdatedCountResponse(
    @SerializedName("updated_count") val updatedCount: Int
)
