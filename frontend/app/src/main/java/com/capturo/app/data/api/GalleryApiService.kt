package com.capturo.app.data.api

import com.capturo.app.data.model.request.*
import com.capturo.app.data.model.response.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface GalleryApiService {
    @Multipart
    @POST("gallery/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("booking_id") bookingId: RequestBody? = null,
        @Part("is_portfolio") isPortfolio: RequestBody? = null,
        @Part("is_client_delivery") isClientDelivery: RequestBody? = null
    ): Response<ApiResponse<GalleryResponse>>

    @GET("gallery/creator/{creator_id}")
    suspend fun getCreatorPortfolio(
        @Path("creator_id") creatorId: String,
        @Query("file_type") fileType: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<PaginatedResponse<GalleryResponse>>

    @GET("gallery/delivery/{booking_id}")
    suspend fun getDeliveryGallery(
        @Path("booking_id") bookingId: String
    ): Response<ApiResponse<ClientDeliveryGalleryResponse>>

    @POST("gallery/delivery/{booking_id}/share")
    suspend fun createShareLink(
        @Path("booking_id") bookingId: String,
        @Body request: CreateShareLinkRequest
    ): Response<ApiResponse<ShareLinkResponse>>

    @GET("gallery/delivery/{booking_id}/download")
    suspend fun downloadDelivery(
        @Path("booking_id") bookingId: String
    ): Response<ApiResponse<DownloadResponse>>

    @PATCH("gallery/{item_id}")
    suspend fun updateGalleryItem(
        @Path("item_id") itemId: String,
        @Body request: UpdateGalleryItemRequest
    ): Response<ApiResponse<GalleryResponse>>

    @DELETE("gallery/{item_id}")
    suspend fun deleteGalleryItem(
        @Path("item_id") itemId: String
    ): Response<ApiResponse<Unit>>
}
