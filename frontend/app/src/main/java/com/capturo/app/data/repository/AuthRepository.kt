package com.capturo.app.data.repository

import com.capturo.app.data.api.AuthApiService
import com.capturo.app.data.model.request.*
import com.capturo.app.data.model.response.*
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApiService,
    private val sessionManager: SessionManager
) {
    fun login(request: LoginRequest): Flow<Resource<AuthResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.login(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                sessionManager.saveSession(data)
                emit(Resource.Success(data))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Login failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun register(request: RegisterRequest): Flow<Resource<UserResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.register(request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Registration failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun logout(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val refreshToken = sessionManager.getRefreshToken()
            if (refreshToken != null) {
                api.logout(LogoutRequest(refreshToken))
            }
            sessionManager.clearSession()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            sessionManager.clearSession()
            emit(Resource.Success(Unit)) // Still emit Success as session is cleared locally
        }
    }.flowOn(Dispatchers.IO)

    fun forgotPassword(email: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.forgotPassword(ForgotPasswordRequest(email))
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to request code"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun resetPassword(token: String, newPass: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.resetPassword(ResetPasswordRequest(token, newPass))
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to reset password"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)
}
