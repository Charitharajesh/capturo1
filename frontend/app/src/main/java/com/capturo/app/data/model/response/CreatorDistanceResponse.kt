package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class CreatorDistanceResponse(
    @SerializedName("creator") val creator: CreatorResponse,
    @SerializedName("distance_km") val distanceKm: Double
)
