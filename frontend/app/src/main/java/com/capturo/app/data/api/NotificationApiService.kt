package com.capturo.app.data.api

import com.capturo.app.data.model.request.*
import com.capturo.app.data.model.response.*
import retrofit2.Response
import retrofit2.http.*

interface NotificationApiService {
    @POST("users/me/fcm-token")
    suspend fun registerFCMToken(
        @Body request: FCMTokenRequest
    ): Response<ApiResponse<Map<String, String>>>

    @GET("notifications")
    suspend fun getNotifications(
        @Query("is_read") isRead: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<PaginatedResponse<NotificationResponse>>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): Response<ApiResponse<Map<String, Int>>>

    @PATCH("notifications/{notification_id}/read")
    suspend fun markNotificationRead(
        @Path("notification_id") notificationId: String
    ): Response<ApiResponse<NotificationResponse>>

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ApiResponse<UpdatedCountResponse>>

    @DELETE("notifications/{notification_id}")
    suspend fun deleteNotification(
        @Path("notification_id") notificationId: String
    ): Response<ApiResponse<Map<String, String>>>
}
