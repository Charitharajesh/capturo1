package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class UpdateCreatorRequest(
    @SerializedName("specializations") val specializations: List<String>? = null,
    @SerializedName("hourly_rate") val hourlyRate: Double? = null,
    @SerializedName("minimum_hours") val minimumHours: Int? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("years_experience") val yearsExperience: Int? = null,
    @SerializedName("equipment") val equipment: List<String>? = null,
    @SerializedName("availability_status") val availabilityStatus: String? = null,
    @SerializedName("service_radius_km") val serviceRadiusKm: Int? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)
