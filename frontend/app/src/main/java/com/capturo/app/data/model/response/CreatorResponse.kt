package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class CreatorResponse(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("profile_pic_url") val profilePicUrl: String? = null,
    @SerializedName("hourly_rate") val hourlyRate: Double,
    @SerializedName("avg_rating") val avgRating: Double,
    @SerializedName("total_reviews") val totalReviews: Int,
    @SerializedName("availability_status") val availabilityStatus: String,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("ai_score") val aiScore: Int? = null,
    @SerializedName("ai_reason") val aiReason: String? = null
)
