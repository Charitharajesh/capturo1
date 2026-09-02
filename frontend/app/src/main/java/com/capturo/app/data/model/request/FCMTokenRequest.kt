package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class FCMTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String
)
