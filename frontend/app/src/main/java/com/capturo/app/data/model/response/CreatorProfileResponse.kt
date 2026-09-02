package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class CreatorProfileResponse(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("specializations") val specializations: List<String>,
    @SerializedName("hourly_rate") val hourlyRate: Double,
    @SerializedName("minimum_hours") val minimumHours: Int,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("years_experience") val yearsExperience: Int? = null,
    @SerializedName("equipment") val equipment: List<String>? = null,
    @SerializedName("availability_status") val availabilityStatus: String,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("service_radius_km") val serviceRadiusKm: Int,
    @SerializedName("avg_rating") val avgRating: Double,
    @SerializedName("total_reviews") val totalReviews: Int,
    @SerializedName("total_bookings") val totalBookings: Int,
    @SerializedName("on_time_rate") val onTimeRate: Double,
    @SerializedName("is_featured") val isFeatured: Boolean,
    @SerializedName("followers_count") val followersCount: Int = 0,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)
