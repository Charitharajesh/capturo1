package com.capturo.app.ui.creator

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capturo.app.data.model.response.AvailabilityCheckResponse
import com.capturo.app.data.model.response.CreatorResponse
import com.capturo.app.data.model.response.CreatorPublicResponse
import com.capturo.app.data.model.response.GalleryResponse
import com.capturo.app.data.repository.CreatorRepository
import com.capturo.app.data.repository.GalleryRepository
import com.capturo.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatorViewModel @Inject constructor(
    private val creatorRepository: CreatorRepository,
    private val galleryRepository: GalleryRepository,
    private val reviewRepository: com.capturo.app.data.repository.ReviewRepository
) : ViewModel() {

    private val _aiSummary = MutableLiveData<Resource<com.capturo.app.data.model.response.ReviewSummariseResponse>>()
    val aiSummary: LiveData<Resource<com.capturo.app.data.model.response.ReviewSummariseResponse>> = _aiSummary

    private val _creatorProfile = MutableLiveData<Resource<CreatorPublicResponse>>()
    val creatorProfile: LiveData<Resource<CreatorPublicResponse>> = _creatorProfile

    private val _portfolio = MutableLiveData<Resource<List<GalleryResponse>>>()
    val portfolio: LiveData<Resource<List<GalleryResponse>>> = _portfolio

    private val _availability = MutableLiveData<Resource<AvailabilityCheckResponse>>()
    val availability: LiveData<Resource<AvailabilityCheckResponse>> = _availability

    private var lastCreatorId: String? = null

    fun loadCreator(creatorId: String) {
        lastCreatorId = creatorId
        viewModelScope.launch {
            creatorRepository.getCreatorById(creatorId).collectLatest { resource ->
                _creatorProfile.value = resource
            }
        }
    }

    fun loadPortfolio(creatorId: String) {
        viewModelScope.launch {
            galleryRepository.getCreatorPortfolio(creatorId).collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _portfolio.value = Resource.Loading
                    }
                    is Resource.Success -> {
                        _portfolio.value = Resource.Success(resource.data.items)
                    }
                    is Resource.Error -> {
                        _portfolio.value = Resource.Error(resource.message)
                    }
                }
            }
        }
    }

    fun checkAvailability(date: String, startTime: String, duration: Double) {
        val creatorId = lastCreatorId ?: return
        viewModelScope.launch {
            creatorRepository.checkAvailability(creatorId, date, startTime, duration).collectLatest { resource ->
                _availability.value = resource
            }
        }
    }

    private val _followState = MutableLiveData<Resource<Map<String, Any>>>()
    val followState: LiveData<Resource<Map<String, Any>>> = _followState

    fun followCreator(id: String) {
        viewModelScope.launch {
            creatorRepository.followCreator(id).collectLatest { resource ->
                _followState.value = resource
                if (resource is Resource.Success) {
                    val currentProfile = _creatorProfile.value
                    if (currentProfile is Resource.Success) {
                        val count = (resource.data["followers_count"] as? Double)?.toInt() ?: currentProfile.data.followersCount
                        val updated = currentProfile.data.copy(followersCount = count, isFollowing = true)
                        _creatorProfile.value = Resource.Success(updated)
                    }
                }
            }
        }
    }

    fun unfollowCreator(id: String) {
        viewModelScope.launch {
            creatorRepository.unfollowCreator(id).collectLatest { resource ->
                _followState.value = resource
                if (resource is Resource.Success) {
                    val currentProfile = _creatorProfile.value
                    if (currentProfile is Resource.Success) {
                        val count = (resource.data["followers_count"] as? Double)?.toInt() ?: currentProfile.data.followersCount
                        val updated = currentProfile.data.copy(followersCount = count, isFollowing = false)
                        _creatorProfile.value = Resource.Success(updated)
                    }
                }
            }
        }
    }

    fun fetchAiSummary(creatorId: String, creatorName: String) {
        viewModelScope.launch {
            reviewRepository.getCreatorReviews(creatorId).collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _aiSummary.value = Resource.Loading
                    }
                    is Resource.Success -> {
                        val reviews = resource.data.items
                        val inputs = reviews.map {
                            com.capturo.app.data.model.request.ReviewInput(
                                comment = it.comment.orEmpty(),
                                rating = it.rating
                            )
                        }
                        val summaryRequest = com.capturo.app.data.model.request.ReviewSummariseRequest(
                            creatorId = creatorId,
                            creatorName = creatorName,
                            reviews = inputs
                        )
                        creatorRepository.aiSummariseReviews(summaryRequest).collectLatest { aiResource ->
                            _aiSummary.value = aiResource
                        }
                    }
                    is Resource.Error -> {
                        _aiSummary.value = Resource.Error(resource.message)
                    }
                }
            }
        }
    }
}
