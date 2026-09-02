package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class ShareLinkResponse(
    @SerializedName("share_url") val shareUrl: String,
    @SerializedName("expires_at") val expiresAt: String
)
