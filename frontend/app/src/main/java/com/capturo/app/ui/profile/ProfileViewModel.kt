package com.capturo.app.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capturo.app.data.model.response.CreatorStatsResponse
import com.capturo.app.data.model.response.UserResponse
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.preferences.UserRole
import com.capturo.app.data.repository.CreatorRepository
import com.capturo.app.data.repository.UserRepository
import com.capturo.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing the user profile screen states and session logout procedures.
 * Lazily binds user role caches from [SessionManager] and synchronizes data with the REST backend.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val creatorRepository: CreatorRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _userProfile = MutableLiveData<Resource<UserResponse>>()
    /** Exposes user profile resource state loaded from network & cache. */
    val userProfile: LiveData<Resource<UserResponse>> get() = _userProfile

    private val _creatorStats = MutableLiveData<Resource<CreatorStatsResponse>>()
    /** Exposes professional creator stats (earnings, total bookings, average ratings) if the user is a creator. */
    val creatorStats: LiveData<Resource<CreatorStatsResponse>> get() = _creatorStats

    private val _logoutEvent = MutableLiveData<Boolean>()
    /** Trigger stream for navigating to Login screen upon session termination. */
    val logoutEvent: LiveData<Boolean> get() = _logoutEvent

    init {
        loadProfile()
    }

    /**
     * Retrieves fresh profile states from the backend, updates local database caches,
     * and queries supplemental professional stats if the authenticated role is a Creator.
     */
    fun loadProfile() {
        viewModelScope.launch {
            userRepository.getProfile().collectLatest { resource ->
                _userProfile.value = resource
                if (resource is Resource.Success) {
                    val user = resource.data
                    // If the user's role is a creator, also load their professional statistics
                    if (sessionManager.getUserRole() == UserRole.CREATOR) {
                        loadCreatorStats(user.id)
                    }
                }
            }
        }
    }

    private fun loadCreatorStats(userId: String) {
        viewModelScope.launch {
            creatorRepository.getCreatorStats(userId).collectLatest { resource ->
                _creatorStats.value = resource
            }
        }
    }

    /**
     * Clears all session tokens from encrypted storage and triggers the logout event.
     */
    fun logout() {
        sessionManager.clearSession()
        _logoutEvent.value = true
    }
}
