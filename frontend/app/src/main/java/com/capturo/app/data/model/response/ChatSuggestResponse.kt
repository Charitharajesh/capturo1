package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class ChatSuggestResponse(
    @SerializedName("suggestions") val suggestions: List<String>
)
