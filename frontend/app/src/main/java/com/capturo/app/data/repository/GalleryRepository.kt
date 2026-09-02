package com.capturo.app.data.repository

import com.capturo.app.data.api.GalleryApiService
import com.capturo.app.data.model.request.*
import com.capturo.app.data.model.response.*
import com.capturo.app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepository @Inject constructor(
    private val api: GalleryApiService
) {
    fun uploadFile(
        file: MultipartBody.Part,
        title: RequestBody,
        description: RequestBody,
        bookingId: RequestBody? = null,
        isPortfolio: RequestBody? = null,
        isClientDelivery: RequestBody? = null
    ): Flow<Resource<GalleryResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.uploadFile(file, title, description, bookingId, isPortfolio, isClientDelivery)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Upload failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getCreatorPortfolio(
        creatorId: String,
        fileType: String? = null,
        page: Int? = null,
        perPage: Int? = null
    ): Flow<Resource<PaginatedResponse<GalleryResponse>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getCreatorPortfolio(creatorId, fileType, page, perPage)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to fetch portfolio"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getDeliveryGallery(bookingId: String): Flow<Resource<ClientDeliveryGalleryResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getDeliveryGallery(bookingId)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to fetch delivery gallery"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun createShareLink(bookingId: String, request: CreateShareLinkRequest): Flow<Resource<ShareLinkResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.createShareLink(bookingId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to create share link"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun downloadDelivery(bookingId: String): Flow<Resource<DownloadResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.downloadDelivery(bookingId)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to initiate download"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun updateGalleryItem(itemId: String, request: UpdateGalleryItemRequest): Flow<Resource<GalleryResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.updateGalleryItem(itemId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to update item"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteGalleryItem(itemId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.deleteGalleryItem(itemId)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to delete item"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)
}
