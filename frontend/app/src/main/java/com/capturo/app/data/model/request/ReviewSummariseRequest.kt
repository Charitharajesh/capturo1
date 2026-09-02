package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class ReviewInput(
    @SerializedName("comment") val comment: String,
    @SerializedName("rating") val rating: Int
)

data class ReviewSummariseRequest(
    @SerializedName("creator_id") val creatorId: String,
    @SerializedName("creator_name") val creatorName: String,
    @SerializedName("reviews") val reviews: List<ReviewInput>
)
