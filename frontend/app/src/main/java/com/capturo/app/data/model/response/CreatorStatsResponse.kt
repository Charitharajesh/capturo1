package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class CreatorStatsResponse(
    @SerializedName("earnings_this_month") val earningsThisMonth: Double,
    @SerializedName("bookings") val bookings: Int,
    @SerializedName("rating") val rating: Double,
    @SerializedName("earnings_last_month") val earningsLastMonth: Double = 0.0,
    @SerializedName("revenue_growth_percentage") val revenueGrowthPercentage: Double = 0.0
)
