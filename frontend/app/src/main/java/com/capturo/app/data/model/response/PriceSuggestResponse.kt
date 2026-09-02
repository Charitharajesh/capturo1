package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class PriceSuggestResponse(
    @SerializedName("suggested_min") val suggestedMin: Double,
    @SerializedName("suggested_max") val suggestedMax: Double,
    @SerializedName("market_avg") val marketAvg: Double,
    @SerializedName("reasoning") val reasoning: String
)
