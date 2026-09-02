package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class UpdateReviewRequest(
    @SerializedName("rating") val rating: Int? = null,
    @SerializedName("comment") val comment: String? = null
)
