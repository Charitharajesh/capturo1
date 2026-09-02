package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class ReviewSummariseResponse(
    @SerializedName("summary") val summary: String,
    @SerializedName("highlights") val highlights: List<String>,
    @SerializedName("watch_out") val watchOut: String? = null,
    @SerializedName("sentiment_score") val sentimentScore: Double
)
