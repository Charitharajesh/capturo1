package com.capturo.app.ui.creatorDashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.capturo.app.data.model.response.BookingResponse
import com.capturo.app.data.model.response.CreatorProfileResponse
import com.capturo.app.data.model.response.CreatorStatsResponse
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.repository.BookingRepository
import com.capturo.app.data.repository.CreatorRepository
import com.capturo.app.ui.common.BaseViewModel
import com.capturo.app.utils.DateTimeUtils
import com.capturo.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CreatorDashboardStats(
    val earningsThisMonth: Double,
    val totalBookings: Int,
    val pendingBookingsCount: Int,
    val averageRating: Double,
    val totalReviews: Int,
    val followersCount: Int,
    val revenueGrowthPercentage: Double
)

@HiltViewModel
class CreatorDashboardViewModel @Inject constructor(
    private val creatorRepository: CreatorRepository,
    private val bookingRepository: BookingRepository,
    private val sessionManager: SessionManager
) : BaseViewModel() {

    private val _dashboardStats = MutableLiveData<Resource<CreatorDashboardStats>>()
    val dashboardStats: LiveData<Resource<CreatorDashboardStats>> = _dashboardStats

    private val _todaySchedule = MutableLiveData<Resource<List<BookingResponse>>>()
    val todaySchedule: LiveData<Resource<List<BookingResponse>>> = _todaySchedule

    // Local buffers to hold independent API results stably
    private val profileData = MutableLiveData<CreatorProfileResponse?>()
    private val statsData = MutableLiveData<CreatorStatsResponse?>()
    private val pendingBookings = MutableLiveData<List<BookingResponse>?>()

    fun loadDashboardData() {
        val userId = sessionManager.getUserId() ?: return
        
        _dashboardStats.value = Resource.Loading
        profileData.value = null
        statsData.value = null
        pendingBookings.value = null
        
        // Fetch Profile independently
        viewModelScope.launch {
            creatorRepository.getOwnCreatorProfile().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        profileData.value = resource.data
                        checkAndEmitStats()
                    }
                    is Resource.Error -> {
                        _dashboardStats.value = Resource.Error(resource.message, resource.exception)
                    }
                    is Resource.Loading -> {}
                }
            }
        }

        // Fetch Stats independently
        viewModelScope.launch {
            creatorRepository.getCreatorStats(userId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        statsData.value = resource.data
                        checkAndEmitStats()
                    }
                    is Resource.Error -> {
                        _dashboardStats.value = Resource.Error(resource.message, resource.exception)
                    }
                    is Resource.Loading -> {}
                }
            }
        }

        // Fetch Pending bookings independently
        viewModelScope.launch {
            bookingRepository.getMyBookings("pending").collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        pendingBookings.value = resource.data.items
                        checkAndEmitStats()
                    }
                    is Resource.Error -> {
                        _dashboardStats.value = Resource.Error(resource.message, resource.exception)
                    }
                    is Resource.Loading -> {}
                }
            }
        }

        loadTodaySchedule()
    }

    private fun checkAndEmitStats() {
        val profile = profileData.value
        val stats = statsData.value
        val pending = pendingBookings.value

        if (profile != null && stats != null && pending != null) {
            _dashboardStats.value = Resource.Success(
                CreatorDashboardStats(
                    earningsThisMonth = stats.earningsThisMonth,
                    totalBookings = stats.bookings,
                    pendingBookingsCount = pending.size,
                    averageRating = profile.avgRating,
                    totalReviews = profile.totalReviews,
                    followersCount = profile.followersCount,
                    revenueGrowthPercentage = stats.revenueGrowthPercentage
                )
            )
        }
    }

    private fun loadTodaySchedule() {
        viewModelScope.launch {
            _todaySchedule.value = Resource.Loading
            val todayIso = DateTimeUtils.toApiDateFormat(LocalDate.now())
            
            bookingRepository.getMyBookings("confirmed").collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val allConfirmed = resource.data.items
                        val todayBookings = allConfirmed.filter { booking ->
                            booking.eventDate == todayIso
                        }
                        if (todayBookings.isNotEmpty()) {
                            _todaySchedule.value = Resource.Success(todayBookings)
                        } else {
                            // Find nearest upcoming booking (date is >= today)
                            val nearestBooking = allConfirmed
                                .filter { booking -> booking.eventDate >= todayIso }
                                .minByOrNull { booking -> booking.eventDate }
                            
                            _todaySchedule.value = Resource.Success(
                                if (nearestBooking != null) listOf(nearestBooking) else emptyList()
                            )
                        }
                    }
                    is Resource.Error -> {
                        _todaySchedule.value = Resource.Error(resource.message, resource.exception)
                    }
                    is Resource.Loading -> {
                        _todaySchedule.value = Resource.Loading
                    }
                }
            }
        }
    }
}
