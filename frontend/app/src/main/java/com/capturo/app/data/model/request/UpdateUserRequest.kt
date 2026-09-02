package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class UpdateUserRequest(
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("profile_pic_url") val profilePicUrl: String? = null
)
