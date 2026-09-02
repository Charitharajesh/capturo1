package com.capturo.app.data.model.response

import com.google.gson.annotations.SerializedName

data class ClientDeliveryGalleryResponse(
    @SerializedName("items") val items: List<GalleryResponse>,
    @SerializedName("total") val total: Int,
    @SerializedName("total_size_gb") val totalSizeGb: Double
)
