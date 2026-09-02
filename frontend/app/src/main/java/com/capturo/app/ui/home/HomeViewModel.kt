package com.capturo.app.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capturo.app.data.model.response.BookingResponse
import com.capturo.app.data.model.response.CreatorResponse
import com.capturo.app.data.repository.BookingRepository
import com.capturo.app.data.repository.CreatorRepository
import com.capturo.app.data.repository.NotificationRepository
import com.capturo.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home Dashboard.
 * Exposes LiveData streams for featured creators, upcoming bookings, and unread notification counts.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val creatorRepository: CreatorRepository,
    private val bookingRepository: BookingRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _featuredCreators = MutableLiveData<Resource<List<CreatorResponse>>>()
    val featuredCreators: LiveData<Resource<List<CreatorResponse>>> get() = _featuredCreators

    private val _followedCreators = MutableLiveData<Resource<List<CreatorResponse>>>()
    val followedCreators: LiveData<Resource<List<CreatorResponse>>> get() = _followedCreators

    private val _upcomingBooking = MutableLiveData<BookingResponse?>()
    val upcomingBooking: LiveData<BookingResponse?> get() = _upcomingBooking

    private val _unreadNotificationCount = MutableLiveData<Int>()
    val unreadNotificationCount: LiveData<Int> get() = _unreadNotificationCount

    private var activeBookingCreatorIds: Set<String> = emptySet()

    init {
        refresh()
    }

    fun refresh() {
        fetchBookingsAndCreators()
        fetchUnreadNotificationCount()
    }

    private fun fetchBookingsAndCreators() {
        viewModelScope.launch {
            bookingRepository.getMyBookings(null, 1, 100).collect { resource ->
                if (resource is Resource.Success) {
                    val bookings = resource.data.items
                    val activeBookings = bookings.filter { it.status != "cancelled" && it.status != "completed" }
                    activeBookingCreatorIds = activeBookings.mapNotNull { it.creatorId }.toSet()

                    val now = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault())
                    val upcoming = activeBookings
                        .mapNotNull { booking ->
                            val dt = getBookingDateTime(booking)
                            if (dt != null && dt.isAfter(now)) Pair(booking, dt) else null
                        }
                        .minByOrNull { it.second }
                        ?.first
                    _upcomingBooking.value = upcoming

                    fetchFeaturedCreators()
                    fetchFollowedCreators()
                } else if (resource is Resource.Error) {
                    fetchFeaturedCreators()
                    fetchFollowedCreators()
                }
            }
        }
    }

    private fun fetchFeaturedCreators() {
        viewModelScope.launch {
            creatorRepository.getCreators().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val filtered = resource.data.filter { it.id !in activeBookingCreatorIds }
                        _featuredCreators.value = Resource.Success(filtered)
                    }
                    else -> {
                        _featuredCreators.value = resource
                    }
                }
            }
        }
    }

    private fun fetchFollowedCreators() {
        viewModelScope.launch {
            creatorRepository.getFollowingCreators().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val filtered = resource.data.filter { it.id !in activeBookingCreatorIds }
                        _followedCreators.value = Resource.Success(filtered)
                    }
                    else -> {
                        _followedCreators.value = resource
                    }
                }
            }
        }
    }

    private fun fetchUnreadNotificationCount() {
        viewModelScope.launch {
            notificationRepository.getUnreadCount().collect { resource ->
                if (resource is Resource.Success) {
                    val count = resource.data["unread_count"] ?: 0
                    _unreadNotificationCount.value = count
                }
            }
        }
    }

    private fun getBookingDateTime(booking: BookingResponse): java.time.ZonedDateTime? {
        return try {
            val date = java.time.LocalDate.parse(booking.eventDate)
            val time = parseLocalTime(booking.startTime)
            java.time.LocalDateTime.of(date, time).atZone(java.time.ZoneId.systemDefault())
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLocalTime(timeStr: String): java.time.LocalTime {
        return try {
            java.time.LocalTime.parse(timeStr)
        } catch (e: Exception) {
            val parts = timeStr.split(":")
            val hh = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val mm = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val ss = parts.getOrNull(2)?.toIntOrNull() ?: 0
            java.time.LocalTime.of(hh, mm, ss)
        }
    }
}
