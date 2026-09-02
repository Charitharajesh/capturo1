package com.capturo.app.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capturo.app.data.model.response.CreatorDistanceResponse
import com.capturo.app.data.repository.CreatorRepository
import com.capturo.app.data.repository.BookingRepository
import com.capturo.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilterParams(
    val specialization: String? = null,
    val minRating: Double? = null,
    val maxRate: Double? = null,
    val radiusKm: Double = 10.0
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val creatorRepository: CreatorRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _nearbyCreators = MutableLiveData<Resource<List<CreatorDistanceResponse>>>()
    val nearbyCreators: LiveData<Resource<List<CreatorDistanceResponse>>> = _nearbyCreators

    // Client-side pagination variables to satisfy "paginated lists" requirement
    private var allNearbyCreators = listOf<CreatorDistanceResponse>()
    private val _paginatedCreators = MutableLiveData<List<CreatorDistanceResponse>>()
    val paginatedCreators: LiveData<List<CreatorDistanceResponse>> = _paginatedCreators

    private var currentLat: Double? = null
    private var currentLon: Double? = null
    
    var activeFilters = FilterParams()
        private set

    var currentPage = 1
        private set
    var isLoading = false
        private set
    var hasMore = true
        private set

    private val perPage = 10

    fun loadNearbyCreators(lat: Double, lon: Double) {
        currentLat = lat
        currentLon = lon
        currentPage = 1
        hasMore = true
        fetchNearby()
    }

    fun applyFilters(params: FilterParams) {
        activeFilters = params
        currentPage = 1
        hasMore = true
        fetchNearby()
    }

    fun clearFilters() {
        activeFilters = FilterParams()
        currentPage = 1
        hasMore = true
        fetchNearby()
    }

    fun loadNextPage() {
        if (isLoading || !hasMore) return
        isLoading = true
        currentPage++
        paginateList()
    }

    private fun fetchNearby() {
        val lat = currentLat ?: return
        val lon = currentLon ?: return

        viewModelScope.launch {
            var activeBookingCreatorIds: Set<String> = emptySet()
            try {
                val bookingResource = bookingRepository.getMyBookings(null, 1, 100)
                    .first { it is Resource.Success || it is Resource.Error }
                if (bookingResource is Resource.Success) {
                    activeBookingCreatorIds = bookingResource.data.items
                        .filter { it.status != "cancelled" && it.status != "completed" }
                        .mapNotNull { it.creatorId }
                        .toSet()
                }
            } catch (e: Exception) {
                // Fallback to empty set on error
            }

            creatorRepository.getNearbyCreators(
                lat, 
                lon, 
                activeFilters.radiusKm, 
                if (activeFilters.specialization?.lowercase() == "all") null else activeFilters.specialization
            ).collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        isLoading = true
                        _nearbyCreators.value = Resource.Loading
                    }
                    is Resource.Success -> {
                        isLoading = false
                        var filteredList = resource.data

                        // Filter out creators with active bookings
                        filteredList = filteredList.filter { it.creator.id !in activeBookingCreatorIds }

                        // Apply min rating filter
                        activeFilters.minRating?.let { minRating ->
                            filteredList = filteredList.filter { it.creator.avgRating >= minRating }
                        }

                        // Apply max price filter
                        activeFilters.maxRate?.let { maxRate ->
                            filteredList = filteredList.filter { it.creator.hourlyRate <= maxRate }
                        }

                        allNearbyCreators = filteredList
                        currentPage = 1
                        hasMore = allNearbyCreators.size > perPage
                        
                        _nearbyCreators.value = Resource.Success(filteredList)
                        paginateList()
                    }
                    is Resource.Error -> {
                        isLoading = false
                        _nearbyCreators.value = Resource.Error(resource.message)
                    }
                }
            }
        }
    }

    private fun paginateList() {
        val end = (currentPage * perPage).coerceAtMost(allNearbyCreators.size)
        val paginated = allNearbyCreators.subList(0, end)
        _paginatedCreators.value = paginated
        hasMore = end < allNearbyCreators.size
        isLoading = false
    }
}
