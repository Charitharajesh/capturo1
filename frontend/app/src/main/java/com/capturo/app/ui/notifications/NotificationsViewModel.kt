package com.capturo.app.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capturo.app.adapter.NotificationListItem
import com.capturo.app.data.model.response.NotificationResponse
import com.capturo.app.data.model.response.UpdatedCountResponse
import com.capturo.app.data.repository.NotificationRepository
import com.capturo.app.utils.DateTimeUtils
import com.capturo.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * ViewModel for managing the state of notifications on the Notifications screen (Screen 18).
 * Coordinates notifications list retrieval, groupings (Today, Yesterday, Earlier),
 * status updates (mark as read / mark all as read), and soft-deletes via the repository.
 *
 * Implements MVVM Architecture Contract: retains zero reference to view contexts, exposes state
 * streams exclusively through [LiveData] wrapped in [Resource] containers, and handles IO-bound coroutine
 * flows inside the [viewModelScope].
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableLiveData<Resource<List<NotificationListItem>>>()
    /** Stream of grouped notifications categorized into section headers (Today, Yesterday, Earlier) and items. */
    val notifications: LiveData<Resource<List<NotificationListItem>>> get() = _notifications

    private val _markAllReadState = MutableLiveData<Resource<UpdatedCountResponse>>()
    /** Transient transaction state for mass read-operation executions. */
    val markAllReadState: LiveData<Resource<UpdatedCountResponse>> get() = _markAllReadState

    // Track the raw notifications list to update local states dynamically for optimistic UI updates.
    private var rawNotifications = mutableListOf<NotificationResponse>()

    init {
        loadNotifications()
    }

    /**
     * Queries the repository for paginated notifications histories and updates [_notifications].
     * Categorizes items under [Today, Yesterday, Earlier] section headers on successful emissions.
     */
    fun loadNotifications() {
        viewModelScope.launch {
            repository.getNotifications(isRead = null, page = 1, perPage = 100).collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _notifications.value = Resource.Loading
                    }
                    is Resource.Success -> {
                        val items = resource.data.items
                        rawNotifications = items.toMutableList()
                        val grouped = groupNotifications(rawNotifications)
                        _notifications.value = Resource.Success(grouped)
                    }
                    is Resource.Error -> {
                        _notifications.value = Resource.Error(resource.message)
                    }
                }
            }
        }
    }

    /**
     * Marks a specific notification as read by communicating with the API and updates local
     * list states dynamically to achieve positive, lag-free UI state updates.
     *
     * @param notificationId The unique ID of the target notification record.
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId).collectLatest { resource ->
                if (resource is Resource.Success) {
                    // Update locally
                    val index = rawNotifications.indexOfFirst { it.id == notificationId }
                    if (index != -1) {
                        rawNotifications[index] = rawNotifications[index].copy(isRead = true)
                        _notifications.value = Resource.Success(groupNotifications(rawNotifications))
                    }
                }
            }
        }
    }

    /**
     * Executes a mass read transaction update against the backend repository and immediately updates
     * local memory records to eliminate outstanding badge indicators across UI components.
     */
    fun markAllRead() {
        viewModelScope.launch {
            _markAllReadState.value = Resource.Loading
            repository.markAllNotificationsRead().collectLatest { resource ->
                _markAllReadState.value = resource
                if (resource is Resource.Success) {
                    // Update all items locally to read
                    rawNotifications = rawNotifications.map { it.copy(isRead = true) }.toMutableList()
                    _notifications.value = Resource.Success(groupNotifications(rawNotifications))
                }
            }
        }
    }

    /**
     * Performs a soft-delete request for the specified notification ID and removes it locally from
     * the list collections to instantly animate item removal states inside active adapters.
     *
     * @param notificationId The unique ID of the deleted notification record.
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId).collectLatest { resource ->
                if (resource is Resource.Success) {
                    // Remove locally
                    rawNotifications.removeAll { it.id == notificationId }
                    _notifications.value = Resource.Success(groupNotifications(rawNotifications))
                }
            }
        }
    }

    private fun groupNotifications(notifications: List<NotificationResponse>): List<NotificationListItem> {
        val today = LocalDate.now(ZoneId.systemDefault())
        val yesterday = today.minusDays(1)

        val todayList = mutableListOf<NotificationResponse>()
        val yesterdayList = mutableListOf<NotificationResponse>()
        val earlierList = mutableListOf<NotificationResponse>()

        for (notif in notifications) {
            val notifDate = DateTimeUtils.parseFromBackend(notif.createdAt).toLocalDate()
            when {
                notifDate.isEqual(today) -> todayList.add(notif)
                notifDate.isEqual(yesterday) -> yesterdayList.add(notif)
                else -> earlierList.add(notif)
            }
        }

        val result = mutableListOf<NotificationListItem>()
        if (todayList.isNotEmpty()) {
            result.add(NotificationListItem.Header("Today"))
            result.addAll(todayList.map { NotificationListItem.Item(it) })
        }
        if (yesterdayList.isNotEmpty()) {
            result.add(NotificationListItem.Header("Yesterday"))
            result.addAll(yesterdayList.map { NotificationListItem.Item(it) })
        }
        if (earlierList.isNotEmpty()) {
            result.add(NotificationListItem.Header("Earlier"))
            result.addAll(earlierList.map { NotificationListItem.Item(it) })
        }
        return result
    }
}
