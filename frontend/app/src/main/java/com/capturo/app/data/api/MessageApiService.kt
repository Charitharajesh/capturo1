package com.capturo.app.data.api

import com.capturo.app.data.model.request.*
import com.capturo.app.data.model.response.*
import retrofit2.Response
import retrofit2.http.*

interface MessageApiService {
    @POST("messages")
    suspend fun sendMessage(
        @Body request: SendMessageRequest
    ): Response<ApiResponse<MessageResponse>>

    @GET("messages/unread-count")
    suspend fun getUnreadCount(): Response<ApiResponse<UnreadCountResponse>>

    @GET("messages/{booking_id}")
    suspend fun getChatHistory(
        @Path("booking_id") bookingId: String,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<PaginatedResponse<MessageResponse>>

    @PATCH("messages/{message_id}/read")
    suspend fun markMessageRead(
        @Path("message_id") messageId: String
    ): Response<ApiResponse<MessageResponse>>

    @DELETE("messages/{message_id}")
    suspend fun deleteMessage(
        @Path("message_id") messageId: String
    ): Response<ApiResponse<Unit>>

    @POST("ai/chat/suggest-reply")
    suspend fun aiSuggestReply(
        @Body request: com.capturo.app.data.model.request.ChatSuggestRequest
    ): Response<ApiResponse<ChatSuggestResponse>>
}
