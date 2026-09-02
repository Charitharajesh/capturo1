package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class MatchResponse(
    @SerializedName("creators") val creators: List<CreatorResponse>,
    @SerializedName("ai_powered") val aiPowered: Boolean = true
)
