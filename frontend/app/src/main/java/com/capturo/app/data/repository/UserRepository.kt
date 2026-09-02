package com.capturo.app.data.repository

import com.capturo.app.data.api.UserApiService
import com.capturo.app.data.local.dao.UserDao
import com.capturo.app.data.local.entity.UserEntity
import com.capturo.app.data.model.response.UserResponse
import com.capturo.app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: UserApiService,
    private val userDao: UserDao
) {
    fun getProfile(): Flow<Resource<UserResponse>> = flow {
        emit(Resource.Loading)
        
        // Emitting cached user if exists
        try {
            val cachedUser = userDao.getUser()
            if (cachedUser != null) {
                emit(Resource.Success(
                    UserResponse(
                        id = cachedUser.id,
                        fullName = cachedUser.fullName,
                        email = cachedUser.email,
                        role = cachedUser.role,
                        phone = null,
                        profilePicUrl = null,
                        isActive = true,
                        isVerified = true,
                        lastLoginAt = null,
                        createdAt = "",
                        updatedAt = ""
                    )
                ))
            }
        } catch (e: Exception) {
            // Silently ignore cache read error
        }

        // Fetch fresh profile from API
        try {
            val response = api.getProfile()
            if (response.isSuccessful && response.body()?.success == true) {
                val user = response.body()!!.data!!
                userDao.deleteUser()
                userDao.insertUser(UserEntity(user.id, user.fullName, user.email, user.role))
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to fetch profile"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun updateProfile(fullName: String?, phone: String?, profilePicUrl: String?): Flow<Resource<UserResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.updateProfile(com.capturo.app.data.model.request.UpdateUserRequest(fullName, phone, profilePicUrl))
            if (response.isSuccessful && response.body()?.success == true) {
                val user = response.body()!!.data!!
                userDao.deleteUser()
                userDao.insertUser(UserEntity(user.id, user.fullName, user.email, user.role))
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to update profile"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun updateAvatar(avatar: MultipartBody.Part): Flow<Resource<UserResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.updateAvatar(avatar)
            if (response.isSuccessful && response.body()?.success == true) {
                val user = response.body()!!.data!!
                userDao.deleteUser()
                userDao.insertUser(UserEntity(user.id, user.fullName, user.email, user.role))
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to upload avatar"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)
}
