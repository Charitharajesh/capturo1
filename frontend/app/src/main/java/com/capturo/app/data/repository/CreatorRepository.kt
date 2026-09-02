package com.capturo.app.data.repository

import com.capturo.app.data.api.CreatorApiService
import com.capturo.app.data.model.response.*
import com.capturo.app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreatorRepository @Inject constructor(
    private val api: CreatorApiService
) {
    fun getNearbyCreators(lat: Double?, lon: Double?, radius: Double?, specialization: String? = null): Flow<Resource<List<CreatorDistanceResponse>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getNearbyCreators(
                lat ?: 0.0,
                lon ?: 0.0,
                radius ?: 10.0,
                specialization
            )
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to get nearby creators"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getCreators(specialization: String? = null, minRating: Double? = null, maxRate: Double? = null): Flow<Resource<List<CreatorResponse>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getCreators(specialization, minRating, maxRate)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.items))
            } else {
                emit(Resource.Error("Failed to list creators"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getCreatorById(id: String): Flow<Resource<CreatorPublicResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getCreatorById(id)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to load creator profile"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun checkAvailability(id: String, date: String, startTime: String, duration: Double): Flow<Resource<AvailabilityCheckResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.checkAvailability(id, date, startTime, duration)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to check availability"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getCreatorStats(id: String): Flow<Resource<CreatorStatsResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getCreatorStats(id)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to load creator stats"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getOwnCreatorProfile(): Flow<Resource<CreatorProfileResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getOwnCreatorProfile()
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to load own profile"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun updateOwnCreatorProfile(request: com.capturo.app.data.model.request.UpdateCreatorProfileRequest): Flow<Resource<CreatorProfileResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.updateOwnCreatorProfile(request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to update profile"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun followCreator(id: String): Flow<Resource<Map<String, Any>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.followCreator(id)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to follow creator"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun unfollowCreator(id: String): Flow<Resource<Map<String, Any>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.unfollowCreator(id)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to unfollow creator"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun aiSummariseReviews(request: com.capturo.app.data.model.request.ReviewSummariseRequest): Flow<Resource<com.capturo.app.data.model.response.ReviewSummariseResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.aiSummariseReviews(request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to summarise reviews"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getFollowingCreators(): Flow<Resource<List<CreatorResponse>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getFollowingCreators()
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to get followed creators"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)
}
