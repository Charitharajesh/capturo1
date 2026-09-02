package com.capturo.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capturo.app.data.local.entity.MessageEntity
import com.capturo.app.data.model.request.SendMessageRequest
import com.capturo.app.data.model.response.MessageResponse
import com.capturo.app.data.repository.MessageRepository
import com.capturo.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _syncState = MutableStateFlow<Resource<Unit>>(Resource.Success(Unit))
    val syncState: StateFlow<Resource<Unit>> = _syncState.asStateFlow()

    private val _sendState = MutableStateFlow<Resource<MessageResponse>?>(null)
    val sendState: StateFlow<Resource<MessageResponse>?> = _sendState.asStateFlow()

    // Expose reactive message list Flow from local database cache directly
    fun getMessageFlow(bookingId: String): Flow<List<MessageEntity>> {
        return messageRepository.getMessagesForBooking(bookingId)
    }

    fun syncChat(bookingId: String) {
        viewModelScope.launch {
            _syncState.value = Resource.Loading
            messageRepository.syncChatHistory(bookingId).collectLatest { resource ->
                _syncState.value = resource
            }
        }
    }

    fun sendMessage(bookingId: String, receiverId: String, content: String) {
        viewModelScope.launch {
            _sendState.value = Resource.Loading
            val request = SendMessageRequest(
                bookingId = bookingId,
                receiverId = receiverId,
                content = content,
                messageType = "text"
            )
            messageRepository.sendMessage(request).collectLatest { resource ->
                _sendState.value = resource
            }
        }
    }

    fun resetSendState() {
        _sendState.value = null
    }

    private val _replySuggestions = MutableStateFlow<Resource<com.capturo.app.data.model.response.ChatSuggestResponse>?>(null)
    val replySuggestions: StateFlow<Resource<com.capturo.app.data.model.response.ChatSuggestResponse>?> = _replySuggestions.asStateFlow()

    fun fetchAiSuggestions(lastMessage: String, senderRole: String, contextText: String) {
        viewModelScope.launch {
            _replySuggestions.value = Resource.Loading
            val request = com.capturo.app.data.model.request.ChatSuggestRequest(
                lastMessage = lastMessage,
                senderRole = senderRole,
                conversationContext = contextText
            )
            messageRepository.aiSuggestReply(request).collectLatest { resource ->
                _replySuggestions.value = resource
            }
        }
    }
}
