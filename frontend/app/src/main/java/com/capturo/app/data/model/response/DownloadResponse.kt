package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class DownloadResponse(
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("size_bytes") val sizeBytes: Long
)
