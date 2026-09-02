package com.capturo.app.data.api

import com.capturo.app.data.model.response.*
import com.capturo.app.data.model.request.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CreatorApiService {
    @GET("creators/nearby")
    suspend fun getNearbyCreators(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radiusKm: Double,
        @Query("specialization") specialization: String? = null
    ): Response<ApiResponse<List<CreatorDistanceResponse>>>

    @GET("creators")
    suspend fun getCreators(
        @Query("specialization") specialization: String? = null,
        @Query("min_rating") minRating: Double? = null,
        @Query("max_rate") maxRate: Double? = null
    ): Response<PaginatedResponse<CreatorResponse>>

    @GET("creators/following")
    suspend fun getFollowingCreators(): Response<ApiResponse<List<CreatorResponse>>>

    @GET("creators/{id}")
    suspend fun getCreatorById(@Path("id") id: String): Response<ApiResponse<CreatorPublicResponse>>

    @GET("creators/{id}/availability")
    suspend fun checkAvailability(
        @Path("id") id: String,
        @Query("date") date: String,
        @Query("start_time") startTime: String,
        @Query("duration") duration: Double
    ): Response<ApiResponse<AvailabilityCheckResponse>>

    @GET("creators/me")
    suspend fun getOwnCreatorProfile(): Response<ApiResponse<CreatorProfileResponse>>

    @retrofit2.http.PATCH("creators/me")
    suspend fun updateOwnCreatorProfile(
        @retrofit2.http.Body request: com.capturo.app.data.model.request.UpdateCreatorProfileRequest
    ): Response<ApiResponse<CreatorProfileResponse>>

    @GET("creators/{id}/stats")
    suspend fun getCreatorStats(@Path("id") id: String): Response<ApiResponse<CreatorStatsResponse>>

    @retrofit2.http.POST("ai/creators/match")
    suspend fun aiRankCreators(
        @retrofit2.http.Body request: com.capturo.app.data.model.request.MatchRequest
    ): Response<ApiResponse<com.capturo.app.data.model.response.MatchResponse>>

    @retrofit2.http.POST("ai/reviews/summarise")
    suspend fun aiSummariseReviews(
        @retrofit2.http.Body request: com.capturo.app.data.model.request.ReviewSummariseRequest
    ): Response<ApiResponse<com.capturo.app.data.model.response.ReviewSummariseResponse>>

    @retrofit2.http.POST("creators/{id}/follow")
    suspend fun followCreator(@Path("id") id: String): Response<ApiResponse<Map<String, Any>>>

    @retrofit2.http.POST("creators/{id}/unfollow")
    suspend fun unfollowCreator(@Path("id") id: String): Response<ApiResponse<Map<String, Any>>>
}
