package com.capturo.app.ui.gallery

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.capturo.app.data.model.request.CreateShareLinkRequest
import com.capturo.app.data.model.response.ClientDeliveryGalleryResponse
import com.capturo.app.data.model.response.DownloadResponse
import com.capturo.app.data.model.response.ShareLinkResponse
import com.capturo.app.data.repository.GalleryRepository
import com.capturo.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val galleryRepository: GalleryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _deliveryState = MutableLiveData<Resource<ClientDeliveryGalleryResponse>>()
    val deliveryState: LiveData<Resource<ClientDeliveryGalleryResponse>> = _deliveryState

    private val _shareLinkState = MutableLiveData<Resource<ShareLinkResponse>>()
    val shareLinkState: LiveData<Resource<ShareLinkResponse>> = _shareLinkState

    private val _downloadState = MutableLiveData<Resource<DownloadResponse>>()
    val downloadState: LiveData<Resource<DownloadResponse>> = _downloadState

    fun loadDeliveryGallery(bookingId: String) {
        viewModelScope.launch {
            galleryRepository.getDeliveryGallery(bookingId).collectLatest { resource ->
                _deliveryState.value = resource
            }
        }
    }

    fun createShareLink(bookingId: String, expiresDays: Int = 30) {
        viewModelScope.launch {
            val request = CreateShareLinkRequest(expiresInDays = expiresDays)
            galleryRepository.createShareLink(bookingId, request).collectLatest { resource ->
                _shareLinkState.value = resource
            }
        }
    }

    fun downloadAllDelivery(bookingId: String) {
        viewModelScope.launch {
            galleryRepository.downloadDelivery(bookingId).collectLatest { resource ->
                _downloadState.value = resource
                if (resource is Resource.Success) {
                    val downloadUrl = resource.data?.downloadUrl
                    if (!downloadUrl.isNullOrEmpty()) {
                        enqueueDownloadWork(bookingId, downloadUrl)
                    }
                }
            }
        }
    }

    private fun enqueueDownloadWork(bookingId: String, downloadUrl: String) {
        val workManager = WorkManager.getInstance(context)
        val data = workDataOf(
            "booking_id" to bookingId,
            "download_url" to downloadUrl
        )
        val workRequest = OneTimeWorkRequestBuilder<DownloadAllWorker>()
            .setInputData(data)
            .build()
        workManager.enqueue(workRequest)
    }
}
