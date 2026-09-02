package com.capturo.app.data.repository

import com.capturo.app.data.api.NotificationApiService
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
class NotificationRepository @Inject constructor(
    private val api: NotificationApiService
) {
    fun registerFCMToken(token: String): Flow<Resource<Map<String, String>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.registerFCMToken(FCMTokenRequest(token))
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to register FCM token"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getNotifications(
        isRead: Boolean? = null,
        page: Int? = null,
        perPage: Int? = null
    ): Flow<Resource<PaginatedResponse<NotificationResponse>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getNotifications(isRead, page, perPage)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to fetch notifications"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getUnreadCount(): Flow<Resource<Map<String, Int>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getUnreadCount()
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to retrieve unread count"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun markNotificationRead(notificationId: String): Flow<Resource<NotificationResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.markNotificationRead(notificationId)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to mark notification as read"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun markAllNotificationsRead(): Flow<Resource<UpdatedCountResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.markAllNotificationsRead()
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to mark notifications as read"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteNotification(notificationId: String): Flow<Resource<Map<String, String>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.deleteNotification(notificationId)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to delete notification"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)
}
