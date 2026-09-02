package com.capturo.app.data.repository

import com.capturo.app.data.api.ReviewApiService
import com.capturo.app.data.model.request.*
import com.capturo.app.data.model.response.*
import com.capturo.app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val api: ReviewApiService
) {
    fun submitReview(request: ReviewRequest): Flow<Resource<ReviewResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.submitReview(request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to submit review"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getCreatorReviews(creatorId: String, page: Int? = null, perPage: Int? = null): Flow<Resource<PaginatedResponse<ReviewResponse>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getCreatorReviews(creatorId, page, perPage)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to fetch creator reviews"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getReviewDetail(reviewId: String): Flow<Resource<ReviewResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getReviewDetail(reviewId)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to retrieve review details"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun editReview(reviewId: String, request: UpdateReviewRequest): Flow<Resource<ReviewResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.editReview(reviewId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to update review"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteReview(reviewId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.deleteReview(reviewId)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to delete review"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)
}
