package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class UnreadCountResponse(
    @SerializedName("unread_count") val unreadCount: Int
)
