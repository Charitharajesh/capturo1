package com.capturo.app.data.api

import com.capturo.app.data.model.request.UpdateCreatorRequest
import com.capturo.app.data.model.request.UpdateUserRequest
import com.capturo.app.data.model.response.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface UserApiService {
    @GET("users/me")
    suspend fun getProfile(): Response<ApiResponse<UserResponse>>

    @PATCH("users/me")
    suspend fun updateProfile(@Body request: UpdateUserRequest): Response<ApiResponse<UserResponse>>

    @Multipart
    @POST("users/me/avatar")
    suspend fun updateAvatar(@Part avatar: MultipartBody.Part): Response<ApiResponse<UserResponse>>

    @PATCH("creators/me")
    suspend fun toggleAvailability(@Body request: UpdateCreatorRequest): Response<ApiResponse<CreatorProfileResponse>>
}
