package com.capturo.app.data.api

import com.capturo.app.data.model.request.*
import com.capturo.app.data.model.response.*
import retrofit2.Response
import retrofit2.http.*

interface ReviewApiService {
    @POST("reviews")
    suspend fun submitReview(
        @Body request: ReviewRequest
    ): Response<ApiResponse<ReviewResponse>>

    @GET("reviews/creator/{creator_id}")
    suspend fun getCreatorReviews(
        @Path("creator_id") creatorId: String,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<PaginatedResponse<ReviewResponse>>

    @GET("reviews/{review_id}")
    suspend fun getReviewDetail(
        @Path("review_id") reviewId: String
    ): Response<ApiResponse<ReviewResponse>>

    @PATCH("reviews/{review_id}")
    suspend fun editReview(
        @Path("review_id") reviewId: String,
        @Body request: UpdateReviewRequest
    ): Response<ApiResponse<ReviewResponse>>

    @DELETE("reviews/{review_id}")
    suspend fun deleteReview(
        @Path("review_id") reviewId: String
    ): Response<ApiResponse<Unit>>
}
