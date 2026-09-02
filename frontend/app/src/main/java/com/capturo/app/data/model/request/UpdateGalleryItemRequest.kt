package com.capturo.app.data.model.request

import com.google.gson.annotations.SerializedName

data class UpdateGalleryItemRequest(
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("is_portfolio") val isPortfolio: Boolean? = null,
    @SerializedName("is_client_delivery") val isClientDelivery: Boolean? = null
)
