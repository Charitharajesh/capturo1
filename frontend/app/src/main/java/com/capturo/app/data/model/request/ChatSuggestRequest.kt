package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class ChatSuggestRequest(
    @SerializedName("last_message") val lastMessage: String,
    @SerializedName("sender_role") val senderRole: String,
    @SerializedName("conversation_context") val conversationContext: String
)
