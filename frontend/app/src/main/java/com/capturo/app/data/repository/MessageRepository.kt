package com.capturo.app.data.repository

import com.capturo.app.data.api.MessageApiService
import com.capturo.app.data.local.dao.MessageDao
import com.capturo.app.data.local.entity.MessageEntity
import com.capturo.app.data.model.request.SendMessageRequest
import com.capturo.app.data.model.response.*
import com.capturo.app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val api: MessageApiService,
    private val messageDao: MessageDao
) {
    // Expose cached messages directly as Flow of DB entities
    fun getMessagesForBooking(bookingId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForBooking(bookingId)
    }

    // Sync remote chat history into local Room database
    fun syncChatHistory(bookingId: String, page: Int? = null, perPage: Int? = null): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getChatHistory(bookingId, page, perPage)
            if (response.isSuccessful && response.body() != null) {
                val paginatedResponse = response.body()!!
                val entities = paginatedResponse.items.map { item ->
                    MessageEntity(
                        id = item.id,
                        bookingId = item.bookingId,
                        senderId = item.senderId,
                        receiverId = item.receiverId,
                        content = item.content ?: "",
                        isRead = item.isRead,
                        createdAt = item.createdAt
                    )
                }
                
                try {
                    // Update cache
                    messageDao.insertMessages(entities)
                } catch (dbEx: Exception) {
                    // Ignore local cache database write errors
                }
                
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error("Failed to sync messages with server"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun sendMessage(request: SendMessageRequest): Flow<Resource<MessageResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.sendMessage(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                
                // Cache our sent message immediately
                try {
                    messageDao.insertMessages(listOf(
                        MessageEntity(
                            id = data.id,
                            bookingId = data.bookingId,
                            senderId = data.senderId,
                            receiverId = data.receiverId,
                            content = data.content ?: "",
                            isRead = data.isRead,
                            createdAt = data.createdAt
                        )
                    ))
                } catch (dbEx: Exception) {
                    // Ignore database errors
                }

                emit(Resource.Success(data))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to send message"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getUnreadCount(): Flow<Resource<UnreadCountResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getUnreadCount()
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to fetch unread count"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getChatHistory(bookingId: String, page: Int? = null, perPage: Int? = null): Flow<Resource<PaginatedResponse<MessageResponse>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getChatHistory(bookingId, page, perPage)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to fetch chat history"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun markMessageRead(messageId: String): Flow<Resource<MessageResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.markMessageRead(messageId)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                
                // Cache read status change
                try {
                    messageDao.markAsRead(messageId)
                } catch (dbEx: Exception) {
                    // Ignore database error
                }
                
                emit(Resource.Success(data))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to mark message as read"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteMessage(messageId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.deleteMessage(messageId)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to delete message"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun aiSuggestReply(request: com.capturo.app.data.model.request.ChatSuggestRequest): Flow<Resource<com.capturo.app.data.model.response.ChatSuggestResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.aiSuggestReply(request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to get smart replies"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)
}
