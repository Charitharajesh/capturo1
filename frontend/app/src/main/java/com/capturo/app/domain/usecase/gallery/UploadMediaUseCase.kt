package com.capturo.app.domain.usecase.gallery

import com.capturo.app.data.repository.GalleryRepository
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class UploadMediaUseCase @Inject constructor(
    private val repository: GalleryRepository
) {
    operator fun invoke(
        file: MultipartBody.Part,
        title: RequestBody,
        description: RequestBody,
        bookingId: RequestBody? = null,
        isPortfolio: RequestBody? = null,
        isClientDelivery: RequestBody? = null
    ) = repository.uploadFile(file, title, description, bookingId, isPortfolio, isClientDelivery)
}
